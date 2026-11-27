package com.nexus.app.domain.rules

import com.nexus.app.core.DateTimeUtils
import com.nexus.app.data.db.entity.AppointmentEntity
import com.nexus.app.data.db.entity.EventEntity
import com.nexus.app.data.db.entity.RecurrenceExceptionEntity
import com.nexus.app.data.db.entity.TaskEntity
import com.nexus.app.data.db.entity.TaskRecurrenceEntity
import com.nexus.app.domain.model.RecurrenceDomain
import com.nexus.app.domain.model.RecurrenceExceptionType
import com.nexus.app.domain.model.TaskRecurrenceFrequency
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
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

@Serializable
data class ChecklistTemplateSnapshot(
    val version: Int = 1,
    val effectiveFromDateIso: String = "",
    val items: List<String> = emptyList()
)

data class RecurrenceAppointmentProjection(
    val recurrence: TaskRecurrenceEntity,
    val scheduledDate: LocalDate,
    val occurrenceDateIso: String,
    val startAt: Long,
    val endAt: Long,
    val isAllDay: Boolean,
    val effectiveTitle: String,
    val effectiveDescription: String,
    val effectiveLocation: String,
    val effectiveLink: String,
    val isOverride: Boolean = false
) {
    fun toVirtualAppointmentEntity(): AppointmentEntity = AppointmentEntity(
        id = RecurrenceEngine.virtualOccurrenceId(recurrence.id, occurrenceDateIso),
        title = effectiveTitle,
        description = effectiveDescription,
        categoryId = recurrence.categoryId,
        eventId = recurrence.eventId,
        startAt = startAt,
        endAt = endAt,
        isAllDay = isAllDay,
        location = effectiveLocation,
        link = effectiveLink,
        recurrenceId = recurrence.id,
        occurrenceDateIso = occurrenceDateIso,
        createdAt = recurrence.createdAt,
        updatedAt = recurrence.updatedAt
    )
}

data class RecurrenceEventProjection(
    val recurrence: TaskRecurrenceEntity,
    val scheduledDate: LocalDate,
    val occurrenceDateIso: String,
    val startAt: Long,
    val endAt: Long,
    val isAllDay: Boolean,
    val effectiveTitle: String,
    val effectiveDescription: String,
    val effectiveOfficialLink: String,
    val effectiveTicketLink: String,
    val effectiveLocationName: String,
    val effectiveAddress: String,
    val effectiveCity: String,
    val effectiveState: String,
    val isOverride: Boolean = false
) {
    fun toVirtualEventEntity(): EventEntity = EventEntity(
        id = RecurrenceEngine.virtualOccurrenceId(recurrence.id, occurrenceDateIso),
        name = effectiveTitle,
        description = effectiveDescription,
        categoryId = recurrence.categoryId,
        startAt = startAt,
        endAt = endAt,
        isAllDay = isAllDay,
        officialLink = effectiveOfficialLink,
        ticketLink = effectiveTicketLink,
        locationName = effectiveLocationName,
        address = effectiveAddress,
        city = effectiveCity,
        state = effectiveState,
        notes = recurrence.notes,
        eventStatus = recurrence.eventStatus,
        ticketStatus = recurrence.ticketStatus,
        ticketPriceCents = recurrence.ticketPriceCents,
        ticketQuantity = recurrence.ticketQuantity,
        ticketFeesCents = recurrence.ticketFeesCents,
        ticketTotalCents = 0L,
        ticketPurchaseDate = null,
        ticketDueDate = null,
        paymentMethod = recurrence.paymentMethod,
        creditCardId = recurrence.creditCardId,
        installmentsCount = recurrence.installmentsCount,
        recurrenceId = recurrence.id,
        occurrenceDateIso = occurrenceDateIso,
        createdAt = recurrence.createdAt,
        updatedAt = recurrence.updatedAt
    )
}

object RecurrenceEngine {

    private const val HARD_GENERATION_CAP = 1000
    private const val VIRTUAL_ID_PREFIX = "rec::"
    private val checklistJson = Json { ignoreUnknownKeys = true }

