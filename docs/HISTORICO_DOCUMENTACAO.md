# Histórico de Evolução da Documentação do NEXUS

Este é o registro central de alterações documentais do NEXUS. **Toda atualização relevante realizada em qualquer documento de /docs/ deve gerar uma entrada aqui**, inclusive correções de estado, inclusão de pesquisas, mudanças de especificação, rastreabilidade, roadmap, governança e registros históricos. Não é changelog do aplicativo nem substitui o histórico Git.

Cada entrada deve explicar motivação, contexto/evidência, documentos afetados, mudanças realizadas, limites e o que não foi alterado. Quando uma atualização ocorrer apenas em memória estratégica ou em um repositório espelho, registrar isso no histórico correspondente daquele repositório e apontar a relação, sem declarar que /docs/ foi atualizado.

---

## 2026-11-21 — Governança Documental e Ciclo de Vida de Ideias

**Tipo:** atualização metodológica.

### Motivação
A documentação descrevia produto, arquitetura, estado e execução, mas não definia suficientemente como ideias novas, decisões antigas, propostas rejeitadas e possibilidades futuras deveriam ser preservadas e reavaliadas. A seção “Fora de Escopo Permanente / Descartado” do roadmap misturava situações distintas e podia transformar uma decisão contextual em aparente proibição definitiva.

### Problemas identificados
- Ausência de ciclo de vida explícito entre conversa, pesquisa, análise, proposta, aprovação, planejamento e implementação.
- Falta de clareza de que documentos vigentes, inclusive o Documento Mestre, podem evoluir.
- Risco de decisões antigas bloquearem reavaliações sem considerar novas evidências.
- Distinção insuficiente entre autorização para pesquisa, aprovação de produto, atualização documental e implementação.
- Continuidade entre conversas dependente de inferências sobre o que foi aprovado.

### Mudanças introduzidas
1. Criado 13_GOVERNANCA_DOCUMENTAL.md como referência normativa.
2. Definidos estados para ideias e iniciativas, da captura à implementação, incluindo pesquisa, análise, proposta, pausa, rejeição, descarte e substituição.
3. Estabelecido que rejeição ou descarte contextual não significa proibição eterna.
4. Reforçada a distinção entre discussão, proposta, aprovação, planejamento e implementação.
5. Esclarecido que documentos mestres, especificações, arquitetura, ADRs e roadmap são revisáveis, preservando histórico.
6. Reorganizada a leitura de escopo no roadmap.
7. Complementado o guia de desenvolvimento com o fluxo de governança.
8. Atualizado o índice documental.

### Limites
- Nenhuma funcionalidade do aplicativo foi aprovada ou implementada.
- Nenhum requisito de IA local, reconhecimento de voz, integração bancária ou importação inteligente foi criado.
- Nenhum código Android, banco Room, backup do aplicativo ou ADR técnico foi alterado.
- Artefatos internos do Google AI Studio não foram modificados nesta operação; deverão ser alinhados quando estiverem acessíveis.
- Não foram executados testes de build ou Android, pois não houve alteração de código.

### Arquivos afetados
- docs/README.md
- docs/01_DOCUMENTO_MESTRE.md
- docs/09_GUIA_DESENVOLVIMENTO.md
- docs/10_ROADMAP.md
- docs/13_GOVERNANCA_DOCUMENTAL.md (novo)
- docs/HISTORICO_DOCUMENTACAO.md (novo)

### Resultado esperado
Uma sessão futura deverá distinguir ideia, pesquisa, análise, proposta, aprovação, planejamento e implementação. Decisões antigas continuarão consultáveis sem bloquear automaticamente novas soluções quando o contexto mudar.

---

## 2026-11-22 — Reforço da rastreabilidade documental e reabertura factual do BUG-001

**Tipo:** atualização de estado técnico documentado e regra de manutenção do histórico.

### Motivação e evidência
Após instalar e testar a versão que continha o lote visual/adaptativo, o usuário relatou novamente que girar o celular ou tablet durante o uso de diferentes abas pode retornar à tela inicial. Exemplo explícito: editar uma tarefa já existente e perder a tela de edição ao girar o dispositivo. O relato confirma que BUG-001 continua perceptível no uso real; não identifica por si só a causa completa nem prova quais telas são afetadas.

### Mudanças introduzidas
1. Reforçado que BUG-001 permanece EM ABERTO, com novo relato posterior à versão visual/adaptativa.
2. Explicitada a separação entre adaptação visual da interface e preservação funcional da rota/edição durante recriação da Activity.
3. Acrescentada rastreabilidade do BUG-001 na matriz, vinculando shell/navegação, editores, estado, investigação e validação necessária.
4. Tornado este arquivo o registro obrigatório de todos os lotes relevantes de atualização documental em /docs/, não apenas mudanças metodológicas.

