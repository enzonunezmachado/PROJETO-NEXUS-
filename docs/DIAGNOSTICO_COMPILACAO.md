# Diagnóstico Técnico e Correção da Falha de Compilação — NEXUS

**Data:** 2026-11-17  
**Base analisada:** código-fonte local do projeto Android `com.nexus.app` (Kotlin `2.1.0`, AGP `8.7.3`, KSP `2.1.0-1.0.29`, Gradle `8.10.2`, Compose BOM `2024.12.01`, Room `2.6.1`).  
**Observação de ambiente:** este ambiente não possui JDK, Gradle ou Android SDK instalados; portanto, a análise e a verificação estrutural foram feitas estaticamente sobre os arquivos reais e o log completo do GitHub Actions, sem execução de nova compilação local.

---

## A. Diagnóstico original

### 1. Erro encontrado
- **Tarefa que falhou:** `:app:compileDebugKotlin` (após `:app:kspDebugKotlin` ter concluído com sucesso em ~30 segundos: `09:35:50` -> `09:36:20`).
- **Tempo até a falha:** ~9 minutos e 7 segundos dentro de `:app:compileDebugKotlin` (`09:36:20` -> `09:45:27`).
- **Erro final:** `e: java.lang.OutOfMemoryError: GC overhead limit exceeded` no worker do compilador Kotlin (`GradleKotlinCompilerWorkAction`), seguido da orientação do Gradle sobre `kotlin.daemon.jvmargs=-Xmx<size>`.

### 2. Causa raiz identificada (stack trace do compilador K2 FIR)
O stack trace aponta diretamente o gargalo no front-end **K2 (FIR)** do Kotlin `2.1.0`:

```text
PersistentVector.add(PersistentVector.kt:39)
  <- LogicSystem.addImplication(LogicSystem.kt:86)
  <- FirDataFlowAnalyzer.addImplication(FirDataFlowAnalyzer.kt:1543)
  <- FirDataFlowAnalyzer.exitSafeCall$lambda$29(FirDataFlowAnalyzer.kt:891)
  <- FirDataFlowAnalyzer.buildDefaultFlow(FirDataFlowAnalyzer.kt:1400)
  <- FirDataFlowAnalyzer.mergeIncomingFlow(FirDataFlowAnalyzer.kt:1446)
  <- FirDataFlowAnalyzer.exitSafeCall(FirDataFlowAnalyzer.kt:852)
  <- FirExpressionsResolveTransformer.transformSafeCallExpression(...)
  <- FirArgumentListImpl.transformArguments(...)
  <- FirExpressionsResolveTransformer.transformFunctionCall(...)
  <- FirDeclarationsResolveTransformer.transformDelegateExpression(...)
```

Esse caminho indica exatamente o que travou o compilador:
1. O compilador estava resolvendo **expressões de delegate (`by ...`)** (`transformDelegateExpression`) contendo chamadas de função (`transformFunctionCall` — `remember(...)`) com **safe calls (`?.`) dentro da lista de argumentos** (`FirArgumentListImpl.transformArguments` -> `transformSafeCallExpression`).
2. No `FirDataFlowAnalyzer` do Kotlin `2.1.0`, cada `?.` em sequência dentro do mesmo corpo de função gera implicações de nulabilidade no `LogicSystem` (armazenadas em `PersistentVector` imutáveis) e executa `mergeIncomingFlow` / `buildDefaultFlow` ao sair de cada chamada segura.
3. Quando dezenas de declarações sequenciais `val ... = remember(obj?.id, ...)` e `var ... by remember(obj?.id, sub?.id, sub?.updatedAt) { mutableStateOf(...) }` referenciam as mesmas variáveis nuláveis encadeadas dentro de uma única função Composable extensa, o conjunto de implicações no `LogicSystem` cresce de forma combinatória, levando o GC da JVM a consumir >98% do tempo copiando `PersistentVector` até estourar o heap.

### 3. Hipóteses investigadas e evidências encontradas

