package com.nexus.app.ui.market

import com.nexus.app.core.DateTimeUtils
import com.nexus.app.domain.model.AssetCategory
import com.nexus.app.domain.model.MarketAlertCondition
import com.nexus.app.domain.model.MarketAlertRules
import com.nexus.app.domain.model.MarketAlertStatus
import com.nexus.app.domain.model.MarketAssetCatalog
import com.nexus.app.domain.model.MarketHistoryUiState
import com.nexus.app.domain.model.MarketPriceAlert
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

data class MarketAssetRowPresentation(
    val descriptor: MarketAssetCatalog.AssetDescriptor,
    val uiState: MarketQuoteUiState,
    val pairLabel: String,
    val ticker: String,
    val exchangeLabel: String?,
    val originalCurrency: String,
    val formattedPrice: String,
    val formattedConvertedBrl: String?,
    val conversionUnavailableNote: String?,
    val changePercent: Double?,
    val formattedChangeWithPeriod: String?,
    val trend: QuoteTrend,
    val modalityLabel: String?,
    val isDelayed: Boolean,
    val delayNotice: String?,
    val sourceName: String?,
    val formattedSourceTime: String?,
    val statusNote: String?,
    val statusSeverity: MarketStatusSeverity,
    val isFavorite: Boolean = false,
    val quoteStatusLabel: String = "Indisponível",
    val unitOrCurrencyLabel: String = originalCurrency
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
    val indices: List<MarketAssetRowPresentation>,
    val stocks: List<MarketAssetRowPresentation>,
    val watchlist: List<MarketAssetRowPresentation> = emptyList()
)

data class MarketAssetDetailPresentation(
    val descriptor: MarketAssetCatalog.AssetDescriptor,
    val uiState: MarketQuoteUiState,
    val ticker: String,
    val exchangeLabel: String?,
    val pairLabel: String,
    val categoryLabel: String,
    val originalCurrency: String,
    val originalCurrencyLabel: String,
    val referenceCurrencyLabel: String,
    val formattedPrice: String,
    val formattedConvertedBrl: String?,
    val convertedBrlDetailNote: String?,
    val formattedBidPrice: String?,
    val formattedAskPrice: String?,
    val formattedPreviousClose: String?,
    val changePercent: Double?,
    val formattedChangeWithPeriod: String?,
    val variationPeriodLabel: String?,
    val trend: QuoteTrend,
    val modalityLabel: String?,
    val isDelayed: Boolean,
    val delayNotice: String?,
    val sourceName: String?,
    val formattedSourceTimestamp: String?,
    val formattedFetchedAt: String?,
    val cacheStatusLabel: String,
    val connectivityStatusLabel: String,
    val statusSeverity: MarketStatusSeverity,
    val hasHistoricalSeries: Boolean,
    val historicalStatusMessage: String,
    val isFavorite: Boolean = false
)

data class MarketAlertRowPresentation(
    val alert: MarketPriceAlert,
    val descriptor: MarketAssetCatalog.AssetDescriptor,
    val unitOrCurrencyLabel: String,
    val conditionSummary: String,
    val formattedTargetPrice: String,
    val formattedCurrentPrice: String,
    val hasCurrentQuote: Boolean,
    val statusLabel: String,
    val formattedCreatedAt: String,
    val triggeredSummary: String?,
    val awaitingResetNote: String?
)

object MarketPresentationMapper {

