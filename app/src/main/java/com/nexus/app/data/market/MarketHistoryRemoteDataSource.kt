package com.nexus.app.data.market

import com.nexus.app.core.DateTimeUtils
import com.nexus.app.domain.model.MarketAssetCatalog
import com.nexus.app.domain.model.MarketHistoryCalculations
import com.nexus.app.domain.model.MarketHistoryGranularity
import com.nexus.app.domain.model.MarketHistoryPoint
import com.nexus.app.domain.model.MarketHistoryRange
import com.nexus.app.domain.model.MarketHistorySeries
import com.nexus.app.domain.model.MarketQuoteCalculations
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import java.io.IOException
import java.net.SocketTimeoutException
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

sealed class RemoteHistoryFetchResult {
    data class Success(val series: MarketHistorySeries) : RemoteHistoryFetchResult()
    data class Failure(
        val reason: FailureReason,
        val httpStatusCode: Int? = null,
        val detail: String
    ) : RemoteHistoryFetchResult()
}

interface MarketHistoryRemoteDataSource {
    fun supports(asset: MarketAssetCatalog.AssetDescriptor, range: MarketHistoryRange): Boolean = true

    suspend fun fetchHistory(
        asset: MarketAssetCatalog.AssetDescriptor,
        range: MarketHistoryRange
    ): RemoteHistoryFetchResult
}

object MarketHistoryParsers {
    private val json = Json { ignoreUnknownKeys = true }
    private val zoneNewYork: ZoneId = ZoneId.of("America/New_York")
    private val zoneUtc: ZoneId = ZoneId.of("UTC")
    private val isoDateFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val nasdaqUsDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy", Locale.US)
    private val cnbcTradeTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss", Locale.US)

    private val coinGeckoIdByAssetCode = mapOf(
        MarketAssetCatalog.BTC_BRL_CODE to "bitcoin",
        MarketAssetCatalog.ETH_BRL_CODE to "ethereum",
        MarketAssetCatalog.SOL_BRL_CODE to "solana"
    )

    internal fun coinGeckoIdFor(assetCode: String): String? = coinGeckoIdByAssetCode[assetCode]

    fun parseBcbOlindaHistory(
        jsonBody: String,
        asset: MarketAssetCatalog.AssetDescriptor,
        range: MarketHistoryRange,
        fetchedAtEpochMs: Long
    ): RemoteHistoryFetchResult {
        val root = runCatching { json.parseToJsonElement(jsonBody) }.getOrNull() as? JsonObject
            ?: return invalidResponse("Resposta JSON inválida da API PTAX (BCB)")

        val values = root["value"] as? JsonArray
            ?: return invalidResponse("Lista 'value' ausente no histórico PTAX (BCB)")

        if (values.isEmpty()) {
            return invalidResponse("Histórico PTAX (BCB) vazio para ${asset.code} (${range.key})")
        }

        // Agrupa por dia local para evitar múltiplos boletins no mesmo dia (mantém Fechamento quando houver).
        val byDate = LinkedHashMap<LocalDate, Pair<Long, Double>>()
        for (element in values) {
            val obj = element as? JsonObject ?: continue
            val bulletinType = (obj["tipoBoletim"] as? JsonPrimitive)?.contentOrNull?.trim().orEmpty()
            val sell = parseDouble(obj["cotacaoVenda"]) ?: parseDouble(obj["cotacaoCompra"]) ?: continue
            if (!MarketQuoteCalculations.isValidPositiveRate(sell)) continue

            val rawDt = (obj["dataHoraCotacao"] as? JsonPrimitive)?.contentOrNull?.trim() ?: continue
            val parsedZdt = parseBcbTimestamp(rawDt) ?: continue
            val localDate = parsedZdt.toLocalDate()
            val epochMs = parsedZdt.toInstant().toEpochMilli()

            val isClosing = bulletinType.contains("Fechamento", ignoreCase = true)
            val existing = byDate[localDate]
            if (existing == null || isClosing || epochMs > existing.first) {
                byDate[localDate] = epochMs to sell
            }
        }

        val points = MarketHistoryCalculations.normalizeAndDeduplicate(
            byDate.values.map { (ts, price) -> MarketHistoryPoint(ts, price) }
        )
        if (points.size < 2) {
            return invalidResponse("Série histórica PTAX insuficiente para ${asset.code} (${range.key})")
        }

        return RemoteHistoryFetchResult.Success(
            MarketHistorySeries(
                assetCode = asset.code,
                rangeKey = range.key,
                granularity = MarketHistoryGranularity.DAILY,
                currency = asset.quoteSymbol,
                isPointsUnit = asset.isPointsUnit,
                sourceName = "Banco Central (PTAX)",
                fetchedAtEpochMs = fetchedAtEpochMs,
                points = points
            )
        )
    }

    fun parseAwesomeApiDailyHistory(
        jsonBody: String,
        asset: MarketAssetCatalog.AssetDescriptor,
        range: MarketHistoryRange,
        fetchedAtEpochMs: Long
    ): RemoteHistoryFetchResult {
        val array = runCatching { json.parseToJsonElement(jsonBody) }.getOrNull() as? JsonArray
            ?: return invalidResponse("Resposta JSON inválida da AwesomeAPI diária")

        val rawPoints = array.mapNotNull { el ->
            val obj = el as? JsonObject ?: return@mapNotNull null
            val bid = parseDouble(obj["bid"]) ?: return@mapNotNull null
            val tsRaw = (obj["timestamp"] as? JsonPrimitive)?.contentOrNull?.trim()?.toLongOrNull()
                ?: return@mapNotNull null
            if (!MarketQuoteCalculations.isValidPositiveRate(bid) || tsRaw <= 0L) return@mapNotNull null
            val epochMs = if (tsRaw < 10_000_000_000L) tsRaw * 1000L else tsRaw
            MarketHistoryPoint(timestampEpochMs = epochMs, price = bid)
        }

        val points = MarketHistoryCalculations.normalizeAndDeduplicate(rawPoints)
        if (points.size < 2) {
            return invalidResponse("Série AwesomeAPI diária insuficiente para ${asset.code} (${range.key})")
        }

        return RemoteHistoryFetchResult.Success(
            MarketHistorySeries(
                assetCode = asset.code,
                rangeKey = range.key,
                granularity = MarketHistoryGranularity.DAILY,
                currency = asset.quoteSymbol,
                isPointsUnit = asset.isPointsUnit,
                sourceName = "AwesomeAPI",
                fetchedAtEpochMs = fetchedAtEpochMs,
                points = points
            )
        )
    }

