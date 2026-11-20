package com.nexus.app.domain.model

import com.nexus.app.core.DateTimeUtils
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class MarketHistoryRange(
    val key: String,
    val label: String,
    val descriptionLabel: String,
    val calendarDays: Int
) {
    D1(key = "1D", label = "1D", descriptionLabel = "Intradiário", calendarDays = 1),
    W1(key = "1W", label = "1W", descriptionLabel = "7 dias", calendarDays = 7),
    M1(key = "1M", label = "1M", descriptionLabel = "1 mês", calendarDays = 30),
    M3(key = "3M", label = "3M", descriptionLabel = "3 meses", calendarDays = 90),
    M6(key = "6M", label = "6M", descriptionLabel = "6 meses", calendarDays = 180),
    Y1(key = "1Y", label = "1Y", descriptionLabel = "1 ano", calendarDays = 365);

    companion object {
        fun fromKey(key: String?): MarketHistoryRange? =
            entries.firstOrNull { it.key.equals(key, ignoreCase = true) }
    }
}

enum class MarketHistoryGranularity(
    val label: String,
    val detailLabel: String
) {
    INTRADAY(label = "Intradiário", detailLabel = "Pontos intradiários"),
    DAILY(label = "Diário", detailLabel = "Fechamento diário")
}

@Serializable
data class MarketHistoryPoint(
    val timestampEpochMs: Long,
    val price: Double
)

@Serializable
data class MarketHistorySeries(
    val assetCode: String,
    val rangeKey: String,
    val granularity: MarketHistoryGranularity,
    val currency: String,
    val isPointsUnit: Boolean = false,
    val sourceName: String,
    val fetchedAtEpochMs: Long,
    val points: List<MarketHistoryPoint>
) {
    fun resolveRange(): MarketHistoryRange? = MarketHistoryRange.fromKey(rangeKey)
}

data class MarketHistoryMetrics(
    val startPrice: Double,
    val endPrice: Double,
    val minPrice: Double,
    val maxPrice: Double,
    val changePercent: Double?,
    val trend: QuoteTrend,
    val startTimestampEpochMs: Long,
    val endTimestampEpochMs: Long,
    val pointCount: Int
)

data class MarketHistoryUiState(
    val assetCode: String,
    val selectedRange: MarketHistoryRange,
    val supportedRanges: List<MarketHistoryRange> = MarketHistoryCalculations.supportedRangesFor(assetCode),
    val series: MarketHistorySeries? = null,
    val metrics: MarketHistoryMetrics? = null,
    val isLoading: Boolean = false,
    val isStale: Boolean = false,
    val isOffline: Boolean = false,
    val lastRefreshFailed: Boolean = false,
    val statusMessage: String? = null,
    val lastAttemptAtEpochMs: Long? = null
)

object MarketHistoryCalculations {
    const val FAILURE_RETRY_COOLDOWN_MS: Long = 60L * 1000L

    private val currencyDailyOnlyRanges = listOf(
        MarketHistoryRange.W1,
        MarketHistoryRange.M1,
        MarketHistoryRange.M3,
        MarketHistoryRange.M6,
        MarketHistoryRange.Y1
    )

    private val fullRanges = MarketHistoryRange.entries.toList()

    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", DateTimeUtils.LOCALE_PT_BR)
    private val dayMonthTimeFormatter = DateTimeFormatter.ofPattern("dd/MM HH:mm", DateTimeUtils.LOCALE_PT_BR)
    private val dayMonthFormatter = DateTimeFormatter.ofPattern("dd/MM", DateTimeUtils.LOCALE_PT_BR)
    private val dayMonthYearFormatter = DateTimeFormatter.ofPattern("dd/MM/yy", DateTimeUtils.LOCALE_PT_BR)
    private val monthYearFormatter = DateTimeFormatter.ofPattern("MMM/yy", Locale("pt", "BR"))

    /**
     * Retorna exclusivamente os intervalos com dados históricos reais disponíveis nas fontes
     * integradas do NEXUS. Moedas (USD/BRL e EUR/BRL) dependem de séries diárias oficiais/de referência
     * (PTAX BCB, AwesomeAPI diário e Frankfurter BCE), portanto não oferecem série intradiária 1D.
     */
    fun supportedRangesFor(assetCode: String): List<MarketHistoryRange> {
        val descriptor = MarketAssetCatalog.findByCode(assetCode)
        return when (descriptor?.category) {
            AssetCategory.CURRENCY -> currencyDailyOnlyRanges
            else -> fullRanges
        }
    }

    fun defaultRangeFor(assetCode: String): MarketHistoryRange {
        val supported = supportedRangesFor(assetCode)
        return if (MarketHistoryRange.M1 in supported) {
            MarketHistoryRange.M1
        } else {
            supported.first()
        }
    }

    fun isRangeSupported(assetCode: String, range: MarketHistoryRange): Boolean =
        range in supportedRangesFor(assetCode)

