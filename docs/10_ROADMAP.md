# 10 — Roadmap Consolidado do NEXUS

> **Princípio organizador:** separação rigorosa entre o que já existe **no código atual**, o que é **trabalho planejado ainda não iniciado**, o que é **dívida técnica / manutenção** e o que está **explicitamente fora de escopo**.

---

## 1. Concluído no Código Atual

### 1.1 Núcleo do Aplicativo e Evolução Base (`v5.3.0` · Room `v8`)
- **Arquitetura base, Room `v8` (`28` tabelas), migrações `v1→v8` (`MIGRATION_6_7` + `MIGRATION_7_8`) e Diagnóstico (`GlobalErrorHandler`, `DiagnosticsScreen` + Restauração de Fábrica)**.
- **Shell de Navegação (`MainActivity` + `BackNavigationRules`):** barra inferior na ordem `TASKS, AGENDA, HOME, FINANCE, EVENTS`, `BackHandler` conectado a `activeEditorDirty` e sub-rotas top-level vs child.
- **Dashboard (`HomeScreen`):** cabeçalho compacto integrando saudação + data + frase diária discreta, `9` blocos reordenáveis/desligáveis, `TaskRingChart`, resumo financeiro compartilhado (`FinanceRules`), indicadores de projetos/objetivos, `AlertBannerCard` para alertas reais e próximos compromissos/eventos.
- **Tarefas e Projetos (`TasksAndProjectsScreen` + `RecurrenceEngine`):** lista executável sem mistura de contas financeiras, Kanban sobre a mesma tabela `tasks`, status separados da condição de prazo, subtarefas, vínculo Tarefa → Gasto (`transactions.taskId`, `"Tarefa: <título>"`) e **Lote 06 de Recorrência de Tarefas** (`task_recurrences`, `recurrence_exceptions`, `"Só esta"` vs `"Toda a série"`, projeção virtual e materialização idempotente).
- **Agenda (`AgendaScreen` + `AgendaEngine`):** 4 visões (`DAY`, `WEEK` horizontal seg–dom, `MONTH` inicial com marcadores, `LIST`), painel único de filtros por tipo/categoria contextual/`showPaid`, eventos multi-dia e detecção de conflitos de compromissos.
- **Financeiro Visual Completo (`FinanceScreen` + `FinanceRules`):** seletor `< mês/ano >`, `DonutChart` interativo com filtro na lista, `BarGroupChart` 6 meses (`Realizado | Previsto` + `"Resultado do mês"`), previsão 30 dias, orçamentos (`ProgressBarRow`), cartões/faturas sem dupla despesa, parcelamentos, recorrentes financeiras, empréstimos, metas e CSV 15 colunas.
- **Objetivos Pessoais (`PlanningGoalsScreen`) & Rotina (`RoutineAssistantScreen`):** modos `AUTO`/`MANUAL`, marcos, logs, Briefing Diário, Revisão Noturna com adiamento em 1 toque e `briefing_history`.
- **Notificações, 4 Widgets Glance Dual-Theme (`DayNightColorProvider`), Biblioteca de Frases, Busca Global e Backup JSON `v1..v7`.**
- **Evolução Visual e Adaptativa (2026-11-22):** tema claro **Grafite Suave** (`NexusTheme` e `NexusWidgetTheme`), revisão proporcional de contornos coloridos (`ColoredBorderRole`) em ambos os temas com bordas neutras preservadas, widgets Glance sem contorno externo, identidade tipográfica `NexusWordmark` (**N-1 — Monólito Chanfrado** + **E, X, U, S** *Kinetik Cut*), remoção do `N` verde redundante na TopBar (`COR-001`), launcher icon adaptativo **N-1**, saudação da Home na mesma linha e adaptação para telas maiores/tablets (`AgendaScreen` 7 dias vs deslizável, altura dinâmica, 2 painéis em tablet paisagem; 2 colunas/2 painéis em `HomeScreen`, `FinanceScreen`, `TasksAndProjectsScreen`, `EventsScreen`, `PlanningGoalsScreen` e `MarketScreen`).
- **Rodada 1 — Confiabilidade, Atualização In-Place e Mesclagem Incremental (2026-11-25 · `v5.3.0`, `versionCode = 8`):**
  - *Infraestrutura de atualização sem desinstalação:* preservação estrita de `applicationId = "com.nexus.app"`, incremento de `versionCode = 8` / `versionName = "5.3.0"`, configuração externa opcional de assinatura (`debug` e `release`) em `app/build.gradle.kts` via `keystore.properties` (modelo `keystore.properties.example`, `.gitignore` protegido) ou variáveis de ambiente, e suporte opcional a secrets no `.github/workflows/android-build.yml` (`NEXUS_DEBUG_KEYSTORE_BASE64`, `NEXUS_KEYSTORE_BASE64`).
  - *Mesclagem Incremental de Backups (Modo 2):* `BackupStorageGateway` + `BackupMergeEngine` + `BackupManager.mergeBackupAtomically` preservando `restoreAtomically` (Modo 1), com ordem topológica, preservação local em conflitos/singletons, políticas de FK (`needsReview` vs rejeição), coerência financeira (cartões, faturas, parcelas, recorrentes e anti-dupla despesa), distinção explícita na `BackupScreen`, resumo `MergeSummary` e suíte `BackupIncrementalMergeUnitTests`.