    fun parseFrankfurterHistory(
        jsonBody: String,
        asset: MarketAssetCatalog.AssetDescriptor,
        range: MarketHistoryRange,
        fetchedAtEpochMs: Long
    ): RemoteHistoryFetchResult {
        val root = runCatching { json.parseToJsonElement(jsonBody) }.getOrNull() as? JsonObject
            ?: return invalidResponse("Resposta JSON inválida da Frankfurter (BCE)")
        val ratesObj = root["rates"] as? JsonObject
            ?: return invalidResponse("Bloco 'rates' ausente na resposta da Frankfurter (BCE)")

        val rawPoints = mutableListOf<MarketHistoryPoint>()
        for ((dateKey, rateEntry) in ratesObj.entries) {
            val dayRates = rateEntry as? JsonObject ?: continue
            val price = parseDouble(dayRates[asset.quoteSymbol]) ?: continue
            val localDate = runCatching { LocalDate.parse(dateKey, isoDateFormatter) }.getOrNull() ?: continue
            val ts = localDate.atTime(LocalTime.of(16, 0)).atZone(zoneUtc).toInstant().toEpochMilli()
            rawPoints.add(MarketHistoryPoint(timestampEpochMs = ts, price = price))
        }

        val points = MarketHistoryCalculations.normalizeAndDeduplicate(rawPoints)
        if (points.size < 2) {
            return invalidResponse("Série Frankfurter (BCE) insuficiente para ${asset.code} (${range.key})")
        }

        return RemoteHistoryFetchResult.Success(
            MarketHistorySeries(
                assetCode = asset.code,
                rangeKey = range.key,
                granularity = MarketHistoryGranularity.DAILY,
                currency = asset.quoteSymbol,
                isPointsUnit = asset.isPointsUnit,
                sourceName = "Frankfurter (BCE)",
                fetchedAtEpochMs = fetchedAtEpochMs,
                points = points
            )
        )
    }

    fun parseCoinGeckoMarketChart(
        jsonBody: String,
        asset: MarketAssetCatalog.AssetDescriptor,
        range: MarketHistoryRange,
        fetchedAtEpochMs: Long
    ): RemoteHistoryFetchResult {
        val root = runCatching { json.parseToJsonElement(jsonBody) }.getOrNull() as? JsonObject
            ?: return invalidResponse("Resposta JSON inválida da CoinGecko")
        val prices = root["prices"] as? JsonArray
            ?: return invalidResponse("Série 'prices' ausente na resposta da CoinGecko")

        val rawPoints = prices.mapNotNull { entry ->
            val pair = entry as? JsonArray ?: return@mapNotNull null
            if (pair.size < 2) return@mapNotNull null
            val ts = parseLong(pair[0]) ?: return@mapNotNull null
            val price = parseDouble(pair[1]) ?: return@mapNotNull null
            MarketHistoryPoint(timestampEpochMs = ts, price = price)
        }

        val points = MarketHistoryCalculations.normalizeAndDeduplicate(rawPoints)
        if (points.size < 2) {
            return invalidResponse("Série CoinGecko insuficiente para ${asset.code} (${range.key})")
        }

        val granularity = when (range) {
            MarketHistoryRange.D1,
            MarketHistoryRange.W1,
            MarketHistoryRange.M1 -> MarketHistoryGranularity.INTRADAY
            MarketHistoryRange.M3,
            MarketHistoryRange.M6,
            MarketHistoryRange.Y1 -> MarketHistoryGranularity.DAILY
        }

        return RemoteHistoryFetchResult.Success(
            MarketHistorySeries(
                assetCode = asset.code,
                rangeKey = range.key,
                granularity = granularity,
                currency = asset.quoteSymbol,
                isPointsUnit = asset.isPointsUnit,
                sourceName = "CoinGecko",
                fetchedAtEpochMs = fetchedAtEpochMs,
                points = points
            )
        )
    }

    fun parseOkxCandles(
        jsonBody: String,
        asset: MarketAssetCatalog.AssetDescriptor,
        range: MarketHistoryRange,
        granularity: MarketHistoryGranularity,
        fetchedAtEpochMs: Long
    ): RemoteHistoryFetchResult {
        val root = runCatching { json.parseToJsonElement(jsonBody) }.getOrNull() as? JsonObject
            ?: return invalidResponse("Resposta JSON inválida da OKX")
        val code = (root["code"] as? JsonPrimitive)?.contentOrNull
        if (code != null && code != "0") {
            return invalidResponse("OKX retornou erro ($code) para ${asset.code}")
        }
        val rows = root["data"] as? JsonArray
            ?: return invalidResponse("Série 'data' ausente na resposta da OKX")

        val rawPoints = rows.mapNotNull { entry ->
            val arr = entry as? JsonArray ?: return@mapNotNull null
            if (arr.size < 5) return@mapNotNull null
            val ts = parseLong(arr[0]) ?: return@mapNotNull null
            val closePrice = parseDouble(arr[4]) ?: return@mapNotNull null
            MarketHistoryPoint(timestampEpochMs = ts, price = closePrice)
        }

        val points = MarketHistoryCalculations.normalizeAndDeduplicate(rawPoints)
        if (points.size < 2) {
            return invalidResponse("Série OKX insuficiente para ${asset.code} (${range.key})")
        }

        return RemoteHistoryFetchResult.Success(
            MarketHistorySeries(
                assetCode = asset.code,
                rangeKey = range.key,
                granularity = granularity,
                currency = asset.quoteSymbol,
                isPointsUnit = asset.isPointsUnit,
                sourceName = "OKX Spot",
                fetchedAtEpochMs = fetchedAtEpochMs,
                points = points
            )
        )
    }

