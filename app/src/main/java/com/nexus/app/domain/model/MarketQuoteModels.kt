package com.nexus.app.domain.model

import com.nexus.app.core.DateTimeUtils
import kotlinx.serialization.Serializable
import java.text.NumberFormat
import kotlin.math.abs

enum class AssetCategory(val displayLabel: String) {
    CURRENCY("Moeda"),
    CRYPTO("Criptomoeda"),
    INDEX("Índice"),
    STOCK("Ação")
}

/**
 * Modalidade efetiva da cotação obtida.
 * Nunca misturar ou rotular PTAX/Turismo/Referência como Comercial.
 */
enum class QuoteModality(val displayLabel: String) {
    COMMERCIAL("Dólar Comercial"),
    TOURISM("Dólar Turismo"),
    PTAX("Dólar PTAX (BCB)"),
    SPOT("Mercado à Vista"),
    REFERENCE("Taxa de Referência"),
    EXCHANGE("Pregão"),
    DELAYED("Com atraso"),
    INDEX("Índice");

    fun labelFor(assetCode: String): String = when (this) {
        COMMERCIAL -> when (assetCode) {
            MarketAssetCatalog.USD_BRL_CODE -> "Dólar Comercial"
            MarketAssetCatalog.EUR_BRL_CODE -> "Euro Comercial"
            else -> "Comercial"
        }
        TOURISM -> when (assetCode) {
            MarketAssetCatalog.USD_BRL_CODE -> "Dólar Turismo"
            MarketAssetCatalog.EUR_BRL_CODE -> "Euro Turismo"
            else -> "Turismo"
        }
        PTAX -> when (assetCode) {
            MarketAssetCatalog.USD_BRL_CODE -> "Dólar PTAX (BCB)"
            MarketAssetCatalog.EUR_BRL_CODE -> "Euro PTAX (BCB)"
            else -> "PTAX (BCB)"
        }
        SPOT -> "Spot (24h)"
        REFERENCE -> "Taxa de Referência"
        EXCHANGE -> "Pregão"
        DELAYED -> "Com atraso"
        INDEX -> "Índice"
    }
}

enum class QuoteTrend {
    UP,
    DOWN,
    FLAT,
    UNKNOWN
}

@Serializable
data class MarketQuoteSnapshot(
    val assetCode: String,
    val category: AssetCategory = AssetCategory.CURRENCY,
    val baseCurrency: String = "USD",
    val quoteCurrency: String = "BRL",
    val modality: QuoteModality,
    val price: Double,
    val bidPrice: Double? = null,
    val askPrice: Double? = null,
    val previousClose: Double? = null,
    val changePercent: Double? = null,
    val variationPeriodLabel: String? = null,
    val trend: QuoteTrend = QuoteTrend.UNKNOWN,
    val sourceName: String,
    val sourceTimestampEpochMs: Long,
    val fetchedAtEpochMs: Long,
    val exchange: String? = null,
    val isDelayed: Boolean = false,
    val delayNotice: String? = null
)

data class MarketQuoteUiState(
    val assetCode: String = MarketAssetCatalog.USD_BRL_CODE,
    val quote: MarketQuoteSnapshot? = null,
    val isRefreshing: Boolean = false,
    val isStale: Boolean = false,
    val isOffline: Boolean = false,
    val lastRefreshFailed: Boolean = false,
    val statusMessage: String? = null,
    val lastAttemptAtEpochMs: Long? = null
)

data class ConvertedBrlPrice(
    val amountBrl: Double,
    val rateUsed: Double,
    val rateIsStale: Boolean,
    val rateIsOffline: Boolean,
    val rateSourceName: String
)

object MarketAssetCatalog {
    const val USD_BRL_CODE = "USD-BRL"
    const val EUR_BRL_CODE = "EUR-BRL"
    const val BTC_BRL_CODE = "BTC-BRL"
    const val ETH_BRL_CODE = "ETH-BRL"
    const val SOL_BRL_CODE = "SOL-BRL"

    const val IBOV_CODE = "IBOV"
    const val SPX_CODE = "SPX"
    const val IXIC_CODE = "IXIC"

    const val PETR4_CODE = "PETR4"
    const val VALE3_CODE = "VALE3"
    const val ITUB4_CODE = "ITUB4"
    const val MELI_CODE = "MELI"

    data class AssetDescriptor(
        val code: String,
        val baseSymbol: String,
        val quoteSymbol: String,
        val displayName: String,
        val category: AssetCategory,
        val ticker: String = baseSymbol,
        val exchange: String? = null,
        val isPointsUnit: Boolean = (category == AssetCategory.INDEX)
    )

