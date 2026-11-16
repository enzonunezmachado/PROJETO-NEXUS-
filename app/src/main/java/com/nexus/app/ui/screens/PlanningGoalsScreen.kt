package com.nexus.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.LinkOff
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.nexus.app.core.DateTimeUtils
import com.nexus.app.data.db.entity.CategoryEntity
import com.nexus.app.data.db.entity.GoalMilestoneEntity
import com.nexus.app.data.db.entity.PersonalGoalEntity
import com.nexus.app.domain.model.GoalProgressMode
import com.nexus.app.domain.model.GoalStatus
import com.nexus.app.domain.model.GoalStatusFilter
import com.nexus.app.domain.model.PersonalGoalWithDetails
import com.nexus.app.domain.model.ProjectWithProgress
import com.nexus.app.domain.model.TaskWithDetails
import com.nexus.app.domain.rules.BackAction
import com.nexus.app.domain.rules.BackNavigationState
import com.nexus.app.domain.rules.BackSubRouteKind
import com.nexus.app.domain.rules.BackTabKind
import com.nexus.app.domain.rules.resolveBackAction
import com.nexus.app.ui.GoalsFilterState
import com.nexus.app.ui.components.ConfirmDangerDialog
import com.nexus.app.ui.components.ConfirmDiscardChangesDialog
import com.nexus.app.ui.components.NexusCard
import com.nexus.app.ui.components.NexusDatePickerDialog
import com.nexus.app.ui.components.NexusEmptyState
import com.nexus.app.ui.components.SectionHeader
import com.nexus.app.ui.components.TaskRowCard
import com.nexus.app.ui.theme.LocalNexusPalette
import java.time.LocalDate

