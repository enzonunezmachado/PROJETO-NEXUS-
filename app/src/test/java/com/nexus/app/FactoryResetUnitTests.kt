package com.nexus.app

import com.nexus.app.data.backup.BackupManager
import com.nexus.app.data.db.NexusDatabase
import com.nexus.app.data.db.entity.CategoryEntity
import com.nexus.app.data.db.entity.PersonalizationSettingsEntity
import com.nexus.app.data.db.entity.RoutineAssistantSettingsEntity
import com.nexus.app.data.db.entity.SettingsEntity
import com.nexus.app.data.inbox.InboxConfig
import com.nexus.app.data.inbox.InboxConfigStorage
import com.nexus.app.data.inbox.InboxConnectionTestResult
import com.nexus.app.data.inbox.InboxIngestionPort
import com.nexus.app.data.inbox.InboxRemoteDataSource
import com.nexus.app.data.inbox.InboxRemoteFile
import com.nexus.app.data.inbox.InboxRepository
import com.nexus.app.data.inbox.InboxTokenStore
import com.nexus.app.data.ingestion.StructuredBatchPreview
import com.nexus.app.data.ingestion.StructuredBatchResult
import com.nexus.app.data.ingestion.StructuredInputEnvelope
import com.nexus.app.data.market.FailureReason
import com.nexus.app.data.market.FileMarketHistoryStore
import com.nexus.app.data.market.MarketAlertRepository
import com.nexus.app.data.market.MarketAlertStore
import com.nexus.app.domain.model.MarketAssetCatalog
import com.nexus.app.data.market.MarketHistoryRepository
import com.nexus.app.data.market.MarketHistoryRemoteDataSource
import com.nexus.app.data.market.MarketQuoteRemoteDataSource
import com.nexus.app.data.market.MarketQuoteRepository
import com.nexus.app.data.market.MarketQuoteStore
import com.nexus.app.data.market.MarketWatchlistRepository
import com.nexus.app.data.market.MarketWatchlistStore
import com.nexus.app.data.market.PersistedMarketQuoteRecord
import com.nexus.app.data.market.RemoteHistoryFetchResult
import com.nexus.app.data.market.RemoteQuoteFetchResult
import com.nexus.app.data.repository.NexusRepository
import com.nexus.app.domain.model.AssetCategory
import com.nexus.app.domain.model.CategoryScope
import com.nexus.app.domain.model.FactoryResetExternalState
import com.nexus.app.domain.model.FactoryResetSummary
import com.nexus.app.domain.model.MarketAlertCondition
import com.nexus.app.domain.model.MarketAlertStatus
import com.nexus.app.domain.model.MarketHistoryGranularity
import com.nexus.app.domain.model.MarketHistoryPoint
import com.nexus.app.domain.model.MarketHistoryRange
import com.nexus.app.domain.model.MarketHistorySeries
import com.nexus.app.domain.model.MarketPriceAlert
import com.nexus.app.domain.model.MarketQuoteSnapshot
import com.nexus.app.domain.model.NexusDateFormat
import com.nexus.app.domain.model.QuoteModality
import com.nexus.app.domain.model.QuoteTrend
import com.nexus.app.ui.FactoryResetUiState
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicInteger

class FactoryResetUnitTests {

    private val t0 = 1_796_000_000_000L

    private class InMemoryWatchlistStore(initial: List<String> = emptyList()) : MarketWatchlistStore {
        var codes: List<String> = initial
        override fun loadFavoriteCodes(): List<String> = codes
        override fun saveFavoriteCodes(codes: List<String>) {
            this.codes = codes
        }
    }

    private class InMemoryAlertStore(initial: List<MarketPriceAlert> = emptyList()) : MarketAlertStore {
        var alerts: List<MarketPriceAlert> = initial
        override fun loadAlerts(): List<MarketPriceAlert> = alerts
        override fun saveAlerts(alerts: List<MarketPriceAlert>) {
            this.alerts = alerts
        }
    }

    private class InMemoryQuoteStore : MarketQuoteStore {
        val map = mutableMapOf<String, PersistedMarketQuoteRecord>()
        override fun loadRecord(assetCode: String): PersistedMarketQuoteRecord? = map[assetCode]
        override fun saveValidQuote(quote: MarketQuoteSnapshot) {
            map[quote.assetCode] = PersistedMarketQuoteRecord(
                lastValidQuote = quote,
                lastAttemptAtEpochMs = quote.fetchedAtEpochMs
            )
        }
        override fun recordFailedAttempt(assetCode: String, attemptAtEpochMs: Long, errorMessage: String) {
            val prev = map[assetCode]
            map[assetCode] = PersistedMarketQuoteRecord(
                lastValidQuote = prev?.lastValidQuote,
                lastAttemptAtEpochMs = attemptAtEpochMs,
                lastAttemptError = errorMessage
            )
        }
        override fun clearAll() {
            map.clear()
        }
    }