    fun buildOverview(
        states: Map<String, MarketQuoteUiState>,
        favoriteCodes: List<String> = emptyList()
    ): MarketOverviewPresentation {
        val usdBrlState = states[MarketAssetCatalog.USD_BRL_CODE]
        val favoriteSet = favoriteCodes.toSet()

        val currencyRows = MarketAssetCatalog.CURRENCY_ASSETS.map { descriptor ->
            toRowPresentation(
                descriptor = descriptor,
                state = states[descriptor.code] ?: MarketQuoteUiState(assetCode = descriptor.code),
                usdBrlState = usdBrlState,
                isFavorite = descriptor.code in favoriteSet
            )
        }
        val cryptoRows = MarketAssetCatalog.CRYPTO_ASSETS.map { descriptor ->
            toRowPresentation(
                descriptor = descriptor,
                state = states[descriptor.code] ?: MarketQuoteUiState(assetCode = descriptor.code),
                usdBrlState = usdBrlState,
                isFavorite = descriptor.code in favoriteSet
            )
        }
        val indexRows = MarketAssetCatalog.INDEX_ASSETS.map { descriptor ->
            toRowPresentation(
                descriptor = descriptor,
                state = states[descriptor.code] ?: MarketQuoteUiState(assetCode = descriptor.code),
                usdBrlState = usdBrlState,
                isFavorite = descriptor.code in favoriteSet
            )
        }
        val stockRows = MarketAssetCatalog.STOCK_ASSETS.map { descriptor ->
            toRowPresentation(
                descriptor = descriptor,
                state = states[descriptor.code] ?: MarketQuoteUiState(assetCode = descriptor.code),
                usdBrlState = usdBrlState,
                isFavorite = descriptor.code in favoriteSet
            )
        }
        val allRows = currencyRows + cryptoRows + indexRows + stockRows
        val rowsByCode = allRows.associateBy { it.descriptor.code }
        val watchlistRows = favoriteCodes
            .distinct()
            .mapNotNull { code -> rowsByCode[code] }

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
            indices = indexRows,
            stocks = stockRows,
            watchlist = watchlistRows
        )
    }

    fun buildDetail(
        descriptor: MarketAssetCatalog.AssetDescriptor,
        state: MarketQuoteUiState,
        usdBrlState: MarketQuoteUiState? = null,
        historyState: MarketHistoryUiState? = null,
        isFavorite: Boolean = false
    ): MarketAssetDetailPresentation {
        val quote = state.quote
        val originalCurrency = quote?.quoteCurrency?.takeIf { it.isNotBlank() } ?: descriptor.quoteSymbol
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

        val needsUsdToBrlConversion =
            descriptor.category == AssetCategory.STOCK && originalCurrency.equals("USD", ignoreCase = true)
        val conversion = if (quote != null && needsUsdToBrlConversion) {
            MarketQuoteCalculations.convertUsdToBrl(quote.price, usdBrlState)
        } else {
            null
        }
        val formattedConvertedBrl = conversion?.let {
            MarketQuoteCalculations.formatPrice(it.amountBrl, AssetCategory.STOCK, "BRL")
        }
        val convertedBrlDetailNote = when {
            !needsUsdToBrlConversion -> null
            quote == null -> null
            conversion != null -> {
                val rateFormatted = MarketQuoteCalculations.formatBrlRate(conversion.rateUsed)
                val cacheTag = if (conversion.rateIsStale || conversion.rateIsOffline) " · câmbio em cache" else ""
                "Taxa USD/BRL: $rateFormatted (${conversion.rateSourceName}$cacheTag)"
            }
            else -> "Conversão em BRL indisponível (sem cotação USD/BRL válida)"
        }

        val originalCurrencyLabel = formatOriginalCurrencyLabel(descriptor.category, originalCurrency)
        val resolvedExchange = quote?.exchange?.takeIf { it.isNotBlank() } ?: descriptor.exchange

        return MarketAssetDetailPresentation(
            descriptor = descriptor,
            uiState = state,
            ticker = descriptor.ticker,
            exchangeLabel = resolvedExchange,
            pairLabel = formatAssetIdentifierLabel(descriptor, originalCurrency),
            categoryLabel = descriptor.category.displayLabel,
            originalCurrency = originalCurrency,
            originalCurrencyLabel = originalCurrencyLabel,
            referenceCurrencyLabel = originalCurrencyLabel,
            formattedPrice = quote?.let {
                MarketQuoteCalculations.formatPrice(it.price, descriptor.category, originalCurrency)
            } ?: MarketQuoteCalculations.formatEmptyPlaceholder(descriptor.category, originalCurrency),
            formattedConvertedBrl = formattedConvertedBrl,
            convertedBrlDetailNote = convertedBrlDetailNote,
            formattedBidPrice = quote?.bidPrice?.let {
                MarketQuoteCalculations.formatPrice(it, descriptor.category, originalCurrency)
            },
            formattedAskPrice = quote?.askPrice?.let {
                MarketQuoteCalculations.formatPrice(it, descriptor.category, originalCurrency)
            },
            formattedPreviousClose = quote?.previousClose?.let {
                MarketQuoteCalculations.formatPrice(it, descriptor.category, originalCurrency)
            },
            changePercent = changePct,
            formattedChangeWithPeriod = formattedChange,
            variationPeriodLabel = quote?.variationPeriodLabel,
            trend = quote?.trend ?: QuoteTrend.UNKNOWN,
            modalityLabel = quote?.modality?.labelFor(descriptor.code),
            isDelayed = quote?.isDelayed == true,
            delayNotice = quote?.delayNotice,
            sourceName = quote?.sourceName,
            formattedSourceTimestamp = quote?.sourceTimestampEpochMs?.let(DateTimeUtils::formatDateTime),
            formattedFetchedAt = quote?.fetchedAtEpochMs?.let(DateTimeUtils::formatDateTime),
            cacheStatusLabel = cacheStatusLabel,
            connectivityStatusLabel = connectivityStatusLabel,
            statusSeverity = severity,
            hasHistoricalSeries = (historyState?.series?.points?.size ?: 0) >= 2,
            historicalStatusMessage = historyState?.statusMessage
                ?: if ((historyState?.series?.points?.size ?: 0) >= 2) {
                    "Série histórica local disponível."
                } else {
                    "Série histórica aguardando coleta para este intervalo."
                },
            isFavorite = isFavorite
        )
    }

    private fun toRowPresentation(
        descriptor: MarketAssetCatalog.AssetDescriptor,
        state: MarketQuoteUiState,
        usdBrlState: MarketQuoteUiState?,
        isFavorite: Boolean
    ): MarketAssetRowPresentation {
        val quote = state.quote
        val originalCurrency = quote?.quoteCurrency?.takeIf { it.isNotBlank() } ?: descriptor.quoteSymbol
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

        val needsUsdToBrlConversion =
            descriptor.category == AssetCategory.STOCK && originalCurrency.equals("USD", ignoreCase = true)
        val conversion = if (quote != null && needsUsdToBrlConversion) {
            MarketQuoteCalculations.convertUsdToBrl(quote.price, usdBrlState)
        } else {
            null
        }
        val formattedConvertedBrl = conversion?.let {
            MarketQuoteCalculations.formatPrice(it.amountBrl, AssetCategory.STOCK, "BRL")
        }
        val conversionUnavailableNote = if (quote != null && needsUsdToBrlConversion && conversion == null) {
            "Conversão em BRL indisponível (sem USD/BRL válido)"
        } else {
            null
        }

        return MarketAssetRowPresentation(
            descriptor = descriptor,
            uiState = state,
            pairLabel = formatAssetIdentifierLabel(descriptor, originalCurrency),
            ticker = descriptor.ticker,
            exchangeLabel = quote?.exchange?.takeIf { it.isNotBlank() } ?: descriptor.exchange,
            originalCurrency = originalCurrency,
            formattedPrice = quote?.let {
                MarketQuoteCalculations.formatPrice(it.price, descriptor.category, originalCurrency)
            } ?: MarketQuoteCalculations.formatEmptyPlaceholder(descriptor.category, originalCurrency),
            formattedConvertedBrl = formattedConvertedBrl,
            conversionUnavailableNote = conversionUnavailableNote,
            changePercent = changePct,
            formattedChangeWithPeriod = formattedChange,
            trend = quote?.trend ?: QuoteTrend.UNKNOWN,
            modalityLabel = quote?.modality?.labelFor(descriptor.code),
            isDelayed = quote?.isDelayed == true,
            delayNotice = quote?.delayNotice,
            sourceName = quote?.sourceName,
            formattedSourceTime = quote?.sourceTimestampEpochMs?.let(DateTimeUtils::formatDateTime),
            statusNote = statusNote,
            statusSeverity = severity,
            isFavorite = isFavorite,
            quoteStatusLabel = resolveQuoteStatusLabel(state),
            unitOrCurrencyLabel = if (descriptor.category == AssetCategory.INDEX) {
                "Pontos (${originalCurrency.uppercase()})"
            } else {
                originalCurrency.uppercase()
            }
        )
    }

    private fun resolveQuoteStatusLabel(state: MarketQuoteUiState): String = when {
        state.quote == null && state.isOffline -> "Offline"
        state.quote == null -> "Indisponível"
        state.isOffline -> "Offline"
        state.isStale || state.lastRefreshFailed -> "Desatualizada"
        else -> "Atualizada"
    }

    private fun formatAssetIdentifierLabel(
        descriptor: MarketAssetCatalog.AssetDescriptor,
        originalCurrency: String
    ): String = when (descriptor.category) {
        AssetCategory.CURRENCY, AssetCategory.CRYPTO -> "${descriptor.baseSymbol}/${descriptor.quoteSymbol}"
        AssetCategory.INDEX -> "${descriptor.ticker} · Pontos"
        AssetCategory.STOCK -> "${descriptor.ticker} · ${originalCurrency.uppercase()}"
    }

    private fun formatOriginalCurrencyLabel(
        category: AssetCategory,
        currencyCode: String
    ): String {
        val code = currencyCode.uppercase()
        return when {
            category == AssetCategory.INDEX -> "Pontos (mercado $code)"
            code == "BRL" -> "Real Brasileiro (BRL)"
            code == "USD" -> "Dólar Americano (USD)"
            code == "EUR" -> "Euro (EUR)"
            else -> code
        }
    }

    private fun resolveSeverity(state: MarketQuoteUiState): MarketStatusSeverity = when {
        state.quote == null -> MarketStatusSeverity.EMPTY
        state.isOffline || state.lastRefreshFailed -> MarketStatusSeverity.WARNING
        state.isStale -> MarketStatusSeverity.STALE
        else -> MarketStatusSeverity.OK
    }

    fun buildAlertRows(
        alerts: List<MarketPriceAlert>,
        states: Map<String, MarketQuoteUiState>
    ): List<MarketAlertRowPresentation> {
        return alerts.mapNotNull { alert ->
            val descriptor = MarketAssetCatalog.findByCode(alert.assetCode) ?: return@mapNotNull null
            val quote = states[descriptor.code]?.quote
            val formattedTarget = MarketAlertRules.formatValueForAsset(alert.targetPrice, descriptor)
            val formattedCurrent = quote?.let {
                MarketAlertRules.formatValueForAsset(it.price, descriptor)
            } ?: MarketQuoteCalculations.formatEmptyPlaceholder(
                category = descriptor.category,
                currencyCode = descriptor.quoteSymbol
            )
            val triggeredSummary = if (alert.status == MarketAlertStatus.TRIGGERED && alert.triggeredAtEpochMs != null) {
                val whenStr = DateTimeUtils.formatDateTime(alert.triggeredAtEpochMs)
                val priceStr = alert.triggeredPrice?.let {
                    MarketAlertRules.formatValueForAsset(it, descriptor)
                }
                if (priceStr != null) {
                    "Disparado em $whenStr · $priceStr"
                } else {
                    "Disparado em $whenStr"
                }
            } else {
                null
            }
            val awaitingResetNote = if (alert.status == MarketAlertStatus.WAITING && alert.awaitingConditionReset) {
                when (alert.condition) {
                    MarketAlertCondition.PRICE_ABOVE ->
                        "Aguardando preço recuar abaixo do alvo para rearmar"
                    MarketAlertCondition.PRICE_BELOW ->
                        "Aguardando preço subir acima do alvo para rearmar"
                }
            } else {
                null
            }

            MarketAlertRowPresentation(
                alert = alert,
                descriptor = descriptor,
                unitOrCurrencyLabel = MarketAlertRules.unitLabelFor(descriptor),
                conditionSummary = "${alert.condition.shortLabel} $formattedTarget",
                formattedTargetPrice = formattedTarget,
                formattedCurrentPrice = formattedCurrent,
                hasCurrentQuote = quote != null,
                statusLabel = alert.status.label,
                formattedCreatedAt = DateTimeUtils.formatDateTime(alert.createdAtEpochMs),
                triggeredSummary = triggeredSummary,
                awaitingResetNote = awaitingResetNote
            )
        }
    }
}
