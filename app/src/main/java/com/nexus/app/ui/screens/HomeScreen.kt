package com.nexus.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.DashboardCustomize
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexus.app.core.DateTimeUtils
import com.nexus.app.domain.model.AgendaItem
import com.nexus.app.domain.model.CategorySpendPoint
import com.nexus.app.domain.model.DailyBriefingSnapshot
import com.nexus.app.domain.model.DailyQuoteSelection
import com.nexus.app.domain.model.DashboardBlockConfig
import com.nexus.app.domain.model.DashboardBlockType
import com.nexus.app.domain.model.DashboardSnapshot
import com.nexus.app.domain.model.FinancialStatus
import com.nexus.app.domain.model.MonthlyCashflowPoint
import com.nexus.app.domain.model.NexusDateFormat
import com.nexus.app.domain.model.StatementStatus
import com.nexus.app.domain.model.TransactionType
import com.nexus.app.ui.components.AlertBannerCard
import com.nexus.app.ui.components.AlertSeverity
import com.nexus.app.ui.components.NexusCard
import com.nexus.app.ui.components.NexusEmptyState
import com.nexus.app.ui.components.SectionHeader
import com.nexus.app.ui.components.TaskRowCard
import com.nexus.app.ui.components.charts.BarGroupChart
import com.nexus.app.ui.components.charts.BarGroupEntry
import com.nexus.app.ui.components.charts.DonutChart
import com.nexus.app.ui.components.charts.DonutSlice
import com.nexus.app.ui.components.charts.TaskRingChart
import com.nexus.app.ui.components.charts.TaskRingSliceKind
import com.nexus.app.ui.components.parseHexColorOrDefault
import com.nexus.app.ui.theme.LocalNexusPalette

