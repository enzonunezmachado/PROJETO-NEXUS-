package com.nexus.app.data.ingestion

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

enum class StructuredEntityType {
    TASK,
    APPOINTMENT,
    EVENT,
    PROJECT,
    PERSONAL_GOAL,
    TRANSACTION;

    companion object {
        fun fromRawOrNull(raw: String?): StructuredEntityType? =
            entries.firstOrNull { it.name.equals(raw?.trim(), ignoreCase = true) }
    }
}

enum class StructuredProcessingState {
    PENDING,
    VALIDATED,
    APPLIED,
    DUPLICATE_IGNORED,
    CONFLICT_PRESERVED_LOCAL,
    REJECTED,
    FAILED_ROLLED_BACK
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
    val data: JsonObject? = null,
    val contractVersion: Int = StructuredInputEnvelope.CURRENT_CONTRACT_VERSION,
    val createdAt: Long = 0L,
    val source: String = "",
    val processingState: StructuredProcessingState = StructuredProcessingState.PENDING,
    val resultDetail: String? = null,
    val rejectionReason: String? = null
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
    val occurrenceDateIso: String? = null
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
    val link: String = ""
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
    val installmentsCount: Int = 1
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
    val rejectionReason: String? = null
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
    val envelopeError: String? = null
) {
    val hasFailures: Boolean
        get() = envelopeError != null || rejectedCount > 0 || failedRolledBackCount > 0
}
