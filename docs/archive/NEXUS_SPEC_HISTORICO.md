> **[DOCUMENTO HISTÓRICO ARQUIVADO — ESPECIFICAÇÃO BASE `v4` → ETAPAS 1–6]**  
> Especificação histórica usada na evolução inicial sobre a baseline `v4`. O módulo Mercado (Etapa 6 original) e outras partes evoluíram posteriormente nos Lotes 01–09. Consulte os documentos vigentes: [`../02_ESPECIFICACAO_PRODUTO.md`](../02_ESPECIFICACAO_PRODUTO.md), [`../10_ROADMAP.md`](../10_ROADMAP.md) e [`../11_DECISOES_ADRS.md`](../11_DECISOES_ADRS.md).

# NEXUS_SPEC — Especificação de evolução do NEXUS

> **Para que serve:** fonte da verdade para qualquer IA que programe o NEXUS (Google AI Studio e outras). Salve em `docs/NEXUS_SPEC.md` no repositório. Cada pedido de trabalho dirá apenas: "Leia `docs/NEXUS_SPEC.md` e implemente a ETAPA X". Se um pedido solto contradiz esta spec, avise a contradição antes de implementar.
>
> **Base:** o app já existe (Kotlin, Compose, Material 3, Room v4, 25 tabelas, 4 widgets Glance, versionName 5.0.0). Esta spec **evolui** o app. Não é uma reescrita. Nomes de classes citados vêm do documento de arquitetura atual; confirme-os no código antes de usar.

---

## 0. Regras de trabalho (valem para TODAS as etapas)

1. **Implemente somente a etapa pedida.** Não antecipe etapas futuras e não remova funcionalidades existentes.
2. **Banco de dados:** nunca destrutivo. Proibido `fallbackToDestructiveMigration`. Toda mudança de schema é uma `Migration` explícita e **aditiva** (novas colunas anuláveis ou com default, novas tabelas). Incremente `DATABASE_VERSION`. Mantenha `exportSchema = true`.
3. **Backup:** toda mudança de schema atualiza `BackupModels` e `BackupManager` (incrementar `CURRENT_FORMAT_VERSION`) mantendo a leitura de TODAS as versões anteriores (`ignoreUnknownKeys`, valores default para campos ausentes).
4. **Você não compila Android neste ambiente.** Nunca diga que compilou nem que testes passaram. Ao final, releia cada `.kt` alterado (imports, assinaturas, tipos, nomes de DAOs, chamadas entre Repository, ViewModel e telas) e liste os arquivos alterados e os riscos de compilação.
5. **Organização do código:**
   - Lógica nova vai em **arquivos novos por domínio**. `NexusRepository` e `NexusViewModel` apenas delegam e expõem; não os faça crescer além do necessário.
   - Regras de cálculo (finanças, datas, agenda, recorrência, atraso) são **funções puras** em `domain/rules/` (ou `core/`), com testes JUnit.
   - Nenhum refactor em massa sem pedido explícito.
6. **Visual:** cores, tipografia e formas somente via `NexusTheme` (tokens). Nenhum hex solto em telas. Tema escuro e claro sempre juntos.
7. **Idioma e formatos:** pt-BR, moeda R$ (BRL), datas `dd/MM/yyyy`, horário 24 h, fuso `America/Sao_Paulo`.
8. **Sem bibliotecas novas**, exceto as citadas explicitamente na etapa. Gráficos em Compose Canvas.
9. **Sem permissão INTERNET** até a Etapa 6.
10. **Sem dados fictícios** em nenhuma tela. Todo número vem do banco.
11. **Estados vazios e erros** com a identidade visual do app, nunca tela em branco nem travamento.
12. **Versão:** incremente `versionCode` e `versionName` a cada etapa.
13. **Relatório final** (português simples): o que mudou por arquivo; testes escritos; o que NÃO pôde verificar sem compilar; se houve migração e o risco para dados existentes; dúvidas.

---

