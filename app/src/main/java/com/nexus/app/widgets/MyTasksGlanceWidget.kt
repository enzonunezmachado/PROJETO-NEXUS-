package com.nexus.app.widgets

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.nexus.app.R
import com.nexus.app.domain.model.TaskPriority
import com.nexus.app.domain.model.TaskStatus

class MyTasksWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MyTasksGlanceWidget()

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        NexusWidgetConfigStore.delete(context, appWidgetIds)
    }
}

class MyTasksGlanceWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Responsive(NexusWidgetSizes.ALL)

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appWidgetId = runCatching {
            GlanceAppWidgetManager(context).getAppWidgetId(id)
        }.getOrDefault(AppWidgetManager.INVALID_APPWIDGET_ID)
        val config = NexusWidgetConfigStore.load(context, appWidgetId, NexusWidgetType.MY_TASKS)
        val pendingConfirmTaskId = NexusWidgetConfigStore.getPendingCompleteTask(context, appWidgetId)
        val state = NexusWidgetUpdater.loadSafeSnapshots(context)

        provideContent {
            val tier = NexusWidgetSizes.tierFor(LocalSize.current)
            when (state) {
                is WidgetDataState.Error -> NexusWidgetErrorState(
                    context = context,
                    appWidgetId = appWidgetId,
                    widgetTitle = "MINHAS TAREFAS",
                    errorMessage = state.message,
                    tier = tier
                )

                is WidgetDataState.Ready -> MyTasksWidgetContent(
                    context = context,
                    appWidgetId = appWidgetId,
                    snapshot = state.data.myTasks,
                    config = config,
                    pendingConfirmTaskId = pendingConfirmTaskId,
                    tier = tier
                )
            }
        }
    }
}

