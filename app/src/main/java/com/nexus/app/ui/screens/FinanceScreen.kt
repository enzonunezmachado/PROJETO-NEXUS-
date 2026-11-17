package com.nexus.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nexus.app.core.DateTimeUtils
import com.nexus.app.data.db.entity.AuditHistoryEntity
import com.nexus.app.data.db.entity.CategoryEntity
import com.nexus.app.data.db.entity.CreditCardEntity
import com.nexus.app.data.db.entity.FinancialGoalEntity
import com.nexus.app.data.db.entity.LoanEntity
import com.nexus.app.data.db.entity.RecurringRuleEntity
import com.nexus.app.domain.model.BudgetProgress
import com.nexus.app.domain.model.CardWithStatements
import com.nexus.app.domain.model.CategoryScope
import com.nexus.app.domain.model.FinancialStatus
import com.nexus.app.domain.model.InstallmentPurchaseWithItems
import com.nexus.app.domain.model.PaymentMethod
import com.nexus.app.domain.model.StatementStatus
import com.nexus.app.domain.model.TransactionType
import com.nexus.app.domain.model.TransactionWithDetails
import com.nexus.app.domain.rules.CashflowSeriesMode
import com.nexus.app.domain.rules.CategorySpendScope
import com.nexus.app.domain.rules.FinanceRules
import com.nexus.app.domain.rules.MonthlyFinanceDashboardData
import com.nexus.app.ui.FinanceFilterState
import com.nexus.app.ui.FinanceTabMode
import com.nexus.app.ui.components.AlertBannerCard
import com.nexus.app.ui.components.AlertSeverity
import com.nexus.app.ui.components.MonthYearSelectorBar
import com.nexus.app.ui.components.NexusCard
import com.nexus.app.ui.components.NexusEmptyState
import com.nexus.app.ui.components.SectionHeader
import com.nexus.app.ui.components.charts.BarGroupChart
import com.nexus.app.ui.components.charts.BarGroupEntry
import com.nexus.app.ui.components.charts.DonutChart
import com.nexus.app.ui.components.charts.DonutSlice
import com.nexus.app.ui.components.charts.ProgressBarRow
import com.nexus.app.ui.components.parseHexColorOrDefault
import com.nexus.app.ui.theme.LocalNexusPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Composable
fun FinanceScreen(
    dashboardData: MonthlyFinanceDashboardData,
    allTransactions: List<TransactionWithDetails>,
    transactions: List<TransactionWithDetails>,
    categories: List<CategoryEntity>,
    filter: FinanceFilterState,
    cardsWithStatements: List<CardWithStatements>,
    installmentPurchases: List<InstallmentPurchaseWithItems>,
    recurringRules: List<RecurringRuleEntity>,
    financialGoals: List<FinancialGoalEntity>,
    loans: List<LoanEntity>,
    auditHistory: List<AuditHistoryEntity>,
    onTabChange: (FinanceTabMode) -> Unit,
    onMonthChange: (YearMonth) -> Unit,
    onTypeFilterChange: (TransactionType?) -> Unit,
    onStatusFilterChange: (FinancialStatus?) -> Unit,
    onCategoryFilterChange: (String?) -> Unit,
    onPaymentMethodFilterChange: (PaymentMethod?) -> Unit,
    onClearFilters: () -> Unit,
    onEditDeclaredBalance: () -> Unit,
    onCreateTransaction: (TransactionType) -> Unit,
    onOpenTransaction: (String) -> Unit,
    onToggleSettleTransaction: (String) -> Unit,
    onCreateCard: () -> Unit,
    onEditCard: (CreditCardEntity) -> Unit,
    onCloseStatement: (String) -> Unit,
    onPayStatement: (String) -> Unit,
    onCreateInstallment: () -> Unit,
    onSettleRemainingInstallments: (String) -> Unit,
    onDeleteInstallmentPurchase: (String) -> Unit,
    onCreateRecurring: () -> Unit,
    onToggleRecurringActive: (RecurringRuleEntity) -> Unit,
    onDeleteRecurringRule: (String) -> Unit,
    onCreateBudget: () -> Unit,
    onDeleteBudget: (String) -> Unit,
    onCreateGoal: () -> Unit,
    onEditGoal: (FinancialGoalEntity) -> Unit,
    onUpdateGoalAmount: (FinancialGoalEntity, Long) -> Unit,
    onDeleteGoal: (String) -> Unit,
    onCreateLoan: () -> Unit,
    onEditLoan: (LoanEntity) -> Unit,
    onPayLoanInstallment: (LoanEntity) -> Unit,
    onDeleteLoan: (String) -> Unit,
    onGenerateCsv: suspend () -> Result<String>,
    onCsvExported: () -> Unit,
    onLogError: (String, Throwable) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalNexusPalette.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val csvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val csvResult = onGenerateCsv()
            csvResult.onSuccess { csv ->
                runCatching {
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use { stream ->
                            stream.write(csv.toByteArray(Charsets.UTF_8))
                        } ?: error("Não foi possível abrir o destino selecionado.")
                    }
                }.onSuccess {
                    onCsvExported()
                }.onFailure { err ->
                    onLogError("Financeiro (Exportação CSV)", err)
                }
            }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Financeiro",
                style = MaterialTheme.typography.headlineMedium,
                color = palette.textPrimary
            )
            OutlinedButton(
                onClick = { onCreateTransaction(TransactionType.EXPENSE) },
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, palette.border),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Lançamento", color = palette.textPrimary, style = MaterialTheme.typography.labelLarge)
            }
        }

        ScrollableTabRow(
            selectedTabIndex = FinanceTabMode.entries.indexOf(filter.tabMode),
            containerColor = palette.background,
            contentColor = palette.highlight,
            edgePadding = 16.dp
        ) {
            FinanceTabMode.entries.forEach { tab ->
                Tab(
                    selected = filter.tabMode == tab,
                    onClick = { onTabChange(tab) },
                    text = { Text(tab.label, maxLines = 1) }
                )
            }
        }

        when (filter.tabMode) {
            FinanceTabMode.OVERVIEW -> OverviewTabContent(
                dashboardData = dashboardData,
                allTransactions = allTransactions,
                selectedMonth = filter.selectedMonth,
                onMonthChange = onMonthChange,
                onEditDeclaredBalance = onEditDeclaredBalance,
                onOpenTransaction = onOpenTransaction,
                onToggleSettleTransaction = onToggleSettleTransaction,
                onCreateTransaction = onCreateTransaction,
                onCreateBudget = onCreateBudget,
                onFilterCategoryInTransactions = { catId ->
                    onCategoryFilterChange(catId)
                    onTypeFilterChange(TransactionType.EXPENSE)
                    onTabChange(FinanceTabMode.TRANSACTIONS)
                },
                onOpenAllTransactions = { onTabChange(FinanceTabMode.TRANSACTIONS) }
            )
            FinanceTabMode.TRANSACTIONS -> TransactionsTabContent(
                transactions = transactions,
                categories = categories,
                filter = filter,
                onTypeFilterChange = onTypeFilterChange,
                onStatusFilterChange = onStatusFilterChange,
                onCategoryFilterChange = onCategoryFilterChange,
                onPaymentMethodFilterChange = onPaymentMethodFilterChange,
                onClearFilters = onClearFilters,
                onOpenTransaction = onOpenTransaction,
                onToggleSettleTransaction = onToggleSettleTransaction,
                onCreateTransaction = { onCreateTransaction(TransactionType.EXPENSE) },
                onExportCsv = {
                    val stamp = DateTimeUtils.formatDate(DateTimeUtils.nowEpochMillis()).replace("/", "-")
                    csvLauncher.launch("nexus-financeiro-$stamp.csv")
                }
            )
            FinanceTabMode.CARDS -> CardsTabContent(
                cards = cardsWithStatements,
                onCreateCard = onCreateCard,
                onEditCard = onEditCard,
                onCloseStatement = onCloseStatement,
                onPayStatement = onPayStatement,
                onOpenTransaction = onOpenTransaction
            )
            FinanceTabMode.RECURRING -> RecurringAndInstallmentsTabContent(
                installmentPurchases = installmentPurchases,
                recurringRules = recurringRules,
                onCreateInstallment = onCreateInstallment,
                onSettleRemainingInstallments = onSettleRemainingInstallments,
                onDeleteInstallmentPurchase = onDeleteInstallmentPurchase,
                onCreateRecurring = onCreateRecurring,
                onToggleRecurringActive = onToggleRecurringActive,
                onDeleteRecurringRule = onDeleteRecurringRule
            )
            FinanceTabMode.PLANNING -> PlanningAndLoansTabContent(
                selectedMonth = filter.selectedMonth,
                budgets = dashboardData.budgetProgress,
                goals = financialGoals,
                loans = loans,
                auditHistory = auditHistory,
                onCreateBudget = onCreateBudget,
                onDeleteBudget = onDeleteBudget,
                onCreateGoal = onCreateGoal,
                onEditGoal = onEditGoal,
                onUpdateGoalAmount = onUpdateGoalAmount,
                onDeleteGoal = onDeleteGoal,
                onCreateLoan = onCreateLoan,
                onEditLoan = onEditLoan,
                onPayLoanInstallment = onPayLoanInstallment,
                onDeleteLoan = onDeleteLoan
            )
        }
    }
}

