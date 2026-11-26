# 02 — Especificação Completa do Produto (`NEXUS_PRODUCT_SPEC`)

> **Finalidade:** definir todos os requisitos funcionais, regras de negócio, fluxos esperados, entradas, saídas e contratos entre os 13 módulos do NEXUS.  
> **Nota de rigor:** cada requisito e módulo diferencia explicitamente o que está **implementado no código atual**, o que é **parcial/inconsistente** e o que é **planejado**, registrando também a **divergência histórica do Módulo Mercado** em relação às especificações da época `v5`.

---

## 1. Regras Globais do Produto

1. **Idioma e localização:** português brasileiro (`pt-BR`), moeda padrão `R$` (`BRL`) para o módulo financeiro — preservando unidade nativa no Módulo Mercado (`BRL`, `USD` ou `pts`), datas `dd/MM/yyyy`, horário 24h (`HH:mm`), fuso `America/Sao_Paulo`.
2. **Fonte única da verdade:** nenhum módulo duplica registros de outro domínio.
3. **Estado de atraso dinâmico:** contas e faturas atrasadas não dependem de job noturno gravando `OVERDUE` (são promovidas dinamicamente em leitura por `FinanceRules` / `NexusRepository`).
4. **Sem números ou conteúdos inventados:** ausência de dados produz estados vazios limpos (`NexusEmptyState`); taxas/juros não são pré-preenchidos; cotações indisponíveis mantêm último cache ou placeholder `--`.
5. **Confirmações de segurança:** exclusões com filhos vinculados explicitam se os filhos serão desvinculados (`SET_NULL`) ou removidos em cascata (`CASCADE`); saída de formulários sujos pede confirmação (`ConfirmDiscardChangesDialog`).

### Matriz Global: "Registrar uma vez, ver em vários lugares"

| Registro Criado | Início (`HomeScreen`) | Agenda (`AgendaEngine`) | Tarefas | Eventos | Financeiro |
| --- | --- | --- | --- | --- | --- |
| **Tarefa** | Anel do mês / hoje / atrasadas | Se `dueAt != null` | Registro completo | Se `eventId != null` | Se houver `transactions.taskId` |
| **Compromisso** | Próximo / hoje | Registro completo (+ conflitos) | — | Se `appointments.eventId != null` | — |
| **Evento** | Próximos eventos | Faixa em todos os dias (`startAt..endAt`) | Tarefas vinculadas | Registro completo | Ingresso + custos extras |
| **Despesa / Receita direta** (`statementId == null`) | Resumo do mês / próximos vencimentos | Se não liquidada (`showPaid=false`) e `dueAt != null` | Badge `"Gasto: R$ X"` se `taskId != null` | Se `eventId != null` | Registro completo + gráficos |
| **Despesa liquidada** | Totais / gráficos realizados | Oculta por padrão (`showPaid = true` exibe) | Badge `"Gasto: R$ X"` | Soma em `paidTotalCostCents` | Registro completo |
| **Compra no Cartão** (`statementId != null`) | Totais / gráficos por categoria | **Oculta** (aparece apenas a fatura) | Badge `"Gasto: R$ X"` | Soma no custo do evento | Lançamento + Fatura do cartão |
| **Fatura de Cartão** | Alerta se atrasada / próxima | Se `status != PAID` e `totalCents > 0` | — | — | Aba Cartões (quitação sem dupla despesa) |
| **Projeto com prazo** | Contadores / lista ativa | No dia de `deadlineAt` | Aba Projetos + progresso | — | Custos agregados (`projectId`) |

---

## 2. Catálogo de Requisitos Funcionais (`REQ-*`) e Status Atual

