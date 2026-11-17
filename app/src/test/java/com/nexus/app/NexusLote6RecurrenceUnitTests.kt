package com.nexus.app

import com.nexus.app.core.DateTimeUtils
import com.nexus.app.data.backup.NexusBackupPayload
import com.nexus.app.data.backup.RecurrenceExceptionBackupDto
import com.nexus.app.data.backup.ReminderBackupDto
import com.nexus.app.data.backup.TaskBackupDto
import com.nexus.app.data.backup.TaskRecurrenceBackupDto
import com.nexus.app.data.db.entity.RecurrenceExceptionEntity
import com.nexus.app.data.db.entity.ReminderEntity
import com.nexus.app.data.db.entity.TaskEntity
import com.nexus.app.data.db.entity.TaskRecurrenceEntity
import com.nexus.app.domain.model.RecurrenceExceptionType
import com.nexus.app.domain.model.ReminderCategory
import com.nexus.app.domain.model.ReminderSourceType
import com.nexus.app.domain.model.ReminderStatus
import com.nexus.app.domain.model.TaskPriority
import com.nexus.app.domain.model.TaskRecurrenceFrequency
import com.nexus.app.domain.model.TaskStatus
import com.nexus.app.domain.rules.AgendaEngine
import com.nexus.app.domain.rules.AgendaFilters
import com.nexus.app.domain.rules.AgendaOccurrenceKind
import com.nexus.app.domain.rules.AgendaSourceType
import com.nexus.app.domain.rules.AgendaSources
import com.nexus.app.domain.rules.DateRange
import com.nexus.app.domain.rules.RecurrenceEngine
import com.nexus.app.domain.rules.ReminderRules
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.util.TimeZone

class NexusLote6RecurrenceUnitTests {

    private var savedDefaultTimeZone: TimeZone? = null

