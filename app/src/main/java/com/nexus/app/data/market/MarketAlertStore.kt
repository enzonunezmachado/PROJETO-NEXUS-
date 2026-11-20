package com.nexus.app.data.market

import android.content.Context
import com.nexus.app.domain.model.MarketAssetCatalog
import com.nexus.app.domain.model.MarketPriceAlert
import com.nexus.app.domain.model.MarketQuoteCalculations
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

interface MarketAlertStore {
    fun loadAlerts(): List<MarketPriceAlert>
    fun saveAlerts(alerts: List<MarketPriceAlert>)
}

/**
 * Persistência dedicada e atômica em arquivo JSON próprio (`filesDir/market_alerts/market_alerts.json`).
 *
 * Motivos da escolha:
 * - Isolada do histórico de preços (`filesDir/market_history/`), do cache de cotações e da watchlist.
 * - Escrita atômica síncrona (`.tmp` + renameTo), garantindo integridade quando o WorkManager
 *   atualiza o estado de disparo em segundo plano antes do término do processo.
 * - Preserva o banco Room (`NexusDatabase`) e `BackupManager` intactos, sem migração de schema.
 */
class FileMarketAlertStore(
    private val targetFile: File
) : MarketAlertStore {

    constructor(context: Context) : this(
        File(File(context.applicationContext.filesDir, DIR_NAME), FILE_NAME)
    )

    private val fileLock = Any()
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    override fun loadAlerts(): List<MarketPriceAlert> {
        synchronized(fileLock) {
            if (!targetFile.exists() || !targetFile.isFile) return emptyList()
            return runCatching {
                val raw = targetFile.readText(Charsets.UTF_8)
                if (raw.isBlank()) return@runCatching emptyList()
                sanitize(json.decodeFromString<List<MarketPriceAlert>>(raw))
            }.getOrDefault(emptyList())
        }
    }

    override fun saveAlerts(alerts: List<MarketPriceAlert>) {
        val sanitized = sanitize(alerts)
        synchronized(fileLock) {
            runCatching {
                val parent = targetFile.parentFile
                if (parent != null && !parent.exists()) {
                    parent.mkdirs()
                }
                val tmpFile = File(parent, "${targetFile.name}.tmp")
                tmpFile.writeText(json.encodeToString(sanitized), Charsets.UTF_8)
                if (!tmpFile.renameTo(targetFile)) {
                    targetFile.writeText(json.encodeToString(sanitized), Charsets.UTF_8)
                    tmpFile.delete()
                }
            }
        }
    }

    private fun sanitize(alerts: List<MarketPriceAlert>): List<MarketPriceAlert> {
        return alerts
            .filter { alert ->
                alert.id.isNotBlank() &&
                    MarketAssetCatalog.findByCode(alert.assetCode) != null &&
                    MarketQuoteCalculations.isValidPositiveRate(alert.targetPrice)
            }
            .distinctBy { it.id }
            .map { alert ->
                when (alert.status) {
                    com.nexus.app.domain.model.MarketAlertStatus.WAITING ->
                        alert.copy(isActive = true)
                    com.nexus.app.domain.model.MarketAlertStatus.TRIGGERED,
                    com.nexus.app.domain.model.MarketAlertStatus.PAUSED ->
                        alert.copy(isActive = false, awaitingConditionReset = false)
                }
            }
    }

    companion object {
        private const val DIR_NAME = "market_alerts"
        private const val FILE_NAME = "market_alerts.json"
    }
}
