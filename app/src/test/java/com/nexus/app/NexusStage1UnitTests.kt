package com.nexus.app

import com.nexus.app.core.DateTimeUtils
import com.nexus.app.data.db.entity.ReminderEntity
import com.nexus.app.data.db.entity.TransactionEntity
import com.nexus.app.domain.model.FinancialStatus
import com.nexus.app.domain.model.ReminderCategory
import com.nexus.app.domain.model.ReminderSourceType
import com.nexus.app.domain.model.ReminderStatus
import com.nexus.app.domain.model.TransactionType
import com.nexus.app.domain.rules.BackAction
import com.nexus.app.domain.rules.BackNavigationState
import com.nexus.app.domain.rules.BackSubRouteKind
import com.nexus.app.domain.rules.BackTabKind
import com.nexus.app.domain.rules.FinanceRules
import com.nexus.app.domain.rules.ReminderRules
import com.nexus.app.domain.rules.ReminderSyncAction
import com.nexus.app.domain.rules.resolveBackAction
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneOffset
import java.util.TimeZone

class NexusStage1UnitTests {

    private var savedDefaultTimeZone: TimeZone? = null

    @Before
    fun captureDefaultTimeZone() {
        savedDefaultTimeZone = TimeZone.getDefault()
    }

    @After
    fun restoreDefaultTimeZone() {
        savedDefaultTimeZone?.let { TimeZone.setDefault(it) }
    }

    // 1. Voltar (BackHandler + resolveBackAction)
    @Test
    fun `back action closes clean editor before subroutes or tabs`() {
        val action = resolveBackAction(
            BackNavigationState(
                hasOpenEditor = true,
                editorHasUnsavedChanges = false,
                subRouteKind = BackSubRouteKind.MORE_CHILD_SUBROUTE,
                currentTab = BackTabKind.OTHER
            )
        )
        assertEquals(BackAction.CLOSE_EDITOR, action)
    }

    @Test
    fun `back action asks confirmation when open editor has unsaved changes`() {
        val action = resolveBackAction(
            BackNavigationState(
                hasOpenEditor = true,
                editorHasUnsavedChanges = true,
                subRouteKind = BackSubRouteKind.TOP_LEVEL_SUBROUTE,
                currentTab = BackTabKind.OTHER
            )
        )
        assertEquals(BackAction.CONFIRM_DISCARD_EDITOR, action)
    }

    @Test
    fun `back action returns from deep subscreen to MoreMenu`() {
        val action = resolveBackAction(
            BackNavigationState(
                hasOpenEditor = false,
                editorHasUnsavedChanges = false,
                subRouteKind = BackSubRouteKind.MORE_CHILD_SUBROUTE,
                currentTab = BackTabKind.OTHER
            )
        )
        assertEquals(BackAction.RETURN_TO_MORE_MENU, action)
    }

    @Test
    fun `back action returns from top-level subscreen to current tab`() {
        val action = resolveBackAction(
            BackNavigationState(
                hasOpenEditor = false,
                editorHasUnsavedChanges = false,
                subRouteKind = BackSubRouteKind.TOP_LEVEL_SUBROUTE,
                currentTab = BackTabKind.OTHER
            )
        )
        assertEquals(BackAction.RETURN_FROM_SUBROUTE, action)
    }

    @Test
    fun `back action returns from non-home tab to home`() {
        val action = resolveBackAction(
            BackNavigationState(
                hasOpenEditor = false,
                editorHasUnsavedChanges = false,
                subRouteKind = BackSubRouteKind.NONE,
                currentTab = BackTabKind.OTHER
            )
        )
        assertEquals(BackAction.RETURN_TO_HOME_TAB, action)
    }

    @Test
    fun `back action allows system exit only on home with nothing open`() {
        val action = resolveBackAction(
            BackNavigationState(
                hasOpenEditor = false,
                editorHasUnsavedChanges = false,
                subRouteKind = BackSubRouteKind.NONE,
                currentTab = BackTabKind.HOME
            )
        )
        assertEquals(BackAction.ALLOW_SYSTEM_EXIT, action)
    }

