# NEXUS — Roadmap Definitivo de Implementação e Viabilidade Técnica (`NEXUS_IMPLEMENTATION_ROADMAP`)

> **Finalidade:** servir como roteiro técnico executável em lotes grandes e seguros de desenvolvimento, conectando a análise arquitetural do código real (`NEXUS_CURRENT_STATE.md`), a proposta funcional (`NEXUS_PRODUCT_SPEC.md`), o sistema visual (`NEXUS_DESIGN_SYSTEM.md`) e as dependências entre módulos (`NEXUS_INTEGRATION_MAP.md`).

---

## 1. Análise de Arquitetura e Viabilidade Técnica

### 1.1 O que já é sólido e DEVE ser reutilizado (não refatorar sem motivo)
- **Navegação por estado em `MainActivity.kt` (`NexusAppRoot`):** modelo `MainTab`, `SubRoute`, `ActiveEditor` + `BackHandler` com `resolveBackAction` funciona de forma previsível sem overhead de `NavHost` XML/string routes (apenas remover a dependência ociosa `navigation-compose` quando conveniente).
- **Motor `AgendaEngine` + `DateTimeUtils`:** arquitetura pura em `domain/rules/` já pronta para absorver recorrências e filtros sem alterar assinaturas de telas.
- **Regras puras em `domain/rules/`:** `FinanceRules`, `ReminderRules`, `BackNavigationRules` reduzem drasticamente risco de regressão e permitem cobertura JUnit rápida em JVM.
- **Room SQLite com migrações aditivas + Backup tolerante:** 25 tabelas e `BackupManager` com `ignoreUnknownKeys = true` + `withTransaction` permitem evoluções contínuas (`v5 -> v6 -> v7 -> v8`) preservando backups `v1..v5` sem perda de dados.
- **Infraestrutura nativa de Notificações e Widgets Glance:** canais, receivers, `AlarmManager`, `WorkManager` e 4 widgets responsivos com `Mutex` em `NexusWidgetUpdater`.

### 1.2 O que precisa ser corrigido antes ou no início dos próximos lotes
- **Testes unitários quebrados:** `NexusStage1UnitTests.kt:340` (`ReminderStatus.SNOOZED` inexistente no enum) e `NexusPhase6UnitTests.kt:403` (`assertEquals(4, decoded.formatVersion)` quando `CURRENT_FORMAT_VERSION == 5`).
- **Inconsistências `INT-01` a `INT-10`:** listadas no `NEXUS_INTEGRATION_MAP.md` (`BackHandler` `editorHasUnsavedChanges = false`, subtarefas salvando antes de confirmar, `TransactionWithDetails.task`, `onToggleSettle = {}`, filtros de `DailyBriefingSnapshot`/`NightReviewSnapshot`, `GlobalSearch` conflitos, `saveEvent` gravando `OVERDUE`, strings `"JSON v4"`, tema único escuro nos widgets).
- **CI e schemas Room:** adicionar `testDebugUnitTest` ao workflow `.github/workflows/android-build.yml`, avaliar geração de `gradlew` / `app/schemas/` e assinatura consistente de release.

### 1.3 O que precisa ser ampliado
- **`NexusTheme.kt`:** incluir `accentNeon`, `onAccentNeon`, `chartSecondary`, `chartTrack` e `chartPalette` (8 cores nos dois temas).
- **`NexusWidgetTheme.kt`:** adicionar paleta clara + suporte dinâmico/preferência de tema nos widgets.
- **Pasta `ui/components/charts/`:** criar biblioteca reutilizável em Compose `Canvas` (`TaskRingChart`, `DonutChart`, `BarGroupChart`, `ProgressBarRow`, `Sparkline`).
- **Unificação total em `AgendaEngine` e `FinanceRules`:** eliminar `observeUnifiedAgenda` legado para que Agenda, Home, Briefing, Revisão Noturna e Widgets leiam exatamente as mesmas regras.

### 1.4 Novas Entidades, Colunas e Planejamento de Migrações Room

**Regra permanente:** `fallbackToDestructiveMigration()` continua estritamente proibido. Toda mudança é **aditiva** com `exportSchema = true` e atualização imediata de `BackupModels.kt` + `BackupManager.kt`.

