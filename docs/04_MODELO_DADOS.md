# 04 — Modelo de Dados & Dicionário Completo (`Room v6 + Stores Externos + Backup v7`)

> **Fonte exata:** `NexusDatabase.kt`, `NexusEntities.kt`, `NexusMigrations.kt`, `NexusTypeConverters.kt`, `NexusEnums.kt`, `BackupModels.kt`, `BackupManager.kt`, `data/market/*Store.kt` e `NexusWidgetConfigStore.kt`.

---

## 1. Visão Geral da Persistência

O NEXUS divide sua persistência em **duas camadas complementares**:
1. **Room SQLite (`nexus_local.db`, `DATABASE_VERSION = 6`, `exportSchema = true`)**: `27` tabelas e `27` DAOs contendo todos os dados estruturados do usuário (rotina, produtividade, financeiro, objetivos, lembretes, logs).
2. **Persistência Isolada fora do Room**:
   - **Módulo Mercado (`data/market/*Store.kt`):** cache de cotações e favoritos em `SharedPreferences`; histórico de preços e alertas de preço em arquivos JSON atômicos em `filesDir`.
   - **Widgets Glance (`widgets/NexusWidgetConfigStore.kt`):** preferências visuais por `appWidgetId` em `SharedPreferences`.

> **Pasta `app/schemas/`:** `exportSchema = true` e `ksp { arg("room.schemaLocation", "$projectDir/schemas") }` estão configurados em `app/build.gradle.kts`, porém o diretório `app/schemas/` **não existe no disco** no repositório atual (é gerado apenas em compilação KSP real).

---

## 2. Dicionário Completo das 27 Entidades Room (`DATABASE_VERSION = 6`)

> **Padrão transversal:** todas as `27` tabelas usam PK `id: String` (`TEXT NOT NULL PRIMARY KEY`) e timestamps `createdAt: Long` / `updatedAt: Long` (`INTEGER NOT NULL`, epoch ms UTC), omitidos na descrição extra quando triviais. Timestamps usam epoch ms UTC; dias calendário isolados usam `dateIso` (`YYYY-MM-DD`) e competências mensais usam `yearMonth` (`YYYY-MM`).

### 2.1 Núcleo, Configurações e Categorias

| Tabela (`@Entity`) | Colunas Completas (Nome : Tipo Kotlin [Default]) | PK / FKs / Índices |
| --- | --- | --- |
| `settings` (`SettingsEntity`) | `id: String ["nexus_settings_default"]`, `userName: String ["Enzo"]`, `darkTheme: Boolean [true]`, `declaredBalanceCents: Long [0L]`, `createdAt: Long`, `updatedAt: Long` | PK: `id` (singleton `SINGLETON_ID`) |
| `categories` (`CategoryEntity`) | `id: String`, `name: String`, `colorHex: String`, `scope: CategoryScope [GENERAL]`, `isDefault: Boolean [false]`, `createdAt: Long`, `updatedAt: Long` | PK: `id`. Seed inicial: 4 `GENERAL` + 12 `FINANCE` |

### 2.2 Produtividade, Agenda e Recorrência

