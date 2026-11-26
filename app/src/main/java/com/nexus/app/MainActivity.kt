package com.nexus.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ConfirmationNumber
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.nexus.app.ui.components.ConfirmDiscardChangesDialog
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nexus.app.data.db.entity.AppointmentEntity
import com.nexus.app.data.db.entity.CreditCardEntity
import com.nexus.app.data.db.entity.FinancialGoalEntity
import com.nexus.app.data.db.entity.LoanEntity
import com.nexus.app.domain.model.MarketAssetCatalog
import com.nexus.app.domain.model.ReminderCategory
import com.nexus.app.domain.model.ReminderSourceType
import com.nexus.app.domain.model.TransactionType
import com.nexus.app.domain.rules.BackAction
import com.nexus.app.domain.rules.BackNavigationState
import com.nexus.app.domain.rules.BackSubRouteKind
import com.nexus.app.domain.rules.BackTabKind
import com.nexus.app.domain.rules.resolveBackAction
import com.nexus.app.notifications.NexusNotificationScheduler
import com.nexus.app.ui.FinanceTabMode
import com.nexus.app.ui.NexusViewModel
import com.nexus.app.ui.TasksTabMode
import com.nexus.app.ui.components.NexusWordmark
import com.nexus.app.ui.screens.AgendaScreen
import com.nexus.app.ui.screens.AppointmentEditorSheet
import com.nexus.app.ui.screens.BackupScreen
import com.nexus.app.ui.screens.BudgetEditorDialog
import com.nexus.app.ui.screens.CreditCardEditorSheet
import com.nexus.app.ui.screens.DashboardCustomizationScreen
import com.nexus.app.ui.screens.DeclaredBalanceDialog
import com.nexus.app.ui.screens.DiagnosticsScreen
import com.nexus.app.ui.screens.EventEditorSheet
import com.nexus.app.ui.screens.EventsScreen
import com.nexus.app.ui.screens.FinanceScreen
import com.nexus.app.ui.screens.FinancialGoalEditorDialog
import com.nexus.app.ui.screens.GlobalSearchScreen
import com.nexus.app.ui.screens.HomeScreen
import com.nexus.app.ui.screens.InstallmentPurchaseEditorSheet
import com.nexus.app.ui.screens.LoanEditorSheet
import com.nexus.app.ui.screens.LoanPaymentDialog
import com.nexus.app.ui.screens.MarketAssetDetailScreen
import com.nexus.app.ui.screens.MarketScreen
import com.nexus.app.ui.screens.MoreScreen
import com.nexus.app.ui.screens.PlanningGoalsScreen
import com.nexus.app.ui.screens.ProjectEditorSheet
import com.nexus.app.ui.screens.QuickAddChooserSheet
import com.nexus.app.ui.screens.QuoteLibraryScreen
import com.nexus.app.ui.screens.RecurringRuleEditorSheet
import com.nexus.app.ui.screens.RoutineAssistantScreen
import com.nexus.app.ui.screens.SettingsScreen
import com.nexus.app.ui.screens.TaskEditorSheet
import com.nexus.app.ui.screens.TasksAndProjectsScreen
import com.nexus.app.ui.screens.TransactionEditorSheet
import com.nexus.app.ui.screens.WidgetsHubScreen
import com.nexus.app.ui.theme.LocalNexusPalette
import com.nexus.app.ui.theme.NexusTheme
import com.nexus.app.widgets.NexusWidgetNavigation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class MainTab(val label: String) {
    HOME("Início"),
    AGENDA("Agenda"),
    TASKS("Tarefas"),
    FINANCE("Finanças"),
    EVENTS("Eventos")
}

private val BOTTOM_NAV_TAB_ORDER = listOf(
    MainTab.TASKS,
    MainTab.AGENDA,
    MainTab.HOME,
    MainTab.FINANCE,
    MainTab.EVENTS
)

sealed interface SubRoute {
    data object None : SubRoute
    data object MoreMenu : SubRoute
    data object WidgetsHub : SubRoute
    data object GlobalSearch : SubRoute
    data object Settings : SubRoute
    data object Backup : SubRoute
    data object Diagnostics : SubRoute
    data class RoutineAssistant(val initialTabIndex: Int = 0) : SubRoute
    data class Planning(val initialGoalId: String? = null) : SubRoute
    data object DashboardCustomization : SubRoute
    data object QuoteLibrary : SubRoute
    data object Market : SubRoute
    data class MarketAssetDetail(val assetCode: String) : SubRoute
}

private fun SubRoute.toBackSubRouteKind(): BackSubRouteKind = when (this) {
    SubRoute.None -> BackSubRouteKind.NONE
    SubRoute.MoreMenu,
    SubRoute.GlobalSearch,
    SubRoute.Market,
    is SubRoute.MarketAssetDetail,
    SubRoute.DashboardCustomization,
    is SubRoute.RoutineAssistant -> BackSubRouteKind.TOP_LEVEL_SUBROUTE
    is SubRoute.Planning,
    SubRoute.QuoteLibrary,
    SubRoute.WidgetsHub,
    SubRoute.Settings,
    SubRoute.Backup,
    SubRoute.Diagnostics -> BackSubRouteKind.MORE_CHILD_SUBROUTE
}

sealed interface ActiveEditor {
    data object None : ActiveEditor
    data object QuickAddChooser : ActiveEditor
    data class TaskEditor(val taskId: String?, val initialEventId: String? = null) : ActiveEditor
    data class AppointmentEditor(val appointmentId: String?) : ActiveEditor
    data class ProjectEditor(val projectId: String?) : ActiveEditor
    data class EventEditor(val eventId: String?) : ActiveEditor
    data class TransactionEditor(
        val transactionId: String?,
        val initialType: TransactionType = TransactionType.EXPENSE,
        val initialEventId: String? = null,
        val initialTaskId: String? = null,
        val initialDescription: String? = null,
        val initialCategoryId: String? = null,
        val requireExplicitPaymentMethod: Boolean = false
    ) : ActiveEditor
    data class CreditCardEditor(val card: CreditCardEntity?) : ActiveEditor
    data object InstallmentEditor : ActiveEditor
    data object RecurringEditor : ActiveEditor
    data class LoanEditor(val loan: LoanEntity?) : ActiveEditor
    data class LoanPayment(val loan: LoanEntity) : ActiveEditor
    data object DeclaredBalance : ActiveEditor
    data object BudgetEditor : ActiveEditor
    data class FinancialGoalEditor(val goal: FinancialGoalEntity?) : ActiveEditor
}

class MainActivity : ComponentActivity() {
    private val incomingIntentFlow = MutableStateFlow<Intent?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        incomingIntentFlow.value = intent

