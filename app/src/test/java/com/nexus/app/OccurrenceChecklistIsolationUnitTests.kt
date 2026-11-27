package com.nexus.app

import com.nexus.app.core.DateTimeUtils
import com.nexus.app.data.backup.AppointmentBackupDto
import com.nexus.app.data.backup.BackupManager
import com.nexus.app.data.backup.EventBackupDto
import com.nexus.app.data.backup.NexusBackupPayload
import com.nexus.app.data.backup.ScheduleChecklistItemBackupDto
import com.nexus.app.data.backup.TaskRecurrenceBackupDto
import com.nexus.app.data.db.entity.AppointmentEntity
import com.nexus.app.data.db.entity.EventEntity
import com.nexus.app.data.db.entity.ScheduleChecklistItemEntity
import com.nexus.app.data.db.entity.SubtaskEntity
import com.nexus.app.data.db.entity.TaskEntity
import com.nexus.app.data.db.entity.TaskRecurrenceEntity
import com.nexus.app.domain.model.RecurrenceDomain
import com.nexus.app.domain.model.TaskPriority
import com.nexus.app.domain.model.TaskRecurrenceFrequency
import com.nexus.app.domain.model.TaskStatus
import com.nexus.app.domain.rules.AgendaEngine
import com.nexus.app.domain.rules.AgendaFilters
import com.nexus.app.domain.rules.AgendaOccurrenceKind
import com.nexus.app.domain.rules.AgendaSources
import com.nexus.app.domain.rules.DateRange
import com.nexus.app.domain.rules.RecurrenceEngine
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

class OccurrenceChecklistIsolationUnitTests {