@Composable
fun PlanningGoalsScreen(
    goals: List<PersonalGoalWithDetails>,
    allGoals: List<PersonalGoalWithDetails>,
    categories: List<CategoryEntity>,
    allTasks: List<TaskWithDetails>,
    allProjects: List<ProjectWithProgress>,
    filter: GoalsFilterState,
    initialSelectedGoalId: String? = null,
    onBack: () -> Unit,
    onStatusFilterChange: (GoalStatusFilter) -> Unit,
    onCategoryFilterChange: (String?) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSaveGoal: (
        id: String?,
        title: String,
        description: String,
        categoryId: String?,
        targetDateAt: Long?,
        status: GoalStatus,
        progressMode: GoalProgressMode,
        manualProgressPercent: Int,
        notes: String
    ) -> Unit,
    onUpdateGoalStatus: (String, GoalStatus) -> Unit,
    onAddProgressLog: (String, Int, String) -> Unit,
    onDeleteGoal: (String) -> Unit,
    onSaveMilestone: (id: String?, goalId: String, title: String, description: String, dueAt: Long?) -> Unit,
    onToggleMilestone: (GoalMilestoneEntity) -> Unit,
    onDeleteMilestone: (String) -> Unit,
    onLinkTaskToGoal: (String, String?) -> Unit,
    onLinkProjectToGoal: (String, String?) -> Unit,
    onOpenTask: (String) -> Unit,
    onToggleTaskComplete: (String) -> Unit,
    onOpenProject: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalNexusPalette.current
    var selectedGoalId by remember(initialSelectedGoalId) { mutableStateOf(initialSelectedGoalId) }
    var showGoalEditor by remember { mutableStateOf(false) }
    var editingGoal by remember { mutableStateOf<PersonalGoalEntity?>(null) }

    val selectedGoalDetails = remember(allGoals, selectedGoalId) {
        selectedGoalId?.let { id -> allGoals.firstOrNull { it.goal.id == id } }
    }

    Column(modifier = modifier.fillMaxSize()) {
        TopSubScreenBar(title = "Planejamento e Objetivos", onBack = onBack)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = filter.searchQuery,
                        onValueChange = onSearchQueryChange,
                        placeholder = { Text("Buscar objetivos...") },
                        trailingIcon = {
                            if (filter.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { onSearchQueryChange("") }) {
                                    Icon(Icons.Outlined.Close, contentDescription = "Limpar busca")
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Button(
                        onClick = {
                            editingGoal = null
                            showGoalEditor = true
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.highlight,
                            contentColor = palette.background
                        )
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = "Novo objetivo", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Novo")
                    }
                }
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(GoalStatusFilter.entries) { stFilter ->
                        FilterChip(
                            selected = filter.statusFilter == stFilter,
                            onClick = { onStatusFilterChange(stFilter) },
                            label = { Text(stFilter.label) }
                        )
                    }
                }
            }

            if (categories.isNotEmpty()) {
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = filter.categoryFilterId == null,
                                onClick = { onCategoryFilterChange(null) },
                                label = { Text("Todas categorias") }
                            )
                        }
                        items(categories, key = { "cat_filter_${it.id}" }) { cat ->
                            FilterChip(
                                selected = filter.categoryFilterId == cat.id,
                                onClick = {
                                    onCategoryFilterChange(if (filter.categoryFilterId == cat.id) null else cat.id)
                                },
                                label = { Text(cat.name) }
                            )
                        }
                    }
                }
            }

            item {
                SectionHeader(
                    title = "Objetivos (${filter.statusFilter.label})",
                    trailing = "${goals.size}"
                )
            }

            if (goals.isEmpty()) {
                item {
                    NexusEmptyState(
                        icon = Icons.Outlined.Flag,
                        title = if (allGoals.isEmpty()) "Nenhum objetivo cadastrado" else "Nenhum objetivo neste filtro",
                        subtitle = if (allGoals.isEmpty()) {
                            "Crie objetivos pessoais de médio ou longo prazo, acompanhe marcos e conecte tarefas e projetos existentes sem duplicação."
                        } else {
                            "Altere o filtro ou adicione um novo objetivo pessoal."
                        },
                        actionLabel = "Novo objetivo",
                        onAction = {
                            editingGoal = null
                            showGoalEditor = true
                        }
                    )
                }
            } else {
                items(goals, key = { it.goal.id }) { item ->
                    PersonalGoalCard(
                        item = item,
                        onClick = { selectedGoalId = item.goal.id }
                    )
                }
            }
        }
    }

    if (showGoalEditor) {
        PersonalGoalEditorDialog(
            existing = editingGoal,
            categories = categories,
            onDismiss = {
                showGoalEditor = false
                editingGoal = null
            },
            onSave = { id, title, desc, catId, targetAt, status, mode, manualPct, notes ->
                onSaveGoal(id, title, desc, catId, targetAt, status, mode, manualPct, notes)
                showGoalEditor = false
                editingGoal = null
            }
        )
    }

    selectedGoalDetails?.let { details ->
        GoalDetailSheet(
            details = details,
            allTasks = allTasks,
            allProjects = allProjects,
            onDismiss = { selectedGoalId = null },
            onEditGoal = {
                editingGoal = details.goal
                showGoalEditor = true
            },
            onUpdateStatus = { st -> onUpdateGoalStatus(details.goal.id, st) },
            onAddProgressLog = { pct, note -> onAddProgressLog(details.goal.id, pct, note) },
            onDeleteGoal = {
                selectedGoalId = null
                onDeleteGoal(details.goal.id)
            },
            onSaveMilestone = { mId, title, desc, dueAt ->
                onSaveMilestone(mId, details.goal.id, title, desc, dueAt)
            },
            onToggleMilestone = onToggleMilestone,
            onDeleteMilestone = onDeleteMilestone,
            onLinkTask = { taskId -> onLinkTaskToGoal(taskId, details.goal.id) },
            onUnlinkTask = { taskId -> onLinkTaskToGoal(taskId, null) },
            onLinkProject = { projId -> onLinkProjectToGoal(projId, details.goal.id) },
            onUnlinkProject = { projId -> onLinkProjectToGoal(projId, null) },
            onOpenTask = onOpenTask,
            onToggleTaskComplete = onToggleTaskComplete,
            onOpenProject = onOpenProject
        )
    }
}