    // 2. Seletor de data (DateTimeUtils 2.1)
    @Test
    fun `picking 20 november in date picker produces 2026-11-20 and reopens on 20 november`() {
        val targetDate = LocalDate.of(2026, 11, 20)
        val pickerUtcMillis = targetDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

        val savedEpoch = DateTimeUtils.datePickerMillisToEpoch(pickerUtcMillis, keepTimeFrom = null)
        assertEquals("2026-11-20", DateTimeUtils.toLocalDate(savedEpoch).toString())
        assertEquals(DateTimeUtils.startOfDayEpoch(targetDate), savedEpoch)

        val reopenedPickerMillis = DateTimeUtils.epochToDatePickerMillis(savedEpoch)
        val reopenedUtcDate = Instant.ofEpochMilli(reopenedPickerMillis).atZone(ZoneOffset.UTC).toLocalDate()
        assertEquals(targetDate, reopenedUtcDate)
    }

    @Test
    fun `date picker conversions preserve day on round trip at 23h30 and 00h10 without shifting day`() {
        val originalTz = TimeZone.getDefault()
        try {
            // Garante independência do fuso padrão da JVM (ex.: runner em UTC vs device em São Paulo)
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"))

            val date = LocalDate.of(2026, 11, 20)
            val lateNightEpoch = DateTimeUtils.localDateTimeToEpoch(date, LocalTime.of(23, 30))
            val earlyMorningEpoch = DateTimeUtils.localDateTimeToEpoch(date, LocalTime.of(0, 10))

            assertEquals("2026-11-20", DateTimeUtils.toLocalDate(lateNightEpoch).toString())
            assertEquals("2026-11-20", DateTimeUtils.toLocalDate(earlyMorningEpoch).toString())

            val latePickerMillis = DateTimeUtils.epochToDatePickerMillis(lateNightEpoch)
            val earlyPickerMillis = DateTimeUtils.epochToDatePickerMillis(earlyMorningEpoch)

            assertEquals(date, Instant.ofEpochMilli(latePickerMillis).atZone(ZoneOffset.UTC).toLocalDate())
            assertEquals(date, Instant.ofEpochMilli(earlyPickerMillis).atZone(ZoneOffset.UTC).toLocalDate())

            val roundTripLate = DateTimeUtils.datePickerMillisToEpoch(latePickerMillis, keepTimeFrom = lateNightEpoch)
            val roundTripEarly = DateTimeUtils.datePickerMillisToEpoch(earlyPickerMillis, keepTimeFrom = earlyMorningEpoch)

            assertEquals(lateNightEpoch, roundTripLate)
            assertEquals(earlyMorningEpoch, roundTripEarly)
            assertEquals(LocalTime.of(23, 30), DateTimeUtils.epochToLocalTime(roundTripLate))
            assertEquals(LocalTime.of(0, 10), DateTimeUtils.epochToLocalTime(roundTripEarly))
        } finally {
            TimeZone.setDefault(originalTz)
        }
    }

    @Test
    fun `date picker without time stores startOfDayEpoch in Sao Paulo`() {
        val date = LocalDate.of(2026, 11, 20)
        val pickerMillis = DateTimeUtils.epochToDatePickerMillis(DateTimeUtils.startOfDayEpoch(date))
        val resultEpoch = DateTimeUtils.datePickerMillisToEpoch(pickerMillis, keepTimeFrom = null)

        assertEquals(DateTimeUtils.startOfDayEpoch(date), resultEpoch)
        assertEquals(LocalTime.MIDNIGHT, DateTimeUtils.epochToLocalTime(resultEpoch))
        assertEquals(date, DateTimeUtils.toLocalDate(resultEpoch))
    }

