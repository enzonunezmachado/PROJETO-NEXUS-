package com.nexus.app.domain.model

import kotlinx.serialization.Serializable
import java.text.NumberFormat
import java.util.Locale

@Serializable
enum class MarketAlertCondition(
    val label: String,
    val shortLabel: String
) {
    PRICE_ABOVE("Preço acima de", "Acima de"),
    PRICE_BELOW("Preço abaixo de", "Abaixo de")
}

@Serializable
enum class MarketAlertStatus(val label: String) {
    WAITING("Aguardando"),
    TRIGGERED("Disparado"),
    PAUSED("Pausado")
}

@Serializable
data class MarketPriceAlert(
    val id: String,
    val assetCode: String,
    val condition: MarketAlertCondition,
    val targetPrice: Double,
    val isActive: Boolean = true,
    val status: MarketAlertStatus = if (isActive) MarketAlertStatus.WAITING else MarketAlertStatus.PAUSED,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long = createdAtEpochMs,
    val triggeredAtEpochMs: Long? = null,
    val triggeredPrice: Double? = null,
    val triggeredQuoteSource: String? = null,
    val triggeredQuoteTimestampEpochMs: Long? = null,
    val awaitingConditionReset: Boolean = false,
    val lastEvaluatedQuoteFetchedAtEpochMs: Long? = null
)

data class MarketAlertTriggerEvent(
    val alert: MarketPriceAlert,
    val observedQuote: MarketQuoteSnapshot
)

data class MarketAlertEvaluationResult(
    val updatedAlerts: List<MarketPriceAlert>,
    val triggeredEvents: List<MarketAlertTriggerEvent>,
    val changed: Boolean
)

object MarketAlertRules {

    fun matchesCondition(
        condition: MarketAlertCondition,
        currentPrice: Double,
        targetPrice: Double
    ): Boolean {
        if (!MarketQuoteCalculations.isValidPositiveRate(currentPrice) ||
            !MarketQuoteCalculations.isValidPositiveRate(targetPrice)
        ) {
            return false
        }
        return when (condition) {
            MarketAlertCondition.PRICE_ABOVE -> currentPrice >= targetPrice
            MarketAlertCondition.PRICE_BELOW -> currentPrice <= targetPrice
        }
    }

    fun isOppositeSideOfTarget(
        condition: MarketAlertCondition,
        currentPrice: Double,
        targetPrice: Double
    ): Boolean {
        if (!MarketQuoteCalculations.isValidPositiveRate(currentPrice) ||
            !MarketQuoteCalculations.isValidPositiveRate(targetPrice)
        ) {
            return false
        }
        return when (condition) {
            MarketAlertCondition.PRICE_ABOVE -> currentPrice < targetPrice
            MarketAlertCondition.PRICE_BELOW -> currentPrice > targetPrice
        }
    }

    fun isQuoteCompatibleWithAsset(
        quote: MarketQuoteSnapshot,
        descriptor: MarketAssetCatalog.AssetDescriptor
    ): Boolean {
        if (quote.assetCode != descriptor.code) return false
        if (!MarketQuoteCalculations.isValidPositiveRate(quote.price)) return false
        if (quote.fetchedAtEpochMs <= 0L) return false
        if (quote.quoteCurrency.isNotBlank() &&
            !quote.quoteCurrency.equals(descriptor.quoteSymbol, ignoreCase = true)
        ) {
            return false
        }
        return true
    }

    fun evaluateAlerts(
        alerts: List<MarketPriceAlert>,
        freshQuotes: Map<String, MarketQuoteSnapshot>,
        nowEpochMs: Long
    ): MarketAlertEvaluationResult {
        if (alerts.isEmpty() || freshQuotes.isEmpty()) {
            return MarketAlertEvaluationResult(
                updatedAlerts = alerts,
                triggeredEvents = emptyList(),
                changed = false
            )
        }

        val triggeredEvents = mutableListOf<MarketAlertTriggerEvent>()
        var changed = false

        val updatedAlerts = alerts.map { alert ->
            if (!alert.isActive || alert.status != MarketAlertStatus.WAITING) {
                return@map alert
            }
            if (!MarketQuoteCalculations.isValidPositiveRate(alert.targetPrice)) {
                return@map alert
            }

            val descriptor = MarketAssetCatalog.findByCode(alert.assetCode) ?: return@map alert
            val quote = freshQuotes[alert.assetCode] ?: return@map alert
            if (!isQuoteCompatibleWithAsset(quote, descriptor)) {
                return@map alert
            }

            val lastEvaluated = alert.lastEvaluatedQuoteFetchedAtEpochMs
            if (lastEvaluated != null && quote.fetchedAtEpochMs <= lastEvaluated) {
                return@map alert
            }

            if (alert.awaitingConditionReset) {
                val clearedReset = isOppositeSideOfTarget(
                    condition = alert.condition,
                    currentPrice = quote.price,
                    targetPrice = alert.targetPrice
                )
                changed = true
                return@map alert.copy(
                    awaitingConditionReset = !clearedReset,
                    lastEvaluatedQuoteFetchedAtEpochMs = quote.fetchedAtEpochMs,
                    updatedAtEpochMs = if (clearedReset) nowEpochMs else alert.updatedAtEpochMs
                )
            }

            if (matchesCondition(alert.condition, quote.price, alert.targetPrice)) {
                val triggeredAlert = alert.copy(
                    isActive = false,
                    status = MarketAlertStatus.TRIGGERED,
                    triggeredAtEpochMs = nowEpochMs,
                    triggeredPrice = quote.price,
                    triggeredQuoteSource = quote.sourceName,
                    triggeredQuoteTimestampEpochMs = quote.sourceTimestampEpochMs,
                    awaitingConditionReset = false,
                    lastEvaluatedQuoteFetchedAtEpochMs = quote.fetchedAtEpochMs,
                    updatedAtEpochMs = nowEpochMs
                )
                changed = true
                triggeredEvents.add(MarketAlertTriggerEvent(triggeredAlert, quote))
                triggeredAlert
            } else {
                changed = true
                alert.copy(
                    lastEvaluatedQuoteFetchedAtEpochMs = quote.fetchedAtEpochMs
                )
            }
        }

        return MarketAlertEvaluationResult(
            updatedAlerts = updatedAlerts,
            triggeredEvents = triggeredEvents,
            changed = changed
        )
    }