| ID | Requisito Resumido | Módulo | Status no Código Atual |
| --- | --- | --- | --- |
| `REQ-001` | Navegação por estado reativo (`MainTab` na ordem `TASKS, AGENDA, HOME, FINANCE, EVENTS`) + `BackHandler` com confirmação de descarte | Shell / Navegação | `IMPLEMENTADA` |
| `REQ-002` | `QuickAddChooser` para criação rápida contextual (Tarefa, Compromisso, Lançamento, Evento, Projeto) | Shell / Navegação | `PARCIALMENTE IMPLEMENTADA` (5 opções e sem colapso uniforme `"Mais detalhes"` em todos os editores; `TaskEditor` possui `"Mais detalhes"` para recorrência) |
| `REQ-003` | Identidade visual N dedicada + `androidx.core:core-splashscreen` (`Theme.NEXUS.Splash`) | Shell / Identidade | `PARCIALMENTE IMPLEMENTADA` (`NexusWordmark` N-1 + Kinetik Cut e launcher icon adaptativo N-1 implementados; falta `core-splashscreen` / `Theme.NEXUS.Splash`) |
| `REQ-004` | Dashboard modular configurável (`9` blocos ordenáveis/desligáveis, modo compacto, saudação, data e frase diária) | Módulo 1 — Dashboard | `IMPLEMENTADA` |
| `REQ-005` | Gráfico de anel de tarefas (`TaskRingChart`), gráficos financeiros (`BarGroupChart`, `DonutChart`), alertas reais (`AlertBannerCard`), metas/projetos e próximos itens no Dashboard | Módulo 1 — Dashboard | `IMPLEMENTADA` (seletor de mês do bloco de tarefas fixo no mês atual na Home; sem bloco `MARKET` configurável na Home) |
| `REQ-006` | CRUD completo de Tarefas + prioridades + 6 status + Kanban sobre a mesma tabela + checklist de subtarefas | Módulo 2 — Tarefas | `IMPLEMENTADA` (`INT-03`: alternar subtarefa persistida em `TaskEditorSheet` grava direto no Room antes de "Salvar") |
| `REQ-007` | Vínculo Tarefa → Gasto (`transactions.taskId`, `"Registrar gasto"`, badge `"Gasto: R$ X"`, exibição `"Tarefa: <título>"` no Financeiro) | Módulo 2 / 6 | `IMPLEMENTADA` (exige salvar tarefa antes quando nova/suja) |
| `REQ-008` | Recorrência de Tarefas (`task_recurrences`, `recurrence_exceptions`, `SKIP`/`OVERRIDE`, `"Só esta"` vs `"Toda a série"`, geração idempotente ao concluir) | Módulo 2 — Tarefas | `IMPLEMENTADA` (`Lote 06 Recorrência`) |
| `REQ-009` | Recorrência para Compromissos e Eventos com expansão na Agenda | Módulo 3 / 4 / 5 | `NÃO IMPLEMENTADA` |
| `REQ-010` | Gestão de Projetos com prazo, status, progresso automático por tarefas, custos e vínculo a Objetivo (`goalId`) | Módulo 3 — Projetos | `IMPLEMENTADA` |
| `REQ-011` | Agenda unificada pura (`AgendaEngine`), 4 visões (`Dia`, `Semana`, `Mês`, `Lista`), filtros por tipo/categoria/busca/`showPaid`, eventos multi-dia e conflito de horários | Módulo 4 — Agenda | `IMPLEMENTADA` (motor legado `observeUnifiedAgenda` ainda coexiste para Dashboard/Briefings) |
| `REQ-012` | Compromissos vinculados a Eventos (`appointments.eventId`) | Módulo 4 / 5 | `PARCIALMENTE IMPLEMENTADA` (coluna/FK/editor e contador em `EventEditorSheet` existem; falta listagem completa e criação direta na ficha do Evento) |
| `REQ-013` | Eventos com status, ingresso gratuito/pago/parcelado vinculado ao cartão e custos extras por `EventCostType`, mais atalhos de mapas | Módulo 5 — Eventos | `IMPLEMENTADA` |
| `REQ-014` | Eventos com `locationMode` (`IN_PERSON`, `ONLINE`, `TBD`), ocultação padrão de `ticketFeesCents` e banner JPEG local (`PickVisualMedia`) | Módulo 5 — Eventos | `NÃO IMPLEMENTADA` |
| `REQ-015` | Financeiro completo: lançamentos, saldo declarado manual, seletor `< mês/ano >`, `DonutChart` interativo, `BarGroupChart` 6m, previsão 30d, orçamentos (`ProgressBarRow`), metas e empréstimos | Módulo 6 — Financeiro | `IMPLEMENTADA` |
| `REQ-016` | Cartões de crédito: fechamento/vencimento de faturas, pagamento sem duplicar despesa, limite calculado vs informado e parcelamentos | Módulo 6 — Financeiro | `IMPLEMENTADA` |
| `REQ-017` | Módulo Mercado: infraestrutura de cotações (`USD/BRL`, `EUR/BRL`, `BTC/BRL`, `ETH/BRL`, `SOL/BRL`, `IBOV`, `SPX`, `IXIC`, `PETR4`, `VALE3`, `ITUB4`, `MELI`), provedores públicos sem token obrigatório + fallbacks + sync offline-first | Módulo 7 — Mercado | `IMPLEMENTADA` (`Lotes 01–04` — evoluiu além da spec antiga `v5`) |
| `REQ-018` | Módulo Mercado: tela própria (`MarketScreen`), detalhe (`MarketAssetDetailScreen`), gráficos históricos offline/online (`1D`–`1Y`, sem `1D` para moedas), favoritos (*Watchlist*) e alertas locais de preço | Módulo 7 — Mercado | `IMPLEMENTADA` (`Lotes 03, 05, 06, 07, 09`) |
| `REQ-019` | Objetivos pessoais (`AUTO`/`MANUAL`), marcos (`goal_milestones`), logs (`goal_progress_logs`) e vínculo com Tarefas/Projetos | Módulo 8 — Objetivos | `IMPLEMENTADA` |
| `REQ-020` | Assistente de Rotina: Briefing Diário, Revisão Noturna, adiamento de tarefas em 1 toque e `briefing_history` | Módulo 9 — Rotina | `IMPLEMENTADA` |
| `REQ-021` | Sistema de Lembretes (`AlarmManager` + `WorkManager`, 5 canais incluindo `CHANNEL_MARKET_ALERTS`, horário de silêncio, ações rápidas em notificação, `BootReceiver`) | Módulo 10 — Notificações | `IMPLEMENTADA` (agendamento automático de recorrências futuras em 30d limitado às ocorrências já materializadas ou projeção conforme scheduler) |
| `REQ-022` | 4 Widgets Glance responsivos (`MyDay`, `MyTasks`, `Finance`, `NexusSummary`) com config isolada por ID, tema claro/escuro e deep-links | Módulo 11 — Widgets | `IMPLEMENTADA` |
| `REQ-023` | Configurações (nome, tema claro/escuro, categorias), Personalização do Dashboard e Biblioteca de Frases (JSON/TXT sem frases embutidas) | Módulo 12 — Config/Personalização | `IMPLEMENTADA` |
| `REQ-024` | Busca Global unificada (`GlobalSearchScreen`) | Módulo 12 / Busca | `IMPLEMENTADA` |
| `REQ-025` | Backup JSON `v7` transacional via SAF (27 tabelas + `marketConfig` com rollback): **Modo 1** restauração completa e **Modo 2** mesclagem incremental idempotente sem apagar dados locais, retrocompatibilidade `v1..v6`, CSV financeiro 15 colunas e Diagnóstico (`error_logs`) | Módulo 13 — Backup/Diagnóstico | `IMPLEMENTADA` |
| `REQ-026` | Backup completo `.zip` (`data.json` + pasta `banners/`) | Módulo 13 — Backup | `NÃO IMPLEMENTADA` |

