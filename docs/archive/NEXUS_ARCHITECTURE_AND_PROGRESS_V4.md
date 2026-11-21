> **[DOCUMENTO HISTÓRICO ARQUIVADO — FASE `5.0.0` / ROOM `v4`]**  
> Preserva o retrato técnico anterior da fase `v4`. **Substituído pela documentação oficial vigente** em [`../03_ARQUITETURA_ENGENHARIA.md`](../03_ARQUITETURA_ENGENHARIA.md), [`../04_MODELO_DADOS.md`](../04_MODELO_DADOS.md) e [`../07_ESTADO_ATUAL.md`](../07_ESTADO_ATUAL.md).

# NEXUS — Mapa Técnico e Arquitetura Atual

> Documento vivo atualizado exclusivamente com base nos arquivos-fonte atuais do repositório. Cada item indica **CONFIRMADO NO CÓDIGO** ou **NÃO VERIFICADO / SUPOSIÇÃO**. Nenhum código, recurso ou configuração foi alterado nesta revisão.

---

## 1. Visão geral

- **O que o app faz hoje (CONFIRMADO NO CÓDIGO):** Centro de comando pessoal Android offline-first que integra agenda unificada, tarefas (lista e Kanban), projetos, objetivos pessoais com marcos, eventos com controle de ingressos e custos, módulo financeiro completo (contas, cartões, faturas, parcelamentos, recorrências, empréstimos, orçamentos e metas), assistente de rotina (briefings e revisão noturna), notificações locais exatas, biblioteca de frases, dashboard personalizável, backup/restauração JSON/CSV e 4 widgets nativos na tela inicial.
- **Identificação e versão (CONFIRMADO NO CÓDIGO):** `applicationId = "com.nexus.app"`, `versionCode = 5`, `versionName = "5.0.0"`, `minSdk = 26`, `targetSdk = 35`, `compileSdk = 35`, `JavaVersion.VERSION_17`.

| Ferramenta / Biblioteca | Versão / Valor | Status |
| --- | --- | --- |
| Gradle Wrapper (`gradle-wrapper.properties`) | `8.10.2` (`bin`) | CONFIRMADO NO CÓDIGO |
| Android Gradle Plugin (`libs.versions.toml`) | `8.7.3` | CONFIRMADO NO CÓDIGO |
| Kotlin / KSP / Serialization plugin | `2.1.0` / `2.1.0-1.0.29` | CONFIRMADO NO CÓDIGO |
| Jetpack Compose BOM + Material3 / Icons | `2024.12.01` | CONFIRMADO NO CÓDIGO |
| `androidx.core:core-ktx` / `activity-compose` | `1.15.0` / `1.9.3` | CONFIRMADO NO CÓDIGO |
| `androidx.lifecycle` (`runtime-ktx`, `runtime-compose`, `viewmodel-compose`) | `2.8.7` | CONFIRMADO NO CÓDIGO |
| `androidx.navigation:navigation-compose` | `2.8.5` (declarada, **não usada**) | CONFIRMADO NO CÓDIGO |
| Room (`room-runtime`, `room-ktx`, `room-compiler`) | `2.6.1` | CONFIRMADO NO CÓDIGO |
| `androidx.work:work-runtime-ktx` | `2.10.0` | CONFIRMADO NO CÓDIGO |
| Jetpack Glance (`glance-appwidget`, `glance-material3`) | `1.1.1` | CONFIRMADO NO CÓDIGO |
| `kotlinx-serialization-json` / `kotlinx-coroutines-android` | `1.7.3` / `1.9.0` | CONFIRMADO NO CÓDIGO |
| JUnit 4 (`testImplementation`) | `4.13.2` | CONFIRMADO NO CÓDIGO |

- **Raiz mista do repositório (CONFIRMADO NO CÓDIGO):** O módulo Android fica em `:app` (`settings.gradle.kts`, `rootProject.name = "NEXUS"`), coexistindo na raiz com arquivos Web/Node (`package.json`, `src/`, `index.html`, `vite.config.ts`).

---

## 2. Estrutura

Projeto Gradle de módulo único (`:app`), sem injeção via Hilt/Koin (DI manual via `AppContainer` em `NexusApplication`). Todos os pontos abaixo: **CONFIRMADO NO CÓDIGO**.

