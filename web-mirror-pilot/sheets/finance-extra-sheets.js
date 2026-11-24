/*
 * NEXUS Web Mirror Pilot — Finance, Market, Goals, Quotes & Settings Editors/Dialogs
 * Fonte Android: FinanceEditors.kt, MarketAlertSheetsAndCards.kt, PlanningGoalsScreen.kt, DashboardCustomizationAndQuotesScreens.kt, MoreSettingsBackupDiagnosticsScreens.kt
 */
(function () {
  const { MARKET_ASSET_CATALOG, LABELS } = window.NexusPilot.Data;
  const { esc } = window.NexusPilot.Primitives;

  function renderSimpleModal({ title, bodyHtml, saveAction, saveLabel = 'Salvar', deleteAction = null, closeAction = 'close-extra-editors', component = 'ExtraEditorDialog' }) {
    return `
      <div class="nx-dialog-backdrop" data-action="${closeAction}">
        <div class="nx-dialog" role="dialog" aria-modal="true" data-nexus-component="${component}" onclick="event.stopPropagation()" style="max-height:86vh;overflow-y:auto">
          <div class="nx-row-between">
            <h3 class="nx-title-lg">${esc(title)}</h3>
            <button type="button" class="nx-btn-text" data-action="${closeAction}">Fechar</button>
          </div>
          ${bodyHtml}
          <div class="nx-row-between" style="margin-top:4px">
            ${deleteAction ? `<button type="button" class="nx-btn-text nx-btn-danger-text" data-action="${deleteAction}">Excluir</button>` : '<span></span>'}
            <div style="display:flex;gap:8px">
              <button type="button" class="nx-btn-text" data-action="${closeAction}">Cancelar</button>
              <button type="button" class="nx-btn-outlined is-selected" data-action="${saveAction}">${esc(saveLabel)}</button>
            </div>
          </div>
        </div>
      </div>
    `;
  }

  function renderCreditCardEditor(draft) {
    return renderSimpleModal({
      title: draft.isNew ? 'Novo cartão de crédito' : 'Editar cartão',
      saveAction: 'save-credit-card',
      deleteAction: draft.isNew ? null : 'delete-credit-card',
      component: 'CreditCardEditorSheet',
      bodyHtml: `
        <div class="nx-field">
          <label class="nx-field-label">Nome do cartão *</label>
          <input class="nx-input" type="text" value="${esc(draft.name)}" data-extra-field="cardDraft.name" />
        </div>
        <div class="nx-field">
          <label class="nx-field-label">Instituição</label>
          <input class="nx-input" type="text" value="${esc(draft.institution)}" data-extra-field="cardDraft.institution" />
        </div>
        <div class="nx-field">
          <label class="nx-field-label">Limite total (R$)</label>
          <input class="nx-input tnum" type="text" value="${esc(draft.totalLimitText)}" data-extra-field="cardDraft.totalLimitText" />
        </div>
        <div class="nx-row-2col">
          <div class="nx-field">
            <label class="nx-field-label">Dia fechamento</label>
            <input class="nx-input tnum" type="number" min="1" max="28" value="${esc(draft.closingDay)}" data-extra-field="cardDraft.closingDay" />
          </div>
          <div class="nx-field">
            <label class="nx-field-label">Dia vencimento</label>
            <input class="nx-input tnum" type="number" min="1" max="28" value="${esc(draft.dueDay)}" data-extra-field="cardDraft.dueDay" />
          </div>
        </div>
      `
    });
  }

  function renderInstallmentEditor(state, draft) {
    return renderSimpleModal({
      title: 'Novo parcelamento',
      saveAction: 'save-installment',
      component: 'InstallmentEditorSheet',
      bodyHtml: `
        <div class="nx-field">
          <label class="nx-field-label">Descrição *</label>
          <input class="nx-input" type="text" value="${esc(draft.description)}" data-extra-field="installmentDraft.description" />
        </div>
        <div class="nx-row-2col">
          <div class="nx-field">
            <label class="nx-field-label">Valor total (R$) *</label>
            <input class="nx-input tnum" type="text" value="${esc(draft.totalAmountText)}" data-extra-field="installmentDraft.totalAmountText" />
          </div>
          <div class="nx-field">
            <label class="nx-field-label">Parcelas</label>
            <input class="nx-input tnum" type="number" min="2" max="48" value="${esc(draft.installmentCount)}" data-extra-field="installmentDraft.installmentCount" />
          </div>
        </div>
        <div class="nx-row-2col">
          <div class="nx-field">
            <label class="nx-field-label">Cartão</label>
            <select class="nx-select" data-extra-field="installmentDraft.creditCardId">
              <option value="">Sem cartão</option>
              ${state.creditCards.map((c) => `<option value="${esc(c.id)}" ${draft.creditCardId === c.id ? 'selected' : ''}>${esc(c.name)}</option>`).join('')}
            </select>
          </div>
          <div class="nx-field">
            <label class="nx-field-label">Categoria</label>
            <select class="nx-select" data-extra-field="installmentDraft.categoryId">
              ${state.categories.map((c) => `<option value="${esc(c.id)}" ${draft.categoryId === c.id ? 'selected' : ''}>${esc(c.name)}</option>`).join('')}
            </select>
          </div>
        </div>
        <div class="nx-field">
          <label class="nx-field-label">1º vencimento</label>
          <input class="nx-input tnum" type="date" value="${esc(draft.firstDueDateIso)}" data-extra-field="installmentDraft.firstDueDateIso" />
        </div>
      `
    });
  }

  function renderRecurringRuleEditor(state, draft) {
    return renderSimpleModal({
      title: 'Nova regra recorrente',
      saveAction: 'save-recurring-rule',
      component: 'RecurringRuleEditorSheet',
      bodyHtml: `
        <div class="nx-field">
          <label class="nx-field-label">Descrição *</label>
          <input class="nx-input" type="text" value="${esc(draft.description)}" data-extra-field="recurringDraft.description" />
        </div>
        <div class="nx-row-2col">
          <div class="nx-field">
            <label class="nx-field-label">Tipo</label>
            <select class="nx-select" data-extra-field="recurringDraft.type">
              ${Object.entries(LABELS.TransactionType).map(([k, l]) => `<option value="${k}" ${draft.type === k ? 'selected' : ''}>${esc(l)}</option>`).join('')}
            </select>
          </div>
          <div class="nx-field">
            <label class="nx-field-label">Valor (R$) *</label>
            <input class="nx-input tnum" type="text" value="${esc(draft.amountText)}" data-extra-field="recurringDraft.amountText" />
          </div>
        </div>
        <div class="nx-row-2col">
          <div class="nx-field">
            <label class="nx-field-label">Frequência</label>
            <select class="nx-select" data-extra-field="recurringDraft.frequency">
              ${Object.entries(LABELS.RecurrenceFrequency).map(([k, l]) => `<option value="${k}" ${draft.frequency === k ? 'selected' : ''}>${esc(l)}</option>`).join('')}
            </select>
          </div>
          <div class="nx-field">
            <label class="nx-field-label">Próxima ocorrência</label>
            <input class="nx-input tnum" type="date" value="${esc(draft.nextOccurrenceIso)}" data-extra-field="recurringDraft.nextOccurrenceIso" />
          </div>
        </div>
        <div class="nx-row-2col">
          <div class="nx-field">
            <label class="nx-field-label">Pagamento</label>
            <select class="nx-select" data-extra-field="recurringDraft.paymentMethod">
              ${Object.entries(LABELS.PaymentMethod).map(([k, l]) => `<option value="${k}" ${draft.paymentMethod === k ? 'selected' : ''}>${esc(l)}</option>`).join('')}
            </select>
          </div>
          <div class="nx-field">
            <label class="nx-field-label">Categoria</label>
            <select class="nx-select" data-extra-field="recurringDraft.categoryId">
              ${state.categories.map((c) => `<option value="${esc(c.id)}" ${draft.categoryId === c.id ? 'selected' : ''}>${esc(c.name)}</option>`).join('')}
            </select>
          </div>
        </div>
      `
    });
  }

  function renderBudgetEditor(state, draft) {
    return renderSimpleModal({
      title: draft.isNew ? 'Novo orçamento' : 'Editar orçamento',
      saveAction: 'save-budget',
      component: 'BudgetEditorSheet',
      bodyHtml: `
        <div class="nx-field">
          <label class="nx-field-label">Categoria</label>
          <select class="nx-select" data-extra-field="budgetDraft.categoryId">
            ${state.categories.map((c) => `<option value="${esc(c.id)}" ${draft.categoryId === c.id ? 'selected' : ''}>${esc(c.name)}</option>`).join('')}
          </select>
        </div>
        <div class="nx-field">
          <label class="nx-field-label">Limite mensal (R$) *</label>
          <input class="nx-input tnum" type="text" value="${esc(draft.limitText)}" data-extra-field="budgetDraft.limitText" />
        </div>
      `
    });
  }

  function renderFinancialGoalEditor(draft) {
    return renderSimpleModal({
      title: 'Nova meta financeira',
      saveAction: 'save-financial-goal',
      component: 'FinancialGoalEditorSheet',
      bodyHtml: `
        <div class="nx-field">
          <label class="nx-field-label">Nome da meta *</label>
          <input class="nx-input" type="text" value="${esc(draft.name)}" data-extra-field="financialGoalDraft.name" />
        </div>
        <div class="nx-row-2col">
          <div class="nx-field">
            <label class="nx-field-label">Valor atual (R$)</label>
            <input class="nx-input tnum" type="text" value="${esc(draft.currentText)}" data-extra-field="financialGoalDraft.currentText" />
          </div>
          <div class="nx-field">
            <label class="nx-field-label">Meta alvo (R$) *</label>
            <input class="nx-input tnum" type="text" value="${esc(draft.targetText)}" data-extra-field="financialGoalDraft.targetText" />
          </div>
        </div>
        <div class="nx-field">
          <label class="nx-field-label">Data alvo</label>
          <input class="nx-input tnum" type="date" value="${esc(draft.targetDateIso)}" data-extra-field="financialGoalDraft.targetDateIso" />
        </div>
      `
    });
  }

  function renderFinancialGoalAmountDialog(draft) {
    return renderSimpleModal({
      title: `Atualizar "${draft.name}"`,
      saveAction: 'save-fgoal-amount',
      component: 'FinancialGoalAmountDialog',
      bodyHtml: `
        <div class="nx-field">
          <label class="nx-field-label">Novo valor acumulado (R$)</label>
          <input class="nx-input tnum" type="text" value="${esc(draft.currentText)}" data-extra-field="fgoalAmountDraft.currentText" />
        </div>
      `
    });
  }

  function renderLoanEditor(draft) {
    return renderSimpleModal({
      title: draft.isNew ? 'Novo empréstimo / financiamento' : 'Editar empréstimo',
      saveAction: 'save-loan',
      component: 'LoanEditorSheet',
      bodyHtml: `
        <div class="nx-field">
          <label class="nx-field-label">Descrição *</label>
          <input class="nx-input" type="text" value="${esc(draft.name)}" data-extra-field="loanDraft.name" />
        </div>
        <div class="nx-row-2col">
          <div class="nx-field">
            <label class="nx-field-label">Instituição / Credor</label>
            <input class="nx-input" type="text" value="${esc(draft.lender)}" data-extra-field="loanDraft.lender" />
          </div>
          <div class="nx-field">
            <label class="nx-field-label">Dia vencimento</label>
            <input class="nx-input tnum" type="number" min="1" max="28" value="${esc(draft.dueDay)}" data-extra-field="loanDraft.dueDay" />
          </div>
        </div>
        <div class="nx-row-2col">
          <div class="nx-field">
            <label class="nx-field-label">Valor principal (R$)</label>
            <input class="nx-input tnum" type="text" value="${esc(draft.principalText)}" data-extra-field="loanDraft.principalText" />
          </div>
          <div class="nx-field">
            <label class="nx-field-label">Saldo devedor (R$)</label>
            <input class="nx-input tnum" type="text" value="${esc(draft.remainingText)}" data-extra-field="loanDraft.remainingText" />
          </div>
        </div>
        <div class="nx-row-2col">
          <div class="nx-field">
            <label class="nx-field-label">Parcela mensal (R$)</label>
            <input class="nx-input tnum" type="text" value="${esc(draft.monthlyText)}" data-extra-field="loanDraft.monthlyText" />
          </div>
          <div class="nx-field">
            <label class="nx-field-label">Total parcelas</label>
            <input class="nx-input tnum" type="number" min="1" max="120" value="${esc(draft.totalInstallments)}" data-extra-field="loanDraft.totalInstallments" />
          </div>
        </div>
      `
    });
  }

  function renderPersonalGoalEditor(state, draft) {
    return renderSimpleModal({
      title: draft.isNew ? 'Novo objetivo pessoal' : 'Editar objetivo pessoal',
      saveAction: 'save-personal-goal',
      component: 'PersonalGoalEditorDialog',
      bodyHtml: `
        <div class="nx-field">
          <label class="nx-field-label">Título *</label>
          <input class="nx-input" type="text" value="${esc(draft.title)}" data-extra-field="personalGoalDraft.title" />
        </div>
        <div class="nx-field">
          <label class="nx-field-label">Descrição</label>
          <textarea class="nx-textarea" rows="2" data-extra-field="personalGoalDraft.description">${esc(draft.description)}</textarea>
        </div>
        <div class="nx-row-2col">
          <div class="nx-field">
            <label class="nx-field-label">Status</label>
            <select class="nx-select" data-extra-field="personalGoalDraft.status">
              ${Object.entries(LABELS.GoalStatus).map(([k, l]) => `<option value="${k}" ${draft.status === k ? 'selected' : ''}>${esc(l)}</option>`).join('')}
            </select>
          </div>
          <div class="nx-field">
            <label class="nx-field-label">Categoria</label>
            <select class="nx-select" data-extra-field="personalGoalDraft.categoryId">
              <option value="">Sem categoria</option>
              ${state.categories.map((c) => `<option value="${esc(c.id)}" ${draft.categoryId === c.id ? 'selected' : ''}>${esc(c.name)}</option>`).join('')}
            </select>
          </div>
        </div>
        <div class="nx-row-2col">
          <div class="nx-field">
            <label class="nx-field-label">Data alvo</label>
            <input class="nx-input tnum" type="date" value="${esc(draft.targetDateIso)}" data-extra-field="personalGoalDraft.targetDateIso" />
          </div>
          <div class="nx-field">
            <label class="nx-field-label">Modo de progresso</label>
            <select class="nx-select" data-extra-field="personalGoalDraft.progressMode">
              ${Object.entries(LABELS.GoalProgressMode).map(([k, l]) => `<option value="${k}" ${draft.progressMode === k ? 'selected' : ''}>${esc(l)}</option>`).join('')}
            </select>
          </div>
        </div>
        ${draft.progressMode === 'MANUAL' ? `
          <div class="nx-field">
            <label class="nx-field-label">Progresso manual: ${draft.manualProgressPercent}%</label>
            <input type="range" min="0" max="100" value="${Number(draft.manualProgressPercent || 0)}" data-extra-field="personalGoalDraft.manualProgressPercent" />
          </div>
        ` : ''}
      `
    });
  }

  function renderMarketAlertEditor(draft) {
    return renderSimpleModal({
      title: draft.isNew ? 'Novo alerta de preço' : 'Editar alerta de preço',
      saveAction: 'save-market-alert',
      component: 'MarketAlertEditorSheet',
      bodyHtml: `
        <div class="nx-field">
          <label class="nx-field-label">Ativo</label>
          <select class="nx-select" data-extra-field="marketAlertDraft.assetCode">
            ${MARKET_ASSET_CATALOG.map((a) => `<option value="${esc(a.code)}" ${draft.assetCode === a.code ? 'selected' : ''}>${esc(a.displayName)} (${esc(a.ticker)})</option>`).join('')}
          </select>
        </div>
        <div class="nx-field">
          <label class="nx-field-label">Condição</label>
          <select class="nx-select" data-extra-field="marketAlertDraft.condition">
            ${Object.entries(LABELS.MarketAlertCondition).map(([k, l]) => `<option value="${k}" ${draft.condition === k ? 'selected' : ''}>${esc(l)}</option>`).join('')}
          </select>
        </div>
        <div class="nx-field">
          <label class="nx-field-label">Preço alvo *</label>
          <input class="nx-input tnum" type="text" value="${esc(draft.targetPriceText)}" placeholder="Ex.: 5,65" data-extra-field="marketAlertDraft.targetPriceText" />
        </div>
        <div class="nx-row-between">
          <span class="nx-body-md">Alerta ativo</span>
          <button type="button" class="nx-switch ${draft.isActive ? 'is-on' : ''}" data-action="toggle-extra-bool" data-path="marketAlertDraft.isActive"></button>
        </div>
      `
    });
  }

  function renderQuoteEditor(draft) {
    return renderSimpleModal({
      title: draft.isNew ? 'Nova frase' : 'Editar frase',
      saveAction: 'save-quote',
      component: 'QuoteEditorDialog',
      bodyHtml: `
        <div class="nx-field">
          <label class="nx-field-label">Texto da frase *</label>
          <textarea class="nx-textarea" rows="3" data-extra-field="quoteDraft.text">${esc(draft.text)}</textarea>
        </div>
        <div class="nx-field">
          <label class="nx-field-label">Autoria (opcional)</label>
          <input class="nx-input" type="text" value="${esc(draft.author)}" data-extra-field="quoteDraft.author" />
        </div>
      `
    });
  }

  function renderCategoryEditor(draft) {
    return renderSimpleModal({
      title: draft.isNew ? 'Nova categoria' : 'Editar categoria',
      saveAction: 'save-category',
      component: 'CategoryEditDialog',
      bodyHtml: `
        <div class="nx-field">
          <label class="nx-field-label">Nome *</label>
          <input class="nx-input" type="text" value="${esc(draft.name)}" data-extra-field="categoryDraft.name" />
        </div>
        <div class="nx-row-2col">
          <div class="nx-field">
            <label class="nx-field-label">Cor (hex)</label>
            <input class="nx-input tnum" type="color" value="${esc(draft.colorHex || '#5CC8FF')}" data-extra-field="categoryDraft.colorHex" />
          </div>
          <div class="nx-field">
            <label class="nx-field-label">Escopo</label>
            <select class="nx-select" data-extra-field="categoryDraft.scope">
              ${Object.entries(LABELS.CategoryScope).map(([k, l]) => `<option value="${k}" ${draft.scope === k ? 'selected' : ''}>${esc(l)}</option>`).join('')}
            </select>
          </div>
        </div>
      `
    });
  }

  function renderBackupPreviewDialog(preview) {
    return `
      <div class="nx-dialog-backdrop" data-action="close-extra-editors">
        <div class="nx-dialog" role="dialog" aria-modal="true" data-nexus-component="BackupRestorePreviewDialog" onclick="event.stopPropagation()">
          <h3 class="nx-title-lg">Confirmar restauração?</h3>
          <div class="nx-label-md nx-text-secondary tnum">Formato v${esc(preview.formatVersion)} · Exportado em ${esc(preview.exportedAt)}</div>
          <div class="nx-body-sm tnum">
            Registros detectados (${preview.totalRecords} itens):
            <br/>• Tarefas: ${preview.tasks} · Projetos: ${preview.projects} · Objetivos: ${preview.personalGoals}
            <br/>• Compromissos: ${preview.appointments} · Eventos: ${preview.events}
            <br/>• Lançamentos: ${preview.transactions} · Cartões: ${preview.creditCards} · Empréstimos: ${preview.loans}
            <br/>• Mercado: ${preview.marketFavorites} favorito(s) · ${preview.marketAlerts} alerta(s)
          </div>
          <div class="nx-label-sm nx-warning">Os dados atuais em memória serão substituídos atomicamente.</div>
          <div class="nx-row-end">
            <button type="button" class="nx-btn-text" data-action="close-extra-editors">Cancelar</button>
            <button type="button" class="nx-btn-outlined is-selected" data-action="confirm-backup-restore">Restaurar agora</button>
          </div>
        </div>
      </div>
    `;
  }

  function renderExtraEditors(state, ui) {
    if (ui.cardDraft) return renderCreditCardEditor(ui.cardDraft);
    if (ui.installmentDraft) return renderInstallmentEditor(state, ui.installmentDraft);
    if (ui.recurringDraft) return renderRecurringRuleEditor(state, ui.recurringDraft);
    if (ui.budgetDraft) return renderBudgetEditor(state, ui.budgetDraft);
    if (ui.financialGoalDraft) return renderFinancialGoalEditor(ui.financialGoalDraft);
    if (ui.fgoalAmountDraft) return renderFinancialGoalAmountDialog(ui.fgoalAmountDraft);
    if (ui.loanDraft) return renderLoanEditor(ui.loanDraft);
    if (ui.personalGoalDraft) return renderPersonalGoalEditor(state, ui.personalGoalDraft);
    if (ui.marketAlertDraft) return renderMarketAlertEditor(ui.marketAlertDraft);
    if (ui.quoteDraft) return renderQuoteEditor(ui.quoteDraft);
    if (ui.categoryDraft) return renderCategoryEditor(ui.categoryDraft);
    if (ui.backupPendingPreview) return renderBackupPreviewDialog(ui.backupPendingPreview);
    return '';
  }

  window.NexusPilot = window.NexusPilot || {};
  window.NexusPilot.Sheets = window.NexusPilot.Sheets || {};
  window.NexusPilot.Sheets.FinanceExtraSheets = {
    renderExtraEditors
  };
})();
