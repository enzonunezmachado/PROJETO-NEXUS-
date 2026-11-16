# NEXUS — Estado Real do Sistema (`NEXUS_CURRENT_STATE`)

> **Finalidade:** inventário factual, verificado diretamente no código-fonte atual do repositório `PROJECT_NEXUS` (sem suposições e sem considerar documentação antiga como verdade automática).
> **Data de referência da auditoria:** codebase atual em disco (`versionCode = 7`, `versionName = "5.2.0"`, `DATABASE_VERSION = 5`, `CURRENT_FORMAT_VERSION = 5`).

---

## 1. Critérios de Classificação

| Status | Significado |
| --- | --- |
| `IMPLEMENTADA` | Funcionalidade existe de ponta a ponta no código atual (banco/domínio/repositório/UI), operando conforme a proposta base. |
| `PARCIALMENTE IMPLEMENTADA` | Existe parte funcional, porém faltam subfluxos, telas, filtros, testes ou comportamentos esperados. |
| `NÃO IMPLEMENTADA` | Não existe no código atual (apenas planejada em especificação ou citada como evolução futura). |
| `INCONSISTENTE` | Funciona com divergências entre módulos, dados exibidos, regras de negócio, persistência ou UI/documentação. |
| `NÃO FOI POSSÍVEL VERIFICAR` | Exige compilação Android/Gradle/KSP, execução de JUnit em JVM ou execução em dispositivo/emulador Android (ausentes neste container). |

---

## 2. Resumo Executivo da Baseline Confirmada

| Dimensão | Estado Atual Confirmado no Código | Fonte |
| --- | --- | --- |
| Identificação | `applicationId = "com.nexus.app"`, `versionCode = 7`, `versionName = "5.2.0"` | `app/build.gradle.kts` |
| SDK / Linguagem | `minSdk = 26`, `targetSdk = 35`, `compileSdk = 35`, Kotlin `2.1.0`, Java `17` | `app/build.gradle.kts`, `gradle/libs.versions.toml` |
| UI | Jetpack Compose BOM `2024.12.01`, Material 3, navegação por estado em `NexusAppRoot` | `MainActivity.kt`, `NexusTheme.kt` |
| Persistência | Room `2.6.1`, `DATABASE_VERSION = 5`, `25` entidades, `25` DAOs, `19` enums, `4` migrações (`1->2`, `2->3`, `3->4`, `4->5`), `exportSchema = true` | `NexusDatabase.kt`, `NexusMigrations.kt` |
| Backup | `CURRENT_FORMAT_VERSION = 5`, JSON v1–v5 transacional via SAF + CSV 15 colunas + frases TXT/JSON | `BackupModels.kt`, `BackupManager.kt` |
| Motor de Agenda / Regras | `AgendaEngine`, `FinanceRules`, `ReminderRules`, `BackNavigationRules` em `domain/rules/` | `app/src/main/java/com/nexus/app/domain/rules/` |
| Notificações e Widgets | `AlarmManager` + `WorkManager` (4 canais, 3 receivers) e `4` widgets Glance responsivos | `notifications/*`, `widgets/*`, `AndroidManifest.xml` |
| Permissões | `POST_NOTIFICATIONS`, `RECEIVE_BOOT_COMPLETED`, `SCHEDULE_EXACT_ALARM`; **sem `INTERNET`**; `allowBackup="false"` | `AndroidManifest.xml` |
| Testes no repositório | 6 arquivos JUnit (`NexusPhase1..6UnitTests.kt` + `NexusStage1UnitTests.kt`), **0 `androidTest`** | `app/src/test/java/com/nexus/app/` |
| Pasta `app/schemas/` | **Ausente no disco**, apesar de `exportSchema = true` e `ksp room.schemaLocation` | Estrutura de pastas |

---

## 3. Situação Detalhada por Módulo e Capacidade

### 3.1 Arquitetura, Navegação e Shell Principal