- `com.nexus.app` (raiz): `NexusApplication` (inicializa `AppContainer`, `GlobalErrorHandler`, canais de notificação e agendamento periódico) e `MainActivity` (única Activity principal + `NexusWidgetConfigActivity` para widgets; shell Compose e tratamento de deep-links).
- `com.nexus.app.core`: `DateTimeUtils` (fuso `America/Sao_Paulo`, formatação `pt-BR`/BRL e utilitários de data) e `GlobalErrorHandler` (`UncaughtExceptionHandler` persistindo falhas em `error_logs`).
- `com.nexus.app.domain.model`: `NexusEnums` (19 enums de domínio com `fromStringOrDefault`) e `DomainModels` (DTOs agregados para UI, como `TaskWithDetails`, `AgendaItem`, `FinanceSummary`, `EventWithDetails`, `PersonalGoalWithDetails`, `BackupResult`).
- `com.nexus.app.data.db`: `NexusDatabase` (Room v4), `NexusMigrations` (`1->2`, `2->3`, `3->4`), `NexusTypeConverters` (conversores dos 19 enums), `entity/NexusEntities.kt` (25 `@Entity`) e `dao/NexusDaos.kt` (25 `@Dao`).
- `com.nexus.app.data.backup`: `BackupModels` (DTOs `@Serializable` JSON v4) e `BackupManager` (exportação/importação JSON v1–v4, exportação CSV financeiro e importação/exportação de frases).
- `com.nexus.app.data.repository`: `NexusRepository` (camada única de regra de negócio, fluxos reativos `Flow`, geração idempotente de recorrências/parcelas/faturas, cálculo financeiro, lembretes, briefings e objetivos).
- `com.nexus.app.notifications`: `NexusNotificationChannels`, `NexusNotificationScheduler` (`AlarmManager`), `NexusBroadcastReceivers` (3 receivers: alarme, ações e boot) e `NexusReminderSyncWorker` (`WorkManager`).
- `com.nexus.app.widgets`: 4 `GlanceAppWidget` + receivers (`MyDay`, `MyTasks`, `Finance`, `NexusSummary`), `NexusWidgetConfigActivity`, `NexusWidgetConfigStore`, `NexusWidgetActions`, `NexusWidgetModels`, `NexusWidgetNavigation`, `NexusWidgetTheme` e `NexusWidgetUpdater`.
- `com.nexus.app.ui`: `NexusViewModel`, `theme/NexusTheme.kt`, `components/NexusComponents.kt` e `screens/` (11 arquivos de telas e modais editores).

---

## 3. Banco de dados

- **Motor e versão (CONFIRMADO NO CÓDIGO):** Room SQLite (`nexus_local.db`), `DATABASE_VERSION = 4`, `exportSchema = true` e `ksp { arg("room.schemaLocation", "$projectDir/schemas") }`, porém a pasta `app/schemas/` **não existe no disco** atualmente.
- **Migrações (CONFIRMADO NO CÓDIGO):** Não usa `fallbackToDestructiveMigration()`. Possui `MIGRATION_1_2` (módulos eventos/financeiro, `tasks.eventId`, `settings.declaredBalanceCents` e 12 categorias financeiras padrão), `MIGRATION_2_3` (`reminders`, `routine_assistant_settings`, `briefing_history` + seed de rotina) e `MIGRATION_3_4` (`personalization_settings`, `quotes`, `personal_goals`, `goal_milestones`, `goal_progress_logs`, `projects.goalId`, `tasks.goalId` + seed de personalização).
- **Datas e fuso horário (CONFIRMADO NO CÓDIGO):** Timestamps são armazenados como `Long` (epoch ms UTC), além de strings `yearMonth` (`YYYY-MM`) e `dateIso` (`YYYY-MM-DD`). Conversões de dia/hora no app usam `ZoneId.of("America/Sao_Paulo")`, exceto `NexusDatePickerDialog`, que lê e escreve datas de calendário do Material3 em `ZoneOffset.UTC`.

### Tabelas (25 entidades — CONFIRMADO NO CÓDIGO)

Todas possuem PK `id TEXT` e timestamps `createdAt INTEGER, updatedAt INTEGER` (omitidos abaixo para concisão):

