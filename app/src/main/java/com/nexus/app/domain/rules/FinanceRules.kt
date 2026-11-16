package com.nexus.app.domain.rules

import com.nexus.app.core.DateTimeUtils
import com.nexus.app.data.db.entity.BudgetEntity
import com.nexus.app.data.db.entity.CategoryEntity
import com.nexus.app.data.db.entity.TransactionEntity
import com.nexus.app.domain.model.BudgetProgress
import com.nexus.app.domain.model.CategorySpendPoint
import com.nexus.app.domain.model.FinancialStatus
import com.nexus.app.domain.model.MonthlyCashflowPoint
import com.nexus.app.domain.model.TransactionType
import com.nexus.app.domain.model.TransactionWithDetails
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

data class MonthlyFinanceSummary(
    val receivedIncomeCents: Long = 0L,
    val plannedIncomeCents: Long = 0L,
    val paidExpenseCents: Long = 0L,
    val pendingExpenseCents: Long = 0L,
    val overdueExpenseCents: Long = 0L,
    val overdueCount: Int = 0
)

data class CreditCardLimitBreakdown(
    val totalLimitCents: Long,
    val computedAvailableCents: Long,
    val declaredAvailableCents: Long?
)

enum class CategorySpendScope(val label: String) {
    PAID_ONLY("Pagas"),
    PAID_AND_PLANNED("Pagas + previstas")
}

enum class CashflowSeriesMode(val label: String) {
    REALIZED("Realizado"),
    PLANNED("Previsto")
}

data class Forecast30DaysSummary(
    val upcomingExpensesCents: Long = 0L,
    val plannedIncomeCents: Long = 0L,
    val topUpcomingExpenses: List<TransactionWithDetails> = emptyList()
)

data class MonthlyFinanceDashboardData(
    val month: YearMonth = YearMonth.from(DateTimeUtils.todayLocalDate()),
    val declaredBalanceCents: Long = 0L,
    val monthlySummary: MonthlyFinanceSummary = MonthlyFinanceSummary(),
    val categorySpendPaidOnly: List<CategorySpendPoint> = emptyList(),
    val categorySpendPaidAndPlanned: List<CategorySpendPoint> = emptyList(),
    val sixMonthCashflowRealized: List<MonthlyCashflowPoint> = emptyList(),
    val sixMonthCashflowPlanned: List<MonthlyCashflowPoint> = emptyList(),
    val forecast30Days: Forecast30DaysSummary = Forecast30DaysSummary(),
    val budgetProgress: List<BudgetProgress> = emptyList(),
    val nextDueTransaction: TransactionWithDetails? = null
) {
    val monthResultCents: Long
        get() = monthlySummary.receivedIncomeCents - monthlySummary.paidExpenseCents
}

object FinanceRules {

    const val OTHERS_CATEGORY_ID = "__OTHERS__"
    private const val OTHERS_COLOR_HEX = "#7A8699"
    private val monthChartFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("MM/yy", DateTimeUtils.LOCALE_PT_BR)

    fun effectiveTransactionStatus(
        tx: TransactionEntity,
        today: LocalDate = DateTimeUtils.todayLocalDate()
    ): TransactionEntity {
        if (tx.status == FinancialStatus.SETTLED || tx.status == FinancialStatus.CANCELLED) {
            return tx
        }
        val due = tx.dueAt ?: return tx
        val startOfToday = DateTimeUtils.startOfDayEpoch(today)
        return if (due < startOfToday) {
            tx.copy(status = FinancialStatus.OVERDUE)
        } else {
            tx
        }
    }

    /**
     * Calcula os totais financeiros mensais de forma pura e compartilhada entre
     * FinanceScreen, HomeScreen e FinanceGlanceWidget:
     * - receivedIncomeCents, plannedIncomeCents, paidExpenseCents e pendingExpenseCents
     *   filtram somente transações cuja data de referência (dueAt ?: occurredAt) pertence a [targetMonth].
     * - overdueExpenseCents (e overdueCount) consideram TODAS as datas.
     */
    fun computeMonthlyFinanceSummary(
        transactions: List<TransactionEntity>,
        targetMonth: YearMonth,
        today: LocalDate = DateTimeUtils.todayLocalDate()
    ): MonthlyFinanceSummary {
        val activeTx = transactions
            .map { effectiveTransactionStatus(it, today) }
            .filter { it.status != FinancialStatus.CANCELLED }

        val monthTx = activeTx.filter { tx ->
            val refEpoch = tx.dueAt ?: tx.occurredAt
            YearMonth.from(DateTimeUtils.toLocalDate(refEpoch)) == targetMonth
        }

        val receivedIncome = monthTx
            .filter { it.type == TransactionType.INCOME && it.status == FinancialStatus.SETTLED }
            .sumOf { it.amountCents }

        val plannedIncome = monthTx
            .filter {
                it.type == TransactionType.INCOME &&
                    (it.status == FinancialStatus.PLANNED || it.status == FinancialStatus.PENDING)
            }
            .sumOf { it.amountCents }

        val paidExpense = monthTx
            .filter { it.type == TransactionType.EXPENSE && it.status == FinancialStatus.SETTLED }
            .sumOf { it.amountCents }

        val pendingExpense = monthTx
            .filter {
                it.type == TransactionType.EXPENSE &&
                    (it.status == FinancialStatus.PENDING || it.status == FinancialStatus.PLANNED)
            }
            .sumOf { it.amountCents }

        val overdueExpenses = activeTx.filter {
            it.type == TransactionType.EXPENSE && it.status == FinancialStatus.OVERDUE
        }

        return MonthlyFinanceSummary(
            receivedIncomeCents = receivedIncome,
            plannedIncomeCents = plannedIncome,
            paidExpenseCents = paidExpense,
            pendingExpenseCents = pendingExpense,
            overdueExpenseCents = overdueExpenses.sumOf { it.amountCents },
            overdueCount = overdueExpenses.size
        )
    }

