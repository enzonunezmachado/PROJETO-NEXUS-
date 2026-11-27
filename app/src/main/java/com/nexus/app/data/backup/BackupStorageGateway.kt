package com.nexus.app.data.backup

import androidx.room.withTransaction
import com.nexus.app.data.db.NexusDatabase
import com.nexus.app.data.db.entity.AppointmentEntity
import com.nexus.app.data.db.entity.AuditHistoryEntity
import com.nexus.app.data.db.entity.BriefingHistoryEntity
import com.nexus.app.data.db.entity.BudgetEntity
import com.nexus.app.data.db.entity.CategoryEntity
import com.nexus.app.data.db.entity.CreditCardEntity
import com.nexus.app.data.db.entity.CreditCardStatementEntity
import com.nexus.app.data.db.entity.ErrorLogEntity
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
import com.nexus.app.data.db.entity.RecurrenceExceptionEntity
import com.nexus.app.data.db.entity.RecurringRuleEntity
import com.nexus.app.data.db.entity.ReminderEntity
import com.nexus.app.data.db.entity.RoutineAssistantSettingsEntity
import com.nexus.app.data.db.entity.ScheduleChecklistItemEntity
import com.nexus.app.data.db.entity.SettingsEntity
import com.nexus.app.data.db.entity.SubtaskEntity
import com.nexus.app.data.db.entity.TaskEntity
import com.nexus.app.data.db.entity.TaskRecurrenceEntity
import com.nexus.app.data.db.entity.TransactionEntity

interface BackupStorageGateway {
    suspend fun <T> inTransaction(block: suspend () -> T): T

    suspend fun getSettings(): List<SettingsEntity>
    suspend fun getCategories(): List<CategoryEntity>
    suspend fun getProjects(): List<ProjectEntity>
    suspend fun getEvents(): List<EventEntity>
    suspend fun getTasks(): List<TaskEntity>
    suspend fun getSubtasks(): List<SubtaskEntity>
    suspend fun getAppointments(): List<AppointmentEntity>
    suspend fun getCreditCards(): List<CreditCardEntity>
    suspend fun getCreditCardStatements(): List<CreditCardStatementEntity>
    suspend fun getInstallmentPurchases(): List<InstallmentPurchaseEntity>
    suspend fun getRecurringRules(): List<RecurringRuleEntity>
    suspend fun getTransactions(): List<TransactionEntity>
    suspend fun getLoans(): List<LoanEntity>
    suspend fun getBudgets(): List<BudgetEntity>
    suspend fun getFinancialGoals(): List<FinancialGoalEntity>
    suspend fun getAuditHistory(): List<AuditHistoryEntity>
    suspend fun getErrorLogs(): List<ErrorLogEntity>
    suspend fun getReminders(): List<ReminderEntity>
    suspend fun getRoutineSettings(): List<RoutineAssistantSettingsEntity>
    suspend fun getBriefingHistory(): List<BriefingHistoryEntity>
    suspend fun getPersonalizationSettings(): List<PersonalizationSettingsEntity>
    suspend fun getQuotes(): List<QuoteEntity>
    suspend fun getPersonalGoals(): List<PersonalGoalEntity>
    suspend fun getGoalMilestones(): List<GoalMilestoneEntity>
    suspend fun getGoalProgressLogs(): List<GoalProgressLogEntity>
    suspend fun getTaskRecurrences(): List<TaskRecurrenceEntity>
    suspend fun getRecurrenceExceptions(): List<RecurrenceExceptionEntity>
    suspend fun getScheduleChecklistItems(): List<ScheduleChecklistItemEntity>

