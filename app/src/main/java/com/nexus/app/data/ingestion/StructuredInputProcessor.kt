package com.nexus.app.data.ingestion

import com.nexus.app.core.DateTimeUtils
import com.nexus.app.data.db.entity.AppointmentEntity
import com.nexus.app.data.db.entity.EventEntity
import com.nexus.app.data.db.entity.PersonalGoalEntity
import com.nexus.app.data.db.entity.ProjectEntity
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
        suspend fun apply(gateway: StructuredInputGateway)

        data class InsertTask(val entity: TaskEntity) : OperationPlan {
            override val targetEntityId: String get() = entity.id
            override suspend fun apply(gateway: StructuredInputGateway) = gateway.insertTask(entity)
        }

        data class InsertAppointment(val entity: AppointmentEntity) : OperationPlan {
            override val targetEntityId: String get() = entity.id
            override suspend fun apply(gateway: StructuredInputGateway) = gateway.insertAppointment(entity)
        }

        data class InsertEvent(val entity: EventEntity) : OperationPlan {
            override val targetEntityId: String get() = entity.id
            override suspend fun apply(gateway: StructuredInputGateway) = gateway.insertEvent(entity)
        }

        data class InsertProject(val entity: ProjectEntity) : OperationPlan {
            override val targetEntityId: String get() = entity.id
            override suspend fun apply(gateway: StructuredInputGateway) = gateway.insertProject(entity)
        }

        data class InsertPersonalGoal(val entity: PersonalGoalEntity) : OperationPlan {
            override val targetEntityId: String get() = entity.id
            override suspend fun apply(gateway: StructuredInputGateway) = gateway.insertPersonalGoal(entity)
        }

        data class InsertTransaction(val entity: TransactionEntity) : OperationPlan {
            override val targetEntityId: String get() = entity.id
            override suspend fun apply(gateway: StructuredInputGateway) = gateway.insertTransaction(entity)
        }
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
        if (envelope.contractVersion != StructuredInputEnvelope.CURRENT_CONTRACT_VERSION) {
            return rejectEnvelope(
                envelope = envelope,
                atomicBatch = atomicBatch,
                reason = "Versão de contrato incompatível (v${envelope.contractVersion}). " +
                    "Suportado: v${StructuredInputEnvelope.CURRENT_CONTRACT_VERSION}."
            )
        }
        if (envelope.source.isBlank() && envelope.operations.any { it.source.isBlank() }) {
            return rejectEnvelope(
                envelope = envelope,
                atomicBatch = atomicBatch,
                reason = "Campo obrigatório 'source' ausente no envelope e em operação(ões)."
            )
        }
        if (envelope.createdAt < 0L) {
            return rejectEnvelope(
                envelope = envelope,
                atomicBatch = atomicBatch,
                reason = "Campo 'createdAt' negativo no envelope."
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

        val priorState = gateway.findRecordedOperationState(opId)
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
                resultDetail = "Operação '$opId' já processada anteriormente ($priorState); ignorada com idempotência."
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
                opId, operation, dataObj, effectiveSource, effectiveCreatedAt, now
            )
            StructuredEntityType.APPOINTMENT -> prepareAndApplyAppointment(
                opId, operation, dataObj, effectiveSource, effectiveCreatedAt, now
            )
            StructuredEntityType.EVENT -> prepareAndApplyEvent(
                opId, operation, dataObj, effectiveSource, effectiveCreatedAt, now
            )
            StructuredEntityType.PROJECT -> prepareAndApplyProject(
                opId, operation, dataObj, effectiveSource, effectiveCreatedAt, now
            )
            StructuredEntityType.PERSONAL_GOAL -> prepareAndApplyPersonalGoal(
                opId, operation, dataObj, effectiveSource, effectiveCreatedAt, now
            )
            StructuredEntityType.TRANSACTION -> prepareAndApplyTransaction(
                opId, operation, dataObj, effectiveSource, effectiveCreatedAt, now
            )
        }
    }

    private suspend fun prepareAndApplyTask(
        opId: String,
        operation: StructuredInputOperation,
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
            return rejectReceipt(opId, operation, StructuredEntityType.TASK, effectiveSource, effectiveCreatedAt, "Título da tarefa é obrigatório.")
        }
        val priority = parseEnumStrict<TaskPriority>(dto.priority, "priority")
            ?: return rejectReceipt(opId, operation, StructuredEntityType.TASK, effectiveSource, effectiveCreatedAt, "Prioridade inválida: '${dto.priority}'.")
        val status = parseEnumStrict<TaskStatus>(dto.status, "status")
            ?: return rejectReceipt(opId, operation, StructuredEntityType.TASK, effectiveSource, effectiveCreatedAt, "Status de tarefa inválido: '${dto.status}'.")

        val entityId = dto.id?.trim()?.takeIf { it.isNotEmpty() } ?: "ing-task-$opId"
        if (gateway.taskExists(entityId)) {
            return recordLocalConflict(opId, operation, StructuredEntityType.TASK, entityId, effectiveSource, effectiveCreatedAt, now, "Tarefa '$entityId' já existe localmente; versão local preservada.")
        }

        val projectId = dto.projectId?.trim()?.takeIf { it.isNotEmpty() }
        if (projectId != null && !gateway.projectExists(projectId)) {
            return rejectReceipt(opId, operation, StructuredEntityType.TASK, effectiveSource, effectiveCreatedAt, "Projeto vinculado '$projectId' não existe localmente.")
        }
        val categoryId = dto.categoryId?.trim()?.takeIf { it.isNotEmpty() }
        if (categoryId != null && !gateway.categoryExists(categoryId)) {
            return rejectReceipt(opId, operation, StructuredEntityType.TASK, effectiveSource, effectiveCreatedAt, "Categoria vinculada '$categoryId' não existe localmente.")
        }
        val eventId = dto.eventId?.trim()?.takeIf { it.isNotEmpty() }
        if (eventId != null && !gateway.eventExists(eventId)) {
            return rejectReceipt(opId, operation, StructuredEntityType.TASK, effectiveSource, effectiveCreatedAt, "Evento vinculado '$eventId' não existe localmente.")
        }
        val goalId = dto.goalId?.trim()?.takeIf { it.isNotEmpty() }
        if (goalId != null && !gateway.personalGoalExists(goalId)) {
            return rejectReceipt(opId, operation, StructuredEntityType.TASK, effectiveSource, effectiveCreatedAt, "Objetivo vinculado '$goalId' não existe localmente.")
        }
        val recurrenceId = dto.recurrenceId?.trim()?.takeIf { it.isNotEmpty() }
        if (recurrenceId != null && !gateway.taskRecurrenceExists(recurrenceId)) {
            return rejectReceipt(opId, operation, StructuredEntityType.TASK, effectiveSource, effectiveCreatedAt, "Série recorrente '$recurrenceId' não existe localmente.")
        }
        val occurrenceDateIso = dto.occurrenceDateIso?.trim()?.takeIf { it.isNotEmpty() }
        if (recurrenceId != null && occurrenceDateIso != null &&
            gateway.getTaskByRecurrenceAndDate(recurrenceId, occurrenceDateIso) != null
        ) {
            return recordLocalConflict(
                opId, operation, StructuredEntityType.TASK, entityId, effectiveSource, effectiveCreatedAt, now,
                "Ocorrência ($recurrenceId, $occurrenceDateIso) já existe localmente; versão local preservada."
            )
        }

        val entity = TaskEntity(
            id = entityId,
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
            completedAt = if (status == TaskStatus.COMPLETED) (dto.completedAt ?: now) else null,
            recurrenceId = recurrenceId,
            occurrenceDateIso = occurrenceDateIso,
            createdAt = now,
            updatedAt = now
        )
        return applyPlan(opId, operation, StructuredEntityType.TASK, OperationPlan.InsertTask(entity), effectiveSource, effectiveCreatedAt, now, "Tarefa '$title' criada via entrada estruturada.")
    }

    private suspend fun prepareAndApplyAppointment(
        opId: String,
        operation: StructuredInputOperation,
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
            return rejectReceipt(opId, operation, StructuredEntityType.APPOINTMENT, effectiveSource, effectiveCreatedAt, "Intervalo de datas inválido no compromisso (startAt/endAt).")
        }
        val entityId = dto.id?.trim()?.takeIf { it.isNotEmpty() } ?: "ing-appt-$opId"
        if (gateway.appointmentExists(entityId)) {
            return recordLocalConflict(opId, operation, StructuredEntityType.APPOINTMENT, entityId, effectiveSource, effectiveCreatedAt, now, "Compromisso '$entityId' já existe localmente; versão local preservada.")
        }

        val categoryId = dto.categoryId?.trim()?.takeIf { it.isNotEmpty() }
        if (categoryId != null && !gateway.categoryExists(categoryId)) {
            return rejectReceipt(opId, operation, StructuredEntityType.APPOINTMENT, effectiveSource, effectiveCreatedAt, "Categoria vinculada '$categoryId' não existe localmente.")
        }
        val eventId = dto.eventId?.trim()?.takeIf { it.isNotEmpty() }
        if (eventId != null && !gateway.eventExists(eventId)) {
            return rejectReceipt(opId, operation, StructuredEntityType.APPOINTMENT, effectiveSource, effectiveCreatedAt, "Evento vinculado '$eventId' não existe localmente.")
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
            createdAt = now,
            updatedAt = now
        )
        return applyPlan(opId, operation, StructuredEntityType.APPOINTMENT, OperationPlan.InsertAppointment(entity), effectiveSource, effectiveCreatedAt, now, "Compromisso '$title' criado via entrada estruturada.")
    }

    private suspend fun prepareAndApplyEvent(
        opId: String,
        operation: StructuredInputOperation,
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
            return rejectReceipt(opId, operation, StructuredEntityType.EVENT, effectiveSource, effectiveCreatedAt, "Intervalo de datas inválido no evento (startAt/endAt).")
        }
        if (dto.ticketPriceCents < 0L || dto.ticketFeesCents < 0L || dto.ticketTotalCents < 0L || dto.ticketQuantity < 1 || dto.installmentsCount < 1) {
            return rejectReceipt(opId, operation, StructuredEntityType.EVENT, effectiveSource, effectiveCreatedAt, "Valores de ingresso/parcelas inválidos no evento.")
        }

        val eventStatus = parseEnumStrict<EventLifecycleStatus>(dto.eventStatus, "eventStatus")
            ?: return rejectReceipt(opId, operation, StructuredEntityType.EVENT, effectiveSource, effectiveCreatedAt, "eventStatus inválido: '${dto.eventStatus}'.")
        val ticketStatus = parseEnumStrict<TicketStatus>(dto.ticketStatus, "ticketStatus")
            ?: return rejectReceipt(opId, operation, StructuredEntityType.EVENT, effectiveSource, effectiveCreatedAt, "ticketStatus inválido: '${dto.ticketStatus}'.")
        val paymentMethod = parseEnumStrict<PaymentMethod>(dto.paymentMethod, "paymentMethod")
            ?: return rejectReceipt(opId, operation, StructuredEntityType.EVENT, effectiveSource, effectiveCreatedAt, "paymentMethod inválido: '${dto.paymentMethod}'.")

        val entityId = dto.id?.trim()?.takeIf { it.isNotEmpty() } ?: "ing-event-$opId"
        if (gateway.eventExists(entityId)) {
            return recordLocalConflict(opId, operation, StructuredEntityType.EVENT, entityId, effectiveSource, effectiveCreatedAt, now, "Evento '$entityId' já existe localmente; versão local preservada.")
        }

        val categoryId = dto.categoryId?.trim()?.takeIf { it.isNotEmpty() }
        if (categoryId != null && !gateway.categoryExists(categoryId)) {
            return rejectReceipt(opId, operation, StructuredEntityType.EVENT, effectiveSource, effectiveCreatedAt, "Categoria vinculada '$categoryId' não existe localmente.")
        }
        val creditCardId = dto.creditCardId?.trim()?.takeIf { it.isNotEmpty() }
        if (creditCardId != null && !gateway.creditCardExists(creditCardId)) {
            return rejectReceipt(opId, operation, StructuredEntityType.EVENT, effectiveSource, effectiveCreatedAt, "Cartão vinculado '$creditCardId' não existe localmente.")
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
            createdAt = now,
            updatedAt = now
        )
        return applyPlan(opId, operation, StructuredEntityType.EVENT, OperationPlan.InsertEvent(entity), effectiveSource, effectiveCreatedAt, now, "Evento '$name' criado via entrada estruturada.")
    }

    private suspend fun prepareAndApplyProject(
        opId: String,
        operation: StructuredInputOperation,
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
            ?: return rejectReceipt(opId, operation, StructuredEntityType.PROJECT, effectiveSource, effectiveCreatedAt, "Status de projeto inválido: '${dto.status}'.")
        val priority = parseEnumStrict<TaskPriority>(dto.priority, "priority")
            ?: return rejectReceipt(opId, operation, StructuredEntityType.PROJECT, effectiveSource, effectiveCreatedAt, "Prioridade de projeto inválida: '${dto.priority}'.")

        val entityId = dto.id?.trim()?.takeIf { it.isNotEmpty() } ?: "ing-proj-$opId"
        if (gateway.projectExists(entityId)) {
            return recordLocalConflict(opId, operation, StructuredEntityType.PROJECT, entityId, effectiveSource, effectiveCreatedAt, now, "Projeto '$entityId' já existe localmente; versão local preservada.")
        }

        val goalId = dto.goalId?.trim()?.takeIf { it.isNotEmpty() }
        if (goalId != null && !gateway.personalGoalExists(goalId)) {
            return rejectReceipt(opId, operation, StructuredEntityType.PROJECT, effectiveSource, effectiveCreatedAt, "Objetivo pessoal vinculado '$goalId' não existe localmente.")
        }

        val entity = ProjectEntity(
            id = entityId,
            name = name,
            description = dto.description.trim(),
            deadlineAt = dto.deadlineAt,
            status = status,
            priority = priority,
            goalId = goalId,
            createdAt = now,
            updatedAt = now
        )
        return applyPlan(opId, operation, StructuredEntityType.PROJECT, OperationPlan.InsertProject(entity), effectiveSource, effectiveCreatedAt, now, "Projeto '$name' criado via entrada estruturada.")
    }

    private suspend fun prepareAndApplyPersonalGoal(
        opId: String,
        operation: StructuredInputOperation,
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
            return rejectReceipt(opId, operation, StructuredEntityType.PERSONAL_GOAL, effectiveSource, effectiveCreatedAt, "manualProgressPercent deve estar entre 0 e 100.")
        }
        val status = parseEnumStrict<GoalStatus>(dto.status, "status")
            ?: return rejectReceipt(opId, operation, StructuredEntityType.PERSONAL_GOAL, effectiveSource, effectiveCreatedAt, "Status do objetivo inválido: '${dto.status}'.")
        val progressMode = parseEnumStrict<GoalProgressMode>(dto.progressMode, "progressMode")
            ?: return rejectReceipt(opId, operation, StructuredEntityType.PERSONAL_GOAL, effectiveSource, effectiveCreatedAt, "progressMode inválido: '${dto.progressMode}'.")

        val entityId = dto.id?.trim()?.takeIf { it.isNotEmpty() } ?: "ing-goal-$opId"
        if (gateway.personalGoalExists(entityId)) {
            return recordLocalConflict(opId, operation, StructuredEntityType.PERSONAL_GOAL, entityId, effectiveSource, effectiveCreatedAt, now, "Objetivo pessoal '$entityId' já existe localmente; versão local preservada.")
        }

        val categoryId = dto.categoryId?.trim()?.takeIf { it.isNotEmpty() }
        if (categoryId != null && !gateway.categoryExists(categoryId)) {
            return rejectReceipt(opId, operation, StructuredEntityType.PERSONAL_GOAL, effectiveSource, effectiveCreatedAt, "Categoria vinculada '$categoryId' não existe localmente.")
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
            completedAt = if (status == GoalStatus.COMPLETED) (dto.completedAt ?: now) else null,
            notes = dto.notes.trim(),
            createdAt = now,
            updatedAt = now
        )
        return applyPlan(opId, operation, StructuredEntityType.PERSONAL_GOAL, OperationPlan.InsertPersonalGoal(entity), effectiveSource, effectiveCreatedAt, now, "Objetivo pessoal '$title' criado via entrada estruturada.")
    }

    private suspend fun prepareAndApplyTransaction(
        opId: String,
        operation: StructuredInputOperation,
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
            return rejectReceipt(opId, operation, StructuredEntityType.TRANSACTION, effectiveSource, effectiveCreatedAt, "Valor financeiro (amountCents) deve ser maior que zero.")
        }
        if (dto.occurredAt <= 0L) {
            return rejectReceipt(opId, operation, StructuredEntityType.TRANSACTION, effectiveSource, effectiveCreatedAt, "Data de ocorrência (occurredAt) obrigatória e positiva.")
        }
        val type = parseEnumStrict<TransactionType>(dto.type, "type")
            ?: return rejectReceipt(opId, operation, StructuredEntityType.TRANSACTION, effectiveSource, effectiveCreatedAt, "Tipo financeiro inválido: '${dto.type}'.")
        val status = parseEnumStrict<FinancialStatus>(dto.status, "status")
            ?: return rejectReceipt(opId, operation, StructuredEntityType.TRANSACTION, effectiveSource, effectiveCreatedAt, "Status financeiro inválido: '${dto.status}'.")
        val paymentMethod = parseEnumStrict<PaymentMethod>(dto.paymentMethod, "paymentMethod")
            ?: return rejectReceipt(opId, operation, StructuredEntityType.TRANSACTION, effectiveSource, effectiveCreatedAt, "Método de pagamento inválido: '${dto.paymentMethod}'.")
        val eventCostType = dto.eventCostType?.trim()?.takeIf { it.isNotEmpty() }?.let { raw ->
            parseEnumStrict<EventCostType>(raw, "eventCostType")
                ?: return rejectReceipt(opId, operation, StructuredEntityType.TRANSACTION, effectiveSource, effectiveCreatedAt, "eventCostType inválido: '$raw'.")
        }

        if (dto.installmentNumber != null && dto.installmentNumber < 1) {
            return rejectReceipt(opId, operation, StructuredEntityType.TRANSACTION, effectiveSource, effectiveCreatedAt, "Número da parcela inválido (${dto.installmentNumber}).")
        }
        if (dto.installmentsTotal != null && dto.installmentsTotal < 1) {
            return rejectReceipt(opId, operation, StructuredEntityType.TRANSACTION, effectiveSource, effectiveCreatedAt, "Total de parcelas inválido (${dto.installmentsTotal}).")
        }
        if (dto.installmentNumber != null && dto.installmentsTotal != null && dto.installmentNumber > dto.installmentsTotal) {
            return rejectReceipt(opId, operation, StructuredEntityType.TRANSACTION, effectiveSource, effectiveCreatedAt, "Parcela ${dto.installmentNumber}/${dto.installmentsTotal} inconsistente.")
        }

        val entityId = dto.id?.trim()?.takeIf { it.isNotEmpty() } ?: "ing-tx-$opId"
        if (gateway.transactionExists(entityId)) {
            return recordLocalConflict(opId, operation, StructuredEntityType.TRANSACTION, entityId, effectiveSource, effectiveCreatedAt, now, "Transação '$entityId' já existe localmente; versão local preservada.")
        }

        val categoryId = dto.categoryId?.trim()?.takeIf { it.isNotEmpty() }
        if (categoryId != null && !gateway.categoryExists(categoryId)) {
            return rejectReceipt(opId, operation, StructuredEntityType.TRANSACTION, effectiveSource, effectiveCreatedAt, "Categoria vinculada '$categoryId' não existe localmente.")
        }
        val projectId = dto.projectId?.trim()?.takeIf { it.isNotEmpty() }
        if (projectId != null && !gateway.projectExists(projectId)) {
            return rejectReceipt(opId, operation, StructuredEntityType.TRANSACTION, effectiveSource, effectiveCreatedAt, "Projeto vinculado '$projectId' não existe localmente.")
        }
        val eventId = dto.eventId?.trim()?.takeIf { it.isNotEmpty() }
        if (eventId != null && !gateway.eventExists(eventId)) {
            return rejectReceipt(opId, operation, StructuredEntityType.TRANSACTION, effectiveSource, effectiveCreatedAt, "Evento vinculado '$eventId' não existe localmente.")
        }
        val taskId = dto.taskId?.trim()?.takeIf { it.isNotEmpty() }
        if (taskId != null && !gateway.taskExists(taskId)) {
            return rejectReceipt(opId, operation, StructuredEntityType.TRANSACTION, effectiveSource, effectiveCreatedAt, "Tarefa vinculada '$taskId' não existe localmente.")
        }

        val creditCardId = dto.creditCardId?.trim()?.takeIf { it.isNotEmpty() }
        if (creditCardId != null && !gateway.creditCardExists(creditCardId)) {
            return rejectReceipt(opId, operation, StructuredEntityType.TRANSACTION, effectiveSource, effectiveCreatedAt, "Cartão vinculado '$creditCardId' não existe localmente.")
        }

        val statementId = dto.statementId?.trim()?.takeIf { it.isNotEmpty() }
        val statement = if (statementId != null) {
            gateway.getStatementById(statementId)
                ?: return rejectReceipt(opId, operation, StructuredEntityType.TRANSACTION, effectiveSource, effectiveCreatedAt, "Fatura vinculada '$statementId' não existe localmente.")
        } else {
            null
        }
        if (statement != null) {
            if (creditCardId != null && statement.creditCardId != creditCardId) {
                return rejectReceipt(opId, operation, StructuredEntityType.TRANSACTION, effectiveSource, effectiveCreatedAt, "Fatura '$statementId' pertence a outro cartão (${statement.creditCardId}).")
            }
            if (statement.status == StatementStatus.PAID && status != FinancialStatus.SETTLED) {
                return rejectReceipt(opId, operation, StructuredEntityType.TRANSACTION, effectiveSource, effectiveCreatedAt, "Lançamento não quitado bloqueado em fatura já paga ($statementId).")
            }
        }

        val installmentPurchaseId = dto.installmentPurchaseId?.trim()?.takeIf { it.isNotEmpty() }
        if (installmentPurchaseId != null) {
            if (!gateway.installmentPurchaseExists(installmentPurchaseId)) {
                return rejectReceipt(opId, operation, StructuredEntityType.TRANSACTION, effectiveSource, effectiveCreatedAt, "Parcelamento '$installmentPurchaseId' não existe localmente.")
            }
            if (dto.installmentNumber != null) {
                val existingInstallment = gateway.getTransactionsByInstallmentPurchase(installmentPurchaseId)
                    .firstOrNull { it.installmentNumber == dto.installmentNumber }
                if (existingInstallment != null) {
                    return recordLocalConflict(
                        opId, operation, StructuredEntityType.TRANSACTION, entityId, effectiveSource, effectiveCreatedAt, now,
                        "Parcela ${dto.installmentNumber} de '$installmentPurchaseId' já existe localmente; versão local preservada."
                    )
                }
            }
        }

        val recurringRuleId = dto.recurringRuleId?.trim()?.takeIf { it.isNotEmpty() }
        if (recurringRuleId != null) {
            if (!gateway.recurringRuleExists(recurringRuleId)) {
                return rejectReceipt(opId, operation, StructuredEntityType.TRANSACTION, effectiveSource, effectiveCreatedAt, "Regra recorrente '$recurringRuleId' não existe localmente.")
            }
            if (dto.occurrenceIndex != null &&
                gateway.getTransactionByRecurringOccurrence(recurringRuleId, dto.occurrenceIndex) != null
            ) {
                return recordLocalConflict(
                    opId, operation, StructuredEntityType.TRANSACTION, entityId, effectiveSource, effectiveCreatedAt, now,
                    "Ocorrência ${dto.occurrenceIndex} da recorrência '$recurringRuleId' já existe localmente; versão local preservada."
                )
            }
        }

        if (isStandaloneStatementPaymentExpense(type, desc, creditCardId, statementId, installmentPurchaseId, dto.amountCents)) {
            return rejectReceipt(
                opId, operation, StructuredEntityType.TRANSACTION, effectiveSource, effectiveCreatedAt,
                "Despesa avulsa de pagamento de fatura bloqueada para evitar dupla contagem financeira."
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
            settledAt = if (status == FinancialStatus.SETTLED) (dto.settledAt ?: dto.occurredAt) else null,
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
            createdAt = now,
            updatedAt = now
        )
        return applyPlan(opId, operation, StructuredEntityType.TRANSACTION, OperationPlan.InsertTransaction(entity), effectiveSource, effectiveCreatedAt, now, "Transação '$desc' registrada via entrada estruturada.")
    }

    private suspend fun applyPlan(
        opId: String,
        operation: StructuredInputOperation,
        entityType: StructuredEntityType,
        plan: OperationPlan,
        effectiveSource: String,
        effectiveCreatedAt: Long,
        now: Long,
        detail: String
    ): StructuredOperationReceipt {
        plan.apply(gateway)
        gateway.recordOperationOutcome(
            operationId = opId,
            state = StructuredProcessingState.APPLIED,
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
            processingState = StructuredProcessingState.APPLIED,
            resultDetail = detail
        )
    }

    private suspend fun recordLocalConflict(
        opId: String,
        operation: StructuredInputOperation,
        entityType: StructuredEntityType,
        targetId: String,
        effectiveSource: String,
        effectiveCreatedAt: Long,
        now: Long,
        detail: String
    ): StructuredOperationReceipt {
        gateway.recordOperationOutcome(
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
            resultDetail = detail
        )
    }

    private suspend fun isStandaloneStatementPaymentExpense(
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
        return creditCardId != null || gateway.getAllStatementsOnce().any { stmt ->
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
        err: Throwable
    ): StructuredOperationReceipt = rejectReceipt(
        opId = opId,
        operation = operation,
        entityType = entityType,
        effectiveSource = effectiveSource,
        effectiveCreatedAt = effectiveCreatedAt,
        reason = err.message ?: "Dados inválidos."
    )

    private fun rejectReceipt(
        opId: String,
        operation: StructuredInputOperation,
        entityType: StructuredEntityType?,
        effectiveSource: String,
        effectiveCreatedAt: Long,
        reason: String
    ): StructuredOperationReceipt = StructuredOperationReceipt(
        operationId = opId,
        entityTypeRaw = operation.entityType,
        entityType = entityType,
        targetEntityId = null,
        contractVersion = operation.contractVersion,
        source = effectiveSource,
        createdAt = effectiveCreatedAt,
        processingState = StructuredProcessingState.REJECTED,
        rejectionReason = reason
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
                rejectionReason = reason
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
