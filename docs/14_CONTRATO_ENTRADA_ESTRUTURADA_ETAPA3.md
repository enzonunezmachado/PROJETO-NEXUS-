# 14 — Contrato de Entrada Estruturada (`v1`) e Ponte ChatGPT → NEXUS-INBOX (Rodada 3)

> **Estado atual (Rodada 3 — Ponte ChatGPT → NEXUS implementada):**
> - Camada interna de ingestão em `app/src/main/java/com/nexus/app/data/ingestion/` (`StructuredInputModels.kt`, `StructuredInputGateway.kt`, `StructuredInputProcessor.kt`) conectada ao `NexusRepository` (`previewStructuredInputJson`, `previewStructuredInputEnvelope`, `processStructuredInputJson`, `processStructuredInputEnvelope`) sobre o banco Room único `v6` (`NexusDatabase`) + `audit_history`.
> - Conector externo somente-leitura **NEXUS-INBOX** em `app/src/main/java/com/nexus/app/data/inbox/` (`InboxModels.kt`, `InboxStores.kt`, `GitHubInboxRemoteDataSource.kt`, `InboxRepository.kt`, `InboxSyncWorker.kt`) integrado à UI em `MoreScreen` → `SubRoute.InboxBridge` (`NexusInboxScreen.kt`).

---

## 1. Propósito e Garantias Estruturais

1. **Banco único (Zero segundo banco / Zero migração Room):** grava diretamente nas tabelas oficiais do `NexusDatabase` (`DATABASE_VERSION = 6`), mantendo 100% compatibilidade com `BackupManager` (`v7`), Widgets Glance, Lembretes (`rescheduleAllRemindersInternal`) e telas Compose.
2. **Idempotência transacional via `audit_history`:** toda operação com desfecho persistido (`APPLIED`, `UPDATED` ou `CONFLICT_PRESERVED_LOCAL`) registra recibo em `audit_history` (`entityType = "STRUCTURED_INPUT_OP"`, `entityId = operationId`, `action = state.name`) dentro da **mesma transação `database.withTransaction`**. O resumo consolidado de cada importação confirmada da NEXUS-INBOX também é gravado em `audit_history` (`entityType = "INBOX_IMPORT_LOG"`).
3. **Prévia Dry-Run fiel antes de qualquer gravação:** `previewJson` / `previewEnvelope` utilizam `PreviewOverlayGateway` em memória para simular o lote inteiro — inclusive dependências encadeadas no mesmo JSON (ex.: `TASK` → `CHECKLIST` ou `TASK` → `TRANSACTION`) — com **zero escritas** no SQLite até confirmação explícita do usuário.
4. **Rollback seguro e reprocessamento limpo:** se houver falha SQLite ou aborto de lote atômico (`atomicBatch = true`), nenhuma entidade parcial nem recibo `APPLIED`/`UPDATED` permanece salvo — permitindo reenviar exatamente o mesmo `operationId` após corrigir a falha sem falso `DUPLICATE_IGNORED`.
5. **JSON estrito (`ignoreUnknownKeys = false`):** campos desconhecidos, `contractVersion != 1`, `source` ausente, valores negativos/nulos inválidos ou enums inexistentes são rejeitados explicitamente (`REJECTED` / `envelopeError`).

---

## 2. Envelope JSON Oficial (`contractVersion = 1`)

### Cabeçalho (`StructuredInputEnvelope` & `StructuredInputOperation`)
- `contractVersion`: obrigatório e estritamente igual a `1` (`CURRENT_CONTRACT_VERSION`) tanto no envelope quanto em cada operação.
- `source`: obrigatório no envelope ou em cada operação (ex.: `"chatgpt-nexus-inbox"`); usado para rastreabilidade em `audit_history`.
- `operationId`: obrigatório e não vazio em cada operação; chave de idempotência.
- `action` *(opcional na operação, padrão `CREATE`)*:
  - `"CREATE"` *(padrão)*: insere se não existir; se já existir por `id` ou chave natural, **preserva o dado local** (`CONFLICT_PRESERVED_LOCAL`).
  - `"UPDATE"`: atualiza registro existente preservando `createdAt` e o histórico prévio; rejeita com `REJECTED` se o alvo não existir.
  - `"UPSERT"`: cria se não existir (`APPLIED`) ou atualiza preservando `createdAt` se já existir (`UPDATED`).