| Versão Room | Migração | Mudanças de Schema (Aditivas) | Versão Backup | Lote |
| --- | --- | --- | --- | --- |
| `v5` (Atual) | — | `appointments.eventId?`, `transactions.taskId?` | `v5` | Atual |
| **`v6`** | `MIGRATION_5_6` | Novas tabelas `recurrences` e `recurrence_exceptions`; colunas anuláveis `tasks.recurrenceId?` e `tasks.occurrenceDateIso?` + índice `UNIQUE(recurrenceId, occurrenceDateIso)` | **`v6`** | Lote 3 (Recorrência) |
| **`v7`** | `MIGRATION_6_7` | Colunas em `events`: `locationMode TEXT NOT NULL DEFAULT 'TBD'` e `bannerPath TEXT` | **`v7`** (+ suporte `.zip`) | Lote 4 (Eventos + ZIP) |
| **`v8`** | `MIGRATION_7_8` | Novas tabelas `market_quotes` e `market_history` | **`v8`** (ou mantidas opcionais; **nunca** inclui token brapi) | Lote 5 (Identidade, Mercado, Dashboard & QuickAdd) |

*(Nota: o Lote 1 — Estabilização — e o Lote 2 — Financeiro Visual — **não alteram schema do Room**, mantendo `DATABASE_VERSION = 5` e zero risco de migração.)*

### 1.5 Impactos em Backup, Notificações e Widgets
- **Backup:** cada incremento (`v6`, `v7`, `v8`) adiciona campos com valores default nos DTOs `@Serializable`, mantendo leitura garantida de `v1..v5`. O Lote 4 introduz exportação/importação `.zip` (`data.json` + `banners/*.jpg`) mantendo `.json` simples funcional.
- **Notificações:** Lote 3 adiciona agendamento para janela de 30 dias em recorrências e reagendamento ao alterar série ou exceção; Lote 1 corrige contagem e filtros de briefing/teste.
- **Widgets:** Lote 1/2 garantem tema claro/escuro em `NexusWidgetTheme` e reutilização de `AgendaEngine`; Lote 3 atualiza widgets automaticamente com ocorrências recorrentes geradas/projetadas.

### 1.6 Riscos de Regressão e Mitigações

| Risco | Impacto | Mitigação |
| --- | --- | --- |
| Duplicação de tarefas recorrentes ao concluir/reabrir ou sincronizar em paralelo | Alto | Índice `UNIQUE(recurrenceId, occurrenceDateIso)` em `tasks` + função pura `RecurrenceEngine` + operação transacional idempotente (`withTransaction`). |
| Divergência entre valores do Financeiro (`Donut`, `BarGroup`, `Summary`, `Widget`) | Alto | Toda agregação mensal/categoria/30 dias reside em `FinanceRules.kt` (`FinanceMonthSummary`), consumida por todas as telas e testada via JUnit. |
| Quebra de restauração de backups antigos (`v1..v5`) após evoluções | Alto | Manter testes automatizados com payloads JSON reais de cada versão histórica (`v1`, `v2`, `v3`, `v4`, `v5`, ..., `v8`) em cada PR. |
| Crescimento excessivo de `NexusRepository` (4.431 linhas) e `NexusViewModel` (1.422 linhas) | Médio | Toda nova regra vai para arquivos dedicados (`domain/rules/RecurrenceEngine.kt`, `data/market/MarketRepository.kt`, `data/backup/ZipBackupArchiver.kt`, `ui/components/charts/*`), deixando Repository/VM como orquestradores finos. |
| Permissão `INTERNET` introduzida antes da hora ou vazamento do token brapi | Alto | Só adicionar `<uses-permission android:name="android.permission.INTERNET" />` no Lote 5 (Mercado); armazenar token em `SharedPreferences` privado excluído explicitamente de `BackupModels`, `BackupManager` e logs. |
| Compilação não verificável localmente neste container | Alto | Habilitar `./gradlew testDebugUnitTest` antes do APK no `.github/workflows/android-build.yml` logo no Lote 1, validando cada lote via CI real do GitHub Actions. |

---

## 2. Ordem Definitiva dos Lotes de Implementação

