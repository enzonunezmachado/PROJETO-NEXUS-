implementação visual e adaptativa autorizada na PARTE 1 de OlhaaquiGoogle aStudio.md, a sincronização factual da documentação em /docs/ e artefatos internos, as verificações estáticas possíveis no container e a análise exclusivamente documental da PARTE 2 foram concluídas.
A. Implementações realizadas
Resumo das mudanças
Tema Claro Grafite Suave: substituição dos tons excessivamente brancos do tema claro anterior por uma paleta em grafite equilibrada no app (LightPalette em NexusTheme.kt) e nos widgets Glance (NexusWidgetColors em NexusWidgetTheme.kt), preservando o tema escuro base e o significado semântico de vermelho (atrasos/baixa), âmbar (conflitos/atenção), verde (positivo/seleção) e azul (informação).
Sistema proporcional de contornos coloridos (ColoredBorderRole): criação de papéis proporcionais de largura e opacidade calibrada para Dark e Grafite Suave, com revisão dos contornos coloridos em banners, cartões semanais, botões destacados, badges e chips ativos — mantendo todas as bordas neutras (palette.border) inalteradas.
Widgets sem bordas externas: remoção da borda ao redor de NexusWidgetShell para todos os 4 widgets Jetpack Glance (MyDay, MyTasks, Finance, NexusSummary).
Identidade tipográfica NEXUS + correção COR-001: implementação do NexusWordmark vetorial combinando N-1 — Monólito Chanfrado e E, X, U, S Kinetik Cut, atualização do NexusMonogramIcon para N-1, e remoção do N verde redundante antes do nome NEXUS na TopBar (MainActivity.kt).
Substituição do ícone Android de produção: novo ícone adaptativo centralizado com N-1 branco (#F2F2F2) sobre fundo preto (#050505), cobrindo foreground/background, roundIcon, mipmap-anydpi-v26 e fallback em drawable.
Saudação da Home na mesma linha (GreetingBlock): saudação contextual e nome exibidos lado a lado na mesma linha, em capitalização convencional e composição compacta.
Agenda semanal e layouts adaptativos para telas maiores:
Altura dinâmica dos cards semanais proporcional à altura útil real da janela (maxHeight).
Alternância automática baseada em constraints (maxWidth >= 720.dp) entre modo expandido (7 dias simultâneos) e modo deslizável (LazyRow), preservando selectedDate e semana consultada.
Composição em 2 painéis na Agenda para tablets em paisagem (maxWidth >= 840.dp && maxHeight >= 480.dp && maxWidth > maxHeight), atualizando os itens do painel direito ao tocar em um dia no painel esquerdo.
Reorganização responsiva em 2 colunas / 2 painéis (>= 720.dp e >= 840.dp) em HomeScreen, TasksAndProjectsScreen, EventsScreen, PlanningGoalsScreen, FinanceScreen, MarketScreen e MarketAssetDetailScreen.
B. Arquivos alterados
Código e recursos Android (app/src/main/)
app/src/main/java/com/nexus/app/ui/theme/NexusTheme.kt — tokens do tema claro Grafite Suave, chartPalette calibrada e sistema ColoredBorderRole (coloredBorderWidth, coloredBorderColor, coloredBorder).
app/src/main/java/com/nexus/app/widgets/NexusWidgetTheme.kt — tokens day em Grafite Suave e remoção da borda externa em NexusWidgetShell.
app/src/main/java/com/nexus/app/ui/components/NexusComponents.kt — NexusWordmark (N-1 + Kinetik Cut), NexusMonogramIcon (N-1) e contorno proporcional (ALERT_BANNER_LARGE) em AlertBannerCard.
app/src/main/java/com/nexus/app/MainActivity.kt — correção COR-001 na TopBar usando NexusWordmark monocromático sem N verde redundante e limpeza do import ocioso MaterialTheme.
app/src/main/res/drawable/ic_launcher_nexus_background.xml — fundo preto #050505 do ícone adaptativo.
app/src/main/res/drawable/ic_launcher_nexus_foreground.xml — camada foreground 108x108 com N-1 #F2F2F2 centralizado na zona segura.
app/src/main/res/drawable/ic_launcher_nexus.xml — ícone fallback vetorial com N-1 #F2F2F2 sobre #050505.
app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml e ic_launcher_round.xml — apontam background e foreground adaptativos.
app/src/main/AndroidManifest.xml — android:icon="@mipmap/ic_launcher" e android:roundIcon="@mipmap/ic_launcher_round".
app/src/main/java/com/nexus/app/ui/components/MarketQuoteComponents.kt — borda sutil (SMALL_BADGE_SUBTLE) em MarketChangeBadge e MarketQuoteStatusBadge.
app/src/main/java/com/nexus/app/ui/screens/MarketAlertSheetsAndCards.kt — bordas proporcionais em MarketNotificationPermissionBanner (MEDIUM_ACCENT) e MarketAlertStatusBadge (SMALL_BADGE_SUBTLE).
app/src/main/java/com/nexus/app/ui/screens/EntityEditors.kt — borda proporcional HIGHLIGHTED_BUTTON no botão "Registrar gasto" quando destacado.
app/src/main/java/com/nexus/app/ui/screens/AgendaScreen.kt — altura semanal adaptável, modo 7 dias vs. deslizável, 2 painéis em tablet paisagem e bordas coloridas proporcionais nos componentes da Agenda.
app/src/main/java/com/nexus/app/ui/screens/HomeScreen.kt — GreetingBlock na mesma linha e reorganização em 2 colunas/painéis para telas largas (>= 720.dp).
app/src/main/java/com/nexus/app/ui/screens/TasksAndProjectsScreen.kt — grade em 2 colunas para listas de tarefas e projetos em telas largas (>= 720.dp).
app/src/main/java/com/nexus/app/ui/screens/EventsScreen.kt — grade em 2 colunas para cartões de eventos em telas largas (>= 720.dp).
app/src/main/java/com/nexus/app/ui/screens/PlanningGoalsScreen.kt — grade em 2 colunas para objetivos em telas largas (>= 720.dp).
app/src/main/java/com/nexus/app/ui/screens/FinanceScreen.kt — composição em 2 colunas nos cards de visão geral e listas das 5 abas em telas largas (>= 720.dp).
app/src/main/java/com/nexus/app/ui/screens/MarketScreen.kt — Watchlist + Alertas lado a lado e grupos em 2 colunas (>= 720.dp), e MarketAssetDetailScreen em 2 painéis (>= 840.dp).
(Preservados intactos: Room DB, entidades, DAOs, migrações, regras de domínio, BackupManager, NavHost inexistente, ic_notification_nexus.xml e protótipos em design-explorations/.)
C. Decisões visuais e técnicas
Tema Grafite Suave: adotados #D4D7DC (fundo), #E3E5E8 (superfície), #C9CED4 (superfície elevada), #B0B6BF (borda neutra) e #121417 / #424852 (textos) para reduzir o brilho excessivo preservando separação entre cartões e fundo, tanto no app quanto nos widgets. Cores semânticas e de gráficos no tema claro foram escurecidas (#136B31, #AD1E1E, #874E00, #1353A6) para manter contraste sobre cinza médio.
Wordmark & Launcher Icon: em vez de fontes textuais, NexusWordmark desenha exatamente o vetor geométrico aprovado (N-1 + E, X, U, S Kinetik Cut em caixa 318x64) e os XMLs do ícone usam o mesmo traçado vetorial N-1 em #F2F2F2 sobre #050505 dentro da safe-zone de 108x108 do launcher adaptativo.
Adaptação sem refatoração arquitetural: usado BoxWithConstraints localmente em cada tela em vez de introduzir novas dependências ou mexer no container/navegação de MainActivity, mantendo compatibilidade direta com a arquitetura atual.
D. Revisão de bordas
Centralização em ColoredBorderRole (NexusTheme.kt):
DAY_CARD_SELECTED: 1.75.dp (alpha 0.80 Dark / 0.94 Light) — referência visual principal na seleção do dia da semana.
ALERT_BANNER_LARGE: 1.5.dp (0.68 Dark / 0.86 Light) — banners grandes de alerta na Home (AlertBannerCard).
HIGHLIGHTED_BUTTON: 1.5.dp (0.78 Dark / 0.90 Light) — botão "Registrar gasto" com destaque.
DAY_CARD_TODAY: 1.25.dp (0.72 Dark / 0.86 Light) — dia atual na semana.
MEDIUM_ACCENT: 1.25.dp (0.68 Dark / 0.84 Light) — faixas dia inteiro, chip de filtro ativo da Agenda e banner de permissão de notificações do Mercado.
SMALL_BADGE_DEFINED: 1.25.dp (0.85 Dark / 0.94 Light) — mini-checkbox 22.dp dentro da Agenda semanal/diária.
COMPACT_ITEM: 1.15.dp (0.60 Dark / 0.80 Light) — conflitos em cards semanais e célula "hoje" na grade mensal.
SMALL_BADGE_SUBTLE: 1.dp (0.46 Dark / 0.62 Light) — badges inline compactos (AgendaKindBadge, MarketChangeBadge, MarketQuoteStatusBadge, MarketAlertStatusBadge).
Preservação das bordas neutras: todos os NexusCard, divisores (HorizontalDivider), chips neutros e botões comuns continuam usando 1.dp com palette.border sem alterações de cor ou espessura.
Widgets sem bordas: removido o Box envolvente com .border(1.dp, NexusWidgetColors.Border) de NexusWidgetShell em NexusWidgetTheme.kt; todos os 4 widgets agora exibem fundo Background com cornerRadius(16.dp) e composição limpa.
E. Comportamento adaptativo
Critério da Agenda Semanal (AgendaScreen.kt):
A decisão é calculada exclusivamente pelas constraints reais de largura da área da Agenda via BoxWithConstraints:
Modo expandido (7 dias simultâneos): ativado quando maxWidth >= 720.dp, garantindo mínimo aproximado confortável por coluna (7 colunas divididas igualmente em Row com weight(1f) e spacedBy(8.dp)).
Modo deslizável (LazyRow): ativado quando maxWidth < 720.dp (incluindo celulares em retrato ou o painel esquerdo estreito do tablet em modo 2 painéis quando < 720.dp), usando largura por card proporcional (216.dp a 248.dp) para não comprimir textos nem quebrar leitura.
Altura adaptável: a altura dos cartões semanais é derivada de maxHeight: (maxHeight - 204.dp).coerceIn(240.dp, 560.dp) em 2 painéis e (maxHeight - 224.dp).coerceIn(220.dp, 540.dp) em 1 painel.
Preservação de contexto: selectedDate e o intervalo da semana são mantidos durante recomposições/redimensionamentos de janela (e o LazyRow rola até o dia selecionado quando volta ao modo deslizável). (Nota: recriação completa da Activity em rotação continua sujeita ao BUG-001, fora deste lote.)
Tablet paisagem na Agenda: quando maxWidth >= 840.dp && maxHeight >= 480.dp && maxWidth > maxHeight, exibe 2 painéis (esquerdo com calendário/semana/cabeçalho e direito com a lista do dia selecionado atualizada a cada toque).
Demais telas largas: HomeScreen, TasksAndProjectsScreen, EventsScreen, PlanningGoalsScreen, FinanceScreen e MarketScreen ativam 2 colunas/painéis em maxWidth >= 720.dp; MarketAssetDetailScreen ativa 2 painéis em maxWidth >= 840.dp.
F. Verificações
Executado e aprovado nesta verificação final (via python3)
Parser XML: validados sem erros de sintaxe AndroidManifest.xml, ic_launcher_nexus_background.xml, ic_launcher_nexus_foreground.xml, ic_launcher_nexus.xml, ic_notification_nexus.xml, mipmap-anydpi-v26/ic_launcher.xml e ic_launcher_round.xml.
Consistência de referências de ícone: confirmado vínculo @mipmap/ic_launcher / @mipmap/ic_launcher_round no manifesto e referências @drawable/ic_launcher_nexus_background / foreground nos adaptive icons.
Balanceamento sintático Kotlin: todos os 14 arquivos .kt modificados validados com contagem exata 0 de ( ), { } e [ ].
Auditoria de regras e tokens: verificado que os tokens hexadecimais Grafite Suave estão presentes em NexusTheme.kt e NexusWidgetTheme.kt, NexusWidgetShell não possui .border(, MainActivity.kt não referencia mais NexusMonogramIcon nem MaterialTheme ocioso, AgendaScreen.kt usa 720.dp / 840.dp x 480.dp / weekCardHeight, e os arquivos em design-explorations/ não foram alterados.
Não pôde ser verificado neste ambiente
Toolchain ausente no container: java, javac, gradle, kotlinc e ./gradlew não estão instalados neste ambiente.
Por essa limitação, não foram executados: compilação Android (:app:kspDebugKotlin, :app:compileDebugKotlin, :app:assembleDebug), suíte JUnit (:app:testDebugUnitTest), nem validação visual/runtime em emulador ou dispositivo físico (celular e tablet paisagem) para temas Claro/Escuro, bordas proporcionais, widgets Glance, Agenda adaptativa e launcher icon.
G. Documentação
Atualizados no repositório ativo (/docs/) e artefatos internos
/docs/06_DESIGN_SYSTEM.md e artefato NEXUS_DESIGN_SYSTEM.md
/docs/07_ESTADO_ATUAL.md e artefato NEXUS_CURRENT_STATE.md
/docs/05_FLUXOS_COMPORTAMENTOS.md
/docs/02_ESPECIFICACAO_PRODUTO.md e artefato NEXUS_PRODUCT_SPEC.md (REQ-003)
/docs/10_ROADMAP.md e artefato NEXUS_IMPLEMENTATION_ROADMAP.md (§1.1, §2, §3)
/docs/12_RASTREABILIDADE.md (REQ-003)
/docs/README.md (data de revisão 2026-11-22)
Artefato NEXUS_INTEGRATION_MAP.md (cabeçalho de revisão) e NEXUS_CONTINUIDADE.md (Seções 7, 8, 9 e nova Seção 12)
Limitação de sincronização externa
Este ambiente está conectado exclusivamente ao repositório ativo PROJETO-NEXUS-. Não há acesso ao repositório separado NEXUS-DOCUMENTACAO, portanto nenhuma sincronização com esse repositório externo foi realizada ou presumida.
H. Análise das demais ideias (PARTE 2 — Somente Analítica, Sem Implementação)
Nota de governança: nenhuma das ideias abaixo foi implementada nem teve seu estado documental de aprovação alterado. Todas permanecem nos estados já registrados nos documentos (/docs/02, 06, 07, 10, 12 e NEXUS_CONTINUIDADE.md).
Ideia 1 — Splash Screen Nativa com androidx.core:core-splashscreen (REQ-003 residual)
Nome e descrição: Integração oficial da biblioteca androidx.core:core-splashscreen com tema Theme.NEXUS.Splash exibindo o símbolo N-1 na abertura fria sem atraso artificial (0 ms).
Problema que pretende resolver: Eliminar transição genérica/branca ou inconsistência visual entre o toque no ícone do launcher e o primeiro frame do MainActivity.
Benefício esperado para o usuário: Continuidade visual imediata entre o novo launcher icon N-1 e a interface do app.
Telas e módulos afetados: Shell de entrada (MainActivity, AndroidManifest.xml, res/values/themes.xml, app/build.gradle.kts, gradle/libs.versions.toml).
Dependências com funcionalidades existentes: Nenhuma de regra de negócio; reutiliza o ic_launcher_nexus_foreground N-1 já implementado.
Impacto no Design System: Baixo; formaliza apenas o tema de splash com fundo escuro #050505 (ou adaptação DayNight do sistema) e símbolo N-1.
Impacto técnico e arquitetural: Baixo; adição de dependência Gradle e chamada de installSplashScreen() antes de super.onCreate() mantendo enableEdgeToEdge().
Complexidade estimada: Baixa.
Riscos e possíveis conflitos: Interação entre tema de splash pós-Android 12 (postSplashScreenTheme) e enableEdgeToEdge() / statusBarsPadding(); exige teste em API 26–35.
Critérios de aceitação que ainda precisam ser definidos: Comportamento visual em tema do sistema claro vs. escuro no boot frio e garantia de 0 ms de bloqueio proposital.
Possibilidade de agrupamento com outras ideias: Pode ser agrupada com ajustes finos de shell/UI (Ideia 5) ou com a correção do BUG-001 no ciclo de vida do MainActivity.
Prioridade sugerida: Média.
Ideia 2 — Eventos Evoluídos: locationMode, ocultação padrão de taxas e banner local (REQ-014)
Nome e descrição: Evolução de EventEntity e EventsScreen/EventEditorSheet com modo de local (IN_PERSON, ONLINE, TBD), ocultação padrão do campo ticketFeesCents (mantendo leitura legada) e banner JPEG local escolhido via PickVisualMedia armazenado em filesDir/event_banners/<id>.jpg.
Problema que pretende resolver: Formulário de evento ainda expõe ticketFeesCents por padrão, não distingue formalmente evento online vs. presencial e não suporta imagem de capa local.
Benefício esperado para o usuário: Cadastro de eventos mais limpo, sem sugestão visual de taxas e com identificação visual rápida por banner.
Telas e módulos afetados: Eventos (EventsScreen, EventEditorSheet, NexusRepository, NexusDatabase, NexusMigrations, BackupManager).
Dependências com funcionalidades existentes: Depende de migração aditiva Room (DATABASE_VERSION = 7 com MIGRATION_6_7) e está diretamente ligada ao Backup .ZIP (Ideia 4) para portabilidade dos banners.
Impacto no Design System: Médio; cartões e detalhe de evento ganham proporção de imagem de capa sem estourar altura em listas.
Impacto técnico e arquitetural: Médio-Alto; envolve migração SQLite (v6 -> v7), processamento/redimensionamento JPEG local seguro e limpeza de arquivos órfãos ao excluir eventos.
Complexidade estimada: Média-Alta.
Riscos e possíveis conflitos: Vazamento de arquivos de imagem no filesDir ao excluir eventos ou restaurar backup; erro de migração Room em dados existentes se não houver MIGRATION_6_7 validada.
Critérios de aceitação que ainda precisam ser definidos: Dimensão/qualidade máxima da compressão JPEG, política de fallback quando imagem faltar e UX para exibir campos legados de taxa apenas quando já preenchidos.
Possibilidade de agrupamento com outras ideias: Altamente recomendável agrupar com Ideia 3 (compromissos na ficha do evento) e Ideia 4 (Backup .ZIP).
Prioridade sugerida: Alta (item 1 do roadmap histórico).
Ideia 3 — Listagem completa e criação direta de Compromissos vinculados no Evento (REQ-012 residual)
Nome e descrição: Exibir na ficha/detalhe do evento a lista completa dos compromissos vinculados (appointments.eventId) com ação direta de criar/abrir compromisso já pré-vinculado ao evento (hoje só há contador linkedAppointmentsCount).
Problema que pretende resolver: Obriga o usuário a sair de Eventos e ir até Agenda/QuickAdd para cadastrar ou consultar os horários ligados a um evento específico.
Benefício esperado para o usuário: Gestão unificada de um congresso/viagem/show diretamente na ficha do evento, mantendo fonte única na Agenda.
Telas e módulos afetados: EventsScreen, EventEditorSheet (ou detalhe do evento), MainActivity (ActiveEditor.AppointmentEditor), NexusRepository.
Dependências com funcionalidades existentes: Já possui coluna e FK appointments.eventId no Room v6 e suporte no AgendaEngine.
Impacto no Design System: Baixo; reutiliza padrão de linhas compactas já aplicado a tarefas e lançamentos vinculados.
Impacto técnico e arquitetural: Baixo; exige apenas propagar initialEventId em ActiveEditor.AppointmentEditor e expor lista no estado/ficha do evento.
Complexidade estimada: Baixa.
Riscos e possíveis conflitos: Risco de duplicar edição suja se o editor de evento abrir outro sheet antes de salvar — precisa do mesmo padrão de confirmação usado em "Registrar gasto".
Critérios de aceitação que ainda precisam ser definidos: Comportamento quando evento novo ainda não salvo tenta criar compromisso vinculado.
Possibilidade de agrupamento com outras ideias: Agrupar com Ideia 2 (Eventos Evoluídos).
Prioridade sugerida: Alta.
Ideia 4 — Backup Completo .ZIP (data.json + banners/, REQ-026)
Nome e descrição: Evoluir a exportação/importação SAF para gerar e restaurar arquivo .zip contendo data.json (payload v7+) e diretório banners/, mantendo compatibilidade retroativa com importação de .json puro (v1..v7).
Problema que pretende resolver: JSON puro não transporta arquivos binários locais (filesDir/event_banners/*.jpg), causando perda das imagens ao trocar de aparelho.
Benefício esperado para o usuário: Backup portátil verdadeiramente completo de registros e imagens locais.
Telas e módulos afetados: BackupManager, ZipBackupArchiver (novo), MoreSettingsBackupDiagnosticsScreens (BackupScreen).
Dependências com funcionalidades existentes: Depende da existência de banners locais da Ideia 2 (REQ-014) e do contrato atual de rollback coordenado Room ↔ Mercado (ADR-013).
Impacto no Design System: Baixo (rótulos, seleção de MIME application/zip + application/json e prévia).
Impacto técnico e arquitetural: Alto; exige proteção contra Zip Slip (validação de caminhos), restauração atômica de arquivos temporários em disco + rollback conjunto com Room e Mercado.
Complexidade estimada: Média-Alta.
Riscos e possíveis conflitos: Corrupção parcial de imagens durante falha pós-commit SQLite, limite de memória ao compactar/descompactar grandes imagens e segurança de nomes de entrada no ZIP.
Critérios de aceitação que ainda precisam ser definidos: Tratamento de conflito de nomes, extensão padrão exportada e comportamento caso um .zip não tenha banners/.
Possibilidade de agrupamento com outras ideias: Deve ser entregue junto com Ideia 2 (REQ-014).
Prioridade sugerida: Média-Alta (condicionada à Ideia 2).
Ideia 5 — Padronização do QuickAddChooser e colapso uniforme "Mais detalhes" nos editores (REQ-002 residual)
Nome e descrição: Revisar o QuickAddChooserSheet com descrições enxutas e padronizar nos demais editores (AppointmentEditor, EventEditor, TransactionEditor, ProjectEditor) o padrão de campos avançados recolhidos em "Mais detalhes", como já existe para recorrência no TaskEditorSheet.
Problema que pretende resolver: Formulários com muitos campos opcionais expostos de imediato aumentam rolagem e carga cognitiva em celulares.
Benefício esperado para o usuário: Cadastro rápido mais ágil sem perder profundidade quando necessário.
Telas e módulos afetados: EntityEditors.kt, FinanceEditors.kt, QuickAddChooserSheet.
Dependências com funcionalidades existentes: Nenhuma dependência estrutural; apenas UI dos modais/editores.
Impacto no Design System: Médio; consolida hierarquia padrão de formulários (campos essenciais visíveis + seção expansível "Mais detalhes").
Impacto técnico e arquitetural: Baixo; atenção apenas ao cálculo de activeEditorDirty ao recolher/expandir e à prevenção de cadeias longas remember(...) que causam OOM no compilador K2.
Complexidade estimada: Baixa-Média.
Riscos e possíveis conflitos: Ocultar campo que já possui dado preenchido ao abrir um registro existente para edição (regra deve autoexpandir ou sinalizar quando houver dados avançados preenchidos).
Critérios de aceitação que ainda precisam ser definidos: Quais campos exatamente são essenciais vs. "Mais detalhes" em cada um dos 5 editores.
Possibilidade de agrupamento com outras ideias: Pode acompanhar Ideia 1 ou revisões de Eventos (Ideias 2 e 3).
Prioridade sugerida: Média.
Ideia 6 — Recorrência de Compromissos e Eventos (REQ-009)
Nome e descrição: Estender o motor de recorrência (RecurrenceEngine + projeção na AgendaEngine + lembretes em janela de 30d) para AppointmentEntity e EventEntity, mantendo projeção virtual sem pré-gerar centenas de linhas futuras.
Problema que pretende resolver: Compromissos fixos (consultas semanais, aulas, reuniões) e eventos anuais/mensais ainda exigem cadastro manual repetido.
Benefício esperado para o usuário: Rotina recorrente completa na Agenda unificada com exceções pontuais ("Só esta" vs "Toda a série").
Telas e módulos afetados: AgendaEngine, RecurrenceEngine, NexusEntities, NexusDatabase, NexusMigrations, AppointmentEditorSheet, EventEditorSheet, NexusNotificationScheduler, BackupManager.
Dependências com funcionalidades existentes: Depende da consolidação do motor único AgendaEngine (Ideia 11 / INT-01) para que Briefing/NightReview/Dashboard enxerguem compromissos recorrentes igualmente.
Impacto no Design System: Médio; reutiliza controles do seletor "Repetir" e diálogos de escopo criados no Lote 06 de Tarefas.
Impacto técnico e arquitetural: Alto; exige generalização ou novas tabelas/colunas aditivas no Room, regras de conflito de horários em ocorrências virtuais, impacto financeiro de ingressos em eventos recorrentes e atualização do backup.
Complexidade estimada: Alta.
Riscos e possíveis conflitos: Eventos pagos recorrentes gerando obrigações financeiras ambíguas; conflitos de horário entre compromissos virtuais; explosão combinatória na janela mensal se não houver teto seguro.
Critérios de aceitação que ainda precisam ser definidos: Como funcionará pagamento/ingresso em eventos recorrentes (desabilitado ou por ocorrência) e até quantos dias futuros notificações serão agendadas.
Possibilidade de agrupamento com outras ideias: Depende primeiro da unificação INT-01 (Ideia 11).
Prioridade sugerida: Média-Baixa (pós-estabilização e Eventos).
Ideia 7 — Bloco configurável do Mercado na HomeScreen e seletor de mês no bloco de tarefas da Home
Nome e descrição: Duas evoluções opcionais de Dashboard registradas na documentação: (a) adicionar DashboardBlockType.MARKET opcional/configurável na HomeScreen resumindo ativos favoritados/principais sem duplicar cálculos; (b) adicionar seletor de mês no bloco de tarefas (TaskRingChart) na Home, hoje fixo no mês atual.
Problema que pretende resolver: Hoje o usuário precisa abrir SubRoute.Market para ver qualquer cotação e não consegue inspecionar o anel de tarefas de outro mês diretamente pela Home.
Benefício esperado para o usuário: Leitura rápida opcional de favoritos de mercado na Home e consulta rápida de tarefas por mês sem sair do painel.
Telas e módulos afetados: HomeScreen.kt, NexusViewModel.kt, NexusRepository.kt, DashboardCustomizationScreen, personalization_settings (se adicionar novo DashboardBlockType).
Dependências com funcionalidades existentes: Consome MarketQuoteRepository + MarketWatchlistRepository já prontos; DashboardBlockType é serializado/persistido no PersonalizationSettingsEntity / DashboardBlockConfig.
Impacto no Design System: Baixo; usa NexusCard, Sparkline e badges já existentes no Mercado.
Impacto técnico e arquitetural: Médio; adicionar novo enum em DashboardBlockType exige garantir migração segura de preferências existentes em personalization_settings e backups v1..v7 sem quebrar ordem salva pelo usuário.
Complexidade estimada: Média.
Riscos e possíveis conflitos: Poluir ou alongar a Home (contrariando o princípio de painel compacto) e exigir cuidado para que a Home nunca aguarde chamadas de rede do Mercado para renderizar.
Critérios de aceitação que ainda precisam ser definidos: Quantos ativos exibir (ex.: top 3 favoritos com fallback USD/EUR/BTC) e se o bloco vem desativado ou ativado por padrão.
Possibilidade de agrupamento com outras ideias: Pode ser avaliada junto com Ideia 8.
Prioridade sugerida: Baixa (somente se solicitada pelo usuário).
Ideia 8 — Expansão opcional do Mercado: alertas por variação percentual e novas classes de ativos
Nome e descrição: Estender o catálogo além dos 12 ativos atuais e suportar alertas baseados em variação percentual diária (ex.: ±3% no dia), além dos atuais alertas por preço-alvo (PRICE_ABOVE / PRICE_BELOW).
Problema que pretende resolver: Alertas atuais exigem preço fixo absoluto; variações bruscas exigem atualização frequente de alvos.
Benefício esperado para o usuário: Avisos automáticos sobre movimentos percentuais relevantes sem precisar recalibrar preço-alvo.
Telas e módulos afetados: MarketAlertModels, MarketAlertRepository, MarketAlertSheetsAndCards, MarketAssetCatalog, BackupModels/BackupManager.
Dependências com funcionalidades existentes: Depende dos provedores gratuitos retornarem changePercent confiável em todas as categorias (alguns fallbacks sem intraday podem não fornecer).
Impacto no Design System: Baixo; adiciona seletor de tipo no modal de criação de alerta.
Impacto técnico e arquitetural: Médio; exige janela/referência clara de reset diário para evitar múltiplos disparos na mesma oscilação e atualização do DTO de backup de alertas.
Complexidade estimada: Média.
Riscos e possíveis conflitos: Disparo falso por divergência de fechamento anterior entre provedor primário e fallback; expansão excessiva do catálogo pode elevar rate limit nas APIs gratuitas.
Critérios de aceitação que ainda precisam ser definidos: Fonte única da referência de fechamento anterior e critério de reset diário em fuso America/Sao_Paulo vs. bolsa de origem.
Possibilidade de agrupamento com outras ideias: Pode ser agrupada com Ideia 9 caso se altere a persistência de alertas.
Prioridade sugerida: Baixa (opcional pós-encerramento do Módulo Mercado).
Ideia 9 — Migração opcional de Watchlist e Alertas do Mercado para tabelas Room
Nome e descrição: Mover a persistência de favoritos (SharedPreferencesMarketWatchlistStore) e alertas (FileMarketAlertStore) para tabelas no NexusDatabase (ADR-010), eliminando a fronteira física dupla atual no Backup v7 (ADR-013).
Problema que pretende resolver: Hoje existe uma limitação física residual de commit distribuído contra SIGKILL exato entre a transação SQLite e .apply()/renameTo dos stores externos durante a restauração.
Benefício esperado para o usuário: Atomicidade física única em SQLite ao salvar, excluir ou restaurar backups.
Telas e módulos afetados: NexusDatabase, NexusMigrations, NexusEntities, NexusDaos, MarketWatchlistRepository, MarketAlertRepository, BackupManager.
Dependências com funcionalidades existentes: Exige incremento de DATABASE_VERSION e importação única dos dados legados de SharedPreferences/market_alerts.json dentro da migração ou inicialização.
Impacto no Design System: Zero impacto visual.
Impacto técnico e arquitetural: Alto; altera ADR-010 e ADR-013, requer migração aditiva Room e leitura única do store antigo para não perder favoritos/alertas de usuários atuais.
Complexidade estimada: Média-Alta.
Riscos e possíveis conflitos: Perda de favoritos/alertas existentes em dispositivos atualizados caso a transferência do store legado falhe na primeira execução pós-migração.
Critérios de aceitação que ainda precisam ser definidos: Estratégia exata de migração única de dados fora do SQLite (já que SupportSQLiteDatabase no Migration não acessa Context/filesDir).
Possibilidade de agrupamento com outras ideias: Ideal agrupar em um eventual incremento único de schema Room junto com Ideia 2 (DATABASE_VERSION = 7).
Prioridade sugerida: Baixa (apenas se a atomicidade física extrema se tornar requisito prioritário).
Ideia 10 — Correção do BUG-001: preservação de estado na rotação e recriação da Activity
Nome e descrição: Substituir remember volátil por rememberSaveable / SavedStateHandle para currentTab, subRoute, subRouteStack, activeEditor, filtros e rascunhos de formulários em editores, além de proteger MainActivity.onCreate contra reatribuição repetida de incomingIntentFlow ao recriar a Activity.
Problema que pretende resolver: Girar o dispositivo ou sofrer recriação de Activity descarta aba atual, subtela aberta, modal/editor e dados ainda não salvos digitados pelo usuário.
Benefício esperado para o usuário: Continuidade real ao girar celulares ou tablets (especialmente agora que os layouts adaptativos em paisagem foram implementados) sem perder digitação nem navegação.
Telas e módulos afetados: MainActivity.kt, NexusViewModel.kt, EntityEditors.kt, FinanceEditors.kt, MarketAlertSheetsAndCards.kt, telas com filtros/seleções locais (AgendaScreen, FinanceScreen, TasksAndProjectsScreen).
Dependências com funcionalidades existentes: Independente de banco de dados; interage diretamente com ActiveEditor e BackNavigationRules.
Impacto no Design System: Zero mudança estética; alto impacto na usabilidade real da adaptação de rotação.
Impacto técnico e arquitetural: Médio-Alto; SubRoute e ActiveEditor hoje contêm entidades não Parcelable (CreditCardEntity, LoanEntity, FinancialGoalEntity), exigindo Saver customizado, troca por IDs serializáveis ou @Parcelize.
Complexidade estimada: Média-Alta.
Riscos e possíveis conflitos: TransactionTooLargeException se colocar objetos grandes no Bundle (melhor salvar IDs e chaves leves + rascunho mínimo) e erro em deep-links se onCreate reprocessar o Intent antigo após rotação.
Critérios de aceitação que ainda precisam ser definidos: Lista exata de estados locais de UI que devem sobreviver à rotação vs. morte de processo (process death).
Possibilidade de agrupamento com outras ideias: Pode ser feito isoladamente (alta coesão) ou junto com Ideia 1 (core-splashscreen).
Prioridade sugerida: Crítica / Imediata (especialmente após a adoção de layouts responsivos em paisagem).
Ideia 11 — Consolidação arquitetural e dívidas técnicas (INT-01, INT-03, navigation-compose, modularização, gradlew/androidTest)
Nome e descrição: Conjunto de dívidas documentadas: (a) aposentar observeUnifiedAgenda() migrando Dashboard/Briefing/NightReview totalmente para AgendaEngine (INT-01); (b) não gravar onToggleSubtask direto no Room antes de clicar "Salvar" (INT-03 residual); (c) remover navigation-compose ociosa; (d) modularizar arquivos extensos (NexusRepository, EntityEditors, FinanceScreen, NexusViewModel); (e) incluir ./gradlew, versionar app/schemas/ e iniciar testes instrumentados androidTest.
Problema que pretende resolver: Reduzir risco de divergência entre Agenda e Dashboard/Rotina, evitar gravações silenciosas em edição cancelada, diminuir tempo de compilação K2 e melhorar confiabilidade de migrações Room.
Benefício esperado para o usuário: Maior consistência entre Home, Agenda e Briefings, comportamento previsível ao cancelar edição de tarefas e menos regressões entre versões.
Telas e módulos afetados: NexusRepository.kt, EntityEditors.kt, MainActivity.kt, FinanceScreen.kt, app/build.gradle.kts, gradle/libs.versions.toml, app/src/androidTest/.
Dependências com funcionalidades existentes: INT-01 é pré-requisito lógico para Ideia 6 (REQ-009 recorrência de compromissos/eventos).
Impacto no Design System: Zero impacto visual direto.
Impacto técnico e arquitetural: Médio-Alto (modularização e unificação de motor sem regressão em fluxos reativos).
Complexidade estimada: Média (INT-01/INT-03/navigation-compose são cirúrgicas; modularização e androidTest são graduais).
Riscos e possíveis conflitos: Regressão sutil em DailyBriefingSnapshot ou NightReviewSnapshot ao trocar observeUnifiedAgenda por AgendaEngine se algum caso de borda de data/status diferir.
Critérios de aceitação que ainda precisam ser definidos: Paridade comprovada por testes unitários entre saída antiga e AgendaEngine para Dashboard, Briefing e Revisão Noturna.
Possibilidade de agrupamento com outras ideias: INT-01 agrupa naturalmente como preparação para Ideia 6; INT-03 agrupa com Ideia 5 (editores); remoção de navigation-compose + ./gradlew podem acompanhar qualquer lote.
Prioridade sugerida: Alta (INT-01, INT-03 e ./gradlew/app/schemas/).
Ideia 12 — Tópicos não planejados / sob avaliação futura (10_ROADMAP.md §4.2: IA embarcada/OCR/LLM, Open Finance, multiusuário/nuvem, corretora/ordens)
Nome e descrição: Capacidades citadas na documentação apenas como não planejadas / não aprovadas atualmente: processamento por IA/OCR/LLM (ex.: leitura de comprovantes), integração bancária/Open Finance, múltiplos usuários com sincronização em nuvem e operações de corretora/recomendações de investimento.
Problema que pretende resolver: Automação de entrada de lançamentos ou sincronização multi-dispositivo (se um dia desejadas).
Benefício esperado para o usuário: Redução eventual de digitação manual ou sincronia contínua entre aparelhos.
Telas e módulos afetados: Transversal a todo o aplicativo (Financeiro, Mercado, Configurações, Rede e Segurança).
Dependências com funcionalidades existentes: Conflita estruturalmente com pilares e ADRs atuais (ADR-003 offline-first sem backend/login e escopo informativo do Mercado).
Impacto no Design System: Alto (fluxos de consentimento, estados de conta/sincronização, disclaimers legais).
Impacto técnico e arquitetural: Muito Alto (infraestrutura de nuvem, segurança, criptografia, LGPD/regulamentação financeira, consumo de bateria/RAM para IA local).
Complexidade estimada: Muito Alta.
Riscos e possíveis conflitos: Quebra do princípio offline-first e privacidade local, custos recorrentes de servidor/API, riscos regulatórios (Open Finance / CVM) e peso elevado de APK em IA embarcada.
Critérios de aceitação que ainda precisam ser definidos: Pesquisa prévia obrigatória conforme 13_GOVERNANCA_DOCUMENTAL.md e revisão explícita de ADRs antes de qualquer definição de escopo.
Possibilidade de agrupamento com outras ideias: Não agrupável com lotes incrementais atuais; exige fase própria de pesquisa/decisão caso o responsável mude o direcionamento do produto.
Prioridade sugerida: Fora de prioridade atual (não aprovada).
Organização final das ideias nos 5 agrupamentos obrigatórios
Melhorias independentes:
Ideia 1: Splash Screen Nativa (core-splashscreen).
Ideia 3: Compromissos vinculados completos na ficha do Evento (REQ-012 residual).
Ideia 5: Padronização do QuickAddChooser e "Mais detalhes" nos editores (REQ-002 residual).
Ideia 10: Correção do BUG-001 (preservação de estado na rotação/recriação).
Partes de dívida técnica da Ideia 11: INT-03 residual, remoção de navigation-compose, inclusão de ./gradlew / app/schemas/ / androidTest.
Melhorias que podem ser agrupadas:
Pacote A (Experiência de Shell & Ciclo de Vida): Ideia 10 (BUG-001) + Ideia 1 (core-splashscreen).
Pacote B (Eventos Completos + Portabilidade): Ideia 2 (REQ-014 Eventos Evoluídos) + Ideia 3 (REQ-012 Compromissos no Evento) + Ideia 4 (REQ-026 Backup .ZIP) — e opcionalmente Ideia 9 caso se decida aproveitar um único incremento para DATABASE_VERSION = 7.
Pacote C (Editores Enxutos): Ideia 5 (QuickAddChooser + "Mais detalhes") + INT-03 residual (Ideia 11).
Melhorias que dependem de outras implementações:
Ideia 4 (Backup .ZIP de banners) depende diretamente da Ideia 2 (events.bannerPath).
Ideia 6 (Recorrência de Compromissos e Eventos) depende da unificação INT-01 (Ideia 11) para não duplicar lógica entre AgendaEngine e observeUnifiedAgenda().
Melhorias que exigem pesquisa ou decisão adicional:
Ideia 7 (Bloco opcional de Mercado na Home e seletor de mês em tarefas na Home — decisão de prioridade/densidade da Home).
Ideia 8 (Alertas percentuais e novas classes de ativos no Mercado — pesquisa de confiabilidade de fontes públicas gratuitas).
Ideia 12 (IA/OCR, Open Finance, multiusuário/nuvem, corretora — pesquisa técnica, legal, de privacidade e revisão explícita de ADRs).
Melhorias que podem causar impactos arquiteturais maiores:
Ideia 6 (Recorrência genérica para Compromissos/Eventos — impacto em schema Room, motor temporal e notificações).
Ideia 9 (Migração de Watchlist/Alertas de Mercado para Room — altera ADR-010 e ADR-013 com migração de stores externos).
Ideia 11 parte estrutural (INT-01 + modularização de repositórios/viewmodels monolíticos).
Ideia 12 (Qualquer iniciativa de backend/nuvem, Open Finance ou IA embarcada).
Sequência recomendada de evolução (para avaliação do usuário)
Validação em CI / Dispositivo do estado atual (build Gradle, testDebugUnitTest e inspeção visual das mudanças recém-aplicadas).
Estabilização de rotação e shell: Ideia 10 (BUG-001) + Ideia 1 (core-splashscreen) + limpeza rápida (navigation-compose, ./gradlew, app/schemas/).
Unificação de base e editores: INT-01 + INT-03 (Ideia 11) + Ideia 5 (QuickAddChooser / "Mais detalhes").
Pacote Eventos Evoluídos + Backup .ZIP: Ideia 3 (REQ-012) + Ideia 2 (REQ-014) + Ideia 4 (REQ-026) (+ avaliação opcional da Ideia 9 no mesmo incremento de versão Room v7).
Expansão temporal: Ideia 6 (REQ-009 — Recorrência de Compromissos e Eventos), já apoiada sobre INT-01 resolvido.
Melhorias opcionais sob demanda: Ideias 7 e 8 (e eventual pesquisa isolada da Ideia 12 apenas se solicitada).
I. Pendências
BUG-001 (perda de estado na rotação / recriação de Activity): mantido em aberto fora deste lote conforme restrição explícita; layouts já se adaptam visualmente à rotação, mas MainActivity e formulários ainda não preservam estado entre recriações.
Compilação e testes automatizados: necessário executar gradle :app:testDebugUnitTest :app:assembleDebug via GitHub Actions ou máquina local com JDK 17 + Android SDK + Gradle 8.10.2.
Validação visual e funcional em dispositivo real/emulador (celular e tablet paisagem): confirmar visualmente Grafite Suave vs. Escuro, espessuras/contrastes das bordas coloridas, widgets Glance sem bordas no launcher, Agenda em 7 colunas vs. deslizável, 2 painéis em tablet, saudação da Home e ícone adaptativo N-1 no launcher Android.
Splash Screen Nativa (REQ-003 residual): androidx.core:core-splashscreen / Theme.NEXUS.Splash segue pendente de implementação futura.
Sincronização com repositório externo NEXUS-DOCUMENTACAO: não há acesso neste ambiente; caso utilize esse repositório em separado, será necessário replicar as atualizações documentais manualmente.
Decisões aguardando sua avaliação: validar este lote visual/adaptativo e decidir quais itens da análise da Parte 2 (ex.: BUG-001, core-splashscreen, Eventos Evoluídos + .ZIP) devem ser autorizados para o próximo lote.s avançados recolhidos em "Mais detalhes", como já existe para recorrência no TaskEditorSheet.
Problema que pretende resolver: Formulários com muitos campos opcionais expostos de imediato aumentam rolagem e carga cognitiva em celulares.
Benefício esperado para o usuário: Ca
