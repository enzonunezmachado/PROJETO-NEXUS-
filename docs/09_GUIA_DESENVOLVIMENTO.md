# 09 — Guia de Desenvolvimento, Convenções e Fluxo de Lotes

> **Finalidade:** reunir todas as instruções práticas para configurar ambiente, evitar regressões conhecidas (inclusive o bug de OOM do compilador Kotlin K2), seguir os padrões arquiteturais do projeto e manter `/docs/` e os artefatos do AI Studio sincronizados a cada entrega.

---

## 1. Ambiente de Desenvolvimento e Build

### Requisitos Recomendados (Local ou CI)
- **JDK:** Java `17` (Temurin ou compatível).
- **Gradle:** `8.10.2` (ver `gradle/wrapper/gradle-wrapper.properties`).
- **Android SDK:** `compileSdk = 35`, `targetSdk = 35`, `minSdk = 26`, `platform-tools`.
- **Memória (`gradle.properties`):** já calibrada para evitar `OutOfMemoryError` no K2 FIR:
  ```properties
  org.gradle.jvmargs=-Xmx1536m -XX:MaxMetaspaceSize=512m -Dfile.encoding=UTF-8
  kotlin.daemon.jvmargs=-Xmx3072m -XX:MaxMetaspaceSize=768m -Dfile.encoding=UTF-8
  ```

### Comandos Oficiais de Build e Teste
> Como `./gradlew` e `gradlew.bat` não estão na raiz do repositório hoje, utilize `gradle` instalado no PATH (como faz `.github/workflows/android-build.yml`) ou crie/regenere o wrapper com `gradle wrapper --gradle-version 8.10.2` se desejar trabalhar offline em máquina local:

```bash
gradle :app:testDebugUnitTest :app:assembleDebug
```

> **Nota sobre raiz mista:** arquivos como `package.json`, `vite.config.ts`, `src/` e `index.html` pertencem apenas ao wrapper Web/iFrame do ambiente Google AI Studio. **Nunca** confunda `npm run build` / `vite` com compilação do aplicativo Android NEXUS.

---

## 2. Regra Crítica de Compilador: Prevenção de OOM no Kotlin `2.1.0` (K2 FIR)

Em `17/11/2026` (documentado em [`archive/DIAGNOSTICO_COMPILACAO_2026-11-17.md`](./archive/DIAGNOSTICO_COMPILACAO_2026-11-17.md)), `:app:compileDebugKotlin` falhou com `OutOfMemoryError: GC overhead limit exceeded` no `FirDataFlowAnalyzer` (`LogicSystem.addImplication`).

**Padrão proibido em telas e formulários Compose:**
- **NÃO escrever** sequências extensas de `var x by remember(obj?.id, sub?.id, sub?.updatedAt) { mutableStateOf(obj?.campo ?: ...) }` dentro de composables grandes. Cada `?.` encadeado dentro de argumentos/corpos de `by remember(...)` multiplica exponencialmente as implicações de nulabilidade no K2 `2.1.0`.

**Padrão obrigatório adotado em `EntityEditors.kt`, `FinanceEditors.kt`, `EventsScreen.kt` e `PlanningGoalsScreen.kt`:**
1. Pré-extrair chaves simples no topo do composable (`val taskKey = existingTask?.task?.id`).
2. Construir uma `data class *EditorInitialState` e uma função pura `build*InitialState(...)` invocada em **um único** `val init = remember(taskKey, ...) { ... }`.
3. Usar `by remember(taskKey) { mutableStateOf(init.campo) }` sem operadores `?.` nos delegates.
4. Decompor seções grandes (como `TaskRecurrenceSectionCard`, `TaskScopeConfirmDialogs`) em composables privados menores.

---

## 3. Regras Permanentes de Arquitetura e Código

1. **Incrementalidade obrigatória:** nunca reescrever o app nem remover módulos funcionais existentes.
2. **Offline-first verdadeiro:** nenhuma tela principal pode bloquear esperando rede; dados de negócio vêm de `Room` (`Flow`) ou cache local.
3. **Sem backend, login ou chaves secretas:** não introduzir Firebase, backend proprietário ou API keys embutidas no código/repositório.
4. **Proibido dados inventados:** nunca gerar cotações falsas, frases embutidas fictícias, lançamentos ou taxas automáticas.
5. **Regras puras em `domain/rules/` ou modelos de domínio puros:**
   - Cálculos de agenda vão em `AgendaEngine`.
   - Cálculos financeiros vão em `FinanceRules`.
   - Cálculos de recorrência vão em `RecurrenceEngine`.
   - Cálculos de lembretes vão em `ReminderRules` e datas em `DateTimeUtils` (`America/Sao_Paulo`).
   - Telas Compose e `NexusRepository` consomem essas funções puras em vez de duplicar regras inline.
