package com.nexus.app.data.inbox

import com.nexus.app.data.ingestion.StructuredBatchPreview
import com.nexus.app.data.ingestion.StructuredInputEnvelope
import kotlinx.serialization.Serializable

@Serializable
data class InboxConfig(
    val owner: String = "",
    val repo: String = DEFAULT_REPO_NAME,
    val branch: String = DEFAULT_BRANCH,
    val pathPrefix: String = "",
    val autoCheckEnabled: Boolean = false,
    val lastCheckedAtEpochMs: Long? = null,
    val lastImportAtEpochMs: Long? = null,
    val lastError: String? = null
) {
    val isConfigured: Boolean
        get() = owner.isNotBlank() && repo.isNotBlank() && branch.isNotBlank()

    companion object {
        const val DEFAULT_REPO_NAME = "NEXUS-INBOX"
        const val DEFAULT_BRANCH = "main"
    }
}

@Serializable
data class InboxRemoteFile(
    val name: String,
    val path: String,
    val sha: String,
    val sizeBytes: Long
) {
    val versionKey: String
        get() = "$path::$sha"
}

data class InboxDownloadedFile(
    val file: InboxRemoteFile,
    val rawJson: String,
    val envelope: StructuredInputEnvelope
)

data class InboxPendingCheckResult(
    val checkedAtEpochMs: Long,
    val filesScannedCount: Int,
    val unseenFiles: List<InboxDownloadedFile>,
    val combinedPreview: StructuredBatchPreview,
    val fileParseErrors: List<String> = emptyList()
) {
    val hasNewActionableItems: Boolean
        get() = combinedPreview.hasActionableChanges

    val totalPendingOperations: Int
        get() = combinedPreview.totalOperations
}

@Serializable
enum class InboxEntityGroup {
    EVENT,
    TASK,
    CHECKLIST,
    APPOINTMENT,
    TRANSACTION,
    PROJECT,
    PERSONAL_GOAL
}

@Serializable
data class InboxCategorySummary(
    val group: InboxEntityGroup,
    val addedCount: Int = 0,
    val updatedCount: Int = 0,
    val itemLabels: List<String> = emptyList()
)

@Serializable
data class InboxUpdateLogEntry(
    val id: String,
    val occurredAtEpochMs: Long,
    val sourceFiles: List<String> = emptyList(),
    val categories: List<InboxCategorySummary> = emptyList(),
    val duplicatesIgnoredCount: Int = 0,
    val conflictsPreservedCount: Int = 0,
    val errorsCount: Int = 0,
    val errorDetails: List<String> = emptyList(),
    val summaryHeadline: String = ""
)

sealed interface InboxConnectionTestResult {
    data class Success(
        val repoFullName: String,
        val branch: String,
        val jsonFilesFound: Int
    ) : InboxConnectionTestResult

    data class Failure(val message: String) : InboxConnectionTestResult
}
