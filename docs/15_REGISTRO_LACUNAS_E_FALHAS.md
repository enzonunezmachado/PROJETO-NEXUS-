# 15 — Registro Consolidado de Lacunas, Defeitos e Validações Pendentes

> **Finalidade:** manter em um único lugar rastreável as funcionalidades incompletas, inconsistências documentais, defeitos observados durante implementação e validações ainda não concluídas. Este registro complementa — não substitui — requisitos, estado atual, roadmap, ADRs e código.
>
> **Regra de rigor:** “implementado” não significa “validado”; falha corrigida em uma execução não deve ser tratada como pendência aberta, mas precisa manter seu histórico. Este é o inventário inicial consolidado em 26/11/2026, não uma declaração de que todos os arquivos e fluxos do aplicativo foram novamente auditados nesta atualização.

## 1. Prioridade alta — recorrência de compromissos e eventos

| Campo | Registro |
|---|---|
| ID | `GAP-001` |
| Requisito relacionado | `REQ-009` |
| Estado | **NÃO IMPLEMENTADA — pendência funcional conhecida** |
| Origem | `docs/02_ESPECIFICACAO_PRODUTO.md`, `docs/07_ESTADO_ATUAL.md`, `docs/10_ROADMAP.md`, `docs/12_RASTREABILIDADE.md`, `ADR-008` |
| Escopo afetado | Compromissos, Eventos, Agenda, lembretes/notificações, persistência, backup e entrada estruturada |
| Evidência prática | No povoamento de 26/11/2026, a corrida semanal “A Vida é um Corre”, quinta-feira às 20h, não pôde ser registrada como compromisso recorrente pela ponte. Foi preservada apenas como informação dentro da checklist da rotina semanal; isso não cria ocorrências reais na Agenda nem lembretes recorrentes. |

### Situação confirmada
- A recorrência de **Tarefas** existe no código: regras, exceções, projeção virtual na Agenda e materialização idempotente ao concluir.
- A recorrência de **Compromissos e Eventos** está explicitamente marcada como não implementada em `REQ-009`.
- A `ADR-008` registra que a expansão para esses domínios foi adiada para lote futuro.
- O contrato da ponte `v1` não oferece entidade `RECURRENCE` nem campos de regra recorrente para `APPOINTMENT`/`EVENT`. O DTO de tarefa pode referenciar `recurrenceId`, mas isso não equivale a permitir criar uma regra nova por meio da ponte.
- Portanto, atualmente não se deve representar uma série recorrente como vários registros únicos inventados, nem dizer que uma tarefa de checklist equivale a um compromisso recorrente.

### Trabalho necessário antes de implementar
1. Levantar o modelo real atual de `TaskRecurrenceEntity`, `RecurrenceExceptionEntity`, `RecurrenceEngine`, `AgendaEngine`, lembretes e regras de notificação.
2. Definir se a regra será generalizada por tipo de entidade ou se compromissos/eventos terão vínculos/tabelas específicas; avaliar migração Room e compatibilidade de backup.
3. Definir frequências, intervalos, dias da semana, âncora mensal, início/fim, limite de ocorrências, fuso horário e comportamento em horário de verão.
4. Definir exceções por ocorrência: pular/cancelar, mover data/hora, sobrescrever dados, editar/excluir “só esta” ou “toda a série”.
5. Definir semântica específica de Evento (duração de vários dias, custos/ingressos, tarefas e compromissos vinculados) versus Compromisso (início/fim, conflito de horários e lembretes).
6. Garantir projeção virtual na janela solicitada, sem pré-gerar centenas de linhas; identidade estável, idempotência, histórico e ausência de duplicação.
7. Definir como lembretes e notificações serão agendados/cancelados para ocorrências projetadas e exceções.
8. Planejar backup/restauração/mesclagem e entrada estruturada para regras e exceções, com versionamento compatível.
9. Criar testes unitários de regra, exceções, limites de janela, mudança de fuso/DST, idempotência, conflitos, lembretes e regressão da recorrência de tarefas.
10. Validar em aparelho: criar série, visualizar na Agenda, editar uma ocorrência, editar a série, cancelar uma ocorrência, reiniciar o app e verificar lembretes sem duplicação.

