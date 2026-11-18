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
    REFERENCE("Taxa de Referência");

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
    val fetchedAtEpochMs: Long
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

object MarketAssetCatalog {
    const val USD_BRL_CODE = "USD-BRL"
    const val EUR_BRL_CODE = "EUR-BRL"
    const val BTC_BRL_CODE = "BTC-BRL"
    const val ETH_BRL_CODE = "ETH-BRL"
    const val SOL_BRL_CODE = "SOL-BRL"

    data class AssetDescriptor(
        val code: String,
        val baseSymbol: String,
        val quoteSymbol: String,
        val displayName: String,
        val category: AssetCategory
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

    val CURRENCY_ASSETS: List<AssetDescriptor> = listOf(USD_BRL, EUR_BRL)
    val CRYPTO_ASSETS: List<AssetDescriptor> = listOf(BTC_BRL, ETH_BRL, SOL_BRL)
    val SUPPORTED_ASSETS: List<AssetDescriptor> = CURRENCY_ASSETS + CRYPTO_ASSETS

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
        if (!value.isFinite() || value <= 0.0) return "R$ --"
        val decimals = when (category) {
            AssetCategory.CURRENCY -> 4
            AssetCategory.CRYPTO -> if (value >= 10.0) 2 else 4
            else -> 2
        }
        val formatter = NumberFormat.getNumberInstance(DateTimeUtils.LOCALE_PT_BR).apply {
            minimumFractionDigits = decimals
            maximumFractionDigits = decimals
        }
        return "R$ ${formatter.format(value)}"
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
