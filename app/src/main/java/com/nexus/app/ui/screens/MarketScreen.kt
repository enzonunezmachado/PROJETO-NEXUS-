package com.nexus.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nexus.app.domain.model.AssetCategory
import com.nexus.app.domain.model.MarketAssetCatalog
import com.nexus.app.domain.model.MarketQuoteUiState
import com.nexus.app.ui.components.MarketChangeBadge
import com.nexus.app.ui.components.MarketModalityBadge
import com.nexus.app.ui.components.MarketStatusBannerRow
import com.nexus.app.ui.components.NexusCard
import com.nexus.app.ui.components.SectionHeader
import com.nexus.app.ui.market.MarketAssetDetailPresentation
import com.nexus.app.ui.market.MarketAssetRowPresentation
import com.nexus.app.ui.market.MarketFilterCategory
import com.nexus.app.ui.market.MarketOverviewPresentation
import com.nexus.app.ui.market.MarketPresentationMapper
import com.nexus.app.ui.market.MarketStatusSeverity
import com.nexus.app.ui.market.PlannedCategorySection
import com.nexus.app.ui.theme.LocalNexusPalette

@Composable
fun MarketScreen(
    quoteStates: Map<String, MarketQuoteUiState>,
    onRefreshAsset: (String) -> Unit,
    onRefreshAll: () -> Unit,
    onAutoRefreshIfStale: () -> Unit,
    onOpenAssetDetail: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalNexusPalette.current
    var selectedFilter by remember { mutableStateOf(MarketFilterCategory.ALL) }
    val overview = remember(quoteStates) {
        MarketPresentationMapper.buildOverview(quoteStates)
    }

    LaunchedEffect(Unit) {
        onAutoRefreshIfStale()
    }

    Column(modifier = modifier.fillMaxSize()) {
        TopSubScreenBar(title = "Mercado", onBack = onBack)

        LazyColumn(
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

            if (selectedFilter == MarketFilterCategory.ALL || selectedFilter == MarketFilterCategory.CURRENCY) {
                item(key = "mkt_section_currencies") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SectionHeader(
                            title = "Moedas",
                            trailing = "${overview.currencies.size} ativos · BRL"
                        )
                        MarketAssetGroupCard(
                            rows = overview.currencies,
                            onOpenAssetDetail = onOpenAssetDetail,
                            onRefreshAsset = onRefreshAsset
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
                            onOpenAssetDetail = onOpenAssetDetail,
                            onRefreshAsset = onRefreshAsset
                        )
                    }
                }
            }

            if (selectedFilter == MarketFilterCategory.ALL || selectedFilter == MarketFilterCategory.INDEX) {
                val indexSection = overview.plannedSections.firstOrNull { it.category == AssetCategory.INDEX }
                if (indexSection != null) {
                    item(key = "mkt_section_indices") {
                        PlannedCategoryBlock(section = indexSection)
                    }
                }
            }

            if (selectedFilter == MarketFilterCategory.ALL || selectedFilter == MarketFilterCategory.STOCK) {
                val stockSection = overview.plannedSections.firstOrNull { it.category == AssetCategory.STOCK }
                if (stockSection != null) {
                    item(key = "mkt_section_stocks") {
                        PlannedCategoryBlock(section = stockSection)
                    }
                }
            }
        }
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
                    text = "${overview.loadedAssetCount}/${overview.activeAssetCount} ativos em cache · Referência BRL",
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
private fun MarketAssetGroupCard(
    rows: List<MarketAssetRowPresentation>,
    onOpenAssetDetail: (String) -> Unit,
    onRefreshAsset: (String) -> Unit
) {
    val palette = LocalNexusPalette.current
    NexusCard(modifier = Modifier.fillMaxWidth()) {
        rows.forEachIndexed { index, row ->
            if (index > 0) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = palette.border)
                Spacer(modifier = Modifier.height(10.dp))
            }
            MarketAssetCompactRow(
                row = row,
                onOpen = { onOpenAssetDetail(row.descriptor.code) },
                onRefresh = { onRefreshAsset(row.descriptor.code) }
            )
        }
    }
}

@Composable
private fun MarketAssetCompactRow(
    row: MarketAssetRowPresentation,
    onOpen: () -> Unit,
    onRefresh: () -> Unit
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
                    } else {
                        Text(
                            text = row.pairLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.textSecondary,
                            maxLines = 1
                        )
                    }
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
private fun PlannedCategoryBlock(section: PlannedCategorySection) {
    val palette = LocalNexusPalette.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader(
            title = section.title,
            trailing = "Etapa futura"
        )
        NexusCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = section.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.textSecondary,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                MarketModalityBadge(label = "Em breve")
            }
            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = palette.border)
            Spacer(modifier = Modifier.height(8.dp))
            section.plannedExamples.forEachIndexed { idx, item ->
                if (idx > 0) {
                    Spacer(modifier = Modifier.height(6.dp))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.textPrimary
                        )
                        Text(
                            text = item.tickerHint,
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.textSecondary
                        )
                    }
                    Text(
                        text = item.marketHint,
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun MarketAssetDetailScreen(
    assetCode: String,
    quoteStates: Map<String, MarketQuoteUiState>,
    onRefreshAsset: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalNexusPalette.current
    val descriptor = remember(assetCode) {
        MarketAssetCatalog.findByCode(assetCode) ?: MarketAssetCatalog.USD_BRL
    }
    val state = quoteStates[descriptor.code] ?: MarketQuoteUiState(assetCode = descriptor.code)
    val detail = remember(descriptor, state) {
        MarketPresentationMapper.buildDetail(descriptor, state)
    }

    Column(modifier = modifier.fillMaxSize()) {
        TopSubScreenBar(title = detail.descriptor.displayName, onBack = onBack)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "detail_hero") {
                MarketAssetDetailHeroCard(
                    detail = detail,
                    onRefresh = { onRefreshAsset(descriptor.code) }
                )
            }

            item(key = "detail_history_slot") {
                MarketHistoryChartSlot(detail = detail)
            }

            item(key = "detail_metadata") {
                MarketAssetDetailMetadataCard(detail = detail)
            }
        }
    }
}

@Composable
private fun MarketAssetDetailHeroCard(
    detail: MarketAssetDetailPresentation,
    onRefresh: () -> Unit
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
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = detail.pairLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.textSecondary
                )
                MarketModalityBadge(label = detail.categoryLabel)
                if (detail.modalityLabel != null) {
                    MarketModalityBadge(label = detail.modalityLabel)
                }
            }

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
private fun MarketHistoryChartSlot(detail: MarketAssetDetailPresentation) {
    val palette = LocalNexusPalette.current
    NexusCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Histórico e Evolução",
                style = MaterialTheme.typography.titleMedium,
                color = palette.textPrimary
            )
            MarketModalityBadge(label = "Sem histórico")
        }
        Spacer(modifier = Modifier.height(10.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            color = palette.surfaceElevated,
            border = BorderStroke(1.dp, palette.border)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ShowChart,
                        contentDescription = null,
                        tint = palette.textSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = detail.historicalStatusMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textSecondary
                    )
                }
            }
        }
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
        DetailMetadataRow(label = "Ativo", value = "${detail.descriptor.displayName} (${detail.pairLabel})")
        DetailMetadataDivider()
        DetailMetadataRow(label = "Moeda de referência", value = detail.referenceCurrencyLabel)
        DetailMetadataDivider()
        DetailMetadataRow(label = "Modalidade", value = detail.modalityLabel ?: "Não aplicável / Aguardando coleta")
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
