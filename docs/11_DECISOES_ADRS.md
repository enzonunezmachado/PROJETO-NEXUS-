# 11 — Registro de Decisões Arquiteturais (`ADRs`)

> **Finalidade:** registrar as decisões estruturais reais do NEXUS com contexto, escolha adotada, alternativas e consequências.  
> **Regra de integridade:** quando o motivo histórico original de uma decisão não constar nos documentos ou comentários do repositório, isso é declarado explicitamente como *"Não documentado nos registros originais"*, sem invenção retroativa.

---

### `ADR-001` — Navegação por Estado Reativo em `NexusAppRoot` (Sem `NavHost`)
- **Status:** Aceita e Ativa (`MainActivity.kt`, `BackNavigationRules.kt`).
- **Contexto:** necessidade de alternar entre `5` abas principais (`BOTTOM_NAV_TAB_ORDER`), sub-rotas (`SubRoute`), editores/modais (`ActiveEditor`), deep-links (`nexus://open/...`) e confirmação de descarte de formulários sujos.
- **Decisão:** gerenciar navegação via estado Kotlin (`MainTab`, `SubRoute`, `ActiveEditor`) em `NexusAppRoot` + `BackHandler` ligado à função pura `resolveBackAction(...)`. Embora `androidx.navigation:navigation-compose` conste em `app/build.gradle.kts`, não utilizá-lo.
- **Alternativas:** migrar para `NavHost` com rotas tipadas/string.
- **Consequências:**
  - *Positivas:* testabilidade unitária pura da pilha lógica de retorno (`BackNavigationRules`), controle direto do estado `activeEditorDirty` e menor indireção em deep-links.
  - *Negativas:* `NexusAppRoot` concentra muito código de roteamento e `navigation-compose` permanece como dependência ociosa até limpeza.

---

### `ADR-002` — Injeção de Dependências Manual via `AppContainer`
- **Status:** Aceita e Ativa (`NexusApplication.kt`).
- **Contexto:** módulo Gradle único `:app` inicializando Room, repositórios, workers, agendador de notificações e módulos do Mercado.
- **Decisão:** utilizar container manual `AppContainer(context)` com propriedades `by lazy` em `NexusApplication`, repassado manualmente ao `NexusViewModel` e workers.
- **Alternativas:** adotar Hilt ou Koin.
- **Justificativa original:** *Não documentado nos registros originais* (padrão presente desde a baseline inicial do projeto; mantido pela regra de não adicionar dependências pesadas ou refatorar infraestrutura funcional sem necessidade).
- **Consequências:** build mais simples sem codegen adicional de DI, porém exige passagem manual de novas dependências na `Application` e fábrica de ViewModel.

---

### `ADR-003` — Arquitetura Local-First / Offline-First, Sem Dependência Obrigatória de Backend, Login ou Nuvem
- **Status:** Aceita e Ativa — vigente até eventual revisão formal (`AndroidManifest.xml`, `NexusDatabase.kt`). Não classificar como permanente ou irrevisável.
- **Contexto:** centro de comando pessoal com privacidade local e disponibilidade contínua sem conexão.
- **Decisão vigente:** manter os dados pessoais e funcionais persistidos localmente (`Room` + arquivos internos + `SharedPreferences`), desabilitar backup automático em nuvem (`android:allowBackup="false"`) e usar exportação/importação explícita via SAF. O núcleo deve permanecer útil sem conexão; integrações externas específicas, como o Mercado, podem usar rede e cache local.
- **Limite de interpretação:** esta decisão descreve a arquitetura adotada hoje, não uma proibição eterna de avaliar sincronização, serviços externos ou outros modelos. Qualquer mudança estrutural exige proposta, avaliação de impacto e aprovação explícita antes da implementação.
- **Alternativas:** sincronização em nuvem, Firebase ou conta de usuário.
- **Consequências:** reduz dependência operacional de infraestrutura própria e mantém controle local dos dados; em contrapartida, a transferência entre aparelhos depende do mecanismo de backup/exportação atualmente disponível. Não prometer privacidade absoluta nem ausência total de tráfego externo: consultar permissões, provedores e comportamento real do código.