    val USD_BRL = AssetDescriptor(
        code = USD_BRL_CODE,
        baseSymbol = "USD",
        quoteSymbol = "BRL",
        displayName = "Dólar (USD/BRL)",
        category = AssetCategory.CURRENCY
    )

    val EUR_BRL = AssetDescriptor(
        code = EUR_BRL_CODE,
        baseSymbol = "EUR",
        quoteSymbol = "BRL",
        displayName = "Euro (EUR/BRL)",
        category = AssetCategory.CURRENCY
    )

    val BTC_BRL = AssetDescriptor(
        code = BTC_BRL_CODE,
        baseSymbol = "BTC",
        quoteSymbol = "BRL",
        displayName = "Bitcoin",
        category = AssetCategory.CRYPTO
    )

    val ETH_BRL = AssetDescriptor(
        code = ETH_BRL_CODE,
        baseSymbol = "ETH",
        quoteSymbol = "BRL",
        displayName = "Ethereum",
        category = AssetCategory.CRYPTO
    )

    val SOL_BRL = AssetDescriptor(
        code = SOL_BRL_CODE,
        baseSymbol = "SOL",
        quoteSymbol = "BRL",
        displayName = "Solana",
        category = AssetCategory.CRYPTO
    )

    val IBOV = AssetDescriptor(
        code = IBOV_CODE,
        baseSymbol = "IBOV",
        quoteSymbol = "BRL",
        displayName = "Ibovespa",
        category = AssetCategory.INDEX,
        ticker = "IBOV",
        exchange = "B3",
        isPointsUnit = true
    )

    val SPX = AssetDescriptor(
        code = SPX_CODE,
        baseSymbol = "SPX",
        quoteSymbol = "USD",
        displayName = "S&P 500",
        category = AssetCategory.INDEX,
        ticker = "SPX",
        exchange = "NYSE / S&P",
        isPointsUnit = true
    )

    val IXIC = AssetDescriptor(
        code = IXIC_CODE,
        baseSymbol = "IXIC",
        quoteSymbol = "USD",
        displayName = "Nasdaq Composite",
        category = AssetCategory.INDEX,
        ticker = "IXIC",
        exchange = "Nasdaq",
        isPointsUnit = true
    )

    val PETR4 = AssetDescriptor(
        code = PETR4_CODE,
        baseSymbol = "PETR4",
        quoteSymbol = "BRL",
        displayName = "Petrobras",
        category = AssetCategory.STOCK,
        ticker = "PETR4",
        exchange = "B3"
    )

    val VALE3 = AssetDescriptor(
        code = VALE3_CODE,
        baseSymbol = "VALE3",
        quoteSymbol = "BRL",
        displayName = "Vale",
        category = AssetCategory.STOCK,
        ticker = "VALE3",
        exchange = "B3"
    )

    val ITUB4 = AssetDescriptor(
        code = ITUB4_CODE,
        baseSymbol = "ITUB4",
        quoteSymbol = "BRL",
        displayName = "Itaú Unibanco",
        category = AssetCategory.STOCK,
        ticker = "ITUB4",
        exchange = "B3"
    )

    val MELI = AssetDescriptor(
        code = MELI_CODE,
        baseSymbol = "MELI",
        quoteSymbol = "USD",
        displayName = "Mercado Livre",
        category = AssetCategory.STOCK,
        ticker = "MELI",
        exchange = "Nasdaq"
    )

    val CURRENCY_ASSETS: List<AssetDescriptor> = listOf(USD_BRL, EUR_BRL)
    val CRYPTO_ASSETS: List<AssetDescriptor> = listOf(BTC_BRL, ETH_BRL, SOL_BRL)
    val INDEX_ASSETS: List<AssetDescriptor> = listOf(IBOV, SPX, IXIC)
    val STOCK_ASSETS: List<AssetDescriptor> = listOf(PETR4, VALE3, ITUB4, MELI)
    val SUPPORTED_ASSETS: List<AssetDescriptor> =
        CURRENCY_ASSETS + CRYPTO_ASSETS + INDEX_ASSETS + STOCK_ASSETS

    fun findByCode(code: String): AssetDescriptor? =
        SUPPORTED_ASSETS.firstOrNull { it.code == code }
}

object MarketQuoteCalculations {
    const val DEFAULT_STALE_TTL_MS: Long = 30L * 60L * 1000L
    const val AUTO_REFRESH_COOLDOWN_MS: Long = 15L * 60L * 1000L
    const val FAILURE_RETRY_COOLDOWN_MS: Long = 60L * 1000L
    private const val FLAT_THRESHOLD_PERCENT = 0.005

    fun isValidPositiveRate(value: Double?): Boolean =
        value != null && value.isFinite() && value > 0.0

