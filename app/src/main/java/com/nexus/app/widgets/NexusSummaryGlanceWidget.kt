package com.nexus.app.widgets

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.action.Action
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
import androidx.glance.unit.ColorProvider
import com.nexus.app.core.DateTimeUtils

class NexusSummaryWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NexusSummaryGlanceWidget()

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        NexusWidgetConfigStore.delete(context, appWidgetIds)
    }
}

class NexusSummaryGlanceWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Responsive(NexusWidgetSizes.ALL)

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appWidgetId = runCatching {
            GlanceAppWidgetManager(context).getAppWidgetId(id)
        }.getOrDefault(AppWidgetManager.INVALID_APPWIDGET_ID)
        val config = NexusWidgetConfigStore.load(context, appWidgetId, NexusWidgetType.NEXUS_SUMMARY)
        val state = NexusWidgetUpdater.loadSafeSnapshots(context)

        provideContent {
            val tier = NexusWidgetSizes.tierFor(LocalSize.current)
            when (state) {
                is WidgetDataState.Error -> NexusWidgetErrorState(
                    context = context,
                    appWidgetId = appWidgetId,
                    widgetTitle = "NEXUS",
                    errorMessage = state.message,
                    tier = tier
                )

                is WidgetDataState.Ready -> NexusSummaryWidgetContent(
                    context = context,
                    appWidgetId = appWidgetId,
                    snapshot = state.data.summary,
                    config = config,
                    tier = tier
                )
            }
        }
    }
}

