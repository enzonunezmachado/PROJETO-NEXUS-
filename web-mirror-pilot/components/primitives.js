/*
 * NEXUS Web Mirror Pilot — Core UI Primitives & Semantic Components
 * Fonte Android: NexusComponents.kt, HomeScreen.kt, FinanceScreen.kt, EventsScreen.kt, AgendaScreen.kt, TasksAndProjectsScreen.kt
 */
(function () {
  const {
    LABELS,
    formatBrl,
    formatDateBr,
    formatMonthTitle,
    computeTaskCounts,
    computeTaskExpenseCents
  } = window.NexusPilot.Data;

  function esc(str) {
    return String(str ?? '')
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;');
  }

  // Coordenadas exatas de buildNexusWordmarkPath() em NexusComponents.kt (viewBox 0 0 318 64)
  function renderWordmarkSvg(heightPx = 17) {
    const widthPx = (heightPx * (318 / 64)).toFixed(2);
    return `
      <svg width="${widthPx}" height="${heightPx}" viewBox="0 0 318 64" fill="currentColor" aria-label="NEXUS" role="img" style="display:block;color:var(--nx-highlight)">
        <path d="M12 54 L12 17 L19 10 L24.8 10 L44.8 38.6 L46.5 38.6 L46.5 13.5 L56 10 L56 47 L49 54 L43.2 54 L23.2 25.4 L21.5 25.4 L21.5 50.5 Z"/>
        <rect x="74" y="10" width="9.5" height="44"/>
        <rect x="87" y="10" width="31" height="8.5"/>
        <path d="M87 27.8 L112 27.8 L108.5 36.2 L87 36.2 Z"/>
        <rect x="87" y="45.5" width="31" height="8.5"/>
        <path d="M136 10 L147.5 10 L182 54 L170.5 54 Z"/>
        <path d="M170.5 10 L182 10 L166.9 29.1 L161.2 21.8 Z"/>
        <path d="M156.8 42.2 L151.1 34.9 L136 54 L147.5 54 Z"/>
        <path d="M200 10 L209.5 10 L209.5 40.5 Q209.5 45.5 214.5 45.5 L229.5 45.5 Q234.5 45.5 234.5 40.5 L234.5 10 L244 10 L244 42 Q244 54 232 54 L212 54 Q200 54 200 42 Z"/>
        <path d="M306 10 L274 10 Q262 10 262 22 L262 25.5 Q262 36 274 36 L292.5 36 Q296.5 36 296.5 40 L296.5 41.5 Q296.5 45.5 292.5 45.5 L265.5 45.5 L262 54 L294 54 Q306 54 306 42 L306 38.5 Q306 28 294 28 L275.5 28 Q271.5 28 271.5 24 L271.5 22.5 Q271.5 18.5 275.5 18.5 L302.5 18.5 Z"/>
      </svg>
    `;
  }

  const ICONS = {
    home: '<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M3 10.5 12 3l9 7.5V20a1 1 0 0 1-1 1h-5v-6H9v6H4a1 1 0 0 1-1-1z"/></svg>',
    agenda: '<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="4" width="18" height="17" rx="2"/><path d="M16 2v4M8 2v4M3 10h18"/></svg>',
    tasks: '<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="9"/><path d="m8.5 12.2 2.4 2.4 4.8-5"/></svg>',
    finance: '<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M19 7V5a2 2 0 0 0-2-2H5a2 2 0 0 0 0 4h14a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5"/><circle cx="16.5" cy="13" r="1"/></svg>',
    events: '<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M3 9a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2v2a2 2 0 0 0 0 4v2a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-2a2 2 0 0 0 0-4z"/><path d="M13 7v10"/></svg>',
    market: '<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M3 17l6-6 4 4 8-8"/><path d="M17 7h4v4"/></svg>',
    search: '<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><circle cx="11" cy="11" r="7"/><path d="m20 20-3.5-3.5"/></svg>',
    more: '<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><circle cx="5" cy="12" r="1.3"/><circle cx="12" cy="12" r="1.3"/><circle cx="19" cy="12" r="1.3"/></svg>',
    add: '<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M12 5v14M5 12h14"/></svg>',
    back: '<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M19 12H5"/><path d="m12 19-7-7 7-7"/></svg>',
    close: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M18 6 6 18M6 6l12 12"/></svg>',
    customize: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="7" height="7" rx="1"/><rect x="14" y="3" width="7" height="4" rx="1"/><rect x="14" y="10" width="7" height="11" rx="1"/><rect x="3" y="13" width="7" height="8" rx="1"/></svg>',
    bell: '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/><path d="M13.73 21a2 2 0 0 1-3.46 0"/></svg>',
    sun: '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="4"/><path d="M12 2v2M12 20v2M4.93 4.93l1.41 1.41M17.66 17.66l1.41 1.41M2 12h2M20 12h2M6.34 17.66l-1.41 1.41M19.07 4.93l-1.41 1.41"/></svg>',
    moon: '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/></svg>',
    checkCircle: '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="9"/><path d="m8.5 12.2 2.4 2.4 4.8-5"/></svg>',
    circle: '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><circle cx="12" cy="12" r="9"/></svg>',
    check: '<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M20 6 9 17l-5-5"/></svg>',
    receipt: '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M4 3v18l2.5-1.5L9 21l3-1.5L15 21l2.5-1.5L20 21V3z"/><path d="M8 8h8M8 12h8M8 16h5"/></svg>',
    card: '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><rect x="2" y="5" width="20" height="14" rx="2"/><path d="M2 10h20"/></svg>',
    warning: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round"><path d="M10.29 3.86 1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/><path d="M12 9v4M12 17h.01"/></svg>',
    danger: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="9"/><path d="M12 8v5M12 16h.01"/></svg>',
    chevLeft: '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m15 18-6-6 6-6"/></svg>',
    chevRight: '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m9 18 6-6-6-6"/></svg>',
    chevronDown: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m6 9 6 6 6-6"/></svg>',
    chevronUp: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m18 15-6-6-6 6"/></svg>',
    edit: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M12 20h9"/><path d="M16.5 3.5a2.1 2.1 0 0 1 3 3L7 19l-4 1 1-4z"/></svg>',
    filter: '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round"><path d="M3 6h18M6 12h12M10 18h4"/></svg>',
    download: '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round"><path d="M12 3v12m0 0-4-4m4 4 4-4"/><path d="M4 17v2a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-2"/></svg>',
    trash: '<svg width="19" height="19" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M3 6h18M8 6V4h8v2M6 6l1 14h10l1-14"/></svg>',
    link: '<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round"><path d="M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71"/><path d="M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71"/></svg>',
    star: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/></svg>',
    starFilled: '<svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor" stroke="currentColor" stroke-width="1.5" stroke-linejoin="round"><polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/></svg>',
    flag: '<svg width="19" height="19" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M4 15s1-1 4-1 5 2 8 2 4-1 4-1V3s-1 1-4 1-5-2-8-2-4 1-4 1z"/><line x1="4" y1="22" x2="4" y2="15"/></svg>',
    repeat: '<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round"><path d="m17 2 4 4-4 4"/><path d="M3 11v-1a4 4 0 0 1 4-4h14"/><path d="m7 22-4-4 4-4"/><path d="M21 13v1a4 4 0 0 1-4 4H3"/></svg>',
    clock: '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/></svg>',
    mapPin: '<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M20 10c0 6-8 12-8 12s-8-6-8-12a8 8 0 0 1 16 0Z"/><circle cx="12" cy="10" r="3"/></svg>',
    externalLink: '<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M15 3h6v6"/><path d="M10 14 21 3"/><path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6"/></svg>',
    play: '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round"><polygon points="6 3 20 12 6 21 6 3"/></svg>',
    pause: '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round"><rect x="6" y="4" width="4" height="16"/><rect x="14" y="4" width="4" height="16"/></svg>',
    settings: '<svg width="19" height="19" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 1 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 1 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 1 1-2.83-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 1 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 1 1 2.83-2.83l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 1 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 1 1 2.83 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 1 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z"/></svg>',
    backup: '<svg width="19" height="19" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" y1="3" x2="12" y2="15"/></svg>',
    bug: '<svg width="19" height="19" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><rect x="8" y="6" width="8" height="14" rx="4"/><path d="m19 7-3 2M5 7l3 2M19 19l-3-2M5 19l3-2M20 13h-4M4 13h4M10 3l2 3 2-3"/></svg>',
    widgets: '<svg width="19" height="19" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="7" height="7" rx="1.5"/><rect x="14" y="3" width="7" height="7" rx="1.5"/><rect x="3" y="14" width="7" height="7" rx="1.5"/><path d="M17.5 14v7M14 17.5h7"/></svg>',
    target: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="9"/><circle cx="12" cy="12" r="5"/><circle cx="12" cy="12" r="1.5"/></svg>',
    alertAdd: '<svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round"><path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h10"/><path d="M19 14v6M16 17h6"/></svg>',
    plusCircle: '<svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="9"/><path d="M12 8v8M8 12h8"/></svg>',
    quote: '<svg width="19" height="19" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M3 21c3 0 7-1 7-8V5c0-1.25-.756-2.017-2-2H4c-1.25 0-2 .75-2 1.972V11c0 1.25.75 2 2 2 1 0 1 0 1 1v1c0 1-1 2-2 2s-1 .008-1 1.031V20c0 1 0 1 1 1z"/><path d="M15 21c3 0 7-1 7-8V5c0-1.25-.757-2.017-2-2h-4c-1.25 0-2 .75-2 1.972V11c0 1.25.75 2 2 2h.75c0 2.25.25 4-2.75 4v3c0 1 0 1 1 1z"/></svg>',
    copy: '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><rect x="9" y="9" width="13" height="13" rx="2"/><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/></svg>',
    refresh: '<svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round"><path d="M21 12a9 9 0 0 0-9-9 9.75 9.75 0 0 0-6.74 2.74L3 8"/><path d="M3 3v5h5"/><path d="M3 12a9 9 0 0 0 9 9 9.75 9.75 0 0 0 6.74-2.74L21 16"/><path d="M16 16h5v5"/></svg>',
    folder: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M20 20a2 2 0 0 0 2-2V8a2 2 0 0 0-2-2h-7.9a2 2 0 0 1-1.69-.9L9.6 3.9A2 2 0 0 0 7.93 3H4a2 2 0 0 0-2 2v13a2 2 0 0 0 2 2Z"/></svg>'
  };

  function renderSectionHeader({ title, trailing = null, actionLabel = null, actionAttr = '' }) {
    return `
      <div class="nx-section-header" data-nexus-component="SectionHeader" data-nexus-source="NexusComponents.kt:SectionHeader">
        <div class="nx-section-header-left">
          <h3 class="nx-title-md">${esc(title)}</h3>
          ${trailing !== null ? `<span class="nx-section-trailing tnum">${esc(trailing)}</span>` : ''}
        </div>
        ${actionLabel ? `<button type="button" class="nx-btn-text" ${actionAttr}>${esc(actionLabel)}</button>` : ''}
      </div>
    `;
  }

  function renderAlertBanner({ title, subtitle, severity = 'danger', actionAttr = '' }) {
    const icon = severity === 'warning' ? ICONS.warning : ICONS.danger;
    const colorClass = severity === 'warning' ? 'nx-warning' : severity === 'info' ? 'nx-info' : 'nx-danger';
    return `
      <button type="button" class="nx-alert-banner severity-${severity}" data-nexus-component="AlertBannerCard" data-nexus-source="NexusComponents.kt:AlertBannerCard" ${actionAttr}>
        <span class="${colorClass}" style="display:inline-flex;flex-shrink:0">${icon}</span>
        <span style="display:flex;flex-direction:column;gap:2px;min-width:0;flex:1">
          <span class="nx-label-lg nx-text-primary">${esc(title)}</span>
          <span class="nx-label-sm nx-text-secondary">${esc(subtitle)}</span>
        </span>
      </button>
    `;
  }

  function renderTaskRowCard(task, state, { showPostpone = false } = {}) {
    const isCompleted = task.status === 'COMPLETED';
    const isOverdue = !isCompleted && task.status !== 'CANCELLED' && task.dueDateIso && task.dueDateIso < state.todayIso;
    const proj = state.projects.find((p) => p.id === task.projectId);
    const ev = state.events.find((e) => e.id === task.eventId);
    const counts = computeTaskCounts(task);
    const expenseCents = computeTaskExpenseCents(state, task.id);

    const metaParts = [LABELS.TaskStatus[task.status] || task.status];
    if (proj) metaParts.push(proj.name);
    if (ev) metaParts.push(`Evento: ${ev.name}`);
    if (task.dueDateIso) {
      const dateStr = task.dueDateIso === state.todayIso ? 'Hoje' : task.dueDateIso === '2026-11-24' ? 'Amanhã' : formatDateBr(task.dueDateIso);
      metaParts.push(task.hasDueTime && task.dueTime ? `${dateStr}, às ${task.dueTime}` : dateStr);
    }
    if (task.recurrence) {
      metaParts.push(LABELS.TaskRecurrenceFrequency[task.recurrence.frequency] || 'Recorrente');
    }
    if (counts.subtasksTotal > 0) metaParts.push(`${counts.subtasksDone}/${counts.subtasksTotal} subtarefas`);
    if (expenseCents > 0) metaParts.push(`Gasto: ${formatBrl(expenseCents)}`);

    const prioColor =
      task.priority === 'URGENT' ? 'var(--nx-danger)'
        : task.priority === 'HIGH' ? 'var(--nx-warning)'
          : task.priority === 'LOW' ? 'var(--nx-text-secondary)'
            : 'var(--nx-text-primary)';

    return `
      <div class="nx-card is-elevated" data-nexus-component="TaskRowCard" data-nexus-source="NexusComponents.kt:TaskRowCard" data-nexus-entity="task:${esc(task.id)}">
        <div class="nx-item-row">
          <button type="button" class="nx-check-btn ${isCompleted ? 'nx-positive' : 'nx-text-secondary'}" data-action="toggle-task" data-task-id="${esc(task.id)}" aria-label="${isCompleted ? 'Reabrir tarefa' : 'Concluir tarefa'}">
            ${isCompleted ? ICONS.checkCircle : ICONS.circle}
          </button>
          <div style="flex:1;min-width:0;display:flex;flex-direction:column;gap:6px;cursor:pointer" data-action="open-task" data-task-id="${esc(task.id)}">
            <div class="nx-title-md" style="${isCompleted ? 'text-decoration:line-through;color:var(--nx-text-secondary)' : ''}">${esc(task.title)}</div>
            <div style="display:flex;align-items:center;gap:6px;flex-wrap:wrap">
              ${isOverdue
                ? `<span class="nx-dot" style="background:var(--nx-danger)"></span><span class="nx-label-sm nx-danger" style="font-weight:600">Atrasada</span>`
                : `<span class="nx-dot" style="background:${prioColor}"></span>`}
              <span class="nx-meta-line tnum">${esc(metaParts.join(' · '))}</span>
            </div>
          </div>
        </div>
        ${showPostpone ? `
          <div class="nx-row-end">
            <button type="button" class="nx-btn-text" data-action="postpone-task" data-task-id="${esc(task.id)}">Adiar p/ amanhã</button>
          </div>
        ` : ''}
      </div>
    `;
  }

  function renderTransactionRowCard(tx, state, { compact = false } = {}) {
    const isSettled = tx.status === 'SETTLED';
    const isIncome = tx.type === 'INCOME';
    const cat = state.categories.find((c) => c.id === tx.categoryId);
    const card = state.creditCards.find((c) => c.id === tx.creditCardId);
    const ev = state.events.find((e) => e.id === tx.eventId);
    const task = state.tasks.find((t) => t.id === tx.taskId);
    const stmt = state.statements.find((s) => s.id === tx.statementId);

    const meta = compact
      ? [
          formatDateBr(tx.dueDateIso),
          LABELS.FinancialStatus[tx.status] || tx.status,
          cat ? cat.name : null
        ].filter(Boolean).join(' · ')
      : [
          LABELS.FinancialStatus[tx.status] || tx.status,
          LABELS.PaymentMethod[tx.paymentMethod] || tx.paymentMethod,
          formatDateBr(tx.dueDateIso),
          cat ? cat.name : null,
          card ? `${card.name}${stmt ? ` (${stmt.yearMonth})` : ''}` : null,
          task ? `Tarefa: ${task.title}` : null,
          ev ? `Evento: ${ev.name}` : null
        ].filter(Boolean).join(' · ');

    const iconColor =
      tx.status === 'SETTLED' ? 'nx-positive'
        : tx.status === 'OVERDUE' ? 'nx-danger'
          : 'nx-text-secondary';

    return `
      <div class="${compact ? '' : 'nx-card is-elevated is-clickable'}" data-nexus-component="${compact ? 'CompactOverviewTransactionRow' : 'TransactionRowCard'}" data-nexus-source="FinanceScreen.kt" data-nexus-entity="transaction:${esc(tx.id)}">
        <div class="nx-item-row" style="align-items:center">
          <button type="button" class="nx-check-btn ${iconColor}" data-action="toggle-settle-tx" data-tx-id="${esc(tx.id)}" aria-label="${isSettled ? 'Reabrir lançamento' : 'Marcar realizado'}">
            ${isSettled ? ICONS.checkCircle : ICONS.circle}
          </button>
          <div style="flex:1;min-width:0;cursor:pointer" data-action="open-tx" data-tx-id="${esc(tx.id)}">
            <div class="${compact ? 'nx-body-md' : 'nx-title-md'}" style="white-space:nowrap;overflow:hidden;text-overflow:ellipsis">${esc(tx.description)}</div>
            <div class="${compact ? 'nx-label-sm' : 'nx-label-md'}" style="color:${tx.status === 'OVERDUE' ? 'var(--nx-danger)' : 'var(--nx-text-secondary)'}">${esc(meta)}</div>
          </div>
          <div class="${compact ? 'nx-label-lg' : 'nx-title-md'} tnum" style="font-weight:600;color:${isIncome ? 'var(--nx-positive)' : 'var(--nx-text-primary)'};cursor:pointer;white-space:nowrap" data-action="open-tx" data-tx-id="${esc(tx.id)}">
            ${isIncome ? '+' : '-'}${esc(formatBrl(tx.amountCents))}
          </div>
        </div>
      </div>
    `;
  }

  function renderEventSummaryCard(eventDetails) {
    const ev = eventDetails.event;
    const place = [ev.locationName, ev.city].filter(Boolean).join(', ');
    const dateLabel = ev.isAllDay
      ? `${formatDateBr(ev.startDateIso)} · Dia inteiro`
      : `${formatDateBr(ev.startDateIso)} · ${ev.startTime}–${ev.endTime}`;
    const meta = [dateLabel, place, eventDetails.category ? eventDetails.category.name : null].filter(Boolean).join(' · ');

    const ticketColor =
      ev.ticketStatus === 'PAID' || ev.ticketStatus === 'BOUGHT' || ev.ticketStatus === 'FREE' ? 'var(--nx-positive)'
        : ev.ticketStatus === 'CANCELLED' ? 'var(--nx-danger)'
          : 'var(--nx-warning)';

    const extraParts = [];
    if (eventDetails.estimatedTotalCostCents > 0) {
      extraParts.push(`Custo total: ${formatBrl(eventDetails.estimatedTotalCostCents)}`);
      extraParts.push(`Pago: ${formatBrl(eventDetails.paidTotalCostCents)}`);
    }
    if (eventDetails.linkedTasks.length > 0) {
      extraParts.push(`${eventDetails.linkedTasks.length} tarefa(s)`);
    }

    return `
      <button type="button" class="nx-card is-elevated is-clickable" data-nexus-component="EventSummaryCard" data-nexus-source="EventsScreen.kt:EventSummaryCard" data-nexus-entity="event:${esc(ev.id)}" data-action="open-event" data-event-id="${esc(ev.id)}">
        <div class="nx-row-between" style="width:100%">
          <span class="nx-title-md" style="text-align:left">${esc(ev.name)}</span>
          <span class="nx-label-md" style="color:${ticketColor};white-space:nowrap">${esc(LABELS.TicketStatus[ev.ticketStatus] || ev.ticketStatus)}</span>
        </div>
        <span class="nx-label-md nx-text-secondary" style="text-align:left">${esc(meta)}</span>
        ${extraParts.length ? `<span class="nx-label-md nx-text-secondary tnum" style="text-align:left">${esc(extraParts.join(' · '))}</span>` : ''}
      </button>
    `;
  }

  function renderAppointmentCompactCard(appt, showConflict = true) {
    const timeSummary = appt.isAllDay ? `${formatDateBr(appt.dateIso)} · Dia inteiro` : `${formatDateBr(appt.dateIso)} · ${appt.startTime}–${appt.endTime}`;
    const meta = [timeSummary, appt.categoryName, appt.location].filter(Boolean).join(' · ');
    return `
      <button type="button" class="nx-card is-elevated is-clickable" data-nexus-component="AppointmentCompactCard" data-nexus-source="HomeScreen.kt:AppointmentCompactCard" data-nexus-entity="appointment:${esc(appt.id)}" data-action="open-appointment" data-appointment-id="${esc(appt.id)}">
        <div class="nx-title-md" style="text-align:left">${esc(appt.title)}</div>
        <div class="nx-label-md nx-text-secondary tnum" style="text-align:left">${esc(meta)}</div>
        ${showConflict && appt.conflictTitles && appt.conflictTitles.length ? `
          <div style="display:flex;align-items:center;gap:6px;margin-top:2px" class="nx-warning">
            ${ICONS.warning}
            <span class="nx-label-md">Conflito com: ${esc(appt.conflictTitles.join(', '))}</span>
          </div>
        ` : ''}
      </button>
    `;
  }

  function renderProjectCard(pr, state) {
    const p = pr.project;
    const goal = state.personalGoals.find((g) => g.id === p.goalId);
    const statusTone = p.status === 'COMPLETED' ? 'is-positive' : p.status === 'ON_HOLD' ? 'is-warning' : 'is-info';
    return `
      <button type="button" class="nx-card is-elevated is-clickable" data-nexus-component="ProjectCard" data-nexus-source="TasksAndProjectsScreen.kt:ProjectsListContent" data-nexus-entity="project:${esc(p.id)}" data-action="open-project" data-project-id="${esc(p.id)}">
        <div class="nx-row-between" style="width:100%">
          <span class="nx-title-md" style="text-align:left">${esc(p.name)}</span>
          <span class="nx-badge ${statusTone}">${esc(LABELS.ProjectStatus[p.status] || p.status)}</span>
        </div>
        ${p.description ? `<span class="nx-body-sm" style="text-align:left">${esc(p.description)}</span>` : ''}
        <div class="nx-progress-track">
          <div class="nx-progress-fill" style="width:${pr.progressPercent}%;background:${p.status === 'COMPLETED' ? 'var(--nx-positive)' : 'var(--nx-accent-neon)'}"></div>
        </div>
        <div class="nx-row-between nx-label-md nx-text-secondary tnum" style="width:100%">
          <span>${pr.completedTasks}/${pr.totalTasks} tarefa(s) · ${pr.progressPercent}%</span>
          <span>${p.deadlineDateIso ? `Prazo ${formatDateBr(p.deadlineDateIso)}` : 'Sem prazo'}</span>
        </div>
        ${(pr.estimatedCostCents > 0 || goal) ? `
          <div class="nx-label-sm nx-text-secondary tnum" style="text-align:left">
            ${pr.estimatedCostCents > 0 ? `Custos: ${formatBrl(pr.estimatedCostCents)} (pago ${formatBrl(pr.paidCostCents)})` : ''}
            ${pr.estimatedCostCents > 0 && goal ? ' · ' : ''}
            ${goal ? `Objetivo: ${goal.title}` : ''}
          </div>
        ` : ''}
      </button>
    `;
  }

  function renderAgendaOccurrenceRow(occ, state, { showDateInMeta = false } = {}) {
    let leadingHtml = '';
    if (occ.sourceType === 'TASK') {
      leadingHtml = `
        <button type="button" class="nx-occ-badge-task ${occ.isDoneOrSettled ? 'is-done' : ''}" data-action="toggle-task" data-task-id="${esc(occ.sourceId)}" aria-label="${occ.isDoneOrSettled ? 'Reabrir tarefa' : 'Concluir tarefa'}">
          ${occ.isDoneOrSettled ? ICONS.check : ''}
        </button>
      `;
    } else if (occ.sourceType === 'TRANSACTION') {
      leadingHtml = `
        <button type="button" class="nx-check-btn ${occ.isDoneOrSettled ? 'nx-positive' : occ.isOverdue ? 'nx-danger' : 'nx-warning'}" data-action="toggle-settle-tx" data-tx-id="${esc(occ.sourceId)}" aria-label="Alternar pagamento">
          ${occ.isDoneOrSettled ? ICONS.checkCircle : ICONS.circle}
        </button>
      `;
    } else if (occ.sourceType === 'APPOINTMENT') {
      leadingHtml = `<span class="nx-occ-bar-appt" style="background:${esc(occ.colorHex || 'var(--nx-info)')}"></span>`;
    } else if (occ.sourceType === 'EVENT') {
      leadingHtml = `<span class="nx-occ-badge-box" style="color:var(--nx-accent-neon);border-color:rgba(124,255,178,0.45)">EV</span>`;
    } else if (occ.sourceType === 'STATEMENT') {
      leadingHtml = `<span class="nx-occ-badge-box" style="color:var(--nx-warning);border-color:rgba(229,160,0,0.45)">FAT</span>`;
    } else {
      leadingHtml = `<span class="nx-occ-badge-box" style="color:var(--nx-info);border-color:rgba(76,139,245,0.45)">PRJ</span>`;
    }

    const metaParts = [];
    if (showDateInMeta) metaParts.push(formatDateBr(occ.occurrenceDateIso));
    if (occ.subtitle) metaParts.push(occ.subtitle);

    return `
      <div class="nx-card is-elevated" data-nexus-component="AgendaOccurrenceRow" data-nexus-source="AgendaScreen.kt:AgendaOccurrenceRow" data-nexus-entity="${esc(occ.sourceType.toLowerCase())}:${esc(occ.sourceId)}">
        <div class="nx-item-row" style="align-items:center">
          ${leadingHtml}
          <div style="flex:1;min-width:0;cursor:pointer;display:flex;flex-direction:column;gap:3px" data-action="open-agenda-occurrence" data-source-type="${esc(occ.sourceType)}" data-source-id="${esc(occ.sourceId)}">
            <div style="display:flex;align-items:center;gap:6px;flex-wrap:wrap">
              <span class="nx-title-md" style="${occ.isDoneOrSettled ? 'text-decoration:line-through;color:var(--nx-text-secondary)' : ''}">${esc(occ.title)}</span>
              ${occ.isOverdue && !occ.isDoneOrSettled ? `<span class="nx-badge is-danger">Atrasado</span>` : ''}
              ${occ.isVirtualRecurrence ? `<span class="nx-badge is-neutral">${ICONS.repeat} Recorrente</span>` : ''}
            </div>
            <span class="nx-label-md nx-text-secondary tnum">${esc(metaParts.join(' · '))}</span>
            ${occ.hasConflict ? `
              <div style="display:flex;align-items:center;gap:5px" class="nx-warning">
                ${ICONS.warning}
                <span class="nx-label-sm">Conflito: ${esc((occ.conflictTitles || []).join(', '))}</span>
              </div>
            ` : ''}
          </div>
          ${occ.amountCents ? `
            <span class="nx-title-sm tnum" style="cursor:pointer;color:${occ.isOverdue ? 'var(--nx-danger)' : 'var(--nx-text-primary)'};white-space:nowrap" data-action="open-agenda-occurrence" data-source-type="${esc(occ.sourceType)}" data-source-id="${esc(occ.sourceId)}">
              ${esc(formatBrl(occ.amountCents))}
            </span>
          ` : ''}
        </div>
      </div>
    `;
  }

  function renderMonthYearSelectorBar(selectedMonthIso, currentMonthIso) {
    const isCurrent = selectedMonthIso === currentMonthIso;
    return `
      <div class="nx-month-bar" data-nexus-component="MonthYearSelectorBar" data-nexus-source="NexusComponents.kt:MonthYearSelectorBar">
        <button type="button" class="nx-icon-btn" data-action="prev-month" aria-label="Mês anterior">${ICONS.chevLeft}</button>
        <div style="display:flex;align-items:center;gap:8px">
          <span class="nx-title-md tnum">${esc(formatMonthTitle(selectedMonthIso))}</span>
          ${!isCurrent ? `<button type="button" class="nx-btn-text nx-accent" data-action="reset-month" style="padding:2px 6px;font-size:11px">Hoje</button>` : ''}
        </div>
        <button type="button" class="nx-icon-btn" data-action="next-month" aria-label="Próximo mês">${ICONS.chevRight}</button>
      </div>
    `;
  }

  window.NexusPilot.Primitives = {
    esc,
    ICONS,
    renderWordmarkSvg,
    renderSectionHeader,
    renderAlertBanner,
    renderTaskRowCard,
    renderTransactionRowCard,
    renderEventSummaryCard,
    renderAppointmentCompactCard,
    renderProjectCard,
    renderAgendaOccurrenceRow,
    renderMonthYearSelectorBar
  };
})();