    suspend fun insertSettings(items: List<SettingsEntity>)
    suspend fun insertCategories(items: List<CategoryEntity>)
    suspend fun insertProjects(items: List<ProjectEntity>)
    suspend fun insertEvents(items: List<EventEntity>)
    suspend fun insertTasks(items: List<TaskEntity>)
    suspend fun insertSubtasks(items: List<SubtaskEntity>)
    suspend fun insertAppointments(items: List<AppointmentEntity>)
    suspend fun insertCreditCards(items: List<CreditCardEntity>)
    suspend fun insertCreditCardStatements(items: List<CreditCardStatementEntity>)
    suspend fun insertInstallmentPurchases(items: List<InstallmentPurchaseEntity>)
    suspend fun insertRecurringRules(items: List<RecurringRuleEntity>)
    suspend fun insertTransactions(items: List<TransactionEntity>)
    suspend fun insertLoans(items: List<LoanEntity>)
    suspend fun insertBudgets(items: List<BudgetEntity>)
    suspend fun insertFinancialGoals(items: List<FinancialGoalEntity>)
    suspend fun insertAuditHistory(items: List<AuditHistoryEntity>)
    suspend fun insertErrorLogs(items: List<ErrorLogEntity>)
    suspend fun insertReminders(items: List<ReminderEntity>)
    suspend fun insertRoutineSettings(items: List<RoutineAssistantSettingsEntity>)
    suspend fun insertBriefingHistory(items: List<BriefingHistoryEntity>)
    suspend fun insertPersonalizationSettings(items: List<PersonalizationSettingsEntity>)
    suspend fun insertQuotes(items: List<QuoteEntity>)
    suspend fun insertPersonalGoals(items: List<PersonalGoalEntity>)
    suspend fun insertGoalMilestones(items: List<GoalMilestoneEntity>)
    suspend fun insertGoalProgressLogs(items: List<GoalProgressLogEntity>)
    suspend fun insertTaskRecurrences(items: List<TaskRecurrenceEntity>)
    suspend fun insertRecurrenceExceptions(items: List<RecurrenceExceptionEntity>)
    suspend fun insertScheduleChecklistItems(items: List<ScheduleChecklistItemEntity>)

    suspend fun clearAllForFullRestore()
}