| Item | Status | Evidência e Situação Real |
| --- | --- | --- |
| Shell Compose (`MainTab`, `SubRoute`, `ActiveEditor`) | `IMPLEMENTADA` | `MainActivity.kt` gerencia 5 abas (`HOME`, `AGENDA`, `TASKS`, `FINANCE`, `EVENTS`), 10 sub-rotas e 14 editores/modais por estado. |
| `BackHandler` no `NexusAppRoot` | `INCONSISTENTE` | `BackHandler` existe (`MainActivity.kt:250-271`), porém passa `editorHasUnsavedChanges = false` fixo (`MainActivity.kt:258`), anulando `BackAction.CONFIRM_DISCARD_EDITOR` ao pressionar Voltar do sistema. Além disso, `DashboardCustomization` (`MainActivity.kt:547`) e `RoutineAssistant` (`MainActivity.kt:613`) têm `onBack` apontando para `SubRoute.None` em vez de retornar ao ponto de origem quando abertos via `MoreMenu`. |
| Manifesto `enableOnBackInvokedCallback` | `IMPLEMENTADA` | `android:enableOnBackInvokedCallback="true"` presente em `AndroidManifest.xml:12`. |
| Dependência `navigation-compose` | `INCONSISTENTE` | Declarada em `app/build.gradle.kts:65` e `libs.versions.toml:9,30` (`2.8.5`), sem nenhum uso no código. |
| Identidade visual N próprio e Splash | `NÃO IMPLEMENTADA` | Símbolo vetorial N específico, ícone adaptativo dedicado e `androidx.core:core-splashscreen` não existem (`NexusEnums`/Etapa 6 pendente). |
| `QuickAddChooser` simplificado (4 opções + "Mais detalhes") | `PARCIALMENTE IMPLEMENTADA` | `QuickAddChooserSheet` existe (`EntityEditors.kt`), mas ainda não segue o formato enxuto de 4 opções descritas na Etapa 6 nem expansão "Mais detalhes" nos formulários. |

---

### 3.2 Dashboard / Tela Inicial (`HomeScreen`)

| Item | Status | Evidência e Situação Real |
| --- | --- | --- |
| Blocos configuráveis (`DashboardBlockType`, 9 blocos) | `IMPLEMENTADA` | `HomeScreen.kt` renderiza dinamicamente blocos habilitados/ordenados via `personalization_settings` e `dashboardBlocksJson`. |
| Saudação personalizada e formato de data | `IMPLEMENTADA` | `GreetingBlock` usa `DateTimeUtils.formatGreeting` e `formatDateByPattern` com 4 formatos (`NexusDateFormat`). |
| Frase diária offline | `IMPLEMENTADA` | `DailyQuoteBlock` exibe frase fixa por `dateIso` sem repetição imediata quando `>= 2` frases ativas. |
| Resumo visual com gráfico de anel central de tarefas | `NÃO IMPLEMENTADA` | Hoje `HomeScreen` exibe 3 `MetricTile` numéricos (`Pendentes`, `Concluídas`, `Atrasadas`) e listas até `maxItems`. Não há gráfico de anel de tarefas. |
| Visão de tarefas do mês + alternância mês anterior/atual/próximo | `NÃO IMPLEMENTADA` | Não há seletor mensal de tarefas nem contagem de status `Em andamento` destacada no Dashboard. |
| Resumo financeiro no Dashboard | `INCONSISTENTE` | Usa `MonthlyFinanceSummary` (Etapa 1 corrigida para mês corrente), porém ainda exibe cartões + barras simples em vez do gráfico de anel financeiro + seletor unificado especificados para a fase visual. |
| Indicadores de projetos e objetivos no Dashboard | `PARCIALMENTE IMPLEMENTADA` | Bloco `GOALS` existe; não há resumo visual consolidado de projetos + objetivos com anéis/barras unificados. |
| Próximos compromissos e eventos | `IMPLEMENTADA` | Blocos `TODAY_AGENDA` e `UPCOMING_EVENTS` operacionais. |
| Bloco Mercado (Ibovespa, Dólar, EUR/BTC + Sparkline) | `NÃO IMPLEMENTADA` | Sem entidades, sem rede e sem `DashboardBlockType.MARKET`. |
| Alertas de atenção real | `PARCIALMENTE IMPLEMENTADA` | Mostra tarefas atrasadas e próximos pagamentos, sem um bloco dedicado unificado de alertas críticos priorizados. |