---

### `ADR-004` — Evolução de Schema Estritamente Aditiva (`fallbackToDestructiveMigration` Proibido)
- **Status:** Aceita e Permanente (`NexusDatabase.kt`, `NexusMigrations.kt`).
- **Contexto:** usuários instalam atualizações em cima de dados financeiros e pessoais existentes.
- **Decisão:** proibir `fallbackToDestructiveMigration()`; exigir `Migration` explícita e aditiva a cada incremento de `DATABASE_VERSION`, mantendo `exportSchema = true`.
- **Consequências:** preserva instalações existentes (`v1→v6`); exige disciplina redobrada ao evoluir tabelas e atualizar `BackupModels`/`BackupManager` juntos.

---

### `ADR-005` — Armazenamento UTC + Fuso Fixo `America/Sao_Paulo` em `DateTimeUtils`
- **Status:** Aceita e Ativa (`DateTimeUtils.kt`).
- **Contexto:** evitar deslocamentos de dia ao passar por `NexusDatePickerDialog` (Material 3 opera internamente em UTC à meia-noite) ou mudar de horário perto da meia-noite.
- **Decisão:** persistir timestamps em epoch ms UTC e datas isoladas em `dateIso` (`YYYY-MM-DD`) / `yearMonth` (`YYYY-MM`), centralizando todas as conversões (`toLocalDate`, `startOfDayEpoch`, `epochToDatePickerMillis`, `datePickerMillisToEpoch`) em `DateTimeUtils` com `ZoneId.of("America/Sao_Paulo")`.
- **Consequências:** elimina bugs de dia-1 (`INT` histórico resolvido na Etapa 1); acopla exibição de calendário ao fuso brasileiro por design de produto.

---

### `ADR-006` — Modelo Contábil de Cartão/Fatura Sem Dupla Despesa e Atraso Dinâmico
- **Status:** Aceita e Permanente (`FinanceRules.kt`, `NexusRepository.kt`).
- **Contexto:** compras no cartão precisam entrar no orçamento/categoria do mês da compra sem duplicar despesa quando a fatura é paga, nem poluir a Agenda com cada compra parcelada individual.
- **Decisão:**
  1. Compras `CREDIT_CARD` ligam-se a `CreditCardStatementEntity` pelo `closingDay`; `payCreditCardStatement` liquida todas as transações da fatura sem criar segunda transação.
  2. `AgendaEngine` suprime transações com `statementId != null` e exibe apenas a fatura não paga (`total > 0`) no vencimento.
  3. Status `OVERDUE` é calculado dinamicamente na leitura (`FinanceRules.effectiveTransactionStatus` / `effectiveStatementStatus`), nunca gravado por job noturno; saldo declarado manual (`declaredBalanceCents`) nunca é sobrescrito pelo resultado mensal.
- **Consequências:** evita dupla contagem em gráficos, Agenda, widgets e briefings; exige que todo consumidor leia os status via `FinanceRules` / `AgendaEngine`.

---

### `ADR-007` — Kanban Compartilhado Sobre a Tabela Única `tasks`
- **Status:** Aceita e Ativa (`NexusEntities.kt`, `TasksAndProjectsScreen.kt`).
- **Decisão:** visualizar a aba Kanban agrupando diretamente a lista reativa de `TaskEntity` pelos 6 valores de `TaskStatus`, sem criar tabela separada de quadros/colunas.
- **Justificativa original:** princípio *"registrar uma vez, ver em vários lugares"* e separação clara entre status de execução (`TaskStatus`) e condição de prazo (`TaskRules.deadlineCondition`).
- **Consequências:** alteração de status no Kanban reflete instantaneamente em Lista, Home, Agenda e Widgets.

---

