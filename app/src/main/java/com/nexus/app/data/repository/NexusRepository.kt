package com.nexus.app.data.repository

import androidx.room.withTransaction
import com.nexus.app.core.DateTimeUtils
import com.nexus.app.data.backup.BackupManager
import com.nexus.app.data.backup.BackupValidationResult
import com.nexus.app.data.backup.MergeSummary
import com.nexus.app.data.backup.NexusBackupPayload
import com.nexus.app.data.backup.RestoreSummary
import com.nexus.app.data.inbox.InboxIngestionPort
import com.nexus.app.data.ingestion.RoomStructuredInputGateway
import com.nexus.app.data.ingestion.StructuredBatchPreview
import com.nexus.app.data.ingestion.StructuredBatchResult
import com.nexus.app.data.ingestion.StructuredInputEnvelope
import com.nexus.app.data.ingestion.StructuredInputProcessor
import com.nexus.app.domain.rules.AgendaEngine
import com.nexus.app.domain.rules.AgendaFilters
import com.nexus.app.domain.rules.AgendaOccurrence
import com.nexus.app.domain.rules.AgendaOccurrenceKind
import com.nexus.app.domain.rules.AgendaSourceType
import com.nexus.app.domain.rules.AgendaSources
import com.nexus.app.domain.rules.CashflowSeriesMode
import com.nexus.app.domain.rules.CategorySpendScope
import com.nexus.app.domain.rules.DateRange
import com.nexus.app.domain.rules.FinanceRules
import com.nexus.app.domain.rules.MonthlyFinanceDashboardData
import com.nexus.app.domain.rules.RecurrenceEngine
import com.nexus.app.domain.rules.ReminderRules
import com.nexus.app.domain.rules.ReminderSyncAction
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
import com.nexus.app.data.db.entity.SettingsEntity
import com.nexus.app.data.db.entity.SubtaskEntity
import com.nexus.app.data.db.entity.TaskEntity
import com.nexus.app.data.db.entity.TaskRecurrenceEntity
import com.nexus.app.data.db.entity.TransactionEntity
import com.nexus.app.domain.model.AgendaItem
import com.nexus.app.domain.model.BriefingType
import com.nexus.app.domain.model.BudgetProgress
import com.nexus.app.domain.model.CardWithStatements
import com.nexus.app.domain.model.CategoryScope
import com.nexus.app.domain.model.CategorySpendPoint
import com.nexus.app.domain.model.DailyBriefingSnapshot
import com.nexus.app.domain.model.DailyQuoteSelection
import com.nexus.app.domain.model.DashboardBlockConfig
import com.nexus.app.domain.model.DashboardBlockType
import com.nexus.app.domain.model.DashboardSnapshot
import com.nexus.app.domain.model.DatabaseTableCounts
import com.nexus.app.domain.model.EventCostType
import com.nexus.app.domain.model.EventLifecycleStatus
import com.nexus.app.domain.model.EventWithDetails
import com.nexus.app.domain.model.FinanceSummary
import com.nexus.app.domain.model.FinancialStatus
import com.nexus.app.domain.model.GlobalSearchResults
import com.nexus.app.domain.model.GoalProgressMode
import com.nexus.app.domain.model.GoalStatus
import com.nexus.app.domain.model.InstallmentPurchaseWithItems
import com.nexus.app.domain.model.MonthlyCashflowPoint
import com.nexus.app.domain.model.NexusDateFormat
import com.nexus.app.domain.model.NightReviewSnapshot
import com.nexus.app.domain.model.PaymentMethod
import com.nexus.app.domain.model.PersonalGoalWithDetails
import com.nexus.app.domain.model.ProjectStatus
import com.nexus.app.domain.model.ProjectWithProgress
import com.nexus.app.domain.model.QuoteLibraryStatus
import com.nexus.app.domain.model.RecurrenceDomain
import com.nexus.app.domain.model.RecurrenceExceptionType
import com.nexus.app.domain.model.RecurrenceFrequency
import com.nexus.app.domain.model.RecurringEditScope
import com.nexus.app.domain.model.ReminderCategory
import com.nexus.app.domain.model.ReminderSourceType
import com.nexus.app.domain.model.ReminderStatus
import com.nexus.app.domain.model.StatementStatus
import com.nexus.app.domain.model.StatementWithDetails
import com.nexus.app.domain.model.TaskPriority
import com.nexus.app.domain.model.TaskRecurrenceDraft
import com.nexus.app.domain.model.TaskStatus
import com.nexus.app.domain.model.TaskWithDetails
import com.nexus.app.domain.model.TicketStatus
import com.nexus.app.domain.model.TransactionType
import com.nexus.app.domain.model.TransactionWithDetails
import com.nexus.app.notifications.NexusNotificationChannels
import com.nexus.app.notifications.NexusNotificationScheduler
import com.nexus.app.widgets.AllWidgetSnapshots
import com.nexus.app.widgets.FinanceWidgetSnapshot
import com.nexus.app.widgets.MyDayAppointmentRow
import com.nexus.app.widgets.MyDayTaskRow
import com.nexus.app.widgets.MyDayWidgetSnapshot
import com.nexus.app.widgets.MyTasksWidgetSnapshot
import com.nexus.app.widgets.NexusSummaryWidgetSnapshot
import com.nexus.app.widgets.WidgetDueTransactionRow
import com.nexus.app.widgets.WidgetOpenStatementRow
import com.nexus.app.widgets.WidgetTaskRow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlin.math.abs

