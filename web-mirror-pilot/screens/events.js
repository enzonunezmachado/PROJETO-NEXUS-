/*
 * NEXUS Web Mirror Pilot — EventsScreen
 * Fonte Android: EventsScreen.kt
 */
(function () {
  const { computeEventDetails } = window.NexusPilot.Data;
  const { esc, renderSectionHeader, renderEventSummaryCard } = window.NexusPilot.Primitives;

  const EVENT_FILTERS = [
    { key: 'UPCOMING', label: 'Próximos' },
    { key: 'TICKET_PENDING', label: 'Ingresso pendente' },
    { key: 'ALL', label: 'Todos' }
  ];

  function renderEventsScreen(state, ui) {
    const filterKey = ui.eventsFilter || 'UPCOMING';
    const filtered = state.events.filter((ev) => {
      if (filterKey === 'UPCOMING') return ev.eventStatus !== 'CANCELLED' && ev.startDateIso >= state.todayIso;
      if (filterKey === 'TICKET_PENDING') return ev.ticketStatus === 'NOT_BOUGHT' || ev.ticketStatus === 'PENDING' || ev.ticketStatus === 'PARTIALLY_PAID';
      return true;
    }).sort((a, b) => a.startDateIso.localeCompare(b.startDateIso));

    const detailsList = filtered.map((ev) => computeEventDetails(state, ev));

    return `
      <div class="nx-screen-scroll" data-nexus-component="EventsScreen" data-nexus-source="EventsScreen.kt:EventsScreen" data-nexus-status="reproduzido">
        <div class="nx-screen-header-row">
          <h2 class="nx-headline-md">Eventos</h2>
          <button type="button" class="nx-btn-outlined" data-action="new-event">+ Novo evento</button>
        </div>

        <div class="nx-chip-row">
          ${EVENT_FILTERS.map((f) => `
            <button type="button" class="nx-chip ${filterKey === f.key ? 'is-active' : ''}" data-action="set-events-filter" data-filter="${f.key}">
              ${esc(f.label)}
            </button>
          `).join('')}
        </div>

        ${renderSectionHeader({
          title: 'Agenda de eventos & custos',
          trailing: `${detailsList.length} evento(s)`
        })}

        ${detailsList.length ? `
          <div class="nx-grid-2">
            ${detailsList.map((ed) => renderEventSummaryCard(ed)).join('')}
          </div>
        ` : `
          <div class="nx-card" style="align-items:center;text-align:center;padding:24px">
            <span class="nx-title-md">Nenhum evento para este filtro</span>
            <button type="button" class="nx-btn-text" data-action="set-events-filter" data-filter="ALL">Ver todos</button>
          </div>
        `}
      </div>
    `;
  }

  window.NexusPilot = window.NexusPilot || {};
  window.NexusPilot.Screens = window.NexusPilot.Screens || {};
  window.NexusPilot.Screens.Events = {
    renderEventsScreen
  };
})();