---

### 3.3 Tarefas, Subtarefas e Recorrência

| Item | Status | Evidência e Situação Real |
| --- | --- | --- |
| CRUD de tarefas + prioridades + 6 status + Kanban | `IMPLEMENTADA` | `TasksAndProjectsScreen.kt` com abas `LIST`, `KANBAN`, `OVERDUE`, `COMPLETED`, `PROJECTS` sobre a mesma tabela `tasks`. |
| Rótulo `"Quando"` para `dueAt` | `IMPLEMENTADA` | Aplicado na Etapa 2 (`EntityEditors.kt`). |
| Subtarefas (`subtasks`) como checklist | `INCONSISTENTE` | `TaskEditorSheet` possui checklist funcional, mas alternar uma subtarefa já existente dispara `onToggleSubtask(persisted)` imediatamente no Room (`EntityEditors.kt:690-693, 705-708`), persistindo antes de o usuário clicar em "Salvar" e contrariando o descarte de alterações. |
| Botão `"Registrar gasto"` na tarefa (`transactions.taskId`) | `INCONSISTENTE` | Funcional na criação (`MainActivity.kt:854-882`, `AgendaEngine.suggestExpenseCategoryId`, badge `"Gasto: R$ X"` na tarefa). Contudo: (1) clicar em `"Registrar gasto"` **salva a tarefa imediatamente** antes mesmo de confirmar o lançamento; (2) `TransactionWithDetails` (`DomainModels.kt:56-62`) não inclui `task: TaskEntity?` e `TransactionRowCard` (`FinanceScreen.kt:731-739`) não exibe o vínculo com a tarefa no Financeiro. |
| Carregar e preservar lembrete ao reabrir Tarefa | `IMPLEMENTADA` | `ReminderRules.resolveEditorReminderMinutes` e `resolveReminderSyncAction` integrados. |
| Sistema de recorrência genérica de tarefas | `NÃO IMPLEMENTADA` | Não existem `recurrences`, `recurrence_exceptions`, `tasks.recurrenceId` nem `tasks.occurrenceDateIso`. |

---

### 3.4 Projetos

| Item | Status | Evidência e Situação Real |
| --- | --- | --- |
| CRUD de projetos, prazo, status e vínculo com objetivos (`goalId`) | `IMPLEMENTADA` | `ProjectEntity`, `ProjectEditorSheet`, aba `PROJECTS` e integração em `PlanningGoalsScreen`. |
| Progresso automático por tarefas (`ProjectWithProgress`) e custos | `IMPLEMENTADA` | Contabiliza `completedTasks / totalTasks`, custos estimados/pagos de `transactions` e `installment_purchases`. |
| Visibilidade na Agenda (quando possui `deadlineAt`) | `IMPLEMENTADA` | Expandido por `AgendaEngine` como `AgendaOccurrenceKind.PROJECT`. |

---

### 3.5 Agenda e Compromissos (`AgendaScreen`, `AgendaEngine`)

