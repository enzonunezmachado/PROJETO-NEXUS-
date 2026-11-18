package com.nexus.app.data.market

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.nexus.app.core.DateTimeUtils
import com.nexus.app.domain.model.MarketAssetCatalog
import com.nexus.app.domain.model.MarketQuoteCalculations
import com.nexus.app.domain.model.MarketQuoteSnapshot
import com.nexus.app.domain.model.MarketQuoteUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class MarketQuoteRepository(
    private val remoteDataSource: MarketQuoteRemoteDataSource,
    private val store: MarketQuoteStore,
    private val isNetworkAvailable: () -> Boolean,
    private val clock: () -> Long = DateTimeUtils::nowEpochMillis
) {
    private val refreshMutex = Mutex()

    private val _usdBrlState = MutableStateFlow(buildStateFromStore())
    val usdBrlState: StateFlow<MarketQuoteUiState> = _usdBrlState.asStateFlow()

    fun refreshUiMetadata(): MarketQuoteUiState {
        val updated = buildStateFromStore(isRefreshing = _usdBrlState.value.isRefreshing)
        _usdBrlState.value = updated
        return updated
    }

    suspend fun refreshIfStale(): MarketQuoteUiState = refreshUsdBrl(force = false)

    suspend fun refreshUsdBrl(force: Boolean = false): MarketQuoteUiState {
        return refreshMutex.withLock {
            val now = clock()
            val stored = store.loadRecord(MarketAssetCatalog.USD_BRL_CODE)
            val cachedQuote = stored?.lastValidQuote

            if (!force && shouldSkipAutoRefresh(stored, cachedQuote, now)) {
                val upToDateState = buildStateFromStore(isRefreshing = false, nowEpochMs = now)
                _usdBrlState.value = upToDateState
                return@withLock upToDateState
            }

            if (!isNetworkAvailable()) {
                val offlineMsg = if (cachedQuote != null) {
                    "Sem conexão — exibindo última cotação válida"
                } else {
                    "Sem conexão com a internet"
                }
                val offlineState = MarketQuoteUiState(
                    assetCode = MarketAssetCatalog.USD_BRL_CODE,
                    quote = cachedQuote,
                    isRefreshing = false,
                    isStale = cachedQuote?.let { MarketQuoteCalculations.isQuoteStale(it, now) } ?: false,
                    isOffline = true,
                    lastRefreshFailed = force,
                    statusMessage = offlineMsg,
                    lastAttemptAtEpochMs = stored?.lastAttemptAtEpochMs
                )
                _usdBrlState.value = offlineState
                return@withLock offlineState
            }

            _usdBrlState.value = _usdBrlState.value.copy(
                isRefreshing = true,
                isOffline = false,
                statusMessage = null
            )

            when (val fetchResult = remoteDataSource.fetchQuote(MarketAssetCatalog.USD_BRL)) {
                is RemoteQuoteFetchResult.Success -> {
                    val quote = fetchResult.quote
                    if (MarketQuoteCalculations.isValidPositiveRate(quote.price)) {
                        store.saveValidQuote(quote)
                        val freshState = MarketQuoteUiState(
                            assetCode = MarketAssetCatalog.USD_BRL_CODE,
                            quote = quote,
                            isRefreshing = false,
                            isStale = false,
                            isOffline = false,
                            lastRefreshFailed = false,
                            statusMessage = null,
                            lastAttemptAtEpochMs = quote.fetchedAtEpochMs
                        )
                        _usdBrlState.value = freshState
                        freshState
                    } else {
                        handleFailure(
                            cachedQuote = cachedQuote,
                            attemptAt = now,
                            message = "Cotação recebida inválida — cache anterior preservado"
                        )
                    }
                }

                is RemoteQuoteFetchResult.Failure -> {
                    handleFailure(
                        cachedQuote = cachedQuote,
                        attemptAt = now,
                        message = toUserFacingErrorMessage(fetchResult, hasCachedQuote = cachedQuote != null)
                    )
                }
            }
        }
    }

    private fun handleFailure(
        cachedQuote: MarketQuoteSnapshot?,
        attemptAt: Long,
        message: String
    ): MarketQuoteUiState {
        store.recordFailedAttempt(
            assetCode = MarketAssetCatalog.USD_BRL_CODE,
            attemptAtEpochMs = attemptAt,
            errorMessage = message
        )
        val offlineFailure = !isNetworkAvailable()
        val state = MarketQuoteUiState(
            assetCode = MarketAssetCatalog.USD_BRL_CODE,
            quote = cachedQuote,
            isRefreshing = false,
            isStale = cachedQuote?.let { MarketQuoteCalculations.isQuoteStale(it, attemptAt) } ?: false,
            isOffline = offlineFailure,
            lastRefreshFailed = true,
            statusMessage = message,
            lastAttemptAtEpochMs = attemptAt
        )
        _usdBrlState.value = state
        return state
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

    private fun buildStateFromStore(
        isRefreshing: Boolean = false,
        nowEpochMs: Long = clock()
    ): MarketQuoteUiState {
        val stored = store.loadRecord(MarketAssetCatalog.USD_BRL_CODE)
        val cachedQuote = stored?.lastValidQuote
        val online = isNetworkAvailable()
        return MarketQuoteUiState(
            assetCode = MarketAssetCatalog.USD_BRL_CODE,
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
        fun createDefault(context: Context): MarketQuoteRepository {
            val appContext = context.applicationContext
            val transport = HttpsUrlConnectionTransport()
            val remote = CompositeMarketQuoteRemoteDataSource(
                providers = listOf(
                    AwesomeApiUsdBrlDataSource(transport),
                    BcbOlindaPtaxDataSource(transport)
                )
            )
            return MarketQuoteRepository(
                remoteDataSource = remote,
                store = SharedPreferencesMarketQuoteStore(appContext),
                isNetworkAvailable = { checkNetworkConnected(appContext) }
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
