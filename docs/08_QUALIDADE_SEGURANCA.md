# 08 — Qualidade, Segurança, Confiabilidade e Testes

> **Base verificada:** `AndroidManifest.xml`, `app/build.gradle.kts`, `keystore.properties.example`, `GlobalErrorHandler.kt`, `BackupStorageGateway.kt`, `BackupMergeEngine.kt`, `BackupManager.kt`, `NexusRepository.kt`, `FileMarketAlertStore.kt`, `MarketQuoteRemoteDataSource.kt`, `MarketHistoryRemoteDataSource.kt`, `.github/workflows/android-build.yml`, `MoreSettingsBackupDiagnosticsScreens.kt` e os `16` arquivos em `app/src/test/java/com/nexus/app/`.

---

## 1. Postura de Segurança e Privacidade (Offline-First)

1. **Soberania local dos dados:** todo dado pessoal, financeiro e de produtividade reside exclusivamente no aparelho (`nexus_local.db`, `SharedPreferences` privadas e `filesDir`).
2. **Zero backend / zero login / zero telemetria:** não há conta de usuário, Firebase, analytics, rastreamento ou envio de transações/tarefas pela rede.
3. **`android:allowBackup="false"` (`AndroidManifest.xml:13`):** bloqueia backup automático não controlado via nuvem do Google Drive; backups são iniciados apenas pelo usuário via **SAF** (`ActivityResultContracts.CreateDocument` / `OpenDocument`).
4. **Sem segredos embutidos ou trafegados:**
   - Provedores de Mercado utilizam exclusivamente APIs públicas sem necessidade de API key pessoal obrigatória.
   - `NetworkSecurity`: conexões do Mercado usam HTTPS (`HttpsURLConnection`) com timeouts explícitos e leitura somente-pública de ativos financeiros (nenhum dado do usuário é enviado).
5. **Privacidade em erros:** `GlobalErrorHandler` e `DiagnosticsScreen` gravam apenas localmente em `error_logs` (máx. últimos 50 exibidos na UI, truncando `stackTrace` em `4000` chars), sem envio remoto.

---

## 2. Permissões Declaradas (`AndroidManifest.xml`)

| Permissão | Tipo | Finalidade Exclusiva | Degradação se Negada |
| --- | --- | --- | --- |
| `INTERNET` | Normal | Consultas HTTPS do Módulo Mercado (cotações e séries históricas) | Núcleo do NEXUS opera 100%; Mercado mantém cache local |
| `ACCESS_NETWORK_STATE` | Normal | Verificação prévia de conectividade e restrição `NetworkType.CONNECTED` no `MarketQuoteSyncWorker` | Mercado evita tentativas desnecessárias e exibe status offline |
| `POST_NOTIFICATIONS` | Runtime (Android 13+) | Entrega de lembretes, briefings e alertas de preço de mercado | Lembretes e alertas são registrados/avaliados localmente sem postar na bandeja do SO |
| `RECEIVE_BOOT_COMPLETED` | Normal | `NexusBootReceiver` restaura alarmes, workers e widgets após boot / troca de data/fuso / atualização do pacote | Reconciliado apenas na próxima abertura do app ou ciclo do `WorkManager` |
| `SCHEDULE_EXACT_ALARM` | Especial (Android 12+) | `AlarmManager.setExactAndAllowWhileIdle` para lembretes pontuais | Faz fallback automático para `setAndAllowWhileIdle` quando `!canScheduleExactAlarms()` |

---

## 3. Integridade Transacional, Falhas e Limites Físicos Conhecidos

### 3.1 Room SQLite
- **Transações atômicas (`database.withTransaction`):** criação de parcelamentos, quitação de faturas (`payCreditCardStatement`), conclusão de tarefa recorrente com geração da próxima ocorrência (`completeTask`), restauração completa (`restoreBackupAtomically`) e mesclagem incremental (`mergeBackupAtomically`) executam em bloco transacional único.
- **Proteção contra remoção de dados:** `fallbackToDestructiveMigration()` é estritamente proibido; toda evolução de schema usa `Migration` aditiva (`MIGRATION_1_2` a `MIGRATION_5_6`). Na mesclagem incremental (`BackupMergeEngine`), nenhuma tabela recebe `deleteAll`, singletons locais e conflitos por ID/chave `UNIQUE` preservam os dados do aparelho, e quebras de FK obrigatória rejeitam apenas o registro problemático enquanto FKs opcionais são zeradas com `NEEDS_REVIEW`.
- **Sanitização de backup:** `BackupManager` (Modos 1 e 2) remove referências órfãs contra pais existentes e aplica fallbacks seguros em enums desconhecidos, aceitando JSONs `v1` a `v7` (`validateForMerge` bloqueia arquivos futuros incompatíveis quando `formatVersion > CURRENT_FORMAT_VERSION` ou `databaseVersion > DATABASE_VERSION`).

