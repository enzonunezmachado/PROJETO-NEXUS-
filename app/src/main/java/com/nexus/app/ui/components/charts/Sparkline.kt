package com.nexus.app.ui.components.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nexus.app.ui.theme.LocalNexusPalette

@Composable
fun Sparkline(
    points: List<Double>,
    isPositive: Boolean,
    height: Dp = 36.dp,
    emptyText: String = "Histórico aguardando pontos",
    modifier: Modifier = Modifier
) {
    val palette = LocalNexusPalette.current
    val strokeColor = if (isPositive) palette.positive else palette.danger

    if (points.size < 2) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(height)
                .semantics { contentDescription = emptyText },
            contentAlignment = Alignment.CenterStart
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val y = size.height / 2f
                drawLine(
                    color = palette.border,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 2f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                )
            }
            Text(
                text = emptyText,
                style = MaterialTheme.typography.labelSmall,
                color = palette.textSecondary
            )
        }
        return
    }

    val minVal = points.minOrNull() ?: 0.0
    val maxVal = points.maxOrNull() ?: 0.0
    val range = (maxVal - minVal).takeIf { it > 1e-9 } ?: 1.0

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .semantics {
                contentDescription = "Tendência com ${points.size} pontos"
            }
    ) {
        val padY = 4.dp.toPx()
        val padX = 4.dp.toPx()
        val usableW = (size.width - padX * 2f).coerceAtLeast(1f)
        val usableH = (size.height - padY * 2f).coerceAtLeast(1f)

        val coords = points.mapIndexed { idx, value ->
            val x = padX + (idx.toFloat() / (points.size - 1).toFloat()) * usableW
            val normY = ((value - minVal) / range).toFloat()
            val y = padY + (1f - normY) * usableH
            Offset(x, y)
        }

        val linePath = Path().apply {
            coords.forEachIndexed { idx, pt ->
                if (idx == 0) moveTo(pt.x, pt.y) else lineTo(pt.x, pt.y)
            }
        }

        val fillPath = Path().apply {
            addPath(linePath)
            lineTo(coords.last().x, size.height)
            lineTo(coords.first().x, size.height)
            close()
        }

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(strokeColor.copy(alpha = 0.22f), Color.Transparent),
                startY = 0f,
                endY = size.height
            )
        )

        drawPath(
            path = linePath,
            color = strokeColor,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        val last = coords.last()
        drawCircle(
            color = strokeColor,
            radius = 3.2.dp.toPx(),
            center = last
        )
    }
}
