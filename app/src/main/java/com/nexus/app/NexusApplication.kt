package com.nexus.app

import android.app.Application
import com.nexus.app.core.GlobalErrorHandler
import com.nexus.app.data.backup.BackupManager
import com.nexus.app.data.db.NexusDatabase
import com.nexus.app.data.inbox.AndroidKeystoreInboxTokenStore
import com.nexus.app.data.inbox.GitHubInboxRemoteDataSource
import com.nexus.app.data.inbox.InboxRepository
import com.nexus.app.data.inbox.InboxSyncScheduler
import com.nexus.app.data.inbox.SharedPrefsInboxConfigStorage
import com.nexus.app.data.market.MarketAlertRepository
import com.nexus.app.data.market.MarketHistoryRepository
import com.nexus.app.data.market.MarketQuoteRepository
import com.nexus.app.data.market.MarketQuoteSyncScheduler
import com.nexus.app.data.market.MarketWatchlistRepository
import com.nexus.app.data.repository.NexusRepository
import com.nexus.app.notifications.NexusNotificationChannels
import com.nexus.app.notifications.NexusNotificationScheduler
import com.nexus.app.widgets.NexusWidgetUpdater

class AppContainer(application: Application) {
    val database: NexusDatabase by lazy {
        NexusDatabase.build(application)
    }

    val backupManager: BackupManager by lazy {
        BackupManager(
            database = database,
            marketWatchlistRepository = marketWatchlistRepository,
            marketAlertRepository = marketAlertRepository
        )
    }

    val notificationScheduler: NexusNotificationScheduler by lazy {
        NexusNotificationScheduler(application)
    }

    val marketAlertRepository: MarketAlertRepository by lazy {
        MarketAlertRepository.createDefault(application) { event ->
            notificationScheduler.showMarketAlertNotification(
                alert = event.alert,
                observedQuote = event.observedQuote
            )
        }
    }

    val marketQuoteRepository: MarketQuoteRepository by lazy {
        MarketQuoteRepository.createDefault(application) { freshQuotes ->
            marketAlertRepository.evaluateQuotes(freshQuotes)
        }
    }

    val marketHistoryRepository: MarketHistoryRepository by lazy {
        MarketHistoryRepository.createDefault(application)
    }

    val marketWatchlistRepository: MarketWatchlistRepository by lazy {
        MarketWatchlistRepository.createDefault(application)
    }

    val repository: NexusRepository by lazy {
        NexusRepository(
            database = database,
            backupManager = backupManager,
            notificationScheduler = notificationScheduler,
            onDataChanged = { NexusWidgetUpdater.updateAllWidgets(application) }
        )
    }

    val inboxRepository: InboxRepository by lazy {
        InboxRepository(
            nexusRepository = repository,
            configStorage = SharedPrefsInboxConfigStorage(application),
            tokenStore = AndroidKeystoreInboxTokenStore(application),
            remoteDataSource = GitHubInboxRemoteDataSource(),
            notificationScheduler = notificationScheduler,
            onAutoCheckPreferenceChanged = { enabled ->
                InboxSyncScheduler.syncSchedule(application, enabled)
            }
        )
    }
}

class NexusApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        NexusNotificationChannels.ensureChannelsCreated(this)
        container = AppContainer(this)
        container.notificationScheduler.enqueuePeriodicSyncWorker()
        MarketQuoteSyncScheduler.enqueuePeriodicSync(this)
        val inboxCfg = container.inboxRepository.config.value
        if (inboxCfg.autoCheckEnabled && inboxCfg.isConfigured && container.inboxRepository.hasSavedToken.value) {
            InboxSyncScheduler.syncSchedule(this, enabled = true)
        }
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler(
            GlobalErrorHandler(
                databaseProvider = { container.database },
                previousHandler = defaultHandler
            )
        )
    }
}
