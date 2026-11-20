package com.nexus.app.data.market

import com.nexus.app.core.DateTimeUtils
import com.nexus.app.domain.model.AssetCategory
import com.nexus.app.domain.model.MarketAssetCatalog
import com.nexus.app.domain.model.MarketQuoteCalculations
import com.nexus.app.domain.model.MarketQuoteSnapshot
import com.nexus.app.domain.model.QuoteModality
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class FailureReason {
    NETWORK_UNAVAILABLE,
    TIMEOUT,
    RATE_LIMITED,
    HTTP_ERROR,
    INVALID_RESPONSE,
    UNSUPPORTED_ASSET
}

sealed class RemoteQuoteFetchResult {
    data class Success(val quote: MarketQuoteSnapshot) : RemoteQuoteFetchResult()
    data class Failure(
        val reason: FailureReason,
        val httpStatusCode: Int? = null,
        val detail: String
    ) : RemoteQuoteFetchResult()
}

interface MarketQuoteRemoteDataSource {
    fun supports(asset: MarketAssetCatalog.AssetDescriptor): Boolean = true

    suspend fun fetchQuote(asset: MarketAssetCatalog.AssetDescriptor): RemoteQuoteFetchResult

    suspend fun fetchQuotes(
        assets: List<MarketAssetCatalog.AssetDescriptor>
    ): Map<String, RemoteQuoteFetchResult> {
        return assets.associate { asset ->
            asset.code to fetchQuote(asset)
        }
    }
}

data class HttpResponsePayload(
    val statusCode: Int,
    val body: String
)

interface MarketHttpTransport {
    suspend fun get(url: String, headers: Map<String, String> = emptyMap()): Result<HttpResponsePayload>
}

class HttpsUrlConnectionTransport(
    private val connectTimeoutMs: Int = 10_000,
    private val readTimeoutMs: Int = 10_000
) : MarketHttpTransport {
    override suspend fun get(url: String, headers: Map<String, String>): Result<HttpResponsePayload> =
        withContext(Dispatchers.IO) {
            runCatching {
                val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = connectTimeoutMs
                    readTimeout = readTimeoutMs
                    instanceFollowRedirects = true
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("User-Agent", "NEXUS-Android/1.0")
                    headers.forEach { (key, value) -> setRequestProperty(key, value) }
                }
                try {
                    val code = connection.responseCode
                    val stream = if (code in 200..299) connection.inputStream else connection.errorStream
                    val body = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                    HttpResponsePayload(statusCode = code, body = body)
                } finally {
                    connection.disconnect()
                }
            }
        }
}

