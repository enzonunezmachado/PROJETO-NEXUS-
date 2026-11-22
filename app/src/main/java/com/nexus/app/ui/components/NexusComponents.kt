package com.nexus.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nexus.app.core.DateTimeUtils
import com.nexus.app.data.repository.NexusRepository
import com.nexus.app.domain.model.TaskPriority
import com.nexus.app.domain.model.TaskStatus
import com.nexus.app.domain.model.TaskWithDetails
import com.nexus.app.domain.rules.RecurrenceEngine
import com.nexus.app.ui.theme.ColoredBorderRole
import com.nexus.app.ui.theme.LocalNexusPalette
import com.nexus.app.ui.theme.coloredBorder
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Composable
fun NexusCard(
    modifier: Modifier = Modifier,
    elevated: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val palette = LocalNexusPalette.current
    val shape = RoundedCornerShape(14.dp)
    val bgColor = if (elevated) palette.surfaceElevated else palette.surface
    val cardModifier = if (onClick != null) {
        modifier.clip(shape).clickable(onClick = onClick)
    } else {
        modifier
    }

    Surface(
        modifier = cardModifier,
        shape = shape,
        color = bgColor,
        border = BorderStroke(1.dp, palette.border),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

@Composable
fun SectionHeader(
    title: String,
    trailing: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val palette = LocalNexusPalette.current
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = palette.textPrimary
            )
            if (trailing != null) {
                Text(
                    text = trailing,
                    style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                    color = palette.textSecondary
                )
            }
        }
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction) {
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.info
                )
            }
        }
    }
}

private fun buildNexusN1Path(): Path = Path().apply {
    // N-1 Monolito Chanfrado (coordenadas exatas do viewBox 0 0 318 64)
    moveTo(12f, 54f)
    lineTo(12f, 17f)
    lineTo(19f, 10f)
    lineTo(24.8f, 10f)
    lineTo(44.8f, 38.6f)
    lineTo(46.5f, 38.6f)
    lineTo(46.5f, 13.5f)
    lineTo(56f, 10f)
    lineTo(56f, 47f)
    lineTo(49f, 54f)
    lineTo(43.2f, 54f)
    lineTo(23.2f, 25.4f)
    lineTo(21.5f, 25.4f)
    lineTo(21.5f, 50.5f)
    close()
}

private fun buildNexusWordmarkPath(): Path = Path().apply {
    // N-1 (Monolito Chanfrado)
    addPath(buildNexusN1Path())
    // E (Kinetik Cut)
    addRect(Rect(74f, 10f, 83.5f, 54f))
    addRect(Rect(87f, 10f, 118f, 18.5f))
    moveTo(87f, 27.8f)
    lineTo(112f, 27.8f)
    lineTo(108.5f, 36.2f)
    lineTo(87f, 36.2f)
    close()
    addRect(Rect(87f, 45.5f, 118f, 54f))
    // X (Kinetik Cut)
    moveTo(136f, 10f)
    lineTo(147.5f, 10f)
    lineTo(182f, 54f)
    lineTo(170.5f, 54f)
    close()
    moveTo(170.5f, 10f)
    lineTo(182f, 10f)
    lineTo(166.9f, 29.1f)
    lineTo(161.2f, 21.8f)
    close()
    moveTo(156.8f, 42.2f)
    lineTo(151.1f, 34.9f)
    lineTo(136f, 54f)
    lineTo(147.5f, 54f)
    close()
    // U (Kinetik Cut)
    moveTo(200f, 10f)
    lineTo(209.5f, 10f)
    lineTo(209.5f, 40.5f)
    quadraticTo(209.5f, 45.5f, 214.5f, 45.5f)
    lineTo(229.5f, 45.5f)
    quadraticTo(234.5f, 45.5f, 234.5f, 40.5f)
    lineTo(234.5f, 10f)
    lineTo(244f, 10f)
    lineTo(244f, 42f)
    quadraticTo(244f, 54f, 232f, 54f)
    lineTo(212f, 54f)
    quadraticTo(200f, 54f, 200f, 42f)
    close()
    // S (Kinetik Cut)
    moveTo(306f, 10f)
    lineTo(274f, 10f)
    quadraticTo(262f, 10f, 262f, 22f)
    lineTo(262f, 25.5f)
    quadraticTo(262f, 36f, 274f, 36f)
    lineTo(292.5f, 36f)
    quadraticTo(296.5f, 36f, 296.5f, 40f)
    lineTo(296.5f, 41.5f)
    quadraticTo(296.5f, 45.5f, 292.5f, 45.5f)
    lineTo(265.5f, 45.5f)
    lineTo(262f, 54f)
    lineTo(294f, 54f)
    quadraticTo(306f, 54f, 306f, 42f)
    lineTo(306f, 38.5f)
    quadraticTo(306f, 28f, 294f, 28f)
    lineTo(275.5f, 28f)
    quadraticTo(271.5f, 28f, 271.5f, 24f)
    lineTo(271.5f, 22.5f)
    quadraticTo(271.5f, 18.5f, 275.5f, 18.5f)
    lineTo(302.5f, 18.5f)
    close()
}