        val appContainer = (application as NexusApplication).container
        setContent {
            val viewModel: NexusViewModel = viewModel(
                factory = NexusViewModel.Factory(
                    repository = appContainer.repository,
                    marketQuoteRepository = appContainer.marketQuoteRepository,
                    marketHistoryRepository = appContainer.marketHistoryRepository,
                    marketWatchlistRepository = appContainer.marketWatchlistRepository,
                    marketAlertRepository = appContainer.marketAlertRepository
                )
            )
            val incomingIntent by incomingIntentFlow.collectAsStateWithLifecycle()
            NexusAppRoot(
                viewModel = viewModel,
                repository = appContainer.repository,
                incomingIntent = incomingIntent,
                onIntentConsumed = { incomingIntentFlow.value = null }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        incomingIntentFlow.value = intent
    }
}

@Composable
private fun NexusAppRoot(
    viewModel: NexusViewModel,
    repository: com.nexus.app.data.repository.NexusRepository,
    incomingIntent: Intent?,
    onIntentConsumed: () -> Unit
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val dashboard by viewModel.dashboard.collectAsStateWithLifecycle()
    val allTasks by viewModel.allTasks.collectAsStateWithLifecycle()
    val filteredTasks by viewModel.filteredTasks.collectAsStateWithLifecycle()
    val tasksFilter by viewModel.tasksFilter.collectAsStateWithLifecycle()
    val allProjects by viewModel.allProjects.collectAsStateWithLifecycle()
    val allEvents by viewModel.allEvents.collectAsStateWithLifecycle()
    val agendaOccurrences by viewModel.agendaOccurrences.collectAsStateWithLifecycle()
    val agendaFilter by viewModel.agendaFilter.collectAsStateWithLifecycle()
    val financeSummary by viewModel.financeSummary.collectAsStateWithLifecycle()
    val financeDashboardData by viewModel.financeDashboardData.collectAsStateWithLifecycle()
    val usdBrlQuoteState by viewModel.usdBrlQuoteState.collectAsStateWithLifecycle()
    val marketQuoteStates by viewModel.marketQuoteStates.collectAsStateWithLifecycle()
    val marketHistoryStates by viewModel.marketHistoryStates.collectAsStateWithLifecycle()
    val marketFavoriteCodes by viewModel.marketFavoriteCodes.collectAsStateWithLifecycle()
    val marketAlerts by viewModel.marketAlerts.collectAsStateWithLifecycle()
    val filteredTransactions by viewModel.filteredTransactions.collectAsStateWithLifecycle()
    val allTransactions by viewModel.allTransactions.collectAsStateWithLifecycle()
    val financeFilter by viewModel.financeFilter.collectAsStateWithLifecycle()
    val cardsWithStatements by viewModel.cardsWithStatements.collectAsStateWithLifecycle()
    val installmentPurchases by viewModel.installmentPurchases.collectAsStateWithLifecycle()
    val recurringRules by viewModel.recurringRules.collectAsStateWithLifecycle()
    val budgets by viewModel.budgets.collectAsStateWithLifecycle()
    val financialGoals by viewModel.financialGoals.collectAsStateWithLifecycle()
    val loans by viewModel.loans.collectAsStateWithLifecycle()
    val auditHistory by viewModel.auditHistory.collectAsStateWithLifecycle()
    val globalSearchQuery by viewModel.globalSearchQuery.collectAsStateWithLifecycle()
    val globalSearchResults by viewModel.globalSearchResults.collectAsStateWithLifecycle()
    val tableCounts by viewModel.tableCounts.collectAsStateWithLifecycle()
    val errorLogs by viewModel.errorLogs.collectAsStateWithLifecycle()
    val backupState by viewModel.backupState.collectAsStateWithLifecycle()
    val routineSettings by viewModel.routineSettings.collectAsStateWithLifecycle()
    val reminders by viewModel.reminders.collectAsStateWithLifecycle()
    val briefingHistory by viewModel.briefingHistory.collectAsStateWithLifecycle()
    val dailyBriefing by viewModel.dailyBriefing.collectAsStateWithLifecycle()
    val nightReview by viewModel.nightReview.collectAsStateWithLifecycle()
    val personalizationSettings by viewModel.personalizationSettings.collectAsStateWithLifecycle()
    val quotes by viewModel.quotes.collectAsStateWithLifecycle()
    val allPersonalGoals by viewModel.allPersonalGoals.collectAsStateWithLifecycle()
    val filteredPersonalGoals by viewModel.filteredPersonalGoals.collectAsStateWithLifecycle()
    val goalsFilter by viewModel.goalsFilter.collectAsStateWithLifecycle()

    NexusTheme(darkTheme = settings.darkTheme) {
        val palette = LocalNexusPalette.current
        val snackbarHostState = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()

        var currentTab by remember { mutableStateOf(MainTab.HOME) }
        var subRoute by remember { mutableStateOf<SubRoute>(SubRoute.None) }
        val subRouteStack = remember { mutableStateListOf<SubRoute>() }
        var activeEditor by remember { mutableStateOf<ActiveEditor>(ActiveEditor.None) }
        var loadedAppointment by remember { mutableStateOf<AppointmentEntity?>(null) }
        var activeEditorDirty by remember { mutableStateOf(false) }
        var showEditorDiscardDialog by remember { mutableStateOf(false) }

        val navigateToSubRoute: (SubRoute) -> Unit = { target ->
            if (target == SubRoute.None) {
                subRouteStack.clear()
                subRoute = SubRoute.None
            } else if (target != subRoute) {
                if (subRoute != SubRoute.None) {
                    subRouteStack.add(subRoute)
                }
                subRoute = target
            }
        }
        val popSubRoute: () -> Unit = {
            subRoute = if (subRouteStack.isNotEmpty()) {
                subRouteStack.removeAt(subRouteStack.lastIndex)
            } else {
                SubRoute.None
            }
        }

        LaunchedEffect(activeEditor) {
            activeEditorDirty = false
            showEditorDiscardDialog = false
        }

        BackHandler(
            enabled = activeEditor != ActiveEditor.None ||
                subRoute != SubRoute.None ||
                currentTab != MainTab.HOME
        ) {
            val resolvedSubRouteKind = when {
                subRoute == SubRoute.None -> BackSubRouteKind.NONE
                subRouteStack.lastOrNull() == SubRoute.MoreMenu -> BackSubRouteKind.MORE_CHILD_SUBROUTE
                else -> subRoute.toBackSubRouteKind()
            }
            val action = resolveBackAction(
                BackNavigationState(
                    hasOpenEditor = activeEditor != ActiveEditor.None,
                    editorHasUnsavedChanges = activeEditorDirty,
                    subRouteKind = resolvedSubRouteKind,
                    currentTab = if (currentTab == MainTab.HOME) BackTabKind.HOME else BackTabKind.OTHER
                )
            )
            when (action) {
                BackAction.CLOSE_EDITOR -> activeEditor = ActiveEditor.None
                BackAction.CONFIRM_DISCARD_EDITOR -> showEditorDiscardDialog = true
                BackAction.RETURN_TO_MORE_MENU,
                BackAction.RETURN_FROM_SUBROUTE -> popSubRoute()
                BackAction.RETURN_TO_HOME_TAB -> currentTab = MainTab.HOME
                BackAction.ALLOW_SYSTEM_EXIT -> Unit
            }
        }

        LaunchedEffect(Unit) {
            viewModel.snackbarEvents.collect { msg ->
                snackbarHostState.showSnackbar(msg)
            }
        }

        val openAppointmentEditor: (String?) -> Unit = { apptId ->
            if (apptId == null) {
                loadedAppointment = null
                activeEditor = ActiveEditor.AppointmentEditor(null)
            } else {
                scope.launch {
                    loadedAppointment = repository.getAppointmentById(apptId)
                    activeEditor = ActiveEditor.AppointmentEditor(apptId)
                }
            }
        }

        val openFinanceCards: () -> Unit = {
            navigateToSubRoute(SubRoute.None)
            currentTab = MainTab.FINANCE
            viewModel.setFinanceTabMode(FinanceTabMode.CARDS)
        }

        LaunchedEffect(incomingIntent) {
            val extrasIntent = incomingIntent ?: return@LaunchedEffect
            val openRoutine = extrasIntent.getBooleanExtra(NexusNotificationScheduler.EXTRA_OPEN_ROUTINE, false)
            val marketAssetCode = extrasIntent.getStringExtra(NexusNotificationScheduler.EXTRA_MARKET_ASSET_CODE)
            val sourceTypeRaw = extrasIntent.getStringExtra(NexusNotificationScheduler.EXTRA_SOURCE_TYPE)
            val sourceId = extrasIntent.getStringExtra(NexusNotificationScheduler.EXTRA_SOURCE_ID)
            val sourceType = sourceTypeRaw?.let { runCatching { ReminderSourceType.valueOf(it) }.getOrNull() }

            val widgetUri = extrasIntent.data
            val widgetTargetFromUri = widgetUri?.takeIf {
                it.scheme == NexusWidgetNavigation.SCHEME && it.host == NexusWidgetNavigation.HOST
            }?.pathSegments?.firstOrNull()
            val widgetRecordFromUri = widgetUri?.takeIf {
                it.scheme == NexusWidgetNavigation.SCHEME && it.host == NexusWidgetNavigation.HOST
            }?.pathSegments?.getOrNull(1)
            val widgetDateFromUri = widgetUri?.takeIf {
                it.scheme == NexusWidgetNavigation.SCHEME && it.host == NexusWidgetNavigation.HOST
            }?.getQueryParameter("date")

            val widgetTarget = extrasIntent.getStringExtra(NexusWidgetNavigation.EXTRA_WIDGET_TARGET) ?: widgetTargetFromUri
            val widgetRecordId = extrasIntent.getStringExtra(NexusWidgetNavigation.EXTRA_WIDGET_RECORD_ID) ?: widgetRecordFromUri
            val widgetDateIso = extrasIntent.getStringExtra(NexusWidgetNavigation.EXTRA_WIDGET_DATE_ISO) ?: widgetDateFromUri

            if (!marketAssetCode.isNullOrBlank() && MarketAssetCatalog.findByCode(marketAssetCode) != null) {
                navigateToSubRoute(SubRoute.None)
                navigateToSubRoute(SubRoute.Market)
                navigateToSubRoute(SubRoute.MarketAssetDetail(marketAssetCode))
                onIntentConsumed()
            } else if (openRoutine || sourceType == ReminderSourceType.ROUTINE_MORNING || sourceType == ReminderSourceType.ROUTINE_NIGHT) {
                val tabIdx = if (sourceType == ReminderSourceType.ROUTINE_NIGHT) 1 else 0
                navigateToSubRoute(SubRoute.None)
                navigateToSubRoute(SubRoute.RoutineAssistant(tabIdx))
                onIntentConsumed()
            } else if (sourceType != null && !sourceId.isNullOrBlank()) {
                navigateToSubRoute(SubRoute.None)
                when (sourceType) {
                    ReminderSourceType.TASK -> {
                        currentTab = MainTab.TASKS
                        activeEditor = ActiveEditor.TaskEditor(sourceId)
                    }
                    ReminderSourceType.APPOINTMENT -> {
                        currentTab = MainTab.AGENDA
                        openAppointmentEditor(sourceId)
                    }
                    ReminderSourceType.EVENT -> {
                        currentTab = MainTab.EVENTS
                        activeEditor = ActiveEditor.EventEditor(sourceId)
                    }
                    ReminderSourceType.TRANSACTION -> {
                        currentTab = MainTab.FINANCE
                        activeEditor = ActiveEditor.TransactionEditor(sourceId)
                    }
                    ReminderSourceType.STATEMENT -> openFinanceCards()
                    ReminderSourceType.ROUTINE_MORNING -> navigateToSubRoute(SubRoute.RoutineAssistant(0))
                    ReminderSourceType.ROUTINE_NIGHT -> navigateToSubRoute(SubRoute.RoutineAssistant(1))
                }
                onIntentConsumed()
            } else if (!widgetTarget.isNullOrBlank()) {
                navigateToSubRoute(SubRoute.None)
                when (widgetTarget) {
                    NexusWidgetNavigation.TARGET_HOME -> {
                        currentTab = MainTab.HOME
                        activeEditor = ActiveEditor.None
                    }
                    NexusWidgetNavigation.TARGET_AGENDA -> {
                        widgetDateIso?.let { iso ->
                            runCatching { LocalDate.parse(iso) }.getOrNull()?.let(viewModel::setAgendaSelectedDate)
                        }
                        currentTab = MainTab.AGENDA
                        activeEditor = ActiveEditor.None
                    }
                    NexusWidgetNavigation.TARGET_TASKS -> {
                        currentTab = MainTab.TASKS
                        activeEditor = ActiveEditor.None
                    }
                    NexusWidgetNavigation.TARGET_FINANCE -> {
                        currentTab = MainTab.FINANCE
                        viewModel.setFinanceTabMode(FinanceTabMode.OVERVIEW)
                        activeEditor = ActiveEditor.None
                    }
                    NexusWidgetNavigation.TARGET_FINANCE_CARDS -> openFinanceCards()
                    NexusWidgetNavigation.TARGET_EVENTS -> {
                        currentTab = MainTab.EVENTS
                        activeEditor = ActiveEditor.None
                    }
                    NexusWidgetNavigation.TARGET_APPOINTMENT -> {
                        currentTab = MainTab.AGENDA
                        if (!widgetRecordId.isNullOrBlank()) {
                            openAppointmentEditor(widgetRecordId)
                        }
                    }
                    NexusWidgetNavigation.TARGET_TASK -> {
                        currentTab = MainTab.TASKS
                        if (!widgetRecordId.isNullOrBlank()) {
                            activeEditor = ActiveEditor.TaskEditor(widgetRecordId)
                        }
                    }
                    NexusWidgetNavigation.TARGET_TRANSACTION -> {
                        currentTab = MainTab.FINANCE
                        if (!widgetRecordId.isNullOrBlank()) {
                            activeEditor = ActiveEditor.TransactionEditor(widgetRecordId)
                        }
                    }
                    NexusWidgetNavigation.TARGET_EVENT -> {
                        currentTab = MainTab.EVENTS
                        if (!widgetRecordId.isNullOrBlank()) {
                            activeEditor = ActiveEditor.EventEditor(widgetRecordId)
                        }
                    }
                }
                onIntentConsumed()
            }
        }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = palette.background,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                if (subRoute == SubRoute.None) {
                    Surface(
                        color = palette.surface,
                        contentColor = palette.textPrimary
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().statusBarsPadding()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                NexusWordmark(
                                    height = 17.dp,
                                    color = palette.highlight,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { navigateToSubRoute(SubRoute.Market) }) {
                                        Icon(
                                            imageVector = Icons.Outlined.ShowChart,
                                            contentDescription = "Mercado",
                                            tint = palette.textPrimary
                                        )
                                    }
                                    IconButton(onClick = { navigateToSubRoute(SubRoute.GlobalSearch) }) {
                                        Icon(
                                            imageVector = Icons.Outlined.Search,
                                            contentDescription = "Pesquisa global",
                                            tint = palette.textPrimary
                                        )
                                    }
                                    IconButton(onClick = { navigateToSubRoute(SubRoute.MoreMenu) }) {
                                        Icon(
                                            imageVector = Icons.Outlined.MoreHoriz,
                                            contentDescription = "Mais opções",
                                            tint = palette.textPrimary
                                        )
                                    }
                                }
                            }
                            HorizontalDivider(color = palette.border)
                        }
                    }
                }
            },
            floatingActionButton = {
                if (subRoute == SubRoute.None) {
                    FloatingActionButton(
                        onClick = { activeEditor = ActiveEditor.QuickAddChooser },
                        containerColor = palette.highlight,
                        contentColor = palette.background,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = "Cadastro rápido")
                    }
                }
            },
            bottomBar = {
                if (subRoute == SubRoute.None) {
                    NavigationBar(
                        containerColor = palette.surface,
                        contentColor = palette.textPrimary
                    ) {
                        BOTTOM_NAV_TAB_ORDER.forEach { tab ->
                            val icon = when (tab) {
                                MainTab.HOME -> Icons.Outlined.Home
                                MainTab.AGENDA -> Icons.Outlined.CalendarMonth
                                MainTab.TASKS -> Icons.Outlined.TaskAlt
                                MainTab.FINANCE -> Icons.Outlined.AccountBalanceWallet
                                MainTab.EVENTS -> Icons.Outlined.ConfirmationNumber
                            }
                            NavigationBarItem(
                                selected = currentTab == tab,
                                onClick = { currentTab = tab },
                                icon = { Icon(icon, contentDescription = tab.label) },
                                label = { Text(tab.label, maxLines = 1) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = palette.highlight,
                                    selectedTextColor = palette.highlight,
                                    unselectedIconColor = palette.textSecondary,
                                    unselectedTextColor = palette.textSecondary,
                                    indicatorColor = palette.surfaceElevated
                                )
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            val contentModifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)

            when (val activeRoute = subRoute) {
                SubRoute.MoreMenu -> MoreScreen(
                    onOpenPlanning = { navigateToSubRoute(SubRoute.Planning(null)) },
                    onOpenMarket = { navigateToSubRoute(SubRoute.Market) },
                    onOpenCustomizeDashboard = { navigateToSubRoute(SubRoute.DashboardCustomization) },
                    onOpenQuoteLibrary = { navigateToSubRoute(SubRoute.QuoteLibrary) },
                    onOpenRoutineAssistant = { navigateToSubRoute(SubRoute.RoutineAssistant(0)) },
                    onOpenWidgets = { navigateToSubRoute(SubRoute.WidgetsHub) },
                    onOpenSearch = { navigateToSubRoute(SubRoute.GlobalSearch) },
                    onOpenSettings = { navigateToSubRoute(SubRoute.Settings) },
                    onOpenBackup = { navigateToSubRoute(SubRoute.Backup) },
                    onOpenDiagnostics = { navigateToSubRoute(SubRoute.Diagnostics) },
                    onBack = popSubRoute,
                    modifier = contentModifier
                )
                is SubRoute.Planning -> PlanningGoalsScreen(
                    goals = filteredPersonalGoals,
                    allGoals = allPersonalGoals,
                    categories = categories,
                    allTasks = allTasks,
                    allProjects = allProjects,
                    filter = goalsFilter,
                    initialSelectedGoalId = activeRoute.initialGoalId,
                    onBack = popSubRoute,
                    onStatusFilterChange = viewModel::setGoalsStatusFilter,
                    onCategoryFilterChange = viewModel::setGoalsCategoryFilter,
                    onSearchQueryChange = viewModel::setGoalsSearchQuery,
                    onSaveGoal = { id, title, desc, catId, targetAt, status, mode, manualPct, notes ->
                        viewModel.savePersonalGoal(id, title, desc, catId, targetAt, status, mode, manualPct, notes)
                    },
                    onUpdateGoalStatus = viewModel::updatePersonalGoalStatus,
                    onAddProgressLog = viewModel::addGoalProgressLog,
                    onDeleteGoal = { goalId -> viewModel.deletePersonalGoal(goalId) },
                    onSaveMilestone = { id, goalId, title, desc, dueAt ->
                        viewModel.saveMilestone(id, goalId, title, desc, dueAt)
                    },
                    onToggleMilestone = viewModel::toggleMilestoneCompleted,
                    onDeleteMilestone = viewModel::deleteMilestone,
                    onLinkTaskToGoal = viewModel::linkTaskToGoal,
                    onLinkProjectToGoal = viewModel::linkProjectToGoal,
                    onOpenTask = { activeEditor = ActiveEditor.TaskEditor(it) },
                    onToggleTaskComplete = viewModel::toggleTaskCompletion,
                    onOpenProject = { activeEditor = ActiveEditor.ProjectEditor(it) },
                    modifier = contentModifier
                )
                SubRoute.DashboardCustomization -> DashboardCustomizationScreen(
                    personalization = personalizationSettings,
                    blocks = dashboard.blocks,
                    onBack = popSubRoute,
                    onToggleBlock = viewModel::toggleDashboardBlock,
                    onMoveBlock = viewModel::moveDashboardBlock,
                    onUpdateBlockConfig = viewModel::updateDashboardBlockConfig,
                    onToggleCompactMode = { compact ->
                        viewModel.saveGreetingAndPresentation(
                            personalizationSettings.greetingEnabled,
                            personalizationSettings.greetingTemplate,
                            personalizationSettings.dateFormatPattern,
                            personalizationSettings.showDailyQuoteOnDashboard,
                            compact,
                            personalizationSettings.showMetricSummaryOnDashboard
                        )
                    },
                    onToggleMetricSummary = { showSummary ->
                        viewModel.saveGreetingAndPresentation(
                            personalizationSettings.greetingEnabled,
                            personalizationSettings.greetingTemplate,
                            personalizationSettings.dateFormatPattern,
                            personalizationSettings.showDailyQuoteOnDashboard,
                            personalizationSettings.compactDashboardMode,
                            showSummary
                        )
                    },
                    onOpenQuoteLibrary = { navigateToSubRoute(SubRoute.QuoteLibrary) },
                    onResetToDefault = viewModel::resetDashboardToDefault,
                    modifier = contentModifier
                )
                SubRoute.QuoteLibrary -> QuoteLibraryScreen(
                    quotes = quotes,
                    dailyQuoteSelection = dashboard.dailyQuoteSelection,
                    showDailyQuoteOnDashboard = personalizationSettings.showDailyQuoteOnDashboard,
                    onBack = popSubRoute,
                    onToggleShowOnDashboard = { showQuote ->
                        viewModel.saveGreetingAndPresentation(
                            personalizationSettings.greetingEnabled,
                            personalizationSettings.greetingTemplate,
                            personalizationSettings.dateFormatPattern,
                            showQuote,
                            personalizationSettings.compactDashboardMode,
                            personalizationSettings.showMetricSummaryOnDashboard
                        )
                    },
                    onSaveQuote = { id, text, author, active ->
                        viewModel.saveQuote(id, text, author, active)
                    },
                    onToggleQuoteActive = viewModel::toggleQuoteActive,
                    onDeleteQuote = viewModel::deleteQuote,
                    onImportQuotes = viewModel::importQuotesFromText,
                    onExportQuotesJson = viewModel::exportQuotesJson,
                    onQuotesExported = viewModel::notifyQuotesExported,
                    onLogError = viewModel::logUiError,
                    modifier = contentModifier
                )
                SubRoute.WidgetsHub -> WidgetsHubScreen(
                    onBack = popSubRoute,
                    onNotifyMessage = viewModel::emitMessage,
                    modifier = contentModifier
                )
                is SubRoute.RoutineAssistant -> RoutineAssistantScreen(
                    dailyBriefing = dailyBriefing,
                    nightReview = nightReview,
                    routineSettings = routineSettings,
                    reminders = reminders,
                    briefingHistory = briefingHistory,
                    initialTabIndex = activeRoute.initialTabIndex,
                    onBack = popSubRoute,
                    onMarkBriefingReviewed = viewModel::markBriefingReviewed,
                    onOpenTask = { activeEditor = ActiveEditor.TaskEditor(it) },
                    onToggleTaskComplete = viewModel::toggleTaskCompletion,
                    onPostponeTaskToTomorrow = viewModel::postponeTaskToTomorrow,
                    onOpenAppointment = openAppointmentEditor,
                    onOpenEvent = { activeEditor = ActiveEditor.EventEditor(it) },
                    onOpenTransaction = { activeEditor = ActiveEditor.TransactionEditor(it) },
                    onSettleTransaction = viewModel::settleTransaction,
                    onPostponeTransactionToTomorrow = viewModel::postponeTransactionToTomorrow,
                    onOpenFinanceCards = openFinanceCards,
                    onSaveRoutineSettings = viewModel::saveRoutineSettings,
                    onSnoozeReminder = { remId -> viewModel.snoozeReminder(remId, 15) },
                    onCancelReminder = viewModel::cancelReminder,
                    onSendTestNotification = viewModel::sendTestNotification,
                    onRescheduleAllReminders = viewModel::rescheduleAllReminders,
                    modifier = contentModifier
                )
                SubRoute.GlobalSearch -> GlobalSearchScreen(
                    query = globalSearchQuery,
                    results = globalSearchResults,
                    onQueryChange = viewModel::setGlobalSearchQuery,
                    onBack = popSubRoute,
                    onOpenTask = { activeEditor = ActiveEditor.TaskEditor(it) },
                    onToggleTaskComplete = viewModel::toggleTaskCompletion,
                    onOpenProject = { activeEditor = ActiveEditor.ProjectEditor(it) },
                    onOpenGoal = { goalId -> navigateToSubRoute(SubRoute.Planning(goalId)) },
                    onOpenAppointment = openAppointmentEditor,
                    onOpenEvent = { activeEditor = ActiveEditor.EventEditor(it) },
                    onOpenTransaction = { activeEditor = ActiveEditor.TransactionEditor(it) },
                    onToggleSettleTransaction = viewModel::settleTransaction,
                    onOpenFinanceCards = openFinanceCards,
                    modifier = contentModifier
                )
                SubRoute.Settings -> SettingsScreen(
                    settings = settings,
                    personalization = personalizationSettings,
                    routineSettings = routineSettings,
                    categories = categories,
                    onBack = popSubRoute,
                    onSaveSettings = viewModel::saveSettings,
                    onSaveGreetingAndPresentation = viewModel::saveGreetingAndPresentation,
                    onOpenCustomizeDashboard = { navigateToSubRoute(SubRoute.DashboardCustomization) },
                    onOpenQuoteLibrary = { navigateToSubRoute(SubRoute.QuoteLibrary) },
                    onResetDashboardToDefault = viewModel::resetDashboardToDefault,
                    onEditDeclaredBalance = { activeEditor = ActiveEditor.DeclaredBalance },
                    onOpenRoutineAssistant = { tabIdx -> navigateToSubRoute(SubRoute.RoutineAssistant(tabIdx)) },
                    onOpenWidgets = { navigateToSubRoute(SubRoute.WidgetsHub) },
                    onOpenBackup = { navigateToSubRoute(SubRoute.Backup) },
                    onOpenDiagnostics = { navigateToSubRoute(SubRoute.Diagnostics) },
                    onSaveCategory = { id, name, hex -> viewModel.saveCategory(id, name, hex) },
                    onDeleteCategory = viewModel::deleteCategory,
                    modifier = contentModifier
                )
                SubRoute.Backup -> BackupScreen(
                    backupState = backupState,
                    onBack = popSubRoute,
                    onGenerateBackupJson = viewModel::generateBackupJson,
                    onNotifyExported = viewModel::notifyBackupExported,
                    onSelectImportMode = viewModel::setBackupImportMode,
                    onInspectBackupJson = { rawJson -> viewModel.inspectBackupJson(rawJson) },
                    onConfirmRestore = viewModel::confirmRestoreBackup,
                    onConfirmMerge = viewModel::confirmMergeBackup,
                    onDismissDialogs = viewModel::dismissBackupDialogs,
                    onLogError = viewModel::logUiError,
                    modifier = contentModifier
                )
                SubRoute.Diagnostics -> DiagnosticsScreen(
                    tableCounts = tableCounts,
                    errorLogs = errorLogs,
                    onBack = popSubRoute,
                    onClearErrorLogs = viewModel::clearErrorLogs,
                    onCopied = { viewModel.emitMessage("Relatório copiado para a área de transferência") },
                    modifier = contentModifier
                )
                SubRoute.Market -> MarketScreen(
                    quoteStates = marketQuoteStates,
                    favoriteCodes = marketFavoriteCodes,
                    alerts = marketAlerts,
                    onRefreshAsset = { code -> viewModel.refreshMarketAsset(code, manual = true) },
                    onRefreshAll = { viewModel.refreshAllMarketQuotes(manual = true) },
                    onAutoRefreshIfStale = { viewModel.refreshAllMarketQuotes(manual = false) },
                    onToggleFavorite = viewModel::toggleMarketFavorite,
                    onCreateAlert = viewModel::createMarketAlert,
                    onUpdateAlert = viewModel::updateMarketAlert,
                    onToggleAlertActive = viewModel::toggleMarketAlertActive,
                    onDeleteAlert = viewModel::deleteMarketAlert,
                    onOpenAssetDetail = { code -> navigateToSubRoute(SubRoute.MarketAssetDetail(code)) },
                    onBack = popSubRoute,
                    modifier = contentModifier
                )
                is SubRoute.MarketAssetDetail -> MarketAssetDetailScreen(
                    assetCode = activeRoute.assetCode,
                    quoteStates = marketQuoteStates,
                    historyStates = marketHistoryStates[activeRoute.assetCode].orEmpty(),
                    isFavorite = activeRoute.assetCode in marketFavoriteCodes,
                    alerts = marketAlerts,
                    onRefreshAsset = { code -> viewModel.refreshMarketAsset(code, manual = true) },
                    onLoadHistory = { code, range, manual ->
                        viewModel.loadMarketHistory(assetCode = code, range = range, manual = manual)
                    },
                    onToggleFavorite = viewModel::toggleMarketFavorite,
                    onCreateAlert = viewModel::createMarketAlert,
                    onUpdateAlert = viewModel::updateMarketAlert,
                    onToggleAlertActive = viewModel::toggleMarketAlertActive,
                    onDeleteAlert = viewModel::deleteMarketAlert,
                    onBack = popSubRoute,
                    modifier = contentModifier
                )
                SubRoute.None -> {
                    when (currentTab) {
                        MainTab.HOME -> HomeScreen(
                            snapshot = dashboard,
                            dailyBriefing = dailyBriefing,
                            onOpenRoutineAssistant = { navigateToSubRoute(SubRoute.RoutineAssistant(0)) },
                            onOpenCustomizeDashboard = { navigateToSubRoute(SubRoute.DashboardCustomization) },
                            onOpenQuoteLibrary = { navigateToSubRoute(SubRoute.QuoteLibrary) },
                            onOpenPlanning = { goalId -> navigateToSubRoute(SubRoute.Planning(goalId)) },
                            onOpenTasks = {
                                currentTab = MainTab.TASKS
                            },
                            onOpenTasksOverdue = {
                                viewModel.setTasksTabMode(TasksTabMode.OVERDUE)
                                currentTab = MainTab.TASKS
                            },
                            onOpenTask = { activeEditor = ActiveEditor.TaskEditor(it) },
                            onToggleTaskComplete = viewModel::toggleTaskCompletion,
                            onOpenAgenda = {
                                currentTab = MainTab.AGENDA
                            },
                            onOpenAppointment = openAppointmentEditor,
                            onOpenEvent = { activeEditor = ActiveEditor.EventEditor(it) },
                            onOpenFinance = { currentTab = MainTab.FINANCE },
                            onOpenFinanceCards = openFinanceCards,
                            onOpenTransaction = { activeEditor = ActiveEditor.TransactionEditor(it) },
                            onQuickAdd = { activeEditor = ActiveEditor.QuickAddChooser },
                            modifier = contentModifier
                        )
                        MainTab.AGENDA -> AgendaScreen(
                            occurrences = agendaOccurrences,
                            categories = categories,
                            filter = agendaFilter,
                            onViewModeChange = viewModel::setAgendaViewMode,
                            onDateChange = viewModel::setAgendaSelectedDate,
                            onKindFilterChange = viewModel::setAgendaKindFilter,
                            onCategoryFilterChange = viewModel::setAgendaCategoryFilter,
                            onShowPaidChange = viewModel::setAgendaShowPaid,
                            onClearFilters = viewModel::clearAgendaFilters,
                            onOpenAppointment = openAppointmentEditor,
                            onOpenTask = { activeEditor = ActiveEditor.TaskEditor(it) },
                            onOpenEvent = { activeEditor = ActiveEditor.EventEditor(it) },
                            onOpenTransaction = { activeEditor = ActiveEditor.TransactionEditor(it) },
                            onOpenCards = openFinanceCards,
                            onOpenProject = { activeEditor = ActiveEditor.ProjectEditor(it) },
                            onCreateAppointment = { openAppointmentEditor(null) },
                            onOpenVirtualRecurringTask = { recId, dateIso ->
                                viewModel.openOrMaterializeRecurringOccurrence(recId, dateIso) { materializedTaskId ->
                                    activeEditor = ActiveEditor.TaskEditor(materializedTaskId)
                                }
                            },
                            modifier = contentModifier
                        )
                        MainTab.TASKS -> TasksAndProjectsScreen(
                            tasks = filteredTasks,
                            allTasks = allTasks,
                            projects = allProjects,
                            filter = tasksFilter,
                            onTabChange = viewModel::setTasksTabMode,
                            onStatusFilterChange = viewModel::setTasksStatusFilter,
                            onPriorityFilterChange = viewModel::setTasksPriorityFilter,
                            onProjectFilterChange = viewModel::setTasksProjectFilter,
                            onClearFilters = viewModel::clearTasksFilters,
                            onOpenTask = { activeEditor = ActiveEditor.TaskEditor(it) },
                            onToggleTaskComplete = viewModel::toggleTaskCompletion,
                            onUpdateTaskStatus = viewModel::updateTaskStatus,
                            onOpenProject = { activeEditor = ActiveEditor.ProjectEditor(it) },
                            onCreateTask = { activeEditor = ActiveEditor.TaskEditor(null) },
                            onCreateProject = { activeEditor = ActiveEditor.ProjectEditor(null) },
                            modifier = contentModifier
                        )
                        MainTab.FINANCE -> FinanceScreen(
                            dashboardData = financeDashboardData,
                            usdBrlQuoteState = usdBrlQuoteState,
                            marketQuoteStates = marketQuoteStates,
                            onRefreshUsdBrl = { viewModel.refreshUsdBrlQuote(manual = true) },
                            onRefreshMarketAsset = { code -> viewModel.refreshMarketAsset(code, manual = true) },
                            onRefreshAllMarketQuotes = { viewModel.refreshAllMarketQuotes(manual = true) },
                            onAutoRefreshMarketQuotes = { viewModel.refreshAllMarketQuotes(manual = false) },
                            onOpenMarket = { navigateToSubRoute(SubRoute.Market) },
                            onOpenMarketAsset = { code -> navigateToSubRoute(SubRoute.MarketAssetDetail(code)) },
                            allTransactions = allTransactions,
                            transactions = filteredTransactions,
                            categories = categories,
                            filter = financeFilter,
                            cardsWithStatements = cardsWithStatements,
                            installmentPurchases = installmentPurchases,
                            recurringRules = recurringRules,
                            financialGoals = financialGoals,
                            loans = loans,
                            auditHistory = auditHistory,
                            onTabChange = viewModel::setFinanceTabMode,
                            onMonthChange = viewModel::setFinanceSelectedMonth,
                            onTypeFilterChange = viewModel::setFinanceTypeFilter,
                            onStatusFilterChange = viewModel::setFinanceStatusFilter,
                            onCategoryFilterChange = viewModel::setFinanceCategoryFilter,
                            onPaymentMethodFilterChange = viewModel::setFinancePaymentMethodFilter,
                            onClearFilters = viewModel::clearFinanceFilters,
                            onEditDeclaredBalance = { activeEditor = ActiveEditor.DeclaredBalance },
                            onCreateTransaction = { t ->
                                activeEditor = ActiveEditor.TransactionEditor(null, initialType = t)
                            },
                            onOpenTransaction = { txId ->
                                activeEditor = ActiveEditor.TransactionEditor(txId)
                            },
                            onToggleSettleTransaction = viewModel::settleTransaction,
                            onCreateCard = { activeEditor = ActiveEditor.CreditCardEditor(null) },
                            onEditCard = { card -> activeEditor = ActiveEditor.CreditCardEditor(card) },
                            onCloseStatement = viewModel::closeCreditCardStatement,
                            onPayStatement = viewModel::payCreditCardStatement,
                            onCreateInstallment = { activeEditor = ActiveEditor.InstallmentEditor },
                            onSettleRemainingInstallments = viewModel::settleAllRemainingInstallments,
                            onDeleteInstallmentPurchase = viewModel::deleteInstallmentPurchase,
                            onCreateRecurring = { activeEditor = ActiveEditor.RecurringEditor },
                            onToggleRecurringActive = viewModel::toggleRecurringRuleActive,
                            onDeleteRecurringRule = viewModel::deleteRecurringRule,
                            onCreateBudget = { activeEditor = ActiveEditor.BudgetEditor },
                            onDeleteBudget = viewModel::deleteBudget,
                            onCreateGoal = { activeEditor = ActiveEditor.FinancialGoalEditor(null) },
                            onEditGoal = { goal -> activeEditor = ActiveEditor.FinancialGoalEditor(goal) },
                            onUpdateGoalAmount = viewModel::updateFinancialGoalAmount,
                            onDeleteGoal = viewModel::deleteFinancialGoal,
                            onCreateLoan = { activeEditor = ActiveEditor.LoanEditor(null) },
                            onEditLoan = { loan -> activeEditor = ActiveEditor.LoanEditor(loan) },
                            onPayLoanInstallment = { loan -> activeEditor = ActiveEditor.LoanPayment(loan) },
                            onDeleteLoan = viewModel::deleteLoan,
                            onGenerateCsv = viewModel::generateFinancialCsv,
                            onCsvExported = viewModel::notifyCsvExported,
                            onLogError = viewModel::logUiError,
                            modifier = contentModifier
                        )
                        MainTab.EVENTS -> EventsScreen(
                            events = allEvents,
                            onOpenEvent = { activeEditor = ActiveEditor.EventEditor(it) },
                            onCreateEvent = { activeEditor = ActiveEditor.EventEditor(null) },
                            modifier = contentModifier
                        )
                    }
                }
            }
        }

        when (val editor = activeEditor) {
            ActiveEditor.None -> Unit
            ActiveEditor.QuickAddChooser -> QuickAddChooserSheet(
                onDismiss = { activeEditor = ActiveEditor.None },
                onSelectNewTask = { activeEditor = ActiveEditor.TaskEditor(null) },
                onSelectNewAppointment = { openAppointmentEditor(null) },
                onSelectNewProject = { activeEditor = ActiveEditor.ProjectEditor(null) },
                onSelectNewEvent = { activeEditor = ActiveEditor.EventEditor(null) },
                onSelectNewTransaction = {
                    activeEditor = ActiveEditor.TransactionEditor(null, initialType = TransactionType.EXPENSE)
                }
            )
            is ActiveEditor.TaskEditor -> {
                val existing = editor.taskId?.let { id ->
                    allTasks.firstOrNull { it.task.id == id }
                }
                val existingReminder = editor.taskId?.let { id ->
                    reminders.firstOrNull {
                        it.sourceType == ReminderSourceType.TASK &&
                            it.sourceId == id &&
                            it.category == ReminderCategory.SCHEDULE
                    }
                }
                TaskEditorSheet(
                    existingTask = existing,
                    initialEventId = editor.initialEventId,
                    existingReminder = existingReminder,
                    defaultReminderMinutes = routineSettings.defaultTaskReminderMinutes,
                    projects = allProjects,
                    categories = categories,
                    events = allEvents,
                    goals = allPersonalGoals,
                    onDismiss = { activeEditor = ActiveEditor.None },
                    onSave = { id, title, desc, projId, catId, evId, goalId, prio, st, dueAt, hasTime, notes, subs, reminderMins, recDraft, editScope ->
                        viewModel.saveTask(
                            id = id,
                            title = title,
                            description = desc,
                            projectId = projId,
                            categoryId = catId,
                            eventId = evId,
                            goalId = goalId,
                            priority = prio,
                            status = st,
                            dueAt = dueAt,
                            hasDueTime = hasTime,
                            notes = notes,
                            subtaskDrafts = subs,
                            reminderMinutesBefore = reminderMins,
                            recurrenceDraft = recDraft,
                            recurringEditScope = editScope,
                            onSaved = { activeEditor = ActiveEditor.None }
                        )
                    },
                    onToggleComplete = viewModel::toggleTaskCompletion,
                    onToggleSubtask = viewModel::toggleSubtask,
                    onOpenEvent = { evId -> activeEditor = ActiveEditor.EventEditor(evId) },
                    onRegisterExpense = { id, title, desc, projId, catId, evId, goalId, prio, st, dueAt, hasTime, notes, subs, reminderMins, recDraft, editScope ->
                        viewModel.saveTask(
                            id = id,
                            title = title,
                            description = desc,
                            projectId = projId,
                            categoryId = catId,
                            eventId = evId,
                            goalId = goalId,
                            priority = prio,
                            status = st,
                            dueAt = dueAt,
                            hasDueTime = hasTime,
                            notes = notes,
                            subtaskDrafts = subs,
                            reminderMinutesBefore = reminderMins,
                            recurrenceDraft = recDraft,
                            recurringEditScope = editScope,
                            onSaved = { savedTaskId ->
                                activeEditor = ActiveEditor.TransactionEditor(
                                    transactionId = null,
                                    initialType = TransactionType.EXPENSE,
                                    initialEventId = evId,
                                    initialTaskId = savedTaskId,
                                    initialDescription = title.trim(),
                                    initialCategoryId = viewModel.suggestExpenseCategoryIdForTask(catId),
                                    requireExplicitPaymentMethod = true
                                )
                            }
                        )
                    },
                    onOpenTransaction = { txId ->
                        activeEditor = ActiveEditor.TransactionEditor(txId)
                    },
                    onDelete = { taskId, deleteScope ->
                        viewModel.deleteTask(taskId, scope = deleteScope) { activeEditor = ActiveEditor.None }
                    },
                    onDirtyStateChange = { activeEditorDirty = it }
                )
            }
            is ActiveEditor.AppointmentEditor -> {
                val existingReminder = editor.appointmentId?.let { id ->
                    reminders.firstOrNull {
                        it.sourceType == ReminderSourceType.APPOINTMENT &&
                            it.sourceId == id &&
                            it.category == ReminderCategory.SCHEDULE
                    }
                }
                AppointmentEditorSheet(
                    existingAppointment = loadedAppointment,
                    categories = categories,
                    events = allEvents,
                    initialDate = agendaFilter.selectedDate,
                    existingReminder = existingReminder,
                    defaultReminderMinutes = routineSettings.defaultAppointmentReminderMinutes,
                    onDismiss = { activeEditor = ActiveEditor.None },
                    onSave = { id, title, desc, catId, evId, startAt, endAt, isAllDay, location, link, reminderMins ->
                        viewModel.saveAppointment(
                            id = id,
                            title = title,
                            description = desc,
                            categoryId = catId,
                            eventId = evId,
                            startAt = startAt,
                            endAt = endAt,
                            isAllDay = isAllDay,
                            location = location,
                            link = link,
                            reminderMinutesBefore = reminderMins,
                            onSaved = { activeEditor = ActiveEditor.None }
                        )
                    },
                    onDelete = { apptId ->
                        viewModel.deleteAppointment(apptId) { activeEditor = ActiveEditor.None }
                    },
                    onError = viewModel::logUiError,
                    onDirtyStateChange = { activeEditorDirty = it }
                )
            }
            is ActiveEditor.ProjectEditor -> {
                val existing = editor.projectId?.let { id ->
                    allProjects.firstOrNull { it.project.id == id }
                }
                ProjectEditorSheet(
                    existingProject = existing,
                    goals = allPersonalGoals,
                    countLinkedTasks = viewModel::countTasksInProject,
                    onDismiss = { activeEditor = ActiveEditor.None },
                    onSave = { id, name, desc, deadline, status, prio, goalId ->
                        viewModel.saveProject(
                            id = id,
                            name = name,
                            description = desc,
                            deadlineAt = deadline,
                            status = status,
                            priority = prio,
                            goalId = goalId,
                            onSaved = { activeEditor = ActiveEditor.None }
                        )
                    },
                    onDelete = { projectId ->
                        viewModel.deleteProject(projectId) { activeEditor = ActiveEditor.None }
                    },
                    onDirtyStateChange = { activeEditorDirty = it }
                )
            }
            is ActiveEditor.EventEditor -> {
                val existing = editor.eventId?.let { id ->
                    allEvents.firstOrNull { it.event.id == id }
                }
                val existingReminder = editor.eventId?.let { id ->
                    reminders.firstOrNull {
                        it.sourceType == ReminderSourceType.EVENT &&
                            it.sourceId == id &&
                            it.category == ReminderCategory.SCHEDULE
                    }
                }
                EventEditorSheet(
                    existingEvent = existing,
                    categories = categories,
                    creditCards = cardsWithStatements.map { it.card },
                    existingReminder = existingReminder,
                    defaultReminderMinutes = routineSettings.defaultEventReminderMinutes,
                    countLinkedAppointments = viewModel::countAppointmentsForEvent,
                    onDismiss = { activeEditor = ActiveEditor.None },
                    onSave = { id, name, desc, catId, startAt, endAt, allDay, officialUrl, ticketUrl, locName, addr, city, st, notes, evStatus, tStatus, tPrice, tQty, tFees, tPurchaseDate, tDueDate, payMethod, cardId, instCount, reminderMins ->
                        viewModel.saveEvent(
                            id = id,
                            name = name,
                            description = desc,
                            categoryId = catId,
                            startAt = startAt,
                            endAt = endAt,
                            isAllDay = allDay,
                            officialLink = officialUrl,
                            ticketLink = ticketUrl,
                            locationName = locName,
                            address = addr,
                            city = city,
                            state = st,
                            notes = notes,
                            eventStatus = evStatus,
                            ticketStatus = tStatus,
                            ticketPriceCents = tPrice,
                            ticketQuantity = tQty,
                            ticketFeesCents = tFees,
                            ticketPurchaseDate = tPurchaseDate,
                            ticketDueDate = tDueDate,
                            paymentMethod = payMethod,
                            creditCardId = cardId,
                            installmentsCount = instCount,
                            reminderMinutesBefore = reminderMins,
                            onSaved = { activeEditor = ActiveEditor.None }
                        )
                    },
                    onDelete = { eventId ->
                        viewModel.deleteEvent(eventId) { activeEditor = ActiveEditor.None }
                    },
                    onAddLinkedTask = { evId ->
                        activeEditor = ActiveEditor.TaskEditor(taskId = null, initialEventId = evId)
                    },
                    onOpenTask = { taskId ->
                        activeEditor = ActiveEditor.TaskEditor(taskId = taskId)
                    },
                    onAddAdditionalExpense = { evId ->
                        activeEditor = ActiveEditor.TransactionEditor(
                            transactionId = null,
                            initialType = TransactionType.EXPENSE,
                            initialEventId = evId
                        )
                    },
                    onOpenTransaction = { txId ->
                        activeEditor = ActiveEditor.TransactionEditor(transactionId = txId)
                    },
                    onError = viewModel::logUiError,
                    onDirtyStateChange = { activeEditorDirty = it }
                )
            }
            is ActiveEditor.TransactionEditor -> {
                val existing = editor.transactionId?.let { id ->
                    allTransactions.firstOrNull { it.transaction.id == id }
                }
                val existingReminder = editor.transactionId?.let { id ->
                    reminders.firstOrNull {
                        it.sourceType == ReminderSourceType.TRANSACTION &&
                            it.sourceId == id &&
                            it.category == ReminderCategory.FINANCIAL
                    }
                }
                TransactionEditorSheet(
                    existing = existing,
                    initialType = editor.initialType,
                    initialEventId = editor.initialEventId,
                    initialTaskId = editor.initialTaskId,
                    initialDescription = editor.initialDescription,
                    initialCategoryId = editor.initialCategoryId,
                    requireExplicitPaymentMethod = editor.requireExplicitPaymentMethod,
                    existingReminder = existingReminder,
                    defaultReminderMinutes = routineSettings.defaultFinanceReminderMinutes,
                    categories = categories,
                    projects = allProjects,
                    events = allEvents,
                    tasks = allTasks,
                    creditCards = cardsWithStatements.map { it.card },
                    onDismiss = { activeEditor = ActiveEditor.None },
                    onSave = { id, type, desc, amount, catId, st, payMethod, occurredAt, dueAt, settledAt, projId, evId, taskId, evCostType, cardId, notes, reminderMins ->
                        viewModel.saveTransaction(
                            id = id,
                            type = type,
                            description = desc,
                            amountCents = amount,
                            categoryId = catId,
                            status = st,
                            paymentMethod = payMethod,
                            occurredAt = occurredAt,
                            dueAt = dueAt,
                            settledAt = settledAt,
                            projectId = projId,
                            eventId = evId,
                            taskId = taskId,
                            eventCostType = evCostType,
                            creditCardId = cardId,
                            notes = notes,
                            reminderMinutesBefore = reminderMins,
                            onSaved = { activeEditor = ActiveEditor.None }
                        )
                    },
                    onDelete = { txId ->
                        viewModel.deleteTransaction(txId) { activeEditor = ActiveEditor.None }
                    },
                    onDirtyStateChange = { activeEditorDirty = it }
                )
            }
            is ActiveEditor.CreditCardEditor -> {
                CreditCardEditorSheet(
                    existing = editor.card,
                    onDismiss = { activeEditor = ActiveEditor.None },
                    onSave = { id, name, inst, totalLimit, availLimit, closing, due, notes ->
                        viewModel.saveCreditCard(
                            id = id,
                            name = name,
                            institution = inst,
                            totalLimitCents = totalLimit,
                            availableLimitCents = availLimit,
                            closingDay = closing,
                            dueDay = due,
                            notes = notes,
                            onSaved = { activeEditor = ActiveEditor.None }
                        )
                    },
                    onDelete = { cardId ->
                        viewModel.deleteCreditCard(cardId)
                        activeEditor = ActiveEditor.None
                    },
                    onDirtyStateChange = { activeEditorDirty = it }
                )
            }
            ActiveEditor.InstallmentEditor -> {
                InstallmentPurchaseEditorSheet(
                    creditCards = cardsWithStatements.map { it.card },
                    categories = categories,
                    onDismiss = { activeEditor = ActiveEditor.None },
                    onSave = { desc, totalCents, count, firstDue, cardId, catId, notes ->
                        viewModel.createInstallmentPurchase(
                            description = desc,
                            totalAmountCents = totalCents,
                            installmentsCount = count,
                            firstDueDate = firstDue,
                            creditCardId = cardId,
                            categoryId = catId,
                            notes = notes,
                            onSaved = { activeEditor = ActiveEditor.None }
                        )
                    },
                    onDirtyStateChange = { activeEditorDirty = it }
                )
            }
            ActiveEditor.RecurringEditor -> {
                RecurringRuleEditorSheet(
                    categories = categories,
                    creditCards = cardsWithStatements.map { it.card },
                    onDismiss = { activeEditor = ActiveEditor.None },
                    onSave = { title, type, amount, catId, payMethod, cardId, freq, customDays, startDate, maxOcc, notes ->
                        viewModel.saveRecurringRule(
                            title = title,
                            type = type,
                            amountCents = amount,
                            categoryId = catId,
                            paymentMethod = payMethod,
                            creditCardId = cardId,
                            frequency = freq,
                            customIntervalDays = customDays,
                            startDate = startDate,
                            maxOccurrences = maxOcc,
                            notes = notes,
                            onSaved = { activeEditor = ActiveEditor.None }
                        )
                    },
                    onDirtyStateChange = { activeEditorDirty = it }
                )
            }
            is ActiveEditor.LoanEditor -> {
                LoanEditorSheet(
                    existing = editor.loan,
                    onDismiss = { activeEditor = ActiveEditor.None },
                    onSave = { id, creditor, orig, contr, totalInst, instAmount, paidInst, nextDue, remBal, finalDate, notes ->
                        viewModel.saveLoan(
                            id = id,
                            creditor = creditor,
                            originalAmountCents = orig,
                            contractedAmountCents = contr,
                            installmentsTotal = totalInst,
                            installmentAmountCents = instAmount,
                            paidInstallments = paidInst,
                            nextDueAt = nextDue,
                            remainingBalanceCents = remBal,
                            finalDateAt = finalDate,
                            notes = notes,
                            onSaved = { activeEditor = ActiveEditor.None }
                        )
                    },
                    onDirtyStateChange = { activeEditorDirty = it }
                )
            }
            is ActiveEditor.LoanPayment -> {
                LoanPaymentDialog(
                    loan = editor.loan,
                    onDismiss = { activeEditor = ActiveEditor.None },
                    onConfirm = { newBalance ->
                        viewModel.registerLoanInstallmentPaid(editor.loan, newBalance)
                        activeEditor = ActiveEditor.None
                    },
                    onDirtyStateChange = { activeEditorDirty = it }
                )
            }
            ActiveEditor.DeclaredBalance -> {
                DeclaredBalanceDialog(
                    currentBalanceCents = settings.declaredBalanceCents,
                    onDismiss = { activeEditor = ActiveEditor.None },
                    onSave = { newBalance ->
                        viewModel.updateDeclaredBalance(newBalance)
                        activeEditor = ActiveEditor.None
                    },
                    onDirtyStateChange = { activeEditorDirty = it }
                )
            }
            ActiveEditor.BudgetEditor -> {
                BudgetEditorDialog(
                    categories = categories,
                    onDismiss = { activeEditor = ActiveEditor.None },
                    onSave = { catId, limitCents ->
                        viewModel.saveBudget(catId, limitCents) {
                            activeEditor = ActiveEditor.None
                        }
                    },
                    onDirtyStateChange = { activeEditorDirty = it }
                )
            }
            is ActiveEditor.FinancialGoalEditor -> {
                FinancialGoalEditorDialog(
                    existing = editor.goal,
                    onDismiss = { activeEditor = ActiveEditor.None },
                    onSave = { id, name, target, current, deadlineAt, notes ->
                        viewModel.saveFinancialGoal(
                            id = id,
                            name = name,
                            targetAmountCents = target,
                            currentAmountCents = current,
                            deadlineAt = deadlineAt,
                            categoryId = editor.goal?.categoryId,
                            notes = notes,
                            onSaved = { activeEditor = ActiveEditor.None }
                        )
                    },
                    onDirtyStateChange = { activeEditorDirty = it }
                )
            }
        }

        if (showEditorDiscardDialog && activeEditor != ActiveEditor.None) {
            ConfirmDiscardChangesDialog(
                onConfirmDiscard = {
                    showEditorDiscardDialog = false
                    activeEditor = ActiveEditor.None
                },
                onContinueEditing = { showEditorDiscardDialog = false }
            )
        }
    }
}
