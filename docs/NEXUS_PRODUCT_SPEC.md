# NEXUS — Especificação Definitiva do Produto (`NEXUS_PRODUCT_SPEC`)

> **Finalidade:** fonte definitiva da proposta funcional, comportamental e estrutural do NEXUS como centro de controle pessoal e profissional offline-first (com exceção opcional e isolada do bloco Mercado).
> **Base:** evolui o aplicativo existente em Kotlin + Jetpack Compose + Material 3 + Room + Glance (atualmente na baseline `5.2.0` / Room `v5`) sem reescrita gratuita e sem remover capacidades válidas existentes.

---

## 1. Visão Central do Produto

O **NEXUS** é um centro de controle pessoal e profissional projetado para **reduzir a carga mental** do usuário por meio da centralização coerente de tarefas, subtarefas, projetos, compromissos, eventos, finanças, objetivos, rotina e cotações de mercado em um único fluxo integrado.

### Princípios Fundamentais
1. **Registrar uma vez, enxergar em vários contextos:** nenhum dado é duplicado manualmente entre módulos. Uma tarefa com data aparece automaticamente na Agenda; uma compra no cartão compõe a fatura e o resumo financeiro do mês; o gasto criado em uma tarefa mantém o vínculo bidirecional nos dois módulos.
2. **Verdade única para cálculos:** regras de agenda (`AgendaEngine`), finanças (`FinanceRules`), lembretes (`ReminderRules`), recorrência (`RecurrenceEngine`) e navegação (`BackNavigationRules`) residem em funções puras compartilhadas por telas, briefings e widgets.
3. **Zero dados fictícios e zero cobranças artificiais:** todo número exibido vem do banco local (ou cache oficial do Mercado após busca real). O app nunca inventa taxas, despesas inexistentes ou mensagens punitivas em estados vazios.
4. **Offline-first com permissão enxuta:** 12 dos 13 módulos funcionam 100% offline. Apenas o bloco opcional **Mercado** utiliza `INTERNET` quando ativo pelo usuário, com cache persistente e fallback silencioso.
5. **Idioma e padrões regionais fixos:** português brasileiro (`pt-BR`), moeda Real (`R$` / `BRL`), datas `dd/MM/yyyy`, relógio 24h (`HH:mm`), fuso `America/Sao_Paulo`.

---

## 2. Cadastro Rápido (`QuickAddChooser`) e Fluxo Contextual

O botão flutuante `+` e atalhos contextuais abrem o seletor enxuto `QuickAddChooserSheet` com **4 opções diretas** e descrição de 1 linha:

| Opção | Descrição de 1 Linha | Comportamento do Formulário |
| --- | --- | --- |
| **Tarefa** | Algo para fazer, com ou sem data | Abre apenas título, "Quando" (opcional), prioridade/categoria essenciais + botão expandível **"Mais detalhes"** (projeto, evento, objetivo, checklist, repetição, lembrete, notas). |
| **Compromisso** | Tempo reservado, com horário | Abre título, data, início/término (ou dia inteiro) + **"Mais detalhes"** (categoria, evento vinculado, local, link, repetição, lembrete, notas). |
| **Evento** | Ocasião com nome, local ou ingresso | Abre nome, início/término, modo de local (`Presencial \| Online \| A definir`) + **"Mais detalhes"** (ingresso/preço real, parcelamento, banner, categoria, notas). |
| **Gasto ou receita** | Dinheiro que entrou ou saiu | Abre tipo (Despesa/Receita), descrição, valor e forma de pagamento + **"Mais detalhes"** (status, vencimento, categoria, cartão, projeto, evento, tarefa, notas). |

**Preenchimento contextual automático:**
- Aberto em um dia selecionado na **Agenda**: pré-preenche a data correspondente.
- Aberto dentro de um **Evento**: vincula `eventId` automaticamente.
- Aberto a partir de uma **Tarefa** ("Registrar gasto"): vincula `taskId` (e `eventId` se a tarefa pertencer a um evento), preenche descrição com o título da tarefa e sugere categoria financeira de mesmo nome (se houver).

---

## 3. Especificação Completa dos 13 Módulos

---

### Módulo 1 — Dashboard / Tela Inicial (Redesenho Conceitual)

#### 1.1 Objetivo
Entregar um **resumo visual inteligente** do dia e do mês logo na abertura do app, respondendo em segundos: *"Como estou hoje, como está meu mês, o que exige atenção imediata e para onde vai meu dinheiro?"* — **sem transformar a tela inicial em uma lista extensa de tarefas pendentes**.

#### 1.2 Funcionalidades
- Cabeçalho com monograma **N** à esquerda, saudação hierárquica + data formatada, busca global e menu de acesso rápido à direita (recolhendo discretamente na rolagem se estável).
- **Frase diária** opcional da biblioteca local offline.
- **Bloco Central de Tarefas (`TaskRingChart` + Visão Mensal):**
  - Alternância rápida entre **mês anterior, mês atual e próximo mês** (`< mês/ano >`).
  - Resumo de **hoje** em destaque compacto (pendentes hoje / concluídas hoje).
  - Gráfico de anel central proporcional com os 4 indicadores da janela mensal/ativa: **Concluídas**, **Pendentes**, **Em andamento** e **Atrasadas**.
- **Alertas de Atenção Real:** seção compacta exibida *somente quando houver* itens críticos reais (tarefas urgentes/atrasadas, contas vencidas ou vencendo hoje, faturas abertas vencendo, conflitos de compromisso hoje).
- **Resumo Financeiro com Gráfico de Anel (`DonutChart`):**
  - Receita realizada/prevista vs despesa paga/prevista do mês corrente + resultado líquido do mês + saldo declarado (quando informado).
  - Gráfico de anel compacto de gastos por categoria do mês corrente.
- **Indicadores de Projetos e Objetivos:** contagem de projetos ativos com barra média/próximos prazos e objetivos pessoais em andamento com percentual atual.
- **Próximos Compromissos e Eventos:** mostra o próximo compromisso de hoje (ou próximos dias) e próximos eventos confirmados/planejados em formato compacto (máximo configurável via `maxItems`).
- **Bloco Mercado:** mini-painel com Dólar (`USD-BRL`), Ibovespa (`^BVSP`, quando token configurado) e opcionalmente `EUR-BRL` / `BTC-BRL`, com valor atual, variação percentual positiva/negativa do dia e `Sparkline` de 30 dias.
- **Personalização completa:** todos os blocos podem ser ligados/desligados e reordenados em `DashboardCustomizationScreen` (inclusive modo compacto).

#### 1.3 Informações Apresentadas (Direto na Home vs Outras Telas)

| Informação | Exibida Diretamente na Home | Destino ao Tocar (Drill-down) |
| --- | --- | --- |
| Saudação e data | Sim | Toque discreto abre Personalização |
| Anel de tarefas + 4 contadores | Sim (resumo visual, **não** lista longa) | Aba **Tarefas** (já filtrada pelo status/aba correspondente ou mês) |
| Resumo de tarefas de hoje | Sim (contagem + até 2–3 prioridades configuráveis) | Editor da Tarefa ou Aba **Tarefas** |
| Tarefas/contas atrasadas críticas | Sim, apenas contagem e até 2–3 alertas críticos | Aba **Tarefas (Atrasadas)** ou **Financeiro** |
| Anel financeiro + totais do mês | Sim (Receitas, Despesas, Resultado do mês, Saldo declarado) | Aba **Financeiro (Visão Geral)** |
| Projetos e objetivos | Sim (barras compactas dos principais ativos) | **Projetos** / **Objetivos Pessoais** |
| Próximo compromisso / eventos | Sim (próximo item + até 2–3 próximos) | **Agenda** ou detalhe do Compromisso/Evento |
| Mercado (`^BVSP`, `USD-BRL`, etc.) | Sim (preço, variação +/- e Sparkline 30d) | Modal/expansão de atualização ou Config de Token |

