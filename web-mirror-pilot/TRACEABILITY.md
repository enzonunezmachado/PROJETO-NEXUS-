# NEXUS — Mapa de Rastreabilidade do Piloto Web (Etapa 1)

Este documento registra o espelhamento estrutural, funcional e visual entre o aplicativo Android nativo (Kotlin / Jetpack Compose / Material 3) e o piloto isolado em HTML, CSS e JavaScript (`web-mirror-pilot/`), criado para experimentação no Google Stitch e posterior análise comparativa de retorno ao Android.

---

## 1. Estrutura de Arquivos do Piloto

| Arquivo Web | Papel Arquitetural | Referências Android Principais |
|---|---|---|
| `web-mirror-pilot/index.html` | Casca HTML limpa (`#nexus-app-shell` isolado de `#pilot-lab-bar`) | `MainActivity.kt`, `Theme.kt` |
| `web-mirror-pilot/tokens.css` | Tokens semânticos Dark (`#050505`) e Light Grafite Suave (`#D4D7DC`), `ColoredBorderRole` | `ui/theme/Theme.kt`, `ui/theme/Color.kt` |
| `web-mirror-pilot/styles.css` | Layout adaptativo (`<720dp` vs `>=720dp`), cards, sheets, dialogs, tabelas numéricas (`tnum`) | `NexusComponents.kt`, `HomeScreen.kt`, `MainActivity.kt` |
| `web-mirror-pilot/data/pilot-data.js` | Modelo relacional em memória (Gabriel, `2026-11`) e seletores puros de domínio | `NexusEntities.kt`, `DomainModels.kt`, `FinanceRules.kt`, `NexusEnums.kt` |
| `web-mirror-pilot/components/primitives.js` | Primitivas visuais e Wordmark `Kinetik Cut` + `N-1` (`viewBox="0 0 318 64"`) | `NexusComponents.kt` |
| `web-mirror-pilot/components/charts.js` | Gráficos SVG interativos (`TaskRingChart`, `BarGroupChart`, `DonutChart`, `ProgressBarRow`) | `ui/components/charts/*.kt` |
| `web-mirror-pilot/screens/home.js` | `HomeScreen` configurável (9 blocos) + prévias de fronteira (`Tasks`, `Agenda`, `More`, `GlobalSearch`) | `HomeScreen.kt`, `MainActivity.kt` |
| `web-mirror-pilot/screens/routine-assistant.js` | `RoutineAssistantScreen` (3 abas: Briefing Diário, Revisão Noturna, Notificações) | `RoutineAssistantScreen.kt` |
| `web-mirror-pilot/screens/finance.js` | `FinanceScreen` (`Resumo`, `Lançamentos`, `Cartões`, e abas de fronteira) | `FinanceScreen.kt`, `NexusViewModel.kt` |
| `web-mirror-pilot/screens/events.js` | `EventsScreen` com filtros (`Próximos`, `Ingresso pendente`, `Todos`) e grid adaptativo | `EventsScreen.kt` |
| `web-mirror-pilot/sheets/transaction-sheet.js` | `TransactionEditorSheet` com ponte de navegação para Fatura/Cartão e Evento vinculado | `FinanceEditors.kt` |
| `web-mirror-pilot/sheets/event-sheet.js` | `EventEditorSheet` com cálculo de custos, lista de transações vinculadas e tarefas | `EventsScreen.kt` |
| `web-mirror-pilot/app.js` | Motor unidirecional de estado, navegação, dialogs, CSV e inspetor DOM de rastreabilidade | `MainActivity.kt`, `NexusViewModel.kt` |

---

## 2. Classificação de Fidelidade por Fluxo e Componente

Legenda usada também nos atributos DOM `data-nexus-status`:
- **`reproduzido`**: Estrutura, hierarquia, regras visuais e interações principais reproduzidas integralmente no piloto em memória.
- **`parcial`**: Estrutura e vínculos principais presentes, porém com subfluxos secundários simplificados ou marcados como fronteira da Etapa 1.
- **`simulado`**: Funcionalidade que no Android depende de infraestrutura de sistema/rede (`AlarmManager`, `WorkManager`, Room, cotações externas) e no piloto opera com dados/feedback simulados no navegador.

