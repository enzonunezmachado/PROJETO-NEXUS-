# 07 — Estado Real do Sistema (Auditoria Factual do Código)

> **Data de referência da inspeção de código:** 2026-11-26 (Rodada 4 — Recorrência de Compromissos/Eventos `GAP-001`/`REQ-009` + Independência de Checklists `GAP-003` em Room `v7` / Backup `v8`)  
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
| **Room SQLite** | `DATABASE_VERSION = 7`, `27` entidades, `27` DAOs, `6` migrações (`1→7`), `exportSchema = true` | `NexusDatabase.kt`, `NexusMigrations.kt` |
| **Backup & Entrada Estruturada** | `CURRENT_FORMAT_VERSION = 8` (27 tabelas + `marketConfig` + suporte a recorrência multi-domínio e `templateSubtasksJson`), Modo 1 (Restauração Completa) + Modo 2 (`BackupMergeEngine.previewMerge` dry-run + `mergeAtomically`), retrocompatível `v1..v8`, CSV 15 cols; camada `data/ingestion/` (`contractVersion = 1`, `CHECKLIST`, `subtasks`, `UPDATE`/`UPSERT`, `previewEnvelope`) + conector `data/inbox/` (**NEXUS-INBOX** via GitHub privado `GET` + Keystore `AES/GCM` + confirmação explícita) com recibos em `audit_history` | `BackupModels.kt`, `BackupStorageGateway.kt`, `BackupMergeEngine.kt`, `BackupManager.kt`, `StructuredInput*.kt`, `data/inbox/*`, `NexusInboxScreen.kt` |
| **Mercado** | **12 ativos**, Lotes `01–09` implementados (cotações, histórico, gráficos, watchlist, alertas, backup v8) | `data/market/*`, `ui/market/*`, `MarketScreen.kt` |
| **Permissões Manifest** | `INTERNET`, `ACCESS_NETWORK_STATE`, `POST_NOTIFICATIONS`, `RECEIVE_BOOT_COMPLETED`, `SCHEDULE_EXACT_ALARM`; `allowBackup="false"` | `AndroidManifest.xml` |
| **Testes JUnit no repo** | **19 arquivos** (`145` `@Test`) em `app/src/test/java/com/nexus/app/` (`0` `androidTest`) | `app/src/test/` |
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
| **Recorrência de Tarefas (`Lote 06` + `Rodada 4 GAP-003`)** | `IMPLEMENTADA` | `TaskRecurrenceEntity` (`templateSubtasksJson`), `RecurrenceExceptionEntity`, `RecurrenceEngine` (incl. `ANNUAL`), projeção virtual na Agenda, `"Só esta"` / `"Toda a série"`, checklist independente por ocorrência (checkboxes nunca alteram o template da série) e `NexusLote6RecurrenceUnitTests.kt` + `NexusRodada4RecurrenceUnitTests.kt`. |
| **Recorrência de Compromissos e Eventos (`GAP-001` / `REQ-009`)** | `IMPLEMENTADA` *(no código — aguardando Gradle/CI e dispositivo)* | `TaskRecurrenceEntity` (`RecurrenceDomain.APPOINTMENT`/`EVENT`), `RecurrenceExceptionEntity` (overrides de horário/descrição/local/link/dia inteiro), projeção virtual em `AgendaEngine` e `EventsScreen`, materialização idempotente sob demanda (`"rec::<recId>::<dateIso>"`), escopos `"Só esta ocorrência"` vs `"Toda a série"`, proteção anti-obrigação financeira fantasma em eventos e `NexusRodada4RecurrenceUnitTests.kt`. *(Contrato `v1` da ponte `NEXUS-INBOX` ainda não cria séries recorrentes novas — `GAP-002` mantido fora desta rodada.)* |
| **Agenda (`AgendaScreen` + `AgendaEngine`)** | `IMPLEMENTADA` / `INCONSISTENTE` | `AgendaEngine` expande 6 fontes + recorrências virtuais (Tarefas, Compromissos e Eventos) + multi-dia + filtros + conflitos (inclusive virtual↔materializado). Visão semanal adapta altura à janela, exibe 7 dias simultaneamente em `>= 720.dp` (ou `LazyRow` abaixo) e ativa 2 painéis em tablet paisagem (`>= 840.dp x 480.dp`). **Pendência `INT-01`:** `NexusRepository.observeUnifiedAgenda()` ainda coexiste para Dashboard/Briefings. |
| **Financeiro Visual Completo** | `IMPLEMENTADA` | `MonthYearSelectorBar`, `DonutChart` interativo, `BarGroupChart` 6 meses (`Realizado | Previsto`), `Forecast30DaysSummary`, `ProgressBarRow` em orçamentos/metas, cartões/faturas sem dupla despesa, `onToggleSettle` conectado e grade/blocos em 2 colunas para telas largas (`>= 720.dp`). |
| **Eventos (`EventsScreen`) & Objetivos (`PlanningGoalsScreen`)** | `PARCIALMENTE IMPLEMENTADA` / `IMPLEMENTADA` | Eventos suportam CRUD, ingresso gratuito/pago/parcelado, custos extras, mapas, séries recorrentes projetadas com preservação de ocorrências liquidadas/`ATTENDED` e 2 colunas em telas largas (`>= 720.dp`); **faltam** `locationMode`, `bannerPath` (`PickVisualMedia`), ocultação padrão de `ticketFeesCents` e criação/listagem completa de compromissos vinculados na ficha do evento. Objetivos (`AUTO`/`MANUAL`, marcos, logs) e 2 colunas em `>= 720.dp` estão implementados. |
| **Módulo Mercado (`Lotes 01–09`)** | `IMPLEMENTADA` | 12 ativos, provedores públicos com fallback, `MarketQuoteSyncWorker` (30m), `MarketHistoryChartCard` + cache em arquivo, watchlist, alertas de preço + `CHANNEL_MARKET_ALERTS` + deep-link, layout adaptativo em telas largas (`>= 720.dp` listagem / `>= 840.dp` detalhe em 2 painéis) e backup v8 com rollback coordenado (`MarketBackupIntegrationUnitTests.kt`). |
| **Assistente de Rotina, Notificações & Widgets Glance** | `IMPLEMENTADA` | Briefing Diário, Revisão Noturna, adiamento 1 toque, 5 canais, `AlarmManager` (com lembretes para ocorrências virtuais `rec::...`), ações rápidas, `BootReceiver`, `NexusReminderSyncWorker` e 4 widgets Glance com paleta Grafite Suave/Dark (`DayNightColorProvider`) e composição limpa **sem borda externa** (`NexusWidgetShell`). |
| **Configurações, Frases, Busca Global, Backup v8 (Dry-Run + Mesclagem/Restauração), Entrada Estruturada (`v1`), Ponte `NEXUS-INBOX` e Diagnóstico** | `IMPLEMENTADA` | `BackupManager` separa explicitamente Restauração Completa (diálogo próprio + checkbox destrutivo obrigatório) de Mesclagem Incremental com prévia real dry-run (`BackupMergeEngine.previewMerge` sem gravações) e `MergeSummary` legível em pt-BR (suporte `v1..v8` + ciclo opcional `recurrences ↔ events`); camada `data/ingestion/` (`StructuredInputProcessor` + `RoomStructuredInputGateway`, `contractVersion = 1` com `CHECKLIST`, `subtasks`, `UPDATE`/`UPSERT` e `previewEnvelope`) conectada ao `NexusRepository` sobre `audit_history`; conector opcional **NEXUS-INBOX** em `Mais → NEXUS-INBOX (Ponte ChatGPT)` (`data/inbox/*`, `NexusInboxScreen.kt`, `InboxSyncWorker`) consulta GitHub privado via `HttpsURLConnection` (`GET`) com PAT cifrado no Android Keystore, exige confirmação explícita sobre a prévia e grava resumo sem categorias vazias em `audit_history` (`INBOX_IMPORT_LOG`); `GlobalSearch` calcula conflitos no conjunto completo. **Falta apenas:** exportação/restauração `.zip` com banners de eventos. |

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