#### 1.4 Telas e Componentes
- `HomeScreen` (`LazyColumn` modular por blocos configuráveis).
- Componentes reutilizáveis: `NexusMonogramHeader`, `TaskRingChart`, `DonutChart`, `ProgressBarRow`, `Sparkline`, `AlertBannerCard`, `MonthYearSelectorBar`, `NexusCard`, `NexusEmptyState`.

#### 1.5 Regras de Negócio
- Tarefas exibidas/contabilizadas no bloco mensal filtram tarefas com `dueAt` no mês selecionado (tarefas sem data ficam disponíveis no módulo Tarefas e opcionalmente no indicador geral de backlog sem poluir a visão mensal).
- Totais financeiros do Dashboard usam exatamente `FinanceRules.computeMonthlyFinanceSummary` (mês corrente para realizados/previstos; todas as datas para atrasadas).
- Frase diária seleciona 1 registro ativo por `dateIso` sem repetição imediata das últimas 2 frases quando há `>= 2` ativas.
- Alertas críticos nunca são inventados: se não há atraso, vencimento hoje ou conflito, o bloco de alertas não aparece (evita ruído visual).

#### 1.6 Estados Possíveis
1. **Banco vazio (primeiro uso):** exibe saudação + cartão acolhedor `"Seu centro de comando está pronto"` com CTA para primeiro registro.
2. **Todos os blocos ocultos:** `NexusEmptyState` com botão `"Personalizar dashboard"`.
3. **Uso normal populado:** blocos ordenados conforme `dashboardBlocksJson`.
4. **Offline para Mercado:** exibe último cache salvo com horário `"atualizado em dd/MM HH:mm"` ou estado neutro sem token/dados.

#### 1.7 Ações Disponíveis
- Trocar mês no bloco de tarefas (`< mês >`).
- Tocar em qualquer métrica/anel/compromisso/evento/objetivo para navegar ou abrir editor.
- Marcar tarefa prioritária diretamente pelo atalho de conclusão rápida (quando exibida) ou abrir busca global / assistente de rotina / personalização.
- Puxar para atualizar (`pull-to-refresh`) quando bloco Mercado estiver ativo.

#### 1.8 Integrações com Outros Módulos
Consome em modo leitura/reativo: Tarefas, Subtarefas, Projetos, Agenda (`AgendaEngine`), Eventos, Financeiro (`FinanceRules`), Objetivos Pessoais, Assistente de Rotina, Mercado e Personalização/Frases.

#### 1.9 Informações Armazenadas
`personalization_settings` (flags, `greetingTemplate`, `dateFormatPattern`, `compactDashboardMode`, `dashboardBlocksJson`, histórico de citação do dia), além de ler dados das demais tabelas.

#### 1.10 Comportamentos Excepcionais
- JSON de blocos corrompido em `dashboardBlocksJson`: faz fallback seguro para `defaultDashboardBlocks()` sem crash.
- Falha ao buscar cotações online: nunca bloqueia renderização da Home e nunca mostra erro técnico HTTP.

---

### Módulo 2 — Tarefas, Subtarefas e Recorrência

#### 2.1 Objetivo
Centralizar toda ação executável do usuário com suporte a prazos opcionais, subtarefas em checklist, organização em lista ou Kanban, conversão direta em gasto financeiro e **recorrência completa sem duplicações**.

#### 2.2 Funcionalidades
- Criação/edição com título, descrição, **"Quando"** (`dueAt` + `hasDueTime`), prioridade (`LOW`, `NORMAL`, `HIGH`, `URGENT`), status (`NOT_STARTED`, `IN_PROGRESS`, `WAITING`, `BLOCKED`, `COMPLETED`, `CANCELLED`), categoria, projeto, evento, objetivo pessoal, notas e lembrete customizável.
- **Checklist leve de subtarefas:** adicionar ao final, marcar/desmarcar e excluir com persistência atômica apenas ao confirmar salvamento (ou toggle rápido direto na lista/detalhe salvo).
- **Kanban:** visão horizontal por todos os 6 status sobre a mesma tabela `tasks`, com movimentação rápida.
- **Botão `"Registrar gasto"`:** abre `TransactionEditorSheet` vinculado à tarefa em qualquer status.
- **Sistema Completo de Recorrência:**
  - Frequências: Não repete, Todos os dias, Dias específicos da semana (ex.: Seg/Qua/Sex), Fins de semana, Toda semana, A cada 2 semanas (quinzenal), Todo mês, Todo ano, Intervalo personalizado (a cada `N` dias ou `N` semanas).
  - Término: Sem data final (contínuo), Em uma data específica (`untilDateIso`), Após `N` ocorrências (`maxCount`).
  - Edição granular: `"Só esta ocorrência"` (cria exceção) vs `"Toda a série"` (atualiza regra).
  - Adiamento: permite remarcar uma ocorrência (`OVERRIDDEN` ou novo `dueAt`) **sem perder o histórico nem apagar ocorrências atrasadas não concluídas**.

#### 2.3 Informações Apresentadas
- Lista: título, prioridade visual, badge de atraso, contagem de subtarefas (`2/4`), rótulo de `"Quando"`, nome de projeto/evento/objetivo, badge de recorrência e resumo financeiro `"Gasto: R$ X"` quando houver transações vinculadas.
- Abas: `Lista`, `Kanban`, `Atrasadas`, `Concluídas`, `Projetos`.
- Histórico da série (no editor de tarefa recorrente): ocorrências concluídas e pendentes vinculadas à mesma `recurrenceId`.

#### 2.4 Telas e Componentes
- `TasksAndProjectsScreen` (Lista, Kanban, Atrasadas, Concluídas, Projetos).
- `TaskEditorSheet` (com checklist, seletor `"Quando"`, bloco de recorrência e botão `"Registrar gasto"`).
- `RecurrenceScopeDialog` (`"Só esta"` / `"Toda a série"`).

#### 2.5 Regras de Negócio (Anti-duplicação & Recorrência)
1. **Regra de criação de ocorrência de Tarefa Recorrente:**
   - Uma série de tarefa recorrente armazena a regra na tabela `recurrences` (`ownerType = TASK`) e mantém **apenas a ocorrência atual/pendente em aberto** na tabela `tasks` com `recurrenceId` + `occurrenceDateIso` (`UNIQUE(recurrenceId, occurrenceDateIso)`).
   - **Ocorrências futuras ainda não iniciadas NÃO são pré-criadas em massa** na tabela `tasks`. Na Agenda (`AgendaEngine`), ocorrências futuras da regra dentro da janela visual (`window`) são projetadas virtualmente em memória para o usuário enxergar seu planejamento futuro sem poluir a lista de tarefas nem gerar centenas de linhas no SQLite.
   - **Ao concluir** uma tarefa recorrente (`COMPLETED`), o repositório calcula a próxima data válida segundo a regra (pulando exceções `CANCELLED` e respeitando `untilDateIso`/`maxCount`) e cria **uma única nova linha em `tasks`** de forma idempotente (`UNIQUE(recurrenceId, occurrenceDateIso)`). A tarefa concluída permanece intacta no histórico (com seu `completedAt` e eventuais gastos vinculados).
   - **Ao reabrir** uma tarefa concluída cujo sucessor já existe, nunca cria duplicata por força do índice único e verificação prévia.
