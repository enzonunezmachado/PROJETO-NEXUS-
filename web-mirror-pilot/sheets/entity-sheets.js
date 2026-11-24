/*
 * NEXUS Web Mirror Pilot — Entity Editors (TaskEditorSheet, AppointmentEditorSheet, ProjectEditorSheet)
 * Fonte Android: EntityEditors.kt
 */
(function () {
  const {
    LABELS,
    formatBrl,
    formatDateBr,
    parseIsoDate,
    weekdayBit,
    weekdaysFromMask,
    formatRecurrenceSummary,
    getActiveTaskRecurrence,
    computeProjectProgress,
    computeTaskExpenseCents
  } = window.NexusPilot.Data;
  const { esc, ICONS } = window.NexusPilot.Primitives;

  const WEEKDAY_CHIPS = [
    { dow: 1, label: 'Seg' },
    { dow: 2, label: 'Ter' },
    { dow: 3, label: 'Qua' },
    { dow: 4, label: 'Qui' },
    { dow: 5, label: 'Sex' },
    { dow: 6, label: 'Sáb' },
    { dow: 7, label: 'Dom' }
  ];

  function buildRecRuleSig(r) {
    if (!r || !r.enabled) return 'NONE';
    return [
      r.frequency || 'WEEKLY',
      Number(r.customIntervalDays) || 7,
      Number(r.weekdaysMask) || 0,
      r.startDateIso || '',
      r.endMode || 'NONE',
      r.endMode === 'DATE' ? (r.endDateIso || '') : '',
      r.endMode === 'COUNT' ? (Number(r.maxOccurrences) || 10) : ''
    ].join('|');
  }

  function createDraftFromTask(task, prefill = {}, state) {
    if (task) {
      const rec = getActiveTaskRecurrence(state, task);
      const endMode = rec ? (rec.endDateIso ? 'DATE' : rec.maxOccurrences ? 'COUNT' : 'NONE') : 'NONE';
      const defaultDowBit = task.dueDateIso
        ? weekdayBit(parseIsoDate(task.dueDateIso).getDay())
        : weekdayBit(parseIsoDate(state.todayIso).getDay());
      const weekdaysMask = rec ? Number(rec.weekdaysMask || 0) : defaultDowBit;
      const customIntervalDays = rec ? String(rec.customIntervalDays || 7) : '7';
      const recStart = rec ? (rec.startDateIso || task.dueDateIso || state.todayIso) : (task.dueDateIso || state.todayIso);
      const recEnd = rec && rec.endDateIso ? rec.endDateIso : '';
      const recMax = rec && rec.maxOccurrences ? String(rec.maxOccurrences) : '10';
      const recFreq = rec ? rec.frequency : 'WEEKLY';

      return {
        id: task.id,
        isNew: false,
        title: task.title,
        description: task.description || '',
        status: task.status || 'NOT_STARTED',
        priority: task.priority || 'NORMAL',
        projectId: task.projectId || '',
        categoryId: task.categoryId || '',
        eventId: task.eventId || '',
        goalId: task.goalId || '',
        dueDateIso: task.dueDateIso || '',
        hasDueTime: Boolean(task.hasDueTime),
        dueTime: task.dueTime || '14:00',
        reminderMinutesBefore: task.reminderMinutesBefore === -1
          ? '-1'
          : task.reminderMinutesBefore != null
            ? String(task.reminderMinutesBefore)
            : '',
        recurrenceId: task.recurrenceId || (rec ? rec.id : null),
        occurrenceDateIso: task.occurrenceDateIso || task.dueDateIso || null,
        recurrenceEnabled: Boolean(rec),
        recurrenceFrequency: recFreq,
        customIntervalDays,
        weekdaysMask,
        recurrenceStartDateIso: recStart,
        recurrenceEndMode: endMode,
        recurrenceEndDateIso: recEnd,
        recurrenceMaxOccurrences: recMax,
        recurrenceExpanded: Boolean(rec),
        initRecRuleSig: buildRecRuleSig({
          enabled: Boolean(rec),
          frequency: recFreq,
          customIntervalDays,
          weekdaysMask,
          startDateIso: recStart,
          endMode,
          endDateIso: recEnd,
          maxOccurrences: recMax
        }),
        initTemplateSig: [
          task.title || '',
          task.description || '',
          task.priority || 'NORMAL',
          task.projectId || '',
          task.categoryId || '',
          task.eventId || '',
          task.goalId || '',
          Boolean(task.hasDueTime),
          task.dueTime || '14:00',
          task.notes || ''
        ].join('|'),
        subtasks: (task.subtasks || []).map((s) => ({ ...s })),
        newSubtaskTitle: '',
        notes: task.notes || ''
      };
    }

    const baseDate = prefill.dueDateIso !== undefined ? prefill.dueDateIso : state.todayIso;
    const baseDowBit = baseDate ? weekdayBit(parseIsoDate(baseDate).getDay()) : weekdayBit(parseIsoDate(state.todayIso).getDay());
    return {
      id: null,
      isNew: true,
      title: prefill.title || '',
      description: prefill.description || '',
      status: 'NOT_STARTED',
      priority: prefill.priority || 'NORMAL',
      projectId: prefill.projectId || '',
      categoryId: prefill.categoryId || '',
      eventId: prefill.eventId || '',
      goalId: prefill.goalId || '',
      dueDateIso: baseDate,
      hasDueTime: false,
      dueTime: '14:00',
      reminderMinutesBefore: '',
      recurrenceId: null,
      occurrenceDateIso: null,
      recurrenceEnabled: false,
      recurrenceFrequency: 'WEEKLY',
      customIntervalDays: '7',
      weekdaysMask: baseDowBit,
      recurrenceStartDateIso: baseDate || state.todayIso,
      recurrenceEndMode: 'NONE',
      recurrenceEndDateIso: '',
      recurrenceMaxOccurrences: '10',
      recurrenceExpanded: false,
      initRecRuleSig: 'NONE',
      initTemplateSig: '',
      subtasks: [],
      newSubtaskTitle: '',
      notes: ''
    };
  }

  function renderTaskSheet(state, draft, dirty) {
    const expenseCents = draft.id ? computeTaskExpenseCents(state, draft.id) : 0;
    const linkedTxs = draft.id
      ? state.transactions.filter((tx) => tx.taskId === draft.id && tx.status !== 'CANCELLED')
      : [];
    const doneSub = draft.subtasks.filter((s) => s.isDone).length;
    const defaultMin = state.routineSettings?.defaultTaskReminderMinutes ?? 30;
    const previewRec = draft.recurrenceEnabled ? {
      frequency: draft.recurrenceFrequency,
      customIntervalDays: Number(draft.customIntervalDays) || 7,
      weekdaysMask: Number(draft.weekdaysMask) || 0
    } : null;
    const recSummary = previewRec ? formatRecurrenceSummary(previewRec) : 'Desativada';
    const isCompleted = draft.status === 'COMPLETED';

    return `
      <div class="nx-sheet-backdrop" data-action="close-task-sheet-backdrop">
        <div class="nx-sheet" role="dialog" aria-modal="true" aria-label="${draft.isNew ? 'Nova tarefa' : 'Editar tarefa'}" data-nexus-component="TaskEditorSheet" data-nexus-source="EntityEditors.kt:TaskEditorSheet" data-nexus-status="reproduzido" onclick="event.stopPropagation()">
          <div class="nx-sheet-handle"></div>
          <div class="nx-sheet-body">
            <div class="nx-row-between">
              <div style="display:flex;align-items:center;gap:8px;min-width:0">
                <h3 class="nx-title-lg">${draft.isNew ? 'Nova tarefa' : 'Editar tarefa'}</h3>
                ${draft.recurrenceId ? `<span class="nx-badge is-accent" style="display:inline-flex;align-items:center;gap:4px">${ICONS.repeat} Série</span>` : ''}
              </div>
              <div style="display:flex;align-items:center;gap:4px">
                ${!draft.isNew ? `<button type="button" class="nx-icon-btn nx-danger" data-action="request-delete-task" aria-label="Excluir tarefa">${ICONS.trash}</button>` : ''}
                <button type="button" class="nx-btn-text" data-action="close-task-sheet">Fechar</button>
              </div>
            </div>

            <div class="nx-field">
              <label class="nx-field-label" for="task-title">Título *</label>
              <input id="task-title" class="nx-input" type="text" value="${esc(draft.title)}" placeholder="Ex.: Conferir documentos..." data-task-field="title" />
            </div>

            <div class="nx-field">
              <label class="nx-field-label" for="task-desc">Descrição</label>
              <textarea id="task-desc" class="nx-textarea" rows="2" data-task-field="description">${esc(draft.description)}</textarea>
            </div>

            <div class="nx-row-2col">
              <div class="nx-field">
                <label class="nx-field-label" for="task-status">Status</label>
                <select id="task-status" class="nx-select" data-task-field="status">
                  ${Object.entries(LABELS.TaskStatus).map(([k, l]) => `<option value="${k}" ${draft.status === k ? 'selected' : ''}>${esc(l)}</option>`).join('')}
                </select>
              </div>
              <div class="nx-field">
                <label class="nx-field-label" for="task-prio">Prioridade</label>
                <select id="task-prio" class="nx-select" data-task-field="priority">
                  ${Object.entries(LABELS.TaskPriority).map(([k, l]) => `<option value="${k}" ${draft.priority === k ? 'selected' : ''}>${esc(l)}</option>`).join('')}
                </select>
              </div>
            </div>

            <div class="nx-row-2col">
              <div class="nx-field">
                <label class="nx-field-label" for="task-due">Data de vencimento</label>
                <input id="task-due" class="nx-input tnum" type="date" value="${esc(draft.dueDateIso)}" data-task-field="dueDateIso" />
              </div>
              <div class="nx-field">
                <label class="nx-field-label" for="task-reminder">Lembrete</label>
                <select id="task-reminder" class="nx-select" data-task-field="reminderMinutesBefore">
                  <option value="" ${draft.reminderMinutesBefore === '' ? 'selected' : ''}>Padrão (${defaultMin} min)</option>
                  <option value="0" ${draft.reminderMinutesBefore === '0' ? 'selected' : ''}>Na hora</option>
                  <option value="15" ${draft.reminderMinutesBefore === '15' ? 'selected' : ''}>15 min antes</option>
                  <option value="30" ${draft.reminderMinutesBefore === '30' ? 'selected' : ''}>30 min antes</option>
                  <option value="60" ${draft.reminderMinutesBefore === '60' ? 'selected' : ''}>1 hora antes</option>
                  <option value="-1" ${draft.reminderMinutesBefore === '-1' ? 'selected' : ''}>Sem lembrete</option>
                </select>
              </div>
            </div>

            <div class="nx-row-between">
              <span class="nx-body-md">Definir horário específico</span>
              <button type="button" class="nx-switch ${draft.hasDueTime ? 'is-on' : ''}" data-action="task-toggle-field" data-field="hasDueTime"></button>
            </div>
            ${draft.hasDueTime ? `
              <div class="nx-field">
                <label class="nx-field-label" for="task-time">Horário</label>
                <input id="task-time" class="nx-input tnum" type="time" value="${esc(draft.dueTime)}" data-task-field="dueTime" />
              </div>
            ` : ''}

            <div class="nx-row-2col">
              <div class="nx-field">
                <label class="nx-field-label" for="task-proj">Projeto</label>
                <select id="task-proj" class="nx-select" data-task-field="projectId">
                  <option value="">Sem projeto</option>
                  ${state.projects.map((p) => `<option value="${esc(p.id)}" ${draft.projectId === p.id ? 'selected' : ''}>${esc(p.name)}</option>`).join('')}
                </select>
              </div>
              <div class="nx-field">
                <label class="nx-field-label" for="task-cat">Categoria</label>
                <select id="task-cat" class="nx-select" data-task-field="categoryId">
                  <option value="">Sem categoria</option>
                  ${state.categories.map((c) => `<option value="${esc(c.id)}" ${draft.categoryId === c.id ? 'selected' : ''}>${esc(c.name)}</option>`).join('')}
                </select>
              </div>
            </div>

            <div class="nx-row-2col">
              <div class="nx-field">
                <div class="nx-row-between">
                  <label class="nx-field-label" for="task-ev">Evento vinculado</label>
                  ${draft.eventId ? `<button type="button" class="nx-btn-text" style="padding:0;font-size:11px" data-action="open-event-from-task" data-event-id="${esc(draft.eventId)}">Abrir evento</button>` : ''}
                </div>
                <select id="task-ev" class="nx-select" data-task-field="eventId">
                  <option value="">Sem evento</option>
                  ${state.events.map((e) => `<option value="${esc(e.id)}" ${draft.eventId === e.id ? 'selected' : ''}>${esc(e.name)}</option>`).join('')}
                </select>
              </div>
              <div class="nx-field">
                <label class="nx-field-label" for="task-goal">Objetivo pessoal</label>
                <select id="task-goal" class="nx-select" data-task-field="goalId">
                  <option value="">Sem objetivo</option>
                  ${state.personalGoals.map((g) => `<option value="${esc(g.id)}" ${draft.goalId === g.id ? 'selected' : ''}>${esc(g.title)}</option>`).join('')}
                </select>
              </div>
            </div>

            <div class="nx-card is-elevated" style="padding:12px">
              <button type="button" class="nx-row-between" style="appearance:none;border:0;background:transparent;color:inherit;cursor:pointer;padding:0;width:100%" data-action="task-toggle-field" data-field="recurrenceExpanded">
                <span class="nx-title-sm" style="display:inline-flex;align-items:center;gap:6px">${ICONS.repeat} Recorrência (${esc(recSummary)})</span>
                ${draft.recurrenceExpanded ? ICONS.chevronUp : ICONS.chevronDown}
              </button>
              ${draft.recurrenceExpanded ? `
                <div class="nx-row-between" style="margin-top:8px">
                  <span class="nx-body-sm">Repetir automaticamente</span>
                  <button type="button" class="nx-switch ${draft.recurrenceEnabled ? 'is-on' : ''}" data-action="task-toggle-field" data-field="recurrenceEnabled"></button>
                </div>
                ${draft.recurrenceEnabled ? `
                  <div style="display:flex;flex-direction:column;gap:8px;margin-top:8px">
                    <div class="nx-field">
                      <label class="nx-field-label" for="task-rec-freq">Frequência</label>
                      <select id="task-rec-freq" class="nx-select" data-task-field="recurrenceFrequency">
                        ${Object.entries(LABELS.TaskRecurrenceFrequency).map(([k, l]) => `<option value="${k}" ${draft.recurrenceFrequency === k ? 'selected' : ''}>${esc(l)}</option>`).join('')}
                      </select>
                    </div>
                    ${draft.recurrenceFrequency === 'CUSTOM_DAYS' ? `
                      <div class="nx-field">
                        <label class="nx-field-label" for="task-rec-interval">Intervalo em dias</label>
                        <input id="task-rec-interval" class="nx-input tnum" type="number" min="1" max="365" value="${esc(draft.customIntervalDays)}" data-task-field="customIntervalDays" />
                      </div>
                    ` : ''}
                    ${draft.recurrenceFrequency === 'SPECIFIC_WEEKDAYS' ? `
                      <div class="nx-field">
                        <span class="nx-field-label">Dias da semana</span>
                        <div class="nx-chip-row">
                          ${WEEKDAY_CHIPS.map((w) => {
                            const active = (Number(draft.weekdaysMask) & weekdayBit(w.dow)) !== 0;
                            return `<button type="button" class="nx-chip ${active ? 'is-active' : ''}" data-action="task-toggle-weekday" data-dow="${w.dow}">${esc(w.label)}</button>`;
                          }).join('')}
                        </div>
                      </div>
                    ` : ''}
                    <div class="nx-row-2col">
                      <div class="nx-field">
                        <label class="nx-field-label" for="task-rec-start">Início da série</label>
                        <input id="task-rec-start" class="nx-input tnum" type="date" value="${esc(draft.recurrenceStartDateIso)}" data-task-field="recurrenceStartDateIso" />
                      </div>
                      <div class="nx-field">
                        <label class="nx-field-label" for="task-rec-endmode">Término da série</label>
                        <select id="task-rec-endmode" class="nx-select" data-task-field="recurrenceEndMode">
                          <option value="NONE" ${draft.recurrenceEndMode === 'NONE' ? 'selected' : ''}>Sem fim</option>
                          <option value="DATE" ${draft.recurrenceEndMode === 'DATE' ? 'selected' : ''}>Até data</option>
                          <option value="COUNT" ${draft.recurrenceEndMode === 'COUNT' ? 'selected' : ''}>Máx. ocorrências</option>
                        </select>
                      </div>
                    </div>
                    ${draft.recurrenceEndMode === 'DATE' ? `
                      <div class="nx-field">
                        <label class="nx-field-label" for="task-rec-enddate">Data final da série</label>
                        <input id="task-rec-enddate" class="nx-input tnum" type="date" value="${esc(draft.recurrenceEndDateIso)}" data-task-field="recurrenceEndDateIso" />
                      </div>
                    ` : ''}
                    ${draft.recurrenceEndMode === 'COUNT' ? `
                      <div class="nx-field">
                        <label class="nx-field-label" for="task-rec-maxocc">Número máximo de ocorrências</label>
                        <input id="task-rec-maxocc" class="nx-input tnum" type="number" min="1" max="365" value="${esc(draft.recurrenceMaxOccurrences)}" data-task-field="recurrenceMaxOccurrences" />
                      </div>
                    ` : ''}
                  </div>
                ` : ''}
              ` : ''}
            </div>

            <hr class="nx-divider"/>
            <div class="nx-row-between">
              <span class="nx-title-sm">Checklist / Subtarefas (${doneSub}/${draft.subtasks.length})</span>
            </div>
            ${draft.subtasks.map((s) => `
              <div class="nx-row-between" style="background:var(--nx-surface-elevated);padding:8px 10px;border-radius:10px;border:1px solid var(--nx-border)">
                <div style="display:flex;align-items:center;gap:8px;min-width:0;flex:1">
                  <button type="button" class="nx-check-btn ${s.isDone ? 'nx-positive' : 'nx-text-secondary'}" data-action="task-toggle-subtask" data-sub-id="${esc(s.id)}">
                    ${s.isDone ? ICONS.checkCircle : ICONS.circle}
                  </button>
                  <span class="nx-body-md" style="${s.isDone ? 'text-decoration:line-through;color:var(--nx-text-secondary)' : ''}">${esc(s.title)}</span>
                </div>
                <button type="button" class="nx-icon-btn nx-danger" style="width:28px;height:28px" data-action="task-delete-subtask" data-sub-id="${esc(s.id)}">${ICONS.close}</button>
              </div>
            `).join('')}
            <div style="display:flex;gap:8px;align-items:center">
              <input id="task-new-sub" class="nx-input" type="text" placeholder="Nova subtarefa..." value="${esc(draft.newSubtaskTitle)}" data-task-field="newSubtaskTitle" />
              <button type="button" class="nx-btn-outlined" data-action="task-add-subtask">+ Item</button>
            </div>

            <div class="nx-relation-box" style="margin-top:4px">
              <div class="nx-row-between">
                <span class="nx-title-sm">Gasto: ${esc(formatBrl(expenseCents))}</span>
                <span class="nx-label-sm nx-text-secondary tnum">${linkedTxs.length} lançamento(s)</span>
              </div>
              ${linkedTxs.length ? linkedTxs.map((tx) => `
                <button type="button" class="nx-row-between is-clickable" style="appearance:none;border:1px solid var(--nx-border);background:var(--nx-surface-elevated);border-radius:10px;padding:8px 10px;color:inherit;width:100%" data-action="open-tx" data-tx-id="${esc(tx.id)}">
                  <span class="nx-body-sm" style="text-align:left">${esc(tx.description)} · <span class="nx-text-secondary">${esc(formatDateBr(tx.dueDateIso))}</span></span>
                  <span class="nx-title-sm tnum">${esc(formatBrl(tx.amountCents))}</span>
                </button>
              `).join('') : ''}
              <button type="button" class="nx-btn-outlined" data-action="task-register-expense">
                ${ICONS.receipt} <span>Registrar gasto</span>
              </button>
            </div>

            ${dirty ? `<div class="nx-label-sm nx-warning">Alterações não salvas neste rascunho.</div>` : ''}

            <div style="display:flex;gap:8px;align-items:center">
              ${!draft.isNew ? `
                <button type="button" class="nx-btn-outlined" style="flex:0 0 auto" data-action="task-sheet-toggle-complete">
                  ${isCompleted ? 'Reabrir' : 'Concluir'}
                </button>
              ` : ''}
              <button type="button" class="nx-btn-primary" style="flex:1" data-action="save-task-sheet">
                ${draft.isNew ? 'Criar tarefa' : 'Salvar tarefa'}
              </button>
            </div>
          </div>
        </div>
      </div>
    `;
  }

  function createDraftFromAppointment(appt, prefill = {}, state) {
    if (appt) {
      return {
        id: appt.id,
        isNew: false,
        title: appt.title,
        description: appt.description || '',
        dateIso: appt.dateIso || state.todayIso,
        endDateIso: appt.endDateIso || appt.dateIso || state.todayIso,
        startTime: appt.startTime || '09:00',
        endTime: appt.endTime || '10:00',
        isAllDay: Boolean(appt.isAllDay),
        location: appt.location || '',
        link: appt.link || '',
        categoryId: appt.categoryId || 'cat_trabalho',
        eventId: appt.eventId || '',
        reminderMinutesBefore: appt.reminderMinutesBefore != null ? String(appt.reminderMinutesBefore) : '15'
      };
    }
    const baseDate = prefill.dateIso || state.todayIso;
    return {
      id: null,
      isNew: true,
      title: prefill.title || '',
      description: '',
      dateIso: baseDate,
      endDateIso: baseDate,
      startTime: '10:00',
      endTime: '11:00',
      isAllDay: false,
      location: '',
      link: '',
      categoryId: 'cat_trabalho',
      eventId: '',
      reminderMinutesBefore: '30'
    };
  }

  function renderAppointmentSheet(state, draft, dirty) {
    const conflicts = !draft.isAllDay
      ? state.appointments.filter((a) =>
          a.id !== draft.id &&
          !a.isAllDay &&
          a.dateIso === draft.dateIso &&
          draft.startTime < a.endTime &&
          a.startTime < draft.endTime
        )
      : [];

    return `
      <div class="nx-sheet-backdrop" data-action="close-appt-sheet-backdrop">
        <div class="nx-sheet" role="dialog" aria-modal="true" aria-label="${draft.isNew ? 'Novo compromisso' : 'Editar compromisso'}" data-nexus-component="AppointmentEditorSheet" data-nexus-source="EntityEditors.kt:AppointmentEditorSheet" data-nexus-status="reproduzido" onclick="event.stopPropagation()">
          <div class="nx-sheet-handle"></div>
          <div class="nx-sheet-body">
            <div class="nx-row-between">
              <h3 class="nx-title-lg">${draft.isNew ? 'Novo compromisso' : 'Editar compromisso'}</h3>
              <div style="display:flex;align-items:center;gap:4px">
                ${!draft.isNew ? `<button type="button" class="nx-icon-btn nx-danger" data-action="request-delete-appt" aria-label="Excluir compromisso">${ICONS.trash}</button>` : ''}
                <button type="button" class="nx-btn-text" data-action="close-appt-sheet">Fechar</button>
              </div>
            </div>

            ${conflicts.length ? `
              <div class="nx-alert-banner severity-warning">
                <span class="nx-warning">${ICONS.warning}</span>
                <span style="display:flex;flex-direction:column;gap:2px">
                  <span class="nx-label-lg nx-text-primary">Conflito de horário detectado</span>
                  <span class="nx-label-sm nx-text-secondary">Sobrepõe com: ${esc(conflicts.map((c) => `${c.title} (${c.startTime}–${c.endTime})`).join(', '))}</span>
                </span>
              </div>
            ` : ''}

            <div class="nx-field">
              <label class="nx-field-label" for="appt-title">Título *</label>
              <input id="appt-title" class="nx-input" type="text" value="${esc(draft.title)}" placeholder="Ex.: Alinhamento..." data-appt-field="title" />
            </div>

            <div class="nx-row-between">
              <span class="nx-body-md">Dia inteiro</span>
              <button type="button" class="nx-switch ${draft.isAllDay ? 'is-on' : ''}" data-action="appt-toggle-allday"></button>
            </div>

            <div class="nx-row-2col">
              <div class="nx-field">
                <label class="nx-field-label" for="appt-date">Data inicial</label>
                <input id="appt-date" class="nx-input tnum" type="date" value="${esc(draft.dateIso)}" data-appt-field="dateIso" />
              </div>
              <div class="nx-field">
                <label class="nx-field-label" for="appt-enddate">Data final</label>
                <input id="appt-enddate" class="nx-input tnum" type="date" value="${esc(draft.endDateIso)}" data-appt-field="endDateIso" />
              </div>
            </div>

            ${!draft.isAllDay ? `
              <div class="nx-row-2col">
                <div class="nx-field">
                  <label class="nx-field-label" for="appt-start">Início</label>
                  <input id="appt-start" class="nx-input tnum" type="time" value="${esc(draft.startTime)}" data-appt-field="startTime" />
                </div>
                <div class="nx-field">
                  <label class="nx-field-label" for="appt-end">Término</label>
                  <input id="appt-end" class="nx-input tnum" type="time" value="${esc(draft.endTime)}" data-appt-field="endTime" />
                </div>
              </div>
            ` : ''}

            <div class="nx-row-2col">
              <div class="nx-field">
                <label class="nx-field-label" for="appt-loc">Local</label>
                <input id="appt-loc" class="nx-input" type="text" value="${esc(draft.location)}" placeholder="Sala / Endereço" data-appt-field="location" />
              </div>
              <div class="nx-field">
                <label class="nx-field-label" for="appt-cat">Categoria</label>
                <select id="appt-cat" class="nx-select" data-appt-field="categoryId">
                  <option value="">Sem categoria</option>
                  ${state.categories.map((c) => `<option value="${esc(c.id)}" ${draft.categoryId === c.id ? 'selected' : ''}>${esc(c.name)}</option>`).join('')}
                </select>
              </div>
            </div>

            <div class="nx-row-2col">
              <div class="nx-field">
                <label class="nx-field-label" for="appt-link">Link da reunião</label>
                <input id="appt-link" class="nx-input" type="url" value="${esc(draft.link)}" placeholder="https://..." data-appt-field="link" />
              </div>
              <div class="nx-field">
                <label class="nx-field-label" for="appt-rem">Lembrete</label>
                <select id="appt-rem" class="nx-select" data-appt-field="reminderMinutesBefore">
                  <option value="">Sem lembrete</option>
                  ${['15', '30', '60'].map((m) => `<option value="${m}" ${draft.reminderMinutesBefore === m ? 'selected' : ''}>${m} min antes</option>`).join('')}
                </select>
              </div>
            </div>

            <div class="nx-field">
              <label class="nx-field-label" for="appt-desc">Descrição / Pauta</label>
              <textarea id="appt-desc" class="nx-textarea" rows="2" data-appt-field="description">${esc(draft.description)}</textarea>
            </div>

            ${dirty ? `<div class="nx-label-sm nx-warning">Alterações não salvas.</div>` : ''}

            <button type="button" class="nx-btn-primary" data-action="save-appt-sheet">
              ${draft.isNew ? 'Criar compromisso' : 'Salvar compromisso'}
            </button>
          </div>
        </div>
      </div>
    `;
  }

  function createDraftFromProject(proj, prefill = {}) {
    if (proj) {
      return {
        id: proj.id,
        isNew: false,
        name: proj.name,
        description: proj.description || '',
        status: proj.status || 'ACTIVE',
        priority: proj.priority || 'NORMAL',
        deadlineDateIso: proj.deadlineDateIso || '',
        goalId: proj.goalId || ''
      };
    }
    return {
      id: null,
      isNew: true,
      name: '',
      description: '',
      status: 'ACTIVE',
      priority: 'NORMAL',
      deadlineDateIso: '',
      goalId: prefill.goalId || ''
    };
  }

  function renderProjectSheet(state, draft, dirty) {
    const existing = draft.id ? state.projects.find((p) => p.id === draft.id) : null;
    const pr = existing ? computeProjectProgress(state, existing) : null;

    return `
      <div class="nx-sheet-backdrop" data-action="close-project-sheet-backdrop">
        <div class="nx-sheet" role="dialog" aria-modal="true" aria-label="${draft.isNew ? 'Novo projeto' : 'Editar projeto'}" data-nexus-component="ProjectEditorSheet" data-nexus-source="EntityEditors.kt:ProjectEditorSheet" data-nexus-status="reproduzido" onclick="event.stopPropagation()">
          <div class="nx-sheet-handle"></div>
          <div class="nx-sheet-body">
            <div class="nx-row-between">
              <h3 class="nx-title-lg">${draft.isNew ? 'Novo projeto' : 'Editar projeto'}</h3>
              <div style="display:flex;align-items:center;gap:4px">
                ${!draft.isNew ? `<button type="button" class="nx-icon-btn nx-danger" data-action="request-delete-project" aria-label="Excluir projeto">${ICONS.trash}</button>` : ''}
                <button type="button" class="nx-btn-text" data-action="close-project-sheet">Fechar</button>
              </div>
            </div>

            ${pr ? `
              <div class="nx-relation-box">
                <div class="nx-row-between">
                  <span class="nx-title-sm">${pr.completedTasks}/${pr.totalTasks} tarefa(s) concluída(s)</span>
                  <span class="nx-title-sm tnum nx-accent">${pr.progressPercent}%</span>
                </div>
                <div class="nx-label-md nx-text-secondary tnum">
                  Custos vinculados: ${esc(formatBrl(pr.estimatedCostCents))} · Pago: ${esc(formatBrl(pr.paidCostCents))}
                </div>
              </div>
            ` : ''}

            <div class="nx-field">
              <label class="nx-field-label" for="proj-name">Nome do projeto *</label>
              <input id="proj-name" class="nx-input" type="text" value="${esc(draft.name)}" data-proj-field="name" />
            </div>

            <div class="nx-field">
              <label class="nx-field-label" for="proj-desc">Descrição</label>
              <textarea id="proj-desc" class="nx-textarea" rows="2" data-proj-field="description">${esc(draft.description)}</textarea>
            </div>

            <div class="nx-row-2col">
              <div class="nx-field">
                <label class="nx-field-label" for="proj-status">Status</label>
                <select id="proj-status" class="nx-select" data-proj-field="status">
                  ${Object.entries(LABELS.ProjectStatus).map(([k, l]) => `<option value="${k}" ${draft.status === k ? 'selected' : ''}>${esc(l)}</option>`).join('')}
                </select>
              </div>
              <div class="nx-field">
                <label class="nx-field-label" for="proj-prio">Prioridade</label>
                <select id="proj-prio" class="nx-select" data-proj-field="priority">
                  ${Object.entries(LABELS.TaskPriority).map(([k, l]) => `<option value="${k}" ${draft.priority === k ? 'selected' : ''}>${esc(l)}</option>`).join('')}
                </select>
              </div>
            </div>

            <div class="nx-row-2col">
              <div class="nx-field">
                <label class="nx-field-label" for="proj-deadline">Prazo alvo</label>
                <input id="proj-deadline" class="nx-input tnum" type="date" value="${esc(draft.deadlineDateIso)}" data-proj-field="deadlineDateIso" />
              </div>
              <div class="nx-field">
                <label class="nx-field-label" for="proj-goal">Objetivo pessoal</label>
                <select id="proj-goal" class="nx-select" data-proj-field="goalId">
                  <option value="">Sem objetivo</option>
                  ${state.personalGoals.map((g) => `<option value="${esc(g.id)}" ${draft.goalId === g.id ? 'selected' : ''}>${esc(g.title)}</option>`).join('')}
                </select>
              </div>
            </div>

            ${dirty ? `<div class="nx-label-sm nx-warning">Alterações não salvas.</div>` : ''}

            <button type="button" class="nx-btn-primary" data-action="save-project-sheet">
              ${draft.isNew ? 'Criar projeto' : 'Salvar projeto'}
            </button>
          </div>
        </div>
      </div>
    `;
  }

  window.NexusPilot = window.NexusPilot || {};
  window.NexusPilot.Sheets = window.NexusPilot.Sheets || {};
  window.NexusPilot.Sheets.EntitySheets = {
    createDraftFromTask,
    renderTaskSheet,
    createDraftFromAppointment,
    renderAppointmentSheet,
    createDraftFromProject,
    renderProjectSheet
  };
})();
