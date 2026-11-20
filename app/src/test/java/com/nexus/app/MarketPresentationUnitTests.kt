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
    fun `buildOverview aggregates currencies cryptos indices and stocks with USD to BRL conversion when FX is valid`() {
        val usdQuote = MarketQuoteSnapshot(
            assetCode = MarketAssetCatalog.USD_BRL_CODE,
            category = AssetCategory.CURRENCY,
            baseCurrency = "USD",
            quoteCurrency = "BRL",
            price = 5.50,
            bidPrice = 5.49,
            askPrice = 5.51,
            previousClose = 5.45,
            changePercent = 0.92,
            variationPeriodLabel = "Dia",
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
            changePercent = -2.4,
            variationPeriodLabel = "24h",
            trend = QuoteTrend.DOWN,
            modality = QuoteModality.SPOT,
            sourceName = "CoinGecko",
            sourceTimestampEpochMs = 1_763_400_050_000L,
            fetchedAtEpochMs = 1_763_400_200_000L
        )
        val ibovQuote = MarketQuoteSnapshot(
            assetCode = MarketAssetCatalog.IBOV_CODE,
            category = AssetCategory.INDEX,
            baseCurrency = "IBOV",
            quoteCurrency = "BRL",
            price = 169_816.64,
            changePercent = 0.31,
            variationPeriodLabel = "Dia",
            trend = QuoteTrend.UP,
            modality = QuoteModality.DELAYED,
            sourceName = "B3 (Cotação Pública)",
            sourceTimestampEpochMs = 1_763_400_050_000L,
            fetchedAtEpochMs = 1_763_400_250_000L,
            exchange = "B3",
            isDelayed = true,
            delayNotice = "Feed público da B3 com atraso aproximado de 15 min"
        )
        val meliQuote = MarketQuoteSnapshot(
            assetCode = MarketAssetCatalog.MELI_CODE,
            category = AssetCategory.STOCK,
            baseCurrency = "MELI",
            quoteCurrency = "USD",
            price = 1_000.00,
            changePercent = -1.25,
            variationPeriodLabel = "Dia",
            trend = QuoteTrend.DOWN,
            modality = QuoteModality.EXCHANGE,
            sourceName = "CNBC Market",
            sourceTimestampEpochMs = 1_763_400_080_000L,
            fetchedAtEpochMs = 1_763_400_300_000L,
            exchange = "Nasdaq"
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
            ),
            MarketAssetCatalog.IBOV_CODE to MarketQuoteUiState(
                assetCode = MarketAssetCatalog.IBOV_CODE,
                quote = ibovQuote
            ),
            MarketAssetCatalog.MELI_CODE to MarketQuoteUiState(
                assetCode = MarketAssetCatalog.MELI_CODE,
                quote = meliQuote
            )
        )

        val overview = MarketPresentationMapper.buildOverview(states)

        assertEquals(MarketAssetCatalog.SUPPORTED_ASSETS.size, overview.activeAssetCount)
        assertEquals(4, overview.loadedAssetCount)
        assertEquals(1, overview.staleOrFailedCount)
        assertTrue(overview.isAnyRefreshing)
        assertTrue(overview.isOffline)
        assertNotNull(overview.formattedLastSyncAt)

        assertEquals(MarketAssetCatalog.CURRENCY_ASSETS.size, overview.currencies.size)
        assertEquals(MarketAssetCatalog.CRYPTO_ASSETS.size, overview.cryptos.size)
        assertEquals(MarketAssetCatalog.INDEX_ASSETS.size, overview.indices.size)
        assertEquals(MarketAssetCatalog.STOCK_ASSETS.size, overview.stocks.size)

        val ibovRow = overview.indices.first { it.descriptor.code == MarketAssetCatalog.IBOV_CODE }
        assertEquals("IBOV", ibovRow.ticker)
        assertEquals("B3", ibovRow.exchangeLabel)
        assertEquals("169.816,64 pts", ibovRow.formattedPrice)
        assertNull(ibovRow.formattedConvertedBrl)
        assertTrue(ibovRow.isDelayed)

        val meliRow = overview.stocks.first { it.descriptor.code == MarketAssetCatalog.MELI_CODE }
        assertEquals("MELI", meliRow.ticker)
        assertEquals("Nasdaq", meliRow.exchangeLabel)
        assertEquals("USD", meliRow.originalCurrency)
        assertEquals("US$ 1.000,00", meliRow.formattedPrice)
        assertEquals("R$ 5.500,00", meliRow.formattedConvertedBrl)
        assertNull(meliRow.conversionUnavailableNote)

        val eurRow = overview.currencies.first { it.descriptor.code == MarketAssetCatalog.EUR_BRL_CODE }
        assertEquals("R$ --", eurRow.formattedPrice)
        assertEquals(MarketStatusSeverity.EMPTY, eurRow.statusSeverity)
        assertEquals("Consultando cotação...", eurRow.statusNote)

        val btcRow = overview.cryptos.first { it.descriptor.code == MarketAssetCatalog.BTC_BRL_CODE }
        assertEquals("Spot (24h)", btcRow.modalityLabel)
        assertEquals(MarketStatusSeverity.WARNING, btcRow.statusSeverity)
    }

    @Test
    fun `buildDetail handles MELI without USD BRL conversion rate honestly`() {
        val meliQuote = MarketQuoteSnapshot(
            assetCode = MarketAssetCatalog.MELI_CODE,
            category = AssetCategory.STOCK,
            baseCurrency = "MELI",
            quoteCurrency = "USD",
            price = 1_436.83,
            previousClose = 1_462.12,
            changePercent = -1.73,
            variationPeriodLabel = "Dia",
            trend = QuoteTrend.DOWN,
            modality = QuoteModality.DELAYED,
            sourceName = "CNBC Market",
            sourceTimestampEpochMs = 1_763_400_000_000L,
            fetchedAtEpochMs = 1_763_400_060_000L,
            exchange = "Nasdaq",
            isDelayed = true,
            delayNotice = "Pré-mercado · referência último fechamento"
        )
        val detailWithoutFx = MarketPresentationMapper.buildDetail(
            descriptor = MarketAssetCatalog.MELI,
            state = MarketQuoteUiState(
                assetCode = MarketAssetCatalog.MELI_CODE,
                quote = meliQuote
            ),
            usdBrlState = null
        )

        assertEquals("MELI", detailWithoutFx.ticker)
        assertEquals("Nasdaq", detailWithoutFx.exchangeLabel)
        assertEquals("Dólar Americano (USD)", detailWithoutFx.originalCurrencyLabel)
        assertEquals("US$ 1.436,83", detailWithoutFx.formattedPrice)
        assertNull(detailWithoutFx.formattedConvertedBrl)
        assertTrue(detailWithoutFx.convertedBrlDetailNote?.contains("indisponível") == true)
        assertTrue(detailWithoutFx.isDelayed)
        assertFalse(detailWithoutFx.hasHistoricalSeries)
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
            variationPeriodLabel = "Dia",
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
        assertEquals("Dia", detail.variationPeriodLabel)
        assertEquals("Cache atualizado", detail.cacheStatusLabel)
        assertEquals("Online", detail.connectivityStatusLabel)
        assertEquals(MarketStatusSeverity.OK, detail.statusSeverity)
        assertFalse(detail.hasHistoricalSeries)
    }

    @Test
    fun `market filter categories expose expected labels in order`() {
        assertEquals(
            listOf("Todos", "Moedas", "Cripto", "Índices", "Ações"),
            MarketFilterCategory.entries.map { it.label }
        )
    }
}
