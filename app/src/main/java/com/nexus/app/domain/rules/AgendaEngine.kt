package com.nexus.app.domain.rules

import com.nexus.app.core.DateTimeUtils
import com.nexus.app.data.db.entity.AppointmentEntity
import com.nexus.app.data.db.entity.CategoryEntity
import com.nexus.app.data.db.entity.CreditCardEntity
import com.nexus.app.data.db.entity.CreditCardStatementEntity
import com.nexus.app.data.db.entity.EventEntity
import com.nexus.app.data.db.entity.ProjectEntity
import com.nexus.app.data.db.entity.RecurrenceExceptionEntity
import com.nexus.app.data.db.entity.TaskEntity
import com.nexus.app.data.db.entity.TaskRecurrenceEntity
import com.nexus.app.data.db.entity.TransactionEntity
import com.nexus.app.domain.model.CategoryScope
import com.nexus.app.domain.model.FinancialStatus
import com.nexus.app.domain.model.ProjectStatus
import com.nexus.app.domain.model.StatementStatus
import com.nexus.app.domain.model.TaskStatus
import com.nexus.app.domain.model.TransactionType
import java.time.LocalDate

data class DateRange(
    val startInclusive: LocalDate,
    val endInclusive: LocalDate
) {
    init {
        require(!endInclusive.isBefore(startInclusive)) {
            "DateRange inválido: $endInclusive < $startInclusive"
        }
    }

    operator fun contains(date: LocalDate): Boolean =
        !date.isBefore(startInclusive) && !date.isAfter(endInclusive)

    fun dates(): List<LocalDate> {
        val result = mutableListOf<LocalDate>()
        var cursor = startInclusive
        while (!cursor.isAfter(endInclusive)) {
            result.add(cursor)
            cursor = cursor.plusDays(1)
        }
        return result
    }
}

enum class AgendaOccurrenceKind(val label: String) {
    TASK("Tarefas"),
    APPOINTMENT("Compromissos"),
    EVENT("Eventos"),
    FINANCE("Financeiro"),
    PROJECT("Projetos")
}

enum class AgendaSourceType {
    TASK,
    APPOINTMENT,
    EVENT,
    TRANSACTION,
    STATEMENT,
    PROJECT
}

data class AgendaOccurrence(
    val sourceType: AgendaSourceType,
    val sourceId: String,
    val occurrenceDateIso: String,
    val startAt: Long,
    val endAt: Long?,
    val isAllDay: Boolean,
    val spanStartDateIso: String,
    val spanEndDateIso: String,
    val title: String,
    val subtitle: String,
    val status: String,
    val kind: AgendaOccurrenceKind,
    val categoryId: String? = null,
    val colorHex: String? = null,
    val isOverdue: Boolean = false,
    val isDoneOrSettled: Boolean = false,
    val hasConflict: Boolean = false,
    val conflictTitles: List<String> = emptyList(),
    val amountCents: Long? = null,
    val isIncome: Boolean = false,
    val sortEpochMillis: Long = startAt,
    val searchableText: String = "",
    val recurrenceId: String? = null,
    val isVirtualRecurrence: Boolean = false
) {
    val occurrenceDate: LocalDate
        get() = LocalDate.parse(occurrenceDateIso)

    val stableKey: String
        get() = if (isVirtualRecurrence) {
            "vrec:$sourceType:$sourceId:$occurrenceDateIso"
        } else {
            "$sourceType:$sourceId:$occurrenceDateIso"
        }
}

data class AgendaSources(
    val appointments: List<AppointmentEntity> = emptyList(),
    val tasks: List<TaskEntity> = emptyList(),
    val events: List<EventEntity> = emptyList(),
    val transactions: List<TransactionEntity> = emptyList(),
    val statements: List<CreditCardStatementEntity> = emptyList(),
    val creditCards: List<CreditCardEntity> = emptyList(),
    val projects: List<ProjectEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val taskRecurrences: List<TaskRecurrenceEntity> = emptyList(),
    val recurrenceExceptions: List<RecurrenceExceptionEntity> = emptyList()
)

