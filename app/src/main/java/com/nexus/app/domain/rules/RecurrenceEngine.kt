package com.nexus.app.domain.rules

import com.nexus.app.core.DateTimeUtils
import com.nexus.app.data.db.entity.RecurrenceExceptionEntity
import com.nexus.app.data.db.entity.TaskEntity
import com.nexus.app.data.db.entity.TaskRecurrenceEntity
import com.nexus.app.domain.model.RecurrenceExceptionType
import com.nexus.app.domain.model.TaskRecurrenceFrequency
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.TextStyle

data class RecurrenceOccurrenceProjection(
    val recurrence: TaskRecurrenceEntity,
    val occurrenceDate: LocalDate,
    val dueAtEpoch: Long,
    val hasDueTime: Boolean,
    val effectiveTitle: String,
    val isOverride: Boolean = false
)

object RecurrenceEngine {

    private const val HARD_GENERATION_CAP = 1000

    fun weekdayBit(dayOfWeek: DayOfWeek): Int = 1 shl (dayOfWeek.value - 1)

    fun maskFromWeekdays(days: Set<DayOfWeek>): Int {
        var mask = 0
        days.forEach { mask = mask or weekdayBit(it) }
        return mask
    }

    fun weekdaysFromMask(mask: Int): Set<DayOfWeek> {
        if (mask <= 0) return emptySet()
        return DayOfWeek.entries.filterTo(linkedSetOf()) { day ->
            (mask and weekdayBit(day)) != 0
        }
    }

    fun minutesOfDayFromTime(time: LocalTime): Int = time.hour * 60 + time.minute

    fun timeFromMinutesOfDay(minutesOfDay: Int?): LocalTime {
        val clamped = (minutesOfDay ?: (9 * 60)).coerceIn(0, 23 * 60 + 59)
        return LocalTime.of(clamped / 60, clamped % 60)
    }

    fun computeDueEpoch(
        date: LocalDate,
        hasDueTime: Boolean,
        dueTimeMinutesOfDay: Int?
    ): Long {
        val time = if (hasDueTime) {
            timeFromMinutesOfDay(dueTimeMinutesOfDay)
        } else {
            LocalTime.of(23, 59)
        }
        return DateTimeUtils.localDateTimeToEpoch(date, time)
    }

    fun parseLocalDateOrNull(iso: String?): LocalDate? {
        if (iso.isNullOrBlank()) return null
        return runCatching { LocalDate.parse(iso.trim()) }.getOrNull()
    }

    /**
     * Gera as datas programadas da série a partir de `startDateIso` até [upToDateInclusive],
     * respeitando `endDateIso` e `maxOccurrences` sem nunca entrar em loop infinito.
     */
    fun generateScheduledDates(
        recurrence: TaskRecurrenceEntity,
        upToDateInclusive: LocalDate,
        hardCap: Int = HARD_GENERATION_CAP
    ): List<LocalDate> {
        val startDate = parseLocalDateOrNull(recurrence.startDateIso) ?: return emptyList()
        val endDate = parseLocalDateOrNull(recurrence.endDateIso)
        val effectiveEnd = if (endDate != null && endDate.isBefore(upToDateInclusive)) {
            endDate
        } else {
            upToDateInclusive
        }
        if (effectiveEnd.isBefore(startDate)) return emptyList()

        val maxCount = recurrence.maxOccurrences?.takeIf { it > 0 }?.coerceAtMost(hardCap) ?: hardCap
        val results = mutableListOf<LocalDate>()

        when (recurrence.frequency) {
            TaskRecurrenceFrequency.DAILY -> {
                var cursor = startDate
                while (!cursor.isAfter(effectiveEnd) && results.size < maxCount) {
                    results.add(cursor)
                    cursor = cursor.plusDays(1)
                }
            }

            TaskRecurrenceFrequency.WEEKLY -> {
                var cursor = startDate
                while (!cursor.isAfter(effectiveEnd) && results.size < maxCount) {
                    results.add(cursor)
                    cursor = cursor.plusWeeks(1)
                }
            }

            TaskRecurrenceFrequency.BIWEEKLY -> {
                var cursor = startDate
                while (!cursor.isAfter(effectiveEnd) && results.size < maxCount) {
                    results.add(cursor)
                    cursor = cursor.plusWeeks(2)
                }
            }

            TaskRecurrenceFrequency.MONTHLY -> {
                var step = 0L
                while (results.size < maxCount) {
                    // plusMonths(step) em startDate preserva o dia âncora (ex: 31 -> 28/29 -> 31).
                    val candidate = startDate.plusMonths(step)
                    if (candidate.isAfter(effectiveEnd)) break
                    results.add(candidate)
                    step++
                }
            }

            TaskRecurrenceFrequency.CUSTOM_DAYS -> {
                val interval = recurrence.customIntervalDays.coerceAtLeast(1).toLong()
                var cursor = startDate
                while (!cursor.isAfter(effectiveEnd) && results.size < maxCount) {
                    results.add(cursor)
                    cursor = cursor.plusDays(interval)
                }
            }

            TaskRecurrenceFrequency.SPECIFIC_WEEKDAYS -> {
                val allowedDays = weekdaysFromMask(recurrence.weekdaysMask)
                    .ifEmpty { setOf(startDate.dayOfWeek) }
                var cursor = startDate
                var guardDays = 0
                val maxDayScan = maxCount * 7 + 31
                while (!cursor.isAfter(effectiveEnd) && results.size < maxCount && guardDays < maxDayScan) {
                    if (cursor.dayOfWeek in allowedDays) {
                        results.add(cursor)
                    }
                    cursor = cursor.plusDays(1)
                    guardDays++
                }
            }
        }

        return results
    }

