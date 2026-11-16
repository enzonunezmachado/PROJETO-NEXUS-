package com.nexus.app.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.nexus.app.core.DateTimeUtils
import com.nexus.app.data.db.entity.BriefingHistoryEntity
import com.nexus.app.data.db.entity.ReminderEntity
import com.nexus.app.data.db.entity.RoutineAssistantSettingsEntity
import com.nexus.app.domain.model.BriefingType
import com.nexus.app.domain.model.DailyBriefingSnapshot
import com.nexus.app.domain.model.NightReviewSnapshot
import com.nexus.app.domain.model.ReminderStatus
import com.nexus.app.notifications.NexusNotificationScheduler
import com.nexus.app.ui.components.NexusCard
import com.nexus.app.ui.components.NexusEmptyState
import com.nexus.app.ui.components.NexusTimePickerDialog
import com.nexus.app.ui.components.SectionHeader
import com.nexus.app.ui.components.TaskRowCard
import com.nexus.app.ui.theme.LocalNexusPalette
import java.time.LocalTime
import java.util.Locale

private enum class RoutineTab(val label: String) {
    DAILY_BRIEFING("Briefing Diário"),
    NIGHT_REVIEW("Revisão Noturna"),
    NOTIFICATIONS("Notificações")
}

@Composable
fun RoutineAssistantScreen(
    dailyBriefing: DailyBriefingSnapshot,
    nightReview: NightReviewSnapshot,
    routineSettings: RoutineAssistantSettingsEntity,
    reminders: List<ReminderEntity>,
    briefingHistory: List<BriefingHistoryEntity>,
    initialTabIndex: Int = 0,
    onBack: () -> Unit,
    onMarkBriefingReviewed: (BriefingType) -> Unit,
    onOpenTask: (String) -> Unit,
    onToggleTaskComplete: (String) -> Unit,
    onPostponeTaskToTomorrow: (String) -> Unit,
    onOpenAppointment: (String) -> Unit,
    onOpenEvent: (String) -> Unit,
    onOpenTransaction: (String) -> Unit,
    onSettleTransaction: (String) -> Unit,
    onPostponeTransactionToTomorrow: (String) -> Unit,
    onOpenFinanceCards: () -> Unit,
    onSaveRoutineSettings: (RoutineAssistantSettingsEntity) -> Unit,
    onSnoozeReminder: (String) -> Unit,
    onCancelReminder: (String) -> Unit,
    onSendTestNotification: () -> Unit,
    onRescheduleAllReminders: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalNexusPalette.current
    var selectedTabIndex by remember(initialTabIndex) {
        mutableIntStateOf(initialTabIndex.coerceIn(0, RoutineTab.entries.lastIndex))
    }

    Column(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Voltar",
                        tint = palette.textPrimary
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Assistente de Rotina",
                    style = MaterialTheme.typography.titleLarge,
                    color = palette.textPrimary
                )
            }
            HorizontalDivider(color = palette.border)
        }

        ScrollableTabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = palette.background,
            contentColor = palette.highlight,
            edgePadding = 16.dp
        ) {
            RoutineTab.entries.forEachIndexed { idx, tab ->
                Tab(
                    selected = selectedTabIndex == idx,
                    onClick = { selectedTabIndex = idx },
                    text = { Text(tab.label, maxLines = 1) }
                )
            }
        }

        when (RoutineTab.entries[selectedTabIndex]) {
            RoutineTab.DAILY_BRIEFING -> DailyBriefingTab(
                snapshot = dailyBriefing,
                onMarkReviewed = { onMarkBriefingReviewed(BriefingType.MORNING) },
                onOpenAppointment = onOpenAppointment,
                onOpenTask = onOpenTask,
                onToggleTaskComplete = onToggleTaskComplete,
                onPostponeTaskToTomorrow = onPostponeTaskToTomorrow,
                onOpenEvent = onOpenEvent,
                onOpenTransaction = onOpenTransaction,
                onSettleTransaction = onSettleTransaction,
                onPostponeTransactionToTomorrow = onPostponeTransactionToTomorrow,
                onOpenFinanceCards = onOpenFinanceCards
            )

            RoutineTab.NIGHT_REVIEW -> NightReviewTab(
                snapshot = nightReview,
                history = briefingHistory,
                onMarkReviewed = { onMarkBriefingReviewed(BriefingType.NIGHT_REVIEW) },
                onOpenTask = onOpenTask,
                onToggleTaskComplete = onToggleTaskComplete,
                onPostponeTaskToTomorrow = onPostponeTaskToTomorrow,
                onOpenTransaction = onOpenTransaction,
                onSettleTransaction = onSettleTransaction,
                onPostponeTransactionToTomorrow = onPostponeTransactionToTomorrow,
                onOpenAppointment = onOpenAppointment,
                onOpenEvent = onOpenEvent
            )

            RoutineTab.NOTIFICATIONS -> NotificationsSettingsTab(
                settings = routineSettings,
                reminders = reminders,
                onSaveSettings = onSaveRoutineSettings,
                onSnoozeReminder = onSnoozeReminder,
                onCancelReminder = onCancelReminder,
                onSendTestNotification = onSendTestNotification,
                onRescheduleAllReminders = onRescheduleAllReminders
            )
        }
    }
}

