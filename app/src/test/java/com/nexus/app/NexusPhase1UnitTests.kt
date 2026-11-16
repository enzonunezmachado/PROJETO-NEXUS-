package com.nexus.app

import com.nexus.app.core.DateTimeUtils
import com.nexus.app.data.backup.NexusBackupPayload
import com.nexus.app.data.db.entity.AppointmentEntity
import com.nexus.app.data.db.entity.TaskEntity
import com.nexus.app.data.repository.NexusRepository
import com.nexus.app.domain.model.ProjectStatus
import com.nexus.app.domain.model.TaskPriority
import com.nexus.app.domain.model.TaskStatus
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class NexusPhase1UnitTests {

    private val tolerantJson = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
        isLenient = true
    }

    @Test
    fun `computeAppointmentConflicts flags overlapping timed events and ignores contiguous or all-day`() {
        val baseDate = LocalDate.of(2026, 5, 10)
        val t9 = DateTimeUtils.localDateTimeToEpoch(baseDate, LocalTime.of(9, 0))
        val t10 = DateTimeUtils.localDateTimeToEpoch(baseDate, LocalTime.of(10, 0))
        val t930 = DateTimeUtils.localDateTimeToEpoch(baseDate, LocalTime.of(9, 30))
        val t11 = DateTimeUtils.localDateTimeToEpoch(baseDate, LocalTime.of(11, 0))

        val appt1 = AppointmentEntity(
            id = "a1", title = "Reunião A", startAt = t9, endAt = t10, createdAt = 1L, updatedAt = 1L
        )
        val appt2 = AppointmentEntity(
            id = "a2", title = "Call B", startAt = t930, endAt = t11, createdAt = 1L, updatedAt = 1L
        )
        val appt3 = AppointmentEntity(
            id = "a3", title = "Contíguo C", startAt = t11, endAt = t11 + 3_600_000L, createdAt = 1L, updatedAt = 1L
        )
        val allDay = AppointmentEntity(
            id = "a4", title = "Dia inteiro", startAt = t9, endAt = t11, isAllDay = true, createdAt = 1L, updatedAt = 1L
        )

        val conflicts = NexusRepository.computeAppointmentConflicts(listOf(appt1, appt2, appt3, allDay))
        assertEquals(listOf("Call B"), conflicts["a1"])
        assertEquals(listOf("Reunião A"), conflicts["a2"])
        assertFalse(conflicts.containsKey("a3"))
        assertFalse(conflicts.containsKey("a4"))
    }

    @Test
    fun `isTaskOverdue respects terminal statuses and due time`() {
        val today = LocalDate.of(2026, 5, 10)
        val yesterdayEpoch = DateTimeUtils.localDateTimeToEpoch(today.minusDays(1), LocalTime.of(18, 0))
        val now = DateTimeUtils.localDateTimeToEpoch(today, LocalTime.of(12, 0))

        val overdueTask = TaskEntity(
            id = "t1", title = "Atrasada", status = TaskStatus.IN_PROGRESS,
            dueAt = yesterdayEpoch, hasDueTime = false, createdAt = 1L, updatedAt = 1L
        )
        val completedYesterday = overdueTask.copy(id = "t2", status = TaskStatus.COMPLETED)

        assertTrue(NexusRepository.isTaskOverdue(overdueTask, now, today))
        assertFalse(NexusRepository.isTaskOverdue(completedYesterday, now, today))
    }

    @Test
    fun `enum fallbacks recover gracefully from unknown values`() {
        assertEquals(TaskPriority.NORMAL, TaskPriority.fromStringOrDefault("FUTURE_PRIORITY"))
        assertEquals(TaskStatus.NOT_STARTED, TaskStatus.fromStringOrDefault("UNKNOWN_STATUS"))
        assertEquals(ProjectStatus.ACTIVE, ProjectStatus.fromStringOrDefault(null))
    }

    @Test
    fun `tolerant backup parser ignores unknown future modules and fields`() {
        val futureJson = """
            {
              "formatVersion": 4,
              "appVersion": "4.2.0",
              "exportedAt": "2027-01-01T10:00:00Z",
              "futureTransactions": [{ "id": "tx-1", "amountCents": 4500 }],
              "tasks": [
                {
                  "id": "task-1",
                  "title": "Revisar arquitetura",
                  "futureField": "ignored"
                }
              ]
            }
        """.trimIndent()

        val payload = tolerantJson.decodeFromString<NexusBackupPayload>(futureJson)
        assertEquals(4, payload.formatVersion)
        assertEquals(1, payload.tasks.size)
        assertEquals("Revisar arquitetura", payload.tasks.first().title)
        assertEquals("NORMAL", payload.tasks.first().priority)
    }
}