**Critério de conclusão:** um compromisso semanal real pode ser criado uma única vez como série, aparece nas datas corretas da Agenda, mantém exceções e lembretes corretos e sobrevive a backup/restauração e reabertura do app. O mesmo deve ser demonstrado para Eventos conforme regras específicas aprovadas.

**Dependência da ponte:** decidir explicitamente como estender o contrato de entrada estruturada. Não alterar silenciosamente o contrato `v1` estrito nem inventar uma entidade/campo antes de aprovar o modelo.

## 2. Inconsistência documental encontrada na ponte

| Campo | Registro |
|---|---|
| ID | `DOC-001` |
| Estado | **CORRIGIDA NESTA ATUALIZAÇÃO DOCUMENTAL** |
| Arquivo | `docs/14_CONTRATO_ENTRADA_ESTRUTURADA_ETAPA3.md` |
| Problema | O exemplo JSON de `TASK.subtasks` e `CHECKLIST.items` usava `isDone` e `orderIndex`, enquanto os campos reais do contrato/código são `isCompleted` e `sortOrder`; na revisão seguinte também foi detectada divergência nos campos de `EVENT` no mesmo exemplo (`location`, `status`, `ticketUnitPriceCents` em vez de `locationName`, `eventStatus`, `ticketPriceCents`). |
| Risco | O ChatGPT ou outra fonte poderia copiar o exemplo e produzir JSON rejeitado pelo parser estrito (`ignoreUnknownKeys = false`). |
| Correção | Exemplo oficial atualizado para os nomes reais dos DTOs (`isCompleted`, `sortOrder`, `locationName`, `eventStatus`, `ticketPriceCents`) e adicionada cobertura de fixture em `InboxBridgeUnitTests` (`official contract template parses strictly and discard leaves DB untouched`). |
| Validação restante | Executar `:app:testDebugUnitTest` no GitHub Actions após sincronizar manualmente com a `main`. |

## 3. Limitação de recorrência na entrada estruturada

| Campo | Registro |
|---|---|
| ID | `GAP-002` |
| Estado | **ABERTA — limitação conhecida do contrato `v1`** |
| Arquivos | `StructuredInputModels.kt`, `StructuredInputProcessor.kt`, `docs/14_CONTRATO_ENTRADA_ESTRUTURADA_ETAPA3.md` |
| Problema | O contrato aceita `APPOINTMENT` e `EVENT` como registros únicos, mas não define série/regra/exceções recorrentes. Também não oferece uma operação para criar a regra de recorrência de uma nova tarefa. |
| Consequência | A Secretária ChatGPT não consegue registrar fielmente compromissos/eventos recorrentes nem criar uma nova série de tarefas via Inbox; referenciar `recurrenceId` não cria a regra. |
| Próximo passo | Resolver junto de `GAP-001`, definindo versão/extensão do contrato, DTOs, validação, preview, idempotência, conflitos e importação transacional. Não simular recorrência com ocorrências avulsas. |

## 4. Requisito transversal: checklist independente por ocorrência recorrente

| Campo | Registro |
|---|---|
| ID | `GAP-003` |
| Estado | **EM ANÁLISE — auditoria inicial identificou implementação parcial; cobertura ponta a ponta ainda não verificada** |
| Escopo | Recorrência de tarefas, checklist/subtasks, histórico, Agenda, backup/restore/merge e Inbox |
| Origem | Necessidade de produto explicitada em 26/11/2026 |
| Dependências | Auditoria de `TaskRecurrenceEntity`, `RecurrenceEngine`, materialização de ocorrências e modelo de subtarefas |

### Necessidade

Uma tarefa recorrente pode reutilizar a mesma estrutura de checklist em cada ocorrência, mas cada ciclo precisa manter seu próprio estado de execução. Marcar os itens da semana anterior não pode fazer com que a ocorrência da semana seguinte já apareça concluída.