2. **Tarefas atrasadas e adiamentos:**
   - Uma ocorrência vencida e não concluída **permanece contabilizada como atrasada** até ser concluída, cancelada ou explicitamente reagendada.
   - Se o usuário **adiar** uma ocorrência atrasada (ex.: para amanhã ou na Revisão Noturna): grava-se o novo `dueAt` (ou exceção `OVERRIDDEN` com registro de que foi adiada se necessário manter rastreabilidade), e **novas ocorrências futuras continuam seguindo a cadência original da série** sem perder o vínculo com a série nem duplicar a data adiada.
   - Caso uma série acumule atraso anterior e a ocorrência anterior não tenha sido concluída, a lista "Atrasadas" continua exibindo a ocorrência pendente vencida até resolução — evitando "sumiço silencioso" de pendências.
3. **Vínculo de gasto:** `"Registrar gasto"` exige primeiro garantir ID válido da tarefa — porém nunca deve gravar alterações de formulário de tarefa às escondidas se o usuário cancelar; deve confirmar ou só vincular ao confirmar, mantendo 1 tarefa -> `0..N` transações (`transactions.taskId`).

#### 2.6 Estados Possíveis
`NOT_STARTED`, `IN_PROGRESS`, `WAITING`, `BLOCKED`, `COMPLETED`, `CANCELLED` + flag derivada de leitura `isOverdue` (quando não terminal e `dueAt` anterior a agora/hoje).

#### 2.7 Ações Disponíveis
Criar, editar, duplicar/transformar em recorrente, concluir/reabrir (1 toque), mover status no Kanban, adicionar/marcar/remover subtarefas, registrar gasto vinculado, adiar para amanhã, configurar lembrete, excluir (desvinculando gastos sem apagá-los).

#### 2.8 Integrações com Outros Módulos
- **Projetos:** atualiza progresso `completedTasks / totalTasks`.
- **Objetivos Pessoais:** contribui para modo `AUTO` quando `goalId != null`.
- **Agenda:** aparece em `AgendaEngine` se `dueAt != null` (e projeção futura quando recorrente).
- **Financeiro:** `transactions.taskId` soma em `"Gasto: R$ X"` na tarefa e exibe `"Tarefa: <título>"` no lançamento.
- **Rotina, Notificações e Widgets:** feeds diretos respeitando status terminal e `ReminderRules`.

#### 2.9 Informações Armazenadas
- `tasks` (`id, title, description, projectId?, categoryId?, eventId?, goalId?, priority, status, dueAt?, hasDueTime, notes, completedAt?, recurrenceId?, occurrenceDateIso?, createdAt, updatedAt`).
- `subtasks` (`id, taskId, title, isCompleted, sortOrder, ...`).
- `recurrences` e `recurrence_exceptions` (Etapa 4).

#### 2.10 Comportamentos Excepcionais
- Tarefa sem data (`dueAt == null`): salva normalmente, aparece em Tarefas/Projetos/Kanban, **nunca** aparece na Agenda e não exige recorrência.
- Regra de recorrência com dia 31 em mês de 30/28 dias: ajusta para o último dia válido daquele mês (`30`, `28` ou `29` em ano bissexto) sem pular o mês nem travar no próximo mês.
- Excluir série inteira: pergunta se remove ocorrências futuras (cancela regra e remove projeções) preservando ocorrências já concluídas e gastos vinculados no histórico.

---

### Módulo 3 — Projetos

#### 3.1 Objetivo
Agrupar iniciativas maiores com prazo opcional, acompanhamento percentual automático de tarefas e consolidação de custos reais/previstos.

#### 3.2 Funcionalidades
- Criação/edição de projeto (nome, descrição, prazo, status, prioridade, vínculo opcional com Objetivo Pessoal).
- Visualização de barra de progresso automática (`concluídas / total`), lista de tarefas associadas e resumo de custos estimados e pagos (`transactions.projectId` + `installment_purchases.projectId`).

#### 3.3 Informações Apresentadas
Nome, status (`Planejado`, `Em andamento`, `Pausado`, `Concluído`, `Arquivado`), prioridade, prazo, percentual/contagem de tarefas, custos estimados vs pagos e objetivo vinculado.

#### 3.4 Telas e Componentes
Aba `PROJECTS` em `TasksAndProjectsScreen`, `ProjectEditorSheet`, resumo em `PlanningGoalsScreen` e bloco opcional no Dashboard.

#### 3.5 Regras de Negócio
- Progresso = `0%` quando `totalTasks == 0`; senão `completedTasks / totalTasks`.
- Projeto com `deadlineAt != null` e status ativo aparece na Agenda como item tipo `PROJECT` (bandeira).
- Excluir projeto desvincula tarefas, transações e parcelamentos (`SET_NULL`) sem apagá-los, com confirmação prévia se houver itens vinculados.

#### 3.6 Estados Possíveis
`PLANNED`, `ACTIVE`, `ON_HOLD`, `COMPLETED`, `ARCHIVED`.

#### 3.7 Ações Disponíveis
Criar, editar, abrir tarefa vinculada, vincular a objetivo pessoal, excluir projeto.

#### 3.8 Integrações com Outros Módulos
Tarefas (`tasks.projectId`), Financeiro (`transactions.projectId`, `installment_purchases.projectId`), Objetivos Pessoais (`projects.goalId`), Agenda (`deadlineAt`), Dashboard.

#### 3.9 Informações Armazenadas
Tabela `projects`.

#### 3.10 Comportamentos Excepcionais
Projeto sem tarefas ou sem gastos exibe progresso `0%` e custo zero de forma limpa, sem divisão por zero.

---

### Módulo 4 — Agenda e Compromissos

#### 4.1 Objetivo
Unificar em uma única linha do tempo cronológica tudo o que ocupa tempo ou exige ação em data marcada — compromissos, tarefas com data, eventos, contas a vencer, faturas e prazos de projetos — sem duplicar registros no banco.

#### 4.2 Funcionalidades
- **4 modos de visualização:**
  - **Dia:** faixa superior de eventos/itens de dia inteiro + linha horária e itens do dia.
  - **Semana:** 7 dias (segunda a domingo) com setas `< semana anterior | próxima semana >`, cabeçalho do intervalo e agrupamento diário.
  - **Mês:** grade mensal com marcadores visuais nos dias ocupados; toque no dia atualiza seleção e lista abaixo (com resumo/contagem clara dos itens).
  - **Lista:** visão cronológica contínua de ocorrências futuras/passadas na janela.
- **Filtros combináveis:** chips de tipo (`Tudo`, `Tarefas`, `Compromissos`, `Eventos`, `Financeiro`, `Projetos`), busca textual, categoria e interruptor **"Mostrar já pagos"** (padrão desligado).
- **Compromissos (`appointments`):** horários início/fim, dia inteiro, categoria, local, link, vínculo opcional com Evento (`eventId`), detecção automática de conflito de horário entre compromissos com hora marcada e recorrência (Etapa 4).

#### 4.3 Informações Apresentadas
- Ocorrências ordenadas por data -> eventos multi-dia/dia inteiro no topo -> horário de início -> título.
- Sinalização visual por **ícone/forma** (seção 6 do Design System) e faixa de conflito quando dois compromissos se sobrepõem.

#### 4.4 Telas e Componentes
`AgendaScreen`, `AppointmentEditorSheet`, motor `AgendaEngine` + `DateRange` + `AgendaFilters`.

