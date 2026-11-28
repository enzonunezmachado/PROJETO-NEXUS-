# 16 — Guia Operacional do ChatGPT para Integração com o NEXUS (`contractVersion = 1`)

> **Versão deste guia:** `1.1.0` (2026-11-27) · **Estado:** **GUIA OPERACIONAL V1.1 ATUALIZADO A PARTIR DO CÓDIGO REAL (`Room v8` / `Backup v9` / `RECURRENCE` + Checklists de Agenda)**  
> **Compatibilidade verificada no código:** App `com.nexus.app` `v5.3.0` (`versionCode = 8`), Room `DATABASE_VERSION = 8`, Backup `CURRENT_FORMAT_VERSION = 9`, Entrada Estruturada `StructuredInputEnvelope.CURRENT_CONTRACT_VERSION = 1` (`8` tipos).  
> **Escopo de validação:** todo o conteúdo técnico, nomes de campos, valores de enum, regras de validação, limitações e exemplos JSON deste guia foram extraídos diretamente de `StructuredInputModels.kt`, `StructuredInputProcessor.kt`, `StructuredInputGateway.kt`, `RecurrenceEngine.kt`, `NexusEnums.kt`, `NexusEntities.kt`, `NexusDatabase.kt`, `NexusMigrations.kt`, `data/inbox/*`, `NexusInboxScreen.kt`, `StructuredInputUnitTests.kt`, `OccurrenceChecklistIsolationUnitTests.kt` e `InboxBridgeUnitTests.kt`.  
> **Nota de ambiente:** este workspace não possui JDK, Gradle ou Android SDK; portanto, os exemplos JSON deste documento foram conferidos por inspeção estática e parser JSON estrito contra os DTOs reais do código, mas a execução de `:app:testDebugUnitTest` (`21` suítes / `170` `@Test`) ocorre após sincronização com o GitHub/CI.

---

## Como usar este guia (leitura obrigatória para o ChatGPT)

Quando o usuário conversar com você e pedir algo como *"Registra tudo isso no NEXUS"*, este documento é o seu **manual operacional oficial**. Ele ensina:

1. **Como o NEXUS funciona conceitualmente** e onde cada dado aparece dentro do app;
2. **Qual entidade escolher** para cada necessidade do usuário (sem duplicar registros);
3. **Exatamente quais campos e enums existem** no contrato `v1` real (`ignoreUnknownKeys = false`), incluindo **`RECURRENCE`** e **`checklist`** em `APPOINTMENT` e `EVENT`;
4. **Como criar (`CREATE`) e atualizar (`UPDATE` / `UPSERT`)** sem apagar campos locais por acidente — incluindo versionamento não retroativo de checklists recorrentes (`effectiveFromDateIso`);
5. **O que o contrato `v1` ainda NÃO suporta** (exceções `SKIP`/`OVERRIDE` isoladas por ocorrência via JSON, catálogo de ícones `GAP-005` e leitura bidirecional `GAP-006`) — e como responder com honestidade sem inventar campos;
6. **Como preparar, nomear e enviar arquivos JSON** para o repositório privado `NEXUS-INBOX` com segurança e previsibilidade.

**Regra de autoridade:** nunca invente nomes de campos, valores de enum, IDs de categorias/cartões/faturas ou capacidades não listadas neste documento. Na dúvida sobre intenção ou dado essencial ausente, pergunte ao usuário antes de gerar o JSON.

---

## 1. Visão Operacional do NEXUS para o ChatGPT

### 1.1 O que é o NEXUS e seu modelo mental

O **NEXUS** é um centro de controle pessoal e profissional **offline-first** em português brasileiro (`pt-BR`, moeda `BRL`, fuso `America/Sao_Paulo`). Todos os dados residem localmente no banco SQLite/Room único do aparelho (`nexus_local.db`), sem login obrigatório nem backend proprietário.

A ponte **ChatGPT → `NEXUS-INBOX` → NEXUS Android** é um canal unidirecional assíncrono e revisável:
- Você **não** escreve diretamente no celular do usuário.
- Você prepara um ou mais arquivos `.json` seguindo exatamente o contrato `v1` para o repositório privado **`NEXUS-INBOX`** no GitHub.
- No aplicativo Android (**Mais → NEXUS-INBOX (Ponte ChatGPT)**), o usuário consulta a caixa de entrada (ou recebe notificação de verificação periódica), revisa a **prévia dry-run** com tudo o que será criado, atualizado, ignorado ou rejeitado, e decide se toca em **"Confirmar importação"** ou **"Descartar"**.

### 1.2 Princípio central: "Registrar uma vez, ver em vários lugares"

O NEXUS **não duplica registros** entre módulos. Uma entidade salva uma única vez aparece automaticamente nas telas correspondentes por meio de **chaves estrangeiras (`FK`)** e motores de leitura compartilhados:

| Entidade criada/atualizada via Inbox `v1` | Tarefas / Projetos | Agenda | Eventos | Financeiro | Início (`HomeScreen`) | Lembretes e Widgets |
| --- | --- | --- | --- | --- | --- | --- |
| **`TASK`** | Sempre (lista e Kanban) | Aparece **somente se `dueAt != null`** | Se `eventId != null` | Mostra `"Gasto: R$ X"` quando houver `TRANSACTION` ligada (`taskId`) | Anel de tarefas, hoje e atrasadas | Agenda lembrete se `dueAt != null` e ativa; atualiza widgets |
| **`CHECKLIST`** | Subtarefas da `TASK` pai (`subtasks`) | Contagem/progresso na tarefa quando aplicável | Via tarefa ligada ao evento | — | Contadores da tarefa | Atualiza widgets |
| **`APPOINTMENT`** | — | Sempre (faixa `startAt..endAt`, conflitos e progresso do `checklist` próprio) | Se `eventId != null` | — | Bloco "Agenda do dia" | Agenda lembrete antes de `startAt`; atualiza widgets |
| **`EVENT`** | Exibe tarefas ligadas (`eventId`) | Faixa multi-dia (`startAt..endAt`) + progresso do `checklist` próprio | Sempre (com `checklist` em `schedule_checklist_items`) | **Atenção:** via Inbox `v1`, `EVENT` **não** gera `TRANSACTION` automática (ver §3.7) | Bloco "Próximos eventos" | Agenda lembretes de evento/ingresso; atualiza widgets |
| **`RECURRENCE`** | Se `domain == "TASK"`, gera/projeta tarefas recorrentes | Projeta ocorrências virtuais para `TASK`, `APPOINTMENT` e `EVENT` (sem duplicar linhas no SQLite) | Se `domain == "EVENT"`, projeta na tela de Eventos sem criar transações fantasma | Nunca cria `TRANSACTION` automática | Próximas ocorrências projetadas | Agenda lembretes virtuais (`rec::...`); checklists por ocorrência com versionamento não retroativo |
| **`PROJECT`** | Aba Projetos (calcula progresso pelas `TASK` ligadas e custo pelas `TRANSACTION` ligadas) | — | — | Custos agregados via `projectId` | Bloco de projetos/metas | Atualiza widgets |
| **`PERSONAL_GOAL`** | Aba Objetivos (`AUTO` calcula progresso de projetos/tarefas/marcos; `MANUAL` usa `manualProgressPercent`) | — | — | — | Bloco de metas | Atualiza widgets |
| **`TRANSACTION`** | Exibe vínculo se `taskId != null` | Se despesa direta (`EXPENSE`, `statementId == null`, não liquidada/cancelada) com vencimento/ocorrência | Soma nos custos do evento (`eventId` + `eventCostType`) | Sempre (mês de `occurredAt`, gráficos, previsão) | Resumo financeiro e próximos pagamentos | Agenda lembrete se pendente/prevista com vencimento futuro; atualiza widgets |

### 1.3 Status real em três camadas (o que já funciona vs. o que está fora da Inbox `v1`)

