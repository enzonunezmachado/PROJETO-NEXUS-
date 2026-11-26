# 07 — Estado Real do Sistema (Auditoria Factual do Código)

> **Data de referência da inspeção de código:** 2026-11-26 (Rodada 3 — Ponte ChatGPT → `NEXUS-INBOX` → NEXUS Android sobre Contrato `v1` + Extensões `CHECKLIST` / `UPDATE` / Dry-Run)  
> **Histórico recente preservado:** relato de uso de 2026-11-22 sobre `BUG-001` mantido em aberto na Seção 4.  
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
| **Aplicativo** | `com.nexus.app`, `versionCode = 8`, `versionName = "5.3.0"`, assinatura configurável via `keystore.properties` / variáveis de ambiente | `app/build.gradle.kts`, `keystore.properties.example` |
| **SDK / Kotlin / AGP** | `minSdk = 26`, `targetSdk = 35`, `compileSdk = 35`, Kotlin `2.1.0`, AGP `8.7.3`, KSP `2.1.0-1.0.29`, Java `17` | `app/build.gradle.kts`, `gradle/libs.versions.toml` |
| **Room SQLite** | `DATABASE_VERSION = 6`, `27` entidades, `27` DAOs, `5` migrações (`1→6`), `exportSchema = true` | `NexusDatabase.kt`, `NexusMigrations.kt` |
| **Backup & Entrada Estruturada** | `CURRENT_FORMAT_VERSION = 7` (27 tabelas + `marketConfig`), Modo 1 (Restauração Completa) + Modo 2 (`BackupMergeEngine.previewMerge` dry-run + `mergeAtomically`), retrocompatível `v1..v7`, CSV 15 cols; camada `data/ingestion/` (`contractVersion = 1`, `CHECKLIST`, `subtasks`, `UPDATE`/`UPSERT`, `previewEnvelope`) + conector `data/inbox/` (**NEXUS-INBOX** via GitHub privado `GET` + Keystore `AES/GCM` + confirmação explícita) sobre Room `v6` + `audit_history` | `BackupModels.kt`, `BackupStorageGateway.kt`, `BackupMergeEngine.kt`, `BackupManager.kt`, `StructuredInput*.kt`, `data/inbox/*`, `NexusInboxScreen.kt` |
| **Mercado** | **12 ativos**, Lotes `01–09` implementados (cotações, histórico, gráficos, watchlist, alertas, backup v7) | `data/market/*`, `ui/market/*`, `MarketScreen.kt` |
| **Permissões Manifest** | `INTERNET`, `ACCESS_NETWORK_STATE`, `POST_NOTIFICATIONS`, `RECEIVE_BOOT_COMPLETED`, `SCHEDULE_EXACT_ALARM`; `allowBackup="false"` | `AndroidManifest.xml` |
| **Testes JUnit no repo** | **18 arquivos** (`137` `@Test`) em `app/src/test/java/com/nexus/app/` (`0` `androidTest`) | `app/src/test/` |
| **CI GitHub Actions** | `setup-android@v4` (`platform-tools`) + `sdkmanager "platform-tools" "build-tools;35.0.0"` com validação de `adb` e `apksigner`, `keytool` (`-storepass:env`), `gradle :app:testDebugUnitTest :app:assembleDebug` + `:app:assembleRelease` condicional, `apksigner verify --verbose --print-certs` (SHA-256), `$GITHUB_STEP_SUMMARY` & `NEXUS-Signing-Verification-Report` | `.github/workflows/android-build.yml` |
| **Pasta `app/schemas/` e `gradlew`** | Ausentes no disco (`app/schemas/` gerada pelo KSP em build; CI usa `setup-gradle@v4`) | Raiz / `app/` |

---

## 3. Situação Detalhada por Módulo e Funcionalidade