| Tabela | Colunas principais | FKs e Índices |
| --- | --- | --- |
| `settings` | `userName, darkTheme, declaredBalanceCents` | — |
| `categories` | `name, colorHex, scope, isDefault` | — |
| `projects` | `name, description, deadlineAt?, status, priority, goalId?` | FK `goalId->personal_goals(SET_NULL)`; idx `goalId` |
| `events` | `name, description, categoryId?, startAt, endAt, isAllDay, officialLink, ticketLink, locationName, address, city, state, notes, eventStatus, ticketStatus, ticketPriceCents, ticketQuantity, ticketFeesCents, ticketTotalCents, ticketPurchaseDate?, ticketDueDate?, paymentMethod, creditCardId?, installmentsCount` | FKs `categoryId->categories`, `creditCardId->credit_cards` (`SET_NULL`); idx `categoryId, creditCardId, startAt` |
| `tasks` | `title, description, projectId?, categoryId?, eventId?, goalId?, priority, status, dueAt?, hasDueTime, notes, completedAt?` | FKs `projectId, categoryId, eventId, goalId` (`SET_NULL`); idx nos 4 + `status, dueAt` |
| `subtasks` | `taskId, title, isCompleted, sortOrder` | FK `taskId->tasks(CASCADE)`; idx `taskId` |
| `appointments` | `title, description, categoryId?, startAt, endAt, isAllDay, location, link` | FK `categoryId->categories(SET_NULL)`; idx `categoryId, startAt, endAt` |
| `credit_cards` | `name, institution, totalLimitCents, availableLimitCents, closingDay, dueDay, colorHex, notes` | — |
| `credit_card_statements` | `creditCardId, yearMonth, closingAt, dueAt, status, paidAt?, paidAmountCents` | FK `creditCardId->credit_cards(CASCADE)`; idx `creditCardId, dueAt`, `UNIQUE(creditCardId, yearMonth)` |
| `installment_purchases` | `description, totalAmountCents, installmentsCount, installmentAmountCents, firstDueAt, creditCardId?, categoryId?, eventId?, projectId?, notes` | FKs `creditCardId, categoryId, eventId, projectId` (`SET_NULL`); idx nos 4 |
| `recurring_rules` | `title, type, amountCents, categoryId?, paymentMethod, creditCardId?, frequency, customIntervalDays, startAt, endAt?, maxOccurrences?, isActive, notes` | FKs `categoryId, creditCardId` (`SET_NULL`); idx nos 2 |
| `transactions` | `type, description, amountCents, categoryId?, status, paymentMethod, occurredAt, dueAt?, settledAt?, projectId?, eventId?, eventCostType?, isTicketObligation, creditCardId?, statementId?, installmentPurchaseId?, installmentNumber?, installmentsTotal?, recurringRuleId?, occurrenceIndex?, notes` | FKs `categoryId, projectId, eventId, creditCardId, statementId, installmentPurchaseId, recurringRuleId` (`SET_NULL`); idx nos 7 + `dueAt, occurredAt`, `UNIQUE(recurringRuleId, occurrenceIndex)` |
| `loans` | `creditor, originalAmountCents, contractedAmountCents, installmentsTotal, installmentAmountCents, paidInstallments, nextDueAt?, remainingBalanceCents, finalDateAt?, notes` | — |
| `budgets` | `categoryId, yearMonth, limitCents` | FK `categoryId->categories(CASCADE)`; idx `categoryId`, `UNIQUE(categoryId, yearMonth)` |
| `financial_goals` | `name, targetAmountCents, currentAmountCents, deadlineAt?, categoryId?, notes` | FK `categoryId->categories(SET_NULL)`; idx `categoryId` |
| `audit_history` | `entityType, entityId, action, summary, occurredAt` | idx `occurredAt` |
| `error_logs` | `source, message, stackTrace, occurredAt` | idx `occurredAt` |
| `reminders` | `sourceType, sourceId, category, title, message, triggerAt, originalDueAt?, minutesBefore, channelId, status, snoozedUntil?, isExactScheduled, firedAt?` | idx `UNIQUE(sourceType, sourceId, category)`, `status, triggerAt` |
| `routine_assistant_settings` | `notificationsEnabled`, flags dos 4 canais, horários/flags `morningBriefing`, `nightReview`, `quietHours`, e 4 `default*ReminderMinutes` | — |
| `briefing_history` | `briefingType, dateIso, summaryText, tasksCount, appointmentsCount, billsDueCount, overdueCount, reviewedAt?` | idx `UNIQUE(briefingType, dateIso)`, `dateIso` |
| `personalization_settings` | `greetingEnabled, greetingTemplate, dateFormatPattern, showDailyQuoteOnDashboard, compactDashboardMode, showMetricSummaryOnDashboard, dashboardBlocksJson, lastQuoteDateIso?, lastQuoteId?, previousQuoteId?` | — |
| `quotes` | `text, author, isActive, lastShownDateIso?, shownCount` | idx `isActive` |
| `personal_goals` | `title, description, categoryId?, targetDateAt?, status, progressMode, manualProgressPercent, completedAt?, notes` | FK `categoryId->categories(SET_NULL)`; idx `categoryId, status, targetDateAt` |
| `goal_milestones` | `goalId, title, description, dueAt?, isCompleted, completedAt?, sortOrder` | FK `goalId->personal_goals(CASCADE)`; idx `goalId` |
| `goal_progress_logs` | `goalId, progressPercent, note, recordedAt` | FK `goalId->personal_goals(CASCADE)`; idx `goalId, recordedAt` |

---

## 4. Relações entre registros