## 1. Estado atual confirmado (resumo)

- Navegação **por estado** em `NexusAppRoot` (`MainTab`, `SubRoute`, `ActiveEditor`). `navigation-compose` está declarado e não é usado. **Decisão: manter navegação por estado e usar `BackHandler`. Não migrar para NavHost.**
- Tarefas têm **um único campo de data** (`dueAt`, com `hasDueTime`). **Decisão: manter um único campo.** Na interface o rótulo passa a ser **"Quando"**. Um "prazo final" separado fica fora do escopo por ora.
- Kanban usa a mesma tabela `tasks`. Subtarefas existem (`subtasks`, com `isCompleted` e `sortOrder`).
- Agenda lê 6 fontes (compromissos, tarefas, eventos, transações, faturas, projetos).
- Financeiro: compra no cartão é a despesa; `payCreditCardStatement` quita sem criar segunda despesa (**manter**).
- Backup JSON v4 tolerante. Widgets Glance (4). Lembretes via `AlarmManager` + `WorkManager`.
- **Não existe:** recorrência para tarefas, compromissos e eventos (só o financeiro tem `recurring_rules`); evento multi-dia na Agenda; banner de evento; vínculo transação→tarefa; gráfico de rosca; cotações; símbolo N, ícone e splash próprios.

---

## 2. Decisões de arquitetura (fixas)

### 2.1 Datas e fuso (um só lugar)
Todas as conversões ficam em `DateTimeUtils`. Armazenamento continua `Long` (epoch ms UTC) + `dateIso` (`YYYY-MM-DD`) onde já existe. Funções obrigatórias:
- `toLocalDate(epochMs): LocalDate` (em `America/Sao_Paulo`)
- `startOfDayEpoch(LocalDate): Long`
- `epochToDatePickerMillis(epochMs): Long` (meia-noite UTC do mesmo `LocalDate` local)
- `datePickerMillisToEpoch(pickerMillis, keepTimeFrom: Long?): Long` (lê o `LocalDate` em UTC e devolve o instante local, preservando a hora se houver)

`NexusDatePickerDialog` usa SOMENTE essas funções.

### 2.2 Agenda como motor de expansão
Criar `AgendaEngine` (arquivo novo) com função pura:
`expand(window: DateRange, sources: AgendaSources, filters: AgendaFilters): List<AgendaOccurrence>`

`AgendaOccurrence`: `sourceType`, `sourceId`, `occurrenceDateIso`, `startAt`, `endAt?`, `isAllDay`, `spanStartDateIso`, `spanEndDateIso`, `title`, `subtitle`, `status`, `kind` (TASK, APPOINTMENT, EVENT, FINANCE, PROJECT), `categoryId?`, `colorHex?`.
Chave estável de UI: `"$sourceType:$sourceId:$occurrenceDateIso"`.
Hoje gera uma ocorrência por registro. A recorrência (Etapa 4) será encaixada AQUI, sem mudar as telas. Todas as visões (Dia, Semana, Mês, Lista, Início, widgets) usam este motor. Nenhum cálculo de agenda fora dele.

### 2.3 Recorrência genérica (implementada na Etapa 4)
Novas tabelas:
- `recurrences(id, ownerType, ownerId, frequency, intervalCount, byWeekdays, startDateIso, untilDateIso?, maxCount?, createdAt, updatedAt)`
- `recurrence_exceptions(id, recurrenceId, occurrenceDateIso, kind [CANCELLED|OVERRIDDEN], overrideStartAt?, overrideEndAt?, overrideTitle?)` com `UNIQUE(recurrenceId, occurrenceDateIso)`