#### 4.5 Regras de Negócio
- **Fonte única:** todas as visões (Dia, Semana, Mês, Lista), widgets e Home usam `AgendaEngine.expand(...)`.
- **Financeiro na Agenda:** transações não canceladas com `dueAt != null` e **`statementId == null`** (compras de cartão vinculadas a fatura NÃO aparecem individualmente; a fatura não paga com `total > 0` aparece no dia do vencimento). Itens `SETTLED`/`PAID` ficam ocultos salvo se `"Mostrar já pagos"` estiver ligado.
- **Multi-dia:** eventos e compromissos que atravessam múltiplos dias geram uma `AgendaOccurrence` em cada dia do intervalo dentro da janela solicitada, exibidos como faixa `"dia inteiro"` no topo do dia.
- **Compromisso recorrente:** grava apenas 1 regra em `recurrences`; `AgendaEngine` calcula ocorrências na janela sem gravar cópias no banco. Edição/cancelamento oferece `"Só esta"` (exceção) ou `"Toda a série"`.
- **Conflitos:** comparação sempre sobre todos os compromissos da janela (inclusive na Busca Global).

#### 4.6 Estados Possíveis
Ocorrência normal, concluída/paga (riscada/atenuada), atrasada (`danger`), em conflito (`warning`).

#### 4.7 Ações Disponíveis
Alternar visão (`Dia/Semana/Mês/Lista`), navegar datas/semanas/meses, ir para Hoje, filtrar por tipo/categoria/texto/pagos, tocar em qualquer ocorrência para abrir o editor/origem real (ou aba Cartões para fatura), criar compromisso na data selecionada.

#### 4.8 Integrações com Outros Módulos
Lê Compromissos, Tarefas (`dueAt`), Eventos (inclusive multi-dia), Transações (`statementId == null`), Faturas de Cartão (`total > 0`), Projetos (`deadlineAt`) e Recorrências.

#### 4.9 Informações Armazenadas
`appointments` (com `eventId?`), `recurrences` e `recurrence_exceptions`.

#### 4.10 Comportamentos Excepcionais
- Dia sem ocorrências: exibe `"Nada marcado para este dia"` de forma neutra.
- Compromisso com `endAt < startAt`: editor impede ou corrige para manter `endAt >= startAt` (mínimo 30–60 min padrão se mesmo horário não for desejado).

---

### Módulo 5 — Eventos (Modelo Revisado)

#### 5.1 Objetivo
Gerenciar ocasiões pontuais ou recorrentes (congressos, cursos, shows, viagens, encontros) com **preço real de ingresso**, localização flexível e opcional, banner visual próprio e visão consolidada de compromissos, tarefas e gastos ligados ao evento.

#### 5.2 Funcionalidades
- Cadastro de nome, descrição, categoria, início/fim, dia inteiro, status (`Planejado`, `Confirmado`, `Realizado`, `Cancelado`), recorrência opcional (Etapa 4).
- **Modo de local (`locationMode`):** `Presencial | Online | A definir` (default `TBD`). **Local nunca é obrigatório**:
  - `Presencial`: nome do local, endereço, cidade, estado + atalhos para Maps/Rota.
  - `Online`: campo de link da transmissão/reunião + botão abrir link.
  - `A definir`: sem campos extras obrigatórios.
- **Banner do evento (`bannerPath`):** seletor nativo `PickVisualMedia` (sem permissão de armazenamento), redimensionamento leve no lado maior <= `1080px`, salvo como JPEG (~80%) em `filesDir/event_banners/<eventId>.jpg`.
- **Ingresso e Preço Real:**
  - Status do ingresso: `Gratuito`, `Não comprado`, `Pendente`, `Parcialmente pago`, `Comprado`, `Pago`, `Cancelado`.
  - Preço unitário × quantidade = preço real do ingresso. **Regra essencial: o app NÃO inventa taxas adicionais e NÃO obriga cadastrar custos inexistentes.** O campo legado `ticketFeesCents` permanece compatível no schema com valor `0` por padrão e oculto/opcional sob demanda explícita (apenas se o usuário quiser registrar taxa real cobrada pela bilheteria).
- **Vínculos integrados no detalhe do Evento:** lista compromissos (`appointments.eventId`), tarefas (`tasks.eventId`) e lançamentos (`transactions.eventId`) com atalhos para criar cada um já vinculado.

#### 5.3 Informações Apresentadas
Diferenciação explícita entre:
1. **Preço do ingresso:** valor real configurado (preço unitário × quantidade).
2. **Pagamentos já realizados:** soma das transações `SETTLED` ligadas ao evento (ingresso pago + custos extras opcionais já pagos).
3. **Obrigações futuras:** soma das transações não liquidadas (`PENDING`/`PLANNED`/`OVERDUE`) de ingresso ou parcelas futuras.
4. **Custo total estimado (evento):** soma real das despesas não canceladas vinculadas (ingresso + extras opcionalmente lançados pelo usuário).

#### 5.4 Telas e Componentes
`EventsScreen` (filtros `Próximos`, `Ingresso pendente`, `Todos`), detalhe do evento com banner e listas de vínculos, `EventEditorSheet`.

#### 5.5 Regras de Negócio
- **Gratuito (`FREE`):** não gera obrigação financeira e remove obrigações de ingresso não liquidadas anteriores.
- **Ingresso pago / a pagar:** gera obrigação única ou `N` parcelas vinculadas ao evento (`isTicketObligation = true`, `eventCostType = EventCostType.TICKET`) associadas à fatura do cartão se `PaymentMethod.CREDIT_CARD`. Status salvo no banco para obrigações em aberto deve ser `PENDING` (nunca `OVERDUE` literal gravado), deixando `FinanceRules.effectiveTransactionStatus` calcular atraso dinamicamente.
- **Cancelar evento:** cancela obrigações futuras não pagas vinculadas ao ingresso.
- **Custos extras (`EventCostType`):** apenas opcionais quando lançados pelo usuário (`Transporte`, `Alimentação`, `Hospedagem`, `Estacionamento`, `Compras`, `Outros`).
- **Exclusão:** nunca apaga compromissos/tarefas/transações filhas; apenas desvincula (`SET_NULL`) após confirmação, e apaga o arquivo de banner interno em `filesDir/event_banners/`.

#### 5.6 Estados Possíveis
Ciclo do evento: `PLANNED`, `CONFIRMED`, `ATTENDED`, `CANCELLED`.
Ciclo do ingresso: `FREE`, `NOT_BOUGHT`, `PENDING`, `PARTIALLY_PAID`, `BOUGHT`, `PAID`, `CANCELLED`.

#### 5.7 Ações Disponíveis
Criar, editar, adicionar/trocar/remover banner, alternar modo de local, abrir mapa/rota/link oficial/link de ingresso, criar compromisso/tarefa/gasto já vinculado ao evento, excluir.

#### 5.8 Integrações com Outros Módulos
Agenda (ocupa todos os dias de `startAt` a `endAt` e exibe compromissos vinculados), Tarefas (`tasks.eventId`), Compromissos (`appointments.eventId`), Financeiro (`transactions.eventId`, `installment_purchases.eventId`, faturas), Backup `.zip` (banners).

#### 5.9 Informações Armazenadas
Tabela `events` (acrescendo `locationMode` e `bannerPath?` na Etapa 5) + arquivo JPEG interno opcional + registros relacionados em `tasks`, `appointments`, `transactions`, `installment_purchases`.

#### 5.10 Comportamentos Excepcionais
- Evento sem local, sem banner e sem custos extras salva em 1 passo sem fricção.
- Arquivo de banner ausente após restauração JSON simples (sem ZIP): UI exibe placeholder limpo sem erro nem crash e permite selecionar nova imagem.

---

### Módulo 6 — Financeiro

#### 6.1 Objetivo
Oferecer controle financeiro claro, visual e livre de ambiguidades — separando **realizado**, **previsto**, **atrasado**, **saldo declarado**, **compras no cartão** e **pagamento de fatura** — com gráficos interativos em `Canvas` que respondem imediatamente aos filtros.