### Fluxo 1 — Home e Navegação Principal

| Elemento Web (`data-nexus-component`) | Origem Android | Status | Observações |
|---|---|---|---|
| `MainTopBar` + `NexusWordmark` | `MainActivity.kt:477-522`, `NexusComponents.kt:buildNexusWordmarkPath` | `reproduzido` | Path vetorial exato da Kinetik Cut + N-1 (`318x64`) e botões Sino, Mercado, Busca, Mais. |
| `BottomNavigationBar` + `QuickAddFab` | `MainActivity.kt:525-565` (`BOTTOM_NAV_TAB_ORDER`) | `reproduzido` | Ordem idêntica (`Tarefas | Agenda | Início | Finanças | Eventos`); oculta automaticamente em `SubRoute`. |
| `GreetingBlock` + `DailyQuoteBlock` | `HomeScreen.kt` | `reproduzido` | Saudação com ícone de personalização na mesma linha e frase em itálico sem card envoltório. |
| `TaskSummaryBlock` + `TaskRingChart` | `HomeScreen.kt`, `TaskRingChart.kt` | `reproduzido` | Anel proporcional, centro com percentual/contagem, legenda clicável e alerta de tarefas atrasadas. |
| `TodayAgendaBlock` + `RoutineBriefingEntryCard` | `HomeScreen.kt` | `reproduzido` | Card de entrada para o Assistente de Rotina alterna dinamicamente entre `"Abrir"` e `"Revisado"`, mais detecção visual de conflitos de horário. |
| `FinanceSummaryBlock` + `BalanceOverviewCard` | `HomeScreen.kt`, `DeclaredBalanceDialog` | `reproduzido` | Edição real em memória do saldo informado e salto para próximo vencimento / contas atrasadas. |
| `ChartsIndicatorsBlock` (`HomeCashflowCard` + `HomeCategoryDonutCard`) | `HomeScreen.kt`, `BarGroupChart.kt`, `DonutChart.kt` | `reproduzido` | Seleção interativa de mês nas barras e fatia no donut com destaque numérico no centro. |
| `UpcomingEventsBlock` + `UpcomingPaymentsBlock` + `GoalsBlock` | `HomeScreen.kt` | `reproduzido` | Inclui card direto para a fatura `Nubank Ultravioleta (2026-11)` em atraso. |
| `DashboardCustomizationSheet` | `DashboardCustomizationScreen.kt` | `reproduzido` | Reordenar (`↑`/`↓`) e ativar/desativar qualquer um dos 9 blocos + toggles de saudação/frase. |
| `TasksTabBoundaryView`, `AgendaTabBoundaryView`, `MoreScreen`, `GlobalSearchScreen` | `TasksScreen.kt`, `AgendaScreen.kt`, `MoreScreen.kt`, `GlobalSearchScreen.kt` | `parcial` | Incluídos como fronteiras funcionais navegáveis (zero cliques mortos) sem replicar todos os editores de Tarefas/Agenda na Etapa 1. |

### Fluxo 2 — Assistente de Rotina (Briefing Diário)

| Elemento Web (`data-nexus-component`) | Origem Android | Status | Observações |
|---|---|---|---|
| `SubRouteTopBar` + `RoutineAssistantTabs` | `RoutineAssistantScreen.kt` | `reproduzido` | Oculta topbar/bottom bar principais; exibe seta voltar + 3 abas (`Briefing Diário`, `Revisão Noturna`, `Notificações`). |
| `DailyBriefingTab` | `RoutineAssistantScreen.kt:DailyBriefingTab` | `reproduzido` | Contadores dinâmicos, conflito de agenda, concluir/adiar tarefa para amanhã, dar baixa/adiar conta, atalho direto para faturas em atenção e botão `"Marcar Briefing como revisado"`. |
| `NightReviewTab` + `TomorrowPreviewCard` | `RoutineAssistantScreen.kt:NightReviewTab` | `reproduzido` | Tarefas adiadas no Briefing aparecem em tempo real na prévia de amanhã (`24/11/2026`); botão `"Fechar revisão de hoje"` registra histórico. |
| `NotificationsSettingsTab` + `ReminderRowCard` | `RoutineAssistantScreen.kt:NotificationsSettingsTab` | `parcial` / `simulado` | Toggles de canais e antecedência funcionam no estado web; `"Notificação teste"`, `"Adiar 15 min"` e `"Cancelar"` simulam o comportamento do `AlarmManager`/WorkManager do Android via snackbar e estado local. |