---

## 3. Especificação Detalhada por Módulo (13 Módulos)

### Módulo 1 — Dashboard / Tela Inicial (`HomeScreen`)
- **Objetivo:** oferecer visão imediata do dia/mês sem poluir a tela com listas infinitas.
- **Funcionalidades e Blocos (`DashboardBlockType`):**
  1. `GREETING`: saudação (`AUTO` por horário ou template com `{nome}`) + data no formato escolhido (`NexusDateFormat`) + atalho de personalização.
  2. `DAILY_QUOTE`: incorporada discretamente ao cabeçalho da saudação (ou inline se saudação desligada); seleciona 1 frase ativa por `dateIso`, evitando repetir `lastQuoteId`/`previousQuoteId` quando há `>= 2` frases ativas.
  3. `TASK_SUMMARY`: gráfico `TaskRingChart` do mês atual (Concluídas, Em andamento, Pendentes, contador separado de Atrasadas e badge de tarefas sem prazo) + banners de alerta real (`AlertBannerCard` financeiro e de conflito de agenda) + listas enxutas de atrasadas/prioritárias.
  4. `TODAY_AGENDA`: próximo compromisso + compromissos do dia.
  5. `FINANCE_SUMMARY`: saldo declarado manual, totais do mês (`FinanceRules`) e próximo vencimento.
  6. `UPCOMING_EVENTS`: eventos confirmados/planejados futuros ou em curso.
  7. `UPCOMING_PAYMENTS`: contas diretas e faturas em aberto.
  8. `GOALS`: objetivos pessoais e metas financeiras ativos.
  9. `CHARTS_INDICATORS`: gráficos `BarGroupChart` (fluxo 6 meses) e `DonutChart` (gastos por categoria do mês) sincronizados com `FinanceRules`.
