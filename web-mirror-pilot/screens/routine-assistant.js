/*
 * NEXUS Web Mirror Pilot — RoutineAssistantScreen (SubRoute)
 * Fonte Android: RoutineAssistantScreen.kt (DailyBriefingTab, NightReviewTab, NotificationsSettingsTab)
 */
(function () {
  const {
    LABELS,
    formatBrl,
    formatDateBr,
    addDaysIso,
    computeDailyBriefing
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

  function renderStatChip(label, value, tone = 'default') {
    const cls = tone === 'danger' ? 'nx-danger' : tone === 'warning' ? 'nx-warning' : 'nx-text-primary';
    return `
      <div class="nx-card is-elevated" style="padding:10px 12px;gap:2px">
        <span class="nx-label-sm nx-text-secondary">${esc(label)}</span>
        <span class="nx-title-lg tnum ${cls}">${esc(value)}</span>
      </div>
    `;
  }

  function renderDailyBriefingTab(state, briefing) {
    const totalBillsCount = briefing.overdueBills.length + briefing.billsDueToday.length;
    const totalPendingTasksCount = briefing.overdueTasks.length + briefing.todayTasks.length;
    const hasConflicts = briefing.todayAppointments.some((a) => a.conflictTitles && a.conflictTitles.length > 0);
    const conflictCount = briefing.todayAppointments.filter((a) => a.conflictTitles && a.conflictTitles.length > 0).length;
    const allBills = [...briefing.overdueBills, ...briefing.billsDueToday];

    return `
      <div data-nexus-component="DailyBriefingTab" data-nexus-source="RoutineAssistantScreen.kt:DailyBriefingTab" data-nexus-status="reproduzido" style="display:flex;flex-direction:column;gap:12px">
        <div class="nx-card">
          <div class="nx-row-between">
            <div>
              <h3 class="nx-title-lg">Briefing Diário · Hoje</h3>
              <span class="nx-label-md nx-text-secondary tnum">${esc(formatDateBr(state.todayIso))} · Perfil ${esc(state.userName)}</span>
            </div>
            <span class="nx-label-md" style="font-weight:600;color:${briefing.isReviewedToday ? 'var(--nx-positive)' : 'var(--nx-accent-neon)'}">
              ${briefing.isReviewedToday ? 'Revisado hoje' : 'Pendente'}
            </span>
          </div>
          <div class="nx-row-2col" style="margin-top:4px">
            ${renderStatChip('Compromissos', String(briefing.todayAppointments.length))}
            ${renderStatChip('Tarefas do dia', String(totalPendingTasksCount), briefing.overdueTasks.length > 0 ? 'danger' : 'default')}
            ${renderStatChip('Contas vencendo', String(totalBillsCount), briefing.overdueBills.length > 0 ? 'danger' : 'default')}
            ${renderStatChip('Valor em aberto', formatBrl(briefing.totalBillsDueAmountCents), briefing.overdueBills.length > 0 ? 'warning' : 'default')}
          </div>
          <div style="margin-top:6px">
            <button type="button" class="nx-btn-primary" data-action="mark-briefing-reviewed" ${briefing.isReviewedToday ? 'style="background:var(--nx-positive);color:#050505"' : ''}>
              ${briefing.isReviewedToday ? 'Briefing de hoje revisado' : 'Marcar Briefing como revisado'}
            </button>
          </div>
        </div>

        ${hasConflicts ? renderAlertBanner({
          title: 'Conflito de horário na agenda de hoje',
          subtitle: `${conflictCount} compromisso(s) com sobreposição de horário hoje.`,
          severity: 'warning',
          actionAttr: 'data-action="open-agenda-tab"'
        }) : ''}

        ${(briefing.overdueTasks.length || briefing.todayTasks.length) ? `
          <div style="display:flex;flex-direction:column;gap:8px">
            ${renderSectionHeader({
              title: 'Tarefas prioritárias e atrasadas',
              trailing: `${briefing.overdueTasks.length + briefing.todayTasks.length}`,
              actionLabel: 'Ver todas',
              actionAttr: 'data-action="open-tasks-tab"'
            })}
            <div class="nx-grid-2">
              ${[...briefing.overdueTasks, ...briefing.todayTasks].map((t) => renderTaskRowCard(t, state, { showPostpone: true })).join('')}
            </div>
          </div>
        ` : ''}

        ${allBills.length ? `
          <div style="display:flex;flex-direction:column;gap:8px">
            ${renderSectionHeader({
              title: 'Contas vencidas e de hoje',
              trailing: `${allBills.length} · ${formatBrl(briefing.totalBillsDueAmountCents)}`,
              actionLabel: 'Ir para Finanças',
              actionAttr: 'data-action="open-finance-tab" data-finance-tab="TRANSACTIONS"'
            })}
            <div class="nx-grid-2">
              ${allBills.map((tx) => {
                const isOverdue = tx.status === 'OVERDUE';
                return `
                  <div class="nx-card is-elevated" data-nexus-component="RoutineBillActionCard" data-nexus-source="RoutineAssistantScreen.kt:DailyBriefingTab" data-nexus-status="reproduzido">
                    <div class="nx-row-between" style="cursor:pointer" data-action="open-tx" data-tx-id="${esc(tx.id)}">
                      <div style="min-width:0">
                        <div class="nx-title-md">${esc(tx.description)}</div>
                        <div class="nx-label-md tnum" style="color:${isOverdue ? 'var(--nx-danger)' : 'var(--nx-text-secondary)'}">
                          ${isOverdue ? 'Atrasada' : 'Vence hoje'} · ${esc(formatDateBr(tx.dueDateIso))} · ${esc(LABELS.PaymentMethod[tx.paymentMethod] || tx.paymentMethod)}
                        </div>
                      </div>
                      <div class="nx-title-md tnum" style="font-weight:600;color:${isOverdue ? 'var(--nx-danger)' : 'var(--nx-text-primary)'}">${esc(formatBrl(tx.amountCents))}</div>
                    </div>
                    <div class="nx-row-end">
                      <button type="button" class="nx-btn-text" data-action="postpone-tx" data-tx-id="${esc(tx.id)}">Adiar</button>
                      <button type="button" class="nx-btn-text nx-btn-positive-text" data-action="settle-tx" data-tx-id="${esc(tx.id)}">Pagar</button>
                    </div>
                  </div>
                `;
              }).join('')}
            </div>
          </div>
        ` : ''}

        ${briefing.statementsDueTodayOrOverdue.length ? `
          <div style="display:flex;flex-direction:column;gap:8px">
            ${renderSectionHeader({
              title: 'Faturas em atenção',
              trailing: String(briefing.statementsDueTodayOrOverdue.length),
              actionLabel: 'Cartões',
              actionAttr: 'data-action="open-finance-tab" data-finance-tab="CARDS"'
            })}
            ${briefing.statementsDueTodayOrOverdue.map((sw) => `
              <button type="button" class="nx-card is-elevated is-clickable" data-action="open-card-statement" data-statement-id="${esc(sw.statement.id)}">
                <div class="nx-row-between" style="width:100%">
                  <span class="nx-title-md">${esc(sw.card.name)} · ${esc(sw.statement.yearMonth)}</span>
                  <span class="nx-title-md nx-danger tnum">${esc(formatBrl(sw.totalCents))}</span>
                </div>
                <span class="nx-label-md nx-text-secondary tnum">
                  Status: ${esc(LABELS.StatementStatus[sw.statement.status])} · Vencimento ${esc(formatDateBr(sw.statement.dueDateIso))} · Toque para abrir Fatura em Finanças
                </span>
              </button>
            `).join('')}
          </div>
        ` : ''}

        ${briefing.todayAppointments.length ? `
          <div style="display:flex;flex-direction:column;gap:8px">
            ${renderSectionHeader({ title: 'Compromissos de hoje', trailing: String(briefing.todayAppointments.length) })}
            <div class="nx-grid-2">
              ${briefing.todayAppointments.map((a) => renderAppointmentCompactCard(a, true)).join('')}
            </div>
          </div>
        ` : ''}

        ${briefing.todayEvents.length ? `
          <div style="display:flex;flex-direction:column;gap:8px">
            ${renderSectionHeader({
              title: 'Eventos de hoje',
              trailing: String(briefing.todayEvents.length),
              actionLabel: 'Ver Eventos',
              actionAttr: 'data-action="open-events-tab"'
            })}
            <div class="nx-grid-2">
              ${briefing.todayEvents.map((ed) => renderEventSummaryCard(ed)).join('')}
            </div>
          </div>
        ` : ''}
      </div>
    `;
  }

  function renderNightReviewTab(state, briefing) {
    const remainingCount = briefing.overdueTasks.length + briefing.todayTasks.length;
    return `
      <div data-nexus-component="NightReviewTab" data-nexus-source="RoutineAssistantScreen.kt:NightReviewTab" data-nexus-status="reproduzido" style="display:flex;flex-direction:column;gap:12px">
        <div class="nx-card">
          <div class="nx-row-between">
            <div>
              <h3 class="nx-title-lg">Revisão Noturna · Encerramento</h3>
              <span class="nx-label-md nx-text-secondary tnum">${esc(formatDateBr(state.todayIso))}</span>
            </div>
            <span class="nx-label-md" style="font-weight:600;color:${briefing.isReviewedTonight ? 'var(--nx-positive)' : 'var(--nx-warning)'}">
              ${briefing.isReviewedTonight ? 'Fechamento concluído' : 'Aguardando fechamento'}
            </span>
          </div>
          <div class="nx-row-2col" style="margin-top:4px">
            ${renderStatChip('Concluídas no ciclo', String(briefing.completedTasksToday.length))}
            ${renderStatChip('Pendentes hoje/atraso', String(remainingCount), remainingCount > 0 ? 'warning' : 'default')}
          </div>
          <p class="nx-body-sm" style="margin-top:4px">
            ${remainingCount > 0
              ? `Você ainda tem ${remainingCount} tarefa(s) pendente(s) ou atrasada(s). Adie para amanhã ou conclua antes de fechar o dia.`
              : 'Todas as tarefas previstas para hoje foram encaminhadas.'}
          </p>
          <button type="button" class="nx-btn-primary" data-action="mark-night-reviewed" ${briefing.isReviewedTonight ? 'style="background:var(--nx-positive);color:#050505"' : ''}>
            ${briefing.isReviewedTonight ? 'Revisão noturna concluída' : 'Fechar revisão de hoje'}
          </button>
        </div>

        ${(briefing.overdueTasks.length || briefing.todayTasks.length) ? `
          <div style="display:flex;flex-direction:column;gap:8px">
            ${renderSectionHeader({ title: 'Pendências para encerrar ou adiar', trailing: String(remainingCount) })}
            ${[...briefing.overdueTasks, ...briefing.todayTasks].map((t) => renderTaskRowCard(t, state, { showPostpone: true })).join('')}
          </div>
        ` : ''}

        <div class="nx-card" data-nexus-component="TomorrowPreviewCard" data-nexus-source="RoutineAssistantScreen.kt:NightReviewTab" data-nexus-status="reproduzido">
          <div class="nx-title-md">Prévia de amanhã · ${esc(formatDateBr(addDaysIso(state.todayIso, 1)))}</div>
          <span class="nx-body-sm tnum">
            ${briefing.tomorrowAppointments.length} compromisso(s) · ${briefing.tomorrowTasks.length} tarefa(s) programada(s) · ${briefing.tomorrowBills.length} conta(s)
          </span>
          ${briefing.tomorrowTasks.length ? `
            <hr class="nx-divider"/>
            ${briefing.tomorrowTasks.map((t) => `
              <div class="nx-row-between">
                <span class="nx-body-md">${esc(t.title)}</span>
                <span class="nx-label-sm nx-accent">Amanhã</span>
              </div>
            `).join('')}
          ` : ''}
          ${briefing.tomorrowAppointments.map((a) => `
            <div class="nx-label-md nx-text-secondary tnum">Agenda: ${esc(a.startTime)} · ${esc(a.title)}</div>
          `).join('')}
        </div>

        <div style="display:flex;flex-direction:column;gap:8px">
          ${renderSectionHeader({ title: 'Histórico recente de briefings', trailing: String(state.briefingState.history.length) })}
          ${state.briefingState.history.map((h) => `
            <div class="nx-card is-elevated">
              <div class="nx-row-between">
                <span class="nx-title-sm">${esc(h.briefingType)}</span>
                <span class="nx-label-sm nx-text-secondary tnum">${esc(h.reviewedAt)}</span>
              </div>
              <span class="nx-body-sm">${esc(h.summaryText)}</span>
            </div>
          `).join('')}
        </div>
      </div>
    `;
  }

  function renderNotificationsTab(state) {
    const s = state.routineSettings;
    const renderToggle = (key, label, sub) => `
      <div class="nx-row-between" style="padding:4px 0">
        <div style="min-width:0">
          <div class="nx-body-md">${esc(label)}</div>
          ${sub ? `<div class="nx-label-sm nx-text-secondary">${esc(sub)}</div>` : ''}
        </div>
        <button type="button" class="nx-switch ${s[key] ? 'is-on' : ''}" role="switch" aria-checked="${Boolean(s[key])}" data-action="toggle-routine-setting" data-setting-key="${esc(key)}"></button>
      </div>
    `;

    return `
      <div data-nexus-component="NotificationsSettingsTab" data-nexus-source="RoutineAssistantScreen.kt:NotificationsSettingsTab" data-nexus-status="parcial" style="display:flex;flex-direction:column;gap:12px">
        <div class="nx-card">
          <div class="nx-row-between">
            <h3 class="nx-title-md">Alertas e notificações locais</h3>
            <button type="button" class="nx-btn-outlined" data-action="test-notification">Notificação teste</button>
          </div>
          ${renderToggle('notificationsEnabled', 'Ativar notificações do NEXUS', 'Motor simulado no piloto web (AlarmManager no Android)')}
          <hr class="nx-divider"/>
          ${renderToggle('scheduleChannelEnabled', 'Canal de Tarefas e Agenda', 'Lembretes de compromissos e prazos')}
          ${renderToggle('financeChannelEnabled', 'Canal Financeiro', 'Contas a pagar, recebimentos e faturas')}
          ${renderToggle('eventsChannelEnabled', 'Canal de Eventos', 'Alertas de início de eventos e ingressos')}
          ${renderToggle('routineChannelEnabled', 'Canal Assistente de Rotina', 'Lembrete diário às ' + s.morningBriefingTime + ' e noturno às ' + s.nightReviewTime)}
        </div>

        <div class="nx-card">
          <div class="nx-title-md">Antecedência padrão · Eventos</div>
          <div class="nx-chip-row">
            ${[15, 30, 60, 120].map((m) => `
              <button type="button" class="nx-chip ${s.defaultEventReminderMinutes === m ? 'is-active' : ''}" data-action="set-default-event-reminder" data-minutes="${m}">${m} min antes</button>
            `).join('')}
          </div>
          <div class="nx-row-between" style="margin-top:4px">
            <span class="nx-label-md nx-text-secondary">Janela silenciosa</span>
            <span class="nx-label-md tnum">${esc(s.quietHoursStart)} – ${esc(s.quietHoursEnd)}</span>
          </div>
        </div>

        <div style="display:flex;flex-direction:column;gap:8px">
          ${renderSectionHeader({ title: 'Lembretes agendados e recentes', trailing: String(state.reminders.length) })}
          ${state.reminders.map((rem) => `
            <div class="nx-card is-elevated" data-nexus-component="ReminderRowCard" data-nexus-source="RoutineAssistantScreen.kt:NotificationsSettingsTab" data-nexus-status="simulado">
              <div class="nx-row-between">
                <span class="nx-title-sm">${esc(rem.title)}</span>
                <span class="nx-label-sm tnum" style="color:${rem.status === 'ACTIVE' ? 'var(--nx-accent-neon)' : rem.status === 'FIRED' ? 'var(--nx-warning)' : 'var(--nx-text-secondary)'}">
                  ${esc(LABELS.ReminderStatus[rem.status] || rem.status)}
                </span>
              </div>
              <span class="nx-body-sm">${esc(rem.message)}</span>
              <span class="nx-label-sm nx-text-secondary tnum">${esc(rem.triggerLabel)}</span>
              ${rem.status === 'ACTIVE' || rem.status === 'FIRED' ? `
                <div class="nx-row-end">
                  <button type="button" class="nx-btn-text" data-action="snooze-reminder" data-reminder-id="${esc(rem.id)}">Adiar 15 min</button>
                  <button type="button" class="nx-btn-text nx-btn-danger-text" data-action="cancel-reminder" data-reminder-id="${esc(rem.id)}">Cancelar</button>
                </div>
              ` : ''}
            </div>
          `).join('')}
        </div>
      </div>
    `;
  }

  function renderRoutineAssistantScreen(state, ui) {
    const tabIdx = Number(ui.routineTabIndex || 0);
    const briefing = computeDailyBriefing(state);
    const tabs = ['Briefing Diário', 'Revisão Noturna', 'Notificações'];

    let bodyHtml = '';
    if (tabIdx === 0) bodyHtml = renderDailyBriefingTab(state, briefing);
    else if (tabIdx === 1) bodyHtml = renderNightReviewTab(state, briefing);
    else bodyHtml = renderNotificationsTab(state);

    return `
      <div class="nx-tabs-bar" data-nexus-component="RoutineAssistantTabs" data-nexus-source="RoutineAssistantScreen.kt" data-nexus-status="reproduzido" role="tablist">
        ${tabs.map((label, idx) => `
          <button type="button" role="tab" aria-selected="${idx === tabIdx}" class="nx-tab ${idx === tabIdx ? 'is-active' : ''}" data-action="select-routine-tab" data-routine-tab="${idx}">
            ${esc(label)}
          </button>
        `).join('')}
      </div>
      <div class="nx-screen-scroll no-bottom-nav">
        ${bodyHtml}
      </div>
    `;
  }

  window.NexusPilot = window.NexusPilot || {};
  window.NexusPilot.Screens = window.NexusPilot.Screens || {};
  window.NexusPilot.Screens.RoutineAssistant = {
    renderRoutineAssistantScreen
  };
})();