@Composable
private fun NexusSummaryWidgetContent(
    context: Context,
    appWidgetId: Int,
    snapshot: NexusSummaryWidgetSnapshot,
    config: NexusWidgetInstanceConfig,
    tier: WidgetSizeTier
) {
    val homeAction = actionStartActivity(NexusWidgetNavigation.openHomeIntent(context))
    val tasksAction = actionStartActivity(NexusWidgetNavigation.openTasksIntent(context))
    val agendaAction = actionStartActivity(NexusWidgetNavigation.openAgendaIntent(context))
    val eventsAction = if (snapshot.nextEvent != null) {
        actionStartActivity(NexusWidgetNavigation.openEventIntent(context, snapshot.nextEvent.event.id))
    } else {
        actionStartActivity(NexusWidgetNavigation.openEventsIntent(context))
    }
    val financeAction = if (snapshot.nextDueTransaction != null) {
        actionStartActivity(NexusWidgetNavigation.openTransactionIntent(context, snapshot.nextDueTransaction.id))
    } else {
        actionStartActivity(NexusWidgetNavigation.openFinanceIntent(context))
    }
    val goalsAction = actionStartActivity(NexusWidgetNavigation.openFinanceIntent(context))

    NexusWidgetShell {
        NexusWidgetHeader(
            context = context,
            appWidgetId = appWidgetId,
            title = "RESUMO NEXUS",
            subtitle = snapshot.dateFormatted,
            tier = tier,
            onHeaderAction = homeAction
        )

        Spacer(modifier = GlanceModifier.height(6.dp))

        if (snapshot.isCompletelyEmpty) {
            NexusWidgetEmptyState(
                title = "Centro de comando pronto",
                subtitle = if (tier == WidgetSizeTier.SMALL) null else "Adicione tarefas, compromissos, eventos ou finanças.",
                onAction = homeAction
            )
        } else {
            val showDetail = !config.compactMode && tier == WidgetSizeTier.LARGE
            val sections = buildList<@Composable () -> Unit> {
                if (config.showTasksSection) {
                    add {
                        SummarySectionRow(
                            label = "Tarefas",
                            value = "${snapshot.pendingTasksCount} pendentes",
                            detail = if (showDetail) snapshot.topTask?.task?.title else if (snapshot.overdueTasksCount > 0) "${snapshot.overdueTasksCount} atrasada(s)" else null,
                            accentColor = if (snapshot.overdueTasksCount > 0) NexusWidgetColors.Danger else NexusWidgetColors.TextPrimary,
                            onClick = tasksAction
                        )
                    }
                }
                if (config.showTodayAppointmentsSection) {
                    add {
                        val nextAppt = snapshot.nextAppointment
                        val detail = if (showDetail && nextAppt != null) {
                            val time = if (nextAppt.appointment.isAllDay) "Dia inteiro" else DateTimeUtils.formatTime(nextAppt.appointment.startAt)
                            "$time · ${nextAppt.appointment.title}"
                        } else null
                        SummarySectionRow(
                            label = "Compromissos hoje",
                            value = snapshot.todayAppointmentsCount.toString(),
                            detail = detail,
                            accentColor = NexusWidgetColors.Info,
                            onClick = agendaAction
                        )
                    }
                }
                if (config.showEventsSection) {
                    add {
                        val nextEv = snapshot.nextEvent
                        val detail = if (showDetail && nextEv != null) {
                            "${DateTimeUtils.formatDate(nextEv.event.startAt)} · ${nextEv.event.name}"
                        } else null
                        SummarySectionRow(
                            label = "Próximos eventos",
                            value = snapshot.upcomingEventsCount.toString(),
                            detail = detail,
                            accentColor = NexusWidgetColors.TextPrimary,
                            onClick = eventsAction
                        )
                    }
                }
                if (config.showFinanceSection) {
                    add {
                        val nextDue = snapshot.nextDueTransaction
                        val detail = if (showDetail && nextDue != null) {
                            "${nextDue.description} · ${DateTimeUtils.formatBrl(nextDue.amountCents)}"
                        } else if (snapshot.overdueBillsCount > 0) {
                            "${snapshot.overdueBillsCount} atrasada(s)"
                        } else null
                        SummarySectionRow(
                            label = "Vencimentos",
                            value = "${snapshot.upcomingDueCount + snapshot.overdueBillsCount}",
                            detail = detail,
                            accentColor = if (snapshot.overdueBillsCount > 0) NexusWidgetColors.Danger else NexusWidgetColors.Warning,
                            onClick = financeAction
                        )
                    }
                }
                if (config.showGoalsSection && snapshot.hasGoals) {
                    add {
                        val pct = ((snapshot.goalsTotalCurrentCents * 100L) / snapshot.goalsTotalTargetCents)
                            .coerceIn(0L, 100L)
                        val detail = if (showDetail && !snapshot.topGoalName.isNullOrBlank()) {
                            "${snapshot.topGoalName} · ${DateTimeUtils.formatBrl(snapshot.goalsTotalCurrentCents)}"
                        } else null
                        SummarySectionRow(
                            label = "Metas (${snapshot.goalsCount})",
                            value = "$pct%",
                            detail = detail,
                            accentColor = NexusWidgetColors.Positive,
                            onClick = goalsAction
                        )
                    }
                }
            }

            val maxVisible = when {
                tier == WidgetSizeTier.SMALL -> 2
                config.compactMode -> config.maxItems.coerceAtMost(3)
                else -> config.maxItems.coerceAtMost(5)
            }
            sections.take(maxVisible).forEachIndexed { idx, render ->
                if (idx > 0) Spacer(modifier = GlanceModifier.height(4.dp))
                render()
            }
        }
    }
}

@Composable
private fun SummarySectionRow(
    label: String,
    value: String,
    detail: String?,
    accentColor: ColorProvider,
    onClick: Action
) {
    Column(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(NexusWidgetColors.Surface)
            .cornerRadius(8.dp)
            .padding(horizontal = 8.dp, vertical = 5.dp)
            .clickable(onClick)
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = TextStyle(color = NexusWidgetColors.TextSecondary, fontSize = 11.sp),
                modifier = GlanceModifier.defaultWeight(),
                maxLines = 1
            )
            Spacer(modifier = GlanceModifier.width(6.dp))
            Text(
                text = value,
                style = TextStyle(
                    color = accentColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 1
            )
        }
        if (!detail.isNullOrBlank()) {
            Text(
                text = detail,
                style = TextStyle(color = NexusWidgetColors.TextMuted, fontSize = 10.sp),
                maxLines = 1
            )
        }
    }
}