### Arquivos afetados
- docs/07_ESTADO_ATUAL.md
- docs/12_RASTREABILIDADE.md
- docs/HISTORICO_DOCUMENTACAO.md

### Limites e itens não alterados
- Nenhum código Android foi alterado.
- Nenhum diagnóstico técnico novo foi executado nesta atualização.
- Nenhum build, teste automatizado ou teste de dispositivo foi executado pelo assistente nesta operação.
- BUG-001 não foi corrigido nem validado.
- Nenhum ADR foi alterado.
- Os artefatos internos do Google AI Studio não foram atualizados; o ambiente conectado e seu conteúdo não foram acessados nesta operação.

### Próximo passo
Realizar investigação técnica abrangente do estado de navegação e edição, propor uma correção transversal e validar em dispositivo em diferentes telas/abas e etapas de edição. Não encerrar BUG-001 com base apenas em compilação bem-sucedida ou layout adaptativo.

---

## 2026-11-22 — Registro da pesquisa e direção adaptativa da Agenda

**Tipo:** consolidação documental de feedback pós-implementação visual.

### Motivação
O usuário aprovou a aparência dos cartões semanais no celular, mas considerou compactas demais as informações da Agenda semanal e insatisfatória a composição em tablet na orientação horizontal. Solicitou painel semanal ocupando a altura útil, rolagem interna independente por dia, coerência visual com Kanban e pesquisa de padrões adaptativos.

### Arquivos afetados no repositório estratégico NEXUS-DOCUMENTACAO
Esta entrada documenta a relação com a memória estratégica; não significa que os arquivos abaixo estejam em /docs/:
- CHATGPT/09_METODOLOGIA_DE_PESQUISA_E_DESENVOLVIMENTO.md (novo)
- CHATGPT/PROPOSTAS/2026-11-21_ROTACAO_E_IDENTIDADE_VISUAL/PESQUISA_LAYOUTS_ADAPTATIVOS_E_ROLAGEM.md (novo)
- CHATGPT/PROPOSTAS/2026-11-21_ROTACAO_E_IDENTIDADE_VISUAL/DIRECAO_LAYOUTS_ADAPTATIVOS.md
- CHATGPT/03_IDEIAS_E_BRAINSTORMS.md
- CHATGPT/06_PENDENCIAS_E_DIRECAO_FUTURA.md
- CHATGPT/07_HISTORICO_DE_REGISTROS.md
- CHATGPT/README.md
- CHATGPT/PROPOSTAS/2026-11-21_ROTACAO_E_IDENTIDADE_VISUAL/README.md

### Limites
- A IDEA-006 segue em pesquisa/análise.
- Não houve implementação adicional nem alteração de código por essa atualização.
- O registro detalhado e as fontes estão no repositório estratégico indicado acima.

---

## Modelo para próximos registros

Registrar data, tipo, motivação, evidência/origem, documentos afetados (com caminhos e repositórios), mudanças introduzidas, estado resultante, limites, itens não alterados e próximos passos. Toda atualização documental relevante em /docs/ deve ser registrada neste arquivo.


## 2026-11-23 — Hierarquia de autoridade e prevenção de contradições

**Tipo:** revisão estrutural de governança e clareza documental.

### Motivação
O responsável identificou risco de agentes encontrarem decisões técnicas vigentes e propostas posteriores de revisão sem indicação explícita de que a proposta não revoga a decisão. O exemplo principal era a arquitetura local/offline-first e a possibilidade futura de serviços online, sincronização e integrações.

### Diagnóstico
- O índice técnico possuía hierarquia de fontes, mas não definia suficientemente o efeito normativo de propostas e brainstorms.
- O ADR-003 usava “Permanente”, embora a governança permita revisar decisões.
- Memória estratégica e documentação técnica convivem em repositórios distintos.
- “100% sem internet” é impreciso em sentido literal, pois o Mercado utiliza provedores externos.

### Mudanças
1. Criado `docs/00_ORIENTACAO_E_AUTORIDADE.md`, leitura obrigatória antes dos demais documentos.
2. Reforçada no README a precedência entre código, documentação técnica, espelho, propostas e histórico.
3. Ajustado o Documento Mestre para descrever arquitetura local-first/offline-first, sem dependência obrigatória de nuvem e sem sugerir ausência absoluta de rede.
4. Revisado o status do ADR-003 de “Aceita e Permanente” para “Aceita e Ativa — vigente até eventual revisão formal”.
5. Acrescentada regra explícita de que propostas, brainstorms e relatórios não revogam decisões vigentes por data ou presença documental.