Regras:
- **Compromissos e eventos:** guardam UMA regra; as ocorrências são calculadas na leitura dentro da janela pedida. Nunca gravar cópias.
- **Tarefas:** `tasks.recurrenceId?` e `tasks.occurrenceDateIso?`. Ao concluir uma tarefa recorrente, criar a PRÓXIMA ocorrência (nova linha) de forma idempotente (`UNIQUE(recurrenceId, occurrenceDateIso)` em `tasks`). A tarefa concluída permanece no histórico.
- Editar "só esta" cria exceção; editar "toda a série" altera a regra.
- Frequências: diária, dias da semana escolhidos, fins de semana, semanal, quinzenal, mensal, anual, intervalo personalizado simples (a cada N dias/semanas). Fim opcional (data ou quantidade).
- O financeiro mantém `recurring_rules` como está. **Não mexer.**

### 2.4 Vínculos adicionais (aditivos)
- `transactions.taskId?` (FK `tasks`, `SET_NULL`, índice): origem de um gasto.
- `appointments.eventId?` (FK `events`, `SET_NULL`, índice): compromisso ligado a um evento.
- Excluir o registro pai NUNCA apaga os filhos; apenas desvincula (com confirmação quando houver filhos).

### 2.5 Eventos
- `events.locationMode` (`IN_PERSON`, `ONLINE`, `TBD`; default `TBD`). Local nunca é obrigatório.
- `events.bannerPath?` (caminho de arquivo interno).

### 2.6 Contas bancárias
Fora do escopo. `settings.declaredBalanceCents` permanece. Se um dia houver contas, será `accounts` + `transactions.accountId?` (aditivo); o modelo atual permite isso sem quebra.

### 2.7 Tokens visuais (adicionar em `NexusTheme`)
- `accentNeon`: dark `#7CFFB2`, light `#0E9F63` (cor de destaque única do app; fácil de trocar em um só lugar).
- Paleta de categorias para gráficos (8 cores), usada na ordem quando a categoria não tem cor própria:
  - Dark: `#7CFFB2, #5CC8FF, #C792FF, #FFD166, #FF7A90, #FF9F6B, #4DE1C1, #A3B1C6`
  - Light: `#0E9F63, #0B7FBF, #7A4FD6, #B98100, #D6334F, #D2691E, #0C9C86, #5A6B80`
- Cores semânticas existentes (verde, vermelho, âmbar, azul) continuam só para status.
- Neon é acento pontual (seleção, destaque, categorias nos gráficos). A base continua preta, branca e cinza. Verificar contraste de texto (mínimo confortável) nos dois temas.

### 2.8 Componentes de gráfico reutilizáveis
Pasta `ui/components/charts/` com: `DonutChart`, `BarGroupChart`, `ProgressBarRow`, `Sparkline`. Todos recebem dados e cores por parâmetro, leem cores do tema, suportam estado vazio e têm descrição de acessibilidade (`contentDescription`/`semantics`).

---

## 3. Regras de produto: o que aparece onde

Princípio: **registrar uma vez, ver em vários lugares.** Nada é copiado.

| Registro | Início | Agenda | Tarefas | Eventos | Financeiro |
|---|---|---|---|---|---|
| Tarefa | Hoje / atrasadas | Se tiver data | Registro completo | Se vinculada | Se tiver gasto vinculado |
| Compromisso | Hoje / próximo | Registro completo | não | Se vinculado | — |
| Evento | Próximos | Faixa (todos os dias do período) | Tarefas vinculadas | Registro completo | Ingresso e custos |
| Transação não liquidada (conta a pagar/receber) | Resumo | No vencimento | — | Se vinculada | Registro completo |
| Transação liquidada | Totais | **Oculta por padrão** | — | Se vinculada | Registro completo |
| Compra de cartão (com fatura) | Totais | **Não aparece individualmente**; aparece a fatura | — | Se vinculada | Registro completo |
| Fatura não paga | Resumo | No vencimento | — | — | Registro completo |
| Projeto com prazo | Resumo | No prazo | Projeto | — | Custos |

Diferenciação visual na Agenda por **ícone e forma** (não por cor): tarefa = caixinha; compromisso = barra de duração; evento = selo; financeiro = símbolo R$; projeto = bandeira. A cor serve apenas ao status (atrasado, pago, atenção).

---