| Tabela (`@Entity`) | Colunas Completas | FKs e Índices |
| --- | --- | --- |
| `projects` (`ProjectEntity`) | `id: String`, `name: String`, `description: String [""]`, `deadlineAt: Long? [null]`, `status: ProjectStatus [ACTIVE]`, `priority: TaskPriority [NORMAL]`, `goalId: String? [null]`, `createdAt: Long`, `updatedAt: Long` | FK `goalId → personal_goals(id)` (`SET_NULL`). Ídx: `goalId` |
| `events` (`EventEntity`) | `id: String`, `name: String`, `description: String [""]`, `categoryId: String? [null]`, `startAt: Long`, `endAt: Long`, `isAllDay: Boolean [false]`, `officialLink: String [""]`, `ticketLink: String [""]`, `locationName: String [""]`, `address: String [""]`, `city: String [""]`, `state: String [""]`, `notes: String [""]`, `eventStatus: EventLifecycleStatus [PLANNED]`, `ticketStatus: TicketStatus [FREE]`, `ticketPriceCents: Long [0L]`, `ticketQuantity: Int [1]`, `ticketFeesCents: Long [0L]`, `ticketTotalCents: Long [0L]`, `ticketPurchaseDate: Long? [null]`, `ticketDueDate: Long? [null]`, `paymentMethod: PaymentMethod [PIX]`, `creditCardId: String? [null]`, `installmentsCount: Int [1]`, `createdAt: Long`, `updatedAt: Long` | FKs `categoryId → categories(id)` (`SET_NULL`), `creditCardId → credit_cards(id)` (`SET_NULL`). Ídx: `categoryId`, `creditCardId`, `startAt` |
| `tasks` (`TaskEntity`) | `id: String`, `title: String`, `description: String [""]`, `projectId: String?`, `categoryId: String?`, `eventId: String?`, `goalId: String?`, `priority: TaskPriority [NORMAL]`, `status: TaskStatus [NOT_STARTED]`, `dueAt: Long?`, `hasDueTime: Boolean [false]`, `notes: String [""]`, `completedAt: Long?`, `recurrenceId: String?`, `occurrenceDateIso: String?`, `createdAt: Long`, `updatedAt: Long` | FKs `projectId → projects`, `categoryId → categories`, `eventId → events`, `goalId → personal_goals`, `recurrenceId → recurrences` (todas `SET_NULL`). Ídx: os 5 FKs + `status`, `dueAt`, `UNIQUE(recurrenceId, occurrenceDateIso)` |
| `subtasks` (`SubtaskEntity`) | `id: String`, `taskId: String`, `title: String`, `isCompleted: Boolean [false]`, `sortOrder: Int [0]`, `createdAt: Long`, `updatedAt: Long` | FK `taskId → tasks(id)` (`CASCADE`). Ídx: `taskId` |
| `appointments` (`AppointmentEntity`) | `id: String`, `title: String`, `description: String [""]`, `categoryId: String?`, `eventId: String?`, `startAt: Long`, `endAt: Long`, `isAllDay: Boolean [false]`, `location: String [""]`, `link: String [""]`, `createdAt: Long`, `updatedAt: Long` | FKs `categoryId → categories`, `eventId → events` (`SET_NULL`). Ídx: `categoryId`, `eventId`, `startAt`, `endAt` |
| `recurrences` (`TaskRecurrenceEntity`) | `id: String`, `title: String`, `description: String [""]`, `projectId: String?`, `categoryId: String?`, `eventId: String?`, `goalId: String?`, `priority: TaskPriority [NORMAL]`, `notes: String [""]`, `hasDueTime: Boolean [false]`, `dueTimeMinutesOfDay: Int?`, `reminderMinutesBefore: Int?`, `frequency: TaskRecurrenceFrequency [WEEKLY]`, `customIntervalDays: Int [1]`, `weekdaysMask: Int [0]`, `startDateIso: String`, `endDateIso: String?`, `maxOccurrences: Int?`, `isActive: Boolean [true]`, `createdAt: Long`, `updatedAt: Long` | FKs `projectId`, `categoryId`, `eventId`, `goalId` (`SET_NULL`). Ídx: os 4 FKs + `isActive` |
| `recurrence_exceptions` (`RecurrenceExceptionEntity`) | `id: String`, `recurrenceId: String`, `occurrenceDateIso: String`, `exceptionType: RecurrenceExceptionType [SKIP]`, `overrideTitle: String?`, `overrideDueAt: Long?`, `taskId: String?`, `createdAt: Long`, `updatedAt: Long` | FK `recurrenceId → recurrences(id)` (`CASCADE`). Ídx: `recurrenceId`, `UNIQUE(recurrenceId, occurrenceDateIso)` |

### 2.3 Financeiro