```
Lote 0 (Já existente: Baseline 5.2.0 / Room v5)
  └─▶ Lote 1: Estabilização da Base, Correções de Auditoria & CI
        └─▶ Lote 2: Design System Visual & Módulo Financeiro Completo (Gráficos + Previsão)
              └─▶ Lote 3: Sistema Completo de Recorrência (Tarefas, Compromissos e Eventos)
                    └─▶ Lote 4: Eventos (Local, Banner JPEG, Preço Real, Vínculos) & Backup .ZIP
                          └─▶ Lote 5: Redesenho do Dashboard, Identidade N/Splash, Mercado & QuickAdd
```

---

### Lote 1 — Estabilização da Base, Correções da Auditoria & Pipeline CI
- **Objetivo:** deixar a base `v5` compilando 100% inclusive nos testes unitários, fechar todas as inconsistências `INT-01` a `INT-10` levantadas na auditoria e tornar o CI confiável.
- **Dependências:** nenhuma (usa baseline atual Room `v5`).
- **Escopo:**
  1. **Testes:** corrigir `NexusStage1UnitTests.kt:340` (`ReminderStatus` válido + testar adiamento via `snoozedUntil`) e `NexusPhase6UnitTests.kt:403` (`CURRENT_FORMAT_VERSION`); adicionar suíte `NexusStage2UnitTests.kt` cobrindo `AgendaEngine` (multi-dia, faturas, `showPaid`, filtros, `suggestExpenseCategoryId`) e Backup `v5` (`appointment.eventId`, `transaction.taskId`).
  2. **Integrações & Bugs (`INT-01`..`INT-10`):**
     - `TransactionWithDetails` ganha `task: TaskEntity? = null` e `TransactionRowCard` passa a exibir `"Tarefa: <título>"`.
     - Conectar `onToggleSettle` nos pontos `FinanceScreen.kt:371` e `MoreSettingsBackupDiagnosticsScreens.kt:336`.
     - Remover salvamento prematuro de subtarefas ao clicar dentro de `TaskEditorSheet` (persistir apenas no `Salvar`) e tratar `onRegisterExpense` com tarefa ainda não salva de forma segura.
     - Corrigir `BackHandler` para ler estado `editorHasUnsavedChanges` real do editor ativo + preservar origem ao voltar de `DashboardCustomization` e `RoutineAssistant`.
     - Corrigir `observeGlobalSearch` (`computeAppointmentConflicts(allAppointments)`), `observeDailyBriefingSnapshot` / `observeNightReviewSnapshot` (filtrar `EXPENSE` e `statementId == null`) e `sendTestNotification` (contar compromissos multi-dia em curso hoje).
     - Alterar `saveEvent` para gravar `FinancialStatus.PENDING` em vez de `OVERDUE` literal.
     - Atualizar textos UI de `"JSON v4"` para `"JSON v5"` em `MoreSettingsBackupDiagnosticsScreens.kt`.
     - Adicionar suporte a tema claro/escuro em `NexusWidgetTheme.kt`.
     - Remover `navigation-compose` não utilizado (opcional se aprovado) e alinhar `AgendaScreen` no modo Mês para oferecer indicador/resumo claro além do dia selecionado.
  3. **CI:** atualizar `.github/workflows/android-build.yml` para rodar `testDebugUnitTest` antes de `assembleDebug` (e documentar segredos para assinatura/schemas).
- **Arquivos envolvidos:** `DomainModels.kt`, `NexusRepository.kt`, `MainActivity.kt`, `EntityEditors.kt`, `FinanceEditors.kt`, `FinanceScreen.kt`, `MoreSettingsBackupDiagnosticsScreens.kt`, `NexusWidgetTheme.kt`, `NexusStage1UnitTests.kt`, `NexusPhase6UnitTests.kt`, novo `NexusStage2UnitTests.kt`, `.github/workflows/android-build.yml`.
- **Critérios de Aceitação:**
  - `testDebugUnitTest` e `assembleDebug` passam no GitHub Actions sem falhas.
  - Pressionar Voltar com editor sujo abre diálogo de confirmação; desmarcar subtarefa e descartar alterações não altera o banco.
  - Gasto originado de tarefa exibe `"Tarefa: <título>"` no Financeiro e botão check de "Próximo vencimento" liquida a conta.
  - Widgets renderizam legíveis em tema claro e escuro.

---

