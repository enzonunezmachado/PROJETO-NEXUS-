# NEXUS — Mapa de Rastreabilidade e Auditoria de Fidelidade do Espelho Web

Este documento registra o espelhamento **visual, funcional, estrutural, de domínio e adaptativo** entre o aplicativo Android nativo (`app/` — Kotlin / Jetpack Compose / Material 3 / Room) e o espelho web estático em HTML, CSS e JavaScript (`web-mirror-pilot/`).

---

## 1. Taxonomia Oficial da Auditoria de Fidelidade

Classificação utilizada nesta auditoria e cruzada com o atributo DOM `data-nexus-status` (`reproduzido` / `parcial` / `simulado` / `não aplicável`):

- **`FIEL`**: Estrutura visual, regras de negócio, fluxos, estados, ordenações e interações reproduzidos com precisão equivalente ao Android.
- **`PARCIAL`**: Fluxo e interface reproduzidos com adaptações inerentes ao navegador ou simplificações controladas documentadas (ex.: persistência em memória em vez de Room SQLite físico; ausência de geração automática de parcelamento de ingresso em `EventEditorSheet`).
- **`DIVERGENTE`**: Havia diferença relevante corrigida nesta auditoria ou residual documentada.
- **`AUSENTE`**: Funcionalidade existente no Android que ainda não existe no espelho web (se aplicável).
- **`SIMULADO`**: Dependente de rede HTTP externa ou APIs exclusivas do sistema operacional Android (`AlarmManager`, `WorkManager`, Jetpack Glance launcher widgets, AwesomeAPI/CoinGecko/Yahoo/B3), operando no espelho web com dados e ações simuladas claramente sinalizadas.
- **`NÃO APLICÁVEL`**: Recursos exclusivos de hardware/SO Android sem equivalente em SPA estática (ex.: permissão `SCHEDULE_EXACT_ALARM`, `BootCompletedReceiver`, atalho nativo de launcher via `AppWidgetManager`).

---

## 2. Arquitetura Modular do Espelho Web

