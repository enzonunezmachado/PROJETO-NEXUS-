package com.nexus.app

import com.nexus.app.data.market.AwesomeApiDailyHistoryDataSource
import com.nexus.app.data.market.BcbOlindaHistoryDataSource
import com.nexus.app.data.market.CompositeMarketHistoryRemoteDataSource
import com.nexus.app.data.market.FailureReason
import com.nexus.app.data.market.FileMarketHistoryStore
import com.nexus.app.data.market.FrankfurterHistoryDataSource
import com.nexus.app.data.market.HttpResponsePayload
import com.nexus.app.data.market.MarketHistoryParsers
import com.nexus.app.data.market.MarketHistoryRemoteDataSource
import com.nexus.app.data.market.MarketHistoryRepository
import com.nexus.app.data.market.MarketHistoryStore
import com.nexus.app.data.market.MarketHttpTransport
import com.nexus.app.data.market.PersistedMarketHistoryRecord
import com.nexus.app.data.market.RemoteHistoryFetchResult
import com.nexus.app.domain.model.MarketAssetCatalog
import com.nexus.app.domain.model.MarketHistoryCalculations
import com.nexus.app.domain.model.MarketHistoryGranularity
import com.nexus.app.domain.model.MarketHistoryPoint
import com.nexus.app.domain.model.MarketHistoryRange
import com.nexus.app.domain.model.MarketHistorySeries
import com.nexus.app.domain.model.MarketQuoteUiState
import com.nexus.app.domain.model.QuoteTrend
import com.nexus.app.ui.market.MarketPresentationMapper
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.time.LocalDate

class MarketHistoryUnitTests {

    @Test
    fun `supportedRangesFor omits 1D intraday for currencies and enables all 6 for other categories`() {
        val usdRanges = MarketHistoryCalculations.supportedRangesFor(MarketAssetCatalog.USD_BRL_CODE)
        val eurRanges = MarketHistoryCalculations.supportedRangesFor(MarketAssetCatalog.EUR_BRL_CODE)
        assertFalse(MarketHistoryRange.D1 in usdRanges)
        assertFalse(MarketHistoryRange.D1 in eurRanges)
        assertEquals(
            listOf(
                MarketHistoryRange.W1,
                MarketHistoryRange.M1,
                MarketHistoryRange.M3,
                MarketHistoryRange.M6,
                MarketHistoryRange.Y1
            ),
            usdRanges
        )

        val btcRanges = MarketHistoryCalculations.supportedRangesFor(MarketAssetCatalog.BTC_BRL_CODE)
        val spxRanges = MarketHistoryCalculations.supportedRangesFor(MarketAssetCatalog.SPX_CODE)
        val petrRanges = MarketHistoryCalculations.supportedRangesFor(MarketAssetCatalog.PETR4_CODE)
        assertEquals(MarketHistoryRange.entries.toList(), btcRanges)
        assertEquals(MarketHistoryRange.entries.toList(), spxRanges)
        assertEquals(MarketHistoryRange.entries.toList(), petrRanges)
    }

    @Test
    fun `normalizeAndDeduplicate removes invalid prices duplicates by timestamp and sorts ascending`() {
        val raw = listOf(
            MarketHistoryPoint(3_000L, 10.5),
            MarketHistoryPoint(1_000L, -5.0),
            MarketHistoryPoint(2_000L, 10.0),
            MarketHistoryPoint(2_000L, 10.2),
            MarketHistoryPoint(1_500L, Double.NaN),
            MarketHistoryPoint(0L, 11.0),
            MarketHistoryPoint(1_200L, 9.8)
        )
        val normalized = MarketHistoryCalculations.normalizeAndDeduplicate(raw)
        assertEquals(3, normalized.size)
        assertEquals(1_200L, normalized[0].timestampEpochMs)
        assertEquals(9.8, normalized[0].price, 0.0001)
        assertEquals(2_000L, normalized[1].timestampEpochMs)
        assertEquals(10.2, normalized[1].price, 0.0001)
        assertEquals(3_000L, normalized[2].timestampEpochMs)
    }

