package com.nexus.app.widgets

import android.content.Context
import android.content.SharedPreferences

/**
 * Armazena exclusivamente preferencias visuais por instancia de widget (appWidgetId).
 * Nao duplica dados de negocio: todas as informacoes de dominio permanecem no SQLite/Room.
 */
object NexusWidgetConfigStore {
    private const val PREFS_NAME = "nexus_widget_instance_prefs"
    private const val CONFIRM_TTL_MS = 15_000L

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(context: Context, appWidgetId: Int, widgetType: NexusWidgetType): NexusWidgetInstanceConfig {
        val p = prefs(context)
        val prefix = "w_${appWidgetId}_"
        return NexusWidgetInstanceConfig(
            appWidgetId = appWidgetId,
            widgetType = widgetType,
            compactMode = p.getBoolean("${prefix}compactMode", false),
            maxItems = p.getInt("${prefix}maxItems", 4).coerceIn(1, 6),
            showAppointments = p.getBoolean("${prefix}showAppointments", true),
            showTasksDueToday = p.getBoolean("${prefix}showTasksDueToday", true),
            showTaskCounters = p.getBoolean("${prefix}showTaskCounters", true),
            showProjectName = p.getBoolean("${prefix}showProjectName", true),
            showDueAndStatus = p.getBoolean("${prefix}showDueAndStatus", true),
            showPendingCounter = p.getBoolean("${prefix}showPendingCounter", true),
            showDeclaredBalance = p.getBoolean("${prefix}showDeclaredBalance", true),
            showPlannedTotals = p.getBoolean("${prefix}showPlannedTotals", true),
            showOverdueAndOpenStatements = p.getBoolean("${prefix}showOverdueAndOpenStatements", true),
            showNextDue = p.getBoolean("${prefix}showNextDue", true),
            showTasksSection = p.getBoolean("${prefix}showTasksSection", true),
            showTodayAppointmentsSection = p.getBoolean("${prefix}showTodayAppointmentsSection", true),
            showEventsSection = p.getBoolean("${prefix}showEventsSection", true),
            showFinanceSection = p.getBoolean("${prefix}showFinanceSection", true),
            showGoalsSection = p.getBoolean("${prefix}showGoalsSection", true)
        )
    }

    fun save(context: Context, config: NexusWidgetInstanceConfig) {
        val prefix = "w_${config.appWidgetId}_"
        prefs(context).edit()
            .putString("${prefix}type", config.widgetType.name)
            .putBoolean("${prefix}compactMode", config.compactMode)
            .putInt("${prefix}maxItems", config.maxItems.coerceIn(1, 6))
            .putBoolean("${prefix}showAppointments", config.showAppointments)
            .putBoolean("${prefix}showTasksDueToday", config.showTasksDueToday)
            .putBoolean("${prefix}showTaskCounters", config.showTaskCounters)
            .putBoolean("${prefix}showProjectName", config.showProjectName)
            .putBoolean("${prefix}showDueAndStatus", config.showDueAndStatus)
            .putBoolean("${prefix}showPendingCounter", config.showPendingCounter)
            .putBoolean("${prefix}showDeclaredBalance", config.showDeclaredBalance)
            .putBoolean("${prefix}showPlannedTotals", config.showPlannedTotals)
            .putBoolean("${prefix}showOverdueAndOpenStatements", config.showOverdueAndOpenStatements)
            .putBoolean("${prefix}showNextDue", config.showNextDue)
            .putBoolean("${prefix}showTasksSection", config.showTasksSection)
            .putBoolean("${prefix}showTodayAppointmentsSection", config.showTodayAppointmentsSection)
            .putBoolean("${prefix}showEventsSection", config.showEventsSection)
            .putBoolean("${prefix}showFinanceSection", config.showFinanceSection)
            .putBoolean("${prefix}showGoalsSection", config.showGoalsSection)
            .apply()
    }

    fun delete(context: Context, appWidgetIds: IntArray) {
        val p = prefs(context)
        val editor = p.edit()
        val allKeys = p.all.keys
        appWidgetIds.forEach { widgetId ->
            val prefix = "w_${widgetId}_"
            allKeys.filter { it.startsWith(prefix) }.forEach { key ->
                editor.remove(key)
            }
            editor.remove(confirmTaskKey(widgetId))
            editor.remove(confirmTimeKey(widgetId))
        }
        editor.apply()
    }

    fun setPendingCompleteTask(context: Context, appWidgetId: Int, taskId: String) {
        prefs(context).edit()
            .putString(confirmTaskKey(appWidgetId), taskId)
            .putLong(confirmTimeKey(appWidgetId), System.currentTimeMillis())
            .apply()
    }

    fun getPendingCompleteTask(context: Context, appWidgetId: Int): String? {
        val p = prefs(context)
        val taskId = p.getString(confirmTaskKey(appWidgetId), null) ?: return null
        val timestamp = p.getLong(confirmTimeKey(appWidgetId), 0L)
        return if (System.currentTimeMillis() - timestamp <= CONFIRM_TTL_MS) {
            taskId
        } else {
            clearPendingCompleteTask(context, appWidgetId)
            null
        }
    }

    fun clearPendingCompleteTask(context: Context, appWidgetId: Int) {
        prefs(context).edit()
            .remove(confirmTaskKey(appWidgetId))
            .remove(confirmTimeKey(appWidgetId))
            .apply()
    }

    private fun confirmTaskKey(appWidgetId: Int): String = "confirm_task_$appWidgetId"
    private fun confirmTimeKey(appWidgetId: Int): String = "confirm_ts_$appWidgetId"
}
