# 12 — Matriz de Rastreabilidade Completa

> **Finalidade:** conectar ponta a ponta cada requisito funcional (`REQ-*` do [`02_ESPECIFICACAO_PRODUTO.md`](./02_ESPECIFICACAO_PRODUTO.md)) à sua funcionalidade, módulo, componentes reais de código, persistência, fluxo de comportamento, testes JUnit e ADRs aplicáveis.

---

## 1. Matriz Principal (`REQ-001` a `REQ-028`)

| REQ | Funcionalidade | Módulo | Componentes / Arquivos Principais | Persistência (Tabela / Store) | Fluxo (`05_*`) | Testes & ADRs |
| --- | --- | --- | --- | --- | --- | --- |
| `REQ-001` | Navegação por estado (`MainTab`, `SubRoute`, `ActiveEditor`) + `BackHandler` com confirmação de descarte | Shell / Navegação | `MainActivity.kt`, `BackNavigationRules.kt`, `NexusComponents.kt` (`ConfirmDiscardChangesDialog`) | Estado UI em memória (`NexusAppRoot`) | §1 | `NexusStage1UnitTests` · `ADR-001` |
| `REQ-002` | `QuickAddChooser` contextual *(parcial: 5 opções, sem colapso uniforme em todos os editores)* | Shell / Navegação | `EntityEditors.kt` (`QuickAddChooserSheet`), `MainActivity.kt` | Depende da entidade criada | §1 | `ADR-001` |
| `REQ-003` | Identidade N-1 + `core-splashscreen` *(parcial: N-1/Wordmark implementados; falta `core-splashscreen`)* | Shell / Identidade | `NexusComponents.kt` (`NexusWordmark`, `NexusMonogramIcon`), `MainActivity.kt`, `ic_launcher_nexus_*.xml`, `mipmap-anydpi-v26/ic_launcher*.xml` | — | — | — |
| `REQ-004` | Dashboard configurável (9 blocos, saudação, data, frase integrada ao cabeçalho, modo compacto) | 1 — Dashboard | `HomeScreen.kt`, `MoreSettingsBackupDiagnosticsScreens.kt` (`DashboardCustomizationScreen`), `DateTimeUtils.kt` | `personalization_settings`, `settings`, `quotes` | §7 | `NexusPhase6UnitTests`, `NexusStage1UnitTests` |
| `REQ-005` | Anel de tarefas (`TaskRingChart`), gráficos financeiros, `AlertBannerCard`, metas/projetos e próximos itens | 1 — Dashboard | `HomeScreen.kt`, `TaskRingChart.kt`, `DonutChart.kt`, `BarGroupChart.kt`, `FinanceRules.kt` | `tasks`, `transactions`, `projects`, `personal_goals`, `events`, `appointments` | §4, §7 | `NexusStage1UnitTests`, `NexusStage3FinanceUnitTests` · `ADR-009`, `ADR-012` |
| `REQ-006` | CRUD Tarefas, 6 status, Kanban na mesma tabela, checklist de subtarefas *(residual `INT-03` no toggle)* | 2 — Tarefas | `TasksAndProjectsScreen.kt`, `EntityEditors.kt` (`TaskEditorSheet`), `TaskRules.kt`, `NexusRepository.kt` | `tasks`, `subtasks`, `categories` | §2 | `NexusPhase1UnitTests` · `ADR-007` |
| `REQ-007` | Vínculo Tarefa → Gasto (`"Registrar gasto"`, `"Gasto: R$ X"`, `"Tarefa: <título>"`) | 2 / 6 — Tarefas & Financeiro | `EntityEditors.kt`, `FinanceEditors.kt`, `FinanceScreen.kt`, `AgendaEngine.kt` (`suggestExpenseCategoryId`), `DomainModels.kt` | `transactions.taskId` | §2 | `NexusStage2UnitTests` · `ADR-003` |
| `REQ-008` | Recorrência de Tarefas (`Lote 06`: frequências, exceções `SKIP`/`OVERRIDE`, `"Só esta"` vs `"Toda a série"`) | 2 — Tarefas | `RecurrenceEngine.kt`, `EntityEditors.kt` (`TaskRecurrenceSectionCard`), `NexusRepository.kt`, `AgendaEngine.kt` | `recurrences`, `recurrence_exceptions`, `tasks` | §3 | `NexusLote6RecurrenceUnitTests` · `ADR-008` |
| `REQ-009` | Recorrência para Compromissos e Eventos *(não implementada)* | 3 / 4 / 5 | Planejado no Roadmap (§2 item 3 de `10_ROADMAP.md`) | — | — | `ADR-008` |
| `REQ-010` | Gestão de Projetos com prazo, progresso automático e custos | 3 — Projetos | `TasksAndProjectsScreen.kt`, `EntityEditors.kt` (`ProjectEditorSheet`), `NexusRepository.kt` | `projects`, `tasks.projectId`, `transactions.projectId` | §2 | `NexusPhase6UnitTests` |
| `REQ-011` | Agenda unificada pura (`AgendaEngine`), Dia/Semana horizontal/Mês/Lista, filtros contextuais, multi-dia, conflitos | 4 — Agenda | `AgendaScreen.kt`, `AgendaEngine.kt`, `NexusRepository.kt` (`observeUnifiedAgenda` legado pendente `INT-01`) | `appointments`, `tasks`, `events`, `transactions`, `credit_card_statements`, `projects`, `recurrences` | §3, §4 | `NexusPhase1UnitTests`, `NexusStage2UnitTests`, `NexusLote6RecurrenceUnitTests` · `ADR-009` |
| `REQ-012` | Compromissos vinculados a Eventos *(parcial: FK e editor existem; falta listagem/criação direta na tela do evento)* | 4 / 5 — Agenda & Eventos | `EntityEditors.kt` (`AppointmentEditorSheet`), `EventsScreen.kt`, `AgendaEngine.kt` | `appointments.eventId` | §5 | `NexusStage2UnitTests` |
| `REQ-013` | Eventos com ingresso gratuito/pago/parcelado, custos extras (`EventCostType`) e atalhos de mapas | 5 — Eventos | `EventsScreen.kt`, `NexusRepository.kt` (`saveEvent`) | `events`, `transactions`, `installment_purchases`, `credit_card_statements` | §5 | `NexusPhase3And4UnitTests` · `ADR-006` |
| `REQ-014` | Eventos `locationMode`, banner JPEG local e ocultação padrão de `ticketFeesCents` *(não implementada)* | 5 — Eventos | Planejado no Roadmap (§2 item 1 de `10_ROADMAP.md`) | — | — | — |
| `REQ-015` | Financeiro completo: `< mês/ano >`, saldo manual, `DonutChart`, `BarGroupChart` 6m, previsão 30d, orçamentos, metas, empréstimos | 6 — Financeiro | `FinanceScreen.kt`, `FinanceEditors.kt`, `FinanceRules.kt`, `DonutChart.kt`, `BarGroupChart.kt`, `ProgressBarRow.kt` | `transactions`, `budgets`, `financial_goals`, `loans`, `recurring_rules`, `settings`, `audit_history` | §4 | `NexusPhase2UnitTests`, `NexusStage1UnitTests`, `NexusStage3FinanceUnitTests` · `ADR-006`, `ADR-012` |
| `REQ-016` | Cartões de crédito: fechamento/vencimento, pagamento de fatura sem dupla despesa, limite calculado vs informado | 6 — Financeiro | `FinanceScreen.kt`, `FinanceRules.kt` (`resolveCreditCardLimitBreakdown`), `NexusRepository.kt` (`payCreditCardStatement`) | `credit_cards`, `credit_card_statements`, `installment_purchases`, `transactions` | §4 | `NexusPhase3And4UnitTests`, `NexusStage1UnitTests` · `ADR-006` |
| `REQ-017` | Mercado Lotes 01–04: 12 ativos, roteamento público com fallback, cooldown/offline, sync 30m e conversão USD→BRL | 7 — Mercado | `MarketQuoteModels.kt`, `MarketQuoteRemoteDataSource.kt`, `MarketQuoteStore.kt`, `MarketQuoteRepository.kt`, `MarketQuoteSyncWorker.kt` | `SharedPreferences` (`nexus_market_quotes_cache`) | §6 | `MarketQuoteUnitTests`, `MarketPresentationUnitTests` · `ADR-010`, `ADR-011` |
| `REQ-018` | Mercado Lotes 03, 05–07, 09: `MarketScreen`, `MarketAssetDetail`, histórico `1D..1Y`, Watchlist, Alertas + deep-link | 7 — Mercado | `MarketScreen.kt`, `MarketAlertSheetsAndCards.kt`, `MarketHistoryChart.kt`, `MarketHistory*.kt`, `MarketWatchlist*.kt`, `MarketAlert*.kt`, `MarketPresentation.kt` | `filesDir/market_history/*.json`, `SharedPreferences` (`nexus_market_watchlist`), `filesDir/market_alerts/market_alerts.json` | §6 | `MarketHistoryUnitTests`, `MarketWatchlistUnitTests`, `MarketAlertUnitTests` · `ADR-010`, `ADR-011`, `ADR-012` |
| `REQ-019` | Objetivos pessoais (`AUTO`/`MANUAL`), marcos, logs e vínculo com Tarefas/Projetos | 8 — Objetivos | `PlanningGoalsScreen.kt`, `NexusRepository.kt` | `personal_goals`, `goal_milestones`, `goal_progress_logs`, `tasks.goalId`, `projects.goalId` | §2 | `NexusPhase6UnitTests` |
| `REQ-020` | Assistente de Rotina: Briefing Diário, Revisão Noturna, adiamento 1 toque e `briefing_history` | 9 — Rotina | `RoutineAssistantScreen.kt`, `NexusRepository.kt` | `routine_assistant_settings`, `briefing_history` | §7 | `NexusPhase3And4UnitTests` · `ADR-009` |
| `REQ-021` | Notificações locais: 5 canais, `AlarmManager` + fallback, silêncio, ações rápidas e `BootReceiver` | 10 — Notificações | `NexusNotificationChannels.kt`, `NexusNotificationScheduler.kt`, `NexusBroadcastReceivers.kt`, `NexusReminderSyncWorker.kt`, `ReminderRules.kt` | `reminders`, `routine_assistant_settings` | §6, §7 | `NexusPhase3And4UnitTests`, `NexusStage1UnitTests` |
| `REQ-022` | 4 Widgets Glance responsivos (`SMALL`/`MEDIUM`/`LARGE`), config por ID, tema claro/escuro, deep-links | 11 — Widgets | `MyDayGlanceWidget.kt`, `MyTasksGlanceWidget.kt`, `FinanceGlanceWidget.kt`, `NexusSummaryGlanceWidget.kt`, `NexusWidgetTheme.kt`, `NexusWidgetConfigActivity.kt`, `NexusWidgetUpdater.kt` | `SharedPreferences` (`nexus_widget_instance_prefs`) + Room | §7 | `NexusPhase5WidgetUnitTests` · `ADR-014` |
| `REQ-023` | Configurações (nome, temas Claro/Escuro, categorias), Personalização e Biblioteca de Frases (TXT/JSON) | 12 — Config/Frases | `MoreSettingsBackupDiagnosticsScreens.kt` (`SettingsScreen`, `DashboardCustomizationScreen`, `QuoteLibraryScreen`), `NexusTheme.kt` | `settings`, `categories`, `personalization_settings`, `quotes` | §7 | `NexusPhase6UnitTests` |
| `REQ-024` | Busca Global unificada com detecção de conflitos em todo o conjunto de compromissos | 12 — Busca | `MoreSettingsBackupDiagnosticsScreens.kt` (`GlobalSearchScreen`), `NexusRepository.kt` (`observeGlobalSearch`) | Lê todas as principais tabelas de domínio | §7 | `NexusPhase1UnitTests` |
| `REQ-025` | Backup JSON `v7` transacional via SAF (`v1..v7`), Modo 1 (restauração completa com diálogo/checkbox destrutivo) e Modo 2 (prévia dry-run `previewMerge` + mesclagem incremental idempotente), rollback Room ↔ Mercado, CSV 15 colunas, Diagnóstico e verificação de assinatura `apksigner` SHA-256 no CI | 13 — Backup/Diag. | `BackupModels.kt`, `BackupStorageGateway.kt`, `BackupMergeEngine.kt`, `BackupManager.kt`, `MoreSettingsBackupDiagnosticsScreens.kt`, `GlobalErrorHandler.kt`, `.github/workflows/android-build.yml` | 27 tabelas Room + `marketConfig` (`SharedPreferences` + JSON alertas) + `error_logs` | §7 | `NexusPhase6UnitTests`, `NexusStage2UnitTests`, `MarketBackupIntegrationUnitTests`, `BackupIncrementalMergeUnitTests` (`13` `@Test`) · `ADR-004`, `ADR-013`, `ADR-015`, `ADR-016` |
| `REQ-026` | Backup completo `.zip` (`data.json` + `banners/`) *(não implementada)* | 13 — Backup | Planejado no Roadmap (§2 item 1 de `10_ROADMAP.md`) | — | — | — |
| `REQ-027` | Camada de entrada estruturada (`contractVersion = 1`: `TASK` + `subtasks`, `CHECKLIST`, `APPOINTMENT`, `EVENT`, `PROJECT`, `PERSONAL_GOAL`, `TRANSACTION`, ações `CREATE`/`UPDATE`/`UPSERT`, `previewEnvelope` dry-run) | 14 — Ingestão Interna | `StructuredInputModels.kt`, `StructuredInputGateway.kt`, `StructuredInputProcessor.kt`, `NexusRepository.kt` (`previewStructuredInput*` / `processStructuredInput*`) | Tabelas oficiais Room `v6` (`tasks`, `subtasks`, `appointments`, `events`, `projects`, `personal_goals`, `transactions`) + `audit_history` (`STRUCTURED_INPUT_OP`) | `14_CONTRATO_*` | `StructuredInputUnitTests` (`10` `@Test`) · `ADR-017`, `ADR-018` |
| `REQ-028` | Ponte opcional ChatGPT → `NEXUS-INBOX` (GitHub privado somente-leitura, PAT em Keystore `AES/GCM`, prévia obrigatória, confirmação manual, resumo sem categorias vazias e `InboxSyncWorker` opcional) | 15 — Ponte Inbox | `InboxModels.kt`, `InboxStores.kt`, `GitHubInboxRemoteDataSource.kt`, `InboxRepository.kt`, `InboxSyncWorker.kt`, `NexusInboxScreen.kt`, `MoreSettingsBackupDiagnosticsScreens.kt`, `MainActivity.kt` (`SubRoute.InboxBridge`) | `SharedPreferences` (`nexus_inbox_config_prefs`, `nexus_inbox_secret_prefs` + Keystore `AES/GCM`) + `audit_history` (`INBOX_IMPORT_LOG`) + tabelas Room via `REQ-027` | `14_CONTRATO_*` | `InboxBridgeUnitTests` (`3` `@Test`) · `ADR-018` |