data class AgendaFilters(
    val kindFilter: AgendaOccurrenceKind? = null,
    val categoryId: String? = null,
    val searchQuery: String = "",
    val showPaid: Boolean = false
)

object AgendaEngine {

    fun isTaskOverdue(
        task: TaskEntity,
        nowEpoch: Long = DateTimeUtils.nowEpochMillis(),
        today: LocalDate = DateTimeUtils.todayLocalDate()
    ): Boolean {
        val due = task.dueAt ?: return false
        if (task.status.isTerminal) return false
        return if (task.hasDueTime) {
            due < nowEpoch
        } else {
            DateTimeUtils.epochToLocalDate(due).isBefore(today)
        }
    }

    fun isAppointmentOnDate(
        appointment: AppointmentEntity,
        date: LocalDate
    ): Boolean {
        val spanStart = DateTimeUtils.toLocalDate(appointment.startAt)
        val spanEnd = maxOf(spanStart, DateTimeUtils.toLocalDate(appointment.endAt))
        return !date.isBefore(spanStart) && !date.isAfter(spanEnd)
    }

    fun isDirectAgendaBill(transaction: TransactionEntity): Boolean {
        return transaction.type == TransactionType.EXPENSE &&
            transaction.statementId == null &&
            transaction.status != FinancialStatus.SETTLED &&
            transaction.status != FinancialStatus.CANCELLED &&
            transaction.dueAt != null
    }

    fun computeAppointmentConflicts(appointments: List<AppointmentEntity>): Map<String, List<String>> {
        val timed = appointments.filter { !it.isAllDay }
        val result = mutableMapOf<String, MutableList<String>>()
        for (i in timed.indices) {
            for (j in (i + 1) until timed.size) {
                val a = timed[i]
                val b = timed[j]
                val overlaps = a.startAt < b.endAt && b.startAt < a.endAt
                if (overlaps) {
                    result.getOrPut(a.id) { mutableListOf() }.add(b.title)
                    result.getOrPut(b.id) { mutableListOf() }.add(a.title)
                }
            }
        }
        return result
    }

    fun boundingRangeForSources(
        sources: AgendaSources,
        fallbackDate: LocalDate = DateTimeUtils.todayLocalDate()
    ): DateRange {
        val dates = mutableListOf<LocalDate>()
        sources.appointments.forEach { appt ->
            val start = DateTimeUtils.toLocalDate(appt.startAt)
            val end = maxOf(start, DateTimeUtils.toLocalDate(appt.endAt))
            dates += start
            dates += end
        }
        sources.tasks.forEach { task ->
            task.dueAt?.let { dates += DateTimeUtils.toLocalDate(it) }
        }
        sources.events.forEach { ev ->
            val start = DateTimeUtils.toLocalDate(ev.startAt)
            val end = maxOf(start, DateTimeUtils.toLocalDate(ev.endAt))
            dates += start
            dates += end
        }
        sources.transactions.forEach { tx ->
            tx.dueAt?.let { dates += DateTimeUtils.toLocalDate(it) }
        }
        sources.statements.forEach { st ->
            dates += DateTimeUtils.toLocalDate(st.dueAt)
        }
        sources.projects.forEach { proj ->
            proj.deadlineAt?.let { dates += DateTimeUtils.toLocalDate(it) }
        }
        sources.taskRecurrences.forEach { rec ->
            if (rec.isActive) {
                RecurrenceEngine.parseLocalDateOrNull(rec.startDateIso)?.let { dates += it }
                val recHorizon = fallbackDate.plusDays(60)
                val endDate = RecurrenceEngine.parseLocalDateOrNull(rec.endDateIso)
                dates += if (endDate != null && endDate.isBefore(recHorizon)) endDate else recHorizon
            }
        }
        if (dates.isEmpty()) {
            return DateRange(fallbackDate, fallbackDate)
        }
        val minDate = dates.minOrNull() ?: fallbackDate
        val maxDate = dates.maxOrNull() ?: fallbackDate
        return DateRange(minDate, maxDate)
    }