- `atomicBatch` (parâmetro do processador/repositório):
  - `false` *(padrão na NEXUS-INBOX)*: cada operação roda em sua própria transação; rejeições individuais retornam `REJECTED` sem bloquear operações irmãs válidas.
  - `true`: todo o lote roda em uma única transação; qualquer `REJECTED` ou exceção reverte integralmente todas as operações do lote (`FAILED_ROLLED_BACK`).

### Template Pronto para ChatGPT (`NEXUS-INBOX`)

Quando o usuário pedir *"Registra tudo isso no NEXUS"*, o ChatGPT deve gerar um arquivo `.json` no repositório privado seguindo exatamente este contrato (incluindo evento, tarefa com subtarefas em checklist e gasto vinculado):

```json
{
  "contractVersion": 1,
  "source": "chatgpt-nexus-inbox",
  "createdAt": 1795000000000,
  "operations": [
    {
      "operationId": "op-2026-11-26-evt-01",
      "entityType": "EVENT",
      "action": "UPSERT",
      "contractVersion": 1,
      "createdAt": 1795000000000,
      "data": {
        "id": "evt-summit-2026",
        "name": "Conferência de Produto 2026",
        "startAt": 1795514400000,
        "endAt": 1795546800000,
        "location": "São Paulo Expo",
        "status": "CONFIRMED",
        "ticketStatus": "FREE",
        "ticketUnitPriceCents": 0,
        "ticketQuantity": 1,
        "ticketFeesCents": 0,
        "notes": "Credenciamento às 08h30"
      }
    },
    {
      "operationId": "op-2026-11-26-task-01",
      "entityType": "TASK",
      "action": "UPSERT",
      "contractVersion": 1,
      "createdAt": 1795000000000,
      "data": {
        "id": "task-prep-summit",
        "title": "Preparar viagem da Conferência",
        "description": "Checklist e reserva de transporte",
        "eventId": "evt-summit-2026",
        "priority": "HIGH",
        "status": "NOT_STARTED",
        "dueAt": 1795428000000,
        "hasDueTime": true,
        "subtasks": [
          { "id": "sub-summit-1", "title": "Confirmar credencial", "isCompleted": false, "sortOrder": 0 },
          { "id": "sub-summit-2", "title": "Separar documentos", "isCompleted": false, "sortOrder": 1 }
        ]
      }
    },
    {
      "operationId": "op-2026-11-26-chk-01",
      "entityType": "CHECKLIST",
      "action": "UPSERT",
      "contractVersion": 1,
      "createdAt": 1795000000000,
      "data": {
        "taskId": "task-prep-summit",
        "items": [
          { "id": "sub-summit-3", "title": "Baixar ingresso offline", "isCompleted": false, "sortOrder": 2 }
        ]
      }
    },
    {
      "operationId": "op-2026-11-26-tx-01",
      "entityType": "TRANSACTION",
      "action": "CREATE",
      "contractVersion": 1,
      "createdAt": 1795000000000,
      "data": {
        "id": "tx-passagem-summit",
        "description": "Passagem aérea conferência",
        "type": "EXPENSE",
        "amountCents": 68900,
        "occurredAt": 1795000000000,
        "status": "PENDING",
        "paymentMethod": "PIX",
        "eventId": "evt-summit-2026",
        "taskId": "task-prep-summit"
      }
    }
  ]
}
```

---

## 3. Entidades Suportadas e Campos Reais (`StructuredEntityType`)

