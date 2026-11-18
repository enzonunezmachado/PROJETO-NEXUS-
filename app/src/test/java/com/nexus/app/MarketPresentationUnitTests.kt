package com.nexus.app

import com.nexus.app.domain.model.AssetCategory
import com.nexus.app.domain.model.MarketAssetCatalog
import com.nexus.app.domain.model.MarketQuoteSnapshot
import com.nexus.app.domain.model.MarketQuoteUiState
import com.nexus.app.domain.model.QuoteModality
import com.nexus.app.domain.model.QuoteTrend
import com.nexus.app.ui.market.MarketFilterCategory
import com.nexus.app.ui.market.MarketPresentationMapper
import com.nexus.app.ui.market.MarketStatusSeverity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MarketPresentationUnitTests {

    @Test
    fun `buildOverview aggregates active assets offline stale and planned sections without fake prices`() {
        val usdQuote = MarketQuoteSnapshot(
            assetCode = MarketAssetCatalog.USD_BRL_CODE,
            category = AssetCategory.CURRENCY,
            baseCurrency = "USD",
            quoteCurrency = "BRL",
            price = 5.78,
            bidPrice = 5.77,
            askPrice = 5.79,
            previousClose = 5.72,
            changePercent = 1.05,
            variationPeriodLabel = "dia",
            trend = QuoteTrend.UP,
            modality = QuoteModality.COMMERCIAL,
            sourceName = "AwesomeAPI",
            sourceTimestampEpochMs = 1_763_400_000_000L,
            fetchedAtEpochMs = 1_763_400_100_000L
        )
        val btcQuote = MarketQuoteSnapshot(
            assetCode = MarketAssetCatalog.BTC_BRL_CODE,
            category = AssetCategory.CRYPTO,
            baseCurrency = "BTC",
            quoteCurrency = "BRL",
            price = 520_000.0,
            bidPrice = null,
            askPrice = null,
            previousClose = null,
            changePercent = -2.4,
            variationPeriodLabel = "24h",
            trend = QuoteTrend.DOWN,
            modality = QuoteModality.SPOT,
            sourceName = "CoinGecko",
            sourceTimestampEpochMs = 1_763_400_050_000L,
            fetchedAtEpochMs = 1_763_400_200_000L
        )

        val states = mapOf(
            MarketAssetCatalog.USD_BRL_CODE to MarketQuoteUiState(
                assetCode = MarketAssetCatalog.USD_BRL_CODE,
                quote = usdQuote,
                isStale = false
            ),
            MarketAssetCatalog.EUR_BRL_CODE to MarketQuoteUiState(
                assetCode = MarketAssetCatalog.EUR_BRL_CODE,
                quote = null,
                isRefreshing = true
            ),
            MarketAssetCatalog.BTC_BRL_CODE to MarketQuoteUiState(
                assetCode = MarketAssetCatalog.BTC_BRL_CODE,
                quote = btcQuote,
                isStale = true,
                isOffline = true,
                lastRefreshFailed = true,
                statusMessage = "Sem internet • Exibindo último cache válido"
            )
        )

        val overview = MarketPresentationMapper.buildOverview(states)

        assertEquals(MarketAssetCatalog.SUPPORTED_ASSETS.size, overview.activeAssetCount)
        assertEquals(2, overview.loadedAssetCount)
        assertEquals(1, overview.staleOrFailedCount)
        assertTrue(overview.isAnyRefreshing)
        assertTrue(overview.isOffline)
        assertNotNull(overview.formattedLastSyncAt)

        assertEquals(MarketAssetCatalog.CURRENCY_ASSETS.size, overview.currencies.size)
        assertEquals(MarketAssetCatalog.CRYPTO_ASSETS.size, overview.cryptos.size)

        val usdRow = overview.currencies.first { it.descriptor.code == MarketAssetCatalog.USD_BRL_CODE }
        assertEquals("USD/BRL", usdRow.pairLabel)
        assertEquals("Dólar Comercial", usdRow.modalityLabel)
        assertEquals(MarketStatusSeverity.OK, usdRow.statusSeverity)
        assertTrue(usdRow.formattedChangeWithPeriod?.contains("dia") == true)

        val eurRow = overview.currencies.first { it.descriptor.code == MarketAssetCatalog.EUR_BRL_CODE }
        assertEquals("R$ --", eurRow.formattedPrice)
        assertEquals(MarketStatusSeverity.EMPTY, eurRow.statusSeverity)
        assertEquals("Consultando cotação...", eurRow.statusNote)

        val btcRow = overview.cryptos.first { it.descriptor.code == MarketAssetCatalog.BTC_BRL_CODE }
        assertEquals("Spot (24h)", btcRow.modalityLabel)
        assertEquals(MarketStatusSeverity.WARNING, btcRow.statusSeverity)
        assertTrue(btcRow.formattedChangeWithPeriod?.contains("24h") == true)

        assertEquals(2, overview.plannedSections.size)
        assertEquals(AssetCategory.INDEX, overview.plannedSections[0].category)
        assertEquals(AssetCategory.STOCK, overview.plannedSections[1].category)
        assertTrue(overview.plannedSections[0].plannedExamples.any { it.tickerHint == "IBOV" })
        assertTrue(overview.plannedSections[1].plannedExamples.any { it.tickerHint == "PETR4" })
    }

    @Test
    fun `buildDetail presents currency metadata and explicit historical limitation cleanly`() {
        val quote = MarketQuoteSnapshot(
            assetCode = MarketAssetCatalog.USD_BRL_CODE,
            category = AssetCategory.CURRENCY,
            baseCurrency = "USD",
            quoteCurrency = "BRL",
            price = 5.7540,
            bidPrice = 5.7530,
            askPrice = 5.7550,
            previousClose = 5.7000,
            changePercent = 0.95,
            variationPeriodLabel = "dia",
            trend = QuoteTrend.UP,
            modality = QuoteModality.PTAX,
            sourceName = "Banco Central (PTAX)",
            sourceTimestampEpochMs = 1_763_400_000_000L,
            fetchedAtEpochMs = 1_763_400_060_000L
        )
        val detail = MarketPresentationMapper.buildDetail(
            descriptor = MarketAssetCatalog.USD_BRL,
            state = MarketQuoteUiState(
                assetCode = MarketAssetCatalog.USD_BRL_CODE,
                quote = quote
            )
        )

        assertEquals("USD/BRL", detail.pairLabel)
        assertEquals("Moeda", detail.categoryLabel)
        assertEquals("Real Brasileiro (BRL)", detail.referenceCurrencyLabel)
        assertEquals("Dólar PTAX (BCB)", detail.modalityLabel)
        assertEquals("Banco Central (PTAX)", detail.sourceName)
        assertNotNull(detail.formattedBidPrice)
        assertNotNull(detail.formattedAskPrice)
        assertNotNull(detail.formattedPreviousClose)
        assertEquals("dia", detail.variationPeriodLabel)
        assertEquals("Cache atualizado", detail.cacheStatusLabel)
        assertEquals("Online", detail.connectivityStatusLabel)
        assertEquals(MarketStatusSeverity.OK, detail.statusSeverity)
        assertFalse(detail.hasHistoricalSeries)
        assertTrue(detail.historicalStatusMessage.contains("não estão armazenados"))
    }

    @Test
    fun `buildDetail handles missing variation and empty uncached state honestly`() {
        val solNoVar = MarketQuoteSnapshot(
            assetCode = MarketAssetCatalog.SOL_BRL_CODE,
            category = AssetCategory.CRYPTO,
            baseCurrency = "SOL",
            quoteCurrency = "BRL",
            price = 980.40,
            bidPrice = null,
            askPrice = null,
            previousClose = null,
            changePercent = null,
            variationPeriodLabel = null,
            trend = QuoteTrend.UNKNOWN,
            modality = QuoteModality.SPOT,
            sourceName = "CoinPaprika",
            sourceTimestampEpochMs = 1_763_400_000_000L,
            fetchedAtEpochMs = 1_763_400_000_000L
        )
        val offlineDetail = MarketPresentationMapper.buildDetail(
            descriptor = MarketAssetCatalog.SOL_BRL,
            state = MarketQuoteUiState(
                assetCode = MarketAssetCatalog.SOL_BRL_CODE,
                quote = solNoVar,
                isOffline = true,
                lastRefreshFailed = true
            )
        )

        assertNull(offlineDetail.changePercent)
        assertNull(offlineDetail.formattedChangeWithPeriod)
        assertNull(offlineDetail.formattedBidPrice)
        assertEquals("Spot (24h)", offlineDetail.modalityLabel)
        assertEquals("Offline", offlineDetail.connectivityStatusLabel)
        assertEquals("Cache preservado (última tentativa falhou)", offlineDetail.cacheStatusLabel)
        assertEquals(MarketStatusSeverity.WARNING, offlineDetail.statusSeverity)
        assertFalse(offlineDetail.hasHistoricalSeries)

        val emptyDetail = MarketPresentationMapper.buildDetail(
            descriptor = MarketAssetCatalog.ETH_BRL,
            state = MarketQuoteUiState(assetCode = MarketAssetCatalog.ETH_BRL_CODE)
        )
        assertEquals("R$ --", emptyDetail.formattedPrice)
        assertEquals("Sem cache local salvo", emptyDetail.cacheStatusLabel)
        assertEquals(MarketStatusSeverity.EMPTY, emptyDetail.statusSeverity)
        assertNull(emptyDetail.sourceName)
        assertFalse(emptyDetail.hasHistoricalSeries)
    }

    @Test
    fun `market filter categories expose expected labels in order`() {
        assertEquals(
            listOf("Todos", "Moedas", "Cripto", "Índices", "Ações"),
            MarketFilterCategory.entries.map { it.label }
        )
    }
}
