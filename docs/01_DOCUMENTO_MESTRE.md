# 01 — Documento Mestre do Projeto NEXUS

> **Natureza:** Visão geral executiva e técnica do projeto.  
> **Baseline verificada:** `com.nexus.app` · `versionCode = 7` · `versionName = "5.2.0"` · Room `v6` (`27` entidades) · Backup JSON `v7`.

---

## 1. O que é o NEXUS

O **NEXUS** é um aplicativo Android nativo, em **português brasileiro (`pt-BR`)**, concebido como um **centro de comando pessoal e financeiro offline-first**.

Em vez de espalhar tarefas, calendário, projetos, objetivos, eventos, contas a pagar e cotações em aplicativos desconexos, o NEXUS centraliza esses domínios sobre um único núcleo local com **fonte única da verdade**, operando **100% sem login, sem servidor proprietário e sem dependência de nuvem**.

### Problema que resolve
- **Fragmentação de rotina e finanças pessoais:** reduz o atrito entre saber *o que precisa ser feito* (tarefas/compromissos/projetos/objetivos), *quando acontece* (agenda unificada) e *quanto custa* (despesas, cartões, faturas, eventos).
- **Duplicação de lançamentos e prazos:** elimina a necessidade de cadastrar duas vezes um boleto no calendário e no controle financeiro, ou de somar compra no cartão e pagamento de fatura como dupla despesa.
- **Dependência de internet e contas em nuvem:** o usuário mantém soberania integral dos dados em SQLite local, com backup portável JSON (`v1..v7`) via Storage Access Framework (SAF).

---

## 2. Pilares e Princípios Permanentes

1. **Offline-first verdadeiro:** todo o núcleo produtivo e financeiro funciona sem rede. Somente a atualização remota do **Módulo Mercado** utiliza `INTERNET` e `ACCESS_NETWORK_STATE`, preservando o último cache válido em falhas ou modo avião.
2. **Registrar uma vez, ver em vários lugares:** integrações são feitas por chaves estrangeiras opcionais (`ON DELETE SET NULL`), projeções puras (`AgendaEngine`, `FinanceRules`, `RecurrenceEngine`) e reatividade via `Flow`.
3. **Zero dados fictícios:** o app nunca inventa cotações, taxas/juros de eventos, frases motivacionais embutidas ou lançamentos de exemplo; estados vazios ou sem histórico são exibidos com clareza.
4. **Zero servidor proprietário, login ou rastreamento:** não há backend próprio, Firebase, contas de usuário ou coleta remota (`android:allowBackup="false"`).
5. **Integridade de dados inegociável:** `fallbackToDestructiveMigration()` é estritamente proibido; evoluções de schema usam `Migration` aditiva explícita e backups legados (`v1..v6`) continuam legíveis.
6. **Mercado informativo e sem chave obrigatória:** cotações, gráficos históricos, lista de acompanhamento e alertas de preço operam sobre APIs públicas gratuitas com fallback, exibindo aviso de caráter meramente informativo.

---

## 3. Evolução dos Princípios e Documentos

Os princípios desta seção descrevem compromissos estruturais vigentes, mas não tornam os documentos imutáveis. A visão do produto, especificações, arquitetura e decisões podem ser revisadas quando novas necessidades ou evidências justificarem a mudança. Revisões devem preservar contexto, histórico e rastreabilidade conforme docs/13_GOVERNANCA_DOCUMENTAL.md; uma decisão anterior não deve ser interpretada como proibição eterna sem verificar seu contexto e status.

## 4. Escopo Atual do Sistema (Visão Consolidada)

