# 08 — Qualidade, Segurança, Confiabilidade e Testes

> **Base verificada:** `AndroidManifest.xml`, `app/build.gradle.kts`, `keystore.properties.example`, `GlobalErrorHandler.kt`, `BackupStorageGateway.kt`, `BackupMergeEngine.kt`, `BackupManager.kt`, `data/ingestion/StructuredInput*.kt`, `data/inbox/*`, `NexusRepository.kt`, `FileMarketAlertStore.kt`, `MarketQuoteRemoteDataSource.kt`, `MarketHistoryRemoteDataSource.kt`, `.github/workflows/android-build.yml`, `MoreSettingsBackupDiagnosticsScreens.kt`, `NexusInboxScreen.kt` e os `21` arquivos em `app/src/test/java/com/nexus/app/`.

---

## 1. Postura de Segurança e Privacidade (Offline-First)

1. **Soberania local dos dados:** todo dado pessoal, financeiro e de produtividade reside exclusivamente no aparelho (`nexus_local.db`, `SharedPreferences` privadas e `filesDir`).
2. **Zero backend proprietário / zero login / zero telemetria:** não há conta de usuário NEXUS, Firebase, analytics ou rastreamento; o núcleo funciona integralmente offline.
3. **`android:allowBackup="false"` (`AndroidManifest.xml:13`):** bloqueia backup automático não controlado via nuvem do Google Drive; backups são iniciados apenas pelo usuário via **SAF** (`ActivityResultContracts.CreateDocument` / `OpenDocument`).
4. **Sem segredos embutidos no repositório:**
   - Provedores de Mercado utilizam exclusivamente APIs públicas sem necessidade de API key pessoal obrigatória.
   - `NetworkSecurity`: conexões do Mercado e da ponte opcional `NEXUS-INBOX` usam HTTPS (`HttpsURLConnection`) com timeouts explícitos e somente método `GET` (no caso da `NEXUS-INBOX`, com `instanceFollowRedirects = false`, checagem de host `https://api.github.com`, validação estrita de `owner`/`repo`/`branch`/`pathPrefix` contra path traversal e teto de `2 MB` por JSON).
   - O Fine-Grained PAT da `NEXUS-INBOX` é informado apenas pelo usuário em tempo de execução, armazenado criptografado localmente via **Android Keystore (`AES/GCM/NoPadding`)** (`AndroidKeystoreInboxTokenStore`), jamais registrado em logs, mensagens de erro, `audit_history` ou backups.
   - Nenhuma alteração recebida pela `NEXUS-INBOX` é aplicada automaticamente em segundo plano: é exigida prévia dry-run e confirmação explícita do usuário.
5. **Privacidade em erros:** `GlobalErrorHandler` e `DiagnosticsScreen` gravam apenas localmente em `error_logs` (máx. últimos 50 exibidos na UI, truncando `stackTrace` em `4000` chars), sem envio remoto.

---

## 2. Permissões Declaradas (`AndroidManifest.xml`)

| Permissão | Tipo | Finalidade Exclusiva | Degradação se Negada |
| --- | --- | --- | --- |
| `INTERNET` | Normal | Consultas HTTPS somente-leitura do Módulo Mercado e da ponte opcional `NEXUS-INBOX` (GitHub REST API `GET`) | Núcleo do NEXUS opera 100%; Mercado e Inbox mantêm dados/cache locais |
| `ACCESS_NETWORK_STATE` | Normal | Verificação prévia de conectividade e restrição `NetworkType.CONNECTED` no `MarketQuoteSyncWorker` e `InboxSyncWorker` | Evita tentativas sem rede e mantém estado offline seguro |
| `POST_NOTIFICATIONS` | Runtime (Android 13+) | Entrega de lembretes, briefings, alertas de preço de mercado e aviso opcional de novidades pendentes na `NEXUS-INBOX` | Lembretes, alertas e checagem seguem gravados/avaliados sem postar notificação |
| `RECEIVE_BOOT_COMPLETED` | Normal | `NexusBootReceiver` restaura alarmes, workers e widgets após boot / troca de data/fuso / atualização do pacote | Reconciliado apenas na próxima abertura do app ou ciclo do `WorkManager` |
| `SCHEDULE_EXACT_ALARM` | Especial (Android 12+) | `AlarmManager.setExactAndAllowWhileIdle` para lembretes pontuais | Faz fallback automático para `setAndAllowWhileIdle` quando `!canScheduleExactAlarms()` |