### Lote 2 — Design System Visual & Financeiro Completo (Equivalente ampliado da Etapa 3)
- **Objetivo:** entregar a camada compartilhada de tokens/gráficos Canvas e a experiência visual completa do módulo Financeiro (sem alterar schema do banco).
- **Dependências:** Lote 1 concluído.
- **Escopo:**
  1. **Tokens em `NexusTheme.kt`:** adicionar `accentNeon` (`#7CFFB2` / `#0E9F63`), `onAccentNeon`, `chartSecondary`, `chartTrack` e `chartPalette` (8 cores para tema escuro e claro).
  2. **Componentes `ui/components/charts/`:** criar `DonutChart.kt`, `BarGroupChart.kt`, `ProgressBarRow.kt`, `Sparkline.kt`, `TaskRingChart.kt` (este último já reutilizável no Lote 5) e `MonthYearSelectorBar.kt` em `NexusComponents.kt`, todos com estados vazios, animação suave e `semantics`.
  3. **Regras puras em `FinanceRules.kt`:** adicionar `FinanceMonthSummary` completo (seletor `YearMonth`, rosca de categorias com modos `Pagas` vs `Pagas + previstas`, top 6 + `Outros`, série 6 meses `Realizado | Previsto` + `"Resultado do mês"`, ordenação de orçamentos por % decrescente, e previsão de **Próximos 30 dias** retornando totais previstos e top 5 vencimentos).
  4. **`FinanceScreen.kt`:**
     - Barra `< mês/ano >` no topo sincronizando todos os blocos.
     - `DonutChart` interativo por fatia/legenda filtrando a lista de lançamentos abaixo com chip `"Categoria: X ✕"`.
     - `BarGroupChart` 6 meses com alternância `Realizado | Previsto`, balão de toque selecionando o mês e texto `"Resultado do mês"`.
     - Cartão de Orçamentos usando `ProgressBarRow` (cores 80% / 100% + `"excedeu R$ X"`).
     - Cartão `"Próximos 30 dias"` (rotulado como previsto) + histórico completo de faturas em Cartões + barras de metas financeiras.
- **Arquivos envolvidos:** `NexusTheme.kt`, `NexusComponents.kt`, novos arquivos em `ui/components/charts/`, `FinanceRules.kt`, `DomainModels.kt`, `NexusRepository.kt`, `NexusViewModel.kt`, `FinanceScreen.kt`, novo `NexusStage3FinanceUnitTests.kt`.
- **Critérios de Aceitação:**
  - Tocar em uma fatia do gráfico de rosca destaca a fatia e filtra os lançamentos pela categoria; tocar novamente limpa o filtro.
  - Alternar `< mês/ano >` recalcula totais, rosca, barras e orçamentos sem misturar previsto e realizado.
  - Comprar no cartão entra na categoria do mês da compra; pagar a fatura quita sem duplicar despesa nem alterar o total de despesas do mês.
  - Contraste aprovado nos dois temas (Claro e Escuro) + testes JUnit de `FinanceRules` passando.

---

### Lote 3 — Sistema Completo de Recorrência (Equivalente à Etapa 4)
- **Objetivo:** dotar Tarefas, Compromissos e Eventos de recorrência completa, exceções granulares, adiamento seguro sem perder atrasos e expansão em memória pela `AgendaEngine`.
- **Dependências:** Lote 1 e Lote 2 concluídos.
- **Mudança de Banco & Backup:** `DATABASE_VERSION = 6` (`MIGRATION_5_6` aditiva), `CURRENT_FORMAT_VERSION = 6`.
- **Escopo:**
  1. **Schema (`NexusEntities.kt`, `NexusDaos.kt`, `NexusMigrations.kt`):**
     - Criar `RecurrenceEntity` (`recurrences`: `id, ownerType [TASK|APPOINTMENT|EVENT], ownerId, frequency, intervalCount, byWeekdays, startDateIso, untilDateIso?, maxCount?, createdAt, updatedAt`).
     - Criar `RecurrenceExceptionEntity` (`recurrence_exceptions`: `id, recurrenceId, occurrenceDateIso, kind [CANCELLED|OVERRIDDEN], overrideStartAt?, overrideEndAt?, overrideTitle?, updatedAt`) com `UNIQUE(recurrenceId, occurrenceDateIso)` e `CASCADE`.
     - Adicionar `tasks.recurrenceId?` (`SET_NULL`) e `tasks.occurrenceDateIso?` com `UNIQUE(recurrenceId, occurrenceDateIso)`.
  2. **Motor puro `RecurrenceEngine.kt` (`domain/rules/`):**
     - Frequências: diária, dias específicos da semana, fins de semana, semanal, quinzenal, mensal (com clamp seguro de dias 29–31), anual, personalizada (`N` dias / `N` semanas).
     - Término: nunca / até data / após `N` ocorrências.
     - Expansão dentro de `DateRange` respeitando `recurrence_exceptions`, cálculo de próxima ocorrência após conclusão e cálculo da janela de lembretes de 30 dias.
  3. **Integrações:**
     - `AgendaEngine.expand` passa a projetar compromissos e eventos recorrentes (e ocorrências futuras de tarefas recorrentes) dentro da janela pedida **sem gravar cópias no SQLite**.
     - Concluir tarefa recorrente gera a próxima instância em `tasks` de forma idempotente mantendo o histórico concluído.
     - Adiar ocorrência atrasada mantém contabilização de atraso até resolução sem quebrar o calendário futuro da série.
     - Editores (`TaskEditorSheet`, `AppointmentEditorSheet`, `EventEditorSheet`) ganham seletor `"Repetir"` + `"Termina em"` + diálogo `"Só esta"` vs `"Toda a série"` ao editar/excluir ocorrência.
     - `NexusNotificationScheduler` e `NexusReminderSyncWorker` agendam ocorrências dentro dos próximos 30 dias e reagendam quando a regra muda.
     - Backup v6 inclui `recurrences` e `recurrence_exceptions` mantendo leitura `v1..v5`.
