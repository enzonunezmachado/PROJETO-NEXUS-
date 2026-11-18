package com.nexus.app.ui.market

import com.nexus.app.core.DateTimeUtils
import com.nexus.app.domain.model.AssetCategory
import com.nexus.app.domain.model.MarketAssetCatalog
import com.nexus.app.domain.model.MarketQuoteCalculations
import com.nexus.app.domain.model.MarketQuoteUiState
import com.nexus.app.domain.model.QuoteTrend

enum class MarketFilterCategory(val label: String) {
    ALL("Todos"),
    CURRENCY("Moedas"),
    CRYPTO("Cripto"),
    INDEX("Índices"),
    STOCK("Ações")
}

data class PlannedAssetPreview(
    val name: String,
    val tickerHint: String,
    val marketHint: String
)

data class PlannedCategorySection(
    val category: AssetCategory,
    val title: String,
    val description: String,
    val plannedExamples: List<PlannedAssetPreview>
)

data class MarketAssetRowPresentation(
    val descriptor: MarketAssetCatalog.AssetDescriptor,
    val uiState: MarketQuoteUiState,
    val pairLabel: String,
    val formattedPrice: String,
    val changePercent: Double?,
    val formattedChangeWithPeriod: String?,
    val trend: QuoteTrend,
    val modalityLabel: String?,
    val sourceName: String?,
    val formattedSourceTime: String?,
    val statusNote: String?,
    val statusSeverity: MarketStatusSeverity
)

enum class MarketStatusSeverity {
    OK,
    STALE,
    WARNING,
    EMPTY
}

data class MarketOverviewPresentation(
    val activeAssetCount: Int,
    val loadedAssetCount: Int,
    val staleOrFailedCount: Int,
    val isAnyRefreshing: Boolean,
    val isOffline: Boolean,
    val formattedLastSyncAt: String?,
    val currencies: List<MarketAssetRowPresentation>,
    val cryptos: List<MarketAssetRowPresentation>,
    val plannedSections: List<PlannedCategorySection>
)

data class MarketAssetDetailPresentation(
    val descriptor: MarketAssetCatalog.AssetDescriptor,
    val uiState: MarketQuoteUiState,
    val pairLabel: String,
    val categoryLabel: String,
    val referenceCurrencyLabel: String,
    val formattedPrice: String,
    val formattedBidPrice: String?,
    val formattedAskPrice: String?,
    val formattedPreviousClose: String?,
    val changePercent: Double?,
    val formattedChangeWithPeriod: String?,
    val variationPeriodLabel: String?,
    val trend: QuoteTrend,
    val modalityLabel: String?,
    val sourceName: String?,
    val formattedSourceTimestamp: String?,
    val formattedFetchedAt: String?,
    val cacheStatusLabel: String,
    val connectivityStatusLabel: String,
    val statusSeverity: MarketStatusSeverity,
    val hasHistoricalSeries: Boolean,
    val historicalStatusMessage: String
)

object MarketPresentationMapper {

    val PLANNED_CATEGORIES: List<PlannedCategorySection> = listOf(
        PlannedCategorySection(
            category = AssetCategory.INDEX,
            title = "Índices",
            description = "Acompanhamento de índices de referência em etapa futura, sem dados simulados.",
            plannedExamples = listOf(
                PlannedAssetPreview("Ibovespa", "IBOV", "Brasil (B3)"),
                PlannedAssetPreview("S&P 500", "SPX", "EUA"),
                PlannedAssetPreview("Nasdaq Composite", "IXIC", "EUA")
            )
        ),
        PlannedCategorySection(
            category = AssetCategory.STOCK,
            title = "Ações",
            description = "Estrutura reservada para ações brasileiras e internacionais em lote posterior.",
            plannedExamples = listOf(
                PlannedAssetPreview("Petrobras", "PETR4", "B3"),
                PlannedAssetPreview("Vale", "VALE3", "B3"),
                PlannedAssetPreview("Itaú Unibanco", "ITUB4", "B3"),
                PlannedAssetPreview("Mercado Livre", "MELI", "Nasdaq / BDR")
            )
        )
    )

    fun buildOverview(
        states: Map<String, MarketQuoteUiState>
    ): MarketOverviewPresentation {
        val currencyRows = MarketAssetCatalog.CURRENCY_ASSETS.map { descriptor ->
            toRowPresentation(
                descriptor = descriptor,
                state = states[descriptor.code] ?: MarketQuoteUiState(assetCode = descriptor.code)
            )
        }
        val cryptoRows = MarketAssetCatalog.CRYPTO_ASSETS.map { descriptor ->
            toRowPresentation(
                descriptor = descriptor,
                state = states[descriptor.code] ?: MarketQuoteUiState(assetCode = descriptor.code)
            )
        }
        val allRows = currencyRows + cryptoRows
        val loadedCount = allRows.count { it.uiState.quote != null }
        val staleOrFailedCount = allRows.count {
            it.uiState.isStale || it.uiState.lastRefreshFailed
        }
        val anyRefreshing = allRows.any { it.uiState.isRefreshing }
        val offline = allRows.any { it.uiState.isOffline }
        val latestFetchedEpoch = allRows.mapNotNull { it.uiState.quote?.fetchedAtEpochMs }.maxOrNull()

        return MarketOverviewPresentation(
            activeAssetCount = allRows.size,
            loadedAssetCount = loadedCount,
            staleOrFailedCount = staleOrFailedCount,
            isAnyRefreshing = anyRefreshing,
            isOffline = offline,
            formattedLastSyncAt = latestFetchedEpoch?.let(DateTimeUtils::formatDateTime),
            currencies = currencyRows,
            cryptos = cryptoRows,
            plannedSections = PLANNED_CATEGORIES
        )
    }