---

## 3. Integridade Transacional, Falhas e Limites Físicos Conhecidos

### 3.1 Room SQLite
- **Transações atômicas (`database.withTransaction`):** criação de parcelamentos, quitação de faturas (`payCreditCardStatement`), conclusão de tarefa recorrente com geração da próxima ocorrência (`completeTask`), edição/materialização sob demanda/exclusão de séries recorrentes (`Task` / `Appointment` / `Event`) com isolamento de `schedule_checklist_items` e versionamento não retroativo em `templateHistoryJson`, restauração completa (`restoreBackupAtomically`), mesclagem incremental (`mergeBackupAtomically`), **restauração de banco de fábrica (`restoreFactoryDatabase`)** e ingestão estruturada (`RoomStructuredInputGateway.inTransaction`, incluindo `RECURRENCE` e checklists) executam em bloco transacional único.
- **Proteção contra remoção de dados e prévia dry-run:** `fallbackToDestructiveMigration()` é estritamente proibido; toda evolução de schema usa `Migration` aditiva (`MIGRATION_1_2` a `MIGRATION_7_8`). Ocorrências virtuais ou materializadas sob demanda de Eventos recorrentes não criam obrigações financeiras automáticas (`TransactionEntity`), e edições/exclusões `"Toda a série"` preservam e desvinculam ocorrências com `ATTENDED` ou `TicketStatus.PAID` / transação `SETTLED`. Na mesclagem incremental (`BackupMergeEngine`), `previewMerge` reutiliza exatamente o mesmo `computeMergePlan` em modo `isDryRun = true` sem gravar no Room nem no Mercado; nenhuma tabela recebe `deleteAll`, singletons locais e conflitos por ID/chave `UNIQUE` preservam os dados do aparelho, e quebras de FK obrigatória rejeitam apenas o registro problemático enquanto FKs opcionais são zeradas com `NEEDS_REVIEW`. Na UI (`MoreSettingsBackupDiagnosticsScreens.kt`), a Restauração Completa exige diálogo próprio com checkbox explícito de confirmação destrutiva, e a **Restauração de Fábrica** exige dupla confirmação (com exportação SAF direta no 1º passo, checkbox obrigatório + digitação exata de `"APAGAR E RESTAURAR"` no 2º passo), mantendo ordenação fixa de locks (`InboxRepository.withExclusiveOperationLock` → `NexusRepository.databaseMutationMutex`) e preservando o PAT Keystore e coordenadas GitHub da `NEXUS-INBOX`.
- **Sanitização de backup e validação estrita de entrada estruturada:** `BackupManager` (Modos 1 e 2) remove referências órfãs contra pais existentes (incluindo `scheduleChecklistItems`) e aplica fallbacks seguros em enums desconhecidos, aceitando JSONs `v1` a `v9` com backfill de `templateSubtasksJson` para backups `<= v7` e de `checklistTemplateVersion`/`templateHistoryJson` para backups `<= v8` (`validateForMerge` bloqueia arquivos futuros incompatíveis quando `formatVersion > CURRENT_FORMAT_VERSION` ou `databaseVersion > DATABASE_VERSION`). Na camada interna `data/ingestion/`, `StructuredInputProcessor` impõe JSON estrito (`ignoreUnknownKeys = false`), validação de `contractVersion = 1` (cobrindo `8` entidades incluindo `RECURRENCE` e `checklist` em `APPOINTMENT`/`EVENT`), FKs reais, regras financeiras anti-dupla despesa/fatura `PAID`, preservação local em colisões e recibos idempotentes em `audit_history` (`STRUCTURED_INPUT_OP`) apenas para `APPLIED` / `UPDATED` / `CONFLICT_PRESERVED_LOCAL`, permitindo retentativa segura após `FAILED_ROLLED_BACK`.