object MarketQuoteParsers {
    private val json = Json { ignoreUnknownKeys = true }
    private val saoPauloDateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.US)
    private val nasdaqDateTimeFormatter =
        DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a", Locale.US)
    private val nasdaqDateOnlyFormatter =
        DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US)
    private val cnbcDateOnlyFormatter =
        DateTimeFormatter.ofPattern("MM/dd/yy", Locale.US)
    private val cnbcIsoDateFormatter =
        DateTimeFormatter.ISO_LOCAL_DATE
    private val zoneNewYork: ZoneId = ZoneId.of("America/New_York")

    private val coinGeckoIdByAssetCode = mapOf(
        MarketAssetCatalog.BTC_BRL_CODE to "bitcoin",
        MarketAssetCatalog.ETH_BRL_CODE to "ethereum",
        MarketAssetCatalog.SOL_BRL_CODE to "solana"
    )

    fun parseAwesomeApiUsdBrl(
        jsonBody: String,
        fetchedAtEpochMs: Long
    ): RemoteQuoteFetchResult = parseAwesomeApiPair(
        jsonBody = jsonBody,
        asset = MarketAssetCatalog.USD_BRL,
        fetchedAtEpochMs = fetchedAtEpochMs
    )

    fun parseAwesomeApiPair(
        jsonBody: String,
        asset: MarketAssetCatalog.AssetDescriptor,
        fetchedAtEpochMs: Long
    ): RemoteQuoteFetchResult {
        val root = runCatching { json.parseToJsonElement(jsonBody) }.getOrNull() as? JsonObject
            ?: return RemoteQuoteFetchResult.Failure(
                reason = FailureReason.INVALID_RESPONSE,
                detail = "Resposta JSON inválida da AwesomeAPI"
            )
        return parseAwesomeApiObject(root, asset, fetchedAtEpochMs)
    }

    fun parseAwesomeApiBatch(
        jsonBody: String,
        assets: List<MarketAssetCatalog.AssetDescriptor>,
        fetchedAtEpochMs: Long
    ): Map<String, RemoteQuoteFetchResult> {
        val root = runCatching { json.parseToJsonElement(jsonBody) }.getOrNull() as? JsonObject
            ?: return assets.associate { asset ->
                asset.code to RemoteQuoteFetchResult.Failure(
                    reason = FailureReason.INVALID_RESPONSE,
                    detail = "Resposta JSON inválida da AwesomeAPI"
                )
            }

        return assets.associate { asset ->
            asset.code to parseAwesomeApiObject(root, asset, fetchedAtEpochMs)
        }
    }

    private fun parseAwesomeApiObject(
        root: JsonObject,
        asset: MarketAssetCatalog.AssetDescriptor,
        fetchedAtEpochMs: Long
    ): RemoteQuoteFetchResult {
        val compactKey = "${asset.baseSymbol}${asset.quoteSymbol}"
        val pairObject = (root[compactKey] as? JsonObject)
            ?: (root[asset.code] as? JsonObject)
            ?: return RemoteQuoteFetchResult.Failure(
                reason = FailureReason.INVALID_RESPONSE,
                detail = "Par $compactKey ausente na resposta da AwesomeAPI"
            )

        val bid = parseDoubleField(pairObject, "bid")
        if (!MarketQuoteCalculations.isValidPositiveRate(bid)) {
            return RemoteQuoteFetchResult.Failure(
                reason = FailureReason.INVALID_RESPONSE,
                detail = "Cotação de compra inválida na AwesomeAPI para ${asset.code}"
            )
        }
        val validBid = bid!!

        val ask = parseDoubleField(pairObject, "ask")
            ?.takeIf { MarketQuoteCalculations.isValidPositiveRate(it) }
        val pctChange = parseDoubleField(pairObject, "pctChange")
            ?.takeIf { it.isFinite() }

        val previousClose = pctChange?.let { pct ->
            val factor = 1.0 + (pct / 100.0)
            if (factor > 0.0) (validBid / factor).takeIf { it.isFinite() && it > 0.0 } else null
        }

        val timestampRaw = (pairObject["timestamp"] as? JsonPrimitive)?.contentOrNull?.trim()?.toLongOrNull()
        val createDateRaw = (pairObject["create_date"] as? JsonPrimitive)?.contentOrNull?.trim()
        val sourceTimestampMs = when {
            timestampRaw != null && timestampRaw > 0L -> {
                if (timestampRaw < 10_000_000_000L) timestampRaw * 1000L else timestampRaw
            }
            !createDateRaw.isNullOrBlank() -> parseSaoPauloTimestamp(createDateRaw) ?: fetchedAtEpochMs
            else -> fetchedAtEpochMs
        }

        return RemoteQuoteFetchResult.Success(
            MarketQuoteSnapshot(
                assetCode = asset.code,
                category = asset.category,
                baseCurrency = asset.baseSymbol,
                quoteCurrency = asset.quoteSymbol,
                modality = QuoteModality.COMMERCIAL,
                price = validBid,
                bidPrice = validBid,
                askPrice = ask,
                previousClose = previousClose,
                changePercent = pctChange,
                variationPeriodLabel = if (pctChange != null) "Dia" else null,
                trend = MarketQuoteCalculations.computeTrend(pctChange),
                sourceName = "AwesomeAPI",
                sourceTimestampEpochMs = sourceTimestampMs,
                fetchedAtEpochMs = fetchedAtEpochMs
            )
        )
    }

    private data class PtaxEntry(
        val buy: Double?,
        val sell: Double,
        val timestampEpochMs: Long,
        val localDate: LocalDate
    )

    fun parseBcbOlindaPtaxUsdBrl(
        jsonBody: String,
        fetchedAtEpochMs: Long
    ): RemoteQuoteFetchResult = parseBcbOlindaPtax(
        jsonBody = jsonBody,
        asset = MarketAssetCatalog.USD_BRL,
        fetchedAtEpochMs = fetchedAtEpochMs
    )

    /**
     * Analisa a resposta OData da API Olinda do Banco Central do Brasil (PTAX) para USD ou EUR.
     * Modalidade explicitamente marcada como QuoteModality.PTAX.
     */
    fun parseBcbOlindaPtax(
        jsonBody: String,
        asset: MarketAssetCatalog.AssetDescriptor,
        fetchedAtEpochMs: Long
    ): RemoteQuoteFetchResult {
        val root = runCatching { json.parseToJsonElement(jsonBody) }.getOrNull() as? JsonObject
            ?: return RemoteQuoteFetchResult.Failure(
                reason = FailureReason.INVALID_RESPONSE,
                detail = "Resposta JSON inválida do Banco Central (PTAX)"
            )

        val entriesArray = root["value"] as? JsonArray
            ?: return RemoteQuoteFetchResult.Failure(
                reason = FailureReason.INVALID_RESPONSE,
                detail = "Campo value ausente na resposta do Banco Central (PTAX)"
            )

        val validEntries = entriesArray.mapNotNull { element ->
            val obj = element as? JsonObject ?: return@mapNotNull null
            val sell = parseDoubleField(obj, "cotacaoVenda") ?: return@mapNotNull null
            if (!MarketQuoteCalculations.isValidPositiveRate(sell)) return@mapNotNull null

            val buy = parseDoubleField(obj, "cotacaoCompra")
                ?.takeIf { MarketQuoteCalculations.isValidPositiveRate(it) }
            val dateStr = (obj["dataHoraCotacao"] as? JsonPrimitive)?.contentOrNull?.trim()
                ?: return@mapNotNull null
            val ts = parseSaoPauloTimestamp(dateStr) ?: return@mapNotNull null
            val date = DateTimeUtils.toLocalDate(ts)
            PtaxEntry(
                buy = buy,
                sell = sell,
                timestampEpochMs = ts,
                localDate = date
            )
        }.sortedByDescending { it.timestampEpochMs }

        if (validEntries.isEmpty()) {
            return RemoteQuoteFetchResult.Failure(
                reason = FailureReason.INVALID_RESPONSE,
                detail = "Nenhuma cotação PTAX válida encontrada para ${asset.code}"
            )
        }

        val latest = validEntries.first()
        val previousDay = validEntries.firstOrNull { it.localDate.isBefore(latest.localDate) }
        val changePercent = MarketQuoteCalculations.computeChangePercent(
            currentPrice = latest.sell,
            previousPrice = previousDay?.sell
        )

        return RemoteQuoteFetchResult.Success(
            MarketQuoteSnapshot(
                assetCode = asset.code,
                category = asset.category,
                baseCurrency = asset.baseSymbol,
                quoteCurrency = asset.quoteSymbol,
                modality = QuoteModality.PTAX,
                price = latest.sell,
                bidPrice = latest.buy,
                askPrice = latest.sell,
                previousClose = previousDay?.sell,
                changePercent = changePercent,
                variationPeriodLabel = if (changePercent != null) "Diária" else null,
                trend = MarketQuoteCalculations.computeTrend(changePercent),
                sourceName = "Banco Central (PTAX)",
                sourceTimestampEpochMs = latest.timestampEpochMs,
                fetchedAtEpochMs = fetchedAtEpochMs
            )
        )
    }

    /**
     * Analisa a resposta em lote da CoinGecko (/api/v3/simple/price) em BRL.
     * Exemplo: {"bitcoin":{"brl":371523,"brl_24h_change":-0.96,"last_updated_at":1795003282}}
     */
    fun parseCoinGeckoPrices(
        jsonBody: String,
        assets: List<MarketAssetCatalog.AssetDescriptor>,
        fetchedAtEpochMs: Long
    ): Map<String, RemoteQuoteFetchResult> {
        val root = runCatching { json.parseToJsonElement(jsonBody) }.getOrNull() as? JsonObject
            ?: return assets.associate { asset ->
                asset.code to RemoteQuoteFetchResult.Failure(
                    reason = FailureReason.INVALID_RESPONSE,
                    detail = "Resposta JSON inválida da CoinGecko"
                )
            }

        return assets.associate { asset ->
            val coinId = coinGeckoIdByAssetCode[asset.code]
            if (coinId == null) {
                asset.code to RemoteQuoteFetchResult.Failure(
                    reason = FailureReason.UNSUPPORTED_ASSET,
                    detail = "Ativo não mapeado na CoinGecko: ${asset.code}"
                )
            } else {
                val coinObj = root[coinId] as? JsonObject
                if (coinObj == null) {
                    asset.code to RemoteQuoteFetchResult.Failure(
                        reason = FailureReason.INVALID_RESPONSE,
                        detail = "Ativo $coinId ausente na resposta da CoinGecko"
                    )
                } else {
                    val priceBrl = parseDoubleField(coinObj, "brl")
                    if (!MarketQuoteCalculations.isValidPositiveRate(priceBrl)) {
                        asset.code to RemoteQuoteFetchResult.Failure(
                            reason = FailureReason.INVALID_RESPONSE,
                            detail = "Preço BRL inválido na CoinGecko para ${asset.code}"
                        )
                    } else {
                        val validPrice = priceBrl!!
                        val change24h = parseDoubleField(coinObj, "brl_24h_change")
                            ?.takeIf { it.isFinite() }
                        val previousClose = change24h?.let { pct ->
                            val factor = 1.0 + (pct / 100.0)
                            if (factor > 0.0) (validPrice / factor).takeIf { it.isFinite() && it > 0.0 } else null
                        }
                        val updatedSec = (coinObj["last_updated_at"] as? JsonPrimitive)
                            ?.contentOrNull?.trim()?.toLongOrNull()
                        val sourceTimestampMs = if (updatedSec != null && updatedSec > 0L) {
                            if (updatedSec < 10_000_000_000L) updatedSec * 1000L else updatedSec
                        } else {
                            fetchedAtEpochMs
                        }
                        asset.code to RemoteQuoteFetchResult.Success(
                            MarketQuoteSnapshot(
                                assetCode = asset.code,
                                category = asset.category,
                                baseCurrency = asset.baseSymbol,
                                quoteCurrency = asset.quoteSymbol,
                                modality = QuoteModality.SPOT,
                                price = validPrice,
                                previousClose = previousClose,
                                changePercent = change24h,
                                variationPeriodLabel = if (change24h != null) "24h" else null,
                                trend = MarketQuoteCalculations.computeTrend(change24h),
                                sourceName = "CoinGecko",
                                sourceTimestampEpochMs = sourceTimestampMs,
                                fetchedAtEpochMs = fetchedAtEpochMs
                            )
                        )
                    }
                }
            }
        }
    }

    /**
     * Analisa a resposta individual da CoinPaprika (/v1/tickers/{id}?quotes=BRL).
     */
    fun parseCoinPaprikaTicker(
        jsonBody: String,
        asset: MarketAssetCatalog.AssetDescriptor,
        fetchedAtEpochMs: Long
    ): RemoteQuoteFetchResult {
        val root = runCatching { json.parseToJsonElement(jsonBody) }.getOrNull() as? JsonObject
            ?: return RemoteQuoteFetchResult.Failure(
                reason = FailureReason.INVALID_RESPONSE,
                detail = "Resposta JSON inválida da CoinPaprika"
            )
        val quotesObj = root["quotes"] as? JsonObject
            ?: return RemoteQuoteFetchResult.Failure(
                reason = FailureReason.INVALID_RESPONSE,
                detail = "Objeto quotes ausente na resposta da CoinPaprika"
            )
        val brlObj = quotesObj["BRL"] as? JsonObject
            ?: return RemoteQuoteFetchResult.Failure(
                reason = FailureReason.INVALID_RESPONSE,
                detail = "Cotação BRL ausente na resposta da CoinPaprika"
            )
        val price = parseDoubleField(brlObj, "price")
        if (!MarketQuoteCalculations.isValidPositiveRate(price)) {
            return RemoteQuoteFetchResult.Failure(
                reason = FailureReason.INVALID_RESPONSE,
                detail = "Preço inválido na CoinPaprika para ${asset.code}"
            )
        }
        val validPrice = price!!
        val change24h = parseDoubleField(brlObj, "percent_change_24h")
            ?.takeIf { it.isFinite() }
        val previousClose = change24h?.let { pct ->
            val factor = 1.0 + (pct / 100.0)
            if (factor > 0.0) (validPrice / factor).takeIf { it.isFinite() && it > 0.0 } else null
        }
        val lastUpdatedIso = (root["last_updated"] as? JsonPrimitive)?.contentOrNull?.trim()
        val sourceTimestampMs = lastUpdatedIso
            ?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() }
            ?: fetchedAtEpochMs

        return RemoteQuoteFetchResult.Success(
            MarketQuoteSnapshot(
                assetCode = asset.code,
                category = asset.category,
                baseCurrency = asset.baseSymbol,
                quoteCurrency = asset.quoteSymbol,
                modality = QuoteModality.SPOT,
                price = validPrice,
                previousClose = previousClose,
                changePercent = change24h,
                variationPeriodLabel = if (change24h != null) "24h" else null,
                trend = MarketQuoteCalculations.computeTrend(change24h),
                sourceName = "CoinPaprika",
                sourceTimestampEpochMs = sourceTimestampMs,
                fetchedAtEpochMs = fetchedAtEpochMs
            )
        )
    }

    /**
     * Analisa resposta pública oficial da B3 (/mds/api/v1/instrumentQuotation/{symbol})
     * para IBOV ou ações B3 (PETR4, VALE3, ITUB4). Feed público gratuito possui atraso (~15 min).
     */
    fun parseB3InstrumentQuotation(
        jsonBody: String,
        asset: MarketAssetCatalog.AssetDescriptor,
        fetchedAtEpochMs: Long
    ): RemoteQuoteFetchResult {
        val root = runCatching { json.parseToJsonElement(jsonBody) }.getOrNull() as? JsonObject
            ?: return RemoteQuoteFetchResult.Failure(
                reason = FailureReason.INVALID_RESPONSE,
                detail = "Resposta JSON inválida da B3"
            )
        val statusCode = ((root["BizSts"] as? JsonObject)?.get("cd") as? JsonPrimitive)?.contentOrNull
        if (!statusCode.equals("OK", ignoreCase = true)) {
            return RemoteQuoteFetchResult.Failure(
                reason = FailureReason.INVALID_RESPONSE,
                detail = "Status inválido da B3 para ${asset.ticker}"
            )
        }
        val tradArray = root["Trad"] as? JsonArray
            ?: return RemoteQuoteFetchResult.Failure(
                reason = FailureReason.INVALID_RESPONSE,
                detail = "Dados de negociação ausentes na B3 para ${asset.ticker}"
            )
        val firstTrad = tradArray.firstOrNull() as? JsonObject
            ?: return RemoteQuoteFetchResult.Failure(
                reason = FailureReason.INVALID_RESPONSE,
                detail = "Nenhuma cotação listada na B3 para ${asset.ticker}"
            )
        val scty = firstTrad["scty"] as? JsonObject
            ?: return RemoteQuoteFetchResult.Failure(
                reason = FailureReason.INVALID_RESPONSE,
                detail = "Estrutura scty ausente na B3 para ${asset.ticker}"
            )
        val qtn = scty["SctyQtn"] as? JsonObject
            ?: return RemoteQuoteFetchResult.Failure(
                reason = FailureReason.INVALID_RESPONSE,
                detail = "Estrutura SctyQtn ausente na B3 para ${asset.ticker}"
            )
        val price = parseDoubleField(qtn, "curPrc")
        if (!MarketQuoteCalculations.isValidPositiveRate(price)) {
            return RemoteQuoteFetchResult.Failure(
                reason = FailureReason.INVALID_RESPONSE,
                detail = "Preço inválido na B3 para ${asset.ticker}"
            )
        }
        val validPrice = price!!
        val pctChange = parseDoubleField(qtn, "prcFlcn")?.takeIf { it.isFinite() }
        val previousClose = pctChange?.let { pct ->
            val factor = 1.0 + (pct / 100.0)
            if (factor > 0.0) (validPrice / factor).takeIf { it.isFinite() && it > 0.0 } else null
        }
        val msgDateTime = ((root["Msg"] as? JsonObject)?.get("dtTm") as? JsonPrimitive)?.contentOrNull?.trim()
        val sourceTimestampMs = msgDateTime?.let(::parseSaoPauloTimestamp) ?: fetchedAtEpochMs

        return RemoteQuoteFetchResult.Success(
            MarketQuoteSnapshot(
                assetCode = asset.code,
                category = asset.category,
                baseCurrency = asset.ticker,
                quoteCurrency = asset.quoteSymbol,
                modality = QuoteModality.DELAYED,
                price = validPrice,
                previousClose = previousClose,
                changePercent = pctChange,
                variationPeriodLabel = if (pctChange != null) "Dia" else null,
                trend = MarketQuoteCalculations.computeTrend(pctChange),
                sourceName = "B3 (Cotação Pública)",
                sourceTimestampEpochMs = sourceTimestampMs,
                fetchedAtEpochMs = fetchedAtEpochMs,
                exchange = "B3",
                isDelayed = true,
                delayNotice = "Feed público da B3 com atraso aproximado de 15 min"
            )
        )
    }

    /**
     * Analisa resposta em lote da brapi.dev (/api/quote/PETR4,VALE3,ITUB4) para ações B3 gratuitas.
     */
    fun parseBrapiBatch(
        jsonBody: String,
        assets: List<MarketAssetCatalog.AssetDescriptor>,
        fetchedAtEpochMs: Long
    ): Map<String, RemoteQuoteFetchResult> {
        val root = runCatching { json.parseToJsonElement(jsonBody) }.getOrNull() as? JsonObject
            ?: return assets.associate { asset ->
                asset.code to RemoteQuoteFetchResult.Failure(
                    reason = FailureReason.INVALID_RESPONSE,
                    detail = "Resposta JSON inválida da brapi.dev"
                )
            }
        val resultsArray = root["results"] as? JsonArray
            ?: return assets.associate { asset ->
                asset.code to RemoteQuoteFetchResult.Failure(
                    reason = FailureReason.INVALID_RESPONSE,
                    detail = "Campo results ausente na resposta da brapi.dev"
                )
            }
        val bySymbol = resultsArray.mapNotNull { el ->
            val obj = el as? JsonObject ?: return@mapNotNull null
            val sym = (obj["symbol"] as? JsonPrimitive)?.contentOrNull?.trim()?.uppercase()
                ?: return@mapNotNull null
            sym to obj
        }.toMap()

        return assets.associate { asset ->
            val obj = bySymbol[asset.ticker.uppercase()]
            if (obj == null) {
                asset.code to RemoteQuoteFetchResult.Failure(
                    reason = FailureReason.INVALID_RESPONSE,
                    detail = "Ativo ${asset.ticker} ausente na resposta da brapi.dev"
                )
            } else {
                val price = parseDoubleField(obj, "regularMarketPrice")
                if (!MarketQuoteCalculations.isValidPositiveRate(price)) {
                    asset.code to RemoteQuoteFetchResult.Failure(
                        reason = FailureReason.INVALID_RESPONSE,
                        detail = "Preço inválido na brapi.dev para ${asset.ticker}"
                    )
                } else {
                    val validPrice = price!!
                    val currency = (obj["currency"] as? JsonPrimitive)?.contentOrNull?.trim()
                        ?.takeIf { it.isNotEmpty() } ?: asset.quoteSymbol
                    val prevClose = parseDoubleField(obj, "regularMarketPreviousClose")
                        ?.takeIf { MarketQuoteCalculations.isValidPositiveRate(it) }
                    val pctChange = parseDoubleField(obj, "regularMarketChangePercent")
                        ?.takeIf { it.isFinite() }
                        ?: MarketQuoteCalculations.computeChangePercent(validPrice, prevClose)
                    val timeRaw = (obj["regularMarketTime"] as? JsonPrimitive)?.contentOrNull?.trim()
                    val sourceTs = timeRaw?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() }
                        ?: fetchedAtEpochMs

                    asset.code to RemoteQuoteFetchResult.Success(
                        MarketQuoteSnapshot(
                            assetCode = asset.code,
                            category = asset.category,
                            baseCurrency = asset.ticker,
                            quoteCurrency = currency,
                            modality = QuoteModality.EXCHANGE,
                            price = validPrice,
                            previousClose = prevClose,
                            changePercent = pctChange,
                            variationPeriodLabel = if (pctChange != null) "Dia" else null,
                            trend = MarketQuoteCalculations.computeTrend(pctChange),
                            sourceName = "brapi.dev (B3)",
                            sourceTimestampEpochMs = sourceTs,
                            fetchedAtEpochMs = fetchedAtEpochMs,
                            exchange = "B3",
                            isDelayed = false,
                            delayNotice = null
                        )
                    )
                }
            }
        }
    }

    /**
     * Analisa lote da CNBC Quote Webservice (.SPX, .IXIC, MELI, .BVSP).
     */
    fun parseCnbcBatch(
        jsonBody: String,
        assets: List<MarketAssetCatalog.AssetDescriptor>,
        symbolForAsset: (MarketAssetCatalog.AssetDescriptor) -> String?,
        fetchedAtEpochMs: Long
    ): Map<String, RemoteQuoteFetchResult> {
        val root = runCatching { json.parseToJsonElement(jsonBody) }.getOrNull() as? JsonObject
            ?: return assets.associate { asset ->
                asset.code to RemoteQuoteFetchResult.Failure(
                    reason = FailureReason.INVALID_RESPONSE,
                    detail = "Resposta JSON inválida da CNBC"
                )
            }
        val quoteResult = root["FormattedQuoteResult"] as? JsonObject
        val quotesArray = quoteResult?.get("FormattedQuote") as? JsonArray
            ?: return assets.associate { asset ->
                asset.code to RemoteQuoteFetchResult.Failure(
                    reason = FailureReason.INVALID_RESPONSE,
                    detail = "FormattedQuote ausente na resposta da CNBC"
                )
            }
        val bySymbol = quotesArray.mapNotNull { el ->
            val obj = el as? JsonObject ?: return@mapNotNull null
            val sym = (obj["symbol"] as? JsonPrimitive)?.contentOrNull?.trim()?.uppercase()
                ?: return@mapNotNull null
            sym to obj
        }.toMap()

        return assets.associate { asset ->
            val providerSym = symbolForAsset(asset)?.uppercase()
            val obj = providerSym?.let { bySymbol[it] }
            if (obj == null) {
                asset.code to RemoteQuoteFetchResult.Failure(
                    reason = FailureReason.UNSUPPORTED_ASSET,
                    detail = "Ativo ${asset.ticker} não retornado pela CNBC"
                )
            } else {
                val lastRaw = (obj["last"] as? JsonPrimitive)?.contentOrNull
                val price = parseUsNumericText(lastRaw)
                if (!MarketQuoteCalculations.isValidPositiveRate(price)) {
                    asset.code to RemoteQuoteFetchResult.Failure(
                        reason = FailureReason.INVALID_RESPONSE,
                        detail = "Cotação inválida na CNBC para ${asset.ticker}"
                    )
                } else {
                    val validPrice = price!!
                    val pctRaw = (obj["change_pct"] as? JsonPrimitive)?.contentOrNull
                    val pctChange = parseUsChangePercent(pctRaw)
                    val netChange = parseUsChangeValue((obj["change"] as? JsonPrimitive)?.contentOrNull)
                    val prevDayClosing = parseUsNumericText(
                        (obj["previous_day_closing"] as? JsonPrimitive)?.contentOrNull
                    )?.takeIf { MarketQuoteCalculations.isValidPositiveRate(it) }
                    val computedPrev = netChange?.let { delta ->
                        (validPrice - delta).takeIf { MarketQuoteCalculations.isValidPositiveRate(it) }
                    } ?: prevDayClosing

                    val currencyCode = (obj["currencyCode"] as? JsonPrimitive)?.contentOrNull?.trim()
                        ?.takeIf { it.isNotEmpty() } ?: asset.quoteSymbol
                    val rawExchange = (obj["exchange"] as? JsonPrimitive)?.contentOrNull?.trim()
                    val resolvedExchange = normalizeExchangeName(rawExchange, asset.exchange)
                    val realTime = (obj["realTime"] as? JsonPrimitive)?.contentOrNull.equals("true", ignoreCase = true)
                    val marketStatus = (obj["curmktstatus"] as? JsonPrimitive)?.contentOrNull?.trim()
                    val isDelayedQuote = !realTime || marketStatus.equals("PRE_MKT", ignoreCase = true) ||
                        marketStatus.equals("POST_MKT", ignoreCase = true)
                    val delayNotice = when {
                        !realTime -> "Dados com atraso ou último fechamento"
                        marketStatus.equals("PRE_MKT", ignoreCase = true) -> "Pré-mercado · referência último fechamento"
                        marketStatus.equals("POST_MKT", ignoreCase = true) -> "Pós-mercado · referência último fechamento"
                        else -> null
                    }
                    val modality = when {
                        isDelayedQuote -> QuoteModality.DELAYED
                        asset.category == AssetCategory.INDEX -> QuoteModality.INDEX
                        else -> QuoteModality.EXCHANGE
                    }

                    val timeZoneTag = (obj["timeZone"] as? JsonPrimitive)?.contentOrNull?.trim()
                    val lastTimeDate = (obj["last_timedate"] as? JsonPrimitive)?.contentOrNull?.trim()
                    val lastTimeIso = (obj["last_time"] as? JsonPrimitive)?.contentOrNull?.trim()
                    val sourceTs = parseCnbcTimestamp(
                        lastTimeDate = lastTimeDate,
                        lastTimeIso = lastTimeIso,
                        timeZoneTag = timeZoneTag
                    ) ?: fetchedAtEpochMs

                    asset.code to RemoteQuoteFetchResult.Success(
                        MarketQuoteSnapshot(
                            assetCode = asset.code,
                            category = asset.category,
                            baseCurrency = asset.ticker,
                            quoteCurrency = currencyCode,
                            modality = modality,
                            price = validPrice,
                            previousClose = computedPrev,
                            changePercent = pctChange,
                            variationPeriodLabel = if (pctChange != null) "Dia" else null,
                            trend = MarketQuoteCalculations.computeTrend(pctChange),
                            sourceName = "CNBC Market",
                            sourceTimestampEpochMs = sourceTs,
                            fetchedAtEpochMs = fetchedAtEpochMs,
                            exchange = resolvedExchange,
                            isDelayed = isDelayedQuote,
                            delayNotice = delayNotice
                        )
                    )
                }
            }
        }
    }

    /**
     * Analisa resposta pública da Nasdaq (/api/quote/{symbol}/info?assetclass={type})
     * usada como fallback para IXIC (COMP) e MELI.
     */
    fun parseNasdaqInfo(
        jsonBody: String,
        asset: MarketAssetCatalog.AssetDescriptor,
        fetchedAtEpochMs: Long
    ): RemoteQuoteFetchResult {
        val root = runCatching { json.parseToJsonElement(jsonBody) }.getOrNull() as? JsonObject
            ?: return RemoteQuoteFetchResult.Failure(
                reason = FailureReason.INVALID_RESPONSE,
                detail = "Resposta JSON inválida da Nasdaq"
            )
        val data = root["data"] as? JsonObject
            ?: return RemoteQuoteFetchResult.Failure(
                reason = FailureReason.INVALID_RESPONSE,
                detail = "Campo data ausente na Nasdaq para ${asset.ticker}"
            )
        val primary = data["primaryData"] as? JsonObject
            ?: return RemoteQuoteFetchResult.Failure(
                reason = FailureReason.INVALID_RESPONSE,
                detail = "Dados primários ausentes na Nasdaq para ${asset.ticker}"
            )
        val price = parseUsNumericText((primary["lastSalePrice"] as? JsonPrimitive)?.contentOrNull)
        if (!MarketQuoteCalculations.isValidPositiveRate(price)) {
            return RemoteQuoteFetchResult.Failure(
                reason = FailureReason.INVALID_RESPONSE,
                detail = "Preço inválido na Nasdaq para ${asset.ticker}"
            )
        }
        val validPrice = price!!
        val pctChange = parseUsChangePercent((primary["percentageChange"] as? JsonPrimitive)?.contentOrNull)
        val keyStats = data["keyStats"] as? JsonObject
        val prevCloseFromStats = ((keyStats?.get("previousclose") as? JsonObject)
            ?.get("value") as? JsonPrimitive)?.contentOrNull?.let(::parseUsNumericText)
            ?.takeIf { MarketQuoteCalculations.isValidPositiveRate(it) }
        val netChange = parseUsChangeValue((primary["netChange"] as? JsonPrimitive)?.contentOrNull)
        val prevClose = prevCloseFromStats ?: netChange?.let { delta ->
            (validPrice - delta).takeIf { MarketQuoteCalculations.isValidPositiveRate(it) }
        }
        val bidPrice = parseUsNumericText((primary["bidPrice"] as? JsonPrimitive)?.contentOrNull)
            ?.takeIf { MarketQuoteCalculations.isValidPositiveRate(it) }
        val askPrice = parseUsNumericText((primary["askPrice"] as? JsonPrimitive)?.contentOrNull)
            ?.takeIf { MarketQuoteCalculations.isValidPositiveRate(it) }
        val isRealTime = (primary["isRealTime"] as? JsonPrimitive)?.contentOrNull.equals("true", ignoreCase = true)
        val marketStatus = (data["marketStatus"] as? JsonPrimitive)?.contentOrNull?.trim()
        val rawExchange = (data["exchange"] as? JsonPrimitive)?.contentOrNull?.trim()
        val resolvedExchange = normalizeExchangeName(rawExchange, asset.exchange)
        val lastTradeTs = (primary["lastTradeTimestamp"] as? JsonPrimitive)?.contentOrNull?.trim()
        val sourceTs = lastTradeTs?.let(::parseNasdaqTimestamp) ?: fetchedAtEpochMs

        val isDelayedQuote = !isRealTime
        val modality = when {
            isDelayedQuote -> QuoteModality.DELAYED
            asset.category == AssetCategory.INDEX -> QuoteModality.INDEX
            else -> QuoteModality.EXCHANGE
        }
        val delayNotice = when {
            !isRealTime -> "Cotação pública Nasdaq (fechamento ou atraso)"
            !marketStatus.isNullOrBlank() && !marketStatus.equals("Market Open", ignoreCase = true) ->
                "Status Nasdaq: $marketStatus"
            else -> null
        }

        return RemoteQuoteFetchResult.Success(
            MarketQuoteSnapshot(
                assetCode = asset.code,
                category = asset.category,
                baseCurrency = asset.ticker,
                quoteCurrency = asset.quoteSymbol,
                modality = modality,
                price = validPrice,
                bidPrice = bidPrice,
                askPrice = askPrice,
                previousClose = prevClose,
                changePercent = pctChange,
                variationPeriodLabel = if (pctChange != null) "Dia" else null,
                trend = MarketQuoteCalculations.computeTrend(pctChange),
                sourceName = "Nasdaq",
                sourceTimestampEpochMs = sourceTs,
                fetchedAtEpochMs = fetchedAtEpochMs,
                exchange = resolvedExchange,
                isDelayed = isDelayedQuote,
                delayNotice = delayNotice
            )
        )
    }

    internal fun parseSaoPauloTimestamp(raw: String): Long? {
        val normalized = raw.trim().replace('T', ' ')
        val withoutFraction = normalized.substringBefore('.').trim()
        if (withoutFraction.length < 19) return null
        val candidate = withoutFraction.take(19)
        return runCatching {
            val ldt = LocalDateTime.parse(candidate, saoPauloDateTimeFormatter)
            ldt.atZone(DateTimeUtils.ZONE_SAO_PAULO).toInstant().toEpochMilli()
        }.getOrNull()
    }

    internal fun parseUsNumericText(raw: String?): Double? {
        val text = raw?.trim() ?: return null
        if (text.isEmpty() || text.equals("null", ignoreCase = true) ||
            text.equals("N/A", ignoreCase = true) || text.equals("NA", ignoreCase = true) ||
            text.equals("UNCH", ignoreCase = true)
        ) {
            return null
        }
        val cleaned = text
            .replace("$", "")
            .replace("US$", "")
            .replace("R$", "")
            .replace(",", "")
            .replace("%", "")
            .replace(" ", "")
        return cleaned.toDoubleOrNull()?.takeIf { it.isFinite() }
    }

    internal fun parseUsChangePercent(raw: String?): Double? {
        val text = raw?.trim() ?: return null
        if (text.equals("UNCH", ignoreCase = true)) return 0.0
        return parseUsNumericText(text)
    }

    internal fun parseUsChangeValue(raw: String?): Double? {
        val text = raw?.trim() ?: return null
        if (text.equals("UNCH", ignoreCase = true)) return 0.0
        return parseUsNumericText(text)
    }

    private fun parseCnbcTimestamp(
        lastTimeDate: String?,
        lastTimeIso: String?,
        timeZoneTag: String?
    ): Long? {
        val zone = when {
            timeZoneTag.equals("BET", ignoreCase = true) ||
                timeZoneTag.equals("BRT", ignoreCase = true) -> DateTimeUtils.ZONE_SAO_PAULO
            else -> zoneNewYork
        }
        if (!lastTimeDate.isNullOrBlank()) {
            val dateToken = lastTimeDate.trim().substringBefore(' ').trim()
            val parsedDate = runCatching { LocalDate.parse(dateToken, cnbcDateOnlyFormatter) }.getOrNull()
            if (parsedDate != null) {
                return parsedDate.atTime(LocalTime.NOON).atZone(zone).toInstant().toEpochMilli()
            }
        }
        if (!lastTimeIso.isNullOrBlank()) {
            val isoParsed = runCatching { Instant.parse(lastTimeIso.trim()).toEpochMilli() }.getOrNull()
            if (isoParsed != null) return isoParsed
            val dateOnly = runCatching { LocalDate.parse(lastTimeIso.trim(), cnbcIsoDateFormatter) }.getOrNull()
            if (dateOnly != null) {
                return dateOnly.atTime(LocalTime.NOON).atZone(zone).toInstant().toEpochMilli()
            }
        }
        return null
    }

    private fun parseNasdaqTimestamp(raw: String): Long? {
        val cleaned = raw
            .replace("Closed at ", "", ignoreCase = true)
            .replace(" ET", "", ignoreCase = true)
            .replace(" EST", "", ignoreCase = true)
            .replace(" EDT", "", ignoreCase = true)
            .trim()
        runCatching { LocalDateTime.parse(cleaned, nasdaqDateTimeFormatter) }.getOrNull()?.let { ldt ->
            return ldt.atZone(zoneNewYork).toInstant().toEpochMilli()
        }
        runCatching { LocalDate.parse(cleaned, nasdaqDateOnlyFormatter) }.getOrNull()?.let { date ->
            return date.atTime(LocalTime.NOON).atZone(zoneNewYork).toInstant().toEpochMilli()
        }
        return null
    }

    private fun normalizeExchangeName(rawExchange: String?, fallback: String?): String? {
        val trimmed = rawExchange?.trim()?.takeIf { it.isNotEmpty() } ?: return fallback
        return when {
            trimmed.equals("INDEX", ignoreCase = true) -> fallback ?: "Índice"
            trimmed.contains("Sao Paulo", ignoreCase = true) -> "B3"
            trimmed.equals("NASDAQ", ignoreCase = true) -> "Nasdaq"
            trimmed.startsWith("NASDAQ", ignoreCase = true) -> trimmed
            else -> trimmed
        }
    }

    private fun parseDoubleField(obj: JsonObject, key: String): Double? {
        val raw = (obj[key] as? JsonPrimitive)?.contentOrNull?.trim() ?: return null
        if (raw.isEmpty() || raw.equals("null", ignoreCase = true)) return null
        return raw.replace(",", ".").toDoubleOrNull()
    }
}

