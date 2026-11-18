package com.nexus.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.TrendingDown
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nexus.app.domain.model.MarketQuoteCalculations
import com.nexus.app.domain.model.MarketQuoteUiState
import com.nexus.app.domain.model.QuoteTrend
import com.nexus.app.ui.theme.LocalNexusPalette

@Composable
fun MarketModalityBadge(
    label: String,
    modifier: Modifier = Modifier
) {
    val palette = LocalNexusPalette.current
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = palette.surfaceElevated,
        border = BorderStroke(1.dp, palette.border)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = palette.textSecondary,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun MarketChangeBadge(
    changePercent: Double,
    trend: QuoteTrend,
    periodLabel: String? = null,
    modifier: Modifier = Modifier
) {
    val palette = LocalNexusPalette.current
    val trendColor = when (trend) {
        QuoteTrend.UP -> palette.positive
        QuoteTrend.DOWN -> palette.danger
        QuoteTrend.FLAT, QuoteTrend.UNKNOWN -> palette.textSecondary
    }
    val trendIcon = when (trend) {
        QuoteTrend.UP -> Icons.Outlined.TrendingUp
        QuoteTrend.DOWN -> Icons.Outlined.TrendingDown
        QuoteTrend.FLAT, QuoteTrend.UNKNOWN -> null
    }
    val pctText = MarketQuoteCalculations.formatSignedPercent(changePercent)
    val fullText = if (!periodLabel.isNullOrBlank()) "$pctText ($periodLabel)" else pctText

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = trendColor.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, trendColor.copy(alpha = 0.35f))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            if (trendIcon != null) {
                Icon(
                    imageVector = trendIcon,
                    contentDescription = when (trend) {
                        QuoteTrend.UP -> "Em alta"
                        QuoteTrend.DOWN -> "Em queda"
                        else -> null
                    },
                    tint = trendColor,
                    modifier = Modifier.size(14.dp)
                )
            }
            Text(
                text = fullText,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontFeatureSettings = "tnum"
                ),
                color = trendColor
            )
        }
    }
}

@Composable
fun MarketStatusBannerRow(
    state: MarketQuoteUiState,
    modifier: Modifier = Modifier
) {
    val palette = LocalNexusPalette.current
    if (state.quote == null) return
    val badgeMessage = when {
        state.isOffline -> state.statusMessage ?: "Offline • Exibindo último cache válido"
        state.lastRefreshFailed -> state.statusMessage ?: "Falha na atualização • Exibindo último cache válido"
        state.isStale -> "Cache desatualizado • Toque para atualizar"
        else -> null
    } ?: return

    Spacer(modifier = Modifier.size(6.dp))
    val statusTint = if (state.isOffline || state.lastRefreshFailed) {
        palette.warning
    } else {
        palette.textSecondary
    }
    val statusIcon = if (state.isOffline) Icons.Outlined.CloudOff else Icons.Outlined.Schedule
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(
            imageVector = statusIcon,
            contentDescription = null,
            tint = statusTint,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = badgeMessage,
            style = MaterialTheme.typography.labelSmall,
            color = statusTint,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}