- **Etapa 2 de 4 — Finalização de Backup (Prévia Dry-Run + UX Separada), Entrada Estruturada Interna (`v1`) e Endurecimento de Assinatura/CI (2026-11-25 · `v5.3.0`):**
  - *Frente A (Backup Dry-Run + Separação de Fluxos):* `BackupMergeEngine.computeMergePlan` compartilhado com `previewMerge(..., isDryRun = true)` (zero gravações no Room e no Mercado), separação completa dos diálogos de Restauração Completa (com checkbox explícito de confirmação destrutiva) e Mesclagem Incremental (com prévia real antes de confirmar e resumo `MergeSummary` em pt-BR), ampliando `BackupIncrementalMergeUnitTests` para `13` `@Test`.
  - *Frente B (Entrada Estruturada Interna `contractVersion = 1`):* pacote `data/ingestion/` (`StructuredInputModels.kt`, `StructuredInputGateway.kt`, `StructuredInputProcessor.kt`) integrado ao `NexusRepository` (`processStructuredInputJson` / `processStructuredInputEnvelope`) sobre Room `v6` + `audit_history` (`STRUCTURED_INPUT_OP`), com JSON estrito, FKs e regras financeiras, rollback seguro e `StructuredInputUnitTests` (`7` `@Test`), **naquele momento sem transporte externo; o transporte e a sincronização pertencem à Rodada 3** (contrato documentado em [`14_CONTRATO_ENTRADA_ESTRUTURADA_ETAPA3.md`](./14_CONTRATO_ENTRADA_ESTRUTURADA_ETAPA3.md)).
  - *Frente C (Pipeline de Assinatura & Verificação `apksigner`):* `app/build.gradle.kts` falha rapidamente em configurações parciais (`1..3/4`) e bloqueia `assembleRelease`/`bundleRelease`/`packageRelease` sem assinatura `release`; `.github/workflows/android-build.yml` adiciona `build-tools;35.0.0`, valida keystores via `keytool -list -v` (`-storepass:env`), verifica APKs via `apksigner verify --verbose --print-certs` comparando o SHA-256 real com o keystore e publica `NEXUS-Signing-Verification-Report` + `$GITHUB_STEP_SUMMARY`.