| Arquivo Web | Papel Arquitetural | Referências Android (`app/`) |
|---|---|---|
| `web-mirror-pilot/index.html` | Casca HTML estática (`#nexus-app-shell` isolado de `#pilot-lab-bar`) com 4 viewports e alternância Dark/Light | `MainActivity.kt`, `Theme.kt` |
| `web-mirror-pilot/tokens.css` | Tokens Dark (`#050505`), Light **Grafite Suave** (`#D4D7DC`) e os 8 `ColoredBorderRole` proporcionais | `ui/theme/Theme.kt`, `ui/theme/Color.kt`, `ColoredBorderRole` |
| `web-mirror-pilot/styles.css` | Breakpoints adaptativos (`390px`, `768px`, `1024px`, fluido), pesos two-pane da Agenda (`0.45fr 0.55fr` / `0.54fr 0.46fr` no modo Semana), Kanban, calendários, sheets e diálogos | `MainActivity.kt`, `NexusComponents.kt`, `HomeScreen.kt`, `AgendaScreen.kt`, `MarketScreen.kt` |
| `web-mirror-pilot/data/pilot-data.js` | Store relacional de Gabriel (`2026-11`), entidades normalizadas `taskRecurrences` + `recurrenceExceptions`, `RecurrenceEngine`, `TaskRules`, `AgendaEngine`, `FinanceRules`, `GoalProgressRules` e catálogo de 12 ativos | `NexusEntities.kt`, `DomainModels.kt`, `RecurrenceEngine.kt`, `TaskRules.kt`, `AgendaEngine.kt`, `FinanceRules.kt`, `GoalProgressRules.kt`, `MarketPresentation.kt`, `NexusDatabase.kt` |
| `web-mirror-pilot/components/primitives.js` | Wordmark `Kinetik Cut + N-1` (`318x64`), `TaskRowCard`, `ProjectCard`, `AgendaOccurrenceRow` (4 ícones/marcadores por origem, materialização virtual, conflito de horário, sinalização `+`/`-`), `MonthYearSelectorBar`, banners | `NexusComponents.kt`, `AgendaScreen.kt`, `TasksAndProjectsScreen.kt` |
| `web-mirror-pilot/components/charts.js` | `TaskRingChart`, `BarGroupChart`, `DonutChart`, `ProgressBarRow` e `MarketHistoryChartSvg` interativo | `ui/components/charts/*.kt`, `MarketScreen.kt` |
| `web-mirror-pilot/screens/home.js` | `HomeScreen` configurável (9 blocos) com ordenação/visibilidade/densidade compacta, métricas no anel e alertas dinâmicos | `HomeScreen.kt` |
| `web-mirror-pilot/screens/tasks.js` | `TasksAndProjectsScreen`: 7 abas (`Hoje`, `Próximas`, `Atrasadas`, `Todas`, `Concluídas`, `Kanban` 6 colunas incluindo `Canceladas`, `Projetos`), agrupamento `TaskRules` (6 grupos) e filtros | `TasksAndProjectsScreen.kt`, `TaskRules.kt` |
| `web-mirror-pilot/screens/agenda.js` | `AgendaScreen`: visão inicial `Mês` (`MONTH`), 4 modos (`Dia`, `Semana`, `Mês`, `Lista`), `MonthCalendarGrid` (`S T Q Q S S D`), `HorizontalWeekBoard` sem duplicação de lista no mobile quando expandido, `AllDayEventBandCard`, `AgendaFilterBottomSheet` com categorias contextuais e toggle `Mostrar já pagos` restrito a `Tudo`/`Financeiro` | `AgendaScreen.kt`, `AgendaEngine.kt` |
| `web-mirror-pilot/screens/finance.js` | `FinanceScreen`: 5 abas (`Resumo`, `Lançamentos`, `Cartões`, `Parcelamentos & Recorrências`, `Orçamentos, Metas & Empréstimos`), projeção 30d e auditoria | `FinanceScreen.kt`, `NexusViewModel.kt` |
| `web-mirror-pilot/screens/events.js` | `EventsScreen` com filtros (`Próximos`, `Ingresso pendente`, `Todos`) e grid adaptativo | `EventsScreen.kt` |
| `web-mirror-pilot/screens/market.js` | `MarketScreen` (12 ativos, Watchlist, alertas `PRICE_ABOVE`/`PRICE_BELOW`) e `MarketAssetDetailScreen` (gráfico multi-faixa, conversão USD→BRL, two-pane) | `MarketScreen.kt`, `MarketAlertSheetsAndCards.kt`, `MarketAlertModels.kt` |
| `web-mirror-pilot/screens/planning.js` | `PlanningGoalsScreen` e `GoalDetailSheet` (marcos, logs de progresso, vínculo ao vivo com tarefas e projetos) | `PlanningGoalsScreen.kt` |
| `web-mirror-pilot/screens/routine-assistant.js` | `RoutineAssistantScreen` (Briefing Diário, Revisão Noturna com data dinâmica de amanhã, Notificações/Lembretes) | `RoutineAssistantScreen.kt` |
| `web-mirror-pilot/screens/more-subroutes.js` | `MoreScreen`, `GlobalSearchScreen`, `DashboardCustomizationScreen`, `QuoteLibraryScreen`, `SettingsScreen`, `BackupScreen` (v7 incluindo `taskRecurrences` e `recurrenceExceptions`), `DiagnosticsScreen` (tabelas Room equivalentes) e `WidgetsHubScreen` | `MoreSettingsBackupDiagnosticsScreens.kt`, `DashboardCustomizationAndQuotesScreens.kt` |
| `web-mirror-pilot/sheets/transaction-sheet.js` | `TransactionEditorSheet` com vínculo bidirecional a Cartão/Fatura, Evento e Tarefa | `FinanceEditors.kt` |
| `web-mirror-pilot/sheets/event-sheet.js` | `EventEditorSheet` com custos vinculados, `+ Adicionar custo` e `+ Nova tarefa` | `EventsScreen.kt` |
| `web-mirror-pilot/sheets/entity-sheets.js` | `TaskEditorSheet` (subtarefas, série recorrente completa com 6 frequências + máscara semanal + início/fim, `ReminderLeadTimeSelector`, atalho `Abrir evento`, despesas vinculadas clicáveis, `Concluir`/`Reabrir`), `AppointmentEditorSheet` (conflito ao vivo) e `ProjectEditorSheet` | `EntityEditors.kt` |
| `web-mirror-pilot/sheets/finance-extra-sheets.js` | Modais CRUD de Finanças, Planejamento, Mercado, Frases, Categorias e prévia Backup v7 | `FinanceEditors.kt`, `MarketAlertSheetsAndCards.kt`, `PlanningGoalsScreen.kt`, `DashboardCustomizationAndQuotesScreens.kt`, `MoreSettingsBackupDiagnosticsScreens.kt` |
| `web-mirror-pilot/app.js` | Motor unidirecional, `subRouteStack`, materialização virtual de ocorrências recorrentes, prompts `RecurringEditScope`, sugestão inteligente de categoria ao registrar gasto da tarefa, preservação de filtros/rascunhos entre tema e viewport | `MainActivity.kt`, `NexusViewModel.kt`, `NexusRepository.kt` |

