package com.nexus.app

import com.nexus.app.data.backup.BackupManager
import com.nexus.app.data.backup.MarketBackupConfigDto
import com.nexus.app.data.backup.MarketPriceAlertBackupDto
import com.nexus.app.data.backup.NexusBackupPayload
import com.nexus.app.data.market.MarketAlertRepository
import com.nexus.app.data.market.MarketAlertStore
import com.nexus.app.data.market.MarketWatchlistRepository
import com.nexus.app.data.market.MarketWatchlistStore
import com.nexus.app.domain.model.AssetCategory
import com.nexus.app.domain.model.MarketAlertCondition
import com.nexus.app.domain.model.MarketAlertStatus
import com.nexus.app.domain.model.MarketAlertTriggerEvent
import com.nexus.app.domain.model.MarketAssetCatalog
import com.nexus.app.domain.model.MarketPriceAlert
import com.nexus.app.domain.model.MarketQuoteSnapshot
import com.nexus.app.domain.model.QuoteModality
import com.nexus.app.domain.model.QuoteTrend
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MarketBackupIntegrationUnitTests {

    private class InMemoryWatchlistStore(initial: List<String> = emptyList()) : MarketWatchlistStore {
        var saved: List<String> = initial
        override fun loadFavoriteCodes(): List<String> = saved
        override fun saveFavoriteCodes(codes: List<String>) {
            saved = codes
        }
    }

    private class InMemoryAlertStore(initial: List<MarketPriceAlert> = emptyList()) : MarketAlertStore {
        var saved: List<MarketPriceAlert> = initial
        override fun loadAlerts(): List<MarketPriceAlert> = saved
        override fun saveAlerts(alerts: List<MarketPriceAlert>) {
            saved = alerts
        }
    }

    private val tolerantJson = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
        isLenient = true
    }

    @Test
    fun `backup v7 payload round-trips marketConfig preserving favorites and alerts metadata`() {
        val payload = NexusBackupPayload(
            formatVersion = NexusBackupPayload.CURRENT_FORMAT_VERSION,
            appVersion = "5.3.0",
            exportedAt = "2026-11-19T10:00:00-03:00",
            databaseVersion = 6,
            marketConfig = MarketBackupConfigDto(
                configVersion = MarketBackupConfigDto.CURRENT_MARKET_CONFIG_VERSION,
                favoriteAssetCodes = listOf(
                    MarketAssetCatalog.BTC_BRL_CODE,
                    MarketAssetCatalog.USD_BRL_CODE,
                    MarketAssetCatalog.MELI_CODE
                ),
                priceAlerts = listOf(
                    MarketPriceAlertBackupDto(
                        id = "alert-wait-1",
                        assetCode = MarketAssetCatalog.USD_BRL_CODE,
                        condition = MarketAlertCondition.PRICE_ABOVE.name,
                        targetPrice = 5.90,
                        currencyOrUnit = "BRL",
                        isActive = true,
                        status = MarketAlertStatus.WAITING.name,
                        createdAtEpochMs = 1_795_000_000_000L,
                        updatedAtEpochMs = 1_795_000_100_000L,
                        awaitingConditionReset = true,
                        lastEvaluatedQuoteFetchedAtEpochMs = 1_795_000_090_000L
                    ),
                    MarketPriceAlertBackupDto(
                        id = "alert-trig-2",
                        assetCode = MarketAssetCatalog.IBOV_CODE,
                        condition = MarketAlertCondition.PRICE_BELOW.name,
                        targetPrice = 165_000.0,
                        currencyOrUnit = "Pontos (BRL)",
                        isActive = false,
                        status = MarketAlertStatus.TRIGGERED.name,
                        createdAtEpochMs = 1_794_900_000_000L,
                        updatedAtEpochMs = 1_795_000_200_000L,
                        triggeredAtEpochMs = 1_795_000_200_000L,
                        triggeredPrice = 164_500.0,
                        triggeredQuoteSource = "B3 (Cotação Pública)",
                        triggeredQuoteTimestampEpochMs = 1_795_000_190_000L,
                        awaitingConditionReset = false,
                        lastEvaluatedQuoteFetchedAtEpochMs = 1_795_000_195_000L
                    )
                )
            )
        )

        assertEquals(7, NexusBackupPayload.CURRENT_FORMAT_VERSION)
        val encoded = tolerantJson.encodeToString(payload)
        val decoded = tolerantJson.decodeFromString<NexusBackupPayload>(encoded)

        val market = decoded.marketConfig!!
        assertEquals(1, market.configVersion)
        assertEquals(
            listOf(
                MarketAssetCatalog.BTC_BRL_CODE,
                MarketAssetCatalog.USD_BRL_CODE,
                MarketAssetCatalog.MELI_CODE
            ),
            market.favoriteAssetCodes
        )
        assertEquals(2, market.priceAlerts?.size)

        val restored = BackupManager.sanitizePriceAlerts(market.priceAlerts!!, fallbackNowEpochMs = 1_795_100_000_000L)
        assertEquals(2, restored.size)
        assertTrue(restored[0].awaitingConditionReset)
        assertEquals(1_795_000_090_000L, restored[0].lastEvaluatedQuoteFetchedAtEpochMs)
        assertEquals(MarketAlertStatus.TRIGGERED, restored[1].status)
        assertFalse(restored[1].isActive)
        assertEquals(164_500.0, restored[1].triggeredPrice!!, 0.001)
        assertEquals("B3 (Cotação Pública)", restored[1].triggeredQuoteSource)
    }

    @Test
    fun `legacy backup without marketConfig keeps marketConfig null so local data is not wiped`() {
        val legacyV6Json = """
            {
              "formatVersion": 6,
              "appVersion": "5.2.0",
              "exportedAt": "2026-11-17T09:00:00-03:00",
              "databaseVersion": 6,
              "tasks": []
            }
        """.trimIndent()

        val decodedLegacy = tolerantJson.decodeFromString<NexusBackupPayload>(legacyV6Json)
        assertNull(decodedLegacy.marketConfig)

        val explicitEmptyJson = """
            {
              "formatVersion": 7,
              "appVersion": "5.3.0",
              "exportedAt": "2026-11-19T09:00:00-03:00",
              "databaseVersion": 6,
              "marketConfig": {
                "configVersion": 1,
                "favoriteAssetCodes": [],
                "priceAlerts": []
              }
            }
        """.trimIndent()

        val decodedEmpty = tolerantJson.decodeFromString<NexusBackupPayload>(explicitEmptyJson)
        assertEquals(emptyList<String>(), decodedEmpty.marketConfig?.favoriteAssetCodes)
        assertEquals(emptyList<MarketPriceAlertBackupDto>(), decodedEmpty.marketConfig?.priceAlerts)
    }

    @Test
    fun `sanitizeFavoriteCodes and sanitizePriceAlerts reject invalid assets units targets and normalize inconsistent states`() {
        val sanitizedFavorites = BackupManager.sanitizeFavoriteCodes(
            listOf(
                " ${MarketAssetCatalog.MELI_CODE} ",
                "UNKNOWN-TICKER",
                MarketAssetCatalog.USD_BRL_CODE,
                MarketAssetCatalog.MELI_CODE
            )
        )
        assertEquals(listOf(MarketAssetCatalog.MELI_CODE, MarketAssetCatalog.USD_BRL_CODE), sanitizedFavorites)

        val dtos = listOf(
            // Nonexistent asset -> rejected
            MarketPriceAlertBackupDto(
                id = "bad-asset",
                assetCode = "FAKE-BRL",
                condition = MarketAlertCondition.PRICE_ABOVE.name,
                targetPrice = 10.0,
                currencyOrUnit = "BRL"
            ),
            // Wrong currency unit (USD declared for BRL stock PETR4) -> rejected
            MarketPriceAlertBackupDto(
                id = "bad-unit",
                assetCode = MarketAssetCatalog.PETR4_CODE,
                condition = MarketAlertCondition.PRICE_ABOVE.name,
                targetPrice = 40.0,
                currencyOrUnit = "USD"
            ),
            // Non-positive target -> rejected
            MarketPriceAlertBackupDto(
                id = "bad-price",
                assetCode = MarketAssetCatalog.USD_BRL_CODE,
                condition = MarketAlertCondition.PRICE_BELOW.name,
                targetPrice = -5.0,
                currencyOrUnit = "BRL"
            ),
            // Unknown condition -> rejected
            MarketPriceAlertBackupDto(
                id = "bad-cond",
                assetCode = MarketAssetCatalog.USD_BRL_CODE,
                condition = "PERCENT_UP",
                targetPrice = 5.5,
                currencyOrUnit = "BRL"
            ),
            // Inconsistent: WAITING + isActive=false -> safely normalized to PAUSED
            MarketPriceAlertBackupDto(
                id = "inconsistent-wait",
                assetCode = MarketAssetCatalog.MELI_CODE,
                condition = MarketAlertCondition.PRICE_ABOVE.name,
                targetPrice = 1_600.0,
                currencyOrUnit = "USD",
                isActive = false,
                status = MarketAlertStatus.WAITING.name,
                createdAtEpochMs = 1_795_000_000_000L,
                updatedAtEpochMs = 1_795_000_000_000L,
                awaitingConditionReset = true
            ),
            // Inconsistent: TRIGGERED + isActive=true -> safely normalized to isActive=false
            MarketPriceAlertBackupDto(
                id = "inconsistent-trig",
                assetCode = MarketAssetCatalog.BTC_BRL_CODE,
                condition = MarketAlertCondition.PRICE_ABOVE.name,
                targetPrice = 400_000.0,
                currencyOrUnit = "BRL",
                isActive = true,
                status = MarketAlertStatus.TRIGGERED.name,
                createdAtEpochMs = 1_795_000_000_000L,
                updatedAtEpochMs = 1_795_000_500_000L
            ),
            // Duplicate ID -> deduplicated
            MarketPriceAlertBackupDto(
                id = "inconsistent-trig",
                assetCode = MarketAssetCatalog.BTC_BRL_CODE,
                condition = MarketAlertCondition.PRICE_ABOVE.name,
                targetPrice = 410_000.0,
                currencyOrUnit = "BRL",
                isActive = true,
                status = MarketAlertStatus.WAITING.name
            )
        )

        val sanitized = BackupManager.sanitizePriceAlerts(dtos, fallbackNowEpochMs = 1_795_100_000_000L)
        assertEquals(2, sanitized.size)

        val meliAlert = sanitized.first { it.id == "inconsistent-wait" }
        assertEquals(MarketAlertStatus.PAUSED, meliAlert.status)
        assertFalse(meliAlert.isActive)
        assertFalse(meliAlert.awaitingConditionReset)

        val btcAlert = sanitized.first { it.id == "inconsistent-trig" }
        assertEquals(MarketAlertStatus.TRIGGERED, btcAlert.status)
        assertFalse(btcAlert.isActive)
        assertEquals(1_795_000_500_000L, btcAlert.triggeredAtEpochMs)
    }

    @Test
    fun `restoring via repositories neither fires notifications nor loses awaitingConditionReset protection`() {
        val watchlistStore = InMemoryWatchlistStore(listOf(MarketAssetCatalog.EUR_BRL_CODE))
        val alertStore = InMemoryAlertStore()
        val notifications = mutableListOf<MarketAlertTriggerEvent>()
        var now = 1_795_200_000_000L

        val watchlistRepo = MarketWatchlistRepository(watchlistStore)
        val alertRepo = MarketAlertRepository(
            store = alertStore,
            onTriggerNotification = { event ->
                notifications.add(event)
                true
            },
            clock = { now }
        )

        watchlistRepo.replaceAllFavorites(
            listOf(MarketAssetCatalog.USD_BRL_CODE, MarketAssetCatalog.PETR4_CODE)
        )
        assertEquals(
            listOf(MarketAssetCatalog.USD_BRL_CODE, MarketAssetCatalog.PETR4_CODE),
            watchlistRepo.favoriteCodes.value
        )

        val restoredAlerts = BackupManager.sanitizePriceAlerts(
            dtos = listOf(
                MarketPriceAlertBackupDto(
                    id = "trig-1",
                    assetCode = MarketAssetCatalog.USD_BRL_CODE,
                    condition = MarketAlertCondition.PRICE_ABOVE.name,
                    targetPrice = 5.50,
                    currencyOrUnit = "BRL",
                    isActive = false,
                    status = MarketAlertStatus.TRIGGERED.name,
                    createdAtEpochMs = 1_795_000_000_000L,
                    updatedAtEpochMs = 1_795_050_000_000L,
                    triggeredAtEpochMs = 1_795_050_000_000L,
                    triggeredPrice = 5.55
                ),
                MarketPriceAlertBackupDto(
                    id = "rearm-2",
                    assetCode = MarketAssetCatalog.PETR4_CODE,
                    condition = MarketAlertCondition.PRICE_ABOVE.name,
                    targetPrice = 38.0,
                    currencyOrUnit = "BRL",
                    isActive = true,
                    status = MarketAlertStatus.WAITING.name,
                    createdAtEpochMs = 1_795_000_000_000L,
                    updatedAtEpochMs = 1_795_060_000_000L,
                    awaitingConditionReset = true,
                    lastEvaluatedQuoteFetchedAtEpochMs = 1_795_060_000_000L
                )
            ),
            fallbackNowEpochMs = now
        )

        alertRepo.replaceAllAlerts(restoredAlerts)
        // Restoration itself must NEVER trigger notifications
        assertEquals(0, notifications.size)

        // When monitoring resumes with fresh quotes still satisfying both targets:
        // - trig-1 stays TRIGGERED (never fires again)
        // - rearm-2 keeps awaitingConditionReset=true and does NOT fire spuriously
        now += 60_000L
        val freshQuotes = mapOf(
            MarketAssetCatalog.USD_BRL_CODE to MarketQuoteSnapshot(
                assetCode = MarketAssetCatalog.USD_BRL_CODE,
                category = AssetCategory.CURRENCY,
                baseCurrency = "USD",
                quoteCurrency = "BRL",
                modality = QuoteModality.COMMERCIAL,
                price = 5.75,
                trend = QuoteTrend.UP,
                sourceName = "AwesomeAPI",
                sourceTimestampEpochMs = now,
                fetchedAtEpochMs = now
            ),
            MarketAssetCatalog.PETR4_CODE to MarketQuoteSnapshot(
                assetCode = MarketAssetCatalog.PETR4_CODE,
                category = AssetCategory.STOCK,
                baseCurrency = "PETR4",
                quoteCurrency = "BRL",
                modality = QuoteModality.EXCHANGE,
                price = 39.20,
                trend = QuoteTrend.UP,
                sourceName = "brapi.dev",
                sourceTimestampEpochMs = now,
                fetchedAtEpochMs = now
            )
        )
        assertTrue(alertRepo.evaluateQuotes(freshQuotes).isEmpty())
        assertEquals(0, notifications.size)
        assertTrue(alertRepo.alerts.value.first { it.id == "rearm-2" }.awaitingConditionReset)
    }
}