    fun computeCategorySpend(
        transactions: List<TransactionWithDetails>,
        targetMonth: YearMonth,
        scope: CategorySpendScope,
        today: LocalDate = DateTimeUtils.todayLocalDate()
    ): List<CategorySpendPoint> {
        val filtered = transactions
            .map { it.copy(transaction = effectiveTransactionStatus(it.transaction, today)) }
            .filter { item ->
                val tx = item.transaction
                if (tx.type != TransactionType.EXPENSE) return@filter false
                if (tx.status == FinancialStatus.CANCELLED) return@filter false
                if (scope == CategorySpendScope.PAID_ONLY && tx.status != FinancialStatus.SETTLED) return@filter false
                val refEpoch = tx.dueAt ?: tx.occurredAt
                YearMonth.from(DateTimeUtils.toLocalDate(refEpoch)) == targetMonth
            }

        val grouped = filtered
            .groupBy { it.category?.id to (it.category?.name ?: "Sem categoria") }
            .map { (key, list) ->
                CategorySpendPoint(
                    categoryId = key.first,
                    categoryName = key.second,
                    colorHex = list.firstOrNull()?.category?.colorHex ?: "#9A9A9A",
                    totalCents = list.sumOf { it.transaction.amountCents }
                )
            }
            .sortedByDescending { it.totalCents }

        val topSix = grouped.take(6)
        val othersTotal = grouped.drop(6).sumOf { it.totalCents }
        return if (othersTotal > 0L) {
            topSix + CategorySpendPoint(
                categoryId = OTHERS_CATEGORY_ID,
                categoryName = "Outros",
                colorHex = OTHERS_COLOR_HEX,
                totalCents = othersTotal
            )
        } else {
            topSix
        }
    }

    fun computeSixMonthCashflow(
        transactions: List<TransactionEntity>,
        endMonth: YearMonth,
        mode: CashflowSeriesMode,
        today: LocalDate = DateTimeUtils.todayLocalDate()
    ): List<MonthlyCashflowPoint> {
        val activeTx = transactions
            .map { effectiveTransactionStatus(it, today) }
            .filter { it.status != FinancialStatus.CANCELLED }

        return (5 downTo 0).map { offset ->
            val ym = endMonth.minusMonths(offset.toLong())
            val monthItems = activeTx.filter { tx ->
                val refEpoch = tx.dueAt ?: tx.occurredAt
                YearMonth.from(DateTimeUtils.toLocalDate(refEpoch)) == ym
            }
            val (income, expense) = when (mode) {
                CashflowSeriesMode.REALIZED -> {
                    monthItems.filter { it.type == TransactionType.INCOME && it.status == FinancialStatus.SETTLED }
                        .sumOf { it.amountCents } to
                        monthItems.filter { it.type == TransactionType.EXPENSE && it.status == FinancialStatus.SETTLED }
                            .sumOf { it.amountCents }
                }
                CashflowSeriesMode.PLANNED -> {
                    monthItems.filter { it.type == TransactionType.INCOME && it.status != FinancialStatus.SETTLED }
                        .sumOf { it.amountCents } to
                        monthItems.filter { it.type == TransactionType.EXPENSE && it.status != FinancialStatus.SETTLED }
                            .sumOf { it.amountCents }
                }
            }
            MonthlyCashflowPoint(
                yearMonthLabel = ym.format(monthChartFormatter),
                receivedIncomeCents = income,
                paidExpenseCents = expense,
                yearMonthIso = ym.toString()
            )
        }
    }

