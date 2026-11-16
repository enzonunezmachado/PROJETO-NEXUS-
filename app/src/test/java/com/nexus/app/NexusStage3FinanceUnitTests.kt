package com.nexus.app

import com.nexus.app.core.DateTimeUtils
import com.nexus.app.data.db.entity.BudgetEntity
import com.nexus.app.data.db.entity.CategoryEntity
import com.nexus.app.data.db.entity.TransactionEntity
import com.nexus.app.domain.model.CategoryScope
import com.nexus.app.domain.model.FinancialStatus
import com.nexus.app.domain.model.PaymentMethod
import com.nexus.app.domain.model.TransactionType
import com.nexus.app.domain.model.TransactionWithDetails
import com.nexus.app.domain.rules.CashflowSeriesMode
import com.nexus.app.domain.rules.CategorySpendScope
import com.nexus.app.domain.rules.FinanceRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class NexusStage3FinanceUnitTests {

    private fun txWithCategory(
        id: String,
        type: TransactionType,
        amountCents: Long,
        status: FinancialStatus,
        date: LocalDate,
        category: CategoryEntity? = null,
        paymentMethod: PaymentMethod = PaymentMethod.PIX,
        creditCardId: String? = null,
        statementId: String? = null
    ): TransactionWithDetails = TransactionWithDetails(
        transaction = TransactionEntity(
            id = id,
            type = type,
            description = "Tx $id",
            amountCents = amountCents,
            categoryId = category?.id,
            status = status,
            paymentMethod = paymentMethod,
            occurredAt = DateTimeUtils.startOfDayEpoch(date),
            dueAt = DateTimeUtils.startOfDayEpoch(date),
            settledAt = if (status == FinancialStatus.SETTLED) DateTimeUtils.startOfDayEpoch(date) else null,
            creditCardId = creditCardId,
            statementId = statementId,
            createdAt = 1L,
            updatedAt = 1L
        ),
        category = category,
        project = null,
        event = null,
        creditCard = null,
        task = null
    )

    @Test
    fun `computeCategorySpend groups past top 6 into Outros and switches PaidOnly vs PaidAndPlanned cleanly`() {
        val month = YearMonth.of(2026, 11)
        val today = LocalDate.of(2026, 11, 16)
        val cats = (1..8).map { i ->
            CategoryEntity(id = "c$i", name = "Cat $i", scope = CategoryScope.FINANCE, createdAt = 1L, updatedAt = 1L)
        }

        val items = listOf(
            txWithCategory("1", TransactionType.EXPENSE, 800_00L, FinancialStatus.SETTLED, LocalDate.of(2026, 11, 2), cats[0]),
            txWithCategory("2", TransactionType.EXPENSE, 700_00L, FinancialStatus.SETTLED, LocalDate.of(2026, 11, 3), cats[1]),
            txWithCategory("3", TransactionType.EXPENSE, 600_00L, FinancialStatus.SETTLED, LocalDate.of(2026, 11, 4), cats[2]),
            txWithCategory("4", TransactionType.EXPENSE, 500_00L, FinancialStatus.SETTLED, LocalDate.of(2026, 11, 5), cats[3]),
            txWithCategory("5", TransactionType.EXPENSE, 400_00L, FinancialStatus.SETTLED, LocalDate.of(2026, 11, 6), cats[4]),
            txWithCategory("6", TransactionType.EXPENSE, 300_00L, FinancialStatus.SETTLED, LocalDate.of(2026, 11, 7), cats[5]),
            txWithCategory("7", TransactionType.EXPENSE, 200_00L, FinancialStatus.SETTLED, LocalDate.of(2026, 11, 8), cats[6]),
            txWithCategory("8", TransactionType.EXPENSE, 100_00L, FinancialStatus.PENDING, LocalDate.of(2026, 11, 20), cats[7]),
            txWithCategory("9", TransactionType.EXPENSE, 999_00L, FinancialStatus.CANCELLED, LocalDate.of(2026, 11, 10), cats[0])
        )

        val paidOnly = FinanceRules.computeCategorySpend(items, month, CategorySpendScope.PAID_ONLY, today)
        assertEquals(7, paidOnly.size) // 6 + Outros (cats[6])
        assertEquals(FinanceRules.OTHERS_CATEGORY_ID, paidOnly.last().categoryId)
        assertEquals(200_00L, paidOnly.last().totalCents)
        assertEquals(3500_00L, paidOnly.sumOf { it.totalCents })

        val paidAndPlanned = FinanceRules.computeCategorySpend(items, month, CategorySpendScope.PAID_AND_PLANNED, today)
        assertEquals(7, paidAndPlanned.size) // 6 + Outros (cats[6] + cats[7])
        assertEquals(300_00L, paidAndPlanned.last().totalCents)
        assertEquals(3600_00L, paidAndPlanned.sumOf { it.totalCents })
    }

    @Test
    fun `computeSixMonthCashflow separates Realized from Planned without mixing`() {
        val endMonth = YearMonth.of(2026, 11)
        val today = LocalDate.of(2026, 11, 16)
        val txs = listOf(
            txWithCategory("inc-paid", TransactionType.INCOME, 5000_00L, FinancialStatus.SETTLED, LocalDate.of(2026, 11, 5)).transaction,
            txWithCategory("inc-plan", TransactionType.INCOME, 1200_00L, FinancialStatus.PLANNED, LocalDate.of(2026, 11, 28)).transaction,
            txWithCategory("exp-paid", TransactionType.EXPENSE, 2000_00L, FinancialStatus.SETTLED, LocalDate.of(2026, 11, 10)).transaction,
            txWithCategory("exp-pend", TransactionType.EXPENSE, 900_00L, FinancialStatus.PENDING, LocalDate.of(2026, 11, 22)).transaction,
            txWithCategory("exp-oct", TransactionType.EXPENSE, 400_00L, FinancialStatus.SETTLED, LocalDate.of(2026, 10, 15)).transaction
        )

        val realized = FinanceRules.computeSixMonthCashflow(txs, endMonth, CashflowSeriesMode.REALIZED, today)
        assertEquals(6, realized.size)
        val novRealized = realized.last()
        assertEquals("2026-11", novRealized.yearMonthIso)
        assertEquals(5000_00L, novRealized.receivedIncomeCents)
        assertEquals(2000_00L, novRealized.paidExpenseCents)
        assertEquals(400_00L, realized[4].paidExpenseCents)

        val planned = FinanceRules.computeSixMonthCashflow(txs, endMonth, CashflowSeriesMode.PLANNED, today)
        val novPlanned = planned.last()
        assertEquals(1200_00L, novPlanned.receivedIncomeCents)
        assertEquals(900_00L, novPlanned.paidExpenseCents)
    }

    @Test
    fun `computeForecast30Days and computeBudgetProgress calculate exact amounts and exceeded budgets`() {
        val today = LocalDate.of(2026, 11, 16)
        val month = YearMonth.of(2026, 11)
        val foodCat = CategoryEntity(id = "c-food", name = "Alimentação", scope = CategoryScope.FINANCE, createdAt = 1L, updatedAt = 1L)

        val items = listOf(
            txWithCategory("over", TransactionType.EXPENSE, 150_00L, FinancialStatus.PENDING, today.minusDays(2), foodCat),
            txWithCategory("d1", TransactionType.EXPENSE, 400_00L, FinancialStatus.PENDING, today.plusDays(3), foodCat),
            txWithCategory("d2", TransactionType.EXPENSE, 100_00L, FinancialStatus.SETTLED, today.plusDays(1), foodCat),
            txWithCategory("d3", TransactionType.EXPENSE, 800_00L, FinancialStatus.PENDING, today.plusDays(10), foodCat),
            txWithCategory("d4", TransactionType.EXPENSE, 900_00L, FinancialStatus.PENDING, today.plusDays(40), foodCat),
            txWithCategory("inc", TransactionType.INCOME, 2500_00L, FinancialStatus.PLANNED, today.plusDays(5))
        )

        val forecast = FinanceRules.computeForecast30Days(items, today)
        // Window [today .. today+30] unsettled expenses = d1 (400) + d3 (800) = 1200 (excludes past 'over', settled 'd2', >30d 'd4')
        assertEquals(1200_00L, forecast.upcomingExpensesCents)
        assertEquals(2500_00L, forecast.plannedIncomeCents)
        assertEquals(listOf("d3", "d1"), forecast.topUpcomingExpenses.map { it.transaction.id })

        val budget = BudgetEntity(id = "b1", categoryId = "c-food", yearMonth = "2026-11", limitCents = 1000_00L, createdAt = 1L, updatedAt = 1L)
        val dashboard = FinanceRules.computeMonthlyDashboardData(
            declaredBalanceCents = 3000_00L,
            transactions = items,
            budgets = listOf(budget),
            categories = listOf(foodCat),
            targetMonth = month,
            today = today
        )
        // Food expenses in 2026-11: over (150) + d1 (400) + d2 (100) + d3 (800) = 1450 (d4 is in Dec)
        val bp = dashboard.budgetProgress.single()
        assertEquals(1450_00L, bp.spentCents)
        assertEquals(450_00L, bp.exceededCents)
        assertEquals(145, bp.usedPercent)
        // Month result = receivedIncome (0) - paidExpense (100) = -100, distinct from declaredBalance (3000)
        assertEquals(-100_00L, dashboard.monthResultCents)
        assertEquals(3000_00L, dashboard.declaredBalanceCents)
        assertTrue(dashboard.forecast30Days.topUpcomingExpenses.isNotEmpty())
    }
}