Todos os DTOs em `StructuredInputModels.kt` espelham **exclusivamente colunas reais** de `NexusEntities.kt`:

| `entityType` | DTO Kotlin | Campos Obrigatórios / Validação Chave | FKs Verificadas (Rejeita Órfãos) | Chaves Naturais / Conflito Local |
| --- | --- | --- | --- | --- |
| `TASK` | `StructuredTaskData` | `title` não vazio; `priority` (`TaskPriority`); `status` (`TaskStatus`); `subtasks` opcional com `title` não vazio | `projectId`, `categoryId`, `eventId`, `goalId`, `recurrenceId` | `id` (`tasks`) e par `(recurrenceId, occurrenceDateIso)` |
| `CHECKLIST` | `StructuredChecklistData` | `taskId` não vazio; `items` não vazio; cada item com `title` não vazio | `taskId` (`tasks`) | `id` de cada subtarefa na tabela `subtasks` |
| `APPOINTMENT` | `StructuredAppointmentData` | `title` não vazio; `startAt > 0`; `endAt >= startAt` | `categoryId`, `eventId` | `id` (`appointments`) |
| `EVENT` | `StructuredEventData` | `name` não vazio; `startAt > 0`; `endAt >= startAt`; quantidades/valores `>= 0`; enums `EventLifecycleStatus`, `TicketStatus`, `PaymentMethod` | `categoryId`, `creditCardId` | `id` (`events`) |
| `PROJECT` | `StructuredProjectData` | `name` não vazio; enums `ProjectStatus`, `TaskPriority` | `goalId` (`personal_goals`) | `id` (`projects`) |
| `PERSONAL_GOAL` | `StructuredPersonalGoalData` | `title` não vazio; `manualProgressPercent in 0..100`; enums `GoalStatus`, `GoalProgressMode` | `categoryId` | `id` (`personal_goals`) |
| `TRANSACTION` | `StructuredTransactionData` | `description` não vazia; `amountCents > 0`; `occurredAt > 0`; enums `TransactionType`, `FinancialStatus`, `PaymentMethod`, `EventCostType?`; bounds de parcelas | `categoryId`, `projectId`, `eventId`, `taskId`, `creditCardId`, `statementId`, `installmentPurchaseId`, `recurringRuleId` | `id` (`transactions`), `(installmentPurchaseId, installmentNumber)`, `(recurringRuleId, occurrenceIndex)` |

### Regras Financeiras Específicas para `TRANSACTION`
1. Se `statementId` for informado e `creditCardId` também, ambos devem pertencer ao mesmo cartão.
2. Faturas com status `PAID` rejeitam lançamentos com status diferente de `SETTLED`.
3. Bloqueio anti-dupla contagem (`ADR-006`): despesas avulsas com descrição de pagamento de fatura (`"pagamento de fatura"`, `"quitação de fatura"`, etc.) sem `statementId`/`installmentPurchaseId` quando há cartão/fatura paga vinculada são **rejeitadas**.

---

## 4. Estados de Processamento (`StructuredProcessingState`)

| Estado | Gravado em `audit_history`? | Significado e Comportamento em Reenvio |
| --- | --- | --- |
| `PENDING` | Não | Estado inicial padrão do DTO antes do processamento |
| `VALIDATED` | Não | Estado reservado para validações intermediárias |
| `APPLIED` | Sim | Entidade inserida no Room com sucesso; reenvio futuro do mesmo `operationId` retorna `DUPLICATE_IGNORED` |
| `UPDATED` | Sim | Entidade existente atualizada (`UPDATE` / `UPSERT`) preservando `createdAt`; reenvio futuro do mesmo `operationId` retorna `DUPLICATE_IGNORED` |
| `CONFLICT_PRESERVED_LOCAL` | Sim | Registro com mesmo `id` ou chave natural já existia no aparelho em operação `CREATE`; dado local preservado intacto; reenvio futuro do `operationId` retorna `DUPLICATE_IGNORED` |
| `DUPLICATE_IGNORED` | Não (lê estado anterior) | `operationId` já registrado anteriormente como `APPLIED`, `UPDATED` ou `CONFLICT_PRESERVED_LOCAL`; nenhuma escrita realizada |
| `REJECTED` | Não | Erro de contrato, campo obrigatório, enum inválido, FK órfã ou regra financeira; sem escrita para aquela operação (ou rollback completo se `atomicBatch = true`) |
| `FAILED_ROLLED_BACK` | Não | Exceção transacional ou aborto de lote atômico; nenhuma escrita nem `audit_history` persistidos — seguro para retentativa com o mesmo `operationId` |

