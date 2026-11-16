# NEXUS — Design System e Padrões Visuais (`NEXUS_DESIGN_SYSTEM`)

> **Finalidade:** definir uma identidade visual única, implementável diretamente em Jetpack Compose e Material 3, eliminando cores soltas, inconsistências entre módulos e widgets exclusivamente escuros.
> **Referência estética:** iOS / Apple Watch Fitness / interfaces financeiras técnicas minimalistas — foco absoluto em legibilidade, contraste nítido, silêncio visual e destaque cromático pontual apenas onde há significado funcional.

---

## 1. Princípios Visuais Inegociáveis

1. **Base neutra, acento cirúrgico:** a interface estrutural é construída em preto, branco e escalas de cinza. Cor nunca é usada como enfeite aleatório; cor comunica **status semântico**, **seleção ativa**, **categoria em gráfico** ou **variação de mercado**.
2. **Tokens centralizados:** nenhuma tela, widget ou gráfico pode conter códigos hex soltos. Toda cor vem de `LocalNexusPalette.current` (`NexusPalette`), exceto `category.colorHex` personalizada pelo usuário com fallback garantido para a paleta oficial.
3. **Tema Escuro (padrão) e Tema Claro sempre equivalentes:** toda tela, sheet, diálogo, gráfico e widget Glance deve manter contraste confortável nos dois temas.
4. **Forma e ícone antes da cor:** na Agenda, no Dashboard e em listas mistas, o usuário reconhece o tipo de registro pela **forma e ícone**, reservando a cor para o estado (atrasado, concluído, atenção) ou categoria.
5. **Densidade respirável e números estáveis:** uso obrigatório de dígitos alinhados (`fontFeatureSettings = "tnum"`) em valores monetários (`R$`), percentuais, cotações e horários (`HH:mm`).

---

## 2. Identidade de Marca (`vector` + `Splash`)

### 2.1 Símbolo N
- **Forma:** monograma geométrico "N" em traço encorpado, vértices precisos e um pequeno corte/chanfro característico na junção diagonal das hastes, garantindo leitura imediata mesmo em tamanhos reduzidos (`20.dp` no cabeçalho até `108.dp` no ícone/splash).
- **Implementação Compose & XML:**
  - Recurso vetorial Android (`ic_nexus_monogram.xml` / `ic_launcher_nexus.xml`) em `#FFFFFF` sobre fundo `#050505` para ícone adaptativo e Splash.
  - Componente Compose `NexusMonogramIcon(size: Dp, tint: Color)` reutilizável no cabeçalho do Dashboard e telas institucionais (`accent` ou `highlight` conforme o tema).
- **Aplicação:**
  - Cabeçalho esquerdo da `HomeScreen`.
  - Ícone adaptativo do launcher (`foreground` branco, `background` `#050505`).
  - Tela de abertura via `androidx.core:core-splashscreen` (`windowSplashScreenBackground = #050505`, `windowSplashScreenAnimatedIcon`, **0 ms de atraso artificial**).

---

## 3. Sistema de Cores e Tokens (`NexusPalette`)

### 3.1 Superfícies, Textos e Semântica (Já existentes em `NexusTheme.kt` — manter e expandir)

| Token (`NexusPalette`) | Tema Escuro (Padrão) | Tema Claro | Uso Obrigatório |
| --- | --- | --- | --- |
| `background` | `#050505` | `#F6F6F6` | Fundo principal de Scaffold/telas |
| `surface` | `#0C0C0C` | `#FFFFFF` | Cartões padrão, sheets, barra inferior |
| `surfaceElevated` | `#121212` | `#EDEDED` | Cartões em destaque, chips selecionados, inputs |
| `border` | `#242424` | `#DCDCDC` | Bordas 1.dp, divisores, trilho de barras |
| `textPrimary` | `#F2F2F2` | `#111111` | Títulos, valores principais, rótulos ativos |
| `textSecondary` | `#9A9A9A` | `#5A5A5A` | Metadados, subtítulos, datas, placeholders |
| `highlight` | `#FFFFFF` | `#000000` | Botão primário, aba selecionada, ícone ativo |
| `positive` | `#34C759` | `#1E8E3E` | Concluído, pago, receita realizada, alta de mercado |
| `danger` | `#E5484D` | `#C62828` | Atrasado, orçamento excedido (>100%), baixa de mercado, exclusão |
| `warning` | `#E5A000` | `#B26A00` | Atenção, conflito de horário, orçamento 80%–100% |
| `info` | `#4C8BF5` | `#1A73E8` | Em andamento, previsto/informativo |