- **Diferenciação de Entrada Mercado:** na implementação atual, o usuário acessa o Módulo Mercado pela aba/menu de atalhos (`FinanceScreen` ou `MoreScreen` -> `SubRoute.Market`); `DashboardBlockType` não possui bloco `MARKET` dedicado na `HomeScreen`.

### Módulo 2 — Tarefas, Subtarefas e Recorrência (`TasksAndProjectsScreen` / `EntityEditors`)
- **Objetivo:** gerenciar itens acionáveis com contexto de projeto, evento, objetivo, checklist, gasto vinculado e recorrência.
- **Entidades:** `TaskEntity`, `SubtaskEntity`, `TaskRecurrenceEntity`, `RecurrenceExceptionEntity`.
- **Regras:**
  - `dueAt` (exibido como `"Quando"`) define presença na Agenda; tarefas sem data ficam fora da Agenda.
  - Status (`NOT_STARTED`, `IN_PROGRESS`, `WAITING`, `BLOCKED`, `COMPLETED`, `CANCELLED`) é separado da condição de atraso (`TaskRules.deadlineCondition` / `AgendaEngine.isTaskOverdue`).
  - Kanban lê os mesmos registros da tabela `tasks` agrupados pelos 6 `TaskStatus`.
  - **Recorrência (`TaskRecurrenceFrequency`):** `DAILY`, `SPECIFIC_WEEKDAYS` (`weekdaysMask`), `WEEKLY`, `BIWEEKLY`, `MONTHLY` (âncora de dia preservada pelo `LocalDate.plusMonths`), `CUSTOM_DAYS`, com término nunca / `endDateIso` / `maxOccurrences`.
  - Ao concluir uma tarefa recorrente (`completeTask`), a linha atual fica `COMPLETED` no histórico e `RecurrenceEngine.nextOccurrenceDateAfter` calcula a próxima ocorrência materializada idempotentemente via `UNIQUE(recurrenceId, occurrenceDateIso)`.
  - Edição de série pergunta `"Só esta ocorrência"` (cria `RecurrenceExceptionEntity` tipo `OVERRIDE` ou `SKIP`) vs `"Toda a série"`.
  - **Inconsistência registrada (`INT-03`):** hoje `onToggleSubtask` em `TaskEditorSheet` grava diretamente no Room quando a subtarefa já existia no banco, em vez de aguardar `"Salvar"`.

### Módulo 3 — Projetos (`TasksAndProjectsScreen`)
- **Objetivo:** agrupar tarefas e custos em entregas com prazo e vínculo opcional a um Objetivo Pessoal (`goalId`).
- **Regras:**
  - Progresso (`ProjectWithProgress`) é derivado automaticamente de `completedTasks / totalTasks` das tarefas ligadas (`tasks.projectId`).
  - Custos somam despesas não canceladas (`transactions.projectId`).
  - Quando `deadlineAt != null` e status `!= COMPLETED/ARCHIVED`, o prazo aparece na Agenda (`AgendaOccurrenceKind.PROJECT`).