- **Arquivos envolvidos:** `NexusEnums.kt`, `NexusEntities.kt`, `NexusDaos.kt`, `NexusDatabase.kt`, `NexusMigrations.kt`, `BackupModels.kt`, `BackupManager.kt`, novo `RecurrenceEngine.kt`, `AgendaEngine.kt`, `NexusRepository.kt`, `NexusViewModel.kt`, `EntityEditors.kt`, `EventsScreen.kt`, `NexusNotificationScheduler.kt`, novo `NexusStage4RecurrenceUnitTests.kt`.
- **Critérios de Aceitação:**
  - Série "Aula de inglês — quartas e sextas" aparece nas datas certas em Semana e Mês sem gravar cópias na tabela de compromissos.
  - Cancelar/editar `"Só esta"` cria exceção sem alterar demais datas; concluir tarefa recorrente cria apenas 1 próxima ocorrência sem duplicatas ao reabrir.
  - Fim de mês (ex.: dia 31), exceção editada/cancelada, fim por data/contagem e migração v5->v6 / backup v6 validados.

---

### Lote 4 — Eventos Evoluídos (Local, Banner, Preço Real, Vínculos) & Backup `.ZIP` (Equivalente à Etapa 5)
- **Objetivo:** simplificar Eventos conforme o princípio do preço real sem taxas inventadas, adicionar modo de local flexível, banner offline leve com backup `.zip` e lista completa de vínculos (incluindo compromissos).
- **Dependências:** Lote 3 concluído (ou compatível diretamente após Lote 3 para sequência limpa de migrações `6->7`).
- **Mudança de Banco & Backup:** `DATABASE_VERSION = 7` (`MIGRATION_6_7`), `CURRENT_FORMAT_VERSION = 7` + exportação/importação `.zip`.
- **Escopo:**
  1. **Schema:** `events.locationMode` (`EventLocationMode`: `IN_PERSON`, `ONLINE`, `TBD`, default `TBD`) + `events.bannerPath?`.
  2. **UI de Evento (`EventsScreen.kt` / `EventEditorSheet`):**
     - Seletor `Presencial | Online | A definir` (nenhum campo de local obrigatório).
     - Revisão de custo: manter `ticketPriceCents × ticketQuantity` como preço real; ocultar `ticketFeesCents` do fluxo principal (zerado por padrão, opcional somente em `"Mais detalhes"` caso realmente pago); distinguir claramente **Preço do ingresso**, **Pagamentos realizados** e **Obrigações futuras**.
     - Seção de banner com `ActivityResultContracts.PickVisualMedia`: redimensionamento nativo `BitmapFactory` (lado maior `<= 1080px`, JPEG ~80%) salvo em `filesDir/event_banners/<eventId>.jpg`, carregamento leve sem biblioteca externa e remoção limpa do arquivo físico.
     - Detalhe do evento listando **Compromissos (`appointments.eventId`)**, **Tarefas (`tasks.eventId`)** e **Lançamentos (`transactions.eventId`)** com atalhos diretos para criar cada um já vinculado.
  3. **Backup `.zip` (`data/backup/ZipBackupArchiver.kt`):**
     - Nova opção `"Backup completo (.zip)"` contendo `data.json` + pasta `banners/`.
     - Restauração aceita `.json` (avisando ausência de imagens) e `.zip` (restaurando JSON + arquivos em `filesDir/event_banners/` dentro de rotina segura).