| Área / Funcionalidade | Status Atual | Evidência Real no Código |
| --- | --- | --- |
| **Shell, `MainTab` (`TASKS, AGENDA, HOME, FINANCE, EVENTS`), `SubRoute`, `BackHandler`** | `IMPLEMENTADA` / `INCONSISTENTE` | `MainActivity.kt` conecta `activeEditorDirty` ao `BackHandler` e `BackNavigationRules` distingue `TOP_LEVEL_SUBROUTE` vs `MORE_CHILD_SUBROUTE`. **Em aberto (`BUG-001`, persistência confirmada por novo relato de uso):** após instalar/testar o lote visual/adaptativo, o usuário voltou a observar retorno à tela inicial ao girar celular/tablet em diferentes abas; exemplo explícito: edição de tarefa já existente. A inspeção anterior apontou estados de navegação/editores locais usando `remember` (sem `rememberSaveable`/`SavedStateHandle`) e reatribuição de `incomingIntentFlow` em `onCreate` sem guarda de recriação. Essas hipóteses precisam ser revalidadas no código atual; o relato confirma a persistência do defeito, não fecha sozinho o diagnóstico. |
| **Dependência `navigation-compose:2.8.5`** | `INCONSISTENTE` | Declarada em `app/build.gradle.kts:65`, porém sem nenhum uso (navegação é por estado). |
| **Identidade Visual N-1, `COR-001` e `core-splashscreen` (`REQ-003`)** | `PARCIALMENTE IMPLEMENTADA` | `NexusWordmark` (`N-1` + `E,X,U,S` *Kinetik Cut*) implementado na TopBar de `MainActivity.kt` sem `N` verde (`COR-001`); launcher icon adaptativo **N-1** branco/preto (`ic_launcher_nexus_*.xml`, `mipmap-anydpi-v26/ic_launcher*.xml`, `AndroidManifest.xml`). **Pendente:** `androidx.core:core-splashscreen` (`Theme.NEXUS.Splash`). |
| **Tema Claro Grafite Suave e `ColoredBorderRole`** | `IMPLEMENTADA` | `LightPalette` atualizada em `NexusTheme.kt` (`#D4D7DC`, `#E3E5E8`, `#C9CED4`, `#B0B6BF`); `ColoredBorderRole` padroniza contornos coloridos proporcionais em ambos os temas preservando bordas neutras. |
| **Dashboard (`HomeScreen`)** | `IMPLEMENTADA` | Blocos configuráveis (9 tipos), saudação + nome na mesma linha com data e frase no cabeçalho, layout em 2 colunas em telas largas (`>= 720.dp`), `TaskRingChart`, `DonutChart`, `BarGroupChart`, `AlertBannerCard`, próximos itens, metas/projetos. *(Sem seletor de mês no bloco de tarefas da Home e sem `DashboardBlockType.MARKET`.)* |
| **Tarefas, Kanban, Subtarefas e `"Registrar gasto"`** | `IMPLEMENTADA` / `INCONSISTENTE` | `TaskEditorSheet` + Kanban + vínculo `transactions.taskId` + `"Tarefa: <título>"` em `TransactionRowCard` (`FinanceScreen.kt`) + grade 2 colunas em telas largas (`>= 720.dp`). **Pendência `INT-03`:** `onToggleSubtask` em `TaskEditorSheet` ainda persiste no Room antes de clicar em `"Salvar"`. |
| **Recorrência de Tarefas (`Lote 06 Recorrência`)** | `IMPLEMENTADA` | `TaskRecurrenceEntity`, `RecurrenceExceptionEntity`, `RecurrenceEngine`, projeção virtual na Agenda, `"Só esta"` / `"Toda a série"` e `NexusLote6RecurrenceUnitTests.kt`. |
| **Recorrência de Compromissos e Eventos** | `NÃO IMPLEMENTADA — GAP-001 / REQ-009` | Apenas Tarefas e regras financeiras (`recurring_rules`) possuem recorrência hoje. A ponte `v1` também não cria regras recorrentes novas (GAP-002); consultar `15_REGISTRO_LACUNAS_E_FALHAS.md`. |
| **Agenda (`AgendaScreen` + `AgendaEngine`)** | `IMPLEMENTADA` / `INCONSISTENTE` | `AgendaEngine` expande 6 fontes + recorrências virtuais + multi-dia + filtros + conflitos. Visão semanal adapta altura à janela, exibe 7 dias simultaneamente em `>= 720.dp` (ou `LazyRow` abaixo) e ativa 2 painéis em tablet paisagem (`>= 840.dp x 480.dp`). **Pendência `INT-01`:** `NexusRepository.observeUnifiedAgenda()` ainda coexiste para Dashboard/Briefings. |
| **Financeiro Visual Completo** | `IMPLEMENTADA` | `MonthYearSelectorBar`, `DonutChart` interativo, `BarGroupChart` 6 meses (`Realizado | Previsto`), `Forecast30DaysSummary`, `ProgressBarRow` em orçamentos/metas, cartões/faturas sem dupla despesa, `onToggleSettle` conectado e grade/blocos em 2 colunas para telas largas (`>= 720.dp`). |
| **Eventos (`EventsScreen`) & Objetivos (`PlanningGoalsScreen`)** | `PARCIALMENTE IMPLEMENTADA` / `IMPLEMENTADA` | Eventos suportam CRUD, ingresso gratuito/pago/parcelado, custos extras, mapas e 2 colunas em telas largas (`>= 720.dp`); **faltam** `locationMode`, `bannerPath` (`PickVisualMedia`), ocultação padrão de `ticketFeesCents` e criação/listagem completa de compromissos vinculados na ficha do evento. Objetivos (`AUTO`/`MANUAL`, marcos, logs) e 2 colunas em `>= 720.dp` estão implementados. |
| **Módulo Mercado (`Lotes 01–09`)** | `IMPLEMENTADA` | 12 ativos, provedores públicos com fallback, `MarketQuoteSyncWorker` (30m), `MarketHistoryChartCard` + cache em arquivo, watchlist, alertas de preço + `CHANNEL_MARKET_ALERTS` + deep-link, layout adaptativo em telas largas (`>= 720.dp` listagem / `>= 840.dp` detalhe em 2 painéis) e backup v7 com rollback coordenado (`MarketBackupIntegrationUnitTests.kt`). |
| **Assistente de Rotina, Notificações & Widgets Glance** | `IMPLEMENTADA` | Briefing Diário, Revisão Noturna, adiamento 1 toque, 5 canais, `AlarmManager`, ações rápidas, `BootReceiver`, `NexusReminderSyncWorker` e 4 widgets Glance com paleta Grafite Suave/Dark (`DayNightColorProvider`) e composição limpa **sem borda externa** (`NexusWidgetShell`). |
| **Configurações, Frases, Busca Global, Backup v7 (Dry-Run + Mesclagem/Restauração), Entrada Estruturada (`v1`), Ponte `NEXUS-INBOX` e Diagnóstico** | `IMPLEMENTADA` | `BackupManager` separa explicitamente Restauração Completa (diálogo próprio + checkbox destrutivo obrigatório) de Mesclagem Incremental com prévia real dry-run (`BackupMergeEngine.previewMerge` sem gravações) e `MergeSummary` legível em pt-BR; camada `data/ingestion/` (`StructuredInputProcessor` + `RoomStructuredInputGateway`, `contractVersion = 1` com `CHECKLIST`, `subtasks`, `UPDATE`/`UPSERT` e `previewEnvelope`) conectada ao `NexusRepository` sobre Room `v6` + `audit_history`; conector opcional **NEXUS-INBOX** em `Mais → NEXUS-INBOX (Ponte ChatGPT)` (`data/inbox/*`, `NexusInboxScreen.kt`, `InboxSyncWorker`) consulta GitHub privado via `HttpsURLConnection` (`GET`) com PAT cifrado no Android Keystore, exige confirmação explícita sobre a prévia e grava resumo sem categorias vazias em `audit_history` (`INBOX_IMPORT_LOG`); `GlobalSearch` calcula conflitos no conjunto completo. **Falta apenas:** exportação/restauração `.zip` com banners de eventos. |