## 4. ETAPA 1 — Correções críticas

**Objetivo:** consertar o que está quebrado antes de crescer.

1. **Voltar:** `BackHandler` sobre `NexusAppRoot`, prioridade: (1) editor aberto → fecha (confirma se houver alterações não salvas); (2) subtela → volta à anterior lógica (subtela profunda → menu Mais → aba atual); (3) aba ≠ Início → Início; (4) Início sem nada aberto → deixa o sistema sair. Extrair `resolveBackAction(estado)` como função pura + testes. Manifest: `android:enableOnBackInvokedCallback="true"`.
2. **Seletor de data:** usar as funções da seção 2.1 em TODOS os pontos que usam `NexusDatePickerDialog` (tarefas, compromissos, eventos, transações, recorrências, parcelas, empréstimos, orçamentos, metas, objetivos, marcos). Listar os pontos corrigidos. Não alterar datas já salvas. Testes: escolher 20/11 → `dateIso 2026-11-20`; reabrir mostra 20/11; ida e volta sem deslocar o dia; 23:30 e 00:10; sem hora.
3. **Totais do Financeiro por mês:** em `observeFinanceSummary`, `receivedIncomeCents`, `expectedIncomeCents`, `paidExpenseCents`, `pendingExpenseCents` filtram o **mês corrente** (referência `dueAt ?: occurredAt`). `overdueExpenseCents` considera TODAS as datas. Rótulos na UI dizem "deste mês". Home e widget Financeiro usam a mesma função. Extrair para função pura + testes.
4. **Limite do cartão:** mostrar "Disponível (calculado)" e, se existir e for diferente, "Disponível (informado)". Não escolher um sozinho.
5. **Lembretes nos editores:** ao reabrir Tarefa, Compromisso, Transação e Evento, carregar `minutesBefore` real da tabela `reminders`. Salvar sem mexer no lembrete não pode alterá-lo nem apagá-lo.

**Aceite:** Voltar nunca fecha o app com um modal aberto; tarefa criada para o dia 20 aparece no dia 20; totais do painel não incluem outros meses; reabrir e salvar um registro preserva seu lembrete.

---

## 5. ETAPA 2 — Agenda limpa e integração tarefa→gasto

### 5.1 Agenda (usa `AgendaEngine`)
- **Visões:** Dia, Semana (sete dias seg–dom, setas para semana anterior e seguinte, cabeçalho com o intervalo), Mês (grade com pontos nos dias ocupados; ao tocar um dia, lista abaixo dele), Lista (cronológica).
- **Filtros por tipo** em chips no topo: Tudo (padrão), Tarefas, Compromissos, Eventos, Financeiro, Projetos. Mantém a busca e o filtro de categoria. Chips funcionam em todas as visões.
- **Financeiro na Agenda:** apenas transações não liquidadas e não canceladas **sem fatura** (`statementId == null`) e faturas não pagas com total > 0. Compras de cartão ligadas a uma fatura NÃO aparecem; a fatura aparece no vencimento. Um interruptor "Mostrar já pagos" (padrão desligado) exibe as liquidadas.
- **Evento de vários dias:** ocupa todos os dias de `startAt` a `endAt`. Em Dia e Semana aparece como faixa "dia inteiro" no topo do dia; no Mês marca todos os dias do período.
- Tocar em qualquer item abre o registro original. Conflitos entre compromissos continuam sinalizados.
- **Estado vazio do dia:** mensagem curta e neutra ("Nada marcado para este dia"), sem cobrança.