#### 6.2 Funcionalidades
- **Seletor global de mês `< mês/ano >`:** todo o painel Visão Geral (totais, rosca, barras 6 meses, orçamentos, previsão) obedece ao mês escolhido (padrão: mês atual).
- **Lançamentos (`transactions`):** receitas e despesas, status (`Previsto`, `Pendente`, `Realizado`, `Cancelado`, além de `Atrasado` calculado em leitura), forma de pagamento (`Pix`, `Débito`, `Cartão de crédito`, `Dinheiro`, `Transferência`, `Boleto`, `Outro`), vínculo opcional com categoria, cartão, projeto, evento e tarefa.
- **Gráfico de Rosca (`DonutChart`):** despesas do mês selecionado por categoria (até 6 fatias + `"Outros"`), alternância `"Pagas"` (padrão) | `"Pagas + previstas"`, centro com total `R$` + `"Despesas"`, legenda ordenada decrescente, toque na fatia/legenda filtra a lista de lançamentos logo abaixo com chip removível `"Categoria: X ✕"`.
- **Gráfico de Barras (`BarGroupChart`):** últimos 6 meses terminando no mês selecionado, Receitas (`accentNeon`) vs Despesas (`chartSecondary`), alternância `"Realizado"` (padrão) | `"Previsto"`, toque seleciona mês e exibe balão com valores; texto `"Resultado do mês"` (`receitas - despesas realizadas`).
- **Previsão dos Próximos 30 Dias:** cartão destacando despesas não liquidadas vencendo de hoje até `+30 dias`, receitas previstas no mesmo período e lista dos 5 maiores vencimentos — sempre rotulado como **previsto**.
- **Cartões de Crédito & Faturas:**
  - Limite total, barra de uso, `"Disponível (calculado)"` (`total - faturas em aberto`) e `"Disponível (informado)"` (somente quando positivo e diferente do calculado).
  - Fatura atual (fechamento, vencimento, valor, status) + histórico de faturas + fechar/pagar fatura.
- **Parcelamentos (`installment_purchases`):** geração automática de `N` parcelas (resto da divisão inteira na 1ª parcela), vínculo opcional com cartão/categoria/evento/projeto, quitar restantes.
- **Recorrências Financeiras (`recurring_rules`):** motor idempotente próprio de receitas/despesas recorrentes (mantido independente da Etapa 4).
- **Orçamentos (`budgets`) com `ProgressBarRow`:** limite mensal por categoria ordenado por % decrescente (`<=80%` neutro, `80%..100%` âmbar, `>100%` vermelho + `"excedeu R$ X"`).
- **Metas Financeiras (`financial_goals`) & Empréstimos (`loans`):** progresso informado pelo usuário, registro de pagamento de parcelas de empréstimo e histórico de auditoria (`audit_history`).
- **Exportação CSV** (15 colunas UTF-8 via SAF).

#### 6.3 Informações Apresentadas
Saldo declarado manual, Receita recebida/prevista do mês, Despesa paga/pendente do mês, Despesas atrasadas (todas as datas), Próximo vencimento (com liquidação funcional), Rosca por categoria, Barras 6 meses + Resultado do mês, Próximos 30 dias, Cartões/Faturas, Orçamentos, Metas, Empréstimos, Auditoria.

#### 6.4 Telas e Componentes
`FinanceScreen` (5 abas: `Resumo`, `Lançamentos`, `Cartões`, `Parcelas/Recorrentes`, `Orçamentos/Metas`), `FinanceEditors`, `ui/components/charts/` (`DonutChart`, `BarGroupChart`, `ProgressBarRow`).

#### 6.5 Regras de Negócio Inegociáveis
1. **Referência mensal:** `dueAt ?: occurredAt` (e `settledAt` opcionalmente documentado para fluxo de caixa realizado).
2. **Sem mistura de previsto e realizado** em um único total sem rótulo explícito.
3. **Regra do Cartão de Crédito (antiduplicação):** compra no cartão entra nos gastos por categoria no mês da compra/referência; **`payCreditCardStatement` liquida a fatura e suas transações sem criar uma segunda despesa** e **pagamento de fatura nunca entra no `DonutChart` nem nos gastos por categoria**.
4. **Saldo declarado vs Resultado do mês:** `declaredBalanceCents` é informativo manual; `"Resultado do mês"` é exclusivamente `receitas realizadas - despesas realizadas` do mês.
5. **Zero taxas fictícias:** nenhum juro, multa ou tarifa é gerado automaticamente sem entrada explícita do usuário.

#### 6.6 Estados Possíveis
`PLANNED`, `PENDING`, `SETTLED`, `CANCELLED` persistidos + `OVERDUE` calculado em leitura (`dueAt < startOfToday`). Faturas: `OPEN`, `CLOSED`, `PENDING`, `PAID`, `OVERDUE`.

#### 6.7 Ações Disponíveis
Criar/editar/liquidar/reabrir/excluir lançamentos, filtrar por tipo/status/categoria/busca/mês, cadastrar/editar cartão, fechar/pagar fatura, criar/quitar parcelamento, ativar/pausar/excluir recorrente, pagar parcela de empréstimo, definir orçamento/meta, editar saldo declarado, exportar CSV.

#### 6.8 Integrações com Outros Módulos
Dashboard (resumo e anel), Agenda (`statementId == null` + faturas não pagas), Tarefas (`taskId`), Projetos (`projectId`), Eventos (`eventId`), Notificações/Rotina (vencimentos e atrasos filtrados por `EXPENSE` e sem duplicar fatura), Widgets (`FinanceGlanceWidget`, `NexusSummaryGlanceWidget`).

#### 6.9 Informações Armazenadas
`transactions`, `credit_cards`, `credit_card_statements`, `installment_purchases`, `recurring_rules`, `loans`, `budgets`, `financial_goals`, `audit_history`, `settings.declaredBalanceCents`.

#### 6.10 Comportamentos Excepcionais
- Mês sem transações: gráficos exibem `"Sem lançamentos neste mês"` com anel/barras neutros, sem quebrar layout.
- Exclusão de cartão/categoria/tarefa/projeto/evento pai: `transactions` preserva o histórico financeiro desvinculando a FK (`SET_NULL`).

---

### Módulo 7 — Mercado Financeiro

#### 7.1 Objetivo
Fornecer acompanhamento rápido e limpo dos principais indicadores financeiros acompanhados pelo usuário (Dólar, Ibovespa e ativos de câmbio/cripto de referência) diretamente no centro de controle, sem transformar o app em corretora ou dependente de internet.

#### 7.2 Funcionalidades
- Exibição de cotações: **Dólar (`USD-BRL`)**, **Ibovespa (`^BVSP`, opcional com token pessoal brapi)**, e suporte opcional configurável a **Euro (`EUR-BRL`)** e **Bitcoin (`BTC-BRL`)**.
- Variação percentual diária (positiva em `positive`, negativa em `danger`) e `Sparkline` de 30 dias.
- Atualização sob demanda (ao abrir o Dashboard ou puxar para atualizar), limitada por throttle de **no máximo 1 atualização automática a cada 30 minutos**.
- Cache local completo em Room (`market_quotes` e `market_history`).
- Ativação/desativação como bloco configurável do Dashboard (`DashboardBlockType.MARKET`) + campo opcional de token pessoal brapi em Configurações (**nunca salvo no código, repositório ou backup JSON**).

#### 7.3 Informações Apresentadas
Nome do ativo, preço formatado (`R$ 5,72`, `129.450 pts`), variação do dia (`+0,84%` / `-1,12%`), gráfico `Sparkline` de 30 dias, timestamp `"atualizado em dd/MM HH:mm"` e rodapé obrigatório: *"Informativo. Não é recomendação de investimento."*

