package com.nexus.app.domain.model

import com.nexus.app.data.db.entity.AppointmentEntity
import com.nexus.app.data.db.entity.BudgetEntity
import com.nexus.app.data.db.entity.CategoryEntity
import com.nexus.app.data.db.entity.CreditCardEntity
import com.nexus.app.data.db.entity.CreditCardStatementEntity
import com.nexus.app.data.db.entity.EventEntity
import com.nexus.app.data.db.entity.FinancialGoalEntity
import com.nexus.app.data.db.entity.GoalMilestoneEntity
import com.nexus.app.data.db.entity.GoalProgressLogEntity
import com.nexus.app.data.db.entity.InstallmentPurchaseEntity
import com.nexus.app.data.db.entity.LoanEntity
import com.nexus.app.data.db.entity.PersonalGoalEntity
import com.nexus.app.data.db.entity.PersonalizationSettingsEntity
import com.nexus.app.data.db.entity.ProjectEntity
import com.nexus.app.data.db.entity.QuoteEntity
import com.nexus.app.data.db.entity.RecurringRuleEntity
import com.nexus.app.data.db.entity.SubtaskEntity
import com.nexus.app.data.db.entity.TaskEntity
import com.nexus.app.data.db.entity.TaskRecurrenceEntity
import com.nexus.app.data.db.entity.TransactionEntity
import kotlinx.serialization.Serializable
import java.time.LocalDate

data class TaskRecurrenceDraft(
    val frequency: TaskRecurrenceFrequency,
    val customIntervalDays: Int = 1,
    val weekdaysMask: Int = 0,
    val startDateIso: String,
    val endDateIso: String? = null,
    val maxOccurrences: Int? = null,
    val templateSubtasks: List<String>? = null
)

enum class RecurringEditScope(val label: String) {
    THIS_OCCURRENCE_ONLY("Só esta ocorrência"),
    ENTIRE_SERIES("Toda a série")
}

data class TaskWithDetails(
    val task: TaskEntity,
    val project: ProjectEntity?,
    val category: CategoryEntity?,
    val event: EventEntity? = null,
    val subtasks: List<SubtaskEntity>,
    val linkedTransactions: List<TransactionEntity> = emptyList(),
    val recurrence: TaskRecurrenceEntity? = null
) {
    val completedSubtasksCount: Int get() = subtasks.count { it.isCompleted }
    val isCompleted: Boolean get() = task.status == TaskStatus.COMPLETED
    val isCancelled: Boolean get() = task.status == TaskStatus.CANCELLED
    val totalExpenseCents: Long
        get() = linkedTransactions
            .filter { it.type == TransactionType.EXPENSE && it.status != FinancialStatus.CANCELLED }
            .sumOf { it.amountCents }
}

data class ProjectWithProgress(
    val project: ProjectEntity,
    val totalTasks: Int,
    val completedTasks: Int,
    val estimatedCostCents: Long = 0L,
    val paidCostCents: Long = 0L,
    val linkedTransactionsCount: Int = 0
) {
    val progressFraction: Float
        get() = if (totalTasks == 0) 0f else completedTasks.toFloat() / totalTasks.toFloat()
    val progressPercent: Int
        get() = (progressFraction * 100f).toInt()
}

data class TransactionWithDetails(
    val transaction: TransactionEntity,
    val category: CategoryEntity?,
    val project: ProjectEntity?,
    val event: EventEntity?,
    val creditCard: CreditCardEntity?,
    val task: TaskEntity? = null
)

data class EventWithDetails(
    val event: EventEntity,
    val category: CategoryEntity?,
    val creditCard: CreditCardEntity?,
    val linkedTasks: List<TaskWithDetails>,
    val linkedTransactions: List<TransactionWithDetails>,
    val checklistDoneCount: Int = 0,
    val checklistTotalCount: Int = 0
) {
    val estimatedTotalCostCents: Long
        get() = linkedTransactions
            .filter { it.transaction.type == TransactionType.EXPENSE && it.transaction.status != FinancialStatus.CANCELLED }
            .sumOf { it.transaction.amountCents }

    val paidTotalCostCents: Long
        get() = linkedTransactions
            .filter { it.transaction.type == TransactionType.EXPENSE && it.transaction.status == FinancialStatus.SETTLED }
            .sumOf { it.transaction.amountCents }
}

data class StatementWithDetails(
    val statement: CreditCardStatementEntity,
    val card: CreditCardEntity,
    val transactions: List<TransactionWithDetails>
) {
    val totalCents: Long
        get() = transactions
            .filter { it.transaction.type == TransactionType.EXPENSE && it.transaction.status != FinancialStatus.CANCELLED }
            .sumOf { it.transaction.amountCents }
}