| Item | Status | Evidência e Situação Real |
| --- | --- | --- |
| Motor puro `AgendaEngine` + `DateRange` | `IMPLEMENTADA` | Expande 6 fontes (`appointments`, `tasks`, `events`, `transactions`, `credit_card_statements`, `projects`) em `AgendaOccurrence`. |
| Visões Dia, Semana, Mês e Lista | `PARCIALMENTE IMPLEMENTADA` | `DAY`, `WEEK` (seg–dom com setas e intervalo) e `LIST` funcionam; em `MONTH` (`AgendaScreen.kt:113-114`), o calendário marca dias ocupados, porém a lista abaixo mostra **somente** `selectedDate` (sem visão resumida do mês ou destaque diferenciado de período). |
| Filtros por tipo (chips), categoria, busca e `"Mostrar já pagos"` | `IMPLEMENTADA` | Presente em `AgendaScreen` e `AgendaFilters`; compras com `statementId != null` ficam ocultas e apenas faturas não pagas com total `> 0` aparecem por padrão. |
| Eventos e compromissos multi-dia | `IMPLEMENTADA` | `AgendaEngine` expande todos os dias entre `startDate` e `endDate`, exibindo faixa "dia inteiro" no topo do dia. |
| Compromisso vinculado a Evento (`appointments.eventId`) | `PARCIALMENTE IMPLEMENTADA` | Coluna e FK existem (`MIGRATION_4_5`), e o campo aparece no `AppointmentEditorSheet` e na `AgendaEngine`; falta seção dedicada listando e criando compromissos vinculados dentro do detalhe do Evento (`EventsScreen`). |
| Código legado paralelo (`observeUnifiedAgenda` / `AgendaItem`) | `INCONSISTENTE` | `NexusRepository` ainda mantém `observeUnifiedAgenda()` e `AgendaItem` lado a lado com `AgendaEngine`/`AgendaOccurrence`; `observeDailyBriefingSnapshot` e `observeNightReviewSnapshot` ainda dependem do legado. |
| Detecção de conflitos na Busca Global | `INCONSISTENTE` | `observeGlobalSearch` (`NexusRepository.kt:785`) chama `computeAppointmentConflicts(matchedAppointments)` apenas sobre o subconjunto filtrado pela busca, podendo esconder conflitos reais com outros compromissos não incluídos no termo buscado. |
| Recorrência de compromissos e eventos | `NÃO IMPLEMENTADA` | Não implementada (planejada para Etapa 4). |

---

### 3.6 Eventos (`EventsScreen`)

| Item | Status | Evidência e Situação Real |
| --- | --- | --- |
| CRUD de eventos, status e exibição de custos | `IMPLEMENTADA` | `EventEntity`, `EventEditorSheet`, `EventsScreen` (filtros `Próximos`, `Ingresso pendente`, `Todos`). |
| Obrigação financeira de ingresso & parcelamento | `INCONSISTENTE` | `saveEvent` (`NexusRepository.kt:1484, 1539`) grava literalmente `FinancialStatus.OVERDUE` no banco para obrigações com vencimento passado, contradizendo o restante do app (`saveTransaction` e `FinanceRules`), onde `OVERDUE` é derivado dinamicamente na leitura a partir de `PENDING`/`PLANNED`. Além disso, `EventWithDetails` e `EventEntity` carregam `ticketFeesCents` sempre visível/calculado, exigindo revisão para manter apenas o preço real sem incentivar taxas fictícias. |
| Modo de local (`IN_PERSON`, `ONLINE`, `TBD`) e local totalmente opcional | `NÃO IMPLEMENTADA` | `EventEntity` ainda usa campos planos (`locationName`, `address`, `city`, `state`) sem `locationMode`. |
| Banner do evento (`PickVisualMedia` + arquivo JPEG interno) | `NÃO IMPLEMENTADA` | Não existe `events.bannerPath` nem seletor de imagem. |
| Seção unificada de vínculos no detalhe do Evento | `PARCIALMENTE IMPLEMENTADA` | Detalhe mostra tarefas e lançamentos vinculados, mas ainda não lista/cria `appointments` vinculados (`appointments.eventId`). |
| `TransactionEditorSheet` ao criar lançamento via Evento | `INCONSISTENTE` | Quando `initialEventId != null` e `tx == null`, pré-seleciona `eventCostType = EventCostType.OTHER` (`FinanceEditors.kt:136-138`) em vez de exigir/deixar explícito ou contextualizar corretamente se veio de tarefa ou evento. |
| Atalhos de mapa (`geo:` / Google Maps URL) | `IMPLEMENTADA` | Funcionando via `Intent.ACTION_VIEW` em `EventsScreen`. |