Todos os vínculos abaixo: **CONFIRMADO NO CÓDIGO**.

- **Tarefa (`tasks`)**: referencia `projects`, `categories`, `events` e `personal_goals` (`SET_NULL`); possui `subtasks` filhas (`CASCADE`).
- **Projeto (`projects`)**: referencia `personal_goals` (`SET_NULL`); agrega tarefas (`tasks.projectId`) e custos (`transactions.projectId` e `installment_purchases.projectId`).
- **Compromisso (`appointments`)**: referencia apenas `categories` (`SET_NULL`); não possui FK direta para tarefa, projeto ou evento.
- **Evento (`events`)**: referencia `categories` e `credit_cards`; é referenciado por `tasks.eventId`, `transactions.eventId` (ingresso `isTicketObligation=true` e custos extras por `eventCostType`) e `installment_purchases.eventId`.
- **Financeiro (`transactions`)**: referencia `categories`, `projects`, `events`, `credit_cards`, `credit_card_statements`, `installment_purchases` e `recurring_rules`.
- **Cartões e Faturas**: `credit_card_statements.creditCardId` aponta para `credit_cards` (`CASCADE`), com unicidade por `(creditCardId, yearMonth)`.
- **Objetivos pessoais (`personal_goals`)**: ligados inversamente por `goal_milestones`, `goal_progress_logs`, `projects.goalId` e `tasks.goalId` (sem duplicar progresso entre tabela de tarefas e tabela de objetivos).

---

## 5. Regras de negócio

Todas as regras abaixo: **CONFIRMADO NO CÓDIGO** (`NexusRepository` e `NexusNotificationScheduler`).

- **Atraso efetivo calculado dinamicamente na leitura (não grava `OVERDUE` no banco):** `effectiveTransactionStatus` e `effectiveStatementStatus` promovem itens não liquidados (`!= SETTLED/CANCELLED` ou `!= PAID`) com `dueAt < startOfToday` para `OVERDUE`; `isTaskOverdue` considera tarefas não terminais com `dueAt < nowEpoch` (se `hasDueTime`) ou dia anterior a hoje; `isGoalOverdue` considera objetivos `ACTIVE` com `targetDateAt < startOfToday`.
- **Recorrências idempotentes (`generateOccurrencesForRuleInternal`):** gera ocorrências para até 6 meses à frente (teto `min(maxOccurrences ?: 60, 60)`), protegidas por checagem prévia, `UNIQUE(recurringRuleId, occurrenceIndex)` e `insertIgnore`. Roda na inicialização e no worker periódico; excluir regra remove apenas ocorrências não liquidadas e desvincula as já pagas.
- **Parcelamentos (`createInstallmentPurchase`):** grava `InstallmentPurchaseEntity` e `N` transações (resto de divisão inteira vai na 1ª parcela), associando à fatura correspondente quando `creditCardId != null`. Excluir remove apenas parcelas não liquidadas.
- **Faturas de cartão vs pagamento:** compras `CREDIT_CARD` são atribuídas a uma fatura pelo `closingDay` (dia da compra `> closingDay` vai para o mês seguinte). `payCreditCardStatement` liquida (`SETTLED`) todas as transações da fatura, sincroniza ingresso de evento se houver, marca a fatura como `PAID` com `paidAmountCents` e cancela lembretes — **sem criar segunda despesa**.
- **Eventos e ingressos:** `TicketStatus.FREE` não gera transação e apaga obrigações não liquidadas anteriores ao editar; ingresso pago no cartão com `installmentsCount > 1` cria parcelamento + `N` parcelas, caso contrário gera 1 transação `isTicketObligation = true`. Cancelar evento (`EventStatus.CANCELLED`) cancela obrigações não liquidadas; pagar parcelas atualiza `ticketStatus` (`PARTIALLY_PAID` -> `PAID`).
- **Lembretes:** `computeReminderTriggerEpoch` calcula `dueEpoch - minutesBefore * 60_000` (usando `09:00` quando não há hora específica); `adjustForQuietHours` adia disparos dentro do horário de silêncio em `America/Sao_Paulo`. Concluir, pagar ou cancelar o registro origem cancela/atualiza seus lembretes em `reminders`.

---

## 6. Agenda

Todos os pontos abaixo: **CONFIRMADO NO CÓDIGO** (`NexusRepository.observeUnifiedAgenda`, `NexusViewModel`, `AgendaScreen`).

