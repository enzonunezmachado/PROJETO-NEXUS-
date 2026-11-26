package com.nexus.app.data.inbox

import com.nexus.app.core.DateTimeUtils
import com.nexus.app.data.db.entity.AuditHistoryEntity
import com.nexus.app.data.ingestion.StructuredBatchPreview
import com.nexus.app.data.ingestion.StructuredBatchResult
import com.nexus.app.data.ingestion.StructuredEntityType
import com.nexus.app.data.ingestion.StructuredInputEnvelope
import com.nexus.app.data.ingestion.StructuredInputOperation
import com.nexus.app.data.ingestion.StructuredOperationReceipt
import com.nexus.app.data.ingestion.StructuredPreviewOutcome
import com.nexus.app.data.ingestion.StructuredProcessingState
import com.nexus.app.data.repository.NexusRepository
import com.nexus.app.notifications.NexusNotificationScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.security.MessageDigest
import java.util.UUID

object InboxSummaryFormatter {
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
    }

    fun buildUpdateLogEntry(
        batchId: String,
        occurredAtEpochMs: Long,
        sourceFiles: List<String>,
        result: StructuredBatchResult,
        preParseErrors: List<String> = emptyList()
    ): InboxUpdateLogEntry {
        val addedByGroup = linkedMapOf<InboxEntityGroup, MutableList<String>>()
        val updatedByGroup = linkedMapOf<InboxEntityGroup, MutableList<String>>()
        val errorDetails = mutableListOf<String>()
        errorDetails.addAll(preParseErrors)

        if (!result.envelopeError.isNullOrBlank()) {
            errorDetails.add(result.envelopeError)
        }

        for (receipt in result.receipts) {
            when (receipt.processingState) {
                StructuredProcessingState.APPLIED -> {
                    val group = receipt.entityType.toInboxGroup()
                    if (group != null) {
                        val label = receipt.displayLabel()
                        addedByGroup.getOrPut(group) { mutableListOf() }.add(label)
                        if (receipt.entityType == StructuredEntityType.TASK && receipt.checklistItemsCount > 0) {
                            addedByGroup.getOrPut(InboxEntityGroup.CHECKLIST) { mutableListOf() }
                                .add("${receipt.checklistItemsCount} item(ns) em $label")
                        }
                    }
                }

                StructuredProcessingState.UPDATED -> {
                    val group = receipt.entityType.toInboxGroup()
                    if (group != null) {
                        val label = receipt.displayLabel()
                        updatedByGroup.getOrPut(group) { mutableListOf() }.add(label)
                        if (receipt.entityType == StructuredEntityType.TASK && receipt.checklistItemsCount > 0) {
                            updatedByGroup.getOrPut(InboxEntityGroup.CHECKLIST) { mutableListOf() }
                                .add("${receipt.checklistItemsCount} item(ns) em $label")
                        }
                    }
                }

                StructuredProcessingState.REJECTED,
                StructuredProcessingState.FAILED_ROLLED_BACK -> {
                    val msg = receipt.rejectionReason ?: "Operação '${receipt.operationId}' rejeitada."
                    errorDetails.add(msg)
                }

                else -> Unit
            }
        }

        val orderedGroups = listOf(
            InboxEntityGroup.EVENT,
            InboxEntityGroup.TASK,
            InboxEntityGroup.CHECKLIST,
            InboxEntityGroup.APPOINTMENT,
            InboxEntityGroup.TRANSACTION,
            InboxEntityGroup.PROJECT,
            InboxEntityGroup.PERSONAL_GOAL
        )

        val categories = orderedGroups.mapNotNull { group ->
            val added = addedByGroup[group].orEmpty()
            val updated = updatedByGroup[group].orEmpty()
            if (added.isEmpty() && updated.isEmpty()) {
                null
            } else {
                InboxCategorySummary(
                    group = group,
                    addedCount = added.size,
                    updatedCount = updated.size,
                    itemLabels = (added + updated).distinct().take(8)
                )
            }
        }

        val totalChanged = categories.sumOf { it.addedCount + it.updatedCount }
        val headline = when {
            totalChanged > 0 && errorDetails.isEmpty() ->
                "$totalChanged atualização(ões) aplicada(s) ao NEXUS"
            totalChanged > 0 ->
                "$totalChanged atualização(ões) aplicada(s) com ${errorDetails.size} alerta(s)"
            result.duplicateIgnoredCount > 0 || result.conflictPreservedLocalCount > 0 ->
                "Nenhuma novidade gravada (duplicidades/conflitos preservados)"
            errorDetails.isNotEmpty() ->
                "Importação falhou com ${errorDetails.size} erro(s)"
            else ->
                "Nenhuma alteração aplicada"
        }

        return InboxUpdateLogEntry(
            id = batchId,
            occurredAtEpochMs = occurredAtEpochMs,
            sourceFiles = sourceFiles,
            categories = categories,
            duplicatesIgnoredCount = result.duplicateIgnoredCount,
            conflictsPreservedCount = result.conflictPreservedLocalCount,
            errorsCount = errorDetails.size,
            errorDetails = errorDetails.take(10),
            summaryHeadline = headline
        )
    }

    fun formatCategorySummaryLine(summary: InboxCategorySummary): String {
        val parts = mutableListOf<String>()
        if (summary.addedCount > 0) {
            parts.add("${summary.addedCount} ${nounFor(summary.group, summary.addedCount)} ${verbAdded(summary.group, summary.addedCount)}")
        }
        if (summary.updatedCount > 0) {
            parts.add("${summary.updatedCount} ${nounFor(summary.group, summary.updatedCount)} ${verbUpdated(summary.group, summary.updatedCount)}")
        }
        val header = parts.joinToString(" e ")
        val labelsPreview = summary.itemLabels.take(4).joinToString(", ")
        return if (labelsPreview.isNotBlank()) "$header: $labelsPreview" else header
    }

    fun encodeLogEntry(entry: InboxUpdateLogEntry): String = json.encodeToString(entry)

    fun decodeAuditEntry(audit: AuditHistoryEntity): InboxUpdateLogEntry? {
        if (audit.entityType != NexusRepository.AUDIT_ENTITY_INBOX_IMPORT_LOG) return null
        return runCatching { json.decodeFromString<InboxUpdateLogEntry>(audit.summary) }.getOrNull()
    }

    private fun StructuredEntityType?.toInboxGroup(): InboxEntityGroup? = when (this) {
        StructuredEntityType.EVENT -> InboxEntityGroup.EVENT
        StructuredEntityType.TASK -> InboxEntityGroup.TASK
        StructuredEntityType.CHECKLIST -> InboxEntityGroup.CHECKLIST
        StructuredEntityType.APPOINTMENT -> InboxEntityGroup.APPOINTMENT
        StructuredEntityType.TRANSACTION -> InboxEntityGroup.TRANSACTION
        StructuredEntityType.PROJECT -> InboxEntityGroup.PROJECT
        StructuredEntityType.PERSONAL_GOAL -> InboxEntityGroup.PERSONAL_GOAL
        null -> null
    }

    private fun StructuredOperationReceipt.displayLabel(): String =
        entityTitle?.takeIf { it.isNotBlank() } ?: targetEntityId ?: operationId

    private fun nounFor(group: InboxEntityGroup, count: Int): String = when (group) {
        InboxEntityGroup.EVENT -> if (count == 1) "evento" else "eventos"
        InboxEntityGroup.TASK -> if (count == 1) "tarefa" else "tarefas"
        InboxEntityGroup.CHECKLIST -> if (count == 1) "checklist" else "checklists"
        InboxEntityGroup.APPOINTMENT -> if (count == 1) "compromisso" else "compromissos"
        InboxEntityGroup.TRANSACTION -> if (count == 1) "despesa/lançamento" else "despesas/lançamentos"
        InboxEntityGroup.PROJECT -> if (count == 1) "projeto" else "projetos"
        InboxEntityGroup.PERSONAL_GOAL -> if (count == 1) "objetivo" else "objetivos"
    }

    private fun verbAdded(group: InboxEntityGroup, count: Int): String = when (group) {
        InboxEntityGroup.TASK, InboxEntityGroup.TRANSACTION ->
            if (count == 1) "adicionada" else "adicionadas"
        else ->
            if (count == 1) "adicionado" else "adicionados"
    }

    private fun verbUpdated(group: InboxEntityGroup, count: Int): String = when (group) {
        InboxEntityGroup.TASK, InboxEntityGroup.TRANSACTION ->
            if (count == 1) "atualizada" else "atualizadas"
        else ->
            if (count == 1) "atualizado" else "atualizados"
    }
}