---

### 3.7 Financeiro (`FinanceScreen`, `FinanceEditors`, `FinanceRules`)

| Item | Status | Evidência e Situação Real |
| --- | --- | --- |
| Totais mensais (`FinanceRules.computeMonthlyFinanceSummary`) | `IMPLEMENTADA` | Filtra receitas/despesas pelo mês alvo (`dueAt ?: occurredAt`) e mantém `overdueExpenseCents` global; usado por Financeiro, Home e Widget. |
| Lançamentos, categorias, métodos de pagamento, saldo declarado | `IMPLEMENTADA` | CRUD completo com `declaredBalanceCents` manual nunca sobrescrito. |
| Cartões, fechamento/vencimento de fatura, pagamento sem duplicar despesa | `IMPLEMENTADA` | `payCreditCardStatement` liquida transações da fatura sem gerar segunda despesa; `resolveCreditCardLimitBreakdown` calcula disponível real e mostra o informado quando positivo e diferente. |
| Parcelamentos (`installment_purchases`), recorrentes (`recurring_rules`), empréstimos (`loans`), orçamentos (`budgets`) e metas financeiras (`financial_goals`) | `IMPLEMENTADA` | Todos possuem entidades, DAOs, editores e abas no Financeiro. |
| Botão rápido de liquidação no cartão de "Próximo vencimento" e na Busca Global | `INCONSISTENTE` | `FinanceScreen.kt:371` e `MoreSettingsBackupDiagnosticsScreens.kt:336` passam `onToggleSettle = {}` (no-op ao tocar no ícone de check). |
| Seletor `< mês/ano >` no topo do Financeiro | `NÃO IMPLEMENTADA` | Painel financeiro mostra apenas o mês atual fixo. |
| Gráfico de rosca (`DonutChart`) com interatividade slice→lista e alternância Pagas / Pagas + previstas | `NÃO IMPLEMENTADA` | Hoje há apenas barras horizontais simples em `CategorySpendCard` (`FinanceScreen.kt:522-567`). |
| Gráfico de barras 6 meses (`BarGroupChart`) com Realizado/Previsto, toque no mês e "Resultado do mês" | `NÃO IMPLEMENTADA` | Hoje `MonthlyCashflowChartCard` (`FinanceScreen.kt:440-519`) renderiza apenas barras estáticas de Realizado sem interação, alternância ou saldo líquido textual. |
| Orçamentos ordenados por % com cores 80%/100% + texto `"excedeu R$ X"` | `PARCIALMENTE IMPLEMENTADA` | `BudgetProgress` já calcula `exceededCents` e `usedFraction`, mas `ProgressBarRow` reutilizável em `ui/components/charts/` e ordenação/regra visual da Etapa 3 ainda não foram implementadas. |
| Previsão de 30 dias (próximos vencimentos previstos) | `NÃO IMPLEMENTADA` | Não existe cartão nem regra pura de previsão de 30 dias. |
| Biblioteca compartilhada `ui/components/charts/` + tokens `accentNeon` / paleta 8 cores | `NÃO IMPLEMENTADA` | Pasta `ui/components/charts/` e tokens `accentNeon` ainda não existem em `NexusTheme.kt`. |

---

### 3.8 Mercado Financeiro

| Item | Status | Evidência e Situação Real |
| --- | --- | --- |
| Módulo de cotações (`market_quotes`, `market_history`, rede `java.net`, AwesomeAPI, brapi opcional, `Sparkline`) | `NÃO IMPLEMENTADA` | Nenhuma entidade, DAO, serviço HTTP, token em Configurações ou bloco de Dashboard existe atualmente. |

---

### 3.9 Objetivos Pessoais (`PlanningGoalsScreen`)