    fun autoRefreshCooldownMs(range: MarketHistoryRange): Long = when (range) {
        MarketHistoryRange.D1 -> 10L * 60L * 1000L
        MarketHistoryRange.W1 -> 20L * 60L * 1000L
        MarketHistoryRange.M1,
        MarketHistoryRange.M3,
        MarketHistoryRange.M6,
        MarketHistoryRange.Y1 -> 60L * 60L * 1000L
    }

    fun staleTtlMs(range: MarketHistoryRange): Long = when (range) {
        MarketHistoryRange.D1 -> 20L * 60L * 1000L
        MarketHistoryRange.W1 -> 60L * 60L * 1000L
        MarketHistoryRange.M1,
        MarketHistoryRange.M3,
        MarketHistoryRange.M6,
        MarketHistoryRange.Y1 -> 6L * 60L * 60L * 1000L
    }

    fun isSeriesStale(
        series: MarketHistorySeries,
        nowEpochMs: Long
    ): Boolean {
        val range = series.resolveRange() ?: MarketHistoryRange.M1
        if (series.fetchedAtEpochMs <= 0L || nowEpochMs < series.fetchedAtEpochMs) return true
        return (nowEpochMs - series.fetchedAtEpochMs) >= staleTtlMs(range)
    }

    /**
     * Valida, deduplica por timestamp (mantendo a última ocorrência para o mesmo instante)
     * e ordena cronologicamente de forma crescente. Nunca interpola pontos ausentes.
     */
    fun normalizeAndDeduplicate(points: List<MarketHistoryPoint>): List<MarketHistoryPoint> {
        if (points.isEmpty()) return emptyList()
        val byTimestamp = LinkedHashMap<Long, MarketHistoryPoint>(points.size)
        for (point in points) {
            if (point.timestampEpochMs > 0L && MarketQuoteCalculations.isValidPositiveRate(point.price)) {
                byTimestamp[point.timestampEpochMs] = point
            }
        }
        return byTimestamp.values.sortedBy { it.timestampEpochMs }
    }

    fun computeMetrics(series: MarketHistorySeries): MarketHistoryMetrics? {
        val sorted = normalizeAndDeduplicate(series.points)
        if (sorted.isEmpty()) return null
        val first = sorted.first()
        val last = sorted.last()
        var minPrice = first.price
        var maxPrice = first.price
        for (pt in sorted) {
            if (pt.price < minPrice) minPrice = pt.price
            if (pt.price > maxPrice) maxPrice = pt.price
        }
        val changePct = if (sorted.size >= 2) {
            MarketQuoteCalculations.computeChangePercent(last.price, first.price)
        } else {
            null
        }
        return MarketHistoryMetrics(
            startPrice = first.price,
            endPrice = last.price,
            minPrice = minPrice,
            maxPrice = maxPrice,
            changePercent = changePct,
            trend = MarketQuoteCalculations.computeTrend(changePct),
            startTimestampEpochMs = first.timestampEpochMs,
            endTimestampEpochMs = last.timestampEpochMs,
            pointCount = sorted.size
        )
    }

    fun formatSeriesValue(
        value: Double,
        descriptor: MarketAssetCatalog.AssetDescriptor,
        currencyOverride: String? = null
    ): String {
        val currency = currencyOverride?.takeIf { it.isNotBlank() } ?: descriptor.quoteSymbol
        return MarketQuoteCalculations.formatPrice(
            value = value,
            category = descriptor.category,
            currencyCode = currency
        )
    }

    fun formatAxisLabel(
        timestampEpochMs: Long,
        range: MarketHistoryRange,
        granularity: MarketHistoryGranularity
    ): String {
        if (timestampEpochMs <= 0L) return "--"
        val zdt = Instant.ofEpochMilli(timestampEpochMs).atZone(DateTimeUtils.ZONE_SAO_PAULO)
        return when {
            range == MarketHistoryRange.D1 && granularity == MarketHistoryGranularity.INTRADAY ->
                zdt.format(timeFormatter)
            range == MarketHistoryRange.W1 && granularity == MarketHistoryGranularity.INTRADAY ->
                zdt.format(dayMonthFormatter)
            range == MarketHistoryRange.Y1 ->
                zdt.format(monthYearFormatter)
            else ->
                zdt.format(dayMonthFormatter)
        }
    }

    fun formatPointTimestamp(
        timestampEpochMs: Long,
        granularity: MarketHistoryGranularity
    ): String {
        if (timestampEpochMs <= 0L) return "--"
        val zdt = Instant.ofEpochMilli(timestampEpochMs).atZone(DateTimeUtils.ZONE_SAO_PAULO)
        return if (granularity == MarketHistoryGranularity.INTRADAY) {
            zdt.format(dayMonthTimeFormatter)
        } else {
            zdt.format(dayMonthYearFormatter)
        }
    }
}
