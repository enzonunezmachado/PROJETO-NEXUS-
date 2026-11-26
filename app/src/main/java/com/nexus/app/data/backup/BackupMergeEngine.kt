package com.nexus.app.data.backup

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
import com.nexus.app.domain.model.MarketPriceAlert
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

internal object BackupMergeEngine {

    fun validateCompatibilityForMerge(payload: NexusBackupPayload): String? {
        if (payload.formatVersion < 1 || payload.formatVersion > NexusBackupPayload.CURRENT_FORMAT_VERSION) {
            return "Versão de formato de backup incompatível para mesclagem (v${payload.formatVersion}). Suportado: v1 a v${NexusBackupPayload.CURRENT_FORMAT_VERSION}."
        }
        if (payload.databaseVersion > NexusDatabase.DATABASE_VERSION) {
            return "Versão do banco de dados do backup (v${payload.databaseVersion}) incompatível com a versão atual do app (v${NexusDatabase.DATABASE_VERSION})."
        }
        return null
    }

    private class TableStatsBuilder(
        val tableName: String,
        val label: String,
        val foundInFile: Int
    ) {
        var newRecords: Int = 0
        var inserted: Int = 0
        var alreadyExisting: Int = 0
        var conflicts: Int = 0
        var invalid: Int = 0
        var needsReview: Int = 0
        var singletonSkipped: Int = 0
        var preservedLocalOnly: Int = 0

        fun build(isDryRun: Boolean = false): EntityMergeCounts = EntityMergeCounts(
            tableName = tableName,
            label = label,
            foundInFile = foundInFile,
            newRecords = newRecords,
            inserted = if (isDryRun) 0 else inserted,
            alreadyExisting = alreadyExisting,
            conflicts = conflicts,
            invalid = invalid,
            needsReview = needsReview,
            singletonSkipped = singletonSkipped,
            preservedLocalOnly = preservedLocalOnly
        )
    }

    private data class PlannedMergeBatch(
        val settings: List<SettingsEntity>,
        val routineSettings: List<RoutineAssistantSettingsEntity>,
        val personalizationSettings: List<PersonalizationSettingsEntity>,
        val categories: List<CategoryEntity>,
        val creditCards: List<CreditCardEntity>,
        val loans: List<LoanEntity>,
        val quotes: List<QuoteEntity>,
        val briefingHistory: List<BriefingHistoryEntity>,
        val auditHistory: List<AuditHistoryEntity>,
        val errorLogs: List<ErrorLogEntity>,
        val personalGoals: List<PersonalGoalEntity>,
        val projects: List<ProjectEntity>,
        val events: List<EventEntity>,
        val creditCardStatements: List<CreditCardStatementEntity>,
        val installmentPurchases: List<InstallmentPurchaseEntity>,
        val recurringRules: List<RecurringRuleEntity>,
        val appointments: List<AppointmentEntity>,
        val goalMilestones: List<GoalMilestoneEntity>,
        val goalProgressLogs: List<GoalProgressLogEntity>,
        val budgets: List<BudgetEntity>,
        val financialGoals: List<FinancialGoalEntity>,
        val taskRecurrences: List<TaskRecurrenceEntity>,
        val tasks: List<TaskEntity>,
        val transactions: List<TransactionEntity>,
        val subtasks: List<SubtaskEntity>,
        val recurrenceExceptions: List<RecurrenceExceptionEntity>,
        val reminders: List<ReminderEntity>
    )

    private data class PlannedMergeResult(
        val summary: MergeSummary,
        val batch: PlannedMergeBatch,
        val favoritesToApply: List<String>?,
        val alertsToApply: List<MarketPriceAlert>?
    )

    suspend fun previewMerge(
        storage: BackupStorageGateway,
        payload: NexusBackupPayload,
        marketWatchlistRepository: MarketWatchlistRepository? = null,
        marketAlertRepository: MarketAlertRepository? = null
    ): MergeSummary {
        validateCompatibilityForMerge(payload)?.let { errorMsg ->
            throw IllegalArgumentException(errorMsg)
        }
        val plan = computeMergePlan(
            storage = storage,
            payload = payload,
            now = DateTimeUtils.nowEpochMillis(),
            currentFavorites = marketWatchlistRepository?.favoriteCodes?.value.orEmpty(),
            currentAlerts = marketAlertRepository?.alerts?.value.orEmpty(),
            hasWatchlistRepo = marketWatchlistRepository != null,
            hasAlertRepo = marketAlertRepository != null,
            isDryRun = true
        )
        return plan.summary
    }

    suspend fun mergeAtomically(
        storage: BackupStorageGateway,
        payload: NexusBackupPayload,
        marketWatchlistRepository: MarketWatchlistRepository? = null,
        marketAlertRepository: MarketAlertRepository? = null
    ): MergeSummary {
        validateCompatibilityForMerge(payload)?.let { errorMsg ->
            throw IllegalArgumentException(errorMsg)
        }

        val now = DateTimeUtils.nowEpochMillis()
        val currentFavorites = marketWatchlistRepository?.favoriteCodes?.value.orEmpty()
        val currentAlerts = marketAlertRepository?.alerts?.value.orEmpty()
        var marketApplied = false
        var marketWriteReached = false
        var planResult: PlannedMergeResult? = null

        try {
            storage.inTransaction {
                val plan = computeMergePlan(
                    storage = storage,
                    payload = payload,
                    now = now,
                    currentFavorites = currentFavorites,
                    currentAlerts = currentAlerts,
                    hasWatchlistRepo = marketWatchlistRepository != null,
                    hasAlertRepo = marketAlertRepository != null,
                    isDryRun = false
                )
                planResult = plan
                val b = plan.batch

                storage.insertSettings(b.settings)
                storage.insertRoutineSettings(b.routineSettings)
                storage.insertPersonalizationSettings(b.personalizationSettings)
                storage.insertCategories(b.categories)
                storage.insertCreditCards(b.creditCards)
                storage.insertLoans(b.loans)
                storage.insertQuotes(b.quotes)
                storage.insertBriefingHistory(b.briefingHistory)
                storage.insertAuditHistory(b.auditHistory)
                storage.insertErrorLogs(b.errorLogs)
                storage.insertPersonalGoals(b.personalGoals)
                storage.insertProjects(b.projects)
                storage.insertEvents(b.events)
                storage.insertCreditCardStatements(b.creditCardStatements)
                storage.insertInstallmentPurchases(b.installmentPurchases)
                storage.insertRecurringRules(b.recurringRules)
                storage.insertAppointments(b.appointments)
                storage.insertGoalMilestones(b.goalMilestones)
                storage.insertGoalProgressLogs(b.goalProgressLogs)
                storage.insertBudgets(b.budgets)
                storage.insertFinancialGoals(b.financialGoals)
                storage.insertTaskRecurrences(b.taskRecurrences)
                storage.insertTasks(b.tasks)
                storage.insertTransactions(b.transactions)
                storage.insertSubtasks(b.subtasks)
                storage.insertRecurrenceExceptions(b.recurrenceExceptions)
                storage.insertReminders(b.reminders)

                // 7. Mercado (dentro da transação, com rollback coordenado se Room falhar no commit)
                if (plan.favoritesToApply != null || plan.alertsToApply != null) {
                    marketWriteReached = true
                    val (_, _, applied) = BackupManager.applyMarketConfigWithRollback(
                        watchlistRepo = marketWatchlistRepository,
                        alertRepo = marketAlertRepository,
                        validatedFavorites = plan.favoritesToApply,
                        validatedAlerts = plan.alertsToApply
                    )
                    marketApplied = applied
                }
            }
        } catch (ex: Exception) {
            if (marketWriteReached) {
                val planned = planResult
                BackupManager.rollbackMarketConfig(
                    watchlistRepo = marketWatchlistRepository,
                    alertRepo = marketAlertRepository,
                    previousFavorites = if (planned?.favoritesToApply != null) currentFavorites else null,
                    previousAlerts = if (planned?.alertsToApply != null) currentAlerts else null
                )
            }
            throw ex
        }

        val finalPlan = requireNotNull(planResult)
        return finalPlan.summary.copy(marketConfigApplied = marketApplied)
    }