### 3.2 Fronteira Transacional Room × Stores do Mercado (Backup v7)
- No `BackupManager.restoreAtomically` (substituição) e no `BackupManager.mergeBackupAtomically` (união incremental):
  - A validação de `marketConfig` ocorre antes da escrita.
  - `applyMarketConfigWithRollback` executa ao final de `database.withTransaction`, capturando snapshots prévios de favoritos e alertas.
  - Se a escrita de favoritos ou alertas falhar em memória/store, a exceção aborta o `withTransaction` do Room e restaura o snapshot do Mercado (`rollbackMarketConfig`).
  - Se o commit final do SQLite falhar no `catch` externo, `rollbackMarketConfig` também reverte favoritos e alertas.
- **Limite físico documentado:** `SharedPreferencesMarketWatchlistStore` usa `.apply()` assíncrono e `FileMarketAlertStore` grava JSON via `.tmp` + `renameTo` (além de encapsular `runCatching` no uso cotidiano em workers). Sem commit distribuído XA/2PC de sistema de arquivos, uma morte abrupta de processo (`SIGKILL`) exatamente no milissegundo entre o commit SQLite e o flush em disco do SO pode deixar os dois armazenamentos momentaneamente assíncronos — risco aceito na arquitetura atual com Mercado fora do Room (ver [`ADR-010`](./11_DECISOES_ADRS.md), [`ADR-013`](./11_DECISOES_ADRS.md) e [`ADR-016`](./11_DECISOES_ADRS.md)).

### 3.3 Captura de Erros e Diagnóstico
- **`GlobalErrorHandler` (`core/GlobalErrorHandler.kt`):** intercepta exceções não tratadas por thread, grava `ErrorLogEntity` via `runBlocking(Dispatchers.IO)` na tabela `error_logs` (protegido contra falha recursiva) e delega ao handler padrão do Android.
- **`DiagnosticsScreen` (`MoreSettingsBackupDiagnosticsScreens.kt:1275`):** exibe versão do app (`BuildConfig.VERSION_NAME` / `VERSION_CODE`), `NexusDatabase.DATABASE_VERSION`, contagem reativa em tempo real das `27` tabelas Room, últimos `50` registros de `error_logs`, cópia consolidada para área de transferência e botão de limpeza com confirmação.

---

## 4. Pipeline de CI (`GitHub Actions`)

Arquivo: `/.github/workflows/android-build.yml` (`NEXUS Android Build`)

- **Gatilhos:** `push` e `pull_request` nas branches `main` e `integracao-google`, além de `workflow_dispatch`.
- **Ambiente:** `ubuntu-latest`, Java `17` (Temurin), Android SDK (`platform-tools`), Gradle `8.10.2` via `gradle/actions/setup-gradle@v4`.
- **Comando executado:** `gradle :app:testDebugUnitTest :app:assembleDebug` (e opcionalmente `:app:assembleRelease` quando os secrets de release estiverem configurados).
- **Assinatura persistente e atualização in-place (`ADR-015`):**
  - `app/build.gradle.kts` suporta configuração externa opcional via `keystore.properties` (ignorado no `.gitignore`; modelo em `keystore.properties.example`) ou variáveis de ambiente (`NEXUS_DEBUG_KEYSTORE_*` / `NEXUS_KEYSTORE_*`) sem embutir segredos no código e mantendo o build `debug` funcional mesmo sem keystore configurada.
  - `.github/workflows/android-build.yml` suporta os secrets opcionais `NEXUS_DEBUG_KEYSTORE_BASE64` e `NEXUS_KEYSTORE_BASE64` (+ senhas/aliases) para emitir APKs com certificado consistente entre builds, eliminando `INSTALL_FAILED_UPDATE_INCOMPATIBLE`.
- **Artefato publicado:** `NEXUS-Android-APK` (`app/build/outputs/apk/debug/*.apk`, com `if-no-files-found: error`) e `NEXUS-Android-Release-APK` opcional quando configurado.
- **Lacunas atuais no CI / Repositório:**
  1. Os scripts `./gradlew` e `gradlew.bat` não estão na raiz (CI depende de `setup-gradle@v4`).
  2. `app/schemas/` não é comitado nem publicado como artefato de build ainda.
  3. Não há workflow nem etapa de testes instrumentados (`connectedDebugAndroidTest`), e o mantenedor ainda precisa provisionar o keystore persistente nos GitHub Secrets / `keystore.properties` locais para ativar a assinatura consistente entre releases.

---

## 5. Cobertura Atual de Testes (`16` Arquivos JUnit, `122` `@Test`)