- **Rodada 4 (Núcleo de Recorrência, Checklists Independentes, Ponte v1 Expandida e Restauração de Fábrica — `2026-11-26` a `2026-11-27` · `v5.3.0` · Room `v8` / Backup `v9`):**
  - *Recorrência de Compromissos e Eventos (`REQ-009` / `GAP-001`):* `TaskRecurrenceEntity` generalizada com `RecurrenceDomain` (`TASK`, `APPOINTMENT`, `EVENT`), nova frequência `ANNUAL`, overrides específicos em `RecurrenceExceptionEntity`, projeção virtual em `AgendaEngine` e `EventsScreen`, detecção de conflitos virtual↔materializado, materialização idempotente sob demanda (`"rec::<recurrenceId>::<occurrenceDateIso>"`), escopos `"Só esta ocorrência"` vs `"Toda a série"`, lembretes virtuais e proteção financeira integral em Eventos (projeções/materialização sob demanda nunca criam `TransactionEntity`, e séries editadas/excluídas preservam ocorrências `ATTENDED` ou com ingresso `PAID`/`SETTLED`).
  - *Independência de Checklists + Checklists em Compromissos/Eventos (`GAP-003` + `GAP-004` / `REQ-029`):* nova tabela `schedule_checklist_items` (`ScheduleChecklistItemEntity`) suportando criação, renomeação inline, exclusão, reordenação, contagem `concluídos/total` e barra de progresso em `AppointmentEditorSheet`, `EventEditorSheet`, `EventSummaryCard` e `AgendaScreen`; isolamento absoluto por ocorrência `(recurrenceId, occurrenceDateIso)` e versionamento não retroativo de templates (`checklistTemplateVersion` + `templateHistoryJson` com `ChecklistTemplateSnapshot(version, effectiveFromDateIso, items)`), preservando ocorrências passadas e exceções ao editar `"Toda a série"`.
  - *Expansão `contractVersion = 1` da `NEXUS-INBOX` (`GAP-002`):* inclusão de `StructuredEntityType.RECURRENCE` (`StructuredRecurrenceData` para `TASK`/`APPOINTMENT`/`EVENT` com `templateChecklist` e `effectiveFromDateIso` não retroativo) e do campo opcional `checklist: List<StructuredSubtaskItem>` em `APPOINTMENT` e `EVENT`, mantendo 100% de retrocompatibilidade do contrato `v1`, `ignoreUnknownKeys = false`, dry-run e idempotência.
  - *Restauração de banco de dados de fábrica:* fluxo em **Configurações → Dados e armazenamento → Restaurar banco de dados de fábrica** com exportação SAF direta antes do reset, dupla confirmação explícita (checkbox obrigatório + `"APAGAR E RESTAURAR"`), travas exclusivas (`InboxRepository` → `NexusRepository`), limpeza transacional das `28` tabelas + re-seed padrão + limpeza de Mercado e rastreamento local da Inbox com rollback coordenado, preservando PAT Keystore e coordenadas GitHub da `NEXUS-INBOX`.
  - *Backup `v9` & Suítes JUnit:* `NexusBackupPayload.CURRENT_FORMAT_VERSION = 9` (retrocompatível `v1..v9` com `scheduleChecklistItems`, `checklistTemplateVersion`/`templateHistoryJson` e ciclo opcional `recurrences ↔ events`), `NexusRodada4RecurrenceUnitTests.kt` (`5` `@Test`), `OccurrenceChecklistIsolationUnitTests.kt` (`12` `@Test`), `FactoryResetUnitTests.kt` (`10` `@Test`) e ampliações em `BackupIncrementalMergeUnitTests.kt` (`15`), `StructuredInputUnitTests.kt` (`12`) e `InboxBridgeUnitTests.kt` (`5`), totalizando `21` suítes / `170` `@Test` (**aguardando Gradle/CI e validação em dispositivo**).


### 1.1.3 Rodada 3 — Ponte ChatGPT → NEXUS-INBOX → Android (**CONCLUÍDA — 2026-11-26**)

A ponte foi implementada no código Android, corrigida e integrada à `main`. O fluxo real foi validado pelo usuário no aplicativo: conexão ao repositório privado `NEXUS-INBOX` e importação de registros por meio da Inbox.

**Evidências de conclusão:**
- Commit na `main`: `4041611a4ef4e111c3633790d0c09babdecb2ae8` — `refactor: enhance inbox parsing and UI terminology`.
- GitHub Actions #70 (run `45792119560`) concluída com sucesso na `main`, incluindo testes unitários e geração do APK Debug. Release permaneceu ignorado por ausência dos secrets de assinatura.
- O usuário confirmou que o aplicativo funcionou e que conseguiu importar registros pela ponte ChatGPT → `NEXUS-INBOX` → NEXUS Android.
- O repositório privado `NEXUS-INBOX` foi localizado e contém arquivos JSON de teste e povoamento.

**Limites da evidência:** a confirmação comprova a importação real. Não registrar como individualmente testados descarte sem gravação, reconsulta sem duplicidade, execução periódica em segundo plano, notificações/deep-links, reinício/rotação ou todos os cenários de segurança do token sem confirmação específica. Esses cenários permanecem no gate contínuo de confiabilidade.