private val NexusN1VectorPath = buildNexusN1Path()
private val NexusWordmarkVectorPath = buildNexusWordmarkPath()

@Composable
fun NexusWordmark(
    modifier: Modifier = Modifier,
    height: Dp = 18.dp,
    color: Color = LocalNexusPalette.current.highlight
) {
    val width = height * (318f / 64f)
    Canvas(
        modifier = modifier
            .width(width)
            .height(height)
            .semantics { contentDescription = "NEXUS" }
    ) {
        withTransform({
            scale(scaleX = size.width / 318f, scaleY = size.height / 64f, pivot = Offset.Zero)
        }) {
            drawPath(path = NexusWordmarkVectorPath, color = color)
        }
    }
}

@Composable
fun NexusMonogramIcon(
    size: Dp = 20.dp,
    tint: Color = LocalNexusPalette.current.highlight,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .size(size)
            .semantics { contentDescription = "NEXUS" }
    ) {
        withTransform({
            translate(left = -6f * (size.width / 56f), top = -4f * (size.height / 56f))
            scale(scaleX = size.width / 56f, scaleY = size.height / 56f, pivot = Offset.Zero)
        }) {
            drawPath(path = NexusN1VectorPath, color = tint)
        }
    }
}

@Composable
fun MonthYearSelectorBar(
    currentMonth: YearMonth,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onResetToCurrentMonth: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val palette = LocalNexusPalette.current
    val formatter = DateTimeFormatter.ofPattern("MMMM 'de' yyyy", DateTimeUtils.LOCALE_PT_BR)
    val label = currentMonth.atDay(1).format(formatter)
        .replaceFirstChar { it.titlecase(DateTimeUtils.LOCALE_PT_BR) }
    val isCurrent = currentMonth == YearMonth.from(DateTimeUtils.todayLocalDate())

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = palette.surfaceElevated,
        border = BorderStroke(1.dp, palette.border)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPreviousMonth) {
                Icon(
                    imageVector = Icons.Outlined.ChevronLeft,
                    contentDescription = "Mês anterior",
                    tint = palette.textPrimary
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = if (!isCurrent && onResetToCurrentMonth != null) {
                    Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onResetToCurrentMonth)
                } else {
                    Modifier
                }
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                    color = palette.textPrimary
                )
                if (!isCurrent && onResetToCurrentMonth != null) {
                    Text(
                        text = "Hoje",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.accentNeon
                    )
                }
            }
            IconButton(onClick = onNextMonth) {
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = "Próximo mês",
                    tint = palette.textPrimary
                )
            }
        }
    }
}

enum class AlertSeverity {
    INFO,
    WARNING,
    DANGER
}

