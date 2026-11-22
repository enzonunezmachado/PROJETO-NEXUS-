# 06 — Design System e Padrões Visuais (`NEXUS Design System`)

> **Base verificada (2026-11-22):** `app/src/main/java/com/nexus/app/ui/theme/NexusTheme.kt`, `ui/components/NexusComponents.kt`, `ui/components/charts/*`, `widgets/NexusWidgetTheme.kt`, `res/drawable/ic_launcher_nexus*.xml`, `res/mipmap-anydpi-v26/ic_launcher*.xml` e telas adaptativas (`HomeScreen`, `AgendaScreen`, `FinanceScreen`, `TasksAndProjectsScreen`, `EventsScreen`, `PlanningGoalsScreen`, `MarketScreen`).

---

## 1. Princípios Visuais

1. **Base neutra, acento cirúrgico:** estrutura em preto, grafite suave e escalas de cinza. Cor comunica apenas **status semântico**, **acento de destaque (`accentNeon`)**, **categoria em gráfico** ou **variação/severidade no Mercado**.
2. **Tokens centralizados:** todas as telas usam `LocalNexusPalette.current` (`NexusPalette`), evitando hexadecimais avulsos em telas/componentes (exceto fallback e `category.colorHex` personalizada pelo usuário).
3. **Equivalência Dark / Light:** tema escuro (`darkTheme = true`) é o padrão inicial (`SettingsEntity`), com alternância completa para o tema claro **Grafite Suave** em app e widgets.
4. **Forma e ícone antes da cor:** na Agenda e listas mistas, o tipo de item é distinguido por ícone/forma; a cor indica estado (atrasado, concluído, atenção).
5. **Alinhamento tabular (`tnum`):** valores monetários (`R$`, `US$`), cotações, variações percentuais e horários aplicam `fontFeatureSettings = "tnum"`.

---

## 2. Sistema Real de Cores (`NexusPalette` em `NexusTheme.kt`)

### 2.1 Superfícies, Semântica e Acentos

| Token (`NexusPalette`) | Tema Escuro (Padrão) | Tema Claro (**Grafite Suave**) | Aplicação Principal |
| --- | --- | --- | --- |
| `background` | `#050505` | `#D4D7DC` | Fundo principal das telas |
| `surface` | `#0C0C0C` | `#E3E5E8` | Cartões padrão, sheets, barra inferior |
| `surfaceElevated` | `#121212` | `#C9CED4` | Cartões destacados, inputs, chips selecionados |
| `border` | `#242424` | `#B0B6BF` | Contornos neutros `1.dp`, divisores (preservados) |
| `textPrimary` | `#F2F2F2` | `#121417` | Títulos, valores principais, texto ativo |
| `textSecondary` | `#9A9A9A` | `#424852` | Metadados, subtítulos, datas |
| `highlight` | `#FFFFFF` | `#121417` | Ação primária, aba selecionada, wordmark |
| `positive` | `#34C759` | `#136B31` | Concluído, pago, receita realizada, alta |
| `danger` | `#E5484D` | `#AD1E1E` | Atrasado, excedido (>100%), baixa, exclusão |
| `warning` | `#E5A000` | `#874E00` | Atenção, conflito, orçamento 80%–100%, stale |
| `info` | `#4C8BF5` | `#1353A6` | Em andamento, previsto/informativo |
| `accentNeon` | `#7CFFB2` | `#136B31` | Acento principal: anel concluído, série de receitas, seleções |
| `onAccentNeon` | `#051A10` | `#FFFFFF` | Texto/ícone sobre fundo sólido `accentNeon` |
| `chartSecondary` | `#D4D8DF` | `#4E5763` | Segunda série em gráficos comparativos (despesas) |
| `chartTrack` | `#1C1F24` | `#BCC1C9` | Trilho inativo de anéis e barras |

### 2.2 Paleta Categórica de Gráficos (`chartPalette`, 8 cores)
- **Dark:** `#7CFFB2`, `#5CC8FF`, `#C792FF`, `#FFD166`, `#FF7A90`, `#FF9F6B`, `#4DE1C1`, `#A3B1C6`.
- **Light (Grafite Suave):** `#136B31`, `#1353A6`, `#5D3AA8`, `#874E00`, `#AD1E1E`, `#9C4716`, `#0E695A`, `#4E5763`.

### 2.3 Sistema Proporcional de Contornos Coloridos (`ColoredBorderRole`)
Para diferenciar semântica, seleção e alertas sem alterar as bordas neutras (`palette.border` em `1.dp`), `NexusTheme.kt` centraliza `ColoredBorderRole` com espessura proporcional ao porte do componente e opacidade calibrada para Dark vs. Grafite Suave:

| Papel (`ColoredBorderRole`) | Largura | Alpha Dark | Alpha Light | Uso Principal |
| --- | --- | --- | --- | --- |
| `DAY_CARD_SELECTED` | `1.75.dp` | `0.80f` | `0.94f` | Cartão do dia selecionado na visão semanal (`accentNeon`) |
| `ALERT_BANNER_LARGE` | `1.5.dp` | `0.68f` | `0.86f` | `AlertBannerCard` de largura total (`danger` / `warning` / `info`) |
| `HIGHLIGHTED_BUTTON` | `1.5.dp` | `0.78f` | `0.90f` | Botões de ação com destaque semântico (`"Registrar gasto"`) |
| `DAY_CARD_TODAY` | `1.25.dp` | `0.72f` | `0.86f` | Cartão do dia atual na visão semanal (`info`) |
| `MEDIUM_ACCENT` | `1.25.dp` | `0.68f` | `0.84f` | Faixas de evento dia inteiro, banner de permissão, chip de filtro ativo |
| `SMALL_BADGE_DEFINED` | `1.25.dp` | `0.85f` | `0.94f` | Miniaturas funcionais pequenas (`22.dp` checkbox na Agenda) |
| `COMPACT_ITEM` | `1.15.dp` | `0.60f` | `0.80f` | Conflitos em cartões semanais e célula "hoje" na grade mensal |
| `SMALL_BADGE_SUBTLE` | `1.dp` | `0.46f` | `0.62f` | Badges compactos de status/variação (`MarketChangeBadge`, `AgendaKindBadge`) |

- **Bordas neutras preservadas:** cartões padrão (`NexusCard`), divisores e botões comuns mantêm `1.dp` com `palette.border`.

---

## 3. Tipografia (`NexusTypography`), Medidas e Responsividade

| Estilo (`MaterialTheme.typography`) | Especificação Real (`NexusTheme.kt`) | Uso |
| --- | --- | --- |
| `headlineMedium` | `24.sp / 30.sp / SemiBold / -0.3.sp` | Grandes títulos/valores |
| `titleLarge` | `19.sp / 25.sp / SemiBold` | Totais centrais, títulos de subtelas e sheets |
| `titleMedium` | `16.sp / 22.sp / Medium` | Títulos de cartões e linhas de itens |
| `bodyLarge` | `15.sp / 22.sp / Normal` | Textos principais e citações |
| `bodyMedium` | `14.sp / 20.sp / Normal` | Subtítulos, mensagens de estado vazio |
| `labelMedium` | `12.sp / 16.sp / Medium` | Metadados (` · `), chips, legendas |
| `labelSmall` | `11.sp / 14.sp / Medium` | Rótulos curtos de eixo, badges compactos |

### Medidas Recorrentes
- Margem horizontal padrão: `16.dp`; espaçamento inferior para listas com FAB/barra: `96.dp`.
- Raio de cartões principais (`NexusCard`): `16.dp`; botões/inputs: `10.dp–12.dp`; chips/badges: `8.dp–10.dp`. Contorno neutro padrão: `1.dp`.
- **Cabeçalho da Home (`GreetingBlock`):** saudação + nome na mesma linha (`20.sp`, `lineHeight = 26.sp`, `FontWeight.SemiBold`, capitalização convencional, `maxLines = 1` com reticências) alinhada ao botão de personalização (`36.dp`), seguida de data e frase diária discreta.

### 3.1 Regras Adaptativas para Telas Maiores (`BoxWithConstraints`)
- **Agenda Semanal (`AgendaScreen.kt`):**
  - *Largura (`maxWidth >= 720.dp`):* exibe todos os **7 cartões** (segunda a domingo) simultaneamente distribuídos em `Row` com `weight(1f)`; abaixo de `720.dp`, mantém `LazyRow` deslizável com largura proporcional (`216.dp`–`248.dp`) para preservar leitura em celulares.
  - *Altura adaptativa:* altura dos cartões da semana calculada pela janela real — `(maxHeight - 204.dp).coerceIn(240.dp, 560.dp)` em 2 painéis e `(maxHeight - 224.dp).coerceIn(220.dp, 540.dp)` em painel único.
  - *Tablet paisagem (`maxWidth >= 840.dp && maxHeight >= 480.dp && maxWidth > maxHeight`):* layout em **2 painéis** — esquerdo (`0.54f` no Mês/Semana, `0.48f` no Dia/Lista) para calendário/navegação e direito para compromissos/itens do dia selecionado.
- **Telas largas (`maxWidth >= 720.dp`):** `HomeScreen`, `TasksAndProjectsScreen`, `EventsScreen`, `PlanningGoalsScreen`, `FinanceScreen` e `MarketScreen` organizam cards e seções em grade de **2 colunas** ou painéis lado a lado (`HomeCashflowCard` + `HomeCategoryDonutCard`, `Watchlist` `0.48f` + `Alertas` `0.52f`, grupos em 2 colunas). Em `MarketAssetDetailScreen`, `maxWidth >= 840.dp` ativa **2 painéis** (`0.44f` resumo/metadados + `0.56f` gráfico histórico/alertas).