---

## 3. Auditoria de Fidelidade por Módulo e Correções Aplicadas

### 3.1 Agenda (`AgendaScreen.kt`, `AgendaEngine.kt`, `RecurrenceEngine.kt`)

| Item / Regra | Origem Android | Classificação | Detalhamento |
|---|---|---|---|
| Visão padrão inicial | `AgendaFilterState(viewMode = MONTH)` | `FIEL` | Corrigido de `WEEK` para `MONTH` na inicialização da UI. |
| 6 fontes de ocorrências | `AgendaEngine.kt` (`APPOINTMENT`, `TASK`, `EVENT`, `TRANSACTION` +/–, `STATEMENT` > 0, `PROJECT` ativo/pausado) | `FIEL` | Inclui agora receitas (`INCOME`), faturas apenas com `totalCents > 0`, status `OVERDUE` dinâmico para transações e faturas, projetos sem prefixo `"Prazo: "` e exclusão de projetos `COMPLETED`/`ARCHIVED`. |
| Projeção virtual de recorrências | `AgendaEngine.kt` + `RecurrenceEngine.kt` | `FIEL` | Projeta ocorrências virtuais para todas as 6 frequências respeitando `recurrenceExceptions` (`SKIP`/`OVERRIDE`), exibindo ícone `repeat` neon sem sufixo `"(recorrência)"` e materializando ao tocar ou concluir checkbox. |
| Comportamento do modo `WEEK` | `AgendaScreen.kt` (`HorizontalWeekBoard` vs `SelectedDayPanel`) | `FIEL` | Em viewport de coluna única (`mobile` e `tablet-portrait`), `WEEK` expandido renderiza exclusivamente o `HorizontalWeekBoard` (sem duplicar `SelectedDayPanel`); colapsado exibe ocorrências do dia ou mensagem orientadora. Em `two-pane`, mantém proporções `0.45fr / 0.55fr` e `0.54fr / 0.46fr`. |
| `MonthCalendarGrid` e `DateNavigationBar` | `AgendaScreen.kt` | `FIEL` | Cabeçalhos semanais `S T Q Q S S D`, `"Semana DD/MM – DD/MM"`, dica `"Toque para voltar a hoje"` fora de hoje e instrução quando mês/semana está recolhido. |
| Filtros contextuais de categoria e `showPaid` | `AgendaFilterBottomSheet` em `AgendaScreen.kt` | `FIEL` | Lista de categorias se adapta ao `kindFilter` selecionado, limpa `categoryFilterId` incompatível ao trocar tipo, e exibe `"Mostrar já pagos"` apenas quando `kindFilter == null` ou `FINANCE`. |
| Renderização dos itens (`AgendaOccurrenceRow`) | `AgendaScreen.kt:AgendaOccurrenceRow` | `FIEL` | Checkbox quadrado para tarefas, ícone calendário para eventos, `R$` para faturas, bandeira para projetos; card elevado apenas para `APPOINTMENT` e `EVENT`; tachado apenas em tarefas concluídas/canceladas; valor financeiro com sinal `+`/`-`. |