@Composable
fun AlertBannerCard(
    title: String,
    subtitle: String,
    severity: AlertSeverity,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val palette = LocalNexusPalette.current
    val accent = when (severity) {
        AlertSeverity.INFO -> palette.info
        AlertSeverity.WARNING -> palette.warning
        AlertSeverity.DANGER -> palette.danger
    }
    val icon = when (severity) {
        AlertSeverity.INFO -> Icons.Outlined.Info
        AlertSeverity.WARNING -> Icons.Outlined.WarningAmber
        AlertSeverity.DANGER -> Icons.Outlined.ErrorOutline
    }
    val shape = RoundedCornerShape(12.dp)
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clip(shape).clickable(onClick = onClick) else Modifier),
        shape = shape,
        color = palette.surfaceElevated,
        border = palette.coloredBorder(accent, ColoredBorderRole.ALERT_BANNER_LARGE)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(18.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = palette.textPrimary
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun NexusEmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val palette = LocalNexusPalette.current
    NexusCard(
        modifier = modifier.fillMaxWidth(),
        elevated = false
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = palette.textSecondary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = palette.textPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = palette.textSecondary
            )
            if (actionLabel != null && onAction != null) {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = onAction,
                    border = BorderStroke(1.dp, palette.border),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(text = actionLabel, color = palette.textPrimary)
                }
            }
        }
    }
}

