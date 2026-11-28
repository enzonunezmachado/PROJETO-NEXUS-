package com.nexus.app

import com.nexus.app.data.db.entity.AppointmentEntity
import com.nexus.app.data.db.entity.AuditHistoryEntity
import com.nexus.app.data.db.entity.CategoryEntity
import com.nexus.app.data.db.entity.CreditCardEntity
import com.nexus.app.data.db.entity.CreditCardStatementEntity
import com.nexus.app.data.db.entity.EventEntity
import com.nexus.app.data.db.entity.InstallmentPurchaseEntity
import com.nexus.app.data.db.entity.PersonalGoalEntity
import com.nexus.app.data.db.entity.ProjectEntity
import com.nexus.app.data.db.entity.RecurringRuleEntity
import com.nexus.app.data.db.entity.ScheduleChecklistItemEntity
import com.nexus.app.data.db.entity.SubtaskEntity
import com.nexus.app.data.db.entity.TaskEntity
import com.nexus.app.data.db.entity.TaskRecurrenceEntity
import com.nexus.app.data.db.entity.TransactionEntity
import com.nexus.app.data.ingestion.RoomStructuredInputGateway
import com.nexus.app.data.ingestion.StructuredEntityType
import com.nexus.app.data.ingestion.StructuredInputEnvelope
import com.nexus.app.data.ingestion.StructuredInputGateway
import com.nexus.app.data.ingestion.StructuredInputOperation
import com.nexus.app.data.ingestion.StructuredInputProcessor
import com.nexus.app.data.ingestion.StructuredPreviewOutcome
import com.nexus.app.data.ingestion.StructuredProcessingState
import com.nexus.app.domain.model.CategoryScope
import com.nexus.app.domain.model.EventLifecycleStatus
import com.nexus.app.domain.model.FinancialStatus
import com.nexus.app.domain.model.GoalProgressMode
import com.nexus.app.domain.model.GoalStatus
import com.nexus.app.domain.model.PaymentMethod
import com.nexus.app.domain.model.ProjectStatus
import com.nexus.app.domain.model.RecurrenceDomain
import com.nexus.app.domain.model.RecurrenceFrequency
import com.nexus.app.domain.model.StatementStatus
import com.nexus.app.domain.model.TaskPriority
import com.nexus.app.domain.model.TaskRecurrenceFrequency
import com.nexus.app.domain.model.TaskStatus
import com.nexus.app.domain.model.TicketStatus
import com.nexus.app.domain.model.TransactionType
import com.nexus.app.domain.rules.RecurrenceEngine
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StructuredInputUnitTests {

    private class InMemoryStructuredInputGateway : StructuredInputGateway {
        val categories = mutableListOf<CategoryEntity>()
        val projects = mutableListOf<ProjectEntity>()
        val personalGoals = mutableListOf<PersonalGoalEntity>()
        val events = mutableListOf<EventEntity>()
        val tasks = mutableListOf<TaskEntity>()
        val subtasks = mutableListOf<SubtaskEntity>()
        val appointments = mutableListOf<AppointmentEntity>()
        val scheduleChecklistItems = mutableListOf<ScheduleChecklistItemEntity>()
        val transactions = mutableListOf<TransactionEntity>()
        val taskRecurrences = mutableListOf<TaskRecurrenceEntity>()
        val creditCards = mutableListOf<CreditCardEntity>()
        val statements = mutableListOf<CreditCardStatementEntity>()
        val installmentPurchases = mutableListOf<InstallmentPurchaseEntity>()
        val recurringRules = mutableListOf<RecurringRuleEntity>()
        val auditHistory = mutableListOf<AuditHistoryEntity>()

        var failOnEntityInsert: StructuredEntityType? = null
        var failOnAuditRecord: Boolean = false

        private data class Snapshot(
            val projects: List<ProjectEntity>,
            val personalGoals: List<PersonalGoalEntity>,
            val events: List<EventEntity>,
            val tasks: List<TaskEntity>,
            val subtasks: List<SubtaskEntity>,
            val appointments: List<AppointmentEntity>,
            val scheduleChecklistItems: List<ScheduleChecklistItemEntity>,
            val taskRecurrences: List<TaskRecurrenceEntity>,
            val transactions: List<TransactionEntity>,
            val auditHistory: List<AuditHistoryEntity>
        )

        private fun snapshot() = Snapshot(
            projects = projects.toList(),
            personalGoals = personalGoals.toList(),
            events = events.toList(),
            tasks = tasks.toList(),
            subtasks = subtasks.toList(),
            appointments = appointments.toList(),
            scheduleChecklistItems = scheduleChecklistItems.toList(),
            taskRecurrences = taskRecurrences.toList(),
            transactions = transactions.toList(),
            auditHistory = auditHistory.toList()
        )

        private fun restoreSnapshot(s: Snapshot) {
            projects.apply { clear(); addAll(s.projects) }
            personalGoals.apply { clear(); addAll(s.personalGoals) }
            events.apply { clear(); addAll(s.events) }
            tasks.apply { clear(); addAll(s.tasks) }
            subtasks.apply { clear(); addAll(s.subtasks) }
            appointments.apply { clear(); addAll(s.appointments) }
            scheduleChecklistItems.apply { clear(); addAll(s.scheduleChecklistItems) }
            taskRecurrences.apply { clear(); addAll(s.taskRecurrences) }
            transactions.apply { clear(); addAll(s.transactions) }
            auditHistory.apply { clear(); addAll(s.auditHistory) }
        }

        override suspend fun <T> inTransaction(block: suspend () -> T): T {
            val snap = snapshot()
            return try {
                block()
            } catch (ex: Exception) {
                restoreSnapshot(snap)
                throw ex
            }
        }

        override suspend fun findRecordedOperationState(operationId: String): StructuredProcessingState? {
            val entry = auditHistory.firstOrNull {
                it.entityType == RoomStructuredInputGateway.AUDIT_ENTITY_TYPE &&
                    it.entityId == operationId &&
                    (it.action == StructuredProcessingState.APPLIED.name ||
                        it.action == StructuredProcessingState.UPDATED.name ||
                        it.action == StructuredProcessingState.CONFLICT_PRESERVED_LOCAL.name)
            } ?: return null
            return runCatching { StructuredProcessingState.valueOf(entry.action) }.getOrNull()
        }

        override suspend fun recordOperationOutcome(
            operationId: String,
            state: StructuredProcessingState,
            detail: String,
            occurredAt: Long
        ) {
            if (failOnAuditRecord) {
                throw IllegalStateException("Simulated SQLite audit_history write failure")
            }
            if (state != StructuredProcessingState.APPLIED &&
                state != StructuredProcessingState.UPDATED &&
                state != StructuredProcessingState.CONFLICT_PRESERVED_LOCAL
            ) {
                return
            }
            auditHistory.add(
                AuditHistoryEntity(
                    id = "audit-$operationId-${auditHistory.size + 1}",
                    entityType = RoomStructuredInputGateway.AUDIT_ENTITY_TYPE,
                    entityId = operationId,
                    action = state.name,
                    summary = detail,
                    occurredAt = occurredAt,
                    createdAt = occurredAt,
                    updatedAt = occurredAt
                )
            )
        }

        override suspend fun categoryExists(id: String): Boolean = categories.any { it.id == id }
        override suspend fun projectExists(id: String): Boolean = projects.any { it.id == id }
        override suspend fun personalGoalExists(id: String): Boolean = personalGoals.any { it.id == id }
        override suspend fun eventExists(id: String): Boolean = events.any { it.id == id }
        override suspend fun taskExists(id: String): Boolean = tasks.any { it.id == id }
        override suspend fun subtaskExists(id: String): Boolean = subtasks.any { it.id == id }
        override suspend fun appointmentExists(id: String): Boolean = appointments.any { it.id == id }
        override suspend fun transactionExists(id: String): Boolean = transactions.any { it.id == id }
        override suspend fun taskRecurrenceExists(id: String): Boolean = taskRecurrences.any { it.id == id }
        override suspend fun creditCardExists(id: String): Boolean = creditCards.any { it.id == id }
        override suspend fun installmentPurchaseExists(id: String): Boolean = installmentPurchases.any { it.id == id }
        override suspend fun recurringRuleExists(id: String): Boolean = recurringRules.any { it.id == id }

        override suspend fun getTaskById(id: String): TaskEntity? = tasks.firstOrNull { it.id == id }
        override suspend fun getSubtaskById(id: String): SubtaskEntity? = subtasks.firstOrNull { it.id == id }
        override suspend fun getSubtasksByTaskId(taskId: String): List<SubtaskEntity> =
            subtasks.filter { it.taskId == taskId }.sortedWith(compareBy<SubtaskEntity> { it.sortOrder }.thenBy { it.createdAt })
        override suspend fun getAppointmentById(id: String): AppointmentEntity? = appointments.firstOrNull { it.id == id }
        override suspend fun getEventById(id: String): EventEntity? = events.firstOrNull { it.id == id }
        override suspend fun getTaskRecurrenceById(id: String): TaskRecurrenceEntity? =
            taskRecurrences.firstOrNull { it.id == id }
        override suspend fun getScheduleChecklistItemsByAppointmentId(appointmentId: String): List<ScheduleChecklistItemEntity> =
            scheduleChecklistItems.filter { it.appointmentId == appointmentId }
                .sortedWith(compareBy<ScheduleChecklistItemEntity> { it.sortOrder }.thenBy { it.createdAt })
        override suspend fun getScheduleChecklistItemsByEventId(eventId: String): List<ScheduleChecklistItemEntity> =
            scheduleChecklistItems.filter { it.eventId == eventId }
                .sortedWith(compareBy<ScheduleChecklistItemEntity> { it.sortOrder }.thenBy { it.createdAt })
        override suspend fun getProjectById(id: String): ProjectEntity? = projects.firstOrNull { it.id == id }
        override suspend fun getPersonalGoalById(id: String): PersonalGoalEntity? = personalGoals.firstOrNull { it.id == id }
        override suspend fun getTransactionById(id: String): TransactionEntity? = transactions.firstOrNull { it.id == id }

        override suspend fun getTaskByRecurrenceAndDate(
            recurrenceId: String,
            occurrenceDateIso: String
        ): TaskEntity? = tasks.firstOrNull {
            it.recurrenceId == recurrenceId && it.occurrenceDateIso == occurrenceDateIso
        }

        override suspend fun getStatementById(id: String): CreditCardStatementEntity? =
            statements.firstOrNull { it.id == id }

        override suspend fun getAllStatementsOnce(): List<CreditCardStatementEntity> =
            statements.toList()

        override suspend fun getTransactionByRecurringOccurrence(
            ruleId: String,
            occurrenceIndex: Int
        ): TransactionEntity? = transactions.firstOrNull {
            it.recurringRuleId == ruleId && it.occurrenceIndex == occurrenceIndex
        }

        override suspend fun getTransactionsByInstallmentPurchase(purchaseId: String): List<TransactionEntity> =
            transactions.filter { it.installmentPurchaseId == purchaseId }

        override suspend fun insertTask(entity: TaskEntity) {
            if (failOnEntityInsert == StructuredEntityType.TASK) {
                throw IllegalStateException("Simulated SQLite insert failure for TASK")
            }
            tasks.removeAll { it.id == entity.id }
            tasks.add(entity)
        }

        override suspend fun insertSubtask(entity: SubtaskEntity) {
            if (failOnEntityInsert == StructuredEntityType.CHECKLIST) {
                throw IllegalStateException("Simulated SQLite insert failure for CHECKLIST")
            }
            subtasks.removeAll { it.id == entity.id }
            subtasks.add(entity)
        }

        override suspend fun insertSubtasks(entities: List<SubtaskEntity>) {
            entities.forEach { insertSubtask(it) }
        }

        override suspend fun insertAppointment(entity: AppointmentEntity) {
            if (failOnEntityInsert == StructuredEntityType.APPOINTMENT) {
                throw IllegalStateException("Simulated SQLite insert failure for APPOINTMENT")
            }
            appointments.removeAll { it.id == entity.id }
            appointments.add(entity)
        }

        override suspend fun insertEvent(entity: EventEntity) {
            if (failOnEntityInsert == StructuredEntityType.EVENT) {
                throw IllegalStateException("Simulated SQLite insert failure for EVENT")
            }
            events.removeAll { it.id == entity.id }
            events.add(entity)
        }

        override suspend fun insertTaskRecurrence(entity: TaskRecurrenceEntity) {
            if (failOnEntityInsert == StructuredEntityType.RECURRENCE) {
                throw IllegalStateException("Simulated SQLite insert failure for RECURRENCE")
            }
            taskRecurrences.removeAll { it.id == entity.id }
            taskRecurrences.add(entity)
        }

        override suspend fun insertScheduleChecklistItems(entities: List<ScheduleChecklistItemEntity>) {
            entities.forEach { entity ->
                scheduleChecklistItems.removeAll { it.id == entity.id }
                scheduleChecklistItems.add(entity)
            }
        }

        override suspend fun insertProject(entity: ProjectEntity) {
            if (failOnEntityInsert == StructuredEntityType.PROJECT) {
                throw IllegalStateException("Simulated SQLite insert failure for PROJECT")
            }
            projects.removeAll { it.id == entity.id }
            projects.add(entity)
        }

        override suspend fun insertPersonalGoal(entity: PersonalGoalEntity) {
            if (failOnEntityInsert == StructuredEntityType.PERSONAL_GOAL) {
                throw IllegalStateException("Simulated SQLite insert failure for PERSONAL_GOAL")
            }
            personalGoals.removeAll { it.id == entity.id }
            personalGoals.add(entity)
        }

        override suspend fun insertTransaction(entity: TransactionEntity) {
            if (failOnEntityInsert == StructuredEntityType.TRANSACTION) {
                throw IllegalStateException("Simulated SQLite insert failure for TRANSACTION")
            }
            transactions.removeAll { it.id == entity.id }
            transactions.add(entity)
        }
    }

    private val t0 = 1_795_000_000_000L

    private fun seedLocalContext(gateway: InMemoryStructuredInputGateway) {
        gateway.categories.add(
            CategoryEntity(
                id = "cat-work",
                name = "Trabalho",
                colorHex = "#4C8BF5",
                scope = CategoryScope.GENERAL,
                createdAt = t0,
                updatedAt = t0
            )
        )
        gateway.creditCards.add(
            CreditCardEntity(
                id = "card-main",
                name = "Cartão Principal",
                totalLimitCents = 500_000L,
                availableLimitCents = 400_000L,
                createdAt = t0,
                updatedAt = t0
            )
        )
        gateway.statements.add(
            CreditCardStatementEntity(
                id = "stmt-open",
                creditCardId = "card-main",
                yearMonth = "2026-11",
                closingAt = t0,
                dueAt = t0 + 7 * 86_400_000L,
                status = StatementStatus.OPEN,
                createdAt = t0,
                updatedAt = t0
            )
        )
        gateway.statements.add(
            CreditCardStatementEntity(
                id = "stmt-paid",
                creditCardId = "card-main",
                yearMonth = "2026-10",
                closingAt = t0 - 30 * 86_400_000L,
                dueAt = t0 - 20 * 86_400_000L,
                status = StatementStatus.PAID,
                paidAt = t0 - 18 * 86_400_000L,
                paidAmountCents = 25_000L,
                createdAt = t0,
                updatedAt = t0
            )
        )
    }

    // Scenario 1: Valid v1 JSON/envelope persists all 6 supported entity types faithfully
    @Test
    fun `valid contract v1 persists all six supported entity types in Room entities`() = runBlocking {
        val gateway = InMemoryStructuredInputGateway()
        seedLocalContext(gateway)
        val processor = StructuredInputProcessor(gateway) { t0 }

        val json = """
            {
              "contractVersion": 1,
              "source": "github_sync_stage3_contract",
              "createdAt": $t0,
              "operations": [
                {
                  "operationId": "op-goal-1",
                  "entityType": "PERSONAL_GOAL",
                  "data": {
                    "id": "goal-ing-1",
                    "title": "Certificação Cloud",
                    "description": "Concluir trilha oficial",
                    "categoryId": "cat-work",
                    "status": "ACTIVE",
                    "progressMode": "MANUAL",
                    "manualProgressPercent": 20
                  }
                },
                {
                  "operationId": "op-proj-1",
                  "entityType": "PROJECT",
                  "data": {
                    "id": "proj-ing-1",
                    "name": "NEXUS Etapa 3",
                    "status": "ACTIVE",
                    "priority": "HIGH",
                    "goalId": "goal-ing-1"
                  }
                },
                {
                  "operationId": "op-event-1",
                  "entityType": "EVENT",
                  "data": {
                    "id": "ev-ing-1",
                    "name": "Conferência Kotlin",
                    "categoryId": "cat-work",
                    "startAt": ${t0 + 86_400_000L},
                    "endAt": ${t0 + 90_000_000L},
                    "locationName": "Auditório Central",
                    "eventStatus": "CONFIRMED",
                    "ticketStatus": "FREE"
                  }
                },
                {
                  "operationId": "op-task-1",
                  "entityType": "TASK",
                  "data": {
                    "id": "task-ing-1",
                    "title": "Revisar contrato JSON v1",
                    "projectId": "proj-ing-1",
                    "categoryId": "cat-work",
                    "eventId": "ev-ing-1",
                    "goalId": "goal-ing-1",
                    "priority": "URGENT",
                    "status": "IN_PROGRESS",
                    "dueAt": ${t0 + 3_600_000L},
                    "hasDueTime": true
                  }
                },
                {
                  "operationId": "op-appt-1",
                  "entityType": "APPOINTMENT",
                  "data": {
                    "id": "appt-ing-1",
                    "title": "Alinhamento semanal",
                    "categoryId": "cat-work",
                    "eventId": "ev-ing-1",
                    "startAt": ${t0 + 7_200_000L},
                    "endAt": ${t0 + 10_800_000L},
                    "location": "Online"
                  }
                },
                {
                  "operationId": "op-tx-1",
                  "entityType": "TRANSACTION",
                  "data": {
                    "id": "tx-ing-1",
                    "type": "EXPENSE",
                    "description": "Taxa de inscrição",
                    "amountCents": 15900,
                    "categoryId": "cat-work",
                    "status": "PENDING",
                    "paymentMethod": "CREDIT_CARD",
                    "occurredAt": $t0,
                    "projectId": "proj-ing-1",
                    "eventId": "ev-ing-1",
                    "taskId": "task-ing-1",
                    "creditCardId": "card-main",
                    "statementId": "stmt-open"
                  }
                }
              ]
            }
        """.trimIndent()

        val result = processor.processJson(json, atomicBatch = true)

        assertNull(result.envelopeError)
        assertFalse(result.hasFailures)
        assertEquals(6, result.totalOperations)
        assertEquals(6, result.appliedCount)
        assertEquals(0, result.rejectedCount)
        assertEquals(0, result.failedRolledBackCount)
        assertTrue(result.receipts.all { it.processingState == StructuredProcessingState.APPLIED })

        assertEquals("Certificação Cloud", gateway.personalGoals.single().title)
        assertEquals(GoalProgressMode.MANUAL, gateway.personalGoals.single().progressMode)
        assertEquals(20, gateway.personalGoals.single().manualProgressPercent)
        assertEquals("goal-ing-1", gateway.projects.single().goalId)
        assertEquals(EventLifecycleStatus.CONFIRMED, gateway.events.single().eventStatus)
        assertEquals(TaskPriority.URGENT, gateway.tasks.single().priority)
        assertEquals("proj-ing-1", gateway.tasks.single().projectId)
        assertEquals("Online", gateway.appointments.single().location)
        assertEquals(15900L, gateway.transactions.single().amountCents)
        assertEquals("stmt-open", gateway.transactions.single().statementId)
        assertEquals(6, gateway.auditHistory.count { it.action == StructuredProcessingState.APPLIED.name })
    }

    // Scenario 2: Malformed JSON, incompatible contractVersion, missing source, unknown keys & negative values
    @Test
    fun `invalid contract version malformed json and unknown fields are rejected without mutating db`() = runBlocking {
        val gateway = InMemoryStructuredInputGateway()
        seedLocalContext(gateway)
        val processor = StructuredInputProcessor(gateway) { t0 }

        val malformed = processor.processJson("{not-valid-json")
        assertNotNull(malformed.envelopeError)
        assertTrue(malformed.hasFailures)

        val unknownKey = processor.processJson(
            """
            {
              "contractVersion": 1,
              "source": "external_inbox",
              "operations": [
                {
                  "operationId": "op-unknown-field",
                  "entityType": "TASK",
                  "data": { "title": "Teste", "unexpectedField": "boom" }
                }
              ]
            }
            """.trimIndent()
        )
        assertEquals(1, unknownKey.rejectedCount)
        assertEquals(StructuredProcessingState.REJECTED, unknownKey.receipts.single().processingState)

        val wrongVersion = processor.processEnvelope(
            StructuredInputEnvelope(
                contractVersion = 2,
                source = "external_inbox",
                createdAt = t0,
                operations = listOf(
                    StructuredInputOperation(
                        operationId = "op-v2",
                        entityType = "TASK",
                        contractVersion = 2,
                        data = buildJsonObject { put("title", "Incompatível") }
                    )
                )
            )
        )
        assertNotNull(wrongVersion.envelopeError)
        assertEquals(1, wrongVersion.rejectedCount)

        val negativeAmount = processor.processJson(
            """
            {
              "contractVersion": 1,
              "source": "external_inbox",
              "operations": [
                {
                  "operationId": "op-neg-tx",
                  "entityType": "TRANSACTION",
                  "data": {
                    "type": "EXPENSE",
                    "description": "Valor inválido",
                    "amountCents": -500,
                    "occurredAt": $t0
                  }
                }
              ]
            }
            """.trimIndent()
        )
        assertEquals(1, negativeAmount.rejectedCount)
        assertEquals(StructuredProcessingState.REJECTED, negativeAmount.receipts.single().processingState)

        assertTrue(gateway.tasks.isEmpty())
        assertTrue(gateway.transactions.isEmpty())
        assertTrue(gateway.auditHistory.isEmpty())
    }

    // Scenario 3: Duplicate operationId is idempotent and returns DUPLICATE_IGNORED
    @Test
    fun `duplicate operationId returns DUPLICATE_IGNORED without creating duplicate entity`() = runBlocking {
        val gateway = InMemoryStructuredInputGateway()
        seedLocalContext(gateway)
        val processor = StructuredInputProcessor(gateway) { t0 }

        val envelope = StructuredInputEnvelope(
            contractVersion = 1,
            source = "inbox_sync",
            createdAt = t0,
            operations = listOf(
                StructuredInputOperation(
                    operationId = "op-idem-1",
                    entityType = "TASK",
                    data = buildJsonObject {
                        put("title", "Tarefa Idempotente")
                        put("priority", "HIGH")
                    }
                )
            )
        )

        val first = processor.processEnvelope(envelope)
        assertEquals(1, first.appliedCount)
        assertEquals(1, gateway.tasks.size)

        val second = processor.processEnvelope(envelope)
        assertEquals(0, second.appliedCount)
        assertEquals(1, second.duplicateIgnoredCount)
        assertEquals(StructuredProcessingState.DUPLICATE_IGNORED, second.receipts.single().processingState)
        assertEquals(1, gateway.tasks.size)

        // Same operationId repeated twice in one batch
        val sameBatchDup = processor.processEnvelope(
            StructuredInputEnvelope(
                contractVersion = 1,
                source = "inbox_sync",
                createdAt = t0,
                operations = listOf(
                    StructuredInputOperation(
                        operationId = "op-idem-2",
                        entityType = "PROJECT",
                        data = buildJsonObject { put("name", "Projeto Único") }
                    ),
                    StructuredInputOperation(
                        operationId = "op-idem-2",
                        entityType = "PROJECT",
                        data = buildJsonObject { put("name", "Projeto Único") }
                    )
                )
            )
        )
        assertEquals(1, sameBatchDup.appliedCount)
        assertEquals(1, sameBatchDup.duplicateIgnoredCount)
        assertEquals(1, gateway.projects.size)
    }

    // Scenario 4: Unsupported entityType is rejected cleanly
    @Test
    fun `unsupported entityType is rejected while valid sibling operations succeed in non-atomic mode`() = runBlocking {
        val gateway = InMemoryStructuredInputGateway()
        val processor = StructuredInputProcessor(gateway) { t0 }

        val result = processor.processEnvelope(
            StructuredInputEnvelope(
                contractVersion = 1,
                source = "inbox_sync",
                createdAt = t0,
                operations = listOf(
                    StructuredInputOperation(
                        operationId = "op-bad-type",
                        entityType = "UNKNOWN_DOMAIN_OBJECT",
                        data = buildJsonObject { put("title", "Algo") }
                    ),
                    StructuredInputOperation(
                        operationId = "op-valid-task",
                        entityType = "TASK",
                        data = buildJsonObject { put("title", "Tarefa Válida") }
                    )
                )
            ),
            atomicBatch = false
        )

        assertEquals(1, result.rejectedCount)
        assertEquals(1, result.appliedCount)
        assertEquals(StructuredProcessingState.REJECTED, result.receipts[0].processingState)
        assertEquals(StructuredProcessingState.APPLIED, result.receipts[1].processingState)
        assertEquals(1, gateway.tasks.size)
    }

    // Scenario 5: Missing required fields, orphan FKs, PAID statement guard & anti-double-counting guard
    @Test
    fun `missing required fields orphan FKs and invalid financial rules are strictly rejected`() = runBlocking {
        val gateway = InMemoryStructuredInputGateway()
        seedLocalContext(gateway)
        val processor = StructuredInputProcessor(gateway) { t0 }

        val result = processor.processEnvelope(
            StructuredInputEnvelope(
                contractVersion = 1,
                source = "finance_sync",
                createdAt = t0,
                operations = listOf(
                    // Missing title
                    StructuredInputOperation(
                        operationId = "op-blank-task",
                        entityType = "TASK",
                        data = buildJsonObject { put("title", "   ") }
                    ),
                    // Orphan project FK
                    StructuredInputOperation(
                        operationId = "op-orphan-fk",
                        entityType = "TASK",
                        data = buildJsonObject {
                            put("title", "Tarefa órfã")
                            put("projectId", "proj-does-not-exist")
                        }
                    ),
                    // Pending transaction into PAID statement
                    StructuredInputOperation(
                        operationId = "op-paid-stmt-block",
                        entityType = "TRANSACTION",
                        data = buildJsonObject {
                            put("type", "EXPENSE")
                            put("description", "Compra tardia")
                            put("amountCents", 3000)
                            put("status", "PENDING")
                            put("paymentMethod", "CREDIT_CARD")
                            put("occurredAt", t0)
                            put("creditCardId", "card-main")
                            put("statementId", "stmt-paid")
                        }
                    ),
                    // Standalone statement payment expense (double-counting guard)
                    StructuredInputOperation(
                        operationId = "op-double-stmt-expense",
                        entityType = "TRANSACTION",
                        data = buildJsonObject {
                            put("type", "EXPENSE")
                            put("description", "Pagamento de fatura Cartão Principal")
                            put("amountCents", 25000)
                            put("status", "SETTLED")
                            put("paymentMethod", "PIX")
                            put("occurredAt", t0)
                            put("creditCardId", "card-main")
                        }
                    )
                )
            ),
            atomicBatch = false
        )

        assertEquals(4, result.rejectedCount)
        assertEquals(0, result.appliedCount)
        assertTrue(result.receipts.all { it.processingState == StructuredProcessingState.REJECTED })
        assertTrue(result.receipts.all { !it.rejectionReason.isNullOrBlank() })
        assertTrue(gateway.tasks.isEmpty())
        assertTrue(gateway.transactions.isEmpty())
    }

    // Scenario 6: Conflicts preserve local records intact and mark CONFLICT_PRESERVED_LOCAL
    @Test
    fun `existing local IDs or natural keys preserve local data and return CONFLICT_PRESERVED_LOCAL`() = runBlocking {
        val gateway = InMemoryStructuredInputGateway()
        seedLocalContext(gateway)
        gateway.tasks.add(
            TaskEntity(
                id = "task-local-locked",
                title = "Versão Local Intacta",
                status = TaskStatus.COMPLETED,
                priority = TaskPriority.URGENT,
                createdAt = t0,
                updatedAt = t0
            )
        )
        gateway.recurringRules.add(
            RecurringRuleEntity(
                id = "rule-local-1",
                title = "Aluguel",
                type = TransactionType.EXPENSE,
                amountCents = 200_000L,
                frequency = RecurrenceFrequency.MONTHLY,
                startAt = t0,
                createdAt = t0,
                updatedAt = t0
            )
        )
        gateway.transactions.add(
            TransactionEntity(
                id = "tx-occ-local",
                type = TransactionType.EXPENSE,
                description = "Aluguel Mês 1",
                amountCents = 200_000L,
                occurredAt = t0,
                recurringRuleId = "rule-local-1",
                occurrenceIndex = 1,
                createdAt = t0,
                updatedAt = t0
            )
        )

        val processor = StructuredInputProcessor(gateway) { t0 }
        val result = processor.processEnvelope(
            StructuredInputEnvelope(
                contractVersion = 1,
                source = "external_mirror",
                createdAt = t0,
                operations = listOf(
                    StructuredInputOperation(
                        operationId = "op-conflict-id",
                        entityType = "TASK",
                        data = buildJsonObject {
                            put("id", "task-local-locked")
                            put("title", "Sobrescrita Externa Indevida")
                            put("status", "NOT_STARTED")
                        }
                    ),
                    StructuredInputOperation(
                        operationId = "op-conflict-rec-occ",
                        entityType = "TRANSACTION",
                        data = buildJsonObject {
                            put("id", "tx-occ-new-id")
                            put("type", "EXPENSE")
                            put("description", "Aluguel duplicado")
                            put("amountCents", 200000)
                            put("occurredAt", t0)
                            put("recurringRuleId", "rule-local-1")
                            put("occurrenceIndex", 1)
                        }
                    )
                )
            )
        )

        assertEquals(2, result.conflictPreservedLocalCount)
        assertEquals(0, result.appliedCount)
        assertTrue(result.receipts.all { it.processingState == StructuredProcessingState.CONFLICT_PRESERVED_LOCAL })
        assertEquals("Versão Local Intacta", gateway.tasks.single().title)
        assertEquals(TaskStatus.COMPLETED, gateway.tasks.single().status)
        assertEquals(1, gateway.transactions.size)

        // Retrying the conflicted operationId later is recognized as idempotent duplicate
        val retry = processor.processEnvelope(
            StructuredInputEnvelope(
                contractVersion = 1,
                source = "external_mirror",
                createdAt = t0,
                operations = listOf(
                    StructuredInputOperation(
                        operationId = "op-conflict-id",
                        entityType = "TASK",
                        data = buildJsonObject {
                            put("id", "task-local-locked")
                            put("title", "Tentativa 2")
                        }
                    )
                )
            )
        )
        assertEquals(1, retry.duplicateIgnoredCount)
        assertEquals(StructuredProcessingState.DUPLICATE_IGNORED, retry.receipts.single().processingState)
    }

    // Scenario 7 & 8: Transactional rollback on failure (single & atomicBatch) + safe retry after rollback
    @Test
    fun `persistence failure rolls back completely without APPLIED receipts and allows safe retry`() = runBlocking {
        val gateway = InMemoryStructuredInputGateway()
        seedLocalContext(gateway)
        val processor = StructuredInputProcessor(gateway) { t0 }

        // Case A: atomicBatch = true fails on audit write after inserting project & task -> entire batch rolls back
        gateway.failOnAuditRecord = true
        val batchEnvelope = StructuredInputEnvelope(
            contractVersion = 1,
            source = "retry_test_source",
            createdAt = t0,
            operations = listOf(
                StructuredInputOperation(
                    operationId = "op-batch-proj",
                    entityType = "PROJECT",
                    data = buildJsonObject {
                        put("id", "proj-atomic-1")
                        put("name", "Projeto Atômico")
                    }
                ),
                StructuredInputOperation(
                    operationId = "op-batch-task",
                    entityType = "TASK",
                    data = buildJsonObject {
                        put("id", "task-atomic-1")
                        put("title", "Tarefa Atômica")
                        put("projectId", "proj-atomic-1")
                    }
                )
            )
        )

        val failedBatch = processor.processEnvelope(batchEnvelope, atomicBatch = true)
        assertTrue(failedBatch.hasFailures)
        assertEquals(0, failedBatch.appliedCount)
        assertEquals(2, failedBatch.failedRolledBackCount)
        assertTrue(failedBatch.receipts.all { it.processingState == StructuredProcessingState.FAILED_ROLLED_BACK })
        assertTrue(gateway.projects.isEmpty())
        assertTrue(gateway.tasks.isEmpty())
        assertTrue(gateway.auditHistory.isEmpty())

        // Case B: Clear transient failure -> safe reprocessing of the exact same operationIds succeeds!
        gateway.failOnAuditRecord = false
        val retriedBatch = processor.processEnvelope(batchEnvelope, atomicBatch = true)
        assertFalse(retriedBatch.hasFailures)
        assertEquals(2, retriedBatch.appliedCount)
        assertEquals(0, retriedBatch.failedRolledBackCount)
        assertEquals("proj-atomic-1", gateway.projects.single().id)
        assertEquals("proj-atomic-1", gateway.tasks.single().projectId)

        // Case C: atomicBatch = true rolls back earlier valid op when later op is rejected semantically
        val semanticAbortEnvelope = StructuredInputEnvelope(
            contractVersion = 1,
            source = "retry_test_source",
            createdAt = t0,
            operations = listOf(
                StructuredInputOperation(
                    operationId = "op-will-rollback-on-abort",
                    entityType = "TASK",
                    data = buildJsonObject { put("title", "Não deve permanecer") }
                ),
                StructuredInputOperation(
                    operationId = "op-semantic-reject",
                    entityType = "TRANSACTION",
                    data = buildJsonObject {
                        put("type", "EXPENSE")
                        put("description", "Falta categoria inexistente")
                        put("amountCents", 1000)
                        put("occurredAt", t0)
                        put("categoryId", "cat-missing")
                    }
                )
            )
        )
        val aborted = processor.processEnvelope(semanticAbortEnvelope, atomicBatch = true)
        assertNotNull(aborted.envelopeError)
        assertEquals(0, aborted.appliedCount)
        assertEquals(StructuredProcessingState.FAILED_ROLLED_BACK, aborted.receipts[0].processingState)
        assertEquals(StructuredProcessingState.REJECTED, aborted.receipts[1].processingState)
        assertEquals(1, gateway.tasks.size) // only task-atomic-1 from Case B
        assertTrue(gateway.transactions.isEmpty())

        // Case D: Single-operation failure (atomicBatch = false) rolls back and can be retried cleanly
        gateway.failOnEntityInsert = StructuredEntityType.APPOINTMENT
        val singleOpEnvelope = StructuredInputEnvelope(
            contractVersion = 1,
            source = "retry_test_source",
            createdAt = t0,
            operations = listOf(
                StructuredInputOperation(
                    operationId = "op-single-retry",
                    entityType = "APPOINTMENT",
                    data = buildJsonObject {
                        put("id", "appt-retry-1")
                        put("title", "Consulta Médica")
                        put("startAt", t0 + 10_000L)
                        put("endAt", t0 + 20_000L)
                    }
                )
            )
        )
        val failedSingle = processor.processEnvelope(singleOpEnvelope, atomicBatch = false)
        assertEquals(1, failedSingle.failedRolledBackCount)
        assertEquals(0, failedSingle.appliedCount)
        assertTrue(gateway.appointments.isEmpty())

        gateway.failOnEntityInsert = null
        val retriedSingle = processor.processEnvelope(singleOpEnvelope, atomicBatch = false)
        assertEquals(1, retriedSingle.appliedCount)
        assertEquals("appt-retry-1", gateway.appointments.single().id)

        // Subsequent retry after APPLIED is idempotent DUPLICATE_IGNORED
        val duplicateAfterSuccess = processor.processEnvelope(singleOpEnvelope, atomicBatch = false)
        assertEquals(1, duplicateAfterSuccess.duplicateIgnoredCount)
        assertEquals(1, gateway.appointments.size)
    }

    @Test
    fun `task with inline subtasks and standalone CHECKLIST operations persist linked subtasks`() = runBlocking {
        val gateway = InMemoryStructuredInputGateway()
        seedLocalContext(gateway)
        val processor = StructuredInputProcessor(gateway) { t0 }

        val createResult = processor.processEnvelope(
            StructuredInputEnvelope(
                contractVersion = 1,
                source = "chatgpt_inbox",
                createdAt = t0,
                operations = listOf(
                    StructuredInputOperation(
                        operationId = "op-task-subtasks",
                        entityType = "TASK",
                        data = buildJsonObject {
                            put("id", "task-checklist-1")
                            put("title", "Organizar viagem")
                            put("subtasks", buildJsonArray {
                                addJsonObject {
                                    put("title", "Reservar hotel")
                                    put("isCompleted", false)
                                }
                                addJsonObject {
                                    put("title", "Comprar passagem")
                                    put("isCompleted", true)
                                }
                            })
                        }
                    ),
                    StructuredInputOperation(
                        operationId = "op-checklist-add-update",
                        entityType = "CHECKLIST",
                        action = "UPSERT",
                        data = buildJsonObject {
                            put("taskId", "task-checklist-1")
                            put("items", buildJsonArray {
                                addJsonObject {
                                    put("title", "Reservar hotel")
                                    put("isCompleted", true)
                                }
                                addJsonObject {
                                    put("title", "Separar documentos")
                                    put("isCompleted", false)
                                }
                            })
                        }
                    )
                )
            )
        )

        assertFalse(createResult.hasFailures)
        assertEquals(1, createResult.appliedCount)
        assertEquals(1, createResult.updatedCount)
        assertEquals(2, createResult.totalAppliedCount)
        val subs = gateway.getSubtasksByTaskId("task-checklist-1")
        assertEquals(3, subs.size)
        assertTrue(subs.first { it.title == "Reservar hotel" }.isCompleted)
        assertTrue(subs.any { it.title == "Separar documentos" })
    }

    @Test
    fun `UPDATE and UPSERT preserve createdAt while CREATE preserves local conflict`() = runBlocking {
        val gateway = InMemoryStructuredInputGateway()
        seedLocalContext(gateway)
        gateway.tasks.add(
            TaskEntity(
                id = "task-existing",
                title = "Título original",
                status = TaskStatus.NOT_STARTED,
                createdAt = t0 - 50_000L,
                updatedAt = t0 - 10_000L
            )
        )
        val processor = StructuredInputProcessor(gateway) { t0 }

        val res = processor.processEnvelope(
            StructuredInputEnvelope(
                contractVersion = 1,
                source = "chatgpt_inbox",
                createdAt = t0,
                operations = listOf(
                    StructuredInputOperation(
                        operationId = "op-create-conflict",
                        entityType = "TASK",
                        action = "CREATE",
                        data = buildJsonObject {
                            put("id", "task-existing")
                            put("title", "Não sobrescrever no CREATE")
                        }
                    ),
                    StructuredInputOperation(
                        operationId = "op-update-missing",
                        entityType = "TASK",
                        action = "UPDATE",
                        data = buildJsonObject {
                            put("id", "task-inexistente")
                            put("title", "Não existe")
                        }
                    ),
                    StructuredInputOperation(
                        operationId = "op-update-existing",
                        entityType = "TASK",
                        action = "UPDATE",
                        data = buildJsonObject {
                            put("id", "task-existing")
                            put("title", "Título atualizado")
                            put("status", "COMPLETED")
                        }
                    )
                )
            )
        )

        assertEquals(1, res.conflictPreservedLocalCount)
        assertEquals(1, res.rejectedCount)
        assertEquals(1, res.updatedCount)
        val updated = gateway.getTaskById("task-existing")!!
        assertEquals("Título atualizado", updated.title)
        assertEquals(TaskStatus.COMPLETED, updated.status)
        assertEquals(t0 - 50_000L, updated.createdAt)
        assertEquals(t0, updated.updatedAt)
    }

    @Test
    fun `previewEnvelope performs zero DB writes while validating intra-batch links`() = runBlocking {
        val gateway = InMemoryStructuredInputGateway()
        seedLocalContext(gateway)
        gateway.tasks.add(
            TaskEntity(
                id = "task-local-update",
                title = "Revisar contas",
                createdAt = t0 - 5_000L,
                updatedAt = t0 - 5_000L
            )
        )
        val processor = StructuredInputProcessor(gateway) { t0 }

        val preview = processor.previewEnvelope(
            StructuredInputEnvelope(
                contractVersion = 1,
                source = "chatgpt_inbox",
                createdAt = t0,
                operations = listOf(
                    StructuredInputOperation(
                        operationId = "op-p1",
                        entityType = "TASK",
                        data = buildJsonObject {
                            put("id", "task-batch-new")
                            put("title", "Pagar inscrição")
                        }
                    ),
                    StructuredInputOperation(
                        operationId = "op-p2",
                        entityType = "TRANSACTION",
                        data = buildJsonObject {
                            put("id", "tx-batch-linked")
                            put("type", "EXPENSE")
                            put("description", "Taxa de inscrição")
                            put("amountCents", 12000)
                            put("occurredAt", t0)
                            put("taskId", "task-batch-new")
                        }
                    ),
                    StructuredInputOperation(
                        operationId = "op-p3",
                        entityType = "CHECKLIST",
                        action = "UPSERT",
                        data = buildJsonObject {
                            put("taskId", "task-local-update")
                            put("title", "Conferir extrato")
                            put("isCompleted", false)
                        }
                    ),
                    StructuredInputOperation(
                        operationId = "op-p4",
                        entityType = "TASK",
                        data = buildJsonObject {
                            put("id", "task-local-update")
                            put("title", "Conflito esperado")
                        }
                    ),
                    StructuredInputOperation(
                        operationId = "op-p5",
                        entityType = "TRANSACTION",
                        data = buildJsonObject {
                            put("type", "EXPENSE")
                            put("description", "Valor inválido")
                            put("amountCents", 0)
                            put("occurredAt", t0)
                        }
                    )
                )
            )
        )

        assertEquals(2, preview.willCreateCount)
        assertEquals(1, preview.willUpdateCount)
        assertEquals(1, preview.conflictPreservedLocalCount)
        assertEquals(1, preview.rejectedCount)
        assertEquals(StructuredPreviewOutcome.WILL_CREATE, preview.items[1].outcome)
        assertEquals(StructuredPreviewOutcome.WILL_UPDATE, preview.items[2].outcome)
        // Zero mutations occurred during dry-run preview
        assertEquals(1, gateway.tasks.size)
        assertTrue(gateway.transactions.isEmpty())
        assertTrue(gateway.subtasks.isEmpty())
        assertTrue(gateway.auditHistory.isEmpty())
    }

    @Test
    fun `RECURRENCE CREATE and non-retroactive UPSERT work for TASK APPOINTMENT and EVENT with appointment event checklists`() = runBlocking {
        val gateway = InMemoryStructuredInputGateway()
        seedLocalContext(gateway)
        val processor = StructuredInputProcessor(gateway) { t0 }

        val createJson = """
            {
              "contractVersion": 1,
              "source": "chatgpt_inbox",
              "createdAt": $t0,
              "operations": [
                {
                  "operationId": "op-rec-task",
                  "entityType": "RECURRENCE",
                  "action": "CREATE",
                  "data": {
                    "id": "rec-review-task",
                    "domain": "TASK",
                    "title": "Fechamento semanal",
                    "frequency": "WEEKLY",
                    "startDateIso": "2026-11-02",
                    "hasDueTime": true,
                    "startTime": "09:30",
                    "templateChecklist": ["Conferir extratos", "Revisar faturas"]
                  }
                },
                {
                  "operationId": "op-rec-appt",
                  "entityType": "RECURRENCE",
                  "action": "CREATE",
                  "data": {
                    "id": "rec-medical-appt",
                    "domain": "APPOINTMENT",
                    "title": "Fisioterapia",
                    "frequency": "SPECIFIC_WEEKDAYS",
                    "weekdays": ["MON", "WED"],
                    "startDateIso": "2026-11-02",
                    "hasDueTime": true,
                    "dueTimeMinutesOfDay": 840,
                    "durationMillis": 3600000,
                    "location": "Clínica Centro",
                    "templateChecklist": ["Levar exame", "Água"]
                  }
                },
                {
                  "operationId": "op-rec-ev",
                  "entityType": "RECURRENCE",
                  "action": "CREATE",
                  "data": {
                    "id": "rec-meetup-ev",
                    "domain": "EVENT",
                    "title": "Meetup Mobile",
                    "frequency": "MONTHLY",
                    "startDateIso": "2026-11-15",
                    "isAllDay": true,
                    "locationName": "Hub Tech",
                    "eventStatus": "CONFIRMED",
                    "ticketStatus": "FREE",
                    "templateChecklist": ["Credencial", "Notebook"]
                  }
                },
                {
                  "operationId": "op-appt-solo-chk",
                  "entityType": "APPOINTMENT",
                  "action": "CREATE",
                  "data": {
                    "id": "appt-with-chk",
                    "title": "Consulta médica",
                    "startAt": $t0,
                    "endAt": ${t0 + 3600000L},
                    "checklist": [
                      { "id": "chk-a1", "title": "Pedido médico", "isCompleted": true, "sortOrder": 0 },
                      { "id": "chk-a2", "title": "Carteirinha", "isCompleted": false, "sortOrder": 1 }
                    ]
                  }
                },
                {
                  "operationId": "op-ev-solo-chk",
                  "entityType": "EVENT",
                  "action": "CREATE",
                  "data": {
                    "id": "ev-with-chk",
                    "name": "Workshop",
                    "startAt": $t0,
                    "endAt": ${t0 + 7200000L},
                    "checklist": [
                      { "id": "chk-e1", "title": "Carregador", "isCompleted": false, "sortOrder": 0 }
                    ]
                  }
                }
              ]
            }
        """.trimIndent()

        val preview = processor.previewJson(createJson)
        assertEquals(5, preview.willCreateCount)
        assertEquals(0, preview.rejectedCount)
        assertTrue(gateway.taskRecurrences.isEmpty())
        assertTrue(gateway.scheduleChecklistItems.isEmpty())

        val result = processor.processJson(createJson, atomicBatch = true)
        assertEquals(5, result.appliedCount)
        assertEquals(0, result.rejectedCount)

        val apptRec = gateway.getTaskRecurrenceById("rec-medical-appt")!!
        assertEquals(RecurrenceDomain.APPOINTMENT, apptRec.domain)
        assertEquals(TaskRecurrenceFrequency.SPECIFIC_WEEKDAYS, apptRec.frequency)
        assertEquals(840, apptRec.dueTimeMinutesOfDay)
        assertEquals(1, apptRec.checklistTemplateVersion)
        assertEquals(listOf("Levar exame", "Água"), RecurrenceEngine.deserializeChecklistTemplate(apptRec.templateSubtasksJson))

        assertEquals(2, gateway.getScheduleChecklistItemsByAppointmentId("appt-with-chk").size)
        assertEquals(1, gateway.getScheduleChecklistItemsByEventId("ev-with-chk").size)

        // Update RECURRENCE template checklist -> increments version non-retroactively and preserves history
        val updateRecJson = """
            {
              "contractVersion": 1,
              "source": "chatgpt_inbox",
              "createdAt": ${t0 + 1000L},
              "operations": [
                {
                  "operationId": "op-rec-appt-upd",
                  "entityType": "RECURRENCE",
                  "action": "UPSERT",
                  "data": {
                    "id": "rec-medical-appt",
                    "domain": "APPOINTMENT",
                    "title": "Fisioterapia",
                    "frequency": "SPECIFIC_WEEKDAYS",
                    "weekdays": ["MON", "WED"],
                    "startDateIso": "2026-11-02",
                    "hasDueTime": true,
                    "dueTimeMinutesOfDay": 840,
                    "durationMillis": 3600000,
                    "templateChecklist": ["Levar exame", "Água", "Toalha"],
                    "effectiveFromDateIso": "2026-11-16"
                  }
                }
              ]
            }
        """.trimIndent()

        val updRes = processor.processJson(updateRecJson, atomicBatch = true)
        assertEquals(1, updRes.updatedCount)
        val updatedRec = gateway.getTaskRecurrenceById("rec-medical-appt")!!
        assertEquals(2, updatedRec.checklistTemplateVersion)
        val (_, pastItems) = RecurrenceEngine.resolveChecklistTemplateVersionAndItems(updatedRec, "2026-11-09")
        val (_, futureItems) = RecurrenceEngine.resolveChecklistTemplateVersionAndItems(updatedRec, "2026-11-16")
        assertEquals(listOf("Levar exame", "Água"), pastItems)
        assertEquals(listOf("Levar exame", "Água", "Toalha"), futureItems)
    }

    @Test
    fun `invalid RECURRENCE payloads are rejected without mutating database`() = runBlocking {
        val gateway = InMemoryStructuredInputGateway()
        val processor = StructuredInputProcessor(gateway) { t0 }
        val invalidJson = """
            {
              "contractVersion": 1,
              "source": "chatgpt_inbox",
              "createdAt": $t0,
              "operations": [
                {
                  "operationId": "op-rec-bad-1",
                  "entityType": "RECURRENCE",
                  "action": "CREATE",
                  "data": {
                    "domain": "APPOINTMENT",
                    "title": "Sem dias",
                    "frequency": "SPECIFIC_WEEKDAYS",
                    "startDateIso": "2026-11-02"
                  }
                },
                {
                  "operationId": "op-rec-bad-2",
                  "entityType": "RECURRENCE",
                  "action": "CREATE",
                  "data": {
                    "domain": "INVALID_DOMAIN",
                    "title": "Domínio ruim",
                    "frequency": "WEEKLY",
                    "startDateIso": "2026-11-02"
                  }
                }
              ]
            }
        """.trimIndent()

        val res = processor.processJson(invalidJson, atomicBatch = false)
        assertEquals(2, res.rejectedCount)
        assertTrue(gateway.taskRecurrences.isEmpty())
    }
}

