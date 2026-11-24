/*
 * NEXUS Web Mirror Pilot — HomeScreen
 * Fonte Android: HomeScreen.kt
 */
(function () {
  const {
    LABELS,
    formatBrl,
    formatDateBr,
    formatDateByPattern,
    computeFinanceSelectors,
    computeTaskDistribution,
    computeEventDetails,
    computeDailyBriefing,
    computeGoalDetails
  } = window.NexusPilot.Data;
  const {
    esc,
    ICONS,
    renderSectionHeader,
    renderAlertBanner,
    renderTaskRowCard,
    renderEventSummaryCard,
    renderAppointmentCompactCard
  } = window.NexusPilot.Primitives;
  const {
    renderTaskRingChart,
    renderBarGroupChart,
    renderDonutChart,
    renderProgressBarRow
  } = window.NexusPilot.Charts;

  function renderHomeScreen(state, ui) {
    const fin = computeFinanceSelectors(state, '2026-11');
    const taskDist = computeTaskDistribution(state);
    const briefing = computeDailyBriefing(state);
    const activeBlocks = [...state.blocks]
      .filter((b) => b.enabled)
      .sort((a, b) => a.order - b.order);

    const htmlBlocks = [];

    activeBlocks.forEach((block) => {
      switch (block.type) {
        case 'GREETING': {
          if (!state.personalization.greetingEnabled) break;
          htmlBlocks.push(`
            <div data-nexus-component="GreetingBlock" data-nexus-source="HomeScreen.kt:HomeScreen" data-nexus-status="reproduzido" style="display:flex;flex-direction:column;gap:2px;padding-top:4px">
              <div class="nx-row-between">
                <h2 class="nx-headline-md">Boa tarde, ${esc(state.userName)}</h2>
                <button type="button" class="nx-icon-btn" data-action="open-subroute" data-subroute="CUSTOMIZE" aria-label="Personalizar painel" title="Personalizar painel">
                  ${ICONS.customize}
                </button>
              </div>
              <span class="nx-label-lg nx-text-secondary">${esc(formatDateByPattern(state.todayIso, state.settings.dateFormatPattern))}</span>
            </div>
          `);
          break;
        }
        case 'DAILY_QUOTE': {
          if (!state.personalization.showDailyQuoteOnDashboard) break;
          htmlBlocks.push(`
            <button type="button" class="nx-btn-text" style="text-align:left;padding:2px 0;white-space:normal" data-nexus-component="DailyQuoteBlock" data-nexus-source="HomeScreen.kt:HomeScreen" data-nexus-status="reproduzido" data-action="open-subroute" data-subroute="QUOTES">
              <span class="nx-body-md nx-text-secondary" style="font-style:italic">
                “${esc(state.dailyQuote.text)}” ${state.dailyQuote.author ? `<span style="font-style:normal">— ${esc(state.dailyQuote.author)}</span>` : ''}
              </span>
            </button>
          `);
          break;
        }
        case 'TASK_SUMMARY': {
          const allFocusTasks = [...taskDist.overdueTasks, ...taskDist.priorityTasksToday].slice(0, block.maxItems || 4);
          htmlBlocks.push(`
            <section data-nexus-component="TaskSummaryBlock" data-nexus-source="HomeScreen.kt:HomeScreen" data-nexus-status="reproduzido" style="display:flex;flex-direction:column;gap:10px">
              ${renderSectionHeader({
                title: 'Tarefas & Foco',
                trailing: taskDist.overdueCount > 0 ? `${taskDist.overdueCount} atrasada(s)` : `${taskDist.priorityTasksToday.length} hoje`,
                actionLabel: 'Ver todas',
                actionAttr: 'data-action="open-tasks-tab"'
              })}
              <div class="nx-card">
                ${renderTaskRingChart(taskDist, 'Consolidado', { showMetricSummary: state.personalization.showMetricSummaryOnDashboard })}
              </div>
              ${(block.showOverdueOrAlerts && taskDist.overdueCount > 0) ? renderAlertBanner({
                title: `${taskDist.overdueCount} tarefa(s) atrasada(s)`,
                subtitle: 'Toque para revisar na aba Atrasadas.',
                severity: 'danger',
                actionAttr: 'data-action="open-tasks-overdue"'
              }) : ''}
              ${allFocusTasks.length ? `
                <div class="nx-grid-2">
                  ${allFocusTasks.map((t) => renderTaskRowCard(t, state)).join('')}
                </div>
              ` : ''}
            </section>
          `);
          break;
        }
        case 'TODAY_AGENDA': {
          const conflictCount = briefing.todayAppointments.filter((a) => a.conflictTitles && a.conflictTitles.length > 0).length;
          htmlBlocks.push(`
            <section data-nexus-component="TodayAgendaBlock" data-nexus-source="HomeScreen.kt:HomeScreen" data-nexus-status="reproduzido" style="display:flex;flex-direction:column;gap:10px">
              ${renderSectionHeader({
                title: 'Agenda de hoje',
                trailing: `${briefing.todayAppointments.length} compromisso(s)`,
                actionLabel: 'Ver agenda',
                actionAttr: 'data-action="open-agenda-tab"'
              })}
              <button type="button" class="nx-card is-elevated is-clickable" data-nexus-component="RoutineBriefingEntryCard" data-nexus-source="HomeScreen.kt:RoutineBriefingEntry" data-nexus-status="reproduzido" data-action="open-routine-assistant" data-routine-tab="0">
                <div class="nx-row-between" style="width:100%">
                  <div style="display:flex;align-items:center;gap:10px">
                    <span class="nx-accent" style="display:inline-flex">${ICONS.sun}</span>
                    <div style="display:flex;flex-direction:column;gap:2px;text-align:left">
                      <span class="nx-title-sm">Assistente de Rotina · Briefing Diário</span>
                      <span class="nx-label-md nx-text-secondary tnum">
                        ${briefing.todayAppointments.length} compromissos · ${briefing.overdueTasks.length + briefing.todayTasks.length} tarefas · ${briefing.overdueBills.length + briefing.billsDueToday.length} contas
                      </span>
                    </div>
                  </div>
                  <span class="nx-label-md" style="font-weight:600;color:${briefing.isReviewedToday ? 'var(--nx-positive)' : 'var(--nx-accent-neon)'}">
                    ${briefing.isReviewedToday ? 'Revisado' : 'Abrir'}
                  </span>
                </div>
              </button>
              ${(block.showOverdueOrAlerts && conflictCount > 0) ? renderAlertBanner({
                title: 'Conflito de horário detectado hoje',
                subtitle: `${conflictCount} compromissos com sobreposição entre 10:00 e 11:15.`,
                severity: 'warning',
                actionAttr: 'data-action="open-agenda-tab"'
              }) : ''}
              <div class="nx-grid-2">
                ${briefing.todayAppointments.slice(0, block.maxItems || 3).map((a) => renderAppointmentCompactCard(a, true)).join('')}
              </div>
            </section>
          `);
          break;
        }
        case 'FINANCE_SUMMARY': {
          const nextTx = fin.nextDueTransaction;
          const balanceCents = state.declaredBalanceCents;
          htmlBlocks.push(`
            <section data-nexus-component="FinanceSummaryBlock" data-nexus-source="HomeScreen.kt:HomeScreen" data-nexus-status="reproduzido" style="display:flex;flex-direction:column;gap:10px">
              ${renderSectionHeader({
                title: 'Resumo financeiro',
                trailing: 'Novembro 2026',
                actionLabel: 'Abrir Finanças',
                actionAttr: 'data-action="open-finance-tab" data-finance-tab="OVERVIEW"'
              })}
              <div class="nx-card" data-nexus-component="BalanceOverviewCard" data-nexus-source="HomeScreen.kt:FinanceSummary" data-nexus-status="reproduzido">
                <div class="nx-row-between">
                  <span class="nx-label-md nx-text-secondary">Saldo informado</span>
                  <button type="button" class="nx-btn-text" data-action="open-balance-dialog" style="display:inline-flex;align-items:center;gap:4px">
                    ${ICONS.edit} <span>Atualizar</span>
                  </button>
                </div>
                <div class="nx-headline-md tnum" style="color:${balanceCents >= 0 ? 'var(--nx-highlight)' : 'var(--nx-danger)'}">${esc(formatBrl(balanceCents))}</div>
                <hr class="nx-divider"/>
                <div class="nx-row-2col tnum">
                  <div>
                    <div class="nx-label-sm nx-text-secondary">Receitas recebidas</div>
                    <div class="nx-title-sm nx-positive">${esc(formatBrl(fin.receivedIncomeCents))}</div>
                  </div>
                  <div>
                    <div class="nx-label-sm nx-text-secondary">Despesas pagas</div>
                    <div class="nx-title-sm">${esc(formatBrl(fin.paidExpenseCents))}</div>
                  </div>
                </div>
                ${(block.showPrimaryDetail && nextTx) ? `
                  <hr class="nx-divider"/>
                  <button type="button" class="nx-legend-btn" style="padding:4px 0" data-action="open-tx" data-tx-id="${esc(nextTx.id)}">
                    <span class="nx-label-md nx-text-secondary">Próximo vencimento: <strong class="nx-text-primary">${esc(nextTx.description)}</strong></span>
                    <span class="nx-label-md nx-warning tnum" style="font-weight:600">${esc(formatBrl(nextTx.amountCents))}</span>
                  </button>
                ` : ''}
              </div>
              ${(block.showOverdueOrAlerts && fin.overdueCount > 0) ? renderAlertBanner({
                title: `${fin.overdueCount} conta(s) atrasada(s)`,
                subtitle: `Total vencido: ${formatBrl(fin.overdueExpenseCents)} · Toque para filtrar lançamentos.`,
                severity: 'danger',
                actionAttr: 'data-action="open-finance-overdue"'
              }) : ''}
            </section>
          `);
          break;
        }
        case 'CHARTS_INDICATORS': {
          const categoryPoints = fin.categorySpendPaidAndPlanned;
          const totalCat = categoryPoints.reduce((acc, p) => acc + p.totalCents, 0);
          const slices = categoryPoints.map((p, idx) => ({
            id: p.categoryId || `__none_${idx}`,
            label: p.categoryName,
            valueCents: p.totalCents,
            percentage: totalCat > 0 ? (p.totalCents / totalCat) * 100 : 0,
            color: p.colorHex
          }));
          const barEntries = fin.cashflow6Months.map((m) => ({
            id: m.yearMonthIso,
            label: m.yearMonthLabel,
            primaryValueCents: m.realizedIncome,
            secondaryValueCents: m.realizedExpense
          }));
          const selectedBarId = ui.homeBarMonthId || '2026-11';
          const selectedSliceId = ui.homeDonutSliceId;

          htmlBlocks.push(`
            <section data-nexus-component="ChartsIndicatorsBlock" data-nexus-source="HomeScreen.kt:HomeScreen" data-nexus-status="reproduzido" style="display:flex;flex-direction:column;gap:10px">
              ${renderSectionHeader({
                title: 'Gráficos e indicadores',
                actionLabel: 'Detalhes',
                actionAttr: 'data-action="open-finance-tab" data-finance-tab="OVERVIEW"'
              })}
              <div class="nx-grid-2">
                <div class="nx-card" data-nexus-component="HomeCashflowCard" data-nexus-source="HomeScreen.kt:HomeCashflowCard" data-nexus-status="reproduzido">
                  <div class="nx-title-md">Fluxo mensal (6 meses)</div>
                  ${renderBarGroupChart({
                    entries: barEntries,
                    primaryLabel: 'Receitas',
                    secondaryLabel: 'Despesas',
                    selectedEntryId: selectedBarId,
                    actionAttr: 'data-action="select-home-bar-month"'
                  })}
                </div>
                <div class="nx-card" data-nexus-component="HomeCategoryDonutCard" data-nexus-source="HomeScreen.kt:HomeCategoryDonutCard" data-nexus-status="reproduzido">
                  <div class="nx-row-between">
                    <div class="nx-title-md">Despesas por categoria</div>
                    <button type="button" class="nx-btn-text" data-action="open-finance-tab" data-finance-tab="OVERVIEW">Filtrar</button>
                  </div>
                  ${renderDonutChart({
                    slices,
                    totalCents: totalCat,
                    centerLabel: 'Nov/2026',
                    selectedSliceId,
                    sliceAction: 'select-home-donut-slice'
                  })}
                </div>
              </div>
            </section>
          `);
          break;
        }
        case 'UPCOMING_EVENTS': {
          const eventCards = state.events
            .filter((e) => e.eventStatus !== 'CANCELLED')
            .slice(0, block.maxItems || 3)
            .map((e) => computeEventDetails(state, e));
          htmlBlocks.push(`
            <section data-nexus-component="UpcomingEventsBlock" data-nexus-source="HomeScreen.kt:HomeScreen" data-nexus-status="reproduzido" style="display:flex;flex-direction:column;gap:10px">
              ${renderSectionHeader({
                title: 'Próximos eventos',
                trailing: `${eventCards.length} evento(s)`,
                actionLabel: 'Ver eventos',
                actionAttr: 'data-action="open-events-tab"'
              })}
              <div class="nx-grid-2">
                ${eventCards.map((ed) => renderEventSummaryCard(ed)).join('')}
              </div>
            </section>
          `);
          break;
        }
        case 'UPCOMING_PAYMENTS': {
          const txs = fin.upcomingPayments.slice(0, block.maxItems || 4);
          const stmt = fin.openStatements.find((sw) => sw.statement.id === 'stmt_uv_2026_11');
          htmlBlocks.push(`
            <section data-nexus-component="UpcomingPaymentsBlock" data-nexus-source="HomeScreen.kt:HomeScreen" data-nexus-status="reproduzido" style="display:flex;flex-direction:column;gap:10px">
              ${renderSectionHeader({
                title: 'Próximos pagamentos',
                trailing: `${fin.upcomingPayments.length} pendente(s)`,
                actionLabel: 'Lançamentos',
                actionAttr: 'data-action="open-finance-tab" data-finance-tab="TRANSACTIONS"'
              })}
              ${stmt ? `
                <button type="button" class="nx-card is-elevated is-clickable" data-action="open-card-statement" data-statement-id="${esc(stmt.statement.id)}">
                  <div class="nx-row-between" style="width:100%">
                    <span class="nx-title-sm" style="display:inline-flex;align-items:center;gap:8px">${ICONS.card} Fatura ${esc(stmt.card.name)} (${esc(stmt.statement.yearMonth)})</span>
                    <span class="nx-title-sm nx-danger tnum">${esc(formatBrl(stmt.totalCents))}</span>
                  </div>
                  <span class="nx-label-md nx-text-secondary tnum">Vencimento ${esc(formatDateBr(stmt.statement.dueDateIso))} · ${esc(LABELS.StatementStatus[stmt.statement.status])} · Toque para abrir Cartões</span>
                </button>
              ` : ''}
              <div class="nx-grid-2">
                ${txs.map((tx) => `
                  <button type="button" class="nx-card is-elevated is-clickable" data-action="open-tx" data-tx-id="${esc(tx.id)}">
                    <div class="nx-row-between" style="width:100%">
                      <span class="nx-title-sm" style="text-align:left">${esc(tx.description)}</span>
                      <span class="nx-title-sm tnum" style="color:${tx.status === 'OVERDUE' ? 'var(--nx-danger)' : 'var(--nx-text-primary)'}">${esc(formatBrl(tx.amountCents))}</span>
                    </div>
                    <span class="nx-label-md nx-text-secondary tnum">${esc(formatDateBr(tx.dueDateIso))} · ${esc(LABELS.FinancialStatus[tx.status])}</span>
                  </button>
                `).join('')}
              </div>
            </section>
          `);
          break;
        }
        case 'GOALS': {
          const pGoal = state.personalGoals[0];
          const pDetails = pGoal ? computeGoalDetails(state, pGoal) : null;
          const fGoal = state.financialGoals[0];
          const fPct = fGoal && fGoal.targetAmountCents > 0 ? Math.round((fGoal.currentAmountCents * 100) / fGoal.targetAmountCents) : 0;
          htmlBlocks.push(`
            <section data-nexus-component="GoalsBlock" data-nexus-source="HomeScreen.kt:HomeScreen" data-nexus-status="reproduzido" style="display:flex;flex-direction:column;gap:10px">
              ${renderSectionHeader({
                title: 'Metas e objetivos',
                actionLabel: 'Planejamento',
                actionAttr: 'data-action="open-subroute" data-subroute="PLANNING"'
              })}
              <div class="nx-grid-2">
                ${pDetails ? `
                  <button type="button" class="nx-card is-elevated is-clickable" data-action="open-personal-goal-in-planning" data-goal-id="${esc(pGoal.id)}">
                    <span class="nx-label-sm nx-text-secondary">Objetivo pessoal</span>
                    <div class="nx-title-sm" style="text-align:left">${esc(pGoal.title)}</div>
                    <div class="nx-progress-track"><div class="nx-progress-fill" style="width:${pDetails.progressPercent}%;background:var(--nx-accent-neon)"></div></div>
                    <span class="nx-label-sm nx-text-secondary tnum">${pDetails.progressPercent}% · ${pDetails.completedMilestonesCount}/${pDetails.milestones.length} marcos · ${pDetails.linkedTasks.length} tarefas</span>
                  </button>
                ` : ''}
                ${fGoal ? `
                  <button type="button" class="nx-card is-elevated is-clickable" data-action="open-finance-tab" data-finance-tab="PLANNING">
                    <span class="nx-label-sm nx-text-secondary">Meta financeira</span>
                    ${renderProgressBarRow({
                      label: fGoal.name,
                      currentCents: fGoal.currentAmountCents,
                      targetCents: fGoal.targetAmountCents,
                      usedPercent: fPct,
                      exceededCents: 0
                    })}
                  </button>
                ` : ''}
              </div>
            </section>
          `);
          break;
        }
        default:
          break;
      }
    });

    htmlBlocks.push(`
      <div style="padding-top:4px;display:flex;justify-content:center">
        <button type="button" class="nx-btn-outlined" data-action="open-subroute" data-subroute="CUSTOMIZE">
          ${ICONS.customize} <span>Personalizar painel</span>
        </button>
      </div>
    `);

    return htmlBlocks.join('');
  }

  window.NexusPilot = window.NexusPilot || {};
  window.NexusPilot.Screens = window.NexusPilot.Screens || {};
  window.NexusPilot.Screens.Home = {
    renderHomeScreen
  };
})();
