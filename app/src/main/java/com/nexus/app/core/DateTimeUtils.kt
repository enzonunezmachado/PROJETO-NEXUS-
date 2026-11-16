package com.nexus.app.core

import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

object DateTimeUtils {
    val ZONE_SAO_PAULO: ZoneId = ZoneId.of("America/Sao_Paulo")
    val LOCALE_PT_BR: Locale = Locale("pt", "BR")

    private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", LOCALE_PT_BR)
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", LOCALE_PT_BR)
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm", LOCALE_PT_BR)
    private val dayMonthFormatter = DateTimeFormatter.ofPattern("dd/MM", LOCALE_PT_BR)
    private val monthYearFormatter = DateTimeFormatter.ofPattern("MMMM 'de' yyyy", LOCALE_PT_BR)
    private val brlCurrencyFormatter = NumberFormat.getCurrencyInstance(LOCALE_PT_BR)

    fun nowEpochMillis(): Long = System.currentTimeMillis()

    fun todayLocalDate(): LocalDate = LocalDate.now(ZONE_SAO_PAULO)

    fun toLocalDate(epochMs: Long): LocalDate =
        Instant.ofEpochMilli(epochMs).atZone(ZONE_SAO_PAULO).toLocalDate()

    fun startOfDayEpoch(date: LocalDate): Long =
        date.atStartOfDay(ZONE_SAO_PAULO).toInstant().toEpochMilli()

    fun epochToDatePickerMillis(epochMs: Long): Long =
        toLocalDate(epochMs).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    fun datePickerMillisToEpoch(pickerMillis: Long, keepTimeFrom: Long? = null): Long {
        val pickedDate = Instant.ofEpochMilli(pickerMillis).atZone(ZoneOffset.UTC).toLocalDate()
        val time = if (keepTimeFrom != null) epochToLocalTime(keepTimeFrom) else LocalTime.MIDNIGHT
        return LocalDateTime.of(pickedDate, time).atZone(ZONE_SAO_PAULO).toInstant().toEpochMilli()
    }

    fun epochToLocalDate(epochMillis: Long): LocalDate = toLocalDate(epochMillis)

    fun epochToLocalTime(epochMillis: Long): LocalTime =
        Instant.ofEpochMilli(epochMillis).atZone(ZONE_SAO_PAULO).toLocalTime()

    fun epochToLocalDateTime(epochMillis: Long): LocalDateTime =
        Instant.ofEpochMilli(epochMillis).atZone(ZONE_SAO_PAULO).toLocalDateTime()

    fun localDateToStartOfDayEpoch(date: LocalDate): Long = startOfDayEpoch(date)

    fun localDateToEndOfDayEpoch(date: LocalDate): Long =
        date.atTime(LocalTime.MAX).atZone(ZONE_SAO_PAULO).toInstant().toEpochMilli()

    fun localDateTimeToEpoch(date: LocalDate, time: LocalTime): Long =
        LocalDateTime.of(date, time).atZone(ZONE_SAO_PAULO).toInstant().toEpochMilli()

    fun formatDate(epochMillis: Long): String =
        epochToLocalDateTime(epochMillis).format(dateFormatter)

    fun formatDate(date: LocalDate): String =
        date.format(dateFormatter)

    fun formatTime(epochMillis: Long): String =
        epochToLocalDateTime(epochMillis).format(timeFormatter)

    fun formatDateTime(epochMillis: Long): String =
        epochToLocalDateTime(epochMillis).format(dateTimeFormatter)

    fun formatDayMonth(date: LocalDate): String =
        date.format(dayMonthFormatter)

    fun formatMonthYear(date: LocalDate): String =
        date.format(monthYearFormatter).replaceFirstChar { it.titlecase(LOCALE_PT_BR) }

    fun formatFullDateWithWeekday(date: LocalDate): String {
        val weekday = date.dayOfWeek.getDisplayName(TextStyle.FULL, LOCALE_PT_BR)
            .replaceFirstChar { it.titlecase(LOCALE_PT_BR) }
        return "$weekday, ${date.format(dateFormatter)}"
    }

    fun formatDateByPattern(date: LocalDate, format: com.nexus.app.domain.model.NexusDateFormat): String {
        val weekday = date.dayOfWeek.getDisplayName(TextStyle.FULL, LOCALE_PT_BR)
            .replaceFirstChar { it.titlecase(LOCALE_PT_BR) }
        return when (format) {
            com.nexus.app.domain.model.NexusDateFormat.WEEKDAY_DD_MM_YYYY -> "$weekday, ${date.format(dateFormatter)}"
            com.nexus.app.domain.model.NexusDateFormat.DD_MM_YYYY -> date.format(dateFormatter)
            com.nexus.app.domain.model.NexusDateFormat.DD_MMM_YYYY -> {
                val month = date.month.getDisplayName(TextStyle.FULL, LOCALE_PT_BR)
                "${date.dayOfMonth} de $month de ${date.year}"
            }
            com.nexus.app.domain.model.NexusDateFormat.DD_MM_WEEKDAY -> "${date.format(dayMonthFormatter)} · $weekday"
        }
    }

    fun greetingForHour(userName: String, hour: Int): String {
        val prefix = when {
            hour < 12 -> "Bom dia"
            hour < 18 -> "Boa tarde"
            else -> "Boa noite"
        }
        val cleanName = userName.trim().ifEmpty { "Enzo" }
        return "$prefix, $cleanName"
    }

    fun greetingForCurrentHour(userName: String): String =
        greetingForHour(userName, LocalTime.now(ZONE_SAO_PAULO).hour)

    fun formatGreeting(userName: String, template: String): String {
        val cleanName = userName.trim().ifEmpty { "Enzo" }
        val cleanTemplate = template.trim()
        if (cleanTemplate.isEmpty() || cleanTemplate.equals("AUTO", ignoreCase = true)) {
            return greetingForCurrentHour(cleanName)
        }
        return cleanTemplate.replace("{nome}", cleanName, ignoreCase = true)
    }

    fun toIsoUtc(epochMillis: Long): String =
        Instant.ofEpochMilli(epochMillis).toString()

    fun formatBrl(amountCents: Long): String =
        brlCurrencyFormatter.format(amountCents / 100.0)

    fun parseBrlToCents(raw: String): Long? {
        val cleaned = raw.trim()
            .replace("R$", "")
            .replace(" ", "")
        if (cleaned.isEmpty()) return null
        val normalized = if (cleaned.contains(",")) {
            cleaned.replace(".", "").replace(",", ".")
        } else {
            cleaned
        }
        val parsed = normalized.toDoubleOrNull() ?: return null
        return kotlin.math.round(parsed * 100.0).toLong()
    }

    fun centsToInputString(cents: Long): String {
        if (cents == 0L) return ""
        return String.format(LOCALE_PT_BR, "%.2f", cents / 100.0)
    }
}
