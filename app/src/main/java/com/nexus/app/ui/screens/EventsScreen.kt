package com.nexus.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ConfirmationNumber
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Directions
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.nexus.app.core.DateTimeUtils
import com.nexus.app.data.db.entity.CategoryEntity
import com.nexus.app.data.db.entity.CreditCardEntity
import com.nexus.app.data.db.entity.EventEntity
import com.nexus.app.data.db.entity.ReminderEntity
import com.nexus.app.data.db.entity.ScheduleChecklistItemEntity
import com.nexus.app.data.db.entity.TaskRecurrenceEntity
import com.nexus.app.domain.model.EventLifecycleStatus
import com.nexus.app.domain.model.EventWithDetails
import com.nexus.app.domain.model.PaymentMethod
import com.nexus.app.domain.model.RecurringEditScope
import com.nexus.app.domain.model.TaskRecurrenceDraft
import com.nexus.app.domain.model.TaskRecurrenceFrequency
import com.nexus.app.domain.model.TicketStatus
import com.nexus.app.domain.rules.BackAction
import com.nexus.app.domain.rules.BackNavigationState
import com.nexus.app.domain.rules.BackSubRouteKind
import com.nexus.app.domain.rules.BackTabKind
import com.nexus.app.domain.rules.RecurrenceEngine
import com.nexus.app.domain.rules.ReminderRules
import com.nexus.app.domain.rules.resolveBackAction
import com.nexus.app.ui.components.ConfirmDangerDialog
import com.nexus.app.ui.components.ConfirmDiscardChangesDialog
import com.nexus.app.ui.components.NexusCard
import com.nexus.app.ui.components.NexusDatePickerDialog
import com.nexus.app.ui.components.NexusEmptyState
import com.nexus.app.ui.components.NexusTimePickerDialog
import com.nexus.app.ui.components.ReminderLeadTimeSelector
import com.nexus.app.ui.theme.LocalNexusPalette
import java.time.LocalDate
import java.time.LocalTime