### Módulo 4 — Agenda e Compromissos (`AgendaScreen` / `AgendaEngine`)
- **Objetivo:** consolidar tudo o que tem data ou horário no NEXUS sem copiar dados.
- **Visões (`AgendaViewMode`):** `MONTH` (padrão ao abrir na aba Agenda, grade mensal com marcadores + lista do dia selecionado), `WEEK` (segunda a domingo com setas e cabeçalho de intervalo), `DAY` e `LIST`.
- **Filtros:** tipo (`Tudo`, `Tarefas`, `Compromissos`, `Eventos`, `Financeiro`, `Projetos`), categoria contextual, busca textual e toggle `"Mostrar já pagos"` (`showPaid = false` por padrão).
- **Regras de exibição:**
  - Compromissos com sobreposição de horário (não `isAllDay`) recebem sinalização de conflito (`computeAppointmentConflicts`).
  - Eventos/compromissos de múltiplos dias expandem para todos os dias de `startAt..endAt`.
  - Compras de cartão ligadas a fatura (`statementId != null`) **não** aparecem individualmente na Agenda; apenas a **fatura** em aberto com `totalCents > 0` aparece no seu vencimento (`dueAt`).
  - Recorrências de tarefas são projetadas virtualmente na janela pedida (`AgendaEngine`) sem duplicar linhas materializadas.

### Módulo 5 — Eventos (`EventsScreen`)
- **Objetivo:** controlar participação, datas, endereço, tarefas vinculadas e impacto financeiro completo de eventos.
- **Regras:**
  - `TicketStatus.FREE` ou `CANCELLED` não mantém obrigações em aberto (`isTicketObligation = true`).
  - Ingresso pago à vista/boleto/pix gera 1 `TransactionEntity` vinculada (`eventCostType = TICKET`, `isTicketObligation = true`).
  - Ingresso pago no cartão em `N > 1` parcelas gera `InstallmentPurchaseEntity` + `N` transações distribuídas nas respectivas faturas.
  - Quitação de parcelas ou fatura sincroniza `ticketStatus` (`PARTIALLY_PAID` → `PAID`).
  - **Divergências pendentes em relação à spec:** ainda não existem `locationMode`, `bannerPath` (`PickVisualMedia`), nem ocultação padrão de `ticketFeesCents` (o campo ainda aparece no formulário de edição de evento).

### Módulo 6 — Financeiro Completo (`FinanceScreen` / `FinanceEditors` / `FinanceRules`)
- **Objetivo:** oferecer controle pessoal completo de fluxo, cartões, faturas, parcelamentos, recorrências, orçamentos, empréstimos e metas.
- **Regras de ouro:**
  - `declaredBalanceCents` é **manual** e nunca é recalculado pelas transações.
  - Mês de referência de uma transação: `dueAt ?: occurredAt`.
  - `DonutChart`: despesas do mês por categoria (`Pagas` ou `Pagas + previstas`), top 6 + `Outros`; toque filtra a lista abaixo.
  - `BarGroupChart`: 6 meses encerrando no mês selecionado (`Realizado` ou `Previsto`) + `"Resultado do mês"` (`receivedIncomeCents - paidExpenseCents`).
  - `Forecast30DaysSummary`: despesas não liquidadas e receitas previstas nos próximos 30 dias + top 5 maiores vencimentos.
  - `BudgetProgress`: barra via `ProgressBarRow`, faixas <= 80%, 80%–100% (`warning`), > 100% (`danger` + `"excedeu R$ X"`), ordenação decrescente.
  - **Cartões:** compras são atribuídas à fatura do mês conforme `closingDay`; `payCreditCardStatement` liquida todas as transações da fatura sem criar segunda despesa. `computedRemainingLimitCents = (totalLimitCents - currentOpenTotalCents).coerceAtLeast(0L)`.

### Módulo 7 — Mercado Financeiro (`MarketScreen` / `data/market/*`) — **Evolução Consolidada (Lotes 01–09)**