| Camada | Situação factual no código atual (`v5.3.0`) | Impacto direto para o ChatGPT |
| --- | --- | --- |
| **Camada 1 — Ponte `NEXUS-INBOX` (`contractVersion = 1` — Base Rodada 3)** | **IMPLEMENTADA E VALIDADA NO FLUXO PRINCIPAL** (Rodada 3: GitHub Actions #70 + importação real confirmada no aparelho): `TASK`, `CHECKLIST`, `APPOINTMENT`, `EVENT`, `PROJECT`, `PERSONAL_GOAL` e `TRANSACTION` com `CREATE`, `UPDATE` e `UPSERT`. | Totalmente operacional em produção. |
| **Camada 2 — Evoluções Rodada 4 no código (`Room v8` / `Backup v9` / `contractVersion = 1` expandido)** | **IMPLEMENTADA NO CÓDIGO, AGUARDANDO CI E VALIDAÇÃO EM DISPOSITIVO:** recorrência multi-domínio (`GAP-001`), independência de checklists (`GAP-003`), checklists próprios em `APPOINTMENT`/`EVENT` com versionamento não retroativo (`GAP-004`/`REQ-029`), extensão `RECURRENCE` + `APPOINTMENT.checklist`/`EVENT.checklist` na Inbox (`GAP-002`) e Restauração de Fábrica. | **Disponível no contrato `v1` atualizado** (`8` tipos) assim que o usuário compilar/instalar o APK da Rodada 4 (`Room v8`). |
| **Camada 3 — Recursos planejados e não implementados** | Exceções `RecurrenceExceptionEntity` (`SKIP`/`OVERRIDE`) isoladas via JSON, `GAP-005` (catálogo de ícones/identidade contextual) e `GAP-006` (catálogo exportável e snapshot bidirecional). | **Proibido** inventar campos de ícone/exceção ou presumir leitura automática do banco do aparelho. |

---

## 2. Guia de Decisão: Qual Entidade Usar?

Antes de montar qualquer JSON, classifique cada intenção do usuário usando a árvore de decisão abaixo.

### 2.1 Árvore rápida de classificação

```text
O usuário quer registrar algo no NEXUS:
├── É dinheiro entrando ou saindo (receita, despesa, conta, ingresso pago, gasto de tarefa/projeto/evento)?
│   └── Use TRANSACTION (INCOME ou EXPENSE). Nunca crie TASK para representar boleto ou conta a pagar.
├── É uma rotina ou compromisso/evento/tarefa que se REPETE (diário, semanal, dias específicos, quinzenal, mensal, anual ou a cada N dias), com ou sem checklist-modelo reutilizável?
│   └── Use RECURRENCE (domain: "TASK", "APPOINTMENT" ou "EVENT", com templateChecklist opcional). Nunca simule série com múltiplos registros avulsos!
├── É um bloco de horário reservado / reunião / consulta / compromisso único (com ou sem checklist próprio)?
│   └── Use APPOINTMENT (obrigatório startAt > 0 e endAt >= startAt; checklist opcional).
├── É um acontecimento maior único (show, conferência, viagem, festa, feira, curso presencial) com local, ingressos, custos e/ou checklist próprio?
│   └── Use EVENT (obrigatório startAt > 0 e endAt >= startAt; checklist opcional).
├── É um conjunto de entregas ou iniciativa temporária com várias tarefas e possível orçamento/prazo?
│   └── Use PROJECT (e conecte TASKs via projectId).
├── É uma meta pessoal contínua ou objetivo de médio/longo prazo que acompanha projetos/tarefas?
│   └── Use PERSONAL_GOAL (e conecte PROJECTs e TASKs via goalId).
├── É uma ação executável única (com ou sem prazo `dueAt`, com ou sem subtarefas)?
│   └── Use TASK (com `subtasks` inline na criação, ou sem subtarefas se simples).
└── São passos/itens de preparo para adicionar/atualizar em uma TASK que já existe (ou criada antes no mesmo lote)?
    └── Use CHECKLIST (apontando obrigatoriamente para `taskId`).
```

### 2.2 Exemplos positivos e negativos por entidade

| Entidade | Quando USAR (Exemplo positivo) | Quando NÃO USAR (Exemplo negativo e o que usar no lugar) |
| --- | --- | --- |
| **`TASK`** | *"Preciso revisar os slides até sexta às 18h"* → `TASK` com `dueAt`, `hasDueTime: true`. | *"Pagar conta de luz dia 10"* → **NÃO** use `TASK`; use `TRANSACTION` (`EXPENSE`, `status: "PENDING"`). |
| **`CHECKLIST`** | *"Adiciona 'levar passaporte' e 'imprimir voucher' naquela tarefa da viagem"* → `CHECKLIST` com `taskId` da tarefa existente. | *"Adiciona checklist no compromisso do dentista"* → Use `APPOINTMENT` com o campo `checklist`, não `CHECKLIST` (pois `CHECKLIST` exige `taskId`). |
| **`APPOINTMENT`** | *"Consulta com Dr. Silva terça de 14h às 15h na Av. Paulista, levar exames e RG"* → `APPOINTMENT` com `checklist`. | *"Toda quinta às 20h corro com o grupo"* → Use `RECURRENCE` (`domain: "APPOINTMENT"`), não `APPOINTMENT` único. |
| **`EVENT`** | *"Vou ao Summit Tech nos dias 10 e 11 de dezembro; paguei R$ 350 no ingresso; levar crachá e carregador"* → `EVENT` com `checklist` (+ `TRANSACTION` para o gasto). | *"Reunião de 1h com o cliente"* → Use `APPOINTMENT`, não `EVENT`. |
| **`RECURRENCE`** | *"Toda terça e quinta às 07:00 tenho treino de academia com checklist: garrafa, luva, fone"* → `RECURRENCE` (`domain: "APPOINTMENT"`, `frequency: "SPECIFIC_WEEKDAYS"`, `weekdays: ["TUESDAY", "THURSDAY"]`, `startTime: "07:00"`, `templateChecklist`). | *"Alterar apenas o treino de quinta que vem para 08:00"* → Exceções pontuais (`SKIP`/`OVERRIDE`) continuam sendo editadas no app (`"Só esta ocorrência"`). |
| **`PROJECT`** | *"Quero organizar a mudança de apartamento com várias tarefas e orçamento"* → `PROJECT` + `TASK`s (`projectId`) + `TRANSACTION`s (`projectId`). | *"Trocar lâmpada da sala"* → Ação única é `TASK`, não `PROJECT`. |
| **`PERSONAL_GOAL`** | *"Objetivo: passar na certificação AWS até março de 2027"* → `PERSONAL_GOAL` (`AUTO`) + `PROJECT`/`TASK` vinculados via `goalId`. | *"Fazer simulado 1 amanhã"* → Ação executável é `TASK`, ligada ao objetivo via `goalId`. |
| **`TRANSACTION`** | *"Gastei R$ 89,90 no Uber para o evento no Pix"* → `TRANSACTION` (`EXPENSE`, `paymentMethod: "PIX"`, `eventId`, `eventCostType: "TRANSPORT"`, `status: "SETTLED"`). | *"Paguei a fatura de R$ 1.200 do cartão"* quando as compras já estão no cartão → **Bloqueado pelo app** se caracterizar dupla contagem (veja §3.11). |

---

## 3. Catálogo Completo de Entidades, Campos Reais e Funcionalidades (`contractVersion = 1`)

### 3.1 Regras gerais do parser JSON estrito

O NEXUS decodifica o JSON usando `kotlinx.serialization.json.Json` com a configuração:
- `ignoreUnknownKeys = false` → **qualquer chave fora dos DTOs abaixo rejeita o arquivo inteiro!**
- `isLenient = false` → JSON estrito (sem comentários `//`, sem vírgula sobrando no final, todas as chaves e strings entre aspas duplas).
- `coerceInputValues = false` → tipos devem bater exatamente (`Long` inteiro sem aspas, `Boolean` `true`/`false` sem aspas, `String` entre aspas, `null` explícito apenas nos campos anuláveis).

**Fuso horário e datas:**
- Todos os campos temporais do contrato `v1` são **epoch milliseconds (`Long`)** — milissegundos UTC desde `1970-01-01T00:00:00Z`.
- O usuário conversa no fuso oficial do NEXUS: **`America/Sao_Paulo` (`UTC-3`)**. Sempre converta data/hora informada pelo usuário de `America/Sao_Paulo` para epoch milliseconds (`Long`).
- Exemplo de conferência: `2026-12-01T14:00:00-03:00` equivale a `1796144400000`.
- Quando a data/hora for `0L` em campos obrigatórios de data (`startAt`, `endAt`, `occurredAt`), a operação é **rejeitada**. Em `createdAt` do envelope/operação, se omitido ou `0`, o processador usa o relógio local no momento da importação; **boas práticas:** sempre envie o epoch ms real de criação.

### 3.2 Estrutura do Envelope e da Operação

#### `StructuredInputEnvelope` (raiz do arquivo `.json`)
| Campo | Tipo Kotlin | Obrigatório? | Padrão | Regra real em `StructuredInputProcessor` |
| --- | --- | --- | --- | --- |
| `contractVersion` | `Int` | **Sim** (ou default `1`) | `1` | Tem que ser exatamente `1`. Qualquer outro valor rejeita todo o lote com `envelopeError`. |
| `source` | `String` | **Sim** (no envelope ou em todas as ops) | `""` | Recomendado: `"chatgpt-nexus-inbox"`. Se vazio no envelope E em alguma operação, rejeita o lote inteiro. |
| `createdAt` | `Long` | Recomendado | `0L` | Deve ser `>= 0`. Epoch ms de geração do JSON. |
| `operations` | `List<StructuredInputOperation>` | **Sim** | `emptyList()` | Lista ordenada de operações. A ordem importa para dependências no mesmo lote! |

#### `StructuredInputOperation` (cada item de `operations`)
| Campo | Tipo Kotlin | Obrigatório? | Padrão | Regra real em `StructuredInputProcessor` |
| --- | --- | --- | --- | --- |
| `operationId` | `String` | **Sim** | `""` | Não pode ser vazio. É a **chave de idempotência** salva em `audit_history`. Nunca reutilize o mesmo `operationId` para operações diferentes. |
| `entityType` | `String` | **Sim** | `""` | Case-insensitive, exatamente um dos 8: `"TASK"`, `"CHECKLIST"`, `"APPOINTMENT"`, `"EVENT"`, `"RECURRENCE"`, `"PROJECT"`, `"PERSONAL_GOAL"`, `"TRANSACTION"`. |
| `action` | `String` | Opcional | `"CREATE"` | `"CREATE"`, `"UPDATE"` ou `"UPSERT"` (string vazia assume `"CREATE"`). |
| `data` | `JsonObject?` | **Sim** | `null` | Objeto JSON exato do DTO correspondente. Se `null`/ausente, rejeita a operação. |
| `contractVersion` | `Int` | Opcional | `1` | Deve ser igual a `1` e igual ao `contractVersion` do envelope. |
| `createdAt` | `Long` | Opcional | `0L` | Deve ser `>= 0`. Se `0`, herda `envelope.createdAt` (ou relógio atual). |
| `source` | `String` | Opcional | `""` | Se omitido/vazio, herda `envelope.source`. |
| `processingState`, `resultDetail`, `rejectionReason` | — | **NÃO ENVIAR** | — | Campos de saída/recibo do app. Não inclua na entrada do ChatGPT. |

---

### 3.3 Categorias padrão garantidas e política de chaves estrangeiras (`FK`)

Ao validar qualquer `categoryId`, `projectId`, `eventId`, `taskId`, `goalId`, `recurrenceId`, `creditCardId`, `statementId`, `installmentPurchaseId` ou `recurringRuleId`, o processador verifica se o ID existe no SQLite **ou foi criado por uma operação anterior no mesmo arquivo JSON** (incluindo na prévia em memória `PreviewOverlayGateway`). **Qualquer FK inexistente causa `REJECTED` na operação.**

**Categorias semeadas de fábrica no NEXUS (`NexusDatabase.kt` / `NexusMigrations.kt`):**

| Escopo no app | `categoryId` real garantido | Nome no app |
| --- | --- | --- |
| `GENERAL` (Tarefas, Compromissos, Objetivos, Agenda) | `"cat-default-pessoal"` | Pessoal |
| `GENERAL` | `"cat-default-trabalho"` | Trabalho |
| `GENERAL` | `"cat-default-saude"` | Saúde |
| `GENERAL` | `"cat-default-estudos"` | Estudos |
| `FINANCE` (Transações) | `"cat-fin-alimentacao"` | Alimentação |
| `FINANCE` | `"cat-fin-transporte"` | Transporte |
| `FINANCE` | `"cat-fin-faculdade"` | Faculdade |
| `FINANCE` | `"cat-fin-moradia"` | Moradia |
| `FINANCE` | `"cat-fin-tecnologia"` | Tecnologia |
| `FINANCE` | `"cat-fin-assinaturas"` | Assinaturas |
| `FINANCE` | `"cat-fin-compras"` | Compras |
| `FINANCE` | `"cat-fin-eventos"` | Eventos |
| `FINANCE` | `"cat-fin-saude"` | Saúde e bem-estar |
| `FINANCE` | `"cat-fin-salario"` | Salário |
| `FINANCE` | `"cat-fin-freelance"` | Freelance |
| `FINANCE` | `"cat-fin-outros"` | Outros |

> **Regra para categorias e FKs:**  
> 1. Use os `cat-default-*` e `cat-fin-*` acima apenas quando se encaixarem claramente (e lembre-se de que o usuário pode ter renomeado ou apagado categorias manualmente — se incerto ou se o usuário pedir uma categoria customizada, omita `categoryId` ou peça o ID).  
> 2. **Nunca adivinhe** `creditCardId`, `statementId`, `installmentPurchaseId`, `recurringRuleId`, `recurrenceId` ou IDs de registros locais que você mesmo não criou — só use esses campos se o usuário fornecer o ID exato ou se o registro pai estiver em operação anterior do lote (nos tipos criáveis via Inbox).

---

### 3.4 DTO 1: `TASK` (`StructuredTaskData`) e sub-DTO `StructuredSubtaskItem`

| Campo (`StructuredTaskData`) | Tipo | Obrigatório? | Padrão | Validação / Regra no app |
| --- | --- | --- | --- | --- |
| `id` | `String?` | Opcional | `null` | Se omitido/vazio, o app gera `"ing-task-$operationId"`. Recomenda-se usar ID determinístico (ex.: `"task-viagem-sp-2026"`) quando outra operação do lote for referenciar `taskId`. |
| `title` | `String` | **Sim** | `""` | Não pode ser vazio após `.trim()`. |
| `description` | `String` | Opcional | `""` | Descrição longa da tarefa. |
| `projectId` | `String?` | Opcional | `null` | FK verificada em `projects`. |
| `categoryId` | `String?` | Opcional | `null` | FK verificada em `categories` (ex.: `"cat-default-trabalho"`). |
| `eventId` | `String?` | Opcional | `null` | FK verificada em `events`. |
| `goalId` | `String?` | Opcional | `null` | FK verificada em `personal_goals`. |
| `priority` | `String` | Opcional | `"NORMAL"` | Enum `TaskPriority`: `"LOW"`, `"NORMAL"`, `"HIGH"`, `"URGENT"`. |
| `status` | `String` | Opcional | `"NOT_STARTED"` | Enum `TaskStatus`: `"NOT_STARTED"`, `"IN_PROGRESS"`, `"WAITING"`, `"BLOCKED"`, `"COMPLETED"`, `"CANCELLED"`. **Obs.:** não existe status `"OVERDUE"`; atraso é calculado dinamicamente pelo app com base em `dueAt`. |
| `dueAt` | `Long?` | Opcional | `null` | Epoch ms do prazo. **A tarefa só aparece na Agenda e dispara lembrete se `dueAt != null`.** |
| `hasDueTime` | `Boolean` | Opcional | `false` | `true` se o prazo tem horário específico (ex.: 18:00); `false` se vale para o dia. |
| `notes` | `String` | Opcional | `""` | Observações livres. |
| `completedAt` | `Long?` | Opcional | `null` | Ignorado (gravado como `null`) se `status != "COMPLETED"`. Se `status == "COMPLETED"`, usa `dto.completedAt ?: existing?.completedAt ?: now`. |
| `recurrenceId` | `String?` | Opcional | `null` | FK verificada em `task_recurrences` (série **já existente** no app). **Não cria regra nova.** Forma chave natural `(recurrenceId, occurrenceDateIso)`. |
| `occurrenceDateIso` | `String?` | Opcional | `null` | Data ISO (`"YYYY-MM-DD"`) associada a `recurrenceId` existente. |
| `subtasks` | `List<StructuredSubtaskItem>` | Opcional | `emptyList()` | Lista inline de itens do checklist na tabela `subtasks`. |

**Sub-DTO `StructuredSubtaskItem` (usado em `TASK.subtasks` e `CHECKLIST.items`):**
| Campo | Tipo | Obrigatório? | Padrão | Validação / Regra |
| --- | --- | --- | --- | --- |
| `id` | `String?` | Opcional | `null` | Em `TASK.subtasks`: se omitido/vazio, casa por `title` case-insensitive na tarefa ou gera `"ing-sub-$taskId-$index"`. Em `CHECKLIST.items`: gera `"ing-sub-$operationId-$index"`. |
| `title` | `String` | **Sim** | `""` | Não pode ser vazio após `.trim()`. |
| `isCompleted` | `Boolean` | Opcional | `false` | Atenção: o nome exato é **`isCompleted`** (nunca `isDone` ou `checked`). |
| `sortOrder` | `Int?` | Opcional | `null` | Atenção: o nome exato é **`sortOrder`** (nunca `orderIndex` ou `order`). Se `null`, preserva o existente ou acrescenta ao final (`max + 1 + index`). |

---

### 3.5 DTO 2: `CHECKLIST` (`StructuredChecklistData`)

Grava na tabela `subtasks` vinculada a uma **`TASK` existente** (no SQLite ou criada antes no mesmo lote). **Não funciona para Compromissos nem Eventos.**

| Campo | Tipo | Obrigatório? | Padrão | Validação / Regra real |
| --- | --- | --- | --- | --- |
| `id` | `String?` | Opcional | `null` | Usado apenas quando informado o formato unitário com `title` em vez de `items`. |
| `taskId` | `String` | **Sim** | `""` | ID da `TASK` pai. Rejeitado se vazio ou se a tarefa não existir. |
| `title` | `String?` | Condicional | `null` | Forma unitária (1 item). Ignorado se `items` não estiver vazio! |
| `isCompleted` | `Boolean` | Opcional | `false` | Usado apenas na forma unitária (`title`). |
| `sortOrder` | `Int?` | Opcional | `null` | Usado apenas na forma unitária (`title`). |
| `items` | `List<StructuredSubtaskItem>` | Condicional | `emptyList()` | **Preferido.** Obrigatório enviar `items` não vazio **ou** `title` não vazio. |

**Peculiaridades reais de `CHECKLIST` no código (`StructuredInputProcessor.prepareAndApplyChecklist`):**
1. **Ação e idempotência:** sempre adiciona ou atualiza itens sem remover subtarefas já existentes na tarefa que não constem no JSON.
2. **Comportamento de `action`:**
   - Em `CREATE`: se o item já existir (por `id` explícito), retorna `CONFLICT_PRESERVED_LOCAL`.
   - Em `UPDATE`: casa por `id` explícito ou por `title` (case-insensitive); se não encontrar o item na tarefa, retorna `REJECTED`.
   - Em `UPSERT`: atualiza os que já existem (por `id` ou `title`) e cria os novos!
3. **Classificação de recibo/prévia:** no código atual (`isUpdate = true` fixo), toda operação `CHECKLIST` bem-sucedida reporta **`WILL_UPDATE`** na prévia e **`UPDATED`** na execução (porque modifica o checklist de uma tarefa já existente).

---

### 3.6 DTO 3: `APPOINTMENT` (`StructuredAppointmentData`)

| Campo | Tipo | Obrigatório? | Padrão | Validação / Regra real |
| --- | --- | --- | --- | --- |
| `id` | `String?` | Opcional | `null` | Padrão se omitido: `"ing-appt-$operationId"`. |
| `title` | `String` | **Sim** | `""` | Não pode ser vazio. |
| `description` | `String` | Opcional | `""` | Detalhes do compromisso. |
| `categoryId` | `String?` | Opcional | `null` | FK verificada em `categories`. |
| `eventId` | `String?` | Opcional | `null` | FK verificada em `events` (ex.: jantar ou deslocamento ligado a um evento). |
| `startAt` | `Long` | **Sim** | `0L` | Deve ser **`> 0`**. Epoch ms do início. |
| `endAt` | `Long` | **Sim** | `0L` | Deve ser **`>= startAt`**. Epoch ms do término. |
| `isAllDay` | `Boolean` | Opcional | `false` | `true` para compromisso de dia inteiro. Compromissos sem `isAllDay` sobrepostos geram aviso de conflito na Agenda. |
| `location` | `String` | Opcional | `""` | Endereço/local textual. |
| `link` | `String` | Opcional | `""` | URL de reunião online/chamada/mapa. |
| `checklist` | `List<StructuredSubtaskItem>` | Opcional | `emptyList()` | Itens próprios do compromisso gravados em `schedule_checklist_items` (`id`, `title`, `isCompleted`, `sortOrder`). Mesclagem aditiva/upsert por `id` ou `title` (lista vazia não apaga itens existentes). |

**Observação sobre `APPOINTMENT`:**
- Compromissos pontuais usam `APPOINTMENT`; para compromissos que se repetem, use **`RECURRENCE`** com `"domain": "APPOINTMENT"` (ver §3.8). O DTO não possui campo `icon` (`GAP-005`).

---

### 3.7 DTO 4: `EVENT` (`StructuredEventData`)

| Campo | Tipo | Obrigatório? | Padrão | Validação / Regra real |
| --- | --- | --- | --- | --- |
| `id` | `String?` | Opcional | `null` | Padrão se omitido: `"ing-event-$operationId"`. |
| `name` | `String` | **Sim** | `""` | Atenção: o campo é **`name`** (e não `title`). Não pode ser vazio. |
| `description` | `String` | Opcional | `""` | Descrição do evento. |
| `categoryId` | `String?` | Opcional | `null` | FK verificada em `categories`. |
| `startAt` | `Long` | **Sim** | `0L` | Deve ser **`> 0`**. |
| `endAt` | `Long` | **Sim** | `0L` | Deve ser **`>= startAt`**. |
| `isAllDay` | `Boolean` | Opcional | `false` | Evento de dia inteiro. |
| `officialLink` | `String` | Opcional | `""` | Site oficial. |
| `ticketLink` | `String` | Opcional | `""` | Link do ingresso/voucher. |
| `locationName` | `String` | Opcional | `""` | Nome do local (ex.: `"São Paulo Expo"`). **Atenção:** não use `location`. |
| `address` | `String` | Opcional | `""` | Logradouro/número. |
| `city` | `String` | Opcional | `""` | Cidade. |
| `state` | `String` | Opcional | `""` | UF/Estado. |
| `notes` | `String` | Opcional | `""` | Notas adicionais. |
| `eventStatus` | `String` | Opcional | `"PLANNED"` | Enum `EventLifecycleStatus`: `"PLANNED"`, `"CONFIRMED"`, `"ATTENDED"`, `"CANCELLED"`. **Atenção:** não use `status`. |
| `ticketStatus` | `String` | Opcional | `"FREE"` | Enum `TicketStatus`: `"FREE"`, `"NOT_BOUGHT"`, `"PENDING"`, `"PARTIALLY_PAID"`, `"BOUGHT"`, `"PAID"`, `"CANCELLED"`. |
| `ticketPriceCents` | `Long` | Opcional | `0L` | Deve ser `>= 0`. Preço unitário em centavos. **Atenção:** não use `ticketUnitPriceCents`. |
| `ticketQuantity` | `Int` | Opcional | `1` | Deve ser `>= 1`. |
| `ticketFeesCents` | `Long` | Opcional | `0L` | Deve ser `>= 0`. Taxas em centavos (só informe se reais; nunca invente taxas). |
| `ticketTotalCents` | `Long` | Opcional | `0L` | Deve ser `>= 0`. Total em centavos. |
| `ticketPurchaseDate` | `Long?` | Opcional | `null` | Epoch ms da compra do ingresso. |
| `ticketDueDate` | `Long?` | Opcional | `null` | Epoch ms de vencimento do ingresso. |
| `paymentMethod` | `String` | Opcional | `"PIX"` | Enum `PaymentMethod`: `"PIX"`, `"DEBIT"`, `"CREDIT_CARD"`, `"CASH"`, `"BANK_TRANSFER"`, `"BOLETO"`, `"OTHER"`. |
| `creditCardId` | `String?` | Opcional | `null` | FK verificada em `credit_cards` (só envie se conhecer o ID local do cartão). |
| `installmentsCount` | `Int` | Opcional | `1` | Deve ser `>= 1`. |
| `checklist` | `List<StructuredSubtaskItem>` | Opcional | `emptyList()` | Itens próprios do evento gravados em `schedule_checklist_items` (`id`, `title`, `isCompleted`, `sortOrder`). Mesclagem aditiva/upsert por `id` ou `title`. |

> **ATENÇÃO — Efeito colateral importante de `EVENT` na Inbox `v1`:**  
> Diferente do editor manual da tela de Eventos (`NexusRepository.saveEvent`), o processador da Inbox grava apenas `EventEntity` (+ `schedule_checklist_items` quando `checklist` é enviado) e **NÃO cria automaticamente** nenhuma `TransactionEntity` nem `InstallmentPurchaseEntity` para o ingresso.  
> **Como agir:** se o usuário disser *"Comprei o ingresso do evento X por R$ 250 no Pix"* e quiser ver o gasto no módulo Financeiro e nos custos do evento, emita **duas operações no mesmo lote**: o `EVENT` e uma `TRANSACTION` ligada com `"eventId"`, `"eventCostType": "TICKET"` e `"isTicketObligation": true`.

---

### 3.8 DTO 5: `RECURRENCE` (`StructuredRecurrenceData`) — Séries Recorrentes Multi-Domínio (`GAP-002`)

Cria ou atualiza uma série recorrente real em `recurrences` (`TaskRecurrenceEntity`) para `TASK`, `APPOINTMENT` ou `EVENT`, com projeção virtual na Agenda/Eventos e versionamento **não retroativo** do checklist-modelo (`templateHistoryJson`).

| Campo (`StructuredRecurrenceData`) | Tipo | Obrigatório? | Padrão | Validação / Regra real em `StructuredInputProcessor` |
| --- | --- | --- | --- | --- |
| `id` | `String?` | Opcional | `null` | Padrão se omitido: `"ing-rec-$operationId"`. Use ID determinístico para poder atualizar a série no futuro (`UPDATE`/`UPSERT`). |
| `domain` | `String` | Opcional | `"TASK"` | Enum `RecurrenceDomain`: **`"TASK"`**, **`"APPOINTMENT"`** ou **`"EVENT"`**. **Imutável** em `UPDATE`/`UPSERT` (tentar trocar o domínio de uma série existente retorna `REJECTED`). |
| `title` | `String` | **Sim** | `""` | Não pode ser vazio após `.trim()`. |
| `description` | `String` | Opcional | `""` | Descrição da série. |
| `projectId` | `String?` | Opcional | `null` | FK verificada em `projects` (aplicada quando `domain == "TASK"`). |
| `categoryId` | `String?` | Opcional | `null` | FK verificada em `categories`. |
| `eventId` | `String?` | Opcional | `null` | FK verificada em `events` (ignorada quando `domain == "EVENT"`). |
| `goalId` | `String?` | Opcional | `null` | FK verificada em `personal_goals` (aplicada quando `domain == "TASK"`). |
| `priority` | `String` | Opcional | `"NORMAL"` | Enum `TaskPriority`: `"LOW"`, `"NORMAL"`, `"HIGH"`, `"URGENT"`. |
| `notes` | `String` | Opcional | `""` | Anotações da série. |
| `frequency` | `String` | Opcional | `"WEEKLY"` | Enum `TaskRecurrenceFrequency`: `"DAILY"`, `"SPECIFIC_WEEKDAYS"`, `"WEEKLY"`, `"BIWEEKLY"`, `"MONTHLY"`, `"ANNUAL"`, `"CUSTOM_DAYS"`. |
| `customIntervalDays` | `Int` | Opcional | `1` | Deve ser `>= 1` (usado em `CUSTOM_DAYS`). |
| `weekdays` | `List<String>` | Condicional | `emptyList()` | Aceita nomes em inglês (`"MONDAY"`..`"SUNDAY"`) ou pt-BR (`"SEG"`, `"TER"`, `"QUA"`, `"QUI"`, `"SEX"`, `"SAB"`, `"DOM"`). Obrigatório (ou `weekdaysMask > 0`) quando `frequency == "SPECIFIC_WEEKDAYS"`. |
| `weekdaysMask` | `Int?` | Condicional | `null` | Alternativa bitmask (Seg=`1`..Dom=`64`) quando `weekdays` estiver vazio. |
| `startDateIso` | `String` | **Sim** | `""` | Data inicial no formato estrito **`"YYYY-MM-DD"`** (ex.: `"2026-12-03"`). |
| `endDateIso` | `String?` | Opcional | `null` | Data final opcional `"YYYY-MM-DD"` (`>= startDateIso`). |
| `maxOccurrences` | `Int?` | Opcional | `null` | Se informado, deve ser `> 0`. |
| `hasDueTime` | `Boolean` | Opcional | `false` | Para `TASK`, define se há horário; para `APPOINTMENT`/`EVENT` com `isAllDay == false`, o processador exige horário (`startTime` ou `dueTimeMinutesOfDay`). |
| `dueTimeMinutesOfDay` | `Int?` | Condicional | `null` | Minutos desde `00:00` (`0..1439`). Alternativa numérica a `startTime`. |
| `startTime` | `String?` | Condicional | `null` | Horário **`"HH:mm"`** (ex.: `"20:00"`). Preferido sobre `dueTimeMinutesOfDay` quando ambos existem. |
| `isAllDay` | `Boolean` | Opcional | `false` | Ignorado (`false`) para `TASK`. Para `APPOINTMENT`/`EVENT`, se `true`, dispensa `startTime`. |
| `durationMillis` | `Long` | Opcional | `3600000L` | Duração em ms (padrão 1h). Para `APPOINTMENT`/`EVENT`, deve ser **`>= 60000`** (1 minuto). |
| `reminderMinutesBefore` | `Int?` | Opcional | `null` | Se informado, deve ser `>= 0`. |
| `location`, `link` | `String` | Opcional | `""` | Local e link para `APPOINTMENT`. |
| `officialLink`, `ticketLink`, `locationName`, `address`, `city`, `state` | `String` | Opcional | `""` | Metadados de local/links para `EVENT`. |
| `eventStatus` | `String` | Opcional | `"PLANNED"` | Enum `EventLifecycleStatus`. `"ATTENDED"` é sanitizado automaticamente (séries futuras nunca nascem como já comparecidas). |
| `ticketStatus` | `String` | Opcional | `"FREE"` | Enum `TicketStatus`. Valores pagos (`PAID`, `BOUGHT`, `PARTIALLY_PAID`) são sanitizados para `"NOT_BOUGHT"` na série (proteção anti-obrigação fantasma). |
| `ticketPriceCents`, `ticketQuantity`, `ticketFeesCents`, `paymentMethod`, `creditCardId`, `installmentsCount` | Vários | Opcional | `0 / 1 / 0 / "PIX" / null / 1` | Parâmetros de ingresso para `EVENT` recorrente (`installmentsCount in 1..120`; `creditCardId` verificado por FK). Nunca criam `TransactionEntity` automaticamente. |
| `templateChecklist` | `List<String>` | Opcional | `emptyList()` | Lista ordenada de títulos do checklist-modelo da série (itens vazios causam `REJECTED`). Cada ocorrência nasce com seus itens **desmarcados e independentes**. |
| `effectiveFromDateIso` | `String?` | Opcional | `null` | Formato `"YYYY-MM-DD"`. Em `UPDATE`/`UPSERT`, define a partir de qual data o novo `templateChecklist` passa a vigorar (**versionamento não retroativo**: ocorrências anteriores ou já materializadas permanecem intactas). Se omitido, usa a data atual em `America/Sao_Paulo`. |
| `isActive` | `Boolean` | Opcional | `true` | `false` pausa a projeção de novas ocorrências sem apagar o histórico. |

---

### 3.9 DTO 6: `PROJECT` (`StructuredProjectData`)

| Campo | Tipo | Obrigatório? | Padrão | Validação / Regra real |
| --- | --- | --- | --- | --- |
| `id` | `String?` | Opcional | `null` | Padrão se omitido: `"ing-proj-$operationId"`. |
| `name` | `String` | **Sim** | `""` | Atenção: o campo é **`name`** (não `title`). Não pode ser vazio. |
| `description` | `String` | Opcional | `""` | Escopo/resumo do projeto. |
| `deadlineAt` | `Long?` | Opcional | `null` | Epoch ms do prazo final. |
| `status` | `String` | Opcional | `"ACTIVE"` | Enum `ProjectStatus`: `"PLANNED"`, `"ACTIVE"`, `"ON_HOLD"`, `"COMPLETED"`, `"ARCHIVED"`. |
| `priority` | `String` | Opcional | `"NORMAL"` | Enum `TaskPriority`: `"LOW"`, `"NORMAL"`, `"HIGH"`, `"URGENT"`. |
| `goalId` | `String?` | Opcional | `null` | FK verificada em `personal_goals`. |

---

### 3.10 DTO 7: `PERSONAL_GOAL` (`StructuredPersonalGoalData`)

| Campo | Tipo | Obrigatório? | Padrão | Validação / Regra real |
| --- | --- | --- | --- | --- |
| `id` | `String?` | Opcional | `null` | Padrão se omitido: `"ing-goal-$operationId"`. |
| `title` | `String` | **Sim** | `""` | Não pode ser vazio. |
| `description` | `String` | Opcional | `""` | Descrição do objetivo. |
| `categoryId` | `String?` | Opcional | `null` | FK verificada em `categories`. |
| `targetDateAt` | `Long?` | Opcional | `null` | Epoch ms da data-alvo. |
| `status` | `String` | Opcional | `"ACTIVE"` | Enum `GoalStatus`: `"ACTIVE"`, `"PAUSED"`, `"COMPLETED"`, `"CANCELLED"`. |
| `progressMode` | `String` | Opcional | `"AUTO"` | Enum `GoalProgressMode`: `"AUTO"` (calcula a partir de marcos, projetos e tarefas ligados) ou `"MANUAL"` (usa `manualProgressPercent`). |
| `manualProgressPercent` | `Int` | Opcional | `0` | Deve estar estritamente entre **`0` e `100`**. |
| `completedAt` | `Long?` | Opcional | `null` | Se `status == "COMPLETED"`, usa `dto.completedAt ?: existing?.completedAt ?: now`; senão grava `null`. |
| `notes` | `String` | Opcional | `""` | Anotações. |

> **Limite:** marcos (`goal_milestones`) e registros manuais de evolução (`goal_progress_logs`) não possuem `entityType` na Inbox `v1`.

---

### 3.11 DTO 8: `TRANSACTION` (`StructuredTransactionData`)

| Campo | Tipo | Obrigatório? | Padrão | Validação / Regra real |
| --- | --- | --- | --- | --- |
| `id` | `String?` | Opcional | `null` | Padrão se omitido: `"ing-tx-$operationId"`. |
| `type` | `String` | **Sim** | `""` | Enum `TransactionType`: **`"INCOME"`** ou **`"EXPENSE"`**. |
| `description` | `String` | **Sim** | `""` | Não pode ser vazio. |
| `amountCents` | `Long` | **Sim** | `0L` | Deve ser **`> 0`** (sempre em centavos inteiros: R$ 125,50 = `12550`). |
| `categoryId` | `String?` | Opcional | `null` | FK verificada em `categories` (veja `cat-fin-*` em §3.3). |
| `status` | `String` | Opcional | `"PENDING"` | Enum `FinancialStatus`: `"PLANNED"`, `"PENDING"`, `"SETTLED"`, `"OVERDUE"`, `"CANCELLED"`. **Regra operacional:** use apenas `"PLANNED"`, `"PENDING"`, `"SETTLED"` ou `"CANCELLED"` — **nunca emita `"OVERDUE"`**, pois atraso é calculado dinamicamente sobre contas pendentes/vencidas! |
| `paymentMethod` | `String` | Opcional | `"PIX"` | Enum `PaymentMethod`: `"PIX"`, `"DEBIT"`, `"CREDIT_CARD"`, `"CASH"`, `"BANK_TRANSFER"`, `"BOLETO"`, `"OTHER"`. |
| `occurredAt` | `Long` | **Sim** | `0L` | Deve ser **`> 0`**. Data de competência/ocorrência que posiciona o lançamento nos gráficos e no mês financeiro. |
| `dueAt` | `Long?` | Opcional | `null` | Epoch ms do vencimento (usado na Agenda para contas diretas a pagar e lembretes). |
| `settledAt` | `Long?` | Opcional | `null` | Se `status == "SETTLED"`, usa `dto.settledAt ?: existing?.settledAt ?: dto.occurredAt`; caso contrário grava `null`. |
| `projectId` | `String?` | Opcional | `null` | FK verificada em `projects`. |
| `eventId` | `String?` | Opcional | `null` | FK verificada em `events`. |
| `taskId` | `String?` | Opcional | `null` | FK verificada em `tasks`. |
| `eventCostType` | `String?` | Opcional | `null` | Se informado e não vazio, deve pertencer a `EventCostType`: `"TICKET"`, `"TRANSPORT"`, `"FOOD"`, `"LODGING"`, `"PARKING"`, `"SHOPPING"`, `"OTHER"`. |
| `isTicketObligation` | `Boolean` | Opcional | `false` | Marque `true` quando representar a obrigação financeira do ingresso do `EVENT`. |
| `creditCardId` | `String?` | Opcional | `null` | FK verificada em `credit_cards`. |
| `statementId` | `String?` | Opcional | `null` | FK verificada em `credit_card_statements`. Se `creditCardId` também for informado, a fatura deve pertencer ao mesmo cartão. Faturas `PAID` rejeitam transações com `status != "SETTLED"`. |
| `installmentPurchaseId` | `String?` | Opcional | `null` | FK verificada em `installment_purchases`. Forma chave natural `(installmentPurchaseId, installmentNumber)`. |
| `installmentNumber` | `Int?` | Opcional | `null` | Se informado, deve ser `>= 1` e `<= installmentsTotal` (quando ambos presentes). |
| `installmentsTotal` | `Int?` | Opcional | `null` | Se informado, deve ser `>= 1`. |
| `recurringRuleId` | `String?` | Opcional | `null` | FK verificada em `recurring_rules`. Forma chave natural `(recurringRuleId, occurrenceIndex)`. |
| `occurrenceIndex` | `Int?` | Opcional | `null` | Índice da ocorrência da regra financeira recorrente. |
| `notes` | `String` | Opcional | `""` | Observações. |

**Regras de segurança financeira da `TRANSACTION` no processador (`StructuredInputProcessor`):**
1. **Anti-dupla contagem de fatura (`ADR-006`):** despesas avulsas (`statementId == null && installmentPurchaseId == null`) cuja descrição contenha `"pagamento de fatura"`, `"pagamento fatura"`, `"quitação de fatura"`, `"quitacao de fatura"` ou seja `"fatura paga"` são **rejeitadas** sempre que `creditCardId != null` ou quando houver fatura `PAID` compatível no app.
2. **Fatura sem vínculo automático na Inbox `v1`:** diferente do editor de finanças no app, `insertTransaction` na Inbox grava a `TransactionEntity` diretamente no Room e **não cria nem associa automaticamente uma `CreditCardStatementEntity` se `statementId` for omitido**. Portanto, se o usuário registrar compra no cartão e você não tiver o `statementId` real da fatura aberta, explique ao usuário antes ou registre explicitando a necessidade de associar à fatura no app caso ele controle faturas lá.

---

### 3.12 O que NÃO existe ou NÃO pode ser enviado via `NEXUS-INBOX` (`v1`)

| Recurso existente no app ou planejado | Pode enviar no JSON `v1`? | Como lidar corretamente |
| --- | --- | --- |
| Exceções pontuais (`RecurrenceExceptionEntity`: `SKIP` / `OVERRIDE` de 1 única data da série) | **NÃO** | A série é criada/atualizada via `RECURRENCE` (inclusive mudanças futuras não retroativas de `templateChecklist` com `effectiveFromDateIso`). Pular ou mover apenas 1 ocorrência específica continua sendo feito na UI do app (`"Só esta ocorrência"`). |
| Ícone / identidade contextual (`icon`, `contextType`, etc.) | **NÃO (`GAP-005`)** | Nenhum DTO aceita campo de ícone/contexto. Nunca invente identificadores de ícones (`ignoreUnknownKeys = false` rejeita o lote). |
| Leitura automática / snapshot do estado atual do aparelho | **NÃO (`GAP-006`)** | A ponte atual é unidirecional (ChatGPT → GitHub → Android). Peça confirmação ao usuário antes de assumir IDs ou campos locais. |
| Criar cartões (`credit_cards`), faturas (`credit_card_statements`), parcelamentos (`installment_purchases`), regras recorrentes financeiras (`recurring_rules`), orçamentos (`budgets`), empréstimos (`loans`), metas financeiras (`financial_goals`) ou categorias customizadas (`categories`) | **NÃO** | Esses cadastros não têm `entityType` na Inbox `v1`; devem ser criados diretamente no app. |
| Marcos de objetivo (`goal_milestones`), logs de progresso (`goal_progress_logs`), frases (`quotes`), preferências, lembretes customizados diretos ou alertas do Mercado | **NÃO** | Gerenciados apenas dentro do aplicativo (e lembretes padrão são recalculados automaticamente pelo app a partir das entidades importadas). |

---

## 4. Manual de Criação (`CREATE`) e Exemplos JSON Reais e Válidos

### 4.1 Passo a passo para gerar um arquivo `CREATE` válido

1. **Verifique o escopo:** todas as intenções cabem nos `8` tipos da Inbox `v1` (`TASK`, `CHECKLIST`, `APPOINTMENT`, `EVENT`, `RECURRENCE`, `PROJECT`, `PERSONAL_GOAL`, `TRANSACTION`)? Se envolver exceção pontual de 1 data, ícone (`GAP-005`) ou cadastro financeiro de cartão/fatura, aplique a Seção 6.
2. **Converta datas/horas em epoch ms (`America/Sao_Paulo`):** garanta `startAt > 0`, `endAt >= startAt`, `occurredAt > 0`.
3. **Escolha IDs consistentes e legíveis:** se uma entidade será referenciada por outra no mesmo JSON (ex.: `EVENT` → `TASK` → `CHECKLIST` / `TRANSACTION`), defina IDs explícitos e únicos em slug (ex.: `"evt-workshop-2026"`, `"task-prep-workshop"`) e ordene as operações **de pai para filho** (`PERSONAL_GOAL` → `PROJECT` → `EVENT` → `TASK` → `CHECKLIST` / `APPOINTMENT` → `TRANSACTION`).
4. **Gere `operationId` único por operação:** ex.: `"op-2026-12-01-task-01"`.
5. **Confira cada nome de campo e enum** contra a Seção 3 (lembrando `ignoreUnknownKeys = false`).

---

### 4.2 Exemplos JSON reais e validados estaticamente contra os DTOs

#### Exemplo 1 — Tarefa simples com prazo
*Pedido do usuário:* *"Anota para eu entregar o relatório trimestral até terça-feira, 01/12/2026 às 15:00, prioridade alta, na categoria Trabalho."*

```json
{
  "contractVersion": 1,
  "source": "chatgpt-nexus-inbox",
  "createdAt": 1795784400000,
  "operations": [
    {
      "operationId": "op-2026-11-27-task-relatorio",
      "entityType": "TASK",
      "action": "CREATE",
      "contractVersion": 1,
      "createdAt": 1795784400000,
      "data": {
        "id": "task-relatorio-trimestral-2026",
        "title": "Entregar relatório trimestral",
        "description": "Consolidar indicadores e enviar versão final para revisão",
        "categoryId": "cat-default-trabalho",
        "priority": "HIGH",
        "status": "NOT_STARTED",
        "dueAt": 1796148000000,
        "hasDueTime": true,
        "notes": "Prazo às 15:00 (01/12/2026)"
      }
    }
  ]
}
```
- **Resultado esperado:** cria 1 `TaskEntity` (`1796148000000` = `01/12/2026 15:00 -03:00`), visível na aba Tarefas, Kanban e Agenda, com lembrete agendado.

#### Exemplo 2 — Tarefa com checklist inline (`subtasks`)
*Pedido do usuário:* *"Cria uma tarefa de revisão de documentos para o dia 02/12/2026 às 18:00 com checklist: separar RG, baixar comprovante de residência e assinar termo."*

```json
{
  "contractVersion": 1,
  "source": "chatgpt-nexus-inbox",
  "createdAt": 1795784400000,
  "operations": [
    {
      "operationId": "op-2026-11-27-task-docs-01",
      "entityType": "TASK",
      "action": "CREATE",
      "contractVersion": 1,
      "createdAt": 1795784400000,
      "data": {
        "id": "task-revisar-documentos-2026",
        "title": "Revisar e enviar documentos",
        "categoryId": "cat-default-pessoal",
        "priority": "NORMAL",
        "status": "NOT_STARTED",
        "dueAt": 1796245200000,
        "hasDueTime": true,
        "subtasks": [
          { "id": "sub-docs-1", "title": "Separar RG", "isCompleted": false, "sortOrder": 0 },
          { "id": "sub-docs-2", "title": "Baixar comprovante de residência", "isCompleted": false, "sortOrder": 1 },
          { "id": "sub-docs-3", "title": "Assinar termo", "isCompleted": false, "sortOrder": 2 }
        ]
      }
    }
  ]
}
```
- **Resultado esperado:** cria 1 `TaskEntity` (`1796245200000` = `02/12/2026 18:00 -03:00`) + 3 `SubtaskEntity`. Aparece na Agenda e gera lembrete.

#### Exemplo 3 — Compromisso único (`APPOINTMENT`)
*Pedido do usuário:* *"Marca minha consulta médica com o Dr. Henrique dia 01/12/2026 das 14:00 às 15:00 na Clínica Central, categoria Saúde."*

```json
{
  "contractVersion": 1,
  "source": "chatgpt-nexus-inbox",
  "createdAt": 1795784400000,
  "operations": [
    {
      "operationId": "op-2026-11-27-appt-consulta",
      "entityType": "APPOINTMENT",
      "action": "CREATE",
      "contractVersion": 1,
      "createdAt": 1795784400000,
      "data": {
        "id": "appt-consulta-dr-henrique",
        "title": "Consulta médica — Dr. Henrique",
        "description": "Retorno com exames",
        "categoryId": "cat-default-saude",
        "startAt": 1796144400000,
        "endAt": 1796148000000,
        "isAllDay": false,
        "location": "Clínica Central"
      }
    }
  ]
}
```
- **Resultado esperado:** aparece na Agenda em `01/12/2026 14:00–15:00` e no bloco "Agenda do dia" da Home, com detecção automática de conflito caso exista outro compromisso no mesmo horário.

#### Exemplo 4 — Evento pago com ingresso + despesa vinculada
*Pedido do usuário:* *"Vou ao Festival de Tecnologia no dia 05/12/2026 das 19:00 às 23:00 no Auditório Paulista em São Paulo/SP. Ingresso já pago via Pix por R$ 180,00 (sem taxas extras)."*

```json
{
  "contractVersion": 1,
  "source": "chatgpt-nexus-inbox",
  "createdAt": 1795784400000,
  "operations": [
    {
      "operationId": "op-2026-11-27-evt-techfest",
      "entityType": "EVENT",
      "action": "CREATE",
      "contractVersion": 1,
      "createdAt": 1795784400000,
      "data": {
        "id": "evt-festival-tech-2026",
        "name": "Festival de Tecnologia 2026",
        "description": "Palestras noturnas e networking",
        "categoryId": "cat-default-estudos",
        "startAt": 1796508000000,
        "endAt": 1796522400000,
        "isAllDay": false,
        "locationName": "Auditório Paulista",
        "city": "São Paulo",
        "state": "SP",
        "eventStatus": "CONFIRMED",
        "ticketStatus": "PAID",
        "ticketPriceCents": 18000,
        "ticketQuantity": 1,
        "ticketFeesCents": 0,
        "ticketTotalCents": 18000,
        "ticketPurchaseDate": 1795784400000,
        "paymentMethod": "PIX",
        "installmentsCount": 1
      }
    },
    {
      "operationId": "op-2026-11-27-tx-techfest-ticket",
      "entityType": "TRANSACTION",
      "action": "CREATE",
      "contractVersion": 1,
      "createdAt": 1795784400000,
      "data": {
        "id": "tx-ingresso-festival-tech-2026",
        "type": "EXPENSE",
        "description": "Ingresso — Festival de Tecnologia 2026",
        "amountCents": 18000,
        "categoryId": "cat-fin-eventos",
        "status": "SETTLED",
        "paymentMethod": "PIX",
        "occurredAt": 1795784400000,
        "settledAt": 1795784400000,
        "eventId": "evt-festival-tech-2026",
        "eventCostType": "TICKET",
        "isTicketObligation": true
      }
    }
  ]
}
```
- **Por que existem 2 operações?** Porque `EVENT` na Inbox `v1` não gera `TransactionEntity` automaticamente. Assim, tanto o módulo **Eventos** quanto o **Financeiro** ficam consistentes e sem duplicidade.

#### Exemplo 5 — Objetivo pessoal + Projeto + Tarefa vinculada a ambos
*Pedido do usuário:* *"Quero criar meu objetivo pessoal de concluir a certificação em Cloud até 31/03/2027 (progresso automático), o projeto 'Preparação Certificação Cloud' ligado a ele com prazo em 20/12/2026, e a tarefa 'Fazer simulado completo 1' até 15/12/2026."*

```json
{
  "contractVersion": 1,
  "source": "chatgpt-nexus-inbox",
  "createdAt": 1795784400000,
  "operations": [
    {
      "operationId": "op-2026-11-27-goal-cloud",
      "entityType": "PERSONAL_GOAL",
      "action": "CREATE",
      "contractVersion": 1,
      "createdAt": 1795784400000,
      "data": {
        "id": "goal-certificacao-cloud-2027",
        "title": "Concluir certificação em Cloud",
        "categoryId": "cat-default-estudos",
        "targetDateAt": 1806548340000,
        "status": "ACTIVE",
        "progressMode": "AUTO"
      }
    },
    {
      "operationId": "op-2026-11-27-proj-cloud",
      "entityType": "PROJECT",
      "action": "CREATE",
      "contractVersion": 1,
      "createdAt": 1795784400000,
      "data": {
        "id": "proj-prep-cert-cloud",
        "name": "Preparação Certificação Cloud",
        "description": "Estudos teóricos, laboratórios práticos e simulados",
        "deadlineAt": 1797800400000,
        "status": "ACTIVE",
        "priority": "HIGH",
        "goalId": "goal-certificacao-cloud-2027"
      }
    },
    {
      "operationId": "op-2026-11-27-task-simulado-1",
      "entityType": "TASK",
      "action": "CREATE",
      "contractVersion": 1,
      "createdAt": 1795784400000,
      "data": {
        "id": "task-simulado-cloud-01",
        "title": "Fazer simulado completo 1",
        "projectId": "proj-prep-cert-cloud",
        "goalId": "goal-certificacao-cloud-2027",
        "categoryId": "cat-default-estudos",
        "priority": "HIGH",
        "status": "NOT_STARTED",
        "dueAt": 1797389940000,
        "hasDueTime": false
      }
    }
  ]
}
```
- **Ordem topológica:** `PERSONAL_GOAL` → `PROJECT` (referencia `goalId`) → `TASK` (referencia `projectId` e `goalId`). Se estivessem invertidos, as operações filhas seriam rejeitadas por FK órfã!

#### Exemplo 6 — Lote completo `"Registra tudo isso no NEXUS"` (Evento + Compromisso ligado + Tarefa com Checklist + Despesa vinculada + Receita)
*Pedido do usuário:* *"Registra tudo isso no NEXUS: dia 10/12/2026 das 09:00 às 18:00 tenho a Conferência Anual de Engenharia no Centro de Convenções Expo em São Paulo/SP (evento gratuito e confirmado). Antes, dia 01/12/2026 às 09:00, tenho reunião de alinhamento de 1h da apresentação ligada ao evento. Cria também a tarefa 'Preparar viagem da conferência' vinculada ao evento com prazo em 02/12/2026 às 18:00 e checklist (Reservar hotel, Separar crachá), uma despesa pendente de R$ 450,00 no boleto da taxa de hospedagem ligada à tarefa e ao evento com vencimento dia 15/12/2026, e uma receita realizada hoje de R$ 2.500,00 de freelance via Pix."*

```json
{
  "contractVersion": 1,
  "source": "chatgpt-nexus-inbox",
  "createdAt": 1795784400000,
  "operations": [
    {
      "operationId": "op-2026-11-27-lote-evt-01",
      "entityType": "EVENT",
      "action": "UPSERT",
      "contractVersion": 1,
      "createdAt": 1795784400000,
      "data": {
        "id": "evt-conf-eng-2026",
        "name": "Conferência Anual de Engenharia",
        "categoryId": "cat-default-trabalho",
        "startAt": 1796904000000,
        "endAt": 1796936400000,
        "isAllDay": false,
        "locationName": "Centro de Convenções Expo",
        "city": "São Paulo",
        "state": "SP",
        "eventStatus": "CONFIRMED",
        "ticketStatus": "FREE",
        "ticketPriceCents": 0,
        "ticketQuantity": 1,
        "ticketFeesCents": 0,
        "ticketTotalCents": 0
      }
    },
    {
      "operationId": "op-2026-11-27-lote-appt-01",
      "entityType": "APPOINTMENT",
      "action": "UPSERT",
      "contractVersion": 1,
      "createdAt": 1795784400000,
      "data": {
        "id": "appt-alinhamento-conf-eng",
        "title": "Reunião de alinhamento da apresentação",
        "categoryId": "cat-default-trabalho",
        "eventId": "evt-conf-eng-2026",
        "startAt": 1796126400000,
        "endAt": 1796130000000,
        "isAllDay": false
      }
    },
    {
      "operationId": "op-2026-11-27-lote-task-01",
      "entityType": "TASK",
      "action": "UPSERT",
      "contractVersion": 1,
      "createdAt": 1795784400000,
      "data": {
        "id": "task-viagem-conf-eng",
        "title": "Preparar viagem da conferência",
        "categoryId": "cat-default-trabalho",
        "eventId": "evt-conf-eng-2026",
        "priority": "HIGH",
        "status": "NOT_STARTED",
        "dueAt": 1796245200000,
        "hasDueTime": true,
        "subtasks": [
          { "id": "sub-conf-1", "title": "Reservar hotel", "isCompleted": false, "sortOrder": 0 },
          { "id": "sub-conf-2", "title": "Separar crachá", "isCompleted": false, "sortOrder": 1 }
        ]
      }
    },
    {
      "operationId": "op-2026-11-27-lote-tx-01",
      "entityType": "TRANSACTION",
      "action": "CREATE",
      "contractVersion": 1,
      "createdAt": 1795784400000,
      "data": {
        "id": "tx-hospedagem-conf-eng",
        "type": "EXPENSE",
        "description": "Reserva de hospedagem — Conferência",
        "amountCents": 45000,
        "categoryId": "cat-fin-eventos",
        "status": "PENDING",
        "paymentMethod": "BOLETO",
        "occurredAt": 1795784400000,
        "dueAt": 1797389940000,
        "eventId": "evt-conf-eng-2026",
        "taskId": "task-viagem-conf-eng",
        "eventCostType": "LODGING",
        "isTicketObligation": false
      }
    },
    {
      "operationId": "op-2026-11-27-lote-tx-02",
      "entityType": "TRANSACTION",
      "action": "CREATE",
      "contractVersion": 1,
      "createdAt": 1795784400000,
      "data": {
        "id": "tx-freelance-nov-2026",
        "type": "INCOME",
        "description": "Consultoria técnica — Novembro",
        "amountCents": 250000,
        "categoryId": "cat-fin-freelance",
        "status": "SETTLED",
        "paymentMethod": "PIX",
        "occurredAt": 1795784400000,
        "settledAt": 1795784400000
      }
    }
  ]
}
```
> *Obs.:* `1796126400000` (`01/12 09:00`) + 1h = `1796130000000` (`01/12 10:00`).

---

## 5. Manual de Atualização (`CREATE`, `UPDATE`, `UPSERT`), Conflitos e Idempotência

### 5.1 Semântica exata das ações (`StructuredOperationAction`)

| Ação | Quando o registro (`id` ou chave natural) **NÃO existe** | Quando o registro **JÁ existe** no aparelho |
| --- | --- | --- |
| **`CREATE`** *(padrão)* | Cria o registro (`APPLIED` / prévia `WILL_CREATE`). | **Preserva os dados locais intactos** e grava `CONFLICT_PRESERVED_LOCAL` em `audit_history` (prévia `CONFLICT_PRESERVED_LOCAL`). Nunca sobrescreve dados locais. |
| **`UPDATE`** | **Rejeita** a operação (`REJECTED`, ex.: `"Tarefa 'X' não encontrada localmente para atualização."`). | Atualiza o registro (`UPDATED` / prévia `WILL_UPDATE`), preservando `createdAt`. |
| **`UPSERT`** | Cria o registro (`APPLIED` / prévia `WILL_CREATE`). | Atualiza o registro (`UPDATED` / prévia `WILL_UPDATE`), preservando `createdAt`. |

### 5.2 REGRA CRÍTICA: substituição completa em `UPDATE`/`UPSERT` vs. mesclagem em `CHECKLIST`

> **ATENÇÃO — Leia isto antes de gerar qualquer `UPDATE` ou `UPSERT`:**  
> Para `TASK`, `APPOINTMENT`, `EVENT`, `PROJECT`, `PERSONAL_GOAL` e `TRANSACTION`, o `StructuredInputProcessor` **não faz patch parcial (PATCH)**. Ele reconstrói a entidade Room inteira a partir dos campos presentes no `data` do JSON e preserva **apenas `createdAt`** (e `completedAt` / `settledAt` quando o status terminal permanecer e o campo vier nulo).  
> **Consequência prática:** se uma tarefa já tiver `description`, `projectId`, `categoryId`, `dueAt` ou `notes` salvos no aparelho e você enviar `UPDATE`/`UPSERT` omitindo esses campos, eles serão **sobrescritos pelos valores padrão do DTO (`""`, `null`, `NORMAL`, `NOT_STARTED`, `false`, `0L`)**!  
> **Exceção:** `CHECKLIST` (e `TASK.subtasks`) faz **mesclagem aditiva/atualização** na tabela `subtasks` por `id` ou `title` case-insensitive, **sem apagar** subtarefas existentes que não foram listadas.

**Como agir na prática ao atualizar:**
- Se você quiser apenas **adicionar ou marcar itens de checklist** em uma tarefa conhecida → **use `CHECKLIST`** (com `action: "UPSERT"` ou `"UPDATE"`), pois ele não toca nos outros campos nem apaga subtarefas antigas da tarefa!
- Se precisar alterar título, prazo ou status de uma entidade conhecida (ex.: criada por você na conversa) → **re-envie todos os campos relevantes** no `data` junto com o `id` conhecido.
- Se não tiver certeza do `id` ou dos campos atuais salvos no celular (pois `v1` não tem snapshot de leitura automática — `GAP-006`) → pergunte ao usuário ou avise explicitamente antes de emitir `UPDATE`/`UPSERT`.

### 5.3 Exemplos JSON de Atualização, Checklist, Conflito e Idempotência

#### Exemplo 7 — Adicionar novos itens e concluir um item existente no checklist (`CHECKLIST` / `UPSERT`)
*Cenário:* Na tarefa `"task-viagem-conf-eng"` criada no Exemplo 6, o usuário já reservou o hotel e quer adicionar *"Fazer check-in online"* ao checklist sem mexer nos demais campos da tarefa.

```json
{
  "contractVersion": 1,
  "source": "chatgpt-nexus-inbox",
  "createdAt": 1795870800000,
  "operations": [
    {
      "operationId": "op-2026-11-28-chk-conf-update",
      "entityType": "CHECKLIST",
      "action": "UPSERT",
      "contractVersion": 1,
      "createdAt": 1795870800000,
      "data": {
        "taskId": "task-viagem-conf-eng",
        "items": [
          { "id": "sub-conf-1", "title": "Reservar hotel", "isCompleted": true, "sortOrder": 0 },
          { "id": "sub-conf-3", "title": "Fazer check-in online", "isCompleted": false, "sortOrder": 2 }
        ]
      }
    }
  ]
}
```
- **Resultado esperado:** `"Reservar hotel"` passa a `isCompleted = true`, `"Separar crachá"` permanece intacto, `"Fazer check-in online"` é adicionado; na prévia mostra `WILL_UPDATE` e após importar grava `UPDATED`.

#### Exemplo 8 — Atualização completa de tarefa existente (`TASK` / `UPDATE`)
*Cenário:* Alterar a tarefa `"task-relatorio-trimestral-2026"` (Exemplo 1) para `"IN_PROGRESS"`, mantendo todos os campos que devem ser preservados.

```json
{
  "contractVersion": 1,
  "source": "chatgpt-nexus-inbox",
  "createdAt": 1795870800000,
  "operations": [
    {
      "operationId": "op-2026-11-28-task-relatorio-upd",
      "entityType": "TASK",
      "action": "UPDATE",
      "contractVersion": 1,
      "createdAt": 1795870800000,
      "data": {
        "id": "task-relatorio-trimestral-2026",
        "title": "Entregar relatório trimestral",
        "description": "Consolidar indicadores e enviar versão final para revisão",
        "categoryId": "cat-default-trabalho",
        "priority": "URGENT",
        "status": "IN_PROGRESS",
        "dueAt": 1796148000000,
        "hasDueTime": true,
        "notes": "Prazo às 15:00 (01/12/2026) — revisão final em andamento"
      }
    }
  ]
}
```

#### Exemplo 9 — Proteção contra colisão local (`CREATE`) e reenvio idempotente (`DUPLICATE_IGNORED`)
- **Conflito local:** se você enviar um **novo `operationId`** com `action: "CREATE"` apontando para um `id` já existente no banco (ex.: `"id": "task-relatorio-trimestral-2026"`), o app **não sobrescreve** o dado do usuário; o resultado é `CONFLICT_PRESERVED_LOCAL`.
- **Idempotência:** se um arquivo contendo o mesmo `operationId` (`"op-2026-11-28-task-relatorio-upd"`) for processado novamente no futuro (porque você reescreveu ou enviou outro lote com o mesmo ID de operação), o processador consulta `audit_history` (`STRUCTURED_INPUT_OP`) e retorna **`DUPLICATE_IGNORED`**, sem aplicar escrita duplicada.

#### Exemplo 10 — Compromisso único com checklist próprio + Série recorrente com `templateChecklist` (`RECURRENCE`)
*Pedido do usuário:* *"Registra no NEXUS: consulta no dentista dia 01/12/2026 das 14:00 às 15:00 com checklist (levar carteira do plano, levar radiografia) e a minha corrida recorrente 'A Vida é um Corre' toda quinta-feira às 20:00 (1h30) a partir de 03/12/2026 no Parque Ibirapuera com checklist-modelo: tênis de corrida, garrafa de água, relógio GPS."*

```json
{
  "contractVersion": 1,
  "source": "chatgpt-nexus-inbox",
  "createdAt": 1795784400000,
  "operations": [
    {
      "operationId": "op-2026-11-27-appt-dentista-chk",
      "entityType": "APPOINTMENT",
      "action": "CREATE",
      "contractVersion": 1,
      "createdAt": 1795784400000,
      "data": {
        "id": "appt-dentista-2026-12-01",
        "title": "Consulta no dentista",
        "categoryId": "cat-default-saude",
        "startAt": 1796144400000,
        "endAt": 1796148000000,
        "isAllDay": false,
        "location": "Consultório Odonto Centro",
        "checklist": [
          { "id": "chk-dent-1", "title": "Levar carteira do plano", "isCompleted": false, "sortOrder": 0 },
          { "id": "chk-dent-2", "title": "Levar radiografia", "isCompleted": false, "sortOrder": 1 }
        ]
      }
    },
    {
      "operationId": "op-2026-11-27-rec-corrida-01",
      "entityType": "RECURRENCE",
      "action": "CREATE",
      "contractVersion": 1,
      "createdAt": 1795784400000,
      "data": {
        "id": "rec-a-vida-e-um-corre",
        "domain": "APPOINTMENT",
        "title": "A Vida é um Corre",
        "description": "Treino semanal em grupo",
        "categoryId": "cat-default-saude",
        "frequency": "SPECIFIC_WEEKDAYS",
        "weekdays": ["THURSDAY"],
        "startDateIso": "2026-12-03",
        "startTime": "20:00",
        "isAllDay": false,
        "durationMillis": 5400000,
        "reminderMinutesBefore": 30,
        "location": "Parque Ibirapuera — Portão 3",
        "templateChecklist": [
          "Tênis de corrida",
          "Garrafa de água",
          "Relógio GPS"
        ],
        "isActive": true
      }
    }
  ]
}
```
- **Resultado esperado:** cria o compromisso com seus 2 itens em `schedule_checklist_items` e cria a série recorrente `rec-a-vida-e-um-corre` (`checklistTemplateVersion = 1`). Cada quinta-feira projetada/materializada nasce com os 3 itens **independentes e desmarcados**.

#### Exemplo 11 — Atualização não retroativa de checklist-modelo em série recorrente (`RECURRENCE` / `UPSERT`)
*Cenário:* A partir de `2026-12-17`, o usuário quer incluir *"Gel de carboidrato"* no checklist da corrida `"rec-a-vida-e-um-corre"` sem alterar as corridas anteriores de `03/12` e `10/12`.

```json
{
  "contractVersion": 1,
  "source": "chatgpt-nexus-inbox",
  "createdAt": 1797000000000,
  "operations": [
    {
      "operationId": "op-2026-12-11-rec-corrida-v2",
      "entityType": "RECURRENCE",
      "action": "UPSERT",
      "contractVersion": 1,
      "createdAt": 1797000000000,
      "data": {
        "id": "rec-a-vida-e-um-corre",
        "domain": "APPOINTMENT",
        "title": "A Vida é um Corre",
        "description": "Treino semanal em grupo",
        "categoryId": "cat-default-saude",
        "frequency": "SPECIFIC_WEEKDAYS",
        "weekdays": ["THURSDAY"],
        "startDateIso": "2026-12-03",
        "startTime": "20:00",
        "isAllDay": false,
        "durationMillis": 5400000,
        "reminderMinutesBefore": 30,
        "location": "Parque Ibirapuera — Portão 3",
        "templateChecklist": [
          "Tênis de corrida",
          "Garrafa de água",
          "Relógio GPS",
          "Gel de carboidrato"
        ],
        "effectiveFromDateIso": "2026-12-17",
        "isActive": true
      }
    }
  ]
}
```
- **Resultado esperado:** incrementa `checklistTemplateVersion` para `2`, adiciona `ChecklistTemplateSnapshot(version = 2, effectiveFromDateIso = "2026-12-17", ...)` em `templateHistoryJson`, preserva intactas as ocorrências de `03/12` e `10/12` e aplica o 4º item somente de `17/12/2026` em diante.

---

## 6. Interpretação de Linguagem Natural, Regras Obrigatórias e Limitações (`GAP-005`, `GAP-006`)

### 6.1 As 7 regras obrigatórias de comportamento do ChatGPT

1. **Verifique compatibilidade com `v1` antes de emitir JSON:** nunca invente campos, enums ou entidades fora da Seção 3.
2. **Use `RECURRENCE` nativa — nunca simule recorrência com registros únicos avulsos:** se o usuário pedir uma série recorrente (ex.: *"toda quinta às 20h"*, *"academia segunda, quarta e sexta com checklist"*), emita uma única operação **`RECURRENCE`** (`domain`: `"TASK"`, `"APPOINTMENT"` ou `"EVENT"`) com `templateChecklist`, jamais 4, 12 ou 52 objetos soltos.
3. **Use `checklist` nativo em `APPOINTMENT` e `EVENT`:** quando o usuário pedir itens de preparação próprios de um compromisso ou evento único, envie-os no campo `checklist` do próprio `APPOINTMENT`/`EVENT` (ou `templateChecklist` quando for uma série `RECURRENCE`), sem criar tarefas falsas.
4. **Nunca invente identificadores locais ou de ícones (`GAP-005`):** sem `creditCardId`, `statementId`, `recurrenceId` ou `categoryId` real, não chute hashes. Só use as categorias `cat-default-*` / `cat-fin-*` documentadas ou IDs fornecidos pelo usuário/criados no mesmo lote. Nunca emita `"icon"` ou `"context"`.
5. **Não confunda obrigação financeira com tarefa:** conta, boleto, fatura, parcela e compra são `TRANSACTION`, nunca `TASK`.
6. **Não duplique despesas de cartão de crédito:** nunca emita despesa avulsa de *"pagamento de fatura"* ligada a cartão/fatura existente.
7. **Pergunte antes quando faltar dado essencial:**
   - Compromisso ou Evento sem data ou sem intervalo mínimo (`startAt`/`endAt`) ou `RECURRENCE` sem `startDateIso`/horário (quando não é dia inteiro) → pergunte antes de gerar JSON.
   - Compra parcelada ou compra em cartão específico sem `creditCardId`/`statementId` conhecidos → alerte que a Inbox `v1` não aloca automaticamente em fatura nem gera `InstallmentPurchaseEntity`.
   - Pedido de atualização (`UPDATE`) quando não se conhece o `id` local nem os campos atuais → alerte sobre a substituição completa dos campos omitidos (§5.2) e peça confirmação antes.

### 6.2 Como lidar com pedidos de recorrência e exceções pontuais

- **Criar ou atualizar regra/série recorrente (incluindo checklist-modelo):** emita `entityType: "RECURRENCE"` com `domain`, `frequency`, `weekdays` (quando `SPECIFIC_WEEKDAYS`), `startDateIso`, `startTime` (ou `isAllDay: true`) e `templateChecklist`. Ao evoluir o `templateChecklist` de uma série existente, informe `effectiveFromDateIso` para garantir atualização **não retroativa**.
- **Pular ou mover apenas 1 ocorrência isolada (`SKIP` / `OVERRIDE`):** o contrato `v1` ainda não tem operação dedicada para `RecurrenceExceptionEntity`; oriente o usuário a tocar na ocorrência na Agenda do app e escolher `"Só esta ocorrência"`.

### 6.3 Como responder a pedidos de ícones (`GAP-005`) ou leitura do banco (`GAP-006`)

- **Ícones/identidade contextual (`GAP-005`):** explique que ainda não existe campo de ícone/contexto no modelo nem no contrato `v1`, e nunca adicione campo `"icon"` ou `"context"` no JSON (pois `ignoreUnknownKeys = false` causaria rejeição).
- **Leitura automática do estado atual (`GAP-006`):** explique que a ponte `NEXUS-INBOX` atual é unidirecional (entrada) e confirme com o usuário os campos/IDs antes de emitir `UPDATE`/`UPSERT` em registros criados fora da conversa.

---

## 7. `NEXUS-INBOX`: Envio, Prévia, Confirmação e Segurança

### 7.1 Configuração e formato aceito no repositório privado `NEXUS-INBOX`

Em **Mais → NEXUS-INBOX (Ponte ChatGPT)** (`SubRoute.InboxBridge`), o usuário configura:
- `owner` (usuário/organização GitHub), `repo` (padrão `NEXUS-INBOX`), `branch` (padrão `main`), `pathPrefix` (opcional, pasta dentro do repo, ex.: `lotes` ou vazio para raiz).
- **Token:** Fine-Grained Personal Access Token (PAT) **somente leitura** (`Contents: Read-only`) restrito exclusivamente ao repositório `NEXUS-INBOX`, armazenado criptografado no **Android Keystore (`AES/GCM/NoPadding`)**.

**Regras reais de leitura em `GitHubInboxRemoteDataSource` e `InboxRepository`:**
1. **Listagem não recursiva:** lê apenas arquivos `.json` diretamente dentro de `pathPrefix` (ou da raiz do repositório se `pathPrefix` estiver vazio). Subpastas não são percorridas automaticamente!
2. **Tamanho:** cada arquivo `.json` deve ter entre **`1 byte` e `2 MB` (`2.097.152 bytes`)**.
3. **Ordenação determinística:** os arquivos novos/alterados são ordenados **lexicograficamente por `path`** antes de unir suas operações (`mergeDownloadedEnvelopes`).
   - **Convenção recomendada de nome de arquivo:** `YYYY-MM-DD_NN_assunto.json` (ex.: `2026-12-01_01_viagem-sp.json`), garantindo ordem cronológica/topológica previsível mesmo quando houver múltiplos arquivos pendentes.
4. **Atenção na combinação de múltiplos arquivos (`mergeDownloadedEnvelopes`):** cada arquivo deve usar `contractVersion: 1`, `source` preenchido e `createdAt > 0`. Arquivos diferentes são mesclados em ordem alfabética de `path`; mantenha todas as dependências pais antes dos filhos.
5. **Deduplicação de arquivo vs. idempotência de operação:**
   - Cada arquivo processado com sucesso total tem seu par `versionKey = "$path::$sha"` salvo localmente (até os últimos `250` registros). Se você editar o arquivo no GitHub (novo `sha`), o app lê o arquivo novamente, mas as operações cujo `operationId` já foi aplicado retornam `DUPLICATE_IGNORED`.
   - **Regra de ouro:** se **qualquer** operação de um arquivo resultar em `REJECTED` ou `FAILED_ROLLED_BACK`, esse arquivo **NÃO** é marcado como concluído por `path::sha`, permitindo corrigir e reprocessar depois.

### 7.2 Fluxo de prévia, confirmação, descarte e background

| Etapa | Comportamento real no NEXUS | Observação operacional |
| --- | --- | --- |
| **1. Consulta (`checkInboxNow`)** | Baixa apenas arquivos ainda não marcados por `path::sha`, valida JSON estrito (`ignoreUnknownKeys = false`) e executa `previewInboxEnvelope` com `PreviewOverlayGateway`. **Zero gravações no Room.** | Classifica cada item como `WILL_CREATE`, `WILL_UPDATE`, `DUPLICATE_IGNORED`, `CONFLICT_PRESERVED_LOCAL` ou `REJECTED`. |
| **2. Decisão do usuário** | Na UI (`NexusInboxScreen`), o usuário pode **"Confirmar importação"** ou **"Descartar"**. | Descartar limpa a prévia em memória sem escrever no Room e sem marcar o `path::sha` como importado. |
| **3. Importação (`confirmAndImportPending`)** | Chama `applyInboxEnvelope` com **`atomicBatch = false`** (cada operação roda em sua própria transação `withTransaction` — operações válidas aplicam e operações inválidas retornam `REJECTED` sem travar as irmãs). | Grava recibos `STRUCTURED_INPUT_OP` e resumo `INBOX_IMPORT_LOG` em `audit_history` (omitindo categorias com zero alterações), recalcula lembretes (`rescheduleAllRemindersInternal`, inclusive janela de 14 dias de séries recorrentes já existentes) e atualiza os widgets Glance. |
| **4. Sync em segundo plano (`InboxSyncWorker`)** | Opcional (`WorkManager >= 15 min`). Apenas verifica e dispara notificação com deep-link se houver novidades acionáveis. | **Nunca grava dados no banco sem o usuário tocar em "Confirmar importação".** |

### 7.3 Segurança e fronteira operacional

- **Nunca** inclua tokens, senhas, chaves privadas, números completos de cartão/documentos ou dados pessoais desnecessários dentro dos arquivos `.json` da `NEXUS-INBOX`.
- **Fronteira de ferramentas (AI Studio × Usuário × GitHub):** o Google AI Studio edita apenas os arquivos do workspace local (sem acesso a GitHub, `git push` ou Actions). A sincronização entre workspace, GitHub (`PROJETO-NEXUS-` / `NEXUS-INBOX`) e ChatGPT é responsabilidade do usuário.

---

## 8. Erros Comuns, Validações do Processador e Resoluções (Baseado nos Testes Reais)

Tabela extraída diretamente de `StructuredInputProcessor.kt`, `StructuredInputUnitTests.kt` e `InboxBridgeUnitTests.kt`:

| Erro / Sintoma real | Causa no processador | Como prevenir / corrigir |
| --- | --- | --- |
| `JSON estruturado inválido ou incompatível com o contrato` (arquivo inteiro rejeitado antes da prévia) | Chave desconhecida (`ignoreUnknownKeys = false`), ex.: `isDone`, `orderIndex`, `location`, `status` em `EVENT`, `ticketUnitPriceCents`, ou tipo errado/comentário no JSON. | Conferir rigorosamente os nomes exatos dos DTOs na Seção 3: `isCompleted`, `sortOrder`, `locationName`, `eventStatus`, `ticketPriceCents`, `name` em `EVENT`/`PROJECT`. |
| `envelopeError: "Versão de contrato incompatível"` ou divergência envelope/operação | `contractVersion != 1` no envelope ou em alguma operação. | Sempre usar `"contractVersion": 1` no envelope e em todas as operações. |
| `Campo obrigatório 'source' ausente` | `source` vazio no envelope e vazio em alguma operação. | Sempre preencher `"source": "chatgpt-nexus-inbox"` no envelope raiz. |
| `REJECTED: Campo obrigatório 'operationId' ausente` | `operationId` vazio ou omitido. | Gerar ID único não vazio para toda operação. |
| `REJECTED: ... vinculada '...' não existe localmente` | FK (`categoryId`, `projectId`, `eventId`, `taskId`, `goalId`, `creditCardId`, `statementId`, etc.) não existe no banco nem foi criada antes no mesmo lote. | Usar apenas IDs garantidos (`cat-default-*` / `cat-fin-*`), IDs informados pelo usuário ou IDs criados em operação **anterior** do mesmo lote. |
| `REJECTED: Intervalo de datas inválido...` | `startAt <= 0` ou `endAt < startAt` em `APPOINTMENT`/`EVENT`, ou `occurredAt <= 0` / `amountCents <= 0` em `TRANSACTION`. | Verificar conversão de datas para epoch ms `> 0`, `endAt >= startAt`, `amountCents > 0`. |
| `REJECTED: Lançamento não quitado bloqueado em fatura já paga` | Tentativa de inserir `TRANSACTION` com `statementId` apontando para fatura `PAID` usando `status != "SETTLED"`. | Se a fatura já está `PAID`, usar `"status": "SETTLED"` (ou associar à fatura correta aberta). |
| `REJECTED: Despesa avulsa de pagamento de fatura bloqueada...` | `TRANSACTION` `EXPENSE` sem `statementId`/`installmentPurchaseId` com texto de pagamento de fatura (`"pagamento de fatura"`, `"quitação de fatura"`, etc.) quando há cartão/fatura paga. | Não gerar transação avulsa de pagamento de fatura (`ADR-006`). |
| `CONFLICT_PRESERVED_LOCAL` | `action: "CREATE"` com `id` (ou chave natural como `(recurrenceId, occurrenceDateIso)` / `(installmentPurchaseId, installmentNumber)`) já existente no aparelho. | Se a intenção for realmente atualizar um registro existente, usar `UPDATE`/`UPSERT` (enviando o objeto completo ou usando `CHECKLIST`). |
| `DUPLICATE_IGNORED` | Reuso de um `operationId` já registrado em `audit_history` como `APPLIED`, `UPDATED` ou `CONFLICT_PRESERVED_LOCAL`. | Sempre gerar um novo `operationId` para uma nova alteração; só repita `operationId` se quiser retentativa idempotente do mesmo comando. |
| Campos de uma tarefa sumiram após `UPDATE` / `UPSERT` | `UPDATE`/`UPSERT` em entidades principais substitui o registro inteiro a partir do JSON, zerando campos omitidos. | Reenviar todos os campos conhecidos da entidade ou usar `CHECKLIST` quando o alvo for apenas subtarefas. |
| `CHECKLIST` aparece como `WILL_UPDATE` / `UPDATED` mesmo adicionando novos itens | Comportamento explícito de `prepareAndApplyChecklist` (`isUpdate = true`), pois altera uma `TASK` existente. | Comportamento normal e esperado no contrato `v1`. |
| Arquivo no GitHub ignorado ou não listado na Inbox | Arquivo dentro de subpasta diferente de `pathPrefix`, extensão diferente de `.json`, tamanho `> 2 MB` ou mesmo `path::sha` já importado anteriormente. | Salvar `.json` diretamente dentro da pasta configurada, `<= 2 MB`, com nome ordenado e novo commit. |

| `REJECTED: Domínio da série '...' (...) não pode ser alterado...` | Tentativa de `UPDATE`/`UPSERT` em `RECURRENCE` mudando `domain` (ex.: `TASK` → `APPOINTMENT`). | Manter o `domain` original da série ou criar uma nova série com novo `id`. |
| `REJECTED: Para SPECIFIC_WEEKDAYS informe 'weekdays' ou 'weekdaysMask' válido` | `RECURRENCE` com `frequency: "SPECIFIC_WEEKDAYS"` sem dias válidos. | Informar `"weekdays": ["MONDAY", "WEDNESDAY"]` (ou equivalente `SEG`/`QUA`). |
| `REJECTED: Informe 'startTime' (HH:mm) ou 'dueTimeMinutesOfDay' para série com horário` | `RECURRENCE` (`APPOINTMENT`/`EVENT` com `isAllDay: false` ou `TASK` com `hasDueTime: true`) sem horário. | Preencher `"startTime": "20:00"` ou `"isAllDay": true`. |

---

## 9. Compatibilidade, Versionamento e Checklist Obrigatório de Manutenção

### 9.1 Histórico de compatibilidade deste guia

| Versão do guia | App mínimo compatível | Room / Backup | Contrato Inbox | Observação |
| --- | --- | --- | --- | --- |
| `0.1` (2026-11-26) | — | — | — | Estrutura inicial não operacional (substituída por `1.0.0`). |
| `1.0.0` (2026-11-27) | `5.3.0` (`versionCode = 8`) | `Room v7` / `Backup v8` | `contractVersion = 1` (`7` tipos) | Primeira versão operacional completa sem `RECURRENCE` nem `checklist` em `APPOINTMENT`/`EVENT`. |
| **`1.1.0` (2026-11-27)** | **`5.3.0` (`versionCode = 8`)** | **`Room v8` / `Backup v9`** | **`contractVersion = 1` (`8` tipos)** | Adiciona `RECURRENCE` (`TASK`/`APPOINTMENT`/`EVENT`, `templateChecklist`, `effectiveFromDateIso` não retroativo — `GAP-002`) e `checklist` opcional em `APPOINTMENT`/`EVENT` (`GAP-004`/`REQ-029`), preservando 100% de retrocompatibilidade do contrato `v1`. Aguardando execução CI/dispositivo. |

**Se a versão instalada do app ou do contrato divergir:**
- Se o `contractVersion` futuro subir (ex.: `v2`), não envie campos da versão nova para um app configurado para `v1` sem antes confirmar que o usuário atualizou o APK e este guia foi atualizado.
- Caso um arquivo retorne `envelopeError` por versão incompatível, ajuste o `contractVersion` para a versão suportada ou peça atualização do aplicativo.

### 9.2 Regra Permanente de Manutenção deste Guia (Obrigatória para Futuras Implementações)

> **Regra normativa vinculada a `docs/00_ORIENTACAO_E_AUTORIDADE.md`, `docs/09_GUIA_DESENVOLVIMENTO.md` e `docs/13_GOVERNANCA_DOCUMENTAL.md`:**  
> Toda implementação futura no NEXUS que **crie, altere, remova ou mude o comportamento** de funcionalidades, entidades, campos, relações, regras de negócio, fluxos de entrada/saída ou contratos acessíveis ao ChatGPT **DEVE atualizar `docs/16_GUIA_OPERACIONAL_CHATGPT_NEXUS.md` no mesmo lote/sessão**, antes de considerar a entrega concluída.

#### Critério de quando atualizar vs. quando não precisa atualizar
- **ATUALIZAR OBRIGATORIAMENTE quando:**
  - houver novo `entityType`, novo DTO, campo adicionado/renomeado/removido, alteração de obrigatoriedade, tipo ou valor padrão;
  - houver alteração em enums usados no contrato ou nas regras de negócio (`TaskStatus`, `EventLifecycleStatus`, `TicketStatus`, `FinancialStatus`, `PaymentMethod`, etc.);
  - `GAP-002`, `GAP-004`, `GAP-005` ou `GAP-006` passarem de planejados para implementados no código/contrato;
  - mudarem regras financeiras (ex.: cartões, faturas, eventos, duplicidade), regras de conflito, idempotência, `UPDATE`/`UPSERT`, fuso horário, permissões da Inbox ou estrutura de arquivos no repositório `NEXUS-INBOX`;
  - novas funcionalidades do app passarem a impor limitações relevantes que o ChatGPT precise explicar ao interpretar pedidos em linguagem natural.
- **NÃO exige atualização funcional deste guia (nem deve antecipar recursos futuros):**
  - funcionalidades apenas planejadas/em análise que não entraram no código;
  - ajustes puramente visuais (cores, espaçamentos, layout Compose) ou refatorações internas sem mudança em entidades, campos, relações, validações ou contrato.

#### Checklist obrigatório em 7 passos por lote impactante
1. **Conferir o código real alterado** (DTOs em `StructuredInputModels.kt`, processador em `StructuredInputProcessor.kt`, `NexusEnums.kt`, `NexusEntities.kt`, `data/inbox/*` e repositórios).
2. **Atualizar matriz de capacidades e versões** (Seção 1.3 e Seção 9.1 deste guia).
3. **Atualizar entidades, campos, enums e regras** na Seção 3 e nas árvores/regras de decisão (Seções 2 e 6).
4. **Atualizar ou adicionar exemplos JSON completos** correspondentes às novas capacidades/ações e remover exemplos que tenham se tornado incompatíveis.
5. **Validar rigorosamente todos os exemplos JSON** contra os mesmos DTOs/validadores/testes reais do aplicativo (`:app:testDebugUnitTest` no CI/Gradle ou inspeção estática declarando explicitamente o limite quando o container não tiver toolchain).
6. **Registrar a atualização no histórico documental** (`docs/HISTORICO_DOCUMENTACAO.md`) e manter coerência com `docs/07_ESTADO_ATUAL.md`, `docs/10_ROADMAP.md`, `docs/14_CONTRATO_ENTRADA_ESTRUTURADA_ETAPA3.md` e `docs/15_REGISTRO_LACUNAS_E_FALHAS.md`.
7. **Sincronizar referências acessíveis** nos artefatos internos do Google AI Studio (e no espelho `NEXUS-DOCUMENTACAO/GOOGLE_IA/docs/` quando acessível no ambiente; quando não estiver montado, registrar honestamente a pendência de sincronização externa).