| Hipótese | Resultado | Evidências concretas no projeto |
|---|---|---|
| **H1. Explosão de DFA (`LogicSystem`) em delegates `by remember(...?. )` de editores grandes** | **Confirmada (causa primária)** | Varredura nos 63 arquivos `.kt` (37.372 linhas) revelou **122 delegates `by` contendo 214 safe calls (`?.`)**, concentrados em 4 telas de edição: **`EntityEditors.kt`** (42 delegates / 84 `?.`, com `TaskEditorSheet` chegando a **1.409 linhas**, **140 `?.`**, **53 `remember(...)`** e **41 delegates `by`** — onde o Lote 6 adicionou 15 chamadas `remember(task?.id, activeRecurrence?.id, activeRecurrence?.updatedAt)` sobre `task = existingTask?.task` e `activeRecurrence = existingTask?.recurrence?.takeIf { it.isActive }`), **`FinanceEditors.kt`** (36 delegates / 55 `?.`), **`EventsScreen.kt`** (27 delegates / 44 `?.` em `EventEditorSheet` de 761 linhas e 97 `?.`) e **`PlanningGoalsScreen.kt`** (11 delegates / 20 `?.`). |
| **H2. Configuração insuficiente/desbalanceada de memória para o Kotlin Daemon** | **Confirmada (fator agravante direto)** | `/gradle.properties` definia apenas `org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8`, sem `kotlin.daemon.jvmargs` explícito. Assim, o Kotlin Daemon executava limitado a 2 GB de heap para compilar Kotlin `2.1.0` + plugin Compose + `material-icons-extended` + saída KSP do Room v6 (27 tabelas) + arquivos grandes (`NexusRepository.kt` com 4.913 linhas, `EntityEditors.kt` com 2.378 linhas). |
| **H3. Falha ou sobrecarga no KSP / Room** | **Descartada** | No log do Actions, `> Task :app:kspDebugKotlin` terminou normalmente às `09:36:20` antes da falha em `:app:compileDebugKotlin`. |
| **H4. Código incompleto / dependências de fases futuras / imports quebrados** | **Descartada** | Verificação estática em todos os 63 `.kt` encontrou **0** ocorrências de `TODO(`, `FIXME` ou `NotImplementedError`. Todos os imports internos `com.nexus.app.*` resolvem para símbolos existentes (apenas `R` e `BuildConfig` são gerados em build, com `buildConfig = true` habilitado em `app/build.gradle.kts`). Todas as assinaturas do Lote 6 entre `MainActivity.kt`, `AgendaScreen.kt`, `EntityEditors.kt`, `NexusViewModel.kt` e `NexusRepository.kt` estão alinhadas. |
| **H5. Regressão por fluxos `combine` no repositório/ViewModel** | **Descartada como causa do stack trace** | `NexusRepository.kt` possui 38 chamadas `combine(`, mas com 0 delegates `by`, em blocos curtos (3 a 5 fluxos) com parâmetros explícitos; além disso, o stack trace aponta explicitamente `transformDelegateExpression`, presente apenas na camada de UI (`by remember(...)` / `by lazy`). |

---

## B. Alterações realizadas

Todas as funcionalidades, regras de negócio, fluxos de persistência, validação de alterações não salvas (`hasUnsavedChanges`) e chaves de recomposição do `remember(...)` foram preservadas integralmente.

### 1. `/gradle.properties`
- **O que foi alterado:** ajustado `org.gradle.jvmargs` para `-Xmx1536m -XX:MaxMetaspaceSize=512m -Dfile.encoding=UTF-8` e adicionado `kotlin.daemon.jvmargs=-Xmx3072m -XX:MaxMetaspaceSize=768m -Dfile.encoding=UTF-8`.
- **Por que foi necessária:** sem `kotlin.daemon.jvmargs`, o Kotlin Daemon ficava restrito ao mesmo limite de 2 GB do Gradle (`2048m`), insuficiente para análise K2 + IR do Compose em projetos deste porte.
- **Problema que pretende resolver:** dar 3 GB dedicados ao compilador Kotlin com teto explícito de Metaspace, mantendo o teto somado de Gradle (1,5 GB) + Kotlin Daemon (3 GB) ~4,5 GB de heap máximo — compatível com runners padrão `ubuntu-latest` do GitHub Actions (mínimo de 7 GB de RAM / 16 GB nos runners atuais), evitando `OutOfMemoryError` do processo Linux.
- **Possíveis impactos:** uso máximo de ~5–5,5 GB de RAM somados durante `:app:compileDebugKotlin`; não altera comportamento do app em runtime.