| Item | Status | Evidência e Situação Real |
| --- | --- | --- |
| Objetivos (`personal_goals`), marcos (`goal_milestones`), histórico de progresso (`goal_progress_logs`) | `IMPLEMENTADA` | CRUD completo com modos `AUTO` e `MANUAL`. |
| Integração com Tarefas (`tasks.goalId`) e Projetos (`projects.goalId`) | `IMPLEMENTADA` | `PlanningGoalsScreen` e `NexusRepository` agregam tarefas/projetos vinculados no cálculo `AUTO` e filtros `GoalStatusFilter`. |

---

### 3.10 Assistente de Rotina (`RoutineAssistantScreen`)

| Item | Status | Evidência e Situação Real |
| --- | --- | --- |
| Briefing Diário, Revisão Noturna, histórico e adiamento em 1 toque | `PARCIALMENTE IMPLEMENTADA` | UI e fluxo principal implementados, inclusive com marcação de revisão em `briefing_history`. |
| Consistência de cálculos no Briefing Diário e Revisão Noturna | `INCONSISTENTE` | (1) `observeDailyBriefingSnapshot` (`NexusRepository.kt:920-930`) coleta transações sem filtrar `TransactionType.EXPENSE` nem excluir compras de fatura (`statementId != null`); (2) `observeNightReviewSnapshot.tomorrowBills` (`NexusRepository.kt:1006-1012`) não filtra `TransactionType.EXPENSE`; (3) `sendTestNotification` (`NexusRepository.kt:2581`) conta apenas compromissos cuja data inicial é hoje (`epochToLocalDate(startAt) == today`), ignorando compromissos multi-dia em curso. |

---

### 3.11 Notificações

| Item | Status | Evidência e Situação Real |
| --- | --- | --- |
| Canais, agendador `AlarmManager`, ações rápidas, boot receiver e `WorkManager` | `IMPLEMENTADA` | `NexusNotificationChannels`, `NexusNotificationScheduler`, `NexusBroadcastReceivers`, `NexusReminderSyncWorker`. |
| Proteção contra lembretes cancelados ou legados (`minutesBefore < 0`) | `IMPLEMENTADA` | `ReminderRules.isEffectivelyCancelled`, `isQueueVisible` e `isSchedulableOrFirable` aplicados nos fluxos principais. |
| Agendamento de ocorrências recorrentes (janela 30 dias) | `NÃO IMPLEMENTADA` | Depende da Etapa 4 (recorrência genérica). |

---

### 3.12 Widgets Android (`widgets/*`)

| Item | Status | Evidência e Situação Real |
| --- | --- | --- |
| 4 widgets Glance responsivos (`SMALL`, `MEDIUM`, `LARGE`) + configuração por ID + ações/deep-links | `IMPLEMENTADA` | `MyDayGlanceWidget`, `MyTasksGlanceWidget`, `FinanceGlanceWidget`, `NexusSummaryGlanceWidget`, `NexusWidgetConfigActivity`, `NexusWidgetUpdater` com `Mutex`. |
| Adaptação a tema claro/escuro nos widgets | `INCONSISTENTE` | `NexusWidgetTheme.kt:43-57` define apenas paleta escura fixa (`Background = 0xFF050505`), sem variante clara ou sincronização com o tema do sistema/app. |
| Uso unificado de `AgendaEngine` nos widgets | `PARCIALMENTE IMPLEMENTADA` | Widgets leem snapshots em `NexusRepository`; após Etapa 4 deverão refletir recorrências via `AgendaEngine`. |

---

### 3.13 Configurações, Personalização, Frases, Backup e Diagnóstico