### 5.2 Tarefa: checklist e "Registrar gasto"
- O rótulo do campo de data passa a ser **"Quando"**. A tarefa sem data não aparece na Agenda.
- **Subtarefas** são itens leves (texto + feito + ordem). Interface de checklist: adicionar item no fim da lista com um toque, marcar com um toque, arrastar não é necessário.
- Na tela da tarefa há o botão **"Registrar gasto"** (visível em qualquer status, destaque discreto quando a tarefa está concluída). Abre `TransactionEditorSheet` já preenchido: tipo Despesa, descrição = título da tarefa, data = hoje, `taskId` = tarefa, categoria sugerida por nome igual ao da categoria da tarefa (se existir), caso contrário vazia. Pede só **valor** e **forma de pagamento** como obrigatórios.
- A tarefa mostra, depois, "Gasto: R$ X" com atalho para a transação. Se houver vários gastos, soma e lista.
- Nunca é obrigatório registrar gasto.
- **Migração aditiva:** `transactions.taskId?` (seção 2.4) e `appointments.eventId?` (campo opcional "Evento" no editor de compromisso).

**Aceite:** tarefa "Ir ao mercado" com checklist → concluir → Registrar gasto → a despesa aparece no Financeiro vinculada; a compra paga não aparece na Agenda; evento de 3 dias aparece nos 3 dias.

---

## 6. ETAPA 3 — Financeiro visual (gráficos)

Todos os gráficos: Compose Canvas, componentes de `ui/components/charts/`, cores da paleta 2.7, funcionam nos dois temas, mostram estado vazio ("Sem lançamentos neste mês") e respeitam o mês selecionado.

### 6.1 Seletor de mês
Barra no topo do painel: `< mês/ano >`. Todos os blocos abaixo respeitam o mês escolhido. Padrão: mês atual.

### 6.2 Definições (função pura `FinanceMonthSummary` + testes)
- **Referência de data do mês:** `dueAt ?: occurredAt`.
- **Despesas pagas:** despesas `SETTLED`. **Pendentes:** não liquidadas e não vencidas. **Atrasadas:** não liquidadas com vencimento anterior a hoje (todas as datas). **Receitas recebidas / previstas:** análogo.
- **Compra no cartão** conta na categoria no mês da compra. **Pagamento de fatura NÃO entra** nas despesas por categoria nem nos gráficos.
- **Previsto e realizado nunca se misturam** num mesmo número sem rótulo.

### 6.3 Rosca de gastos por categoria (`DonutChart`)
- Dados: despesas do mês por categoria. Alternância no cabeçalho do cartão: **"Pagas"** (padrão) | **"Pagas + previstas"**.
- Até 6 fatias + "Outros" (agrupa o restante). Cor da fatia: `colorHex` da categoria se existir, senão a paleta 2.7 na ordem.
- Centro da rosca: total do mês em R$ e a palavra "Despesas".
- Legenda abaixo: bolinha de cor, nome, valor e percentual, ordenada do maior para o menor.
- **Interação:** tocar em uma fatia (ou item da legenda) destaca a fatia e **filtra a lista de lançamentos** logo abaixo por aquela categoria, com um chip "Categoria: X ✕" para limpar. Tocar de novo limpa.
- Acessibilidade: cada fatia com descrição "Alimentação, R$ 420,00, 28%".

### 6.4 Receitas × Despesas (`BarGroupChart`)
- Últimos 6 meses terminando no mês selecionado. Duas barras por mês: receitas (cor `accentNeon`) e despesas (cor secundária cinza-claro/branco suave).
- Alternância **Realizado | Previsto** (padrão Realizado). Rótulo do mês embaixo (jan, fev...).
- Tocar em um mês mostra os valores em um balão e seleciona o mês no seletor 6.1.
- Linha de saldo líquido do mês (receitas − despesas realizadas) em texto abaixo, rotulada **"Resultado do mês"** (não é saldo bancário).

### 6.5 Orçamento (`ProgressBarRow`)
Para cada categoria com orçamento no mês: nome, barra, texto "R$ gasto de R$ limite (xx%)". Cores: até 80% neutra; de 80% a 100% âmbar; acima de 100% vermelha (barra limitada a 100% visualmente, com texto "excedeu R$ X"). Ordenar por percentual decrescente.