> **Nota sobre Divergência Histórica vs Spec `v5`:**  
> Documentos antigos (`NEXUS_PRODUCT_SPEC` v5, `lista.md`, `NEXUS_IMPLEMENTATION_ROADMAP` Lote 5) descreviam o Mercado apenas como um pequeno bloco na `HomeScreen` com tabelas Room (`market_quotes`, `market_history`), sem ações individuais, sem alertas e sem `WorkManager`.  
> **Os Lotes 01 a 09 (aprovados e implementados posteriormente) substituíram essa visão** pela arquitetura real atual descrita abaixo:

- **12 Ativos Suportados (`MarketAssetCatalog`):**
  - **Moedas:** `USD-BRL` (Dólar), `EUR-BRL` (Euro).
  - **Criptomoedas:** `BTC-BRL` (Bitcoin), `ETH-BRL` (Ethereum), `SOL-BRL` (Solana).
  - **Índices:** `IBOV` (B3, `pts`), `SPX` (S&P 500, `pts`, base USD), `IXIC` (Nasdaq Composite, `pts`, base USD).
  - **Ações:** `PETR4` (B3, BRL), `VALE3` (B3, BRL), `ITUB4` (B3, BRL), `MELI` (Nasdaq, USD).
- **Funcionalidades Reais Implementadas:**
  - **Cotações sem chave obrigatória:** provedores públicos gratuitos por categoria com fallback automático (`AwesomeApi`, `BcbOlindaPtax`, `CoinGecko`, `CoinPaprika`, `B3`, `Brapi` público, `CNBC`, `Nasdaq`), preservando modalidade (`COMMERCIAL`, `PTAX`, `SPOT`, `EXCHANGE`, `DELAYED`, `INDEX`), unidade nativa e conversão informativa de USD para BRL usando a taxa `USD-BRL` em cache.
  - **Histórico Gráfico (`Lote 05`):** intervalos `1D`, `1W`, `1M`, `3M`, `6M`, `1Y` (`1D` omitido para moedas fiduciárias por ausência de intraday confiável gratuito), cache offline por arquivo em `filesDir/market_history/` e gráfico `MarketHistoryChartCard`.
  - **Watchlist / Acompanhamento (`Lote 06`):** favoritar/desfavoritar sem sair da tela na listagem e no detalhe, seção **Acompanhamento**, persistência deduplicada em `SharedPreferencesMarketWatchlistStore`.
  - **Alertas de Preço (`Lote 07`):** alertas locais `PRICE_ABOVE` / `PRICE_BELOW` na moeda/unidade original do ativo, proteção anti-duplicação (`awaitingConditionReset` e `lastEvaluatedQuoteFetchedAtEpochMs`), avaliação automática ao receber cotações frescas (manual ou `MarketQuoteSyncWorker` 30m) e canal `CHANNEL_MARKET_ALERTS` com deep-link direto para o ativo.
  - **Backup v7 (`Lote 08–09`):** favoritos e alertas pessoais são exportados/restaurados no bloco `marketConfig` do Backup JSON `v7` com rollback coordenado; cotações em cache e históricos ficam fora do backup.
  - **Aviso obrigatório:** textos explícitos informando natureza meramente informativa (não é recomendação de investimento).

### Módulo 8 — Objetivos Pessoais (`PlanningGoalsScreen`)
- **Objetivo:** acompanhar objetivos de médio/longo prazo via marcos (`goal_milestones`), progresso manual (`goal_progress_logs`) ou cálculo automático (`AUTO`) combinando marcos + tarefas (`tasks.goalId`) + projetos (`projects.goalId`).

### Módulo 9 — Assistente de Rotina (`RoutineAssistantScreen`)
- **Objetivo:** fornecer **Briefing Diário** (resumo do dia, contas e faturas vencendo/atrasadas, eventos e tarefas) e **Revisão Noturna** (concluídas hoje, pendências de hoje/atrasadas com adiamento em 1 toque para amanhã 09h/12h e prévia de amanhã), gravando revisão em `briefing_history` (1 registro por tipo/dia).