**Critérios de encerramento atendidos:** correções na `main`, CI verde e importação real no aparelho. A versão atual da Inbox permanece funcional como ponte de entrada v1. A ampliação do contrato para recorrências e a integração bidirecional definitiva foram adiadas para a Rodada 6, depois que o domínio do aplicativo e sua experiência estiverem estabilizados.

### 1.2 Módulo Mercado Financeiro (`Lotes 01 a 09` — Módulo Concluído e Encerrado)
- **Lotes 01–02:** infraestrutura offline-first em `SharedPreferences`, roteamento e fallback de provedores públicos sem token para `USD-BRL`, `EUR-BRL`, `BTC-BRL`, `ETH-BRL`, `SOL-BRL`.
- **Lote 03:** área própria `MarketScreen` + detalhe `MarketAssetDetailScreen` + `MarketPresentationMapper`.
- **Lote 04:** expansão para **12 ativos** com Índices (`IBOV`, `SPX`, `IXIC`) e Ações (`PETR4`, `VALE3`, `ITUB4`, `MELI`), preservando moedas/unidades nativas e conversão informativa via `USD-BRL`.
- **Lote 05:** séries históricas (`1D` a `1Y`, com `1D` omitido em moedas), cache em `filesDir/market_history/` e componente `MarketHistoryChart`.
- **Lote 06:** lista de acompanhamento (*Watchlist*) deduplicada e toggle rápido na listagem e no detalhe.
- **Lote 07:** alertas locais de preço (`PRICE_ABOVE`/`PRICE_BELOW`) na unidade original do ativo, proteção anti-duplicidade/rearme, avaliação pós-sync (`MarketQuoteSyncWorker` 30m e manual), canal `CHANNEL_MARKET_ALERTS` e deep-link para o ativo.
- **Lote 08:** Backup JSON `v7` exportando e restaurando exclusivamente favoritos e alertas pessoais (`marketConfig`) com validação e sanitização, preservando compatibilidade `v1..v6` (`null` vs `[]`).
- **Lote 09:** auditoria ponta a ponta dos Lotes 01–08, sincronização de rótulos/descrições de interface em `MoreScreen`/`BackupScreen`, e rollback coordenado Room ↔ Mercado em `BackupManager`.

---

## 2. Sequência revisada de desenvolvimento

> **Reorganização de 26/11/2026.** As Rodadas 1, 2 e 3 estão concluídas; o fluxo principal da ponte foi validado por CI e importação real no aparelho. A Rodada 4 é a etapa atual para completar a base funcional e as lacunas de domínio. A evolução da experiência passa a ser a Rodada 5, baseada na documentação conceitual atualizada em `NEXUS-DOCUMENTACAO/CHATGPT/ATUALIZACAO/`, não na adoção automática da proposta original do Claude.

| Etapa | Estado | Objetivo e entregas |
|---|---|---|
| **Rodada 1 — Atualização e proteção dos dados** | **CONCLUÍDA** | Atualização in-place, assinatura configurável, backup incremental, merge transacional e testes unitários. Validações em aparelho e keystore persistente continuam no gate de release. |
| **Rodada 2 — Importação e entrada estruturada** | **CONCLUÍDA** | Prévia dry-run, restauração/mesclagem separadas, contrato estruturado v1, validação, idempotência e rollback. |
| **Rodada 3 — Ponte ChatGPT → GitHub → Android** | **CONCLUÍDA — fluxo principal validado em 26/11/2026** | Correções integradas à `main`, GitHub Actions #70 verde e importação real confirmada pelo usuário no aparelho. Descarte sem gravação, reconsulta sem duplicidade, sincronização periódica, notificações/deep-links e todos os cenários de segurança permanecem no gate contínuo, sem evidência individual de teste. |
| **Rodada 4 — Completar o sistema funcional e fechar lacunas de domínio** | **EM ANDAMENTO — `GAP-001`, `GAP-002`, `GAP-003`, `GAP-004` e Restauração de Fábrica implementados no código** | Recorrência de Compromissos e Eventos (`REQ-009` / `GAP-001`), independência de checklists (`GAP-003`), checklists em Compromissos/Eventos com versionamento não retroativo (`REQ-029` / `GAP-004`), operação `RECURRENCE` + checklists na Inbox (`GAP-002`) e Restauração de Fábrica implementados em Room `v8` / `MIGRATION_7_8` / Backup `v9` (aguardando Gradle/CI e validação em aparelho). Restam fora deste lote: identidade contextual (`GAP-005` / `REQ-030`), pendências de Eventos/backup ZIP (`REQ-012`, `REQ-014`, `REQ-026`), consistência da Agenda (`INT-01`), checklist rascunho de Tarefa (`INT-03`) e estado em rotação (`BUG-001`). |
| **Rodada 5 — Arquitetura da informação e evolução da experiência** | **POSTERIOR — EM ANÁLISE** | Auditar o aplicativo funcional completo; mapear entidades e relações; decidir arquitetura de informação; definir linguagem visual; especificar componentes, fichas e interações; prototipar e validar; só então implementar em lotes pequenos. A proposta Claude permanece referência, não especificação aprovada. |