@Composable
private fun OverviewTabContent(
    dashboardData: MonthlyFinanceDashboardData,
    allTransactions: List<TransactionWithDetails>,
    selectedMonth: YearMonth,
    onMonthChange: (YearMonth) -> Unit,
    onEditDeclaredBalance: () -> Unit,
    onOpenTransaction: (String) -> Unit,
    onToggleSettleTransaction: (String) -> Unit,
    onCreateTransaction: (TransactionType) -> Unit,
    onCreateBudget: () -> Unit,
    onFilterCategoryInTransactions: (String?) -> Unit,
    onOpenAllTransactions: () -> Unit
) {
    val palette = LocalNexusPalette.current
    val today = DateTimeUtils.todayLocalDate()
    val currentMonth = YearMonth.from(today)
    val monthTitle = remember(selectedMonth) {
        val raw = selectedMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", DateTimeUtils.LOCALE_PT_BR))
        raw.replaceFirstChar { it.uppercase() }
    }

    var spendScope by remember { mutableStateOf(CategorySpendScope.PAID_AND_PLANNED) }
    var selectedCategorySliceId by remember(selectedMonth, spendScope) { mutableStateOf<String?>(null) }
    var cashflowMode by remember { mutableStateOf(CashflowSeriesMode.REALIZED) }
    var selectedBarMonthIso by remember(selectedMonth) { mutableStateOf<String?>(selectedMonth.toString()) }

    val summary = dashboardData.monthlySummary
    val monthResultCents = dashboardData.monthResultCents
    val activeCategoryPoints = remember(dashboardData, spendScope) {
        when (spendScope) {
            CategorySpendScope.PAID_ONLY -> dashboardData.categorySpendPaidOnly
            CategorySpendScope.PAID_AND_PLANNED -> dashboardData.categorySpendPaidAndPlanned
        }
    }
    val categoryTotalCents = remember(activeCategoryPoints) {
        activeCategoryPoints.sumOf { it.totalCents }
    }
    val donutSlices = remember(activeCategoryPoints, categoryTotalCents, palette) {
        val safeTotal = categoryTotalCents.coerceAtLeast(1L).toFloat()
        activeCategoryPoints.mapIndexed { idx, pt ->
            val fallbackColor = palette.chartPalette[idx % palette.chartPalette.size]
            val parsedColor = parseHexColorOrDefault(pt.colorHex, fallbackColor)
            DonutSlice(
                id = pt.categoryId ?: "__UNCATEGORIZED__",
                label = pt.categoryName,
                valueCents = pt.totalCents,
                color = parsedColor,
                percentage = (pt.totalCents.toFloat() / safeTotal) * 100f
            )
        }
    }

    val topExplicitCategoryIds = remember(activeCategoryPoints) {
        activeCategoryPoints
            .mapNotNull { it.categoryId }
            .filter { it != FinanceRules.OTHERS_CATEGORY_ID }
            .toSet()
    }

    val filteredMonthCategoryTransactions = remember(
        allTransactions,
        selectedMonth,
        spendScope,
        selectedCategorySliceId,
        topExplicitCategoryIds
    ) {
        val sel = selectedCategorySliceId ?: return@remember emptyList()
        allTransactions.filter { item ->
            val tx = FinanceRules.effectiveTransactionStatus(item.transaction, today)
            if (tx.type != TransactionType.EXPENSE || tx.status == FinancialStatus.CANCELLED) return@filter false
            if (spendScope == CategorySpendScope.PAID_ONLY && tx.status != FinancialStatus.SETTLED) return@filter false
            val ym = YearMonth.from(DateTimeUtils.toLocalDate(tx.dueAt ?: tx.occurredAt))
            if (ym != selectedMonth) return@filter false
            when (sel) {
                FinanceRules.OTHERS_CATEGORY_ID -> tx.categoryId != null && tx.categoryId !in topExplicitCategoryIds
                "__UNCATEGORIZED__" -> tx.categoryId == null
                else -> tx.categoryId == sel
            }
        }
    }

    val cashflowEntries = remember(dashboardData, cashflowMode) {
        val list = when (cashflowMode) {
            CashflowSeriesMode.REALIZED -> dashboardData.sixMonthCashflowRealized
            CashflowSeriesMode.PLANNED -> dashboardData.sixMonthCashflowPlanned
        }
        list.map { pt ->
            BarGroupEntry(
                id = pt.yearMonthIso,
                label = pt.yearMonthLabel,
                primaryValueCents = pt.receivedIncomeCents,
                secondaryValueCents = pt.paidExpenseCents
            )
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "fin_month_selector") {
            MonthYearSelectorBar(
                currentMonth = selectedMonth,
                onPreviousMonth = { onMonthChange(selectedMonth.minusMonths(1)) },
                onNextMonth = { onMonthChange(selectedMonth.plusMonths(1)) },
                onResetToCurrentMonth = if (selectedMonth != currentMonth) {
                    { onMonthChange(currentMonth) }
                } else null
            )
        }

        item(key = "fin_balance_and_result") {
            NexusCard(modifier = Modifier.fillMaxWidth(), elevated = true) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onEditDeclaredBalance),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Saldo atual informado",
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.textSecondary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = DateTimeUtils.formatBrl(dashboardData.declaredBalanceCents),
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontFeatureSettings = "tnum"
                            ),
                            color = if (dashboardData.declaredBalanceCents >= 0L) palette.textPrimary else palette.danger
                        )
                    }
                    IconButton(onClick = onEditDeclaredBalance) {
                        Icon(Icons.Outlined.Edit, contentDescription = "Editar saldo informado", tint = palette.highlight)
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = palette.border)
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Resultado do mês (realizado)",
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.textSecondary
                    )
                    Text(
                        text = DateTimeUtils.formatBrl(monthResultCents),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontFeatureSettings = "tnum"
                        ),
                        color = if (monthResultCents >= 0L) palette.positive else palette.danger
                    )
                }
            }
        }

        item(key = "fin_month_totals") {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FinanceMetricCard(
                    title = "Receitas realizadas",
                    amountCents = summary.receivedIncomeCents,
                    accent = palette.positive,
                    subtitle = "Previstas no mês: ${DateTimeUtils.formatBrl(summary.plannedIncomeCents)}",
                    modifier = Modifier.weight(1f)
                )
                FinanceMetricCard(
                    title = "Despesas pagas",
                    amountCents = summary.paidExpenseCents,
                    accent = palette.textPrimary,
                    subtitle = "Pendentes no mês: ${DateTimeUtils.formatBrl(summary.pendingExpenseCents)}",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (summary.overdueExpenseCents > 0L) {
            item(key = "fin_overdue_alert") {
                AlertBannerCard(
                    severity = AlertSeverity.DANGER,
                    title = "${summary.overdueCount} ${if (summary.overdueCount == 1) "conta vencida" else "contas vencidas"}",
                    subtitle = "Total em aberto (todas as datas): ${DateTimeUtils.formatBrl(summary.overdueExpenseCents)}",
                    onClick = {
                        onStatusFilterChangeSafeAction(onOpenAllTransactions)
                    }
                )
            }
        }

        item(key = "fin_donut_categories") {
            NexusCard(modifier = Modifier.fillMaxWidth(), elevated = true) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Despesas por categoria",
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.textPrimary
                    )
                    SegmentedTogglePill(
                        options = CategorySpendScope.entries.toList(),
                        selected = spendScope,
                        labelOf = { it.label },
                        onSelect = { spendScope = it }
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                DonutChart(
                    slices = donutSlices,
                    totalCents = categoryTotalCents,
                    centerLabel = if (spendScope == CategorySpendScope.PAID_ONLY) "Pagas" else "Total do mês",
                    selectedSliceId = selectedCategorySliceId,
                    onSliceClick = { selectedCategorySliceId = it },
                    emptyMessage = "Sem despesas registradas para este mês."
                )

                if (selectedCategorySliceId != null) {
                    val selLabel = donutSlices.firstOrNull { it.id == selectedCategorySliceId }?.label ?: "Categoria"
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = palette.border)
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Lançamentos em $selLabel (${filteredMonthCategoryTransactions.size})",
                            style = MaterialTheme.typography.labelLarge,
                            color = palette.textPrimary
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(
                                onClick = { selectedCategorySliceId = null },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("Limpar", style = MaterialTheme.typography.labelMedium, color = palette.textSecondary)
                            }
                            if (selectedCategorySliceId != FinanceRules.OTHERS_CATEGORY_ID &&
                                selectedCategorySliceId != "__UNCATEGORIZED__"
                            ) {
                                TextButton(
                                    onClick = { onFilterCategoryInTransactions(selectedCategorySliceId) },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("Ver todos", style = MaterialTheme.typography.labelMedium, color = palette.highlight)
                                }
                            }
                        }
                    }
                    if (filteredMonthCategoryTransactions.isEmpty()) {
                        Text(
                            text = "Nenhum lançamento encontrado para esta seleção.",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textSecondary
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            filteredMonthCategoryTransactions.take(6).forEach { txItem ->
                                CompactOverviewTransactionRow(
                                    item = txItem,
                                    onClick = { onOpenTransaction(txItem.transaction.id) },
                                    onToggleSettle = { onToggleSettleTransaction(txItem.transaction.id) }
                                )
                            }
                        }
                    }
                }
            }
        }

        item(key = "fin_bars_6months") {
            NexusCard(modifier = Modifier.fillMaxWidth(), elevated = true) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Comparativo 6 meses",
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.textPrimary
                    )
                    SegmentedTogglePill(
                        options = CashflowSeriesMode.entries.toList(),
                        selected = cashflowMode,
                        labelOf = { it.label },
                        onSelect = { cashflowMode = it }
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (cashflowMode == CashflowSeriesMode.REALIZED) {
                        "Valores efetivamente recebidos e pagos"
                    } else {
                        "Valores previstos/pendentes ainda não liquidados"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textSecondary
                )
                Spacer(modifier = Modifier.height(10.dp))
                BarGroupChart(
                    entries = cashflowEntries,
                    primaryLabel = if (cashflowMode == CashflowSeriesMode.REALIZED) "Receitas pagas" else "Receitas previstas",
                    secondaryLabel = if (cashflowMode == CashflowSeriesMode.REALIZED) "Despesas pagas" else "Despesas previstas",
                    selectedEntryId = selectedBarMonthIso,
                    onSelectEntry = { entry ->
                        selectedBarMonthIso = entry.id
                        runCatching { YearMonth.parse(entry.id) }.getOrNull()?.let { pickedYm ->
                            if (pickedYm != selectedMonth) {
                                onMonthChange(pickedYm)
                            }
                        }
                    },
                    emptyMessage = "Sem lançamentos ${cashflowMode.label.lowercase()} nos últimos 6 meses."
                )
            }
        }

        item(key = "fin_forecast_30d") {
            val forecast = dashboardData.forecast30Days
            NexusCard(modifier = Modifier.fillMaxWidth(), elevated = true) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Previsão próximos 30 dias",
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.textPrimary
                    )
                    Text(
                        text = "Previsto",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = palette.info
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Receitas previstas", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                        Text(
                            text = DateTimeUtils.formatBrl(forecast.plannedIncomeCents),
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontFeatureSettings = "tnum"
                            ),
                            color = palette.positive
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("A vencer (não pago)", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                        Text(
                            text = DateTimeUtils.formatBrl(forecast.upcomingExpensesCents),
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontFeatureSettings = "tnum"
                            ),
                            color = palette.warning
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = palette.border)
                Spacer(modifier = Modifier.height(8.dp))
                if (forecast.topUpcomingExpenses.isEmpty()) {
                    Text(
                        text = "Nenhuma conta a vencer nos próximos 30 dias.",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        forecast.topUpcomingExpenses.forEach { txItem ->
                            CompactOverviewTransactionRow(
                                item = txItem,
                                onClick = { onOpenTransaction(txItem.transaction.id) },
                                onToggleSettle = { onToggleSettleTransaction(txItem.transaction.id) }
                            )
                        }
                    }
                }
            }
        }

        item(key = "fin_budgets_overview") {
            NexusCard(modifier = Modifier.fillMaxWidth(), elevated = true) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Orçamentos do mês",
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.textPrimary
                    )
                    TextButton(
                        onClick = onCreateBudget,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("Definir", color = palette.highlight)
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                if (dashboardData.budgetProgress.isEmpty()) {
                    Text(
                        text = "Nenhum orçamento definido para $monthTitle.",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        dashboardData.budgetProgress.forEach { bp ->
                            ProgressBarRow(
                                label = bp.category.name,
                                currentCents = bp.spentCents,
                                targetCents = bp.budget.limitCents,
                                usedPercent = bp.usedPercent,
                                exceededCents = bp.exceededCents
                            )
                        }
                    }
                }
            }
        }

        item(key = "fin_quick_actions") {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { onCreateTransaction(TransactionType.INCOME) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, palette.border)
                ) {
                    Text("+ Receita", color = palette.positive)
                }
                OutlinedButton(
                    onClick = { onCreateTransaction(TransactionType.EXPENSE) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, palette.border)
                ) {
                    Text("+ Despesa", color = palette.textPrimary)
                }
            }
        }
    }
}

