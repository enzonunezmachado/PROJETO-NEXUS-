/*
 * NEXUS Web Mirror Pilot — PlanningGoalsScreen & GoalDetailSheet
 * Fonte Android: PlanningGoalsScreen.kt, GoalProgressRules.kt
 */
(function () {
  const {
    LABELS,
    formatDateBr,
    computeGoalDetails
  } = window.NexusPilot.Data;
  const {
    esc,
    ICONS,
    renderSectionHeader,
    renderTaskRowCard,
    renderProjectCard
  } = window.NexusPilot.Primitives;

  const GOAL_STATUS_FILTERS = [
    { key: 'ACTIVE', label: 'Em andamento' },
    { key: 'PAUSED', label: 'Pausados' },
    { key: 'COMPLETED', label: 'Concluídos' },
    { key: 'ALL', label: 'Todos' }
  ];

  function renderPersonalGoalCard(details) {
    const g = details.goal;
    const statusTone = g.status === 'COMPLETED' ? 'is-positive' : g.status === 'PAUSED' ? 'is-warning' : g.status === 'CANCELLED' ? 'is-danger' : 'is-info';
    return `
      <button type="button" class="nx-card is-elevated is-clickable" data-nexus-component="PersonalGoalCard" data-nexus-source="PlanningGoalsScreen.kt:PersonalGoalCard" data-nexus-status="reproduzido" data-nexus-entity="personal-goal:${esc(g.id)}" data-action="open-personal-goal" data-goal-id="${esc(g.id)}">
        <div class="nx-row-between" style="width:100%">
          <span class="nx-title-md" style="text-align:left">${esc(g.title)}</span>
          <span class="nx-badge ${statusTone}">${esc(LABELS.GoalStatus[g.status] || g.status)}</span>
        </div>
        ${g.description ? `<span class="nx-body-sm" style="text-align:left">${esc(g.description)}</span>` : ''}
        <div style="display:flex;align-items:center;gap:6px;flex-wrap:wrap">
          ${details.category ? `<span class="nx-badge is-neutral"><span class="nx-dot" style="background:${esc(details.category.colorHex)}"></span>${esc(details.category.name)}</span>` : ''}
          <span class="nx-badge is-neutral">${g.progressMode === 'AUTO' ? 'Automático' : 'Manual'}</span>
          ${details.isOverdue ? `<span class="nx-badge is-danger">Prazo vencido</span>` : ''}
        </div>
        <div class="nx-progress-track">
          <div class="nx-progress-fill" style="width:${details.progressPercent}%;background:${g.status === 'COMPLETED' ? 'var(--nx-positive)' : 'var(--nx-accent-neon)'}"></div>
        </div>
        <div class="nx-row-between nx-label-sm nx-text-secondary tnum" style="width:100%">
          <span>${details.progressPercent}% · ${details.completedMilestonesCount}/${details.milestones.length} marco(s) · ${details.linkedTasks.length} tarefa(s) · ${details.linkedProjects.length} projeto(s)</span>
          <span>${g.targetDateIso ? formatDateBr(g.targetDateIso) : 'Sem prazo'}</span>
        </div>
      </button>
    `;
  }

  function renderPlanningScreen(state, ui) {
    const pf = ui.planningFilter || { query: '', status: 'ACTIVE', categoryId: null };
    const q = (pf.query || '').trim().toLowerCase();

    const filtered = state.personalGoals
      .filter((g) => {
        if (pf.status && pf.status !== 'ALL' && g.status !== pf.status) return false;
        if (pf.categoryId && g.categoryId !== pf.categoryId) return false;
        if (q && !g.title.toLowerCase().includes(q) && !(g.description || '').toLowerCase().includes(q)) return false;
        return true;
      })
      .map((g) => computeGoalDetails(state, g));

    return `
      <div class="nx-screen-scroll no-bottom-nav" data-nexus-component="PlanningGoalsScreen" data-nexus-source="PlanningGoalsScreen.kt" data-nexus-status="reproduzido">
        <div class="nx-row-between">
          <div>
            <h2 class="nx-headline-md">Planejamento</h2>
            <span class="nx-label-sm nx-text-secondary">Objetivos pessoais, marcos e vínculo com execução</span>
          </div>
          <button type="button" class="nx-btn-outlined" data-action="new-personal-goal">+ Novo objetivo</button>
        </div>

        <div class="nx-field">
          <input id="nx-planning-search" class="nx-input" type="search" placeholder="Buscar objetivos ou descrições..." value="${esc(pf.query || '')}" data-input="planning-search" />
        </div>

        <div class="nx-chip-row">
          ${GOAL_STATUS_FILTERS.map((s) => `
            <button type="button" class="nx-chip ${pf.status === s.key ? 'is-active' : ''}" data-action="set-planning-status" data-status="${s.key}">
              ${esc(s.label)}
            </button>
          `).join('')}
        </div>

        <div class="nx-chip-row">
          <button type="button" class="nx-chip ${!pf.categoryId ? 'is-active' : ''}" data-action="set-planning-category" data-category-id="">Todas categorias</button>
          ${state.categories.map((c) => `
            <button type="button" class="nx-chip ${pf.categoryId === c.id ? 'is-active' : ''}" data-action="set-planning-category" data-category-id="${esc(c.id)}">
              ${esc(c.name)}
            </button>
          `).join('')}
        </div>

        ${renderSectionHeader({ title: 'Objetivos pessoais', trailing: `${filtered.length}` })}

        ${filtered.length ? `
          <div class="nx-grid-2">
            ${filtered.map(renderPersonalGoalCard).join('')}
          </div>
        ` : `
          <div class="nx-card" style="align-items:center;text-align:center;padding:24px">
            <span class="nx-title-md">Nenhum objetivo para este filtro</span>
            <span class="nx-body-sm">Crie um objetivo pessoal para acompanhar marcos, tarefas e projetos.</span>
            <button type="button" class="nx-btn-outlined" data-action="new-personal-goal">+ Novo objetivo</button>
          </div>
        `}
      </div>
    `;
  }

  function renderGoalDetailSheet(state, goalId, ui) {
    const goal = state.personalGoals.find((g) => g.id === goalId);
    if (!goal) return '';
    const d = computeGoalDetails(state, goal);
    const unlinkedTasks = state.tasks.filter((t) => t.goalId !== goal.id && t.status !== 'CANCELLED');
    const unlinkedProjects = state.projects.filter((p) => p.goalId !== goal.id);

    return `
      <div class="nx-sheet-backdrop" data-action="close-goal-detail">
        <div class="nx-sheet" role="dialog" aria-modal="true" data-nexus-component="GoalDetailSheet" data-nexus-source="PlanningGoalsScreen.kt:GoalDetailSheet" data-nexus-status="reproduzido" onclick="event.stopPropagation()">
          <div class="nx-sheet-handle"></div>
          <div class="nx-sheet-body">
            <div class="nx-row-between">
              <h3 class="nx-title-lg">${esc(goal.title)}</h3>
              <div style="display:flex;align-items:center;gap:4px">
                <button type="button" class="nx-icon-btn" data-action="edit-personal-goal" data-goal-id="${esc(goal.id)}" aria-label="Editar objetivo">${ICONS.edit}</button>
                <button type="button" class="nx-icon-btn nx-danger" data-action="request-delete-goal" data-goal-id="${esc(goal.id)}" aria-label="Excluir objetivo">${ICONS.trash}</button>
                <button type="button" class="nx-btn-text" data-action="close-goal-detail">Fechar</button>
              </div>
            </div>

            ${goal.description ? `<p class="nx-body-sm">${esc(goal.description)}</p>` : ''}

            <div class="nx-card is-elevated">
              <div class="nx-row-between">
                <span class="nx-label-md nx-text-secondary">${esc(LABELS.GoalProgressMode[goal.progressMode])}</span>
                <span class="nx-title-md tnum nx-accent" style="font-weight:700">${d.progressPercent}%</span>
              </div>
              <div class="nx-progress-track">
                <div class="nx-progress-fill" style="width:${d.progressPercent}%;background:${goal.status === 'COMPLETED' ? 'var(--nx-positive)' : 'var(--nx-accent-neon)'}"></div>
              </div>
              <div class="nx-row-between nx-label-sm nx-text-secondary tnum">
                <span>Status: ${esc(LABELS.GoalStatus[goal.status])}</span>
                <span>Alvo: ${goal.targetDateIso ? esc(formatDateBr(goal.targetDateIso)) : 'Sem data'}</span>
              </div>
            </div>

            <div class="nx-row-between">
              <span class="nx-title-md">Marcos (${d.completedMilestonesCount}/${d.milestones.length})</span>
            </div>
            ${d.milestones.map((m) => `
              <div class="nx-card is-elevated" style="padding:10px 12px">
                <div class="nx-row-between">
                  <div style="display:flex;align-items:center;gap:10px;min-width:0;flex:1">
                    <button type="button" class="nx-check-btn ${m.isCompleted ? 'nx-positive' : 'nx-text-secondary'}" data-action="toggle-goal-milestone" data-goal-id="${esc(goal.id)}" data-milestone-id="${esc(m.id)}">
                      ${m.isCompleted ? ICONS.checkCircle : ICONS.circle}
                    </button>
                    <div style="min-width:0">
                      <div class="nx-body-md" style="${m.isCompleted ? 'text-decoration:line-through;color:var(--nx-text-secondary)' : ''}">${esc(m.title)}</div>
                      ${m.dueDateIso ? `<div class="nx-label-sm nx-text-secondary tnum">Previsto: ${esc(formatDateBr(m.dueDateIso))}</div>` : ''}
                    </div>
                  </div>
                  <button type="button" class="nx-icon-btn nx-danger" style="width:30px;height:30px" data-action="delete-goal-milestone" data-goal-id="${esc(goal.id)}" data-milestone-id="${esc(m.id)}" aria-label="Remover marco">${ICONS.close}</button>
                </div>
              </div>
            `).join('') || '<span class="nx-body-sm">Nenhum marco cadastrado.</span>'}

            <div style="display:flex;gap:8px;align-items:center">
              <input id="nx-new-milestone-input" class="nx-input" type="text" placeholder="Adicionar novo marco..." value="${esc(ui.newMilestoneTitle || '')}" data-input="new-milestone-title" />
              <button type="button" class="nx-btn-outlined" data-action="add-goal-milestone" data-goal-id="${esc(goal.id)}">+ Marco</button>
            </div>

            <hr class="nx-divider"/>
            <div class="nx-title-md">Tarefas vinculadas (${d.linkedTasks.length})</div>
            ${d.linkedTasks.map((t) => renderTaskRowCard(t, state)).join('') || '<span class="nx-body-sm">Nenhuma tarefa vinculada.</span>'}
            ${unlinkedTasks.length ? `
              <div style="display:flex;gap:8px;align-items:center">
                <select id="nx-link-task-select" class="nx-select" data-action="link-task-to-goal-select" data-goal-id="${esc(goal.id)}">
                  <option value="">+ Vincular tarefa existente...</option>
                  ${unlinkedTasks.map((t) => `<option value="${esc(t.id)}">${esc(t.title)}</option>`).join('')}
                </select>
              </div>
            ` : ''}

            <hr class="nx-divider"/>
            <div class="nx-title-md">Projetos vinculados (${d.linkedProjects.length})</div>
            ${d.linkedProjects.map((pr) => renderProjectCard(pr, state)).join('') || '<span class="nx-body-sm">Nenhum projeto vinculado.</span>'}
            ${unlinkedProjects.length ? `
              <select class="nx-select" data-action="link-project-to-goal-select" data-goal-id="${esc(goal.id)}">
                <option value="">+ Vincular projeto existente...</option>
                ${unlinkedProjects.map((p) => `<option value="${esc(p.id)}">${esc(p.name)}</option>`).join('')}
              </select>
            ` : ''}

            <hr class="nx-divider"/>
            <div class="nx-title-md">Registros de progresso (${d.progressLogs.length})</div>
            ${d.progressLogs.map((pl) => `
              <div class="nx-card is-elevated" style="padding:10px 12px">
                <div class="nx-row-between">
                  <span class="nx-label-md nx-text-secondary tnum">${esc(formatDateBr(pl.dateIso))}</span>
                  <span class="nx-badge is-info tnum">${pl.percent}%</span>
                </div>
                <span class="nx-body-sm" style="color:var(--nx-text-primary)">${esc(pl.note)}</span>
              </div>
            `).join('') || '<span class="nx-body-sm">Sem registros de progresso ainda.</span>'}
            <div style="display:flex;gap:8px;align-items:center">
              <input id="nx-new-goal-log-note" class="nx-input" type="text" placeholder="Nota de evolução..." value="${esc(ui.newGoalLogNote || '')}" data-input="new-goal-log-note" />
              <button type="button" class="nx-btn-outlined" data-action="add-goal-progress-log" data-goal-id="${esc(goal.id)}">Registrar</button>
            </div>
          </div>
        </div>
      </div>
    `;
  }

  window.NexusPilot = window.NexusPilot || {};
  window.NexusPilot.Screens = window.NexusPilot.Screens || {};
  window.NexusPilot.Screens.Planning = {
    renderPersonalGoalCard,
    renderPlanningScreen,
    renderGoalDetailSheet
  };
})();