---

## 2. Rastreabilidade de defeitos transversais e validação

### BUG-001 — Preservação de navegação e edição durante rotação

| Campo | Registro |
|---|---|
| Natureza | Defeito funcional transversal, não requisito de módulo isolado |
| Áreas envolvidas | Shell/navegação (MainActivity.kt, estado de NexusAppRoot), editores e formulários em diferentes módulos |
| Estado documentado | **EM ABERTO — relato de persistência após o lote visual/adaptativo** |
| Evidência mais recente | Relato do usuário após instalar/testar a versão visual: ao girar celular/tablet em diferentes abas, inclusive durante edição de tarefa já existente, o app pode retornar à tela inicial |
| Causa | Ainda não confirmada integralmente |
| Relação com REQ | Afeta a continuidade operacional dos fluxos de REQ-001 e de módulos que usam editores; não é um novo requisito funcional de domínio |
| Investigação | Rastrear estado de rota, aba, editor ativo, entidade em edição, rascunhos, seleções, diálogos e intents na recriação da Activity |
| Validação exigida | Abrir edição existente e criação em diferentes módulos; alterar campos; girar em etapas diferentes; confirmar permanência na tela, nos dados e no progresso; repetir em celular e tablet |
| Exclusões | Não considerar layout adaptativo, build verde ou teste unitário isolado como prova de correção |

