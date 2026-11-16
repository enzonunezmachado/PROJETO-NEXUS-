package com.nexus.app.data.backup

import kotlinx.serialization.Serializable

@Serializable
data class NexusBackupPayload(
    val formatVersion: Int = CURRENT_FORMAT_VERSION,
    val appVersion: String = "",
    val exportedAt: String = "",
    val databaseVersion: Int = 5,
    val settings: List<SettingsBackupDto> = emptyList(),
    val categories: List<CategoryBackupDto> = emptyList(),
    val projects: List<ProjectBackupDto> = emptyList(),
    val events: List<EventBackupDto> = emptyList(),
    val tasks: List<TaskBackupDto> = emptyList(),
    val subtasks: List<SubtaskBackupDto> = emptyList(),
    val appointments: List<AppointmentBackupDto> = emptyList(),
    val creditCards: List<CreditCardBackupDto> = emptyList(),
    val creditCardStatements: List<CreditCardStatementBackupDto> = emptyList(),
    val installmentPurchases: List<InstallmentPurchaseBackupDto> = emptyList(),
    val recurringRules: List<RecurringRuleBackupDto> = emptyList(),
    val transactions: List<TransactionBackupDto> = emptyList(),
    val loans: List<LoanBackupDto> = emptyList(),
    val budgets: List<BudgetBackupDto> = emptyList(),
    val financialGoals: List<FinancialGoalBackupDto> = emptyList(),
    val auditHistory: List<AuditHistoryBackupDto> = emptyList(),
    val errorLogs: List<ErrorLogBackupDto> = emptyList(),
    val reminders: List<ReminderBackupDto> = emptyList(),
    val routineSettings: List<RoutineAssistantSettingsBackupDto> = emptyList(),
    val briefingHistory: List<BriefingHistoryBackupDto> = emptyList(),
    val personalizationSettings: List<PersonalizationSettingsBackupDto> = emptyList(),
    val quotes: List<QuoteBackupDto> = emptyList(),
    val personalGoals: List<PersonalGoalBackupDto> = emptyList(),
    val goalMilestones: List<GoalMilestoneBackupDto> = emptyList(),
    val goalProgressLogs: List<GoalProgressLogBackupDto> = emptyList(),
    val recurrences: List<TaskRecurrenceBackupDto> = emptyList(),
    val recurrenceExceptions: List<RecurrenceExceptionBackupDto> = emptyList()
) {
    companion object {
        const val CURRENT_FORMAT_VERSION = 6
    }
}