    private suspend fun computeMergePlan(
        storage: BackupStorageGateway,
        payload: NexusBackupPayload,
        now: Long,
        currentFavorites: List<String>,
        currentAlerts: List<MarketPriceAlert>,
        hasWatchlistRepo: Boolean,
        hasAlertRepo: Boolean,
        isDryRun: Boolean
    ): PlannedMergeResult {
        val issues = mutableListOf<MergeIssueItem>()
        val perEntity = mutableListOf<EntityMergeCounts>()

        // Pre-validate Market favorites and alerts (zero writes)
        val rawFavCodes = payload.marketConfig?.favoriteAssetCodes
        val rawAlertDtos = payload.marketConfig?.priceAlerts

        var marketFavFound = 0
        var marketFavAdded = 0
        var marketFavExisting = 0
        var favoritesToApply: List<String>? = null

        if (rawFavCodes != null) {
            marketFavFound = rawFavCodes.size
            val sanitized = BackupManager.sanitizeFavoriteCodes(rawFavCodes)
            val missing = sanitized.filter { it !in currentFavorites }
            marketFavExisting = sanitized.count { it in currentFavorites }
            marketFavAdded = missing.size
            if (missing.isNotEmpty() && hasWatchlistRepo) {
                favoritesToApply = (currentFavorites + missing).distinct()
            }
        }

        var marketAlertsFound = 0
        var marketAlertsAdded = 0
        var marketAlertsExisting = 0
        var marketAlertsConflicts = 0
        var marketAlertsInvalid = 0
        var alertsToApply: List<MarketPriceAlert>? = null

        if (rawAlertDtos != null) {
            marketAlertsFound = rawAlertDtos.size
            val localAlertsById = currentAlerts.associateBy { it.id }
            val seenAlertIds = mutableSetOf<String>()
            val addedAlerts = mutableListOf<MarketPriceAlert>()

            rawAlertDtos.forEach { dto ->
                val sanitizedSingle = BackupManager.sanitizePriceAlerts(listOf(dto), now).firstOrNull()
                if (dto.id.isBlank() || sanitizedSingle == null) {
                    marketAlertsInvalid++
                    issues.add(
                        MergeIssueItem(
                            tableName = "market_alerts",
                            recordId = dto.id.ifBlank { "(sem id)" },
                            kind = MergeIssueKind.INVALID_REJECTED,
                            reason = "Alerta de preço inválido ou com ativo/unidade incompatível."
                        )
                    )
                } else if (!seenAlertIds.add(sanitizedSingle.id)) {
                    marketAlertsExisting++
                } else {
                    val local = localAlertsById[sanitizedSingle.id]
                    if (local == null) {
                        addedAlerts.add(sanitizedSingle)
                        marketAlertsAdded++
                    } else if (areMarketAlertsSemanticallyEqual(local, sanitizedSingle)) {
                        marketAlertsExisting++
                    } else {
                        marketAlertsConflicts++
                        issues.add(
                            MergeIssueItem(
                                tableName = "market_alerts",
                                recordId = sanitizedSingle.id,
                                kind = MergeIssueKind.CONFLICT_KEPT_LOCAL,
                                reason = "Alerta de mercado já existe com configuração diferente; mantido local."
                            )
                        )
                    }
                }
            }
            if (addedAlerts.isNotEmpty() && hasAlertRepo) {
                alertsToApply = currentAlerts + addedAlerts
            }
        }

        // 1. Singletons (Regra 6 — nunca sobrescrever configurações singleton silenciosamente)
        val localSettings = storage.getSettings()
        val settingsStats = TableStatsBuilder("settings", "Configurações", payload.settings.size)
        val newSettings = mutableListOf<SettingsEntity>()
        if (localSettings.isNotEmpty()) {
            val local = localSettings.first()
            settingsStats.preservedLocalOnly = 0
            payload.settings.forEach { dto ->
                settingsStats.singletonSkipped++
                val incoming = SettingsEntity(
                    id = SettingsEntity.SINGLETON_ID,
                    userName = dto.userName.ifBlank { "Enzo" },
                    darkTheme = dto.darkTheme,
                    declaredBalanceCents = dto.declaredBalanceCents,
                    createdAt = local.createdAt,
                    updatedAt = local.updatedAt
                )
                if (incoming == local) {
                    settingsStats.alreadyExisting++
                } else {
                    settingsStats.conflicts++
                    issues.add(
                        MergeIssueItem(
                            tableName = "settings",
                            recordId = SettingsEntity.SINGLETON_ID,
                            kind = MergeIssueKind.SINGLETON_PRESERVED,
                            reason = "Configuração singleton preservada localmente (nome, tema e saldo declarado)."
                        )
                    )
                }
            }
        } else if (payload.settings.isNotEmpty()) {
            val dto = payload.settings.first()
            val entity = SettingsEntity(
                id = SettingsEntity.SINGLETON_ID,
                userName = dto.userName.ifBlank { "Enzo" },
                darkTheme = dto.darkTheme,
                declaredBalanceCents = dto.declaredBalanceCents,
                createdAt = if (dto.createdAt > 0L) dto.createdAt else now,
                updatedAt = if (dto.updatedAt > 0L) dto.updatedAt else now
            )
            newSettings.add(entity)
            settingsStats.newRecords = 1
            settingsStats.inserted = 1
            if (payload.settings.size > 1) {
                settingsStats.singletonSkipped = payload.settings.size - 1
            }
        }
        perEntity.add(settingsStats.build(isDryRun))

        val localRoutine = storage.getRoutineSettings()
        val routineStats = TableStatsBuilder(
            "routine_assistant_settings",
            "Configurações de rotina",
            payload.routineSettings.size
        )
        val newRoutineSettings = mutableListOf<RoutineAssistantSettingsEntity>()
        if (localRoutine.isNotEmpty()) {
            val local = localRoutine.first()
            payload.routineSettings.forEach { dto ->
                routineStats.singletonSkipped++
                val incoming = normalizeRoutineSettings(dto, local.createdAt, local.updatedAt)
                if (incoming == local) {
                    routineStats.alreadyExisting++
                } else {
                    routineStats.conflicts++
                    issues.add(
                        MergeIssueItem(
                            tableName = "routine_assistant_settings",
                            recordId = RoutineAssistantSettingsEntity.SINGLETON_ID,
                            kind = MergeIssueKind.SINGLETON_PRESERVED,
                            reason = "Configuração de rotina/notificações preservada localmente."
                        )
                    )
                }
            }
        } else if (payload.routineSettings.isNotEmpty()) {
            val dto = payload.routineSettings.first()
            val entity = normalizeRoutineSettings(
                dto = dto,
                createdAt = if (dto.createdAt > 0L) dto.createdAt else now,
                updatedAt = if (dto.updatedAt > 0L) dto.updatedAt else now
            )
            newRoutineSettings.add(entity)
            routineStats.newRecords = 1
            routineStats.inserted = 1
            if (payload.routineSettings.size > 1) {
                routineStats.singletonSkipped = payload.routineSettings.size - 1
            }
        }
        perEntity.add(routineStats.build(isDryRun))

        val localPers = storage.getPersonalizationSettings()
        val persStats = TableStatsBuilder(
            "personalization_settings",
            "Personalização do painel",
            payload.personalizationSettings.size
        )
        val newPersonalizationSettings = mutableListOf<PersonalizationSettingsEntity>()
        if (localPers.isNotEmpty()) {
            val local = localPers.first()
            payload.personalizationSettings.forEach { dto ->
                persStats.singletonSkipped++
                val incoming = normalizePersonalization(dto, local.createdAt, local.updatedAt)
                if (incoming == local) {
                    persStats.alreadyExisting++
                } else {
                    persStats.conflicts++
                    issues.add(
                        MergeIssueItem(
                            tableName = "personalization_settings",
                            recordId = PersonalizationSettingsEntity.SINGLETON_ID,
                            kind = MergeIssueKind.SINGLETON_PRESERVED,
                            reason = "Personalização do dashboard preservada localmente."
                        )
                    )
                }
            }
        } else if (payload.personalizationSettings.isNotEmpty()) {
            val dto = payload.personalizationSettings.first()
            val entity = normalizePersonalization(
                dto = dto,
                createdAt = if (dto.createdAt > 0L) dto.createdAt else now,
                updatedAt = if (dto.updatedAt > 0L) dto.updatedAt else now
            )
            newPersonalizationSettings.add(entity)
            persStats.newRecords = 1
            persStats.inserted = 1
            if (payload.personalizationSettings.size > 1) {
                persStats.singletonSkipped = payload.personalizationSettings.size - 1
            }
        }
        perEntity.add(persStats.build(isDryRun))

        // 2. Level 0 — Sem dependências estrangeiras
        val localCategories = storage.getCategories()
        val validCategoryIds = localCategories.map { it.id }.toMutableSet()
        val newCategories = mergeSimpleTable(
            tableName = "categories",
            label = "Categorias",
            dtos = payload.categories,
            localItems = localCategories,
            idOf = { it.id.trim() },
            localIdOf = { it.id },
            isBasicValid = { it.name.isNotBlank() },
            toEntity = { dto ->
                CategoryEntity(
                    id = dto.id.trim(),
                    name = dto.name.trim(),
                    colorHex = dto.colorHex.ifBlank { "#9A9A9A" },
                    scope = CategoryScope.fromStringOrDefault(dto.scope),
                    isDefault = dto.isDefault,
                    createdAt = if (dto.createdAt > 0L) dto.createdAt else now,
                    updatedAt = if (dto.updatedAt > 0L) dto.updatedAt else now
                )
            },
            normalizeForCompare = { it.copy(createdAt = 0L, updatedAt = 0L) },
            issues = issues,
            perEntity = perEntity
        )
        validCategoryIds.addAll(newCategories.map { it.id })

        val localCards = storage.getCreditCards()
        val validCardIds = localCards.map { it.id }.toMutableSet()
        val newCards = mergeSimpleTable(
            tableName = "credit_cards",
            label = "Cartões de crédito",
            dtos = payload.creditCards,
            localItems = localCards,
            idOf = { it.id.trim() },
            localIdOf = { it.id },
            isBasicValid = { it.name.isNotBlank() },
            toEntity = { dto ->
                CreditCardEntity(
                    id = dto.id.trim(),
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
            },
            normalizeForCompare = { it.copy(createdAt = 0L, updatedAt = 0L) },
            issues = issues,
            perEntity = perEntity
        )
        validCardIds.addAll(newCards.map { it.id })

        val localLoans = storage.getLoans()
        val newLoans = mergeSimpleTable(
            tableName = "loans",
            label = "Empréstimos",
            dtos = payload.loans,
            localItems = localLoans,
            idOf = { it.id.trim() },
            localIdOf = { it.id },
            isBasicValid = {
                it.creditor.isNotBlank() &&
                    it.installmentsTotal >= 1 &&
                    it.originalAmountCents >= 0L &&
                    it.contractedAmountCents >= 0L &&
                    it.remainingBalanceCents >= 0L
            },
            toEntity = { dto ->
                LoanEntity(
                    id = dto.id.trim(),
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
            },
            normalizeForCompare = { it.copy(createdAt = 0L, updatedAt = 0L) },
            issues = issues,
            perEntity = perEntity
        )

        val localQuotes = storage.getQuotes()
        val newQuotes = mergeSimpleTable(
            tableName = "quotes",
            label = "Frases",
            dtos = payload.quotes,
            localItems = localQuotes,
            idOf = { it.id.trim() },
            localIdOf = { it.id },
            isBasicValid = { it.text.isNotBlank() },
            toEntity = { dto ->
                QuoteEntity(
                    id = dto.id.trim(),
                    text = dto.text.trim(),
                    author = dto.author.trim(),
                    isActive = dto.isActive,
                    lastShownDateIso = dto.lastShownDateIso,
                    shownCount = dto.shownCount.coerceAtLeast(0),
                    createdAt = if (dto.createdAt > 0L) dto.createdAt else now,
                    updatedAt = if (dto.updatedAt > 0L) dto.updatedAt else now
                )
            },
            normalizeForCompare = { it.copy(createdAt = 0L, updatedAt = 0L) },
            issues = issues,
            perEntity = perEntity
        )

        val localBriefings = storage.getBriefingHistory()
        val newBriefings = mergeTableWithNaturalKey(
            tableName = "briefing_history",
            label = "Histórico de briefings",
            dtos = payload.briefingHistory,
            localItems = localBriefings,
            idOf = { it.id.trim() },
            localIdOf = { it.id },
            naturalKeyOf = { entity -> entity.briefingType to entity.dateIso },
            isBasicValid = { it.dateIso.isNotBlank() },
            toEntity = { dto ->
                BriefingHistoryEntity(
                    id = dto.id.trim(),
                    briefingType = BriefingType.fromStringOrDefault(dto.briefingType),
                    dateIso = dto.dateIso.trim(),
                    summaryText = dto.summaryText,
                    tasksCount = dto.tasksCount.coerceAtLeast(0),
                    appointmentsCount = dto.appointmentsCount.coerceAtLeast(0),
                    billsDueCount = dto.billsDueCount.coerceAtLeast(0),
                    overdueCount = dto.overdueCount.coerceAtLeast(0),
                    reviewedAt = dto.reviewedAt,
                    createdAt = if (dto.createdAt > 0L) dto.createdAt else now,
                    updatedAt = if (dto.updatedAt > 0L) dto.updatedAt else now
                )
            },
            normalizeForCompare = { it.copy(createdAt = 0L, updatedAt = 0L) },
            issues = issues,
            perEntity = perEntity
        )

        val localAudits = storage.getAuditHistory()
        val newAudits = mergeSimpleTable(
            tableName = "audit_history",
            label = "Auditoria",
            dtos = payload.auditHistory,
            localItems = localAudits,
            idOf = { it.id.trim() },
            localIdOf = { it.id },
            isBasicValid = { it.entityType.isNotBlank() && it.action.isNotBlank() },
            toEntity = { dto ->
                AuditHistoryEntity(
                    id = dto.id.trim(),
                    entityType = dto.entityType,
                    entityId = dto.entityId,
                    action = dto.action,
                    summary = dto.summary,
                    occurredAt = if (dto.occurredAt > 0L) dto.occurredAt else now,
                    createdAt = if (dto.createdAt > 0L) dto.createdAt else now,
                    updatedAt = if (dto.updatedAt > 0L) dto.updatedAt else now
                )
            },
            normalizeForCompare = { it.copy(createdAt = 0L, updatedAt = 0L) },
            issues = issues,
            perEntity = perEntity
        )

        val localErrors = storage.getErrorLogs()
        val newErrors = mergeSimpleTable(
            tableName = "error_logs",
            label = "Logs de erro",
            dtos = payload.errorLogs,
            localItems = localErrors,
            idOf = { it.id.trim() },
            localIdOf = { it.id },
            isBasicValid = { it.message.isNotBlank() },
            toEntity = { dto ->
                ErrorLogEntity(
                    id = dto.id.trim(),
                    source = dto.source.ifBlank { "Backup" },
                    message = dto.message,
                    stackTrace = dto.stackTrace,
                    occurredAt = if (dto.occurredAt > 0L) dto.occurredAt else now,
                    createdAt = if (dto.createdAt > 0L) dto.createdAt else now,
                    updatedAt = if (dto.updatedAt > 0L) dto.updatedAt else now
                )
            },
            normalizeForCompare = { it.copy(createdAt = 0L, updatedAt = 0L) },
            issues = issues,
            perEntity = perEntity
        )

        // 3. Level 1 — Dependências de Nível 0
        val localPersonalGoals = storage.getPersonalGoals()
        val validPersonalGoalIds = localPersonalGoals.map { it.id }.toMutableSet()
        val newPersonalGoals = mergePersonalGoals(
            dtos = payload.personalGoals,
            localItems = localPersonalGoals,
            validCategoryIds = validCategoryIds,
            now = now,
            issues = issues,
            perEntity = perEntity
        )
        validPersonalGoalIds.addAll(newPersonalGoals.map { it.id })

        val localProjects = storage.getProjects()
        val validProjectIds = localProjects.map { it.id }.toMutableSet()
        val newProjects = mergeProjects(
            dtos = payload.projects,
            localItems = localProjects,
            validPersonalGoalIds = validPersonalGoalIds,
            now = now,
            issues = issues,
            perEntity = perEntity
        )
        validProjectIds.addAll(newProjects.map { it.id })

        val localEvents = storage.getEvents()
        val validEventIds = localEvents.map { it.id }.toMutableSet()
        val newEvents = mergeEvents(
            dtos = payload.events,
            localItems = localEvents,
            validCategoryIds = validCategoryIds,
            validCardIds = validCardIds,
            now = now,
            issues = issues,
            perEntity = perEntity
        )
        validEventIds.addAll(newEvents.map { it.id })

        val localStatements = storage.getCreditCardStatements()
        val localStatementsById = localStatements.associateBy { it.id }
        val allStatementsById = localStatementsById.toMutableMap()
        val newStatements = mergeStatements(
            dtos = payload.creditCardStatements,
            localItems = localStatements,
            validCardIds = validCardIds,
            now = now,
            issues = issues,
            perEntity = perEntity
        )
        newStatements.forEach { allStatementsById[it.id] = it }
        val validStatementIds = allStatementsById.keys

        val localPurchases = storage.getInstallmentPurchases()
        val validPurchaseIds = localPurchases.map { it.id }.toMutableSet()
        val newPurchases = mergeInstallmentPurchases(
            dtos = payload.installmentPurchases,
            localItems = localPurchases,
            validCardIds = validCardIds,
            validCategoryIds = validCategoryIds,
            validEventIds = validEventIds,
            validProjectIds = validProjectIds,
            now = now,
            issues = issues,
            perEntity = perEntity
        )
        validPurchaseIds.addAll(newPurchases.map { it.id })

        val localRules = storage.getRecurringRules()
        val validRuleIds = localRules.map { it.id }.toMutableSet()
        val newRules = mergeRecurringRules(
            dtos = payload.recurringRules,
            localItems = localRules,
            validCategoryIds = validCategoryIds,
            validCardIds = validCardIds,
            now = now,
            issues = issues,
            perEntity = perEntity
        )
        validRuleIds.addAll(newRules.map { it.id })

        val localAppointments = storage.getAppointments()
        val validAppointmentIds = localAppointments.map { it.id }.toMutableSet()
        val newAppointments = mergeAppointments(
            dtos = payload.appointments,
            localItems = localAppointments,
            validCategoryIds = validCategoryIds,
            validEventIds = validEventIds,
            now = now,
            issues = issues,
            perEntity = perEntity
        )
        validAppointmentIds.addAll(newAppointments.map { it.id })

        // 4. Level 2 — Dependências de Nível 0 & 1
        val localMilestones = storage.getGoalMilestones()
        val newMilestones = mergeGoalMilestones(
            dtos = payload.goalMilestones,
            localItems = localMilestones,
            validPersonalGoalIds = validPersonalGoalIds,
            now = now,
            issues = issues,
            perEntity = perEntity
        )

        val localGoalLogs = storage.getGoalProgressLogs()
        val newGoalLogs = mergeGoalProgressLogs(
            dtos = payload.goalProgressLogs,
            localItems = localGoalLogs,
            validPersonalGoalIds = validPersonalGoalIds,
            now = now,
            issues = issues,
            perEntity = perEntity
        )

        val localBudgets = storage.getBudgets()
        val newBudgets = mergeBudgets(
            dtos = payload.budgets,
            localItems = localBudgets,
            validCategoryIds = validCategoryIds,
            now = now,
            issues = issues,
            perEntity = perEntity
        )

        val localFinGoals = storage.getFinancialGoals()
        val newFinGoals = mergeFinancialGoals(
            dtos = payload.financialGoals,
            localItems = localFinGoals,
            validCategoryIds = validCategoryIds,
            now = now,
            issues = issues,
            perEntity = perEntity
        )

        val localRecurrences = storage.getTaskRecurrences()
        val validRecurrenceIds = localRecurrences.map { it.id }.toMutableSet()
        val newRecurrences = mergeTaskRecurrences(
            dtos = payload.recurrences,
            localItems = localRecurrences,
            validProjectIds = validProjectIds,
            validCategoryIds = validCategoryIds,
            validEventIds = validEventIds,
            validPersonalGoalIds = validPersonalGoalIds,
            now = now,
            issues = issues,
            perEntity = perEntity
        )
        validRecurrenceIds.addAll(newRecurrences.map { it.id })

        // 5. Level 3 — Tarefas & Transações
        val localTasks = storage.getTasks()
        val validTaskIds = localTasks.map { it.id }.toMutableSet()
        val newTasks = mergeTasks(
            dtos = payload.tasks,
            localItems = localTasks,
            validProjectIds = validProjectIds,
            validCategoryIds = validCategoryIds,
            validEventIds = validEventIds,
            validPersonalGoalIds = validPersonalGoalIds,
            validRecurrenceIds = validRecurrenceIds,
            now = now,
            issues = issues,
            perEntity = perEntity
        )
        validTaskIds.addAll(newTasks.map { it.id })

        val localTransactions = storage.getTransactions()
        val validTransactionIds = localTransactions.map { it.id }.toMutableSet()
        val newTransactions = mergeTransactions(
            dtos = payload.transactions,
            localItems = localTransactions,
            validCategoryIds = validCategoryIds,
            validProjectIds = validProjectIds,
            validEventIds = validEventIds,
            validTaskIds = validTaskIds,
            validCardIds = validCardIds,
            localStatementsById = localStatementsById,
            allStatementsById = allStatementsById,
            validPurchaseIds = validPurchaseIds,
            validRuleIds = validRuleIds,
            now = now,
            issues = issues,
            perEntity = perEntity
        )
        validTransactionIds.addAll(newTransactions.map { it.id })

        // 6. Level 4 — Filhos finais (Subtarefas, Exceções, Lembretes)
        val localSubtasks = storage.getSubtasks()
        val newSubtasks = mergeSubtasks(
            dtos = payload.subtasks,
            localItems = localSubtasks,
            validTaskIds = validTaskIds,
            now = now,
            issues = issues,
            perEntity = perEntity
        )

        val localRecExceptions = storage.getRecurrenceExceptions()
        val newRecExceptions = mergeRecurrenceExceptions(
            dtos = payload.recurrenceExceptions,
            localItems = localRecExceptions,
            validRecurrenceIds = validRecurrenceIds,
            validTaskIds = validTaskIds,
            now = now,
            issues = issues,
            perEntity = perEntity
        )

        val localReminders = storage.getReminders()
        val newReminders = mergeReminders(
            dtos = payload.reminders,
            localItems = localReminders,
            validTaskIds = validTaskIds,
            validAppointmentIds = validAppointmentIds,
            validEventIds = validEventIds,
            validTransactionIds = validTransactionIds,
            validStatementIds = validStatementIds,
            now = now,
            issues = issues,
            perEntity = perEntity
        )

        val finalizedPerEntity = if (isDryRun) {
            perEntity.map { it.copy(inserted = 0) }
        } else {
            perEntity.toList()
        }

        val summary = MergeSummary(
            totalFoundInFile = finalizedPerEntity.sumOf { it.foundInFile },
            totalNewRecords = finalizedPerEntity.sumOf { it.newRecords },
            totalInserted = finalizedPerEntity.sumOf { it.inserted },
            totalAlreadyExisting = finalizedPerEntity.sumOf { it.alreadyExisting },
            totalConflicts = finalizedPerEntity.sumOf { it.conflicts },
            totalInvalid = finalizedPerEntity.sumOf { it.invalid },
            totalNeedsReview = finalizedPerEntity.sumOf { it.needsReview },
            totalSingletonSkipped = finalizedPerEntity.sumOf { it.singletonSkipped },
            totalPreservedLocalOnly = finalizedPerEntity.sumOf { it.preservedLocalOnly },
            perEntity = finalizedPerEntity,
            issues = issues,
            marketFavoritesFound = marketFavFound,
            marketFavoritesAdded = marketFavAdded,
            marketFavoritesAlreadyExisting = marketFavExisting,
            marketAlertsFound = marketAlertsFound,
            marketAlertsAdded = marketAlertsAdded,
            marketAlertsAlreadyExisting = marketAlertsExisting,
            marketAlertsConflicts = marketAlertsConflicts,
            marketAlertsInvalid = marketAlertsInvalid,
            marketConfigApplied = false
        )

        return PlannedMergeResult(
            summary = summary,
            batch = PlannedMergeBatch(
                settings = newSettings,
                routineSettings = newRoutineSettings,
                personalizationSettings = newPersonalizationSettings,
                categories = newCategories,
                creditCards = newCards,
                loans = newLoans,
                quotes = newQuotes,
                briefingHistory = newBriefings,
                auditHistory = newAudits,
                errorLogs = newErrors,
                personalGoals = newPersonalGoals,
                projects = newProjects,
                events = newEvents,
                creditCardStatements = newStatements,
                installmentPurchases = newPurchases,
                recurringRules = newRules,
                appointments = newAppointments,
                goalMilestones = newMilestones,
                goalProgressLogs = newGoalLogs,
                budgets = newBudgets,
                financialGoals = newFinGoals,
                taskRecurrences = newRecurrences,
                tasks = newTasks,
                transactions = newTransactions,
                subtasks = newSubtasks,
                recurrenceExceptions = newRecExceptions,
                reminders = newReminders
            ),
            favoritesToApply = favoritesToApply,
            alertsToApply = alertsToApply
        )
    }

    private fun <Dto, Entity> mergeSimpleTable(
        tableName: String,
        label: String,
        dtos: List<Dto>,
        localItems: List<Entity>,
        idOf: (Dto) -> String,
        localIdOf: (Entity) -> String,
        isBasicValid: (Dto) -> Boolean,
        toEntity: (Dto) -> Entity,
        normalizeForCompare: (Entity) -> Any,
        issues: MutableList<MergeIssueItem>,
        perEntity: MutableList<EntityMergeCounts>
    ): List<Entity> {
        val stats = TableStatsBuilder(tableName, label, dtos.size)
        val localById = localItems.associateBy(localIdOf)
        val acceptedNewById = mutableMapOf<String, Entity>()
        val seenLocalIds = mutableSetOf<String>()

        dtos.forEach { dto ->
            val cleanId = idOf(dto)
            if (cleanId.isEmpty() || !isBasicValid(dto)) {
                stats.invalid++
                stats.needsReview++
                issues.add(
                    MergeIssueItem(
                        tableName = tableName,
                        recordId = cleanId.ifEmpty { "(sem id)" },
                        kind = MergeIssueKind.INVALID_REJECTED,
                        reason = "Registro inválido ou sem identificador estável."
                    )
                )
                return@forEach
            }

            val candidate = toEntity(dto)
            val existingLocal = localById[cleanId]
            if (existingLocal != null) {
                seenLocalIds.add(cleanId)
                if (normalizeForCompare(existingLocal) == normalizeForCompare(candidate)) {
                    stats.alreadyExisting++
                } else {
                    stats.conflicts++
                    issues.add(
                        MergeIssueItem(
                            tableName = tableName,
                            recordId = cleanId,
                            kind = MergeIssueKind.CONFLICT_KEPT_LOCAL,
                            reason = "Registro já existe com conteúdo diferente; versão local mantida."
                        )
                    )
                }
                return@forEach
            }

            val batchExisting = acceptedNewById[cleanId]
            if (batchExisting != null) {
                if (normalizeForCompare(batchExisting) == normalizeForCompare(candidate)) {
                    stats.alreadyExisting++
                } else {
                    stats.conflicts++
                    issues.add(
                        MergeIssueItem(
                            tableName = tableName,
                            recordId = cleanId,
                            kind = MergeIssueKind.CONFLICT_KEPT_LOCAL,
                            reason = "ID duplicado dentro do mesmo arquivo de backup com conteúdo divergente."
                        )
                    )
                }
                return@forEach
            }

            acceptedNewById[cleanId] = candidate
            stats.newRecords++
            stats.inserted++
        }

        stats.preservedLocalOnly = localItems.count { localIdOf(it) !in seenLocalIds }
        perEntity.add(stats.build())
        return acceptedNewById.values.toList()
    }

    private fun <Dto, Entity, Key> mergeTableWithNaturalKey(
        tableName: String,
        label: String,
        dtos: List<Dto>,
        localItems: List<Entity>,
        idOf: (Dto) -> String,
        localIdOf: (Entity) -> String,
        naturalKeyOf: (Entity) -> Key,
        isBasicValid: (Dto) -> Boolean,
        toEntity: (Dto) -> Entity,
        normalizeForCompare: (Entity) -> Any,
        issues: MutableList<MergeIssueItem>,
        perEntity: MutableList<EntityMergeCounts>
    ): List<Entity> {
        val stats = TableStatsBuilder(tableName, label, dtos.size)
        val localById = localItems.associateBy(localIdOf)
        val localByNaturalKey = localItems.associateBy(naturalKeyOf)
        val acceptedNewById = mutableMapOf<String, Entity>()
        val acceptedNaturalKeys = mutableMapOf<Key, Entity>()
        val seenLocalIds = mutableSetOf<String>()

        dtos.forEach { dto ->
            val cleanId = idOf(dto)
            if (cleanId.isEmpty() || !isBasicValid(dto)) {
                stats.invalid++
                stats.needsReview++
                issues.add(
                    MergeIssueItem(
                        tableName = tableName,
                        recordId = cleanId.ifEmpty { "(sem id)" },
                        kind = MergeIssueKind.INVALID_REJECTED,
                        reason = "Registro inválido ou sem identificador estável."
                    )
                )
                return@forEach
            }

            val candidate = toEntity(dto)
            val naturalKey = naturalKeyOf(candidate)

            val existingLocal = localById[cleanId]
            if (existingLocal != null) {
                seenLocalIds.add(cleanId)
                if (normalizeForCompare(existingLocal) == normalizeForCompare(candidate)) {
                    stats.alreadyExisting++
                } else {
                    stats.conflicts++
                    issues.add(
                        MergeIssueItem(
                            tableName = tableName,
                            recordId = cleanId,
                            kind = MergeIssueKind.CONFLICT_KEPT_LOCAL,
                            reason = "Registro já existe localmente com dados diferentes; mantido local."
                        )
                    )
                }
                return@forEach
            }

            val collidingLocal = localByNaturalKey[naturalKey]
            if (collidingLocal != null) {
                seenLocalIds.add(localIdOf(collidingLocal))
                stats.conflicts++
                issues.add(
                    MergeIssueItem(
                        tableName = tableName,
                        recordId = cleanId,
                        kind = MergeIssueKind.CONFLICT_KEPT_LOCAL,
                        reason = "Chave única ($naturalKey) já existe no banco local sob outro ID; mantido local."
                    )
                )
                return@forEach
            }

            val batchDupId = acceptedNewById[cleanId]
            val batchDupKey = acceptedNaturalKeys[naturalKey]
            if (batchDupId != null || batchDupKey != null) {
                val existing = batchDupId ?: batchDupKey!!
                if (normalizeForCompare(existing) == normalizeForCompare(candidate)) {
                    stats.alreadyExisting++
                } else {
                    stats.conflicts++
                    issues.add(
                        MergeIssueItem(
                            tableName = tableName,
                            recordId = cleanId,
                            kind = MergeIssueKind.CONFLICT_KEPT_LOCAL,
                            reason = "Chave única ($naturalKey) duplicada dentro do mesmo backup com conteúdo divergente."
                        )
                    )
                }
                return@forEach
            }

            acceptedNewById[cleanId] = candidate
            acceptedNaturalKeys[naturalKey] = candidate
            stats.newRecords++
            stats.inserted++
        }

        stats.preservedLocalOnly = localItems.count { localIdOf(it) !in seenLocalIds }
        perEntity.add(stats.build())
        return acceptedNewById.values.toList()
    }

    private fun mergePersonalGoals(
        dtos: List<PersonalGoalBackupDto>,
        localItems: List<PersonalGoalEntity>,
        validCategoryIds: Set<String>,
        now: Long,
        issues: MutableList<MergeIssueItem>,
        perEntity: MutableList<EntityMergeCounts>
    ): List<PersonalGoalEntity> {
        val table = "personal_goals"
        val stats = TableStatsBuilder(table, "Objetivos pessoais", dtos.size)
        val localById = localItems.associateBy { it.id }
        val accepted = mutableMapOf<String, PersonalGoalEntity>()
        val seenLocal = mutableSetOf<String>()

        dtos.forEach { dto ->
            val id = dto.id.trim()
            if (id.isEmpty() || dto.title.isBlank()) {
                stats.invalid++
                stats.needsReview++
                issues.add(MergeIssueItem(table, id.ifEmpty { "(sem id)" }, MergeIssueKind.INVALID_REJECTED, "Objetivo sem ID estável ou título."))
                return@forEach
            }
            val rawCat = dto.categoryId?.trim()?.takeIf { it.isNotEmpty() }
            val resolvedCat = rawCat?.takeIf { it in validCategoryIds }
            val sanitizedRef = rawCat != null && resolvedCat == null

            val candidate = PersonalGoalEntity(
                id = id,
                title = dto.title.trim(),
                description = dto.description,
                categoryId = resolvedCat,
                targetDateAt = dto.targetDateAt,
                status = GoalStatus.fromStringOrDefault(dto.status),
                progressMode = GoalProgressMode.fromStringOrDefault(dto.progressMode),
                manualProgressPercent = dto.manualProgressPercent.coerceIn(0, 100),
                completedAt = dto.completedAt,
                notes = dto.notes,
                createdAt = if (dto.createdAt > 0L) dto.createdAt else now,
                updatedAt = if (dto.updatedAt > 0L) dto.updatedAt else now
            )

            val local = localById[id]
            if (local != null) {
                seenLocal.add(id)
                if (local.copy(createdAt = 0L, updatedAt = 0L) == candidate.copy(createdAt = 0L, updatedAt = 0L) && !sanitizedRef) {
                    stats.alreadyExisting++
                } else {
                    stats.conflicts++
                    issues.add(MergeIssueItem(table, id, MergeIssueKind.CONFLICT_KEPT_LOCAL, "Objetivo já existente com alterações locais; mantido local."))
                }
                return@forEach
            }
            if (id in accepted) {
                stats.alreadyExisting++
                return@forEach
            }
            accepted[id] = candidate
            stats.newRecords++
            stats.inserted++
            if (sanitizedRef) {
                stats.needsReview++
                issues.add(MergeIssueItem(table, id, MergeIssueKind.REFERENCE_SANITIZED_NEEDS_REVIEW, "Categoria ($rawCat) ausente; vínculo opcional removido."))
            }
        }
        stats.preservedLocalOnly = localItems.count { it.id !in seenLocal }
        perEntity.add(stats.build())
        return accepted.values.toList()
    }

    private fun mergeProjects(
        dtos: List<ProjectBackupDto>,
        localItems: List<ProjectEntity>,
        validPersonalGoalIds: Set<String>,
        now: Long,
        issues: MutableList<MergeIssueItem>,
        perEntity: MutableList<EntityMergeCounts>
    ): List<ProjectEntity> {
        val table = "projects"
        val stats = TableStatsBuilder(table, "Projetos", dtos.size)
        val localById = localItems.associateBy { it.id }
        val accepted = mutableMapOf<String, ProjectEntity>()
        val seenLocal = mutableSetOf<String>()

        dtos.forEach { dto ->
            val id = dto.id.trim()
            if (id.isEmpty() || dto.name.isBlank()) {
                stats.invalid++
                stats.needsReview++
                issues.add(MergeIssueItem(table, id.ifEmpty { "(sem id)" }, MergeIssueKind.INVALID_REJECTED, "Projeto sem ID estável ou nome."))
                return@forEach
            }
            val rawGoal = dto.goalId?.trim()?.takeIf { it.isNotEmpty() }
            val resolvedGoal = rawGoal?.takeIf { it in validPersonalGoalIds }
            val sanitizedRef = rawGoal != null && resolvedGoal == null

            val candidate = ProjectEntity(
                id = id,
                name = dto.name.trim(),
                description = dto.description,
                deadlineAt = dto.deadlineAt,
                status = ProjectStatus.fromStringOrDefault(dto.status),
                priority = TaskPriority.fromStringOrDefault(dto.priority),
                goalId = resolvedGoal,
                createdAt = if (dto.createdAt > 0L) dto.createdAt else now,
                updatedAt = if (dto.updatedAt > 0L) dto.updatedAt else now
            )

            val local = localById[id]
            if (local != null) {
                seenLocal.add(id)
                if (local.copy(createdAt = 0L, updatedAt = 0L) == candidate.copy(createdAt = 0L, updatedAt = 0L) && !sanitizedRef) {
                    stats.alreadyExisting++
                } else {
                    stats.conflicts++
                    issues.add(MergeIssueItem(table, id, MergeIssueKind.CONFLICT_KEPT_LOCAL, "Projeto já existe com dados diferentes; mantido local."))
                }
                return@forEach
            }
            if (id in accepted) {
                stats.alreadyExisting++
                return@forEach
            }
            accepted[id] = candidate
            stats.newRecords++
            stats.inserted++
            if (sanitizedRef) {
                stats.needsReview++
                issues.add(MergeIssueItem(table, id, MergeIssueKind.REFERENCE_SANITIZED_NEEDS_REVIEW, "Objetivo ($rawGoal) ausente; vínculo opcional removido."))
            }
        }
        stats.preservedLocalOnly = localItems.count { it.id !in seenLocal }
        perEntity.add(stats.build())
        return accepted.values.toList()
    }

    private fun mergeEvents(
        dtos: List<EventBackupDto>,
        localItems: List<EventEntity>,
        validCategoryIds: Set<String>,
        validCardIds: Set<String>,
        now: Long,
        issues: MutableList<MergeIssueItem>,
        perEntity: MutableList<EntityMergeCounts>
    ): List<EventEntity> {
        val table = "events"
        val stats = TableStatsBuilder(table, "Eventos", dtos.size)
        val localById = localItems.associateBy { it.id }
        val accepted = mutableMapOf<String, EventEntity>()
        val seenLocal = mutableSetOf<String>()

        dtos.forEach { dto ->
            val id = dto.id.trim()
            if (id.isEmpty() || dto.name.isBlank()) {
                stats.invalid++
                stats.needsReview++
                issues.add(MergeIssueItem(table, id.ifEmpty { "(sem id)" }, MergeIssueKind.INVALID_REJECTED, "Evento sem ID estável ou nome."))
                return@forEach
            }
            val rawCard = dto.creditCardId?.trim()?.takeIf { it.isNotEmpty() }
            if (rawCard != null && rawCard !in validCardIds) {
                if (id in localById) seenLocal.add(id)
                stats.invalid++
                stats.needsReview++
                issues.add(MergeIssueItem(table, id, MergeIssueKind.INVALID_REJECTED, "Evento referencia cartão de crédito inexistente ($rawCard)."))
                return@forEach
            }
            val rawCat = dto.categoryId?.trim()?.takeIf { it.isNotEmpty() }
            val resolvedCat = rawCat?.takeIf { it in validCategoryIds }
            val sanitizedRef = rawCat != null && resolvedCat == null

            val start = if (dto.startAt > 0L) dto.startAt else now
            val end = if (dto.endAt >= start) dto.endAt else start + 3_600_000L
            val candidate = EventEntity(
                id = id,
                name = dto.name.trim(),
                description = dto.description,
                categoryId = resolvedCat,
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
                creditCardId = rawCard,
                installmentsCount = dto.installmentsCount.coerceAtLeast(1),
                createdAt = if (dto.createdAt > 0L) dto.createdAt else now,
                updatedAt = if (dto.updatedAt > 0L) dto.updatedAt else now
            )

            val local = localById[id]
            if (local != null) {
                seenLocal.add(id)
                if (local.copy(createdAt = 0L, updatedAt = 0L) == candidate.copy(createdAt = 0L, updatedAt = 0L) && !sanitizedRef) {
                    stats.alreadyExisting++
                } else {
                    stats.conflicts++
                    issues.add(MergeIssueItem(table, id, MergeIssueKind.CONFLICT_KEPT_LOCAL, "Evento já existe com diferenças; mantido local."))
                }
                return@forEach
            }
            if (id in accepted) {
                stats.alreadyExisting++
                return@forEach
            }
            accepted[id] = candidate
            stats.newRecords++
            stats.inserted++
            if (sanitizedRef) {
                stats.needsReview++
                issues.add(MergeIssueItem(table, id, MergeIssueKind.REFERENCE_SANITIZED_NEEDS_REVIEW, "Categoria ($rawCat) ausente; vínculo opcional removido."))
            }
        }
        stats.preservedLocalOnly = localItems.count { it.id !in seenLocal }
        perEntity.add(stats.build())
        return accepted.values.toList()
    }

    private fun mergeStatements(
        dtos: List<CreditCardStatementBackupDto>,
        localItems: List<CreditCardStatementEntity>,
        validCardIds: Set<String>,
        now: Long,
        issues: MutableList<MergeIssueItem>,
        perEntity: MutableList<EntityMergeCounts>
    ): List<CreditCardStatementEntity> {
        val table = "credit_card_statements"
        val stats = TableStatsBuilder(table, "Faturas de cartão", dtos.size)
        val localById = localItems.associateBy { it.id }
        val localByCardMonth = localItems.associateBy { it.creditCardId to it.yearMonth }
        val accepted = mutableMapOf<String, CreditCardStatementEntity>()
        val acceptedCardMonths = mutableSetOf<Pair<String, String>>()
        val seenLocal = mutableSetOf<String>()

        dtos.forEach { dto ->
            val id = dto.id.trim()
            val cardId = dto.creditCardId.trim()
            val ym = dto.yearMonth.trim()
            if (id.isEmpty() || cardId.isEmpty() || ym.isEmpty() || cardId !in validCardIds) {
                if (id.isNotEmpty() && id in localById) seenLocal.add(id)
                stats.invalid++
                stats.needsReview++
                issues.add(MergeIssueItem(table, id.ifEmpty { "(sem id)" }, MergeIssueKind.INVALID_REJECTED, "Fatura inválida ou sem cartão válido ($cardId)."))
                return@forEach
            }
            val candidate = CreditCardStatementEntity(
                id = id,
                creditCardId = cardId,
                yearMonth = ym,
                closingAt = dto.closingAt,
                dueAt = dto.dueAt,
                status = StatementStatus.fromStringOrDefault(dto.status),
                paidAt = dto.paidAt,
                paidAmountCents = dto.paidAmountCents.coerceAtLeast(0L),
                createdAt = if (dto.createdAt > 0L) dto.createdAt else now,
                updatedAt = if (dto.updatedAt > 0L) dto.updatedAt else now
            )

            val local = localById[id]
            if (local != null) {
                seenLocal.add(id)
                if (local.copy(createdAt = 0L, updatedAt = 0L) == candidate.copy(createdAt = 0L, updatedAt = 0L)) {
                    stats.alreadyExisting++
                } else {
                    stats.conflicts++
                    issues.add(MergeIssueItem(table, id, MergeIssueKind.CONFLICT_KEPT_LOCAL, "Fatura já existe com status/valores diferentes; mantida local."))
                }
                return@forEach
            }

            val naturalColliding = localByCardMonth[cardId to ym]
            if (naturalColliding != null) {
                seenLocal.add(naturalColliding.id)
                stats.conflicts++
                issues.add(MergeIssueItem(table, id, MergeIssueKind.CONFLICT_KEPT_LOCAL, "Fatura para cartão $cardId competência $ym já existe sob outro ID; mantida local."))
                return@forEach
            }

            if (id in accepted || (cardId to ym) in acceptedCardMonths) {
                stats.alreadyExisting++
                return@forEach
            }
            accepted[id] = candidate
            acceptedCardMonths.add(cardId to ym)
            stats.newRecords++
            stats.inserted++
        }
        stats.preservedLocalOnly = localItems.count { it.id !in seenLocal }
        perEntity.add(stats.build())
        return accepted.values.toList()
    }

    private fun mergeInstallmentPurchases(
        dtos: List<InstallmentPurchaseBackupDto>,
        localItems: List<InstallmentPurchaseEntity>,
        validCardIds: Set<String>,
        validCategoryIds: Set<String>,
        validEventIds: Set<String>,
        validProjectIds: Set<String>,
        now: Long,
        issues: MutableList<MergeIssueItem>,
        perEntity: MutableList<EntityMergeCounts>
    ): List<InstallmentPurchaseEntity> {
        val table = "installment_purchases"
        val stats = TableStatsBuilder(table, "Parcelamentos", dtos.size)
        val localById = localItems.associateBy { it.id }
        val accepted = mutableMapOf<String, InstallmentPurchaseEntity>()
        val seenLocal = mutableSetOf<String>()

        dtos.forEach { dto ->
            val id = dto.id.trim()
            if (id.isEmpty() || dto.description.isBlank() || dto.installmentsCount < 1) {
                stats.invalid++
                stats.needsReview++
                issues.add(MergeIssueItem(table, id.ifEmpty { "(sem id)" }, MergeIssueKind.INVALID_REJECTED, "Parcelamento inválido ou sem ID estável."))
                return@forEach
            }
            val rawCard = dto.creditCardId?.trim()?.takeIf { it.isNotEmpty() }
            if (rawCard != null && rawCard !in validCardIds) {
                if (id in localById) seenLocal.add(id)
                stats.invalid++
                stats.needsReview++
                issues.add(MergeIssueItem(table, id, MergeIssueKind.INVALID_REJECTED, "Parcelamento referencia cartão inexistente ($rawCard)."))
                return@forEach
            }

            val rawCat = dto.categoryId?.trim()?.takeIf { it.isNotEmpty() }
            val resolvedCat = rawCat?.takeIf { it in validCategoryIds }
            val rawEvent = dto.eventId?.trim()?.takeIf { it.isNotEmpty() }
            val resolvedEvent = rawEvent?.takeIf { it in validEventIds }
            val rawProj = dto.projectId?.trim()?.takeIf { it.isNotEmpty() }
            val resolvedProj = rawProj?.takeIf { it in validProjectIds }
            val sanitizedRef = (rawCat != null && resolvedCat == null) ||
                (rawEvent != null && resolvedEvent == null) ||
                (rawProj != null && resolvedProj == null)

            val candidate = InstallmentPurchaseEntity(
                id = id,
                description = dto.description.trim(),
                totalAmountCents = dto.totalAmountCents,
                installmentsCount = dto.installmentsCount.coerceAtLeast(1),
                installmentAmountCents = dto.installmentAmountCents,
                firstDueAt = if (dto.firstDueAt > 0L) dto.firstDueAt else now,
                creditCardId = rawCard,
                categoryId = resolvedCat,
                eventId = resolvedEvent,
                projectId = resolvedProj,
                notes = dto.notes,
                createdAt = if (dto.createdAt > 0L) dto.createdAt else now,
                updatedAt = if (dto.updatedAt > 0L) dto.updatedAt else now
            )

            val local = localById[id]
            if (local != null) {
                seenLocal.add(id)
                if (local.copy(createdAt = 0L, updatedAt = 0L) == candidate.copy(createdAt = 0L, updatedAt = 0L) && !sanitizedRef) {
                    stats.alreadyExisting++
                } else {
                    stats.conflicts++
                    issues.add(MergeIssueItem(table, id, MergeIssueKind.CONFLICT_KEPT_LOCAL, "Parcelamento já existe com alterações locais; mantido local."))
                }
                return@forEach
            }
            if (id in accepted) {
                stats.alreadyExisting++
                return@forEach
            }
            accepted[id] = candidate
            stats.newRecords++
            stats.inserted++
            if (sanitizedRef) {
                stats.needsReview++
                issues.add(MergeIssueItem(table, id, MergeIssueKind.REFERENCE_SANITIZED_NEEDS_REVIEW, "Referência opcional de parcelamento não encontrada; removida."))
            }
        }
        stats.preservedLocalOnly = localItems.count { it.id !in seenLocal }
        perEntity.add(stats.build())
        return accepted.values.toList()
    }

    private fun mergeRecurringRules(
        dtos: List<RecurringRuleBackupDto>,
        localItems: List<RecurringRuleEntity>,
        validCategoryIds: Set<String>,
        validCardIds: Set<String>,
        now: Long,
        issues: MutableList<MergeIssueItem>,
        perEntity: MutableList<EntityMergeCounts>
    ): List<RecurringRuleEntity> {
        val table = "recurring_rules"
        val stats = TableStatsBuilder(table, "Recorrências financeiras", dtos.size)
        val localById = localItems.associateBy { it.id }
        val accepted = mutableMapOf<String, RecurringRuleEntity>()
        val seenLocal = mutableSetOf<String>()

        dtos.forEach { dto ->
            val id = dto.id.trim()
            if (id.isEmpty() || dto.title.isBlank()) {
                stats.invalid++
                stats.needsReview++
                issues.add(MergeIssueItem(table, id.ifEmpty { "(sem id)" }, MergeIssueKind.INVALID_REJECTED, "Regra financeira sem ID estável ou título."))
                return@forEach
            }
            val rawCard = dto.creditCardId?.trim()?.takeIf { it.isNotEmpty() }
            if (rawCard != null && rawCard !in validCardIds) {
                if (id in localById) seenLocal.add(id)
                stats.invalid++
                stats.needsReview++
                issues.add(MergeIssueItem(table, id, MergeIssueKind.INVALID_REJECTED, "Regra recorrente referencia cartão inexistente ($rawCard)."))
                return@forEach
            }
            val rawCat = dto.categoryId?.trim()?.takeIf { it.isNotEmpty() }
            val resolvedCat = rawCat?.takeIf { it in validCategoryIds }
            val sanitizedRef = rawCat != null && resolvedCat == null

            val candidate = RecurringRuleEntity(
                id = id,
                title = dto.title.trim(),
                type = TransactionType.fromStringOrDefault(dto.type),
                amountCents = dto.amountCents,
                categoryId = resolvedCat,
                paymentMethod = PaymentMethod.fromStringOrDefault(dto.paymentMethod),
                creditCardId = rawCard,
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

            val local = localById[id]
            if (local != null) {
                seenLocal.add(id)
                if (local.copy(createdAt = 0L, updatedAt = 0L) == candidate.copy(createdAt = 0L, updatedAt = 0L) && !sanitizedRef) {
                    stats.alreadyExisting++
                } else {
                    stats.conflicts++
                    issues.add(MergeIssueItem(table, id, MergeIssueKind.CONFLICT_KEPT_LOCAL, "Recorrência financeira já existe com dados diferentes; mantida local."))
                }
                return@forEach
            }
            if (id in accepted) {
                stats.alreadyExisting++
                return@forEach
            }
            accepted[id] = candidate
            stats.newRecords++
            stats.inserted++
            if (sanitizedRef) {
                stats.needsReview++
                issues.add(MergeIssueItem(table, id, MergeIssueKind.REFERENCE_SANITIZED_NEEDS_REVIEW, "Categoria ($rawCat) ausente; vínculo opcional removido."))
            }
        }
        stats.preservedLocalOnly = localItems.count { it.id !in seenLocal }
        perEntity.add(stats.build())
        return accepted.values.toList()
    }

    private fun mergeAppointments(
        dtos: List<AppointmentBackupDto>,
        localItems: List<AppointmentEntity>,
        validCategoryIds: Set<String>,
        validEventIds: Set<String>,
        now: Long,
        issues: MutableList<MergeIssueItem>,
        perEntity: MutableList<EntityMergeCounts>
    ): List<AppointmentEntity> {
        val table = "appointments"
        val stats = TableStatsBuilder(table, "Compromissos", dtos.size)
        val localById = localItems.associateBy { it.id }
        val accepted = mutableMapOf<String, AppointmentEntity>()
        val seenLocal = mutableSetOf<String>()

        dtos.forEach { dto ->
            val id = dto.id.trim()
            if (id.isEmpty() || dto.title.isBlank()) {
                stats.invalid++
                stats.needsReview++
                issues.add(MergeIssueItem(table, id.ifEmpty { "(sem id)" }, MergeIssueKind.INVALID_REJECTED, "Compromisso sem ID estável ou título."))
                return@forEach
            }
            val rawCat = dto.categoryId?.trim()?.takeIf { it.isNotEmpty() }
            val resolvedCat = rawCat?.takeIf { it in validCategoryIds }
            val rawEvent = dto.eventId?.trim()?.takeIf { it.isNotEmpty() }
            val resolvedEvent = rawEvent?.takeIf { it in validEventIds }
            val sanitizedRef = (rawCat != null && resolvedCat == null) || (rawEvent != null && resolvedEvent == null)

            val start = if (dto.startAt > 0L) dto.startAt else now
            val end = if (dto.endAt >= start) dto.endAt else start + 3_600_000L
            val candidate = AppointmentEntity(
                id = id,
                title = dto.title.trim(),
                description = dto.description,
                categoryId = resolvedCat,
                eventId = resolvedEvent,
                startAt = start,
                endAt = end,
                isAllDay = dto.isAllDay,
                location = dto.location,
                link = dto.link,
                createdAt = if (dto.createdAt > 0L) dto.createdAt else now,
                updatedAt = if (dto.updatedAt > 0L) dto.updatedAt else now
            )

            val local = localById[id]
            if (local != null) {
                seenLocal.add(id)
                if (local.copy(createdAt = 0L, updatedAt = 0L) == candidate.copy(createdAt = 0L, updatedAt = 0L) && !sanitizedRef) {
                    stats.alreadyExisting++
                } else {
                    stats.conflicts++
                    issues.add(MergeIssueItem(table, id, MergeIssueKind.CONFLICT_KEPT_LOCAL, "Compromisso já existe com diferenças; mantido local."))
                }
                return@forEach
            }
            if (id in accepted) {
                stats.alreadyExisting++
                return@forEach
            }
            accepted[id] = candidate
            stats.newRecords++
            stats.inserted++
            if (sanitizedRef) {
                stats.needsReview++
                issues.add(MergeIssueItem(table, id, MergeIssueKind.REFERENCE_SANITIZED_NEEDS_REVIEW, "Referência opcional de compromisso não encontrada; removida."))
            }
        }
        stats.preservedLocalOnly = localItems.count { it.id !in seenLocal }
        perEntity.add(stats.build())
        return accepted.values.toList()
    }

    private fun mergeGoalMilestones(
        dtos: List<GoalMilestoneBackupDto>,
        localItems: List<GoalMilestoneEntity>,
        validPersonalGoalIds: Set<String>,
        now: Long,
        issues: MutableList<MergeIssueItem>,
        perEntity: MutableList<EntityMergeCounts>
    ): List<GoalMilestoneEntity> {
        val table = "goal_milestones"
        val stats = TableStatsBuilder(table, "Marcos de objetivos", dtos.size)
        val localById = localItems.associateBy { it.id }
        val accepted = mutableMapOf<String, GoalMilestoneEntity>()
        val seenLocal = mutableSetOf<String>()

        dtos.forEach { dto ->
            val id = dto.id.trim()
            val goalId = dto.goalId.trim()
            if (id.isEmpty() || dto.title.isBlank() || goalId.isEmpty() || goalId !in validPersonalGoalIds) {
                stats.invalid++
                stats.needsReview++
                issues.add(MergeIssueItem(table, id.ifEmpty { "(sem id)" }, MergeIssueKind.INVALID_REJECTED, "Marco órfão ou inválido (objetivo $goalId inexistente)."))
                return@forEach
            }
            val candidate = GoalMilestoneEntity(
                id = id,
                goalId = goalId,
                title = dto.title.trim(),
                description = dto.description,
                dueAt = dto.dueAt,
                isCompleted = dto.isCompleted,
                completedAt = dto.completedAt,
                sortOrder = dto.sortOrder,
                createdAt = if (dto.createdAt > 0L) dto.createdAt else now,
                updatedAt = if (dto.updatedAt > 0L) dto.updatedAt else now
            )
            val local = localById[id]
            if (local != null) {
                seenLocal.add(id)
                if (local.copy(createdAt = 0L, updatedAt = 0L) == candidate.copy(createdAt = 0L, updatedAt = 0L)) {
                    stats.alreadyExisting++
                } else {
                    stats.conflicts++
                    issues.add(MergeIssueItem(table, id, MergeIssueKind.CONFLICT_KEPT_LOCAL, "Marco já existe com diferenças; mantido local."))
                }
                return@forEach
            }
            if (id in accepted) {
                stats.alreadyExisting++
                return@forEach
            }
            accepted[id] = candidate
            stats.newRecords++
            stats.inserted++
        }
        stats.preservedLocalOnly = localItems.count { it.id !in seenLocal }
        perEntity.add(stats.build())
        return accepted.values.toList()
    }

    private fun mergeGoalProgressLogs(
        dtos: List<GoalProgressLogBackupDto>,
        localItems: List<GoalProgressLogEntity>,
        validPersonalGoalIds: Set<String>,
        now: Long,
        issues: MutableList<MergeIssueItem>,
        perEntity: MutableList<EntityMergeCounts>
    ): List<GoalProgressLogEntity> {
        val table = "goal_progress_logs"
        val stats = TableStatsBuilder(table, "Histórico de objetivos", dtos.size)
        val localById = localItems.associateBy { it.id }
        val accepted = mutableMapOf<String, GoalProgressLogEntity>()
        val seenLocal = mutableSetOf<String>()

        dtos.forEach { dto ->
            val id = dto.id.trim()
            val goalId = dto.goalId.trim()
            if (id.isEmpty() || goalId.isEmpty() || goalId !in validPersonalGoalIds) {
                stats.invalid++
                stats.needsReview++
                issues.add(MergeIssueItem(table, id.ifEmpty { "(sem id)" }, MergeIssueKind.INVALID_REJECTED, "Registro de progresso órfão (objetivo $goalId inexistente)."))
                return@forEach
            }
            val candidate = GoalProgressLogEntity(
                id = id,
                goalId = goalId,
                progressPercent = dto.progressPercent.coerceIn(0, 100),
                note = dto.note,
                recordedAt = if (dto.recordedAt > 0L) dto.recordedAt else now,
                createdAt = if (dto.createdAt > 0L) dto.createdAt else now,
                updatedAt = if (dto.updatedAt > 0L) dto.updatedAt else now
            )
            val local = localById[id]
            if (local != null) {
                seenLocal.add(id)
                if (local.copy(createdAt = 0L, updatedAt = 0L) == candidate.copy(createdAt = 0L, updatedAt = 0L)) {
                    stats.alreadyExisting++
                } else {
                    stats.conflicts++
                    issues.add(MergeIssueItem(table, id, MergeIssueKind.CONFLICT_KEPT_LOCAL, "Registro de progresso já existe com diferenças; mantido local."))
                }
                return@forEach
            }
            if (id in accepted) {
                stats.alreadyExisting++
                return@forEach
            }
            accepted[id] = candidate
            stats.newRecords++
            stats.inserted++
        }
        stats.preservedLocalOnly = localItems.count { it.id !in seenLocal }
        perEntity.add(stats.build())
        return accepted.values.toList()
    }

    private fun mergeBudgets(
        dtos: List<BudgetBackupDto>,
        localItems: List<BudgetEntity>,
        validCategoryIds: Set<String>,
        now: Long,
        issues: MutableList<MergeIssueItem>,
        perEntity: MutableList<EntityMergeCounts>
    ): List<BudgetEntity> {
        val table = "budgets"
        val stats = TableStatsBuilder(table, "Orçamentos", dtos.size)
        val localById = localItems.associateBy { it.id }
        val localByCatMonth = localItems.associateBy { it.categoryId to it.yearMonth }
        val accepted = mutableMapOf<String, BudgetEntity>()
        val acceptedCatMonths = mutableSetOf<Pair<String, String>>()
        val seenLocal = mutableSetOf<String>()

        dtos.forEach { dto ->
            val id = dto.id.trim()
            val catId = dto.categoryId.trim()
            val ym = dto.yearMonth.trim()
            if (id.isEmpty() || catId.isEmpty() || ym.isEmpty() || catId !in validCategoryIds) {
                stats.invalid++
                stats.needsReview++
                issues.add(MergeIssueItem(table, id.ifEmpty { "(sem id)" }, MergeIssueKind.INVALID_REJECTED, "Orçamento inválido ou categoria inexistente ($catId)."))
                return@forEach
            }
            val candidate = BudgetEntity(
                id = id,
                categoryId = catId,
                yearMonth = ym,
                limitCents = dto.limitCents.coerceAtLeast(0L),
                createdAt = if (dto.createdAt > 0L) dto.createdAt else now,
                updatedAt = if (dto.updatedAt > 0L) dto.updatedAt else now
            )
            val local = localById[id]
            if (local != null) {
                seenLocal.add(id)
                if (local.copy(createdAt = 0L, updatedAt = 0L) == candidate.copy(createdAt = 0L, updatedAt = 0L)) {
                    stats.alreadyExisting++
                } else {
                    stats.conflicts++
                    issues.add(MergeIssueItem(table, id, MergeIssueKind.CONFLICT_KEPT_LOCAL, "Orçamento já existe com limite diferente; mantido local."))
                }
                return@forEach
            }
            val colliding = localByCatMonth[catId to ym]
            if (colliding != null) {
                seenLocal.add(colliding.id)
                stats.conflicts++
                issues.add(MergeIssueItem(table, id, MergeIssueKind.CONFLICT_KEPT_LOCAL, "Orçamento para categoria $catId em $ym já existe sob outro ID; mantido local."))
                return@forEach
            }
            if (id in accepted || (catId to ym) in acceptedCatMonths) {
                stats.alreadyExisting++
                return@forEach
            }
            accepted[id] = candidate
            acceptedCatMonths.add(catId to ym)
            stats.newRecords++
            stats.inserted++
        }
        stats.preservedLocalOnly = localItems.count { it.id !in seenLocal }
        perEntity.add(stats.build())
        return accepted.values.toList()
    }

    private fun mergeFinancialGoals(
        dtos: List<FinancialGoalBackupDto>,
        localItems: List<FinancialGoalEntity>,
        validCategoryIds: Set<String>,
        now: Long,
        issues: MutableList<MergeIssueItem>,
        perEntity: MutableList<EntityMergeCounts>
    ): List<FinancialGoalEntity> {
        val table = "financial_goals"
        val stats = TableStatsBuilder(table, "Metas financeiras", dtos.size)
        val localById = localItems.associateBy { it.id }
        val accepted = mutableMapOf<String, FinancialGoalEntity>()
        val seenLocal = mutableSetOf<String>()

        dtos.forEach { dto ->
            val id = dto.id.trim()
            if (id.isEmpty() || dto.name.isBlank() || dto.targetAmountCents < 1L) {
                stats.invalid++
                stats.needsReview++
                issues.add(MergeIssueItem(table, id.ifEmpty { "(sem id)" }, MergeIssueKind.INVALID_REJECTED, "Meta financeira inválida ou sem ID estável."))
                return@forEach
            }
            val rawCat = dto.categoryId?.trim()?.takeIf { it.isNotEmpty() }
            val resolvedCat = rawCat?.takeIf { it in validCategoryIds }
            val sanitizedRef = rawCat != null && resolvedCat == null

            val candidate = FinancialGoalEntity(
                id = id,
                name = dto.name.trim(),
                targetAmountCents = dto.targetAmountCents.coerceAtLeast(1L),
                currentAmountCents = dto.currentAmountCents.coerceAtLeast(0L),
                deadlineAt = dto.deadlineAt,
                categoryId = resolvedCat,
                notes = dto.notes,
                createdAt = if (dto.createdAt > 0L) dto.createdAt else now,
                updatedAt = if (dto.updatedAt > 0L) dto.updatedAt else now
            )
            val local = localById[id]
            if (local != null) {
                seenLocal.add(id)
                if (local.copy(createdAt = 0L, updatedAt = 0L) == candidate.copy(createdAt = 0L, updatedAt = 0L) && !sanitizedRef) {
                    stats.alreadyExisting++
                } else {
                    stats.conflicts++
                    issues.add(MergeIssueItem(table, id, MergeIssueKind.CONFLICT_KEPT_LOCAL, "Meta financeira já existe com valores diferentes; mantida local."))
                }
                return@forEach
            }
            if (id in accepted) {
                stats.alreadyExisting++
                return@forEach
            }
            accepted[id] = candidate
            stats.newRecords++
            stats.inserted++
            if (sanitizedRef) {
                stats.needsReview++
                issues.add(MergeIssueItem(table, id, MergeIssueKind.REFERENCE_SANITIZED_NEEDS_REVIEW, "Categoria ($rawCat) ausente; vínculo opcional removido."))
            }
        }
        stats.preservedLocalOnly = localItems.count { it.id !in seenLocal }
        perEntity.add(stats.build())
        return accepted.values.toList()
    }

    private fun mergeTaskRecurrences(
        dtos: List<TaskRecurrenceBackupDto>,
        localItems: List<TaskRecurrenceEntity>,
        validProjectIds: Set<String>,
        validCategoryIds: Set<String>,
        validEventIds: Set<String>,
        validPersonalGoalIds: Set<String>,
        now: Long,
        issues: MutableList<MergeIssueItem>,
        perEntity: MutableList<EntityMergeCounts>
    ): List<TaskRecurrenceEntity> {
        val table = "recurrences"
        val stats = TableStatsBuilder(table, "Séries recorrentes", dtos.size)
        val localById = localItems.associateBy { it.id }
        val accepted = mutableMapOf<String, TaskRecurrenceEntity>()
        val seenLocal = mutableSetOf<String>()

        dtos.forEach { dto ->
            val id = dto.id.trim()
            if (id.isEmpty() || dto.title.isBlank()) {
                stats.invalid++
                stats.needsReview++
                issues.add(MergeIssueItem(table, id.ifEmpty { "(sem id)" }, MergeIssueKind.INVALID_REJECTED, "Série recorrente sem ID estável ou título."))
                return@forEach
            }
            val rawProj = dto.projectId?.trim()?.takeIf { it.isNotEmpty() }
            val resolvedProj = rawProj?.takeIf { it in validProjectIds }
            val rawCat = dto.categoryId?.trim()?.takeIf { it.isNotEmpty() }
            val resolvedCat = rawCat?.takeIf { it in validCategoryIds }
            val rawEvent = dto.eventId?.trim()?.takeIf { it.isNotEmpty() }
            val resolvedEvent = rawEvent?.takeIf { it in validEventIds }
            val rawGoal = dto.goalId?.trim()?.takeIf { it.isNotEmpty() }
            val resolvedGoal = rawGoal?.takeIf { it in validPersonalGoalIds }
            val sanitizedRef = (rawProj != null && resolvedProj == null) ||
                (rawCat != null && resolvedCat == null) ||
                (rawEvent != null && resolvedEvent == null) ||
                (rawGoal != null && resolvedGoal == null)

            val candidate = TaskRecurrenceEntity(
                id = id,
                title = dto.title.trim(),
                description = dto.description,
                projectId = resolvedProj,
                categoryId = resolvedCat,
                eventId = resolvedEvent,
                goalId = resolvedGoal,
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

            val local = localById[id]
            if (local != null) {
                seenLocal.add(id)
                if (local.copy(createdAt = 0L, updatedAt = 0L) == candidate.copy(createdAt = 0L, updatedAt = 0L) && !sanitizedRef) {
                    stats.alreadyExisting++
                } else {
                    stats.conflicts++
                    issues.add(MergeIssueItem(table, id, MergeIssueKind.CONFLICT_KEPT_LOCAL, "Série recorrente já existe com dados diferentes; mantida local."))
                }
                return@forEach
            }
            if (id in accepted) {
                stats.alreadyExisting++
                return@forEach
            }
            accepted[id] = candidate
            stats.newRecords++
            stats.inserted++
            if (sanitizedRef) {
                stats.needsReview++
                issues.add(MergeIssueItem(table, id, MergeIssueKind.REFERENCE_SANITIZED_NEEDS_REVIEW, "Referência opcional de série recorrente não encontrada; removida."))
            }
        }
        stats.preservedLocalOnly = localItems.count { it.id !in seenLocal }
        perEntity.add(stats.build())
        return accepted.values.toList()
    }

    private fun mergeTasks(
        dtos: List<TaskBackupDto>,
        localItems: List<TaskEntity>,
        validProjectIds: Set<String>,
        validCategoryIds: Set<String>,
        validEventIds: Set<String>,
        validPersonalGoalIds: Set<String>,
        validRecurrenceIds: Set<String>,
        now: Long,
        issues: MutableList<MergeIssueItem>,
        perEntity: MutableList<EntityMergeCounts>
    ): List<TaskEntity> {
        val table = "tasks"
        val stats = TableStatsBuilder(table, "Tarefas", dtos.size)
        val localById = localItems.associateBy { it.id }
        val localByRecurrenceOccurrence = localItems
            .filter { it.recurrenceId != null && it.occurrenceDateIso != null }
            .associateBy { it.recurrenceId!! to it.occurrenceDateIso!! }
        val accepted = mutableMapOf<String, TaskEntity>()
        val acceptedRecurrenceKeys = mutableSetOf<Pair<String, String>>()
        val seenLocal = mutableSetOf<String>()

        dtos.forEach { dto ->
            val id = dto.id.trim()
            if (id.isEmpty() || dto.title.isBlank()) {
                stats.invalid++
                stats.needsReview++
                issues.add(MergeIssueItem(table, id.ifEmpty { "(sem id)" }, MergeIssueKind.INVALID_REJECTED, "Tarefa sem ID estável ou título."))
                return@forEach
            }
            val rawProj = dto.projectId?.trim()?.takeIf { it.isNotEmpty() }
            val resolvedProj = rawProj?.takeIf { it in validProjectIds }
            val rawCat = dto.categoryId?.trim()?.takeIf { it.isNotEmpty() }
            val resolvedCat = rawCat?.takeIf { it in validCategoryIds }
            val rawEvent = dto.eventId?.trim()?.takeIf { it.isNotEmpty() }
            val resolvedEvent = rawEvent?.takeIf { it in validEventIds }
            val rawGoal = dto.goalId?.trim()?.takeIf { it.isNotEmpty() }
            val resolvedGoal = rawGoal?.takeIf { it in validPersonalGoalIds }
            val rawRec = dto.recurrenceId?.trim()?.takeIf { it.isNotEmpty() }
            val resolvedRec = rawRec?.takeIf { it in validRecurrenceIds }
            val occIso = dto.occurrenceDateIso?.trim()?.takeIf { it.isNotEmpty() }
            val sanitizedRef = (rawProj != null && resolvedProj == null) ||
                (rawCat != null && resolvedCat == null) ||
                (rawEvent != null && resolvedEvent == null) ||
                (rawGoal != null && resolvedGoal == null) ||
                (rawRec != null && resolvedRec == null)

            val candidate = TaskEntity(
                id = id,
                title = dto.title.trim(),
                description = dto.description,
                projectId = resolvedProj,
                categoryId = resolvedCat,
                eventId = resolvedEvent,
                goalId = resolvedGoal,
                priority = TaskPriority.fromStringOrDefault(dto.priority),
                status = TaskStatus.fromStringOrDefault(dto.status),
                dueAt = dto.dueAt,
                hasDueTime = dto.hasDueTime,
                notes = dto.notes,
                completedAt = dto.completedAt,
                recurrenceId = resolvedRec,
                occurrenceDateIso = if (resolvedRec != null) occIso else null,
                createdAt = if (dto.createdAt > 0L) dto.createdAt else now,
                updatedAt = if (dto.updatedAt > 0L) dto.updatedAt else now
            )

            val local = localById[id]
            if (local != null) {
                seenLocal.add(id)
                if (local.copy(createdAt = 0L, updatedAt = 0L) == candidate.copy(createdAt = 0L, updatedAt = 0L) && !sanitizedRef) {
                    stats.alreadyExisting++
                } else {
                    stats.conflicts++
                    issues.add(MergeIssueItem(table, id, MergeIssueKind.CONFLICT_KEPT_LOCAL, "Tarefa já existe com diferenças; mantida local."))
                }
                return@forEach
            }

            if (candidate.recurrenceId != null && candidate.occurrenceDateIso != null) {
                val recKey = candidate.recurrenceId to candidate.occurrenceDateIso
                val colliding = localByRecurrenceOccurrence[recKey]
                if (colliding != null) {
                    seenLocal.add(colliding.id)
                    stats.conflicts++
                    issues.add(MergeIssueItem(table, id, MergeIssueKind.CONFLICT_KEPT_LOCAL, "Ocorrência recorrente (${recKey.second}) já existe no banco local; mantida local."))
                    return@forEach
                }
                if (recKey in acceptedRecurrenceKeys) {
                    stats.alreadyExisting++
                    return@forEach
                }
                acceptedRecurrenceKeys.add(recKey)
            }

            if (id in accepted) {
                stats.alreadyExisting++
                return@forEach
            }
            accepted[id] = candidate
            stats.newRecords++
            stats.inserted++
            if (sanitizedRef) {
                stats.needsReview++
                issues.add(MergeIssueItem(table, id, MergeIssueKind.REFERENCE_SANITIZED_NEEDS_REVIEW, "Referência opcional de tarefa não encontrada; vínculo limpo."))
            }
        }
        stats.preservedLocalOnly = localItems.count { it.id !in seenLocal }
        perEntity.add(stats.build())
        return accepted.values.toList()
    }

    private fun mergeTransactions(
        dtos: List<TransactionBackupDto>,
        localItems: List<TransactionEntity>,
        validCategoryIds: Set<String>,
        validProjectIds: Set<String>,
        validEventIds: Set<String>,
        validTaskIds: Set<String>,
        validCardIds: Set<String>,
        localStatementsById: Map<String, CreditCardStatementEntity>,
        allStatementsById: Map<String, CreditCardStatementEntity>,
        validPurchaseIds: Set<String>,
        validRuleIds: Set<String>,
        now: Long,
        issues: MutableList<MergeIssueItem>,
        perEntity: MutableList<EntityMergeCounts>
    ): List<TransactionEntity> {
        val table = "transactions"
        val stats = TableStatsBuilder(table, "Transações", dtos.size)
        val localById = localItems.associateBy { it.id }
        val localByRecurrenceOcc = localItems
            .filter { it.recurringRuleId != null && it.occurrenceIndex != null }
            .associateBy { it.recurringRuleId!! to it.occurrenceIndex!! }
        val localByInstallmentNum = localItems
            .filter { it.installmentPurchaseId != null && it.installmentNumber != null }
            .associateBy { it.installmentPurchaseId!! to it.installmentNumber!! }

        val accepted = mutableMapOf<String, TransactionEntity>()
        val acceptedRecOcc = mutableSetOf<Pair<String, Int>>()
        val acceptedInstNum = mutableSetOf<Pair<String, Int>>()
        val seenLocal = mutableSetOf<String>()

        dtos.forEach { dto ->
            val id = dto.id.trim()
            if (id.isEmpty() || dto.description.isBlank() || dto.amountCents < 0L) {
                stats.invalid++
                stats.needsReview++
                issues.add(MergeIssueItem(table, id.ifEmpty { "(sem id)" }, MergeIssueKind.INVALID_REJECTED, "Lançamento sem ID estável, descrição ou valor válido."))
                return@forEach
            }

            // Strict financial FK validation (Regra 5 & Regra 7)
            val rawCard = dto.creditCardId?.trim()?.takeIf { it.isNotEmpty() }
            if (rawCard != null && rawCard !in validCardIds) {
                if (id in localById) seenLocal.add(id)
                stats.invalid++
                stats.needsReview++
                issues.add(MergeIssueItem(table, id, MergeIssueKind.INVALID_REJECTED, "Lançamento referencia cartão inexistente ($rawCard)."))
                return@forEach
            }
            val rawStatement = dto.statementId?.trim()?.takeIf { it.isNotEmpty() }
            if (rawStatement != null && rawStatement !in allStatementsById) {
                if (id in localById) seenLocal.add(id)
                stats.invalid++
                stats.needsReview++
                issues.add(MergeIssueItem(table, id, MergeIssueKind.INVALID_REJECTED, "Lançamento referencia fatura inexistente ($rawStatement)."))
                return@forEach
            }
            val rawPurchase = dto.installmentPurchaseId?.trim()?.takeIf { it.isNotEmpty() }
            if (rawPurchase != null && rawPurchase !in validPurchaseIds) {
                if (id in localById) seenLocal.add(id)
                stats.invalid++
                stats.needsReview++
                issues.add(MergeIssueItem(table, id, MergeIssueKind.INVALID_REJECTED, "Lançamento referencia parcelamento inexistente ($rawPurchase)."))
                return@forEach
            }
            val rawRule = dto.recurringRuleId?.trim()?.takeIf { it.isNotEmpty() }
            if (rawRule != null && rawRule !in validRuleIds) {
                if (id in localById) seenLocal.add(id)
                stats.invalid++
                stats.needsReview++
                issues.add(MergeIssueItem(table, id, MergeIssueKind.INVALID_REJECTED, "Lançamento referencia regra recorrente inexistente ($rawRule)."))
                return@forEach
            }

            val rawCat = dto.categoryId?.trim()?.takeIf { it.isNotEmpty() }
            val resolvedCat = rawCat?.takeIf { it in validCategoryIds }
            val rawProj = dto.projectId?.trim()?.takeIf { it.isNotEmpty() }
            val resolvedProj = rawProj?.takeIf { it in validProjectIds }
            val rawEvent = dto.eventId?.trim()?.takeIf { it.isNotEmpty() }
            val resolvedEvent = rawEvent?.takeIf { it in validEventIds }
            val rawTask = dto.taskId?.trim()?.takeIf { it.isNotEmpty() }
            val resolvedTask = rawTask?.takeIf { it in validTaskIds }
            val sanitizedRef = (rawCat != null && resolvedCat == null) ||
                (rawProj != null && resolvedProj == null) ||
                (rawEvent != null && resolvedEvent == null) ||
                (rawTask != null && resolvedTask == null)

            val candidate = TransactionEntity(
                id = id,
                type = TransactionType.fromStringOrDefault(dto.type),
                description = dto.description.trim(),
                amountCents = dto.amountCents,
                categoryId = resolvedCat,
                status = FinancialStatus.fromStringOrDefault(dto.status),
                paymentMethod = PaymentMethod.fromStringOrDefault(dto.paymentMethod),
                occurredAt = if (dto.occurredAt > 0L) dto.occurredAt else now,
                dueAt = dto.dueAt,
                settledAt = dto.settledAt,
                projectId = resolvedProj,
                eventId = resolvedEvent,
                taskId = resolvedTask,
                eventCostType = dto.eventCostType?.let { EventCostType.fromStringOrDefault(it) },
                isTicketObligation = dto.isTicketObligation,
                creditCardId = rawCard,
                statementId = rawStatement,
                installmentPurchaseId = rawPurchase,
                installmentNumber = if (rawPurchase != null) dto.installmentNumber else null,
                installmentsTotal = if (rawPurchase != null) dto.installmentsTotal else null,
                recurringRuleId = rawRule,
                occurrenceIndex = if (rawRule != null) dto.occurrenceIndex else null,
                notes = dto.notes,
                createdAt = if (dto.createdAt > 0L) dto.createdAt else now,
                updatedAt = if (dto.updatedAt > 0L) dto.updatedAt else now
            )

            // Match existing local ID first
            val local = localById[id]
            if (local != null) {
                seenLocal.add(id)
                if (local.copy(createdAt = 0L, updatedAt = 0L) == candidate.copy(createdAt = 0L, updatedAt = 0L) && !sanitizedRef) {
                    stats.alreadyExisting++
                } else {
                    stats.conflicts++
                    issues.add(MergeIssueItem(table, id, MergeIssueKind.CONFLICT_KEPT_LOCAL, "Lançamento já existe localmente com valores/status diferentes; mantido local."))
                }
                return@forEach
            }

            // Regra 7: Pagamento de fatura não deve ser importado como despesa independente duplicada
            if (isStandaloneStatementPaymentExpense(dto, allStatementsById.keys, allStatementsById.values)) {
                stats.invalid++
                stats.needsReview++
                issues.add(
                    MergeIssueItem(
                        tableName = table,
                        recordId = id,
                        kind = MergeIssueKind.FINANCIAL_INTEGRITY_GUARD,
                        reason = "Pagamento de fatura ignorado para evitar dupla contabilização de despesas."
                    )
                )
                return@forEach
            }

            // Regra 7: Coerência cartão × fatura
            if (candidate.statementId != null) {
                val stmt = allStatementsById[candidate.statementId]!!
                if (candidate.paymentMethod != PaymentMethod.CREDIT_CARD) {
                    stats.invalid++
                    stats.needsReview++
                    issues.add(MergeIssueItem(table, id, MergeIssueKind.FINANCIAL_INTEGRITY_GUARD, "Lançamento vinculado à fatura exige método CREDIT_CARD."))
                    return@forEach
                }
                if (candidate.creditCardId != null && candidate.creditCardId != stmt.creditCardId) {
                    stats.invalid++
                    stats.needsReview++
                    issues.add(MergeIssueItem(table, id, MergeIssueKind.FINANCIAL_INTEGRITY_GUARD, "Cartão do lançamento difere do cartão da fatura vinculada."))
                    return@forEach
                }
                val existingLocalStmt = localStatementsById[stmt.id]
                if (existingLocalStmt != null &&
                    existingLocalStmt.status == StatementStatus.PAID &&
                    candidate.status != FinancialStatus.SETTLED &&
                    candidate.status != FinancialStatus.CANCELLED
                ) {
                    stats.conflicts++
                    stats.needsReview++
                    issues.add(
                        MergeIssueItem(
                            tableName = table,
                            recordId = id,
                            kind = MergeIssueKind.FINANCIAL_INTEGRITY_GUARD,
                            reason = "Lançamento pendente não pode ser inserido em fatura local já quitada (${existingLocalStmt.yearMonth})."
                        )
                    )
                    return@forEach
                }
            }

            // Regra 7: Unicidade de ocorrência recorrente
            if (candidate.recurringRuleId != null && candidate.occurrenceIndex != null) {
                val recKey = candidate.recurringRuleId to candidate.occurrenceIndex
                val colliding = localByRecurrenceOcc[recKey]
                if (colliding != null) {
                    seenLocal.add(colliding.id)
                    stats.conflicts++
                    issues.add(MergeIssueItem(table, id, MergeIssueKind.FINANCIAL_INTEGRITY_GUARD, "Ocorrência #${recKey.second} da recorrência já existe localmente; duplicidade evitada."))
                    return@forEach
                }
                if (recKey in acceptedRecOcc) {
                    stats.alreadyExisting++
                    return@forEach
                }
                acceptedRecOcc.add(recKey)
            }

            // Regra 7: Unicidade de parcela dentro do mesmo parcelamento
            if (candidate.installmentPurchaseId != null && candidate.installmentNumber != null) {
                val instKey = candidate.installmentPurchaseId to candidate.installmentNumber
                val colliding = localByInstallmentNum[instKey]
                if (colliding != null) {
                    seenLocal.add(colliding.id)
                    stats.conflicts++
                    issues.add(MergeIssueItem(table, id, MergeIssueKind.FINANCIAL_INTEGRITY_GUARD, "Parcela ${instKey.second} do parcelamento já existe localmente; duplicidade evitada."))
                    return@forEach
                }
                if (instKey in acceptedInstNum) {
                    stats.alreadyExisting++
                    return@forEach
                }
                acceptedInstNum.add(instKey)
            }

            if (id in accepted) {
                stats.alreadyExisting++
                return@forEach
            }
            accepted[id] = candidate
            stats.newRecords++
            stats.inserted++
            if (sanitizedRef) {
                stats.needsReview++
                issues.add(MergeIssueItem(table, id, MergeIssueKind.REFERENCE_SANITIZED_NEEDS_REVIEW, "Referência opcional do lançamento não encontrada; vínculo limpo."))
            }
        }
        stats.preservedLocalOnly = localItems.count { it.id !in seenLocal }
        perEntity.add(stats.build())
        return accepted.values.toList()
    }

    private fun mergeSubtasks(
        dtos: List<SubtaskBackupDto>,
        localItems: List<SubtaskEntity>,
        validTaskIds: Set<String>,
        now: Long,
        issues: MutableList<MergeIssueItem>,
        perEntity: MutableList<EntityMergeCounts>
    ): List<SubtaskEntity> {
        val table = "subtasks"
        val stats = TableStatsBuilder(table, "Subtarefas", dtos.size)
        val localById = localItems.associateBy { it.id }
        val accepted = mutableMapOf<String, SubtaskEntity>()
        val seenLocal = mutableSetOf<String>()

        dtos.forEach { dto ->
            val id = dto.id.trim()
            val taskId = dto.taskId.trim()
            if (id.isEmpty() || dto.title.isBlank() || taskId.isEmpty() || taskId !in validTaskIds) {
                stats.invalid++
                stats.needsReview++
                issues.add(MergeIssueItem(table, id.ifEmpty { "(sem id)" }, MergeIssueKind.INVALID_REJECTED, "Subtarefa órfã ou inválida (tarefa $taskId inexistente)."))
                return@forEach
            }
            val candidate = SubtaskEntity(
                id = id,
                taskId = taskId,
                title = dto.title.trim(),
                isCompleted = dto.isCompleted,
                sortOrder = dto.sortOrder,
                createdAt = if (dto.createdAt > 0L) dto.createdAt else now,
                updatedAt = if (dto.updatedAt > 0L) dto.updatedAt else now
            )
            val local = localById[id]
            if (local != null) {
                seenLocal.add(id)
                if (local.copy(createdAt = 0L, updatedAt = 0L) == candidate.copy(createdAt = 0L, updatedAt = 0L)) {
                    stats.alreadyExisting++
                } else {
                    stats.conflicts++
                    issues.add(MergeIssueItem(table, id, MergeIssueKind.CONFLICT_KEPT_LOCAL, "Subtarefa já existe com diferenças; mantida local."))
                }
                return@forEach
            }
            if (id in accepted) {
                stats.alreadyExisting++
                return@forEach
            }
            accepted[id] = candidate
            stats.newRecords++
            stats.inserted++
        }
        stats.preservedLocalOnly = localItems.count { it.id !in seenLocal }
        perEntity.add(stats.build())
        return accepted.values.toList()
    }

    private fun mergeRecurrenceExceptions(
        dtos: List<RecurrenceExceptionBackupDto>,
        localItems: List<RecurrenceExceptionEntity>,
        validRecurrenceIds: Set<String>,
        validTaskIds: Set<String>,
        now: Long,
        issues: MutableList<MergeIssueItem>,
        perEntity: MutableList<EntityMergeCounts>
    ): List<RecurrenceExceptionEntity> {
        val table = "recurrence_exceptions"
        val stats = TableStatsBuilder(table, "Exceções de recorrência", dtos.size)
        val localById = localItems.associateBy { it.id }
        val localByNaturalKey = localItems.associateBy { it.recurrenceId to it.occurrenceDateIso }
        val accepted = mutableMapOf<String, RecurrenceExceptionEntity>()
        val acceptedKeys = mutableSetOf<Pair<String, String>>()
        val seenLocal = mutableSetOf<String>()

        dtos.forEach { dto ->
            val id = dto.id.trim()
            val recId = dto.recurrenceId.trim()
            val occIso = dto.occurrenceDateIso.trim()
            if (id.isEmpty() || recId.isEmpty() || occIso.isEmpty() || recId !in validRecurrenceIds) {
                stats.invalid++
                stats.needsReview++
                issues.add(MergeIssueItem(table, id.ifEmpty { "(sem id)" }, MergeIssueKind.INVALID_REJECTED, "Exceção de recorrência órfã ou inválida (série $recId inexistente)."))
                return@forEach
            }
            val rawTask = dto.taskId?.trim()?.takeIf { it.isNotEmpty() }
            val resolvedTask = rawTask?.takeIf { it in validTaskIds }
            val sanitizedRef = rawTask != null && resolvedTask == null

            val candidate = RecurrenceExceptionEntity(
                id = id,
                recurrenceId = recId,
                occurrenceDateIso = occIso,
                exceptionType = RecurrenceExceptionType.fromStringOrDefault(dto.exceptionType),
                overrideTitle = dto.overrideTitle,
                overrideDueAt = dto.overrideDueAt,
                taskId = resolvedTask,
                createdAt = if (dto.createdAt > 0L) dto.createdAt else now,
                updatedAt = if (dto.updatedAt > 0L) dto.updatedAt else now
            )
            val local = localById[id]
            if (local != null) {
                seenLocal.add(id)
                if (local.copy(createdAt = 0L, updatedAt = 0L) == candidate.copy(createdAt = 0L, updatedAt = 0L) && !sanitizedRef) {
                    stats.alreadyExisting++
                } else {
                    stats.conflicts++
                    issues.add(MergeIssueItem(table, id, MergeIssueKind.CONFLICT_KEPT_LOCAL, "Exceção de recorrência já existe com diferenças; mantida local."))
                }
                return@forEach
            }
            val colliding = localByNaturalKey[recId to occIso]
            if (colliding != null) {
                seenLocal.add(colliding.id)
                stats.conflicts++
                issues.add(MergeIssueItem(table, id, MergeIssueKind.CONFLICT_KEPT_LOCAL, "Exceção para $recId em $occIso já existe sob outro ID; mantida local."))
                return@forEach
            }
            if (id in accepted || (recId to occIso) in acceptedKeys) {
                stats.alreadyExisting++
                return@forEach
            }
            accepted[id] = candidate
            acceptedKeys.add(recId to occIso)
            stats.newRecords++
            stats.inserted++
            if (sanitizedRef) {
                stats.needsReview++
                issues.add(MergeIssueItem(table, id, MergeIssueKind.REFERENCE_SANITIZED_NEEDS_REVIEW, "Tarefa vinculada à exceção ($rawTask) ausente; vínculo removido."))
            }
        }
        stats.preservedLocalOnly = localItems.count { it.id !in seenLocal }
        perEntity.add(stats.build())
        return accepted.values.toList()
    }

    private fun mergeReminders(
        dtos: List<ReminderBackupDto>,
        localItems: List<ReminderEntity>,
        validTaskIds: Set<String>,
        validAppointmentIds: Set<String>,
        validEventIds: Set<String>,
        validTransactionIds: Set<String>,
        validStatementIds: Set<String>,
        now: Long,
        issues: MutableList<MergeIssueItem>,
        perEntity: MutableList<EntityMergeCounts>
    ): List<ReminderEntity> {
        val table = "reminders"
        val stats = TableStatsBuilder(table, "Lembretes", dtos.size)
        val localById = localItems.associateBy { it.id }
        val localBySourceKey = localItems.associateBy { Triple(it.sourceType, it.sourceId, it.category) }
        val accepted = mutableMapOf<String, ReminderEntity>()
        val acceptedSourceKeys = mutableSetOf<Triple<ReminderSourceType, String, ReminderCategory>>()
        val seenLocal = mutableSetOf<String>()

        dtos.forEach { dto ->
            val id = dto.id.trim()
            val sourceId = dto.sourceId.trim()
            if (id.isEmpty() || sourceId.isEmpty() || dto.title.isBlank()) {
                stats.invalid++
                stats.needsReview++
                issues.add(MergeIssueItem(table, id.ifEmpty { "(sem id)" }, MergeIssueKind.INVALID_REJECTED, "Lembrete sem ID estável, título ou origem."))
                return@forEach
            }
            val sourceType = ReminderSourceType.fromStringOrDefault(dto.sourceType)
            val sourceExists = when (sourceType) {
                ReminderSourceType.TASK -> sourceId in validTaskIds
                ReminderSourceType.APPOINTMENT -> sourceId in validAppointmentIds
                ReminderSourceType.EVENT -> sourceId in validEventIds
                ReminderSourceType.TRANSACTION -> sourceId in validTransactionIds
                ReminderSourceType.STATEMENT -> sourceId in validStatementIds
                ReminderSourceType.ROUTINE_MORNING,
                ReminderSourceType.ROUTINE_NIGHT -> true
            }
            if (!sourceExists) {
                stats.invalid++
                stats.needsReview++
                issues.add(MergeIssueItem(table, id, MergeIssueKind.INVALID_REJECTED, "Lembrete órfão: origem ${sourceType.name} ($sourceId) não existe."))
                return@forEach
            }

            val category = ReminderCategory.fromStringOrDefault(dto.category)
            val candidate = ReminderEntity(
                id = id,
                sourceType = sourceType,
                sourceId = sourceId,
                category = category,
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

            val local = localById[id]
            if (local != null) {
                seenLocal.add(id)
                if (local.copy(createdAt = 0L, updatedAt = 0L) == candidate.copy(createdAt = 0L, updatedAt = 0L)) {
                    stats.alreadyExisting++
                } else {
                    stats.conflicts++
                    issues.add(MergeIssueItem(table, id, MergeIssueKind.CONFLICT_KEPT_LOCAL, "Lembrete já existe com horário/status diferente; mantido local."))
                }
                return@forEach
            }

            val sourceKey = Triple(sourceType, sourceId, category)
            val colliding = localBySourceKey[sourceKey]
            if (colliding != null) {
                seenLocal.add(colliding.id)
                stats.conflicts++
                issues.add(MergeIssueItem(table, id, MergeIssueKind.CONFLICT_KEPT_LOCAL, "Lembrete para mesma origem ($sourceType/$sourceId) já existe localmente; mantido local."))
                return@forEach
            }
            if (id in accepted || sourceKey in acceptedSourceKeys) {
                stats.alreadyExisting++
                return@forEach
            }
            accepted[id] = candidate
            acceptedSourceKeys.add(sourceKey)
            stats.newRecords++
            stats.inserted++
        }
        stats.preservedLocalOnly = localItems.count { it.id !in seenLocal }
        perEntity.add(stats.build())
        return accepted.values.toList()
    }

    private fun isStandaloneStatementPaymentExpense(
        dto: TransactionBackupDto,
        validStatementIds: Set<String>,
        allStatements: Collection<CreditCardStatementEntity>
    ): Boolean {
        if (dto.statementId != null && dto.statementId in validStatementIds) return false
        if (dto.installmentPurchaseId != null) return false
        if (TransactionType.fromStringOrDefault(dto.type) != TransactionType.EXPENSE) return false
        val normalizedDesc = dto.description.trim().lowercase()
        val looksLikeStatementPayment = normalizedDesc.contains("pagamento de fatura") ||
            normalizedDesc.contains("pagamento fatura") ||
            normalizedDesc.contains("quitação de fatura") ||
            normalizedDesc.contains("quitacao de fatura") ||
            normalizedDesc == "fatura paga"
        if (!looksLikeStatementPayment) return false
        return !dto.creditCardId.isNullOrBlank() ||
            allStatements.any { stmt ->
                stmt.status == StatementStatus.PAID &&
                    (stmt.paidAmountCents == dto.amountCents || stmt.creditCardId == dto.creditCardId)
            }
    }

    private fun normalizeRoutineSettings(
        dto: RoutineAssistantSettingsBackupDto,
        createdAt: Long,
        updatedAt: Long
    ): RoutineAssistantSettingsEntity = RoutineAssistantSettingsEntity(
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
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    private fun normalizePersonalization(
        dto: PersonalizationSettingsBackupDto,
        createdAt: Long,
        updatedAt: Long
    ): PersonalizationSettingsEntity = PersonalizationSettingsEntity(
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
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    private fun areMarketAlertsSemanticallyEqual(
        a: MarketPriceAlert,
        b: MarketPriceAlert
    ): Boolean = a.id == b.id &&
        a.assetCode == b.assetCode &&
        a.condition == b.condition &&
        a.targetPrice == b.targetPrice &&
        a.isActive == b.isActive &&
        a.status == b.status
}