### 6.6 Cartões
Para cada cartão: nome, instituição, "Limite total", "Usado" (barra), "Disponível (calculado)", "Disponível (informado)" se diferente; **fatura atual** (valor, fechamento, vencimento, status); lista do **histórico de faturas** (mês, valor, status). Pagar fatura continua quitando sem nova despesa.

### 6.7 Previsão de 30 dias
Cartão "Próximos 30 dias": soma das despesas **não liquidadas** com vencimento de hoje a +30 dias, e das receitas previstas; lista dos 5 maiores vencimentos. Tudo rotulado como **previsto**.

### 6.8 Metas financeiras
Barra de progresso por meta (valor atual / alvo), prazo e percentual. O valor atual é o informado pelo usuário (não inventar).

**Aceite:** tocar na fatia "Alimentação" filtra a lista; trocar o mês muda todos os blocos; pagar uma fatura não altera o total de despesas do mês; tudo legível em tema claro e escuro.

---

## 7. ETAPA 4 — Recorrência de tarefas, compromissos e eventos

Implementar conforme a seção 2.3. Interface do editor: seletor "Repetir" com **Não repete, Todos os dias, Dias da semana (escolher), Fins de semana, Toda semana, A cada 2 semanas, Todo mês, Todo ano, Personalizado (a cada N dias/semanas)** + "Termina: nunca / em data / após N vezes".
- Ao editar ou excluir uma ocorrência: perguntar **"Só esta" ou "Toda a série"**. "Só esta" cria exceção (`CANCELLED` ou `OVERRIDDEN`).
- A `AgendaEngine` passa a expandir recorrências dentro da janela pedida (sem gravar cópias).
- Lembretes: cada ocorrência futura dentro dos próximos 30 dias recebe lembrete; reagendar quando a regra mudar.
- Tarefas: concluir cria a próxima (idempotente).
- Testes: expansão semanal/quinzenal/mensal (inclui fim de mês, ex.: dia 31), exceção cancelada, exceção editada, fim por data e por contagem, ausência de duplicação ao reabrir.

**Aceite:** "Aula de inglês, quartas e sextas" aparece nas datas certas em Semana e Mês; cancelar uma aula não apaga as demais.

---

## 8. ETAPA 5 — Eventos: banner, modo de local e custo total

- **Modo de local:** `Presencial | Online | A definir`. Presencial mostra endereço e atalhos de mapa; Online mostra campo de link; A definir não pede nada. Nenhum é obrigatório.
- **Banner:** botão lápis na capa do detalhe do evento abre o **seletor de fotos do Android (`PickVisualMedia`, sem permissão)**. O app reduz a imagem para no máximo 1080 px no lado maior, salva como JPEG (qualidade ~80) em `filesDir/event_banners/<eventId>.jpg` e guarda o caminho em `events.bannerPath`. Exibir com carregamento leve (decodificar já reduzido; sem biblioteca nova). Remover banner apaga o arquivo.
- **Custo total do evento:** ingresso + custos extras (`EventCostType`), mostrando "Estimado" e "Pago" separados.
- **Backup:** nova opção **"Backup completo (.zip)"** contendo `data.json` + pasta `banners/`. A importação aceita `.json` antigo e `.zip` novo. O backup `.json` simples continua existindo (sem imagens) e avisa que banners não estão incluídos.
- **Compromissos vinculados:** na tela do evento, listar tarefas, compromissos (`appointments.eventId`) e lançamentos vinculados, com botão para criar cada um já vinculado.

**Aceite:** evento sem local salva; banner escolhido aparece e sobrevive ao fechar o app; backup .zip restaurado traz os banners.

---

## 9. ETAPA 6 — Início, Mercado e identidade

### 9.1 Identidade
- **Símbolo N:** vetor (`VectorDrawable`/`Path` em Compose), branco, geométrico, traço encorpado, com um pequeno corte característico na junção das hastes. Usado no cabeçalho, no ícone adaptativo (fundo preto) e na splash.
- **Splash:** API oficial `androidx.core:core-splashscreen`, fundo preto, N branco centralizado, sem atraso artificial.
- **Cabeçalho:** N à esquerda, saudação e data como hierarquia (saudação maior, data menor em cinza), busca e menu de três pontos à direita, alinhados e com alvo de toque adequado. Barra recolhe discretamente na rolagem se isso for simples e estável.