    fun unitLabelFor(descriptor: MarketAssetCatalog.AssetDescriptor): String {
        return if (descriptor.isPointsUnit || descriptor.category == AssetCategory.INDEX) {
            "Pontos (${descriptor.quoteSymbol.uppercase()})"
        } else {
            descriptor.quoteSymbol.uppercase()
        }
    }

    fun formatValueForAsset(
        value: Double,
        descriptor: MarketAssetCatalog.AssetDescriptor
    ): String = MarketQuoteCalculations.formatPrice(
        value = value,
        category = descriptor.category,
        currencyCode = descriptor.quoteSymbol
    )

    fun inputPrefixAndSuffix(
        descriptor: MarketAssetCatalog.AssetDescriptor
    ): Pair<String?, String?> {
        if (descriptor.isPointsUnit || descriptor.category == AssetCategory.INDEX) {
            return null to "pts"
        }
        val prefix = when (descriptor.quoteSymbol.uppercase()) {
            "BRL" -> "R$"
            "USD" -> "US$"
            "EUR" -> "€"
            else -> descriptor.quoteSymbol.uppercase()
        }
        return prefix to null
    }

    fun formatEditableTarget(
        value: Double,
        category: AssetCategory
    ): String {
        if (!MarketQuoteCalculations.isValidPositiveRate(value)) return ""
        val decimals = when (category) {
            AssetCategory.CURRENCY -> 4
            AssetCategory.CRYPTO -> if (value >= 10.0) 2 else 4
            else -> 2
        }
        val formatter = NumberFormat.getNumberInstance(Locale("pt", "BR")).apply {
            isGroupingUsed = false
            minimumFractionDigits = decimals
            maximumFractionDigits = decimals
        }
        return formatter.format(value)
    }

    fun parseTargetInput(rawInput: String): Double? {
        val cleaned = rawInput
            .trim()
            .replace("R$", "", ignoreCase = true)
            .replace("US$", "", ignoreCase = true)
            .replace("pts", "", ignoreCase = true)
            .replace(" ", "")
            .replace("\u00A0", "")
        if (cleaned.isEmpty()) return null
        if (!cleaned.all { it.isDigit() || it == '.' || it == ',' }) return null

        val lastComma = cleaned.lastIndexOf(',')
        val lastDot = cleaned.lastIndexOf('.')
        val normalized = when {
            lastComma >= 0 && lastDot >= 0 -> {
                if (lastComma > lastDot) {
                    // Formato pt-BR: 170.000,50
                    cleaned.replace(".", "").replace(',', '.')
                } else {
                    // Formato US: 170,000.50
                    cleaned.replace(",", "")
                }
            }
            lastComma >= 0 -> {
                val parts = cleaned.split(',')
                if (parts.size == 2 && parts[0].isNotEmpty() && parts[1].isNotEmpty()) {
                    cleaned.replace(',', '.')
                } else if (parts.size > 2 && parts.drop(1).all { it.length == 3 }) {
                    cleaned.replace(",", "")
                } else {
                    return null
                }
            }
            lastDot >= 0 -> {
                val parts = cleaned.split('.')
                if (parts.size == 2 && parts[0].isNotEmpty() && parts[1].isNotEmpty()) {
                    cleaned
                } else if (parts.size > 2 && parts.drop(1).all { it.length == 3 }) {
                    cleaned.replace(".", "")
                } else {
                    return null
                }
            }
            else -> cleaned
        }

        val parsed = normalized.toDoubleOrNull() ?: return null
        return parsed.takeIf { MarketQuoteCalculations.isValidPositiveRate(it) }
    }
}