class AwesomeApiUsdBrlDataSource(
    private val transport: MarketHttpTransport,
    private val clock: () -> Long = DateTimeUtils::nowEpochMillis
) : MarketQuoteRemoteDataSource {

    override fun supports(asset: MarketAssetCatalog.AssetDescriptor): Boolean =
        asset.code == MarketAssetCatalog.USD_BRL_CODE

    override suspend fun fetchQuote(asset: MarketAssetCatalog.AssetDescriptor): RemoteQuoteFetchResult {
        if (!supports(asset)) {
            return RemoteQuoteFetchResult.Failure(
                reason = FailureReason.UNSUPPORTED_ASSET,
                detail = "Ativo ainda não habilitado nesta fonte: ${asset.code}"
            )
        }
        val response = transport.get(AWESOME_USD_BRL_URL).getOrElse { err ->
            return mapExceptionToFailure(err, "AwesomeAPI")
        }
        return when (response.statusCode) {
            in 200..299 -> MarketQuoteParsers.parseAwesomeApiUsdBrl(response.body, clock())
            429 -> RemoteQuoteFetchResult.Failure(
                reason = FailureReason.RATE_LIMITED,
                httpStatusCode = 429,
                detail = "Limite temporário de requisições da AwesomeAPI atingido"
            )
            else -> RemoteQuoteFetchResult.Failure(
                reason = FailureReason.HTTP_ERROR,
                httpStatusCode = response.statusCode,
                detail = "Erro HTTP ${response.statusCode} ao consultar AwesomeAPI"
            )
        }
    }

    companion object {
        const val AWESOME_USD_BRL_URL = "https://economia.awesomeapi.com.br/json/last/USD-BRL"
    }
}

