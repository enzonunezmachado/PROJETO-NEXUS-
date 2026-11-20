package com.nexus.app.data.market

import android.content.Context
import com.nexus.app.domain.model.MarketAssetCatalog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Gerencia a lista local de favoritos (Acompanhamento) do módulo Mercado.
 * Não executa requisições remotas e não armazena dados derivados de cotação.
 */
class MarketWatchlistRepository(
    private val store: MarketWatchlistStore
) {
    private val writeLock = Any()

    private val _favoriteCodes = MutableStateFlow(sanitize(store.loadFavoriteCodes()))
    val favoriteCodes: StateFlow<List<String>> = _favoriteCodes.asStateFlow()

    fun isFavorite(assetCode: String): Boolean = assetCode in _favoriteCodes.value

    /**
     * Alterna o estado de favorito de [assetCode].
     * Retorna `true` se o ativo passou a estar favoritado, ou `false` caso contrário.
     */
    fun toggleFavorite(assetCode: String): Boolean {
        if (MarketAssetCatalog.findByCode(assetCode) == null) return false
        synchronized(writeLock) {
            val current = _favoriteCodes.value
            val isNowFavorite: Boolean
            val next = if (assetCode in current) {
                isNowFavorite = false
                current.filterNot { it == assetCode }
            } else {
                isNowFavorite = true
                sanitize(current + assetCode)
            }
            store.saveFavoriteCodes(next)
            _favoriteCodes.value = next
            return isNowFavorite
        }
    }

    fun addFavorite(assetCode: String): Boolean {
        if (MarketAssetCatalog.findByCode(assetCode) == null) return false
        synchronized(writeLock) {
            val current = _favoriteCodes.value
            if (assetCode in current) return false
            val next = sanitize(current + assetCode)
            store.saveFavoriteCodes(next)
            _favoriteCodes.value = next
            return true
        }
    }

    fun removeFavorite(assetCode: String): Boolean {
        synchronized(writeLock) {
            val current = _favoriteCodes.value
            if (assetCode !in current) return false
            val next = current.filterNot { it == assetCode }
            store.saveFavoriteCodes(next)
            _favoriteCodes.value = next
            return true
        }
    }

    fun replaceAllFavorites(codes: List<String>): List<String> {
        val next = sanitize(codes)
        synchronized(writeLock) {
            store.saveFavoriteCodes(next)
            _favoriteCodes.value = next
        }
        return next
    }

    private fun sanitize(codes: List<String>): List<String> =
        codes
            .map { it.trim() }
            .filter { MarketAssetCatalog.findByCode(it) != null }
            .distinct()

    companion object {
        fun createDefault(context: Context): MarketWatchlistRepository {
            return MarketWatchlistRepository(
                store = SharedPreferencesMarketWatchlistStore(context.applicationContext)
            )
        }
    }
}