### 3.2 Fronteira Transacional Room × Stores do Mercado / Inbox (Backup v9 & Factory Reset)
- No `BackupManager.restoreAtomically` (substituição), `BackupManager.mergeBackupAtomically` (união incremental) e `NexusRepository.restoreFactoryDatabase` (restauração de fábrica):
  - A validação de `marketConfig` ocorre antes da escrita (nos fluxos de backup).
  - `applyMarketConfigWithRollback` (ou limpeza de cotações/favoritos/alertas/histórico com rollback coordenado no reset de fábrica) executa dentro de `database.withTransaction`, capturando snapshots prévios de favoritos, alertas e dados locais de rastreamento da Inbox.
  - Se a escrita ou limpeza de stores externos falhar, a exceção aborta o `withTransaction` do Room e restaura os snapshots prévios (`rollbackMarketConfig` / `restoreTrackingSnapshot`).
  - Se o commit final do SQLite falhar no `catch` externo, o bloco de compensação também reverte os stores externos antes de relançar o erro.
- **Limite físico documentado:** `SharedPreferencesMarketWatchlistStore` usa `.apply()` assíncrono e `FileMarketAlertStore` grava JSON via `.tmp` + `renameTo` (além de encapsular `runCatching` no uso cotidiano em workers). Sem commit distribuído XA/2PC de sistema de arquivos, uma morte abrupta de processo (`SIGKILL`) exatamente no milissegundo entre o commit SQLite e o flush em disco do SO pode deixar os dois armazenamentos momentaneamente assíncronos — risco aceito na arquitetura atual com Mercado fora do Room (ver [`ADR-010`](./11_DECISOES_ADRS.md), [`ADR-013`](./11_DECISOES_ADRS.md), [`ADR-016`](./11_DECISOES_ADRS.md) e [`ADR-020`](./11_DECISOES_ADRS.md)).

### 3.3 Captura de Erros e Diagnóstico
- **`GlobalErrorHandler` (`core/GlobalErrorHandler.kt`):** intercepta exceções não tratadas por thread, grava `ErrorLogEntity` via `runBlocking(Dispatchers.IO)` na tabela `error_logs` (protegido contra falha recursiva) e delega ao handler padrão do Android.
- **`DiagnosticsScreen` (`MoreSettingsBackupDiagnosticsScreens.kt`):** exibe versão do app (`BuildConfig.VERSION_NAME` / `VERSION_CODE`), `NexusDatabase.DATABASE_VERSION`, contagem reativa em tempo real das `28` tabelas Room (incluindo `schedule_checklist_items`), últimos `50` registros de `error_logs`, cópia consolidada para área de transferência e botão de limpeza com confirmação. A entrada para **Restaurar banco de dados de fábrica** reside em **Configurações → Dados e armazenamento**.

---

## 4. Pipeline de CI (`GitHub Actions`)

Arquivo: `/.github/workflows/android-build.yml` (`NEXUS Android Build`)

