/*
 * NEXUS Web Mirror Pilot — FinanceScreen (Resumo, Lançamentos, Cartões, Parcelas/Recorrentes, Orçamentos/Metas)
 * Fonte Android: FinanceScreen.kt
 */
(function () {
  const {
    LABELS,
    formatBrl,
    formatDateBr,
    formatMonthTitle,
    computeFinanceSelectors
  } = window.NexusPilot.Data;
  const {
    esc,
    ICONS,
    renderSectionHeader,
    renderAlertBanner,
    renderTransactionRowCard,
    renderMonthYearSelectorBar
  } = window.NexusPilot.Primitives;
  const {
    renderBarGroupChart,
    renderDonutChart,
    renderProgressBarRow
  } = window.NexusPilot.Charts;

  const FINANCE_TABS = [
    { key: 'OVERVIEW', label: 'Resumo' },
    { key: 'TRANSACTIONS', label: 'Lançamentos' },
    { key: 'CARDS', label: 'Cartões' },
    { key: 'RECURRING', label: 'Parcelas/Recorrentes' },
    { key: 'PLANNING', label: 'Orçamentos/Metas' }
  ];

  function renderOverviewTab(state, ui, fin) {
    const balanceCents = state.declaredBalanceCents;
    const catScope = ui.financeCategoryScope || 'PAID_AND_PLANNED';
    const rawCatPoints = catScope === 'PAID_ONLY' ? fin.categorySpendPaidOnly : fin.categorySpendPaidAndPlanned;
    const totalCatCents = rawCatPoints.reduce((acc, p) => acc + p.totalCents, 0);
    const slices = rawCatPoints.map((p, idx) => ({
      id: p.categoryId || `__none_${idx}`,
      label: p.categoryName,
      valueCents: p.totalCents,
      percentage: totalCatCents > 0 ? (p.totalCents / totalCatCents) * 100 : 0,
      color: p.colorHex
    }));

    const selectedSliceId = ui.financeDonutSliceId;
    const matchingSliceTxs = selectedSliceId
      ? state.transactions.filter((tx) =>
          (tx.dueDateIso || '').startsWith(state.selectedMonthIso) &&
          tx.type === 'EXPENSE' &&
          tx.status !== 'CANCELLED' &&
          (catScope === 'PAID_ONLY' ? tx.status === 'SETTLED' : true) &&
          (selectedSliceId.startsWith('__none') ? !tx.categoryId : tx.categoryId === selectedSliceId)
        )
      : [];

    const cashflowMode = ui.financeCashflowMode || 'REALIZED';
    const barEntries = fin.cashflow6Months.map((m) => ({
      id: m.yearMonthIso,
      label: m.yearMonthLabel,
      primaryValueCents: cashflowMode === 'REALIZED' ? m.realizedIncome : m.plannedIncome,
      secondaryValueCents: cashflowMode === 'REALIZED' ? m.realizedExpense : m.plannedExpense
    }));
    const selectedBarId = ui.financeBarMonthId || '2026-11';

    return `
      <div data-nexus-component="FinanceOverviewTab" data-nexus-source="FinanceScreen.kt:OverviewTabContent" data-nexus-status="reproduzido" style="display:flex;flex-direction:column;gap:12px">
        ${renderMonthYearSelectorBar(state.selectedMonthIso, '2026-11')}

        <div class="nx-grid-finance-top">
          <div class="nx-card" data-nexus-component="FinanceDeclaredBalanceCard" data-nexus-source="FinanceScreen.kt:OverviewTabContent" data-nexus-status="reproduzido">
            <div class="nx-row-between">
              <span class="nx-label-md nx-text-secondary">Saldo informado</span>
              <button type="button" class="nx-btn-text" data-action="open-balance-dialog" style="display:inline-flex;align-items:center;gap:4px">
                ${ICONS.edit} <span>Atualizar</span>
              </button>
            </div>
            <div class="nx-headline-md tnum" style="color:${balanceCents >= 0 ? 'var(--nx-highlight)' : 'var(--nx-danger)'}">
              ${esc(formatBrl(balanceCents))}
            </div>
            <div class="nx-row-between" style="margin-top:4px">
              <button type="button" class="nx-btn-outlined" style="flex:1" data-action="new-tx" data-tx-type="INCOME">+ Receita</button>
              <button type="button" class="nx-btn-outlined" style="flex:1" data-action="new-tx" data-tx-type="EXPENSE">+ Despesa</button>
            </div>
          </div>

          <div class="nx-card" data-nexus-component="FinanceMonthSummaryCard" data-nexus-source="FinanceScreen.kt:OverviewTabContent" data-nexus-status="reproduzido">
            <div class="nx-row-between">
              <span class="nx-title-md">Totais de ${esc(formatMonthTitle(state.selectedMonthIso))}</span>
              <span class="nx-label-md tnum" style="font-weight:600;color:${fin.monthResultCents >= 0 ? 'var(--nx-positive)' : 'var(--nx-danger)'}">
                Resultado: ${esc(formatBrl(fin.monthResultCents))}
              </span>
            </div>
            <div class="nx-row-2col tnum">
              <div>
                <div class="nx-label-sm nx-text-secondary">Recebido</div>
                <div class="nx-title-sm nx-positive">${esc(formatBrl(fin.receivedIncomeCents))}</div>
                <div class="nx-label-sm nx-text-secondary">Previsto: ${esc(formatBrl(fin.plannedIncomeCents))}</div>
              </div>
              <div>
                <div class="nx-label-sm nx-text-secondary">Pago</div>
                <div class="nx-title-sm">${esc(formatBrl(fin.paidExpenseCents))}</div>
                <div class="nx-label-sm nx-text-secondary">A vencer: ${esc(formatBrl(fin.pendingExpenseCents))}</div>
              </div>
            </div>
          </div>
        </div>

        ${fin.overdueCount > 0 ? renderAlertBanner({
          title: `${fin.overdueCount} despesa(s) atrasada(s)`,
          subtitle: `Total vencido: ${formatBrl(fin.overdueExpenseCents)} · Toque para abrir Lançamentos filtrados.`,
          severity: 'danger',
          actionAttr: 'data-action="open-finance-overdue"'
        }) : ''}

        <div class="nx-grid-2">
          <div class="nx-card" data-nexus-component="OverviewCategoryDonutCard" data-nexus-source="FinanceScreen.kt:OverviewCategoryDonutCard" data-nexus-status="reproduzido">
            <div class="nx-row-between">
              <span class="nx-title-md">Despesas por categoria</span>
              <div class="nx-seg-control">
                <button type="button" class="nx-seg-item ${catScope === 'PAID_AND_PLANNED' ? 'is-active' : ''}" data-action="set-finance-cat-scope" data-scope="PAID_AND_PLANNED">Pagos + prev.</button>
                <button type="button" class="nx-seg-item ${catScope === 'PAID_ONLY' ? 'is-active' : ''}" data-action="set-finance-cat-scope" data-scope="PAID_ONLY">Só pagos</button>
              </div>
            </div>
            ${renderDonutChart({
              slices,
              totalCents: totalCatCents,
              centerLabel: formatMonthTitle(state.selectedMonthIso),
              selectedSliceId,
              sliceAction: 'select-finance-donut-slice'
            })}
            ${selectedSliceId ? `
              <hr class="nx-divider"/>
              <div class="nx-row-between">
                <span class="nx-label-md nx-text-secondary">${matchingSliceTxs.length} lançamento(s) na categoria</span>
                <button type="button" class="nx-btn-text" data-action="filter-tx-by-slice" data-slice-id="${esc(selectedSliceId)}">Ver todos</button>
              </div>
              ${matchingSliceTxs.slice(0, 3).map((tx) => renderTransactionRowCard(tx, state, { compact: true })).join('')}
            ` : ''}
          </div>

          <div class="nx-card" data-nexus-component="OverviewSixMonthBarsCard" data-nexus-source="FinanceScreen.kt:OverviewSixMonthBarsCard" data-nexus-status="reproduzido">
            <div class="nx-row-between">
              <span class="nx-title-md">Receitas × despesas (6 meses)</span>
              <div class="nx-seg-control">
                <button type="button" class="nx-seg-item ${cashflowMode === 'REALIZED' ? 'is-active' : ''}" data-action="set-finance-cashflow-mode" data-mode="REALIZED">Realizado</button>
                <button type="button" class="nx-seg-item ${cashflowMode === 'PLANNED' ? 'is-active' : ''}" data-action="set-finance-cashflow-mode" data-mode="PLANNED">Previsto</button>
              </div>
            </div>
            ${renderBarGroupChart({
              entries: barEntries,
              primaryLabel: cashflowMode === 'REALIZED' ? 'Recebido' : 'Receita prev.',
              secondaryLabel: cashflowMode === 'REALIZED' ? 'Pago' : 'Despesa prev.',
              selectedEntryId: selectedBarId,
              actionAttr: 'data-action="select-finance-bar-month"'
            })}
          </div>
        </div>

        <div class="nx-grid-2">
          <div class="nx-card" data-nexus-component="OverviewBudgetsCard" data-nexus-source="FinanceScreen.kt:OverviewBudgetsCard" data-nexus-status="reproduzido">
            <div class="nx-row-between">
              <span class="nx-title-md">Orçamentos por categoria</span>
              <button type="button" class="nx-btn-text" data-action="open-finance-tab" data-finance-tab="PLANNING">Gerenciar</button>
            </div>
            ${fin.budgetProgress.map((bp) => renderProgressBarRow({
              label: bp.category.name,
              currentCents: bp.spentCents,
              targetCents: bp.budget.limitCents,
              usedPercent: bp.usedPercent,
              exceededCents: bp.exceededCents
            })).join('')}
          </div>

          <div class="nx-card" data-nexus-component="MarketSummarySimulatedCard" data-nexus-source="FinanceScreen.kt:MarketSummarySection" data-nexus-status="simulado">
            <div class="nx-row-between">
              <span class="nx-title-md">Mercado & Referência (Simulado)</span>
              <span class="pilot-trace-tag simulado">Cotações fictícias offline</span>
            </div>
            <div style="display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:8px">
              ${state.marketDemoQuotes.map((q) => `
                <div class="nx-card is-elevated" style="padding:10px;gap:2px">
                  <span class="nx-label-sm nx-text-secondary">${esc(q.symbol)}</span>
                  <span class="nx-title-sm tnum">${esc(q.value)}</span>
                  <span class="nx-label-sm tnum" style="color:${q.positive ? 'var(--nx-positive)' : 'var(--nx-danger)'}">${esc(q.change)}</span>
                </div>
              `).join('')}
            </div>
          </div>
        </div>
      </div>
    `;
  }

  function filterTransactions(state, filter) {
    return state.transactions.filter((tx) => {
      if (filter.monthScope !== 'ALL' && !(tx.dueDateIso || '').startsWith(state.selectedMonthIso)) return false;
      if (filter.type && tx.type !== filter.type) return false;
      if (filter.status && tx.status !== filter.status) return false;
      if (filter.categoryId && tx.categoryId !== filter.categoryId) return false;
      if (filter.paymentMethod && tx.paymentMethod !== filter.paymentMethod) return false;
      return true;
    }).sort((a, b) => (b.dueDateIso || '').localeCompare(a.dueDateIso || ''));
  }

  function renderTransactionsTab(state, ui) {
    const filter = ui.financeFilter;
    const filtered = filterTransactions(state, filter);
    const activeFiltersCount = [filter.type, filter.status, filter.categoryId, filter.paymentMethod, filter.monthScope === 'ALL' ? 'ALL' : null].filter(Boolean).length;

    return `
      <div data-nexus-component="TransactionsTabContent" data-nexus-source="FinanceScreen.kt:TransactionsTabContent" data-nexus-status="reproduzido" style="display:flex;flex-direction:column;gap:12px">
        ${renderMonthYearSelectorBar(state.selectedMonthIso, '2026-11')}

        <div class="nx-row-between">
          <span class="nx-title-md tnum">${filtered.length} lançamento(s)</span>
          <div style="display:flex;gap:8px;align-items:center">
            <button type="button" class="nx-btn-outlined ${activeFiltersCount > 0 ? 'is-selected' : ''}" data-action="open-finance-filter-sheet">
              ${ICONS.filter} <span>Filtros${activeFiltersCount > 0 ? ` (${activeFiltersCount})` : ''}</span>
            </button>
            <button type="button" class="nx-btn-outlined" data-action="export-finance-csv">
              ${ICONS.download} <span>CSV</span>
            </button>
          </div>
        </div>

        ${activeFiltersCount > 0 ? `
          <div class="nx-row-between" style="background:var(--nx-surface-elevated);padding:8px 12px;border-radius:10px;border:1px solid var(--nx-border)">
            <span class="nx-label-md nx-text-secondary">
              Filtros ativos: ${[
                filter.type ? LABELS.TransactionType[filter.type] : null,
                filter.status ? LABELS.FinancialStatus[filter.status] : null,
                filter.categoryId ? (state.categories.find((c) => c.id === filter.categoryId)?.name || 'Categoria') : null,
                filter.paymentMethod ? LABELS.PaymentMethod[filter.paymentMethod] : null,
                filter.monthScope === 'ALL' ? 'Todos os meses' : null
              ].filter(Boolean).join(' · ')}
            </span>
            <button type="button" class="nx-btn-text" data-action="clear-finance-filters">Limpar</button>
          </div>
        ` : ''}

        ${filtered.length ? `
          <div style="display:flex;flex-direction:column;gap:8px">
            ${filtered.map((tx) => renderTransactionRowCard(tx, state)).join('')}
          </div>
        ` : `
          <div class="nx-card" style="align-items:center;text-align:center;padding:24px">
            <span class="nx-title-md">Nenhum lançamento com os filtros atuais</span>
            <button type="button" class="nx-btn-text" data-action="clear-finance-filters">Limpar filtros</button>
          </div>
        `}
      </div>
    `;
  }

  function renderCardsTab(state, ui, fin) {
    const highlightedStmtId = ui.highlightedStatementId;
    return `
      <div data-nexus-component="CardsTabContent" data-nexus-source="FinanceScreen.kt:CardsTabContent" data-nexus-status="reproduzido" style="display:flex;flex-direction:column;gap:12px">
        ${renderSectionHeader({
          title: 'Cartões e faturas',
          trailing: `${fin.cardsWithStatements.length} cartões`,
          actionLabel: '+ Novo lançamento',
          actionAttr: 'data-action="new-tx" data-tx-type="EXPENSE"'
        })}

        ${fin.cardsWithStatements.map((cw) => {
          const usedCents = Math.max(0, cw.card.totalLimitCents - cw.computedRemainingLimitCents);
          const usedPct = cw.card.totalLimitCents > 0 ? Math.min(100, Math.round((usedCents * 100) / cw.card.totalLimitCents)) : 0;
          return `
            <div class="nx-card" data-nexus-component="CreditCardOverviewCard" data-nexus-source="FinanceScreen.kt:CardsTabContent" data-nexus-status="reproduzido" data-nexus-entity="card:${esc(cw.card.id)}">
              <div class="nx-row-between">
                <div>
                  <div class="nx-title-lg">${esc(cw.card.name)}</div>
                  <div class="nx-label-md nx-text-secondary">${esc(cw.card.institution)} · Fecha dia ${cw.card.closingDay} · Vence dia ${cw.card.dueDay}</div>
                </div>
                <span class="nx-accent">${ICONS.card}</span>
              </div>

              <div style="display:flex;flex-direction:column;gap:4px;margin-top:4px">
                <div class="nx-row-between nx-label-md tnum">
                  <span class="nx-text-secondary">Em aberto: <strong class="nx-text-primary">${esc(formatBrl(cw.currentOpenTotalCents))}</strong></span>
                  <span class="nx-text-secondary">Disponível: <strong class="nx-positive">${esc(formatBrl(cw.computedRemainingLimitCents))}</strong></span>
                </div>
                <div class="nx-progress-track">
                  <div class="nx-progress-fill" style="width:${usedPct}%;background:${usedPct > 85 ? 'var(--nx-danger)' : 'var(--nx-accent-neon)'}"></div>
                </div>
                <span class="nx-label-sm nx-text-secondary tnum">Limite total: ${esc(formatBrl(cw.card.totalLimitCents))} (${usedPct}% comprometido)</span>
              </div>

              <hr class="nx-divider"/>
              <div class="nx-title-sm">Faturas</div>
              ${cw.statements.map((sw) => {
                const isHighlighted = highlightedStmtId === sw.statement.id;
                const statusColor = sw.statement.status === 'OVERDUE' ? 'var(--nx-danger)' : sw.statement.status === 'PAID' ? 'var(--nx-positive)' : 'var(--nx-warning)';
                return `
                  <div class="nx-card is-elevated ${isHighlighted ? 'is-focused-highlight' : ''}" data-nexus-component="CreditCardStatementCard" data-nexus-source="FinanceScreen.kt:CardsTabContent" data-nexus-status="reproduzido" data-nexus-entity="statement:${esc(sw.statement.id)}" id="stmt-anchor-${esc(sw.statement.id)}">
                    <div class="nx-row-between">
                      <div>
                        <div class="nx-title-md tnum">Fatura ${esc(sw.statement.yearMonth)}</div>
                        <div class="nx-label-md tnum" style="color:${statusColor};font-weight:600">
                          ${esc(LABELS.StatementStatus[sw.statement.status])} · Vence em ${esc(formatDateBr(sw.statement.dueDateIso))}
                        </div>
                      </div>
                      <div class="nx-title-md tnum" style="font-weight:600">${esc(formatBrl(sw.totalCents))}</div>
                    </div>

                    ${sw.transactions.length ? `
                      <div style="display:flex;flex-direction:column;gap:4px;margin-top:4px">
                        <div class="nx-label-sm nx-text-secondary">Itens vinculados nesta fatura (toque para abrir):</div>
                        ${sw.transactions.map((tx) => {
                          const linkedEv = state.events.find((e) => e.id === tx.eventId);
                          return `
                            <button type="button" class="nx-legend-btn" style="background:var(--nx-surface);border:1px solid var(--nx-border)" data-action="open-tx" data-tx-id="${esc(tx.id)}">
                              <span style="display:flex;flex-direction:column;gap:2px;min-width:0">
                                <span class="nx-body-md" style="font-weight:500">${esc(tx.description)}</span>
                                <span class="nx-label-sm nx-text-secondary">
                                  ${esc(formatDateBr(tx.dueDateIso))}${linkedEv ? ` · Evento: ${esc(linkedEv.name)}` : ''}
                                </span>
                              </span>
                              <span class="nx-label-lg tnum" style="font-weight:600">${esc(formatBrl(tx.amountCents))}</span>
                            </button>
                          `;
                        }).join('')}
                      </div>
                    ` : '<span class="nx-label-sm nx-text-secondary">Nenhum lançamento nesta fatura.</span>'}

                    <div class="nx-row-end" style="margin-top:4px">
                      ${sw.statement.status === 'OPEN' ? `<button type="button" class="nx-btn-text" data-action="close-statement" data-statement-id="${esc(sw.statement.id)}">Fechar fatura</button>` : ''}
                      ${sw.statement.status !== 'PAID' ? `<button type="button" class="nx-btn-text nx-btn-positive-text" data-action="pay-statement" data-statement-id="${esc(sw.statement.id)}">Pagar fatura</button>` : '<span class="nx-label-sm nx-positive">Fatura quitada</span>'}
                    </div>
                  </div>
                `;
              }).join('')}
            </div>
          `;
        }).join('')}
      </div>
    `;
  }

  function renderRecurringBoundaryTab(state) {
    const recurringTxs = state.transactions.filter((t) => t.creditCardId || t.id === 'tx_aluguel' || t.id === 'tx_internet' || t.id === 'tx_cloud_dez');
    return `
      <div data-nexus-component="FinanceRecurringBoundaryTab" data-nexus-source="FinanceScreen.kt:RecurringTabContent" data-nexus-status="parcial" style="display:flex;flex-direction:column;gap:12px">
        ${renderAlertBanner({
          title: 'Parcelas e Recorrentes (visão resumida na Etapa 1)',
          subtitle: 'Lista as despesas recorrentes/assinaturas derivadas dos dados de Gabriel; regras completas de geração automática serão expandidas na Etapa 2.',
          severity: 'info',
          actionAttr: 'data-action="open-finance-tab" data-finance-tab="TRANSACTIONS"'
        })}
        ${recurringTxs.map((tx) => renderTransactionRowCard(tx, state)).join('')}
      </div>
    `;
  }

  function renderPlanningBoundaryTab(state, fin) {
    const fGoal = state.financialGoals[0];
    const fPct = fGoal && fGoal.targetAmountCents > 0 ? Math.round((fGoal.currentAmountCents * 100) / fGoal.targetAmountCents) : 0;
    return `
      <div data-nexus-component="FinancePlanningBoundaryTab" data-nexus-source="FinanceScreen.kt:PlanningTabContent" data-nexus-status="parcial" style="display:flex;flex-direction:column;gap:12px">
        <div class="nx-card">
          <div class="nx-title-md">Orçamentos de ${esc(formatMonthTitle(state.selectedMonthIso))}</div>
          ${fin.budgetProgress.map((bp) => renderProgressBarRow({
            label: bp.category.name,
            currentCents: bp.spentCents,
            targetCents: bp.budget.limitCents,
            usedPercent: bp.usedPercent,
            exceededCents: bp.exceededCents
          })).join('')}
        </div>
        ${fGoal ? `
          <div class="nx-card">
            <div class="nx-title-md">Metas financeiras</div>
            ${renderProgressBarRow({
              label: fGoal.name,
              currentCents: fGoal.currentAmountCents,
              targetCents: fGoal.targetAmountCents,
              usedPercent: fPct,
              exceededCents: 0
            })}
          </div>
        ` : ''}
      </div>
    `;
  }

  function renderFinanceScreen(state, ui) {
    const activeTab = ui.financeTab || 'OVERVIEW';
    const fin = computeFinanceSelectors(state, state.selectedMonthIso);

    let tabBody = '';
    if (activeTab === 'OVERVIEW') tabBody = renderOverviewTab(state, ui, fin);
    else if (activeTab === 'TRANSACTIONS') tabBody = renderTransactionsTab(state, ui);
    else if (activeTab === 'CARDS') tabBody = renderCardsTab(state, ui, fin);
    else if (activeTab === 'RECURRING') tabBody = renderRecurringBoundaryTab(state);
    else tabBody = renderPlanningBoundaryTab(state, fin);

    return `
      <div class="nx-tabs-bar" data-nexus-component="FinanceScrollableTabs" data-nexus-source="FinanceScreen.kt:FinanceScreen" data-nexus-status="reproduzido" role="tablist">
        ${FINANCE_TABS.map((t) => `
          <button type="button" role="tab" aria-selected="${activeTab === t.key}" class="nx-tab ${activeTab === t.key ? 'is-active' : ''}" data-action="open-finance-tab" data-finance-tab="${t.key}">
            ${esc(t.label)}
          </button>
        `).join('')}
      </div>
      <div class="nx-screen-scroll">
        ${tabBody}
      </div>
    `;
  }

  window.NexusPilot = window.NexusPilot || {};
  window.NexusPilot.Screens = window.NexusPilot.Screens || {};
  window.NexusPilot.Screens.Finance = {
    renderFinanceScreen,
    filterTransactions
  };
})();
