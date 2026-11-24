# NEXUS — Mapa de Rastreabilidade do Espelho Web de Alta Fidelidade

Este documento registra o espelhamento **visual, funcional, estrutural e adaptativo** entre o aplicativo Android nativo (`app/` — Kotlin / Jetpack Compose / Material 3 / Room) e o espelho web estático em HTML, CSS e JavaScript (`web-mirror-pilot/`).

---

## 1. Arquitetura Modular do Espelho Web

| Arquivo Web | Papel Arquitetural | Referências Android (`app/`) |
|---|---|---|
| `web-mirror-pilot/index.html` | Casca HTML estática (`#nexus-app-shell` isolado de `#pilot-lab-bar`) com 4 viewports e alternância Dark/Light | `MainActivity.kt`, `Theme.kt` |
| `web-mirror-pilot/tokens.css` | Tokens Dark (`#050505`), Light **Grafite Suave** (`#D4D7DC`) e os 8 `ColoredBorderRole` proporcionais | `ui/theme/Theme.kt`, `ui/theme/Color.kt`, `ColoredBorderRole` |
| `web-mirror-pilot/styles.css` | Breakpoints adaptativos (`390px`, `768px`, `1024px`, fluido), dois painéis (`is-two-pane`), Kanban, calendários, sheets e diálogos | `MainActivity.kt`, `NexusComponents.kt`, `HomeScreen.kt`, `AgendaScreen.kt`, `MarketScreen.kt` |
| `web-mirror-pilot/data/pilot-data.js` | Store relacional completo de Gabriel (`2026-11`), catálogo de 12 ativos e motores puros de domínio | `NexusEntities.kt`, `DomainModels.kt`, `FinanceRules.kt`, `TaskRules.kt`, `AgendaEngine.kt`, `GoalProgressRules.kt`, `MarketPresentation.kt`, `NexusDatabase.kt` |
| `web-mirror-pilot/components/primitives.js` | Wordmark `Kinetik Cut + N-1` (`318x64`), cards, banners, `MonthYearSelectorBar`, `ProjectCard` e `AgendaOccurrenceRow` | `NexusComponents.kt` |
| `web-mirror-pilot/components/charts.js` | `TaskRingChart`, `BarGroupChart`, `DonutChart`, `ProgressBarRow` e `MarketHistoryChartSvg` interativo | `ui/components/charts/*.kt`, `MarketScreen.kt` |
| `web-mirror-pilot/screens/home.js` | `HomeScreen` configurável (9 blocos) com ordenação/visibilidade/densidade compacta e métricas no anel | `HomeScreen.kt` |
| `web-mirror-pilot/screens/tasks.js` | `TasksAndProjectsScreen`: 7 abas (`Hoje`, `Próximas`, `Atrasadas`, `Todas`, `Concluídas`, `Kanban`, `Projetos`), agrupamento e filtros | `TasksAndProjectsScreen.kt` |
| `web-mirror-pilot/screens/agenda.js` | `AgendaScreen`: 4 modos (`Dia`, `Semana`, `Mês`, `Lista`), grade mensal, quadro semanal, faixas `Dia inteiro`, conflitos e two-pane | `AgendaScreen.kt`, `AgendaEngine.kt` |
| `web-mirror-pilot/screens/finance.js` | `FinanceScreen`: 5 abas completas (`Resumo`, `Lançamentos`, `Cartões`, `Parcelamentos & Recorrências`, `Orçamentos, Metas & Empréstimos`), projeção 30d e histórico | `FinanceScreen.kt`, `NexusViewModel.kt` |
| `web-mirror-pilot/screens/events.js` | `EventsScreen` com filtros (`Próximos`, `Ingresso pendente`, `Todos`) e grid adaptativo | `EventsScreen.kt` |
| `web-mirror-pilot/screens/market.js` | `MarketScreen` (12 ativos, Watchlist, alertas de preço) e `MarketAssetDetailScreen` (gráfico multi-faixa, conversão USD→BRL, two-pane) | `MarketScreen.kt`, `MarketAlertSheetsAndCards.kt` |
| `web-mirror-pilot/screens/planning.js` | `PlanningGoalsScreen` e `GoalDetailSheet` (marcos, logs de progresso, vínculo ao vivo com tarefas e projetos) | `PlanningGoalsScreen.kt` |
| `web-mirror-pilot/screens/routine-assistant.js` | `RoutineAssistantScreen` (3 abas: Briefing Diário, Revisão Noturna, Notificações/Lembretes) | `RoutineAssistantScreen.kt` |
| `web-mirror-pilot/screens/more-subroutes.js` | `MoreScreen` (10 itens), `GlobalSearchScreen` (7 entidades), `DashboardCustomizationScreen`, `QuoteLibraryScreen`, `SettingsScreen`, `BackupScreen` (v7), `DiagnosticsScreen` (27 tabelas) e `WidgetsHubScreen` (4 widgets) | `MoreSettingsBackupDiagnosticsScreens.kt`, `DashboardCustomizationAndQuotesScreens.kt` |
| `web-mirror-pilot/sheets/transaction-sheet.js` | `TransactionEditorSheet` com vínculo bidirecional a Cartão/Fatura, Evento e Tarefa | `FinanceEditors.kt` |
| `web-mirror-pilot/sheets/event-sheet.js` | `EventEditorSheet` com custos vinculados, `+ Adicionar custo` e `+ Nova tarefa` | `EventsScreen.kt` |
| `web-mirror-pilot/sheets/entity-sheets.js` | `TaskEditorSheet` (subtarefas, recorrência, gasto vinculado), `AppointmentEditorSheet` (detecção ao vivo de conflito) e `ProjectEditorSheet` | `EntityEditors.kt` |
| `web-mirror-pilot/sheets/finance-extra-sheets.js` | Modais CRUD para Cartão, Parcelamento, Recorrência, Orçamento, Meta Financeira, Empréstimo, Objetivo Pessoal, Alerta de Preço, Frase, Categoria e prévia Backup v7 | `FinanceEditors.kt`, `MarketAlertSheetsAndCards.kt`, `PlanningGoalsScreen.kt`, `DashboardCustomizationAndQuotesScreens.kt`, `MoreSettingsBackupDiagnosticsScreens.kt` |
| `web-mirror-pilot/app.js` | Motor unidirecional, pilha real `subRouteStack`, `MainTopBar` (3 ícones), `QuickAddChooserSheet` (5 ações), auditoria e preservação de foco em buscas | `MainActivity.kt`, `NexusViewModel.kt` |

