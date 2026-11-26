package com.nexus.app.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.nexus.app.core.DateTimeUtils
import com.nexus.app.domain.model.CategoryScope
import com.nexus.app.domain.rules.RecurrenceEngine

/**
 * Estrategia permanente de evolucao do banco NEXUS.
 *
 * REGRA OBRIGATORIA: Nunca usar fallbackToDestructiveMigration().
 * Toda evolucao adiciona uma Migration explicita preservando todos os dados existentes.
 */
object NexusMigrations {
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE settings ADD COLUMN declaredBalanceCents INTEGER NOT NULL DEFAULT 0")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS credit_cards (
                    id TEXT NOT NULL PRIMARY KEY,
                    name TEXT NOT NULL,
                    institution TEXT NOT NULL,
                    totalLimitCents INTEGER NOT NULL,
                    availableLimitCents INTEGER NOT NULL,
                    closingDay INTEGER NOT NULL,
                    dueDay INTEGER NOT NULL,
                    colorHex TEXT NOT NULL,
                    notes TEXT NOT NULL,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL
                )
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS events (
                    id TEXT NOT NULL PRIMARY KEY,
                    name TEXT NOT NULL,
                    description TEXT NOT NULL,
                    categoryId TEXT,
                    startAt INTEGER NOT NULL,
                    endAt INTEGER NOT NULL,
                    isAllDay INTEGER NOT NULL,
                    officialLink TEXT NOT NULL,
                    ticketLink TEXT NOT NULL,
                    locationName TEXT NOT NULL,
                    address TEXT NOT NULL,
                    city TEXT NOT NULL,
                    state TEXT NOT NULL,
                    notes TEXT NOT NULL,
                    eventStatus TEXT NOT NULL,
                    ticketStatus TEXT NOT NULL,
                    ticketPriceCents INTEGER NOT NULL,
                    ticketQuantity INTEGER NOT NULL,
                    ticketFeesCents INTEGER NOT NULL,
                    ticketTotalCents INTEGER NOT NULL,
                    ticketPurchaseDate INTEGER,
                    ticketDueDate INTEGER,
                    paymentMethod TEXT NOT NULL,
                    creditCardId TEXT,
                    installmentsCount INTEGER NOT NULL,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    FOREIGN KEY(categoryId) REFERENCES categories(id) ON DELETE SET NULL,
                    FOREIGN KEY(creditCardId) REFERENCES credit_cards(id) ON DELETE SET NULL
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_events_categoryId ON events(categoryId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_events_creditCardId ON events(creditCardId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_events_startAt ON events(startAt)")

            db.execSQL("ALTER TABLE tasks ADD COLUMN eventId TEXT REFERENCES events(id) ON DELETE SET NULL")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_tasks_eventId ON tasks(eventId)")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS credit_card_statements (
                    id TEXT NOT NULL PRIMARY KEY,
                    creditCardId TEXT NOT NULL,
                    yearMonth TEXT NOT NULL,
                    closingAt INTEGER NOT NULL,
                    dueAt INTEGER NOT NULL,
                    status TEXT NOT NULL,
                    paidAt INTEGER,
                    paidAmountCents INTEGER NOT NULL,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    FOREIGN KEY(creditCardId) REFERENCES credit_cards(id) ON DELETE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_credit_card_statements_creditCardId ON credit_card_statements(creditCardId)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_credit_card_statements_creditCardId_yearMonth ON credit_card_statements(creditCardId, yearMonth)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_credit_card_statements_dueAt ON credit_card_statements(dueAt)")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS installment_purchases (
                    id TEXT NOT NULL PRIMARY KEY,
                    description TEXT NOT NULL,
                    totalAmountCents INTEGER NOT NULL,
                    installmentsCount INTEGER NOT NULL,
                    installmentAmountCents INTEGER NOT NULL,
                    firstDueAt INTEGER NOT NULL,
                    creditCardId TEXT,
                    categoryId TEXT,
                    eventId TEXT,
                    projectId TEXT,
                    notes TEXT NOT NULL,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    FOREIGN KEY(creditCardId) REFERENCES credit_cards(id) ON DELETE SET NULL,
                    FOREIGN KEY(categoryId) REFERENCES categories(id) ON DELETE SET NULL,
                    FOREIGN KEY(eventId) REFERENCES events(id) ON DELETE SET NULL,
                    FOREIGN KEY(projectId) REFERENCES projects(id) ON DELETE SET NULL
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_installment_purchases_creditCardId ON installment_purchases(creditCardId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_installment_purchases_categoryId ON installment_purchases(categoryId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_installment_purchases_eventId ON installment_purchases(eventId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_installment_purchases_projectId ON installment_purchases(projectId)")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS recurring_rules (
                    id TEXT NOT NULL PRIMARY KEY,
                    title TEXT NOT NULL,
                    type TEXT NOT NULL,
                    amountCents INTEGER NOT NULL,
                    categoryId TEXT,
                    paymentMethod TEXT NOT NULL,
                    creditCardId TEXT,
                    frequency TEXT NOT NULL,
                    customIntervalDays INTEGER NOT NULL,
                    startAt INTEGER NOT NULL,
                    endAt INTEGER,
                    maxOccurrences INTEGER,
                    isActive INTEGER NOT NULL,
                    notes TEXT NOT NULL,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    FOREIGN KEY(categoryId) REFERENCES categories(id) ON DELETE SET NULL,
                    FOREIGN KEY(creditCardId) REFERENCES credit_cards(id) ON DELETE SET NULL
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_recurring_rules_categoryId ON recurring_rules(categoryId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_recurring_rules_creditCardId ON recurring_rules(creditCardId)")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS transactions (
                    id TEXT NOT NULL PRIMARY KEY,
                    type TEXT NOT NULL,
                    description TEXT NOT NULL,
                    amountCents INTEGER NOT NULL,
                    categoryId TEXT,
                    status TEXT NOT NULL,
                    paymentMethod TEXT NOT NULL,
                    occurredAt INTEGER NOT NULL,
                    dueAt INTEGER,
                    settledAt INTEGER,
                    projectId TEXT,
                    eventId TEXT,
                    eventCostType TEXT,
                    isTicketObligation INTEGER NOT NULL,
                    creditCardId TEXT,
                    statementId TEXT,
                    installmentPurchaseId TEXT,
                    installmentNumber INTEGER,
                    installmentsTotal INTEGER,
                    recurringRuleId TEXT,
                    occurrenceIndex INTEGER,
                    notes TEXT NOT NULL,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    FOREIGN KEY(categoryId) REFERENCES categories(id) ON DELETE SET NULL,
                    FOREIGN KEY(projectId) REFERENCES projects(id) ON DELETE SET NULL,
                    FOREIGN KEY(eventId) REFERENCES events(id) ON DELETE SET NULL,
                    FOREIGN KEY(creditCardId) REFERENCES credit_cards(id) ON DELETE SET NULL,
                    FOREIGN KEY(statementId) REFERENCES credit_card_statements(id) ON DELETE SET NULL,
                    FOREIGN KEY(installmentPurchaseId) REFERENCES installment_purchases(id) ON DELETE SET NULL,
                    FOREIGN KEY(recurringRuleId) REFERENCES recurring_rules(id) ON DELETE SET NULL
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_categoryId ON transactions(categoryId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_projectId ON transactions(projectId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_eventId ON transactions(eventId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_creditCardId ON transactions(creditCardId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_statementId ON transactions(statementId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_installmentPurchaseId ON transactions(installmentPurchaseId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_recurringRuleId ON transactions(recurringRuleId)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_transactions_recurringRuleId_occurrenceIndex ON transactions(recurringRuleId, occurrenceIndex)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_dueAt ON transactions(dueAt)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_occurredAt ON transactions(occurredAt)")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS loans (
                    id TEXT NOT NULL PRIMARY KEY,
                    creditor TEXT NOT NULL,
                    originalAmountCents INTEGER NOT NULL,
                    contractedAmountCents INTEGER NOT NULL,
                    installmentsTotal INTEGER NOT NULL,
                    installmentAmountCents INTEGER NOT NULL,
                    paidInstallments INTEGER NOT NULL,
                    nextDueAt INTEGER,
                    remainingBalanceCents INTEGER NOT NULL,
                    finalDateAt INTEGER,
                    notes TEXT NOT NULL,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL
                )
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS budgets (
                    id TEXT NOT NULL PRIMARY KEY,
                    categoryId TEXT NOT NULL,
                    yearMonth TEXT NOT NULL,
                    limitCents INTEGER NOT NULL,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    FOREIGN KEY(categoryId) REFERENCES categories(id) ON DELETE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_budgets_categoryId ON budgets(categoryId)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_budgets_categoryId_yearMonth ON budgets(categoryId, yearMonth)")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS financial_goals (
                    id TEXT NOT NULL PRIMARY KEY,
                    name TEXT NOT NULL,
                    targetAmountCents INTEGER NOT NULL,
                    currentAmountCents INTEGER NOT NULL,
                    deadlineAt INTEGER,
                    categoryId TEXT,
                    notes TEXT NOT NULL,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    FOREIGN KEY(categoryId) REFERENCES categories(id) ON DELETE SET NULL
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_financial_goals_categoryId ON financial_goals(categoryId)")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS audit_history (
                    id TEXT NOT NULL PRIMARY KEY,
                    entityType TEXT NOT NULL,
                    entityId TEXT NOT NULL,
                    action TEXT NOT NULL,
                    summary TEXT NOT NULL,
                    occurredAt INTEGER NOT NULL,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_audit_history_occurredAt ON audit_history(occurredAt)")

            seedDefaultFinancialCategories(db)
        }
    }

    fun seedDefaultFinancialCategories(db: SupportSQLiteDatabase) {
        val now = DateTimeUtils.nowEpochMillis()
        val financeCategories = listOf(
            Triple("cat-fin-alimentacao", "Alimentação", "#E5A000"),
            Triple("cat-fin-transporte", "Transporte", "#4C8BF5"),
            Triple("cat-fin-faculdade", "Faculdade", "#9A9A9A"),
            Triple("cat-fin-moradia", "Moradia", "#F2F2F2"),
            Triple("cat-fin-tecnologia", "Tecnologia", "#4C8BF5"),
            Triple("cat-fin-assinaturas", "Assinaturas", "#E5A000"),
            Triple("cat-fin-compras", "Compras", "#9A9A9A"),
            Triple("cat-fin-eventos", "Eventos", "#34C759"),
            Triple("cat-fin-saude", "Saúde e bem-estar", "#34C759"),
            Triple("cat-fin-salario", "Salário", "#34C759"),
            Triple("cat-fin-freelance", "Freelance", "#34C759"),
            Triple("cat-fin-outros", "Outros", "#777777")
        )
        financeCategories.forEach { (id, name, color) ->
            db.execSQL(
                """
                INSERT OR IGNORE INTO categories (id, name, colorHex, scope, isDefault, createdAt, updatedAt)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                arrayOf<Any>(id, name, color, CategoryScope.FINANCE.name, 1, now, now)
            )
        }
    }

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS reminders (
                    id TEXT NOT NULL PRIMARY KEY,
                    sourceType TEXT NOT NULL,
                    sourceId TEXT NOT NULL,
                    category TEXT NOT NULL,
                    title TEXT NOT NULL,
                    message TEXT NOT NULL,
                    triggerAt INTEGER NOT NULL,
                    originalDueAt INTEGER,
                    minutesBefore INTEGER NOT NULL,
                    channelId TEXT NOT NULL,
                    status TEXT NOT NULL,
                    snoozedUntil INTEGER,
                    isExactScheduled INTEGER NOT NULL,
                    firedAt INTEGER,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS index_reminders_sourceType_sourceId_category ON reminders(sourceType, sourceId, category)"
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_reminders_status ON reminders(status)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_reminders_triggerAt ON reminders(triggerAt)")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS routine_assistant_settings (
                    id TEXT NOT NULL PRIMARY KEY,
                    notificationsEnabled INTEGER NOT NULL,
                    scheduleChannelEnabled INTEGER NOT NULL,
                    financeChannelEnabled INTEGER NOT NULL,
                    eventsChannelEnabled INTEGER NOT NULL,
                    routineChannelEnabled INTEGER NOT NULL,
                    morningBriefingEnabled INTEGER NOT NULL,
                    morningBriefingHour INTEGER NOT NULL,
                    morningBriefingMinute INTEGER NOT NULL,
                    nightReviewEnabled INTEGER NOT NULL,
                    nightReviewHour INTEGER NOT NULL,
                    nightReviewMinute INTEGER NOT NULL,
                    quietHoursEnabled INTEGER NOT NULL,
                    quietHoursStartHour INTEGER NOT NULL,
                    quietHoursStartMinute INTEGER NOT NULL,
                    quietHoursEndHour INTEGER NOT NULL,
                    quietHoursEndMinute INTEGER NOT NULL,
                    defaultTaskReminderMinutes INTEGER NOT NULL,
                    defaultAppointmentReminderMinutes INTEGER NOT NULL,
                    defaultEventReminderMinutes INTEGER NOT NULL,
                    defaultFinanceReminderMinutes INTEGER NOT NULL,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL
                )
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS briefing_history (
                    id TEXT NOT NULL PRIMARY KEY,
                    briefingType TEXT NOT NULL,
                    dateIso TEXT NOT NULL,
                    summaryText TEXT NOT NULL,
                    tasksCount INTEGER NOT NULL,
                    appointmentsCount INTEGER NOT NULL,
                    billsDueCount INTEGER NOT NULL,
                    overdueCount INTEGER NOT NULL,
                    reviewedAt INTEGER,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS index_briefing_history_briefingType_dateIso ON briefing_history(briefingType, dateIso)"
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_briefing_history_dateIso ON briefing_history(dateIso)")

            seedDefaultRoutineSettings(db)
        }
    }

    fun seedDefaultRoutineSettings(db: SupportSQLiteDatabase) {
        val now = DateTimeUtils.nowEpochMillis()
        db.execSQL(
            """
            INSERT OR IGNORE INTO routine_assistant_settings (
                id, notificationsEnabled, scheduleChannelEnabled, financeChannelEnabled,
                eventsChannelEnabled, routineChannelEnabled, morningBriefingEnabled,
                morningBriefingHour, morningBriefingMinute, nightReviewEnabled,
                nightReviewHour, nightReviewMinute, quietHoursEnabled,
                quietHoursStartHour, quietHoursStartMinute, quietHoursEndHour,
                quietHoursEndMinute, defaultTaskReminderMinutes,
                defaultAppointmentReminderMinutes, defaultEventReminderMinutes,
                defaultFinanceReminderMinutes, createdAt, updatedAt
            ) VALUES (?, 1, 1, 1, 1, 1, 1, 8, 0, 1, 21, 0, 0, 22, 0, 7, 0, 30, 30, 60, 0, ?, ?)
            """.trimIndent(),
            arrayOf<Any>("nexus_routine_default", now, now)
        )
    }

    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS personalization_settings (
                    id TEXT NOT NULL PRIMARY KEY,
                    greetingEnabled INTEGER NOT NULL,
                    greetingTemplate TEXT NOT NULL,
                    dateFormatPattern TEXT NOT NULL,
                    showDailyQuoteOnDashboard INTEGER NOT NULL,
                    compactDashboardMode INTEGER NOT NULL,
                    showMetricSummaryOnDashboard INTEGER NOT NULL,
                    dashboardBlocksJson TEXT NOT NULL,
                    lastQuoteDateIso TEXT,
                    lastQuoteId TEXT,
                    previousQuoteId TEXT,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL
                )
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS quotes (
                    id TEXT NOT NULL PRIMARY KEY,
                    text TEXT NOT NULL,
                    author TEXT NOT NULL,
                    isActive INTEGER NOT NULL,
                    lastShownDateIso TEXT,
                    shownCount INTEGER NOT NULL,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_quotes_isActive ON quotes(isActive)")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS personal_goals (
                    id TEXT NOT NULL PRIMARY KEY,
                    title TEXT NOT NULL,
                    description TEXT NOT NULL,
                    categoryId TEXT,
                    targetDateAt INTEGER,
                    status TEXT NOT NULL,
                    progressMode TEXT NOT NULL,
                    manualProgressPercent INTEGER NOT NULL,
                    completedAt INTEGER,
                    notes TEXT NOT NULL,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    FOREIGN KEY(categoryId) REFERENCES categories(id) ON DELETE SET NULL
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_personal_goals_categoryId ON personal_goals(categoryId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_personal_goals_status ON personal_goals(status)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_personal_goals_targetDateAt ON personal_goals(targetDateAt)")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS goal_milestones (
                    id TEXT NOT NULL PRIMARY KEY,
                    goalId TEXT NOT NULL,
                    title TEXT NOT NULL,
                    description TEXT NOT NULL,
                    dueAt INTEGER,
                    isCompleted INTEGER NOT NULL,
                    completedAt INTEGER,
                    sortOrder INTEGER NOT NULL,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    FOREIGN KEY(goalId) REFERENCES personal_goals(id) ON DELETE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_goal_milestones_goalId ON goal_milestones(goalId)")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS goal_progress_logs (
                    id TEXT NOT NULL PRIMARY KEY,
                    goalId TEXT NOT NULL,
                    progressPercent INTEGER NOT NULL,
                    note TEXT NOT NULL,
                    recordedAt INTEGER NOT NULL,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    FOREIGN KEY(goalId) REFERENCES personal_goals(id) ON DELETE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_goal_progress_logs_goalId ON goal_progress_logs(goalId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_goal_progress_logs_recordedAt ON goal_progress_logs(recordedAt)")

            db.execSQL("ALTER TABLE projects ADD COLUMN goalId TEXT REFERENCES personal_goals(id) ON DELETE SET NULL")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_projects_goalId ON projects(goalId)")

            db.execSQL("ALTER TABLE tasks ADD COLUMN goalId TEXT REFERENCES personal_goals(id) ON DELETE SET NULL")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_tasks_goalId ON tasks(goalId)")

            seedDefaultPersonalizationSettings(db)
        }
    }

    fun seedDefaultPersonalizationSettings(db: SupportSQLiteDatabase) {
        val now = DateTimeUtils.nowEpochMillis()
        db.execSQL(
            """
            INSERT OR IGNORE INTO personalization_settings (
                id, greetingEnabled, greetingTemplate, dateFormatPattern,
                showDailyQuoteOnDashboard, compactDashboardMode, showMetricSummaryOnDashboard,
                dashboardBlocksJson, lastQuoteDateIso, lastQuoteId, previousQuoteId,
                createdAt, updatedAt
            ) VALUES (?, 1, 'AUTO', 'WEEKDAY_DD_MM_YYYY', 1, 0, 1, '', NULL, NULL, NULL, ?, ?)
            """.trimIndent(),
            arrayOf<Any>("nexus_personalization_default", now, now)
        )
    }

    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE appointments ADD COLUMN eventId TEXT REFERENCES events(id) ON DELETE SET NULL")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_appointments_eventId ON appointments(eventId)")

            db.execSQL("ALTER TABLE transactions ADD COLUMN taskId TEXT REFERENCES tasks(id) ON DELETE SET NULL")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_taskId ON transactions(taskId)")
        }
    }

    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS recurrences (
                    id TEXT NOT NULL PRIMARY KEY,
                    title TEXT NOT NULL,
                    description TEXT NOT NULL,
                    projectId TEXT,
                    categoryId TEXT,
                    eventId TEXT,
                    goalId TEXT,
                    priority TEXT NOT NULL,
                    notes TEXT NOT NULL,
                    hasDueTime INTEGER NOT NULL,
                    dueTimeMinutesOfDay INTEGER,
                    reminderMinutesBefore INTEGER,
                    frequency TEXT NOT NULL,
                    customIntervalDays INTEGER NOT NULL,
                    weekdaysMask INTEGER NOT NULL,
                    startDateIso TEXT NOT NULL,
                    endDateIso TEXT,
                    maxOccurrences INTEGER,
                    isActive INTEGER NOT NULL,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    FOREIGN KEY(projectId) REFERENCES projects(id) ON DELETE SET NULL,
                    FOREIGN KEY(categoryId) REFERENCES categories(id) ON DELETE SET NULL,
                    FOREIGN KEY(eventId) REFERENCES events(id) ON DELETE SET NULL,
                    FOREIGN KEY(goalId) REFERENCES personal_goals(id) ON DELETE SET NULL
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_recurrences_projectId ON recurrences(projectId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_recurrences_categoryId ON recurrences(categoryId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_recurrences_eventId ON recurrences(eventId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_recurrences_goalId ON recurrences(goalId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_recurrences_isActive ON recurrences(isActive)")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS recurrence_exceptions (
                    id TEXT NOT NULL PRIMARY KEY,
                    recurrenceId TEXT NOT NULL,
                    occurrenceDateIso TEXT NOT NULL,
                    exceptionType TEXT NOT NULL,
                    overrideTitle TEXT,
                    overrideDueAt INTEGER,
                    taskId TEXT,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    FOREIGN KEY(recurrenceId) REFERENCES recurrences(id) ON DELETE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_recurrence_exceptions_recurrenceId ON recurrence_exceptions(recurrenceId)")
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS index_recurrence_exceptions_recurrenceId_occurrenceDateIso ON recurrence_exceptions(recurrenceId, occurrenceDateIso)"
            )

            db.execSQL("ALTER TABLE tasks ADD COLUMN recurrenceId TEXT REFERENCES recurrences(id) ON DELETE SET NULL")
            db.execSQL("ALTER TABLE tasks ADD COLUMN occurrenceDateIso TEXT")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_tasks_recurrenceId ON tasks(recurrenceId)")
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS index_tasks_recurrenceId_occurrenceDateIso ON tasks(recurrenceId, occurrenceDateIso)"
            )
        }
    }

    val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE appointments ADD COLUMN recurrenceId TEXT REFERENCES recurrences(id) ON DELETE SET NULL")
            db.execSQL("ALTER TABLE appointments ADD COLUMN occurrenceDateIso TEXT")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_appointments_recurrenceId ON appointments(recurrenceId)")
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS index_appointments_recurrenceId_occurrenceDateIso ON appointments(recurrenceId, occurrenceDateIso)"
            )

            db.execSQL("ALTER TABLE events ADD COLUMN recurrenceId TEXT REFERENCES recurrences(id) ON DELETE SET NULL")
            db.execSQL("ALTER TABLE events ADD COLUMN occurrenceDateIso TEXT")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_events_recurrenceId ON events(recurrenceId)")
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS index_events_recurrenceId_occurrenceDateIso ON events(recurrenceId, occurrenceDateIso)"
            )

            db.execSQL("ALTER TABLE recurrences ADD COLUMN domain TEXT NOT NULL DEFAULT 'TASK'")
            db.execSQL("ALTER TABLE recurrences ADD COLUMN isAllDay INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE recurrences ADD COLUMN durationMillis INTEGER NOT NULL DEFAULT 3600000")
            db.execSQL("ALTER TABLE recurrences ADD COLUMN location TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE recurrences ADD COLUMN link TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE recurrences ADD COLUMN officialLink TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE recurrences ADD COLUMN ticketLink TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE recurrences ADD COLUMN locationName TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE recurrences ADD COLUMN address TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE recurrences ADD COLUMN city TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE recurrences ADD COLUMN state TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE recurrences ADD COLUMN eventStatus TEXT NOT NULL DEFAULT 'PLANNED'")
            db.execSQL("ALTER TABLE recurrences ADD COLUMN ticketStatus TEXT NOT NULL DEFAULT 'FREE'")
            db.execSQL("ALTER TABLE recurrences ADD COLUMN ticketPriceCents INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE recurrences ADD COLUMN ticketQuantity INTEGER NOT NULL DEFAULT 1")
            db.execSQL("ALTER TABLE recurrences ADD COLUMN ticketFeesCents INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE recurrences ADD COLUMN paymentMethod TEXT NOT NULL DEFAULT 'PIX'")
            db.execSQL("ALTER TABLE recurrences ADD COLUMN creditCardId TEXT")
            db.execSQL("ALTER TABLE recurrences ADD COLUMN installmentsCount INTEGER NOT NULL DEFAULT 1")
            db.execSQL("ALTER TABLE recurrences ADD COLUMN templateSubtasksJson TEXT NOT NULL DEFAULT '[]'")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_recurrences_domain ON recurrences(domain)")

            db.execSQL("ALTER TABLE recurrence_exceptions ADD COLUMN overrideEndAt INTEGER")
            db.execSQL("ALTER TABLE recurrence_exceptions ADD COLUMN overrideDescription TEXT")
            db.execSQL("ALTER TABLE recurrence_exceptions ADD COLUMN overrideLocation TEXT")
            db.execSQL("ALTER TABLE recurrence_exceptions ADD COLUMN overrideLink TEXT")
            db.execSQL("ALTER TABLE recurrence_exceptions ADD COLUMN overrideIsAllDay INTEGER")
            db.execSQL("ALTER TABLE recurrence_exceptions ADD COLUMN appointmentId TEXT")
            db.execSQL("ALTER TABLE recurrence_exceptions ADD COLUMN eventId TEXT")

            // Backfill legacy v6 checklist template into recurrences.templateSubtasksJson
            val recurrenceIds = mutableListOf<String>()
            db.query("SELECT id FROM recurrences").use { cursor ->
                while (cursor.moveToNext()) {
                    recurrenceIds += cursor.getString(0)
                }
            }
            recurrenceIds.forEach { recId ->
                var sourceTaskId: String? = null
                db.query(
                    """
                    SELECT id FROM tasks
                    WHERE recurrenceId = ?
                    ORDER BY CASE WHEN status != 'COMPLETED' AND status != 'CANCELLED' THEN 0 ELSE 1 END, createdAt DESC
                    LIMIT 1
                    """.trimIndent(),
                    arrayOf<Any>(recId)
                ).use { taskCursor ->
                    if (taskCursor.moveToFirst()) {
                        sourceTaskId = taskCursor.getString(0)
                    }
                }
                val taskId = sourceTaskId ?: return@forEach
                val titles = mutableListOf<String>()
                db.query(
                    "SELECT title FROM subtasks WHERE taskId = ? ORDER BY sortOrder ASC, createdAt ASC",
                    arrayOf<Any>(taskId)
                ).use { subCursor ->
                    while (subCursor.moveToNext()) {
                        val t = subCursor.getString(0)?.trim().orEmpty()
                        if (t.isNotEmpty()) titles += t
                    }
                }
                if (titles.isNotEmpty()) {
                    db.execSQL(
                        "UPDATE recurrences SET templateSubtasksJson = ? WHERE id = ?",
                        arrayOf<Any>(RecurrenceEngine.serializeChecklistTemplate(titles), recId)
                    )
                }
            }
        }
    }

    val ALL_MIGRATIONS: Array<Migration> = arrayOf(
        MIGRATION_1_2,
        MIGRATION_2_3,
        MIGRATION_3_4,
        MIGRATION_4_5,
        MIGRATION_5_6,
        MIGRATION_6_7
    )
}