### 3.2 Tarefas & Projetos (`TasksAndProjectsScreen.kt`, `TaskRules.kt`, `RecurrenceEngine.kt`, `EntityEditors.kt`)

| Item / Regra | Origem Android | Classificação | Detalhamento |
|---|---|---|---|
| Abas e agrupamento (`TaskRules`) | `TasksAndProjectsScreen.kt`, `TaskRules.kt` | `FIEL` | Corrigido `isTaskOverdue` considerando `hasDueTime` + horário atual (`nowTime`); aba `Todas` preserva `CANCELLED`; 6 grupos exatos (`Atrasadas`, `Hoje`, `Esta semana`, `Próximas`, `Sem prazo`, `Concluídas e canceladas`) com ordenação completa do Android. |
| Kanban completo (6 colunas) | `TasksAndProjectsScreen.kt:TasksKanbanView` | `FIEL` | Expandido de 5 para todas as 6 colunas incluindo `Canceladas` (`CANCELLED`), além de cartões com prazo contextual, projeto, prioridade e indicador `Atrasada`. |
| Ciclo de conclusão, reabertura e série recorrente | `NexusViewModel.kt`, `NexusRepository.kt`, `RecurrenceEngine.kt` | `FIEL` | Reabrir tarefa concluída move para `IN_PROGRESS` (não `NOT_STARTED`); concluir ocorrência recorrente materializa automaticamente a próxima ocorrência livre respeitando `SKIP`; diálogos `SAVE_TASK_SCOPE` (`Só esta ocorrência` vs `Toda a série`), `CONFIRM_RECURRENCE_RULE_CHANGE` e `DELETE_TASK_RECURRING` implementados. |
| `TaskEditorSheet` e vínculo financeiro | `EntityEditors.kt`, `NexusViewModel.suggestExpenseCategoryIdForTask` | `FIEL` | Seletor completo de recorrência (6 frequências, `CUSTOM_DAYS`, chips `Seg..Dom`, início, término sem fim/até data/máx. ocorrências), `ReminderLeadTimeSelector` (`Padrão`, `0`, `15`, `30`, `60`, `-1`), atalho `Abrir evento`, lista clicável de lançamentos vinculados, sugestão automática de categoria de despesa ao registrar gasto e botão `Concluir`/`Reabrir`. |
| `ProjectCard` e estados vazios | `TasksAndProjectsScreen.kt`, `NexusComponents.kt` | `FIEL` | Texto `"X/Y tarefas concluídas"`, botão `"Novo projeto"` e títulos contextuais por aba vazia (`Nenhuma tarefa agendada para hoje`, etc.). |

### 3.3 Demais Telas e Fluxos Transversais