---

## 2. Classificação de Fidelidade por Módulo e Recurso

Legenda usada também no atributo DOM `data-nexus-status`:
- **`reproduzido`**: Estrutura, hierarquia, regras de negócio, relacionamentos e interações principais reproduzidos fielmente em memória.
- **`parcial`**: Estrutura e regras reproduzidas, mas adaptadas para ambiente browser sem APIs exclusivas de sistema (ex.: SAF nativo, Room SQLite físico).
- **`simulado`**: Funcionalidade que no Android depende de rede HTTP ou sistema operacional (`AlarmManager`, `WorkManager`, Jetpack Glance launcher widgets, AwesomeAPI/CoinGecko/Yahoo/B3) e opera no navegador com dados locais/simulados e sinalização explícita.
- **`não aplicável`**: Recursos exclusivos do SO Android sem equivalente direto em SPA estática (ex.: permissão `SCHEDULE_EXACT_ALARM`, boot receiver, atalho de launcher nativo).

### 2.1 Navegação Principal, Identidade e Responsividade

| Elemento Web | Origem Android | Status | Observações |
|---|---|---|---|
| `MainTopBar` + `NexusWordmark` | `MainActivity.kt:477-522`, `NexusComponents.kt` | `reproduzido` | Wordmark vetorial `Kinetik Cut + N-1` (`318x64`) e **exatamente 3 ações** (`Mercado`, `Pesquisa global`, `Mais`). |
| `BottomNavigationBar` + `QuickAddFab` | `MainActivity.kt:525-565` | `reproduzido` | Ordem exata (`Tarefas \| Agenda \| Início \| Finanças \| Eventos`), oculta automaticamente quando `subRouteStack` não está vazia. |
| `QuickAddChooserSheet` | `EntityEditors.kt:QuickAddChooserSheet` | `reproduzido` | 5 opções nativas: `Nova tarefa`, `Novo compromisso`, `Novo lançamento financeiro`, `Novo evento`, `Novo projeto`. |
| Pilha `subRouteStack` | `MainActivity.kt:SubRoute` | `reproduzido` | Suporta empilhamento (ex.: `Mais` → `Planejamento`, `Mercado` → `Detalhe do Ativo`, `Personalizar` → `Frases`). |
| Breakpoints adaptativos (`390px`, `768px`, `1024px`, `Fluido`) | `MainActivity.kt`, `HomeScreen.kt`, `AgendaScreen.kt`, `MarketScreen.kt` | `reproduzido` | `>=720px` ativa grids 2 colunas (`.is-wide`); `>=1024px` / Fluido ativa layout de dois painéis (`.is-two-pane`) em `Agenda` e `Detalhe do Ativo`. |
| Temas `Dark` (`#050505`) e `Grafite Suave` (`#D4D7DC`) | `Theme.kt`, `Color.kt` | `reproduzido` | 8 `ColoredBorderRole` com bordas e alfas proporcionais nos dois temas. |