### Núcleo Implementado no Código
- **Dashboard (`HomeScreen`):** saudação personalizada + data + frase diária discreta no cabeçalho, gráfico de anel de tarefas do mês (`TaskRingChart`), alertas críticos reais (`AlertBannerCard`), próximos compromissos/eventos/pagamentos, gráfico de fluxo financeiro de 6 meses (`BarGroupChart`), gráfico de rosca por categoria (`DonutChart`), projetos e objetivos, tudo ordenável e ligável/desligável via `DashboardCustomizationScreen`.
- **Agenda Unificada (`AgendaScreen` + `AgendaEngine`):** 4 visões (`Mês`, `Semana`, `Dia`, `Lista`), leitura em tempo real de 6 fontes (compromissos, tarefas datadas + recorrências virtuais, eventos inclusive multi-dia, despesas diretas sem fatura, faturas em aberto com valor > 0 e prazos de projetos), detecção de conflitos entre compromissos e filtro contextual.
- **Tarefas, Subtarefas, Projetos e Recorrência (`TasksAndProjectsScreen` + `RecurrenceEngine`):** abas `Lista`, `Kanban` (6 colunas sobre a mesma tabela `tasks`), `Atrasadas`, `Concluídas` e `Projetos`; checklist de subtarefas; vínculo direto com gasto (`transactions.taskId`); recorrência completa de tarefas com exceções (`SKIP` / `OVERRIDE`), edição `"Só esta"` vs `"Toda a série"` e geração idempotente da próxima ocorrência.
- **Financeiro Completo (`FinanceScreen` + `FinanceRules`):** seletor `< mês/ano >`, totais mensais puros, `DonutChart` interativo por categoria (`Pagas` vs `Pagas + previstas`), `BarGroupChart` 6 meses (`Realizado` vs `Previsto` + resultado do mês), previsão de 30 dias, cartões de crédito com fechamento e quitação de fatura sem dupla despesa, parcelamentos, regras recorrentes financeiras, empréstimos, orçamentos (`ProgressBarRow`), metas financeiras e exportação CSV de 15 colunas.
- **Eventos (`EventsScreen`):** controle de ciclo de vida, ingresso gratuito ou pago/parcelado integrado ao cartão e ao financeiro, custos extras por tipo e atalhos para aplicativos de mapas.
- **Objetivos Pessoais (`PlanningGoalsScreen`):** objetivos com progresso automático (`AUTO` agregando marcos, tarefas e projetos vinculados) ou manual (`MANUAL`) e histórico de evolução.
- **Assistente de Rotina e Notificações (`RoutineAssistantScreen`):** Briefing Diário, Revisão Noturna com adiamento de pendências em 1 toque, histórico persistido (`briefing_history`), 5 canais de notificação, agendamentos via `AlarmManager` + `WorkManager`, ações rápidas em notificação e restauração no boot.
- **Widgets Android (`widgets/*`):** 4 widgets em Jetpack Glance (`Meu Dia`, `Minhas Tarefas`, `Financeiro`, `Resumo NEXUS`) responsivos (`SMALL`, `MEDIUM`, `LARGE`), configuráveis por instância e adaptados aos temas claro e escuro (`DayNightColorProvider`).
- **Módulo Mercado (`Lotes 01–09` concluídos no código):** acompanhamento de **12 ativos** (`USD-BRL`, `EUR-BRL`, `BTC-BRL`, `ETH-BRL`, `SOL-BRL`, `IBOV`, `SPX`, `IXIC`, `PETR4`, `VALE3`, `ITUB4`, `MELI`), tela dedicada `MarketScreen` + detalhe `MarketAssetDetailScreen`, provedores públicos gratuitos com fallback por categoria, histórico gráfico offline/online (`MarketHistoryChartCard`), favoritos (*Acompanhamento*), alertas locais de preço com notificação + deep-link e persistência de preferências no Backup JSON `v7`.
- **Backup, Diagnóstico e Busca Global:** busca unificada em tempo real, temas Claro/Escuro, biblioteca de frases local (TXT/JSON), registro automático de exceções não tratadas (`GlobalErrorHandler` → `error_logs`), contagens ao vivo das 27 tabelas e backup JSON transacional `v7` (retrocompatível com `v1..v6`).

### Fora do Escopo / Planejado e Pendente
- **Planejado ainda não implementado:** Eventos com `locationMode` (`IN_PERSON`, `ONLINE`, `TBD`), banner JPEG via `PickVisualMedia` e empacotamento `.zip` (`data.json` + `banners/`); dependência oficial `androidx.core:core-splashscreen`; expansão de recorrência para Compromissos e Eventos; eventual bloco Mercado opcional na `HomeScreen`.
- **Restrições e decisões vigentes:** login, backend próprio e sincronização obrigatória em nuvem não fazem parte da arquitetura offline-first atual; múltiplos usuários e troca do Room/navegação por estado para NavHost não estão planejados. IA local/remota e leitura automática de documentos/extratos não estão aprovadas como funcionalidades; qualquer possibilidade deve passar por pesquisa, análise de compatibilidade e aprovação conforme 13_GOVERNANCA_DOCUMENTAL.md. Integrações financeiras reais exigem avaliação específica de segurança, privacidade, regulamentação e viabilidade.

---

## 5. Visão Técnica Resumida

| Dimensão | Valor Real Confirmado no Código |
| --- | --- |
| **Pacote / ID** | `com.nexus.app` (`versionCode = 7`, `versionName = "5.2.0"`) |
| **Toolchain / SDK** | Kotlin `2.1.0`, AGP `8.7.3`, KSP `2.1.0-1.0.29`, Gradle `8.10.2`, Java `17`, `minSdk = 26`, `targetSdk = 35`, `compileSdk = 35` |
| **UI & Widgets** | Jetpack Compose BOM `2024.12.01`, Material 3, gráficos nativos `Canvas`, Jetpack Glance `1.1.1` |
| **Persistência Estruturada** | Room `2.6.1` (`nexus_local.db`, `DATABASE_VERSION = 6`, `27` entidades, `27` DAOs, `5` migrações) |
| **Persistência Mercado / Widgets** | `SharedPreferences` (`nexus_market_quotes_cache`, `nexus_market_watchlist`, `nexus_widget_instance_prefs`) + arquivos JSON atômicos (`filesDir/market_alerts/market_alerts.json`, `filesDir/market_history/*.json`) |
| **Backup** | `NexusBackupPayload` (`CURRENT_FORMAT_VERSION = 7`, JSON retrocompatível `v1..v7`) + CSV financeiro (15 colunas) |
| **Background & Rede** | `AlarmManager`, `WorkManager` `2.10.0` (`NexusReminderSyncWorker` 15m, `MarketQuoteSyncWorker` 30m), `HttpsURLConnection` (`java.net`) |
| **Testes no Repositório** | `15` arquivos JUnit 4 em `app/src/test/java/com/nexus/app/` (`0` testes instrumentados `androidTest`) |
| **Estrutura do Workspace** | Repositório misto na raiz: módulo Android real em `:app` (`settings.gradle.kts`) coexistindo com arquivos do wrapper Web/Node do AI Studio (`package.json`, `src/`, `vite.config.ts`, `index.html`) — os arquivos Web não pertencem ao APK Android |