    fun parseBrapiHistorical(
        jsonBody: String,
        asset: MarketAssetCatalog.AssetDescriptor,
        range: MarketHistoryRange,
        granularity: MarketHistoryGranularity,
        fetchedAtEpochMs: Long
    ): RemoteHistoryFetchResult {
        val root = runCatching { json.parseToJsonElement(jsonBody) }.getOrNull() as? JsonObject
            ?: return invalidResponse("Resposta JSON inválida da brapi.dev")
        val results = root["results"] as? JsonArray
            ?: return invalidResponse("Bloco 'results' ausente na resposta da brapi.dev")
        val first = results.firstOrNull() as? JsonObject
            ?: return invalidResponse("Ativo ${asset.code} ausente na resposta da brapi.dev")
        val history = first["historicalDataPrice"] as? JsonArray
            ?: return invalidResponse("Série 'historicalDataPrice' ausente na brapi.dev")

        val currency = (first["currency"] as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }
            ?: asset.quoteSymbol

        val rawPoints = history.mapNotNull { el ->
            val obj = el as? JsonObject ?: return@mapNotNull null
            val dateSec = parseLong(obj["date"]) ?: return@mapNotNull null
            val price = parseDouble(obj["close"]) ?: parseDouble(obj["adjustedClose"]) ?: return@mapNotNull null
            if (dateSec <= 0L) return@mapNotNull null
            val tsMs = if (dateSec < 10_000_000_000L) dateSec * 1000L else dateSec
            MarketHistoryPoint(timestampEpochMs = tsMs, price = price)
        }

        val points = MarketHistoryCalculations.normalizeAndDeduplicate(rawPoints)
        if (points.size < 2) {
            return invalidResponse("Série brapi.dev insuficiente para ${asset.code} (${range.key})")
        }

        return RemoteHistoryFetchResult.Success(
            MarketHistorySeries(
                assetCode = asset.code,
                rangeKey = range.key,
                granularity = granularity,
                currency = currency,
                isPointsUnit = asset.isPointsUnit,
                sourceName = "brapi.dev (B3)",
                fetchedAtEpochMs = fetchedAtEpochMs,
                points = points
            )
        )
    }

    fun parseB3DailyFluctuation(
        jsonBody: String,
        asset: MarketAssetCatalog.AssetDescriptor,
        fetchedAtEpochMs: Long
    ): RemoteHistoryFetchResult {
        val root = runCatching { json.parseToJsonElement(jsonBody) }.getOrNull() as? JsonObject
            ?: return invalidResponse("Resposta JSON inválida da B3")
        val tradingFloor = root["TradgFlr"] as? JsonObject
            ?: return invalidResponse("Bloco 'TradgFlr' ausente no histórico intradiário da B3")
        val dateIso = (tradingFloor["date"] as? JsonPrimitive)?.contentOrNull?.trim()
            ?: return invalidResponse("Data do pregão ausente no histórico intradiário da B3")
        val localDate = runCatching { LocalDate.parse(dateIso, isoDateFormatter) }.getOrNull()
            ?: return invalidResponse("Data inválida no histórico da B3: $dateIso")
        val scty = tradingFloor["scty"] as? JsonObject
            ?: return invalidResponse("Bloco 'scty' ausente no histórico da B3")
        val quotes = scty["lstQtn"] as? JsonArray
            ?: return invalidResponse("Lista 'lstQtn' ausente no histórico da B3")

        val rawPoints = quotes.mapNotNull { el ->
            val obj = el as? JsonObject ?: return@mapNotNull null
            val price = parseDouble(obj["closPric"]) ?: return@mapNotNull null
            val timeRaw = (obj["dtTm"] as? JsonPrimitive)?.contentOrNull?.trim() ?: return@mapNotNull null
            val localTime = runCatching { LocalTime.parse(timeRaw) }.getOrNull() ?: return@mapNotNull null
            val tsMs = LocalDateTime.of(localDate, localTime)
                .atZone(DateTimeUtils.ZONE_SAO_PAULO)
                .toInstant()
                .toEpochMilli()
            MarketHistoryPoint(timestampEpochMs = tsMs, price = price)
        }

        val points = MarketHistoryCalculations.normalizeAndDeduplicate(rawPoints)
        if (points.size < 2) {
            return invalidResponse("Histórico intradiário da B3 insuficiente para ${asset.code}")
        }

        return RemoteHistoryFetchResult.Success(
            MarketHistorySeries(
                assetCode = asset.code,
                rangeKey = MarketHistoryRange.D1.key,
                granularity = MarketHistoryGranularity.INTRADAY,
                currency = asset.quoteSymbol,
                isPointsUnit = asset.isPointsUnit,
                sourceName = "B3 (Pregão Público)",
                fetchedAtEpochMs = fetchedAtEpochMs,
                points = points
            )
        )
    }

    fun parseNasdaqChart(
        jsonBody: String,
        asset: MarketAssetCatalog.AssetDescriptor,
        fetchedAtEpochMs: Long
    ): RemoteHistoryFetchResult {
        val root = runCatching { json.parseToJsonElement(jsonBody) }.getOrNull() as? JsonObject
            ?: return invalidResponse("Resposta JSON inválida da Nasdaq")
        val dataObj = root["data"] as? JsonObject
            ?: return invalidResponse("Bloco 'data' ausente no gráfico da Nasdaq")
        val chartArray = dataObj["chart"] as? JsonArray
            ?: return invalidResponse("Série 'chart' ausente no gráfico da Nasdaq")

        val rawPoints = chartArray.mapNotNull { el ->
            val obj = el as? JsonObject ?: return@mapNotNull null
            val ts = parseLong(obj["x"]) ?: return@mapNotNull null
            val zObj = obj["z"] as? JsonObject
            val price = parseDouble(obj["y"])
                ?: parseUsNumber((zObj?.get("value") as? JsonPrimitive)?.contentOrNull)
                ?: return@mapNotNull null
            MarketHistoryPoint(timestampEpochMs = ts, price = price)
        }

        val points = MarketHistoryCalculations.normalizeAndDeduplicate(rawPoints)
        if (points.size < 2) {
            return invalidResponse("Gráfico intradiário da Nasdaq insuficiente para ${asset.code}")
        }

        return RemoteHistoryFetchResult.Success(
            MarketHistorySeries(
                assetCode = asset.code,
                rangeKey = MarketHistoryRange.D1.key,
                granularity = MarketHistoryGranularity.INTRADAY,
                currency = asset.quoteSymbol,
                isPointsUnit = asset.isPointsUnit,
                sourceName = "Nasdaq",
                fetchedAtEpochMs = fetchedAtEpochMs,
                points = points
            )
        )
    }