/**
 * Fonte comercial para moedas fiduciarias (USD/BRL e EUR/BRL) com suporte a lote em chamada unica.
 */
class AwesomeApiCurrencyDataSource(
    private val transport: MarketHttpTransport,
    private val clock: () -> Long = DateTimeUtils::nowEpochMillis
) : MarketQuoteRemoteDataSource {

    override fun supports(asset: MarketAssetCatalog.AssetDescriptor): Boolean =
        asset.category == AssetCategory.CURRENCY &&
            (asset.code == MarketAssetCatalog.USD_BRL_CODE || asset.code == MarketAssetCatalog.EUR_BRL_CODE)

    override suspend fun fetchQuote(asset: MarketAssetCatalog.AssetDescriptor): RemoteQuoteFetchResult {
        if (!supports(asset)) {
            return RemoteQuoteFetchResult.Failure(
                reason = FailureReason.UNSUPPORTED_ASSET,
                detail = "Ativo não suportado pela fonte AwesomeAPI Moedas: ${asset.code}"
            )
        }
        return fetchQuotes(listOf(asset))[asset.code] ?: RemoteQuoteFetchResult.Failure(
            reason = FailureReason.INVALID_RESPONSE,
            detail = "Resposta vazia da AwesomeAPI"
        )
    }

    override suspend fun fetchQuotes(
        assets: List<MarketAssetCatalog.AssetDescriptor>
    ): Map<String, RemoteQuoteFetchResult> {
        val supported = assets.filter { supports(it) }
        val unsupported = assets.filterNot { supports(it) }.associate { asset ->
            asset.code to RemoteQuoteFetchResult.Failure(
                reason = FailureReason.UNSUPPORTED_ASSET,
                detail = "Ativo não suportado pela AwesomeAPI Moedas: ${asset.code}"
            )
        }
        if (supported.isEmpty()) return unsupported

        val pairsParam = supported.joinToString(",") { it.code }
        val url = "https://economia.awesomeapi.com.br/json/last/$pairsParam"
        val response = transport.get(url).getOrElse { err ->
            val failure = mapExceptionToFailure(err, "AwesomeAPI")
            return unsupported + supported.associate { it.code to failure }
        }
        val parsed = when (response.statusCode) {
            in 200..299 -> MarketQuoteParsers.parseAwesomeApiBatch(response.body, supported, clock())
            429 -> {
                val failure = RemoteQuoteFetchResult.Failure(
                    reason = FailureReason.RATE_LIMITED,
                    httpStatusCode = 429,
                    detail = "Limite temporário de requisições da AwesomeAPI atingido"
                )
                supported.associate { it.code to failure }
            }
            else -> {
                val failure = RemoteQuoteFetchResult.Failure(
                    reason = FailureReason.HTTP_ERROR,
                    httpStatusCode = response.statusCode,
                    detail = "Erro HTTP ${response.statusCode} ao consultar AwesomeAPI"
                )
                supported.associate { it.code to failure }
            }
        }
        return unsupported + parsed
    }
}