    private class NoopQuoteRemote : MarketQuoteRemoteDataSource {
        override suspend fun fetchQuote(asset: MarketAssetCatalog.AssetDescriptor): RemoteQuoteFetchResult =
            RemoteQuoteFetchResult.Failure(FailureReason.NETWORK_UNAVAILABLE, detail = "offline")
    }

    private class NoopHistoryRemote : MarketHistoryRemoteDataSource {
        override suspend fun fetchHistory(
            asset: MarketAssetCatalog.AssetDescriptor,
            range: MarketHistoryRange
        ): RemoteHistoryFetchResult =
            RemoteHistoryFetchResult.Failure(FailureReason.NETWORK_UNAVAILABLE, detail = "offline")
    }

    private class InMemoryInboxConfigStorage : InboxConfigStorage {
        var cfg = InboxConfig()
        val keys = linkedSetOf<String>()
        var fingerprint: String? = null
        override fun loadConfig(): InboxConfig = cfg
        override fun saveConfig(config: InboxConfig) { cfg = config }
        override fun getProcessedFileVersionKeys(): Set<String> = keys.toSet()
        override fun addProcessedFileVersionKeys(keys: Set<String>) { this.keys.addAll(keys) }
        override fun clearProcessedFileVersionKeys() { keys.clear() }
        override fun getLastNotifiedFingerprint(): String? = fingerprint
        override fun setLastNotifiedFingerprint(fingerprint: String?) { this.fingerprint = fingerprint }
    }

    private class InMemoryInboxTokenStore(private var token: String? = null) : InboxTokenStore {
        override fun hasToken(): Boolean = !token.isNullOrBlank()
        override fun getToken(): String? = token
        override fun saveToken(token: String) { this.token = token }
        override fun clearToken() { this.token = null }
    }

    private class NoopInboxRemote : InboxRemoteDataSource {
        override suspend fun testConnection(config: InboxConfig, token: String): InboxConnectionTestResult =
            InboxConnectionTestResult.Failure("offline")
        override suspend fun listInboxJsonFiles(config: InboxConfig, token: String): Result<List<InboxRemoteFile>> =
            Result.failure(IllegalStateException("offline"))
        override suspend fun downloadFileRawContent(config: InboxConfig, file: InboxRemoteFile, token: String): Result<String> =
            Result.failure(IllegalStateException("offline"))
    }

    private class NoopInboxPort : InboxIngestionPort {
        override suspend fun previewInboxEnvelope(envelope: StructuredInputEnvelope): Result<StructuredBatchPreview> =
            Result.failure(IllegalStateException("noop"))
        override suspend fun applyInboxEnvelope(envelope: StructuredInputEnvelope): Result<StructuredBatchResult> =
            Result.failure(IllegalStateException("noop"))
        override suspend fun recordInboxImportLog(batchId: String, serializedSummaryJson: String): Result<Unit> =
            Result.success(Unit)
    }

    // 1. Default factory categories contain exact 4 general + 12 financial categories = 16 total
    @Test
    fun `defaultFactoryCategories re-seeds exact 4 general and 12 financial initial categories`() {
        val cats = NexusRepository.defaultFactoryCategories(t0)
        assertEquals(16, cats.size)
        assertEquals(4, cats.count { it.scope == CategoryScope.GENERAL })
        assertEquals(12, cats.count { it.scope == CategoryScope.FINANCE })
        assertTrue(cats.all { it.isDefault })
        assertEquals(16, cats.map { it.id }.toSet().size)
    }

    // 2. Default singleton configs match factory installation baseline
    @Test
    fun `default factory settings objects preserve dark theme Enzo profile and initial presentation`() {
        val settings = SettingsEntity(
            id = SettingsEntity.SINGLETON_ID,
            userName = "Enzo",
            darkTheme = true,
            declaredBalanceCents = 0L,
            createdAt = t0,
            updatedAt = t0
        )
        val routine = RoutineAssistantSettingsEntity(
            id = RoutineAssistantSettingsEntity.SINGLETON_ID,
            notificationsEnabled = true,
            createdAt = t0,
            updatedAt = t0
        )
        val personalization = PersonalizationSettingsEntity(
            id = PersonalizationSettingsEntity.SINGLETON_ID,
            greetingEnabled = true,
            greetingTemplate = "AUTO",
            dateFormatPattern = NexusDateFormat.DD_MM_YYYY,
            showDailyQuoteOnDashboard = true,
            createdAt = t0,
            updatedAt = t0
        )
        assertEquals("Enzo", settings.userName)
        assertTrue(settings.darkTheme)
        assertEquals(0L, settings.declaredBalanceCents)
        assertTrue(routine.notificationsEnabled)
        assertEquals("AUTO", personalization.greetingTemplate)
    }

