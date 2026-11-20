package com.nexus.app.ui.components.charts

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nexus.app.core.DateTimeUtils
import com.nexus.app.domain.model.AssetCategory
import com.nexus.app.domain.model.MarketAssetCatalog
import com.nexus.app.domain.model.MarketHistoryCalculations
import com.nexus.app.domain.model.MarketHistoryMetrics
import com.nexus.app.domain.model.MarketHistoryPoint
import com.nexus.app.domain.model.MarketHistoryRange
import com.nexus.app.domain.model.MarketHistorySeries
import com.nexus.app.domain.model.MarketHistoryUiState
import com.nexus.app.domain.model.QuoteTrend
import com.nexus.app.ui.components.MarketChangeBadge
import com.nexus.app.ui.components.MarketModalityBadge
import com.nexus.app.ui.components.NexusCard
import com.nexus.app.ui.theme.LocalNexusPalette
import kotlin.math.roundToInt

@Composable
fun MarketHistoryChartCard(
    descriptor: MarketAssetCatalog.AssetDescriptor,
    selectedRange: MarketHistoryRange,
    supportedRanges: List<MarketHistoryRange>,
    historyState: MarketHistoryUiState?,
    onSelectRange: (MarketHistoryRange) -> Unit,
    onRefreshRange: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalNexusPalette.current
    val effectiveState = historyState ?: MarketHistoryUiState(
        assetCode = descriptor.code,
        selectedRange = selectedRange,
        supportedRanges = supportedRanges
    )
    val series = effectiveState.series
    val metrics = effectiveState.metrics
    var inspectedIndex by remember(descriptor.code, selectedRange, series?.fetchedAtEpochMs) {
        mutableStateOf<Int?>(null)
    }

    NexusCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Histórico e Evolução",
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.textPrimary
                )
                val subtitle = when {
                    series != null -> "${series.granularity.label} · ${series.sourceName}"
                    effectiveState.isLoading -> "Consultando série ${selectedRange.label}..."
                    else -> "Intervalo ${selectedRange.label}"
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (series != null) {
                    MarketModalityBadge(label = series.granularity.label)
                }
                IconButton(
                    onClick = onRefreshRange,
                    enabled = !effectiveState.isLoading,
                    modifier = Modifier.size(34.dp)
                ) {
                    if (effectiveState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = palette.highlight
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = "Atualizar histórico ${selectedRange.label}",
                            tint = palette.highlight,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            supportedRanges.forEach { range ->
                val isSelected = range == selectedRange
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelectRange(range) },
                    label = {
                        Text(
                            text = range.label,
                            style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                            maxLines = 1
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = palette.surfaceElevated,
                        selectedLabelColor = palette.highlight,
                        containerColor = palette.surface,
                        labelColor = palette.textSecondary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = palette.border,
                        selectedBorderColor = palette.highlight
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (series != null && metrics != null && series.points.size >= 2) {
            val inspectedPoint = inspectedIndex
                ?.takeIf { it in series.points.indices }
                ?.let { series.points[it] }

            HistorySeriesSummaryRow(
                descriptor = descriptor,
                series = series,
                metrics = metrics,
                selectedRange = selectedRange,
                inspectedPoint = inspectedPoint
            )

            Spacer(modifier = Modifier.height(10.dp))

            HistoryLineChartCanvas(
                descriptor = descriptor,
                series = series,
                metrics = metrics,
                selectedRange = selectedRange,
                inspectedIndex = inspectedIndex,
                onInspectIndexChange = { inspectedIndex = it }
            )

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = palette.border)
            Spacer(modifier = Modifier.height(10.dp))

            HistoryMetricsGrid(
                descriptor = descriptor,
                series = series,
                metrics = metrics
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Série: ${metrics.pointCount} pontos (${series.granularity.detailLabel.lowercase()}) · " +
                    "Atualizado em ${DateTimeUtils.formatDateTime(series.fetchedAtEpochMs)}",
                style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                color = palette.textSecondary
            )

            if (effectiveState.isOffline || effectiveState.lastRefreshFailed || effectiveState.isStale) {
                Spacer(modifier = Modifier.height(6.dp))
                HistoryStatusBanner(state = effectiveState)
            }
        } else {
            HistoryEmptyOrLoadingBox(
                selectedRange = selectedRange,
                state = effectiveState,
                onRetry = onRefreshRange
            )
        }

        if (descriptor.category == AssetCategory.CURRENCY && MarketHistoryRange.D1 !in supportedRanges) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Moedas exibem séries diárias oficiais/de referência (sem feed intradiário 1D).",
                style = MaterialTheme.typography.labelSmall,
                color = palette.textSecondary
            )
        }
    }
}

