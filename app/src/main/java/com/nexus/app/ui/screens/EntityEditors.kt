package com.nexus.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ConfirmationNumber
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.nexus.app.core.DateTimeUtils
import com.nexus.app.data.db.entity.AppointmentEntity
import com.nexus.app.data.db.entity.CategoryEntity
import com.nexus.app.data.db.entity.ReminderEntity
import com.nexus.app.data.db.entity.SubtaskEntity
import com.nexus.app.data.db.entity.TaskEntity
import com.nexus.app.data.db.entity.TaskRecurrenceEntity
import com.nexus.app.domain.model.EventWithDetails
import com.nexus.app.domain.model.PersonalGoalWithDetails
import com.nexus.app.domain.model.ProjectStatus
import com.nexus.app.domain.model.ProjectWithProgress
import com.nexus.app.domain.model.RecurringEditScope
import com.nexus.app.domain.model.TaskPriority
import com.nexus.app.domain.model.TaskRecurrenceDraft
import com.nexus.app.domain.model.TaskRecurrenceFrequency
import com.nexus.app.domain.model.TaskStatus
import com.nexus.app.domain.model.TaskWithDetails
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
import com.nexus.app.ui.components.NexusTimePickerDialog
import com.nexus.app.ui.components.ReminderLeadTimeSelector
import com.nexus.app.ui.theme.ColoredBorderRole
import com.nexus.app.ui.theme.LocalNexusPalette
import com.nexus.app.ui.theme.coloredBorder
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.TextStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddChooserSheet(
    onDismiss: () -> Unit,
    onSelectNewTask: () -> Unit,
    onSelectNewAppointment: () -> Unit,
    onSelectNewProject: () -> Unit,
    onSelectNewEvent: () -> Unit,
    onSelectNewTransaction: () -> Unit
) {
    val palette = LocalNexusPalette.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = palette.surface,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Cadastro rápido",
                style = MaterialTheme.typography.titleLarge,
                color = palette.textPrimary
            )
            Text(
                text = "Escolha o que deseja registrar no banco local:",
                style = MaterialTheme.typography.bodyMedium,
                color = palette.textSecondary
            )

            QuickAddOptionCard(
                icon = Icons.Outlined.TaskAlt,
                title = "Nova tarefa",
                subtitle = "Título obrigatório; quando, projeto, evento e subtarefas opcionais.",
                onClick = onSelectNewTask
            )
            QuickAddOptionCard(
                icon = Icons.Outlined.CalendarMonth,
                title = "Novo compromisso",
                subtitle = "Horário, local, link externo e detecção automática de conflito.",
                onClick = onSelectNewAppointment
            )
            QuickAddOptionCard(
                icon = Icons.Outlined.AccountBalanceWallet,
                title = "Novo lançamento financeiro",
                subtitle = "Receita ou despesa vinculável a cartão, projeto ou evento.",
                onClick = onSelectNewTransaction
            )
            QuickAddOptionCard(
                icon = Icons.Outlined.ConfirmationNumber,
                title = "Novo evento",
                subtitle = "Data, local, ingresso integrado ao financeiro e tarefas vinculadas.",
                onClick = onSelectNewEvent
            )
            QuickAddOptionCard(
                icon = Icons.Outlined.FolderOpen,
                title = "Novo projeto",
                subtitle = "Agrupe tarefas com progresso calculado automaticamente.",
                onClick = onSelectNewProject
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun QuickAddOptionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val palette = LocalNexusPalette.current
    NexusCard(
        modifier = Modifier.fillMaxWidth(),
        elevated = true,
        onClick = onClick
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = null, tint = palette.highlight)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = title, style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
                Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = palette.textSecondary)
            }
        }
    }
}

private data class TaskEditorInitialState(
    val title: String,
    val description: String,
    val notes: String,
    val projectId: String?,
    val categoryId: String?,
    val eventId: String?,
    val goalId: String?,
    val priority: TaskPriority,
    val status: TaskStatus,
    val dueDate: LocalDate?,
    val hasDueTime: Boolean,
    val dueTime: LocalTime,
    val subtasks: List<Triple<String?, String, Boolean>>,
    val reminderMinutes: Int?,
    val recFrequency: TaskRecurrenceFrequency?,
    val customIntervalText: String,
    val weekdaysMask: Int,
    val recStartDate: LocalDate,
    val recEndDate: LocalDate?,
    val maxOccurrencesText: String,
    val recEndMode: String,
    val hasActiveRecurrence: Boolean
)