    fun parseNasdaqHistoricalTable(
        jsonBody: String,
        asset: MarketAssetCatalog.AssetDescriptor,
        range: MarketHistoryRange,
        fetchedAtEpochMs: Long
    ): RemoteHistoryFetchResult {
        val root = runCatching { json.parseToJsonElement(jsonBody) }.getOrNull() as? JsonObject
            ?: return invalidResponse("Resposta JSON inválida do histórico Nasdaq")
        val dataObj = root["data"] as? JsonObject
            ?: return invalidResponse("Bloco 'data' ausente no histórico Nasdaq")
        val tradesTable = dataObj["tradesTable"] as? JsonObject
            ?: return invalidResponse("Tabela 'tradesTable' ausente no histórico Nasdaq")
        val rows = tradesTable["rows"] as? JsonArray
            ?: return invalidResponse("Linhas ausentes no histórico Nasdaq")

        val rawPoints = rows.mapNotNull { el ->
            val obj = el as? JsonObject ?: return@mapNotNull null
            val dateText = (obj["date"] as? JsonPrimitive)?.contentOrNull?.trim() ?: return@mapNotNull null
            val closeText = (obj["close"] as? JsonPrimitive)?.contentOrNull ?: return@mapNotNull null
            val localDate = runCatching { LocalDate.parse(dateText, nasdaqUsDateFormatter) }.getOrNull()
                ?: return@mapNotNull null
            val closePrice = parseUsNumber(closeText) ?: return@mapNotNull null
            val tsMs = localDate.atTime(LocalTime.of(16, 0)).atZone(zoneNewYork).toInstant().toEpochMilli()
            MarketHistoryPoint(timestampEpochMs = tsMs, price = closePrice)
        }

        val points = MarketHistoryCalculations.normalizeAndDeduplicate(rawPoints)
        if (points.size < 2) {
            return invalidResponse("Histórico diário Nasdaq insuficiente para ${asset.code} (${range.key})")
        }

        return RemoteHistoryFetchResult.Success(
            MarketHistorySeries(
                assetCode = asset.code,
                rangeKey = range.key,
                granularity = MarketHistoryGranularity.DAILY,
                currency = asset.quoteSymbol,
                isPointsUnit = asset.isPointsUnit,
                sourceName = "Nasdaq",
                fetchedAtEpochMs = fetchedAtEpochMs,
                points = points
            )
        )
    }

    fun parseCnbcBars(
        jsonBody: String,
        asset: MarketAssetCatalog.AssetDescriptor,
        range: MarketHistoryRange,
        granularity: MarketHistoryGranularity,
        marketZone: ZoneId,
        latestSessionOnly: Boolean,
        fetchedAtEpochMs: Long
    ): RemoteHistoryFetchResult {
        val root = runCatching { json.parseToJsonElement(jsonBody) }.getOrNull() as? JsonObject
            ?: return invalidResponse("Resposta JSON inválida da CNBC")
        val status = (root["status"] as? JsonPrimitive)?.contentOrNull
        if (status.equals("ERROR", ignoreCase = true)) {
            val msg = (root["statusMessage"] as? JsonPrimitive)?.contentOrNull ?: "Erro na API de barras da CNBC"
            return invalidResponse(msg)
        }
        val barData = root["barData"] as? JsonObject
            ?: return invalidResponse("Bloco 'barData' ausente na resposta da CNBC")
        val priceBars = barData["priceBars"] as? JsonArray
            ?: return invalidResponse("Lista 'priceBars' ausente na resposta da CNBC")

        data class BarEntry(val tsMs: Long, val price: Double, val sessionDate: LocalDate)

        val parsedBars = priceBars.mapNotNull { el ->
            val obj = el as? JsonObject ?: return@mapNotNull null
            val close = parseDouble(obj["close"]) ?: return@mapNotNull null
            val tradeTimeInMills = parseLong(obj["tradeTimeinMills"])
            val tradeTimeStr = (obj["tradeTime"] as? JsonPrimitive)?.contentOrNull?.trim()
            val tsMs = when {
                tradeTimeInMills != null && tradeTimeInMills > 0L -> tradeTimeInMills
                !tradeTimeStr.isNullOrBlank() -> runCatching {
                    LocalDateTime.parse(tradeTimeStr, cnbcTradeTimeFormatter)
                        .atZone(marketZone)
                        .toInstant()
                        .toEpochMilli()
                }.getOrNull()
                else -> null
            } ?: return@mapNotNull null

            val sessionDate = if (!tradeTimeStr.isNullOrBlank() && tradeTimeStr.length >= 8) {
                runCatching {
                    LocalDate.parse(
                        "${tradeTimeStr.substring(0, 4)}-${tradeTimeStr.substring(4, 6)}-${tradeTimeStr.substring(6, 8)}",
                        isoDateFormatter
                    )
                }.getOrElse {
                    Instant.ofEpochMilli(tsMs).atZone(marketZone).toLocalDate()
                }
            } else {
                Instant.ofEpochMilli(tsMs).atZone(marketZone).toLocalDate()
            }
            BarEntry(tsMs = tsMs, price = close, sessionDate = sessionDate)
        }

        val filteredBars = if (latestSessionOnly && parsedBars.isNotEmpty()) {
            val latestDate = parsedBars.maxOf { it.sessionDate }
            parsedBars.filter { it.sessionDate == latestDate }
        } else {
            parsedBars
        }

        val points = MarketHistoryCalculations.normalizeAndDeduplicate(
            filteredBars.map { MarketHistoryPoint(timestampEpochMs = it.tsMs, price = it.price) }
        )
        if (points.size < 2) {
            return invalidResponse("Histórico CNBC insuficiente para ${asset.code} (${range.key})")
        }

        return RemoteHistoryFetchResult.Success(
            MarketHistorySeries(
                assetCode = asset.code,
                rangeKey = range.key,
                granularity = granularity,
                currency = asset.quoteSymbol,
                isPointsUnit = asset.isPointsUnit,
                sourceName = "CNBC Market",
                fetchedAtEpochMs = fetchedAtEpochMs,
                points = points
            )
        )
    }

