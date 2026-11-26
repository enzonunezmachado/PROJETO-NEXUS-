# 15 — Registro Consolidado de Lacunas, Defeitos e Validações Pendentes

> **Finalidade:** manter em um único lugar rastreável as funcionalidades incompletas, inconsistências documentais, defeitos observados durante implementação e validações ainda não concluídas. Este registro complementa — não substitui — requisitos, estado atual, roadmap, ADRs e código.
>
> **Regra de rigor:** “implementado” não significa “validado”; falha corrigida em uma execução não deve ser tratada como pendência aberta, mas precisa manter seu histórico. Este é o inventário inicial consolidado em 26/11/2026, não uma declaração de que todos os arquivos e fluxos do aplicativo foram novamente auditados nesta atualização.

## 1. Prioridade alta — recorrência de compromissos e eventos

| Campo | Registro |
|---|---|
| ID | `GAP-001` |
| Requisito relacionado | `REQ-009` |
| Estado | **IMPLEMENTADA NO CÓDIGO (Rodada 4) — aguardando compilação Gradle/CI e validação em dispositivo** |
| Origem | `docs/02_ESPECIFICACAO_PRODUTO.md`, `docs/07_ESTADO_ATUAL.md`, `docs/10_ROADMAP.md`, `docs/12_RASTREABILIDADE.md`, `ADR-008`, `ADR-019` |
| Escopo afetado | Compromissos, Eventos, Agenda, lembretes/notificações, Room `v7`, Backup `v8` (entrada estruturada `GAP-002` mantida fora desta rodada) |
| Evidência prática | No povoamento de 26/11/2026, a corrida semanal “A Vida é um Corre”, quinta-feira às 20h, não podia ser registrada como compromisso recorrente nem criada no app. Na Rodada 4, o suporte nativo a séries recorrentes de Compromissos e Eventos foi implementado no aplicativo Android. |

### Implementação realizada na Rodada 4 (`ADR-019`)
- **Modelo e migração (`Room v7`):** `TaskRecurrenceEntity` estendida com `RecurrenceDomain` (`TASK`, `APPOINTMENT`, `EVENT`), frequência `TaskRecurrenceFrequency.ANNUAL`, duração/allDay/local/links/snapshot de evento; `appointments` e `events` receberam `recurrenceId` (`SET_NULL`) + `occurrenceDateIso` + `UNIQUE(recurrenceId, occurrenceDateIso)`; `RecurrenceExceptionEntity` recebeu overrides de horário/descrição/local/link/allDay e referências `appointmentId`/`eventId` via `MIGRATION_6_7`.
- **Motor, projeção e conflitos:** `RecurrenceEngine` calcula ocorrências de Compromissos e Eventos (incluindo `ANNUAL` com ajuste de 29/02) na zona `America/Sao_Paulo` e gera IDs virtuais determinísticos `"rec::<recurrenceId>::<occurrenceDateIso>"`. `AgendaEngine` e `EventsScreen` projetam séries virtuais na janela consultada (com detecção de conflitos virtual↔materializado na Agenda), sem pré-gerar centenas de linhas.
- **Materialização sob demanda e escopos:** ao abrir/editar/excluir um item `"rec::..."`, `NexusRepository` materializa a ocorrência de forma idempotente ou aplica `RecurrenceExceptionEntity` (`SKIP` / `OVERRIDE`), suportando `"Só esta ocorrência"` vs `"Toda a série"` em `EntityEditors.kt` (`AppointmentEditorSheet` e `EventEditorSheet`).
- **Proteção financeira em Eventos:** ocorrências virtuais e materializações sob demanda nunca criam obrigações `TransactionEntity`; edições/exclusões `"Toda a série"` preservam e desvinculam ocorrências já com `ATTENDED` ou ingresso `PAID` / transação `SETTLED`.
- **Lembretes e Backup `v8`:** `NexusNotificationScheduler` e `NexusRepository` agendam/cancelam lembretes para ocorrências virtuais `rec::...`; `BackupModels.kt`, `BackupManager.kt` e `BackupMergeEngine.kt` evoluídos para `CURRENT_FORMAT_VERSION = 8`.
- **Testes automatizados adicionados:** `NexusRodada4RecurrenceUnitTests.kt` (`5` `@Test`) e ampliação de `BackupIncrementalMergeUnitTests.kt` (`15` `@Test`).