| Tabela (`@Entity`) | Colunas Completas | FKs e Índices |
| --- | --- | --- |
| `credit_cards` (`CreditCardEntity`) | `id: String`, `name: String`, `institution: String [""]`, `totalLimitCents: Long [0L]`, `availableLimitCents: Long [0L]`, `closingDay: Int [10]`, `dueDay: Int [17]`, `colorHex: String ["#9A9A9A"]`, `notes: String [""]`, `createdAt: Long`, `updatedAt: Long` | PK: `id` |
| `credit_card_statements` (`CreditCardStatementEntity`) | `id: String`, `creditCardId: String`, `yearMonth: String`, `closingAt: Long`, `dueAt: Long`, `status: StatementStatus [OPEN]`, `paidAt: Long?`, `paidAmountCents: Long [0L]`, `createdAt: Long`, `updatedAt: Long` | FK `creditCardId → credit_cards(id)` (`CASCADE`). Ídx: `creditCardId`, `dueAt`, `UNIQUE(creditCardId, yearMonth)` |
| `installment_purchases` (`InstallmentPurchaseEntity`) | `id: String`, `description: String`, `totalAmountCents: Long`, `installmentsCount: Int`, `installmentAmountCents: Long`, `firstDueAt: Long`, `creditCardId: String?`, `categoryId: String?`, `eventId: String?`, `projectId: String?`, `notes: String [""]`, `createdAt: Long`, `updatedAt: Long` | FKs `creditCardId`, `categoryId`, `eventId`, `projectId` (`SET_NULL`). Ídx nos 4 FKs |
| `recurring_rules` (`RecurringRuleEntity`) | `id: String`, `title: String`, `type: TransactionType [EXPENSE]`, `amountCents: Long`, `categoryId: String?`, `paymentMethod: PaymentMethod [PIX]`, `creditCardId: String?`, `frequency: RecurrenceFrequency [MONTHLY]`, `customIntervalDays: Int [30]`, `startAt: Long`, `endAt: Long?`, `maxOccurrences: Int?`, `isActive: Boolean [true]`, `notes: String [""]`, `createdAt: Long`, `updatedAt: Long` | FKs `categoryId`, `creditCardId` (`SET_NULL`). Ídx nos 2 FKs |
| `transactions` (`TransactionEntity`) | `id: String`, `type: TransactionType`, `description: String`, `amountCents: Long`, `categoryId: String?`, `status: FinancialStatus [PENDING]`, `paymentMethod: PaymentMethod [PIX]`, `occurredAt: Long`, `dueAt: Long?`, `settledAt: Long?`, `projectId: String?`, `eventId: String?`, `taskId: String?`, `eventCostType: EventCostType?`, `isTicketObligation: Boolean [false]`, `creditCardId: String?`, `statementId: String?`, `installmentPurchaseId: String?`, `installmentNumber: Int?`, `installmentsTotal: Int?`, `recurringRuleId: String?`, `occurrenceIndex: Int?`, `notes: String [""]`, `createdAt: Long`, `updatedAt: Long` | FKs `categoryId`, `projectId`, `eventId`, `creditCardId`, `statementId`, `installmentPurchaseId`, `recurringRuleId`, `taskId` (todas `SET_NULL`). Ídx: todos os 8 FKs + `dueAt`, `occurredAt`, `UNIQUE(recurringRuleId, occurrenceIndex)` |
| `loans` (`LoanEntity`) | `id: String`, `creditor: String`, `originalAmountCents: Long`, `contractedAmountCents: Long`, `installmentsTotal: Int`, `installmentAmountCents: Long`, `paidInstallments: Int [0]`, `nextDueAt: Long?`, `remainingBalanceCents: Long`, `finalDateAt: Long?`, `notes: String [""]`, `createdAt: Long`, `updatedAt: Long` | PK: `id` |
| `budgets` (`BudgetEntity`) | `id: String`, `categoryId: String`, `yearMonth: String`, `limitCents: Long`, `createdAt: Long`, `updatedAt: Long` | FK `categoryId → categories(id)` (`CASCADE`). Ídx: `categoryId`, `UNIQUE(categoryId, yearMonth)` |
| `financial_goals` (`FinancialGoalEntity`) | `id: String`, `name: String`, `targetAmountCents: Long`, `currentAmountCents: Long [0L]`, `deadlineAt: Long?`, `categoryId: String?`, `notes: String [""]`, `createdAt: Long`, `updatedAt: Long` | FK `categoryId → categories(id)` (`SET_NULL`). Ídx: `categoryId` |

### 2.4 Rotina, Notificações, Personalização, Frases, Objetivos e Diagnóstico