    private fun parseBcbTimestamp(raw: String): java.time.ZonedDateTime? {
        val normalized = raw.replace(' ', 'T')
        val ldt = runCatching { LocalDateTime.parse(normalized) }.getOrNull() ?: return null
        return ldt.atZone(DateTimeUtils.ZONE_SAO_PAULO)
    }

    private fun parseDouble(element: kotlinx.serialization.json.JsonElement?): Double? {
        val primitive = element as? JsonPrimitive ?: return null
        val content = primitive.contentOrNull?.trim() ?: return null
        if (content.isEmpty() || content.equals("null", ignoreCase = true)) return null
        return content.toDoubleOrNull()?.takeIf { it.isFinite() }
    }

    private fun parseLong(element: kotlinx.serialization.json.JsonElement?): Long? {
        val primitive = element as? JsonPrimitive ?: return null
        val content = primitive.contentOrNull?.trim() ?: return null
        return content.toLongOrNull() ?: content.toDoubleOrNull()?.toLong()
    }

    private fun parseUsNumber(raw: String?): Double? {
        if (raw.isNullOrBlank()) return null
        val cleaned = raw
            .replace("$", "")
            .replace(",", "")
            .replace("+", "")
            .trim()
        return cleaned.toDoubleOrNull()?.takeIf { it.isFinite() }
    }

    private fun invalidResponse(detail: String): RemoteHistoryFetchResult.Failure =
        RemoteHistoryFetchResult.Failure(
            reason = FailureReason.INVALID_RESPONSE,
            detail = detail
        )
}

internal fun mapHistoryHttpFailure(
    statusCode: Int,
    providerName: String
): RemoteHistoryFetchResult.Failure = when (statusCode) {
    429 -> RemoteHistoryFetchResult.Failure(
        reason = FailureReason.RATE_LIMITED,
        httpStatusCode = 429,
        detail = "Limite de requisições atingido em $providerName"
    )
    else -> RemoteHistoryFetchResult.Failure(
        reason = FailureReason.HTTP_ERROR,
        httpStatusCode = statusCode,
        detail = "Erro HTTP $statusCode em $providerName"
    )
}

internal fun mapHistoryExceptionToFailure(
    throwable: Throwable,
    providerName: String
): RemoteHistoryFetchResult.Failure = when (throwable) {
    is SocketTimeoutException -> RemoteHistoryFetchResult.Failure(
        reason = FailureReason.TIMEOUT,
        detail = "Tempo esgotado ao consultar $providerName"
    )
    is IOException -> RemoteHistoryFetchResult.Failure(
        reason = FailureReason.NETWORK_UNAVAILABLE,
        detail = "Falha de rede ao consultar $providerName"
    )
    else -> RemoteHistoryFetchResult.Failure(
        reason = FailureReason.INVALID_RESPONSE,
        detail = "Erro inesperado em $providerName: ${throwable.localizedMessage ?: "desconhecido"}"
    )
}

class CompositeMarketHistoryRemoteDataSource(
    private val providers: List<MarketHistoryRemoteDataSource>
) : MarketHistoryRemoteDataSource {

    override fun supports(asset: MarketAssetCatalog.AssetDescriptor, range: MarketHistoryRange): Boolean =
        providers.any { it.supports(asset, range) }

    override suspend fun fetchHistory(
        asset: MarketAssetCatalog.AssetDescriptor,
        range: MarketHistoryRange
    ): RemoteHistoryFetchResult {
        val eligible = providers.filter { it.supports(asset, range) }
        if (eligible.isEmpty()) {
            return RemoteHistoryFetchResult.Failure(
                reason = FailureReason.UNSUPPORTED_ASSET,
                detail = "Intervalo ${range.key} indisponível para ${asset.code}"
            )
        }
        var lastFailure: RemoteHistoryFetchResult.Failure? = null
        for (provider in eligible) {
            when (val res = provider.fetchHistory(asset, range)) {
                is RemoteHistoryFetchResult.Success -> return res
                is RemoteHistoryFetchResult.Failure -> lastFailure = res
            }
        }
        return lastFailure ?: RemoteHistoryFetchResult.Failure(
            reason = FailureReason.INVALID_RESPONSE,
            detail = "Nenhuma fonte retornou histórico válido para ${asset.code}"
        )
    }
}

class AssetCategoryRoutedHistoryDataSource(
    private val byAssetCode: Map<String, MarketHistoryRemoteDataSource>
) : MarketHistoryRemoteDataSource {

    override fun supports(asset: MarketAssetCatalog.AssetDescriptor, range: MarketHistoryRange): Boolean =
        byAssetCode[asset.code]?.supports(asset, range) == true

    override suspend fun fetchHistory(
        asset: MarketAssetCatalog.AssetDescriptor,
        range: MarketHistoryRange
    ): RemoteHistoryFetchResult {
        val route = byAssetCode[asset.code] ?: return RemoteHistoryFetchResult.Failure(
            reason = FailureReason.UNSUPPORTED_ASSET,
            detail = "Ativo sem provedor histórico configurado: ${asset.code}"
        )
        return route.fetchHistory(asset, range)
    }
}