6. **Não engordar `NexusRepository` nem `NexusViewModel`:** novos domínios devem residir em arquivos dedicados por domínio (como `data/market/*` e `ui/market/*`).
7. **Sem hex solto em UI:** toda cor deve vir de `LocalNexusPalette.current` (`NexusPalette`), exceto `category.colorHex` personalizada pelo usuário com `parseHexColorOrDefault`.
8. **Banco Room e Backup:**
   - Proibido `fallbackToDestructiveMigration()`.
   - Toda alteração de schema exige incremento de `NexusDatabase.DATABASE_VERSION`, nova `Migration` aditiva em `NexusMigrations.kt` (colunas anuláveis ou com `DEFAULT`, ou novas tabelas), atualização de `BackupModels` (`CURRENT_FORMAT_VERSION`), `BackupManager` e `DiagnosticsScreen`.
   - Manter retrocompatibilidade com backups `v1..vN` (`ignoreUnknownKeys = true` e defaults seguros).
9. **Versão do aplicativo:** planejar incrementos coerentes de `versionCode` e `versionName` em `app/build.gradle.kts` ao fechar entregas.
10. **Verdade técnica:** nunca declarar que Gradle, KSP, testes ou APK passaram sem evidência de execução real (local ou GitHub Actions).

---

## 4. Convenções de Nomenclatura e Organização

| Elemento | Convenção Adotada |
| --- | --- |
| Pacote raiz | `com.nexus.app` |
| Entidades Room | `*Entity` (`@Entity(tableName = "snake_case")`) em `data/db/entity/` |
| DAOs | `*Dao` em `data/db/dao/NexusDaos.kt` |
| DTOs de UI / Domínio | Modelos compostos `*WithDetails` / `*Snapshot` / `*Summary` em `domain/model/` |
| DTOs de Backup | `*BackupDto` (`@Serializable`) em `data/backup/BackupModels.kt` |
| Regras puras | `object *Rules` / `object *Engine` em `domain/rules/` (sem `android.*` sempre que possível, para rodar rápido no `:app:testDebugUnitTest`) |
| Testes unitários JVM | `app/src/test/java/com/nexus/app/*UnitTests.kt` (JUnit 4) |

---

## 5. Checklist Obrigatório por Lote de Implementação e Sincronização Documental (Seção 8)

Todo novo lote de implementação no NEXUS só é considerado concluído quando cumprir **ambas as partes** abaixo:

### A. Checklist Técnico do Lote
1. Escopo claro e incremental (sem alterações paralelas não solicitadas).
2. Imports, assinaturas, tipos e chamadores conferidos ponta a ponta (`DAO → Repository → ViewModel → Screen → Widget/Worker`).
3. Caso tenha havido alteração de schema ou persistência: `Migration` aditiva + atualização de `BackupModels` + `BackupManager` + `DiagnosticsScreen` + teste de retrocompatibilidade.
4. Novos fluxos críticos acompanhados de testes JUnit em `app/src/test/java/com/nexus/app/`.
5. Execução de `gradle :app:testDebugUnitTest :app:assembleDebug` (local ou via `.github/workflows/android-build.yml`) com publicação do APK validada — ou declaração explícita do que ficou `NÃO VERIFICADO` se executado em container sem Android SDK.

### B. Checklist Obrigatório de Atualização Documental + Artefatos AI Studio
Sempre atualizar **no mesmo fechamento do lote**, sem criar cópias paralelas nem nomes vagos:

1. **Documentos `/docs/` do repositório ativo (fonte portátil):**
   - `docs/07_ESTADO_ATUAL.md`: atualizar baseline (`versionCode`, `versionName`, `DATABASE_VERSION`, `CURRENT_FORMAT_VERSION`), tabela de status e itens `IMPLEMENTADA` / `PARCIAL` / `INCONSISTENTE` / `NÃO VERIFICADO`.
   - `docs/10_ROADMAP.md`: mover itens concluídos de `Planejado` / `Dívida` para `Concluído` com evidência real.
   - `docs/02_ESPECIFICACAO_PRODUTO.md`, `docs/04_MODELO_DADOS.md`, `docs/05_FLUXOS_COMPORTAMENTOS.md`, `docs/06_DESIGN_SYSTEM.md`, `docs/03_ARQUITETURA_ENGENHARIA.md` e `docs/08_QUALIDADE_SEGURANCA.md`: atualizar apenas as seções impactadas pelo lote.
   - `docs/11_DECISOES_ADRS.md`: registrar novo `ADR-0XX` caso o lote tome ou altere uma decisão arquitetural relevante.
   - `docs/12_RASTREABILIDADE.md`: atualizar vínculos de `REQ-*`, componentes, stores e suítes de teste.
   - `docs/README.md`: atualizar cabeçalho de baseline e data da última revisão se houver mudança de versão/schema.
2. **Sincronização com os Artefatos do Google AI Studio (quando operando no AI Studio):**
   - Atualizar **os mesmos artefatos existentes** (`NEXUS_CONTINUIDADE.md`, `NEXUS_CURRENT_STATE.md`, `NEXUS_PRODUCT_SPEC.md`, `NEXUS_DESIGN_SYSTEM.md`, `NEXUS_INTEGRATION_MAP.md`, `NEXUS_IMPLEMENTATION_ROADMAP.md`) preservando o histórico em `NEXUS_CONTINUIDADE.md` e garantindo zero contradição com `/docs/`.
   - **Nunca** criar arquivos duplicados ou cópias temporárias dentro ou fora de `/docs/`.
