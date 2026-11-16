package com.nexus.app.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nexus.app.NexusApplication
import com.nexus.app.widgets.NexusWidgetUpdater

/**
 * Worker 100% offline executado periodicamente para garantir recorrencias e lembretes
 * caso o sistema operacional restrinja alarmes exatos ou encerramentos em segundo plano,
 * alem de manter os widgets atualizados mesmo na virada de dia.
 */
class NexusReminderSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? NexusApplication ?: return Result.success()
        return try {
            app.container.repository.syncPeriodicOfflineSafety()
            NexusWidgetUpdater.updateAllWidgets(applicationContext)
            Result.success()
        } catch (ex: Exception) {
            app.container.repository.logError("NexusReminderSyncWorker", ex)
            Result.retry()
        }
    }
}