---

## 4. Reavaliação Factual das Inconsistências `INT-01` a `INT-10` e `BUG-001`

| ID | Descrição | Situação Real Hoje no Código |
| --- | --- | --- |
| `BUG-001` | Perda de estado de navegação/formulário na rotação/recriação da `Activity` | **EM ABERTO — persistiu no teste pós-lote visual/adaptativo**. Novo relato: rotação em diferentes abas, inclusive editando tarefa existente, retorna à tela inicial. Causa e abrangência por tela ainda exigem auditoria atual do código. |
| `INT-01` | `observeUnifiedAgenda` paralelo ao `AgendaEngine` | **EM ABERTO** (`NexusRepository.kt:494` ainda alimenta Dashboard, Briefing e NightReview) |
| `INT-02` | `TransactionWithDetails` sem `task` e card sem `"Tarefa: ..."` | **RESOLVIDO** (`DomainModels.kt:78` + `FinanceScreen.kt`) |
| `INT-03` | Subtarefa salva no Room antes de clicar "Salvar" / gasto força save | **PARCIALMENTE EM ABERTO** (`onToggleSubtask` persiste direto; `"Registrar gasto"` pede confirmação explícita `showSaveForExpenseConfirm`) |
| `INT-04` | Briefing/NightReview sem filtro `EXPENSE` e `statementId == null` | **PARCIALMENTE RESOLVIDO** (`AgendaEngine.isDirectAgendaBill` já aplicado, embora compromissos ainda venham de `observeUnifiedAgenda`) |
| `INT-05` | `saveEvent` gravava `FinancialStatus.OVERDUE` literal no banco | **RESOLVIDO** (`NexusRepository.kt:1896, 1950` agora grava `PENDING` ou `SETTLED`) |
| `INT-06` | `observeGlobalSearch` calculava conflito só no subconjunto | **RESOLVIDO** (`NexusRepository.kt:788` usa `computeAppointmentConflicts(allAppointments)`) |
| `INT-07` | `TransactionEditorSheet` forçava `EventCostType.OTHER` quando `initialEventId != null` | **RESOLVIDO** (`FinanceEditors.kt:117` mantém `tx?.eventCostType` ou `null`) |
| `INT-08` | `BackHandler` com `editorHasUnsavedChanges = false` fixo e volta quebrada | **RESOLVIDO** (`MainActivity.kt` passa `activeEditorDirty` real e `BackNavigationRules` distingue sub-rotas) |
| `INT-09` | `onToggleSettle = {}` no-op no Financeiro / Busca Global | **RESOLVIDO** (`MainActivity.kt` + `FinanceScreen.kt` + `MoreSettingsBackupDiagnosticsScreens.kt`) |
| `INT-10` | `NexusWidgetTheme` exclusivamente escuro | **RESOLVIDO** (`NexusWidgetTheme.kt` usa `DayNightColorProvider` atualizado para Grafite Suave / Dark) |
| *Testes antigos* | `NexusStage1UnitTests.kt:340` (`SNOOZED`) e `NexusPhase6UnitTests.kt:403` (`assertEquals(4)`) | **RESOLVIDO** (corrigidos no código atual) |