### `ADR-008` — Recorrência de Tarefas Materializada na Conclusão + Projeção Virtual na Agenda
- **Status:** Aceita e Ativa (`Lote 06 Recorrência`: `RecurrenceEngine.kt`, `AgendaEngine.kt`, `NexusEntities.kt`).
- **Contexto:** pré-gerar centenas de linhas futuras no SQLite polui tabelas e listas de tarefas; manter apenas regra sem linha aberta dificultaria histórico individual e subtarefas/gastos por execução.
- **Decisão:** guardar a regra em `recurrences` (`TaskRecurrenceEntity`), manter aberta no máximo a ocorrência corrente na tabela `tasks` (com `UNIQUE(recurrenceId, occurrenceDateIso)`), gerar a próxima ocorrência de forma idempotente somente ao concluir (`completeTask`) e projetar ocorrências futuras apenas em memória na `AgendaEngine` dentro da janela solicitada, respeitando `recurrence_exceptions` (`SKIP`/`OVERRIDE`).
- **Consequências:** zero inchaço no SQLite, histórico concluído preservado com subtarefas/gastos vinculados; adiamento de uma instância não altera a cadência futura. *(Expansão para Compromissos e Eventos segue adiada para lote futuro.)*

---

### `ADR-009` — Motor Único de Agenda (`AgendaEngine` em Transição sobre `observeUnifiedAgenda`)
- **Status:** Em Transição (`INT-01` parcialmente aberto).
- **Decisão:** centralizar expansão temporal, eventos multi-dia, filtros, supressão de compras de fatura e projeção de recorrências na função pura `AgendaEngine.expand(...)`.
- **Estado atual:** `AgendaScreen` utiliza `AgendaEngine`, mas `NexusRepository.observeUnifiedAgenda()` ainda permanece para `observeDashboard()`, `observeDailyBriefingSnapshot()` e `observeNightReviewSnapshot()`.
- **Consequência:** dívida técnica `INT-01` documentada no Roadmap para unificação total futura.

---

### `ADR-010` — Persistência do Módulo Mercado Isolada do Room (`SharedPreferences` + Arquivos JSON)
- **Status:** Aceita e Ativa (`Lotes 01–09`) — **Substitui** a proposta anterior da spec `v5` de tabelas `market_quotes` e `market_history` no Room.
- **Contexto:** spec antiga (`NEXUS_PRODUCT_SPEC` v5 / `NEXUS_SPEC.md`) previa tabelas Room para o Mercado; durante a evolução real nos Lotes 01–09, o módulo Mercado cresceu para 12 ativos, séries históricas por intervalo e alertas de preço.
- **Decisão:** armazenar cotações em cache e favoritos em `SharedPreferences`, séries históricas em `filesDir/market_history/` e alertas em `filesDir/market_alerts/market_alerts.json` (escrita atômica `.tmp` + `renameTo`).
- **Alternativas:** criar migrações Room para cotações, histórico, watchlist e alertas.
- **Consequências:**
  - *Positivas:* evoluiu 9 lotes do Mercado mantendo `DATABASE_VERSION = 6` estável e zero risco de migração SQLite para dados de mercado.
  - *Negativas:* Backup `v7` exige coordenação explícita de rollback entre Room e stores externos (`ADR-013`). Caso se deseje atomicidade física única no futuro, favoritos e alertas podem ser migrados para Room em próxima versão de schema.

---

### `ADR-011` — Provedores Públicos Gratuitos com Fallback e Sync Periódico no Mercado (Sem Token Obrigatório)
- **Status:** Aceita e Ativa (`Lotes 01–09`) — **Substitui** a proposta antiga `v5` que sugeria token pessoal `brapi` para Ibovespa e proibia worker em background.
- **Decisão:**
  1. Roteamento por categoria (`CategoryRoutedMarketQuoteRemoteDataSource` e `AssetCategoryRoutedHistoryDataSource`) usando exclusivamente `HttpsURLConnection` nativo e APIs públicas sem necessidade de token obrigatório, com cadeias de fallback por ativo/categoria.
  2. Preservação explícita da moeda/unidade nativa (`BRL`, `USD`, `pts`) e modalidade (`COMMERCIAL`, `PTAX`, `SPOT`, `EXCHANGE`, `DELAYED`, `INDEX`), com conversão informativa opcional em BRL via `USD-BRL`.
  3. Cooldown contra chamadas repetidas + `MarketQuoteSyncWorker` (30 min via `WorkManager` com `NetworkType.CONNECTED`) para suportar avaliação automática de alertas locais de preço (`Lote 07`).