    fun computeForecast30Days(
        transactions: List<TransactionWithDetails>,
        today: LocalDate = DateTimeUtils.todayLocalDate()
    ): Forecast30DaysSummary {
        val horizon = today.plusDays(30)
        val openWindow = transactions
            .map { it.copy(transaction = effectiveTransactionStatus(it.transaction, today)) }
            .filter { item ->
                val tx = item.transaction
                if (tx.status == FinancialStatus.SETTLED || tx.status == FinancialStatus.CANCELLED) return@filter false
                val refDate = DateTimeUtils.toLocalDate(tx.dueAt ?: tx.occurredAt)
                !refDate.isBefore(today) && !refDate.isAfter(horizon)
            }

        val upcomingExpenses = openWindow
            .filter { it.transaction.type == TransactionType.EXPENSE }
            .sortedWith(
                compareByDescending<TransactionWithDetails> { it.transaction.amountCents }
                    .thenBy { it.transaction.dueAt ?: it.transaction.occurredAt }
            )

        val plannedIncomeCents = openWindow
            .filter { it.transaction.type == TransactionType.INCOME }
            .sumOf { it.transaction.amountCents }

        return Forecast30DaysSummary(
            upcomingExpensesCents = upcomingExpenses.sumOf { it.transaction.amountCents },
            plannedIncomeCents = plannedIncomeCents,
            topUpcomingExpenses = upcomingExpenses.take(5)
        )
    }

    fun computeBudgetProgress(
        budgets: List<BudgetEntity>,
        categories: List<CategoryEntity>,
        transactions: List<TransactionEntity>,
        targetMonth: YearMonth
    ): List<BudgetProgress> {
        val monthStr = targetMonth.toString()
        val catMap = categories.associateBy { it.id }
        val spentByCategory = transactions
            .filter { tx ->
                tx.type == TransactionType.EXPENSE &&
                    tx.status != FinancialStatus.CANCELLED &&
                    YearMonth.from(DateTimeUtils.toLocalDate(tx.dueAt ?: tx.occurredAt)) == targetMonth
            }
            .groupBy { it.categoryId }
            .mapValues { (_, list) -> list.sumOf { it.amountCents } }

        return budgets
            .filter { it.yearMonth == monthStr }
            .mapNotNull { budget ->
                val cat = catMap[budget.categoryId] ?: return@mapNotNull null
                BudgetProgress(
                    budget = budget,
                    category = cat,
                    spentCents = spentByCategory[budget.categoryId] ?: 0L
                )
            }
            .sortedByDescending { it.usedPercent }
    }

    fun computeMonthlyDashboardData(
        declaredBalanceCents: Long,
        transactions: List<TransactionWithDetails>,
        budgets: List<BudgetEntity>,
        categories: List<CategoryEntity>,
        targetMonth: YearMonth,
        today: LocalDate = DateTimeUtils.todayLocalDate()
    ): MonthlyFinanceDashboardData {
        val rawTx = transactions.map { it.transaction }
        val activeWithDetails = transactions
            .map { it.copy(transaction = effectiveTransactionStatus(it.transaction, today)) }
            .filter { it.transaction.status != FinancialStatus.CANCELLED }

        val startOfToday = DateTimeUtils.startOfDayEpoch(today)
        val nextDue = activeWithDetails
            .filter {
                it.transaction.type == TransactionType.EXPENSE &&
                    it.transaction.status != FinancialStatus.SETTLED &&
                    (it.transaction.dueAt ?: it.transaction.occurredAt) >= startOfToday
            }
            .minByOrNull { it.transaction.dueAt ?: it.transaction.occurredAt }
            ?: activeWithDetails
                .filter {
                    it.transaction.type == TransactionType.EXPENSE &&
                        it.transaction.status == FinancialStatus.OVERDUE
                }
                .minByOrNull { it.transaction.dueAt ?: it.transaction.occurredAt }

        return MonthlyFinanceDashboardData(
            month = targetMonth,
            declaredBalanceCents = declaredBalanceCents,
            monthlySummary = computeMonthlyFinanceSummary(rawTx, targetMonth, today),
            categorySpendPaidOnly = computeCategorySpend(transactions, targetMonth, CategorySpendScope.PAID_ONLY, today),
            categorySpendPaidAndPlanned = computeCategorySpend(transactions, targetMonth, CategorySpendScope.PAID_AND_PLANNED, today),
            sixMonthCashflowRealized = computeSixMonthCashflow(rawTx, targetMonth, CashflowSeriesMode.REALIZED, today),
            sixMonthCashflowPlanned = computeSixMonthCashflow(rawTx, targetMonth, CashflowSeriesMode.PLANNED, today),
            forecast30Days = computeForecast30Days(transactions, today),
            budgetProgress = computeBudgetProgress(budgets, categories, rawTx, targetMonth),
            nextDueTransaction = nextDue
        )
    }

    fun resolveCreditCardLimitBreakdown(
        totalLimitCents: Long,
        computedRemainingLimitCents: Long,
        declaredAvailableLimitCents: Long
    ): CreditCardLimitBreakdown {
        val computed = computedRemainingLimitCents.coerceAtLeast(0L)
        val declared = declaredAvailableLimitCents
            .takeIf { it > 0L && it != computed }
        return CreditCardLimitBreakdown(
            totalLimitCents = totalLimitCents,
            computedAvailableCents = computed,
            declaredAvailableCents = declared
        )
    }
}