class BcbOlindaPtaxDataSource(
    private val transport: MarketHttpTransport,
    private val clock: () -> Long = DateTimeUtils::nowEpochMillis,
    private val todayProvider: () -> LocalDate = DateTimeUtils::todayLocalDate
) : MarketQuoteRemoteDataSource {

    override fun supports(asset: MarketAssetCatalog.AssetDescriptor): Boolean =
        asset.code == MarketAssetCatalog.USD_BRL_CODE || asset.code == MarketAssetCatalog.EUR_BRL_CODE

    override suspend fun fetchQuote(asset: MarketAssetCatalog.AssetDescriptor): RemoteQuoteFetchResult {
        if (!supports(asset)) {
            return RemoteQuoteFetchResult.Failure(
                reason = FailureReason.UNSUPPORTED_ASSET,
                detail = "Ativo não suportado pela API PTAX do BCB: ${asset.code}"
            )
        }
        val endDate = todayProvider()
        val startDate = endDate.minusDays(14)
        val startParam = startDate.format(OLINDA_DATE_FORMATTER)
        val endParam = endDate.format(OLINDA_DATE_FORMATTER)
        val url = if (asset.code == MarketAssetCatalog.USD_BRL_CODE) {
            "https://olinda.bcb.gov.br/olinda/servico/PTAX/versao/v1/odata/" +
                "CotacaoDolarPeriodo(dataInicial=@dataInicial,dataFinalCotacao=@dataFinalCotacao)" +
                "?@dataInicial='$startParam'&@dataFinalCotacao='$endParam'" +
                "&\$top=10&\$orderby=dataHoraCotacao%20desc&\$format=json"
        } else {
            "https://olinda.bcb.gov.br/olinda/servico/PTAX/versao/v1/odata/" +
                "CotacaoMoedaPeriodo(moeda=@moeda,dataInicial=@dataInicial,dataFinalCotacao=@dataFinalCotacao)" +
                "?@moeda='${asset.baseSymbol}'&@dataInicial='$startParam'&@dataFinalCotacao='$endParam'" +
                "&\$top=20&\$orderby=dataHoraCotacao%20desc&\$format=json"
        }

        val response = transport.get(url).getOrElse { err ->
            return mapExceptionToFailure(err, "Banco Central (PTAX)")
        }
        return when (response.statusCode) {
            in 200..299 -> MarketQuoteParsers.parseBcbOlindaPtax(response.body, asset, clock())
            429 -> RemoteQuoteFetchResult.Failure(
                reason = FailureReason.RATE_LIMITED,
                httpStatusCode = 429,
                detail = "Limite de requisições atingido no serviço PTAX"
            )
            else -> RemoteQuoteFetchResult.Failure(
                reason = FailureReason.HTTP_ERROR,
                httpStatusCode = response.statusCode,
                detail = "Erro HTTP ${response.statusCode} ao consultar PTAX (BCB)"
            )
        }
    }

    override suspend fun fetchQuotes(
        assets: List<MarketAssetCatalog.AssetDescriptor>
    ): Map<String, RemoteQuoteFetchResult> = coroutineScope {
        assets.map { asset ->
            async { asset.code to fetchQuote(asset) }
        }.awaitAll().toMap()
    }

    companion object {
        private val OLINDA_DATE_FORMATTER = DateTimeFormatter.ofPattern("MM-dd-yyyy", Locale.US)
    }
}

