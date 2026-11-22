package com.nexus.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nexus.app.domain.model.AssetCategory
import com.nexus.app.domain.model.MarketAlertCondition
import com.nexus.app.domain.model.MarketAlertStatus
import com.nexus.app.domain.model.MarketAssetCatalog
import com.nexus.app.domain.model.MarketHistoryCalculations
import com.nexus.app.domain.model.MarketHistoryRange
import com.nexus.app.domain.model.MarketHistoryUiState
import com.nexus.app.domain.model.MarketPriceAlert
import com.nexus.app.domain.model.MarketQuoteUiState
import com.nexus.app.ui.components.MarketChangeBadge
import com.nexus.app.ui.components.MarketFavoriteStarButton
import com.nexus.app.ui.components.MarketModalityBadge
import com.nexus.app.ui.components.MarketQuoteStatusBadge
import com.nexus.app.ui.components.MarketStatusBannerRow
import com.nexus.app.ui.components.NexusCard
import com.nexus.app.ui.components.SectionHeader
import com.nexus.app.ui.components.charts.MarketHistoryChartCard
import com.nexus.app.ui.market.MarketAssetDetailPresentation
import com.nexus.app.ui.market.MarketAssetRowPresentation
import com.nexus.app.ui.market.MarketFilterCategory
import com.nexus.app.ui.market.MarketOverviewPresentation
import com.nexus.app.ui.market.MarketPresentationMapper
import com.nexus.app.ui.market.MarketStatusSeverity
import com.nexus.app.ui.theme.LocalNexusPalette
import kotlinx.coroutines.launch

