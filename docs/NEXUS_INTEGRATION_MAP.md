# NEXUS — Mapa de Integração e Dependências (`NEXUS_INTEGRATION_MAP`)

> **Finalidade:** mapear formalmente como cada módulo do NEXUS lê, altera, dispara e depende dos demais módulos, garantindo o princípio **"registrar uma vez, ver em vários lugares"**, além de catalogar duplicações e inconsistências hoje existentes no código.

---

## 1. Princípio Arquitetural de Integração

O NEXUS **não copia registros** entre módulos. Toda integração acontece por:
1. **Chaves estrangeiras opcionais e não destrutivas (`ON DELETE SET NULL`)** para vínculos entre entidades independentes (ex.: `transactions.taskId`, `appointments.eventId`, `tasks.projectId`, `tasks.goalId`).
2. **Cascata (`ON DELETE CASCADE`) apenas para partes estritamente subordinadas** ao pai (`subtasks`, `goal_milestones`, `goal_progress_logs`, `credit_card_statements`, `budgets`, `recurrence_exceptions`).
3. **Motores puros compartilhados (`domain/rules/`)** que projetam visões consistentes para múltiplas telas, notificações e widgets:
   - `AgendaEngine` -> Agenda, Dashboard, Widgets (`MyDay`, `NexusSummary`) e (após unificação) Briefing/Revisão.
   - `FinanceRules` -> Financeiro, Dashboard, Widget (`FinanceGlanceWidget`, `NexusSummary`) e Briefing/Revisão.
   - `RecurrenceEngine` (Etapa 4) -> Agenda, Tarefas, Eventos, Notificações.
   - `ReminderRules` -> Editores, `NexusRepository`, `NexusNotificationScheduler`, `NexusReminderSyncWorker`.
   - `BackNavigationRules` -> `NexusAppRoot` e modais/sheets.

---

## 2. Matriz de Integração N×N Entre Módulos

Legenda: **E** = Escreve/Atualiza vínculo · **L** = Lê/Consome reativamente · **D** = Dispara efeito automático (notificação/widget/cálculo).

| Origem \ Impactado | Dashboard | Tarefas | Projetos | Agenda / Compr. | Eventos | Financeiro | Mercado | Objetivos | Rotina | Notif. | Widgets | Backup |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| **Tarefas / Subtarefas** | **L/D** (anel, hoje, atrasos) | — | **D** (% progresso) | **L** (se `dueAt != null`) | **L** (se `eventId != null`) | **E/L** (`taskId`) | — | **D** (% `AUTO` se `goalId`) | **L/D** (adiar/concluir) | **D** (agenda/cancela) | **D** (`MyTasks`, `MyDay`, `Summary`) | **E/L** |
| **Projetos** | **L** (resumo ativos) | **L** (filtro/badge) | — | **L** (prazo `deadlineAt`) | — | **L** (`projectId`) | — | **D** (% `AUTO` se `goalId`) | — | — | — | **E/L** |
| **Compromissos** | **L** (próximo / hoje) | — | — | — | **L** (`eventId`, lista no evento) | — | — | — | **L** (hoje/amanhã) | **D** (alarme) | **D** (`MyDay`, `Summary`) | **E/L** |
| **Eventos** | **L** (próximos) | **L/E** (`tasks.eventId`) | — | **L** (faixa multi-dia) | — | **E/D** (ingresso / parcelas / custos) | — | — | **L** (hoje/amanhã) | **D** (alarme) | **D** (`MyDay`) | **E/L** (+ ZIP `banners/`) |
| **Financeiro (Trans., Cartões, Faturas)** | **L/D** (resumo, anel, alertas) | **L** (`Gasto: R$ X` via `taskId`) | **L** (custos do projeto) | **L** (`statementId == null` + faturas > 0) | **L/D** (atualiza `ticketStatus` e custo pago) | — | — | — | **L** (contas `EXPENSE` e faturas) | **D** (vencimento) | **D** (`Finance`, `Summary`) | **E/L** (+ CSV) |
| **Mercado** | **L** (bloco Mercado) | — | — | — | — | — | — | — | — | — | — | **Opcional** (sem token!) |
| **Objetivos Pessoais** | **L** (bloco Metas) | **L** (`goalId`) | **L** (`goalId`) | — | — | — | — | — | — | — | — | **E/L** |
| **Recorrências (Etapa 4)** | **L/D** (anel mensal) | **D** (gera próxima ao concluir) | **D** (se tarefa de projeto) | **D** (expande ocorrências em leitura) | **D** (expande série em leitura) | — | — | — | **L** (amanhã) | **D** (janela 30d) | **D** (`MyDay`, `MyTasks`) | **E/L** |
| **Rotina / Notificações** | **L** (status briefing) | **E** (concluir / adiar) | — | — | — | **E** (marcar pago / adiar) | — | — | — | — | **D** (sync pós-ação) | **E/L** |
| **Configurações / Personalização** | **D** (blocos, saudação, tema) | **L** (categorias, tema) | **L** | **L** (categorias) | **L** (categorias) | **L** (categorias, saldo) | **E** (token opcional) | **L** | **L** (horários/canais) | **D** (canais/silêncio) | **D** (tema / refresh) | **E/L** (sem token) |
| **Backup / Restore** | **D** (reload total) | **D** | **D** | **D** | **D** (+ `banners/`) | **D** | — | **D** | **D** | **D** (resync geral) | **D** (updateAll) | — |

