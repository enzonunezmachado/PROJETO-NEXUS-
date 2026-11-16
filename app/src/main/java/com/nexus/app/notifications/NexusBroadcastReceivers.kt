package com.nexus.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.nexus.app.NexusApplication
import com.nexus.app.domain.model.BriefingType
import com.nexus.app.widgets.NexusWidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NexusReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != NexusNotificationScheduler.ACTION_FIRE_REMINDER) return
        val reminderId = intent.getStringExtra(NexusNotificationScheduler.EXTRA_REMINDER_ID) ?: return
        val app = context.applicationContext as? NexusApplication ?: return
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                app.container.repository.handleAlarmFired(reminderId)
            } catch (ex: Exception) {
                app.container.repository.logError("NexusReminderReceiver", ex)
            } finally {
                pendingResult.finish()
            }
        }
    }
}

class NexusNotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as? NexusApplication ?: return
        val reminderId = intent.getStringExtra(NexusNotificationScheduler.EXTRA_REMINDER_ID)
        val sourceId = intent.getStringExtra(NexusNotificationScheduler.EXTRA_SOURCE_ID)
        val notifId = intent.getIntExtra(NexusNotificationScheduler.EXTRA_NOTIFICATION_ID, -1)
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    NexusNotificationScheduler.ACTION_COMPLETE_TASK -> {
                        if (!sourceId.isNullOrBlank()) {
                            app.container.repository.completeTaskFromNotification(sourceId)
                        }
                    }

                    NexusNotificationScheduler.ACTION_SETTLE_TRANSACTION -> {
                        if (!sourceId.isNullOrBlank()) {
                            app.container.repository.settleTransactionFromNotification(sourceId)
                        }
                    }

                    NexusNotificationScheduler.ACTION_SNOOZE_REMINDER -> {
                        if (!reminderId.isNullOrBlank()) {
                            app.container.repository.snoozeReminder(reminderId, 15)
                        }
                    }

                    NexusNotificationScheduler.ACTION_DISMISS_REMINDER -> {
                        if (!reminderId.isNullOrBlank()) {
                            app.container.repository.dismissReminder(reminderId)
                        }
                    }

                    NexusNotificationScheduler.ACTION_MARK_BRIEFING_REVIEWED -> {
                        val typeName = intent.getStringExtra(NexusNotificationScheduler.EXTRA_BRIEFING_TYPE)
                        val type = BriefingType.fromStringOrDefault(typeName)
                        app.container.repository.markBriefingReviewed(type)
                    }
                }
                if (notifId > 0) {
                    androidx.core.app.NotificationManagerCompat.from(context.applicationContext)
                        .cancel(notifId)
                }
            } catch (ex: Exception) {
                app.container.repository.logError("NexusNotificationActionReceiver", ex)
            } finally {
                pendingResult.finish()
            }
        }
    }
}

class NexusBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action !in SUPPORTED_ACTIONS) return
        val app = context.applicationContext as? NexusApplication ?: return
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                NexusNotificationChannels.ensureChannelsCreated(context.applicationContext)
                app.container.repository.rescheduleAllReminders()
                app.container.notificationScheduler.enqueuePeriodicSyncWorker()
                NexusWidgetUpdater.updateAllWidgets(context.applicationContext)
            } catch (ex: Exception) {
                app.container.repository.logError("NexusBootReceiver", ex)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private val SUPPORTED_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED
        )
    }
}