## 5. Validação previamente concluída — Ponte NEXUS-INBOX (Rodada 3)

A GitHub Actions #70 (run `45792119560`) concluiu com sucesso na `main`, com testes unitários e geração do APK Debug; Release foi ignorado por ausência dos secrets de assinatura. O usuário confirmou a importação real de registros no aparelho pelo fluxo ChatGPT → repositório privado `NEXUS-INBOX` → NEXUS Android.

Essa evidência valida o fluxo principal de importação, mas não comprova individualmente descarte sem gravação, reconsulta sem duplicidade, sincronização periódica em segundo plano, notificações/deep-links, recuperação após reinício ou todos os cenários de segurança do token. Esses itens permanecem no gate contínuo de confiabilidade.

## 6. Itens Não Verificados Neste Ambiente (`NÃO VERIFICADO`)

Por ausência de **JDK, Gradle, Android SDK, emulador/dispositivo físico Android** neste container:
1. Compilação Kotlin/Compose/KSP (`:app:kspDebugKotlin`, `:app:compileDebugKotlin`, `:app:compileDebugUnitTestKotlin`, `:app:assembleDebug`, `:app:assembleRelease`) — incluindo as evoluções visuais/adaptativas, Rodada 1, Etapa 2, Rodada 3 e **Rodada 4** (`MIGRATION_6_7`, `RecurrenceEngine`, `AgendaEngine`, `NexusRepository`, `BackupModels v8`, `EntityEditors`, `EventsScreen`, `AgendaScreen`, `HomeScreen`, `MainActivity`).
2. Execução real das `19` suítes de teste JUnit (`145` `@Test`) em `:app:testDebugUnitTest` na `main` (incluindo `NexusRodada4RecurrenceUnitTests` com `5` `@Test`, `BackupIncrementalMergeUnitTests` com `15` `@Test`, `StructuredInputUnitTests` com `10` `@Test`, `InboxBridgeUnitTests` com `4` `@Test`, `NexusLote6RecurrenceUnitTests` e os `6` arquivos `Market*UnitTests`). Apenas inspeção estática e verificação AST/símbolos em Python sobre os `.kt` foram executadas neste container.
3. Geração de `app/schemas/` e testes reais de migração SQLite `1→2→3→4→5→6→7` (especialmente `MIGRATION_6_7` com backfill de `templateSubtasksJson`).
4. Validação em dispositivo Android real e tablet/emulador: atualização in-place `v6→v7` sem perda de dados, séries recorrentes de Compromissos e Eventos (criação, projeção, materialização sob demanda, `"Só esta ocorrência"` vs `"Toda a série"`, ausência de transações fantasma), checklist independente entre ocorrências de tarefas recorrentes, prévia dry-run e mesclagem incremental `v8`, consulta real da `NEXUS-INBOX` com GitHub privado + PAT Keystore, tema Grafite Suave, widgets, SAF, Doze, `BootReceiver`, notificações + deep-links e HTTP real.


## 7. Referência ao inventário consolidado

As lacunas funcionais, defeitos, inconsistências documentais e validações pendentes estão consolidados em [`15_REGISTRO_LACUNAS_E_FALHAS.md`](./15_REGISTRO_LACUNAS_E_FALHAS.md): na Rodada 4 foram implementados no código `GAP-001` (`REQ-009`) e `GAP-003` (aguardando validação Gradle/dispositivo); permanecem abertos fora do escopo desta rodada `GAP-002` (contrato `v1` da Inbox), `BUG-001`, `INT-01`, `INT-03`, `REQ-012`, `REQ-014` e `REQ-026`.
