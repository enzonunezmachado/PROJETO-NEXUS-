package com.nexus.app

import com.nexus.app.core.DateTimeUtils
import com.nexus.app.data.backup.NexusBackupPayload
import com.nexus.app.data.db.entity.TransactionEntity
import com.nexus.app.data.repository.NexusRepository
import com.nexus.app.domain.model.EventLifecycleStatus
import com.nexus.app.domain.model.FinancialStatus
import com.nexus.app.domain.model.PaymentMethod
import com.nexus.app.domain.model.RecurrenceFrequency
import com.nexus.app.domain.model.TicketStatus
import com.nexus.app.domain.model.TransactionType
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class NexusPhase2UnitTests {

    private val tolerantJson = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
        isLenient = true
    }

    @Test
    fun `parseBrlToCents handles pt-BR comma, thousand separators, currency prefix and invalid inputs`() {
        assertEquals(125000L, DateTimeUtils.parseBrlToCents("R$ 1.250,00"))
        assertEquals(4990L, DateTimeUtils.parseBrlToCents("49,90"))
        assertEquals(10000L, DateTimeUtils.parseBrlToCents("100.00"))
        assertEquals(350L, DateTimeUtils.parseBrlToCents("3,5"))
        assertNull(DateTimeUtils.parseBrlToCents(""))
        assertNull(DateTimeUtils.parseBrlToCents("abc"))
    }

    @Test
    fun `advanceDateByFrequency computes exact dates for all frequencies`() {
        val base = LocalDate.of(2026, 1, 31)
        assertEquals(LocalDate.of(2026, 2, 1), NexusRepository.advanceDateByFrequency(base, RecurrenceFrequency.DAILY, 1))
        assertEquals(LocalDate.of(2026, 2, 7), NexusRepository.advanceDateByFrequency(base, RecurrenceFrequency.WEEKLY, 1))
        assertEquals(LocalDate.of(2026, 2, 14), NexusRepository.advanceDateByFrequency(base, RecurrenceFrequency.BIWEEKLY, 1))
        assertEquals(LocalDate.of(2026, 2, 28), NexusRepository.advanceDateByFrequency(base, RecurrenceFrequency.MONTHLY, 1))
        assertEquals(LocalDate.of(2026, 3, 31), NexusRepository.advanceDateByFrequency(base, RecurrenceFrequency.BIMONTHLY, 1))
        assertEquals(LocalDate.of(2026, 4, 30), NexusRepository.advanceDateByFrequency(base, RecurrenceFrequency.QUARTERLY, 1))
        assertEquals(LocalDate.of(2026, 7, 31), NexusRepository.advanceDateByFrequency(base, RecurrenceFrequency.SEMIANNUAL, 1))
        assertEquals(LocalDate.of(2027, 1, 31), NexusRepository.advanceDateByFrequency(base, RecurrenceFrequency.ANNUAL, 1))
        assertEquals(LocalDate.of(2026, 2, 15), NexusRepository.advanceDateByFrequency(base, RecurrenceFrequency.CUSTOM_DAYS, 15))
    }

    @Test
    fun `effectiveTransactionStatus preserves settled and cancelled while flagging past pending`() {
        val pastEpoch = DateTimeUtils.localDateTimeToEpoch(
            DateTimeUtils.todayLocalDate().minusDays(3),
            LocalTime.of(12, 0)
        )
        val pendingPast = TransactionEntity(
            id = "tx1",
            type = TransactionType.EXPENSE,
            description = "Conta de luz",
            amountCents = 15000L,
            status = FinancialStatus.PENDING,
            paymentMethod = PaymentMethod.PIX,
            occurredAt = pastEpoch,
            dueAt = pastEpoch,
            createdAt = 1L,
            updatedAt = 1L
        )
        val settledPast = pendingPast.copy(id = "tx2", status = FinancialStatus.SETTLED, settledAt = pastEpoch)
        val cancelledPast = pendingPast.copy(id = "tx3", status = FinancialStatus.CANCELLED)

        assertEquals(FinancialStatus.OVERDUE, NexusRepository.effectiveTransactionStatus(pendingPast).status)
        assertEquals(FinancialStatus.SETTLED, NexusRepository.effectiveTransactionStatus(settledPast).status)
        assertEquals(FinancialStatus.CANCELLED, NexusRepository.effectiveTransactionStatus(cancelledPast).status)
    }

    @Test
    fun `phase 2 enum fallbacks and v1 to v2 backup compatibility work cleanly`() {
        assertEquals(EventLifecycleStatus.PLANNED, EventLifecycleStatus.fromStringOrDefault("UNKNOWN"))
        assertEquals(TicketStatus.FREE, TicketStatus.fromStringOrDefault("UNKNOWN"))
        assertEquals(PaymentMethod.PIX, PaymentMethod.fromStringOrDefault("UNKNOWN"))
        assertEquals(FinancialStatus.PENDING, FinancialStatus.fromStringOrDefault("UNKNOWN"))

        val v1Json = """
            {
              "formatVersion": 1,
              "appVersion": "1.0.0",
              "exportedAt": "2026-05-01T10:00:00Z",
              "settings": [
                { "id": "nexus_settings_default", "userName": "Enzo", "darkTheme": true }
              ],
              "tasks": [
                { "id": "t1", "title": "Tarefa antiga v1" }
              ]
            }
        """.trimIndent()

        val payload = tolerantJson.decodeFromString<NexusBackupPayload>(v1Json)
        assertEquals(1, payload.formatVersion)
        assertEquals(0L, payload.settings.first().declaredBalanceCents)
        assertEquals(0, payload.transactions.size)
        assertEquals(0, payload.events.size)
        assertEquals(null, payload.tasks.first().eventId)
    }
}
