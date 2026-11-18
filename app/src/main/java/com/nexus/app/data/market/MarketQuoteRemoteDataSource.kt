package com.nexus.app.data.market

import com.nexus.app.core.DateTimeUtils
import com.nexus.app.domain.model.AssetCategory
import com.nexus.app.domain.model.MarketAssetCatalog
import com.nexus.app.domain.model.MarketQuoteCalculations
import com.nexus.app.domain.model.MarketQuoteSnapshot
import com.nexus.app.domain.model.QuoteModality
import kotlinx.coroutines.Dispatchers
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
import java.time.LocalDate
import java.time.LocalDateTime
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
    suspend fun fetchQuote(asset: MarketAssetCatalog.AssetDescriptor): RemoteQuoteFetchResult
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

    /**
     * Analisa a resposta da AwesomeAPI para USD-BRL (Dolar Comercial).
     * Exemplo: {"USDBRL":{"bid":"5.772","ask":"5.774","pctChange":"0.85","timestamp":"1794960000",...}}
     */
    fun parseAwesomeApiUsdBrl(
        jsonBody: String,
        fetchedAtEpochMs: Long
    ): RemoteQuoteFetchResult {
        val root = runCatching { json.parseToJsonElement(jsonBody) }.getOrNull() as? JsonObject
            ?: return RemoteQuoteFetchResult.Failure(
                reason = FailureReason.INVALID_RESPONSE,
                detail = "Resposta JSON inválida da AwesomeAPI"
            )

        val pairObject = (root["USDBRL"] as? JsonObject)
            ?: (root["USD-BRL"] as? JsonObject)
            ?: return RemoteQuoteFetchResult.Failure(
                reason = FailureReason.INVALID_RESPONSE,
                detail = "Par USDBRL ausente na resposta da AwesomeAPI"
            )

        val bid = parseDoubleField(pairObject, "bid")
        if (!MarketQuoteCalculations.isValidPositiveRate(bid)) {
            return RemoteQuoteFetchResult.Failure(
                reason = FailureReason.INVALID_RESPONSE,
                detail = "Cotação de compra inválida na AwesomeAPI"
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
                assetCode = MarketAssetCatalog.USD_BRL_CODE,
                category = AssetCategory.CURRENCY,
                baseCurrency = "USD",
                quoteCurrency = "BRL",
                modality = QuoteModality.COMMERCIAL,
                price = validBid,
                bidPrice = validBid,
                askPrice = ask,
                previousClose = previousClose,
                changePercent = pctChange,
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

    /**
     * Analisa a resposta OData da API Olinda do Banco Central do Brasil (Dolar PTAX).
     * Modality explicitamente marcada como QuoteModality.PTAX.
     */
    fun parseBcbOlindaPtaxUsdBrl(
        jsonBody: String,
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
                detail = "Nenhuma cotação PTAX válida encontrada no período"
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
                assetCode = MarketAssetCatalog.USD_BRL_CODE,
                category = AssetCategory.CURRENCY,
                baseCurrency = "USD",
                quoteCurrency = "BRL",
                modality = QuoteModality.PTAX,
                price = latest.sell,
                bidPrice = latest.buy,
                askPrice = latest.sell,
                previousClose = previousDay?.sell,
                changePercent = changePercent,
                trend = MarketQuoteCalculations.computeTrend(changePercent),
                sourceName = "Banco Central (PTAX)",
                sourceTimestampEpochMs = latest.timestampEpochMs,
                fetchedAtEpochMs = fetchedAtEpochMs
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

    override suspend fun fetchQuote(asset: MarketAssetCatalog.AssetDescriptor): RemoteQuoteFetchResult {
        if (asset.code != MarketAssetCatalog.USD_BRL_CODE) {
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

class BcbOlindaPtaxDataSource(
    private val transport: MarketHttpTransport,
    private val clock: () -> Long = DateTimeUtils::nowEpochMillis,
    private val todayProvider: () -> LocalDate = DateTimeUtils::todayLocalDate
) : MarketQuoteRemoteDataSource {

    override suspend fun fetchQuote(asset: MarketAssetCatalog.AssetDescriptor): RemoteQuoteFetchResult {
        if (asset.code != MarketAssetCatalog.USD_BRL_CODE) {
            return RemoteQuoteFetchResult.Failure(
                reason = FailureReason.UNSUPPORTED_ASSET,
                detail = "Ativo não suportado pela API PTAX do BCB: ${asset.code}"
            )
        }
        val endDate = todayProvider()
        val startDate = endDate.minusDays(14)
        val startParam = startDate.format(OLINDA_DATE_FORMATTER)
        val endParam = endDate.format(OLINDA_DATE_FORMATTER)
        val url = "https://olinda.bcb.gov.br/olinda/servico/PTAX/versao/v1/odata/" +
            "CotacaoDolarPeriodo(dataInicial=@dataInicial,dataFinalCotacao=@dataFinalCotacao)" +
            "?@dataInicial='$startParam'&@dataFinalCotacao='$endParam'" +
            "&\$top=10&\$orderby=dataHoraCotacao%20desc&\$format=json"

        val response = transport.get(url).getOrElse { err ->
            return mapExceptionToFailure(err, "Banco Central (PTAX)")
        }
        return when (response.statusCode) {
            in 200..299 -> MarketQuoteParsers.parseBcbOlindaPtaxUsdBrl(response.body, clock())
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

    companion object {
        private val OLINDA_DATE_FORMATTER = DateTimeFormatter.ofPattern("MM-dd-yyyy", Locale.US)
    }
}

/**
 * Orquestra múltiplas fontes públicas sem exigir chave paga.
 * Mantém a modalidade fiel à fonte que respondeu com sucesso.
 */
class CompositeMarketQuoteRemoteDataSource(
    private val providers: List<MarketQuoteRemoteDataSource>
) : MarketQuoteRemoteDataSource {

    override suspend fun fetchQuote(asset: MarketAssetCatalog.AssetDescriptor): RemoteQuoteFetchResult {
        var lastFailure: RemoteQuoteFetchResult.Failure? = null
        for (provider in providers) {
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