### Limites
- Nenhuma decisão sobre sincronização, backend, login, Open Finance ou IA foi aprovada.
- Arquitetura implementada e código Android não foram alterados.
- Nenhum teste ou build foi executado.
- Espelho e memória estratégica ainda precisam ser sincronizados; artefatos internos do AI Studio não foram acessados.

### Arquivos afetados
- `docs/00_ORIENTACAO_E_AUTORIDADE.md` (novo)
- `docs/README.md`
- `docs/01_DOCUMENTO_MESTRE.md`
- `docs/11_DECISOES_ADRS.md`
- `docs/13_GOVERNANCA_DOCUMENTAL.md`
- `docs/HISTORICO_DOCUMENTACAO.md`

### Resultado esperado
Agentes distinguem arquitetura vigente de propostas de evolução. Uma proposta não produz comportamento contraditório antes de ser aprovada e refletida nos documentos normativos.


### Complemento — 2026-11-23 — Linguagem normativa revisável no Documento Mestre e Guia

A varredura identificou títulos e comandos absolutos que poderiam ser interpretados como imutáveis, mesmo quando a governança permite revisão formal.

**Ajustes adicionais:**
- `01_DOCUMENTO_MESTRE.md`: “Pilares e Princípios Permanentes” passou a “Princípios técnicos vigentes (revisáveis)”.
- `09_GUIA_DESENVOLVIMENTO.md`: “Regras Permanentes” passou a “Regras Vigentes”; incrementalidade foi mantida como padrão, sem proibir reescritas justificadas e aprovadas; restrições de backend/login foram explicitamente vinculadas ao ADR-003 ativo.

**Limites:** não houve mudança de decisão arquitetural, código ou escopo. Os documentos foram ajustados para deixar claro que as regras atuais são obrigatórias enquanto vigentes, mas revisáveis pelo processo formal.

---

## 2026-11-25 — Rodada 1: Confiabilidade, Atualização In-Place e Mesclagem Incremental de Backups (`v5.3.0`)

**Tipo:** evolução funcional implementada no código Android + atualização sincronizada de `/docs/` e artefatos do Google AI Studio.

### Motivação e evidência
Preparar o NEXUS para uso cotidiano contínuo resolvendo dois bloqueios operacionais reais:
1. Conflitos de assinatura (`INSTALL_FAILED_UPDATE_INCOMPATIBLE`) em builds de CI/dispositivo que obrigavam a desinstalar o app antes de atualizar (apagando `nexus_local.db`, dado `android:allowBackup="false"`).
2. Ausência de importação incremental de backups sem apagar o banco atual (`BackupManager.restoreAtomically` executava apenas substituição destrutiva completa).

### Mudanças implementadas e documentadas
1. **Assinatura persistente opcional e versionamento (`ADR-015`):**
   - Preservado `applicationId = "com.nexus.app"`, atualizado `versionCode` (`7 -> 8`) e `versionName` (`"5.2.0" -> "5.3.0"`).
   - Adicionada leitura segura opcional de `keystore.properties` / variáveis de ambiente em `app/build.gradle.kts`, criado `keystore.properties.example`, protegidas extensões sensíveis no `.gitignore` e adicionado suporte opcional a secrets (`NEXUS_DEBUG_KEYSTORE_BASE64`, `NEXUS_KEYSTORE_BASE64`) no `.github/workflows/android-build.yml`.
2. **Mesclagem incremental idempotente (`ADR-016`):**
   - Criados `BackupStorageGateway.kt` (`RoomBackupStorageGateway`), `BackupMergeEngine.kt` e a suíte `BackupIncrementalMergeUnitTests.kt` (`11` `@Test` cobrindo 20 cenários).
   - Expandidos `BackupModels.kt` (`BackupImportMode`, `MergeSummary`, `MergeIssueItem`, `EntityMergeCounts`), `BackupManager.kt` (`validateForMerge`, `mergeBackupAtomically`), `NexusRepository.kt`, `NexusViewModel.kt`, `MainActivity.kt` e `MoreSettingsBackupDiagnosticsScreens.kt` (distinção explícita entre Modo 1 e Modo 2 na `BackupScreen`).