@Composable
fun EventsScreen(
    events: List<EventWithDetails>,
    virtualRecurringEvents: List<EventWithDetails> = emptyList(),
    onOpenEvent: (String) -> Unit,
    onCreateEvent: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalNexusPalette.current
    var filterMode by remember { mutableStateOf("UPCOMING") } // UPCOMING, ALL, PENDING_TICKET
    val todayStart = DateTimeUtils.localDateToStartOfDayEpoch(DateTimeUtils.todayLocalDate())

    val filtered = remember(events, virtualRecurringEvents, filterMode, todayStart) {
        val combined = (events + virtualRecurringEvents)
            .distinctBy { it.event.id }
            .sortedBy { it.event.startAt }
        combined.filter { item ->
            val ev = item.event
            val isVirtual = RecurrenceEngine.parseVirtualOccurrenceId(ev.id) != null
            when (filterMode) {
                "UPCOMING" -> ev.endAt >= todayStart && ev.eventStatus != EventLifecycleStatus.CANCELLED
                "PENDING_TICKET" -> !isVirtual &&
                    !ev.ticketStatus.isPaidOrBought &&
                    ev.ticketStatus != TicketStatus.FREE &&
                    ev.ticketStatus != TicketStatus.CANCELLED
                else -> true
            }
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWide = maxWidth >= 720.dp

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Eventos",
                        style = MaterialTheme.typography.headlineMedium,
                        color = palette.textPrimary
                    )
                    OutlinedButton(onClick = onCreateEvent, shape = RoundedCornerShape(10.dp)) {
                        Text("Novo evento", color = palette.textPrimary)
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = filterMode == "UPCOMING",
                        onClick = { filterMode = "UPCOMING" },
                        label = { Text("Próximos") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = palette.surfaceElevated,
                            selectedLabelColor = palette.highlight
                        )
                    )
                    FilterChip(
                        selected = filterMode == "PENDING_TICKET",
                        onClick = { filterMode = "PENDING_TICKET" },
                        label = { Text("Ingresso pendente") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = palette.surfaceElevated,
                            selectedLabelColor = palette.highlight
                        )
                    )
                    FilterChip(
                        selected = filterMode == "ALL",
                        onClick = { filterMode = "ALL" },
                        label = { Text("Todos") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = palette.surfaceElevated,
                            selectedLabelColor = palette.highlight
                        )
                    )
                }
            }

            if (filtered.isEmpty()) {
                item {
                    NexusEmptyState(
                        icon = Icons.Outlined.Event,
                        title = "Nenhum evento encontrado",
                        subtitle = "Cadastre congressos, palestras, workshops ou encontros para acompanhar ingressos, custos e agenda.",
                        actionLabel = "Novo evento",
                        onAction = onCreateEvent
                    )
                }
            } else if (!isWide) {
                items(filtered, key = { it.event.id }) { ew ->
                    EventSummaryCard(
                        item = ew,
                        onClick = { onOpenEvent(ew.event.id) }
                    )
                }
            } else {
                items(
                    items = filtered.chunked(2),
                    key = { row -> "ev_row_${row.first().event.id}" }
                ) { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            EventSummaryCard(
                                item = pair[0],
                                onClick = { onOpenEvent(pair[0].event.id) }
                            )
                        }
                        if (pair.size > 1) {
                            Box(modifier = Modifier.weight(1f)) {
                                EventSummaryCard(
                                    item = pair[1],
                                    onClick = { onOpenEvent(pair[1].event.id) }
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EventSummaryCard(
    item: EventWithDetails,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalNexusPalette.current
    val ev = item.event
    val dateLabel = if (ev.isAllDay) {
        "${DateTimeUtils.formatDate(ev.startAt)} · Dia inteiro"
    } else {
        "${DateTimeUtils.formatDate(ev.startAt)} · ${DateTimeUtils.formatTime(ev.startAt)}–${DateTimeUtils.formatTime(ev.endAt)}"
    }
    val placeText = listOf(ev.locationName, ev.city).filter { it.isNotBlank() }.joinToString(", ")

    NexusCard(
        modifier = modifier.fillMaxWidth(),
        elevated = true,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = ev.name,
                style = MaterialTheme.typography.titleMedium,
                color = palette.textPrimary,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = ev.ticketStatus.label,
                style = MaterialTheme.typography.labelMedium,
                color = when (ev.ticketStatus) {
                    TicketStatus.PAID, TicketStatus.BOUGHT, TicketStatus.FREE -> palette.positive
                    TicketStatus.PENDING, TicketStatus.PARTIALLY_PAID, TicketStatus.NOT_BOUGHT -> palette.warning
                    TicketStatus.CANCELLED -> palette.danger
                }
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        val meta = buildList {
            add(dateLabel)
            if (placeText.isNotBlank()) add(placeText)
            item.category?.let { add(it.name) }
        }.joinToString(" · ")
        if (ev.recurrenceId != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Repeat,
                    contentDescription = "Evento recorrente",
                    tint = palette.accentNeon,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = "Recorrente",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.accentNeon
                )
                Text(
                    text = "· $meta",
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.textSecondary
                )
            }
        } else {
            Text(text = meta, style = MaterialTheme.typography.labelMedium, color = palette.textSecondary)
        }

        if (item.estimatedTotalCostCents > 0L || item.linkedTasks.isNotEmpty() || item.checklistTotalCount > 0) {
            Spacer(modifier = Modifier.height(6.dp))
            val extra = buildList {
                if (item.estimatedTotalCostCents > 0L) {
                    add("Custo total: ${DateTimeUtils.formatBrl(item.estimatedTotalCostCents)}")
                    add("Pago: ${DateTimeUtils.formatBrl(item.paidTotalCostCents)}")
                }
                if (item.linkedTasks.isNotEmpty()) {
                    add("${item.linkedTasks.size} tarefa(s)")
                }
                if (item.checklistTotalCount > 0) {
                    add("Checklist: ${item.checklistDoneCount}/${item.checklistTotalCount}")
                }
            }.joinToString(" · ")
            Text(text = extra, style = MaterialTheme.typography.labelMedium, color = palette.textSecondary)
        }
    }
}

private data class EventEditorInitialState(
    val startDate: LocalDate,
    val startTime: LocalTime,
    val endDate: LocalDate,
    val endTime: LocalTime,
    val ticketPrice: String,
    val ticketQty: String,
    val ticketFees: String,
    val ticketDueDate: LocalDate?,
    val installments: String,
    val reminderMinutes: Int?,
    val name: String,
    val description: String,
    val categoryId: String?,
    val isAllDay: Boolean,
    val officialLink: String,
    val ticketLink: String,
    val locationName: String,
    val address: String,
    val city: String,
    val state: String,
    val notes: String,
    val eventStatus: EventLifecycleStatus,
    val ticketStatus: TicketStatus,
    val paymentMethod: PaymentMethod,
    val creditCardId: String?,
    val recFrequency: TaskRecurrenceFrequency?,
    val customIntervalText: String,
    val weekdaysMask: Int,
    val recStartDate: LocalDate,
    val recEndDate: LocalDate?,
    val maxOccurrencesText: String,
    val recEndMode: String,
    val hasActiveRecurrence: Boolean
)

private fun buildEventEditorInitialState(
    ev: EventEntity?,
    activeRecurrence: TaskRecurrenceEntity?,
    existingReminder: ReminderEntity?
): EventEditorInitialState {
    val today = DateTimeUtils.todayLocalDate()
    val initialStartDate = ev?.let { DateTimeUtils.toLocalDate(it.startAt) } ?: today
    val initialRecStartDate = RecurrenceEngine.parseLocalDateOrNull(activeRecurrence?.startDateIso)
        ?: initialStartDate
    val initialRecEndDate = RecurrenceEngine.parseLocalDateOrNull(activeRecurrence?.endDateIso)
    val initialRecEndMode = when {
        initialRecEndDate != null -> "DATE"
        activeRecurrence?.maxOccurrences != null && activeRecurrence.maxOccurrences > 0 -> "COUNT"
        else -> "NONE"
    }
    return EventEditorInitialState(
        startDate = initialStartDate,
        startTime = ev?.let { DateTimeUtils.epochToLocalTime(it.startAt) } ?: LocalTime.of(19, 0),
        endDate = ev?.let { DateTimeUtils.toLocalDate(it.endAt) } ?: today,
        endTime = ev?.let { DateTimeUtils.epochToLocalTime(it.endAt) } ?: LocalTime.of(21, 0),
        ticketPrice = ev?.let { DateTimeUtils.centsToInputString(it.ticketPriceCents) } ?: "",
        ticketQty = (ev?.ticketQuantity ?: 1).toString(),
        ticketFees = ev?.let { DateTimeUtils.centsToInputString(it.ticketFeesCents) } ?: "",
        ticketDueDate = ev?.ticketDueDate?.let { DateTimeUtils.toLocalDate(it) },
        installments = (ev?.installmentsCount ?: 1).toString(),
        reminderMinutes = ReminderRules.resolveEditorReminderMinutes(existingReminder),
        name = ev?.name ?: "",
        description = ev?.description ?: "",
        categoryId = ev?.categoryId,
        isAllDay = ev?.isAllDay ?: false,
        officialLink = ev?.officialLink ?: "",
        ticketLink = ev?.ticketLink ?: "",
        locationName = ev?.locationName ?: "",
        address = ev?.address ?: "",
        city = ev?.city ?: "",
        state = ev?.state ?: "",
        notes = ev?.notes ?: "",
        eventStatus = ev?.eventStatus ?: EventLifecycleStatus.PLANNED,
        ticketStatus = ev?.ticketStatus ?: TicketStatus.FREE,
        paymentMethod = ev?.paymentMethod ?: PaymentMethod.PIX,
        creditCardId = ev?.creditCardId,
        recFrequency = activeRecurrence?.frequency,
        customIntervalText = (activeRecurrence?.customIntervalDays?.coerceAtLeast(1) ?: 2).toString(),
        weekdaysMask = activeRecurrence?.weekdaysMask ?: 0,
        recStartDate = initialRecStartDate,
        recEndDate = initialRecEndDate,
        maxOccurrencesText = activeRecurrence?.maxOccurrences?.takeIf { it > 0 }?.toString() ?: "",
        recEndMode = initialRecEndMode,
        hasActiveRecurrence = activeRecurrence != null
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventEditorSheet(
    existingEvent: EventWithDetails?,
    existingRecurrence: TaskRecurrenceEntity? = null,
    existingChecklistItems: List<ScheduleChecklistItemEntity> = emptyList(),
    categories: List<CategoryEntity>,
    creditCards: List<CreditCardEntity>,
    existingReminder: ReminderEntity? = null,
    defaultReminderMinutes: Int = 60,
    countLinkedAppointments: suspend (String) -> Int = { 0 },
    onDismiss: () -> Unit,
    onSave: (
        id: String?,
        name: String,
        description: String,
        categoryId: String?,
        startAt: Long,
        endAt: Long,
        isAllDay: Boolean,
        officialLink: String,
        ticketLink: String,
        locationName: String,
        address: String,
        city: String,
        state: String,
        notes: String,
        eventStatus: EventLifecycleStatus,
        ticketStatus: TicketStatus,
        ticketPriceCents: Long,
        ticketQuantity: Int,
        ticketFeesCents: Long,
        ticketPurchaseDate: Long?,
        ticketDueDate: Long?,
        paymentMethod: PaymentMethod,
        creditCardId: String?,
        installmentsCount: Int,
        reminderMinutesBefore: Int?,
        recurrenceDraft: TaskRecurrenceDraft?,
        recurringEditScope: RecurringEditScope,
        checklistDrafts: List<Triple<String?, String, Boolean>>
    ) -> Unit,
    onDelete: (String, RecurringEditScope) -> Unit,
    onAddLinkedTask: (String) -> Unit,
    onOpenTask: (String) -> Unit,
    onAddAdditionalExpense: (String) -> Unit,
    onOpenTransaction: (String) -> Unit,
    onError: (String, Throwable) -> Unit,
    onDirtyStateChange: (Boolean) -> Unit = {}
) {
    val palette = LocalNexusPalette.current
    val context = LocalContext.current
    val ev = existingEvent?.event
    val evId: String? = ev?.id
    val activeRecurrence = existingRecurrence?.takeIf { it.isActive }
    val activeRecurrenceId: String? = activeRecurrence?.id
    val activeRecurrenceUpdatedAt: Long? = activeRecurrence?.updatedAt
    val reminderId: String? = existingReminder?.id
    val reminderMinutesKey: Int? = existingReminder?.minutesBefore
    val reminderStatusKey: Any? = existingReminder?.status

    val init = remember(
        evId,
        activeRecurrenceId,
        activeRecurrenceUpdatedAt,
        reminderId,
        reminderMinutesKey,
        reminderStatusKey
    ) {
        buildEventEditorInitialState(ev, activeRecurrence, existingReminder)
    }

    var name by remember(evId) { mutableStateOf(init.name) }
    var description by remember(evId) { mutableStateOf(init.description) }
    var categoryId by remember(evId) { mutableStateOf(init.categoryId) }
    var isAllDay by remember(evId) { mutableStateOf(init.isAllDay) }
    var startDate by remember(evId) { mutableStateOf(init.startDate) }
    var startTime by remember(evId) { mutableStateOf(init.startTime) }
    var endDate by remember(evId) { mutableStateOf(init.endDate) }
    var endTime by remember(evId) { mutableStateOf(init.endTime) }

    var officialLink by remember(evId) { mutableStateOf(init.officialLink) }
    var ticketLink by remember(evId) { mutableStateOf(init.ticketLink) }
    var locationName by remember(evId) { mutableStateOf(init.locationName) }
    var address by remember(evId) { mutableStateOf(init.address) }
    var city by remember(evId) { mutableStateOf(init.city) }
    var state by remember(evId) { mutableStateOf(init.state) }
    var notes by remember(evId) { mutableStateOf(init.notes) }

    var eventStatus by remember(evId) { mutableStateOf(init.eventStatus) }
    var ticketStatus by remember(evId) { mutableStateOf(init.ticketStatus) }
    var ticketPriceInput by remember(evId) { mutableStateOf(init.ticketPrice) }
    var ticketQtyInput by remember(evId) { mutableStateOf(init.ticketQty) }
    var ticketFeesInput by remember(evId) { mutableStateOf(init.ticketFees) }
    var ticketDueDate by remember(evId) { mutableStateOf(init.ticketDueDate) }
    var paymentMethod by remember(evId) { mutableStateOf(init.paymentMethod) }
    var creditCardId by remember(evId) { mutableStateOf(init.creditCardId) }
    var installmentsInput by remember(evId) { mutableStateOf(init.installments) }

    var recurrenceFrequency by remember(evId, activeRecurrenceId, activeRecurrenceUpdatedAt) {
        mutableStateOf(init.recFrequency)
    }
    var customIntervalDaysText by remember(evId, activeRecurrenceId, activeRecurrenceUpdatedAt) {
        mutableStateOf(init.customIntervalText)
    }
    var weekdaysMask by remember(evId, activeRecurrenceId, activeRecurrenceUpdatedAt) {
        mutableStateOf(init.weekdaysMask)
    }
    var recurrenceStartDate by remember(evId, activeRecurrenceId, activeRecurrenceUpdatedAt) {
        mutableStateOf(init.recStartDate)
    }
    var recurrenceStartCustomized by remember(evId, activeRecurrenceId, activeRecurrenceUpdatedAt) {
        mutableStateOf(init.hasActiveRecurrence)
    }
    var recurrenceEndMode by remember(evId, activeRecurrenceId, activeRecurrenceUpdatedAt) {
        mutableStateOf(init.recEndMode)
    }
    var recurrenceEndDate by remember(evId, activeRecurrenceId, activeRecurrenceUpdatedAt) {
        mutableStateOf(init.recEndDate)
    }
    var maxOccurrencesText by remember(evId, activeRecurrenceId, activeRecurrenceUpdatedAt) {
        mutableStateOf(init.maxOccurrencesText)
    }
    var showMoreDetails by remember(evId, activeRecurrenceId) {
        mutableStateOf(init.hasActiveRecurrence)
    }
    var showRecurrenceFreqDropdown by remember { mutableStateOf(false) }
    var showRecStartDatePicker by remember { mutableStateOf(false) }
    var showRecEndDatePicker by remember { mutableStateOf(false) }
    var pendingScopeSaveAction by remember { mutableStateOf<((RecurringEditScope) -> Unit)?>(null) }
    var pendingRuleChangeConfirmAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    var pickTarget by remember { mutableStateOf<String?>(null) }
    var showCategoryMenu by remember { mutableStateOf(false) }
    var showLifecycleMenu by remember { mutableStateOf(false) }
    var showTicketStatusMenu by remember { mutableStateOf(false) }
    var showPaymentMenu by remember { mutableStateOf(false) }
    var showCardMenu by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showDiscardConfirm by remember { mutableStateOf(false) }
    var nameError by remember { mutableStateOf(false) }
    var reminderMinutesBefore by remember(evId, reminderId) {
        mutableStateOf(init.reminderMinutes)
    }
    var reminderModified by remember(evId, reminderId) { mutableStateOf(false) }
    var linkedAppointmentsCount by remember(evId) { mutableStateOf(0) }

    var checklistInitialized by remember(evId) { mutableStateOf(false) }
    var initialChecklistSnapshot by remember(evId) {
        mutableStateOf<List<Triple<String?, String, Boolean>>>(emptyList())
    }
    val draftChecklist = remember(evId) {
        mutableStateListOf<Triple<String?, String, Boolean>>()
    }
    var newChecklistItemText by remember(evId) { mutableStateOf("") }

    LaunchedEffect(evId, existingChecklistItems) {
        val mapped = existingChecklistItems
            .sortedBy { it.sortOrder }
            .map { Triple<String?, String, Boolean>(it.id, it.title, it.isCompleted) }
        val userEditedChecklist = draftChecklist.toList() != initialChecklistSnapshot ||
            newChecklistItemText.isNotBlank()
        if (!checklistInitialized || (!userEditedChecklist && mapped != initialChecklistSnapshot)) {
            initialChecklistSnapshot = mapped
            draftChecklist.clear()
            draftChecklist.addAll(mapped)
            checklistInitialized = true
        }
    }

    LaunchedEffect(evId, showDeleteConfirm) {
        if (ev != null && showDeleteConfirm) {
            linkedAppointmentsCount = countLinkedAppointments(ev.id)
        }
    }

    val fullAddressText = listOf(locationName, address, city, state).filter { it.isNotBlank() }.joinToString(", ")

    val recurrenceRuleChanged = recurrenceFrequency != init.recFrequency ||
        (recurrenceFrequency == TaskRecurrenceFrequency.CUSTOM_DAYS &&
            customIntervalDaysText.trim() != init.customIntervalText) ||
        (recurrenceFrequency == TaskRecurrenceFrequency.SPECIFIC_WEEKDAYS &&
            weekdaysMask != init.weekdaysMask) ||
        (recurrenceFrequency != null && (
            recurrenceStartDate != init.recStartDate ||
                recurrenceEndMode != init.recEndMode ||
                (recurrenceEndMode == "DATE" && recurrenceEndDate != init.recEndDate) ||
                (recurrenceEndMode == "COUNT" && maxOccurrencesText.trim() != init.maxOccurrencesText)
            ))

    val initChecklistTitles = initialChecklistSnapshot.map { it.second.trim() }
    val currentChecklistTitles = draftChecklist.map { it.second.trim() } +
        listOfNotNull(newChecklistItemText.trim().takeIf { it.isNotEmpty() })
    val checklistStructureChanged = currentChecklistTitles != initChecklistTitles
    val checklistChanged = draftChecklist.toList() != initialChecklistSnapshot ||
        newChecklistItemText.isNotBlank()

    val templateFieldsChanged = name != init.name ||
        description != init.description ||
        categoryId != init.categoryId ||
        isAllDay != init.isAllDay ||
        startDate != init.startDate ||
        endDate != init.endDate ||
        (!isAllDay && (startTime != init.startTime || endTime != init.endTime)) ||
        officialLink != init.officialLink ||
        ticketLink != init.ticketLink ||
        locationName != init.locationName ||
        address != init.address ||
        city != init.city ||
        state != init.state ||
        notes != init.notes ||
        eventStatus != init.eventStatus ||
        ticketStatus != init.ticketStatus ||
        ticketPriceInput != init.ticketPrice ||
        ticketQtyInput != init.ticketQty ||
        ticketFeesInput != init.ticketFees ||
        ticketDueDate != init.ticketDueDate ||
        paymentMethod != init.paymentMethod ||
        creditCardId != init.creditCardId ||
        installmentsInput != init.installments ||
        reminderModified ||
        checklistStructureChanged

    val hasUnsavedChanges = templateFieldsChanged || checklistChanged || recurrenceRuleChanged

    fun dispatchSaveWithScopeHandling(executeSave: (RecurringEditScope) -> Unit) {
        if (activeRecurrence == null) {
            executeSave(RecurringEditScope.THIS_OCCURRENCE_ONLY)
        } else if (recurrenceRuleChanged) {
            pendingRuleChangeConfirmAction = { executeSave(RecurringEditScope.ENTIRE_SERIES) }
        } else if (templateFieldsChanged) {
            pendingScopeSaveAction = executeSave
        } else {
            executeSave(RecurringEditScope.THIS_OCCURRENCE_ONLY)
        }
    }

    val currentIsDirty by rememberUpdatedState(hasUnsavedChanges)
    LaunchedEffect(hasUnsavedChanges) { onDirtyStateChange(hasUnsavedChanges) }
    val requestDismiss: () -> Unit = {
        val action = resolveBackAction(
            BackNavigationState(
                hasOpenEditor = true,
                editorHasUnsavedChanges = currentIsDirty,
                subRouteKind = BackSubRouteKind.NONE,
                currentTab = BackTabKind.HOME
            )
        )
        if (action == BackAction.CONFIRM_DISCARD_EDITOR) {
            showDiscardConfirm = true
        } else {
            onDismiss()
        }
    }

    ModalBottomSheet(
        onDismissRequest = requestDismiss,
        containerColor = palette.surface,
        sheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
            confirmValueChange = { target ->
                if (target == SheetValue.Hidden && currentIsDirty) {
                    showDiscardConfirm = true
                    false
                } else {
                    true
                }
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (ev == null) "Novo evento" else "Detalhes do evento",
                    style = MaterialTheme.typography.titleLarge,
                    color = palette.textPrimary
                )
                if (ev != null) {
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(Icons.Outlined.DeleteOutline, contentDescription = "Excluir evento", tint = palette.danger)
                    }
                }
            }

            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    if (it.isNotBlank()) nameError = false
                },
                label = { Text("Nome do evento *") },
                isError = nameError,
                supportingText = if (nameError) {
                    { Text("Informe o nome do evento.") }
                } else null,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Descrição") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    val catName = categories.firstOrNull { it.id == categoryId }?.name ?: "Sem categoria"
                    OutlinedButton(
                        onClick = { showCategoryMenu = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(catName, color = palette.textPrimary, maxLines = 1)
                    }
                    DropdownMenu(expanded = showCategoryMenu, onDismissRequest = { showCategoryMenu = false }) {
                        DropdownMenuItem(text = { Text("Sem categoria") }, onClick = {
                            categoryId = null; showCategoryMenu = false
                        })
                        categories.forEach { c ->
                            DropdownMenuItem(text = { Text(c.name) }, onClick = {
                                categoryId = c.id; showCategoryMenu = false
                            })
                        }
                    }
                }
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedButton(
                        onClick = { showLifecycleMenu = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(eventStatus.label, color = palette.textPrimary, maxLines = 1)
                    }
                    DropdownMenu(expanded = showLifecycleMenu, onDismissRequest = { showLifecycleMenu = false }) {
                        EventLifecycleStatus.entries.forEach { st ->
                            DropdownMenuItem(text = { Text(st.label) }, onClick = {
                                eventStatus = st; showLifecycleMenu = false
                            })
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Dia inteiro", style = MaterialTheme.typography.bodyLarge, color = palette.textPrimary)
                Switch(checked = isAllDay, onCheckedChange = { isAllDay = it })
            }

            NexusCard(modifier = Modifier.fillMaxWidth(), elevated = true) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Início", style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { pickTarget = "startDate" }) {
                            Text(DateTimeUtils.formatDate(startDate), color = palette.textPrimary)
                        }
                        if (!isAllDay) {
                            OutlinedButton(onClick = { pickTarget = "startTime" }) {
                                Text(startTime.toString(), color = palette.textPrimary)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Término", style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { pickTarget = "endDate" }) {
                            Text(DateTimeUtils.formatDate(endDate), color = palette.textPrimary)
                        }
                        if (!isAllDay) {
                            OutlinedButton(onClick = { pickTarget = "endTime" }) {
                                Text(endTime.toString(), color = palette.textPrimary)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                ReminderLeadTimeSelector(
                    selectedMinutes = reminderMinutesBefore,
                    onSelectMinutes = {
                        reminderMinutesBefore = it
                        reminderModified = true
                    }
                )
            }

            TaskRecurrenceSectionCard(
                showMoreDetails = showMoreDetails,
                onToggleMoreDetails = { showMoreDetails = !showMoreDetails },
                recurrenceFrequency = recurrenceFrequency,
                showRecurrenceFreqDropdown = showRecurrenceFreqDropdown,
                onToggleRecurrenceFreqDropdown = { showRecurrenceFreqDropdown = it },
                onSelectRecurrenceFrequency = { freq ->
                    recurrenceFrequency = freq
                    if (freq != null) {
                        if (!recurrenceStartCustomized) {
                            recurrenceStartDate = startDate
                        }
                        if (freq == TaskRecurrenceFrequency.SPECIFIC_WEEKDAYS && weekdaysMask == 0) {
                            weekdaysMask = RecurrenceEngine.weekdayBit(startDate.dayOfWeek)
                        }
                    }
                },
                customIntervalDaysText = customIntervalDaysText,
                onCustomIntervalChange = { customIntervalDaysText = it },
                weekdaysMask = weekdaysMask,
                onWeekdaysMaskChange = { weekdaysMask = it },
                recurrenceStartDate = recurrenceStartDate,
                onPickRecurrenceStartDate = { showRecStartDatePicker = true },
                recurrenceEndMode = recurrenceEndMode,
                onSelectRecurrenceEndMode = { modeKey ->
                    recurrenceEndMode = modeKey
                    if (modeKey == "DATE" && recurrenceEndDate == null) {
                        recurrenceEndDate = recurrenceStartDate.plusMonths(1)
                    } else if (modeKey == "COUNT" && maxOccurrencesText.isBlank()) {
                        maxOccurrencesText = "5"
                    }
                },
                recurrenceEndDate = recurrenceEndDate,
                onPickRecurrenceEndDate = { showRecEndDatePicker = true },
                maxOccurrencesText = maxOccurrencesText,
                onMaxOccurrencesChange = { maxOccurrencesText = it },
                infoNotice = "Ocorrências futuras aparecem na Agenda sem gerar cobranças financeiras até serem abertas e confirmadas."
            )

            ScheduleChecklistSectionCard(
                items = draftChecklist,
                newItemText = newChecklistItemText,
                onNewItemTextChange = { newChecklistItemText = it },
                notice = if (activeRecurrence != null) {
                    "Marcações e edições neste checklist ficam isoladas apenas nesta ocorrência."
                } else {
                    null
                }
            )

            NexusCard(modifier = Modifier.fillMaxWidth(), elevated = true) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = palette.textSecondary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Localização e Links", style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = locationName,
                    onValueChange = { locationName = it },
                    label = { Text("Nome do local") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Endereço") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = city,
                        onValueChange = { city = it },
                        label = { Text("Cidade") },
                        modifier = Modifier.weight(2f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = state,
                        onValueChange = { state = it },
                        label = { Text("UF") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                if (fullAddressText.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Prévia de mapa online indisponível offline. Endereço salvo localmente.",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = {
                            runCatching {
                                val uri = Uri.parse("geo:0,0?q=${Uri.encode(fullAddressText)}")
                                context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                            }.onFailure { onError("Abrir Maps", it) }
                        }) {
                            Icon(Icons.Outlined.OpenInNew, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Google Maps")
                        }
                        OutlinedButton(onClick = {
                            runCatching {
                                val uri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${Uri.encode(fullAddressText)}")
                                context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                            }.onFailure { onError("Abrir Rota", it) }
                        }) {
                            Icon(Icons.Outlined.Directions, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Rota")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = officialLink,
                    onValueChange = { officialLink = it },
                    label = { Text("Link oficial") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = ticketLink,
                    onValueChange = { ticketLink = it },
                    label = { Text("Link do ingresso") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            NexusCard(modifier = Modifier.fillMaxWidth(), elevated = true) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.ConfirmationNumber, contentDescription = null, tint = palette.textSecondary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ingresso e Financeiro", style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Box {
                    OutlinedButton(
                        onClick = { showTicketStatusMenu = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Situação do ingresso: ${ticketStatus.label}", color = palette.textPrimary)
                    }
                    DropdownMenu(expanded = showTicketStatusMenu, onDismissRequest = { showTicketStatusMenu = false }) {
                        TicketStatus.entries.forEach { ts ->
                            DropdownMenuItem(text = { Text(ts.label) }, onClick = {
                                ticketStatus = ts; showTicketStatusMenu = false
                            })
                        }
                    }
                }

                if (ticketStatus == TicketStatus.FREE) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Evento gratuito não gera obrigação financeira nem lembrete de pagamento.",
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.positive
                    )
                } else if (ticketStatus != TicketStatus.CANCELLED) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = ticketPriceInput,
                            onValueChange = { ticketPriceInput = it },
                            label = { Text("Preço (R$)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1.2f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = ticketQtyInput,
                            onValueChange = { ticketQtyInput = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Qtd") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(0.7f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = ticketFeesInput,
                            onValueChange = { ticketFeesInput = it },
                            label = { Text("Taxas") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedButton(
                                onClick = { showPaymentMenu = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(paymentMethod.label, color = palette.textPrimary, maxLines = 1)
                            }
                            DropdownMenu(expanded = showPaymentMenu, onDismissRequest = { showPaymentMenu = false }) {
                                PaymentMethod.entries.forEach { pm ->
                                    DropdownMenuItem(text = { Text(pm.label) }, onClick = {
                                        paymentMethod = pm; showPaymentMenu = false
                                    })
                                }
                            }
                        }
                        OutlinedButton(
                            onClick = { pickTarget = "ticketDueDate" },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = ticketDueDate?.let { "Venc: ${DateTimeUtils.formatDate(it)}" } ?: "Limite pgto",
                                color = palette.textPrimary,
                                maxLines = 1
                            )
                        }
                    }

                    if (paymentMethod == PaymentMethod.CREDIT_CARD) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(modifier = Modifier.weight(1.4f)) {
                                val cardName = creditCards.firstOrNull { it.id == creditCardId }?.name ?: "Selecionar cartão"
                                OutlinedButton(
                                    onClick = { showCardMenu = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(cardName, color = palette.textPrimary, maxLines = 1)
                                }
                                DropdownMenu(expanded = showCardMenu, onDismissRequest = { showCardMenu = false }) {
                                    DropdownMenuItem(text = { Text("Nenhum") }, onClick = {
                                        creditCardId = null; showCardMenu = false
                                    })
                                    creditCards.forEach { card ->
                                        DropdownMenuItem(text = { Text(card.name) }, onClick = {
                                            creditCardId = card.id; showCardMenu = false
                                        })
                                    }
                                }
                            }
                            OutlinedTextField(
                                value = installmentsInput,
                                onValueChange = { installmentsInput = it.filter { c -> c.isDigit() } },
                                label = { Text("Parcelas") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(0.8f),
                                singleLine = true
                            )
                        }
                    }
                }
            }

            if (existingEvent != null) {
                NexusCard(modifier = Modifier.fillMaxWidth(), elevated = true) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Custos do evento", style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
                            Text(
                                text = "Estimado: ${DateTimeUtils.formatBrl(existingEvent.estimatedTotalCostCents)} · Pago: ${DateTimeUtils.formatBrl(existingEvent.paidTotalCostCents)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = palette.textSecondary
                            )
                        }
                        OutlinedButton(onClick = { onAddAdditionalExpense(existingEvent.event.id) }) {
                            Icon(Icons.Outlined.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Custo")
                        }
                    }
                    if (existingEvent.linkedTransactions.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        existingEvent.linkedTransactions.forEach { txItem ->
                            val tx = txItem.transaction
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(onClick = { onOpenTransaction(tx.id) }) {
                                    Text(
                                        text = "${tx.eventCostType?.label ?: "Despesa"}: ${tx.description}",
                                        color = palette.textPrimary
                                    )
                                }
                                Text(
                                    text = "${DateTimeUtils.formatBrl(tx.amountCents)} · ${tx.status.label}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = palette.textSecondary
                                )
                            }
                        }
                    }
                }

                NexusCard(modifier = Modifier.fillMaxWidth(), elevated = true) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Tarefas do evento", style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
                        OutlinedButton(onClick = { onAddLinkedTask(existingEvent.event.id) }) {
                            Icon(Icons.Outlined.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Tarefa")
                        }
                    }
                    if (existingEvent.linkedTasks.isEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Nenhuma tarefa vinculada a este evento.", style = MaterialTheme.typography.bodyMedium, color = palette.textSecondary)
                    } else {
                        existingEvent.linkedTasks.forEach { tw ->
                            TextButton(onClick = { onOpenTask(tw.task.id) }) {
                                Text("${tw.task.title} (${tw.task.status.label})", color = palette.textPrimary)
                            }
                        }
                    }
                }
            }

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Observações") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4
            )

            Button(
                onClick = {
                    if (name.isBlank()) {
                        nameError = true
                        return@Button
                    }
                    val startEpoch = if (isAllDay) {
                        DateTimeUtils.startOfDayEpoch(startDate)
                    } else {
                        DateTimeUtils.localDateTimeToEpoch(startDate, startTime)
                    }
                    val normalizedEndDate = if (endDate < startDate) startDate else endDate
                    val endEpoch = if (isAllDay) {
                        DateTimeUtils.localDateToEndOfDayEpoch(normalizedEndDate)
                    } else {
                        DateTimeUtils.localDateTimeToEpoch(normalizedEndDate, endTime)
                    }
                    val priceCents = DateTimeUtils.parseBrlToCents(ticketPriceInput) ?: 0L
                    val feesCents = DateTimeUtils.parseBrlToCents(ticketFeesInput) ?: 0L
                    val qty = ticketQtyInput.toIntOrNull()?.coerceAtLeast(1) ?: 1
                    val instCount = installmentsInput.toIntOrNull()?.coerceAtLeast(1) ?: 1
                    val dueEpoch = ticketDueDate?.let { DateTimeUtils.localDateTimeToEpoch(it, LocalTime.of(12, 0)) }
                    val purchaseEpoch = if (ticketStatus.isPaidOrBought) {
                        ev?.ticketPurchaseDate ?: DateTimeUtils.nowEpochMillis()
                    } else null

                    val recDraft = buildTaskRecurrenceDraft(
                        recurrenceFrequency = recurrenceFrequency,
                        customIntervalDaysText = customIntervalDaysText,
                        weekdaysMask = weekdaysMask,
                        recurrenceStartDate = recurrenceStartDate,
                        recurrenceEndMode = recurrenceEndMode,
                        recurrenceEndDate = recurrenceEndDate,
                        maxOccurrencesText = maxOccurrencesText
                    )
                    val finalChecklistDrafts = buildFinalChecklistDrafts(
                        draftChecklist = draftChecklist,
                        pendingItemText = newChecklistItemText
                    )
                    dispatchSaveWithScopeHandling { editScope ->
                        onSave(
                            ev?.id,
                            name,
                            description,
                            categoryId,
                            startEpoch,
                            endEpoch,
                            isAllDay,
                            officialLink,
                            ticketLink,
                            locationName,
                            address,
                            city,
                            state,
                            notes,
                            eventStatus,
                            ticketStatus,
                            priceCents,
                            qty,
                            feesCents,
                            purchaseEpoch,
                            dueEpoch,
                            paymentMethod,
                            creditCardId,
                            instCount,
                            if (reminderModified) (reminderMinutesBefore ?: defaultReminderMinutes) else null,
                            recDraft,
                            editScope,
                            finalChecklistDrafts
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.highlight,
                    contentColor = palette.background
                )
            ) {
                Text("Salvar evento")
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    when (pickTarget) {
        "startDate" -> NexusDatePickerDialog(
            initialDate = startDate,
            onDateSelected = {
                startDate = it
                if (endDate < it) endDate = it
                if (!recurrenceStartCustomized) {
                    recurrenceStartDate = it
                }
            },
            onDismiss = { pickTarget = null }
        )
        "endDate" -> NexusDatePickerDialog(
            initialDate = endDate,
            onDateSelected = { endDate = it },
            onDismiss = { pickTarget = null }
        )
        "startTime" -> NexusTimePickerDialog(
            initialTime = startTime,
            onTimeSelected = { startTime = it },
            onDismiss = { pickTarget = null }
        )
        "endTime" -> NexusTimePickerDialog(
            initialTime = endTime,
            onTimeSelected = { endTime = it },
            onDismiss = { pickTarget = null }
        )
        "ticketDueDate" -> NexusDatePickerDialog(
            initialDate = ticketDueDate ?: startDate,
            onDateSelected = { ticketDueDate = it },
            onDismiss = { pickTarget = null }
        )
    }

    if (showRecStartDatePicker) {
        NexusDatePickerDialog(
            initialDate = recurrenceStartDate,
            onDateSelected = { picked ->
                recurrenceStartDate = picked
                recurrenceStartCustomized = true
            },
            onDismiss = { showRecStartDatePicker = false }
        )
    }

    if (showRecEndDatePicker) {
        NexusDatePickerDialog(
            initialDate = recurrenceEndDate ?: recurrenceStartDate.plusMonths(1),
            onDateSelected = { picked ->
                recurrenceEndDate = if (picked.isBefore(recurrenceStartDate)) recurrenceStartDate else picked
            },
            onDismiss = { showRecEndDatePicker = false }
        )
    }

    val linkedTasksCount = existingEvent?.linkedTasks?.size ?: 0
    val linkedTxCount = existingEvent?.linkedTransactions?.size ?: 0
    val singleDeleteMessage = ev?.let {
        buildString {
            append("O evento \"${it.name}\" será excluído")
            val children = buildList {
                if (linkedTasksCount > 0) add("$linkedTasksCount tarefa(s)")
                if (linkedAppointmentsCount > 0) add("$linkedAppointmentsCount compromisso(s)")
                if (linkedTxCount > 0) add("$linkedTxCount lançamento(s) extra(s)")
            }
            if (children.isNotEmpty()) {
                append(" e ${children.joinToString(", ")} vinculados serão mantidos sem vínculo.")
            } else {
                append(" e eventuais registros vinculados serão mantidos sem vínculo.")
            }
        }
    }.orEmpty()
    val recurringDeleteMessage = ev?.let {
        "\"${it.name}\" faz parte de uma série recorrente. Você pode remover apenas esta ocorrência ou encerrar toda a série (ocorrências já comparecidas ou com ingresso quitado são preservadas sem vínculo)."
    }.orEmpty()

    RecurringScopeConfirmDialogs(
        entityTitle = ev?.name,
        hasActiveRecurrence = activeRecurrence != null,
        recurrenceFrequency = recurrenceFrequency,
        deleteTitle = "Excluir evento?",
        deleteRecurringTitle = "Excluir evento recorrente?",
        deleteSingleMessage = singleDeleteMessage,
        deleteRecurringMessage = recurringDeleteMessage,
        updateRecurringTitle = "Atualizar evento recorrente",
        showDeleteConfirm = showDeleteConfirm,
        onDismissDeleteConfirm = { showDeleteConfirm = false },
        onConfirmDelete = { deleteScope ->
            showDeleteConfirm = false
            ev?.let { onDelete(it.id, deleteScope) }
        },
        pendingScopeSaveAction = pendingScopeSaveAction,
        onClearPendingScopeSave = { pendingScopeSaveAction = null },
        pendingRuleChangeConfirmAction = pendingRuleChangeConfirmAction,
        onClearPendingRuleChange = { pendingRuleChangeConfirmAction = null }
    )

    if (showDiscardConfirm) {
        ConfirmDiscardChangesDialog(
            onConfirmDiscard = {
                showDiscardConfirm = false
                onDismiss()
            },
            onContinueEditing = { showDiscardConfirm = false }
        )
    }
}
