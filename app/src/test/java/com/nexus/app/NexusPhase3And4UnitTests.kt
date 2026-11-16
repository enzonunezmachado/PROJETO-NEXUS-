package com.nexus.app

import com.nexus.app.core.DateTimeUtils
import com.nexus.app.data.backup.NexusBackupPayload
import com.nexus.app.data.db.entity.CreditCardEntity
import com.nexus.app.data.db.entity.CreditCardStatementEntity
import com.nexus.app.data.db.entity.EventEntity
import com.nexus.app.data.db.entity.ProjectEntity
import com.nexus.app.data.db.entity.RoutineAssistantSettingsEntity
import com.nexus.app.data.db.entity.TransactionEntity
import com.nexus.app.data.repository.NexusRepository
import com.nexus.app.domain.model.BriefingType
import com.nexus.app.domain.model.CardWithStatements
import com.nexus.app.domain.model.EventWithDetails
import com.nexus.app.domain.model.FinancialStatus
import com.nexus.app.domain.model.PaymentMethod
import com.nexus.app.domain.model.ProjectWithProgress
import com.nexus.app.domain.model.ReminderCategory
import com.nexus.app.domain.model.ReminderSourceType
import com.nexus.app.domain.model.ReminderStatus
import com.nexus.app.domain.model.StatementStatus
import com.nexus.app.domain.model.TransactionType
import com.nexus.app.domain.model.TransactionWithDetails
import com.nexus.app.notifications.NexusNotificationScheduler
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class NexusPhase3And4UnitTests {

    private val tolerantJson = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
        isLenient = true
    }

    @Test
    fun `computeReminderTriggerEpoch respects timed items vs date-only 09h00 anchor`() {
        val date = LocalDate.of(2026, 8, 20)
        val timedDue = DateTimeUtils.localDateTimeToEpoch(date, LocalTime.of(14, 30))
        val timedTrigger = NexusNotificationScheduler.computeReminderTriggerEpoch(
            dueEpoch = timedDue,
            hasSpecificTime = true,
            minutesBefore = 30
        )
        assertEquals(
            DateTimeUtils.localDateTimeToEpoch(date, LocalTime.of(14, 0)),
            timedTrigger
        )

        val dateOnlyDue = DateTimeUtils.localDateTimeToEpoch(date, LocalTime.of(23, 59))
        val dateOnlyTrigger = NexusNotificationScheduler.computeReminderTriggerEpoch(
            dueEpoch = dateOnlyDue,
            hasSpecificTime = false,
            minutesBefore = 15
        )
        assertEquals(
            DateTimeUtils.localDateTimeToEpoch(date, LocalTime.of(8, 45)),
            dateOnlyTrigger
        )
    }

    @Test
    fun `adjustForQuietHours handles disabled, daytime window and overnight crossing midnight`() {
        val date = LocalDate.of(2026, 8, 20)
        val nightTrigger = DateTimeUtils.localDateTimeToEpoch(date, LocalTime.of(23, 15))
        val earlyMorningTrigger = DateTimeUtils.localDateTimeToEpoch(date, LocalTime.of(5, 45))
        val afternoonTrigger = DateTimeUtils.localDateTimeToEpoch(date, LocalTime.of(15, 0))

        val disabled = RoutineAssistantSettingsEntity(
            quietHoursEnabled = false,
            quietHoursStartHour = 22,
            quietHoursStartMinute = 0,
            quietHoursEndHour = 7,
            quietHoursEndMinute = 0,
            createdAt = 1L,
            updatedAt = 1L
        )
        assertEquals(nightTrigger, NexusNotificationScheduler.adjustForQuietHours(nightTrigger, disabled))

        val overnight = disabled.copy(quietHoursEnabled = true)
        assertEquals(
            DateTimeUtils.localDateTimeToEpoch(date.plusDays(1), LocalTime.of(7, 0)),
            NexusNotificationScheduler.adjustForQuietHours(nightTrigger, overnight)
        )
        assertEquals(
            DateTimeUtils.localDateTimeToEpoch(date, LocalTime.of(7, 0)),
            NexusNotificationScheduler.adjustForQuietHours(earlyMorningTrigger, overnight)
        )
        assertEquals(
            afternoonTrigger,
            NexusNotificationScheduler.adjustForQuietHours(afternoonTrigger, overnight)
        )

        val daytime = disabled.copy(
            quietHoursEnabled = true,
            quietHoursStartHour = 13,
            quietHoursStartMinute = 0,
            quietHoursEndHour = 16,
            quietHoursEndMinute = 30
        )
        assertEquals(
            DateTimeUtils.localDateTimeToEpoch(date, LocalTime.of(16, 30)),
            NexusNotificationScheduler.adjustForQuietHours(afternoonTrigger, daytime)
        )
    }

    @Test
    fun `computeNextDailyRoutineEpoch schedules same day when future and rolls to tomorrow when past`() {
        val date = LocalDate.of(2026, 8, 20)
        val morningNow = DateTimeUtils.localDateTimeToEpoch(date, LocalTime.of(7, 0))
        val eveningNow = DateTimeUtils.localDateTimeToEpoch(date, LocalTime.of(20, 0))

        val todayBriefing = NexusNotificationScheduler.computeNextDailyRoutineEpoch(8, 0, morningNow)
        assertEquals(DateTimeUtils.localDateTimeToEpoch(date, LocalTime.of(8, 0)), todayBriefing)

        val tomorrowBriefing = NexusNotificationScheduler.computeNextDailyRoutineEpoch(8, 0, eveningNow)
        assertEquals(DateTimeUtils.localDateTimeToEpoch(date.plusDays(1), LocalTime.of(8, 0)), tomorrowBriefing)
    }

    @Test
    fun `effectiveStatementStatus marks unpaid past statements overdue while preserving paid`() {
        val pastDue = DateTimeUtils.localDateTimeToEpoch(
            DateTimeUtils.todayLocalDate().minusDays(2),
            LocalTime.of(12, 0)
        )
        val futureDue = DateTimeUtils.localDateTimeToEpoch(
            DateTimeUtils.todayLocalDate().plusDays(10),
            LocalTime.of(12, 0)
        )

        val openPast = CreditCardStatementEntity(
            id = "s1",
            creditCardId = "c1",
            yearMonth = "2026-08",
            closingAt = pastDue,
            dueAt = pastDue,
            status = StatementStatus.OPEN,
            createdAt = 1L,
            updatedAt = 1L
        )
        val paidPast = openPast.copy(id = "s2", status = StatementStatus.PAID, paidAt = pastDue)
        val openFuture = openPast.copy(id = "s3", dueAt = futureDue)

        assertEquals(StatementStatus.OVERDUE, NexusRepository.effectiveStatementStatus(openPast).status)
        assertEquals(StatementStatus.PAID, NexusRepository.effectiveStatementStatus(paidPast).status)
        assertEquals(StatementStatus.OPEN, NexusRepository.effectiveStatementStatus(openFuture).status)
    }

    @Test
    fun `computedRemainingLimit and event or project cost aggregations stay consistent`() {
        val card = CreditCardEntity(
            id = "c1",
            name = "Nubank",
            totalLimitCents = 500_000L,
            availableLimitCents = 500_000L,
            createdAt = 1L,
            updatedAt = 1L
        )
        val cw = CardWithStatements(card = card, statements = emptyList(), currentOpenTotalCents = 180_000L)
        assertEquals(320_000L, cw.computedRemainingLimitCents)

        val cwExceeded = CardWithStatements(card = card, statements = emptyList(), currentOpenTotalCents = 650_000L)
        assertEquals(0L, cwExceeded.computedRemainingLimitCents)

        val event = EventEntity(
            id = "e1",
            name = "Festival",
            startAt = 1000L,
            endAt = 2000L,
            createdAt = 1L,
            updatedAt = 1L
        )
        val txPaid = TransactionEntity(
            id = "tx1",
            type = TransactionType.EXPENSE,
            description = "Ingresso",
            amountCents = 25_000L,
            status = FinancialStatus.SETTLED,
            paymentMethod = PaymentMethod.PIX,
            occurredAt = 1000L,
            eventId = "e1",
            isTicketObligation = true,
            createdAt = 1L,
            updatedAt = 1L
        )
        val txPending = txPaid.copy(id = "tx2", description = "Transporte", amountCents = 8_000L, status = FinancialStatus.PENDING)
        val txCancelled = txPaid.copy(id = "tx3", description = "Estorno", amountCents = 10_000L, status = FinancialStatus.CANCELLED)

        val ew = EventWithDetails(
            event = event,
            category = null,
            creditCard = null,
            linkedTasks = emptyList(),
            linkedTransactions = listOf(
                TransactionWithDetails(txPaid, null, null, event, null),
                TransactionWithDetails(txPending, null, null, event, null),
                TransactionWithDetails(txCancelled, null, null, event, null)
            )
        )
        assertEquals(33_000L, ew.estimatedTotalCostCents)
        assertEquals(25_000L, ew.paidTotalCostCents)

        val projectWithCost = ProjectWithProgress(
            project = ProjectEntity(id = "p1", name = "Reforma", createdAt = 1L, updatedAt = 1L),
            totalTasks = 4,
            completedTasks = 1,
            estimatedCostCents = 150_000L,
            paidCostCents = 50_000L,
            linkedTransactionsCount = 2
        )
        assertEquals(25, projectWithCost.progressPercent)
        assertEquals(150_000L, projectWithCost.estimatedCostCents)
    }

    @Test
    fun `phase 3 and 4 enum fallbacks and backup v2 to v3 compatibility work cleanly`() {
        assertEquals(ReminderSourceType.TASK, ReminderSourceType.fromStringOrDefault("UNKNOWN"))
        assertEquals(ReminderCategory.SCHEDULE, ReminderCategory.fromStringOrDefault("UNKNOWN"))
        assertEquals(ReminderStatus.ACTIVE, ReminderStatus.fromStringOrDefault("UNKNOWN"))
        assertEquals(BriefingType.MORNING, BriefingType.fromStringOrDefault("UNKNOWN"))
        assertEquals(StatementStatus.OPEN, StatementStatus.fromStringOrDefault("UNKNOWN"))

        val v2Json = """
            {
              "formatVersion": 2,
              "appVersion": "2.0.0",
              "databaseVersion": 2,
              "events": [{ "id": "ev1", "name": "Show" }],
              "transactions": [{ "id": "tx1", "description": "Aluguel", "amountCents": 200000 }]
            }
        """.trimIndent()
        val payloadFromV2 = tolerantJson.decodeFromString<NexusBackupPayload>(v2Json)
        assertEquals(2, payloadFromV2.formatVersion)
        assertEquals(1, payloadFromV2.events.size)
        assertEquals(1, payloadFromV2.transactions.size)
        assertTrue(payloadFromV2.reminders.isEmpty())
        assertTrue(payloadFromV2.routineSettings.isEmpty())
        assertTrue(payloadFromV2.briefingHistory.isEmpty())

        val v3Json = """
            {
              "formatVersion": 3,
              "appVersion": "3.0.0",
              "databaseVersion": 3,
              "reminders": [
                {
                  "id": "r1",
                  "sourceType": "TASK",
                  "sourceId": "t1",
                  "category": "SCHEDULE",
                  "title": "Tarefa",
                  "triggerAt": 1700000000000
                }
              ],
              "routineSettings": [
                {
                  "id": "nexus_routine_default",
                  "notificationsEnabled": true,
                  "quietHoursEnabled": true
                }
              ],
              "briefingHistory": [
                {
                  "id": "b1",
                  "briefingType": "MORNING",
                  "dateIso": "2026-08-20"
                }
              ]
            }
        """.trimIndent()
        val payloadV3 = tolerantJson.decodeFromString<NexusBackupPayload>(v3Json)
        assertEquals(3, payloadV3.formatVersion)
        assertEquals(1, payloadV3.reminders.size)
        assertEquals(1, payloadV3.routineSettings.size)
        assertEquals(1, payloadV3.briefingHistory.size)
        assertTrue(payloadV3.routineSettings.first().quietHoursEnabled)
    }
}
