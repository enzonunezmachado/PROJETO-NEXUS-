package com.nexus.app

import com.nexus.app.core.DateTimeUtils
import com.nexus.app.data.backup.AppointmentBackupDto
import com.nexus.app.data.backup.NexusBackupPayload
import com.nexus.app.data.backup.TaskBackupDto
import com.nexus.app.data.db.entity.AppointmentEntity
import com.nexus.app.data.db.entity.CategoryEntity
import com.nexus.app.data.db.entity.CreditCardEntity
import com.nexus.app.data.db.entity.CreditCardStatementEntity
import com.nexus.app.data.db.entity.TaskEntity
import com.nexus.app.data.db.entity.TransactionEntity
import com.nexus.app.domain.model.CategoryScope
import com.nexus.app.domain.model.FinancialStatus
import com.nexus.app.domain.model.PaymentMethod
import com.nexus.app.domain.model.StatementStatus
import com.nexus.app.domain.model.TaskStatus
import com.nexus.app.domain.model.TransactionType
import com.nexus.app.domain.rules.AgendaEngine
import com.nexus.app.domain.rules.AgendaFilters
import com.nexus.app.domain.rules.AgendaSourceType
import com.nexus.app.domain.rules.AgendaSources
import com.nexus.app.domain.rules.DateRange
import com.nexus.app.data.repository.NexusRepository
import com.nexus.app.domain.model.TaskPriority
import com.nexus.app.domain.model.TaskWithDetails
import com.nexus.app.domain.rules.AgendaOccurrenceKind
import com.nexus.app.domain.rules.DeadlineCondition
import com.nexus.app.domain.rules.TaskRules
import com.nexus.app.domain.rules.TaskScheduleGroup
import java.time.YearMonth
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class NexusStage2UnitTests {

    private val tolerantJson = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
        isLenient = true
    }

    @Test
    fun `agenda expands multi-day appointments across all overlapped days`() {
        val start = LocalDate.of(2026, 11, 10)
        val end = LocalDate.of(2026, 11, 12)
        val appt = AppointmentEntity(
            id = "appt-multi",
            title = "Imersão Técnica",
            startAt = DateTimeUtils.localDateTimeToEpoch(start, LocalTime.of(9, 0)),
            endAt = DateTimeUtils.localDateTimeToEpoch(end, LocalTime.of(18, 0)),
            isAllDay = false,
            createdAt = 1L,
            updatedAt = 1L
        )

        assertFalse(AgendaEngine.isAppointmentOnDate(appt, LocalDate.of(2026, 11, 9)))
        assertTrue(AgendaEngine.isAppointmentOnDate(appt, LocalDate.of(2026, 11, 10)))
        assertTrue(AgendaEngine.isAppointmentOnDate(appt, LocalDate.of(2026, 11, 11)))
        assertTrue(AgendaEngine.isAppointmentOnDate(appt, LocalDate.of(2026, 11, 12)))
        assertFalse(AgendaEngine.isAppointmentOnDate(appt, LocalDate.of(2026, 11, 13)))

        val expanded = AgendaEngine.expand(
            window = DateRange(LocalDate.of(2026, 11, 9), LocalDate.of(2026, 11, 13)),
            sources = AgendaSources(appointments = listOf(appt)),
            today = start
        )
        assertEquals(listOf("2026-11-10", "2026-11-11", "2026-11-12"), expanded.map { it.occurrenceDateIso })
    }

    @Test
    fun `agenda suppresses credit card purchases covered by statement and excludes zero-total statements`() {
        val today = LocalDate.of(2026, 11, 15)
        val dueDate = LocalDate.of(2026, 11, 20)
        val card = CreditCardEntity(id = "card-1", name = "Black", createdAt = 1L, updatedAt = 1L)
        val openStmt = CreditCardStatementEntity(
            id = "stmt-open",
            creditCardId = "card-1",
            yearMonth = "2026-11",
            closingAt = DateTimeUtils.startOfDayEpoch(LocalDate.of(2026, 11, 10)),
            dueAt = DateTimeUtils.startOfDayEpoch(dueDate),
            status = StatementStatus.CLOSED,
            createdAt = 1L,
            updatedAt = 1L
        )
        val emptyStmt = openStmt.copy(id = "stmt-empty", yearMonth = "2026-12", dueAt = DateTimeUtils.startOfDayEpoch(dueDate.plusMonths(1)))

        val cardTx = TransactionEntity(
            id = "tx-card",
            type = TransactionType.EXPENSE,
            description = "Compra no cartão",
            amountCents = 150_00L,
            status = FinancialStatus.PENDING,
            paymentMethod = PaymentMethod.CREDIT_CARD,
            occurredAt = DateTimeUtils.startOfDayEpoch(today),
            dueAt = DateTimeUtils.startOfDayEpoch(dueDate),
            creditCardId = "card-1",
            statementId = "stmt-open",
            createdAt = 1L,
            updatedAt = 1L
        )
        val directBill = TransactionEntity(
            id = "tx-boleto",
            type = TransactionType.EXPENSE,
            description = "Energia",
            amountCents = 220_00L,
            status = FinancialStatus.PENDING,
            paymentMethod = PaymentMethod.BOLETO,
            occurredAt = DateTimeUtils.startOfDayEpoch(today),
            dueAt = DateTimeUtils.startOfDayEpoch(dueDate),
            createdAt = 1L,
            updatedAt = 1L
        )
        val paidBill = directBill.copy(id = "tx-paid", description = "Água", status = FinancialStatus.SETTLED)
        val income = directBill.copy(id = "tx-inc", type = TransactionType.INCOME, description = "Freela")

        assertFalse(AgendaEngine.isDirectAgendaBill(cardTx))
        assertTrue(AgendaEngine.isDirectAgendaBill(directBill))
        assertFalse(AgendaEngine.isDirectAgendaBill(paidBill))
        assertFalse(AgendaEngine.isDirectAgendaBill(income))

        val hiddenPaid = AgendaEngine.expand(
            window = DateRange(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 12, 31)),
            sources = AgendaSources(
                transactions = listOf(cardTx, directBill, paidBill),
                statements = listOf(openStmt, emptyStmt),
                creditCards = listOf(card)
            ),
            filters = AgendaFilters(showPaid = false),
            today = today
        )

        assertEquals(2, hiddenPaid.size)
        assertTrue(hiddenPaid.none { it.sourceId == "tx-card" })
        assertTrue(hiddenPaid.none { it.sourceId == "tx-paid" })
        assertTrue(hiddenPaid.none { it.sourceId == "stmt-empty" })
        val stmtOcc = hiddenPaid.first { it.sourceType == AgendaSourceType.STATEMENT }
        assertEquals(150_00L, stmtOcc.amountCents)

        val shownPaid = AgendaEngine.expand(
            window = DateRange(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 12, 31)),
            sources = AgendaSources(
                transactions = listOf(cardTx, directBill, paidBill),
                statements = listOf(openStmt, emptyStmt),
                creditCards = listOf(card)
            ),
            filters = AgendaFilters(showPaid = true),
            today = today
        )
        assertEquals(3, shownPaid.size)
        assertTrue(shownPaid.any { it.sourceId == "tx-paid" })
    }

    @Test
    fun `computeAppointmentConflicts detects overlap against full appointment set`() {
        val day = LocalDate.of(2026, 11, 18)
        val a1 = AppointmentEntity(
            id = "a1",
            title = "Reunião Alpha",
            startAt = DateTimeUtils.localDateTimeToEpoch(day, LocalTime.of(10, 0)),
            endAt = DateTimeUtils.localDateTimeToEpoch(day, LocalTime.of(11, 0)),
            createdAt = 1L,
            updatedAt = 1L
        )
        val a2 = AppointmentEntity(
            id = "a2",
            title = "Consulta",
            startAt = DateTimeUtils.localDateTimeToEpoch(day, LocalTime.of(10, 30)),
            endAt = DateTimeUtils.localDateTimeToEpoch(day, LocalTime.of(11, 30)),
            createdAt = 1L,
            updatedAt = 1L
        )
        val conflicts = AgendaEngine.computeAppointmentConflicts(listOf(a1, a2))
        assertEquals(listOf("Consulta"), conflicts["a1"])
        assertEquals(listOf("Reunião Alpha"), conflicts["a2"])
    }

    @Test
    fun `suggestExpenseCategoryId matches finance category by name or returns null`() {
        val taskHealth = CategoryEntity(id = "cat-task-saude", name = "Saúde", scope = CategoryScope.TASK, createdAt = 1L, updatedAt = 1L)
        val finHealth = CategoryEntity(id = "cat-fin-saude", name = "saúde", scope = CategoryScope.FINANCE, createdAt = 1L, updatedAt = 1L)
        val taskStudy = CategoryEntity(id = "cat-task-estudo", name = "Estudos", scope = CategoryScope.TASK, createdAt = 1L, updatedAt = 1L)

        assertEquals("cat-fin-saude", AgendaEngine.suggestExpenseCategoryId("cat-task-saude", listOf(taskHealth, finHealth, taskStudy)))
        assertNull(AgendaEngine.suggestExpenseCategoryId("cat-task-estudo", listOf(taskHealth, finHealth, taskStudy)))
        assertNull(AgendaEngine.suggestExpenseCategoryId(null, listOf(taskHealth, finHealth)))
    }

    @Test
    fun `contextualCategoriesForKind filters strictly by selected occurrence kind`() {
        val generalCat = CategoryEntity(id = "c-gen", name = "Geral", scope = CategoryScope.GENERAL, createdAt = 1L, updatedAt = 1L)
        val taskCat = CategoryEntity(id = "c-task", name = "Trabalho", scope = CategoryScope.TASK, createdAt = 1L, updatedAt = 1L)
        val finCat = CategoryEntity(id = "c-fin", name = "Moradia", scope = CategoryScope.FINANCE, createdAt = 1L, updatedAt = 1L)
        val eventCat = CategoryEntity(id = "c-ev", name = "Show", scope = CategoryScope.EVENT, createdAt = 1L, updatedAt = 1L)
        val all = listOf(generalCat, taskCat, finCat, eventCat)

        assertTrue(AgendaEngine.contextualCategoriesForKind(all, null).isEmpty())
        assertTrue(AgendaEngine.contextualCategoriesForKind(all, AgendaOccurrenceKind.PROJECT).isEmpty())
        assertEquals(listOf("c-gen", "c-task"), AgendaEngine.contextualCategoriesForKind(all, AgendaOccurrenceKind.TASK).map { it.id })
        assertEquals(listOf("c-gen", "c-fin"), AgendaEngine.contextualCategoriesForKind(all, AgendaOccurrenceKind.FINANCE).map { it.id })
        assertEquals(listOf("c-gen", "c-ev"), AgendaEngine.contextualCategoriesForKind(all, AgendaOccurrenceKind.EVENT).map { it.id })
    }

    @Test
    fun `computeMonthlyTaskDistribution uses three exclusive slices and tracks overdue and undated separately`() {
        val today = LocalDate.of(2026, 11, 16)
        val now = DateTimeUtils.localDateTimeToEpoch(today, LocalTime.of(14, 0))
        val month = YearMonth.of(2026, 11)

        val done = TaskEntity(
            id = "t-done",
            title = "Concluída no mês",
            status = TaskStatus.COMPLETED,
            dueAt = DateTimeUtils.localDateTimeToEpoch(LocalDate.of(2026, 11, 5), LocalTime.of(10, 0)),
            completedAt = now,
            createdAt = 1L,
            updatedAt = 1L
        )
        val inProgressOverdue = TaskEntity(
            id = "t-prog",
            title = "Em andamento atrasada",
            status = TaskStatus.IN_PROGRESS,
            dueAt = DateTimeUtils.localDateTimeToEpoch(LocalDate.of(2026, 11, 14), LocalTime.of(10, 0)),
            createdAt = 1L,
            updatedAt = 1L
        )
        val waitingFuture = TaskEntity(
            id = "t-wait",
            title = "Aguardando",
            status = TaskStatus.WAITING,
            dueAt = DateTimeUtils.localDateTimeToEpoch(LocalDate.of(2026, 11, 25), LocalTime.of(10, 0)),
            createdAt = 1L,
            updatedAt = 1L
        )
        val cancelled = TaskEntity(
            id = "t-canc",
            title = "Cancelada",
            status = TaskStatus.CANCELLED,
            dueAt = DateTimeUtils.localDateTimeToEpoch(LocalDate.of(2026, 11, 10), LocalTime.of(10, 0)),
            createdAt = 1L,
            updatedAt = 1L
        )
        val undated = TaskEntity(
            id = "t-nodate",
            title = "Sem prazo",
            status = TaskStatus.NOT_STARTED,
            dueAt = null,
            createdAt = 1L,
            updatedAt = 1L
        )

        val dist = NexusRepository.computeMonthlyTaskDistribution(
            tasks = listOf(done, inProgressOverdue, waitingFuture, cancelled, undated),
            targetMonth = month,
            nowEpoch = now,
            today = today
        )

        assertEquals(1, dist.completedCount)
        assertEquals(1, dist.inProgressCount)
        assertEquals(1, dist.pendingCount)
        assertEquals(3, dist.totalInDistribution)
        assertEquals(1, dist.overdueCount)
        assertEquals(1, dist.undatedPendingCount)
    }

    @Test
    fun `taskRules separates deadline condition from execution status and groups tasks cleanly`() {
        val today = LocalDate.of(2026, 11, 16)
        val now = DateTimeUtils.localDateTimeToEpoch(today, LocalTime.of(14, 0))

        val inProgressOverdue = TaskEntity(
            id = "t1",
            title = "Revisar contrato",
            status = TaskStatus.IN_PROGRESS,
            priority = TaskPriority.HIGH,
            dueAt = DateTimeUtils.localDateTimeToEpoch(today.minusDays(1), LocalTime.of(18, 0)),
            hasDueTime = true,
            createdAt = 1L,
            updatedAt = 1L
        )
        val todayTask = TaskEntity(
            id = "t2",
            title = "Enviar relatório",
            status = TaskStatus.NOT_STARTED,
            dueAt = DateTimeUtils.localDateTimeToEpoch(today, LocalTime.of(18, 0)),
            hasDueTime = true,
            createdAt = 1L,
            updatedAt = 1L
        )
        val weekTask = TaskEntity(
            id = "t3",
            title = "Agendar exame",
            status = TaskStatus.WAITING,
            dueAt = DateTimeUtils.localDateTimeToEpoch(today.plusDays(3), LocalTime.of(12, 0)),
            createdAt = 1L,
            updatedAt = 1L
        )
        val undatedTask = TaskEntity(
            id = "t4",
            title = "Organizar pasta",
            status = TaskStatus.NOT_STARTED,
            dueAt = null,
            createdAt = 1L,
            updatedAt = 1L
        )
        val donePastDue = TaskEntity(
            id = "t5",
            title = "Concluída ontem",
            status = TaskStatus.COMPLETED,
            dueAt = DateTimeUtils.localDateTimeToEpoch(today.minusDays(2), LocalTime.of(9, 0)),
            completedAt = now,
            createdAt = 1L,
            updatedAt = 1L
        )

        assertEquals(TaskStatus.IN_PROGRESS, inProgressOverdue.status)
        assertEquals(DeadlineCondition.OVERDUE, TaskRules.deadlineCondition(inProgressOverdue, now, today))
        assertEquals(DeadlineCondition.ON_TIME, TaskRules.deadlineCondition(todayTask, now, today))
        assertEquals(DeadlineCondition.NO_DEADLINE, TaskRules.deadlineCondition(undatedTask, now, today))
        assertEquals(DeadlineCondition.ON_TIME, TaskRules.deadlineCondition(donePastDue, now, today))

        val wrapped = listOf(inProgressOverdue, todayTask, weekTask, undatedTask, donePastDue).map {
            TaskWithDetails(task = it, project = null, category = null, subtasks = emptyList())
        }
        val sections = TaskRules.groupTasks(wrapped, now, today)
        assertEquals(
            listOf(
                TaskScheduleGroup.OVERDUE,
                TaskScheduleGroup.TODAY,
                TaskScheduleGroup.THIS_WEEK,
                TaskScheduleGroup.NO_DEADLINE,
                TaskScheduleGroup.TERMINAL
            ),
            sections.map { it.group }
        )
    }

    @Test
    fun `backup payload round-trips appointment endAt and remains backward compatible with older versions`() {
        val payload = NexusBackupPayload(
            formatVersion = NexusBackupPayload.CURRENT_FORMAT_VERSION,
            appVersion = "5.2.0",
            exportedAt = "2026-11-16T10:00:00-03:00",
            databaseVersion = 5,
            tasks = listOf(TaskBackupDto(id = "t1", title = "Tarefa", status = TaskStatus.NOT_STARTED.name)),
            appointments = listOf(AppointmentBackupDto(id = "a1", title = "Evento X", startAt = 1000L, endAt = 5000L))
        )
        val encoded = tolerantJson.encodeToString(payload)
        val decoded = tolerantJson.decodeFromString<NexusBackupPayload>(encoded)
        assertEquals(5000L, decoded.appointments.single().endAt)
        assertEquals(NexusBackupPayload.CURRENT_FORMAT_VERSION, decoded.formatVersion)

        val legacyV4 = """
            {
              "formatVersion": 4,
              "exportedAt": "2026-09-01T08:00:00-03:00",
              "appVersion": "5.0.0",
              "databaseVersion": 4,
              "tasks": [
                { "id": "t-v4", "title": "Legado v4", "status": "IN_PROGRESS", "createdAt": 1, "updatedAt": 1 }
              ]
            }
        """.trimIndent()
        val decodedV4 = tolerantJson.decodeFromString<NexusBackupPayload>(legacyV4)
        assertEquals(4, decodedV4.formatVersion)
        assertEquals("t-v4", decodedV4.tasks.single().id)
    }
}