---

## 5. Itens Não Verificados Neste Ambiente (`NÃO VERIFICADO`)

Por ausência de **JDK, Gradle, Android SDK, emulador/dispositivo físico Android** neste container:
1. Compilação Kotlin/Compose/KSP (`:app:kspDebugKotlin`, `:app:compileDebugKotlin`, `:app:compileDebugUnitTestKotlin`, `:app:assembleDebug`, `:app:assembleRelease`) — incluindo as evoluções visuais/adaptativas, Rodada 1, Etapa 2 e Rodada 3 (`data/inbox/*`, `NexusInboxScreen.kt`, extensões de `data/ingestion/*`, `InboxSyncWorker`).
2. Execução real das `18` suítes de teste JUnit (`137` `@Test`) em `:app:testDebugUnitTest` (incluindo `BackupIncrementalMergeUnitTests` com `13` `@Test`, `StructuredInputUnitTests` com `10` `@Test`, `InboxBridgeUnitTests` com `3` `@Test`, `NexusLote6RecurrenceUnitTests` e os `6` arquivos `Market*UnitTests`). Apenas validação estática e verificação bash do workflow foram executadas neste container.
3. Geração de `app/schemas/` e testes reais de migração SQLite `1→2→3→4→5→6`.
4. Validação em dispositivo Android real e tablet/emulador: atualização in-place via `adb install -r` com verificação `apksigner verify --verbose --print-certs` após provisionamento de keystore persistente, prévia dry-run e mesclagem incremental de backup real, consulta real da `NEXUS-INBOX` com repositório privado GitHub e PAT no Keystore + notificação/deep-link em segundo plano, tema Grafite Suave, bordas proporcionais, widgets sem bordas, Agenda em 7 colunas vs deslizável e 2 painéis, ícone N-1, SAF, Doze, `BootReceiver`, notificações + deep-links e HTTP real.


## 6. Referência ao inventário consolidado

As lacunas funcionais, defeitos, inconsistências documentais e erros/validações da Rodada 3 estão consolidados em [`15_REGISTRO_LACUNAS_E_FALHAS.md`](./15_REGISTRO_LACUNAS_E_FALHAS.md). A recorrência de compromissos e eventos permanece não implementada; não confundir a recorrência de tarefas existente com suporte genérico a séries.
