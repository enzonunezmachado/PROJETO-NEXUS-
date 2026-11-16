package com.nexus.app

import android.app.Application
import com.nexus.app.core.GlobalErrorHandler
import com.nexus.app.data.backup.BackupManager
import com.nexus.app.data.db.NexusDatabase
import com.nexus.app.data.repository.NexusRepository
import com.nexus.app.notifications.NexusNotificationChannels
import com.nexus.app.notifications.NexusNotificationScheduler
import com.nexus.app.widgets.NexusWidgetUpdater

class AppContainer(application: Application) {
    val database: NexusDatabase by lazy {
        NexusDatabase.build(application)
    }

    val backupManager: BackupManager by lazy {
        BackupManager(database)
    }

    val notificationScheduler: NexusNotificationScheduler by lazy {
        NexusNotificationScheduler(application)
    }

    val repository: NexusRepository by lazy {
        NexusRepository(
            database = database,
            backupManager = backupManager,
            notificationScheduler = notificationScheduler,
            onDataChanged = { NexusWidgetUpdater.updateAllWidgets(application) }
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
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler(
            GlobalErrorHandler(
                databaseProvider = { container.database },
                previousHandler = defaultHandler
            )
        )
    }
}