@Composable
private fun DailyBriefingTab(
    snapshot: DailyBriefingSnapshot,
    onMarkReviewed: () -> Unit,
    onOpenAppointment: (String) -> Unit,
    onOpenTask: (String) -> Unit,
    onToggleTaskComplete: (String) -> Unit,
    onPostponeTaskToTomorrow: (String) -> Unit,
    onOpenEvent: (String) -> Unit,
    onOpenTransaction: (String) -> Unit,
    onSettleTransaction: (String) -> Unit,
    onPostponeTransactionToTomorrow: (String) -> Unit,
    onOpenFinanceCards: () -> Unit
) {
    val palette = LocalNexusPalette.current
    val today = DateTimeUtils.todayLocalDate()
    val billsCount = snapshot.billsDueToday.size + snapshot.overdueBills.size

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            NexusCard(modifier = Modifier.fillMaxWidth(), elevated = true) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Outlined.WbSunny, contentDescription = null, tint = palette.highlight)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Briefing Diário",
                                style = MaterialTheme.typography.titleMedium,
                                color = palette.textPrimary
                            )
                            Text(
                                text = DateTimeUtils.formatFullDateWithWeekday(today),
                                style = MaterialTheme.typography.labelMedium,
                                color = palette.textSecondary
                            )
                        }
                    }
                    if (snapshot.isReviewedToday) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = palette.positive)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Revisado", style = MaterialTheme.typography.labelMedium, color = palette.positive)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "${snapshot.todayAppointments.size} compromissos · ${snapshot.todayTasks.size} tarefas hoje · ${snapshot.overdueTasks.size} atrasadas · $billsCount contas (${DateTimeUtils.formatBrl(snapshot.totalBillsDueAmountCents)})",
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.textPrimary
                )
                if (!snapshot.isReviewedToday) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onMarkReviewed,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.highlight,
                            contentColor = palette.background
                        )
                    ) {
                        Text("Marcar Briefing como revisado")
                    }
                }
            }
        }

        if (snapshot.todayAppointments.isNotEmpty()) {
            item {
                SectionHeader(title = "Compromissos de hoje", trailing = "${snapshot.todayAppointments.size}")
            }
            items(snapshot.todayAppointments, key = { "db_appt_${it.id}" }) { appt ->
                AppointmentCompactCard(
                    item = appt,
                    onClick = { onOpenAppointment(appt.appointment.id) }
                )
            }
        }

        if (snapshot.overdueTasks.isNotEmpty()) {
            item {
                SectionHeader(title = "Tarefas atrasadas", trailing = "${snapshot.overdueTasks.size}")
            }
            items(snapshot.overdueTasks, key = { "db_over_t_${it.task.id}" }) { tw ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    TaskRowCard(
                        item = tw,
                        onClick = { onOpenTask(tw.task.id) },
                        onToggleComplete = { onToggleTaskComplete(tw.task.id) }
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { onPostponeTaskToTomorrow(tw.task.id) }) {
                            Text("Adiar p/ amanhã", color = palette.info)
                        }
                    }
                }
            }
        }

        if (snapshot.todayTasks.isNotEmpty()) {
            item {
                SectionHeader(title = "Tarefas de hoje", trailing = "${snapshot.todayTasks.size}")
            }
            items(snapshot.todayTasks, key = { "db_tod_t_${it.task.id}" }) { tw ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    TaskRowCard(
                        item = tw,
                        onClick = { onOpenTask(tw.task.id) },
                        onToggleComplete = { onToggleTaskComplete(tw.task.id) }
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { onPostponeTaskToTomorrow(tw.task.id) }) {
                            Text("Adiar p/ amanhã", color = palette.info)
                        }
                    }
                }
            }
        }

        if (snapshot.todayEvents.isNotEmpty()) {
            item {
                SectionHeader(title = "Eventos de hoje", trailing = "${snapshot.todayEvents.size}")
            }
            items(snapshot.todayEvents, key = { "db_ev_${it.event.id}" }) { ew ->
                EventSummaryCard(item = ew, onClick = { onOpenEvent(ew.event.id) })
            }
        }

        val allBills = (snapshot.overdueBills + snapshot.billsDueToday).distinctBy { it.transaction.id }
        if (allBills.isNotEmpty()) {
            item {
                SectionHeader(title = "Contas vencendo hoje ou em atraso", trailing = "${allBills.size}")
            }
            items(allBills, key = { "db_tx_${it.transaction.id}" }) { tw ->
                val tx = tw.transaction
                NexusCard(modifier = Modifier.fillMaxWidth(), elevated = true, onClick = { onOpenTransaction(tx.id) }) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(tx.description, style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
                            Text(
                                text = "${tx.status.label} · ${tx.dueAt?.let { DateTimeUtils.formatDate(it) } ?: "Sem data"}",
                                style = MaterialTheme.typography.labelMedium,
                                color = palette.textSecondary
                            )
                        }
                        Text(
                            text = DateTimeUtils.formatBrl(tx.amountCents),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = palette.warning
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { onPostponeTransactionToTomorrow(tx.id) }) {
                            Text("Adiar p/ amanhã", color = palette.info)
                        }
                        TextButton(onClick = { onSettleTransaction(tx.id) }) {
                            Text("Quitar", color = palette.positive)
                        }
                    }
                }
            }
        }

        if (snapshot.statementsDueTodayOrOverdue.isNotEmpty()) {
            item {
                SectionHeader(title = "Faturas de cartão", trailing = "${snapshot.statementsDueTodayOrOverdue.size}")
            }
            items(snapshot.statementsDueTodayOrOverdue, key = { "db_st_${it.statement.id}" }) { sw ->
                NexusCard(modifier = Modifier.fillMaxWidth(), elevated = true, onClick = onOpenFinanceCards) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${sw.card.name} (${sw.statement.yearMonth})", style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
                            Text(
                                text = "Vence ${DateTimeUtils.formatDate(sw.statement.dueAt)} · ${sw.statement.status.label}",
                                style = MaterialTheme.typography.labelMedium,
                                color = palette.textSecondary
                            )
                        }
                        Text(
                            text = DateTimeUtils.formatBrl(sw.totalCents),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = palette.danger
                        )
                    }
                }
            }
        }

        if (
            snapshot.todayAppointments.isEmpty() &&
            snapshot.overdueTasks.isEmpty() &&
            snapshot.todayTasks.isEmpty() &&
            snapshot.todayEvents.isEmpty() &&
            allBills.isEmpty() &&
            snapshot.statementsDueTodayOrOverdue.isEmpty()
        ) {
            item {
                NexusEmptyState(
                    icon = Icons.Outlined.WbSunny,
                    title = "Dia limpo no radar",
                    subtitle = "Não há compromissos, tarefas ou contas vencendo hoje."
                )
            }
        }
    }
}

