package com.nexus.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nexus.app.core.DateTimeUtils
import com.nexus.app.data.backup.BackupPreview
import com.nexus.app.data.backup.BackupValidationResult
import com.nexus.app.data.backup.RestoreSummary
import com.nexus.app.data.db.entity.AuditHistoryEntity
import com.nexus.app.data.db.entity.BriefingHistoryEntity
import com.nexus.app.data.db.entity.CategoryEntity
import com.nexus.app.data.db.entity.CreditCardEntity
import com.nexus.app.data.db.entity.ErrorLogEntity
import com.nexus.app.data.db.entity.FinancialGoalEntity
import com.nexus.app.data.db.entity.GoalMilestoneEntity
import com.nexus.app.data.db.entity.LoanEntity
import com.nexus.app.data.db.entity.PersonalGoalEntity
import com.nexus.app.data.db.entity.PersonalizationSettingsEntity
import com.nexus.app.data.db.entity.QuoteEntity
import com.nexus.app.data.db.entity.RecurringRuleEntity
import com.nexus.app.data.db.entity.ReminderEntity
import com.nexus.app.data.db.entity.RoutineAssistantSettingsEntity
import com.nexus.app.data.db.entity.SettingsEntity
import com.nexus.app.data.db.entity.SubtaskEntity
import com.nexus.app.data.market.MarketAlertRepository
import com.nexus.app.data.market.MarketHistoryRepository
import com.nexus.app.data.market.MarketQuoteRepository
import com.nexus.app.data.market.MarketWatchlistRepository
import com.nexus.app.data.repository.NexusRepository
import com.nexus.app.domain.model.AgendaItem
import com.nexus.app.domain.model.BriefingType
import com.nexus.app.domain.model.BudgetProgress
import com.nexus.app.domain.model.CardWithStatements
import com.nexus.app.domain.model.CategoryScope
import com.nexus.app.domain.model.DailyBriefingSnapshot
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
import com.nexus.app.domain.model.GoalStatusFilter
import com.nexus.app.domain.model.InstallmentPurchaseWithItems
import com.nexus.app.domain.model.MarketAlertCondition
import com.nexus.app.domain.model.MarketHistoryRange
import com.nexus.app.domain.model.MarketHistoryUiState
import com.nexus.app.domain.model.MarketPriceAlert
import com.nexus.app.domain.model.MarketQuoteUiState
import com.nexus.app.domain.model.NexusDateFormat
import com.nexus.app.domain.model.NightReviewSnapshot
import com.nexus.app.domain.model.PaymentMethod
import com.nexus.app.domain.model.PersonalGoalWithDetails
import com.nexus.app.domain.model.ProjectStatus
import com.nexus.app.domain.model.ProjectWithProgress
import com.nexus.app.domain.model.RecurrenceFrequency
import com.nexus.app.domain.model.RecurringEditScope
import com.nexus.app.domain.model.TaskPriority
import com.nexus.app.domain.model.TaskRecurrenceDraft
import com.nexus.app.domain.model.TaskStatus
import com.nexus.app.domain.model.TaskWithDetails
import com.nexus.app.domain.model.TicketStatus
import com.nexus.app.domain.model.TransactionType
import com.nexus.app.domain.model.TransactionWithDetails
import com.nexus.app.domain.rules.AgendaEngine
import com.nexus.app.domain.rules.AgendaFilters
import com.nexus.app.domain.rules.AgendaOccurrence
import com.nexus.app.domain.rules.AgendaOccurrenceKind
import com.nexus.app.domain.rules.FinanceRules
import com.nexus.app.domain.rules.MonthlyFinanceDashboardData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

enum class AgendaViewMode(val label: String) {
    DAY("Dia"),
    WEEK("Semana"),
    MONTH("Mês"),
    LIST("Lista")
}

enum class TasksTabMode(val label: String) {
    TODAY("Hoje"),
    UPCOMING("Próximas"),
    OVERDUE("Atrasadas"),
    ALL("Todas"),
    COMPLETED("Concluídas"),
    KANBAN("Kanban"),
    PROJECTS("Projetos")
}

enum class FinanceTabMode(val label: String) {
    OVERVIEW("Resumo"),
    TRANSACTIONS("Lançamentos"),
    CARDS("Cartões"),
    RECURRING("Parcelas/Recorrentes"),
    PLANNING("Orçamentos/Metas")
}

data class TasksFilterState(
    val tabMode: TasksTabMode = TasksTabMode.TODAY,
    val statusFilter: TaskStatus? = null,
    val priorityFilter: TaskPriority? = null,
    val projectFilterId: String? = null,
    val searchQuery: String = ""
)

data class AgendaFilterState(
    val viewMode: AgendaViewMode = AgendaViewMode.MONTH,
    val selectedDate: LocalDate = DateTimeUtils.todayLocalDate(),
    val kindFilter: AgendaOccurrenceKind? = null,
    val categoryFilterId: String? = null,
    val searchQuery: String = "",
    val showPaid: Boolean = false
)

data class FinanceFilterState(
    val tabMode: FinanceTabMode = FinanceTabMode.OVERVIEW,
    val selectedMonth: YearMonth = YearMonth.from(DateTimeUtils.todayLocalDate()),
    val typeFilter: TransactionType? = null,
    val statusFilter: FinancialStatus? = null,
    val selectedCategoryFilterId: String? = null,
    val paymentMethodFilter: PaymentMethod? = null,
    val searchQuery: String = ""
)

data class GoalsFilterState(
    val statusFilter: GoalStatusFilter = GoalStatusFilter.ACTIVE,
    val categoryFilterId: String? = null,
    val searchQuery: String = ""
)

