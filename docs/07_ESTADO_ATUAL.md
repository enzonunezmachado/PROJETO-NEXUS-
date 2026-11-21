# 07 — Estado Real do Sistema (Auditoria Factual do Código)

> **Data de referência da auditoria:** 2026-11-20  
> **Escopo auditado:** inspeção direta de todos os arquivos-fonte `.kt`, `.xml`, `.kts`, `.toml` e `.yml` do repositório ativo.  
> **Princípio absoluto:** nenhum item é marcado como aprovado em compilação/runtime sem execução real.

---

## 1. Critérios de Classificação

| Status | Critério |
| --- | --- |
| `IMPLEMENTADA` | Existe ponta a ponta no código atual (banco/store + regra/repositório + UI) conforme comportamento ativo. |
| `PARCIALMENTE IMPLEMENTADA` | Existe parte funcional no código, faltando subfluxos, telas ou unificação previstos. |
| `INCONSISTENTE` | Funciona no código, mas apresenta divergência interna, acoplamento residual ou contradição com regras do projeto. |
| `NÃO IMPLEMENTADA` | Apenas planejada em documentos; não existe no código atual. |
| `NÃO VERIFICADO` | Exige toolchain JDK/Gradle/Android SDK ou dispositivo físico/emulador (indisponíveis neste container). |

---

## 2. Baseline Confirmada no Código

| Dimensão | Valor Verificado | Arquivo Fonte |
| --- | --- | --- |
| **Aplicativo** | `com.nexus.app`, `versionCode = 7`, `versionName = "5.2.0"` | `app/build.gradle.kts` |
| **SDK / Kotlin / AGP** | `minSdk = 26`, `targetSdk = 35`, `compileSdk = 35`, Kotlin `2.1.0`, AGP `8.7.3`, KSP `2.1.0-1.0.29`, Java `17` | `app/build.gradle.kts`, `gradle/libs.versions.toml` |
| **Room SQLite** | `DATABASE_VERSION = 6`, `27` entidades, `27` DAOs, `5` migrações (`1→6`), `exportSchema = true` | `NexusDatabase.kt`, `NexusMigrations.kt` |
| **Backup** | `CURRENT_FORMAT_VERSION = 7` (27 tabelas + `marketConfig`), retrocompatível `v1..v7`, CSV 15 cols | `BackupModels.kt`, `BackupManager.kt` |
| **Mercado** | **12 ativos**, Lotes `01–09` implementados (cotações, histórico, gráficos, watchlist, alertas, backup v7) | `data/market/*`, `ui/market/*`, `MarketScreen.kt` |
| **Permissões Manifest** | `INTERNET`, `ACCESS_NETWORK_STATE`, `POST_NOTIFICATIONS`, `RECEIVE_BOOT_COMPLETED`, `SCHEDULE_EXACT_ALARM`; `allowBackup="false"` | `AndroidManifest.xml` |
| **Testes JUnit no repo** | **15 arquivos** em `app/src/test/java/com/nexus/app/` (`0` `androidTest`) | `app/src/test/` |
| **CI GitHub Actions** | `gradle :app:testDebugUnitTest :app:assembleDebug` + upload APK | `.github/workflows/android-build.yml` |
| **Pasta `app/schemas/` e `gradlew`** | Ausentes no disco (`app/schemas/` gerada pelo KSP em build; CI usa `setup-gradle@v4`) | Raiz / `app/` |

---

## 3. Situação Detalhada por Módulo e Funcionalidade