### 9.2 Bloco "Mercado" (cotações) — opcional
- **Única etapa que adiciona a permissão `INTERNET`.** O restante do app continua funcional sem internet.
- Fonte de moedas (sem chave): `https://economia.awesomeapi.com.br/last/USD-BRL,EUR-BRL,BTC-BRL` e histórico `https://economia.awesomeapi.com.br/json/daily/USD-BRL/30`.
- Ibovespa (opcional): brapi, símbolo `^BVSP`, exige token pessoal. Campo opcional em Configurações onde o usuário cola o token. **O token nunca vai no código, no repositório nem no backup JSON.** Plano gratuito tem limite mensal, um ativo por chamada e atraso de cerca de 30 minutos.
- Rede com `java.net` (sem biblioteca nova), timeout de 8 s, execução fora da thread principal.
- **Cache local** em tabelas `market_quotes(symbol, bid, pctChange, fetchedAt)` e `market_history(symbol, dateIso, close)`.
- **Atualização:** ao abrir o Início, puxar para atualizar, e no máximo a cada 30 minutos. Sem atualização em segundo plano.
- **Interface:** cartão com cada ativo (nome, valor, variação do dia em verde/vermelho discretos) e mini gráfico de linha (`Sparkline`) dos últimos 30 dias. Sem internet ou erro da API: mostra o último valor com "atualizado em dd/MM HH:mm" e nunca exibe erro técnico.
- Rodapé discreto: "Informativo. Não é recomendação de investimento."
- Bloco aparece na lista de blocos personalizáveis do Início e pode ser desligado.

### 9.3 Cadastro rápido
`QuickAddChooser` com quatro opções, cada uma com descrição de uma linha:
- **Tarefa** — algo para fazer
- **Compromisso** — tempo reservado, com hora
- **Evento** — ocasião com nome, local ou ingresso
- **Gasto ou receita** — dinheiro que entrou ou saiu

Cada formulário abre só com o essencial e um "Mais detalhes" que expande o resto. Criar dentro de um dia da Agenda preenche a data; dentro de um evento vincula ao evento; a partir de uma tarefa vincula à tarefa.

---

## 10. Testes e CI

- Funções puras com teste JUnit: datas (2.1), `resolveBackAction`, `FinanceMonthSummary`, `AgendaEngine` (filtros, faturas, multi-dia), recorrência, regras de lembrete, parsers de backup (v1…vN).
- O workflow do GitHub Actions deve rodar `./gradlew testDebugUnitTest` antes de gerar o APK, assinar sempre com a MESMA chave (segredos do GitHub) e publicar a pasta `app/schemas` como artefato. Isso é mantido fora do código do app.
- Nenhum teste "decorativo": cada teste verifica uma regra real.

---

## 11. Fora do escopo (não fazer)

IA online ou local, sincronização em nuvem, leitura de extratos, múltiplos usuários, Play Store, busca automática de imagens, contas bancárias, troca de banco de dados, migração para NavHost, reescrita do app.

---

## 12. Ordem e critérios de pronto

| Etapa | Entrega | Pronto quando |
|---|---|---|
| 1 | Correções críticas | Seção 4, aceite cumprido |
| 2 | Agenda limpa + tarefa→gasto | Seção 5.2, aceite cumprido |
| 3 | Financeiro visual | Seção 6.8, aceite cumprido |
| 4 | Recorrência | Seção 7, aceite cumprido |
| 5 | Eventos | Seção 8, aceite cumprido |
| 6 | Identidade, Mercado e cadastro rápido | Seção 9 |

Cada etapa termina com: build no GitHub Actions, backup exportado e restaurado em teste, APK instalado por cima do anterior sem perder dados.
