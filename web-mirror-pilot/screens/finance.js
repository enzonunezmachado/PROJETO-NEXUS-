/*
 * NEXUS Web Mirror Pilot — FinanceScreen (Resumo, Lançamentos, Cartões, Parcelas/Recorrentes, Orçamentos/Metas/Empréstimos/Auditoria)
 * Fonte Android: FinanceScreen.kt
 */
(function () {
  const {
    LABELS,
    formatBrl,
    formatDateBr,
    formatMonthTitle,
    formatMarketValue,
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
    const fc = fin.forecast30Days;

    const marketHighlightCodes = ['USD-BRL', 'EUR-BRL', 'BTC-BRL', 'ETH-BRL'];

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

          <div class="nx-card" data-nexus-component="OverviewForecast30DaysCard" data-nexus-source="FinanceScreen.kt:OverviewForecast30DaysCard" data-nexus-status="reproduzido">
            <div class="nx-row-between">
              <span class="nx-title-md">Previsão dos próximos 30 dias</span>
              <button type="button" class="nx-btn-text" data-action="open-finance-tab" data-finance-tab="TRANSACTIONS">Lançamentos</button>
            </div>
            <div class="nx-row-2col tnum">
              <div>
                <div class="nx-label-sm nx-text-secondary">Receitas previstas</div>
                <div class="nx-title-sm nx-positive">${esc(formatBrl(fc.plannedIncomeCents))}</div>
              </div>
              <div>
                <div class="nx-label-sm nx-text-secondary">Despesas a vencer</div>
                <div class="nx-title-sm nx-warning">${esc(formatBrl(fc.upcomingExpensesCents))}</div>
              </div>
            </div>
            ${fc.topUpcomingExpenses.length ? `
              <hr class="nx-divider"/>
              ${fc.topUpcomingExpenses.map((tx) => renderTransactionRowCard(tx, state, { compact: true })).join('')}
            ` : '<span class="nx-body-sm">Nenhuma despesa prevista nos próximos 30 dias.</span>'}
          </div>
        </div>

        <div class="nx-card" data-nexus-component="MarketSummarySimulatedCard" data-nexus-source="FinanceScreen.kt:MarketSummarySection" data-nexus-status="simulado">
          <div class="nx-row-between">
            <div style="display:flex;align-items:center;gap:8px">
              <span class="nx-title-md">Moedas & Criptomoedas</span>
              <span class="pilot-trace-tag simulado">Simulado</span>
            </div>
            <button type="button" class="nx-btn-text" data-action="open-subroute" data-subroute="MARKET">Ver Mercado (12)</button>
          </div>
          <div class="nx-grid-2">
            ${marketHighlightCodes.map((code) => {
              const q = state.marketQuotes[code];
              if (!q) return '';
              const cat = code.endsWith('BRL') && (code.startsWith('USD') || code.startsWith('EUR')) ? 'CURRENCY' : 'CRYPTO';
              return `
                <button type="button" class="nx-card is-elevated is-clickable" style="padding:10px 12px" data-action="open-market-asset" data-asset-code="${esc(code)}">
                  <div class="nx-row-between" style="width:100%">
                    <span class="nx-title-sm">${esc(code)}</span>
                    <span class="nx-badge ${q.changePercent >= 0 ? 'is-positive' : 'is-danger'} tnum">${q.changePercent >= 0 ? '+' : ''}${q.changePercent.toFixed(2)}%</span>
                  </div>
                  <span class="nx-title-md tnum">${esc(formatMarketValue(q.price, cat, 'BRL'))}</span>
                </button>
              `;
            }).join('')}
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
        <div class="nx-row-between">
          <div>
            <h3 class="nx-title-md">Cartões e faturas</h3>
            <span class="nx-label-sm nx-text-secondary tnum">${fin.cardsWithStatements.length} cartão(ões)</span>
          </div>
          <div style="display:flex;gap:6px">
            <button type="button" class="nx-btn-outlined" data-action="new-credit-card">+ Cartão</button>
            <button type="button" class="nx-btn-outlined" data-action="new-tx" data-tx-type="EXPENSE">+ Despesa</button>
          </div>
        </div>

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
                <div style="display:flex;align-items:center;gap:4px">
                  <button type="button" class="nx-icon-btn" data-action="edit-credit-card" data-card-id="${esc(cw.card.id)}" aria-label="Editar cartão">${ICONS.edit}</button>
                  <span class="nx-accent">${ICONS.card}</span>
                </div>
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

  function renderRecurringTab(state) {
    return `
      <div data-nexus-component="RecurringAndInstallmentsTabContent" data-nexus-source="FinanceScreen.kt:RecurringAndInstallmentsTabContent" data-nexus-status="reproduzido" style="display:flex;flex-direction:column;gap:14px">
        ${renderSectionHeader({
          title: 'Parcelamentos',
          trailing: `${state.installmentPurchases.length}`,
          actionLabel: '+ Parcelamento',
          actionAttr: 'data-action="new-installment"'
        })}

        ${state.installmentPurchases.length ? `
          <div class="nx-grid-2">
            ${state.installmentPurchases.map((inst) => {
              const card = state.creditCards.find((c) => c.id === inst.creditCardId);
              const cat = state.categories.find((c) => c.id === inst.categoryId);
              const pct = inst.installmentCount > 0 ? Math.round((inst.paidCount * 100) / inst.installmentCount) : 100;
              const remainingCents = inst.remainingCount * inst.installmentAmountCents;
              return `
                <div class="nx-card is-elevated" data-nexus-entity="installment:${esc(inst.id)}">
                  <div class="nx-row-between">
                    <span class="nx-title-md">${esc(inst.description)}</span>
                    <span class="nx-badge ${inst.remainingCount === 0 ? 'is-positive' : 'is-info'} tnum">${inst.paidCount}/${inst.installmentCount}</span>
                  </div>
                  <div class="nx-label-md nx-text-secondary tnum">
                    Parcela: ${esc(formatBrl(inst.installmentAmountCents))} · Total: ${esc(formatBrl(inst.totalAmountCents))}${card ? ` · ${card.name}` : ''}${cat ? ` · ${cat.name}` : ''}
                  </div>
                  <div class="nx-progress-track">
                    <div class="nx-progress-fill" style="width:${pct}%;background:${inst.remainingCount === 0 ? 'var(--nx-positive)' : 'var(--nx-accent-neon)'}"></div>
                  </div>
                  <div class="nx-row-between nx-label-sm nx-text-secondary tnum">
                    <span>Restam ${inst.remainingCount} parcela(s) (${esc(formatBrl(remainingCents))})</span>
                    <span>1º venc.: ${esc(formatDateBr(inst.firstDueDateIso))}</span>
                  </div>
                  <div class="nx-row-end">
                    ${inst.remainingCount > 0 ? `<button type="button" class="nx-btn-text" data-action="pay-installment-one" data-inst-id="${esc(inst.id)}">Pagar 1 parcela</button>` : ''}
                    ${inst.remainingCount > 1 ? `<button type="button" class="nx-btn-text nx-btn-positive-text" data-action="pay-installment-all" data-inst-id="${esc(inst.id)}">Quitar restantes</button>` : ''}
                    <button type="button" class="nx-btn-text nx-btn-danger-text" data-action="delete-installment" data-inst-id="${esc(inst.id)}">Excluir</button>
                  </div>
                </div>
              `;
            }).join('')}
          </div>
        ` : `<div class="nx-card"><span class="nx-body-sm">Nenhum parcelamento ativo.</span></div>`}

        ${renderSectionHeader({
          title: 'Regras recorrentes',
          trailing: `${state.recurringRules.length}`,
          actionLabel: '+ Recorrente',
          actionAttr: 'data-action="new-recurring-rule"'
        })}

        ${state.recurringRules.length ? `
          <div class="nx-grid-2">
            ${state.recurringRules.map((rec) => {
              const cat = state.categories.find((c) => c.id === rec.categoryId);
              return `
                <div class="nx-card is-elevated" data-nexus-entity="recurring:${esc(rec.id)}">
                  <div class="nx-row-between">
                    <div>
                      <div class="nx-title-md">${esc(rec.description)}</div>
                      <div class="nx-label-md nx-text-secondary tnum">
                        ${esc(LABELS.TransactionType[rec.type])} · ${esc(LABELS.RecurrenceFrequency[rec.frequency])} · Próxima: ${esc(formatDateBr(rec.nextOccurrenceIso))}
                      </div>
                    </div>
                    <span class="nx-title-md tnum" style="font-weight:600;color:${rec.type === 'INCOME' ? 'var(--nx-positive)' : 'var(--nx-text-primary)'}">
                      ${esc(formatBrl(rec.amountCents))}
                    </span>
                  </div>
                  <div class="nx-label-sm nx-text-secondary">${esc(LABELS.PaymentMethod[rec.paymentMethod])}${cat ? ` · ${esc(cat.name)}` : ''}</div>
                  <div class="nx-row-between">
                    <div style="display:flex;gap:6px">
                      <button type="button" class="nx-btn-text" data-action="generate-recurring-tx" data-rec-id="${esc(rec.id)}">Gerar próximo</button>
                      <button type="button" class="nx-btn-text nx-btn-danger-text" data-action="delete-recurring-rule" data-rec-id="${esc(rec.id)}">Excluir</button>
                    </div>
                    <button type="button" class="nx-switch ${rec.isActive ? 'is-on' : ''}" data-action="toggle-recurring-active" data-rec-id="${esc(rec.id)}" aria-label="Ativar ou pausar regra"></button>
                  </div>
                </div>
              `;
            }).join('')}
          </div>
        ` : `<div class="nx-card"><span class="nx-body-sm">Nenhuma regra recorrente cadastrada.</span></div>`}
      </div>
    `;
  }

  function renderPlanningTab(state, fin) {
    return `
      <div data-nexus-component="PlanningAndLoansTabContent" data-nexus-source="FinanceScreen.kt:PlanningAndLoansTabContent" data-nexus-status="reproduzido" style="display:flex;flex-direction:column;gap:14px">
        ${renderSectionHeader({
          title: `Orçamentos de ${formatMonthTitle(state.selectedMonthIso)}`,
          trailing: `${fin.budgetProgress.length}`,
          actionLabel: '+ Orçamento',
          actionAttr: 'data-action="new-budget"'
        })}
        <div class="nx-card">
          ${fin.budgetProgress.length ? fin.budgetProgress.map((bp) => `
            <div style="display:flex;flex-direction:column;gap:4px">
              ${renderProgressBarRow({
                label: bp.category.name,
                currentCents: bp.spentCents,
                targetCents: bp.budget.limitCents,
                usedPercent: bp.usedPercent,
                exceededCents: bp.exceededCents
              })}
              <div class="nx-row-end">
                <button type="button" class="nx-btn-text" data-action="edit-budget" data-budget-id="${esc(bp.budget.id)}">Editar</button>
                <button type="button" class="nx-btn-text nx-btn-danger-text" data-action="delete-budget" data-budget-id="${esc(bp.budget.id)}">Excluir</button>
              </div>
            </div>
          `).join('<hr class="nx-divider"/>') : '<span class="nx-body-sm">Nenhum orçamento configurado.</span>'}
        </div>

        ${renderSectionHeader({
          title: 'Metas financeiras',
          trailing: `${state.financialGoals.length}`,
          actionLabel: '+ Meta',
          actionAttr: 'data-action="new-financial-goal"'
        })}
        <div class="nx-grid-2">
          ${state.financialGoals.map((fg) => {
            const pct = fg.targetAmountCents > 0 ? Math.round((fg.currentAmountCents * 100) / fg.targetAmountCents) : 0;
            return `
              <div class="nx-card is-elevated">
                ${renderProgressBarRow({
                  label: fg.name,
                  currentCents: fg.currentAmountCents,
                  targetCents: fg.targetAmountCents,
                  usedPercent: pct,
                  exceededCents: 0
                })}
                <div class="nx-row-between">
                  <span class="nx-label-sm nx-text-secondary tnum">${fg.targetDateIso ? `Alvo: ${formatDateBr(fg.targetDateIso)}` : 'Sem prazo'}</span>
                  <div style="display:flex;gap:4px">
                    <button type="button" class="nx-btn-text" data-action="open-fgoal-amount" data-fgoal-id="${esc(fg.id)}">Atualizar valor</button>
                    <button type="button" class="nx-btn-text nx-btn-danger-text" data-action="delete-financial-goal" data-fgoal-id="${esc(fg.id)}">Excluir</button>
                  </div>
                </div>
              </div>
            `;
          }).join('') || '<div class="nx-card"><span class="nx-body-sm">Nenhuma meta financeira cadastrada.</span></div>'}
        </div>

        ${renderSectionHeader({
          title: 'Empréstimos e financiamentos',
          trailing: `${state.loans.length}`,
          actionLabel: '+ Empréstimo',
          actionAttr: 'data-action="new-loan"'
        })}
        <div class="nx-grid-2">
          ${state.loans.map((ln) => {
            const paidPct = ln.totalInstallments > 0 ? Math.round((ln.paidInstallments * 100) / ln.totalInstallments) : 100;
            const isSettled = ln.paidInstallments >= ln.totalInstallments || ln.remainingCents <= 0;
            return `
              <div class="nx-card is-elevated" data-nexus-entity="loan:${esc(ln.id)}">
                <div class="nx-row-between">
                  <div>
                    <div class="nx-title-md">${esc(ln.name)}</div>
                    <div class="nx-label-sm nx-text-secondary">${esc(ln.lender)} · Dia ${ln.dueDay}${ln.interestNote ? ` · ${esc(ln.interestNote)}` : ''}</div>
                  </div>
                  <span class="nx-badge ${isSettled ? 'is-positive' : 'is-warning'} tnum">${ln.paidInstallments}/${ln.totalInstallments}</span>
                </div>
                <div class="nx-progress-track">
                  <div class="nx-progress-fill" style="width:${paidPct}%;background:${isSettled ? 'var(--nx-positive)' : 'var(--nx-accent-neon)'}"></div>
                </div>
                <div class="nx-row-between nx-label-md tnum">
                  <span class="nx-text-secondary">Saldo devedor: <strong class="nx-text-primary">${esc(formatBrl(ln.remainingCents))}</strong></span>
                  <span class="nx-text-secondary">Parcela: ${esc(formatBrl(ln.monthlyInstallmentCents))}</span>
                </div>
                <div class="nx-row-end">
                  ${!isSettled ? `<button type="button" class="nx-btn-text nx-btn-positive-text" data-action="pay-loan-installment" data-loan-id="${esc(ln.id)}">Pagar parcela</button>` : ''}
                  <button type="button" class="nx-btn-text" data-action="edit-loan" data-loan-id="${esc(ln.id)}">Editar</button>
                  <button type="button" class="nx-btn-text nx-btn-danger-text" data-action="delete-loan" data-loan-id="${esc(ln.id)}">Excluir</button>
                </div>
              </div>
            `;
          }).join('') || '<div class="nx-card"><span class="nx-body-sm">Nenhum empréstimo registrado.</span></div>'}
        </div>

        ${renderSectionHeader({
          title: 'Histórico recente de alterações',
          trailing: `${state.auditHistory.length}`
        })}
        <div class="nx-card" data-nexus-component="FinanceAuditHistoryCard" data-nexus-source="FinanceScreen.kt:AuditHistorySection" data-nexus-status="reproduzido">
          ${state.auditHistory.slice(0, 8).map((aud) => `
            <div style="display:flex;flex-direction:column;gap:2px;padding:4px 0">
              <div class="nx-row-between">
                <span class="nx-label-sm nx-accent">${esc(aud.actionType)} · ${esc(aud.entityType)}</span>
                <span class="nx-label-sm nx-text-secondary tnum">${esc(aud.timestamp)}</span>
              </div>
              <span class="nx-body-sm" style="color:var(--nx-text-primary)">${esc(aud.summary)}</span>
            </div>
          `).join('<hr class="nx-divider"/>')}
        </div>
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
    else if (activeTab === 'RECURRING') tabBody = renderRecurringTab(state);
    else tabBody = renderPlanningTab(state, fin);

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
