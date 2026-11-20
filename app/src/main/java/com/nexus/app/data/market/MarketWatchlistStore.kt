package com.nexus.app.data.market

import android.content.Context
import android.content.SharedPreferences
import com.nexus.app.domain.model.MarketAssetCatalog
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

interface MarketWatchlistStore {
    fun loadFavoriteCodes(): List<String>
    fun saveFavoriteCodes(codes: List<String>)
}

/**
 * Persistência local dedicada e leve para a lista de acompanhamento do Mercado.
 * Armazena apenas a lista ordenada e deduplicada de códigos de ativos suportados,
 * separada do cache de cotações e do histórico em arquivos.
 */
class SharedPreferencesMarketWatchlistStore(context: Context) : MarketWatchlistStore {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    override fun loadFavoriteCodes(): List<String> {
        val raw = prefs.getString(KEY_FAVORITE_CODES, null) ?: return emptyList()
        return runCatching {
            sanitize(json.decodeFromString<List<String>>(raw))
        }.getOrDefault(emptyList())
    }

    override fun saveFavoriteCodes(codes: List<String>) {
        val sanitized = sanitize(codes)
        prefs.edit()
            .putString(KEY_FAVORITE_CODES, json.encodeToString(sanitized))
            .apply()
    }

    private fun sanitize(codes: List<String>): List<String> =
        codes
            .map { it.trim() }
            .filter { MarketAssetCatalog.findByCode(it) != null }
            .distinct()

    companion object {
        private const val PREFS_NAME = "nexus_market_watchlist"
        private const val KEY_FAVORITE_CODES = "favorite_asset_codes"
    }
}
