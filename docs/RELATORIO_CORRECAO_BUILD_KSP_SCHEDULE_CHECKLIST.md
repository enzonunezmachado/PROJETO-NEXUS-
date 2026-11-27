# Relatório de correção — KSP / ScheduleChecklistItemDao

- **Data:** 27/11/2026
- **Repositório:** `enzonunezmachado/PROJETO-NEXUS-`
- **Branch:** `fix/ksp-schedule-checklist-missing-import`
- **Base:** `main` em `dbba50906a732ec1705fa68542391f3ac30dcbe9`
- **Commit da correção:** `12c7b5fc23fc259b8a1e426fab9175f9b12be493`

## Erro observado

A tarefa `:app:kspDebugKotlin` falhava durante o processamento do Room:

`NexusDaos.kt:1026: [MissingType]: Element 'com.nexus.app.data.db.dao.ScheduleChecklistItemDao' references a type that is not present`

## Causa raiz

A interface `ScheduleChecklistItemDao`, em `app/src/main/java/com/nexus/app/data/db/dao/NexusDaos.kt`, declara métodos que recebem e retornam `ScheduleChecklistItemEntity`, mas o arquivo não importava essa classe.

A entidade **já existia** em `app/src/main/java/com/nexus/app/data/db/entity/NexusEntities.kt`, e já estava registrada no `NexusDatabase`. Portanto, não era necessário criar outra entidade, alterar a tabela ou modificar a migração.

## Alteração realizada

Adicionado ao bloco de imports de `NexusDaos.kt`, em ordem alfabética:

```kotlin
import com.nexus.app.data.db.entity.ScheduleChecklistItemEntity
```

Nenhum outro arquivo de código foi alterado. Não houve mudança em Room, schema, Backup, contrato Inbox, migrações ou comportamento funcional.

## Validação

- **Inspeção estática:** confirmada a existência da entidade em `NexusEntities.kt`, seu registro em `NexusDatabase.kt` e o uso no DAO.
- **Build Gradle local:** não executada por esta operação.
- **GitHub Actions nesta branch:** pendente de execução após o commit.
- **Validação em dispositivo:** não realizada.

A correção deve ser considerada aplicada no código, mas ainda não validada por compilação até que `:app:testDebugUnitTest :app:assembleDebug` conclua com sucesso no CI.

## Integridade da branch principal

A correção foi feita em uma branch criada diretamente a partir do SHA de `main` informado acima. A branch `main` não foi modificada por esta correção.


## Segunda falha encontrada — compilação do processador de recorrência

Após a correção do import, o KSP passou, mas o CI encontrou erros Kotlin em `StructuredInputProcessor.kt` nas linhas 1306, 1424, 1428, 1469, 1473 e 1500–1517.

### Causas raiz

1. `RecurrenceEngine.parseWeekdaysStrict` exige os argumentos `weekdays` e `rawMask`, e retorna `Result<Int>`. O processador chamava a função com apenas um argumento e tratava o resultado como `Int`.
2. O tipo de `effectiveDueMinutes` não estava explicitado como `Int?`, causando inferência incompatível nos construtores e cópias da entidade.
3. A chamada de `buildUpdatedChecklistTemplate` usava o nome inexistente `newTemplateItems`; a API recebe `newTitles`.
4. `buildUpdatedChecklistTemplate` retorna um `Triple<String, Int, String>`, não uma entidade. O resultado precisava ser aplicado de volta a `baseUpdated` por meio de `copy`, preenchendo `templateSubtasksJson`, `checklistTemplateVersion` e `templateHistoryJson`.

### Correções realizadas

- A chamada de `parseWeekdaysStrict` agora passa os dois campos do DTO e trata falhas com `getOrElse`, retornando recibo de rejeição com a mensagem de validação.
- `effectiveDueMinutes` agora tem tipo explícito `Int?`.
- A chamada do atualizador de template usa `newTitles`.
- O Triple retornado pelo engine é aplicado em `baseUpdated.copy(...)`, preservando os demais campos da série e o histórico de versões do checklist.

**Arquivo de código alterado nesta segunda correção:** `app/src/main/java/com/nexus/app/data/ingestion/StructuredInputProcessor.kt`.

**Commit:** `bb68f74787dcdb1c92e6d8cf435af85d54a5897b`.

### Validação pendente

A correção foi aplicada no branch, mas depende de nova execução do GitHub Actions para confirmar compilação, testes unitários e geração do APK Debug. Não considerar validada até o CI terminar com sucesso.


## Terceira falha encontrada — retorno Result<Int?> no horário da recorrência

Na execução do CI do commit `bb68f74787dcdb1c92e6d8cf435af85d54a5897b`, o KSP passou e a compilação Kotlin avançou até `StructuredInputProcessor.kt:1330`, onde falhou com:

`Initializer type mismatch: expected 'kotlin.Int?', actual 'java.io.Serializable?'`

### Causa raiz

`RecurrenceEngine.parseTimeOfDayMinutesStrict` retorna `Result<Int?>`, mas o processador tratava o retorno como se fosse diretamente `Int?`. O operador Elvis não desempacota um `Result`, e por isso o valor inferido era incompatível com `effectiveDueMinutes`.