### 2.1 Escopo da Rodada 4 — completar o núcleo funcional

A Rodada 4 prioriza a completude funcional do domínio antes da reformulação ampla da experiência. As frentes de recorrência multi-domínio, checklists independentes em `TASK`/`APPOINTMENT`/`EVENT`, extensão `RECURRENCE` na Inbox e restauração de fábrica foram produzidas no código, mas permanecem condicionadas à compilação/testes no CI e à validação em dispositivo.

1. **Recorrência de Compromissos e Eventos (`REQ-009` / `GAP-001`) — IMPLEMENTADA NO CÓDIGO; validação pendente:** modelo generalizado via `RecurrenceDomain`, frequência `ANNUAL`, exceções `SKIP`/`OVERRIDE`, edição de ocorrência/série, projeção virtual na Agenda e em Eventos, materialização sob demanda, lembretes e migração Room `v6→v7` (`MIGRATION_6_7`).
2. **Checklists independentes em Tarefas, Compromissos e Eventos (`GAP-003` + `GAP-004` / `REQ-029`) — IMPLEMENTADO NO CÓDIGO; validação pendente:** `schedule_checklist_items` em Room `v8` (`MIGRATION_7_8`), isolamento por ocorrência `(recurrenceId, occurrenceDateIso)` e versionamento não retroativo via `checklistTemplateVersion` + `templateHistoryJson` (`ChecklistTemplateSnapshot`), com backup `v9`.
3. **Recorrência e checklists na Ponte `NEXUS-INBOX` (`GAP-002`) — IMPLEMENTADO NO CÓDIGO; validação pendente:** `StructuredEntityType.RECURRENCE` (`StructuredRecurrenceData` para `TASK`/`APPOINTMENT`/`EVENT`) e `checklist` opcional em `APPOINTMENT`/`EVENT` sobre `contractVersion = 1`. Permanece adiada para a **Rodada 6** apenas a integração bidirecional / catálogo completo (`GAP-006`).
4. **Restauração de banco de dados de fábrica — IMPLEMENTADA NO CÓDIGO; validação pendente:** opção em **Configurações → Dados e armazenamento** com pré-backup SAF opcional, dupla confirmação explícita, re-seed limpo das `28` tabelas + Mercado e preservação de PAT/coordenadas da `NEXUS-INBOX`.
5. **Identidade contextual em Compromissos/Eventos (`GAP-005` / `REQ-030`):** catálogo controlado de ícones contextuais sem confundir contexto com status ou ação.
6. **Completar Eventos e dados associados:** resolver `REQ-012` (gestão de compromissos vinculados), `REQ-014` (campos avançados e banner) e `REQ-026` (backup ZIP com banners), respeitando migrações e compatibilidade.
7. **Consistência funcional e técnica:** resolver `INT-01` (motor legado da Agenda), `INT-03` (rascunho de checklist em `TaskEditorSheet`) e `BUG-001` (preservação de rota/rascunhos), após confirmar o comportamento atual e os testes necessários.
8. **Auditoria de completude:** percorrer requisitos e rastreabilidade, identificar funcionalidades parcialmente implementadas ou não verificadas, classificar por impacto e decidir explicitamente o que precisa ser concluído antes da reformulação. Incluir cenários combinando recorrência, checklists, relações, contexto, notificações, backup e histórico.

