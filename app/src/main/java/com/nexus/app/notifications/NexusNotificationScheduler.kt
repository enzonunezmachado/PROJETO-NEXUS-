package com.nexus.app.notifications

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.nexus.app.MainActivity
import com.nexus.app.R
import com.nexus.app.core.DateTimeUtils
import com.nexus.app.data.db.entity.ReminderEntity
import com.nexus.app.data.db.entity.RoutineAssistantSettingsEntity
import com.nexus.app.domain.model.BriefingType
import com.nexus.app.domain.model.MarketAlertRules
import com.nexus.app.domain.model.MarketAssetCatalog
import com.nexus.app.domain.model.MarketPriceAlert
import com.nexus.app.domain.model.MarketQuoteSnapshot
import com.nexus.app.domain.model.ReminderCategory
import com.nexus.app.domain.model.ReminderSourceType
import com.nexus.app.domain.rules.ReminderRules
import java.time.LocalDate
import java.time.LocalTime
import java.util.concurrent.TimeUnit

class NexusNotificationScheduler(private val context: Context) {

    private val appContext: Context = context.applicationContext

    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun canUseExactAlarms(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val alarmManager = appContext.getSystemService(AlarmManager::class.java) ?: return false
        return alarmManager.canScheduleExactAlarms()
    }