- **Gatilhos:** `push` e `pull_request` nas branches `main` e `integracao-google`, além de `workflow_dispatch`.
- **Ambiente:** `ubuntu-latest`, Java `17` (Temurin), Android SDK inicializado via `android-actions/setup-android@v4` (`platform-tools`) seguido de `sdkmanager "platform-tools" "build-tools;35.0.0"` com argumentos independentes e validação explícita de `$ANDROID_HOME/platform-tools/adb` e `$ANDROID_HOME/build-tools/35.0.0/apksigner` (corrigindo falha `#45733193619` causada por `packages` multilinha), Gradle `8.10.2` via `gradle/actions/setup-gradle@v4`.
- **Comandos executados:** `gradle :app:testDebugUnitTest :app:assembleDebug` e, quando os 4 secrets `NEXUS_KEYSTORE_*` estiverem configurados, `gradle :app:assembleRelease`.
- **Endurecimento de assinatura persistente e atualização in-place (`ADR-015`):**
  - `app/build.gradle.kts` suporta configuração externa opcional via `keystore.properties` (ignorado no `.gitignore`; modelo em `keystore.properties.example`) ou variáveis de ambiente (`NEXUS_DEBUG_KEYSTORE_*` / `NEXUS_KEYSTORE_*`), **falhando imediatamente (`GradleException`)** em configurações parciais (`1..3` de `4` campos) ou arquivos ausentes, além de bloquear `assembleRelease`/`bundleRelease`/`packageRelease` sem configuração `release` válida. Quando nenhum campo Debug é informado, mantém o keystore Debug local funcional.
  - `.github/workflows/android-build.yml` valida previamente `NEXUS_DEBUG_KEYSTORE_*` e `NEXUS_KEYSTORE_*` via `base64 -d` + `keytool -list -v` (com senha via `-storepass:env`, sem vazar segredos em logs), extrai o SHA-256 esperado e, após o build, executa `apksigner verify --verbose --print-certs` (priorizando `build-tools/35.0.0/apksigner`) comparando o certificado real de cada APK contra o SHA-256 esperado do keystore.
  - Emite relatório público (sem segredos) no `$GITHUB_STEP_SUMMARY` e no artefato `NEXUS-Signing-Verification-Report`.
- **Artefatos publicados:** `NEXUS-Android-APK` (`app/build/outputs/apk/debug/*.apk`, `if-no-files-found: error`), `NEXUS-Android-Release-APK` (condicional quando configurado) e `NEXUS-Signing-Verification-Report`.
- **Lacunas atuais no CI / Repositório:**
  1. Os scripts `./gradlew` e `gradlew.bat` não estão na raiz (CI depende de `setup-gradle@v4`).
  2. `app/schemas/` não é comitado nem publicado como artefato de build ainda.
  3. Não há workflow nem etapa de testes instrumentados (`connectedDebugAndroidTest`), e o mantenedor precisa provisionar os GitHub Secrets / `keystore.properties` locais para ativar a assinatura persistente entre builds e releases.

---

## 5. Cobertura Atual de Testes (`21` Arquivos JUnit, `170` `@Test`)