- **Fontes lidas em tempo real (6 fontes, sem duplicação):**
  1. `appointments`: usa `startAt`; detecta conflitos de horário entre compromissos não `isAllDay` (`computeAppointmentConflicts`).
  2. `tasks` com prazo (`WHERE dueAt IS NOT NULL`): usa o próprio campo **`dueAt`** (não existe coluna separada de horário planejado; `hasDueTime` apenas define se exibe horário ou `23:59` padrão).
  3. `events`: usa `startAt`.
  4. `transactions` (`!= CANCELLED` com `dueAt != null`): usa `dueAt`.
  5. `credit_card_statements` (`!= PAID` e total `> 0`): usa `dueAt`.
  6. `projects` (`!= COMPLETED/ARCHIVED` com `deadlineAt != null`): usa `deadlineAt`.
- **Modos de visualização (`AgendaViewMode`):** `DAY` filtra `it.date == selectedDate`; `WEEK` filtra semana segunda–domingo de `selectedDate`; `MONTH` renderiza grade do mês com marcação de dias ocupados, mas **a lista abaixo exibe apenas `it.date == selectedDate`**; `LIST` lista todos os itens filtrados por busca e categoria. Todo agrupamento usa `America/Sao_Paulo`.

---

## 7. Tarefas e Kanban

Todos os pontos abaixo: **CONFIRMADO NO CÓDIGO** (`NexusEntities`, `TasksAndProjectsScreen`).

- **Modelo (`TaskEntity`):** `id, title, description, projectId?, categoryId?, eventId?, goalId?, priority (LOW, NORMAL, HIGH, URGENT), status (NOT_STARTED, IN_PROGRESS, WAITING, BLOCKED, COMPLETED, CANCELLED), dueAt?, hasDueTime, notes, completedAt?, createdAt, updatedAt`. Subtarefas em `subtasks` (`FK taskId CASCADE`).
- **Modos na tela (`TasksTabMode`):** `LIST`, `KANBAN`, `OVERDUE`, `COMPLETED` e `PROJECTS`.
- **Kanban:** lê exatamente a mesma lista reativa `filteredTasks` da tabela `tasks` (sem tabela separada). Exibe colunas horizontais (`LazyRow`) para todos os 6 valores de `TaskStatus`, com botão `"Mover status"` via dropdown em cada cartão.

---

## 8. Financeiro

Todos os pontos abaixo: **CONFIRMADO NO CÓDIGO** (`FinanceScreen`, `FinanceEditors`, `NexusRepository`).

- **Abas e editores:** 5 abas (`OVERVIEW`, `TRANSACTIONS`, `CARDS`, `RECURRING` [parcelamentos + recorrentes], `PLANNING` [orçamentos, metas financeiras, empréstimos e histórico de auditoria]) e 9 diálogos/sheets de edição (`TransactionEditorSheet`, `CreditCardEditorSheet`, `InstallmentPurchaseEditorSheet`, `RecurringRuleEditorSheet`, `LoanEditorSheet`, `LoanPaymentDialog`, `DeclaredBalanceDialog`, `BudgetEditorDialog`, `FinancialGoalEditorDialog`).
- **Gráficos existentes (Canvas Compose nativo, sem biblioteca externa):** (1) **Fluxo mensal (Realizado)** em barras agrupadas (Receitas vs Despesas nos últimos 6 meses de itens `SETTLED`), exibido no Financeiro e na Home; (2) **Gasto por categoria** no mês atual em barras horizontais proporcionais por cor da categoria (`CategorySpendCard` / Home).
- **Regras de cálculo exatas:**
  - `declaredBalanceCents`: valor manual salvo em `settings`; nunca é recalculado automaticamente pelas transações.
  - **Totalizadores gerais (`observeFinanceSummary`)**: `receivedIncomeCents`, `expectedIncomeCents`, `paidExpenseCents`, `pendingExpenseCents` e `overdueExpenseCents` somam **todas as transações não canceladas de todas as datas** (sem filtro de mês atual), classificadas por `effectiveTransactionStatus`. Apenas `categorySpend` filtra o mês corrente (`dueAt ?: occurredAt`) e `monthlyCashflow` agrupa os últimos 6 meses (`settledAt ?: dueAt ?: occurredAt`).
  - **Limite do cartão (`CardWithStatements`):** `currentOpenTotalCents` soma despesas não canceladas de faturas `!= PAID`; `computedRemainingLimitCents = (card.totalLimitCents - currentOpenTotalCents).coerceAtLeast(0L)` (**ignora a coluna manual `availableLimitCents` salva no banco**).

---

## 9. Eventos

Todos os pontos abaixo: **CONFIRMADO NO CÓDIGO** (`NexusEntities`, `EventsScreen`, `NexusRepository`).