---

## 4. Componentes Compartilhados e Biblioteca de Gráficos (`Canvas` Puro)

### 4.1 Componentes Estruturais (`ui/components/NexusComponents.kt`)
- `NexusWordmark` (identidade **N-1 — Monólito Chanfrado** + **E, X, U, S** *Kinetik Cut*), `NexusMonogramIcon` (**N-1** vetorial), `NexusCard`, `SectionHeader`, `NexusEmptyState`, `TaskRowCard`, `PriorityIndicatorDot`, `MonthYearSelectorBar`, `AlertBannerCard`, `ConfirmDangerDialog`, `ConfirmDiscardChangesDialog`, `NexusDatePickerDialog`, `NexusTimePickerDialog`, `ReminderLeadTimeSelector`, `TopSubScreenBar`, `parseHexColorOrDefault`.

### 4.2 Biblioteca de Gráficos (`ui/components/charts/` — zero libs externas)
1. **`TaskRingChart.kt`:** anel de progresso de tarefas do mês (`Concluídas` em `accentNeon`, `Em andamento` em `info`, `Pendentes` neutras; `Atrasadas` e `Sem prazo` em indicadores separados para não distorcer a proporção).
2. **`DonutChart.kt`:** gráfico de rosca interativo por toque angular em `pointerInput` (`minOf(size.width, size.height) / 2f`) com top 6 categorias + `Outros`, legenda sincronizada e filtro na lista do Financeiro.
3. **`BarGroupChart.kt`:** barras agrupadas de 6 meses (`Receitas` em `accentNeon` vs `Despesas` em `chartSecondary`), alternância `Realizado | Previsto` e seleção por toque no mês.
4. **`ProgressBarRow.kt`:** barra horizontal de orçamento/meta com faixas automáticas (`<=80%`, `80%–100%` `warning`, `>100%` `danger` + `"excedeu R$ X"`).
5. **`Sparkline.kt`:** mini gráfico de tendência em linha + preenchimento gradiente sutil.
6. **`MarketHistoryChart.kt`:** cartão gráfico completo para o detalhe de ativos do Mercado com seletor de janelas (`1D`, `1W`, `1M`, `3M`, `6M`, `1Y`), linha/área colorida pela variação do período, marcadores de máxima/mínima e estado vazio/insuficiente explícito.

---

## 5. Widgets Android (`NexusWidgetTheme.kt`)

`NexusWidgetColors` implementa **suporte dual Claro (Grafite Suave) / Escuro** usando `androidx.glance.color.ColorProvider(day = ..., night = ...)` espelhando os tokens da `NexusPalette`:
- `Background` (`#D4D7DC` / `#050505`), `Surface` (`#E3E5E8` / `#0C0C0C`), `SurfaceElevated` (`#C9CED4` / `#121212`), `Border` (`#B0B6BF` / `#242424` usado em `NexusWidgetDivider`), `TextPrimary` (`#121417` / `#F2F2F2`), `TextSecondary` (`#424852` / `#9A9A9A`), `TextMuted` (`#545B66` / `#777777`), `Highlight` (`#121417` / `#FFFFFF`), `Positive` (`#136B31` / `#34C759`), `OnPositive` (`#FFFFFF` / `#050505`), `Warning` (`#874E00` / `#E5A000`), `Danger` (`#AD1E1E` / `#E5484D`), `Info` (`#1353A6` / `#4C8BF5`).
- **`NexusWidgetShell` sem bordas:** os 4 widgets (`MyDay`, `MyTasks`, `Finance`, `NexusSummary`) usam composição limpa com fundo `Background` e `cornerRadius(16.dp)`, sem contorno externo ao redor do widget.
- Componentes comuns: `NexusWidgetShell`, `NexusWidgetHeader` (refresh + config por instância), `NexusWidgetDivider`, `NexusWidgetEmptyState`, `NexusWidgetErrorState` e `NexusWidgetSizes` (`SMALL`, `MEDIUM`, `LARGE`).

---

## 6. Identidade Visual e Pendências Visuais

- **Implementado no código atual:**
  - `NexusWordmark` monocromático na TopBar de `MainActivity.kt` (removido o `N` verde redundante antes do nome — correção `COR-001`).
  - Ícone de produção Android substituído pelo símbolo **N-1 — Monólito Chanfrado** (`#F2F2F2` centralizado sobre `#050505`) em formato adaptativo (`ic_launcher_nexus_foreground.xml`, `ic_launcher_nexus_background.xml`, `mipmap-anydpi-v26/ic_launcher.xml`, `ic_launcher_round.xml`, fallback `ic_launcher_nexus.xml` e `AndroidManifest.xml`), além de `ic_notification_nexus.xml`.
- **Pendente (`REQ-003` residual):** integração oficial de `androidx.core:core-splashscreen` (`Theme.NEXUS.Splash`) com 0 ms de atraso artificial.