@Composable
fun MarketScreen(
    quoteStates: Map<String, MarketQuoteUiState>,
    favoriteCodes: List<String> = emptyList(),
    alerts: List<MarketPriceAlert> = emptyList(),
    onRefreshAsset: (String) -> Unit,
    onRefreshAll: () -> Unit,
    onAutoRefreshIfStale: () -> Unit,
    onToggleFavorite: (String) -> Unit = {},
    onCreateAlert: (String, MarketAlertCondition, Double, Boolean) -> Unit = { _, _, _, _ -> },
    onUpdateAlert: (String, String, MarketAlertCondition, Double, Boolean) -> Unit = { _, _, _, _, _ -> },
    onToggleAlertActive: (String, String, Boolean) -> Unit = { _, _, _ -> },
    onDeleteAlert: (String) -> Unit = {},
    onOpenAssetDetail: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalNexusPalette.current
    var selectedFilter by remember { mutableStateOf(MarketFilterCategory.ALL) }
    var alertEditorState by remember { mutableStateOf<MarketAlertEditorState?>(null) }
    val (hasNotificationPermission, requestNotificationPermission) =
        rememberMarketNotificationPermissionState()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val overview = remember(quoteStates, favoriteCodes) {
        MarketPresentationMapper.buildOverview(
            states = quoteStates,
            favoriteCodes = favoriteCodes
        )
    }
    val alertRows = remember(alerts, quoteStates) {
        MarketPresentationMapper.buildAlertRows(
            alerts = alerts,
            states = quoteStates
        )
    }

    LaunchedEffect(Unit) {
        onAutoRefreshIfStale()
    }

    Column(modifier = modifier.fillMaxSize()) {
        TopSubScreenBar(title = "Mercado", onBack = onBack)

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isWide = maxWidth >= 720.dp

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item(key = "mkt_overview_summary") {
                    MarketOverviewSummaryCard(
                        overview = overview,
                        onRefreshAll = onRefreshAll
                    )
                }

                item(key = "mkt_category_filters") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MarketFilterCategory.entries.forEach { filter ->
                            val isSelected = selectedFilter == filter
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedFilter = filter },
                                label = { Text(filter.label, maxLines = 1) },
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
                }

                if (selectedFilter == MarketFilterCategory.ALL) {
                    val waitingCount = alerts.count { it.status == MarketAlertStatus.WAITING }
                    val triggeredCount = alerts.count { it.status == MarketAlertStatus.TRIGGERED }
                    val alertsSummary = when {
                        alerts.isEmpty() -> "Sem alertas"
                        triggeredCount > 0 -> "$waitingCount ativo(s) · $triggeredCount disparado(s)"
                        else -> "$waitingCount ativo(s)"
                    }
                    val openNewAlertEditor = {
                        alertEditorState = MarketAlertEditorState(
                            initialAssetCode = favoriteCodes.firstOrNull()
                                ?: MarketAssetCatalog.USD_BRL_CODE,
                            lockAssetSelection = false
                        )
                    }

                    if (isWide) {
                        item(key = "mkt_section_watchlist_alerts_row") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier.weight(0.48f),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    SectionHeader(
                                        title = "Acompanhamento",
                                        trailing = if (overview.watchlist.isEmpty()) {
                                            "Sem favoritos"
                                        } else {
                                            "${overview.watchlist.size} favorito(s)"
                                        }
                                    )
                                    if (overview.watchlist.isEmpty()) {
                                        MarketWatchlistEmptyCard(
                                            onExploreAssets = {
                                                coroutineScope.launch {
                                                    listState.animateScrollToItem(3)
                                                }
                                            }
                                        )
                                    } else {
                                        MarketWatchlistCard(
                                            rows = overview.watchlist,
                                            onOpenAssetDetail = onOpenAssetDetail,
                                            onToggleFavorite = onToggleFavorite
                                        )
                                    }
                                }

                                Column(
                                    modifier = Modifier.weight(0.52f),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    SectionHeader(
                                        title = "Alertas",
                                        trailing = alertsSummary,
                                        actionLabel = "Novo alerta",
                                        onAction = openNewAlertEditor
                                    )
                                    MarketAlertsSectionCard(
                                        alertRows = alertRows,
                                        hasNotificationPermission = hasNotificationPermission,
                                        onRequestNotificationPermission = requestNotificationPermission,
                                        onCreateAlert = openNewAlertEditor,
                                        onEditAlert = { alert ->
                                            alertEditorState = MarketAlertEditorState(
                                                existingAlert = alert,
                                                initialAssetCode = alert.assetCode,
                                                lockAssetSelection = true
                                            )
                                        },
                                        onToggleAlertActive = { alert, active ->
                                            onToggleAlertActive(alert.id, alert.assetCode, active)
                                        },
                                        onDeleteAlert = { alert -> onDeleteAlert(alert.id) },
                                        onOpenAssetDetail = onOpenAssetDetail
                                    )
                                }
                            }
                        }
                    } else {
                        item(key = "mkt_section_watchlist") {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                SectionHeader(
                                    title = "Acompanhamento",
                                    trailing = if (overview.watchlist.isEmpty()) {
                                        "Sem favoritos"
                                    } else {
                                        "${overview.watchlist.size} favorito(s)"
                                    }
                                )
                                if (overview.watchlist.isEmpty()) {
                                    MarketWatchlistEmptyCard(
                                        onExploreAssets = {
                                            coroutineScope.launch {
                                                listState.animateScrollToItem(4)
                                            }
                                        }
                                    )
                                } else {
                                    MarketWatchlistCard(
                                        rows = overview.watchlist,
                                        onOpenAssetDetail = onOpenAssetDetail,
                                        onToggleFavorite = onToggleFavorite
                                    )
                                }
                            }
                        }

                        item(key = "mkt_section_alerts") {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                SectionHeader(
                                    title = "Alertas",
                                    trailing = alertsSummary,
                                    actionLabel = "Novo alerta",
                                    onAction = openNewAlertEditor
                                )
                                MarketAlertsSectionCard(
                                    alertRows = alertRows,
                                    hasNotificationPermission = hasNotificationPermission,
                                    onRequestNotificationPermission = requestNotificationPermission,
                                    onCreateAlert = openNewAlertEditor,
                                    onEditAlert = { alert ->
                                        alertEditorState = MarketAlertEditorState(
                                            existingAlert = alert,
                                            initialAssetCode = alert.assetCode,
                                            lockAssetSelection = true
                                        )
                                    },
                                    onToggleAlertActive = { alert, active ->
                                        onToggleAlertActive(alert.id, alert.assetCode, active)
                                    },
                                    onDeleteAlert = { alert -> onDeleteAlert(alert.id) },
                                    onOpenAssetDetail = onOpenAssetDetail
                                )
                            }
                        }
                    }
                }

                if (selectedFilter == MarketFilterCategory.ALL || selectedFilter == MarketFilterCategory.CURRENCY) {
                    item(key = "mkt_section_currencies") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SectionHeader(
                                title = "Moedas",
                                trailing = "${overview.currencies.size} ativos · BRL"
                            )
                            MarketAssetGroupCard(
                                rows = overview.currencies,
                                isWide = isWide,
                                onOpenAssetDetail = onOpenAssetDetail,
                                onRefreshAsset = onRefreshAsset,
                                onToggleFavorite = onToggleFavorite
                            )
                        }
                    }
                }

                if (selectedFilter == MarketFilterCategory.ALL || selectedFilter == MarketFilterCategory.CRYPTO) {
                    item(key = "mkt_section_crypto") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SectionHeader(
                                title = "Criptomoedas",
                                trailing = "${overview.cryptos.size} ativos · 24h"
                            )
                            MarketAssetGroupCard(
                                rows = overview.cryptos,
                                isWide = isWide,
                                onOpenAssetDetail = onOpenAssetDetail,
                                onRefreshAsset = onRefreshAsset,
                                onToggleFavorite = onToggleFavorite
                            )
                        }
                    }
                }

                if (selectedFilter == MarketFilterCategory.ALL || selectedFilter == MarketFilterCategory.INDEX) {
                    item(key = "mkt_section_indices") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SectionHeader(
                                title = "Índices",
                                trailing = "${overview.indices.size} ativos · B3 e EUA"
                            )
                            MarketAssetGroupCard(
                                rows = overview.indices,
                                isWide = isWide,
                                onOpenAssetDetail = onOpenAssetDetail,
                                onRefreshAsset = onRefreshAsset,
                                onToggleFavorite = onToggleFavorite
                            )
                        }
                    }
                }

                if (selectedFilter == MarketFilterCategory.ALL || selectedFilter == MarketFilterCategory.STOCK) {
                    item(key = "mkt_section_stocks") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SectionHeader(
                                title = "Ações",
                                trailing = "${overview.stocks.size} ativos · B3 e Nasdaq"
                            )
                            MarketAssetGroupCard(
                                rows = overview.stocks,
                                isWide = isWide,
                                onOpenAssetDetail = onOpenAssetDetail,
                                onRefreshAsset = onRefreshAsset,
                                onToggleFavorite = onToggleFavorite
                            )
                        }
                    }
                }
            }
        }
    }

    alertEditorState?.let { editor ->
        MarketAlertEditorSheet(
            editorState = editor,
            quoteStates = quoteStates,
            hasNotificationPermission = hasNotificationPermission,
            onRequestNotificationPermission = requestNotificationPermission,
            onSaveCreate = onCreateAlert,
            onSaveUpdate = onUpdateAlert,
            onDismiss = { alertEditorState = null }
        )
    }
}