@Composable
private fun HistorySeriesSummaryRow(
    descriptor: MarketAssetCatalog.AssetDescriptor,
    series: MarketHistorySeries,
    metrics: MarketHistoryMetrics,
    selectedRange: MarketHistoryRange,
    inspectedPoint: MarketHistoryPoint?
) {
    val palette = LocalNexusPalette.current
    val activePrice = inspectedPoint?.price ?: metrics.endPrice
    val formattedActivePrice = MarketHistoryCalculations.formatSeriesValue(
        value = activePrice,
        descriptor = descriptor,
        currencyOverride = series.currency
    )
    val timestampLabel = if (inspectedPoint != null) {
        "Ponto selecionado · ${MarketHistoryCalculations.formatPointTimestamp(inspectedPoint.timestampEpochMs, series.granularity)}"
    } else {
        "${MarketHistoryCalculations.formatPointTimestamp(metrics.startTimestampEpochMs, series.granularity)} → " +
            MarketHistoryCalculations.formatPointTimestamp(metrics.endTimestampEpochMs, series.granularity)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = formattedActivePrice,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontFeatureSettings = "tnum"
                ),
                color = palette.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = timestampLabel,
                style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                color = if (inspectedPoint != null) palette.highlight else palette.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        metrics.changePercent?.let { pct ->
            MarketChangeBadge(
                changePercent = pct,
                trend = metrics.trend,
                periodLabel = selectedRange.label
            )
        }
    }
}

@Composable
private fun HistoryLineChartCanvas(
    descriptor: MarketAssetCatalog.AssetDescriptor,
    series: MarketHistorySeries,
    metrics: MarketHistoryMetrics,
    selectedRange: MarketHistoryRange,
    inspectedIndex: Int?,
    onInspectIndexChange: (Int?) -> Unit
) {
    val palette = LocalNexusPalette.current
    val points = series.points
    val lineColor = when (metrics.trend) {
        QuoteTrend.UP -> palette.positive
        QuoteTrend.DOWN -> palette.danger
        QuoteTrend.FLAT, QuoteTrend.UNKNOWN -> palette.highlight
    }

    val minVal = metrics.minPrice
    val maxVal = metrics.maxPrice
    val rangeVal = (maxVal - minVal).takeIf { it > 1e-9 } ?: (maxVal * 0.01).coerceAtLeast(1e-6)

    val maxLabel = MarketHistoryCalculations.formatSeriesValue(maxVal, descriptor, series.currency)
    val minLabel = MarketHistoryCalculations.formatSeriesValue(minVal, descriptor, series.currency)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Máx $maxLabel",
                style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                color = palette.textSecondary
            )
            Text(
                text = "Toque ou arraste no gráfico",
                style = MaterialTheme.typography.labelSmall,
                color = palette.textSecondary
            )
        }
        Spacer(modifier = Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(176.dp)
                .pointerInput(points, inspectedIndex) {
                    detectTapGestures(
                        onTap = { offset ->
                            val idx = mapXToPointIndex(offset.x, size.width.toFloat(), points.size)
                            onInspectIndexChange(if (inspectedIndex == idx) null else idx)
                        }
                    )
                }
                .pointerInput(points) {
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            onInspectIndexChange(mapXToPointIndex(offset.x, size.width.toFloat(), points.size))
                        },
                        onHorizontalDrag = { change, _ ->
                            onInspectIndexChange(
                                mapXToPointIndex(change.position.x, size.width.toFloat(), points.size)
                            )
                        },
                        onDragEnd = { onInspectIndexChange(null) },
                        onDragCancel = { onInspectIndexChange(null) }
                    )
                }
                .semantics {
                    contentDescription = "Gráfico histórico ${descriptor.displayName} ${selectedRange.label} com ${points.size} pontos"
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val padX = 6.dp.toPx()
                val padY = 10.dp.toPx()
                val usableW = (size.width - padX * 2f).coerceAtLeast(1f)
                val usableH = (size.height - padY * 2f).coerceAtLeast(1f)

                val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                listOf(padY, padY + usableH * 0.5f, padY + usableH).forEach { yLine ->
                    drawLine(
                        color = palette.border.copy(alpha = 0.7f),
                        start = Offset(padX, yLine),
                        end = Offset(size.width - padX, yLine),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = dashEffect
                    )
                }

                val coords = points.mapIndexed { idx, pt ->
                    val x = padX + (idx.toFloat() / (points.size - 1).toFloat()) * usableW
                    val normY = ((pt.price - minVal) / rangeVal).toFloat().coerceIn(0f, 1f)
                    val y = padY + (1f - normY) * usableH
                    Offset(x, y)
                }

                val linePath = Path().apply {
                    coords.forEachIndexed { idx, offset ->
                        if (idx == 0) moveTo(offset.x, offset.y) else lineTo(offset.x, offset.y)
                    }
                }

                val fillPath = Path().apply {
                    addPath(linePath)
                    lineTo(coords.last().x, padY + usableH)
                    lineTo(coords.first().x, padY + usableH)
                    close()
                }

                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(lineColor.copy(alpha = 0.24f), Color.Transparent),
                        startY = padY,
                        endY = padY + usableH
                    )
                )

                drawPath(
                    path = linePath,
                    color = lineColor,
                    style = Stroke(
                        width = 2.2.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )

                val activeIdx = inspectedIndex?.takeIf { it in coords.indices }
                if (activeIdx != null) {
                    val target = coords[activeIdx]
                    drawLine(
                        color = palette.textSecondary.copy(alpha = 0.65f),
                        start = Offset(target.x, padY),
                        end = Offset(target.x, padY + usableH),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = dashEffect
                    )
                    drawCircle(
                        color = palette.surface,
                        radius = 5.5.dp.toPx(),
                        center = target
                    )
                    drawCircle(
                        color = lineColor,
                        radius = 3.8.dp.toPx(),
                        center = target
                    )
                } else {
                    val last = coords.last()
                    drawCircle(
                        color = lineColor,
                        radius = 3.5.dp.toPx(),
                        center = last
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Mín $minLabel",
                style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                color = palette.textSecondary
            )
            val startLabel = MarketHistoryCalculations.formatAxisLabel(
                points.first().timestampEpochMs,
                selectedRange,
                series.granularity
            )
            val midLabel = MarketHistoryCalculations.formatAxisLabel(
                points[points.size / 2].timestampEpochMs,
                selectedRange,
                series.granularity
            )
            val endLabel = MarketHistoryCalculations.formatAxisLabel(
                points.last().timestampEpochMs,
                selectedRange,
                series.granularity
            )
            Text(
                text = "$startLabel · $midLabel · $endLabel",
                style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                color = palette.textSecondary
            )
        }
    }
}