    private val tolerantJson = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
        isLenient = true
    }

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
    fun `recurrence engine generates all six frequencies accurately including month-end 31 anchor`() {
        val daily = sampleRecurrence(
            id = "rec-daily",
            frequency = TaskRecurrenceFrequency.DAILY,
            startDateIso = "2026-11-10"
        )
        assertEquals(
            listOf(
                LocalDate.of(2026, 11, 10),
                LocalDate.of(2026, 11, 11),
                LocalDate.of(2026, 11, 12)
            ),
            RecurrenceEngine.generateScheduledDates(daily, LocalDate.of(2026, 11, 12))
        )

        val mask = RecurrenceEngine.maskFromWeekdays(
            setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)
        )
        val weekdays = sampleRecurrence(
            id = "rec-wd",
            frequency = TaskRecurrenceFrequency.SPECIFIC_WEEKDAYS,
            weekdaysMask = mask,
            startDateIso = "2026-11-09" // Segunda-feira
        )
        assertEquals(
            listOf(
                LocalDate.of(2026, 11, 9),
                LocalDate.of(2026, 11, 11),
                LocalDate.of(2026, 11, 13),
                LocalDate.of(2026, 11, 16)
            ),
            RecurrenceEngine.generateScheduledDates(weekdays, LocalDate.of(2026, 11, 16))
        )

        val weekly = sampleRecurrence(
            id = "rec-weekly",
            frequency = TaskRecurrenceFrequency.WEEKLY,
            startDateIso = "2026-11-02"
        )
        assertEquals(
            listOf(
                LocalDate.of(2026, 11, 2),
                LocalDate.of(2026, 11, 9),
                LocalDate.of(2026, 11, 16)
            ),
            RecurrenceEngine.generateScheduledDates(weekly, LocalDate.of(2026, 11, 18))
        )

        val biweekly = sampleRecurrence(
            id = "rec-biweekly",
            frequency = TaskRecurrenceFrequency.BIWEEKLY,
            startDateIso = "2026-11-02"
        )
        assertEquals(
            listOf(
                LocalDate.of(2026, 11, 2),
                LocalDate.of(2026, 11, 16),
                LocalDate.of(2026, 11, 30)
            ),
            RecurrenceEngine.generateScheduledDates(biweekly, LocalDate.of(2026, 12, 1))
        )

        val monthlyDay31 = sampleRecurrence(
            id = "rec-monthly-31",
            frequency = TaskRecurrenceFrequency.MONTHLY,
            startDateIso = "2026-01-31"
        )
        assertEquals(
            listOf(
                LocalDate.of(2026, 1, 31),
                LocalDate.of(2026, 2, 28),
                LocalDate.of(2026, 3, 31),
                LocalDate.of(2026, 4, 30),
                LocalDate.of(2026, 5, 31)
            ),
            RecurrenceEngine.generateScheduledDates(monthlyDay31, LocalDate.of(2026, 5, 31))
        )

        val customDays = sampleRecurrence(
            id = "rec-custom",
            frequency = TaskRecurrenceFrequency.CUSTOM_DAYS,
            customIntervalDays = 3,
            startDateIso = "2026-11-01"
        )
        assertEquals(
            listOf(
                LocalDate.of(2026, 11, 1),
                LocalDate.of(2026, 11, 4),
                LocalDate.of(2026, 11, 7)
            ),
            RecurrenceEngine.generateScheduledDates(customDays, LocalDate.of(2026, 11, 9))
        )
    }

    @Test
    fun `recurrence engine respects endDateIso and maxOccurrences limits`() {
        val limitedByDate = sampleRecurrence(
            id = "rec-end",
            frequency = TaskRecurrenceFrequency.DAILY,
            startDateIso = "2026-11-01",
            endDateIso = "2026-11-03"
        )
        assertEquals(
            listOf(
                LocalDate.of(2026, 11, 1),
                LocalDate.of(2026, 11, 2),
                LocalDate.of(2026, 11, 3)
            ),
            RecurrenceEngine.generateScheduledDates(limitedByDate, LocalDate.of(2026, 11, 30))
        )

        val limitedByCount = sampleRecurrence(
            id = "rec-count",
            frequency = TaskRecurrenceFrequency.DAILY,
            startDateIso = "2026-11-01",
            maxOccurrences = 2
        )
        assertEquals(
            listOf(
                LocalDate.of(2026, 11, 1),
                LocalDate.of(2026, 11, 2)
            ),
            RecurrenceEngine.generateScheduledDates(limitedByCount, LocalDate.of(2026, 11, 30))
        )
    }

    @Test
    fun `nextOccurrenceDateAfter keeps original cadence and skips SKIP or materialized dates`() {
        val weeklyMonday = sampleRecurrence(
            id = "rec-cadence",
            frequency = TaskRecurrenceFrequency.WEEKLY,
            startDateIso = "2026-11-02" // Segunda
        )
        val skipSecond = RecurrenceExceptionEntity(
            id = "ex-skip",
            recurrenceId = "rec-cadence",
            occurrenceDateIso = "2026-11-09",
            exceptionType = RecurrenceExceptionType.SKIP,
            createdAt = 1L,
            updatedAt = 1L
        )
        val materializedThird = TaskEntity(
            id = "task-3",
            title = "Revisão semanal",
            status = TaskStatus.NOT_STARTED,
            recurrenceId = "rec-cadence",
            occurrenceDateIso = "2026-11-16",
            createdAt = 1L,
            updatedAt = 1L
        )

        val next = RecurrenceEngine.nextOccurrenceDateAfter(
            recurrence = weeklyMonday,
            afterDateInclusive = LocalDate.of(2026, 11, 2),
            exceptions = listOf(skipSecond),
            materializedTasks = listOf(materializedThird)
        )
        assertEquals(LocalDate.of(2026, 11, 23), next)

        val inactive = weeklyMonday.copy(isActive = false)
        assertNull(RecurrenceEngine.nextOccurrenceDateAfter(inactive, LocalDate.of(2026, 11, 2)))
    }

    @Test
    fun `agenda engine projects virtual recurring occurrences without duplicating materialized tasks and respects exceptions`() {
        val rec = sampleRecurrence(
            id = "rec-agenda",
            title = "Pagar guia mensal",
            frequency = TaskRecurrenceFrequency.WEEKLY,
            startDateIso = "2026-11-02",
            hasDueTime = true,
            dueTimeMinutesOfDay = 9 * 60 + 30
        )
        val materializedFirst = TaskEntity(
            id = "t-mat-1",
            title = "Pagar guia mensal",
            status = TaskStatus.COMPLETED,
            dueAt = DateTimeUtils.localDateTimeToEpoch(LocalDate.of(2026, 11, 2), LocalTime.of(9, 30)),
            hasDueTime = true,
            recurrenceId = "rec-agenda",
            occurrenceDateIso = "2026-11-02",
            completedAt = 100L,
            createdAt = 1L,
            updatedAt = 1L
        )
        val skipSecond = RecurrenceExceptionEntity(
            id = "ex-skip-2",
            recurrenceId = "rec-agenda",
            occurrenceDateIso = "2026-11-09",
            exceptionType = RecurrenceExceptionType.SKIP,
            createdAt = 1L,
            updatedAt = 1L
        )
        val overrideThird = RecurrenceExceptionEntity(
            id = "ex-ovr-3",
            recurrenceId = "rec-agenda",
            occurrenceDateIso = "2026-11-16",
            exceptionType = RecurrenceExceptionType.OVERRIDE,
            overrideTitle = "Pagar guia ajustada",
            overrideDueAt = DateTimeUtils.localDateTimeToEpoch(LocalDate.of(2026, 11, 17), LocalTime.of(14, 0)),
            createdAt = 1L,
            updatedAt = 1L
        )

        val window = DateRange(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 25))
        val occurrences = AgendaEngine.expand(
            window = window,
            sources = AgendaSources(
                tasks = listOf(materializedFirst),
                taskRecurrences = listOf(rec),
                recurrenceExceptions = listOf(skipSecond, overrideThird)
            ),
            today = LocalDate.of(2026, 11, 1)
        )

        val taskOccurrences = occurrences.filter { it.sourceType == AgendaSourceType.TASK }
        // 02/11 (materializada), 17/11 (override de 16/11), 23/11 (virtual normal). 09/11 foi SKIP.
        assertEquals(listOf("2026-11-02", "2026-11-17", "2026-11-23"), taskOccurrences.map { it.occurrenceDateIso })

        val matOcc = taskOccurrences[0]
        assertFalse(matOcc.isVirtualRecurrence)
        assertEquals("t-mat-1", matOcc.sourceId)
        assertEquals("rec-agenda", matOcc.recurrenceId)

        val overriddenVirtual = taskOccurrences[1]
        assertTrue(overriddenVirtual.isVirtualRecurrence)
        assertEquals("Pagar guia ajustada", overriddenVirtual.title)
        assertEquals("rec-agenda", overriddenVirtual.recurrenceId)

        val normalVirtual = taskOccurrences[2]
        assertTrue(normalVirtual.isVirtualRecurrence)
        assertEquals("Pagar guia mensal", normalVirtual.title)

        // Filtro contextual de Compromissos deve ocultar ocorrências de tarefa recorrente
        val apptOnly = AgendaEngine.expand(
            window = window,
            sources = AgendaSources(
                tasks = listOf(materializedFirst),
                taskRecurrences = listOf(rec),
                recurrenceExceptions = listOf(skipSecond, overrideThird)
            ),
            filters = AgendaFilters(kindFilter = AgendaOccurrenceKind.APPOINTMENT),
            today = LocalDate.of(2026, 11, 1)
        )
        assertTrue(apptOnly.isEmpty())
    }

    @Test
    fun `backup payload v6 round-trips task recurrences exceptions and reminder cancellation cleanly`() {
        val payload = NexusBackupPayload(
            formatVersion = NexusBackupPayload.CURRENT_FORMAT_VERSION,
            appVersion = "5.2.0",
            exportedAt = "2026-11-17T10:00:00-03:00",
            databaseVersion = 6,
            tasks = listOf(
                TaskBackupDto(
                    id = "t-rec-1",
                    title = "Backup semanal",
                    status = TaskStatus.NOT_STARTED.name,
                    recurrenceId = "rec-1",
                    occurrenceDateIso = "2026-11-16",
                    createdAt = 10L,
                    updatedAt = 10L
                )
            ),
            recurrences = listOf(
                TaskRecurrenceBackupDto(
                    id = "rec-1",
                    title = "Backup semanal",
                    priority = TaskPriority.HIGH.name,
                    frequency = TaskRecurrenceFrequency.SPECIFIC_WEEKDAYS.name,
                    weekdaysMask = RecurrenceEngine.maskFromWeekdays(setOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY)),
                    startDateIso = "2026-11-16",
                    maxOccurrences = 10,
                    isActive = true,
                    createdAt = 10L,
                    updatedAt = 10L
                )
            ),
            recurrenceExceptions = listOf(
                RecurrenceExceptionBackupDto(
                    id = "ex-1",
                    recurrenceId = "rec-1",
                    occurrenceDateIso = "2026-11-19",
                    exceptionType = RecurrenceExceptionType.SKIP.name,
                    createdAt = 10L,
                    updatedAt = 10L
                )
            ),
            reminders = listOf(
                ReminderBackupDto(
                    id = "rem-1",
                    sourceType = ReminderSourceType.TASK.name,
                    sourceId = "t-rec-1",
                    category = ReminderCategory.SCHEDULE.name,
                    title = "Backup semanal",
                    message = "Lembrete",
                    triggerAt = 1000L,
                    minutesBefore = 30,
                    channelId = "nexus_schedule",
                    status = ReminderStatus.CANCELLED.name,
                    createdAt = 10L,
                    updatedAt = 10L
                )
            )
        )

        assertEquals(6, NexusBackupPayload.CURRENT_FORMAT_VERSION)
        val encoded = tolerantJson.encodeToString(payload)
        val decoded = tolerantJson.decodeFromString<NexusBackupPayload>(encoded)

        assertEquals(1, decoded.recurrences.size)
        assertEquals(1, decoded.recurrenceExceptions.size)
        assertEquals("rec-1", decoded.tasks.single().recurrenceId)
        assertEquals("2026-11-16", decoded.tasks.single().occurrenceDateIso)
        assertEquals(30, decoded.reminders.single().minutesBefore)
        assertEquals(ReminderStatus.CANCELLED.name, decoded.reminders.single().status)

        // Confirma compatibilidade retroativa com JSON v5 sem campos de recorrência
        val legacyV5 = """
            {
              "formatVersion": 5,
              "appVersion": "5.1.0",
              "exportedAt": "2026-10-01T09:00:00-03:00",
              "databaseVersion": 5,
              "tasks": [
                { "id": "t-v5", "title": "Tarefa v5", "status": "NOT_STARTED", "createdAt": 1, "updatedAt": 1 }
              ]
            }
        """.trimIndent()
        val decodedV5 = tolerantJson.decodeFromString<NexusBackupPayload>(legacyV5)
        assertEquals(5, decodedV5.formatVersion)
        assertTrue(decodedV5.recurrences.isEmpty())
        assertTrue(decodedV5.recurrenceExceptions.isEmpty())
        assertNull(decodedV5.tasks.single().recurrenceId)

        // Lembrete cancelado com minutesBefore preservado ou legado -1 deve ser tratado como cancelado
        val legacyCancelledReminder = ReminderEntity(
            id = "rem-legacy",
            sourceType = ReminderSourceType.TASK,
            sourceId = "t-v5",
            category = ReminderCategory.SCHEDULE,
            title = "Legado",
            message = "",
            triggerAt = 1000L,
            minutesBefore = -1,
            channelId = "nexus_schedule",
            status = ReminderStatus.ACTIVE,
            createdAt = 1L,
            updatedAt = 1L
        )
        assertTrue(ReminderRules.isEffectivelyCancelled(legacyCancelledReminder))
        assertFalse(ReminderRules.isQueueVisible(legacyCancelledReminder))
        assertFalse(ReminderRules.isSchedulableOrFirable(legacyCancelledReminder))
    }

    @Test
    fun `formatRecurrenceSummary produces concise PT-BR label without errors`() {
        val rec = sampleRecurrence(
            id = "rec-fmt",
            frequency = TaskRecurrenceFrequency.CUSTOM_DAYS,
            customIntervalDays = 4,
            startDateIso = "2026-11-01",
            maxOccurrences = 6
        )
        val summary = RecurrenceEngine.formatRecurrenceSummary(rec)
        assertNotNull(summary)
        assertTrue(summary.contains("4 dias"))
        assertTrue(summary.contains("6x"))
    }

    private fun sampleRecurrence(
        id: String,
        title: String = "Tarefa recorrente",
        frequency: TaskRecurrenceFrequency,
        customIntervalDays: Int = 1,
        weekdaysMask: Int = 0,
        startDateIso: String,
        endDateIso: String? = null,
        maxOccurrences: Int? = null,
        hasDueTime: Boolean = false,
        dueTimeMinutesOfDay: Int? = null
    ) = TaskRecurrenceEntity(
        id = id,
        title = title,
        frequency = frequency,
        customIntervalDays = customIntervalDays,
        weekdaysMask = weekdaysMask,
        startDateIso = startDateIso,
        endDateIso = endDateIso,
        maxOccurrences = maxOccurrences,
        hasDueTime = hasDueTime,
        dueTimeMinutesOfDay = dueTimeMinutesOfDay,
        isActive = true,
        createdAt = 1L,
        updatedAt = 1L
    )
}