class NexusRepository(
    private val database: NexusDatabase,
    private val backupManager: BackupManager,
    private val notificationScheduler: NexusNotificationScheduler? = null,
    private val onDataChanged: (suspend () -> Unit)? = null
) : InboxIngestionPort {
    private val settingsDao = database.settingsDao()
    private val categoryDao = database.categoryDao()
    private val projectDao = database.projectDao()
    private val eventDao = database.eventDao()
    private val taskDao = database.taskDao()
    private val subtaskDao = database.subtaskDao()
    private val appointmentDao = database.appointmentDao()
    private val creditCardDao = database.creditCardDao()
    private val statementDao = database.creditCardStatementDao()
    private val installmentPurchaseDao = database.installmentPurchaseDao()
    private val recurringRuleDao = database.recurringRuleDao()
    private val transactionDao = database.transactionDao()
    private val loanDao = database.loanDao()
    private val budgetDao = database.budgetDao()
    private val financialGoalDao = database.financialGoalDao()
    private val auditHistoryDao = database.auditHistoryDao()
    private val errorLogDao = database.errorLogDao()
    private val reminderDao = database.reminderDao()
    private val routineSettingsDao = database.routineAssistantSettingsDao()
    private val briefingHistoryDao = database.briefingHistoryDao()
    private val personalizationSettingsDao = database.personalizationSettingsDao()
    private val quoteDao = database.quoteDao()
    private val personalGoalDao = database.personalGoalDao()
    private val goalMilestoneDao = database.goalMilestoneDao()
    private val goalProgressLogDao = database.goalProgressLogDao()
    private val taskRecurrenceDao = database.taskRecurrenceDao()
    private val recurrenceExceptionDao = database.recurrenceExceptionDao()
    private val structuredInputProcessor = StructuredInputProcessor(RoomStructuredInputGateway(database))

    fun observeSettings(): Flow<SettingsEntity> =
        settingsDao.observeSettings().combine(flowOf(Unit)) { entity, _ ->
            entity ?: SettingsEntity(
                id = SettingsEntity.SINGLETON_ID,
                userName = "Enzo",
                darkTheme = true,
                declaredBalanceCents = 0L,
                createdAt = DateTimeUtils.nowEpochMillis(),
                updatedAt = DateTimeUtils.nowEpochMillis()
            )
        }

    fun observePersonalizationSettings(): Flow<PersonalizationSettingsEntity> =
        personalizationSettingsDao.observeSettings().combine(flowOf(Unit)) { entity, _ ->
            entity ?: defaultPersonalizationSettings(DateTimeUtils.nowEpochMillis())
        }

    fun observeQuotes(): Flow<List<QuoteEntity>> = quoteDao.observeAll()

    fun observePersonalGoalsWithDetails(): Flow<List<PersonalGoalWithDetails>> =
        combine(
            personalGoalDao.observeAll(),
            categoryDao.observeAll(),
            goalMilestoneDao.observeAll(),
            observeTasksWithDetails(),
            combine(observeProjectsWithProgress(), goalProgressLogDao.observeAll()) { p, l -> p to l }
        ) { goals, categories, milestones, tasks, (projects, logs) ->
            val catMap = categories.associateBy { it.id }
            val milestonesByGoal = milestones.groupBy { it.goalId }
            val tasksByGoal = tasks.groupBy { it.task.goalId }
            val projectsByGoal = projects.groupBy { it.project.goalId }
            val logsByGoal = logs.groupBy { it.goalId }
            val startOfToday = DateTimeUtils.localDateToStartOfDayEpoch(DateTimeUtils.todayLocalDate())

            goals.map { goal ->
                val gMilestones = milestonesByGoal[goal.id].orEmpty()
                val gTasks = tasksByGoal[goal.id].orEmpty()
                val gProjects = projectsByGoal[goal.id].orEmpty()
                PersonalGoalWithDetails(
                    goal = goal,
                    category = goal.categoryId?.let { catMap[it] },
                    milestones = gMilestones,
                    linkedTasks = gTasks,
                    linkedProjects = gProjects,
                    progressLogs = logsByGoal[goal.id].orEmpty(),
                    isOverdue = isGoalOverdue(goal, startOfToday),
                    progressPercent = computeGoalProgressPercent(goal, gMilestones, gTasks, gProjects)
                )
            }
        }

    fun observeCategories(): Flow<List<CategoryEntity>> = categoryDao.observeAll()

    fun observeProjectsWithProgress(): Flow<List<ProjectWithProgress>> =
        combine(projectDao.observeAll(), taskDao.observeAll(), transactionDao.observeAll()) { projects, tasks, transactions ->
            val tasksByProject = tasks.groupBy { it.projectId }
            val txByProject = transactions.groupBy { it.projectId }
            projects.map { project ->
                val projectTasks = tasksByProject[project.id].orEmpty()
                val completed = projectTasks.count { it.status == TaskStatus.COMPLETED }
                val projectTx = txByProject[project.id].orEmpty()
                    .filter { it.status != FinancialStatus.CANCELLED }
                val expenses = projectTx.filter { it.type == TransactionType.EXPENSE }
                ProjectWithProgress(
                    project = project,
                    totalTasks = projectTasks.size,
                    completedTasks = completed,
                    estimatedCostCents = expenses.sumOf { it.amountCents },
                    paidCostCents = expenses.filter { it.status == FinancialStatus.SETTLED }.sumOf { it.amountCents },
                    linkedTransactionsCount = projectTx.size
                )
            }
        }

    fun observeTasksWithDetails(): Flow<List<TaskWithDetails>> =
        combine(
            taskDao.observeAll(),
            projectDao.observeAll(),
            categoryDao.observeAll(),
            eventDao.observeAll(),
            combine(
                subtaskDao.observeAll(),
                transactionDao.observeAll(),
                taskRecurrenceDao.observeAll()
            ) { subs, txs, recs -> Triple(subs, txs, recs) }
        ) { tasks, projects, categories, events, (subtasks, transactions, recurrences) ->
            val projectMap = projects.associateBy { it.id }
            val categoryMap = categories.associateBy { it.id }
            val eventMap = events.associateBy { it.id }
            val subtasksByTask = subtasks.groupBy { it.taskId }
            val txByTask = transactions.groupBy { it.taskId }
            val recMap = recurrences.associateBy { it.id }
            tasks.map { task ->
                TaskWithDetails(
                    task = task,
                    project = task.projectId?.let { projectMap[it] },
                    category = task.categoryId?.let { categoryMap[it] },
                    event = task.eventId?.let { eventMap[it] },
                    subtasks = subtasksByTask[task.id].orEmpty(),
                    linkedTransactions = txByTask[task.id].orEmpty(),
                    recurrence = task.recurrenceId?.let { recMap[it] }
                )
            }
        }

    fun observeTaskRecurrences(): Flow<List<TaskRecurrenceEntity>> = taskRecurrenceDao.observeAll()

    suspend fun getTaskRecurrenceById(id: String): TaskRecurrenceEntity? = withContext(Dispatchers.IO) {
        taskRecurrenceDao.getById(id)
    }

    suspend fun getEventById(id: String): EventEntity? = withContext(Dispatchers.IO) {
        eventDao.getById(id)
    }

    fun observeTransactionsWithDetails(): Flow<List<TransactionWithDetails>> =
        combine(
            combine(transactionDao.observeAll(), categoryDao.observeAll(), projectDao.observeAll()) { txs, cats, projs ->
                Triple(txs, cats, projs)
            },
            combine(eventDao.observeAll(), creditCardDao.observeAll(), taskDao.observeAll()) { events, cards, tasks ->
                Triple(events, cards, tasks)
            }
        ) { (transactions, categories, projects), (events, cards, tasks) ->
            val catMap = categories.associateBy { it.id }
            val projMap = projects.associateBy { it.id }
            val eventMap = events.associateBy { it.id }
            val cardMap = cards.associateBy { it.id }
            val taskMap = tasks.associateBy { it.id }
            transactions.map { tx ->
                TransactionWithDetails(
                    transaction = effectiveTransactionStatus(tx),
                    category = tx.categoryId?.let { catMap[it] },
                    project = tx.projectId?.let { projMap[it] },
                    event = tx.eventId?.let { eventMap[it] },
                    creditCard = tx.creditCardId?.let { cardMap[it] },
                    task = tx.taskId?.let { taskMap[it] }
                )
            }
        }

    fun observeEventsWithDetails(): Flow<List<EventWithDetails>> =
        combine(
            eventDao.observeAll(),
            categoryDao.observeAll(),
            creditCardDao.observeAll(),
            observeTasksWithDetails(),
            observeTransactionsWithDetails()
        ) { events, categories, cards, tasks, transactions ->
            val catMap = categories.associateBy { it.id }
            val cardMap = cards.associateBy { it.id }
            val tasksByEvent = tasks.groupBy { it.task.eventId }
            val txByEvent = transactions.groupBy { it.transaction.eventId }
            events.map { event ->
                EventWithDetails(
                    event = event,
                    category = event.categoryId?.let { catMap[it] },
                    creditCard = event.creditCardId?.let { cardMap[it] },
                    linkedTasks = tasksByEvent[event.id].orEmpty(),
                    linkedTransactions = txByEvent[event.id].orEmpty()
                )
            }
        }

    fun observeCardsWithStatements(): Flow<List<CardWithStatements>> =
        combine(
            creditCardDao.observeAll(),
            statementDao.observeAll(),
            observeTransactionsWithDetails()
        ) { cards, statements, transactions ->
            val txByStatement = transactions.groupBy { it.transaction.statementId }
            val stmtsByCard = statements.groupBy { it.creditCardId }
            cards.map { card ->
                val cardStatements = stmtsByCard[card.id].orEmpty().map { stmt ->
                    StatementWithDetails(
                        statement = effectiveStatementStatus(stmt),
                        card = card,
                        transactions = txByStatement[stmt.id].orEmpty()
                    )
                }.sortedByDescending { it.statement.yearMonth }
                val openTotal = cardStatements
                    .filter { it.statement.status != StatementStatus.PAID }
                    .sumOf { it.totalCents }
                CardWithStatements(
                    card = card,
                    statements = cardStatements,
                    currentOpenTotalCents = openTotal
                )
            }
        }

    fun observeInstallmentPurchases(): Flow<List<InstallmentPurchaseWithItems>> =
        combine(
            installmentPurchaseDao.observeAll(),
            creditCardDao.observeAll(),
            categoryDao.observeAll(),
            transactionDao.observeAll()
        ) { purchases, cards, categories, transactions ->
            val cardMap = cards.associateBy { it.id }
            val catMap = categories.associateBy { it.id }
            val txByPurchase = transactions.groupBy { it.installmentPurchaseId }
            purchases.map { purchase ->
                InstallmentPurchaseWithItems(
                    purchase = purchase,
                    card = purchase.creditCardId?.let { cardMap[it] },
                    category = purchase.categoryId?.let { catMap[it] },
                    installments = txByPurchase[purchase.id].orEmpty()
                        .map(::effectiveTransactionStatus)
                        .sortedBy { it.installmentNumber ?: 0 }
                )
            }
        }

    fun observeRecurringRules(): Flow<List<RecurringRuleEntity>> = recurringRuleDao.observeAll()

    fun observeLoans(): Flow<List<LoanEntity>> = loanDao.observeAll()

    fun observeBudgetProgress(yearMonth: String = currentYearMonth()): Flow<List<BudgetProgress>> =
        combine(
            budgetDao.observeAll(),
            categoryDao.observeAll(),
            transactionDao.observeAll()
        ) { budgets, categories, transactions ->
            val ym = runCatching { YearMonth.parse(yearMonth) }.getOrDefault(YearMonth.from(DateTimeUtils.todayLocalDate()))
            FinanceRules.computeBudgetProgress(
                budgets = budgets,
                categories = categories,
                transactions = transactions,
                targetMonth = ym
            )
        }

    fun observeFinancialGoals(): Flow<List<FinancialGoalEntity>> = financialGoalDao.observeAll()

    fun observeAuditHistory(): Flow<List<AuditHistoryEntity>> = auditHistoryDao.observeLatest()

    fun observeInboxImportLogs(): Flow<List<AuditHistoryEntity>> =
        auditHistoryDao.observeLatestByEntityType(AUDIT_ENTITY_INBOX_IMPORT_LOG, limit = 50)

    fun observeMonthlyFinanceDashboard(monthFlow: Flow<YearMonth>): Flow<MonthlyFinanceDashboardData> =
        combine(
            observeSettings(),
            observeTransactionsWithDetails(),
            budgetDao.observeAll(),
            categoryDao.observeAll(),
            monthFlow
        ) { settings, transactions, budgets, categories, month ->
            FinanceRules.computeMonthlyDashboardData(
                declaredBalanceCents = settings.declaredBalanceCents,
                transactions = transactions,
                budgets = budgets,
                categories = categories,
                targetMonth = month,
                today = DateTimeUtils.todayLocalDate()
            )
        }

    fun observeFinanceSummary(): Flow<FinanceSummary> =
        combine(
            observeSettings(),
            observeTransactionsWithDetails()
        ) { settings, transactions ->
            val today = DateTimeUtils.todayLocalDate()
            val todayYm = YearMonth.from(today)
            val data = FinanceRules.computeMonthlyDashboardData(
                declaredBalanceCents = settings.declaredBalanceCents,
                transactions = transactions,
                budgets = emptyList(),
                categories = emptyList(),
                targetMonth = todayYm,
                today = today
            )
            FinanceSummary(
                declaredBalanceCents = settings.declaredBalanceCents,
                receivedIncomeCents = data.monthlySummary.receivedIncomeCents,
                plannedIncomeCents = data.monthlySummary.plannedIncomeCents,
                paidExpenseCents = data.monthlySummary.paidExpenseCents,
                pendingExpenseCents = data.monthlySummary.pendingExpenseCents,
                overdueExpenseCents = data.monthlySummary.overdueExpenseCents,
                nextDueTransaction = data.nextDueTransaction,
                categorySpend = data.categorySpendPaidAndPlanned,
                monthlyCashflow = data.sixMonthCashflowRealized
            )
        }

    fun observeAgendaSources(): Flow<AgendaSources> {
        val coreFlow = combine(
            appointmentDao.observeAll(),
            taskDao.observeTasksWithDueDate(),
            projectDao.observeAll(),
            categoryDao.observeAll(),
            eventDao.observeAll()
        ) { appointments, tasksWithDue, projects, categories, events ->
            listOf(appointments, tasksWithDue, projects, categories, events)
        }
        val financeFlow = combine(
            transactionDao.observeAll(),
            statementDao.observeAll(),
            creditCardDao.observeAll()
        ) { transactions, statements, cards ->
            Triple(transactions, statements, cards)
        }
        val recurrenceFlow = combine(
            taskRecurrenceDao.observeAll(),
            recurrenceExceptionDao.observeAll()
        ) { recurrences, exceptions ->
            recurrences to exceptions
        }

        return combine(coreFlow, financeFlow, recurrenceFlow) { core, (transactions, statements, cards), (recurrences, exceptions) ->
            @Suppress("UNCHECKED_CAST")
            AgendaSources(
                appointments = core[0] as List<AppointmentEntity>,
                tasks = core[1] as List<TaskEntity>,
                projects = core[2] as List<ProjectEntity>,
                categories = core[3] as List<CategoryEntity>,
                events = core[4] as List<EventEntity>,
                transactions = transactions,
                statements = statements,
                creditCards = cards,
                taskRecurrences = recurrences,
                recurrenceExceptions = exceptions
            )
        }
    }

    fun observeAgendaOccurrences(
        filtersFlow: Flow<AgendaFilters>,
        windowFlow: Flow<DateRange?> = flowOf(null)
    ): Flow<List<AgendaOccurrence>> =
        combine(observeAgendaSources(), filtersFlow, windowFlow) { sources, filters, explicitWindow ->
            val today = DateTimeUtils.todayLocalDate()
            val window = explicitWindow ?: AgendaEngine.boundingRangeForSources(sources, today)
            AgendaEngine.expand(
                window = window,
                sources = sources,
                filters = filters,
                today = today,
                nowEpoch = DateTimeUtils.nowEpochMillis()
            )
        }

    /**
     * Observa a Agenda unificada via AgendaEngine diretamente das fontes do banco.
     * Nunca cria cópias no banco.
     */
    fun observeUnifiedAgenda(): Flow<List<AgendaItem>> {
        return observeAgendaSources().combine(flowOf(Unit)) { sources, _ ->
            val now = DateTimeUtils.nowEpochMillis()
            val today = DateTimeUtils.todayLocalDate()
            val window = AgendaEngine.boundingRangeForSources(sources, today)
            val occurrences = AgendaEngine.expand(
                window = window,
                sources = sources,
                filters = AgendaFilters(showPaid = true),
                today = today,
                nowEpoch = now
            )

            val categoryMap = sources.categories.associateBy { it.id }
            val projectMap = sources.projects.associateBy { it.id }
            val eventMap = sources.events.associateBy { it.id }
            val apptMap = sources.appointments.associateBy { it.id }
            val taskMap = sources.tasks.associateBy { it.id }
            val txMap = sources.transactions.associateBy { it.id }
            val stmtMap = sources.statements.associateBy { it.id }
            val cardMap = sources.creditCards.associateBy { it.id }
            val recMap = sources.taskRecurrences.associateBy { it.id }
            val exceptionMap = sources.recurrenceExceptions.associateBy { it.recurrenceId to it.occurrenceDateIso }

            occurrences.mapNotNull { occ ->
                when (occ.sourceType) {
                    AgendaSourceType.APPOINTMENT -> {
                        if (occ.isVirtualRecurrence) {
                            val rec = occ.recurrenceId?.let { recMap[it] } ?: return@mapNotNull null
                            val anchorIso = occ.seriesOccurrenceDateIso ?: occ.occurrenceDateIso
                            val ex = exceptionMap[rec.id to anchorIso]
                            val isOverride = ex?.exceptionType == RecurrenceExceptionType.OVERRIDE
                            val virtualAppt = AppointmentEntity(
                                id = RecurrenceEngine.virtualOccurrenceId(rec.id, anchorIso),
                                title = occ.title,
                                description = if (isOverride && ex?.overrideDescription != null) ex.overrideDescription else rec.description,
                                categoryId = rec.categoryId,
                                eventId = rec.eventId,
                                startAt = occ.startAt,
                                endAt = occ.endAt ?: (occ.startAt + rec.durationMillis.coerceAtLeast(60_000L)),
                                isAllDay = occ.isAllDay,
                                location = if (isOverride && ex?.overrideLocation != null) ex.overrideLocation else rec.location,
                                link = if (isOverride && ex?.overrideLink != null) ex.overrideLink else rec.link,
                                recurrenceId = rec.id,
                                occurrenceDateIso = anchorIso,
                                createdAt = rec.createdAt,
                                updatedAt = rec.updatedAt
                            )
                            AgendaItem.AppointmentItem(
                                appointment = virtualAppt,
                                category = rec.categoryId?.let { categoryMap[it] },
                                conflictTitles = occ.conflictTitles,
                                date = occ.occurrenceDate,
                                event = rec.eventId?.let { eventMap[it] },
                                recurrenceId = rec.id,
                                occurrenceDateIso = anchorIso,
                                isVirtualRecurrence = true
                            )
                        } else {
                            val appt = apptMap[occ.sourceId] ?: return@mapNotNull null
                            AgendaItem.AppointmentItem(
                                appointment = appt,
                                category = appt.categoryId?.let { categoryMap[it] },
                                conflictTitles = occ.conflictTitles,
                                date = occ.occurrenceDate,
                                event = appt.eventId?.let { eventMap[it] },
                                recurrenceId = appt.recurrenceId,
                                occurrenceDateIso = appt.occurrenceDateIso,
                                isVirtualRecurrence = false
                            )
                        }
                    }
                    AgendaSourceType.TASK -> {
                        val task = taskMap[occ.sourceId] ?: return@mapNotNull null
                        AgendaItem.TaskDeadlineItem(
                            task = task,
                            project = task.projectId?.let { projectMap[it] },
                            event = task.eventId?.let { eventMap[it] },
                            category = task.categoryId?.let { categoryMap[it] },
                            isOverdue = occ.isOverdue,
                            date = occ.occurrenceDate
                        )
                    }
                    AgendaSourceType.EVENT -> {
                        if (occ.isVirtualRecurrence) {
                            val rec = occ.recurrenceId?.let { recMap[it] } ?: return@mapNotNull null
                            val anchorIso = occ.seriesOccurrenceDateIso ?: occ.occurrenceDateIso
                            val ex = exceptionMap[rec.id to anchorIso]
                            val isOverride = ex?.exceptionType == RecurrenceExceptionType.OVERRIDE
                            val virtualEvent = EventEntity(
                                id = RecurrenceEngine.virtualOccurrenceId(rec.id, anchorIso),
                                name = occ.title,
                                description = if (isOverride && ex?.overrideDescription != null) ex.overrideDescription else rec.description,
                                categoryId = rec.categoryId,
                                startAt = occ.startAt,
                                endAt = occ.endAt ?: (occ.startAt + rec.durationMillis.coerceAtLeast(60_000L)),
                                isAllDay = occ.isAllDay,
                                officialLink = if (isOverride && ex?.overrideLink != null) ex.overrideLink else rec.officialLink,
                                ticketLink = rec.ticketLink,
                                locationName = if (isOverride && ex?.overrideLocation != null) ex.overrideLocation else rec.locationName,
                                address = rec.address,
                                city = rec.city,
                                state = rec.state,
                                notes = rec.notes,
                                eventStatus = rec.eventStatus,
                                ticketStatus = rec.ticketStatus,
                                ticketPriceCents = rec.ticketPriceCents,
                                ticketQuantity = rec.ticketQuantity,
                                ticketFeesCents = rec.ticketFeesCents,
                                ticketTotalCents = 0L,
                                ticketPurchaseDate = null,
                                ticketDueDate = null,
                                paymentMethod = rec.paymentMethod,
                                creditCardId = rec.creditCardId,
                                installmentsCount = rec.installmentsCount,
                                recurrenceId = rec.id,
                                occurrenceDateIso = anchorIso,
                                createdAt = rec.createdAt,
                                updatedAt = rec.updatedAt
                            )
                            AgendaItem.EventItem(
                                event = virtualEvent,
                                category = rec.categoryId?.let { categoryMap[it] },
                                totalCostCents = 0L,
                                date = occ.occurrenceDate,
                                spanStartDate = LocalDate.parse(occ.spanStartDateIso),
                                spanEndDate = LocalDate.parse(occ.spanEndDateIso),
                                recurrenceId = rec.id,
                                occurrenceDateIso = anchorIso,
                                seriesOccurrenceDateIso = anchorIso,
                                isVirtualRecurrence = true
                            )
                        } else {
                            val ev = eventMap[occ.sourceId] ?: return@mapNotNull null
                            AgendaItem.EventItem(
                                event = ev,
                                category = ev.categoryId?.let { categoryMap[it] },
                                totalCostCents = occ.amountCents ?: 0L,
                                date = occ.occurrenceDate,
                                spanStartDate = LocalDate.parse(occ.spanStartDateIso),
                                spanEndDate = LocalDate.parse(occ.spanEndDateIso),
                                recurrenceId = ev.recurrenceId,
                                occurrenceDateIso = ev.occurrenceDateIso,
                                seriesOccurrenceDateIso = occ.seriesOccurrenceDateIso,
                                isVirtualRecurrence = false
                            )
                        }
                    }
                    AgendaSourceType.TRANSACTION -> {
                        val tx = txMap[occ.sourceId]?.let(::effectiveTransactionStatus) ?: return@mapNotNull null
                        AgendaItem.FinancialDueItem(
                            transaction = tx,
                            category = tx.categoryId?.let { categoryMap[it] },
                            isOverdue = occ.isOverdue,
                            date = occ.occurrenceDate
                        )
                    }
                    AgendaSourceType.STATEMENT -> {
                        val stmt = stmtMap[occ.sourceId]?.let(::effectiveStatementStatus) ?: return@mapNotNull null
                        val card = cardMap[stmt.creditCardId] ?: return@mapNotNull null
                        AgendaItem.StatementDueItem(
                            statement = stmt,
                            card = card,
                            totalCents = occ.amountCents ?: 0L,
                            date = occ.occurrenceDate
                        )
                    }
                    AgendaSourceType.PROJECT -> {
                        val proj = projectMap[occ.sourceId] ?: return@mapNotNull null
                        AgendaItem.ProjectDeadlineItem(
                            project = proj,
                            date = occ.occurrenceDate
                        )
                    }
                }
            }
        }
    }

    fun observeDashboard(): Flow<DashboardSnapshot> {
        val core = combine(
            observeSettings(),
            observeUnifiedAgenda(),
            observeTasksWithDetails(),
            observeProjectsWithProgress()
        ) { settings, agenda, tasks, projects ->
            listOf(settings, agenda, tasks, projects)
        }
        val financeAndEvents = combine(
            observeEventsWithDetails(),
            observeFinanceSummary(),
            observeTransactionsWithDetails(),
            observeCardsWithStatements()
        ) { events, finance, transactions, cards ->
            listOf(events, finance, transactions, cards)
        }
        val phase6Flow = combine(
            observePersonalizationSettings(),
            observeQuotes(),
            observePersonalGoalsWithDetails(),
            observeFinancialGoals()
        ) { personalization, quotes, personalGoals, financialGoals ->
            listOf(personalization, quotes, personalGoals, financialGoals)
        }

        return combine(core, financeAndEvents, phase6Flow) { coreList, finList, p6List ->
            val settings = coreList[0] as SettingsEntity
            @Suppress("UNCHECKED_CAST")
            val agendaItems = coreList[1] as List<AgendaItem>
            @Suppress("UNCHECKED_CAST")
            val tasksWithDetails = coreList[2] as List<TaskWithDetails>
            @Suppress("UNCHECKED_CAST")
            val projects = coreList[3] as List<ProjectWithProgress>

            @Suppress("UNCHECKED_CAST")
            val events = finList[0] as List<EventWithDetails>
            val financeSummary = finList[1] as FinanceSummary
            @Suppress("UNCHECKED_CAST")
            val allTransactions = finList[2] as List<TransactionWithDetails>
            @Suppress("UNCHECKED_CAST")
            val cardsWithStatements = finList[3] as List<CardWithStatements>

            val personalization = p6List[0] as PersonalizationSettingsEntity
            @Suppress("UNCHECKED_CAST")
            val quotes = p6List[1] as List<QuoteEntity>
            @Suppress("UNCHECKED_CAST")
            val personalGoals = p6List[2] as List<PersonalGoalWithDetails>
            @Suppress("UNCHECKED_CAST")
            val financialGoals = p6List[3] as List<FinancialGoalEntity>

            val now = DateTimeUtils.nowEpochMillis()
            val today = DateTimeUtils.todayLocalDate()
            val todayIso = today.toString()

            val appointments = agendaItems.filterIsInstance<AgendaItem.AppointmentItem>()
            val todayAppointments = appointments.filter { it.date == today }
            val nextAppointment = appointments
                .filter { !it.appointment.isAllDay && it.appointment.endAt >= now }
                .minByOrNull { it.appointment.startAt }
                ?: appointments.firstOrNull { it.date >= today }

            val overdueTasks = tasksWithDetails.filter { isTaskOverdue(it.task, now, today) }
            val priorityTasksToday = tasksWithDetails
                .filter { details ->
                    val t = details.task
                    !t.status.isTerminal && (
                        (t.dueAt != null && DateTimeUtils.epochToLocalDate(t.dueAt) == today) ||
                            t.priority == TaskPriority.URGENT ||
                            t.priority == TaskPriority.HIGH
                        )
                }
                .sortedWith(
                    compareByDescending<TaskWithDetails> { it.task.priority.sortRank }
                        .thenBy { it.task.dueAt ?: Long.MAX_VALUE }
                )
                .take(8)

            val materializedUpcomingEvents = events
                .filter { it.event.eventStatus != EventLifecycleStatus.CANCELLED && it.event.endAt >= DateTimeUtils.localDateToStartOfDayEpoch(today) }
            val startOfTodayEpoch = DateTimeUtils.localDateToStartOfDayEpoch(today)
            val virtualUpcomingEvents = agendaItems
                .filterIsInstance<AgendaItem.EventItem>()
                .filter {
                    it.isVirtualRecurrence &&
                        it.event.eventStatus != EventLifecycleStatus.CANCELLED &&
                        it.event.endAt >= startOfTodayEpoch
                }
                .distinctBy { it.event.id }
                .map { item ->
                    EventWithDetails(
                        event = item.event,
                        category = item.category,
                        creditCard = null,
                        linkedTasks = emptyList(),
                        linkedTransactions = emptyList()
                    )
                }
            val upcomingEvents = (materializedUpcomingEvents + virtualUpcomingEvents)
                .sortedBy { it.event.startAt }
                .take(8)

            val upcomingPayments = allTransactions
                .filter {
                    AgendaEngine.isDirectAgendaBill(it.transaction)
                }
                .sortedWith(
                    compareByDescending<TransactionWithDetails> { it.transaction.status == FinancialStatus.OVERDUE }
                        .thenBy { it.transaction.dueAt ?: Long.MAX_VALUE }
                )
                .take(8)

            val openStatements = cardsWithStatements
                .flatMap { it.statements }
                .filter { it.statement.status != StatementStatus.PAID && it.totalCents > 0L }
                .sortedBy { it.statement.dueAt }

            val pendingCount = tasksWithDetails.count { !it.task.status.isTerminal }
            val completedCount = tasksWithDetails.count { it.task.status == TaskStatus.COMPLETED }
            val taskDistribution = computeMonthlyTaskDistribution(
                tasks = tasksWithDetails.map { it.task },
                targetMonth = YearMonth.from(today),
                nowEpoch = now,
                today = today
            )
            val activeProjects = projects.filter { it.project.status == ProjectStatus.ACTIVE }
            val activeProjectsCount = activeProjects.size

            val blocks = parseDashboardBlocks(personalization.dashboardBlocksJson)
            val (quoteSelection, _) = selectDailyQuote(
                quotes = quotes,
                personalization = personalization,
                todayIso = todayIso
            )

            DashboardSnapshot(
                userName = settings.userName.ifBlank { "Enzo" },
                todayAppointments = todayAppointments,
                nextAppointment = nextAppointment,
                priorityTasksToday = priorityTasksToday,
                overdueTasks = overdueTasks,
                upcomingEvents = upcomingEvents,
                financeSummary = financeSummary,
                pendingCount = pendingCount,
                completedCount = completedCount,
                taskDistribution = taskDistribution,
                activeProjectsCount = activeProjectsCount,
                totalItemsInDb = tasksWithDetails.size + appointments.size + projects.size + events.size + personalGoals.size + allTransactions.size,
                personalization = personalization,
                blocks = blocks,
                dailyQuoteSelection = quoteSelection,
                personalGoals = personalGoals,
                financialGoals = financialGoals,
                upcomingPayments = upcomingPayments,
                openStatements = openStatements,
                activeProjects = activeProjects
            )
        }
    }

    fun observeGlobalSearch(query: String): Flow<GlobalSearchResults> {
        val clean = query.trim()
        if (clean.isEmpty()) return flowOf(GlobalSearchResults())

        val searchFlow = combine(
            taskDao.search(clean),
            projectDao.search(clean),
            eventDao.search(clean),
            appointmentDao.search(clean),
            transactionDao.search(clean)
        ) { t, p, e, a, tx ->
            listOf(t, p, e, a, tx)
        }

        val contextFlow = combine(
            creditCardDao.search(clean),
            observeTasksWithDetails(),
            observeProjectsWithProgress(),
            observeEventsWithDetails(),
            observeTransactionsWithDetails()
        ) { cards, tasks, projects, events, txs ->
            listOf(cards, tasks, projects, events, txs)
        }

        val goalsFlow = combine(
            personalGoalDao.search(clean),
            observePersonalGoalsWithDetails(),
            categoryDao.observeAll(),
            appointmentDao.observeAll(),
            combine(taskRecurrenceDao.observeAll(), recurrenceExceptionDao.observeAll(), eventDao.observeAll()) { r, ex, ev ->
                Triple(r, ex, ev)
            }
        ) { matchedGoals, allGoals, categories, allAppointments, (allRecurrences, allExceptions, allRawEvents) ->
            listOf(matchedGoals, allGoals, categories, allAppointments, allRecurrences, allExceptions, allRawEvents)
        }

        return combine(searchFlow, contextFlow, goalsFlow) { searched, ctx, meta ->
            @Suppress("UNCHECKED_CAST")
            val matchedTaskIds = (searched[0] as List<TaskEntity>).map { it.id }.toSet()
            @Suppress("UNCHECKED_CAST")
            val matchedProjIds = (searched[1] as List<ProjectEntity>).map { it.id }.toSet()
            @Suppress("UNCHECKED_CAST")
            val matchedEventIds = (searched[2] as List<EventEntity>).map { it.id }.toSet()
            @Suppress("UNCHECKED_CAST")
            val matchedAppointments = searched[3] as List<AppointmentEntity>
            @Suppress("UNCHECKED_CAST")
            val matchedTxIds = (searched[4] as List<TransactionEntity>).map { it.id }.toSet()

            @Suppress("UNCHECKED_CAST")
            val matchedCards = ctx[0] as List<CreditCardEntity>
            @Suppress("UNCHECKED_CAST")
            val allTasks = ctx[1] as List<TaskWithDetails>
            @Suppress("UNCHECKED_CAST")
            val allProjects = ctx[2] as List<ProjectWithProgress>
            @Suppress("UNCHECKED_CAST")
            val allEvents = ctx[3] as List<EventWithDetails>
            @Suppress("UNCHECKED_CAST")
            val allTx = ctx[4] as List<TransactionWithDetails>

            @Suppress("UNCHECKED_CAST")
            val matchedGoals = meta[0] as List<PersonalGoalEntity>
            @Suppress("UNCHECKED_CAST")
            val allGoals = meta[1] as List<PersonalGoalWithDetails>
            @Suppress("UNCHECKED_CAST")
            val categories = meta[2] as List<CategoryEntity>
            @Suppress("UNCHECKED_CAST")
            val allAppointments = meta[3] as List<AppointmentEntity>
            @Suppress("UNCHECKED_CAST")
            val allRecurrences = meta[4] as List<TaskRecurrenceEntity>
            @Suppress("UNCHECKED_CAST")
            val allExceptions = meta[5] as List<RecurrenceExceptionEntity>
            @Suppress("UNCHECKED_CAST")
            val allRawEvents = meta[6] as List<EventEntity>
            val matchedGoalIds = matchedGoals.map { it.id }.toSet()

            val today = DateTimeUtils.todayLocalDate()
            val searchWindow = DateRange(today, today.plusDays(60))
            val categoryMap = categories.associateBy { it.id }
            val rawEventMap = allRawEvents.associateBy { it.id }
            val conflictMap = AgendaEngine.computeAppointmentConflicts(
                appointments = allAppointments,
                taskRecurrences = allRecurrences,
                recurrenceExceptions = allExceptions,
                window = searchWindow
            )
            val cleanLower = clean.lowercase()

            val virtualApptMatches = RecurrenceEngine.projectVirtualAppointments(
                window = searchWindow,
                recurrences = allRecurrences,
                exceptions = allExceptions,
                materializedAppointments = allAppointments
            ).filter { proj ->
                proj.effectiveTitle.lowercase().contains(cleanLower) ||
                    proj.effectiveDescription.lowercase().contains(cleanLower) ||
                    proj.effectiveLocation.lowercase().contains(cleanLower)
            }.map { proj ->
                val vAppt = proj.toVirtualAppointmentEntity()
                AgendaItem.AppointmentItem(
                    appointment = vAppt,
                    category = proj.recurrence.categoryId?.let { categoryMap[it] },
                    conflictTitles = conflictMap[AgendaEngine.virtualAppointmentConflictKey(proj.recurrence.id, proj.occurrenceDateIso)].orEmpty(),
                    date = DateTimeUtils.epochToLocalDate(proj.startAt),
                    event = proj.recurrence.eventId?.let { rawEventMap[it] },
                    recurrenceId = proj.recurrence.id,
                    occurrenceDateIso = proj.occurrenceDateIso,
                    isVirtualRecurrence = true
                )
            }

            val virtualEventMatches = RecurrenceEngine.projectVirtualEvents(
                window = searchWindow,
                recurrences = allRecurrences,
                exceptions = allExceptions,
                materializedEvents = allRawEvents
            ).filter { proj ->
                proj.effectiveTitle.lowercase().contains(cleanLower) ||
                    proj.effectiveDescription.lowercase().contains(cleanLower) ||
                    proj.effectiveLocationName.lowercase().contains(cleanLower) ||
                    proj.effectiveCity.lowercase().contains(cleanLower)
            }.map { proj ->
                EventWithDetails(
                    event = proj.toVirtualEventEntity(),
                    category = proj.recurrence.categoryId?.let { categoryMap[it] },
                    creditCard = null,
                    linkedTasks = emptyList(),
                    linkedTransactions = emptyList()
                )
            }

            val materializedApptItems = matchedAppointments.map { appt ->
                AgendaItem.AppointmentItem(
                    appointment = appt,
                    category = appt.categoryId?.let { categoryMap[it] },
                    conflictTitles = conflictMap[appt.id].orEmpty(),
                    date = DateTimeUtils.epochToLocalDate(appt.startAt),
                    event = appt.eventId?.let { rawEventMap[it] },
                    recurrenceId = appt.recurrenceId,
                    occurrenceDateIso = appt.occurrenceDateIso,
                    isVirtualRecurrence = false
                )
            }

            GlobalSearchResults(
                tasks = allTasks.filter { it.task.id in matchedTaskIds },
                projects = allProjects.filter { it.project.id in matchedProjIds },
                events = (allEvents.filter { it.event.id in matchedEventIds } + virtualEventMatches)
                    .sortedBy { it.event.startAt },
                appointments = (materializedApptItems + virtualApptMatches)
                    .sortedBy { it.appointment.startAt },
                transactions = allTx.filter { it.transaction.id in matchedTxIds },
                creditCards = matchedCards,
                personalGoals = allGoals.filter { it.goal.id in matchedGoalIds }
            )
        }
    }

    fun observeDatabaseTableCounts(): Flow<DatabaseTableCounts> {
        val part1 = combine(
            listOf(
                settingsDao.observeCount(),
                categoryDao.observeCount(),
                projectDao.observeCount(),
                eventDao.observeCount(),
                taskDao.observeCount(),
                subtaskDao.observeCount(),
                appointmentDao.observeCount(),
                creditCardDao.observeCount(),
                statementDao.observeCount(),
                installmentPurchaseDao.observeCount()
            )
        ) { it }

        val part2 = combine(
            listOf(
                recurringRuleDao.observeCount(),
                transactionDao.observeCount(),
                loanDao.observeCount(),
                budgetDao.observeCount(),
                financialGoalDao.observeCount(),
                auditHistoryDao.observeCount(),
                errorLogDao.observeCount(),
                reminderDao.observeCount(),
                routineSettingsDao.observeCount(),
                briefingHistoryDao.observeCount()
            )
        ) { it }

        val part3 = combine(
            listOf(
                personalizationSettingsDao.observeCount(),
                quoteDao.observeCount(),
                personalGoalDao.observeCount(),
                goalMilestoneDao.observeCount(),
                goalProgressLogDao.observeCount(),
                taskRecurrenceDao.observeCount(),
                recurrenceExceptionDao.observeCount()
            )
        ) { it }

        return combine(part1, part2, part3) { a, b, c ->
            DatabaseTableCounts(
                settings = a[0],
                categories = a[1],
                projects = a[2],
                events = a[3],
                tasks = a[4],
                subtasks = a[5],
                appointments = a[6],
                creditCards = a[7],
                statements = a[8],
                installmentPurchases = a[9],
                recurringRules = b[0],
                transactions = b[1],
                loans = b[2],
                budgets = b[3],
                financialGoals = b[4],
                auditHistory = b[5],
                errorLogs = b[6],
                reminders = b[7],
                routineSettings = b[8],
                briefingHistory = b[9],
                personalizationSettings = c[0],
                quotes = c[1],
                personalGoals = c[2],
                goalMilestones = c[3],
                goalProgressLogs = c[4],
                recurrences = c[5],
                recurrenceExceptions = c[6]
            )
        }
    }

    fun observeLatestErrorLogs(): Flow<List<ErrorLogEntity>> = errorLogDao.observeLatest50()

    // --- Assistente de Rotina e Notificacoes (Fase 4) ---

    fun observeRoutineSettings(): Flow<RoutineAssistantSettingsEntity> =
        routineSettingsDao.observeSettings().combine(flowOf(Unit)) { entity, _ ->
            entity ?: defaultRoutineSettings(DateTimeUtils.nowEpochMillis())
        }

    fun observeReminders(): Flow<List<ReminderEntity>> = reminderDao.observeAll()

    fun observeBriefingHistory(): Flow<List<BriefingHistoryEntity>> = briefingHistoryDao.observeLatest()

    fun observeDailyBriefingSnapshot(): Flow<DailyBriefingSnapshot> =
        combine(
            observeUnifiedAgenda(),
            observeTasksWithDetails(),
            observeEventsWithDetails(),
            observeTransactionsWithDetails(),
            combine(observeCardsWithStatements(), observeBriefingHistory()) { c, h -> c to h }
        ) { agenda, tasks, events, transactions, (cards, history) ->
            val today = DateTimeUtils.todayLocalDate()
            val todayIso = today.toString()
            val now = DateTimeUtils.nowEpochMillis()

            val todayAppointments = agenda
                .filterIsInstance<AgendaItem.AppointmentItem>()
                .filter { it.date == today }

            val todayTasks = tasks.filter { details ->
                !details.task.status.isTerminal &&
                    details.task.dueAt != null &&
                    DateTimeUtils.epochToLocalDate(details.task.dueAt) == today
            }
            val overdueTasks = tasks.filter { isTaskOverdue(it.task, now, today) }

            val materializedTodayEvents = events.filter { ew ->
                ew.event.eventStatus != EventLifecycleStatus.CANCELLED &&
                    !DateTimeUtils.epochToLocalDate(ew.event.startAt).isAfter(today) &&
                    !DateTimeUtils.epochToLocalDate(ew.event.endAt).isBefore(today)
            }
            val virtualTodayEvents = agenda
                .filterIsInstance<AgendaItem.EventItem>()
                .filter {
                    it.isVirtualRecurrence &&
                        it.event.eventStatus != EventLifecycleStatus.CANCELLED &&
                        !it.spanStartDate.isAfter(today) &&
                        !it.spanEndDate.isBefore(today)
                }
                .distinctBy { it.event.id }
                .map { item ->
                    EventWithDetails(
                        event = item.event,
                        category = item.category,
                        creditCard = null,
                        linkedTasks = emptyList(),
                        linkedTransactions = emptyList()
                    )
                }
            val todayEvents = (materializedTodayEvents + virtualTodayEvents).sortedBy { it.event.startAt }

            val billsDueToday = transactions.filter { tw ->
                val tx = tw.transaction
                AgendaEngine.isDirectAgendaBill(tx) &&
                    DateTimeUtils.epochToLocalDate(tx.dueAt!!) == today
            }
            val overdueBills = transactions.filter { tw ->
                val tx = tw.transaction
                AgendaEngine.isDirectAgendaBill(tx) && tx.status == FinancialStatus.OVERDUE
            }
            val statementsDue = cards.flatMap { it.statements }
                .filter { sw ->
                    sw.statement.status != StatementStatus.PAID &&
                        sw.totalCents > 0L &&
                        !DateTimeUtils.epochToLocalDate(sw.statement.dueAt).isAfter(today)
                }

            val reviewedToday = history.any {
                it.briefingType == BriefingType.MORNING && it.dateIso == todayIso && it.reviewedAt != null
            }

            DailyBriefingSnapshot(
                dateIso = todayIso,
                todayAppointments = todayAppointments,
                todayTasks = todayTasks,
                overdueTasks = overdueTasks,
                todayEvents = todayEvents,
                billsDueToday = billsDueToday,
                overdueBills = overdueBills,
                statementsDueTodayOrOverdue = statementsDue,
                isReviewedToday = reviewedToday
            )
        }

    fun observeNightReviewSnapshot(): Flow<NightReviewSnapshot> =
        combine(
            observeUnifiedAgenda(),
            observeTasksWithDetails(),
            observeEventsWithDetails(),
            observeTransactionsWithDetails(),
            observeBriefingHistory()
        ) { agenda, tasks, events, transactions, history ->
            val today = DateTimeUtils.todayLocalDate()
            val tomorrow = today.plusDays(1)
            val todayIso = today.toString()
            val now = DateTimeUtils.nowEpochMillis()

            val completedToday = tasks.filter { details ->
                details.task.status == TaskStatus.COMPLETED &&
                    details.task.completedAt?.let { DateTimeUtils.epochToLocalDate(it) == today } == true
            }

            val pendingTodayOrOverdue = tasks.filter { details ->
                val t = details.task
                !t.status.isTerminal && (
                    isTaskOverdue(t, now, today) ||
                        (t.dueAt != null && DateTimeUtils.epochToLocalDate(t.dueAt) == today)
                    )
            }

            val pendingBillsTodayOrOverdue = transactions.filter { tw ->
                val tx = tw.transaction
                AgendaEngine.isDirectAgendaBill(tx) &&
                    !DateTimeUtils.epochToLocalDate(tx.dueAt!!).isAfter(today)
            }

            val tomorrowAppointments = agenda
                .filterIsInstance<AgendaItem.AppointmentItem>()
                .filter { it.date == tomorrow }

            val tomorrowTasks = tasks.filter { details ->
                !details.task.status.isTerminal &&
                    details.task.dueAt != null &&
                    DateTimeUtils.epochToLocalDate(details.task.dueAt) == tomorrow
            }

            val materializedTomorrowEvents = events.filter { ew ->
                ew.event.eventStatus != EventLifecycleStatus.CANCELLED &&
                    !DateTimeUtils.epochToLocalDate(ew.event.startAt).isAfter(tomorrow) &&
                    !DateTimeUtils.epochToLocalDate(ew.event.endAt).isBefore(tomorrow)
            }
            val virtualTomorrowEvents = agenda
                .filterIsInstance<AgendaItem.EventItem>()
                .filter {
                    it.isVirtualRecurrence &&
                        it.event.eventStatus != EventLifecycleStatus.CANCELLED &&
                        !it.spanStartDate.isAfter(tomorrow) &&
                        !it.spanEndDate.isBefore(tomorrow)
                }
                .distinctBy { it.event.id }
                .map { item ->
                    EventWithDetails(
                        event = item.event,
                        category = item.category,
                        creditCard = null,
                        linkedTasks = emptyList(),
                        linkedTransactions = emptyList()
                    )
                }
            val tomorrowEvents = (materializedTomorrowEvents + virtualTomorrowEvents).sortedBy { it.event.startAt }

            val tomorrowBills = transactions.filter { tw ->
                val tx = tw.transaction
                AgendaEngine.isDirectAgendaBill(tx) &&
                    DateTimeUtils.epochToLocalDate(tx.dueAt!!) == tomorrow
            }

            val reviewedTonight = history.any {
                it.briefingType == BriefingType.NIGHT_REVIEW && it.dateIso == todayIso && it.reviewedAt != null
            }

            NightReviewSnapshot(
                dateIso = todayIso,
                completedTasksToday = completedToday,
                pendingTasksTodayOrOverdue = pendingTodayOrOverdue,
                pendingBillsTodayOrOverdue = pendingBillsTodayOrOverdue,
                tomorrowAppointments = tomorrowAppointments,
                tomorrowTasks = tomorrowTasks,
                tomorrowEvents = tomorrowEvents,
                tomorrowBills = tomorrowBills,
                isReviewedTonight = reviewedTonight
            )
        }

    // --- Configuracoes e Categorias ---

    suspend fun saveSettings(
        userName: String,
        darkTheme: Boolean,
        declaredBalanceCents: Long? = null
    ): Result<Unit> = safeDbCall("Configurações") {
        val now = DateTimeUtils.nowEpochMillis()
        val existing = settingsDao.getSettings()
        settingsDao.upsert(
            SettingsEntity(
                id = SettingsEntity.SINGLETON_ID,
                userName = userName.trim().ifEmpty { "Enzo" },
                darkTheme = darkTheme,
                declaredBalanceCents = declaredBalanceCents ?: existing?.declaredBalanceCents ?: 0L,
                createdAt = existing?.createdAt ?: now,
                updatedAt = now
            )
        )
        notifyWidgetsChanged()
    }

    suspend fun updateDeclaredBalance(balanceCents: Long): Result<Unit> =
        safeDbCall("Financeiro (Saldo)") {
            val now = DateTimeUtils.nowEpochMillis()
            val existing = settingsDao.getSettings()
            database.withTransaction {
                settingsDao.upsert(
                    SettingsEntity(
                        id = SettingsEntity.SINGLETON_ID,
                        userName = existing?.userName ?: "Enzo",
                        darkTheme = existing?.darkTheme ?: true,
                        declaredBalanceCents = balanceCents,
                        createdAt = existing?.createdAt ?: now,
                        updatedAt = now
                    )
                )
                recordAudit("Settings", SettingsEntity.SINGLETON_ID, "Saldo atualizado", "Saldo informado ajustado para ${DateTimeUtils.formatBrl(balanceCents)}")
            }
            notifyWidgetsChanged()
        }

    suspend fun saveCategory(
        id: String? = null,
        name: String,
        colorHex: String,
        scope: CategoryScope = CategoryScope.GENERAL
    ): Result<Unit> = safeDbCall("Categorias") {
        val cleanName = name.trim()
        require(cleanName.isNotEmpty()) { "O nome da categoria é obrigatório." }
        val now = DateTimeUtils.nowEpochMillis()
        val existing = id?.let { categoryDao.getById(it) }
        categoryDao.upsert(
            CategoryEntity(
                id = existing?.id ?: UUID.randomUUID().toString(),
                name = cleanName,
                colorHex = colorHex.ifBlank { "#9A9A9A" },
                scope = scope,
                isDefault = existing?.isDefault ?: false,
                createdAt = existing?.createdAt ?: now,
                updatedAt = now
            )
        )
    }

    suspend fun deleteCategory(categoryId: String): Result<Unit> = safeDbCall("Categorias") {
        categoryDao.deleteById(categoryId)
    }

    // --- Projetos e Tarefas ---

    suspend fun saveProject(
        id: String? = null,
        name: String,
        description: String,
        deadlineAt: Long?,
        status: ProjectStatus,
        priority: TaskPriority,
        goalId: String? = null
    ): Result<String> = safeDbCall("Projetos") {
        val cleanName = name.trim()
        require(cleanName.isNotEmpty()) { "O nome do projeto é obrigatório." }
        val now = DateTimeUtils.nowEpochMillis()
        val existing = id?.let { projectDao.getById(it) }
        val projectId = existing?.id ?: UUID.randomUUID().toString()
        projectDao.upsert(
            ProjectEntity(
                id = projectId,
                name = cleanName,
                description = description.trim(),
                deadlineAt = deadlineAt,
                status = status,
                priority = priority,
                goalId = goalId,
                createdAt = existing?.createdAt ?: now,
                updatedAt = now
            )
        )
        notifyWidgetsChanged()
        projectId
    }

    suspend fun countTasksInProject(projectId: String): Int = withContext(Dispatchers.IO) {
        taskDao.countByProject(projectId)
    }

    suspend fun deleteProjectAndUnlinkTasks(projectId: String): Result<Unit> =
        safeDbCall("Projetos") {
            val now = DateTimeUtils.nowEpochMillis()
            database.withTransaction {
                taskDao.unlinkProject(projectId, now)
                transactionDao.unlinkProject(projectId, now)
                projectDao.deleteById(projectId)
            }
            notifyWidgetsChanged()
        }

    suspend fun saveTask(
        id: String? = null,
        title: String,
        description: String = "",
        projectId: String? = null,
        categoryId: String? = null,
        eventId: String? = null,
        goalId: String? = null,
        priority: TaskPriority = TaskPriority.NORMAL,
        status: TaskStatus = TaskStatus.NOT_STARTED,
        dueAt: Long? = null,
        hasDueTime: Boolean = false,
        notes: String = "",
        subtaskDrafts: List<Triple<String?, String, Boolean>>? = null,
        reminderMinutesBefore: Int? = null,
        recurrenceDraft: TaskRecurrenceDraft? = null,
        recurringEditScope: RecurringEditScope = RecurringEditScope.THIS_OCCURRENCE_ONLY
    ): Result<String> = safeDbCall("Tarefas") {
        val cleanTitle = title.trim()
        require(cleanTitle.isNotEmpty()) { "O título da tarefa é obrigatório." }
        val now = DateTimeUtils.nowEpochMillis()
        val existing = id?.let { taskDao.getById(it) }
        val taskId = existing?.id ?: UUID.randomUUID().toString()

        val resolvedCompletedAt = when {
            status == TaskStatus.COMPLETED -> existing?.completedAt ?: now
            else -> null
        }

        val effectiveHasDueTime = dueAt != null && hasDueTime
        val dueMinutesOfDay = if (effectiveHasDueTime && dueAt != null) {
            RecurrenceEngine.minutesOfDayFromTime(DateTimeUtils.epochToLocalTime(dueAt))
        } else {
            null
        }

        val existingRecurrence = existing?.recurrenceId?.let { taskRecurrenceDao.getById(it) }
        var finalRecurrenceId: String? = existing?.recurrenceId
        var finalOccurrenceDateIso: String? = existing?.occurrenceDateIso
        var finalDueAt: Long? = dueAt
        var finalHasDueTime: Boolean = effectiveHasDueTime
        var seriesTemplateToUpsert: TaskRecurrenceEntity? = null
        var exceptionToUpsert: RecurrenceExceptionEntity? = null
        var tasksToDeleteIds: List<String> = emptyList()
        var recurrenceToDeleteId: String? = null
        var unlinkRecurrenceId: String? = null
        var propagateSeriesTasks: List<TaskEntity> = emptyList()
        var recurrenceReminderMinutes: Int? = reminderMinutesBefore

        if (existing == null || existingRecurrence == null) {
            if (recurrenceDraft != null) {
                val newRecId = UUID.randomUUID().toString()
                val startDateIso = recurrenceDraft.startDateIso.ifBlank {
                    dueAt?.let { DateTimeUtils.epochToLocalDate(it).toString() }
                        ?: DateTimeUtils.todayLocalDate().toString()
                }
                val startDate = RecurrenceEngine.parseLocalDateOrNull(startDateIso)
                    ?: DateTimeUtils.todayLocalDate()
                val initialTemplateTitles = if (subtaskDrafts != null) {
                    subtaskDrafts.mapNotNull { (_, subTitle, _) -> subTitle.trim().takeIf(String::isNotBlank) }
                } else if (existing != null) {
                    subtaskDao.getByTaskId(existing.id).sortedBy { it.sortOrder }.map { it.title }
                } else {
                    emptyList()
                }
                val newRec = TaskRecurrenceEntity(
                    id = newRecId,
                    title = cleanTitle,
                    description = description.trim(),
                    projectId = projectId,
                    categoryId = categoryId,
                    eventId = eventId,
                    goalId = goalId,
                    priority = priority,
                    notes = notes.trim(),
                    hasDueTime = effectiveHasDueTime,
                    dueTimeMinutesOfDay = dueMinutesOfDay,
                    reminderMinutesBefore = reminderMinutesBefore,
                    frequency = recurrenceDraft.frequency,
                    customIntervalDays = recurrenceDraft.customIntervalDays.coerceAtLeast(1),
                    weekdaysMask = recurrenceDraft.weekdaysMask,
                    startDateIso = startDate.toString(),
                    endDateIso = recurrenceDraft.endDateIso?.takeIf { it.isNotBlank() },
                    maxOccurrences = recurrenceDraft.maxOccurrences?.takeIf { it > 0 },
                    isActive = true,
                    domain = RecurrenceDomain.TASK,
                    templateSubtasksJson = RecurrenceEngine.serializeChecklistTemplate(initialTemplateTitles),
                    createdAt = now,
                    updatedAt = now
                )
                val firstScheduledDate = RecurrenceEngine.generateScheduledDates(
                    recurrence = newRec,
                    upToDateInclusive = startDate.plusDays(45),
                    hardCap = 1
                ).firstOrNull() ?: startDate
                val userDueDate = dueAt?.let { DateTimeUtils.epochToLocalDate(it) }
                finalOccurrenceDateIso = firstScheduledDate.toString()
                finalDueAt = if (userDueDate == firstScheduledDate && dueAt != null) {
                    dueAt
                } else {
                    RecurrenceEngine.computeDueEpoch(firstScheduledDate, effectiveHasDueTime, dueMinutesOfDay)
                }
                finalHasDueTime = effectiveHasDueTime
                finalRecurrenceId = newRecId
                seriesTemplateToUpsert = newRec
                recurrenceReminderMinutes = newRec.reminderMinutesBefore
            } else {
                finalRecurrenceId = null
                finalOccurrenceDateIso = null
            }
        } else {
            recurrenceReminderMinutes = existingRecurrence.reminderMinutesBefore
            when (recurringEditScope) {
                RecurringEditScope.THIS_OCCURRENCE_ONLY -> {
                    val occIso = existing.occurrenceDateIso
                        ?: existing.dueAt?.let { DateTimeUtils.epochToLocalDate(it).toString() }
                    finalRecurrenceId = existing.recurrenceId
                    finalOccurrenceDateIso = occIso
                    if (occIso != null && (cleanTitle != existingRecurrence.title || dueAt != existing.dueAt || description.trim() != existingRecurrence.description)) {
                        val existingEx = recurrenceExceptionDao.getByRecurrenceAndDate(existingRecurrence.id, occIso)
                        exceptionToUpsert = RecurrenceExceptionEntity(
                            id = existingEx?.id ?: UUID.randomUUID().toString(),
                            recurrenceId = existingRecurrence.id,
                            occurrenceDateIso = occIso,
                            exceptionType = RecurrenceExceptionType.OVERRIDE,
                            overrideTitle = cleanTitle,
                            overrideDueAt = dueAt,
                            overrideDescription = description.trim(),
                            taskId = taskId,
                            createdAt = existingEx?.createdAt ?: now,
                            updatedAt = now
                        )
                    }
                }
                RecurringEditScope.ENTIRE_SERIES -> {
                    if (recurrenceDraft == null) {
                        val otherTasks = taskDao.getByRecurrenceId(existingRecurrence.id)
                            .filter { it.id != taskId && it.status != TaskStatus.COMPLETED }
                        tasksToDeleteIds = otherTasks.map { it.id }
                        unlinkRecurrenceId = existingRecurrence.id
                        recurrenceToDeleteId = existingRecurrence.id
                        finalRecurrenceId = null
                        finalOccurrenceDateIso = null
                    } else {
                        val resolvedReminderPref = reminderMinutesBefore ?: existingRecurrence.reminderMinutesBefore
                        val resolvedTemplateTitles = if (subtaskDrafts != null) {
                            subtaskDrafts.mapNotNull { (_, subTitle, _) -> subTitle.trim().takeIf(String::isNotBlank) }
                        } else {
                            RecurrenceEngine.deserializeChecklistTemplate(existingRecurrence.templateSubtasksJson)
                        }
                        val updatedRec = existingRecurrence.copy(
                            title = cleanTitle,
                            description = description.trim(),
                            projectId = projectId,
                            categoryId = categoryId,
                            eventId = eventId,
                            goalId = goalId,
                            priority = priority,
                            notes = notes.trim(),
                            hasDueTime = effectiveHasDueTime,
                            dueTimeMinutesOfDay = dueMinutesOfDay,
                            reminderMinutesBefore = resolvedReminderPref,
                            frequency = recurrenceDraft.frequency,
                            customIntervalDays = recurrenceDraft.customIntervalDays.coerceAtLeast(1),
                            weekdaysMask = recurrenceDraft.weekdaysMask,
                            startDateIso = recurrenceDraft.startDateIso.ifBlank { existingRecurrence.startDateIso },
                            endDateIso = recurrenceDraft.endDateIso?.takeIf { it.isNotBlank() },
                            maxOccurrences = recurrenceDraft.maxOccurrences?.takeIf { it > 0 },
                            templateSubtasksJson = RecurrenceEngine.serializeChecklistTemplate(resolvedTemplateTitles),
                            updatedAt = now
                        )
                        seriesTemplateToUpsert = updatedRec
                        recurrenceReminderMinutes = resolvedReminderPref
                        finalRecurrenceId = updatedRec.id
                        finalOccurrenceDateIso = existing.occurrenceDateIso
                            ?: dueAt?.let { DateTimeUtils.epochToLocalDate(it).toString() }

                        propagateSeriesTasks = taskDao.getByRecurrenceId(existingRecurrence.id)
                            .filter { it.id != taskId && !it.status.isTerminal }
                            .map { linked ->
                                linked.copy(
                                    title = cleanTitle,
                                    description = description.trim(),
                                    projectId = projectId,
                                    categoryId = categoryId,
                                    eventId = eventId,
                                    goalId = goalId,
                                    priority = priority,
                                    notes = notes.trim(),
                                    updatedAt = now
                                )
                            }
                    }
                }
            }
        }

        val savedTask = TaskEntity(
            id = taskId,
            title = cleanTitle,
            description = description.trim(),
            projectId = projectId,
            categoryId = categoryId,
            eventId = eventId,
            goalId = goalId,
            priority = priority,
            status = status,
            dueAt = finalDueAt,
            hasDueTime = finalDueAt != null && finalHasDueTime,
            notes = notes.trim(),
            completedAt = resolvedCompletedAt,
            recurrenceId = finalRecurrenceId,
            occurrenceDateIso = finalOccurrenceDateIso,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now
        )

        tasksToDeleteIds.forEach { delId ->
            cancelAndDeleteRemindersForSource(ReminderSourceType.TASK, delId)
        }

        var materializedNext: Pair<TaskEntity, Int?>? = null
        database.withTransaction {
            if (unlinkRecurrenceId != null) {
                tasksToDeleteIds.forEach { delId ->
                    transactionDao.unlinkTask(delId, now)
                    taskDao.deleteById(delId)
                }
                recurrenceExceptionDao.deleteByRecurrenceId(unlinkRecurrenceId)
                taskDao.unlinkRecurrence(unlinkRecurrenceId, now)
                recurrenceToDeleteId?.let { taskRecurrenceDao.deleteById(it) }
            }
            seriesTemplateToUpsert?.let { taskRecurrenceDao.upsert(it) }
            taskDao.upsert(savedTask)
            propagateSeriesTasks.forEach { taskDao.update(it) }
            exceptionToUpsert?.let { recurrenceExceptionDao.upsert(it) }

            if (subtaskDrafts != null) {
                val existingSubtasks = subtaskDao.getByTaskId(taskId).associateBy { it.id }
                val newEntities = subtaskDrafts.mapIndexedNotNull { index, (subId, subTitle, isDone) ->
                    val cleanSub = subTitle.trim()
                    if (cleanSub.isEmpty()) return@mapIndexedNotNull null
                    val old = subId?.let { existingSubtasks[it] }
                    SubtaskEntity(
                        id = old?.id ?: UUID.randomUUID().toString(),
                        taskId = taskId,
                        title = cleanSub,
                        isCompleted = isDone,
                        sortOrder = index,
                        createdAt = old?.createdAt ?: now,
                        updatedAt = now
                    )
                }
                subtaskDao.deleteByTaskId(taskId)
                if (newEntities.isNotEmpty()) {
                    subtaskDao.insertAll(newEntities)
                }
            }

            if (savedTask.status == TaskStatus.COMPLETED && existing?.status != TaskStatus.COMPLETED) {
                materializedNext = materializeNextOccurrenceIfNeededInternal(savedTask, now)
            }
        }
        syncTaskReminder(savedTask, reminderMinutesBefore)
        materializedNext?.let { (nextTask, recRemMins) ->
            syncTaskReminder(nextTask, overrideMinutesBefore = recRemMins)
        }
        notifyWidgetsChanged()
        taskId
    }

    suspend fun toggleTaskCompletion(taskId: String): Result<TaskStatus> =
        safeDbCall("Tarefas") {
            val existing = taskDao.getById(taskId)
                ?: throw IllegalStateException("Tarefa não encontrada.")
            val now = DateTimeUtils.nowEpochMillis()
            val nextStatus = if (existing.status == TaskStatus.COMPLETED) {
                TaskStatus.IN_PROGRESS
            } else {
                TaskStatus.COMPLETED
            }
            val nextCompletedAt = if (nextStatus == TaskStatus.COMPLETED) now else null
            val updated = existing.copy(
                status = nextStatus,
                completedAt = nextCompletedAt,
                updatedAt = now
            )
            var materializedNext: Pair<TaskEntity, Int?>? = null
            database.withTransaction {
                taskDao.update(updated)
                if (nextStatus == TaskStatus.COMPLETED) {
                    materializedNext = materializeNextOccurrenceIfNeededInternal(updated, now)
                }
            }
            syncTaskReminder(updated, overrideMinutesBefore = null)
            materializedNext?.let { (nextTask, recRemMins) ->
                syncTaskReminder(nextTask, overrideMinutesBefore = recRemMins)
            }
            notifyWidgetsChanged()
            nextStatus
        }

    suspend fun updateTaskStatus(taskId: String, status: TaskStatus): Result<Unit> =
        safeDbCall("Tarefas") {
            val existing = taskDao.getById(taskId)
                ?: throw IllegalStateException("Tarefa não encontrada.")
            val now = DateTimeUtils.nowEpochMillis()
            val completedAt = if (status == TaskStatus.COMPLETED) existing.completedAt ?: now else null
            val updated = existing.copy(
                status = status,
                completedAt = completedAt,
                updatedAt = now
            )
            var materializedNext: Pair<TaskEntity, Int?>? = null
            database.withTransaction {
                taskDao.update(updated)
                if (status == TaskStatus.COMPLETED && existing.status != TaskStatus.COMPLETED) {
                    materializedNext = materializeNextOccurrenceIfNeededInternal(updated, now)
                }
            }
            syncTaskReminder(updated, overrideMinutesBefore = null)
            materializedNext?.let { (nextTask, recRemMins) ->
                syncTaskReminder(nextTask, overrideMinutesBefore = recRemMins)
            }
            notifyWidgetsChanged()
        }

    suspend fun toggleSubtask(subtask: SubtaskEntity): Result<Unit> =
        safeDbCall("Subtarefas") {
            subtaskDao.upsert(
                subtask.copy(
                    isCompleted = !subtask.isCompleted,
                    updatedAt = DateTimeUtils.nowEpochMillis()
                )
            )
        }

    suspend fun countTransactionsForTask(taskId: String): Int = withContext(Dispatchers.IO) {
        transactionDao.countByTask(taskId)
    }

    suspend fun deleteTask(
        taskId: String,
        scope: RecurringEditScope = RecurringEditScope.THIS_OCCURRENCE_ONLY
    ): Result<Unit> =
        safeDbCall("Tarefas") {
            val now = DateTimeUtils.nowEpochMillis()
            val existing = taskDao.getById(taskId)
            val recId = existing?.recurrenceId

            if (existing != null && recId != null) {
                if (scope == RecurringEditScope.ENTIRE_SERIES) {
                    val seriesTasks = taskDao.getByRecurrenceId(recId)
                    val nonCompletedIds = seriesTasks
                        .filter { it.status != TaskStatus.COMPLETED || it.id == taskId }
                        .map { it.id }
                    nonCompletedIds.forEach { idToCancel ->
                        cancelAndDeleteRemindersForSource(ReminderSourceType.TASK, idToCancel)
                    }
                    database.withTransaction {
                        nonCompletedIds.forEach { idToDelete ->
                            transactionDao.unlinkTask(idToDelete, now)
                        }
                        taskDao.deleteNonCompletedByRecurrence(recId)
                        if (existing.status == TaskStatus.COMPLETED) {
                            taskDao.deleteById(taskId)
                        }
                        recurrenceExceptionDao.deleteByRecurrenceId(recId)
                        taskDao.unlinkRecurrence(recId, now)
                        taskRecurrenceDao.deleteById(recId)
                    }
                } else {
                    cancelAndDeleteRemindersForSource(ReminderSourceType.TASK, taskId)
                    var materializedNext: Pair<TaskEntity, Int?>? = null
                    database.withTransaction {
                        val occIso = existing.occurrenceDateIso
                            ?: existing.dueAt?.let { DateTimeUtils.epochToLocalDate(it).toString() }
                        if (occIso != null) {
                            val existingEx = recurrenceExceptionDao.getByRecurrenceAndDate(recId, occIso)
                            recurrenceExceptionDao.upsert(
                                RecurrenceExceptionEntity(
                                    id = existingEx?.id ?: UUID.randomUUID().toString(),
                                    recurrenceId = recId,
                                    occurrenceDateIso = occIso,
                                    exceptionType = RecurrenceExceptionType.SKIP,
                                    overrideTitle = null,
                                    overrideDueAt = null,
                                    taskId = null,
                                    createdAt = existingEx?.createdAt ?: now,
                                    updatedAt = now
                                )
                            )
                        }
                        transactionDao.unlinkTask(taskId, now)
                        taskDao.deleteById(taskId)
                        if (!existing.status.isTerminal) {
                            val otherActive = taskDao.getByRecurrenceId(recId).any { !it.status.isTerminal }
                            if (!otherActive) {
                                materializedNext = materializeNextOccurrenceIfNeededInternal(
                                    completedOrSkippedTask = existing,
                                    now = now,
                                    requireCompletedStatus = false
                                )
                            }
                        }
                    }
                    materializedNext?.let { (nextTask, recRemMins) ->
                        syncTaskReminder(nextTask, overrideMinutesBefore = recRemMins)
                    }
                }
            } else {
                cancelAndDeleteRemindersForSource(ReminderSourceType.TASK, taskId)
                database.withTransaction {
                    transactionDao.unlinkTask(taskId, now)
                    taskDao.deleteById(taskId)
                }
            }
            notifyWidgetsChanged()
        }

    suspend fun openOrMaterializeRecurringOccurrence(
        recurrenceId: String,
        occurrenceDateIso: String
    ): Result<String> = safeDbCall("Tarefas Recorrentes") {
        val existing = taskDao.getByRecurrenceAndDate(recurrenceId, occurrenceDateIso)
        if (existing != null) return@safeDbCall existing.id

        val recurrence = taskRecurrenceDao.getById(recurrenceId)
            ?: throw IllegalStateException("Série recorrente não encontrada.")
        val occDate = RecurrenceEngine.parseLocalDateOrNull(occurrenceDateIso)
            ?: throw IllegalArgumentException("Data de ocorrência inválida: $occurrenceDateIso")
        val now = DateTimeUtils.nowEpochMillis()
        val exception = recurrenceExceptionDao.getByRecurrenceAndDate(recurrenceId, occurrenceDateIso)
        val effectiveTitle = exception
            ?.takeIf { it.exceptionType == RecurrenceExceptionType.OVERRIDE && !it.overrideTitle.isNullOrBlank() }
            ?.overrideTitle
            ?: recurrence.title
        val effectiveDescription = exception
            ?.takeIf { it.exceptionType == RecurrenceExceptionType.OVERRIDE && it.overrideDescription != null }
            ?.overrideDescription
            ?: recurrence.description
        val effectiveDueAt = exception
            ?.takeIf { it.exceptionType == RecurrenceExceptionType.OVERRIDE && it.overrideDueAt != null }
            ?.overrideDueAt
            ?: RecurrenceEngine.computeDueEpoch(
                date = occDate,
                hasDueTime = recurrence.hasDueTime,
                dueTimeMinutesOfDay = recurrence.dueTimeMinutesOfDay
            )
        val candidate = TaskEntity(
            id = UUID.randomUUID().toString(),
            title = effectiveTitle,
            description = effectiveDescription,
            projectId = recurrence.projectId,
            categoryId = recurrence.categoryId,
            eventId = recurrence.eventId,
            goalId = recurrence.goalId,
            priority = recurrence.priority,
            status = TaskStatus.NOT_STARTED,
            dueAt = effectiveDueAt,
            hasDueTime = recurrence.hasDueTime,
            notes = recurrence.notes,
            completedAt = null,
            recurrenceId = recurrence.id,
            occurrenceDateIso = occurrenceDateIso,
            createdAt = now,
            updatedAt = now
        )
        val templateTitles = RecurrenceEngine.deserializeChecklistTemplate(recurrence.templateSubtasksJson)
        val rowId = database.withTransaction {
            val insertedId = taskDao.insertIgnore(candidate)
            if (insertedId != -1L) {
                if (templateTitles.isNotEmpty()) {
                    val clonedSubs = templateTitles.mapIndexed { index, subTitle ->
                        SubtaskEntity(
                            id = UUID.randomUUID().toString(),
                            taskId = candidate.id,
                            title = subTitle,
                            isCompleted = false,
                            sortOrder = index,
                            createdAt = now,
                            updatedAt = now
                        )
                    }
                    subtaskDao.insertAll(clonedSubs)
                }
                if (exception != null) {
                    recurrenceExceptionDao.upsert(exception.copy(taskId = candidate.id, updatedAt = now))
                }
            }
            insertedId
        }
        val persisted = if (rowId != -1L) {
            candidate
        } else {
            taskDao.getByRecurrenceAndDate(recurrenceId, occurrenceDateIso) ?: candidate
        }
        if (rowId != -1L) {
            syncTaskReminder(persisted, overrideMinutesBefore = recurrence.reminderMinutesBefore)
            notifyWidgetsChanged()
        }
        persisted.id
    }

    private suspend fun materializeNextOccurrenceIfNeededInternal(
        completedOrSkippedTask: TaskEntity,
        now: Long,
        requireCompletedStatus: Boolean = true
    ): Pair<TaskEntity, Int?>? {
        if (requireCompletedStatus && completedOrSkippedTask.status != TaskStatus.COMPLETED) return null
        val recId = completedOrSkippedTask.recurrenceId ?: return null
        val recurrence = taskRecurrenceDao.getById(recId) ?: return null
        if (!recurrence.isActive || recurrence.domain != RecurrenceDomain.TASK) return null

        val exceptions = recurrenceExceptionDao.getByRecurrenceId(recId)
        val existingTasks = taskDao.getByRecurrenceId(recId)
        val anchorDate = RecurrenceEngine.parseLocalDateOrNull(completedOrSkippedTask.occurrenceDateIso)
            ?: completedOrSkippedTask.dueAt?.let { DateTimeUtils.epochToLocalDate(it) }
            ?: DateTimeUtils.todayLocalDate()

        val nextDate = RecurrenceEngine.nextOccurrenceDateAfter(
            recurrence = recurrence,
            afterDateInclusive = anchorDate,
            exceptions = exceptions,
            materializedTasks = existingTasks
        ) ?: return null

        val nextIso = nextDate.toString()
        if (taskDao.getByRecurrenceAndDate(recId, nextIso) != null) return null

        val overrideEx = exceptions.firstOrNull {
            it.occurrenceDateIso == nextIso && it.exceptionType == RecurrenceExceptionType.OVERRIDE
        }
        val nextDueAt = overrideEx?.overrideDueAt ?: RecurrenceEngine.computeDueEpoch(
            date = nextDate,
            hasDueTime = recurrence.hasDueTime,
            dueTimeMinutesOfDay = recurrence.dueTimeMinutesOfDay
        )
        val nextTitle = overrideEx?.overrideTitle?.takeIf { it.isNotBlank() } ?: recurrence.title
        val nextDescription = overrideEx?.overrideDescription ?: recurrence.description

        val nextTask = TaskEntity(
            id = UUID.randomUUID().toString(),
            title = nextTitle,
            description = nextDescription,
            projectId = recurrence.projectId,
            categoryId = recurrence.categoryId,
            eventId = recurrence.eventId,
            goalId = recurrence.goalId,
            priority = recurrence.priority,
            status = TaskStatus.NOT_STARTED,
            dueAt = nextDueAt,
            hasDueTime = recurrence.hasDueTime,
            notes = recurrence.notes,
            completedAt = null,
            recurrenceId = recId,
            occurrenceDateIso = nextIso,
            createdAt = now,
            updatedAt = now
        )

        val rowId = taskDao.insertIgnore(nextTask)
        if (rowId == -1L) return null

        val templateTitles = RecurrenceEngine.deserializeChecklistTemplate(recurrence.templateSubtasksJson)
        if (templateTitles.isNotEmpty()) {
            val clonedSubs = templateTitles.mapIndexed { index, subTitle ->
                SubtaskEntity(
                    id = UUID.randomUUID().toString(),
                    taskId = nextTask.id,
                    title = subTitle,
                    isCompleted = false,
                    sortOrder = index,
                    createdAt = now,
                    updatedAt = now
                )
            }
            subtaskDao.insertAll(clonedSubs)
        }
        if (overrideEx != null) {
            recurrenceExceptionDao.upsert(overrideEx.copy(taskId = nextTask.id, updatedAt = now))
        }
        return nextTask to recurrence.reminderMinutesBefore
    }

    // --- Compromissos ---

    suspend fun saveAppointment(
        id: String? = null,
        title: String,
        description: String = "",
        categoryId: String? = null,
        eventId: String? = null,
        startAt: Long,
        endAt: Long,
        isAllDay: Boolean = false,
        location: String = "",
        link: String = "",
        reminderMinutesBefore: Int? = null,
        recurrenceDraft: TaskRecurrenceDraft? = null,
        recurringEditScope: RecurringEditScope = RecurringEditScope.THIS_OCCURRENCE_ONLY
    ): Result<String> = safeDbCall("Agenda") {
        val cleanTitle = title.trim()
        require(cleanTitle.isNotEmpty()) { "O título do compromisso é obrigatório." }
        val cleanDescription = description.trim()
        val cleanLocation = location.trim()
        val cleanLink = link.trim()
        val normalizedEnd = if (endAt >= startAt) endAt else startAt + 3_600_000L
        val durationMillis = (normalizedEnd - startAt).coerceAtLeast(60_000L)
        val dueMinutesOfDay = if (isAllDay) {
            null
        } else {
            RecurrenceEngine.minutesOfDayFromTime(DateTimeUtils.epochToLocalTime(startAt))
        }
        val now = DateTimeUtils.nowEpochMillis()

        val virtualPair = RecurrenceEngine.parseVirtualOccurrenceId(id)
        val existing = when {
            virtualPair != null -> appointmentDao.getByRecurrenceAndDate(virtualPair.first, virtualPair.second)
            id != null -> appointmentDao.getById(id)
            else -> null
        }
        val appointmentId = existing?.id ?: UUID.randomUUID().toString()
        val existingRecurrenceId = existing?.recurrenceId ?: virtualPair?.first
        val existingOccurrenceIso = existing?.occurrenceDateIso ?: virtualPair?.second
        val existingRecurrence = existingRecurrenceId?.let { taskRecurrenceDao.getById(it) }

        var finalRecurrenceId: String? = existingRecurrenceId
        var finalOccurrenceDateIso: String? = existingOccurrenceIso
        var finalStartAt: Long = startAt
        var finalEndAt: Long = normalizedEnd
        var seriesTemplateToUpsert: TaskRecurrenceEntity? = null
        var exceptionToUpsert: RecurrenceExceptionEntity? = null
        var exceptionToDeleteKey: Pair<String, String>? = null
        var apptsToDeleteIds: List<String> = emptyList()
        var recurrenceToDeleteId: String? = null
        var unlinkRecurrenceId: String? = null
        var propagateSeriesAppointments: List<AppointmentEntity> = emptyList()
        var recurrenceReminderMinutes: Int? = reminderMinutesBefore

        if (existingRecurrence == null) {
            if (recurrenceDraft != null) {
                val newRecId = UUID.randomUUID().toString()
                val startDateIso = recurrenceDraft.startDateIso.ifBlank {
                    DateTimeUtils.toLocalDate(startAt).toString()
                }
                val startDate = RecurrenceEngine.parseLocalDateOrNull(startDateIso)
                    ?: DateTimeUtils.toLocalDate(startAt)
                val newRec = TaskRecurrenceEntity(
                    id = newRecId,
                    title = cleanTitle,
                    description = cleanDescription,
                    categoryId = categoryId,
                    eventId = eventId,
                    priority = TaskPriority.NORMAL,
                    notes = "",
                    hasDueTime = !isAllDay,
                    dueTimeMinutesOfDay = dueMinutesOfDay,
                    reminderMinutesBefore = reminderMinutesBefore,
                    frequency = recurrenceDraft.frequency,
                    customIntervalDays = recurrenceDraft.customIntervalDays.coerceAtLeast(1),
                    weekdaysMask = recurrenceDraft.weekdaysMask,
                    startDateIso = startDate.toString(),
                    endDateIso = recurrenceDraft.endDateIso?.takeIf { it.isNotBlank() },
                    maxOccurrences = recurrenceDraft.maxOccurrences?.takeIf { it > 0 },
                    isActive = true,
                    domain = RecurrenceDomain.APPOINTMENT,
                    isAllDay = isAllDay,
                    durationMillis = durationMillis,
                    location = cleanLocation,
                    link = cleanLink,
                    createdAt = now,
                    updatedAt = now
                )
                val firstScheduledDate = RecurrenceEngine.generateScheduledDates(
                    recurrence = newRec,
                    upToDateInclusive = startDate.plusDays(400),
                    hardCap = 1
                ).firstOrNull() ?: startDate
                val userStartDate = DateTimeUtils.toLocalDate(startAt)
                finalOccurrenceDateIso = firstScheduledDate.toString()
                if (userStartDate == firstScheduledDate) {
                    finalStartAt = startAt
                    finalEndAt = normalizedEnd
                } else {
                    finalStartAt = RecurrenceEngine.computeScheduleStartEpoch(
                        date = firstScheduledDate,
                        isAllDay = isAllDay,
                        dueTimeMinutesOfDay = dueMinutesOfDay
                    )
                    finalEndAt = if (isAllDay) {
                        val spanEndDate = DateTimeUtils.toLocalDate(finalStartAt + durationMillis - 60_000L)
                            .coerceAtLeast(firstScheduledDate)
                        DateTimeUtils.localDateToEndOfDayEpoch(spanEndDate)
                    } else {
                        finalStartAt + durationMillis
                    }
                }
                finalRecurrenceId = newRecId
                seriesTemplateToUpsert = newRec
                recurrenceReminderMinutes = newRec.reminderMinutesBefore
            } else {
                finalRecurrenceId = null
                finalOccurrenceDateIso = null
            }
        } else {
            recurrenceReminderMinutes = existingRecurrence.reminderMinutesBefore
            when (recurringEditScope) {
                RecurringEditScope.THIS_OCCURRENCE_ONLY -> {
                    val occIso = existingOccurrenceIso ?: DateTimeUtils.toLocalDate(startAt).toString()
                    finalRecurrenceId = existingRecurrence.id
                    finalOccurrenceDateIso = occIso
                    val changedVsSeriesOrExisting = cleanTitle != existingRecurrence.title ||
                        cleanDescription != existingRecurrence.description ||
                        cleanLocation != existingRecurrence.location ||
                        cleanLink != existingRecurrence.link ||
                        isAllDay != existingRecurrence.isAllDay ||
                        (existing != null && (startAt != existing.startAt || normalizedEnd != existing.endAt)) ||
                        virtualPair != null
                    if (changedVsSeriesOrExisting) {
                        val existingEx = recurrenceExceptionDao.getByRecurrenceAndDate(existingRecurrence.id, occIso)
                        exceptionToUpsert = RecurrenceExceptionEntity(
                            id = existingEx?.id ?: UUID.randomUUID().toString(),
                            recurrenceId = existingRecurrence.id,
                            occurrenceDateIso = occIso,
                            exceptionType = RecurrenceExceptionType.OVERRIDE,
                            overrideTitle = cleanTitle,
                            overrideDueAt = startAt,
                            overrideEndAt = normalizedEnd,
                            overrideDescription = cleanDescription,
                            overrideLocation = cleanLocation,
                            overrideLink = cleanLink,
                            overrideIsAllDay = isAllDay,
                            appointmentId = appointmentId,
                            createdAt = existingEx?.createdAt ?: now,
                            updatedAt = now
                        )
                    }
                }

                RecurringEditScope.ENTIRE_SERIES -> {
                    if (recurrenceDraft == null) {
                        val otherAppts = appointmentDao.getByRecurrenceId(existingRecurrence.id)
                            .filter { it.id != appointmentId }
                        apptsToDeleteIds = otherAppts.map { it.id }
                        unlinkRecurrenceId = existingRecurrence.id
                        recurrenceToDeleteId = existingRecurrence.id
                        finalRecurrenceId = null
                        finalOccurrenceDateIso = null
                    } else {
                        val resolvedReminderPref = reminderMinutesBefore ?: existingRecurrence.reminderMinutesBefore
                        val updatedRec = existingRecurrence.copy(
                            title = cleanTitle,
                            description = cleanDescription,
                            categoryId = categoryId,
                            eventId = eventId,
                            hasDueTime = !isAllDay,
                            dueTimeMinutesOfDay = dueMinutesOfDay,
                            reminderMinutesBefore = resolvedReminderPref,
                            frequency = recurrenceDraft.frequency,
                            customIntervalDays = recurrenceDraft.customIntervalDays.coerceAtLeast(1),
                            weekdaysMask = recurrenceDraft.weekdaysMask,
                            startDateIso = recurrenceDraft.startDateIso.ifBlank { existingRecurrence.startDateIso },
                            endDateIso = recurrenceDraft.endDateIso?.takeIf { it.isNotBlank() },
                            maxOccurrences = recurrenceDraft.maxOccurrences?.takeIf { it > 0 },
                            isAllDay = isAllDay,
                            durationMillis = durationMillis,
                            location = cleanLocation,
                            link = cleanLink,
                            updatedAt = now
                        )
                        seriesTemplateToUpsert = updatedRec
                        recurrenceReminderMinutes = resolvedReminderPref
                        finalRecurrenceId = updatedRec.id
                        finalOccurrenceDateIso = existingOccurrenceIso ?: DateTimeUtils.toLocalDate(startAt).toString()
                        if (existingOccurrenceIso != null) {
                            exceptionToDeleteKey = updatedRec.id to existingOccurrenceIso
                        }
                        val allExByDate = recurrenceExceptionDao.getByRecurrenceId(existingRecurrence.id)
                            .associateBy { it.occurrenceDateIso }
                        propagateSeriesAppointments = appointmentDao.getByRecurrenceId(existingRecurrence.id)
                            .filter { it.id != appointmentId }
                            .mapNotNull { linked ->
                                val linkedIso = linked.occurrenceDateIso
                                    ?: DateTimeUtils.toLocalDate(linked.startAt).toString()
                                val linkedEx = allExByDate[linkedIso]
                                if (linkedEx?.exceptionType == RecurrenceExceptionType.OVERRIDE) {
                                    return@mapNotNull null
                                }
                                val linkedDate = RecurrenceEngine.parseLocalDateOrNull(linkedIso)
                                    ?: DateTimeUtils.toLocalDate(linked.startAt)
                                val nextLinkedStart = RecurrenceEngine.computeScheduleStartEpoch(
                                    date = linkedDate,
                                    isAllDay = isAllDay,
                                    dueTimeMinutesOfDay = dueMinutesOfDay
                                )
                                val nextLinkedEnd = if (isAllDay) {
                                    val spanEndDate = DateTimeUtils.toLocalDate(nextLinkedStart + durationMillis - 60_000L)
                                        .coerceAtLeast(linkedDate)
                                    DateTimeUtils.localDateToEndOfDayEpoch(spanEndDate)
                                } else {
                                    nextLinkedStart + durationMillis
                                }
                                linked.copy(
                                    title = cleanTitle,
                                    description = cleanDescription,
                                    categoryId = categoryId,
                                    eventId = eventId,
                                    startAt = nextLinkedStart,
                                    endAt = nextLinkedEnd,
                                    isAllDay = isAllDay,
                                    location = cleanLocation,
                                    link = cleanLink,
                                    updatedAt = now
                                )
                            }
                    }
                }
            }
        }

        val appointment = AppointmentEntity(
            id = appointmentId,
            title = cleanTitle,
            description = cleanDescription,
            categoryId = categoryId,
            eventId = eventId,
            startAt = finalStartAt,
            endAt = finalEndAt,
            isAllDay = isAllDay,
            location = cleanLocation,
            link = cleanLink,
            recurrenceId = finalRecurrenceId,
            occurrenceDateIso = finalOccurrenceDateIso,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now
        )

        apptsToDeleteIds.forEach { delId ->
            cancelAndDeleteRemindersForSource(ReminderSourceType.APPOINTMENT, delId)
        }
        if (virtualPair != null) {
            cancelAndDeleteRemindersForSource(
                ReminderSourceType.APPOINTMENT,
                RecurrenceEngine.virtualOccurrenceId(virtualPair.first, virtualPair.second)
            )
        }
        if (recurrenceToDeleteId != null) {
            cancelAndDeleteVirtualRemindersForRecurrence(ReminderSourceType.APPOINTMENT, recurrenceToDeleteId)
        }

        database.withTransaction {
            if (unlinkRecurrenceId != null) {
                apptsToDeleteIds.forEach { delId ->
                    appointmentDao.deleteById(delId)
                }
                recurrenceExceptionDao.deleteByRecurrenceId(unlinkRecurrenceId)
                appointmentDao.unlinkRecurrence(unlinkRecurrenceId, now)
                recurrenceToDeleteId?.let { taskRecurrenceDao.deleteById(it) }
            }
            seriesTemplateToUpsert?.let { taskRecurrenceDao.upsert(it) }
            appointmentDao.upsert(appointment)
            propagateSeriesAppointments.forEach { appointmentDao.update(it) }
            exceptionToDeleteKey?.let { (recId, occIso) ->
                recurrenceExceptionDao.getByRecurrenceAndDate(recId, occIso)
                    ?.takeIf { it.exceptionType == RecurrenceExceptionType.OVERRIDE }
                    ?.let { recurrenceExceptionDao.deleteById(it.id) }
            }
            exceptionToUpsert?.let { recurrenceExceptionDao.upsert(it) }
        }
        syncAppointmentReminder(appointment, reminderMinutesBefore ?: recurrenceReminderMinutes)
        propagateSeriesAppointments.forEach { syncAppointmentReminder(it, overrideMinutesBefore = recurrenceReminderMinutes) }
        syncRecurringScheduleRemindersInternal()
        notifyWidgetsChanged()
        appointmentId
    }

    suspend fun getAppointmentById(id: String): AppointmentEntity? = withContext(Dispatchers.IO) {
        appointmentDao.getById(id)
    }

    suspend fun openOrMaterializeRecurringAppointmentOccurrence(
        recurrenceId: String,
        occurrenceDateIso: String
    ): Result<String> = safeDbCall("Compromissos Recorrentes") {
        val existing = appointmentDao.getByRecurrenceAndDate(recurrenceId, occurrenceDateIso)
        if (existing != null) return@safeDbCall existing.id

        val recurrence = taskRecurrenceDao.getById(recurrenceId)
            ?: throw IllegalStateException("Série recorrente não encontrada.")
        val occDate = RecurrenceEngine.parseLocalDateOrNull(occurrenceDateIso)
            ?: throw IllegalArgumentException("Data de ocorrência inválida: $occurrenceDateIso")
        val now = DateTimeUtils.nowEpochMillis()
        val exception = recurrenceExceptionDao.getByRecurrenceAndDate(recurrenceId, occurrenceDateIso)
        val isOverride = exception?.exceptionType == RecurrenceExceptionType.OVERRIDE
        val effectiveAllDay = if (isOverride && exception?.overrideIsAllDay != null) {
            exception.overrideIsAllDay
        } else {
            recurrence.isAllDay
        }
        val baseDuration = recurrence.durationMillis.coerceAtLeast(60_000L)
        val startAt = if (isOverride && exception?.overrideDueAt != null) {
            exception.overrideDueAt
        } else {
            RecurrenceEngine.computeScheduleStartEpoch(
                date = occDate,
                isAllDay = effectiveAllDay,
                dueTimeMinutesOfDay = recurrence.dueTimeMinutesOfDay
            )
        }
        val endAt = if (isOverride && exception?.overrideEndAt != null) {
            exception.overrideEndAt.coerceAtLeast(startAt + 60_000L)
        } else if (effectiveAllDay) {
            val spanEndDate = DateTimeUtils.toLocalDate(startAt + baseDuration - 60_000L)
                .coerceAtLeast(DateTimeUtils.toLocalDate(startAt))
            DateTimeUtils.localDateToEndOfDayEpoch(spanEndDate)
        } else {
            startAt + baseDuration
        }
        val effectiveTitle = if (isOverride && !exception?.overrideTitle.isNullOrBlank()) {
            exception!!.overrideTitle!!
        } else {
            recurrence.title
        }
        val effectiveDescription = if (isOverride && exception?.overrideDescription != null) {
            exception.overrideDescription
        } else {
            recurrence.description
        }
        val effectiveLocation = if (isOverride && exception?.overrideLocation != null) {
            exception.overrideLocation
        } else {
            recurrence.location
        }
        val effectiveLink = if (isOverride && exception?.overrideLink != null) {
            exception.overrideLink
        } else {
            recurrence.link
        }

        val candidate = AppointmentEntity(
            id = UUID.randomUUID().toString(),
            title = effectiveTitle,
            description = effectiveDescription,
            categoryId = recurrence.categoryId,
            eventId = recurrence.eventId,
            startAt = startAt,
            endAt = endAt,
            isAllDay = effectiveAllDay,
            location = effectiveLocation,
            link = effectiveLink,
            recurrenceId = recurrence.id,
            occurrenceDateIso = occurrenceDateIso,
            createdAt = now,
            updatedAt = now
        )
        val rowId = database.withTransaction {
            val insertedId = appointmentDao.insertIgnore(candidate)
            if (insertedId != -1L && exception != null) {
                recurrenceExceptionDao.upsert(exception.copy(appointmentId = candidate.id, updatedAt = now))
            }
            insertedId
        }
        val persisted = if (rowId != -1L) {
            candidate
        } else {
            appointmentDao.getByRecurrenceAndDate(recurrenceId, occurrenceDateIso) ?: candidate
        }
        if (rowId != -1L) {
            val virtualId = RecurrenceEngine.virtualOccurrenceId(recurrenceId, occurrenceDateIso)
            val virtualReminder = reminderDao.getBySourceAndCategory(
                ReminderSourceType.APPOINTMENT,
                virtualId,
                ReminderCategory.SCHEDULE
            )
            val effectiveRemMinutes = virtualReminder?.minutesBefore?.takeIf { it >= 0 }
                ?: recurrence.reminderMinutesBefore
            cancelAndDeleteRemindersForSource(ReminderSourceType.APPOINTMENT, virtualId)
            if (virtualReminder?.status != ReminderStatus.DISMISSED) {
                syncAppointmentReminder(persisted, overrideMinutesBefore = effectiveRemMinutes)
            }
            notifyWidgetsChanged()
        }
        persisted.id
    }

    suspend fun deleteAppointment(
        appointmentId: String,
        scope: RecurringEditScope = RecurringEditScope.THIS_OCCURRENCE_ONLY
    ): Result<Unit> = safeDbCall("Agenda") {
        val now = DateTimeUtils.nowEpochMillis()
        val virtualPair = RecurrenceEngine.parseVirtualOccurrenceId(appointmentId)
        val existing = if (virtualPair != null) {
            appointmentDao.getByRecurrenceAndDate(virtualPair.first, virtualPair.second)
        } else {
            appointmentDao.getById(appointmentId)
        }
        val recId = existing?.recurrenceId ?: virtualPair?.first

        if (recId != null) {
            if (scope == RecurringEditScope.ENTIRE_SERIES) {
                val seriesAppts = appointmentDao.getByRecurrenceId(recId)
                seriesAppts.forEach { appt ->
                    cancelAndDeleteRemindersForSource(ReminderSourceType.APPOINTMENT, appt.id)
                }
                cancelAndDeleteVirtualRemindersForRecurrence(ReminderSourceType.APPOINTMENT, recId)
                database.withTransaction {
                    appointmentDao.deleteByRecurrenceId(recId)
                    recurrenceExceptionDao.deleteByRecurrenceId(recId)
                    taskRecurrenceDao.deleteById(recId)
                }
            } else {
                val occIso = existing?.occurrenceDateIso
                    ?: virtualPair?.second
                    ?: existing?.let { DateTimeUtils.toLocalDate(it.startAt).toString() }
                if (existing != null) {
                    cancelAndDeleteRemindersForSource(ReminderSourceType.APPOINTMENT, existing.id)
                }
                if (occIso != null) {
                    cancelAndDeleteRemindersForSource(
                        ReminderSourceType.APPOINTMENT,
                        RecurrenceEngine.virtualOccurrenceId(recId, occIso)
                    )
                }
                database.withTransaction {
                    if (occIso != null) {
                        val existingEx = recurrenceExceptionDao.getByRecurrenceAndDate(recId, occIso)
                        recurrenceExceptionDao.upsert(
                            RecurrenceExceptionEntity(
                                id = existingEx?.id ?: UUID.randomUUID().toString(),
                                recurrenceId = recId,
                                occurrenceDateIso = occIso,
                                exceptionType = RecurrenceExceptionType.SKIP,
                                appointmentId = null,
                                createdAt = existingEx?.createdAt ?: now,
                                updatedAt = now
                            )
                        )
                    }
                    existing?.let { appointmentDao.deleteById(it.id) }
                }
            }
        } else if (existing != null) {
            cancelAndDeleteRemindersForSource(ReminderSourceType.APPOINTMENT, existing.id)
            appointmentDao.deleteById(existing.id)
        }
        syncRecurringScheduleRemindersInternal()
        notifyWidgetsChanged()
    }

    // --- Eventos e Integracao Financeira ---

    suspend fun saveEvent(
        id: String? = null,
        name: String,
        description: String = "",
        categoryId: String? = null,
        startAt: Long,
        endAt: Long,
        isAllDay: Boolean = false,
        officialLink: String = "",
        ticketLink: String = "",
        locationName: String = "",
        address: String = "",
        city: String = "",
        state: String = "",
        notes: String = "",
        eventStatus: EventLifecycleStatus = EventLifecycleStatus.PLANNED,
        ticketStatus: TicketStatus = TicketStatus.FREE,
        ticketPriceCents: Long = 0L,
        ticketQuantity: Int = 1,
        ticketFeesCents: Long = 0L,
        ticketPurchaseDate: Long? = null,
        ticketDueDate: Long? = null,
        paymentMethod: PaymentMethod = PaymentMethod.PIX,
        creditCardId: String? = null,
        installmentsCount: Int = 1,
        reminderMinutesBefore: Int? = null,
        recurrenceDraft: TaskRecurrenceDraft? = null,
        recurringEditScope: RecurringEditScope = RecurringEditScope.THIS_OCCURRENCE_ONLY
    ): Result<String> = safeDbCall("Eventos") {
        val cleanName = name.trim()
        require(cleanName.isNotEmpty()) { "O nome do evento é obrigatório." }
        val cleanDescription = description.trim()
        val cleanOfficialLink = officialLink.trim()
        val cleanTicketLink = ticketLink.trim()
        val cleanLocationName = locationName.trim()
        val cleanAddress = address.trim()
        val cleanCity = city.trim()
        val cleanState = state.trim()
        val cleanNotes = notes.trim()
        val normalizedEnd = if (endAt >= startAt) endAt else startAt + 7_200_000L
        val durationMillis = (normalizedEnd - startAt).coerceAtLeast(60_000L)
        val dueMinutesOfDay = if (isAllDay) {
            null
        } else {
            RecurrenceEngine.minutesOfDayFromTime(DateTimeUtils.epochToLocalTime(startAt))
        }

        val safeQty = ticketQuantity.coerceAtLeast(1)
        val safeInstallments = if (paymentMethod == PaymentMethod.CREDIT_CARD) {
            installmentsCount.coerceIn(1, 120)
        } else {
            1
        }
        val computedTotal = if (ticketStatus == TicketStatus.FREE) {
            0L
        } else {
            (ticketPriceCents.coerceAtLeast(0L) * safeQty) + ticketFeesCents.coerceAtLeast(0L)
        }
        val now = DateTimeUtils.nowEpochMillis()

        val virtualPair = RecurrenceEngine.parseVirtualOccurrenceId(id)
        val existing = when {
            virtualPair != null -> eventDao.getByRecurrenceAndDate(virtualPair.first, virtualPair.second)
            id != null -> eventDao.getById(id)
            else -> null
        }
        val eventId = existing?.id ?: UUID.randomUUID().toString()
        val resolvedCardId = if (paymentMethod == PaymentMethod.CREDIT_CARD) creditCardId else null
        val existingRecurrenceId = existing?.recurrenceId ?: virtualPair?.first
        val existingOccurrenceIso = existing?.occurrenceDateIso ?: virtualPair?.second
        val existingRecurrence = existingRecurrenceId?.let { taskRecurrenceDao.getById(it) }

        val templateTicketStatus = if (
            ticketStatus == TicketStatus.FREE ||
            ticketStatus == TicketStatus.NOT_BOUGHT ||
            ticketStatus == TicketStatus.CANCELLED
        ) {
            ticketStatus
        } else {
            TicketStatus.NOT_BOUGHT
        }

        var finalRecurrenceId: String? = existingRecurrenceId
        var finalOccurrenceDateIso: String? = existingOccurrenceIso
        var finalStartAt: Long = startAt
        var finalEndAt: Long = normalizedEnd
        var seriesTemplateToUpsert: TaskRecurrenceEntity? = null
        var exceptionToUpsert: RecurrenceExceptionEntity? = null
        var exceptionToDeleteKey: Pair<String, String>? = null
        var eventsToDeleteIds: List<String> = emptyList()
        var recurrenceToDeleteId: String? = null
        var unlinkRecurrenceId: String? = null
        var propagateSeriesEvents: List<EventEntity> = emptyList()
        var recurrenceReminderMinutes: Int? = reminderMinutesBefore

        if (existingRecurrence == null) {
            if (recurrenceDraft != null) {
                val newRecId = UUID.randomUUID().toString()
                val startDateIso = recurrenceDraft.startDateIso.ifBlank {
                    DateTimeUtils.toLocalDate(startAt).toString()
                }
                val startDate = RecurrenceEngine.parseLocalDateOrNull(startDateIso)
                    ?: DateTimeUtils.toLocalDate(startAt)
                val newRec = TaskRecurrenceEntity(
                    id = newRecId,
                    title = cleanName,
                    description = cleanDescription,
                    categoryId = categoryId,
                    priority = TaskPriority.NORMAL,
                    notes = cleanNotes,
                    hasDueTime = !isAllDay,
                    dueTimeMinutesOfDay = dueMinutesOfDay,
                    reminderMinutesBefore = reminderMinutesBefore,
                    frequency = recurrenceDraft.frequency,
                    customIntervalDays = recurrenceDraft.customIntervalDays.coerceAtLeast(1),
                    weekdaysMask = recurrenceDraft.weekdaysMask,
                    startDateIso = startDate.toString(),
                    endDateIso = recurrenceDraft.endDateIso?.takeIf { it.isNotBlank() },
                    maxOccurrences = recurrenceDraft.maxOccurrences?.takeIf { it > 0 },
                    isActive = true,
                    domain = RecurrenceDomain.EVENT,
                    isAllDay = isAllDay,
                    durationMillis = durationMillis,
                    officialLink = cleanOfficialLink,
                    ticketLink = cleanTicketLink,
                    locationName = cleanLocationName,
                    address = cleanAddress,
                    city = cleanCity,
                    state = cleanState,
                    eventStatus = if (eventStatus == EventLifecycleStatus.ATTENDED) {
                        EventLifecycleStatus.PLANNED
                    } else {
                        eventStatus
                    },
                    ticketStatus = templateTicketStatus,
                    ticketPriceCents = if (ticketStatus == TicketStatus.FREE) 0L else ticketPriceCents.coerceAtLeast(0L),
                    ticketQuantity = safeQty,
                    ticketFeesCents = if (ticketStatus == TicketStatus.FREE) 0L else ticketFeesCents.coerceAtLeast(0L),
                    paymentMethod = paymentMethod,
                    creditCardId = resolvedCardId,
                    installmentsCount = safeInstallments,
                    createdAt = now,
                    updatedAt = now
                )
                val firstScheduledDate = RecurrenceEngine.generateScheduledDates(
                    recurrence = newRec,
                    upToDateInclusive = startDate.plusDays(400),
                    hardCap = 1
                ).firstOrNull() ?: startDate
                val userStartDate = DateTimeUtils.toLocalDate(startAt)
                finalOccurrenceDateIso = firstScheduledDate.toString()
                if (userStartDate == firstScheduledDate) {
                    finalStartAt = startAt
                    finalEndAt = normalizedEnd
                } else {
                    finalStartAt = RecurrenceEngine.computeScheduleStartEpoch(
                        date = firstScheduledDate,
                        isAllDay = isAllDay,
                        dueTimeMinutesOfDay = dueMinutesOfDay
                    )
                    finalEndAt = if (isAllDay) {
                        val spanEndDate = DateTimeUtils.toLocalDate(finalStartAt + durationMillis - 60_000L)
                            .coerceAtLeast(firstScheduledDate)
                        DateTimeUtils.localDateToEndOfDayEpoch(spanEndDate)
                    } else {
                        finalStartAt + durationMillis
                    }
                }
                finalRecurrenceId = newRecId
                seriesTemplateToUpsert = newRec
                recurrenceReminderMinutes = newRec.reminderMinutesBefore
            } else {
                finalRecurrenceId = null
                finalOccurrenceDateIso = null
            }
        } else {
            recurrenceReminderMinutes = existingRecurrence.reminderMinutesBefore
            when (recurringEditScope) {
                RecurringEditScope.THIS_OCCURRENCE_ONLY -> {
                    val occIso = existingOccurrenceIso ?: DateTimeUtils.toLocalDate(startAt).toString()
                    finalRecurrenceId = existingRecurrence.id
                    finalOccurrenceDateIso = occIso
                    val changedVsSeriesOrExisting = cleanName != existingRecurrence.title ||
                        cleanDescription != existingRecurrence.description ||
                        cleanLocationName != existingRecurrence.locationName ||
                        cleanOfficialLink != existingRecurrence.officialLink ||
                        isAllDay != existingRecurrence.isAllDay ||
                        (existing != null && (startAt != existing.startAt || normalizedEnd != existing.endAt)) ||
                        virtualPair != null
                    if (changedVsSeriesOrExisting) {
                        val existingEx = recurrenceExceptionDao.getByRecurrenceAndDate(existingRecurrence.id, occIso)
                        exceptionToUpsert = RecurrenceExceptionEntity(
                            id = existingEx?.id ?: UUID.randomUUID().toString(),
                            recurrenceId = existingRecurrence.id,
                            occurrenceDateIso = occIso,
                            exceptionType = RecurrenceExceptionType.OVERRIDE,
                            overrideTitle = cleanName,
                            overrideDueAt = startAt,
                            overrideEndAt = normalizedEnd,
                            overrideDescription = cleanDescription,
                            overrideLocation = cleanLocationName,
                            overrideLink = cleanOfficialLink,
                            overrideIsAllDay = isAllDay,
                            eventId = eventId,
                            createdAt = existingEx?.createdAt ?: now,
                            updatedAt = now
                        )
                    }
                }

                RecurringEditScope.ENTIRE_SERIES -> {
                    if (recurrenceDraft == null) {
                        val siblingEvents = eventDao.getByRecurrenceId(existingRecurrence.id)
                            .filter { it.id != eventId }
                        eventsToDeleteIds = siblingEvents.filter { sib ->
                            sib.eventStatus != EventLifecycleStatus.ATTENDED &&
                                transactionDao.getTicketObligationsByEvent(sib.id).none { it.status == FinancialStatus.SETTLED }
                        }.map { it.id }
                        unlinkRecurrenceId = existingRecurrence.id
                        recurrenceToDeleteId = existingRecurrence.id
                        finalRecurrenceId = null
                        finalOccurrenceDateIso = null
                    } else {
                        val resolvedReminderPref = reminderMinutesBefore ?: existingRecurrence.reminderMinutesBefore
                        val updatedRec = existingRecurrence.copy(
                            title = cleanName,
                            description = cleanDescription,
                            categoryId = categoryId,
                            notes = cleanNotes,
                            hasDueTime = !isAllDay,
                            dueTimeMinutesOfDay = dueMinutesOfDay,
                            reminderMinutesBefore = resolvedReminderPref,
                            frequency = recurrenceDraft.frequency,
                            customIntervalDays = recurrenceDraft.customIntervalDays.coerceAtLeast(1),
                            weekdaysMask = recurrenceDraft.weekdaysMask,
                            startDateIso = recurrenceDraft.startDateIso.ifBlank { existingRecurrence.startDateIso },
                            endDateIso = recurrenceDraft.endDateIso?.takeIf { it.isNotBlank() },
                            maxOccurrences = recurrenceDraft.maxOccurrences?.takeIf { it > 0 },
                            isAllDay = isAllDay,
                            durationMillis = durationMillis,
                            officialLink = cleanOfficialLink,
                            ticketLink = cleanTicketLink,
                            locationName = cleanLocationName,
                            address = cleanAddress,
                            city = cleanCity,
                            state = cleanState,
                            eventStatus = if (eventStatus == EventLifecycleStatus.ATTENDED) {
                                existingRecurrence.eventStatus
                            } else {
                                eventStatus
                            },
                            ticketStatus = templateTicketStatus,
                            ticketPriceCents = if (ticketStatus == TicketStatus.FREE) 0L else ticketPriceCents.coerceAtLeast(0L),
                            ticketQuantity = safeQty,
                            ticketFeesCents = if (ticketStatus == TicketStatus.FREE) 0L else ticketFeesCents.coerceAtLeast(0L),
                            paymentMethod = paymentMethod,
                            creditCardId = resolvedCardId,
                            installmentsCount = safeInstallments,
                            updatedAt = now
                        )
                        seriesTemplateToUpsert = updatedRec
                        recurrenceReminderMinutes = resolvedReminderPref
                        finalRecurrenceId = updatedRec.id
                        finalOccurrenceDateIso = existingOccurrenceIso ?: DateTimeUtils.toLocalDate(startAt).toString()
                        if (existingOccurrenceIso != null) {
                            exceptionToDeleteKey = updatedRec.id to existingOccurrenceIso
                        }
                        val allExByDate = recurrenceExceptionDao.getByRecurrenceId(existingRecurrence.id)
                            .associateBy { it.occurrenceDateIso }
                        propagateSeriesEvents = eventDao.getByRecurrenceId(existingRecurrence.id)
                            .filter {
                                it.id != eventId &&
                                    it.eventStatus != EventLifecycleStatus.ATTENDED &&
                                    it.eventStatus != EventLifecycleStatus.CANCELLED
                            }
                            .mapNotNull { linked ->
                                val linkedIso = linked.occurrenceDateIso
                                    ?: DateTimeUtils.toLocalDate(linked.startAt).toString()
                                if (allExByDate[linkedIso]?.exceptionType == RecurrenceExceptionType.OVERRIDE) {
                                    return@mapNotNull null
                                }
                                val linkedDate = RecurrenceEngine.parseLocalDateOrNull(linkedIso)
                                    ?: DateTimeUtils.toLocalDate(linked.startAt)
                                val nextLinkedStart = RecurrenceEngine.computeScheduleStartEpoch(
                                    date = linkedDate,
                                    isAllDay = isAllDay,
                                    dueTimeMinutesOfDay = dueMinutesOfDay
                                )
                                val nextLinkedEnd = if (isAllDay) {
                                    val spanEndDate = DateTimeUtils.toLocalDate(nextLinkedStart + durationMillis - 60_000L)
                                        .coerceAtLeast(linkedDate)
                                    DateTimeUtils.localDateToEndOfDayEpoch(spanEndDate)
                                } else {
                                    nextLinkedStart + durationMillis
                                }
                                // Preserva status financeiro e transações existentes da ocorrência irmã
                                linked.copy(
                                    name = cleanName,
                                    description = cleanDescription,
                                    categoryId = categoryId,
                                    startAt = nextLinkedStart,
                                    endAt = nextLinkedEnd,
                                    isAllDay = isAllDay,
                                    officialLink = cleanOfficialLink,
                                    ticketLink = cleanTicketLink,
                                    locationName = cleanLocationName,
                                    address = cleanAddress,
                                    city = cleanCity,
                                    state = cleanState,
                                    notes = cleanNotes,
                                    updatedAt = now
                                )
                            }
                    }
                }
            }
        }

        val savedEvent = EventEntity(
            id = eventId,
            name = cleanName,
            description = cleanDescription,
            categoryId = categoryId,
            startAt = finalStartAt,
            endAt = finalEndAt,
            isAllDay = isAllDay,
            officialLink = cleanOfficialLink,
            ticketLink = cleanTicketLink,
            locationName = cleanLocationName,
            address = cleanAddress,
            city = cleanCity,
            state = cleanState,
            notes = cleanNotes,
            eventStatus = eventStatus,
            ticketStatus = ticketStatus,
            ticketPriceCents = if (ticketStatus == TicketStatus.FREE) 0L else ticketPriceCents.coerceAtLeast(0L),
            ticketQuantity = safeQty,
            ticketFeesCents = if (ticketStatus == TicketStatus.FREE) 0L else ticketFeesCents.coerceAtLeast(0L),
            ticketTotalCents = computedTotal,
            ticketPurchaseDate = ticketPurchaseDate,
            ticketDueDate = ticketDueDate,
            paymentMethod = paymentMethod,
            creditCardId = resolvedCardId,
            installmentsCount = safeInstallments,
            recurrenceId = finalRecurrenceId,
            occurrenceDateIso = finalOccurrenceDateIso,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now
        )

        var affectedTicketTransactions: List<TransactionEntity> = emptyList()
        var removedTicketTransactionIds: List<String> = emptyList()

        eventsToDeleteIds.forEach { delId ->
            cancelAndDeleteRemindersForSource(ReminderSourceType.EVENT, delId)
            transactionDao.getTicketObligationsByEvent(delId).forEach { tx ->
                cancelAndDeleteRemindersForSource(ReminderSourceType.TRANSACTION, tx.id)
            }
        }
        if (virtualPair != null) {
            cancelAndDeleteRemindersForSource(
                ReminderSourceType.EVENT,
                RecurrenceEngine.virtualOccurrenceId(virtualPair.first, virtualPair.second)
            )
        }
        if (recurrenceToDeleteId != null) {
            cancelAndDeleteVirtualRemindersForRecurrence(ReminderSourceType.EVENT, recurrenceToDeleteId)
        }

        database.withTransaction {
            if (unlinkRecurrenceId != null) {
                eventsToDeleteIds.forEach { delId ->
                    deleteSingleEventInternal(delId, now)
                }
                recurrenceExceptionDao.deleteByRecurrenceId(unlinkRecurrenceId)
                eventDao.unlinkRecurrence(unlinkRecurrenceId, now)
                recurrenceToDeleteId?.let { taskRecurrenceDao.deleteById(it) }
            }
            seriesTemplateToUpsert?.let { taskRecurrenceDao.upsert(it) }
            eventDao.upsert(savedEvent)
            propagateSeriesEvents.forEach { eventDao.update(it) }
            exceptionToDeleteKey?.let { (recId, occIso) ->
                recurrenceExceptionDao.getByRecurrenceAndDate(recId, occIso)
                    ?.takeIf { it.exceptionType == RecurrenceExceptionType.OVERRIDE }
                    ?.let { recurrenceExceptionDao.deleteById(it.id) }
            }
            exceptionToUpsert?.let { recurrenceExceptionDao.upsert(it) }

            val existingObligations = transactionDao.getTicketObligationsByEvent(eventId)
            val existingPurchaseId = existingObligations.firstNotNullOfOrNull { it.installmentPurchaseId }

            if (!ticketStatus.requiresFinancialObligation || computedTotal <= 0L) {
                if (ticketStatus == TicketStatus.CANCELLED) {
                    existingObligations.forEach { tx ->
                        val updatedTx = tx.copy(status = FinancialStatus.CANCELLED, updatedAt = now)
                        transactionDao.upsert(updatedTx)
                        affectedTicketTransactions = affectedTicketTransactions + updatedTx
                    }
                } else {
                    removedTicketTransactionIds = existingObligations.map { it.id }
                    transactionDao.deleteTicketObligationForEvent(eventId)
                    if (existingPurchaseId != null) {
                        installmentPurchaseDao.deleteById(existingPurchaseId)
                    }
                }
            } else if (safeInstallments > 1 && paymentMethod == PaymentMethod.CREDIT_CARD) {
                val firstDueEpoch = ticketDueDate ?: ticketPurchaseDate ?: finalStartAt
                val firstDueDate = DateTimeUtils.epochToLocalDate(firstDueEpoch)
                val purchaseId = existingPurchaseId ?: UUID.randomUUID().toString()
                val baseInstallment = computedTotal / safeInstallments
                val remainder = computedTotal % safeInstallments

                val unlinkedOld = existingObligations.filter { it.installmentPurchaseId == null }
                removedTicketTransactionIds = removedTicketTransactionIds + unlinkedOld.map { it.id }
                unlinkedOld.forEach { transactionDao.deleteById(it.id) }

                installmentPurchaseDao.upsert(
                    InstallmentPurchaseEntity(
                        id = purchaseId,
                        description = "Ingresso: $cleanName",
                        totalAmountCents = computedTotal,
                        installmentsCount = safeInstallments,
                        installmentAmountCents = baseInstallment,
                        firstDueAt = firstDueEpoch,
                        creditCardId = resolvedCardId,
                        categoryId = categoryId ?: "cat-fin-eventos",
                        eventId = eventId,
                        notes = "Parcelamento vinculado automaticamente ao evento $cleanName",
                        createdAt = existing?.createdAt ?: now,
                        updatedAt = now
                    )
                )

                val existingByNumber = existingObligations
                    .filter { it.installmentNumber != null }
                    .associateBy { it.installmentNumber!! }

                val upsertedList = (1..safeInstallments).map { number ->
                    val oldTx = existingByNumber[number]
                    val dueDate = firstDueDate.plusMonths((number - 1).toLong())
                    val dueEpoch = DateTimeUtils.localDateTimeToEpoch(dueDate, LocalTime.of(12, 0))
                    val amount = baseInstallment + if (number == 1) remainder else 0L
                    val stmtId = if (resolvedCardId != null) {
                        ensureStatementForCardAndDate(resolvedCardId, dueEpoch, now)?.id
                    } else null

                    val targetStatus = when {
                        ticketStatus.isPaidOrBought -> FinancialStatus.SETTLED
                        oldTx?.status == FinancialStatus.SETTLED && ticketStatus == TicketStatus.PARTIALLY_PAID -> FinancialStatus.SETTLED
                        else -> FinancialStatus.PENDING
                    }

                    TransactionEntity(
                        id = oldTx?.id ?: UUID.randomUUID().toString(),
                        type = TransactionType.EXPENSE,
                        description = "Ingresso: $cleanName ($number/$safeInstallments)",
                        amountCents = amount,
                        categoryId = categoryId ?: "cat-fin-eventos",
                        status = targetStatus,
                        paymentMethod = PaymentMethod.CREDIT_CARD,
                        occurredAt = ticketPurchaseDate ?: now,
                        dueAt = dueEpoch,
                        settledAt = if (targetStatus == FinancialStatus.SETTLED) (oldTx?.settledAt ?: ticketPurchaseDate ?: now) else null,
                        eventId = eventId,
                        eventCostType = EventCostType.TICKET,
                        isTicketObligation = true,
                        creditCardId = resolvedCardId,
                        statementId = stmtId,
                        installmentPurchaseId = purchaseId,
                        installmentNumber = number,
                        installmentsTotal = safeInstallments,
                        notes = "Parcela de ingresso vinculada ao evento $cleanName",
                        createdAt = oldTx?.createdAt ?: now,
                        updatedAt = now
                    )
                }
                val excessOld = existingObligations.filter { (it.installmentNumber ?: 0) > safeInstallments }
                removedTicketTransactionIds = removedTicketTransactionIds + excessOld.map { it.id }
                excessOld.forEach { transactionDao.deleteById(it.id) }

                transactionDao.insertAll(upsertedList)
                affectedTicketTransactions = upsertedList
                syncEventTicketStatusFromTransactionsInternal(eventId, now)
                recordAudit(
                    "EventTicket",
                    eventId,
                    "Ingresso parcelado vinculado",
                    "$cleanName · ${safeInstallments}x · ${DateTimeUtils.formatBrl(computedTotal)}"
                )
            } else {
                if (existingPurchaseId != null) {
                    val purchaseTxs = existingObligations.filter { it.installmentPurchaseId == existingPurchaseId }
                    removedTicketTransactionIds = removedTicketTransactionIds + purchaseTxs.drop(1).map { it.id }
                    purchaseTxs.drop(1).forEach { transactionDao.deleteById(it.id) }
                    installmentPurchaseDao.deleteById(existingPurchaseId)
                }
                val keeper = existingObligations.firstOrNull()
                val dueEpoch = ticketDueDate ?: finalStartAt
                val occurredEpoch = ticketPurchaseDate ?: now
                val targetFinancialStatus = if (ticketStatus.isPaidOrBought) {
                    FinancialStatus.SETTLED
                } else {
                    FinancialStatus.PENDING
                }
                val stmtId = if (resolvedCardId != null) {
                    ensureStatementForCardAndDate(resolvedCardId, occurredEpoch, now)?.id
                } else null

                val tx = TransactionEntity(
                    id = keeper?.id ?: UUID.randomUUID().toString(),
                    type = TransactionType.EXPENSE,
                    description = "Ingresso: $cleanName",
                    amountCents = computedTotal,
                    categoryId = categoryId ?: "cat-fin-eventos",
                    status = targetFinancialStatus,
                    paymentMethod = paymentMethod,
                    occurredAt = occurredEpoch,
                    dueAt = dueEpoch,
                    settledAt = if (targetFinancialStatus == FinancialStatus.SETTLED) (ticketPurchaseDate ?: now) else null,
                    eventId = eventId,
                    eventCostType = EventCostType.TICKET,
                    isTicketObligation = true,
                    creditCardId = resolvedCardId,
                    statementId = stmtId,
                    installmentPurchaseId = null,
                    installmentNumber = null,
                    installmentsTotal = null,
                    notes = "Obrigação vinculada automaticamente ao evento $cleanName",
                    createdAt = keeper?.createdAt ?: now,
                    updatedAt = now
                )
                transactionDao.upsert(tx)
                affectedTicketTransactions = listOf(tx)
                recordAudit(
                    "EventTicket",
                    eventId,
                    if (keeper == null) "Ingresso vinculado ao financeiro" else "Ingresso atualizado",
                    "$cleanName · ${ticketStatus.label} · ${DateTimeUtils.formatBrl(computedTotal)}"
                )
            }
        }

        syncEventReminder(savedEvent, reminderMinutesBefore ?: recurrenceReminderMinutes)
        propagateSeriesEvents.forEach { syncEventReminder(it, overrideMinutesBefore = recurrenceReminderMinutes) }
        removedTicketTransactionIds.forEach { txId ->
            cancelAndDeleteRemindersForSource(ReminderSourceType.TRANSACTION, txId)
        }
        affectedTicketTransactions.forEach { tx ->
            syncTransactionReminder(tx, overrideMinutesBefore = null)
        }
        syncRecurringScheduleRemindersInternal()
        notifyWidgetsChanged()
        eventId
    }

    suspend fun openOrMaterializeRecurringEventOccurrence(
        recurrenceId: String,
        occurrenceDateIso: String
    ): Result<String> = safeDbCall("Eventos Recorrentes") {
        val existing = eventDao.getByRecurrenceAndDate(recurrenceId, occurrenceDateIso)
        if (existing != null) return@safeDbCall existing.id

        val recurrence = taskRecurrenceDao.getById(recurrenceId)
            ?: throw IllegalStateException("Série recorrente não encontrada.")
        val occDate = RecurrenceEngine.parseLocalDateOrNull(occurrenceDateIso)
            ?: throw IllegalArgumentException("Data de ocorrência inválida: $occurrenceDateIso")
        val now = DateTimeUtils.nowEpochMillis()
        val exception = recurrenceExceptionDao.getByRecurrenceAndDate(recurrenceId, occurrenceDateIso)
        val isOverride = exception?.exceptionType == RecurrenceExceptionType.OVERRIDE
        val effectiveAllDay = if (isOverride && exception?.overrideIsAllDay != null) {
            exception.overrideIsAllDay
        } else {
            recurrence.isAllDay
        }
        val baseDuration = recurrence.durationMillis.coerceAtLeast(60_000L)
        val startAt = if (isOverride && exception?.overrideDueAt != null) {
            exception.overrideDueAt
        } else {
            RecurrenceEngine.computeScheduleStartEpoch(
                date = occDate,
                isAllDay = effectiveAllDay,
                dueTimeMinutesOfDay = recurrence.dueTimeMinutesOfDay
            )
        }
        val endAt = if (isOverride && exception?.overrideEndAt != null) {
            exception.overrideEndAt.coerceAtLeast(startAt + 60_000L)
        } else if (effectiveAllDay) {
            val spanEndDate = DateTimeUtils.toLocalDate(startAt + baseDuration - 60_000L)
                .coerceAtLeast(DateTimeUtils.toLocalDate(startAt))
            DateTimeUtils.localDateToEndOfDayEpoch(spanEndDate)
        } else {
            startAt + baseDuration
        }
        val effectiveName = if (isOverride && !exception?.overrideTitle.isNullOrBlank()) {
            exception!!.overrideTitle!!
        } else {
            recurrence.title
        }
        val effectiveDescription = if (isOverride && exception?.overrideDescription != null) {
            exception.overrideDescription
        } else {
            recurrence.description
        }
        val effectiveLocationName = if (isOverride && exception?.overrideLocation != null) {
            exception.overrideLocation
        } else {
            recurrence.locationName
        }
        val effectiveOfficialLink = if (isOverride && exception?.overrideLink != null) {
            exception.overrideLink
        } else {
            recurrence.officialLink
        }
        val safeTicketStatus = when (recurrence.ticketStatus) {
            TicketStatus.FREE -> TicketStatus.FREE
            TicketStatus.CANCELLED -> TicketStatus.CANCELLED
            else -> TicketStatus.NOT_BOUGHT
        }

        // Materialização sob demanda nunca cria transações ou obrigações financeiras fantasmas
        val candidate = EventEntity(
            id = UUID.randomUUID().toString(),
            name = effectiveName,
            description = effectiveDescription,
            categoryId = recurrence.categoryId,
            startAt = startAt,
            endAt = endAt,
            isAllDay = effectiveAllDay,
            officialLink = effectiveOfficialLink,
            ticketLink = recurrence.ticketLink,
            locationName = effectiveLocationName,
            address = recurrence.address,
            city = recurrence.city,
            state = recurrence.state,
            notes = recurrence.notes,
            eventStatus = if (recurrence.eventStatus == EventLifecycleStatus.ATTENDED) {
                EventLifecycleStatus.PLANNED
            } else {
                recurrence.eventStatus
            },
            ticketStatus = safeTicketStatus,
            ticketPriceCents = if (safeTicketStatus == TicketStatus.FREE) 0L else recurrence.ticketPriceCents,
            ticketQuantity = recurrence.ticketQuantity.coerceAtLeast(1),
            ticketFeesCents = if (safeTicketStatus == TicketStatus.FREE) 0L else recurrence.ticketFeesCents,
            ticketTotalCents = 0L,
            ticketPurchaseDate = null,
            ticketDueDate = null,
            paymentMethod = recurrence.paymentMethod,
            creditCardId = recurrence.creditCardId,
            installmentsCount = recurrence.installmentsCount.coerceIn(1, 120),
            recurrenceId = recurrence.id,
            occurrenceDateIso = occurrenceDateIso,
            createdAt = now,
            updatedAt = now
        )

        val rowId = database.withTransaction {
            val insertedId = eventDao.insertIgnore(candidate)
            if (insertedId != -1L && exception != null) {
                recurrenceExceptionDao.upsert(exception.copy(eventId = candidate.id, updatedAt = now))
            }
            insertedId
        }
        val persisted = if (rowId != -1L) {
            candidate
        } else {
            eventDao.getByRecurrenceAndDate(recurrenceId, occurrenceDateIso) ?: candidate
        }
        if (rowId != -1L) {
            val virtualId = RecurrenceEngine.virtualOccurrenceId(recurrenceId, occurrenceDateIso)
            val virtualReminder = reminderDao.getBySourceAndCategory(
                ReminderSourceType.EVENT,
                virtualId,
                ReminderCategory.SCHEDULE
            )
            val effectiveRemMinutes = virtualReminder?.minutesBefore?.takeIf { it >= 0 }
                ?: recurrence.reminderMinutesBefore
            cancelAndDeleteRemindersForSource(ReminderSourceType.EVENT, virtualId)
            if (virtualReminder?.status != ReminderStatus.DISMISSED) {
                syncEventReminder(persisted, overrideMinutesBefore = effectiveRemMinutes)
            }
            notifyWidgetsChanged()
        }
        persisted.id
    }

    suspend fun countAppointmentsForEvent(eventId: String): Int = withContext(Dispatchers.IO) {
        appointmentDao.countByEvent(eventId)
    }

    suspend fun deleteEvent(
        eventId: String,
        scope: RecurringEditScope = RecurringEditScope.THIS_OCCURRENCE_ONLY
    ): Result<Unit> = safeDbCall("Eventos") {
        val now = DateTimeUtils.nowEpochMillis()
        val virtualPair = RecurrenceEngine.parseVirtualOccurrenceId(eventId)
        val existing = if (virtualPair != null) {
            eventDao.getByRecurrenceAndDate(virtualPair.first, virtualPair.second)
        } else {
            eventDao.getById(eventId)
        }
        val recId = existing?.recurrenceId ?: virtualPair?.first

        if (recId != null) {
            if (scope == RecurringEditScope.ENTIRE_SERIES) {
                val seriesEvents = eventDao.getByRecurrenceId(recId)
                val deletableIds = mutableListOf<String>()
                for (ev in seriesEvents) {
                    val evTicketTxs = transactionDao.getTicketObligationsByEvent(ev.id)
                    val hasSettled = evTicketTxs.any { it.status == FinancialStatus.SETTLED }
                    if (ev.eventStatus != EventLifecycleStatus.ATTENDED && !hasSettled) {
                        deletableIds += ev.id
                        cancelAndDeleteRemindersForSource(ReminderSourceType.EVENT, ev.id)
                        evTicketTxs.forEach { tx ->
                            cancelAndDeleteRemindersForSource(ReminderSourceType.TRANSACTION, tx.id)
                        }
                    }
                }
                cancelAndDeleteVirtualRemindersForRecurrence(ReminderSourceType.EVENT, recId)
                database.withTransaction {
                    deletableIds.forEach { delId ->
                        deleteSingleEventInternal(delId, now)
                    }
                    recurrenceExceptionDao.deleteByRecurrenceId(recId)
                    eventDao.unlinkRecurrence(recId, now)
                    taskRecurrenceDao.deleteById(recId)
                }
            } else {
                val occIso = existing?.occurrenceDateIso
                    ?: virtualPair?.second
                    ?: existing?.let { DateTimeUtils.toLocalDate(it.startAt).toString() }
                if (existing != null) {
                    val ticketTxs = transactionDao.getTicketObligationsByEvent(existing.id)
                    cancelAndDeleteRemindersForSource(ReminderSourceType.EVENT, existing.id)
                    ticketTxs.filter { it.status != FinancialStatus.SETTLED }.forEach { tx ->
                        cancelAndDeleteRemindersForSource(ReminderSourceType.TRANSACTION, tx.id)
                    }
                }
                if (occIso != null) {
                    cancelAndDeleteRemindersForSource(
                        ReminderSourceType.EVENT,
                        RecurrenceEngine.virtualOccurrenceId(recId, occIso)
                    )
                }
                database.withTransaction {
                    if (occIso != null) {
                        val existingEx = recurrenceExceptionDao.getByRecurrenceAndDate(recId, occIso)
                        recurrenceExceptionDao.upsert(
                            RecurrenceExceptionEntity(
                                id = existingEx?.id ?: UUID.randomUUID().toString(),
                                recurrenceId = recId,
                                occurrenceDateIso = occIso,
                                exceptionType = RecurrenceExceptionType.SKIP,
                                eventId = null,
                                createdAt = existingEx?.createdAt ?: now,
                                updatedAt = now
                            )
                        )
                    }
                    existing?.let { deleteSingleEventInternal(it.id, now) }
                }
            }
        } else if (existing != null) {
            val ticketTxs = transactionDao.getTicketObligationsByEvent(existing.id)
            cancelAndDeleteRemindersForSource(ReminderSourceType.EVENT, existing.id)
            ticketTxs.filter { it.status != FinancialStatus.SETTLED }.forEach { tx ->
                cancelAndDeleteRemindersForSource(ReminderSourceType.TRANSACTION, tx.id)
            }
            database.withTransaction {
                deleteSingleEventInternal(existing.id, now)
            }
        }
        syncRecurringScheduleRemindersInternal()
        notifyWidgetsChanged()
    }

    private suspend fun deleteSingleEventInternal(eventId: String, now: Long) {
        val ticketTxs = transactionDao.getTicketObligationsByEvent(eventId)
        val purchaseId = ticketTxs.firstNotNullOfOrNull { it.installmentPurchaseId }
        taskDao.unlinkEvent(eventId, now)
        appointmentDao.unlinkEvent(eventId, now)
        // Protege transações liquidadas (SETTLED), removendo apenas obrigações de ingresso em aberto
        ticketTxs.filter { it.status != FinancialStatus.SETTLED }.forEach { tx ->
            transactionDao.deleteById(tx.id)
        }
        if (purchaseId != null) {
            val remainingPurchaseTxs = transactionDao.getByInstallmentPurchaseId(purchaseId)
            if (remainingPurchaseTxs.isEmpty()) {
                installmentPurchaseDao.deleteById(purchaseId)
            }
        }
        transactionDao.unlinkEvent(eventId, now)
        eventDao.deleteById(eventId)
    }

    // --- Financeiro: Transacoes (Receitas, Despesas e Custos de Evento) ---

    suspend fun saveTransaction(
        id: String? = null,
        type: TransactionType,
        description: String,
        amountCents: Long,
        categoryId: String? = null,
        status: FinancialStatus = FinancialStatus.PENDING,
        paymentMethod: PaymentMethod = PaymentMethod.PIX,
        occurredAt: Long = DateTimeUtils.nowEpochMillis(),
        dueAt: Long? = null,
        settledAt: Long? = null,
        projectId: String? = null,
        eventId: String? = null,
        taskId: String? = null,
        eventCostType: EventCostType? = null,
        creditCardId: String? = null,
        notes: String = "",
        reminderMinutesBefore: Int? = null
    ): Result<String> = safeDbCall("Financeiro") {
        val cleanDesc = description.trim()
        require(cleanDesc.isNotEmpty()) { "A descrição do lançamento é obrigatória." }
        require(amountCents > 0L) { "O valor deve ser maior que zero." }

        val now = DateTimeUtils.nowEpochMillis()
        val existing = id?.let { transactionDao.getById(it) }
        val txId = existing?.id ?: UUID.randomUUID().toString()
        val resolvedCardId = if (type == TransactionType.EXPENSE && paymentMethod == PaymentMethod.CREDIT_CARD) {
            creditCardId
        } else null

        var savedTx: TransactionEntity? = null
        database.withTransaction {
            val stmtId = if (resolvedCardId != null) {
                ensureStatementForCardAndDate(resolvedCardId, dueAt ?: occurredAt, now)?.id
            } else null

            val finalSettledAt = when (status) {
                FinancialStatus.SETTLED -> settledAt ?: existing?.settledAt ?: now
                else -> null
            }

            val tx = TransactionEntity(
                id = txId,
                type = type,
                description = cleanDesc,
                amountCents = amountCents,
                categoryId = categoryId,
                status = status,
                paymentMethod = paymentMethod,
                occurredAt = occurredAt,
                dueAt = dueAt,
                settledAt = finalSettledAt,
                projectId = projectId,
                eventId = eventId,
                taskId = taskId,
                eventCostType = eventCostType,
                isTicketObligation = existing?.isTicketObligation ?: false,
                creditCardId = resolvedCardId,
                statementId = stmtId ?: existing?.statementId,
                installmentPurchaseId = existing?.installmentPurchaseId,
                installmentNumber = existing?.installmentNumber,
                installmentsTotal = existing?.installmentsTotal,
                recurringRuleId = existing?.recurringRuleId,
                occurrenceIndex = existing?.occurrenceIndex,
                notes = notes.trim(),
                createdAt = existing?.createdAt ?: now,
                updatedAt = now
            )
            transactionDao.upsert(tx)
            savedTx = tx

            if (tx.isTicketObligation && tx.eventId != null) {
                syncEventTicketStatusFromTransactionsInternal(tx.eventId, now)
            }

            recordAudit(
                "Transaction",
                txId,
                if (existing == null) "${type.label} criada" else "${type.label} editada",
                "$cleanDesc · ${DateTimeUtils.formatBrl(amountCents)} · ${status.label}"
            )
        }
        savedTx?.let { syncTransactionReminder(it, reminderMinutesBefore) }
        notifyWidgetsChanged()
        txId
    }

    suspend fun settleTransaction(transactionId: String): Result<Unit> = safeDbCall("Financeiro") {
        val existing = transactionDao.getById(transactionId)
            ?: throw IllegalStateException("Lançamento não encontrado.")
        val now = DateTimeUtils.nowEpochMillis()
        lateinit var updatedTx: TransactionEntity
        database.withTransaction {
            val nextStatus = if (existing.status == FinancialStatus.SETTLED) {
                if (existing.dueAt != null && existing.dueAt < DateTimeUtils.localDateToStartOfDayEpoch(DateTimeUtils.todayLocalDate())) {
                    FinancialStatus.OVERDUE
                } else {
                    FinancialStatus.PENDING
                }
            } else {
                FinancialStatus.SETTLED
            }
            updatedTx = existing.copy(
                status = nextStatus,
                settledAt = if (nextStatus == FinancialStatus.SETTLED) now else null,
                updatedAt = now
            )
            transactionDao.upsert(updatedTx)
            if (existing.isTicketObligation && existing.eventId != null) {
                syncEventTicketStatusFromTransactionsInternal(existing.eventId, now)
            }
            recordAudit(
                "Transaction",
                transactionId,
                if (nextStatus == FinancialStatus.SETTLED) "Marcado como realizado" else "Reaberto como pendente",
                "${existing.description} · ${DateTimeUtils.formatBrl(existing.amountCents)}"
            )
        }
        syncTransactionReminder(updatedTx, overrideMinutesBefore = null)
        notifyWidgetsChanged()
    }

    suspend fun deleteTransaction(transactionId: String): Result<Unit> = safeDbCall("Financeiro") {
        val existing = transactionDao.getById(transactionId)
        cancelAndDeleteRemindersForSource(ReminderSourceType.TRANSACTION, transactionId)
        database.withTransaction {
            transactionDao.deleteById(transactionId)
            if (existing != null) {
                if (existing.isTicketObligation && existing.eventId != null) {
                    syncEventTicketStatusFromTransactionsInternal(existing.eventId, DateTimeUtils.nowEpochMillis())
                }
                recordAudit(
                    "Transaction",
                    transactionId,
                    "Lançamento excluído",
                    "${existing.description} · ${DateTimeUtils.formatBrl(existing.amountCents)}"
                )
            }
        }
        notifyWidgetsChanged()
    }

    // --- Cartoes de Credito e Faturas (sem duplicar despesa!) ---

    suspend fun saveCreditCard(
        id: String? = null,
        name: String,
        institution: String = "",
        totalLimitCents: Long,
        availableLimitCents: Long,
        closingDay: Int,
        dueDay: Int,
        colorHex: String = "#9A9A9A",
        notes: String = ""
    ): Result<String> = safeDbCall("Cartões") {
        val cleanName = name.trim()
        require(cleanName.isNotEmpty()) { "O nome do cartão é obrigatório." }
        val now = DateTimeUtils.nowEpochMillis()
        val existing = id?.let { creditCardDao.getById(it) }
        val cardId = existing?.id ?: UUID.randomUUID().toString()
        creditCardDao.upsert(
            CreditCardEntity(
                id = cardId,
                name = cleanName,
                institution = institution.trim(),
                totalLimitCents = totalLimitCents.coerceAtLeast(0L),
                availableLimitCents = availableLimitCents.coerceAtLeast(0L),
                closingDay = closingDay.coerceIn(1, 31),
                dueDay = dueDay.coerceIn(1, 31),
                colorHex = colorHex.ifBlank { "#9A9A9A" },
                notes = notes.trim(),
                createdAt = existing?.createdAt ?: now,
                updatedAt = now
            )
        )
        cardId
    }

    suspend fun deleteCreditCard(cardId: String): Result<Unit> = safeDbCall("Cartões") {
        val statements = statementDao.getAllOnce().filter { it.creditCardId == cardId }
        statements.forEach { stmt ->
            cancelAndDeleteRemindersForSource(ReminderSourceType.STATEMENT, stmt.id)
        }
        creditCardDao.deleteById(cardId)
    }

    /**
     * Paga a fatura marcando a fatura e suas compras vinculadas como quitadas,
     * SEM criar uma segunda despesa de consumo. Sincroniza ingressos vinculados e cancela lembretes financeiros.
     */
    suspend fun payCreditCardStatement(statementId: String): Result<Unit> = safeDbCall("Faturas") {
        val stmt = statementDao.getById(statementId)
            ?: throw IllegalStateException("Fatura não encontrada.")
        val now = DateTimeUtils.nowEpochMillis()
        var affectedTxIds: List<String> = emptyList()
        database.withTransaction {
            val linkedTx = transactionDao.getByStatementId(statementId)
            val total = linkedTx
                .filter { it.type == TransactionType.EXPENSE && it.status != FinancialStatus.CANCELLED }
                .sumOf { it.amountCents }

            val affectedEvents = linkedTx.mapNotNull { if (it.isTicketObligation) it.eventId else null }.toSet()

            linkedTx.forEach { tx ->
                if (tx.status != FinancialStatus.CANCELLED && tx.status != FinancialStatus.SETTLED) {
                    transactionDao.upsert(
                        tx.copy(
                            status = FinancialStatus.SETTLED,
                            settledAt = now,
                            updatedAt = now
                        )
                    )
                    affectedTxIds = affectedTxIds + tx.id
                }
            }
            affectedEvents.forEach { evId ->
                syncEventTicketStatusFromTransactionsInternal(evId, now)
            }
            statementDao.upsert(
                stmt.copy(
                    status = StatementStatus.PAID,
                    paidAt = now,
                    paidAmountCents = total,
                    updatedAt = now
                )
            )
            recordAudit(
                "CreditCardStatement",
                statementId,
                "Fatura paga (sem duplicar compras)",
                "Competência ${stmt.yearMonth} · ${DateTimeUtils.formatBrl(total)}"
            )
        }
        cancelFinancialRemindersForSource(ReminderSourceType.STATEMENT, statementId)
        affectedTxIds.forEach { txId ->
            cancelFinancialRemindersForSource(ReminderSourceType.TRANSACTION, txId)
        }
        notifyWidgetsChanged()
    }

    suspend fun closeCreditCardStatement(statementId: String): Result<Unit> = safeDbCall("Faturas") {
        val stmt = statementDao.getById(statementId)
            ?: throw IllegalStateException("Fatura não encontrada.")
        statementDao.upsert(
            stmt.copy(
                status = StatementStatus.CLOSED,
                updatedAt = DateTimeUtils.nowEpochMillis()
            )
        )
        notifyWidgetsChanged()
    }

    // --- Compras Parceladas ---

    suspend fun createInstallmentPurchase(
        description: String,
        totalAmountCents: Long,
        installmentsCount: Int,
        firstDueDate: LocalDate,
        creditCardId: String?,
        categoryId: String?,
        projectId: String? = null,
        eventId: String? = null,
        notes: String = ""
    ): Result<String> = safeDbCall("Parcelamentos") {
        val cleanDesc = description.trim()
        require(cleanDesc.isNotEmpty()) { "A descrição do parcelamento é obrigatória." }
        require(totalAmountCents > 0L) { "O valor total deve ser maior que zero." }
        val count = installmentsCount.coerceIn(2, 120)
        val now = DateTimeUtils.nowEpochMillis()
        val purchaseId = UUID.randomUUID().toString()
        val baseInstallment = totalAmountCents / count
        val remainder = totalAmountCents % count

        var generatedTransactions: List<TransactionEntity> = emptyList()
        database.withTransaction {
            installmentPurchaseDao.upsert(
                InstallmentPurchaseEntity(
                    id = purchaseId,
                    description = cleanDesc,
                    totalAmountCents = totalAmountCents,
                    installmentsCount = count,
                    installmentAmountCents = baseInstallment,
                    firstDueAt = DateTimeUtils.localDateTimeToEpoch(firstDueDate, LocalTime.of(12, 0)),
                    creditCardId = creditCardId,
                    categoryId = categoryId,
                    eventId = eventId,
                    projectId = projectId,
                    notes = notes.trim(),
                    createdAt = now,
                    updatedAt = now
                )
            )

            generatedTransactions = (1..count).map { number ->
                val dueDate = firstDueDate.plusMonths((number - 1).toLong())
                val dueEpoch = DateTimeUtils.localDateTimeToEpoch(dueDate, LocalTime.of(12, 0))
                val amount = baseInstallment + if (number == 1) remainder else 0L
                val stmtId = if (creditCardId != null) {
                    ensureStatementForCardAndDate(creditCardId, dueEpoch, now)?.id
                } else null

                TransactionEntity(
                    id = UUID.randomUUID().toString(),
                    type = TransactionType.EXPENSE,
                    description = "$cleanDesc ($number/$count)",
                    amountCents = amount,
                    categoryId = categoryId,
                    status = FinancialStatus.PENDING,
                    paymentMethod = if (creditCardId != null) PaymentMethod.CREDIT_CARD else PaymentMethod.OTHER,
                    occurredAt = now,
                    dueAt = dueEpoch,
                    projectId = projectId,
                    eventId = eventId,
                    creditCardId = creditCardId,
                    statementId = stmtId,
                    installmentPurchaseId = purchaseId,
                    installmentNumber = number,
                    installmentsTotal = count,
                    notes = notes.trim(),
                    createdAt = now,
                    updatedAt = now
                )
            }
            transactionDao.insertAll(generatedTransactions)
            recordAudit(
                "InstallmentPurchase",
                purchaseId,
                "Parcelamento criado",
                "$cleanDesc · ${count}x · ${DateTimeUtils.formatBrl(totalAmountCents)}"
            )
        }
        generatedTransactions.forEach { syncTransactionReminder(it, overrideMinutesBefore = null) }
        notifyWidgetsChanged()
        purchaseId
    }

    suspend fun settleAllRemainingInstallments(purchaseId: String): Result<Unit> =
        safeDbCall("Parcelamentos") {
            val now = DateTimeUtils.nowEpochMillis()
            var settledIds: List<String> = emptyList()
            database.withTransaction {
                val items = transactionDao.getByInstallmentPurchaseId(purchaseId)
                val affectedEvents = items.mapNotNull { if (it.isTicketObligation) it.eventId else null }.toSet()
                items.filter { it.status != FinancialStatus.SETTLED && it.status != FinancialStatus.CANCELLED }
                    .forEach { tx ->
                        transactionDao.upsert(
                            tx.copy(
                                status = FinancialStatus.SETTLED,
                                settledAt = now,
                                updatedAt = now
                            )
                        )
                        settledIds = settledIds + tx.id
                    }
                affectedEvents.forEach { evId ->
                    syncEventTicketStatusFromTransactionsInternal(evId, now)
                }
                recordAudit("InstallmentPurchase", purchaseId, "Parcelamento quitado", "Parcelas restantes liquidadas")
            }
            settledIds.forEach { txId ->
                cancelFinancialRemindersForSource(ReminderSourceType.TRANSACTION, txId)
            }
            notifyWidgetsChanged()
        }

    suspend fun deleteInstallmentPurchase(purchaseId: String): Result<Unit> =
        safeDbCall("Parcelamentos") {
            val now = DateTimeUtils.nowEpochMillis()
            val items = transactionDao.getByInstallmentPurchaseId(purchaseId)
            val nonSettled = items.filter { it.status != FinancialStatus.SETTLED }
            nonSettled.forEach { cancelAndDeleteRemindersForSource(ReminderSourceType.TRANSACTION, it.id) }

            database.withTransaction {
                val affectedEvents = items.mapNotNull { if (it.isTicketObligation) it.eventId else null }.toSet()
                transactionDao.deleteNonSettledByInstallmentPurchase(purchaseId)
                transactionDao.unlinkInstallmentPurchase(purchaseId, now)
                installmentPurchaseDao.deleteById(purchaseId)
                affectedEvents.forEach { evId ->
                    syncEventTicketStatusFromTransactionsInternal(evId, now)
                }
                recordAudit(
                    "InstallmentPurchase",
                    purchaseId,
                    "Parcelamento excluído",
                    "Parcelas em aberto removidas; parcelas já pagas preservadas no histórico."
                )
            }
            notifyWidgetsChanged()
        }

    // --- Contas Recorrentes (geracao idempotente) ---

    suspend fun saveRecurringRule(
        title: String,
        type: TransactionType,
        amountCents: Long,
        categoryId: String?,
        paymentMethod: PaymentMethod,
        creditCardId: String?,
        frequency: RecurrenceFrequency,
        customIntervalDays: Int = 30,
        startDate: LocalDate,
        endDate: LocalDate? = null,
        maxOccurrences: Int? = null,
        notes: String = ""
    ): Result<String> = safeDbCall("Contas Recorrentes") {
        val cleanTitle = title.trim()
        require(cleanTitle.isNotEmpty()) { "O título da recorrência é obrigatório." }
        require(amountCents > 0L) { "O valor deve ser maior que zero." }
        val now = DateTimeUtils.nowEpochMillis()
        val ruleId = UUID.randomUUID().toString()
        val rule = RecurringRuleEntity(
            id = ruleId,
            title = cleanTitle,
            type = type,
            amountCents = amountCents,
            categoryId = categoryId,
            paymentMethod = paymentMethod,
            creditCardId = if (paymentMethod == PaymentMethod.CREDIT_CARD) creditCardId else null,
            frequency = frequency,
            customIntervalDays = customIntervalDays.coerceAtLeast(1),
            startAt = DateTimeUtils.localDateTimeToEpoch(startDate, LocalTime.of(9, 0)),
            endAt = endDate?.let { DateTimeUtils.localDateToEndOfDayEpoch(it) },
            maxOccurrences = maxOccurrences?.coerceAtLeast(1),
            isActive = true,
            notes = notes.trim(),
            createdAt = now,
            updatedAt = now
        )
        var createdTransactions: List<TransactionEntity> = emptyList()
        database.withTransaction {
            recurringRuleDao.upsert(rule)
            createdTransactions = generateOccurrencesForRuleInternal(rule, now)
        }
        createdTransactions.forEach { syncTransactionReminder(it, overrideMinutesBefore = null) }
        notifyWidgetsChanged()
        ruleId
    }

    suspend fun generateAllUpcomingRecurringOccurrences(): Result<Unit> =
        safeDbCall("Contas Recorrentes") {
            val now = DateTimeUtils.nowEpochMillis()
            val rules = recurringRuleDao.getAllOnce().filter { it.isActive }
            var createdTransactions: List<TransactionEntity> = emptyList()
            database.withTransaction {
                rules.forEach { rule ->
                    createdTransactions = createdTransactions + generateOccurrencesForRuleInternal(rule, now)
                }
            }
            createdTransactions.forEach { syncTransactionReminder(it, overrideMinutesBefore = null) }
            if (createdTransactions.isNotEmpty()) {
                notifyWidgetsChanged()
            }
        }

    suspend fun toggleRecurringRuleActive(rule: RecurringRuleEntity): Result<Unit> =
        safeDbCall("Contas Recorrentes") {
            val now = DateTimeUtils.nowEpochMillis()
            val updated = rule.copy(
                isActive = !rule.isActive,
                updatedAt = now
            )
            var createdTransactions: List<TransactionEntity> = emptyList()
            database.withTransaction {
                recurringRuleDao.upsert(updated)
                if (updated.isActive) {
                    createdTransactions = generateOccurrencesForRuleInternal(updated, now)
                }
            }
            createdTransactions.forEach { syncTransactionReminder(it, overrideMinutesBefore = null) }
            notifyWidgetsChanged()
        }

    suspend fun deleteRecurringRule(ruleId: String): Result<Unit> =
        safeDbCall("Contas Recorrentes") {
            val now = DateTimeUtils.nowEpochMillis()
            val allTxForRule = transactionDao.getAllOnce().filter { it.recurringRuleId == ruleId }
            val nonSettled = allTxForRule.filter { it.status != FinancialStatus.SETTLED }
            nonSettled.forEach { cancelAndDeleteRemindersForSource(ReminderSourceType.TRANSACTION, it.id) }

            database.withTransaction {
                transactionDao.deleteNonSettledByRecurringRule(ruleId)
                transactionDao.unlinkRecurringRule(ruleId, now)
                recurringRuleDao.deleteById(ruleId)
                recordAudit(
                    "RecurringRule",
                    ruleId,
                    "Recorrência excluída",
                    "Lançamentos pendentes removidos; histórico liquidado preservado."
                )
            }
            notifyWidgetsChanged()
        }

    private suspend fun generateOccurrencesForRuleInternal(
        rule: RecurringRuleEntity,
        now: Long
    ): List<TransactionEntity> {
        val horizonDate = DateTimeUtils.todayLocalDate().plusMonths(6)
        val startDate = DateTimeUtils.epochToLocalDate(rule.startAt)
        val endDate = rule.endAt?.let { DateTimeUtils.epochToLocalDate(it) }
        val maxCap = (rule.maxOccurrences ?: 60).coerceAtMost(60)
        val generated = mutableListOf<TransactionEntity>()

        var index = 0
        var currentDate = startDate
        while (index < maxCap && !currentDate.isAfter(horizonDate)) {
            if (endDate != null && currentDate.isAfter(endDate)) break
            val existing = transactionDao.getRecurringOccurrence(rule.id, index)
            if (existing == null) {
                val dueEpoch = DateTimeUtils.localDateTimeToEpoch(currentDate, LocalTime.of(9, 0))
                val stmtId = if (rule.creditCardId != null) {
                    ensureStatementForCardAndDate(rule.creditCardId, dueEpoch, now)?.id
                } else null
                val tx = TransactionEntity(
                    id = UUID.randomUUID().toString(),
                    type = rule.type,
                    description = rule.title,
                    amountCents = rule.amountCents,
                    categoryId = rule.categoryId,
                    status = FinancialStatus.PENDING,
                    paymentMethod = rule.paymentMethod,
                    occurredAt = dueEpoch,
                    dueAt = dueEpoch,
                    creditCardId = rule.creditCardId,
                    statementId = stmtId,
                    recurringRuleId = rule.id,
                    occurrenceIndex = index,
                    notes = rule.notes,
                    createdAt = now,
                    updatedAt = now
                )
                val insertedRow = transactionDao.insertIgnore(tx)
                if (insertedRow != -1L) {
                    generated.add(tx)
                }
            }
            index++
            currentDate = advanceDateByFrequency(currentDate, rule.frequency, rule.customIntervalDays)
        }
        return generated
    }

    // --- Emprestimos, Orcamentos e Metas ---

    suspend fun saveLoan(
        id: String? = null,
        creditor: String,
        originalAmountCents: Long,
        contractedAmountCents: Long,
        installmentsTotal: Int,
        installmentAmountCents: Long,
        paidInstallments: Int,
        nextDueAt: Long?,
        remainingBalanceCents: Long,
        finalDateAt: Long?,
        notes: String = ""
    ): Result<String> = safeDbCall("Empréstimos") {
        val cleanCreditor = creditor.trim()
        require(cleanCreditor.isNotEmpty()) { "O nome do credor é obrigatório." }
        val now = DateTimeUtils.nowEpochMillis()
        val existing = id?.let { loanDao.getById(it) }
        val loanId = existing?.id ?: UUID.randomUUID().toString()
        loanDao.upsert(
            LoanEntity(
                id = loanId,
                creditor = cleanCreditor,
                originalAmountCents = originalAmountCents.coerceAtLeast(0L),
                contractedAmountCents = contractedAmountCents.coerceAtLeast(0L),
                installmentsTotal = installmentsTotal.coerceAtLeast(1),
                installmentAmountCents = installmentAmountCents.coerceAtLeast(0L),
                paidInstallments = paidInstallments.coerceIn(0, installmentsTotal.coerceAtLeast(1)),
                nextDueAt = nextDueAt,
                remainingBalanceCents = remainingBalanceCents.coerceAtLeast(0L),
                finalDateAt = finalDateAt,
                notes = notes.trim(),
                createdAt = existing?.createdAt ?: now,
                updatedAt = now
            )
        )
        loanId
    }

    suspend fun registerLoanInstallmentPaid(loan: LoanEntity, newRemainingBalanceCents: Long): Result<Unit> =
        safeDbCall("Empréstimos") {
            val now = DateTimeUtils.nowEpochMillis()
            val nextPaid = (loan.paidInstallments + 1).coerceAtMost(loan.installmentsTotal)
            val nextDue = loan.nextDueAt?.let {
                val d = DateTimeUtils.epochToLocalDate(it).plusMonths(1)
                DateTimeUtils.localDateTimeToEpoch(d, LocalTime.of(12, 0))
            }
            database.withTransaction {
                loanDao.upsert(
                    loan.copy(
                        paidInstallments = nextPaid,
                        remainingBalanceCents = newRemainingBalanceCents.coerceAtLeast(0L),
                        nextDueAt = if (nextPaid >= loan.installmentsTotal) null else nextDue,
                        updatedAt = now
                    )
                )
                recordAudit(
                    "Loan",
                    loan.id,
                    "Parcela de empréstimo registrada",
                    "${loan.creditor} · $nextPaid/${loan.installmentsTotal} · Saldo informado: ${DateTimeUtils.formatBrl(newRemainingBalanceCents)}"
                )
            }
        }

    suspend fun deleteLoan(loanId: String): Result<Unit> = safeDbCall("Empréstimos") {
        loanDao.deleteById(loanId)
    }

    suspend fun saveBudget(
        categoryId: String,
        yearMonth: String,
        limitCents: Long
    ): Result<Unit> = safeDbCall("Orçamentos") {
        require(limitCents > 0L) { "O limite do orçamento deve ser maior que zero." }
        val now = DateTimeUtils.nowEpochMillis()
        val existing = budgetDao.getByCategoryAndMonth(categoryId, yearMonth)
        budgetDao.upsert(
            BudgetEntity(
                id = existing?.id ?: UUID.randomUUID().toString(),
                categoryId = categoryId,
                yearMonth = yearMonth,
                limitCents = limitCents,
                createdAt = existing?.createdAt ?: now,
                updatedAt = now
            )
        )
    }

    suspend fun deleteBudget(budgetId: String): Result<Unit> = safeDbCall("Orçamentos") {
        budgetDao.deleteById(budgetId)
    }

    suspend fun saveFinancialGoal(
        id: String? = null,
        name: String,
        targetAmountCents: Long,
        currentAmountCents: Long,
        deadlineAt: Long?,
        categoryId: String? = null,
        notes: String = ""
    ): Result<String> = safeDbCall("Metas Financeiras") {
        val cleanName = name.trim()
        require(cleanName.isNotEmpty()) { "O nome da meta é obrigatório." }
        require(targetAmountCents > 0L) { "O valor desejado deve ser maior que zero." }
        val now = DateTimeUtils.nowEpochMillis()
        val existing = id?.let { financialGoalDao.getById(it) }
        val goalId = existing?.id ?: UUID.randomUUID().toString()
        financialGoalDao.upsert(
            FinancialGoalEntity(
                id = goalId,
                name = cleanName,
                targetAmountCents = targetAmountCents,
                currentAmountCents = currentAmountCents.coerceAtLeast(0L),
                deadlineAt = deadlineAt,
                categoryId = categoryId,
                notes = notes.trim(),
                createdAt = existing?.createdAt ?: now,
                updatedAt = now
            )
        )
        notifyWidgetsChanged()
        goalId
    }

    suspend fun updateFinancialGoalAmount(goal: FinancialGoalEntity, newCurrentCents: Long): Result<Unit> =
        safeDbCall("Metas Financeiras") {
            financialGoalDao.upsert(
                goal.copy(
                    currentAmountCents = newCurrentCents.coerceAtLeast(0L),
                    updatedAt = DateTimeUtils.nowEpochMillis()
                )
            )
            notifyWidgetsChanged()
        }

    suspend fun deleteFinancialGoal(goalId: String): Result<Unit> = safeDbCall("Metas Financeiras") {
        financialGoalDao.deleteById(goalId)
        notifyWidgetsChanged()
    }

    // --- Backup, CSV e Diagnostico ---

    suspend fun exportBackupJson(): Result<String> = safeDbCall("Backup (Exportação JSON)") {
        backupManager.exportToJson()
    }

    suspend fun exportFinancialCsv(): Result<String> = safeDbCall("Financeiro (Exportação CSV)") {
        backupManager.exportFinancialCsv()
    }

    fun validateBackupJson(rawJson: String): BackupValidationResult =
        backupManager.validateAndParse(rawJson)

    suspend fun restoreBackupAtomically(payload: NexusBackupPayload): Result<RestoreSummary> =
        safeDbCall("Backup (Restauração)") {
            val summary = backupManager.restoreAtomically(payload)
            rescheduleAllRemindersInternal()
            notifyWidgetsChanged()
            summary
        }

    fun validateBackupForMerge(payload: NexusBackupPayload): String? =
        backupManager.validateForMerge(payload)

    suspend fun previewBackupMerge(payload: NexusBackupPayload): Result<MergeSummary> =
        safeDbCall("Backup (Prévia de mesclagem)") {
            backupManager.previewMerge(payload)
        }

    suspend fun mergeBackupAtomically(payload: NexusBackupPayload): Result<MergeSummary> =
        safeDbCall("Backup (Mesclagem)") {
            val summary = backupManager.mergeBackupAtomically(payload)
            rescheduleAllRemindersInternal()
            notifyWidgetsChanged()
            summary
        }

    // --- Camada interna de entrada estruturada (Etapa 2 / Rodada 3) ---

    suspend fun previewStructuredInputJson(rawJson: String): Result<StructuredBatchPreview> =
        safeDbCall("Entrada Estruturada (Prévia JSON)") {
            structuredInputProcessor.previewJson(rawJson)
        }

    suspend fun previewStructuredInputEnvelope(
        envelope: StructuredInputEnvelope
    ): Result<StructuredBatchPreview> =
        safeDbCall("Entrada Estruturada (Prévia)") {
            structuredInputProcessor.previewEnvelope(envelope)
        }

    suspend fun processStructuredInputJson(
        rawJson: String,
        atomicBatch: Boolean = false
    ): Result<StructuredBatchResult> =
        safeDbCall("Entrada Estruturada (JSON)") {
            val result = structuredInputProcessor.processJson(rawJson, atomicBatch = atomicBatch)
            if (result.totalAppliedCount > 0) {
                rescheduleAllRemindersInternal()
                notifyWidgetsChanged()
            }
            result
        }

    suspend fun processStructuredInputEnvelope(
        envelope: StructuredInputEnvelope,
        atomicBatch: Boolean = false
    ): Result<StructuredBatchResult> =
        safeDbCall("Entrada Estruturada") {
            val result = structuredInputProcessor.processEnvelope(envelope, atomicBatch = atomicBatch)
            if (result.totalAppliedCount > 0) {
                rescheduleAllRemindersInternal()
                notifyWidgetsChanged()
            }
            result
        }

    override suspend fun previewInboxEnvelope(
        envelope: StructuredInputEnvelope
    ): Result<StructuredBatchPreview> = previewStructuredInputEnvelope(envelope)

    override suspend fun applyInboxEnvelope(
        envelope: StructuredInputEnvelope
    ): Result<StructuredBatchResult> = processStructuredInputEnvelope(envelope, atomicBatch = false)

    override suspend fun recordInboxImportLog(batchId: String, serializedSummaryJson: String): Result<Unit> =
        safeDbCall("Histórico NEXUS-INBOX") {
            recordAudit(
                entityType = AUDIT_ENTITY_INBOX_IMPORT_LOG,
                entityId = batchId,
                action = "IMPORT_CONFIRMED",
                summary = serializedSummaryJson
            )
        }

    // --- Acoes do Assistente de Rotina e Notificacoes (Fase 4) ---

    suspend fun saveRoutineSettings(settings: RoutineAssistantSettingsEntity): Result<Unit> =
        safeDbCall("Assistente de Rotina") {
            val now = DateTimeUtils.nowEpochMillis()
            val normalized = settings.copy(
                id = RoutineAssistantSettingsEntity.SINGLETON_ID,
                morningBriefingHour = settings.morningBriefingHour.coerceIn(0, 23),
                morningBriefingMinute = settings.morningBriefingMinute.coerceIn(0, 59),
                nightReviewHour = settings.nightReviewHour.coerceIn(0, 23),
                nightReviewMinute = settings.nightReviewMinute.coerceIn(0, 59),
                quietHoursStartHour = settings.quietHoursStartHour.coerceIn(0, 23),
                quietHoursStartMinute = settings.quietHoursStartMinute.coerceIn(0, 59),
                quietHoursEndHour = settings.quietHoursEndHour.coerceIn(0, 23),
                quietHoursEndMinute = settings.quietHoursEndMinute.coerceIn(0, 59),
                updatedAt = now
            )
            routineSettingsDao.upsert(normalized)
            rescheduleAllRemindersInternal()
        }

    suspend fun postponeTaskToTomorrow(taskId: String): Result<Unit> =
        safeDbCall("Assistente de Rotina") {
            val task = taskDao.getById(taskId)
                ?: throw IllegalStateException("Tarefa não encontrada.")
            val tomorrow = DateTimeUtils.todayLocalDate().plusDays(1)
            val newDue = if (task.hasDueTime && task.dueAt != null) {
                val time = DateTimeUtils.epochToLocalTime(task.dueAt)
                DateTimeUtils.localDateTimeToEpoch(tomorrow, time)
            } else {
                DateTimeUtils.localDateTimeToEpoch(tomorrow, LocalTime.of(12, 0))
            }
            val now = DateTimeUtils.nowEpochMillis()
            val occIso = task.occurrenceDateIso
                ?: if (task.recurrenceId != null) task.dueAt?.let { DateTimeUtils.epochToLocalDate(it).toString() } else null
            val updated = task.copy(
                dueAt = newDue,
                occurrenceDateIso = occIso,
                updatedAt = now
            )
            database.withTransaction {
                taskDao.update(updated)
                val recId = task.recurrenceId
                if (recId != null && occIso != null) {
                    val existingEx = recurrenceExceptionDao.getByRecurrenceAndDate(recId, occIso)
                    recurrenceExceptionDao.upsert(
                        RecurrenceExceptionEntity(
                            id = existingEx?.id ?: UUID.randomUUID().toString(),
                            recurrenceId = recId,
                            occurrenceDateIso = occIso,
                            exceptionType = RecurrenceExceptionType.OVERRIDE,
                            overrideTitle = task.title,
                            overrideDueAt = newDue,
                            taskId = task.id,
                            createdAt = existingEx?.createdAt ?: now,
                            updatedAt = now
                        )
                    )
                }
            }
            syncTaskReminder(updated, overrideMinutesBefore = null)
            recordAudit(
                "RoutineAssistant",
                taskId,
                "Tarefa adiada para amanhã",
                "${task.title} -> ${DateTimeUtils.formatDate(tomorrow)}"
            )
            notifyWidgetsChanged()
        }

    suspend fun postponeTransactionToTomorrow(transactionId: String): Result<Unit> =
        safeDbCall("Assistente de Rotina") {
            val tx = transactionDao.getById(transactionId)
                ?: throw IllegalStateException("Lançamento não encontrado.")
            val tomorrow = DateTimeUtils.todayLocalDate().plusDays(1)
            val newDue = DateTimeUtils.localDateTimeToEpoch(tomorrow, LocalTime.of(12, 0))
            val now = DateTimeUtils.nowEpochMillis()
            val updated = tx.copy(
                dueAt = newDue,
                status = if (tx.status == FinancialStatus.OVERDUE) FinancialStatus.PENDING else tx.status,
                updatedAt = now
            )
            transactionDao.upsert(updated)
            syncTransactionReminder(updated, overrideMinutesBefore = null)
            recordAudit(
                "RoutineAssistant",
                transactionId,
                "Lançamento adiado para amanhã",
                "${tx.description} -> ${DateTimeUtils.formatDate(tomorrow)}"
            )
            notifyWidgetsChanged()
        }

    suspend fun markBriefingReviewed(type: BriefingType): Result<Unit> =
        safeDbCall("Assistente de Rotina") {
            val todayIso = DateTimeUtils.todayLocalDate().toString()
            val now = DateTimeUtils.nowEpochMillis()
            val existing = briefingHistoryDao.getByTypeAndDate(type, todayIso)
            val entry = if (existing != null) {
                existing.copy(reviewedAt = now, updatedAt = now)
            } else {
                buildBriefingHistoryEntry(type, todayIso, reviewedAt = now, now = now)
            }
            briefingHistoryDao.upsert(entry)
        }

    suspend fun snoozeReminder(reminderId: String, minutes: Int = 15): Result<Unit> =
        safeDbCall("Notificações") {
            val reminder = reminderDao.getById(reminderId) ?: return@safeDbCall
            if (ReminderRules.isEffectivelyCancelled(reminder)) {
                notificationScheduler?.cancelAlarm(reminderId)
                notificationScheduler?.cancelDisplayedNotification(reminderId)
                return@safeDbCall
            }
            val now = DateTimeUtils.nowEpochMillis()
            val nextTrigger = now + (minutes.coerceAtLeast(1).toLong() * 60_000L)
            val updated = reminder.copy(
                triggerAt = nextTrigger,
                snoozedUntil = nextTrigger,
                status = ReminderStatus.ACTIVE,
                updatedAt = now
            )
            val exact = notificationScheduler?.scheduleAlarm(updated) ?: false
            reminderDao.upsert(updated.copy(isExactScheduled = exact))
            notificationScheduler?.cancelDisplayedNotification(reminderId)
        }

    suspend fun dismissReminder(reminderId: String): Result<Unit> =
        safeDbCall("Notificações") {
            val reminder = reminderDao.getById(reminderId) ?: return@safeDbCall
            notificationScheduler?.cancelAlarm(reminderId)
            notificationScheduler?.cancelDisplayedNotification(reminderId)
            reminderDao.upsert(
                reminder.copy(
                    status = ReminderStatus.DISMISSED,
                    updatedAt = DateTimeUtils.nowEpochMillis()
                )
            )
        }

    suspend fun cancelReminder(reminderId: String): Result<Unit> =
        safeDbCall("Notificações") {
            val reminder = reminderDao.getById(reminderId) ?: return@safeDbCall
            notificationScheduler?.cancelAlarm(reminderId)
            notificationScheduler?.cancelDisplayedNotification(reminderId)
            reminderDao.upsert(
                reminder.copy(
                    status = ReminderStatus.CANCELLED,
                    isExactScheduled = false,
                    updatedAt = DateTimeUtils.nowEpochMillis()
                )
            )
        }

    suspend fun completeTaskFromNotification(taskId: String) {
        val task = taskDao.getById(taskId) ?: return
        if (task.status == TaskStatus.COMPLETED) return
        val now = DateTimeUtils.nowEpochMillis()
        val updated = task.copy(
            status = TaskStatus.COMPLETED,
            completedAt = now,
            updatedAt = now
        )
        var materializedNext: Pair<TaskEntity, Int?>? = null
        database.withTransaction {
            taskDao.update(updated)
            materializedNext = materializeNextOccurrenceIfNeededInternal(updated, now)
        }
        syncTaskReminder(updated, overrideMinutesBefore = null)
        materializedNext?.let { (nextTask, recRemMins) ->
            syncTaskReminder(nextTask, overrideMinutesBefore = recRemMins)
        }
        notifyWidgetsChanged()
    }

    suspend fun settleTransactionFromNotification(transactionId: String) {
        val tx = transactionDao.getById(transactionId) ?: return
        if (tx.status == FinancialStatus.SETTLED) return
        val now = DateTimeUtils.nowEpochMillis()
        val updated = tx.copy(
            status = FinancialStatus.SETTLED,
            settledAt = now,
            updatedAt = now
        )
        database.withTransaction {
            transactionDao.upsert(updated)
            if (tx.isTicketObligation && tx.eventId != null) {
                syncEventTicketStatusFromTransactionsInternal(tx.eventId, now)
            }
        }
        syncTransactionReminder(updated, overrideMinutesBefore = null)
        notifyWidgetsChanged()
    }

    suspend fun handleAlarmFired(reminderId: String) {
        withContext(Dispatchers.IO) {
            val reminder = reminderDao.getById(reminderId) ?: return@withContext
            if (!ReminderRules.isSchedulableOrFirable(reminder)) {
                notificationScheduler?.cancelAlarm(reminder.id)
                notificationScheduler?.cancelDisplayedNotification(reminder.id)
                if (reminder.minutesBefore < 0 && reminder.status != ReminderStatus.CANCELLED) {
                    reminderDao.upsert(
                        reminder.copy(
                            status = ReminderStatus.CANCELLED,
                            isExactScheduled = false,
                            updatedAt = DateTimeUtils.nowEpochMillis()
                        )
                    )
                }
                return@withContext
            }
            val now = DateTimeUtils.nowEpochMillis()
            val settings = getOrCreateRoutineSettings(now)
            if (!settings.notificationsEnabled || !isChannelEnabled(settings, reminder.channelId)) {
                return@withContext
            }

            // Verifica se o registro de origem ainda demanda notificacao
            val stillValid = isSourceStillEligibleForNotification(reminder)
            if (!stillValid) {
                reminderDao.upsert(
                    reminder.copy(status = ReminderStatus.CANCELLED, updatedAt = now)
                )
                return@withContext
            }

            val userName = settingsDao.getSettings()?.userName?.ifBlank { "Enzo" } ?: "Enzo"
            var resolvedReminder = reminder

            if (reminder.sourceType == ReminderSourceType.ROUTINE_MORNING) {
                val entry = buildBriefingHistoryEntry(BriefingType.MORNING, DateTimeUtils.todayLocalDate().toString(), null, now)
                briefingHistoryDao.upsert(entry)
                resolvedReminder = reminder.copy(message = entry.summaryText)
                val nextTrigger = NexusNotificationScheduler.computeNextDailyRoutineEpoch(
                    settings.morningBriefingHour,
                    settings.morningBriefingMinute,
                    now + 60_000L
                )
                val nextEntity = resolvedReminder.copy(
                    triggerAt = nextTrigger,
                    status = ReminderStatus.ACTIVE,
                    firedAt = now,
                    updatedAt = now
                )
                val exact = notificationScheduler?.scheduleAlarm(nextEntity) ?: false
                reminderDao.upsert(nextEntity.copy(isExactScheduled = exact))
            } else if (reminder.sourceType == ReminderSourceType.ROUTINE_NIGHT) {
                val entry = buildBriefingHistoryEntry(BriefingType.NIGHT_REVIEW, DateTimeUtils.todayLocalDate().toString(), null, now)
                briefingHistoryDao.upsert(entry)
                resolvedReminder = reminder.copy(message = entry.summaryText)
                val nextTrigger = NexusNotificationScheduler.computeNextDailyRoutineEpoch(
                    settings.nightReviewHour,
                    settings.nightReviewMinute,
                    now + 60_000L
                )
                val nextEntity = resolvedReminder.copy(
                    triggerAt = nextTrigger,
                    status = ReminderStatus.ACTIVE,
                    firedAt = now,
                    updatedAt = now
                )
                val exact = notificationScheduler?.scheduleAlarm(nextEntity) ?: false
                reminderDao.upsert(nextEntity.copy(isExactScheduled = exact))
            } else {
                reminderDao.upsert(
                    reminder.copy(
                        status = ReminderStatus.FIRED,
                        firedAt = now,
                        updatedAt = now
                    )
                )
            }

            notificationScheduler?.showReminderNotification(resolvedReminder, userName)
        }
    }

    suspend fun rescheduleAllReminders(): Result<Unit> = safeDbCall("Notificações") {
        rescheduleAllRemindersInternal()
    }

    suspend fun sendTestNotification(): Result<Boolean> = safeDbCall("Notificações") {
        val today = DateTimeUtils.todayLocalDate()
        val now = DateTimeUtils.nowEpochMillis()
        val tasksPending = taskDao.getAllOnce().count { !it.status.isTerminal }
        val apptsToday = AgendaEngine.expand(
            window = DateRange(today, today),
            sources = AgendaSources(
                appointments = appointmentDao.getAllOnce(),
                taskRecurrences = taskRecurrenceDao.getAllOnce(),
                recurrenceExceptions = recurrenceExceptionDao.getAllOnce()
            ),
            filters = AgendaFilters(kindFilter = AgendaOccurrenceKind.APPOINTMENT),
            today = today,
            nowEpoch = now
        ).size
        val summary = "Canal local ativo · $tasksPending tarefas pendentes · $apptsToday compromissos hoje."
        notificationScheduler?.showTestNotification(summary) ?: false
    }

    suspend fun syncPeriodicOfflineSafety() {
        withContext(Dispatchers.IO) {
            generateAllUpcomingRecurringOccurrences()
            syncRecurringScheduleRemindersInternal()
            val now = DateTimeUtils.nowEpochMillis()
            val settings = getOrCreateRoutineSettings(now)
            if (!settings.notificationsEnabled) return@withContext

            val activeReminders = reminderDao.getByStatus(ReminderStatus.ACTIVE)
            activeReminders.forEach { reminder ->
                if (ReminderRules.isEffectivelyCancelled(reminder)) {
                    notificationScheduler?.cancelAlarm(reminder.id)
                    notificationScheduler?.cancelDisplayedNotification(reminder.id)
                    reminderDao.upsert(
                        reminder.copy(
                            status = ReminderStatus.CANCELLED,
                            isExactScheduled = false,
                            updatedAt = now
                        )
                    )
                } else if (reminder.triggerAt in (now - 1_800_000L)..now && reminder.firedAt == null) {
                    handleAlarmFired(reminder.id)
                } else if (reminder.triggerAt > now && isChannelEnabled(settings, reminder.channelId)) {
                    val exact = notificationScheduler?.scheduleAlarm(reminder) ?: false
                    if (exact != reminder.isExactScheduled) {
                        reminderDao.upsert(reminder.copy(isExactScheduled = exact, updatedAt = now))
                    }
                }
            }
            notifyWidgetsChanged()
        }
    }

    private suspend fun rescheduleAllRemindersInternal() {
        val now = DateTimeUtils.nowEpochMillis()
        val settings = getOrCreateRoutineSettings(now)

        if (!settings.notificationsEnabled) {
            reminderDao.getByStatus(ReminderStatus.ACTIVE).forEach { r ->
                notificationScheduler?.cancelAlarm(r.id)
            }
            return
        }

        // Sincroniza lembretes de rotina (Briefing Diário e Revisão Noturna)
        syncRoutineScheduleReminder(
            sourceType = ReminderSourceType.ROUTINE_MORNING,
            sourceId = NexusNotificationScheduler.ROUTINE_MORNING_SOURCE_ID,
            enabled = settings.morningBriefingEnabled && settings.routineChannelEnabled,
            hour = settings.morningBriefingHour,
            minute = settings.morningBriefingMinute,
            title = "Briefing Diário",
            defaultMessage = "Veja seu resumo de compromissos, tarefas e vencimentos de hoje.",
            now = now
        )
        syncRoutineScheduleReminder(
            sourceType = ReminderSourceType.ROUTINE_NIGHT,
            sourceId = NexusNotificationScheduler.ROUTINE_NIGHT_SOURCE_ID,
            enabled = settings.nightReviewEnabled && settings.routineChannelEnabled,
            hour = settings.nightReviewHour,
            minute = settings.nightReviewMinute,
            title = "Revisão Noturna",
            defaultMessage = "Feche pendências de hoje e prepare a agenda de amanhã.",
            now = now
        )

        // Sincroniza janela rolante (14 dias) de lembretes virtuais de compromissos e eventos recorrentes
        syncRecurringScheduleRemindersInternal()

        // Reagenda lembretes ativos existentes respeitando modo silencio e canais
        val active = reminderDao.getByStatus(ReminderStatus.ACTIVE)
        active.forEach { reminder ->
            if (ReminderRules.isEffectivelyCancelled(reminder) || !isSourceStillEligibleForNotification(reminder)) {
                notificationScheduler?.cancelAlarm(reminder.id)
                notificationScheduler?.cancelDisplayedNotification(reminder.id)
                reminderDao.upsert(
                    reminder.copy(
                        status = ReminderStatus.CANCELLED,
                        isExactScheduled = false,
                        updatedAt = now
                    )
                )
            } else if (!isChannelEnabled(settings, reminder.channelId)) {
                notificationScheduler?.cancelAlarm(reminder.id)
            } else {
                val targetTrigger = if (reminder.category == ReminderCategory.ROUTINE) {
                    reminder.triggerAt
                } else {
                    NexusNotificationScheduler.adjustForQuietHours(reminder.triggerAt, settings)
                }
                if (targetTrigger > now) {
                    val updated = reminder.copy(triggerAt = targetTrigger, updatedAt = now)
                    val exact = notificationScheduler?.scheduleAlarm(updated) ?: false
                    reminderDao.upsert(updated.copy(isExactScheduled = exact))
                }
            }
        }
    }

    private suspend fun syncRoutineScheduleReminder(
        sourceType: ReminderSourceType,
        sourceId: String,
        enabled: Boolean,
        hour: Int,
        minute: Int,
        title: String,
        defaultMessage: String,
        now: Long
    ) {
        val existing = reminderDao.getBySourceAndCategory(sourceType, sourceId, ReminderCategory.ROUTINE)
        if (!enabled) {
            if (existing != null) {
                notificationScheduler?.cancelAlarm(existing.id)
                reminderDao.upsert(existing.copy(status = ReminderStatus.CANCELLED, updatedAt = now))
            }
            return
        }
        val nextTrigger = NexusNotificationScheduler.computeNextDailyRoutineEpoch(hour, minute, now)
        val reminder = ReminderEntity(
            id = existing?.id ?: UUID.randomUUID().toString(),
            sourceType = sourceType,
            sourceId = sourceId,
            category = ReminderCategory.ROUTINE,
            title = title,
            message = defaultMessage,
            triggerAt = nextTrigger,
            originalDueAt = nextTrigger,
            minutesBefore = 0,
            channelId = NexusNotificationChannels.CHANNEL_ROUTINE,
            status = ReminderStatus.ACTIVE,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now
        )
        val exact = notificationScheduler?.scheduleAlarm(reminder) ?: false
        reminderDao.upsert(reminder.copy(isExactScheduled = exact))
    }

    private suspend fun syncTaskReminder(task: TaskEntity, overrideMinutesBefore: Int?) {
        val existing = reminderDao.getBySourceAndCategory(
            ReminderSourceType.TASK,
            task.id,
            ReminderCategory.SCHEDULE
        )
        val now = DateTimeUtils.nowEpochMillis()
        if (task.status.isTerminal || task.dueAt == null) {
            if (existing != null && existing.status != ReminderStatus.CANCELLED) {
                notificationScheduler?.cancelAlarm(existing.id)
                notificationScheduler?.cancelDisplayedNotification(existing.id)
                reminderDao.upsert(
                    existing.copy(
                        status = ReminderStatus.CANCELLED,
                        isExactScheduled = false,
                        updatedAt = now
                    )
                )
            }
            return
        }

        val nextTitle = "Tarefa: ${task.title}"
        val nextMsg = if (task.hasDueTime) {
            "Prazo às ${DateTimeUtils.formatTime(task.dueAt)}"
        } else {
            "Vence em ${DateTimeUtils.formatDate(task.dueAt)}"
        }

        when (ReminderRules.resolveReminderSyncAction(existing, task.dueAt, overrideMinutesBefore)) {
            ReminderSyncAction.DISABLE -> {
                val settings = getOrCreateRoutineSettings(now)
                if (existing != null) {
                    notificationScheduler?.cancelAlarm(existing.id)
                    notificationScheduler?.cancelDisplayedNotification(existing.id)
                }
                reminderDao.upsert(
                    ReminderEntity(
                        id = existing?.id ?: UUID.randomUUID().toString(),
                        sourceType = ReminderSourceType.TASK,
                        sourceId = task.id,
                        category = ReminderCategory.SCHEDULE,
                        title = nextTitle,
                        message = nextMsg,
                        triggerAt = task.dueAt,
                        originalDueAt = task.dueAt,
                        minutesBefore = ReminderRules.resolveDisabledMinutesBefore(
                            existing = existing,
                            fallbackDefaultMinutes = settings.defaultTaskReminderMinutes
                        ),
                        channelId = NexusNotificationChannels.CHANNEL_SCHEDULE,
                        status = ReminderStatus.CANCELLED,
                        isExactScheduled = false,
                        createdAt = existing?.createdAt ?: now,
                        updatedAt = now
                    )
                )
                return
            }
            ReminderSyncAction.PRESERVE_UNTOUCHED -> {
                if (existing != null && !ReminderRules.isEffectivelyCancelled(existing) &&
                    (existing.title != nextTitle || existing.message != nextMsg)
                ) {
                    reminderDao.upsert(existing.copy(title = nextTitle, message = nextMsg, updatedAt = now))
                }
                return
            }
            ReminderSyncAction.RESCHEDULE -> Unit
        }

        val settings = getOrCreateRoutineSettings(now)
        val minutes = overrideMinutesBefore
            ?: existing?.takeIf { !ReminderRules.isEffectivelyCancelled(it) }?.minutesBefore
            ?: settings.defaultTaskReminderMinutes

        val rawTrigger = NexusNotificationScheduler.computeReminderTriggerEpoch(
            dueEpoch = task.dueAt,
            hasSpecificTime = task.hasDueTime,
            minutesBefore = minutes
        )
        val triggerAt = NexusNotificationScheduler.adjustForQuietHours(rawTrigger, settings)
        if (triggerAt <= now) {
            if (existing != null && existing.status == ReminderStatus.ACTIVE) {
                notificationScheduler?.cancelAlarm(existing.id)
                reminderDao.upsert(
                    existing.copy(
                        status = ReminderStatus.CANCELLED,
                        isExactScheduled = false,
                        updatedAt = now
                    )
                )
            }
            return
        }

        val entity = ReminderEntity(
            id = existing?.id ?: UUID.randomUUID().toString(),
            sourceType = ReminderSourceType.TASK,
            sourceId = task.id,
            category = ReminderCategory.SCHEDULE,
            title = nextTitle,
            message = nextMsg,
            triggerAt = triggerAt,
            originalDueAt = task.dueAt,
            minutesBefore = minutes,
            channelId = NexusNotificationChannels.CHANNEL_SCHEDULE,
            status = ReminderStatus.ACTIVE,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now
        )
        val shouldSchedule = settings.notificationsEnabled && settings.scheduleChannelEnabled
        val exact = if (shouldSchedule) notificationScheduler?.scheduleAlarm(entity) ?: false else false
        reminderDao.upsert(entity.copy(isExactScheduled = exact))
    }

    private suspend fun syncAppointmentReminder(appointment: AppointmentEntity, overrideMinutesBefore: Int?) {
        val existing = reminderDao.getBySourceAndCategory(
            ReminderSourceType.APPOINTMENT,
            appointment.id,
            ReminderCategory.SCHEDULE
        )
        val now = DateTimeUtils.nowEpochMillis()
        val nextTitle = appointment.title
        val nextMsg = buildString {
            append(if (appointment.isAllDay) "Dia inteiro" else "${DateTimeUtils.formatTime(appointment.startAt)} - ${DateTimeUtils.formatTime(appointment.endAt)}")
            if (appointment.location.isNotBlank()) append(" · ${appointment.location}")
        }

        when (ReminderRules.resolveReminderSyncAction(existing, appointment.startAt, overrideMinutesBefore)) {
            ReminderSyncAction.DISABLE -> {
                val settings = getOrCreateRoutineSettings(now)
                if (existing != null) {
                    notificationScheduler?.cancelAlarm(existing.id)
                    notificationScheduler?.cancelDisplayedNotification(existing.id)
                }
                reminderDao.upsert(
                    ReminderEntity(
                        id = existing?.id ?: UUID.randomUUID().toString(),
                        sourceType = ReminderSourceType.APPOINTMENT,
                        sourceId = appointment.id,
                        category = ReminderCategory.SCHEDULE,
                        title = nextTitle,
                        message = nextMsg,
                        triggerAt = appointment.startAt,
                        originalDueAt = appointment.startAt,
                        minutesBefore = ReminderRules.resolveDisabledMinutesBefore(
                            existing = existing,
                            fallbackDefaultMinutes = settings.defaultAppointmentReminderMinutes
                        ),
                        channelId = NexusNotificationChannels.CHANNEL_SCHEDULE,
                        status = ReminderStatus.CANCELLED,
                        isExactScheduled = false,
                        createdAt = existing?.createdAt ?: now,
                        updatedAt = now
                    )
                )
                return
            }
            ReminderSyncAction.PRESERVE_UNTOUCHED -> {
                if (existing != null && !ReminderRules.isEffectivelyCancelled(existing) &&
                    (existing.title != nextTitle || existing.message != nextMsg)
                ) {
                    reminderDao.upsert(existing.copy(title = nextTitle, message = nextMsg, updatedAt = now))
                }
                return
            }
            ReminderSyncAction.RESCHEDULE -> Unit
        }

        val settings = getOrCreateRoutineSettings(now)
        val minutes = overrideMinutesBefore
            ?: existing?.takeIf { !ReminderRules.isEffectivelyCancelled(it) }?.minutesBefore
            ?: settings.defaultAppointmentReminderMinutes

        val rawTrigger = NexusNotificationScheduler.computeReminderTriggerEpoch(
            dueEpoch = appointment.startAt,
            hasSpecificTime = !appointment.isAllDay,
            minutesBefore = minutes
        )
        val triggerAt = NexusNotificationScheduler.adjustForQuietHours(rawTrigger, settings)
        if (triggerAt <= now) {
            if (existing != null && existing.status == ReminderStatus.ACTIVE) {
                notificationScheduler?.cancelAlarm(existing.id)
                reminderDao.upsert(
                    existing.copy(
                        status = ReminderStatus.CANCELLED,
                        isExactScheduled = false,
                        updatedAt = now
                    )
                )
            }
            return
        }

        val entity = ReminderEntity(
            id = existing?.id ?: UUID.randomUUID().toString(),
            sourceType = ReminderSourceType.APPOINTMENT,
            sourceId = appointment.id,
            category = ReminderCategory.SCHEDULE,
            title = nextTitle,
            message = nextMsg,
            triggerAt = triggerAt,
            originalDueAt = appointment.startAt,
            minutesBefore = minutes,
            channelId = NexusNotificationChannels.CHANNEL_SCHEDULE,
            status = ReminderStatus.ACTIVE,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now
        )
        val shouldSchedule = settings.notificationsEnabled && settings.scheduleChannelEnabled
        val exact = if (shouldSchedule) notificationScheduler?.scheduleAlarm(entity) ?: false else false
        reminderDao.upsert(entity.copy(isExactScheduled = exact))
    }

    private suspend fun syncEventReminder(event: EventEntity, overrideMinutesBefore: Int?) {
        val existing = reminderDao.getBySourceAndCategory(
            ReminderSourceType.EVENT,
            event.id,
            ReminderCategory.SCHEDULE
        )
        val now = DateTimeUtils.nowEpochMillis()
        if (event.eventStatus == EventLifecycleStatus.CANCELLED || event.eventStatus == EventLifecycleStatus.ATTENDED) {
            if (existing != null && existing.status != ReminderStatus.CANCELLED) {
                notificationScheduler?.cancelAlarm(existing.id)
                notificationScheduler?.cancelDisplayedNotification(existing.id)
                reminderDao.upsert(
                    existing.copy(
                        status = ReminderStatus.CANCELLED,
                        isExactScheduled = false,
                        updatedAt = now
                    )
                )
            }
            return
        }

        val nextTitle = "Evento: ${event.name}"
        val nextMsg = buildString {
            append(DateTimeUtils.formatDateTime(event.startAt))
            if (event.locationName.isNotBlank()) append(" · ${event.locationName}")
        }

        when (ReminderRules.resolveReminderSyncAction(existing, event.startAt, overrideMinutesBefore)) {
            ReminderSyncAction.DISABLE -> {
                val settings = getOrCreateRoutineSettings(now)
                if (existing != null) {
                    notificationScheduler?.cancelAlarm(existing.id)
                    notificationScheduler?.cancelDisplayedNotification(existing.id)
                }
                reminderDao.upsert(
                    ReminderEntity(
                        id = existing?.id ?: UUID.randomUUID().toString(),
                        sourceType = ReminderSourceType.EVENT,
                        sourceId = event.id,
                        category = ReminderCategory.SCHEDULE,
                        title = nextTitle,
                        message = nextMsg,
                        triggerAt = event.startAt,
                        originalDueAt = event.startAt,
                        minutesBefore = ReminderRules.resolveDisabledMinutesBefore(
                            existing = existing,
                            fallbackDefaultMinutes = settings.defaultEventReminderMinutes
                        ),
                        channelId = NexusNotificationChannels.CHANNEL_EVENTS,
                        status = ReminderStatus.CANCELLED,
                        isExactScheduled = false,
                        createdAt = existing?.createdAt ?: now,
                        updatedAt = now
                    )
                )
                return
            }
            ReminderSyncAction.PRESERVE_UNTOUCHED -> {
                if (existing != null && !ReminderRules.isEffectivelyCancelled(existing) &&
                    (existing.title != nextTitle || existing.message != nextMsg)
                ) {
                    reminderDao.upsert(existing.copy(title = nextTitle, message = nextMsg, updatedAt = now))
                }
                return
            }
            ReminderSyncAction.RESCHEDULE -> Unit
        }

        val settings = getOrCreateRoutineSettings(now)
        val minutes = overrideMinutesBefore
            ?: existing?.takeIf { !ReminderRules.isEffectivelyCancelled(it) }?.minutesBefore
            ?: settings.defaultEventReminderMinutes

        val rawTrigger = NexusNotificationScheduler.computeReminderTriggerEpoch(
            dueEpoch = event.startAt,
            hasSpecificTime = !event.isAllDay,
            minutesBefore = minutes
        )
        val triggerAt = NexusNotificationScheduler.adjustForQuietHours(rawTrigger, settings)
        if (triggerAt <= now) {
            if (existing != null && existing.status == ReminderStatus.ACTIVE) {
                notificationScheduler?.cancelAlarm(existing.id)
                reminderDao.upsert(
                    existing.copy(
                        status = ReminderStatus.CANCELLED,
                        isExactScheduled = false,
                        updatedAt = now
                    )
                )
            }
            return
        }

        val entity = ReminderEntity(
            id = existing?.id ?: UUID.randomUUID().toString(),
            sourceType = ReminderSourceType.EVENT,
            sourceId = event.id,
            category = ReminderCategory.SCHEDULE,
            title = nextTitle,
            message = nextMsg,
            triggerAt = triggerAt,
            originalDueAt = event.startAt,
            minutesBefore = minutes,
            channelId = NexusNotificationChannels.CHANNEL_EVENTS,
            status = ReminderStatus.ACTIVE,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now
        )
        val shouldSchedule = settings.notificationsEnabled && settings.eventsChannelEnabled
        val exact = if (shouldSchedule) notificationScheduler?.scheduleAlarm(entity) ?: false else false
        reminderDao.upsert(entity.copy(isExactScheduled = exact))
    }

    private suspend fun syncTransactionReminder(tx: TransactionEntity, overrideMinutesBefore: Int?) {
        val existing = reminderDao.getBySourceAndCategory(
            ReminderSourceType.TRANSACTION,
            tx.id,
            ReminderCategory.FINANCIAL
        )
        val now = DateTimeUtils.nowEpochMillis()
        if (tx.status == FinancialStatus.SETTLED || tx.status == FinancialStatus.CANCELLED || tx.dueAt == null) {
            if (existing != null && existing.status != ReminderStatus.CANCELLED) {
                notificationScheduler?.cancelAlarm(existing.id)
                notificationScheduler?.cancelDisplayedNotification(existing.id)
                reminderDao.upsert(
                    existing.copy(
                        status = ReminderStatus.CANCELLED,
                        isExactScheduled = false,
                        updatedAt = now
                    )
                )
            }
            return
        }

        val nextTitle = "${tx.type.label}: ${tx.description}"
        val nextMsg = "Vencimento em ${DateTimeUtils.formatDate(tx.dueAt)} · ${DateTimeUtils.formatBrl(tx.amountCents)}"

        when (ReminderRules.resolveReminderSyncAction(existing, tx.dueAt, overrideMinutesBefore)) {
            ReminderSyncAction.DISABLE -> {
                val settings = getOrCreateRoutineSettings(now)
                if (existing != null) {
                    notificationScheduler?.cancelAlarm(existing.id)
                    notificationScheduler?.cancelDisplayedNotification(existing.id)
                }
                reminderDao.upsert(
                    ReminderEntity(
                        id = existing?.id ?: UUID.randomUUID().toString(),
                        sourceType = ReminderSourceType.TRANSACTION,
                        sourceId = tx.id,
                        category = ReminderCategory.FINANCIAL,
                        title = nextTitle,
                        message = nextMsg,
                        triggerAt = tx.dueAt,
                        originalDueAt = tx.dueAt,
                        minutesBefore = ReminderRules.resolveDisabledMinutesBefore(
                            existing = existing,
                            fallbackDefaultMinutes = settings.defaultFinanceReminderMinutes
                        ),
                        channelId = NexusNotificationChannels.CHANNEL_FINANCE,
                        status = ReminderStatus.CANCELLED,
                        isExactScheduled = false,
                        createdAt = existing?.createdAt ?: now,
                        updatedAt = now
                    )
                )
                return
            }
            ReminderSyncAction.PRESERVE_UNTOUCHED -> {
                if (existing != null && !ReminderRules.isEffectivelyCancelled(existing) &&
                    (existing.title != nextTitle || existing.message != nextMsg)
                ) {
                    reminderDao.upsert(existing.copy(title = nextTitle, message = nextMsg, updatedAt = now))
                }
                return
            }
            ReminderSyncAction.RESCHEDULE -> Unit
        }

        val settings = getOrCreateRoutineSettings(now)
        val minutes = overrideMinutesBefore
            ?: existing?.takeIf { !ReminderRules.isEffectivelyCancelled(it) }?.minutesBefore
            ?: settings.defaultFinanceReminderMinutes

        val rawTrigger = NexusNotificationScheduler.computeReminderTriggerEpoch(
            dueEpoch = tx.dueAt,
            hasSpecificTime = false,
            minutesBefore = minutes
        )
        val triggerAt = NexusNotificationScheduler.adjustForQuietHours(rawTrigger, settings)
        if (triggerAt <= now) {
            if (existing != null && existing.status == ReminderStatus.ACTIVE) {
                notificationScheduler?.cancelAlarm(existing.id)
                reminderDao.upsert(
                    existing.copy(
                        status = ReminderStatus.CANCELLED,
                        isExactScheduled = false,
                        updatedAt = now
                    )
                )
            }
            return
        }

        val entity = ReminderEntity(
            id = existing?.id ?: UUID.randomUUID().toString(),
            sourceType = ReminderSourceType.TRANSACTION,
            sourceId = tx.id,
            category = ReminderCategory.FINANCIAL,
            title = nextTitle,
            message = nextMsg,
            triggerAt = triggerAt,
            originalDueAt = tx.dueAt,
            minutesBefore = minutes,
            channelId = NexusNotificationChannels.CHANNEL_FINANCE,
            status = ReminderStatus.ACTIVE,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now
        )
        val shouldSchedule = settings.notificationsEnabled && settings.financeChannelEnabled
        val exact = if (shouldSchedule) notificationScheduler?.scheduleAlarm(entity) ?: false else false
        reminderDao.upsert(entity.copy(isExactScheduled = exact))
    }

    private suspend fun cancelFinancialRemindersForSource(sourceType: ReminderSourceType, sourceId: String) {
        val existing = reminderDao.getBySourceAndCategory(sourceType, sourceId, ReminderCategory.FINANCIAL)
            ?: return
        notificationScheduler?.cancelAlarm(existing.id)
        notificationScheduler?.cancelDisplayedNotification(existing.id)
        reminderDao.updateStatusForSourceCategory(
            sourceType = sourceType,
            sourceId = sourceId,
            category = ReminderCategory.FINANCIAL,
            newStatus = ReminderStatus.CANCELLED,
            updatedAt = DateTimeUtils.nowEpochMillis()
        )
    }

    private suspend fun cancelAndDeleteRemindersForSource(sourceType: ReminderSourceType, sourceId: String) {
        val items = reminderDao.getBySource(sourceType, sourceId)
        items.forEach { reminder ->
            notificationScheduler?.cancelAlarm(reminder.id)
            notificationScheduler?.cancelDisplayedNotification(reminder.id)
            reminderDao.deleteById(reminder.id)
        }
    }

    private suspend fun cancelAndDeleteVirtualRemindersForRecurrence(
        sourceType: ReminderSourceType,
        recurrenceId: String
    ) {
        val matching = reminderDao.getAllOnce().filter { reminder ->
            reminder.sourceType == sourceType &&
                RecurrenceEngine.parseVirtualOccurrenceId(reminder.sourceId)?.first == recurrenceId
        }
        matching.forEach { reminder ->
            notificationScheduler?.cancelAlarm(reminder.id)
            notificationScheduler?.cancelDisplayedNotification(reminder.id)
            reminderDao.deleteById(reminder.id)
        }
    }

    private suspend fun syncRecurringScheduleRemindersInternal() {
        val now = DateTimeUtils.nowEpochMillis()
        val settings = getOrCreateRoutineSettings(now)
        val today = DateTimeUtils.todayLocalDate()
        val reminderWindow = DateRange(today, today.plusDays(14))

        val activeRecurrences = taskRecurrenceDao.getActiveOnce()
        val allExceptions = recurrenceExceptionDao.getAllOnce()
        val allAppointments = appointmentDao.getAllOnce()
        val allEvents = eventDao.getAllOnce()

        val desiredKeys = hashSetOf<Pair<ReminderSourceType, String>>()

        val virtualAppts = RecurrenceEngine.projectVirtualAppointments(
            window = reminderWindow,
            recurrences = activeRecurrences,
            exceptions = allExceptions,
            materializedAppointments = allAppointments
        )
        virtualAppts.forEach { proj ->
            val remPref = proj.recurrence.reminderMinutesBefore
            if (remPref != null && remPref < 0) return@forEach
            val vSourceId = RecurrenceEngine.virtualOccurrenceId(proj.recurrence.id, proj.occurrenceDateIso)
            val minutes = remPref ?: settings.defaultAppointmentReminderMinutes
            val rawTrigger = NexusNotificationScheduler.computeReminderTriggerEpoch(
                dueEpoch = proj.startAt,
                hasSpecificTime = !proj.isAllDay,
                minutesBefore = minutes
            )
            val triggerAt = NexusNotificationScheduler.adjustForQuietHours(rawTrigger, settings)
            if (triggerAt <= now) return@forEach

            desiredKeys += ReminderSourceType.APPOINTMENT to vSourceId
            val existing = reminderDao.getBySourceAndCategory(
                ReminderSourceType.APPOINTMENT,
                vSourceId,
                ReminderCategory.SCHEDULE
            )
            if (existing != null &&
                (ReminderRules.isEffectivelyCancelled(existing) ||
                    existing.status == ReminderStatus.DISMISSED ||
                    (existing.snoozedUntil != null && existing.snoozedUntil > now && existing.originalDueAt == proj.startAt))
            ) {
                return@forEach
            }
            val nextMsg = buildString {
                append(
                    if (proj.isAllDay) {
                        "Dia inteiro"
                    } else {
                        "${DateTimeUtils.formatTime(proj.startAt)} - ${DateTimeUtils.formatTime(proj.endAt)}"
                    }
                )
                if (proj.effectiveLocation.isNotBlank()) append(" · ${proj.effectiveLocation}")
            }
            val entity = ReminderEntity(
                id = existing?.id ?: UUID.randomUUID().toString(),
                sourceType = ReminderSourceType.APPOINTMENT,
                sourceId = vSourceId,
                category = ReminderCategory.SCHEDULE,
                title = proj.effectiveTitle,
                message = nextMsg,
                triggerAt = triggerAt,
                originalDueAt = proj.startAt,
                minutesBefore = minutes,
                channelId = NexusNotificationChannels.CHANNEL_SCHEDULE,
                status = ReminderStatus.ACTIVE,
                createdAt = existing?.createdAt ?: now,
                updatedAt = now
            )
            val shouldSchedule = settings.notificationsEnabled && settings.scheduleChannelEnabled
            val exact = if (shouldSchedule) notificationScheduler?.scheduleAlarm(entity) ?: false else false
            reminderDao.upsert(entity.copy(isExactScheduled = exact))
        }

        val virtualEvents = RecurrenceEngine.projectVirtualEvents(
            window = reminderWindow,
            recurrences = activeRecurrences,
            exceptions = allExceptions,
            materializedEvents = allEvents
        )
        virtualEvents.forEach { proj ->
            if (proj.recurrence.eventStatus == EventLifecycleStatus.CANCELLED ||
                proj.recurrence.eventStatus == EventLifecycleStatus.ATTENDED
            ) {
                return@forEach
            }
            val remPref = proj.recurrence.reminderMinutesBefore
            if (remPref != null && remPref < 0) return@forEach
            val vSourceId = RecurrenceEngine.virtualOccurrenceId(proj.recurrence.id, proj.occurrenceDateIso)
            val minutes = remPref ?: settings.defaultEventReminderMinutes
            val rawTrigger = NexusNotificationScheduler.computeReminderTriggerEpoch(
                dueEpoch = proj.startAt,
                hasSpecificTime = !proj.isAllDay,
                minutesBefore = minutes
            )
            val triggerAt = NexusNotificationScheduler.adjustForQuietHours(rawTrigger, settings)
            if (triggerAt <= now) return@forEach

            desiredKeys += ReminderSourceType.EVENT to vSourceId
            val existing = reminderDao.getBySourceAndCategory(
                ReminderSourceType.EVENT,
                vSourceId,
                ReminderCategory.SCHEDULE
            )
            if (existing != null &&
                (ReminderRules.isEffectivelyCancelled(existing) ||
                    existing.status == ReminderStatus.DISMISSED ||
                    (existing.snoozedUntil != null && existing.snoozedUntil > now && existing.originalDueAt == proj.startAt))
            ) {
                return@forEach
            }
            val nextMsg = buildString {
                append(DateTimeUtils.formatDateTime(proj.startAt))
                if (proj.effectiveLocationName.isNotBlank()) append(" · ${proj.effectiveLocationName}")
            }
            val entity = ReminderEntity(
                id = existing?.id ?: UUID.randomUUID().toString(),
                sourceType = ReminderSourceType.EVENT,
                sourceId = vSourceId,
                category = ReminderCategory.SCHEDULE,
                title = "Evento: ${proj.effectiveTitle}",
                message = nextMsg,
                triggerAt = triggerAt,
                originalDueAt = proj.startAt,
                minutesBefore = minutes,
                channelId = NexusNotificationChannels.CHANNEL_EVENTS,
                status = ReminderStatus.ACTIVE,
                createdAt = existing?.createdAt ?: now,
                updatedAt = now
            )
            val shouldSchedule = settings.notificationsEnabled && settings.eventsChannelEnabled
            val exact = if (shouldSchedule) notificationScheduler?.scheduleAlarm(entity) ?: false else false
            reminderDao.upsert(entity.copy(isExactScheduled = exact))
        }

        // Remove lembretes virtuais ativos que ficaram obsoletos após edição/exclusão/materialização
        val activeVirtualReminders = reminderDao.getByStatus(ReminderStatus.ACTIVE).filter { r ->
            RecurrenceEngine.parseVirtualOccurrenceId(r.sourceId) != null
        }
        activeVirtualReminders.forEach { reminder ->
            if ((reminder.sourceType to reminder.sourceId) !in desiredKeys) {
                notificationScheduler?.cancelAlarm(reminder.id)
                notificationScheduler?.cancelDisplayedNotification(reminder.id)
                reminderDao.deleteById(reminder.id)
            }
        }
    }

    private suspend fun syncEventTicketStatusFromTransactionsInternal(eventId: String, now: Long) {
        val ev = eventDao.getById(eventId) ?: return
        if (ev.ticketStatus == TicketStatus.FREE || ev.ticketStatus == TicketStatus.CANCELLED) return
        val ticketTxs = transactionDao.getTicketObligationsByEvent(eventId)
            .filter { it.status != FinancialStatus.CANCELLED }
        val nextTicketStatus = when {
            ticketTxs.isEmpty() -> TicketStatus.NOT_BOUGHT
            ticketTxs.all { it.status == FinancialStatus.SETTLED } -> TicketStatus.PAID
            ticketTxs.any { it.status == FinancialStatus.SETTLED } -> TicketStatus.PARTIALLY_PAID
            else -> TicketStatus.PENDING
        }
        if (ev.ticketStatus != nextTicketStatus) {
            eventDao.upsert(ev.copy(ticketStatus = nextTicketStatus, updatedAt = now))
        }
    }

    private suspend fun getOrCreateRoutineSettings(now: Long): RoutineAssistantSettingsEntity {
        val existing = routineSettingsDao.getSettings()
        if (existing != null) return existing
        val created = defaultRoutineSettings(now)
        routineSettingsDao.upsert(created)
        return created
    }

    private suspend fun isSourceStillEligibleForNotification(reminder: ReminderEntity): Boolean {
        return when (reminder.sourceType) {
            ReminderSourceType.TASK -> {
                val task = taskDao.getById(reminder.sourceId) ?: return false
                !task.status.isTerminal && task.dueAt != null
            }

            ReminderSourceType.APPOINTMENT -> {
                val virtualPair = RecurrenceEngine.parseVirtualOccurrenceId(reminder.sourceId)
                if (virtualPair != null) {
                    val (recId, occIso) = virtualPair
                    val rec = taskRecurrenceDao.getById(recId) ?: return false
                    if (!rec.isActive || rec.domain != RecurrenceDomain.APPOINTMENT) return false
                    if (appointmentDao.getByRecurrenceAndDate(recId, occIso) != null) return false
                    val ex = recurrenceExceptionDao.getByRecurrenceAndDate(recId, occIso)
                    if (ex?.exceptionType == RecurrenceExceptionType.SKIP) return false
                    val occDate = RecurrenceEngine.parseLocalDateOrNull(occIso) ?: return false
                    occDate in RecurrenceEngine.generateScheduledDates(rec, occDate)
                } else {
                    appointmentDao.getById(reminder.sourceId) != null
                }
            }

            ReminderSourceType.EVENT -> {
                val virtualPair = RecurrenceEngine.parseVirtualOccurrenceId(reminder.sourceId)
                if (virtualPair != null) {
                    val (recId, occIso) = virtualPair
                    val rec = taskRecurrenceDao.getById(recId) ?: return false
                    if (!rec.isActive || rec.domain != RecurrenceDomain.EVENT) return false
                    if (rec.eventStatus == EventLifecycleStatus.CANCELLED || rec.eventStatus == EventLifecycleStatus.ATTENDED) {
                        return false
                    }
                    if (eventDao.getByRecurrenceAndDate(recId, occIso) != null) return false
                    val ex = recurrenceExceptionDao.getByRecurrenceAndDate(recId, occIso)
                    if (ex?.exceptionType == RecurrenceExceptionType.SKIP) return false
                    val occDate = RecurrenceEngine.parseLocalDateOrNull(occIso) ?: return false
                    occDate in RecurrenceEngine.generateScheduledDates(rec, occDate)
                } else {
                    val ev = eventDao.getById(reminder.sourceId) ?: return false
                    ev.eventStatus != EventLifecycleStatus.CANCELLED && ev.eventStatus != EventLifecycleStatus.ATTENDED
                }
            }

            ReminderSourceType.TRANSACTION -> {
                val tx = transactionDao.getById(reminder.sourceId) ?: return false
                tx.status != FinancialStatus.SETTLED && tx.status != FinancialStatus.CANCELLED
            }

            ReminderSourceType.STATEMENT -> {
                val stmt = statementDao.getById(reminder.sourceId) ?: return false
                stmt.status != StatementStatus.PAID
            }

            ReminderSourceType.ROUTINE_MORNING, ReminderSourceType.ROUTINE_NIGHT -> true
        }
    }

    private suspend fun buildBriefingHistoryEntry(
        type: BriefingType,
        dateIso: String,
        reviewedAt: Long?,
        now: Long
    ): BriefingHistoryEntity {
        val today = DateTimeUtils.todayLocalDate()
        val tasks = taskDao.getAllOnce()
        val appts = appointmentDao.getAllOnce()
        val recurrences = taskRecurrenceDao.getAllOnce()
        val exceptions = recurrenceExceptionDao.getAllOnce()
        val txs = transactionDao.getAllOnce().map(::effectiveTransactionStatus)

        val todayApptsCount = AgendaEngine.expand(
            window = DateRange(today, today),
            sources = AgendaSources(
                appointments = appts,
                taskRecurrences = recurrences,
                recurrenceExceptions = exceptions
            ),
            filters = AgendaFilters(kindFilter = AgendaOccurrenceKind.APPOINTMENT),
            today = today,
            nowEpoch = now
        ).size
        val todayTasksCount = tasks.count {
            !it.status.isTerminal && it.dueAt != null && DateTimeUtils.epochToLocalDate(it.dueAt) == today
        }
        val overdueTasksCount = tasks.count { isTaskOverdue(it, now, today) }
        val billsDueTodayCount = txs.count {
            AgendaEngine.isDirectAgendaBill(it) && DateTimeUtils.epochToLocalDate(it.dueAt!!) == today
        }
        val overdueBillsCount = txs.count {
            AgendaEngine.isDirectAgendaBill(it) && it.status == FinancialStatus.OVERDUE
        }

        val summary = if (type == BriefingType.MORNING) {
            "$todayApptsCount compromissos · $todayTasksCount tarefas hoje ($overdueTasksCount atrasadas) · $billsDueTodayCount contas vencendo ($overdueBillsCount em atraso)."
        } else {
            val completedTodayCount = tasks.count {
                it.status == TaskStatus.COMPLETED && it.completedAt != null &&
                    DateTimeUtils.epochToLocalDate(it.completedAt) == today
            }
            val pendingTodayCount = tasks.count {
                !it.status.isTerminal && it.dueAt != null && !DateTimeUtils.epochToLocalDate(it.dueAt).isAfter(today)
            }
            "Concluídas hoje: $completedTodayCount · Pendentes/atrasadas: $pendingTodayCount · Contas pendentes: ${billsDueTodayCount + overdueBillsCount}."
        }

        val existing = briefingHistoryDao.getByTypeAndDate(type, dateIso)
        return BriefingHistoryEntity(
            id = existing?.id ?: UUID.randomUUID().toString(),
            briefingType = type,
            dateIso = dateIso,
            summaryText = summary,
            tasksCount = todayTasksCount,
            appointmentsCount = todayApptsCount,
            billsDueCount = billsDueTodayCount,
            overdueCount = overdueTasksCount + overdueBillsCount,
            reviewedAt = reviewedAt ?: existing?.reviewedAt,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now
        )
    }

    private fun isChannelEnabled(settings: RoutineAssistantSettingsEntity, channelId: String): Boolean {
        return when (channelId) {
            NexusNotificationChannels.CHANNEL_SCHEDULE -> settings.scheduleChannelEnabled
            NexusNotificationChannels.CHANNEL_FINANCE -> settings.financeChannelEnabled
            NexusNotificationChannels.CHANNEL_EVENTS -> settings.eventsChannelEnabled
            NexusNotificationChannels.CHANNEL_ROUTINE -> settings.routineChannelEnabled
            else -> true
        }
    }

    private fun defaultRoutineSettings(now: Long): RoutineAssistantSettingsEntity =
        RoutineAssistantSettingsEntity(
            id = RoutineAssistantSettingsEntity.SINGLETON_ID,
            createdAt = now,
            updatedAt = now
        )

    suspend fun logError(source: String, throwable: Throwable) {
        withContext(Dispatchers.IO) {
            runCatching {
                val now = DateTimeUtils.nowEpochMillis()
                errorLogDao.insert(
                    ErrorLogEntity(
                        id = UUID.randomUUID().toString(),
                        source = source,
                        message = throwable.message ?: throwable::class.simpleName ?: "Erro desconhecido",
                        stackTrace = throwable.stackTraceToString().take(4000),
                        occurredAt = now,
                        createdAt = now,
                        updatedAt = now
                    )
                )
            }
        }
    }

    suspend fun clearErrorLogs(): Result<Unit> = safeDbCall("Diagnóstico") {
        errorLogDao.deleteAll()
    }

    private suspend fun ensureStatementForCardAndDate(
        cardId: String,
        referenceEpoch: Long,
        now: Long
    ): CreditCardStatementEntity? {
        val card = creditCardDao.getById(cardId) ?: return null
        val refDate = DateTimeUtils.epochToLocalDate(referenceEpoch)
        val statementMonth = if (refDate.dayOfMonth > card.closingDay) {
            YearMonth.from(refDate).plusMonths(1)
        } else {
            YearMonth.from(refDate)
        }
        val ymStr = statementMonth.toString()
        val existing = statementDao.getByCardAndMonth(cardId, ymStr)
        if (existing != null) return existing

        val closingDay = card.closingDay.coerceIn(1, statementMonth.lengthOfMonth())
        val dueDay = card.dueDay.coerceIn(1, statementMonth.lengthOfMonth())
        val closingDate = statementMonth.atDay(closingDay)
        val dueMonth = if (dueDay < closingDay) statementMonth.plusMonths(1) else statementMonth
        val dueDate = dueMonth.atDay(dueDay.coerceAtMost(dueMonth.lengthOfMonth()))

        val created = CreditCardStatementEntity(
            id = UUID.randomUUID().toString(),
            creditCardId = cardId,
            yearMonth = ymStr,
            closingAt = DateTimeUtils.localDateToEndOfDayEpoch(closingDate),
            dueAt = DateTimeUtils.localDateTimeToEpoch(dueDate, LocalTime.of(12, 0)),
            status = StatementStatus.OPEN,
            paidAt = null,
            paidAmountCents = 0L,
            createdAt = now,
            updatedAt = now
        )
        statementDao.upsert(created)
        return created
    }

    private suspend fun recordAudit(
        entityType: String,
        entityId: String,
        action: String,
        summary: String
    ) {
        val now = DateTimeUtils.nowEpochMillis()
        auditHistoryDao.insert(
            AuditHistoryEntity(
                id = UUID.randomUUID().toString(),
                entityType = entityType,
                entityId = entityId,
                action = action,
                summary = summary,
                occurredAt = now,
                createdAt = now,
                updatedAt = now
            )
        )
    }

    private suspend fun <T> safeDbCall(source: String, block: suspend () -> T): Result<T> =
        withContext(Dispatchers.IO) {
            try {
                Result.success(block())
            } catch (ex: Throwable) {
                logError(source, ex)
                Result.failure(ex)
            }
        }

    // --- Widgets Android Nativos (Fase 5) ---

    suspend fun canCompleteTaskFromWidget(taskId: String): Boolean = withContext(Dispatchers.IO) {
        val task = taskDao.getById(taskId) ?: return@withContext false
        !task.status.isTerminal && task.status != TaskStatus.BLOCKED
    }

    suspend fun completeTaskFromWidget(taskId: String): Result<Unit> = safeDbCall("Widgets") {
        val task = taskDao.getById(taskId)
            ?: throw IllegalStateException("Tarefa não encontrada.")
        if (task.status.isTerminal || task.status == TaskStatus.BLOCKED) {
            throw IllegalStateException("Tarefas bloqueadas, concluídas ou canceladas não podem ser concluídas pelo widget.")
        }
        val now = DateTimeUtils.nowEpochMillis()
        val updated = task.copy(
            status = TaskStatus.COMPLETED,
            completedAt = now,
            updatedAt = now
        )
        var materializedNext: Pair<TaskEntity, Int?>? = null
        database.withTransaction {
            taskDao.update(updated)
            materializedNext = materializeNextOccurrenceIfNeededInternal(updated, now)
            recordAudit("TaskWidget", taskId, "Tarefa concluída pelo widget", task.title)
        }
        syncTaskReminder(updated, overrideMinutesBefore = null)
        materializedNext?.let { (nextTask, recRemMins) ->
            syncTaskReminder(nextTask, overrideMinutesBefore = recRemMins)
        }
        notifyWidgetsChanged()
    }

    suspend fun loadAllWidgetSnapshots(): Result<AllWidgetSnapshots> = safeDbCall("Widgets") {
        val now = DateTimeUtils.nowEpochMillis()
        val today = DateTimeUtils.todayLocalDate()
        val settings = settingsDao.getSettings() ?: SettingsEntity(createdAt = now, updatedAt = now)
        val appointments = appointmentDao.getAllOnce()
        val tasks = taskDao.getAllOnce()
        val projects = projectDao.getAllOnce()
        val categories = categoryDao.getAllOnce()
        val events = eventDao.getAllOnce()
        val transactions = transactionDao.getAllOnce()
        val statements = statementDao.getAllOnce()
        val cards = creditCardDao.getAllOnce()
        val goals = financialGoalDao.getAllOnce()
        val recurrences = taskRecurrenceDao.getAllOnce()
        val exceptions = recurrenceExceptionDao.getAllOnce()

        AllWidgetSnapshots(
            myDay = buildMyDayWidgetSnapshot(
                appointments = appointments,
                tasks = tasks,
                today = today,
                nowEpoch = now,
                taskRecurrences = recurrences,
                recurrenceExceptions = exceptions
            ),
            myTasks = buildMyTasksWidgetSnapshot(
                tasks = tasks,
                projects = projects,
                today = today,
                nowEpoch = now
            ),
            finance = buildFinanceWidgetSnapshot(
                declaredBalanceCents = settings.declaredBalanceCents,
                transactions = transactions,
                statements = statements,
                cards = cards,
                today = today
            ),
            summary = buildNexusSummaryWidgetSnapshot(
                appointments = appointments,
                tasks = tasks,
                projects = projects,
                categories = categories,
                events = events,
                transactions = transactions,
                statements = statements,
                cards = cards,
                goals = goals,
                today = today,
                nowEpoch = now,
                taskRecurrences = recurrences,
                recurrenceExceptions = exceptions
            ),
            loadedAtEpoch = now
        )
    }

    private suspend fun notifyWidgetsChanged() {
        runCatching { onDataChanged?.invoke() }
    }

    // --- Fase 6: Personalizacao do Dashboard, Frases Locais e Objetivos Pessoais ---

    private suspend fun getOrCreatePersonalizationSettings(now: Long): PersonalizationSettingsEntity {
        val existing = personalizationSettingsDao.getSettings()
        if (existing != null) return existing
        val created = defaultPersonalizationSettings(now)
        personalizationSettingsDao.upsert(created)
        return created
    }

    private fun defaultPersonalizationSettings(now: Long): PersonalizationSettingsEntity =
        PersonalizationSettingsEntity(
            id = PersonalizationSettingsEntity.SINGLETON_ID,
            dashboardBlocksJson = serializeDashboardBlocks(defaultDashboardBlocks()),
            createdAt = now,
            updatedAt = now
        )

    suspend fun saveGreetingAndPresentation(
        greetingEnabled: Boolean,
        greetingTemplate: String,
        dateFormatPattern: NexusDateFormat,
        showDailyQuoteOnDashboard: Boolean,
        compactDashboardMode: Boolean,
        showMetricSummaryOnDashboard: Boolean
    ): Result<Unit> = safeDbCall("Personalização") {
        val now = DateTimeUtils.nowEpochMillis()
        val existing = getOrCreatePersonalizationSettings(now)
        personalizationSettingsDao.upsert(
            existing.copy(
                greetingEnabled = greetingEnabled,
                greetingTemplate = greetingTemplate.trim().ifEmpty { "AUTO" },
                dateFormatPattern = dateFormatPattern,
                showDailyQuoteOnDashboard = showDailyQuoteOnDashboard,
                compactDashboardMode = compactDashboardMode,
                showMetricSummaryOnDashboard = showMetricSummaryOnDashboard,
                updatedAt = now
            )
        )
    }

    suspend fun saveDashboardBlocks(blocks: List<DashboardBlockConfig>): Result<Unit> =
        safeDbCall("Dashboard") {
            val now = DateTimeUtils.nowEpochMillis()
            val existing = getOrCreatePersonalizationSettings(now)
            val normalized = blocks.sortedBy { it.order }
                .mapIndexed { idx, block -> block.copy(order = idx) }
            personalizationSettingsDao.upsert(
                existing.copy(
                    dashboardBlocksJson = serializeDashboardBlocks(normalized),
                    updatedAt = now
                )
            )
        }

    suspend fun resetDashboardToDefault(): Result<Unit> =
        safeDbCall("Dashboard") {
            val now = DateTimeUtils.nowEpochMillis()
            val existing = getOrCreatePersonalizationSettings(now)
            val defaults = defaultDashboardBlocks()
            personalizationSettingsDao.upsert(
                existing.copy(
                    greetingEnabled = true,
                    greetingTemplate = "AUTO",
                    dateFormatPattern = NexusDateFormat.WEEKDAY_DD_MM_YYYY,
                    showDailyQuoteOnDashboard = true,
                    compactDashboardMode = false,
                    showMetricSummaryOnDashboard = true,
                    dashboardBlocksJson = serializeDashboardBlocks(defaults),
                    updatedAt = now
                )
            )
        }

    suspend fun refreshDailyQuoteIfNeeded(today: LocalDate = DateTimeUtils.todayLocalDate()): Result<DailyQuoteSelection> =
        safeDbCall("Frase diária") {
            val now = DateTimeUtils.nowEpochMillis()
            val todayIso = today.toString()
            val personalization = getOrCreatePersonalizationSettings(now)
            val quotes = quoteDao.getAllOnce()
            val (selection, newlyPicked) = selectDailyQuote(quotes, personalization, todayIso)
            if (newlyPicked != null && personalization.lastQuoteDateIso != todayIso) {
                database.withTransaction {
                    personalizationSettingsDao.upsert(
                        personalization.copy(
                            previousQuoteId = personalization.lastQuoteId,
                            lastQuoteId = newlyPicked.id,
                            lastQuoteDateIso = todayIso,
                            updatedAt = now
                        )
                    )
                    quoteDao.upsert(
                        newlyPicked.copy(
                            lastShownDateIso = todayIso,
                            shownCount = newlyPicked.shownCount + 1,
                            updatedAt = now
                        )
                    )
                }
            }
            selection
        }

    suspend fun saveQuote(
        id: String? = null,
        text: String,
        author: String = "",
        isActive: Boolean = true
    ): Result<String> = safeDbCall("Biblioteca de Frases") {
        val cleanText = text.trim()
        require(cleanText.isNotEmpty()) { "O texto da frase é obrigatório." }
        val now = DateTimeUtils.nowEpochMillis()
        val existing = id?.let { quoteDao.getById(it) }
        val quoteId = existing?.id ?: UUID.randomUUID().toString()
        quoteDao.upsert(
            QuoteEntity(
                id = quoteId,
                text = cleanText,
                author = author.trim(),
                isActive = isActive,
                lastShownDateIso = existing?.lastShownDateIso,
                shownCount = existing?.shownCount ?: 0,
                createdAt = existing?.createdAt ?: now,
                updatedAt = now
            )
        )
        quoteId
    }

    suspend fun toggleQuoteActive(quoteId: String): Result<Unit> = safeDbCall("Biblioteca de Frases") {
        val existing = quoteDao.getById(quoteId)
            ?: throw IllegalStateException("Frase não encontrada.")
        quoteDao.upsert(
            existing.copy(
                isActive = !existing.isActive,
                updatedAt = DateTimeUtils.nowEpochMillis()
            )
        )
    }

    suspend fun deleteQuote(quoteId: String): Result<Unit> = safeDbCall("Biblioteca de Frases") {
        val now = DateTimeUtils.nowEpochMillis()
        database.withTransaction {
            quoteDao.deleteById(quoteId)
            val personalization = personalizationSettingsDao.getSettings()
            if (personalization != null && personalization.lastQuoteId == quoteId) {
                personalizationSettingsDao.upsert(
                    personalization.copy(
                        lastQuoteId = null,
                        lastQuoteDateIso = null,
                        updatedAt = now
                    )
                )
            }
        }
    }

    suspend fun importQuotesFromText(rawContent: String): Result<Int> = safeDbCall("Biblioteca de Frases") {
        val parsed = parseImportedQuotes(rawContent)
        require(parsed.isNotEmpty()) { "Nenhuma frase válida encontrada no arquivo selecionado." }
        val now = DateTimeUtils.nowEpochMillis()
        val existingSet = quoteDao.getAllOnce()
            .map { it.text.trim().lowercase() to it.author.trim().lowercase() }
            .toMutableSet()

        val toInsert = mutableListOf<QuoteEntity>()
        parsed.forEach { (text, author) ->
            val key = text.trim().lowercase() to author.trim().lowercase()
            if (existingSet.add(key)) {
                toInsert.add(
                    QuoteEntity(
                        id = UUID.randomUUID().toString(),
                        text = text.trim(),
                        author = author.trim(),
                        isActive = true,
                        createdAt = now,
                        updatedAt = now
                    )
                )
            }
        }
        if (toInsert.isNotEmpty()) {
            quoteDao.insertAll(toInsert)
        }
        toInsert.size
    }

    suspend fun exportQuotesJson(): Result<String> = safeDbCall("Biblioteca de Frases") {
        val items = quoteDao.getAllOnce().map {
            QuoteExportItem(
                text = it.text,
                author = it.author,
                isActive = it.isActive
            )
        }
        compactJson.encodeToString(items)
    }

    suspend fun savePersonalGoal(
        id: String? = null,
        title: String,
        description: String = "",
        categoryId: String? = null,
        targetDateAt: Long? = null,
        status: GoalStatus = GoalStatus.ACTIVE,
        progressMode: GoalProgressMode = GoalProgressMode.AUTO,
        manualProgressPercent: Int = 0,
        notes: String = ""
    ): Result<String> = safeDbCall("Planejamento e Objetivos") {
        val cleanTitle = title.trim()
        require(cleanTitle.isNotEmpty()) { "O título do objetivo é obrigatório." }
        val now = DateTimeUtils.nowEpochMillis()
        val existing = id?.let { personalGoalDao.getById(it) }
        val goalId = existing?.id ?: UUID.randomUUID().toString()
        val safePercent = if (status == GoalStatus.COMPLETED) 100 else manualProgressPercent.coerceIn(0, 100)
        val completedAt = when {
            status == GoalStatus.COMPLETED -> existing?.completedAt ?: now
            else -> null
        }

        val entity = PersonalGoalEntity(
            id = goalId,
            title = cleanTitle,
            description = description.trim(),
            categoryId = categoryId,
            targetDateAt = targetDateAt,
            status = status,
            progressMode = progressMode,
            manualProgressPercent = safePercent,
            completedAt = completedAt,
            notes = notes.trim(),
            createdAt = existing?.createdAt ?: now,
            updatedAt = now
        )

        database.withTransaction {
            personalGoalDao.upsert(entity)
            if (existing == null) {
                recordAudit("PersonalGoal", goalId, "Objetivo criado", cleanTitle)
            } else if (existing.manualProgressPercent != safePercent && progressMode == GoalProgressMode.MANUAL) {
                goalProgressLogDao.insert(
                    GoalProgressLogEntity(
                        id = UUID.randomUUID().toString(),
                        goalId = goalId,
                        progressPercent = safePercent,
                        note = "Progresso manual atualizado",
                        recordedAt = now,
                        createdAt = now,
                        updatedAt = now
                    )
                )
            }
        }
        goalId
    }

    suspend fun updatePersonalGoalStatus(goalId: String, status: GoalStatus): Result<Unit> =
        safeDbCall("Planejamento e Objetivos") {
            val existing = personalGoalDao.getById(goalId)
                ?: throw IllegalStateException("Objetivo não encontrado.")
            val now = DateTimeUtils.nowEpochMillis()
            val updated = existing.copy(
                status = status,
                completedAt = if (status == GoalStatus.COMPLETED) existing.completedAt ?: now else null,
                updatedAt = now
            )
            database.withTransaction {
                personalGoalDao.upsert(updated)
                recordAudit("PersonalGoal", goalId, "Status alterado para ${status.label}", existing.title)
            }
        }

    suspend fun addGoalProgressLog(goalId: String, progressPercent: Int, note: String): Result<Unit> =
        safeDbCall("Planejamento e Objetivos") {
            val existing = personalGoalDao.getById(goalId)
                ?: throw IllegalStateException("Objetivo não encontrado.")
            val now = DateTimeUtils.nowEpochMillis()
            val clamped = progressPercent.coerceIn(0, 100)
            database.withTransaction {
                if (existing.progressMode == GoalProgressMode.MANUAL) {
                    personalGoalDao.upsert(
                        existing.copy(
                            manualProgressPercent = clamped,
                            status = if (clamped >= 100) GoalStatus.COMPLETED else existing.status,
                            completedAt = if (clamped >= 100) existing.completedAt ?: now else existing.completedAt,
                            updatedAt = now
                        )
                    )
                }
                goalProgressLogDao.insert(
                    GoalProgressLogEntity(
                        id = UUID.randomUUID().toString(),
                        goalId = goalId,
                        progressPercent = clamped,
                        note = note.trim().ifEmpty { "Registro de evolução" },
                        recordedAt = now,
                        createdAt = now,
                        updatedAt = now
                    )
                )
            }
        }

    suspend fun deletePersonalGoal(goalId: String): Result<Unit> =
        safeDbCall("Planejamento e Objetivos") {
            val now = DateTimeUtils.nowEpochMillis()
            database.withTransaction {
                taskDao.unlinkGoal(goalId, now)
                projectDao.unlinkGoal(goalId, now)
                goalMilestoneDao.deleteByGoalId(goalId)
                goalProgressLogDao.deleteByGoalId(goalId)
                personalGoalDao.deleteById(goalId)
                recordAudit("PersonalGoal", goalId, "Objetivo excluído", "Tarefas e projetos vinculados foram preservados")
            }
        }

    suspend fun saveMilestone(
        id: String? = null,
        goalId: String,
        title: String,
        description: String = "",
        dueAt: Long? = null
    ): Result<String> = safeDbCall("Marcos do Objetivo") {
        val cleanTitle = title.trim()
        require(cleanTitle.isNotEmpty()) { "O título do marco é obrigatório." }
        val now = DateTimeUtils.nowEpochMillis()
        val existing = id?.let { goalMilestoneDao.getById(it) }
        val currentCount = goalMilestoneDao.getByGoalOnce(goalId).size
        val milestoneId = existing?.id ?: UUID.randomUUID().toString()
        goalMilestoneDao.upsert(
            GoalMilestoneEntity(
                id = milestoneId,
                goalId = goalId,
                title = cleanTitle,
                description = description.trim(),
                dueAt = dueAt,
                isCompleted = existing?.isCompleted ?: false,
                completedAt = existing?.completedAt,
                sortOrder = existing?.sortOrder ?: currentCount,
                createdAt = existing?.createdAt ?: now,
                updatedAt = now
            )
        )
        milestoneId
    }

    suspend fun toggleMilestoneCompleted(milestone: GoalMilestoneEntity): Result<Unit> =
        safeDbCall("Marcos do Objetivo") {
            val now = DateTimeUtils.nowEpochMillis()
            val nextDone = !milestone.isCompleted
            goalMilestoneDao.upsert(
                milestone.copy(
                    isCompleted = nextDone,
                    completedAt = if (nextDone) now else null,
                    updatedAt = now
                )
            )
        }

    suspend fun deleteMilestone(milestoneId: String): Result<Unit> = safeDbCall("Marcos do Objetivo") {
        goalMilestoneDao.deleteById(milestoneId)
    }

    suspend fun linkTaskToGoal(taskId: String, goalId: String?): Result<Unit> =
        safeDbCall("Planejamento e Objetivos") {
            taskDao.updateGoalLink(taskId, goalId, DateTimeUtils.nowEpochMillis())
        }

    suspend fun linkProjectToGoal(projectId: String, goalId: String?): Result<Unit> =
        safeDbCall("Planejamento e Objetivos") {
            projectDao.updateGoalLink(projectId, goalId, DateTimeUtils.nowEpochMillis())
        }

    @Serializable
    private data class QuoteExportItem(
        val text: String,
        val author: String = "",
        val isActive: Boolean = true
    )

    companion object {
        const val AUDIT_ENTITY_INBOX_IMPORT_LOG = "INBOX_IMPORT_LOG"

        private val compactJson = Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
            encodeDefaults = true
            prettyPrint = true
            isLenient = true
        }

        fun defaultDashboardBlocks(): List<DashboardBlockConfig> {
            val priorityOrder = listOf(
                DashboardBlockType.GREETING,
                DashboardBlockType.DAILY_QUOTE,
                DashboardBlockType.TASK_SUMMARY,
                DashboardBlockType.FINANCE_SUMMARY,
                DashboardBlockType.CHARTS_INDICATORS,
                DashboardBlockType.TODAY_AGENDA,
                DashboardBlockType.GOALS,
                DashboardBlockType.UPCOMING_EVENTS,
                DashboardBlockType.UPCOMING_PAYMENTS
            )
            return priorityOrder.mapIndexed { index, type ->
                DashboardBlockConfig(
                    type = type,
                    enabled = true,
                    order = index,
                    maxItems = when (type) {
                        DashboardBlockType.TASK_SUMMARY -> 4
                        DashboardBlockType.TODAY_AGENDA -> 4
                        DashboardBlockType.UPCOMING_EVENTS -> 3
                        DashboardBlockType.UPCOMING_PAYMENTS -> 4
                        DashboardBlockType.GOALS -> 3
                        else -> 4
                    }
                )
            }
        }

        fun computeMonthlyTaskDistribution(
            tasks: List<TaskEntity>,
            targetMonth: YearMonth,
            nowEpoch: Long = DateTimeUtils.nowEpochMillis(),
            today: LocalDate = DateTimeUtils.todayLocalDate()
        ): com.nexus.app.domain.model.TaskDistributionSummary {
            val datedMonthTasks = tasks.filter { t ->
                t.status != TaskStatus.CANCELLED &&
                    t.dueAt != null &&
                    YearMonth.from(DateTimeUtils.epochToLocalDate(t.dueAt)) == targetMonth
            }
            val completed = datedMonthTasks.count { it.status == TaskStatus.COMPLETED }
            val inProgress = datedMonthTasks.count { it.status == TaskStatus.IN_PROGRESS }
            val pending = datedMonthTasks.count {
                it.status == TaskStatus.NOT_STARTED ||
                    it.status == TaskStatus.WAITING ||
                    it.status == TaskStatus.BLOCKED
            }
            val overdue = tasks.count { isTaskOverdue(it, nowEpoch, today) }
            val undatedPending = tasks.count { !it.status.isTerminal && it.dueAt == null }
            return com.nexus.app.domain.model.TaskDistributionSummary(
                completedCount = completed,
                pendingCount = pending,
                inProgressCount = inProgress,
                overdueCount = overdue,
                undatedPendingCount = undatedPending
            )
        }

        fun parseDashboardBlocks(json: String): List<DashboardBlockConfig> {
            val defaults = defaultDashboardBlocks()
            if (json.isBlank()) return defaults
            return runCatching {
                val parsed = compactJson.decodeFromString<List<DashboardBlockConfig>>(json)
                val byType = parsed.associateBy { it.type }
                val existingOrdered = parsed
                    .distinctBy { it.type }
                    .sortedBy { it.order }
                val missing = defaults.filter { it.type !in byType }
                (existingOrdered + missing).mapIndexed { index, block ->
                    block.copy(order = index)
                }
            }.getOrDefault(defaults)
        }

        fun serializeDashboardBlocks(blocks: List<DashboardBlockConfig>): String {
            val normalized = blocks.distinctBy { it.type }
                .sortedBy { it.order }
                .mapIndexed { idx, block -> block.copy(order = idx) }
            return compactJson.encodeToString(normalized)
        }

        fun toggleDashboardBlock(
            blocks: List<DashboardBlockConfig>,
            type: DashboardBlockType,
            enabled: Boolean
        ): List<DashboardBlockConfig> = blocks.map { block ->
            if (block.type == type) block.copy(enabled = enabled) else block
        }

        fun moveDashboardBlock(
            blocks: List<DashboardBlockConfig>,
            type: DashboardBlockType,
            delta: Int
        ): List<DashboardBlockConfig> {
            val sorted = blocks.sortedBy { it.order }.toMutableList()
            val currentIndex = sorted.indexOfFirst { it.type == type }
            if (currentIndex == -1) return sorted
            val targetIndex = (currentIndex + delta).coerceIn(0, sorted.lastIndex)
            if (targetIndex == currentIndex) return sorted
            val item = sorted.removeAt(currentIndex)
            sorted.add(targetIndex, item)
            return sorted.mapIndexed { index, block -> block.copy(order = index) }
        }

        fun updateDashboardBlockConfig(
            blocks: List<DashboardBlockConfig>,
            updated: DashboardBlockConfig
        ): List<DashboardBlockConfig> = blocks.map { block ->
            if (block.type == updated.type) updated.copy(order = block.order) else block
        }

        fun selectDailyQuote(
            quotes: List<QuoteEntity>,
            personalization: PersonalizationSettingsEntity?,
            todayIso: String
        ): Pair<DailyQuoteSelection, QuoteEntity?> {
            val activeQuotes = quotes.filter { it.isActive && it.text.isNotBlank() }
                .sortedWith(compareBy<QuoteEntity> { it.createdAt }.thenBy { it.id })
            if (activeQuotes.isEmpty()) {
                return DailyQuoteSelection(
                    quote = null,
                    libraryStatus = QuoteLibraryStatus.EMPTY,
                    totalQuotesCount = quotes.size,
                    activeQuotesCount = 0
                ) to null
            }

            if (activeQuotes.size == 1) {
                val single = activeQuotes.first()
                val needsPersist = personalization?.lastQuoteDateIso != todayIso || personalization.lastQuoteId != single.id
                return DailyQuoteSelection(
                    quote = single,
                    libraryStatus = QuoteLibraryStatus.SINGLE_QUOTE,
                    totalQuotesCount = quotes.size,
                    activeQuotesCount = 1
                ) to (if (needsPersist) single else null)
            }

            val sameDayPersisted = personalization?.takeIf { it.lastQuoteDateIso == todayIso }
                ?.lastQuoteId
                ?.let { savedId -> activeQuotes.firstOrNull { it.id == savedId } }
            if (sameDayPersisted != null) {
                return DailyQuoteSelection(
                    quote = sameDayPersisted,
                    libraryStatus = QuoteLibraryStatus.ROTATING,
                    totalQuotesCount = quotes.size,
                    activeQuotesCount = activeQuotes.size
                ) to null
            }

            val lastQuoteId = personalization?.lastQuoteId
            val candidates = activeQuotes.filter { it.id != lastQuoteId }.ifEmpty { activeQuotes }
            val minShownCount = candidates.minOf { it.shownCount }
            val leastShownPool = candidates.filter { it.shownCount == minShownCount }
            val index = abs(todayIso.hashCode()) % leastShownPool.size
            val picked = leastShownPool[index]

            return DailyQuoteSelection(
                quote = picked,
                libraryStatus = QuoteLibraryStatus.ROTATING,
                totalQuotesCount = quotes.size,
                activeQuotesCount = activeQuotes.size
            ) to picked
        }

        fun isGoalOverdue(
            goal: PersonalGoalEntity,
            startOfTodayEpoch: Long = DateTimeUtils.localDateToStartOfDayEpoch(DateTimeUtils.todayLocalDate())
        ): Boolean {
            if (goal.status != GoalStatus.ACTIVE) return false
            val target = goal.targetDateAt ?: return false
            return target < startOfTodayEpoch
        }

        fun computeGoalProgressPercent(
            goal: PersonalGoalEntity,
            milestones: List<GoalMilestoneEntity>,
            linkedTasks: List<TaskWithDetails>,
            linkedProjects: List<ProjectWithProgress>
        ): Int {
            if (goal.status == GoalStatus.COMPLETED) return 100
            if (goal.progressMode == GoalProgressMode.MANUAL) {
                return goal.manualProgressPercent.coerceIn(0, 100)
            }
            val totalItems = milestones.size + linkedTasks.size + linkedProjects.size
            if (totalItems == 0) return 0
            val completedItems = milestones.count { it.isCompleted } +
                linkedTasks.count { it.task.status == TaskStatus.COMPLETED } +
                linkedProjects.count { it.project.status == ProjectStatus.COMPLETED }
            return ((completedItems * 100.0) / totalItems.toDouble()).toInt().coerceIn(0, 100)
        }

        /**
         * Importa frases locais em formato JSON ou TXT.
         * NUNCA inventa autor quando nao informado no arquivo: retorna string vazia.
         */
        fun parseImportedQuotes(rawContent: String): List<Pair<String, String>> {
            val trimmed = rawContent.trim()
            if (trimmed.isEmpty()) return emptyList()

            if (trimmed.startsWith("[") || trimmed.startsWith("{")) {
                val jsonParsed = runCatching {
                    val elem = compactJson.parseToJsonElement(trimmed)
                    val array: JsonArray = when (elem) {
                        is JsonArray -> elem
                        is JsonObject -> elem["quotes"] as? JsonArray ?: JsonArray(emptyList())
                        else -> JsonArray(emptyList())
                    }
                    array.mapNotNull { item ->
                        when (item) {
                            is JsonPrimitive -> item.contentOrNull?.trim()?.takeIf { it.isNotEmpty() }?.let { it to "" }
                            is JsonObject -> {
                                val text = (item["text"] ?: item["quote"] ?: item["frase"])
                                    ?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
                                val author = (item["author"] ?: item["autor"])
                                    ?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
                                if (text.isNotEmpty()) text to author else null
                            }
                            else -> null
                        }
                    }
                }.getOrNull()
                if (!jsonParsed.isNullOrEmpty()) return jsonParsed
            }

            return trimmed.lines()
                .mapNotNull { line ->
                    val clean = line.trim()
                    if (clean.isEmpty() || clean.startsWith("#")) return@mapNotNull null
                    val separators = listOf(" | ", " — ", " - ")
                    val matchedSep = separators.firstOrNull { clean.contains(it) }
                    if (matchedSep != null) {
                        val parts = clean.split(matchedSep, limit = 2)
                        val quoteText = parts[0].trim().trim('"', '“', '”')
                        val authorText = parts.getOrNull(1)?.trim().orEmpty()
                        if (quoteText.isNotEmpty()) quoteText to authorText else null
                    } else {
                        val quoteText = clean.trim('"', '“', '”')
                        if (quoteText.isNotEmpty()) quoteText to "" else null
                    }
                }
        }

        fun currentYearMonth(): String = YearMonth.now(DateTimeUtils.ZONE_SAO_PAULO).toString()

        fun yearMonthOfEpoch(epochMillis: Long): String =
            YearMonth.from(DateTimeUtils.epochToLocalDate(epochMillis)).toString()

        fun effectiveTransactionStatus(tx: TransactionEntity): TransactionEntity =
            FinanceRules.effectiveTransactionStatus(tx)

        fun effectiveStatementStatus(stmt: CreditCardStatementEntity): CreditCardStatementEntity {
            if (stmt.status == StatementStatus.PAID) return stmt
            val startOfToday = DateTimeUtils.localDateToStartOfDayEpoch(DateTimeUtils.todayLocalDate())
            return if (stmt.dueAt < startOfToday) {
                stmt.copy(status = StatementStatus.OVERDUE)
            } else {
                stmt
            }
        }

        fun advanceDateByFrequency(
            date: LocalDate,
            frequency: RecurrenceFrequency,
            customDays: Int
        ): LocalDate = when (frequency) {
            RecurrenceFrequency.DAILY -> date.plusDays(1)
            RecurrenceFrequency.WEEKLY -> date.plusWeeks(1)
            RecurrenceFrequency.BIWEEKLY -> date.plusWeeks(2)
            RecurrenceFrequency.MONTHLY -> date.plusMonths(1)
            RecurrenceFrequency.BIMONTHLY -> date.plusMonths(2)
            RecurrenceFrequency.QUARTERLY -> date.plusMonths(3)
            RecurrenceFrequency.SEMIANNUAL -> date.plusMonths(6)
            RecurrenceFrequency.ANNUAL -> date.plusYears(1)
            RecurrenceFrequency.CUSTOM_DAYS -> date.plusDays(customDays.coerceAtLeast(1).toLong())
        }

        fun isTaskOverdue(
            task: TaskEntity,
            nowEpoch: Long = DateTimeUtils.nowEpochMillis(),
            today: LocalDate = DateTimeUtils.todayLocalDate()
        ): Boolean {
            return AgendaEngine.isTaskOverdue(task, nowEpoch, today)
        }

        fun computeAppointmentConflicts(appointments: List<AppointmentEntity>): Map<String, List<String>> =
            AgendaEngine.computeAppointmentConflicts(appointments)

        fun buildMyDayWidgetSnapshot(
            appointments: List<AppointmentEntity>,
            tasks: List<TaskEntity>,
            today: LocalDate = DateTimeUtils.todayLocalDate(),
            nowEpoch: Long = DateTimeUtils.nowEpochMillis(),
            taskRecurrences: List<TaskRecurrenceEntity> = emptyList(),
            recurrenceExceptions: List<RecurrenceExceptionEntity> = emptyList()
        ): MyDayWidgetSnapshot {
            val todayOccurrences = AgendaEngine.expand(
                window = DateRange(today, today),
                sources = AgendaSources(
                    appointments = appointments,
                    tasks = tasks,
                    taskRecurrences = taskRecurrences,
                    recurrenceExceptions = recurrenceExceptions
                ),
                filters = AgendaFilters(),
                today = today,
                nowEpoch = nowEpoch
            )
            val appointmentMap = appointments.associateBy { it.id }
            val taskMap = tasks.associateBy { it.id }
            val recMap = taskRecurrences.associateBy { it.id }
            val exMap = recurrenceExceptions.associateBy { it.recurrenceId to it.occurrenceDateIso }

            val todayAppointments = todayOccurrences
                .filter { it.sourceType == AgendaSourceType.APPOINTMENT }
                .mapNotNull { occ ->
                    if (occ.isVirtualRecurrence) {
                        val rec = occ.recurrenceId?.let { recMap[it] } ?: return@mapNotNull null
                        val anchorIso = occ.seriesOccurrenceDateIso ?: occ.occurrenceDateIso
                        val ex = exMap[rec.id to anchorIso]
                        val isOverride = ex?.exceptionType == RecurrenceExceptionType.OVERRIDE
                        val loc = if (isOverride && ex?.overrideLocation != null) ex.overrideLocation else rec.location
                        val endEpoch = occ.endAt ?: (occ.startAt + rec.durationMillis.coerceAtLeast(60_000L))
                        val timeLabel = if (occ.isAllDay) {
                            "Dia inteiro"
                        } else {
                            "${DateTimeUtils.formatTime(occ.startAt)} - ${DateTimeUtils.formatTime(endEpoch)}"
                        }
                        MyDayAppointmentRow(
                            id = RecurrenceEngine.virtualOccurrenceId(rec.id, anchorIso),
                            title = occ.title,
                            timeLabel = timeLabel,
                            location = loc,
                            hasConflict = occ.hasConflict
                        )
                    } else {
                        val appt = appointmentMap[occ.sourceId] ?: return@mapNotNull null
                        val timeLabel = if (appt.isAllDay) {
                            "Dia inteiro"
                        } else {
                            "${DateTimeUtils.formatTime(appt.startAt)} - ${DateTimeUtils.formatTime(appt.endAt)}"
                        }
                        MyDayAppointmentRow(
                            id = appt.id,
                            title = appt.title,
                            timeLabel = timeLabel,
                            location = appt.location,
                            hasConflict = occ.hasConflict
                        )
                    }
                }

            val tasksDueToday = todayOccurrences
                .filter { it.sourceType == AgendaSourceType.TASK }
                .mapNotNull { occ ->
                    if (occ.isVirtualRecurrence) {
                        val rec = occ.recurrenceId?.let { recMap[it] } ?: return@mapNotNull null
                        MyDayTaskRow(
                            id = RecurrenceEngine.virtualOccurrenceId(rec.id, occ.occurrenceDateIso),
                            title = occ.title,
                            priority = rec.priority,
                            status = TaskStatus.NOT_STARTED,
                            timeLabel = if (rec.hasDueTime) DateTimeUtils.formatTime(occ.startAt) else "Hoje",
                            isOverdue = occ.isOverdue
                        )
                    } else {
                        val task = taskMap[occ.sourceId] ?: return@mapNotNull null
                        if (task.status.isTerminal) return@mapNotNull null
                        MyDayTaskRow(
                            id = task.id,
                            title = task.title,
                            priority = task.priority,
                            status = task.status,
                            timeLabel = if (task.hasDueTime && task.dueAt != null) DateTimeUtils.formatTime(task.dueAt) else "Hoje",
                            isOverdue = isTaskOverdue(task, nowEpoch, today)
                        )
                    }
                }
                .sortedWith(compareByDescending<MyDayTaskRow> { it.priority.sortRank })

            val completedToday = tasks.count {
                it.status == TaskStatus.COMPLETED &&
                    it.completedAt != null &&
                    DateTimeUtils.epochToLocalDate(it.completedAt) == today
            }
            val totalCompleted = tasks.count { it.status == TaskStatus.COMPLETED }
            val pendingCount = tasks.count { !it.status.isTerminal }

            return MyDayWidgetSnapshot(
                date = today,
                dateFormatted = DateTimeUtils.formatFullDateWithWeekday(today),
                appointments = todayAppointments,
                tasksDueToday = tasksDueToday,
                completedTasksTodayCount = completedToday,
                totalCompletedTasksCount = totalCompleted,
                pendingTasksCount = pendingCount
            )
        }

        fun buildMyTasksWidgetSnapshot(
            tasks: List<TaskEntity>,
            projects: List<ProjectEntity>,
            today: LocalDate = DateTimeUtils.todayLocalDate(),
            nowEpoch: Long = DateTimeUtils.nowEpochMillis()
        ): MyTasksWidgetSnapshot {
            val projectMap = projects.associateBy { it.id }
            val pending = tasks.filter { !it.status.isTerminal }
            val sorted = pending.sortedWith(
                compareByDescending<TaskEntity> { it.priority.sortRank }
                    .thenBy { it.dueAt ?: Long.MAX_VALUE }
                    .thenBy { it.createdAt }
            )
            val rows = sorted.map { task ->
                val overdue = isTaskOverdue(task, nowEpoch, today)
                val dueLabel = task.dueAt?.let { due ->
                    if (task.hasDueTime) {
                        "${DateTimeUtils.formatDayMonth(DateTimeUtils.epochToLocalDate(due))} ${DateTimeUtils.formatTime(due)}"
                    } else {
                        DateTimeUtils.formatDate(due)
                    }
                }
                WidgetTaskRow(
                    id = task.id,
                    title = task.title,
                    projectName = task.projectId?.let { projectMap[it]?.name },
                    dueLabel = dueLabel,
                    priority = task.priority,
                    status = task.status,
                    isOverdue = overdue,
                    canCompleteFromWidget = !task.status.isTerminal && task.status != TaskStatus.BLOCKED
                )
            }
            val urgentOrHigh = pending.count {
                it.priority == TaskPriority.URGENT || it.priority == TaskPriority.HIGH
            }
            return MyTasksWidgetSnapshot(
                tasks = rows,
                pendingTasksCount = pending.size,
                urgentOrHighCount = urgentOrHigh
            )
        }

        fun buildFinanceWidgetSnapshot(
            declaredBalanceCents: Long,
            transactions: List<TransactionEntity>,
            statements: List<CreditCardStatementEntity>,
            cards: List<CreditCardEntity>,
            today: LocalDate = DateTimeUtils.todayLocalDate()
        ): FinanceWidgetSnapshot {
            val startOfToday = DateTimeUtils.startOfDayEpoch(today)
            val effectiveTx = transactions.map { FinanceRules.effectiveTransactionStatus(it, today) }
            val activeTx = effectiveTx.filter { it.status != FinancialStatus.CANCELLED }
            val monthlyTotals = FinanceRules.computeMonthlyFinanceSummary(
                transactions = transactions,
                targetMonth = YearMonth.from(today),
                today = today
            )

            val cardMap = cards.associateBy { it.id }
            val txByStatement = transactions.groupBy { it.statementId }
            val openStatements = statements.mapNotNull { rawStmt ->
                val effStmt = if (rawStmt.status != StatementStatus.PAID && rawStmt.dueAt < startOfToday) {
                    rawStmt.copy(status = StatementStatus.OVERDUE)
                } else {
                    rawStmt
                }
                if (effStmt.status == StatementStatus.PAID) return@mapNotNull null
                val card = cardMap[effStmt.creditCardId] ?: return@mapNotNull null
                val total = txByStatement[effStmt.id].orEmpty()
                    .filter { it.type == TransactionType.EXPENSE && it.status != FinancialStatus.CANCELLED }
                    .sumOf { it.amountCents }
                if (total <= 0L) return@mapNotNull null
                WidgetOpenStatementRow(
                    statementId = effStmt.id,
                    cardName = card.name,
                    yearMonth = effStmt.yearMonth,
                    totalCents = total,
                    dueAt = effStmt.dueAt,
                    isOverdue = effStmt.status == StatementStatus.OVERDUE
                )
            }.sortedBy { it.dueAt }

            val upcomingDueRows = activeTx
                .filter { it.status != FinancialStatus.SETTLED && it.dueAt != null }
                .sortedWith(
                    compareByDescending<TransactionEntity> { it.status == FinancialStatus.OVERDUE }
                        .thenBy { it.dueAt ?: Long.MAX_VALUE }
                )
                .map { tx ->
                    WidgetDueTransactionRow(
                        id = tx.id,
                        description = tx.description,
                        type = tx.type,
                        amountCents = tx.amountCents,
                        dueAt = tx.dueAt ?: tx.occurredAt,
                        status = tx.status,
                        isOverdue = tx.status == FinancialStatus.OVERDUE
                    )
                }

            return FinanceWidgetSnapshot(
                declaredBalanceCents = declaredBalanceCents,
                plannedIncomeCents = monthlyTotals.plannedIncomeCents,
                receivedIncomeCents = monthlyTotals.receivedIncomeCents,
                plannedExpenseCents = monthlyTotals.pendingExpenseCents,
                paidExpenseCents = monthlyTotals.paidExpenseCents,
                overdueExpenseCents = monthlyTotals.overdueExpenseCents,
                overdueCount = monthlyTotals.overdueCount,
                openStatementsCount = openStatements.size,
                openStatementsTotalCents = openStatements.sumOf { it.totalCents },
                overdueStatementsCount = openStatements.count { it.isOverdue },
                upcomingDueTransactions = upcomingDueRows,
                openStatements = openStatements
            )
        }

        fun buildNexusSummaryWidgetSnapshot(
            appointments: List<AppointmentEntity>,
            tasks: List<TaskEntity>,
            projects: List<ProjectEntity>,
            categories: List<CategoryEntity>,
            events: List<EventEntity>,
            transactions: List<TransactionEntity>,
            statements: List<CreditCardStatementEntity>,
            cards: List<CreditCardEntity>,
            goals: List<FinancialGoalEntity>,
            today: LocalDate = DateTimeUtils.todayLocalDate(),
            nowEpoch: Long = DateTimeUtils.nowEpochMillis(),
            taskRecurrences: List<TaskRecurrenceEntity> = emptyList(),
            recurrenceExceptions: List<RecurrenceExceptionEntity> = emptyList()
        ): NexusSummaryWidgetSnapshot {
            val startOfToday = DateTimeUtils.localDateToStartOfDayEpoch(today)
            val projectMap = projects.associateBy { it.id }
            val catMap = categories.associateBy { it.id }
            val cardMap = cards.associateBy { it.id }
            val recMap = taskRecurrences.associateBy { it.id }
            val exMap = recurrenceExceptions.associateBy { it.recurrenceId to it.occurrenceDateIso }

            val pendingTasks = tasks.filter { !it.status.isTerminal }
            val overdueTasksCount = pendingTasks.count { isTaskOverdue(it, nowEpoch, today) }
            val topTaskEntity = pendingTasks.sortedWith(
                compareByDescending<TaskEntity> { it.priority.sortRank }
                    .thenBy { it.dueAt ?: Long.MAX_VALUE }
            ).firstOrNull()
            val topTask = topTaskEntity?.let { t ->
                TaskWithDetails(
                    task = t,
                    project = t.projectId?.let { projectMap[it] },
                    category = t.categoryId?.let { catMap[it] },
                    event = null,
                    subtasks = emptyList()
                )
            }

            val todayOccurrences = AgendaEngine.expand(
                window = DateRange(today, today),
                sources = AgendaSources(
                    appointments = appointments,
                    tasks = tasks,
                    projects = projects,
                    categories = categories,
                    events = events,
                    taskRecurrences = taskRecurrences,
                    recurrenceExceptions = recurrenceExceptions
                ),
                filters = AgendaFilters(),
                today = today,
                nowEpoch = nowEpoch
            )
            val appointmentMap = appointments.associateBy { it.id }
            val todayAppointmentOccurrences = todayOccurrences.filter { it.sourceType == AgendaSourceType.APPOINTMENT }
            val nextApptOccurrence = todayAppointmentOccurrences.firstOrNull { occ ->
                val endEpoch = occ.endAt ?: occ.startAt
                !occ.isAllDay && endEpoch >= nowEpoch
            } ?: todayAppointmentOccurrences.firstOrNull()
            val nextAppointment = nextApptOccurrence?.let { occ ->
                if (occ.isVirtualRecurrence) {
                    val rec = occ.recurrenceId?.let { recMap[it] } ?: return@let null
                    val anchorIso = occ.seriesOccurrenceDateIso ?: occ.occurrenceDateIso
                    val ex = exMap[rec.id to anchorIso]
                    val isOverride = ex?.exceptionType == RecurrenceExceptionType.OVERRIDE
                    val virtualAppt = AppointmentEntity(
                        id = RecurrenceEngine.virtualOccurrenceId(rec.id, anchorIso),
                        title = occ.title,
                        description = if (isOverride && ex?.overrideDescription != null) ex.overrideDescription else rec.description,
                        categoryId = rec.categoryId,
                        eventId = rec.eventId,
                        startAt = occ.startAt,
                        endAt = occ.endAt ?: (occ.startAt + rec.durationMillis.coerceAtLeast(60_000L)),
                        isAllDay = occ.isAllDay,
                        location = if (isOverride && ex?.overrideLocation != null) ex.overrideLocation else rec.location,
                        link = if (isOverride && ex?.overrideLink != null) ex.overrideLink else rec.link,
                        recurrenceId = rec.id,
                        occurrenceDateIso = anchorIso,
                        createdAt = rec.createdAt,
                        updatedAt = rec.updatedAt
                    )
                    AgendaItem.AppointmentItem(
                        appointment = virtualAppt,
                        category = rec.categoryId?.let { catMap[it] },
                        conflictTitles = occ.conflictTitles,
                        date = today,
                        recurrenceId = rec.id,
                        occurrenceDateIso = anchorIso,
                        isVirtualRecurrence = true
                    )
                } else {
                    val appt = appointmentMap[occ.sourceId] ?: return@let null
                    AgendaItem.AppointmentItem(
                        appointment = appt,
                        category = appt.categoryId?.let { catMap[it] },
                        conflictTitles = occ.conflictTitles,
                        date = today,
                        recurrenceId = appt.recurrenceId,
                        occurrenceDateIso = appt.occurrenceDateIso,
                        isVirtualRecurrence = false
                    )
                }
            }

            val materializedUpcomingEvents = events
                .filter { it.eventStatus != EventLifecycleStatus.CANCELLED && it.endAt >= startOfToday }
                .map { ev ->
                    EventWithDetails(
                        event = ev,
                        category = ev.categoryId?.let { catMap[it] },
                        creditCard = ev.creditCardId?.let { cardMap[it] },
                        linkedTasks = emptyList(),
                        linkedTransactions = emptyList()
                    )
                }
            val virtualUpcomingEvents = RecurrenceEngine.projectVirtualEvents(
                window = DateRange(today, today.plusDays(90)),
                recurrences = taskRecurrences,
                exceptions = recurrenceExceptions,
                materializedEvents = events
            )
                .filter { it.recurrence.eventStatus != EventLifecycleStatus.CANCELLED && it.endAt >= startOfToday }
                .map { proj ->
                    EventWithDetails(
                        event = proj.toVirtualEventEntity(),
                        category = proj.recurrence.categoryId?.let { catMap[it] },
                        creditCard = null,
                        linkedTasks = emptyList(),
                        linkedTransactions = emptyList()
                    )
                }
            val upcomingEvents = (materializedUpcomingEvents + virtualUpcomingEvents)
                .sortedBy { it.event.startAt }
            val nextEvent = upcomingEvents.firstOrNull()

            val financeSnap = buildFinanceWidgetSnapshot(
                declaredBalanceCents = 0L,
                transactions = transactions,
                statements = statements,
                cards = cards,
                today = today
            )
            val txByStatement = transactions.groupBy { it.statementId }
            val openStatementDetails = statements.mapNotNull { rawStmt ->
                val effStmt = if (rawStmt.status != StatementStatus.PAID && rawStmt.dueAt < startOfToday) {
                    rawStmt.copy(status = StatementStatus.OVERDUE)
                } else {
                    rawStmt
                }
                if (effStmt.status == StatementStatus.PAID) return@mapNotNull null
                val card = cardMap[effStmt.creditCardId] ?: return@mapNotNull null
                val linked = txByStatement[effStmt.id].orEmpty()
                if (linked.none { it.type == TransactionType.EXPENSE && it.status != FinancialStatus.CANCELLED }) {
                    return@mapNotNull null
                }
                StatementWithDetails(statement = effStmt, card = card, transactions = emptyList())
            }

            val upcomingDueCount = financeSnap.upcomingDueTransactions.count { !it.isOverdue }

            return NexusSummaryWidgetSnapshot(
                dateFormatted = DateTimeUtils.formatFullDateWithWeekday(today),
                pendingTasksCount = pendingTasks.size,
                overdueTasksCount = overdueTasksCount,
                topTask = topTask,
                todayAppointmentsCount = todayAppointmentOccurrences.size,
                nextAppointment = nextAppointment,
                upcomingEventsCount = upcomingEvents.size,
                nextEvent = nextEvent,
                upcomingDueCount = upcomingDueCount,
                overdueBillsCount = financeSnap.overdueCount,
                nextDueTransaction = financeSnap.upcomingDueTransactions.firstOrNull(),
                openStatementsCount = financeSnap.openStatementsCount,
                openStatements = openStatementDetails,
                goalsCount = goals.size,
                goalsTotalTargetCents = goals.sumOf { it.targetAmountCents },
                goalsTotalCurrentCents = goals.sumOf { it.currentAmountCents },
                topGoalName = goals.firstOrNull()?.name
            )
        }
    }
}