private fun mapXToPointIndex(xPx: Float, widthPx: Float, count: Int): Int {
    if (count <= 1 || widthPx <= 0f) return 0
    val ratio = (xPx / widthPx).coerceIn(0f, 1f)
    return (ratio * (count - 1)).roundToInt().coerceIn(0, count - 1)
}

@Composable
private fun HistoryMetricsGrid(
    descriptor: MarketAssetCatalog.AssetDescriptor,
    series: MarketHistorySeries,
    metrics: MarketHistoryMetrics
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        HistoryStatCell(
            label = "Inicial",
            value = MarketHistoryCalculations.formatSeriesValue(metrics.startPrice, descriptor, series.currency),
            modifier = Modifier.weight(1f)
        )
        HistoryStatCell(
            label = "Mínima",
            value = MarketHistoryCalculations.formatSeriesValue(metrics.minPrice, descriptor, series.currency),
            modifier = Modifier.weight(1f)
        )
        HistoryStatCell(
            label = "Máxima",
            value = MarketHistoryCalculations.formatSeriesValue(metrics.maxPrice, descriptor, series.currency),
            modifier = Modifier.weight(1f)
        )
        HistoryStatCell(
            label = "Final",
            value = MarketHistoryCalculations.formatSeriesValue(metrics.endPrice, descriptor, series.currency),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun HistoryStatCell(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val palette = LocalNexusPalette.current
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = palette.textSecondary
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Medium,
                fontFeatureSettings = "tnum"
            ),
            color = palette.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun HistoryStatusBanner(state: MarketHistoryUiState) {
    val palette = LocalNexusPalette.current
    val message = when {
        state.isOffline -> state.statusMessage ?: "Sem conexão — exibindo histórico em cache"
        state.lastRefreshFailed -> state.statusMessage ?: "Falha ao atualizar histórico — cache preservado"
        state.isStale -> "Cache histórico antigo — toque em atualizar para buscar pontos recentes"
        else -> null
    } ?: return

    val tint = if (state.isOffline || state.lastRefreshFailed) palette.warning else palette.textSecondary
    val icon = if (state.isOffline) Icons.Outlined.CloudOff else Icons.Outlined.Schedule

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = message,
            style = MaterialTheme.typography.labelSmall,
            color = tint,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun HistoryEmptyOrLoadingBox(
    selectedRange: MarketHistoryRange,
    state: MarketHistoryUiState,
    onRetry: () -> Unit
) {
    val palette = LocalNexusPalette.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = palette.surfaceElevated,
        border = BorderStroke(1.dp, palette.border)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 22.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = palette.highlight
                    )
                    Text(
                        text = "Buscando histórico ${selectedRange.label}...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textSecondary
                    )
                } else {
                    Icon(
                        imageVector = if (state.isOffline) Icons.Outlined.CloudOff else Icons.Outlined.ShowChart,
                        contentDescription = null,
                        tint = if (state.isOffline || state.lastRefreshFailed) palette.warning else palette.textSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = state.statusMessage
                            ?: "Série histórica ${selectedRange.label} ainda não armazenada localmente.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textSecondary
                    )
                    OutlinedButton(
                        onClick = onRetry,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, palette.border),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = null,
                            tint = palette.highlight,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Tentar novamente",
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.textPrimary
                        )
                    }
                }
            }
        }
    }
}