| Item | Status | Evidência e Situação Real |
| --- | --- | --- |
| Configurações (nome, tema claro/escuro, categorias) | `IMPLEMENTADA` | `SettingsScreen` operacional para os dois temas. |
| Personalização do Dashboard e Biblioteca de Frases (JSON/TXT) | `IMPLEMENTADA` | `DashboardCustomizationScreen` e `QuoteLibraryScreen` funcionais, sem frases pré-carregadas fictícias. |
| Backup JSON (`CURRENT_FORMAT_VERSION = 5`) e CSV financeiro | `INCONSISTENTE` | O mecanismo interno já está na versão `5` (com `appointment.eventId` e `transaction.taskId`), porém os textos na UI de `MoreSettingsBackupDiagnosticsScreens.kt:193, 1070, 1108` ainda informam `"JSON v4"` / `"v1, v2, v3 e v4"`. |
| Backup completo `.zip` (JSON + `banners/`) | `NÃO IMPLEMENTADA` | Planejado para Etapa 5. |
| Diagnóstico (contagens das 25 tabelas e `error_logs`) | `IMPLEMENTADA` | `DiagnosticsScreen` exibe contagens reativas via `observeDatabaseTableCounts()` e logs locais de `GlobalErrorHandler`. |

---

## 4. Testes, CI e Documentação: Bugs e Lacunas Encontradas

| Arquivo / Área | Status | Problema Real Confirmado no Código |
| --- | --- | --- |
| `NexusPhase6UnitTests.kt:360-403` | `INCONSISTENTE` (Quebrado) | Instancia `NexusBackupPayload(formatVersion = NexusBackupPayload.CURRENT_FORMAT_VERSION, ...)` (atualmente `5`) e executa `assertEquals(4, decoded.formatVersion)` na linha `403` — falha garantida em `testDebugUnitTest`. |
| `NexusStage1UnitTests.kt:340` | `INCONSISTENTE` (Erro de compilação) | Referencia `ReminderStatus.SNOOZED`, porém o enum `ReminderStatus` (`NexusEnums.kt:194-204`) possui apenas `ACTIVE, FIRED, DISMISSED, CANCELLED` — causa erro de compilação Kotlin em `compileDebugUnitTestKotlin`. |
| Testes da Etapa 2 (`AgendaEngine` e Backup v5) | `NÃO IMPLEMENTADA` | Não existe suíte de testes cobrindo `AgendaEngine.expand` (multi-dia, faturas, `showPaid`, filtros), `suggestExpenseCategoryId` nem round-trip `appointment.eventId` / `transaction.taskId` no Backup v5. |
| `.github/workflows/android-build.yml` | `PARCIALMENTE IMPLEMENTADA` | Executa apenas `gradle :app:assembleDebug` (`android-build.yml:36`). Não executa `testDebugUnitTest`, não possui assinatura persistente de release e não publica `app/schemas` como artefato. Além disso, `gradlew` e `gradlew.bat` não existem na raiz (apenas `gradle/wrapper/`). |
| `NEXUS_ARCHITECTURE_AND_PROGRESS.md` | `INCONSISTENTE` | Documento antigo desatualizado: ainda aponta `versionName = "5.0.0"`, `versionCode = 5`, Room `v4`, ausência de `BackHandler` e `observeUnifiedAgenda` como motor único. |

---

## 5. Itens Não Verificados Neste Ambiente (`NÃO FOI POSSÍVEL VERIFICAR`)

Os itens abaixo dependem de JDK + Android SDK + Gradle ou emulador/dispositivo físico Android (inexistentes neste ambiente) e **não podem ser declarados como aprovados**:

1. Compilação efetiva `:app:compileDebugKotlin`, `:app:compileDebugUnitTestKotlin` e `:app:assembleDebug`.
2. Execução de testes unitários JUnit (`testDebugUnitTest`).
3. Geração dos arquivos JSON de schema do Room pelo KSP (`app/schemas/`) e validação das migrações reais `1->2->3->4->5` no SQLite.
4. Fluxos interativos de dispositivo: seletor SAF (JSON/CSV), disparos exatos de `AlarmManager` em modo Doze, `NexusBootReceiver`, ações de notificação, deep-links `nexus://open/...` e renderização real dos 4 widgets Glance em launcher Android.
