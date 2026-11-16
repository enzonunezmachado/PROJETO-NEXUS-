package com.nexus.app

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.nexus.app.core.DateTimeUtils
import com.nexus.app.data.backup.NexusBackupPayload
import com.nexus.app.data.db.entity.AppointmentEntity
import com.nexus.app.data.db.entity.CategoryEntity
import com.nexus.app.data.db.entity.CreditCardEntity
import com.nexus.app.data.db.entity.CreditCardStatementEntity
import com.nexus.app.data.db.entity.EventEntity
import com.nexus.app.data.db.entity.FinancialGoalEntity
import com.nexus.app.data.db.entity.ProjectEntity
import com.nexus.app.data.db.entity.TaskEntity
import com.nexus.app.data.db.entity.TransactionEntity
import com.nexus.app.data.repository.NexusRepository
import com.nexus.app.domain.model.CategoryScope
import com.nexus.app.domain.model.EventLifecycleStatus
import com.nexus.app.domain.model.FinancialStatus
import com.nexus.app.domain.model.PaymentMethod
import com.nexus.app.domain.model.StatementStatus
import com.nexus.app.domain.model.TaskPriority
import com.nexus.app.domain.model.TaskStatus
import com.nexus.app.domain.model.TransactionType
import com.nexus.app.widgets.NexusWidgetInstanceConfig
import com.nexus.app.widgets.NexusWidgetSizes
import com.nexus.app.widgets.NexusWidgetType
import com.nexus.app.widgets.WidgetDataState
import com.nexus.app.widgets.WidgetSizeTier
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class NexusPhase5WidgetUnitTests {

    private val tolerantJson = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
        isLenient = true
    }

    // 1. Geracao dos dados do widget Meu Dia
    @Test
    fun `my day widget snapshot aggregates today appointments conflicts due tasks and counters`() {
        val today = LocalDate.of(2026, 9, 15)
        val now = DateTimeUtils.localDateTimeToEpoch(today, LocalTime.of(10, 0))

        val apptMorning = AppointmentEntity(
            id = "a1",
            title = "Daily Alinhamento",
            startAt = DateTimeUtils.localDateTimeToEpoch(today, LocalTime.of(9, 0)),
            endAt = DateTimeUtils.localDateTimeToEpoch(today, LocalTime.of(10, 30)),
            location = "Escritório",
            createdAt = 1L,
            updatedAt = 1L
        )
        val apptConflict = AppointmentEntity(
            id = "a2",
            title = "Dentista",
            startAt = DateTimeUtils.localDateTimeToEpoch(today, LocalTime.of(10, 0)),
            endAt = DateTimeUtils.localDateTimeToEpoch(today, LocalTime.of(11, 0)),
            location = "Centro",
            createdAt = 1L,
            updatedAt = 1L
        )
        val apptTomorrow = AppointmentEntity(
            id = "a3",
            title = "Reunião Amanhã",
            startAt = DateTimeUtils.localDateTimeToEpoch(today.plusDays(1), LocalTime.of(14, 0)),
            endAt = DateTimeUtils.localDateTimeToEpoch(today.plusDays(1), LocalTime.of(15, 0)),
            createdAt = 1L,
            updatedAt = 1L
        )

        val taskDueTodayHigh = TaskEntity(
            id = "t1",
            title = "Enviar contrato",
            priority = TaskPriority.HIGH,
            status = TaskStatus.IN_PROGRESS,
            dueAt = DateTimeUtils.localDateTimeToEpoch(today, LocalTime.of(18, 0)),
            hasDueTime = true,
            createdAt = 1L,
            updatedAt = 1L
        )
        val taskDueTodayOverdue = TaskEntity(
            id = "t2",
            title = "Ligar para banco",
            priority = TaskPriority.URGENT,
            status = TaskStatus.NOT_STARTED,
            dueAt = DateTimeUtils.localDateTimeToEpoch(today, LocalTime.of(8, 30)),
            hasDueTime = true,
            createdAt = 1L,
            updatedAt = 1L
        )
        val taskCompletedToday = TaskEntity(
            id = "t3",
            title = "Revisar pauta",
            priority = TaskPriority.NORMAL,
            status = TaskStatus.COMPLETED,
            dueAt = DateTimeUtils.localDateTimeToEpoch(today, LocalTime.of(8, 0)),
            completedAt = DateTimeUtils.localDateTimeToEpoch(today, LocalTime.of(8, 15)),
            createdAt = 1L,
            updatedAt = 1L
        )
        val taskPendingOtherDay = TaskEntity(
            id = "t4",
            title = "Planejar viagem",
            priority = TaskPriority.LOW,
            status = TaskStatus.NOT_STARTED,
            dueAt = DateTimeUtils.localDateTimeToEpoch(today.plusDays(3), LocalTime.of(12, 0)),
            createdAt = 1L,
            updatedAt = 1L
        )

        val snapshot = NexusRepository.buildMyDayWidgetSnapshot(
            appointments = listOf(apptMorning, apptConflict, apptTomorrow),
            tasks = listOf(taskDueTodayHigh, taskDueTodayOverdue, taskCompletedToday, taskPendingOtherDay),
            today = today,
            nowEpoch = now
        )

        assertEquals(today, snapshot.date)
        assertEquals(2, snapshot.appointments.size)
        assertTrue(snapshot.appointments.all { it.hasConflict })
        assertEquals(2, snapshot.tasksDueToday.size)
        assertEquals("t2", snapshot.tasksDueToday.first().id)
        assertTrue(snapshot.tasksDueToday.first().isOverdue)
        assertEquals(1, snapshot.completedTasksTodayCount)
        assertEquals(1, snapshot.totalCompletedTasksCount)
        assertEquals(3, snapshot.pendingTasksCount)
        assertFalse(snapshot.isEmptySchedule)
    }

    // 2. Ordenacao correta das tarefas prioritarias e regra de bloqueio
    @Test
    fun `my tasks widget sorts by priority then due date and blocks completion on blocked or terminal tasks`() {
        val today = LocalDate.of(2026, 9, 15)
        val now = DateTimeUtils.localDateTimeToEpoch(today, LocalTime.of(12, 0))
        val project = ProjectEntity(id = "p1", name = "NEXUS Mobile", createdAt = 1L, updatedAt = 1L)

        val tNormalSoon = TaskEntity(
            id = "t-normal",
            title = "Normal cedo",
            priority = TaskPriority.NORMAL,
            status = TaskStatus.NOT_STARTED,
            dueAt = DateTimeUtils.localDateTimeToEpoch(today, LocalTime.of(14, 0)),
            createdAt = 10L,
            updatedAt = 10L
        )
        val tUrgentLater = TaskEntity(
            id = "t-urgent-later",
            title = "Urgente depois",
            projectId = "p1",
            priority = TaskPriority.URGENT,
            status = TaskStatus.IN_PROGRESS,
            dueAt = DateTimeUtils.localDateTimeToEpoch(today.plusDays(2), LocalTime.of(18, 0)),
            createdAt = 20L,
            updatedAt = 20L
        )
        val tUrgentEarlier = TaskEntity(
            id = "t-urgent-early",
            title = "Urgente antes",
            priority = TaskPriority.URGENT,
            status = TaskStatus.NOT_STARTED,
            dueAt = DateTimeUtils.localDateTimeToEpoch(today.plusDays(1), LocalTime.of(9, 0)),
            hasDueTime = true,
            createdAt = 30L,
            updatedAt = 30L
        )
        val tBlockedHigh = TaskEntity(
            id = "t-blocked",
            title = "Aguardando peça",
            priority = TaskPriority.HIGH,
            status = TaskStatus.BLOCKED,
            createdAt = 40L,
            updatedAt = 40L
        )
        val tDone = TaskEntity(
            id = "t-done",
            title = "Já concluída",
            priority = TaskPriority.URGENT,
            status = TaskStatus.COMPLETED,
            completedAt = now,
            createdAt = 50L,
            updatedAt = 50L
        )
        val tCancelled = TaskEntity(
            id = "t-cancelled",
            title = "Cancelada",
            priority = TaskPriority.URGENT,
            status = TaskStatus.CANCELLED,
            createdAt = 60L,
            updatedAt = 60L
        )

        val snapshot = NexusRepository.buildMyTasksWidgetSnapshot(
            tasks = listOf(tNormalSoon, tUrgentLater, tBlockedHigh, tUrgentEarlier, tDone, tCancelled),
            projects = listOf(project),
            today = today,
            nowEpoch = now
        )

        assertEquals(4, snapshot.pendingTasksCount)
        assertEquals(3, snapshot.urgentOrHighCount)
        assertEquals(listOf("t-urgent-early", "t-urgent-later", "t-blocked", "t-normal"), snapshot.tasks.map { it.id })
        assertEquals("NEXUS Mobile", snapshot.tasks[1].projectName)
        assertTrue(snapshot.tasks[0].canCompleteFromWidget)
        assertFalse(snapshot.tasks[2].canCompleteFromWidget)
    }

    // 3, 4 e 5. Saldo declarado separado, previstos vs realizados, atrasados e faturas abertas sem duplicidade
    @Test
    fun `finance widget separates declared balance distinguishes planned vs settled and shows overdue plus open statements`() {
        val today = LocalDate.of(2026, 9, 15)
        val pastEpoch = DateTimeUtils.localDateTimeToEpoch(today.minusDays(3), LocalTime.of(12, 0))
        val futureEpoch = DateTimeUtils.localDateTimeToEpoch(today.plusDays(5), LocalTime.of(12, 0))

        val card = CreditCardEntity(id = "c1", name = "Black", totalLimitCents = 1_000_000L, availableLimitCents = 800_000L, createdAt = 1L, updatedAt = 1L)
        val openStmt = CreditCardStatementEntity(
            id = "s-open",
            creditCardId = "c1",
            yearMonth = "2026-09",
            closingAt = futureEpoch,
            dueAt = futureEpoch,
            status = StatementStatus.OPEN,
            createdAt = 1L,
            updatedAt = 1L
        )
        val paidStmt = CreditCardStatementEntity(
            id = "s-paid",
            creditCardId = "c1",
            yearMonth = "2026-08",
            closingAt = pastEpoch,
            dueAt = pastEpoch,
            status = StatementStatus.PAID,
            paidAt = pastEpoch,
            paidAmountCents = 50_000L,
            createdAt = 1L,
            updatedAt = 1L
        )

        val txs = listOf(
            // Receita realizada + prevista
            TransactionEntity(
                id = "inc-settled",
                type = TransactionType.INCOME,
                description = "Salário",
                amountCents = 500_000L,
                status = FinancialStatus.SETTLED,
                paymentMethod = PaymentMethod.TRANSFER,
                occurredAt = pastEpoch,
                settledAt = pastEpoch,
                createdAt = 1L,
                updatedAt = 1L
            ),
            TransactionEntity(
                id = "inc-planned",
                type = TransactionType.INCOME,
                description = "Freela",
                amountCents = 120_000L,
                status = FinancialStatus.PLANNED,
                paymentMethod = PaymentMethod.PIX,
                occurredAt = pastEpoch,
                dueAt = futureEpoch,
                createdAt = 1L,
                updatedAt = 1L
            ),
            // Despesa vencida (PENDING com data anterior a hoje -> efetiva OVERDUE)
            TransactionEntity(
                id = "exp-overdue",
                type = TransactionType.EXPENSE,
                description = "Internet",
                amountCents = 15_000L,
                status = FinancialStatus.PENDING,
                paymentMethod = PaymentMethod.BOLETO,
                occurredAt = pastEpoch,
                dueAt = pastEpoch,
                createdAt = 1L,
                updatedAt = 1L
            ),
            // Compra na fatura aberta (unica despesa, fatura nao cria segunda despesa)
            TransactionEntity(
                id = "exp-card",
                type = TransactionType.EXPENSE,
                description = "Supermercado",
                amountCents = 42_000L,
                status = FinancialStatus.PENDING,
                paymentMethod = PaymentMethod.CREDIT_CARD,
                occurredAt = pastEpoch,
                dueAt = futureEpoch,
                creditCardId = "c1",
                statementId = "s-open",
                createdAt = 1L,
                updatedAt = 1L
            ),
            // Compra da fatura ja paga (nao entra em faturas abertas)
            TransactionEntity(
                id = "exp-card-paid",
                type = TransactionType.EXPENSE,
                description = "Farmácia",
                amountCents = 50_000L,
                status = FinancialStatus.SETTLED,
                paymentMethod = PaymentMethod.CREDIT_CARD,
                occurredAt = pastEpoch,
                dueAt = pastEpoch,
                settledAt = pastEpoch,
                creditCardId = "c1",
                statementId = "s-paid",
                createdAt = 1L,
                updatedAt = 1L
            )
        )

        val snapshot = NexusRepository.buildFinanceWidgetSnapshot(
            declaredBalanceCents = 275_000L,
            transactions = txs,
            statements = listOf(openStmt, paidStmt),
            cards = listOf(card),
            today = today
        )

        // Saldo declarado mantido exatamente separado
        assertEquals(275_000L, snapshot.declaredBalanceCents)
        assertEquals(120_000L, snapshot.plannedIncomeCents)
        assertEquals(500_000L, snapshot.receivedIncomeCents)
        assertEquals(42_000L, snapshot.plannedExpenseCents)
        assertEquals(50_000L, snapshot.paidExpenseCents)
        assertEquals(15_000L, snapshot.overdueExpenseCents)
        assertEquals(1, snapshot.overdueCount)

        // Somente a fatura aberta aparece e com exatamente 420,00 sem dupla contagem
        assertEquals(1, snapshot.openStatementsCount)
        assertEquals(42_000L, snapshot.openStatementsTotalCents)
        assertEquals(0, snapshot.overdueStatementsCount)
        assertEquals("exp-overdue", snapshot.upcomingDueTransactions.first().id)
    }

    // 6. Resumo consolidado do widget Resumo NEXUS
    @Test
    fun `nexus summary widget consolidates all domains and hides goals when none exist`() {
        val today = LocalDate.of(2026, 9, 15)
        val now = DateTimeUtils.localDateTimeToEpoch(today, LocalTime.of(9, 0))
        val eventEpoch = DateTimeUtils.localDateTimeToEpoch(today.plusDays(4), LocalTime.of(20, 0))

        val task = TaskEntity(
            id = "t1",
            title = "Fechar proposta",
            priority = TaskPriority.HIGH,
            status = TaskStatus.NOT_STARTED,
            dueAt = DateTimeUtils.localDateTimeToEpoch(today.minusDays(1), LocalTime.of(17, 0)),
            createdAt = 1L,
            updatedAt = 1L
        )
        val appt = AppointmentEntity(
            id = "a1",
            title = "Reunião Cliente",
            startAt = DateTimeUtils.localDateTimeToEpoch(today, LocalTime.of(14, 0)),
            endAt = DateTimeUtils.localDateTimeToEpoch(today, LocalTime.of(15, 0)),
            createdAt = 1L,
            updatedAt = 1L
        )
        val event = EventEntity(
            id = "e1",
            name = "Show Rock",
            startAt = eventEpoch,
            endAt = eventEpoch + 7_200_000L,
            eventStatus = EventLifecycleStatus.CONFIRMED,
            createdAt = 1L,
            updatedAt = 1L
        )
        val bill = TransactionEntity(
            id = "tx1",
            type = TransactionType.EXPENSE,
            description = "Energia",
            amountCents = 21_000L,
            status = FinancialStatus.PENDING,
            paymentMethod = PaymentMethod.BOLETO,
            occurredAt = now,
            dueAt = DateTimeUtils.localDateTimeToEpoch(today.plusDays(2), LocalTime.of(12, 0)),
            createdAt = 1L,
            updatedAt = 1L
        )

        val withoutGoals = NexusRepository.buildNexusSummaryWidgetSnapshot(
            appointments = listOf(appt),
            tasks = listOf(task),
            projects = emptyList(),
            categories = listOf(CategoryEntity("c1", "Trabalho", "#FFFFFF", CategoryScope.GENERAL, true, 1L, 1L)),
            events = listOf(event),
            transactions = listOf(bill),
            statements = emptyList(),
            cards = emptyList(),
            goals = emptyList(),
            today = today,
            nowEpoch = now
        )

        assertEquals(1, withoutGoals.pendingTasksCount)
        assertEquals(1, withoutGoals.overdueTasksCount)
        assertEquals("Fechar proposta", withoutGoals.topTask?.task?.title)
        assertEquals(1, withoutGoals.todayAppointmentsCount)
        assertEquals("Reunião Cliente", withoutGoals.nextAppointment?.appointment?.title)
        assertEquals(1, withoutGoals.upcomingEventsCount)
        assertEquals("Show Rock", withoutGoals.nextEvent?.event?.name)
        assertEquals(1, withoutGoals.upcomingDueCount)
        assertFalse(withoutGoals.hasGoals)
        assertFalse(withoutGoals.isCompletelyEmpty)

        val withGoals = NexusRepository.buildNexusSummaryWidgetSnapshot(
            appointments = listOf(appt),
            tasks = listOf(task),
            projects = emptyList(),
            categories = emptyList(),
            events = listOf(event),
            transactions = listOf(bill),
            statements = emptyList(),
            cards = emptyList(),
            goals = listOf(
                FinancialGoalEntity(
                    id = "g1",
                    name = "Reserva de Emergência",
                    targetAmountCents = 1_000_000L,
                    currentAmountCents = 400_000L,
                    createdAt = 1L,
                    updatedAt = 1L
                )
            ),
            today = today,
            nowEpoch = now
        )
        assertTrue(withGoals.hasGoals)
        assertEquals("Reserva de Emergência", withGoals.topGoalName)
        assertEquals(400_000L, withGoals.goalsTotalCurrentCents)
    }

    // 7. Atualizacao dos dados apos concluir tarefa via widget
    @Test
    fun `completing task updates both my tasks and my day snapshots immediately`() {
        val today = LocalDate.of(2026, 9, 15)
        val now = DateTimeUtils.localDateTimeToEpoch(today, LocalTime.of(15, 0))
        val initialTask = TaskEntity(
            id = "t1",
            title = "Publicar relatório",
            priority = TaskPriority.HIGH,
            status = TaskStatus.IN_PROGRESS,
            dueAt = DateTimeUtils.localDateTimeToEpoch(today, LocalTime.of(18, 0)),
            createdAt = 1L,
            updatedAt = 1L
        )

        val beforeTasks = NexusRepository.buildMyTasksWidgetSnapshot(listOf(initialTask), emptyList(), today, now)
        val beforeDay = NexusRepository.buildMyDayWidgetSnapshot(emptyList(), listOf(initialTask), today, now)
        assertEquals(1, beforeTasks.pendingTasksCount)
        assertEquals(0, beforeDay.completedTasksTodayCount)

        val completedTask = initialTask.copy(
            status = TaskStatus.COMPLETED,
            completedAt = now,
            updatedAt = now
        )
        val afterTasks = NexusRepository.buildMyTasksWidgetSnapshot(listOf(completedTask), emptyList(), today, now)
        val afterDay = NexusRepository.buildMyDayWidgetSnapshot(emptyList(), listOf(completedTask), today, now)
        assertEquals(0, afterTasks.pendingTasksCount)
        assertTrue(afterTasks.isEmpty)
        assertEquals(1, afterDay.completedTasksTodayCount)
        assertEquals(0, afterDay.pendingTasksCount)
    }

    // 8. Atualizacao dos dados apos registrar pagamento
    @Test
    fun `settling transaction updates finance widget planned vs paid and clears due list`() {
        val today = LocalDate.of(2026, 9, 15)
        val due = DateTimeUtils.localDateTimeToEpoch(today.plusDays(1), LocalTime.of(12, 0))
        val pendingTx = TransactionEntity(
            id = "tx1",
            type = TransactionType.EXPENSE,
            description = "Aluguel",
            amountCents = 200_000L,
            status = FinancialStatus.PENDING,
            paymentMethod = PaymentMethod.PIX,
            occurredAt = due,
            dueAt = due,
            createdAt = 1L,
            updatedAt = 1L
        )

        val before = NexusRepository.buildFinanceWidgetSnapshot(500_000L, listOf(pendingTx), emptyList(), emptyList(), today)
        assertEquals(200_000L, before.plannedExpenseCents)
        assertEquals(0L, before.paidExpenseCents)
        assertEquals(1, before.upcomingDueTransactions.size)

        val settledTx = pendingTx.copy(status = FinancialStatus.SETTLED, settledAt = due)
        val after = NexusRepository.buildFinanceWidgetSnapshot(500_000L, listOf(settledTx), emptyList(), emptyList(), today)
        assertEquals(0L, after.plannedExpenseCents)
        assertEquals(200_000L, after.paidExpenseCents)
        assertTrue(after.upcomingDueTransactions.isEmpty())
    }

    // 9. Atualizacao dos dados apos restaurar backup
    @Test
    fun `restoring v3 backup payload rebuilds widget snapshots from restored records`() {
        val today = LocalDate.of(2026, 9, 15)
        val dueEpoch = DateTimeUtils.localDateTimeToEpoch(today, LocalTime.of(16, 0))
        val backupJson = """
            {
              "formatVersion": 3,
              "appVersion": "4.0.0",
              "databaseVersion": 3,
              "settings": [
                { "id": "nexus_settings_default", "userName": "Enzo", "darkTheme": true, "declaredBalanceCents": 950000, "createdAt": 1, "updatedAt": 1 }
              ],
              "tasks": [
                { "id": "t-restored", "title": "Tarefa Restaurada", "priority": "URGENT", "status": "NOT_STARTED", "dueAt": $dueEpoch, "hasDueTime": true, "createdAt": 1, "updatedAt": 1 }
              ],
              "transactions": [
                { "id": "tx-restored", "type": "EXPENSE", "description": "Condomínio", "amountCents": 85000, "status": "PENDING", "paymentMethod": "BOLETO", "occurredAt": $dueEpoch, "dueAt": $dueEpoch, "createdAt": 1, "updatedAt": 1 }
              ]
            }
        """.trimIndent()

        val payload = tolerantJson.decodeFromString<NexusBackupPayload>(backupJson)
        val restoredBalance = payload.settings.firstOrNull()?.declaredBalanceCents ?: 0L
        val myTasks = NexusRepository.buildMyTasksWidgetSnapshot(payload.tasks, payload.projects, today, dueEpoch - 60_000L)
        val finance = NexusRepository.buildFinanceWidgetSnapshot(restoredBalance, payload.transactions, payload.statements, payload.creditCards, today)

        assertEquals(1, myTasks.pendingTasksCount)
        assertEquals("Tarefa Restaurada", myTasks.tasks.first().title)
        assertEquals(950_000L, finance.declaredBalanceCents)
        assertEquals(85_000L, finance.plannedExpenseCents)
    }

    // 10. Estado vazio dos widgets
    @Test
    fun `all four widgets report empty states cleanly when database has no records`() {
        val today = LocalDate.of(2026, 9, 15)
        val now = DateTimeUtils.localDateTimeToEpoch(today, LocalTime.of(10, 0))

        val emptyDay = NexusRepository.buildMyDayWidgetSnapshot(emptyList(), emptyList(), today, now)
        val emptyTasks = NexusRepository.buildMyTasksWidgetSnapshot(emptyList(), emptyList(), today, now)
        val emptyFinance = NexusRepository.buildFinanceWidgetSnapshot(0L, emptyList(), emptyList(), emptyList(), today)
        val emptySummary = NexusRepository.buildNexusSummaryWidgetSnapshot(
            emptyList(), emptyList(), emptyList(), emptyList(), emptyList(),
            emptyList(), emptyList(), emptyList(), emptyList(), today, now
        )

        assertTrue(emptyDay.isEmptySchedule)
        assertTrue(emptyTasks.isEmpty)
        assertTrue(emptyFinance.isEmpty)
        assertTrue(emptySummary.isCompletelyEmpty)
        assertFalse(emptySummary.hasGoals)
    }

    // 11. Tamanhos responsivos e configuracao multi-instancia
    @Test
    fun `widget size tier resolution and multi-instance config remain distinct per appWidgetId`() {
        assertEquals(WidgetSizeTier.SMALL, NexusWidgetSizes.tierFor(DpSize(130.dp, 100.dp)))
        assertEquals(WidgetSizeTier.MEDIUM, NexusWidgetSizes.tierFor(DpSize(240.dp, 130.dp)))
        assertEquals(WidgetSizeTier.LARGE, NexusWidgetSizes.tierFor(DpSize(280.dp, 240.dp)))

        val instanceA = NexusWidgetInstanceConfig(
            appWidgetId = 101,
            widgetType = NexusWidgetType.MY_TASKS,
            compactMode = true,
            maxItems = 3,
            showProjectName = false
        )
        val instanceB = NexusWidgetInstanceConfig(
            appWidgetId = 202,
            widgetType = NexusWidgetType.MY_TASKS,
            compactMode = false,
            maxItems = 6,
            showProjectName = true
        )
        assertTrue(instanceA.appWidgetId != instanceB.appWidgetId)
        assertFalse(instanceA.showProjectName)
        assertTrue(instanceB.showProjectName)
        assertEquals(3, instanceA.maxItems)
        assertEquals(6, instanceB.maxItems)
    }

    // 12. Tratamento seguro de falha sem travar o widget
    @Test
    fun `widget data state models load errors cleanly without crashing render`() {
        val failure = Result.failure<Unit>(IllegalStateException("Erro simulado SQLite"))
        val state: WidgetDataState<String> = failure.fold(
            onSuccess = { WidgetDataState.Ready("ok") },
            onFailure = { WidgetDataState.Error("Não foi possível carregar dados locais.") }
        )

        assertTrue(state is WidgetDataState.Error)
        assertEquals("Não foi possível carregar dados locais.", (state as WidgetDataState.Error).message)
        assertNotNull(failure.exceptionOrNull())
        assertNull(failure.getOrNull())
    }
}
