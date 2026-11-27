package com.nexus.app.data.market

import android.content.Context
import android.content.SharedPreferences
import com.nexus.app.domain.model.MarketQuoteCalculations
import com.nexus.app.domain.model.MarketQuoteSnapshot
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class PersistedMarketQuoteRecord(
    val lastValidQuote: MarketQuoteSnapshot? = null,
    val lastAttemptAtEpochMs: Long? = null,
    val lastAttemptError: String? = null
)

interface MarketQuoteStore {
    fun loadRecord(assetCode: String): PersistedMarketQuoteRecord?
    fun saveValidQuote(quote: MarketQuoteSnapshot)
    fun recordFailedAttempt(assetCode: String, attemptAtEpochMs: Long, errorMessage: String)
    fun clearAll() {}
}

/**
 * Cache local leve em SharedPreferences (JSON serializado por ativo) alinhado ao padrão de
 * NexusWidgetConfigStore, evitando alterações estruturais/migrações no Room.
 *
 * Invariante crítica: falhas de atualização NUNCA sobrescrevem [PersistedMarketQuoteRecord.lastValidQuote].
 */
class SharedPreferencesMarketQuoteStore(context: Context) : MarketQuoteStore {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    override fun loadRecord(assetCode: String): PersistedMarketQuoteRecord? {
        val raw = prefs.getString(keyFor(assetCode), null) ?: return null
        return runCatching {
            val decoded = json.decodeFromString<PersistedMarketQuoteRecord>(raw)
            val validQuote = decoded.lastValidQuote?.takeIf {
                MarketQuoteCalculations.isValidPositiveRate(it.price)
            }
            decoded.copy(lastValidQuote = validQuote)
        }.getOrNull()
    }

    override fun saveValidQuote(quote: MarketQuoteSnapshot) {
        if (!MarketQuoteCalculations.isValidPositiveRate(quote.price)) return
        val record = PersistedMarketQuoteRecord(
            lastValidQuote = quote,
            lastAttemptAtEpochMs = quote.fetchedAtEpochMs,
            lastAttemptError = null
        )
        prefs.edit()
            .putString(keyFor(quote.assetCode), json.encodeToString(record))
            .apply()
    }

    override fun recordFailedAttempt(assetCode: String, attemptAtEpochMs: Long, errorMessage: String) {
        val existing = loadRecord(assetCode)
        val updated = PersistedMarketQuoteRecord(
            lastValidQuote = existing?.lastValidQuote,
            lastAttemptAtEpochMs = attemptAtEpochMs,
            lastAttemptError = errorMessage
        )
        prefs.edit()
            .putString(keyFor(assetCode), json.encodeToString(updated))
            .apply()
    }

    override fun clearAll() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val PREFS_NAME = "nexus_market_quotes_cache"
        private fun keyFor(assetCode: String): String = "quote_record_$assetCode"
    }
}