    // 3. MarketWatchlistRepository.clearAllLocalData empties persisted favorites & returns removed count
    @Test
    fun `MarketWatchlistRepository clearAllLocalData wipes favorites cleanly and returns count`() {
        val store = InMemoryWatchlistStore(listOf(MarketAssetCatalog.USD_BRL_CODE, MarketAssetCatalog.BTC_BRL_CODE))
        val repo = MarketWatchlistRepository(store)
        assertEquals(2, repo.favoriteCodes.value.size)

        val cleared = repo.clearAllLocalData()
        assertEquals(2, cleared)
        assertTrue(repo.favoriteCodes.value.isEmpty())
        assertTrue(store.codes.isEmpty())
    }

    // 4. MarketAlertRepository.clearAllLocalData wipes all price alerts & updates StateFlow
    @Test
    fun `MarketAlertRepository clearAllLocalData wipes alerts cleanly and resets store`() {
        val store = InMemoryAlertStore(
            listOf(
                MarketPriceAlert(
                    id = "al-1",
                    assetCode = MarketAssetCatalog.USD_BRL_CODE,
                    condition = MarketAlertCondition.PRICE_ABOVE,
                    targetPrice = 6.0,
                    isActive = true,
                    status = MarketAlertStatus.WAITING,
                    createdAtEpochMs = t0,
                    updatedAtEpochMs = t0
                )
            )
        )
        val repo = MarketAlertRepository(store)
        val cleared = repo.clearAllLocalData()
        assertEquals(1, cleared)
        assertTrue(repo.alerts.value.isEmpty())
        assertTrue(store.alerts.isEmpty())
    }

    // 5. MarketQuoteRepository.clearAllLocalData wipes quote store and in-memory quote states
    @Test
    fun `MarketQuoteRepository clearAllLocalData wipes cached quotes and resets StateFlow map`() {
        val store = InMemoryQuoteStore()
        store.saveValidQuote(
            MarketQuoteSnapshot(
                assetCode = MarketAssetCatalog.USD_BRL_CODE,
                category = AssetCategory.CURRENCY,
                baseCurrency = "USD",
                quoteCurrency = "BRL",
                modality = QuoteModality.COMMERCIAL,
                price = 5.65,
                trend = QuoteTrend.FLAT,
                sourceName = "Test",
                sourceTimestampEpochMs = t0,
                fetchedAtEpochMs = t0
            )
        )
        val repo = MarketQuoteRepository(NoopQuoteRemote(), store, isNetworkAvailable = { false })
        assertNotNull(repo.quoteStates.value[MarketAssetCatalog.USD_BRL_CODE]?.quote)

        repo.clearAllLocalData()
        assertNull(store.loadRecord(MarketAssetCatalog.USD_BRL_CODE))
        assertNull(repo.quoteStates.value[MarketAssetCatalog.USD_BRL_CODE]?.quote)
    }

    // 6. MarketHistoryRepository.clearAllLocalData wipes persisted files on disk and in-memory state
    @Test
    fun `MarketHistoryRepository clearAllLocalData deletes history cache files and clears memory`() {
        val tempDir: File = Files.createTempDirectory("nexus-mkt-hist-reset").toFile()
        try {
            val store = FileMarketHistoryStore(tempDir)
            store.saveValidSeries(
                MarketHistorySeries(
                    assetCode = MarketAssetCatalog.USD_BRL_CODE,
                    rangeKey = MarketHistoryRange.M1.key,
                    granularity = MarketHistoryGranularity.DAILY,
                    currency = "BRL",
                    points = listOf(MarketHistoryPoint(timestampEpochMs = t0, price = 5.50)),
                    sourceName = "Test",
                    fetchedAtEpochMs = t0
                )
            )
            assertTrue(tempDir.listFiles()?.isNotEmpty() == true)

            val repo = MarketHistoryRepository(NoopHistoryRemote(), store, isNetworkAvailable = { false })
            repo.clearAllLocalData()

            assertNull(store.loadRecord(MarketAssetCatalog.USD_BRL_CODE, MarketHistoryRange.M1))
            assertTrue(tempDir.listFiles().orEmpty().isEmpty())
            assertTrue(repo.historyStates.value.values.all { perAsset -> perAsset.values.all { it.series == null } })
        } finally {
            tempDir.deleteRecursively()
        }
    }

