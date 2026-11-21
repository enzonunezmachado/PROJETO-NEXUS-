# 06 — Design System e Padrões Visuais (`NEXUS Design System`)

> **Base verificada:** `app/src/main/java/com/nexus/app/ui/theme/NexusTheme.kt`, `ui/components/NexusComponents.kt`, `ui/components/charts/*` e `widgets/NexusWidgetTheme.kt`.

---

## 1. Princípios Visuais

1. **Base neutra, acento cirúrgico:** estrutura em preto, branco e escalas de cinza. Cor comunica apenas **status semântico**, **acento de destaque (`accentNeon`)**, **categoria em gráfico** ou **variação/severidade no Mercado**.
2. **Tokens centralizados:** todas as telas usam `LocalNexusPalette.current` (`NexusPalette`), evitando hexadecimais avulsos em telas/componentes (exceto fallback e `category.colorHex` personalizada pelo usuário).
3. **Equivalência Dark / Light:** tema escuro (`darkTheme = true`) é o padrão inicial (`SettingsEntity`), com alternância completa para tema claro em app e widgets.
4. **Forma e ícone antes da cor:** na Agenda e listas mistas, o tipo de item é distinguido por ícone/forma; a cor indica estado (atrasado, concluído, atenção).
5. **Alinhamento tabular (`tnum`):** valores monetários (`R$`, `US$`), cotações, variações percentuais e horários aplicam `fontFeatureSettings = "tnum"`.

---

## 2. Sistema Real de Cores (`NexusPalette` em `NexusTheme.kt`)

### 2.1 Superfícies, Semântica e Acentos

| Token (`NexusPalette`) | Tema Escuro (Padrão) | Tema Claro | Aplicação Principal |
| --- | --- | --- | --- |
| `background` | `#050505` | `#F6F6F6` | Fundo principal das telas |
| `surface` | `#0C0C0C` | `#FFFFFF` | Cartões padrão, sheets, barra inferior |
| `surfaceElevated` | `#121212` | `#EDEDED` | Cartões destacados, inputs, chips selecionados |
| `border` | `#242424` | `#DCDCDC` | Contornos `1.dp`, divisores |
| `textPrimary` | `#F2F2F2` | `#111111` | Títulos, valores principais, texto ativo |
| `textSecondary` | `#9A9A9A` | `#5A5A5A` | Metadados, subtítulos, datas |
| `highlight` | `#FFFFFF` | `#000000` | Ação primária, aba selecionada |
| `positive` | `#34C759` | `#1E8E3E` | Concluído, pago, receita realizada, alta |
| `danger` | `#E5484D` | `#C62828` | Atrasado, excedido (>100%), baixa, exclusão |
| `warning` | `#E5A000` | `#B26A00` | Atenção, conflito, orçamento 80%–100%, stale |
| `info` | `#4C8BF5` | `#1A73E8` | Em andamento, previsto/informativo |
| `accentNeon` | `#7CFFB2` | `#0E9F63` | Acento principal: anel concluído, série de receitas, seleções de destaque |
| `onAccentNeon` | `#051A10` | `#FFFFFF` | Texto/ícone sobre fundo sólido `accentNeon` |
| `chartSecondary` | `#D4D8DF` | `#4A5568` | Segunda série em gráficos comparativos (despesas) |
| `chartTrack` | `#1C1F24` | `#E3E6EC` | Trilho inativo de anéis e barras |

### 2.2 Paleta Categórica de Gráficos (`chartPalette`, 8 cores)
- **Dark:** `#7CFFB2`, `#5CC8FF`, `#C792FF`, `#FFD166`, `#FF7A90`, `#FF9F6B`, `#4DE1C1`, `#A3B1C6`.
- **Light:** `#0E9F63`, `#0B7FBF`, `#7A4FD6`, `#B98100`, `#D6334F`, `#D2691E`, `#0C9C86`, `#5A6B80`.

---

## 3. Tipografia (`NexusTypography`) e Medidas Canônicas

