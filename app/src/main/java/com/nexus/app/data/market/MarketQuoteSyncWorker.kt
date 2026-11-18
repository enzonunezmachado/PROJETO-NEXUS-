package com.nexus.app.data.market

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

class MarketQuoteSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? NexusApplication ?: return Result.success()
        return try {
            app.container.marketQuoteRepository.refreshIfStale()
            Result.success()
        } catch (ex: Exception) {
            app.container.repository.logError("MarketQuoteSyncWorker", ex)
            Result.success()
        }
    }
}

object MarketQuoteSyncScheduler {
    private const val WORK_NAME_USD_BRL_SYNC = "nexus_market_quote_sync"

    fun enqueuePeriodicSync(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = PeriodicWorkRequestBuilder<MarketQuoteSyncWorker>(
            30,
            TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
            WORK_NAME_USD_BRL_SYNC,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}