    fun buildDetail(
        descriptor: MarketAssetCatalog.AssetDescriptor,
        state: MarketQuoteUiState
    ): MarketAssetDetailPresentation {
        val quote = state.quote
        val changePct = quote?.changePercent
        val formattedChange = changePct?.let { pct ->
            val base = MarketQuoteCalculations.formatSignedPercent(pct)
            val period = quote.variationPeriodLabel
            if (!period.isNullOrBlank()) "$base ($period)" else base
        }

        val severity = resolveSeverity(state)
        val cacheStatusLabel = when {
            quote == null && state.isRefreshing -> "Consultando cotação..."
            quote == null -> "Sem cache local salvo"
            state.lastRefreshFailed -> "Cache preservado (última tentativa falhou)"
            state.isStale -> "Cache desatualizado (> 30 min)"
            else -> "Cache atualizado"
        }
        val connectivityStatusLabel = when {
            state.isOffline -> "Offline"
            state.lastRefreshFailed -> "Online com falha recente na fonte"
            else -> "Online"
        }

        return MarketAssetDetailPresentation(
            descriptor = descriptor,
            uiState = state,
            pairLabel = "${descriptor.baseSymbol}/${descriptor.quoteSymbol}",
            categoryLabel = descriptor.category.displayLabel,
            referenceCurrencyLabel = "Real Brasileiro (${descriptor.quoteSymbol})",
            formattedPrice = quote?.let {
                MarketQuoteCalculations.formatPrice(it.price, descriptor.category)
            } ?: "R$ --",
            formattedBidPrice = quote?.bidPrice?.let {
                MarketQuoteCalculations.formatPrice(it, descriptor.category)
            },
            formattedAskPrice = quote?.askPrice?.let {
                MarketQuoteCalculations.formatPrice(it, descriptor.category)
            },
            formattedPreviousClose = quote?.previousClose?.let {
                MarketQuoteCalculations.formatPrice(it, descriptor.category)
            },
            changePercent = changePct,
            formattedChangeWithPeriod = formattedChange,
            variationPeriodLabel = quote?.variationPeriodLabel,
            trend = quote?.trend ?: QuoteTrend.UNKNOWN,
            modalityLabel = quote?.modality?.labelFor(descriptor.code),
            sourceName = quote?.sourceName,
            formattedSourceTimestamp = quote?.sourceTimestampEpochMs?.let(DateTimeUtils::formatDateTime),
            formattedFetchedAt = quote?.fetchedAtEpochMs?.let(DateTimeUtils::formatDateTime),
            cacheStatusLabel = cacheStatusLabel,
            connectivityStatusLabel = connectivityStatusLabel,
            statusSeverity = severity,
            hasHistoricalSeries = false,
            historicalStatusMessage = "Série histórica local e gráfico de evolução ainda não estão armazenados nesta versão. Esta seção receberá o histórico em uma etapa posterior."
        )
    }

    private fun toRowPresentation(
        descriptor: MarketAssetCatalog.AssetDescriptor,
        state: MarketQuoteUiState
    ): MarketAssetRowPresentation {
        val quote = state.quote
        val changePct = quote?.changePercent
        val formattedChange = changePct?.let { pct ->
            val base = MarketQuoteCalculations.formatSignedPercent(pct)
            val period = quote.variationPeriodLabel
            if (!period.isNullOrBlank()) "$base ($period)" else base
        }
        val severity = resolveSeverity(state)
        val statusNote = when {
            quote == null && state.isRefreshing -> "Consultando cotação..."
            quote == null -> state.statusMessage ?: "Aguardando primeira consulta"
            state.isOffline -> state.statusMessage ?: "Offline · Exibindo último cache válido"
            state.lastRefreshFailed -> state.statusMessage ?: "Falha na atualização · Cache preservado"
            state.isStale -> "Cache desatualizado · Toque para atualizar"
            else -> null
        }

        return MarketAssetRowPresentation(
            descriptor = descriptor,
            uiState = state,
            pairLabel = "${descriptor.baseSymbol}/${descriptor.quoteSymbol}",
            formattedPrice = quote?.let {
                MarketQuoteCalculations.formatPrice(it.price, descriptor.category)
            } ?: "R$ --",
            changePercent = changePct,
            formattedChangeWithPeriod = formattedChange,
            trend = quote?.trend ?: QuoteTrend.UNKNOWN,
            modalityLabel = quote?.modality?.labelFor(descriptor.code),
            sourceName = quote?.sourceName,
            formattedSourceTime = quote?.sourceTimestampEpochMs?.let(DateTimeUtils::formatDateTime),
            statusNote = statusNote,
            statusSeverity = severity
        )
    }

    private fun resolveSeverity(state: MarketQuoteUiState): MarketStatusSeverity = when {
        state.quote == null -> MarketStatusSeverity.EMPTY
        state.isOffline || state.lastRefreshFailed -> MarketStatusSeverity.WARNING
        state.isStale -> MarketStatusSeverity.STALE
        else -> MarketStatusSeverity.OK
    }
}
