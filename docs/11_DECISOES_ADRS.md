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
