/*
 * NEXUS Web Mirror Pilot — TransactionEditorSheet
 * Fonte Android: FinanceEditors.kt (TransactionEditorSheet)
 * Permite editar/criar lançamento e navegar diretamente para Fatura/Cartão ou Evento relacionado.
 */
(function () {
  const { LABELS, formatBrl, formatDateBr } = window.NexusPilot.Data;
  const { esc, ICONS } = window.NexusPilot.Primitives;

  function centsToInputBr(cents) {
    const val = (Number(cents) || 0) / 100;
    return val.toFixed(2).replace('.', ',');
  }

  function parseInputBrToCents(str) {
    const cleaned = String(str || '').replace(/\s+/g, '').replace(/\./g, '').replace(',', '.');
    const num = Number.parseFloat(cleaned);
    if (!Number.isFinite(num) || num < 0) return 0;
    return Math.round(num * 100);
  }

  function createDraftFromTx(tx, prefill = {}, state) {
    if (tx) {
      return {
        id: tx.id,
        isNew: false,
        type: tx.type,
        description: tx.description,
        amountText: centsToInputBr(tx.amountCents),
        categoryId: tx.categoryId || '',
        status: tx.status,
        paymentMethod: tx.paymentMethod,
        dueDateIso: tx.dueDateIso || state.todayIso,
        creditCardId: tx.creditCardId || '',
        statementId: tx.statementId || '',
        projectId: tx.projectId || '',
        eventId: tx.eventId || '',
        taskId: tx.taskId || '',
        eventCostType: tx.eventCostType || 'OTHER',
        notes: tx.notes || ''
      };
    }
    return {
      id: null,
      isNew: true,
      type: prefill.type || 'EXPENSE',
      description: prefill.description || '',
      amountText: prefill.amountCents ? centsToInputBr(prefill.amountCents) : '',
      categoryId: prefill.categoryId || (prefill.eventId ? 'cat_eventos' : 'cat_trabalho'),
      status: 'PENDING',
      paymentMethod: prefill.paymentMethod || 'PIX',
      dueDateIso: prefill.dueDateIso || state.todayIso,
      creditCardId: prefill.creditCardId || '',
      statementId: prefill.statementId || '',
      projectId: prefill.projectId || '',
      eventId: prefill.eventId || '',
      taskId: prefill.taskId || '',
      eventCostType: prefill.eventCostType || 'TICKET',
      notes: prefill.notes || ''
    };
  }

  function renderTransactionSheet(state, draft, dirty) {
    const linkedCard = state.creditCards.find((c) => c.id === draft.creditCardId);
    const availableStatements = state.statements.filter((s) => s.creditCardId === draft.creditCardId);
    const linkedStatement = state.statements.find((s) => s.id === draft.statementId) || availableStatements[0] || null;
    const linkedEvent = state.events.find((e) => e.id === draft.eventId);
    const linkedTask = state.tasks.find((t) => t.id === draft.taskId);

    const hasRelationBridge = Boolean(linkedCard || linkedStatement || linkedEvent || linkedTask);

    return `
      <div class="nx-sheet-backdrop" data-action="close-tx-sheet-backdrop">
        <div class="nx-sheet" role="dialog" aria-modal="true" aria-label="${draft.isNew ? 'Novo lançamento' : 'Editar lançamento'}" data-nexus-component="TransactionEditorSheet" data-nexus-source="FinanceEditors.kt:TransactionEditorSheet" data-nexus-status="reproduzido" onclick="event.stopPropagation()">
          <div class="nx-sheet-handle"></div>
          <div class="nx-sheet-body">
            <div class="nx-row-between">
              <h3 class="nx-title-lg">${draft.isNew ? 'Novo lançamento' : 'Editar lançamento'}</h3>
              <div style="display:flex;align-items:center;gap:4px">
                ${!draft.isNew ? `<button type="button" class="nx-icon-btn nx-danger" data-action="request-delete-tx" aria-label="Excluir lançamento">${ICONS.trash}</button>` : ''}
                <button type="button" class="nx-btn-text" data-action="close-tx-sheet">Fechar</button>
              </div>
            </div>

            ${hasRelationBridge ? `
              <div class="nx-relation-box" data-nexus-component="TransactionRelationsBridge" data-nexus-source="FinanceEditors.kt:TransactionEditorSheet" data-nexus-status="reproduzido">
                <div style="display:flex;align-items:center;gap:6px">
                  <span class="nx-accent" style="display:inline-flex">${ICONS.link}</span>
                  <span class="nx-title-sm">Vínculos deste lançamento</span>
                </div>
                ${linkedCard && linkedStatement ? `
                  <div class="nx-label-md nx-text-secondary tnum">
                    Cartão/Fatura: <strong class="nx-text-primary">${esc(linkedCard.name)} · Fatura ${esc(linkedStatement.yearMonth)}</strong> (${esc(LABELS.StatementStatus[linkedStatement.status])}, vence ${esc(formatDateBr(linkedStatement.dueDateIso))})
                  </div>
                ` : ''}
                ${linkedEvent ? `
                  <div class="nx-label-md nx-text-secondary">
                    Evento vinculado: <strong class="nx-text-primary">${esc(linkedEvent.name)}</strong> (${esc(formatDateBr(linkedEvent.startDateIso))})
                  </div>
                ` : ''}
                ${linkedTask ? `
                  <div class="nx-label-md nx-text-secondary">
                    Tarefa vinculada: <strong class="nx-text-primary">${esc(linkedTask.title)}</strong>
                  </div>
                ` : ''}
                <div class="nx-relation-actions">
                  ${linkedStatement ? `
                    <button type="button" class="nx-btn-outlined" data-action="jump-to-statement" data-statement-id="${esc(linkedStatement.id)}">
                      ${ICONS.card} <span>Abrir Fatura ${esc(linkedStatement.yearMonth)}</span>
                    </button>
                  ` : ''}
                  ${linkedEvent ? `
                    <button type="button" class="nx-btn-outlined" data-action="jump-to-event" data-event-id="${esc(linkedEvent.id)}">
                      ${ICONS.events} <span>Abrir Evento relacionado</span>
                    </button>
                  ` : ''}
                </div>
              </div>
            ` : ''}

            <div class="nx-seg-control" style="width:100%;display:grid;grid-template-columns:1fr 1fr">
              <button type="button" class="nx-seg-item ${draft.type === 'EXPENSE' ? 'is-active' : ''}" style="padding:8px;font-size:13px" data-action="tx-field-btn" data-field="type" data-value="EXPENSE">Despesa</button>
              <button type="button" class="nx-seg-item ${draft.type === 'INCOME' ? 'is-active' : ''}" style="padding:8px;font-size:13px" data-action="tx-field-btn" data-field="type" data-value="INCOME">Receita</button>
            </div>

            <div class="nx-field">
              <label class="nx-field-label" for="tx-desc">Descrição *</label>
              <input id="tx-desc" class="nx-input" type="text" value="${esc(draft.description)}" placeholder="Ex.: Ingresso Conferência..." data-tx-field="description" />
            </div>

            <div class="nx-row-2col">
              <div class="nx-field">
                <label class="nx-field-label" for="tx-amount">Valor (R$) *</label>
                <input id="tx-amount" class="nx-input tnum" type="text" inputmode="decimal" value="${esc(draft.amountText)}" placeholder="0,00" data-tx-field="amountText" />
              </div>
              <div class="nx-field">
                <label class="nx-field-label" for="tx-date">Vencimento / Data</label>
                <input id="tx-date" class="nx-input tnum" type="date" value="${esc(draft.dueDateIso)}" data-tx-field="dueDateIso" />
              </div>
            </div>

            <div class="nx-row-2col">
              <div class="nx-field">
                <label class="nx-field-label" for="tx-status">Status financeiro</label>
                <select id="tx-status" class="nx-select" data-tx-field="status">
                  ${Object.entries(LABELS.FinancialStatus).map(([k, label]) => `
                    <option value="${k}" ${draft.status === k ? 'selected' : ''}>${esc(label)}</option>
                  `).join('')}
                </select>
              </div>
              <div class="nx-field">
                <label class="nx-field-label" for="tx-method">Forma de pagamento</label>
                <select id="tx-method" class="nx-select" data-tx-field="paymentMethod">
                  ${Object.entries(LABELS.PaymentMethod).map(([k, label]) => `
                    <option value="${k}" ${draft.paymentMethod === k ? 'selected' : ''}>${esc(label)}</option>
                  `).join('')}
                </select>
              </div>
            </div>

            ${draft.paymentMethod === 'CREDIT_CARD' ? `
              <div class="nx-row-2col">
                <div class="nx-field">
                  <label class="nx-field-label" for="tx-card">Cartão de crédito</label>
                  <select id="tx-card" class="nx-select" data-tx-field="creditCardId">
                    <option value="">Selecionar cartão</option>
                    ${state.creditCards.map((c) => `
                      <option value="${esc(c.id)}" ${draft.creditCardId === c.id ? 'selected' : ''}>${esc(c.name)}</option>
                    `).join('')}
                  </select>
                </div>
                <div class="nx-field">
                  <label class="nx-field-label" for="tx-stmt">Fatura vinculada</label>
                  <select id="tx-stmt" class="nx-select" data-tx-field="statementId">
                    <option value="">Automática pelo ciclo</option>
                    ${availableStatements.map((s) => `
                      <option value="${esc(s.id)}" ${draft.statementId === s.id ? 'selected' : ''}>Fatura ${esc(s.yearMonth)} (${esc(LABELS.StatementStatus[s.status])})</option>
                    `).join('')}
                  </select>
                </div>
              </div>
            ` : ''}

            <div class="nx-row-2col">
              <div class="nx-field">
                <label class="nx-field-label" for="tx-category">Categoria</label>
                <select id="tx-category" class="nx-select" data-tx-field="categoryId">
                  <option value="">Sem categoria</option>
                  ${state.categories.map((c) => `
                    <option value="${esc(c.id)}" ${draft.categoryId === c.id ? 'selected' : ''}>${esc(c.name)}</option>
                  `).join('')}
                </select>
              </div>
              <div class="nx-field">
                <label class="nx-field-label" for="tx-project">Projeto</label>
                <select id="tx-project" class="nx-select" data-tx-field="projectId">
                  <option value="">Sem projeto</option>
                  ${state.projects.map((p) => `
                    <option value="${esc(p.id)}" ${draft.projectId === p.id ? 'selected' : ''}>${esc(p.name)}</option>
                  `).join('')}
                </select>
              </div>
            </div>

            <div class="nx-row-2col">
              <div class="nx-field">
                <label class="nx-field-label" for="tx-event">Evento vinculado</label>
                <select id="tx-event" class="nx-select" data-tx-field="eventId">
                  <option value="">Sem evento</option>
                  ${state.events.map((e) => `
                    <option value="${esc(e.id)}" ${draft.eventId === e.id ? 'selected' : ''}>${esc(e.name)}</option>
                  `).join('')}
                </select>
              </div>
              ${draft.eventId ? `
                <div class="nx-field">
                  <label class="nx-field-label" for="tx-cost-type">Tipo de custo no evento</label>
                  <select id="tx-cost-type" class="nx-select" data-tx-field="eventCostType">
                    ${Object.entries(LABELS.EventCostType).map(([k, label]) => `
                      <option value="${k}" ${draft.eventCostType === k ? 'selected' : ''}>${esc(label)}</option>
                    `).join('')}
                  </select>
                </div>
              ` : `
                <div class="nx-field">
                  <label class="nx-field-label" for="tx-task">Tarefa vinculada</label>
                  <select id="tx-task" class="nx-select" data-tx-field="taskId">
                    <option value="">Sem tarefa</option>
                    ${state.tasks.map((t) => `
                      <option value="${esc(t.id)}" ${draft.taskId === t.id ? 'selected' : ''}>${esc(t.title)}</option>
                    `).join('')}
                  </select>
                </div>
              `}
            </div>

            <div class="nx-field">
              <label class="nx-field-label" for="tx-notes">Observações</label>
              <textarea id="tx-notes" class="nx-textarea" rows="2" data-tx-field="notes">${esc(draft.notes)}</textarea>
            </div>

            ${dirty ? `<div class="nx-label-sm nx-warning">Alterações não salvas neste rascunho.</div>` : ''}

            <button type="button" class="nx-btn-primary" data-action="save-tx-sheet">
              ${draft.isNew ? 'Criar lançamento' : 'Salvar alterações'}
            </button>
          </div>
        </div>
      </div>
    `;
  }

  window.NexusPilot = window.NexusPilot || {};
  window.NexusPilot.Sheets = window.NexusPilot.Sheets || {};
  window.NexusPilot.Sheets.TransactionSheet = {
    createDraftFromTx,
    parseInputBrToCents,
    centsToInputBr,
    renderTransactionSheet
  };
})();
