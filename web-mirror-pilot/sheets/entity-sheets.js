/*
 * NEXUS Web Mirror Pilot — Entity Editors (TaskEditorSheet, AppointmentEditorSheet, ProjectEditorSheet)
 * Fonte Android: EntityEditors.kt
 */
(function () {
  const { LABELS, formatBrl, computeProjectProgress, computeTaskExpenseCents } = window.NexusPilot.Data;
  const { esc, ICONS } = window.NexusPilot.Primitives;

  function createDraftFromTask(task, prefill = {}, state) {
    if (task) {
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
        reminderMinutesBefore: task.reminderMinutesBefore != null ? String(task.reminderMinutesBefore) : '',
        recurrenceEnabled: Boolean(task.recurrence),
        recurrenceFrequency: task.recurrence ? task.recurrence.frequency : 'WEEKLY',
        recurrenceExpanded: Boolean(task.recurrence),
        subtasks: (task.subtasks || []).map((s) => ({ ...s })),
        newSubtaskTitle: '',
        notes: task.notes || ''
      };
    }
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
      dueDateIso: prefill.dueDateIso !== undefined ? prefill.dueDateIso : state.todayIso,
      hasDueTime: false,
      dueTime: '14:00',
      reminderMinutesBefore: '30',
      recurrenceEnabled: false,
      recurrenceFrequency: 'WEEKLY',
      recurrenceExpanded: false,
      subtasks: [],
      newSubtaskTitle: '',
      notes: ''
    };
  }

  function renderTaskSheet(state, draft, dirty) {
    const expenseCents = draft.id ? computeTaskExpenseCents(state, draft.id) : 0;
    const doneSub = draft.subtasks.filter((s) => s.isDone).length;

    return `
      <div class="nx-sheet-backdrop" data-action="close-task-sheet-backdrop">
        <div class="nx-sheet" role="dialog" aria-modal="true" aria-label="${draft.isNew ? 'Nova tarefa' : 'Editar tarefa'}" data-nexus-component="TaskEditorSheet" data-nexus-source="EntityEditors.kt:TaskEditorSheet" data-nexus-status="reproduzido" onclick="event.stopPropagation()">
          <div class="nx-sheet-handle"></div>
          <div class="nx-sheet-body">
            <div class="nx-row-between">
              <h3 class="nx-title-lg">${draft.isNew ? 'Nova tarefa' : 'Editar tarefa'}</h3>
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
                <label class="nx-field-label" for="task-reminder">Lembrete (min antes)</label>
                <select id="task-reminder" class="nx-select" data-task-field="reminderMinutesBefore">
                  <option value="">Sem lembrete</option>
                  ${['0', '15', '30', '60', '120'].map((m) => `<option value="${m}" ${draft.reminderMinutesBefore === m ? 'selected' : ''}>${m === '0' ? 'Na hora' : `${m} min antes`}</option>`).join('')}
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
                <label class="nx-field-label" for="task-ev">Evento vinculado</label>
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
                <span class="nx-title-sm" style="display:inline-flex;align-items:center;gap:6px">${ICONS.repeat} Recorrência ${draft.recurrenceEnabled ? `(${LABELS.TaskRecurrenceFrequency[draft.recurrenceFrequency]})` : '(Desativada)'}</span>
                ${draft.recurrenceExpanded ? ICONS.chevronUp : ICONS.chevronDown}
              </button>
              ${draft.recurrenceExpanded ? `
                <div class="nx-row-between" style="margin-top:6px">
                  <span class="nx-body-sm">Repetir automaticamente</span>
                  <button type="button" class="nx-switch ${draft.recurrenceEnabled ? 'is-on' : ''}" data-action="task-toggle-field" data-field="recurrenceEnabled"></button>
                </div>
                ${draft.recurrenceEnabled ? `
                  <div class="nx-field" style="margin-top:4px">
                    <label class="nx-field-label" for="task-rec-freq">Frequência</label>
                    <select id="task-rec-freq" class="nx-select" data-task-field="recurrenceFrequency">
                      ${Object.entries(LABELS.TaskRecurrenceFrequency).map(([k, l]) => `<option value="${k}" ${draft.recurrenceFrequency === k ? 'selected' : ''}>${esc(l)}</option>`).join('')}
                    </select>
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
                <span class="nx-title-sm">Financeiro da tarefa</span>
                <span class="nx-title-sm tnum">${esc(formatBrl(expenseCents))}</span>
              </div>
              <button type="button" class="nx-btn-outlined" data-action="task-register-expense">
                ${ICONS.receipt} <span>Registrar gasto vinculado a esta tarefa</span>
              </button>
            </div>

            ${dirty ? `<div class="nx-label-sm nx-warning">Alterações não salvas neste rascunho.</div>` : ''}

            <button type="button" class="nx-btn-primary" data-action="save-task-sheet">
              ${draft.isNew ? 'Criar tarefa' : 'Salvar tarefa'}
            </button>
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
