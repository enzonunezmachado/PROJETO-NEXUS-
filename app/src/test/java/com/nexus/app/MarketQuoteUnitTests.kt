package com.nexus.app

import com.nexus.app.data.market.AwesomeApiUsdBrlDataSource
import com.nexus.app.data.market.BcbOlindaPtaxDataSource
import com.nexus.app.data.market.CompositeMarketQuoteRemoteDataSource
import com.nexus.app.data.market.FailureReason
import com.nexus.app.data.market.HttpResponsePayload
import com.nexus.app.data.market.MarketHttpTransport
import com.nexus.app.data.market.MarketQuoteParsers
import com.nexus.app.data.market.MarketQuoteRepository
import com.nexus.app.data.market.MarketQuoteStore
import com.nexus.app.data.market.PersistedMarketQuoteRecord
import com.nexus.app.data.market.RemoteQuoteFetchResult
import com.nexus.app.domain.model.MarketAssetCatalog
import com.nexus.app.domain.model.MarketQuoteCalculations
import com.nexus.app.domain.model.MarketQuoteSnapshot
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
    }
}
