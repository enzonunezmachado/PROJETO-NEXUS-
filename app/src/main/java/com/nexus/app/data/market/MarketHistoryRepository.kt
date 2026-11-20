package com.nexus.app.data.market

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.nexus.app.core.DateTimeUtils
import com.nexus.app.domain.model.MarketAssetCatalog
import com.nexus.app.domain.model.MarketHistoryCalculations
import com.nexus.app.domain.model.MarketHistoryRange
import com.nexus.app.domain.model.MarketHistorySeries
import com.nexus.app.domain.model.MarketHistoryUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

class MarketHistoryRepository(
    private val remoteDataSource: MarketHistoryRemoteDataSource,
    private val store: MarketHistoryStore,
    private val isNetworkAvailable: () -> Boolean,
    private val clock: () -> Long = DateTimeUtils::nowEpochMillis
) {
    private val requestMutexes = ConcurrentHashMap<String, Mutex>()
    private val stateWriteLock = Any()

    private val _historyStates = MutableStateFlow(buildInitialStatesFromStore())
    val historyStates: StateFlow<Map<String, Map<MarketHistoryRange, MarketHistoryUiState>>> =
        _historyStates.asStateFlow()

    private fun mutexFor(assetCode: String, range: MarketHistoryRange): Mutex =
        requestMutexes.computeIfAbsent("${assetCode}_${range.key}") { Mutex() }

    suspend fun loadHistory(
        assetCode: String,
        range: MarketHistoryRange,
        force: Boolean = false
    ): MarketHistoryUiState {
        val asset = MarketAssetCatalog.findByCode(assetCode)
            ?: return MarketHistoryUiState(
                assetCode = assetCode,
                selectedRange = range,
                supportedRanges = emptyList(),
                lastRefreshFailed = true,
                statusMessage = "Ativo não suportado: $assetCode"
            )

        val supported = MarketHistoryCalculations.supportedRangesFor(asset.code)
        val effectiveRange = if (range in supported) range else MarketHistoryCalculations.defaultRangeFor(asset.code)

        return mutexFor(asset.code, effectiveRange).withLock {
            val now = clock()
            val stored = store.loadRecord(asset.code, effectiveRange)
            val cachedSeries = stored?.series

            if (!force && shouldSkipAutoRefresh(stored, cachedSeries, effectiveRange, now)) {
                val state = buildStateFromRecord(
                    assetCode = asset.code,
                    range = effectiveRange,
                    record = stored,
                    isLoading = false,
                    nowEpochMs = now
                )
                updateSingleState(asset.code, effectiveRange, state)
                return@withLock state
            }

            if (!isNetworkAvailable()) {
                val offlineState = buildOfflineState(
                    assetCode = asset.code,
                    range = effectiveRange,
                    cachedSeries = cachedSeries,
                    stored = stored,
                    nowEpochMs = now,
                    markFailure = force
                )
                updateSingleState(asset.code, effectiveRange, offlineState)
                return@withLock offlineState
            }

            val base = _historyStates.value[asset.code]?.get(effectiveRange)
                ?: buildStateFromRecord(asset.code, effectiveRange, stored, isLoading = false, nowEpochMs = now)
            updateSingleState(
                asset.code,
                effectiveRange,
                base.copy(isLoading = true, isOffline = false, statusMessage = null)
            )

            val result = remoteDataSource.fetchHistory(asset, effectiveRange)
            val finalState = applyFetchResult(asset.code, effectiveRange, cachedSeries, result, now)
            updateSingleState(asset.code, effectiveRange, finalState)
            finalState
        }
    }

    private fun applyFetchResult(
        assetCode: String,
        range: MarketHistoryRange,
        cachedSeries: MarketHistorySeries?,
        result: RemoteHistoryFetchResult,
        attemptAtEpochMs: Long
    ): MarketHistoryUiState {
        return when (result) {
            is RemoteHistoryFetchResult.Success -> {
                val normalized = result.series.copy(
                    points = MarketHistoryCalculations.normalizeAndDeduplicate(result.series.points)
                )
                if (normalized.points.size >= 2) {
                    store.saveValidSeries(normalized)
                    MarketHistoryUiState(
                        assetCode = assetCode,
                        selectedRange = range,
                        supportedRanges = MarketHistoryCalculations.supportedRangesFor(assetCode),
                        series = normalized,
                        metrics = MarketHistoryCalculations.computeMetrics(normalized),
                        isLoading = false,
                        isStale = false,
                        isOffline = false,
                        lastRefreshFailed = false,
                        statusMessage = null,
                        lastAttemptAtEpochMs = normalized.fetchedAtEpochMs
                    )
                } else {
                    handleFailure(
                        assetCode = assetCode,
                        range = range,
                        cachedSeries = cachedSeries,
                        attemptAt = attemptAtEpochMs,
                        message = "Série histórica recebida insuficiente — cache preservado"
                    )
                }
            }

            is RemoteHistoryFetchResult.Failure -> {
                handleFailure(
                    assetCode = assetCode,
                    range = range,
                    cachedSeries = cachedSeries,
                    attemptAt = attemptAtEpochMs,
                    message = toUserFacingErrorMessage(result, hasCachedSeries = cachedSeries != null)
                )
            }
        }
    }

    private fun handleFailure(
        assetCode: String,
        range: MarketHistoryRange,
        cachedSeries: MarketHistorySeries?,
        attemptAt: Long,
        message: String
    ): MarketHistoryUiState {
        store.recordFailedAttempt(
            assetCode = assetCode,
            range = range,
            attemptAtEpochMs = attemptAt,
            errorMessage = message
        )
        val offlineFailure = !isNetworkAvailable()
        return MarketHistoryUiState(
            assetCode = assetCode,
            selectedRange = range,
            supportedRanges = MarketHistoryCalculations.supportedRangesFor(assetCode),
            series = cachedSeries,
            metrics = cachedSeries?.let(MarketHistoryCalculations::computeMetrics),
            isLoading = false,
            isStale = cachedSeries?.let { MarketHistoryCalculations.isSeriesStale(it, attemptAt) } ?: false,
            isOffline = offlineFailure,
            lastRefreshFailed = true,
            statusMessage = message,
            lastAttemptAtEpochMs = attemptAt
        )
    }

    private fun buildOfflineState(
        assetCode: String,
        range: MarketHistoryRange,
        cachedSeries: MarketHistorySeries?,
        stored: PersistedMarketHistoryRecord?,
        nowEpochMs: Long,
        markFailure: Boolean
    ): MarketHistoryUiState {
        val message = if (cachedSeries != null) {
            "Sem conexão — exibindo série histórica em cache"
        } else {
            "Sem conexão e sem histórico local para este intervalo"
        }
        return MarketHistoryUiState(
            assetCode = assetCode,
            selectedRange = range,
            supportedRanges = MarketHistoryCalculations.supportedRangesFor(assetCode),
            series = cachedSeries,
            metrics = cachedSeries?.let(MarketHistoryCalculations::computeMetrics),
            isLoading = false,
            isStale = cachedSeries?.let { MarketHistoryCalculations.isSeriesStale(it, nowEpochMs) } ?: false,
            isOffline = true,
            lastRefreshFailed = markFailure,
            statusMessage = message,
            lastAttemptAtEpochMs = stored?.lastAttemptAtEpochMs
        )
    }

    private fun shouldSkipAutoRefresh(
        stored: PersistedMarketHistoryRecord?,
        cachedSeries: MarketHistorySeries?,
        range: MarketHistoryRange,
        nowEpochMs: Long
    ): Boolean {
        if (cachedSeries != null) {
            val ageMs = nowEpochMs - cachedSeries.fetchedAtEpochMs
            if (ageMs in 0 until MarketHistoryCalculations.autoRefreshCooldownMs(range)) {
                return true
            }
        }
        val lastAttempt = stored?.lastAttemptAtEpochMs
        if (lastAttempt != null && !stored.lastAttemptError.isNullOrBlank()) {
            val sinceFailure = nowEpochMs - lastAttempt
            if (sinceFailure in 0 until MarketHistoryCalculations.FAILURE_RETRY_COOLDOWN_MS) {
                return true
            }
        }
        return false
    }

    private fun buildInitialStatesFromStore(): Map<String, Map<MarketHistoryRange, MarketHistoryUiState>> {
        val now = clock()
        val online = isNetworkAvailable()
        return MarketAssetCatalog.SUPPORTED_ASSETS.associate { asset ->
            val storedByRange = store.loadAllForAsset(asset.code)
            val perRange = MarketHistoryCalculations.supportedRangesFor(asset.code).associateWith { range ->
                buildStateFromRecord(
                    assetCode = asset.code,
                    range = range,
                    record = storedByRange[range],
                    isLoading = false,
                    nowEpochMs = now,
                    onlineOverride = online
                )
            }
            asset.code to perRange
        }
    }

    private fun buildStateFromRecord(
        assetCode: String,
        range: MarketHistoryRange,
        record: PersistedMarketHistoryRecord?,
        isLoading: Boolean,
        nowEpochMs: Long,
        onlineOverride: Boolean? = null
    ): MarketHistoryUiState {
        val online = onlineOverride ?: isNetworkAvailable()
        val cached = record?.series
        return MarketHistoryUiState(
            assetCode = assetCode,
            selectedRange = range,
            supportedRanges = MarketHistoryCalculations.supportedRangesFor(assetCode),
            series = cached,
            metrics = cached?.let(MarketHistoryCalculations::computeMetrics),
            isLoading = isLoading,
            isStale = cached?.let { MarketHistoryCalculations.isSeriesStale(it, nowEpochMs) } ?: false,
            isOffline = !online,
            lastRefreshFailed = !record?.lastAttemptError.isNullOrBlank(),
            statusMessage = when {
                !online && cached != null -> "Sem conexão — exibindo série histórica em cache"
                !online -> "Sem conexão com a internet"
                else -> record?.lastAttemptError
            },
            lastAttemptAtEpochMs = record?.lastAttemptAtEpochMs
        )
    }

    private fun updateSingleState(
        assetCode: String,
        range: MarketHistoryRange,
        state: MarketHistoryUiState
    ) {
        synchronized(stateWriteLock) {
            val root = _historyStates.value.toMutableMap()
            val assetMap = (root[assetCode] ?: emptyMap()).toMutableMap()
            assetMap[range] = state
            root[assetCode] = assetMap
            _historyStates.value = root
        }
    }

    private fun toUserFacingErrorMessage(
        failure: RemoteHistoryFetchResult.Failure,
        hasCachedSeries: Boolean
    ): String {
        val base = when (failure.reason) {
            FailureReason.NETWORK_UNAVAILABLE -> "Sem conexão com o serviço de histórico"
            FailureReason.TIMEOUT -> "Tempo esgotado ao carregar histórico"
            FailureReason.RATE_LIMITED -> "Serviço de histórico temporariamente limitado"
            FailureReason.HTTP_ERROR -> "Serviço de histórico indisponível (${failure.httpStatusCode ?: "HTTP"})"
            FailureReason.INVALID_RESPONSE -> "Dados históricos indisponíveis neste momento"
            FailureReason.UNSUPPORTED_ASSET -> "Intervalo não suportado para este ativo"
        }
        return if (hasCachedSeries) "$base — exibindo cache" else base
    }

    companion object {
        fun createDefault(context: Context): MarketHistoryRepository {
            val appContext = context.applicationContext
            val transport = HttpsUrlConnectionTransport()

            val currencySource = CompositeMarketHistoryRemoteDataSource(
                providers = listOf(
                    BcbOlindaHistoryDataSource(transport),
                    AwesomeApiDailyHistoryDataSource(transport),
                    FrankfurterHistoryDataSource(transport)
                )
            )
            val cryptoSource = CompositeMarketHistoryRemoteDataSource(
                providers = listOf(
                    CoinGeckoHistoryDataSource(transport),
                    OkxHistoryDataSource(transport)
                )
            )
            val b3StockSource = CompositeMarketHistoryRemoteDataSource(
                providers = listOf(
                    BrapiHistoryDataSource(transport),
                    B3FluctuationHistoryDataSource(transport)
                )
            )
            val ibovSource = CompositeMarketHistoryRemoteDataSource(
                providers = listOf(
                    B3FluctuationHistoryDataSource(transport),
                    CnbcHistoryDataSource(transport)
                )
            )
            val spxSource = CnbcHistoryDataSource(transport)
            val nasdaqAndCnbcSource = CompositeMarketHistoryRemoteDataSource(
                providers = listOf(
                    NasdaqHistoryDataSource(transport),
                    CnbcHistoryDataSource(transport)
                )
            )

            val byAssetCode = mapOf(
                MarketAssetCatalog.USD_BRL_CODE to currencySource,
                MarketAssetCatalog.EUR_BRL_CODE to currencySource,
                MarketAssetCatalog.BTC_BRL_CODE to cryptoSource,
                MarketAssetCatalog.ETH_BRL_CODE to cryptoSource,
                MarketAssetCatalog.SOL_BRL_CODE to cryptoSource,
                MarketAssetCatalog.PETR4_CODE to b3StockSource,
                MarketAssetCatalog.VALE3_CODE to b3StockSource,
                MarketAssetCatalog.ITUB4_CODE to b3StockSource,
                MarketAssetCatalog.IBOV_CODE to ibovSource,
                MarketAssetCatalog.SPX_CODE to spxSource,
                MarketAssetCatalog.IXIC_CODE to nasdaqAndCnbcSource,
                MarketAssetCatalog.MELI_CODE to nasdaqAndCnbcSource
            )

            return MarketHistoryRepository(
                remoteDataSource = AssetCategoryRoutedHistoryDataSource(byAssetCode),
                store = FileMarketHistoryStore(appContext),
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