    @Test
    fun `computeMetrics calculates start end min max and percentage change accurately`() {
        val series = MarketHistorySeries(
            assetCode = MarketAssetCatalog.PETR4_CODE,
            rangeKey = MarketHistoryRange.M1.key,
            granularity = MarketHistoryGranularity.DAILY,
            currency = "BRL",
            sourceName = "brapi.dev (B3)",
            fetchedAtEpochMs = 1_795_000_000_000L,
            points = listOf(
                MarketHistoryPoint(1_000L, 40.0),
                MarketHistoryPoint(2_000L, 38.0),
                MarketHistoryPoint(3_000L, 46.0),
                MarketHistoryPoint(4_000L, 44.0)
            )
        )
        val metrics = MarketHistoryCalculations.computeMetrics(series)
        assertNotNull(metrics)
        assertEquals(40.0, metrics!!.startPrice, 0.001)
        assertEquals(44.0, metrics.endPrice, 0.001)
        assertEquals(38.0, metrics.minPrice, 0.001)
        assertEquals(46.0, metrics.maxPrice, 0.001)
        assertEquals(10.0, metrics.changePercent ?: 0.0, 0.001)
        assertEquals(QuoteTrend.UP, metrics.trend)
        assertEquals(4, metrics.pointCount)
    }

    @Test
    fun `parseBcbOlindaHistory keeps daily Fechamento points chronologically without duplicates`() {
        val bcbJson = """
            {
              "value": [
                {"cotacaoCompra": 5.71, "cotacaoVenda": 5.72, "dataHoraCotacao": "2026-11-16 10:00:00.000", "tipoBoletim": "Abertura"},
                {"cotacaoCompra": 5.74, "cotacaoVenda": 5.75, "dataHoraCotacao": "2026-11-16 13:05:00.000", "tipoBoletim": "Fechamento"},
                {"cotacaoCompra": 5.77, "cotacaoVenda": 5.78, "dataHoraCotacao": "2026-11-17 13:07:00.000", "tipoBoletim": "Fechamento"}
              ]
            }
        """.trimIndent()
        val result = MarketHistoryParsers.parseBcbOlindaHistory(
            jsonBody = bcbJson,
            asset = MarketAssetCatalog.EUR_BRL,
            range = MarketHistoryRange.M1,
            fetchedAtEpochMs = 1_795_000_000_000L
        )
        assertTrue(result is RemoteHistoryFetchResult.Success)
        val series = (result as RemoteHistoryFetchResult.Success).series
        assertEquals(MarketHistoryGranularity.DAILY, series.granularity)
        assertEquals("Banco Central (PTAX)", series.sourceName)
        assertEquals(2, series.points.size)
        assertEquals(5.75, series.points[0].price, 0.0001)
        assertEquals(5.78, series.points[1].price, 0.0001)
    }

