package com.nexus.app

import com.nexus.app.data.market.MarketWatchlistRepository
import com.nexus.app.data.market.MarketWatchlistStore
import com.nexus.app.domain.model.AssetCategory
import com.nexus.app.domain.model.MarketAssetCatalog
import com.nexus.app.domain.model.MarketQuoteSnapshot
import com.nexus.app.domain.model.MarketQuoteUiState
import com.nexus.app.domain.model.QuoteModality
import com.nexus.app.domain.model.QuoteTrend
import com.nexus.app.ui.market.MarketPresentationMapper
import com.nexus.app.ui.market.MarketStatusSeverity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MarketWatchlistUnitTests {

    private class InMemoryWatchlistStore(initial: List<String> = emptyList()) : MarketWatchlistStore {
        var saved: List<String> = initial
        override fun loadFavoriteCodes(): List<String> = saved
        override fun saveFavoriteCodes(codes: List<String>) {
            saved = codes
        }
    }

    @Test
    fun `watchlist repository toggles adds removes without duplicates and preserves insertion order`() {
        val store = InMemoryWatchlistStore()
        val repo = MarketWatchlistRepository(store)

        assertTrue(repo.favoriteCodes.value.isEmpty())

        assertTrue(repo.toggleFavorite(MarketAssetCatalog.BTC_BRL_CODE))
        assertTrue(repo.toggleFavorite(MarketAssetCatalog.USD_BRL_CODE))
        assertTrue(repo.toggleFavorite(MarketAssetCatalog.PETR4_CODE))

        assertEquals(
            listOf(
                MarketAssetCatalog.BTC_BRL_CODE,
                MarketAssetCatalog.USD_BRL_CODE,
                MarketAssetCatalog.PETR4_CODE
            ),
            repo.favoriteCodes.value
        )

        // Adding duplicate or unknown code must be ignored without changing order
        assertFalse(repo.addFavorite(MarketAssetCatalog.USD_BRL_CODE))
        assertFalse(repo.toggleFavorite("UNKNOWN_ASSET"))
        assertEquals(3, repo.favoriteCodes.value.size)

        // Removing middle item preserves order of remaining favorites
        assertFalse(repo.toggleFavorite(MarketAssetCatalog.USD_BRL_CODE))
        assertEquals(
            listOf(MarketAssetCatalog.BTC_BRL_CODE, MarketAssetCatalog.PETR4_CODE),
            repo.favoriteCodes.value
        )

        // Recreating repository simulates app restart and restores persisted order
        val reloaded = MarketWatchlistRepository(store)
        assertEquals(
            listOf(MarketAssetCatalog.BTC_BRL_CODE, MarketAssetCatalog.PETR4_CODE),
            reloaded.favoriteCodes.value
        )
    }

    @Test
    fun `store sanitization drops invalid and duplicate codes on initialization`() {
        val store = InMemoryWatchlistStore(
            listOf(
                MarketAssetCatalog.IBOV_CODE,
                "FAKE",
                MarketAssetCatalog.IBOV_CODE,
                MarketAssetCatalog.MELI_CODE
            )
        )
        val repo = MarketWatchlistRepository(store)
        assertEquals(
            listOf(MarketAssetCatalog.IBOV_CODE, MarketAssetCatalog.MELI_CODE),
            repo.favoriteCodes.value
        )
    }

    @Test
    fun `MarketPresentationMapper builds watchlist exclusively from favorites in stable order with accurate statuses`() {
        val btcQuote = MarketQuoteSnapshot(
            assetCode = MarketAssetCatalog.BTC_BRL_CODE,
            category = AssetCategory.CRYPTO,
            baseCurrency = "BTC",
            quoteCurrency = "BRL",
            modality = QuoteModality.SPOT,
            price = 372_000.0,
            changePercent = 1.85,
            variationPeriodLabel = "24h",
            trend = QuoteTrend.UP,
            sourceName = "CoinGecko",
            sourceTimestampEpochMs = 1_795_000_000_000L,
            fetchedAtEpochMs = 1_795_000_000_000L
        )
        val ibovQuote = MarketQuoteSnapshot(
            assetCode = MarketAssetCatalog.IBOV_CODE,
            category = AssetCategory.INDEX,
            baseCurrency = "IBOV",
            quoteCurrency = "BRL",
            modality = QuoteModality.DELAYED,
            price = 169_816.64,
            changePercent = 0.31,
            variationPeriodLabel = "Dia",
            trend = QuoteTrend.UP,
            sourceName = "B3 (Cotação Pública)",
            sourceTimestampEpochMs = 1_795_000_000_000L,
            fetchedAtEpochMs = 1_795_000_000_000L,
            exchange = "B3"
        )
        val meliQuote = MarketQuoteSnapshot(
            assetCode = MarketAssetCatalog.MELI_CODE,
            category = AssetCategory.STOCK,
            baseCurrency = "MELI",
            quoteCurrency = "USD",
            modality = QuoteModality.EXCHANGE,
            price = 1_441.00,
            changePercent = -0.40,
            variationPeriodLabel = "Dia",
            trend = QuoteTrend.DOWN,
            sourceName = "Nasdaq",
            sourceTimestampEpochMs = 1_795_000_000_000L,
            fetchedAtEpochMs = 1_795_000_000_000L,
            exchange = "Nasdaq"
        )

        val states = mapOf(
            MarketAssetCatalog.BTC_BRL_CODE to MarketQuoteUiState(
                assetCode = MarketAssetCatalog.BTC_BRL_CODE,
                quote = btcQuote,
                isStale = false,
                isOffline = false
            ),
            MarketAssetCatalog.IBOV_CODE to MarketQuoteUiState(
                assetCode = MarketAssetCatalog.IBOV_CODE,
                quote = ibovQuote,
                isStale = true,
                isOffline = false
            ),
            MarketAssetCatalog.MELI_CODE to MarketQuoteUiState(
                assetCode = MarketAssetCatalog.MELI_CODE,
                quote = meliQuote,
                isOffline = true
            ),
            MarketAssetCatalog.PETR4_CODE to MarketQuoteUiState(
                assetCode = MarketAssetCatalog.PETR4_CODE,
                quote = null,
                isOffline = false
            )
        )

        val favorites = listOf(
            MarketAssetCatalog.MELI_CODE,
            MarketAssetCatalog.BTC_BRL_CODE,
            MarketAssetCatalog.IBOV_CODE,
            MarketAssetCatalog.PETR4_CODE
        )
        val overview = MarketPresentationMapper.buildOverview(
            states = states,
            favoriteCodes = favorites
        )

        assertEquals(favorites, overview.watchlist.map { it.descriptor.code })
        assertTrue(overview.watchlist.all { it.isFavorite })

        val meliRow = overview.watchlist[0]
        assertEquals("Offline", meliRow.quoteStatusLabel)
        assertEquals("USD", meliRow.unitOrCurrencyLabel)
        assertEquals(MarketStatusSeverity.WARNING, meliRow.statusSeverity)

        val btcRow = overview.watchlist[1]
        assertEquals("Atualizada", btcRow.quoteStatusLabel)
        assertEquals("BRL", btcRow.unitOrCurrencyLabel)
        assertEquals(MarketStatusSeverity.OK, btcRow.statusSeverity)

        val ibovRow = overview.watchlist[2]
        assertEquals("Desatualizada", ibovRow.quoteStatusLabel)
        assertEquals("Pontos (BRL)", ibovRow.unitOrCurrencyLabel)
        assertEquals(MarketStatusSeverity.STALE, ibovRow.statusSeverity)

        val petrRow = overview.watchlist[3]
        assertEquals("Indisponível", petrRow.quoteStatusLabel)
        assertEquals(MarketStatusSeverity.EMPTY, petrRow.statusSeverity)

        val emptyOverview = MarketPresentationMapper.buildOverview(states = states, favoriteCodes = emptyList())
        assertTrue(emptyOverview.watchlist.isEmpty())
    }
}
