package com.nexus.app.widgets

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import com.nexus.app.NexusApplication

object NexusWidgetActionKeys {
    val TaskId = ActionParameters.Key<String>("widget_task_id")
    val AppWidgetId = ActionParameters.Key<Int>("widget_app_widget_id")
}

class RefreshWidgetsAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        NexusWidgetUpdater.updateAllWidgets(context.applicationContext)
    }
}

class RequestCompleteTaskAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val taskId = parameters[NexusWidgetActionKeys.TaskId] ?: return
        val appWidgetId = parameters[NexusWidgetActionKeys.AppWidgetId]
            ?: AppWidgetManager.INVALID_APPWIDGET_ID
        val app = context.applicationContext as? NexusApplication ?: return
        if (!app.container.repository.canCompleteTaskFromWidget(taskId)) {
            MyTasksGlanceWidget().update(context, glanceId)
            return
        }
        NexusWidgetConfigStore.setPendingCompleteTask(context, appWidgetId, taskId)
        MyTasksGlanceWidget().update(context, glanceId)
    }
}

class ConfirmCompleteTaskAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val taskId = parameters[NexusWidgetActionKeys.TaskId] ?: return
        val appWidgetId = parameters[NexusWidgetActionKeys.AppWidgetId]
            ?: AppWidgetManager.INVALID_APPWIDGET_ID
        NexusWidgetConfigStore.clearPendingCompleteTask(context, appWidgetId)
        val app = context.applicationContext as? NexusApplication ?: return
        app.container.repository.completeTaskFromWidget(taskId)
        NexusWidgetUpdater.updateAllWidgets(context.applicationContext)
    }
}

class CancelCompleteTaskAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val appWidgetId = parameters[NexusWidgetActionKeys.AppWidgetId]
            ?: AppWidgetManager.INVALID_APPWIDGET_ID
        NexusWidgetConfigStore.clearPendingCompleteTask(context, appWidgetId)
        MyTasksGlanceWidget().update(context, glanceId)
    }
}