    /**
     * Projeta ocorrências virtuais para a janela da Agenda, omitindo datas já materializadas
     * na tabela `tasks` e respeitando exceções `SKIP` / `OVERRIDE` sem duplicar registros.
     */
    fun projectVirtualOccurrences(
        window: DateRange,
        recurrences: List<TaskRecurrenceEntity>,
        exceptions: List<RecurrenceExceptionEntity>,
        materializedTasks: List<TaskEntity>
    ): List<RecurrenceOccurrenceProjection> {
        if (recurrences.isEmpty()) return emptyList()

        val materializedKeys = buildSet {
            materializedTasks.forEach { task ->
                val recId = task.recurrenceId ?: return@forEach
                val occIso = task.occurrenceDateIso
                    ?: task.dueAt?.let { DateTimeUtils.epochToLocalDate(it).toString() }
                if (occIso != null) {
                    add(recId to occIso)
                }
            }
        }
        val exceptionMap = exceptions.associateBy { it.recurrenceId to it.occurrenceDateIso }

        val projections = mutableListOf<RecurrenceOccurrenceProjection>()
        recurrences.forEach { recurrence ->
            if (!recurrence.isActive) return@forEach
            val dates = generateScheduledDates(recurrence, window.endInclusive)
            dates.forEach { scheduledDate ->
                val scheduledIso = scheduledDate.toString()
                if ((recurrence.id to scheduledIso) in materializedKeys) return@forEach

                val exception = exceptionMap[recurrence.id to scheduledIso]
                if (exception?.exceptionType == RecurrenceExceptionType.SKIP) return@forEach

                val effectiveDueAt = if (exception?.exceptionType == RecurrenceExceptionType.OVERRIDE && exception.overrideDueAt != null) {
                    exception.overrideDueAt
                } else {
                    computeDueEpoch(
                        date = scheduledDate,
                        hasDueTime = recurrence.hasDueTime,
                        dueTimeMinutesOfDay = recurrence.dueTimeMinutesOfDay
                    )
                }
                val effectiveDate = DateTimeUtils.epochToLocalDate(effectiveDueAt)
                if (effectiveDate !in window) return@forEach

                val effectiveTitle = if (exception?.exceptionType == RecurrenceExceptionType.OVERRIDE && !exception.overrideTitle.isNullOrBlank()) {
                    exception.overrideTitle
                } else {
                    recurrence.title
                }

                projections += RecurrenceOccurrenceProjection(
                    recurrence = recurrence,
                    occurrenceDate = effectiveDate,
                    dueAtEpoch = effectiveDueAt,
                    hasDueTime = recurrence.hasDueTime,
                    effectiveTitle = effectiveTitle,
                    isOverride = exception?.exceptionType == RecurrenceExceptionType.OVERRIDE
                )
            }
        }
        return projections
    }