data class CardWithStatements(
    val card: CreditCardEntity,
    val statements: List<StatementWithDetails>,
    val currentOpenTotalCents: Long
) {
    val computedRemainingLimitCents: Long
        get() = (card.totalLimitCents - currentOpenTotalCents).coerceAtLeast(0L)
}

data class InstallmentPurchaseWithItems(
    val purchase: InstallmentPurchaseEntity,
    val card: CreditCardEntity?,
    val category: CategoryEntity?,
    val installments: List<TransactionEntity>
) {
    val paidCount: Int get() = installments.count { it.status == FinancialStatus.SETTLED }
    val paidCents: Long get() = installments.filter { it.status == FinancialStatus.SETTLED }.sumOf { it.amountCents }
    val remainingCents: Long get() = installments.filter { it.status != FinancialStatus.SETTLED && it.status != FinancialStatus.CANCELLED }.sumOf { it.amountCents }
}

data class BudgetProgress(
    val budget: BudgetEntity,
    val category: CategoryEntity,
    val spentCents: Long
) {
    val remainingCents: Long get() = (budget.limitCents - spentCents).coerceAtLeast(0L)
    val exceededCents: Long get() = (spentCents - budget.limitCents).coerceAtLeast(0L)
    val usedFraction: Float
        get() = if (budget.limitCents <= 0L) 0f else (spentCents.toFloat() / budget.limitCents.toFloat()).coerceIn(0f, 1f)
    val usedPercent: Int
        get() = if (budget.limitCents <= 0L) 0 else ((spentCents * 100L) / budget.limitCents).toInt()
}

data class CategorySpendPoint(
    val categoryId: String? = null,
    val categoryName: String,
    val colorHex: String,
    val totalCents: Long
)

data class MonthlyCashflowPoint(
    val yearMonthLabel: String,
    val receivedIncomeCents: Long,
    val paidExpenseCents: Long,
    val yearMonthIso: String = yearMonthLabel
)

data class FinanceSummary(
    val declaredBalanceCents: Long = 0L,
    val receivedIncomeCents: Long = 0L,
    val plannedIncomeCents: Long = 0L,
    val paidExpenseCents: Long = 0L,
    val pendingExpenseCents: Long = 0L,
    val overdueExpenseCents: Long = 0L,
    val nextDueTransaction: TransactionWithDetails? = null,
    val categorySpend: List<CategorySpendPoint> = emptyList(),
    val monthlyCashflow: List<MonthlyCashflowPoint> = emptyList()
) {
    val monthResultCents: Long get() = receivedIncomeCents - paidExpenseCents
}

data class TaskDistributionSummary(
    val completedCount: Int = 0,
    val pendingCount: Int = 0,
    val inProgressCount: Int = 0,
    val overdueCount: Int = 0,
    val undatedPendingCount: Int = 0
) {
    val totalInDistribution: Int get() = completedCount + pendingCount + inProgressCount
}

/**
 * Item unificado exibido na Agenda sem duplicar registros no banco.
 */
sealed interface AgendaItem {
    val id: String
    val sortEpochMillis: Long
    val date: LocalDate
    val category: CategoryEntity?

    data class AppointmentItem(
        val appointment: AppointmentEntity,
        override val category: CategoryEntity?,
        val conflictTitles: List<String> = emptyList(),
        override val date: LocalDate,
        val event: EventEntity? = null,
        val recurrenceId: String? = null,
        val occurrenceDateIso: String? = null,
        val isVirtualRecurrence: Boolean = false,
        val checklistDoneCount: Int = 0,
        val checklistTotalCount: Int = 0
    ) : AgendaItem {
        override val id: String = if (isVirtualRecurrence && recurrenceId != null && occurrenceDateIso != null) {
            "appt_rec_${recurrenceId}_${occurrenceDateIso}"
        } else {
            "appt_${appointment.id}"
        }
        override val sortEpochMillis: Long = appointment.startAt
        val hasConflict: Boolean get() = conflictTitles.isNotEmpty()
    }

    data class TaskDeadlineItem(
        val task: TaskEntity,
        val project: ProjectEntity?,
        val event: EventEntity?,
        override val category: CategoryEntity?,
        val isOverdue: Boolean,
        override val date: LocalDate
    ) : AgendaItem {
        override val id: String = "task_${task.id}"
        override val sortEpochMillis: Long = task.dueAt ?: task.createdAt
    }