### 3.2 Novo Token de Destaque e Paleta de Gráficos (Adicionar em `NexusTheme.kt`)

| Token | Tema Escuro | Tema Claro | Uso |
| --- | --- | --- | --- |
| `accentNeon` | `#7CFFB2` | `#0E9F63` | Acento único do app: anel principal de progresso/tarefas selecionadas, série de receitas no gráfico comparativo, indicador ativo de período, destaque sutil em ações principais |
| `onAccentNeon` | `#051A10` | `#FFFFFF` | Texto/ícone sobre preenchimento sólido `accentNeon` (contraste >= 4.5:1) |
| `chartSecondary` | `#D4D8DF` | `#4A5568` | Segunda série em gráficos comparativos (ex.: despesas realizadas no `BarGroupChart`) |
| `chartTrack` | `#1C1F24` | `#E3E6EC` | Fundo/trilho inativo de anéis (`DonutChart`, `TaskRingChart`) e barras de progresso |

**Paleta Categórica de Gráficos (`chartPalette: List<Color>`, 8 cores ordenadas):**
Usada em ordem determinística quando uma categoria não possui `colorHex` válido ou quando há múltiplas séries sem cor fixa:

| Índice | Tema Escuro | Tema Claro | Papel sugerido |
| --- | --- | --- | --- |
| 0 | `#7CFFB2` | `#0E9F63` | Verde menta / principal |
| 1 | `#5CC8FF` | `#0B7FBF` | Azul ciano |
| 2 | `#C792FF` | `#7A4FD6` | Violeta |
| 3 | `#FFD166` | `#B98100` | Âmbar claro |
| 4 | `#FF7A90` | `#D6334F` | Coral / rosa |
| 5 | `#FF9F6B` | `#D2691E` | Laranja |
| 6 | `#4DE1C1` | `#0C9C86` | Turquesa |
| 7 | `#A3B1C6` | `#5A6B80` | Ardósia ("Outros") |

> **Regra de contraste:** texto primário sobre qualquer `surface`/`surfaceElevated` nunca deve ficar abaixo de `7:1` no tema escuro e `4.5:1` no tema claro. Nunca renderizar texto branco sobre `accentNeon` `#7CFFB2` no tema escuro — usar sempre `onAccentNeon` (`#051A10`).

---

## 4. Tipografia e Padrões Numéricos

Mantém os estilos base de `NexusTypography` (`NexusTheme.kt`) com uso consistente por hierarquia:

| Estilo | Tamanho / LineHeight / Peso | Uso Canônico |
| --- | --- | --- |
| `headlineMedium` | `24.sp / 30.sp / SemiBold / -0.3.sp` | Título da tela, saudação do Dashboard, hero numéricos |
| `titleLarge` | `19.sp / 25.sp / SemiBold` | Valores principais de resumo (`R$`, percentual central de anel, sub-telas) |
| `titleMedium` | `16.sp / 22.sp / Medium` | Títulos de cartões, nomes de tarefas/eventos/lançamentos |
| `bodyLarge` | `15.sp / 22.sp / Normal` | Conteúdo principal, descrição, citação |
| `bodyMedium` | `14.sp / 20.sp / Normal` | Subtítulos de cartões, corpo de estados vazios |
| `labelMedium` | `12.sp / 16.sp / Medium` | Metadados separados por ` · `, chips, legendas de gráficos |
| `labelSmall` | `11.sp / 14.sp / Medium` | Rótulos curtos de eixo (`jan`, `fev`), badges, avisos discretos |