O guia operacional do ChatGPT (`docs/16_GUIA_OPERACIONAL_CHATGPT_NEXUS.md`, atualizado para versão `1.1.0`) reflete integralmente os DTOs, enums, processador, conector `data/inbox/` e testes reais do contrato `v1` (incluindo `RECURRENCE` e `checklist` em `APPOINTMENT`/`EVENT`), mantendo explícito o que permanece fora do contrato (`GAP-005`, `GAP-006`). Por regra permanente (`docs/09_GUIA_DESENVOLVIMENTO.md` Seção 5.C), o guia continuará sendo atualizado no mesmo lote sempre que futuras implementações alterarem entidades, campos, relações, regras ou contratos acessíveis ao ChatGPT.

Cada item deve ter especificação, implementação, testes automatizados adequados e validação no aplicativo real quando depender de Android/Room/WorkManager/UI. O registro `15_REGISTRO_LACUNAS_E_FALHAS.md` mantém as evidências e o estado de cada pendência.


### 2.2 Escopo da Rodada 5 — reformulação da experiência

A Rodada 5 não é “implementar o Claude” nem fazer redesign de uma vez. Seguir a sequência de investigação registrada em `NEXUS-DOCUMENTACAO/CHATGPT/ATUALIZACAO/05_PLANO_DE_EVOLUCAO_E_QUESTOES_ABERTAS.md`:

1. Auditoria da experiência e implementação atual.
2. Modelo mental, mapa de entidades e relações.
3. Arquitetura da informação e decisão humana sobre destinos/navegação.
4. Linguagem visual, semântica, cores, símbolos e proporções.
5. Componentes, cards, fichas especializadas e contratos de interação.
6. Protótipos e validação de fluxos em celular/tablet.
7. Planejamento técnico aprovado e implementação incremental.
8. Refinamento, acessibilidade, desempenho e validação contínua.

A atualização conceitual posterior ao documento original do Claude refinou pontos importantes: fichas especializadas, consulta separada de edição, componentes proporcionais, relações sem duplicar registros e navegação ainda em análise. A arquitetura de cinco destinos do Claude não está aprovada. Preservar a navegação atual até decisão formal.

### 2.3 Gate contínuo de confiabilidade e release

Não é uma rodada isolada: acompanha todas as etapas. Inclui keystore persistente, atualização in-place, migrações Room, backup/restauração/merge, testes de regressão, segurança do token, notificações, desempenho e validação em aparelho. Build verde não substitui validação prática.

## 3. Dívidas Técnicas e Manutenção Recomendada

| ID | Descrição | Arquivos Envolvidos | Risco / Impacto |
| --- | --- | --- | --- |
| **`BUG-001`** | Preservar estado de navegação (`MainTab`, `SubRoute`, `ActiveEditor`) e rascunhos locais durante rotação/recriação da `Activity` (`rememberSaveable` / `SavedStateHandle` + guarda de `incomingIntentFlow`) | `MainActivity.kt`, telas e editores | Alto na experiência de rotação (permanece **em aberto**; fora do escopo do lote visual) |
| **`INT-01`** | Aposentar o motor legado `observeUnifiedAgenda()` e migrar `observeDashboard()`, `observeDailyBriefingSnapshot()` e `observeNightReviewSnapshot()` integralmente para `AgendaEngine` | `NexusRepository.kt:494+` | Médio — reduz risco de divergência entre Agenda, Dashboard e Rotina |
| **`INT-03` (residual)** | Remover gravação imediata de `onToggleSubtask` dentro de `TaskEditorSheet` antes de o usuário clicar em `"Salvar"` (usar rascunho em memória até confirmação) | `EntityEditors.kt`, `MainActivity.kt` | Baixo — evita mutação silenciosa se usuário descartar edição |
| **Dependência ociosa** | Remover `androidx.navigation:navigation-compose:2.8.5` do catálogo e `app/build.gradle.kts` | `app/build.gradle.kts:65`, `gradle/libs.versions.toml` | Baixo — reduz tamanho do build e evita confusão arquitetural |
| **Modularização gradual** | Quebrar arquivos monolíticos extensos (`NexusRepository.kt` ~4.914 linhas, `EntityEditors.kt` ~2.378, `FinanceScreen.kt`, `NexusViewModel.kt` ~1.521) conforme novos lotes tocarem nessas áreas | `data/repository/`, `ui/` | Médio — melhora legibilidade e tempo de análise K2 |
| **Infraestrutura de build/testes** | Adicionar `./gradlew` na raiz, gerar/versionar `app/schemas/` após KSP real, provisionar os secrets/keystore persistente para ativar assinatura consistente entre releases e iniciar cobertura `androidTest` (migrações Room + UI crítica) | Raiz, `app/schemas/`, `keystore.properties`, `.github/workflows/android-build.yml`, `app/src/androidTest/` | Alto para previsibilidade de releases futuras e atualização in-place |
| **Validação real em CI / Dispositivo** | Executar `gradle :app:testDebugUnitTest :app:assembleDebug` (e `:app:assembleRelease` com secrets) sobre o código atual (`v5.3.0`, Room `v8`, `21` suítes / `170` `@Test`) e validar em aparelho/tablet real (atualização in-place `v7→v8` via `adb install -r` após `apksigner verify`, checklists independentes com versionamento não retroativo em Compromissos/Eventos, ingestão de `RECURRENCE` + checklists via `NEXUS-INBOX`, Restauração de Fábrica preservando PAT/coordenadas, prévia dry-run e mesclagem incremental `v9`, evolução visual/adaptativa, notificações Android 13+, deep-link e HTTP real) | CI + Dispositivo Android | Crítico antes de publicar nova release |