    fun expand(
        window: DateRange,
        sources: AgendaSources,
        filters: AgendaFilters = AgendaFilters(),
        today: LocalDate = DateTimeUtils.todayLocalDate(),
        nowEpoch: Long = DateTimeUtils.nowEpochMillis()
    ): List<AgendaOccurrence> {
        val categoryMap = sources.categories.associateBy { it.id }
        val projectMap = sources.projects.associateBy { it.id }
        val eventMap = sources.events.associateBy { it.id }
        val cardMap = sources.creditCards.associateBy { it.id }
        val conflictMap = computeAppointmentConflicts(sources.appointments)
        val startOfToday = DateTimeUtils.startOfDayEpoch(today)
        val txByStatement = sources.transactions.groupBy { it.statementId }
        val txByEvent = sources.transactions.groupBy { it.eventId }

        val occurrences = mutableListOf<AgendaOccurrence>()

        // 1. Compromissos (suporta 1 ou mais dias)
        if (filters.kindFilter == null || filters.kindFilter == AgendaOccurrenceKind.APPOINTMENT) {
            sources.appointments.forEach { appt ->
                val spanStart = DateTimeUtils.toLocalDate(appt.startAt)
                val spanEnd = maxOf(spanStart, DateTimeUtils.toLocalDate(appt.endAt))
                val iterStart = maxOf(spanStart, window.startInclusive)
                val iterEnd = minOf(spanEnd, window.endInclusive)
                if (!iterStart.isAfter(iterEnd)) {
                    val cat = appt.categoryId?.let { categoryMap[it] }
                    val conflicts = conflictMap[appt.id].orEmpty()
                    val linkedEvent = appt.eventId?.let { eventMap[it] }
                    val timeLabel = if (appt.isAllDay || spanStart != spanEnd) {
                        "Dia inteiro"
                    } else {
                        "${DateTimeUtils.formatTime(appt.startAt)}–${DateTimeUtils.formatTime(appt.endAt)}"
                    }
                    val subtitle = buildList {
                        add(timeLabel)
                        cat?.let { add(it.name) }
                        if (appt.location.isNotBlank()) add(appt.location)
                        linkedEvent?.let { add("Evento: ${it.name}") }
                    }.joinToString(" · ")
                    val searchText = listOfNotNull(
                        appt.title,
                        appt.description,
                        appt.location,
                        cat?.name,
                        linkedEvent?.name
                    ).joinToString(" ").lowercase()

                    var day = iterStart
                    while (!day.isAfter(iterEnd)) {
                        occurrences += AgendaOccurrence(
                            sourceType = AgendaSourceType.APPOINTMENT,
                            sourceId = appt.id,
                            occurrenceDateIso = day.toString(),
                            startAt = appt.startAt,
                            endAt = appt.endAt,
                            isAllDay = appt.isAllDay || spanStart != spanEnd,
                            spanStartDateIso = spanStart.toString(),
                            spanEndDateIso = spanEnd.toString(),
                            title = appt.title,
                            subtitle = subtitle,
                            status = if (conflicts.isNotEmpty()) "CONFLICT" else "SCHEDULED",
                            kind = AgendaOccurrenceKind.APPOINTMENT,
                            categoryId = appt.categoryId,
                            colorHex = cat?.colorHex,
                            isOverdue = false,
                            isDoneOrSettled = false,
                            hasConflict = conflicts.isNotEmpty(),
                            conflictTitles = conflicts,
                            sortEpochMillis = appt.startAt,
                            searchableText = searchText
                        )
                        day = day.plusDays(1)
                    }
                }
            }
        }

        // 2. Tarefas (apenas se tiver dueAt; tarefa sem data não aparece na Agenda)
        if (filters.kindFilter == null || filters.kindFilter == AgendaOccurrenceKind.TASK) {
            sources.tasks.forEach { task ->
                val due = task.dueAt ?: return@forEach
                val dueDate = DateTimeUtils.toLocalDate(due)
                if (dueDate !in window) return@forEach

                val cat = task.categoryId?.let { categoryMap[it] }
                val proj = task.projectId?.let { projectMap[it] }
                val linkedEv = task.eventId?.let { eventMap[it] }
                val overdue = isTaskOverdue(task, nowEpoch, today)
                val done = task.status == TaskStatus.COMPLETED || task.status == TaskStatus.CANCELLED
                val whenLabel = if (task.hasDueTime) DateTimeUtils.formatTime(due) else DateTimeUtils.formatDate(due)
                val subtitle = buildList {
                    add(whenLabel)
                    add(task.status.label)
                    proj?.let { add(it.name) }
                    linkedEv?.let { add("Evento: ${it.name}") }
                }.joinToString(" · ")
                val searchText = listOfNotNull(
                    task.title,
                    task.description,
                    task.notes,
                    cat?.name,
                    proj?.name,
                    linkedEv?.name
                ).joinToString(" ").lowercase()

                occurrences += AgendaOccurrence(
                    sourceType = AgendaSourceType.TASK,
                    sourceId = task.id,
                    occurrenceDateIso = dueDate.toString(),
                    startAt = due,
                    endAt = null,
                    isAllDay = !task.hasDueTime,
                    spanStartDateIso = dueDate.toString(),
                    spanEndDateIso = dueDate.toString(),
                    title = task.title,
                    subtitle = subtitle,
                    status = task.status.name,
                    kind = AgendaOccurrenceKind.TASK,
                    categoryId = task.categoryId,
                    colorHex = cat?.colorHex,
                    isOverdue = overdue,
                    isDoneOrSettled = done,
                    sortEpochMillis = due,
                    searchableText = searchText,
                    recurrenceId = task.recurrenceId,
                    isVirtualRecurrence = false
                )
            }

            if (sources.taskRecurrences.isNotEmpty()) {
                val virtualProjections = RecurrenceEngine.projectVirtualOccurrences(
                    window = window,
                    recurrences = sources.taskRecurrences,
                    exceptions = sources.recurrenceExceptions,
                    materializedTasks = sources.tasks
                )
                virtualProjections.forEach { projOcc ->
                    val rec = projOcc.recurrence
                    val cat = rec.categoryId?.let { categoryMap[it] }
                    val proj = rec.projectId?.let { projectMap[it] }
                    val linkedEv = rec.eventId?.let { eventMap[it] }
                    val whenLabel = if (projOcc.hasDueTime) {
                        DateTimeUtils.formatTime(projOcc.dueAtEpoch)
                    } else {
                        DateTimeUtils.formatDate(projOcc.dueAtEpoch)
                    }
                    val subtitle = buildList {
                        add(whenLabel)
                        add("Recorrente (${rec.frequency.label})")
                        proj?.let { add(it.name) }
                        linkedEv?.let { add("Evento: ${it.name}") }
                    }.joinToString(" · ")
                    val searchText = listOfNotNull(
                        projOcc.effectiveTitle,
                        rec.description,
                        rec.notes,
                        cat?.name,
                        proj?.name,
                        linkedEv?.name
                    ).joinToString(" ").lowercase()

                    occurrences += AgendaOccurrence(
                        sourceType = AgendaSourceType.TASK,
                        sourceId = rec.id,
                        occurrenceDateIso = projOcc.occurrenceDate.toString(),
                        startAt = projOcc.dueAtEpoch,
                        endAt = null,
                        isAllDay = !projOcc.hasDueTime,
                        spanStartDateIso = projOcc.occurrenceDate.toString(),
                        spanEndDateIso = projOcc.occurrenceDate.toString(),
                        title = projOcc.effectiveTitle,
                        subtitle = subtitle,
                        status = TaskStatus.NOT_STARTED.name,
                        kind = AgendaOccurrenceKind.TASK,
                        categoryId = rec.categoryId,
                        colorHex = cat?.colorHex,
                        isOverdue = false,
                        isDoneOrSettled = false,
                        sortEpochMillis = projOcc.dueAtEpoch,
                        searchableText = searchText,
                        recurrenceId = rec.id,
                        isVirtualRecurrence = true
                    )
                }
            }
        }

        // 3. Eventos (expande para todos os dias entre startAt e endAt dentro da janela)
        if (filters.kindFilter == null || filters.kindFilter == AgendaOccurrenceKind.EVENT) {
            sources.events.forEach { ev ->
                val spanStart = DateTimeUtils.toLocalDate(ev.startAt)
                val spanEnd = maxOf(spanStart, DateTimeUtils.toLocalDate(ev.endAt))
                val iterStart = maxOf(spanStart, window.startInclusive)
                val iterEnd = minOf(spanEnd, window.endInclusive)
                if (!iterStart.isAfter(iterEnd)) {
                    val cat = ev.categoryId?.let { categoryMap[it] }
                    val isMultiDay = spanStart != spanEnd
                    val allDayOccurrence = ev.isAllDay || isMultiDay
                    val totalCostCents = txByEvent[ev.id].orEmpty()
                        .filter { it.type == TransactionType.EXPENSE && it.status != FinancialStatus.CANCELLED }
                        .sumOf { it.amountCents }
                    val timeLabel = when {
                        isMultiDay -> "${DateTimeUtils.formatDayMonth(spanStart)} – ${DateTimeUtils.formatDayMonth(spanEnd)} · Dia inteiro"
                        ev.isAllDay -> "Dia inteiro"
                        else -> "${DateTimeUtils.formatTime(ev.startAt)}–${DateTimeUtils.formatTime(ev.endAt)}"
                    }
                    val subtitle = buildList {
                        add(timeLabel)
                        add("Ingresso: ${ev.ticketStatus.label}")
                        if (ev.locationName.isNotBlank()) add(ev.locationName)
                        if (totalCostCents > 0L) add(DateTimeUtils.formatBrl(totalCostCents))
                    }.joinToString(" · ")
                    val searchText = listOfNotNull(
                        ev.name,
                        ev.description,
                        ev.locationName,
                        ev.city,
                        cat?.name
                    ).joinToString(" ").lowercase()

                    var day = iterStart
                    while (!day.isAfter(iterEnd)) {
                        occurrences += AgendaOccurrence(
                            sourceType = AgendaSourceType.EVENT,
                            sourceId = ev.id,
                            occurrenceDateIso = day.toString(),
                            startAt = ev.startAt,
                            endAt = ev.endAt,
                            isAllDay = allDayOccurrence,
                            spanStartDateIso = spanStart.toString(),
                            spanEndDateIso = spanEnd.toString(),
                            title = ev.name,
                            subtitle = subtitle,
                            status = ev.eventStatus.name,
                            kind = AgendaOccurrenceKind.EVENT,
                            categoryId = ev.categoryId,
                            colorHex = cat?.colorHex,
                            isOverdue = false,
                            isDoneOrSettled = ev.eventStatus.name == "ATTENDED" || ev.eventStatus.name == "CANCELLED",
                            amountCents = totalCostCents.takeIf { it > 0L },
                            sortEpochMillis = ev.startAt,
                            searchableText = searchText
                        )
                        day = day.plusDays(1)
                    }
                }
            }
        }

        // 4. Financeiro: transações sem fatura (statementId == null) e faturas com total > 0
        if (filters.kindFilter == null || filters.kindFilter == AgendaOccurrenceKind.FINANCE) {
            sources.transactions.forEach { rawTx ->
                if (rawTx.statementId != null) return@forEach // Compra ligada a fatura não aparece individualmente
                val due = rawTx.dueAt ?: return@forEach
                val effTx = FinanceRules.effectiveTransactionStatus(rawTx, today)
                if (effTx.status == FinancialStatus.CANCELLED) return@forEach
                if (!filters.showPaid && effTx.status == FinancialStatus.SETTLED) return@forEach

                val dueDate = DateTimeUtils.toLocalDate(due)
                if (dueDate !in window) return@forEach

                val cat = effTx.categoryId?.let { categoryMap[it] }
                val isOverdue = effTx.status == FinancialStatus.OVERDUE
                val isSettled = effTx.status == FinancialStatus.SETTLED
                val subtitle = buildList {
                    add(effTx.type.label)
                    add(effTx.status.label)
                    add(DateTimeUtils.formatDate(due))
                    cat?.let { add(it.name) }
                }.joinToString(" · ")
                val searchText = listOfNotNull(
                    effTx.description,
                    effTx.notes,
                    cat?.name
                ).joinToString(" ").lowercase()

                occurrences += AgendaOccurrence(
                    sourceType = AgendaSourceType.TRANSACTION,
                    sourceId = effTx.id,
                    occurrenceDateIso = dueDate.toString(),
                    startAt = due,
                    endAt = null,
                    isAllDay = true,
                    spanStartDateIso = dueDate.toString(),
                    spanEndDateIso = dueDate.toString(),
                    title = effTx.description,
                    subtitle = subtitle,
                    status = effTx.status.name,
                    kind = AgendaOccurrenceKind.FINANCE,
                    categoryId = effTx.categoryId,
                    colorHex = cat?.colorHex,
                    isOverdue = isOverdue,
                    isDoneOrSettled = isSettled,
                    amountCents = effTx.amountCents,
                    isIncome = effTx.type == TransactionType.INCOME,
                    sortEpochMillis = due,
                    searchableText = searchText
                )
            }

            sources.statements.forEach { rawStmt ->
                val effStatus = if (rawStmt.status != StatementStatus.PAID && rawStmt.dueAt < startOfToday) {
                    StatementStatus.OVERDUE
                } else {
                    rawStmt.status
                }
                if (!filters.showPaid && effStatus == StatementStatus.PAID) return@forEach

                val card = cardMap[rawStmt.creditCardId] ?: return@forEach
                val totalCents = txByStatement[rawStmt.id].orEmpty()
                    .filter { it.type == TransactionType.EXPENSE && it.status != FinancialStatus.CANCELLED }
                    .sumOf { it.amountCents }
                if (totalCents <= 0L) return@forEach

                val dueDate = DateTimeUtils.toLocalDate(rawStmt.dueAt)
                if (dueDate !in window) return@forEach

                val title = "Fatura ${card.name} (${rawStmt.yearMonth})"
                val subtitle = "Vencimento ${DateTimeUtils.formatDate(rawStmt.dueAt)} · ${effStatus.label}"
                val searchText = "$title ${card.institution}".lowercase()

                occurrences += AgendaOccurrence(
                    sourceType = AgendaSourceType.STATEMENT,
                    sourceId = rawStmt.id,
                    occurrenceDateIso = dueDate.toString(),
                    startAt = rawStmt.dueAt,
                    endAt = null,
                    isAllDay = true,
                    spanStartDateIso = dueDate.toString(),
                    spanEndDateIso = dueDate.toString(),
                    title = title,
                    subtitle = subtitle,
                    status = effStatus.name,
                    kind = AgendaOccurrenceKind.FINANCE,
                    categoryId = null,
                    colorHex = null,
                    isOverdue = effStatus == StatementStatus.OVERDUE,
                    isDoneOrSettled = effStatus == StatementStatus.PAID,
                    amountCents = totalCents,
                    isIncome = false,
                    sortEpochMillis = rawStmt.dueAt,
                    searchableText = searchText
                )
            }
        }

        // 5. Projetos com prazo (ativos/planejados/pausados)
        if (filters.kindFilter == null || filters.kindFilter == AgendaOccurrenceKind.PROJECT) {
            sources.projects.forEach { proj ->
                val deadline = proj.deadlineAt ?: return@forEach
                if (proj.status == ProjectStatus.COMPLETED || proj.status == ProjectStatus.ARCHIVED) {
                    return@forEach
                }
                val dueDate = DateTimeUtils.toLocalDate(deadline)
                if (dueDate !in window) return@forEach

                val overdue = dueDate.isBefore(today)
                val subtitle = "Prazo de projeto · ${proj.status.label} · ${DateTimeUtils.formatDate(dueDate)}"
                val searchText = "${proj.name} ${proj.description}".lowercase()

                occurrences += AgendaOccurrence(
                    sourceType = AgendaSourceType.PROJECT,
                    sourceId = proj.id,
                    occurrenceDateIso = dueDate.toString(),
                    startAt = deadline,
                    endAt = null,
                    isAllDay = true,
                    spanStartDateIso = dueDate.toString(),
                    spanEndDateIso = dueDate.toString(),
                    title = proj.name,
                    subtitle = subtitle,
                    status = proj.status.name,
                    kind = AgendaOccurrenceKind.PROJECT,
                    categoryId = null,
                    colorHex = null,
                    isOverdue = overdue,
                    isDoneOrSettled = false,
                    sortEpochMillis = deadline,
                    searchableText = searchText
                )
            }
        }

        val cleanQuery = filters.searchQuery.trim().lowercase()
        return occurrences
            .filter { occ ->
                val matchesCategory = filters.categoryId == null || occ.categoryId == filters.categoryId
                val matchesQuery = cleanQuery.isEmpty() ||
                    occ.title.lowercase().contains(cleanQuery) ||
                    occ.subtitle.lowercase().contains(cleanQuery) ||
                    occ.searchableText.contains(cleanQuery)
                matchesCategory && matchesQuery
            }
            .sortedWith(
                compareBy<AgendaOccurrence> { it.occurrenceDateIso }
                    .thenByDescending { it.kind == AgendaOccurrenceKind.EVENT && it.isAllDay }
                    .thenByDescending { it.isAllDay }
                    .thenBy { it.sortEpochMillis }
                    .thenBy { it.title }
            )
    }