@Composable
fun PersonalGoalCard(
    item: PersonalGoalWithDetails,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalNexusPalette.current
    val goal = item.goal

    NexusCard(
        modifier = modifier.fillMaxWidth(),
        elevated = true,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = goal.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.textPrimary
                )
                if (goal.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = goal.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary,
                        maxLines = 2
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (item.isOverdue) "Atrasado" else goal.status.label,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = when {
                        item.isOverdue -> palette.danger
                        goal.status == GoalStatus.COMPLETED -> palette.positive
                        goal.status == GoalStatus.PAUSED -> palette.warning
                        else -> palette.info
                    }
                )
                Text(
                    text = "${item.progressPercent}%",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = palette.textPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        LinearProgressIndicator(
            progress = { (item.progressPercent / 100f).coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = when {
                item.isOverdue -> palette.danger
                item.progressPercent >= 100 -> palette.positive
                else -> palette.highlight
            },
            trackColor = palette.border
        )

        Spacer(modifier = Modifier.height(8.dp))
        val meta = buildList {
            item.category?.let { add(it.name) }
            goal.targetDateAt?.let { add("Prazo ${DateTimeUtils.formatDate(it)}") }
            add("${item.completedMilestonesCount}/${item.milestones.size} marcos")
            if (item.linkedTasks.isNotEmpty()) add("${item.linkedTasks.size} tarefa(s)")
            if (item.linkedProjects.isNotEmpty()) add("${item.linkedProjects.size} projeto(s)")
        }.joinToString(" · ")
        Text(
            text = meta,
            style = MaterialTheme.typography.labelSmall,
            color = palette.textSecondary
        )
    }
}

@Composable
private fun PersonalGoalEditorDialog(
    existing: PersonalGoalEntity?,
    categories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onSave: (
        id: String?,
        title: String,
        description: String,
        categoryId: String?,
        targetDateAt: Long?,
        status: GoalStatus,
        progressMode: GoalProgressMode,
        manualProgressPercent: Int,
        notes: String
    ) -> Unit
) {
    val palette = LocalNexusPalette.current
    val initialTargetDate = remember(existing?.id) {
        existing?.targetDateAt?.let { DateTimeUtils.toLocalDate(it) }
    }
    var title by remember(existing?.id) { mutableStateOf(existing?.title ?: "") }
    var description by remember(existing?.id) { mutableStateOf(existing?.description ?: "") }
    var categoryId by remember(existing?.id) { mutableStateOf(existing?.categoryId) }
    var targetDate by remember(existing?.id) { mutableStateOf(initialTargetDate) }
    var status by remember(existing?.id) { mutableStateOf(existing?.status ?: GoalStatus.ACTIVE) }
    var progressMode by remember(existing?.id) { mutableStateOf(existing?.progressMode ?: GoalProgressMode.AUTO) }
    var manualPct by remember(existing?.id) {
        mutableFloatStateOf((existing?.manualProgressPercent ?: 0).toFloat())
    }
    var notes by remember(existing?.id) { mutableStateOf(existing?.notes ?: "") }

    var showDatePicker by remember { mutableStateOf(false) }
    var showCategoryMenu by remember { mutableStateOf(false) }
    var showDiscardConfirm by remember { mutableStateOf(false) }
    var titleError by remember { mutableStateOf(false) }

    val hasUnsavedChanges = title != (existing?.title ?: "") ||
        description != (existing?.description ?: "") ||
        categoryId != existing?.categoryId ||
        targetDate != initialTargetDate ||
        status != (existing?.status ?: GoalStatus.ACTIVE) ||
        progressMode != (existing?.progressMode ?: GoalProgressMode.AUTO) ||
        manualPct.toInt() != (existing?.manualProgressPercent ?: 0) ||
        notes != (existing?.notes ?: "")

    val requestDismiss: () -> Unit = {
        val action = resolveBackAction(
            BackNavigationState(
                hasOpenEditor = true,
                editorHasUnsavedChanges = hasUnsavedChanges,
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

    AlertDialog(
        onDismissRequest = requestDismiss,
        containerColor = palette.surfaceElevated,
        title = {
            Text(
                text = if (existing == null) "Novo objetivo pessoal" else "Editar objetivo",
                color = palette.textPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        if (it.isNotBlank()) titleError = false
                    },
                    label = { Text("Título *") },
                    isError = titleError,
                    supportingText = if (titleError) {
                        { Text("Informe o título do objetivo.") }
                    } else null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descrição") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = targetDate?.let { "Prazo: ${DateTimeUtils.formatDate(it)}" } ?: "Sem prazo-alvo",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textPrimary
                    )
                    Row {
                        if (targetDate != null) {
                            TextButton(onClick = { targetDate = null }) {
                                Text("Limpar", color = palette.textSecondary)
                            }
                        }
                        TextButton(onClick = { showDatePicker = true }) {
                            Text(if (targetDate == null) "Definir data" else "Alterar", color = palette.info)
                        }
                    }
                }

                Text("Status", style = MaterialTheme.typography.labelMedium, color = palette.textSecondary)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(GoalStatus.entries) { st ->
                        FilterChip(
                            selected = status == st,
                            onClick = { status = st },
                            label = { Text(st.label) }
                        )
                    }
                }

                Text("Cálculo de progresso", style = MaterialTheme.typography.labelMedium, color = palette.textSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GoalProgressMode.entries.forEach { mode ->
                        FilterChip(
                            selected = progressMode == mode,
                            onClick = { progressMode = mode },
                            label = { Text(mode.label) }
                        )
                    }
                }

                if (progressMode == GoalProgressMode.MANUAL) {
                    Text(
                        text = "Progresso manual: ${manualPct.toInt()}%",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textPrimary
                    )
                    Slider(
                        value = manualPct,
                        onValueChange = { manualPct = it },
                        valueRange = 0f..100f
                    )
                } else {
                    Text(
                        text = "Calculado automaticamente por marcos, tarefas e projetos vinculados.",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary
                    )
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Observações") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isBlank()) {
                        titleError = true
                    } else {
                        val targetEpoch = targetDate?.let { DateTimeUtils.localDateToEndOfDayEpoch(it) }
                        onSave(
                            existing?.id,
                            title,
                            description,
                            categoryId,
                            targetEpoch,
                            status,
                            progressMode,
                            manualPct.toInt().coerceIn(0, 100),
                            notes
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.highlight,
                    contentColor = palette.background
                )
            ) {
                Text("Salvar")
            }
        },
        dismissButton = {
            TextButton(onClick = requestDismiss) {
                Text("Cancelar", color = palette.textPrimary)
            }
        }
    )

    if (showDatePicker) {
        NexusDatePickerDialog(
            initialDate = targetDate ?: DateTimeUtils.todayLocalDate(),
            onDateSelected = { targetDate = it },
            onDismiss = { showDatePicker = false }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GoalDetailSheet(
    details: PersonalGoalWithDetails,
    allTasks: List<TaskWithDetails>,
    allProjects: List<ProjectWithProgress>,
    onDismiss: () -> Unit,
    onEditGoal: () -> Unit,
    onUpdateStatus: (GoalStatus) -> Unit,
    onAddProgressLog: (Int, String) -> Unit,
    onDeleteGoal: () -> Unit,
    onSaveMilestone: (id: String?, title: String, description: String, dueAt: Long?) -> Unit,
    onToggleMilestone: (GoalMilestoneEntity) -> Unit,
    onDeleteMilestone: (String) -> Unit,
    onLinkTask: (String) -> Unit,
    onUnlinkTask: (String) -> Unit,
    onLinkProject: (String) -> Unit,
    onUnlinkProject: (String) -> Unit,
    onOpenTask: (String) -> Unit,
    onToggleTaskComplete: (String) -> Unit,
    onOpenProject: (String) -> Unit
) {
    val palette = LocalNexusPalette.current
    val goal = details.goal

    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showMilestoneEditor by remember { mutableStateOf(false) }
    var editingMilestone by remember { mutableStateOf<GoalMilestoneEntity?>(null) }
    var showTaskPicker by remember { mutableStateOf(false) }
    var showProjectPicker by remember { mutableStateOf(false) }
    var showProgressLogDialog by remember { mutableStateOf(false) }

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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = goal.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = palette.textPrimary
                    )
                    Text(
                        text = buildString {
                            append(if (details.isOverdue) "Atrasado" else goal.status.label)
                            details.category?.let { append(" · ${it.name}") }
                            goal.targetDateAt?.let { append(" · Prazo ${DateTimeUtils.formatDate(it)}") }
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = if (details.isOverdue) palette.danger else palette.textSecondary
                    )
                }
                IconButton(onClick = onEditGoal) {
                    Icon(Icons.Outlined.Edit, contentDescription = "Editar objetivo", tint = palette.textPrimary)
                }
                IconButton(onClick = { showDeleteConfirm = true }) {
                    Icon(Icons.Outlined.DeleteOutline, contentDescription = "Excluir objetivo", tint = palette.danger)
                }
            }

            if (goal.description.isNotBlank()) {
                Text(
                    text = goal.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.textSecondary
                )
            }

            NexusCard(modifier = Modifier.fillMaxWidth(), elevated = true) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Progresso (${goal.progressMode.label})",
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.textPrimary
                    )
                    Text(
                        text = "${details.progressPercent}%",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = if (details.progressPercent >= 100) palette.positive else palette.highlight
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { (details.progressPercent / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = if (details.progressPercent >= 100) palette.positive else palette.highlight,
                    trackColor = palette.border
                )
                Spacer(modifier = Modifier.height(10.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(GoalStatus.entries) { st ->
                        FilterChip(
                            selected = goal.status == st,
                            onClick = { onUpdateStatus(st) },
                            label = { Text(st.label) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedButton(
                    onClick = { showProgressLogDialog = true },
                    border = BorderStroke(1.dp, palette.border),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Outlined.TrendingUp, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Registrar evolução", color = palette.textPrimary)
                }
            }

            // Marcos
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Marcos (${details.completedMilestonesCount}/${details.milestones.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.textPrimary
                )
                TextButton(onClick = {
                    editingMilestone = null
                    showMilestoneEditor = true
                }) {
                    Text("+ Marco", color = palette.info)
                }
            }
            if (details.milestones.isEmpty()) {
                NexusCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Nenhum marco intermediário cadastrado.",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary
                    )
                }
            } else {
                details.milestones.forEach { milestone ->
                    NexusCard(modifier = Modifier.fillMaxWidth(), elevated = true) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = milestone.isCompleted,
                                onCheckedChange = { onToggleMilestone(milestone) }
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = milestone.title,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        textDecoration = if (milestone.isCompleted) TextDecoration.LineThrough else null
                                    ),
                                    color = if (milestone.isCompleted) palette.textSecondary else palette.textPrimary
                                )
                                if (milestone.dueAt != null) {
                                    Text(
                                        text = "Prazo ${DateTimeUtils.formatDate(milestone.dueAt)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = palette.textSecondary
                                    )
                                }
                            }
                            IconButton(onClick = {
                                editingMilestone = milestone
                                showMilestoneEditor = true
                            }) {
                                Icon(Icons.Outlined.Edit, contentDescription = "Editar marco", tint = palette.textSecondary)
                            }
                            IconButton(onClick = { onDeleteMilestone(milestone.id) }) {
                                Icon(Icons.Outlined.DeleteOutline, contentDescription = "Remover marco", tint = palette.danger)
                            }
                        }
                    }
                }
            }

            // Tarefas vinculadas (sem duplicar registros)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tarefas vinculadas (${details.linkedTasks.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.textPrimary
                )
                TextButton(onClick = { showTaskPicker = true }) {
                    Text("+ Vincular tarefa", color = palette.info)
                }
            }
            if (details.linkedTasks.isEmpty()) {
                NexusCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Nenhuma tarefa vinculada. Conecte tarefas reais sem duplicá-las.",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary
                    )
                }
            } else {
                details.linkedTasks.forEach { taskItem ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            TaskRowCard(
                                item = taskItem,
                                onClick = { onOpenTask(taskItem.task.id) },
                                onToggleComplete = { onToggleTaskComplete(taskItem.task.id) }
                            )
                        }
                        IconButton(onClick = { onUnlinkTask(taskItem.task.id) }) {
                            Icon(
                                Icons.Outlined.LinkOff,
                                contentDescription = "Desvincular tarefa",
                                tint = palette.textSecondary
                            )
                        }
                    }
                }
            }

            // Projetos vinculados (sem duplicar registros)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Projetos vinculados (${details.linkedProjects.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.textPrimary
                )
                TextButton(onClick = { showProjectPicker = true }) {
                    Text("+ Vincular projeto", color = palette.info)
                }
            }
            if (details.linkedProjects.isEmpty()) {
                NexusCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Nenhum projeto vinculado.",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary
                    )
                }
            } else {
                details.linkedProjects.forEach { pw ->
                    NexusCard(
                        modifier = Modifier.fillMaxWidth(),
                        elevated = true,
                        onClick = { onOpenProject(pw.project.id) }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.FolderOpen, contentDescription = null, tint = palette.highlight)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(pw.project.name, style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
                                Text(
                                    text = "${pw.project.status.label} · ${pw.completedTasks}/${pw.totalTasks} tarefas (${pw.progressPercent}%)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.textSecondary
                                )
                            }
                            IconButton(onClick = { onUnlinkProject(pw.project.id) }) {
                                Icon(
                                    Icons.Outlined.LinkOff,
                                    contentDescription = "Desvincular projeto",
                                    tint = palette.textSecondary
                                )
                            }
                        }
                    }
                }
            }

            // Historico de evolucao
            if (details.progressLogs.isNotEmpty()) {
                SectionHeader(
                    title = "Histórico de evolução",
                    trailing = "${details.progressLogs.size}"
                )
                details.progressLogs.take(10).forEach { log ->
                    NexusCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(log.note, style = MaterialTheme.typography.bodyMedium, color = palette.textPrimary)
                            Text(
                                text = "${log.progressPercent}%",
                                style = MaterialTheme.typography.labelMedium,
                                color = palette.highlight
                            )
                        }
                        Text(
                            text = DateTimeUtils.formatDateTime(log.recordedAt),
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.textSecondary
                        )
                    }
                }
            }

            if (goal.notes.isNotBlank()) {
                NexusCard(modifier = Modifier.fillMaxWidth()) {
                    Text("Observações", style = MaterialTheme.typography.labelMedium, color = palette.textSecondary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(goal.notes, style = MaterialTheme.typography.bodyMedium, color = palette.textPrimary)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showDeleteConfirm) {
        ConfirmDangerDialog(
            title = "Excluir objetivo?",
            message = "O objetivo \"${goal.title}\" e seus marcos serão removidos. Tarefas e projetos vinculados serão preservados e apenas desvinculados.",
            confirmLabel = "Excluir objetivo",
            onConfirm = {
                showDeleteConfirm = false
                onDeleteGoal()
            },
            onDismiss = { showDeleteConfirm = false }
        )
    }

    if (showMilestoneEditor) {
        MilestoneEditorDialog(
            existing = editingMilestone,
            onDismiss = {
                showMilestoneEditor = false
                editingMilestone = null
            },
            onSave = { title, desc, dueAt ->
                onSaveMilestone(editingMilestone?.id, title, desc, dueAt)
                showMilestoneEditor = false
                editingMilestone = null
            }
        )
    }

    if (showProgressLogDialog) {
        ProgressLogDialog(
            initialPercent = details.progressPercent,
            isManualMode = goal.progressMode == GoalProgressMode.MANUAL,
            onDismiss = { showProgressLogDialog = false },
            onSave = { pct, note ->
                onAddProgressLog(pct, note)
                showProgressLogDialog = false
            }
        )
    }

    if (showTaskPicker) {
        val availableTasks = allTasks.filter { it.task.goalId != goal.id }
        AlertDialog(
            onDismissRequest = { showTaskPicker = false },
            containerColor = palette.surfaceElevated,
            title = { Text("Vincular tarefa existente", color = palette.textPrimary) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (availableTasks.isEmpty()) {
                        Text(
                            text = "Todas as tarefas cadastradas já estão vinculadas a este objetivo ou nenhuma tarefa existe ainda.",
                            color = palette.textSecondary
                        )
                    } else {
                        availableTasks.forEach { t ->
                            NexusCard(
                                modifier = Modifier.fillMaxWidth(),
                                elevated = true,
                                onClick = {
                                    onLinkTask(t.task.id)
                                    showTaskPicker = false
                                }
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Outlined.Link, contentDescription = null, tint = palette.highlight)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(t.task.title, color = palette.textPrimary, style = MaterialTheme.typography.bodyMedium)
                                        Text(
                                            text = buildString {
                                                append(t.task.status.label)
                                                t.project?.let { append(" · ${it.name}") }
                                                if (t.task.goalId != null) append(" · vinculada a outro objetivo")
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = palette.textSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTaskPicker = false }) {
                    Text("Fechar", color = palette.textPrimary)
                }
            }
        )
    }

    if (showProjectPicker) {
        val availableProjects = allProjects.filter { it.project.goalId != goal.id }
        AlertDialog(
            onDismissRequest = { showProjectPicker = false },
            containerColor = palette.surfaceElevated,
            title = { Text("Vincular projeto existente", color = palette.textPrimary) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (availableProjects.isEmpty()) {
                        Text(
                            text = "Todos os projetos cadastrados já estão vinculados a este objetivo ou nenhum projeto existe ainda.",
                            color = palette.textSecondary
                        )
                    } else {
                        availableProjects.forEach { pw ->
                            NexusCard(
                                modifier = Modifier.fillMaxWidth(),
                                elevated = true,
                                onClick = {
                                    onLinkProject(pw.project.id)
                                    showProjectPicker = false
                                }
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Outlined.Link, contentDescription = null, tint = palette.highlight)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(pw.project.name, color = palette.textPrimary, style = MaterialTheme.typography.bodyMedium)
                                        Text(
                                            text = "${pw.project.status.label} · ${pw.completedTasks}/${pw.totalTasks} tarefas",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = palette.textSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showProjectPicker = false }) {
                    Text("Fechar", color = palette.textPrimary)
                }
            }
        )
    }
}

@Composable
private fun MilestoneEditorDialog(
    existing: GoalMilestoneEntity?,
    onDismiss: () -> Unit,
    onSave: (title: String, description: String, dueAt: Long?) -> Unit
) {
    val palette = LocalNexusPalette.current
    val initialDueDate = remember(existing?.id) {
        existing?.dueAt?.let { DateTimeUtils.toLocalDate(it) }
    }
    var title by remember(existing?.id) { mutableStateOf(existing?.title ?: "") }
    var description by remember(existing?.id) { mutableStateOf(existing?.description ?: "") }
    var dueDate by remember(existing?.id) { mutableStateOf(initialDueDate) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showDiscardConfirm by remember { mutableStateOf(false) }
    var titleError by remember { mutableStateOf(false) }

    val hasUnsavedChanges = title != (existing?.title ?: "") ||
        description != (existing?.description ?: "") ||
        dueDate != initialDueDate

    val requestDismiss: () -> Unit = {
        val action = resolveBackAction(
            BackNavigationState(
                hasOpenEditor = true,
                editorHasUnsavedChanges = hasUnsavedChanges,
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

    AlertDialog(
        onDismissRequest = requestDismiss,
        containerColor = palette.surfaceElevated,
        title = {
            Text(
                text = if (existing == null) "Novo marco" else "Editar marco",
                color = palette.textPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        if (it.isNotBlank()) titleError = false
                    },
                    label = { Text("Título do marco *") },
                    isError = titleError,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descrição") },
                    minLines = 2,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = dueDate?.let { "Prazo: ${DateTimeUtils.formatDate(it)}" } ?: "Sem prazo",
                        color = palette.textPrimary
                    )
                    Row {
                        if (dueDate != null) {
                            TextButton(onClick = { dueDate = null }) {
                                Text("Limpar", color = palette.textSecondary)
                            }
                        }
                        TextButton(onClick = { showDatePicker = true }) {
                            Text(if (dueDate == null) "Definir" else "Alterar", color = palette.info)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isBlank()) {
                        titleError = true
                    } else {
                        onSave(title, description, dueDate?.let { DateTimeUtils.localDateToEndOfDayEpoch(it) })
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.highlight,
                    contentColor = palette.background
                )
            ) {
                Text("Salvar")
            }
        },
        dismissButton = {
            TextButton(onClick = requestDismiss) {
                Text("Cancelar", color = palette.textPrimary)
            }
        }
    )

    if (showDatePicker) {
        NexusDatePickerDialog(
            initialDate = dueDate ?: DateTimeUtils.todayLocalDate(),
            onDateSelected = { dueDate = it },
            onDismiss = { showDatePicker = false }
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

@Composable
private fun ProgressLogDialog(
    initialPercent: Int,
    isManualMode: Boolean,
    onDismiss: () -> Unit,
    onSave: (Int, String) -> Unit
) {
    val palette = LocalNexusPalette.current
    var sliderValue by remember(initialPercent) { mutableFloatStateOf(initialPercent.toFloat()) }
    var note by remember { mutableStateOf("") }
    var showDiscardConfirm by remember { mutableStateOf(false) }

    val hasUnsavedChanges = note.isNotEmpty() || (isManualMode && sliderValue.toInt() != initialPercent)
    val requestDismiss: () -> Unit = {
        val action = resolveBackAction(
            BackNavigationState(
                hasOpenEditor = true,
                editorHasUnsavedChanges = hasUnsavedChanges,
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

    AlertDialog(
        onDismissRequest = requestDismiss,
        containerColor = palette.surfaceElevated,
        title = { Text("Registrar evolução", color = palette.textPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (isManualMode) {
                    Text("Percentual atual: ${sliderValue.toInt()}%", color = palette.textPrimary)
                    Slider(
                        value = sliderValue,
                        onValueChange = { sliderValue = it },
                        valueRange = 0f..100f
                    )
                } else {
                    Text(
                        text = "Progresso automático atual: $initialPercent%. Este registro será salvo no histórico de evolução.",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary
                    )
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Anotação sobre o avanço") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(sliderValue.toInt().coerceIn(0, 100), note) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.highlight,
                    contentColor = palette.background
                )
            ) {
                Text("Registrar")
            }
        },
        dismissButton = {
            TextButton(onClick = requestDismiss) {
                Text("Cancelar", color = palette.textPrimary)
            }
        }
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