### 2.2 Telas Principais (Bottom Bar)

| Tela / Fluxo | Origem Android | Status | Observações |
|---|---|---|---|
| `HomeScreen` (9 blocos configuráveis) | `HomeScreen.kt` | `reproduzido` | Ordem, visibilidade, `showPrimaryDetail`, `showOverdueOrAlerts`, `maxItems`, `compactDashboardMode`, `showMetricSummaryOnDashboard`, clique em `Frase` → `QUOTES` e `Objetivo` → `PLANNING`. |
| `TasksAndProjectsScreen` (7 abas + CRUD) | `TasksAndProjectsScreen.kt`, `EntityEditors.kt` | `reproduzido` | Abas `Hoje`, `Próximas`, `Atrasadas (N)`, `Todas`, `Concluídas`, `Kanban` (seletor rápido de coluna) e `Projetos`; filtros por status/prioridade/projeto; `TaskEditorSheet` com subtarefas, recorrência e `"Registrar gasto"`; `ProjectEditorSheet`. |
| `AgendaScreen` (4 visões + 6 fontes + Two-Pane) | `AgendaScreen.kt`, `AgendaEngine.kt` | `reproduzido` | Modos `Dia`, `Semana`, `Mês`, `Lista`; `MonthCalendarGrid`, `HorizontalWeekBoard`, `AllDayEventBandCard`, detecção de conflitos de horário, filtros de tipo/categoria/`showPaid`, abertura contextual de cada origem e `AppointmentEditorSheet`. |
| `FinanceScreen` (5 abas + projeção + auditoria) | `FinanceScreen.kt`, `FinanceEditors.kt`, `FinanceRules.kt` | `reproduzido` | `Resumo` (saldo, donut com toggle Pago/Previsto, barras 6 meses, projeção 30d, card Mercado clicável), `Lançamentos` (filtros + CSV), `Cartões` (`+ Cartão`, editar, fechar/pagar fatura), `Parcelamentos & Recorrências` (quitar 1/todas, gerar próximo, pausar), `Orçamentos, Metas & Empréstimos` (CRUD completo + `Histórico de Auditoria`). |
| `EventsScreen` + `EventEditorSheet` | `EventsScreen.kt` | `reproduzido` | Filtros (`Próximos`, `Ingresso pendente`, `Todos`), cálculo automático de custos vinculados, `+ Adicionar custo` e `+ Nova tarefa` direto do evento. |

### 2.3 SubRotas (`Mais`, `Mercado`, `Planejamento`, `Sistema`)