class CoinGeckoCryptoDataSource(
    private val transport: MarketHttpTransport,
    private val clock: () -> Long = DateTimeUtils::nowEpochMillis
) : MarketQuoteRemoteDataSource {

    override fun supports(asset: MarketAssetCatalog.AssetDescriptor): Boolean =
        asset.code in COIN_IDS_BY_CODE

    override suspend fun fetchQuote(asset: MarketAssetCatalog.AssetDescriptor): RemoteQuoteFetchResult {
        if (!supports(asset)) {
            return RemoteQuoteFetchResult.Failure(
                reason = FailureReason.UNSUPPORTED_ASSET,
                detail = "Ativo não suportado pela CoinGecko: ${asset.code}"
            )
        }
        return fetchQuotes(listOf(asset))[asset.code] ?: RemoteQuoteFetchResult.Failure(
            reason = FailureReason.INVALID_RESPONSE,
            detail = "Resposta vazia da CoinGecko"
        )
    }

    override suspend fun fetchQuotes(
        assets: List<MarketAssetCatalog.AssetDescriptor>
    ): Map<String, RemoteQuoteFetchResult> {
        val supported = assets.filter { supports(it) }
        val unsupported = assets.filterNot { supports(it) }.associate { asset ->
            asset.code to RemoteQuoteFetchResult.Failure(
                reason = FailureReason.UNSUPPORTED_ASSET,
                detail = "Ativo não suportado pela CoinGecko: ${asset.code}"
            )
        }
        if (supported.isEmpty()) return unsupported

        val idsParam = supported.mapNotNull { COIN_IDS_BY_CODE[it.code] }.distinct().joinToString(",")
        val url = "https://api.coingecko.com/api/v3/simple/price" +
            "?ids=$idsParam&vs_currencies=brl&include_24hr_change=true&include_last_updated_at=true"

        val response = transport.get(url).getOrElse { err ->
            val failure = mapExceptionToFailure(err, "CoinGecko")
            return unsupported + supported.associate { it.code to failure }
        }
        val parsed = when (response.statusCode) {
            in 200..299 -> MarketQuoteParsers.parseCoinGeckoPrices(response.body, supported, clock())
            429 -> {
                val failure = RemoteQuoteFetchResult.Failure(
                    reason = FailureReason.RATE_LIMITED,
                    httpStatusCode = 429,
                    detail = "Limite temporário de requisições da CoinGecko atingido"
                )
                supported.associate { it.code to failure }
            }
            else -> {
                val failure = RemoteQuoteFetchResult.Failure(
                    reason = FailureReason.HTTP_ERROR,
                    httpStatusCode = response.statusCode,
                    detail = "Erro HTTP ${response.statusCode} ao consultar CoinGecko"
                )
                supported.associate { it.code to failure }
            }
        }
        return unsupported + parsed
    }

    companion object {
        private val COIN_IDS_BY_CODE = mapOf(
            MarketAssetCatalog.BTC_BRL_CODE to "bitcoin",
            MarketAssetCatalog.ETH_BRL_CODE to "ethereum",
            MarketAssetCatalog.SOL_BRL_CODE to "solana"
        )
    }
}

class CoinPaprikaCryptoDataSource(
    private val transport: MarketHttpTransport,
    private val clock: () -> Long = DateTimeUtils::nowEpochMillis
) : MarketQuoteRemoteDataSource {

    override fun supports(asset: MarketAssetCatalog.AssetDescriptor): Boolean =
        asset.code in PAPRIKA_IDS_BY_CODE

    override suspend fun fetchQuote(asset: MarketAssetCatalog.AssetDescriptor): RemoteQuoteFetchResult {
        val tickerId = PAPRIKA_IDS_BY_CODE[asset.code]
            ?: return RemoteQuoteFetchResult.Failure(
                reason = FailureReason.UNSUPPORTED_ASSET,
                detail = "Ativo não suportado pela CoinPaprika: ${asset.code}"
            )
        val url = "https://api.coinpaprika.com/v1/tickers/$tickerId?quotes=BRL"
        val response = transport.get(url).getOrElse { err ->
            return mapExceptionToFailure(err, "CoinPaprika")
        }
        return when (response.statusCode) {
            in 200..299 -> MarketQuoteParsers.parseCoinPaprikaTicker(response.body, asset, clock())
            429 -> RemoteQuoteFetchResult.Failure(
                reason = FailureReason.RATE_LIMITED,
                httpStatusCode = 429,
                detail = "Limite temporário de requisições da CoinPaprika atingido"
            )
            else -> RemoteQuoteFetchResult.Failure(
                reason = FailureReason.HTTP_ERROR,
                httpStatusCode = response.statusCode,
                detail = "Erro HTTP ${response.statusCode} ao consultar CoinPaprika"
            )
        }
    }

    override suspend fun fetchQuotes(
        assets: List<MarketAssetCatalog.AssetDescriptor>
    ): Map<String, RemoteQuoteFetchResult> = coroutineScope {
        assets.map { asset ->
            async { asset.code to fetchQuote(asset) }
        }.awaitAll().toMap()
    }

    companion object {
        private val PAPRIKA_IDS_BY_CODE = mapOf(
            MarketAssetCatalog.BTC_BRL_CODE to "btc-bitcoin",
            MarketAssetCatalog.ETH_BRL_CODE to "eth-ethereum",
            MarketAssetCatalog.SOL_BRL_CODE to "sol-solana"
        )
    }
}