- **Campos principais:** nome, descrição, categoria, início/término/`isAllDay`, `officialLink`, `ticketLink`, `locationName`, `address`, `city`, `state`, `eventStatus` (`PLANNED, CONFIRMED, ATTENDED, CANCELLED`), `ticketStatus` (`FREE, NOT_BOUGHT, PENDING, PARTIALLY_PAID, BOUGHT, PAID, CANCELLED`), preço unitário, quantidade, taxas, total, compra, vencimento, `paymentMethod`, `creditCardId`, `installmentsCount` e `notes`.
- **Vínculos e custos:** relaciona tarefas (`tasks.eventId`) e lançamentos financeiros (`transactions.eventId`), diferenciando ingresso (`isTicketObligation = true`) de custos extras por `EventCostType` (`TICKET, TRANSPORT, FOOD, LODGING, PARKING, SHOPPING, OTHER`). `EventWithDetails` soma custo estimado (exclui `CANCELLED`) e custo pago (`SETTLED`).
- **Integração externa local:** abre endereço no aplicativo de mapas via Intent `geo:0,0?q=...` e rota via `https://www.google.com/maps/dir/?api=1&destination=...`.

---

## 10. Navegação

Todos os pontos abaixo: **CONFIRMADO NO CÓDIGO** (`MainActivity`, `AndroidManifest.xml`).

- **Implementação atual:** Embora `navigation-compose:2.8.5` conste no Gradle, não há `NavHost`. A navegação é gerenciada integralmente por estado Compose em `NexusAppRoot`: `currentTab: MainTab` (`HOME, AGENDA, TASKS, FINANCE, EVENTS`), `subRoute: SubRoute` (`None, MoreMenu, WidgetsHub, GlobalSearch, Settings, Backup, Diagnostics, RoutineAssistant, Planning, DashboardCustomization, QuoteLibrary`) e `activeEditor: ActiveEditor` (`None, QuickAddChooser` e 12 editores).
- **Botão Voltar e deep-links:** `BackHandler` **não existe no código**; apenas botões internos de voltar/fechar (`onBack` / `onDismissRequest`) fecham subtelas e modais (pressionar Voltar do sistema encerra a Activity). Deep-links `nexus://open/...` e extras de notificação/widget são roteados via `incomingIntentFlow` e `onNewIntent`.

---

## 11. Backup e exportação

Todos os pontos abaixo: **CONFIRMADO NO CÓDIGO** (`BackupManager`, `BackupModels`, `QuoteLibraryScreen`).

- **JSON completo (`CURRENT_FORMAT_VERSION = 4`):** exporta via SAF todas as **25 tabelas** com metadados (`formatVersion, appVersion, exportedAt, databaseVersion`). O parser (`Json { ignoreUnknownKeys = true; coerceInputValues = true; isLenient = true }`) tolera backups v1, v2 e v3 (campos ausentes assumem defaults; enums inválidos usam fallback; blocos futuros geram aviso). A restauração executa em `database.withTransaction`, limpa tabelas, saneia FKs órfãs, grava em ordem segura e dispara ressincronização de lembretes e widgets.
- **CSV financeiro (`exportFinancialCsv`):** exporta `transactions` em UTF-8 via SAF com **15 colunas separadas por vírgula (`,`)** e escape por aspas, valores numéricos em `Locale.US` (`%.2f`) e datas em `dd/MM/yyyy`.
- **Frases:** `QuoteLibraryScreen` importa/exporta frases via SAF em JSON ou TXT (separadores ` - ` ou ` | ` para autoria, ignorando linhas `#`).

---

## 12. Notificações e widgets

Todos os pontos abaixo: **CONFIRMADO NO CÓDIGO** (`notifications/*`, `widgets/*`, `AndroidManifest.xml`).

- **Notificações nativas:** `NexusNotificationChannels` cria 4 canais (`nexus_schedule`, `nexus_finance`, `nexus_events`, `nexus_routine`). `NexusNotificationScheduler` agenda via `AlarmManager.setExactAndAllowWhileIdle` (com fallback para `setAndAllowWhileIdle` se `!canScheduleExactAlarms()`). `NexusBroadcastReceivers` trata disparo (`NexusReminderReceiver`), ações rápidas (`Concluir`, `Marcar pago`, `Adiar 15m`, `Dispensar`, `Marcar revisado` em `NexusNotificationActionReceiver`) e reagendamento pós-boot (`NexusBootReceiver`). `NexusReminderSyncWorker` sincroniza a cada 15 minutos via `WorkManager` (`KEEP`).
- **Widgets Glance (4 widgets reais):** `MyDayGlanceWidget`, `MyTasksGlanceWidget` (conclusão em 2 etapas com proteção contra `BLOCKED`/terminais), `FinanceGlanceWidget` e `NexusSummaryGlanceWidget`, todos com `SizeMode.Responsive` (`SMALL, MEDIUM, LARGE`), configuração isolada por `appWidgetId` (`NexusWidgetConfigActivity` + `SharedPreferences`) e atualização reativa controlada por `Mutex` em `NexusWidgetUpdater` + worker/receivers de data/boot.
- **Permissões no Manifest:** somente `POST_NOTIFICATIONS`, `RECEIVE_BOOT_COMPLETED` e `SCHEDULE_EXACT_ALARM`; `android:allowBackup="false"`. Sem permissão `INTERNET`.