class BcbOlindaHistoryDataSource(
    private val transport: MarketHttpTransport,
    private val clock: () -> Long = DateTimeUtils::nowEpochMillis,
    private val todayProvider: () -> LocalDate = DateTimeUtils::todayLocalDate
) : MarketHistoryRemoteDataSource {

    override fun supports(asset: MarketAssetCatalog.AssetDescriptor, range: MarketHistoryRange): Boolean =
        (asset.code == MarketAssetCatalog.USD_BRL_CODE || asset.code == MarketAssetCatalog.EUR_BRL_CODE) &&
            range != MarketHistoryRange.D1

    override suspend fun fetchHistory(
        asset: MarketAssetCatalog.AssetDescriptor,
        range: MarketHistoryRange
    ): RemoteHistoryFetchResult {
        if (!supports(asset, range)) {
            return RemoteHistoryFetchResult.Failure(
                reason = FailureReason.UNSUPPORTED_ASSET,
                detail = "PTAX histórico indisponível para ${asset.code} (${range.key})"
            )
        }
        val lookbackDays = if (range == MarketHistoryRange.W1) 9L else range.calendarDays.toLong()
        val endDate = todayProvider()
        val startDate = endDate.minusDays(lookbackDays)
        val startParam = startDate.format(OLINDA_DATE_FORMATTER)
        val endParam = endDate.format(OLINDA_DATE_FORMATTER)

        val url = if (asset.code == MarketAssetCatalog.USD_BRL_CODE) {
            "https://olinda.bcb.gov.br/olinda/servico/PTAX/versao/v1/odata/" +
                "CotacaoDolarPeriodo(dataInicial=@dataInicial,dataFinalCotacao=@dataFinalCotacao)" +
                "?@dataInicial='$startParam'&@dataFinalCotacao='$endParam'" +
                "&\$top=400&\$orderby=dataHoraCotacao%20asc&\$format=json"
        } else {
            "https://olinda.bcb.gov.br/olinda/servico/PTAX/versao/v1/odata/" +
                "CotacaoMoedaPeriodo(moeda=@moeda,dataInicial=@dataInicial,dataFinalCotacao=@dataFinalCotacao)" +
                "?@moeda='${asset.baseSymbol}'&@dataInicial='$startParam'&@dataFinalCotacao='$endParam'" +
                "&\$filter=contains(tipoBoletim,'Fechamento')&\$top=400&\$orderby=dataHoraCotacao%20asc&\$format=json"
        }

        val response = transport.get(url).getOrElse { err ->
            return mapHistoryExceptionToFailure(err, "Banco Central (PTAX)")
        }
        return if (response.statusCode in 200..299) {
            MarketHistoryParsers.parseBcbOlindaHistory(response.body, asset, range, clock())
        } else {
            mapHistoryHttpFailure(response.statusCode, "Banco Central (PTAX)")
        }
    }

    companion object {
        private val OLINDA_DATE_FORMATTER = DateTimeFormatter.ofPattern("MM-dd-yyyy", Locale.US)
    }
}

class AwesomeApiDailyHistoryDataSource(
    private val transport: MarketHttpTransport,
    private val clock: () -> Long = DateTimeUtils::nowEpochMillis
) : MarketHistoryRemoteDataSource {

    override fun supports(asset: MarketAssetCatalog.AssetDescriptor, range: MarketHistoryRange): Boolean =
        (asset.code == MarketAssetCatalog.USD_BRL_CODE || asset.code == MarketAssetCatalog.EUR_BRL_CODE) &&
            range != MarketHistoryRange.D1

    override suspend fun fetchHistory(
        asset: MarketAssetCatalog.AssetDescriptor,
        range: MarketHistoryRange
    ): RemoteHistoryFetchResult {
        if (!supports(asset, range)) {
            return RemoteHistoryFetchResult.Failure(
                reason = FailureReason.UNSUPPORTED_ASSET,
                detail = "AwesomeAPI histórico indisponível para ${asset.code} (${range.key})"
            )
        }
        val daysCount = when (range) {
            MarketHistoryRange.W1 -> 7
            MarketHistoryRange.M1 -> 30
            MarketHistoryRange.M3 -> 90
            MarketHistoryRange.M6 -> 180
            MarketHistoryRange.Y1 -> 360
            MarketHistoryRange.D1 -> 7
        }
        val url = "https://economia.awesomeapi.com.br/json/daily/${asset.code}/$daysCount"
        val response = transport.get(url).getOrElse { err ->
            return mapHistoryExceptionToFailure(err, "AwesomeAPI")
        }
        return if (response.statusCode in 200..299) {
            MarketHistoryParsers.parseAwesomeApiDailyHistory(response.body, asset, range, clock())
        } else {
            mapHistoryHttpFailure(response.statusCode, "AwesomeAPI")
        }
    }
}

class FrankfurterHistoryDataSource(
    private val transport: MarketHttpTransport,
    private val clock: () -> Long = DateTimeUtils::nowEpochMillis,
    private val todayProvider: () -> LocalDate = DateTimeUtils::todayLocalDate
) : MarketHistoryRemoteDataSource {

    override fun supports(asset: MarketAssetCatalog.AssetDescriptor, range: MarketHistoryRange): Boolean =
        (asset.code == MarketAssetCatalog.USD_BRL_CODE || asset.code == MarketAssetCatalog.EUR_BRL_CODE) &&
            range != MarketHistoryRange.D1

    override suspend fun fetchHistory(
        asset: MarketAssetCatalog.AssetDescriptor,
        range: MarketHistoryRange
    ): RemoteHistoryFetchResult {
        if (!supports(asset, range)) {
            return RemoteHistoryFetchResult.Failure(
                reason = FailureReason.UNSUPPORTED_ASSET,
                detail = "Frankfurter histórico indisponível para ${asset.code} (${range.key})"
            )
        }
        val lookbackDays = if (range == MarketHistoryRange.W1) 9L else range.calendarDays.toLong()
        val endDate = todayProvider()
        val startDate = endDate.minusDays(lookbackDays)
        val url = "https://api.frankfurter.app/$startDate..$endDate?from=${asset.baseSymbol}&to=${asset.quoteSymbol}"

        val response = transport.get(url).getOrElse { err ->
            return mapHistoryExceptionToFailure(err, "Frankfurter (BCE)")
        }
        return if (response.statusCode in 200..299) {
            MarketHistoryParsers.parseFrankfurterHistory(response.body, asset, range, clock())
        } else {
            mapHistoryHttpFailure(response.statusCode, "Frankfurter (BCE)")
        }
    }
}

class CoinGeckoHistoryDataSource(
    private val transport: MarketHttpTransport,
    private val clock: () -> Long = DateTimeUtils::nowEpochMillis
) : MarketHistoryRemoteDataSource {

    override fun supports(asset: MarketAssetCatalog.AssetDescriptor, range: MarketHistoryRange): Boolean =
        MarketHistoryParsers.coinGeckoIdFor(asset.code) != null

    override suspend fun fetchHistory(
        asset: MarketAssetCatalog.AssetDescriptor,
        range: MarketHistoryRange
    ): RemoteHistoryFetchResult {
        val coinId = MarketHistoryParsers.coinGeckoIdFor(asset.code)
            ?: return RemoteHistoryFetchResult.Failure(
                reason = FailureReason.UNSUPPORTED_ASSET,
                detail = "Ativo não suportado pela CoinGecko: ${asset.code}"
            )
        val url = "https://api.coingecko.com/api/v3/coins/$coinId/market_chart" +
            "?vs_currency=${asset.quoteSymbol.lowercase(Locale.US)}&days=${range.calendarDays}"

        val response = transport.get(url).getOrElse { err ->
            return mapHistoryExceptionToFailure(err, "CoinGecko")
        }
        return if (response.statusCode in 200..299) {
            MarketHistoryParsers.parseCoinGeckoMarketChart(response.body, asset, range, clock())
        } else {
            mapHistoryHttpFailure(response.statusCode, "CoinGecko")
        }
    }
}