| Estilo (`MaterialTheme.typography`) | Especificação Real (`NexusTheme.kt`) | Uso |
| --- | --- | --- |
| `headlineMedium` | `24.sp / 30.sp / SemiBold / -0.3.sp` | Saudação do Dashboard, grandes títulos/valores |
| `titleLarge` | `19.sp / 25.sp / SemiBold` | Totais centrais, títulos de subtelas e sheets |
| `titleMedium` | `16.sp / 22.sp / Medium` | Títulos de cartões e linhas de itens |
| `bodyLarge` | `15.sp / 22.sp / Normal` | Textos principais e citações |
| `bodyMedium` | `14.sp / 20.sp / Normal` | Subtítulos, mensagens de estado vazio |
| `labelMedium` | `12.sp / 16.sp / Medium` | Metadados (` · `), chips, legendas |
| `labelSmall` | `11.sp / 14.sp / Medium` | Rótulos curtos de eixo, badges compactos |

### Medidas Recorrentes
- Margem horizontal padrão: `16.dp`; espaçamento inferior para listas com FAB/barra: `96.dp`.
- Raio de cartões principais (`NexusCard`): `16.dp`; botões/inputs: `10.dp–12.dp`; chips/badges: `8.dp–10.dp`. Contorno: `1.dp`.

---

## 4. Componentes Compartilhados e Biblioteca de Gráficos (`Canvas` Puro)

### 4.1 Componentes Estruturais (`ui/components/NexusComponents.kt`)
- `NexusCard`, `SectionHeader`, `NexusEmptyState`, `TaskRowCard`, `PriorityIndicatorDot`, `MonthYearSelectorBar`, `AlertBannerCard`, `ConfirmDangerDialog`, `ConfirmDiscardChangesDialog`, `NexusDatePickerDialog`, `NexusTimePickerDialog`, `ReminderLeadTimeSelector`, `TopSubScreenBar`, `parseHexColorOrDefault`.

### 4.2 Biblioteca de Gráficos (`ui/components/charts/` — zero libs externas)
1. **`TaskRingChart.kt`:** anel de progresso de tarefas do mês (`Concluídas` em `accentNeon`, `Em andamento` em `info`, `Pendentes` neutras; `Atrasadas` e `Sem prazo` em indicadores separados para não distorcer a proporção).
2. **`DonutChart.kt`:** gráfico de rosca interativo por toque angular em `pointerInput` (`minOf(size.width, size.height) / 2f`) com top 6 categorias + `Outros`, legenda sincronizada e filtro na lista do Financeiro.
3. **`BarGroupChart.kt`:** barras agrupadas de 6 meses (`Receitas` em `accentNeon` vs `Despesas` em `chartSecondary`), alternância `Realizado | Previsto` e seleção por toque no mês.
4. **`ProgressBarRow.kt`:** barra horizontal de orçamento/meta com faixas automáticas (`<=80%`, `80%–100%` `warning`, `>100%` `danger` + `"excedeu R$ X"`).
5. **`Sparkline.kt`:** mini gráfico de tendência em linha + preenchimento gradiente sutil.
6. **`MarketHistoryChart.kt`:** cartão gráfico completo para o detalhe de ativos do Mercado com seletor de janelas (`1D`, `1W`, `1M`, `3M`, `6M`, `1Y`), linha/área colorida pela variação do período, marcadores de máxima/mínima e estado vazio/insuficiente explícito.

---

## 5. Widgets Android (`NexusWidgetTheme.kt`)

Diferente das versões antigas, `NexusWidgetColors` já implementa **suporte dual Claro/Escuro** usando `androidx.glance.color.ColorProvider(day = ..., night = ...)` espelhando os tokens da `NexusPalette`:
- `Background` (`#F6F6F6` / `#050505`), `Surface` (`#FFFFFF` / `#0C0C0C`), `SurfaceElevated` (`#EDEDED` / `#121212`), `Border` (`#DCDCDC` / `#242424`), `TextPrimary`, `TextSecondary`, `TextMuted`, `Highlight`, `Positive`, `OnPositive`, `Warning`, `Danger`, `Info`.
- Componentes comuns: `NexusWidgetShell`, `NexusWidgetHeader` (refresh + config por instância), `NexusWidgetDivider`, `NexusWidgetEmptyState`, `NexusWidgetErrorState` e `NexusWidgetSizes` (`SMALL`, `MEDIUM`, `LARGE`).

---

## 6. Identidade Visual e Pendências Visuais

- **Presente hoje:** `ic_launcher_nexus.xml` e `ic_notification_nexus.xml` vetoriais.
- **Pendente (`REQ-003`):** integração oficial de `androidx.core:core-splashscreen` (`Theme.NEXUS.Splash`) com monograma N geométrico dedicado e 0 ms de atraso artificial.
