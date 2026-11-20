package com.nexus.app

import com.nexus.app.data.market.MarketAlertRepository
import com.nexus.app.data.market.MarketAlertStore
import com.nexus.app.domain.model.AssetCategory
import com.nexus.app.domain.model.MarketAlertCondition
import com.nexus.app.domain.model.MarketAlertRules
import com.nexus.app.domain.model.MarketAlertStatus
import com.nexus.app.domain.model.MarketAlertTriggerEvent
import com.nexus.app.domain.model.MarketAssetCatalog
import com.nexus.app.domain.model.MarketPriceAlert
import com.nexus.app.domain.model.MarketQuoteSnapshot
import com.nexus.app.domain.model.MarketQuoteUiState
import com.nexus.app.domain.model.QuoteModality
import com.nexus.app.domain.model.QuoteTrend
import com.nexus.app.ui.market.MarketPresentationMapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MarketAlertUnitTests {

    private class InMemoryAlertStore(initial: List<MarketPriceAlert> = emptyList()) : MarketAlertStore {
        var saved: List<MarketPriceAlert> = initial
        override fun loadAlerts(): List<MarketPriceAlert> = saved
        override fun saveAlerts(alerts: List<MarketPriceAlert>) {
            saved = alerts
        }
    }

    private fun quote(
        assetCode: String,
        category: AssetCategory,
        baseCurrency: String,
        quoteCurrency: String,
        price: Double,
        fetchedAtEpochMs: Long,
        sourceName: String = "AwesomeAPI"
    ): MarketQuoteSnapshot {
        return MarketQuoteSnapshot(
            assetCode = assetCode,
            category = category,
            baseCurrency = baseCurrency,
            quoteCurrency = quoteCurrency,
            modality = QuoteModality.SPOT,
            price = price,
            trend = QuoteTrend.FLAT,
            sourceName = sourceName,
            sourceTimestampEpochMs = fetchedAtEpochMs,
            fetchedAtEpochMs = fetchedAtEpochMs
        )
    }

    @Test
    fun `parseTargetInput handles pt-BR and standard formats and rejects invalid values`() {
        assertEquals(5.85, MarketAlertRules.parseTargetInput("5,85")!!, 0.0001)
        assertEquals(5.85, MarketAlertRules.parseTargetInput("5.85")!!, 0.0001)
        assertEquals(170_500.25, MarketAlertRules.parseTargetInput("170.500,25")!!, 0.0001)
        assertEquals(1_436.83, MarketAlertRules.parseTargetInput("US$ 1,436.83")!!, 0.0001)
        assertEquals(375_000.0, MarketAlertRules.parseTargetInput("375000")!!, 0.0001)

        assertNull(MarketAlertRules.parseTargetInput(""))
        assertNull(MarketAlertRules.parseTargetInput("0"))
        assertNull(MarketAlertRules.parseTargetInput("-10"))
        assertNull(MarketAlertRules.parseTargetInput("abc"))
    }

    @Test
    fun `alert triggers once on valid condition match and never sends duplicate notifications`() {
        val store = InMemoryAlertStore()
        var now = 1_795_000_000_000L
        val notifications = mutableListOf<MarketAlertTriggerEvent>()

        val repo = MarketAlertRepository(
            store = store,
            onTriggerNotification = { event ->
                notifications.add(event)
                true
            },
            clock = { now }
        )

        val initialQuote = quote(
            assetCode = MarketAssetCatalog.USD_BRL_CODE,
            category = AssetCategory.CURRENCY,
            baseCurrency = "USD",
            quoteCurrency = "BRL",
            price = 5.65,
            fetchedAtEpochMs = now - 1_000L
        )
        val created = repo.createAlert(
            assetCode = MarketAssetCatalog.USD_BRL_CODE,
            condition = MarketAlertCondition.PRICE_ABOVE,
            targetPrice = 5.75,
            isActive = true,
            currentQuote = initialQuote
        ).getOrThrow()

        assertEquals(MarketAlertStatus.WAITING, created.status)
        assertTrue(created.isActive)

        // Same timestamp quote or below-target quote does not trigger
        now += 10_000L
        val belowQuote = quote(
            assetCode = MarketAssetCatalog.USD_BRL_CODE,
            category = AssetCategory.CURRENCY,
            baseCurrency = "USD",
            quoteCurrency = "BRL",
            price = 5.70,
            fetchedAtEpochMs = now
        )
        assertTrue(repo.evaluateQuotes(mapOf(MarketAssetCatalog.USD_BRL_CODE to belowQuote)).isEmpty())
        assertEquals(0, notifications.size)

        // Mismatched currency quote is ignored safely
        val invalidCurrencyQuote = belowQuote.copy(quoteCurrency = "USD", price = 9.0)
        assertTrue(repo.evaluateQuotes(mapOf(MarketAssetCatalog.USD_BRL_CODE to invalidCurrencyQuote)).isEmpty())

        // Crossing above target triggers once
        now += 60_000L
        val crossingQuote = quote(
            assetCode = MarketAssetCatalog.USD_BRL_CODE,
            category = AssetCategory.CURRENCY,
            baseCurrency = "USD",
            quoteCurrency = "BRL",
            price = 5.78,
            fetchedAtEpochMs = now
        )
        val fired = repo.evaluateQuotes(mapOf(MarketAssetCatalog.USD_BRL_CODE to crossingQuote))
        assertEquals(1, fired.size)
        assertEquals(1, notifications.size)

        val storedAlert = repo.alerts.value.single()
        assertEquals(MarketAlertStatus.TRIGGERED, storedAlert.status)
        assertFalse(storedAlert.isActive)
        assertEquals(5.78, storedAlert.triggeredPrice!!, 0.0001)
        assertNotNull(storedAlert.triggeredAtEpochMs)

        // Subsequent higher quote must NOT duplicate notification
        now += 60_000L
        val higherQuote = quote(
            assetCode = MarketAssetCatalog.USD_BRL_CODE,
            category = AssetCategory.CURRENCY,
            baseCurrency = "USD",
            quoteCurrency = "BRL",
            price = 5.85,
            fetchedAtEpochMs = now
        )
        assertTrue(repo.evaluateQuotes(mapOf(MarketAssetCatalog.USD_BRL_CODE to higherQuote)).isEmpty())
        assertEquals(1, notifications.size)

        // Simulating app restart preserves TRIGGERED state
        val reloaded = MarketAlertRepository(store = store, clock = { now })
        assertEquals(MarketAlertStatus.TRIGGERED, reloaded.alerts.value.single().status)
        assertFalse(reloaded.alerts.value.single().isActive)
    }

    @Test
    fun `reactivating triggered alert while price remains on target side requires return to opposite side before re-triggering`() {
        val store = InMemoryAlertStore()
        var now = 1_795_100_000_000L
        val notifications = mutableListOf<MarketAlertTriggerEvent>()

        val repo = MarketAlertRepository(
            store = store,
            onTriggerNotification = {
                notifications.add(it)
                true
            },
            clock = { now }
        )

        val alert = repo.createAlert(
            assetCode = MarketAssetCatalog.MELI_CODE,
            condition = MarketAlertCondition.PRICE_BELOW,
            targetPrice = 1_450.0,
            isActive = true,
            currentQuote = null
        ).getOrThrow()

        // First trigger at US$ 1,430.00
        now += 10_000L
        val firstDip = quote(
            assetCode = MarketAssetCatalog.MELI_CODE,
            category = AssetCategory.STOCK,
            baseCurrency = "MELI",
            quoteCurrency = "USD",
            price = 1_430.0,
            fetchedAtEpochMs = now,
            sourceName = "Nasdaq"
        )
        assertEquals(1, repo.evaluateQuotes(mapOf(MarketAssetCatalog.MELI_CODE to firstDip)).size)

        // User reactivates while MELI is still at 1,430 (< 1,450)
        now += 10_000L
        val reactivated = repo.setAlertActive(alert.id, active = true, currentQuote = firstDip).getOrThrow()
        assertEquals(MarketAlertStatus.WAITING, reactivated.status)
        assertTrue(reactivated.isActive)
        assertTrue(reactivated.awaitingConditionReset)

        // Still below target (1,420) -> must NOT fire again spuriously
        now += 30_000L
        val stillBelow = quote(
            assetCode = MarketAssetCatalog.MELI_CODE,
            category = AssetCategory.STOCK,
            baseCurrency = "MELI",
            quoteCurrency = "USD",
            price = 1_420.0,
            fetchedAtEpochMs = now,
            sourceName = "Nasdaq"
        )
        assertTrue(repo.evaluateQuotes(mapOf(MarketAssetCatalog.MELI_CODE to stillBelow)).isEmpty())
        assertTrue(repo.alerts.value.single().awaitingConditionReset)

        // Price recovers above target (1,460) -> clears awaitingConditionReset without firing
        now += 30_000L
        val recovered = quote(
            assetCode = MarketAssetCatalog.MELI_CODE,
            category = AssetCategory.STOCK,
            baseCurrency = "MELI",
            quoteCurrency = "USD",
            price = 1_460.0,
            fetchedAtEpochMs = now,
            sourceName = "Nasdaq"
        )
        assertTrue(repo.evaluateQuotes(mapOf(MarketAssetCatalog.MELI_CODE to recovered)).isEmpty())
        assertFalse(repo.alerts.value.single().awaitingConditionReset)
        assertEquals(MarketAlertStatus.WAITING, repo.alerts.value.single().status)

        // Price crosses below 1,450 again -> now triggers legitimately!
        now += 30_000L
        val secondDip = quote(
            assetCode = MarketAssetCatalog.MELI_CODE,
            category = AssetCategory.STOCK,
            baseCurrency = "MELI",
            quoteCurrency = "USD",
            price = 1_445.0,
            fetchedAtEpochMs = now,
            sourceName = "Nasdaq"
        )
        assertEquals(1, repo.evaluateQuotes(mapOf(MarketAssetCatalog.MELI_CODE to secondDip)).size)
        assertEquals(2, notifications.size)
        assertEquals(MarketAlertStatus.TRIGGERED, repo.alerts.value.single().status)
    }

    @Test
    fun `paused alerts do not trigger and editing or deleting updates persisted list`() {
        val store = InMemoryAlertStore()
        var now = 1_795_200_000_000L
        val repo = MarketAlertRepository(store = store, clock = { now })

        val ibovAlert = repo.createAlert(
            assetCode = MarketAssetCatalog.IBOV_CODE,
            condition = MarketAlertCondition.PRICE_ABOVE,
            targetPrice = 175_000.0,
            isActive = false
        ).getOrThrow()
        assertEquals(MarketAlertStatus.PAUSED, ibovAlert.status)

        now += 10_000L
        val highIbov = quote(
            assetCode = MarketAssetCatalog.IBOV_CODE,
            category = AssetCategory.INDEX,
            baseCurrency = "IBOV",
            quoteCurrency = "BRL",
            price = 180_000.0,
            fetchedAtEpochMs = now,
            sourceName = "B3"
        )
        assertTrue(repo.evaluateQuotes(mapOf(MarketAssetCatalog.IBOV_CODE to highIbov)).isEmpty())

        // Editing activates with new target
        val updated = repo.updateAlert(
            alertId = ibovAlert.id,
            condition = MarketAlertCondition.PRICE_ABOVE,
            targetPrice = 185_000.0,
            isActive = true,
            currentQuote = highIbov
        ).getOrThrow()
        assertEquals(MarketAlertStatus.WAITING, updated.status)
        assertEquals(185_000.0, updated.targetPrice, 0.001)

        assertTrue(repo.deleteAlert(ibovAlert.id))
        assertTrue(repo.alerts.value.isEmpty())
    }

    @Test
    fun `buildAlertRows formats values strictly in each asset original currency or points unit`() {
        val alerts = listOf(
            MarketPriceAlert(
                id = "a1",
                assetCode = MarketAssetCatalog.USD_BRL_CODE,
                condition = MarketAlertCondition.PRICE_ABOVE,
                targetPrice = 5.80,
                isActive = true,
                status = MarketAlertStatus.WAITING,
                createdAtEpochMs = 1_795_000_000_000L
            ),
            MarketPriceAlert(
                id = "a2",
                assetCode = MarketAssetCatalog.SPX_CODE,
                condition = MarketAlertCondition.PRICE_BELOW,
                targetPrice = 5_800.0,
                isActive = false,
                status = MarketAlertStatus.TRIGGERED,
                createdAtEpochMs = 1_795_000_000_000L,
                triggeredAtEpochMs = 1_795_000_500_000L,
                triggeredPrice = 5_790.25
            ),
            MarketPriceAlert(
                id = "a3",
                assetCode = MarketAssetCatalog.MELI_CODE,
                condition = MarketAlertCondition.PRICE_ABOVE,
                targetPrice = 1_500.0,
                isActive = false,
                status = MarketAlertStatus.PAUSED,
                createdAtEpochMs = 1_795_000_000_000L
            )
        )
        val states = mapOf(
            MarketAssetCatalog.USD_BRL_CODE to MarketQuoteUiState(
                assetCode = MarketAssetCatalog.USD_BRL_CODE,
                quote = quote(MarketAssetCatalog.USD_BRL_CODE, AssetCategory.CURRENCY, "USD", "BRL", 5.75, 1_795_000_100_000L)
            )
        )

        val rows = MarketPresentationMapper.buildAlertRows(alerts, states)
        assertEquals(3, rows.size)

        assertEquals("BRL", rows[0].unitOrCurrencyLabel)
        assertTrue(rows[0].conditionSummary.contains("R$"))
        assertEquals("Aguardando", rows[0].statusLabel)

        assertEquals("Pontos (USD)", rows[1].unitOrCurrencyLabel)
        assertTrue(rows[1].conditionSummary.contains("pts"))
        assertTrue(rows[1].triggeredSummary?.contains("pts") == true)

        assertEquals("USD", rows[2].unitOrCurrencyLabel)
        assertTrue(rows[2].conditionSummary.contains("US$"))
        assertEquals("Pausado", rows[2].statusLabel)
    }
}