    data class EventItem(
        val event: EventEntity,
        override val category: CategoryEntity?,
        val totalCostCents: Long,
        override val date: LocalDate,
        val spanStartDate: LocalDate = date,
        val spanEndDate: LocalDate = date,
        val recurrenceId: String? = null,
        val occurrenceDateIso: String? = null,
        val seriesOccurrenceDateIso: String? = null,
        val isVirtualRecurrence: Boolean = false,
        val checklistDoneCount: Int = 0,
        val checklistTotalCount: Int = 0
    ) : AgendaItem {
        override val id: String = if (isVirtualRecurrence && recurrenceId != null && (seriesOccurrenceDateIso ?: occurrenceDateIso) != null) {
            "event_rec_${recurrenceId}_${seriesOccurrenceDateIso ?: occurrenceDateIso}:${date}"
        } else {
            "event_${event.id}:${date}"
        }
        override val sortEpochMillis: Long = event.startAt
        val isMultiDay: Boolean get() = spanStartDate != spanEndDate
    }

    data class FinancialDueItem(
        val transaction: TransactionEntity,
        override val category: CategoryEntity?,
        val isOverdue: Boolean,
        override val date: LocalDate
    ) : AgendaItem {
        override val id: String = "tx_${transaction.id}"
        override val sortEpochMillis: Long = transaction.dueAt ?: transaction.occurredAt
    }

    data class StatementDueItem(
        val statement: CreditCardStatementEntity,
        val card: CreditCardEntity,
        val totalCents: Long,
        override val date: LocalDate
    ) : AgendaItem {
        override val id: String = "stmt_${statement.id}"
        override val sortEpochMillis: Long = statement.dueAt
        override val category: CategoryEntity? = null
    }

    data class ProjectDeadlineItem(
        val project: ProjectEntity,
        override val date: LocalDate
    ) : AgendaItem {
        override val id: String = "proj_${project.id}"
        override val sortEpochMillis: Long = project.deadlineAt ?: project.createdAt
        override val category: CategoryEntity? = null
    }
}

@Serializable
data class DashboardBlockConfig(
    val type: DashboardBlockType,
    val enabled: Boolean = true,
    val order: Int = 0,
    val maxItems: Int = 4,
    val showPrimaryDetail: Boolean = true,
    val showSecondaryDetail: Boolean = true,
    val showOverdueOrAlerts: Boolean = true
)

data class DailyQuoteSelection(
    val quote: QuoteEntity? = null,
    val libraryStatus: QuoteLibraryStatus = QuoteLibraryStatus.EMPTY,
    val totalQuotesCount: Int = 0,
    val activeQuotesCount: Int = 0
)

enum class GoalStatusFilter(val label: String) {
    ACTIVE("Ativos"),
    OVERDUE("Atrasados"),
    PAUSED("Pausados"),
    COMPLETED("Concluídos"),
    ALL("Todos")
}

data class PersonalGoalWithDetails(
    val goal: PersonalGoalEntity,
    val category: CategoryEntity? = null,
    val milestones: List<GoalMilestoneEntity> = emptyList(),
    val linkedTasks: List<TaskWithDetails> = emptyList(),
    val linkedProjects: List<ProjectWithProgress> = emptyList(),
    val progressLogs: List<GoalProgressLogEntity> = emptyList(),
    val isOverdue: Boolean = false,
    val progressPercent: Int = 0
) {
    val completedMilestonesCount: Int get() = milestones.count { it.isCompleted }
    val completedTasksCount: Int get() = linkedTasks.count { it.task.status == TaskStatus.COMPLETED }
    val completedProjectsCount: Int get() = linkedProjects.count { it.project.status == ProjectStatus.COMPLETED }
}

data class DashboardSnapshot(
    val userName: String = "Enzo",
    val todayAppointments: List<AgendaItem.AppointmentItem> = emptyList(),
    val nextAppointment: AgendaItem.AppointmentItem? = null,
    val priorityTasksToday: List<TaskWithDetails> = emptyList(),
    val overdueTasks: List<TaskWithDetails> = emptyList(),
    val upcomingEvents: List<EventWithDetails> = emptyList(),
    val financeSummary: FinanceSummary = FinanceSummary(),
    val pendingCount: Int = 0,
    val completedCount: Int = 0,
    val taskDistribution: TaskDistributionSummary = TaskDistributionSummary(),
    val activeProjectsCount: Int = 0,
    val totalItemsInDb: Int = 0,
    val personalization: PersonalizationSettingsEntity? = null,
    val blocks: List<DashboardBlockConfig> = emptyList(),
    val dailyQuoteSelection: DailyQuoteSelection = DailyQuoteSelection(),
    val personalGoals: List<PersonalGoalWithDetails> = emptyList(),
    val financialGoals: List<FinancialGoalEntity> = emptyList(),
    val upcomingPayments: List<TransactionWithDetails> = emptyList(),
    val openStatements: List<StatementWithDetails> = emptyList(),
    val activeProjects: List<ProjectWithProgress> = emptyList()
)