@Composable
private fun NightReviewTab(
    snapshot: NightReviewSnapshot,
    history: List<BriefingHistoryEntity>,
    onMarkReviewed: () -> Unit,
    onOpenTask: (String) -> Unit,
    onToggleTaskComplete: (String) -> Unit,
    onPostponeTaskToTomorrow: (String) -> Unit,
    onOpenTransaction: (String) -> Unit,
    onSettleTransaction: (String) -> Unit,
    onPostponeTransactionToTomorrow: (String) -> Unit,
    onOpenAppointment: (String) -> Unit,
    onOpenEvent: (String) -> Unit
) {
    val palette = LocalNexusPalette.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            NexusCard(modifier = Modifier.fillMaxWidth(), elevated = true) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Outlined.NightsStay, contentDescription = null, tint = palette.highlight)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Revisão Noturna", style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
                            Text(
                                text = "Fechamento do dia e preparação de amanhã",
                                style = MaterialTheme.typography.labelMedium,
                                color = palette.textSecondary
                            )
                        }
                    }
                    if (snapshot.isReviewedTonight) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = palette.positive)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Concluída", style = MaterialTheme.typography.labelMedium, color = palette.positive)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Concluídas hoje: ${snapshot.completedTasksToday.size} · Tarefas em aberto: ${snapshot.pendingTasksTodayOrOverdue.size} · Contas pendentes: ${snapshot.pendingBillsTodayOrOverdue.size}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.textPrimary
                )
                if (!snapshot.isReviewedTonight) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onMarkReviewed,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.highlight,
                            contentColor = palette.background
                        )
                    ) {
                        Text("Fechar revisão de hoje")
                    }
                }
            }
        }

        if (snapshot.pendingTasksTodayOrOverdue.isNotEmpty()) {
            item {
                SectionHeader(title = "Tarefas em aberto de hoje / atrasadas", trailing = "${snapshot.pendingTasksTodayOrOverdue.size}")
            }
            items(snapshot.pendingTasksTodayOrOverdue, key = { "nr_t_${it.task.id}" }) { tw ->
                NexusCard(modifier = Modifier.fillMaxWidth(), elevated = true, onClick = { onOpenTask(tw.task.id) }) {
                    Text(tw.task.title, style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
                    tw.task.dueAt?.let { due ->
                        Text(
                            text = "Prazo: ${DateTimeUtils.formatDate(due)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.textSecondary
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { onPostponeTaskToTomorrow(tw.task.id) }) {
                            Text("Adiar p/ amanhã", color = palette.info)
                        }
                        TextButton(onClick = { onToggleTaskComplete(tw.task.id) }) {
                            Text("Concluir", color = palette.positive)
                        }
                    }
                }
            }
        }

        if (snapshot.pendingBillsTodayOrOverdue.isNotEmpty()) {
            item {
                SectionHeader(title = "Contas em aberto de hoje / vencidas", trailing = "${snapshot.pendingBillsTodayOrOverdue.size}")
            }
            items(snapshot.pendingBillsTodayOrOverdue, key = { "nr_tx_${it.transaction.id}" }) { tw ->
                val tx = tw.transaction
                NexusCard(modifier = Modifier.fillMaxWidth(), elevated = true, onClick = { onOpenTransaction(tx.id) }) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(tx.description, style = MaterialTheme.typography.titleMedium, color = palette.textPrimary, modifier = Modifier.weight(1f))
                        Text(DateTimeUtils.formatBrl(tx.amountCents), style = MaterialTheme.typography.titleMedium, color = palette.warning)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { onPostponeTransactionToTomorrow(tx.id) }) {
                            Text("Adiar p/ amanhã", color = palette.info)
                        }
                        TextButton(onClick = { onSettleTransaction(tx.id) }) {
                            Text("Pagar", color = palette.positive)
                        }
                    }
                }
            }
        }

        item {
            SectionHeader(title = "Prévia de amanhã")
        }

        if (
            snapshot.tomorrowAppointments.isEmpty() &&
            snapshot.tomorrowTasks.isEmpty() &&
            snapshot.tomorrowEvents.isEmpty() &&
            snapshot.tomorrowBills.isEmpty()
        ) {
            item {
                NexusCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Nenhum compromisso, tarefa, evento ou conta agendado para amanhã.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textSecondary
                    )
                }
            }
        } else {
            items(snapshot.tomorrowAppointments, key = { "tm_a_${it.id}" }) { appt ->
                AppointmentCompactCard(item = appt, onClick = { onOpenAppointment(appt.appointment.id) })
            }
            items(snapshot.tomorrowTasks, key = { "tm_t_${it.task.id}" }) { tw ->
                TaskRowCard(item = tw, onClick = { onOpenTask(tw.task.id) }, onToggleComplete = { onToggleTaskComplete(tw.task.id) })
            }
            items(snapshot.tomorrowEvents, key = { "tm_e_${it.event.id}" }) { ew ->
                EventSummaryCard(item = ew, onClick = { onOpenEvent(ew.event.id) })
            }
            items(snapshot.tomorrowBills, key = { "tm_b_${it.transaction.id}" }) { tw ->
                TransactionRowCard(item = tw, onClick = { onOpenTransaction(tw.transaction.id) }, onToggleSettle = { onSettleTransaction(tw.transaction.id) })
            }
        }

        if (history.isNotEmpty()) {
            item {
                SectionHeader(title = "Histórico recente de briefings")
            }
            items(history.take(10), key = { it.id }) { item ->
                NexusCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(item.briefingType.label, style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
                        Text(item.dateIso, style = MaterialTheme.typography.labelMedium, color = palette.textSecondary)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(item.summaryText, style = MaterialTheme.typography.bodyMedium, color = palette.textSecondary)
                    item.reviewedAt?.let { rev ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Revisado em ${DateTimeUtils.formatDateTime(rev)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.positive
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationsSettingsTab(
    settings: RoutineAssistantSettingsEntity,
    reminders: List<ReminderEntity>,
    onSaveSettings: (RoutineAssistantSettingsEntity) -> Unit,
    onSnoozeReminder: (String) -> Unit,
    onCancelReminder: (String) -> Unit,
    onSendTestNotification: () -> Unit,
    onRescheduleAllReminders: () -> Unit
) {
    val palette = LocalNexusPalette.current
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scheduler = remember(context) { NexusNotificationScheduler(context) }

    var hasNotifPermission by remember { mutableStateOf(scheduler.hasNotificationPermission()) }
    var canExactAlarm by remember { mutableStateOf(scheduler.canUseExactAlarms()) }
    var editingTimeTarget by remember { mutableStateOf<String?>(null) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasNotifPermission = scheduler.hasNotificationPermission()
                canExactAlarm = scheduler.canUseExactAlarms()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotifPermission = granted || scheduler.hasNotificationPermission()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            NexusCard(modifier = Modifier.fillMaxWidth(), elevated = true) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.NotificationsActive, contentDescription = null, tint = palette.highlight)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Permissões do Android", style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Notificações (Android 13+)", style = MaterialTheme.typography.bodyMedium, color = palette.textPrimary)
                        Text(
                            text = if (hasNotifPermission) "Concedida" else "Pendente — necessária para exibir alertas",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (hasNotifPermission) palette.positive else palette.warning
                        )
                    }
                    if (!hasNotifPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        OutlinedButton(onClick = { notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }) {
                            Text("Permitir", color = palette.textPrimary)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = palette.border)
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Alarmes exatos", style = MaterialTheme.typography.bodyMedium, color = palette.textPrimary)
                        Text(
                            text = if (canExactAlarm) "Ativos (disparo pontual com app fechado)" else "Janela do sistema (fallback seguro WorkManager/AlarmManager)",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (canExactAlarm) palette.positive else palette.textSecondary
                        )
                    }
                    if (!canExactAlarm && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        OutlinedButton(onClick = {
                            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                data = Uri.parse("package:${context.packageName}")
                            }
                            runCatching { context.startActivity(intent) }.onFailure {
                                context.startActivity(
                                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = Uri.parse("package:${context.packageName}")
                                    }
                                )
                            }
                        }) {
                            Text("Configurar", color = palette.textPrimary)
                        }
                    }
                }
            }
        }

        item {
            NexusCard(modifier = Modifier.fillMaxWidth(), elevated = true) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Notificações locais do NEXUS", style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
                        Text(
                            text = "100% offline via AlarmManager + WorkManager",
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.textSecondary
                        )
                    }
                    Switch(
                        checked = settings.notificationsEnabled,
                        onCheckedChange = { onSaveSettings(settings.copy(notificationsEnabled = it)) }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = palette.border)
                ChannelToggleRow(
                    label = "Agenda e Tarefas",
                    checked = settings.scheduleChannelEnabled,
                    onCheckedChange = { onSaveSettings(settings.copy(scheduleChannelEnabled = it)) }
                )
                ChannelToggleRow(
                    label = "Financeiro e Faturas",
                    checked = settings.financeChannelEnabled,
                    onCheckedChange = { onSaveSettings(settings.copy(financeChannelEnabled = it)) }
                )
                ChannelToggleRow(
                    label = "Eventos",
                    checked = settings.eventsChannelEnabled,
                    onCheckedChange = { onSaveSettings(settings.copy(eventsChannelEnabled = it)) }
                )
                ChannelToggleRow(
                    label = "Assistente de Rotina (Briefing e Revisão)",
                    checked = settings.routineChannelEnabled,
                    onCheckedChange = { onSaveSettings(settings.copy(routineChannelEnabled = it)) }
                )
            }
        }

        item {
            NexusCard(modifier = Modifier.fillMaxWidth(), elevated = true) {
                Text("Horários do Assistente de Rotina", style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Briefing Diário", style = MaterialTheme.typography.bodyMedium, color = palette.textPrimary)
                        Text(
                            text = formatHhMm(settings.morningBriefingHour, settings.morningBriefingMinute),
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.info
                        )
                    }
                    OutlinedButton(onClick = { editingTimeTarget = "morning" }) {
                        Text("Horário", color = palette.textPrimary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = settings.morningBriefingEnabled,
                        onCheckedChange = { onSaveSettings(settings.copy(morningBriefingEnabled = it)) }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Revisão Noturna", style = MaterialTheme.typography.bodyMedium, color = palette.textPrimary)
                        Text(
                            text = formatHhMm(settings.nightReviewHour, settings.nightReviewMinute),
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.info
                        )
                    }
                    OutlinedButton(onClick = { editingTimeTarget = "night" }) {
                        Text("Horário", color = palette.textPrimary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = settings.nightReviewEnabled,
                        onCheckedChange = { onSaveSettings(settings.copy(nightReviewEnabled = it)) }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = palette.border)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Horário de silêncio", style = MaterialTheme.typography.bodyMedium, color = palette.textPrimary)
                        Text(
                            text = "Adia lembretes entre ${formatHhMm(settings.quietHoursStartHour, settings.quietHoursStartMinute)} e ${formatHhMm(settings.quietHoursEndHour, settings.quietHoursEndMinute)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.textSecondary
                        )
                    }
                    Switch(
                        checked = settings.quietHoursEnabled,
                        onCheckedChange = { onSaveSettings(settings.copy(quietHoursEnabled = it)) }
                    )
                }
                if (settings.quietHoursEnabled) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { editingTimeTarget = "quietStart" }, modifier = Modifier.weight(1f)) {
                            Text("Início ${formatHhMm(settings.quietHoursStartHour, settings.quietHoursStartMinute)}", color = palette.textPrimary)
                        }
                        OutlinedButton(onClick = { editingTimeTarget = "quietEnd" }, modifier = Modifier.weight(1f)) {
                            Text("Fim ${formatHhMm(settings.quietHoursEndHour, settings.quietHoursEndMinute)}", color = palette.textPrimary)
                        }
                    }
                }
            }
        }

        item {
            NexusCard(modifier = Modifier.fillMaxWidth(), elevated = true) {
                Text("Antecedência padrão dos lembretes", style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
                Spacer(modifier = Modifier.height(8.dp))
                LeadTimePresetRow(
                    label = "Tarefas",
                    currentMinutes = settings.defaultTaskReminderMinutes,
                    onSelect = { onSaveSettings(settings.copy(defaultTaskReminderMinutes = it)) }
                )
                LeadTimePresetRow(
                    label = "Compromissos",
                    currentMinutes = settings.defaultAppointmentReminderMinutes,
                    onSelect = { onSaveSettings(settings.copy(defaultAppointmentReminderMinutes = it)) }
                )
                LeadTimePresetRow(
                    label = "Eventos",
                    currentMinutes = settings.defaultEventReminderMinutes,
                    onSelect = { onSaveSettings(settings.copy(defaultEventReminderMinutes = it)) }
                )
                LeadTimePresetRow(
                    label = "Contas financeiras",
                    currentMinutes = settings.defaultFinanceReminderMinutes,
                    onSelect = { onSaveSettings(settings.copy(defaultFinanceReminderMinutes = it)) }
                )
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onSendTestNotification,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.highlight,
                        contentColor = palette.background
                    )
                ) {
                    Text("Notificação teste")
                }
                OutlinedButton(
                    onClick = onRescheduleAllReminders,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Reagendar tudo", color = palette.textPrimary)
                }
            }
        }

        val visibleReminders = reminders.filter { com.nexus.app.domain.rules.ReminderRules.isQueueVisible(it) }

        item {
            SectionHeader(title = "Fila local de lembretes", trailing = "${visibleReminders.size}")
        }

        if (visibleReminders.isEmpty()) {
            item {
                NexusEmptyState(
                    icon = Icons.Outlined.Alarm,
                    title = "Nenhum lembrete agendado",
                    subtitle = "Ao criar tarefas, compromissos, eventos ou contas futuras, os lembretes locais aparecerão aqui."
                )
            }
        } else {
            items(visibleReminders.take(40), key = { it.id }) { rem ->
                NexusCard(modifier = Modifier.fillMaxWidth(), elevated = true) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(rem.title, style = MaterialTheme.typography.titleMedium, color = palette.textPrimary, modifier = Modifier.weight(1f))
                        Text(
                            text = rem.status.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = when (rem.status) {
                                ReminderStatus.ACTIVE -> palette.info
                                ReminderStatus.FIRED -> palette.warning
                                ReminderStatus.DISMISSED -> palette.positive
                                ReminderStatus.CANCELLED -> palette.textSecondary
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(rem.message, style = MaterialTheme.typography.bodyMedium, color = palette.textSecondary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Disparo: ${DateTimeUtils.formatDateTime(rem.triggerAt)} · ${if (rem.isExactScheduled) "Alarme exato" else "Alarme do sistema"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textSecondary
                    )
                    if (rem.status == ReminderStatus.ACTIVE || rem.status == ReminderStatus.FIRED) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { onSnoozeReminder(rem.id) }) {
                                Text("Adiar 15m", color = palette.info)
                            }
                            TextButton(onClick = { onCancelReminder(rem.id) }) {
                                Text("Cancelar", color = palette.danger)
                            }
                        }
                    }
                }
            }
        }
    }

    val initialPickerTime = when (editingTimeTarget) {
        "morning" -> LocalTime.of(settings.morningBriefingHour, settings.morningBriefingMinute)
        "night" -> LocalTime.of(settings.nightReviewHour, settings.nightReviewMinute)
        "quietStart" -> LocalTime.of(settings.quietHoursStartHour, settings.quietHoursStartMinute)
        "quietEnd" -> LocalTime.of(settings.quietHoursEndHour, settings.quietHoursEndMinute)
        else -> null
    }
    if (editingTimeTarget != null && initialPickerTime != null) {
        NexusTimePickerDialog(
            initialTime = initialPickerTime,
            onTimeSelected = { picked ->
                val updated = when (editingTimeTarget) {
                    "morning" -> settings.copy(morningBriefingHour = picked.hour, morningBriefingMinute = picked.minute)
                    "night" -> settings.copy(nightReviewHour = picked.hour, nightReviewMinute = picked.minute)
                    "quietStart" -> settings.copy(quietHoursStartHour = picked.hour, quietHoursStartMinute = picked.minute)
                    "quietEnd" -> settings.copy(quietHoursEndHour = picked.hour, quietHoursEndMinute = picked.minute)
                    else -> settings
                }
                onSaveSettings(updated)
            },
            onDismiss = { editingTimeTarget = null }
        )
    }
}

@Composable
private fun ChannelToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val palette = LocalNexusPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = palette.textPrimary, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun LeadTimePresetRow(
    label: String,
    currentMinutes: Int,
    onSelect: (Int) -> Unit
) {
    val palette = LocalNexusPalette.current
    val presets = listOf(0 to "Na hora", 15 to "15m", 30 to "30m", 60 to "1h")
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = palette.textSecondary)
        Spacer(modifier = Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            presets.forEach { (mins, text) ->
                val selected = currentMinutes == mins
                OutlinedButton(
                    onClick = { onSelect(mins) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, if (selected) palette.highlight else palette.border),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = text,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selected) palette.highlight else palette.textSecondary
                    )
                }
            }
        }
    }
}

private fun formatHhMm(hour: Int, minute: Int): String =
    String.format(Locale("pt", "BR"), "%02d:%02d", hour.coerceIn(0, 23), minute.coerceIn(0, 59))
