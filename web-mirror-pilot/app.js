/*
 * NEXUS Web Mirror Pilot — Unidirectional State Engine, Navigation & Pilot Lab Controller
 * Fonte Android: MainActivity.kt (BOTTOM_NAV_TAB_ORDER, SubRoute, QuickAddChooser, Navigation)
 */
(function () {
  const {
    createInitialState,
    LABELS,
    formatBrl,
    shiftMonthIso,
    computeFinanceSelectors
  } = window.NexusPilot.Data;
  const { esc, ICONS, renderWordmarkSvg } = window.NexusPilot.Primitives;
  const { Home, RoutineAssistant, Finance, Events } = window.NexusPilot.Screens;
  const { TransactionSheet, EventSheet } = window.NexusPilot.Sheets;

  // Ordem exata de MainActivity.kt: BOTTOM_NAV_TAB_ORDER = [TASKS, AGENDA, HOME, FINANCE, EVENTS]
  const BOTTOM_TABS = [
    { key: 'TASKS', label: 'Tarefas', icon: ICONS.tasks, status: 'parcial' },
    { key: 'AGENDA', label: 'Agenda', icon: ICONS.agenda, status: 'parcial' },
    { key: 'HOME', label: 'Início', icon: ICONS.home, status: 'reproduzido' },
    { key: 'FINANCE', label: 'Finanças', icon: ICONS.finance, status: 'reproduzido' },
    { key: 'EVENTS', label: 'Eventos', icon: ICONS.events, status: 'reproduzido' }
  ];

  const app = {
    state: createInitialState(),
    ui: {
      theme: 'dark',
      viewport: 'mobile', // 'mobile' (390px) | 'tablet' (900px) | 'fluid'
      traceOpen: false,
      activeTab: 'HOME',
      subRoute: null, // null | 'ROUTINE_ASSISTANT' | 'MORE' | 'SEARCH'
      routineTabIndex: 0,
      financeTab: 'OVERVIEW',
      financeCategoryScope: 'PAID_AND_PLANNED',
      financeCashflowMode: 'REALIZED',
      financeDonutSliceId: null,
      financeBarMonthId: '2026-11',
      homeDonutSliceId: null,
      homeBarMonthId: '2026-11',
      highlightedStatementId: null,
      eventsFilter: 'UPCOMING',
      globalSearchQuery: '',
      financeFilter: {
        monthScope: 'MONTH',
        type: null,
        status: null,
        categoryId: null,
        paymentMethod: null
      },
      // Overlays
      quickAddOpen: false,
      customizeOpen: false,
      balanceDialogOpen: false,
      balanceDraftText: '4825,00',
      financeFilterSheetOpen: false,
      csvDialogOpen: false,
      txDraft: null,
      txDirty: false,
      eventDraft: null,
      eventDirty: false,
      confirmDialog: null, // { type, title, message, payload }
      snackbar: null // { message }
    }
  };

  let snackbarTimer = null;

  function showSnackbar(message) {
    app.ui.snackbar = { message };
    render();
    clearTimeout(snackbarTimer);
    snackbarTimer = setTimeout(() => {
      app.ui.snackbar = null;
      render();
    }, 3400);
  }

  function renderTopBar() {
    if (app.ui.subRoute === 'ROUTINE_ASSISTANT') {
      return `
        <header class="nx-subroute-bar" data-nexus-component="SubRouteTopBar" data-nexus-source="RoutineAssistantScreen.kt" data-nexus-status="reproduzido">
          <button type="button" class="nx-icon-btn" data-action="pop-subroute" aria-label="Voltar">${ICONS.back}</button>
          <h1 class="nx-subroute-title">Assistente de Rotina</h1>
        </header>
      `;
    }
    if (app.ui.subRoute === 'MORE') {
      return `
        <header class="nx-subroute-bar" data-nexus-component="SubRouteTopBar" data-nexus-source="MoreScreen.kt" data-nexus-status="parcial">
          <button type="button" class="nx-icon-btn" data-action="pop-subroute" aria-label="Voltar">${ICONS.back}</button>
          <h1 class="nx-subroute-title">Mais</h1>
        </header>
      `;
    }
    if (app.ui.subRoute === 'SEARCH') {
      return `
        <header class="nx-subroute-bar" data-nexus-component="SubRouteTopBar" data-nexus-source="GlobalSearchScreen.kt" data-nexus-status="parcial">
          <button type="button" class="nx-icon-btn" data-action="pop-subroute" aria-label="Voltar">${ICONS.back}</button>
          <h1 class="nx-subroute-title">Busca global</h1>
        </header>
      `;
    }

    return `
      <header class="nx-topbar" data-nexus-component="MainTopBar" data-nexus-source="MainActivity.kt:477-522" data-nexus-status="reproduzido">
        <div class="nx-topbar-inner">
          <div class="nx-wordmark-wrap" data-nexus-component="NexusWordmark" data-nexus-source="NexusComponents.kt:buildNexusWordmarkPath" data-nexus-status="reproduzido">
            ${renderWordmarkSvg(17)}
          </div>
          <div class="nx-topbar-actions">
            <button type="button" class="nx-icon-btn" data-action="open-routine-assistant" data-routine-tab="0" aria-label="Briefing Diário" title="Assistente de Rotina / Briefing">
              ${ICONS.bell}
            </button>
            <button type="button" class="nx-icon-btn" data-action="open-market-notice" aria-label="Mercado" title="Mercado">
              ${ICONS.market}
            </button>
            <button type="button" class="nx-icon-btn" data-action="open-global-search" aria-label="Busca global" title="Busca global">
              ${ICONS.search}
            </button>
            <button type="button" class="nx-icon-btn" data-action="open-more-menu" aria-label="Mais opções" title="Mais">
              ${ICONS.more}
            </button>
          </div>
        </div>
      </header>
    `;
  }

  function renderMainContent() {
    if (app.ui.subRoute === 'ROUTINE_ASSISTANT') {
      return RoutineAssistant.renderRoutineAssistantScreen(app.state, app.ui);
    }
    if (app.ui.subRoute === 'MORE') {
      return `<div class="nx-screen-scroll no-bottom-nav">${Home.renderMoreSubRoute(app.state)}</div>`;
    }
    if (app.ui.subRoute === 'SEARCH') {
      return `<div class="nx-screen-scroll no-bottom-nav">${Home.renderGlobalSearchSubRoute(app.state, app.ui.globalSearchQuery)}</div>`;
    }

    if (app.ui.activeTab === 'HOME') {
      return `<main class="nx-screen-scroll" data-nexus-screen="HomeScreen">${Home.renderHomeScreen(app.state, app.ui)}</main>`;
    }
    if (app.ui.activeTab === 'FINANCE') {
      return `<main style="display:flex;flex-direction:column;flex:1;min-height:0" data-nexus-screen="FinanceScreen">${Finance.renderFinanceScreen(app.state, app.ui)}</main>`;
    }
    if (app.ui.activeTab === 'EVENTS') {
      return `<main style="display:flex;flex-direction:column;flex:1;min-height:0" data-nexus-screen="EventsScreen">${Events.renderEventsScreen(app.state, app.ui)}</main>`;
    }
    if (app.ui.activeTab === 'TASKS') {
      return `<main class="nx-screen-scroll" data-nexus-screen="TasksBoundary">${Home.renderTasksBoundaryPreview(app.state)}</main>`;
    }
    return `<main class="nx-screen-scroll" data-nexus-screen="AgendaBoundary">${Home.renderAgendaBoundaryPreview(app.state)}</main>`;
  }

  function renderBottomBarAndFab() {
    if (app.ui.subRoute !== null) return '';
    return `
      <button type="button" class="nx-fab" data-action="open-quick-add" aria-label="Adicionar" data-nexus-component="QuickAddFab" data-nexus-source="MainActivity.kt:525-535" data-nexus-status="reproduzido">
        ${ICONS.add}
      </button>
      <nav class="nx-bottom-nav" aria-label="Navegação principal" data-nexus-component="BottomNavigationBar" data-nexus-source="MainActivity.kt:537-565" data-nexus-status="reproduzido">
        ${BOTTOM_TABS.map((tab) => {
          const isSel = app.ui.activeTab === tab.key;
          return `
            <button type="button" class="nx-nav-item ${isSel ? 'is-selected' : ''}" data-action="select-bottom-tab" data-tab="${tab.key}" aria-current="${isSel ? 'page' : 'false'}">
              <span class="nx-nav-pill">${tab.icon}</span>
              <span class="nx-nav-label">${esc(tab.label)}</span>
            </button>
          `;
        }).join('')}
      </nav>
    `;
  }

  function renderAuxiliaryOverlays() {
    const parts = [];

    if (app.ui.quickAddOpen) {
      parts.push(`
        <div class="nx-sheet-backdrop" data-action="close-overlays">
          <div class="nx-sheet" role="dialog" aria-modal="true" data-nexus-component="QuickAddChooserSheet" data-nexus-source="MainActivity.kt:QuickAddChooser" data-nexus-status="parcial" onclick="event.stopPropagation()">
            <div class="nx-sheet-handle"></div>
            <div class="nx-sheet-body">
              <div class="nx-row-between">
                <h3 class="nx-title-lg">Criar novo registro</h3>
                <button type="button" class="nx-btn-text" data-action="close-overlays">Fechar</button>
              </div>
              <button type="button" class="nx-card is-elevated is-clickable" data-action="quick-new-tx" data-tx-type="EXPENSE">
                <span class="nx-title-md">Nova despesa</span>
                <span class="nx-label-md nx-text-secondary">Abre TransactionEditorSheet (com vínculo opcional a cartão e evento)</span>
              </button>
              <button type="button" class="nx-card is-elevated is-clickable" data-action="quick-new-tx" data-tx-type="INCOME">
                <span class="nx-title-md">Nova receita</span>
                <span class="nx-label-md nx-text-secondary">Lançamento financeiro de entrada</span>
              </button>
              <button type="button" class="nx-card is-elevated is-clickable" data-action="quick-new-event">
                <span class="nx-title-md">Novo evento</span>
                <span class="nx-label-md nx-text-secondary">Abre EventEditorSheet com controle de ingresso e custos</span>
              </button>
              <button type="button" class="nx-card is-elevated is-clickable" data-action="open-routine-assistant" data-routine-tab="0">
                <span class="nx-title-md">Abrir Briefing Diário</span>
                <span class="nx-label-md nx-text-secondary">Assistente de Rotina · Resumo do dia</span>
              </button>
            </div>
          </div>
        </div>
      `);
    }

    if (app.ui.customizeOpen) {
      const sortedBlocks = [...app.state.blocks].sort((a, b) => a.order - b.order);
      parts.push(`
        <div class="nx-sheet-backdrop" data-action="close-overlays">
          <div class="nx-sheet" role="dialog" aria-modal="true" data-nexus-component="DashboardCustomizationSheet" data-nexus-source="DashboardCustomizationScreen.kt" data-nexus-status="reproduzido" onclick="event.stopPropagation()">
            <div class="nx-sheet-handle"></div>
            <div class="nx-sheet-body">
              <div class="nx-row-between">
                <h3 class="nx-title-lg">Personalizar painel da Home</h3>
                <button type="button" class="nx-btn-text" data-action="close-overlays">Concluir</button>
              </div>
              <div class="nx-row-between">
                <span class="nx-body-md">Exibir saudação com nome (${esc(app.state.userName)})</span>
                <button type="button" class="nx-switch ${app.state.personalization.greetingEnabled ? 'is-on' : ''}" data-action="toggle-personalization" data-key="greetingEnabled"></button>
              </div>
              <div class="nx-row-between">
                <span class="nx-body-md">Exibir frase diária</span>
                <button type="button" class="nx-switch ${app.state.personalization.showDailyQuoteOnDashboard ? 'is-on' : ''}" data-action="toggle-personalization" data-key="showDailyQuoteOnDashboard"></button>
              </div>
              <hr class="nx-divider"/>
              <div class="nx-title-sm">Ordem e visibilidade dos blocos</div>
              ${sortedBlocks.map((b, idx) => `
                <div class="nx-card is-elevated" style="padding:10px 12px">
                  <div class="nx-row-between">
                    <span class="nx-body-md" style="font-weight:500">${idx + 1}. ${esc(b.title)}</span>
                    <div style="display:flex;align-items:center;gap:6px">
                      <button type="button" class="nx-btn-text" data-action="move-block" data-block-type="${esc(b.type)}" data-dir="-1" ${idx === 0 ? 'disabled style="opacity:0.35"' : ''}>↑</button>
                      <button type="button" class="nx-btn-text" data-action="move-block" data-block-type="${esc(b.type)}" data-dir="1" ${idx === sortedBlocks.length - 1 ? 'disabled style="opacity:0.35"' : ''}>↓</button>
                      <button type="button" class="nx-switch ${b.enabled ? 'is-on' : ''}" data-action="toggle-block-enabled" data-block-type="${esc(b.type)}"></button>
                    </div>
                  </div>
                </div>
              `).join('')}
            </div>
          </div>
        </div>
      `);
    }

    if (app.ui.balanceDialogOpen) {
      parts.push(`
        <div class="nx-dialog-backdrop" data-action="close-overlays">
          <div class="nx-dialog" role="dialog" aria-modal="true" data-nexus-component="DeclaredBalanceDialog" data-nexus-source="FinanceScreen.kt" data-nexus-status="reproduzido" onclick="event.stopPropagation()">
            <h3 class="nx-title-lg">Atualizar saldo informado</h3>
            <p class="nx-body-sm">Informe o saldo total disponível em conta para cálculo de projeção.</p>
            <div class="nx-field">
              <label class="nx-field-label" for="nx-balance-input">Saldo em R$</label>
              <input id="nx-balance-input" class="nx-input tnum" type="text" inputmode="decimal" value="${esc(app.ui.balanceDraftText)}" data-input="balance-draft" />
            </div>
            <div class="nx-row-end">
              <button type="button" class="nx-btn-text" data-action="close-overlays">Cancelar</button>
              <button type="button" class="nx-btn-outlined is-selected" data-action="save-balance-dialog">Salvar saldo</button>
            </div>
          </div>
        </div>
      `);
    }

    if (app.ui.financeFilterSheetOpen) {
      const f = app.ui.financeFilter;
      parts.push(`
        <div class="nx-sheet-backdrop" data-action="close-overlays">
          <div class="nx-sheet" role="dialog" aria-modal="true" data-nexus-component="FinanceFilterBottomSheet" data-nexus-source="FinanceScreen.kt:FinanceFilterBottomSheet" data-nexus-status="reproduzido" onclick="event.stopPropagation()">
            <div class="nx-sheet-handle"></div>
            <div class="nx-sheet-body">
              <div class="nx-row-between">
                <h3 class="nx-title-lg">Filtrar lançamentos</h3>
                <button type="button" class="nx-btn-text" data-action="clear-finance-filters">Limpar tudo</button>
              </div>
              <div class="nx-title-sm">Período</div>
              <div class="nx-chip-row">
                <button type="button" class="nx-chip ${f.monthScope === 'MONTH' ? 'is-active' : ''}" data-action="set-tx-filter" data-key="monthScope" data-value="MONTH">Somente mês selecionado</button>
                <button type="button" class="nx-chip ${f.monthScope === 'ALL' ? 'is-active' : ''}" data-action="set-tx-filter" data-key="monthScope" data-value="ALL">Todos os meses</button>
              </div>
              <div class="nx-title-sm">Tipo</div>
              <div class="nx-chip-row">
                <button type="button" class="nx-chip ${!f.type ? 'is-active' : ''}" data-action="set-tx-filter" data-key="type" data-value="">Todos</button>
                ${Object.entries(LABELS.TransactionType).map(([k, l]) => `
                  <button type="button" class="nx-chip ${f.type === k ? 'is-active' : ''}" data-action="set-tx-filter" data-key="type" data-value="${k}">${esc(l)}</button>
                `).join('')}
              </div>
              <div class="nx-title-sm">Status</div>
              <div class="nx-chip-row">
                <button type="button" class="nx-chip ${!f.status ? 'is-active' : ''}" data-action="set-tx-filter" data-key="status" data-value="">Todos</button>
                ${Object.entries(LABELS.FinancialStatus).map(([k, l]) => `
                  <button type="button" class="nx-chip ${f.status === k ? 'is-active' : ''}" data-action="set-tx-filter" data-key="status" data-value="${k}">${esc(l)}</button>
                `).join('')}
              </div>
              <div class="nx-title-sm">Categoria</div>
              <div class="nx-chip-row">
                <button type="button" class="nx-chip ${!f.categoryId ? 'is-active' : ''}" data-action="set-tx-filter" data-key="categoryId" data-value="">Todas</button>
                ${app.state.categories.map((c) => `
                  <button type="button" class="nx-chip ${f.categoryId === c.id ? 'is-active' : ''}" data-action="set-tx-filter" data-key="categoryId" data-value="${esc(c.id)}">${esc(c.name)}</button>
                `).join('')}
              </div>
              <button type="button" class="nx-btn-primary" style="margin-top:6px" data-action="close-overlays">Aplicar filtros</button>
            </div>
          </div>
        </div>
      `);
    }

    if (app.ui.csvDialogOpen) {
      const csvText = buildCsvExportText();
      parts.push(`
        <div class="nx-dialog-backdrop" data-action="close-overlays">
          <div class="nx-dialog" role="dialog" aria-modal="true" data-nexus-component="FinanceCsvExportPreview" data-nexus-source="FinanceScreen.kt" data-nexus-status="parcial" onclick="event.stopPropagation()">
            <h3 class="nx-title-lg">Exportação CSV</h3>
            <p class="nx-body-sm">Gerado a partir dos lançamentos filtrados atuais (${csvText.split('\n').length - 1} registro(s)).</p>
            <textarea class="nx-textarea tnum" rows="6" readonly style="font-family:var(--nx-font-mono);font-size:11px">${esc(csvText)}</textarea>
            <div class="nx-row-end">
              <button type="button" class="nx-btn-text" data-action="close-overlays">Fechar</button>
              <button type="button" class="nx-btn-outlined is-selected" data-action="download-csv">Baixar .csv</button>
            </div>
          </div>
        </div>
      `);
    }

    if (app.ui.txDraft) {
      parts.push(TransactionSheet.renderTransactionSheet(app.state, app.ui.txDraft, app.ui.txDirty));
    }
    if (app.ui.eventDraft) {
      parts.push(EventSheet.renderEventSheet(app.state, app.ui.eventDraft, app.ui.eventDirty));
    }

    if (app.ui.confirmDialog) {
      const cd = app.ui.confirmDialog;
      parts.push(`
        <div class="nx-dialog-backdrop" data-action="cancel-confirm-dialog">
          <div class="nx-dialog" role="alertdialog" aria-modal="true" onclick="event.stopPropagation()">
            <h3 class="nx-title-lg">${esc(cd.title)}</h3>
            <p class="nx-body-md nx-text-secondary">${esc(cd.message)}</p>
            <div class="nx-row-end">
              <button type="button" class="nx-btn-text" data-action="cancel-confirm-dialog">Voltar</button>
              <button type="button" class="nx-btn-text nx-btn-danger-text" data-action="accept-confirm-dialog">${esc(cd.confirmLabel || 'Confirmar')}</button>
            </div>
          </div>
        </div>
      `);
    }

    if (app.ui.snackbar) {
      parts.push(`
        <div class="nx-snackbar" role="status">
          <span>${esc(app.ui.snackbar.message)}</span>
          <button type="button" class="nx-btn-text" style="color:var(--nx-bg);padding:2px 6px" data-action="dismiss-snackbar">OK</button>
        </div>
      `);
    }

    return parts.join('');
  }

  function buildCsvExportText() {
    const txs = Finance.filterTransactions(app.state, app.ui.financeFilter);
    const header = 'data,tipo,descricao,valor_brl,status,forma_pagamento,cartao,fatura,evento';
    const lines = txs.map((tx) => {
      const card = app.state.creditCards.find((c) => c.id === tx.creditCardId);
      const stmt = app.state.statements.find((s) => s.id === tx.statementId);
      const ev = app.state.events.find((e) => e.id === tx.eventId);
      const cols = [
        tx.dueDateIso || '',
        LABELS.TransactionType[tx.type] || tx.type,
        tx.description,
        (tx.amountCents / 100).toFixed(2).replace('.', ','),
        LABELS.FinancialStatus[tx.status] || tx.status,
        LABELS.PaymentMethod[tx.paymentMethod] || tx.paymentMethod,
        card ? card.name : '',
        stmt ? stmt.yearMonth : '',
        ev ? ev.name : ''
      ];
      return cols.map((v) => `"${String(v).replace(/"/g, '""')}"`).join(',');
    });
    return [header, ...lines].join('\n');
  }

  function renderTracePanel() {
    const panel = document.getElementById('pilot-trace-panel');
    if (!panel) return;
    panel.hidden = !app.ui.traceOpen;
    if (!app.ui.traceOpen) return;

    const shell = document.getElementById('nexus-app-shell');
    const nodes = shell ? Array.from(shell.querySelectorAll('[data-nexus-component]')) : [];
    const seen = new Set();
    const items = [];
    nodes.forEach((n) => {
      const comp = n.getAttribute('data-nexus-component');
      const src = n.getAttribute('data-nexus-source') || '';
      const status = n.getAttribute('data-nexus-status') || 'reproduzido';
      const key = `${comp}|${src}`;
      if (!seen.has(key)) {
        seen.add(key);
        items.push({ comp, src, status });
      }
    });

    panel.innerHTML = `
      <div class="nx-row-between">
        <strong style="font-size:14px">Rastreabilidade Android ↔ Web</strong>
        <button type="button" class="pilot-btn" data-action="toggle-trace">Fechar</button>
      </div>
      <div style="font-size:12px;color:#9A9A9A;line-height:1.45">
        Componentes ativos na árvore DOM de <code>#nexus-app-shell</code> (${items.length}):
      </div>
      <div class="pilot-trace-item" style="border-color:#2E4057">
        <span style="font-size:12px;font-weight:600;color:#7CFFB2">Atalho do fluxo ponta a ponta</span>
        <span style="font-size:11px;color:#9A9A9A">Testa Finanças → Ingresso Kotlin → Fatura 2026-11 → Evento</span>
        <div style="display:flex;flex-wrap:wrap;gap:6px;margin-top:4px">
          <button type="button" class="pilot-btn" data-action="demo-flow-finance-tx">Abrir Ingresso Kotlin</button>
          <button type="button" class="pilot-btn" data-action="open-routine-assistant" data-routine-tab="0">Briefing Diário</button>
        </div>
      </div>
      ${items.map((it) => `
        <div class="pilot-trace-item">
          <div style="display:flex;justify-content:space-between;gap:8px;align-items:center">
            <span style="font-size:12px;font-weight:600;color:#F2F2F2">${esc(it.comp)}</span>
            <span class="pilot-trace-tag ${esc(it.status)}">${esc(it.status)}</span>
          </div>
          <code style="font-size:11px;color:#9A9A9A;word-break:break-all">${esc(it.src)}</code>
        </div>
      `).join('')}
      <div style="font-size:11px;color:#9A9A9A">
        Documento completo em <code>web-mirror-pilot/TRACEABILITY.md</code>.
      </div>
    `;
  }

  function syncLabBarUi() {
    document.querySelectorAll('[data-lab-theme]').forEach((btn) => {
      btn.classList.toggle('is-active', btn.getAttribute('data-lab-theme') === app.ui.theme);
    });
    document.querySelectorAll('[data-lab-viewport]').forEach((btn) => {
      btn.classList.toggle('is-active', btn.getAttribute('data-lab-viewport') === app.ui.viewport);
    });
    const traceBtn = document.querySelector('[data-action="toggle-trace"]');
    if (traceBtn) {
      traceBtn.textContent = app.ui.traceOpen ? 'Ocultar rastreabilidade' : 'Rastreabilidade';
    }
  }

  function render() {
    const shell = document.getElementById('nexus-app-shell');
    if (!shell) return;

    shell.setAttribute('data-nexus-theme', app.ui.theme);
    shell.classList.toggle('viewport-tablet', app.ui.viewport === 'tablet');
    shell.classList.toggle('viewport-fluid', app.ui.viewport === 'fluid');
    shell.classList.toggle('is-wide', app.ui.viewport !== 'mobile');

    shell.innerHTML = `
      ${renderTopBar()}
      ${renderMainContent()}
      ${renderBottomBarAndFab()}
      ${renderAuxiliaryOverlays()}
    `;

    syncLabBarUi();
    renderTracePanel();

    if (app.ui.highlightedStatementId && app.ui.activeTab === 'FINANCE' && app.ui.financeTab === 'CARDS') {
      const el = document.getElementById(`stmt-anchor-${app.ui.highlightedStatementId}`);
      if (el) el.scrollIntoView({ behavior: 'smooth', block: 'center' });
    }
  }

  function saveTxFromDraft() {
    const d = app.ui.txDraft;
    if (!d) return;
    if (!d.description.trim()) {
      showSnackbar('Informe a descrição do lançamento.');
      return;
    }
    const cents = TransactionSheet.parseInputBrToCents(d.amountText);
    if (cents <= 0) {
      showSnackbar('Informe um valor maior que zero.');
      return;
    }

    let statementId = null;
    let creditCardId = null;
    if (d.paymentMethod === 'CREDIT_CARD' && d.creditCardId) {
      creditCardId = d.creditCardId;
      statementId = d.statementId || (app.state.statements.find((s) => s.creditCardId === d.creditCardId)?.id || null);
    }

    const entity = {
      id: d.id || `tx_${Date.now()}`,
      type: d.type,
      description: d.description.trim(),
      amountCents: cents,
      categoryId: d.categoryId || null,
      status: d.status,
      paymentMethod: d.paymentMethod,
      dueDateIso: d.dueDateIso || app.state.todayIso,
      creditCardId,
      statementId,
      projectId: d.projectId || null,
      eventId: d.eventId || null,
      taskId: d.taskId || null,
      eventCostType: d.eventId ? (d.eventCostType || 'OTHER') : null,
      notes: d.notes || ''
    };

    if (d.isNew) {
      app.state.transactions.unshift(entity);
      showSnackbar('Lançamento criado e vinculado ao modelo relacional.');
    } else {
      const idx = app.state.transactions.findIndex((t) => t.id === entity.id);
      if (idx >= 0) app.state.transactions[idx] = entity;
      showSnackbar('Lançamento atualizado.');
    }
    app.ui.txDraft = null;
    app.ui.txDirty = false;
    render();
  }

  function saveEventFromDraft() {
    const d = app.ui.eventDraft;
    if (!d) return;
    if (!d.name.trim()) {
      showSnackbar('Informe o nome do evento.');
      return;
    }
    const priceCents = TransactionSheet.parseInputBrToCents(d.ticketPriceText);
    const feesCents = TransactionSheet.parseInputBrToCents(d.ticketFeesText);

    const existing = d.id ? app.state.events.find((e) => e.id === d.id) : null;
    const entity = {
      ...(existing || {}),
      id: d.id || `evt_${Date.now()}`,
      name: d.name.trim(),
      description: d.description || '',
      categoryId: d.categoryId || 'cat_eventos',
      startDateIso: d.startDateIso || app.state.todayIso,
      startTime: d.startTime || '18:00',
      endDateIso: d.startDateIso || app.state.todayIso,
      endTime: d.endTime || '21:00',
      isAllDay: Boolean(d.isAllDay),
      locationName: d.locationName || '',
      city: d.city || '',
      state: d.state || '',
      eventStatus: d.eventStatus || 'PLANNED',
      ticketStatus: d.ticketStatus || 'PENDING',
      ticketPriceCents: priceCents,
      ticketFeesCents: feesCents,
      notes: d.notes || ''
    };

    if (d.isNew) {
      app.state.events.push(entity);
      showSnackbar('Evento criado.');
    } else {
      const idx = app.state.events.findIndex((e) => e.id === entity.id);
      if (idx >= 0) app.state.events[idx] = entity;
      showSnackbar('Evento atualizado.');
    }
    app.ui.eventDraft = null;
    app.ui.eventDirty = false;
    render();
  }

  function attemptCloseTxSheet() {
    if (app.ui.txDirty) {
      app.ui.confirmDialog = {
        type: 'DISCARD_TX',
        title: 'Descartar alterações?',
        message: 'Há modificações não salvas neste lançamento.',
        confirmLabel: 'Descartar'
      };
      render();
      return;
    }
    app.ui.txDraft = null;
    app.ui.txDirty = false;
    render();
  }

  function attemptCloseEventSheet() {
    if (app.ui.eventDirty) {
      app.ui.confirmDialog = {
        type: 'DISCARD_EVENT',
        title: 'Descartar alterações?',
        message: 'Há modificações não salvas neste evento.',
        confirmLabel: 'Descartar'
      };
      render();
      return;
    }
    app.ui.eventDraft = null;
    app.ui.eventDirty = false;
    render();
  }

  // Event Delegation global
  document.addEventListener('click', (e) => {
    const target = e.target.closest('[data-action], [data-lab-theme], [data-lab-viewport]');
    if (!target) return;

    const labTheme = target.getAttribute('data-lab-theme');
    if (labTheme) {
      app.ui.theme = labTheme;
      render();
      return;
    }
    const labVp = target.getAttribute('data-lab-viewport');
    if (labVp) {
      app.ui.viewport = labVp;
      render();
      return;
    }

    const action = target.getAttribute('data-action');
    switch (action) {
      case 'toggle-trace':
        app.ui.traceOpen = !app.ui.traceOpen;
        render();
        break;
      case 'reset-pilot-data':
        app.state = createInitialState();
        app.ui.txDraft = null;
        app.ui.eventDraft = null;
        app.ui.highlightedStatementId = null;
        app.ui.financeDonutSliceId = null;
        app.ui.homeDonutSliceId = null;
        app.ui.financeFilter = { monthScope: 'MONTH', type: null, status: null, categoryId: null, paymentMethod: null };
        showSnackbar('Dados de Gabriel restaurados ao estado inicial.');
        break;
      case 'select-bottom-tab':
        app.ui.subRoute = null;
        app.ui.activeTab = target.getAttribute('data-tab') || 'HOME';
        app.ui.highlightedStatementId = null;
        render();
        break;
      case 'open-home-tab':
        app.ui.subRoute = null;
        app.ui.activeTab = 'HOME';
        render();
        break;
      case 'open-tasks-tab':
        app.ui.subRoute = null;
        app.ui.activeTab = 'TASKS';
        render();
        break;
      case 'open-agenda-tab':
        app.ui.subRoute = null;
        app.ui.activeTab = 'AGENDA';
        render();
        break;
      case 'open-events-tab':
        app.ui.subRoute = null;
        app.ui.activeTab = 'EVENTS';
        render();
        break;
      case 'open-finance-tab':
        app.ui.subRoute = null;
        app.ui.activeTab = 'FINANCE';
        app.ui.financeTab = target.getAttribute('data-finance-tab') || 'OVERVIEW';
        app.ui.highlightedStatementId = null;
        render();
        break;
      case 'open-finance-overdue':
        app.ui.subRoute = null;
        app.ui.activeTab = 'FINANCE';
        app.ui.financeTab = 'TRANSACTIONS';
        app.ui.financeFilter = { monthScope: 'ALL', type: 'EXPENSE', status: 'OVERDUE', categoryId: null, paymentMethod: null };
        render();
        break;
      case 'open-routine-assistant':
        app.ui.quickAddOpen = false;
        app.ui.subRoute = 'ROUTINE_ASSISTANT';
        app.ui.routineTabIndex = Number(target.getAttribute('data-routine-tab') || 0);
        render();
        break;
      case 'select-routine-tab':
        app.ui.routineTabIndex = Number(target.getAttribute('data-routine-tab') || 0);
        render();
        break;
      case 'open-more-menu':
        app.ui.subRoute = 'MORE';
        render();
        break;
      case 'open-global-search':
        app.ui.subRoute = 'SEARCH';
        render();
        break;
      case 'open-market-notice':
        app.ui.subRoute = null;
        app.ui.activeTab = 'FINANCE';
        app.ui.financeTab = 'OVERVIEW';
        showSnackbar('Módulo Mercado completo fica fora da Etapa 1; veja o resumo simulado em Finanças.');
        break;
      case 'pop-subroute':
        app.ui.subRoute = null;
        render();
        break;
      case 'open-quick-add':
        app.ui.quickAddOpen = true;
        render();
        break;
      case 'close-overlays':
        app.ui.quickAddOpen = false;
        app.ui.customizeOpen = false;
        app.ui.balanceDialogOpen = false;
        app.ui.financeFilterSheetOpen = false;
        app.ui.csvDialogOpen = false;
        render();
        break;
      case 'open-customize-dashboard':
        app.ui.customizeOpen = true;
        render();
        break;
      case 'toggle-personalization': {
        const key = target.getAttribute('data-key');
        app.state.personalization[key] = !app.state.personalization[key];
        render();
        break;
      }
      case 'toggle-block-enabled': {
        const bType = target.getAttribute('data-block-type');
        const b = app.state.blocks.find((x) => x.type === bType);
        if (b) b.enabled = !b.enabled;
        render();
        break;
      }
      case 'move-block': {
        const bType = target.getAttribute('data-block-type');
        const dir = Number(target.getAttribute('data-dir'));
        const sorted = [...app.state.blocks].sort((a, b) => a.order - b.order);
        const idx = sorted.findIndex((x) => x.type === bType);
        const swapIdx = idx + dir;
        if (idx >= 0 && swapIdx >= 0 && swapIdx < sorted.length) {
          const tmp = sorted[idx].order;
          sorted[idx].order = sorted[swapIdx].order;
          sorted[swapIdx].order = tmp;
        }
        render();
        break;
      }
      case 'open-balance-dialog':
        app.ui.balanceDraftText = TransactionSheet.centsToInputBr(app.state.declaredBalanceCents);
        app.ui.balanceDialogOpen = true;
        render();
        break;
      case 'save-balance-dialog':
        app.state.declaredBalanceCents = TransactionSheet.parseInputBrToCents(app.ui.balanceDraftText);
        app.ui.balanceDialogOpen = false;
        showSnackbar('Saldo informado atualizado.');
        break;
      case 'toggle-task': {
        const tId = target.getAttribute('data-task-id');
        const task = app.state.tasks.find((t) => t.id === tId);
        if (task) {
          task.status = task.status === 'COMPLETED' ? 'NOT_STARTED' : 'COMPLETED';
          showSnackbar(task.status === 'COMPLETED' ? 'Tarefa concluída.' : 'Tarefa reaberta.');
        }
        break;
      }
      case 'postpone-task': {
        const tId = target.getAttribute('data-task-id');
        const task = app.state.tasks.find((t) => t.id === tId);
        if (task) {
          task.dueDateIso = '2026-11-24';
          showSnackbar('Tarefa adiada para amanhã (24/11).');
        }
        break;
      }
      case 'toggle-settle-tx':
      case 'settle-tx': {
        const txId = target.getAttribute('data-tx-id');
        const tx = app.state.transactions.find((t) => t.id === txId);
        if (tx) {
          if (action === 'settle-tx') tx.status = 'SETTLED';
          else tx.status = tx.status === 'SETTLED' ? 'PENDING' : 'SETTLED';
          showSnackbar(tx.status === 'SETTLED' ? 'Lançamento marcado como realizado.' : 'Lançamento reaberto como pendente.');
        }
        break;
      }
      case 'postpone-tx': {
        const txId = target.getAttribute('data-tx-id');
        const tx = app.state.transactions.find((t) => t.id === txId);
        if (tx) {
          tx.dueDateIso = '2026-11-24';
          if (tx.status === 'OVERDUE') tx.status = 'PENDING';
          showSnackbar('Conta adiada para 24/11/2026.');
        }
        break;
      }
      case 'mark-briefing-reviewed':
        app.state.briefingState.morningReviewedToday = true;
        if (!app.state.briefingState.history.some((h) => h.dateIso === '2026-11-23' && h.briefingType === 'Briefing Diário')) {
          app.state.briefingState.history.unshift({
            id: `bh_${Date.now()}`,
            briefingType: 'Briefing Diário',
            dateIso: '2026-11-23',
            summaryText: 'Revisão diária concluída no Assistente de Rotina.',
            reviewedAt: '23/11/2026 agora'
          });
        }
        showSnackbar('Briefing Diário marcado como revisado.');
        break;
      case 'mark-night-reviewed':
        app.state.briefingState.nightReviewedTonight = true;
        if (!app.state.briefingState.history.some((h) => h.dateIso === '2026-11-23' && h.briefingType === 'Revisão Noturna')) {
          app.state.briefingState.history.unshift({
            id: `bh_${Date.now()}`,
            briefingType: 'Revisão Noturna',
            dateIso: '2026-11-23',
            summaryText: 'Fechamento noturno realizado · pendências encaminhadas para 24/11.',
            reviewedAt: '23/11/2026 agora'
          });
        }
        showSnackbar('Revisão Noturna concluída.');
        break;
      case 'toggle-routine-setting': {
        const key = target.getAttribute('data-setting-key');
        app.state.routineSettings[key] = !app.state.routineSettings[key];
        render();
        break;
      }
      case 'set-default-event-reminder':
        app.state.routineSettings.defaultEventReminderMinutes = Number(target.getAttribute('data-minutes') || 60);
        render();
        break;
      case 'test-notification':
        showSnackbar('Notificação simulada: Briefing NEXUS e lembrete de fatura ativos.');
        break;
      case 'snooze-reminder': {
        const r = app.state.reminders.find((x) => x.id === target.getAttribute('data-reminder-id'));
        if (r) {
          r.status = 'ACTIVE';
          r.triggerLabel = `${r.triggerLabel} (+15m)`;
          showSnackbar('Lembrete adiado em 15 minutos.');
        }
        break;
      }
      case 'cancel-reminder': {
        const r = app.state.reminders.find((x) => x.id === target.getAttribute('data-reminder-id'));
        if (r) {
          r.status = 'CANCELLED';
          showSnackbar('Lembrete cancelado.');
        }
        break;
      }
      case 'prev-month':
        app.state.selectedMonthIso = shiftMonthIso(app.state.selectedMonthIso, -1);
        app.ui.financeDonutSliceId = null;
        render();
        break;
      case 'next-month':
        app.state.selectedMonthIso = shiftMonthIso(app.state.selectedMonthIso, 1);
        app.ui.financeDonutSliceId = null;
        render();
        break;
      case 'reset-month':
        app.state.selectedMonthIso = '2026-11';
        app.ui.financeDonutSliceId = null;
        render();
        break;
      case 'select-home-bar-month':
        app.ui.homeBarMonthId = target.getAttribute('data-month-id');
        render();
        break;
      case 'select-home-donut-slice': {
        const sId = target.getAttribute('data-slice-id');
        app.ui.homeDonutSliceId = app.ui.homeDonutSliceId === sId ? null : sId;
        render();
        break;
      }
      case 'select-finance-bar-month':
        app.ui.financeBarMonthId = target.getAttribute('data-month-id');
        render();
        break;
      case 'select-finance-donut-slice': {
        const sId = target.getAttribute('data-slice-id');
        app.ui.financeDonutSliceId = app.ui.financeDonutSliceId === sId ? null : sId;
        render();
        break;
      }
      case 'set-finance-cat-scope':
        app.ui.financeCategoryScope = target.getAttribute('data-scope');
        app.ui.financeDonutSliceId = null;
        render();
        break;
      case 'set-finance-cashflow-mode':
        app.ui.financeCashflowMode = target.getAttribute('data-mode');
        render();
        break;
      case 'filter-tx-by-slice': {
        const sliceId = target.getAttribute('data-slice-id');
        app.ui.financeTab = 'TRANSACTIONS';
        app.ui.financeFilter = {
          monthScope: 'MONTH',
          type: 'EXPENSE',
          status: null,
          categoryId: sliceId && !sliceId.startsWith('__none') ? sliceId : null,
          paymentMethod: null
        };
        render();
        break;
      }
      case 'open-finance-filter-sheet':
        app.ui.financeFilterSheetOpen = true;
        render();
        break;
      case 'set-tx-filter': {
        const key = target.getAttribute('data-key');
        const val = target.getAttribute('data-value') || null;
        app.ui.financeFilter[key] = val;
        render();
        break;
      }
      case 'clear-finance-filters':
        app.ui.financeFilter = { monthScope: 'MONTH', type: null, status: null, categoryId: null, paymentMethod: null };
        render();
        break;
      case 'export-finance-csv':
        app.ui.csvDialogOpen = true;
        render();
        break;
      case 'download-csv': {
        const csv = buildCsvExportText();
        const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `nexus_lancamentos_${app.state.selectedMonthIso}.csv`;
        document.body.appendChild(a);
        a.click();
        a.remove();
        URL.revokeObjectURL(url);
        app.ui.csvDialogOpen = false;
        showSnackbar('CSV exportado com sucesso.');
        break;
      }
      case 'close-statement': {
        const stmt = app.state.statements.find((s) => s.id === target.getAttribute('data-statement-id'));
        if (stmt) {
          stmt.status = 'CLOSED';
          showSnackbar(`Fatura ${stmt.yearMonth} fechada.`);
        }
        break;
      }
      case 'pay-statement': {
        const stmt = app.state.statements.find((s) => s.id === target.getAttribute('data-statement-id'));
        if (stmt) {
          stmt.status = 'PAID';
          app.state.transactions.forEach((tx) => {
            if (tx.statementId === stmt.id && tx.status !== 'CANCELLED') tx.status = 'SETTLED';
          });
          showSnackbar(`Fatura ${stmt.yearMonth} paga e lançamentos liquidados.`);
        }
        break;
      }
      case 'open-card-statement': {
        const stmtId = target.getAttribute('data-statement-id');
        app.ui.subRoute = null;
        app.ui.activeTab = 'FINANCE';
        app.ui.financeTab = 'CARDS';
        app.ui.highlightedStatementId = stmtId;
        render();
        break;
      }
      case 'open-tx': {
        const tx = app.state.transactions.find((t) => t.id === target.getAttribute('data-tx-id'));
        if (tx) {
          app.ui.txDraft = TransactionSheet.createDraftFromTx(tx, {}, app.state);
          app.ui.txDirty = false;
          render();
        }
        break;
      }
      case 'demo-flow-finance-tx': {
        app.ui.subRoute = null;
        app.ui.activeTab = 'FINANCE';
        app.ui.financeTab = 'TRANSACTIONS';
        const tx = app.state.transactions.find((t) => t.id === 'tx_kotlin_ingresso');
        if (tx) {
          app.ui.txDraft = TransactionSheet.createDraftFromTx(tx, {}, app.state);
          app.ui.txDirty = false;
        }
        render();
        break;
      }
      case 'new-tx':
      case 'quick-new-tx':
        app.ui.quickAddOpen = false;
        app.ui.txDraft = TransactionSheet.createDraftFromTx(null, { type: target.getAttribute('data-tx-type') || 'EXPENSE' }, app.state);
        app.ui.txDirty = false;
        render();
        break;
      case 'add-event-cost': {
        const evId = target.getAttribute('data-event-id');
        const ev = app.state.events.find((x) => x.id === evId);
        app.ui.eventDraft = null;
        app.ui.eventDirty = false;
        app.ui.txDraft = TransactionSheet.createDraftFromTx(null, {
          type: 'EXPENSE',
          eventId: evId,
          dueDateIso: ev ? ev.startDateIso : app.state.todayIso,
          paymentMethod: 'CREDIT_CARD',
          creditCardId: 'card_uv',
          statementId: 'stmt_uv_2026_11'
        }, app.state);
        app.ui.txDirty = false;
        render();
        break;
      }
      case 'tx-field-btn':
        if (app.ui.txDraft) {
          app.ui.txDraft[target.getAttribute('data-field')] = target.getAttribute('data-value');
          app.ui.txDirty = true;
          render();
        }
        break;
      case 'save-tx-sheet':
        saveTxFromDraft();
        break;
      case 'close-tx-sheet':
      case 'close-tx-sheet-backdrop':
        attemptCloseTxSheet();
        break;
      case 'request-delete-tx':
        app.ui.confirmDialog = {
          type: 'DELETE_TX',
          title: 'Excluir lançamento?',
          message: 'Esta ação removerá o lançamento e atualizará faturas e custos de eventos vinculados.',
          confirmLabel: 'Excluir'
        };
        render();
        break;
      case 'jump-to-statement': {
        const stmtId = target.getAttribute('data-statement-id');
        app.ui.txDraft = null;
        app.ui.txDirty = false;
        app.ui.subRoute = null;
        app.ui.activeTab = 'FINANCE';
        app.ui.financeTab = 'CARDS';
        app.ui.highlightedStatementId = stmtId;
        showSnackbar('Navegou para a fatura vinculada em Finanças › Cartões.');
        break;
      }
      case 'jump-to-event': {
        const evId = target.getAttribute('data-event-id');
        const ev = app.state.events.find((x) => x.id === evId);
        app.ui.txDraft = null;
        app.ui.txDirty = false;
        app.ui.subRoute = null;
        app.ui.activeTab = 'EVENTS';
        app.ui.eventsFilter = 'ALL';
        if (ev) {
          app.ui.eventDraft = EventSheet.createDraftFromEvent(ev, app.state);
          app.ui.eventDirty = false;
        }
        render();
        break;
      }
      case 'set-events-filter':
        app.ui.eventsFilter = target.getAttribute('data-filter') || 'UPCOMING';
        render();
        break;
      case 'open-event': {
        const ev = app.state.events.find((x) => x.id === target.getAttribute('data-event-id'));
        if (ev) {
          app.ui.eventDraft = EventSheet.createDraftFromEvent(ev, app.state);
          app.ui.eventDirty = false;
          render();
        }
        break;
      }
      case 'new-event':
      case 'quick-new-event':
        app.ui.quickAddOpen = false;
        app.ui.eventDraft = EventSheet.createDraftFromEvent(null, app.state);
        app.ui.eventDirty = false;
        render();
        break;
      case 'save-event-sheet':
        saveEventFromDraft();
        break;
      case 'close-event-sheet':
      case 'close-event-sheet-backdrop':
        attemptCloseEventSheet();
        break;
      case 'request-delete-event':
        app.ui.confirmDialog = {
          type: 'DELETE_EVENT',
          title: 'Excluir evento?',
          message: 'O evento será removido da lista (lançamentos vinculados permanecem preservados sem vínculo).',
          confirmLabel: 'Excluir'
        };
        render();
        break;
      case 'cancel-confirm-dialog':
        app.ui.confirmDialog = null;
        render();
        break;
      case 'accept-confirm-dialog': {
        const cd = app.ui.confirmDialog;
        app.ui.confirmDialog = null;
        if (!cd) break;
        if (cd.type === 'DISCARD_TX') {
          app.ui.txDraft = null;
          app.ui.txDirty = false;
          render();
        } else if (cd.type === 'DISCARD_EVENT') {
          app.ui.eventDraft = null;
          app.ui.eventDirty = false;
          render();
        } else if (cd.type === 'DELETE_TX' && app.ui.txDraft && app.ui.txDraft.id) {
          app.state.transactions = app.state.transactions.filter((t) => t.id !== app.ui.txDraft.id);
          app.ui.txDraft = null;
          app.ui.txDirty = false;
          showSnackbar('Lançamento excluído.');
        } else if (cd.type === 'DELETE_EVENT' && app.ui.eventDraft && app.ui.eventDraft.id) {
          const evId = app.ui.eventDraft.id;
          app.state.events = app.state.events.filter((ev) => ev.id !== evId);
          app.state.transactions.forEach((tx) => {
            if (tx.eventId === evId) tx.eventId = null;
          });
          app.ui.eventDraft = null;
          app.ui.eventDirty = false;
          showSnackbar('Evento excluído.');
        }
        break;
      }
      case 'dismiss-snackbar':
        app.ui.snackbar = null;
        render();
        break;
      default:
        break;
    }
  });

  document.addEventListener('input', (e) => {
    const el = e.target;
    if (el.getAttribute('data-input') === 'global-search') {
      app.ui.globalSearchQuery = el.value;
      const pos = el.selectionStart;
      render();
      const nextInput = document.getElementById('nx-search-input');
      if (nextInput) {
        nextInput.focus();
        nextInput.setSelectionRange(pos, pos);
      }
      return;
    }
    if (el.getAttribute('data-input') === 'balance-draft') {
      app.ui.balanceDraftText = el.value;
      return;
    }
    const txField = el.getAttribute('data-tx-field');
    if (txField && app.ui.txDraft) {
      app.ui.txDraft[txField] = el.value;
      app.ui.txDirty = true;
      return;
    }
    const evField = el.getAttribute('data-ev-field');
    if (evField && app.ui.eventDraft) {
      app.ui.eventDraft[evField] = el.value;
      app.ui.eventDirty = true;
    }
  });

  document.addEventListener('change', (e) => {
    const el = e.target;
    const txField = el.getAttribute('data-tx-field');
    if (txField && app.ui.txDraft) {
      app.ui.txDraft[txField] = el.value;
      if (txField === 'creditCardId') {
        const firstStmt = app.state.statements.find((s) => s.creditCardId === el.value);
        app.ui.txDraft.statementId = firstStmt ? firstStmt.id : '';
      }
      if (txField === 'paymentMethod' && el.value === 'CREDIT_CARD' && !app.ui.txDraft.creditCardId) {
        app.ui.txDraft.creditCardId = app.state.creditCards[0]?.id || '';
        const firstStmt = app.state.statements.find((s) => s.creditCardId === app.ui.txDraft.creditCardId);
        app.ui.txDraft.statementId = firstStmt ? firstStmt.id : '';
      }
      app.ui.txDirty = true;
      render();
      return;
    }
    const evField = el.getAttribute('data-ev-field');
    if (evField && app.ui.eventDraft) {
      app.ui.eventDraft[evField] = el.value;
      app.ui.eventDirty = true;
      render();
    }
  });

  document.addEventListener('DOMContentLoaded', render);
})();
