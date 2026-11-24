/*
 * NEXUS Web Mirror Pilot — TasksAndProjectsScreen
 * Fonte Android: TasksAndProjectsScreen.kt, TaskRules.kt
 */
(function () {
  const {
    LABELS,
    addDaysIso,
    computeProjectProgress
  } = window.NexusPilot.Data;
  const {
    esc,
    ICONS,
    renderSectionHeader,
    renderAlertBanner,
    renderTaskRowCard,
    renderProjectCard
  } = window.NexusPilot.Primitives;

  const TASKS_TABS = [
    { key: 'TODAY', label: 'Hoje' },
    { key: 'UPCOMING', label: 'Próximas' },
    { key: 'OVERDUE', label: 'Atrasadas' },
    { key: 'ALL', label: 'Todas' },
    { key: 'COMPLETED', label: 'Concluídas' },
    { key: 'KANBAN', label: 'Kanban' },
    { key: 'PROJECTS', label: 'Projetos' }
  ];

  const KANBAN_LANES = [
    { status: 'NOT_STARTED', label: 'Não iniciada' },
    { status: 'IN_PROGRESS', label: 'Em andamento' },
    { status: 'WAITING', label: 'Aguardando' },
    { status: 'BLOCKED', label: 'Bloqueada' },
    { status: 'COMPLETED', label: 'Concluída' }
  ];

  function isTaskOverdue(task, todayIso) {
    return task.status !== 'COMPLETED' && task.status !== 'CANCELLED' && Boolean(task.dueDateIso) && task.dueDateIso < todayIso;
  }

  function applyCommonFilters(tasks, filter) {
    return tasks.filter((t) => {
      if (filter.statusFilter && t.status !== filter.statusFilter) return false;
      if (filter.priorityFilter && t.priority !== filter.priorityFilter) return false;
      if (filter.projectFilterId && t.projectId !== filter.projectFilterId) return false;
      return true;
    });
  }

  function filterTasksForTab(state, filter) {
    const today = state.todayIso;
    const plus30 = addDaysIso(today, 30);
    const base = applyCommonFilters(state.tasks.filter((t) => t.status !== 'CANCELLED'), filter);

    switch (filter.tabMode) {
      case 'TODAY':
        return base.filter((t) => t.dueDateIso === today);
      case 'UPCOMING':
        return base
          .filter((t) => t.status !== 'COMPLETED' && t.dueDateIso && t.dueDateIso > today && t.dueDateIso <= plus30)
          .sort((a, b) => a.dueDateIso.localeCompare(b.dueDateIso));
      case 'OVERDUE':
        return base
          .filter((t) => isTaskOverdue(t, today))
          .sort((a, b) => a.dueDateIso.localeCompare(b.dueDateIso));
      case 'COMPLETED':
        return base.filter((t) => t.status === 'COMPLETED');
      case 'KANBAN':
      case 'ALL':
      default:
        return base;
    }
  }

  function groupTasks(tasks, todayIso) {
    const plus7 = addDaysIso(todayIso, 7);
    const buckets = [
      { key: 'OVERDUE', label: 'Atrasadas', items: [] },
      { key: 'TODAY', label: 'Hoje', items: [] },
      { key: 'NEXT_7', label: 'Próximos 7 dias', items: [] },
      { key: 'LATER', label: 'Mais adiante', items: [] },
      { key: 'UNDATED', label: 'Sem prazo', items: [] },
      { key: 'DONE', label: 'Concluídas', items: [] }
    ];
    tasks.forEach((t) => {
      if (t.status === 'COMPLETED') {
        buckets[5].items.push(t);
      } else if (!t.dueDateIso) {
        buckets[4].items.push(t);
      } else if (t.dueDateIso < todayIso) {
        buckets[0].items.push(t);
      } else if (t.dueDateIso === todayIso) {
        buckets[1].items.push(t);
      } else if (t.dueDateIso <= plus7) {
        buckets[2].items.push(t);
      } else {
        buckets[3].items.push(t);
      }
    });
    return buckets.filter((b) => b.items.length > 0);
  }

  function renderKanbanBoard(tasks, state) {
    return `
      <div class="nx-screen-scroll" data-nexus-component="KanbanBoardContent" data-nexus-source="TasksAndProjectsScreen.kt:KanbanBoardContent" data-nexus-status="reproduzido">
        <div class="nx-kanban-scroll">
          ${KANBAN_LANES.map((lane) => {
            const laneTasks = tasks.filter((t) => t.status === lane.status);
            return `
              <div class="nx-kanban-col" data-kanban-lane="${lane.status}">
                <div class="nx-row-between">
                  <span class="nx-title-sm">${esc(lane.label)}</span>
                  <span class="nx-badge is-neutral tnum">${laneTasks.length}</span>
                </div>
                ${laneTasks.length ? laneTasks.map((t) => {
                  const proj = state.projects.find((p) => p.id === t.projectId);
                  const overdue = isTaskOverdue(t, state.todayIso);
                  return `
                    <div class="nx-kanban-card" data-nexus-entity="task:${esc(t.id)}">
                      <div style="cursor:pointer" data-action="open-task" data-task-id="${esc(t.id)}">
                        <div class="nx-title-sm" style="${t.status === 'COMPLETED' ? 'text-decoration:line-through;color:var(--nx-text-secondary)' : ''}">${esc(t.title)}</div>
                        <div class="nx-label-sm nx-text-secondary tnum" style="margin-top:3px">
                          ${LABELS.TaskPriority[t.priority]}${proj ? ` · ${proj.name}` : ''}${overdue ? ' · Atrasada' : ''}
                        </div>
                      </div>
                      <select class="nx-select" style="padding:5px 8px;font-size:11px" aria-label="Mover status da tarefa" data-action="change-task-status-select" data-task-id="${esc(t.id)}">
                        ${KANBAN_LANES.map((opt) => `
                          <option value="${opt.status}" ${t.status === opt.status ? 'selected' : ''}>${esc(opt.label)}</option>
                        `).join('')}
                      </select>
                    </div>
                  `;
                }).join('') : `<p class="nx-body-sm" style="padding:8px 0">Sem tarefas nesta coluna.</p>`}
              </div>
            `;
          }).join('')}
        </div>
      </div>
    `;
  }

  function renderProjectsList(state) {
    const projectCards = state.projects.map((p) => computeProjectProgress(state, p));
    return `
      <div class="nx-screen-scroll" data-nexus-component="ProjectsListContent" data-nexus-source="TasksAndProjectsScreen.kt:ProjectsListContent" data-nexus-status="reproduzido">
        ${projectCards.length ? `
          <div class="nx-grid-2">
            ${projectCards.map((pr) => renderProjectCard(pr, state)).join('')}
          </div>
        ` : `
          <div class="nx-card" style="align-items:center;text-align:center;padding:24px">
            <span class="nx-title-md">Nenhum projeto cadastrado</span>
            <span class="nx-body-sm">Agrupe tarefas e gastos relacionados sob um projeto.</span>
            <button type="button" class="nx-btn-outlined" data-action="new-project">+ Novo projeto</button>
          </div>
        `}
      </div>
    `;
  }

  function renderTasksScreen(state, ui) {
    const filter = ui.tasksFilter || { tabMode: 'TODAY', statusFilter: null, priorityFilter: null, projectFilterId: null };
    const activeFilterCount = [filter.statusFilter, filter.priorityFilter, filter.projectFilterId].filter(Boolean).length;
    const overdueCount = state.tasks.filter((t) => isTaskOverdue(t, state.todayIso)).length;
    const undatedActiveCount = state.tasks.filter((t) => t.status !== 'COMPLETED' && t.status !== 'CANCELLED' && !t.dueDateIso).length;
    const filteredTasks = filterTasksForTab(state, filter);

    let bodyHtml = '';
    if (filter.tabMode === 'PROJECTS') {
      bodyHtml = renderProjectsList(state);
    } else if (filter.tabMode === 'KANBAN') {
      bodyHtml = renderKanbanBoard(filteredTasks, state);
    } else {
      const grouped = (filter.tabMode === 'UPCOMING' || filter.tabMode === 'ALL') ? groupTasks(filteredTasks, state.todayIso) : [];
      const orderedToday = filter.tabMode === 'TODAY'
        ? [...filteredTasks].sort((a, b) => Number(a.status === 'COMPLETED') - Number(b.status === 'COMPLETED'))
        : filteredTasks;

      bodyHtml = `
        <div class="nx-screen-scroll">
          ${filter.tabMode === 'TODAY' && overdueCount > 0 ? renderAlertBanner({
            title: `${overdueCount} ${overdueCount === 1 ? 'tarefa atrasada' : 'tarefas atrasadas'}`,
            subtitle: 'Toque para revisar em Atrasadas sem misturar com as de hoje.',
            severity: 'danger',
            actionAttr: 'data-action="select-tasks-tab" data-tasks-tab="OVERDUE"'
          }) : ''}

          ${activeFilterCount > 0 ? `
            <div class="nx-row-between" style="background:var(--nx-surface-elevated);padding:8px 12px;border-radius:10px;border:1px solid var(--nx-border)">
              <span class="nx-label-md nx-text-secondary">
                Filtros: ${[
                  filter.statusFilter ? LABELS.TaskStatus[filter.statusFilter] : null,
                  filter.priorityFilter ? `Prioridade ${LABELS.TaskPriority[filter.priorityFilter]}` : null,
                  filter.projectFilterId ? (state.projects.find((p) => p.id === filter.projectFilterId)?.name || 'Projeto') : null
                ].filter(Boolean).join(' · ')}
              </span>
              <button type="button" class="nx-btn-text" data-action="clear-tasks-filters">Limpar</button>
            </div>
          ` : ''}

          ${filteredTasks.length === 0 ? `
            <div class="nx-card" style="align-items:center;text-align:center;padding:24px">
              <span class="nx-title-md">Nenhuma tarefa encontrada</span>
              <span class="nx-body-sm">
                ${filter.tabMode === 'TODAY' && undatedActiveCount > 0
                  ? `Você tem ${undatedActiveCount} tarefa(s) sem prazo em Todas.`
                  : 'Crie uma tarefa pelo botão + ou limpe os filtros ativos.'}
              </span>
              ${activeFilterCount > 0
                ? `<button type="button" class="nx-btn-outlined" data-action="clear-tasks-filters">Limpar filtros</button>`
                : filter.tabMode === 'TODAY' && undatedActiveCount > 0
                  ? `<button type="button" class="nx-btn-outlined" data-action="select-tasks-tab" data-tasks-tab="ALL">Ver todas</button>`
                  : `<button type="button" class="nx-btn-outlined" data-action="new-task">+ Nova tarefa</button>`}
            </div>
          ` : (grouped.length > 0 ? grouped.map((sec) => `
            <div style="display:flex;flex-direction:column;gap:8px">
              ${renderSectionHeader({ title: `${sec.label} (${sec.items.length})` })}
              <div class="nx-grid-2">
                ${sec.items.map((t) => renderTaskRowCard(t, state, { showPostpone: t.status !== 'COMPLETED' && isTaskOverdue(t, state.todayIso) })).join('')}
              </div>
            </div>
          `).join('') : `
            <div class="nx-grid-2">
              ${orderedToday.map((t) => renderTaskRowCard(t, state, { showPostpone: t.status !== 'COMPLETED' })).join('')}
            </div>
          `)}

          ${filter.tabMode === 'TODAY' && undatedActiveCount > 0 ? `
            <button type="button" class="nx-card is-elevated is-clickable" data-action="select-tasks-tab" data-tasks-tab="ALL">
              <div class="nx-row-between" style="width:100%">
                <span class="nx-label-lg nx-text-secondary tnum">${undatedActiveCount} tarefa(s) sem prazo no backlog</span>
                <span class="nx-label-lg nx-highlight">Ver Todas</span>
              </div>
            </button>
          ` : ''}
        </div>
      `;
    }

    return `
      <div style="display:flex;flex-direction:column;flex:1;min-height:0" data-nexus-component="TasksAndProjectsScreen" data-nexus-source="TasksAndProjectsScreen.kt" data-nexus-status="reproduzido">
        <div style="padding:12px 16px 8px;display:flex;align-items:center;justify-content:space-between;gap:10px">
          <h2 class="nx-headline-md">Tarefas e Projetos</h2>
          ${filter.tabMode === 'PROJECTS' ? `
            <button type="button" class="nx-btn-outlined" data-action="new-project">+ Novo projeto</button>
          ` : `
            <button type="button" class="nx-btn-outlined ${activeFilterCount > 0 ? 'is-selected' : ''}" data-action="open-tasks-filter-sheet">
              ${ICONS.filter} <span>${activeFilterCount > 0 ? `Filtros (${activeFilterCount})` : 'Filtros'}</span>
            </button>
          `}
        </div>

        <div class="nx-tabs-bar" role="tablist">
          ${TASKS_TABS.map((tab) => {
            const label = tab.key === 'OVERDUE' && overdueCount > 0 ? `${tab.label} (${overdueCount})` : tab.label;
            const isSel = filter.tabMode === tab.key;
            return `
              <button type="button" role="tab" aria-selected="${isSel}" class="nx-tab ${isSel ? 'is-active' : ''}" data-action="select-tasks-tab" data-tasks-tab="${tab.key}">
                ${esc(label)}
              </button>
            `;
          }).join('')}
        </div>

        ${bodyHtml}
      </div>
    `;
  }

  function renderTasksFilterSheet(state, ui) {
    const f = ui.tasksFilter || {};
    return `
      <div class="nx-sheet-backdrop" data-action="close-overlays">
        <div class="nx-sheet" role="dialog" aria-modal="true" data-nexus-component="TasksFilterBottomSheet" data-nexus-source="TasksAndProjectsScreen.kt:TasksFilterBottomSheet" data-nexus-status="reproduzido" onclick="event.stopPropagation()">
          <div class="nx-sheet-handle"></div>
          <div class="nx-sheet-body">
            <div class="nx-row-between">
              <h3 class="nx-title-lg">Filtrar tarefas</h3>
              <button type="button" class="nx-btn-text" data-action="clear-tasks-filters">Limpar</button>
            </div>
            <div class="nx-title-sm">Status</div>
            <div class="nx-chip-row">
              <button type="button" class="nx-chip ${!f.statusFilter ? 'is-active' : ''}" data-action="set-task-filter" data-key="statusFilter" data-value="">Todos</button>
              ${Object.entries(LABELS.TaskStatus).map(([k, l]) => `
                <button type="button" class="nx-chip ${f.statusFilter === k ? 'is-active' : ''}" data-action="set-task-filter" data-key="statusFilter" data-value="${k}">${esc(l)}</button>
              `).join('')}
            </div>
            <div class="nx-title-sm">Prioridade</div>
            <div class="nx-chip-row">
              <button type="button" class="nx-chip ${!f.priorityFilter ? 'is-active' : ''}" data-action="set-task-filter" data-key="priorityFilter" data-value="">Todas</button>
              ${Object.entries(LABELS.TaskPriority).map(([k, l]) => `
                <button type="button" class="nx-chip ${f.priorityFilter === k ? 'is-active' : ''}" data-action="set-task-filter" data-key="priorityFilter" data-value="${k}">${esc(l)}</button>
              `).join('')}
            </div>
            <div class="nx-title-sm">Projeto</div>
            <div class="nx-chip-row">
              <button type="button" class="nx-chip ${!f.projectFilterId ? 'is-active' : ''}" data-action="set-task-filter" data-key="projectFilterId" data-value="">Todos</button>
              ${state.projects.map((p) => `
                <button type="button" class="nx-chip ${f.projectFilterId === p.id ? 'is-active' : ''}" data-action="set-task-filter" data-key="projectFilterId" data-value="${esc(p.id)}">${esc(p.name)}</button>
              `).join('')}
            </div>
            <button type="button" class="nx-btn-primary" style="margin-top:6px" data-action="close-overlays">Aplicar</button>
          </div>
        </div>
      </div>
    `;
  }

  window.NexusPilot = window.NexusPilot || {};
  window.NexusPilot.Screens = window.NexusPilot.Screens || {};
  window.NexusPilot.Screens.Tasks = {
    renderTasksScreen,
    renderTasksFilterSheet
  };
})();