### Correção realizada

O processamento de `startTime` agora desempacota o resultado com `getOrElse`. Em caso de horário inválido, o processador retorna um recibo de rejeição com a mensagem da validação; em caso válido ou ausente, mantém o valor `Int?` esperado.

**Arquivo:** `app/src/main/java/com/nexus/app/data/ingestion/StructuredInputProcessor.kt`.

**Commit:** `f469b1295528f2ea384220e0a49084a5eda5deff`.

### Validação pendente

A nova execução do GitHub Actions para esse commit ainda precisa concluir `:app:testDebugUnitTest :app:assembleDebug`. A correção do erro de tipo foi aplicada, mas ainda não está validada por build/testes nem por dispositivo físico.


## Quarta falha encontrada — testes unitários incompatíveis com as APIs atuais

Na execução do CI do commit `f469b1295528f2ea384220e0a49084a5eda5deff`, a compilação do código principal avançou, mas `:app:compileDebugUnitTestKotlin` falhou. Os erros estavam concentrados em testes introduzidos/atualizados na rodada e apontavam nomes de campos, assinaturas e propriedades que não correspondem às APIs atuais do repositório.

### Ajustes realizados

- `OccurrenceChecklistIsolationUnitTests.kt`: alinhados os nomes dos campos de template à entidade real (`templateSubtasksJson`), o snapshot ao tipo top-level `ChecklistTemplateSnapshot`, a chamada de atualização à assinatura `newTitles`, a ordem do `Triple` retornado pelo engine e o nome da coleção de recorrências do backup (`recurrences`).
- `StructuredInputUnitTests.kt`: substituído o método inexistente `previewEnvelopeJson` por `previewJson` e alinhado o campo do template.
- `FactoryResetUnitTests.kt`: corrigidos o pacote de `MarketAssetCatalog`, os campos/enumerações atuais de histórico de mercado, a assinatura do construtor de `InboxRepository` e o retorno booleano de `resetLocalTrackingPreservingCredentials`. A asserção do estado do histórico agora considera o mapa inicial de ativos, verificando que todas as séries foram removidas.
- `InboxBridgeUnitTests.kt`: alinhados o construtor e os métodos atuais de `InboxRepository`, a leitura dos itens de prévia, a confirmação da importação e o retorno booleano da limpeza de rastreamento.

Esses ajustes mantêm os cenários de teste e os alinham às interfaces efetivamente implementadas, em vez de alterar as APIs de produção apenas para satisfazer testes desatualizados.

### Commits das correções de testes

- `c49c9b850424aed3d404628288555b4577914019` — testes de isolamento/backup de recorrências.
- `217a7e150b63af5cac06c31f9cb280baa38dc5be` — testes de entrada estruturada.
- `aedf7032f34d7c0ab1a5049cb66d28d4edd454fb` e `49bd3b8c051c70cc235a04afaf680081e0d32999` — testes de restauração de fábrica.
- `8698cb96f2f57ce10aa4ab91b9c864b6a818e2e2` e `37adecd31ed8391edd8bb93f356feaa54b7bdb85` — testes da ponte Inbox/recorrência e ajuste final de backup.

### Validação atual

O GitHub Actions foi disparado para o commit final `37adecd31ed8391edd8bb93f356feaa54b7bdb85` (execução `46065813592`). Na última consulta, a etapa `:app:testDebugUnitTest :app:assembleDebug` ainda estava em execução. Portanto, a compatibilidade dos testes foi corrigida no código, mas a validação integral do CI continua pendente.


## Quinta validação — correção da fixture e CI aprovado

Na execução #124 (`46067250178`), KSP, compilação Kotlin principal e geração do APK Debug passaram. O único teste que falhou foi a asserção de criação de arquivo em `FactoryResetUnitTests.kt:285`.

### Causa raiz

A fixture de `FileMarketHistoryStore` fornecia apenas um `MarketHistoryPoint`. A implementação real de `saveValidSeries` normaliza os pontos e descarta séries com menos de dois pontos; por isso, nenhum arquivo era persistido e a asserção falhava. A regra de produção estava correta; a massa de teste era inválida.

### Correção realizada

A fixture passou a fornecer dois pontos válidos, separados por um dia, preservando a regra de produção.

**Arquivo:** `app/src/test/java/com/nexus/app/data/market/FactoryResetUnitTests.kt`.

**Commit:** `9156aba40dfbd9ae3754fb8d883e33c40ea6f422`.

### Resultado comprovado no GitHub Actions

- **Execução:** #125 — `46067672812`.
- **Commit testado:** `9156aba40dfbd9ae3754fb8d883e33c40ea6f422`.
- **Job `build`:** `success`.
- **Run Unit Tests and Build NEXUS Debug APK:** `success`.
- **Verificação de assinatura e certificado:** `success`.
- **Upload do APK Debug:** `success`.
- **Build Release:** `skipped`, pois os secrets de assinatura Release não estão configurados.

Link: https://github.com/enzonunezmachado/PROJETO-NEXUS-/actions/runs/46067672812

A validação CI de testes unitários e APK Debug está aprovada para esse commit. Isso não equivale a validação em dispositivo físico. A branch permanece isolada; não houve merge para `main`.