### Exemplo de aceite

Uma tarefa semanal de segunda-feira possui três itens de checklist. Na primeira semana, os três são concluídos. Na segunda semana, os mesmos três itens aparecem novamente pendentes. A ocorrência anterior permanece concluída no histórico e não é alterada. Cada ocorrência mantém identidade e estado independentes.

### Evidências da primeira auditoria de código (26/11/2026)

- `SubtaskEntity` contém apenas `taskId`, título, estado de conclusão e ordem. Não existe entidade própria de checklist-base ligada diretamente à série; os itens pertencem a uma tarefa específica.
- `NexusRepository.materializeNextOccurrenceIfNeededInternal()` clona os itens da ocorrência concluída para a próxima ocorrência materializada e define `isCompleted = false`. Isso já implementa parte da independência do estado entre ocorrências nesse caminho.
- Entretanto, `NexusRepository.openOrMaterializeRecurringOccurrence()`, chamado quando uma ocorrência virtual futura é aberta pela Agenda, cria a nova `TaskEntity` sem clonar as subtarefas da ocorrência/modelo. Portanto, o comportamento pode variar conforme o caminho de materialização.
- Como o checklist-base não está separado do checklist de uma ocorrência, ainda não há evidência de uma fonte de verdade estável para alterações do modelo da série, ocorrências isoladas e histórico.
- A suíte `NexusLote6RecurrenceUnitTests` cobre geração de datas, projeção virtual, exceções e materialização, mas a inspeção inicial não encontrou cobertura explícita para clonar checklist ao abrir uma ocorrência virtual nem para independência completa dos estados entre várias ocorrências.

**Conclusão provisória:** GAP-003 não deve ser tratado como totalmente ausente nem como resolvido. Há reset de estado ao materializar a próxima ocorrência após conclusão, mas a abertura/materialização antecipada de uma ocorrência virtual aparenta não transportar o checklist. É necessário confirmar o fluxo de UI e criar testes focados antes de decidir a menor correção arquitetural. Nenhum código foi alterado nesta auditoria.

### Auditoria e definição necessárias

1. Verificar como subtasks estão ligadas à tarefa recorrente e como novas ocorrências são materializadas.
2. Separar conceitualmente o modelo reutilizável do checklist e o estado de execução de cada ocorrência, caso o código atual não faça essa separação.
3. Definir como alterações no modelo (adicionar, remover, renomear ou reordenar itens) afetam ocorrências futuras, já materializadas e históricas.
4. Definir edição do modelo da série versus edição de uma ocorrência isolada.
5. Preservar IDs, histórico, idempotência, conclusão parcial, reabertura e exceções.
6. Conferir a projeção virtual da Agenda, backup/restauração/merge e entrada estruturada.
7. Criar testes de regressão demonstrando que concluir uma ocorrência não altera nenhuma outra.

**Regra de rigor:** ainda não foi verificado se o comportamento já existe parcialmente no código. Não registrar como defeito confirmado nem implementar uma segunda estrutura antes de auditar o motor atual. O GAP representa uma necessidade e uma verificação obrigatória de completude.

---

## 5. Pendências funcionais e técnicas já rastreadas

Os itens abaixo já constam dos documentos oficiais e permanecem no inventário para evitar que desapareçam entre rodadas. Consultar o estado atual antes de iniciar cada correção:

| ID | Item | Estado registrado | Fonte principal |
|---|---|---|---|
| `BUG-001` | Perda de rota/rascunho ao girar aparelho ou recriar Activity | EM ABERTO; exige validação em dispositivo | `07_ESTADO_ATUAL.md`, `12_RASTREABILIDADE.md` |
| `INT-01` | Motor legado `observeUnifiedAgenda()` ainda alimenta Dashboard/Briefings | EM ABERTO | `07_ESTADO_ATUAL.md`, `10_ROADMAP.md` |
| `INT-03` | Toggle de subtarefa persiste antes de tocar em “Salvar” | PARCIALMENTE EM ABERTO | `07_ESTADO_ATUAL.md`, `10_ROADMAP.md` |
| `REQ-012` | Listagem/criação direta de compromissos na ficha de Evento | PARCIALMENTE IMPLEMENTADA | `02_ESPECIFICACAO_PRODUTO.md`, `12_RASTREABILIDADE.md` |
| `REQ-014` | Campos avançados de Evento (`locationMode`, banner local, apresentação de taxas) | NÃO IMPLEMENTADA | `02_ESPECIFICACAO_PRODUTO.md`, `10_ROADMAP.md` |
| `REQ-026` | Backup ZIP com banners de Eventos | NÃO IMPLEMENTADA | `02_ESPECIFICACAO_PRODUTO.md`, `10_ROADMAP.md` |
| Build/dispositivo | Atualização in-place com keystore persistente, migrações Room e fluxos críticos em aparelho | NÃO VERIFICADO até validação correspondente | `07_ESTADO_ATUAL.md`, `10_ROADMAP.md` |

## 5. Erros encontrados durante a Rodada 3 — histórico e resultado

| Execução | Problema encontrado | Tratamento / resultado conhecido |
|---|---|---|
| GitHub Actions #47 — run `45745484984` | Erros Kotlin em `InboxRepository.kt` (controle `break/continue` em lambda), referência inexistente `palette.negative` e argumento incorreto `NexusEmptyState(message)` em vez de `subtitle` | Correções aplicadas em branch de correção; não considerar #47 validado. |
| GitHub Actions #48 — run `45747309389` | Um teste falhou: `StructuredInputUnitTests > previewEnvelope performs zero DB writes while validating intra-batch links`; a classificação de CHECKLIST como criação/atualização estava incorreta | Corrigido o cálculo semântico em `prepareAndApplyChecklist` para classificar checklist de tarefa existente como atualização. |
| GitHub Actions #49 — run `45747557869` | Tentativa intermediária na branch `fix/inbox-kotlin-compile`; voltou a falhar no mesmo teste `previewEnvelope performs zero DB writes while validating intra-batch links` (`StructuredInputUnitTests.kt:1023`) | Não validou a correção; manter como histórico. |
| GitHub Actions #50 — run `45748189511` | Reexecução após correção final do checklist | **Build job SUCCESS**; testes unitários e APK Debug concluíram com sucesso. Release foi ignorado por ausência de secrets de assinatura Release. Isso valida o workflow dessa execução, não os testes em aparelho, migrações reais ou atualização in-place. |

### 5.1 Branches de correção e decisão de integração (26/11/2026)

As branches abaixo foram criadas para isolar as tentativas de correção da ponte/Inbox e validar o pipeline. Elas não devem ser tratadas como código errado nem integradas integralmente sem revisão.

| Branch | Situação na comparação com `main` em 26/11/2026 | Evidência |
|---|---|---|
| `fix/inbox-build-47-kotlin-compile` | Divergente: 4 commits à frente e 18 atrás (comparação atualizada em 26/11/2026 contra a `main` `18f16438f34c9178274cfb3e82a68a3c80b52409`) | Inclui a correção do teste de CHECKLIST; commit conhecido `30ee1453713fc648d0688fb12615161933e6778b`; GitHub Actions #50 / run `45748189511` passou. |
| `fix/inbox-kotlin-compile` | Divergente: 3 commits à frente e 16 atrás (comparação atualizada em 26/11/2026 contra a `main` `18f16438f34c9178274cfb3e82a68a3c80b52409`) | Tentativa intermediária; manter como histórico, sem integração integral. |

Não foi localizado PR aberto para essas branches. Decisão operacional: **não fazer merge integral**. Manter as branches e seus commits como evidência; reaplicar as correções necessárias no código atual da `main` com o Google AI Studio, revisar o diff e exigir nova CI verde na `main`. Isso evita trazer histórico divergente de uma branch de teste para a linha principal.