- **Consequências:** experiência imediata para 12 ativos sem configuração manual de chave; sujeito a eventuais mudanças de layout ou *rate limits* de APIs públicas de terceiros (mitigado por fallbacks e cache offline).

---

### `ADR-012` — Gráficos Nativos em Jetpack Compose `Canvas` + Token `accentNeon`
- **Status:** Aceita e Ativa (`NexusTheme.kt`, `ui/components/charts/*`).
- **Decisão:** implementar todos os gráficos (`TaskRingChart`, `DonutChart`, `BarGroupChart`, `ProgressBarRow`, `Sparkline`, `MarketHistoryChart`) diretamente em `Canvas` do Compose sem dependências de terceiros, adicionando `accentNeon`, `onAccentNeon`, `chartSecondary`, `chartTrack` e `chartPalette` (8 cores) nos temas Escuro e Claro.
- **Consequências:** APK enxuto, controle absoluto sobre gestos (`pointerInput` no `DonutChart`), acessibilidade (`semantics`) e aderência total ao design system; exige cuidado com tipos geométricos em scopes diferentes (ex.: `IntSize` vs `Size`).

---

### `ADR-013` — Backup JSON `v7` com Rollback Coordenado e Semântica `null` vs `[]`
- **Status:** Aceita e Ativa (`Lotes 08–09`: `BackupModels.kt`, `BackupManager.kt`).
- **Decisão:** evoluir `CURRENT_FORMAT_VERSION` para `7` adicionando `marketConfig: MarketBackupConfigDto?` (apenas favoritos e alertas válidos; fora cache de cotações e histórico). Na importação:
  - `marketConfig == null` (ou campo `null` em `v1..v6`) **preserva** configurações locais existentes no aparelho;
  - lista explícita (`[]` ou populada) **substitui** os dados após validação pelo `MarketAssetCatalog` e sanitização de estados;
  - mutação dos stores do Mercado ocorre no fim de `database.withTransaction` com `rollbackMarketConfig` em exceções pré ou pós-commit do Room;
  - restauração não dispara avaliação nem notificações de alertas.
- **Consequências:** portabilidade total entre aparelhos mantendo retrocompatibilidade `v1..v7` e restauração silenciosa; limite físico residual apenas contra `SIGKILL` exato entre commit Room e `.apply()`/`renameTo` do SO.

---

### `ADR-014` — Widgets Jetpack Glance com Suporte Dual Claro/Escuro via `DayNightColorProvider`
- **Status:** Aceita e Ativa (`NexusWidgetTheme.kt`).
- **Contexto:** `INT-10` apontava widgets fixos no tema escuro enquanto o app possuía tema Claro e Escuro.
- **Decisão:** utilizar `DayNightColorProvider` (`ColorProvider(day, night)`) em `NexusWidgetColors` espelhando tokens equivalentes de `NexusPalette` claro e escuro, com `SizeMode.Responsive` e confirmação em 2 etapas para conclusão de tarefa em widget.
- **Consequências:** legibilidade consistente em launchers claros e escuros sem depender de reconfiguração manual por tema.

---