interface InboxIngestionPort {
    suspend fun previewInboxEnvelope(envelope: StructuredInputEnvelope): Result<StructuredBatchPreview>
    suspend fun applyInboxEnvelope(envelope: StructuredInputEnvelope): Result<StructuredBatchResult>
    suspend fun recordInboxImportLog(batchId: String, serializedSummaryJson: String): Result<Unit>
}

class InboxRepository(
    private val nexusRepository: InboxIngestionPort,
    private val configStorage: InboxConfigStorage,
    private val tokenStore: InboxTokenStore,
    private val remoteDataSource: InboxRemoteDataSource,
    private val notificationScheduler: NexusNotificationScheduler? = null,
    private val onAutoCheckPreferenceChanged: ((Boolean) -> Unit)? = null,
    private val clockMillis: () -> Long = { DateTimeUtils.nowEpochMillis() }
) {
    private val strictContractJson = Json {
        ignoreUnknownKeys = false
        isLenient = false
        coerceInputValues = false
    }

    private val _config = MutableStateFlow(configStorage.loadConfig())
    val config: StateFlow<InboxConfig> = _config.asStateFlow()

    private val _hasSavedToken = MutableStateFlow(tokenStore.hasToken())
    val hasSavedToken: StateFlow<Boolean> = _hasSavedToken.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _pendingCheck = MutableStateFlow<InboxPendingCheckResult?>(null)
    val pendingCheck: StateFlow<InboxPendingCheckResult?> = _pendingCheck.asStateFlow()

    private val _lastConnectionTest = MutableStateFlow<InboxConnectionTestResult?>(null)
    val lastConnectionTest: StateFlow<InboxConnectionTestResult?> = _lastConnectionTest.asStateFlow()

    private val _lastUpdateLog = MutableStateFlow<InboxUpdateLogEntry?>(null)
    val lastUpdateLog: StateFlow<InboxUpdateLogEntry?> = _lastUpdateLog.asStateFlow()

    fun saveConfig(
        owner: String,
        repo: String,
        branch: String,
        pathPrefix: String,
        autoCheckEnabled: Boolean,
        newTokenInput: String? = null
    ): Result<InboxConfig> {
        val current = _config.value
        val candidate = current.copy(
            owner = owner,
            repo = repo,
            branch = branch,
            pathPrefix = pathPrefix,
            autoCheckEnabled = autoCheckEnabled,
            lastError = null
        )
        val validated = InboxInputValidator.normalizeAndValidate(candidate).getOrElse {
            return Result.failure(it)
        }

        val cleanToken = newTokenInput?.trim()
        if (!cleanToken.isNullOrEmpty()) {
            val validToken = InboxInputValidator.validateTokenInput(cleanToken).getOrElse {
                return Result.failure(it)
            }
            tokenStore.saveToken(validToken)
            _hasSavedToken.value = true
        }

        val repoLocationChanged = current.owner != validated.owner ||
            current.repo != validated.repo ||
            current.branch != validated.branch ||
            current.pathPrefix != validated.pathPrefix
        if (repoLocationChanged) {
            configStorage.clearProcessedFileVersionKeys()
            configStorage.setLastNotifiedFingerprint(null)
            _pendingCheck.value = null
            _lastConnectionTest.value = null
        }

        configStorage.saveConfig(validated)
        _config.value = validated
        onAutoCheckPreferenceChanged?.invoke(validated.autoCheckEnabled && validated.isConfigured && _hasSavedToken.value)
        return Result.success(validated)
    }

    fun clearToken() {
        tokenStore.clearToken()
        _hasSavedToken.value = false
        _lastConnectionTest.value = null
        _pendingCheck.value = null
        val updated = _config.value.copy(autoCheckEnabled = false)
        configStorage.saveConfig(updated)
        _config.value = updated
        onAutoCheckPreferenceChanged?.invoke(false)
    }

    suspend fun testConnection(pendingTokenOverride: String? = null): InboxConnectionTestResult {
        val currentConfig = _config.value
        val token = pendingTokenOverride?.trim()?.takeIf { it.isNotEmpty() }
            ?: tokenStore.getToken()
            ?: return InboxConnectionTestResult.Failure("Informe e salve um token de leitura do GitHub primeiro.")
        _isBusy.value = true
        return try {
            val res = remoteDataSource.testConnection(currentConfig, token)
            _lastConnectionTest.value = res
            if (res is InboxConnectionTestResult.Failure) {
                updateConfigError(res.message)
            } else {
                updateConfigError(null)
            }
            res
        } finally {
            _isBusy.value = false
        }
    }

    suspend fun checkInboxNow(fromBackgroundWorker: Boolean = false): Result<InboxPendingCheckResult> {
        val currentConfig = _config.value
        if (!currentConfig.isConfigured) {
            return Result.failure(IllegalStateException("Configure o repositório NEXUS-INBOX antes de verificar."))
        }
        val token = tokenStore.getToken()
            ?: return Result.failure(IllegalStateException("Token da NEXUS-INBOX não configurado."))

        _isBusy.value = true
        return try {
            val now = clockMillis()
            val remoteFiles = remoteDataSource.listInboxJsonFiles(currentConfig, token).getOrElse { err ->
                val msg = err.message ?: "Falha ao listar arquivos da NEXUS-INBOX."
                updateCheckedStatus(now, msg)
                return Result.failure(IllegalStateException(msg))
            }

            val processedKeys = configStorage.getProcessedFileVersionKeys()
            val candidateFiles = remoteFiles.filterNot { processedKeys.contains(it.versionKey) }

            val downloadedValid = mutableListOf<InboxDownloadedFile>()
            val parseErrors = mutableListOf<String>()

            for (file in candidateFiles) {
                val raw = remoteDataSource.downloadFileRawContent(currentConfig, file, token).getOrElse { err ->
                    parseErrors.add("${file.name}: ${err.message ?: "erro ao baixar"}")
                    continue
                }
                val parsed = parseEnvelopeStrictly(raw).getOrElse { err ->
                    parseErrors.add("${file.name}: ${err.message ?: "JSON incompatível com o contrato"}")
                    continue
                }
                downloadedValid.add(
                    InboxDownloadedFile(
                        file = file,
                        rawJson = raw,
                        envelope = parsed
                    )
                )
            }

            val mergedEnvelope = mergeDownloadedEnvelopes(downloadedValid, now)
            val preview = nexusRepository.previewInboxEnvelope(mergedEnvelope).getOrElse { err ->
                val msg = err.message ?: "Erro ao validar prévia da NEXUS-INBOX."
                updateCheckedStatus(now, msg)
                return Result.failure(IllegalStateException(msg))
            }

            val result = InboxPendingCheckResult(
                checkedAtEpochMs = now,
                filesScannedCount = remoteFiles.size,
                unseenFiles = downloadedValid,
                combinedPreview = preview,
                fileParseErrors = parseErrors
            )

            _pendingCheck.value = result
            updateCheckedStatus(now, if (parseErrors.isNotEmpty() && downloadedValid.isEmpty()) parseErrors.first() else null)

            if (fromBackgroundWorker && result.hasNewActionableItems) {
                val fingerprint = computePendingFingerprint(result)
                if (fingerprint != configStorage.getLastNotifiedFingerprint()) {
                    val notified = notificationScheduler?.showInboxPendingNotification(
                        pendingNewCount = preview.willCreateCount,
                        pendingUpdateCount = preview.willUpdateCount,
                        filesCount = downloadedValid.size
                    ) ?: false
                    if (notified) {
                        configStorage.setLastNotifiedFingerprint(fingerprint)
                    }
                }
            }

            Result.success(result)
        } finally {
            _isBusy.value = false
        }
    }

    suspend fun confirmAndImportPending(): Result<InboxUpdateLogEntry> {
        val pending = _pendingCheck.value
            ?: return Result.failure(IllegalStateException("Nenhuma prévia pendente para importar. Verifique a NEXUS-INBOX primeiro."))

        if (pending.unseenFiles.isEmpty() && pending.fileParseErrors.isEmpty()) {
            return Result.failure(IllegalStateException("Não há operações pendentes na NEXUS-INBOX."))
        }

        _isBusy.value = true
        return try {
            val now = clockMillis()
            val mergedEnvelope = mergeDownloadedEnvelopes(pending.unseenFiles, now)
            val batchResult = nexusRepository.applyInboxEnvelope(
                envelope = mergedEnvelope
            ).getOrElse { err ->
                return Result.failure(err)
            }

            val batchId = "inbox-${UUID.randomUUID()}"
            val sourceFiles = pending.unseenFiles.map { it.file.name }
            val logEntry = InboxSummaryFormatter.buildUpdateLogEntry(
                batchId = batchId,
                occurredAtEpochMs = now,
                sourceFiles = sourceFiles,
                result = batchResult,
                preParseErrors = pending.fileParseErrors
            )

            nexusRepository.recordInboxImportLog(
                batchId = batchId,
                serializedSummaryJson = InboxSummaryFormatter.encodeLogEntry(logEntry)
            )

            val receiptsByOpId = batchResult.receipts.associateBy { it.operationId.trim() }
            val completedFileKeys = pending.unseenFiles.mapNotNull { downloaded ->
                val allTerminalWithoutFailure = downloaded.envelope.operations.all { op ->
                    val state = receiptsByOpId[op.operationId.trim()]?.processingState
                    state == StructuredProcessingState.APPLIED ||
                        state == StructuredProcessingState.UPDATED ||
                        state == StructuredProcessingState.DUPLICATE_IGNORED ||
                        state == StructuredProcessingState.CONFLICT_PRESERVED_LOCAL
                }
                if (allTerminalWithoutFailure) downloaded.file.versionKey else null
            }.toSet()

            if (completedFileKeys.isNotEmpty()) {
                configStorage.addProcessedFileVersionKeys(completedFileKeys)
            }
            configStorage.setLastNotifiedFingerprint(null)

            val updatedConfig = _config.value.copy(
                lastImportAtEpochMs = now,
                lastError = if (logEntry.errorsCount > 0) logEntry.errorDetails.firstOrNull() else null
            )
            configStorage.saveConfig(updatedConfig)
            _config.value = updatedConfig
            _lastUpdateLog.value = logEntry
            _pendingCheck.value = null

            Result.success(logEntry)
        } finally {
            _isBusy.value = false
        }
    }

    fun discardPendingPreview() {
        _pendingCheck.value = null
    }

    fun clearLastUpdateLogBanner() {
        _lastUpdateLog.value = null
    }

    private fun parseEnvelopeStrictly(raw: String): Result<StructuredInputEnvelope> {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(IllegalArgumentException("Arquivo JSON vazio."))
        }
        return runCatching {
            strictContractJson.decodeFromString(StructuredInputEnvelope.serializer(), trimmed)
        }
    }

    private fun mergeDownloadedEnvelopes(
        files: List<InboxDownloadedFile>,
        fallbackEpochMs: Long
    ): StructuredInputEnvelope {
        if (files.isEmpty()) {
            return StructuredInputEnvelope(
                contractVersion = StructuredInputEnvelope.CURRENT_CONTRACT_VERSION,
                source = "nexus_inbox",
                createdAt = fallbackEpochMs,
                operations = emptyList()
            )
        }
        val normalizedOps = mutableListOf<StructuredInputOperation>()
        for (downloaded in files) {
            val env = downloaded.envelope
            val fileSource = env.source.trim().ifEmpty { "nexus_inbox:${downloaded.file.name}" }
            val fileCreatedAt = if (env.createdAt > 0L) env.createdAt else fallbackEpochMs
            for (op in env.operations) {
                normalizedOps.add(
                    op.copy(
                        source = op.source.trim().ifEmpty { fileSource },
                        createdAt = if (op.createdAt > 0L) op.createdAt else fileCreatedAt
                    )
                )
            }
        }
        val firstEnv = files.first().envelope
        return StructuredInputEnvelope(
            contractVersion = firstEnv.contractVersion,
            source = firstEnv.source.trim().ifEmpty { "nexus_inbox" },
            createdAt = if (firstEnv.createdAt > 0L) firstEnv.createdAt else fallbackEpochMs,
            operations = normalizedOps
        )
    }

    private fun computePendingFingerprint(result: InboxPendingCheckResult): String {
        val raw = buildString {
            result.unseenFiles.forEach { append(it.file.versionKey).append('|') }
            result.combinedPreview.items
                .filter {
                    it.outcome == StructuredPreviewOutcome.WILL_CREATE ||
                        it.outcome == StructuredPreviewOutcome.WILL_UPDATE
                }
                .forEach { append(it.operationId).append(':') }
        }
        val digest = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun updateCheckedStatus(checkedAt: Long, error: String?) {
        val updated = _config.value.copy(
            lastCheckedAtEpochMs = checkedAt,
            lastError = error
        )
        configStorage.saveConfig(updated)
        _config.value = updated
    }

    private fun updateConfigError(error: String?) {
        val updated = _config.value.copy(lastError = error)
        configStorage.saveConfig(updated)
        _config.value = updated
    }
}