> **Aviso de honestidade:** todos os `16` arquivos existem em `app/src/test/java/com/nexus/app/`, totalizando **122** casos `@Test` em JUnit 4 puro (JVM). Neste container sem JDK/Gradle/Android SDK, **nenhum deles foi compilado ou executado localmente**.

| Arquivo de Teste | `@Test` | Escopo Principal Coberto |
| --- | --- | --- |
| `NexusPhase1UnitTests.kt` | `4` | Conflitos de compromissos, `isTaskOverdue`, fallbacks de enum e tolerância de backup |
| `NexusPhase2UnitTests.kt` | `4` | `parseBrlToCents`, `advanceDateByFrequency`, `effectiveTransactionStatus`, backup `v1→v2` |
| `NexusPhase3And4UnitTests.kt` | `6` | Gatilho de lembrete, horário de silêncio, próximo horário de rotina, faturas, limite de cartão, backup `v2/v3` |
| `NexusPhase5WidgetUnitTests.kt` | `10` | Snapshots e estados vazio/erro dos 4 widgets Glance, tamanhos responsivos e ações pós-restauração |
| `NexusPhase6UnitTests.kt` | `13` | Blocos do Dashboard, JSON corrompido, saudação/datas, rotação de frases, objetivos `AUTO`/`MANUAL` e backup `v4` (`CURRENT_FORMAT_VERSION`) |
| `NexusStage1UnitTests.kt` | `14` | `resolveBackAction`, `DateTimeUtils` (`datePicker` UTC ↔ `America/Sao_Paulo`), resumo financeiro mensal, breakdown de cartão e `ReminderRules` |
| `NexusStage2UnitTests.kt` | `8` | `AgendaEngine.expand` (multi-dia, supressão `statementId != null`, `showPaid`, filtros, sugestão de categoria) e round-trip Backup `v5` |
| `NexusStage3FinanceUnitTests.kt` | `3` | `FinanceRules.computeFinanceMonthSummary` (rosca top 6 + Outros, fluxo 6 meses Realizado/Previsto, previsão 30d, orçamentos) |
| `NexusLote6RecurrenceUnitTests.kt` | `6` | `RecurrenceEngine` (semanal/quinzenal/mensal dia 31/fim por data/contagem), exceções `SKIP`/`OVERRIDE` e projeção virtual na `AgendaEngine` |
| `MarketQuoteUnitTests.kt` | `17` | Catálogo de 12 ativos, parsing de AwesomeAPI/PTAX/CoinGecko/CoinPaprika/B3/Brapi/CNBC/Nasdaq, cooldown/cache offline e conversão USD→BRL |
| `MarketHistoryUnitTests.kt` | `9` | Regras de intervalos (`1D` omitido para moedas), parsers históricos por provedor, cache e cálculo de variação/extremos |
| `MarketPresentationUnitTests.kt` | `4` | `MarketPresentationMapper`: formatação por unidade (`BRL`/`USD`/`pts`), severidade de status e badges de fonte/atraso |
| `MarketWatchlistUnitTests.kt` | `3` | Adição/remoção, deduplicação preservando ordem e validação via `MarketAssetCatalog` |
| `MarketAlertUnitTests.kt` | `5` | `MarketAlertRules`: avaliação acima/abaixo, prevenção de duplicidade (`awaitingConditionReset`, timestamp), rearme e validação de moeda/unidade |
| `MarketBackupIntegrationUnitTests.kt` | `5` | Exportação/restauração Backup `v7` com `marketConfig`, retrocompatibilidade `null` vs `[]`, sanitização de alertas inválidos e rollback Room ↔ Mercado |
| `BackupIncrementalMergeUnitTests.kt` | `11` | Suíte transacional (`InMemoryBackupStorageGateway`) cobrindo os 20 cenários de Modo 1 vs Modo 2: mesclagem idempotente sem `deleteAll`, preservação de conflitos/singletons, ordem topológica, FKs obrigatórias vs opcionais (`NEEDS_REVIEW`), coerência cartão/fatura/parcela/recorrência e bloqueio anti-dupla despesa, versões `v1..v7`/incompatíveis e rollback Room ↔ Mercado |

### Lacunas de Teste Identificadas
- **`0` testes em `app/src/androidTest/`:** não há testes instrumentados de:
  - Migrações reais do SQLite Room (`1→2→3→4→5→6`) com `MigrationTestHelper`;
  - UI Jetpack Compose (fluxos de editores, `BackHandler`, gráficos `Canvas`, filtros da Agenda);
  - `SAF` real (leitura/escrita `.json`, `.csv`, `.txt`), `AlarmManager` em Doze, `NexusBootReceiver`, notificações + deep-links em Android 13+ e widgets Glance reais no launcher;
  - Integração HTTP real contra provedores públicos do Mercado.
