# 10 — Roadmap Consolidado do NEXUS

> **Princípio organizador:** separação rigorosa entre o que já existe **no código atual**, o que é **trabalho planejado ainda não iniciado**, o que é **dívida técnica / manutenção** e o que está **explicitamente fora de escopo**.

---

## 1. Concluído no Código Atual

### 1.1 Núcleo do Aplicativo e Evolução Base (`v5.2.0` · Room `v6`)
- **Arquitetura base, Room `v6` (`27` tabelas), migrações `v1→v6` e Diagnóstico (`GlobalErrorHandler`, `DiagnosticsScreen`)**.
- **Shell de Navegação (`MainActivity` + `BackNavigationRules`):** barra inferior na ordem `TASKS, AGENDA, HOME, FINANCE, EVENTS`, `BackHandler` conectado a `activeEditorDirty` e sub-rotas top-level vs child.
- **Dashboard (`HomeScreen`):** cabeçalho compacto integrando saudação + data + frase diária discreta, `9` blocos reordenáveis/desligáveis, `TaskRingChart`, resumo financeiro compartilhado (`FinanceRules`), indicadores de projetos/objetivos, `AlertBannerCard` para alertas reais e próximos compromissos/eventos.
- **Tarefas e Projetos (`TasksAndProjectsScreen` + `RecurrenceEngine`):** lista executável sem mistura de contas financeiras, Kanban sobre a mesma tabela `tasks`, status separados da condição de prazo, subtarefas, vínculo Tarefa → Gasto (`transactions.taskId`, `"Tarefa: <título>"`) e **Lote 06 de Recorrência de Tarefas** (`task_recurrences`, `recurrence_exceptions`, `"Só esta"` vs `"Toda a série"`, projeção virtual e materialização idempotente).
- **Agenda (`AgendaScreen` + `AgendaEngine`):** 4 visões (`DAY`, `WEEK` horizontal seg–dom, `MONTH` inicial com marcadores, `LIST`), painel único de filtros por tipo/categoria contextual/`showPaid`, eventos multi-dia e detecção de conflitos de compromissos.
- **Financeiro Visual Completo (`FinanceScreen` + `FinanceRules`):** seletor `< mês/ano >`, `DonutChart` interativo com filtro na lista, `BarGroupChart` 6 meses (`Realizado | Previsto` + `"Resultado do mês"`), previsão 30 dias, orçamentos (`ProgressBarRow`), cartões/faturas sem dupla despesa, parcelamentos, recorrentes financeiras, empréstimos, metas e CSV 15 colunas.
- **Objetivos Pessoais (`PlanningGoalsScreen`) & Rotina (`RoutineAssistantScreen`):** modos `AUTO`/`MANUAL`, marcos, logs, Briefing Diário, Revisão Noturna com adiamento em 1 toque e `briefing_history`.
- **Notificações, 4 Widgets Glance Dual-Theme (`DayNightColorProvider`), Biblioteca de Frases, Busca Global e Backup JSON `v1..v7`.**

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

## 2. Planejado / Pendente (Funcionalidades Ainda Não Iniciadas)

> A ordem abaixo segue o contexto histórico do projeto, mas depende de priorização explícita antes da execução de cada lote.

