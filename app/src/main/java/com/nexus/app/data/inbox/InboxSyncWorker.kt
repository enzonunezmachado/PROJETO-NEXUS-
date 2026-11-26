package com.nexus.app.data.inbox

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.nexus.app.NexusApplication
import java.util.concurrent.TimeUnit

class InboxSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? NexusApplication ?: return Result.success()
        val inboxRepo = app.container.inboxRepository
        val cfg = inboxRepo.config.value
        if (!cfg.autoCheckEnabled || !cfg.isConfigured || !inboxRepo.hasSavedToken.value) {
            return Result.success()
        }
        return try {
            // Somente leitura/prévia + notificação local: nunca importa dados em segundo plano sem confirmação.
            inboxRepo.checkInboxNow(fromBackgroundWorker = true)
            Result.success()
        } catch (ex: Exception) {
            app.container.repository.logError("InboxSyncWorker", ex)
            Result.success()
        }
    }
}

object InboxSyncScheduler {
    private const val WORK_NAME_INBOX_CHECK = "nexus_inbox_periodic_check"

    fun syncSchedule(context: Context, enabled: Boolean) {
        val workManager = WorkManager.getInstance(context.applicationContext)
        if (!enabled) {
            workManager.cancelUniqueWork(WORK_NAME_INBOX_CHECK)
            return
        }

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val request = PeriodicWorkRequestBuilder<InboxSyncWorker>(
            15,
            TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniquePeriodicWork(
            WORK_NAME_INBOX_CHECK,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}