/**
 * Orquestra múltiplas fontes públicas sem exigir chave paga.
 * Mantém a modalidade fiel à fonte que respondeu com sucesso e isola falhas por ativo.
 */
class CompositeMarketQuoteRemoteDataSource(
    private val providers: List<MarketQuoteRemoteDataSource>
) : MarketQuoteRemoteDataSource {

    override fun supports(asset: MarketAssetCatalog.AssetDescriptor): Boolean =
        providers.any { it.supports(asset) }

    override suspend fun fetchQuote(asset: MarketAssetCatalog.AssetDescriptor): RemoteQuoteFetchResult {
        var lastFailure: RemoteQuoteFetchResult.Failure? = null
        for (provider in providers) {
            if (!provider.supports(asset)) continue
            when (val result = provider.fetchQuote(asset)) {
                is RemoteQuoteFetchResult.Success -> return result
                is RemoteQuoteFetchResult.Failure -> lastFailure = result
            }
        }
        return lastFailure ?: RemoteQuoteFetchResult.Failure(
            reason = FailureReason.UNSUPPORTED_ASSET,
            detail = "Nenhuma fonte configurada para ${asset.code}"
        )
    }

    override suspend fun fetchQuotes(
        assets: List<MarketAssetCatalog.AssetDescriptor>
    ): Map<String, RemoteQuoteFetchResult> {
        val resolved = mutableMapOf<String, RemoteQuoteFetchResult>()
        val pending = assets.toMutableList()

        for (provider in providers) {
            if (pending.isEmpty()) break
            val supportedSubset = pending.filter { provider.supports(it) }
            if (supportedSubset.isEmpty()) continue

            val batchResult = provider.fetchQuotes(supportedSubset)
            for (asset in supportedSubset) {
                val res = batchResult[asset.code] ?: continue
                if (res is RemoteQuoteFetchResult.Success) {
                    resolved[asset.code] = res
                    pending.removeAll { it.code == asset.code }
                } else {
                    resolved[asset.code] = res
                }
            }
        }

        for (asset in pending) {
            if (asset.code !in resolved) {
                resolved[asset.code] = RemoteQuoteFetchResult.Failure(
                    reason = FailureReason.UNSUPPORTED_ASSET,
                    detail = "Nenhuma fonte configurada para ${asset.code}"
                )
            }
        }

        return resolved
    }
}

/**
 * Roteia categorias distintas (Moedas, Criptomoedas) para suas respectivas fontes especializadas
 * sem depender de uma unica API para todo o mercado.
 */
class CategoryRoutedMarketQuoteRemoteDataSource(
    private val routes: Map<AssetCategory, MarketQuoteRemoteDataSource>
) : MarketQuoteRemoteDataSource {

    override fun supports(asset: MarketAssetCatalog.AssetDescriptor): Boolean =
        routes[asset.category]?.supports(asset) == true

    override suspend fun fetchQuote(asset: MarketAssetCatalog.AssetDescriptor): RemoteQuoteFetchResult {
        val route = routes[asset.category]
            ?: return RemoteQuoteFetchResult.Failure(
                reason = FailureReason.UNSUPPORTED_ASSET,
                detail = "Categoria sem fonte configurada: ${asset.category.displayLabel}"
            )
        return route.fetchQuote(asset)
    }

    override suspend fun fetchQuotes(
        assets: List<MarketAssetCatalog.AssetDescriptor>
    ): Map<String, RemoteQuoteFetchResult> = coroutineScope {
        val grouped = assets.groupBy { it.category }
        val deferred = grouped.map { (category, categoryAssets) ->
            async {
                val route = routes[category]
                if (route == null) {
                    categoryAssets.associate { asset ->
                        asset.code to RemoteQuoteFetchResult.Failure(
                            reason = FailureReason.UNSUPPORTED_ASSET,
                            detail = "Categoria sem fonte configurada: ${category.displayLabel}"
                        )
                    }
                } else {
                    route.fetchQuotes(categoryAssets)
                }
            }
        }
        buildMap {
            deferred.awaitAll().forEach { putAll(it) }
        }
    }
}

/**
 * Fonte oficial pública da B3 (/mds/api/v1/instrumentQuotation/{symbol}) para IBOV e ações B3 (fallback).
 */
class B3QuoteDataSource(
    private val transport: MarketHttpTransport,
    private val clock: () -> Long = DateTimeUtils::nowEpochMillis,
    private val supportedCodes: Set<String> = DEFAULT_SUPPORTED_CODES
) : MarketQuoteRemoteDataSource {

    override fun supports(asset: MarketAssetCatalog.AssetDescriptor): Boolean =
        asset.code in supportedCodes && asset.code in SYMBOL_PATH_BY_CODE

    override suspend fun fetchQuote(asset: MarketAssetCatalog.AssetDescriptor): RemoteQuoteFetchResult {
        val symbolPath = SYMBOL_PATH_BY_CODE[asset.code]
        if (symbolPath == null || !supports(asset)) {
            return RemoteQuoteFetchResult.Failure(
                reason = FailureReason.UNSUPPORTED_ASSET,
                detail = "Ativo não suportado pela fonte pública B3: ${asset.code}"
            )
        }
        val url = "https://cotacao.b3.com.br/mds/api/v1/instrumentQuotation/$symbolPath"
        val response = transport.get(url).getOrElse { err ->
            return mapExceptionToFailure(err, "B3")
        }
        return when (response.statusCode) {
            in 200..299 -> MarketQuoteParsers.parseB3InstrumentQuotation(response.body, asset, clock())
            429 -> RemoteQuoteFetchResult.Failure(
                reason = FailureReason.RATE_LIMITED,
                httpStatusCode = 429,
                detail = "Limite temporário de requisições da B3 atingido"
            )
            else -> RemoteQuoteFetchResult.Failure(
                reason = FailureReason.HTTP_ERROR,
                httpStatusCode = response.statusCode,
                detail = "Erro HTTP ${response.statusCode} ao consultar B3"
            )
        }
    }

    override suspend fun fetchQuotes(
        assets: List<MarketAssetCatalog.AssetDescriptor>
    ): Map<String, RemoteQuoteFetchResult> = coroutineScope {
        assets.map { asset ->
            async { asset.code to fetchQuote(asset) }
        }.awaitAll().toMap()
    }

    companion object {
        private val SYMBOL_PATH_BY_CODE = mapOf(
            MarketAssetCatalog.IBOV_CODE to "ibov",
            MarketAssetCatalog.PETR4_CODE to "PETR4",
            MarketAssetCatalog.VALE3_CODE to "VALE3",
            MarketAssetCatalog.ITUB4_CODE to "ITUB4"
        )
        val DEFAULT_SUPPORTED_CODES: Set<String> = SYMBOL_PATH_BY_CODE.keys
    }
}

/**
 * Fonte gratuita sem chave para ações brasileiras liberadas na brapi.dev (PETR4, VALE3, ITUB4) em lote único.
 */