3. **Documentação e artefatos sincronizados:**
   - Atualizados `docs/README.md`, `02_ESPECIFICACAO_PRODUTO.md`, `03_ARQUITETURA_ENGENHARIA.md`, `04_MODELO_DADOS.md`, `07_ESTADO_ATUAL.md`, `08_QUALIDADE_SEGURANCA.md`, `10_ROADMAP.md`, `11_DECISOES_ADRS.md` (`ADR-015` e `ADR-016`), `12_RASTREABILIDADE.md`, `HISTORICO_DOCUMENTACAO.md` e os artefatos `NEXUS_CONTINUIDADE.md`, `NEXUS_CURRENT_STATE.md` e `NEXUS_IMPLEMENTATION_ROADMAP.md`.

### Limites e itens não alterados
- **Não compilado/executado localmente:** este container não possui JDK, Gradle, Android SDK (`apksigner`/`adb`) nem emulador; `:app:testDebugUnitTest` (16 arquivos, 122 `@Test`), `:app:assembleDebug` e testes em aparelho permanecem como `NÃO VERIFICADO` até execução no GitHub Actions ou máquina local.
- **Sem secrets embutidos:** cabe ao mantenedor gerar/armazenar o arquivo `.jks` e configurar `keystore.properties` / GitHub Secrets.
- **`web-mirror-pilot/`, `NexusDatabase` (`DATABASE_VERSION = 6`), `NexusMigrations.kt` e `CURRENT_FORMAT_VERSION = 7`** permaneceram inalterados. `BUG-001` segue em aberto para rodada própria.

### Próximo passo
Executar o workflow `.github/workflows/android-build.yml` (ou `:app:testDebugUnitTest :app:assembleDebug` com JDK 17 + Android SDK), configurar keystore persistente para validar atualização in-place (`apksigner` + `adb install -r`) e validar a mesclagem incremental com backup real em dispositivo.

---

## 2026-11-25 — Etapa 2: Prévia Dry-Run de Backup, Entrada Estruturada Interna (v1) e Endurecimento de Assinatura/CI

**Tipo:** evolução funcional implementada no código Android + pipeline CI + atualização sincronizada de `/docs/` e artefatos do Google AI Studio.

### Motivação e evidência
Completar os 3 pilares previstos para a **Etapa 2 de 4** após a Rodada 1:
1. Eliminar risco de restauração destrutiva acidental e oferecer prévia dry-run fiel à execução real antes de confirmar a mesclagem incremental (**Frente A**).
2. Disponibilizar uma camada interna versionada e testável de entrada estruturada (`contractVersion = 1`) sobre o banco Room único `v6` pronta para o conector externo da Etapa 3, sem introduzir segundo banco nem acoplar rede/GitHub API antecipadamente (**Frente B**).
3. Impedir falhas silenciosas de assinatura no Gradle e no GitHub Actions, verificando criptograficamente o SHA-256 dos APKs emitidos via `apksigner` (**Frente C**).

### Mudanças implementadas e documentadas
1. **Frente A — Prévia Dry-Run e Separação de Fluxos de Backup (`ADR-016`):**
   - Refatorado `BackupMergeEngine.kt` com `computeMergePlan` compartilhado e `previewMerge(..., isDryRun = true)` com garantia de zero gravações no Room e nos stores do Mercado.
   - Adicionado `BackupManager.previewMerge`, `NexusRepository.previewBackupMerge`, `BackupUiState` e prévia assíncrona em `NexusViewModel.kt`, e separados completamente os fluxos na UI (`MoreSettingsBackupDiagnosticsScreens.kt` + `MainActivity.kt`): Restauração Completa com diálogo próprio e checkbox obrigatório de confirmação destrutiva; Mesclagem Incremental com diálogo de prévia dry-run antes de aplicar e resumo legível (`MergeSummary`) em pt-BR.
   - Expandido `BackupIncrementalMergeUnitTests.kt` para `13` `@Test`.
2. **Frente B — Camada Interna de Entrada Estruturada (`ADR-017`):**
   - Criados `StructuredInputModels.kt`, `StructuredInputGateway.kt` e `StructuredInputProcessor.kt` em `app/src/main/java/com/nexus/app/data/ingestion/`, conectados ao `NexusRepository.kt` (`processStructuredInputJson`, `processStructuredInputEnvelope`) sobre Room `v6` + idempotência em `audit_history` (`STRUCTURED_INPUT_OP`).
   - Criada a suíte `StructuredInputUnitTests.kt` (`7` `@Test`) cobrindo contrato `v1`, JSON estrito, enums, FKs, regras financeiras, conflitos locais, idempotência por `operationId`, rollback de lote atômico e retentativa limpa após `FAILED_ROLLED_BACK`.
   - Criado o guia normativo `docs/14_CONTRATO_ENTRADA_ESTRUTURADA_ETAPA3.md`.