data class DailyBriefingSnapshot(
    val dateIso: String = "",
    val todayAppointments: List<AgendaItem.AppointmentItem> = emptyList(),
    val todayTasks: List<TaskWithDetails> = emptyList(),
    val overdueTasks: List<TaskWithDetails> = emptyList(),
    val todayEvents: List<EventWithDetails> = emptyList(),
    val billsDueToday: List<TransactionWithDetails> = emptyList(),
    val overdueBills: List<TransactionWithDetails> = emptyList(),
    val statementsDueTodayOrOverdue: List<StatementWithDetails> = emptyList(),
    val isReviewedToday: Boolean = false
) {
    val totalBillsDueAmountCents: Long
        get() = billsDueToday.filter { it.transaction.type == TransactionType.EXPENSE }
            .sumOf { it.transaction.amountCents } +
            overdueBills.filter { it.transaction.type == TransactionType.EXPENSE }
                .sumOf { it.transaction.amountCents }
}

data class NightReviewSnapshot(
    val dateIso: String = "",
    val completedTasksToday: List<TaskWithDetails> = emptyList(),
    val pendingTasksTodayOrOverdue: List<TaskWithDetails> = emptyList(),
    val pendingBillsTodayOrOverdue: List<TransactionWithDetails> = emptyList(),
    val tomorrowAppointments: List<AgendaItem.AppointmentItem> = emptyList(),
    val tomorrowTasks: List<TaskWithDetails> = emptyList(),
    val tomorrowEvents: List<EventWithDetails> = emptyList(),
    val tomorrowBills: List<TransactionWithDetails> = emptyList(),
    val isReviewedTonight: Boolean = false
)

data class DatabaseTableCounts(
    val settings: Int = 0,
    val categories: Int = 0,
    val projects: Int = 0,
    val events: Int = 0,
    val tasks: Int = 0,
    val subtasks: Int = 0,
    val appointments: Int = 0,
    val creditCards: Int = 0,
    val statements: Int = 0,
    val installmentPurchases: Int = 0,
    val recurringRules: Int = 0,
    val transactions: Int = 0,
    val loans: Int = 0,
    val budgets: Int = 0,
    val financialGoals: Int = 0,
    val auditHistory: Int = 0,
    val errorLogs: Int = 0,
    val reminders: Int = 0,
    val routineSettings: Int = 0,
    val briefingHistory: Int = 0,
    val personalizationSettings: Int = 0,
    val quotes: Int = 0,
    val personalGoals: Int = 0,
    val goalMilestones: Int = 0,
    val goalProgressLogs: Int = 0,
    val recurrences: Int = 0,
    val recurrenceExceptions: Int = 0,
    val scheduleChecklistItems: Int = 0
)

data class FactoryResetSummary(
    val deletedRowsTotal: Int,
    val cancelledRemindersCount: Int,
    val clearedMarketFavoritesCount: Int,
    val clearedMarketAlertsCount: Int,
    val marketCachesCleared: Boolean,
    val inboxTrackingCleared: Boolean,
    val preservedCredentials: Boolean,
    val preservedInboxConfig: Boolean,
    val restoredDefaultCategoriesCount: Int
)

data class FactoryResetExternalState(
    val clearedMarketFavoritesCount: Int = 0,
    val clearedMarketAlertsCount: Int = 0,
    val marketCachesCleared: Boolean = false,
    val inboxTrackingCleared: Boolean = false,
    val preservedCredentials: Boolean = true,
    val preservedInboxConfig: Boolean = true
)

data class GlobalSearchResults(
    val tasks: List<TaskWithDetails> = emptyList(),
    val projects: List<ProjectWithProgress> = emptyList(),
    val events: List<EventWithDetails> = emptyList(),
    val appointments: List<AgendaItem.AppointmentItem> = emptyList(),
    val transactions: List<TransactionWithDetails> = emptyList(),
    val creditCards: List<CreditCardEntity> = emptyList(),
    val personalGoals: List<PersonalGoalWithDetails> = emptyList()
) {
    val isEmpty: Boolean
        get() = tasks.isEmpty() &&
            projects.isEmpty() &&
            events.isEmpty() &&
            appointments.isEmpty() &&
            transactions.isEmpty() &&
            creditCards.isEmpty() &&
            personalGoals.isEmpty()
}