**Regra:** qualquer implementação de correção deve atualizar esta matriz com arquivos/componentes reais, estado de implementação, testes efetivamente executados e evidência. Só marcar como resolvido/validado após teste funcional em dispositivo cobrindo o fluxo.


## 3. Lacunas e falhas transversais adicionais

### GAP-001 — Recorrência de Compromissos e Eventos (REQ-009)

| Campo | Registro |
|---|---|
| Estado | **NÃO IMPLEMENTADA** |
| Evidência | `02_ESPECIFICACAO_PRODUTO.md` e `07_ESTADO_ATUAL.md` classificam `REQ-009` como não implementada; `ADR-008` adia a expansão além de tarefas. |
| Impacto | Não é possível criar uma série recorrente de compromisso/evento, projetar ocorrências e gerenciar exceções/lembretes de forma integrada. |
| Entrada estruturada | `contractVersion = 1` não suporta regras recorrentes novas para `TASK`, `APPOINTMENT` ou `EVENT`. |
| Evidência prática | A corrida semanal “A Vida é um Corre” não pôde ser cadastrada como compromisso recorrente no povoamento de 26/11/2026. |
| Próximo passo | Especificar domínio, exceções, projeção, notificações, backup e extensão versionada da Inbox; ver `15_REGISTRO_LACUNAS_E_FALHAS.md`. |

### GAP-002 — Recorrência ausente no contrato de entrada estruturada

A ponte aceita entidades únicas `APPOINTMENT` e `EVENT`, mas não cria uma regra recorrente. A referência opcional `TASK.recurrenceId` não cria uma regra nova. A limitação deve ser resolvida junto de `GAP-001`, sem simular séries por múltiplos registros únicos.

### DOC-001 — Exemplo de checklist divergente do DTO real

O exemplo JSON de `docs/14_CONTRATO_ENTRADA_ESTRUTURADA_ETAPA3.md` usava `isDone`/`orderIndex`, mas o contrato/código atual usa `isCompleted`/`sortOrder`. **Corrigido nesta atualização documental**; manter fixture/teste para evitar regressão.