### 2. `/app/src/main/java/com/nexus/app/ui/screens/EntityEditors.kt`
- **O que foi alterado:**
  - Adicionados os imports `TaskEntity` e `TaskRecurrenceEntity`.
  - Criados `TaskEditorInitialState` e a função pura `buildTaskEditorInitialState(...)` para extrair de uma só vez os valores iniciais da tarefa, subtarefas, lembrete e recorrência em um único `remember(...)`.
  - Em `TaskEditorSheet`, pré-extraídas as chaves simples `taskKey`, `activeRecurrenceId`, `activeRecurrenceUpdatedAt`, `reminderKey`, `reminderMinutesKey` e `reminderStatusKey`, removendo todos os `?.` de dentro dos argumentos dos `remember(...)` e dos `by remember(...)` (inclusive nos campos de recorrência do Lote 6) e simplificando `recurrenceRuleChanged`, `templateFieldsChanged` e `hasUnsavedChanges` para comparar contra `init.*`.
  - Decomposta a seção expansível de recorrência (`"Mais detalhes"`) para o composable privado `TaskRecurrenceSectionCard(...)` e os diálogos de escopo/exclusão recorrente para `TaskScopeConfirmDialogs(...)`, reduzindo o tamanho do corpo principal de `TaskEditorSheet`.
  - Em `AppointmentEditorSheet` e `ProjectEditorSheet`, pré-extraídas chaves e valores iniciais (`appointmentId`, `reminderId`, `projectIdKey`, `init*`) fora das expressões `by remember(...)` e de `hasUnsavedChanges`.
- **Por que foi necessária:** `TaskEditorSheet` concentrava 41 delegates `by` e 140 `?.` em uma função de ~1.409 linhas, sendo o ponto mais crítico para o padrão `transformDelegateExpression -> transformSafeCallExpression -> LogicSystem.addImplication` visto no log.
- **Problema que pretende resolver:** eliminar o acúmulo exponencial de implicações de nulabilidade no `FirDataFlowAnalyzer` dentro de `TaskEditorSheet`, `AppointmentEditorSheet` e `ProjectEditorSheet`, além de reduzir o tamanho por função analisada pelo compilador Compose/FIR.
- **Possíveis impactos:** comportamento idêntico em runtime (mesmas chaves de `remember`, mesmos estados iniciais, mesma detecção de dirty state e mesmos fluxos de salvamento/exclusão recorrente).

### 3. `/app/src/main/java/com/nexus/app/ui/screens/EventsScreen.kt`
- **O que foi alterado:**
  - Adicionado import `EventEntity`.
  - Criados `EventEditorInitialState` e `buildEventEditorInitialState(...)`.
  - Em `EventEditorSheet`, pré-extraídas as chaves simples `evId`, `reminderId`, `reminderMinutesKey`, `reminderStatusKey` e um único `val init = remember(...) { ... }`, removendo os 27 delegates `by remember(ev?.id)` com safe calls e os 15 `?.` da cadeia de `hasUnsavedChanges`.
- **Por que foi necessária:** `EventEditorSheet` era o terceiro maior concentrador de delegates com safe calls (27 delegates / 44 `?.` e 97 `?.` totais na função).
- **Problema que pretende resolver:** impedir que `EventEditorSheet` também provoque explosão de `FirDataFlowAnalyzer` após a correção de `TaskEditorSheet`.
- **Possíveis impactos:** nenhum impacto funcional; preserva exatamente as mesmas chaves (`evId`, `reminderId`) e inicializações.

### 4. `/app/src/main/java/com/nexus/app/ui/screens/FinanceEditors.kt`
- **O que foi alterado:**
  - Adicionado import `TransactionEntity`.
  - Criados `TransactionEditorInitialState` e `buildTransactionEditorInitialState(...)` para `TransactionEditorSheet`, removendo safe calls dos argumentos e corpos de `by remember(...)` e de `hasUnsavedChanges`.
  - Pré-extraídas chaves (`cardIdKey`, `loanIdKey`, `goalIdKey`) e valores iniciais (`init*`) em `CreditCardEditorSheet`, `LoanEditorSheet` e `FinancialGoalEditorDialog`.
- **Por que foi necessária:** `FinanceEditors.kt` era o segundo arquivo com mais delegates contendo safe calls (36 delegates / 55 `?.`).
- **Problema que pretende resolver:** zerar a incidência de `FirSafeCallExpression` dentro de `transformDelegateExpression` em todo o módulo de editores financeiros.
- **Possíveis impactos:** comportamento idêntico de edição, validação e descarte de alterações nos formulários financeiros.