### `ADR-015` — Infraestrutura de Assinatura Persistente Externa para Atualização In-Place (`com.nexus.app`)
- **Status:** Aceita e Ativa (`app/build.gradle.kts`, `keystore.properties.example`, `.gitignore`, `.github/workflows/android-build.yml`).
- **Contexto:** o pipeline de CI (`ubuntu-latest`) compilava `:app:assembleDebug` sem keystore persistente, gerando um certificado debug efêmero por ambiente e provocando `INSTALL_FAILED_UPDATE_INCOMPATIBLE` ao tentar atualizar o app por cima de uma instalação existente (forçando desinstalação e perda do `nexus_local.db`, dado `android:allowBackup="false"`). Na Etapa 2, exigiu-se também falha rápida contra configurações parciais e verificação criptográfica pós-build.
- **Decisão:**
  1. Preservar `applicationId = "com.nexus.app"` e incrementar `versionCode` / `versionName` a cada versão (`8` / `"5.3.0"`).
  2. Configurar `signingConfigs` (`debug` e `release`) em `app/build.gradle.kts` lendo `keystore.properties` na raiz (ignorado no Git; modelo em `keystore.properties.example`) ou variáveis de ambiente (`NEXUS_DEBUG_KEYSTORE_*`, `NEXUS_KEYSTORE_*`), com **falha rápida (`GradleException`)** em configurações parciais (`1..3` de `4` campos) ou arquivos ausentes e bloqueio explícito de tarefas `assembleRelease`/`bundleRelease`/`packageRelease` sem assinatura `release` válida.
  3. No `.github/workflows/android-build.yml`, inicializar o SDK com `setup-android@v4` (`platform-tools`), instalar `"platform-tools"` e `"build-tools;35.0.0"` via `sdkmanager` em argumentos independentes validando `adb` e `apksigner`, validar secrets via `keytool -list -v` (`-storepass:env`), compilar `:app:assembleRelease` quando os 4 secrets `NEXUS_KEYSTORE_*` existirem, verificar todos os APKs gerados com `apksigner verify --verbose --print-certs` comparando o SHA-256 real contra o keystore configurado e publicar resumo público em `$GITHUB_STEP_SUMMARY` e `NEXUS-Signing-Verification-Report`.
  4. Nunca versionar `.jks`/`.keystore`/senhas nem imprimir valores secretos em logs; manter `:app:assembleDebug` funcional quando nenhuma assinatura Debug externa for configurada.
- **Alternativas:** commitar keystore de debug no repositório ou permitir builds release não assinados/configurações silenciosamente ignoradas.
- **Consequências:** permite atualizações in-place preservando Room e stores locais com verificação automatizada de fingerprint SHA-256; erros de secrets são detectados antes de distribuir APKs incompatíveis.

---

### `ADR-016` — Mesclagem Incremental Idempotente de Backups com Prévia Dry-Run (`BackupStorageGateway` e `BackupMergeEngine`)
- **Status:** Aceita e Ativa (`BackupStorageGateway.kt`, `BackupMergeEngine.kt`, `BackupManager.kt`, `NexusRepository.kt`, `MoreSettingsBackupDiagnosticsScreens.kt`).
- **Contexto:** até `v5.2.0`, a importação de backup (`BackupManager.restoreAtomically`) sempre executava `deleteAll` em todas as 27 tabelas, impossibilitando incorporar dados de outro backup ou aparelho sem apagar os registros já existentes no uso cotidiano. Na Etapa 2, reforçou-se a previsibilidade pré-mesclagem e a proteção contra restauração destrutiva acidental.
- **Decisão:**
  1. Preservar `restoreAtomically` como **Modo 1 (Restauração Completa)** destrutivo — em fluxo/diálogo separado com checkbox explícito de confirmação destrutiva — e manter **Modo 2 (Mesclagem Incremental)** via `BackupMergeEngine` + `BackupStorageGateway` (`mergeBackupAtomically`).
  2. Compartilhar exatamente o mesmo planejador puro `BackupMergeEngine.computeMergePlan` entre `previewMerge(..., isDryRun = true)` e `mergeAtomically` (`isDryRun = false`), garantindo que a prévia exiba contagens e conflitos reais sem executar nenhuma escrita no Room ou nos stores do Mercado.
  3. Isolar o acesso transacional às 27 tabelas atrás da interface `BackupStorageGateway` (`RoomBackupStorageGateway` em produção dentro de `database.withTransaction`; `InMemoryBackupStorageGateway` em testes unitários JVM puros).
  4. Adotar estratégia conservadora na mesclagem: ordem topológica (níveis 0–4), zero exclusões implícitas (`deleteAll` proibido), preservação integral dos dados locais e singletons em qualquer colisão por `id` ou chave natural `UNIQUE`, rejeição apenas do item com FK obrigatória inválida, nulificação de FK opcional quebrada com `NEEDS_REVIEW`, proteção financeira estrita (faturas únicas e bloqueio anti-dupla despesa `"pagamento de fatura"`) e união incremental de `marketConfig` com rollback coordenado Room ↔ Mercado.