| Tela / Fluxo | Origem Android | Classificação | Observações da Auditoria |
|---|---|---|---|
| Navegação (`MainTopBar`, `BottomNavigationBar`, `QuickAddFab`, `subRouteStack`) | `MainActivity.kt` | `FIEL` | 3 ações no topo (`Mercado`, `Pesquisa global`, `Mais`), 5 abas na ordem nativa, FAB oculto em subrotas e pilha de subrotas funcional. |
| `HomeScreen` (9 blocos personalizáveis) | `HomeScreen.kt` | `FIEL` | Ordem, visibilidade, flags por bloco, tema Dark/Grafite Suave, saudação, frase do dia e alerta dinâmico de conflitos de hoje. |
| `FinanceScreen` (5 abas + projeção 30d + CSV) | `FinanceScreen.kt`, `FinanceEditors.kt`, `FinanceRules.kt` | `FIEL` | Cartões, faturas, parcelamentos, recorrências financeiras, orçamentos, metas, empréstimos e auditoria com mutações reais em memória. |
| `EventsScreen` e `EventEditorSheet` | `EventsScreen.kt`, `NexusRepository.saveEvent` | `PARCIAL` | Filtros, custos vinculados e atalhos `+ Adicionar custo` / `+ Nova tarefa` estão fiéis; porém a geração automática de transação/parcelamento de ingresso ao salvar evento (`NexusRepository.saveEvent`) não é disparada automaticamente nesta versão web (custos são vinculados manualmente pelo fluxo de lançamentos). |
| `PlanningGoalsScreen` e `GoalDetailSheet` | `PlanningGoalsScreen.kt`, `GoalProgressRules.kt` | `FIEL` | Cálculo `AUTO`/`MANUAL`, marcos, logs e vínculos bidirecionais preservados. |
| `MarketScreen` e `MarketAssetDetailScreen` | `MarketScreen.kt`, `MarketAlertModels.kt` | `SIMULADO` | Corrigido divergência de enum de alertas (`ABOVE_OR_EQUAL`/`BELOW_OR_EQUAL` → `PRICE_ABOVE`/`PRICE_BELOW` alinhado a `MarketAlertModels.kt` e `pilot-data.js`). Rede/cotações são simuladas localmente. |
| `RoutineAssistantScreen` | `RoutineAssistantScreen.kt` | `PARCIAL` | Briefing Diário e Revisão Noturna fiéis (corrigida data hardcoded `'24/11/2026'` para cálculo dinâmico a partir de `todayIso`); agendamento de notificações na 3ª aba é `SIMULADO`. |
| `BackupScreen` (v7) e `DiagnosticsScreen` | `MoreSettingsBackupDiagnosticsScreens.kt` | `PARCIAL` | Backup JSON v7 agora inclui explicitamente `taskRecurrences` e `recurrenceExceptions` na exportação e restauração; contagem de tabelas de diagnóstico (`21` coleções modeladas em memória frente às `27` tabelas Room quando consideramos tabelas auxiliares/visões) adaptada ao modelo em memória sem SAF nativo. |
| `WidgetsHubScreen` / `AlarmManager` / `WorkManager` / SAF nativo | `AppWidgetManager`, Android OS | `SIMULADO` / `NÃO APLICÁVEL` | Widgets Glance são simulados para inspeção; infraestrutura nativa de sistema operacional não se aplica a SPA web estática. |

---

## 4. Verificação Executada e Separação entre Node Headless e Browser Gráfico

- **Verificação estática de sintaxe**: `node --check` em todos os arquivos `.js` de `web-mirror-pilot/`.
- **Testes automatizados funcionais headless (`node` + `vm` + mock mínimo de DOM)**: validam inicialização nos 4 viewports e 2 temas, visão padrão `MONTH`, comportamento `WEEK` sem duplicação de `SelectedDayPanel`, todas as 6 fontes da Agenda e 6 frequências de recorrência, materialização virtual ao clicar/concluir, filtros contextuais e `showPaid`, 7 abas de Tarefas, Kanban de 6 colunas, `TaskRules` (6 grupos + `hasDueTime`), reabertura → `IN_PROGRESS`, sugestão de categoria ao registrar gasto, alertas `PRICE_ABOVE`/`PRICE_BELOW`, round-trip de Backup v7 com séries recorrentes e preservação de estado ao alternar tema/viewport.
- **Limitação explícita**: A renderização visual pixel-a-pixel em motor browser real (Chromium/Firefox/Safari) não foi executada neste ambiente terminal sem GUI; portanto, inspeção visual em tela e layout CSS são derivados de revisão de código CSS/HTML e cobertura estrutural DOM no teste headless.

---

## 5. Convenções para Importação e Diff no Google Stitch

1. **Separação de Casca**: Tudo fora de `#nexus-app-shell` (`#pilot-lab-bar` e `#pilot-trace-panel`) é ferramental de laboratório e deve ser ignorado na análise visual de UI do NEXUS.
2. **Atributos Semânticos no DOM**: Alterações dentro de `#nexus-app-shell` mantêm rastreabilidade via `data-nexus-screen`, `data-nexus-component`, `data-nexus-source`, `data-nexus-entity` e `data-nexus-status`.
3. **Integridade do Android**: Nenhum arquivo em `app/` foi alterado.