**Reaplicação local concluída (aguardando sincronização manual e CI na `main`):** na revisão de continuidade da Rodada 3 sobre o código local deste workspace, foram reaplicadas diretamente: (1) remoção de `continue` dentro de lambdas `getOrElse` em `InboxRepository.kt`, (2) substituição de `palette.negative` por `palette.danger` e `NexusEmptyState(message = ...)` por `subtitle = ...` em `NexusInboxScreen.kt`, e (3) `val isUpdate = true` em `StructuredInputProcessor.prepareAndApplyChecklist`. A execução #50 teve job `build` com conclusão `success` apenas na branch isolada; a validação oficial nesta linha principal depende de sincronização manual para `main` e nova execução do CI. A #47 registrou erros de compilação; a #48 registrou um teste falho posteriormente corrigido; a #49 foi tentativa intermediária. Preservar todas como histórico.

## 6. Regras de manutenção deste registro

1. Ao descobrir falha, registrar sintoma, evidência, ambiente, commit/run, causa (somente quando confirmada), correção, estado e validação pendente.
2. Não apagar falhas corrigidas: movê-las para histórico/estado resolvido, mantendo a evidência.
3. Não marcar como corrigido apenas porque o código foi alterado; exigir o teste apropriado.
4. Ao concluir uma lacuna, atualizar também requisito, estado atual, rastreabilidade, roadmap, ADR/arquitetura e contrato afetados.
5. Distinguir falha de implementação, limitação deliberada de escopo, dívida técnica, inconsistência documental e validação não executada.
6. Atualizar o espelho `NEXUS-DOCUMENTACAO/GOOGLE_IA/docs/` e os artefatos internos do AI Studio quando acessíveis; o espelho e os artefatos nunca substituem a origem em `PROJETO-NEXUS-/docs/`.

## 7. Relação com outros documentos

- Requisitos e status: `02_ESPECIFICACAO_PRODUTO.md`
- Arquitetura e modelo de dados: `03_ARQUITETURA_ENGENHARIA.md`, `04_MODELO_DADOS.md`
- Estado factual: `07_ESTADO_ATUAL.md`
- Qualidade/testes: `08_QUALIDADE_SEGURANCA.md`
- Planejamento: `10_ROADMAP.md`
- Decisões: `11_DECISOES_ADRS.md`
- Rastreabilidade: `12_RASTREABILIDADE.md`
- Entrada estruturada: `14_CONTRATO_ENTRADA_ESTRUTURADA_ETAPA3.md`
- Histórico: `HISTORICO_DOCUMENTACAO.md`


## 7. Validação prática da Ponte ChatGPT → NEXUS (2026-11-26)

| Campo | Registro |
|---|---|
| Estado | **VALIDADA NO FLUXO PRINCIPAL — Rodada 3 concluída** |
| Evidência de CI | GitHub Actions #70, run `45792119560`, na branch `main`: testes unitários e APK Debug concluídos com sucesso. Release ignorado por ausência dos secrets de assinatura. |
| Evidência de integração | O usuário confirmou que o aplicativo funcionou no aparelho e que conseguiu importar registros pela ponte ChatGPT → repositório privado `NEXUS-INBOX` → NEXUS Android. |
| Commit de referência | `4041611a4ef4e111c3633790d0c09babdecb2ae8` |
| Limite da confirmação | A confirmação comprova a importação real, mas não detalha individualmente descarte sem gravação, reconsulta sem duplicidade, execução periódica em segundo plano, notificação/deep-link, reinício/rotação ou todos os cenários de segurança do token. Não marcar esses cenários como testados sem evidência específica. |
| Próxima etapa | Rodada 4: investigação conjunta de `GAP-001` (recorrência de Compromissos/Eventos) e `GAP-003` (checklist independente por ocorrência). `GAP-002` depende da definição dos modelos e permanece aberto. |

A conclusão da Rodada 3 não encerra a manutenção da ponte. Segurança do token, idempotência, sincronização periódica, notificações e recuperação de falhas continuam no gate contínuo de confiabilidade.