#### 7.4 Telas e Componentes
Bloco `MarketDashboardCard` em `HomeScreen`, seção opcional em `SettingsScreen` (token brapi em `EncryptedSharedPreferences` ou `SharedPreferences` privado excluído do backup + toggle de moedas), componente `Sparkline`.

#### 7.5 Regras de Negócio
- **Moedas (sem chave):** `https://economia.awesomeapi.com.br/last/USD-BRL,EUR-BRL,BTC-BRL` e histórico `https://economia.awesomeapi.com.br/json/daily/USD-BRL/30` (extensível para EUR/BTC).
- **Ibovespa (`^BVSP`):** consultado na brapi somente se o usuário informou token pessoal válido; sem token, exibe estado informativo discreto (`"Configure seu token brapi para acompanhar o Ibovespa"`), nunca erro.
- **Execução de rede:** `java.net.HttpURLConnection` puro fora da main thread (`Dispatchers.IO`), timeout de **8 segundos**, zero bibliotecas HTTP extras, zero atualização em segundo plano via WorkManager.
- **Segurança do token:** não entra no código, não vai para `NexusBackupPayload` e não aparece em `error_logs`.

#### 7.6 Estados Possíveis
1. **Cache vazio e offline / nunca consultado:** estado neutro com botão `"Atualizar agora"`.
2. **Atualizando:** indicador sutil no cartão preservando valores em cache.
3. **Atualizado:** exibe cotações + Sparkline + horário.
4. **Falha de API / sem internet:** mantém último valor em cache com timestamp `"atualizado em dd/MM HH:mm"` e **nunca exibe erro técnico ao usuário**.

#### 7.7 Ações Disponíveis
Atualizar (pull-to-refresh / botão manual respeitando intervalo razoável), configurar token Ibovespa em Configurações, ocultar/mostrar bloco no Dashboard.

#### 7.8 Integrações com Outros Módulos
Dashboard (`HomeScreen`), Personalização (`DashboardBlockType`), Configurações, Diagnóstico (contagem de cache), Backup (decisão pendente se `market_quotes`/`market_history` entram no JSON — por natureza de cache efêmero, recomenda-se não exigir restauração ou torná-las opcionais/ignoráveis, e **jamais** incluir token brapi).

#### 7.9 Informações Armazenadas
- `market_quotes(symbol PK, displayName, bid, pctChange, fetchedAt)`
- `market_history(id / composite PK(symbol, dateIso), symbol, dateIso, close)`
- Token brapi em armazenamento local privado fora do banco/backup.

#### 7.10 Comportamentos Excepcionais
Resposta JSON inesperada, rate limit da API gratuita ou timeout de 8s: falha é capturada com segurança, cache anterior permanece intacto e `error_logs` registra mensagem sanitizada (sem token).

---

### Módulo 8 — Objetivos Pessoais (`PlanningGoalsScreen`)

#### 8.1 Objetivo
Conectar metas de médio/longo prazo à execução diária (marcos, tarefas e projetos reais) com acompanhamento automático ou manual de evolução.

#### 8.2 Funcionalidades
- CRUD de objetivos com categoria, data-alvo, status, notas e modo de cálculo (`AUTO` vs `MANUAL`).
- Marcos ordenáveis (`goal_milestones`), histórico de evoluções (`goal_progress_logs`) e vínculo direto com tarefas (`tasks.goalId`) e projetos (`projects.goalId`).

#### 8.3 Informações Apresentadas
Filtros (`Ativos`, `Atrasados`, `Pausados`, `Concluídos`, `Todos`), barra de progresso, contagem de marcos/tarefas/projetos concluídos e linha do tempo de logs.

#### 8.4 Telas e Componentes
`PlanningGoalsScreen`, `PersonalGoalEditorSheet`, bloco `GOALS` no Dashboard.

#### 8.5 Regras de Negócio
- Modo `AUTO`: calcula percentual combinando marcos concluídos, tarefas vinculadas e projetos vinculados.
- Modo `MANUAL`: respeita `manualProgressPercent` informado e registra pontos em `goal_progress_logs`.
- Objetivo `ACTIVE` com `targetDateAt < startOfToday` recebe indicador `isOverdue`.

#### 8.6 Estados Possíveis
`ACTIVE`, `PAUSED`, `COMPLETED`, `CANCELLED` + atrasado derivado em leitura.

#### 8.7 Ações Disponíveis
Criar/editar objetivo, adicionar/concluir/excluir marcos, registrar progresso manual, vincular tarefa/projeto existente, abrir tarefa/projeto vinculado, excluir objetivo (`SET_NULL` em tarefas/projetos).

#### 8.8 Integrações com Outros Módulos
Tarefas, Projetos, Dashboard, Busca Global, Backup/Diagnóstico.

#### 8.9 Informações Armazenadas
`personal_goals`, `goal_milestones`, `goal_progress_logs`.

#### 8.10 Comportamentos Excepcionais
Objetivo `AUTO` sem nenhum marco, tarefa ou projeto vinculado apresenta `0%` com convite claro para vincular itens ou alternar para modo manual.

---

### Módulo 9 — Assistente de Rotina

#### 9.1 Objetivo
Estruturar abertura do dia (**Briefing Diário**) e fechamento do dia (**Revisão Noturna**) com ação rápida sobre pendências, reduzindo esquecimentos sem gerar ansiedade.

#### 9.2 Funcionalidades
- **Briefing Diário:** resume compromissos de hoje, tarefas de hoje, tarefas atrasadas, eventos de hoje, **despesas** a vencer hoje, despesas atrasadas e faturas pendentes/atrasadas.
- **Revisão Noturna:** mostra tarefas concluídas hoje, pendências de hoje/atrasadas com **adiamento em 1 toque para amanhã (09h ou 12h)**, prévia de amanhã (compromissos, tarefas, eventos, despesas) e marcação de revisão do dia.
- **Fila de Lembretes & Config:** gerencia horários dos briefings, silêncio noturno e antecedências padrão.

#### 9.3 Informações Apresentadas
Contadores reais do dia, lista acionável de itens e histórico em `briefing_history` (unicidade por tipo + `dateIso`).

#### 9.4 Telas e Componentes
`RoutineAssistantScreen` (3 abas: `Briefing Diário`, `Revisão Noturna`, `Lembretes & Config`).

#### 9.5 Regras de Negócio
- **Correção obrigatória de consistência:** contas exibidas em `billsDueToday`, `overdueBills`, `pendingBillsTodayOrOverdue` e `tomorrowBills` devem filtrar `type == TransactionType.EXPENSE`, excluir `CANCELLED`/`SETTLED` e excluir compras vinculadas a fatura (`statementId != null`) — alinhado 100% com `AgendaEngine`. Receitas previstas podem ser informadas separadamente, nunca somadas como "conta a pagar".
- Eventos e compromissos multi-dia em curso hoje devem ser contados corretamente (inclusive em `sendTestNotification`).
- Lembretes cancelados (`CANCELLED` ou legado `minutesBefore < 0`) nunca aparecem na fila.

#### 9.6 Estados Possíveis
Não revisado hoje vs Revisado hoje (`reviewedAt != null`).

#### 9.7 Ações Disponíveis
Marcar briefing/revisão como concluído, concluir tarefa, adiar tarefa para amanhã (09h/12h), liquidar transação, adiar transação, abrir item, adiar/cancelar lembrete, enviar notificação de teste, reagendar todos os lembretes.

#### 9.8 Integrações com Outros Módulos
Agenda (`AgendaEngine`), Tarefas, Financeiro, Eventos, Notificações.

#### 9.9 Informações Armazenadas
`routine_assistant_settings`, `briefing_history`.