### 5. `/app/src/main/java/com/nexus/app/ui/screens/PlanningGoalsScreen.kt`
- **O que foi alterado:** pré-extraídas chaves (`goalIdKey`, `milestoneIdKey`) e valores iniciais (`init*`) em `PersonalGoalEditorDialog` e `MilestoneEditorDialog`, removendo os 11 delegates `by remember(existing?.id)` com safe calls.
- **Por que foi necessária:** eliminar o mesmo padrão nos formulários de Objetivos Pessoais e Marcos.
- **Problema que pretende resolver:** manter consistência preventiva contra o bug de DFA do K2 em todos os editores do app.
- **Possíveis impactos:** nenhum impacto funcional.

> **Resultado da varredura após as edições:** o projeto caiu de **122 delegates `by` com 214 `?.`** para apenas **6 ocorrências triviais** (em dialogs pequenos: `QuoteEditorDialog` com 3, `CategoryEditorDialog` com 2 e `HomeScreen` com 1), e todas as 4 telas modificadas estão com parênteses e chaves balanceados.

---

## C. Problemas ainda existentes

1. **Ausência de compilação real neste ambiente:** como este ambiente não dispõe de JDK, Android SDK nem Gradle, `:app:compileDebugKotlin`, `:app:testDebugUnitTest` e `:app:assembleDebug` ainda não foram executados após as correções.
2. **Arquivos extensos remanescentes (risco futuro de manutenção, não impeditivo imediato):**
   - `NexusRepository.kt` (4.913 linhas), `FinanceScreen.kt` (1.870 linhas), `MoreSettingsBackupDiagnosticsScreens.kt` (1.635 linhas), `NexusViewModel.kt` (1.521 linhas) e `MainActivity.kt` (`NexusAppRoot` com 1.113 linhas) continuam grandes em extensão total, embora **não** apresentem cadeias massivas de `?.` dentro de `by` delegates. Caso futuras fases adicionem novos módulos grandes (ex.: Lote 7 — Mercado), recomenda-se dividir por responsabilidade em arquivos separados (ex.: repositórios/telas por domínio).
3. **Observação sobre pasta de documentação:** o repositório existente utiliza `/docs` em minúsculas (contendo `NEXUS_SPEC.md`, `NEXUS_CURRENT_STATE.md`, etc.); este documento foi salvo em `docs/DIAGNOSTICO_COMPILACAO.md` para manter a convenção real do projeto em sistema de arquivos case-sensitive.

---

## D. Dependências de fases futuras

- **Não há dependência de fase futura impedindo a compilação no estágio atual.**
  - Os Lotes 1 a 6 previstos em `NEXUS_UPDATE_01_CONTROL_CENTER.md` e `lista.md` estão estruturalmente completos no código (Room v6 com 27 tabelas, entidades, DAOs, migrações, recorrência de tarefas, `AgendaEngine`, backup v6, notificações, widgets e testes unitários).
  - O **Lote 7 (Mercado)** ainda não foi iniciado e não há referências pendentes a ele no código atual.
- **Quando compilar novamente:** o projeto já pode ser submetido imediatamente a uma nova execução do workflow `.github/workflows/android-build.yml` no GitHub Actions.

---

## E. Situação final

**Classificação escolhida:** **1. Pronto para nova tentativa de compilação** *(ressalvando-se que a validação real depende da execução no GitHub Actions, pois este ambiente não possui toolchain Android/JVM).*

**Justificativa:**  
A falha em `:app:compileDebugKotlin` teve causa raiz clara e comprovada tanto pelo stack trace (`transformDelegateExpression` -> `transformSafeCallExpression` -> `FirDataFlowAnalyzer.exitSafeCall` -> `LogicSystem.addImplication` -> `OutOfMemoryError`) quanto pela análise quantitativa do código-fonte e do `/gradle.properties`. As duas frentes foram corrigidas de forma cirúrgica e sem perda de funcionalidades: (1) eliminação do padrão disparador de explosão no `FirDataFlowAnalyzer` em todos os editores grandes (`EntityEditors.kt`, `EventsScreen.kt`, `FinanceEditors.kt`, `PlanningGoalsScreen.kt`) com decomposição de `TaskEditorSheet`, e (2) dimensionamento explícito e seguro de memória em `/gradle.properties` (`kotlin.daemon.jvmargs=-Xmx3072m` + `org.gradle.jvmargs=-Xmx1536m`). Além disso, não há pendências estruturais, imports quebrados ou código inacabado bloqueando o build.