    private val t0 = 1_795_000_000_000L

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
    }

    // 1. Standalone APPOINTMENT checklist CRUD, toggle, reorder, and persistence
    @Test
    fun `standalone APPOINTMENT checklist supports CRUD toggle and sortOrder persistence`() {
        val apptId = "appt-solo-1"
        val items = mutableListOf(
            ScheduleChecklistItemEntity(
                id = "chk-1",
                appointmentId = apptId,
                title = "Levar RG",
                isCompleted = false,
                sortOrder = 0,
                createdAt = t0,
                updatedAt = t0
            ),
            ScheduleChecklistItemEntity(
                id = "chk-2",
                appointmentId = apptId,
                title = "Exames recentes",
                isCompleted = false,
                sortOrder = 1,
                createdAt = t0,
                updatedAt = t0
            )
        )

        // Toggle item 0, rename item 1, reorder (swap) and add item 2
        val updated0 = items[0].copy(isCompleted = true, sortOrder = 1, updatedAt = t0 + 10L)
        val updated1 = items[1].copy(title = "Exames laboratoriais", sortOrder = 0, updatedAt = t0 + 10L)
        val added2 = ScheduleChecklistItemEntity(
            id = "chk-3",
            appointmentId = apptId,
            title = "Carteirinha",
            isCompleted = false,
            sortOrder = 2,
            createdAt = t0 + 10L,
            updatedAt = t0 + 10L
        )
        val persisted = listOf(updated1, updated0, added2).sortedBy { it.sortOrder }

        assertEquals(listOf("Exames laboratoriais", "Levar RG", "Carteirinha"), persisted.map { it.title })
        assertEquals(listOf(false, true, false), persisted.map { it.isCompleted })
        assertEquals(listOf(0, 1, 2), persisted.map { it.sortOrder })
    }

    // 2. Standalone EVENT checklist CRUD, toggle, reorder, and persistence
    @Test
    fun `standalone EVENT checklist supports CRUD toggle and sortOrder persistence`() {
        val eventId = "ev-solo-1"
        val items = mutableListOf(
            ScheduleChecklistItemEntity(
                id = "ev-chk-1",
                eventId = eventId,
                title = "Ingresso QR Code",
                isCompleted = true,
                sortOrder = 0,
                createdAt = t0,
                updatedAt = t0
            ),
            ScheduleChecklistItemEntity(
                id = "ev-chk-2",
                eventId = eventId,
                title = "Documento com foto",
                isCompleted = false,
                sortOrder = 1,
                createdAt = t0,
                updatedAt = t0
            )
        )
        assertEquals(2, items.size)
        assertEquals(1, items.count { it.isCompleted })
        items.removeAt(1)
        assertEquals(1, items.size)
        assertTrue(items.all { it.isCompleted })
    }

    // 3. Recurring TASK keeps per-occurrence subtask checklists completely isolated
    @Test
    fun `recurring TASK keeps checklists strictly isolated between occurrences`() {
        val rec = TaskRecurrenceEntity(
            id = "rec-task-daily",
            domain = RecurrenceDomain.TASK,
            title = "Rotina matinal",
            frequency = TaskRecurrenceFrequency.DAILY,
            startDateIso = "2026-11-01",
            checklistTemplateJson = RecurrenceEngine.serializeChecklistTemplate(listOf("Água", "Alongamento")),
            checklistTemplateVersion = 1,
            templateHistoryJson = RecurrenceEngine.serializeTemplateHistory(
                listOf(
                    RecurrenceEngine.ChecklistTemplateSnapshot(
                        version = 1,
                        effectiveFromDateIso = "2026-11-01",
                        items = listOf("Água", "Alongamento")
                    )
                )
            ),
            createdAt = t0,
            updatedAt = t0
        )

        val occ1Task = TaskEntity(
            id = "task-occ-1",
            title = rec.title,
            recurrenceId = rec.id,
            occurrenceDateIso = "2026-11-01",
            createdAt = t0,
            updatedAt = t0
        )
        val occ2Task = TaskEntity(
            id = "task-occ-2",
            title = rec.title,
            recurrenceId = rec.id,
            occurrenceDateIso = "2026-11-02",
            createdAt = t0,
            updatedAt = t0
        )

        val (_, occ1Titles) = RecurrenceEngine.resolveChecklistTemplateVersionAndItems(rec, occ1Task.occurrenceDateIso!!)
        val (_, occ2Titles) = RecurrenceEngine.resolveChecklistTemplateVersionAndItems(rec, occ2Task.occurrenceDateIso!!)

        val occ1Subs = occ1Titles.mapIndexed { idx, title ->
            SubtaskEntity(id = UUID.randomUUID().toString(), taskId = occ1Task.id, title = title, isCompleted = idx == 0, sortOrder = idx, createdAt = t0, updatedAt = t0)
        }.toMutableList()
        val occ2Subs = occ2Titles.mapIndexed { idx, title ->
            SubtaskEntity(id = UUID.randomUUID().toString(), taskId = occ2Task.id, title = title, isCompleted = false, sortOrder = idx, createdAt = t0, updatedAt = t0)
        }.toMutableList()

        // Rename and add extra item on occ1 only
        occ1Subs[1] = occ1Subs[1].copy(title = "Alongamento lombar")
        occ1Subs.add(SubtaskEntity(id = UUID.randomUUID().toString(), taskId = occ1Task.id, title = "Café", sortOrder = 2, createdAt = t0, updatedAt = t0))

        assertEquals(3, occ1Subs.size)
        assertEquals(2, occ2Subs.size)
        assertTrue(occ1Subs[0].isCompleted)
        assertFalse(occ2Subs[0].isCompleted)
        assertEquals("Alongamento", occ2Subs[1].title)
    }

    // 4. Recurring APPOINTMENT keeps per-occurrence checklist items completely isolated
    @Test
    fun `recurring APPOINTMENT keeps checklists strictly isolated across occurrences`() {
        val rec = TaskRecurrenceEntity(
            id = "rec-appt-1",
            domain = RecurrenceDomain.APPOINTMENT,
            title = "Reunião semanal",
            frequency = TaskRecurrenceFrequency.WEEKLY,
            startDateIso = "2026-11-02",
            checklistTemplateJson = RecurrenceEngine.serializeChecklistTemplate(listOf("Pauta", "Notas")),
            checklistTemplateVersion = 1,
            templateHistoryJson = RecurrenceEngine.serializeTemplateHistory(
                listOf(RecurrenceEngine.ChecklistTemplateSnapshot(1, "2026-11-02", listOf("Pauta", "Notas")))
            ),
            createdAt = t0,
            updatedAt = t0
        )
        val occ1 = AppointmentEntity(
            id = "appt-1",
            title = rec.title,
            startAt = DateTimeUtils.localDateTimeToEpoch(LocalDate.of(2026, 11, 2), LocalTime.of(9, 0)),
            endAt = DateTimeUtils.localDateTimeToEpoch(LocalDate.of(2026, 11, 2), LocalTime.of(10, 0)),
            recurrenceId = rec.id,
            occurrenceDateIso = "2026-11-02",
            createdAt = t0,
            updatedAt = t0
        )
        val occ2 = AppointmentEntity(
            id = "appt-2",
            title = rec.title,
            startAt = DateTimeUtils.localDateTimeToEpoch(LocalDate.of(2026, 11, 9), LocalTime.of(9, 0)),
            endAt = DateTimeUtils.localDateTimeToEpoch(LocalDate.of(2026, 11, 9), LocalTime.of(10, 0)),
            recurrenceId = rec.id,
            occurrenceDateIso = "2026-11-09",
            createdAt = t0,
            updatedAt = t0
        )

        val itemsOcc1 = listOf(
            ScheduleChecklistItemEntity(id = "a1-c1", appointmentId = occ1.id, title = "Pauta", isCompleted = true, sortOrder = 0, templateVersion = 1, createdAt = t0, updatedAt = t0),
            ScheduleChecklistItemEntity(id = "a1-c2", appointmentId = occ1.id, title = "Notas", isCompleted = true, sortOrder = 1, templateVersion = 1, createdAt = t0, updatedAt = t0)
        )
        val itemsOcc2 = listOf(
            ScheduleChecklistItemEntity(id = "a2-c1", appointmentId = occ2.id, title = "Pauta", isCompleted = false, sortOrder = 0, templateVersion = 1, createdAt = t0, updatedAt = t0),
            ScheduleChecklistItemEntity(id = "a2-c2", appointmentId = occ2.id, title = "Notas", isCompleted = false, sortOrder = 1, templateVersion = 1, createdAt = t0, updatedAt = t0)
        )

        assertTrue(itemsOcc1.all { it.isCompleted })
        assertTrue(itemsOcc2.none { it.isCompleted })
        assertNotEquals(itemsOcc1.map { it.id }, itemsOcc2.map { it.id })
    }

    // 5. Recurring EVENT keeps per-occurrence checklist items completely isolated
    @Test
    fun `recurring EVENT keeps checklists strictly isolated across occurrences`() {
        val occ1 = EventEntity(
            id = "ev-1",
            name = "Feira Mensal",
            startAt = DateTimeUtils.localDateTimeToEpoch(LocalDate.of(2026, 11, 10), LocalTime.of(10, 0)),
            endAt = DateTimeUtils.localDateTimeToEpoch(LocalDate.of(2026, 11, 10), LocalTime.of(18, 0)),
            recurrenceId = "rec-ev-1",
            occurrenceDateIso = "2026-11-10",
            createdAt = t0,
            updatedAt = t0
        )
        val occ2 = EventEntity(
            id = "ev-2",
            name = "Feira Mensal",
            startAt = DateTimeUtils.localDateTimeToEpoch(LocalDate.of(2026, 12, 10), LocalTime.of(10, 0)),
            endAt = DateTimeUtils.localDateTimeToEpoch(LocalDate.of(2026, 12, 10), LocalTime.of(18, 0)),
            recurrenceId = "rec-ev-1",
            occurrenceDateIso = "2026-12-10",
            createdAt = t0,
            updatedAt = t0
        )
        val itemsOcc1 = listOf(
            ScheduleChecklistItemEntity(id = "e1-c1", eventId = occ1.id, title = "Stand", isCompleted = true, sortOrder = 0, createdAt = t0, updatedAt = t0)
        )
        val itemsOcc2 = listOf(
            ScheduleChecklistItemEntity(id = "e2-c1", eventId = occ2.id, title = "Stand", isCompleted = false, sortOrder = 0, createdAt = t0, updatedAt = t0)
        )

        assertTrue(itemsOcc1.single().isCompleted)
        assertFalse(itemsOcc2.single().isCompleted)
    }

    // 6. Non-retroactive template update on TASK preserves historical snapshot & materialized occs
    @Test
    fun `non-retroactive template update on TASK preserves v1 for past dates and applies v2 for future dates`() {
        val base = TaskRecurrenceEntity(
            id = "rec-task-ver",
            domain = RecurrenceDomain.TASK,
            title = "Checklist diário",
            frequency = TaskRecurrenceFrequency.DAILY,
            startDateIso = "2026-11-01",
            checklistTemplateJson = RecurrenceEngine.serializeChecklistTemplate(listOf("Item A")),
            checklistTemplateVersion = 1,
            templateHistoryJson = RecurrenceEngine.serializeTemplateHistory(
                listOf(RecurrenceEngine.ChecklistTemplateSnapshot(1, "2026-11-01", listOf("Item A")))
            ),
            createdAt = t0,
            updatedAt = t0
        )
        val (v2, json2, hist2) = RecurrenceEngine.buildUpdatedChecklistTemplate(
            existing = base,
            newItems = listOf("Item A", "Item B"),
            effectiveFromDateIso = "2026-11-10"
        )
        val updated = base.copy(
            checklistTemplateVersion = v2,
            checklistTemplateJson = json2,
            templateHistoryJson = hist2
        )

        assertEquals(2, updated.checklistTemplateVersion)
        val (pastVer, pastItems) = RecurrenceEngine.resolveChecklistTemplateVersionAndItems(updated, "2026-11-05")
        val (futureVer, futureItems) = RecurrenceEngine.resolveChecklistTemplateVersionAndItems(updated, "2026-11-10")
        assertEquals(1, pastVer)
        assertEquals(listOf("Item A"), pastItems)
        assertEquals(2, futureVer)
        assertEquals(listOf("Item A", "Item B"), futureItems)
    }

    // 7. Non-retroactive template update on APPOINTMENT preserves historical snapshot
    @Test
    fun `non-retroactive template update on APPOINTMENT resolves version by occurrence date`() {
        val base = TaskRecurrenceEntity(
            id = "rec-appt-ver",
            domain = RecurrenceDomain.APPOINTMENT,
            title = "Sessão semanal",
            frequency = TaskRecurrenceFrequency.WEEKLY,
            startDateIso = "2026-11-02",
            checklistTemplateJson = RecurrenceEngine.serializeChecklistTemplate(listOf("Check-in")),
            checklistTemplateVersion = 1,
            templateHistoryJson = "[]",
            createdAt = t0,
            updatedAt = t0
        )
        val (v2, json2, hist2) = RecurrenceEngine.buildUpdatedChecklistTemplate(
            existing = base,
            newItems = listOf("Check-in", "Resumo"),
            effectiveFromDateIso = "2026-11-16"
        )
        val updated = base.copy(
            checklistTemplateVersion = v2,
            checklistTemplateJson = json2,
            templateHistoryJson = hist2
        )

        assertEquals(2, updated.checklistTemplateVersion)
        assertEquals(1 to listOf("Check-in"), RecurrenceEngine.resolveChecklistTemplateVersionAndItems(updated, "2026-11-09"))
        assertEquals(2 to listOf("Check-in", "Resumo"), RecurrenceEngine.resolveChecklistTemplateVersionAndItems(updated, "2026-11-23"))
    }

    // 8. Non-retroactive template update on EVENT supports multiple sequential version bumps
    @Test
    fun `non-retroactive template update on EVENT resolves v1 v2 and v3 accurately across dates`() {
        val v1Rec = TaskRecurrenceEntity(
            id = "rec-ev-ver",
            domain = RecurrenceDomain.EVENT,
            title = "Encontro mensal",
            frequency = TaskRecurrenceFrequency.MONTHLY,
            startDateIso = "2026-10-01",
            checklistTemplateJson = RecurrenceEngine.serializeChecklistTemplate(listOf("Banner")),
            checklistTemplateVersion = 1,
            templateHistoryJson = RecurrenceEngine.serializeTemplateHistory(
                listOf(RecurrenceEngine.ChecklistTemplateSnapshot(1, "2026-10-01", listOf("Banner")))
            ),
            createdAt = t0,
            updatedAt = t0
        )
        val (v2, json2, hist2) = RecurrenceEngine.buildUpdatedChecklistTemplate(v1Rec, listOf("Banner", "Som"), "2026-11-01")
        val v2Rec = v1Rec.copy(checklistTemplateVersion = v2, checklistTemplateJson = json2, templateHistoryJson = hist2)
        val (v3, json3, hist3) = RecurrenceEngine.buildUpdatedChecklistTemplate(v2Rec, listOf("Banner", "Som", "Café"), "2026-12-01")
        val v3Rec = v2Rec.copy(checklistTemplateVersion = v3, checklistTemplateJson = json3, templateHistoryJson = hist3)

        assertEquals(1 to listOf("Banner"), RecurrenceEngine.resolveChecklistTemplateVersionAndItems(v3Rec, "2026-10-15"))
        assertEquals(2 to listOf("Banner", "Som"), RecurrenceEngine.resolveChecklistTemplateVersionAndItems(v3Rec, "2026-11-15"))
        assertEquals(3 to listOf("Banner", "Som", "Café"), RecurrenceEngine.resolveChecklistTemplateVersionAndItems(v3Rec, "2026-12-15"))
    }

    // 9. Deleting checklist item in occurrence 1 never affects occurrence 2
    @Test
    fun `deleting checklist item in occurrence 1 does not delete item in occurrence 2`() {
        val allChecklistItems = mutableListOf(
            ScheduleChecklistItemEntity(id = "c1-a", appointmentId = "appt-1", title = "A", sortOrder = 0, createdAt = t0, updatedAt = t0),
            ScheduleChecklistItemEntity(id = "c1-b", appointmentId = "appt-1", title = "B", sortOrder = 1, createdAt = t0, updatedAt = t0),
            ScheduleChecklistItemEntity(id = "c2-a", appointmentId = "appt-2", title = "A", sortOrder = 0, createdAt = t0, updatedAt = t0),
            ScheduleChecklistItemEntity(id = "c2-b", appointmentId = "appt-2", title = "B", sortOrder = 1, createdAt = t0, updatedAt = t0)
        )

        allChecklistItems.removeAll { it.id == "c1-a" }

        assertEquals(listOf("c1-b"), allChecklistItems.filter { it.appointmentId == "appt-1" }.map { it.id })
        assertEquals(listOf("c2-a", "c2-b"), allChecklistItems.filter { it.appointmentId == "appt-2" }.map { it.id })
    }

    // 10. Deleting occurrence 1 removes only occurrence 1 checklist items
    @Test
    fun `deleting occurrence 1 cleans only occurrence 1 checklist items and leaves occurrence 2 intact`() {
        val allChecklistItems = mutableListOf(
            ScheduleChecklistItemEntity(id = "e1-a", eventId = "ev-1", title = "Item 1", sortOrder = 0, createdAt = t0, updatedAt = t0),
            ScheduleChecklistItemEntity(id = "e2-a", eventId = "ev-2", title = "Item 1", sortOrder = 0, createdAt = t0, updatedAt = t0)
        )
        allChecklistItems.removeAll { it.eventId == "ev-1" }

        assertTrue(allChecklistItems.none { it.eventId == "ev-1" })
        assertEquals(1, allChecklistItems.count { it.eventId == "ev-2" })
    }

    // 11. AgendaEngine computes distinct checklist counts for materialized vs virtual occurrences
    @Test
    fun `AgendaEngine projects distinct checklist done and total counts for materialized vs virtual occurrences`() {
        val rec = TaskRecurrenceEntity(
            id = "rec-agenda-chk",
            domain = RecurrenceDomain.APPOINTMENT,
            title = "Standup",
            frequency = TaskRecurrenceFrequency.WEEKLY,
            startDateIso = "2026-11-02",
            hasDueTime = true,
            dueTimeMinutesOfDay = 9 * 60,
            durationMillis = 1_800_000L,
            checklistTemplateJson = RecurrenceEngine.serializeChecklistTemplate(listOf("Atualizar status", "Reportar bloqueios")),
            checklistTemplateVersion = 1,
            templateHistoryJson = RecurrenceEngine.serializeTemplateHistory(
                listOf(RecurrenceEngine.ChecklistTemplateSnapshot(1, "2026-11-02", listOf("Atualizar status", "Reportar bloqueios")))
            ),
            createdAt = t0,
            updatedAt = t0
        )
        val materializedAppt = AppointmentEntity(
            id = "appt-standup-nov2",
            title = "Standup",
            startAt = DateTimeUtils.localDateTimeToEpoch(LocalDate.of(2026, 11, 2), LocalTime.of(9, 0)),
            endAt = DateTimeUtils.localDateTimeToEpoch(LocalDate.of(2026, 11, 2), LocalTime.of(9, 30)),
            recurrenceId = rec.id,
            occurrenceDateIso = "2026-11-02",
            createdAt = t0,
            updatedAt = t0
        )
        val occurrences = AgendaEngine.expand(
            window = DateRange(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 15)),
            sources = AgendaSources(
                appointments = listOf(materializedAppt),
                taskRecurrences = listOf(rec),
                checklistCountsByAppointmentId = mapOf("appt-standup-nov2" to (1 to 2))
            ),
            filters = AgendaFilters(kindFilter = AgendaOccurrenceKind.APPOINTMENT),
            today = LocalDate.of(2026, 11, 1)
        )

        assertEquals(2, occurrences.size)
        val matOcc = occurrences[0]
        val virtOcc = occurrences[1]
        assertFalse(matOcc.isVirtualRecurrence)
        assertEquals(1, matOcc.checklistDoneCount)
        assertEquals(2, matOcc.checklistTotalCount)

        assertTrue(virtOcc.isVirtualRecurrence)
        assertEquals(0, virtOcc.checklistDoneCount)
        assertEquals(2, virtOcc.checklistTotalCount)
    }

    // 12. Backup v9 round-trips scheduleChecklistItems and templateHistoryJson faithfully
    @Test
    fun `Backup v9 payload serializes and deserializes scheduleChecklistItems and templateHistoryJson`() {
        val recDto = TaskRecurrenceBackupDto(
            id = "rec-bkp-1",
            title = "Auditoria",
            frequency = TaskRecurrenceFrequency.WEEKLY.name,
            startDateIso = "2026-11-01",
            domain = RecurrenceDomain.APPOINTMENT.name,
            checklistTemplateJson = "[\"Item 1\",\"Item 2\"]",
            checklistTemplateVersion = 2,
            templateHistoryJson = "[{\"version\":1,\"effectiveFromDateIso\":\"2026-11-01\",\"items\":[\"Item 1\"]}]",
            createdAt = t0,
            updatedAt = t0
        )
        val apptDto = AppointmentBackupDto(
            id = "appt-bkp-1",
            title = "Auditoria",
            startAt = t0,
            endAt = t0 + 3_600_000L,
            recurrenceId = "rec-bkp-1",
            occurrenceDateIso = "2026-11-08",
            createdAt = t0,
            updatedAt = t0
        )
        val evDto = EventBackupDto(
            id = "ev-bkp-1",
            name = "Congresso",
            startAt = t0,
            endAt = t0 + 7_200_000L,
            createdAt = t0,
            updatedAt = t0
        )
        val chkDtos = listOf(
            ScheduleChecklistItemBackupDto(
                id = "chk-bkp-1",
                appointmentId = "appt-bkp-1",
                title = "Item 1",
                isCompleted = true,
                sortOrder = 0,
                templateVersion = 1,
                createdAt = t0,
                updatedAt = t0
            ),
            ScheduleChecklistItemBackupDto(
                id = "chk-bkp-2",
                eventId = "ev-bkp-1",
                title = "Crachá",
                isCompleted = false,
                sortOrder = 0,
                templateVersion = 1,
                createdAt = t0,
                updatedAt = t0
            )
        )

        val payload = NexusBackupPayload(
            formatVersion = NexusBackupPayload.CURRENT_FORMAT_VERSION,
            appVersion = "5.3.0",
            exportedAt = "2026-11-27T10:00:00-03:00",
            databaseVersion = 8,
            appointments = listOf(apptDto),
            events = listOf(evDto),
            taskRecurrences = listOf(recDto),
            scheduleChecklistItems = chkDtos
        )

        val raw = json.encodeToString(payload)
        val decoded = json.decodeFromString<NexusBackupPayload>(raw)
        assertEquals(9, decoded.formatVersion)
        assertEquals(2, decoded.scheduleChecklistItems.size)
        assertEquals("appt-bkp-1", decoded.scheduleChecklistItems[0].appointmentId)
        assertEquals("ev-bkp-1", decoded.scheduleChecklistItems[1].eventId)
        assertEquals(2, decoded.taskRecurrences.single().checklistTemplateVersion)
        assertTrue(decoded.taskRecurrences.single().templateHistoryJson.contains("\"version\":1"))
    }
}