| SubRota | Origem Android | Status | Observações |
|---|---|---|---|
| `MoreScreen` (10 itens) | `MoreSettingsBackupDiagnosticsScreens.kt` | `reproduzido` | Lista completa dos 10 atalhos do Android, sem itens mortos. |
| `PlanningGoalsScreen` + `GoalDetailSheet` | `PlanningGoalsScreen.kt`, `GoalProgressRules.kt` | `reproduzido` | Busca, filtros por status/categoria, cálculo `AUTO` (média de marcos, tarefas e projetos) e `MANUAL`, marcos interativos, logs de progresso e vínculo ao vivo de tarefas/projetos existentes. |
| `MarketScreen` + `MarketAssetDetailScreen` | `MarketScreen.kt`, `MarketAlertSheetsAndCards.kt` | `simulado` | Catálogo completo de **12 ativos** (Moedas, Cripto, Índices, Ações), Watchlist, conversão `USD → BRL` em `MELI`, gráfico SVG com 5 períodos (`1D..1A`), pontos selecionáveis, linhas de alerta e CRUD de alertas com avaliação simulada no clique em `Atualizar`. Sem chamadas HTTP reais. |
| `RoutineAssistantScreen` | `RoutineAssistantScreen.kt` | `reproduzido` / `simulado` | Briefing Diário e Revisão Noturna reproduzidos com mutações reais em tarefas/contas; testes e agendamento de notificação marcados como `simulado` (sem `AlarmManager`). |
| `DashboardCustomizationScreen` & `QuoteLibraryScreen` | `DashboardCustomizationAndQuotesScreens.kt` | `reproduzido` | Controles globais + expansão por bloco (`showPrimaryDetail`, `showOverdueOrAlerts`, `maxItems`), restauração padrão e biblioteca completa de frases. |
| `GlobalSearchScreen` | `MoreSettingsBackupDiagnosticsScreens.kt` | `reproduzido` | Pesquisa em tempo real (com preservação de cursor) nas **7 entidades**: Tarefas, Eventos, Lançamentos, Cartões, Projetos, Objetivos Pessoais e Compromissos. |
| `SettingsScreen` | `MoreSettingsBackupDiagnosticsScreens.kt` | `reproduzido` | Nome de usuário, Tema Escuro/Grafite Suave, modo compacto, 4 padrões de data e CRUD de Categorias. |
| `BackupScreen` (v7) | `MoreSettingsBackupDiagnosticsScreens.kt` | `parcial` | Exportação `.json` real e importação via arquivo/texto com validação de esquema v7 e diálogo `BackupRestorePreviewDialog`; sem Storage Access Framework (SAF) nativo do Android. |
| `DiagnosticsScreen` | `MoreSettingsBackupDiagnosticsScreens.kt` | `parcial` | Exibe versão do esquema (`Room v18`), contagem exata das **27 tabelas** derivada do store em memória e logs locais com cópia/limpeza. |
| `WidgetsHubScreen` | `MoreSettingsBackupDiagnosticsScreens.kt` | `simulado` | Prévia viva dos 4 widgets Jetpack Glance (`Meu Dia`, `Minhas Tarefas`, `Financeiro`, `Resumo NEXUS`) e ações simuladas de sincronização/fixação. |
| `AlarmManager` / `WorkManager` / Launcher Pin nativos | Android OS APIs | `não aplicável` | Infraestrutura nativa de SO fora do escopo do navegador estático. |

---

## 3. Convenções para Importação e Diff no Google Stitch

1. **Separação de Casca**: Tudo fora de `#nexus-app-shell` (`#pilot-lab-bar` e `#pilot-trace-panel`) é ferramental de laboratório e deve ser ignorado na análise visual de UI do NEXUS.
2. **Atributos Semânticos no DOM**: Alterações feitas dentro de `#nexus-app-shell` permanecem mapeáveis aos arquivos Kotlin originais via `data-nexus-screen`, `data-nexus-component`, `data-nexus-source`, `data-nexus-entity` e `data-nexus-status`.
3. **Integridade do Android**: Nenhum arquivo em `app/` foi alterado.