### Validação restante antes de encerrar em produção
- Executar `:app:testDebugUnitTest :app:assembleDebug` no GitHub Actions ou Android Studio (container atual sem JDK/Gradle/SDK).
- Validar `MIGRATION_6_7` em aparelho e testar criação de série semanal/anual de Compromisso e Evento, projeção na Agenda/Eventos, edição `"Só esta ocorrência"` vs `"Toda a série"`, cancelamento, ausência de transações fantasma, backup `v8` e lembretes após reinício.
- **Dependência da ponte (`GAP-002`):** extensão do contrato `v1` da `NEXUS-INBOX` para criar regras recorrentes via JSON ficou explicitamente fora da Rodada 4.

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
| Próximo passo | Agora que `GAP-001` e `GAP-003` foram implementados no aplicativo (`Room v7` / `Backup v8`), definir extensão/versão do contrato `v1`, DTOs, validação, preview, idempotência, conflitos e importação transacional para séries recorrentes. Não simular recorrência com ocorrências avulsas. |

## 4. Requisito transversal: checklist independente por ocorrência recorrente

| Campo | Registro |
|---|---|
| ID | `GAP-003` |
| Estado | **IMPLEMENTADA NO CÓDIGO (Rodada 4) — aguardando compilação Gradle/CI e validação em dispositivo** |
| Escopo | Recorrência de tarefas, checklist/subtasks, histórico, Agenda, backup/restore/merge |
| Origem | Necessidade de produto explicitada em 26/11/2026 |
| Implementação | `TaskRecurrenceEntity.templateSubtasksJson` (`Room v7` / `ADR-019`), backfill `MIGRATION_6_7` + backups `<= v7`, `RecurrenceEngine`, `NexusRepository`, `TaskEditorSheet` e `NexusRodada4RecurrenceUnitTests.kt` |

### Necessidade e implementação realizada (Rodada 4)

- **Separação modelo × execução:** a estrutura reutilizável da série (títulos ordenados das subtarefas) agora fica persistida em `TaskRecurrenceEntity.templateSubtasksJson`, enquanto cada `TaskEntity` mantém suas próprias `SubtaskEntity` com estado independente `isCompleted`.
- **Ciclo de ocorrência e retrocompatibilidade:** na conclusão (`completeTask`), a nova ocorrência é materializada com subtarefas zeradas (`isCompleted = false`) a partir do template da série, sem copiar marcações da ocorrência anterior (que permanece intacta no histórico). `MIGRATION_6_7` e o `BackupManager` (`<= v7`) preenchem automaticamente `templateSubtasksJson` para séries antigas.
- **Controle explícito de escopo na UI:** alternar `isCompleted` grava apenas na ocorrência aberta sem tocar em `templateSubtasksJson`; alterações estruturais (adicionar, renomear, remover, reordenar) em tarefa recorrente perguntam explicitamente `"Só esta ocorrência"` vs `"Toda a série"` em `TaskEditorSheet`.
- **Validação restante:** executar `:app:testDebugUnitTest` no CI/Gradle (incluindo `NexusRodada4RecurrenceUnitTests`) e confirmar em aparelho o cenário completo de 2 ciclos semanais.

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
| Build/dispositivo | Atualização in-place (`v6→v7`), `MIGRATION_6_7`, séries de Compromissos/Eventos, checklist independente entre ocorrências, `19` suítes (`145` `@Test`) e fluxos críticos em aparelho | NÃO VERIFICADO até execução Gradle/CI e validação em dispositivo | `07_ESTADO_ATUAL.md`, `10_ROADMAP.md` |

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