**Regras tipográficas numéricas:**
- Valores financeiros, contagens em anéis, variações percentuais (`+1,42%` / `-0,85%`) e horários devem aplicar `fontFeatureSettings = "tnum"`.
- Formatação fixa: moeda `R$ 1.234,56` (`pt-BR`), data `dd/MM/yyyy` (ou padrão escolhido em `NexusDateFormat` na saudação), hora `HH:mm` (24h).

---

## 5. Espaçamento, Grid e Forma

| Token / Medida | Valor | Aplicação |
| --- | --- | --- |
| `ScreenPaddingHorizontal` | `16.dp` | Margem lateral padrão de todas as listas e cabeçalhos |
| `CardPadding` | `14.dp` (`10.dp` no modo compacto) | Padding interno padrão de `NexusCard` |
| `SectionGap` | `16.dp` (`10.dp` no modo compacto) | Espaçamento vertical entre blocos no Dashboard |
| `ItemGap` | `10.dp` | Espaçamento vertical entre cards em listas (`LazyColumn`) |
| `CornerRadiusCard` | `16.dp` | Cartões principais (`NexusCard`), painéis de gráficos |
| `CornerRadiusInputButton` | `10.dp` – `12.dp` | Botões, campos de texto, banners reduzidos, miniaturas |
| `CornerRadiusChip` | `8.dp` – `10.dp` | Chips de filtro, selos de tipo e status (evitar excesso de pílulas cheias sem necessidade) |
| `BorderWidth` | `1.dp` | Contorno sutil (`palette.border`) em cards, sheets e inputs |
| `BottomListClearance` | `96.dp` | Padding inferior das listas para não colidir com `FAB` / `NavigationBar` |
| `MinTouchTarget` | `48.dp` | Área de toque mínima obrigatória para checkboxes, ações, setas de mês e ícones de barra |

---

## 6. Diferenciação Visual por Forma e Ícone

Especialmente na **Agenda** e na **Home**, diferentes tipos de registro convivem lado a lado. A diferenciação primária é **geométrica e iconográfica**:

| Tipo de Registro | Ícone / Forma na Lista | Cor Aplicada |
| --- | --- | --- |
| **Tarefa** | Checkbox / caixinha de seleção (`Icons.Outlined.CheckBoxOutlineBlank` / `CheckBox`) | Neutra pendente; `positive` concluída; `danger` atrasada |
| **Compromisso** | Barra vertical de duração (`3.dp × fillHeight`) à esquerda + ícone de relógio | `info` / neutra; `warning` se houver conflito de horário |
| **Evento** | Selo / badge com ícone de ingresso/evento (`Icons.Outlined.Event` ou `ConfirmationNumber`) + faixa horizontal superior quando multi-dia | Categoria do evento ou neutra |
| **Financeiro (Conta / Fatura)** | Símbolo monetário `R$` em container quadrado arredondado + indicador de status | `positive` receita/pago; `danger` atrasado; neutra pendente |
| **Projeto (Prazo)** | Ícone de bandeira (`Icons.Outlined.Flag`) | Neutra; `danger` se prazo vencido |

---

## 7. Componentes Reutilizáveis em Jetpack Compose

### 7.1 Componentes Estruturais (`ui/components/NexusComponents.kt`)
- `NexusCard(elevated, borderColorOverride?, onClick?, content)` — cartão padrão com borda `1.dp` e fundo `surface`/`surfaceElevated`.
- `SectionHeader(title, trailing?, actionLabel?, onAction?)` — cabeçalho padronizado de seção com ação opcional de "Ver tudo".
- `NexusEmptyState(icon, title, subtitle, actionLabel?, onAction?)` — estado vazio limpo e sem cobranças.
- `TaskRowCard(item, onClick, onToggleComplete)` — linha padrão de tarefa com prioridade, subtarefas (`2/5`), projeto/evento/gasto vinculado (`Gasto: R$ X`) e atraso.
- `MonthYearSelectorBar(currentMonth: YearMonth, onPrevious, onNext, onSelectToday?)` — barra `< mês/ano >` compartilhada entre Dashboard (tarefas do mês), Financeiro e Agenda.
- `AlertBannerCard(title, subtitle, severity: AlertSeverity, onClick)` — bloco compacto para alertas críticos reais (contas vencidas, faturas fechadas vencendo hoje, conflitos de agenda, tarefas urgentes atrasadas).
- `ConfirmDangerDialog`, `ConfirmDiscardChangesDialog`, `NexusDatePickerDialog`, `NexusTimePickerDialog`, `ReminderLeadTimeSelector`, `TopSubScreenBar`.