@Composable
private fun MarketOverviewSummaryCard(
    overview: MarketOverviewPresentation,
    onRefreshAll: () -> Unit
) {
    val palette = LocalNexusPalette.current
    NexusCard(modifier = Modifier.fillMaxWidth(), elevated = true) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Visão geral de mercado",
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.textPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${overview.loadedAssetCount}/${overview.activeAssetCount} ativos em cache · Cotações reais",
                    style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                    color = palette.textSecondary
                )
            }
            OutlinedButton(
                onClick = onRefreshAll,
                enabled = !overview.isAnyRefreshing,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, palette.border),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                if (overview.isAnyRefreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(15.dp),
                        strokeWidth = 2.dp,
                        color = palette.highlight
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Atualizando", color = palette.textSecondary, style = MaterialTheme.typography.labelMedium)
                } else {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = null,
                        tint = palette.highlight,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Atualizar tudo", color = palette.textPrimary, style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        if (overview.formattedLastSyncAt != null || overview.isOffline || overview.staleOrFailedCount > 0) {
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = palette.border)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = overview.formattedLastSyncAt?.let { "Última coleta: $it" } ?: "Aguardando primeira coleta",
                    style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                    color = palette.textSecondary
                )
                when {
                    overview.isOffline -> Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CloudOff,
                            contentDescription = null,
                            tint = palette.warning,
                            modifier = Modifier.size(13.dp)
                        )
                        Text("Modo offline", style = MaterialTheme.typography.labelSmall, color = palette.warning)
                    }
                    overview.staleOrFailedCount > 0 -> Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Schedule,
                            contentDescription = null,
                            tint = palette.warning,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "${overview.staleOrFailedCount} pendente(s)",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.warning
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MarketWatchlistEmptyCard(
    onExploreAssets: () -> Unit
) {
    val palette = LocalNexusPalette.current
    NexusCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Acompanhe seus ativos principais em um só lugar. Toque na estrela de qualquer ativo na listagem abaixo para adicioná-lo aqui.",
                style = MaterialTheme.typography.bodyMedium,
                color = palette.textSecondary
            )
            OutlinedButton(
                onClick = onExploreAssets,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, palette.border),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "Ir para listagem de ativos",
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.textPrimary
                )
            }
        }
    }
}