@Composable
private fun MyTasksWidgetContent(
    context: Context,
    appWidgetId: Int,
    snapshot: MyTasksWidgetSnapshot,
    config: NexusWidgetInstanceConfig,
    pendingConfirmTaskId: String?,
    tier: WidgetSizeTier
) {
    val maxRows = when {
        tier == WidgetSizeTier.SMALL -> 2
        config.compactMode -> config.maxItems.coerceAtMost(3)
        tier == WidgetSizeTier.MEDIUM -> config.maxItems.coerceAtMost(3)
        else -> config.maxItems
    }
    val tasksAction = actionStartActivity(NexusWidgetNavigation.openTasksIntent(context))
    val subtitle = if (config.showPendingCounter) {
        "${snapshot.pendingTasksCount} pendentes · ${snapshot.urgentOrHighCount} prioritárias"
    } else {
        null
    }

    NexusWidgetShell {
        NexusWidgetHeader(
            context = context,
            appWidgetId = appWidgetId,
            title = "MINHAS TAREFAS",
            subtitle = subtitle,
            tier = tier,
            onHeaderAction = tasksAction
        )

        Spacer(modifier = GlanceModifier.height(8.dp))

        if (snapshot.isEmpty) {
            NexusWidgetEmptyState(
                title = "Nenhuma tarefa pendente",
                subtitle = if (tier == WidgetSizeTier.SMALL) null else "Toque para abrir o painel de tarefas.",
                onAction = tasksAction
            )
        } else {
            val visibleTasks = snapshot.tasks.take(maxRows)
            visibleTasks.forEachIndexed { idx, task ->
                if (idx > 0) Spacer(modifier = GlanceModifier.height(5.dp))
                val isAwaitingConfirm = pendingConfirmTaskId == task.id && task.canCompleteFromWidget
                val priorityColor = when (task.priority) {
                    TaskPriority.URGENT -> NexusWidgetColors.Danger
                    TaskPriority.HIGH -> NexusWidgetColors.Warning
                    TaskPriority.NORMAL -> NexusWidgetColors.TextPrimary
                    TaskPriority.LOW -> NexusWidgetColors.TextSecondary
                }

                Column(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .background(NexusWidgetColors.Surface)
                        .cornerRadius(8.dp)
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = GlanceModifier
                                .defaultWeight()
                                .clickable(
                                    actionStartActivity(
                                        NexusWidgetNavigation.openTaskIntent(context, task.id)
                                    )
                                )
                        ) {
                            Text(
                                text = task.title,
                                style = TextStyle(
                                    color = NexusWidgetColors.TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                maxLines = 1
                            )
                            if (!config.compactMode && (config.showProjectName || config.showDueAndStatus)) {
                                val metaParts = buildList {
                                    if (config.showProjectName && !task.projectName.isNullOrBlank()) {
                                        add(task.projectName)
                                    }
                                    if (config.showDueAndStatus) {
                                        add(task.priority.label)
                                        if (task.status == TaskStatus.BLOCKED) add("Bloqueada")
                                        task.dueLabel?.let { add(it) }
                                    }
                                }
                                if (metaParts.isNotEmpty()) {
                                    Text(
                                        text = metaParts.joinToString(" · "),
                                        style = TextStyle(
                                            color = when {
                                                task.isOverdue -> NexusWidgetColors.Danger
                                                task.status == TaskStatus.BLOCKED -> NexusWidgetColors.Warning
                                                else -> priorityColor
                                            },
                                            fontSize = 10.sp
                                        ),
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        if (task.canCompleteFromWidget && !isAwaitingConfirm && tier != WidgetSizeTier.SMALL) {
                            Spacer(modifier = GlanceModifier.width(6.dp))
                            Box(
                                modifier = GlanceModifier
                                    .size(26.dp)
                                    .background(NexusWidgetColors.SurfaceElevated)
                                    .cornerRadius(7.dp)
                                    .clickable(
                                        actionRunCallback<RequestCompleteTaskAction>(
                                            actionParametersOf(
                                                NexusWidgetActionKeys.TaskId to task.id,
                                                NexusWidgetActionKeys.AppWidgetId to appWidgetId
                                            )
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    provider = ImageProvider(R.drawable.ic_widget_check),
                                    contentDescription = "Concluir tarefa",
                                    modifier = GlanceModifier.size(14.dp),
                                    colorFilter = ColorFilter.tint(NexusWidgetColors.TextPrimary)
                                )
                            }
                        }
                    }

                    if (isAwaitingConfirm) {
                        Spacer(modifier = GlanceModifier.height(5.dp))
                        Row(
                            modifier = GlanceModifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Confirmar conclusão?",
                                style = TextStyle(
                                    color = NexusWidgetColors.TextSecondary,
                                    fontSize = 10.sp
                                ),
                                modifier = GlanceModifier.defaultWeight()
                            )
                            Box(
                                modifier = GlanceModifier
                                    .background(NexusWidgetColors.Positive)
                                    .cornerRadius(6.dp)
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                                    .clickable(
                                        actionRunCallback<ConfirmCompleteTaskAction>(
                                            actionParametersOf(
                                                NexusWidgetActionKeys.TaskId to task.id,
                                                NexusWidgetActionKeys.AppWidgetId to appWidgetId
                                            )
                                        )
                                    )
                            ) {
                                Text(
                                    text = "Concluir",
                                    style = TextStyle(
                                        color = NexusWidgetColors.OnPositive,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                            Spacer(modifier = GlanceModifier.width(6.dp))
                            Box(
                                modifier = GlanceModifier
                                    .background(NexusWidgetColors.SurfaceElevated)
                                    .cornerRadius(6.dp)
                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                                    .clickable(
                                        actionRunCallback<CancelCompleteTaskAction>(
                                            actionParametersOf(
                                                NexusWidgetActionKeys.AppWidgetId to appWidgetId
                                            )
                                        )
                                    )
                            ) {
                                Text(
                                    text = "Cancelar",
                                    style = TextStyle(
                                        color = NexusWidgetColors.TextSecondary,
                                        fontSize = 10.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