internal class RoomBackupStorageGateway(
    private val database: NexusDatabase
) : BackupStorageGateway {

    override suspend fun <T> inTransaction(block: suspend () -> T): T =
        database.withTransaction { block() }

    override suspend fun getSettings(): List<SettingsEntity> = database.settingsDao().getAllOnce()
    override suspend fun getCategories(): List<CategoryEntity> = database.categoryDao().getAllOnce()
    override suspend fun getProjects(): List<ProjectEntity> = database.projectDao().getAllOnce()
    override suspend fun getEvents(): List<EventEntity> = database.eventDao().getAllOnce()
    override suspend fun getTasks(): List<TaskEntity> = database.taskDao().getAllOnce()
    override suspend fun getSubtasks(): List<SubtaskEntity> = database.subtaskDao().getAllOnce()
    override suspend fun getAppointments(): List<AppointmentEntity> = database.appointmentDao().getAllOnce()
    override suspend fun getCreditCards(): List<CreditCardEntity> = database.creditCardDao().getAllOnce()
    override suspend fun getCreditCardStatements(): List<CreditCardStatementEntity> =
        database.creditCardStatementDao().getAllOnce()
    override suspend fun getInstallmentPurchases(): List<InstallmentPurchaseEntity> =
        database.installmentPurchaseDao().getAllOnce()
    override suspend fun getRecurringRules(): List<RecurringRuleEntity> =
        database.recurringRuleDao().getAllOnce()
    override suspend fun getTransactions(): List<TransactionEntity> =
        database.transactionDao().getAllOnce()
    override suspend fun getLoans(): List<LoanEntity> = database.loanDao().getAllOnce()
    override suspend fun getBudgets(): List<BudgetEntity> = database.budgetDao().getAllOnce()
    override suspend fun getFinancialGoals(): List<FinancialGoalEntity> =
        database.financialGoalDao().getAllOnce()
    override suspend fun getAuditHistory(): List<AuditHistoryEntity> =
        database.auditHistoryDao().getAllOnce()
    override suspend fun getErrorLogs(): List<ErrorLogEntity> = database.errorLogDao().getAllOnce()
    override suspend fun getReminders(): List<ReminderEntity> = database.reminderDao().getAllOnce()
    override suspend fun getRoutineSettings(): List<RoutineAssistantSettingsEntity> =
        database.routineAssistantSettingsDao().getAllOnce()
    override suspend fun getBriefingHistory(): List<BriefingHistoryEntity> =
        database.briefingHistoryDao().getAllOnce()
    override suspend fun getPersonalizationSettings(): List<PersonalizationSettingsEntity> =
        database.personalizationSettingsDao().getAllOnce()
    override suspend fun getQuotes(): List<QuoteEntity> = database.quoteDao().getAllOnce()
    override suspend fun getPersonalGoals(): List<PersonalGoalEntity> =
        database.personalGoalDao().getAllOnce()
    override suspend fun getGoalMilestones(): List<GoalMilestoneEntity> =
        database.goalMilestoneDao().getAllOnce()
    override suspend fun getGoalProgressLogs(): List<GoalProgressLogEntity> =
        database.goalProgressLogDao().getAllOnce()
    override suspend fun getTaskRecurrences(): List<TaskRecurrenceEntity> =
        database.taskRecurrenceDao().getAllOnce()
    override suspend fun getRecurrenceExceptions(): List<RecurrenceExceptionEntity> =
        database.recurrenceExceptionDao().getAllOnce()
    override suspend fun getScheduleChecklistItems(): List<ScheduleChecklistItemEntity> =
        database.scheduleChecklistItemDao().getAllOnce()

    override suspend fun insertSettings(items: List<SettingsEntity>) {
        if (items.isNotEmpty()) database.settingsDao().insertAll(items)
    }

    override suspend fun insertCategories(items: List<CategoryEntity>) {
        if (items.isNotEmpty()) database.categoryDao().insertAll(items)
    }

    override suspend fun insertProjects(items: List<ProjectEntity>) {
        if (items.isNotEmpty()) database.projectDao().insertAll(items)
    }

    override suspend fun insertEvents(items: List<EventEntity>) {
        if (items.isNotEmpty()) database.eventDao().insertAll(items)
    }

    override suspend fun insertTasks(items: List<TaskEntity>) {
        if (items.isNotEmpty()) database.taskDao().insertAll(items)
    }

    override suspend fun insertSubtasks(items: List<SubtaskEntity>) {
        if (items.isNotEmpty()) database.subtaskDao().insertAll(items)
    }

    override suspend fun insertAppointments(items: List<AppointmentEntity>) {
        if (items.isNotEmpty()) database.appointmentDao().insertAll(items)
    }

    override suspend fun insertCreditCards(items: List<CreditCardEntity>) {
        if (items.isNotEmpty()) database.creditCardDao().insertAll(items)
    }

    override suspend fun insertCreditCardStatements(items: List<CreditCardStatementEntity>) {
        if (items.isNotEmpty()) database.creditCardStatementDao().insertAll(items)
    }

    override suspend fun insertInstallmentPurchases(items: List<InstallmentPurchaseEntity>) {
        if (items.isNotEmpty()) database.installmentPurchaseDao().insertAll(items)
    }

    override suspend fun insertRecurringRules(items: List<RecurringRuleEntity>) {
        if (items.isNotEmpty()) database.recurringRuleDao().insertAll(items)
    }

    override suspend fun insertTransactions(items: List<TransactionEntity>) {
        if (items.isNotEmpty()) database.transactionDao().insertAll(items)
    }

    override suspend fun insertLoans(items: List<LoanEntity>) {
        if (items.isNotEmpty()) database.loanDao().insertAll(items)
    }

    override suspend fun insertBudgets(items: List<BudgetEntity>) {
        if (items.isNotEmpty()) database.budgetDao().insertAll(items)
    }

    override suspend fun insertFinancialGoals(items: List<FinancialGoalEntity>) {
        if (items.isNotEmpty()) database.financialGoalDao().insertAll(items)
    }

    override suspend fun insertAuditHistory(items: List<AuditHistoryEntity>) {
        if (items.isNotEmpty()) database.auditHistoryDao().insertAll(items)
    }

    override suspend fun insertErrorLogs(items: List<ErrorLogEntity>) {
        if (items.isNotEmpty()) database.errorLogDao().insertAll(items)
    }

    override suspend fun insertReminders(items: List<ReminderEntity>) {
        if (items.isNotEmpty()) database.reminderDao().insertAll(items)
    }

    override suspend fun insertRoutineSettings(items: List<RoutineAssistantSettingsEntity>) {
        if (items.isNotEmpty()) database.routineAssistantSettingsDao().insertAll(items)
    }

    override suspend fun insertBriefingHistory(items: List<BriefingHistoryEntity>) {
        if (items.isNotEmpty()) database.briefingHistoryDao().insertAll(items)
    }

    override suspend fun insertPersonalizationSettings(items: List<PersonalizationSettingsEntity>) {
        if (items.isNotEmpty()) database.personalizationSettingsDao().insertAll(items)
    }

    override suspend fun insertQuotes(items: List<QuoteEntity>) {
        if (items.isNotEmpty()) database.quoteDao().insertAll(items)
    }

    override suspend fun insertPersonalGoals(items: List<PersonalGoalEntity>) {
        if (items.isNotEmpty()) database.personalGoalDao().insertAll(items)
    }

    override suspend fun insertGoalMilestones(items: List<GoalMilestoneEntity>) {
        if (items.isNotEmpty()) database.goalMilestoneDao().insertAll(items)
    }

    override suspend fun insertGoalProgressLogs(items: List<GoalProgressLogEntity>) {
        if (items.isNotEmpty()) database.goalProgressLogDao().insertAll(items)
    }

    override suspend fun insertTaskRecurrences(items: List<TaskRecurrenceEntity>) {
        if (items.isNotEmpty()) database.taskRecurrenceDao().insertAll(items)
    }

    override suspend fun insertRecurrenceExceptions(items: List<RecurrenceExceptionEntity>) {
        if (items.isNotEmpty()) database.recurrenceExceptionDao().insertAll(items)
    }

    override suspend fun insertScheduleChecklistItems(items: List<ScheduleChecklistItemEntity>) {
        if (items.isNotEmpty()) database.scheduleChecklistItemDao().insertAll(items)
    }

    override suspend fun clearAllForFullRestore() {
        database.goalProgressLogDao().deleteAll()
        database.goalMilestoneDao().deleteAll()
        database.reminderDao().deleteAll()
        database.briefingHistoryDao().deleteAll()
        database.routineAssistantSettingsDao().deleteAll()
        database.personalizationSettingsDao().deleteAll()
        database.quoteDao().deleteAll()
        database.transactionDao().deleteAll()
        database.subtaskDao().deleteAll()
        database.scheduleChecklistItemDao().deleteAll()
        database.recurrenceExceptionDao().deleteAll()
        database.taskDao().deleteAll()
        database.taskRecurrenceDao().deleteAll()
        database.appointmentDao().deleteAll()
        database.installmentPurchaseDao().deleteAll()
        database.recurringRuleDao().deleteAll()
        database.creditCardStatementDao().deleteAll()
        database.budgetDao().deleteAll()
        database.financialGoalDao().deleteAll()
        database.loanDao().deleteAll()
        database.eventDao().deleteAll()
        database.creditCardDao().deleteAll()
        database.projectDao().deleteAll()
        database.personalGoalDao().deleteAll()
        database.categoryDao().deleteAll()
        database.settingsDao().deleteAll()
        database.auditHistoryDao().deleteAll()
        database.errorLogDao().deleteAll()
    }
}