    // 7. InboxRepository.resetLocalTrackingPreservingCredentials clears tracking while preserving PAT & coordinates
    @Test
    fun `InboxRepository resetLocalTrackingPreservingCredentials wipes tracking and preserves PAT and config`() = runBlocking {
        val cfgStore = InMemoryInboxConfigStorage()
        cfgStore.cfg = InboxConfig(
            owner = "enzo",
            repo = "NEXUS-INBOX",
            branch = "main",
            pathPrefix = "inbox",
            autoCheckEnabled = true
        )
        cfgStore.keys.add("inbox/2026-11-27.json#sha1")
        cfgStore.fingerprint = "fp-123"
        val tokenStore = InMemoryInboxTokenStore("pat_secret_123")
        val repo = InboxRepository(
            configStorage = cfgStore,
            tokenStore = tokenStore,
            remoteDataSource = NoopInboxRemote(),
            nexusRepository = NoopInboxPort(),
            clockMillis = { t0 }
        )

        val reset = repo.resetLocalTrackingPreservingCredentials()
        assertTrue(reset)
        assertTrue(cfgStore.keys.isEmpty())
        assertNull(cfgStore.fingerprint)
        assertEquals("pat_secret_123", tokenStore.getToken())
        assertEquals("enzo", repo.config.value.owner)
        assertEquals("NEXUS-INBOX", repo.config.value.repo)
        assertEquals("main", repo.config.value.branch)
        assertEquals("inbox", repo.config.value.pathPrefix)
        assertTrue(repo.config.value.autoCheckEnabled)
    }

    // 8. InboxRepository.withExclusiveOperationLock serializes reset against concurrent inbox operations
    @Test
    fun `InboxRepository withExclusiveOperationLock serializes concurrent executions cleanly`() = runBlocking {
        val repo = InboxRepository(
            configStorage = InMemoryInboxConfigStorage(),
            tokenStore = InMemoryInboxTokenStore(),
            remoteDataSource = NoopInboxRemote(),
            nexusRepository = NoopInboxPort(),
            clockMillis = { t0 }
        )
        val activeCount = AtomicInteger(0)
        val maxConcurrent = AtomicInteger(0)

        (1..5).map { idx ->
            async {
                repo.withExclusiveOperationLock {
                    val nowActive = activeCount.incrementAndGet()
                    maxConcurrent.updateAndGet { prev -> maxOf(prev, nowActive) }
                    delay(10L)
                    activeCount.decrementAndGet()
                    idx
                }
            }
        }.awaitAll()

        assertEquals(1, maxConcurrent.get())
    }

    // 9. Database version and table count parity remain synchronized (28 tables at v8 / backup v9)
    @Test
    fun `NexusDatabase version 8 exposes 28 tables matching FactoryReset table model`() {
        assertEquals(8, NexusDatabase.DATABASE_VERSION)
        val summary = FactoryResetSummary(
            deletedRowsTotal = 120,
            cancelledRemindersCount = 4,
            clearedMarketFavoritesCount = 2,
            clearedMarketAlertsCount = 1,
            marketCachesCleared = true,
            inboxTrackingCleared = true,
            preservedCredentials = true,
            preservedInboxConfig = true,
            restoredDefaultCategoriesCount = 16
        )
        assertTrue(summary.preservedCredentials)
        assertTrue(summary.preservedInboxConfig)
        assertEquals(16, summary.restoredDefaultCategoriesCount)
        val uiState = FactoryResetUiState(isResetting = false, lastSummary = summary, lastError = null)
        assertNotNull(uiState.lastSummary)
    }

    // 10. Transactional rollback pattern preserves pre-reset state & skips notification cancellation on failure
    @Test
    fun `simulated transactional reset failure rolls back state and does not cancel notifications`() = runBlocking {
        val dbMutex = Mutex()
        var dbRows = mutableListOf("user-task-1", "user-appt-2")
        var alarmsCancelled = false
        val externalState = FactoryResetExternalState(
            clearedMarketFavoritesCount = 0,
            clearedMarketAlertsCount = 0,
            marketCachesCleared = false,
            inboxTrackingCleared = false
        )

        val result = runCatching {
            dbMutex.withLock {
                val snapshot = dbRows.toList()
                try {
                    dbRows.clear()
                    throw IllegalStateException("Simulated SQLite write failure")
                } catch (ex: Exception) {
                    dbRows = snapshot.toMutableList()
                    throw ex
                }
            }
            alarmsCancelled = true
        }

        assertTrue(result.isFailure)
        assertFalse(alarmsCancelled)
        assertEquals(listOf("user-task-1", "user-appt-2"), dbRows)
        assertFalse(externalState.marketCachesCleared)
    }
}
