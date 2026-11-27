package com.nexus.app.data.market

import android.content.Context
import com.nexus.app.domain.model.MarketHistoryCalculations
import com.nexus.app.domain.model.MarketHistoryRange
import com.nexus.app.domain.model.MarketHistorySeries
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.concurrent.ConcurrentHashMap

@Serializable
data class PersistedMarketHistoryRecord(
    val series: MarketHistorySeries? = null,
    val lastAttemptAtEpochMs: Long? = null,
    val lastAttemptError: String? = null
)

interface MarketHistoryStore {
    fun loadRecord(assetCode: String, range: MarketHistoryRange): PersistedMarketHistoryRecord?
    fun loadAllForAsset(assetCode: String): Map<MarketHistoryRange, PersistedMarketHistoryRecord>
    fun saveValidSeries(series: MarketHistorySeries)
    fun recordFailedAttempt(
        assetCode: String,
        range: MarketHistoryRange,
        attemptAtEpochMs: Long,
        errorMessage: String
    )
    fun clearAll() {}
}

/**
 * Persistência local em arquivos individuais por (ativo, intervalo) em `filesDir/market_history/`.
 *
 * Motivos da escolha:
 * - Evita inflar SharedPreferences com séries temporais de dezenas/centenas de pontos.
 * - Preserva o banco Room (NexusDatabase) exclusivamente para dados do usuário e backup pessoal,
 *   sem exigir migração de schema para cache descartável de mercado.
 * - Gravação atômica via arquivo temporário + substituição, garantindo leitura offline consistente
 *   e preservação integral da última série válida em falhas de rede.
 */
class FileMarketHistoryStore(
    private val directory: File
) : MarketHistoryStore {

    constructor(context: Context) : this(
        File(context.applicationContext.filesDir, DIR_NAME)
    )

    private val fileLocks = ConcurrentHashMap<String, Any>()
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private fun lockFor(key: String): Any = fileLocks.computeIfAbsent(key) { Any() }

    private fun fileKey(assetCode: String, range: MarketHistoryRange): String {
        val safeCode = assetCode.replace(Regex("[^A-Za-z0-9_-]"), "_")
        return "${safeCode}_${range.key}"
    }

    private fun targetFile(assetCode: String, range: MarketHistoryRange): File =
        File(directory, "${fileKey(assetCode, range)}.json")

    override fun loadRecord(assetCode: String, range: MarketHistoryRange): PersistedMarketHistoryRecord? {
        val key = fileKey(assetCode, range)
        synchronized(lockFor(key)) {
            return readRecordLocked(targetFile(assetCode, range))
        }
    }

    override fun loadAllForAsset(assetCode: String): Map<MarketHistoryRange, PersistedMarketHistoryRecord> {
        val result = LinkedHashMap<MarketHistoryRange, PersistedMarketHistoryRecord>()
        for (range in MarketHistoryCalculations.supportedRangesFor(assetCode)) {
            loadRecord(assetCode, range)?.let { result[range] = it }
        }
        return result
    }

    override fun saveValidSeries(series: MarketHistorySeries) {
        val range = series.resolveRange() ?: return
        val normalizedPoints = MarketHistoryCalculations.normalizeAndDeduplicate(series.points)
        if (normalizedPoints.size < 2) return

        val cleanSeries = series.copy(points = normalizedPoints)
        val key = fileKey(series.assetCode, range)
        synchronized(lockFor(key)) {
            val record = PersistedMarketHistoryRecord(
                series = cleanSeries,
                lastAttemptAtEpochMs = cleanSeries.fetchedAtEpochMs,
                lastAttemptError = null
            )
            writeRecordAtomicallyLocked(targetFile(series.assetCode, range), record)
        }
    }

    override fun recordFailedAttempt(
        assetCode: String,
        range: MarketHistoryRange,
        attemptAtEpochMs: Long,
        errorMessage: String
    ) {
        val key = fileKey(assetCode, range)
        synchronized(lockFor(key)) {
            val file = targetFile(assetCode, range)
            val existing = readRecordLocked(file)
            val updated = PersistedMarketHistoryRecord(
                series = existing?.series,
                lastAttemptAtEpochMs = attemptAtEpochMs,
                lastAttemptError = errorMessage
            )
            writeRecordAtomicallyLocked(file, updated)
        }
    }

    override fun clearAll() {
        runCatching {
            if (directory.exists() && directory.isDirectory) {
                directory.listFiles()?.forEach { child ->
                    val lockKey = child.nameWithoutExtension
                    synchronized(lockFor(lockKey)) {
                        child.delete()
                    }
                }
            }
        }
    }

    private fun readRecordLocked(file: File): PersistedMarketHistoryRecord? {
        if (!file.exists() || !file.isFile) return null
        return runCatching {
            val raw = file.readText(Charsets.UTF_8)
            if (raw.isBlank()) return@runCatching null
            val decoded = json.decodeFromString<PersistedMarketHistoryRecord>(raw)
            val cleanSeries = decoded.series?.let { s ->
                val normalized = MarketHistoryCalculations.normalizeAndDeduplicate(s.points)
                if (normalized.size >= 2) s.copy(points = normalized) else null
            }
            decoded.copy(series = cleanSeries)
        }.getOrNull()
    }

    private fun writeRecordAtomicallyLocked(file: File, record: PersistedMarketHistoryRecord) {
        runCatching {
            val parent = file.parentFile
            if (parent != null && !parent.exists()) {
                parent.mkdirs()
            }
            val tmpFile = File(parent, "${file.name}.tmp")
            tmpFile.writeText(json.encodeToString(record), Charsets.UTF_8)
            if (!tmpFile.renameTo(file)) {
                file.writeText(json.encodeToString(record), Charsets.UTF_8)
                tmpFile.delete()
            }
        }
    }

    companion object {
        private const val DIR_NAME = "market_history"
    }
}
