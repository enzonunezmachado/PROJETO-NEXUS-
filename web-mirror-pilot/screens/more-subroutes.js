/*
 * NEXUS Web Mirror Pilot — MoreScreen & SubRoutes
 * (GlobalSearch, DashboardCustomization, QuoteLibrary, Settings, Backup v7, Diagnostics 27 tables, WidgetsHub)
 * Fonte Android: MoreSettingsBackupDiagnosticsScreens.kt, DashboardCustomizationAndQuotesScreens.kt
 */
(function () {
  const {
    LABELS,
    formatBrl,
    formatDateBr,
    formatDateByPattern,
    computeProjectProgress,
    computeGoalDetails,
    computeEventDetails,
    computeAppointmentConflicts,
    computeFinanceSelectors,
    computeTaskDistribution,
    computeDailyBriefing,
    computeDatabaseTableCounts
  } = window.NexusPilot.Data;
  const {
    esc,
    ICONS,
    renderSectionHeader,
    renderTaskRowCard,
    renderTransactionRowCard,
    renderEventSummaryCard,
    renderAppointmentCompactCard,
    renderProjectCard
  } = window.NexusPilot.Primitives;
  const { renderPersonalGoalCard } = window.NexusPilot.Screens.Planning;

  function renderMoreScreen(state) {
    const items = [
      { icon: ICONS.flag, title: 'Planejamento e Objetivos', subtitle: 'Objetivos pessoais, marcos, histórico de evolução e vínculo com tarefas e projetos', action: 'data-action="open-subroute" data-subroute="PLANNING"', status: 'reproduzido' },
      { icon: ICONS.market, title: 'Mercado', subtitle: '12 ativos (moedas, cripto, índices, ações), gráficos históricos e alertas de preço', action: 'data-action="open-subroute" data-subroute="MARKET"', status: 'simulado' },
      { icon: ICONS.customize, title: 'Personalizar Dashboard', subtitle: 'Ativar, ocultar, reordenar blocos e configurar exibição da tela inicial', action: 'data-action="open-subroute" data-subroute="CUSTOMIZE"', status: 'reproduzido' },
      { icon: ICONS.quote, title: 'Biblioteca de Frases', subtitle: 'Frases locais do dia com autoria opcional, rotação offline e exportação', action: 'data-action="open-subroute" data-subroute="QUOTES"', status: 'reproduzido' },
      { icon: ICONS.bell, title: 'Assistente de Rotina e Notificações', subtitle: 'Briefing diário, revisão noturna, canais offline, silêncio e fila de lembretes', action: 'data-action="open-routine-assistant" data-routine-tab="0"', status: 'reproduzido' },
      { icon: ICONS.widgets, title: 'Widgets da Tela Inicial', subtitle: 'Prévia viva dos 4 widgets Glance (Meu Dia, Tarefas, Financeiro, Resumo)', action: 'data-action="open-subroute" data-subroute="WIDGETS"', status: 'simulado' },
      { icon: ICONS.search, title: 'Pesquisa global', subtitle: 'Buscar tarefas, objetivos, projetos, compromissos, eventos, transações e cartões', action: 'data-action="open-subroute" data-subroute="SEARCH"', status: 'reproduzido' },
      { icon: ICONS.settings, title: 'Configurações', subtitle: 'Aparência, saudação, formato de data, categorias e armazenamento local', action: 'data-action="open-subroute" data-subroute="SETTINGS"', status: 'reproduzido' },
      { icon: ICONS.backup, title: 'Backup e Restauração', subtitle: 'Exportar ou restaurar banco e configurações do Mercado (v7) em JSON', action: 'data-action="open-subroute" data-subroute="BACKUP"', status: 'parcial' },
      { icon: ICONS.bug, title: 'Diagnóstico', subtitle: 'Versão do banco (v18), contagem das 27 tabelas e logs locais', action: 'data-action="open-subroute" data-subroute="DIAGNOSTICS"', status: 'parcial' }
    ];

    return `
      <div class="nx-screen-scroll no-bottom-nav" data-nexus-component="MoreScreen" data-nexus-source="MoreSettingsBackupDiagnosticsScreens.kt:MoreScreen" data-nexus-status="reproduzido">
        ${items.map((it) => `
          <button type="button" class="nx-card is-elevated is-clickable" ${it.action}>
            <div style="display:flex;align-items:center;gap:12px;width:100%">
              <span class="nx-highlight" style="display:inline-flex;flex-shrink:0">${it.icon}</span>
              <div style="flex:1;min-width:0;text-align:left">
                <div style="display:flex;align-items:center;justify-content:space-between;gap:8px">
                  <span class="nx-title-md">${esc(it.title)}</span>
                  <span class="pilot-trace-tag ${it.status}">${esc(it.status)}</span>
                </div>
                <div class="nx-body-sm" style="margin-top:2px">${esc(it.subtitle)}</div>
              </div>
              <span class="nx-text-secondary" style="display:inline-flex">${ICONS.chevRight}</span>
            </div>
          </button>
        `).join('')}
      </div>
    `;
  }

  function renderGlobalSearchScreen(state, ui) {
    const raw = (ui.globalSearchQuery || '').trim();
    const q = raw.toLowerCase();
    const conflictMap = computeAppointmentConflicts(state.appointments);

    const taskHits = q ? state.tasks.filter((t) => t.title.toLowerCase().includes(q) || (t.description || '').toLowerCase().includes(q)) : [];
    const eventHits = q ? state.events.filter((e) => e.name.toLowerCase().includes(q) || (e.locationName || '').toLowerCase().includes(q) || (e.city || '').toLowerCase().includes(q)) : [];
    const txHits = q ? state.transactions.filter((t) => t.description.toLowerCase().includes(q) || (t.notes || '').toLowerCase().includes(q)) : [];
    const cardHits = q ? state.creditCards.filter((c) => c.name.toLowerCase().includes(q) || (c.institution || '').toLowerCase().includes(q)) : [];
    const projectHits = q ? state.projects.filter((p) => p.name.toLowerCase().includes(q) || (p.description || '').toLowerCase().includes(q)) : [];
    const goalHits = q ? state.personalGoals.filter((g) => g.title.toLowerCase().includes(q) || (g.description || '').toLowerCase().includes(q)) : [];
    const apptHits = q ? state.appointments.filter((a) => a.title.toLowerCase().includes(q) || (a.location || '').toLowerCase().includes(q) || (a.description || '').toLowerCase().includes(q)) : [];

    const totalHits = taskHits.length + eventHits.length + txHits.length + cardHits.length + projectHits.length + goalHits.length + apptHits.length;

    return `
      <div class="nx-screen-scroll no-bottom-nav" data-nexus-component="GlobalSearchScreen" data-nexus-source="MoreSettingsBackupDiagnosticsScreens.kt:GlobalSearchScreen" data-nexus-status="reproduzido">
        ${!raw ? `
          <div class="nx-card" style="align-items:center;text-align:center;padding:28px 18px">
            <span class="nx-accent">${ICONS.search}</span>
            <span class="nx-title-md">Pesquisa global</span>
            <span class="nx-body-sm">Pesquise em tarefas, objetivos pessoais, projetos, compromissos, eventos, lançamentos financeiros e cartões.</span>
            <div class="nx-chip-row" style="justify-content:center;margin-top:4px">
              ${['Kotlin', 'Ultravioleta', 'Aluguel', 'Monitor', 'NEXUS'].map((sug) => `
                <button type="button" class="nx-chip" data-action="apply-search-suggestion" data-query="${sug}">${sug}</button>
              `).join('')}
            </div>
          </div>
        ` : totalHits === 0 ? `
          <div class="nx-card" style="align-items:center;text-align:center;padding:28px 18px">
            <span class="nx-title-md">Nenhum resultado</span>
            <span class="nx-body-sm">Nenhum registro encontrado para "${esc(raw)}".</span>
          </div>
        ` : `
          ${taskHits.length ? `${renderSectionHeader({ title: 'Tarefas', trailing: String(taskHits.length) })}<div class="nx-grid-2">${taskHits.map((t) => renderTaskRowCard(t, state)).join('')}</div>` : ''}
          ${eventHits.length ? `${renderSectionHeader({ title: 'Eventos', trailing: String(eventHits.length) })}<div class="nx-grid-2">${eventHits.map((e) => renderEventSummaryCard(computeEventDetails(state, e))).join('')}</div>` : ''}
          ${txHits.length ? `${renderSectionHeader({ title: 'Lançamentos financeiros', trailing: String(txHits.length) })}<div style="display:flex;flex-direction:column;gap:8px">${txHits.map((tx) => renderTransactionRowCard(tx, state)).join('')}</div>` : ''}
          ${cardHits.length ? `${renderSectionHeader({ title: 'Cartões', trailing: String(cardHits.length) })}<div class="nx-grid-2">${cardHits.map((c) => `
            <button type="button" class="nx-card is-elevated is-clickable" data-action="open-finance-tab" data-finance-tab="CARDS">
              <div class="nx-row-between" style="width:100%">
                <span class="nx-title-md">${esc(c.name)}</span>
                <span class="nx-accent">${ICONS.card}</span>
              </div>
              <span class="nx-label-md nx-text-secondary">${esc(c.institution)} · Fecha dia ${c.closingDay} · Vence dia ${c.dueDay}</span>
            </button>
          `).join('')}</div>` : ''}
          ${projectHits.length ? `${renderSectionHeader({ title: 'Projetos', trailing: String(projectHits.length) })}<div class="nx-grid-2">${projectHits.map((p) => renderProjectCard(computeProjectProgress(state, p), state)).join('')}</div>` : ''}
          ${goalHits.length ? `${renderSectionHeader({ title: 'Objetivos pessoais', trailing: String(goalHits.length) })}<div class="nx-grid-2">${goalHits.map((g) => renderPersonalGoalCard(computeGoalDetails(state, g))).join('')}</div>` : ''}
          ${apptHits.length ? `${renderSectionHeader({ title: 'Compromissos', trailing: String(apptHits.length) })}<div class="nx-grid-2">${apptHits.map((a) => renderAppointmentCompactCard({
            ...a,
            categoryName: state.categories.find((c) => c.id === a.categoryId)?.name || 'Agenda',
            conflictTitles: conflictMap[a.id] || []
          }, true)).join('')}</div>` : ''}
        `}
      </div>
    `;
  }

  function renderDashboardCustomizationScreen(state, ui) {
    const sorted = [...state.blocks].sort((a, b) => a.order - b.order);
    const expandedType = ui.expandedCustomBlockType;

    return `
      <div class="nx-screen-scroll no-bottom-nav" data-nexus-component="DashboardCustomizationScreen" data-nexus-source="DashboardCustomizationAndQuotesScreens.kt:DashboardCustomizationScreen" data-nexus-status="reproduzido">
        <div class="nx-card">
          <div class="nx-title-md">Exibição geral do painel</div>
          <div class="nx-row-between">
            <div>
              <div class="nx-body-md">Modo compacto da Home</div>
              <div class="nx-label-sm nx-text-secondary">Reduz espaçamento vertical entre os blocos</div>
            </div>
            <button type="button" class="nx-switch ${state.personalization.compactDashboardMode ? 'is-on' : ''}" data-action="toggle-personalization" data-key="compactDashboardMode"></button>
          </div>
          <hr class="nx-divider"/>
          <div class="nx-row-between">
            <div>
              <div class="nx-body-md">Resumo de métricas no anel de tarefas</div>
              <div class="nx-label-sm nx-text-secondary">Exibe rodapé com tarefas atrasadas e sem prazo</div>
            </div>
            <button type="button" class="nx-switch ${state.personalization.showMetricSummaryOnDashboard ? 'is-on' : ''}" data-action="toggle-personalization" data-key="showMetricSummaryOnDashboard"></button>
          </div>
          <hr class="nx-divider"/>
          <div class="nx-row-between">
            <span class="nx-body-md">Exibir saudação (${esc(state.userName)})</span>
            <button type="button" class="nx-switch ${state.personalization.greetingEnabled ? 'is-on' : ''}" data-action="toggle-personalization" data-key="greetingEnabled"></button>
          </div>
          <div class="nx-row-between">
            <span class="nx-body-md">Exibir frase diária</span>
            <button type="button" class="nx-switch ${state.personalization.showDailyQuoteOnDashboard ? 'is-on' : ''}" data-action="toggle-personalization" data-key="showDailyQuoteOnDashboard"></button>
          </div>
        </div>

        <button type="button" class="nx-card is-elevated is-clickable" data-action="open-subroute" data-subroute="QUOTES">
          <div class="nx-row-between" style="width:100%">
            <div style="display:flex;align-items:center;gap:10px">
              <span class="nx-accent">${ICONS.quote}</span>
              <div style="text-align:left">
                <div class="nx-title-sm">Biblioteca de Frases</div>
                <div class="nx-label-sm nx-text-secondary">${state.quotes.filter((q) => q.isActive).length} ativa(s) · Gerenciar frases e seleção de hoje</div>
              </div>
            </div>
            ${ICONS.chevRight}
          </div>
        </button>

        ${renderSectionHeader({
          title: 'Blocos da tela inicial',
          trailing: `${sorted.filter((b) => b.enabled).length}/${sorted.length}`,
          actionLabel: 'Restaurar padrão',
          actionAttr: 'data-action="request-reset-dashboard"'
        })}

        ${sorted.map((b, idx) => {
          const isExp = expandedType === b.type;
          return `
            <div class="nx-card is-elevated" style="padding:12px">
              <div class="nx-row-between">
                <button type="button" class="nx-btn-text" style="padding:2px 4px;color:var(--nx-text-primary);font-weight:600;display:inline-flex;align-items:center;gap:6px" data-action="toggle-expand-custom-block" data-block-type="${esc(b.type)}">
                  <span>${idx + 1}. ${esc(b.title)}</span>
                  ${isExp ? ICONS.chevronUp : ICONS.chevronDown}
                </button>
                <div style="display:flex;align-items:center;gap:4px">
                  <button type="button" class="nx-btn-text" data-action="move-block" data-block-type="${esc(b.type)}" data-dir="-1" ${idx === 0 ? 'disabled style="opacity:0.35"' : ''}>↑</button>
                  <button type="button" class="nx-btn-text" data-action="move-block" data-block-type="${esc(b.type)}" data-dir="1" ${idx === sorted.length - 1 ? 'disabled style="opacity:0.35"' : ''}>↓</button>
                  <button type="button" class="nx-switch ${b.enabled ? 'is-on' : ''}" data-action="toggle-block-enabled" data-block-type="${esc(b.type)}"></button>
                </div>
              </div>
              ${isExp ? `
                <hr class="nx-divider"/>
                <div class="nx-row-between">
                  <span class="nx-label-md nx-text-secondary">Detalhe principal</span>
                  <button type="button" class="nx-switch ${b.showPrimaryDetail ? 'is-on' : ''}" data-action="toggle-block-prop" data-block-type="${esc(b.type)}" data-prop="showPrimaryDetail"></button>
                </div>
                <div class="nx-row-between">
                  <span class="nx-label-md nx-text-secondary">Alertas / atrasos</span>
                  <button type="button" class="nx-switch ${b.showOverdueOrAlerts ? 'is-on' : ''}" data-action="toggle-block-prop" data-block-type="${esc(b.type)}" data-prop="showOverdueOrAlerts"></button>
                </div>
                <div class="nx-row-between">
                  <span class="nx-label-md nx-text-secondary">Máximo de itens</span>
                  <div style="display:flex;align-items:center;gap:8px">
                    <button type="button" class="nx-btn-outlined" style="padding:3px 9px" data-action="step-block-max" data-block-type="${esc(b.type)}" data-delta="-1">-</button>
                    <span class="nx-title-sm tnum">${b.maxItems}</span>
                    <button type="button" class="nx-btn-outlined" style="padding:3px 9px" data-action="step-block-max" data-block-type="${esc(b.type)}" data-delta="1">+</button>
                  </div>
                </div>
              ` : ''}
            </div>
          `;
        }).join('')}
      </div>
    `;
  }

  function renderQuoteLibraryScreen(state, ui) {
    const q = (ui.quoteSearchQuery || '').trim().toLowerCase();
    const list = state.quotes.filter((qt) => !q || qt.text.toLowerCase().includes(q) || (qt.author || '').toLowerCase().includes(q));

    return `
      <div class="nx-screen-scroll no-bottom-nav" data-nexus-component="QuoteLibraryScreen" data-nexus-source="DashboardCustomizationAndQuotesScreens.kt:QuoteLibraryScreen" data-nexus-status="reproduzido">
        <div class="nx-card">
          <div class="nx-row-between">
            <span class="nx-title-sm">Seleção de hoje na Home</span>
            <button type="button" class="nx-btn-text" data-action="rotate-daily-quote">Sortear frase ativa</button>
          </div>
          <p class="nx-body-md" style="font-style:italic">“${esc(state.dailyQuote.text)}”</p>
          ${state.dailyQuote.author ? `<span class="nx-label-sm nx-text-secondary">— ${esc(state.dailyQuote.author)}</span>` : ''}
        </div>

        <div class="nx-row-between">
          <input id="nx-quote-search" class="nx-input" type="search" placeholder="Buscar frase ou autor..." value="${esc(ui.quoteSearchQuery || '')}" data-input="quote-search" style="flex:1" />
          <button type="button" class="nx-btn-outlined" data-action="new-quote">+ Frase</button>
        </div>

        ${list.map((qt) => `
          <div class="nx-card is-elevated" data-nexus-entity="quote:${esc(qt.id)}">
            <p class="nx-body-md">“${esc(qt.text)}”</p>
            <div class="nx-row-between">
              <span class="nx-label-sm nx-text-secondary">${qt.author ? `— ${esc(qt.author)}` : 'Sem autoria'}</span>
              <span class="nx-badge ${qt.isActive ? 'is-positive' : 'is-neutral'}">${qt.isActive ? 'Ativa' : 'Inativa'}</span>
            </div>
            <div class="nx-row-between">
              <div style="display:flex;gap:6px">
                <button type="button" class="nx-btn-text" data-action="use-quote-today" data-quote-id="${esc(qt.id)}">Usar hoje</button>
                <button type="button" class="nx-btn-text" data-action="edit-quote" data-quote-id="${esc(qt.id)}">Editar</button>
                <button type="button" class="nx-btn-text nx-btn-danger-text" data-action="delete-quote" data-quote-id="${esc(qt.id)}">Excluir</button>
              </div>
              <button type="button" class="nx-switch ${qt.isActive ? 'is-on' : ''}" data-action="toggle-quote-active" data-quote-id="${esc(qt.id)}"></button>
            </div>
          </div>
        `).join('')}
      </div>
    `;
  }

  function renderSettingsScreen(state, ui) {
    return `
      <div class="nx-screen-scroll no-bottom-nav" data-nexus-component="SettingsScreen" data-nexus-source="MoreSettingsBackupDiagnosticsScreens.kt:SettingsScreen" data-nexus-status="reproduzido">
        <div class="nx-card">
          <div class="nx-title-md">Perfil e Aparência</div>
          <div class="nx-field">
            <label class="nx-field-label" for="nx-settings-username">Nome exibido na saudação</label>
            <input id="nx-settings-username" class="nx-input" type="text" value="${esc(state.userName)}" data-input="settings-username" />
          </div>
          <div class="nx-row-between">
            <div>
              <div class="nx-body-md">Tema escuro (OLED / Grafite Suave no claro)</div>
              <div class="nx-label-sm nx-text-secondary">Sincronizado com o controle de tema do laboratório</div>
            </div>
            <button type="button" class="nx-switch ${ui.theme === 'dark' ? 'is-on' : ''}" data-action="toggle-settings-theme"></button>
          </div>
          <div class="nx-row-between">
            <span class="nx-body-md">Modo compacto da Home</span>
            <button type="button" class="nx-switch ${state.personalization.compactDashboardMode ? 'is-on' : ''}" data-action="toggle-personalization" data-key="compactDashboardMode"></button>
          </div>
        </div>

        <div class="nx-card">
          <div class="nx-title-md">Formato de data na Home</div>
          <div style="display:flex;flex-direction:column;gap:6px">
            ${Object.entries(LABELS.NexusDateFormat).map(([k, label]) => {
              const isSel = state.settings.dateFormatPattern === k;
              return `
                <button type="button" class="nx-card is-elevated is-clickable ${isSel ? 'is-focused-highlight' : ''}" style="padding:10px 12px" data-action="set-date-format" data-format="${k}">
                  <div class="nx-row-between" style="width:100%">
                    <span class="nx-body-md">${esc(label)}</span>
                    <span class="nx-label-sm nx-text-secondary tnum">${esc(formatDateByPattern(state.todayIso, k))}</span>
                  </div>
                </button>
              `;
            }).join('')}
          </div>
        </div>

        <div class="nx-card">
          <div class="nx-title-md">Atalhos rápidos</div>
          <div class="nx-grid-2">
            <button type="button" class="nx-btn-outlined" data-action="open-subroute" data-subroute="CUSTOMIZE">Personalizar Dashboard</button>
            <button type="button" class="nx-btn-outlined" data-action="open-subroute" data-subroute="QUOTES">Biblioteca de Frases</button>
            <button type="button" class="nx-btn-outlined" data-action="open-balance-dialog">Atualizar saldo informado</button>
            <button type="button" class="nx-btn-outlined" data-action="open-subroute" data-subroute="BACKUP">Backup e Restauração</button>
          </div>
        </div>

        <div style="display:flex;flex-direction:column;gap:8px">
          ${renderSectionHeader({
            title: 'Categorias',
            trailing: String(state.categories.length),
            actionLabel: '+ Nova categoria',
            actionAttr: 'data-action="new-category"'
          })}
          <div class="nx-grid-2">
            ${state.categories.map((c) => `
              <div class="nx-card is-elevated" style="padding:12px">
                <div class="nx-row-between">
                  <div style="display:flex;align-items:center;gap:8px;min-width:0">
                    <span class="nx-dot" style="background:${esc(c.colorHex)};width:10px;height:10px"></span>
                    <div style="min-width:0">
                      <div class="nx-title-sm">${esc(c.name)}</div>
                      <div class="nx-label-sm nx-text-secondary">${esc(LABELS.CategoryScope[c.scope] || c.scope)}</div>
                    </div>
                  </div>
                  <div style="display:flex;gap:4px">
                    <button type="button" class="nx-icon-btn" style="width:32px;height:32px" data-action="edit-category" data-category-id="${esc(c.id)}" aria-label="Editar categoria">${ICONS.edit}</button>
                    <button type="button" class="nx-icon-btn nx-danger" style="width:32px;height:32px" data-action="delete-category" data-category-id="${esc(c.id)}" aria-label="Excluir categoria">${ICONS.trash}</button>
                  </div>
                </div>
              </div>
            `).join('')}
          </div>
        </div>
      </div>
    `;
  }

  function buildBackupV7Payload(state) {
    return {
      formatVersion: 7,
      appVersion: '1.7.0-web-mirror',
      exportedAt: `${state.todayIso}T14:40:00`,
      settings: state.settings,
      personalizationSettings: state.personalization,
      dashboardBlocks: state.blocks,
      quotes: state.quotes,
      categories: state.categories,
      personalGoals: state.personalGoals,
      projects: state.projects,
      events: state.events,
      tasks: state.tasks,
      appointments: state.appointments,
      creditCards: state.creditCards,
      statements: state.statements,
      installmentPurchases: state.installmentPurchases,
      recurringRules: state.recurringRules,
      transactions: state.transactions,
      loans: state.loans,
      budgets: state.budgets,
      financialGoals: state.financialGoals,
      reminders: state.reminders,
      routineSettings: state.routineSettings,
      briefingHistory: state.briefingState.history,
      auditHistory: state.auditHistory,
      errorLogs: state.errorLogs,
      marketConfig: {
        favorites: state.marketFavorites,
        alerts: state.marketAlerts
      }
    };
  }

  function renderBackupScreen(state, ui) {
    const payload = buildBackupV7Payload(state);
    const jsonText = JSON.stringify(payload, null, 2);

    return `
      <div class="nx-screen-scroll no-bottom-nav" data-nexus-component="BackupScreen" data-nexus-source="MoreSettingsBackupDiagnosticsScreens.kt:BackupScreen" data-nexus-status="parcial">
        <div class="nx-card">
          <div class="nx-row-between">
            <span class="nx-title-md">Exportar backup local (JSON v7)</span>
            <span class="pilot-trace-tag parcial">Sem SAF nativo</span>
          </div>
          <p class="nx-body-sm">Gera um snapshot completo compatível com o esquema v7 do NEXUS (27 entidades + favoritos e alertas do Mercado).</p>
          <div class="nx-row-2col">
            <button type="button" class="nx-btn-primary" data-action="download-backup-json">Baixar backup .json</button>
            <button type="button" class="nx-btn-outlined" data-action="toggle-backup-json-preview">${ui.backupJsonExpanded ? 'Ocultar JSON' : 'Inspecionar JSON'}</button>
          </div>
          ${ui.backupJsonExpanded ? `<pre class="nx-code-block">${esc(jsonText.slice(0, 2400))}${jsonText.length > 2400 ? '\n... (truncado na prévia)' : ''}</pre>` : ''}
        </div>

        <div class="nx-card is-elevated">
          <div class="nx-title-md">Importar / Restaurar backup</div>
          <p class="nx-body-sm">Selecione um arquivo .json exportado anteriormente ou cole o conteúdo JSON abaixo para inspecionar e restaurar em memória.</p>
          <input type="file" accept=".json,application/json" class="nx-input" data-action="upload-backup-file" />
          <div class="nx-field">
            <label class="nx-field-label" for="nx-backup-import-text">Ou cole o JSON de backup</label>
            <textarea id="nx-backup-import-text" class="nx-textarea" rows="4" placeholder='{"formatVersion":7,...}' data-input="backup-import-text">${esc(ui.backupImportText || '')}</textarea>
          </div>
          <button type="button" class="nx-btn-outlined" data-action="inspect-backup-import">Validar e pré-visualizar restauração</button>
        </div>

        ${ui.backupLastSummary ? `
          <div class="nx-card is-elevated">
            <span class="nx-title-sm nx-positive">Resultado da última restauração</span>
            <span class="nx-body-sm">${esc(ui.backupLastSummary)}</span>
          </div>
        ` : ''}
      </div>
    `;
  }

  function renderDiagnosticsScreen(state) {
    const counts = computeDatabaseTableCounts(state);
    return `
      <div class="nx-screen-scroll no-bottom-nav" data-nexus-component="DiagnosticsScreen" data-nexus-source="MoreSettingsBackupDiagnosticsScreens.kt:DiagnosticsScreen" data-nexus-status="parcial">
        <div class="nx-card is-elevated">
          <div class="nx-row-between">
            <span class="nx-title-md">Versões</span>
            <span class="pilot-trace-tag parcial">Store em memória (Room v18 no Android)</span>
          </div>
          <div class="nx-body-sm tnum">App: 1.7.0 (Espelho Web) · Esquema SQLite Room: versão 18 · Backup v7</div>
        </div>

        <div class="nx-card is-elevated">
          <div class="nx-title-md">Registros por tabela (${counts.length} tabelas)</div>
          <div style="display:flex;flex-direction:column;gap:4px;margin-top:4px">
            ${counts.map((row) => `
              <div class="nx-row-between" style="font-family:var(--nx-font-mono);font-size:12px">
                <span class="nx-text-secondary">${esc(row.table)}</span>
                <span class="nx-text-primary tnum" style="font-weight:600">${row.count}</span>
              </div>
            `).join('')}
          </div>
        </div>

        <div class="nx-row-between">
          <span class="nx-title-md">Últimos ${state.errorLogs.length} registros de erro</span>
          <div style="display:flex;gap:6px">
            ${state.errorLogs.length ? `<button type="button" class="nx-btn-text nx-btn-danger-text" data-action="clear-error-logs">Limpar</button>` : ''}
            <button type="button" class="nx-btn-outlined" data-action="copy-diagnostics">${ICONS.copy} <span>Copiar relatório</span></button>
          </div>
        </div>

        ${state.errorLogs.length ? state.errorLogs.map((err) => `
          <div class="nx-card is-elevated">
            <div class="nx-label-md nx-danger tnum">${esc(err.timestamp)} · ${esc(err.origin)}</div>
            <div class="nx-body-md">${esc(err.message)}</div>
          </div>
        `).join('') : `
          <div class="nx-card"><span class="nx-body-sm">Nenhum erro capturado até o momento.</span></div>
        `}
      </div>
    `;
  }

  function renderWidgetsHubScreen(state) {
    const fin = computeFinanceSelectors(state, '2026-11');
    const briefing = computeDailyBriefing(state);
    const taskDist = computeTaskDistribution(state);
    const pendingTasks = [...taskDist.overdueTasks, ...taskDist.priorityTasksToday].slice(0, 3);

    const specs = [
      {
        title: 'NEXUS — Meu Dia',
        desc: 'Data de hoje, compromissos, tarefas com prazo hoje e atalhos diretos.',
        body: `
          <div class="nx-label-sm nx-accent tnum">${esc(formatDateByPattern(state.todayIso, state.settings.dateFormatPattern))}</div>
          <div class="nx-body-sm tnum">${briefing.todayAppointments.length} compromisso(s) · ${briefing.todayTasks.length + briefing.overdueTasks.length} tarefa(s) · ${briefing.todayEvents.length} evento(s)</div>
        `
      },
      {
        title: 'NEXUS — Minhas Tarefas',
        desc: 'Lista priorizada por urgência e prazo com conclusão em 2 etapas no widget.',
        body: pendingTasks.map((t) => `<div class="nx-label-md" style="white-space:nowrap;overflow:hidden;text-overflow:ellipsis">• ${esc(t.title)}</div>`).join('') || '<span class="nx-label-sm">Nenhuma pendência prioritária.</span>'
      },
      {
        title: 'NEXUS — Financeiro',
        desc: 'Saldo informado, receitas/despesas realizadas e contas vencidas.',
        body: `
          <div class="nx-title-md tnum">${esc(formatBrl(state.declaredBalanceCents))}</div>
          <div class="nx-label-sm nx-text-secondary tnum">+${esc(formatBrl(fin.receivedIncomeCents))} · -${esc(formatBrl(fin.paidExpenseCents))} · ${fin.overdueCount} atrasada(s)</div>
        `
      },
      {
        title: 'NEXUS — Resumo NEXUS',
        desc: 'Painel consolidado de tarefas, compromissos, eventos e metas.',
        body: `<div class="nx-label-sm nx-text-secondary tnum">${state.tasks.filter((t) => t.status === 'COMPLETED').length}/${state.tasks.length} tarefas concluídas · ${state.personalGoals.length} objetivo(s) ativo(s)</div>`
      }
    ];

    return `
      <div class="nx-screen-scroll no-bottom-nav" data-nexus-component="WidgetsHubScreen" data-nexus-source="MoreSettingsBackupDiagnosticsScreens.kt:WidgetsHubScreen" data-nexus-status="simulado">
        <div class="nx-card is-elevated">
          <div class="nx-row-between">
            <span class="nx-title-md">Widgets Nativos (Jetpack Glance)</span>
            <span class="pilot-trace-tag simulado">Simulado na Web</span>
          </div>
          <p class="nx-body-sm">No Android, os 4 widgets consultam o Room diretamente. Abaixo está a prévia fiel com os dados atuais de Gabriel.</p>
          <button type="button" class="nx-btn-primary" data-action="sync-widgets-sim">
            ${ICONS.refresh} <span>Sincronizar widgets agora</span>
          </button>
        </div>

        <div class="nx-grid-2">
          ${specs.map((sp) => `
            <div class="nx-widget-preview">
              <div class="nx-row-between">
                <span class="nx-title-sm">${esc(sp.title)}</span>
                <span class="nx-badge is-neutral">Glance</span>
              </div>
              <span class="nx-label-sm nx-text-secondary">${esc(sp.desc)}</span>
              <hr class="nx-divider"/>
              ${sp.body}
              <button type="button" class="nx-btn-outlined" style="margin-top:4px" data-action="pin-widget-sim" data-widget-title="${esc(sp.title)}">
                ${ICONS.widgets} <span>Fixar na tela inicial (Simulado)</span>
              </button>
            </div>
          `).join('')}
        </div>
      </div>
    `;
  }

  window.NexusPilot = window.NexusPilot || {};
  window.NexusPilot.Screens = window.NexusPilot.Screens || {};
  window.NexusPilot.Screens.MoreSubroutes = {
    renderMoreScreen,
    renderGlobalSearchScreen,
    renderDashboardCustomizationScreen,
    renderQuoteLibraryScreen,
    renderSettingsScreen,
    renderBackupScreen,
    renderDiagnosticsScreen,
    renderWidgetsHubScreen,
    buildBackupV7Payload
  };
})();