    @Test
    fun `parseCoinGeckoMarketChart and OkxCandles parse BRL crypto history with honest granularity`() {
        val geckoJson = """
            {
              "prices": [
                [1794919800000, 375306.73],
                [1794923400000, 373900.12],
                [1795005880000, 372664.99]
              ]
            }
        """.trimIndent()
        val geckoRes = MarketHistoryParsers.parseCoinGeckoMarketChart(
            jsonBody = geckoJson,
            asset = MarketAssetCatalog.BTC_BRL,
            range = MarketHistoryRange.D1,
            fetchedAtEpochMs = 1_795_006_000_000L
        )
        assertTrue(geckoRes is RemoteHistoryFetchResult.Success)
        val geckoSeries = (geckoRes as RemoteHistoryFetchResult.Success).series
        assertEquals(MarketHistoryGranularity.INTRADAY, geckoSeries.granularity)
        assertEquals("CoinGecko", geckoSeries.sourceName)
        assertEquals(3, geckoSeries.points.size)

        val okxJson = """
            {
              "code": "0",
              "data": [
                ["1795005900000","432.1","435.0","431.0","433.5"],
                ["1795003200000","428.0","433.0","427.0","432.1"]
              ]
            }
        """.trimIndent()
        val okxRes = MarketHistoryParsers.parseOkxCandles(
            jsonBody = okxJson,
            asset = MarketAssetCatalog.SOL_BRL,
            range = MarketHistoryRange.W1,
            granularity = MarketHistoryGranularity.INTRADAY,
            fetchedAtEpochMs = 1_795_006_000_000L
        )
        assertTrue(okxRes is RemoteHistoryFetchResult.Success)
        val okxSeries = (okxRes as RemoteHistoryFetchResult.Success).series
        assertEquals(2, okxSeries.points.size)
        assertEquals(432.1, okxSeries.points[0].price, 0.001)
        assertEquals(433.5, okxSeries.points[1].price, 0.001)
    }

    @Test
    fun `parseBrapiHistorical B3Fluctuation Nasdaq and CnbcBars parse stock and index series`() {
        val brapiJson = """
            {
              "results": [
                {
                  "symbol": "PETR4",
                  "currency": "BRL",
                  "historicalDataPrice": [
                    {"date": 1794538800, "close": 45.30},
                    {"date": 1794798000, "close": 43.98}
                  ]
                }
              ]
            }
        """.trimIndent()
        val brapiRes = MarketHistoryParsers.parseBrapiHistorical(
            jsonBody = brapiJson,
            asset = MarketAssetCatalog.PETR4,
            range = MarketHistoryRange.M1,
            granularity = MarketHistoryGranularity.DAILY,
            fetchedAtEpochMs = 1_795_000_000_000L
        )
        assertTrue(brapiRes is RemoteHistoryFetchResult.Success)
        assertEquals(2, (brapiRes as RemoteHistoryFetchResult.Success).series.points.size)

        val b3Json = """
            {
              "TradgFlr": {
                "date": "2026-11-17",
                "scty": {
                  "lstQtn": [
                    {"closPric": 169300.29, "dtTm": "10:00:01"},
                    {"closPric": 169816.64, "dtTm": "18:17:00"}
                  ]
                }
              }
            }
        """.trimIndent()
        val b3Res = MarketHistoryParsers.parseB3DailyFluctuation(
            jsonBody = b3Json,
            asset = MarketAssetCatalog.IBOV,
            fetchedAtEpochMs = 1_795_000_000_000L
        )
        assertTrue(b3Res is RemoteHistoryFetchResult.Success)
        val ibovSeries = (b3Res as RemoteHistoryFetchResult.Success).series
        assertEquals(MarketHistoryGranularity.INTRADAY, ibovSeries.granularity)
        assertEquals(169816.64, ibovSeries.points.last().price, 0.01)

        val nasdaqHistJson = """
            {
              "data": {
                "tradesTable": {
                  "rows": [
                    {"date": "11/17/2026", "close": "${'$'}1,436.83"},
                    {"date": "11/16/2026", "close": "${'$'}1,462.12"}
                  ]
                }
              }
            }
        """.trimIndent()
        val nasdaqRes = MarketHistoryParsers.parseNasdaqHistoricalTable(
            jsonBody = nasdaqHistJson,
            asset = MarketAssetCatalog.MELI,
            range = MarketHistoryRange.M1,
            fetchedAtEpochMs = 1_795_000_000_000L
        )
        assertTrue(nasdaqRes is RemoteHistoryFetchResult.Success)
        val meliSeries = (nasdaqRes as RemoteHistoryFetchResult.Success).series
        assertEquals(1462.12, meliSeries.points[0].price, 0.01)
        assertEquals(1436.83, meliSeries.points[1].price, 0.01)

        val cnbcBarsJson = """
            {
              "barData": {
                "priceBars": [
                  {"close": "7500.00", "tradeTime": "20261116153000", "tradeTimeinMills": 1794861000000},
                  {"close": "7340.53", "tradeTime": "20261117093000", "tradeTimeinMills": 1794925800000},
                  {"close": "7355.10", "tradeTime": "20261117160000", "tradeTimeinMills": 1794949200000}
                ]
              }
            }
        """.trimIndent()
        val cnbcRes = MarketHistoryParsers.parseCnbcBars(
            jsonBody = cnbcBarsJson,
            asset = MarketAssetCatalog.SPX,
            range = MarketHistoryRange.D1,
            granularity = MarketHistoryGranularity.INTRADAY,
            marketZone = java.time.ZoneId.of("America/New_York"),
            latestSessionOnly = true,
            fetchedAtEpochMs = 1_795_000_000_000L
        )
        assertTrue(cnbcRes is RemoteHistoryFetchResult.Success)
        val spxPoints = (cnbcRes as RemoteHistoryFetchResult.Success).series.points
        assertEquals(2, spxPoints.size)
        assertEquals(7340.53, spxPoints[0].price, 0.01)
        assertEquals(7355.10, spxPoints[1].price, 0.01)
    }

