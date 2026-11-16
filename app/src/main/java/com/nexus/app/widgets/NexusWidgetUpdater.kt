package com.nexus.app.widgets

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll
import com.nexus.app.NexusApplication
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Mecanismo centralizado e sincronizado de atualizacao dos widgets NEXUS.
 * Evita rajadas repetidas de leitura/renderizacao ao Room quando multiplas operacoes ocorrem em sequencia.
 */
object NexusWidgetUpdater {
    private val updateMutex = Mutex()

    suspend fun loadSafeSnapshots(context: Context): WidgetDataState<AllWidgetSnapshots> {
        val app = context.applicationContext as? NexusApplication
            ?: return WidgetDataState.Error("Aplicativo NEXUS indisponível no momento.")
        return app.container.repository.loadAllWidgetSnapshots().fold(
            onSuccess = { WidgetDataState.Ready(it) },
            onFailure = { WidgetDataState.Error("Não foi possível carregar dados locais.") }
        )
    }

    suspend fun updateAllWidgets(context: Context) {
        val appContext = context.applicationContext
        updateMutex.withLock {
            runCatching { MyDayGlanceWidget().updateAll(appContext) }
            runCatching { MyTasksGlanceWidget().updateAll(appContext) }
            runCatching { FinanceGlanceWidget().updateAll(appContext) }
            runCatching { NexusSummaryGlanceWidget().updateAll(appContext) }
        }
    }

    suspend fun updateWidgetById(context: Context, appWidgetId: Int) {
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return
        val appContext = context.applicationContext
        val providerName = runCatching {
            AppWidgetManager.getInstance(appContext).getAppWidgetInfo(appWidgetId)?.provider?.className
        }.getOrNull() ?: return

        val manager = GlanceAppWidgetManager(appContext)
        val glanceId = runCatching { manager.getGlanceIdBy(appWidgetId) }.getOrNull() ?: return

        updateMutex.withLock {
            when (providerName) {
                MyDayWidgetReceiver::class.java.name ->
                    runCatching { MyDayGlanceWidget().update(appContext, glanceId) }
                MyTasksWidgetReceiver::class.java.name ->
                    runCatching { MyTasksGlanceWidget().update(appContext, glanceId) }
                FinanceWidgetReceiver::class.java.name ->
                    runCatching { FinanceGlanceWidget().update(appContext, glanceId) }
                NexusSummaryWidgetReceiver::class.java.name ->
                    runCatching { NexusSummaryGlanceWidget().update(appContext, glanceId) }
            }
        }
    }
}