---

## 4. Restrições Estruturais, Itens Não Planejados e Decisões Históricas

> Esta seção descreve o estado atual de escopo, não uma lista de proibições eternas. Ideias não planejadas, rejeitadas ou descartadas podem ser reavaliadas quando houver mudança de contexto ou novas evidências, conforme docs/13_GOVERNANCA_DOCUMENTAL.md. Reavaliação não equivale a aprovação ou implementação.

### 4.1 Restrições estruturais atualmente adotadas
- Preservar a arquitetura Android nativa Kotlin + Jetpack Compose + Room, com evolução incremental; substituição exige proposta e decisão explícita.
- Manter o núcleo offline-first, sem login obrigatório, backend proprietário ou dependência de nuvem para o funcionamento principal.
- Não introduzir chaves secretas ou credenciais privadas embutidas no aplicativo/repositório.
- Preservar navegação por estado enquanto a decisão arquitetural correspondente permanecer ativa.
- Preservar integridade de dados e migrações Room aditivas; não utilizar fallbackToDestructiveMigration().

### 4.2 Não planejado ou não aprovado atualmente
- IA embarcada ou online, chatbots, interpretação automática por OCR/LLM e assistentes generativos: não aprovados nem planejados neste momento. Pesquisa exploratória pode avaliar utilidade, execução local, privacidade, desempenho, licença e compatibilidade com aparelhos modestos. Pesquisa não autoriza implementação.
- Integração com contas bancárias reais/Open Finance: não aprovada nem planejada. Exige avaliação específica de segurança, privacidade, consentimento, regulamentação, provedores, custos e manutenção.
- Múltiplos usuários, contas e sincronização obrigatória em nuvem: não fazem parte da arquitetura atual.
- Corretora de valores, ordens de compra/venda e recomendações de investimento: não fazem parte do escopo atual.

### 4.3 Decisões ou alternativas históricas
- Reescrever o aplicativo do zero ou substituir a stack atual: alternativa não adotada; não executar sem nova proposta fundamentada e aprovação explícita.
- Substituir as 12 fontes públicas sem token do Mercado por provedores pagos ou exigir chave pessoal brapi: proposta anterior superada pelos Lotes 01–09 e pelos ADRs correspondentes.
- Migrar navegação por estado para NavHost: alternativa não adotada enquanto ADR-001 permanecer ativo.

### 4.4 Regra de reavaliação
Uma ideia anteriormente rejeitada, descartada ou não planejada pode voltar à pesquisa/análise quando surgir necessidade nova, tecnologia/custo/licença/capacidade dos dispositivos mudar, novas evidências reduzirem riscos ou a alternativa resolver problema mais importante. Recuperar a justificativa original, registrar o que mudou e atualizar ADRs/documentos afetados quando houver nova decisão. Não apagar nem reescrever silenciosamente o histórico.


> **Rastreabilidade de pendências:** consultar também [`15_REGISTRO_LACUNAS_E_FALHAS.md`](./15_REGISTRO_LACUNAS_E_FALHAS.md), que consolida lacunas funcionais, defeitos, inconsistências documentais e falhas observadas em CI. A posição de prioridade na tabela é sugestão para reavaliação; não autoriza, isoladamente, alteração de schema ou implementação.
