# 10 — Roadmap Consolidado do NEXUS

> **Princípio organizador:** separação rigorosa entre o que já existe **no código atual**, o que é **trabalho planejado ainda não iniciado**, o que é **dívida técnica / manutenção** e o que está **explicitamente fora de escopo**.

---

## 1. Concluído no Código Atual

### 1.1 Núcleo do Aplicativo e Evolução Base (`v5.3.0` · Room `v6`)
- **Arquitetura base, Room `v6` (`27` tabelas), migrações `v1→v6` e Diagnóstico (`GlobalErrorHandler`, `DiagnosticsScreen`)**.
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

### 1.1.3 Rodada 3 — Ponte ChatGPT → NEXUS-INBOX → Android (**EM ANDAMENTO**)

A primeira implementação da ponte foi produzida no AI Studio e passou por correções em branches isoladas. O escopo inclui repositório GitHub privado, consulta read-only, token protegido por Android Keystore, sincronização manual/periódica, prévia e confirmação explícita antes da importação.

**Situação de integração:** as correções finais ainda não foram incorporadas à `main`. A branch `fix/inbox-build-47-kotlin-compile` passou na GitHub Actions #50 (run `45748189511`), mas está divergente da `main` (4 commits à frente / 9 atrás na comparação de 26/11/2026). Não fazer merge integral dessa branch. Reaplicar as correções necessárias no estado atual da `main` com o AI Studio, executar CI na `main` e validar a sincronização real no aparelho.

**Critérios para encerrar a Rodada 3:**
1. Correções de compilação e teste da Inbox reproduzidas no código atual da `main`.
2. GitHub Actions verde na `main`, com testes unitários e APK Debug.
3. Repositório privado `NEXUS-INBOX` configurado no aplicativo, token read-only protegido pelo Android Keystore e teste de conexão real concluído.
4. Arquivo suportado pela versão atual do contrato aparece na prévia, pode ser confirmado e é importado sem duplicação; descarte não altera dados.
5. Documentação técnica, espelho `GOOGLE_IA/docs/` e artefatos internos do AI Studio coerentes.

A extensão da Inbox para regras recorrentes novas será concluída junto da Rodada 4, depois da definição do modelo de recorrência (`GAP-001`/`GAP-002`).

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

> **Reorganização de 26/11/2026.** As Rodadas 1 e 2 permanecem concluídas; a Rodada 3 continua sendo a etapa atual. A antiga Rodada 4 (aplicar a proposta Claude) deixa de ser a próxima execução: primeiro será concluída a base funcional e as lacunas de domínio. A evolução da experiência passa a ser a Rodada 5, baseada na documentação conceitual atualizada em `NEXUS-DOCUMENTACAO/CHATGPT/ATUALIZACAO/`, não na adoção automática da proposta original do Claude.

| Etapa | Estado | Objetivo e entregas |
|---|---|---|
| **Rodada 1 — Atualização e proteção dos dados** | **CONCLUÍDA** | Atualização in-place, assinatura configurável, backup incremental, merge transacional e testes unitários. Validações em aparelho e keystore persistente continuam no gate de release. |
| **Rodada 2 — Importação e entrada estruturada** | **CONCLUÍDA** | Prévia dry-run, restauração/mesclagem separadas, contrato estruturado v1, validação, idempotência e rollback. |
| **Rodada 3 — Ponte ChatGPT → GitHub → Android** | **EM ANDAMENTO — etapa atual** | Estabilizar as correções da Inbox sobre a `main`, executar CI e validar o ciclo real de sincronização, prévia, confirmação, descarte e importação no aparelho. Não integrar branches antigas integralmente. |
| **Rodada 4 — Completar o sistema funcional e fechar lacunas de domínio** | **PRÓXIMA — prioridade** | Concluir funcionalidades importantes iniciadas ou especificadas, começando por recorrência de Compromissos e Eventos (`REQ-009`, `GAP-001`) e extensão versionada da Inbox (`GAP-002`). Depois fechar pendências de Eventos/backup, consistência da Agenda, estado de edição/navegação e demais requisitos prioritários do inventário. |
| **Rodada 5 — Arquitetura da informação e evolução da experiência** | **POSTERIOR — EM ANÁLISE** | Auditar o aplicativo funcional completo; mapear entidades e relações; decidir arquitetura de informação; definir linguagem visual; especificar componentes, fichas e interações; prototipar e validar; só então implementar em lotes pequenos. A proposta Claude permanece referência, não especificação aprovada. |

### 2.1 Escopo da Rodada 4 — completar o núcleo funcional

A ordem interna inicial recomendada é:

1. **Recorrência de Compromissos e Eventos (`REQ-009` / `GAP-001`):** auditar o motor atual de tarefas; definir modelo de séries, frequências, exceções, edição “só esta/toda a série”, projeção virtual, fuso horário, lembretes, backup e migrações; implementar e testar sem regredir recorrência de tarefas.
2. **Ponte e contrato de entrada (`GAP-002`):** após aprovar o modelo, estender/versionar o contrato para criar e atualizar regras e exceções recorrentes com prévia, idempotência, validação e rollback. Não simular séries com registros únicos.
3. **Completar Eventos e seus dados associados:** resolver `REQ-012` (gestão de compromissos vinculados), `REQ-014` (campos avançados e banner) e `REQ-026` (backup ZIP com banners), respeitando migrações e compatibilidade.
4. **Consistência funcional e técnica:** resolver `INT-01` (motor legado da Agenda), `INT-03` (rascunho de checklist) e `BUG-001` (preservação de rota/rascunhos), após confirmar o comportamento atual e os testes necessários.
5. **Auditoria de completude:** percorrer requisitos e rastreabilidade, identificar funcionalidades parcialmente implementadas ou não verificadas, classificar por impacto e decidir explicitamente o que precisa ser concluído antes da reformulação. Não transformar cada item opcional/baixo impacto em bloqueio artificial.

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
| **Validação real em CI / Dispositivo** | Executar `gradle :app:testDebugUnitTest :app:assembleDebug` (e `:app:assembleRelease` com secrets) sobre o código atual (`v5.3.0`, `18` suítes / `137` `@Test`) e validar em aparelho/tablet real (atualização in-place via `adb install -r` após `apksigner verify`, prévia dry-run e mesclagem incremental com backup real, fluxo real ChatGPT → `NEXUS-INBOX` com GitHub privado + Keystore PAT, evolução visual/adaptativa, notificações Android 13+, deep-link e HTTP real) | CI + Dispositivo Android | Crítico antes de publicar nova release |

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
