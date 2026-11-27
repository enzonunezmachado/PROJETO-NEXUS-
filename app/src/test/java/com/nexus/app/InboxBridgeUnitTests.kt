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
import com.nexus.app.data.inbox.InboxConfig
import com.nexus.app.data.inbox.InboxConfigStorage
import com.nexus.app.data.inbox.InboxConnectionTestResult
import com.nexus.app.data.inbox.InboxEntityGroup
import com.nexus.app.data.inbox.InboxIngestionPort
import com.nexus.app.data.inbox.InboxInputValidator
import com.nexus.app.data.inbox.InboxRemoteDataSource
import com.nexus.app.data.inbox.InboxRemoteFile
import com.nexus.app.data.inbox.InboxRepository
import com.nexus.app.data.inbox.InboxSummaryFormatter
import com.nexus.app.data.inbox.InboxTokenStore
import com.nexus.app.data.ingestion.RoomStructuredInputGateway
import com.nexus.app.data.ingestion.StructuredBatchPreview
import com.nexus.app.data.ingestion.StructuredBatchResult
import com.nexus.app.data.ingestion.StructuredInputEnvelope
import com.nexus.app.data.ingestion.StructuredInputGateway
import com.nexus.app.data.ingestion.StructuredInputProcessor
import com.nexus.app.data.ingestion.StructuredPreviewOutcome
import com.nexus.app.data.ingestion.StructuredProcessingState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InboxBridgeUnitTests {

    private class InMemoryGateway : StructuredInputGateway {
        val categories = mutableListOf<CategoryEntity>()
        val projects = mutableListOf<ProjectEntity>()
        val personalGoals = mutableListOf<PersonalGoalEntity>()
        val events = mutableListOf<EventEntity>()
        val tasks = mutableListOf<TaskEntity>()
        val subtasks = mutableListOf<SubtaskEntity>()
        val appointments = mutableListOf<AppointmentEntity>()
        val scheduleChecklistItems = mutableListOf<ScheduleChecklistItemEntity>()
        val taskRecurrences = mutableListOf<TaskRecurrenceEntity>()
        val transactions = mutableListOf<TransactionEntity>()
        val auditHistory = mutableListOf<AuditHistoryEntity>()

        override suspend fun <T> inTransaction(block: suspend () -> T): T = block()

        override suspend fun findRecordedOperationState(operationId: String): StructuredProcessingState? {
            val entry = auditHistory.firstOrNull {
                it.entityType == RoomStructuredInputGateway.AUDIT_ENTITY_TYPE &&
                    it.entityId == operationId
            } ?: return null
            return runCatching { StructuredProcessingState.valueOf(entry.action) }.getOrNull()
        }

        override suspend fun recordOperationOutcome(
            operationId: String,
            state: StructuredProcessingState,
            detail: String,
            occurredAt: Long
        ) {
            if (state != StructuredProcessingState.APPLIED &&
                state != StructuredProcessingState.UPDATED &&
                state != StructuredProcessingState.CONFLICT_PRESERVED_LOCAL
            ) {
                return
            }
            auditHistory.add(
                AuditHistoryEntity(
                    id = "audit-$operationId-${auditHistory.size}",
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
        override suspend fun creditCardExists(id: String): Boolean = false
        override suspend fun installmentPurchaseExists(id: String): Boolean = false
        override suspend fun recurringRuleExists(id: String): Boolean = false

        override suspend fun getTaskById(id: String): TaskEntity? = tasks.firstOrNull { it.id == id }
        override suspend fun getSubtaskById(id: String): SubtaskEntity? = subtasks.firstOrNull { it.id == id }
        override suspend fun getSubtasksByTaskId(taskId: String): List<SubtaskEntity> =
            subtasks.filter { it.taskId == taskId }
        override suspend fun getAppointmentById(id: String): AppointmentEntity? = appointments.firstOrNull { it.id == id }
        override suspend fun getEventById(id: String): EventEntity? = events.firstOrNull { it.id == id }
        override suspend fun getTaskRecurrenceById(id: String): TaskRecurrenceEntity? =
            taskRecurrences.firstOrNull { it.id == id }
        override suspend fun getScheduleChecklistItemsByAppointmentId(appointmentId: String): List<ScheduleChecklistItemEntity> =
            scheduleChecklistItems.filter { it.appointmentId == appointmentId }
        override suspend fun getScheduleChecklistItemsByEventId(eventId: String): List<ScheduleChecklistItemEntity> =
            scheduleChecklistItems.filter { it.eventId == eventId }
        override suspend fun getProjectById(id: String): ProjectEntity? = projects.firstOrNull { it.id == id }
        override suspend fun getPersonalGoalById(id: String): PersonalGoalEntity? = personalGoals.firstOrNull { it.id == id }
        override suspend fun getTransactionById(id: String): TransactionEntity? = transactions.firstOrNull { it.id == id }

        override suspend fun getTaskByRecurrenceAndDate(recurrenceId: String, occurrenceDateIso: String): TaskEntity? = null
        override suspend fun getStatementById(id: String): CreditCardStatementEntity? = null
        override suspend fun getAllStatementsOnce(): List<CreditCardStatementEntity> = emptyList()
        override suspend fun getTransactionByRecurringOccurrence(ruleId: String, occurrenceIndex: Int): TransactionEntity? = null
        override suspend fun getTransactionsByInstallmentPurchase(purchaseId: String): List<TransactionEntity> = emptyList()

        override suspend fun insertTask(entity: TaskEntity) {
            tasks.removeAll { it.id == entity.id }
            tasks.add(entity)
        }

        override suspend fun insertSubtask(entity: SubtaskEntity) {
            subtasks.removeAll { it.id == entity.id }
            subtasks.add(entity)
        }

        override suspend fun insertSubtasks(entities: List<SubtaskEntity>) {
            entities.forEach { insertSubtask(it) }
        }

        override suspend fun insertAppointment(entity: AppointmentEntity) {
            appointments.removeAll { it.id == entity.id }
            appointments.add(entity)
        }

        override suspend fun insertEvent(entity: EventEntity) {
            events.removeAll { it.id == entity.id }
            events.add(entity)
        }

        override suspend fun insertTaskRecurrence(entity: TaskRecurrenceEntity) {
            taskRecurrences.removeAll { it.id == entity.id }
            taskRecurrences.add(entity)
        }

        override suspend fun insertScheduleChecklistItems(entities: List<ScheduleChecklistItemEntity>) {
            entities.forEach { item ->
                scheduleChecklistItems.removeAll { it.id == item.id }
                scheduleChecklistItems.add(item)
            }
        }

        override suspend fun insertProject(entity: ProjectEntity) {
            projects.removeAll { it.id == entity.id }
            projects.add(entity)
        }

        override suspend fun insertPersonalGoal(entity: PersonalGoalEntity) {
            personalGoals.removeAll { it.id == entity.id }
            personalGoals.add(entity)
        }

        override suspend fun insertTransaction(entity: TransactionEntity) {
            transactions.removeAll { it.id == entity.id }
            transactions.add(entity)
        }
    }

    private class FakeInboxConfigStorage : InboxConfigStorage {
        var storedConfig = InboxConfig()
        val processedKeys = linkedSetOf<String>()
        var notifiedFingerprint: String? = null

        override fun loadConfig(): InboxConfig = storedConfig
        override fun saveConfig(config: InboxConfig) { storedConfig = config }
        override fun getProcessedFileVersionKeys(): Set<String> = processedKeys.toSet()
        override fun addProcessedFileVersionKeys(keys: Set<String>) { processedKeys.addAll(keys) }
        override fun clearProcessedFileVersionKeys() { processedKeys.clear() }
        override fun getLastNotifiedFingerprint(): String? = notifiedFingerprint
        override fun setLastNotifiedFingerprint(fingerprint: String?) { notifiedFingerprint = fingerprint }
    }

    private class FakeInboxTokenStore : InboxTokenStore {
        private var token: String? = null
        override fun hasToken(): Boolean = !token.isNullOrBlank()
        override fun getToken(): String? = token
        override fun saveToken(token: String) { this.token = token }
        override fun clearToken() { token = null }
    }

    private class FakeInboxRemoteDataSource : InboxRemoteDataSource {
        var files: List<InboxRemoteFile> = emptyList()
        val contentsByPath = mutableMapOf<String, String>()
        var simulatedListError: Throwable? = null

        override suspend fun testConnection(config: InboxConfig, token: String): InboxConnectionTestResult {
            if (token != "pat_read_only_valid") {
                return InboxConnectionTestResult.Failure("Token inválido ou expirado.")
            }
            return InboxConnectionTestResult.Success(
                repoFullName = "${config.owner}/${config.repo}",
                branch = config.branch,
                jsonFilesFound = files.size
            )
        }

        override suspend fun listInboxJsonFiles(config: InboxConfig, token: String): Result<List<InboxRemoteFile>> {
            simulatedListError?.let { return Result.failure(it) }
            if (token != "pat_read_only_valid") {
                return Result.failure(IllegalStateException("Token do GitHub inválido ou expirado."))
            }
            return Result.success(files)
        }

        override suspend fun downloadFileRawContent(
            config: InboxConfig,
            file: InboxRemoteFile,
            token: String
        ): Result<String> {
            val raw = contentsByPath[file.path]
                ?: return Result.failure(IllegalStateException("Arquivo não encontrado."))
            return Result.success(raw)
        }
    }

    private class FakeIngestionPort(
        private val gateway: InMemoryGateway,
        private val clockMillis: () -> Long
    ) : InboxIngestionPort {
        private val processor = StructuredInputProcessor(gateway, clockMillis)
        val recordedImportLogs = mutableListOf<Pair<String, String>>()

        override suspend fun previewInboxEnvelope(envelope: StructuredInputEnvelope): Result<StructuredBatchPreview> =
            Result.success(processor.previewEnvelope(envelope))

        override suspend fun applyInboxEnvelope(envelope: StructuredInputEnvelope): Result<StructuredBatchResult> =
            Result.success(processor.processEnvelope(envelope, atomicBatch = false))

        override suspend fun recordInboxImportLog(batchId: String, serializedSummaryJson: String): Result<Unit> {
            recordedImportLogs.add(batchId to serializedSummaryJson)
            return Result.success(Unit)
        }
    }

    private val t0 = 1_795_100_000_000L

    @Test
    fun `input validator blocks path traversal, invalid owner, and whitespace in token`() {
        assertTrue(
            InboxInputValidator.normalizeAndValidate(
                InboxConfig(owner = "enzo", repo = "NEXUS-INBOX", branch = "main", pathPrefix = "../secrets")
            ).isFailure
        )
        assertTrue(
            InboxInputValidator.normalizeAndValidate(
                InboxConfig(owner = "enzo/outro", repo = "NEXUS-INBOX", branch = "main")
            ).isFailure
        )
        assertTrue(
            InboxInputValidator.validateTokenInput("token com espaco").isFailure
        )
        val valid = InboxInputValidator.normalizeAndValidate(
            InboxConfig(owner = "enzo-nunez", repo = "NEXUS-INBOX", branch = "main", pathPrefix = "/lotes/")
        ).getOrThrow()
        assertEquals("lotes", valid.pathPrefix)
    }

    @Test
    fun `checkInboxNow performs dry-run preview with zero DB writes until explicit confirmation`() = runBlocking {
        val gateway = InMemoryGateway()
        val port = FakeIngestionPort(gateway) { t0 }
        val configStorage = FakeInboxConfigStorage()
        val tokenStore = FakeInboxTokenStore()
        val remote = FakeInboxRemoteDataSource()

        var scheduledFlag: Boolean? = null
        val repo = InboxRepository(
            nexusRepository = port,
            configStorage = configStorage,
            tokenStore = tokenStore,
            remoteDataSource = remote,
            onAutoCheckPreferenceChanged = { scheduledFlag = it },
            clockMillis = { t0 }
        )

        repo.saveConfig(
            owner = "enzo",
            repo = "NEXUS-INBOX",
            branch = "main",
            pathPrefix = "",
            autoCheckEnabled = true,
            newTokenInput = "pat_read_only_valid"
        ).getOrThrow()
        assertEquals(true, scheduledFlag)

        val file1 = InboxRemoteFile(name = "2026-batch-1.json", path = "2026-batch-1.json", sha = "sha-1", sizeBytes = 900)
        remote.files = listOf(file1)
        remote.contentsByPath[file1.path] = """
            {
              "contractVersion": 1,
              "source": "chatgpt",
              "createdAt": $t0,
              "operations": [
                {
                  "operationId": "inbox-op-ev-1",
                  "entityType": "EVENT",
                  "data": {
                    "id": "ev-inbox-1",
                    "name": "Conferência Dev",
                    "startAt": ${t0 + 86_400_000L},
                    "endAt": ${t0 + 90_000_000L}
                  }
                },
                {
                  "operationId": "inbox-op-task-1",
                  "entityType": "TASK",
                  "data": {
                    "id": "task-inbox-1",
                    "title": "Preparar mala",
                    "eventId": "ev-inbox-1",
                    "subtasks": [
                      { "title": "Separar crachá", "isCompleted": false },
                      { "title": "Levar carregador", "isCompleted": false }
                    ]
                  }
                },
                {
                  "operationId": "inbox-op-tx-1",
                  "entityType": "TRANSACTION",
                  "data": {
                    "id": "tx-inbox-1",
                    "type": "EXPENSE",
                    "description": "Passagem aérea",
                    "amountCents": 85000,
                    "occurredAt": $t0,
                    "taskId": "task-inbox-1",
                    "eventId": "ev-inbox-1"
                  }
                }
              ]
            }
        """.trimIndent()

        // Step 1: Manual check -> only dry-run preview, ZERO writes in gateway
        val previewResult = repo.checkInboxNow(fromBackgroundWorker = false).getOrThrow()
        assertTrue(previewResult.hasNewActionableItems)
        assertEquals(3, previewResult.combinedPreview.willCreateCount)
        assertEquals(0, previewResult.combinedPreview.rejectedCount)
        assertTrue(gateway.events.isEmpty())
        assertTrue(gateway.tasks.isEmpty())
        assertTrue(gateway.subtasks.isEmpty())
        assertTrue(gateway.transactions.isEmpty())
        assertTrue(port.recordedImportLogs.isEmpty())

        // Step 2: Explicit confirmation -> real persistence + update log omitting empty groups
        val logEntry = repo.confirmAndImportPending().getOrThrow()
        assertEquals(1, gateway.events.size)
        assertEquals(1, gateway.tasks.size)
        assertEquals(2, gateway.subtasks.size)
        assertEquals(1, gateway.transactions.size)
        assertEquals("task-inbox-1", gateway.transactions.single().taskId)
        assertEquals(1, port.recordedImportLogs.size)
        assertNull(repo.pendingCheck.value)

        // Log contains ONLY categories that actually changed (EVENT, TASK, CHECKLIST, TRANSACTION)
        val groups = logEntry.categories.map { it.group }.toSet()
        assertEquals(
            setOf(
                InboxEntityGroup.EVENT,
                InboxEntityGroup.TASK,
                InboxEntityGroup.CHECKLIST,
                InboxEntityGroup.TRANSACTION
            ),
            groups
        )
        assertFalse(groups.contains(InboxEntityGroup.APPOINTMENT))
        assertFalse(groups.contains(InboxEntityGroup.PROJECT))
        assertFalse(groups.contains(InboxEntityGroup.PERSONAL_GOAL))

        val eventLine = InboxSummaryFormatter.formatCategorySummaryLine(
            logEntry.categories.first { it.group == InboxEntityGroup.EVENT }
        )
        assertTrue(eventLine.contains("1 evento adicionado"))
        assertTrue(eventLine.contains("Conferência Dev"))

        // Step 3: Re-checking the same SHA returns zero pending items
        val secondCheck = repo.checkInboxNow(fromBackgroundWorker = false).getOrThrow()
        assertFalse(secondCheck.hasNewActionableItems)
        assertEquals(0, secondCheck.totalPendingOperations)
    }

    @Test
    fun `background check never imports automatically and network failure preserves state`() = runBlocking {
        val gateway = InMemoryGateway()
        val port = FakeIngestionPort(gateway) { t0 }
        val configStorage = FakeInboxConfigStorage()
        val tokenStore = FakeInboxTokenStore()
        val remote = FakeInboxRemoteDataSource()

        val repo = InboxRepository(
            nexusRepository = port,
            configStorage = configStorage,
            tokenStore = tokenStore,
            remoteDataSource = remote,
            clockMillis = { t0 }
        )

        repo.saveConfig(
            owner = "enzo",
            repo = "NEXUS-INBOX",
            branch = "main",
            pathPrefix = "",
            autoCheckEnabled = true,
            newTokenInput = "pat_read_only_valid"
        ).getOrThrow()

        // Network failure test
        remote.simulatedListError = IllegalStateException("Sem conexão com a internet.")
        val failedCheck = repo.checkInboxNow(fromBackgroundWorker = true)
        assertTrue(failedCheck.isFailure)
        assertNotNull(repo.config.value.lastError)
        assertTrue(gateway.tasks.isEmpty())

        // Recover network with new remote file: background worker check must NOT write to DB
        remote.simulatedListError = null
        val file = InboxRemoteFile(name = "bg.json", path = "bg.json", sha = "sha-bg", sizeBytes = 400)
        remote.files = listOf(file)
        remote.contentsByPath[file.path] = """
            {
              "contractVersion": 1,
              "source": "chatgpt",
              "createdAt": $t0,
              "operations": [
                {
                  "operationId": "bg-op-1",
                  "entityType": "TASK",
                  "data": { "id": "task-bg", "title": "Só deve entrar após confirmar" }
                }
              ]
            }
        """.trimIndent()

        val bgResult = repo.checkInboxNow(fromBackgroundWorker = true).getOrThrow()
        assertTrue(bgResult.hasNewActionableItems)
        assertEquals(StructuredPreviewOutcome.WILL_CREATE, bgResult.combinedPreview.items.single().outcome)
        assertTrue("Background check must never mutate DB", gateway.tasks.isEmpty())
    }

    @Test
    fun `official contract template parses strictly and discard leaves DB untouched`() = runBlocking {
        val gateway = InMemoryGateway()
        val port = FakeIngestionPort(gateway) { t0 }
        val configStorage = FakeInboxConfigStorage()
        val tokenStore = FakeInboxTokenStore()
        val remote = FakeInboxRemoteDataSource()

        val repo = InboxRepository(
            nexusRepository = port,
            configStorage = configStorage,
            tokenStore = tokenStore,
            remoteDataSource = remote,
            clockMillis = { t0 }
        )

        repo.saveConfig(
            owner = "enzo",
            repo = "NEXUS-INBOX",
            branch = "main",
            pathPrefix = "",
            autoCheckEnabled = false,
            newTokenInput = "pat_read_only_valid"
        ).getOrThrow()

        val file = InboxRemoteFile(name = "template.json", path = "template.json", sha = "sha-tpl", sizeBytes = 1400)
        remote.files = listOf(file)
        remote.contentsByPath[file.path] = """
            {
              "contractVersion": 1,
              "source": "chatgpt-nexus-inbox",
              "createdAt": $t0,
              "operations": [
                {
                  "operationId": "op-2026-11-26-evt-01",
                  "entityType": "EVENT",
                  "action": "UPSERT",
                  "contractVersion": 1,
                  "createdAt": $t0,
                  "data": {
                    "id": "evt-summit-2026",
                    "name": "Conferência de Produto 2026",
                    "startAt": ${t0 + 86_400_000L},
                    "endAt": ${t0 + 118_800_000L},
                    "locationName": "São Paulo Expo",
                    "eventStatus": "CONFIRMED",
                    "ticketStatus": "FREE",
                    "ticketPriceCents": 0,
                    "ticketQuantity": 1,
                    "ticketFeesCents": 0,
                    "notes": "Credenciamento às 08h30"
                  }
                },
                {
                  "operationId": "op-2026-11-26-task-01",
                  "entityType": "TASK",
                  "action": "UPSERT",
                  "contractVersion": 1,
                  "createdAt": $t0,
                  "data": {
                    "id": "task-prep-summit",
                    "title": "Preparar viagem da Conferência",
                    "description": "Checklist e reserva de transporte",
                    "eventId": "evt-summit-2026",
                    "priority": "HIGH",
                    "status": "NOT_STARTED",
                    "dueAt": ${t0 + 43_200_000L},
                    "hasDueTime": true,
                    "subtasks": [
                      { "id": "sub-summit-1", "title": "Confirmar credencial", "isCompleted": false, "sortOrder": 0 },
                      { "id": "sub-summit-2", "title": "Separar documentos", "isCompleted": false, "sortOrder": 1 }
                    ]
                  }
                },
                {
                  "operationId": "op-2026-11-26-chk-01",
                  "entityType": "CHECKLIST",
                  "action": "UPSERT",
                  "contractVersion": 1,
                  "createdAt": $t0,
                  "data": {
                    "taskId": "task-prep-summit",
                    "items": [
                      { "id": "sub-summit-3", "title": "Baixar ingresso offline", "isCompleted": false, "sortOrder": 2 }
                    ]
                  }
                },
                {
                  "operationId": "op-2026-11-26-tx-01",
                  "entityType": "TRANSACTION",
                  "action": "CREATE",
                  "contractVersion": 1,
                  "createdAt": $t0,
                  "data": {
                    "id": "tx-passagem-summit",
                    "description": "Passagem aérea conferência",
                    "type": "EXPENSE",
                    "amountCents": 68900,
                    "occurredAt": $t0,
                    "status": "PENDING",
                    "paymentMethod": "PIX",
                    "eventId": "evt-summit-2026",
                    "taskId": "task-prep-summit"
                  }
                }
              ]
            }
        """.trimIndent()

        val check = repo.checkInboxNow(fromBackgroundWorker = false).getOrThrow()
        assertEquals(0, check.fileParseErrors.size)
        assertEquals(0, check.combinedPreview.rejectedCount)
        assertEquals(3, check.combinedPreview.willCreateCount)
        assertEquals(1, check.combinedPreview.willUpdateCount)
        assertEquals(StructuredPreviewOutcome.WILL_UPDATE, check.combinedPreview.items[2].outcome)

        // Discarding preview leaves DB and processed keys completely untouched
        repo.discardPendingPreview()
        assertNull(repo.pendingCheck.value)
        assertTrue(gateway.events.isEmpty())
        assertTrue(gateway.tasks.isEmpty())
        assertTrue(gateway.subtasks.isEmpty())
        assertTrue(gateway.transactions.isEmpty())
        assertTrue(configStorage.processedKeys.isEmpty())
    }

    @Test
    fun `inbox processes RECURRENCE operations and resetLocalTrackingPreservingCredentials keeps PAT and coordinates`() = runBlocking {
        val gateway = InMemoryGateway()
        val configStorage = FakeInboxConfigStorage()
        val tokenStore = FakeInboxTokenStore()
        val remote = FakeInboxRemoteDataSource()
        val ingestion = FakeIngestionPort(gateway) { t0 }
        val repo = InboxRepository(
            configStorage = configStorage,
            tokenStore = tokenStore,
            remoteDataSource = remote,
            ingestionPort = ingestion,
            clockMillis = { t0 }
        )

        repo.saveConfiguration(
            owner = "enzo",
            repo = "NEXUS-INBOX",
            branch = "main",
            pathPrefix = "inbox",
            autoCheckEnabled = true,
            newTokenOrBlank = "pat_read_only_valid"
        ).getOrThrow()

        remote.files = listOf(
            InboxRemoteFile(
                name = "2026-11-27-recurrence.json",
                path = "inbox/2026-11-27-recurrence.json",
                sha = "sha-rec-1",
                sizeBytes = 600L
            )
        )
        remote.contentsByPath["inbox/2026-11-27-recurrence.json"] = """
            {
              "contractVersion": 1,
              "source": "chatgpt-nexus-inbox",
              "createdAt": $t0,
              "operations": [
                {
                  "operationId": "op-rec-inbox-1",
                  "entityType": "RECURRENCE",
                  "action": "UPSERT",
                  "data": {
                    "id": "rec-inbox-terapia",
                    "domain": "APPOINTMENT",
                    "title": "Terapia semanal",
                    "frequency": "WEEKLY",
                    "startDateIso": "2026-11-05",
                    "hasDueTime": true,
                    "startTime": "18:00",
                    "durationMillis": 3000000,
                    "templateChecklist": ["Anotar tópicos"]
                  }
                }
              ]
            }
        """.trimIndent()

        val preview = repo.checkInboxNow(fromBackgroundWorker = false).getOrThrow()
        assertEquals(1, preview.combinedPreview.willCreateCount)
        assertTrue(
            preview.entityGroups.any {
                it.entityType == com.nexus.app.data.ingestion.StructuredEntityType.RECURRENCE
            }
        )

        val log = repo.confirmAndApplyPendingCheck().getOrThrow()
        assertEquals(1, log.totalApplied)
        assertEquals(1, gateway.taskRecurrences.size)
        assertTrue(configStorage.processedKeys.isNotEmpty())

        val preserved = repo.resetLocalTrackingPreservingCredentials()
        assertTrue(preserved.first)
        assertTrue(preserved.second)
        assertTrue(configStorage.processedKeys.isEmpty())
        assertNull(configStorage.notifiedFingerprint)
        assertNull(repo.pendingCheck.value)
        assertNull(repo.lastUpdateLog.value)
        assertTrue(repo.updateLogs.value.isEmpty())
        assertEquals("pat_read_only_valid", tokenStore.getToken())
        assertEquals("enzo", repo.config.value.owner)
        assertEquals("NEXUS-INBOX", repo.config.value.repo)
        assertEquals("inbox", repo.config.value.pathPrefix)
        assertTrue(repo.config.value.autoCheckEnabled)
    }
}

