package com.nexus.app.widgets

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle

class MyDayWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MyDayGlanceWidget()

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        NexusWidgetConfigStore.delete(context, appWidgetIds)
    }
}

class MyDayGlanceWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Responsive(NexusWidgetSizes.ALL)

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appWidgetId = runCatching {
            GlanceAppWidgetManager(context).getAppWidgetId(id)
        }.getOrDefault(AppWidgetManager.INVALID_APPWIDGET_ID)
        val config = NexusWidgetConfigStore.load(context, appWidgetId, NexusWidgetType.MY_DAY)
        val state = NexusWidgetUpdater.loadSafeSnapshots(context)

        provideContent {
            val tier = NexusWidgetSizes.tierFor(LocalSize.current)
            when (state) {
                is WidgetDataState.Error -> NexusWidgetErrorState(
                    context = context,
                    appWidgetId = appWidgetId,
                    widgetTitle = "MEU DIA",
                    errorMessage = state.message,
                    tier = tier
                )

                is WidgetDataState.Ready -> MyDayWidgetContent(
                    context = context,
                    appWidgetId = appWidgetId,
                    snapshot = state.data.myDay,
                    config = config,
                    tier = tier
                )
            }
        }
    }
}

@Composable
private fun MyDayWidgetContent(
    context: Context,
    appWidgetId: Int,
    snapshot: MyDayWidgetSnapshot,
    config: NexusWidgetInstanceConfig,
    tier: WidgetSizeTier
) {
    val maxAppts = when {
        tier == WidgetSizeTier.SMALL -> 1
        config.compactMode -> config.maxItems.coerceAtMost(2)
        tier == WidgetSizeTier.MEDIUM -> config.maxItems.coerceAtMost(3)
        else -> config.maxItems
    }
    val maxTasks = when {
        tier == WidgetSizeTier.SMALL -> 1
        config.compactMode -> config.maxItems.coerceAtMost(2)
        tier == WidgetSizeTier.MEDIUM -> config.maxItems.coerceAtMost(2)
        else -> config.maxItems
    }

    val agendaAction = actionStartActivity(
        NexusWidgetNavigation.openAgendaIntent(context, snapshot.date)
    )

    NexusWidgetShell {
        NexusWidgetHeader(
            context = context,
            appWidgetId = appWidgetId,
            title = "MEU DIA",
            subtitle = snapshot.dateFormatted,
            tier = tier,
            onHeaderAction = agendaAction
        )

        if (config.showTaskCounters) {
            Spacer(modifier = GlanceModifier.height(6.dp))
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .background(NexusWidgetColors.Surface)
                    .cornerRadius(8.dp)
                    .padding(horizontal = 8.dp, vertical = 5.dp)
                    .clickable(actionStartActivity(NexusWidgetNavigation.openTasksIntent(context))),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${snapshot.pendingTasksCount} pend.",
                    style = TextStyle(
                        color = NexusWidgetColors.TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
                Spacer(modifier = GlanceModifier.width(8.dp))
                Text(
                    text = "•",
                    style = TextStyle(color = NexusWidgetColors.TextMuted, fontSize = 11.sp)
                )
                Spacer(modifier = GlanceModifier.width(8.dp))
                Text(
                    text = "${snapshot.totalCompletedTasksCount} concl.",
                    style = TextStyle(
                        color = NexusWidgetColors.Positive,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }

        Spacer(modifier = GlanceModifier.height(8.dp))

        if (snapshot.isEmptySchedule) {
            NexusWidgetEmptyState(
                title = "Agenda livre hoje",
                subtitle = if (tier == WidgetSizeTier.SMALL) null else "Nenhum compromisso ou tarefa com prazo para hoje.",
                onAction = agendaAction
            )
        } else {
            val visibleAppts = if (config.showAppointments) snapshot.appointments.take(maxAppts) else emptyList()
            val visibleTasks = if (config.showTasksDueToday) snapshot.tasksDueToday.take(maxTasks) else emptyList()

            if (visibleAppts.isEmpty() && visibleTasks.isEmpty()) {
                NexusWidgetEmptyState(
                    title = "Nenhuma seção visível",
                    subtitle = "Toque para abrir a agenda de hoje.",
                    onAction = agendaAction
                )
            } else {
                visibleAppts.forEachIndexed { idx, appt ->
                    if (idx > 0) Spacer(modifier = GlanceModifier.height(4.dp))
                    Row(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .background(NexusWidgetColors.Surface)
                            .cornerRadius(8.dp)
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                            .clickable(
                                actionStartActivity(
                                    NexusWidgetNavigation.openAppointmentIntent(context, appt.id)
                                )
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = GlanceModifier.defaultWeight()) {
                            Text(
                                text = appt.title,
                                style = TextStyle(
                                    color = NexusWidgetColors.TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                maxLines = 1
                            )
                            val meta = buildString {
                                append(appt.timeLabel)
                                if (!config.compactMode && appt.location.isNotBlank()) {
                                    append(" · ${appt.location}")
                                }
                                if (appt.hasConflict) append(" · Conflito")
                            }
                            Text(
                                text = meta,
                                style = TextStyle(
                                    color = if (appt.hasConflict) NexusWidgetColors.Warning else NexusWidgetColors.TextMuted,
                                    fontSize = 10.sp
                                ),
                                maxLines = 1
                            )
                        }
                    }
                }

                if (visibleTasks.isNotEmpty() && tier != WidgetSizeTier.SMALL) {
                    if (visibleAppts.isNotEmpty()) {
                        Spacer(modifier = GlanceModifier.height(6.dp))
                        NexusWidgetDivider()
                        Spacer(modifier = GlanceModifier.height(6.dp))
                    }
                    Text(
                        text = "TAREFAS PARA HOJE",
                        style = TextStyle(
                            color = NexusWidgetColors.TextMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = GlanceModifier.height(4.dp))
                    visibleTasks.forEachIndexed { idx, task ->
                        if (idx > 0) Spacer(modifier = GlanceModifier.height(4.dp))
                        Row(
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .background(NexusWidgetColors.SurfaceElevated)
                                .cornerRadius(8.dp)
                                .padding(horizontal = 8.dp, vertical = 5.dp)
                                .clickable(
                                    actionStartActivity(
                                        NexusWidgetNavigation.openTaskIntent(context, task.id)
                                    )
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = task.title,
                                style = TextStyle(
                                    color = NexusWidgetColors.TextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                maxLines = 1,
                                modifier = GlanceModifier.defaultWeight()
                            )
                            Spacer(modifier = GlanceModifier.width(6.dp))
                            Text(
                                text = task.timeLabel,
                                style = TextStyle(
                                    color = if (task.isOverdue) NexusWidgetColors.Danger else NexusWidgetColors.TextSecondary,
                                    fontSize = 10.sp
                                ),
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}
