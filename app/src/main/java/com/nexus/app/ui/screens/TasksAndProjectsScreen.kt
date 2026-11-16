package com.nexus.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nexus.app.core.DateTimeUtils
import com.nexus.app.domain.model.ProjectWithProgress
import com.nexus.app.domain.model.TaskPriority
import com.nexus.app.domain.model.TaskStatus
import com.nexus.app.domain.model.TaskWithDetails
import com.nexus.app.domain.rules.AgendaEngine
import com.nexus.app.domain.rules.DeadlineCondition
import com.nexus.app.domain.rules.TaskRules
import com.nexus.app.ui.TasksFilterState
import com.nexus.app.ui.TasksTabMode
import com.nexus.app.ui.components.AlertBannerCard
import com.nexus.app.ui.components.AlertSeverity
import com.nexus.app.ui.components.NexusCard
import com.nexus.app.ui.components.NexusEmptyState
import com.nexus.app.ui.components.SectionHeader
import com.nexus.app.ui.components.TaskRowCard
import com.nexus.app.ui.components.formatContextualDueDate
import com.nexus.app.ui.theme.LocalNexusPalette

@Composable
fun TasksAndProjectsScreen(
    tasks: List<TaskWithDetails>,
    allTasks: List<TaskWithDetails>,
    projects: List<ProjectWithProgress>,
    filter: TasksFilterState,
    onTabChange: (TasksTabMode) -> Unit,
    onStatusFilterChange: (TaskStatus?) -> Unit,
    onPriorityFilterChange: (TaskPriority?) -> Unit,
    onProjectFilterChange: (String?) -> Unit,
    onClearFilters: () -> Unit,
    onOpenTask: (String) -> Unit,
    onToggleTaskComplete: (String) -> Unit,
    onUpdateTaskStatus: (String, TaskStatus) -> Unit,
    onOpenProject: (String) -> Unit,
    onCreateTask: () -> Unit,
    onCreateProject: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalNexusPalette.current
    var showFilterSheet by remember { mutableStateOf(false) }

    val activeFilterCount = remember(filter) {
        var count = 0
        if (filter.statusFilter != null) count++
        if (filter.priorityFilter != null) count++
        if (filter.projectFilterId != null) count++
        count
    }

    val now = DateTimeUtils.nowEpochMillis()
    val today = DateTimeUtils.todayLocalDate()
    val overdueCount = remember(allTasks, now, today) {
        allTasks.count { AgendaEngine.isTaskOverdue(it.task, now, today) }
    }
    val undatedActiveCount = remember(allTasks) {
        allTasks.count { !it.task.status.isTerminal && it.task.dueAt == null }
    }

    if (showFilterSheet) {
        TasksFilterBottomSheet(
            filter = filter,
            projects = projects,
            onStatusFilterChange = onStatusFilterChange,
            onPriorityFilterChange = onPriorityFilterChange,
            onProjectFilterChange = onProjectFilterChange,
            onClearFilters = onClearFilters,
            onDismiss = { showFilterSheet = false }
        )
    }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Tarefas e Projetos",
                style = MaterialTheme.typography.headlineMedium,
                color = palette.textPrimary
            )
            if (filter.tabMode == TasksTabMode.PROJECTS) {
                OutlinedButton(
                    onClick = onCreateProject,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, palette.border),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("Novo projeto", color = palette.textPrimary, style = MaterialTheme.typography.labelLarge)
                }
            } else {
                OutlinedButton(
                    onClick = { showFilterSheet = true },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(
                        1.dp,
                        if (activeFilterCount > 0) palette.highlight else palette.border
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FilterList,
                        contentDescription = "Filtros",
                        modifier = Modifier.size(16.dp),
                        tint = if (activeFilterCount > 0) palette.highlight else palette.textPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (activeFilterCount > 0) "Filtros ($activeFilterCount)" else "Filtros",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (activeFilterCount > 0) palette.highlight else palette.textPrimary
                    )
                }
            }
        }

        ScrollableTabRow(
            selectedTabIndex = TasksTabMode.entries.indexOf(filter.tabMode),
            containerColor = palette.background,
            contentColor = palette.highlight,
            edgePadding = 16.dp
        ) {
            TasksTabMode.entries.forEach { tab ->
                val tabLabel = if (tab == TasksTabMode.OVERDUE && overdueCount > 0) {
                    "${tab.label} ($overdueCount)"
                } else {
                    tab.label
                }
                Tab(
                    selected = filter.tabMode == tab,
                    onClick = { onTabChange(tab) },
                    text = { Text(tabLabel, maxLines = 1) }
                )
            }
        }

        if (filter.tabMode == TasksTabMode.PROJECTS) {
            ProjectsListContent(
                projects = projects,
                onOpenProject = onOpenProject,
                onCreateProject = onCreateProject
            )
        } else if (filter.tabMode == TasksTabMode.KANBAN) {
            KanbanBoardContent(
                tasks = tasks,
                onOpenTask = onOpenTask,
                onUpdateTaskStatus = onUpdateTaskStatus
            )
        } else {
            val groupedSections = remember(tasks, filter.tabMode, now, today) {
                if (filter.tabMode == TasksTabMode.UPCOMING || filter.tabMode == TasksTabMode.ALL) {
                    TaskRules.groupTasks(tasks, now, today)
                } else {
                    emptyList()
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (filter.tabMode == TasksTabMode.TODAY && overdueCount > 0) {
                    item(key = "today_overdue_banner") {
                        AlertBannerCard(
                            severity = AlertSeverity.DANGER,
                            title = "$overdueCount ${if (overdueCount == 1) "tarefa atrasada" else "tarefas atrasadas"}",
                            subtitle = "Toque para revisar em Atrasadas sem misturar com as de hoje.",
                            onClick = { onTabChange(TasksTabMode.OVERDUE) }
                        )
                    }
                }

                if (tasks.isEmpty()) {
                    item(key = "tasks_empty_state") {
                        val (title, subtitle) = when (filter.tabMode) {
                            TasksTabMode.TODAY -> "Nenhuma tarefa agendada para hoje" to
                                if (undatedActiveCount > 0) {
                                    "Você tem $undatedActiveCount ${if (undatedActiveCount == 1) "tarefa sem prazo" else "tarefas sem prazo"} em Todas."
                                } else {
                                    "Crie uma nova tarefa ou acompanhe Próximas e Todas."
                                }
                            TasksTabMode.UPCOMING -> "Nenhuma tarefa nos próximos 30 dias" to
                                "Tarefas com prazo futuro próximo aparecerão agrupadas aqui."
                            TasksTabMode.OVERDUE -> "Nenhuma tarefa atrasada" to
                                "Você não possui tarefas pendentes com prazo vencido."
                            TasksTabMode.COMPLETED -> "Nenhuma tarefa concluída" to
                                "As tarefas concluídas aparecerão aqui para consulta."
                            TasksTabMode.ALL, TasksTabMode.KANBAN, TasksTabMode.PROJECTS ->
                                "Nenhuma tarefa encontrada" to "Crie uma tarefa pelo botão + ou limpe os filtros ativos."
                        }
                        NexusEmptyState(
                            icon = Icons.Outlined.TaskAlt,
                            title = title,
                            subtitle = subtitle,
                            actionLabel = when {
                                activeFilterCount > 0 -> "Limpar filtros"
                                filter.tabMode == TasksTabMode.TODAY && undatedActiveCount > 0 -> "Ver todas"
                                filter.tabMode == TasksTabMode.TODAY || filter.tabMode == TasksTabMode.ALL -> "Nova tarefa"
                                else -> null
                            },
                            onAction = when {
                                activeFilterCount > 0 -> onClearFilters
                                filter.tabMode == TasksTabMode.TODAY && undatedActiveCount > 0 -> {
                                    { onTabChange(TasksTabMode.ALL) }
                                }
                                filter.tabMode == TasksTabMode.TODAY || filter.tabMode == TasksTabMode.ALL -> onCreateTask
                                else -> null
                            }
                        )
                    }
                } else if (filter.tabMode == TasksTabMode.UPCOMING || filter.tabMode == TasksTabMode.ALL) {
                    groupedSections.forEach { section ->
                        item(key = "section_header_${section.group.name}") {
                            SectionHeader(
                                title = "${section.group.label} (${section.items.size})",
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                        items(section.items, key = { it.task.id }) { item ->
                            TaskRowCard(
                                item = item,
                                onClick = { onOpenTask(item.task.id) },
                                onToggleComplete = { onToggleTaskComplete(item.task.id) }
                            )
                        }
                    }
                } else {
                    val orderedTasks = if (filter.tabMode == TasksTabMode.TODAY) {
                        tasks.sortedWith(
                            compareBy<TaskWithDetails>(
                                { it.task.status == TaskStatus.COMPLETED },
                                { it.task.dueAt ?: Long.MAX_VALUE },
                                { -it.task.priority.sortRank }
                            )
                        )
                    } else {
                        tasks
                    }
                    items(orderedTasks, key = { it.task.id }) { item ->
                        TaskRowCard(
                            item = item,
                            onClick = { onOpenTask(item.task.id) },
                            onToggleComplete = { onToggleTaskComplete(item.task.id) }
                        )
                    }

                    if (filter.tabMode == TasksTabMode.TODAY && undatedActiveCount > 0) {
                        item(key = "today_undated_hint") {
                            Surface(
                                onClick = { onTabChange(TasksTabMode.ALL) },
                                shape = RoundedCornerShape(10.dp),
                                color = palette.surfaceElevated,
                                border = BorderStroke(1.dp, palette.border),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "$undatedActiveCount ${if (undatedActiveCount == 1) "tarefa sem prazo" else "tarefas sem prazo"} no backlog",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = palette.textSecondary
                                    )
                                    Text(
                                        text = "Ver Todas",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = palette.highlight
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun TasksFilterBottomSheet(
    filter: TasksFilterState,
    projects: List<ProjectWithProgress>,
    onStatusFilterChange: (TaskStatus?) -> Unit,
    onPriorityFilterChange: (TaskPriority?) -> Unit,
    onProjectFilterChange: (String?) -> Unit,
    onClearFilters: () -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalNexusPalette.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = palette.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Filtrar tarefas",
                    style = MaterialTheme.typography.titleLarge,
                    color = palette.textPrimary
                )
                TextButton(onClick = onClearFilters) {
                    Text("Limpar filtros", color = palette.textSecondary)
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Status de execução",
                    style = MaterialTheme.typography.labelLarge,
                    color = palette.textSecondary
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = filter.statusFilter == null,
                        onClick = { onStatusFilterChange(null) },
                        label = { Text("Todos") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = palette.surfaceElevated,
                            selectedLabelColor = palette.highlight
                        )
                    )
                    TaskStatus.entries.forEach { st ->
                        FilterChip(
                            selected = filter.statusFilter == st,
                            onClick = { onStatusFilterChange(if (filter.statusFilter == st) null else st) },
                            label = { Text(st.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = palette.surfaceElevated,
                                selectedLabelColor = palette.highlight
                            )
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Prioridade",
                    style = MaterialTheme.typography.labelLarge,
                    color = palette.textSecondary
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = filter.priorityFilter == null,
                        onClick = { onPriorityFilterChange(null) },
                        label = { Text("Todas") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = palette.surfaceElevated,
                            selectedLabelColor = palette.highlight
                        )
                    )
                    TaskPriority.entries.forEach { pr ->
                        FilterChip(
                            selected = filter.priorityFilter == pr,
                            onClick = { onPriorityFilterChange(if (filter.priorityFilter == pr) null else pr) },
                            label = { Text(pr.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = palette.surfaceElevated,
                                selectedLabelColor = palette.highlight
                            )
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Projeto",
                    style = MaterialTheme.typography.labelLarge,
                    color = palette.textSecondary
                )
                if (projects.isEmpty()) {
                    Text(
                        text = "Nenhum projeto criado.",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary
                    )
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = filter.projectFilterId == null,
                            onClick = { onProjectFilterChange(null) },
                            label = { Text("Todos") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = palette.surfaceElevated,
                                selectedLabelColor = palette.highlight
                            )
                        )
                        projects.forEach { pw ->
                            FilterChip(
                                selected = filter.projectFilterId == pw.project.id,
                                onClick = {
                                    onProjectFilterChange(
                                        if (filter.projectFilterId == pw.project.id) null else pw.project.id
                                    )
                                },
                                label = { Text(pw.project.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = palette.surfaceElevated,
                                    selectedLabelColor = palette.highlight
                                )
                            )
                        }
                    }
                }
            }

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.highlight,
                    contentColor = palette.background
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Aplicar")
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun KanbanBoardContent(
    tasks: List<TaskWithDetails>,
    onOpenTask: (String) -> Unit,
    onUpdateTaskStatus: (String, TaskStatus) -> Unit
) {
    val palette = LocalNexusPalette.current
    LazyRow(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(TaskStatus.entries, key = { it.name }) { status ->
            val columnTasks = tasks.filter { it.task.status == status }
            NexusCard(
                modifier = Modifier
                    .width(280.dp)
                    .fillMaxSize(),
                elevated = false
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = status.label,
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.textPrimary
                    )
                    Text(
                        text = columnTasks.size.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.textSecondary
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                if (columnTasks.isEmpty()) {
                    Text(
                        text = "Nenhuma tarefa nesta coluna.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textSecondary
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(columnTasks, key = { "kanban_${it.task.id}" }) { taskItem ->
                            KanbanTaskCard(
                                item = taskItem,
                                onOpenTask = { onOpenTask(taskItem.task.id) },
                                onMoveStatus = { next -> onUpdateTaskStatus(taskItem.task.id, next) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KanbanTaskCard(
    item: TaskWithDetails,
    onOpenTask: () -> Unit,
    onMoveStatus: (TaskStatus) -> Unit
) {
    val palette = LocalNexusPalette.current
    var showMoveMenu by remember { mutableStateOf(false) }
    val deadlineCond = TaskRules.deadlineCondition(item.task)
    val today = DateTimeUtils.todayLocalDate()

    NexusCard(
        modifier = Modifier.fillMaxWidth(),
        elevated = true,
        onClick = onOpenTask
    ) {
        Text(
            text = item.task.title,
            style = MaterialTheme.typography.titleMedium,
            color = palette.textPrimary
        )
        Spacer(modifier = Modifier.height(4.dp))
        val meta = buildList {
            add(item.task.priority.label)
            item.project?.let { add(it.name) }
            item.task.dueAt?.let { add(formatContextualDueDate(it, item.task.hasDueTime, today)) }
        }.joinToString(" · ")
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (deadlineCond == DeadlineCondition.OVERDUE) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(palette.danger)
                )
                Text(
                    text = "Atrasada",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = palette.danger
                )
            }
            Text(
                text = meta,
                style = MaterialTheme.typography.labelMedium,
                color = palette.textSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Box {
            OutlinedButton(
                onClick = { showMoveMenu = true },
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text("Mover status", style = MaterialTheme.typography.labelMedium, color = palette.textPrimary)
            }
            DropdownMenu(
                expanded = showMoveMenu,
                onDismissRequest = { showMoveMenu = false }
            ) {
                TaskStatus.entries.forEach { target ->
                    if (target != item.task.status) {
                        DropdownMenuItem(
                            text = { Text(target.label) },
                            onClick = {
                                showMoveMenu = false
                                onMoveStatus(target)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProjectsListContent(
    projects: List<ProjectWithProgress>,
    onOpenProject: (String) -> Unit,
    onCreateProject: () -> Unit
) {
    val palette = LocalNexusPalette.current
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (projects.isEmpty()) {
            item {
                NexusEmptyState(
                    icon = Icons.Outlined.FolderOpen,
                    title = "Nenhum projeto criado",
                    subtitle = "Agrupe tarefas em projetos e acompanhe o progresso calculado automaticamente.",
                    actionLabel = "Novo projeto",
                    onAction = onCreateProject
                )
            }
        } else {
            items(projects, key = { it.project.id }) { pw ->
                val project = pw.project
                NexusCard(
                    modifier = Modifier.fillMaxWidth(),
                    elevated = true,
                    onClick = { onOpenProject(project.id) }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = project.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = palette.textPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "${pw.progressPercent}%",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (pw.progressPercent == 100 && pw.totalTasks > 0) palette.positive else palette.textPrimary
                        )
                    }
                    if (project.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = project.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.textSecondary,
                            maxLines = 2
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { pw.progressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = if (pw.progressPercent == 100 && pw.totalTasks > 0) palette.positive else palette.highlight,
                        trackColor = palette.border
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    val meta = buildList {
                        add(project.status.label)
                        add("Prioridade ${project.priority.label}")
                        add("${pw.completedTasks}/${pw.totalTasks} tarefas concluídas")
                        project.deadlineAt?.let { add("Prazo ${DateTimeUtils.formatDate(it)}") }
                    }.joinToString(" · ")
                    Text(
                        text = meta,
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.textSecondary
                    )
                    if (pw.linkedTransactionsCount > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Financeiro (${pw.linkedTransactionsCount}): ${DateTimeUtils.formatBrl(pw.estimatedCostCents)} · Pago: ${DateTimeUtils.formatBrl(pw.paidCostCents)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.info
                        )
                    }
                }
            }
        }
    }
}