@Composable
fun HomeScreen(
    snapshot: DashboardSnapshot,
    dailyBriefing: DailyBriefingSnapshot,
    onOpenRoutineAssistant: () -> Unit,
    onOpenCustomizeDashboard: () -> Unit,
    onOpenQuoteLibrary: () -> Unit,
    onOpenPlanning: (goalId: String?) -> Unit,
    onOpenTasks: () -> Unit,
    onOpenTasksOverdue: () -> Unit,
    onOpenTask: (String) -> Unit,
    onToggleTaskComplete: (String) -> Unit,
    onOpenAgenda: () -> Unit,
    onOpenAppointment: (String) -> Unit,
    onOpenEvent: (String) -> Unit,
    onOpenFinance: () -> Unit,
    onOpenFinanceCards: () -> Unit,
    onOpenTransaction: (String) -> Unit,
    onQuickAdd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalNexusPalette.current
    val today = DateTimeUtils.todayLocalDate()
    val personalization = snapshot.personalization
    val compact = personalization?.compactDashboardMode ?: false
    val verticalGap = if (compact) 8.dp else 12.dp
    val enabledBlocks = snapshot.blocks.filter { it.enabled }.sortedBy { it.order }
    val quoteBlockConfig = snapshot.blocks.firstOrNull { it.type == DashboardBlockType.DAILY_QUOTE }
    val greetingBlockConfig = snapshot.blocks.firstOrNull { it.type == DashboardBlockType.GREETING }
    val showMergedQuote = (quoteBlockConfig?.enabled ?: true) && (personalization?.showDailyQuoteOnDashboard ?: true)

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWide = maxWidth >= 720.dp

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(verticalGap)
        ) {
            if (enabledBlocks.isEmpty()) {
            item {
                NexusEmptyState(
                    icon = Icons.Outlined.DashboardCustomize,
                    title = "Todos os blocos do dashboard estão ocultos",
                    subtitle = "Seus registros continuam salvos no banco local. Ative ou reordene blocos quando quiser.",
                    actionLabel = "Personalizar dashboard",
                    onAction = onOpenCustomizeDashboard
                )
            }
        } else {
            enabledBlocks.forEach { block ->
                when (block.type) {
                    DashboardBlockType.GREETING -> {
                        if (personalization?.greetingEnabled ?: true) {
                            item(key = "blk_greeting") {
                                GreetingBlock(
                                    userName = snapshot.userName,
                                    greetingTemplate = personalization?.greetingTemplate ?: "AUTO",
                                    formattedDate = DateTimeUtils.formatDateByPattern(
                                        today,
                                        personalization?.dateFormatPattern ?: NexusDateFormat.WEEKDAY_DD_MM_YYYY
                                    ),
                                    showDate = block.showPrimaryDetail,
                                    quoteSelection = if (showMergedQuote) snapshot.dailyQuoteSelection else null,
                                    showQuoteAuthor = quoteBlockConfig?.showPrimaryDetail ?: true,
                                    onOpenQuoteLibrary = onOpenQuoteLibrary,
                                    onCustomize = onOpenCustomizeDashboard
                                )
                            }
                        }
                    }

                    DashboardBlockType.DAILY_QUOTE -> {
                        // A frase diária fica integrada ao cabeçalho de saudação sem card próprio.
                        // Caso a saudação esteja oculta mas a frase habilitada, exibe apenas texto discreto sem card.
                        val greetingHidden = !(greetingBlockConfig?.enabled ?: true) || !(personalization?.greetingEnabled ?: true)
                        if (greetingHidden && showMergedQuote && snapshot.dailyQuoteSelection.quote != null) {
                            item(key = "blk_quote_inline") {
                                InlineHeaderQuoteText(
                                    selection = snapshot.dailyQuoteSelection,
                                    showAuthor = block.showPrimaryDetail,
                                    onClick = onOpenQuoteLibrary
                                )
                            }
                        }
                    }

                    DashboardBlockType.TASK_SUMMARY -> {
                        if ((personalization?.showMetricSummaryOnDashboard ?: true) && block.showPrimaryDetail) {
                            item(key = "blk_task_ring") {
                                val dist = snapshot.taskDistribution
                                NexusCard(modifier = Modifier.fillMaxWidth(), elevated = true) {
                                    SectionHeader(
                                        title = "Tarefas do mês",
                                        actionLabel = "Ver todas",
                                        onAction = onOpenTasks
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    TaskRingChart(
                                        completedCount = dist.completedCount,
                                        inProgressCount = dist.inProgressCount,
                                        pendingCount = dist.pendingCount,
                                        overdueCount = dist.overdueCount,
                                        undatedPendingCount = dist.undatedPendingCount,
                                        periodLabel = DateTimeUtils.formatMonthYear(today),
                                        onSliceClick = { kind ->
                                            when (kind) {
                                                TaskRingSliceKind.COMPLETED,
                                                TaskRingSliceKind.IN_PROGRESS,
                                                TaskRingSliceKind.PENDING -> onOpenTasks()
                                            }
                                        },
                                        onOverdueClick = onOpenTasksOverdue
                                    )
                                }
                            }
                        }

                        if (snapshot.totalItemsInDb == 0) {
                            item(key = "blk_empty_cmd") {
                                NexusEmptyState(
                                    icon = Icons.Outlined.Inbox,
                                    title = "Seu centro de comando está pronto",
                                    subtitle = "Adicione sua primeira tarefa, compromisso, evento ou lançamento financeiro.",
                                    actionLabel = "Criar primeiro registro",
                                    onAction = onQuickAdd
                                )
                            }
                        } else {
                            if (block.showOverdueOrAlerts) {
                                val overdueStmtsCount = snapshot.openStatements.count { it.statement.status == StatementStatus.OVERDUE }
                                val conflictCount = snapshot.todayAppointments.count { it.hasConflict }
                                val hasFinanceAlert = snapshot.financeSummary.overdueExpenseCents > 0L || overdueStmtsCount > 0
                                val hasConflictAlert = conflictCount > 0
                                val financeAlertSubtitle = buildString {
                                    if (snapshot.financeSummary.overdueExpenseCents > 0L) {
                                        append("Contas vencidas: ${DateTimeUtils.formatBrl(snapshot.financeSummary.overdueExpenseCents)}")
                                    }
                                    if (overdueStmtsCount > 0) {
                                        if (isNotEmpty()) append(" · ")
                                        append("$overdueStmtsCount fatura(s) em atraso")
                                    }
                                }
                                val financeAlertClick = if (overdueStmtsCount > 0 && snapshot.financeSummary.overdueExpenseCents == 0L) {
                                    onOpenFinanceCards
                                } else {
                                    onOpenFinance
                                }

                                if (isWide && hasFinanceAlert && hasConflictAlert) {
                                    item(key = "blk_alerts_row") {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Box(modifier = Modifier.weight(1f)) {
                                                AlertBannerCard(
                                                    title = "Atenção financeira",
                                                    subtitle = financeAlertSubtitle,
                                                    severity = AlertSeverity.DANGER,
                                                    onClick = financeAlertClick
                                                )
                                            }
                                            Box(modifier = Modifier.weight(1f)) {
                                                AlertBannerCard(
                                                    title = "Conflito de horário hoje",
                                                    subtitle = "$conflictCount compromisso(s) com sobreposição na Agenda.",
                                                    severity = AlertSeverity.WARNING,
                                                    onClick = onOpenAgenda
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    if (hasFinanceAlert) {
                                        item(key = "blk_alert_finance") {
                                            AlertBannerCard(
                                                title = "Atenção financeira",
                                                subtitle = financeAlertSubtitle,
                                                severity = AlertSeverity.DANGER,
                                                onClick = financeAlertClick
                                            )
                                        }
                                    }
                                    if (hasConflictAlert) {
                                        item(key = "blk_alert_conflict") {
                                            AlertBannerCard(
                                                title = "Conflito de horário hoje",
                                                subtitle = "$conflictCount compromisso(s) com sobreposição na Agenda.",
                                                severity = AlertSeverity.WARNING,
                                                onClick = onOpenAgenda
                                            )
                                        }
                                    }
                                }
                            }

                            if (block.showOverdueOrAlerts && snapshot.overdueTasks.isNotEmpty()) {
                                val limitedOverdue = snapshot.overdueTasks.take(minOf(block.maxItems, 3))
                                item(key = "blk_tasks_overdue_hdr") {
                                    SectionHeader(
                                        title = "Tarefas atrasadas",
                                        trailing = "${snapshot.overdueTasks.size}",
                                        actionLabel = if (snapshot.overdueTasks.size > limitedOverdue.size) "Ver todas" else null,
                                        onAction = onOpenTasksOverdue
                                    )
                                }
                                adaptiveTwoColumnItems(
                                    itemsList = limitedOverdue,
                                    isWide = isWide,
                                    keyPrefix = "overdue",
                                    keyOf = { it.task.id }
                                ) { taskItem ->
                                    TaskRowCard(
                                        item = taskItem,
                                        onClick = { onOpenTask(taskItem.task.id) },
                                        onToggleComplete = { onToggleTaskComplete(taskItem.task.id) }
                                    )
                                }
                            }

                            if (block.showSecondaryDetail) {
                                val limitedPrio = snapshot.priorityTasksToday.take(minOf(block.maxItems, 4))
                                item(key = "blk_tasks_prio_hdr") {
                                    SectionHeader(
                                        title = "Prioridades de hoje",
                                        trailing = if (snapshot.priorityTasksToday.isEmpty()) null else "${snapshot.priorityTasksToday.size}",
                                        actionLabel = "Ver todas",
                                        onAction = onOpenTasks
                                    )
                                }
                                if (limitedPrio.isEmpty()) {
                                    item(key = "blk_tasks_prio_empty") {
                                        NexusCard(modifier = Modifier.fillMaxWidth()) {
                                            Text(
                                                text = "Nenhuma tarefa prioritária ou com prazo para hoje.",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = palette.textSecondary
                                            )
                                        }
                                    }
                                } else {
                                    adaptiveTwoColumnItems(
                                        itemsList = limitedPrio,
                                        isWide = isWide,
                                        keyPrefix = "prio",
                                        keyOf = { it.task.id }
                                    ) { taskItem ->
                                        TaskRowCard(
                                            item = taskItem,
                                            onClick = { onOpenTask(taskItem.task.id) },
                                            onToggleComplete = { onToggleTaskComplete(taskItem.task.id) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    DashboardBlockType.FINANCE_SUMMARY -> {
                        item(key = "blk_finance_summary") {
                            FinanceSummaryBlock(
                                snapshot = snapshot,
                                config = block,
                                onOpenFinance = onOpenFinance,
                                onOpenTransaction = onOpenTransaction
                            )
                        }
                    }

                    DashboardBlockType.CHARTS_INDICATORS -> {
                        item(key = "blk_charts") {
                            DashboardChartsBlock(
                                snapshot = snapshot,
                                showCashflow = block.showPrimaryDetail,
                                showCategoryBreakdown = block.showSecondaryDetail,
                                isWide = isWide,
                                onOpenFinance = onOpenFinance
                            )
                        }
                    }

                    DashboardBlockType.TODAY_AGENDA -> {
                        if (block.showPrimaryDetail) {
                            item(key = "blk_routine") {
                                val billsDueCount = dailyBriefing.billsDueToday.size + dailyBriefing.overdueBills.size
                                NexusCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    elevated = true,
                                    onClick = onOpenRoutineAssistant
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                            Icon(
                                                imageVector = Icons.Outlined.NotificationsActive,
                                                contentDescription = null,
                                                tint = palette.highlight
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = "Assistente de Rotina",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    color = palette.textPrimary
                                                )
                                                Text(
                                                    text = "${dailyBriefing.todayAppointments.size} compromissos · ${dailyBriefing.todayTasks.size} tarefas · $billsDueCount contas",
                                                    style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                                                    color = palette.textSecondary
                                                )
                                            }
                                        }
                                        TextButton(onClick = onOpenRoutineAssistant) {
                                            Text(
                                                text = if (dailyBriefing.isReviewedToday) "Revisado" else "Abrir",
                                                color = if (dailyBriefing.isReviewedToday) palette.positive else palette.info
                                            )
                                        }
                                    }
                                }
                            }

                            item(key = "blk_next_appt") {
                                SectionHeader(
                                    title = "Próximo compromisso",
                                    actionLabel = "Agenda",
                                    onAction = onOpenAgenda
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                val next = snapshot.nextAppointment
                                if (next == null) {
                                    NexusCard(modifier = Modifier.fillMaxWidth()) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Outlined.CalendarToday,
                                                contentDescription = null,
                                                tint = palette.textSecondary
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(
                                                text = "Nenhum compromisso futuro agendado.",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = palette.textSecondary
                                            )
                                        }
                                    }
                                } else {
                                    AppointmentCompactCard(
                                        item = next,
                                        showConflictAlert = block.showOverdueOrAlerts,
                                        onClick = { onOpenAppointment(next.appointment.id) }
                                    )
                                }
                            }
                        }

                        if (block.showSecondaryDetail && snapshot.todayAppointments.isNotEmpty()) {
                            val limitedAppts = snapshot.todayAppointments.take(block.maxItems)
                            item(key = "blk_today_appts_hdr") {
                                SectionHeader(
                                    title = "Compromissos de hoje",
                                    trailing = "${snapshot.todayAppointments.size}",
                                    actionLabel = if (snapshot.todayAppointments.size > limitedAppts.size) "Ver agenda" else null,
                                    onAction = onOpenAgenda
                                )
                            }
                            adaptiveTwoColumnItems(
                                itemsList = limitedAppts,
                                isWide = isWide,
                                keyPrefix = "today_appt",
                                keyOf = { it.id }
                            ) { appt ->
                                AppointmentCompactCard(
                                    item = appt,
                                    showConflictAlert = block.showOverdueOrAlerts,
                                    onClick = { onOpenAppointment(appt.appointment.id) }
                                )
                            }
                        }
                    }

                    DashboardBlockType.UPCOMING_EVENTS -> {
                        if (snapshot.upcomingEvents.isNotEmpty()) {
                            val limitedEvents = snapshot.upcomingEvents.take(block.maxItems)
                            item(key = "blk_events_hdr") {
                                SectionHeader(
                                    title = "Próximos eventos",
                                    trailing = "${snapshot.upcomingEvents.size}"
                                )
                            }
                            adaptiveTwoColumnItems(
                                itemsList = limitedEvents,
                                isWide = isWide,
                                keyPrefix = "home_ev",
                                keyOf = { it.event.id }
                            ) { ew ->
                                EventSummaryCard(
                                    item = ew,
                                    onClick = { onOpenEvent(ew.event.id) }
                                )
                            }
                        }
                    }

                    DashboardBlockType.UPCOMING_PAYMENTS -> {
                        val hasDue = snapshot.upcomingPayments.isNotEmpty()
                        val hasStmts = block.showSecondaryDetail && snapshot.openStatements.isNotEmpty()
                        if (hasDue || hasStmts) {
                            item(key = "blk_upcoming_pay_hdr") {
                                SectionHeader(
                                    title = "Próximos pagamentos",
                                    trailing = "${snapshot.upcomingPayments.size + if (hasStmts) snapshot.openStatements.size else 0}",
                                    actionLabel = "Finanças",
                                    onAction = onOpenFinance
                                )
                            }
                            adaptiveTwoColumnItems(
                                itemsList = snapshot.upcomingPayments.take(block.maxItems),
                                isWide = isWide,
                                keyPrefix = "up_tx",
                                keyOf = { it.transaction.id }
                            ) { tw ->
                                val tx = tw.transaction
                                val isOverdue = tx.status == FinancialStatus.OVERDUE
                                NexusCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    elevated = true,
                                    onClick = { onOpenTransaction(tx.id) }
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                            Icon(
                                                imageVector = Icons.Outlined.ReceiptLong,
                                                contentDescription = null,
                                                tint = if (isOverdue && block.showOverdueOrAlerts) palette.danger else palette.textSecondary
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(tx.description, style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
                                                Text(
                                                    text = buildString {
                                                        tx.dueAt?.let { append("Vence ${DateTimeUtils.formatDate(it)}") }
                                                        if (isOverdue) append(" · Em atraso")
                                                    },
                                                    style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                                                    color = if (isOverdue && block.showOverdueOrAlerts) palette.danger else palette.textSecondary
                                                )
                                            }
                                        }
                                        if (block.showPrimaryDetail) {
                                            val sign = if (tx.type == TransactionType.INCOME) "+" else "-"
                                            Text(
                                                text = "$sign${DateTimeUtils.formatBrl(tx.amountCents)}",
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontFeatureSettings = "tnum"
                                                ),
                                                color = if (tx.type == TransactionType.INCOME) palette.positive else palette.textPrimary
                                            )
                                        }
                                    }
                                }
                            }
                            if (hasStmts) {
                                adaptiveTwoColumnItems(
                                    itemsList = snapshot.openStatements.take(block.maxItems),
                                    isWide = isWide,
                                    keyPrefix = "up_stmt",
                                    keyOf = { it.statement.id }
                                ) { sw ->
                                    val isOverdueStmt = sw.statement.status == StatementStatus.OVERDUE
                                    NexusCard(
                                        modifier = Modifier.fillMaxWidth(),
                                        elevated = true,
                                        onClick = onOpenFinanceCards
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                Icon(
                                                    imageVector = Icons.Outlined.CreditCard,
                                                    contentDescription = null,
                                                    tint = if (isOverdueStmt && block.showOverdueOrAlerts) palette.danger else palette.highlight
                                                )
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column {
                                                    Text(
                                                        text = "Fatura ${sw.card.name} (${sw.statement.yearMonth})",
                                                        style = MaterialTheme.typography.titleMedium,
                                                        color = palette.textPrimary
                                                    )
                                                    Text(
                                                        text = "Vence ${DateTimeUtils.formatDate(sw.statement.dueAt)} · ${sw.statement.status.label}",
                                                        style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                                                        color = if (isOverdueStmt && block.showOverdueOrAlerts) palette.danger else palette.textSecondary
                                                    )
                                                }
                                            }
                                            if (block.showPrimaryDetail) {
                                                Text(
                                                    text = DateTimeUtils.formatBrl(sw.totalCents),
                                                    style = MaterialTheme.typography.titleMedium.copy(
                                                        fontWeight = FontWeight.SemiBold,
                                                        fontFeatureSettings = "tnum"
                                                    ),
                                                    color = if (isOverdueStmt && block.showOverdueOrAlerts) palette.danger else palette.textPrimary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    DashboardBlockType.GOALS -> {
                        val hasPersonal = block.showPrimaryDetail && snapshot.personalGoals.isNotEmpty()
                        val hasFinancial = block.showSecondaryDetail && snapshot.financialGoals.isNotEmpty()
                        item(key = "blk_goals_hdr") {
                            SectionHeader(
                                title = "Objetivos e metas",
                                trailing = "${snapshot.personalGoals.size + snapshot.financialGoals.size}",
                                actionLabel = "Planejar",
                                onAction = { onOpenPlanning(null) }
                            )
                        }
                        if (!hasPersonal && !hasFinancial) {
                            item(key = "blk_goals_empty") {
                                NexusCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = { onOpenPlanning(null) }
                                ) {
                                    Text(
                                        text = "Nenhum objetivo ativo ainda. Toque para abrir Planejamento e Objetivos.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = palette.textSecondary
                                    )
                                }
                            }
                        } else {
                            if (hasPersonal) {
                                adaptiveTwoColumnItems(
                                    itemsList = snapshot.personalGoals.take(block.maxItems),
                                    isWide = isWide,
                                    keyPrefix = "home_pg",
                                    keyOf = { it.goal.id }
                                ) { pg ->
                                    PersonalGoalCard(
                                        item = pg,
                                        onClick = { onOpenPlanning(pg.goal.id) }
                                    )
                                }
                            }
                            if (hasFinancial) {
                                adaptiveTwoColumnItems(
                                    itemsList = snapshot.financialGoals.take(block.maxItems),
                                    isWide = isWide,
                                    keyPrefix = "home_fg",
                                    keyOf = { it.id }
                                ) { fg ->
                                    val fraction = if (fg.targetAmountCents > 0L) {
                                        (fg.currentAmountCents.toFloat() / fg.targetAmountCents.toFloat()).coerceIn(0f, 1f)
                                    } else 0f
                                    NexusCard(
                                        modifier = Modifier.fillMaxWidth(),
                                        elevated = true,
                                        onClick = onOpenFinance
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(fg.name, style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
                                            Text(
                                                "${(fraction * 100).toInt()}%",
                                                style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
                                                color = palette.positive
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        LinearProgressIndicator(
                                            progress = { fraction },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(6.dp)
                                                .clip(RoundedCornerShape(4.dp)),
                                            color = palette.accentNeon,
                                            trackColor = palette.chartTrack
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "${DateTimeUtils.formatBrl(fg.currentAmountCents)} de ${DateTimeUtils.formatBrl(fg.targetAmountCents)}",
                                            style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                                            color = palette.textSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

            item(key = "blk_footer_customize") {
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = if (isWide) Alignment.CenterStart else Alignment.Center
                ) {
                    OutlinedButton(
                        onClick = onOpenCustomizeDashboard,
                        border = BorderStroke(1.dp, palette.border),
                        shape = RoundedCornerShape(10.dp),
                        modifier = if (isWide) Modifier.widthIn(min = 240.dp) else Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Outlined.DashboardCustomize, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Personalizar painel", color = palette.textPrimary)
                    }
                }
            }
        }
    }
}

private fun <T> LazyListScope.adaptiveTwoColumnItems(
    itemsList: List<T>,
    isWide: Boolean,
    keyPrefix: String,
    keyOf: (T) -> Any,
    itemContent: @Composable (T) -> Unit
) {
    if (!isWide) {
        items(itemsList, key = { "${keyPrefix}_${keyOf(it)}" }) { item ->
            itemContent(item)
        }
    } else {
        items(
            items = itemsList.chunked(2),
            key = { row -> "${keyPrefix}_row_${keyOf(row.first())}" }
        ) { pair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    itemContent(pair[0])
                }
                if (pair.size > 1) {
                    Box(modifier = Modifier.weight(1f)) {
                        itemContent(pair[1])
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun GreetingBlock(
    userName: String,
    greetingTemplate: String,
    formattedDate: String,
    showDate: Boolean,
    quoteSelection: DailyQuoteSelection?,
    showQuoteAuthor: Boolean,
    onOpenQuoteLibrary: () -> Unit,
    onCustomize: () -> Unit
) {
    val palette = LocalNexusPalette.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = DateTimeUtils.formatGreeting(userName, greetingTemplate),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 20.sp,
                    lineHeight = 26.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = palette.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = onCustomize,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.DashboardCustomize,
                    contentDescription = "Personalizar dashboard",
                    tint = palette.textSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        if (showDate) {
            Text(
                text = formattedDate,
                style = MaterialTheme.typography.bodyMedium,
                color = palette.textSecondary
            )
        }
        if (quoteSelection != null && quoteSelection.quote != null) {
            Spacer(modifier = Modifier.height(2.dp))
            InlineHeaderQuoteText(
                selection = quoteSelection,
                showAuthor = showQuoteAuthor,
                onClick = onOpenQuoteLibrary
            )
        }
    }
}

@Composable
private fun InlineHeaderQuoteText(
    selection: DailyQuoteSelection,
    showAuthor: Boolean,
    onClick: () -> Unit
) {
    val palette = LocalNexusPalette.current
    val quote = selection.quote ?: return
    val text = buildString {
        append("\"${quote.text}\"")
        if (showAuthor && quote.author.isNotBlank()) {
            append(" — ${quote.author}")
        }
    }
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
        color = palette.textSecondary,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp)
    )
}

@Composable
private fun FinanceSummaryBlock(
    snapshot: DashboardSnapshot,
    config: DashboardBlockConfig,
    onOpenFinance: () -> Unit,
    onOpenTransaction: (String) -> Unit
) {
    val palette = LocalNexusPalette.current
    val finance = snapshot.financeSummary
    val monthResult = finance.monthResultCents

    NexusCard(
        modifier = Modifier.fillMaxWidth(),
        elevated = true,
        onClick = onOpenFinance
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Resultado do mês (realizado)",
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.textSecondary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = DateTimeUtils.formatBrl(monthResult),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontFeatureSettings = "tnum"
                    ),
                    color = if (monthResult >= 0L) palette.positive else palette.danger
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Saldo informado: ${DateTimeUtils.formatBrl(finance.declaredBalanceCents)}",
                    style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                    color = palette.textSecondary
                )
            }
            TextButton(onClick = onOpenFinance) {
                Text("Finanças", color = palette.info)
            }
        }

        if (config.showPrimaryDetail) {
            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = palette.border)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Receitas realizadas", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                    Text(
                        text = DateTimeUtils.formatBrl(finance.receivedIncomeCents),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontFeatureSettings = "tnum"
                        ),
                        color = palette.positive
                    )
                    if (finance.plannedIncomeCents > 0L) {
                        Text(
                            text = "Previsto: +${DateTimeUtils.formatBrl(finance.plannedIncomeCents)}",
                            style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                            color = palette.textSecondary
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Despesas pagas", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                    Text(
                        text = DateTimeUtils.formatBrl(finance.paidExpenseCents),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontFeatureSettings = "tnum"
                        ),
                        color = palette.textPrimary
                    )
                    if (finance.pendingExpenseCents > 0L) {
                        Text(
                            text = "A pagar: ${DateTimeUtils.formatBrl(finance.pendingExpenseCents)}",
                            style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                            color = palette.textSecondary
                        )
                    }
                }
            }
        }

        if (config.showOverdueOrAlerts && finance.overdueExpenseCents > 0L) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Vencidas sem pagamento: ${DateTimeUtils.formatBrl(finance.overdueExpenseCents)}",
                style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                color = palette.danger
            )
        }

        if (config.showSecondaryDetail) {
            finance.nextDueTransaction?.let { nextDue ->
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Próximo venc.: ${nextDue.transaction.description} (${DateTimeUtils.formatBrl(nextDue.transaction.amountCents)})",
                    style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                    color = palette.warning,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onOpenTransaction(nextDue.transaction.id) }
                        .padding(vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun DashboardChartsBlock(
    snapshot: DashboardSnapshot,
    showCashflow: Boolean,
    showCategoryBreakdown: Boolean,
    isWide: Boolean,
    onOpenFinance: () -> Unit
) {
    val palette = LocalNexusPalette.current
    val points = snapshot.financeSummary.monthlyCashflow
    val categories = snapshot.financeSummary.categorySpend
    if (!showCashflow && !showCategoryBreakdown) return

    var selectedMonthId by remember(points) { mutableStateOf(points.lastOrNull()?.yearMonthIso) }
    var selectedCategoryKey by remember { mutableStateOf<String?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader(
            title = "Visão financeira (6 meses & categorias)",
            actionLabel = "Detalhes",
            onAction = onOpenFinance
        )

        if (isWide && showCashflow && showCategoryBreakdown) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    HomeCashflowCard(
                        points = points,
                        selectedMonthId = selectedMonthId,
                        onSelectMonth = { selectedMonthId = it }
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    HomeCategoryDonutCard(
                        categories = categories,
                        selectedCategoryKey = selectedCategoryKey,
                        onSelectCategory = { selectedCategoryKey = it }
                    )
                }
            }
        } else {
            if (showCashflow) {
                HomeCashflowCard(
                    points = points,
                    selectedMonthId = selectedMonthId,
                    onSelectMonth = { selectedMonthId = it }
                )
            }

            if (showCategoryBreakdown) {
                HomeCategoryDonutCard(
                    categories = categories,
                    selectedCategoryKey = selectedCategoryKey,
                    onSelectCategory = { selectedCategoryKey = it }
                )
            }
        }
    }
}

@Composable
private fun HomeCashflowCard(
    points: List<MonthlyCashflowPoint>,
    selectedMonthId: String?,
    onSelectMonth: (String?) -> Unit
) {
    val palette = LocalNexusPalette.current
    NexusCard(modifier = Modifier.fillMaxWidth(), elevated = true) {
        Text(
            text = "Receitas vs Despesas (realizado)",
            style = MaterialTheme.typography.titleSmall,
            color = palette.textPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        val barEntries = points.map { pt ->
            BarGroupEntry(
                id = pt.yearMonthIso,
                label = pt.yearMonthLabel,
                primaryValueCents = pt.receivedIncomeCents,
                secondaryValueCents = pt.paidExpenseCents
            )
        }
        BarGroupChart(
            entries = barEntries,
            primaryLabel = "Receitas",
            secondaryLabel = "Despesas",
            selectedEntryId = selectedMonthId,
            onSelectEntry = { entry -> onSelectMonth(entry.id) }
        )
    }
}

@Composable
private fun HomeCategoryDonutCard(
    categories: List<CategorySpendPoint>,
    selectedCategoryKey: String?,
    onSelectCategory: (String?) -> Unit
) {
    val palette = LocalNexusPalette.current
    NexusCard(modifier = Modifier.fillMaxWidth(), elevated = true) {
        Text(
            text = "Gastos por categoria (mês)",
            style = MaterialTheme.typography.titleSmall,
            color = palette.textPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        val totalSpend = categories.sumOf { it.totalCents }
        val slices = categories.mapIndexed { index, cat ->
            val fallbackColor = palette.chartPalette[index % palette.chartPalette.size]
            val resolvedColor = if (cat.categoryId == "__OTHERS__") {
                palette.chartPalette.last()
            } else {
                parseHexColorOrDefault(cat.colorHex, fallbackColor)
            }
            val pct = if (totalSpend > 0L) (cat.totalCents * 100f) / totalSpend.toFloat() else 0f
            DonutSlice(
                id = cat.categoryId ?: cat.categoryName,
                label = cat.categoryName,
                valueCents = cat.totalCents,
                color = resolvedColor,
                percentage = pct
            )
        }
        DonutChart(
            slices = slices,
            totalCents = totalSpend,
            centerLabel = "Gastos do mês",
            selectedSliceId = selectedCategoryKey,
            onSliceClick = onSelectCategory,
            emptyMessage = "Sem despesas lançadas neste mês."
        )
    }
}

@Composable
fun AppointmentCompactCard(
    item: AgendaItem.AppointmentItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showConflictAlert: Boolean = true
) {
    val palette = LocalNexusPalette.current
    val appt = item.appointment
    val timeSummary = if (appt.isAllDay) {
        "${DateTimeUtils.formatDate(appt.startAt)} · Dia inteiro"
    } else {
        "${DateTimeUtils.formatDate(appt.startAt)} · ${DateTimeUtils.formatTime(appt.startAt)}–${DateTimeUtils.formatTime(appt.endAt)}"
    }

    NexusCard(
        modifier = modifier.fillMaxWidth(),
        elevated = true,
        onClick = onClick
    ) {
        Text(
            text = appt.title,
            style = MaterialTheme.typography.titleMedium,
            color = palette.textPrimary
        )
        Spacer(modifier = Modifier.height(4.dp))
        val meta = buildList {
            add(timeSummary)
            item.category?.let { add(it.name) }
            if (appt.location.isNotBlank()) add(appt.location)
        }.joinToString(" · ")
        Text(
            text = meta,
            style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
            color = palette.textSecondary
        )
        if (showConflictAlert && item.hasConflict) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.WarningAmber,
                    contentDescription = "Conflito de horário",
                    tint = palette.warning
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Conflito com: ${item.conflictTitles.joinToString(", ")}",
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.warning
                )
            }
        }
    }
}