| Área / Funcionalidade | Status Atual | Evidência Real no Código |
| --- | --- | --- |
| **Shell, `MainTab` (`TASKS, AGENDA, HOME, FINANCE, EVENTS`), `SubRoute`, `BackHandler`** | `IMPLEMENTADA` | `MainActivity.kt` conecta `activeEditorDirty` ao `BackHandler` e `BackNavigationRules` distingue `TOP_LEVEL_SUBROUTE` vs `MORE_CHILD_SUBROUTE`. |
| **Dependência `navigation-compose:2.8.5`** | `INCONSISTENTE` | Declarada em `app/build.gradle.kts:65`, porém sem nenhum uso (navegação é por estado). |
| **SplashScreen API (`core-splashscreen`)** | `NÃO IMPLEMENTADA` | Não há dependência nem tema `Theme.NEXUS.Splash`; existe apenas `ic_launcher_nexus.xml`. |
| **Dashboard (`HomeScreen`)** | `IMPLEMENTADA` | Blocos configuráveis (9 tipos), saudação + data + frase no cabeçalho, `TaskRingChart`, `DonutChart`, `BarGroupChart`, `AlertBannerCard`, próximos itens, metas/projetos. *(Sem seletor de mês no bloco de tarefas da Home e sem `DashboardBlockType.MARKET`.)* |
| **Tarefas, Kanban, Subtarefas e `"Registrar gasto"`** | `IMPLEMENTADA` / `INCONSISTENTE` | `TaskEditorSheet` + Kanban + vínculo `transactions.taskId` + `"Tarefa: <título>"` em `TransactionRowCard` (`FinanceScreen.kt:1317`). **Pendência `INT-03`:** `onToggleSubtask` em `TaskEditorSheet` (`EntityEditors.kt:308` / `MainActivity.kt:1005`) ainda persiste no Room antes de clicar em `"Salvar"`. |
| **Recorrência de Tarefas (`Lote 06 Recorrência`)** | `IMPLEMENTADA` | `TaskRecurrenceEntity`, `RecurrenceExceptionEntity`, `RecurrenceEngine`, projeção virtual na Agenda, `"Só esta ocorrência"` / `"Toda a série"` e `NexusLote6RecurrenceUnitTests.kt`. |
| **Recorrência de Compromissos e Eventos** | `NÃO IMPLEMENTADA` | Apenas Tarefas e regras financeiras (`recurring_rules`) possuem recorrência hoje. |
| **Agenda (`AgendaScreen` + `AgendaEngine`)** | `IMPLEMENTADA` / `INCONSISTENTE` | `AgendaEngine` expande 6 fontes + recorrências virtuais + multi-dia + filtros + conflitos. **Pendência `INT-01`:** `NexusRepository.observeUnifiedAgenda()` (`line 494`) ainda coexiste e alimenta `observeDashboard()`, `observeDailyBriefingSnapshot()` e `observeNightReviewSnapshot()`. No modo `MONTH`, lista inferior exibe apenas `selectedDate`. |
| **Financeiro Visual Completo** | `IMPLEMENTADA` | `MonthYearSelectorBar`, `DonutChart` interativo, `BarGroupChart` 6 meses (`Realizado | Previsto`), `Forecast30DaysSummary`, `ProgressBarRow` em orçamentos/metas, cartões/faturas sem dupla despesa e `onToggleSettle` conectado. |
| **Eventos (`EventsScreen`)** | `PARCIALMENTE IMPLEMENTADA` | CRUD, ingresso gratuito/pago/parcelado no cartão, custos extras e mapas funcionam; **faltam** `locationMode`, `bannerPath` (`PickVisualMedia`) e ocultação padrão de `ticketFeesCents` (`EventsScreen.kt:764`). Ficha lista contagem de compromissos vinculados (`linkedAppointmentsCount`), sem seção completa de criação direta de compromissos na tela do evento. |
| **Módulo Mercado (`Lotes 01–09`)** | `IMPLEMENTADA` | 12 ativos, provedores públicos com fallback, `MarketQuoteSyncWorker` (30m), `MarketHistoryChartCard` + cache em arquivo, watchlist, alertas de preço + `CHANNEL_MARKET_ALERTS` + deep-link, textos atualizados na UI e backup v7 com rollback coordenado (`MarketBackupIntegrationUnitTests.kt`). |
| **Objetivos Pessoais & Assistente de Rotina** | `IMPLEMENTADA` | `PlanningGoalsScreen` (`AUTO`/`MANUAL`, marcos, logs) e `RoutineAssistantScreen` (Briefing Diário, Revisão Noturna, adiamento 1 toque, `AgendaEngine.isDirectAgendaBill` nos filtros). |
| **Notificações & Widgets Glance** | `IMPLEMENTADA` | 5 canais, `AlarmManager`, ações rápidas, `BootReceiver`, `NexusReminderSyncWorker` e 4 widgets Glance com `DayNightColorProvider` (`NexusWidgetTheme.kt`). |
| **Configurações, Frases, Busca Global, Backup v7 e Diagnóstico** | `IMPLEMENTADA` | Todos funcionais; `MoreSettingsBackupDiagnosticsScreens.kt` atualizado para `v7`; `GlobalSearch` calcula conflitos no conjunto completo de compromissos (`NexusRepository.kt:788`). **Falta apenas:** exportação/restauração `.zip` com banners de eventos. |