    @Test
    fun `currency history composite falls back to Frankfurter when BCB and AwesomeAPI fail`() = runBlocking {
        val transport = object : MarketHttpTransport {
            override suspend fun get(url: String, headers: Map<String, String>): Result<HttpResponsePayload> {
                return when {
                    url.contains("olinda.bcb.gov.br") -> Result.success(HttpResponsePayload(503, ""))
                    url.contains("awesomeapi.com.br") -> Result.success(HttpResponsePayload(429, "{}"))
                    url.contains("frankfurter.app") -> Result.success(
                        HttpResponsePayload(
                            200,
                            """{"rates":{"2026-11-13":{"BRL":5.71},"2026-11-16":{"BRL":5.74}}}"""
                        )
                    )
                    else -> Result.success(HttpResponsePayload(404, ""))
                }
            }
        }
        val today = { LocalDate.of(2026, 11, 18) }
        val composite = CompositeMarketHistoryRemoteDataSource(
            providers = listOf(
                BcbOlindaHistoryDataSource(transport, todayProvider = today),
                AwesomeApiDailyHistoryDataSource(transport),
                FrankfurterHistoryDataSource(transport, todayProvider = today)
            )
        )
        val result = composite.fetchHistory(MarketAssetCatalog.USD_BRL, MarketHistoryRange.M1)
        assertTrue(result is RemoteHistoryFetchResult.Success)
        val series = (result as RemoteHistoryFetchResult.Success).series
        assertEquals("Frankfurter (BCE)", series.sourceName)
        assertEquals(2, series.points.size)
    }