- **Arquivos envolvidos:** `NexusEnums.kt`, `NexusEntities.kt`, `NexusMigrations.kt`, `BackupModels.kt`, `BackupManager.kt`, novo `ZipBackupArchiver.kt`, novo `ImageBannerStorage.kt`, `NexusRepository.kt`, `NexusViewModel.kt`, `EventsScreen.kt`, `MoreSettingsBackupDiagnosticsScreens.kt`, novo `NexusStage5EventsUnitTests.kt`.
- **Critérios de Aceitação:**
  - Evento sem local e sem taxas extras salva em 1 fluxo direto.
  - Banner selecionado persiste após reiniciar o app; remover apaga o arquivo local; backup `.zip` exportado e restaurado recupera dados + banners.
  - Detalhe do evento permite criar e visualizar compromissos, tarefas e gastos vinculados.

---

### Lote 5 — Redesenho Conceitual do Dashboard, Identidade N/Splash, Mercado & Cadastro Rápido (Etapa 6 Unificada)
- **Objetivo:** entregar a experiência final consolidada da Tela Inicial (anel central de tarefas com alternância de mês, anel financeiro, projetos/objetivos, alertas reais e bloco Mercado com Ibovespa + Dólar), identidade própria (Símbolo N + Splash) e `QuickAddChooser` simplificado.
- **Dependências:** Lotes 2, 3 e 4 concluídos (`TaskRingChart`, `DonutChart`, `ProgressBarRow`, `Sparkline`, `AgendaEngine` com recorrência e schema `v7` estabilizado).
- **Mudança de Banco, Manifesto & Gradle:**
  - `DATABASE_VERSION = 8` (`MIGRATION_7_8`: tabelas `market_quotes` e `market_history`).
  - `CURRENT_FORMAT_VERSION = 8` (cache de mercado opcional/ignorado na restauração; **token brapi jamais salvo em código, banco ou backup**).
  - `AndroidManifest.xml`: **único lote** que adiciona `<uses-permission android:name="android.permission.INTERNET" />`.
  - `libs.versions.toml` + `app/build.gradle.kts`: adicionar apenas `androidx.core:core-splashscreen`.
- **Escopo:**
  1. **Identidade & Splash:** vetor geométrico N (traço encorpado com corte característico na junção das hastes), ícone adaptativo fundo `#050505`, tema de splash oficial `Theme.NEXUS.Splash` sem atraso artificial, cabeçalho da Home com monograma N + hierarquia saudação/data + busca + menu.
  2. **Módulo Mercado (`data/market/`):**
     - `MarketNetworkClient.kt` com `HttpURLConnection` (`java.net`, timeout `8s`, `Dispatchers.IO`, zero libs HTTP extras) consumindo AwesomeAPI (`USD-BRL`, `EUR-BRL`, `BTC-BRL` + histórico 30d) e brapi (`^BVSP` apenas se usuário configurou token pessoal).
     - `MarketRepository.kt` com throttle de `30 min` ao abrir/puxar para atualizar (zero polling em background), cache Room (`market_quotes`, `market_history`) e armazenamento isolado de token em `Settings`.
     - Bloco `MarketDashboardCard` configurável em `DashboardBlockType.MARKET` com `Sparkline`, variação `+/-` e rodapé *"Informativo. Não é recomendação de investimento."*
  3. **Redesenho do Dashboard (`HomeScreen.kt`):**
     - Saudação + frase contextual.
     - Bloco central de **Tarefas** com `TaskRingChart`, resumo de hoje, seletor **mês anterior / atual / próximo** (`< mês/ano >`), e os 4 indicadores (**Concluídas, Pendentes, Em andamento, Atrasadas**) sem lista longa poluindo a Home.
     - **Alertas importantes reais** (exibidos apenas quando existirem).
     - **Resumo financeiro** com `DonutChart` + totais do mês + resultado.
     - **Indicadores de Projetos e Objetivos** compactos com `ProgressBarRow`.
     - **Próximos compromissos e eventos**.
     - **Bloco Mercado**.
  4. **Cadastro Rápido (`QuickAddChooser` + `"Mais detalhes"`):** 4 opções com descrição de 1 linha e formulários abrindo enxutos com seção expansível `"Mais detalhes"` e pré-preenchimento contextual.