@Composable
private fun MarketWatchlistCard(
    rows: List<MarketAssetRowPresentation>,
    onOpenAssetDetail: (String) -> Unit,
    onToggleFavorite: (String) -> Unit
) {
    val palette = LocalNexusPalette.current
    NexusCard(modifier = Modifier.fillMaxWidth(), elevated = true) {
        rows.forEachIndexed { index, row ->
            if (index > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = palette.border)
                Spacer(modifier = Modifier.height(8.dp))
            }
            MarketWatchlistCompactRow(
                row = row,
                onOpen = { onOpenAssetDetail(row.descriptor.code) },
                onToggleFavorite = { onToggleFavorite(row.descriptor.code) }
            )
        }
    }
}

@Composable
private fun MarketWatchlistCompactRow(
    row: MarketAssetRowPresentation,
    onOpen: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val palette = LocalNexusPalette.current
    val state = row.uiState
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onOpen)
            .padding(vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = row.descriptor.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${row.ticker} · ${row.unitOrCurrencyLabel}",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textSecondary,
                    maxLines = 1
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                MarketQuoteStatusBadge(
                    label = row.quoteStatusLabel,
                    severity = row.statusSeverity
                )
                MarketFavoriteStarButton(
                    isFavorite = row.isFavorite,
                    assetName = row.descriptor.displayName,
                    onToggle = onToggleFavorite
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (state.quote == null && state.isRefreshing) "Consultando..." else row.formattedPrice,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontFeatureSettings = "tnum"
                    ),
                    color = if (state.quote != null) palette.textPrimary else palette.textSecondary
                )
                if (row.formattedConvertedBrl != null) {
                    Text(
                        text = "≈ ${row.formattedConvertedBrl} em BRL",
                        style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                        color = palette.textSecondary,
                        maxLines = 1
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (row.changePercent != null) {
                    MarketChangeBadge(
                        changePercent = row.changePercent,
                        trend = row.trend,
                        periodLabel = state.quote?.variationPeriodLabel
                    )
                } else if (state.quote != null) {
                    Text(
                        text = "Var. indisp.",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textSecondary
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    contentDescription = "Ver detalhes de ${row.descriptor.displayName}",
                    tint = palette.textSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun MarketAssetGroupCard(
    rows: List<MarketAssetRowPresentation>,
    isWide: Boolean,
    onOpenAssetDetail: (String) -> Unit,
    onRefreshAsset: (String) -> Unit,
    onToggleFavorite: (String) -> Unit
) {
    val palette = LocalNexusPalette.current
    NexusCard(modifier = Modifier.fillMaxWidth()) {
        if (!isWide || rows.size <= 1) {
            rows.forEachIndexed { index, row ->
                if (index > 0) {
                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = palette.border)
                    Spacer(modifier = Modifier.height(10.dp))
                }
                MarketAssetCompactRow(
                    row = row,
                    onOpen = { onOpenAssetDetail(row.descriptor.code) },
                    onRefresh = { onRefreshAsset(row.descriptor.code) },
                    onToggleFavorite = { onToggleFavorite(row.descriptor.code) }
                )
            }
        } else {
            val pairs = rows.chunked(2)
            pairs.forEachIndexed { pairIndex, pair ->
                if (pairIndex > 0) {
                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = palette.border)
                    Spacer(modifier = Modifier.height(10.dp))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        MarketAssetCompactRow(
                            row = pair[0],
                            onOpen = { onOpenAssetDetail(pair[0].descriptor.code) },
                            onRefresh = { onRefreshAsset(pair[0].descriptor.code) },
                            onToggleFavorite = { onToggleFavorite(pair[0].descriptor.code) }
                        )
                    }
                    if (pair.size > 1) {
                        Box(modifier = Modifier.weight(1f)) {
                            MarketAssetCompactRow(
                                row = pair[1],
                                onOpen = { onOpenAssetDetail(pair[1].descriptor.code) },
                                onRefresh = { onRefreshAsset(pair[1].descriptor.code) },
                                onToggleFavorite = { onToggleFavorite(pair[1].descriptor.code) }
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun MarketAssetCompactRow(
    row: MarketAssetRowPresentation,
    onOpen: () -> Unit,
    onRefresh: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val palette = LocalNexusPalette.current
    val state = row.uiState

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onOpen)
            .padding(vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = row.descriptor.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (row.modalityLabel != null) {
                        MarketModalityBadge(label = row.modalityLabel)
                    } else if (row.exchangeLabel != null) {
                        Text(
                            text = "${row.ticker} · ${row.exchangeLabel}",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.textSecondary,
                            maxLines = 1
                        )
                    } else {
                        Text(
                            text = row.pairLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.textSecondary,
                            maxLines = 1
                        )
                    }
                }
                if (row.descriptor.category == AssetCategory.INDEX || row.descriptor.category == AssetCategory.STOCK) {
                    Spacer(modifier = Modifier.height(1.dp))
                    Text(
                        text = buildString {
                            append(row.ticker)
                            row.exchangeLabel?.let { append(" · $it") }
                            append(" · ${row.originalCurrency}")
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textSecondary,
                        maxLines = 1
                    )
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = if (state.quote == null && state.isRefreshing) "Consultando..." else row.formattedPrice,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontFeatureSettings = "tnum"
                    ),
                    color = if (state.quote != null) palette.textPrimary else palette.textSecondary
                )
                if (row.formattedConvertedBrl != null) {
                    Text(
                        text = "≈ ${row.formattedConvertedBrl} em BRL",
                        style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                        color = palette.textSecondary,
                        maxLines = 1
                    )
                } else if (row.conversionUnavailableNote != null) {
                    Text(
                        text = row.conversionUnavailableNote,
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.warning,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (row.changePercent != null) {
                    MarketChangeBadge(
                        changePercent = row.changePercent,
                        trend = row.trend,
                        periodLabel = state.quote?.variationPeriodLabel
                    )
                } else if (state.quote != null) {
                    Text(
                        text = "Var. indisp.",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textSecondary
                    )
                }

                MarketFavoriteStarButton(
                    isFavorite = row.isFavorite,
                    assetName = row.descriptor.displayName,
                    onToggle = onToggleFavorite
                )

                IconButton(
                    onClick = onRefresh,
                    enabled = !state.isRefreshing,
                    modifier = Modifier.size(32.dp)
                ) {
                    if (state.isRefreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(15.dp),
                            strokeWidth = 2.dp,
                            color = palette.highlight
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = "Atualizar ${row.descriptor.displayName}",
                            tint = palette.highlight,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    contentDescription = "Ver detalhes de ${row.descriptor.displayName}",
                    tint = palette.textSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        if (row.sourceName != null && row.formattedSourceTime != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Fonte: ${row.sourceName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = row.formattedSourceTime,
                    style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                    color = palette.textSecondary,
                    maxLines = 1
                )
            }
        }

        if (row.delayNotice != null && state.quote != null) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = row.delayNotice,
                style = MaterialTheme.typography.labelSmall,
                color = palette.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (row.statusNote != null) {
            if (state.quote != null) {
                MarketStatusBannerRow(state = state)
            } else {
                Text(
                    text = row.statusNote,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (row.statusSeverity == MarketStatusSeverity.WARNING) palette.warning else palette.textSecondary
                )
            }
        }
    }
}

@Composable
fun MarketAssetDetailScreen(
    assetCode: String,
    quoteStates: Map<String, MarketQuoteUiState>,
    historyStates: Map<MarketHistoryRange, MarketHistoryUiState> = emptyMap(),
    isFavorite: Boolean = false,
    alerts: List<MarketPriceAlert> = emptyList(),
    onRefreshAsset: (String) -> Unit,
    onLoadHistory: (String, MarketHistoryRange, Boolean) -> Unit = { _, _, _ -> },
    onToggleFavorite: (String) -> Unit = {},
    onCreateAlert: (String, MarketAlertCondition, Double, Boolean) -> Unit = { _, _, _, _ -> },
    onUpdateAlert: (String, String, MarketAlertCondition, Double, Boolean) -> Unit = { _, _, _, _, _ -> },
    onToggleAlertActive: (String, String, Boolean) -> Unit = { _, _, _ -> },
    onDeleteAlert: (String) -> Unit = {},
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val descriptor = remember(assetCode) {
        MarketAssetCatalog.findByCode(assetCode) ?: MarketAssetCatalog.USD_BRL
    }
    var alertEditorState by remember { mutableStateOf<MarketAlertEditorState?>(null) }
    val (hasNotificationPermission, requestNotificationPermission) =
        rememberMarketNotificationPermissionState()
    val supportedRanges = remember(descriptor.code) {
        MarketHistoryCalculations.supportedRangesFor(descriptor.code)
    }
    var selectedRange by remember(descriptor.code) {
        mutableStateOf(MarketHistoryCalculations.defaultRangeFor(descriptor.code))
    }
    val activeHistoryState = historyStates[selectedRange]

    LaunchedEffect(descriptor.code, selectedRange) {
        onLoadHistory(descriptor.code, selectedRange, false)
    }

    val state = quoteStates[descriptor.code] ?: MarketQuoteUiState(assetCode = descriptor.code)
    val usdBrlState = quoteStates[MarketAssetCatalog.USD_BRL_CODE]
    val detail = remember(descriptor, state, usdBrlState, activeHistoryState, isFavorite) {
        MarketPresentationMapper.buildDetail(
            descriptor = descriptor,
            state = state,
            usdBrlState = usdBrlState,
            historyState = activeHistoryState,
            isFavorite = isFavorite
        )
    }
    val assetAlertRows = remember(alerts, quoteStates, descriptor.code) {
        MarketPresentationMapper.buildAlertRows(
            alerts = alerts.filter { it.assetCode == descriptor.code },
            states = quoteStates
        )
    }

    Column(modifier = modifier.fillMaxSize()) {
        TopSubScreenBar(title = detail.descriptor.displayName, onBack = onBack)

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isTwoPane = maxWidth >= 840.dp

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isTwoPane) {
                    item(key = "detail_two_pane") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.weight(0.44f),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                MarketAssetDetailHeroCard(
                                    detail = detail,
                                    onRefresh = { onRefreshAsset(descriptor.code) },
                                    onToggleFavorite = { onToggleFavorite(descriptor.code) }
                                )
                                MarketAssetDetailMetadataCard(detail = detail)
                            }
                            Column(
                                modifier = Modifier.weight(0.56f),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                MarketHistoryChartCard(
                                    descriptor = descriptor,
                                    selectedRange = selectedRange,
                                    supportedRanges = supportedRanges,
                                    historyState = activeHistoryState,
                                    onSelectRange = { range -> selectedRange = range },
                                    onRefreshRange = { onLoadHistory(descriptor.code, selectedRange, true) }
                                )
                                MarketAssetAlertsCard(
                                    descriptor = descriptor,
                                    alertRows = assetAlertRows,
                                    hasNotificationPermission = hasNotificationPermission,
                                    onRequestNotificationPermission = requestNotificationPermission,
                                    onCreateAlertForAsset = {
                                        alertEditorState = MarketAlertEditorState(
                                            initialAssetCode = descriptor.code,
                                            lockAssetSelection = true
                                        )
                                    },
                                    onEditAlert = { alert ->
                                        alertEditorState = MarketAlertEditorState(
                                            existingAlert = alert,
                                            initialAssetCode = alert.assetCode,
                                            lockAssetSelection = true
                                        )
                                    },
                                    onToggleAlertActive = { alert, active ->
                                        onToggleAlertActive(alert.id, alert.assetCode, active)
                                    },
                                    onDeleteAlert = { alert -> onDeleteAlert(alert.id) }
                                )
                            }
                        }
                    }
                } else {
                    item(key = "detail_hero") {
                        MarketAssetDetailHeroCard(
                            detail = detail,
                            onRefresh = { onRefreshAsset(descriptor.code) },
                            onToggleFavorite = { onToggleFavorite(descriptor.code) }
                        )
                    }

                    item(key = "detail_history_chart") {
                        MarketHistoryChartCard(
                            descriptor = descriptor,
                            selectedRange = selectedRange,
                            supportedRanges = supportedRanges,
                            historyState = activeHistoryState,
                            onSelectRange = { range -> selectedRange = range },
                            onRefreshRange = { onLoadHistory(descriptor.code, selectedRange, true) }
                        )
                    }

                    item(key = "detail_alerts") {
                        MarketAssetAlertsCard(
                            descriptor = descriptor,
                            alertRows = assetAlertRows,
                            hasNotificationPermission = hasNotificationPermission,
                            onRequestNotificationPermission = requestNotificationPermission,
                            onCreateAlertForAsset = {
                                alertEditorState = MarketAlertEditorState(
                                    initialAssetCode = descriptor.code,
                                    lockAssetSelection = true
                                )
                            },
                            onEditAlert = { alert ->
                                alertEditorState = MarketAlertEditorState(
                                    existingAlert = alert,
                                    initialAssetCode = alert.assetCode,
                                    lockAssetSelection = true
                                )
                            },
                            onToggleAlertActive = { alert, active ->
                                onToggleAlertActive(alert.id, alert.assetCode, active)
                            },
                            onDeleteAlert = { alert -> onDeleteAlert(alert.id) }
                        )
                    }

                    item(key = "detail_metadata") {
                        MarketAssetDetailMetadataCard(detail = detail)
                    }
                }
            }
        }
    }

    alertEditorState?.let { editor ->
        MarketAlertEditorSheet(
            editorState = editor,
            quoteStates = quoteStates,
            hasNotificationPermission = hasNotificationPermission,
            onRequestNotificationPermission = requestNotificationPermission,
            onSaveCreate = onCreateAlert,
            onSaveUpdate = onUpdateAlert,
            onDismiss = { alertEditorState = null }
        )
    }
}

@Composable
private fun MarketAssetDetailHeroCard(
    detail: MarketAssetDetailPresentation,
    onRefresh: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val palette = LocalNexusPalette.current
    val state = detail.uiState

    NexusCard(modifier = Modifier.fillMaxWidth(), elevated = true) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = detail.pairLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.textSecondary
                )
                if (detail.exchangeLabel != null) {
                    MarketModalityBadge(label = detail.exchangeLabel)
                }
                MarketModalityBadge(label = detail.categoryLabel)
                if (detail.modalityLabel != null) {
                    MarketModalityBadge(label = detail.modalityLabel)
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                MarketFavoriteStarButton(
                    isFavorite = detail.isFavorite,
                    assetName = detail.descriptor.displayName,
                    onToggle = onToggleFavorite
                )

                IconButton(
                    onClick = onRefresh,
                    enabled = !state.isRefreshing,
                    modifier = Modifier.size(36.dp)
                ) {
                    if (state.isRefreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = palette.highlight
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = "Atualizar ${detail.descriptor.displayName}",
                            tint = palette.highlight,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (state.quote == null && state.isRefreshing) "Consultando..." else detail.formattedPrice,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontFeatureSettings = "tnum"
                ),
                color = if (state.quote != null) palette.textPrimary else palette.textSecondary
            )

            if (detail.changePercent != null) {
                MarketChangeBadge(
                    changePercent = detail.changePercent,
                    trend = detail.trend,
                    periodLabel = detail.variationPeriodLabel
                )
            } else if (state.quote != null) {
                Text(
                    text = "Variação indisponível",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textSecondary
                )
            }
        }

        if (detail.formattedConvertedBrl != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Conversão aproximada: ${detail.formattedConvertedBrl}",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Medium,
                    fontFeatureSettings = "tnum"
                ),
                color = palette.textPrimary
            )
            detail.convertedBrlDetailNote?.let { note ->
                Text(
                    text = note,
                    style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                    color = palette.textSecondary
                )
            }
        } else if (detail.convertedBrlDetailNote != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = detail.convertedBrlDetailNote,
                style = MaterialTheme.typography.labelSmall,
                color = palette.warning
            )
        }

        if (detail.delayNotice != null && state.quote != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = detail.delayNotice,
                style = MaterialTheme.typography.labelSmall,
                color = palette.textSecondary
            )
        }

        if (detail.formattedBidPrice != null || detail.formattedAskPrice != null || detail.formattedPreviousClose != null) {
            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = palette.border)
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                detail.formattedBidPrice?.let { bid ->
                    DetailMiniMetric(
                        label = "Compra",
                        value = bid,
                        modifier = Modifier.weight(1f)
                    )
                }
                detail.formattedAskPrice?.let { ask ->
                    DetailMiniMetric(
                        label = "Venda",
                        value = ask,
                        modifier = Modifier.weight(1f)
                    )
                }
                detail.formattedPreviousClose?.let { prev ->
                    DetailMiniMetric(
                        label = "Ref. anterior",
                        value = prev,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        if (state.quote != null) {
            MarketStatusBannerRow(state = state)
        } else if (!state.isRefreshing) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = state.statusMessage ?: "Sem cotação armazenada para este ativo.",
                style = MaterialTheme.typography.labelSmall,
                color = if (state.isOffline || state.lastRefreshFailed) palette.warning else palette.textSecondary
            )
        }
    }
}