---

## 4. Reavaliação Factual das Inconsistências `INT-01` a `INT-10`

Muitas pendências registradas na auditoria antiga `v5` **já foram corrigidas no código atual**:

| ID Histórico | Descrição Original (`v5`) | Situação Real Hoje no Código |
| --- | --- | --- |
| `INT-01` | `observeUnifiedAgenda` paralelo ao `AgendaEngine` | **EM ABERTO** (`NexusRepository.kt:494` ainda alimenta Dashboard, Briefing e NightReview) |
| `INT-02` | `TransactionWithDetails` sem `task` e card sem `"Tarefa: ..."` | **RESOLVIDO** (`DomainModels.kt:78` + `FinanceScreen.kt:1317`) |
| `INT-03` | Subtarefa salva no Room antes de clicar "Salvar" / gasto força save | **PARCIALMENTE EM ABERTO** (`onToggleSubtask` persiste direto; `"Registrar gasto"` pede confirmação explícita `showSaveForExpenseConfirm`) |
| `INT-04` | Briefing/NightReview sem filtro `EXPENSE` e `statementId == null` | **PARCIALMENTE RESOLVIDO** (`observeDailyBriefingSnapshot` e `observeNightReviewSnapshot` já usam `AgendaEngine.isDirectAgendaBill`, embora ainda leiam `appointments` via `observeUnifiedAgenda`) |
| `INT-05` | `saveEvent` gravava `FinancialStatus.OVERDUE` literal no banco | **RESOLVIDO** (`NexusRepository.kt:1896, 1950` agora grava `PENDING` ou `SETTLED`) |
| `INT-06` | `observeGlobalSearch` calculava conflito só no subconjunto | **RESOLVIDO** (`NexusRepository.kt:788` usa `computeAppointmentConflicts(allAppointments)`) |
| `INT-07` | `TransactionEditorSheet` forçava `EventCostType.OTHER` quando `initialEventId != null` | **RESOLVIDO** (`FinanceEditors.kt:117` mantém `tx?.eventCostType` ou `null`) |
| `INT-08` | `BackHandler` com `editorHasUnsavedChanges = false` fixo e volta quebrada | **RESOLVIDO** (`MainActivity.kt` passa `activeEditorDirty` real e `BackNavigationRules` distingue sub-rotas) |
| `INT-09` | `onToggleSettle = {}` no-op no Financeiro / Busca Global | **RESOLVIDO** (`MainActivity.kt:731, 913` + `FinanceScreen.kt:657, 776` + `MoreSettingsBackupDiagnosticsScreens.kt:348`) |
| `INT-10` | `NexusWidgetTheme` exclusivamente escuro | **RESOLVIDO** (`NexusWidgetTheme.kt:44-98` usa `DayNightColorProvider`) |
| *Testes antigos* | `NexusStage1UnitTests.kt:340` (`SNOOZED`) e `NexusPhase6UnitTests.kt:403` (`assertEquals(4)`) | **RESOLVIDO** (corrigidos no código atual) |

---

## 5. Itens Não Verificados Neste Ambiente (`NÃO VERIFICADO`)

Por ausência de **JDK, Gradle, Android SDK, emulador/dispositivo físico Android** (e sem repositório `.git` local neste container):
1. Compilação Kotlin/Compose/KSP (`:app:kspDebugKotlin`, `:app:compileDebugKotlin`, `:app:compileDebugUnitTestKotlin`, `:app:assembleDebug`).
2. Execução real das `15` suítes de teste JUnit em `:app:testDebugUnitTest` (incluindo `NexusLote6RecurrenceUnitTests` e os `6` arquivos `Market*UnitTests`).
3. Geração de `app/schemas/` e testes reais de migração SQLite `1→2→3→4→5→6`.
4. Validação em dispositivo Android real: SAF (`.json`, `.csv`, `.txt`), alarmes em modo Doze, `BootReceiver`, ações de notificação, notificações e deep-links de alertas de mercado em Android 13+, widgets Glance no launcher e chamadas HTTP reais aos provedores públicos de mercado.