    fun virtualOccurrenceId(recurrenceId: String, occurrenceDateIso: String): String =
        "$VIRTUAL_ID_PREFIX$recurrenceId::$occurrenceDateIso"

    fun parseVirtualOccurrenceId(rawId: String?): Pair<String, String>? {
        if (rawId.isNullOrBlank() || !rawId.startsWith(VIRTUAL_ID_PREFIX)) return null
        val parts = rawId.split("::")
        if (parts.size != 3) return null
        val recurrenceId = parts[1].trim()
        val occurrenceDateIso = parts[2].trim()
        if (recurrenceId.isEmpty() || occurrenceDateIso.isEmpty()) return null
        if (parseLocalDateOrNull(occurrenceDateIso) == null) return null
        return recurrenceId to occurrenceDateIso
    }

    fun serializeChecklistTemplate(titles: List<String>): String {
        val cleaned = titles.mapNotNull { it.trim().takeIf(String::isNotBlank) }
        return checklistJson.encodeToString(ListSerializer(String.serializer()), cleaned)
    }

    fun deserializeChecklistTemplate(rawJson: String?): List<String> {
        if (rawJson.isNullOrBlank() || rawJson.trim() == "[]") return emptyList()
        return runCatching {
            checklistJson.decodeFromString(ListSerializer(String.serializer()), rawJson)
                .mapNotNull { it.trim().takeIf(String::isNotBlank) }
        }.getOrDefault(emptyList())
    }

    fun serializeTemplateHistory(snapshots: List<ChecklistTemplateSnapshot>): String {
        val cleaned = snapshots
            .filter { it.effectiveFromDateIso.isNotBlank() }
            .sortedWith(compareBy<ChecklistTemplateSnapshot>({ it.effectiveFromDateIso }, { it.version }))
        return checklistJson.encodeToString(ListSerializer(ChecklistTemplateSnapshot.serializer()), cleaned)
    }

    fun deserializeTemplateHistory(rawJson: String?): List<ChecklistTemplateSnapshot> {
        if (rawJson.isNullOrBlank() || rawJson.trim() == "[]") return emptyList()
        return runCatching {
            checklistJson.decodeFromString(ListSerializer(ChecklistTemplateSnapshot.serializer()), rawJson)
                .map { snap ->
                    snap.copy(items = snap.items.mapNotNull { it.trim().takeIf(String::isNotBlank) })
                }
                .filter { it.effectiveFromDateIso.isNotBlank() }
                .sortedWith(compareBy<ChecklistTemplateSnapshot>({ it.effectiveFromDateIso }, { it.version }))
        }.getOrDefault(emptyList())
    }

    /**
     * Resolve de forma determinística e NÃO-retroativa qual versão do template de checklist se aplica
     * a uma ocorrência identificada por [occurrenceDateIso].
     *
     * Ocorrências anteriores à vigência de revisões mais novas preservam versões históricas;
     * ocorrências na data de vigência ou posteriores recebem a revisão mais recente aplicável.
     */
    fun resolveChecklistTemplateVersionAndItems(
        recurrence: TaskRecurrenceEntity,
        occurrenceDateIso: String
    ): Pair<Int, List<String>> {
        val history = deserializeTemplateHistory(recurrence.templateHistoryJson)
        if (history.isEmpty()) {
            return recurrence.checklistTemplateVersion.coerceAtLeast(1) to
                deserializeChecklistTemplate(recurrence.templateSubtasksJson)
        }
        val targetDate = parseLocalDateOrNull(occurrenceDateIso)
        if (targetDate == null) {
            val latest = history.last()
            return latest.version.coerceAtLeast(1) to latest.items
        }
        val applicable = history.lastOrNull { snap ->
            val effectiveDate = parseLocalDateOrNull(snap.effectiveFromDateIso)
            effectiveDate != null && !effectiveDate.isAfter(targetDate)
        }
        val chosen = applicable ?: history.first()
        return chosen.version.coerceAtLeast(1) to chosen.items
    }