    fun scheduleAlarm(reminder: ReminderEntity): Boolean {
        if (!ReminderRules.isSchedulableOrFirable(reminder)) {
            cancelAlarm(reminder.id)
            return false
        }
        val alarmManager = appContext.getSystemService(AlarmManager::class.java) ?: return false
        val pendingIntent = buildAlarmPendingIntent(reminder)
        val triggerAt = maxOf(reminder.triggerAt, DateTimeUtils.nowEpochMillis() + 1_000L)

        val exactAllowed = canUseExactAlarms()
        return try {
            if (exactAllowed) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAt,
                    pendingIntent
                )
                true
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAt,
                    pendingIntent
                )
                false
            }
        } catch (_: SecurityException) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAt,
                pendingIntent
            )
            false
        }
    }

    fun cancelAlarm(reminderId: String) {
        val alarmManager = appContext.getSystemService(AlarmManager::class.java) ?: return
        val intent = Intent(appContext, NexusReminderReceiver::class.java).apply {
            action = ACTION_FIRE_REMINDER
            putExtra(EXTRA_REMINDER_ID, reminderId)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            appContext,
            requestCodeFor(reminderId),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    fun cancelDisplayedNotification(reminderId: String) {
        NotificationManagerCompat.from(appContext).cancel(notificationIdFor(reminderId))
    }

    @SuppressLint("MissingPermission")
    fun showReminderNotification(reminder: ReminderEntity, userName: String): Boolean {
        if (!ReminderRules.isSchedulableOrFirable(reminder)) return false
        if (!hasNotificationPermission()) return false
        NexusNotificationChannels.ensureChannelsCreated(appContext)

        val notificationId = notificationIdFor(reminder.id)
        val contentIntent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_SOURCE_TYPE, reminder.sourceType.name)
            putExtra(EXTRA_SOURCE_ID, reminder.sourceId)
            putExtra(
                EXTRA_OPEN_ROUTINE,
                reminder.sourceType == ReminderSourceType.ROUTINE_MORNING ||
                    reminder.sourceType == ReminderSourceType.ROUTINE_NIGHT
            )
        }
        val contentPendingIntent = PendingIntent.getActivity(
            appContext,
            notificationId + 17,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val titleText = if (reminder.category == ReminderCategory.ROUTINE) {
            "NEXUS • ${reminder.title} ($userName)"
        } else {
            reminder.title
        }

        val builder = NotificationCompat.Builder(appContext, reminder.channelId)
            .setSmallIcon(R.drawable.ic_notification_nexus)
            .setContentTitle(titleText)
            .setContentText(reminder.message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(reminder.message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(
                when (reminder.category) {
                    ReminderCategory.FINANCIAL -> NotificationCompat.CATEGORY_REMINDER
                    ReminderCategory.SCHEDULE -> NotificationCompat.CATEGORY_EVENT
                    ReminderCategory.ROUTINE -> NotificationCompat.CATEGORY_STATUS
                }
            )
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)

        when (reminder.sourceType) {
            ReminderSourceType.TASK -> {
                builder.addAction(
                    0,
                    "Concluir",
                    buildActionPendingIntent(
                        action = ACTION_COMPLETE_TASK,
                        reminder = reminder,
                        notifId = notificationId,
                        salt = 101
                    )
                )
                builder.addAction(
                    0,
                    "Adiar 15m",
                    buildActionPendingIntent(
                        action = ACTION_SNOOZE_REMINDER,
                        reminder = reminder,
                        notifId = notificationId,
                        salt = 102
                    )
                )
            }

            ReminderSourceType.TRANSACTION -> {
                builder.addAction(
                    0,
                    "Marcar pago",
                    buildActionPendingIntent(
                        action = ACTION_SETTLE_TRANSACTION,
                        reminder = reminder,
                        notifId = notificationId,
                        salt = 201
                    )
                )
                builder.addAction(
                    0,
                    "Adiar 15m",
                    buildActionPendingIntent(
                        action = ACTION_SNOOZE_REMINDER,
                        reminder = reminder,
                        notifId = notificationId,
                        salt = 202
                    )
                )
            }

            ReminderSourceType.ROUTINE_MORNING, ReminderSourceType.ROUTINE_NIGHT -> {
                val briefingType = if (reminder.sourceType == ReminderSourceType.ROUTINE_MORNING) {
                    BriefingType.MORNING.name
                } else {
                    BriefingType.NIGHT_REVIEW.name
                }
                builder.addAction(
                    0,
                    "Marcar revisado",
                    buildActionPendingIntent(
                        action = ACTION_MARK_BRIEFING_REVIEWED,
                        reminder = reminder,
                        notifId = notificationId,
                        salt = 301,
                        extraBriefingType = briefingType
                    )
                )
            }

            else -> {
                builder.addAction(
                    0,
                    "Adiar 15m",
                    buildActionPendingIntent(
                        action = ACTION_SNOOZE_REMINDER,
                        reminder = reminder,
                        notifId = notificationId,
                        salt = 401
                    )
                )
                builder.addAction(
                    0,
                    "Dispensar",
                    buildActionPendingIntent(
                        action = ACTION_DISMISS_REMINDER,
                        reminder = reminder,
                        notifId = notificationId,
                        salt = 402
                    )
                )
            }
        }

        NotificationManagerCompat.from(appContext).notify(notificationId, builder.build())
        return true
    }

    @SuppressLint("MissingPermission")
    fun showTestNotification(summaryMessage: String): Boolean {
        if (!hasNotificationPermission()) return false
        NexusNotificationChannels.ensureChannelsCreated(appContext)
        val notifId = TEST_NOTIFICATION_ID
        val openIntent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_ROUTINE, true)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            appContext,
            notifId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(appContext, NexusNotificationChannels.CHANNEL_ROUTINE)
            .setSmallIcon(R.drawable.ic_notification_nexus)
            .setContentTitle("NEXUS • Teste de Notificação")
            .setContentText(summaryMessage)
            .setStyle(NotificationCompat.BigTextStyle().bigText(summaryMessage))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .build()

        NotificationManagerCompat.from(appContext).notify(notifId, notification)
        return true
    }

    @SuppressLint("MissingPermission")
    fun showMarketAlertNotification(
        alert: MarketPriceAlert,
        observedQuote: MarketQuoteSnapshot
    ): Boolean {
        if (!hasNotificationPermission()) return false
        val descriptor = MarketAssetCatalog.findByCode(alert.assetCode) ?: return false
        if (!MarketAlertRules.isQuoteCompatibleWithAsset(observedQuote, descriptor)) return false

        NexusNotificationChannels.ensureChannelsCreated(appContext)
        val notifId = marketAlertNotificationIdFor(alert.id)
        val openIntent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_MARKET_ASSET_CODE, descriptor.code)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            appContext,
            notifId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val observedFormatted = MarketAlertRules.formatValueForAsset(observedQuote.price, descriptor)
        val targetFormatted = MarketAlertRules.formatValueForAsset(alert.targetPrice, descriptor)
        val conditionText = alert.condition.shortLabel.lowercase()
        val bodyText =
            "${descriptor.ticker} atingiu $observedFormatted ($conditionText $targetFormatted · ${observedQuote.sourceName})."

        val notification = NotificationCompat.Builder(
            appContext,
            NexusNotificationChannels.CHANNEL_MARKET_ALERTS
        )
            .setSmallIcon(R.drawable.ic_notification_nexus)
            .setContentTitle("Alerta de Mercado · ${descriptor.displayName}")
            .setContentText(bodyText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bodyText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .build()

        NotificationManagerCompat.from(appContext).notify(notifId, notification)
        return true
    }

    @SuppressLint("MissingPermission")
    fun showInboxPendingNotification(
        pendingNewCount: Int,
        pendingUpdateCount: Int,
        filesCount: Int
    ): Boolean {
        if (!hasNotificationPermission()) return false
        val total = pendingNewCount + pendingUpdateCount
        if (total <= 0) return false
        NexusNotificationChannels.ensureChannelsCreated(appContext)
        val openIntent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_INBOX, true)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            appContext,
            INBOX_NOTIFICATION_ID,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val parts = buildList {
            if (pendingNewCount > 0) add("$pendingNewCount novo(s)")
            if (pendingUpdateCount > 0) add("$pendingUpdateCount atualização(ões)")
        }.joinToString(" e ")
        val bodyText = "NEXUS-INBOX tem $parts em $filesCount arquivo(s) aguardando sua confirmação."

        val notification = NotificationCompat.Builder(
            appContext,
            NexusNotificationChannels.CHANNEL_ROUTINE
        )
            .setSmallIcon(R.drawable.ic_notification_nexus)
            .setContentTitle("NEXUS-INBOX • Nova atualização pendente")
            .setContentText(bodyText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bodyText))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .build()

        NotificationManagerCompat.from(appContext).notify(INBOX_NOTIFICATION_ID, notification)
        return true
    }

    fun enqueuePeriodicSyncWorker() {
        val workRequest = PeriodicWorkRequestBuilder<NexusReminderSyncWorker>(
            15,
            TimeUnit.MINUTES
        ).build()

        WorkManager.getInstance(appContext).enqueueUniquePeriodicWork(
            WORK_NAME_PERIODIC_SYNC,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    private fun buildAlarmPendingIntent(reminder: ReminderEntity): PendingIntent {
        val intent = Intent(appContext, NexusReminderReceiver::class.java).apply {
            action = ACTION_FIRE_REMINDER
            putExtra(EXTRA_REMINDER_ID, reminder.id)
        }
        return PendingIntent.getBroadcast(
            appContext,
            requestCodeFor(reminder.id),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun buildActionPendingIntent(
        action: String,
        reminder: ReminderEntity,
        notifId: Int,
        salt: Int,
        extraBriefingType: String? = null
    ): PendingIntent {
        val intent = Intent(appContext, NexusNotificationActionReceiver::class.java).apply {
            this.action = action
            putExtra(EXTRA_REMINDER_ID, reminder.id)
            putExtra(EXTRA_SOURCE_ID, reminder.sourceId)
            putExtra(EXTRA_SOURCE_TYPE, reminder.sourceType.name)
            putExtra(EXTRA_NOTIFICATION_ID, notifId)
            if (extraBriefingType != null) {
                putExtra(EXTRA_BRIEFING_TYPE, extraBriefingType)
            }
        }
        return PendingIntent.getBroadcast(
            appContext,
            notifId * 31 + salt,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        const val ACTION_FIRE_REMINDER = "com.nexus.app.ACTION_FIRE_REMINDER"
        const val ACTION_COMPLETE_TASK = "com.nexus.app.ACTION_COMPLETE_TASK"
        const val ACTION_SETTLE_TRANSACTION = "com.nexus.app.ACTION_SETTLE_TRANSACTION"
        const val ACTION_SNOOZE_REMINDER = "com.nexus.app.ACTION_SNOOZE_REMINDER"
        const val ACTION_DISMISS_REMINDER = "com.nexus.app.ACTION_DISMISS_REMINDER"
        const val ACTION_MARK_BRIEFING_REVIEWED = "com.nexus.app.ACTION_MARK_BRIEFING_REVIEWED"

        const val EXTRA_REMINDER_ID = "extra_reminder_id"
        const val EXTRA_SOURCE_TYPE = "extra_source_type"
        const val EXTRA_SOURCE_ID = "extra_source_id"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
        const val EXTRA_OPEN_ROUTINE = "extra_open_routine"
        const val EXTRA_BRIEFING_TYPE = "extra_briefing_type"
        const val EXTRA_MARKET_ASSET_CODE = "extra_market_asset_code"
        const val EXTRA_OPEN_INBOX = "extra_open_inbox"

        const val ROUTINE_MORNING_SOURCE_ID = "nexus_routine_morning"
        const val ROUTINE_NIGHT_SOURCE_ID = "nexus_routine_night"

        private const val WORK_NAME_PERIODIC_SYNC = "nexus_offline_reminder_sync"
        private const val TEST_NOTIFICATION_ID = 900_999
        private const val INBOX_NOTIFICATION_ID = 901_001

        fun requestCodeFor(id: String): Int = (id.hashCode() and 0x7FFFFFFF)
        fun notificationIdFor(id: String): Int = ((id.hashCode() and 0x00FFFFFF) + 1000)
        fun marketAlertNotificationIdFor(alertId: String): Int =
            ((alertId.hashCode() and 0x00FFFFFF) + 2_000_000)

        /**
         * Calcula o horario de disparo do lembrete.
         * Quando um item tem apenas data (sem horario especifico), dispara as 09:00 do dia
         * subtraindo [minutesBefore].
         */
        fun computeReminderTriggerEpoch(
            dueEpoch: Long,
            hasSpecificTime: Boolean,
            minutesBefore: Int
        ): Long {
            val effectiveAnchor = if (hasSpecificTime) {
                dueEpoch
            } else {
                val date = DateTimeUtils.epochToLocalDate(dueEpoch)
                DateTimeUtils.localDateTimeToEpoch(date, LocalTime.of(9, 0))
            }
            return effectiveAnchor - (minutesBefore.coerceAtLeast(0).toLong() * 60_000L)
        }

        /**
         * Ajusta o disparo para fora do modo silencio configurado pelo usuario,
         * respeitando o fuso America/Sao_Paulo.
         */
        fun adjustForQuietHours(
            triggerMillis: Long,
            settings: RoutineAssistantSettingsEntity
        ): Long {
            if (!settings.quietHoursEnabled) return triggerMillis
            val localDt = DateTimeUtils.epochToLocalDateTime(triggerMillis)
            val triggerTime = localDt.toLocalTime()
            val start = LocalTime.of(
                settings.quietHoursStartHour.coerceIn(0, 23),
                settings.quietHoursStartMinute.coerceIn(0, 59)
            )
            val end = LocalTime.of(
                settings.quietHoursEndHour.coerceIn(0, 23),
                settings.quietHoursEndMinute.coerceIn(0, 59)
            )
            if (start == end) return triggerMillis

            val isInsideQuietWindow = if (start < end) {
                !triggerTime.isBefore(start) && triggerTime.isBefore(end)
            } else {
                // Janela noturna cruzando meia-noite (ex.: 22:00 -> 07:00)
                !triggerTime.isBefore(start) || triggerTime.isBefore(end)
            }

            if (!isInsideQuietWindow) return triggerMillis

            val resumeDate: LocalDate = if (start < end) {
                localDt.toLocalDate()
            } else {
                if (!triggerTime.isBefore(start)) {
                    localDt.toLocalDate().plusDays(1)
                } else {
                    localDt.toLocalDate()
                }
            }
            return DateTimeUtils.localDateTimeToEpoch(resumeDate, end)
        }

        fun computeNextDailyRoutineEpoch(
            hour: Int,
            minute: Int,
            nowMillis: Long = DateTimeUtils.nowEpochMillis()
        ): Long {
            val nowDt = DateTimeUtils.epochToLocalDateTime(nowMillis)
            val targetTime = LocalTime.of(hour.coerceIn(0, 23), minute.coerceIn(0, 59))
            var targetDate = nowDt.toLocalDate()
            var candidate = DateTimeUtils.localDateTimeToEpoch(targetDate, targetTime)
            if (candidate <= nowMillis + 30_000L) {
                targetDate = targetDate.plusDays(1)
                candidate = DateTimeUtils.localDateTimeToEpoch(targetDate, targetTime)
            }
            return candidate
        }
    }
}