#### 9.10 Comportamentos Excepcionais
Quando horário de silêncio está ativo, disparos programados para essa janela são postergados automaticamente para o término do silêncio (`adjustForQuietHours`).

---

### Módulo 10 — Notificações Locais

#### 10.1 Objetivo
Entregar lembretes pontuais, exatos e acionáveis respeitando o estado real da entidade no momento do disparo.

#### 10.2 Funcionalidades
- 4 canais dedicados (`nexus_schedule`, `nexus_finance`, `nexus_events`, `nexus_routine`) com toggle individual + chave geral.
- Agendamento exato via `AlarmManager` (com fallback seguro se permissão de alarme exato for revogada) + sincronização periódica a cada 15 minutos via `WorkManager` + reagendamento após boot/mudança de hora/fuso/atualização do app.
- Ações diretas na notificação: `Concluir`, `Marcar pago`, `Adiar 15m`, `Dispensar`, `Marcar revisado`.

#### 10.3 Informações Apresentadas
Título do registro, contexto/horário/valor e ações contextuais na bandeja Android.

#### 10.4 Telas e Componentes
`NexusNotificationChannels`, `NexusNotificationScheduler`, `NexusBroadcastReceivers` (`NexusReminderReceiver`, `NexusNotificationActionReceiver`, `NexusBootReceiver`), `NexusReminderSyncWorker`, configuração em `RoutineAssistantScreen`.

#### 10.5 Regras de Negócio
- **Estado atual manda:** antes de exibir a notificação ou sincronizar, validar se a entidade de origem continua pendente/ativa. Se já foi concluída, paga ou cancelada (ou se o lembrete estiver `CANCELLED`/`DISMISSED`/`minutesBefore < 0`), cancelar o alarme silenciosamente.
- **Preservação nos editores (`ReminderRules`):** editar e salvar um registro sem mexer no seletor de lembrete preserva `minutesBefore`, estado cancelado e soneca ativa.
- **Recorrências (Etapa 4):** ao salvar ou atualizar série recorrente (e no sync periódico), agendar lembretes apenas para ocorrências válidas dentro dos próximos **30 dias**; reagendar imediatamente se a regra ou exceção mudar.

#### 10.6 Estados Possíveis
`ACTIVE`, `FIRED`, `DISMISSED`, `CANCELLED` (+ campo `snoozedUntil`).

#### 10.7 Ações Disponíveis
Configurar antecedência padrão ou por item (`0`, `15`, `30`, `60`, `120` min, dia anterior ou `Sem lembrete`), testar canal, adiar 15 min, concluir/pagar pela notificação, abrir tela/item via deep-link.

#### 10.8 Integrações com Outros Módulos
Tarefas, Compromissos, Eventos, Transações, Faturas, Recorrências e Assistente de Rotina.

#### 10.9 Informações Armazenadas
`reminders`, `routine_assistant_settings`.

#### 10.10 Comportamentos Excepcionais
Falta de permissão `POST_NOTIFICATIONS` (Android 13+) ou `SCHEDULE_EXACT_ALARM`: aplicativo degrada graciosamente sem crashes, usa fallback `setAndAllowWhileIdle` e exibe aviso orientativo na tela de Rotina/Diagnóstico.

---

### Módulo 11 — Widgets Android (`Glance`)

#### 11.1 Objetivo
Expor o pulso essencial do NEXUS diretamente na tela inicial do Android sem obrigar o usuário a abrir o app a cada checagem.

#### 11.2 Funcionalidades
- **4 widgets responsivos (`SMALL`, `MEDIUM`, `LARGE`)**:
  1. `MyDayGlanceWidget` — compromissos, tarefas e alertas do dia.
  2. `MyTasksGlanceWidget` — tarefas pendentes com conclusão rápida em 2 etapas (evita toque acidental e bloqueia tarefas `BLOCKED`/terminais).
  3. `FinanceGlanceWidget` — resumo mensal (`FinanceRules`) + próximo vencimento.
  4. `NexusSummaryGlanceWidget` — visão combinada dia + tarefas + financeiro.
- Configuração isolada por `appWidgetId` (`NexusWidgetConfigActivity`) + tela guia `WidgetsHubScreen`.

#### 11.3 Informações Apresentadas
Dados compactos proporcionais ao tamanho do widget, botão refresh, botão config e deep-links diretos (`nexus://open/...`).

#### 11.4 Telas e Componentes
`widgets/*`, `NexusWidgetTheme.kt`, `NexusWidgetConfigActivity`, `WidgetsHubScreen`.

#### 11.5 Regras de Negócio
- **Mesmos cálculos do app:** widgets devem consumir `AgendaEngine` e `FinanceRules` — nunca lógica paralela divergente.
- **Suporte Claro/Escuro obrigatório:** atualizar `NexusWidgetTheme` para refletir tema claro e escuro com contraste adequado.
- Atualizações serializadas via `Mutex` em `NexusWidgetUpdater` após qualquer mutação no repositório ou restauração de backup.

#### 11.6 Estados Possíveis
Populado (Small/Medium/Large), Vazio acionável, Erro de leitura local recuperável com 1 toque (`NexusWidgetErrorState`).

#### 11.7 Ações Disponíveis
Atualizar dados, configurar widget, abrir item/aba específica, conclusão em 2 etapas de tarefa.

#### 11.8 Integrações com Outros Módulos
Agenda (`AgendaEngine`), Tarefas, Financeiro (`FinanceRules`), Configurações (`darkTheme`), Backup (dispara refresh geral pós-restauração).

#### 11.9 Informações Armazenadas
`SharedPreferences` por `appWidgetId` (`NexusWidgetConfigStore`) + leitura do Room.

#### 11.10 Comportamentos Excepcionais
Widget recém-adicionado durante leitura pesada ou falha transitória exibe layout de carregamento/erro amigável com toque para re-sincronizar.

---

### Módulo 12 — Configurações e Personalização

#### 12.1 Objetivo
Permitir adaptar o centro de controle à rotina e estética do usuário sem dispersar preferências pelo sistema.

#### 12.2 Funcionalidades
- **Configurações (`SettingsScreen`):** nome do usuário, alternância Tema Escuro (padrão) / Tema Claro, gestão completa de Categorias (nome, cor, escopo `GENERAL/APPOINTMENT/TASK/FINANCE/EVENT`) e configuração opcional do token brapi (`^BVSP`).
- **Personalização do Dashboard (`DashboardCustomizationScreen`):** ativar/desativar e reordenar blocos (`DashboardBlockType`), modo compacto, template de saudação (`AUTO` ou texto com `{nome}`), formato de data (`NexusDateFormat`) e limite de itens por bloco (`maxItems`).
- **Biblioteca de Frases (`QuoteLibraryScreen`):** CRUD de frases locais, ativar/pausar, importação e exportação via SAF em TXT (`texto - autor` ou `texto | autor`) ou JSON.

#### 12.3 Informações Apresentadas
Prévia de saudação/data, lista ordenável de blocos, catálogo de categorias com amostra de cor e estatística da biblioteca de frases (`EMPTY`, `SINGLE_QUOTE`, `ROTATING`).

#### 12.4 Telas e Componentes
`SettingsScreen`, `DashboardCustomizationScreen`, `QuoteLibraryScreen`, `MoreScreen` (hub de subtelas secundárias).

#### 12.5 Regras de Negócio
- Categorias padrão (`isDefault`) podem ser editadas/usadas com fallback seguro; excluir categoria desvincula (`SET_NULL`) registros ou impede perda em orçamentos com confirmação explícita (`budgets` tem `CASCADE`).
- Nenhuma frase fictícia vem pré-cadastrada; autoria em branco mostra `"Autoria não informada"`.