class OkxHistoryDataSource(
    private val transport: MarketHttpTransport,
    private val clock: () -> Long = DateTimeUtils::nowEpochMillis
) : MarketHistoryRemoteDataSource {

    override fun supports(asset: MarketAssetCatalog.AssetDescriptor, range: MarketHistoryRange): Boolean =
        asset.code in setOf(
            MarketAssetCatalog.BTC_BRL_CODE,
            MarketAssetCatalog.ETH_BRL_CODE,
            MarketAssetCatalog.SOL_BRL_CODE
        )

    override suspend fun fetchHistory(
        asset: MarketAssetCatalog.AssetDescriptor,
        range: MarketHistoryRange
    ): RemoteHistoryFetchResult {
        if (!supports(asset, range)) {
            return RemoteHistoryFetchResult.Failure(
                reason = FailureReason.UNSUPPORTED_ASSET,
                detail = "Ativo não suportado pela OKX: ${asset.code}"
            )
        }
        val (bar, limit, granularity) = when (range) {
            MarketHistoryRange.D1 -> Triple("15m", 96, MarketHistoryGranularity.INTRADAY)
            MarketHistoryRange.W1 -> Triple("1H", 168, MarketHistoryGranularity.INTRADAY)
            MarketHistoryRange.M1 -> Triple("4H", 180, MarketHistoryGranularity.INTRADAY)
            MarketHistoryRange.M3 -> Triple("1D", 90, MarketHistoryGranularity.DAILY)
            MarketHistoryRange.M6 -> Triple("1D", 180, MarketHistoryGranularity.DAILY)
            MarketHistoryRange.Y1 -> Triple("1D", 300, MarketHistoryGranularity.DAILY)
        }
        val url = "https://www.okx.com/api/v5/market/candles?instId=${asset.code}&bar=$bar&limit=$limit"
        val response = transport.get(url).getOrElse { err ->
            return mapHistoryExceptionToFailure(err, "OKX Spot")
        }
        return if (response.statusCode in 200..299) {
            MarketHistoryParsers.parseOkxCandles(response.body, asset, range, granularity, clock())
        } else {
            mapHistoryHttpFailure(response.statusCode, "OKX Spot")
        }
    }
}

class BrapiHistoryDataSource(
    private val transport: MarketHttpTransport,
    private val clock: () -> Long = DateTimeUtils::nowEpochMillis
) : MarketHistoryRemoteDataSource {

    override fun supports(asset: MarketAssetCatalog.AssetDescriptor, range: MarketHistoryRange): Boolean =
        asset.code in setOf(
            MarketAssetCatalog.PETR4_CODE,
            MarketAssetCatalog.VALE3_CODE,
            MarketAssetCatalog.ITUB4_CODE
        )

    override suspend fun fetchHistory(
        asset: MarketAssetCatalog.AssetDescriptor,
        range: MarketHistoryRange
    ): RemoteHistoryFetchResult {
        if (!supports(asset, range)) {
            return RemoteHistoryFetchResult.Failure(
                reason = FailureReason.UNSUPPORTED_ASSET,
                detail = "Ativo não suportado sem token na brapi.dev: ${asset.code}"
            )
        }
        val (brapiRange, interval, granularity) = when (range) {
            MarketHistoryRange.D1 -> Triple("1d", "15m", MarketHistoryGranularity.INTRADAY)
            MarketHistoryRange.W1 -> Triple("5d", "1h", MarketHistoryGranularity.INTRADAY)
            MarketHistoryRange.M1 -> Triple("1mo", "1d", MarketHistoryGranularity.DAILY)
            MarketHistoryRange.M3 -> Triple("3mo", "1d", MarketHistoryGranularity.DAILY)
            MarketHistoryRange.M6 -> Triple("6mo", "1d", MarketHistoryGranularity.DAILY)
            MarketHistoryRange.Y1 -> Triple("1y", "1d", MarketHistoryGranularity.DAILY)
        }
        val url = "https://brapi.dev/api/quote/${asset.ticker}?range=$brapiRange&interval=$interval"
        val response = transport.get(url).getOrElse { err ->
            return mapHistoryExceptionToFailure(err, "brapi.dev")
        }
        return if (response.statusCode in 200..299) {
            MarketHistoryParsers.parseBrapiHistorical(response.body, asset, range, granularity, clock())
        } else {
            mapHistoryHttpFailure(response.statusCode, "brapi.dev")
        }
    }
}

class B3FluctuationHistoryDataSource(
    private val transport: MarketHttpTransport,
    private val clock: () -> Long = DateTimeUtils::nowEpochMillis
) : MarketHistoryRemoteDataSource {

    override fun supports(asset: MarketAssetCatalog.AssetDescriptor, range: MarketHistoryRange): Boolean =
        range == MarketHistoryRange.D1 && asset.code in setOf(
            MarketAssetCatalog.IBOV_CODE,
            MarketAssetCatalog.PETR4_CODE,
            MarketAssetCatalog.VALE3_CODE,
            MarketAssetCatalog.ITUB4_CODE
        )

    override suspend fun fetchHistory(
        asset: MarketAssetCatalog.AssetDescriptor,
        range: MarketHistoryRange
    ): RemoteHistoryFetchResult {
        if (!supports(asset, range)) {
            return RemoteHistoryFetchResult.Failure(
                reason = FailureReason.UNSUPPORTED_ASSET,
                detail = "B3 Flutuação Diária disponível apenas para 1D em ativos B3"
            )
        }
        val url = "https://cotacao.b3.com.br/mds/api/v1/DailyFluctuationHistory/${asset.ticker}"
        val response = transport.get(url).getOrElse { err ->
            return mapHistoryExceptionToFailure(err, "B3 (Pregão Público)")
        }
        return if (response.statusCode in 200..299) {
            MarketHistoryParsers.parseB3DailyFluctuation(response.body, asset, clock())
        } else {
            mapHistoryHttpFailure(response.statusCode, "B3 (Pregão Público)")
        }
    }
}

