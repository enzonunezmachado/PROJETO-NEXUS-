/*
 * NEXUS Web Mirror Pilot — EventEditorSheet
 * Fonte Android: EventsScreen.kt (EventEditorSheet)
 * Exibe detalhes, ingresso, custos vinculados (transações -> fatura) e tarefas do evento.
 */
(function () {
  const { LABELS, formatBrl, formatDateBr, computeEventDetails } = window.NexusPilot.Data;
  const { esc, ICONS, renderTaskRowCard } = window.NexusPilot.Primitives;

  function createDraftFromEvent(ev, state) {
    if (ev) {
      return {
        id: ev.id,
        isNew: false,
        name: ev.name,
        description: ev.description || '',
        categoryId: ev.categoryId || 'cat_eventos',
        startDateIso: ev.startDateIso || state.todayIso,
        startTime: ev.startTime || '18:00',
        endTime: ev.endTime || '21:00',
        isAllDay: Boolean(ev.isAllDay),
        locationName: ev.locationName || '',
        city: ev.city || '',
        state: ev.state || '',
        eventStatus: ev.eventStatus || 'PLANNED',
        ticketStatus: ev.ticketStatus || 'PENDING',
        ticketPriceText: ((ev.ticketPriceCents || 0) / 100).toFixed(2).replace('.', ','),
        ticketFeesText: ((ev.ticketFeesCents || 0) / 100).toFixed(2).replace('.', ','),
        paymentMethod: ev.paymentMethod || 'PIX',
        creditCardId: ev.creditCardId || '',
        notes: ev.notes || ''
      };
    }
    return {
      id: null,
      isNew: true,
      name: '',
      description: '',
      categoryId: 'cat_eventos',
      startDateIso: state.todayIso,
      startTime: '19:00',
      endTime: '22:00',
      isAllDay: false,
      locationName: '',
      city: 'São Paulo',
      state: 'SP',
      eventStatus: 'PLANNED',
      ticketStatus: 'PENDING',
      ticketPriceText: '0,00',
      ticketFeesText: '0,00',
      paymentMethod: 'PIX',
      creditCardId: '',
      notes: ''
    };
  }

  function renderEventSheet(state, draft, dirty) {
    const existing = draft.id ? state.events.find((e) => e.id === draft.id) : null;
    const details = existing ? computeEventDetails(state, existing) : null;

    return `
      <div class="nx-sheet-backdrop" data-action="close-event-sheet-backdrop">
        <div class="nx-sheet" role="dialog" aria-modal="true" aria-label="${draft.isNew ? 'Novo evento' : 'Detalhes do evento'}" data-nexus-component="EventEditorSheet" data-nexus-source="EventsScreen.kt:EventEditorSheet" data-nexus-status="reproduzido" onclick="event.stopPropagation()">
          <div class="nx-sheet-handle"></div>
          <div class="nx-sheet-body">
            <div class="nx-row-between">
              <h3 class="nx-title-lg">${draft.isNew ? 'Novo evento' : 'Detalhes e custos do evento'}</h3>
              <div style="display:flex;align-items:center;gap:4px">
                ${!draft.isNew ? `<button type="button" class="nx-icon-btn nx-danger" data-action="request-delete-event" aria-label="Excluir evento">${ICONS.trash}</button>` : ''}
                <button type="button" class="nx-btn-text" data-action="close-event-sheet">Fechar</button>
              </div>
            </div>

            ${details ? `
              <div class="nx-relation-box" data-nexus-component="EventLinkedFinanceSummary" data-nexus-source="EventsScreen.kt:EventEditorSheet" data-nexus-status="reproduzido">
                <div class="nx-row-between">
                  <span class="nx-title-sm">Resumo financeiro do evento</span>
                  <span class="nx-title-sm tnum">${esc(formatBrl(details.estimatedTotalCostCents))}</span>
                </div>
                <div class="nx-row-between nx-label-md tnum">
                  <span class="nx-text-secondary">Custos liquidados: <strong class="nx-positive">${esc(formatBrl(details.paidTotalCostCents))}</strong></span>
                  <span class="nx-text-secondary">Ingresso: <strong class="nx-text-primary">${esc(LABELS.TicketStatus[existing.ticketStatus])}</strong></span>
                </div>
              </div>
            ` : ''}

            <div class="nx-field">
              <label class="nx-field-label" for="ev-name">Nome do evento *</label>
              <input id="ev-name" class="nx-input" type="text" value="${esc(draft.name)}" placeholder="Ex.: Conferência..." data-ev-field="name" />
            </div>

            <div class="nx-row-2col">
              <div class="nx-field">
                <label class="nx-field-label" for="ev-date">Data</label>
                <input id="ev-date" class="nx-input tnum" type="date" value="${esc(draft.startDateIso)}" data-ev-field="startDateIso" />
              </div>
              <div class="nx-field">
                <label class="nx-field-label" for="ev-status">Status do evento</label>
                <select id="ev-status" class="nx-select" data-ev-field="eventStatus">
                  ${Object.entries(LABELS.EventLifecycleStatus).map(([k, label]) => `
                    <option value="${k}" ${draft.eventStatus === k ? 'selected' : ''}>${esc(label)}</option>
                  `).join('')}
                </select>
              </div>
            </div>

            <div class="nx-row-2col">
              <div class="nx-field">
                <label class="nx-field-label" for="ev-start">Início</label>
                <input id="ev-start" class="nx-input tnum" type="time" value="${esc(draft.startTime)}" data-ev-field="startTime" />
              </div>
              <div class="nx-field">
                <label class="nx-field-label" for="ev-end">Término</label>
                <input id="ev-end" class="nx-input tnum" type="time" value="${esc(draft.endTime)}" data-ev-field="endTime" />
              </div>
            </div>

            <div class="nx-row-2col">
              <div class="nx-field">
                <label class="nx-field-label" for="ev-loc">Local</label>
                <input id="ev-loc" class="nx-input" type="text" value="${esc(draft.locationName)}" data-ev-field="locationName" />
              </div>
              <div class="nx-field">
                <label class="nx-field-label" for="ev-ticket">Status do ingresso</label>
                <select id="ev-ticket" class="nx-select" data-ev-field="ticketStatus">
                  ${Object.entries(LABELS.TicketStatus).map(([k, label]) => `
                    <option value="${k}" ${draft.ticketStatus === k ? 'selected' : ''}>${esc(label)}</option>
                  `).join('')}
                </select>
              </div>
            </div>

            <div class="nx-row-2col">
              <div class="nx-field">
                <label class="nx-field-label" for="ev-price">Valor ingresso (R$)</label>
                <input id="ev-price" class="nx-input tnum" type="text" value="${esc(draft.ticketPriceText)}" data-ev-field="ticketPriceText" />
              </div>
              <div class="nx-field">
                <label class="nx-field-label" for="ev-fees">Taxas (R$)</label>
                <input id="ev-fees" class="nx-input tnum" type="text" value="${esc(draft.ticketFeesText)}" data-ev-field="ticketFeesText" />
              </div>
            </div>

            ${details ? `
              <hr class="nx-divider"/>
              <div class="nx-row-between">
                <span class="nx-title-md">Custos do evento (${details.linkedTransactions.length})</span>
                <button type="button" class="nx-btn-text" data-action="add-event-cost" data-event-id="${esc(existing.id)}">+ Adicionar custo</button>
              </div>
              ${details.linkedTransactions.length ? details.linkedTransactions.map((tx) => {
                const card = state.creditCards.find((c) => c.id === tx.creditCardId);
                const stmt = state.statements.find((s) => s.id === tx.statementId);
                return `
                  <button type="button" class="nx-card is-elevated is-clickable" data-action="open-tx" data-tx-id="${esc(tx.id)}">
                    <div class="nx-row-between" style="width:100%">
                      <span class="nx-title-sm">${esc(tx.description)}</span>
                      <span class="nx-title-sm tnum">${esc(formatBrl(tx.amountCents))}</span>
                    </div>
                    <span class="nx-label-md nx-text-secondary tnum">
                      ${esc(LABELS.EventCostType[tx.eventCostType] || 'Custo')} · ${esc(LABELS.FinancialStatus[tx.status])} · ${esc(formatDateBr(tx.dueDateIso))}${card ? ` · ${esc(card.name)}${stmt ? ` (${esc(stmt.yearMonth)})` : ''}` : ''}
                    </span>
                  </button>
                `;
              }).join('') : '<span class="nx-body-sm">Nenhum lançamento financeiro vinculado a este evento.</span>'}

              <hr class="nx-divider"/>
              <div class="nx-row-between">
                <span class="nx-title-md">Tarefas do evento (${details.linkedTasks.length})</span>
                <button type="button" class="nx-btn-text" data-action="add-event-task" data-event-id="${esc(existing.id)}">+ Nova tarefa</button>
              </div>
              ${details.linkedTasks.length ? details.linkedTasks.map((t) => renderTaskRowCard(t, state)).join('') : '<span class="nx-body-sm">Nenhuma tarefa vinculada.</span>'}
            ` : ''}

            <div class="nx-field">
              <label class="nx-field-label" for="ev-notes">Anotações</label>
              <textarea id="ev-notes" class="nx-textarea" rows="2" data-ev-field="notes">${esc(draft.notes)}</textarea>
            </div>

            ${dirty ? `<div class="nx-label-sm nx-warning">Alterações não salvas neste evento.</div>` : ''}

            <button type="button" class="nx-btn-primary" data-action="save-event-sheet">
              ${draft.isNew ? 'Criar evento' : 'Salvar evento'}
            </button>
          </div>
        </div>
      </div>
    `;
  }

  window.NexusPilot = window.NexusPilot || {};
  window.NexusPilot.Sheets = window.NexusPilot.Sheets || {};
  window.NexusPilot.Sheets.EventSheet = {
    createDraftFromEvent,
    renderEventSheet
  };
})();
