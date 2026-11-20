package com.nexus.app.data.backup

import androidx.room.withTransaction
import com.nexus.app.BuildConfig
import com.nexus.app.core.DateTimeUtils
import com.nexus.app.data.db.NexusDatabase
import com.nexus.app.data.db.entity.AppointmentEntity
import com.nexus.app.data.db.entity.AuditHistoryEntity
import com.nexus.app.data.db.entity.BriefingHistoryEntity
import com.nexus.app.data.db.entity.BudgetEntity
import com.nexus.app.data.db.entity.CategoryEntity
import com.nexus.app.data.db.entity.CreditCardEntity
import com.nexus.app.data.db.entity.CreditCardStatementEntity
import com.nexus.app.data.db.entity.ErrorLogEntity
import com.nexus.app.data.db.entity.EventEntity
import com.nexus.app.data.db.entity.FinancialGoalEntity
import com.nexus.app.data.db.entity.GoalMilestoneEntity
import com.nexus.app.data.db.entity.GoalProgressLogEntity
import com.nexus.app.data.db.entity.InstallmentPurchaseEntity
import com.nexus.app.data.db.entity.LoanEntity
import com.nexus.app.data.db.entity.PersonalGoalEntity
import com.nexus.app.data.db.entity.PersonalizationSettingsEntity
import com.nexus.app.data.db.entity.ProjectEntity
import com.nexus.app.data.db.entity.QuoteEntity
import com.nexus.app.data.db.entity.RecurrenceExceptionEntity
import com.nexus.app.data.db.entity.RecurringRuleEntity
import com.nexus.app.data.db.entity.ReminderEntity
import com.nexus.app.data.db.entity.RoutineAssistantSettingsEntity
import com.nexus.app.data.db.entity.SettingsEntity
import com.nexus.app.data.db.entity.SubtaskEntity
import com.nexus.app.data.db.entity.TaskEntity
import com.nexus.app.data.db.entity.TaskRecurrenceEntity
import com.nexus.app.data.db.entity.TransactionEntity
import com.nexus.app.data.market.MarketAlertRepository
import com.nexus.app.data.market.MarketWatchlistRepository
import com.nexus.app.domain.model.BriefingType
import com.nexus.app.domain.model.CategoryScope
import com.nexus.app.domain.model.EventCostType
import com.nexus.app.domain.model.EventLifecycleStatus
import com.nexus.app.domain.model.FinancialStatus
import com.nexus.app.domain.model.GoalProgressMode
import com.nexus.app.domain.model.GoalStatus
import com.nexus.app.domain.model.MarketAlertCondition
import com.nexus.app.domain.model.MarketAlertRules
import com.nexus.app.domain.model.MarketAlertStatus
import com.nexus.app.domain.model.MarketAssetCatalog
import com.nexus.app.domain.model.MarketPriceAlert
import com.nexus.app.domain.model.MarketQuoteCalculations
import com.nexus.app.domain.model.NexusDateFormat
import com.nexus.app.domain.model.PaymentMethod
import com.nexus.app.domain.model.ProjectStatus
import com.nexus.app.domain.model.RecurrenceExceptionType
import com.nexus.app.domain.model.RecurrenceFrequency
import com.nexus.app.domain.model.ReminderCategory
import com.nexus.app.domain.model.ReminderSourceType
import com.nexus.app.domain.model.ReminderStatus
import com.nexus.app.domain.model.StatementStatus
import com.nexus.app.domain.model.TaskPriority
import com.nexus.app.domain.model.TaskRecurrenceFrequency
import com.nexus.app.domain.model.TaskStatus
import com.nexus.app.domain.model.TicketStatus
import com.nexus.app.domain.model.TransactionType
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import java.util.Locale
import java.util.UUID