@Serializable
data class SettingsBackupDto(
    val id: String = "nexus_settings_default",
    val userName: String = "Enzo",
    val darkTheme: Boolean = true,
    val declaredBalanceCents: Long = 0L,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

@Serializable
data class CategoryBackupDto(
    val id: String = "",
    val name: String = "",
    val colorHex: String = "#9A9A9A",
    val scope: String = "GENERAL",
    val isDefault: Boolean = false,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

@Serializable
data class ProjectBackupDto(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val deadlineAt: Long? = null,
    val status: String = "ACTIVE",
    val priority: String = "NORMAL",
    val goalId: String? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

@Serializable
data class EventBackupDto(
    val id: String = "",
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
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

@Serializable
data class TaskBackupDto(
    val id: String = "",
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
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

@Serializable
data class SubtaskBackupDto(
    val id: String = "",
    val taskId: String = "",
    val title: String = "",
    val isCompleted: Boolean = false,
    val sortOrder: Int = 0,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

@Serializable
data class AppointmentBackupDto(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val categoryId: String? = null,
    val eventId: String? = null,
    val startAt: Long = 0L,
    val endAt: Long = 0L,
    val isAllDay: Boolean = false,
    val location: String = "",
    val link: String = "",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

@Serializable
data class CreditCardBackupDto(
    val id: String = "",
    val name: String = "",
    val institution: String = "",
    val totalLimitCents: Long = 0L,
    val availableLimitCents: Long = 0L,
    val closingDay: Int = 10,
    val dueDay: Int = 17,
    val colorHex: String = "#9A9A9A",
    val notes: String = "",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

@Serializable
data class CreditCardStatementBackupDto(
    val id: String = "",
    val creditCardId: String = "",
    val yearMonth: String = "",
    val closingAt: Long = 0L,
    val dueAt: Long = 0L,
    val status: String = "OPEN",
    val paidAt: Long? = null,
    val paidAmountCents: Long = 0L,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

@Serializable
data class InstallmentPurchaseBackupDto(
    val id: String = "",
    val description: String = "",
    val totalAmountCents: Long = 0L,
    val installmentsCount: Int = 1,
    val installmentAmountCents: Long = 0L,
    val firstDueAt: Long = 0L,
    val creditCardId: String? = null,
    val categoryId: String? = null,
    val eventId: String? = null,
    val projectId: String? = null,
    val notes: String = "",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

@Serializable
data class RecurringRuleBackupDto(
    val id: String = "",
    val title: String = "",
    val type: String = "EXPENSE",
    val amountCents: Long = 0L,
    val categoryId: String? = null,
    val paymentMethod: String = "PIX",
    val creditCardId: String? = null,
    val frequency: String = "MONTHLY",
    val customIntervalDays: Int = 30,
    val startAt: Long = 0L,
    val endAt: Long? = null,
    val maxOccurrences: Int? = null,
    val isActive: Boolean = true,
    val notes: String = "",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

@Serializable
data class TransactionBackupDto(
    val id: String = "",
    val type: String = "EXPENSE",
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
    val notes: String = "",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

@Serializable
data class LoanBackupDto(
    val id: String = "",
    val creditor: String = "",
    val originalAmountCents: Long = 0L,
    val contractedAmountCents: Long = 0L,
    val installmentsTotal: Int = 1,
    val installmentAmountCents: Long = 0L,
    val paidInstallments: Int = 0,
    val nextDueAt: Long? = null,
    val remainingBalanceCents: Long = 0L,
    val finalDateAt: Long? = null,
    val notes: String = "",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

@Serializable
data class BudgetBackupDto(
    val id: String = "",
    val categoryId: String = "",
    val yearMonth: String = "",
    val limitCents: Long = 0L,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

@Serializable
data class FinancialGoalBackupDto(
    val id: String = "",
    val name: String = "",
    val targetAmountCents: Long = 0L,
    val currentAmountCents: Long = 0L,
    val deadlineAt: Long? = null,
    val categoryId: String? = null,
    val notes: String = "",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

@Serializable
data class AuditHistoryBackupDto(
    val id: String = "",
    val entityType: String = "",
    val entityId: String = "",
    val action: String = "",
    val summary: String = "",
    val occurredAt: Long = 0L,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

@Serializable
data class ErrorLogBackupDto(
    val id: String = "",
    val source: String = "",
    val message: String = "",
    val stackTrace: String = "",
    val occurredAt: Long = 0L,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

@Serializable
data class ReminderBackupDto(
    val id: String = "",
    val sourceType: String = "TASK",
    val sourceId: String = "",
    val category: String = "SCHEDULE",
    val title: String = "",
    val message: String = "",
    val triggerAt: Long = 0L,
    val originalDueAt: Long? = null,
    val minutesBefore: Int = 0,
    val channelId: String = "nexus_schedule",
    val status: String = "ACTIVE",
    val snoozedUntil: Long? = null,
    val isExactScheduled: Boolean = false,
    val firedAt: Long? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

@Serializable
data class RoutineAssistantSettingsBackupDto(
    val id: String = "nexus_routine_default",
    val notificationsEnabled: Boolean = true,
    val scheduleChannelEnabled: Boolean = true,
    val financeChannelEnabled: Boolean = true,
    val eventsChannelEnabled: Boolean = true,
    val routineChannelEnabled: Boolean = true,
    val morningBriefingEnabled: Boolean = true,
    val morningBriefingHour: Int = 8,
    val morningBriefingMinute: Int = 0,
    val nightReviewEnabled: Boolean = true,
    val nightReviewHour: Int = 21,
    val nightReviewMinute: Int = 0,
    val quietHoursEnabled: Boolean = false,
    val quietHoursStartHour: Int = 22,
    val quietHoursStartMinute: Int = 0,
    val quietHoursEndHour: Int = 7,
    val quietHoursEndMinute: Int = 0,
    val defaultTaskReminderMinutes: Int = 30,
    val defaultAppointmentReminderMinutes: Int = 30,
    val defaultEventReminderMinutes: Int = 60,
    val defaultFinanceReminderMinutes: Int = 0,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

@Serializable
data class BriefingHistoryBackupDto(
    val id: String = "",
    val briefingType: String = "MORNING",
    val dateIso: String = "",
    val summaryText: String = "",
    val tasksCount: Int = 0,
    val appointmentsCount: Int = 0,
    val billsDueCount: Int = 0,
    val overdueCount: Int = 0,
    val reviewedAt: Long? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

@Serializable
data class PersonalizationSettingsBackupDto(
    val id: String = "nexus_personalization_default",
    val greetingEnabled: Boolean = true,
    val greetingTemplate: String = "AUTO",
    val dateFormatPattern: String = "WEEKDAY_DD_MM_YYYY",
    val showDailyQuoteOnDashboard: Boolean = true,
    val compactDashboardMode: Boolean = false,
    val showMetricSummaryOnDashboard: Boolean = true,
    val dashboardBlocksJson: String = "",
    val lastQuoteDateIso: String? = null,
    val lastQuoteId: String? = null,
    val previousQuoteId: String? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

@Serializable
data class QuoteBackupDto(
    val id: String = "",
    val text: String = "",
    val author: String = "",
    val isActive: Boolean = true,
    val lastShownDateIso: String? = null,
    val shownCount: Int = 0,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

@Serializable
data class PersonalGoalBackupDto(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val categoryId: String? = null,
    val targetDateAt: Long? = null,
    val status: String = "ACTIVE",
    val progressMode: String = "AUTO",
    val manualProgressPercent: Int = 0,
    val completedAt: Long? = null,
    val notes: String = "",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

@Serializable
data class GoalMilestoneBackupDto(
    val id: String = "",
    val goalId: String = "",
    val title: String = "",
    val description: String = "",
    val dueAt: Long? = null,
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    val sortOrder: Int = 0,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

@Serializable
data class GoalProgressLogBackupDto(
    val id: String = "",
    val goalId: String = "",
    val progressPercent: Int = 0,
    val note: String = "",
    val recordedAt: Long = 0L,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

@Serializable
data class TaskRecurrenceBackupDto(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val projectId: String? = null,
    val categoryId: String? = null,
    val eventId: String? = null,
    val goalId: String? = null,
    val priority: String = "NORMAL",
    val notes: String = "",
    val hasDueTime: Boolean = false,
    val dueTimeMinutesOfDay: Int? = null,
    val reminderMinutesBefore: Int? = null,
    val frequency: String = "WEEKLY",
    val customIntervalDays: Int = 1,
    val weekdaysMask: Int = 0,
    val startDateIso: String = "",
    val endDateIso: String? = null,
    val maxOccurrences: Int? = null,
    val isActive: Boolean = true,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

@Serializable
data class RecurrenceExceptionBackupDto(
    val id: String = "",
    val recurrenceId: String = "",
    val occurrenceDateIso: String = "",
    val exceptionType: String = "SKIP",
    val overrideTitle: String? = null,
    val overrideDueAt: Long? = null,
    val taskId: String? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

data class BackupPreview(
    val payload: NexusBackupPayload,
    val formatVersion: Int,
    val appVersion: String,
    val exportedAt: String,
    val settingsCount: Int,
    val categoriesCount: Int,
    val projectsCount: Int,
    val eventsCount: Int,
    val tasksCount: Int,
    val subtasksCount: Int,
    val appointmentsCount: Int,
    val creditCardsCount: Int,
    val statementsCount: Int,
    val installmentPurchasesCount: Int,
    val recurringRulesCount: Int,
    val transactionsCount: Int,
    val loansCount: Int,
    val budgetsCount: Int,
    val financialGoalsCount: Int,
    val auditHistoryCount: Int,
    val errorLogsCount: Int,
    val remindersCount: Int = 0,
    val routineSettingsCount: Int = 0,
    val briefingHistoryCount: Int = 0,
    val personalizationSettingsCount: Int = 0,
    val quotesCount: Int = 0,
    val personalGoalsCount: Int = 0,
    val goalMilestonesCount: Int = 0,
    val goalProgressLogsCount: Int = 0,
    val recurrencesCount: Int = 0,
    val recurrenceExceptionsCount: Int = 0,
    val compatibilityNotice: String? = null
) {
    val totalRecords: Int
        get() = settingsCount + categoriesCount + projectsCount + eventsCount +
            tasksCount + subtasksCount + appointmentsCount + creditCardsCount +
            statementsCount + installmentPurchasesCount + recurringRulesCount +
            transactionsCount + loansCount + budgetsCount + financialGoalsCount +
            auditHistoryCount + errorLogsCount + remindersCount +
            routineSettingsCount + briefingHistoryCount + personalizationSettingsCount +
            quotesCount + personalGoalsCount + goalMilestonesCount + goalProgressLogsCount +
            recurrencesCount + recurrenceExceptionsCount
}

sealed interface BackupValidationResult {
    data class Valid(val preview: BackupPreview) : BackupValidationResult
    data class Invalid(val userMessage: String) : BackupValidationResult
}

data class RestoreSummary(
    val settingsRestored: Int,
    val categoriesRestored: Int,
    val projectsRestored: Int,
    val eventsRestored: Int,
    val tasksRestored: Int,
    val subtasksRestored: Int,
    val appointmentsRestored: Int,
    val creditCardsRestored: Int,
    val statementsRestored: Int,
    val installmentPurchasesRestored: Int,
    val recurringRulesRestored: Int,
    val transactionsRestored: Int,
    val loansRestored: Int,
    val budgetsRestored: Int,
    val financialGoalsRestored: Int,
    val auditHistoryRestored: Int,
    val errorLogsRestored: Int,
    val remindersRestored: Int = 0,
    val routineSettingsRestored: Int = 0,
    val briefingHistoryRestored: Int = 0,
    val personalizationSettingsRestored: Int = 0,
    val quotesRestored: Int = 0,
    val personalGoalsRestored: Int = 0,
    val goalMilestonesRestored: Int = 0,
    val goalProgressLogsRestored: Int = 0,
    val recurrencesRestored: Int = 0,
    val recurrenceExceptionsRestored: Int = 0
) {
    val totalRestored: Int
        get() = settingsRestored + categoriesRestored + projectsRestored + eventsRestored +
            tasksRestored + subtasksRestored + appointmentsRestored + creditCardsRestored +
            statementsRestored + installmentPurchasesRestored + recurringRulesRestored +
            transactionsRestored + loansRestored + budgetsRestored + financialGoalsRestored +
            auditHistoryRestored + errorLogsRestored + remindersRestored +
            routineSettingsRestored + briefingHistoryRestored + personalizationSettingsRestored +
            quotesRestored + personalGoalsRestored + goalMilestonesRestored + goalProgressLogsRestored +
            recurrencesRestored + recurrenceExceptionsRestored
}