| Tabela (`@Entity`) | Colunas Completas | FKs e Índices |
| --- | --- | --- |
| `reminders` (`ReminderEntity`) | `id: String`, `sourceType: ReminderSourceType`, `sourceId: String`, `category: ReminderCategory`, `title: String`, `message: String`, `triggerAt: Long`, `originalDueAt: Long?`, `minutesBefore: Int [0]`, `channelId: String`, `status: ReminderStatus [ACTIVE]`, `snoozedUntil: Long?`, `isExactScheduled: Boolean [false]`, `firedAt: Long?`, `createdAt: Long`, `updatedAt: Long` | Ídx: `UNIQUE(sourceType, sourceId, category)`, `status`, `triggerAt` |
| `routine_assistant_settings` (`RoutineAssistantSettingsEntity`) | `id: String ["nexus_routine_default"]`, `notificationsEnabled: Boolean [true]`, `scheduleChannelEnabled: Boolean [true]`, `financeChannelEnabled: Boolean [true]`, `eventsChannelEnabled: Boolean [true]`, `routineChannelEnabled: Boolean [true]`, `morningBriefingEnabled: Boolean [true]`, `morningBriefingHour: Int [8]`, `morningBriefingMinute: Int [0]`, `nightReviewEnabled: Boolean [true]`, `nightReviewHour: Int [21]`, `nightReviewMinute: Int [0]`, `quietHoursEnabled: Boolean [false]`, `quietHoursStartHour: Int [22]`, `quietHoursStartMinute: Int [0]`, `quietHoursEndHour: Int [7]`, `quietHoursEndMinute: Int [0]`, `defaultTaskReminderMinutes: Int [30]`, `defaultAppointmentReminderMinutes: Int [30]`, `defaultEventReminderMinutes: Int [60]`, `defaultFinanceReminderMinutes: Int [0]`, `createdAt: Long`, `updatedAt: Long` | PK: `id` (singleton) |
| `briefing_history` (`BriefingHistoryEntity`) | `id: String`, `briefingType: BriefingType`, `dateIso: String`, `summaryText: String`, `tasksCount: Int [0]`, `appointmentsCount: Int [0]`, `billsDueCount: Int [0]`, `overdueCount: Int [0]`, `reviewedAt: Long?`, `createdAt: Long`, `updatedAt: Long` | Ídx: `UNIQUE(briefingType, dateIso)`, `dateIso` |
| `personalization_settings` (`PersonalizationSettingsEntity`) | `id: String ["nexus_personalization_default"]`, `greetingEnabled: Boolean [true]`, `greetingTemplate: String ["AUTO"]`, `dateFormatPattern: NexusDateFormat [WEEKDAY_DD_MM_YYYY]`, `showDailyQuoteOnDashboard: Boolean [true]`, `compactDashboardMode: Boolean [false]`, `showMetricSummaryOnDashboard: Boolean [true]`, `dashboardBlocksJson: String [""]`, `lastQuoteDateIso: String?`, `lastQuoteId: String?`, `previousQuoteId: String?`, `createdAt: Long`, `updatedAt: Long` | PK: `id` (singleton) |
| `quotes` (`QuoteEntity`) | `id: String`, `text: String`, `author: String [""]`, `isActive: Boolean [true]`, `lastShownDateIso: String?`, `shownCount: Int [0]`, `createdAt: Long`, `updatedAt: Long` | Ídx: `isActive` |
| `personal_goals` (`PersonalGoalEntity`) | `id: String`, `title: String`, `description: String [""]`, `categoryId: String?`, `targetDateAt: Long?`, `status: GoalStatus [ACTIVE]`, `progressMode: GoalProgressMode [AUTO]`, `manualProgressPercent: Int [0]`, `completedAt: Long?`, `notes: String [""]`, `createdAt: Long`, `updatedAt: Long` | FK `categoryId → categories(id)` (`SET_NULL`). Ídx: `categoryId`, `status`, `targetDateAt` |
| `goal_milestones` (`GoalMilestoneEntity`) | `id: String`, `goalId: String`, `title: String`, `description: String [""]`, `dueAt: Long?`, `isCompleted: Boolean [false]`, `completedAt: Long?`, `sortOrder: Int [0]`, `createdAt: Long`, `updatedAt: Long` | FK `goalId → personal_goals(id)` (`CASCADE`). Ídx: `goalId` |
| `goal_progress_logs` (`GoalProgressLogEntity`) | `id: String`, `goalId: String`, `progressPercent: Int`, `note: String [""]`, `recordedAt: Long`, `createdAt: Long`, `updatedAt: Long` | FK `goalId → personal_goals(id)` (`CASCADE`). Ídx: `goalId`, `recordedAt` |
| `audit_history` (`AuditHistoryEntity`) | `id: String`, `entityType: String`, `entityId: String`, `action: String`, `summary: String`, `occurredAt: Long`, `createdAt: Long`, `updatedAt: Long` | Ídx: `occurredAt` |
| `error_logs` (`ErrorLogEntity`) | `id: String`, `source: String`, `message: String`, `stackTrace: String`, `occurredAt: Long`, `createdAt: Long`, `updatedAt: Long` | Ídx: `occurredAt` |

---

## 3. Catálogo de Enums Persistidos (`NexusTypeConverters` e Mercado)