    /**
     * Calcula a próxima data de ocorrência estritamente após [afterDateInclusive]
     * mantendo a cadência original da série (mesmo se uma ocorrência foi adiada),
     * pulando ocorrências marcadas como SKIP ou já materializadas no SQLite.
     */
    fun nextOccurrenceDateAfter(
        recurrence: TaskRecurrenceEntity,
        afterDateInclusive: LocalDate,
        exceptions: List<RecurrenceExceptionEntity> = emptyList(),
        materializedTasks: List<TaskEntity> = emptyList()
    ): LocalDate? {
        if (!recurrence.isActive) return null
        val startDate = parseLocalDateOrNull(recurrence.startDateIso) ?: return null
        val endDate = parseLocalDateOrNull(recurrence.endDateIso)
        if (endDate != null && !endDate.isAfter(afterDateInclusive)) return null

        val searchAnchor = maxOf(startDate, afterDateInclusive)
        val searchHorizon = when (recurrence.frequency) {
            TaskRecurrenceFrequency.DAILY -> searchAnchor.plusDays(60)
            TaskRecurrenceFrequency.WEEKLY -> searchAnchor.plusWeeks(16)
            TaskRecurrenceFrequency.BIWEEKLY -> searchAnchor.plusWeeks(24)
            TaskRecurrenceFrequency.MONTHLY -> searchAnchor.plusMonths(24)
            TaskRecurrenceFrequency.CUSTOM_DAYS -> searchAnchor.plusDays((recurrence.customIntervalDays.coerceAtLeast(1) * 16).toLong())
            TaskRecurrenceFrequency.SPECIFIC_WEEKDAYS -> searchAnchor.plusWeeks(16)
        }
        val cappedHorizon = if (endDate != null && endDate.isBefore(searchHorizon)) endDate else searchHorizon

        val scheduledDates = generateScheduledDates(recurrence, cappedHorizon)
        val skippedDates = exceptions
            .filter { it.recurrenceId == recurrence.id && it.exceptionType == RecurrenceExceptionType.SKIP }
            .mapTo(hashSetOf()) { it.occurrenceDateIso }
        val materializedDates = materializedTasks
            .filter { it.recurrenceId == recurrence.id }
            .mapNotNullTo(hashSetOf()) { it.occurrenceDateIso }

        return scheduledDates.firstOrNull { candidate ->
            val iso = candidate.toString()
            candidate.isAfter(afterDateInclusive) && iso !in skippedDates && iso !in materializedDates
        }
    }

    fun activeScheduledDatesForHorizon(
        recurrences: List<TaskRecurrenceEntity>,
        exceptions: List<RecurrenceExceptionEntity>,
        materializedTasks: List<TaskEntity>,
        today: LocalDate = DateTimeUtils.todayLocalDate(),
        horizonDays: Long = 30L
    ): List<RecurrenceOccurrenceProjection> {
        val window = DateRange(today, today.plusDays(horizonDays.coerceAtLeast(1L)))
        return projectVirtualOccurrences(window, recurrences, exceptions, materializedTasks)
    }

    fun formatRecurrenceSummary(recurrence: TaskRecurrenceEntity): String {
        val freqLabel = when (recurrence.frequency) {
            TaskRecurrenceFrequency.DAILY -> "Diariamente"
            TaskRecurrenceFrequency.WEEKLY -> "Semanalmente"
            TaskRecurrenceFrequency.BIWEEKLY -> "Quinzenalmente"
            TaskRecurrenceFrequency.MONTHLY -> "Mensalmente"
            TaskRecurrenceFrequency.CUSTOM_DAYS -> "A cada ${recurrence.customIntervalDays.coerceAtLeast(1)} dias"
            TaskRecurrenceFrequency.SPECIFIC_WEEKDAYS -> {
                val days = weekdaysFromMask(recurrence.weekdaysMask)
                if (days.isEmpty()) {
                    "Dias específicos"
                } else {
                    days.joinToString(", ") { day ->
                        day.getDisplayName(TextStyle.SHORT, DateTimeUtils.LOCALE_PT_BR)
                            .replace(".", "")
                            .replaceFirstChar { it.uppercase(DateTimeUtils.LOCALE_PT_BR) }
                    }
                }
            }
        }
        val endDate = parseLocalDateOrNull(recurrence.endDateIso)
        return buildString {
            append(freqLabel)
            if (endDate != null) {
                append(" · até ${DateTimeUtils.formatDate(endDate)}")
            } else if (recurrence.maxOccurrences != null && recurrence.maxOccurrences > 0) {
                append(" · máx. ${recurrence.maxOccurrences}x")
            }
        }
    }
}