class BackupManager(
    private val database: NexusDatabase,
    private val marketWatchlistRepository: MarketWatchlistRepository? = null,
    private val marketAlertRepository: MarketAlertRepository? = null
) {

    private val tolerantJson = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
        prettyPrint = true
        isLenient = true
    }

    suspend fun exportToJson(): String {
        val now = DateTimeUtils.nowEpochMillis()
        val payload = NexusBackupPayload(
            formatVersion = NexusBackupPayload.CURRENT_FORMAT_VERSION,
            appVersion = BuildConfig.VERSION_NAME,
            exportedAt = DateTimeUtils.toIsoUtc(now),
            databaseVersion = NexusDatabase.DATABASE_VERSION,
            settings = database.settingsDao().getAllOnce().map {
                SettingsBackupDto(
                    id = it.id,
                    userName = it.userName,
                    darkTheme = it.darkTheme,
                    declaredBalanceCents = it.declaredBalanceCents,
                    createdAt = it.createdAt,
                    updatedAt = it.updatedAt
                )
            },
            categories = database.categoryDao().getAllOnce().map {
                CategoryBackupDto(
                    id = it.id,
                    name = it.name,
                    colorHex = it.colorHex,
                    scope = it.scope.name,
                    isDefault = it.isDefault,
                    createdAt = it.createdAt,
                    updatedAt = it.updatedAt
                )
            },
            projects = database.projectDao().getAllOnce().map {
                ProjectBackupDto(
                    id = it.id,
                    name = it.name,
                    description = it.description,
                    deadlineAt = it.deadlineAt,
                    status = it.status.name,
                    priority = it.priority.name,
                    goalId = it.goalId,
                    createdAt = it.createdAt,
                    updatedAt = it.updatedAt
                )
            },
            events = database.eventDao().getAllOnce().map {
                EventBackupDto(
                    id = it.id,
                    name = it.name,
                    description = it.description,
                    categoryId = it.categoryId,
                    startAt = it.startAt,
                    endAt = it.endAt,
                    isAllDay = it.isAllDay,
                    officialLink = it.officialLink,
                    ticketLink = it.ticketLink,
                    locationName = it.locationName,
                    address = it.address,
                    city = it.city,
                    state = it.state,
                    notes = it.notes,
                    eventStatus = it.eventStatus.name,
                    ticketStatus = it.ticketStatus.name,
                    ticketPriceCents = it.ticketPriceCents,
                    ticketQuantity = it.ticketQuantity,
                    ticketFeesCents = it.ticketFeesCents,
                    ticketTotalCents = it.ticketTotalCents,
                    ticketPurchaseDate = it.ticketPurchaseDate,
                    ticketDueDate = it.ticketDueDate,
                    paymentMethod = it.paymentMethod.name,
                    creditCardId = it.creditCardId,
                    installmentsCount = it.installmentsCount,
                    createdAt = it.createdAt,
                    updatedAt = it.updatedAt
                )
            },
            tasks = database.taskDao().getAllOnce().map {
                TaskBackupDto(
                    id = it.id,
                    title = it.title,
                    description = it.description,
                    projectId = it.projectId,
                    categoryId = it.categoryId,
                    eventId = it.eventId,
                    goalId = it.goalId,
                    priority = it.priority.name,
                    status = it.status.name,
                    dueAt = it.dueAt,
                    hasDueTime = it.hasDueTime,
                    notes = it.notes,
                    completedAt = it.completedAt,
                    recurrenceId = it.recurrenceId,
                    occurrenceDateIso = it.occurrenceDateIso,
                    createdAt = it.createdAt,
                    updatedAt = it.updatedAt
                )
            },
            subtasks = database.subtaskDao().getAllOnce().map {
                SubtaskBackupDto(
                    id = it.id,
                    taskId = it.taskId,
                    title = it.title,
                    isCompleted = it.isCompleted,
                    sortOrder = it.sortOrder,
                    createdAt = it.createdAt,
                    updatedAt = it.updatedAt
                )
            },
            appointments = database.appointmentDao().getAllOnce().map {
                AppointmentBackupDto(
                    id = it.id,
                    title = it.title,
                    description = it.description,
                    categoryId = it.categoryId,
                    eventId = it.eventId,
                    startAt = it.startAt,
                    endAt = it.endAt,
                    isAllDay = it.isAllDay,
                    location = it.location,
                    link = it.link,
                    createdAt = it.createdAt,
                    updatedAt = it.updatedAt
                )
            },
            creditCards = database.creditCardDao().getAllOnce().map {
                CreditCardBackupDto(
                    id = it.id,
                    name = it.name,
                    institution = it.institution,
                    totalLimitCents = it.totalLimitCents,
                    availableLimitCents = it.availableLimitCents,
                    closingDay = it.closingDay,
                    dueDay = it.dueDay,
                    colorHex = it.colorHex,
                    notes = it.notes,
                    createdAt = it.createdAt,
                    updatedAt = it.updatedAt
                )
            },
            creditCardStatements = database.creditCardStatementDao().getAllOnce().map {
                CreditCardStatementBackupDto(
                    id = it.id,
                    creditCardId = it.creditCardId,
                    yearMonth = it.yearMonth,
                    closingAt = it.closingAt,
                    dueAt = it.dueAt,
                    status = it.status.name,
                    paidAt = it.paidAt,
                    paidAmountCents = it.paidAmountCents,
                    createdAt = it.createdAt,
                    updatedAt = it.updatedAt
                )
            },
            installmentPurchases = database.installmentPurchaseDao().getAllOnce().map {
                InstallmentPurchaseBackupDto(
                    id = it.id,
                    description = it.description,
                    totalAmountCents = it.totalAmountCents,
                    installmentsCount = it.installmentsCount,
                    installmentAmountCents = it.installmentAmountCents,
                    firstDueAt = it.firstDueAt,
                    creditCardId = it.creditCardId,
                    categoryId = it.categoryId,
                    eventId = it.eventId,
                    projectId = it.projectId,
                    notes = it.notes,
                    createdAt = it.createdAt,
                    updatedAt = it.updatedAt
                )
            },
            recurringRules = database.recurringRuleDao().getAllOnce().map {
                RecurringRuleBackupDto(
                    id = it.id,
                    title = it.title,
                    type = it.type.name,
                    amountCents = it.amountCents,
                    categoryId = it.categoryId,
                    paymentMethod = it.paymentMethod.name,
                    creditCardId = it.creditCardId,
                    frequency = it.frequency.name,
                    customIntervalDays = it.customIntervalDays,
                    startAt = it.startAt,
                    endAt = it.endAt,
                    maxOccurrences = it.maxOccurrences,
                    isActive = it.isActive,
                    notes = it.notes,
                    createdAt = it.createdAt,
                    updatedAt = it.updatedAt
                )
            },
            transactions = database.transactionDao().getAllOnce().map {
                TransactionBackupDto(
                    id = it.id,
                    type = it.type.name,
                    description = it.description,
                    amountCents = it.amountCents,
                    categoryId = it.categoryId,
                    status = it.status.name,
                    paymentMethod = it.paymentMethod.name,
                    occurredAt = it.occurredAt,
                    dueAt = it.dueAt,
                    settledAt = it.settledAt,
                    projectId = it.projectId,
                    eventId = it.eventId,
                    taskId = it.taskId,
                    eventCostType = it.eventCostType?.name,
                    isTicketObligation = it.isTicketObligation,
                    creditCardId = it.creditCardId,
                    statementId = it.statementId,
                    installmentPurchaseId = it.installmentPurchaseId,
                    installmentNumber = it.installmentNumber,
                    installmentsTotal = it.installmentsTotal,
                    recurringRuleId = it.recurringRuleId,
                    occurrenceIndex = it.occurrenceIndex,
                    notes = it.notes,
                    createdAt = it.createdAt,
                    updatedAt = it.updatedAt
                )
            },
            loans = database.loanDao().getAllOnce().map {
                LoanBackupDto(
                    id = it.id,
                    creditor = it.creditor,
                    originalAmountCents = it.originalAmountCents,
                    contractedAmountCents = it.contractedAmountCents,
                    installmentsTotal = it.installmentsTotal,
                    installmentAmountCents = it.installmentAmountCents,
                    paidInstallments = it.paidInstallments,
                    nextDueAt = it.nextDueAt,
                    remainingBalanceCents = it.remainingBalanceCents,
                    finalDateAt = it.finalDateAt,
                    notes = it.notes,
                    createdAt = it.createdAt,
                    updatedAt = it.updatedAt
                )
            },
            budgets = database.budgetDao().getAllOnce().map {
                BudgetBackupDto(
                    id = it.id,
                    categoryId = it.categoryId,
                    yearMonth = it.yearMonth,
                    limitCents = it.limitCents,
                    createdAt = it.createdAt,
                    updatedAt = it.updatedAt
                )
            },
            financialGoals = database.financialGoalDao().getAllOnce().map {
                FinancialGoalBackupDto(
                    id = it.id,
                    name = it.name,
                    targetAmountCents = it.targetAmountCents,
                    currentAmountCents = it.currentAmountCents,
                    deadlineAt = it.deadlineAt,
                    categoryId = it.categoryId,
                    notes = it.notes,
                    createdAt = it.createdAt,
                    updatedAt = it.updatedAt
                )
            },
            auditHistory = database.auditHistoryDao().getAllOnce().map {
                AuditHistoryBackupDto(
                    id = it.id,
                    entityType = it.entityType,
                    entityId = it.entityId,
                    action = it.action,
                    summary = it.summary,
                    occurredAt = it.occurredAt,
                    createdAt = it.createdAt,
                    updatedAt = it.updatedAt
                )
            },
            errorLogs = database.errorLogDao().getAllOnce().map {
                ErrorLogBackupDto(
                    id = it.id,
                    source = it.source,
                    message = it.message,
                    stackTrace = it.stackTrace,
                    occurredAt = it.occurredAt,
                    createdAt = it.createdAt,
                    updatedAt = it.updatedAt
                )
            },
            reminders = database.reminderDao().getAllOnce().map {
                ReminderBackupDto(
                    id = it.id,
                    sourceType = it.sourceType.name,
                    sourceId = it.sourceId,
                    category = it.category.name,
                    title = it.title,
                    message = it.message,
                    triggerAt = it.triggerAt,
                    originalDueAt = it.originalDueAt,
                    minutesBefore = it.minutesBefore,
                    channelId = it.channelId,
                    status = it.status.name,
                    snoozedUntil = it.snoozedUntil,
                    isExactScheduled = it.isExactScheduled,
                    firedAt = it.firedAt,
                    createdAt = it.createdAt,
                    updatedAt = it.updatedAt
                )
            },
            routineSettings = database.routineAssistantSettingsDao().getAllOnce().map {
                RoutineAssistantSettingsBackupDto(
                    id = it.id,
                    notificationsEnabled = it.notificationsEnabled,
                    scheduleChannelEnabled = it.scheduleChannelEnabled,
                    financeChannelEnabled = it.financeChannelEnabled,
                    eventsChannelEnabled = it.eventsChannelEnabled,
                    routineChannelEnabled = it.routineChannelEnabled,
                    morningBriefingEnabled = it.morningBriefingEnabled,
                    morningBriefingHour = it.morningBriefingHour,
                    morningBriefingMinute = it.morningBriefingMinute,
                    nightReviewEnabled = it.nightReviewEnabled,
                    nightReviewHour = it.nightReviewHour,
                    nightReviewMinute = it.nightReviewMinute,
                    quietHoursEnabled = it.quietHoursEnabled,
                    quietHoursStartHour = it.quietHoursStartHour,
                    quietHoursStartMinute = it.quietHoursStartMinute,
                    quietHoursEndHour = it.quietHoursEndHour,
                    quietHoursEndMinute = it.quietHoursEndMinute,
                    defaultTaskReminderMinutes = it.defaultTaskReminderMinutes,
                    defaultAppointmentReminderMinutes = it.defaultAppointmentReminderMinutes,
                    defaultEventReminderMinutes = it.defaultEventReminderMinutes,
                    defaultFinanceReminderMinutes = it.defaultFinanceReminderMinutes,
                    createdAt = it.createdAt,
                    updatedAt = it.updatedAt
                )
            },
            briefingHistory = database.briefingHistoryDao().getAllOnce().map {
                BriefingHistoryBackupDto(
                    id = it.id,
                    briefingType = it.briefingType.name,
                    dateIso = it.dateIso,
                    summaryText = it.summaryText,
                    tasksCount = it.tasksCount,
                    appointmentsCount = it.appointmentsCount,
                    billsDueCount = it.billsDueCount,
                    overdueCount = it.overdueCount,
                    reviewedAt = it.reviewedAt,
                    createdAt = it.createdAt,
                    updatedAt = it.updatedAt
                )
            },
            personalizationSettings = database.personalizationSettingsDao().getAllOnce().map {
                PersonalizationSettingsBackupDto(
                    id = it.id,
                    greetingEnabled = it.greetingEnabled,
                    greetingTemplate = it.greetingTemplate,
                    dateFormatPattern = it.dateFormatPattern.name,
                    showDailyQuoteOnDashboard = it.showDailyQuoteOnDashboard,
                    compactDashboardMode = it.compactDashboardMode,
                    showMetricSummaryOnDashboard = it.showMetricSummaryOnDashboard,
                    dashboardBlocksJson = it.dashboardBlocksJson,
                    lastQuoteDateIso = it.lastQuoteDateIso,
                    lastQuoteId = it.lastQuoteId,
                    previousQuoteId = it.previousQuoteId,
                    createdAt = it.createdAt,
                    updatedAt = it.updatedAt
                )
            },
            quotes = database.quoteDao().getAllOnce().map {
                QuoteBackupDto(
                    id = it.id,
                    text = it.text,
                    author = it.author,
                    isActive = it.isActive,
                    lastShownDateIso = it.lastShownDateIso,
                    shownCount = it.shownCount,
                    createdAt = it.createdAt,
                    updatedAt = it.updatedAt
                )
            },
            personalGoals = database.personalGoalDao().getAllOnce().map {
                PersonalGoalBackupDto(
                    id = it.id,
                    title = it.title,
                    description = it.description,
                    categoryId = it.categoryId,
                    targetDateAt = it.targetDateAt,
                    status = it.status.name,
                    progressMode = it.progressMode.name,
                    manualProgressPercent = it.manualProgressPercent,
                    completedAt = it.completedAt,
                    notes = it.notes,
                    createdAt = it.createdAt,
                    updatedAt = it.updatedAt
                )
            },
            goalMilestones = database.goalMilestoneDao().getAllOnce().map {
                GoalMilestoneBackupDto(
                    id = it.id,
                    goalId = it.goalId,
                    title = it.title,
                    description = it.description,
                    dueAt = it.dueAt,
                    isCompleted = it.isCompleted,
                    completedAt = it.completedAt,
                    sortOrder = it.sortOrder,
                    createdAt = it.createdAt,
                    updatedAt = it.updatedAt
                )
            },
            goalProgressLogs = database.goalProgressLogDao().getAllOnce().map {
                GoalProgressLogBackupDto(
                    id = it.id,
                    goalId = it.goalId,
                    progressPercent = it.progressPercent,
                    note = it.note,
                    recordedAt = it.recordedAt,
                    createdAt = it.createdAt,
                    updatedAt = it.updatedAt
                )
            },
            recurrences = database.taskRecurrenceDao().getAllOnce().map {
                TaskRecurrenceBackupDto(
                    id = it.id,
                    title = it.title,
                    description = it.description,
                    projectId = it.projectId,
                    categoryId = it.categoryId,
                    eventId = it.eventId,
                    goalId = it.goalId,
                    priority = it.priority.name,
                    notes = it.notes,
                    hasDueTime = it.hasDueTime,
                    dueTimeMinutesOfDay = it.dueTimeMinutesOfDay,
                    reminderMinutesBefore = it.reminderMinutesBefore,
                    frequency = it.frequency.name,
                    customIntervalDays = it.customIntervalDays,
                    weekdaysMask = it.weekdaysMask,
                    startDateIso = it.startDateIso,
                    endDateIso = it.endDateIso,
                    maxOccurrences = it.maxOccurrences,
                    isActive = it.isActive,
                    createdAt = it.createdAt,
                    updatedAt = it.updatedAt
                )
            },
            recurrenceExceptions = database.recurrenceExceptionDao().getAllOnce().map {
                RecurrenceExceptionBackupDto(
                    id = it.id,
                    recurrenceId = it.recurrenceId,
                    occurrenceDateIso = it.occurrenceDateIso,
                    exceptionType = it.exceptionType.name,
                    overrideTitle = it.overrideTitle,
                    overrideDueAt = it.overrideDueAt,
                    taskId = it.taskId,
                    createdAt = it.createdAt,
                    updatedAt = it.updatedAt
                )
            },
            marketConfig = exportMarketConfig()
        )
        return tolerantJson.encodeToString(payload)
    }

    private fun exportMarketConfig(): MarketBackupConfigDto? {
        if (marketWatchlistRepository == null && marketAlertRepository == null) return null
        val favorites = marketWatchlistRepository?.favoriteCodes?.value
        val alerts = marketAlertRepository?.alerts?.value?.map { alert ->
            val descriptor = MarketAssetCatalog.findByCode(alert.assetCode)
            MarketPriceAlertBackupDto(
                id = alert.id,
                assetCode = alert.assetCode,
                condition = alert.condition.name,
                targetPrice = alert.targetPrice,
                currencyOrUnit = descriptor?.let { MarketAlertRules.unitLabelFor(it) }.orEmpty(),
                isActive = alert.isActive,
                status = alert.status.name,
                createdAtEpochMs = alert.createdAtEpochMs,
                updatedAtEpochMs = alert.updatedAtEpochMs,
                triggeredAtEpochMs = alert.triggeredAtEpochMs,
                triggeredPrice = alert.triggeredPrice,
                triggeredQuoteSource = alert.triggeredQuoteSource,
                triggeredQuoteTimestampEpochMs = alert.triggeredQuoteTimestampEpochMs,
                awaitingConditionReset = alert.awaitingConditionReset,
                lastEvaluatedQuoteFetchedAtEpochMs = alert.lastEvaluatedQuoteFetchedAtEpochMs
            )
        }
        return MarketBackupConfigDto(
            configVersion = MarketBackupConfigDto.CURRENT_MARKET_CONFIG_VERSION,
            favoriteAssetCodes = favorites,
            priceAlerts = alerts
        )
    }

    suspend fun exportFinancialCsv(): String {
        val transactions = database.transactionDao().getAllOnce()
        val categories = database.categoryDao().getAllOnce().associateBy { it.id }
        val projects = database.projectDao().getAllOnce().associateBy { it.id }
        val events = database.eventDao().getAllOnce().associateBy { it.id }
        val cards = database.creditCardDao().getAllOnce().associateBy { it.id }

        fun csvEscape(value: String): String {
            val clean = value.replace("\r", " ").replace("\n", " ")
            return "\"${clean.replace("\"", "\"\"")}\""
        }

        return buildString {
            appendLine("id,tipo,descricao,valor_brl,situacao,metodo,categoria,data_ocorrencia,vencimento,data_liquidacao,cartao,parcela,projeto,evento,observacoes")
            transactions.forEach { tx ->
                val amountFormatted = String.format(Locale.US, "%.2f", tx.amountCents / 100.0)
                val catName = tx.categoryId?.let { categories[it]?.name }.orEmpty()
                val projName = tx.projectId?.let { projects[it]?.name }.orEmpty()
                val eventName = tx.eventId?.let { events[it]?.name }.orEmpty()
                val cardName = tx.creditCardId?.let { cards[it]?.name }.orEmpty()
                val installmentText = if (tx.installmentNumber != null && tx.installmentsTotal != null) {
                    "${tx.installmentNumber}/${tx.installmentsTotal}"
                } else ""

                val cols = listOf(
                    tx.id,
                    tx.type.label,
                    tx.description,
                    amountFormatted,
                    tx.status.label,
                    tx.paymentMethod.label,
                    catName,
                    DateTimeUtils.formatDate(tx.occurredAt),
                    tx.dueAt?.let { DateTimeUtils.formatDate(it) }.orEmpty(),
                    tx.settledAt?.let { DateTimeUtils.formatDate(it) }.orEmpty(),
                    cardName,
                    installmentText,
                    projName,
                    eventName,
                    tx.notes
                ).joinToString(",") { csvEscape(it) }
                appendLine(cols)
            }
        }
    }

    fun validateAndParse(rawJson: String): BackupValidationResult {
        if (rawJson.isBlank()) {
            return BackupValidationResult.Invalid("O arquivo selecionado está vazio.")
        }

        return try {
            val element = tolerantJson.parseToJsonElement(rawJson)
            if (element !is JsonObject) {
                return BackupValidationResult.Invalid("Arquivo inválido: o conteúdo não é um objeto JSON do NEXUS.")
            }

            val rootKeys = element.jsonObject.keys
            val recognizedKeys = setOf(
                "formatVersion", "appVersion", "exportedAt", "databaseVersion",
                "settings", "categories", "projects", "events", "tasks",
                "subtasks", "appointments", "creditCards", "creditCardStatements",
                "installmentPurchases", "recurringRules", "transactions",
                "loans", "budgets", "financialGoals", "auditHistory", "errorLogs",
                "reminders", "routineSettings", "briefingHistory",
                "personalizationSettings", "quotes", "personalGoals",
                "goalMilestones", "goalProgressLogs",
                "recurrences", "recurrenceExceptions", "marketConfig"
            )
            if (rootKeys.none { it in recognizedKeys }) {
                return BackupValidationResult.Invalid("Este JSON não parece ser um backup válido do NEXUS.")
            }

            val payload = tolerantJson.decodeFromString<NexusBackupPayload>(rawJson)
            if (payload.formatVersion < 1) {
                return BackupValidationResult.Invalid("Versão de formato de backup inválida (${payload.formatVersion}).")
            }

            val unknownTopKeys = rootKeys - recognizedKeys
            val notice = buildString {
                if (payload.formatVersion < NexusBackupPayload.CURRENT_FORMAT_VERSION) {
                    append("Backup de versão anterior (v${payload.formatVersion}) convertido automaticamente com valores padrão.")
                } else if (payload.formatVersion > NexusBackupPayload.CURRENT_FORMAT_VERSION) {
                    append("Backup de versão mais recente (v${payload.formatVersion}). Campos desconhecidos serão ignorados com segurança.")
                }
                if (unknownTopKeys.isNotEmpty()) {
                    if (isNotEmpty()) append(" ")
                    append("Blocos extras (${unknownTopKeys.joinToString(", ")}) serão ignorados.")
                }
            }.ifBlank { null }

            val now = DateTimeUtils.nowEpochMillis()
            val previewFavorites = payload.marketConfig?.favoriteAssetCodes?.let { sanitizeFavoriteCodes(it) }
            val previewAlerts = payload.marketConfig?.priceAlerts?.let { sanitizePriceAlerts(it, now) }

            BackupValidationResult.Valid(
                BackupPreview(
                    payload = payload,
                    formatVersion = payload.formatVersion,
                    appVersion = payload.appVersion.ifBlank { "Desconhecida" },
                    exportedAt = payload.exportedAt.ifBlank { "Data não informada" },
                    settingsCount = payload.settings.size,
                    categoriesCount = payload.categories.size,
                    projectsCount = payload.projects.size,
                    eventsCount = payload.events.size,
                    tasksCount = payload.tasks.size,
                    subtasksCount = payload.subtasks.size,
                    appointmentsCount = payload.appointments.size,
                    creditCardsCount = payload.creditCards.size,
                    statementsCount = payload.creditCardStatements.size,
                    installmentPurchasesCount = payload.installmentPurchases.size,
                    recurringRulesCount = payload.recurringRules.size,
                    transactionsCount = payload.transactions.size,
                    loansCount = payload.loans.size,
                    budgetsCount = payload.budgets.size,
                    financialGoalsCount = payload.financialGoals.size,
                    auditHistoryCount = payload.auditHistory.size,
                    errorLogsCount = payload.errorLogs.size,
                    remindersCount = payload.reminders.size,
                    routineSettingsCount = payload.routineSettings.size,
                    briefingHistoryCount = payload.briefingHistory.size,
                    personalizationSettingsCount = payload.personalizationSettings.size,
                    quotesCount = payload.quotes.size,
                    personalGoalsCount = payload.personalGoals.size,
                    goalMilestonesCount = payload.goalMilestones.size,
                    goalProgressLogsCount = payload.goalProgressLogs.size,
                    recurrencesCount = payload.recurrences.size,
                    recurrenceExceptionsCount = payload.recurrenceExceptions.size,
                    marketFavoritesCount = previewFavorites?.size ?: 0,
                    marketAlertsCount = previewAlerts?.size ?: 0,
                    includesMarketConfig = payload.marketConfig != null &&
                        (previewFavorites != null || previewAlerts != null),
                    compatibilityNotice = notice
                )
            )
        } catch (ex: Exception) {
            BackupValidationResult.Invalid(
                "Não foi possível ler o arquivo de backup. Verifique se o JSON está íntegro. (${ex.localizedMessage ?: "erro de leitura"})"
            )
        }
    }

    suspend fun restoreAtomically(payload: NexusBackupPayload): RestoreSummary {
        val now = DateTimeUtils.nowEpochMillis()

        val settingsEntities = if (payload.settings.isNotEmpty()) {
            payload.settings.take(1).map { dto ->
                SettingsEntity(
                    id = SettingsEntity.SINGLETON_ID,
                    userName = dto.userName.ifBlank { "Enzo" },
                    darkTheme = dto.darkTheme,
                    declaredBalanceCents = dto.declaredBalanceCents,
                    createdAt = if (dto.createdAt > 0L) dto.createdAt else now,
                    updatedAt = if (dto.updatedAt > 0L) dto.updatedAt else now
                )
            }
        } else {
            listOf(
                SettingsEntity(
                    id = SettingsEntity.SINGLETON_ID,
                    userName = "Enzo",
                    darkTheme = true,
                    declaredBalanceCents = 0L,
                    createdAt = now,
                    updatedAt = now
                )
            )
        }

        val categoryEntities = payload.categories
            .filter { it.name.isNotBlank() }
            .map { dto ->
                CategoryEntity(
                    id = dto.id.ifBlank { UUID.randomUUID().toString() },
                    name = dto.name.trim(),
                    colorHex = dto.colorHex.ifBlank { "#9A9A9A" },
                    scope = CategoryScope.fromStringOrDefault(dto.scope),
                    isDefault = dto.isDefault,
                    createdAt = if (dto.createdAt > 0L) dto.createdAt else now,
                    updatedAt = if (dto.updatedAt > 0L) dto.updatedAt else now
                )
            }.distinctBy { it.id }
        val validCategoryIds = categoryEntities.map { it.id }.toSet()

        val personalGoalEntities = payload.personalGoals
            .filter { it.title.isNotBlank() }
            .map { dto ->
                PersonalGoalEntity(
                    id = dto.id.ifBlank { UUID.randomUUID().toString() },
                    title = dto.title.trim(),
                    description = dto.description,
                    categoryId = dto.categoryId?.takeIf { it in validCategoryIds },
                    targetDateAt = dto.targetDateAt,
                    status = GoalStatus.fromStringOrDefault(dto.status),
                    progressMode = GoalProgressMode.fromStringOrDefault(dto.progressMode),
                    manualProgressPercent = dto.manualProgressPercent.coerceIn(0, 100),
                    completedAt = dto.completedAt,
                    notes = dto.notes,
                    createdAt = if (dto.createdAt > 0L) dto.createdAt else now,
                    updatedAt = if (dto.updatedAt > 0L) dto.updatedAt else now
                )
            }.distinctBy { it.id }
        val validPersonalGoalIds = personalGoalEntities.map { it.id }.toSet()

        val projectEntities = payload.projects
            .filter { it.name.isNotBlank() }
            .map { dto ->
                ProjectEntity(
                    id = dto.id.ifBlank { UUID.randomUUID().toString() },
                    name = dto.name.trim(),
                    description = dto.description,
                    deadlineAt = dto.deadlineAt,
                    status = ProjectStatus.fromStringOrDefault(dto.status),
                    priority = TaskPriority.fromStringOrDefault(dto.priority),
                    goalId = dto.goalId?.takeIf { it in validPersonalGoalIds },
                    createdAt = if (dto.createdAt > 0L) dto.createdAt else now,
                    updatedAt = if (dto.updatedAt > 0L) dto.updatedAt else now
                )
            }.distinctBy { it.id }
        val validProjectIds = projectEntities.map { it.id }.toSet()

        val cardEntities = payload.creditCards
            .filter { it.name.isNotBlank() }
            .map { dto ->
                CreditCardEntity(
                    id = dto.id.ifBlank { UUID.randomUUID().toString() },
                    name = dto.name.trim(),
                    institution = dto.institution,
                    totalLimitCents = dto.totalLimitCents,
                    availableLimitCents = dto.availableLimitCents,
                    closingDay = dto.closingDay.coerceIn(1, 31),
                    dueDay = dto.dueDay.coerceIn(1, 31),
                    colorHex = dto.colorHex.ifBlank { "#9A9A9A" },
                    notes = dto.notes,
                    createdAt = if (dto.createdAt > 0L) dto.createdAt else now,
                    updatedAt = if (dto.updatedAt > 0L) dto.updatedAt else now
                )
            }.distinctBy { it.id }
        val validCardIds = cardEntities.map { it.id }.toSet()

        val eventEntities = payload.events
            .filter { it.name.isNotBlank() }
            .map { dto ->
                val start = if (dto.startAt > 0L) dto.startAt else now
                val end = if (dto.endAt >= start) dto.endAt else start + 3_600_000L
                EventEntity(
                    id = dto.id.ifBlank { UUID.randomUUID().toString() },
                    name = dto.name.trim(),
                    description = dto.description,
                    categoryId = dto.categoryId?.takeIf { it in validCategoryIds },
                    startAt = start,
                    endAt = end,
                    isAllDay = dto.isAllDay,
                    officialLink = dto.officialLink,
                    ticketLink = dto.ticketLink,
                    locationName = dto.locationName,
                    address = dto.address,
                    city = dto.city,
                    state = dto.state,
                    notes = dto.notes,
                    eventStatus = EventLifecycleStatus.fromStringOrDefault(dto.eventStatus),
                    ticketStatus = TicketStatus.fromStringOrDefault(dto.ticketStatus),
                    ticketPriceCents = dto.ticketPriceCents,
                    ticketQuantity = dto.ticketQuantity.coerceAtLeast(1),
                    ticketFeesCents = dto.ticketFeesCents,
                    ticketTotalCents = dto.ticketTotalCents,
                    ticketPurchaseDate = dto.ticketPurchaseDate,
                    ticketDueDate = dto.ticketDueDate,
                    paymentMethod = PaymentMethod.fromStringOrDefault(dto.paymentMethod),
                    creditCardId = dto.creditCardId?.takeIf { it in validCardIds },
                    installmentsCount = dto.installmentsCount.coerceAtLeast(1),
                    createdAt = if (dto.createdAt > 0L) dto.createdAt else now,
                    updatedAt = if (dto.updatedAt > 0L) dto.updatedAt else now
                )
            }.distinctBy { it.id }
        val validEventIds = eventEntities.map { it.id }.toSet()

        val taskRecurrenceEntities = payload.recurrences
            .filter { it.title.isNotBlank() }
            .map { dto ->
                TaskRecurrenceEntity(
                    id = dto.id.ifBlank { UUID.randomUUID().toString() },
                    title = dto.title.trim(),
                    description = dto.description,
                    projectId = dto.projectId?.takeIf { it in validProjectIds },
                    categoryId = dto.categoryId?.takeIf { it in validCategoryIds },
                    eventId = dto.eventId?.takeIf { it in validEventIds },
                    goalId = dto.goalId?.takeIf { it in validPersonalGoalIds },
                    priority = TaskPriority.fromStringOrDefault(dto.priority),
                    notes = dto.notes,
                    hasDueTime = dto.hasDueTime,
                    dueTimeMinutesOfDay = dto.dueTimeMinutesOfDay?.coerceIn(0, 23 * 60 + 59),
                    reminderMinutesBefore = dto.reminderMinutesBefore?.coerceAtLeast(-1),
                    frequency = TaskRecurrenceFrequency.fromStringOrDefault(dto.frequency),
                    customIntervalDays = dto.customIntervalDays.coerceAtLeast(1),
                    weekdaysMask = dto.weekdaysMask,
                    startDateIso = dto.startDateIso.ifBlank { DateTimeUtils.todayLocalDate().toString() },
                    endDateIso = dto.endDateIso?.takeIf { it.isNotBlank() },
                    maxOccurrences = dto.maxOccurrences?.takeIf { it > 0 },
                    isActive = dto.isActive,
                    createdAt = if (dto.createdAt > 0L) dto.createdAt else now,
                    updatedAt = if (dto.updatedAt > 0L) dto.updatedAt else now
                )
            }.distinctBy { it.id }
        val validRecurrenceIds = taskRecurrenceEntities.map { it.id }.toSet()

        val seenTaskRecurrenceKeys = mutableSetOf<Pair<String, String>>()
        val taskEntities = payload.tasks
            .filter { it.title.isNotBlank() }
            .map { dto ->
                val rawRecId = dto.recurrenceId?.takeIf { it in validRecurrenceIds }
                val rawOccIso = dto.occurrenceDateIso?.takeIf { it.isNotBlank() }
                val keepRecLink = if (rawRecId != null && rawOccIso != null) {
                    seenTaskRecurrenceKeys.add(rawRecId to rawOccIso)
                } else {
                    rawRecId != null
                }
                TaskEntity(
                    id = dto.id.ifBlank { UUID.randomUUID().toString() },
                    title = dto.title.trim(),
                    description = dto.description,
                    projectId = dto.projectId?.takeIf { it in validProjectIds },
                    categoryId = dto.categoryId?.takeIf { it in validCategoryIds },
                    eventId = dto.eventId?.takeIf { it in validEventIds },
                    goalId = dto.goalId?.takeIf { it in validPersonalGoalIds },
                    priority = TaskPriority.fromStringOrDefault(dto.priority),
                    status = TaskStatus.fromStringOrDefault(dto.status),
                    dueAt = dto.dueAt,
                    hasDueTime = dto.hasDueTime,
                    notes = dto.notes,
                    completedAt = dto.completedAt,
                    recurrenceId = if (keepRecLink) rawRecId else null,
                    occurrenceDateIso = if (keepRecLink) rawOccIso else null,
                    createdAt = if (dto.createdAt > 0L) dto.createdAt else now,
                    updatedAt = if (dto.updatedAt > 0L) dto.updatedAt else now
                )
            }.distinctBy { it.id }
        val validTaskIds = taskEntities.map { it.id }.toSet()

        val subtaskEntities = payload.subtasks
            .filter { it.title.isNotBlank() && it.taskId in validTaskIds }
            .map { dto ->
                SubtaskEntity(
                    id = dto.id.ifBlank { UUID.randomUUID().toString() },
                    taskId = dto.taskId,
                    title = dto.title.trim(),
                    isCompleted = dto.isCompleted,
                    sortOrder = dto.sortOrder,
                    createdAt = if (dto.createdAt > 0L) dto.createdAt else now,
                    updatedAt = if (dto.updatedAt > 0L) dto.updatedAt else now
                )
            }.distinctBy { it.id }

        val appointmentEntities = payload.appointments
            .filter { it.title.isNotBlank() }
            .map { dto ->
                val start = if (dto.startAt > 0L) dto.startAt else now
                val end = if (dto.endAt >= start) dto.endAt else start + 3_600_000L
                AppointmentEntity(
                    id = dto.id.ifBlank { UUID.randomUUID().toString() },
                    title = dto.title.trim(),
                    description = dto.description,
                    categoryId = dto.categoryId?.takeIf { it in validCategoryIds },
                    eventId = dto.eventId?.takeIf { it in validEventIds },
                    startAt = start,
                    endAt = end,
                    isAllDay = dto.isAllDay,
                    location = dto.location,
                    link = dto.link,
                    createdAt = if (dto.createdAt > 0L) dto.createdAt else now,
                    updatedAt = if (dto.updatedAt > 0L) dto.updatedAt else now
                )
            }.distinctBy { it.id }

        val statementEntities = payload.creditCardStatements
            .filter { it.creditCardId in validCardIds && it.yearMonth.isNotBlank() }
            .map { dto ->
                CreditCardStatementEntity(
                    id = dto.id.ifBlank { UUID.randomUUID().toString() },
                    creditCardId = dto.creditCardId,
                    yearMonth = dto.yearMonth,
                    closingAt = dto.closingAt,
                    dueAt = dto.dueAt,
                    status = StatementStatus.fromStringOrDefault(dto.status),
                    paidAt = dto.paidAt,
                    paidAmountCents = dto.paidAmountCents,
                    createdAt = if (dto.createdAt > 0L) dto.createdAt else now,
                    updatedAt = if (dto.updatedAt > 0L) dto.updatedAt else now
                )
            }.distinctBy { it.id }
            .distinctBy { it.creditCardId to it.yearMonth }
        val validStatementIds = statementEntities.map { it.id }.toSet()

        val installmentPurchaseEntities = payload.installmentPurchases
            .filter { it.description.isNotBlank() }
            .map { dto ->
                InstallmentPurchaseEntity(
                    id = dto.id.ifBlank { UUID.randomUUID().toString() },
                    description = dto.description.trim(),
                    totalAmountCents = dto.totalAmountCents,
                    installmentsCount = dto.installmentsCount.coerceAtLeast(1),
                    installmentAmountCents = dto.installmentAmountCents,
                    firstDueAt = if (dto.firstDueAt > 0L) dto.firstDueAt else now,
                    creditCardId = dto.creditCardId?.takeIf { it in validCardIds },
                    categoryId = dto.categoryId?.takeIf { it in validCategoryIds },
                    eventId = dto.eventId?.takeIf { it in validEventIds },
                    projectId = dto.projectId?.takeIf { it in validProjectIds },
                    notes = dto.notes,
                    createdAt = if (dto.createdAt > 0L) dto.createdAt else now,
                    updatedAt = if (dto.updatedAt > 0L) dto.updatedAt else now
                )
            }.distinctBy { it.id }
        val validPurchaseIds = installmentPurchaseEntities.map { it.id }.toSet()

        val recurringRuleEntities = payload.recurringRules
            .filter { it.title.isNotBlank() }
            .map { dto ->
                RecurringRuleEntity(
                    id = dto.id.ifBlank { UUID.randomUUID().toString() },
                    title = dto.title.trim(),
                    type = TransactionType.fromStringOrDefault(dto.type),
                    amountCents = dto.amountCents,
                    categoryId = dto.categoryId?.takeIf { it in validCategoryIds },
                    paymentMethod = PaymentMethod.fromStringOrDefault(dto.paymentMethod),
                    creditCardId = dto.creditCardId?.takeIf { it in validCardIds },
                    frequency = RecurrenceFrequency.fromStringOrDefault(dto.frequency),
                    customIntervalDays = dto.customIntervalDays.coerceAtLeast(1),
                    startAt = if (dto.startAt > 0L) dto.startAt else now,
                    endAt = dto.endAt,
                    maxOccurrences = dto.maxOccurrences,
                    isActive = dto.isActive,
                    notes = dto.notes,
                    createdAt = if (dto.createdAt > 0L) dto.createdAt else now,
                    updatedAt = if (dto.updatedAt > 0L) dto.updatedAt else now
                )
            }.distinctBy { it.id }
        val validRuleIds = recurringRuleEntities.map { it.id }.toSet()

        val transactionEntities = payload.transactions
            .filter { it.description.isNotBlank() }
            .map { dto ->
                val validRule = dto.recurringRuleId?.takeIf { it in validRuleIds }
                TransactionEntity(
                    id = dto.id.ifBlank { UUID.randomUUID().toString() },
                    type = TransactionType.fromStringOrDefault(dto.type),
                    description = dto.description.trim(),
                    amountCents = dto.amountCents,
                    categoryId = dto.categoryId?.takeIf { it in validCategoryIds },
                    status = FinancialStatus.fromStringOrDefault(dto.status),
                    paymentMethod = PaymentMethod.fromStringOrDefault(dto.paymentMethod),
                    occurredAt = if (dto.occurredAt > 0L) dto.occurredAt else now,
                    dueAt = dto.dueAt,
                    settledAt = dto.settledAt,
                    projectId = dto.projectId?.takeIf { it in validProjectIds },
                    eventId = dto.eventId?.takeIf { it in validEventIds },
                    taskId = dto.taskId?.takeIf { it in validTaskIds },
                    eventCostType = dto.eventCostType?.let { EventCostType.fromStringOrDefault(it) },
                    isTicketObligation = dto.isTicketObligation,
                    creditCardId = dto.creditCardId?.takeIf { it in validCardIds },
                    statementId = dto.statementId?.takeIf { it in validStatementIds },
                    installmentPurchaseId = dto.installmentPurchaseId?.takeIf { it in validPurchaseIds },
                    installmentNumber = dto.installmentNumber,
                    installmentsTotal = dto.installmentsTotal,
                    recurringRuleId = validRule,
                    occurrenceIndex = if (validRule != null) dto.occurrenceIndex else null,
                    notes = dto.notes,
                    createdAt = if (dto.createdAt > 0L) dto.createdAt else now,
                    updatedAt = if (dto.updatedAt > 0L) dto.updatedAt else now
                )
            }.distinctBy { it.id }

        val loanEntities = payload.loans
            .filter { it.creditor.isNotBlank() }
            .map { dto ->
                LoanEntity(
                    id = dto.id.ifBlank { UUID.randomUUID().toString() },
                    creditor = dto.creditor.trim(),
                    originalAmountCents = dto.originalAmountCents,
                    contractedAmountCents = dto.contractedAmountCents,
                    installmentsTotal = dto.installmentsTotal.coerceAtLeast(1),
                    installmentAmountCents = dto.installmentAmountCents,
                    paidInstallments = dto.paidInstallments.coerceAtLeast(0),
                    nextDueAt = dto.nextDueAt,
                    remainingBalanceCents = dto.remainingBalanceCents.coerceAtLeast(0L),
                    finalDateAt = dto.finalDateAt,
                    notes = dto.notes,
                    createdAt = if (dto.createdAt > 0L) dto.createdAt else now,
                    updatedAt = if (dto.updatedAt > 0L) dto.updatedAt else now
                )
            }.distinctBy { it.id }

        val budgetEntities = payload.budgets
            .filter { it.categoryId in validCategoryIds && it.yearMonth.isNotBlank() }
            .map { dto ->
                BudgetEntity(
                    id = dto.id.ifBlank { UUID.randomUUID().toString() },
                    categoryId = dto.categoryId,
                    yearMonth = dto.yearMonth,
                    limitCents = dto.limitCents.coerceAtLeast(0L),
                    createdAt = if (dto.createdAt > 0L) dto.createdAt else now,
                    updatedAt = if (dto.updatedAt > 0L) dto.updatedAt else now
                )
            }.distinctBy { it.id }
            .distinctBy { it.categoryId to it.yearMonth }

        val goalEntities = payload.financialGoals
            .filter { it.name.isNotBlank() }
            .map { dto ->
                FinancialGoalEntity(
                    id = dto.id.ifBlank { UUID.randomUUID().toString() },
                    name = dto.name.trim(),
                    targetAmountCents = dto.targetAmountCents.coerceAtLeast(1L),
                    currentAmountCents = dto.currentAmountCents.coerceAtLeast(0L),
                    deadlineAt = dto.deadlineAt,
                    categoryId = dto.categoryId?.takeIf { it in validCategoryIds },
                    notes = dto.notes,
                    createdAt = if (dto.createdAt > 0L) dto.createdAt else now,
                    updatedAt = if (dto.updatedAt > 0L) dto.updatedAt else now
                )
            }.distinctBy { it.id }

        val auditEntities = payload.auditHistory.map { dto ->
            AuditHistoryEntity(
                id = dto.id.ifBlank { UUID.randomUUID().toString() },
                entityType = dto.entityType,
                entityId = dto.entityId,
                action = dto.action,
                summary = dto.summary,
                occurredAt = if (dto.occurredAt > 0L) dto.occurredAt else now,
                createdAt = if (dto.createdAt > 0L) dto.createdAt else now,
                updatedAt = if (dto.updatedAt > 0L) dto.updatedAt else now
            )
        }.distinctBy { it.id }

        val errorLogEntities = payload.errorLogs.map { dto ->
            ErrorLogEntity(
                id = dto.id.ifBlank { UUID.randomUUID().toString() },
                source = dto.source.ifBlank { "Backup" },
                message = dto.message,
                stackTrace = dto.stackTrace,
                occurredAt = if (dto.occurredAt > 0L) dto.occurredAt else now,
                createdAt = if (dto.createdAt > 0L) dto.createdAt else now,
                updatedAt = if (dto.updatedAt > 0L) dto.updatedAt else now
            )
        }.distinctBy { it.id }

        val reminderEntities = payload.reminders
            .filter { it.sourceId.isNotBlank() && it.title.isNotBlank() }
            .map { dto ->
                ReminderEntity(
                    id = dto.id.ifBlank { UUID.randomUUID().toString() },
                    sourceType = ReminderSourceType.fromStringOrDefault(dto.sourceType),
                    sourceId = dto.sourceId,
                    category = ReminderCategory.fromStringOrDefault(dto.category),
                    title = dto.title,
                    message = dto.message,
                    triggerAt = if (dto.triggerAt > 0L) dto.triggerAt else now,
                    originalDueAt = dto.originalDueAt,
                    minutesBefore = dto.minutesBefore.coerceAtLeast(0),
                    channelId = dto.channelId.ifBlank { "nexus_schedule" },
                    status = if (dto.minutesBefore < 0) {
                        ReminderStatus.CANCELLED
                    } else {
                        ReminderStatus.fromStringOrDefault(dto.status)
                    },
                    snoozedUntil = dto.snoozedUntil,
                    isExactScheduled = dto.isExactScheduled,
                    firedAt = dto.firedAt,
                    createdAt = if (dto.createdAt > 0L) dto.createdAt else now,
                    updatedAt = if (dto.updatedAt > 0L) dto.updatedAt else now
                )
            }
            .distinctBy { it.id }
            .distinctBy { Triple(it.sourceType, it.sourceId, it.category) }

        val routineSettingsEntities = if (payload.routineSettings.isNotEmpty()) {
            payload.routineSettings.take(1).map { dto ->
                RoutineAssistantSettingsEntity(
                    id = RoutineAssistantSettingsEntity.SINGLETON_ID,
                    notificationsEnabled = dto.notificationsEnabled,
                    scheduleChannelEnabled = dto.scheduleChannelEnabled,
                    financeChannelEnabled = dto.financeChannelEnabled,
                    eventsChannelEnabled = dto.eventsChannelEnabled,
                    routineChannelEnabled = dto.routineChannelEnabled,
                    morningBriefingEnabled = dto.morningBriefingEnabled,
                    morningBriefingHour = dto.morningBriefingHour.coerceIn(0, 23),
                    morningBriefingMinute = dto.morningBriefingMinute.coerceIn(0, 59),
                    nightReviewEnabled = dto.nightReviewEnabled,
                    nightReviewHour = dto.nightReviewHour.coerceIn(0, 23),
                    nightReviewMinute = dto.nightReviewMinute.coerceIn(0, 59),
                    quietHoursEnabled = dto.quietHoursEnabled,
                    quietHoursStartHour = dto.quietHoursStartHour.coerceIn(0, 23),
                    quietHoursStartMinute = dto.quietHoursStartMinute.coerceIn(0, 59),
                    quietHoursEndHour = dto.quietHoursEndHour.coerceIn(0, 23),
                    quietHoursEndMinute = dto.quietHoursEndMinute.coerceIn(0, 59),
                    defaultTaskReminderMinutes = dto.defaultTaskReminderMinutes.coerceAtLeast(0),
                    defaultAppointmentReminderMinutes = dto.defaultAppointmentReminderMinutes.coerceAtLeast(0),
                    defaultEventReminderMinutes = dto.defaultEventReminderMinutes.coerceAtLeast(0),
                    defaultFinanceReminderMinutes = dto.defaultFinanceReminderMinutes.coerceAtLeast(0),
                    createdAt = if (dto.createdAt > 0L) dto.createdAt else now,
                    updatedAt = if (dto.updatedAt > 0L) dto.updatedAt else now
                )
            }
        } else {
            listOf(RoutineAssistantSettingsEntity(createdAt = now, updatedAt = now))
        }

        val briefingHistoryEntities = payload.briefingHistory
            .filter { it.dateIso.isNotBlank() }
            .map { dto ->
                BriefingHistoryEntity(
                    id = dto.id.ifBlank { UUID.randomUUID().toString() },
                    briefingType = BriefingType.fromStringOrDefault(dto.briefingType),
                    dateIso = dto.dateIso,
                    summaryText = dto.summaryText,
                    tasksCount = dto.tasksCount.coerceAtLeast(0),
                    appointmentsCount = dto.appointmentsCount.coerceAtLeast(0),
                    billsDueCount = dto.billsDueCount.coerceAtLeast(0),
                    overdueCount = dto.overdueCount.coerceAtLeast(0),
                    reviewedAt = dto.reviewedAt,
                    createdAt = if (dto.createdAt > 0L) dto.createdAt else now,
                    updatedAt = if (dto.updatedAt > 0L) dto.updatedAt else now
                )
            }
            .distinctBy { it.id }
            .distinctBy { it.briefingType to it.dateIso }

        val personalizationEntities = if (payload.personalizationSettings.isNotEmpty()) {
            payload.personalizationSettings.take(1).map { dto ->
                PersonalizationSettingsEntity(
                    id = PersonalizationSettingsEntity.SINGLETON_ID,
                    greetingEnabled = dto.greetingEnabled,
                    greetingTemplate = dto.greetingTemplate.ifBlank { "AUTO" },
                    dateFormatPattern = NexusDateFormat.fromStringOrDefault(dto.dateFormatPattern),
                    showDailyQuoteOnDashboard = dto.showDailyQuoteOnDashboard,
                    compactDashboardMode = dto.compactDashboardMode,
                    showMetricSummaryOnDashboard = dto.showMetricSummaryOnDashboard,
                    dashboardBlocksJson = dto.dashboardBlocksJson,
                    lastQuoteDateIso = dto.lastQuoteDateIso,
                    lastQuoteId = dto.lastQuoteId,
                    previousQuoteId = dto.previousQuoteId,
                    createdAt = if (dto.createdAt > 0L) dto.createdAt else now,
                    updatedAt = if (dto.updatedAt > 0L) dto.updatedAt else now
                )
            }
        } else {
            listOf(PersonalizationSettingsEntity(createdAt = now, updatedAt = now))
        }

        val quoteEntities = payload.quotes
            .filter { it.text.isNotBlank() }
            .map { dto ->
                QuoteEntity(
                    id = dto.id.ifBlank { UUID.randomUUID().toString() },
                    text = dto.text.trim(),
                    author = dto.author.trim(),
                    isActive = dto.isActive,
                    lastShownDateIso = dto.lastShownDateIso,
                    shownCount = dto.shownCount.coerceAtLeast(0),
                    createdAt = if (dto.createdAt > 0L) dto.createdAt else now,
                    updatedAt = if (dto.updatedAt > 0L) dto.updatedAt else now
                )
            }.distinctBy { it.id }

        val goalMilestoneEntities = payload.goalMilestones
            .filter { it.title.isNotBlank() && it.goalId in validPersonalGoalIds }
            .map { dto ->
                GoalMilestoneEntity(
                    id = dto.id.ifBlank { UUID.randomUUID().toString() },
                    goalId = dto.goalId,
                    title = dto.title.trim(),
                    description = dto.description,
                    dueAt = dto.dueAt,
                    isCompleted = dto.isCompleted,
                    completedAt = dto.completedAt,
                    sortOrder = dto.sortOrder,
                    createdAt = if (dto.createdAt > 0L) dto.createdAt else now,
                    updatedAt = if (dto.updatedAt > 0L) dto.updatedAt else now
                )
            }.distinctBy { it.id }

        val goalProgressLogEntities = payload.goalProgressLogs
            .filter { it.goalId in validPersonalGoalIds }
            .map { dto ->
                GoalProgressLogEntity(
                    id = dto.id.ifBlank { UUID.randomUUID().toString() },
                    goalId = dto.goalId,
                    progressPercent = dto.progressPercent.coerceIn(0, 100),
                    note = dto.note,
                    recordedAt = if (dto.recordedAt > 0L) dto.recordedAt else now,
                    createdAt = if (dto.createdAt > 0L) dto.createdAt else now,
                    updatedAt = if (dto.updatedAt > 0L) dto.updatedAt else now
                )
            }.distinctBy { it.id }

        val recurrenceExceptionEntities = payload.recurrenceExceptions
            .filter { it.recurrenceId in validRecurrenceIds && it.occurrenceDateIso.isNotBlank() }
            .map { dto ->
                RecurrenceExceptionEntity(
                    id = dto.id.ifBlank { UUID.randomUUID().toString() },
                    recurrenceId = dto.recurrenceId,
                    occurrenceDateIso = dto.occurrenceDateIso,
                    exceptionType = RecurrenceExceptionType.fromStringOrDefault(dto.exceptionType),
                    overrideTitle = dto.overrideTitle,
                    overrideDueAt = dto.overrideDueAt,
                    taskId = dto.taskId?.takeIf { it in validTaskIds },
                    createdAt = if (dto.createdAt > 0L) dto.createdAt else now,
                    updatedAt = if (dto.updatedAt > 0L) dto.updatedAt else now
                )
            }
            .distinctBy { it.id }
            .distinctBy { it.recurrenceId to it.occurrenceDateIso }

        // Valida e sanitiza configurações do Mercado ANTES de iniciar qualquer escrita.
        // Campo ausente (null) preserva os dados locais existentes; lista explicitamente vazia ([]) substitui.
        val validatedMarketFavorites: List<String>? =
            payload.marketConfig?.favoriteAssetCodes?.let { sanitizeFavoriteCodes(it) }
        val validatedMarketAlerts: List<MarketPriceAlert>? =
            payload.marketConfig?.priceAlerts?.let { sanitizePriceAlerts(it, now) }

        database.withTransaction {
            // Limpa na ordem inversa das chaves estrangeiras
            database.goalProgressLogDao().deleteAll()
            database.goalMilestoneDao().deleteAll()
            database.reminderDao().deleteAll()
            database.briefingHistoryDao().deleteAll()
            database.routineAssistantSettingsDao().deleteAll()
            database.personalizationSettingsDao().deleteAll()
            database.quoteDao().deleteAll()
            database.transactionDao().deleteAll()
            database.subtaskDao().deleteAll()
            database.recurrenceExceptionDao().deleteAll()
            database.taskDao().deleteAll()
            database.taskRecurrenceDao().deleteAll()
            database.appointmentDao().deleteAll()
            database.installmentPurchaseDao().deleteAll()
            database.recurringRuleDao().deleteAll()
            database.creditCardStatementDao().deleteAll()
            database.budgetDao().deleteAll()
            database.financialGoalDao().deleteAll()
            database.loanDao().deleteAll()
            database.eventDao().deleteAll()
            database.creditCardDao().deleteAll()
            database.projectDao().deleteAll()
            database.personalGoalDao().deleteAll()
            database.categoryDao().deleteAll()
            database.settingsDao().deleteAll()
            database.auditHistoryDao().deleteAll()
            database.errorLogDao().deleteAll()

            // Reinsere em ordem segura de dependencia
            database.settingsDao().insertAll(settingsEntities)
            database.routineAssistantSettingsDao().insertAll(routineSettingsEntities)
            database.personalizationSettingsDao().insertAll(personalizationEntities)
            database.quoteDao().insertAll(quoteEntities)
            database.categoryDao().insertAll(categoryEntities)
            database.personalGoalDao().insertAll(personalGoalEntities)
            database.goalMilestoneDao().insertAll(goalMilestoneEntities)
            database.goalProgressLogDao().insertAll(goalProgressLogEntities)
            database.projectDao().insertAll(projectEntities)
            database.creditCardDao().insertAll(cardEntities)
            database.eventDao().insertAll(eventEntities)
            database.taskRecurrenceDao().insertAll(taskRecurrenceEntities)
            database.taskDao().insertAll(taskEntities)
            database.recurrenceExceptionDao().insertAll(recurrenceExceptionEntities)
            database.subtaskDao().insertAll(subtaskEntities)
            database.appointmentDao().insertAll(appointmentEntities)
            database.creditCardStatementDao().insertAll(statementEntities)
            database.installmentPurchaseDao().insertAll(installmentPurchaseEntities)
            database.recurringRuleDao().insertAll(recurringRuleEntities)
            database.transactionDao().insertAll(transactionEntities)
            database.loanDao().insertAll(loanEntities)
            database.budgetDao().insertAll(budgetEntities)
            database.financialGoalDao().insertAll(goalEntities)
            database.auditHistoryDao().insertAll(auditEntities)
            database.errorLogDao().insertAll(errorLogEntities)
            database.reminderDao().insertAll(reminderEntities)
            database.briefingHistoryDao().insertAll(briefingHistoryEntities)
        }

        val (appliedFavoritesCount, appliedAlertsCount, marketApplied) =
            applyMarketConfigWithRollback(validatedMarketFavorites, validatedMarketAlerts)

        return RestoreSummary(
            settingsRestored = settingsEntities.size,
            categoriesRestored = categoryEntities.size,
            projectsRestored = projectEntities.size,
            eventsRestored = eventEntities.size,
            tasksRestored = taskEntities.size,
            subtasksRestored = subtaskEntities.size,
            appointmentsRestored = appointmentEntities.size,
            creditCardsRestored = cardEntities.size,
            statementsRestored = statementEntities.size,
            installmentPurchasesRestored = installmentPurchaseEntities.size,
            recurringRulesRestored = recurringRuleEntities.size,
            transactionsRestored = transactionEntities.size,
            loansRestored = loanEntities.size,
            budgetsRestored = budgetEntities.size,
            financialGoalsRestored = goalEntities.size,
            auditHistoryRestored = auditEntities.size,
            errorLogsRestored = errorLogEntities.size,
            remindersRestored = reminderEntities.size,
            routineSettingsRestored = routineSettingsEntities.size,
            briefingHistoryRestored = briefingHistoryEntities.size,
            personalizationSettingsRestored = personalizationEntities.size,
            quotesRestored = quoteEntities.size,
            personalGoalsRestored = personalGoalEntities.size,
            goalMilestonesRestored = goalMilestoneEntities.size,
            goalProgressLogsRestored = goalProgressLogEntities.size,
            recurrencesRestored = taskRecurrenceEntities.size,
            recurrenceExceptionsRestored = recurrenceExceptionEntities.size,
            marketFavoritesRestored = appliedFavoritesCount,
            marketAlertsRestored = appliedAlertsCount,
            marketConfigApplied = marketApplied
        )
    }

    private fun applyMarketConfigWithRollback(
        validatedFavorites: List<String>?,
        validatedAlerts: List<MarketPriceAlert>?
    ): Triple<Int, Int, Boolean> {
        val watchlistRepo = marketWatchlistRepository
        val alertRepo = marketAlertRepository
        if (watchlistRepo == null && alertRepo == null) {
            return Triple(0, 0, false)
        }

        val previousFavorites = watchlistRepo?.favoriteCodes?.value
        var favoritesApplied = false
        var restoredFavoritesCount = 0
        var restoredAlertsCount = 0

        try {
            if (validatedFavorites != null && watchlistRepo != null) {
                restoredFavoritesCount = watchlistRepo.replaceAllFavorites(validatedFavorites).size
                favoritesApplied = true
            }
            if (validatedAlerts != null && alertRepo != null) {
                restoredAlertsCount = alertRepo.replaceAllAlerts(validatedAlerts).size
            }
        } catch (ex: Exception) {
            if (favoritesApplied && watchlistRepo != null && previousFavorites != null) {
                runCatching { watchlistRepo.replaceAllFavorites(previousFavorites) }
            }
            throw ex
        }

        val applied = (validatedFavorites != null && watchlistRepo != null) ||
            (validatedAlerts != null && alertRepo != null)
        return Triple(restoredFavoritesCount, restoredAlertsCount, applied)
    }

    companion object {
        fun sanitizeFavoriteCodes(codes: List<String>): List<String> =
            codes
                .map { it.trim() }
                .filter { MarketAssetCatalog.findByCode(it) != null }
                .distinct()

        fun sanitizePriceAlerts(
            dtos: List<MarketPriceAlertBackupDto>,
            fallbackNowEpochMs: Long
        ): List<MarketPriceAlert> {
            return dtos.mapNotNull { dto ->
                val code = dto.assetCode.trim()
                val descriptor = MarketAssetCatalog.findByCode(code) ?: return@mapNotNull null
                val condition = runCatching {
                    MarketAlertCondition.valueOf(dto.condition.trim())
                }.getOrNull() ?: return@mapNotNull null

                if (!MarketQuoteCalculations.isValidPositiveRate(dto.targetPrice)) {
                    return@mapNotNull null
                }

                val declaredUnit = dto.currencyOrUnit.trim()
                if (declaredUnit.isNotEmpty()) {
                    val expectedUnit = MarketAlertRules.unitLabelFor(descriptor)
                    val matchesUnit = declaredUnit.equals(expectedUnit, ignoreCase = true) ||
                        (!descriptor.isPointsUnit && declaredUnit.equals(descriptor.quoteSymbol, ignoreCase = true))
                    if (!matchesUnit) return@mapNotNull null
                }

                val id = dto.id.trim().ifBlank { UUID.randomUUID().toString() }
                val createdAt = if (dto.createdAtEpochMs > 0L) dto.createdAtEpochMs else fallbackNowEpochMs
                val updatedAt = when {
                    dto.updatedAtEpochMs >= createdAt -> dto.updatedAtEpochMs
                    dto.updatedAtEpochMs > 0L -> dto.updatedAtEpochMs
                    else -> createdAt
                }

                val parsedStatus = runCatching {
                    MarketAlertStatus.valueOf(dto.status.trim())
                }.getOrNull()

                // Regra segura para estados inválidos/inconsistentes:
                // - Status desconhecido -> PAUSED (evita disparo indevido após restauração).
                // - WAITING com isActive=false -> PAUSED (respeita inatividade sem notificar).
                val resolvedStatus = when {
                    parsedStatus == null -> MarketAlertStatus.PAUSED
                    parsedStatus == MarketAlertStatus.WAITING && !dto.isActive -> MarketAlertStatus.PAUSED
                    else -> parsedStatus
                }

                val validTriggeredAt = dto.triggeredAtEpochMs?.takeIf { it > 0L }
                val validTriggeredPrice = dto.triggeredPrice?.takeIf {
                    MarketQuoteCalculations.isValidPositiveRate(it)
                }
                val validTriggeredSource = dto.triggeredQuoteSource?.trim()?.takeIf { it.isNotEmpty() }
                val validTriggeredQuoteTs = dto.triggeredQuoteTimestampEpochMs?.takeIf { it > 0L }
                val validLastEvaluatedTs = dto.lastEvaluatedQuoteFetchedAtEpochMs?.takeIf { it > 0L }

                when (resolvedStatus) {
                    MarketAlertStatus.TRIGGERED -> MarketPriceAlert(
                        id = id,
                        assetCode = descriptor.code,
                        condition = condition,
                        targetPrice = dto.targetPrice,
                        isActive = false,
                        status = MarketAlertStatus.TRIGGERED,
                        createdAtEpochMs = createdAt,
                        updatedAtEpochMs = updatedAt,
                        triggeredAtEpochMs = validTriggeredAt ?: updatedAt,
                        triggeredPrice = validTriggeredPrice,
                        triggeredQuoteSource = validTriggeredSource,
                        triggeredQuoteTimestampEpochMs = validTriggeredQuoteTs,
                        awaitingConditionReset = false,
                        lastEvaluatedQuoteFetchedAtEpochMs = validLastEvaluatedTs
                    )

                    MarketAlertStatus.PAUSED -> MarketPriceAlert(
                        id = id,
                        assetCode = descriptor.code,
                        condition = condition,
                        targetPrice = dto.targetPrice,
                        isActive = false,
                        status = MarketAlertStatus.PAUSED,
                        createdAtEpochMs = createdAt,
                        updatedAtEpochMs = updatedAt,
                        triggeredAtEpochMs = validTriggeredAt,
                        triggeredPrice = validTriggeredPrice,
                        triggeredQuoteSource = validTriggeredSource,
                        triggeredQuoteTimestampEpochMs = validTriggeredQuoteTs,
                        awaitingConditionReset = false,
                        lastEvaluatedQuoteFetchedAtEpochMs = validLastEvaluatedTs
                    )

                    MarketAlertStatus.WAITING -> MarketPriceAlert(
                        id = id,
                        assetCode = descriptor.code,
                        condition = condition,
                        targetPrice = dto.targetPrice,
                        isActive = true,
                        status = MarketAlertStatus.WAITING,
                        createdAtEpochMs = createdAt,
                        updatedAtEpochMs = updatedAt,
                        triggeredAtEpochMs = validTriggeredAt,
                        triggeredPrice = validTriggeredPrice,
                        triggeredQuoteSource = validTriggeredSource,
                        triggeredQuoteTimestampEpochMs = validTriggeredQuoteTs,
                        awaitingConditionReset = dto.awaitingConditionReset,
                        lastEvaluatedQuoteFetchedAtEpochMs = validLastEvaluatedTs
                    )
                }
            }.distinctBy { it.id }
        }
    }
}
