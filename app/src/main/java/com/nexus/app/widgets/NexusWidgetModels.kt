package com.nexus.app.widgets

import com.nexus.app.domain.model.AgendaItem
import com.nexus.app.domain.model.EventWithDetails
import com.nexus.app.domain.model.FinancialStatus
import com.nexus.app.domain.model.StatementWithDetails
import com.nexus.app.domain.model.TaskPriority
import com.nexus.app.domain.model.TaskStatus
import com.nexus.app.domain.model.TaskWithDetails
import com.nexus.app.domain.model.TransactionType
import java.time.LocalDate

enum class NexusWidgetType(val title: String) {
    MY_DAY("Meu Dia"),
    MY_TASKS("Minhas Tarefas"),
    FINANCE("Financeiro"),
    NEXUS_SUMMARY("Resumo NEXUS")
}

data class NexusWidgetInstanceConfig(
    val appWidgetId: Int,
    val widgetType: NexusWidgetType,
    val compactMode: Boolean = false,
    val maxItems: Int = 4,
    // Meu Dia
    val showAppointments: Boolean = true,
    val showTasksDueToday: Boolean = true,
    val showTaskCounters: Boolean = true,
    // Minhas Tarefas
    val showProjectName: Boolean = true,
    val showDueAndStatus: Boolean = true,
    val showPendingCounter: Boolean = true,
    // Financeiro
    val showDeclaredBalance: Boolean = true,
    val showPlannedTotals: Boolean = true,
    val showOverdueAndOpenStatements: Boolean = true,
    val showNextDue: Boolean = true,
    // Resumo NEXUS
    val showTasksSection: Boolean = true,
    val showTodayAppointmentsSection: Boolean = true,
    val showEventsSection: Boolean = true,
    val showFinanceSection: Boolean = true,
    val showGoalsSection: Boolean = true
)

data class MyDayAppointmentRow(
    val id: String,
    val title: String,
    val timeLabel: String,
    val location: String,
    val hasConflict: Boolean
)

data class MyDayTaskRow(
    val id: String,
    val title: String,
    val priority: TaskPriority,
    val status: TaskStatus,
    val timeLabel: String,
    val isOverdue: Boolean
)

data class MyDayWidgetSnapshot(
    val date: LocalDate,
    val dateFormatted: String,
    val appointments: List<MyDayAppointmentRow>,
    val tasksDueToday: List<MyDayTaskRow>,
    val completedTasksTodayCount: Int,
    val totalCompletedTasksCount: Int,
    val pendingTasksCount: Int
) {
    val isEmptySchedule: Boolean
        get() = appointments.isEmpty() && tasksDueToday.isEmpty()
}

data class WidgetTaskRow(
    val id: String,
    val title: String,
    val projectName: String?,
    val dueLabel: String?,
    val priority: TaskPriority,
    val status: TaskStatus,
    val isOverdue: Boolean,
    val canCompleteFromWidget: Boolean
)

data class MyTasksWidgetSnapshot(
    val tasks: List<WidgetTaskRow>,
    val pendingTasksCount: Int,
    val urgentOrHighCount: Int
) {
    val isEmpty: Boolean get() = tasks.isEmpty()
}

data class WidgetDueTransactionRow(
    val id: String,
    val description: String,
    val type: TransactionType,
    val amountCents: Long,
    val dueAt: Long,
    val status: FinancialStatus,
    val isOverdue: Boolean
)

data class WidgetOpenStatementRow(
    val statementId: String,
    val cardName: String,
    val yearMonth: String,
    val totalCents: Long,
    val dueAt: Long,
    val isOverdue: Boolean
)

data class FinanceWidgetSnapshot(
    val declaredBalanceCents: Long,
    val plannedIncomeCents: Long,
    val receivedIncomeCents: Long,
    val plannedExpenseCents: Long,
    val paidExpenseCents: Long,
    val overdueExpenseCents: Long,
    val overdueCount: Int,
    val openStatementsCount: Int,
    val openStatementsTotalCents: Long,
    val overdueStatementsCount: Int,
    val upcomingDueTransactions: List<WidgetDueTransactionRow>,
    val openStatements: List<WidgetOpenStatementRow>
) {
    val isEmpty: Boolean
        get() = declaredBalanceCents == 0L &&
            plannedIncomeCents == 0L &&
            receivedIncomeCents == 0L &&
            plannedExpenseCents == 0L &&
            paidExpenseCents == 0L &&
            overdueExpenseCents == 0L &&
            upcomingDueTransactions.isEmpty() &&
            openStatements.isEmpty()
}

data class NexusSummaryWidgetSnapshot(
    val dateFormatted: String,
    val pendingTasksCount: Int,
    val overdueTasksCount: Int,
    val topTask: TaskWithDetails?,
    val todayAppointmentsCount: Int,
    val nextAppointment: AgendaItem.AppointmentItem?,
    val upcomingEventsCount: Int,
    val nextEvent: EventWithDetails?,
    val upcomingDueCount: Int,
    val overdueBillsCount: Int,
    val nextDueTransaction: WidgetDueTransactionRow?,
    val openStatementsCount: Int,
    val openStatements: List<StatementWithDetails>,
    val goalsCount: Int,
    val goalsTotalTargetCents: Long,
    val goalsTotalCurrentCents: Long,
    val topGoalName: String?
) {
    val hasGoals: Boolean get() = goalsCount > 0 && goalsTotalTargetCents > 0L
    val isCompletelyEmpty: Boolean
        get() = pendingTasksCount == 0 &&
            todayAppointmentsCount == 0 &&
            upcomingEventsCount == 0 &&
            upcomingDueCount == 0 &&
            overdueBillsCount == 0 &&
            openStatementsCount == 0 &&
            goalsCount == 0
}

data class AllWidgetSnapshots(
    val myDay: MyDayWidgetSnapshot,
    val myTasks: MyTasksWidgetSnapshot,
    val finance: FinanceWidgetSnapshot,
    val summary: NexusSummaryWidgetSnapshot,
    val loadedAtEpoch: Long
)

sealed interface WidgetDataState<out T> {
    data class Ready<T>(val data: T) : WidgetDataState<T>
    data class Error(val message: String) : WidgetDataState<Nothing>
}
