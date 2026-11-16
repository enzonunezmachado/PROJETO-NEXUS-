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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nexus.app.core.DateTimeUtils
import com.nexus.app.ui.theme.LocalNexusPalette

data class BarGroupEntry(
    val id: String,
    val label: String,
    val primaryValueCents: Long,
    val secondaryValueCents: Long
)

@Composable
fun BarGroupChart(
    entries: List<BarGroupEntry>,
    primaryLabel: String = "Receitas",
    secondaryLabel: String = "Despesas",
    selectedEntryId: String?,
    onSelectEntry: (BarGroupEntry) -> Unit,
    emptyMessage: String = "Sem movimentação suficiente no período.",
    modifier: Modifier = Modifier
) {
    val palette = LocalNexusPalette.current
    val hasAnyValue = entries.any { it.primaryValueCents > 0L || it.secondaryValueCents > 0L }

    if (entries.isEmpty() || !hasAnyValue) {
        Text(
            text = emptyMessage,
            style = MaterialTheme.typography.bodyMedium,
            color = palette.textSecondary,
            modifier = modifier.padding(vertical = 8.dp)
        )
        return
    }

    val heightScale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
        label = "bar_group_height"
    )
    val maxValue = entries.maxOf { maxOf(it.primaryValueCents, it.secondaryValueCents) }.coerceAtLeast(1L)
    val selectedEntry = selectedEntryId?.let { id -> entries.firstOrNull { it.id == id } } ?: entries.lastOrNull()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = entries.joinToString("; ") {
                    "${it.label}: $primaryLabel ${DateTimeUtils.formatBrl(it.primaryValueCents)}, $secondaryLabel ${DateTimeUtils.formatBrl(it.secondaryValueCents)}"
                }
            },
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(palette.accentNeon))
                Spacer(modifier = Modifier.width(6.dp))
                Text(primaryLabel, style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(palette.chartSecondary))
                Spacer(modifier = Modifier.width(6.dp))
                Text(secondaryLabel, style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(116.dp)
        ) {
            val count = entries.size.coerceAtLeast(1)
            val groupWidth = size.width / count
            val barWidth = (groupWidth * 0.28f).coerceIn(8f, 22f)
            val gapBetweenBars = 4f
            val baseline = size.height - 6f
            val maxBarHeight = baseline - 8f

            // Subtle mid grid line
            drawLine(
                color = palette.border.copy(alpha = 0.55f),
                start = Offset(0f, baseline - maxBarHeight / 2f),
                end = Offset(size.width, baseline - maxBarHeight / 2f),
                strokeWidth = 1f
            )
            // Baseline
            drawLine(
                color = palette.border,
                start = Offset(0f, baseline),
                end = Offset(size.width, baseline),
                strokeWidth = 2f
            )

            val corner = CornerRadius(4.dp.toPx(), 4.dp.toPx())
            entries.forEachIndexed { index, entry ->
                val isSelected = selectedEntry?.id == entry.id
                val centerX = groupWidth * index + groupWidth / 2f

                if (isSelected) {
                    drawRoundRect(
                        color = palette.surfaceElevated.copy(alpha = 0.85f),
                        topLeft = Offset(groupWidth * index + 3f, 2f),
                        size = Size(groupWidth - 6f, baseline - 2f),
                        cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                    )
                }

                val pHeight = (entry.primaryValueCents.toFloat() / maxValue.toFloat()) * maxBarHeight * heightScale
                val sHeight = (entry.secondaryValueCents.toFloat() / maxValue.toFloat()) * maxBarHeight * heightScale

                if (pHeight > 0f) {
                    drawRoundRect(
                        color = palette.accentNeon,
                        topLeft = Offset(centerX - barWidth - gapBetweenBars / 2f, baseline - pHeight),
                        size = Size(barWidth, pHeight.coerceAtLeast(4f)),
                        cornerRadius = corner
                    )
                }
                if (sHeight > 0f) {
                    drawRoundRect(
                        color = palette.chartSecondary,
                        topLeft = Offset(centerX + gapBetweenBars / 2f, baseline - sHeight),
                        size = Size(barWidth, sHeight.coerceAtLeast(4f)),
                        cornerRadius = corner
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            entries.forEach { entry ->
                val isSelected = selectedEntry?.id == entry.id
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onSelectEntry(entry) }
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = entry.label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            fontFeatureSettings = "tnum"
                        ),
                        color = if (isSelected) palette.accentNeon else palette.textSecondary,
                        maxLines = 1
                    )
                }
            }
        }

        selectedEntry?.let { entry ->
            val resultCents = entry.primaryValueCents - entry.secondaryValueCents
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = palette.surfaceElevated
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = entry.label.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = palette.textPrimary
                    )
                    Text(
                        text = "+${DateTimeUtils.formatBrl(entry.primaryValueCents)}",
                        style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                        color = palette.positive
                    )
                    Text(
                        text = "-${DateTimeUtils.formatBrl(entry.secondaryValueCents)}",
                        style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                        color = palette.textPrimary
                    )
                    Text(
                        text = "Saldo: ${DateTimeUtils.formatBrl(resultCents)}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontFeatureSettings = "tnum"
                        ),
                        color = if (resultCents >= 0L) palette.positive else palette.danger
                    )
                }
            }
        }
    }
}
