package com.nexus.app

import com.nexus.app.core.DateTimeUtils
import com.nexus.app.data.backup.NexusBackupPayload
import com.nexus.app.data.db.NexusDatabase
import com.nexus.app.data.db.NexusMigrations
import com.nexus.app.data.db.entity.AppointmentEntity
import com.nexus.app.data.db.entity.EventEntity
import com.nexus.app.data.db.entity.RecurrenceExceptionEntity
import com.nexus.app.data.db.entity.SubtaskEntity
import com.nexus.app.data.db.entity.TaskEntity
import com.nexus.app.data.db.entity.TaskRecurrenceEntity
import com.nexus.app.domain.model.EventLifecycleStatus
import com.nexus.app.domain.model.RecurrenceDomain
import com.nexus.app.domain.model.RecurrenceExceptionType
import com.nexus.app.domain.model.TaskRecurrenceFrequency
import com.nexus.app.domain.model.TaskStatus
import com.nexus.app.domain.model.TicketStatus
import com.nexus.app.domain.rules.AgendaEngine
import com.nexus.app.domain.rules.AgendaFilters
import com.nexus.app.domain.rules.AgendaOccurrenceKind
import com.nexus.app.domain.rules.AgendaSourceType
import com.nexus.app.domain.rules.AgendaSources
import com.nexus.app.domain.rules.DateRange
import com.nexus.app.domain.rules.RecurrenceEngine
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.util.TimeZone

class NexusRodada4RecurrenceUnitTests {

    private var savedDefaultTimeZone: TimeZone? = null