### Fluxo 3 — Finanças → Transação → Fatura → Evento Relacionado

| Elemento Web (`data-nexus-component`) | Origem Android | Status | Observações |
|---|---|---|---|
| `FinanceOverviewTab` (`Resumo`) | `FinanceScreen.kt:OverviewTabContent` | `reproduzido` | Seletor mensal (`MonthYearSelectorBar`), saldo, totais do mês, donut interativo com filtro de categoria expansível (`Pagos + prev.` / `Só pagos`) + `"Ver todos"`, barras de 6 meses (`Realizado` / `Previsto`) e orçamentos. |
| `MarketSummarySimulatedCard` | `FinanceScreen.kt:MarketSummarySection` | `simulado` | Exibe cotações fictícias offline (USD, EUR, BTC) marcadas como simuladas; tela dedicada de Mercado ficou fora da Etapa 1. |
| `TransactionsTabContent` + `FinanceFilterBottomSheet` | `FinanceScreen.kt:TransactionsTabContent` | `reproduzido` | Filtros por período, tipo, status e categoria; marcação rápida de baixa (`SETTLED`) e exportação CSV real com download/preview (`parcial`). |
| `CardsTabContent` + `CreditCardStatementCard` | `FinanceScreen.kt:CardsTabContent` | `reproduzido` | Limite comprometido/disponível recalculado dinamicamente; ações `"Fechar fatura"` e `"Pagar fatura"`; lista clicável de lançamentos vinculados abrindo diretamente `TransactionEditorSheet`. |
| `TransactionEditorSheet` + `TransactionRelationsBridge` | `FinanceEditors.kt:TransactionEditorSheet` | `reproduzido` | Criação, edição, exclusão e proteção contra descarte (`confirmDialog`); exibe ponte explícita para saltar diretamente à **Fatura vinculada** (com destaque visual na aba Cartões) ou ao **Evento relacionado**. |
| `EventsScreen` + `EventEditorSheet` | `EventsScreen.kt` | `reproduzido` | Filtros de eventos, cálculo automático de `Custos do evento` a partir das transações vinculadas, botão `"+ Adicionar custo"` pré-vinculado ao evento e lista interativa de tarefas vinculadas. |
| `FinanceRecurringBoundaryTab` & `FinancePlanningBoundaryTab` | `FinanceScreen.kt` | `parcial` | Abas presentes no `ScrollableTabRow` com dados reais filtrados para manter integridade de navegação sem replicar CRUD completo de parcelas/empréstimos nesta etapa. |

---

## 3. Convenções para Importação e Diff no Google Stitch

1. **Separação de Casca**: Tudo fora de `#nexus-app-shell` (`#pilot-lab-bar` e `#pilot-trace-panel`) é ferramental de laboratório e deve ser ignorado na análise visual de UI do NEXUS.
2. **Atributos Semânticos no DOM**: Qualquer alteração visual feita no Stitch dentro de `#nexus-app-shell` pode ser rastreada de volta ao arquivo Kotlin original pelos atributos:
   - `data-nexus-screen`
   - `data-nexus-component`
   - `data-nexus-source`
   - `data-nexus-entity`
   - `data-nexus-status`
3. **Integridade do Android**: Nenhum arquivo em `app/` foi alterado nesta fase. Qualquer evolução aprovada após a rodada no Stitch deverá ser aplicada manualmente ou em etapa futura autorizada no código Kotlin/Compose.
