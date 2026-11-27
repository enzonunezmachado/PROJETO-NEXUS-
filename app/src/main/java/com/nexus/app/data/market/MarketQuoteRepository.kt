package com.nexus.app.data.market

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.nexus.app.core.DateTimeUtils
import com.nexus.app.domain.model.AssetCategory
import com.nexus.app.domain.model.MarketAssetCatalog
import com.nexus.app.domain.model.MarketQuoteCalculations
import com.nexus.app.domain.model.MarketQuoteSnapshot
import com.nexus.app.domain.model.MarketQuoteUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

class MarketQuoteRepository(
    private val remoteDataSource: MarketQuoteRemoteDataSource,
    private val store: MarketQuoteStore,
    private val isNetworkAvailable: () -> Boolean,
    private val clock: () -> Long = DateTimeUtils::nowEpochMillis,
    private val supportedAssets: List<MarketAssetCatalog.AssetDescriptor> = MarketAssetCatalog.SUPPORTED_ASSETS,
    private val onValidQuotesSaved: ((Map<String, MarketQuoteSnapshot>) -> Unit)? = null
) {
    private val assetMutexes = ConcurrentHashMap<String, Mutex>()
    private val stateWriteLock = Any()

    private val _quoteStates = MutableStateFlow(buildInitialStatesFromStore())
    val quoteStates: StateFlow<Map<String, MarketQuoteUiState>> = _quoteStates.asStateFlow()

    private val _usdBrlState = MutableStateFlow(
        _quoteStates.value[MarketAssetCatalog.USD_BRL_CODE]
            ?: MarketQuoteUiState(assetCode = MarketAssetCatalog.USD_BRL_CODE)
    )
    val usdBrlState: StateFlow<MarketQuoteUiState> = _usdBrlState.asStateFlow()

    private fun mutexFor(assetCode: String): Mutex =
        assetMutexes.computeIfAbsent(assetCode) { Mutex() }

    fun refreshUiMetadata(): Map<String, MarketQuoteUiState> {
        val now = clock()
        val current = _quoteStates.value
        val updated = supportedAssets.associate { asset ->
            val wasRefreshing = current[asset.code]?.isRefreshing == true
            asset.code to buildStateFromStore(
                assetCode = asset.code,
                isRefreshing = wasRefreshing,
                nowEpochMs = now
            )
        }
        publishStates(updated)
        return updated
    }

    fun clearAllLocalData() {
        store.clearAll()
        publishStates(buildInitialStatesFromStore())
    }

    suspend fun refreshIfStale(): Map<String, MarketQuoteUiState> = refreshAll(force = false)

    suspend fun refreshUsdBrl(force: Boolean = false): MarketQuoteUiState =
        refreshAsset(MarketAssetCatalog.USD_BRL_CODE, force = force)

    suspend fun refreshAsset(assetCode: String, force: Boolean = false): MarketQuoteUiState {
        val asset = MarketAssetCatalog.findByCode(assetCode)
            ?: supportedAssets.firstOrNull { it.code == assetCode }
            ?: return MarketQuoteUiState(
                assetCode = assetCode,
                lastRefreshFailed = true,
                statusMessage = "Ativo não suportado: $assetCode"
            )

        return mutexFor(asset.code).withLock {
            val now = clock()
            val stored = store.loadRecord(asset.code)
            val cachedQuote = stored?.lastValidQuote

            if (!force && shouldSkipAutoRefresh(stored, cachedQuote, now)) {
                val currentState = buildStateFromStore(asset.code, isRefreshing = false, nowEpochMs = now)
                updateSingleState(asset.code, currentState)
                return@withLock currentState
            }

            if (!isNetworkAvailable()) {
                val offlineState = buildOfflineState(
                    assetCode = asset.code,
                    cachedQuote = cachedQuote,
                    stored = stored,
                    nowEpochMs = now,
                    markFailure = force
                )
                updateSingleState(asset.code, offlineState)
                return@withLock offlineState
            }

            val current = _quoteStates.value[asset.code] ?: buildStateFromStore(asset.code, nowEpochMs = now)
            updateSingleState(
                asset.code,
                current.copy(isRefreshing = true, isOffline = false, statusMessage = null)
            )

            val result = remoteDataSource.fetchQuote(asset)
            val finalState = applyFetchResult(asset.code, cachedQuote, result, now)
            updateSingleState(asset.code, finalState)
            val savedQuote = finalState.quote
            if (!finalState.lastRefreshFailed && !finalState.isOffline && !finalState.isStale && savedQuote != null) {
                onValidQuotesSaved?.invoke(mapOf(asset.code to savedQuote))
            }
            finalState
        }
    }

    suspend fun refreshAll(force: Boolean = false): Map<String, MarketQuoteUiState> {
        val now = clock()
        val dueAssets = mutableListOf<MarketAssetCatalog.AssetDescriptor>()
        val skippedOrOfflineUpdates = mutableMapOf<String, MarketQuoteUiState>()

        val online = isNetworkAvailable()

        for (asset in supportedAssets) {
            val stored = store.loadRecord(asset.code)
            val cachedQuote = stored?.lastValidQuote

            if (!force && shouldSkipAutoRefresh(stored, cachedQuote, now)) {
                skippedOrOfflineUpdates[asset.code] = buildStateFromStore(
                    assetCode = asset.code,
                    isRefreshing = false,
                    nowEpochMs = now
                )
                continue
            }

            if (!online) {
                skippedOrOfflineUpdates[asset.code] = buildOfflineState(
                    assetCode = asset.code,
                    cachedQuote = cachedQuote,
                    stored = stored,
                    nowEpochMs = now,
                    markFailure = force
                )
                continue
            }

            dueAssets.add(asset)
        }

        if (skippedOrOfflineUpdates.isNotEmpty()) {
            updateBatchStates(skippedOrOfflineUpdates)
        }

        if (dueAssets.isEmpty()) {
            return _quoteStates.value
        }

        val lockedDueAssets = dueAssets.filter { mutexFor(it.code).tryLock() }
        if (lockedDueAssets.isEmpty()) {
            return _quoteStates.value
        }

        try {
            val refreshingUpdates = lockedDueAssets.associate { asset ->
                val base = _quoteStates.value[asset.code] ?: buildStateFromStore(asset.code, nowEpochMs = now)
                asset.code to base.copy(isRefreshing = true, isOffline = false, statusMessage = null)
            }
            updateBatchStates(refreshingUpdates)

            val results = remoteDataSource.fetchQuotes(lockedDueAssets)
            val finishedAt = clock()
            val savedQuotes = mutableMapOf<String, MarketQuoteSnapshot>()
            val resolvedUpdates = lockedDueAssets.associate { asset ->
                val cachedQuote = store.loadRecord(asset.code)?.lastValidQuote
                val fetchResult = results[asset.code] ?: RemoteQuoteFetchResult.Failure(
                    reason = FailureReason.INVALID_RESPONSE,
                    detail = "Sem resposta para ${asset.code}"
                )
                val resolvedState = applyFetchResult(asset.code, cachedQuote, fetchResult, finishedAt)
                val freshQuote = resolvedState.quote
                if (!resolvedState.lastRefreshFailed && !resolvedState.isOffline && !resolvedState.isStale && freshQuote != null) {
                    savedQuotes[asset.code] = freshQuote
                }
                asset.code to resolvedState
            }
            updateBatchStates(resolvedUpdates)
            if (savedQuotes.isNotEmpty()) {
                onValidQuotesSaved?.invoke(savedQuotes)
            }
        } finally {
            lockedDueAssets.forEach { asset ->
                runCatching { mutexFor(asset.code).unlock() }
            }
        }

        return _quoteStates.value
    }

    private fun applyFetchResult(
        assetCode: String,
        cachedQuote: MarketQuoteSnapshot?,
        fetchResult: RemoteQuoteFetchResult,
        attemptAtEpochMs: Long
    ): MarketQuoteUiState {
        return when (fetchResult) {
            is RemoteQuoteFetchResult.Success -> {
                val quote = fetchResult.quote
                if (MarketQuoteCalculations.isValidPositiveRate(quote.price)) {
                    store.saveValidQuote(quote)
                    MarketQuoteUiState(
                        assetCode = assetCode,
                        quote = quote,
                        isRefreshing = false,
                        isStale = false,
                        isOffline = false,
                        lastRefreshFailed = false,
                        statusMessage = null,
                        lastAttemptAtEpochMs = quote.fetchedAtEpochMs
                    )
                } else {
                    handleFailure(
                        assetCode = assetCode,
                        cachedQuote = cachedQuote,
                        attemptAt = attemptAtEpochMs,
                        message = "Cotação recebida inválida — cache anterior preservado"
                    )
                }
            }

            is RemoteQuoteFetchResult.Failure -> {
                handleFailure(
                    assetCode = assetCode,
                    cachedQuote = cachedQuote,
                    attemptAt = attemptAtEpochMs,
                    message = toUserFacingErrorMessage(fetchResult, hasCachedQuote = cachedQuote != null)
                )
            }
        }
    }

    private fun handleFailure(
        assetCode: String,
        cachedQuote: MarketQuoteSnapshot?,
        attemptAt: Long,
        message: String
    ): MarketQuoteUiState {
        store.recordFailedAttempt(
            assetCode = assetCode,
            attemptAtEpochMs = attemptAt,
            errorMessage = message
        )
        val offlineFailure = !isNetworkAvailable()
        return MarketQuoteUiState(
            assetCode = assetCode,
            quote = cachedQuote,
            isRefreshing = false,
            isStale = cachedQuote?.let { MarketQuoteCalculations.isQuoteStale(it, attemptAt) } ?: false,
            isOffline = offlineFailure,
            lastRefreshFailed = true,
            statusMessage = message,
            lastAttemptAtEpochMs = attemptAt
        )
    }

    private fun buildOfflineState(
        assetCode: String,
        cachedQuote: MarketQuoteSnapshot?,
        stored: PersistedMarketQuoteRecord?,
        nowEpochMs: Long,
        markFailure: Boolean
    ): MarketQuoteUiState {
        val offlineMsg = if (cachedQuote != null) {
            "Sem conexão — exibindo última cotação válida"
        } else {
            "Sem conexão com a internet"
        }
        return MarketQuoteUiState(
            assetCode = assetCode,
            quote = cachedQuote,
            isRefreshing = false,
            isStale = cachedQuote?.let { MarketQuoteCalculations.isQuoteStale(it, nowEpochMs) } ?: false,
            isOffline = true,
            lastRefreshFailed = markFailure,
            statusMessage = offlineMsg,
            lastAttemptAtEpochMs = stored?.lastAttemptAtEpochMs
        )
    }

    private fun shouldSkipAutoRefresh(
        stored: PersistedMarketQuoteRecord?,
        cachedQuote: MarketQuoteSnapshot?,
        nowEpochMs: Long
    ): Boolean {
        if (cachedQuote != null) {
            val ageMs = nowEpochMs - cachedQuote.fetchedAtEpochMs
            if (ageMs in 0 until MarketQuoteCalculations.AUTO_REFRESH_COOLDOWN_MS) {
                return true
            }
        }
        val lastAttempt = stored?.lastAttemptAtEpochMs
        if (lastAttempt != null && stored.lastAttemptError != null) {
            val sinceLastFailure = nowEpochMs - lastAttempt
            if (sinceLastFailure in 0 until MarketQuoteCalculations.FAILURE_RETRY_COOLDOWN_MS) {
                return true
            }
        }
        return false
    }

    private fun buildInitialStatesFromStore(): Map<String, MarketQuoteUiState> {
        val now = clock()
        return supportedAssets.associate { asset ->
            asset.code to buildStateFromStore(asset.code, isRefreshing = false, nowEpochMs = now)
        }
    }

    private fun buildStateFromStore(
        assetCode: String,
        isRefreshing: Boolean = false,
        nowEpochMs: Long = clock()
    ): MarketQuoteUiState {
        val stored = store.loadRecord(assetCode)
        val cachedQuote = stored?.lastValidQuote
        val online = isNetworkAvailable()
        return MarketQuoteUiState(
            assetCode = assetCode,
            quote = cachedQuote,
            isRefreshing = isRefreshing,
            isStale = cachedQuote?.let { MarketQuoteCalculations.isQuoteStale(it, nowEpochMs) } ?: false,
            isOffline = !online,
            lastRefreshFailed = !stored?.lastAttemptError.isNullOrBlank(),
            statusMessage = when {
                !online && cachedQuote != null -> "Sem conexão — exibindo última cotação válida"
                !online -> "Sem conexão com a internet"
                else -> stored?.lastAttemptError
            },
            lastAttemptAtEpochMs = stored?.lastAttemptAtEpochMs
        )
    }

    private fun updateSingleState(assetCode: String, state: MarketQuoteUiState) {
        synchronized(stateWriteLock) {
            val next = _quoteStates.value.toMutableMap()
            next[assetCode] = state
            publishStatesLocked(next)
        }
    }

    private fun updateBatchStates(updates: Map<String, MarketQuoteUiState>) {
        synchronized(stateWriteLock) {
            val next = _quoteStates.value.toMutableMap()
            next.putAll(updates)
            publishStatesLocked(next)
        }
    }

    private fun publishStates(states: Map<String, MarketQuoteUiState>) {
        synchronized(stateWriteLock) {
            publishStatesLocked(states)
        }
    }

    private fun publishStatesLocked(states: Map<String, MarketQuoteUiState>) {
        _quoteStates.value = states
        states[MarketAssetCatalog.USD_BRL_CODE]?.let {
            _usdBrlState.value = it
        }
    }

    private fun toUserFacingErrorMessage(
        failure: RemoteQuoteFetchResult.Failure,
        hasCachedQuote: Boolean
    ): String {
        val base = when (failure.reason) {
            FailureReason.NETWORK_UNAVAILABLE -> "Sem conexão com o serviço de cotações"
            FailureReason.TIMEOUT -> "Tempo esgotado ao atualizar cotação"
            FailureReason.RATE_LIMITED -> "Serviço temporariamente limitado"
            FailureReason.HTTP_ERROR -> "Serviço de cotações indisponível (${failure.httpStatusCode ?: "HTTP"})"
            FailureReason.INVALID_RESPONSE -> "Dados de cotação inválidos"
            FailureReason.UNSUPPORTED_ASSET -> "Ativo não suportado"
        }
        return if (hasCachedQuote) "$base — exibindo cache" else base
    }

    companion object {
        fun createDefault(
            context: Context,
            onValidQuotesSaved: ((Map<String, MarketQuoteSnapshot>) -> Unit)? = null
        ): MarketQuoteRepository {
            val appContext = context.applicationContext
            val transport = HttpsUrlConnectionTransport()
            val currencyRoute = CompositeMarketQuoteRemoteDataSource(
                providers = listOf(
                    AwesomeApiCurrencyDataSource(transport),
                    BcbOlindaPtaxDataSource(transport)
                )
            )
            val cryptoRoute = CompositeMarketQuoteRemoteDataSource(
                providers = listOf(
                    CoinGeckoCryptoDataSource(transport),
                    CoinPaprikaCryptoDataSource(transport)
                )
            )
            val indexRoute = CompositeMarketQuoteRemoteDataSource(
                providers = listOf(
                    B3QuoteDataSource(
                        transport = transport,
                        supportedCodes = setOf(MarketAssetCatalog.IBOV_CODE)
                    ),
                    CnbcMarketDataSource(
                        transport = transport,
                        supportedCodes = setOf(
                            MarketAssetCatalog.SPX_CODE,
                            MarketAssetCatalog.IXIC_CODE,
                            MarketAssetCatalog.IBOV_CODE
                        )
                    ),
                    NasdaqPublicDataSource(transport)
                )
            )
            val stockRoute = CompositeMarketQuoteRemoteDataSource(
                providers = listOf(
                    BrapiB3StockDataSource(transport),
                    CnbcMarketDataSource(
                        transport = transport,
                        supportedCodes = setOf(MarketAssetCatalog.MELI_CODE)
                    ),
                    B3QuoteDataSource(
                        transport = transport,
                        supportedCodes = setOf(
                            MarketAssetCatalog.PETR4_CODE,
                            MarketAssetCatalog.VALE3_CODE,
                            MarketAssetCatalog.ITUB4_CODE
                        )
                    ),
                    NasdaqPublicDataSource(transport)
                )
            )
            val remote = CategoryRoutedMarketQuoteRemoteDataSource(
                routes = mapOf(
                    AssetCategory.CURRENCY to currencyRoute,
                    AssetCategory.CRYPTO to cryptoRoute,
                    AssetCategory.INDEX to indexRoute,
                    AssetCategory.STOCK to stockRoute
                )
            )
            return MarketQuoteRepository(
                remoteDataSource = remote,
                store = SharedPreferencesMarketQuoteStore(appContext),
                isNetworkAvailable = { checkNetworkConnected(appContext) },
                onValidQuotesSaved = onValidQuotesSaved
            )
        }

        private fun checkNetworkConnected(context: Context): Boolean {
            return runCatching {
                val cm = context.getSystemService(ConnectivityManager::class.java) ?: return@runCatching true
                val activeNetwork = cm.activeNetwork ?: return@runCatching false
                val caps = cm.getNetworkCapabilities(activeNetwork) ?: return@runCatching false
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            }.getOrDefault(false)
        }
    }
}