    /**
     * Constrói (templateSubtasksJson, checklistTemplateVersion, templateHistoryJson) preservando versões
     * anteriores para ocorrências passadas/já materializadas e aplicando a nova versão a partir de [effectiveFromDateIso].
     */
    fun buildUpdatedChecklistTemplate(
        existing: TaskRecurrenceEntity?,
        newTitles: List<String>,
        effectiveFromDateIso: String
    ): Triple<String, Int, String> {
        val cleaned = newTitles.mapNotNull { it.trim().takeIf(String::isNotBlank) }
        val newSubtasksJson = serializeChecklistTemplate(cleaned)
        if (existing == null) {
            val initialHistory = if (cleaned.isEmpty() || effectiveFromDateIso.isBlank()) {
                emptyList()
            } else {
                listOf(
                    ChecklistTemplateSnapshot(
                        version = 1,
                        effectiveFromDateIso = effectiveFromDateIso,
                        items = cleaned
                    )
                )
            }
            return Triple(newSubtasksJson, 1, serializeTemplateHistory(initialHistory))
        }

        val previousItems = deserializeChecklistTemplate(existing.templateSubtasksJson)
        val currentVersion = existing.checklistTemplateVersion.coerceAtLeast(1)
        val existingHistory = deserializeTemplateHistory(existing.templateHistoryJson).toMutableList()

        if (previousItems == cleaned) {
            if (existingHistory.isEmpty() && cleaned.isNotEmpty() && existing.startDateIso.isNotBlank()) {
                existingHistory += ChecklistTemplateSnapshot(
                    version = currentVersion,
                    effectiveFromDateIso = existing.startDateIso,
                    items = cleaned
                )
                return Triple(existing.templateSubtasksJson, currentVersion, serializeTemplateHistory(existingHistory))
            }
            return Triple(existing.templateSubtasksJson, currentVersion, existing.templateHistoryJson)
        }

        if (existingHistory.isEmpty() && existing.startDateIso.isNotBlank()) {
            existingHistory += ChecklistTemplateSnapshot(
                version = currentVersion,
                effectiveFromDateIso = existing.startDateIso,
                items = previousItems
            )
        }
        val nextVersion = (existingHistory.maxOfOrNull { it.version } ?: currentVersion) + 1
        existingHistory += ChecklistTemplateSnapshot(
            version = nextVersion,
            effectiveFromDateIso = effectiveFromDateIso.ifBlank { existing.startDateIso },
            items = cleaned
        )
        return Triple(newSubtasksJson, nextVersion, serializeTemplateHistory(existingHistory))
    }

    fun parseWeekdaysStrict(weekdays: List<String>?, rawMask: Int?): Result<Int> {
        if (rawMask != null && rawMask > 0) {
            if (rawMask > 0b1111111) {
                return Result.failure(IllegalArgumentException("weekdaysMask inválido: $rawMask"))
            }
            return Result.success(rawMask)
        }
        val items = weekdays.orEmpty()
        if (items.isEmpty()) return Result.success(0)
        val parsed = linkedSetOf<DayOfWeek>()
        items.forEach { token ->
            val normalized = token.trim().uppercase()
                .replace("Á", "A")
                .replace("Ã", "A")
                .replace("É", "E")
                .replace("Í", "I")
                .replace("Ó", "O")
                .replace("Ú", "U")
                .replace("Ç", "C")
            val day = when (normalized) {
                "MONDAY", "MON", "SEG", "SEGUNDA", "SEGUNDA-FEIRA", "1" -> DayOfWeek.MONDAY
                "TUESDAY", "TUE", "TER", "TERCA", "TERCA-FEIRA", "2" -> DayOfWeek.TUESDAY
                "WEDNESDAY", "WED", "QUA", "QUARTA", "QUARTA-FEIRA", "3" -> DayOfWeek.WEDNESDAY
                "THURSDAY", "THU", "QUI", "QUINTA", "QUINTA-FEIRA", "4" -> DayOfWeek.THURSDAY
                "FRIDAY", "FRI", "SEX", "SEXTA", "SEXTA-FEIRA", "5" -> DayOfWeek.FRIDAY
                "SATURDAY", "SAT", "SAB", "SABADO", "6" -> DayOfWeek.SATURDAY
                "SUNDAY", "SUN", "DOM", "DOMINGO", "7", "0" -> DayOfWeek.SUNDAY
                else -> return Result.failure(IllegalArgumentException("Dia da semana inválido: $token"))
            }
            parsed += day
        }
        return Result.success(maskFromWeekdays(parsed))
    }