private fun buildTaskEditorInitialState(
    task: TaskEntity?,
    activeRecurrence: TaskRecurrenceEntity?,
    existingTask: TaskWithDetails?,
    initialEventId: String?,
    existingReminder: ReminderEntity?
): TaskEditorInitialState {
    val initialDueDate = task?.dueAt?.let { DateTimeUtils.toLocalDate(it) }
    val initialDueTime = if (task?.dueAt != null && task.hasDueTime) {
        DateTimeUtils.epochToLocalTime(task.dueAt)
    } else {
        LocalTime.of(9, 0)
    }
    val initialRecStartDate = RecurrenceEngine.parseLocalDateOrNull(activeRecurrence?.startDateIso)
        ?: initialDueDate
        ?: DateTimeUtils.todayLocalDate()
    val initialRecEndDate = RecurrenceEngine.parseLocalDateOrNull(activeRecurrence?.endDateIso)
    val initialRecEndMode = when {
        initialRecEndDate != null -> "DATE"
        activeRecurrence?.maxOccurrences != null && activeRecurrence.maxOccurrences > 0 -> "COUNT"
        else -> "NONE"
    }
    return TaskEditorInitialState(
        title = task?.title ?: "",
        description = task?.description ?: "",
        notes = task?.notes ?: "",
        projectId = task?.projectId,
        categoryId = task?.categoryId,
        eventId = task?.eventId ?: initialEventId,
        goalId = task?.goalId,
        priority = task?.priority ?: TaskPriority.NORMAL,
        status = task?.status ?: TaskStatus.NOT_STARTED,
        dueDate = initialDueDate,
        hasDueTime = task?.hasDueTime ?: false,
        dueTime = initialDueTime,
        subtasks = existingTask?.subtasks?.map { Triple(it.id as String?, it.title, it.isCompleted) }.orEmpty(),
        reminderMinutes = ReminderRules.resolveEditorReminderMinutes(existingReminder),
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TaskEditorSheet(
    existingTask: TaskWithDetails?,
    initialEventId: String? = null,
    existingReminder: ReminderEntity? = null,
    defaultReminderMinutes: Int = 30,
    projects: List<ProjectWithProgress>,
    categories: List<CategoryEntity>,
    events: List<EventWithDetails>,
    goals: List<PersonalGoalWithDetails> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (
        id: String?,
        title: String,
        description: String,
        projectId: String?,
        categoryId: String?,
        eventId: String?,
        goalId: String?,
        priority: TaskPriority,
        status: TaskStatus,
        dueAt: Long?,
        hasDueTime: Boolean,
        notes: String,
        subtaskDrafts: List<Triple<String?, String, Boolean>>,
        reminderMinutesBefore: Int?,
        recurrenceDraft: TaskRecurrenceDraft?,
        recurringEditScope: RecurringEditScope
    ) -> Unit,
    onToggleComplete: (String) -> Unit,
    onToggleSubtask: (SubtaskEntity) -> Unit,
    onOpenEvent: (String) -> Unit,
    onRegisterExpense: (
        taskId: String?,
        title: String,
        description: String,
        projectId: String?,
        categoryId: String?,
        eventId: String?,
        goalId: String?,
        priority: TaskPriority,
        status: TaskStatus,
        dueAt: Long?,
        hasDueTime: Boolean,
        notes: String,
        subtaskDrafts: List<Triple<String?, String, Boolean>>,
        reminderMinutesBefore: Int?,
        recurrenceDraft: TaskRecurrenceDraft?,
        recurringEditScope: RecurringEditScope
    ) -> Unit = { _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _ -> },
    onOpenTransaction: (String) -> Unit = {},
    onDelete: (String, RecurringEditScope) -> Unit,
    onDirtyStateChange: (Boolean) -> Unit = {}
) {
    val palette = LocalNexusPalette.current
    val task = existingTask?.task
    val activeRecurrence = existingTask?.recurrence?.takeIf { it.isActive }
    val taskKey: String? = task?.id
    val activeRecurrenceId: String? = activeRecurrence?.id
    val activeRecurrenceUpdatedAt: Long? = activeRecurrence?.updatedAt
    val reminderKey: String? = existingReminder?.id
    val reminderMinutesKey: Int? = existingReminder?.minutesBefore
    val reminderStatusKey: Any? = existingReminder?.status

    val init = remember(
        taskKey,
        activeRecurrenceId,
        activeRecurrenceUpdatedAt,
        initialEventId,
        reminderKey,
        reminderMinutesKey,
        reminderStatusKey
    ) {
        buildTaskEditorInitialState(
            task = task,
            activeRecurrence = activeRecurrence,
            existingTask = existingTask,
            initialEventId = initialEventId,
            existingReminder = existingReminder
        )
    }

    var title by remember(taskKey) { mutableStateOf(init.title) }
    var description by remember(taskKey) { mutableStateOf(init.description) }
    var notes by remember(taskKey) { mutableStateOf(init.notes) }
    var projectId by remember(taskKey) { mutableStateOf(init.projectId) }
    var categoryId by remember(taskKey) { mutableStateOf(init.categoryId) }
    var eventId by remember(taskKey, initialEventId) { mutableStateOf(init.eventId) }
    var goalId by remember(taskKey) { mutableStateOf(init.goalId) }
    var priority by remember(taskKey) { mutableStateOf(init.priority) }
    var status by remember(taskKey) { mutableStateOf(init.status) }

    var dueDate by remember(taskKey) { mutableStateOf(init.dueDate) }
    var hasDueTime by remember(taskKey) { mutableStateOf(init.hasDueTime) }
    var dueTime by remember(taskKey) { mutableStateOf(init.dueTime) }

    val draftSubtasks = remember(taskKey) {
        mutableStateListOf<Triple<String?, String, Boolean>>().apply {
            addAll(init.subtasks)
        }
    }
    var newSubtaskText by remember { mutableStateOf("") }

    var recurrenceFrequency by remember(taskKey, activeRecurrenceId, activeRecurrenceUpdatedAt) {
        mutableStateOf(init.recFrequency)
    }
    var customIntervalDaysText by remember(taskKey, activeRecurrenceId, activeRecurrenceUpdatedAt) {
        mutableStateOf(init.customIntervalText)
    }
    var weekdaysMask by remember(taskKey, activeRecurrenceId, activeRecurrenceUpdatedAt) {
        mutableStateOf(init.weekdaysMask)
    }
    var recurrenceStartDate by remember(taskKey, activeRecurrenceId, activeRecurrenceUpdatedAt) {
        mutableStateOf(init.recStartDate)
    }
    var recurrenceStartCustomized by remember(taskKey, activeRecurrenceId, activeRecurrenceUpdatedAt) {
        mutableStateOf(init.hasActiveRecurrence)
    }
    var recurrenceEndMode by remember(taskKey, activeRecurrenceId, activeRecurrenceUpdatedAt) {
        mutableStateOf(init.recEndMode)
    }
    var recurrenceEndDate by remember(taskKey, activeRecurrenceId, activeRecurrenceUpdatedAt) {
        mutableStateOf(init.recEndDate)
    }
    var maxOccurrencesText by remember(taskKey, activeRecurrenceId, activeRecurrenceUpdatedAt) {
        mutableStateOf(init.maxOccurrencesText)
    }
    var showMoreDetails by remember(taskKey, activeRecurrenceId) {
        mutableStateOf(init.hasActiveRecurrence)
    }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showRecStartDatePicker by remember { mutableStateOf(false) }
    var showRecEndDatePicker by remember { mutableStateOf(false) }
    var showProjectDropdown by remember { mutableStateOf(false) }
    var showCategoryDropdown by remember { mutableStateOf(false) }
    var showEventDropdown by remember { mutableStateOf(false) }
    var showGoalDropdown by remember { mutableStateOf(false) }
    var showPriorityDropdown by remember { mutableStateOf(false) }
    var showStatusDropdown by remember { mutableStateOf(false) }
    var showRecurrenceFreqDropdown by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showDiscardConfirm by remember { mutableStateOf(false) }
    var showSaveForExpenseConfirm by remember { mutableStateOf(false) }
    var pendingScopeSaveAction by remember { mutableStateOf<((RecurringEditScope) -> Unit)?>(null) }
    var pendingRuleChangeConfirmAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var titleError by remember { mutableStateOf(false) }
    var reminderMinutesBefore by remember(taskKey, reminderKey) {
        mutableStateOf(init.reminderMinutes)
    }
    var reminderModified by remember(taskKey, reminderKey) { mutableStateOf(false) }

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

    val initChecklistSignature = init.subtasks.map { it.second.trim() }
    val currentChecklistSignature = draftSubtasks.map { it.second.trim() } +
        listOfNotNull(newSubtaskText.trim().takeIf { it.isNotEmpty() })
    val checklistStructureChanged = currentChecklistSignature != initChecklistSignature

    val templateFieldsChanged = title != init.title ||
        description != init.description ||
        notes != init.notes ||
        projectId != init.projectId ||
        categoryId != init.categoryId ||
        eventId != init.eventId ||
        goalId != init.goalId ||
        priority != init.priority ||
        dueDate != init.dueDate ||
        hasDueTime != init.hasDueTime ||
        (hasDueTime && dueTime != init.dueTime) ||
        reminderModified ||
        checklistStructureChanged

    val hasUnsavedChanges = templateFieldsChanged ||
        status != init.status ||
        draftSubtasks.toList() != init.subtasks ||
        newSubtaskText.isNotBlank() ||
        recurrenceRuleChanged

    fun buildRecurrenceDraft(): TaskRecurrenceDraft? = buildTaskRecurrenceDraft(
        recurrenceFrequency = recurrenceFrequency,
        customIntervalDaysText = customIntervalDaysText,
        weekdaysMask = weekdaysMask,
        recurrenceStartDate = recurrenceStartDate,
        recurrenceEndMode = recurrenceEndMode,
        recurrenceEndDate = recurrenceEndDate,
        maxOccurrencesText = maxOccurrencesText
    )

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
                    text = if (task == null) "Nova tarefa" else "Detalhes da tarefa",
                    style = MaterialTheme.typography.titleLarge,
                    color = palette.textPrimary
                )
                if (task != null) {
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = "Excluir tarefa",
                            tint = palette.danger
                        )
                    }
                }
            }

            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    if (it.isNotBlank()) titleError = false
                },
                label = { Text("Título *") },
                isError = titleError,
                supportingText = if (titleError) {
                    { Text("Informe o título da tarefa.") }
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedButton(
                        onClick = { showPriorityDropdown = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Prioridade: ${priority.label}", color = palette.textPrimary)
                    }
                    DropdownMenu(
                        expanded = showPriorityDropdown,
                        onDismissRequest = { showPriorityDropdown = false }
                    ) {
                        TaskPriority.entries.forEach { p ->
                            DropdownMenuItem(
                                text = { Text(p.label) },
                                onClick = {
                                    priority = p
                                    showPriorityDropdown = false
                                }
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    OutlinedButton(
                        onClick = { showStatusDropdown = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(status.label, color = palette.textPrimary)
                    }
                    DropdownMenu(
                        expanded = showStatusDropdown,
                        onDismissRequest = { showStatusDropdown = false }
                    ) {
                        TaskStatus.entries.forEach { s ->
                            DropdownMenuItem(
                                text = { Text(s.label) },
                                onClick = {
                                    status = s
                                    showStatusDropdown = false
                                }
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    val projName = projects.firstOrNull { it.project.id == projectId }?.project?.name ?: "Sem projeto"
                    OutlinedButton(
                        onClick = { showProjectDropdown = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(projName, color = palette.textPrimary, maxLines = 1)
                    }
                    DropdownMenu(
                        expanded = showProjectDropdown,
                        onDismissRequest = { showProjectDropdown = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Sem projeto") },
                            onClick = {
                                projectId = null
                                showProjectDropdown = false
                            }
                        )
                        projects.forEach { pw ->
                            DropdownMenuItem(
                                text = { Text(pw.project.name) },
                                onClick = {
                                    projectId = pw.project.id
                                    showProjectDropdown = false
                                }
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    val catName = categories.firstOrNull { it.id == categoryId }?.name ?: "Sem categoria"
                    OutlinedButton(
                        onClick = { showCategoryDropdown = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(catName, color = palette.textPrimary, maxLines = 1)
                    }
                    DropdownMenu(
                        expanded = showCategoryDropdown,
                        onDismissRequest = { showCategoryDropdown = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Sem categoria") },
                            onClick = {
                                categoryId = null
                                showCategoryDropdown = false
                            }
                        )
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.name) },
                                onClick = {
                                    categoryId = cat.id
                                    showCategoryDropdown = false
                                }
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    val evName = events.firstOrNull { it.event.id == eventId }?.event?.name ?: "Sem evento"
                    OutlinedButton(
                        onClick = { showEventDropdown = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Evento: $evName", color = palette.textPrimary, maxLines = 1)
                    }
                    DropdownMenu(
                        expanded = showEventDropdown,
                        onDismissRequest = { showEventDropdown = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Sem evento") },
                            onClick = {
                                eventId = null
                                showEventDropdown = false
                            }
                        )
                        events.forEach { ew ->
                            DropdownMenuItem(
                                text = { Text(ew.event.name) },
                                onClick = {
                                    eventId = ew.event.id
                                    showEventDropdown = false
                                }
                            )
                        }
                    }
                }
                eventId?.let { linkedEventId ->
                    TextButton(onClick = { onOpenEvent(linkedEventId) }) {
                        Text("Abrir evento", color = palette.info)
                    }
                }
            }

            if (goals.isNotEmpty()) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    val goalTitle = goals.firstOrNull { it.goal.id == goalId }?.goal?.title ?: "Sem objetivo pessoal"
                    OutlinedButton(
                        onClick = { showGoalDropdown = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Objetivo: $goalTitle", color = palette.textPrimary, maxLines = 1)
                    }
                    DropdownMenu(
                        expanded = showGoalDropdown,
                        onDismissRequest = { showGoalDropdown = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Sem objetivo pessoal") },
                            onClick = {
                                goalId = null
                                showGoalDropdown = false
                            }
                        )
                        goals.forEach { gw ->
                            DropdownMenuItem(
                                text = { Text(gw.goal.title) },
                                onClick = {
                                    goalId = gw.goal.id
                                    showGoalDropdown = false
                                }
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
                    Column {
                        Text("Quando", style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
                        Text(
                            text = dueDate?.let { d ->
                                if (hasDueTime) "${DateTimeUtils.formatDate(d)} às $dueTime" else DateTimeUtils.formatDate(d)
                            } ?: "Sem data definida",
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.textSecondary
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (dueDate != null) {
                            TextButton(onClick = {
                                dueDate = null
                                hasDueTime = false
                            }) {
                                Text("Limpar", color = palette.textSecondary)
                            }
                        }
                        OutlinedButton(onClick = { showDatePicker = true }) {
                            Text(if (dueDate == null) "Definir" else "Alterar", color = palette.textPrimary)
                        }
                    }
                }

                if (dueDate != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Incluir horário (24h)", style = MaterialTheme.typography.bodyMedium, color = palette.textPrimary)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (hasDueTime) {
                                TextButton(onClick = { showTimePicker = true }) {
                                    Text(dueTime.toString(), color = palette.info)
                                }
                            }
                            Switch(
                                checked = hasDueTime,
                                onCheckedChange = { checked ->
                                    hasDueTime = checked
                                    if (checked) showTimePicker = true
                                }
                            )
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
                            recurrenceStartDate = dueDate ?: DateTimeUtils.todayLocalDate()
                        }
                        if (freq == TaskRecurrenceFrequency.SPECIFIC_WEEKDAYS && weekdaysMask == 0) {
                            val anchor = dueDate ?: recurrenceStartDate
                            weekdaysMask = RecurrenceEngine.weekdayBit(anchor.dayOfWeek)
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
                onMaxOccurrencesChange = { maxOccurrencesText = it }
            )

            NexusCard(modifier = Modifier.fillMaxWidth(), elevated = true) {
                val addPendingSubtask = {
                    val clean = newSubtaskText.trim()
                    if (clean.isNotEmpty()) {
                        draftSubtasks.add(Triple(null, clean, false))
                        newSubtaskText = ""
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Checklist", style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
                    if (draftSubtasks.isNotEmpty()) {
                        val doneCount = draftSubtasks.count { it.third }
                        Text(
                            text = "$doneCount/${draftSubtasks.size}",
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.textSecondary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                draftSubtasks.forEachIndexed { index, (subId, subTitle, isDone) ->
                    val toggleItem = {
                        val nextChecked = !isDone
                        draftSubtasks[index] = Triple(subId, subTitle, nextChecked)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { toggleItem() },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isDone,
                            onCheckedChange = { checked ->
                                draftSubtasks[index] = Triple(subId, subTitle, checked)
                            }
                        )
                        Text(
                            text = subTitle,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None
                            ),
                            color = if (isDone) palette.textSecondary else palette.textPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { draftSubtasks.removeAt(index) }) {
                            Icon(Icons.Outlined.DeleteOutline, contentDescription = "Remover", tint = palette.textSecondary)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newSubtaskText,
                        onValueChange = { newSubtaskText = it },
                        placeholder = { Text("Adicionar item ao checklist") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { addPendingSubtask() }),
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = addPendingSubtask) {
                        Icon(Icons.Outlined.Add, contentDescription = "Adicionar item", tint = palette.highlight)
                    }
                }
            }

            val linkedTransactions = existingTask?.linkedTransactions.orEmpty()
            val totalExpenseCents = existingTask?.totalExpenseCents ?: 0L
            val highlightExpenseButton = status == TaskStatus.COMPLETED
            NexusCard(modifier = Modifier.fillMaxWidth(), elevated = true) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (linkedTransactions.isEmpty()) "Gastos da tarefa"
                            else "Gasto: ${DateTimeUtils.formatBrl(totalExpenseCents)}",
                            style = MaterialTheme.typography.titleMedium,
                            color = palette.textPrimary
                        )
                        Text(
                            text = if (linkedTransactions.isEmpty()) {
                                "Opcional — vincule despesas feitas para esta tarefa."
                            } else {
                                "${linkedTransactions.size} lançamento(s) vinculado(s)"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.textSecondary
                        )
                    }
                    OutlinedButton(
                        onClick = {
                            if (title.isBlank()) {
                                titleError = true
                                return@OutlinedButton
                            }
                            if (task == null || hasUnsavedChanges) {
                                showSaveForExpenseConfirm = true
                                return@OutlinedButton
                            }
                            val dueEpoch = dueDate?.let { date ->
                                val time = if (hasDueTime) dueTime else LocalTime.of(23, 59)
                                DateTimeUtils.localDateTimeToEpoch(date, time)
                            }
                            onRegisterExpense(
                                task.id,
                                title,
                                description,
                                projectId,
                                categoryId,
                                eventId,
                                goalId,
                                priority,
                                status,
                                dueEpoch,
                                hasDueTime,
                                notes,
                                draftSubtasks.toList(),
                                null,
                                buildRecurrenceDraft(),
                                RecurringEditScope.THIS_OCCURRENCE_ONLY
                            )
                        },
                        shape = RoundedCornerShape(10.dp),
                        border = if (highlightExpenseButton) {
                            palette.coloredBorder(palette.positive, ColoredBorderRole.HIGHLIGHTED_BUTTON)
                        } else {
                            BorderStroke(1.dp, palette.border)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ReceiptLong,
                            contentDescription = null,
                            tint = if (highlightExpenseButton) palette.positive else palette.highlight
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Registrar gasto",
                            color = if (highlightExpenseButton) palette.positive else palette.textPrimary
                        )
                    }
                }
                if (linkedTransactions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    linkedTransactions.forEach { linkedTx ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenTransaction(linkedTx.id) }
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = linkedTx.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = palette.textPrimary
                                )
                                Text(
                                    text = "${linkedTx.paymentMethod.label} · ${linkedTx.status.label}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.textSecondary
                                )
                            }
                            Text(
                                text = DateTimeUtils.formatBrl(linkedTx.amountCents),
                                style = MaterialTheme.typography.labelMedium,
                                color = palette.info
                            )
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

            if (task != null) {
                HorizontalDivider(color = palette.border)
                Text(
                    text = buildString {
                        append("Criada em ${DateTimeUtils.formatDateTime(task.createdAt)}")
                        task.completedAt?.let {
                            append(" · Concluída em ${DateTimeUtils.formatDateTime(it)}")
                        }
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.textSecondary
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (task != null) {
                    OutlinedButton(
                        onClick = {
                            onToggleComplete(task.id)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, palette.border)
                    ) {
                        Text(
                            text = if (task.status == TaskStatus.COMPLETED) "Reabrir" else "Concluir",
                            color = palette.positive
                        )
                    }
                }

                Button(
                    onClick = {
                        if (title.isBlank()) {
                            titleError = true
                            return@Button
                        }
                        val dueEpoch = dueDate?.let { date ->
                            val time = if (hasDueTime) dueTime else LocalTime.of(23, 59)
                            DateTimeUtils.localDateTimeToEpoch(date, time)
                        }
                        val finalSubtasks = draftSubtasks.toMutableList().apply {
                            val pendingSub = newSubtaskText.trim()
                            if (pendingSub.isNotEmpty()) add(Triple(null, pendingSub, false))
                        }
                        val recDraft = buildRecurrenceDraft()
                        dispatchSaveWithScopeHandling { editScope ->
                            onSave(
                                task?.id,
                                title,
                                description,
                                projectId,
                                categoryId,
                                eventId,
                                goalId,
                                priority,
                                status,
                                dueEpoch,
                                hasDueTime,
                                notes,
                                finalSubtasks,
                                if (reminderModified) (reminderMinutesBefore ?: defaultReminderMinutes) else null,
                                recDraft,
                                editScope
                            )
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.highlight,
                        contentColor = palette.background
                    )
                ) {
                    Text("Salvar")
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    if (showDatePicker) {
        NexusDatePickerDialog(
            initialDate = dueDate ?: DateTimeUtils.todayLocalDate(),
            onDateSelected = { picked ->
                dueDate = picked
                if (!recurrenceStartCustomized) {
                    recurrenceStartDate = picked
                }
            },
            onDismiss = { showDatePicker = false }
        )
    }

    if (showTimePicker) {
        NexusTimePickerDialog(
            initialTime = dueTime,
            onTimeSelected = { dueTime = it },
            onDismiss = { showTimePicker = false }
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

    TaskScopeConfirmDialogs(
        task = task,
        hasActiveRecurrence = activeRecurrence != null,
        linkedTransactionsCount = existingTask?.linkedTransactions?.size ?: 0,
        recurrenceFrequency = recurrenceFrequency,
        showDeleteConfirm = showDeleteConfirm,
        onDismissDeleteConfirm = { showDeleteConfirm = false },
        onConfirmDelete = { deleteScope ->
            showDeleteConfirm = false
            task?.let { onDelete(it.id, deleteScope) }
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

    if (showSaveForExpenseConfirm) {
        AlertDialog(
            onDismissRequest = { showSaveForExpenseConfirm = false },
            containerColor = palette.surfaceElevated,
            title = { Text("Salvar tarefa para vincular gasto?", color = palette.textPrimary) },
            text = {
                Text(
                    text = if (task == null) {
                        "Para registrar uma despesa vinculada, a nova tarefa será salva primeiro."
                    } else {
                        "Há alterações não salvas nesta tarefa. Deseja salvar a tarefa antes de abrir o lançamento?"
                    },
                    color = palette.textSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSaveForExpenseConfirm = false
                        val dueEpoch = dueDate?.let { date ->
                            val time = if (hasDueTime) dueTime else LocalTime.of(23, 59)
                            DateTimeUtils.localDateTimeToEpoch(date, time)
                        }
                        val finalSubtasks = draftSubtasks.toMutableList().apply {
                            val pendingSub = newSubtaskText.trim()
                            if (pendingSub.isNotEmpty()) add(Triple(null, pendingSub, false))
                        }
                        val recDraft = buildRecurrenceDraft()
                        dispatchSaveWithScopeHandling { editScope ->
                            onRegisterExpense(
                                task?.id,
                                title,
                                description,
                                projectId,
                                categoryId,
                                eventId,
                                goalId,
                                priority,
                                status,
                                dueEpoch,
                                hasDueTime,
                                notes,
                                finalSubtasks,
                                if (reminderModified) (reminderMinutesBefore ?: defaultReminderMinutes) else null,
                                recDraft,
                                editScope
                            )
                        }
                    }
                ) {
                    Text("Salvar e registrar gasto")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveForExpenseConfirm = false }) {
                    Text("Continuar editando", color = palette.textSecondary)
                }
            }
        )
    }
}

internal fun buildTaskRecurrenceDraft(
    recurrenceFrequency: TaskRecurrenceFrequency?,
    customIntervalDaysText: String,
    weekdaysMask: Int,
    recurrenceStartDate: LocalDate,
    recurrenceEndMode: String,
    recurrenceEndDate: LocalDate?,
    maxOccurrencesText: String
): TaskRecurrenceDraft? {
    val freq = recurrenceFrequency ?: return null
    val interval = (customIntervalDaysText.toIntOrNull() ?: 1).coerceAtLeast(1)
    val effectiveMask = if (freq == TaskRecurrenceFrequency.SPECIFIC_WEEKDAYS) {
        weekdaysMask.takeIf { it > 0 } ?: RecurrenceEngine.weekdayBit(recurrenceStartDate.dayOfWeek)
    } else {
        0
    }
    val effectiveEndDateIso = if (recurrenceEndMode == "DATE") {
        (recurrenceEndDate ?: recurrenceStartDate.plusMonths(1)).toString()
    } else {
        null
    }
    val effectiveMaxOccurrences = if (recurrenceEndMode == "COUNT") {
        (maxOccurrencesText.toIntOrNull() ?: 5).coerceAtLeast(1)
    } else {
        null
    }
    return TaskRecurrenceDraft(
        frequency = freq,
        customIntervalDays = interval,
        weekdaysMask = effectiveMask,
        startDateIso = recurrenceStartDate.toString(),
        endDateIso = effectiveEndDateIso,
        maxOccurrences = effectiveMaxOccurrences
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TaskRecurrenceSectionCard(
    showMoreDetails: Boolean,
    onToggleMoreDetails: () -> Unit,
    recurrenceFrequency: TaskRecurrenceFrequency?,
    showRecurrenceFreqDropdown: Boolean,
    onToggleRecurrenceFreqDropdown: (Boolean) -> Unit,
    onSelectRecurrenceFrequency: (TaskRecurrenceFrequency?) -> Unit,
    customIntervalDaysText: String,
    onCustomIntervalChange: (String) -> Unit,
    weekdaysMask: Int,
    onWeekdaysMaskChange: (Int) -> Unit,
    recurrenceStartDate: LocalDate,
    onPickRecurrenceStartDate: () -> Unit,
    recurrenceEndMode: String,
    onSelectRecurrenceEndMode: (String) -> Unit,
    recurrenceEndDate: LocalDate?,
    onPickRecurrenceEndDate: () -> Unit,
    maxOccurrencesText: String,
    onMaxOccurrencesChange: (String) -> Unit,
    subtitleWhenEmpty: String = "Repetição e opções da série",
    infoNotice: String? = null
) {
    val palette = LocalNexusPalette.current
    NexusCard(modifier = Modifier.fillMaxWidth(), elevated = true) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggleMoreDetails() },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Repeat,
                    contentDescription = null,
                    tint = if (recurrenceFrequency != null) palette.accentNeon else palette.textSecondary,
                    modifier = Modifier.size(18.dp)
                )
                Column {
                    Text(
                        text = "Mais detalhes",
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.textPrimary
                    )
                    Text(
                        text = recurrenceFrequency?.let { "Repetir: ${it.label}" } ?: subtitleWhenEmpty,
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.textSecondary
                    )
                }
            }
            Icon(
                imageVector = if (showMoreDetails) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                contentDescription = if (showMoreDetails) "Recolher detalhes" else "Expandir detalhes",
                tint = palette.textSecondary
            )
        }

        if (showMoreDetails) {
            Spacer(modifier = Modifier.height(12.dp))
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { onToggleRecurrenceFreqDropdown(true) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "Repetir: ${recurrenceFrequency?.label ?: "Não repetir"}",
                        color = palette.textPrimary,
                        maxLines = 1
                    )
                }
                DropdownMenu(
                    expanded = showRecurrenceFreqDropdown,
                    onDismissRequest = { onToggleRecurrenceFreqDropdown(false) }
                ) {
                    DropdownMenuItem(
                        text = { Text("Não repetir") },
                        onClick = {
                            onSelectRecurrenceFrequency(null)
                            onToggleRecurrenceFreqDropdown(false)
                        }
                    )
                    TaskRecurrenceFrequency.entries.forEach { freq ->
                        DropdownMenuItem(
                            text = { Text(freq.label) },
                            onClick = {
                                onSelectRecurrenceFrequency(freq)
                                onToggleRecurrenceFreqDropdown(false)
                            }
                        )
                    }
                }
            }

            if (recurrenceFrequency == TaskRecurrenceFrequency.CUSTOM_DAYS) {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = customIntervalDaysText,
                    onValueChange = { raw ->
                        onCustomIntervalChange(raw.filter { it.isDigit() }.take(3))
                    },
                    label = { Text("Intervalo em dias") },
                    supportingText = { Text("Mínimo de 1 dia entre ocorrências.") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (recurrenceFrequency == TaskRecurrenceFrequency.SPECIFIC_WEEKDAYS) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Dias da semana",
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.textSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    DayOfWeek.entries.forEach { day ->
                        val bit = RecurrenceEngine.weekdayBit(day)
                        val selected = (weekdaysMask and bit) != 0
                        val dayLabel = day.getDisplayName(TextStyle.SHORT, DateTimeUtils.LOCALE_PT_BR)
                            .replace(".", "")
                            .replaceFirstChar { it.uppercase(DateTimeUtils.LOCALE_PT_BR) }
                        FilterChip(
                            selected = selected,
                            onClick = {
                                val nextMask = if (selected) weekdaysMask and bit.inv() else weekdaysMask or bit
                                if (nextMask != 0) {
                                    onWeekdaysMaskChange(nextMask)
                                }
                            },
                            label = { Text(dayLabel) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = palette.surface,
                                selectedLabelColor = palette.accentNeon
                            )
                        )
                    }
                }
            }

            if (recurrenceFrequency != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Início da série",
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.textSecondary
                        )
                        Text(
                            text = DateTimeUtils.formatDate(recurrenceStartDate),
                            style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
                            color = palette.textPrimary
                        )
                    }
                    OutlinedButton(
                        onClick = onPickRecurrenceStartDate,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Alterar", color = palette.textPrimary)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Término da repetição",
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.textSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(
                        "NONE" to "Sem fim",
                        "DATE" to "Até data",
                        "COUNT" to "Máx. ocorrências"
                    ).forEach { (modeKey, label) ->
                        FilterChip(
                            selected = recurrenceEndMode == modeKey,
                            onClick = { onSelectRecurrenceEndMode(modeKey) },
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = palette.surface,
                                selectedLabelColor = palette.accentNeon
                            )
                        )
                    }
                }

                if (recurrenceEndMode == "DATE") {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Data final",
                                style = MaterialTheme.typography.labelMedium,
                                color = palette.textSecondary
                            )
                            Text(
                                text = DateTimeUtils.formatDate(recurrenceEndDate ?: recurrenceStartDate.plusMonths(1)),
                                style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
                                color = palette.textPrimary
                            )
                        }
                        OutlinedButton(
                            onClick = onPickRecurrenceEndDate,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Alterar", color = palette.textPrimary)
                        }
                    }
                } else if (recurrenceEndMode == "COUNT") {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = maxOccurrencesText,
                        onValueChange = { raw ->
                            onMaxOccurrencesChange(raw.filter { it.isDigit() }.take(3))
                        },
                        label = { Text("Máximo de ocorrências") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (!infoNotice.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = infoNotice,
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textSecondary
                    )
                }
            }
        }
    }
}

@Composable
internal fun RecurringScopeConfirmDialogs(
    entityTitle: String?,
    hasActiveRecurrence: Boolean,
    recurrenceFrequency: TaskRecurrenceFrequency?,
    deleteTitle: String,
    deleteRecurringTitle: String,
    deleteSingleMessage: String,
    deleteRecurringMessage: String,
    updateRecurringTitle: String,
    showDeleteConfirm: Boolean,
    onDismissDeleteConfirm: () -> Unit,
    onConfirmDelete: (RecurringEditScope) -> Unit,
    pendingScopeSaveAction: ((RecurringEditScope) -> Unit)?,
    onClearPendingScopeSave: () -> Unit,
    pendingRuleChangeConfirmAction: (() -> Unit)?,
    onClearPendingRuleChange: () -> Unit
) {
    val palette = LocalNexusPalette.current

    if (showDeleteConfirm && entityTitle != null) {
        if (hasActiveRecurrence) {
            AlertDialog(
                onDismissRequest = onDismissDeleteConfirm,
                containerColor = palette.surfaceElevated,
                title = {
                    Text(
                        text = deleteRecurringTitle,
                        color = palette.textPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                text = {
                    Text(
                        text = deleteRecurringMessage,
                        color = palette.textSecondary
                    )
                },
                confirmButton = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(
                            onClick = { onConfirmDelete(RecurringEditScope.ENTIRE_SERIES) }
                        ) {
                            Text("Toda a série", color = palette.danger)
                        }
                        Button(
                            onClick = { onConfirmDelete(RecurringEditScope.THIS_OCCURRENCE_ONLY) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = palette.danger,
                                contentColor = androidx.compose.ui.graphics.Color.White
                            )
                        ) {
                            Text("Só esta")
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismissDeleteConfirm) {
                        Text("Cancelar", color = palette.textPrimary)
                    }
                }
            )
        } else {
            ConfirmDangerDialog(
                title = deleteTitle,
                message = deleteSingleMessage,
                onConfirm = { onConfirmDelete(RecurringEditScope.THIS_OCCURRENCE_ONLY) },
                onDismiss = onDismissDeleteConfirm
            )
        }
    }

    pendingScopeSaveAction?.let { executeSave ->
        AlertDialog(
            onDismissRequest = onClearPendingScopeSave,
            containerColor = palette.surfaceElevated,
            title = {
                Text(
                    text = updateRecurringTitle,
                    color = palette.textPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Text(
                    text = "Deseja aplicar as alterações somente nesta ocorrência ou no modelo de toda a série?",
                    color = palette.textSecondary
                )
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            onClearPendingScopeSave()
                            executeSave(RecurringEditScope.ENTIRE_SERIES)
                        }
                    ) {
                        Text("Toda a série", color = palette.info)
                    }
                    Button(
                        onClick = {
                            onClearPendingScopeSave()
                            executeSave(RecurringEditScope.THIS_OCCURRENCE_ONLY)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.highlight,
                            contentColor = palette.background
                        )
                    ) {
                        Text("Só esta ocorrência")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = onClearPendingScopeSave) {
                    Text("Cancelar", color = palette.textSecondary)
                }
            }
        )
    }

    pendingRuleChangeConfirmAction?.let { confirmSeriesSave ->
        AlertDialog(
            onDismissRequest = onClearPendingRuleChange,
            containerColor = palette.surfaceElevated,
            title = {
                Text(
                    text = "Alterar regra da série?",
                    color = palette.textPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Text(
                    text = if (recurrenceFrequency == null) {
                        "Remover a repetição encerrará a série recorrente, preservando registros já realizados quando aplicável."
                    } else {
                        "Alterações na frequência, dias ou período de repetição serão aplicadas a toda a série."
                    },
                    color = palette.textSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onClearPendingRuleChange()
                        confirmSeriesSave()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.highlight,
                        contentColor = palette.background
                    )
                ) {
                    Text("Aplicar na série")
                }
            },
            dismissButton = {
                TextButton(onClick = onClearPendingRuleChange) {
                    Text("Cancelar", color = palette.textSecondary)
                }
            }
        )
    }
}

@Composable
private fun TaskScopeConfirmDialogs(
    task: TaskEntity?,
    hasActiveRecurrence: Boolean,
    linkedTransactionsCount: Int,
    recurrenceFrequency: TaskRecurrenceFrequency?,
    showDeleteConfirm: Boolean,
    onDismissDeleteConfirm: () -> Unit,
    onConfirmDelete: (RecurringEditScope) -> Unit,
    pendingScopeSaveAction: ((RecurringEditScope) -> Unit)?,
    onClearPendingScopeSave: () -> Unit,
    pendingRuleChangeConfirmAction: (() -> Unit)?,
    onClearPendingRuleChange: () -> Unit
) {
    val deleteSingleMessage = if (task != null && linkedTransactionsCount > 0) {
        "A tarefa \"${task.title}\" e suas subtarefas serão removidas permanentemente. Os $linkedTransactionsCount lançamento(s) financeiro(s) vinculado(s) serão mantidos sem vínculo."
    } else if (task != null) {
        "A tarefa \"${task.title}\" e suas subtarefas serão removidas permanentemente."
    } else {
        ""
    }
    val deleteRecurringMessage = if (task != null && linkedTransactionsCount > 0) {
        "\"${task.title}\" faz parte de uma série recorrente. Você pode remover só esta ocorrência ou toda a série (os $linkedTransactionsCount lançamento(s) vinculado(s) serão mantidos sem vínculo)."
    } else if (task != null) {
        "\"${task.title}\" faz parte de uma série recorrente. Deseja remover apenas esta ocorrência ou encerrar toda a série?"
    } else {
        ""
    }
    RecurringScopeConfirmDialogs(
        entityTitle = task?.title,
        hasActiveRecurrence = hasActiveRecurrence,
        recurrenceFrequency = recurrenceFrequency,
        deleteTitle = "Excluir tarefa?",
        deleteRecurringTitle = "Excluir tarefa recorrente?",
        deleteSingleMessage = deleteSingleMessage,
        deleteRecurringMessage = deleteRecurringMessage,
        updateRecurringTitle = "Atualizar tarefa recorrente",
        showDeleteConfirm = showDeleteConfirm,
        onDismissDeleteConfirm = onDismissDeleteConfirm,
        onConfirmDelete = onConfirmDelete,
        pendingScopeSaveAction = pendingScopeSaveAction,
        onClearPendingScopeSave = onClearPendingScopeSave,
        pendingRuleChangeConfirmAction = pendingRuleChangeConfirmAction,
        onClearPendingRuleChange = onClearPendingRuleChange
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppointmentEditorSheet(
    existingAppointment: AppointmentEntity?,
    existingRecurrence: TaskRecurrenceEntity? = null,
    categories: List<CategoryEntity>,
    events: List<EventWithDetails> = emptyList(),
    initialDate: LocalDate,
    existingReminder: ReminderEntity? = null,
    defaultReminderMinutes: Int = 30,
    onDismiss: () -> Unit,
    onSave: (
        id: String?,
        title: String,
        description: String,
        categoryId: String?,
        eventId: String?,
        startAt: Long,
        endAt: Long,
        isAllDay: Boolean,
        location: String,
        link: String,
        reminderMinutesBefore: Int?,
        recurrenceDraft: TaskRecurrenceDraft?,
        recurringEditScope: RecurringEditScope
    ) -> Unit,
    onDelete: (String, RecurringEditScope) -> Unit,
    onError: (String, Throwable) -> Unit,
    onDirtyStateChange: (Boolean) -> Unit = {}
) {
    val palette = LocalNexusPalette.current
    val context = LocalContext.current

    val appointmentId: String? = existingAppointment?.id
    val activeRecurrence = existingRecurrence?.takeIf { it.isActive }
    val activeRecurrenceId: String? = activeRecurrence?.id
    val activeRecurrenceUpdatedAt: Long? = activeRecurrence?.updatedAt
    val reminderId: String? = existingReminder?.id
    val reminderMinutesKey: Int? = existingReminder?.minutesBefore
    val reminderStatusKey: Any? = existingReminder?.status

    val initStartDate = remember(appointmentId, initialDate) {
        existingAppointment?.let { DateTimeUtils.toLocalDate(it.startAt) } ?: initialDate
    }
    val initStartTime = remember(appointmentId) {
        existingAppointment?.let { DateTimeUtils.epochToLocalTime(it.startAt) } ?: LocalTime.of(9, 0)
    }
    val initEndDate = remember(appointmentId, initialDate) {
        existingAppointment?.let { DateTimeUtils.toLocalDate(it.endAt) } ?: initialDate
    }
    val initEndTime = remember(appointmentId) {
        existingAppointment?.let { DateTimeUtils.epochToLocalTime(it.endAt) } ?: LocalTime.of(10, 0)
    }
    val initReminderMinutes = remember(appointmentId, reminderId, reminderMinutesKey, reminderStatusKey) {
        ReminderRules.resolveEditorReminderMinutes(existingReminder)
    }
    val initRecFrequency = activeRecurrence?.frequency
    val initCustomIntervalText = (activeRecurrence?.customIntervalDays?.coerceAtLeast(1) ?: 2).toString()
    val initWeekdaysMask = activeRecurrence?.weekdaysMask ?: 0
    val initRecStartDate = remember(appointmentId, activeRecurrenceId, activeRecurrenceUpdatedAt, initStartDate) {
        RecurrenceEngine.parseLocalDateOrNull(activeRecurrence?.startDateIso) ?: initStartDate
    }
    val initRecEndDate = remember(appointmentId, activeRecurrenceId, activeRecurrenceUpdatedAt) {
        RecurrenceEngine.parseLocalDateOrNull(activeRecurrence?.endDateIso)
    }
    val initMaxOccurrencesText = activeRecurrence?.maxOccurrences?.takeIf { it > 0 }?.toString() ?: ""
    val initRecEndMode = when {
        initRecEndDate != null -> "DATE"
        activeRecurrence?.maxOccurrences != null && activeRecurrence.maxOccurrences > 0 -> "COUNT"
        else -> "NONE"
    }

    val initTitle = existingAppointment?.title ?: ""
    val initDescription = existingAppointment?.description ?: ""
    val initCategoryId = existingAppointment?.categoryId
    val initEventId = existingAppointment?.eventId
    val initIsAllDay = existingAppointment?.isAllDay ?: false
    val initLocation = existingAppointment?.location ?: ""
    val initLink = existingAppointment?.link ?: ""

    var title by remember(appointmentId) { mutableStateOf(initTitle) }
    var description by remember(appointmentId) { mutableStateOf(initDescription) }
    var categoryId by remember(appointmentId) { mutableStateOf(initCategoryId) }
    var eventId by remember(appointmentId) { mutableStateOf(initEventId) }
    var isAllDay by remember(appointmentId) { mutableStateOf(initIsAllDay) }
    var location by remember(appointmentId) { mutableStateOf(initLocation) }
    var link by remember(appointmentId) { mutableStateOf(initLink) }

    var startDate by remember(appointmentId) { mutableStateOf(initStartDate) }
    var startTime by remember(appointmentId) { mutableStateOf(initStartTime) }
    var endDate by remember(appointmentId) { mutableStateOf(initEndDate) }
    var endTime by remember(appointmentId) { mutableStateOf(initEndTime) }

    var recurrenceFrequency by remember(appointmentId, activeRecurrenceId, activeRecurrenceUpdatedAt) {
        mutableStateOf(initRecFrequency)
    }
    var customIntervalDaysText by remember(appointmentId, activeRecurrenceId, activeRecurrenceUpdatedAt) {
        mutableStateOf(initCustomIntervalText)
    }
    var weekdaysMask by remember(appointmentId, activeRecurrenceId, activeRecurrenceUpdatedAt) {
        mutableStateOf(initWeekdaysMask)
    }
    var recurrenceStartDate by remember(appointmentId, activeRecurrenceId, activeRecurrenceUpdatedAt) {
        mutableStateOf(initRecStartDate)
    }
    var recurrenceStartCustomized by remember(appointmentId, activeRecurrenceId, activeRecurrenceUpdatedAt) {
        mutableStateOf(activeRecurrence != null)
    }
    var recurrenceEndMode by remember(appointmentId, activeRecurrenceId, activeRecurrenceUpdatedAt) {
        mutableStateOf(initRecEndMode)
    }
    var recurrenceEndDate by remember(appointmentId, activeRecurrenceId, activeRecurrenceUpdatedAt) {
        mutableStateOf(initRecEndDate)
    }
    var maxOccurrencesText by remember(appointmentId, activeRecurrenceId, activeRecurrenceUpdatedAt) {
        mutableStateOf(initMaxOccurrencesText)
    }
    var showMoreDetails by remember(appointmentId, activeRecurrenceId) {
        mutableStateOf(activeRecurrence != null)
    }
    var showRecurrenceFreqDropdown by remember { mutableStateOf(false) }
    var showRecStartDatePicker by remember { mutableStateOf(false) }
    var showRecEndDatePicker by remember { mutableStateOf(false) }
    var pendingScopeSaveAction by remember { mutableStateOf<((RecurringEditScope) -> Unit)?>(null) }
    var pendingRuleChangeConfirmAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    var pickTarget by remember { mutableStateOf<String?>(null) }
    var showCategoryMenu by remember { mutableStateOf(false) }
    var showEventMenu by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showDiscardConfirm by remember { mutableStateOf(false) }
    var titleError by remember { mutableStateOf(false) }
    var reminderMinutesBefore by remember(appointmentId, reminderId) {
        mutableStateOf(initReminderMinutes)
    }
    var reminderModified by remember(appointmentId, reminderId) { mutableStateOf(false) }

    val recurrenceRuleChanged = recurrenceFrequency != initRecFrequency ||
        (recurrenceFrequency == TaskRecurrenceFrequency.CUSTOM_DAYS &&
            customIntervalDaysText.trim() != initCustomIntervalText) ||
        (recurrenceFrequency == TaskRecurrenceFrequency.SPECIFIC_WEEKDAYS &&
            weekdaysMask != initWeekdaysMask) ||
        (recurrenceFrequency != null && (
            recurrenceStartDate != initRecStartDate ||
                recurrenceEndMode != initRecEndMode ||
                (recurrenceEndMode == "DATE" && recurrenceEndDate != initRecEndDate) ||
                (recurrenceEndMode == "COUNT" && maxOccurrencesText.trim() != initMaxOccurrencesText)
            ))

    val templateFieldsChanged = title != initTitle ||
        description != initDescription ||
        categoryId != initCategoryId ||
        eventId != initEventId ||
        isAllDay != initIsAllDay ||
        location != initLocation ||
        link != initLink ||
        startDate != initStartDate ||
        endDate != initEndDate ||
        (!isAllDay && (startTime != initStartTime || endTime != initEndTime)) ||
        reminderModified

    val hasUnsavedChanges = templateFieldsChanged || recurrenceRuleChanged

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
                    text = if (existingAppointment == null) "Novo compromisso" else "Detalhes do compromisso",
                    style = MaterialTheme.typography.titleLarge,
                    color = palette.textPrimary
                )
                if (existingAppointment != null) {
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = "Excluir compromisso",
                            tint = palette.danger
                        )
                    }
                }
            }

            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    if (it.isNotBlank()) titleError = false
                },
                label = { Text("Título *") },
                isError = titleError,
                supportingText = if (titleError) {
                    { Text("Informe o título do compromisso.") }
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

            Box(modifier = Modifier.fillMaxWidth()) {
                val catName = categories.firstOrNull { it.id == categoryId }?.name ?: "Sem categoria"
                OutlinedButton(
                    onClick = { showCategoryMenu = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Categoria: $catName", color = palette.textPrimary)
                }
                DropdownMenu(
                    expanded = showCategoryMenu,
                    onDismissRequest = { showCategoryMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Sem categoria") },
                        onClick = {
                            categoryId = null
                            showCategoryMenu = false
                        }
                    )
                    categories.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat.name) },
                            onClick = {
                                categoryId = cat.id
                                showCategoryMenu = false
                            }
                        )
                    }
                }
            }

            if (events.isNotEmpty() || eventId != null) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    val evName = events.firstOrNull { it.event.id == eventId }?.event?.name ?: "Sem evento"
                    OutlinedButton(
                        onClick = { showEventMenu = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Evento: $evName", color = palette.textPrimary, maxLines = 1)
                    }
                    DropdownMenu(
                        expanded = showEventMenu,
                        onDismissRequest = { showEventMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Sem evento") },
                            onClick = {
                                eventId = null
                                showEventMenu = false
                            }
                        )
                        events.forEach { ew ->
                            DropdownMenuItem(
                                text = { Text(ew.event.name) },
                                onClick = {
                                    eventId = ew.event.id
                                    showEventMenu = false
                                }
                            )
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
                onMaxOccurrencesChange = { maxOccurrencesText = it }
            )

            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("Local") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = link,
                onValueChange = { link = it },
                label = { Text("Link opcional (URL ou Maps)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            if (link.isNotBlank() || location.isNotBlank()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (link.isNotBlank()) {
                        OutlinedButton(
                            onClick = {
                                runCatching {
                                    val normalized = if (link.startsWith("http://") || link.startsWith("https://")) {
                                        link.trim()
                                    } else {
                                        "https://${link.trim()}"
                                    }
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(normalized)))
                                }.onFailure { onError("Link externo", it) }
                            }
                        ) {
                            Icon(Icons.Outlined.OpenInNew, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Abrir link")
                        }
                    }
                    if (location.isNotBlank()) {
                        OutlinedButton(
                            onClick = {
                                runCatching {
                                    val geoUri = Uri.parse("geo:0,0?q=${Uri.encode(location.trim())}")
                                    context.startActivity(Intent(Intent.ACTION_VIEW, geoUri))
                                }.onFailure { onError("Abrir mapa", it) }
                            }
                        ) {
                            Icon(Icons.Outlined.OpenInNew, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Abrir no Mapa")
                        }
                    }
                }
            }

            Button(
                onClick = {
                    if (title.isBlank()) {
                        titleError = true
                        return@Button
                    }
                    val startEpoch = if (isAllDay) {
                        DateTimeUtils.startOfDayEpoch(startDate)
                    } else {
                        DateTimeUtils.localDateTimeToEpoch(startDate, startTime)
                    }
                    val endEpoch = if (isAllDay) {
                        DateTimeUtils.localDateToEndOfDayEpoch(if (endDate < startDate) startDate else endDate)
                    } else {
                        DateTimeUtils.localDateTimeToEpoch(
                            if (endDate < startDate) startDate else endDate,
                            endTime
                        )
                    }
                    val recDraft = buildTaskRecurrenceDraft(
                        recurrenceFrequency = recurrenceFrequency,
                        customIntervalDaysText = customIntervalDaysText,
                        weekdaysMask = weekdaysMask,
                        recurrenceStartDate = recurrenceStartDate,
                        recurrenceEndMode = recurrenceEndMode,
                        recurrenceEndDate = recurrenceEndDate,
                        maxOccurrencesText = maxOccurrencesText
                    )
                    dispatchSaveWithScopeHandling { editScope ->
                        onSave(
                            existingAppointment?.id,
                            title,
                            description,
                            categoryId,
                            eventId,
                            startEpoch,
                            endEpoch,
                            isAllDay,
                            location,
                            link,
                            if (reminderModified) (reminderMinutesBefore ?: defaultReminderMinutes) else null,
                            recDraft,
                            editScope
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
                Text("Salvar compromisso")
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

    RecurringScopeConfirmDialogs(
        entityTitle = existingAppointment?.title,
        hasActiveRecurrence = activeRecurrence != null,
        recurrenceFrequency = recurrenceFrequency,
        deleteTitle = "Excluir compromisso?",
        deleteRecurringTitle = "Excluir compromisso recorrente?",
        deleteSingleMessage = existingAppointment?.let {
            "O compromisso \"${it.title}\" será excluído permanentemente."
        }.orEmpty(),
        deleteRecurringMessage = existingAppointment?.let {
            "\"${it.title}\" faz parte de uma série recorrente. Deseja remover apenas esta ocorrência ou encerrar toda a série?"
        }.orEmpty(),
        updateRecurringTitle = "Atualizar compromisso recorrente",
        showDeleteConfirm = showDeleteConfirm,
        onDismissDeleteConfirm = { showDeleteConfirm = false },
        onConfirmDelete = { deleteScope ->
            showDeleteConfirm = false
            existingAppointment?.let { onDelete(it.id, deleteScope) }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectEditorSheet(
    existingProject: ProjectWithProgress?,
    goals: List<PersonalGoalWithDetails> = emptyList(),
    countLinkedTasks: suspend (String) -> Int,
    onDismiss: () -> Unit,
    onSave: (
        id: String?,
        name: String,
        description: String,
        deadlineAt: Long?,
        status: ProjectStatus,
        priority: TaskPriority,
        goalId: String?
    ) -> Unit,
    onDelete: (String) -> Unit,
    onDirtyStateChange: (Boolean) -> Unit = {}
) {
    val palette = LocalNexusPalette.current
    val project = existingProject?.project
    val projectIdKey: String? = project?.id

    val initialDeadlineDate = remember(projectIdKey) {
        project?.deadlineAt?.let { DateTimeUtils.toLocalDate(it) }
    }
    val initName = project?.name ?: ""
    val initDescription = project?.description ?: ""
    val initStatus = project?.status ?: ProjectStatus.ACTIVE
    val initPriority = project?.priority ?: TaskPriority.NORMAL
    val initGoalId = project?.goalId

    var name by remember(projectIdKey) { mutableStateOf(initName) }
    var description by remember(projectIdKey) { mutableStateOf(initDescription) }
    var deadlineDate by remember(projectIdKey) { mutableStateOf(initialDeadlineDate) }
    var status by remember(projectIdKey) { mutableStateOf(initStatus) }
    var priority by remember(projectIdKey) { mutableStateOf(initPriority) }
    var goalId by remember(projectIdKey) { mutableStateOf(initGoalId) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showStatusMenu by remember { mutableStateOf(false) }
    var showPriorityMenu by remember { mutableStateOf(false) }
    var showGoalMenu by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showDiscardConfirm by remember { mutableStateOf(false) }
    var linkedTaskCount by remember { mutableStateOf(0) }
    var nameError by remember { mutableStateOf(false) }

    LaunchedEffect(showDeleteConfirm, projectIdKey) {
        if (showDeleteConfirm && project != null) {
            linkedTaskCount = countLinkedTasks(project.id)
        }
    }

    val hasUnsavedChanges = name != initName ||
        description != initDescription ||
        deadlineDate != initialDeadlineDate ||
        status != initStatus ||
        priority != initPriority ||
        goalId != initGoalId

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
                    text = if (project == null) "Novo projeto" else "Editar projeto",
                    style = MaterialTheme.typography.titleLarge,
                    color = palette.textPrimary
                )
                if (project != null) {
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = "Excluir projeto",
                            tint = palette.danger
                        )
                    }
                }
            }

            if (existingProject != null) {
                Text(
                    text = "Progresso calculado: ${existingProject.completedTasks}/${existingProject.totalTasks} tarefas concluídas (${existingProject.progressPercent}%)",
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.textSecondary
                )
                if (existingProject.linkedTransactionsCount > 0) {
                    Text(
                        text = "Financeiro vinculado (${existingProject.linkedTransactionsCount}): ${DateTimeUtils.formatBrl(existingProject.estimatedCostCents)} · Pago: ${DateTimeUtils.formatBrl(existingProject.paidCostCents)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.info
                    )
                }
            }

            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    if (it.isNotBlank()) nameError = false
                },
                label = { Text("Nome do projeto *") },
                isError = nameError,
                supportingText = if (nameError) {
                    { Text("Informe o nome do projeto.") }
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedButton(
                        onClick = { showStatusMenu = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(status.label, color = palette.textPrimary)
                    }
                    DropdownMenu(
                        expanded = showStatusMenu,
                        onDismissRequest = { showStatusMenu = false }
                    ) {
                        ProjectStatus.entries.forEach { st ->
                            DropdownMenuItem(
                                text = { Text(st.label) },
                                onClick = {
                                    status = st
                                    showStatusMenu = false
                                }
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    OutlinedButton(
                        onClick = { showPriorityMenu = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Prioridade: ${priority.label}", color = palette.textPrimary)
                    }
                    DropdownMenu(
                        expanded = showPriorityMenu,
                        onDismissRequest = { showPriorityMenu = false }
                    ) {
                        TaskPriority.entries.forEach { pr ->
                            DropdownMenuItem(
                                text = { Text(pr.label) },
                                onClick = {
                                    priority = pr
                                    showPriorityMenu = false
                                }
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
                    Column {
                        Text("Prazo do projeto", style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
                        Text(
                            text = deadlineDate?.let { DateTimeUtils.formatDate(it) } ?: "Sem prazo definido",
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.textSecondary
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (deadlineDate != null) {
                            TextButton(onClick = { deadlineDate = null }) {
                                Text("Limpar", color = palette.textSecondary)
                            }
                        }
                        OutlinedButton(onClick = { showDatePicker = true }) {
                            Text(if (deadlineDate == null) "Definir" else "Alterar", color = palette.textPrimary)
                        }
                    }
                }
            }

            if (goals.isNotEmpty()) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    val goalTitle = goals.firstOrNull { it.goal.id == goalId }?.goal?.title ?: "Sem objetivo pessoal"
                    OutlinedButton(
                        onClick = { showGoalMenu = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Objetivo: $goalTitle", color = palette.textPrimary, maxLines = 1)
                    }
                    DropdownMenu(
                        expanded = showGoalMenu,
                        onDismissRequest = { showGoalMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Sem objetivo pessoal") },
                            onClick = {
                                goalId = null
                                showGoalMenu = false
                            }
                        )
                        goals.forEach { gw ->
                            DropdownMenuItem(
                                text = { Text(gw.goal.title) },
                                onClick = {
                                    goalId = gw.goal.id
                                    showGoalMenu = false
                                }
                            )
                        }
                    }
                }
            }

            Button(
                onClick = {
                    if (name.isBlank()) {
                        nameError = true
                        return@Button
                    }
                    val deadlineEpoch = deadlineDate?.let { DateTimeUtils.localDateToEndOfDayEpoch(it) }
                    onSave(project?.id, name, description, deadlineEpoch, status, priority, goalId)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.highlight,
                    contentColor = palette.background
                )
            ) {
                Text("Salvar projeto")
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    if (showDatePicker) {
        NexusDatePickerDialog(
            initialDate = deadlineDate ?: DateTimeUtils.todayLocalDate(),
            onDateSelected = { deadlineDate = it },
            onDismiss = { showDatePicker = false }
        )
    }

    if (showDeleteConfirm && project != null) {
        val message = if (linkedTaskCount > 0) {
            "Excluir o projeto \"${project.name}\" desvinculará $linkedTaskCount tarefa(s), sem apagá-las."
        } else {
            "Excluir o projeto \"${project.name}\"?"
        }
        ConfirmDangerDialog(
            title = "Excluir projeto?",
            message = message,
            confirmLabel = "Excluir projeto",
            onConfirm = {
                showDeleteConfirm = false
                onDelete(project.id)
            },
            onDismiss = { showDeleteConfirm = false }
        )
    }

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
