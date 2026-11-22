package com.nexus.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ConfirmationNumber
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nexus.app.core.DateTimeUtils
import com.nexus.app.data.db.entity.CategoryEntity
import com.nexus.app.domain.rules.AgendaEngine
import com.nexus.app.domain.rules.AgendaOccurrence
import com.nexus.app.domain.rules.AgendaOccurrenceKind
import com.nexus.app.domain.rules.AgendaSourceType
import com.nexus.app.ui.AgendaFilterState
import com.nexus.app.ui.AgendaViewMode
import com.nexus.app.ui.components.NexusCard
import com.nexus.app.ui.components.NexusEmptyState
import com.nexus.app.ui.components.parseHexColorOrDefault
import com.nexus.app.ui.theme.ColoredBorderRole
import com.nexus.app.ui.theme.LocalNexusPalette
import com.nexus.app.ui.theme.coloredBorder
import com.nexus.app.ui.theme.coloredBorderColor
import com.nexus.app.ui.theme.coloredBorderWidth
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AgendaScreen(
    occurrences: List<AgendaOccurrence>,
    categories: List<CategoryEntity>,
    filter: AgendaFilterState,
    onViewModeChange: (AgendaViewMode) -> Unit,
    onDateChange: (LocalDate) -> Unit,
    onKindFilterChange: (AgendaOccurrenceKind?) -> Unit,
    onCategoryFilterChange: (String?) -> Unit,
    onShowPaidChange: (Boolean) -> Unit,
    onClearFilters: () -> Unit,
    onOpenAppointment: (String) -> Unit,
    onOpenTask: (String) -> Unit,
    onOpenEvent: (String) -> Unit,
    onOpenTransaction: (String) -> Unit,
    onOpenCards: () -> Unit,
    onOpenProject: (String) -> Unit,
    onCreateAppointment: () -> Unit,
    onOpenVirtualRecurringTask: (recurrenceId: String, occurrenceDateIso: String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val palette = LocalNexusPalette.current
    var isCalendarExpanded by remember { mutableStateOf(true) }
    var showFilterSheet by remember { mutableStateOf(false) }

    val openOccurrence: (AgendaOccurrence) -> Unit = { occ ->
        when (occ.sourceType) {
            AgendaSourceType.APPOINTMENT -> onOpenAppointment(occ.sourceId)
            AgendaSourceType.TASK -> {
                if (occ.isVirtualRecurrence && occ.recurrenceId != null) {
                    onOpenVirtualRecurringTask(occ.recurrenceId, occ.occurrenceDateIso)
                } else {
                    onOpenTask(occ.sourceId)
                }
            }
            AgendaSourceType.EVENT -> onOpenEvent(occ.sourceId)
            AgendaSourceType.TRANSACTION -> onOpenTransaction(occ.sourceId)
            AgendaSourceType.STATEMENT -> onOpenCards()
            AgendaSourceType.PROJECT -> onOpenProject(occ.sourceId)
        }
    }

    val weekStart = filter.selectedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val weekDays = (0L..6L).map { weekStart.plusDays(it) }
    val weekEnd = weekDays.last()

    val displayedOccurrences = when (filter.viewMode) {
        AgendaViewMode.DAY, AgendaViewMode.MONTH ->
            occurrences.filter { it.occurrenceDate == filter.selectedDate }
        AgendaViewMode.WEEK ->
            occurrences.filter { it.occurrenceDate in weekStart..weekEnd }
        AgendaViewMode.LIST -> occurrences
    }

    val activeFilterCount = listOfNotNull(
        filter.kindFilter,
        filter.categoryFilterId,
        filter.showPaid.takeIf { it }
    ).size

    val conflictsInView = displayedOccurrences
        .filter { it.sourceType == AgendaSourceType.APPOINTMENT && it.hasConflict }
        .distinctBy { it.sourceId }

    val selectedDayItems = occurrences.filter { it.occurrenceDate == filter.selectedDate }
    val dayBands = selectedDayItems.filter { it.kind == AgendaOccurrenceKind.EVENT && it.isAllDay }
    val dayRegularItems = selectedDayItems.filterNot { it.kind == AgendaOccurrenceKind.EVENT && it.isAllDay }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val useTabletTwoPane = maxWidth >= 840.dp && maxHeight >= 480.dp && maxWidth > maxHeight
        val weekCardHeight = if (useTabletTwoPane) {
            (maxHeight - 204.dp).coerceIn(240.dp, 560.dp)
        } else {
            (maxHeight - 224.dp).coerceIn(220.dp, 540.dp)
        }

        if (useTabletTwoPane) {
            Column(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AgendaHeaderRow(
                        viewMode = filter.viewMode,
                        isCalendarExpanded = isCalendarExpanded,
                        activeFilterCount = activeFilterCount,
                        onToggleExpand = { isCalendarExpanded = !isCalendarExpanded },
                        onOpenFilters = { showFilterSheet = true }
                    )
                    AgendaViewModeSelector(
                        selectedMode = filter.viewMode,
                        onViewModeChange = onViewModeChange
                    )
                }

                if (filter.viewMode == AgendaViewMode.LIST) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (conflictsInView.isNotEmpty()) {
                            item(key = "agenda_conflicts_list_wide") {
                                AgendaConflictNoticeCard(conflictCount = conflictsInView.size)
                            }
                        }
                        if (displayedOccurrences.isEmpty()) {
                            item(key = "agenda_list_empty_wide") {
                                NexusEmptyState(
                                    icon = Icons.Outlined.CalendarMonth,
                                    title = "Nenhum item encontrado",
                                    subtitle = "Ajuste os filtros ou registre um compromisso, tarefa com data, evento ou lançamento.",
                                    actionLabel = "Novo compromisso",
                                    onAction = onCreateAppointment
                                )
                            }
                        } else {
                            val pairs = displayedOccurrences.chunked(2)
                            items(pairs, key = { "list_wide_${it.first().stableKey}" }) { pair ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        AgendaOccurrenceRow(
                                            occurrence = pair[0],
                                            showDateInMeta = true,
                                            onClick = { openOccurrence(pair[0]) }
                                        )
                                    }
                                    if (pair.size > 1) {
                                        Box(modifier = Modifier.weight(1f)) {
                                            AgendaOccurrenceRow(
                                                occurrence = pair[1],
                                                showDateInMeta = true,
                                                onClick = { openOccurrence(pair[1]) }
                                            )
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                } else {
                    val leftWeight = if (filter.viewMode == AgendaViewMode.WEEK) 0.54f else 0.45f
                    val rightWeight = 1f - leftWeight

                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        LazyColumn(
                            modifier = Modifier
                                .weight(leftWeight)
                                .fillMaxSize(),
                            contentPadding = PaddingValues(top = 4.dp, bottom = 96.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            item(key = "agenda_two_pane_date_nav") {
                                DateNavigationBar(
                                    viewMode = filter.viewMode,
                                    selectedDate = filter.selectedDate,
                                    isCollapsed = (filter.viewMode == AgendaViewMode.MONTH || filter.viewMode == AgendaViewMode.WEEK) && !isCalendarExpanded,
                                    onToggleExpand = { isCalendarExpanded = !isCalendarExpanded },
                                    onDateChange = onDateChange
                                )
                            }

                            if (conflictsInView.isNotEmpty()) {
                                item(key = "agenda_two_pane_conflicts") {
                                    AgendaConflictNoticeCard(conflictCount = conflictsInView.size)
                                }
                            }

                            when (filter.viewMode) {
                                AgendaViewMode.WEEK -> {
                                    if (isCalendarExpanded) {
                                        item(key = "agenda_two_pane_week") {
                                            HorizontalWeekBoard(
                                                weekDays = weekDays,
                                                selectedDate = filter.selectedDate,
                                                occurrences = displayedOccurrences,
                                                cardHeight = weekCardHeight,
                                                onSelectDay = onDateChange,
                                                onOpenOccurrence = openOccurrence
                                            )
                                        }
                                    }
                                }
                                AgendaViewMode.MONTH, AgendaViewMode.DAY -> {
                                    if (filter.viewMode == AgendaViewMode.DAY || isCalendarExpanded) {
                                        item(key = "agenda_two_pane_month") {
                                            MonthCalendarGrid(
                                                selectedDate = filter.selectedDate,
                                                occurrences = occurrences,
                                                onDateSelected = onDateChange
                                            )
                                        }
                                    }
                                }
                                AgendaViewMode.LIST -> Unit
                            }
                        }

                        LazyColumn(
                            modifier = Modifier
                                .weight(rightWeight)
                                .fillMaxSize(),
                            contentPadding = PaddingValues(top = 4.dp, bottom = 96.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            item(key = "agenda_two_pane_day_header") {
                                NexusCard(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = DateTimeUtils.formatFullDateWithWeekday(filter.selectedDate),
                                                style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                                                color = palette.textPrimary
                                            )
                                            Text(
                                                text = "${selectedDayItems.size} ${if (selectedDayItems.size == 1) "item" else "itens"} neste dia",
                                                style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                                                color = palette.textSecondary
                                            )
                                        }
                                        TextButton(onClick = onCreateAppointment) {
                                            Text("Novo", color = palette.info)
                                        }
                                    }
                                }
                            }

                            if (selectedDayItems.isEmpty()) {
                                item(key = "agenda_two_pane_day_empty") {
                                    NexusEmptyState(
                                        icon = Icons.Outlined.CalendarMonth,
                                        title = "Nada marcado para ${DateTimeUtils.formatDayMonth(filter.selectedDate)}",
                                        subtitle = "Os compromissos, tarefas com data, eventos e vencimentos deste dia aparecem aqui.",
                                        actionLabel = "Novo compromisso",
                                        onAction = onCreateAppointment
                                    )
                                }
                            } else {
                                if (dayBands.isNotEmpty()) {
                                    items(dayBands, key = { "tp_band_${it.stableKey}" }) { band ->
                                        AllDayEventBandCard(
                                            occurrence = band,
                                            onClick = { openOccurrence(band) }
                                        )
                                    }
                                }
                                items(dayRegularItems, key = { "tp_${it.stableKey}" }) { occ ->
                                    AgendaOccurrenceRow(
                                        occurrence = occ,
                                        showDateInMeta = false,
                                        onClick = { openOccurrence(occ) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item(key = "agenda_header") {
                    AgendaHeaderRow(
                        viewMode = filter.viewMode,
                        isCalendarExpanded = isCalendarExpanded,
                        activeFilterCount = activeFilterCount,
                        onToggleExpand = { isCalendarExpanded = !isCalendarExpanded },
                        onOpenFilters = { showFilterSheet = true }
                    )
                }

                item(key = "agenda_view_modes") {
                    AgendaViewModeSelector(
                        selectedMode = filter.viewMode,
                        onViewModeChange = onViewModeChange
                    )
                }

                if (filter.viewMode != AgendaViewMode.LIST) {
                    item(key = "agenda_date_nav") {
                        DateNavigationBar(
                            viewMode = filter.viewMode,
                            selectedDate = filter.selectedDate,
                            isCollapsed = (filter.viewMode == AgendaViewMode.MONTH || filter.viewMode == AgendaViewMode.WEEK) && !isCalendarExpanded,
                            onToggleExpand = { isCalendarExpanded = !isCalendarExpanded },
                            onDateChange = onDateChange
                        )
                    }
                }

                if (filter.viewMode == AgendaViewMode.MONTH && isCalendarExpanded) {
                    item(key = "agenda_month_grid") {
                        MonthCalendarGrid(
                            selectedDate = filter.selectedDate,
                            occurrences = occurrences,
                            onDateSelected = onDateChange
                        )
                    }
                }

                if (conflictsInView.isNotEmpty()) {
                    item(key = "agenda_conflicts") {
                        AgendaConflictNoticeCard(conflictCount = conflictsInView.size)
                    }
                }

                when (filter.viewMode) {
                    AgendaViewMode.DAY, AgendaViewMode.MONTH -> {
                        if (selectedDayItems.isEmpty()) {
                            item(key = "agenda_day_empty") {
                                NexusEmptyState(
                                    icon = Icons.Outlined.CalendarMonth,
                                    title = "Nada marcado para ${DateTimeUtils.formatDayMonth(filter.selectedDate)}",
                                    subtitle = "Os compromissos, tarefas com data, eventos e vencimentos deste dia aparecem aqui.",
                                    actionLabel = "Novo compromisso",
                                    onAction = onCreateAppointment
                                )
                            }
                        } else {
                            if (dayBands.isNotEmpty()) {
                                items(dayBands, key = { "band_${it.stableKey}" }) { band ->
                                    AllDayEventBandCard(
                                        occurrence = band,
                                        onClick = { openOccurrence(band) }
                                    )
                                }
                            }
                            items(dayRegularItems, key = { it.stableKey }) { occ ->
                                AgendaOccurrenceRow(
                                    occurrence = occ,
                                    showDateInMeta = false,
                                    onClick = { openOccurrence(occ) }
                                )
                            }
                        }
                    }

                    AgendaViewMode.WEEK -> {
                        if (isCalendarExpanded) {
                            item(key = "agenda_week_board") {
                                HorizontalWeekBoard(
                                    weekDays = weekDays,
                                    selectedDate = filter.selectedDate,
                                    occurrences = displayedOccurrences,
                                    cardHeight = weekCardHeight,
                                    onSelectDay = onDateChange,
                                    onOpenOccurrence = openOccurrence
                                )
                            }
                        } else {
                            // Quando a faixa semanal horizontal é recolhida, foca diretamente nos itens do dia selecionado sem duplicar cards.
                            if (selectedDayItems.isEmpty()) {
                                item(key = "agenda_week_collapsed_empty") {
                                    NexusEmptyState(
                                        icon = Icons.Outlined.CalendarMonth,
                                        title = "Nada marcado para ${DateTimeUtils.formatDayMonth(filter.selectedDate)}",
                                        subtitle = "Expanda a semana acima para ver todos os dias ou adicione um novo compromisso.",
                                        actionLabel = "Novo compromisso",
                                        onAction = onCreateAppointment
                                    )
                                }
                            } else {
                                if (dayBands.isNotEmpty()) {
                                    items(dayBands, key = { "wcband_${it.stableKey}" }) { band ->
                                        AllDayEventBandCard(
                                            occurrence = band,
                                            onClick = { openOccurrence(band) }
                                        )
                                    }
                                }
                                items(dayRegularItems, key = { "wc_${it.stableKey}" }) { occ ->
                                    AgendaOccurrenceRow(
                                        occurrence = occ,
                                        showDateInMeta = false,
                                        onClick = { openOccurrence(occ) }
                                    )
                                }
                            }
                        }
                    }

                    AgendaViewMode.LIST -> {
                        if (displayedOccurrences.isEmpty()) {
                            item(key = "agenda_list_empty") {
                                NexusEmptyState(
                                    icon = Icons.Outlined.CalendarMonth,
                                    title = "Nenhum item encontrado",
                                    subtitle = "Ajuste os filtros ou registre um compromisso, tarefa com data, evento ou lançamento.",
                                    actionLabel = "Novo compromisso",
                                    onAction = onCreateAppointment
                                )
                            }
                        } else {
                            items(displayedOccurrences, key = { "list_${it.stableKey}" }) { occ ->
                                AgendaOccurrenceRow(
                                    occurrence = occ,
                                    showDateInMeta = true,
                                    onClick = { openOccurrence(occ) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showFilterSheet) {
        AgendaFilterBottomSheet(
            filter = filter,
            categories = categories,
            onKindFilterChange = onKindFilterChange,
            onCategoryFilterChange = onCategoryFilterChange,
            onShowPaidChange = onShowPaidChange,
            onClearFilters = onClearFilters,
            onDismiss = { showFilterSheet = false }
        )
    }
}

@Composable
private fun AgendaHeaderRow(
    viewMode: AgendaViewMode,
    isCalendarExpanded: Boolean,
    activeFilterCount: Int,
    onToggleExpand: () -> Unit,
    onOpenFilters: () -> Unit
) {
    val palette = LocalNexusPalette.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Agenda",
            style = MaterialTheme.typography.headlineMedium,
            color = palette.textPrimary
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (viewMode == AgendaViewMode.MONTH || viewMode == AgendaViewMode.WEEK) {
                IconButton(onClick = onToggleExpand) {
                    Icon(
                        imageVector = if (isCalendarExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                        contentDescription = if (isCalendarExpanded) "Recolher calendário" else "Expandir calendário",
                        tint = palette.textPrimary
                    )
                }
            }
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (activeFilterCount > 0) palette.surfaceElevated else palette.surface,
                border = if (activeFilterCount > 0) {
                    palette.coloredBorder(palette.accentNeon, ColoredBorderRole.MEDIUM_ACCENT)
                } else {
                    BorderStroke(1.dp, palette.border)
                },
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onOpenFilters)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Tune,
                        contentDescription = "Filtrar Agenda",
                        tint = if (activeFilterCount > 0) palette.accentNeon else palette.textPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = if (activeFilterCount > 0) "Filtros ($activeFilterCount)" else "Filtros",
                        style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                        color = if (activeFilterCount > 0) palette.accentNeon else palette.textPrimary
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AgendaViewModeSelector(
    selectedMode: AgendaViewMode,
    onViewModeChange: (AgendaViewMode) -> Unit
) {
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        AgendaViewMode.entries.forEachIndexed { index, mode ->
            SegmentedButton(
                selected = selectedMode == mode,
                onClick = { onViewModeChange(mode) },
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = AgendaViewMode.entries.size
                ),
                label = { Text(mode.label, maxLines = 1) }
            )
        }
    }
}

@Composable
private fun AgendaConflictNoticeCard(conflictCount: Int) {
    val palette = LocalNexusPalette.current
    NexusCard(modifier = Modifier.fillMaxWidth(), elevated = true) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Outlined.WarningAmber,
                contentDescription = "Alerta de conflito",
                tint = palette.warning
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Atenção: $conflictCount compromisso(s) com conflito de horário neste período.",
                style = MaterialTheme.typography.bodyMedium,
                color = palette.warning
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun AgendaFilterBottomSheet(
    filter: AgendaFilterState,
    categories: List<CategoryEntity>,
    onKindFilterChange: (AgendaOccurrenceKind?) -> Unit,
    onCategoryFilterChange: (String?) -> Unit,
    onShowPaidChange: (Boolean) -> Unit,
    onClearFilters: () -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalNexusPalette.current
    val contextualCategories = remember(categories, filter.kindFilter) {
        AgendaEngine.contextualCategoriesForKind(categories, filter.kindFilter)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = palette.surfaceElevated
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Filtrar Agenda",
                    style = MaterialTheme.typography.titleLarge,
                    color = palette.textPrimary
                )
                TextButton(onClick = onClearFilters) {
                    Text("Limpar filtros", color = palette.info)
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Tipo de registro",
                    style = MaterialTheme.typography.titleSmall,
                    color = palette.textPrimary
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = filter.kindFilter == null,
                        onClick = { onKindFilterChange(null) },
                        label = { Text("Tudo") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = palette.surface,
                            selectedLabelColor = palette.accentNeon
                        )
                    )
                    AgendaOccurrenceKind.entries.forEach { kind ->
                        FilterChip(
                            selected = filter.kindFilter == kind,
                            onClick = {
                                onKindFilterChange(if (filter.kindFilter == kind) null else kind)
                            },
                            label = { Text(kind.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = palette.surface,
                                selectedLabelColor = palette.accentNeon
                            )
                        )
                    }
                }
            }

            HorizontalDivider(color = palette.border)

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Categoria",
                    style = MaterialTheme.typography.titleSmall,
                    color = palette.textPrimary
                )
                when {
                    filter.kindFilter == null -> {
                        Text(
                            text = "Selecione um tipo acima para filtrar pelas categorias daquele módulo.",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textSecondary
                        )
                    }
                    contextualCategories.isEmpty() -> {
                        Text(
                            text = "Nenhuma categoria específica disponível para ${filter.kindFilter.label}.",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textSecondary
                        )
                    }
                    else -> {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = filter.categoryFilterId == null,
                                onClick = { onCategoryFilterChange(null) },
                                label = { Text("Todas") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = palette.surface,
                                    selectedLabelColor = palette.accentNeon
                                )
                            )
                            contextualCategories.forEach { cat ->
                                val dotColor = parseHexColorOrDefault(cat.colorHex, palette.textSecondary)
                                FilterChip(
                                    selected = filter.categoryFilterId == cat.id,
                                    onClick = {
                                        onCategoryFilterChange(
                                            if (filter.categoryFilterId == cat.id) null else cat.id
                                        )
                                    },
                                    leadingIcon = {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(dotColor)
                                        )
                                    },
                                    label = { Text(cat.name) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = palette.surface,
                                        selectedLabelColor = palette.accentNeon
                                    )
                                )
                            }
                        }
                    }
                }
            }

            if (filter.kindFilter == null || filter.kindFilter == AgendaOccurrenceKind.FINANCE) {
                HorizontalDivider(color = palette.border)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Mostrar já pagos",
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.textPrimary
                        )
                        Text(
                            text = "Exibe contas e faturas já liquidadas neste período.",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.textSecondary
                        )
                    }
                    Switch(
                        checked = filter.showPaid,
                        onCheckedChange = onShowPaidChange
                    )
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
        }
    }
}

@Composable
private fun HorizontalWeekBoard(
    weekDays: List<LocalDate>,
    selectedDate: LocalDate,
    occurrences: List<AgendaOccurrence>,
    cardHeight: Dp,
    onSelectDay: (LocalDate) -> Unit,
    onOpenOccurrence: (AgendaOccurrence) -> Unit
) {
    val today = DateTimeUtils.todayLocalDate()
    val byDay = remember(occurrences) { occurrences.groupBy { it.occurrenceDate } }
    val listState = rememberLazyListState()
    val selectedIndex = weekDays.indexOf(selectedDate).takeIf { it >= 0 } ?: 0

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val expandAllSeven = maxWidth >= 720.dp
        val scrollableCardWidth = if (maxWidth >= 560.dp) {
            ((maxWidth - 36.dp) / 4f).coerceIn(145.dp, 220.dp)
        } else {
            ((maxWidth - 20.dp) / 2.25f).coerceIn(148.dp, 205.dp)
        }

        LaunchedEffect(selectedDate, expandAllSeven) {
            if (!expandAllSeven) {
                listState.animateScrollToItem(selectedIndex)
            }
        }

        if (expandAllSeven) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                weekDays.forEach { day ->
                    WeekDayBoardCard(
                        day = day,
                        dayItems = byDay[day].orEmpty(),
                        isSelected = day == selectedDate,
                        isToday = day == today,
                        compactPadding = maxWidth < 840.dp,
                        onSelectDay = { onSelectDay(day) },
                        onOpenOccurrence = onOpenOccurrence,
                        modifier = Modifier
                            .weight(1f)
                            .height(cardHeight)
                    )
                }
            }
        } else {
            LazyRow(
                state = listState,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 2.dp)
            ) {
                items(weekDays, key = { it.toString() }) { day ->
                    WeekDayBoardCard(
                        day = day,
                        dayItems = byDay[day].orEmpty(),
                        isSelected = day == selectedDate,
                        isToday = day == today,
                        compactPadding = false,
                        onSelectDay = { onSelectDay(day) },
                        onOpenOccurrence = onOpenOccurrence,
                        modifier = Modifier
                            .width(scrollableCardWidth)
                            .height(cardHeight)
                    )
                }
            }
        }
    }
}

@Composable
private fun WeekDayBoardCard(
    day: LocalDate,
    dayItems: List<AgendaOccurrence>,
    isSelected: Boolean,
    isToday: Boolean,
    compactPadding: Boolean,
    onSelectDay: () -> Unit,
    onOpenOccurrence: (AgendaOccurrence) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalNexusPalette.current
    val weekdayShort = day.dayOfWeek
        .getDisplayName(TextStyle.SHORT, DateTimeUtils.LOCALE_PT_BR)
        .replace(".", "")
        .uppercase(DateTimeUtils.LOCALE_PT_BR)

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = palette.surfaceElevated,
        border = when {
            isSelected -> palette.coloredBorder(palette.accentNeon, ColoredBorderRole.DAY_CARD_SELECTED)
            isToday -> palette.coloredBorder(palette.info, ColoredBorderRole.DAY_CARD_TODAY)
            else -> BorderStroke(1.dp, palette.border)
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (compactPadding) 8.dp else 10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onSelectDay)
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = weekdayShort,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = when {
                            isSelected -> palette.accentNeon
                            isToday -> palette.info
                            else -> palette.textSecondary
                        }
                    )
                    Text(
                        text = DateTimeUtils.formatDayMonth(day),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = if (isSelected || isToday) FontWeight.SemiBold else FontWeight.Medium,
                            fontFeatureSettings = "tnum"
                        ),
                        color = palette.textPrimary
                    )
                }
                if (dayItems.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = palette.surface
                    ) {
                        Text(
                            text = "${dayItems.size}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontFeatureSettings = "tnum"
                            ),
                            color = if (dayItems.any { it.isOverdue }) palette.danger else palette.textSecondary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            HorizontalDivider(color = palette.border, modifier = Modifier.padding(vertical = 6.dp))

            if (dayItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onSelectDay),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Livre",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textSecondary
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    dayItems.forEach { occ ->
                        WeekCardOccurrenceItem(
                            occurrence = occ,
                            onClick = { onOpenOccurrence(occ) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WeekCardOccurrenceItem(
    occurrence: AgendaOccurrence,
    onClick: () -> Unit
) {
    val palette = LocalNexusPalette.current
    val statusColor = resolveOccurrenceStatusColor(occurrence)
    val strike = occurrence.isDoneOrSettled && occurrence.kind == AgendaOccurrenceKind.TASK
    val timeText = when {
        occurrence.kind == AgendaOccurrenceKind.FINANCE && occurrence.amountCents != null ->
            (if (occurrence.isIncome) "+" else "-") + DateTimeUtils.formatBrl(occurrence.amountCents)
        occurrence.isAllDay -> "Dia todo"
        else -> DateTimeUtils.formatTime(occurrence.startAt)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = palette.surface,
        border = if (occurrence.hasConflict) {
            palette.coloredBorder(palette.warning, ColoredBorderRole.COMPACT_ITEM)
        } else {
            BorderStroke(1.dp, palette.border)
        }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .size(7.dp)
                    .clip(if (occurrence.kind == AgendaOccurrenceKind.TASK) RoundedCornerShape(2.dp) else CircleShape)
                    .background(statusColor)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = occurrence.title,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Medium,
                        textDecoration = if (strike) TextDecoration.LineThrough else TextDecoration.None
                    ),
                    color = if (strike) palette.textSecondary else palette.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (occurrence.recurrenceId != null) {
                        Icon(
                            imageVector = Icons.Outlined.Repeat,
                            contentDescription = "Tarefa recorrente",
                            tint = palette.accentNeon,
                            modifier = Modifier.size(11.dp)
                        )
                    }
                    Text(
                        text = timeText,
                        style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                        color = if (occurrence.isOverdue) palette.danger else palette.textSecondary,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun AllDayEventBandCard(
    occurrence: AgendaOccurrence,
    onClick: () -> Unit
) {
    val palette = LocalNexusPalette.current
    val statusColor = resolveOccurrenceStatusColor(occurrence)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = palette.surfaceElevated,
        border = palette.coloredBorder(statusColor, ColoredBorderRole.MEDIUM_ACCENT)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AgendaKindBadge(kind = occurrence.kind, statusColor = statusColor, isDone = occurrence.isDoneOrSettled)
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = occurrence.title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = palette.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Dia inteiro · ${occurrence.subtitle}",
                    style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                    color = palette.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun AgendaOccurrenceRow(
    occurrence: AgendaOccurrence,
    showDateInMeta: Boolean,
    onClick: () -> Unit
) {
    val palette = LocalNexusPalette.current
    val statusColor = resolveOccurrenceStatusColor(occurrence)
    val strike = occurrence.isDoneOrSettled && occurrence.kind == AgendaOccurrenceKind.TASK

    NexusCard(
        modifier = Modifier.fillMaxWidth(),
        elevated = occurrence.kind == AgendaOccurrenceKind.APPOINTMENT || occurrence.kind == AgendaOccurrenceKind.EVENT,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            AgendaKindBadge(
                kind = occurrence.kind,
                statusColor = statusColor,
                isDone = occurrence.isDoneOrSettled
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = occurrence.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            textDecoration = if (strike) TextDecoration.LineThrough else TextDecoration.None
                        ),
                        color = if (strike) palette.textSecondary else palette.textPrimary,
                        modifier = Modifier.weight(1f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    occurrence.amountCents?.let { cents ->
                        if (occurrence.kind == AgendaOccurrenceKind.FINANCE) {
                            Spacer(modifier = Modifier.width(8.dp))
                            val prefix = if (occurrence.isIncome) "+" else "-"
                            Text(
                                text = "$prefix${DateTimeUtils.formatBrl(cents)}",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontFeatureSettings = "tnum"
                                ),
                                color = if (occurrence.isIncome && !occurrence.isOverdue) palette.positive else statusColor
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                val meta = buildString {
                    if (showDateInMeta) {
                        append(DateTimeUtils.formatDate(occurrence.occurrenceDate))
                        append(" · ")
                    }
                    append(occurrence.subtitle)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (occurrence.isOverdue) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(palette.danger)
                        )
                    }
                    if (occurrence.recurrenceId != null) {
                        Icon(
                            imageVector = Icons.Outlined.Repeat,
                            contentDescription = "Tarefa recorrente",
                            tint = palette.accentNeon,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                    Text(
                        text = meta,
                        style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                        color = if (occurrence.isOverdue) palette.danger else palette.textSecondary
                    )
                }
                if (occurrence.hasConflict && occurrence.conflictTitles.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Conflito de horário com: ${occurrence.conflictTitles.joinToString(", ")}",
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.warning
                    )
                }
            }
        }
    }
}

@Composable
private fun resolveOccurrenceStatusColor(occurrence: AgendaOccurrence): Color {
    val palette = LocalNexusPalette.current
    return when {
        occurrence.isOverdue -> palette.danger
        occurrence.hasConflict -> palette.warning
        occurrence.isDoneOrSettled -> palette.positive
        occurrence.kind == AgendaOccurrenceKind.FINANCE -> palette.warning
        else -> palette.textSecondary
    }
}

@Composable
private fun AgendaKindBadge(
    kind: AgendaOccurrenceKind,
    statusColor: Color,
    isDone: Boolean
) {
    val palette = LocalNexusPalette.current
    when (kind) {
        AgendaOccurrenceKind.TASK -> {
            Box(
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(22.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (isDone) statusColor.copy(alpha = 0.16f) else Color.Transparent)
                    .border(
                        palette.coloredBorderWidth(ColoredBorderRole.SMALL_BADGE_DEFINED),
                        palette.coloredBorderColor(statusColor, ColoredBorderRole.SMALL_BADGE_DEFINED),
                        RoundedCornerShape(4.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isDone) {
                    Icon(
                        imageVector = Icons.Outlined.Check,
                        contentDescription = "Tarefa",
                        tint = statusColor,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
        AgendaOccurrenceKind.APPOINTMENT -> {
            Box(
                modifier = Modifier
                    .padding(top = 2.dp, start = 8.dp, end = 8.dp)
                    .width(5.dp)
                    .height(26.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(statusColor)
            )
        }
        AgendaOccurrenceKind.EVENT -> {
            Box(
                modifier = Modifier
                    .padding(top = 1.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(statusColor.copy(alpha = 0.12f))
                    .border(
                        palette.coloredBorderWidth(ColoredBorderRole.SMALL_BADGE_SUBTLE),
                        palette.coloredBorderColor(statusColor, ColoredBorderRole.SMALL_BADGE_SUBTLE),
                        RoundedCornerShape(6.dp)
                    )
                    .padding(horizontal = 5.dp, vertical = 3.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.ConfirmationNumber,
                    contentDescription = "Evento",
                    tint = statusColor,
                    modifier = Modifier.size(15.dp)
                )
            }
        }
        AgendaOccurrenceKind.FINANCE -> {
            Box(
                modifier = Modifier
                    .padding(top = 1.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .border(
                        palette.coloredBorderWidth(ColoredBorderRole.SMALL_BADGE_SUBTLE),
                        palette.coloredBorderColor(statusColor, ColoredBorderRole.SMALL_BADGE_SUBTLE),
                        RoundedCornerShape(6.dp)
                    )
                    .padding(horizontal = 5.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "R$",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = statusColor
                )
            }
        }
        AgendaOccurrenceKind.PROJECT -> {
            Icon(
                imageVector = Icons.Outlined.Flag,
                contentDescription = "Projeto",
                tint = statusColor,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(22.dp)
            )
        }
    }
}

@Composable
private fun DateNavigationBar(
    viewMode: AgendaViewMode,
    selectedDate: LocalDate,
    isCollapsed: Boolean,
    onToggleExpand: () -> Unit,
    onDateChange: (LocalDate) -> Unit
) {
    val palette = LocalNexusPalette.current
    val label = when (viewMode) {
        AgendaViewMode.DAY -> DateTimeUtils.formatFullDateWithWeekday(selectedDate)
        AgendaViewMode.WEEK -> {
            val start = selectedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val end = start.plusDays(6)
            "Semana ${DateTimeUtils.formatDayMonth(start)} – ${DateTimeUtils.formatDayMonth(end)}"
        }
        AgendaViewMode.MONTH -> DateTimeUtils.formatMonthYear(selectedDate)
        AgendaViewMode.LIST -> "Cronológica"
    }

    NexusCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = {
                    val prev = when (viewMode) {
                        AgendaViewMode.DAY -> selectedDate.minusDays(1)
                        AgendaViewMode.WEEK -> selectedDate.minusWeeks(1)
                        AgendaViewMode.MONTH -> selectedDate.minusMonths(1)
                        AgendaViewMode.LIST -> selectedDate
                    }
                    onDateChange(prev)
                }
            ) {
                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, contentDescription = "Anterior")
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .clickable {
                        if (isCollapsed) onToggleExpand() else onDateChange(DateTimeUtils.todayLocalDate())
                    }
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                    color = palette.textPrimary
                )
                when {
                    isCollapsed -> {
                        Text(
                            text = "Dia ${DateTimeUtils.formatDayMonth(selectedDate)} · Toque para expandir",
                            style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                            color = palette.accentNeon
                        )
                    }
                    selectedDate != DateTimeUtils.todayLocalDate() -> {
                        Text(
                            text = "Toque para voltar a hoje",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.info
                        )
                    }
                }
            }

            IconButton(
                onClick = {
                    val next = when (viewMode) {
                        AgendaViewMode.DAY -> selectedDate.plusDays(1)
                        AgendaViewMode.WEEK -> selectedDate.plusWeeks(1)
                        AgendaViewMode.MONTH -> selectedDate.plusMonths(1)
                        AgendaViewMode.LIST -> selectedDate
                    }
                    onDateChange(next)
                }
            ) {
                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = "Próximo")
            }
        }
    }
}

@Composable
private fun MonthCalendarGrid(
    selectedDate: LocalDate,
    occurrences: List<AgendaOccurrence>,
    onDateSelected: (LocalDate) -> Unit
) {
    val palette = LocalNexusPalette.current
    val yearMonth = YearMonth.from(selectedDate)
    val firstDay = yearMonth.atDay(1)
    val offset = (firstDay.dayOfWeek.value - 1)
    val daysInMonth = yearMonth.lengthOfMonth()
    val datesWithItems = remember(occurrences) { occurrences.map { it.occurrenceDate }.toSet() }
    val overdueDates = remember(occurrences) {
        occurrences.filter { it.isOverdue }.map { it.occurrenceDate }.toSet()
    }
    val today = DateTimeUtils.todayLocalDate()
    val weekHeaders = listOf("S", "T", "Q", "Q", "S", "S", "D")

    NexusCard(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth()) {
            weekHeaders.forEach { dayName ->
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = dayName,
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textSecondary
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        val totalCells = offset + daysInMonth
        val rows = (totalCells + 6) / 7
        for (row in 0 until rows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (col in 0 until 7) {
                    val cellIndex = row * 7 + col
                    val dayNumber = cellIndex - offset + 1
                    if (dayNumber in 1..daysInMonth) {
                        val cellDate = yearMonth.atDay(dayNumber)
                        val isSelected = cellDate == selectedDate
                        val isToday = cellDate == today
                        val hasEvents = cellDate in datesWithItems
                        val hasOverdue = cellDate in overdueDates

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onDateSelected(cellDate) },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) palette.highlight else palette.surface,
                            border = if (isToday && !isSelected) {
                                palette.coloredBorder(palette.accentNeon, ColoredBorderRole.COMPACT_ITEM)
                            } else {
                                null
                            }
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = dayNumber.toString(),
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (isSelected || isToday) FontWeight.SemiBold else FontWeight.Normal,
                                        fontFeatureSettings = "tnum"
                                    ),
                                    color = if (isSelected) palette.background else palette.textPrimary
                                )
                                if (hasEvents) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(4.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when {
                                                    isSelected -> palette.background
                                                    hasOverdue -> palette.danger
                                                    else -> palette.accentNeon
                                                }
                                            )
                                    )
                                }
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}
