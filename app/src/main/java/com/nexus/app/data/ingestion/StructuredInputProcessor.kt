package com.nexus.app.data.ingestion

import com.nexus.app.core.DateTimeUtils
import com.nexus.app.data.db.entity.AppointmentEntity
import com.nexus.app.data.db.entity.CreditCardStatementEntity
import com.nexus.app.data.db.entity.EventEntity
import com.nexus.app.data.db.entity.PersonalGoalEntity
import com.nexus.app.data.db.entity.ProjectEntity
import com.nexus.app.data.db.entity.SubtaskEntity
import com.nexus.app.data.db.entity.TaskEntity
import com.nexus.app.data.db.entity.TransactionEntity
import com.nexus.app.domain.model.EventCostType
import com.nexus.app.domain.model.EventLifecycleStatus
import com.nexus.app.domain.model.FinancialStatus
import com.nexus.app.domain.model.GoalProgressMode
import com.nexus.app.domain.model.GoalStatus
import com.nexus.app.domain.model.PaymentMethod
import com.nexus.app.domain.model.ProjectStatus
import com.nexus.app.domain.model.StatementStatus
import com.nexus.app.domain.model.TaskPriority
import com.nexus.app.domain.model.TaskStatus
import com.nexus.app.domain.model.TicketStatus
import com.nexus.app.domain.model.TransactionType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement

class StructuredInputProcessor(
    private val gateway: StructuredInputGateway,
    private val clockMillis: () -> Long = { DateTimeUtils.nowEpochMillis() }
) {

    private val strictJson = Json {
        ignoreUnknownKeys = false
        isLenient = false
        coerceInputValues = false
    }

    private sealed interface OperationPlan {
        val targetEntityId: String
        suspend fun apply(activeGateway: StructuredInputGateway)

        data class UpsertTaskWithSubtasks(
            val entity: TaskEntity,
            val subtasks: List<SubtaskEntity>
        ) : OperationPlan {
            override val targetEntityId: String get() = entity.id
            override suspend fun apply(activeGateway: StructuredInputGateway) {
                activeGateway.insertTask(entity)
                if (subtasks.isNotEmpty()) {
                    activeGateway.insertSubtasks(subtasks)
                }
            }
        }

        data class UpsertChecklist(
            val taskId: String,
            val subtasks: List<SubtaskEntity>
        ) : OperationPlan {
            override val targetEntityId: String get() = subtasks.firstOrNull()?.id ?: taskId
            override suspend fun apply(activeGateway: StructuredInputGateway) {
                activeGateway.insertSubtasks(subtasks)
            }
        }

        data class UpsertAppointment(val entity: AppointmentEntity) : OperationPlan {
            override val targetEntityId: String get() = entity.id
            override suspend fun apply(activeGateway: StructuredInputGateway) =
                activeGateway.insertAppointment(entity)
        }

        data class UpsertEvent(val entity: EventEntity) : OperationPlan {
            override val targetEntityId: String get() = entity.id
            override suspend fun apply(activeGateway: StructuredInputGateway) =
                activeGateway.insertEvent(entity)
        }

        data class UpsertProject(val entity: ProjectEntity) : OperationPlan {
            override val targetEntityId: String get() = entity.id
            override suspend fun apply(activeGateway: StructuredInputGateway) =
                activeGateway.insertProject(entity)
        }

        data class UpsertPersonalGoal(val entity: PersonalGoalEntity) : OperationPlan {
            override val targetEntityId: String get() = entity.id
            override suspend fun apply(activeGateway: StructuredInputGateway) =
                activeGateway.insertPersonalGoal(entity)
        }

        data class UpsertTransaction(val entity: TransactionEntity) : OperationPlan {
            override val targetEntityId: String get() = entity.id
            override suspend fun apply(activeGateway: StructuredInputGateway) =
                activeGateway.insertTransaction(entity)
        }
    }

    /**
     * In-memory overlay wrapper used exclusively for dry-run previews so chained references
     * within the same batch validate faithfully with zero writes to Room / audit_history.
     */
    private class PreviewOverlayGateway(
        private val delegate: StructuredInputGateway
    ) : StructuredInputGateway {
        private val recordedOps = linkedMapOf<String, StructuredProcessingState>()
        private val tasks = linkedMapOf<String, TaskEntity>()
        private val subtasks = linkedMapOf<String, SubtaskEntity>()
        private val appointments = linkedMapOf<String, AppointmentEntity>()
        private val events = linkedMapOf<String, EventEntity>()
        private val projects = linkedMapOf<String, ProjectEntity>()
        private val personalGoals = linkedMapOf<String, PersonalGoalEntity>()
        private val transactions = linkedMapOf<String, TransactionEntity>()

        override suspend fun <T> inTransaction(block: suspend () -> T): T = block()

        override suspend fun findRecordedOperationState(operationId: String): StructuredProcessingState? =
            recordedOps[operationId] ?: delegate.findRecordedOperationState(operationId)

        override suspend fun recordOperationOutcome(
            operationId: String,
            state: StructuredProcessingState,
            detail: String,
            occurredAt: Long
        ) {
            if (state == StructuredProcessingState.APPLIED ||
                state == StructuredProcessingState.UPDATED ||
                state == StructuredProcessingState.CONFLICT_PRESERVED_LOCAL
            ) {
                recordedOps[operationId] = state
            }
        }

        override suspend fun categoryExists(id: String): Boolean = delegate.categoryExists(id)
        override suspend fun projectExists(id: String): Boolean = projects.containsKey(id) || delegate.projectExists(id)
        override suspend fun personalGoalExists(id: String): Boolean = personalGoals.containsKey(id) || delegate.personalGoalExists(id)
        override suspend fun eventExists(id: String): Boolean = events.containsKey(id) || delegate.eventExists(id)
        override suspend fun taskExists(id: String): Boolean = tasks.containsKey(id) || delegate.taskExists(id)
        override suspend fun subtaskExists(id: String): Boolean = subtasks.containsKey(id) || delegate.subtaskExists(id)
        override suspend fun appointmentExists(id: String): Boolean = appointments.containsKey(id) || delegate.appointmentExists(id)
        override suspend fun transactionExists(id: String): Boolean = transactions.containsKey(id) || delegate.transactionExists(id)
        override suspend fun taskRecurrenceExists(id: String): Boolean = delegate.taskRecurrenceExists(id)
        override suspend fun creditCardExists(id: String): Boolean = delegate.creditCardExists(id)
        override suspend fun installmentPurchaseExists(id: String): Boolean = delegate.installmentPurchaseExists(id)
        override suspend fun recurringRuleExists(id: String): Boolean = delegate.recurringRuleExists(id)

        override suspend fun getTaskById(id: String): TaskEntity? = tasks[id] ?: delegate.getTaskById(id)
        override suspend fun getSubtaskById(id: String): SubtaskEntity? = subtasks[id] ?: delegate.getSubtaskById(id)
        override suspend fun getSubtasksByTaskId(taskId: String): List<SubtaskEntity> {
            val base = delegate.getSubtasksByTaskId(taskId).associateBy { it.id }.toMutableMap()
            subtasks.values.filter { it.taskId == taskId }.forEach { base[it.id] = it }
            return base.values.sortedWith(compareBy<SubtaskEntity> { it.sortOrder }.thenBy { it.createdAt })
        }

        override suspend fun getAppointmentById(id: String): AppointmentEntity? =
            appointments[id] ?: delegate.getAppointmentById(id)

        override suspend fun getEventById(id: String): EventEntity? =
            events[id] ?: delegate.getEventById(id)

        override suspend fun getProjectById(id: String): ProjectEntity? =
            projects[id] ?: delegate.getProjectById(id)

        override suspend fun getPersonalGoalById(id: String): PersonalGoalEntity? =
            personalGoals[id] ?: delegate.getPersonalGoalById(id)

        override suspend fun getTransactionById(id: String): TransactionEntity? =
            transactions[id] ?: delegate.getTransactionById(id)

        override suspend fun getTaskByRecurrenceAndDate(recurrenceId: String, occurrenceDateIso: String): TaskEntity? =
            tasks.values.firstOrNull { it.recurrenceId == recurrenceId && it.occurrenceDateIso == occurrenceDateIso }
                ?: delegate.getTaskByRecurrenceAndDate(recurrenceId, occurrenceDateIso)

        override suspend fun getStatementById(id: String): CreditCardStatementEntity? = delegate.getStatementById(id)
        override suspend fun getAllStatementsOnce(): List<CreditCardStatementEntity> = delegate.getAllStatementsOnce()

        override suspend fun getTransactionByRecurringOccurrence(ruleId: String, occurrenceIndex: Int): TransactionEntity? =
            transactions.values.firstOrNull { it.recurringRuleId == ruleId && it.occurrenceIndex == occurrenceIndex }
                ?: delegate.getTransactionByRecurringOccurrence(ruleId, occurrenceIndex)

        override suspend fun getTransactionsByInstallmentPurchase(purchaseId: String): List<TransactionEntity> {
            val base = delegate.getTransactionsByInstallmentPurchase(purchaseId).associateBy { it.id }.toMutableMap()
            transactions.values.filter { it.installmentPurchaseId == purchaseId }.forEach { base[it.id] = it }
            return base.values.sortedBy { it.installmentNumber ?: Int.MAX_VALUE }
        }

        override suspend fun insertTask(entity: TaskEntity) { tasks[entity.id] = entity }
        override suspend fun insertSubtask(entity: SubtaskEntity) { subtasks[entity.id] = entity }
        override suspend fun insertSubtasks(entities: List<SubtaskEntity>) {
            entities.forEach { subtasks[it.id] = it }
        }
        override suspend fun insertAppointment(entity: AppointmentEntity) { appointments[entity.id] = entity }
        override suspend fun insertEvent(entity: EventEntity) { events[entity.id] = entity }
        override suspend fun insertProject(entity: ProjectEntity) { projects[entity.id] = entity }
        override suspend fun insertPersonalGoal(entity: PersonalGoalEntity) { personalGoals[entity.id] = entity }
        override suspend fun insertTransaction(entity: TransactionEntity) { transactions[entity.id] = entity }
    }

    suspend fun previewJson(rawJson: String): StructuredBatchPreview {
        val trimmed = rawJson.trim()
        if (trimmed.isEmpty()) {
            return StructuredBatchPreview(
                contractVersion = 0,
                source = "",
                totalOperations = 0,
                willCreateCount = 0,
                willUpdateCount = 0,
                duplicateIgnoredCount = 0,
                conflictPreservedLocalCount = 0,
                rejectedCount = 0,
                items = emptyList(),
                envelopeError = "Conteúdo JSON vazio."
            )
        }
        val envelope = try {
            strictJson.decodeFromString(StructuredInputEnvelope.serializer(), trimmed)
        } catch (ex: Exception) {
            return StructuredBatchPreview(
                contractVersion = 0,
                source = "",
                totalOperations = 0,
                willCreateCount = 0,
                willUpdateCount = 0,
                duplicateIgnoredCount = 0,
                conflictPreservedLocalCount = 0,
                rejectedCount = 0,
                items = emptyList(),
                envelopeError = "JSON estruturado inválido ou incompatível com o contrato: ${ex.message}"
            )
        }
        return previewEnvelope(envelope)
    }

    suspend fun previewEnvelope(envelope: StructuredInputEnvelope): StructuredBatchPreview {
        val envelopeError = validateEnvelopeHeader(envelope)
        if (envelopeError != null) {
            val rejectedItems = envelope.operations.map { op ->
                StructuredPreviewItem(
                    operationId = op.operationId.trim().ifEmpty { "(sem operationId)" },
                    entityTypeRaw = op.entityType,
                    entityType = StructuredEntityType.fromRawOrNull(op.entityType),
                    targetEntityId = null,
                    displayTitle = op.operationId.trim().ifEmpty { op.entityType },
                    outcome = StructuredPreviewOutcome.REJECTED,
                    detail = envelopeError,
                    source = op.source.trim().ifEmpty { envelope.source.trim() },
                    createdAt = if (op.createdAt > 0L) op.createdAt else envelope.createdAt
                )
            }
            return StructuredBatchPreview(
                contractVersion = envelope.contractVersion,
                source = envelope.source.trim(),
                totalOperations = rejectedItems.size,
                willCreateCount = 0,
                willUpdateCount = 0,
                duplicateIgnoredCount = 0,
                conflictPreservedLocalCount = 0,
                rejectedCount = rejectedItems.size,
                items = rejectedItems,
                envelopeError = envelopeError
            )
        }

        if (envelope.operations.isEmpty()) {
            return StructuredBatchPreview(
                contractVersion = envelope.contractVersion,
                source = envelope.source.trim(),
                totalOperations = 0,
                willCreateCount = 0,
                willUpdateCount = 0,
                duplicateIgnoredCount = 0,
                conflictPreservedLocalCount = 0,
                rejectedCount = 0,
                items = emptyList()
            )
        }

        val overlay = PreviewOverlayGateway(gateway)
        val previewItems = envelope.operations.map { op ->
            val opId = op.operationId.trim()
            val effectiveSource = op.source.trim().ifEmpty { envelope.source.trim() }
            val effectiveCreatedAt = when {
                op.createdAt > 0L -> op.createdAt
                envelope.createdAt > 0L -> envelope.createdAt
                else -> clockMillis()
            }
            val headerError = validateOperationHeader(op, envelope, effectiveSource)
            val receipt = if (headerError != null) {
                rejectReceipt(
                    opId = opId.ifEmpty { "(sem operationId)" },
                    operation = op,
                    entityType = StructuredEntityType.fromRawOrNull(op.entityType),
                    effectiveSource = effectiveSource,
                    effectiveCreatedAt = effectiveCreatedAt,
                    reason = headerError
                )
            } else {
                executeSingleInsideTransaction(
                    activeGateway = overlay,
                    operation = op,
                    opId = opId,
                    effectiveSource = effectiveSource,
                    effectiveCreatedAt = effectiveCreatedAt
                )
            }
            receiptToPreviewItem(receipt)
        }

        return StructuredBatchPreview(
            contractVersion = envelope.contractVersion,
            source = envelope.source.trim(),
            totalOperations = previewItems.size,
            willCreateCount = previewItems.count { it.outcome == StructuredPreviewOutcome.WILL_CREATE },
            willUpdateCount = previewItems.count { it.outcome == StructuredPreviewOutcome.WILL_UPDATE },
            duplicateIgnoredCount = previewItems.count { it.outcome == StructuredPreviewOutcome.DUPLICATE_IGNORED },
            conflictPreservedLocalCount = previewItems.count { it.outcome == StructuredPreviewOutcome.CONFLICT_PRESERVED_LOCAL },
            rejectedCount = previewItems.count { it.outcome == StructuredPreviewOutcome.REJECTED },
            items = previewItems
        )
    }

    private fun receiptToPreviewItem(receipt: StructuredOperationReceipt): StructuredPreviewItem {
        val outcome = when (receipt.processingState) {
            StructuredProcessingState.APPLIED -> StructuredPreviewOutcome.WILL_CREATE
            StructuredProcessingState.UPDATED -> StructuredPreviewOutcome.WILL_UPDATE
            StructuredProcessingState.DUPLICATE_IGNORED -> StructuredPreviewOutcome.DUPLICATE_IGNORED
            StructuredProcessingState.CONFLICT_PRESERVED_LOCAL -> StructuredPreviewOutcome.CONFLICT_PRESERVED_LOCAL
            else -> StructuredPreviewOutcome.REJECTED
        }
        val title = receipt.entityTitle?.takeIf { it.isNotBlank() }
            ?: receipt.targetEntityId
            ?: receipt.operationId
        val detail = receipt.rejectionReason ?: receipt.resultDetail.orEmpty()
        return StructuredPreviewItem(
            operationId = receipt.operationId,
            entityTypeRaw = receipt.entityTypeRaw,
            entityType = receipt.entityType,
            targetEntityId = receipt.targetEntityId,
            displayTitle = title,
            outcome = outcome,
            detail = detail,
            checklistItemsCount = receipt.checklistItemsCount,
            source = receipt.source,
            createdAt = receipt.createdAt
        )
    }

    suspend fun processJson(
        rawJson: String,
        atomicBatch: Boolean = false
    ): StructuredBatchResult {
        val trimmed = rawJson.trim()
        if (trimmed.isEmpty()) {
            return StructuredBatchResult(
                contractVersion = 0,
                source = "",
                atomicBatch = atomicBatch,
                totalOperations = 0,
                appliedCount = 0,
                duplicateIgnoredCount = 0,
                conflictPreservedLocalCount = 0,
                rejectedCount = 0,
                failedRolledBackCount = 0,
                receipts = emptyList(),
                envelopeError = "Conteúdo JSON vazio."
            )
        }

        val envelope = try {
            strictJson.decodeFromString(StructuredInputEnvelope.serializer(), trimmed)
        } catch (ex: Exception) {
            return StructuredBatchResult(
                contractVersion = 0,
                source = "",
                atomicBatch = atomicBatch,
                totalOperations = 0,
                appliedCount = 0,
                duplicateIgnoredCount = 0,
                conflictPreservedLocalCount = 0,
                rejectedCount = 0,
                failedRolledBackCount = 0,
                receipts = emptyList(),
                envelopeError = "JSON estruturado inválido ou incompatível com o contrato: ${ex.message}"
            )
        }

        return processEnvelope(envelope, atomicBatch = atomicBatch)
    }

    suspend fun processEnvelope(
        envelope: StructuredInputEnvelope,
        atomicBatch: Boolean = false
    ): StructuredBatchResult {
        val envelopeError = validateEnvelopeHeader(envelope)
        if (envelopeError != null) {
            return rejectEnvelope(
                envelope = envelope,
                atomicBatch = atomicBatch,
                reason = envelopeError
            )
        }
        if (envelope.operations.isEmpty()) {
            return StructuredBatchResult(
                contractVersion = envelope.contractVersion,
                source = envelope.source.trim(),
                atomicBatch = atomicBatch,
                totalOperations = 0,
                appliedCount = 0,
                duplicateIgnoredCount = 0,
                conflictPreservedLocalCount = 0,
                rejectedCount = 0,
                failedRolledBackCount = 0,
                receipts = emptyList()
            )
        }

        return if (atomicBatch) {
            processAtomicallyAsBatch(envelope)
        } else {
            val receipts = envelope.operations.map { op ->
                processSingleOperationSafely(op, envelope)
            }
            buildBatchResult(
                contractVersion = envelope.contractVersion,
                source = envelope.source.trim(),
                atomicBatch = false,
                receipts = receipts
            )
        }
    }

    private fun validateEnvelopeHeader(envelope: StructuredInputEnvelope): String? {
        if (envelope.contractVersion != StructuredInputEnvelope.CURRENT_CONTRACT_VERSION) {
            return "Versão de contrato incompatível (v${envelope.contractVersion}). " +
                "Suportado: v${StructuredInputEnvelope.CURRENT_CONTRACT_VERSION}."
        }
        if (envelope.source.isBlank() && envelope.operations.any { it.source.isBlank() }) {
            return "Campo obrigatório 'source' ausente no envelope e em operação(ões)."
        }
        if (envelope.createdAt < 0L) {
            return "Campo 'createdAt' negativo no envelope."
        }
        return null
    }

    private suspend fun processSingleOperationSafely(
        operation: StructuredInputOperation,
        envelope: StructuredInputEnvelope
    ): StructuredOperationReceipt {
        val opId = operation.operationId.trim()
        val effectiveSource = operation.source.trim().ifEmpty { envelope.source.trim() }
        val effectiveCreatedAt = when {
            operation.createdAt > 0L -> operation.createdAt
            envelope.createdAt > 0L -> envelope.createdAt
            else -> clockMillis()
        }

        val headerError = validateOperationHeader(operation, envelope, effectiveSource)
        if (headerError != null) {
            return StructuredOperationReceipt(
                operationId = opId.ifEmpty { "(sem operationId)" },
                entityTypeRaw = operation.entityType,
                entityType = StructuredEntityType.fromRawOrNull(operation.entityType),
                targetEntityId = null,
                contractVersion = operation.contractVersion,
                source = effectiveSource,
                createdAt = effectiveCreatedAt,
                processingState = StructuredProcessingState.REJECTED,
                rejectionReason = headerError
            )
        }

        return try {
            gateway.inTransaction {
                executeSingleInsideTransaction(
                    activeGateway = gateway,
                    operation = operation,
                    opId = opId,
                    effectiveSource = effectiveSource,
                    effectiveCreatedAt = effectiveCreatedAt
                )
            }
        } catch (ex: Exception) {
            StructuredOperationReceipt(
                operationId = opId,
                entityTypeRaw = operation.entityType,
                entityType = StructuredEntityType.fromRawOrNull(operation.entityType),
                targetEntityId = null,
                contractVersion = operation.contractVersion,
                source = effectiveSource,
                createdAt = effectiveCreatedAt,
                processingState = StructuredProcessingState.FAILED_ROLLED_BACK,
                rejectionReason = "Falha transacional revertida sem alterações parciais: ${ex.message}"
            )
        }
    }

    private suspend fun processAtomicallyAsBatch(
        envelope: StructuredInputEnvelope
    ): StructuredBatchResult {
        return try {
            val receipts = gateway.inTransaction {
                val list = mutableListOf<StructuredOperationReceipt>()
                for (op in envelope.operations) {
                    val opId = op.operationId.trim()
                    val effectiveSource = op.source.trim().ifEmpty { envelope.source.trim() }
                    val effectiveCreatedAt = when {
                        op.createdAt > 0L -> op.createdAt
                        envelope.createdAt > 0L -> envelope.createdAt
                        else -> clockMillis()
                    }
                    val headerError = validateOperationHeader(op, envelope, effectiveSource)
                    if (headerError != null) {
                        val rejected = StructuredOperationReceipt(
                            operationId = opId.ifEmpty { "(sem operationId)" },
                            entityTypeRaw = op.entityType,
                            entityType = StructuredEntityType.fromRawOrNull(op.entityType),
                            targetEntityId = null,
                            contractVersion = op.contractVersion,
                            source = effectiveSource,
                            createdAt = effectiveCreatedAt,
                            processingState = StructuredProcessingState.REJECTED,
                            rejectionReason = headerError
                        )
                        list.add(rejected)
                        throw BatchAbortException(rejected, list.toList())
                    }
                    val receipt = executeSingleInsideTransaction(
                        activeGateway = gateway,
                        operation = op,
                        opId = opId,
                        effectiveSource = effectiveSource,
                        effectiveCreatedAt = effectiveCreatedAt
                    )
                    list.add(receipt)
                    if (receipt.processingState == StructuredProcessingState.REJECTED) {
                        throw BatchAbortException(receipt, list.toList())
                    }
                }
                list
            }
            buildBatchResult(
                contractVersion = envelope.contractVersion,
                source = envelope.source.trim(),
                atomicBatch = true,
                receipts = receipts
            )
        } catch (abort: BatchAbortException) {
            val rolledBackReceipts = envelope.operations.mapIndexed { idx, op ->
                val prior = abort.partialReceipts.getOrNull(idx)
                val opId = op.operationId.trim().ifEmpty { "(sem operationId)" }
                val effectiveSource = op.source.trim().ifEmpty { envelope.source.trim() }
                val effectiveCreatedAt = if (op.createdAt > 0L) op.createdAt else envelope.createdAt
                when {
                    prior == null -> StructuredOperationReceipt(
                        operationId = opId,
                        entityTypeRaw = op.entityType,
                        entityType = StructuredEntityType.fromRawOrNull(op.entityType),
                        targetEntityId = null,
                        contractVersion = op.contractVersion,
                        source = effectiveSource,
                        createdAt = effectiveCreatedAt,
                        processingState = StructuredProcessingState.FAILED_ROLLED_BACK,
                        rejectionReason = "Lote atômico revertido devido a rejeição em '${abort.causeReceipt.operationId}'."
                    )
                    prior.processingState == StructuredProcessingState.REJECTED -> prior
                    else -> prior.copy(
                        processingState = StructuredProcessingState.FAILED_ROLLED_BACK,
                        resultDetail = null,
                        rejectionReason = "Alteração revertida no rollback atômico do lote devido a '${abort.causeReceipt.operationId}'."
                    )
                }
            }
            buildBatchResult(
                contractVersion = envelope.contractVersion,
                source = envelope.source.trim(),
                atomicBatch = true,
                receipts = rolledBackReceipts,
                envelopeError = "Lote atômico revertido integralmente após rejeição na operação '${abort.causeReceipt.operationId}'."
            )
        } catch (ex: Exception) {
            val rolledBackReceipts = envelope.operations.map { op ->
                StructuredOperationReceipt(
                    operationId = op.operationId.trim().ifEmpty { "(sem operationId)" },
                    entityTypeRaw = op.entityType,
                    entityType = StructuredEntityType.fromRawOrNull(op.entityType),
                    targetEntityId = null,
                    contractVersion = op.contractVersion,
                    source = op.source.trim().ifEmpty { envelope.source.trim() },
                    createdAt = if (op.createdAt > 0L) op.createdAt else envelope.createdAt,
                    processingState = StructuredProcessingState.FAILED_ROLLED_BACK,
                    rejectionReason = "Falha de persistência revertida em todo o lote: ${ex.message}"
                )
            }
            buildBatchResult(
                contractVersion = envelope.contractVersion,
                source = envelope.source.trim(),
                atomicBatch = true,
                receipts = rolledBackReceipts,
                envelopeError = "Falha de persistência no lote atômico (nenhuma operação aplicada): ${ex.message}"
            )
        }
    }

    private suspend fun executeSingleInsideTransaction(
        activeGateway: StructuredInputGateway,
        operation: StructuredInputOperation,
        opId: String,
        effectiveSource: String,
        effectiveCreatedAt: Long
    ): StructuredOperationReceipt {
        val entityType = StructuredEntityType.fromRawOrNull(operation.entityType)
            ?: return rejectReceipt(
                opId = opId,
                operation = operation,
                entityType = null,
                effectiveSource = effectiveSource,
                effectiveCreatedAt = effectiveCreatedAt,
                reason = "Tipo de entidade não suportado: '${operation.entityType}'."
            )

        val opAction = StructuredOperationAction.fromRawOrNull(operation.action)
            ?: return rejectReceipt(
                opId = opId,
                operation = operation,
                entityType = entityType,
                effectiveSource = effectiveSource,
                effectiveCreatedAt = effectiveCreatedAt,
                reason = "Ação inválida: '${operation.action}'. Use CREATE, UPDATE ou UPSERT."
            )

        val priorState = activeGateway.findRecordedOperationState(opId)
        if (priorState != null) {
            return StructuredOperationReceipt(
                operationId = opId,
                entityTypeRaw = operation.entityType,
                entityType = entityType,
                targetEntityId = null,
                contractVersion = operation.contractVersion,
                source = effectiveSource,
                createdAt = effectiveCreatedAt,
                processingState = StructuredProcessingState.DUPLICATE_IGNORED,
                resultDetail = "Operação '$opId' já processada anteriormente ($priorState); ignorada com idempotência.",
                actionTaken = "NONE"
            )
        }

        val dataObj = operation.data
            ?: return rejectReceipt(
                opId = opId,
                operation = operation,
                entityType = entityType,
                effectiveSource = effectiveSource,
                effectiveCreatedAt = effectiveCreatedAt,
                reason = "Objeto 'data' ausente na operação '$opId'."
            )

        val now = effectiveCreatedAt

        return when (entityType) {
            StructuredEntityType.TASK -> prepareAndApplyTask(
                activeGateway, opId, operation, opAction, dataObj, effectiveSource, effectiveCreatedAt, now
            )
            StructuredEntityType.CHECKLIST -> prepareAndApplyChecklist(
                activeGateway, opId, operation, opAction, dataObj, effectiveSource, effectiveCreatedAt, now
            )
            StructuredEntityType.APPOINTMENT -> prepareAndApplyAppointment(
                activeGateway, opId, operation, opAction, dataObj, effectiveSource, effectiveCreatedAt, now
            )
            StructuredEntityType.EVENT -> prepareAndApplyEvent(
                activeGateway, opId, operation, opAction, dataObj, effectiveSource, effectiveCreatedAt, now
            )
            StructuredEntityType.PROJECT -> prepareAndApplyProject(
                activeGateway, opId, operation, opAction, dataObj, effectiveSource, effectiveCreatedAt, now
            )
            StructuredEntityType.PERSONAL_GOAL -> prepareAndApplyPersonalGoal(
                activeGateway, opId, operation, opAction, dataObj, effectiveSource, effectiveCreatedAt, now
            )
            StructuredEntityType.TRANSACTION -> prepareAndApplyTransaction(
                activeGateway, opId, operation, opAction, dataObj, effectiveSource, effectiveCreatedAt, now
            )
        }
    }

    private suspend fun prepareAndApplyTask(
        activeGateway: StructuredInputGateway,
        opId: String,
        operation: StructuredInputOperation,
        opAction: StructuredOperationAction,
        dataObj: JsonElement,
        effectiveSource: String,
        effectiveCreatedAt: Long,
        now: Long
    ): StructuredOperationReceipt {
        val dto = decodeData<StructuredTaskData>(dataObj).getOrElse { err ->
            return rejectReceipt(opId, operation, StructuredEntityType.TASK, effectiveSource, effectiveCreatedAt, err)
        }
        val title = dto.title.trim()
        if (title.isEmpty()) {
            return rejectReceipt(opId, operation, StructuredEntityType.TASK, effectiveSource, effectiveCreatedAt, "Título da tarefa é obrigatório.", title)
        }
        val priority = parseEnumStrict<TaskPriority>(dto.priority, "priority")
            ?: return rejectReceipt(opId, operation, StructuredEntityType.TASK, effectiveSource, effectiveCreatedAt, "Prioridade inválida: '${dto.priority}'.", title)
        val status = parseEnumStrict<TaskStatus>(dto.status, "status")
            ?: return rejectReceipt(opId, operation, StructuredEntityType.TASK, effectiveSource, effectiveCreatedAt, "Status de tarefa inválido: '${dto.status}'.", title)

        val entityId = dto.id?.trim()?.takeIf { it.isNotEmpty() } ?: "ing-task-$opId"
        val existingById = activeGateway.getTaskById(entityId)

        val projectId = dto.projectId?.trim()?.takeIf { it.isNotEmpty() }
        if (projectId != null && !activeGateway.projectExists(projectId)) {
            return rejectReceipt(opId, operation, StructuredEntityType.TASK, effectiveSource, effectiveCreatedAt, "Projeto vinculado '$projectId' não existe localmente.", title)
        }
        val categoryId = dto.categoryId?.trim()?.takeIf { it.isNotEmpty() }
        if (categoryId != null && !activeGateway.categoryExists(categoryId)) {
            return rejectReceipt(opId, operation, StructuredEntityType.TASK, effectiveSource, effectiveCreatedAt, "Categoria vinculada '$categoryId' não existe localmente.", title)
        }
        val eventId = dto.eventId?.trim()?.takeIf { it.isNotEmpty() }
        if (eventId != null && !activeGateway.eventExists(eventId)) {
            return rejectReceipt(opId, operation, StructuredEntityType.TASK, effectiveSource, effectiveCreatedAt, "Evento vinculado '$eventId' não existe localmente.", title)
        }
        val goalId = dto.goalId?.trim()?.takeIf { it.isNotEmpty() }
        if (goalId != null && !activeGateway.personalGoalExists(goalId)) {
            return rejectReceipt(opId, operation, StructuredEntityType.TASK, effectiveSource, effectiveCreatedAt, "Objetivo vinculado '$goalId' não existe localmente.", title)
        }
        val recurrenceId = dto.recurrenceId?.trim()?.takeIf { it.isNotEmpty() }
        if (recurrenceId != null && !activeGateway.taskRecurrenceExists(recurrenceId)) {
            return rejectReceipt(opId, operation, StructuredEntityType.TASK, effectiveSource, effectiveCreatedAt, "Série recorrente '$recurrenceId' não existe localmente.", title)
        }
        val occurrenceDateIso = dto.occurrenceDateIso?.trim()?.takeIf { it.isNotEmpty() }
        val existingByRecurrence = if (existingById == null && recurrenceId != null && occurrenceDateIso != null) {
            activeGateway.getTaskByRecurrenceAndDate(recurrenceId, occurrenceDateIso)
        } else {
            null
        }
        val existing = existingById ?: existingByRecurrence

        when {
            existing != null && opAction == StructuredOperationAction.CREATE -> {
                val msg = if (existingById != null) {
                    "Tarefa '$entityId' já existe localmente; versão local preservada."
                } else {
                    "Ocorrência ($recurrenceId, $occurrenceDateIso) já existe localmente; versão local preservada."
                }
                return recordLocalConflict(
                    activeGateway, opId, operation, StructuredEntityType.TASK, existing.id, effectiveSource, effectiveCreatedAt, now, msg, existing.title
                )
            }
            existing == null && opAction == StructuredOperationAction.UPDATE -> {
                return rejectReceipt(
                    opId, operation, StructuredEntityType.TASK, effectiveSource, effectiveCreatedAt,
                    "Tarefa '$entityId' não encontrada localmente para atualização.", title
                )
            }
        }

        val resolvedTaskId = existing?.id ?: entityId
        val existingSubtasks = activeGateway.getSubtasksByTaskId(resolvedTaskId)
        val builtSubtasks = buildSubtaskEntities(
            taskId = resolvedTaskId,
            items = dto.subtasks,
            existingSubtasks = existingSubtasks,
            now = now
        ).getOrElse { err ->
            return rejectReceipt(opId, operation, StructuredEntityType.TASK, effectiveSource, effectiveCreatedAt, err, title)
        }

        val entity = TaskEntity(
            id = resolvedTaskId,
            title = title,
            description = dto.description.trim(),
            projectId = projectId,
            categoryId = categoryId,
            eventId = eventId,
            goalId = goalId,
            priority = priority,
            status = status,
            dueAt = dto.dueAt,
            hasDueTime = dto.hasDueTime,
            notes = dto.notes.trim(),
            completedAt = if (status == TaskStatus.COMPLETED) (dto.completedAt ?: existing?.completedAt ?: now) else null,
            recurrenceId = recurrenceId,
            occurrenceDateIso = occurrenceDateIso,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now
        )
        val isUpdate = existing != null
        val checklistSuffix = if (builtSubtasks.isNotEmpty()) " (${builtSubtasks.size} item(ns) no checklist)" else ""
        val detail = if (isUpdate) {
            "Tarefa '$title' atualizada via entrada estruturada$checklistSuffix."
        } else {
            "Tarefa '$title' criada via entrada estruturada$checklistSuffix."
        }
        return applyPlan(
            activeGateway = activeGateway,
            opId = opId,
            operation = operation,
            entityType = StructuredEntityType.TASK,
            plan = OperationPlan.UpsertTaskWithSubtasks(entity, builtSubtasks),
            effectiveSource = effectiveSource,
            effectiveCreatedAt = effectiveCreatedAt,
            now = now,
            detail = detail,
            isUpdate = isUpdate,
            entityTitle = title,
            checklistItemsCount = builtSubtasks.size
        )
    }

    private suspend fun prepareAndApplyChecklist(
        activeGateway: StructuredInputGateway,
        opId: String,
        operation: StructuredInputOperation,
        opAction: StructuredOperationAction,
        dataObj: JsonElement,
        effectiveSource: String,
        effectiveCreatedAt: Long,
        now: Long
    ): StructuredOperationReceipt {
        val dto = decodeData<StructuredChecklistData>(dataObj).getOrElse { err ->
            return rejectReceipt(opId, operation, StructuredEntityType.CHECKLIST, effectiveSource, effectiveCreatedAt, err)
        }
        val taskId = dto.taskId.trim()
        if (taskId.isEmpty()) {
            return rejectReceipt(opId, operation, StructuredEntityType.CHECKLIST, effectiveSource, effectiveCreatedAt, "Campo 'taskId' é obrigatório para checklist.")
        }
        val parentTask = activeGateway.getTaskById(taskId)
            ?: return rejectReceipt(opId, operation, StructuredEntityType.CHECKLIST, effectiveSource, effectiveCreatedAt, "Tarefa vinculada '$taskId' não existe localmente.")

        val requestedItems = when {
            dto.items.isNotEmpty() -> dto.items
            !dto.title.isNullOrBlank() -> listOf(
                StructuredSubtaskItem(
                    id = dto.id,
                    title = dto.title,
                    isCompleted = dto.isCompleted,
                    sortOrder = dto.sortOrder
                )
            )
            else -> emptyList()
        }
        if (requestedItems.isEmpty()) {
            return rejectReceipt(
                opId, operation, StructuredEntityType.CHECKLIST, effectiveSource, effectiveCreatedAt,
                "Informe 'items' ou 'title' para o checklist da tarefa '${parentTask.title}'.",
                parentTask.title
            )
        }

        val existingSubtasks = activeGateway.getSubtasksByTaskId(taskId)
        val existingById = existingSubtasks.associateBy { it.id }
        val existingByTitle = existingSubtasks.associateBy { it.title.trim().lowercase() }
        val nextSortBase = (existingSubtasks.maxOfOrNull { it.sortOrder } ?: -1) + 1

        val resolvedEntities = mutableListOf<SubtaskEntity>()
        var updatedExistingCount = 0
        var createdNewCount = 0

        for ((index, item) in requestedItems.withIndex()) {
            val cleanTitle = item.title.trim()
            if (cleanTitle.isEmpty()) {
                return rejectReceipt(
                    opId, operation, StructuredEntityType.CHECKLIST, effectiveSource, effectiveCreatedAt,
                    "Todos os itens do checklist precisam de título não vazio.",
                    parentTask.title
                )
            }
            val explicitId = item.id?.trim()?.takeIf { it.isNotEmpty() }
            val matchedExisting = when {
                explicitId != null -> existingById[explicitId] ?: activeGateway.getSubtaskById(explicitId)
                opAction != StructuredOperationAction.CREATE -> existingByTitle[cleanTitle.lowercase()]
                else -> null
            }

            if (matchedExisting != null && matchedExisting.taskId != taskId) {
                return rejectReceipt(
                    opId, operation, StructuredEntityType.CHECKLIST, effectiveSource, effectiveCreatedAt,
                    "Subtarefa '${matchedExisting.id}' pertence a outra tarefa (${matchedExisting.taskId}).",
                    parentTask.title
                )
            }

            if (matchedExisting != null && opAction == StructuredOperationAction.CREATE) {
                return recordLocalConflict(
                    activeGateway, opId, operation, StructuredEntityType.CHECKLIST, matchedExisting.id,
                    effectiveSource, effectiveCreatedAt, now,
                    "Item de checklist '${matchedExisting.title}' já existe localmente na tarefa '${parentTask.title}'; versão local preservada.",
                    parentTask.title
                )
            }
            if (matchedExisting == null && opAction == StructuredOperationAction.UPDATE) {
                return rejectReceipt(
                    opId, operation, StructuredEntityType.CHECKLIST, effectiveSource, effectiveCreatedAt,
                    "Item de checklist '$cleanTitle' não encontrado localmente na tarefa '${parentTask.title}' para atualização.",
                    parentTask.title
                )
            }

            val resolvedId = matchedExisting?.id ?: explicitId ?: "ing-sub-$opId-$index"
            val resolvedOrder = item.sortOrder ?: matchedExisting?.sortOrder ?: (nextSortBase + index)
            if (matchedExisting != null) updatedExistingCount++ else createdNewCount++

            resolvedEntities.add(
                SubtaskEntity(
                    id = resolvedId,
                    taskId = taskId,
                    title = cleanTitle,
                    isCompleted = item.isCompleted,
                    sortOrder = resolvedOrder,
                    createdAt = matchedExisting?.createdAt ?: now,
                    updatedAt = now
                )
            )
        }

        // A checklist operation always changes the checklist of an existing parent task.
        // Even when every item is new, the affected domain entity is the existing task,
        // so previews and processing history must classify this as an update.
        val isUpdate = true
        val label = if (resolvedEntities.size == 1) {
            "${resolvedEntities.first().title} (${parentTask.title})"
        } else {
            "${resolvedEntities.size} itens em ${parentTask.title}"
        }
        val detail = if (isUpdate) {
            "Checklist de '${parentTask.title}' atualizado ($updatedExistingCount atualizado(s), $createdNewCount adicionado(s))."
        } else {
            "Checklist de '${parentTask.title}' adicionado ($createdNewCount item(ns))."
        }

        return applyPlan(
            activeGateway = activeGateway,
            opId = opId,
            operation = operation,
            entityType = StructuredEntityType.CHECKLIST,
            plan = OperationPlan.UpsertChecklist(taskId = taskId, subtasks = resolvedEntities),
            effectiveSource = effectiveSource,
            effectiveCreatedAt = effectiveCreatedAt,
            now = now,
            detail = detail,
            isUpdate = isUpdate,
            entityTitle = label,
            checklistItemsCount = resolvedEntities.size
        )
    }

    private fun buildSubtaskEntities(
        taskId: String,
        items: List<StructuredSubtaskItem>,
        existingSubtasks: List<SubtaskEntity>,
        now: Long
    ): Result<List<SubtaskEntity>> {
        if (items.isEmpty()) return Result.success(emptyList())
        val existingById = existingSubtasks.associateBy { it.id }
        val existingByTitle = existingSubtasks.associateBy { it.title.trim().lowercase() }
        val nextSortBase = (existingSubtasks.maxOfOrNull { it.sortOrder } ?: -1) + 1

        val entities = mutableListOf<SubtaskEntity>()
        items.forEachIndexed { index, item ->
            val cleanTitle = item.title.trim()
            if (cleanTitle.isEmpty()) {
                return Result.failure(IllegalArgumentException("Item #${index + 1} do checklist está sem título."))
            }
            val explicitId = item.id?.trim()?.takeIf { it.isNotEmpty() }
            val existing = explicitId?.let { existingById[it] } ?: existingByTitle[cleanTitle.lowercase()]
            val resolvedId = existing?.id ?: explicitId ?: "ing-sub-$taskId-$index"
            entities.add(
                SubtaskEntity(
                    id = resolvedId,
                    taskId = taskId,
                    title = cleanTitle,
                    isCompleted = item.isCompleted,
                    sortOrder = item.sortOrder ?: existing?.sortOrder ?: (nextSortBase + index),
                    createdAt = existing?.createdAt ?: now,
                    updatedAt = now
                )
            )
        }
        return Result.success(entities)
    }

    private suspend fun prepareAndApplyAppointment(
        activeGateway: StructuredInputGateway,
        opId: String,
        operation: StructuredInputOperation,
        opAction: StructuredOperationAction,
        dataObj: JsonElement,
        effectiveSource: String,
        effectiveCreatedAt: Long,
        now: Long
    ): StructuredOperationReceipt {
        val dto = decodeData<StructuredAppointmentData>(dataObj).getOrElse { err ->
            return rejectReceipt(opId, operation, StructuredEntityType.APPOINTMENT, effectiveSource, effectiveCreatedAt, err)
        }
        val title = dto.title.trim()
        if (title.isEmpty()) {
            return rejectReceipt(opId, operation, StructuredEntityType.APPOINTMENT, effectiveSource, effectiveCreatedAt, "Título do compromisso é obrigatório.")
        }
        if (dto.startAt <= 0L || dto.endAt < dto.startAt) {
            return rejectReceipt(opId, operation, StructuredEntityType.APPOINTMENT, effectiveSource, effectiveCreatedAt, "Intervalo de datas inválido no compromisso (startAt/endAt).", title)
        }
        val entityId = dto.id?.trim()?.takeIf { it.isNotEmpty() } ?: "ing-appt-$opId"
        val existing = activeGateway.getAppointmentById(entityId)
        when {
            existing != null && opAction == StructuredOperationAction.CREATE ->
                return recordLocalConflict(activeGateway, opId, operation, StructuredEntityType.APPOINTMENT, entityId, effectiveSource, effectiveCreatedAt, now, "Compromisso '$entityId' já existe localmente; versão local preservada.", existing.title)
            existing == null && opAction == StructuredOperationAction.UPDATE ->
                return rejectReceipt(opId, operation, StructuredEntityType.APPOINTMENT, effectiveSource, effectiveCreatedAt, "Compromisso '$entityId' não encontrado localmente para atualização.", title)
        }

        val categoryId = dto.categoryId?.trim()?.takeIf { it.isNotEmpty() }
        if (categoryId != null && !activeGateway.categoryExists(categoryId)) {
            return rejectReceipt(opId, operation, StructuredEntityType.APPOINTMENT, effectiveSource, effectiveCreatedAt, "Categoria vinculada '$categoryId' não existe localmente.", title)
        }
        val eventId = dto.eventId?.trim()?.takeIf { it.isNotEmpty() }
        if (eventId != null && !activeGateway.eventExists(eventId)) {
            return rejectReceipt(opId, operation, StructuredEntityType.APPOINTMENT, effectiveSource, effectiveCreatedAt, "Evento vinculado '$eventId' não existe localmente.", title)
        }

        val entity = AppointmentEntity(
            id = entityId,
            title = title,
            description = dto.description.trim(),
            categoryId = categoryId,
            eventId = eventId,
            startAt = dto.startAt,
            endAt = dto.endAt,
            isAllDay = dto.isAllDay,
            location = dto.location.trim(),
            link = dto.link.trim(),
            createdAt = existing?.createdAt ?: now,
            updatedAt = now
        )
        val isUpdate = existing != null
        val detail = if (isUpdate) {
            "Compromisso '$title' atualizado via entrada estruturada."
        } else {
            "Compromisso '$title' criado via entrada estruturada."
        }
        return applyPlan(
            activeGateway = activeGateway,
            opId = opId,
            operation = operation,
            entityType = StructuredEntityType.APPOINTMENT,
            plan = OperationPlan.UpsertAppointment(entity),
            effectiveSource = effectiveSource,
            effectiveCreatedAt = effectiveCreatedAt,
            now = now,
            detail = detail,
            isUpdate = isUpdate,
            entityTitle = title
        )
    }

    private suspend fun prepareAndApplyEvent(
        activeGateway: StructuredInputGateway,
        opId: String,
        operation: StructuredInputOperation,
        opAction: StructuredOperationAction,
        dataObj: JsonElement,
        effectiveSource: String,
        effectiveCreatedAt: Long,
        now: Long
    ): StructuredOperationReceipt {
        val dto = decodeData<StructuredEventData>(dataObj).getOrElse { err ->
            return rejectReceipt(opId, operation, StructuredEntityType.EVENT, effectiveSource, effectiveCreatedAt, err)
        }
        val name = dto.name.trim()
        if (name.isEmpty()) {
            return rejectReceipt(opId, operation, StructuredEntityType.EVENT, effectiveSource, effectiveCreatedAt, "Nome do evento é obrigatório.")
        }
        if (dto.startAt <= 0L || dto.endAt < dto.startAt) {
            return rejectReceipt(opId, operation, StructuredEntityType.EVENT, effectiveSource, effectiveCreatedAt, "Intervalo de datas inválido no evento (startAt/endAt).", name)
        }
        if (dto.ticketPriceCents < 0L || dto.ticketFeesCents < 0L || dto.ticketTotalCents < 0L || dto.ticketQuantity < 1 || dto.installmentsCount < 1) {
            return rejectReceipt(opId, operation, StructuredEntityType.EVENT, effectiveSource, effectiveCreatedAt, "Valores de ingresso/parcelas inválidos no evento.", name)
        }

        val eventStatus = parseEnumStrict<EventLifecycleStatus>(dto.eventStatus, "eventStatus")
            ?: return rejectReceipt(opId, operation, StructuredEntityType.EVENT, effectiveSource, effectiveCreatedAt, "eventStatus inválido: '${dto.eventStatus}'.", name)
        val ticketStatus = parseEnumStrict<TicketStatus>(dto.ticketStatus, "ticketStatus")
            ?: return rejectReceipt(opId, operation, StructuredEntityType.EVENT, effectiveSource, effectiveCreatedAt, "ticketStatus inválido: '${dto.ticketStatus}'.", name)
        val paymentMethod = parseEnumStrict<PaymentMethod>(dto.paymentMethod, "paymentMethod")
            ?: return rejectReceipt(opId, operation, StructuredEntityType.EVENT, effectiveSource, effectiveCreatedAt, "paymentMethod inválido: '${dto.paymentMethod}'.", name)

        val entityId = dto.id?.trim()?.takeIf { it.isNotEmpty() } ?: "ing-event-$opId"
        val existing = activeGateway.getEventById(entityId)
        when {
            existing != null && opAction == StructuredOperationAction.CREATE ->
                return recordLocalConflict(activeGateway, opId, operation, StructuredEntityType.EVENT, entityId, effectiveSource, effectiveCreatedAt, now, "Evento '$entityId' já existe localmente; versão local preservada.", existing.name)
            existing == null && opAction == StructuredOperationAction.UPDATE ->
                return rejectReceipt(opId, operation, StructuredEntityType.EVENT, effectiveSource, effectiveCreatedAt, "Evento '$entityId' não encontrado localmente para atualização.", name)
        }

        val categoryId = dto.categoryId?.trim()?.takeIf { it.isNotEmpty() }
        if (categoryId != null && !activeGateway.categoryExists(categoryId)) {
            return rejectReceipt(opId, operation, StructuredEntityType.EVENT, effectiveSource, effectiveCreatedAt, "Categoria vinculada '$categoryId' não existe localmente.", name)
        }
        val creditCardId = dto.creditCardId?.trim()?.takeIf { it.isNotEmpty() }
        if (creditCardId != null && !activeGateway.creditCardExists(creditCardId)) {
            return rejectReceipt(opId, operation, StructuredEntityType.EVENT, effectiveSource, effectiveCreatedAt, "Cartão vinculado '$creditCardId' não existe localmente.", name)
        }

        val entity = EventEntity(
            id = entityId,
            name = name,
            description = dto.description.trim(),
            categoryId = categoryId,
            startAt = dto.startAt,
            endAt = dto.endAt,
            isAllDay = dto.isAllDay,
            officialLink = dto.officialLink.trim(),
            ticketLink = dto.ticketLink.trim(),
            locationName = dto.locationName.trim(),
            address = dto.address.trim(),
            city = dto.city.trim(),
            state = dto.state.trim(),
            notes = dto.notes.trim(),
            eventStatus = eventStatus,
            ticketStatus = ticketStatus,
            ticketPriceCents = dto.ticketPriceCents,
            ticketQuantity = dto.ticketQuantity,
            ticketFeesCents = dto.ticketFeesCents,
            ticketTotalCents = dto.ticketTotalCents,
            ticketPurchaseDate = dto.ticketPurchaseDate,
            ticketDueDate = dto.ticketDueDate,
            paymentMethod = paymentMethod,
            creditCardId = creditCardId,
            installmentsCount = dto.installmentsCount,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now
        )
        val isUpdate = existing != null
        val detail = if (isUpdate) {
            "Evento '$name' atualizado via entrada estruturada."
        } else {
            "Evento '$name' criado via entrada estruturada."
        }
        return applyPlan(
            activeGateway = activeGateway,
            opId = opId,
            operation = operation,
            entityType = StructuredEntityType.EVENT,
            plan = OperationPlan.UpsertEvent(entity),
            effectiveSource = effectiveSource,
            effectiveCreatedAt = effectiveCreatedAt,
            now = now,
            detail = detail,
            isUpdate = isUpdate,
            entityTitle = name
        )
    }

    private suspend fun prepareAndApplyProject(
        activeGateway: StructuredInputGateway,
        opId: String,
        operation: StructuredInputOperation,
        opAction: StructuredOperationAction,
        dataObj: JsonElement,
        effectiveSource: String,
        effectiveCreatedAt: Long,
        now: Long
    ): StructuredOperationReceipt {
        val dto = decodeData<StructuredProjectData>(dataObj).getOrElse { err ->
            return rejectReceipt(opId, operation, StructuredEntityType.PROJECT, effectiveSource, effectiveCreatedAt, err)
        }
        val name = dto.name.trim()
        if (name.isEmpty()) {
            return rejectReceipt(opId, operation, StructuredEntityType.PROJECT, effectiveSource, effectiveCreatedAt, "Nome do projeto é obrigatório.")
        }
        val status = parseEnumStrict<ProjectStatus>(dto.status, "status")
            ?: return rejectReceipt(opId, operation, StructuredEntityType.PROJECT, effectiveSource, effectiveCreatedAt, "Status de projeto inválido: '${dto.status}'.", name)
        val priority = parseEnumStrict<TaskPriority>(dto.priority, "priority")
            ?: return rejectReceipt(opId, operation, StructuredEntityType.PROJECT, effectiveSource, effectiveCreatedAt, "Prioridade de projeto inválida: '${dto.priority}'.", name)

        val entityId = dto.id?.trim()?.takeIf { it.isNotEmpty() } ?: "ing-proj-$opId"
        val existing = activeGateway.getProjectById(entityId)
        when {
            existing != null && opAction == StructuredOperationAction.CREATE ->
                return recordLocalConflict(activeGateway, opId, operation, StructuredEntityType.PROJECT, entityId, effectiveSource, effectiveCreatedAt, now, "Projeto '$entityId' já existe localmente; versão local preservada.", existing.name)
            existing == null && opAction == StructuredOperationAction.UPDATE ->
                return rejectReceipt(opId, operation, StructuredEntityType.PROJECT, effectiveSource, effectiveCreatedAt, "Projeto '$entityId' não encontrado localmente para atualização.", name)
        }

        val goalId = dto.goalId?.trim()?.takeIf { it.isNotEmpty() }
        if (goalId != null && !activeGateway.personalGoalExists(goalId)) {
            return rejectReceipt(opId, operation, StructuredEntityType.PROJECT, effectiveSource, effectiveCreatedAt, "Objetivo pessoal vinculado '$goalId' não existe localmente.", name)
        }

        val entity = ProjectEntity(
            id = entityId,
            name = name,
            description = dto.description.trim(),
            deadlineAt = dto.deadlineAt,
            status = status,
            priority = priority,
            goalId = goalId,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now
        )
        val isUpdate = existing != null
        val detail = if (isUpdate) {
            "Projeto '$name' atualizado via entrada estruturada."
        } else {
            "Projeto '$name' criado via entrada estruturada."
        }
        return applyPlan(
            activeGateway = activeGateway,
            opId = opId,
            operation = operation,
            entityType = StructuredEntityType.PROJECT,
            plan = OperationPlan.UpsertProject(entity),
            effectiveSource = effectiveSource,
            effectiveCreatedAt = effectiveCreatedAt,
            now = now,
            detail = detail,
            isUpdate = isUpdate,
            entityTitle = name
        )
    }

    private suspend fun prepareAndApplyPersonalGoal(
        activeGateway: StructuredInputGateway,
        opId: String,
        operation: StructuredInputOperation,
        opAction: StructuredOperationAction,
        dataObj: JsonElement,
        effectiveSource: String,
        effectiveCreatedAt: Long,
        now: Long
    ): StructuredOperationReceipt {
        val dto = decodeData<StructuredPersonalGoalData>(dataObj).getOrElse { err ->
            return rejectReceipt(opId, operation, StructuredEntityType.PERSONAL_GOAL, effectiveSource, effectiveCreatedAt, err)
        }
        val title = dto.title.trim()
        if (title.isEmpty()) {
            return rejectReceipt(opId, operation, StructuredEntityType.PERSONAL_GOAL, effectiveSource, effectiveCreatedAt, "Título do objetivo pessoal é obrigatório.")
        }
        if (dto.manualProgressPercent !in 0..100) {
            return rejectReceipt(opId, operation, StructuredEntityType.PERSONAL_GOAL, effectiveSource, effectiveCreatedAt, "manualProgressPercent deve estar entre 0 e 100.", title)
        }
        val status = parseEnumStrict<GoalStatus>(dto.status, "status")
            ?: return rejectReceipt(opId, operation, StructuredEntityType.PERSONAL_GOAL, effectiveSource, effectiveCreatedAt, "Status do objetivo inválido: '${dto.status}'.", title)
        val progressMode = parseEnumStrict<GoalProgressMode>(dto.progressMode, "progressMode")
            ?: return rejectReceipt(opId, operation, StructuredEntityType.PERSONAL_GOAL, effectiveSource, effectiveCreatedAt, "progressMode inválido: '${dto.progressMode}'.", title)

        val entityId = dto.id?.trim()?.takeIf { it.isNotEmpty() } ?: "ing-goal-$opId"
        val existing = activeGateway.getPersonalGoalById(entityId)
        when {
            existing != null && opAction == StructuredOperationAction.CREATE ->
                return recordLocalConflict(activeGateway, opId, operation, StructuredEntityType.PERSONAL_GOAL, entityId, effectiveSource, effectiveCreatedAt, now, "Objetivo pessoal '$entityId' já existe localmente; versão local preservada.", existing.title)
            existing == null && opAction == StructuredOperationAction.UPDATE ->
                return rejectReceipt(opId, operation, StructuredEntityType.PERSONAL_GOAL, effectiveSource, effectiveCreatedAt, "Objetivo pessoal '$entityId' não encontrado localmente para atualização.", title)
        }

        val categoryId = dto.categoryId?.trim()?.takeIf { it.isNotEmpty() }
        if (categoryId != null && !activeGateway.categoryExists(categoryId)) {
            return rejectReceipt(opId, operation, StructuredEntityType.PERSONAL_GOAL, effectiveSource, effectiveCreatedAt, "Categoria vinculada '$categoryId' não existe localmente.", title)
        }

        val entity = PersonalGoalEntity(
            id = entityId,
            title = title,
            description = dto.description.trim(),
            categoryId = categoryId,
            targetDateAt = dto.targetDateAt,
            status = status,
            progressMode = progressMode,
            manualProgressPercent = dto.manualProgressPercent,
            completedAt = if (status == GoalStatus.COMPLETED) (dto.completedAt ?: existing?.completedAt ?: now) else null,
            notes = dto.notes.trim(),
            createdAt = existing?.createdAt ?: now,
            updatedAt = now
        )
        val isUpdate = existing != null
        val detail = if (isUpdate) {
            "Objetivo pessoal '$title' atualizado via entrada estruturada."
        } else {
            "Objetivo pessoal '$title' criado via entrada estruturada."
        }
        return applyPlan(
            activeGateway = activeGateway,
            opId = opId,
            operation = operation,
            entityType = StructuredEntityType.PERSONAL_GOAL,
            plan = OperationPlan.UpsertPersonalGoal(entity),
            effectiveSource = effectiveSource,
            effectiveCreatedAt = effectiveCreatedAt,
            now = now,
            detail = detail,
            isUpdate = isUpdate,
            entityTitle = title
        )
    }

    private suspend fun prepareAndApplyTransaction(
        activeGateway: StructuredInputGateway,
        opId: String,
        operation: StructuredInputOperation,
        opAction: StructuredOperationAction,
        dataObj: JsonElement,
        effectiveSource: String,
        effectiveCreatedAt: Long,
        now: Long
    ): StructuredOperationReceipt {
        val dto = decodeData<StructuredTransactionData>(dataObj).getOrElse { err ->
            return rejectReceipt(opId, operation, StructuredEntityType.TRANSACTION, effectiveSource, effectiveCreatedAt, err)
        }
        val desc = dto.description.trim()
        if (desc.isEmpty()) {
            return rejectReceipt(opId, operation, StructuredEntityType.TRANSACTION, effectiveSource, effectiveCreatedAt, "Descrição do lançamento financeiro é obrigatória.")
        }
        if (dto.amountCents <= 0L) {
            return rejectReceipt(opId, operation, StructuredEntityType.TRANSACTION, effectiveSource, effectiveCreatedAt, "Valor financeiro (amountCents) deve ser maior que zero.", desc)
        }
        if (dto.occurredAt <= 0L) {
            return rejectReceipt(opId, operation, StructuredEntityType.TRANSACTION, effectiveSource, effectiveCreatedAt, "Data de ocorrência (occurredAt) obrigatória e positiva.", desc)
        }
        val type = parseEnumStrict<TransactionType>(dto.type, "type")
            ?: return rejectReceipt(opId, operation, StructuredEntityType.TRANSACTION, effectiveSource, effectiveCreatedAt, "Tipo financeiro inválido: '${dto.type}'.", desc)
        val status = parseEnumStrict<FinancialStatus>(dto.status, "status")
            ?: return rejectReceipt(opId, operation, StructuredEntityType.TRANSACTION, effectiveSource, effectiveCreatedAt, "Status financeiro inválido: '${dto.status}'.", desc)
        val paymentMethod = parseEnumStrict<PaymentMethod>(dto.paymentMethod, "paymentMethod")
            ?: return rejectReceipt(opId, operation, StructuredEntityType.TRANSACTION, effectiveSource, effectiveCreatedAt, "Método de pagamento inválido: '${dto.paymentMethod}'.", desc)
        val eventCostType = dto.eventCostType?.trim()?.takeIf { it.isNotEmpty() }?.let { raw ->
            parseEnumStrict<EventCostType>(raw, "eventCostType")
                ?: return rejectReceipt(opId, operation, StructuredEntityType.TRANSACTION, effectiveSource, effectiveCreatedAt, "eventCostType inválido: '$raw'.", desc)
        }

        if (dto.installmentNumber != null && dto.installmentNumber < 1) {
            return rejectReceipt(opId, operation, StructuredEntityType.TRANSACTION, effectiveSource, effectiveCreatedAt, "Número da parcela inválido (${dto.installmentNumber}).", desc)
        }
        if (dto.installmentsTotal != null && dto.installmentsTotal < 1) {
            return rejectReceipt(opId, operation, StructuredEntityType.TRANSACTION, effectiveSource, effectiveCreatedAt, "Total de parcelas inválido (${dto.installmentsTotal}).", desc)
        }
        if (dto.installmentNumber != null && dto.installmentsTotal != null && dto.installmentNumber > dto.installmentsTotal) {
            return rejectReceipt(opId, operation, StructuredEntityType.TRANSACTION, effectiveSource, effectiveCreatedAt, "Parcela ${dto.installmentNumber}/${dto.installmentsTotal} inconsistente.", desc)
        }

        val entityId = dto.id?.trim()?.takeIf { it.isNotEmpty() } ?: "ing-tx-$opId"
        val existing = activeGateway.getTransactionById(entityId)
        when {
            existing != null && opAction == StructuredOperationAction.CREATE ->
                return recordLocalConflict(activeGateway, opId, operation, StructuredEntityType.TRANSACTION, entityId, effectiveSource, effectiveCreatedAt, now, "Transação '$entityId' já existe localmente; versão local preservada.", existing.description)
            existing == null && opAction == StructuredOperationAction.UPDATE ->
                return rejectReceipt(opId, operation, StructuredEntityType.TRANSACTION, effectiveSource, effectiveCreatedAt, "Transação '$entityId' não encontrada localmente para atualização.", desc)
        }

        val categoryId = dto.categoryId?.trim()?.takeIf { it.isNotEmpty() }
        if (categoryId != null && !activeGateway.categoryExists(categoryId)) {
            return rejectReceipt(opId, operation, StructuredEntityType.TRANSACTION, effectiveSource, effectiveCreatedAt, "Categoria vinculada '$categoryId' não existe localmente.", desc)
        }
        val projectId = dto.projectId?.trim()?.takeIf { it.isNotEmpty() }
        if (projectId != null && !activeGateway.projectExists(projectId)) {
            return rejectReceipt(opId, operation, StructuredEntityType.TRANSACTION, effectiveSource, effectiveCreatedAt, "Projeto vinculado '$projectId' não existe localmente.", desc)
        }
        val eventId = dto.eventId?.trim()?.takeIf { it.isNotEmpty() }
        if (eventId != null && !activeGateway.eventExists(eventId)) {
            return rejectReceipt(opId, operation, StructuredEntityType.TRANSACTION, effectiveSource, effectiveCreatedAt, "Evento vinculado '$eventId' não existe localmente.", desc)
        }
        val taskId = dto.taskId?.trim()?.takeIf { it.isNotEmpty() }
        if (taskId != null && !activeGateway.taskExists(taskId)) {
            return rejectReceipt(opId, operation, StructuredEntityType.TRANSACTION, effectiveSource, effectiveCreatedAt, "Tarefa vinculada '$taskId' não existe localmente.", desc)
        }

        val creditCardId = dto.creditCardId?.trim()?.takeIf { it.isNotEmpty() }
        if (creditCardId != null && !activeGateway.creditCardExists(creditCardId)) {
            return rejectReceipt(opId, operation, StructuredEntityType.TRANSACTION, effectiveSource, effectiveCreatedAt, "Cartão vinculado '$creditCardId' não existe localmente.", desc)
        }

        val statementId = dto.statementId?.trim()?.takeIf { it.isNotEmpty() }
        val statement = if (statementId != null) {
            activeGateway.getStatementById(statementId)
                ?: return rejectReceipt(opId, operation, StructuredEntityType.TRANSACTION, effectiveSource, effectiveCreatedAt, "Fatura vinculada '$statementId' não existe localmente.", desc)
        } else {
            null
        }
        if (statement != null) {
            if (creditCardId != null && statement.creditCardId != creditCardId) {
                return rejectReceipt(opId, operation, StructuredEntityType.TRANSACTION, effectiveSource, effectiveCreatedAt, "Fatura '$statementId' pertence a outro cartão (${statement.creditCardId}).", desc)
            }
            if (statement.status == StatementStatus.PAID && status != FinancialStatus.SETTLED) {
                return rejectReceipt(opId, operation, StructuredEntityType.TRANSACTION, effectiveSource, effectiveCreatedAt, "Lançamento não quitado bloqueado em fatura já paga ($statementId).", desc)
            }
        }

        val installmentPurchaseId = dto.installmentPurchaseId?.trim()?.takeIf { it.isNotEmpty() }
        if (installmentPurchaseId != null) {
            if (!activeGateway.installmentPurchaseExists(installmentPurchaseId)) {
                return rejectReceipt(opId, operation, StructuredEntityType.TRANSACTION, effectiveSource, effectiveCreatedAt, "Parcelamento '$installmentPurchaseId' não existe localmente.", desc)
            }
            if (dto.installmentNumber != null && existing == null) {
                val existingInstallment = activeGateway.getTransactionsByInstallmentPurchase(installmentPurchaseId)
                    .firstOrNull { it.installmentNumber == dto.installmentNumber }
                if (existingInstallment != null) {
                    return recordLocalConflict(
                        activeGateway, opId, operation, StructuredEntityType.TRANSACTION, entityId, effectiveSource, effectiveCreatedAt, now,
                        "Parcela ${dto.installmentNumber} de '$installmentPurchaseId' já existe localmente; versão local preservada.",
                        existingInstallment.description
                    )
                }
            }
        }

        val recurringRuleId = dto.recurringRuleId?.trim()?.takeIf { it.isNotEmpty() }
        if (recurringRuleId != null) {
            if (!activeGateway.recurringRuleExists(recurringRuleId)) {
                return rejectReceipt(opId, operation, StructuredEntityType.TRANSACTION, effectiveSource, effectiveCreatedAt, "Regra recorrente '$recurringRuleId' não existe localmente.", desc)
            }
            if (dto.occurrenceIndex != null && existing == null) {
                val existingOccurrence = activeGateway.getTransactionByRecurringOccurrence(recurringRuleId, dto.occurrenceIndex)
                if (existingOccurrence != null) {
                    return recordLocalConflict(
                        activeGateway, opId, operation, StructuredEntityType.TRANSACTION, entityId, effectiveSource, effectiveCreatedAt, now,
                        "Ocorrência ${dto.occurrenceIndex} da recorrência '$recurringRuleId' já existe localmente; versão local preservada.",
                        existingOccurrence.description
                    )
                }
            }
        }

        if (isStandaloneStatementPaymentExpense(activeGateway, type, desc, creditCardId, statementId, installmentPurchaseId, dto.amountCents)) {
            return rejectReceipt(
                opId, operation, StructuredEntityType.TRANSACTION, effectiveSource, effectiveCreatedAt,
                "Despesa avulsa de pagamento de fatura bloqueada para evitar dupla contagem financeira.",
                desc
            )
        }

        val entity = TransactionEntity(
            id = entityId,
            type = type,
            description = desc,
            amountCents = dto.amountCents,
            categoryId = categoryId,
            status = status,
            paymentMethod = paymentMethod,
            occurredAt = dto.occurredAt,
            dueAt = dto.dueAt,
            settledAt = if (status == FinancialStatus.SETTLED) (dto.settledAt ?: existing?.settledAt ?: dto.occurredAt) else null,
            projectId = projectId,
            eventId = eventId,
            taskId = taskId,
            eventCostType = eventCostType,
            isTicketObligation = dto.isTicketObligation,
            creditCardId = creditCardId,
            statementId = statementId,
            installmentPurchaseId = installmentPurchaseId,
            installmentNumber = dto.installmentNumber,
            installmentsTotal = dto.installmentsTotal,
            recurringRuleId = recurringRuleId,
            occurrenceIndex = dto.occurrenceIndex,
            notes = dto.notes.trim(),
            createdAt = existing?.createdAt ?: now,
            updatedAt = now
        )
        val isUpdate = existing != null
        val detail = if (isUpdate) {
            "Transação '$desc' atualizada via entrada estruturada."
        } else {
            "Transação '$desc' registrada via entrada estruturada."
        }
        return applyPlan(
            activeGateway = activeGateway,
            opId = opId,
            operation = operation,
            entityType = StructuredEntityType.TRANSACTION,
            plan = OperationPlan.UpsertTransaction(entity),
            effectiveSource = effectiveSource,
            effectiveCreatedAt = effectiveCreatedAt,
            now = now,
            detail = detail,
            isUpdate = isUpdate,
            entityTitle = desc
        )
    }

    private suspend fun applyPlan(
        activeGateway: StructuredInputGateway,
        opId: String,
        operation: StructuredInputOperation,
        entityType: StructuredEntityType,
        plan: OperationPlan,
        effectiveSource: String,
        effectiveCreatedAt: Long,
        now: Long,
        detail: String,
        isUpdate: Boolean = false,
        entityTitle: String? = null,
        checklistItemsCount: Int = 0
    ): StructuredOperationReceipt {
        val state = if (isUpdate) StructuredProcessingState.UPDATED else StructuredProcessingState.APPLIED
        val actionTaken = if (isUpdate) "UPDATE" else "CREATE"
        plan.apply(activeGateway)
        activeGateway.recordOperationOutcome(
            operationId = opId,
            state = state,
            detail = "$detail [entityType=${entityType.name}, targetId=${plan.targetEntityId}, source=$effectiveSource]",
            occurredAt = now
        )
        return StructuredOperationReceipt(
            operationId = opId,
            entityTypeRaw = operation.entityType,
            entityType = entityType,
            targetEntityId = plan.targetEntityId,
            contractVersion = operation.contractVersion,
            source = effectiveSource,
            createdAt = effectiveCreatedAt,
            processingState = state,
            resultDetail = detail,
            entityTitle = entityTitle,
            checklistItemsCount = checklistItemsCount,
            actionTaken = actionTaken
        )
    }

    private suspend fun recordLocalConflict(
        activeGateway: StructuredInputGateway,
        opId: String,
        operation: StructuredInputOperation,
        entityType: StructuredEntityType,
        targetId: String,
        effectiveSource: String,
        effectiveCreatedAt: Long,
        now: Long,
        detail: String,
        entityTitle: String? = null
    ): StructuredOperationReceipt {
        activeGateway.recordOperationOutcome(
            operationId = opId,
            state = StructuredProcessingState.CONFLICT_PRESERVED_LOCAL,
            detail = "$detail [entityType=${entityType.name}, targetId=$targetId, source=$effectiveSource]",
            occurredAt = now
        )
        return StructuredOperationReceipt(
            operationId = opId,
            entityTypeRaw = operation.entityType,
            entityType = entityType,
            targetEntityId = targetId,
            contractVersion = operation.contractVersion,
            source = effectiveSource,
            createdAt = effectiveCreatedAt,
            processingState = StructuredProcessingState.CONFLICT_PRESERVED_LOCAL,
            resultDetail = detail,
            entityTitle = entityTitle,
            actionTaken = "NONE"
        )
    }

    private suspend fun isStandaloneStatementPaymentExpense(
        activeGateway: StructuredInputGateway,
        type: TransactionType,
        description: String,
        creditCardId: String?,
        statementId: String?,
        installmentPurchaseId: String?,
        amountCents: Long
    ): Boolean {
        if (statementId != null || installmentPurchaseId != null) return false
        if (type != TransactionType.EXPENSE) return false
        val normalized = description.trim().lowercase()
        val looksLikePayment = normalized.contains("pagamento de fatura") ||
            normalized.contains("pagamento fatura") ||
            normalized.contains("quitação de fatura") ||
            normalized.contains("quitacao de fatura") ||
            normalized == "fatura paga"
        if (!looksLikePayment) return false
        return creditCardId != null || activeGateway.getAllStatementsOnce().any { stmt ->
            stmt.status == StatementStatus.PAID &&
                (stmt.paidAmountCents == amountCents || stmt.creditCardId == creditCardId)
        }
    }

    private fun validateOperationHeader(
        operation: StructuredInputOperation,
        envelope: StructuredInputEnvelope,
        effectiveSource: String
    ): String? {
        if (operation.operationId.isBlank()) {
            return "Campo obrigatório 'operationId' ausente."
        }
        if (operation.contractVersion != StructuredInputEnvelope.CURRENT_CONTRACT_VERSION) {
            return "Versão de contrato da operação incompatível (v${operation.contractVersion}). " +
                "Suportado: v${StructuredInputEnvelope.CURRENT_CONTRACT_VERSION}."
        }
        if (operation.contractVersion != envelope.contractVersion) {
            return "Divergência entre contractVersion do envelope (v${envelope.contractVersion}) e da operação (v${operation.contractVersion})."
        }
        if (effectiveSource.isBlank()) {
            return "Campo obrigatório 'source' ausente."
        }
        if (operation.createdAt < 0L) {
            return "Campo 'createdAt' negativo na operação."
        }
        if (StructuredOperationAction.fromRawOrNull(operation.action) == null) {
            return "Ação inválida: '${operation.action}'. Use CREATE, UPDATE ou UPSERT."
        }
        return null
    }

    private inline fun <reified T> decodeData(dataObj: JsonElement): Result<T> =
        try {
            Result.success(strictJson.decodeFromJsonElement<T>(dataObj))
        } catch (ex: Exception) {
            Result.failure(IllegalArgumentException("Payload 'data' inválido para o contrato: ${ex.message}"))
        }

    private inline fun <reified E : Enum<E>> parseEnumStrict(raw: String, fieldName: String): E? {
        val clean = raw.trim()
        if (clean.isEmpty()) return null
        return enumValues<E>().firstOrNull { it.name.equals(clean, ignoreCase = true) }
    }

    private fun rejectReceipt(
        opId: String,
        operation: StructuredInputOperation,
        entityType: StructuredEntityType?,
        effectiveSource: String,
        effectiveCreatedAt: Long,
        err: Throwable,
        entityTitle: String? = null
    ): StructuredOperationReceipt = rejectReceipt(
        opId = opId,
        operation = operation,
        entityType = entityType,
        effectiveSource = effectiveSource,
        effectiveCreatedAt = effectiveCreatedAt,
        reason = err.message ?: "Dados inválidos.",
        entityTitle = entityTitle
    )

    private fun rejectReceipt(
        opId: String,
        operation: StructuredInputOperation,
        entityType: StructuredEntityType?,
        effectiveSource: String,
        effectiveCreatedAt: Long,
        reason: String,
        entityTitle: String? = null
    ): StructuredOperationReceipt = StructuredOperationReceipt(
        operationId = opId,
        entityTypeRaw = operation.entityType,
        entityType = entityType,
        targetEntityId = null,
        contractVersion = operation.contractVersion,
        source = effectiveSource,
        createdAt = effectiveCreatedAt,
        processingState = StructuredProcessingState.REJECTED,
        rejectionReason = reason,
        entityTitle = entityTitle,
        actionTaken = "NONE"
    )

    private fun rejectEnvelope(
        envelope: StructuredInputEnvelope,
        atomicBatch: Boolean,
        reason: String
    ): StructuredBatchResult {
        val receipts = envelope.operations.map { op ->
            StructuredOperationReceipt(
                operationId = op.operationId.trim().ifEmpty { "(sem operationId)" },
                entityTypeRaw = op.entityType,
                entityType = StructuredEntityType.fromRawOrNull(op.entityType),
                targetEntityId = null,
                contractVersion = op.contractVersion,
                source = op.source.trim().ifEmpty { envelope.source.trim() },
                createdAt = if (op.createdAt > 0L) op.createdAt else envelope.createdAt,
                processingState = StructuredProcessingState.REJECTED,
                rejectionReason = reason,
                actionTaken = "NONE"
            )
        }
        return buildBatchResult(
            contractVersion = envelope.contractVersion,
            source = envelope.source.trim(),
            atomicBatch = atomicBatch,
            receipts = receipts,
            envelopeError = reason
        )
    }

    private fun buildBatchResult(
        contractVersion: Int,
        source: String,
        atomicBatch: Boolean,
        receipts: List<StructuredOperationReceipt>,
        envelopeError: String? = null
    ): StructuredBatchResult = StructuredBatchResult(
        contractVersion = contractVersion,
        source = source,
        atomicBatch = atomicBatch,
        totalOperations = receipts.size,
        appliedCount = receipts.count { it.processingState == StructuredProcessingState.APPLIED },
        updatedCount = receipts.count { it.processingState == StructuredProcessingState.UPDATED },
        duplicateIgnoredCount = receipts.count { it.processingState == StructuredProcessingState.DUPLICATE_IGNORED },
        conflictPreservedLocalCount = receipts.count { it.processingState == StructuredProcessingState.CONFLICT_PRESERVED_LOCAL },
        rejectedCount = receipts.count { it.processingState == StructuredProcessingState.REJECTED },
        failedRolledBackCount = receipts.count { it.processingState == StructuredProcessingState.FAILED_ROLLED_BACK },
        receipts = receipts,
        envelopeError = envelopeError
    )

    private class BatchAbortException(
        val causeReceipt: StructuredOperationReceipt,
        val partialReceipts: List<StructuredOperationReceipt>
    ) : RuntimeException(causeReceipt.rejectionReason)
}