> **Aviso de honestidade:** todos os `21` arquivos existem em `app/src/test/java/com/nexus/app/`, totalizando **170** casos `@Test` em JUnit 4 puro (JVM). Neste container sem JDK/Gradle/Android SDK, **nenhum deles foi compilado ou executado localmente** (apenas verificação estática de sintaxe/paridade em Python sobre os `.kt` e `bash -n` nos blocos do workflow `.github/workflows/android-build.yml` foram executadas).

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
| `NexusRodada4RecurrenceUnitTests.kt` | `5` | `TaskRecurrenceFrequency.ANNUAL` + 29/02, recorrência virtual de Compromissos + `SKIP`/`OVERRIDE` + conflito virtual↔materializado, Eventos recorrentes multi-dia sem obrigações financeiras fantasma, independência de checklists (`templateSubtasksJson` / `GAP-003`) e paridade Schema `v8` / `MIGRATION_7_8` / Backup `v9` |
| `OccurrenceChecklistIsolationUnitTests.kt` | `12` | Resolução não retroativa de templates (`ChecklistTemplateSnapshot` / `effectiveFromDateIso` / `checklistTemplateVersion`), isolamento absoluto `(recurrenceId, occurrenceDateIso)` em `TASK`, `APPOINTMENT` e `EVENT`, ordenação/progresso, compatibilidade legada e migração de dados pré-`v8` |
| `FactoryResetUnitTests.kt` | `10` | Contrato da Restauração de Fábrica: limpeza das `28` tabelas, re-seed padrão (`Enzo`, tema escuro, `16` categorias, rotina/personalização), preservação integral de PAT Keystore e coordenadas GitHub da `NEXUS-INBOX`, limpeza do rastreamento local da Inbox permitindo reimportação após reset e rollback coordenado em falhas |
| `MarketQuoteUnitTests.kt` | `17` | Catálogo de 12 ativos, parsing de AwesomeAPI/PTAX/CoinGecko/CoinPaprika/B3/Brapi/CNBC/Nasdaq, cooldown/cache offline e conversão USD→BRL |
| `MarketHistoryUnitTests.kt` | `9` | Regras de intervalos (`1D` omitido para moedas), parsers históricos por provedor, cache e cálculo de variação/extremos |
| `MarketPresentationUnitTests.kt` | `4` | `MarketPresentationMapper`: formatação por unidade (`BRL`/`USD`/`pts`), severidade de status e badges de fonte/atraso |
| `MarketWatchlistUnitTests.kt` | `3` | Adição/remoção, deduplicação preservando ordem e validação via `MarketAssetCatalog` |
| `MarketAlertUnitTests.kt` | `5` | `MarketAlertRules`: avaliação acima/abaixo, prevenção de duplicidade (`awaitingConditionReset`, timestamp), rearme e validação de moeda/unidade |
| `MarketBackupIntegrationUnitTests.kt` | `5` | Exportação/restauração Backup `v9` com `marketConfig`, retrocompatibilidade `null` vs `[]`, sanitização de alertas inválidos e rollback Room ↔ Mercado |
| `BackupIncrementalMergeUnitTests.kt` | `15` | Suíte transacional (`InMemoryBackupStorageGateway`) cobrindo Modo 1 vs Modo 2: mesclagem idempotente sem `deleteAll`, preservação de conflitos/singletons, ordem topológica e ciclo `recurrences ↔ events`, `scheduleChecklistItems`, FKs obrigatórias vs opcionais (`NEEDS_REVIEW`), coerência cartão/fatura/parcela/recorrência e bloqueio anti-dupla despesa, versões `v1..v9` + backfill de `v7`/`v8` + `RecurrenceException` overrides + lembretes `rec::...`, rollback Room ↔ Mercado, paridade estrita do dry-run `previewMerge` (zero gravações) e separação explícita de modos |
| `StructuredInputUnitTests.kt` | `12` | Suíte transacional (`InMemoryStructuredInputGateway`) cobrindo contrato `v1`, JSON estrito (`ignoreUnknownKeys = false`), enums inválidos, validação de FKs entre os `8` tipos (`TASK`, `CHECKLIST`, `APPOINTMENT` + `checklist`, `EVENT` + `checklist`, `RECURRENCE` com `templateChecklist` e `effectiveFromDateIso` não retroativo, `PROJECT`, `PERSONAL_GOAL`, `TRANSACTION`), ações `CREATE`/`UPDATE`/`UPSERT` preservando `createdAt`, bloqueio financeiro (`PAID` / dupla despesa), prévia dry-run `previewEnvelope` sem escritas (com vínculos intra-lote), idempotência por `operationId`, rollback completo de `atomicBatch` e retentativa limpa após `FAILED_ROLLED_BACK` |
| `InboxBridgeUnitTests.kt` | `5` | Validação `InboxInputValidator`, template oficial, fluxo completo `checkInboxNow` (dry-run) → confirmação explícita (`confirmAndImportPending`) → persistência real (`EVENT`, `TASK`, `CHECKLIST`, `RECURRENCE`, `APPOINTMENT`/`EVENT` com `checklist`, `TRANSACTION`) + omissão de categorias vazias no resumo + deduplicação por `(path::sha)` em reconsulta, segurança em segundo plano e reset local preservando PAT/coordenadas |

### Lacunas de Teste Identificadas
- **`0` testes em `app/src/androidTest/`:** não há testes instrumentados de:
  - Migrações reais do SQLite Room (`1→2→3→4→5→6→7→8`) com `MigrationTestHelper` (incluindo `MIGRATION_7_8`);
  - UI Jetpack Compose (fluxos de editores, `ScheduleChecklistSectionCard`, diálogos de confirmação da Restauração de Fábrica, `BackHandler`, gráficos `Canvas`, filtros da Agenda);
  - `SAF` real (leitura/escrita `.json`, `.csv`, `.txt`), `AlarmManager` em Doze, `NexusBootReceiver`, notificações + deep-links em Android 13+ e widgets Glance reais no launcher;
  - Integração HTTP real contra provedores públicos do Mercado.
