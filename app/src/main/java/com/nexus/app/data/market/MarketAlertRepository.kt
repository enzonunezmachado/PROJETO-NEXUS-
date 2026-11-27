package com.nexus.app.data.market

import android.content.Context
import com.nexus.app.core.DateTimeUtils
import com.nexus.app.domain.model.MarketAlertCondition
import com.nexus.app.domain.model.MarketAlertRules
import com.nexus.app.domain.model.MarketAlertStatus
import com.nexus.app.domain.model.MarketAlertTriggerEvent
import com.nexus.app.domain.model.MarketAssetCatalog
import com.nexus.app.domain.model.MarketPriceAlert
import com.nexus.app.domain.model.MarketQuoteCalculations
import com.nexus.app.domain.model.MarketQuoteSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/**
 * Gerencia alertas locais de preço para os 12 ativos suportados no Mercado.
 * Não realiza chamadas de rede próprias: consome exclusivamente cotações válidas
 * emitidas pelo [MarketQuoteRepository].
 */
class MarketAlertRepository(
    private val store: MarketAlertStore,
    private val onTriggerNotification: (MarketAlertTriggerEvent) -> Boolean = { false },
    private val clock: () -> Long = DateTimeUtils::nowEpochMillis
) {
    private val writeLock = Any()

    private val _alerts = MutableStateFlow(sortAlerts(store.loadAlerts()))
    val alerts: StateFlow<List<MarketPriceAlert>> = _alerts.asStateFlow()

    fun createAlert(
        assetCode: String,
        condition: MarketAlertCondition,
        targetPrice: Double,
        isActive: Boolean = true,
        currentQuote: MarketQuoteSnapshot? = null
    ): Result<MarketPriceAlert> {
        val descriptor = MarketAssetCatalog.findByCode(assetCode)
            ?: return Result.failure(IllegalArgumentException("Ativo não suportado para alertas."))
        if (!MarketQuoteCalculations.isValidPositiveRate(targetPrice)) {
            return Result.failure(IllegalArgumentException("Informe um valor-alvo positivo válido."))
        }

        val compatibleBaseline = currentQuote?.takeIf {
            MarketAlertRules.isQuoteCompatibleWithAsset(it, descriptor)
        }
        val now = clock()
        val created = MarketPriceAlert(
            id = UUID.randomUUID().toString(),
            assetCode = descriptor.code,
            condition = condition,
            targetPrice = targetPrice,
            isActive = isActive,
            status = if (isActive) MarketAlertStatus.WAITING else MarketAlertStatus.PAUSED,
            createdAtEpochMs = now,
            updatedAtEpochMs = now,
            awaitingConditionReset = false,
            lastEvaluatedQuoteFetchedAtEpochMs = compatibleBaseline?.fetchedAtEpochMs
        )

        synchronized(writeLock) {
            val next = sortAlerts(_alerts.value + created)
            store.saveAlerts(next)
            _alerts.value = next
        }
        return Result.success(created)
    }

    fun updateAlert(
        alertId: String,
        condition: MarketAlertCondition,
        targetPrice: Double,
        isActive: Boolean,
        currentQuote: MarketQuoteSnapshot? = null
    ): Result<MarketPriceAlert> {
        if (!MarketQuoteCalculations.isValidPositiveRate(targetPrice)) {
            return Result.failure(IllegalArgumentException("Informe um valor-alvo positivo válido."))
        }

        synchronized(writeLock) {
            val currentList = _alerts.value
            val existing = currentList.firstOrNull { it.id == alertId }
                ?: return Result.failure(IllegalArgumentException("Alerta não encontrado."))
            val descriptor = MarketAssetCatalog.findByCode(existing.assetCode)
                ?: return Result.failure(IllegalArgumentException("Ativo não suportado."))

            val compatibleQuote = currentQuote?.takeIf {
                MarketAlertRules.isQuoteCompatibleWithAsset(it, descriptor)
            }
            val sameRuleAfterTrigger =
                existing.status == MarketAlertStatus.TRIGGERED &&
                    existing.condition == condition &&
                    existing.targetPrice == targetPrice
            val referencePrice = compatibleQuote?.price ?: existing.triggeredPrice
            val needsReset = isActive &&
                sameRuleAfterTrigger &&
                referencePrice != null &&
                MarketAlertRules.matchesCondition(condition, referencePrice, targetPrice)

            val now = clock()
            val updated = existing.copy(
                condition = condition,
                targetPrice = targetPrice,
                isActive = isActive,
                status = if (isActive) MarketAlertStatus.WAITING else MarketAlertStatus.PAUSED,
                updatedAtEpochMs = now,
                triggeredAtEpochMs = null,
                triggeredPrice = null,
                triggeredQuoteSource = null,
                triggeredQuoteTimestampEpochMs = null,
                awaitingConditionReset = needsReset,
                lastEvaluatedQuoteFetchedAtEpochMs = compatibleQuote?.fetchedAtEpochMs
                    ?: existing.lastEvaluatedQuoteFetchedAtEpochMs
            )
            val next = sortAlerts(currentList.map { if (it.id == alertId) updated else it })
            store.saveAlerts(next)
            _alerts.value = next
            return Result.success(updated)
        }
    }

    fun setAlertActive(
        alertId: String,
        active: Boolean,
        currentQuote: MarketQuoteSnapshot? = null
    ): Result<MarketPriceAlert> {
        synchronized(writeLock) {
            val currentList = _alerts.value
            val existing = currentList.firstOrNull { it.id == alertId }
                ?: return Result.failure(IllegalArgumentException("Alerta não encontrado."))
            val descriptor = MarketAssetCatalog.findByCode(existing.assetCode)
                ?: return Result.failure(IllegalArgumentException("Ativo não suportado."))

            val compatibleQuote = currentQuote?.takeIf {
                MarketAlertRules.isQuoteCompatibleWithAsset(it, descriptor)
            }
            val now = clock()
            val updated = if (!active) {
                existing.copy(
                    isActive = false,
                    status = MarketAlertStatus.PAUSED,
                    awaitingConditionReset = false,
                    updatedAtEpochMs = now
                )
            } else {
                val referencePrice = compatibleQuote?.price ?: existing.triggeredPrice
                val wasTriggered = existing.status == MarketAlertStatus.TRIGGERED ||
                    existing.triggeredAtEpochMs != null
                val requiresReset = wasTriggered &&
                    referencePrice != null &&
                    MarketAlertRules.matchesCondition(
                        condition = existing.condition,
                        currentPrice = referencePrice,
                        targetPrice = existing.targetPrice
                    )

                existing.copy(
                    isActive = true,
                    status = MarketAlertStatus.WAITING,
                    awaitingConditionReset = requiresReset,
                    lastEvaluatedQuoteFetchedAtEpochMs = compatibleQuote?.fetchedAtEpochMs
                        ?: existing.lastEvaluatedQuoteFetchedAtEpochMs,
                    updatedAtEpochMs = now
                )
            }

            val next = sortAlerts(currentList.map { if (it.id == alertId) updated else it })
            store.saveAlerts(next)
            _alerts.value = next
            return Result.success(updated)
        }
    }

    fun deleteAlert(alertId: String): Boolean {
        synchronized(writeLock) {
            val currentList = _alerts.value
            if (currentList.none { it.id == alertId }) return false
            val next = currentList.filterNot { it.id == alertId }
            store.saveAlerts(next)
            _alerts.value = next
            return true
        }
    }

    /**
     * Substitui integralmente os alertas persistidos a partir de restauração validada.
     * Não executa avaliação de cotações nem dispara notificações.
     */
    fun replaceAllAlerts(restoredAlerts: List<MarketPriceAlert>): List<MarketPriceAlert> {
        synchronized(writeLock) {
            val next = sortAlerts(restoredAlerts.distinctBy { it.id })
            store.saveAlerts(next)
            _alerts.value = next
            return next
        }
    }

    fun clearAllLocalData(): Int {
        synchronized(writeLock) {
            val removedCount = _alerts.value.size
            store.saveAlerts(emptyList())
            _alerts.value = emptyList()
            return removedCount
        }
    }

    fun evaluateQuotes(freshQuotes: Map<String, MarketQuoteSnapshot>): List<MarketAlertTriggerEvent> {
        if (freshQuotes.isEmpty()) return emptyList()

        val triggered = synchronized(writeLock) {
            val evaluation = MarketAlertRules.evaluateAlerts(
                alerts = _alerts.value,
                freshQuotes = freshQuotes,
                nowEpochMs = clock()
            )
            if (evaluation.changed) {
                val sorted = sortAlerts(evaluation.updatedAlerts)
                store.saveAlerts(sorted)
                _alerts.value = sorted
            }
            evaluation.triggeredEvents
        }

        triggered.forEach { event ->
            runCatching { onTriggerNotification(event) }
        }
        return triggered
    }

    private fun sortAlerts(items: List<MarketPriceAlert>): List<MarketPriceAlert> {
        return items.sortedWith(
            compareBy<MarketPriceAlert>(
                { statusSortPriority(it.status) },
                { -(it.triggeredAtEpochMs ?: it.updatedAtEpochMs) },
                { -it.createdAtEpochMs }
            )
        )
    }

    private fun statusSortPriority(status: MarketAlertStatus): Int = when (status) {
        MarketAlertStatus.WAITING -> 0
        MarketAlertStatus.TRIGGERED -> 1
        MarketAlertStatus.PAUSED -> 2
    }

    companion object {
        fun createDefault(
            context: Context,
            onTriggerNotification: (MarketAlertTriggerEvent) -> Boolean
        ): MarketAlertRepository {
            return MarketAlertRepository(
                store = FileMarketAlertStore(context.applicationContext),
                onTriggerNotification = onTriggerNotification
            )
        }
    }
}
