package com.nexus.app

import com.nexus.app.data.backup.AppointmentBackupDto
import com.nexus.app.data.backup.BackupImportMode
import com.nexus.app.data.backup.BackupManager
import com.nexus.app.data.backup.BackupStorageGateway
import com.nexus.app.data.backup.BackupValidationResult
import com.nexus.app.data.backup.BudgetBackupDto
import com.nexus.app.data.backup.CategoryBackupDto
import com.nexus.app.data.backup.CreditCardBackupDto
import com.nexus.app.data.backup.CreditCardStatementBackupDto
import com.nexus.app.data.backup.EventBackupDto
import com.nexus.app.data.backup.GoalMilestoneBackupDto
import com.nexus.app.data.backup.InstallmentPurchaseBackupDto
import com.nexus.app.data.backup.MarketBackupConfigDto
import com.nexus.app.data.backup.MarketPriceAlertBackupDto
import com.nexus.app.data.backup.MergeIssueKind
import com.nexus.app.data.backup.NexusBackupPayload
import com.nexus.app.data.backup.PersonalGoalBackupDto
import com.nexus.app.data.backup.PersonalizationSettingsBackupDto
import com.nexus.app.data.backup.ProjectBackupDto
import com.nexus.app.data.backup.RecurrenceExceptionBackupDto
import com.nexus.app.data.backup.RecurringRuleBackupDto
import com.nexus.app.data.backup.ReminderBackupDto
import com.nexus.app.data.backup.RoutineAssistantSettingsBackupDto
import com.nexus.app.data.backup.SettingsBackupDto
import com.nexus.app.data.backup.SubtaskBackupDto
import com.nexus.app.data.backup.TaskBackupDto
import com.nexus.app.data.backup.TaskRecurrenceBackupDto
import com.nexus.app.data.backup.TransactionBackupDto
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
import com.nexus.app.data.market.MarketAlertRepository
import com.nexus.app.data.market.MarketAlertStore
import com.nexus.app.data.market.MarketWatchlistRepository
import com.nexus.app.data.market.MarketWatchlistStore
import com.nexus.app.domain.model.CategoryScope
import com.nexus.app.domain.model.EventLifecycleStatus
import com.nexus.app.domain.model.FinancialStatus
import com.nexus.app.domain.model.MarketAlertCondition
import com.nexus.app.domain.model.MarketAlertStatus
import com.nexus.app.domain.model.MarketAssetCatalog
import com.nexus.app.domain.model.MarketPriceAlert
import com.nexus.app.domain.model.PaymentMethod
import com.nexus.app.domain.model.ProjectStatus
import com.nexus.app.domain.model.RecurrenceDomain
import com.nexus.app.domain.model.RecurrenceExceptionType
import com.nexus.app.domain.model.RecurrenceFrequency
import com.nexus.app.domain.model.ReminderCategory
import com.nexus.app.domain.model.ReminderSourceType
import com.nexus.app.domain.model.StatementStatus
import com.nexus.app.domain.model.TaskPriority
import com.nexus.app.domain.model.TaskRecurrenceFrequency
import com.nexus.app.domain.model.TaskStatus
import com.nexus.app.domain.model.TicketStatus
import com.nexus.app.domain.model.TransactionType
import com.nexus.app.domain.rules.RecurrenceEngine
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class BackupIncrementalMergeUnitTests {

    private class InMemoryBackupStorageGateway : BackupStorageGateway {
        var clearAllCallsCount: Int = 0
            private set

        var failOnTableInsert: String? = null

        val settings = mutableListOf<SettingsEntity>()
        val categories = mutableListOf<CategoryEntity>()
        val projects = mutableListOf<ProjectEntity>()
        val events = mutableListOf<EventEntity>()
        val tasks = mutableListOf<TaskEntity>()
        val subtasks = mutableListOf<SubtaskEntity>()
        val appointments = mutableListOf<AppointmentEntity>()
        val creditCards = mutableListOf<CreditCardEntity>()
        val creditCardStatements = mutableListOf<CreditCardStatementEntity>()
        val installmentPurchases = mutableListOf<InstallmentPurchaseEntity>()
        val recurringRules = mutableListOf<RecurringRuleEntity>()
        val transactions = mutableListOf<TransactionEntity>()
        val loans = mutableListOf<LoanEntity>()
        val budgets = mutableListOf<BudgetEntity>()
        val financialGoals = mutableListOf<FinancialGoalEntity>()
        val auditHistory = mutableListOf<AuditHistoryEntity>()
        val errorLogs = mutableListOf<ErrorLogEntity>()
        val reminders = mutableListOf<ReminderEntity>()
        val routineSettings = mutableListOf<RoutineAssistantSettingsEntity>()
        val briefingHistory = mutableListOf<BriefingHistoryEntity>()
        val personalizationSettings = mutableListOf<PersonalizationSettingsEntity>()
        val quotes = mutableListOf<QuoteEntity>()
        val personalGoals = mutableListOf<PersonalGoalEntity>()
        val goalMilestones = mutableListOf<GoalMilestoneEntity>()
        val goalProgressLogs = mutableListOf<GoalProgressLogEntity>()
        val taskRecurrences = mutableListOf<TaskRecurrenceEntity>()
        val recurrenceExceptions = mutableListOf<RecurrenceExceptionEntity>()
        val scheduleChecklistItems = mutableListOf<ScheduleChecklistItemEntity>()

        private data class Snapshot(
            val settings: List<SettingsEntity>,
            val categories: List<CategoryEntity>,
            val projects: List<ProjectEntity>,
            val events: List<EventEntity>,
            val tasks: List<TaskEntity>,
            val subtasks: List<SubtaskEntity>,
            val appointments: List<AppointmentEntity>,
            val creditCards: List<CreditCardEntity>,
            val creditCardStatements: List<CreditCardStatementEntity>,
            val installmentPurchases: List<InstallmentPurchaseEntity>,
            val recurringRules: List<RecurringRuleEntity>,
            val transactions: List<TransactionEntity>,
            val loans: List<LoanEntity>,
            val budgets: List<BudgetEntity>,
            val financialGoals: List<FinancialGoalEntity>,
            val auditHistory: List<AuditHistoryEntity>,
            val errorLogs: List<ErrorLogEntity>,
            val reminders: List<ReminderEntity>,
            val routineSettings: List<RoutineAssistantSettingsEntity>,
            val briefingHistory: List<BriefingHistoryEntity>,
            val personalizationSettings: List<PersonalizationSettingsEntity>,
            val quotes: List<QuoteEntity>,
            val personalGoals: List<PersonalGoalEntity>,
            val goalMilestones: List<GoalMilestoneEntity>,
            val goalProgressLogs: List<GoalProgressLogEntity>,
            val taskRecurrences: List<TaskRecurrenceEntity>,
            val recurrenceExceptions: List<RecurrenceExceptionEntity>,
            val scheduleChecklistItems: List<ScheduleChecklistItemEntity>
        )

        private fun snapshot() = Snapshot(
            settings = settings.toList(),
            categories = categories.toList(),
            projects = projects.toList(),
            events = events.toList(),
            tasks = tasks.toList(),
            subtasks = subtasks.toList(),
            appointments = appointments.toList(),
            creditCards = creditCards.toList(),
            creditCardStatements = creditCardStatements.toList(),
            installmentPurchases = installmentPurchases.toList(),
            recurringRules = recurringRules.toList(),
            transactions = transactions.toList(),
            loans = loans.toList(),
            budgets = budgets.toList(),
            financialGoals = financialGoals.toList(),
            auditHistory = auditHistory.toList(),
            errorLogs = errorLogs.toList(),
            reminders = reminders.toList(),
            routineSettings = routineSettings.toList(),
            briefingHistory = briefingHistory.toList(),
            personalizationSettings = personalizationSettings.toList(),
            quotes = quotes.toList(),
            personalGoals = personalGoals.toList(),
            goalMilestones = goalMilestones.toList(),
            goalProgressLogs = goalProgressLogs.toList(),
            taskRecurrences = taskRecurrences.toList(),
            recurrenceExceptions = recurrenceExceptions.toList(),
            scheduleChecklistItems = scheduleChecklistItems.toList()
        )

        private fun restoreSnapshot(s: Snapshot) {
            settings.apply { clear(); addAll(s.settings) }
            categories.apply { clear(); addAll(s.categories) }
            projects.apply { clear(); addAll(s.projects) }
            events.apply { clear(); addAll(s.events) }
            tasks.apply { clear(); addAll(s.tasks) }
            subtasks.apply { clear(); addAll(s.subtasks) }
            appointments.apply { clear(); addAll(s.appointments) }
            creditCards.apply { clear(); addAll(s.creditCards) }
            creditCardStatements.apply { clear(); addAll(s.creditCardStatements) }
            installmentPurchases.apply { clear(); addAll(s.installmentPurchases) }
            recurringRules.apply { clear(); addAll(s.recurringRules) }
            transactions.apply { clear(); addAll(s.transactions) }
            loans.apply { clear(); addAll(s.loans) }
            budgets.apply { clear(); addAll(s.budgets) }
            financialGoals.apply { clear(); addAll(s.financialGoals) }
            auditHistory.apply { clear(); addAll(s.auditHistory) }
            errorLogs.apply { clear(); addAll(s.errorLogs) }
            reminders.apply { clear(); addAll(s.reminders) }
            routineSettings.apply { clear(); addAll(s.routineSettings) }
            briefingHistory.apply { clear(); addAll(s.briefingHistory) }
            personalizationSettings.apply { clear(); addAll(s.personalizationSettings) }
            quotes.apply { clear(); addAll(s.quotes) }
            personalGoals.apply { clear(); addAll(s.personalGoals) }
            goalMilestones.apply { clear(); addAll(s.goalMilestones) }
            goalProgressLogs.apply { clear(); addAll(s.goalProgressLogs) }
            taskRecurrences.apply { clear(); addAll(s.taskRecurrences) }
            recurrenceExceptions.apply { clear(); addAll(s.recurrenceExceptions) }
            scheduleChecklistItems.apply { clear(); addAll(s.scheduleChecklistItems) }
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

        private fun <T> upsertById(target: MutableList<T>, items: List<T>, idOf: (T) -> String) {
            items.forEach { item ->
                val id = idOf(item)
                val idx = target.indexOfFirst { idOf(it) == id }
                if (idx >= 0) target[idx] = item else target.add(item)
            }
        }

        private fun checkFail(table: String) {
            if (failOnTableInsert == table) {
                throw IllegalStateException("Simulated SQLite failure inserting into $table")
            }
        }

        private fun checkFk(fkId: String?, parentIds: Set<String>, message: String) {
            if (fkId != null && fkId !in parentIds) {
                throw IllegalStateException("FOREIGN KEY constraint failed: $message ($fkId)")
            }
        }

        override suspend fun getSettings() = settings.toList()
        override suspend fun getCategories() = categories.toList()
        override suspend fun getProjects() = projects.toList()
        override suspend fun getEvents() = events.toList()
        override suspend fun getTasks() = tasks.toList()
        override suspend fun getSubtasks() = subtasks.toList()
        override suspend fun getAppointments() = appointments.toList()
        override suspend fun getCreditCards() = creditCards.toList()
        override suspend fun getCreditCardStatements() = creditCardStatements.toList()
        override suspend fun getInstallmentPurchases() = installmentPurchases.toList()
        override suspend fun getRecurringRules() = recurringRules.toList()
        override suspend fun getTransactions() = transactions.toList()
        override suspend fun getLoans() = loans.toList()
        override suspend fun getBudgets() = budgets.toList()
        override suspend fun getFinancialGoals() = financialGoals.toList()
        override suspend fun getAuditHistory() = auditHistory.toList()
        override suspend fun getErrorLogs() = errorLogs.toList()
        override suspend fun getReminders() = reminders.toList()
        override suspend fun getRoutineSettings() = routineSettings.toList()
        override suspend fun getBriefingHistory() = briefingHistory.toList()
        override suspend fun getPersonalizationSettings() = personalizationSettings.toList()
        override suspend fun getQuotes() = quotes.toList()
        override suspend fun getPersonalGoals() = personalGoals.toList()
        override suspend fun getGoalMilestones() = goalMilestones.toList()
        override suspend fun getGoalProgressLogs() = goalProgressLogs.toList()
        override suspend fun getTaskRecurrences() = taskRecurrences.toList()
        override suspend fun getRecurrenceExceptions() = recurrenceExceptions.toList()
        override suspend fun getScheduleChecklistItems() = scheduleChecklistItems.toList()

        override suspend fun insertSettings(items: List<SettingsEntity>) {
            checkFail("settings")
            upsertById(settings, items) { it.id }
        }

        override suspend fun insertCategories(items: List<CategoryEntity>) {
            checkFail("categories")
            upsertById(categories, items) { it.id }
        }

        override suspend fun insertProjects(items: List<ProjectEntity>) {
            checkFail("projects")
            val goalIds = personalGoals.map { it.id }.toSet()
            items.forEach { checkFk(it.goalId, goalIds, "projects.goalId") }
            upsertById(projects, items) { it.id }
        }

        override suspend fun insertEvents(items: List<EventEntity>) {
            checkFail("events")
            val catIds = categories.map { it.id }.toSet()
            val cardIds = creditCards.map { it.id }.toSet()
            val recIds = taskRecurrences.map { it.id }.toSet()
            items.forEach {
                checkFk(it.categoryId, catIds, "events.categoryId")
                checkFk(it.creditCardId, cardIds, "events.creditCardId")
                checkFk(it.recurrenceId, recIds, "events.recurrenceId")
            }
            upsertById(events, items) { it.id }
            val uniqueRecOcc = events.filter { it.recurrenceId != null && it.occurrenceDateIso != null }
                .map { it.recurrenceId!! to it.occurrenceDateIso!! }
            if (uniqueRecOcc.size != uniqueRecOcc.toSet().size) {
                throw IllegalStateException("UNIQUE constraint failed: events(recurrenceId, occurrenceDateIso)")
            }
        }

        override suspend fun insertTasks(items: List<TaskEntity>) {
            checkFail("tasks")
            val projIds = projects.map { it.id }.toSet()
            val catIds = categories.map { it.id }.toSet()
            val evIds = events.map { it.id }.toSet()
            val goalIds = personalGoals.map { it.id }.toSet()
            val recIds = taskRecurrences.map { it.id }.toSet()
            items.forEach {
                checkFk(it.projectId, projIds, "tasks.projectId")
                checkFk(it.categoryId, catIds, "tasks.categoryId")
                checkFk(it.eventId, evIds, "tasks.eventId")
                checkFk(it.goalId, goalIds, "tasks.goalId")
                checkFk(it.recurrenceId, recIds, "tasks.recurrenceId")
            }
            upsertById(tasks, items) { it.id }
            val uniqueRecOcc = tasks.filter { it.recurrenceId != null && it.occurrenceDateIso != null }
                .map { it.recurrenceId!! to it.occurrenceDateIso!! }
            if (uniqueRecOcc.size != uniqueRecOcc.toSet().size) {
                throw IllegalStateException("UNIQUE constraint failed: tasks(recurrenceId, occurrenceDateIso)")
            }
        }

        override suspend fun insertSubtasks(items: List<SubtaskEntity>) {
            checkFail("subtasks")
            val taskIds = tasks.map { it.id }.toSet()
            items.forEach { checkFk(it.taskId, taskIds, "subtasks.taskId") }
            upsertById(subtasks, items) { it.id }
        }

        override suspend fun insertAppointments(items: List<AppointmentEntity>) {
            checkFail("appointments")
            val catIds = categories.map { it.id }.toSet()
            val evIds = events.map { it.id }.toSet()
            val recIds = taskRecurrences.map { it.id }.toSet()
            items.forEach {
                checkFk(it.categoryId, catIds, "appointments.categoryId")
                checkFk(it.eventId, evIds, "appointments.eventId")
                checkFk(it.recurrenceId, recIds, "appointments.recurrenceId")
            }
            upsertById(appointments, items) { it.id }
            val uniqueRecOcc = appointments.filter { it.recurrenceId != null && it.occurrenceDateIso != null }
                .map { it.recurrenceId!! to it.occurrenceDateIso!! }
            if (uniqueRecOcc.size != uniqueRecOcc.toSet().size) {
                throw IllegalStateException("UNIQUE constraint failed: appointments(recurrenceId, occurrenceDateIso)")
            }
        }

        override suspend fun insertCreditCards(items: List<CreditCardEntity>) {
            checkFail("credit_cards")
            upsertById(creditCards, items) { it.id }
        }

        override suspend fun insertCreditCardStatements(items: List<CreditCardStatementEntity>) {
            checkFail("credit_card_statements")
            val cardIds = creditCards.map { it.id }.toSet()
            items.forEach { checkFk(it.creditCardId, cardIds, "statements.creditCardId") }
            upsertById(creditCardStatements, items) { it.id }
            val keys = creditCardStatements.map { it.creditCardId to it.yearMonth }
            if (keys.size != keys.toSet().size) {
                throw IllegalStateException("UNIQUE constraint failed: credit_card_statements(creditCardId, yearMonth)")
            }
        }

        override suspend fun insertInstallmentPurchases(items: List<InstallmentPurchaseEntity>) {
            checkFail("installment_purchases")
            val cardIds = creditCards.map { it.id }.toSet()
            val catIds = categories.map { it.id }.toSet()
            val evIds = events.map { it.id }.toSet()
            val projIds = projects.map { it.id }.toSet()
            items.forEach {
                checkFk(it.creditCardId, cardIds, "purchases.creditCardId")
                checkFk(it.categoryId, catIds, "purchases.categoryId")
                checkFk(it.eventId, evIds, "purchases.eventId")
                checkFk(it.projectId, projIds, "purchases.projectId")
            }
            upsertById(installmentPurchases, items) { it.id }
        }

        override suspend fun insertRecurringRules(items: List<RecurringRuleEntity>) {
            checkFail("recurring_rules")
            val catIds = categories.map { it.id }.toSet()
            val cardIds = creditCards.map { it.id }.toSet()
            items.forEach {
                checkFk(it.categoryId, catIds, "recurring_rules.categoryId")
                checkFk(it.creditCardId, cardIds, "recurring_rules.creditCardId")
            }
            upsertById(recurringRules, items) { it.id }
        }

        override suspend fun insertTransactions(items: List<TransactionEntity>) {
            checkFail("transactions")
            val catIds = categories.map { it.id }.toSet()
            val projIds = projects.map { it.id }.toSet()
            val evIds = events.map { it.id }.toSet()
            val cardIds = creditCards.map { it.id }.toSet()
            val stmtIds = creditCardStatements.map { it.id }.toSet()
            val purchIds = installmentPurchases.map { it.id }.toSet()
            val ruleIds = recurringRules.map { it.id }.toSet()
            val taskIds = tasks.map { it.id }.toSet()
            items.forEach {
                checkFk(it.categoryId, catIds, "tx.categoryId")
                checkFk(it.projectId, projIds, "tx.projectId")
                checkFk(it.eventId, evIds, "tx.eventId")
                checkFk(it.creditCardId, cardIds, "tx.creditCardId")
                checkFk(it.statementId, stmtIds, "tx.statementId")
                checkFk(it.installmentPurchaseId, purchIds, "tx.installmentPurchaseId")
                checkFk(it.recurringRuleId, ruleIds, "tx.recurringRuleId")
                checkFk(it.taskId, taskIds, "tx.taskId")
            }
            upsertById(transactions, items) { it.id }
            val uniqueRuleOcc = transactions.filter { it.recurringRuleId != null && it.occurrenceIndex != null }
                .map { it.recurringRuleId!! to it.occurrenceIndex!! }
            if (uniqueRuleOcc.size != uniqueRuleOcc.toSet().size) {
                throw IllegalStateException("UNIQUE constraint failed: transactions(recurringRuleId, occurrenceIndex)")
            }
        }

        override suspend fun insertLoans(items: List<LoanEntity>) {
            checkFail("loans")
            upsertById(loans, items) { it.id }
        }

        override suspend fun insertBudgets(items: List<BudgetEntity>) {
            checkFail("budgets")
            val catIds = categories.map { it.id }.toSet()
            items.forEach { checkFk(it.categoryId, catIds, "budgets.categoryId") }
            upsertById(budgets, items) { it.id }
            val keys = budgets.map { it.categoryId to it.yearMonth }
            if (keys.size != keys.toSet().size) {
                throw IllegalStateException("UNIQUE constraint failed: budgets(categoryId, yearMonth)")
            }
        }

        override suspend fun insertFinancialGoals(items: List<FinancialGoalEntity>) {
            checkFail("financial_goals")
            val catIds = categories.map { it.id }.toSet()
            items.forEach { checkFk(it.categoryId, catIds, "financial_goals.categoryId") }
            upsertById(financialGoals, items) { it.id }
        }

        override suspend fun insertAuditHistory(items: List<AuditHistoryEntity>) {
            checkFail("audit_history")
            upsertById(auditHistory, items) { it.id }
        }

        override suspend fun insertErrorLogs(items: List<ErrorLogEntity>) {
            checkFail("error_logs")
            upsertById(errorLogs, items) { it.id }
        }

        override suspend fun insertReminders(items: List<ReminderEntity>) {
            checkFail("reminders")
            upsertById(reminders, items) { it.id }
            val keys = reminders.map { Triple(it.sourceType, it.sourceId, it.category) }
            if (keys.size != keys.toSet().size) {
                throw IllegalStateException("UNIQUE constraint failed: reminders(sourceType, sourceId, category)")
            }
        }

        override suspend fun insertRoutineSettings(items: List<RoutineAssistantSettingsEntity>) {
            checkFail("routine_assistant_settings")
            upsertById(routineSettings, items) { it.id }
        }

        override suspend fun insertBriefingHistory(items: List<BriefingHistoryEntity>) {
            checkFail("briefing_history")
            upsertById(briefingHistory, items) { it.id }
            val keys = briefingHistory.map { it.briefingType to it.dateIso }
            if (keys.size != keys.toSet().size) {
                throw IllegalStateException("UNIQUE constraint failed: briefing_history(briefingType, dateIso)")
            }
        }

        override suspend fun insertPersonalizationSettings(items: List<PersonalizationSettingsEntity>) {
            checkFail("personalization_settings")
            upsertById(personalizationSettings, items) { it.id }
        }

        override suspend fun insertQuotes(items: List<QuoteEntity>) {
            checkFail("quotes")
            upsertById(quotes, items) { it.id }
        }

        override suspend fun insertPersonalGoals(items: List<PersonalGoalEntity>) {
            checkFail("personal_goals")
            val catIds = categories.map { it.id }.toSet()
            items.forEach { checkFk(it.categoryId, catIds, "personal_goals.categoryId") }
            upsertById(personalGoals, items) { it.id }
        }

        override suspend fun insertGoalMilestones(items: List<GoalMilestoneEntity>) {
            checkFail("goal_milestones")
            val goalIds = personalGoals.map { it.id }.toSet()
            items.forEach { checkFk(it.goalId, goalIds, "goal_milestones.goalId") }
            upsertById(goalMilestones, items) { it.id }
        }

        override suspend fun insertGoalProgressLogs(items: List<GoalProgressLogEntity>) {
            checkFail("goal_progress_logs")
            val goalIds = personalGoals.map { it.id }.toSet()
            items.forEach { checkFk(it.goalId, goalIds, "goal_progress_logs.goalId") }
            upsertById(goalProgressLogs, items) { it.id }
        }

        override suspend fun insertTaskRecurrences(items: List<TaskRecurrenceEntity>) {
            checkFail("recurrences")
            val projIds = projects.map { it.id }.toSet()
            val catIds = categories.map { it.id }.toSet()
            val evIds = events.map { it.id }.toSet()
            val goalIds = personalGoals.map { it.id }.toSet()
            items.forEach {
                checkFk(it.projectId, projIds, "recurrences.projectId")
                checkFk(it.categoryId, catIds, "recurrences.categoryId")
                checkFk(it.eventId, evIds, "recurrences.eventId")
                checkFk(it.goalId, goalIds, "recurrences.goalId")
            }
            upsertById(taskRecurrences, items) { it.id }
        }

        override suspend fun insertRecurrenceExceptions(items: List<RecurrenceExceptionEntity>) {
            checkFail("recurrence_exceptions")
            val recIds = taskRecurrences.map { it.id }.toSet()
            items.forEach { checkFk(it.recurrenceId, recIds, "recurrence_exceptions.recurrenceId") }
            upsertById(recurrenceExceptions, items) { it.id }
            val keys = recurrenceExceptions.map { it.recurrenceId to it.occurrenceDateIso }
            if (keys.size != keys.toSet().size) {
                throw IllegalStateException("UNIQUE constraint failed: recurrence_exceptions(recurrenceId, occurrenceDateIso)")
            }
        }

        override suspend fun insertScheduleChecklistItems(items: List<ScheduleChecklistItemEntity>) {
            checkFail("schedule_checklist_items")
            val apptIds = appointments.map { it.id }.toSet()
            val eventIds = events.map { it.id }.toSet()
            items.forEach { item ->
                checkFk(item.appointmentId, apptIds, "schedule_checklist_items.appointmentId")
                checkFk(item.eventId, eventIds, "schedule_checklist_items.eventId")
            }
            upsertById(scheduleChecklistItems, items) { it.id }
        }

        override suspend fun clearAllForFullRestore() {
            clearAllCallsCount++
            goalProgressLogs.clear()
            goalMilestones.clear()
            reminders.clear()
            briefingHistory.clear()
            routineSettings.clear()
            personalizationSettings.clear()
            quotes.clear()
            transactions.clear()
            scheduleChecklistItems.clear()
            subtasks.clear()
            recurrenceExceptions.clear()
            tasks.clear()
            taskRecurrences.clear()
            appointments.clear()
            installmentPurchases.clear()
            recurringRules.clear()
            creditCardStatements.clear()
            budgets.clear()
            financialGoals.apply { clear() }
            loans.clear()
            events.clear()
            creditCards.clear()
            projects.clear()
            personalGoals.clear()
            categories.clear()
            settings.clear()
            auditHistory.clear()
            errorLogs.clear()
        }
    }

    private class InMemoryWatchlistStore(
        initial: List<String> = emptyList(),
        var failOnSave: Boolean = false
    ) : MarketWatchlistStore {
        var saved: List<String> = initial
        override fun loadFavoriteCodes(): List<String> = saved
        override fun saveFavoriteCodes(codes: List<String>) {
            if (failOnSave) throw IllegalStateException("Watchlist write failed")
            saved = codes
        }
    }

    private class InMemoryAlertStore(
        initial: List<MarketPriceAlert> = emptyList(),
        var failOnSave: Boolean = false
    ) : MarketAlertStore {
        var saved: List<MarketPriceAlert> = initial
        override fun loadAlerts(): List<MarketPriceAlert> = saved
        override fun saveAlerts(alerts: List<MarketPriceAlert>) {
            if (failOnSave) throw IllegalStateException("Alert write failed")
            saved = alerts
        }
    }

    private val t0 = 1_795_000_000_000L

    private fun seedLocalDb(storage: InMemoryBackupStorageGateway) {
        storage.settings.add(
            SettingsEntity(
                id = SettingsEntity.SINGLETON_ID,
                userName = "Enzo Local",
                darkTheme = true,
                declaredBalanceCents = 250_000L,
                createdAt = t0,
                updatedAt = t0
            )
        )
        storage.routineSettings.add(
            RoutineAssistantSettingsEntity(
                morningBriefingHour = 7,
                morningBriefingMinute = 30,
                createdAt = t0,
                updatedAt = t0
            )
        )
        storage.personalizationSettings.add(
            PersonalizationSettingsEntity(
                greetingTemplate = "Foco total, Enzo",
                compactDashboardMode = true,
                createdAt = t0,
                updatedAt = t0
            )
        )
        storage.categories.add(
            CategoryEntity(
                id = "cat-local-1",
                name = "Moradia Local",
                colorHex = "#4C8BF5",
                scope = CategoryScope.GENERAL,
                createdAt = t0,
                updatedAt = t0
            )
        )
        storage.tasks.add(
            TaskEntity(
                id = "task-local-only",
                title = "Tarefa Exclusiva Local",
                status = TaskStatus.IN_PROGRESS,
                priority = TaskPriority.HIGH,
                createdAt = t0,
                updatedAt = t0
            )
        )
        storage.tasks.add(
            TaskEntity(
                id = "task-shared-conflict",
                title = "Título Modificado Localmente",
                status = TaskStatus.COMPLETED,
                priority = TaskPriority.URGENT,
                createdAt = t0,
                updatedAt = t0 + 10_000L
            )
        )
    }

    // Scenarios 1 & 17: Full restore still clears and replaces local data cleanly
    @Test
    fun `full restore mode clears local database and replaces with backup payload`() = runBlocking {
        val storage = InMemoryBackupStorageGateway()
        seedLocalDb(storage)
        val manager = BackupManager(storage)

        val payload = NexusBackupPayload(
            formatVersion = 7,
            databaseVersion = 6,
            settings = listOf(SettingsBackupDto(userName = "Novo Usuario", darkTheme = false, declaredBalanceCents = 10_000L)),
            categories = listOf(CategoryBackupDto(id = "cat-restored", name = "Nova Cat", colorHex = "#34C759")),
            tasks = listOf(TaskBackupDto(id = "task-restored", title = "Tarefa Restaurada"))
        )

        val summary = manager.restoreAtomically(payload)
        assertEquals(1, storage.clearAllCallsCount)
        assertEquals(1, summary.tasksRestored)
        assertEquals("Novo Usuario", storage.settings.single().userName)
        assertEquals(listOf("task-restored"), storage.tasks.map { it.id })
        assertTrue(storage.tasks.none { it.id == "task-local-only" })
    }

    // Scenario 2 & 18: Merge 1 new record cleanly without ever calling clearAll
    @Test
    fun `merge single new record preserves all local data and never calls deleteAll`() = runBlocking {
        val storage = InMemoryBackupStorageGateway()
        seedLocalDb(storage)
        val manager = BackupManager(storage)

        val payload = NexusBackupPayload(
            formatVersion = 7,
            databaseVersion = 6,
            tasks = listOf(TaskBackupDto(id = "task-new-1", title = "Nova Tarefa do Backup"))
        )

        val summary = manager.mergeBackupAtomically(payload)
        assertEquals(0, storage.clearAllCallsCount)
        assertEquals(1, summary.totalFoundInFile)
        assertEquals(1, summary.totalNewRecords)
        assertEquals(1, summary.totalInserted)
        assertEquals(0, summary.totalConflicts)
        assertEquals(3, storage.tasks.size)
        assertTrue(storage.tasks.any { it.id == "task-new-1" })
        assertTrue(storage.tasks.any { it.id == "task-local-only" })
    }

    // Scenarios 3 & 8: Merge multiple related records in dependency order preserving relationships
    @Test
    fun `merge multiple related entities preserves parent-child relationships in topological order`() = runBlocking {
        val storage = InMemoryBackupStorageGateway()
        seedLocalDb(storage)
        val manager = BackupManager(storage)

        val payload = NexusBackupPayload(
            formatVersion = 7,
            databaseVersion = 6,
            categories = listOf(CategoryBackupDto(id = "cat-estudos", name = "Estudos", colorHex = "#4C8BF5")),
            personalGoals = listOf(PersonalGoalBackupDto(id = "goal-1", title = "Formar em 2027", categoryId = "cat-estudos")),
            goalMilestones = listOf(GoalMilestoneBackupDto(id = "ms-1", goalId = "goal-1", title = "TCC Fase 1")),
            projects = listOf(ProjectBackupDto(id = "proj-1", name = "Projeto TCC", goalId = "goal-1")),
            recurrences = listOf(TaskRecurrenceBackupDto(id = "rec-1", title = "Revisar artigos", projectId = "proj-1", startDateIso = "2026-11-25")),
            tasks = listOf(
                TaskBackupDto(
                    id = "task-rel-1",
                    title = "Escrever capítulo 1",
                    projectId = "proj-1",
                    categoryId = "cat-estudos",
                    goalId = "goal-1",
                    recurrenceId = "rec-1",
                    occurrenceDateIso = "2026-11-25"
                )
            ),
            subtasks = listOf(SubtaskBackupDto(id = "sub-1", taskId = "task-rel-1", title = "Revisar bibliografia")),
            recurrenceExceptions = listOf(RecurrenceExceptionBackupDto(id = "exc-1", recurrenceId = "rec-1", occurrenceDateIso = "2026-12-02", exceptionType = "SKIP")),
            appointments = listOf(AppointmentBackupDto(id = "appt-1", title = "Orientação", categoryId = "cat-estudos", startAt = t0, endAt = t0 + 3_600_000L)),
            reminders = listOf(ReminderBackupDto(id = "rem-1", sourceType = "TASK", sourceId = "task-rel-1", title = "Lembrete TCC", triggerAt = t0 + 1_000L))
        )

        val summary = manager.mergeBackupAtomically(payload)
        assertEquals(0, storage.clearAllCallsCount)
        assertEquals(0, summary.totalInvalid)
        assertEquals(0, summary.totalConflicts)
        assertEquals(10, summary.totalInserted)
        assertEquals("goal-1", storage.projects.single().goalId)
        assertEquals("proj-1", storage.tasks.first { it.id == "task-rel-1" }.projectId)
        assertEquals("task-rel-1", storage.subtasks.single().taskId)
        assertEquals("task-rel-1", storage.reminders.single().sourceId)
    }

    // Scenarios 4 & 19: Re-importing the exact same backup is idempotent with zero duplicates
    @Test
    fun `re-importing the same backup is idempotent and creates no duplicates`() = runBlocking {
        val storage = InMemoryBackupStorageGateway()
        seedLocalDb(storage)
        val manager = BackupManager(storage)

        val payload = NexusBackupPayload(
            formatVersion = 7,
            databaseVersion = 6,
            categories = listOf(CategoryBackupDto(id = "cat-new", name = "Saúde", colorHex = "#34C759")),
            tasks = listOf(TaskBackupDto(id = "task-new-a", title = "Consulta", categoryId = "cat-new")),
            loans = listOf(LoanBackupDtoFixture())
        )

        val first = manager.mergeBackupAtomically(payload)
        assertEquals(3, first.totalInserted)
        val snapshotCountsAfterFirst = Triple(storage.categories.size, storage.tasks.size, storage.loans.size)

        val second = manager.mergeBackupAtomically(payload)
        assertEquals(0, storage.clearAllCallsCount)
        assertEquals(0, second.totalNewRecords)
        assertEquals(0, second.totalInserted)
        assertEquals(3, second.totalAlreadyExisting)
        assertEquals(0, second.totalConflicts)
        assertEquals(snapshotCountsAfterFirst, Triple(storage.categories.size, storage.tasks.size, storage.loans.size))
    }

    // Scenarios 5, 6, 7 & 20: Preserve locally modified records, count conflicts, keep local-only, accurate counters
    @Test
    fun `conflicting IDs preserve local edits increment conflict counter and keep local-only records`() = runBlocking {
        val storage = InMemoryBackupStorageGateway()
        seedLocalDb(storage)
        val manager = BackupManager(storage)

        val payload = NexusBackupPayload(
            formatVersion = 7,
            databaseVersion = 6,
            categories = listOf(
                CategoryBackupDto(id = "cat-local-1", name = "Moradia Local", colorHex = "#4C8BF5"), // identical
                CategoryBackupDto(id = "cat-extra", name = "Lazer", colorHex = "#E5A000") // new
            ),
            tasks = listOf(
                TaskBackupDto(
                    id = "task-shared-conflict",
                    title = "Título Antigo do Outro Aparelho",
                    status = TaskStatus.NOT_STARTED.name,
                    priority = TaskPriority.LOW.name
                ),
                TaskBackupDto(id = "task-brand-new", title = "Nova Tarefa", priority = TaskPriority.NORMAL.name)
            )
        )

        val summary = manager.mergeBackupAtomically(payload)
        // Local conflict preserved intact
        val preserved = storage.tasks.first { it.id == "task-shared-conflict" }
        assertEquals("Título Modificado Localmente", preserved.title)
        assertEquals(TaskStatus.COMPLETED, preserved.status)
        assertEquals(TaskPriority.URGENT, preserved.priority)
        // Local-only preserved
        assertTrue(storage.tasks.any { it.id == "task-local-only" })

        assertEquals(4, summary.totalFoundInFile)
        assertEquals(2, summary.totalNewRecords)
        assertEquals(2, summary.totalInserted)
        assertEquals(1, summary.totalAlreadyExisting)
        assertEquals(1, summary.totalConflicts)
        assertEquals(0, summary.totalInvalid)
        // task-local-only wasn't touched in tasks table => preservedLocalOnly >= 1
        assertTrue(summary.totalPreservedLocalOnly >= 1)
        val taskStats = summary.perEntity.first { it.tableName == "tasks" }
        assertEquals(2, taskStats.foundInFile)
        assertEquals(1, taskStats.inserted)
        assertEquals(1, taskStats.conflicts)
        assertEquals(1, taskStats.preservedLocalOnly)
    }

    // Scenario 9: Invalid / orphan foreign keys & invalid records handled safely without corrupting DB
    @Test
    fun `orphan strict FKs are rejected and broken optional FKs are sanitized with needsReview`() = runBlocking {
        val storage = InMemoryBackupStorageGateway()
        seedLocalDb(storage)
        val manager = BackupManager(storage)

        val payload = NexusBackupPayload(
            formatVersion = 7,
            databaseVersion = 6,
            tasks = listOf(
                // Blank ID -> rejected
                TaskBackupDto(id = " ", title = "Sem ID"),
                // Missing optional project/category -> inserted with nullified FK + needsReview
                TaskBackupDto(
                    id = "task-orphan-optional",
                    title = "Tarefa com projeto ausente",
                    projectId = "proj-missing",
                    categoryId = "cat-missing"
                )
            ),
            subtasks = listOf(
                // Strict FK missing -> rejected without failing transaction
                SubtaskBackupDto(id = "sub-orphan", taskId = "task-missing", title = "Subtarefa órfã")
            ),
            creditCardStatements = listOf(
                // Strict FK missing -> rejected
                CreditCardStatementBackupDto(id = "stmt-orphan", creditCardId = "card-missing", yearMonth = "2026-11")
            ),
            budgets = listOf(
                // Strict FK missing -> rejected
                BudgetBackupDto(id = "bud-orphan", categoryId = "cat-missing", yearMonth = "2026-11", limitCents = 1000L)
            ),
            reminders = listOf(
                // Orphan reminder -> rejected
                ReminderBackupDto(id = "rem-orphan", sourceType = "TASK", sourceId = "task-missing", title = "Órfão", triggerAt = t0)
            )
        )

        val summary = manager.mergeBackupAtomically(payload)
        assertEquals(0, storage.clearAllCallsCount)
        assertEquals(1, summary.totalInserted)
        assertEquals(5, summary.totalInvalid)
        assertEquals(6, summary.totalNeedsReview) // 5 invalid + 1 sanitized optional FK

        val insertedTask = storage.tasks.first { it.id == "task-orphan-optional" }
        assertNull(insertedTask.projectId)
        assertNull(insertedTask.categoryId)
        assertTrue(storage.subtasks.isEmpty())
        assertTrue(storage.creditCardStatements.isEmpty())
        assertTrue(storage.budgets.isEmpty())
        assertTrue(storage.reminders.isEmpty())
        assertTrue(summary.issues.any { it.kind == MergeIssueKind.REFERENCE_SANITIZED_NEEDS_REVIEW })
        assertTrue(summary.issues.any { it.kind == MergeIssueKind.INVALID_REJECTED })
    }

    // Scenario 10: Singleton settings are never overwritten silently
    @Test
    fun `singleton settings are preserved locally and counted as skipped conflicts`() = runBlocking {
        val storage = InMemoryBackupStorageGateway()
        seedLocalDb(storage)
        val manager = BackupManager(storage)

        val payload = NexusBackupPayload(
            formatVersion = 7,
            databaseVersion = 6,
            settings = listOf(SettingsBackupDto(userName = "Outro Aparelho", darkTheme = false, declaredBalanceCents = 999L)),
            routineSettings = listOf(RoutineAssistantSettingsBackupDto(morningBriefingHour = 10, notificationsEnabled = false)),
            personalizationSettings = listOf(PersonalizationSettingsBackupDto(greetingTemplate = "Outro", compactDashboardMode = false))
        )

        val summary = manager.mergeBackupAtomically(payload)
        assertEquals(3, summary.totalSingletonSkipped)
        assertEquals(3, summary.totalConflicts)
        assertEquals("Enzo Local", storage.settings.single().userName)
        assertEquals(true, storage.settings.single().darkTheme)
        assertEquals(250_000L, storage.settings.single().declaredBalanceCents)
        assertEquals(7, storage.routineSettings.single().morningBriefingHour)
        assertEquals("Foco total, Enzo", storage.personalizationSettings.single().greetingTemplate)
    }

    // Scenarios 11 & 12: Financial integrity for cards, statements, installments, recurrences & no double expense
    @Test
    fun `financial merge enforces card statement coherence blocks double expenses and deduplicates installments and recurrences`() = runBlocking {
        val storage = InMemoryBackupStorageGateway()
        seedLocalDb(storage)
        storage.creditCards.add(
            CreditCardEntity(id = "card-nubank", name = "Nubank", totalLimitCents = 500_000L, availableLimitCents = 300_000L, createdAt = t0, updatedAt = t0)
        )
        storage.creditCardStatements.add(
            CreditCardStatementEntity(
                id = "stmt-paid-local",
                creditCardId = "card-nubank",
                yearMonth = "2026-10",
                closingAt = t0,
                dueAt = t0 + 7 * 86_400_000L,
                status = StatementStatus.PAID,
                paidAt = t0,
                paidAmountCents = 45_000L,
                createdAt = t0,
                updatedAt = t0
            )
        )
        storage.installmentPurchases.add(
            InstallmentPurchaseEntity(
                id = "purch-1",
                description = "Monitor",
                totalAmountCents = 100_000L,
                installmentsCount = 2,
                installmentAmountCents = 50_000L,
                firstDueAt = t0,
                creditCardId = "card-nubank",
                createdAt = t0,
                updatedAt = t0
            )
        )
        storage.recurringRules.add(
            RecurringRuleEntity(
                id = "rule-1",
                title = "Internet",
                type = TransactionType.EXPENSE,
                amountCents = 12_000L,
                frequency = RecurrenceFrequency.MONTHLY,
                startAt = t0,
                createdAt = t0,
                updatedAt = t0
            )
        )
        storage.transactions.add(
            TransactionEntity(
                id = "tx-inst-1-local",
                type = TransactionType.EXPENSE,
                description = "Monitor 1/2",
                amountCents = 50_000L,
                status = FinancialStatus.SETTLED,
                paymentMethod = PaymentMethod.CREDIT_CARD,
                occurredAt = t0,
                creditCardId = "card-nubank",
                statementId = "stmt-paid-local",
                installmentPurchaseId = "purch-1",
                installmentNumber = 1,
                installmentsTotal = 2,
                createdAt = t0,
                updatedAt = t0
            )
        )
        storage.transactions.add(
            TransactionEntity(
                id = "tx-rule-occ-1",
                type = TransactionType.EXPENSE,
                description = "Internet Out",
                amountCents = 12_000L,
                occurredAt = t0,
                recurringRuleId = "rule-1",
                occurrenceIndex = 1,
                createdAt = t0,
                updatedAt = t0
            )
        )

        val manager = BackupManager(storage)
        val payload = NexusBackupPayload(
            formatVersion = 7,
            databaseVersion = 6,
            creditCards = listOf(
                CreditCardBackupDto(id = "card-inter", name = "Inter", totalLimitCents = 200_000L, availableLimitCents = 200_000L)
            ),
            creditCardStatements = listOf(
                // Same (card-nubank, 2026-10) under different ID -> conflict, local PAID kept
                CreditCardStatementBackupDto(id = "stmt-paid-other-id", creditCardId = "card-nubank", yearMonth = "2026-10", status = "OPEN"),
                // Valid new statement
                CreditCardStatementBackupDto(id = "stmt-open-nov", creditCardId = "card-nubank", yearMonth = "2026-11", status = "OPEN")
            ),
            transactions = listOf(
                // Standalone statement payment expense -> rejected to prevent double expense
                TransactionBackupDto(
                    id = "tx-double-stmt-pay",
                    type = "EXPENSE",
                    description = "Pagamento de fatura Nubank",
                    amountCents = 45_000L,
                    paymentMethod = "PIX",
                    creditCardId = "card-nubank"
                ),
                // Pending expense pointing to locally PAID statement -> blocked
                TransactionBackupDto(
                    id = "tx-into-paid-stmt",
                    type = "EXPENSE",
                    description = "Compra atrasada",
                    amountCents = 5_000L,
                    status = "PENDING",
                    paymentMethod = "CREDIT_CARD",
                    creditCardId = "card-nubank",
                    statementId = "stmt-paid-local"
                ),
                // Card mismatch with statement -> invalid
                TransactionBackupDto(
                    id = "tx-card-mismatch",
                    type = "EXPENSE",
                    description = "Erro cartao",
                    amountCents = 3_000L,
                    paymentMethod = "CREDIT_CARD",
                    creditCardId = "card-inter",
                    statementId = "stmt-open-nov"
                ),
                // Same installment 1/2 under different ID -> blocked
                TransactionBackupDto(
                    id = "tx-inst-1-dup",
                    type = "EXPENSE",
                    description = "Monitor 1/2 duplicada",
                    amountCents = 50_000L,
                    paymentMethod = "CREDIT_CARD",
                    creditCardId = "card-nubank",
                    statementId = "stmt-open-nov",
                    installmentPurchaseId = "purch-1",
                    installmentNumber = 1,
                    installmentsTotal = 2
                ),
                // Same recurring rule occurrenceIndex=1 under different ID -> blocked
                TransactionBackupDto(
                    id = "tx-rule-occ-1-dup",
                    type = "EXPENSE",
                    description = "Internet Out duplicada",
                    amountCents = 12_000L,
                    recurringRuleId = "rule-1",
                    occurrenceIndex = 1
                ),
                // Valid new installment 2/2 on open statement -> inserted
                TransactionBackupDto(
                    id = "tx-inst-2-new",
                    type = "EXPENSE",
                    description = "Monitor 2/2",
                    amountCents = 50_000L,
                    status = "PENDING",
                    paymentMethod = "CREDIT_CARD",
                    creditCardId = "card-nubank",
                    statementId = "stmt-open-nov",
                    installmentPurchaseId = "purch-1",
                    installmentNumber = 2,
                    installmentsTotal = 2
                )
            )
        )

        val summary = manager.mergeBackupAtomically(payload)
        assertEquals(3, summary.totalInserted) // card-inter + stmt-open-nov + tx-inst-2-new
        assertTrue(storage.transactions.any { it.id == "tx-inst-2-new" })
        assertFalse(storage.transactions.any { it.id == "tx-double-stmt-pay" })
        assertFalse(storage.transactions.any { it.id == "tx-into-paid-stmt" })
        assertFalse(storage.transactions.any { it.id == "tx-card-mismatch" })
        assertFalse(storage.transactions.any { it.id == "tx-inst-1-dup" })
        assertFalse(storage.transactions.any { it.id == "tx-rule-occ-1-dup" })
        assertTrue(summary.issues.any { it.kind == MergeIssueKind.FINANCIAL_INTEGRITY_GUARD })
    }

    // Scenarios 13 & 14: Malformed JSON & incompatible backup versions rejected safely
    @Test
    fun `malformed JSON and incompatible backup format or db versions are rejected cleanly`() = runBlocking {
        val storage = InMemoryBackupStorageGateway()
        seedLocalDb(storage)
        val manager = BackupManager(storage)

        assertTrue(manager.validateAndParse("") is BackupValidationResult.Invalid)
        assertTrue(manager.validateAndParse("{broken-json") is BackupValidationResult.Invalid)
        assertTrue(manager.validateAndParse("""{"foo":"bar"}""") is BackupValidationResult.Invalid)

        val futureFormatPayload = NexusBackupPayload(formatVersion = 99, databaseVersion = 6)
        assertNotNull(manager.validateForMerge(futureFormatPayload))
        try {
            manager.mergeBackupAtomically(futureFormatPayload)
            fail("Expected IllegalArgumentException for formatVersion=99")
        } catch (_: IllegalArgumentException) {
            // expected
        }

        val futureDbPayload = NexusBackupPayload(formatVersion = 7, databaseVersion = 99)
        assertNotNull(manager.validateForMerge(futureDbPayload))
        try {
            manager.mergeBackupAtomically(futureDbPayload)
            fail("Expected IllegalArgumentException for databaseVersion=99")
        } catch (_: IllegalArgumentException) {
            // expected
        }

        // Local state completely untouched
        assertEquals(2, storage.tasks.size)
    }

    // Scenario 15: Transactional rollback on failure (both Room storage failure and Market write failure)
    @Test
    fun `atomic merge rolls back all Room and Market changes if any write fails`() = runBlocking {
        val storage = InMemoryBackupStorageGateway()
        seedLocalDb(storage)
        val watchlistStore = InMemoryWatchlistStore(listOf(MarketAssetCatalog.USD_BRL_CODE))
        val alertStore = InMemoryAlertStore(
            listOf(
                MarketPriceAlert(
                    id = "local-alert-1",
                    assetCode = MarketAssetCatalog.USD_BRL_CODE,
                    condition = MarketAlertCondition.PRICE_ABOVE,
                    targetPrice = 6.0,
                    isActive = true,
                    status = MarketAlertStatus.WAITING,
                    createdAtEpochMs = t0,
                    updatedAtEpochMs = t0
                )
            )
        )
        val watchlistRepo = MarketWatchlistRepository(watchlistStore)
        val alertRepo = MarketAlertRepository(alertStore)
        val manager = BackupManager(storage, watchlistRepo, alertRepo)

        // Case A: failure during SQLite/Room transaction after earlier tables were inserted
        storage.failOnTableInsert = "subtasks"
        val payloadA = NexusBackupPayload(
            formatVersion = 7,
            databaseVersion = 6,
            categories = listOf(CategoryBackupDto(id = "cat-rollback", name = "Rollback", colorHex = "#9A9A9A")),
            tasks = listOf(TaskBackupDto(id = "task-rollback", title = "Rollback Task")),
            subtasks = listOf(SubtaskBackupDto(id = "sub-rollback", taskId = "task-rollback", title = "Boom")),
            marketConfig = MarketBackupConfigDto(favoriteAssetCodes = listOf(MarketAssetCatalog.BTC_BRL_CODE))
        )
        try {
            manager.mergeBackupAtomically(payloadA)
            fail("Expected simulated SQLite error")
        } catch (_: IllegalStateException) {
            // expected rollback
        }
        assertFalse(storage.categories.any { it.id == "cat-rollback" })
        assertFalse(storage.tasks.any { it.id == "task-rollback" })
        assertEquals(listOf(MarketAssetCatalog.USD_BRL_CODE), watchlistRepo.favoriteCodes.value)

        // Case B: Market alert store fails -> SQLite tasks and watchlist must ALSO roll back!
        storage.failOnTableInsert = null
        alertStore.failOnSave = true
        val payloadB = NexusBackupPayload(
            formatVersion = 7,
            databaseVersion = 6,
            tasks = listOf(TaskBackupDto(id = "task-rollback-market", title = "Should Rollback")),
            marketConfig = MarketBackupConfigDto(
                favoriteAssetCodes = listOf(MarketAssetCatalog.PETR4_CODE),
                priceAlerts = listOf(
                    MarketPriceAlertBackupDto(
                        id = "new-alert-boom",
                        assetCode = MarketAssetCatalog.PETR4_CODE,
                        condition = MarketAlertCondition.PRICE_BELOW.name,
                        targetPrice = 30.0,
                        currencyOrUnit = "BRL"
                    )
                )
            )
        )
        try {
            manager.mergeBackupAtomically(payloadB)
            fail("Expected simulated Market alert store error")
        } catch (_: IllegalStateException) {
            // expected rollback
        }
        assertFalse(storage.tasks.any { it.id == "task-rollback-market" })
        assertEquals(listOf(MarketAssetCatalog.USD_BRL_CODE), watchlistRepo.favoriteCodes.value)
        assertEquals(1, alertRepo.alerts.value.size)
        assertEquals("local-alert-1", alertRepo.alerts.value.single().id)
    }

    // Scenario 16: Compatibility across legacy format versions v1..v7 + MarketConfig incremental merge
    @Test
    fun `legacy backup formats v1 through v7 parse validate and merge cleanly including market watchlist and alerts`() = runBlocking {
        val storage = InMemoryBackupStorageGateway()
        seedLocalDb(storage)
        val watchlistRepo = MarketWatchlistRepository(InMemoryWatchlistStore(listOf(MarketAssetCatalog.USD_BRL_CODE)))
        val alertRepo = MarketAlertRepository(
            InMemoryAlertStore(
                listOf(
                    MarketPriceAlert(
                        id = "alert-shared",
                        assetCode = MarketAssetCatalog.USD_BRL_CODE,
                        condition = MarketAlertCondition.PRICE_ABOVE,
                        targetPrice = 5.95,
                        isActive = true,
                        status = MarketAlertStatus.WAITING,
                        createdAtEpochMs = t0,
                        updatedAtEpochMs = t0
                    )
                )
            )
        )
        val manager = BackupManager(storage, watchlistRepo, alertRepo)

        // v1 minimal JSON (before goals/recurrences/marketConfig)
        val v1Json = """
            {
              "formatVersion": 1,
              "appVersion": "1.0.0",
              "exportedAt": "2026-01-10T12:00:00Z",
              "databaseVersion": 1,
              "categories": [{"id": "cat-v1", "name": "Legado v1"}],
              "tasks": [{"id": "task-v1", "title": "Tarefa v1", "categoryId": "cat-v1"}]
            }
        """.trimIndent()
        val v1Result = manager.validateAndParse(v1Json) as BackupValidationResult.Valid
        assertNull(manager.validateForMerge(v1Result.preview.payload))
        val v1Summary = manager.mergeBackupAtomically(v1Result.preview.payload)
        assertEquals(2, v1Summary.totalInserted)
        assertTrue(storage.tasks.any { it.id == "task-v1" })
        // Local market intact on v1 without marketConfig
        assertEquals(listOf(MarketAssetCatalog.USD_BRL_CODE), watchlistRepo.favoriteCodes.value)

        // v7 with marketConfig: unions favorites and merges alerts preserving conflicting local alert
        val v7Payload = NexusBackupPayload(
            formatVersion = 7,
            databaseVersion = 6,
            tasks = listOf(TaskBackupDto(id = "task-v7", title = "Tarefa v7")),
            marketConfig = MarketBackupConfigDto(
                favoriteAssetCodes = listOf(MarketAssetCatalog.USD_BRL_CODE, MarketAssetCatalog.BTC_BRL_CODE),
                priceAlerts = listOf(
                    MarketPriceAlertBackupDto(
                        id = "alert-shared",
                        assetCode = MarketAssetCatalog.USD_BRL_CODE,
                        condition = MarketAlertCondition.PRICE_ABOVE.name,
                        targetPrice = 5.10, // different -> conflict, keep local 5.95
                        currencyOrUnit = "BRL"
                    ),
                    MarketPriceAlertBackupDto(
                        id = "alert-new-btc",
                        assetCode = MarketAssetCatalog.BTC_BRL_CODE,
                        condition = MarketAlertCondition.PRICE_BELOW.name,
                        targetPrice = 350_000.0,
                        currencyOrUnit = "BRL"
                    )
                )
            )
        )
        val v7Summary = manager.mergeBackupAtomically(v7Payload)
        assertEquals(1, v7Summary.totalInserted)
        assertTrue(v7Summary.marketConfigApplied)
        assertEquals(1, v7Summary.marketFavoritesAdded)
        assertEquals(1, v7Summary.marketFavoritesAlreadyExisting)
        assertEquals(1, v7Summary.marketAlertsAdded)
        assertEquals(1, v7Summary.marketAlertsConflicts)
        assertEquals(listOf(MarketAssetCatalog.USD_BRL_CODE, MarketAssetCatalog.BTC_BRL_CODE), watchlistRepo.favoriteCodes.value)
        assertEquals(2, alertRepo.alerts.value.size)
        assertEquals(5.95, alertRepo.alerts.value.first { it.id == "alert-shared" }.targetPrice, 0.001)
    }

    // Scenarios 21 & 22: Dry-run previewMerge executes zero writes and matches actual merge predictions
    @Test
    fun `previewMerge runs real dry-run with zero writes and matches subsequent mergeBackupAtomically`() = runBlocking {
        val storage = InMemoryBackupStorageGateway()
        seedLocalDb(storage)
        val watchlistStore = InMemoryWatchlistStore(listOf(MarketAssetCatalog.USD_BRL_CODE))
        val alertStore = InMemoryAlertStore(
            listOf(
                MarketPriceAlert(
                    id = "alert-local-only",
                    assetCode = MarketAssetCatalog.USD_BRL_CODE,
                    condition = MarketAlertCondition.PRICE_ABOVE,
                    targetPrice = 6.20,
                    isActive = true,
                    status = MarketAlertStatus.WAITING,
                    createdAtEpochMs = t0,
                    updatedAtEpochMs = t0
                )
            )
        )
        val watchlistRepo = MarketWatchlistRepository(watchlistStore)
        val alertRepo = MarketAlertRepository(alertStore)
        val manager = BackupManager(storage, watchlistRepo, alertRepo)

        val payload = NexusBackupPayload(
            formatVersion = 7,
            databaseVersion = 6,
            categories = listOf(
                CategoryBackupDto(id = "cat-local-1", name = "Moradia Local", colorHex = "#4C8BF5"),
                CategoryBackupDto(id = "cat-preview-new", name = "Preview", colorHex = "#34C759")
            ),
            tasks = listOf(
                TaskBackupDto(
                    id = "task-shared-conflict",
                    title = "Versão Antiga",
                    status = TaskStatus.NOT_STARTED.name
                ),
                TaskBackupDto(
                    id = "task-preview-orphan",
                    title = "Órfã Opcional",
                    projectId = "proj-missing"
                )
            ),
            subtasks = listOf(
                SubtaskBackupDto(id = "sub-invalid", taskId = "task-missing", title = "Sem tarefa")
            ),
            marketConfig = MarketBackupConfigDto(
                favoriteAssetCodes = listOf(MarketAssetCatalog.USD_BRL_CODE, MarketAssetCatalog.BTC_BRL_CODE),
                priceAlerts = listOf(
                    MarketPriceAlertBackupDto(
                        id = "alert-local-only",
                        assetCode = MarketAssetCatalog.USD_BRL_CODE,
                        condition = MarketAlertCondition.PRICE_ABOVE.name,
                        targetPrice = 5.50,
                        currencyOrUnit = "BRL"
                    ),
                    MarketPriceAlertBackupDto(
                        id = "alert-preview-new",
                        assetCode = MarketAssetCatalog.BTC_BRL_CODE,
                        condition = MarketAlertCondition.PRICE_BELOW.name,
                        targetPrice = 320_000.0,
                        currencyOrUnit = "BRL"
                    )
                )
            )
        )

        val categoriesBefore = storage.categories.toList()
        val tasksBefore = storage.tasks.toList()
        val subtasksBefore = storage.subtasks.toList()
        val favsBefore = watchlistRepo.favoriteCodes.value.toList()
        val alertsBefore = alertRepo.alerts.value.toList()

        val preview = manager.previewMerge(payload)

        // Zero side effects during preview
        assertEquals(0, storage.clearAllCallsCount)
        assertEquals(categoriesBefore, storage.categories)
        assertEquals(tasksBefore, storage.tasks)
        assertEquals(subtasksBefore, storage.subtasks)
        assertEquals(favsBefore, watchlistRepo.favoriteCodes.value)
        assertEquals(alertsBefore, alertRepo.alerts.value)
        assertEquals(0, preview.totalInserted)
        assertFalse(preview.marketConfigApplied)
        assertEquals(2, preview.totalNewRecords) // cat-preview-new + task-preview-orphan
        assertEquals(1, preview.totalAlreadyExisting) // cat-local-1
        assertEquals(1, preview.totalConflicts) // task-shared-conflict
        assertEquals(1, preview.totalInvalid) // sub-invalid
        assertEquals(2, preview.totalNeedsReview) // sub-invalid + task-preview-orphan sanitized FK
        assertEquals(1, preview.marketFavoritesAdded)
        assertEquals(1, preview.marketAlertsAdded)
        assertEquals(1, preview.marketAlertsConflicts)

        // Executing merge immediately afterward yields exact same predictions applied
        val applied = manager.mergeBackupAtomically(payload)
        assertEquals(0, storage.clearAllCallsCount)
        assertEquals(preview.totalNewRecords, applied.totalInserted)
        assertEquals(preview.totalAlreadyExisting, applied.totalAlreadyExisting)
        assertEquals(preview.totalConflicts, applied.totalConflicts)
        assertEquals(preview.totalInvalid, applied.totalInvalid)
        assertEquals(preview.totalNeedsReview, applied.totalNeedsReview)
        assertEquals(preview.totalSingletonSkipped, applied.totalSingletonSkipped)
        assertEquals(preview.totalPreservedLocalOnly, applied.totalPreservedLocalOnly)
        assertEquals(preview.issues, applied.issues)
        assertEquals(preview.marketFavoritesAdded, applied.marketFavoritesAdded)
        assertEquals(preview.marketAlertsAdded, applied.marketAlertsAdded)
        assertTrue(applied.marketConfigApplied)
    }

    @Test
    fun `previewMerge rejects incompatible backup versions and preserves distinct import modes`() = runBlocking {
        val storage = InMemoryBackupStorageGateway()
        seedLocalDb(storage)
        val manager = BackupManager(storage)

        assertEquals(
            setOf(BackupImportMode.FULL_RESTORE, BackupImportMode.INCREMENTAL_MERGE),
            BackupImportMode.entries.toSet()
        )

        val incompatible = NexusBackupPayload(formatVersion = 99, databaseVersion = 6)
        try {
            manager.previewMerge(incompatible)
            fail("Expected IllegalArgumentException in previewMerge for incompatible formatVersion")
        } catch (_: IllegalArgumentException) {
            // expected
        }
        assertEquals(0, storage.clearAllCallsCount)
        assertEquals(2, storage.tasks.size)
    }

    @Test
    fun `v8 backup restore and incremental merge preserve appointment and event recurrences templates exceptions and virtual reminders`() = runBlocking {
        val storage = InMemoryBackupStorageGateway()
        seedLocalDb(storage)
        val manager = BackupManager(storage)

        val templateJson = RecurrenceEngine.serializeChecklistTemplate(listOf("Aquecer", "Registrar tempo"))
        val payload = NexusBackupPayload(
            formatVersion = NexusBackupPayload.CURRENT_FORMAT_VERSION,
            databaseVersion = 7,
            categories = listOf(
                CategoryBackupDto(id = "cat-v8", name = "Rotina", colorHex = "#4C8BF5")
            ),
            recurrences = listOf(
                TaskRecurrenceBackupDto(
                    id = "rec-task-v8",
                    title = "Treino funcional",
                    categoryId = "cat-v8",
                    frequency = TaskRecurrenceFrequency.ANNUAL.name,
                    startDateIso = "2026-11-26",
                    domain = RecurrenceDomain.TASK.name,
                    templateSubtasksJson = templateJson
                ),
                TaskRecurrenceBackupDto(
                    id = "rec-appt-v8",
                    title = "Mentoria quinzenal",
                    categoryId = "cat-v8",
                    frequency = TaskRecurrenceFrequency.BIWEEKLY.name,
                    startDateIso = "2026-11-26",
                    domain = RecurrenceDomain.APPOINTMENT.name,
                    isAllDay = false,
                    durationMillis = 5_400_000L,
                    location = "Sala 3",
                    link = "https://meet.nexus.local/mentoria"
                ),
                TaskRecurrenceBackupDto(
                    id = "rec-ev-v8",
                    title = "Feira Anual de Tecnologia",
                    categoryId = "cat-v8",
                    frequency = TaskRecurrenceFrequency.ANNUAL.name,
                    startDateIso = "2026-11-26",
                    domain = RecurrenceDomain.EVENT.name,
                    isAllDay = true,
                    durationMillis = 172_800_000L,
                    locationName = "Centro de Convenções",
                    city = "São Paulo",
                    state = "SP",
                    eventStatus = EventLifecycleStatus.CONFIRMED.name,
                    ticketStatus = TicketStatus.NOT_BOUGHT.name
                )
            ),
            appointments = listOf(
                AppointmentBackupDto(
                    id = "appt-mat-1",
                    title = "Mentoria quinzenal",
                    categoryId = "cat-v8",
                    startAt = t0,
                    endAt = t0 + 5_400_000L,
                    location = "Sala 3",
                    recurrenceId = "rec-appt-v8",
                    occurrenceDateIso = "2026-11-26"
                )
            ),
            events = listOf(
                EventBackupDto(
                    id = "ev-mat-1",
                    name = "Feira Anual de Tecnologia",
                    categoryId = "cat-v8",
                    startAt = t0,
                    endAt = t0 + 172_800_000L,
                    isAllDay = true,
                    locationName = "Centro de Convenções",
                    recurrenceId = "rec-ev-v8",
                    occurrenceDateIso = "2026-11-26"
                )
            ),
            recurrenceExceptions = listOf(
                RecurrenceExceptionBackupDto(
                    id = "exc-appt-ovr",
                    recurrenceId = "rec-appt-v8",
                    occurrenceDateIso = "2026-12-10",
                    exceptionType = RecurrenceExceptionType.OVERRIDE.name,
                    overrideTitle = "Mentoria Especial",
                    overrideDueAt = t0 + 1_200_000_000L,
                    overrideEndAt = t0 + 1_205_400_000L,
                    overrideDescription = "Com convidado",
                    overrideLocation = "Auditório",
                    overrideLink = "https://meet.nexus.local/especial",
                    overrideIsAllDay = false,
                    appointmentId = "appt-mat-1"
                ),
                RecurrenceExceptionBackupDto(
                    id = "exc-ev-ovr",
                    recurrenceId = "rec-ev-v8",
                    occurrenceDateIso = "2027-11-26",
                    exceptionType = RecurrenceExceptionType.OVERRIDE.name,
                    overrideTitle = "Feira Edição 2027",
                    eventId = "ev-mat-1"
                )
            ),
            reminders = listOf(
                ReminderBackupDto(
                    id = "rem-virt-appt",
                    sourceType = ReminderSourceType.APPOINTMENT.name,
                    sourceId = RecurrenceEngine.virtualOccurrenceId("rec-appt-v8", "2026-12-10"),
                    category = ReminderCategory.SCHEDULE.name,
                    title = "Mentoria Especial",
                    triggerAt = t0 + 1_199_100_000L,
                    minutesBefore = 15
                ),
                ReminderBackupDto(
                    id = "rem-virt-ev",
                    sourceType = ReminderSourceType.EVENT.name,
                    sourceId = RecurrenceEngine.virtualOccurrenceId("rec-ev-v8", "2027-11-26"),
                    category = ReminderCategory.SCHEDULE.name,
                    title = "Feira Edição 2027",
                    triggerAt = t0 + 2_000_000_000L,
                    minutesBefore = 60
                )
            )
        )

        val mergeSummary = manager.mergeBackupAtomically(payload)
        assertEquals(0, mergeSummary.totalInvalid)
        assertEquals(0, mergeSummary.totalConflicts)
        assertEquals(10, mergeSummary.totalInserted)

        val mergedApptRec = storage.taskRecurrences.first { it.id == "rec-appt-v8" }
        assertEquals(RecurrenceDomain.APPOINTMENT, mergedApptRec.domain)
        assertEquals("Sala 3", mergedApptRec.location)
        val mergedEvRec = storage.taskRecurrences.first { it.id == "rec-ev-v8" }
        assertEquals(RecurrenceDomain.EVENT, mergedEvRec.domain)
        assertEquals(TaskRecurrenceFrequency.ANNUAL, mergedEvRec.frequency)
        assertEquals("São Paulo", mergedEvRec.city)
        val mergedTaskRec = storage.taskRecurrences.first { it.id == "rec-task-v8" }
        assertEquals(listOf("Aquecer", "Registrar tempo"), RecurrenceEngine.deserializeChecklistTemplate(mergedTaskRec.templateSubtasksJson))

        val mergedApptExc = storage.recurrenceExceptions.first { it.id == "exc-appt-ovr" }
        assertEquals("Auditório", mergedApptExc.overrideLocation)
        assertEquals("Com convidado", mergedApptExc.overrideDescription)
        assertEquals("appt-mat-1", mergedApptExc.appointmentId)

        // Export -> validate -> full restore round-trip must preserve everything without FK violations
        val exportedJson = manager.exportToJson()
        val parsed = manager.validateAndParse(exportedJson) as BackupValidationResult.Valid
        assertEquals(NexusBackupPayload.CURRENT_FORMAT_VERSION, parsed.preview.formatVersion)

        val restoreStorage = InMemoryBackupStorageGateway()
        val restoreManager = BackupManager(restoreStorage)
        val restoreSummary = restoreManager.restoreAtomically(parsed.preview.payload)
        assertEquals(3, restoreSummary.recurrencesRestored)
        assertEquals(2, restoreSummary.recurrenceExceptionsRestored)
        assertTrue(restoreStorage.events.any { it.id == "ev-mat-1" && it.recurrenceId == "rec-ev-v8" })
        assertTrue(restoreStorage.appointments.any { it.id == "appt-mat-1" && it.recurrenceId == "rec-appt-v8" })
    }

    @Test
    fun `restoring legacy v7 task recurrence without templateSubtasksJson backfills template from subtasks`() = runBlocking {
        val storage = InMemoryBackupStorageGateway()
        val manager = BackupManager(storage)

        val legacyV7Payload = NexusBackupPayload(
            formatVersion = 7,
            databaseVersion = 6,
            recurrences = listOf(
                TaskRecurrenceBackupDto(
                    id = "rec-legacy-v7",
                    title = "Fechamento semanal",
                    startDateIso = "2026-11-20",
                    templateSubtasksJson = "[]"
                )
            ),
            tasks = listOf(
                TaskBackupDto(
                    id = "t-legacy-occ",
                    title = "Fechamento semanal",
                    status = TaskStatus.NOT_STARTED.name,
                    recurrenceId = "rec-legacy-v7",
                    occurrenceDateIso = "2026-11-20",
                    createdAt = t0,
                    updatedAt = t0
                )
            ),
            subtasks = listOf(
                SubtaskBackupDto(id = "sub-v7-1", taskId = "t-legacy-occ", title = "Conferir extrato", sortOrder = 0),
                SubtaskBackupDto(id = "sub-v7-2", taskId = "t-legacy-occ", title = "Arquivar comprovantes", sortOrder = 1)
            )
        )

        manager.restoreAtomically(legacyV7Payload)
        val restoredRec = storage.taskRecurrences.single()
        assertEquals(
            listOf("Conferir extrato", "Arquivar comprovantes"),
            RecurrenceEngine.deserializeChecklistTemplate(restoredRec.templateSubtasksJson)
        )
    }

    private fun LoanBackupDtoFixture() = com.nexus.app.data.backup.LoanBackupDto(
        id = "loan-new-1",
        creditor = "Banco Teste",
        originalAmountCents = 100_000L,
        contractedAmountCents = 110_000L,
        installmentsTotal = 10,
        installmentAmountCents = 11_000L,
        remainingBalanceCents = 110_000L
    )
}