### Módulo 10 — Sistema de Lembretes e Notificações (`notifications/*`)
- **Objetivo:** entregar alertas locais confiáveis sem backend push.
- **Canais (`NexusNotificationChannels`):** `nexus_schedule`, `nexus_finance`, `nexus_events`, `nexus_routine`, `nexus_market_alerts`.
- **Comportamentos:** respeito a horário de silêncio (`quietHours`), proteção contra lembretes cancelados/dispensados (`ReminderRules`), ações rápidas na notificação (`Concluir`, `Marcar pago`, `Adiar 15m`, `Dispensar`, `Marcar revisado`), fallback para alarme inexato quando `canScheduleExactAlarms()` for falso e reagendamento após `BOOT_COMPLETED` / troca de data/fuso.

### Módulo 11 — Widgets Android (`widgets/*`)
- **Objetivo:** expor atalhos e resumos acionáveis na tela inicial do Android via **Jetpack Glance**.
- **4 Widgets:** `MyDayGlanceWidget` (Meu Dia), `MyTasksGlanceWidget` (Minhas Tarefas com conclusão segura em 2 etapas), `FinanceGlanceWidget` (Financeiro do mês) e `NexusSummaryGlanceWidget` (Resumo geral). Todos suportam `SMALL`, `MEDIUM`, `LARGE`, configuração individual (`NexusWidgetConfigActivity`) e tema dual Claro/Escuro (`DayNightColorProvider`).

### Módulo 12 — Configurações, Personalização, Frases e Busca Global
- **Configurações (`SettingsScreen`):** nome do usuário, alternância tema Escuro (padrão) / Claro e CRUD de categorias (`CategoryScope`).
- **Personalização (`DashboardCustomizationScreen`):** ordenação e toggles dos 9 `DashboardBlockType`, template de saudação, formato de data (`NexusDateFormat`) e modo compacto.
- **Biblioteca de Frases (`QuoteLibraryScreen`):** começa vazia (zero frases fictícias), suporta CRUD e importação/exportação SAF em JSON ou TXT (`texto - autor` / `texto | autor`).
- **Busca Global (`GlobalSearchScreen`):** busca textual unificada em tempo real em tarefas, projetos, compromissos (com detecção de conflitos sobre o conjunto total), eventos, transações, cartões e objetivos.

### Módulo 13 — Backup, Restauração, Mesclagem Incremental, Entrada Estruturada e Diagnóstico (`BackupManager` / `BackupMergeEngine` / `StructuredInputProcessor` / `MoreSettingsBackupDiagnosticsScreens`)
- **JSON `v7` via SAF:** exporta as 27 tabelas Room + `marketConfig` (favoritos e alertas válidos). A importação valida o arquivo, tolera `v1..v6` (`null` em `marketConfig` preserva dados locais existentes) e oferece **dois fluxos totalmente separados na UI** (sem troca silenciosa de modo dentro do diálogo):
  - **Modo 1 — Restauração Completa (`restoreAtomically`):** exibe diálogo dedicado com alerta explícito de ação destrutiva e **checkbox obrigatório de confirmação** antes de habilitar o botão `"Substituir tudo"`. Limpa tabelas filhas → pais, substitui o banco inteiro e aplica `marketConfig` com rollback coordenado Room ↔ Mercado.
  - **Modo 2 — Mesclagem Incremental (`mergeBackupAtomically` com prévia `previewMerge`):** operação padrão recomendada e idempotente (`append-only` / sem exclusões implícitas). Antes de confirmar, executa **simulação real em dry-run** (`BackupMergeEngine.previewMerge`) compartilhando exatamente o mesmo planejador da mesclagem efetiva (zero gravações no Room ou Mercado), exibindo quantos registros serão adicionados, já existem, entraram em conflito (mantendo o local), foram rejeitados ou exigem revisão de vínculos. Na confirmação, insere na ordem topológica, preserva singletons e edições locais, garante coerência financeira (faturas únicas e bloqueio de dupla despesa `"pagamento de fatura"`) e exibe `MergeSummary` legível em pt-BR.
