package com.nexus.app.data.ingestion

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

enum class StructuredEntityType {
    TASK,
    CHECKLIST,
    APPOINTMENT,
    EVENT,
    RECURRENCE,
    PROJECT,
    PERSONAL_GOAL,
    TRANSACTION;

    companion object {
        fun fromRawOrNull(raw: String?): StructuredEntityType? =
            entries.firstOrNull { it.name.equals(raw?.trim(), ignoreCase = true) }
    }
}

enum class StructuredOperationAction {
    CREATE,
    UPDATE,
    UPSERT;

    companion object {
        fun fromRawOrNull(raw: String?): StructuredOperationAction? {
            val clean = raw?.trim().orEmpty()
            if (clean.isEmpty()) return CREATE
            return entries.firstOrNull { it.name.equals(clean, ignoreCase = true) }
        }
    }
}

enum class StructuredProcessingState {
    PENDING,
    VALIDATED,
    APPLIED,
    UPDATED,
    DUPLICATE_IGNORED,
    CONFLICT_PRESERVED_LOCAL,
    REJECTED,
    FAILED_ROLLED_BACK
}

enum class StructuredPreviewOutcome {
    WILL_CREATE,
    WILL_UPDATE,
    DUPLICATE_IGNORED,
    CONFLICT_PRESERVED_LOCAL,
    REJECTED
}

@Serializable
data class StructuredInputEnvelope(
    val contractVersion: Int = CURRENT_CONTRACT_VERSION,
    val source: String = "",
    val createdAt: Long = 0L,
    val operations: List<StructuredInputOperation> = emptyList()
) {
    companion object {
        const val CURRENT_CONTRACT_VERSION = 1
    }
}

@Serializable
data class StructuredInputOperation(
    val operationId: String = "",
    val entityType: String = "",
    val action: String = "CREATE",
    val data: JsonObject? = null,
    val contractVersion: Int = StructuredInputEnvelope.CURRENT_CONTRACT_VERSION,
    val createdAt: Long = 0L,
    val source: String = "",
    val processingState: StructuredProcessingState = StructuredProcessingState.PENDING,
    val resultDetail: String? = null,
    val rejectionReason: String? = null
)

@Serializable
data class StructuredSubtaskItem(
    val id: String? = null,
    val title: String = "",
    val isCompleted: Boolean = false,
    val sortOrder: Int? = null
)

@Serializable
data class StructuredTaskData(
    val id: String? = null,
    val title: String = "",
    val description: String = "",
    val projectId: String? = null,
    val categoryId: String? = null,
    val eventId: String? = null,
    val goalId: String? = null,
    val priority: String = "NORMAL",
    val status: String = "NOT_STARTED",
    val dueAt: Long? = null,
    val hasDueTime: Boolean = false,
    val notes: String = "",
    val completedAt: Long? = null,
    val recurrenceId: String? = null,
    val occurrenceDateIso: String? = null,
    val subtasks: List<StructuredSubtaskItem> = emptyList()
)

@Serializable
data class StructuredChecklistData(
    val id: String? = null,
    val taskId: String = "",
    val title: String? = null,
    val isCompleted: Boolean = false,
    val sortOrder: Int? = null,
    val items: List<StructuredSubtaskItem> = emptyList()
)

@Serializable
data class StructuredAppointmentData(
    val id: String? = null,
    val title: String = "",
    val description: String = "",
    val categoryId: String? = null,
    val eventId: String? = null,
    val startAt: Long = 0L,
    val endAt: Long = 0L,
    val isAllDay: Boolean = false,
    val location: String = "",
    val link: String = "",
    val checklist: List<StructuredSubtaskItem> = emptyList()
)

@Serializable
data class StructuredEventData(
    val id: String? = null,
    val name: String = "",
    val description: String = "",
    val categoryId: String? = null,
    val startAt: Long = 0L,
    val endAt: Long = 0L,
    val isAllDay: Boolean = false,
    val officialLink: String = "",
    val ticketLink: String = "",
    val locationName: String = "",
    val address: String = "",
    val city: String = "",
    val state: String = "",
    val notes: String = "",
    val eventStatus: String = "PLANNED",
    val ticketStatus: String = "FREE",
    val ticketPriceCents: Long = 0L,
    val ticketQuantity: Int = 1,
    val ticketFeesCents: Long = 0L,
    val ticketTotalCents: Long = 0L,
    val ticketPurchaseDate: Long? = null,
    val ticketDueDate: Long? = null,
    val paymentMethod: String = "PIX",
    val creditCardId: String? = null,
    val installmentsCount: Int = 1,
    val checklist: List<StructuredSubtaskItem> = emptyList()
)

