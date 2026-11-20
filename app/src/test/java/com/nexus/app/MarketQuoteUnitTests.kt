package com.nexus.app

import com.nexus.app.data.market.AwesomeApiCurrencyDataSource
import com.nexus.app.data.market.AwesomeApiUsdBrlDataSource
import com.nexus.app.data.market.B3QuoteDataSource
import com.nexus.app.data.market.BcbOlindaPtaxDataSource
import com.nexus.app.data.market.BrapiB3StockDataSource
import com.nexus.app.data.market.CategoryRoutedMarketQuoteRemoteDataSource
import com.nexus.app.data.market.CnbcMarketDataSource
import com.nexus.app.data.market.CoinGeckoCryptoDataSource
import com.nexus.app.data.market.CoinPaprikaCryptoDataSource
import com.nexus.app.data.market.CompositeMarketQuoteRemoteDataSource
import com.nexus.app.data.market.FailureReason
import com.nexus.app.data.market.HttpResponsePayload
import com.nexus.app.data.market.MarketHttpTransport
import com.nexus.app.data.market.MarketQuoteParsers
import com.nexus.app.data.market.MarketQuoteRemoteDataSource
import com.nexus.app.data.market.MarketQuoteRepository
import com.nexus.app.data.market.MarketQuoteStore
import com.nexus.app.data.market.NasdaqPublicDataSource
import com.nexus.app.data.market.PersistedMarketQuoteRecord
import com.nexus.app.data.market.RemoteQuoteFetchResult
import com.nexus.app.domain.model.AssetCategory
import com.nexus.app.domain.model.MarketAssetCatalog
import com.nexus.app.domain.model.MarketQuoteCalculations
import com.nexus.app.domain.model.MarketQuoteSnapshot
import com.nexus.app.domain.model.MarketQuoteUiState
import com.nexus.app.domain.model.QuoteModality
import com.nexus.app.domain.model.QuoteTrend
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class MarketQuoteUnitTests {

    private class InMemoryMarketQuoteStore(
        initialRecord: PersistedMarketQuoteRecord? = null
    ) : MarketQuoteStore {
        private val records = mutableMapOf<String, PersistedMarketQuoteRecord>()

        init {
            if (initialRecord != null) {
                records[MarketAssetCatalog.USD_BRL_CODE] = initialRecord
            }
        }

        override fun loadRecord(assetCode: String): PersistedMarketQuoteRecord? = records[assetCode]

        override fun saveValidQuote(quote: MarketQuoteSnapshot) {
            records[quote.assetCode] = PersistedMarketQuoteRecord(
                lastValidQuote = quote,
                lastAttemptAtEpochMs = quote.fetchedAtEpochMs,
                lastAttemptError = null
            )
        }

        override fun recordFailedAttempt(assetCode: String, attemptAtEpochMs: Long, errorMessage: String) {
            val existing = records[assetCode]
            records[assetCode] = PersistedMarketQuoteRecord(
                lastValidQuote = existing?.lastValidQuote,
                lastAttemptAtEpochMs = attemptAtEpochMs,
                lastAttemptError = errorMessage
            )
        }
    }

    @Test
    fun `parseAwesomeApiUsdBrl parses commercial dollar with positive variation cleanly`() {
        val json = """
            {
              "USDBRL": {
                "code": "USD",
                "codein": "BRL",
                "bid": "5.7720",
                "ask": "5.7740",
                "pctChange": "0.85",
                "timestamp": "1763400000",
                "create_date": "2025-11-17 14:20:00"
              }
            }
        """.trimIndent()

        val result = MarketQuoteParsers.parseAwesomeApiUsdBrl(json, fetchedAtEpochMs = 1_763_400_500_000L)
        assertTrue(result is RemoteQuoteFetchResult.Success)
        val quote = (result as RemoteQuoteFetchResult.Success).quote

        assertEquals(MarketAssetCatalog.USD_BRL_CODE, quote.assetCode)
        assertEquals(QuoteModality.COMMERCIAL, quote.modality)
        assertEquals(5.7720, quote.price, 0.0001)
        assertEquals(5.7720, quote.bidPrice ?: 0.0, 0.0001)
        assertEquals(5.7740, quote.askPrice ?: 0.0, 0.0001)
        assertEquals(0.85, quote.changePercent ?: 0.0, 0.0001)
        assertEquals(QuoteTrend.UP, quote.trend)
        assertEquals("AwesomeAPI", quote.sourceName)
        assertEquals(1_763_400_000_000L, quote.sourceTimestampEpochMs)
    }

    @Test
    fun `parseAwesomeApiUsdBrl rejects non-positive or malformed quotes`() {
        val zeroBidJson = """{"USDBRL":{"bid":"0","ask":"5.70","pctChange":"0.10"}}"""
        val negativeBidJson = """{"USDBRL":{"bid":"-5.10"}}"""
        val invalidJson = """{"error":"bad"}"""

        assertTrue(MarketQuoteParsers.parseAwesomeApiUsdBrl(zeroBidJson, 1000L) is RemoteQuoteFetchResult.Failure)
        assertTrue(MarketQuoteParsers.parseAwesomeApiUsdBrl(negativeBidJson, 1000L) is RemoteQuoteFetchResult.Failure)
        assertTrue(MarketQuoteParsers.parseAwesomeApiUsdBrl(invalidJson, 1000L) is RemoteQuoteFetchResult.Failure)
    }

    @Test
    fun `parseBcbOlindaPtaxUsdBrl extracts PTAX modality and daily variation against previous business day`() {
        val bcbJson = """
            {
              "value": [
                {"cotacaoCompra": 5.73180, "cotacaoVenda": 5.73240, "dataHoraCotacao": "2026-11-16 13:06:32.509132"},
                {"cotacaoCompra": 5.78450, "cotacaoVenda": 5.78510, "dataHoraCotacao": "2026-11-17 13:07:26.01516"}
              ]
            }
        """.trimIndent()

        val result = MarketQuoteParsers.parseBcbOlindaPtaxUsdBrl(bcbJson, fetchedAtEpochMs = 1_794_920_000_000L)
        assertTrue(result is RemoteQuoteFetchResult.Success)
        val quote = (result as RemoteQuoteFetchResult.Success).quote

        assertEquals(QuoteModality.PTAX, quote.modality)
        assertEquals("Banco Central (PTAX)", quote.sourceName)
        assertEquals(5.7851, quote.price, 0.0001)
        assertEquals(5.7845, quote.bidPrice ?: 0.0, 0.0001)
        assertEquals(5.7324, quote.previousClose ?: 0.0, 0.0001)
        assertEquals(QuoteTrend.UP, quote.trend)
        val expectedPct = ((5.78510 - 5.73240) / 5.73240) * 100.0
        assertEquals(expectedPct, quote.changePercent ?: 0.0, 0.0001)
    }

    @Test
    fun `parseBcbOlindaPtaxUsdBrl omits variation when only a single business day exists`() {
        val singleDayJson = """
            {
              "value": [
                {"cotacaoCompra": 5.78450, "cotacaoVenda": 5.78510, "dataHoraCotacao": "2026-11-17 13:07:26.01516"}
              ]
            }
        """.trimIndent()

        val result = MarketQuoteParsers.parseBcbOlindaPtaxUsdBrl(singleDayJson, fetchedAtEpochMs = 1_794_920_000_000L)
        assertTrue(result is RemoteQuoteFetchResult.Success)
        val quote = (result as RemoteQuoteFetchResult.Success).quote

        assertNull(quote.changePercent)
        assertEquals(QuoteTrend.UNKNOWN, quote.trend)
    }

    @Test
    fun `composite remote source falls back from AwesomeAPI 429 to BCB PTAX without mislabeling modality`() = runBlocking {
        val transport = object : MarketHttpTransport {
            override suspend fun get(url: String, headers: Map<String, String>): Result<HttpResponsePayload> {
                return if (url.contains("awesomeapi")) {
                    Result.success(HttpResponsePayload(429, """{"status":429,"code":"QuotaExceeded"}"""))
                } else {
                    Result.success(
                        HttpResponsePayload(
                            200,
                            """{"value":[{"cotacaoCompra":5.7845,"cotacaoVenda":5.7851,"dataHoraCotacao":"2026-11-17 13:07:26.015"},{"cotacaoCompra":5.8100,"cotacaoVenda":5.8106,"dataHoraCotacao":"2026-11-16 13:06:00.0"}]}"""
                        )
                    )
                }
            }
        }

        val composite = CompositeMarketQuoteRemoteDataSource(
            providers = listOf(
                AwesomeApiUsdBrlDataSource(transport, clock = { 10_000L }),
                BcbOlindaPtaxDataSource(
                    transport = transport,
                    clock = { 10_000L },
                    todayProvider = { LocalDate.of(2026, 11, 17) }
                )
            )
        )

        val res = composite.fetchQuote(MarketAssetCatalog.USD_BRL)
        assertTrue(res is RemoteQuoteFetchResult.Success)
        val quote = (res as RemoteQuoteFetchResult.Success).quote
        assertEquals(QuoteModality.PTAX, quote.modality)
        assertEquals(QuoteTrend.DOWN, quote.trend)
    }

    @Test
    fun `repository preserves valid cached quote when refresh fails or device is offline`() = runBlocking {
        val cachedQuote = MarketQuoteSnapshot(
            assetCode = MarketAssetCatalog.USD_BRL_CODE,
            modality = QuoteModality.COMMERCIAL,
            price = 5.7500,
            bidPrice = 5.7500,
            askPrice = 5.7520,
            previousClose = 5.7000,
            changePercent = 0.88,
            trend = QuoteTrend.UP,
            sourceName = "AwesomeAPI",
            sourceTimestampEpochMs = 1_000_000L,
            fetchedAtEpochMs = 1_000_000L
        )
        val store = InMemoryMarketQuoteStore(PersistedMarketQuoteRecord(lastValidQuote = cachedQuote))
        var nowMs = 1_000_000L + MarketQuoteCalculations.DEFAULT_STALE_TTL_MS + 10_000L
        var networkOnline = true

        val failingRemote = object : com.nexus.app.data.market.MarketQuoteRemoteDataSource {
            var calls = 0
            override suspend fun fetchQuote(asset: MarketAssetCatalog.AssetDescriptor): RemoteQuoteFetchResult {
                calls++
                return RemoteQuoteFetchResult.Failure(
                    reason = FailureReason.TIMEOUT,
                    detail = "Timeout simulado"
                )
            }
        }

        val repo = MarketQuoteRepository(
            remoteDataSource = failingRemote,
            store = store,
            isNetworkAvailable = { networkOnline },
            clock = { nowMs }
        )

        val failedState = repo.refreshUsdBrl(force = true)
        assertEquals(1, failingRemote.calls)
        assertTrue(failedState.lastRefreshFailed)
        assertNotNull(failedState.quote)
        assertEquals(5.7500, failedState.quote!!.price, 0.0001)
        assertEquals(5.7500, store.loadRecord(MarketAssetCatalog.USD_BRL_CODE)?.lastValidQuote?.price ?: 0.0, 0.0001)

        // Quando offline, nao dispara chamada de rede e mantem ultimo cache valido
        networkOnline = false
        nowMs += 120_000L
        val offlineState = repo.refreshUsdBrl(force = true)
        assertEquals(1, failingRemote.calls)
        assertTrue(offlineState.isOffline)
        assertEquals(5.7500, offlineState.quote!!.price, 0.0001)
    }

    @Test
    fun `repository respects auto-refresh cooldown unless forced manually`() = runBlocking {
        val cachedQuote = MarketQuoteSnapshot(
            assetCode = MarketAssetCatalog.USD_BRL_CODE,
            modality = QuoteModality.PTAX,
            price = 5.7000,
            changePercent = -0.20,
            trend = QuoteTrend.DOWN,
            sourceName = "Banco Central (PTAX)",
            sourceTimestampEpochMs = 10_000_000L,
            fetchedAtEpochMs = 10_000_000L
        )
        val store = InMemoryMarketQuoteStore(PersistedMarketQuoteRecord(lastValidQuote = cachedQuote))
        val nowMs = 10_000_000L + 5L * 60L * 1000L // 5 min (< 15 min cooldown)

        val countingRemote = object : com.nexus.app.data.market.MarketQuoteRemoteDataSource {
            var calls = 0
            override suspend fun fetchQuote(asset: MarketAssetCatalog.AssetDescriptor): RemoteQuoteFetchResult {
                calls++
                return RemoteQuoteFetchResult.Success(
                    cachedQuote.copy(price = 5.8000, fetchedAtEpochMs = nowMs)
                )
            }
        }

        val repo = MarketQuoteRepository(
            remoteDataSource = countingRemote,
            store = store,
            isNetworkAvailable = { true },
            clock = { nowMs }
        )

        val skippedState = repo.refreshUsdBrl(force = false)
        assertEquals(0, countingRemote.calls)
        assertEquals(5.7000, skippedState.quote!!.price, 0.0001)
        assertFalse(skippedState.isStale)

        val forcedState = repo.refreshUsdBrl(force = true)
        assertEquals(1, countingRemote.calls)
        assertEquals(5.8000, forcedState.quote!!.price, 0.0001)
    }

    @Test
    fun `formatting and staleness helpers work consistently in pt-BR`() {
        assertEquals("R$ 5,7851", MarketQuoteCalculations.formatBrlRate(5.7851))
        assertEquals("+0,85%", MarketQuoteCalculations.formatSignedPercent(0.85))
        assertEquals("-1,23%", MarketQuoteCalculations.formatSignedPercent(-1.23))
        assertEquals("0,00%", MarketQuoteCalculations.formatSignedPercent(0.001))

        val quote = MarketQuoteSnapshot(
            assetCode = MarketAssetCatalog.USD_BRL_CODE,
            modality = QuoteModality.COMMERCIAL,
            price = 5.75,
            sourceName = "AwesomeAPI",
            sourceTimestampEpochMs = 1000L,
            fetchedAtEpochMs = 1000L
        )
        assertFalse(MarketQuoteCalculations.isQuoteStale(quote, nowEpochMs = 1000L + 60_000L))
        assertTrue(
            MarketQuoteCalculations.isQuoteStale(
                quote,
                nowEpochMs = 1000L + MarketQuoteCalculations.DEFAULT_STALE_TTL_MS
            )
        )
        assertEquals("R$ 5,7851", MarketQuoteCalculations.formatPrice(5.7851, AssetCategory.CURRENCY))
        assertEquals("R$ 371.523,00", MarketQuoteCalculations.formatPrice(371523.0, AssetCategory.CRYPTO))
    }

    @Test
    fun `AwesomeAPI batch parses USD and EUR independently and labels modality accurately`() = runBlocking {
        val batchJson = """
            {
              "USDBRL": {"bid": "5.7720", "ask": "5.7740", "pctChange": "0.50", "timestamp": "1763400000"},
              "EURBRL": {"bid": "6.3510", "ask": "6.3540", "pctChange": "-0.35", "timestamp": "1763400000"}
            }
        """.trimIndent()
        val requestedUrls = mutableListOf<String>()
        val transport = object : MarketHttpTransport {
            override suspend fun get(url: String, headers: Map<String, String>): Result<HttpResponsePayload> {
                requestedUrls += url
                return Result.success(HttpResponsePayload(200, batchJson))
            }
        }
        val source = AwesomeApiCurrencyDataSource(transport, clock = { 1_763_400_500_000L })
        val results = source.fetchQuotes(MarketAssetCatalog.CURRENCY_ASSETS)

        assertEquals(1, requestedUrls.size)
        assertTrue(requestedUrls.first().contains("USD-BRL,EUR-BRL"))

        val eur = (results[MarketAssetCatalog.EUR_BRL_CODE] as RemoteQuoteFetchResult.Success).quote
        assertEquals(QuoteModality.COMMERCIAL, eur.modality)
        assertEquals("Euro Comercial", eur.modality.labelFor(eur.assetCode))
        assertEquals(6.3510, eur.price, 0.0001)
        assertEquals(QuoteTrend.DOWN, eur.trend)
        assertEquals("Dia", eur.variationPeriodLabel)
    }

    @Test
    fun `BCB Olinda PTAX supports EUR-BRL via CotacaoMoedaPeriodo and labels Euro PTAX`() = runBlocking {
        val eurPtaxJson = """
            {
              "value": [
                {"cotacaoCompra": 6.3100, "cotacaoVenda": 6.3120, "dataHoraCotacao": "2026-11-16 13:06:00.0"},
                {"cotacaoCompra": 6.3647, "cotacaoVenda": 6.3659, "dataHoraCotacao": "2026-11-17 13:07:26.015"}
              ]
            }
        """.trimIndent()
        val capturedUrls = mutableListOf<String>()
        val transport = object : MarketHttpTransport {
            override suspend fun get(url: String, headers: Map<String, String>): Result<HttpResponsePayload> {
                capturedUrls += url
                return Result.success(HttpResponsePayload(200, eurPtaxJson))
            }
        }
        val bcb = BcbOlindaPtaxDataSource(
            transport = transport,
            clock = { 10_000L },
            todayProvider = { LocalDate.of(2026, 11, 17) }
        )
        val res = bcb.fetchQuote(MarketAssetCatalog.EUR_BRL)
        assertTrue(res is RemoteQuoteFetchResult.Success)
        assertTrue(capturedUrls.first().contains("CotacaoMoedaPeriodo"))
        assertTrue(capturedUrls.first().contains("@moeda='EUR'"))

        val quote = (res as RemoteQuoteFetchResult.Success).quote
        assertEquals(QuoteModality.PTAX, quote.modality)
        assertEquals("Euro PTAX (BCB)", quote.modality.labelFor(quote.assetCode))
        assertEquals("Diária", quote.variationPeriodLabel)
        assertEquals(QuoteTrend.UP, quote.trend)
    }

    @Test
    fun `CoinGecko batch fetches BTC ETH SOL in one call and falls back per failed asset to CoinPaprika`() = runBlocking {
        val requestedUrls = mutableListOf<String>()
        val transport = object : MarketHttpTransport {
            override suspend fun get(url: String, headers: Map<String, String>): Result<HttpResponsePayload> {
                requestedUrls += url
                return when {
                    url.contains("coingecko.com") -> Result.success(
                        HttpResponsePayload(
                            200,
                            """
                            {
                              "bitcoin":{"brl":371523,"brl_24h_change":-0.96,"last_updated_at":1795003282},
                              "ethereum":{"brl":10417.86,"brl_24h_change":1.20,"last_updated_at":1795003296},
                              "solana":{"brl":0,"brl_24h_change":0}
                            }
                            """.trimIndent()
                        )
                    )
                    url.contains("coinpaprika.com/v1/tickers/sol-solana") -> Result.success(
                        HttpResponsePayload(
                            200,
                            """
                            {
                              "id":"sol-solana",
                              "last_updated":"2026-11-18T11:57:12Z",
                              "quotes":{"BRL":{"price":430.76,"percent_change_24h":1.60}}
                            }
                            """.trimIndent()
                        )
                    )
                    else -> Result.success(HttpResponsePayload(404, "{}"))
                }
            }
        }

        val cryptoRoute = CompositeMarketQuoteRemoteDataSource(
            providers = listOf(
                CoinGeckoCryptoDataSource(transport, clock = { 50_000L }),
                CoinPaprikaCryptoDataSource(transport, clock = { 50_000L })
            )
        )

        val results = cryptoRoute.fetchQuotes(MarketAssetCatalog.CRYPTO_ASSETS)
        assertEquals(2, requestedUrls.size) // 1 batch CoinGecko + 1 fallback only for SOL-BRL

        val btc = (results[MarketAssetCatalog.BTC_BRL_CODE] as RemoteQuoteFetchResult.Success).quote
        assertEquals("CoinGecko", btc.sourceName)
        assertEquals(371523.0, btc.price, 0.01)
        assertEquals(QuoteTrend.DOWN, btc.trend)
        assertEquals("24h", btc.variationPeriodLabel)

        val eth = (results[MarketAssetCatalog.ETH_BRL_CODE] as RemoteQuoteFetchResult.Success).quote
        assertEquals("CoinGecko", eth.sourceName)
        assertEquals(QuoteTrend.UP, eth.trend)

        val sol = (results[MarketAssetCatalog.SOL_BRL_CODE] as RemoteQuoteFetchResult.Success).quote
        assertEquals("CoinPaprika", sol.sourceName)
        assertEquals(430.76, sol.price, 0.01)
        assertEquals(QuoteTrend.UP, sol.trend)
        assertEquals("24h", sol.variationPeriodLabel)
    }

    @Test
    fun `repository isolates per-asset failures and skips fresh assets during batch auto-refresh`() = runBlocking {
        val nowMs = 20_000_000L
        val store = InMemoryMarketQuoteStore()
        // Pre-seed EUR with valid cached quote that is stale (>30m) and USD with fresh quote (<15m)
        store.saveValidQuote(
            MarketQuoteSnapshot(
                assetCode = MarketAssetCatalog.USD_BRL_CODE,
                category = AssetCategory.CURRENCY,
                baseCurrency = "USD",
                quoteCurrency = "BRL",
                modality = QuoteModality.COMMERCIAL,
                price = 5.75,
                changePercent = 0.25,
                variationPeriodLabel = "Dia",
                trend = QuoteTrend.UP,
                sourceName = "AwesomeAPI",
                sourceTimestampEpochMs = nowMs - 60_000L,
                fetchedAtEpochMs = nowMs - 60_000L
            )
        )
        store.saveValidQuote(
            MarketQuoteSnapshot(
                assetCode = MarketAssetCatalog.EUR_BRL_CODE,
                category = AssetCategory.CURRENCY,
                baseCurrency = "EUR",
                quoteCurrency = "BRL",
                modality = QuoteModality.PTAX,
                price = 6.30,
                changePercent = -0.10,
                variationPeriodLabel = "Diária",
                trend = QuoteTrend.DOWN,
                sourceName = "Banco Central (PTAX)",
                sourceTimestampEpochMs = nowMs - 3_600_000L,
                fetchedAtEpochMs = nowMs - 3_600_000L
            )
        )

        val requestedCodes = mutableListOf<String>()
        val routedRemote = CategoryRoutedMarketQuoteRemoteDataSource(
            routes = mapOf(
                AssetCategory.CURRENCY to object : MarketQuoteRemoteDataSource {
                    override suspend fun fetchQuote(asset: MarketAssetCatalog.AssetDescriptor): RemoteQuoteFetchResult {
                        requestedCodes += asset.code
                        return RemoteQuoteFetchResult.Failure(FailureReason.TIMEOUT, detail = "Timeout EUR")
                    }
                },
                AssetCategory.CRYPTO to object : MarketQuoteRemoteDataSource {
                    override suspend fun fetchQuote(asset: MarketAssetCatalog.AssetDescriptor): RemoteQuoteFetchResult {
                        requestedCodes += asset.code
                        return RemoteQuoteFetchResult.Success(
                            MarketQuoteSnapshot(
                                assetCode = asset.code,
                                category = AssetCategory.CRYPTO,
                                baseCurrency = asset.baseSymbol,
                                quoteCurrency = "BRL",
                                modality = QuoteModality.SPOT,
                                price = 1000.0,
                                changePercent = 2.0,
                                variationPeriodLabel = "24h",
                                trend = QuoteTrend.UP,
                                sourceName = "CoinGecko",
                                sourceTimestampEpochMs = nowMs,
                                fetchedAtEpochMs = nowMs
                            )
                        )
                    }
                }
            )
        )

        val repo = MarketQuoteRepository(
            remoteDataSource = routedRemote,
            store = store,
            isNetworkAvailable = { true },
            clock = { nowMs }
        )

        val states = repo.refreshAll(force = false)
        // USD-BRL estava dentro do cooldown (1 min) e nao foi consultado de novo
        assertFalse(MarketAssetCatalog.USD_BRL_CODE in requestedCodes)
        assertTrue(MarketAssetCatalog.EUR_BRL_CODE in requestedCodes)
        assertTrue(MarketAssetCatalog.BTC_BRL_CODE in requestedCodes)

        // EUR falhou mas preservou o cache valido de 6.30
        val eurState = states[MarketAssetCatalog.EUR_BRL_CODE]!!
        assertTrue(eurState.lastRefreshFailed)
        assertEquals(6.30, eurState.quote!!.price, 0.0001)

        // Criptos atualizaram com sucesso apesar da falha do EUR
        val btcState = states[MarketAssetCatalog.BTC_BRL_CODE]!!
        assertFalse(btcState.lastRefreshFailed)
        assertEquals(1000.0, btcState.quote!!.price, 0.01)

        // USD continua saudavel
        val usdState = states[MarketAssetCatalog.USD_BRL_CODE]!!
        assertFalse(usdState.lastRefreshFailed)
        assertEquals(5.75, usdState.quote!!.price, 0.0001)
    }

    @Test
    fun `parseB3InstrumentQuotation parses official delayed IBOV index points and daily change`() {
        val b3Json = """
            {"BizSts":{"cd":"OK"},"Msg":{"dtTm":"2026-11-18 09:38:29"},"Trad":[{"scty":{"SctyQtn":{"curPrc":169816.64,"prcFlcn":0.31},"mkt":{"nm":"Indices"},"symb":"IBOV","desc":"IBOVESPA"}}]}
        """.trimIndent()

        val result = MarketQuoteParsers.parseB3InstrumentQuotation(
            jsonBody = b3Json,
            asset = MarketAssetCatalog.IBOV,
            fetchedAtEpochMs = 1_795_000_000_000L
        )
        assertTrue(result is RemoteQuoteFetchResult.Success)
        val quote = (result as RemoteQuoteFetchResult.Success).quote

        assertEquals(MarketAssetCatalog.IBOV_CODE, quote.assetCode)
        assertEquals(AssetCategory.INDEX, quote.category)
        assertEquals("IBOV", quote.baseCurrency)
        assertEquals("BRL", quote.quoteCurrency)
        assertEquals("B3", quote.exchange)
        assertEquals(QuoteModality.DELAYED, quote.modality)
        assertTrue(quote.isDelayed)
        assertEquals(169816.64, quote.price, 0.01)
        assertEquals(0.31, quote.changePercent ?: 0.0, 0.001)
        assertEquals(QuoteTrend.UP, quote.trend)
        assertEquals("B3 (Cotação Pública)", quote.sourceName)
    }

    @Test
    fun `parseBrapiBatch parses PETR4 VALE3 and ITUB4 in BRL cleanly`() {
        val brapiJson = """
            {
              "results": [
                {"symbol":"PETR4","currency":"BRL","regularMarketPrice":43.98,"regularMarketChangePercent":-0.09,"regularMarketPreviousClose":44.02,"regularMarketTime":"2026-11-18T12:37:30.000Z"},
                {"symbol":"VALE3","currency":"BRL","regularMarketPrice":60.34,"regularMarketChangePercent":-0.59,"regularMarketPreviousClose":60.70,"regularMarketTime":"2026-11-18T12:37:30.000Z"},
                {"symbol":"ITUB4","currency":"BRL","regularMarketPrice":40.89,"regularMarketChangePercent":-0.99,"regularMarketPreviousClose":41.30,"regularMarketTime":"2026-11-18T12:37:30.000Z"}
              ]
            }
        """.trimIndent()

        val results = MarketQuoteParsers.parseBrapiBatch(
            jsonBody = brapiJson,
            assets = listOf(MarketAssetCatalog.PETR4, MarketAssetCatalog.VALE3, MarketAssetCatalog.ITUB4),
            fetchedAtEpochMs = 1_795_000_000_000L
        )

        val petr4 = (results[MarketAssetCatalog.PETR4_CODE] as RemoteQuoteFetchResult.Success).quote
        assertEquals(43.98, petr4.price, 0.001)
        assertEquals("BRL", petr4.quoteCurrency)
        assertEquals("B3", petr4.exchange)
        assertEquals(QuoteModality.EXCHANGE, petr4.modality)
        assertEquals(-0.09, petr4.changePercent ?: 0.0, 0.001)
        assertEquals(QuoteTrend.DOWN, petr4.trend)

        val vale3 = (results[MarketAssetCatalog.VALE3_CODE] as RemoteQuoteFetchResult.Success).quote
        assertEquals(60.34, vale3.price, 0.001)

        val itub4 = (results[MarketAssetCatalog.ITUB4_CODE] as RemoteQuoteFetchResult.Success).quote
        assertEquals(40.89, itub4.price, 0.001)
    }

    @Test
    fun `parseCnbcBatch and Nasdaq fallback handle SPX IXIC and MELI with US formatting`() {
        val cnbcJson = """
            {
              "FormattedQuoteResult": {
                "FormattedQuote": [
                  {"symbol":".SPX","last":"7,340.53","last_timedate":"11/17/26 EST","last_time":"2026-11-17","change":"-76.06","change_pct":"-1.03%","previous_day_closing":"7,340.53","currencyCode":"USD","exchange":"INDEX","realTime":"true","curmktstatus":"REG_MKT","timeZone":"EST"},
                  {"symbol":".IXIC","last":"24,328.383","last_timedate":"11/17/26 EST","last_time":"2026-11-17","change":"-181.375","change_pct":"-0.74%","previous_day_closing":"24,328.383","currencyCode":"USD","exchange":"NASDAQ","realTime":"true","curmktstatus":"REG_MKT","timeZone":"EST"},
                  {"symbol":"MELI","last":"1,436.83","last_timedate":"11/17/26 EST","last_time":"2026-11-17","change":"-25.29","change_pct":"-1.73%","previous_day_closing":"1,436.83","currencyCode":"USD","exchange":"NASDAQ","realTime":"true","curmktstatus":"PRE_MKT","timeZone":"EST"}
                ]
              }
            }
        """.trimIndent()

        val nasdaqMeliJson = """
            {
              "data": {
                "symbol": "MELI",
                "exchange": "NASDAQ-GS",
                "marketStatus": "Pre-Market",
                "primaryData": {
                  "lastSalePrice": "${'$'}1,446.00",
                  "netChange": "+9.17",
                  "percentageChange": "+0.64%",
                  "lastTradeTimestamp": "Nov 18, 2026 7:38 AM ET",
                  "isRealTime": true,
                  "bidPrice": "${'$'}1,439.68",
                  "askPrice": "${'$'}1,448.70"
                },
                "keyStats": {
                  "previousclose": {"value": "1,436.83"}
                }
              }
            }
        """.trimIndent()

        val cnbcTransport = object : MarketHttpTransport {
            override suspend fun get(url: String, headers: Map<String, String>): Result<HttpResponsePayload> {
                return Result.success(HttpResponsePayload(200, cnbcJson))
            }
        }
        val cnbcSource = CnbcMarketDataSource(cnbcTransport, clock = { 1_795_000_000_000L })
        val cnbcResults = runBlocking {
            cnbcSource.fetchQuotes(listOf(MarketAssetCatalog.SPX, MarketAssetCatalog.IXIC, MarketAssetCatalog.MELI))
        }

        val spx = (cnbcResults[MarketAssetCatalog.SPX_CODE] as RemoteQuoteFetchResult.Success).quote
        assertEquals(7340.53, spx.price, 0.01)
        assertEquals(-1.03, spx.changePercent ?: 0.0, 0.01)
        assertEquals("USD", spx.quoteCurrency)
        assertEquals("NYSE / S&P", spx.exchange)
        assertEquals(7416.59, spx.previousClose ?: 0.0, 0.01)

        val ixic = (cnbcResults[MarketAssetCatalog.IXIC_CODE] as RemoteQuoteFetchResult.Success).quote
        assertEquals(24328.383, ixic.price, 0.001)
        assertEquals("Nasdaq", ixic.exchange)

        val meliCnbc = (cnbcResults[MarketAssetCatalog.MELI_CODE] as RemoteQuoteFetchResult.Success).quote
        assertEquals(1436.83, meliCnbc.price, 0.01)
        assertTrue(meliCnbc.isDelayed)
        assertEquals("USD", meliCnbc.quoteCurrency)

        val nasdaqTransport = object : MarketHttpTransport {
            override suspend fun get(url: String, headers: Map<String, String>): Result<HttpResponsePayload> {
                assertTrue(url.contains("/quote/MELI/info?assetclass=stocks"))
                assertEquals("https://www.nasdaq.com", headers["Origin"])
                return Result.success(HttpResponsePayload(200, nasdaqMeliJson))
            }
        }
        val nasdaqResult = runBlocking {
            NasdaqPublicDataSource(nasdaqTransport, clock = { 1_795_000_000_000L })
                .fetchQuote(MarketAssetCatalog.MELI)
        }
        assertTrue(nasdaqResult is RemoteQuoteFetchResult.Success)
        val meliNasdaq = (nasdaqResult as RemoteQuoteFetchResult.Success).quote
        assertEquals(1446.00, meliNasdaq.price, 0.01)
        assertEquals(0.64, meliNasdaq.changePercent ?: 0.0, 0.01)
        assertEquals(1439.68, meliNasdaq.bidPrice ?: 0.0, 0.01)
        assertEquals("NASDAQ-GS", meliNasdaq.exchange)
        assertEquals("Nasdaq", meliNasdaq.sourceName)
    }

    @Test
    fun `stock route falls back from brapi to B3 when brapi hits rate limit`() = runBlocking {
        val transport = object : MarketHttpTransport {
            override suspend fun get(url: String, headers: Map<String, String>): Result<HttpResponsePayload> {
                return when {
                    url.contains("brapi.dev") -> Result.success(HttpResponsePayload(429, "{}"))
                    url.contains("cotacao.b3.com.br") -> Result.success(
                        HttpResponsePayload(
                            200,
                            """{"BizSts":{"cd":"OK"},"Msg":{"dtTm":"2026-11-18 10:00:00"},"Trad":[{"scty":{"SctyQtn":{"curPrc":44.10,"prcFlcn":0.25},"symb":"PETR4"}}]}"""
                        )
                    )
                    else -> Result.success(HttpResponsePayload(404, ""))
                }
            }
        }
        val stockRoute = CompositeMarketQuoteRemoteDataSource(
            providers = listOf(
                BrapiB3StockDataSource(transport),
                B3QuoteDataSource(transport)
            )
        )

        val result = stockRoute.fetchQuote(MarketAssetCatalog.PETR4)
        assertTrue(result is RemoteQuoteFetchResult.Success)
        val quote = (result as RemoteQuoteFetchResult.Success).quote
        assertEquals("B3 (Cotação Pública)", quote.sourceName)
        assertEquals(44.10, quote.price, 0.001)
        assertTrue(quote.isDelayed)
    }

    @Test
    fun `convertUsdToBrl requires valid USD BRL quote and formats index points vs USD stocks cleanly`() {
        assertNull(MarketQuoteCalculations.convertUsdToBrl(1436.83, null))
        assertNull(MarketQuoteCalculations.convertUsdToBrl(1436.83, MarketQuoteUiState(assetCode = MarketAssetCatalog.USD_BRL_CODE, quote = null)))

        val fxState = MarketQuoteUiState(
            assetCode = MarketAssetCatalog.USD_BRL_CODE,
            quote = MarketQuoteSnapshot(
                assetCode = MarketAssetCatalog.USD_BRL_CODE,
                category = AssetCategory.CURRENCY,
                baseCurrency = "USD",
                quoteCurrency = "BRL",
                modality = QuoteModality.COMMERCIAL,
                price = 5.50,
                sourceName = "AwesomeAPI",
                sourceTimestampEpochMs = 1_000L,
                fetchedAtEpochMs = 1_000L
            )
        )
        val converted = MarketQuoteCalculations.convertUsdToBrl(1000.0, fxState)
        assertNotNull(converted)
        assertEquals(5500.0, converted!!.amountBrl, 0.01)
        assertEquals("169.816,64 pts", MarketQuoteCalculations.formatPrice(169816.64, AssetCategory.INDEX, "BRL"))
        assertEquals("US$ 1.436,83", MarketQuoteCalculations.formatPrice(1436.83, AssetCategory.STOCK, "USD"))
        assertEquals("R$ 43,98", MarketQuoteCalculations.formatPrice(43.98, AssetCategory.STOCK, "BRL"))
    }
}
