package com.nexus.app.ui.components.charts

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nexus.app.core.DateTimeUtils
import com.nexus.app.ui.theme.LocalNexusPalette
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.roundToInt

data class DonutSlice(
    val id: String?,
    val label: String,
    val valueCents: Long,
    val color: Color,
    val percentage: Float
)

@Composable
fun DonutChart(
    slices: List<DonutSlice>,
    totalCents: Long,
    centerLabel: String,
    selectedSliceId: String?,
    onSliceClick: (String?) -> Unit,
    emptyMessage: String = "Sem lançamentos neste mês",
    modifier: Modifier = Modifier
) {
    val palette = LocalNexusPalette.current
    if (slices.isEmpty() || totalCents <= 0L) {
        Text(
            text = emptyMessage,
            style = MaterialTheme.typography.bodyMedium,
            color = palette.textSecondary,
            modifier = modifier.padding(vertical = 8.dp)
        )
        return
    }

    val sweepProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
        label = "donut_sweep"
    )

    val gapDeg = if (slices.size > 1) 2.6f else 0f
    val availableSweep = 360f - gapDeg * slices.size
    val sliceRanges = buildList {
        var cursor = 0f
        slices.forEach { slice ->
            val sweep = ((slice.valueCents.toFloat() / totalCents.toFloat()) * availableSweep).coerceAtLeast(1.5f)
            add(Triple(slice, cursor, cursor + sweep))
            cursor += sweep + gapDeg
        }
    }

    val selectedSlice = selectedSliceId?.let { id -> slices.firstOrNull { it.id == id } }
    val semanticText = slices.joinToString("; ") {
        "${it.label}, ${DateTimeUtils.formatBrl(it.valueCents)}, ${it.percentage.roundToInt()}%"
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = semanticText },
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier.size(164.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier
                        .size(164.dp)
                        .pointerInput(sliceRanges, selectedSliceId) {
                            detectTapGestures { offset ->
                                val cx = size.width / 2f
                                val cy = size.height / 2f
                                val dx = offset.x - cx
                                val dy = offset.y - cy
                                val dist = hypot(dx, dy)
                                val outerR = size.minDimension / 2f
                                val innerR = outerR * 0.45f
                                if (dist < innerR || dist > outerR) return@detectTapGestures

                                var angleDeg = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat() + 90f
                                if (angleDeg < 0f) angleDeg += 360f
                                val hit = sliceRanges.firstOrNull { (_, start, end) ->
                                    angleDeg >= start && angleDeg <= end
                                }?.first
                                if (hit != null) {
                                    val next = if (selectedSliceId == hit.id) null else hit.id
                                    onSliceClick(next)
                                }
                            }
                        }
                ) {
                    val baseStroke = 20.dp.toPx()
                    val selectedBoost = 4.dp.toPx()
                    val maxStroke = baseStroke + selectedBoost
                    val diameter = size.minDimension - maxStroke
                    val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
                    val arcSize = Size(diameter, diameter)

                    drawArc(
                        color = palette.chartTrack,
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = baseStroke)
                    )

                    sliceRanges.forEach { (slice, startOffset, endOffset) ->
                        val isSelected = selectedSliceId != null && selectedSliceId == slice.id
                        val isMuted = selectedSliceId != null && !isSelected
                        val strokeWidth = if (isSelected) baseStroke + selectedBoost else baseStroke
                        val drawColor = if (isMuted) slice.color.copy(alpha = 0.42f) else slice.color
                        val sweep = (endOffset - startOffset) * sweepProgress

                        drawArc(
                            color = drawColor,
                            startAngle = -90f + startOffset,
                            sweepAngle = sweep,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(
                                width = strokeWidth,
                                cap = if (slices.size == 1) StrokeCap.Butt else StrokeCap.Round
                            )
                        )
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 18.dp)
                ) {
                    Text(
                        text = selectedSlice?.label ?: centerLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = DateTimeUtils.formatBrl(selectedSlice?.valueCents ?: totalCents),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontFeatureSettings = "tnum"
                        ),
                        color = palette.textPrimary,
                        maxLines = 1
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            slices.forEach { slice ->
                val isSelected = selectedSliceId != null && selectedSliceId == slice.id
                val isMuted = selectedSliceId != null && !isSelected
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) palette.surfaceElevated else Color.Transparent)
                        .clickable {
                            val next = if (selectedSliceId == slice.id) null else slice.id
                            onSliceClick(next)
                        }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(if (isMuted) slice.color.copy(alpha = 0.45f) else slice.color)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = slice.label,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            ),
                            color = if (isMuted) palette.textSecondary else palette.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${DateTimeUtils.formatBrl(slice.valueCents)} (${slice.percentage.roundToInt()}%)",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                            fontFeatureSettings = "tnum"
                        ),
                        color = if (isSelected) palette.accentNeon else palette.textSecondary
                    )
                }
            }
        }
    }
}
