package com.nexus.app.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.nexus.app.domain.model.ReminderCategory
import com.nexus.app.domain.model.ReminderSourceType

object NexusNotificationChannels {
    const val CHANNEL_SCHEDULE = "nexus_schedule"
    const val CHANNEL_FINANCE = "nexus_finance"
    const val CHANNEL_EVENTS = "nexus_events"
    const val CHANNEL_ROUTINE = "nexus_routine"

    fun ensureChannelsCreated(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return

        val channels = listOf(
            NotificationChannel(
                CHANNEL_SCHEDULE,
                "Agenda e Tarefas",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Lembretes de compromissos e tarefas com prazo."
                enableVibration(true)
            },
            NotificationChannel(
                CHANNEL_FINANCE,
                "Financeiro e Faturas",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alertas de contas a pagar, parcelas e faturas de cartão."
                enableVibration(true)
            },
            NotificationChannel(
                CHANNEL_EVENTS,
                "Eventos",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Lembretes de eventos confirmados e programados."
            },
            NotificationChannel(
                CHANNEL_ROUTINE,
                "Assistente de Rotina",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Briefing diário da manhã e revisão noturna do NEXUS."
            }
        )
        manager.createNotificationChannels(channels)
    }

    fun channelFor(sourceType: ReminderSourceType, category: ReminderCategory): String {
        return when {
            category == ReminderCategory.FINANCIAL ||
                sourceType == ReminderSourceType.TRANSACTION ||
                sourceType == ReminderSourceType.STATEMENT -> CHANNEL_FINANCE

            sourceType == ReminderSourceType.EVENT -> CHANNEL_EVENTS
            sourceType == ReminderSourceType.ROUTINE_MORNING ||
                sourceType == ReminderSourceType.ROUTINE_NIGHT ||
                category == ReminderCategory.ROUTINE -> CHANNEL_ROUTINE

            else -> CHANNEL_SCHEDULE
        }
    }
}