#### 12.6 Estados Possíveis
Configuração inicial default, personalizada, biblioteca de frases vazia / única / rotativa.

#### 12.7 Ações Disponíveis
Editar perfil/tema/categorias/token mercado, reordenar/ocultar blocos, restaurar padrão do dashboard, importar/exportar/editar frases.

#### 12.8 Integrações com Outros Módulos
Alimenta todo o `NexusTheme`, Dashboard, Categorias de todos os módulos e Bloco Mercado.

#### 12.9 Informações Armazenadas
`settings`, `categories`, `personalization_settings`, `quotes` (e token brapi privado fora do DB).

#### 12.10 Comportamentos Excepcionais
- `DashboardCustomizationScreen` e `RoutineAssistantScreen` devem respeitar o fluxo de origem ao voltar (se abertas pelo menu `Mais` ou pela `Home`).

---

### Módulo 13 — Backup, Restauração e Diagnóstico

#### 13.1 Objetivo
Garantir soberania total do usuário sobre seus dados locais (portabilidade entre versões do app e auditoria transparente de saúde do banco).

#### 13.2 Funcionalidades
- **Exportação JSON versionada (SAF):** todas as tabelas estruturais em formato `CURRENT_FORMAT_VERSION`, com tolerância total de leitura para versões `v1..vN`.
- **Exportação / Restauração `.zip` completo (Etapa 5):** empacota `data.json` + pasta `banners/` (imagens JPEG de eventos), restaurando dados e arquivos em conjunto; `.json` simples permanece aceito (avisando sobre ausência de banners).
- **Exportação CSV Financeiro:** planilha UTF-8 de 15 colunas.
- **Diagnóstico (`DiagnosticsScreen`):** versão do app/schema, contagem em tempo real de todas as tabelas, histórico de erros capturados por `GlobalErrorHandler` (`error_logs`) com opção de limpar logs.

#### 13.3 Informações Apresentadas
Resumo prévio de validação de backup (versão de origem, app, data de exportação, quantidade por tabela e avisos de compatibilidade) antes de confirmar substituição; após restauração, sumário de registros restaurados.

#### 13.4 Telas e Componentes
`BackupScreen`, `DiagnosticsScreen` em `MoreSettingsBackupDiagnosticsScreens.kt`, `BackupManager`, `BackupModels`, `GlobalErrorHandler`.

#### 13.5 Regras de Negócio
1. **Atomicidade total:** restauração ocorre dentro de `database.withTransaction`; falha em qualquer etapa faz rollback automático sem corromper o banco atual.
2. **Retrocompatibilidade garantida:** leitor usa `ignoreUnknownKeys = true`, `coerceInputValues = true`, `isLenient = true`, saneamento de FKs órfãs e inserção na ordem topológica correta (pais antes de filhos).
3. **Pós-restauração:** limpa notificações/alarmes antigos, re-sincroniza lembretes válidos (`ReminderRules`), gera ocorrências futuras e atualiza os 4 widgets.
4. **Privacidade:** token pessoal da brapi nunca entra no backup JSON/ZIP nem nos logs.

#### 13.6 Estados Possíveis
Ocioso, Exportando/Validando/Restaurando (`isBusy`), Prévia pronta para confirmação, Erro de validação legível, Restauração concluída com resumo.

#### 13.7 Ações Disponíveis
Exportar JSON, Exportar ZIP completo, Importar JSON/ZIP (com preview + confirmação), Exportar CSV financeiro, Inspecionar/limpar `error_logs`.

#### 13.8 Integrações com Outros Módulos
Lê e restaura **todas** as tabelas persistentes do sistema + banners de Eventos; dispara ressincronização em Notificações, Recorrências e Widgets.

#### 13.9 Informações Armazenadas
`error_logs`, além de ser consumidor/produtor dos arquivos SAF `.json`, `.zip` e `.csv`.

#### 13.10 Comportamentos Excepcionais
- Arquivo inválido, JSON truncado ou ZIP sem `data.json`: `BackupValidationResult.Invalid` mostra mensagem clara sem tocar nos dados existentes no Room.
- Backup com versão futura (`formatVersion > CURRENT_FORMAT_VERSION`): lê campos conhecidos com aviso explícito (`compatibilityNotice`) em vez de rejeitar cegamente quando estruturalmente compatível.

---

## 4. Dúvidas e Decisões que Dependem de Confirmação

As decisões abaixo emergiram da consolidação entre `docs/NEXUS_SPEC.md`, o codebase atual e a visão expandida desta especificação e devem ser confirmadas antes (ou durante) a implementação dos respectivos lotes:

1. **Escopo do Redesenho do Dashboard (`Etapa 3/6` vs lote dedicado):**
   - `docs/NEXUS_SPEC.md` definia a Etapa 6 focando apenas em Cabeçalho N + Mercado + QuickAddChooser, mantendo o bloco de tarefas com tiles numéricos. A nova visão consolidada pede **gráfico de anel central de tarefas no Dashboard + alternância mês anterior/atual/próximo + resumo financeiro com anel + indicadores de projetos e objetivos**.
   - **Recomendação documentada no Roadmap:** incluir os componentes visuais (`TaskRingChart`, `DonutChart`) já na base de gráficos e finalizar o redesenho completo da `HomeScreen` junto com o lote de Identidade & Dashboard (sem atrasar a Etapa 3 do Financeiro). Confirmar se prefere em lote próprio pós-Etapa 5 ou acoplado à Etapa 6.
2. **Mês selecionado no bloco de Tarefas da Home para tarefas recorrentes e sem data:**
   - Tarefas **sem data (`dueAt == null`)** devem ficar fora do anel mensal (aparecendo em um sub-badge discreto `"X sem data"`) ou somar no anel apenas quando `Mês = Atual`? (Recomendação: anel mensal conta exclusivamente tarefas com `dueAt` no mês escolhido + recorrentes projetadas naquele mês, exibindo `"Sem prazo: N"` como atalho ao lado).
3. **Campo `ticketFeesCents` em Eventos:**
   - Manter a coluna no banco por compatibilidade de migração/backup (`0` por padrão) e ocultar completamente da UI padrão de evento, ou deixá-la acessível dentro de `"Mais detalhes"` com rótulo `"Taxa de serviço paga (opcional)"`? (Recomendação: ocultar do formulário principal e deixar opcional zerado em `"Mais detalhes"`, jamais pré-preenchido ou obrigatório).
4. **Cotações de Mercado além de Dólar e Ibovespa:**
   - `NEXUS_SPEC.md` cita `USD-BRL, EUR-BRL, BTC-BRL` e `^BVSP`; a seção 4 deste briefing destaca Ibovespa e Dólar no Dashboard. Confirmar se `EUR-BRL` e `BTC-BRL` devem aparecer por padrão no bloco Mercado ou como toggles opcionais para manter a Home mais enxuta.
5. **Política de cache `market_quotes` / `market_history` no Backup JSON:**
   - Manter fora do backup JSON/ZIP (já que são cotações públicas reobtidas online) ou incluir como listas opcionais? (Recomendação: excluir ou tornar estritamente opcional, mantendo **sempre** o token brapi fora do backup).
6. **Subtítulos / Edição de ocorrencia adiada em Tarefas Recorrentes:**
   - Quando o usuário adia uma tarefa recorrente atrasada de ontem para amanhã, ela deve contar como `"Pendente para amanhã"` mantendo o carimbo original (`occurrenceDateIso` de ontem) para auditoria ou mostrar selo `"Adiada de dd/MM"`? (Recomendação: manter `occurrenceDateIso` original da série e exibir selo `"Adiada"` quando `toLocalDate(dueAt) > LocalDate.parse(occurrenceDateIso)`).