---

## 3. Fluxos de Integração Críticos (Passo a Passo)

### Fluxo A — Tarefa -> Projeto -> Objetivo Pessoal
1. Tarefa (`tasks`) pode referenciar `projectId` e/ou `goalId`.
2. Projeto (`projects`) pode referenciar `goalId`.
3. Ao alternar conclusão da tarefa:
   - Atualiza `tasks.status` e `completedAt`.
   - `ProjectWithProgress` recalcula `completedTasks / totalTasks` reativamente via `Flow`.
   - `PersonalGoalWithDetails` (modo `AUTO`) recalcula percentual somando marcos, tarefas vinculadas (`tasks.goalId`) e projetos vinculados (`projects.goalId`).
   - Cancela ou reativa lembrete da tarefa em `reminders` e agenda atualização de `MyTasksGlanceWidget` / `MyDayGlanceWidget`.

### Fluxo B — Tarefa -> Gasto (`"Registrar gasto"`)
1. Na tarefa, o botão `"Registrar gasto"` pré-carrega `TransactionEditorSheet` com `type = EXPENSE`, `description = task.title`, `taskId = task.id`, `eventId = task.eventId`, `projectId = task.projectId` e `categoryId = AgendaEngine.suggestExpenseCategoryId(...)`.
2. Usuário informa **valor** e **forma de pagamento** (e ajusta categoria/vencimento se quiser).
3. Ao salvar a transação:
   - Aparece imediatamente no módulo **Financeiro** (com indicação `"Tarefa: <título>"` no `TransactionWithDetails` — *correção pendente*).
   - Soma no total `"Gasto: R$ X"` exibido na própria tarefa (`TaskWithDetails.totalExpenseCents`).
   - Se a transação for paga à vista (sem cartão) e não estiver quitada com `dueAt`, aparece na Agenda; se já for marcada `SETTLED`, fica oculta por padrão na Agenda (`showPaid = false`); se for no **Cartão**, entra na fatura sem duplicar na Agenda.

### Fluxo C — Tarefa Recorrente -> Conclusão -> Histórico -> Agenda
1. Usuário cria regra em `recurrences` (`ownerType = TASK`) vinculada à tarefa inicial (`recurrenceId`, `occurrenceDateIso`).
2. `AgendaEngine` projeta ocorrências futuras da regra na janela da Agenda (Dia/Semana/Mês) **sem gravar cópias no SQLite**.
3. `NexusNotificationScheduler` agenda lembretes para ocorrências futuras dentro da janela de 30 dias.
4. Quando o usuário conclui a ocorrência atual:
   - A tarefa atual permanece salva com `status = COMPLETED` e `completedAt` (histórico preservado junto de suas subtarefas e gastos).
   - O repositório calcula a próxima data válida da série (respeitando `untilDateIso`, `maxCount` e `recurrence_exceptions`) e cria a próxima linha em `tasks` de forma idempotente via índice `UNIQUE(recurrenceId, occurrenceDateIso)`.
5. Quando o usuário **adia** uma ocorrência atrasada: altera o `dueAt` dessa instância (ou cria exceção `OVERRIDDEN`), mantendo-a contabilizada e acessível até conclusão sem deslocar o calendário futuro da série.