@Serializable
data class StructuredRecurrenceData(
    val id: String? = null,
    val domain: String = "TASK",
    val title: String = "",
    val description: String = "",
    val projectId: String? = null,
    val categoryId: String? = null,
    val eventId: String? = null,
    val goalId: String? = null,
    val priority: String = "NORMAL",
    val notes: String = "",
    val frequency: String = "WEEKLY",
    val customIntervalDays: Int = 1,
    val weekdays: List<String> = emptyList(),
    val weekdaysMask: Int? = null,
    val startDateIso: String = "",
    val endDateIso: String? = null,
    val maxOccurrences: Int? = null,
    val hasDueTime: Boolean = false,
    val dueTimeMinutesOfDay: Int? = null,
    val startTime: String? = null,
    val isAllDay: Boolean = false,
    val durationMillis: Long = 3_600_000L,
    val reminderMinutesBefore: Int? = null,
    val location: String = "",
    val link: String = "",
    val officialLink: String = "",
    val ticketLink: String = "",
    val locationName: String = "",
    val address: String = "",
    val city: String = "",
    val state: String = "",
    val eventStatus: String = "PLANNED",
    val ticketStatus: String = "FREE",
    val ticketPriceCents: Long = 0L,
    val ticketQuantity: Int = 1,
    val ticketFeesCents: Long = 0L,
    val paymentMethod: String = "PIX",
    val creditCardId: String? = null,
    val installmentsCount: Int = 1,
    val templateChecklist: List<String> = emptyList(),
    val effectiveFromDateIso: String? = null,
    val isActive: Boolean = true
)

@Serializable
data class StructuredProjectData(
    val id: String? = null,
    val name: String = "",
    val description: String = "",
    val deadlineAt: Long? = null,
    val status: String = "ACTIVE",
    val priority: String = "NORMAL",
    val goalId: String? = null
)

@Serializable
data class StructuredPersonalGoalData(
    val id: String? = null,
    val title: String = "",
    val description: String = "",
    val categoryId: String? = null,
    val targetDateAt: Long? = null,
    val status: String = "ACTIVE",
    val progressMode: String = "AUTO",
    val manualProgressPercent: Int = 0,
    val completedAt: Long? = null,
    val notes: String = ""
)

@Serializable
data class StructuredTransactionData(
    val id: String? = null,
    val type: String = "",
    val description: String = "",
    val amountCents: Long = 0L,
    val categoryId: String? = null,
    val status: String = "PENDING",
    val paymentMethod: String = "PIX",
    val occurredAt: Long = 0L,
    val dueAt: Long? = null,
    val settledAt: Long? = null,
    val projectId: String? = null,
    val eventId: String? = null,
    val taskId: String? = null,
    val eventCostType: String? = null,
    val isTicketObligation: Boolean = false,
    val creditCardId: String? = null,
    val statementId: String? = null,
    val installmentPurchaseId: String? = null,
    val installmentNumber: Int? = null,
    val installmentsTotal: Int? = null,
    val recurringRuleId: String? = null,
    val occurrenceIndex: Int? = null,
    val notes: String = ""
)

data class StructuredOperationReceipt(
    val operationId: String,
    val entityTypeRaw: String,
    val entityType: StructuredEntityType?,
    val targetEntityId: String?,
    val contractVersion: Int,
    val source: String,
    val createdAt: Long,
    val processingState: StructuredProcessingState,
    val resultDetail: String? = null,
    val rejectionReason: String? = null,
    val entityTitle: String? = null,
    val checklistItemsCount: Int = 0,
    val actionTaken: String = "CREATE"
) {
    fun toOperationStatusCopy(original: StructuredInputOperation): StructuredInputOperation =
        original.copy(
            processingState = processingState,
            resultDetail = resultDetail,
            rejectionReason = rejectionReason
        )
}

data class StructuredBatchResult(
    val contractVersion: Int,
    val source: String,
    val atomicBatch: Boolean,
    val totalOperations: Int,
    val appliedCount: Int,
    val duplicateIgnoredCount: Int,
    val conflictPreservedLocalCount: Int,
    val rejectedCount: Int,
    val failedRolledBackCount: Int,
    val receipts: List<StructuredOperationReceipt>,
    val envelopeError: String? = null,
    val updatedCount: Int = 0
) {
    val totalAppliedCount: Int
        get() = appliedCount + updatedCount

    val hasFailures: Boolean
        get() = envelopeError != null || rejectedCount > 0 || failedRolledBackCount > 0
}

data class StructuredPreviewItem(
    val operationId: String,
    val entityTypeRaw: String,
    val entityType: StructuredEntityType?,
    val targetEntityId: String?,
    val displayTitle: String,
    val outcome: StructuredPreviewOutcome,
    val detail: String,
    val checklistItemsCount: Int = 0,
    val source: String = "",
    val createdAt: Long = 0L
)

data class StructuredBatchPreview(
    val contractVersion: Int,
    val source: String,
    val totalOperations: Int,
    val willCreateCount: Int,
    val willUpdateCount: Int,
    val duplicateIgnoredCount: Int,
    val conflictPreservedLocalCount: Int,
    val rejectedCount: Int,
    val items: List<StructuredPreviewItem>,
    val envelopeError: String? = null
) {
    val hasActionableChanges: Boolean
        get() = willCreateCount > 0 || willUpdateCount > 0

    val hasErrors: Boolean
        get() = envelopeError != null || rejectedCount > 0
}