---

## 13. Briefing, frases e personalização

Todos os pontos abaixo: **CONFIRMADO NO CÓDIGO**.

- **Personalização (`PersonalizationSettingsEntity` + `DashboardCustomizationScreen`):** controla exibição/template de saudação (`AUTO` ou `{nome}`), formato de data (`NexusDateFormat`), modo compacto, resumo numérico e lista ordenada/habilitável de 9 blocos (`DashboardBlockType`) serializada em `dashboardBlocksJson`.
- **Frases (`QuoteEntity` + `QuoteLibraryScreen`):** começa vazia (zero frases fictícias embutidas); autoria vazia exibe `"Autoria não informada"`; `selectDailyQuote` fixa uma frase por `dateIso` e evita repetir `lastQuoteId`/`previousQuoteId` quando há `>= 2` frases ativas.
- **Briefing e Revisão Noturna (`RoutineAssistantScreen`):** 3 abas (`Briefing Diário`, `Revisão Noturna` com opção de adiar tarefas para amanhã 09h/12h em 1 toque, e `Lembretes & Config`), gravando execuções em `briefing_history` com unicidade por `(briefingType, dateIso)`.

---

## 14. Visual

Todos os pontos abaixo: **CONFIRMADO NO CÓDIGO** (`NexusTheme`, `NexusComponents`).

- **Tema (escuro por padrão, alternável para claro):**
  - **DarkPalette:** fundo `#050505`, superfície `#0C0C0C`, elevada `#121212`, borda `#242424`, texto `#F2F2F2` / `#9A9A9A`, destaque `#FFFFFF`, semântica `#34C759` / `#E5484D` / `#E5A000` / `#4C8BF5`.
  - **LightPalette:** fundo `#F6F6F6`, superfície `#FFFFFF`, elevada `#EDEDED`, borda `#DCDCDC`, texto `#111111` / `#5A5A5A`, destaque `#000000`, semântica `#1E8E3E` / `#C62828` / `#B26A00` / `#1A73E8`.
- **Componentes compartilhados:** `NexusCard`, `SectionHeader`, `NexusEmptyState`, `TaskRowCard`, `PriorityIndicatorDot`, `ConfirmDangerDialog`, `NexusDatePickerDialog`, `NexusTimePickerDialog`, `ReminderLeadTimeSelector` e `TopSubScreenBar`.

---

## 15. Testes

- **Suíte existente (CONFIRMADO NO CÓDIGO):** 5 arquivos JUnit 4 puro em `app/src/test/java/com/nexus/app/` (36 `@Test` no total), cobrindo apenas lógica estática, funções puras e parsers/DTOs — **nenhum teste instrumentado (`androidTest`) ou de migração Room real existe**:
  - `NexusPhase1UnitTests.kt` (4 `@Test`): conflitos, `isTaskOverdue`, fallbacks Fase 1, tolerância de backup.
  - `NexusPhase2UnitTests.kt` (4 `@Test`): `parseBrlToCents`, `advanceDateByFrequency`, `effectiveTransactionStatus`, compatibilidade v1->v2.
  - `NexusPhase3And4UnitTests.kt` (6 `@Test`): gatilho de lembrete, horário de silêncio, próximo horário de rotina, status de fatura, limite de cartão/custos e backup v2/v3.
  - `NexusPhase5WidgetUnitTests.kt` (10 `@Test`): montagem de snapshots dos 4 widgets, estado vazio/erro, tamanho responsivo e efeito de ações/restauração.
  - `NexusPhase6UnitTests.kt` (12 `@Test`): blocos do dashboard, JSON corrompido, saudação/datas, frases (0/1/rotação e importação TXT/JSON), objetivos (`AUTO`/`MANUAL`/`goalId`) e backup v4/v3->v4.
- **Execução atual (NÃO VERIFICADO / SUPOSIÇÃO):** Nenhum build Gradle ou teste JUnit foi executado neste ambiente (container sem JDK/Android SDK).

---

## 16. Limitações e dívidas técnicas