@Composable
fun TaskRowCard(
    item: TaskWithDetails,
    onClick: () -> Unit,
    onToggleComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalNexusPalette.current
    val task = item.task
    val isCompleted = task.status == TaskStatus.COMPLETED
    val isOverdue = NexusRepository.isTaskOverdue(task)
    val today = DateTimeUtils.todayLocalDate()

    NexusCard(
        modifier = modifier.fillMaxWidth(),
        elevated = true,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            IconButton(
                onClick = onToggleComplete,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = if (isCompleted) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                    contentDescription = if (isCompleted) "Reabrir tarefa" else "Concluir tarefa",
                    tint = if (isCompleted) palette.positive else palette.textSecondary
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None
                    ),
                    color = if (isCompleted) palette.textSecondary else palette.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                val activeRecurrence = item.recurrence?.takeIf { it.isActive }
                val metaParts = buildList {
                    add(task.status.label)
                    item.project?.let { add(it.name) }
                    task.dueAt?.let { due ->
                        add(formatContextualDueDate(due, task.hasDueTime, today))
                    }
                    activeRecurrence?.let { rec ->
                        add(RecurrenceEngine.formatRecurrenceSummary(rec))
                    }
                    if (item.subtasks.isNotEmpty()) {
                        add("${item.completedSubtasksCount}/${item.subtasks.size} subtarefas")
                    }
                    if (item.totalExpenseCents > 0L) {
                        add("Gasto: ${DateTimeUtils.formatBrl(item.totalExpenseCents)}")
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isOverdue) {
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
                    } else {
                        PriorityIndicatorDot(priority = task.priority)
                    }
                    if (activeRecurrence != null) {
                        Icon(
                            imageVector = Icons.Outlined.Repeat,
                            contentDescription = "Tarefa recorrente",
                            tint = palette.accentNeon,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Text(
                        text = metaParts.joinToString(" · "),
                        style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                        color = palette.textSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

fun formatContextualDueDate(
    dueAt: Long,
    hasDueTime: Boolean,
    today: LocalDate = DateTimeUtils.todayLocalDate()
): String {
    val dueDate = DateTimeUtils.toLocalDate(dueAt)
    val dateLabel = when (dueDate) {
        today -> "Hoje"
        today.plusDays(1) -> "Amanhã"
        today.minusDays(1) -> "Ontem"
        else -> if (dueDate.year == today.year) {
            DateTimeUtils.formatDayMonth(dueDate)
        } else {
            DateTimeUtils.formatDate(dueDate)
        }
    }
    return if (hasDueTime) "$dateLabel, às ${DateTimeUtils.formatTime(dueAt)}" else dateLabel
}

@Composable
fun PriorityIndicatorDot(priority: TaskPriority) {
    val palette = LocalNexusPalette.current
    val color = when (priority) {
        TaskPriority.LOW -> palette.textSecondary
        TaskPriority.NORMAL -> palette.textPrimary
        TaskPriority.HIGH -> palette.warning
        TaskPriority.URGENT -> palette.danger
    }
    Box(
        modifier = Modifier
            .size(7.dp)
            .clip(CircleShape)
            .background(color)
    )
}

fun parseHexColorOrDefault(hex: String?, fallback: Color): Color {
    if (hex.isNullOrBlank()) return fallback
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (_: Throwable) {
        fallback
    }
}

@Composable
fun ConfirmDangerDialog(
    title: String,
    message: String,
    confirmLabel: String = "Excluir",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalNexusPalette.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.surfaceElevated,
        title = {
            Text(text = title, color = palette.textPrimary, fontWeight = FontWeight.SemiBold)
        },
        text = {
            Text(text = message, color = palette.textSecondary)
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.danger,
                    contentColor = Color.White
                )
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = palette.textPrimary)
            }
        }
    )
}

@Composable
fun ConfirmDiscardChangesDialog(
    onConfirmDiscard: () -> Unit,
    onContinueEditing: () -> Unit
) {
    val palette = LocalNexusPalette.current
    AlertDialog(
        onDismissRequest = onContinueEditing,
        containerColor = palette.surfaceElevated,
        title = {
            Text(
                text = "Descartar alterações?",
                color = palette.textPrimary,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Text(
                text = "Existem alterações não salvas neste formulário. Deseja sair sem salvar?",
                color = palette.textSecondary
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirmDiscard,
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.danger,
                    contentColor = Color.White
                )
            ) {
                Text("Descartar")
            }
        },
        dismissButton = {
            TextButton(onClick = onContinueEditing) {
                Text("Continuar", color = palette.textPrimary)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NexusDatePickerDialog(
    initialDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    val initialMillis = DateTimeUtils.epochToDatePickerMillis(DateTimeUtils.startOfDayEpoch(initialDate))
    val state = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val millis = state.selectedDateMillis
                    if (millis != null) {
                        val pickedEpoch = DateTimeUtils.datePickerMillisToEpoch(millis, keepTimeFrom = null)
                        onDateSelected(DateTimeUtils.toLocalDate(pickedEpoch))
                    }
                    onDismiss()
                }
            ) {
                Text("Confirmar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    ) {
        DatePicker(state = state)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NexusTimePickerDialog(
    initialTime: LocalTime,
    onTimeSelected: (LocalTime) -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalNexusPalette.current
    val state = rememberTimePickerState(
        initialHour = initialTime.hour,
        initialMinute = initialTime.minute,
        is24Hour = true
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.surfaceElevated,
        title = { Text("Selecionar horário (24h)", color = palette.textPrimary) },
        text = {
            TimePicker(state = state)
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onTimeSelected(LocalTime.of(state.hour, state.minute))
                    onDismiss()
                }
            ) {
                Text("Confirmar", color = palette.highlight)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = palette.textSecondary)
            }
        }
    )
}

@Composable
fun ReminderLeadTimeSelector(
    selectedMinutes: Int?,
    onSelectMinutes: (Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalNexusPalette.current
    val options: List<Pair<Int?, String>> = listOf(
        null to "Padrão",
        0 to "Na hora",
        15 to "15 min",
        30 to "30 min",
        60 to "1 hora",
        -1 to "Sem lembrete"
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Lembrete local",
            style = MaterialTheme.typography.titleMedium,
            color = palette.textPrimary
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            options.take(3).forEach { (value, label) ->
                val isSelected = selectedMinutes == value
                OutlinedButton(
                    onClick = { onSelectMinutes(value) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, if (isSelected) palette.highlight else palette.border)
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isSelected) palette.highlight else palette.textSecondary,
                        maxLines = 1
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            options.drop(3).forEach { (value, label) ->
                val isSelected = selectedMinutes == value
                OutlinedButton(
                    onClick = { onSelectMinutes(value) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, if (isSelected) palette.highlight else palette.border)
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isSelected) palette.highlight else palette.textSecondary,
                        maxLines = 1
                    )
                }
            }
        }
        val presetValues = setOf<Int?>(null, 0, 15, 30, 60, -1)
        if (selectedMinutes != null && selectedMinutes !in presetValues && selectedMinutes > 0) {
            Spacer(modifier = Modifier.height(6.dp))
            val customLabel = when {
                selectedMinutes % 1440 == 0 -> "${selectedMinutes / 1440} dia(s) antes"
                selectedMinutes % 60 == 0 -> "${selectedMinutes / 60} h antes"
                else -> "$selectedMinutes min antes"
            }
            OutlinedButton(
                onClick = { onSelectMinutes(selectedMinutes) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, palette.highlight)
            ) {
                Text(
                    text = "Configurado: $customLabel",
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.highlight
                )
            }
        }
    }
}

