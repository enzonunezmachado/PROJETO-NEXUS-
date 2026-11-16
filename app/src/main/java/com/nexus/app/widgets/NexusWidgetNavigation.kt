package com.nexus.app.widgets

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.nexus.app.MainActivity
import java.time.LocalDate

/**
 * Constroi Intents com URIs exclusivos (`nexus://open/...`) para evitar que o
 * AppWidgetManager reutilize o mesmo PendingIntent entre linhas diferentes.
 */
object NexusWidgetNavigation {
    const val SCHEME = "nexus"
    const val HOST = "open"

    const val TARGET_HOME = "home"
    const val TARGET_AGENDA = "agenda"
    const val TARGET_TASKS = "tasks"
    const val TARGET_FINANCE = "finance"
    const val TARGET_FINANCE_CARDS = "finance_cards"
    const val TARGET_EVENTS = "events"
    const val TARGET_APPOINTMENT = "appointment"
    const val TARGET_TASK = "task"
    const val TARGET_TRANSACTION = "transaction"
    const val TARGET_EVENT = "event"

    const val EXTRA_WIDGET_TARGET = "extra_widget_target"
    const val EXTRA_WIDGET_RECORD_ID = "extra_widget_record_id"
    const val EXTRA_WIDGET_DATE_ISO = "extra_widget_date_iso"

    fun openHomeIntent(context: Context): Intent =
        baseMainIntent(context, Uri.parse("$SCHEME://$HOST/$TARGET_HOME"), TARGET_HOME)

    fun openAgendaIntent(context: Context, date: LocalDate? = null): Intent {
        val uri = if (date != null) {
            Uri.parse("$SCHEME://$HOST/$TARGET_AGENDA?date=$date")
        } else {
            Uri.parse("$SCHEME://$HOST/$TARGET_AGENDA")
        }
        return baseMainIntent(context, uri, TARGET_AGENDA).apply {
            if (date != null) putExtra(EXTRA_WIDGET_DATE_ISO, date.toString())
        }
    }

    fun openTasksIntent(context: Context): Intent =
        baseMainIntent(context, Uri.parse("$SCHEME://$HOST/$TARGET_TASKS"), TARGET_TASKS)

    fun openFinanceIntent(context: Context): Intent =
        baseMainIntent(context, Uri.parse("$SCHEME://$HOST/$TARGET_FINANCE"), TARGET_FINANCE)

    fun openFinanceCardsIntent(context: Context): Intent =
        baseMainIntent(context, Uri.parse("$SCHEME://$HOST/$TARGET_FINANCE_CARDS"), TARGET_FINANCE_CARDS)

    fun openEventsIntent(context: Context): Intent =
        baseMainIntent(context, Uri.parse("$SCHEME://$HOST/$TARGET_EVENTS"), TARGET_EVENTS)

    fun openAppointmentIntent(context: Context, appointmentId: String): Intent =
        baseMainIntent(
            context,
            Uri.parse("$SCHEME://$HOST/$TARGET_APPOINTMENT/$appointmentId"),
            TARGET_APPOINTMENT,
            appointmentId
        )

    fun openTaskIntent(context: Context, taskId: String): Intent =
        baseMainIntent(
            context,
            Uri.parse("$SCHEME://$HOST/$TARGET_TASK/$taskId"),
            TARGET_TASK,
            taskId
        )

    fun openTransactionIntent(context: Context, transactionId: String): Intent =
        baseMainIntent(
            context,
            Uri.parse("$SCHEME://$HOST/$TARGET_TRANSACTION/$transactionId"),
            TARGET_TRANSACTION,
            transactionId
        )

    fun openEventIntent(context: Context, eventId: String): Intent =
        baseMainIntent(
            context,
            Uri.parse("$SCHEME://$HOST/$TARGET_EVENT/$eventId"),
            TARGET_EVENT,
            eventId
        )

    fun openWidgetConfigIntent(context: Context, appWidgetId: Int): Intent =
        Intent(context, NexusWidgetConfigActivity::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_CONFIGURE
            data = Uri.parse("$SCHEME://widget-config/$appWidgetId")
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

    private fun baseMainIntent(
        context: Context,
        uri: Uri,
        target: String,
        recordId: String? = null
    ): Intent = Intent(context, MainActivity::class.java).apply {
        action = Intent.ACTION_VIEW
        data = uri
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        putExtra(EXTRA_WIDGET_TARGET, target)
        if (!recordId.isNullOrBlank()) {
            putExtra(EXTRA_WIDGET_RECORD_ID, recordId)
        }
    }
}