**Confirmadas no código (CONFIRMADO NO CÓDIGO):**
- Ausência total de `BackHandler`: o botão Voltar físico do Android fecha a `MainActivity` em vez de retornar de subtelas/modais.
- `navigation-compose:2.8.5` está no catálogo/Gradle sem uso real.
- Pasta `app/schemas/` ausente no repositório apesar de `exportSchema = true`.
- `observeFinanceSummary` soma receitas e despesas de **todas as datas do banco** nos cartões totais (não apenas o mês corrente), e `CreditCardEntity.availableLimitCents` é editável na UI mas ignorado no cálculo de `computedRemainingLimitCents`.
- `AgendaViewMode.MONTH` marca os dias do mês, mas a lista inferior exibe somente os itens de `selectedDate`.
- Editores de Tarefa, Compromisso, Transação e Evento inicializam `reminderMinutesBefore = null` ao reabrir um registro, sem carregar a antecedência personalizada já salva na tabela `reminders`.
- `NexusDatePickerDialog` converte datas em `ZoneOffset.UTC` enquanto o restante do app usa `America/Sao_Paulo`.
- Widgets possuem `updatePeriodMillis` mínimo de 30 minutos (limitação Android) e dependem de `WorkManager`/alarmes sujeitos a restrições OEM/Doze.

**Não verificáveis sem compilação ou dispositivo/emulador Android (NÃO VERIFICADO / SUPOSIÇÃO):**
- Compilação efetiva `:app:compileDebugKotlin` / `:app:assembleDebug` e execução de `./gradlew testDebugUnitTest` na árvore atual.
- Geração de schema KSP, execução real das migrações Room `v1->v2->v3->v4`, fluxo SAF de importação/exportação, alarmes em Doze, ações de notificação, deep-links e renderização/interação dos 4 widgets Glance no Launcher.

---

## 17. Mapa das telas

Todas as telas abaixo: **CONFIRMADO NO CÓDIGO**.

| Tela / Modal | O que mostra | Tabelas lidas |
| --- | --- | --- |
| `HomeScreen` | Blocos configuráveis: saudação, frase, resumo, ações rápidas, agenda de hoje, tarefas, finanças/gráficos, eventos e objetivos | `personalization_settings, settings, quotes, tasks, appointments, events, transactions, credit_card_statements, projects, categories, personal_goals, goal_milestones, briefing_history` |
| `AgendaScreen` | Calendário/lista unificada por Dia, Semana, Mês e Lista, busca, categoria e conflitos | `appointments, tasks, events, transactions, credit_card_statements, credit_cards, projects, categories` |
| `TasksAndProjectsScreen` | Abas Lista, Kanban, Atrasadas, Concluídas e Projetos, filtros e progresso | `tasks, subtasks, projects, categories, events, personal_goals, transactions` |
| `FinanceScreen` | Abas Visão Geral (gráficos/saldo), Lançamentos, Cartões/Faturas, Recorrências/Parcelas e Planejamento | `settings, transactions, categories, credit_cards, credit_card_statements, installment_purchases, recurring_rules, loans, budgets, financial_goals, audit_history` |
| `EventsScreen` | Eventos por status, ingresso, parcelas, tarefas/custos vinculados e atalhos para Maps | `events, categories, credit_cards, tasks, transactions` |
| `PlanningGoalsScreen` | Objetivos pessoais, metas por marco/manual, progresso ligado a projetos/tarefas e histórico | `personal_goals, goal_milestones, goal_progress_logs, categories, projects, tasks` |
| `RoutineAssistantScreen` | Briefing diário, revisão noturna com adiamento em 1 toque e configuração de canais/silêncio | `routine_assistant_settings, briefing_history, reminders, tasks, appointments, transactions, credit_card_statements, events, projects` |
| `DashboardCustomizationScreen` | Toggle/ordenação dos 9 blocos, saudação, formato de data e modo compacto | `personalization_settings, settings` |
| `QuoteLibraryScreen` | Cadastro, edição, ativação, importação TXT/JSON e exportação de frases | `quotes, personalization_settings` |
| `GlobalSearchScreen` | Busca textual unificada em tempo real | `tasks, projects, appointments, events, transactions, credit_cards, loans, categories, personal_goals` |
| `MoreMenuScreen` / `WidgetsHubScreen` | Menu secundário, atalhos para subtelas e guia de status/widgets Glance | Estado do VM + `AppWidgetManager` |
| `SettingsScreen` | Nome de usuário, tema claro/escuro e gestão de categorias | `settings, categories` |
| `BackupScreen` / `DiagnosticsScreen` | Exportação/restauração JSON, CSV financeiro, integridade de contagens e `error_logs` | Todas as 25 tabelas (backup) / `error_logs, audit_history` (diagnóstico) |
| Modais editores (`EntityEditors`, `FinanceEditors`) | Criação/edição rápida de tarefas, subtarefas, compromissos, projetos, eventos, objetivos e itens financeiros | Escrita nas entidades respectivas + `reminders` + `audit_history` |
