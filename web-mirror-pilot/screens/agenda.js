/*
 * NEXUS Web Mirror Pilot — AgendaScreen
 * Fonte Android: AgendaScreen.kt, AgendaEngine.kt
 */
(function () {
  const {
    LABELS,
    addDaysIso,
    getWeekDatesIso,
    parseIsoDate,
    toIsoDate,
    shiftMonthIso,
    formatMonthTitle,
    formatDayMonth,
    formatWeekdayShort,
    formatDateByPattern,
    computeAgendaOccurrences
  } = window.NexusPilot.Data;
  const {
    esc,
    ICONS,
    renderAlertBanner,
    renderAgendaOccurrenceRow
  } = window.NexusPilot.Primitives;

  const VIEW_MODES = [
    { key: 'DAY', label: 'Dia' },
    { key: 'WEEK', label: 'Semana' },
    { key: 'MONTH', label: 'Mês' },
    { key: 'LIST', label: 'Lista' }
  ];

  const KIND_LABELS = {
    APPOINTMENT: 'Compromissos',
    TASK: 'Tarefas',
    EVENT: 'Eventos',
    FINANCE: 'Financeiro',
    PROJECT: 'Projetos'
  };

  function getMonthCalendarCells(monthIso) {
    const [y, m] = monthIso.split('-').map(Number);
    const first = new Date(y, m - 1, 1);
    const lastDay = new Date(y, m, 0).getDate();
    // 0=Dom..6=Sab -> queremos Seg=0..Dom=6
    const startOffset = (first.getDay() + 6) % 7;
    const cells = [];
    for (let i = 0; i < startOffset; i++) cells.push(null);
    for (let d = 1; d <= lastDay; d++) {
      cells.push(toIsoDate(new Date(y, m - 1, d)));
    }
    return cells;
  }

  function renderDateNavigationBar(viewMode, selectedDateIso, todayIso, isCollapsed) {
    let label = '';
    if (viewMode === 'MONTH') {
      label = formatMonthTitle(selectedDateIso.slice(0, 7));
    } else if (viewMode === 'WEEK') {
      const week = getWeekDatesIso(selectedDateIso);
      label = `${formatDayMonth(week[0])} – ${formatDayMonth(week[6])}`;
    } else {
      label = formatDateByPattern(selectedDateIso, 'DD_MM_WEEKDAY');
    }
    const isToday = selectedDateIso === todayIso;
    return `
      <div class="nx-month-bar" data-nexus-component="DateNavigationBar" data-nexus-source="AgendaScreen.kt:DateNavigationBar">
        <button type="button" class="nx-icon-btn" data-action="agenda-nav" data-dir="-1" aria-label="Período anterior">${ICONS.chevLeft}</button>
        <div style="display:flex;align-items:center;gap:8px;min-width:0">
          <span class="nx-title-md tnum" style="white-space:nowrap;overflow:hidden;text-overflow:ellipsis">${esc(label)}</span>
          ${!isToday ? `<button type="button" class="nx-btn-text nx-accent" data-action="agenda-go-today" style="padding:2px 6px;font-size:11px">Hoje</button>` : ''}
          ${(viewMode === 'MONTH' || viewMode === 'WEEK') ? `
            <button type="button" class="nx-icon-btn" style="width:28px;height:28px" data-action="agenda-toggle-calendar" aria-label="${isCollapsed ? 'Expandir' : 'Recolher'}">
              ${isCollapsed ? ICONS.chevronDown : ICONS.chevronUp}
            </button>
          ` : ''}
        </div>
        <button type="button" class="nx-icon-btn" data-action="agenda-nav" data-dir="1" aria-label="Próximo período">${ICONS.chevRight}</button>
      </div>
    `;
  }

  function renderMonthCalendarGrid(selectedDateIso, todayIso, occurrences) {
    const monthIso = selectedDateIso.slice(0, 7);
    const cells = getMonthCalendarCells(monthIso);
    const weekdays = ['Seg', 'Ter', 'Qua', 'Qui', 'Sex', 'Sáb', 'Dom'];
    const occByDay = new Map();
    occurrences.forEach((o) => {
      if (!occByDay.has(o.occurrenceDateIso)) occByDay.set(o.occurrenceDateIso, []);
      occByDay.get(o.occurrenceDateIso).push(o);
    });

    return `
      <div class="nx-card" style="padding:12px" data-nexus-component="MonthCalendarGrid" data-nexus-source="AgendaScreen.kt:MonthCalendarGrid">
        <div class="nx-cal-grid">
          ${weekdays.map((w) => `<div class="nx-cal-weekday">${w}</div>`).join('')}
          ${cells.map((iso) => {
            if (!iso) return '<div></div>';
            const dayNum = Number(iso.slice(8, 10));
            const dayOccs = occByDay.get(iso) || [];
            const isSel = iso === selectedDateIso;
            const isTod = iso === todayIso;
            const hasOverdue = dayOccs.some((o) => o.isOverdue && !o.isDoneOrSettled);
            return `
              <button type="button" class="nx-cal-cell tnum ${isTod ? 'is-today' : ''} ${isSel ? 'is-selected' : ''}" data-action="agenda-select-date" data-date-iso="${iso}">
                <span>${dayNum}</span>
                ${dayOccs.length ? `<span class="nx-cal-dot" style="${!isSel && hasOverdue ? 'background:var(--nx-danger)' : ''}"></span>` : '<span style="height:4px"></span>'}
              </button>
            `;
          }).join('')}
        </div>
      </div>
    `;
  }

  function renderHorizontalWeekBoard(weekDays, selectedDateIso, todayIso, occurrences) {
    return `
      <div class="nx-week-board" data-nexus-component="HorizontalWeekBoard" data-nexus-source="AgendaScreen.kt:HorizontalWeekBoard">
        ${weekDays.map((iso) => {
          const dayOccs = occurrences.filter((o) => o.occurrenceDateIso === iso);
          const isSel = iso === selectedDateIso;
          const isTod = iso === todayIso;
          return `
            <div class="nx-week-day-card ${isTod ? 'is-today' : ''} ${isSel ? 'is-selected' : ''}" data-action="agenda-select-date" data-date-iso="${iso}">
              <div class="nx-row-between">
                <span class="nx-label-md" style="font-weight:700;color:${isSel || isTod ? 'var(--nx-highlight)' : 'var(--nx-text-secondary)'}">${esc(formatWeekdayShort(iso))}</span>
                <span class="nx-title-sm tnum">${esc(formatDayMonth(iso))}</span>
              </div>
              <div class="nx-week-day-items">
                ${dayOccs.length ? dayOccs.map((occ) => `
                  <button type="button" class="nx-week-mini-item" style="border-left-color:${esc(occ.colorHex || 'var(--nx-accent-neon)')}" data-action="open-agenda-occurrence" data-source-type="${esc(occ.sourceType)}" data-source-id="${esc(occ.sourceId)}">
                    <span class="nx-label-sm" style="font-weight:600;color:var(--nx-text-primary);white-space:nowrap;overflow:hidden;text-overflow:ellipsis;width:100%;${occ.isDoneOrSettled ? 'text-decoration:line-through;opacity:0.65' : ''}">${esc(occ.title)}</span>
                    <span class="nx-label-sm nx-text-secondary tnum" style="font-size:10px">${esc(occ.isAllDay ? 'Dia todo' : occ.sortTime)}</span>
                  </button>
                `).join('') : `<span class="nx-label-sm nx-text-secondary" style="opacity:0.7">Livre</span>`}
              </div>
            </div>
          `;
        }).join('')}
      </div>
    `;
  }

  function renderAllDayBand(occ) {
    return `
      <button type="button" class="nx-card is-elevated is-clickable" style="border-left:4px solid ${esc(occ.colorHex || 'var(--nx-accent-neon)')};padding:10px 12px" data-nexus-component="AllDayEventBandCard" data-nexus-source="AgendaScreen.kt:AllDayEventBandCard" data-action="open-agenda-occurrence" data-source-type="${esc(occ.sourceType)}" data-source-id="${esc(occ.sourceId)}">
        <div class="nx-row-between" style="width:100%">
          <span class="nx-title-sm">${esc(occ.title)}</span>
          <span class="nx-badge is-info">Dia inteiro</span>
        </div>
        <span class="nx-label-sm nx-text-secondary">${esc(occ.subtitle)}</span>
      </button>
    `;
  }

  function renderSelectedDayPanel(selectedDateIso, selectedDayItems, state) {
    const dayBands = selectedDayItems.filter((o) => o.kind === 'EVENT' && o.isAllDay);
    const regularItems = selectedDayItems.filter((o) => !(o.kind === 'EVENT' && o.isAllDay));

    return `
      <div class="nx-card" style="padding:12px 14px">
        <div class="nx-row-between">
          <div>
            <div class="nx-title-md tnum">${esc(formatDateByPattern(selectedDateIso, 'WEEKDAY_DD_MM_YYYY'))}</div>
            <div class="nx-label-sm nx-text-secondary tnum">${selectedDayItems.length} ${selectedDayItems.length === 1 ? 'item' : 'itens'} neste dia</div>
          </div>
          <button type="button" class="nx-btn-text" data-action="new-appointment" data-date-iso="${esc(selectedDateIso)}">+ Novo</button>
        </div>
      </div>

      ${selectedDayItems.length === 0 ? `
        <div class="nx-card" style="align-items:center;text-align:center;padding:24px">
          <span class="nx-title-md">Nada marcado para ${esc(formatDayMonth(selectedDateIso))}</span>
          <span class="nx-body-sm">Compromissos, tarefas com prazo, eventos, vencimentos e projetos deste dia aparecem aqui.</span>
          <button type="button" class="nx-btn-outlined" data-action="new-appointment" data-date-iso="${esc(selectedDateIso)}">+ Novo compromisso</button>
        </div>
      ` : `
        ${dayBands.map(renderAllDayBand).join('')}
        ${regularItems.map((occ) => renderAgendaOccurrenceRow(occ, state, { showDateInMeta: false })).join('')}
      `}
    `;
  }

  function renderAgendaScreen(state, ui) {
    const af = ui.agendaFilter || { viewMode: 'WEEK', selectedDateIso: state.todayIso, kindFilter: null, categoryFilterId: null, showPaid: false, isCalendarExpanded: true };
    const viewMode = af.viewMode || 'WEEK';
    const selectedDateIso = af.selectedDateIso || state.todayIso;
    const isExpanded = af.isCalendarExpanded !== false;
    const isTwoPane = ui.viewport === 'tablet-landscape' || ui.viewport === 'fluid';

    const windowStart = addDaysIso(selectedDateIso.slice(0, 7) + '-01', -10);
    const windowEnd = addDaysIso(shiftMonthIso(selectedDateIso.slice(0, 7), 1) + '-28', 15);
    const occurrences = computeAgendaOccurrences(state, windowStart, windowEnd, af);

    const weekDays = getWeekDatesIso(selectedDateIso);
    const weekStart = weekDays[0];
    const weekEnd = weekDays[6];

    const displayedOccurrences = viewMode === 'WEEK'
      ? occurrences.filter((o) => o.occurrenceDateIso >= weekStart && o.occurrenceDateIso <= weekEnd)
      : viewMode === 'LIST'
        ? occurrences
        : occurrences.filter((o) => o.occurrenceDateIso === selectedDateIso);

    const conflictsInView = Array.from(
      new Set(
        displayedOccurrences
          .filter((o) => o.sourceType === 'APPOINTMENT' && o.hasConflict)
          .map((o) => o.sourceId)
      )
    );

    const selectedDayItems = occurrences.filter((o) => o.occurrenceDateIso === selectedDateIso);
    const activeFilterCount = [af.kindFilter, af.categoryFilterId, af.showPaid ? 'PAID' : null].filter(Boolean).length;

    const headerHtml = `
      <div style="display:flex;flex-direction:column;gap:10px;padding:${isTwoPane ? '10px 20px' : '12px 16px 6px'};flex-shrink:0">
        <div class="nx-row-between">
          <h2 class="nx-headline-md">Agenda</h2>
          <div style="display:flex;gap:6px;align-items:center">
            ${(viewMode === 'MONTH' || viewMode === 'WEEK') ? `
              <button type="button" class="nx-btn-text" data-action="agenda-toggle-calendar">
                ${isExpanded ? 'Recolher' : 'Expandir'}
              </button>
            ` : ''}
            <button type="button" class="nx-btn-outlined ${activeFilterCount > 0 ? 'is-selected' : ''}" data-action="open-agenda-filter-sheet">
              ${ICONS.filter} <span>${activeFilterCount > 0 ? `Filtros (${activeFilterCount})` : 'Filtros'}</span>
            </button>
          </div>
        </div>
        <div class="nx-seg-full" role="tablist">
          ${VIEW_MODES.map((m) => `
            <button type="button" role="tab" aria-selected="${viewMode === m.key}" class="nx-seg-item ${viewMode === m.key ? 'is-active' : ''}" data-action="agenda-set-view" data-view-mode="${m.key}">
              ${esc(m.label)}
            </button>
          `).join('')}
        </div>
      </div>
    `;

    const conflictBanner = conflictsInView.length > 0 ? renderAlertBanner({
      title: `${conflictsInView.length} compromisso(s) com conflito de horário`,
      subtitle: 'Verifique os horários sobrepostos na lista abaixo.',
      severity: 'warning'
    }) : '';

    if (viewMode === 'LIST') {
      return `
        <div style="display:flex;flex-direction:column;flex:1;min-height:0" data-nexus-component="AgendaScreen" data-nexus-source="AgendaScreen.kt" data-nexus-status="reproduzido">
          ${headerHtml}
          <div class="nx-screen-scroll">
            ${conflictBanner}
            ${displayedOccurrences.length ? `
              <div class="nx-grid-2">
                ${displayedOccurrences.map((occ) => renderAgendaOccurrenceRow(occ, state, { showDateInMeta: true })).join('')}
              </div>
            ` : `
              <div class="nx-card" style="align-items:center;text-align:center;padding:24px">
                <span class="nx-title-md">Nenhum item encontrado</span>
                <button type="button" class="nx-btn-outlined" data-action="new-appointment" data-date-iso="${esc(selectedDateIso)}">+ Novo compromisso</button>
              </div>
            `}
          </div>
        </div>
      `;
    }

    if (isTwoPane) {
      return `
        <div style="display:flex;flex-direction:column;flex:1;min-height:0" data-nexus-component="AgendaScreen" data-nexus-source="AgendaScreen.kt:TwoPane" data-nexus-status="reproduzido">
          ${headerHtml}
          <div class="nx-two-pane-agenda" style="padding:0 20px 16px">
            <div class="nx-pane-scroll" style="padding-bottom:84px">
              ${renderDateNavigationBar(viewMode, selectedDateIso, state.todayIso, !isExpanded)}
              ${conflictBanner}
              ${viewMode === 'WEEK' && isExpanded ? renderHorizontalWeekBoard(weekDays, selectedDateIso, state.todayIso, displayedOccurrences) : ''}
              ${(viewMode === 'DAY' || (viewMode === 'MONTH' && isExpanded)) ? renderMonthCalendarGrid(selectedDateIso, state.todayIso, occurrences) : ''}
            </div>
            <div class="nx-pane-scroll" style="padding-bottom:84px">
              ${renderSelectedDayPanel(selectedDateIso, selectedDayItems, state)}
            </div>
          </div>
        </div>
      `;
    }

    return `
      <div style="display:flex;flex-direction:column;flex:1;min-height:0" data-nexus-component="AgendaScreen" data-nexus-source="AgendaScreen.kt" data-nexus-status="reproduzido">
        ${headerHtml}
        <div class="nx-screen-scroll">
          ${renderDateNavigationBar(viewMode, selectedDateIso, state.todayIso, !isExpanded)}
          ${viewMode === 'MONTH' && isExpanded ? renderMonthCalendarGrid(selectedDateIso, state.todayIso, occurrences) : ''}
          ${conflictBanner}
          ${viewMode === 'WEEK' ? `
            ${isExpanded ? renderHorizontalWeekBoard(weekDays, selectedDateIso, state.todayIso, displayedOccurrences) : ''}
            ${renderSelectedDayPanel(selectedDateIso, selectedDayItems, state)}
          ` : renderSelectedDayPanel(selectedDateIso, selectedDayItems, state)}
        </div>
      </div>
    `;
  }

  function renderAgendaFilterSheet(state, ui) {
    const af = ui.agendaFilter || {};
    return `
      <div class="nx-sheet-backdrop" data-action="close-overlays">
        <div class="nx-sheet" role="dialog" aria-modal="true" data-nexus-component="AgendaFilterBottomSheet" data-nexus-source="AgendaScreen.kt:AgendaFilterSheet" data-nexus-status="reproduzido" onclick="event.stopPropagation()">
          <div class="nx-sheet-handle"></div>
          <div class="nx-sheet-body">
            <div class="nx-row-between">
              <h3 class="nx-title-lg">Filtros da Agenda</h3>
              <button type="button" class="nx-btn-text" data-action="clear-agenda-filters">Limpar</button>
            </div>
            <div class="nx-title-sm">Tipo de ocorrência</div>
            <div class="nx-chip-row">
              <button type="button" class="nx-chip ${!af.kindFilter ? 'is-active' : ''}" data-action="set-agenda-filter" data-key="kindFilter" data-value="">Todos</button>
              ${Object.entries(KIND_LABELS).map(([k, l]) => `
                <button type="button" class="nx-chip ${af.kindFilter === k ? 'is-active' : ''}" data-action="set-agenda-filter" data-key="kindFilter" data-value="${k}">${esc(l)}</button>
              `).join('')}
            </div>
            <div class="nx-title-sm">Categoria</div>
            <div class="nx-chip-row">
              <button type="button" class="nx-chip ${!af.categoryFilterId ? 'is-active' : ''}" data-action="set-agenda-filter" data-key="categoryFilterId" data-value="">Todas</button>
              ${state.categories.map((c) => `
                <button type="button" class="nx-chip ${af.categoryFilterId === c.id ? 'is-active' : ''}" data-action="set-agenda-filter" data-key="categoryFilterId" data-value="${esc(c.id)}">${esc(c.name)}</button>
              `).join('')}
            </div>
            <div class="nx-row-between" style="padding:4px 0">
              <span class="nx-body-md">Mostrar contas já pagas/liquidadas</span>
              <button type="button" class="nx-switch ${af.showPaid ? 'is-on' : ''}" data-action="toggle-agenda-show-paid"></button>
            </div>
            <button type="button" class="nx-btn-primary" data-action="close-overlays">Aplicar</button>
          </div>
        </div>
      </div>
    `;
  }

  window.NexusPilot = window.NexusPilot || {};
  window.NexusPilot.Screens = window.NexusPilot.Screens || {};
  window.NexusPilot.Screens.Agenda = {
    renderAgendaScreen,
    renderAgendaFilterSheet
  };
})();