    fun computeChangePercent(currentPrice: Double, previousPrice: Double?): Double? {
        if (!isValidPositiveRate(currentPrice) || !isValidPositiveRate(previousPrice)) {
            return null
        }
        val prev = previousPrice!!
        val pct = ((currentPrice - prev) / prev) * 100.0
        return pct.takeIf { it.isFinite() }
    }

    fun computeTrend(changePercent: Double?): QuoteTrend {
        if (changePercent == null || !changePercent.isFinite()) return QuoteTrend.UNKNOWN
        return when {
            changePercent > FLAT_THRESHOLD_PERCENT -> QuoteTrend.UP
            changePercent < -FLAT_THRESHOLD_PERCENT -> QuoteTrend.DOWN
            else -> QuoteTrend.FLAT
        }
    }

    fun isQuoteStale(
        quote: MarketQuoteSnapshot,
        nowEpochMs: Long,
        maxAgeMs: Long = DEFAULT_STALE_TTL_MS
    ): Boolean {
        if (quote.fetchedAtEpochMs <= 0L || nowEpochMs < quote.fetchedAtEpochMs) return true
        return (nowEpochMs - quote.fetchedAtEpochMs) >= maxAgeMs
    }

    fun formatBrlRate(value: Double): String {
        if (!value.isFinite() || value <= 0.0) return "R$ --"
        val formatter = NumberFormat.getNumberInstance(DateTimeUtils.LOCALE_PT_BR).apply {
            minimumFractionDigits = 4
            maximumFractionDigits = 4
        }
        return "R$ ${formatter.format(value)}"
    }

    fun formatPrice(value: Double, category: AssetCategory): String {
        return formatPrice(value = value, category = category, currencyCode = "BRL")
    }

    fun formatPrice(value: Double, category: AssetCategory, currencyCode: String): String {
        if (!value.isFinite() || value <= 0.0) {
            return when {
                category == AssetCategory.INDEX -> "-- pts"
                currencyCode.equals("USD", ignoreCase = true) -> "US$ --"
                currencyCode.equals("BRL", ignoreCase = true) -> "R$ --"
                else -> "${currencyCode.uppercase()} --"
            }
        }
        if (category == AssetCategory.INDEX) {
            val formatter = NumberFormat.getNumberInstance(DateTimeUtils.LOCALE_PT_BR).apply {
                minimumFractionDigits = 2
                maximumFractionDigits = 2
            }
            return "${formatter.format(value)} pts"
        }
        val decimals = when (category) {
            AssetCategory.CURRENCY -> 4
            AssetCategory.CRYPTO -> if (value >= 10.0) 2 else 4
            else -> 2
        }
        val formatter = NumberFormat.getNumberInstance(DateTimeUtils.LOCALE_PT_BR).apply {
            minimumFractionDigits = decimals
            maximumFractionDigits = decimals
        }
        val prefix = when (currencyCode.uppercase()) {
            "BRL" -> "R$"
            "USD" -> "US$"
            "EUR" -> "€"
            else -> currencyCode.uppercase()
        }
        return "$prefix ${formatter.format(value)}"
    }

    fun formatEmptyPlaceholder(category: AssetCategory, currencyCode: String): String {
        return when {
            category == AssetCategory.INDEX -> "-- pts"
            currencyCode.equals("USD", ignoreCase = true) -> "US$ --"
            currencyCode.equals("BRL", ignoreCase = true) -> "R$ --"
            else -> "${currencyCode.uppercase()} --"
        }
    }

    fun convertUsdToBrl(
        usdAmount: Double,
        usdBrlState: MarketQuoteUiState?
    ): ConvertedBrlPrice? {
        if (!isValidPositiveRate(usdAmount)) return null
        val fxQuote = usdBrlState?.quote ?: return null
        if (!isValidPositiveRate(fxQuote.price)) return null
        val converted = usdAmount * fxQuote.price
        if (!converted.isFinite() || converted <= 0.0) return null
        return ConvertedBrlPrice(
            amountBrl = converted,
            rateUsed = fxQuote.price,
            rateIsStale = usdBrlState.isStale,
            rateIsOffline = usdBrlState.isOffline,
            rateSourceName = fxQuote.sourceName
        )
    }

    fun formatSignedPercent(changePercent: Double): String {
        if (!changePercent.isFinite()) return "--"
        val formatter = NumberFormat.getNumberInstance(DateTimeUtils.LOCALE_PT_BR).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
        if (abs(changePercent) < FLAT_THRESHOLD_PERCENT) {
            return "${formatter.format(0.0)}%"
        }
        val sign = if (changePercent > 0.0) "+" else "-"
        return "$sign${formatter.format(abs(changePercent))}%"
    }
}
