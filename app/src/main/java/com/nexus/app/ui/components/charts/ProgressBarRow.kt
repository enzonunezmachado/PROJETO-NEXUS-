package com.nexus.app.ui.components.charts

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.nexus.app.core.DateTimeUtils
import com.nexus.app.ui.theme.LocalNexusPalette

@Composable
fun ProgressBarRow(
    label: String,
    currentCents: Long,
    targetCents: Long,
    usedPercent: Int,
    exceededCents: Long = (currentCents - targetCents).coerceAtLeast(0L),
    accentColorOverride: Color? = null,
    modifier: Modifier = Modifier
) {
    val palette = LocalNexusPalette.current
    val rawFraction = if (targetCents <= 0L) 0f else (currentCents.toFloat() / targetCents.toFloat()).coerceIn(0f, 1f)
    val animatedFraction by animateFloatAsState(
        targetValue = rawFraction,
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "progress_row_fraction"
    )
    val fillColor = accentColorOverride ?: when {
        usedPercent > 100 -> palette.danger
        usedPercent >= 80 -> palette.warning
        else -> palette.accentNeon
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "$label: ${DateTimeUtils.formatBrl(currentCents)} de ${DateTimeUtils.formatBrl(targetCents)}, $usedPercent%"
            }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                color = palette.textPrimary
            )
            Text(
                text = "$usedPercent%",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontFeatureSettings = "tnum"
                ),
                color = fillColor
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp)
                .clip(RoundedCornerShape(4.dp))
        ) {
            val radius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
            drawRoundRect(
                color = palette.chartTrack,
                size = size,
                cornerRadius = radius
            )
            if (animatedFraction > 0f) {
                drawRoundRect(
                    color = fillColor,
                    size = Size(size.width * animatedFraction, size.height),
                    cornerRadius = radius
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${DateTimeUtils.formatBrl(currentCents)} de ${DateTimeUtils.formatBrl(targetCents)}",
                style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                color = palette.textSecondary
            )
            if (exceededCents > 0L) {
                Text(
                    text = "excedeu ${DateTimeUtils.formatBrl(exceededCents)}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontFeatureSettings = "tnum"
                    ),
                    color = palette.danger
                )
            }
        }
    }
}