3. **Frente C — Endurecimento de Assinatura e CI (`ADR-015`):**
   - Endurecido `app/build.gradle.kts` para falhar imediatamente (`GradleException`) em configurações parciais (`1..3/4`) ou arquivos inexistentes e bloquear `assembleRelease`/`bundleRelease`/`packageRelease` sem configuração `release` válida.
   - Atualizado `.github/workflows/android-build.yml` com `build-tools;35.0.0`, validação prévia via `keytool -list -v` (`-storepass:env`), build condicional `:app:assembleRelease`, verificação pós-build via `apksigner verify --verbose --print-certs` comparando o fingerprint SHA-256 e publicação do relatório público em `$GITHUB_STEP_SUMMARY` e `NEXUS-Signing-Verification-Report`.
4. **Documentação e artefatos sincronizados:**
   - Criado `docs/14_CONTRATO_ENTRADA_ESTRUTURADA_ETAPA3.md` e atualizados `docs/README.md`, `02_ESPECIFICACAO_PRODUTO.md`, `03_ARQUITETURA_ENGENHARIA.md`, `04_MODELO_DADOS.md`, `07_ESTADO_ATUAL.md`, `08_QUALIDADE_SEGURANCA.md`, `10_ROADMAP.md`, `11_DECISOES_ADRS.md` (`ADR-015`, `ADR-016`, `ADR-017`), `12_RASTREABILIDADE.md` (`REQ-025`, `REQ-027`), `HISTORICO_DOCUMENTACAO.md`, além dos artefatos `NEXUS_CONTINUIDADE.md`, `NEXUS_CURRENT_STATE.md` e `NEXUS_IMPLEMENTATION_ROADMAP.md`.

### Limites e itens não alterados
- **Compilação Kotlin/Gradle e testes JUnit NÃO EXECUTADOS localmente:** este container não possui JDK, Gradle, Android SDK (`keytool`/`apksigner`/`adb`) nem emulador; os `17` arquivos de teste (`131` `@Test`) permanecem como `NÃO EXECUTADO` localmente. Apenas os blocos bash de `.github/workflows/android-build.yml` foram verificados via `bash -n` (exit 0) junto com inspeção estática dos arquivos modificados/criados.
- **Sem transporte externo/GitHub API ainda:** reservado exclusivamente à **Etapa 3** sobre o contrato `docs/14_CONTRATO_ENTRADA_ESTRUTURADA_ETAPA3.md`.
- **`applicationId = "com.nexus.app"`, `DATABASE_VERSION = 6`, `NexusMigrations.kt`, `CURRENT_FORMAT_VERSION = 7` e `web-mirror-pilot/`** permaneceram inalterados. `BUG-001` segue em aberto.

### Próximo passo
Executar `.github/workflows/android-build.yml` no GitHub Actions (com ou sem os 8 Secrets de assinatura configurados conforme manual entregue) e validar `NEXUS-Signing-Verification-Report`, prévia dry-run/mesclagem em aparelho real e posteriormente iniciar a **Etapa 3** conectando o transporte externo ao `NexusRepository.processStructuredInputJson` / `processStructuredInputEnvelope`.

---

## 2026-11-25 — Correção Cirúrgica do Setup do Android SDK no CI (`#45733193619`)

**Tipo:** correção pontual de pipeline GitHub Actions + sincronização cirúrgica de documentação/artefatos.

### Motivação e evidência
A execução `#45733193619` (`commit fc7a4d6752cab01d05fcd5bdb1f63130fcdd526f`) do workflow `NEXUS Android Build` falhou na etapa `android-actions/setup-android@v4` porque o parâmetro multilinha `packages: |` enviou `'platform-tools\nbuild-tools;35.0.0'` como um único argumento ao `sdkmanager` (`Warning: Failed to find package 'platform-tools\nbuild-tools;35.0.0'`).

### Mudanças implementadas
1. **`.github/workflows/android-build.yml`:**
   - Ajustado `android-actions/setup-android@v4` para `packages: 'platform-tools'`.
   - Adicionado o passo `Install and verify Android SDK tools (platform-tools & build-tools 35.0.0)` que localiza o `sdkmanager`, garante aceite de licenças sem erro de `SIGPIPE`, invoca `"$SDKMANAGER" --sdk_root="$SDK_ROOT" "platform-tools" "build-tools;35.0.0"` passando cada pacote como argumento independente, e valida existência executável + versão de `$SDK_ROOT/platform-tools/adb` e `$SDK_ROOT/build-tools/35.0.0/apksigner`.
   - Atualizada a etapa `Verify APK Signatures and Certificate Fingerprints` para priorizar `$ANDROID_HOME/build-tools/35.0.0/apksigner`.