---

## 5. Conector Externo `NEXUS-INBOX` (`data/inbox/`)

A ponte **ChatGPT → NEXUS-INBOX → NEXUS Android** opera sobre este contrato seguindo as regras abaixo:

1. **Acesso discreto:** localizada em **Mais → NEXUS-INBOX (Ponte ChatGPT)** (`SubRoute.InboxBridge`, `NexusInboxScreen.kt`), preservando inalterada a navegação inferior (`MainTab`).
2. **Transporte GitHub privado somente leitura:** `GitHubInboxRemoteDataSource` usa exclusivamente `HttpsURLConnection` (`GET`, sem redirects automáticos, validando host `https://api.github.com`, limite de `2 MB` por arquivo JSON) com autenticação por **Fine-Grained PAT** configurado pelo usuário e criptografado via **Android Keystore (`AES/GCM/NoPadding`)** em `AndroidKeystoreInboxTokenStore`. Zero credenciais, URLs privadas ou tokens são embutidos no repositório.
3. **Fluxo em 2 etapas (Dry-Run → Confirmação Explícita):**
   - `InboxRepository.checkInboxNow` baixa arquivos `.json` novos ou com SHA alterado (`path::sha`), executa `NexusRepository.previewInboxEnvelope` sem gravar no Room e exibe a prévia detalhada.
   - Somente após o usuário tocar em **"Confirmar importação"**, `InboxRepository.confirmAndImportPending` invoca `NexusRepository.applyInboxEnvelope`, marca arquivos terminais não defeituosos como processados e persiste em `audit_history` (`INBOX_IMPORT_LOG`) o resumo real das alterações — omitindo categorias com contagem zero.
4. **Verificação periódica opcional em segundo plano:** `InboxSyncWorker` (`WorkManager`, intervalo mínimo de `15 min`, `NetworkType.CONNECTED`, bateria não baixa) apenas consulta e notifica (`NexusNotificationScheduler.showInboxPendingNotification`) com deep-link para `SubRoute.InboxBridge`, **jamais importando dados automaticamente sem confirmação**.


## 6. Limitações atuais de recorrência

O contrato `v1` não permite criar regras/séries recorrentes para `APPOINTMENT` ou `EVENT`. Também não disponibiliza uma operação `RECURRENCE` para criar uma regra nova de recorrência de tarefa; a presença de `recurrenceId` como referência opcional em dados de tarefa não cria a regra referenciada.

Até a implementação e validação de `REQ-009` e a definição de uma extensão/versionamento compatível do contrato:
- `APPOINTMENT` e `EVENT` recebidos pela Inbox representam registros únicos.
- Não gerar múltiplas operações avulsas para simular uma série recorrente.
- Não tratar texto em notas/checklists como compromisso real nem como agendamento de lembretes.
- O ChatGPT deve explicar a limitação e preservar a intenção recorrente como pendência explícita, sem inventar ocorrências.

A especificação e o acompanhamento dessa lacuna estão em [`15_REGISTRO_LACUNAS_E_FALHAS.md`](./15_REGISTRO_LACUNAS_E_FALHAS.md) (`GAP-001` e `GAP-002`).