| Prioridade Sugerida | Entrega Planejada | Mudança Principal Prevista | Impacto em Schema / Backup |
| --- | --- | --- | --- |
| **1** | **Eventos Evoluídos + Backup `.ZIP`** *(antiga Etapa 5 / Lote 4)* | `events.locationMode` (`IN_PERSON`, `ONLINE`, `TBD`), ocultar/opcionalizar `ticketFeesCents` por padrão, banner local JPEG (`PickVisualMedia` → `filesDir/event_banners/<id>.jpg`), listagem/criação direta de compromissos vinculados (`appointments.eventId`) na ficha do evento e pacote `.zip` (`data.json` + `banners/`) | Requer `DATABASE_VERSION = 7` (`MIGRATION_6_7`) + `ZipBackupArchiver` (mantendo `.json` funcional) |
| **2** | **Identidade Visual N & Splash Screen + Refino do `QuickAddChooser`** | Símbolo vetorial N próprio no cabeçalho da Home/ícone, `androidx.core:core-splashscreen` (`Theme.NEXUS.Splash`) e padronização do `QuickAddChooser` (descrições enxutas + colapso `"Mais detalhes"` nos editores) | Sem mudança de Room (adiciona `core-splashscreen` no Gradle) |
| **3** | **Recorrência de Compromissos e Eventos** *(complemento da Etapa 4)* | Expandir modelo/motor de recorrência para `appointments` e `events` com projeção virtual na `AgendaEngine` (sem gravar cópias) e lembretes em 30d | Possível generalização ou novas tabelas/colunas aditivas (`ownerType` / FKs) |
| **4** | **Melhorias Opcionais Pós-Mercado** *(somente se solicitadas)* | Bloco configurável `DashboardBlockType.MARKET` na `HomeScreen`, alertas por variação percentual, ou migração opcional de watchlist/alertas de mercado para Room | Somente se explicitamente aprovado em lote futuro |

---

## 3. Dívidas Técnicas e Manutenção Recomendada

| ID | Descrição | Arquivos Envolvidos | Risco / Impacto |
| --- | --- | --- | --- |
| **`INT-01`** | Aposentar o motor legado `observeUnifiedAgenda()` e migrar `observeDashboard()`, `observeDailyBriefingSnapshot()` e `observeNightReviewSnapshot()` integralmente para `AgendaEngine` | `NexusRepository.kt:494+` | Médio — reduz risco de divergência entre Agenda, Dashboard e Rotina |
| **`INT-03` (residual)** | Remover gravação imediata de `onToggleSubtask` dentro de `TaskEditorSheet` antes de o usuário clicar em `"Salvar"` (usar rascunho em memória até confirmação) | `EntityEditors.kt`, `MainActivity.kt` | Baixo — evita mutação silenciosa se usuário descartar edição |
| **Dependência ociosa** | Remover `androidx.navigation:navigation-compose:2.8.5` do catálogo e `app/build.gradle.kts` | `app/build.gradle.kts:65`, `gradle/libs.versions.toml` | Baixo — reduz tamanho do build e evita confusão arquitetural |
| **Modularização gradual** | Quebrar arquivos monolíticos extensos (`NexusRepository.kt` ~4.914 linhas, `EntityEditors.kt` ~2.378, `FinanceScreen.kt` ~1.870, `NexusViewModel.kt` ~1.521) conforme novos lotes tocarem nessas áreas | `data/repository/`, `ui/` | Médio — melhora legibilidade e tempo de análise K2 |
| **Infraestrutura de build/testes** | Adicionar `./gradlew` na raiz, gerar/versionar `app/schemas/` após KSP real, validar assinatura consistente de APK e iniciar cobertura `androidTest` (migrações Room + UI crítica) | Raiz, `app/schemas/`, `.github/workflows/android-build.yml`, `app/src/androidTest/` | Alto para previsibilidade de releases futuras |
| **Validação real em CI / Dispositivo** | Executar `gradle :app:testDebugUnitTest :app:assembleDebug` sobre o código pós-Lotes 07–09 e validar em aparelho real (notificações Android 13+, deep-link de alertas de mercado, backup `v7` e chamadas HTTP reais) | CI + Dispositivo Android | Crítico antes de publicar nova release |

---

## 4. Fora de Escopo Permanente / Descartado

- Reescrever o aplicativo do zero ou substituir Jetpack Compose / Room por outras stacks.
- Login, contas de usuário, backend próprio, sincronização em nuvem ou Firebase.
- IA embarcada ou online (chatbots, leitura automática de extratos bancários por OCR/LLM).
- Integração com contas bancárias reais / Open Finance, corretora de valores, ordens de compra/venda ou recomendações de investimento.
- Migração da navegação por estado para `NavHost` baseado em rotas string.
- Substituição das `12` fontes públicas sem token do Mercado por provedores pagos ou dependência obrigatória de chave pessoal brapi *(decisão substituída pelos Lotes 01–09)*.