private fun onStatusFilterChangeSafeAction(openAll: () -> Unit) {
    openAll()
}

@Composable
private fun <T> SegmentedTogglePill(
    options: List<T>,
    selected: T,
    labelOf: (T) -> String,
    onSelect: (T) -> Unit
) {
    val palette = LocalNexusPalette.current
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = palette.surfaceElevated,
        border = BorderStroke(1.dp, palette.border)
    ) {
        Row(modifier = Modifier.padding(2.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            options.forEach { opt ->
                val isSel = opt == selected
                Surface(
                    onClick = { onSelect(opt) },
                    shape = RoundedCornerShape(6.dp),
                    color = if (isSel) palette.highlight else Color.Transparent
                ) {
                    Text(
                        text = labelOf(opt),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (isSel) FontWeight.SemiBold else FontWeight.Normal
                        ),
                        color = if (isSel) palette.background else palette.textSecondary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CompactOverviewTransactionRow(
    item: TransactionWithDetails,
    onClick: () -> Unit,
    onToggleSettle: () -> Unit
) {
    val palette = LocalNexusPalette.current
    val tx = item.transaction
    val isSettled = tx.status == FinancialStatus.SETTLED
    val isIncome = tx.type == TransactionType.INCOME
    val refEpoch = tx.dueAt ?: tx.occurredAt

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onToggleSettle,
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                imageVector = if (isSettled) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                contentDescription = if (isSettled) "Reabrir" else "Liquidar",
                tint = when (tx.status) {
                    FinancialStatus.SETTLED -> palette.positive
                    FinancialStatus.OVERDUE -> palette.danger
                    else -> palette.textSecondary
                },
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = tx.description,
                style = MaterialTheme.typography.bodyMedium,
                color = palette.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = buildList {
                    add(DateTimeUtils.formatDate(refEpoch))
                    add(tx.status.label)
                    item.category?.let { add(it.name) }
                }.joinToString(" · "),
                style = MaterialTheme.typography.labelSmall,
                color = if (tx.status == FinancialStatus.OVERDUE) palette.danger else palette.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = (if (isIncome) "+" else "-") + DateTimeUtils.formatBrl(tx.amountCents),
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.SemiBold,
                fontFeatureSettings = "tnum"
            ),
            color = if (isIncome) palette.positive else palette.textPrimary
        )
    }
}

@Composable
private fun FinanceMetricCard(
    title: String,
    amountCents: Long,
    accent: Color,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    val palette = LocalNexusPalette.current
    NexusCard(modifier = modifier, elevated = true) {
        Text(text = title, style = MaterialTheme.typography.labelMedium, color = palette.textSecondary)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = DateTimeUtils.formatBrl(amountCents),
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.SemiBold,
                fontFeatureSettings = "tnum"
            ),
            color = accent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
            color = palette.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun TransactionsTabContent(
    transactions: List<TransactionWithDetails>,
    categories: List<CategoryEntity>,
    filter: FinanceFilterState,
    onTypeFilterChange: (TransactionType?) -> Unit,
    onStatusFilterChange: (FinancialStatus?) -> Unit,
    onCategoryFilterChange: (String?) -> Unit,
    onPaymentMethodFilterChange: (PaymentMethod?) -> Unit,
    onClearFilters: () -> Unit,
    onOpenTransaction: (String) -> Unit,
    onToggleSettleTransaction: (String) -> Unit,
    onCreateTransaction: () -> Unit,
    onExportCsv: () -> Unit
) {
    val palette = LocalNexusPalette.current
    var showFilterSheet by remember { mutableStateOf(false) }

    val activeFilterCount = remember(filter) {
        var c = 0
        if (filter.typeFilter != null) c++
        if (filter.statusFilter != null) c++
        if (filter.selectedCategoryFilterId != null && filter.selectedCategoryFilterId != FinanceRules.OTHERS_CATEGORY_ID) c++
        if (filter.paymentMethodFilter != null) c++
        c
    }

    if (showFilterSheet) {
        FinanceFilterBottomSheet(
            filter = filter,
            categories = categories.filter { it.scope == CategoryScope.FINANCE || it.scope == CategoryScope.GENERAL },
            onTypeFilterChange = onTypeFilterChange,
            onStatusFilterChange = onStatusFilterChange,
            onCategoryFilterChange = onCategoryFilterChange,
            onPaymentMethodFilterChange = onPaymentMethodFilterChange,
            onClearFilters = onClearFilters,
            onDismiss = { showFilterSheet = false }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item(key = "tx_compact_actions") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${transactions.size} ${if (transactions.size == 1) "lançamento" else "lançamentos"}",
                    style = MaterialTheme.typography.labelLarge,
                    color = palette.textSecondary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { showFilterSheet = true },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, if (activeFilterCount > 0) palette.highlight else palette.border),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            Icons.Outlined.FilterList,
                            contentDescription = "Filtros",
                            modifier = Modifier.size(16.dp),
                            tint = if (activeFilterCount > 0) palette.highlight else palette.textPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (activeFilterCount > 0) "Filtros ($activeFilterCount)" else "Filtros",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (activeFilterCount > 0) palette.highlight else palette.textPrimary
                        )
                    }
                    OutlinedButton(
                        onClick = onExportCsv,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, palette.border),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Outlined.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("CSV", color = palette.textPrimary, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }

        if (transactions.isEmpty()) {
            item {
                NexusEmptyState(
                    icon = Icons.Outlined.AccountBalanceWallet,
                    title = "Nenhum lançamento encontrado",
                    subtitle = if (activeFilterCount > 0) {
                        "Nenhum registro corresponde aos filtros selecionados."
                    } else {
                        "Registre receitas, despesas ou contas para acompanhar previstos e realizados."
                    },
                    actionLabel = if (activeFilterCount > 0) "Limpar filtros" else "Novo lançamento",
                    onAction = if (activeFilterCount > 0) onClearFilters else onCreateTransaction
                )
            }
        } else {
            items(transactions, key = { it.transaction.id }) { item ->
                TransactionRowCard(
                    item = item,
                    onClick = { onOpenTransaction(item.transaction.id) },
                    onToggleSettle = { onToggleSettleTransaction(item.transaction.id) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun FinanceFilterBottomSheet(
    filter: FinanceFilterState,
    categories: List<CategoryEntity>,
    onTypeFilterChange: (TransactionType?) -> Unit,
    onStatusFilterChange: (FinancialStatus?) -> Unit,
    onCategoryFilterChange: (String?) -> Unit,
    onPaymentMethodFilterChange: (PaymentMethod?) -> Unit,
    onClearFilters: () -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalNexusPalette.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = palette.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Filtrar lançamentos", style = MaterialTheme.typography.titleLarge, color = palette.textPrimary)
                TextButton(onClick = onClearFilters) {
                    Text("Limpar filtros", color = palette.textSecondary)
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Tipo", style = MaterialTheme.typography.labelLarge, color = palette.textSecondary)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = filter.typeFilter == null,
                        onClick = { onTypeFilterChange(null) },
                        label = { Text("Todos") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = palette.surfaceElevated,
                            selectedLabelColor = palette.highlight
                        )
                    )
                    TransactionType.entries.forEach { t ->
                        FilterChip(
                            selected = filter.typeFilter == t,
                            onClick = { onTypeFilterChange(if (filter.typeFilter == t) null else t) },
                            label = { Text(t.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = palette.surfaceElevated,
                                selectedLabelColor = palette.highlight
                            )
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Situação", style = MaterialTheme.typography.labelLarge, color = palette.textSecondary)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = filter.statusFilter == null,
                        onClick = { onStatusFilterChange(null) },
                        label = { Text("Todas") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = palette.surfaceElevated,
                            selectedLabelColor = palette.highlight
                        )
                    )
                    FinancialStatus.entries.forEach { st ->
                        FilterChip(
                            selected = filter.statusFilter == st,
                            onClick = { onStatusFilterChange(if (filter.statusFilter == st) null else st) },
                            label = { Text(st.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = palette.surfaceElevated,
                                selectedLabelColor = palette.highlight
                            )
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Forma de pagamento", style = MaterialTheme.typography.labelLarge, color = palette.textSecondary)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = filter.paymentMethodFilter == null,
                        onClick = { onPaymentMethodFilterChange(null) },
                        label = { Text("Todas") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = palette.surfaceElevated,
                            selectedLabelColor = palette.highlight
                        )
                    )
                    PaymentMethod.entries.forEach { pm ->
                        FilterChip(
                            selected = filter.paymentMethodFilter == pm,
                            onClick = { onPaymentMethodFilterChange(if (filter.paymentMethodFilter == pm) null else pm) },
                            label = { Text(pm.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = palette.surfaceElevated,
                                selectedLabelColor = palette.highlight
                            )
                        )
                    }
                }
            }

            if (categories.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Categoria", style = MaterialTheme.typography.labelLarge, color = palette.textSecondary)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = filter.selectedCategoryFilterId == null,
                            onClick = { onCategoryFilterChange(null) },
                            label = { Text("Todas") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = palette.surfaceElevated,
                                selectedLabelColor = palette.highlight
                            )
                        )
                        categories.forEach { cat ->
                            FilterChip(
                                selected = filter.selectedCategoryFilterId == cat.id,
                                onClick = {
                                    onCategoryFilterChange(if (filter.selectedCategoryFilterId == cat.id) null else cat.id)
                                },
                                label = { Text(cat.name) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = palette.surfaceElevated,
                                    selectedLabelColor = palette.highlight
                                )
                            )
                        }
                    }
                }
            }

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.highlight,
                    contentColor = palette.background
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Aplicar")
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
fun TransactionRowCard(
    item: TransactionWithDetails,
    onClick: () -> Unit,
    onToggleSettle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalNexusPalette.current
    val tx = item.transaction
    val isSettled = tx.status == FinancialStatus.SETTLED
    val isIncome = tx.type == TransactionType.INCOME

    NexusCard(
        modifier = modifier.fillMaxWidth(),
        elevated = true,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onToggleSettle,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = if (isSettled) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                    contentDescription = if (isSettled) "Reabrir como pendente" else "Marcar realizado",
                    tint = when (tx.status) {
                        FinancialStatus.SETTLED -> palette.positive
                        FinancialStatus.OVERDUE -> palette.danger
                        else -> palette.textSecondary
                    }
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tx.description,
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val meta = buildList {
                    add(tx.status.label)
                    add(tx.paymentMethod.label)
                    val dateRef = tx.dueAt ?: tx.occurredAt
                    add(DateTimeUtils.formatDate(dateRef))
                    item.category?.let { add(it.name) }
                    item.task?.let { add("Tarefa: ${it.title}") }
                    item.event?.let { add("Evento: ${it.name}") }
                    item.creditCard?.let { add(it.name) }
                }.joinToString(" · ")
                Text(
                    text = meta,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (tx.status == FinancialStatus.OVERDUE) palette.danger else palette.textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = (if (isIncome) "+" else "-") + DateTimeUtils.formatBrl(tx.amountCents),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontFeatureSettings = "tnum"
                ),
                color = if (isIncome) palette.positive else palette.textPrimary
            )
        }
    }
}

@Composable
private fun CardsTabContent(
    cards: List<CardWithStatements>,
    onCreateCard: () -> Unit,
    onEditCard: (CreditCardEntity) -> Unit,
    onCloseStatement: (String) -> Unit,
    onPayStatement: (String) -> Unit,
    onOpenTransaction: (String) -> Unit
) {
    val palette = LocalNexusPalette.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Cartões e Faturas", style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
                OutlinedButton(onClick = onCreateCard, shape = RoundedCornerShape(10.dp)) {
                    Text("Novo cartão", color = palette.textPrimary)
                }
            }
        }

        if (cards.isEmpty()) {
            item {
                NexusEmptyState(
                    icon = Icons.Outlined.CreditCard,
                    title = "Nenhum cartão cadastrado",
                    subtitle = "Cadastre cartões para agrupar compras por fechamento e quitar faturas sem duplicar despesas.",
                    actionLabel = "Adicionar cartão",
                    onAction = onCreateCard
                )
            }
        } else {
            items(cards, key = { it.card.id }) { cw ->
                val card = cw.card
                NexusCard(
                    modifier = Modifier.fillMaxWidth(),
                    elevated = true,
                    onClick = { onEditCard(card) }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(card.name, style = MaterialTheme.typography.titleLarge, color = palette.textPrimary)
                            Text(
                                text = buildList {
                                    if (card.institution.isNotBlank()) add(card.institution)
                                    add("Fecha dia ${card.closingDay}")
                                    add("Vence dia ${card.dueDay}")
                                }.joinToString(" · "),
                                style = MaterialTheme.typography.labelMedium,
                                color = palette.textSecondary
                            )
                        }
                        Text(
                            text = DateTimeUtils.formatBrl(cw.currentOpenTotalCents),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontFeatureSettings = "tnum"
                            ),
                            color = palette.warning
                        )
                    }

                    val limitBreakdown = FinanceRules.resolveCreditCardLimitBreakdown(
                        totalLimitCents = card.totalLimitCents,
                        computedRemainingLimitCents = cw.computedRemainingLimitCents,
                        declaredAvailableLimitCents = card.availableLimitCents
                    )
                    if (limitBreakdown.totalLimitCents > 0L || limitBreakdown.declaredAvailableCents != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        if (limitBreakdown.totalLimitCents > 0L) {
                            Text(
                                text = "Limite total: ${DateTimeUtils.formatBrl(limitBreakdown.totalLimitCents)} · Disponível (calculado): ${DateTimeUtils.formatBrl(limitBreakdown.computedAvailableCents)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = palette.textSecondary
                            )
                        }
                        limitBreakdown.declaredAvailableCents?.let { declaredCents ->
                            Text(
                                text = "Disponível (informado): ${DateTimeUtils.formatBrl(declaredCents)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = palette.info
                            )
                        }
                    }

                    if (cw.statements.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = palette.border)
                        Spacer(modifier = Modifier.height(8.dp))
                        cw.statements.take(4).forEach { sw ->
                            val st = sw.statement
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Fatura ${st.yearMonth} · ${st.status.label}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (st.status == StatementStatus.PAID) palette.positive else palette.textPrimary
                                    )
                                    Text(
                                        text = "Vence ${DateTimeUtils.formatDate(st.dueAt)} · ${sw.transactions.size} item(ns) · ${DateTimeUtils.formatBrl(sw.totalCents)}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = palette.textSecondary
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    if (st.status == StatementStatus.OPEN) {
                                        TextButton(onClick = { onCloseStatement(st.id) }) {
                                            Text("Fechar", color = palette.textSecondary)
                                        }
                                    }
                                    if (st.status != StatementStatus.PAID) {
                                        OutlinedButton(
                                            onClick = { onPayStatement(st.id) },
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("Pagar", color = palette.positive)
                                        }
                                    }
                                }
                            }
                            if (sw.transactions.isNotEmpty() && st.status != StatementStatus.PAID) {
                                sw.transactions.take(3).forEach { txItem ->
                                    TextButton(
                                        onClick = { onOpenTransaction(txItem.transaction.id) },
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                                    ) {
                                        Text(
                                            text = "  • ${txItem.transaction.description}: ${DateTimeUtils.formatBrl(txItem.transaction.amountCents)}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = palette.textSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecurringAndInstallmentsTabContent(
    installmentPurchases: List<InstallmentPurchaseWithItems>,
    recurringRules: List<RecurringRuleEntity>,
    onCreateInstallment: () -> Unit,
    onSettleRemainingInstallments: (String) -> Unit,
    onDeleteInstallmentPurchase: (String) -> Unit,
    onCreateRecurring: () -> Unit,
    onToggleRecurringActive: (RecurringRuleEntity) -> Unit,
    onDeleteRecurringRule: (String) -> Unit
) {
    val palette = LocalNexusPalette.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Compras Parceladas", style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
                OutlinedButton(onClick = onCreateInstallment, shape = RoundedCornerShape(10.dp)) {
                    Text("Novo parcelamento", color = palette.textPrimary)
                }
            }
        }

        if (installmentPurchases.isEmpty()) {
            item {
                NexusEmptyState(
                    icon = Icons.Outlined.CreditCard,
                    title = "Nenhum parcelamento cadastrado",
                    subtitle = "Gere parcelas mensais automaticamente no cartão ou em conta.",
                    actionLabel = "Criar parcelamento",
                    onAction = onCreateInstallment
                )
            }
        } else {
            items(installmentPurchases, key = { it.purchase.id }) { item ->
                val p = item.purchase
                val progress = if (p.installmentsCount > 0) {
                    item.paidCount.toFloat() / p.installmentsCount.toFloat()
                } else 0f

                NexusCard(modifier = Modifier.fillMaxWidth(), elevated = true) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(p.description, style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
                            Text(
                                text = "${item.paidCount}/${p.installmentsCount} pagas · Total ${DateTimeUtils.formatBrl(p.totalAmountCents)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = palette.textSecondary
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (item.remainingCents > 0L) {
                                TextButton(onClick = { onSettleRemainingInstallments(p.id) }) {
                                    Text("Quitar", color = palette.positive)
                                }
                            } else {
                                Text("Quitado", style = MaterialTheme.typography.labelMedium, color = palette.positive)
                            }
                            IconButton(onClick = { onDeleteInstallmentPurchase(p.id) }) {
                                Icon(
                                    imageVector = Icons.Outlined.DeleteOutline,
                                    contentDescription = "Excluir parcelamento",
                                    tint = palette.danger
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { progress.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = if (item.remainingCents == 0L) palette.positive else palette.highlight,
                        trackColor = palette.border
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Pago: ${DateTimeUtils.formatBrl(item.paidCents)} · Restante: ${DateTimeUtils.formatBrl(item.remainingCents)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.textSecondary
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Contas e Receitas Recorrentes", style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
                OutlinedButton(onClick = onCreateRecurring, shape = RoundedCornerShape(10.dp)) {
                    Text("Nova recorrência", color = palette.textPrimary)
                }
            }
        }

        if (recurringRules.isEmpty()) {
            item {
                NexusEmptyState(
                    icon = Icons.Outlined.Repeat,
                    title = "Nenhuma recorrência ativa",
                    subtitle = "Configure assinaturas, aluguel ou salários com geração idempotente sem duplicação.",
                    actionLabel = "Criar recorrência",
                    onAction = onCreateRecurring
                )
            }
        } else {
            items(recurringRules, key = { it.id }) { rule ->
                NexusCard(modifier = Modifier.fillMaxWidth(), elevated = true) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(rule.title, style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
                            Text(
                                text = "${rule.type.label} · ${DateTimeUtils.formatBrl(rule.amountCents)} · ${rule.frequency.label}",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (rule.type == TransactionType.INCOME) palette.positive else palette.textSecondary
                            )
                            Text(
                                text = "Início: ${DateTimeUtils.formatDate(rule.startAt)} · ${rule.paymentMethod.label}",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.textSecondary
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Switch(
                                checked = rule.isActive,
                                onCheckedChange = { onToggleRecurringActive(rule) }
                            )
                            IconButton(onClick = { onDeleteRecurringRule(rule.id) }) {
                                Icon(
                                    imageVector = Icons.Outlined.DeleteOutline,
                                    contentDescription = "Excluir recorrência",
                                    tint = palette.danger
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlanningAndLoansTabContent(
    selectedMonth: YearMonth,
    budgets: List<BudgetProgress>,
    goals: List<FinancialGoalEntity>,
    loans: List<LoanEntity>,
    auditHistory: List<AuditHistoryEntity>,
    onCreateBudget: () -> Unit,
    onDeleteBudget: (String) -> Unit,
    onCreateGoal: () -> Unit,
    onEditGoal: (FinancialGoalEntity) -> Unit,
    onUpdateGoalAmount: (FinancialGoalEntity, Long) -> Unit,
    onDeleteGoal: (String) -> Unit,
    onCreateLoan: () -> Unit,
    onEditLoan: (LoanEntity) -> Unit,
    onPayLoanInstallment: (LoanEntity) -> Unit,
    onDeleteLoan: (String) -> Unit
) {
    val palette = LocalNexusPalette.current
    var goalAmountDialogTarget by remember { mutableStateOf<FinancialGoalEntity?>(null) }
    val monthLabel = remember(selectedMonth) {
        selectedMonth.format(DateTimeFormatter.ofPattern("MM/yyyy", DateTimeUtils.LOCALE_PT_BR))
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Orçamentos ($monthLabel)", style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
                OutlinedButton(onClick = onCreateBudget, shape = RoundedCornerShape(10.dp)) {
                    Text("Definir orçamento", color = palette.textPrimary)
                }
            }
        }

        if (budgets.isEmpty()) {
            item {
                NexusCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Nenhum limite mensal definido por categoria em $monthLabel.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textSecondary
                    )
                }
            }
        } else {
            items(budgets, key = { it.budget.id }) { bp ->
                NexusCard(modifier = Modifier.fillMaxWidth(), elevated = true) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            ProgressBarRow(
                                label = bp.category.name,
                                currentCents = bp.spentCents,
                                targetCents = bp.budget.limitCents,
                                usedPercent = bp.usedPercent,
                                exceededCents = bp.exceededCents
                            )
                        }
                        IconButton(onClick = { onDeleteBudget(bp.budget.id) }) {
                            Icon(Icons.Outlined.DeleteOutline, contentDescription = "Remover orçamento", tint = palette.textSecondary)
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Metas Financeiras", style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
                OutlinedButton(onClick = onCreateGoal, shape = RoundedCornerShape(10.dp)) {
                    Text("Nova meta", color = palette.textPrimary)
                }
            }
        }

        if (goals.isEmpty()) {
            item {
                NexusEmptyState(
                    icon = Icons.Outlined.Savings,
                    title = "Nenhuma meta financeira",
                    subtitle = "Defina reservas de emergência, viagens ou aquisições e acompanhe o progresso.",
                    actionLabel = "Criar meta",
                    onAction = onCreateGoal
                )
            }
        } else {
            items(goals, key = { it.id }) { goal ->
                val fraction = if (goal.targetAmountCents > 0L) {
                    (goal.currentAmountCents.toFloat() / goal.targetAmountCents.toFloat()).coerceIn(0f, 1f)
                } else 0f
                val pct = (fraction * 100f).toInt()
                NexusCard(
                    modifier = Modifier.fillMaxWidth(),
                    elevated = true,
                    onClick = { onEditGoal(goal) }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(goal.name, style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
                            Text(
                                text = "${DateTimeUtils.formatBrl(goal.currentAmountCents)} de ${DateTimeUtils.formatBrl(goal.targetAmountCents)} ($pct%)",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (pct >= 100) palette.positive else palette.textSecondary
                            )
                        }
                        Row {
                            TextButton(onClick = { goalAmountDialogTarget = goal }) {
                                Text("Atualizar", color = palette.info)
                            }
                            IconButton(onClick = { onDeleteGoal(goal.id) }) {
                                Icon(Icons.Outlined.DeleteOutline, contentDescription = "Excluir", tint = palette.textSecondary)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { fraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = if (pct >= 100) palette.positive else palette.highlight,
                        trackColor = palette.border
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Empréstimos e Dívidas", style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
                OutlinedButton(onClick = onCreateLoan, shape = RoundedCornerShape(10.dp)) {
                    Text("Novo empréstimo", color = palette.textPrimary)
                }
            }
        }

        if (loans.isEmpty()) {
            item {
                NexusCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Nenhum empréstimo ou financiamento cadastrado.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textSecondary
                    )
                }
            }
        } else {
            items(loans, key = { it.id }) { loan ->
                val done = loan.paidInstallments >= loan.installmentsTotal
                NexusCard(
                    modifier = Modifier.fillMaxWidth(),
                    elevated = true,
                    onClick = { onEditLoan(loan) }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(loan.creditor, style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
                            Text(
                                text = "${loan.paidInstallments}/${loan.installmentsTotal} pagas · Saldo devedor: ${DateTimeUtils.formatBrl(loan.remainingBalanceCents)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (done) palette.positive else palette.warning
                            )
                            loan.nextDueAt?.let { due ->
                                Text(
                                    text = "Próximo vencimento: ${DateTimeUtils.formatDate(due)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.textSecondary
                                )
                            }
                        }
                        Row {
                            if (!done) {
                                TextButton(onClick = { onPayLoanInstallment(loan) }) {
                                    Text("Pagar parcela", color = palette.positive)
                                }
                            }
                            IconButton(onClick = { onDeleteLoan(loan.id) }) {
                                Icon(Icons.Outlined.DeleteOutline, contentDescription = "Excluir", tint = palette.textSecondary)
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(6.dp))
            SectionHeader(title = "Histórico recente de alterações")
        }

        if (auditHistory.isEmpty()) {
            item {
                NexusCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "As movimentações financeiras e de faturas serão registradas aqui.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textSecondary
                    )
                }
            }
        } else {
            items(auditHistory.take(15), key = { it.id }) { audit ->
                NexusCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "${audit.action} · ${DateTimeUtils.formatDateTime(audit.occurredAt)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.textSecondary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = audit.summary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textPrimary
                    )
                }
            }
        }
    }

    goalAmountDialogTarget?.let { goal ->
        var amountInput by remember(goal.id) {
            mutableStateOf(DateTimeUtils.centsToInputString(goal.currentAmountCents))
        }
        AlertDialog(
            onDismissRequest = { goalAmountDialogTarget = null },
            containerColor = palette.surfaceElevated,
            title = { Text("Atualizar progresso", color = palette.textPrimary) },
            text = {
                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { amountInput = it },
                    label = { Text("Valor acumulado (R$)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    val cents = DateTimeUtils.parseBrlToCents(amountInput) ?: 0L
                    onUpdateGoalAmount(goal, cents)
                    goalAmountDialogTarget = null
                }) {
                    Text("Salvar")
                }
            },
            dismissButton = {
                TextButton(onClick = { goalAmountDialogTarget = null }) { Text("Cancelar") }
            }
        )
    }
}