### `NexusTypeConverters` (21 enums persistidos como `TEXT` via `.name` e recuperados com `fromStringOrDefault`)
- `TaskPriority` (`LOW, NORMAL, HIGH, URGENT`)
- `TaskStatus` (`NOT_STARTED, IN_PROGRESS, WAITING, BLOCKED, COMPLETED, CANCELLED`)
- `ProjectStatus` (`PLANNED, ACTIVE, ON_HOLD, COMPLETED, ARCHIVED`)
- `CategoryScope` (`GENERAL, APPOINTMENT, TASK, FINANCE, EVENT`)
- `EventLifecycleStatus` (`PLANNED, CONFIRMED, ATTENDED, CANCELLED`)
- `TicketStatus` (`FREE, NOT_BOUGHT, PENDING, PARTIALLY_PAID, BOUGHT, PAID, CANCELLED`)
- `PaymentMethod` (`PIX, DEBIT, CREDIT_CARD, CASH, BANK_TRANSFER, BOLETO, OTHER`)
- `TransactionType` (`INCOME, EXPENSE`)
- `FinancialStatus` (`PLANNED, PENDING, SETTLED, OVERDUE, CANCELLED`)
- `EventCostType` (`TICKET, TRANSPORT, FOOD, LODGING, PARKING, SHOPPING, OTHER` — anulável)
- `StatementStatus` (`OPEN, CLOSED, PENDING, PAID, OVERDUE`)
- `RecurrenceFrequency` (`DAILY, WEEKLY, BIWEEKLY, MONTHLY, BIMONTHLY, QUARTERLY, SEMIANNUAL, ANNUAL, CUSTOM_DAYS`)
- `ReminderSourceType` (`TASK, APPOINTMENT, EVENT, TRANSACTION, STATEMENT, ROUTINE_MORNING, ROUTINE_NIGHT`)
- `ReminderCategory` (`SCHEDULE, FINANCIAL, ROUTINE`)
- `ReminderStatus` (`ACTIVE, FIRED, DISMISSED, CANCELLED`)
- `BriefingType` (`MORNING, NIGHT_REVIEW`)
- `NexusDateFormat` (`WEEKDAY_DD_MM_YYYY, DD_MM_YYYY, DD_MMM_YYYY, DD_MM_WEEKDAY`)
- `GoalStatus` (`ACTIVE, PAUSED, COMPLETED, CANCELLED`)
- `GoalProgressMode` (`AUTO, MANUAL`)
- `TaskRecurrenceFrequency` (`DAILY, SPECIFIC_WEEKDAYS, WEEKLY, BIWEEKLY, MONTHLY, CUSTOM_DAYS`)
- `RecurrenceExceptionType` (`SKIP, OVERRIDE`)

*(Enums usados apenas em JSON/Domínio: `DashboardBlockType` [9 blocos], `QuoteLibraryStatus`, `AssetCategory`, `QuoteModality`, `QuoteTrend`, `MarketHistoryRange`, `MarketHistoryGranularity`, `MarketAlertCondition`, `MarketAlertStatus`.)*

---

## 4. Persistência Não-Room (Mercado e Widgets)

| Componente | Mecanismo e Localização | Estrutura / Chaves | Entra no Backup v7? |
| --- | --- | --- | --- |
| `SharedPreferencesMarketQuoteStore` | `SharedPreferences` `"nexus_market_quotes_cache"` (`MODE_PRIVATE`) | Chave `"quote_record_<assetCode>"` → `PersistedMarketQuoteRecord` (`lastValidQuote`, `lastAttemptAtEpochMs`, `lastAttemptError`) | **Não** (cache reconstruível) |
| `FileMarketHistoryStore` | Arquivos em `filesDir/market_history/<SAFE_CODE>_<RANGE>.json` (escrita atômica `.tmp` + `renameTo`) | `PersistedMarketHistoryRecord` (`series: MarketHistorySeries?`, `lastAttemptAtEpochMs`, `lastAttemptError`) | **Não** (cache reconstruível) |
| `SharedPreferencesMarketWatchlistStore` | `SharedPreferences` `"nexus_market_watchlist"` (`MODE_PRIVATE`) | Chave `"favorite_asset_codes"` → `List<String>` deduplicada/validada pelo `MarketAssetCatalog` | **Sim** (`marketConfig.favoriteAssetCodes`) |
| `FileMarketAlertStore` | Arquivo JSON único `filesDir/market_alerts/market_alerts.json` (escrita atômica `.tmp` + `renameTo`) | `List<MarketPriceAlert>` (`id`, `assetCode`, `condition`, `targetPrice`, `isActive`, `status`, timestamps, disparo, `awaitingConditionReset`, `lastEvaluatedQuoteFetchedAtEpochMs`) | **Sim** (`marketConfig.priceAlerts`) |
| `NexusWidgetConfigStore` | `SharedPreferences` `"nexus_widget_instance_prefs"` (`MODE_PRIVATE`) | Chaves `"w_<appWidgetId>_*"` (`NexusWidgetInstanceConfig`) + `"confirm_task_<id>"` / `"confirm_ts_<id>"` (TTL 15s) | **Não** (config local por launcher) |