### Fluxo D — Evento <-> Compromissos + Tarefas + Financeiro
1. Evento é criado com `locationMode` (`Presencial | Online | A definir`), banner opcional em `filesDir/event_banners/<id>.jpg` e preço real de ingresso (`ticketPriceCents × ticketQuantity`).
2. **Se ingresso gratuito (`FREE`):** nenhuma transação é gerada; eventuais obrigações anteriores em aberto são removidas.
3. **Se ingresso com obrigação financeira:**
   - Parcelado no cartão (`installmentsCount > 1`): gera `InstallmentPurchaseEntity` + `N` `TransactionEntity` (`isTicketObligation = true`, `eventCostType = TICKET`), cada uma atribuída à respectiva fatura (`statementId`).
   - Pagamento único: gera 1 `TransactionEntity` vinculada ao evento (e à fatura se cartão).
4. **Custos extras opcionais:** qualquer transação criada com `eventId` soma em `EventWithDetails.estimatedTotalCostCents` / `paidTotalCostCents` sem obrigar preenchimento.
5. **Compromissos vinculados (`appointments.eventId`):** aparecem na Agenda normalmente e são listados dentro do detalhe do Evento, permitindo agenda multi-horário dentro de congressos/festivais de múltiplos dias.
6. **Pagar transação/fatura de ingresso:** dispara sincronização automática de `EventEntity.ticketStatus` (`PARTIALLY_PAID` -> `PAID`).

### Fluxo E — Compra no Cartão -> Fatura -> Pagamento (Zero Duplicação)
1. Despesa com `PaymentMethod.CREDIT_CARD` e `creditCardId != null` é vinculada à fatura correspondente ao `closingDay` do cartão (`ensureStatementForCardAndDate`).
2. **Nos cálculos por categoria e fluxo de despesas (`FinanceMonthSummary` / `DonutChart`):** conta a **compra original** no mês correspondente (nunca o pagamento da fatura).
3. **Na Agenda (`AgendaEngine`):** transações com `statementId != null` são **suprimidas** individualmente; apenas a **fatura** (`credit_card_statements`) com total `> 0` aparece no dia do vencimento (`dueAt`).
4. **Ao pagar fatura (`payCreditCardStatement`):** marca todas as transações da fatura como `SETTLED`, atualiza a fatura como `PAID` com `paidAmountCents`, sincroniza eventuais ingressos de evento ligados a ela e cancela lembretes da fatura — **sem criar segunda transação de despesa**.

---

## 4. Regras de Exclusão e Integridade Referencial

| Registro Excluído | Efeito nos Relacionamentos | Regra de UX |
| --- | --- | --- |
| **Tarefa** | `subtasks` (`CASCADE`); `transactions.taskId` (`SET_NULL`); cancela lembrete. | Confirmar avisando se há gastos vinculados (desvincula, não apaga gastos). |
| **Projeto** | `tasks.projectId`, `transactions.projectId`, `installment_purchases.projectId` (`SET_NULL`). | Confirmar avisando quantas tarefas/lançamentos ficarão sem projeto. |
| **Evento** | `tasks.eventId`, `appointments.eventId`, `transactions.eventId`, `installment_purchases.eventId` (`SET_NULL`); remove `bannerPath` físico; cancela lembrete. | Confirmar se há vínculos; decidir/informar explicitamente sobre obrigações financeiras não pagas (ex.: cancelar evento cancela obrigações em aberto; excluir desvincula ou remove conforme confirmação explícita). |
| **Cartão de Crédito** | `credit_card_statements` (`CASCADE`); `transactions.creditCardId`, `installment_purchases.creditCardId`, `recurring_rules.creditCardId`, `events.creditCardId` (`SET_NULL`). | `ConfirmDangerDialog` explicando remoção de faturas e desvínculo dos lançamentos históricos. |
| **Categoria** | `budgets` (`CASCADE`); demais entidades (`SET_NULL`). | Avisar caso existam orçamentos vinculados à categoria. |
| **Objetivo Pessoal** | `goal_milestones`, `goal_progress_logs` (`CASCADE`); `tasks.goalId`, `projects.goalId` (`SET_NULL`). | Confirmar que tarefas/projetos vinculados permanecem intactos. |

---

## 5. Duplicações, Inconsistências e Dependências Identificadas no Código Atual

Esta lista reúne **todos** os desvios reais encontrados na auditoria do código atual e que devem ser sanados conforme o `NEXUS_IMPLEMENTATION_ROADMAP.md`:

| ID | Tipo | Onde Está no Código | Descrição do Problema | Correção Planejada |
| --- | --- | --- | --- | --- |
| **INT-01** | Duplicação | `NexusRepository.observeUnifiedAgenda` (`~4250-4360`) + `DomainModels.AgendaItem` vs `AgendaEngine` | Existem dois motores de agenda paralelos; `Briefing`/`NightReview` e `DashboardSnapshot` ainda usam `AgendaItem`/`observeUnifiedAgenda` em vez de `AgendaEngine`. | Migrar briefings e dashboard para `AgendaEngine` e aposentar `observeUnifiedAgenda` gradualmente. |
| **INT-02** | Inconsistência | `DomainModels.kt:56-62`, `FinanceScreen.kt:731-739`, `NexusRepository` | `TransactionWithDetails` não carrega `task: TaskEntity?`; portanto `TransactionRowCard` no Financeiro não exibe quando o gasto veio de uma tarefa. | Adicionar `task: TaskEntity? = null` em `TransactionWithDetails`, popular no repository e exibir `"Tarefa: ..."` no card. |
| **INT-03** | Inconsistência | `MainActivity.kt:854-882` + `EntityEditors.kt:690-709` | (a) Clicar `"Registrar gasto"` salva a tarefa imediatamente antes mesmo de o usuário salvar o gasto; (b) Toggling de subtarefa já existente dentro de `TaskEditorSheet` persiste no Room antes de clicar "Salvar". | Adiar persistência de subtarefas para o botão "Salvar" do editor (mantendo rascunho em memória até salvar) e tornar o fluxo de criação de gasto explícito/seguro quanto a tarefa nova vs existente. |
| **INT-04** | Inconsistência | `NexusRepository.kt:920-930, 1006-1012, 2581` | `observeDailyBriefingSnapshot` e `tomorrowBills` não filtram `TransactionType.EXPENSE` nem `statementId == null` (somando receitas e compras individuais já cobertas pela fatura); `sendTestNotification` ignora compromissos multi-dia em curso hoje. | Alinhar filtros com `AgendaEngine` (`EXPENSE` + `statementId == null` + multi-dia em curso). |
| **INT-05** | Inconsistência | `NexusRepository.kt:1484, 1539` | `saveEvent` grava `FinancialStatus.OVERDUE` literal no SQLite para obrigações de ingresso vencidas, enquanto `saveTransaction` e `FinanceRules` mantêm `PENDING` no banco e calculam `OVERDUE` na leitura. | Gravar `FinancialStatus.PENDING` no banco em `saveEvent` e depender exclusivamente de `FinanceRules.effectiveTransactionStatus`. |
| **INT-06** | Inconsistência | `NexusRepository.kt:785` (`observeGlobalSearch`) | `computeAppointmentConflicts` é calculado somente sobre `matchedAppointments` (subconjunto buscado) em vez de toda a lista de compromissos. | Calcular `conflictMap` com `allAppointments` antes de filtrar para os resultados da busca. |
| **INT-07** | Inconsistência | `FinanceEditors.kt:136-138` | Quando `TransactionEditorSheet` recebe `initialEventId != null` (inclusive vindo de tarefa ligada a evento), define `eventCostType = EventCostType.OTHER` automaticamente. | Permitir `eventCostType` opcional ou contextualizado claramente no editor sem forçar classificação imprecisa. |
| **INT-08** | Inconsistência | `MainActivity.kt:258` + `MainActivity.kt:547, 613` | `BackHandler` passa `editorHasUnsavedChanges = false` fixo (nunca dispara confirmação de descarte ao pressionar Voltar do sistema); `DashboardCustomization` e `RoutineAssistant` voltam sempre para `SubRoute.None` ignorando se vieram de `MoreMenu`. | Expor callback/estado `hasUnsavedChanges` do editor ativo para `BackHandler` e rastrear sub-rota de origem (`previousSubRoute`). |
| **INT-09** | Lacuna de Integração | `FinanceScreen.kt:371` e `MoreSettingsBackupDiagnosticsScreens.kt:336` | Ambos passam `onToggleSettle = {}` no-op no card `TransactionRowCard` ("Próximo vencimento" e "Pesquisa Global"). | Conectar ao `viewModel::toggleSettleTransaction`. |
| **INT-10** | Lacuna Visual | `NexusWidgetTheme.kt:43-57` | Widgets usam cores escuras fixas mesmo quando o app e o sistema estão em tema claro. | Suportar `day`/`night` nos tokens Glance alinhados ao `NexusTheme`. |
