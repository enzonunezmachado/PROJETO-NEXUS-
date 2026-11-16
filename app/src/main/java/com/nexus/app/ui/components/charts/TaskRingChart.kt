package com.nexus.app.ui.components.charts

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nexus.app.ui.theme.LocalNexusPalette

enum class TaskRingSliceKind {
    COMPLETED,
    IN_PROGRESS,
    PENDING
}

/**
 * Anel de distribuição mensal de tarefas em exatamente 3 fatias mutuamente exclusivas:
 * Concluídas, Em andamento e Pendentes (excluindo canceladas e tarefas sem prazo).
 * Tarefas atrasadas nunca compõem uma 4a fatia — aparecem como indicador separado abaixo.
 */
@Composable
fun TaskRingChart(
    completedCount: Int,
    inProgressCount: Int,
    pendingCount: Int,
    overdueCount: Int,
    undatedPendingCount: Int = 0,
    periodLabel: String = "Mês atual",
    onSliceClick: ((TaskRingSliceKind) -> Unit)? = null,
    onOverdueClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val palette = LocalNexusPalette.current
    val total = (completedCount + inProgressCount + pendingCount).coerceAtLeast(0)
    val completionPercent = if (total == 0) 0 else ((completedCount * 100f) / total.toFloat()).toInt()
    val progressRatio by animateFloatAsState(
        targetValue = if (total == 0) 0f else 1f,
        animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
        label = "task_ring_sweep"
    )

    val completedColor = palette.accentNeon
    val inProgressColor = palette.info
    val pendingColor = palette.chartSecondary

    val nonZeroSegments = listOf(
        Triple(TaskRingSliceKind.COMPLETED, completedCount, completedColor),
        Triple(TaskRingSliceKind.IN_PROGRESS, inProgressCount, inProgressColor),
        Triple(TaskRingSliceKind.PENDING, pendingCount, pendingColor)
    ).filter { it.second > 0 }

    val semanticSummary = if (total == 0) {
        "Sem tarefas com prazo em $periodLabel. $overdueCount atrasadas."
    } else {
        "$completionPercent% concluídas em $periodLabel: $completedCount concluídas, $inProgressCount em andamento, $pendingCount pendentes, $overdueCount atrasadas."
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = semanticSummary },
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier.size(116.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(116.dp)) {
                    val strokePx = 14.dp.toPx()
                    val diameter = size.minDimension - strokePx
                    val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
                    val arcSize = Size(diameter, diameter)

                    drawArc(
                        color = palette.chartTrack,
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokePx)
                    )

                    if (total > 0 && nonZeroSegments.isNotEmpty()) {
                        val gapDeg = if (nonZeroSegments.size > 1) 4f else 0f
                        val availableSweep = 360f - gapDeg * nonZeroSegments.size
                        var startAngle = -90f

                        nonZeroSegments.forEach { (_, count, color) ->
                            val rawSweep = (count.toFloat() / total.toFloat()) * availableSweep
                            val sweep = (rawSweep * progressRatio).coerceAtLeast(1.5f)
                            drawArc(
                                color = color,
                                startAngle = startAngle,
                                sweepAngle = sweep,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(
                                    width = strokePx,
                                    cap = if (nonZeroSegments.size == 1) StrokeCap.Butt else StrokeCap.Round
                                )
                            )
                            startAngle += rawSweep + gapDeg
                        }
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (total == 0) "0%" else "$completionPercent%",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontFeatureSettings = "tnum"
                        ),
                        color = palette.textPrimary
                    )
                    Text(
                        text = if (total == 0) "Sem prazo" else "$completedCount/$total",
                        style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                        color = palette.textSecondary
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = periodLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.textSecondary
                )
                TaskRingLegendRow(
                    label = "Concluídas",
                    count = completedCount,
                    dotColor = completedColor,
                    onClick = onSliceClick?.let { cb -> { cb(TaskRingSliceKind.COMPLETED) } }
                )
                TaskRingLegendRow(
                    label = "Em andamento",
                    count = inProgressCount,
                    dotColor = inProgressColor,
                    onClick = onSliceClick?.let { cb -> { cb(TaskRingSliceKind.IN_PROGRESS) } }
                )
                TaskRingLegendRow(
                    label = "Pendentes",
                    count = pendingCount,
                    dotColor = pendingColor,
                    onClick = onSliceClick?.let { cb -> { cb(TaskRingSliceKind.PENDING) } }
                )
            }
        }

        HorizontalDivider(color = palette.border)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val overdueRowModifier = if (overdueCount > 0 && onOverdueClick != null) {
                Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onOverdueClick)
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            } else {
                Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            }

            Row(
                modifier = overdueRowModifier,
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(if (overdueCount > 0) palette.danger else palette.textSecondary)
                )
                Text(
                    text = if (overdueCount == 0) {
                        "0 atrasadas"
                    } else if (overdueCount == 1) {
                        "1 tarefa atrasada"
                    } else {
                        "$overdueCount tarefas atrasadas"
                    },
                    style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                    color = if (overdueCount > 0) palette.danger else palette.textSecondary
                )
            }

            if (undatedPendingCount > 0) {
                Text(
                    text = "$undatedPendingCount sem prazo",
                    style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                    color = palette.textSecondary
                )
            }
        }
    }
}

@Composable
private fun TaskRingLegendRow(
    label: String,
    count: Int,
    dotColor: Color,
    onClick: (() -> Unit)?
) {
    val palette = LocalNexusPalette.current
    val rowModifier = if (onClick != null) {
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp, horizontal = 4.dp)
    } else {
        Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp, horizontal = 4.dp)
    }
    Row(
        modifier = rowModifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = palette.textPrimary
            )
        }
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontFeatureSettings = "tnum"
            ),
            color = palette.textPrimary
        )
    }
}