    fun parseTimeOfDayMinutesStrict(raw: String?): Result<Int?> {
        if (raw == null) return Result.success(null)
        val clean = raw.trim()
        if (clean.isEmpty()) return Result.success(null)
        val match = Regex("^(\\d{1,2}):(\\d{2})$").matchEntire(clean)
            ?: return Result.failure(IllegalArgumentException("Horário inválido (use HH:mm): $raw"))
        val hour = match.groupValues[1].toIntOrNull()
        val minute = match.groupValues[2].toIntOrNull()
        if (hour == null || minute == null || hour !in 0..23 || minute !in 0..59) {
            return Result.failure(IllegalArgumentException("Horário fora do intervalo 00:00-23:59: $raw"))
        }
        return Result.success(hour * 60 + minute)
    }

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

    fun computeScheduleStartEpoch(
        date: LocalDate,
        isAllDay: Boolean,
        dueTimeMinutesOfDay: Int?
    ): Long {
        return if (isAllDay) {
            DateTimeUtils.startOfDayEpoch(date)
        } else {
            DateTimeUtils.localDateTimeToEpoch(date, timeFromMinutesOfDay(dueTimeMinutesOfDay))
        }
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

            TaskRecurrenceFrequency.ANNUAL -> {
                var step = 0L
                while (results.size < maxCount) {
                    // plusYears(step) em startDate preserva mês/dia âncora (ex: 29/02 -> 28/02 -> 29/02).
                    val candidate = startDate.plusYears(step)
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
     * Projeta ocorrências virtuais de tarefas para a janela da Agenda, omitindo datas já materializadas
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
            if (!recurrence.isActive || recurrence.domain != RecurrenceDomain.TASK) return@forEach
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
     * Projeta ocorrências virtuais de compromissos recorrentes cujo intervalo intercepta [window].
     */
    fun projectVirtualAppointments(
        window: DateRange,
        recurrences: List<TaskRecurrenceEntity>,
        exceptions: List<RecurrenceExceptionEntity>,
        materializedAppointments: List<AppointmentEntity>
    ): List<RecurrenceAppointmentProjection> {
        if (recurrences.isEmpty()) return emptyList()

        val materializedKeys = buildSet {
            materializedAppointments.forEach { appt ->
                val recId = appt.recurrenceId ?: return@forEach
                val occIso = appt.occurrenceDateIso
                    ?: DateTimeUtils.toLocalDate(appt.startAt).toString()
                add(recId to occIso)
            }
        }
        val exceptionMap = exceptions.associateBy { it.recurrenceId to it.occurrenceDateIso }

        val projections = mutableListOf<RecurrenceAppointmentProjection>()
        recurrences.forEach { recurrence ->
            if (!recurrence.isActive || recurrence.domain != RecurrenceDomain.APPOINTMENT) return@forEach
            val dates = generateScheduledDates(recurrence, window.endInclusive.plusDays(7))
            val baseDuration = recurrence.durationMillis.coerceAtLeast(60_000L)
            dates.forEach { scheduledDate ->
                val scheduledIso = scheduledDate.toString()
                if ((recurrence.id to scheduledIso) in materializedKeys) return@forEach

                val exception = exceptionMap[recurrence.id to scheduledIso]
                if (exception?.exceptionType == RecurrenceExceptionType.SKIP) return@forEach

                val isOverride = exception?.exceptionType == RecurrenceExceptionType.OVERRIDE
                val effectiveAllDay = if (isOverride && exception?.overrideIsAllDay != null) {
                    exception.overrideIsAllDay
                } else {
                    recurrence.isAllDay
                }
                val startAt = if (isOverride && exception?.overrideDueAt != null) {
                    exception.overrideDueAt
                } else {
                    computeScheduleStartEpoch(
                        date = scheduledDate,
                        isAllDay = effectiveAllDay,
                        dueTimeMinutesOfDay = recurrence.dueTimeMinutesOfDay
                    )
                }
                val endAt = if (isOverride && exception?.overrideEndAt != null) {
                    exception.overrideEndAt.coerceAtLeast(startAt + 60_000L)
                } else if (effectiveAllDay) {
                    DateTimeUtils.localDateToEndOfDayEpoch(DateTimeUtils.toLocalDate(startAt + baseDuration - 60_000L).coerceAtLeast(DateTimeUtils.toLocalDate(startAt)))
                } else {
                    startAt + baseDuration
                }

                val spanStart = DateTimeUtils.toLocalDate(startAt)
                val spanEnd = maxOf(spanStart, DateTimeUtils.toLocalDate(endAt))
                if (spanEnd.isBefore(window.startInclusive) || spanStart.isAfter(window.endInclusive)) {
                    return@forEach
                }

                val effectiveTitle = if (isOverride && !exception?.overrideTitle.isNullOrBlank()) {
                    exception!!.overrideTitle!!
                } else {
                    recurrence.title
                }
                val effectiveDesc = if (isOverride && exception?.overrideDescription != null) {
                    exception.overrideDescription
                } else {
                    recurrence.description
                }
                val effectiveLocation = if (isOverride && exception?.overrideLocation != null) {
                    exception.overrideLocation
                } else {
                    recurrence.location
                }
                val effectiveLink = if (isOverride && exception?.overrideLink != null) {
                    exception.overrideLink
                } else {
                    recurrence.link
                }

                projections += RecurrenceAppointmentProjection(
                    recurrence = recurrence,
                    scheduledDate = scheduledDate,
                    occurrenceDateIso = scheduledIso,
                    startAt = startAt,
                    endAt = endAt,
                    isAllDay = effectiveAllDay,
                    effectiveTitle = effectiveTitle,
                    effectiveDescription = effectiveDesc,
                    effectiveLocation = effectiveLocation,
                    effectiveLink = effectiveLink,
                    isOverride = isOverride
                )
            }
        }
        return projections
    }

    /**
     * Projeta ocorrências virtuais de eventos recorrentes (inclusive multi-dia) cujo intervalo intercepta [window].
     * Cada ocorrência mantém [occurrenceDateIso] como data-âncora da série mesmo quando abrange vários dias.
     */
    fun projectVirtualEvents(
        window: DateRange,
        recurrences: List<TaskRecurrenceEntity>,
        exceptions: List<RecurrenceExceptionEntity>,
        materializedEvents: List<EventEntity>
    ): List<RecurrenceEventProjection> {
        if (recurrences.isEmpty()) return emptyList()

        val materializedKeys = buildSet {
            materializedEvents.forEach { ev ->
                val recId = ev.recurrenceId ?: return@forEach
                val occIso = ev.occurrenceDateIso
                    ?: DateTimeUtils.toLocalDate(ev.startAt).toString()
                add(recId to occIso)
            }
        }
        val exceptionMap = exceptions.associateBy { it.recurrenceId to it.occurrenceDateIso }

        val projections = mutableListOf<RecurrenceEventProjection>()
        recurrences.forEach { recurrence ->
            if (!recurrence.isActive || recurrence.domain != RecurrenceDomain.EVENT) return@forEach
            val dates = generateScheduledDates(recurrence, window.endInclusive.plusDays(7))
            val baseDuration = recurrence.durationMillis.coerceAtLeast(60_000L)
            dates.forEach { scheduledDate ->
                val scheduledIso = scheduledDate.toString()
                if ((recurrence.id to scheduledIso) in materializedKeys) return@forEach

                val exception = exceptionMap[recurrence.id to scheduledIso]
                if (exception?.exceptionType == RecurrenceExceptionType.SKIP) return@forEach

                val isOverride = exception?.exceptionType == RecurrenceExceptionType.OVERRIDE
                val effectiveAllDay = if (isOverride && exception?.overrideIsAllDay != null) {
                    exception.overrideIsAllDay
                } else {
                    recurrence.isAllDay
                }
                val startAt = if (isOverride && exception?.overrideDueAt != null) {
                    exception.overrideDueAt
                } else {
                    computeScheduleStartEpoch(
                        date = scheduledDate,
                        isAllDay = effectiveAllDay,
                        dueTimeMinutesOfDay = recurrence.dueTimeMinutesOfDay
                    )
                }
                val endAt = if (isOverride && exception?.overrideEndAt != null) {
                    exception.overrideEndAt.coerceAtLeast(startAt + 60_000L)
                } else if (effectiveAllDay) {
                    val spanEndDate = DateTimeUtils.toLocalDate(startAt + baseDuration - 60_000L)
                        .coerceAtLeast(DateTimeUtils.toLocalDate(startAt))
                    DateTimeUtils.localDateToEndOfDayEpoch(spanEndDate)
                } else {
                    startAt + baseDuration
                }

                val spanStart = DateTimeUtils.toLocalDate(startAt)
                val spanEnd = maxOf(spanStart, DateTimeUtils.toLocalDate(endAt))
                if (spanEnd.isBefore(window.startInclusive) || spanStart.isAfter(window.endInclusive)) {
                    return@forEach
                }

                val effectiveTitle = if (isOverride && !exception?.overrideTitle.isNullOrBlank()) {
                    exception!!.overrideTitle!!
                } else {
                    recurrence.title
                }
                val effectiveDesc = if (isOverride && exception?.overrideDescription != null) {
                    exception.overrideDescription
                } else {
                    recurrence.description
                }
                val effectiveLocationName = if (isOverride && exception?.overrideLocation != null) {
                    exception.overrideLocation
                } else {
                    recurrence.locationName
                }
                val effectiveOfficialLink = if (isOverride && exception?.overrideLink != null) {
                    exception.overrideLink
                } else {
                    recurrence.officialLink
                }

                projections += RecurrenceEventProjection(
                    recurrence = recurrence,
                    scheduledDate = scheduledDate,
                    occurrenceDateIso = scheduledIso,
                    startAt = startAt,
                    endAt = endAt,
                    isAllDay = effectiveAllDay,
                    effectiveTitle = effectiveTitle,
                    effectiveDescription = effectiveDesc,
                    effectiveOfficialLink = effectiveOfficialLink,
                    effectiveTicketLink = recurrence.ticketLink,
                    effectiveLocationName = effectiveLocationName,
                    effectiveAddress = recurrence.address,
                    effectiveCity = recurrence.city,
                    effectiveState = recurrence.state,
                    isOverride = isOverride
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
        val materializedDates = materializedTasks
            .filter { it.recurrenceId == recurrence.id }
            .mapNotNullTo(hashSetOf()) { it.occurrenceDateIso }
        return nextOccurrenceDateForSeries(
            recurrence = recurrence,
            afterDateInclusive = afterDateInclusive,
            exceptions = exceptions,
            materializedDates = materializedDates
        )
    }

    fun nextOccurrenceDateForSeries(
        recurrence: TaskRecurrenceEntity,
        afterDateInclusive: LocalDate,
        exceptions: List<RecurrenceExceptionEntity> = emptyList(),
        materializedDates: Set<String> = emptySet()
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
            TaskRecurrenceFrequency.ANNUAL -> searchAnchor.plusYears(4)
            TaskRecurrenceFrequency.CUSTOM_DAYS -> searchAnchor.plusDays((recurrence.customIntervalDays.coerceAtLeast(1) * 16).toLong())
            TaskRecurrenceFrequency.SPECIFIC_WEEKDAYS -> searchAnchor.plusWeeks(16)
        }
        val cappedHorizon = if (endDate != null && endDate.isBefore(searchHorizon)) endDate else searchHorizon

        val scheduledDates = generateScheduledDates(recurrence, cappedHorizon)
        val skippedDates = exceptions
            .filter { it.recurrenceId == recurrence.id && it.exceptionType == RecurrenceExceptionType.SKIP }
            .mapTo(hashSetOf()) { it.occurrenceDateIso }

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
            TaskRecurrenceFrequency.ANNUAL -> "Anualmente"
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