    @Before
    fun captureTimeZone() {
        savedDefaultTimeZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("America/Sao_Paulo"))
    }

    @After
    fun restoreTimeZone() {
        savedDefaultTimeZone?.let { TimeZone.setDefault(it) }
    }

    @Test
    fun `annual recurrence handles leap day anchor and formats PT-BR label`() {
        val annual = TaskRecurrenceEntity(
            id = "rec-annual-leap",
            title = "Aniversário bissexto",
            frequency = TaskRecurrenceFrequency.ANNUAL,
            startDateIso = "2024-02-29",
            domain = RecurrenceDomain.EVENT,
            isAllDay = true,
            durationMillis = 86_400_000L,
            createdAt = 1L,
            updatedAt = 1L
        )

        val generated = RecurrenceEngine.generateScheduledDates(annual, LocalDate.of(2028, 3, 1))
        assertEquals(
            listOf(
                LocalDate.of(2024, 2, 29),
                LocalDate.of(2025, 2, 28),
                LocalDate.of(2026, 2, 28),
                LocalDate.of(2027, 2, 28),
                LocalDate.of(2028, 2, 29)
            ),
            generated
        )
        assertEquals("Anualmente", TaskRecurrenceFrequency.ANNUAL.label)
        assertTrue(RecurrenceEngine.formatRecurrenceSummary(annual).contains("Anualmente"))

        val nextAfter2025 = RecurrenceEngine.nextOccurrenceDateForSeries(
            recurrence = annual,
            afterDateInclusive = LocalDate.of(2025, 2, 28)
        )
        assertEquals(LocalDate.of(2026, 2, 28), nextAfter2025)
    }

    @Test
    fun `recurring appointments project virtual occurrences respect exceptions and detect conflicts across virtual and materialized`() {
        val apptRec = TaskRecurrenceEntity(
            id = "rec-appt-weekly",
            title = "Reunião de alinhamento",
            frequency = TaskRecurrenceFrequency.WEEKLY,
            startDateIso = "2026-11-02",
            hasDueTime = true,
            dueTimeMinutesOfDay = 10 * 60,
            durationMillis = 3_600_000L, // 10:00 - 11:00
            location = "Sala A",
            link = "https://nexus.app/meet",
            domain = RecurrenceDomain.APPOINTMENT,
            createdAt = 1L,
            updatedAt = 1L
        )
        val materializedFirst = AppointmentEntity(
            id = "appt-mat-1",
            title = "Reunião de alinhamento",
            startAt = DateTimeUtils.localDateTimeToEpoch(LocalDate.of(2026, 11, 2), LocalTime.of(10, 0)),
            endAt = DateTimeUtils.localDateTimeToEpoch(LocalDate.of(2026, 11, 2), LocalTime.of(11, 0)),
            location = "Sala A",
            recurrenceId = "rec-appt-weekly",
            occurrenceDateIso = "2026-11-02",
            createdAt = 1L,
            updatedAt = 1L
        )
        val standaloneConflict = AppointmentEntity(
            id = "appt-solo-conflict",
            title = "Dentista",
            startAt = DateTimeUtils.localDateTimeToEpoch(LocalDate.of(2026, 11, 16), LocalTime.of(10, 30)),
            endAt = DateTimeUtils.localDateTimeToEpoch(LocalDate.of(2026, 11, 16), LocalTime.of(11, 30)),
            createdAt = 1L,
            updatedAt = 1L
        )
        val skipSecond = RecurrenceExceptionEntity(
            id = "exc-appt-skip",
            recurrenceId = "rec-appt-weekly",
            occurrenceDateIso = "2026-11-09",
            exceptionType = RecurrenceExceptionType.SKIP,
            createdAt = 1L,
            updatedAt = 1L
        )
        val overrideFourth = RecurrenceExceptionEntity(
            id = "exc-appt-ovr",
            recurrenceId = "rec-appt-weekly",
            occurrenceDateIso = "2026-11-23",
            exceptionType = RecurrenceExceptionType.OVERRIDE,
            overrideTitle = "Alinhamento com Diretoria",
            overrideDueAt = DateTimeUtils.localDateTimeToEpoch(LocalDate.of(2026, 11, 24), LocalTime.of(15, 0)),
            overrideEndAt = DateTimeUtils.localDateTimeToEpoch(LocalDate.of(2026, 11, 24), LocalTime.of(16, 30)),
            overrideLocation = "Auditório",
            createdAt = 1L,
            updatedAt = 1L
        )

        val window = DateRange(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30))
        val occurrences = AgendaEngine.expand(
            window = window,
            sources = AgendaSources(
                appointments = listOf(materializedFirst, standaloneConflict),
                taskRecurrences = listOf(apptRec),
                recurrenceExceptions = listOf(skipSecond, overrideFourth)
            ),
            filters = AgendaFilters(kindFilter = AgendaOccurrenceKind.APPOINTMENT),
            today = LocalDate.of(2026, 11, 1)
        )

        val seriesOccs = occurrences.filter { it.recurrenceId == "rec-appt-weekly" }
        // 02/11 (mat), 16/11 (virtual, conflita com Dentista), 24/11 (override de 23/11), 30/11 (virtual). 09/11 skipped.
        assertEquals(
            listOf("2026-11-02", "2026-11-16", "2026-11-24", "2026-11-30"),
            seriesOccs.map { it.occurrenceDateIso }
        )
        assertFalse(seriesOccs[0].isVirtualRecurrence)
        assertEquals("appt-mat-1", seriesOccs[0].sourceId)

        val thirdVirt = seriesOccs[1]
        assertTrue(thirdVirt.isVirtualRecurrence)
        assertTrue(thirdVirt.hasConflict)
        assertEquals(listOf("Dentista"), thirdVirt.conflictTitles)

        val solo = occurrences.first { it.sourceId == "appt-solo-conflict" }
        assertTrue(solo.hasConflict)
        assertEquals(listOf("Reunião de alinhamento"), solo.conflictTitles)

        val overridden = seriesOccs[2]
        assertTrue(overridden.isVirtualRecurrence)
        assertEquals("Alinhamento com Diretoria", overridden.title)
        assertEquals("2026-11-23", overridden.seriesOccurrenceDateIso)
        assertTrue(overridden.subtitle.contains("Auditório"))

        // Idempotent virtual ID round-trip
        val virtId = RecurrenceEngine.virtualOccurrenceId("rec-appt-weekly", "2026-11-16")
        assertEquals("rec-appt-weekly" to "2026-11-16", RecurrenceEngine.parseVirtualOccurrenceId(virtId))
    }

    @Test
    fun `recurring multi-day events project cleanly preserve seriesOccurrenceDateIso and create zero financial transactions`() {
        val eventRec = TaskRecurrenceEntity(
            id = "rec-ev-festival",
            title = "Festival de Tecnologia",
            frequency = TaskRecurrenceFrequency.ANNUAL,
            startDateIso = "2026-11-10",
            domain = RecurrenceDomain.EVENT,
            isAllDay = true,
            durationMillis = 172_800_000L, // 2 dias (10/11 e 11/11)
            locationName = "Parque Central",
            eventStatus = EventLifecycleStatus.CONFIRMED,
            ticketStatus = TicketStatus.NOT_BOUGHT,
            ticketPriceCents = 25_000L,
            ticketQuantity = 2,
            createdAt = 1L,
            updatedAt = 1L
        )

        val window = DateRange(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30))
        val sources = AgendaSources(
            taskRecurrences = listOf(eventRec),
            transactions = emptyList()
        )
        val agendaOccs = AgendaEngine.expand(
            window = window,
            sources = sources,
            today = LocalDate.of(2026, 11, 1)
        )

        val eventOccs = agendaOccs.filter { it.sourceType == AgendaSourceType.EVENT }
        assertEquals(listOf("2026-11-10", "2026-11-11"), eventOccs.map { it.occurrenceDateIso })
        // Both calendar day rows keep the series anchor occurrenceDateIso = 2026-11-10
        assertTrue(eventOccs.all { it.seriesOccurrenceDateIso == "2026-11-10" })
        assertTrue(eventOccs.all { it.isVirtualRecurrence })

        // Finance filter shows zero phantom ticket obligations for virtual recurring events
        val financeOccs = agendaOccs.filter { it.sourceType == AgendaSourceType.TRANSACTION }
        assertTrue(financeOccs.isEmpty())

        val virtualEntity = RecurrenceEngine.projectVirtualEvents(
            window = window,
            recurrences = listOf(eventRec),
            exceptions = emptyList(),
            materializedEvents = emptyList()
        ).single().toVirtualEventEntity()
        assertEquals(0L, virtualEntity.ticketTotalCents)
        assertNull(virtualEntity.ticketPurchaseDate)
        assertNull(virtualEntity.ticketDueDate)
    }

    @Test
    fun `gap003 checklist template serializes cleanly and occurrence checklists remain independent`() {
        val template = listOf("Revisar backlog", "  ", "Validar PRs", "Revisar backlog")
        val serialized = RecurrenceEngine.serializeChecklistTemplate(template)
        val deserialized = RecurrenceEngine.deserializeChecklistTemplate(serialized)
        assertEquals(listOf("Revisar backlog", "Validar PRs", "Revisar backlog"), deserialized)
        assertTrue(RecurrenceEngine.deserializeChecklistTemplate("[]").isEmpty())
        assertTrue(RecurrenceEngine.deserializeChecklistTemplate("invalid-json").isEmpty())

        // Simulate cloning template for occurrence 1 & occurrence 2 independently
        val occ1Subtasks = deserialized.mapIndexed { idx, title ->
            SubtaskEntity(
                id = "sub-occ1-$idx",
                taskId = "task-occ1",
                title = title,
                isCompleted = idx == 0, // Toggling only occurrence 1 item 0
                sortOrder = idx,
                createdAt = 10L,
                updatedAt = 10L
            )
        }
        val occ2Subtasks = deserialized.mapIndexed { idx, title ->
            SubtaskEntity(
                id = "sub-occ2-$idx",
                taskId = "task-occ2",
                title = title,
                isCompleted = false,
                sortOrder = idx,
                createdAt = 20L,
                updatedAt = 20L
            )
        }

        assertTrue(occ1Subtasks[0].isCompleted)
        assertFalse(occ2Subtasks[0].isCompleted)
        assertNotEquals(occ1Subtasks.map { it.id }, occ2Subtasks.map { it.id })
        // Template JSON remains untouched by occurrence 1 completion toggle
        assertEquals(deserialized, RecurrenceEngine.deserializeChecklistTemplate(serialized))
    }

    @Test
    fun `database version migration 6_7 7_8 and backup format version are consistent`() {
        assertEquals(8, NexusDatabase.DATABASE_VERSION)
        assertEquals(9, NexusBackupPayload.CURRENT_FORMAT_VERSION)
        assertEquals(6, NexusMigrations.MIGRATION_6_7.startVersion)
        assertEquals(7, NexusMigrations.MIGRATION_6_7.endVersion)
        assertEquals(7, NexusMigrations.MIGRATION_7_8.startVersion)
        assertEquals(8, NexusMigrations.MIGRATION_7_8.endVersion)
        assertTrue(NexusMigrations.ALL_MIGRATIONS.any { it.startVersion == 6 && it.endVersion == 7 })
        assertTrue(NexusMigrations.ALL_MIGRATIONS.any { it.startVersion == 7 && it.endVersion == 8 })
        assertNotNull(RecurrenceDomain.fromStringOrDefault("APPOINTMENT"))
        assertEquals(RecurrenceDomain.EVENT, RecurrenceDomain.fromStringOrDefault("event"))
        assertEquals(RecurrenceDomain.TASK, RecurrenceDomain.fromStringOrDefault("UNKNOWN"))
    }
}