    // 3. Totais do Financeiro por mês
    @Test
    fun `monthly finance summary filters current month totals and keeps overdue across all dates`() {
        val today = LocalDate.of(2026, 11, 15)
        val currentMonth = YearMonth.of(2026, 11)

        fun tx(
            id: String,
            type: TransactionType,
            status: FinancialStatus,
            amountCents: Long,
            dueDate: LocalDate?,
            occurredDate: LocalDate = today
        ): TransactionEntity = TransactionEntity(
            id = id,
            type = type,
            description = id,
            amountCents = amountCents,
            status = status,
            occurredAt = DateTimeUtils.localDateTimeToEpoch(occurredDate, LocalTime.NOON),
            dueAt = dueDate?.let { DateTimeUtils.localDateTimeToEpoch(it, LocalTime.NOON) },
            createdAt = 1L,
            updatedAt = 1L
        )

        val items = listOf(
            // Mês atual (nov/2026)
            tx("inc-settled-nov", TransactionType.INCOME, FinancialStatus.SETTLED, 500_00L, LocalDate.of(2026, 11, 5)),
            tx("inc-planned-nov", TransactionType.INCOME, FinancialStatus.PLANNED, 120_00L, LocalDate.of(2026, 11, 25)),
            tx("exp-paid-nov", TransactionType.EXPENSE, FinancialStatus.SETTLED, 200_00L, LocalDate.of(2026, 11, 10)),
            tx("exp-pending-nov", TransactionType.EXPENSE, FinancialStatus.PENDING, 80_00L, LocalDate.of(2026, 11, 20)),
            tx("exp-overdue-nov", TransactionType.EXPENSE, FinancialStatus.PENDING, 40_00L, LocalDate.of(2026, 11, 2)),
            tx("exp-cancelled-nov", TransactionType.EXPENSE, FinancialStatus.CANCELLED, 999_00L, LocalDate.of(2026, 11, 12)),
            // Outubro/2026 (não deve entrar nos totais do mês, exceto se vencida em aberto)
            tx("inc-settled-oct", TransactionType.INCOME, FinancialStatus.SETTLED, 900_00L, LocalDate.of(2026, 10, 15)),
            tx("exp-paid-oct", TransactionType.EXPENSE, FinancialStatus.SETTLED, 350_00L, LocalDate.of(2026, 10, 18)),
            tx("exp-overdue-oct", TransactionType.EXPENSE, FinancialStatus.PENDING, 60_00L, LocalDate.of(2026, 10, 25)),
            // Dezembro/2026 (futuro, não entra em nov/2026)
            tx("inc-planned-dec", TransactionType.INCOME, FinancialStatus.PLANNED, 400_00L, LocalDate.of(2026, 12, 5)),
            tx("exp-pending-dec", TransactionType.EXPENSE, FinancialStatus.PENDING, 150_00L, LocalDate.of(2026, 12, 10))
        )

        val summary = FinanceRules.computeMonthlyFinanceSummary(
            transactions = items,
            targetMonth = currentMonth,
            today = today
        )

        assertEquals(500_00L, summary.receivedIncomeCents)
        assertEquals(120_00L, summary.plannedIncomeCents)
        assertEquals(200_00L, summary.paidExpenseCents)
        assertEquals(80_00L, summary.pendingExpenseCents)
        assertEquals(100_00L, summary.overdueExpenseCents)
        assertEquals(2, summary.overdueCount)
    }

    // 4. Limite do cartão (calculado + informado)
    @Test
    fun `credit card limit breakdown shows declared limit only when positive and different from computed`() {
        val different = FinanceRules.resolveCreditCardLimitBreakdown(
            totalLimitCents = 5_000_00L,
            computedRemainingLimitCents = 3_200_00L,
            declaredAvailableLimitCents = 2_950_00L
        )
        assertEquals(3_200_00L, different.computedAvailableCents)
        assertEquals(2_950_00L, different.declaredAvailableCents)

        val equal = FinanceRules.resolveCreditCardLimitBreakdown(
            totalLimitCents = 5_000_00L,
            computedRemainingLimitCents = 3_200_00L,
            declaredAvailableLimitCents = 3_200_00L
        )
        assertEquals(3_200_00L, equal.computedAvailableCents)
        assertNull(equal.declaredAvailableCents)

        val absent = FinanceRules.resolveCreditCardLimitBreakdown(
            totalLimitCents = 5_000_00L,
            computedRemainingLimitCents = -100_00L,
            declaredAvailableLimitCents = 0L
        )
        assertEquals(0L, absent.computedAvailableCents)
        assertNull(absent.declaredAvailableCents)
    }

    // 5. Lembretes nos editores
    private fun sampleReminder(
        minutesBefore: Int,
        status: ReminderStatus,
        dueAt: Long
    ): ReminderEntity = ReminderEntity(
        id = "rem-1",
        sourceType = ReminderSourceType.TASK,
        sourceId = "task-1",
        category = ReminderCategory.SCHEDULE,
        title = "Teste",
        message = "Mensagem",
        triggerAt = dueAt - minutesBefore * 60_000L,
        originalDueAt = dueAt,
        minutesBefore = minutesBefore,
        channelId = "schedule",
        status = status,
        createdAt = 1L,
        updatedAt = 1L
    )