### 7.2 Biblioteca de Gráficos Canvas (`ui/components/charts/`)

Todos os gráficos devem ser implementados **exclusivamente em Jetpack Compose `Canvas`**, sem bibliotecas externas, aceitando dados e cores por parâmetro, respeitando tema claro/escuro, animando suavemente na entrada (`animateFloatAsState`), expondo `Modifier.semantics { contentDescription = ... }` e exibindo fallback limpo quando sem dados:

1. **`TaskRingChart` (`ui/components/charts/TaskRingChart.kt`)**
   - **Uso:** Dashboard (bloco central de tarefas) e resumo opcional na tela de Tarefas.
   - **Visual:** anel estilo Apple Watch (`strokeWidth = 14.dp..18.dp`, `StrokeCap.Round`) dividido proporcionalmente em segmentos: Concluídas (`accentNeon`), Em andamento (`info`), Pendentes (`textSecondary` suavizado), Atrasadas (`danger`).
   - **Centro do anel:** percentual de conclusão (`72%`) em `titleLarge` + subtítulo (`18/25 tarefas` ou `Concluídas`).
   - **Interação:** tocar em um segmento ou indicador de status abaixo filtra ou abre a visualização correspondente.
   - **Estado vazio:** anel neutro contínuo (`chartTrack`) com `0%` e `"Sem tarefas no período"`.

2. **`DonutChart` (`ui/components/charts/DonutChart.kt`)**
   - **Modelo de entrada:** `DonutSlice(id: String?, label: String, valueCents: Long, color: Color, percentage: Float)`.
   - **Visual:** rosca segmentada (`strokeWidth` proporcional, pequeno espaçamento angular entre fatias quando >1 fatia), fatia selecionada levemente expandida (`+4.dp` de raio/espessura) e demais fatias com `alpha = 0.45f` quando há seleção ativa.
   - **Centro:** valor total formatado em `R$` + rótulo configurável (ex.: `"Despesas"`).
   - **Interação:** `pointerInput` detecta toque angular sobre o anel e dispara `onSliceClick(slice.id)` (tocar na mesma fatia desmarca); legenda sincronizada abaixo com bolinha de cor, nome, valor em `R$` e percentual ordenado decrescente.
   - **Acessibilidade:** `contentDescription` por fatia (ex.: `"Alimentação, R$ 420,00, 28%"`).
   - **Estado vazio:** mensagem neutra (`"Sem lançamentos neste mês"`).

3. **`BarGroupChart` (`ui/components/charts/BarGroupChart.kt`)**
   - **Modelo de entrada:** `BarGroupEntry(id: String, label: String, primaryValueCents: Long, secondaryValueCents: Long)`.
   - **Visual:** até 6 grupos mensais (`jan`, `fev`...) com duas barras verticais lado a lado por grupo: série 1 = Receitas (`accentNeon`), série 2 = Despesas (`chartSecondary`), cantos superiores arredondados (`4.dp`), linha de base em `palette.border`.
   - **Interação:** toque seleciona a coluna do mês, exibe balão/resumo flutuante com os valores exatos de Receita, Despesa e Resultado daquele mês e sincroniza o seletor mensal do painel.