class NasdaqHistoryDataSource(
    private val transport: MarketHttpTransport,
    private val clock: () -> Long = DateTimeUtils::nowEpochMillis,
    private val todayProvider: () -> LocalDate = DateTimeUtils::todayLocalDate
) : MarketHistoryRemoteDataSource {

    override fun supports(asset: MarketAssetCatalog.AssetDescriptor, range: MarketHistoryRange): Boolean =
        (asset.code == MarketAssetCatalog.IXIC_CODE || asset.code == MarketAssetCatalog.MELI_CODE) &&
            range != MarketHistoryRange.W1

    override suspend fun fetchHistory(
        asset: MarketAssetCatalog.AssetDescriptor,
        range: MarketHistoryRange
    ): RemoteHistoryFetchResult {
        val target = when (asset.code) {
            MarketAssetCatalog.IXIC_CODE -> "COMP" to "index"
            MarketAssetCatalog.MELI_CODE -> "MELI" to "stocks"
            else -> return RemoteHistoryFetchResult.Failure(
                reason = FailureReason.UNSUPPORTED_ASSET,
                detail = "Ativo não suportado na Nasdaq: ${asset.code}"
            )
        }
        if (range == MarketHistoryRange.W1) {
            return RemoteHistoryFetchResult.Failure(
                reason = FailureReason.UNSUPPORTED_ASSET,
                detail = "Nasdaq não oferece endpoint dedicado 1W intradiário para ${asset.code}"
            )
        }

        val headers = mapOf(
            "Origin" to "https://www.nasdaq.com",
            "Referer" to "https://www.nasdaq.com/"
        )

        return if (range == MarketHistoryRange.D1) {
            val url = "https://api.nasdaq.com/api/quote/${target.first}/chart?assetclass=${target.second}"
            val response = transport.get(url, headers).getOrElse { err ->
                return mapHistoryExceptionToFailure(err, "Nasdaq")
            }
            if (response.statusCode in 200..299) {
                MarketHistoryParsers.parseNasdaqChart(response.body, asset, clock())
            } else {
                mapHistoryHttpFailure(response.statusCode, "Nasdaq")
            }
        } else {
            val endDate = todayProvider()
            val startDate = endDate.minusDays(range.calendarDays.toLong())
            val url = "https://api.nasdaq.com/api/quote/${target.first}/historical" +
                "?assetclass=${target.second}&fromdate=$startDate&limit=300&todate=$endDate"
            val response = transport.get(url, headers).getOrElse { err ->
                return mapHistoryExceptionToFailure(err, "Nasdaq")
            }
            if (response.statusCode in 200..299) {
                MarketHistoryParsers.parseNasdaqHistoricalTable(response.body, asset, range, clock())
            } else {
                mapHistoryHttpFailure(response.statusCode, "Nasdaq")
            }
        }
    }
}

class CnbcHistoryDataSource(
    private val transport: MarketHttpTransport,
    private val clock: () -> Long = DateTimeUtils::nowEpochMillis,
    private val todayProvider: () -> LocalDate = DateTimeUtils::todayLocalDate
) : MarketHistoryRemoteDataSource {

    private data class CnbcTarget(val symbol: String, val zoneId: ZoneId)

    private val targets = mapOf(
        MarketAssetCatalog.IBOV_CODE to CnbcTarget(".BVSP", DateTimeUtils.ZONE_SAO_PAULO),
        MarketAssetCatalog.SPX_CODE to CnbcTarget(".SPX", ZoneId.of("America/New_York")),
        MarketAssetCatalog.IXIC_CODE to CnbcTarget(".IXIC", ZoneId.of("America/New_York")),
        MarketAssetCatalog.MELI_CODE to CnbcTarget("MELI", ZoneId.of("America/New_York"))
    )

    override fun supports(asset: MarketAssetCatalog.AssetDescriptor, range: MarketHistoryRange): Boolean =
        asset.code in targets

    override suspend fun fetchHistory(
        asset: MarketAssetCatalog.AssetDescriptor,
        range: MarketHistoryRange
    ): RemoteHistoryFetchResult {
        val target = targets[asset.code]
            ?: return RemoteHistoryFetchResult.Failure(
                reason = FailureReason.UNSUPPORTED_ASSET,
                detail = "Ativo não suportado pela CNBC: ${asset.code}"
            )

        val endDate = todayProvider()
        val (barType, lookbackDays, granularity, latestOnly) = when (range) {
            MarketHistoryRange.D1 -> Quad("30M", 6L, MarketHistoryGranularity.INTRADAY, true)
            MarketHistoryRange.W1 -> Quad("1H", 7L, MarketHistoryGranularity.INTRADAY, false)
            MarketHistoryRange.M1 -> Quad("1D", 30L, MarketHistoryGranularity.DAILY, false)
            MarketHistoryRange.M3 -> Quad("1D", 90L, MarketHistoryGranularity.DAILY, false)
            MarketHistoryRange.M6 -> Quad("1D", 180L, MarketHistoryGranularity.DAILY, false)
            MarketHistoryRange.Y1 -> Quad("1D", 365L, MarketHistoryGranularity.DAILY, false)
        }
        val startDate = endDate.minusDays(lookbackDays)
        val startParam = "${startDate.format(BASIC_DATE_FMT)}000000"
        val endParam = "${endDate.format(BASIC_DATE_FMT)}235959"
        val url = "https://ts-api.cnbc.com/harmony/app/bars/${target.symbol}/$barType/" +
            "$startParam/$endParam/adjusted/EST5EDT.json"

        val response = transport.get(url).getOrElse { err ->
            return mapHistoryExceptionToFailure(err, "CNBC Market")
        }
        return if (response.statusCode in 200..299) {
            MarketHistoryParsers.parseCnbcBars(
                jsonBody = response.body,
                asset = asset,
                range = range,
                granularity = granularity,
                marketZone = target.zoneId,
                latestSessionOnly = latestOnly,
                fetchedAtEpochMs = clock()
            )
        } else {
            mapHistoryHttpFailure(response.statusCode, "CNBC Market")
        }
    }

    private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

    companion object {
        private val BASIC_DATE_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd", Locale.US)
    }
}