    @Test
    fun `editor loads real minutesBefore or -1 when cancelled or legacy negative`() {
        val due = 1_795_000_000_000L
        assertNull(ReminderRules.resolveEditorReminderMinutes(null))
        assertEquals(15, ReminderRules.resolveEditorReminderMinutes(sampleReminder(15, ReminderStatus.ACTIVE, due)))
        assertEquals(120, ReminderRules.resolveEditorReminderMinutes(sampleReminder(120, ReminderStatus.FIRED, due)))
        assertEquals(-1, ReminderRules.resolveEditorReminderMinutes(sampleReminder(30, ReminderStatus.CANCELLED, due)))
        assertEquals(-1, ReminderRules.resolveEditorReminderMinutes(sampleReminder(-1, ReminderStatus.ACTIVE, due)))
    }

    @Test
    fun `saving without touching reminder preserves existing reminder state and custom minutes`() {
        val due = 1_795_000_000_000L
        val active = sampleReminder(120, ReminderStatus.FIRED, due)
        val cancelledWithOriginalMinutes = sampleReminder(30, ReminderStatus.CANCELLED, due)
        val legacyNegative = sampleReminder(-1, ReminderStatus.ACTIVE, due)

        assertEquals(
            ReminderSyncAction.PRESERVE_UNTOUCHED,
            ReminderRules.resolveReminderSyncAction(active, due, overrideMinutesBefore = null)
        )
        assertEquals(
            ReminderSyncAction.PRESERVE_UNTOUCHED,
            ReminderRules.resolveReminderSyncAction(cancelledWithOriginalMinutes, due + 3_600_000L, overrideMinutesBefore = null)
        )
        assertEquals(
            ReminderSyncAction.PRESERVE_UNTOUCHED,
            ReminderRules.resolveReminderSyncAction(legacyNegative, due + 3_600_000L, overrideMinutesBefore = null)
        )
        assertEquals(
            ReminderSyncAction.RESCHEDULE,
            ReminderRules.resolveReminderSyncAction(active, due + 3_600_000L, overrideMinutesBefore = null)
        )
        assertEquals(
            ReminderSyncAction.DISABLE,
            ReminderRules.resolveReminderSyncAction(active, due, overrideMinutesBefore = -1)
        )
        assertEquals(
            ReminderSyncAction.RESCHEDULE,
            ReminderRules.resolveReminderSyncAction(active, due, overrideMinutesBefore = 15)
        )
    }

    @Test
    fun `disabled reminder preserves original minutesBefore and cancelled or legacy rows stay out of queue and alarms`() {
        val due = 1_795_000_000_000L
        val existingCustom = sampleReminder(45, ReminderStatus.ACTIVE, due)
        val existingZero = sampleReminder(0, ReminderStatus.ACTIVE, due)
        val legacyNegative = sampleReminder(-1, ReminderStatus.ACTIVE, due)
        val cancelled = sampleReminder(30, ReminderStatus.CANCELLED, due)
        val dismissed = sampleReminder(15, ReminderStatus.DISMISSED, due)
        val snoozed = sampleReminder(15, ReminderStatus.ACTIVE, due).copy(snoozedUntil = due + 900_000L)

        assertEquals(45, ReminderRules.resolveDisabledMinutesBefore(existingCustom, fallbackDefaultMinutes = 15))
        assertEquals(0, ReminderRules.resolveDisabledMinutesBefore(existingZero, fallbackDefaultMinutes = 15))
        assertEquals(15, ReminderRules.resolveDisabledMinutesBefore(legacyNegative, fallbackDefaultMinutes = 15))
        assertEquals(15, ReminderRules.resolveDisabledMinutesBefore(null, fallbackDefaultMinutes = 15))

        assertTrue(ReminderRules.isEffectivelyCancelled(cancelled))
        assertTrue(ReminderRules.isEffectivelyCancelled(legacyNegative))
        assertFalse(ReminderRules.isEffectivelyCancelled(existingCustom))

        assertFalse(ReminderRules.isQueueVisible(cancelled))
        assertFalse(ReminderRules.isQueueVisible(legacyNegative))
        assertTrue(ReminderRules.isQueueVisible(existingCustom))
        assertTrue(ReminderRules.isQueueVisible(dismissed))

        assertFalse(ReminderRules.isSchedulableOrFirable(cancelled))
        assertFalse(ReminderRules.isSchedulableOrFirable(legacyNegative))
        assertFalse(ReminderRules.isSchedulableOrFirable(dismissed))
        assertTrue(ReminderRules.isSchedulableOrFirable(existingCustom))
        assertTrue(ReminderRules.isSchedulableOrFirable(snoozed))
    }
}