4. **`ProgressBarRow` (`ui/components/charts/ProgressBarRow.kt`)**
   - **Uso:** Orçamentos por categoria, metas financeiras e metas de projetos/objetivos.
   - **Visual:** barra horizontal com trilho `chartTrack`, preenchimento animado limitado visualmente a `100%` (`coerceIn(0f, 1f)`), rótulo à esquerda, texto `"R$ gasto de R$ limite (xx%)"` à direita e regra de cor semântica automática para orçamentos (`<=80%` neutra/`accentNeon`, `80%..100%` `warning`, `>100%` `danger` + sufixo `"excedeu R$ X"`).

5. **`Sparkline` (`ui/components/charts/Sparkline.kt`)**
   - **Uso:** cards do módulo Mercado (tendência de 30 dias para Dólar, Euro, Bitcoin e Ibovespa).
   - **Visual:** linha contínua suavizada (`Stroke(2.dp)`) em `positive` se variação do período `>= 0` ou `danger` se `< 0`, com área inferior em gradiente sutil (`alpha 0.18f -> 0.0f`) e ponto marcador no valor mais recente. Sem eixos ruidosos.
   - **Estado vazio / cache insuficiente:** linha horizontal tracejada neutra com texto `"Histórico aguardando pontos"`.

---

## 8. Estados Padronizados da Interface

| Estado | Regra Visual e Comportamental |
| --- | --- |
| **Vazio (`Empty`)** | Nunca exibir tela em branco nem mensagens punitivas/de culpa. Usar `NexusEmptyState` com ícone `32.dp`, título direto, explicação objetiva e ação primária quando aplicável. |
| **Carregamento (`Loading`)** | Preferir dados em cache imediato via `StateFlow`. Em operações assíncronas (importação ZIP/JSON, atualização de Mercado), usar indicador discreto (`LinearProgressIndicator` fino ou botão com spinner) sem bloquear o resto do app. |
| **Erro (`Error`)** | Nunca exibir `Exception`, stacktrace ou código HTTP cru ao usuário. Mostrar cartão de aviso amigável (`palette.warning` ou `palette.danger`) mantendo dados em cache visíveis e registrando erro técnico apenas em `error_logs` para a tela de Diagnóstico. |
| **Alterações não salvas** | Qualquer formulário modificado ao receber toque em Voltar/Fechar exibe `ConfirmDiscardChangesDialog` (`"Continuar editando"` / `"Descartar"`). |
| **Exclusão com vínculos** | Quando um registro pai possui filhos vinculados (ex.: tarefa com gastos, evento com compromissos/custos/tarefas, projeto com tarefas), o diálogo de exclusão informa explicitamente que os vínculos serão desfeitos sem apagar os registros filhos (exceto subtarefas/marcos/logs em cascata, que devem ser citados). |

---

## 9. Animações e Microinterações

- **Princípio:** rápidas (`180ms–260ms`), com `FastOutSlowInEasing`, apenas para reforçar mudança de estado (preenchimento de anéis/barras, expansão de `"Mais detalhes"`, alternância de mês, recolhimento suave do cabeçalho na rolagem).
- **Respeito ao sistema:** evitar loops infinitos de pulso/brilho; nenhuma animação deve atrasar toque ou carregamento inicial.

---

## 10. Widgets Android (`NexusWidgetTheme`)

**Problema atual a corrigir:** `NexusWidgetTheme.kt` hoje é exclusivamente escuro (`0xFF050505`).
- **Padrão definitivo:** `NexusWidgetColors` deve oferecer suporte dual Claro/Escuro via `ColorProvider(day = LightColor, night = DarkColor)` do Glance/Resources ou seguir a preferência de tema configurada no app (`settings.darkTheme` sincronizada no `NexusWidgetUpdater`), usando os mesmos tokens de `NexusPalette` (`background`, `surface`, `border`, `textPrimary`, `textSecondary`, `accentNeon`, `positive`, `warning`, `danger`).
- Todos os 4 widgets mantêm `SizeMode.Responsive` (`SMALL`, `MEDIUM`, `LARGE`), header com ação de refresh/configuração e fallback explícito `NexusWidgetErrorState` em caso de falha de leitura local.
