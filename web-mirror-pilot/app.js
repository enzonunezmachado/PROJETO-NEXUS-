/*
 * NEXUS Web Mirror Pilot — Unidirectional State Engine, Navigation & Pilot Lab Controller
 * Fonte Android: MainActivity.kt (BOTTOM_NAV_TAB_ORDER, SubRoute, QuickAddChooserSheet), NexusViewModel.kt
 */
(function () {
  const {
    createInitialState,
    appendAudit,
    LABELS,
    MARKET_ASSET_CATALOG,
    formatBrl,
    formatDateBr,
    shiftMonthIso,
    addDaysIso,
    parseIsoDate,
    weekdayBit,
    openOrMaterializeRecurringOccurrence,
    materializeNextOccurrenceIfNeeded,
    contextualCategoriesForAgendaKind,
    suggestExpenseCategoryIdForTask
  } = window.NexusPilot.Data;
  const { esc, ICONS, renderWordmarkSvg } = window.NexusPilot.Primitives;
  const {
    Home,
    Tasks,
    Agenda,
    Finance,
    Events,
    Market,
    Planning,
    RoutineAssistant,
    MoreSubroutes
  } = window.NexusPilot.Screens;
  const {
    TransactionSheet,
    EventSheet,
    EntitySheets,
    FinanceExtraSheets
  } = window.NexusPilot.Sheets;

  // Ordem exata de MainActivity.kt: BOTTOM_NAV_TAB_ORDER = [TASKS, AGENDA, HOME, FINANCE, EVENTS]
  const BOTTOM_TABS = [
    { key: 'TASKS', label: 'Tarefas', icon: ICONS.tasks },
    { key: 'AGENDA', label: 'Agenda', icon: ICONS.agenda },
    { key: 'HOME', label: 'Início', icon: ICONS.home },
    { key: 'FINANCE', label: 'Finanças', icon: ICONS.finance },
    { key: 'EVENTS', label: 'Eventos', icon: ICONS.events }
  ];

  const SUBROUTE_TITLES = {
    ROUTINE_ASSISTANT: 'Assistente de Rotina',
    MORE: 'Mais',
    PLANNING: 'Planejamento',
    MARKET: 'Mercado',
    MARKET_DETAIL: 'Ativo',
    CUSTOMIZE: 'Personalizar Dashboard',
    QUOTES: 'Biblioteca de Frases',
    WIDGETS: 'Widgets do NEXUS',
    SETTINGS: 'Configurações',
    BACKUP: 'Backup e Restauração',
    DIAGNOSTICS: 'Diagnóstico',
    SEARCH: 'Pesquisa global'
  };

  function createInitialUi() {
    return {
      theme: 'dark',
      viewport: 'mobile', // 'mobile' | 'tablet-portrait' | 'tablet-landscape' | 'fluid'
      traceOpen: false,
      activeTab: 'HOME',
      subRouteStack: [],
      routineTabIndex: 0,
      tasksFilter: {
        tabMode: 'TODAY',
        statusFilter: null,
        priorityFilter: null,
        projectFilterId: null
      },
      agendaFilter: {
        viewMode: 'MONTH',
        selectedDateIso: '2026-11-23',
        kindFilter: null,
        categoryFilterId: null,
        showPaid: false,
        isCalendarExpanded: true
      },
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
      marketCategoryFilter: 'ALL',
      selectedMarketAssetCode: 'USD-BRL',
      marketHistoryRange: '1M',
      marketSelectedPointIdx: null,
      planningFilter: {
        query: '',
        status: 'ACTIVE',
        categoryId: null
      },
      selectedPersonalGoalId: null,
      newMilestoneTitle: '',
      newGoalLogNote: '',
      expandedCustomBlockType: null,
      quoteSearchQuery: '',
      backupJsonExpanded: false,
      backupImportText: '',
      backupLastSummary: '',
      backupPendingPreview: null,
      financeFilter: {
        monthScope: 'MONTH',
        type: null,
        status: null,
        categoryId: null,
        paymentMethod: null
      },
      // Modais e BottomSheets
      quickAddOpen: false,
      tasksFilterSheetOpen: false,
      agendaFilterSheetOpen: false,
      balanceDialogOpen: false,
      balanceDraftText: '4825,00',
      financeFilterSheetOpen: false,
      csvDialogOpen: false,
      txDraft: null,
      txDirty: false,
      eventDraft: null,
      eventDirty: false,
      taskDraft: null,
      taskDirty: false,
      apptDraft: null,
      apptDirty: false,
      projectDraft: null,
      projectDirty: false,
      cardDraft: null,
      installmentDraft: null,
      recurringDraft: null,
      budgetDraft: null,
      financialGoalDraft: null,
      fgoalAmountDraft: null,
      loanDraft: null,
      personalGoalDraft: null,
      marketAlertDraft: null,
      quoteDraft: null,
      categoryDraft: null,
      confirmDialog: null,
      snackbar: null
    };
  }

  const app = {
    state: createInitialState(),
    ui: createInitialUi()
  };

  let snackbarTimer = null;

  function currentSubRoute() {
    const stack = app.ui.subRouteStack;
    return stack.length ? stack[stack.length - 1] : null;
  }

  function pushSubRoute(route) {
    if (!route) return;
    if (currentSubRoute() !== route) {
      app.ui.subRouteStack.push(route);
    }
  }

  function popSubRoute() {
    if (app.ui.subRouteStack.length > 0) {
      app.ui.subRouteStack.pop();
    }
  }

  function clearSubRoutes() {
    app.ui.subRouteStack = [];
  }

  function clearExtraEditors() {
    app.ui.cardDraft = null;
    app.ui.installmentDraft = null;
    app.ui.recurringDraft = null;
    app.ui.budgetDraft = null;
    app.ui.financialGoalDraft = null;
    app.ui.fgoalAmountDraft = null;
    app.ui.loanDraft = null;
    app.ui.personalGoalDraft = null;
    app.ui.marketAlertDraft = null;
    app.ui.quoteDraft = null;
    app.ui.categoryDraft = null;
    app.ui.backupPendingPreview = null;
  }

  function showSnackbar(message) {
    app.ui.snackbar = { message };
    render();
    clearTimeout(snackbarTimer);
    snackbarTimer = setTimeout(() => {
      app.ui.snackbar = null;
      render();
    }, 3400);
  }

  function parseDecimalPt(text) {
    const clean = String(text || '').trim().replace(/\s+/g, '');
    if (!clean) return NaN;
    if (clean.includes(',')) {
      return Number(clean.replace(/\./g, '').replace(',', '.'));
    }
    return Number(clean);
  }

  function formatDecimalPt(val, decimals = 2) {
    if (val == null || Number.isNaN(Number(val))) return '';
    return Number(val).toFixed(decimals).replace('.', ',');
  }

  function evaluateMarketAlerts(state) {
    let triggeredCount = 0;
    state.marketAlerts.forEach((al) => {
      if (!al.isActive || al.status !== 'WAITING') return;
      const q = state.marketQuotes[al.assetCode];
      if (!q || q.price == null) return;
      const hit = al.condition === 'PRICE_ABOVE'
        ? q.price >= al.targetPrice
        : q.price <= al.targetPrice;
      if (hit) {
        al.status = 'TRIGGERED';
        al.triggeredAt = q.fetchedAt || `${state.todayIso} agora`;
        triggeredCount += 1;
      }
    });
    return triggeredCount;
  }

  function renderTopBar() {
    const sub = currentSubRoute();
    if (sub === 'SEARCH') {
      return `
        <header class="nx-search-topbar" data-nexus-component="GlobalSearchTopBar" data-nexus-source="MoreSettingsBackupDiagnosticsScreens.kt:GlobalSearchScreen" data-nexus-status="reproduzido">
          <button type="button" class="nx-icon-btn" data-action="pop-subroute" aria-label="Voltar">${ICONS.back}</button>
          <input id="nx-search-input" class="nx-input" type="search" placeholder="Buscar tarefas, objetivos, projetos, eventos ou financeiro" value="${esc(app.ui.globalSearchQuery)}" data-input="global-search" autofocus />
          ${app.ui.globalSearchQuery ? `<button type="button" class="nx-icon-btn" data-action="clear-global-search" aria-label="Limpar busca">${ICONS.close}</button>` : ''}
        </header>
      `;
    }

    if (sub) {
      let title = SUBROUTE_TITLES[sub] || sub;
      let trailing = '';
      if (sub === 'MARKET_DETAIL') {
        const asset = MARKET_ASSET_CATALOG.find((a) => a.code === app.ui.selectedMarketAssetCode);
        title = asset ? `${asset.displayName} (${asset.ticker})` : 'Detalhe do ativo';
        if (asset) {
          trailing = `<button type="button" class="nx-btn-outlined" style="margin-left:auto" data-action="new-market-alert" data-asset-code="${esc(asset.code)}">+ Alerta</button>`;
        }
      }
      return `
        <header class="nx-subroute-bar" data-nexus-component="SubRouteTopBar" data-nexus-source="MainActivity.kt:SubRouteTopBar" data-nexus-status="reproduzido">
          <button type="button" class="nx-icon-btn" data-action="pop-subroute" aria-label="Voltar">${ICONS.back}</button>
          <h1 class="nx-subroute-title">${esc(title)}</h1>
          ${trailing}
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
            <button type="button" class="nx-icon-btn" data-action="open-subroute" data-subroute="MARKET" aria-label="Mercado" title="Mercado">
              ${ICONS.market}
            </button>
            <button type="button" class="nx-icon-btn" data-action="open-subroute" data-subroute="SEARCH" aria-label="Pesquisa global" title="Pesquisa global">
              ${ICONS.search}
            </button>
            <button type="button" class="nx-icon-btn" data-action="open-subroute" data-subroute="MORE" aria-label="Mais opções" title="Mais">
              ${ICONS.more}
            </button>
          </div>
        </div>
      </header>
    `;
  }

  function renderMainContent() {
    const sub = currentSubRoute();
    if (sub === 'ROUTINE_ASSISTANT') return RoutineAssistant.renderRoutineAssistantScreen(app.state, app.ui);
    if (sub === 'MORE') return MoreSubroutes.renderMoreScreen(app.state);
    if (sub === 'SEARCH') return MoreSubroutes.renderGlobalSearchScreen(app.state, app.ui);
    if (sub === 'MARKET') return Market.renderMarketScreen(app.state, app.ui);
    if (sub === 'MARKET_DETAIL') return Market.renderMarketAssetDetailScreen(app.state, app.ui);
    if (sub === 'PLANNING') return Planning.renderPlanningScreen(app.state, app.ui);
    if (sub === 'CUSTOMIZE') return MoreSubroutes.renderDashboardCustomizationScreen(app.state, app.ui);
    if (sub === 'QUOTES') return MoreSubroutes.renderQuoteLibraryScreen(app.state, app.ui);
    if (sub === 'SETTINGS') return MoreSubroutes.renderSettingsScreen(app.state, app.ui);
    if (sub === 'BACKUP') return MoreSubroutes.renderBackupScreen(app.state, app.ui);
    if (sub === 'DIAGNOSTICS') return MoreSubroutes.renderDiagnosticsScreen(app.state);
    if (sub === 'WIDGETS') return MoreSubroutes.renderWidgetsHubScreen(app.state);

    if (app.ui.activeTab === 'HOME') {
      const compactCls = app.state.personalization.compactDashboardMode ? 'is-compact' : '';
      return `<main class="nx-screen-scroll ${compactCls}" data-nexus-screen="HomeScreen">${Home.renderHomeScreen(app.state, app.ui)}</main>`;
    }
    if (app.ui.activeTab === 'TASKS') {
      return `<main style="display:flex;flex-direction:column;flex:1;min-height:0" data-nexus-screen="TasksAndProjectsScreen">${Tasks.renderTasksScreen(app.state, app.ui)}</main>`;
    }
    if (app.ui.activeTab === 'AGENDA') {
      return `<main style="display:flex;flex-direction:column;flex:1;min-height:0" data-nexus-screen="AgendaScreen">${Agenda.renderAgendaScreen(app.state, app.ui)}</main>`;
    }
    if (app.ui.activeTab === 'FINANCE') {
      return `<main style="display:flex;flex-direction:column;flex:1;min-height:0" data-nexus-screen="FinanceScreen">${Finance.renderFinanceScreen(app.state, app.ui)}</main>`;
    }
    return `<main style="display:flex;flex-direction:column;flex:1;min-height:0" data-nexus-screen="EventsScreen">${Events.renderEventsScreen(app.state, app.ui)}</main>`;
  }

  function renderBottomBarAndFab() {
    if (currentSubRoute() !== null) return '';
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

  function renderQuickAddSheet() {
    return `
      <div class="nx-sheet-backdrop" data-action="close-overlays">
        <div class="nx-sheet" role="dialog" aria-modal="true" data-nexus-component="QuickAddChooserSheet" data-nexus-source="EntityEditors.kt:QuickAddChooserSheet" data-nexus-status="reproduzido" onclick="event.stopPropagation()">
          <div class="nx-sheet-handle"></div>
          <div class="nx-sheet-body">
            <div class="nx-row-between">
              <h3 class="nx-title-lg">Criar novo registro</h3>
              <button type="button" class="nx-btn-text" data-action="close-overlays">Fechar</button>
            </div>
            <button type="button" class="nx-card is-elevated is-clickable" data-action="quick-new-task">
              <div style="display:flex;align-items:center;gap:12px">
                <span class="nx-accent">${ICONS.tasks}</span>
                <div style="text-align:left">
                  <div class="nx-title-md">Nova tarefa</div>
                  <div class="nx-label-md nx-text-secondary">Prazo, subtarefas, recorrência e vínculo com projeto</div>
                </div>
              </div>
            </button>
            <button type="button" class="nx-card is-elevated is-clickable" data-action="quick-new-appointment">
              <div style="display:flex;align-items:center;gap:12px">
                <span class="nx-accent">${ICONS.agenda}</span>
                <div style="text-align:left">
                  <div class="nx-title-md">Novo compromisso</div>
                  <div class="nx-label-md nx-text-secondary">Horário, local, link e detecção de conflito</div>
                </div>
              </div>
            </button>
            <button type="button" class="nx-card is-elevated is-clickable" data-action="quick-new-tx" data-tx-type="EXPENSE">
              <div style="display:flex;align-items:center;gap:12px">
                <span class="nx-accent">${ICONS.finance}</span>
                <div style="text-align:left">
                  <div class="nx-title-md">Novo lançamento financeiro</div>
                  <div class="nx-label-md nx-text-secondary">Despesa ou receita com cartão, fatura, tarefa ou evento</div>
                </div>
              </div>
            </button>
            <button type="button" class="nx-card is-elevated is-clickable" data-action="quick-new-event">
              <div style="display:flex;align-items:center;gap:12px">
                <span class="nx-accent">${ICONS.events}</span>
                <div style="text-align:left">
                  <div class="nx-title-md">Novo evento</div>
                  <div class="nx-label-md nx-text-secondary">Data, local, ingresso e custos vinculados</div>
                </div>
              </div>
            </button>
            <button type="button" class="nx-card is-elevated is-clickable" data-action="quick-new-project">
              <div style="display:flex;align-items:center;gap:12px">
                <span class="nx-accent">${ICONS.flag}</span>
                <div style="text-align:left">
                  <div class="nx-title-md">Novo projeto</div>
                  <div class="nx-label-md nx-text-secondary">Agrupar tarefas, custos e objetivo pessoal</div>
                </div>
              </div>
            </button>
          </div>
        </div>
      </div>
    `;
  }

  function renderAuxiliaryOverlays() {
    const parts = [];

    if (app.ui.quickAddOpen) parts.push(renderQuickAddSheet());
    if (app.ui.tasksFilterSheetOpen) parts.push(Tasks.renderTasksFilterSheet(app.state, app.ui));
    if (app.ui.agendaFilterSheetOpen) parts.push(Agenda.renderAgendaFilterSheet(app.state, app.ui));

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

    if (app.ui.selectedPersonalGoalId) {
      parts.push(Planning.renderGoalDetailSheet(app.state, app.ui.selectedPersonalGoalId, app.ui));
    }

    if (app.ui.taskDraft) parts.push(EntitySheets.renderTaskSheet(app.state, app.ui.taskDraft, app.ui.taskDirty));
    if (app.ui.apptDraft) parts.push(EntitySheets.renderAppointmentSheet(app.state, app.ui.apptDraft, app.ui.apptDirty));
    if (app.ui.projectDraft) parts.push(EntitySheets.renderProjectSheet(app.state, app.ui.projectDraft, app.ui.projectDirty));
    if (app.ui.eventDraft) parts.push(EventSheet.renderEventSheet(app.state, app.ui.eventDraft, app.ui.eventDirty));
    if (app.ui.txDraft) parts.push(TransactionSheet.renderTransactionSheet(app.state, app.ui.txDraft, app.ui.txDirty));

    const extraModal = FinanceExtraSheets.renderExtraEditors(app.state, app.ui);
    if (extraModal) parts.push(extraModal);

    if (app.ui.confirmDialog) {
      const cd = app.ui.confirmDialog;
      parts.push(`
        <div class="nx-dialog-backdrop" data-action="cancel-confirm-dialog">
          <div class="nx-dialog" role="alertdialog" aria-modal="true" onclick="event.stopPropagation()">
            <h3 class="nx-title-lg">${esc(cd.title)}</h3>
            <p class="nx-body-md nx-text-secondary">${esc(cd.message)}</p>
            <div class="nx-row-end" style="flex-wrap:wrap;gap:6px">
              <button type="button" class="nx-btn-text" data-action="cancel-confirm-dialog">Voltar</button>
              ${cd.altLabel ? `<button type="button" class="nx-btn-outlined" data-action="alt-confirm-dialog">${esc(cd.altLabel)}</button>` : ''}
              <button type="button" class="nx-btn-text ${cd.danger !== false ? 'nx-btn-danger-text' : ''}" data-action="accept-confirm-dialog">${esc(cd.confirmLabel || 'Confirmar')}</button>
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
        Componentes ativos em <code>#nexus-app-shell</code> (${items.length}) · Rota: <code>${esc(currentSubRoute() || app.ui.activeTab)}</code>
      </div>
      <div class="pilot-trace-item" style="border-color:#2E4057">
        <span style="font-size:12px;font-weight:600;color:#7CFFB2">Atalhos rápidos de inspeção</span>
        <div style="display:flex;flex-wrap:wrap;gap:6px;margin-top:4px">
          <button type="button" class="pilot-btn" data-action="demo-flow-finance-tx">Ingresso Kotlin</button>
          <button type="button" class="pilot-btn" data-action="open-routine-assistant" data-routine-tab="0">Briefing</button>
          <button type="button" class="pilot-btn" data-action="open-subroute" data-subroute="MARKET">Mercado</button>
          <button type="button" class="pilot-btn" data-action="open-subroute" data-subroute="PLANNING">Objetivos</button>
          <button type="button" class="pilot-btn" data-action="open-subroute" data-subroute="DIAGNOSTICS">Diagnóstico</button>
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

    app.state.settings.darkTheme = app.ui.theme === 'dark';

    shell.setAttribute('data-nexus-theme', app.ui.theme);
    shell.classList.toggle('viewport-tablet-portrait', app.ui.viewport === 'tablet-portrait');
    shell.classList.toggle('viewport-tablet-landscape', app.ui.viewport === 'tablet-landscape');
    shell.classList.toggle('viewport-fluid', app.ui.viewport === 'fluid');
    shell.classList.toggle('is-wide', app.ui.viewport !== 'mobile');
    shell.classList.toggle('is-two-pane', app.ui.viewport === 'tablet-landscape' || app.ui.viewport === 'fluid');

    shell.innerHTML = `
      ${renderTopBar()}
      ${renderMainContent()}
      ${renderBottomBarAndFab()}
      ${renderAuxiliaryOverlays()}
    `;

    syncLabBarUi();
    renderTracePanel();

    if (app.ui.highlightedStatementId && !currentSubRoute() && app.ui.activeTab === 'FINANCE' && app.ui.financeTab === 'CARDS') {
      const el = document.getElementById(`stmt-anchor-${app.ui.highlightedStatementId}`);
      if (el && typeof el.scrollIntoView === 'function') {
        el.scrollIntoView({ behavior: 'smooth', block: 'center' });
      }
    }
  }

  // ==================== PERSISTENCE / MUTATION HELPERS ====================

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
      appendAudit(app.state, 'CRIAÇÃO', 'Lançamento', `Registrado "${entity.description}" (${formatBrl(entity.amountCents)}).`);
      showSnackbar('Lançamento criado e vinculado ao modelo relacional.');
    } else {
      const idx = app.state.transactions.findIndex((t) => t.id === entity.id);
      if (idx >= 0) app.state.transactions[idx] = entity;
      appendAudit(app.state, 'ATUALIZAÇÃO', 'Lançamento', `Atualizado "${entity.description}" (${formatBrl(entity.amountCents)}).`);
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
      appendAudit(app.state, 'CRIAÇÃO', 'Evento', `Criado evento "${entity.name}".`);
      showSnackbar('Evento criado.');
    } else {
      const idx = app.state.events.findIndex((e) => e.id === entity.id);
      if (idx >= 0) app.state.events[idx] = entity;
      appendAudit(app.state, 'ATUALIZAÇÃO', 'Evento', `Atualizado evento "${entity.name}".`);
      showSnackbar('Evento atualizado.');
    }
    app.ui.eventDraft = null;
    app.ui.eventDirty = false;
    render();
  }

  function buildDraftRecRuleSig(d) {
    if (!d || !d.recurrenceEnabled) return 'NONE';
    return [
      d.recurrenceFrequency || 'WEEKLY',
      Number(d.customIntervalDays) || 7,
      Number(d.weekdaysMask) || 0,
      d.recurrenceStartDateIso || '',
      d.recurrenceEndMode || 'NONE',
      d.recurrenceEndMode === 'DATE' ? (d.recurrenceEndDateIso || '') : '',
      d.recurrenceEndMode === 'COUNT' ? (Number(d.recurrenceMaxOccurrences) || 10) : ''
    ].join('|');
  }

  function buildDraftTemplateSig(d) {
    return [
      d.title || '',
      d.description || '',
      d.priority || 'NORMAL',
      d.projectId || '',
      d.categoryId || '',
      d.eventId || '',
      d.goalId || '',
      Boolean(d.hasDueTime),
      d.dueTime || '14:00',
      d.notes || ''
    ].join('|');
  }

  function buildTaskFromDraft(d) {
    const existing = d.id ? app.state.tasks.find((t) => t.id === d.id) : null;
    const nowStamp = `${app.state.todayIso}T${app.state.nowTime || '09:30'}`;
    const reminderVal = d.reminderMinutesBefore === '-1'
      ? -1
      : d.reminderMinutesBefore !== '' && d.reminderMinutesBefore != null
        ? Number(d.reminderMinutesBefore)
        : null;
    const endMode = d.recurrenceEndMode || 'NONE';
    const recStart = d.recurrenceStartDateIso || d.dueDateIso || app.state.todayIso;
    const customDays = Math.max(1, Number(d.customIntervalDays) || 7);
    const weekdaysMask = Number(d.weekdaysMask) || (recStart ? weekdayBit(parseIsoDate(recStart).getDay()) : 0);

    return {
      id: d.id || `task_${Date.now()}`,
      title: d.title.trim(),
      description: d.description || '',
      status: d.status || 'NOT_STARTED',
      priority: d.priority || 'NORMAL',
      projectId: d.projectId || null,
      categoryId: d.categoryId || null,
      eventId: d.eventId || null,
      goalId: d.goalId || null,
      dueDateIso: d.dueDateIso || null,
      hasDueTime: Boolean(d.hasDueTime && d.dueDateIso),
      dueTime: d.hasDueTime ? (d.dueTime || '14:00') : null,
      reminderMinutesBefore: reminderVal,
      recurrenceId: d.recurrenceEnabled ? (d.recurrenceId || null) : null,
      occurrenceDateIso: d.recurrenceEnabled ? (d.occurrenceDateIso || d.dueDateIso || recStart) : null,
      recurrence: d.recurrenceEnabled ? {
        frequency: d.recurrenceFrequency || 'WEEKLY',
        startDateIso: recStart,
        endMode,
        endDateIso: endMode === 'DATE' ? (d.recurrenceEndDateIso || null) : null,
        maxOccurrences: endMode === 'COUNT' ? Math.max(1, Number(d.recurrenceMaxOccurrences) || 10) : null,
        customIntervalDays: customDays,
        weekdaysMask
      } : null,
      subtasks: (d.subtasks || []).map((s) => ({ ...s })),
      updatedAtIso: nowStamp,
      completedAtIso: d.status === 'COMPLETED' ? (existing?.completedAtIso || nowStamp) : null,
      notes: d.notes || ''
    };
  }

  function saveTaskFromDraft(scopeChoice = null) {
    const d = app.ui.taskDraft;
    if (!d) return false;
    if (!d.title.trim()) {
      showSnackbar('Informe o título da tarefa.');
      return false;
    }

    if (!d.isNew && d.recurrenceId && !scopeChoice) {
      const ruleChanged = buildDraftRecRuleSig(d) !== d.initRecRuleSig;
      const templateChanged = buildDraftTemplateSig(d) !== d.initTemplateSig;
      if (ruleChanged) {
        app.ui.confirmDialog = {
          type: 'CONFIRM_RECURRENCE_RULE_CHANGE',
          title: 'Alterar regra da série?',
          message: d.recurrenceEnabled
            ? 'A regra de repetição foi modificada e será aplicada à série recorrente.'
            : 'A recorrência automática será encerrada para esta série.',
          confirmLabel: 'Aplicar na série',
          danger: false
        };
        render();
        return false;
      }
      if (templateChanged) {
        app.ui.confirmDialog = {
          type: 'SAVE_TASK_SCOPE',
          title: 'Aplicar alterações em...',
          message: 'Esta tarefa faz parte de uma série recorrente. Deseja atualizar só esta ocorrência ou toda a série?',
          altLabel: 'Só esta ocorrência',
          confirmLabel: 'Toda a série',
          danger: false
        };
        render();
        return false;
      }
    }

    const entity = buildTaskFromDraft(d);
    app.state.taskRecurrences = app.state.taskRecurrences || [];
    app.state.recurrenceExceptions = app.state.recurrenceExceptions || [];

    if (d.recurrenceEnabled) {
      const recStart = entity.recurrence.startDateIso || entity.dueDateIso || app.state.todayIso;
      if (!entity.recurrenceId) {
        const newRecId = `rec_${Date.now()}`;
        entity.recurrenceId = newRecId;
        entity.occurrenceDateIso = entity.occurrenceDateIso || entity.dueDateIso || recStart;
        app.state.taskRecurrences.push({
          id: newRecId,
          templateTaskId: entity.id,
          title: entity.title,
          description: entity.description,
          projectId: entity.projectId,
          categoryId: entity.categoryId,
          eventId: entity.eventId,
          goalId: entity.goalId,
          priority: entity.priority,
          hasDueTime: entity.hasDueTime,
          dueTime: entity.dueTime,
          notes: entity.notes,
          frequency: entity.recurrence.frequency,
          customIntervalDays: entity.recurrence.customIntervalDays,
          weekdaysMask: entity.recurrence.weekdaysMask,
          startDateIso: recStart,
          endDateIso: entity.recurrence.endDateIso,
          maxOccurrences: entity.recurrence.maxOccurrences,
          isActive: true
        });
      } else {
        const rec = app.state.taskRecurrences.find((r) => r.id === entity.recurrenceId);
        if (rec && scopeChoice !== 'THIS_OCCURRENCE_ONLY') {
          rec.title = entity.title;
          rec.description = entity.description;
          rec.projectId = entity.projectId;
          rec.categoryId = entity.categoryId;
          rec.eventId = entity.eventId;
          rec.goalId = entity.goalId;
          rec.priority = entity.priority;
          rec.hasDueTime = entity.hasDueTime;
          rec.dueTime = entity.dueTime;
          rec.notes = entity.notes;
          rec.frequency = entity.recurrence.frequency;
          rec.customIntervalDays = entity.recurrence.customIntervalDays;
          rec.weekdaysMask = entity.recurrence.weekdaysMask;
          rec.startDateIso = recStart;
          rec.endDateIso = entity.recurrence.endDateIso;
          rec.maxOccurrences = entity.recurrence.maxOccurrences;
          rec.isActive = true;
          app.state.tasks.forEach((other) => {
            if (other.id !== entity.id && other.recurrenceId === rec.id && other.status !== 'COMPLETED' && other.status !== 'CANCELLED') {
              other.title = entity.title;
              other.description = entity.description;
              other.priority = entity.priority;
              other.projectId = entity.projectId;
              other.categoryId = entity.categoryId;
              other.eventId = entity.eventId;
              other.goalId = entity.goalId;
              other.hasDueTime = entity.hasDueTime;
              other.dueTime = entity.dueTime;
              other.notes = entity.notes;
              other.recurrence = { ...entity.recurrence };
            }
          });
        } else if (scopeChoice === 'THIS_OCCURRENCE_ONLY' && entity.occurrenceDateIso) {
          const existsEx = app.state.recurrenceExceptions.some((ex) => ex.recurrenceId === entity.recurrenceId && ex.occurrenceDateIso === entity.occurrenceDateIso);
          if (!existsEx) {
            app.state.recurrenceExceptions.push({
              id: `rex_${Date.now()}`,
              recurrenceId: entity.recurrenceId,
              occurrenceDateIso: entity.occurrenceDateIso,
              exceptionType: 'OVERRIDE'
            });
          }
        }
      }
    } else if (d.recurrenceId) {
      const rec = app.state.taskRecurrences.find((r) => r.id === d.recurrenceId);
      if (rec) rec.isActive = false;
      entity.recurrenceId = null;
      entity.recurrence = null;
    }

    if (d.isNew) {
      app.state.tasks.unshift(entity);
      appendAudit(app.state, 'CRIAÇÃO', 'Tarefa', `Criada tarefa "${entity.title}".`);
      if (entity.status === 'COMPLETED' && entity.recurrenceId) {
        materializeNextOccurrenceIfNeeded(app.state, entity);
      }
      showSnackbar('Tarefa criada.');
    } else {
      const idx = app.state.tasks.findIndex((t) => t.id === entity.id);
      if (idx >= 0) app.state.tasks[idx] = entity;
      if (entity.status === 'COMPLETED' && entity.recurrenceId) {
        materializeNextOccurrenceIfNeeded(app.state, entity);
      }
      appendAudit(app.state, 'ATUALIZAÇÃO', 'Tarefa', `Atualizada tarefa "${entity.title}".`);
      showSnackbar('Tarefa salva.');
    }
    app.ui.taskDraft = null;
    app.ui.taskDirty = false;
    render();
    return entity;
  }

  function toggleTaskCompletion(task) {
    if (!task) return;
    const wasDone = task.status === 'COMPLETED';
    const nowStamp = `${app.state.todayIso}T${app.state.nowTime || '09:30'}`;
    task.status = wasDone ? 'IN_PROGRESS' : 'COMPLETED';
    task.updatedAtIso = nowStamp;
    task.completedAtIso = wasDone ? null : nowStamp;
    let nextCreated = null;
    if (!wasDone && task.recurrenceId) {
      nextCreated = materializeNextOccurrenceIfNeeded(app.state, task);
    }
    appendAudit(app.state, 'ATUALIZAÇÃO', 'Tarefa', `Tarefa "${task.title}" marcada como ${LABELS.TaskStatus[task.status]}.`);
    showSnackbar(
      wasDone
        ? 'Tarefa reaberta.'
        : nextCreated
          ? `Tarefa concluída · próxima ocorrência gerada (${formatDateBr(nextCreated.dueDateIso).slice(0, 5)}).`
          : 'Tarefa concluída.'
    );
  }

  function saveApptFromDraft() {
    const d = app.ui.apptDraft;
    if (!d) return;
    if (!d.title.trim()) {
      showSnackbar('Informe o título do compromisso.');
      return;
    }
    const entity = {
      id: d.id || `appt_${Date.now()}`,
      title: d.title.trim(),
      description: d.description || '',
      dateIso: d.dateIso || app.state.todayIso,
      endDateIso: d.endDateIso || d.dateIso || app.state.todayIso,
      startTime: d.isAllDay ? '00:00' : (d.startTime || '09:00'),
      endTime: d.isAllDay ? '23:59' : (d.endTime || '10:00'),
      isAllDay: Boolean(d.isAllDay),
      location: d.location || '',
      link: d.link || '',
      categoryId: d.categoryId || null,
      eventId: d.eventId || null,
      reminderMinutesBefore: d.reminderMinutesBefore !== '' ? Number(d.reminderMinutesBefore) : null
    };
    if (d.isNew) {
      app.state.appointments.push(entity);
      appendAudit(app.state, 'CRIAÇÃO', 'Compromisso', `Agendado "${entity.title}" em ${entity.dateIso}.`);
      showSnackbar('Compromisso criado.');
    } else {
      const idx = app.state.appointments.findIndex((a) => a.id === entity.id);
      if (idx >= 0) app.state.appointments[idx] = entity;
      appendAudit(app.state, 'ATUALIZAÇÃO', 'Compromisso', `Atualizado "${entity.title}".`);
      showSnackbar('Compromisso atualizado.');
    }
    app.ui.apptDraft = null;
    app.ui.apptDirty = false;
    render();
  }

  function saveProjectFromDraft() {
    const d = app.ui.projectDraft;
    if (!d) return;
    if (!d.name.trim()) {
      showSnackbar('Informe o nome do projeto.');
      return;
    }
    const entity = {
      id: d.id || `proj_${Date.now()}`,
      name: d.name.trim(),
      description: d.description || '',
      status: d.status || 'ACTIVE',
      priority: d.priority || 'NORMAL',
      deadlineDateIso: d.deadlineDateIso || null,
      goalId: d.goalId || null
    };
    if (d.isNew) {
      app.state.projects.push(entity);
      appendAudit(app.state, 'CRIAÇÃO', 'Projeto', `Criado projeto "${entity.name}".`);
      showSnackbar('Projeto criado.');
    } else {
      const idx = app.state.projects.findIndex((p) => p.id === entity.id);
      if (idx >= 0) app.state.projects[idx] = entity;
      appendAudit(app.state, 'ATUALIZAÇÃO', 'Projeto', `Atualizado projeto "${entity.name}".`);
      showSnackbar('Projeto salvo.');
    }
    app.ui.projectDraft = null;
    app.ui.projectDirty = false;
    render();
  }

  function attemptCloseSheet(dirtyKey, draftKey, confirmType, label) {
    if (app.ui[dirtyKey]) {
      app.ui.confirmDialog = {
        type: confirmType,
        title: 'Descartar alterações?',
        message: `Há modificações não salvas neste ${label}.`,
        confirmLabel: 'Descartar'
      };
      render();
      return;
    }
    app.ui[draftKey] = null;
    app.ui[dirtyKey] = false;
    render();
  }

  function validateAndPreviewBackupJson(rawText) {
    let parsed;
    try {
      parsed = JSON.parse(rawText);
    } catch (_e) {
      showSnackbar('JSON inválido. Verifique a sintaxe do backup.');
      return;
    }
    if (!parsed || typeof parsed !== 'object' || !Array.isArray(parsed.tasks) || !Array.isArray(parsed.transactions)) {
      showSnackbar('Arquivo incompatível com o esquema NEXUS v7.');
      return;
    }
    const marketCfg = parsed.marketConfig || {};
    const totalRecords =
      (parsed.tasks?.length || 0) +
      (parsed.projects?.length || 0) +
      (parsed.personalGoals?.length || 0) +
      (parsed.appointments?.length || 0) +
      (parsed.events?.length || 0) +
      (parsed.transactions?.length || 0) +
      (parsed.creditCards?.length || 0) +
      (parsed.loans?.length || 0);

    app.ui.backupPendingPreview = {
      formatVersion: parsed.formatVersion || 7,
      exportedAt: parsed.exportedAt || '--',
      totalRecords,
      tasks: parsed.tasks?.length || 0,
      projects: parsed.projects?.length || 0,
      personalGoals: parsed.personalGoals?.length || 0,
      appointments: parsed.appointments?.length || 0,
      events: parsed.events?.length || 0,
      transactions: parsed.transactions?.length || 0,
      creditCards: parsed.creditCards?.length || 0,
      loans: parsed.loans?.length || 0,
      marketFavorites: Array.isArray(marketCfg.favorites) ? marketCfg.favorites.length : 0,
      marketAlerts: Array.isArray(marketCfg.alerts) ? marketCfg.alerts.length : 0,
      payload: parsed
    };
    render();
  }

  function applyBackupRestore(preview) {
    const p = preview.payload;
    const base = createInitialState();
    app.state = {
      ...base,
      settings: p.settings || base.settings,
      personalization: p.personalizationSettings || base.personalization,
      blocks: Array.isArray(p.dashboardBlocks) ? p.dashboardBlocks : base.blocks,
      quotes: Array.isArray(p.quotes) ? p.quotes : base.quotes,
      categories: Array.isArray(p.categories) ? p.categories : base.categories,
      personalGoals: Array.isArray(p.personalGoals) ? p.personalGoals : base.personalGoals,
      projects: Array.isArray(p.projects) ? p.projects : base.projects,
      events: Array.isArray(p.events) ? p.events : base.events,
      tasks: Array.isArray(p.tasks) ? p.tasks : base.tasks,
      taskRecurrences: Array.isArray(p.taskRecurrences)
        ? p.taskRecurrences
        : (Array.isArray(p.tasks) ? p.tasks.filter((t) => t.recurrence).map((t) => ({
            id: t.recurrenceId || `rec_${t.id}`,
            templateTaskId: t.id,
            title: t.title,
            description: t.description || '',
            projectId: t.projectId || null,
            categoryId: t.categoryId || null,
            eventId: t.eventId || null,
            goalId: t.goalId || null,
            priority: t.priority || 'NORMAL',
            hasDueTime: Boolean(t.hasDueTime),
            dueTime: t.dueTime || null,
            notes: t.notes || '',
            frequency: t.recurrence.frequency || 'WEEKLY',
            customIntervalDays: t.recurrence.customIntervalDays || 7,
            weekdaysMask: t.recurrence.weekdaysMask || 0,
            startDateIso: t.recurrence.startDateIso || t.dueDateIso || base.todayIso,
            endDateIso: t.recurrence.endDateIso || null,
            maxOccurrences: t.recurrence.maxOccurrences || null,
            isActive: true
          })) : base.taskRecurrences),
      recurrenceExceptions: Array.isArray(p.recurrenceExceptions) ? p.recurrenceExceptions : [],
      appointments: Array.isArray(p.appointments) ? p.appointments : base.appointments,
      creditCards: Array.isArray(p.creditCards) ? p.creditCards : base.creditCards,
      statements: Array.isArray(p.statements) ? p.statements : base.statements,
      installmentPurchases: Array.isArray(p.installmentPurchases) ? p.installmentPurchases : base.installmentPurchases,
      recurringRules: Array.isArray(p.recurringRules) ? p.recurringRules : base.recurringRules,
      transactions: Array.isArray(p.transactions) ? p.transactions : base.transactions,
      loans: Array.isArray(p.loans) ? p.loans : base.loans,
      budgets: Array.isArray(p.budgets) ? p.budgets : base.budgets,
      financialGoals: Array.isArray(p.financialGoals) ? p.financialGoals : base.financialGoals,
      reminders: Array.isArray(p.reminders) ? p.reminders : base.reminders,
      routineSettings: p.routineSettings || base.routineSettings,
      briefingState: {
        ...base.briefingState,
        history: Array.isArray(p.briefingHistory) ? p.briefingHistory : base.briefingState.history
      },
      auditHistory: Array.isArray(p.auditHistory) ? p.auditHistory : base.auditHistory,
      errorLogs: Array.isArray(p.errorLogs) ? p.errorLogs : base.errorLogs,
      marketFavorites: Array.isArray(p.marketConfig?.favorites) ? p.marketConfig.favorites : base.marketFavorites,
      marketAlerts: Array.isArray(p.marketConfig?.alerts) ? p.marketConfig.alerts : base.marketAlerts
    };
    app.ui.theme = app.state.settings.darkTheme ? 'dark' : 'light';
    app.ui.backupLastSummary = `Restaurado v${preview.formatVersion}: ${preview.totalRecords} registros principais + ${preview.marketFavorites} favorito(s) e ${preview.marketAlerts} alerta(s) de mercado.`;
    app.ui.backupPendingPreview = null;
    appendAudit(app.state, 'RESTAURAÇÃO', 'Backup v7', app.ui.backupLastSummary);
    showSnackbar('Backup restaurado com sucesso.');
  }

  function setByPath(root, path, value) {
    const parts = path.split('.');
    let obj = root;
    for (let i = 0; i < parts.length - 1; i++) {
      if (!obj[parts[i]]) return false;
      obj = obj[parts[i]];
    }
    obj[parts[parts.length - 1]] = value;
    return true;
  }

  function getByPath(root, path) {
    return path.split('.').reduce((acc, k) => (acc ? acc[k] : undefined), root);
  }

  // ==================== GLOBAL CLICK DELEGATION ====================
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
      // Lab bar
      case 'toggle-trace':
        app.ui.traceOpen = !app.ui.traceOpen;
        render();
        break;
      case 'reset-pilot-data':
        app.state = createInitialState();
        app.ui = createInitialUi();
        showSnackbar('Dados de Gabriel restaurados ao estado inicial.');
        break;

      // Navegação principal e subrotas
      case 'select-bottom-tab':
        clearSubRoutes();
        app.ui.activeTab = target.getAttribute('data-tab') || 'HOME';
        app.ui.highlightedStatementId = null;
        render();
        break;
      case 'open-home-tab':
        clearSubRoutes();
        app.ui.activeTab = 'HOME';
        render();
        break;
      case 'open-tasks-tab':
        clearSubRoutes();
        app.ui.activeTab = 'TASKS';
        render();
        break;
      case 'open-tasks-overdue':
        clearSubRoutes();
        app.ui.activeTab = 'TASKS';
        app.ui.tasksFilter.tabMode = 'OVERDUE';
        render();
        break;
      case 'open-agenda-tab':
        clearSubRoutes();
        app.ui.activeTab = 'AGENDA';
        render();
        break;
      case 'open-events-tab':
        clearSubRoutes();
        app.ui.activeTab = 'EVENTS';
        render();
        break;
      case 'open-finance-tab':
        clearSubRoutes();
        app.ui.activeTab = 'FINANCE';
        app.ui.financeTab = target.getAttribute('data-finance-tab') || 'OVERVIEW';
        app.ui.highlightedStatementId = null;
        render();
        break;
      case 'open-finance-overdue':
        clearSubRoutes();
        app.ui.activeTab = 'FINANCE';
        app.ui.financeTab = 'TRANSACTIONS';
        app.ui.financeFilter = { monthScope: 'ALL', type: 'EXPENSE', status: 'OVERDUE', categoryId: null, paymentMethod: null };
        render();
        break;
      case 'open-subroute': {
        const sub = target.getAttribute('data-subroute');
        if (sub) {
          pushSubRoute(sub);
          render();
        }
        break;
      }
      case 'pop-subroute':
        popSubRoute();
        render();
        break;
      case 'open-routine-assistant':
        app.ui.quickAddOpen = false;
        app.ui.routineTabIndex = Number(target.getAttribute('data-routine-tab') || 0);
        pushSubRoute('ROUTINE_ASSISTANT');
        render();
        break;
      case 'select-routine-tab':
        app.ui.routineTabIndex = Number(target.getAttribute('data-routine-tab') || 0);
        render();
        break;
      case 'clear-global-search':
        app.ui.globalSearchQuery = '';
        render();
        break;
      case 'apply-search-suggestion':
        app.ui.globalSearchQuery = target.getAttribute('data-query') || '';
        render();
        break;

      // QuickAdd & overlays básicos
      case 'open-quick-add':
        app.ui.quickAddOpen = true;
        render();
        break;
      case 'close-overlays':
        app.ui.quickAddOpen = false;
        app.ui.tasksFilterSheetOpen = false;
        app.ui.agendaFilterSheetOpen = false;
        app.ui.balanceDialogOpen = false;
        app.ui.financeFilterSheetOpen = false;
        app.ui.csvDialogOpen = false;
        render();
        break;
      case 'close-extra-editors':
        clearExtraEditors();
        render();
        break;

      // Tarefas & Projetos
      case 'select-tasks-tab':
        app.ui.tasksFilter.tabMode = target.getAttribute('data-tasks-tab') || 'TODAY';
        render();
        break;
      case 'open-tasks-filter-sheet':
        app.ui.tasksFilterSheetOpen = true;
        render();
        break;
      case 'set-task-filter': {
        const key = target.getAttribute('data-key');
        const val = target.getAttribute('data-value') || null;
        app.ui.tasksFilter[key] = val;
        render();
        break;
      }
      case 'clear-tasks-filters':
        app.ui.tasksFilter.statusFilter = null;
        app.ui.tasksFilter.priorityFilter = null;
        app.ui.tasksFilter.projectFilterId = null;
        render();
        break;
      case 'toggle-task': {
        const tId = target.getAttribute('data-task-id');
        const task = app.state.tasks.find((t) => t.id === tId);
        if (task) toggleTaskCompletion(task);
        break;
      }
      case 'toggle-virtual-task': {
        const recId = target.getAttribute('data-recurrence-id');
        const occDateIso = target.getAttribute('data-occurrence-date-iso');
        if (recId && occDateIso) {
          const mat = openOrMaterializeRecurringOccurrence(app.state, recId, occDateIso);
          if (mat) toggleTaskCompletion(mat);
        }
        break;
      }
      case 'task-sheet-toggle-complete': {
        const d = app.ui.taskDraft;
        if (d && !d.isNew && d.id) {
          const task = app.state.tasks.find((t) => t.id === d.id);
          app.ui.taskDraft = null;
          app.ui.taskDirty = false;
          if (task) toggleTaskCompletion(task);
        }
        break;
      }
      case 'postpone-task': {
        const tId = target.getAttribute('data-task-id');
        const task = app.state.tasks.find((t) => t.id === tId);
        if (task) {
          const tomorrowIso = addDaysIso(app.state.todayIso, 1);
          task.dueDateIso = tomorrowIso;
          showSnackbar(`Tarefa adiada para amanhã (${formatDateBr(tomorrowIso).slice(0, 5)}).`);
        }
        break;
      }
      case 'open-task': {
        const task = app.state.tasks.find((t) => t.id === target.getAttribute('data-task-id'));
        if (task) {
          app.ui.taskDraft = EntitySheets.createDraftFromTask(task, {}, app.state);
          app.ui.taskDirty = false;
          render();
        }
        break;
      }
      case 'open-event-from-task': {
        const evId = target.getAttribute('data-event-id');
        const ev = app.state.events.find((x) => x.id === evId);
        if (ev) {
          app.ui.taskDraft = null;
          app.ui.taskDirty = false;
          app.ui.eventDraft = EventSheet.createDraftFromEvent(ev, app.state);
          app.ui.eventDirty = false;
          render();
        }
        break;
      }
      case 'new-task':
      case 'quick-new-task':
        app.ui.quickAddOpen = false;
        app.ui.taskDraft = EntitySheets.createDraftFromTask(null, {}, app.state);
        app.ui.taskDirty = false;
        render();
        break;
      case 'add-event-task': {
        const evId = target.getAttribute('data-event-id');
        const ev = app.state.events.find((x) => x.id === evId);
        app.ui.eventDraft = null;
        app.ui.eventDirty = false;
        app.ui.taskDraft = EntitySheets.createDraftFromTask(null, {
          eventId: evId,
          categoryId: ev?.categoryId || 'cat_eventos',
          dueDateIso: ev?.startDateIso || app.state.todayIso
        }, app.state);
        app.ui.taskDirty = false;
        render();
        break;
      }
      case 'task-toggle-field': {
        const field = target.getAttribute('data-field');
        if (app.ui.taskDraft && field) {
          app.ui.taskDraft[field] = !app.ui.taskDraft[field];
          if (field !== 'recurrenceExpanded') app.ui.taskDirty = true;
          render();
        }
        break;
      }
      case 'task-toggle-weekday': {
        const dow = Number(target.getAttribute('data-dow'));
        if (app.ui.taskDraft && !Number.isNaN(dow)) {
          const bit = weekdayBit(dow);
          app.ui.taskDraft.weekdaysMask = Number(app.ui.taskDraft.weekdaysMask || 0) ^ bit;
          app.ui.taskDirty = true;
          render();
        }
        break;
      }
      case 'task-toggle-subtask': {
        const subId = target.getAttribute('data-sub-id');
        if (app.ui.taskDraft) {
          const st = app.ui.taskDraft.subtasks.find((s) => s.id === subId);
          if (st) st.isDone = !st.isDone;
          app.ui.taskDirty = true;
          render();
        }
        break;
      }
      case 'task-delete-subtask': {
        const subId = target.getAttribute('data-sub-id');
        if (app.ui.taskDraft) {
          app.ui.taskDraft.subtasks = app.ui.taskDraft.subtasks.filter((s) => s.id !== subId);
          app.ui.taskDirty = true;
          render();
        }
        break;
      }
      case 'task-add-subtask': {
        const d = app.ui.taskDraft;
        if (d) {
          const txt = (d.newSubtaskTitle || '').trim();
          if (!txt) {
            showSnackbar('Digite o título do item da checklist.');
          } else {
            d.subtasks.push({ id: `st_${Date.now()}`, title: txt, isDone: false });
            d.newSubtaskTitle = '';
            app.ui.taskDirty = true;
            render();
          }
        }
        break;
      }
      case 'task-register-expense': {
        const d = app.ui.taskDraft;
        if (!d) break;
        let savedTask = null;
        if (d.isNew) {
          if (!d.title.trim()) {
            showSnackbar('Salve ou preencha o título da tarefa antes de registrar um gasto.');
            break;
          }
          savedTask = buildTaskFromDraft(d);
          app.state.tasks.unshift(savedTask);
        } else {
          savedTask = app.state.tasks.find((t) => t.id === d.id) || buildTaskFromDraft(d);
        }
        app.ui.taskDraft = null;
        app.ui.taskDirty = false;
        const suggestedCat = suggestExpenseCategoryIdForTask(app.state, savedTask.categoryId);
        app.ui.txDraft = TransactionSheet.createDraftFromTx(null, {
          type: 'EXPENSE',
          description: `Gasto: ${savedTask.title}`,
          taskId: savedTask.id,
          projectId: savedTask.projectId || '',
          eventId: savedTask.eventId || '',
          categoryId: suggestedCat || ''
        }, app.state);
        app.ui.txDirty = false;
        render();
        break;
      }
      case 'save-task-sheet':
        saveTaskFromDraft();
        break;
      case 'close-task-sheet':
      case 'close-task-sheet-backdrop':
        attemptCloseSheet('taskDirty', 'taskDraft', 'DISCARD_TASK', 'rascunho de tarefa');
        break;
      case 'request-delete-task':
        if (app.ui.taskDraft?.recurrenceId) {
          app.ui.confirmDialog = {
            type: 'DELETE_TASK_RECURRING',
            title: 'Excluir tarefa recorrente?',
            message: 'Escolha excluir apenas esta ocorrência ou encerrar toda a série.',
            altLabel: 'Só esta ocorrência',
            confirmLabel: 'Toda a série',
            danger: true
          };
        } else {
          app.ui.confirmDialog = {
            type: 'DELETE_TASK',
            title: 'Excluir tarefa?',
            message: 'A tarefa e suas subtarefas serão removidas (gastos vinculados ficam preservados).',
            confirmLabel: 'Excluir',
            danger: true
          };
        }
        render();
        break;

      // Projetos
      case 'new-project':
      case 'quick-new-project':
        app.ui.quickAddOpen = false;
        app.ui.projectDraft = EntitySheets.createDraftFromProject(null, {});
        app.ui.projectDirty = false;
        render();
        break;
      case 'open-project': {
        const proj = app.state.projects.find((p) => p.id === target.getAttribute('data-project-id'));
        if (proj) {
          app.ui.projectDraft = EntitySheets.createDraftFromProject(proj, {});
          app.ui.projectDirty = false;
          render();
        }
        break;
      }
      case 'save-project-sheet':
        saveProjectFromDraft();
        break;
      case 'close-project-sheet':
      case 'close-project-sheet-backdrop':
        attemptCloseSheet('projectDirty', 'projectDraft', 'DISCARD_PROJECT', 'projeto');
        break;
      case 'request-delete-project':
        app.ui.confirmDialog = {
          type: 'DELETE_PROJECT',
          title: 'Excluir projeto?',
          message: 'O projeto será removido; tarefas e gastos associados permanecem sem projeto.',
          confirmLabel: 'Excluir'
        };
        render();
        break;

      // Agenda & Compromissos
      case 'agenda-set-view':
        app.ui.agendaFilter.viewMode = target.getAttribute('data-view-mode') || 'WEEK';
        render();
        break;
      case 'agenda-nav': {
        const dir = Number(target.getAttribute('data-dir') || 0);
        const vm = app.ui.agendaFilter.viewMode;
        const cur = app.ui.agendaFilter.selectedDateIso;
        if (vm === 'MONTH') {
          const nextM = shiftMonthIso(cur.slice(0, 7), dir);
          app.ui.agendaFilter.selectedDateIso = `${nextM}-01`;
        } else if (vm === 'WEEK') {
          app.ui.agendaFilter.selectedDateIso = addDaysIso(cur, dir * 7);
        } else {
          app.ui.agendaFilter.selectedDateIso = addDaysIso(cur, dir);
        }
        render();
        break;
      }
      case 'agenda-go-today':
        app.ui.agendaFilter.selectedDateIso = app.state.todayIso;
        render();
        break;
      case 'agenda-select-date':
        app.ui.agendaFilter.selectedDateIso = target.getAttribute('data-date-iso') || app.state.todayIso;
        render();
        break;
      case 'agenda-toggle-calendar':
        app.ui.agendaFilter.isCalendarExpanded = !app.ui.agendaFilter.isCalendarExpanded;
        render();
        break;
      case 'open-agenda-filter-sheet':
        app.ui.agendaFilterSheetOpen = true;
        render();
        break;
      case 'set-agenda-filter': {
        const key = target.getAttribute('data-key');
        const val = target.getAttribute('data-value') || null;
        app.ui.agendaFilter[key] = val;
        if (key === 'kindFilter' && app.ui.agendaFilter.categoryFilterId) {
          const allowed = contextualCategoriesForAgendaKind(app.state, val);
          if (!allowed.some((c) => c.id === app.ui.agendaFilter.categoryFilterId)) {
            app.ui.agendaFilter.categoryFilterId = null;
          }
        }
        render();
        break;
      }
      case 'toggle-agenda-show-paid':
        app.ui.agendaFilter.showPaid = !app.ui.agendaFilter.showPaid;
        render();
        break;
      case 'clear-agenda-filters':
        app.ui.agendaFilter.kindFilter = null;
        app.ui.agendaFilter.categoryFilterId = null;
        app.ui.agendaFilter.showPaid = false;
        render();
        break;
      case 'new-appointment':
      case 'quick-new-appointment': {
        app.ui.quickAddOpen = false;
        const dIso = target.getAttribute('data-date-iso') || app.ui.agendaFilter.selectedDateIso || app.state.todayIso;
        app.ui.apptDraft = EntitySheets.createDraftFromAppointment(null, { dateIso: dIso }, app.state);
        app.ui.apptDirty = false;
        render();
        break;
      }
      case 'open-appointment': {
        const appt = app.state.appointments.find((a) => a.id === target.getAttribute('data-appointment-id'));
        if (appt) {
          app.ui.apptDraft = EntitySheets.createDraftFromAppointment(appt, {}, app.state);
          app.ui.apptDirty = false;
          render();
        }
        break;
      }
      case 'open-agenda-occurrence': {
        const sType = target.getAttribute('data-source-type');
        const sId = target.getAttribute('data-source-id');
        if (sType === 'APPOINTMENT') {
          const appt = app.state.appointments.find((a) => a.id === sId);
          if (appt) {
            app.ui.apptDraft = EntitySheets.createDraftFromAppointment(appt, {}, app.state);
            app.ui.apptDirty = false;
            render();
          }
        } else if (sType === 'TASK') {
          const isVirtual = target.getAttribute('data-is-virtual') === 'true';
          let task = null;
          if (isVirtual) {
            const recId = target.getAttribute('data-recurrence-id');
            const occDateIso = target.getAttribute('data-occurrence-date-iso');
            if (recId && occDateIso) {
              task = openOrMaterializeRecurringOccurrence(app.state, recId, occDateIso);
            }
          } else {
            task = app.state.tasks.find((t) => t.id === sId);
          }
          if (task) {
            app.ui.taskDraft = EntitySheets.createDraftFromTask(task, {}, app.state);
            app.ui.taskDirty = false;
            render();
          }
        } else if (sType === 'EVENT') {
          const ev = app.state.events.find((x) => x.id === sId);
          if (ev) {
            app.ui.eventDraft = EventSheet.createDraftFromEvent(ev, app.state);
            app.ui.eventDirty = false;
            render();
          }
        } else if (sType === 'TRANSACTION') {
          const tx = app.state.transactions.find((t) => t.id === sId);
          if (tx) {
            app.ui.txDraft = TransactionSheet.createDraftFromTx(tx, {}, app.state);
            app.ui.txDirty = false;
            render();
          }
        } else if (sType === 'STATEMENT') {
          clearSubRoutes();
          app.ui.activeTab = 'FINANCE';
          app.ui.financeTab = 'CARDS';
          app.ui.highlightedStatementId = sId;
          render();
        } else if (sType === 'PROJECT') {
          const proj = app.state.projects.find((p) => p.id === sId);
          if (proj) {
            app.ui.projectDraft = EntitySheets.createDraftFromProject(proj, {});
            app.ui.projectDirty = false;
            render();
          }
        }
        break;
      }
      case 'appt-toggle-allday':
        if (app.ui.apptDraft) {
          app.ui.apptDraft.isAllDay = !app.ui.apptDraft.isAllDay;
          app.ui.apptDirty = true;
          render();
        }
        break;
      case 'save-appt-sheet':
        saveApptFromDraft();
        break;
      case 'close-appt-sheet':
      case 'close-appt-sheet-backdrop':
        attemptCloseSheet('apptDirty', 'apptDraft', 'DISCARD_APPT', 'compromisso');
        break;
      case 'request-delete-appt':
        app.ui.confirmDialog = {
          type: 'DELETE_APPT',
          title: 'Excluir compromisso?',
          message: 'O compromisso será removido da Agenda.',
          confirmLabel: 'Excluir'
        };
        render();
        break;

      // Mercado
      case 'open-market-asset':
        app.ui.selectedMarketAssetCode = target.getAttribute('data-asset-code') || 'USD-BRL';
        app.ui.marketSelectedPointIdx = null;
        pushSubRoute('MARKET_DETAIL');
        render();
        break;
      case 'set-market-category':
        app.ui.marketCategoryFilter = target.getAttribute('data-category') || 'ALL';
        render();
        break;
      case 'toggle-market-fav': {
        const code = target.getAttribute('data-asset-code');
        if (!code) break;
        const idx = app.state.marketFavorites.indexOf(code);
        if (idx >= 0) {
          app.state.marketFavorites.splice(idx, 1);
          showSnackbar(`${code} removido dos favoritos.`);
        } else {
          app.state.marketFavorites.push(code);
          showSnackbar(`${code} adicionado à Watchlist.`);
        }
        break;
      }
      case 'refresh-market-sim': {
        const keys = Object.keys(app.state.marketQuotes);
        keys.forEach((k, i) => {
          const q = app.state.marketQuotes[k];
          const delta = ((i % 2 === 0 ? 1 : -1) * 0.0025);
          q.price = Number((q.price * (1 + delta)).toFixed(q.price < 20 ? 4 : 2));
          q.changePercent = Number((q.changePercent + (i % 2 === 0 ? 0.18 : -0.14)).toFixed(2));
          q.fetchedAt = '23/11/2026 14:42 (Simulado)';
        });
        const hits = evaluateMarketAlerts(app.state);
        showSnackbar(hits > 0
          ? `Cotações simuladas atualizadas · ${hits} alerta(s) atingiram o preço alvo!`
          : 'Snapshot simulado de cotações atualizado.');
        break;
      }
      case 'set-market-range':
        app.ui.marketHistoryRange = target.getAttribute('data-range') || '1M';
        app.ui.marketSelectedPointIdx = null;
        render();
        break;
      case 'select-market-point':
        app.ui.marketSelectedPointIdx = Number(target.getAttribute('data-point-idx'));
        render();
        break;
      case 'new-market-alert': {
        const code = target.getAttribute('data-asset-code') || app.ui.selectedMarketAssetCode || 'USD-BRL';
        const q = app.state.marketQuotes[code];
        app.ui.marketAlertDraft = {
          id: null,
          isNew: true,
          assetCode: code,
          condition: 'PRICE_BELOW',
          targetPriceText: q ? formatDecimalPt(q.price, q.price < 20 ? 2 : 2) : '5,70',
          isActive: true
        };
        render();
        break;
      }
      case 'edit-market-alert': {
        const al = app.state.marketAlerts.find((a) => a.id === target.getAttribute('data-alert-id'));
        if (al) {
          app.ui.marketAlertDraft = {
            id: al.id,
            isNew: false,
            assetCode: al.assetCode,
            condition: al.condition,
            targetPriceText: formatDecimalPt(al.targetPrice, 2),
            isActive: al.isActive
          };
          render();
        }
        break;
      }
      case 'save-market-alert': {
        const d = app.ui.marketAlertDraft;
        if (!d) break;
        const val = parseDecimalPt(d.targetPriceText);
        if (Number.isNaN(val) || val <= 0) {
          showSnackbar('Informe um preço alvo válido.');
          break;
        }
        const entity = {
          id: d.id || `ma_${Date.now()}`,
          assetCode: d.assetCode,
          condition: d.condition,
          targetPrice: val,
          isActive: Boolean(d.isActive),
          status: 'WAITING',
          triggeredAt: null
        };
        if (d.isNew) {
          app.state.marketAlerts.unshift(entity);
        } else {
          const idx = app.state.marketAlerts.findIndex((a) => a.id === entity.id);
          if (idx >= 0) app.state.marketAlerts[idx] = entity;
        }
        evaluateMarketAlerts(app.state);
        app.ui.marketAlertDraft = null;
        showSnackbar('Alerta de preço salvo.');
        break;
      }
      case 'toggle-market-alert': {
        const al = app.state.marketAlerts.find((a) => a.id === target.getAttribute('data-alert-id'));
        if (al) {
          al.isActive = !al.isActive;
          if (!al.isActive && al.status === 'WAITING') al.status = 'PAUSED';
          if (al.isActive && al.status === 'PAUSED') al.status = 'WAITING';
          render();
        }
        break;
      }
      case 'reactivate-market-alert': {
        const al = app.state.marketAlerts.find((a) => a.id === target.getAttribute('data-alert-id'));
        if (al) {
          al.isActive = true;
          al.status = 'WAITING';
          al.triggeredAt = null;
          showSnackbar('Alerta reativado.');
        }
        break;
      }
      case 'delete-market-alert': {
        const id = target.getAttribute('data-alert-id');
        app.state.marketAlerts = app.state.marketAlerts.filter((a) => a.id !== id);
        showSnackbar('Alerta removido.');
        break;
      }

      // Planejamento & Objetivos Pessoais
      case 'set-planning-status':
        app.ui.planningFilter.status = target.getAttribute('data-status') || 'ACTIVE';
        render();
        break;
      case 'set-planning-category':
        app.ui.planningFilter.categoryId = target.getAttribute('data-category-id') || null;
        render();
        break;
      case 'open-personal-goal':
        app.ui.selectedPersonalGoalId = target.getAttribute('data-goal-id');
        render();
        break;
      case 'open-personal-goal-in-planning':
        pushSubRoute('PLANNING');
        app.ui.selectedPersonalGoalId = target.getAttribute('data-goal-id');
        render();
        break;
      case 'close-goal-detail':
        app.ui.selectedPersonalGoalId = null;
        render();
        break;
      case 'new-personal-goal':
        app.ui.personalGoalDraft = {
          id: null,
          isNew: true,
          title: '',
          description: '',
          categoryId: 'cat_trabalho',
          targetDateIso: '2026-12-31',
          status: 'ACTIVE',
          progressMode: 'AUTO',
          manualProgressPercent: 0
        };
        render();
        break;
      case 'edit-personal-goal': {
        const g = app.state.personalGoals.find((x) => x.id === target.getAttribute('data-goal-id'));
        if (g) {
          app.ui.personalGoalDraft = {
            id: g.id,
            isNew: false,
            title: g.title,
            description: g.description || '',
            categoryId: g.categoryId || '',
            targetDateIso: g.targetDateIso || '',
            status: g.status || 'ACTIVE',
            progressMode: g.progressMode || 'AUTO',
            manualProgressPercent: Number(g.manualProgressPercent || 0)
          };
          render();
        }
        break;
      }
      case 'save-personal-goal': {
        const d = app.ui.personalGoalDraft;
        if (!d) break;
        if (!d.title.trim()) {
          showSnackbar('Informe o título do objetivo.');
          break;
        }
        const existing = d.id ? app.state.personalGoals.find((g) => g.id === d.id) : null;
        const entity = {
          ...(existing || { milestones: [], progressLogs: [] }),
          id: d.id || `pg_${Date.now()}`,
          title: d.title.trim(),
          description: d.description || '',
          categoryId: d.categoryId || null,
          targetDateIso: d.targetDateIso || null,
          status: d.status || 'ACTIVE',
          progressMode: d.progressMode || 'AUTO',
          manualProgressPercent: Number(d.manualProgressPercent || 0)
        };
        if (d.isNew) {
          app.state.personalGoals.push(entity);
          appendAudit(app.state, 'CRIAÇÃO', 'Objetivo pessoal', `Criado "${entity.title}".`);
          showSnackbar('Objetivo pessoal criado.');
        } else {
          const idx = app.state.personalGoals.findIndex((g) => g.id === entity.id);
          if (idx >= 0) app.state.personalGoals[idx] = entity;
          showSnackbar('Objetivo atualizado.');
        }
        app.ui.personalGoalDraft = null;
        render();
        break;
      }
      case 'request-delete-goal': {
        const gId = target.getAttribute('data-goal-id');
        app.ui.confirmDialog = {
          type: 'DELETE_PERSONAL_GOAL',
          title: 'Excluir objetivo pessoal?',
          message: 'O objetivo e seus marcos serão removidos (tarefas/projetos vinculados são mantidos).',
          confirmLabel: 'Excluir',
          payload: gId
        };
        render();
        break;
      }
      case 'toggle-goal-milestone': {
        const g = app.state.personalGoals.find((x) => x.id === target.getAttribute('data-goal-id'));
        const ms = g?.milestones?.find((m) => m.id === target.getAttribute('data-milestone-id'));
        if (ms) {
          ms.isCompleted = !ms.isCompleted;
          showSnackbar(ms.isCompleted ? 'Marco concluído.' : 'Marco reaberto.');
        }
        break;
      }
      case 'delete-goal-milestone': {
        const g = app.state.personalGoals.find((x) => x.id === target.getAttribute('data-goal-id'));
        const mId = target.getAttribute('data-milestone-id');
        if (g && Array.isArray(g.milestones)) {
          g.milestones = g.milestones.filter((m) => m.id !== mId);
          render();
        }
        break;
      }
      case 'add-goal-milestone': {
        const g = app.state.personalGoals.find((x) => x.id === target.getAttribute('data-goal-id'));
        const title = (app.ui.newMilestoneTitle || '').trim();
        if (g && title) {
          g.milestones = g.milestones || [];
          g.milestones.push({ id: `ms_${Date.now()}`, title, description: '', dueDateIso: null, isCompleted: false });
          app.ui.newMilestoneTitle = '';
          showSnackbar('Marco adicionado.');
        } else {
          showSnackbar('Digite o título do marco.');
        }
        break;
      }
      case 'add-goal-progress-log': {
        const g = app.state.personalGoals.find((x) => x.id === target.getAttribute('data-goal-id'));
        const note = (app.ui.newGoalLogNote || '').trim();
        if (g && note) {
          const det = window.NexusPilot.Data.computeGoalDetails(app.state, g);
          g.progressLogs = g.progressLogs || [];
          g.progressLogs.unshift({
            id: `pl_${Date.now()}`,
            dateIso: app.state.todayIso,
            percent: det.progressPercent,
            note
          });
          app.ui.newGoalLogNote = '';
          showSnackbar('Evolução registrada.');
        } else {
          showSnackbar('Escreva uma nota de evolução.');
        }
        break;
      }

      // Personalização & Biblioteca de Frases & Configurações
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
      case 'toggle-expand-custom-block': {
        const bType = target.getAttribute('data-block-type');
        app.ui.expandedCustomBlockType = app.ui.expandedCustomBlockType === bType ? null : bType;
        render();
        break;
      }
      case 'toggle-block-prop': {
        const bType = target.getAttribute('data-block-type');
        const prop = target.getAttribute('data-prop');
        const b = app.state.blocks.find((x) => x.type === bType);
        if (b && prop) b[prop] = !b[prop];
        render();
        break;
      }
      case 'step-block-max': {
        const bType = target.getAttribute('data-block-type');
        const delta = Number(target.getAttribute('data-delta') || 0);
        const b = app.state.blocks.find((x) => x.type === bType);
        if (b) {
          b.maxItems = Math.max(1, Math.min(10, Number(b.maxItems || 3) + delta));
        }
        render();
        break;
      }
      case 'request-reset-dashboard':
        app.ui.confirmDialog = {
          type: 'RESET_DASHBOARD',
          title: 'Restaurar padrão da Home?',
          message: 'A ordem, visibilidade e limites dos 9 blocos voltarão ao padrão inicial.',
          confirmLabel: 'Restaurar'
        };
        render();
        break;
      case 'rotate-daily-quote': {
        const active = app.state.quotes.filter((q) => q.isActive);
        if (!active.length) {
          showSnackbar('Nenhuma frase ativa na biblioteca.');
          break;
        }
        const curIdx = active.findIndex((q) => q.id === app.state.dailyQuote?.id);
        const nextQ = active[(curIdx + 1) % active.length];
        app.state.dailyQuote = { id: nextQ.id, text: nextQ.text, author: nextQ.author };
        showSnackbar('Frase do dia alternada.');
        break;
      }
      case 'use-quote-today': {
        const qt = app.state.quotes.find((q) => q.id === target.getAttribute('data-quote-id'));
        if (qt) {
          app.state.dailyQuote = { id: qt.id, text: qt.text, author: qt.author };
          showSnackbar('Frase aplicada na Home hoje.');
        }
        break;
      }
      case 'new-quote':
        app.ui.quoteDraft = { id: null, isNew: true, text: '', author: '' };
        render();
        break;
      case 'edit-quote': {
        const qt = app.state.quotes.find((q) => q.id === target.getAttribute('data-quote-id'));
        if (qt) {
          app.ui.quoteDraft = { id: qt.id, isNew: false, text: qt.text, author: qt.author || '' };
          render();
        }
        break;
      }
      case 'save-quote': {
        const d = app.ui.quoteDraft;
        if (!d) break;
        if (!d.text.trim()) {
          showSnackbar('Digite o texto da frase.');
          break;
        }
        if (d.isNew) {
          const item = { id: `qt_${Date.now()}`, text: d.text.trim(), author: d.author.trim(), isActive: true };
          app.state.quotes.unshift(item);
        } else {
          const qt = app.state.quotes.find((q) => q.id === d.id);
          if (qt) {
            qt.text = d.text.trim();
            qt.author = d.author.trim();
            if (app.state.dailyQuote?.id === qt.id) {
              app.state.dailyQuote = { id: qt.id, text: qt.text, author: qt.author };
            }
          }
        }
        app.ui.quoteDraft = null;
        showSnackbar('Frase salva.');
        break;
      }
      case 'delete-quote': {
        const id = target.getAttribute('data-quote-id');
        app.state.quotes = app.state.quotes.filter((q) => q.id !== id);
        showSnackbar('Frase removida.');
        break;
      }
      case 'toggle-quote-active': {
        const qt = app.state.quotes.find((q) => q.id === target.getAttribute('data-quote-id'));
        if (qt) {
          qt.isActive = !qt.isActive;
          render();
        }
        break;
      }
      case 'toggle-settings-theme':
        app.ui.theme = app.ui.theme === 'dark' ? 'light' : 'dark';
        render();
        break;
      case 'set-date-format':
        app.state.settings.dateFormatPattern = target.getAttribute('data-format') || 'WEEKDAY_DD_MM_YYYY';
        render();
        break;
      case 'new-category':
        app.ui.categoryDraft = { id: null, isNew: true, name: '', colorHex: '#5CC8FF', scope: 'GENERAL' };
        render();
        break;
      case 'edit-category': {
        const c = app.state.categories.find((x) => x.id === target.getAttribute('data-category-id'));
        if (c) {
          app.ui.categoryDraft = { id: c.id, isNew: false, name: c.name, colorHex: c.colorHex || '#5CC8FF', scope: c.scope || 'GENERAL' };
          render();
        }
        break;
      }
      case 'save-category': {
        const d = app.ui.categoryDraft;
        if (!d) break;
        if (!d.name.trim()) {
          showSnackbar('Informe o nome da categoria.');
          break;
        }
        if (d.isNew) {
          app.state.categories.push({ id: `cat_${Date.now()}`, name: d.name.trim(), colorHex: d.colorHex || '#5CC8FF', scope: d.scope || 'GENERAL' });
        } else {
          const c = app.state.categories.find((x) => x.id === d.id);
          if (c) {
            c.name = d.name.trim();
            c.colorHex = d.colorHex || '#5CC8FF';
            c.scope = d.scope || 'GENERAL';
          }
        }
        app.ui.categoryDraft = null;
        showSnackbar('Categoria salva.');
        break;
      }
      case 'delete-category': {
        const id = target.getAttribute('data-category-id');
        app.state.categories = app.state.categories.filter((c) => c.id !== id);
        showSnackbar('Categoria excluída.');
        break;
      }

      // Backup & Diagnóstico & Widgets
      case 'download-backup-json': {
        const payload = MoreSubroutes.buildBackupV7Payload(app.state);
        const text = JSON.stringify(payload, null, 2);
        const blob = new Blob([text], { type: 'application/json;charset=utf-8;' });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `nexus_backup_v7_${app.state.todayIso}.json`;
        document.body.appendChild(a);
        a.click();
        a.remove();
        URL.revokeObjectURL(url);
        showSnackbar('Backup JSON v7 exportado.');
        break;
      }
      case 'toggle-backup-json-preview':
        app.ui.backupJsonExpanded = !app.ui.backupJsonExpanded;
        render();
        break;
      case 'inspect-backup-import':
        validateAndPreviewBackupJson(app.ui.backupImportText);
        break;
      case 'confirm-backup-restore':
        if (app.ui.backupPendingPreview) applyBackupRestore(app.ui.backupPendingPreview);
        break;
      case 'copy-diagnostics': {
        const counts = window.NexusPilot.Data.computeDatabaseTableCounts(app.state);
        const text = `NEXUS Diagnóstico\nApp: 1.7.0 (Espelho Web) · Room v18\nTabelas: ${counts.map((c) => `${c.table}=${c.count}`).join(', ')}\nErros: ${app.state.errorLogs.length}`;
        if (navigator.clipboard && navigator.clipboard.writeText) {
          navigator.clipboard.writeText(text).catch(() => {});
        }
        showSnackbar('Relatório de diagnóstico copiado.');
        break;
      }
      case 'clear-error-logs':
        app.state.errorLogs = [];
        showSnackbar('Logs locais limpos.');
        break;
      case 'sync-widgets-sim':
        showSnackbar('4 widgets Glance sincronizados com os dados atuais (simulado).');
        break;
      case 'pin-widget-sim':
        showSnackbar(`Solicitação de fixação simulada: ${target.getAttribute('data-widget-title') || 'Widget'}.`);
        break;

      // Finanças extras: Cartões, Parcelamentos, Recorrências, Orçamentos, Metas, Empréstimos
      case 'open-balance-dialog':
        app.ui.balanceDraftText = TransactionSheet.centsToInputBr(app.state.declaredBalanceCents);
        app.ui.balanceDialogOpen = true;
        render();
        break;
      case 'save-balance-dialog':
        app.state.declaredBalanceCents = TransactionSheet.parseInputBrToCents(app.ui.balanceDraftText);
        app.ui.balanceDialogOpen = false;
        appendAudit(app.state, 'ATUALIZAÇÃO', 'Saldo informado', `Saldo ajustado para ${formatBrl(app.state.declaredBalanceCents)}.`);
        showSnackbar('Saldo informado atualizado.');
        break;
      case 'new-credit-card':
        app.ui.cardDraft = { id: null, isNew: true, name: '', institution: '', totalLimitText: '5000,00', closingDay: '10', dueDay: '17' };
        render();
        break;
      case 'edit-credit-card': {
        const c = app.state.creditCards.find((x) => x.id === target.getAttribute('data-card-id'));
        if (c) {
          app.ui.cardDraft = {
            id: c.id,
            isNew: false,
            name: c.name,
            institution: c.institution || '',
            totalLimitText: TransactionSheet.centsToInputBr(c.totalLimitCents),
            closingDay: String(c.closingDay),
            dueDay: String(c.dueDay)
          };
          render();
        }
        break;
      }
      case 'save-credit-card': {
        const d = app.ui.cardDraft;
        if (!d) break;
        if (!d.name.trim()) {
          showSnackbar('Informe o nome do cartão.');
          break;
        }
        const limitCents = TransactionSheet.parseInputBrToCents(d.totalLimitText);
        const entity = {
          id: d.id || `card_${Date.now()}`,
          name: d.name.trim(),
          institution: d.institution.trim() || 'Banco',
          totalLimitCents: limitCents > 0 ? limitCents : 500000,
          availableLimitCents: limitCents > 0 ? limitCents : 500000,
          closingDay: Math.max(1, Math.min(28, Number(d.closingDay) || 10)),
          dueDay: Math.max(1, Math.min(28, Number(d.dueDay) || 17)),
          notes: ''
        };
        if (d.isNew) {
          app.state.creditCards.push(entity);
          app.state.statements.push({
            id: `stmt_${entity.id}_${app.state.selectedMonthIso}`,
            creditCardId: entity.id,
            yearMonth: app.state.selectedMonthIso,
            dueDateIso: `${app.state.selectedMonthIso}-${String(entity.dueDay).padStart(2, '0')}`,
            status: 'OPEN'
          });
          appendAudit(app.state, 'CRIAÇÃO', 'Cartão', `Cartão "${entity.name}" adicionado.`);
        } else {
          const idx = app.state.creditCards.findIndex((x) => x.id === entity.id);
          if (idx >= 0) app.state.creditCards[idx] = entity;
        }
        app.ui.cardDraft = null;
        showSnackbar('Cartão salvo.');
        break;
      }
      case 'delete-credit-card': {
        const d = app.ui.cardDraft;
        if (d?.id) {
          app.state.creditCards = app.state.creditCards.filter((c) => c.id !== d.id);
          app.state.statements = app.state.statements.filter((s) => s.creditCardId !== d.id);
          app.ui.cardDraft = null;
          showSnackbar('Cartão removido.');
        }
        break;
      }
      case 'new-installment':
        app.ui.installmentDraft = {
          description: '',
          totalAmountText: '1200,00',
          installmentCount: '6',
          creditCardId: app.state.creditCards[0]?.id || '',
          categoryId: 'cat_equip',
          firstDueDateIso: app.state.todayIso
        };
        render();
        break;
      case 'save-installment': {
        const d = app.ui.installmentDraft;
        if (!d) break;
        if (!d.description.trim()) {
          showSnackbar('Informe a descrição do parcelamento.');
          break;
        }
        const totalCents = TransactionSheet.parseInputBrToCents(d.totalAmountText);
        const count = Math.max(2, Math.min(48, Number(d.installmentCount) || 2));
        if (totalCents <= 0) {
          showSnackbar('Informe o valor total.');
          break;
        }
        const inst = {
          id: `inst_${Date.now()}`,
          description: d.description.trim(),
          totalAmountCents: totalCents,
          installmentCount: count,
          paidCount: 0,
          remainingCount: count,
          installmentAmountCents: Math.round(totalCents / count),
          paymentMethod: d.creditCardId ? 'CREDIT_CARD' : 'PIX',
          creditCardId: d.creditCardId || null,
          categoryId: d.categoryId || null,
          firstDueDateIso: d.firstDueDateIso || app.state.todayIso
        };
        app.state.installmentPurchases.unshift(inst);
        appendAudit(app.state, 'CRIAÇÃO', 'Parcelamento', `"${inst.description}" em ${count}x de ${formatBrl(inst.installmentAmountCents)}.`);
        app.ui.installmentDraft = null;
        showSnackbar('Parcelamento criado.');
        break;
      }
      case 'pay-installment-one': {
        const inst = app.state.installmentPurchases.find((i) => i.id === target.getAttribute('data-inst-id'));
        if (inst && inst.remainingCount > 0) {
          inst.paidCount += 1;
          inst.remainingCount -= 1;
          appendAudit(app.state, 'PAGAMENTO', 'Parcelamento', `Parcela ${inst.paidCount}/${inst.installmentCount} de "${inst.description}" paga.`);
          showSnackbar(`Parcela ${inst.paidCount}/${inst.installmentCount} quitada.`);
        }
        break;
      }
      case 'pay-installment-all': {
        const inst = app.state.installmentPurchases.find((i) => i.id === target.getAttribute('data-inst-id'));
        if (inst && inst.remainingCount > 0) {
          inst.paidCount = inst.installmentCount;
          inst.remainingCount = 0;
          appendAudit(app.state, 'QUITAÇÃO', 'Parcelamento', `Quitadas todas as parcelas de "${inst.description}".`);
          showSnackbar('Parcelamento quitado integralmente.');
        }
        break;
      }
      case 'delete-installment': {
        const id = target.getAttribute('data-inst-id');
        app.state.installmentPurchases = app.state.installmentPurchases.filter((i) => i.id !== id);
        showSnackbar('Parcelamento removido.');
        break;
      }
      case 'new-recurring-rule':
        app.ui.recurringDraft = {
          description: '',
          type: 'EXPENSE',
          amountText: '150,00',
          frequency: 'MONTHLY',
          paymentMethod: 'PIX',
          categoryId: 'cat_moradia',
          nextOccurrenceIso: '2026-12-01'
        };
        render();
        break;
      case 'save-recurring-rule': {
        const d = app.ui.recurringDraft;
        if (!d) break;
        if (!d.description.trim()) {
          showSnackbar('Informe a descrição da regra.');
          break;
        }
        const amountCents = TransactionSheet.parseInputBrToCents(d.amountText);
        if (amountCents <= 0) {
          showSnackbar('Informe um valor maior que zero.');
          break;
        }
        app.state.recurringRules.unshift({
          id: `rec_${Date.now()}`,
          description: d.description.trim(),
          type: d.type,
          amountCents,
          frequency: d.frequency,
          paymentMethod: d.paymentMethod,
          categoryId: d.categoryId || null,
          nextOccurrenceIso: d.nextOccurrenceIso || app.state.todayIso,
          isActive: true
        });
        appendAudit(app.state, 'CRIAÇÃO', 'Recorrência', `Regra "${d.description.trim()}" criada.`);
        app.ui.recurringDraft = null;
        showSnackbar('Regra recorrente criada.');
        break;
      }
      case 'toggle-recurring-active': {
        const rec = app.state.recurringRules.find((r) => r.id === target.getAttribute('data-rec-id'));
        if (rec) {
          rec.isActive = !rec.isActive;
          render();
        }
        break;
      }
      case 'generate-recurring-tx': {
        const rec = app.state.recurringRules.find((r) => r.id === target.getAttribute('data-rec-id'));
        if (rec) {
          const stmt = rec.paymentMethod === 'CREDIT_CARD' ? app.state.statements.find((s) => s.status !== 'PAID') : null;
          app.state.transactions.unshift({
            id: `tx_${Date.now()}`,
            type: rec.type,
            description: rec.description,
            amountCents: rec.amountCents,
            categoryId: rec.categoryId,
            status: 'PENDING',
            paymentMethod: rec.paymentMethod,
            dueDateIso: rec.nextOccurrenceIso,
            creditCardId: stmt ? stmt.creditCardId : null,
            statementId: stmt ? stmt.id : null,
            projectId: null,
            eventId: null,
            taskId: null,
            eventCostType: null,
            notes: 'Gerado por regra recorrente.'
          });
          const nextMonth = shiftMonthIso(rec.nextOccurrenceIso.slice(0, 7), 1);
          rec.nextOccurrenceIso = `${nextMonth}-${rec.nextOccurrenceIso.slice(8, 10)}`;
          appendAudit(app.state, 'GERAÇÃO', 'Recorrência', `Gerado lançamento de "${rec.description}".`);
          showSnackbar(`Lançamento gerado e próxima data avançada para ${rec.nextOccurrenceIso}.`);
        }
        break;
      }
      case 'delete-recurring-rule': {
        const id = target.getAttribute('data-rec-id');
        app.state.recurringRules = app.state.recurringRules.filter((r) => r.id !== id);
        showSnackbar('Regra recorrente removida.');
        break;
      }
      case 'new-budget':
        app.ui.budgetDraft = { id: null, isNew: true, categoryId: app.state.categories[0]?.id || 'cat_moradia', limitText: '1000,00' };
        render();
        break;
      case 'edit-budget': {
        const b = app.state.budgets.find((x) => x.id === target.getAttribute('data-budget-id'));
        if (b) {
          app.ui.budgetDraft = { id: b.id, isNew: false, categoryId: b.categoryId, limitText: TransactionSheet.centsToInputBr(b.limitCents) };
          render();
        }
        break;
      }
      case 'save-budget': {
        const d = app.ui.budgetDraft;
        if (!d) break;
        const limitCents = TransactionSheet.parseInputBrToCents(d.limitText);
        if (limitCents <= 0) {
          showSnackbar('Informe um limite válido.');
          break;
        }
        if (d.isNew) {
          app.state.budgets.push({ id: `bdg_${Date.now()}`, categoryId: d.categoryId, limitCents });
        } else {
          const b = app.state.budgets.find((x) => x.id === d.id);
          if (b) {
            b.categoryId = d.categoryId;
            b.limitCents = limitCents;
          }
        }
        app.ui.budgetDraft = null;
        showSnackbar('Orçamento salvo.');
        break;
      }
      case 'delete-budget': {
        const id = target.getAttribute('data-budget-id');
        app.state.budgets = app.state.budgets.filter((b) => b.id !== id);
        showSnackbar('Orçamento excluído.');
        break;
      }
      case 'new-financial-goal':
        app.ui.financialGoalDraft = { name: '', currentText: '0,00', targetText: '5000,00', targetDateIso: '2027-06-30' };
        render();
        break;
      case 'save-financial-goal': {
        const d = app.ui.financialGoalDraft;
        if (!d) break;
        if (!d.name.trim()) {
          showSnackbar('Informe o nome da meta financeira.');
          break;
        }
        const cur = TransactionSheet.parseInputBrToCents(d.currentText);
        const tgt = TransactionSheet.parseInputBrToCents(d.targetText);
        if (tgt <= 0) {
          showSnackbar('Informe um valor alvo maior que zero.');
          break;
        }
        app.state.financialGoals.push({
          id: `fg_${Date.now()}`,
          name: d.name.trim(),
          currentAmountCents: cur,
          targetAmountCents: tgt,
          targetDateIso: d.targetDateIso || null
        });
        app.ui.financialGoalDraft = null;
        showSnackbar('Meta financeira criada.');
        break;
      }
      case 'open-fgoal-amount': {
        const fg = app.state.financialGoals.find((x) => x.id === target.getAttribute('data-fgoal-id'));
        if (fg) {
          app.ui.fgoalAmountDraft = { id: fg.id, name: fg.name, currentText: TransactionSheet.centsToInputBr(fg.currentAmountCents) };
          render();
        }
        break;
      }
      case 'save-fgoal-amount': {
        const d = app.ui.fgoalAmountDraft;
        const fg = d ? app.state.financialGoals.find((x) => x.id === d.id) : null;
        if (fg) {
          fg.currentAmountCents = TransactionSheet.parseInputBrToCents(d.currentText);
          appendAudit(app.state, 'ATUALIZAÇÃO', 'Meta financeira', `"${fg.name}" atualizada para ${formatBrl(fg.currentAmountCents)}.`);
          app.ui.fgoalAmountDraft = null;
          showSnackbar('Progresso da meta atualizado.');
        }
        break;
      }
      case 'delete-financial-goal': {
        const id = target.getAttribute('data-fgoal-id');
        app.state.financialGoals = app.state.financialGoals.filter((x) => x.id !== id);
        showSnackbar('Meta financeira excluída.');
        break;
      }
      case 'new-loan':
        app.ui.loanDraft = {
          id: null,
          isNew: true,
          name: '',
          lender: '',
          dueDay: '15',
          principalText: '5000,00',
          remainingText: '5000,00',
          monthlyText: '500,00',
          totalInstallments: '10'
        };
        render();
        break;
      case 'edit-loan': {
        const ln = app.state.loans.find((x) => x.id === target.getAttribute('data-loan-id'));
        if (ln) {
          app.ui.loanDraft = {
            id: ln.id,
            isNew: false,
            name: ln.name,
            lender: ln.lender || '',
            dueDay: String(ln.dueDay || 15),
            principalText: TransactionSheet.centsToInputBr(ln.principalCents),
            remainingText: TransactionSheet.centsToInputBr(ln.remainingCents),
            monthlyText: TransactionSheet.centsToInputBr(ln.monthlyInstallmentCents),
            totalInstallments: String(ln.totalInstallments || 12)
          };
          render();
        }
        break;
      }
      case 'save-loan': {
        const d = app.ui.loanDraft;
        if (!d) break;
        if (!d.name.trim()) {
          showSnackbar('Informe a descrição do empréstimo.');
          break;
        }
        const principal = TransactionSheet.parseInputBrToCents(d.principalText);
        const remaining = TransactionSheet.parseInputBrToCents(d.remainingText);
        const monthly = TransactionSheet.parseInputBrToCents(d.monthlyText);
        const totalInst = Math.max(1, Number(d.totalInstallments) || 12);
        if (d.isNew) {
          app.state.loans.push({
            id: `loan_${Date.now()}`,
            name: d.name.trim(),
            lender: d.lender.trim() || 'Instituição',
            principalCents: principal,
            remainingCents: remaining,
            monthlyInstallmentCents: monthly,
            totalInstallments: totalInst,
            paidInstallments: 0,
            dueDay: Math.max(1, Math.min(28, Number(d.dueDay) || 15)),
            interestNote: '',
            notes: ''
          });
        } else {
          const ln = app.state.loans.find((x) => x.id === d.id);
          if (ln) {
            ln.name = d.name.trim();
            ln.lender = d.lender.trim();
            ln.principalCents = principal;
            ln.remainingCents = remaining;
            ln.monthlyInstallmentCents = monthly;
            ln.totalInstallments = totalInst;
            ln.dueDay = Math.max(1, Math.min(28, Number(d.dueDay) || 15));
          }
        }
        app.ui.loanDraft = null;
        showSnackbar('Empréstimo salvo.');
        break;
      }
      case 'pay-loan-installment': {
        const ln = app.state.loans.find((x) => x.id === target.getAttribute('data-loan-id'));
        if (ln && ln.remainingCents > 0) {
          ln.paidInstallments = Math.min(ln.totalInstallments, ln.paidInstallments + 1);
          ln.remainingCents = Math.max(0, ln.remainingCents - ln.monthlyInstallmentCents);
          appendAudit(app.state, 'PAGAMENTO', 'Empréstimo', `Parcela ${ln.paidInstallments}/${ln.totalInstallments} de "${ln.name}" quitada.`);
          showSnackbar(`Parcela ${ln.paidInstallments}/${ln.totalInstallments} paga.`);
        }
        break;
      }
      case 'delete-loan': {
        const id = target.getAttribute('data-loan-id');
        app.state.loans = app.state.loans.filter((l) => l.id !== id);
        showSnackbar('Empréstimo excluído.');
        break;
      }
      case 'toggle-extra-bool': {
        const path = target.getAttribute('data-path');
        if (path) {
          const cur = Boolean(getByPath(app.ui, path));
          setByPath(app.ui, path, !cur);
          render();
        }
        break;
      }

      // Finanças: Lançamentos, Faturas, Gráficos e CSV
      case 'toggle-settle-tx':
      case 'settle-tx': {
        const txId = target.getAttribute('data-tx-id');
        const tx = app.state.transactions.find((t) => t.id === txId);
        if (tx) {
          if (action === 'settle-tx') tx.status = 'SETTLED';
          else tx.status = tx.status === 'SETTLED' ? 'PENDING' : 'SETTLED';
          appendAudit(app.state, 'ATUALIZAÇÃO', 'Lançamento', `"${tx.description}" alterado para ${LABELS.FinancialStatus[tx.status]}.`);
          showSnackbar(tx.status === 'SETTLED' ? 'Lançamento marcado como realizado.' : 'Lançamento reaberto como pendente.');
        }
        break;
      }
      case 'postpone-tx': {
        const txId = target.getAttribute('data-tx-id');
        const tx = app.state.transactions.find((t) => t.id === txId);
        if (tx) {
          tx.dueDateIso = addDaysIso(app.state.todayIso, 1);
          if (tx.status === 'OVERDUE') tx.status = 'PENDING';
          showSnackbar(`Conta adiada para ${formatDateBr(tx.dueDateIso)}.`);
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
          appendAudit(app.state, 'FECHAMENTO', 'Fatura', `Fatura ${stmt.yearMonth} fechada.`);
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
          appendAudit(app.state, 'PAGAMENTO', 'Fatura', `Fatura ${stmt.yearMonth} quitada com seus lançamentos.`);
          showSnackbar(`Fatura ${stmt.yearMonth} paga e lançamentos liquidados.`);
        }
        break;
      }
      case 'open-card-statement': {
        const stmtId = target.getAttribute('data-statement-id');
        clearSubRoutes();
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
        clearSubRoutes();
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
        attemptCloseSheet('txDirty', 'txDraft', 'DISCARD_TX', 'lançamento');
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
        clearSubRoutes();
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
        clearSubRoutes();
        app.ui.activeTab = 'EVENTS';
        app.ui.eventsFilter = 'ALL';
        if (ev) {
          app.ui.eventDraft = EventSheet.createDraftFromEvent(ev, app.state);
          app.ui.eventDirty = false;
        }
        render();
        break;
      }

      // Eventos
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
        attemptCloseSheet('eventDirty', 'eventDraft', 'DISCARD_EVENT', 'evento');
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

      // Assistente de Rotina
      case 'mark-briefing-reviewed':
        app.state.briefingState.morningReviewedToday = true;
        if (!app.state.briefingState.history.some((h) => h.dateIso === app.state.todayIso && h.briefingType === 'Briefing Diário')) {
          app.state.briefingState.history.unshift({
            id: `bh_${Date.now()}`,
            briefingType: 'Briefing Diário',
            dateIso: app.state.todayIso,
            summaryText: 'Revisão diária concluída no Assistente de Rotina.',
            reviewedAt: '23/11/2026 agora'
          });
        }
        showSnackbar('Briefing Diário marcado como revisado.');
        break;
      case 'mark-night-reviewed':
        app.state.briefingState.nightReviewedTonight = true;
        if (!app.state.briefingState.history.some((h) => h.dateIso === app.state.todayIso && h.briefingType === 'Revisão Noturna')) {
          app.state.briefingState.history.unshift({
            id: `bh_${Date.now()}`,
            briefingType: 'Revisão Noturna',
            dateIso: app.state.todayIso,
            summaryText: `Fechamento noturno realizado · pendências encaminhadas para ${formatDateBr(addDaysIso(app.state.todayIso, 1)).slice(0, 5)}.`,
            reviewedAt: `${formatDateBr(app.state.todayIso)} agora`
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

      // Diálogo de confirmação genérico + escopos recorrentes
      case 'cancel-confirm-dialog':
        app.ui.confirmDialog = null;
        render();
        break;
      case 'alt-confirm-dialog': {
        const cd = app.ui.confirmDialog;
        app.ui.confirmDialog = null;
        if (!cd) break;
        if (cd.type === 'SAVE_TASK_SCOPE') {
          saveTaskFromDraft('THIS_OCCURRENCE_ONLY');
        } else if (cd.type === 'DELETE_TASK_RECURRING' && app.ui.taskDraft?.id) {
          const tId = app.ui.taskDraft.id;
          const recId = app.ui.taskDraft.recurrenceId;
          const occDate = app.ui.taskDraft.occurrenceDateIso || app.ui.taskDraft.dueDateIso;
          if (recId && occDate) {
            app.state.recurrenceExceptions = app.state.recurrenceExceptions || [];
            if (!app.state.recurrenceExceptions.some((ex) => ex.recurrenceId === recId && ex.occurrenceDateIso === occDate)) {
              app.state.recurrenceExceptions.push({
                id: `rex_${Date.now()}`,
                recurrenceId: recId,
                occurrenceDateIso: occDate,
                exceptionType: 'SKIP'
              });
            }
          }
          app.state.tasks = app.state.tasks.filter((t) => t.id !== tId);
          app.state.transactions.forEach((tx) => {
            if (tx.taskId === tId) tx.taskId = null;
          });
          app.ui.taskDraft = null;
          app.ui.taskDirty = false;
          showSnackbar('Ocorrência excluída da série.');
        }
        break;
      }
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
        } else if (cd.type === 'DISCARD_TASK') {
          app.ui.taskDraft = null;
          app.ui.taskDirty = false;
          render();
        } else if (cd.type === 'DISCARD_APPT') {
          app.ui.apptDraft = null;
          app.ui.apptDirty = false;
          render();
        } else if (cd.type === 'DISCARD_PROJECT') {
          app.ui.projectDraft = null;
          app.ui.projectDirty = false;
          render();
        } else if (cd.type === 'SAVE_TASK_SCOPE' || cd.type === 'CONFIRM_RECURRENCE_RULE_CHANGE') {
          saveTaskFromDraft('ENTIRE_SERIES');
        } else if (cd.type === 'DELETE_TX' && app.ui.txDraft?.id) {
          app.state.transactions = app.state.transactions.filter((t) => t.id !== app.ui.txDraft.id);
          app.ui.txDraft = null;
          app.ui.txDirty = false;
          showSnackbar('Lançamento excluído.');
        } else if (cd.type === 'DELETE_EVENT' && app.ui.eventDraft?.id) {
          const evId = app.ui.eventDraft.id;
          app.state.events = app.state.events.filter((ev) => ev.id !== evId);
          app.state.transactions.forEach((tx) => {
            if (tx.eventId === evId) tx.eventId = null;
          });
          app.ui.eventDraft = null;
          app.ui.eventDirty = false;
          showSnackbar('Evento excluído.');
        } else if (cd.type === 'DELETE_TASK' && app.ui.taskDraft?.id) {
          const tId = app.ui.taskDraft.id;
          app.state.tasks = app.state.tasks.filter((t) => t.id !== tId);
          app.state.transactions.forEach((tx) => {
            if (tx.taskId === tId) tx.taskId = null;
          });
          app.ui.taskDraft = null;
          app.ui.taskDirty = false;
          showSnackbar('Tarefa excluída.');
        } else if (cd.type === 'DELETE_TASK_RECURRING' && app.ui.taskDraft?.id) {
          const tId = app.ui.taskDraft.id;
          const recId = app.ui.taskDraft.recurrenceId;
          if (recId) {
            const rec = (app.state.taskRecurrences || []).find((r) => r.id === recId);
            if (rec) rec.isActive = false;
            app.state.tasks.forEach((t) => {
              if (t.recurrenceId === recId) {
                t.recurrenceId = null;
                t.recurrence = null;
              }
            });
          }
          app.state.tasks = app.state.tasks.filter((t) => t.id !== tId);
          app.state.transactions.forEach((tx) => {
            if (tx.taskId === tId) tx.taskId = null;
          });
          app.ui.taskDraft = null;
          app.ui.taskDirty = false;
          showSnackbar('Série recorrente encerrada e tarefa excluída.');
        } else if (cd.type === 'DELETE_APPT' && app.ui.apptDraft?.id) {
          const aId = app.ui.apptDraft.id;
          app.state.appointments = app.state.appointments.filter((a) => a.id !== aId);
          app.ui.apptDraft = null;
          app.ui.apptDirty = false;
          showSnackbar('Compromisso excluído.');
        } else if (cd.type === 'DELETE_PROJECT' && app.ui.projectDraft?.id) {
          const pId = app.ui.projectDraft.id;
          app.state.projects = app.state.projects.filter((p) => p.id !== pId);
          app.state.tasks.forEach((t) => {
            if (t.projectId === pId) t.projectId = null;
          });
          app.state.transactions.forEach((tx) => {
            if (tx.projectId === pId) tx.projectId = null;
          });
          app.ui.projectDraft = null;
          app.ui.projectDirty = false;
          showSnackbar('Projeto excluído.');
        } else if (cd.type === 'DELETE_PERSONAL_GOAL' && cd.payload) {
          const gId = cd.payload;
          app.state.personalGoals = app.state.personalGoals.filter((g) => g.id !== gId);
          app.state.tasks.forEach((t) => {
            if (t.goalId === gId) t.goalId = null;
          });
          app.state.projects.forEach((p) => {
            if (p.goalId === gId) p.goalId = null;
          });
          app.ui.selectedPersonalGoalId = null;
          showSnackbar('Objetivo pessoal excluído.');
        } else if (cd.type === 'RESET_DASHBOARD') {
          const fresh = createInitialState();
          app.state.blocks = fresh.blocks;
          app.state.personalization = fresh.personalization;
          showSnackbar('Painel da Home restaurado ao padrão.');
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

  // ==================== INPUT DELEGATION (PRESERVANDO FOCO) ====================
  function renderWithCaretPreserved(inputId, pos) {
    render();
    const nextEl = document.getElementById(inputId);
    if (nextEl) {
      nextEl.focus();
      if (typeof pos === 'number' && typeof nextEl.setSelectionRange === 'function') {
        try { nextEl.setSelectionRange(pos, pos); } catch (_err) { /* noop */ }
      }
    }
  }

  document.addEventListener('input', (e) => {
    const el = e.target;
    const dataInput = el.getAttribute('data-input');

    if (dataInput === 'global-search') {
      app.ui.globalSearchQuery = el.value;
      renderWithCaretPreserved('nx-search-input', el.selectionStart);
      return;
    }
    if (dataInput === 'planning-search') {
      app.ui.planningFilter.query = el.value;
      renderWithCaretPreserved('nx-planning-search', el.selectionStart);
      return;
    }
    if (dataInput === 'quote-search') {
      app.ui.quoteSearchQuery = el.value;
      renderWithCaretPreserved('nx-quote-search', el.selectionStart);
      return;
    }
    if (dataInput === 'settings-username') {
      app.state.userName = el.value;
      return;
    }
    if (dataInput === 'balance-draft') {
      app.ui.balanceDraftText = el.value;
      return;
    }
    if (dataInput === 'new-milestone-title') {
      app.ui.newMilestoneTitle = el.value;
      return;
    }
    if (dataInput === 'new-goal-log-note') {
      app.ui.newGoalLogNote = el.value;
      return;
    }
    if (dataInput === 'backup-import-text') {
      app.ui.backupImportText = el.value;
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
      return;
    }
    const taskField = el.getAttribute('data-task-field');
    if (taskField && app.ui.taskDraft) {
      app.ui.taskDraft[taskField] = el.value;
      if (taskField !== 'newSubtaskTitle') app.ui.taskDirty = true;
      return;
    }
    const apptField = el.getAttribute('data-appt-field');
    if (apptField && app.ui.apptDraft) {
      app.ui.apptDraft[apptField] = el.value;
      app.ui.apptDirty = true;
      return;
    }
    const projField = el.getAttribute('data-proj-field');
    if (projField && app.ui.projectDraft) {
      app.ui.projectDraft[projField] = el.value;
      app.ui.projectDirty = true;
      return;
    }
    const extraPath = el.getAttribute('data-extra-field');
    if (extraPath) {
      setByPath(app.ui, extraPath, el.type === 'range' ? Number(el.value) : el.value);
      if (el.type === 'range') render();
    }
  });

  // ==================== CHANGE DELEGATION (SELECTS, DATES, FILES) ====================
  document.addEventListener('change', (e) => {
    const el = e.target;
    const action = el.getAttribute('data-action');

    if (action === 'change-task-status-select') {
      const task = app.state.tasks.find((t) => t.id === el.getAttribute('data-task-id'));
      if (task) {
        const nowStamp = `${app.state.todayIso}T${app.state.nowTime || '09:30'}`;
        task.status = el.value;
        task.updatedAtIso = nowStamp;
        task.completedAtIso = task.status === 'COMPLETED' ? nowStamp : null;
        if (task.status === 'COMPLETED' && task.recurrenceId) {
          materializeNextOccurrenceIfNeeded(app.state, task);
        }
        appendAudit(app.state, 'KANBAN', 'Tarefa', `"${task.title}" movida para ${LABELS.TaskStatus[task.status]}.`);
        showSnackbar(`Movida para ${LABELS.TaskStatus[task.status]}.`);
      }
      return;
    }

    if (action === 'link-task-to-goal-select') {
      const gId = el.getAttribute('data-goal-id');
      const tId = el.value;
      const task = app.state.tasks.find((t) => t.id === tId);
      if (task && gId) {
        task.goalId = gId;
        showSnackbar('Tarefa vinculada ao objetivo.');
      }
      return;
    }

    if (action === 'link-project-to-goal-select') {
      const gId = el.getAttribute('data-goal-id');
      const pId = el.value;
      const proj = app.state.projects.find((p) => p.id === pId);
      if (proj && gId) {
        proj.goalId = gId;
        showSnackbar('Projeto vinculado ao objetivo.');
      }
      return;
    }

    if (action === 'upload-backup-file') {
      const file = el.files && el.files[0];
      if (!file) return;
      const reader = new FileReader();
      reader.onload = () => {
        app.ui.backupImportText = String(reader.result || '');
        validateAndPreviewBackupJson(app.ui.backupImportText);
      };
      reader.readAsText(file);
      return;
    }

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
      return;
    }

    const taskField = el.getAttribute('data-task-field');
    if (taskField && app.ui.taskDraft) {
      app.ui.taskDraft[taskField] = el.value;
      app.ui.taskDirty = true;
      render();
      return;
    }

    const apptField = el.getAttribute('data-appt-field');
    if (apptField && app.ui.apptDraft) {
      app.ui.apptDraft[apptField] = el.value;
      app.ui.apptDirty = true;
      render();
      return;
    }

    const projField = el.getAttribute('data-proj-field');
    if (projField && app.ui.projectDraft) {
      app.ui.projectDraft[projField] = el.value;
      app.ui.projectDirty = true;
      render();
      return;
    }

    const extraPath = el.getAttribute('data-extra-field');
    if (extraPath) {
      setByPath(app.ui, extraPath, el.value);
      render();
    }
  });

  document.addEventListener('DOMContentLoaded', render);

  // Exposição controlada para inspeção/automação em testes offline sem alterar comportamento em browser
  window.NexusPilot.App = {
    getState: () => app.state,
    getUi: () => app.ui,
    render
  };
})();