    @Test
    fun `FileMarketHistoryStore persists offline reads deduplicates and preserves valid cache on failures`() = runBlocking {
        val tempDir = Files.createTempDirectory("nexus_hist_test").toFile()
        try {
            val store = FileMarketHistoryStore(tempDir)
            val validSeries = MarketHistorySeries(
                assetCode = MarketAssetCatalog.BTC_BRL_CODE,
                rangeKey = MarketHistoryRange.M1.key,
                granularity = MarketHistoryGranularity.INTRADAY,
                currency = "BRL",
                sourceName = "CoinGecko",
                fetchedAtEpochMs = 1_000_000L,
                points = listOf(
                    MarketHistoryPoint(200_000L, 372_000.0),
                    MarketHistoryPoint(100_000L, 365_000.0),
                    MarketHistoryPoint(200_000L, 372_500.0)
                )
            )
            store.saveValidSeries(validSeries)
            val loaded = store.loadRecord(MarketAssetCatalog.BTC_BRL_CODE, MarketHistoryRange.M1)
            assertNotNull(loaded?.series)
            assertEquals(2, loaded!!.series!!.points.size)
            assertEquals(365_000.0, loaded.series!!.points[0].price, 0.1)
            assertEquals(372_500.0, loaded.series!!.points[1].price, 0.1)

            val failingRemote = object : MarketHistoryRemoteDataSource {
                override suspend fun fetchHistory(
                    asset: MarketAssetCatalog.AssetDescriptor,
                    range: MarketHistoryRange
                ): RemoteHistoryFetchResult = RemoteHistoryFetchResult.Failure(
                    reason = FailureReason.RATE_LIMITED,
                    httpStatusCode = 429,
                    detail = "Rate limit"
                )
            }
            val repo = MarketHistoryRepository(
                remoteDataSource = failingRemote,
                store = store,
                isNetworkAvailable = { true },
                clock = { 5_000_000L }
            )
            val afterFail = repo.loadHistory(MarketAssetCatalog.BTC_BRL_CODE, MarketHistoryRange.M1, force = true)
            assertTrue(afterFail.lastRefreshFailed)
            assertNotNull(afterFail.series)
            assertEquals(2, afterFail.series!!.points.size)
            assertEquals("CoinGecko", afterFail.series!!.sourceName)

            val detail = MarketPresentationMapper.buildDetail(
                descriptor = MarketAssetCatalog.BTC_BRL,
                state = MarketQuoteUiState(assetCode = MarketAssetCatalog.BTC_BRL_CODE),
                historyState = afterFail
            )
            assertTrue(detail.hasHistoricalSeries)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `MarketHistoryRepository falls back to default supported range when unsupported range requested`() = runBlocking {
        val inMemoryStore = object : MarketHistoryStore {
            private val map = HashMap<Pair<String, MarketHistoryRange>, PersistedMarketHistoryRecord>()
            override fun loadRecord(assetCode: String, range: MarketHistoryRange) = map[assetCode to range]
            override fun loadAllForAsset(assetCode: String) =
                map.filterKeys { it.first == assetCode }.mapKeys { it.key.second }
            override fun saveValidSeries(series: MarketHistorySeries) {
                val range = series.resolveRange() ?: return
                map[series.assetCode to range] = PersistedMarketHistoryRecord(series, series.fetchedAtEpochMs, null)
            }
            override fun recordFailedAttempt(assetCode: String, range: MarketHistoryRange, attemptAtEpochMs: Long, errorMessage: String) {
                val prev = map[assetCode to range]
                map[assetCode to range] = PersistedMarketHistoryRecord(prev?.series, attemptAtEpochMs, errorMessage)
            }
        }
        var requestedRange: MarketHistoryRange? = null
        val remote = object : MarketHistoryRemoteDataSource {
            override suspend fun fetchHistory(
                asset: MarketAssetCatalog.AssetDescriptor,
                range: MarketHistoryRange
            ): RemoteHistoryFetchResult {
                requestedRange = range
                return RemoteHistoryFetchResult.Success(
                    MarketHistorySeries(
                        assetCode = asset.code,
                        rangeKey = range.key,
                        granularity = MarketHistoryGranularity.DAILY,
                        currency = "BRL",
                        sourceName = "Banco Central (PTAX)",
                        fetchedAtEpochMs = 2_000_000L,
                        points = listOf(
                            MarketHistoryPoint(1_000L, 5.70),
                            MarketHistoryPoint(2_000L, 5.75)
                        )
                    )
                )
            }
        }
        val repo = MarketHistoryRepository(
            remoteDataSource = remote,
            store = inMemoryStore,
            isNetworkAvailable = { true },
            clock = { 2_000_000L }
        )
        val state = repo.loadHistory(MarketAssetCatalog.USD_BRL_CODE, MarketHistoryRange.D1, force = true)
        assertEquals(MarketHistoryRange.M1, requestedRange)
        assertEquals(MarketHistoryRange.M1, state.selectedRange)
        assertNotNull(state.series)
    }
}