@Composable
private fun DetailMiniMetric(
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
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Medium,
                fontFeatureSettings = "tnum"
            ),
            color = palette.textPrimary,
            maxLines = 1
        )
    }
}

@Composable
private fun MarketAssetDetailMetadataCard(detail: MarketAssetDetailPresentation) {
    val palette = LocalNexusPalette.current
    NexusCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Informações da cotação",
            style = MaterialTheme.typography.titleMedium,
            color = palette.textPrimary
        )
        Spacer(modifier = Modifier.height(10.dp))
        DetailMetadataRow(label = "Ativo", value = detail.descriptor.displayName)
        DetailMetadataDivider()
        DetailMetadataRow(label = "Ticker / Código", value = detail.ticker)
        if (detail.exchangeLabel != null) {
            DetailMetadataDivider()
            DetailMetadataRow(label = "Bolsa / Mercado", value = detail.exchangeLabel)
        }
        DetailMetadataDivider()
        DetailMetadataRow(label = "Moeda original", value = detail.originalCurrencyLabel)
        if (detail.formattedConvertedBrl != null || detail.convertedBrlDetailNote != null) {
            DetailMetadataDivider()
            DetailMetadataRow(
                label = "Valor em BRL",
                value = detail.formattedConvertedBrl ?: "Indisponível (sem câmbio USD/BRL)"
            )
        }
        DetailMetadataDivider()
        DetailMetadataRow(
            label = "Modalidade",
            value = buildString {
                append(detail.modalityLabel ?: "Não aplicável / Aguardando coleta")
                if (detail.isDelayed && detail.delayNotice != null) {
                    append(" · ${detail.delayNotice}")
                }
            }
        )
        DetailMetadataDivider()
        DetailMetadataRow(
            label = "Variação",
            value = detail.formattedChangeWithPeriod ?: "Indisponível"
        )
        DetailMetadataDivider()
        DetailMetadataRow(label = "Fonte dos dados", value = detail.sourceName ?: "Ainda não consultado")
        DetailMetadataDivider()
        DetailMetadataRow(
            label = "Horário na fonte",
            value = detail.formattedSourceTimestamp ?: "Indisponível"
        )
        DetailMetadataDivider()
        DetailMetadataRow(
            label = "Última coleta local",
            value = detail.formattedFetchedAt ?: "Ainda não armazenado"
        )
        DetailMetadataDivider()
        DetailMetadataRow(label = "Estado do cache", value = detail.cacheStatusLabel)
        DetailMetadataDivider()
        DetailMetadataRow(label = "Conectividade", value = detail.connectivityStatusLabel)
    }
}

@Composable
private fun DetailMetadataRow(label: String, value: String) {
    val palette = LocalNexusPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = palette.textSecondary,
            modifier = Modifier.weight(0.45f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
            color = palette.textPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(0.55f)
        )
    }
}

@Composable
private fun DetailMetadataDivider() {
    HorizontalDivider(color = LocalNexusPalette.current.border)
}