2. **Documentação e artefatos sincronizados:**
   - Atualizados pontualmente `docs/07_ESTADO_ATUAL.md`, `docs/08_QUALIDADE_SEGURANCA.md`, `docs/11_DECISOES_ADRS.md` (`ADR-015`), `docs/HISTORICO_DOCUMENTACAO.md` e os artefatos `NEXUS_CURRENT_STATE.md` e `NEXUS_CONTINUIDADE.md`.

### Limites e itens não alterados
- **Não executado em runner/ambiente Android:** neste container não há JDK, Gradle, Android SDK nem acesso Git (`NO_GIT_REPO_IN_CONTAINER`); a correção foi validada localmente apenas via `bash -n` nos 3 blocos de script do workflow (`exit 0`). A confirmação definitiva depende de nova execução do GitHub Actions na branch `main`.
- **Código Android preservado:** nenhuma alteração foi feita em `app/`, `NexusDatabase` (`v6`), `BackupModels` (`v7`), `data/ingestion/`, `applicationId`, `versionCode`/`versionName`, testes JUnit ou `web-mirror-pilot/`.

### Próximo passo
Fazer push da correção para `main` e executar o workflow `NEXUS Android Build` no GitHub Actions para confirmar a instalação de `platform-tools` + `build-tools;35.0.0`, os testes unitários e o build/assinatura do APK.

---

## 2026-11-26 — Rodada 3: Ponte ChatGPT → `NEXUS-INBOX` → NEXUS Android (`ADR-018`)

**Tipo:** evolução funcional implementada no código Android + atualização sincronizada de `/docs/` e artefatos do Google AI Studio.

### Motivação e evidência
Entregar a primeira versão funcional ponta a ponta da integração **ChatGPT → `NEXUS-INBOX` (repositório GitHub privado) → NEXUS Android** sobre a camada `data/ingestion/` (`contractVersion = 1`) e o banco Room único `v6`, mantendo acesso discreto em `"Mais"`, segurança criptográfica da credencial no aparelho e controle explícito do usuário antes de qualquer escrita.

### Mudanças implementadas e documentadas
1. **Extensão retrocompatível da entrada estruturada (`data/ingestion/` + `NexusDaos.kt` + `NexusRepository.kt`):**
   - Adicionado `StructuredEntityType.CHECKLIST`, suporte a `subtasks` inline em `TASK`, ações explícitas `CREATE`/`UPDATE`/`UPSERT` preservando `createdAt` em atualizações, estado `UPDATED` e simulação dry-run `previewJson`/`previewEnvelope` via `PreviewOverlayGateway` (zero gravações no banco, suportando vínculos intra-lote como `TASK` → `CHECKLIST` e `TASK` → `TRANSACTION`).
   - Expandido `NexusDaos.kt` (`SubtaskDao.getById` / `getByTaskId`, `AuditHistoryDao.observeLatestByEntityType`) sem alterar `DATABASE_VERSION = 6`, e adicionado `AUDIT_ENTITY_INBOX_IMPORT_LOG = "INBOX_IMPORT_LOG"` + `observeInboxImportLogs()` + implementação de `InboxIngestionPort` em `NexusRepository.kt`.
   - Expandido `StructuredInputUnitTests.kt` de `7` para `10` `@Test`.
2. **Conector `data/inbox/` + Worker + Notificações + UI (`SubRoute.InboxBridge`):**
   - Criados `InboxModels.kt`, `InboxStores.kt` (`InboxInputValidator`, `SharedPrefsInboxConfigStorage`, `AndroidKeystoreInboxTokenStore` com `AES/GCM/NoPadding`), `GitHubInboxRemoteDataSource.kt` (`HttpsURLConnection` `GET` somente leitura, sem redirects, limite `2 MB`, zero credenciais hardcoded), `InboxRepository.kt` (`InboxSummaryFormatter` omitindo categorias sem alteração real, dry-run `checkInboxNow`, confirmação explícita `confirmAndImportPending` e deduplicação por `path::sha`) e `InboxSyncWorker.kt` (`WorkManager` opcional `>= 15 min`, nunca auto-importa).
   - Adicionados `showInboxPendingNotification` + `EXTRA_OPEN_INBOX` (`NexusNotificationScheduler.kt`), instanciação lazy em `AppContainer` (`NexusApplication.kt`), estados/ações em `NexusViewModel.kt`, tela `NexusInboxScreen.kt`, entrada discreta em `MoreSettingsBackupDiagnosticsScreens.kt` (`"NEXUS-INBOX (Ponte ChatGPT)"`) e rota `SubRoute.InboxBridge` + deep-link em `MainActivity.kt`.
   - Criada a suíte `InboxBridgeUnitTests.kt` (`3` `@Test`), elevando o total do projeto para `18` arquivos e `137` `@Test`.