---

## 5. Histórico de Migrações Room (`NexusMigrations.kt`)

`fallbackToDestructiveMigration()` é **proibido**. Todas as `5` migrações adicionadas preservam dados anteriores:

| Migração | Alterações Aplicadas |
| --- | --- |
| `MIGRATION_1_2` | `settings.declaredBalanceCents`; cria `credit_cards`, `events`, `credit_card_statements`, `installment_purchases`, `recurring_rules`, `transactions`, `loans`, `budgets`, `financial_goals`, `audit_history`; `tasks.eventId`; seed das 12 categorias financeiras padrão |
| `MIGRATION_2_3` | Cria `reminders`, `routine_assistant_settings`, `briefing_history`; seed padrão de rotina |
| `MIGRATION_3_4` | Cria `personalization_settings`, `quotes`, `personal_goals`, `goal_milestones`, `goal_progress_logs`; `projects.goalId`, `tasks.goalId`; seed de personalização |
| `MIGRATION_4_5` | `appointments.eventId` (`SET_NULL`), `transactions.taskId` (`SET_NULL`) + índices |
| `MIGRATION_5_6` | Cria `recurrences` e `recurrence_exceptions`; adiciona `tasks.recurrenceId` (`SET_NULL`) e `tasks.occurrenceDateIso` + índice `UNIQUE(recurrenceId, occurrenceDateIso)` |

---

## 6. Especificação de Backup e Exportação (`CURRENT_FORMAT_VERSION = 7`)

- **Formato JSON (`NexusBackupPayload`):**
  - Metadados: `formatVersion` (`7`), `appVersion`, `exportedAt` (ISO UTC), `databaseVersion` (`6`).
  - `27` listas tipadas (`*BackupDto`) espelhando as 27 tabelas Room.
  - `marketConfig: MarketBackupConfigDto?` (`configVersion = 1`, `favoriteAssetCodes: List<String>?`, `priceAlerts: List<MarketPriceAlertBackupDto>?`).
- **Retrocompatibilidade (`v1..v7`):** parser `Json { ignoreUnknownKeys = true; coerceInputValues = true; isLenient = true }` com defaults em todos os DTOs. Se `marketConfig == null` (ou subcampos `== null`), preserva os favoritos/alertas locais existentes; se lista presente e vazia (`[]`), substitui por vazio.
- **Validação & Ordem de Restauração (`BackupManager.restoreAtomically`):**
  1. Sanitiza FKs órfãs contra pais existentes e mapeia enums inválidos com fallback seguro.
  2. Valida `marketConfig`: filtra códigos inexistentes no `MarketAssetCatalog`, `targetPrice > 0 && isFinite()`, compatibilidade estrita de `currencyOrUnit` (`pts` para índices, código ISO para demais), normaliza estados inconsistentes (`WAITING` + `isActive=false` → `PAUSED`; `TRIGGERED` → `isActive=false`, `awaitingConditionReset=false`) e remove duplicidades.
  3. Dentro de `database.withTransaction`: limpa tabelas filhas → pais, insere pais → filhos na ordem topológica e executa `applyMarketConfigWithRollback` ao final; se o commit Room ou a gravação do Mercado falhar, aciona `rollbackMarketConfig` restaurando os snapshots prévios de favoritos e alertas.
  4. Pós-commit: não avalia alertas nem dispara notificações de mercado na restauração; ressincroniza apenas lembretes Room e atualiza widgets Glance.
- **CSV Financeiro (`exportFinancialCsv`):** UTF-8 via SAF com **15 colunas** separadas por vírgula (valores `%.2f` em `Locale.US`, datas `dd/MM/yyyy`).
- **Frases (`QuoteLibraryScreen`):** exportação e importação SAF em `.json` ou `.txt` (separadores ` - ` ou ` | `, ignorando linhas iniciadas por `#`).