data class BackupUiState(
    val isBusy: Boolean = false,
    val pendingPreview: BackupPreview? = null,
    val validationError: String? = null,
    val lastRestoreSummary: RestoreSummary? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class NexusViewModel(
    private val repository: NexusRepository,
    private val marketQuoteRepository: MarketQuoteRepository? = null,
    private val marketHistoryRepository: MarketHistoryRepository? = null,
    private val marketWatchlistRepository: MarketWatchlistRepository? = null,
    private val marketAlertRepository: MarketAlertRepository? = null
) : ViewModel() {

    private val _snackbarEvents = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val snackbarEvents: SharedFlow<String> = _snackbarEvents.asSharedFlow()

    val usdBrlQuoteState: StateFlow<MarketQuoteUiState> =
        marketQuoteRepository?.usdBrlState ?: MutableStateFlow(MarketQuoteUiState())

    val marketQuoteStates: StateFlow<Map<String, MarketQuoteUiState>> =
        marketQuoteRepository?.quoteStates ?: MutableStateFlow(emptyMap())

    val marketHistoryStates: StateFlow<Map<String, Map<MarketHistoryRange, MarketHistoryUiState>>> =
        marketHistoryRepository?.historyStates ?: MutableStateFlow(emptyMap())

    val marketFavoriteCodes: StateFlow<List<String>> =
        marketWatchlistRepository?.favoriteCodes ?: MutableStateFlow(emptyList())

    val marketAlerts: StateFlow<List<MarketPriceAlert>> =
        marketAlertRepository?.alerts ?: MutableStateFlow(emptyList())

    init {
        // Garante geracao idempotente de recorrencias e frase do dia ao iniciar sem nunca duplicar
        viewModelScope.launch {
            repository.generateAllUpcomingRecurringOccurrences()
            repository.refreshDailyQuoteIfNeeded()
        }
    }

    val settings: StateFlow<SettingsEntity> = repository.observeSettings()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            SettingsEntity(
                userName = "Enzo",
                darkTheme = true,
                declaredBalanceCents = 0L,
                createdAt = 0L,
                updatedAt = 0L
            )
        )

    val categories: StateFlow<List<CategoryEntity>> = repository.observeCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val dashboard: StateFlow<DashboardSnapshot> = repository.observeDashboard()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardSnapshot())

    val allTasks: StateFlow<List<TaskWithDetails>> = repository.observeTasksWithDetails()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val allProjects: StateFlow<List<ProjectWithProgress>> = repository.observeProjectsWithProgress()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val allEvents: StateFlow<List<EventWithDetails>> = repository.observeEventsWithDetails()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val allTransactions: StateFlow<List<TransactionWithDetails>> = repository.observeTransactionsWithDetails()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val financeSummary: StateFlow<FinanceSummary> = repository.observeFinanceSummary()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FinanceSummary())

    val cardsWithStatements: StateFlow<List<CardWithStatements>> = repository.observeCardsWithStatements()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val installmentPurchases: StateFlow<List<InstallmentPurchaseWithItems>> =
        repository.observeInstallmentPurchases()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val recurringRules: StateFlow<List<RecurringRuleEntity>> = repository.observeRecurringRules()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val loans: StateFlow<List<LoanEntity>> = repository.observeLoans()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val budgets: StateFlow<List<BudgetProgress>> = repository.observeBudgetProgress()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val financialGoals: StateFlow<List<FinancialGoalEntity>> = repository.observeFinancialGoals()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val auditHistory: StateFlow<List<AuditHistoryEntity>> = repository.observeAuditHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val routineSettings: StateFlow<RoutineAssistantSettingsEntity> = repository.observeRoutineSettings()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            RoutineAssistantSettingsEntity(createdAt = 0L, updatedAt = 0L)
        )

    val reminders: StateFlow<List<ReminderEntity>> = repository.observeReminders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val briefingHistory: StateFlow<List<BriefingHistoryEntity>> = repository.observeBriefingHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val dailyBriefing: StateFlow<DailyBriefingSnapshot> = repository.observeDailyBriefingSnapshot()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DailyBriefingSnapshot())

    val nightReview: StateFlow<NightReviewSnapshot> = repository.observeNightReviewSnapshot()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NightReviewSnapshot())

    val personalizationSettings: StateFlow<PersonalizationSettingsEntity> =
        repository.observePersonalizationSettings()
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                PersonalizationSettingsEntity(
                    dashboardBlocksJson = NexusRepository.serializeDashboardBlocks(
                        NexusRepository.defaultDashboardBlocks()
                    ),
                    createdAt = 0L,
                    updatedAt = 0L
                )
            )

    val quotes: StateFlow<List<QuoteEntity>> = repository.observeQuotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val allPersonalGoals: StateFlow<List<PersonalGoalWithDetails>> =
        repository.observePersonalGoalsWithDetails()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _goalsFilter = MutableStateFlow(GoalsFilterState())
    val goalsFilter: StateFlow<GoalsFilterState> = _goalsFilter.asStateFlow()

    val filteredPersonalGoals: StateFlow<List<PersonalGoalWithDetails>> =
        combine(allPersonalGoals, _goalsFilter) { goals, filter ->
            val query = filter.searchQuery.trim().lowercase()
            goals.filter { item ->
                val g = item.goal
                val matchesStatus = when (filter.statusFilter) {
                    GoalStatusFilter.ACTIVE -> g.status == GoalStatus.ACTIVE
                    GoalStatusFilter.OVERDUE -> item.isOverdue
                    GoalStatusFilter.PAUSED -> g.status == GoalStatus.PAUSED
                    GoalStatusFilter.COMPLETED -> g.status == GoalStatus.COMPLETED
                    GoalStatusFilter.ALL -> true
                }
                val matchesCategory = filter.categoryFilterId == null || g.categoryId == filter.categoryFilterId
                val matchesQuery = query.isEmpty() ||
                    g.title.lowercase().contains(query) ||
                    g.description.lowercase().contains(query) ||
                    g.notes.lowercase().contains(query)
                matchesStatus && matchesCategory && matchesQuery
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _tasksFilter = MutableStateFlow(TasksFilterState())
    val tasksFilter: StateFlow<TasksFilterState> = _tasksFilter.asStateFlow()

    val filteredTasks: StateFlow<List<TaskWithDetails>> =
        combine(allTasks, _tasksFilter) { tasks, filter ->
            val now = DateTimeUtils.nowEpochMillis()
            val today = DateTimeUtils.todayLocalDate()
            val upcomingHorizon = today.plusDays(30)
            val query = filter.searchQuery.trim().lowercase()

            tasks.filter { item ->
                val t = item.task
                val dueDate = t.dueAt?.let { DateTimeUtils.toLocalDate(it) }
                val matchesTab = when (filter.tabMode) {
                    TasksTabMode.TODAY ->
                        t.status != TaskStatus.CANCELLED && dueDate == today
                    TasksTabMode.UPCOMING ->
                        !t.status.isTerminal &&
                            dueDate != null &&
                            dueDate.isAfter(today) &&
                            !dueDate.isAfter(upcomingHorizon)
                    TasksTabMode.OVERDUE -> NexusRepository.isTaskOverdue(t, now, today)
                    TasksTabMode.ALL, TasksTabMode.KANBAN, TasksTabMode.PROJECTS -> true
                    TasksTabMode.COMPLETED -> t.status == TaskStatus.COMPLETED
                }
                val matchesStatus = filter.statusFilter == null || t.status == filter.statusFilter
                val matchesPriority = filter.priorityFilter == null || t.priority == filter.priorityFilter
                val matchesProject = filter.projectFilterId == null || t.projectId == filter.projectFilterId
                val matchesQuery = query.isEmpty() ||
                    t.title.lowercase().contains(query) ||
                    t.description.lowercase().contains(query) ||
                    t.notes.lowercase().contains(query)

                matchesTab && matchesStatus && matchesPriority && matchesProject && matchesQuery
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _agendaFilter = MutableStateFlow(AgendaFilterState())
    val agendaFilter: StateFlow<AgendaFilterState> = _agendaFilter.asStateFlow()

    val allAgendaItems: StateFlow<List<AgendaItem>> = repository.observeUnifiedAgenda()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val agendaOccurrences: StateFlow<List<AgendaOccurrence>> =
        repository.observeAgendaOccurrences(
            filtersFlow = _agendaFilter.map { filter ->
                AgendaFilters(
                    kindFilter = filter.kindFilter,
                    categoryId = filter.categoryFilterId,
                    searchQuery = filter.searchQuery,
                    showPaid = filter.showPaid
                )
            }
        ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val filteredAgendaItems: StateFlow<List<AgendaItem>> =
        combine(allAgendaItems, _agendaFilter) { items, filter ->
            val query = filter.searchQuery.trim().lowercase()
            items.filter { item ->
                val matchesCategory = filter.categoryFilterId == null || item.category?.id == filter.categoryFilterId
                val matchesQuery = query.isEmpty() || when (item) {
                    is AgendaItem.AppointmentItem ->
                        item.appointment.title.lowercase().contains(query) ||
                            item.appointment.description.lowercase().contains(query) ||
                            item.appointment.location.lowercase().contains(query)
                    is AgendaItem.TaskDeadlineItem ->
                        item.task.title.lowercase().contains(query) ||
                            item.task.description.lowercase().contains(query)
                    is AgendaItem.EventItem ->
                        item.event.name.lowercase().contains(query) ||
                            item.event.locationName.lowercase().contains(query) ||
                            item.event.city.lowercase().contains(query)
                    is AgendaItem.FinancialDueItem ->
                        item.transaction.description.lowercase().contains(query)
                    is AgendaItem.StatementDueItem ->
                        item.card.name.lowercase().contains(query)
                    is AgendaItem.ProjectDeadlineItem ->
                        item.project.name.lowercase().contains(query)
                }
                matchesCategory && matchesQuery
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _financeFilter = MutableStateFlow(FinanceFilterState())
    val financeFilter: StateFlow<FinanceFilterState> = _financeFilter.asStateFlow()

    val financeDashboardData: StateFlow<MonthlyFinanceDashboardData> =
        repository.observeMonthlyFinanceDashboard(
            _financeFilter.map { it.selectedMonth }.distinctUntilChanged()
        ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MonthlyFinanceDashboardData())

    val filteredTransactions: StateFlow<List<TransactionWithDetails>> =
        combine(allTransactions, _financeFilter) { list, filter ->
            val query = filter.searchQuery.trim().lowercase()
            list.filter { item ->
                val tx = item.transaction
                val matchesType = filter.typeFilter == null || tx.type == filter.typeFilter
                val matchesStatus = filter.statusFilter == null || tx.status == filter.statusFilter
                val matchesCategory = filter.selectedCategoryFilterId == null ||
                    filter.selectedCategoryFilterId == FinanceRules.OTHERS_CATEGORY_ID ||
                    tx.categoryId == filter.selectedCategoryFilterId
                val matchesMethod = filter.paymentMethodFilter == null || tx.paymentMethod == filter.paymentMethodFilter
                val matchesQuery = query.isEmpty() ||
                    tx.description.lowercase().contains(query) ||
                    tx.notes.lowercase().contains(query) ||
                    (item.category?.name?.lowercase()?.contains(query) == true) ||
                    (item.event?.name?.lowercase()?.contains(query) == true) ||
                    (item.project?.name?.lowercase()?.contains(query) == true)
                matchesType && matchesStatus && matchesCategory && matchesMethod && matchesQuery
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _globalSearchQuery = MutableStateFlow("")
    val globalSearchQuery: StateFlow<String> = _globalSearchQuery.asStateFlow()

    val globalSearchResults: StateFlow<GlobalSearchResults> =
        _globalSearchQuery
            .flatMapLatest { query -> repository.observeGlobalSearch(query) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GlobalSearchResults())

    val tableCounts: StateFlow<DatabaseTableCounts> = repository.observeDatabaseTableCounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DatabaseTableCounts())

    val errorLogs: StateFlow<List<ErrorLogEntity>> = repository.observeLatestErrorLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _backupState = MutableStateFlow(BackupUiState())
    val backupState: StateFlow<BackupUiState> = _backupState.asStateFlow()

    // --- Filtros ---

    fun setTasksTabMode(mode: TasksTabMode) {
        _tasksFilter.value = _tasksFilter.value.copy(tabMode = mode)
    }

    fun setTasksStatusFilter(status: TaskStatus?) {
        _tasksFilter.value = _tasksFilter.value.copy(statusFilter = status)
    }

    fun setTasksPriorityFilter(priority: TaskPriority?) {
        _tasksFilter.value = _tasksFilter.value.copy(priorityFilter = priority)
    }

    fun setTasksProjectFilter(projectId: String?) {
        _tasksFilter.value = _tasksFilter.value.copy(projectFilterId = projectId)
    }

    fun setTasksSearchQuery(query: String) {
        _tasksFilter.value = _tasksFilter.value.copy(searchQuery = query)
    }

    fun clearTasksFilters() {
        _tasksFilter.value = _tasksFilter.value.copy(
            statusFilter = null,
            priorityFilter = null,
            projectFilterId = null,
            searchQuery = ""
        )
    }

    fun setAgendaViewMode(mode: AgendaViewMode) {
        _agendaFilter.value = _agendaFilter.value.copy(viewMode = mode)
    }

    fun setAgendaSelectedDate(date: LocalDate) {
        _agendaFilter.value = _agendaFilter.value.copy(selectedDate = date)
    }

    fun setAgendaKindFilter(kind: AgendaOccurrenceKind?) {
        val currentCatId = _agendaFilter.value.categoryFilterId
        val validCategoryIds = AgendaEngine
            .contextualCategoriesForKind(categories.value, kind)
            .map { it.id }
            .toSet()
        val nextCatId = currentCatId?.takeIf { it in validCategoryIds }
        _agendaFilter.value = _agendaFilter.value.copy(
            kindFilter = kind,
            categoryFilterId = nextCatId
        )
    }

    fun setAgendaCategoryFilter(categoryId: String?) {
        _agendaFilter.value = _agendaFilter.value.copy(categoryFilterId = categoryId)
    }

    fun setAgendaSearchQuery(query: String) {
        _agendaFilter.value = _agendaFilter.value.copy(searchQuery = query)
    }

    fun setAgendaShowPaid(showPaid: Boolean) {
        _agendaFilter.value = _agendaFilter.value.copy(showPaid = showPaid)
    }

    fun clearAgendaFilters() {
        _agendaFilter.value = _agendaFilter.value.copy(
            kindFilter = null,
            categoryFilterId = null,
            searchQuery = "",
            showPaid = false
        )
    }

    fun setFinanceTabMode(mode: FinanceTabMode) {
        _financeFilter.value = _financeFilter.value.copy(tabMode = mode)
    }

    fun setFinanceSelectedMonth(month: YearMonth) {
        _financeFilter.value = _financeFilter.value.copy(selectedMonth = month)
    }

    fun setFinanceTypeFilter(type: TransactionType?) {
        _financeFilter.value = _financeFilter.value.copy(typeFilter = type)
    }

    fun setFinanceStatusFilter(status: FinancialStatus?) {
        _financeFilter.value = _financeFilter.value.copy(statusFilter = status)
    }

    fun setFinanceCategoryFilter(categoryId: String?) {
        _financeFilter.value = _financeFilter.value.copy(selectedCategoryFilterId = categoryId)
    }

    fun setFinancePaymentMethodFilter(method: PaymentMethod?) {
        _financeFilter.value = _financeFilter.value.copy(paymentMethodFilter = method)
    }

    fun setFinanceSearchQuery(query: String) {
        _financeFilter.value = _financeFilter.value.copy(searchQuery = query)
    }

    fun clearFinanceFilters() {
        _financeFilter.value = _financeFilter.value.copy(
            typeFilter = null,
            statusFilter = null,
            selectedCategoryFilterId = null,
            paymentMethodFilter = null,
            searchQuery = ""
        )
    }

    fun setGlobalSearchQuery(query: String) {
        _globalSearchQuery.value = query
    }

    // --- Tarefas e Projetos ---

    fun saveTask(
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
        recurringEditScope: RecurringEditScope = RecurringEditScope.THIS_OCCURRENCE_ONLY,
        onSaved: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            repository.saveTask(
                id = id,
                title = title,
                description = description,
                projectId = projectId,
                categoryId = categoryId,
                eventId = eventId,
                goalId = goalId,
                priority = priority,
                status = status,
                dueAt = dueAt,
                hasDueTime = hasDueTime,
                notes = notes,
                subtaskDrafts = subtaskDrafts,
                reminderMinutesBefore = reminderMinutesBefore,
                recurrenceDraft = recurrenceDraft,
                recurringEditScope = recurringEditScope
            ).onSuccess { savedId ->
                emitMessage(if (id == null) "Tarefa criada" else "Tarefa atualizada")
                onSaved(savedId)
            }.onFailure { emitMessage(it.message ?: "Erro ao salvar tarefa") }
        }
    }

    fun openOrMaterializeRecurringOccurrence(
        recurrenceId: String,
        occurrenceDateIso: String,
        onReady: (String) -> Unit
    ) {
        viewModelScope.launch {
            repository.openOrMaterializeRecurringOccurrence(recurrenceId, occurrenceDateIso)
                .onSuccess { taskId -> onReady(taskId) }
                .onFailure { emitMessage(it.message ?: "Erro ao abrir ocorrência recorrente") }
        }
    }

    fun toggleTaskCompletion(taskId: String) {
        viewModelScope.launch {
            repository.toggleTaskCompletion(taskId)
                .onSuccess { nextStatus ->
                    emitMessage(if (nextStatus == TaskStatus.COMPLETED) "Tarefa concluída" else "Tarefa reaberta")
                }
                .onFailure { emitMessage(it.message ?: "Erro ao atualizar tarefa") }
        }
    }

    fun updateTaskStatus(taskId: String, status: TaskStatus) {
        viewModelScope.launch {
            repository.updateTaskStatus(taskId, status)
                .onSuccess { emitMessage("Status alterado para ${status.label}") }
                .onFailure { emitMessage(it.message ?: "Erro ao alterar status") }
        }
    }

    fun deleteTask(
        taskId: String,
        scope: RecurringEditScope = RecurringEditScope.THIS_OCCURRENCE_ONLY,
        onDeleted: () -> Unit = {}
    ) {
        viewModelScope.launch {
            repository.deleteTask(taskId, scope)
                .onSuccess {
                    emitMessage(
                        if (scope == RecurringEditScope.ENTIRE_SERIES) "Série recorrente excluída" else "Tarefa excluída"
                    )
                    onDeleted()
                }
                .onFailure { emitMessage(it.message ?: "Erro ao excluir tarefa") }
        }
    }

    fun toggleSubtask(subtask: SubtaskEntity) {
        viewModelScope.launch {
            repository.toggleSubtask(subtask)
                .onFailure { emitMessage(it.message ?: "Erro ao atualizar subtarefa") }
        }
    }

    fun saveProject(
        id: String? = null,
        name: String,
        description: String = "",
        deadlineAt: Long? = null,
        status: ProjectStatus = ProjectStatus.ACTIVE,
        priority: TaskPriority = TaskPriority.NORMAL,
        goalId: String? = null,
        onSaved: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            repository.saveProject(id, name, description, deadlineAt, status, priority, goalId)
                .onSuccess { savedId ->
                    emitMessage(if (id == null) "Projeto criado" else "Projeto atualizado")
                    onSaved(savedId)
                }
                .onFailure { emitMessage(it.message ?: "Erro ao salvar projeto") }
        }
    }

    suspend fun countTasksInProject(projectId: String): Int =
        repository.countTasksInProject(projectId)

    fun deleteProject(projectId: String, onDeleted: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteProjectAndUnlinkTasks(projectId)
                .onSuccess {
                    emitMessage("Projeto excluído e tarefas desvinculadas")
                    onDeleted()
                }
                .onFailure { emitMessage(it.message ?: "Erro ao excluir projeto") }
        }
    }

    fun suggestExpenseCategoryIdForTask(taskCategoryId: String?): String? =
        AgendaEngine.suggestExpenseCategoryId(taskCategoryId, categories.value)

    suspend fun countAppointmentsForEvent(eventId: String): Int =
        repository.countAppointmentsForEvent(eventId)

    // --- Compromissos ---

    fun saveAppointment(
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
        onSaved: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            repository.saveAppointment(
                id = id,
                title = title,
                description = description,
                categoryId = categoryId,
                eventId = eventId,
                startAt = startAt,
                endAt = endAt,
                isAllDay = isAllDay,
                location = location,
                link = link,
                reminderMinutesBefore = reminderMinutesBefore
            ).onSuccess { savedId ->
                emitMessage(if (id == null) "Compromisso salvo" else "Compromisso atualizado")
                onSaved(savedId)
            }.onFailure { emitMessage(it.message ?: "Erro ao salvar compromisso") }
        }
    }

    fun deleteAppointment(appointmentId: String, onDeleted: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteAppointment(appointmentId)
                .onSuccess {
                    emitMessage("Compromisso excluído")
                    onDeleted()
                }
                .onFailure { emitMessage(it.message ?: "Erro ao excluir compromisso") }
        }
    }

    // --- Eventos ---

    fun saveEvent(
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
        onSaved: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            repository.saveEvent(
                id = id,
                name = name,
                description = description,
                categoryId = categoryId,
                startAt = startAt,
                endAt = endAt,
                isAllDay = isAllDay,
                officialLink = officialLink,
                ticketLink = ticketLink,
                locationName = locationName,
                address = address,
                city = city,
                state = state,
                notes = notes,
                eventStatus = eventStatus,
                ticketStatus = ticketStatus,
                ticketPriceCents = ticketPriceCents,
                ticketQuantity = ticketQuantity,
                ticketFeesCents = ticketFeesCents,
                ticketPurchaseDate = ticketPurchaseDate,
                ticketDueDate = ticketDueDate,
                paymentMethod = paymentMethod,
                creditCardId = creditCardId,
                installmentsCount = installmentsCount,
                reminderMinutesBefore = reminderMinutesBefore
            ).onSuccess { savedId ->
                emitMessage(if (id == null) "Evento salvo" else "Evento atualizado")
                onSaved(savedId)
            }.onFailure { emitMessage(it.message ?: "Erro ao salvar evento") }
        }
    }

    fun deleteEvent(eventId: String, onDeleted: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteEvent(eventId)
                .onSuccess {
                    emitMessage("Evento excluído")
                    onDeleted()
                }
                .onFailure { emitMessage(it.message ?: "Erro ao excluir evento") }
        }
    }

    // --- Financeiro ---

    fun updateDeclaredBalance(balanceCents: Long) {
        viewModelScope.launch {
            repository.updateDeclaredBalance(balanceCents)
                .onSuccess { emitMessage("Saldo atualizado") }
                .onFailure { emitMessage(it.message ?: "Erro ao atualizar saldo") }
        }
    }

    fun saveTransaction(
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
        reminderMinutesBefore: Int? = null,
        onSaved: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            repository.saveTransaction(
                id = id,
                type = type,
                description = description,
                amountCents = amountCents,
                categoryId = categoryId,
                status = status,
                paymentMethod = paymentMethod,
                occurredAt = occurredAt,
                dueAt = dueAt,
                settledAt = settledAt,
                projectId = projectId,
                eventId = eventId,
                taskId = taskId,
                eventCostType = eventCostType,
                creditCardId = creditCardId,
                notes = notes,
                reminderMinutesBefore = reminderMinutesBefore
            ).onSuccess { savedId ->
                emitMessage(if (id == null) "${type.label} registrada" else "${type.label} atualizada")
                onSaved(savedId)
            }.onFailure { emitMessage(it.message ?: "Erro ao salvar lançamento") }
        }
    }

    fun settleTransaction(transactionId: String) {
        viewModelScope.launch {
            repository.settleTransaction(transactionId)
                .onSuccess { emitMessage("Situação financeira atualizada") }
                .onFailure { emitMessage(it.message ?: "Erro ao atualizar lançamento") }
        }
    }

    fun deleteTransaction(transactionId: String, onDeleted: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteTransaction(transactionId)
                .onSuccess {
                    emitMessage("Lançamento excluído")
                    onDeleted()
                }
                .onFailure { emitMessage(it.message ?: "Erro ao excluir lançamento") }
        }
    }

    fun saveCreditCard(
        id: String? = null,
        name: String,
        institution: String = "",
        totalLimitCents: Long,
        availableLimitCents: Long,
        closingDay: Int,
        dueDay: Int,
        colorHex: String = "#9A9A9A",
        notes: String = "",
        onSaved: () -> Unit = {}
    ) {
        viewModelScope.launch {
            repository.saveCreditCard(
                id, name, institution, totalLimitCents, availableLimitCents, closingDay, dueDay, colorHex, notes
            ).onSuccess {
                emitMessage(if (id == null) "Cartão adicionado" else "Cartão atualizado")
                onSaved()
            }.onFailure { emitMessage(it.message ?: "Erro ao salvar cartão") }
        }
    }

    fun deleteCreditCard(cardId: String) {
        viewModelScope.launch {
            repository.deleteCreditCard(cardId)
                .onSuccess { emitMessage("Cartão removido") }
                .onFailure { emitMessage(it.message ?: "Erro ao remover cartão") }
        }
    }

    fun payCreditCardStatement(statementId: String) {
        viewModelScope.launch {
            repository.payCreditCardStatement(statementId)
                .onSuccess { emitMessage("Fatura quitada sem duplicar compras") }
                .onFailure { emitMessage(it.message ?: "Erro ao pagar fatura") }
        }
    }

    fun closeCreditCardStatement(statementId: String) {
        viewModelScope.launch {
            repository.closeCreditCardStatement(statementId)
                .onSuccess { emitMessage("Fatura fechada") }
                .onFailure { emitMessage(it.message ?: "Erro ao fechar fatura") }
        }
    }

    fun createInstallmentPurchase(
        description: String,
        totalAmountCents: Long,
        installmentsCount: Int,
        firstDueDate: LocalDate,
        creditCardId: String?,
        categoryId: String?,
        projectId: String? = null,
        eventId: String? = null,
        notes: String = "",
        onSaved: () -> Unit = {}
    ) {
        viewModelScope.launch {
            repository.createInstallmentPurchase(
                description, totalAmountCents, installmentsCount, firstDueDate,
                creditCardId, categoryId, projectId, eventId, notes
            ).onSuccess {
                emitMessage("Parcelamento gerado (${installmentsCount}x)")
                onSaved()
            }.onFailure { emitMessage(it.message ?: "Erro ao criar parcelamento") }
        }
    }

    fun settleAllRemainingInstallments(purchaseId: String) {
        viewModelScope.launch {
            repository.settleAllRemainingInstallments(purchaseId)
                .onSuccess { emitMessage("Parcelamento quitado") }
                .onFailure { emitMessage(it.message ?: "Erro ao quitar parcelamento") }
        }
    }

    fun deleteInstallmentPurchase(purchaseId: String) {
        viewModelScope.launch {
            repository.deleteInstallmentPurchase(purchaseId)
                .onSuccess { emitMessage("Parcelamento excluído (parcelas pagas preservadas)") }
                .onFailure { emitMessage(it.message ?: "Erro ao excluir parcelamento") }
        }
    }

    fun saveRecurringRule(
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
        notes: String = "",
        onSaved: () -> Unit = {}
    ) {
        viewModelScope.launch {
            repository.saveRecurringRule(
                title, type, amountCents, categoryId, paymentMethod, creditCardId,
                frequency, customIntervalDays, startDate, endDate, maxOccurrences, notes
            ).onSuccess {
                emitMessage("Recorrência criada e ocorrências geradas")
                onSaved()
            }.onFailure { emitMessage(it.message ?: "Erro ao salvar recorrência") }
        }
    }

    fun toggleRecurringRuleActive(rule: RecurringRuleEntity) {
        viewModelScope.launch {
            repository.toggleRecurringRuleActive(rule)
                .onSuccess { emitMessage(if (rule.isActive) "Recorrência pausada" else "Recorrência ativada") }
        }
    }

    fun deleteRecurringRule(ruleId: String) {
        viewModelScope.launch {
            repository.deleteRecurringRule(ruleId)
                .onSuccess { emitMessage("Recorrência excluída (histórico pago preservado)") }
                .onFailure { emitMessage(it.message ?: "Erro ao excluir recorrência") }
        }
    }

    fun saveLoan(
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
        notes: String = "",
        onSaved: () -> Unit = {}
    ) {
        viewModelScope.launch {
            repository.saveLoan(
                id, creditor, originalAmountCents, contractedAmountCents,
                installmentsTotal, installmentAmountCents, paidInstallments,
                nextDueAt, remainingBalanceCents, finalDateAt, notes
            ).onSuccess {
                emitMessage("Empréstimo salvo")
                onSaved()
            }.onFailure { emitMessage(it.message ?: "Erro ao salvar empréstimo") }
        }
    }

    fun registerLoanInstallmentPaid(loan: LoanEntity, newRemainingBalanceCents: Long) {
        viewModelScope.launch {
            repository.registerLoanInstallmentPaid(loan, newRemainingBalanceCents)
                .onSuccess { emitMessage("Parcela de empréstimo registrada") }
                .onFailure { emitMessage(it.message ?: "Erro ao registrar parcela") }
        }
    }

    fun deleteLoan(loanId: String) {
        viewModelScope.launch {
            repository.deleteLoan(loanId)
                .onSuccess { emitMessage("Empréstimo excluído") }
        }
    }

    fun saveBudget(categoryId: String, limitCents: Long, onSaved: () -> Unit = {}) {
        viewModelScope.launch {
            val targetYm = _financeFilter.value.selectedMonth.toString()
            repository.saveBudget(categoryId, targetYm, limitCents)
                .onSuccess {
                    emitMessage("Orçamento salvo")
                    onSaved()
                }
                .onFailure { emitMessage(it.message ?: "Erro ao salvar orçamento") }
        }
    }

    fun deleteBudget(budgetId: String) {
        viewModelScope.launch {
            repository.deleteBudget(budgetId)
                .onSuccess { emitMessage("Orçamento removido") }
        }
    }

    fun saveFinancialGoal(
        id: String? = null,
        name: String,
        targetAmountCents: Long,
        currentAmountCents: Long,
        deadlineAt: Long?,
        categoryId: String? = null,
        notes: String = "",
        onSaved: () -> Unit = {}
    ) {
        viewModelScope.launch {
            repository.saveFinancialGoal(id, name, targetAmountCents, currentAmountCents, deadlineAt, categoryId, notes)
                .onSuccess {
                    emitMessage("Meta financeira salva")
                    onSaved()
                }
                .onFailure { emitMessage(it.message ?: "Erro ao salvar meta") }
        }
    }

    fun updateFinancialGoalAmount(goal: FinancialGoalEntity, newCurrentCents: Long) {
        viewModelScope.launch {
            repository.updateFinancialGoalAmount(goal, newCurrentCents)
                .onSuccess { emitMessage("Progresso da meta atualizado") }
        }
    }

    fun deleteFinancialGoal(goalId: String) {
        viewModelScope.launch {
            repository.deleteFinancialGoal(goalId)
                .onSuccess { emitMessage("Meta removida") }
        }
    }

    // --- Configuracoes e Categorias ---

    fun saveSettings(userName: String, darkTheme: Boolean) {
        viewModelScope.launch {
            repository.saveSettings(userName, darkTheme)
                .onSuccess { emitMessage("Configurações salvas") }
                .onFailure { emitMessage(it.message ?: "Erro ao salvar configurações") }
        }
    }

    fun saveCategory(
        id: String? = null,
        name: String,
        colorHex: String,
        scope: CategoryScope = CategoryScope.GENERAL
    ) {
        viewModelScope.launch {
            repository.saveCategory(id, name, colorHex, scope)
                .onSuccess { emitMessage(if (id == null) "Categoria adicionada" else "Categoria atualizada") }
                .onFailure { emitMessage(it.message ?: "Erro ao salvar categoria") }
        }
    }

    fun deleteCategory(categoryId: String) {
        viewModelScope.launch {
            repository.deleteCategory(categoryId)
                .onSuccess { emitMessage("Categoria removida") }
                .onFailure { emitMessage(it.message ?: "Erro ao remover categoria") }
        }
    }

    // --- Assistente de Rotina e Notificacoes (Fase 4) ---

    fun saveRoutineSettings(settings: RoutineAssistantSettingsEntity) {
        viewModelScope.launch {
            repository.saveRoutineSettings(settings)
                .onSuccess { emitMessage("Preferências de rotina e notificações salvas") }
                .onFailure { emitMessage(it.message ?: "Erro ao salvar preferências") }
        }
    }

    fun postponeTaskToTomorrow(taskId: String) {
        viewModelScope.launch {
            repository.postponeTaskToTomorrow(taskId)
                .onSuccess { emitMessage("Tarefa adiada para amanhã") }
                .onFailure { emitMessage(it.message ?: "Erro ao adiar tarefa") }
        }
    }

    fun postponeTransactionToTomorrow(transactionId: String) {
        viewModelScope.launch {
            repository.postponeTransactionToTomorrow(transactionId)
                .onSuccess { emitMessage("Lançamento adiado para amanhã") }
                .onFailure { emitMessage(it.message ?: "Erro ao adiar lançamento") }
        }
    }

    fun markBriefingReviewed(type: BriefingType) {
        viewModelScope.launch {
            repository.markBriefingReviewed(type)
                .onSuccess {
                    val label = if (type == BriefingType.MORNING) "Briefing Diário marcado como revisado" else "Revisão Noturna concluída"
                    emitMessage(label)
                }
                .onFailure { emitMessage(it.message ?: "Erro ao registrar revisão") }
        }
    }

    fun snoozeReminder(reminderId: String, minutes: Int = 15) {
        viewModelScope.launch {
            repository.snoozeReminder(reminderId, minutes)
                .onSuccess { emitMessage("Lembrete adiado em $minutes min") }
                .onFailure { emitMessage(it.message ?: "Erro ao adiar lembrete") }
        }
    }

    fun dismissReminder(reminderId: String) {
        viewModelScope.launch {
            repository.dismissReminder(reminderId)
                .onSuccess { emitMessage("Lembrete dispensado") }
                .onFailure { emitMessage(it.message ?: "Erro ao dispensar lembrete") }
        }
    }

    fun cancelReminder(reminderId: String) {
        viewModelScope.launch {
            repository.cancelReminder(reminderId)
                .onSuccess { emitMessage("Lembrete cancelado") }
                .onFailure { emitMessage(it.message ?: "Erro ao cancelar lembrete") }
        }
    }

    fun sendTestNotification() {
        viewModelScope.launch {
            repository.sendTestNotification()
                .onSuccess { shown ->
                    if (shown) {
                        emitMessage("Notificação de teste disparada")
                    } else {
                        emitMessage("Permissão de notificação não concedida no sistema")
                    }
                }
                .onFailure { emitMessage(it.message ?: "Erro ao disparar notificação de teste") }
        }
    }

    fun rescheduleAllReminders() {
        viewModelScope.launch {
            repository.rescheduleAllReminders()
                .onSuccess { emitMessage("Todos os lembretes foram reagendados") }
                .onFailure { emitMessage(it.message ?: "Erro ao reagendar lembretes") }
        }
    }

    // --- Backup, CSV e Diagnostico ---

    suspend fun generateBackupJson(): Result<String> {
        _backupState.value = _backupState.value.copy(isBusy = true)
        val result = repository.exportBackupJson()
        _backupState.value = _backupState.value.copy(isBusy = false)
        result.onFailure { emitMessage("Falha ao gerar backup: ${it.message}") }
        return result
    }

    suspend fun generateFinancialCsv(): Result<String> = repository.exportFinancialCsv()

    fun notifyBackupExported() {
        emitMessage("Backup exportado com sucesso")
    }

    fun notifyCsvExported() {
        emitMessage("CSV financeiro exportado com sucesso")
    }

    fun inspectBackupJson(rawJson: String) {
        when (val result = repository.validateBackupJson(rawJson)) {
            is BackupValidationResult.Valid -> {
                _backupState.value = BackupUiState(pendingPreview = result.preview)
            }
            is BackupValidationResult.Invalid -> {
                _backupState.value = BackupUiState(validationError = result.userMessage)
            }
        }
    }

    fun confirmRestoreBackup() {
        val preview = _backupState.value.pendingPreview ?: return
        viewModelScope.launch {
            _backupState.value = _backupState.value.copy(isBusy = true)
            repository.restoreBackupAtomically(preview.payload)
                .onSuccess { summary ->
                    _backupState.value = BackupUiState(lastRestoreSummary = summary)
                    emitMessage("Restauração concluída: ${summary.totalRestored} registros")
                }
                .onFailure { err ->
                    _backupState.value = BackupUiState(
                        validationError = "Falha na restauração (dados atuais preservados): ${err.message}"
                    )
                }
        }
    }

    fun dismissBackupDialogs() {
        _backupState.value = BackupUiState()
    }

    fun clearErrorLogs() {
        viewModelScope.launch {
            repository.clearErrorLogs()
                .onSuccess { emitMessage("Histórico de erros limpo") }
        }
    }

    fun logUiError(source: String, throwable: Throwable) {
        viewModelScope.launch {
            repository.logError(source, throwable)
            emitMessage("Erro registrado em Diagnóstico")
        }
    }

    // --- Fase 6: Personalizacao do Dashboard, Frases Locais e Objetivos Pessoais ---

    fun saveGreetingAndPresentation(
        greetingEnabled: Boolean,
        greetingTemplate: String,
        dateFormatPattern: NexusDateFormat,
        showDailyQuoteOnDashboard: Boolean,
        compactDashboardMode: Boolean,
        showMetricSummaryOnDashboard: Boolean
    ) {
        viewModelScope.launch {
            repository.saveGreetingAndPresentation(
                greetingEnabled = greetingEnabled,
                greetingTemplate = greetingTemplate,
                dateFormatPattern = dateFormatPattern,
                showDailyQuoteOnDashboard = showDailyQuoteOnDashboard,
                compactDashboardMode = compactDashboardMode,
                showMetricSummaryOnDashboard = showMetricSummaryOnDashboard
            ).onSuccess {
                emitMessage("Preferências de apresentação salvas")
            }.onFailure {
                emitMessage(it.message ?: "Erro ao salvar personalização")
            }
        }
    }

    fun saveDashboardBlocks(blocks: List<DashboardBlockConfig>) {
        viewModelScope.launch {
            repository.saveDashboardBlocks(blocks)
                .onFailure { emitMessage(it.message ?: "Erro ao atualizar blocos") }
        }
    }

    fun toggleDashboardBlock(type: DashboardBlockType, enabled: Boolean) {
        val current = NexusRepository.parseDashboardBlocks(personalizationSettings.value.dashboardBlocksJson)
        val updated = NexusRepository.toggleDashboardBlock(current, type, enabled)
        saveDashboardBlocks(updated)
    }

    fun moveDashboardBlock(type: DashboardBlockType, delta: Int) {
        val current = NexusRepository.parseDashboardBlocks(personalizationSettings.value.dashboardBlocksJson)
        val updated = NexusRepository.moveDashboardBlock(current, type, delta)
        saveDashboardBlocks(updated)
    }

    fun updateDashboardBlockConfig(updated: DashboardBlockConfig) {
        val current = NexusRepository.parseDashboardBlocks(personalizationSettings.value.dashboardBlocksJson)
        val next = NexusRepository.updateDashboardBlockConfig(current, updated)
        saveDashboardBlocks(next)
    }

    fun resetDashboardToDefault() {
        viewModelScope.launch {
            repository.resetDashboardToDefault()
                .onSuccess { emitMessage("Organização padrão do dashboard restaurada") }
                .onFailure { emitMessage(it.message ?: "Erro ao restaurar padrão") }
        }
    }

    fun refreshDailyQuote() {
        viewModelScope.launch {
            repository.refreshDailyQuoteIfNeeded()
        }
    }

    fun saveQuote(
        id: String? = null,
        text: String,
        author: String = "",
        isActive: Boolean = true,
        onSaved: () -> Unit = {}
    ) {
        viewModelScope.launch {
            repository.saveQuote(id, text, author, isActive)
                .onSuccess {
                    repository.refreshDailyQuoteIfNeeded()
                    emitMessage(if (id == null) "Frase adicionada à biblioteca" else "Frase atualizada")
                    onSaved()
                }
                .onFailure { emitMessage(it.message ?: "Erro ao salvar frase") }
        }
    }

    fun toggleQuoteActive(quoteId: String) {
        viewModelScope.launch {
            repository.toggleQuoteActive(quoteId)
                .onSuccess { repository.refreshDailyQuoteIfNeeded() }
                .onFailure { emitMessage(it.message ?: "Erro ao atualizar frase") }
        }
    }

    fun deleteQuote(quoteId: String) {
        viewModelScope.launch {
            repository.deleteQuote(quoteId)
                .onSuccess {
                    repository.refreshDailyQuoteIfNeeded()
                    emitMessage("Frase removida")
                }
                .onFailure { emitMessage(it.message ?: "Erro ao remover frase") }
        }
    }

    fun importQuotesFromText(rawContent: String) {
        viewModelScope.launch {
            repository.importQuotesFromText(rawContent)
                .onSuccess { count ->
                    repository.refreshDailyQuoteIfNeeded()
                    emitMessage(
                        if (count == 0) "Nenhuma frase nova importada (duplicadas ignoradas)"
                        else "$count frase(s) importada(s) localmente"
                    )
                }
                .onFailure { emitMessage(it.message ?: "Arquivo de frases inválido") }
        }
    }

    suspend fun exportQuotesJson(): Result<String> = repository.exportQuotesJson()

    fun notifyQuotesExported() {
        emitMessage("Biblioteca de frases exportada em JSON")
    }

    fun setGoalsStatusFilter(filter: GoalStatusFilter) {
        _goalsFilter.value = _goalsFilter.value.copy(statusFilter = filter)
    }

    fun setGoalsCategoryFilter(categoryId: String?) {
        _goalsFilter.value = _goalsFilter.value.copy(categoryFilterId = categoryId)
    }

    fun setGoalsSearchQuery(query: String) {
        _goalsFilter.value = _goalsFilter.value.copy(searchQuery = query)
    }

    fun savePersonalGoal(
        id: String? = null,
        title: String,
        description: String = "",
        categoryId: String? = null,
        targetDateAt: Long? = null,
        status: GoalStatus = GoalStatus.ACTIVE,
        progressMode: GoalProgressMode = GoalProgressMode.AUTO,
        manualProgressPercent: Int = 0,
        notes: String = "",
        onSaved: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            repository.savePersonalGoal(
                id = id,
                title = title,
                description = description,
                categoryId = categoryId,
                targetDateAt = targetDateAt,
                status = status,
                progressMode = progressMode,
                manualProgressPercent = manualProgressPercent,
                notes = notes
            ).onSuccess { savedId ->
                emitMessage(if (id == null) "Objetivo criado" else "Objetivo atualizado")
                onSaved(savedId)
            }.onFailure { emitMessage(it.message ?: "Erro ao salvar objetivo") }
        }
    }

    fun updatePersonalGoalStatus(goalId: String, status: GoalStatus) {
        viewModelScope.launch {
            repository.updatePersonalGoalStatus(goalId, status)
                .onSuccess { emitMessage("Status do objetivo alterado para ${status.label}") }
                .onFailure { emitMessage(it.message ?: "Erro ao alterar status do objetivo") }
        }
    }

    fun addGoalProgressLog(goalId: String, progressPercent: Int, note: String) {
        viewModelScope.launch {
            repository.addGoalProgressLog(goalId, progressPercent, note)
                .onSuccess { emitMessage("Evolução registrada") }
                .onFailure { emitMessage(it.message ?: "Erro ao registrar evolução") }
        }
    }

    fun deletePersonalGoal(goalId: String, onDeleted: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deletePersonalGoal(goalId)
                .onSuccess {
                    emitMessage("Objetivo excluído (tarefas e projetos mantidos)")
                    onDeleted()
                }
                .onFailure { emitMessage(it.message ?: "Erro ao excluir objetivo") }
        }
    }

    fun saveMilestone(
        id: String? = null,
        goalId: String,
        title: String,
        description: String = "",
        dueAt: Long? = null,
        onSaved: () -> Unit = {}
    ) {
        viewModelScope.launch {
            repository.saveMilestone(id, goalId, title, description, dueAt)
                .onSuccess {
                    emitMessage(if (id == null) "Marco adicionado" else "Marco atualizado")
                    onSaved()
                }
                .onFailure { emitMessage(it.message ?: "Erro ao salvar marco") }
        }
    }

    fun toggleMilestoneCompleted(milestone: GoalMilestoneEntity) {
        viewModelScope.launch {
            repository.toggleMilestoneCompleted(milestone)
                .onFailure { emitMessage(it.message ?: "Erro ao atualizar marco") }
        }
    }

    fun deleteMilestone(milestoneId: String) {
        viewModelScope.launch {
            repository.deleteMilestone(milestoneId)
                .onSuccess { emitMessage("Marco removido") }
                .onFailure { emitMessage(it.message ?: "Erro ao remover marco") }
        }
    }

    fun linkTaskToGoal(taskId: String, goalId: String?) {
        viewModelScope.launch {
            repository.linkTaskToGoal(taskId, goalId)
                .onSuccess {
                    emitMessage(if (goalId != null) "Tarefa vinculada ao objetivo" else "Tarefa desvinculada do objetivo")
                }
                .onFailure { emitMessage(it.message ?: "Erro ao vincular tarefa") }
        }
    }

    fun linkProjectToGoal(projectId: String, goalId: String?) {
        viewModelScope.launch {
            repository.linkProjectToGoal(projectId, goalId)
                .onSuccess {
                    emitMessage(if (goalId != null) "Projeto vinculado ao objetivo" else "Projeto desvinculado do objetivo")
                }
                .onFailure { emitMessage(it.message ?: "Erro ao vincular projeto") }
        }
    }

    fun refreshUsdBrlQuote(manual: Boolean = true) {
        refreshMarketAsset(com.nexus.app.domain.model.MarketAssetCatalog.USD_BRL_CODE, manual = manual)
    }

    fun refreshMarketAsset(assetCode: String, manual: Boolean = true) {
        val repo = marketQuoteRepository ?: return
        viewModelScope.launch {
            val state = repo.refreshAsset(assetCode, force = manual)
            if (manual && state.lastRefreshFailed && !state.statusMessage.isNullOrBlank()) {
                emitMessage(state.statusMessage)
            }
        }
    }

    fun refreshAllMarketQuotes(manual: Boolean = true) {
        val repo = marketQuoteRepository ?: return
        viewModelScope.launch {
            val states = repo.refreshAll(force = manual)
            if (manual) {
                val failedCount = states.values.count { it.lastRefreshFailed }
                if (failedCount == states.size && states.isNotEmpty()) {
                    val sampleMsg = states.values.firstOrNull { !it.statusMessage.isNullOrBlank() }?.statusMessage
                    if (!sampleMsg.isNullOrBlank()) emitMessage(sampleMsg)
                } else if (failedCount > 0) {
                    emitMessage("Algumas cotações não puderam ser atualizadas — cache preservado")
                }
            }
        }
    }

    fun loadMarketHistory(
        assetCode: String,
        range: MarketHistoryRange,
        manual: Boolean = false
    ) {
        val repo = marketHistoryRepository ?: return
        viewModelScope.launch {
            val state = repo.loadHistory(assetCode = assetCode, range = range, force = manual)
            if (manual && state.lastRefreshFailed && !state.statusMessage.isNullOrBlank()) {
                emitMessage(state.statusMessage)
            }
        }
    }

    fun toggleMarketFavorite(assetCode: String) {
        marketWatchlistRepository?.toggleFavorite(assetCode)
    }

    fun createMarketAlert(
        assetCode: String,
        condition: MarketAlertCondition,
        targetPrice: Double,
        isActive: Boolean
    ) {
        val repo = marketAlertRepository ?: return
        val currentQuote = marketQuoteStates.value[assetCode]?.quote
        repo.createAlert(
            assetCode = assetCode,
            condition = condition,
            targetPrice = targetPrice,
            isActive = isActive,
            currentQuote = currentQuote
        ).onSuccess {
            emitMessage(
                if (isActive) {
                    "Alerta criado e aguardando próxima atualização válida"
                } else {
                    "Alerta salvo como pausado"
                }
            )
        }.onFailure {
            emitMessage(it.message ?: "Não foi possível criar o alerta")
        }
    }

    fun updateMarketAlert(
        alertId: String,
        assetCode: String,
        condition: MarketAlertCondition,
        targetPrice: Double,
        isActive: Boolean
    ) {
        val repo = marketAlertRepository ?: return
        val currentQuote = marketQuoteStates.value[assetCode]?.quote
        repo.updateAlert(
            alertId = alertId,
            condition = condition,
            targetPrice = targetPrice,
            isActive = isActive,
            currentQuote = currentQuote
        ).onSuccess { updated ->
            val msg = when {
                !updated.isActive -> "Alerta atualizado e pausado"
                updated.awaitingConditionReset -> "Alerta reativado — aguardando retorno do preço para rearmar"
                else -> "Alerta atualizado"
            }
            emitMessage(msg)
        }.onFailure {
            emitMessage(it.message ?: "Não foi possível atualizar o alerta")
        }
    }

    fun toggleMarketAlertActive(alertId: String, assetCode: String, active: Boolean) {
        val repo = marketAlertRepository ?: return
        val currentQuote = marketQuoteStates.value[assetCode]?.quote
        repo.setAlertActive(
            alertId = alertId,
            active = active,
            currentQuote = currentQuote
        ).onSuccess { updated ->
            val msg = when {
                !updated.isActive -> "Alerta pausado"
                updated.awaitingConditionReset -> "Alerta reativado — aguardando retorno do preço para rearmar"
                else -> "Alerta ativado"
            }
            emitMessage(msg)
        }.onFailure {
            emitMessage(it.message ?: "Não foi possível alterar o alerta")
        }
    }

    fun deleteMarketAlert(alertId: String) {
        val repo = marketAlertRepository ?: return
        if (repo.deleteAlert(alertId)) {
            emitMessage("Alerta excluído")
        }
    }

    fun emitMessage(message: String) {
        _snackbarEvents.tryEmit(message)
    }

    class Factory(
        private val repository: NexusRepository,
        private val marketQuoteRepository: MarketQuoteRepository? = null,
        private val marketHistoryRepository: MarketHistoryRepository? = null,
        private val marketWatchlistRepository: MarketWatchlistRepository? = null,
        private val marketAlertRepository: MarketAlertRepository? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return NexusViewModel(
                repository = repository,
                marketQuoteRepository = marketQuoteRepository,
                marketHistoryRepository = marketHistoryRepository,
                marketWatchlistRepository = marketWatchlistRepository,
                marketAlertRepository = marketAlertRepository
            ) as T
        }
    }
}