3. **Documentação e artefatos sincronizados:**
   - Atualizados `docs/14_CONTRATO_ENTRADA_ESTRUTURADA_ETAPA3.md`, `docs/03_ARQUITETURA_ENGENHARIA.md`, `docs/07_ESTADO_ATUAL.md`, `docs/08_QUALIDADE_SEGURANCA.md`, `docs/10_ROADMAP.md`, `docs/11_DECISOES_ADRS.md` (`ADR-018`), `docs/12_RASTREABILIDADE.md` (`REQ-027`, `REQ-028`), `docs/HISTORICO_DOCUMENTACAO.md`, além dos artefatos `NEXUS_CURRENT_STATE.md`, `NEXUS_CONTINUIDADE.md` e `NEXUS_IMPLEMENTATION_ROADMAP.md`.

### Limites e itens não alterados
- **Compilação Kotlin/Gradle e testes JUnit NÃO EXECUTADOS localmente:** este container não possui JDK, Gradle, Android SDK (`keytool`/`apksigner`/`adb`) nem emulador; os `18` arquivos de teste (`137` `@Test`) permanecem como `NÃO EXECUTADO` localmente até execução no GitHub Actions ou Android Studio.
- **`applicationId = "com.nexus.app"`, `versionCode = 8`, `versionName = "5.3.0"`, `DATABASE_VERSION = 6`, `NexusMigrations.kt`, `CURRENT_FORMAT_VERSION = 7` e `web-mirror-pilot/`** permaneceram inalterados. `BUG-001` segue em aberto.

### Próximo passo
Rodar `.github/workflows/android-build.yml` no GitHub Actions (ou `:app:testDebugUnitTest :app:assembleDebug` com JDK 17 + Android SDK), criar o repositório privado `NEXUS-INBOX` no GitHub com Fine-Grained PAT (`Contents: Read-only`), configurar em **Mais → NEXUS-INBOX** no app e testar o fluxo completo com JSON gerado pelo ChatGPT.






## 2026-11-26 — Inventário consolidado de lacunas, recorrência e correções da Rodada 3

**Tipo:** consolidação de pendências, correção documental e rastreabilidade de falhas de implementação/CI.

### Motivação e evidência
Durante o povoamento inicial pela ponte ChatGPT → NEXUS-INBOX, ficou evidente que uma rotina semanal de compromisso (corrida às quintas, 20h) não podia ser registrada como série recorrente. A revisão documental confirmou que `REQ-009` já classificava recorrência de Compromissos e Eventos como não implementada, e que a `ADR-008` adiava a expansão. A ponte `v1` também não permite criar regras recorrentes novas. A inspeção do contrato encontrou ainda nomes de campos de checklist divergentes no exemplo JSON.

### Mudanças introduzidas
1. Criado `docs/15_REGISTRO_LACUNAS_E_FALHAS.md` como inventário consolidado inicial, com `GAP-001` (recorrência de compromissos/eventos), `GAP-002` (limitação de recorrência na Inbox) e `DOC-001` (exemplo JSON incompatível), além de pendências técnicas já rastreadas e histórico de falhas da Rodada 3.
2. Corrigidos no exemplo de `docs/14_CONTRATO_ENTRADA_ESTRUTURADA_ETAPA3.md` os campos `isDone`/`orderIndex` para `isCompleted`/`sortOrder`, e documentada explicitamente a limitação atual de recorrência do contrato `v1`.
3. Reforçada em `07_ESTADO_ATUAL.md`, `10_ROADMAP.md` e `12_RASTREABILIDADE.md` a ligação entre `REQ-009`, `GAP-001` e `GAP-002`.
4. Atualizados o índice e a orientação documental para apontar o novo registro.
5. Registrado o histórico de CI: #47 com erros de compilação, #48 com um teste falho posteriormente corrigido e #50 com build/testes/Debug APK concluídos com sucesso; Release permaneceu ignorado por falta de secrets.