- **Camada Interna de Entrada Estruturada (`data/ingestion/`, Etapa 2):** contrato JSON/DTO interno versionado (`contractVersion = 1`) acoplado ao `NexusRepository` (`processStructuredInputJson` / `processStructuredInputEnvelope`) para ingerir operações de `TASK`, `APPOINTMENT`, `EVENT`, `PROJECT`, `PERSONAL_GOAL` e `TRANSACTION` diretamente no Room `v6` com validação estrita, checagem de FKs e regras financeiras, idempotência por `operationId` em `audit_history`, preservação local em conflitos e rollback atômico seguro (preparado para a Etapa 3, sem acesso externo/GitHub nesta etapa).
- **CSV Financeiro:** exporta `transactions` em UTF-8 com 15 colunas separadas por vírgula.
- **Diagnóstico (`DiagnosticsScreen`):** exibe versão do app/DB, contagem reativa das 27 tabelas e últimos 50 registros de `error_logs` (`GlobalErrorHandler`) com opção de limpeza.


---

## Requisitos transversais em evolução — completude de registros

> Os requisitos abaixo registram a direção funcional aprovada para a Rodada 4. Não significam que já existam no código. A implementação deve respeitar o modelo de dados real, os ADRs vigentes e os critérios de aceite.

| ID | Requisito | Estado |
|---|---|---|
| `REQ-029` | Compromissos e Eventos podem possuir checklist opcional, com itens ordenados e estado de conclusão persistido. O checklist deve ser independente do checklist de tarefas vinculadas e não pode ser confundido com lembretes ou notas. | **PLANEJADA — Rodada 4** |
| `REQ-030` | Compromissos e Eventos podem possuir identidade visual contextual (ícone/categoria visual), para diferenciar atividades como academia, corrida, consulta, viagem, show ou reunião na Agenda e nas fichas. O ícone identifica o tipo/contexto do registro; nunca substitui status, vínculo ou controle de ação. | **PLANEJADA — Rodada 4/5** |
| `REQ-031` | A integração externa deve expor um catálogo versionado e legível por máquina das capacidades e contratos efetivamente suportados pelo aplicativo, separado de qualquer snapshot dos dados pessoais atuais. | **PLANEJADA — Rodada 6** |
| `REQ-032` | O usuário deve poder solicitar sincronização de entrada e saída com prévia/resultado claros. O desenho deve contemplar sincronização manual bidirecional e avaliar automação periódica opcional sem prometer horário exato de execução. | **PLANEJADA — Rodada 6** |

### Regras esperadas para checklists de Compromissos e Eventos (`REQ-029`)
- O checklist é opcional; registros simples não devem ganhar campos ou etapas desnecessárias.
- Cada item tem identidade, título, ordem e estado de conclusão próprios.
- O checklist pertence ao compromisso/evento, não às tarefas relacionadas. Tarefas vinculadas continuam sendo entidades independentes.
- Em registros recorrentes, cada ocorrência deve ter estado próprio; a série pode fornecer um modelo reutilizável, mas marcar um item em uma ocorrência não pode marcar as demais.
- Definir explicitamente como alterações no checklist-base afetam ocorrências passadas, futuras e exceções.
- Considerar backup/restauração/merge, importação estruturada, exportação de estado, notificações e histórico.
- Não persistir mudanças de rascunho antes de salvar/confirmar o editor.

### Regras esperadas para identidade contextual (`REQ-030`)
- Permitir representar contextos reais distintos sem depender apenas do título textual.
- Ícone é metadado de apresentação associado a uma categoria/contexto controlado; não é um emoji arbitrário que o ChatGPT inventa livremente.
- Definir catálogo permitido, fallback, acessibilidade, tema claro/escuro, Agenda, lista, ficha, widgets e backup.
- Diferenciar o ícone do registro do controle de concluir, pagar, editar ou abrir.
- Exemplo de caso a suportar: uma atividade de academia pode usar um ícone de atividade física na Agenda, enquanto sua recorrência, horário e checklist continuam sendo dados estruturados independentes.