- **Alternativas:** sobrescrever registros locais pelo `updatedAt` mais recente (*last-write-wins*), usar estimativa separada para o preview ou compartilhar um único diálogo genérico para os dois modos.
- **Consequências:** garante idempotência, fidelidade absoluta entre prévia dry-run e execução real e proteção contra perda silenciosa de edições locais; divergências entre registros com mesmo ID exigem inspeção do `MergeSummary` em vez de sobrescrita automática.

---

### `ADR-017` — Camada Interna de Entrada Estruturada (`contractVersion = 1`) Sobre Banco Room Único `v6` (`audit_history`)
- **Status:** Aceita e Ativa (`data/ingestion/StructuredInputModels.kt`, `StructuredInputGateway.kt`, `StructuredInputProcessor.kt`, `NexusRepository.kt`, `docs/14_CONTRATO_ENTRADA_ESTRUTURADA_ETAPA3.md`).
- **Contexto:** preparar o NEXUS na Etapa 2 para receber criações estruturadas de domínio (`TASK`, `APPOINTMENT`, `EVENT`, `PROJECT`, `PERSONAL_GOAL`, `TRANSACTION`) a partir de futuras fontes externas (Etapa 3) sem antecipar transporte de rede/GitHub API e sem criar um segundo banco SQLite ou alterar o schema Room `v6`.
- **Decisão:**
  1. Manter o **banco Room único `NexusDatabase` (`DATABASE_VERSION = 6`)**, gravando diretamente nas tabelas oficiais via `RoomStructuredInputGateway` dentro de `database.withTransaction` e atualizando lembretes/widgets via `NexusRepository`.
  2. Registrar a idempotência por `operationId` na tabela existente `audit_history` (`entityType = "STRUCTURED_INPUT_OP"`, `entityId = operationId`, `action = state.name`) exclusivamente para estados persistidos (`APPLIED` e `CONFLICT_PRESERVED_LOCAL`).
  3. Impor contrato versionado (`contractVersion = 1`) com JSON estrito (`ignoreUnknownKeys = false`), validação rigorosa de campos obrigatórios, enums, FKs existentes e regras financeiras (`PAID` statement / anti-dupla despesa `ADR-006`), preservação local em colisões por ID/chave natural e suporte a modo por item (`atomicBatch = false`) ou lote atômico (`atomicBatch = true`).
  4. Manter `FAILED_ROLLED_BACK` e `REJECTED` fora de `audit_history` para que falhas transacionais revertam integralmente e possam ser reprocessadas com o mesmo `operationId` sem falso `DUPLICATE_IGNORED`.
- **Alternativas:** criar banco SQLite paralelo de staging, migrar `DATABASE_VERSION` para `7` adicionando tabela de inbox antes da Etapa 3 ou já acoplar sincronização HTTP/GitHub na Etapa 2.
- **Consequências:** zero impacto de migração em instalações existentes, compatibilidade imediata com Backup `v7` e Widgets/Agenda, e fronteira limpa e testável (`StructuredInputUnitTests`) pronta para o conector da Etapa 3.