    /**
     * Sugere categoria financeira por nome igual ao da categoria da tarefa (se existir),
     * caso contrário retorna null (vazia).
     */
    fun suggestExpenseCategoryId(
        taskCategoryId: String?,
        categories: List<CategoryEntity>
    ): String? {
        if (taskCategoryId.isNullOrBlank()) return null
        val taskCategory = categories.firstOrNull { it.id == taskCategoryId } ?: return null
        val targetName = taskCategory.name.trim()
        if (targetName.isEmpty()) return null

        val financeMatch = categories.firstOrNull {
            it.scope == CategoryScope.FINANCE && it.name.trim().equals(targetName, ignoreCase = true)
        }
        if (financeMatch != null) return financeMatch.id

        if (taskCategory.scope == CategoryScope.FINANCE) return taskCategory.id
        return null
    }

    /**
     * Retorna apenas categorias compatíveis com o tipo selecionado na Agenda.
     * Quando "Tudo" (kind == null) ou Projetos estão ativos, não mistura categorias de módulos distintos.
     */
    fun contextualCategoriesForKind(
        categories: List<CategoryEntity>,
        kind: AgendaOccurrenceKind?
    ): List<CategoryEntity> {
        val targetScope = when (kind) {
            null -> return emptyList()
            AgendaOccurrenceKind.TASK -> CategoryScope.TASK
            AgendaOccurrenceKind.APPOINTMENT -> CategoryScope.APPOINTMENT
            AgendaOccurrenceKind.EVENT -> CategoryScope.EVENT
            AgendaOccurrenceKind.FINANCE -> CategoryScope.FINANCE
            AgendaOccurrenceKind.PROJECT -> return emptyList()
        }
        return categories
            .filter { it.scope == targetScope || it.scope == CategoryScope.GENERAL }
            .sortedBy { it.name.lowercase() }
    }
}