- **Arquivos envolvidos:** `AndroidManifest.xml`, `gradle/libs.versions.toml`, `app/build.gradle.kts`, `res/drawable/*`, `res/values/themes.xml`, `MainActivity.kt`, `NexusEnums.kt`, `NexusEntities.kt`, `NexusDaos.kt`, `NexusDatabase.kt`, `NexusMigrations.kt`, `BackupModels.kt`, `BackupManager.kt`, novo `data/market/*`, `HomeScreen.kt`, `MoreSettingsBackupDiagnosticsScreens.kt`, `EntityEditors.kt`, `FinanceEditors.kt`, `NexusStage6UnitTests.kt`.
- **Critérios de Aceitação:**
  - Splash exibe N branco em fundo preto sem travamento; restante do app opera 100% offline se sem internet ou se bloco Mercado estiver desligado.
  - Com internet, Dólar/Ibovespa (com token) atualizam respeitando intervalo de 30 min; offline exibe último valor em cache com horário sem mensagens de erro técnicas.
  - Dashboard apresenta anel central de tarefas com troca de mês, anel financeiro, projetos/objetivos, agenda/eventos, Mercado e alertas reais com navegação direta ao tocar.
  - `QuickAddChooser` exibe as 4 opções e `"Mais detalhes"` expande campos avançados mantendo preenchimento contextual.

---

## 3. Verificação Cruzada de Consistência Entre os 5 Documentos

Revisão executada antes da entrega confirmando alinhamento entre `NEXUS_CURRENT_STATE.md`, `NEXUS_PRODUCT_SPEC.md`, `NEXUS_DESIGN_SYSTEM.md`, `NEXUS_INTEGRATION_MAP.md` e `NEXUS_IMPLEMENTATION_ROADMAP.md`:

| Ponto Verificado | Status de Alinhamento |
| --- | --- |
| Baseline real (`5.2.0`, Room `v5`, 25 entidades, Backup `v5`, testes quebrados e bugs `INT-01`..`INT-10`) | Consistente nos 5 documentos |
| Os 13 módulos completos com os 10 subitens obrigatórios | Cobertos integralmente em `NEXUS_PRODUCT_SPEC.md` e referenciados nos demais |
| Redesenho do Dashboard (anel de tarefas, troca mês anterior/atual/próximo, 4 status, anel financeiro, projetos/objetivos, Mercado Ibovespa+Dólar, alertas) | Coerente entre `NEXUS_PRODUCT_SPEC.md`, `NEXUS_DESIGN_SYSTEM.md` (`TaskRingChart`, `DonutChart`, `Sparkline`) e Lote 5 do `NEXUS_IMPLEMENTATION_ROADMAP.md` |
| Recorrência completa sem duplicações e mantendo atrasadas após adiamento | Regras uniformes em `NEXUS_PRODUCT_SPEC.md` (§3 Mód. 2), `NEXUS_INTEGRATION_MAP.md` (Fluxo C) e Lote 3 |
| Eventos com preço real sem taxas inventadas, modo de local opcional e banner JPEG | Regras uniformes em `NEXUS_PRODUCT_SPEC.md` (§3 Mód. 5), `NEXUS_INTEGRATION_MAP.md` (Fluxo D) e Lote 4 |
| Financeiro com cartão/fatura sem duplicação, `DonutChart`, `BarGroupChart`, `ProgressBarRow` e 30 dias | Regras uniformes em `NEXUS_PRODUCT_SPEC.md` (§3 Mód. 6), `NEXUS_DESIGN_SYSTEM.md` (§7.2), `NEXUS_INTEGRATION_MAP.md` (Fluxo E) e Lote 2 |
| Preservação de compatibilidade com `docs/NEXUS_SPEC.md` + registro explícito de dúvidas em aberto | Documentado na Seção 4 de `NEXUS_PRODUCT_SPEC.md` e na tabela de migrações/lotes deste roadmap |