class BrapiB3StockDataSource(
    private val transport: MarketHttpTransport,
    private val clock: () -> Long = DateTimeUtils::nowEpochMillis
) : MarketQuoteRemoteDataSource {

    override fun supports(asset: MarketAssetCatalog.AssetDescriptor): Boolean =
        asset.category == AssetCategory.STOCK && asset.code in KEYLESS_B3_STOCKS

    override suspend fun fetchQuote(asset: MarketAssetCatalog.AssetDescriptor): RemoteQuoteFetchResult {
        if (!supports(asset)) {
            return RemoteQuoteFetchResult.Failure(
                reason = FailureReason.UNSUPPORTED_ASSET,
                detail = "Ativo exige autenticação na brapi.dev ou não pertence à lista gratuita: ${asset.code}"
            )
        }
        return fetchQuotes(listOf(asset))[asset.code] ?: RemoteQuoteFetchResult.Failure(
            reason = FailureReason.INVALID_RESPONSE,
            detail = "Resposta vazia da brapi.dev"
        )
    }

    override suspend fun fetchQuotes(
        assets: List<MarketAssetCatalog.AssetDescriptor>
    ): Map<String, RemoteQuoteFetchResult> {
        val supported = assets.filter { supports(it) }
        val unsupported = assets.filterNot { supports(it) }.associate { asset ->
            asset.code to RemoteQuoteFetchResult.Failure(
                reason = FailureReason.UNSUPPORTED_ASSET,
                detail = "Ativo não disponível sem chave na brapi.dev: ${asset.code}"
            )
        }
        if (supported.isEmpty()) return unsupported

        val symbolsParam = supported.joinToString(",") { it.ticker }
        val url = "https://brapi.dev/api/quote/$symbolsParam"
        val response = transport.get(url).getOrElse { err ->
            val failure = mapExceptionToFailure(err, "brapi.dev")
            return unsupported + supported.associate { it.code to failure }
        }
        val parsed = when (response.statusCode) {
            in 200..299 -> MarketQuoteParsers.parseBrapiBatch(response.body, supported, clock())
            429 -> {
                val failure = RemoteQuoteFetchResult.Failure(
                    reason = FailureReason.RATE_LIMITED,
                    httpStatusCode = 429,
                    detail = "Limite temporário de requisições da brapi.dev atingido"
                )
                supported.associate { it.code to failure }
            }
            else -> {
                val failure = RemoteQuoteFetchResult.Failure(
                    reason = FailureReason.HTTP_ERROR,
                    httpStatusCode = response.statusCode,
                    detail = "Erro HTTP ${response.statusCode} ao consultar brapi.dev"
                )
                supported.associate { it.code to failure }
            }
        }
        return unsupported + parsed
    }

    companion object {
        private val KEYLESS_B3_STOCKS = setOf(
            MarketAssetCatalog.PETR4_CODE,
            MarketAssetCatalog.VALE3_CODE,
            MarketAssetCatalog.ITUB4_CODE
        )
    }
}

/**
 * Fonte pública gratuita em lote para índices globais e ações dos EUA (SPX, IXIC, MELI e fallback IBOV).
 */
class CnbcMarketDataSource(
    private val transport: MarketHttpTransport,
    private val clock: () -> Long = DateTimeUtils::nowEpochMillis,
    private val supportedCodes: Set<String> = DEFAULT_SUPPORTED_CODES
) : MarketQuoteRemoteDataSource {

    override fun supports(asset: MarketAssetCatalog.AssetDescriptor): Boolean =
        asset.code in supportedCodes && asset.code in CNBC_SYMBOL_BY_CODE

    override suspend fun fetchQuote(asset: MarketAssetCatalog.AssetDescriptor): RemoteQuoteFetchResult {
        if (!supports(asset)) {
            return RemoteQuoteFetchResult.Failure(
                reason = FailureReason.UNSUPPORTED_ASSET,
                detail = "Ativo não habilitado na fonte CNBC: ${asset.code}"
            )
        }
        return fetchQuotes(listOf(asset))[asset.code] ?: RemoteQuoteFetchResult.Failure(
            reason = FailureReason.INVALID_RESPONSE,
            detail = "Resposta vazia da CNBC"
        )
    }

    override suspend fun fetchQuotes(
        assets: List<MarketAssetCatalog.AssetDescriptor>
    ): Map<String, RemoteQuoteFetchResult> {
        val supported = assets.filter { supports(it) }
        val unsupported = assets.filterNot { supports(it) }.associate { asset ->
            asset.code to RemoteQuoteFetchResult.Failure(
                reason = FailureReason.UNSUPPORTED_ASSET,
                detail = "Ativo não habilitado na fonte CNBC: ${asset.code}"
            )
        }
        if (supported.isEmpty()) return unsupported

        val symbolsParam = supported.mapNotNull { CNBC_SYMBOL_BY_CODE[it.code] }.joinToString("%7C")
        val url = "https://quote.cnbc.com/quote-html-webservice/restQuote/symbolType/symbol" +
            "?symbols=$symbolsParam&requestMethod=itv&noform=1&partnerId=2&fund=1&exthrs=1&output=json&events=1"
        val headers = mapOf(
            "User-Agent" to BROWSER_USER_AGENT,
            "Accept" to "application/json, text/plain, */*"
        )
        val response = transport.get(url, headers = headers).getOrElse { err ->
            val failure = mapExceptionToFailure(err, "CNBC Market")
            return unsupported + supported.associate { it.code to failure }
        }
        val parsed = when (response.statusCode) {
            in 200..299 -> MarketQuoteParsers.parseCnbcBatch(
                jsonBody = response.body,
                assets = supported,
                symbolForAsset = { CNBC_SYMBOL_BY_CODE[it.code] },
                fetchedAtEpochMs = clock()
            )
            429 -> {
                val failure = RemoteQuoteFetchResult.Failure(
                    reason = FailureReason.RATE_LIMITED,
                    httpStatusCode = 429,
                    detail = "Limite temporário de requisições da CNBC atingido"
                )
                supported.associate { it.code to failure }
            }
            else -> {
                val failure = RemoteQuoteFetchResult.Failure(
                    reason = FailureReason.HTTP_ERROR,
                    httpStatusCode = response.statusCode,
                    detail = "Erro HTTP ${response.statusCode} ao consultar CNBC Market"
                )
                supported.associate { it.code to failure }
            }
        }
        return unsupported + parsed
    }

    companion object {
        private const val BROWSER_USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"
        private val CNBC_SYMBOL_BY_CODE = mapOf(
            MarketAssetCatalog.SPX_CODE to ".SPX",
            MarketAssetCatalog.IXIC_CODE to ".IXIC",
            MarketAssetCatalog.MELI_CODE to "MELI",
            MarketAssetCatalog.IBOV_CODE to ".BVSP"
        )
        val DEFAULT_SUPPORTED_CODES: Set<String> = CNBC_SYMBOL_BY_CODE.keys
    }
}

/**
 * Fonte pública oficial da Nasdaq usada como fallback para IXIC (COMP) e MELI.
 */
class NasdaqPublicDataSource(
    private val transport: MarketHttpTransport,
    private val clock: () -> Long = DateTimeUtils::nowEpochMillis
) : MarketQuoteRemoteDataSource {

    override fun supports(asset: MarketAssetCatalog.AssetDescriptor): Boolean =
        asset.code in TARGET_BY_CODE

    override suspend fun fetchQuote(asset: MarketAssetCatalog.AssetDescriptor): RemoteQuoteFetchResult {
        val target = TARGET_BY_CODE[asset.code]
            ?: return RemoteQuoteFetchResult.Failure(
                reason = FailureReason.UNSUPPORTED_ASSET,
                detail = "Ativo não suportado pela API pública da Nasdaq: ${asset.code}"
            )
        val url = "https://api.nasdaq.com/api/quote/${target.first}/info?assetclass=${target.second}"
        val headers = mapOf(
            "User-Agent" to BROWSER_USER_AGENT,
            "Accept" to "application/json, text/plain, */*",
            "Origin" to "https://www.nasdaq.com",
            "Referer" to "https://www.nasdaq.com/"
        )
        val response = transport.get(url, headers = headers).getOrElse { err ->
            return mapExceptionToFailure(err, "Nasdaq")
        }
        return when (response.statusCode) {
            in 200..299 -> MarketQuoteParsers.parseNasdaqInfo(response.body, asset, clock())
            429 -> RemoteQuoteFetchResult.Failure(
                reason = FailureReason.RATE_LIMITED,
                httpStatusCode = 429,
                detail = "Limite temporário de requisições da Nasdaq atingido"
            )
            else -> RemoteQuoteFetchResult.Failure(
                reason = FailureReason.HTTP_ERROR,
                httpStatusCode = response.statusCode,
                detail = "Erro HTTP ${response.statusCode} ao consultar Nasdaq"
            )
        }
    }

    override suspend fun fetchQuotes(
        assets: List<MarketAssetCatalog.AssetDescriptor>
    ): Map<String, RemoteQuoteFetchResult> = coroutineScope {
        assets.map { asset ->
            async { asset.code to fetchQuote(asset) }
        }.awaitAll().toMap()
    }

    companion object {
        private const val BROWSER_USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"
        private val TARGET_BY_CODE = mapOf(
            MarketAssetCatalog.IXIC_CODE to ("COMP" to "index"),
            MarketAssetCatalog.MELI_CODE to ("MELI" to "stocks")
        )
    }
}

private fun mapExceptionToFailure(error: Throwable, sourceLabel: String): RemoteQuoteFetchResult.Failure {
    return when (error) {
        is SocketTimeoutException -> RemoteQuoteFetchResult.Failure(
            reason = FailureReason.TIMEOUT,
            detail = "Tempo limite excedido ao consultar $sourceLabel"
        )
        is IOException -> RemoteQuoteFetchResult.Failure(
            reason = FailureReason.NETWORK_UNAVAILABLE,
            detail = "Falha de rede ao consultar $sourceLabel"
        )
        else -> RemoteQuoteFetchResult.Failure(
            reason = FailureReason.INVALID_RESPONSE,
            detail = "Erro inesperado ao consultar $sourceLabel"
        )
    }
}
