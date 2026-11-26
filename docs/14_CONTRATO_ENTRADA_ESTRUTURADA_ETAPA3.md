# 14 — Contrato da Camada Interna de Entrada Estruturada (Guia para Etapa 3)

> **Escopo atual (Etapa 2):** camada interna isolada, versionada e testável em `app/src/main/java/com/nexus/app/data/ingestion/` (`StructuredInputModels.kt`, `StructuredInputGateway.kt`, `StructuredInputProcessor.kt`) conectada ao `NexusRepository` (`processStructuredInputJson` e `processStructuredInputEnvelope`) sobre o banco Room único `v6` (`NexusDatabase`), **sem sincronização externa/rede/GitHub API ainda** (reservada para a **Etapa 3**).

---

## 1. Propósito e Garantias Estruturais

1. **Banco único (Zero segundo banco / Zero migração Room):** grava diretamente nas tabelas oficiais do `NexusDatabase` (`DATABASE_VERSION = 6`), mantendo 100% compatibilidade com `BackupManager` (exportação/importação/mesclagem), Widgets Glance, Lembretes (`rescheduleAllRemindersInternal`) e telas Compose.
2. **Idempotência transacional via `audit_history`:** toda operação com desfecho persistido (`APPLIED` ou `CONFLICT_PRESERVED_LOCAL`) registra recibo em `audit_history` (`entityType = "STRUCTURED_INPUT_OP"`, `entityId = operationId`, `action = state.name`) dentro da **mesma transação `database.withTransaction`**.
3. **Rollback seguro e reprocessamento limpo:** se houver falha SQLite ou aborto de lote atômico (`atomicBatch = true`), nenhuma entidade parcial nem recibo `APPLIED` permanece salvo — permitindo reenviar exatamente o mesmo `operationId` após corrigir a falha sem falso `DUPLICATE_IGNORED`.
4. **JSON estrito (`ignoreUnknownKeys = false`):** campos desconhecidos, `contractVersion != 1`, `source` ausente, valores negativos/nulos inválidos ou enums inexistentes são rejeitados explicitamente (`REJECTED` / `envelopeError`).

---

## 2. Envelope JSON Oficial (`contractVersion = 1`)

```json
{
  "contractVersion": 1,
  "source": "identificador_origem_etapa3",
  "createdAt": 1795000000000,
  "operations": [
    {
      "operationId": "op-unica-2026-11-25-001",
      "entityType": "TASK",
      "contractVersion": 1,
      "createdAt": 1795000000000,
      "source": "opcional_se_presente_no_envelope",
      "data": {
        "id": "opcional_se_omitido_gera_ing-task-operationId",
        "title": "Revisar entrega da Etapa 3",
        "description": "Detalhes opcionais",
        "projectId": null,
        "categoryId": null,
        "eventId": null,
        "goalId": null,
        "priority": "HIGH",
        "status": "NOT_STARTED",
        "dueAt": 1795086400000,
        "hasDueTime": true,
        "notes": "",
        "completedAt": null,
        "recurrenceId": null,
        "occurrenceDateIso": null
      }
    }
  ]
}
```

### Regras do Cabeçalho (`StructuredInputEnvelope` & `StructuredInputOperation`)
- `contractVersion`: obrigatório e estritamente igual a `1` (`CURRENT_CONTRACT_VERSION`) tanto no envelope quanto em cada operação.
- `source`: obrigatório no envelope ou em cada operação; usado para rastreabilidade em `audit_history`.
- `operationId`: obrigatório e não vazio em cada operação; chave de idempotência.
- `atomicBatch` (parâmetro do processador/repositório):
  - `false` *(padrão)*: cada operação roda em sua própria transação; rejeições individuais retornam `REJECTED` sem bloquear operações irmãs válidas.
  - `true`: todo o lote roda em uma única transação; qualquer `REJECTED` ou exceção reverte integralmente todas as operações do lote (`FAILED_ROLLED_BACK`).

---

## 3. Entidades Suportadas e Campos Reais (`StructuredEntityType`)

Todos os DTOs em `StructuredInputModels.kt` espelham **exclusivamente colunas reais** de `NexusEntities.kt`:

| `entityType` | DTO Kotlin | Campos Obrigatórios / Validação Chave | FKs Verificadas (Rejeita Órfãos) | Chaves Naturais / Conflito Local |
| --- | --- | --- | --- | --- |
| `TASK` | `StructuredTaskData` | `title` não vazio; `priority` (`TaskPriority`); `status` (`TaskStatus`) | `projectId`, `categoryId`, `eventId`, `goalId`, `recurrenceId` | `id` (`tasks`) e par `(recurrenceId, occurrenceDateIso)` |
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
| `CONFLICT_PRESERVED_LOCAL` | Sim | Registro com mesmo `id` ou chave natural já existia no aparelho; dado local preservado intacto; reenvio futuro do `operationId` retorna `DUPLICATE_IGNORED` |
| `DUPLICATE_IGNORED` | Não (lê estado anterior) | `operationId` já registrado anteriormente como `APPLIED` ou `CONFLICT_PRESERVED_LOCAL`; nenhuma escrita realizada |
| `REJECTED` | Não | Erro de contrato, campo obrigatório, enum inválido, FK órfã ou regra financeira; sem escrita para aquela operação (ou rollback completo se `atomicBatch = true`) |
| `FAILED_ROLLED_BACK` | Não | Exceção transacional ou aborto de lote atômico; nenhuma escrita nem `audit_history` persistidos — seguro para retentativa com o mesmo `operationId` |

---

## 5. Como a Etapa 3 Deve Integrar Aqui

Na Etapa 3 (quando o transporte externo aprovado for conectado, ex.: leitor de pasta/inbox sincronizada):
1. O conector externo **não** deve acessar DAOs do Room diretamente; ele deve apenas construir o JSON ou `StructuredInputEnvelope` (`contractVersion = 1`) e chamar `nexusRepository.processStructuredInputJson(rawJson, atomicBatch)` ou `nexusRepository.processStructuredInputEnvelope(envelope, atomicBatch)`.
2. O conector deve inspecionar `StructuredBatchResult.receipts` para confirmar apenas os itens `APPLIED`, `CONFLICT_PRESERVED_LOCAL` ou `DUPLICATE_IGNORED`, mantendo `FAILED_ROLLED_BACK` elegíveis para retentativa e expondo `REJECTED` para correção na origem.
3. Para sincronização e atualização contínua do APK sem perda de dados (`INSTALL_FAILED_UPDATE_INCOMPATIBLE`), manter configurados os **GitHub Secrets de assinatura** descritos em [`08_QUALIDADE_SEGURANCA.md`](./08_QUALIDADE_SEGURANCA.md) e [`keystore.properties.example`](../keystore.properties.example).