### Limites e itens não alterados
- Nenhum código Android, schema Room ou contrato executável foi alterado nesta atualização.
- Nenhuma decisão técnica sobre o modelo final de recorrência ou versão futura do contrato foi tomada.
- A recorrência de Compromissos e Eventos continua **não implementada**.
- A validação em dispositivo, migrações Room reais e atualização in-place continuam pendentes conforme `07_ESTADO_ATUAL.md`.
- O documento do Claude foi consultado: trata série e opções “só esta/toda a série” como princípios de experiência e lista recorrência de compromissos/eventos entre evoluções futuras, mas não fornece especificação técnica suficiente para implementar `REQ-009`.
- Espelho `NEXUS-DOCUMENTACAO/GOOGLE_IA/docs/` atualizado nesta sincronização; artefatos internos do AI Studio não foram acessados nesta operação.

### Arquivos afetados
- `docs/15_REGISTRO_LACUNAS_E_FALHAS.md` (novo)
- `docs/README.md`
- `docs/00_ORIENTACAO_E_AUTORIDADE.md`
- `docs/07_ESTADO_ATUAL.md`
- `docs/10_ROADMAP.md`
- `docs/12_RASTREABILIDADE.md`
- `docs/14_CONTRATO_ENTRADA_ESTRUTURADA_ETAPA3.md`
- `docs/HISTORICO_DOCUMENTACAO.md`



## 2026-11-26 — Reorganização das etapas e decisão sobre branches da Rodada 3

**Motivação:** a Rodada 3 (ponte ChatGPT → GitHub → Android) avançou, mas as correções finais ficaram em branches isoladas. Ao mesmo tempo, a revisão de requisitos evidenciou lacunas funcionais importantes, especialmente recorrência de Compromissos e Eventos. A antiga Rodada 4, que previa aplicar a proposta Claude, não deve ser iniciada antes de completar o núcleo funcional.

**Decisões de planejamento:**
1. Preservar Rodadas 1 e 2 como concluídas e manter a Rodada 3 como etapa atual, ainda em fechamento.
2. Não integrar integralmente `fix/inbox-build-47-kotlin-compile` nem `fix/inbox-kotlin-compile` à `main`. Em 26/11, estavam divergentes da `main` (respectivamente 4/9 e 3/9 commits à frente/atrás). Reaplicar as correções necessárias sobre a `main` atual via Google AI Studio, revisar diff e executar CI.
3. Registrar que a GitHub Actions #50 (run `45748189511`) passou no job de build, testes unitários e APK Debug; Release foi ignorado por falta de secrets. Preservar #47, #48 e #49 como histórico de falhas/tentativas.
4. Criar a Rodada 4 para completar funcionalidades centrais e lacunas de domínio, começando por `REQ-009` / `GAP-001` (recorrência de Compromissos e Eventos) e `GAP-002` (extensão da Inbox), seguindo para pendências de Eventos, Agenda e estado.
5. Reposicionar a evolução da experiência para Rodada 5, usando a documentação atualizada `NEXUS-DOCUMENTACAO/CHATGPT/ATUALIZACAO/` como base de investigação. A proposta original do Claude permanece referência, não arquitetura aprovada.
6. Tratar confiabilidade, backup, migrações, assinatura e validação em aparelho como gate contínuo, não como atividade que possa ser considerada resolvida por build verde.

**Arquivos alterados:** `docs/10_ROADMAP.md`, `docs/15_REGISTRO_LACUNAS_E_FALHAS.md`, `docs/HISTORICO_DOCUMENTACAO.md`.

**Limites:** nenhuma alteração de código Android, merge de branch ou aprovação de redesign foi realizada nesta atualização.


---

## 2026-11-26 — Captura de requisito transversal: checklist por ocorrência recorrente

**Tipo:** registro de necessidade funcional e atualização do planejamento.

### Motivação
O usuário explicitou que tarefas recorrentes podem reutilizar um checklist, mas cada ocorrência semanal precisa começar com os itens pendentes, sem herdar as marcações da semana anterior. O histórico da ocorrência passada deve permanecer intacto.

### Mudanças introduzidas
1. Registrado `GAP-003` em `docs/15_REGISTRO_LACUNAS_E_FALHAS.md`, como requisito transversal CAPTURADO / EM ANÁLISE.
2. Incluída a auditoria de checklist por ocorrência na Rodada 4 de `docs/10_ROADMAP.md`, antes de ampliar o contrato Inbox.

### Limites e itens não alterados
- Nenhum código Android, schema Room ou contrato Inbox foi alterado.
- O comportamento atual ainda precisa ser inspecionado; não foi classificado como defeito confirmado nem como funcionalidade ausente.
- A definição técnica deve partir da auditoria do motor de recorrência de tarefas e do modelo de subtarefas.
