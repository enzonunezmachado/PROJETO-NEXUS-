/*
 * NEXUS Web Mirror Pilot — Interactive SVG Charts
 * Fonte Android: ui/components/charts/{TaskRingChart,BarGroupChart,DonutChart,ProgressBarRow}.kt e MarketAlertSheetsAndCards.kt
 */
(function () {
  const { formatBrl, formatMarketValue } = window.NexusPilot.Data;
  const { esc } = window.NexusPilot.Primitives;

  function polarToCartesian(cx, cy, r, angleDeg) {
    const rad = ((angleDeg - 90) * Math.PI) / 180;
    return { x: cx + r * Math.cos(rad), y: cy + r * Math.sin(rad) };
  }

  function describeArc(cx, cy, r, startAngle, endAngle) {
    const span = Math.max(0.5, endAngle - startAngle);
    if (span >= 359.5) {
      return `M ${cx} ${cy - r} A ${r} ${r} 0 1 1 ${cx - 0.01} ${cy - r}`;
    }
    const start = polarToCartesian(cx, cy, r, startAngle);
    const end = polarToCartesian(cx, cy, r, endAngle);
    const largeArcFlag = span <= 180 ? '0' : '1';
    return `M ${start.x.toFixed(2)} ${start.y.toFixed(2)} A ${r} ${r} 0 ${largeArcFlag} 1 ${end.x.toFixed(2)} ${end.y.toFixed(2)}`;
  }

  function renderTaskRingChart(dist, periodLabel, { showMetricSummary = true } = {}) {
    const total = Math.max(0, dist.completedCount + dist.inProgressCount + dist.pendingCount);
    const completionPercent = total === 0 ? 0 : Math.floor((dist.completedCount * 100) / total);
    const segments = [
      { kind: 'COMPLETED', label: 'Concluídas', count: dist.completedCount, color: 'var(--nx-accent-neon)' },
      { kind: 'IN_PROGRESS', label: 'Em andamento', count: dist.inProgressCount, color: 'var(--nx-info)' },
      { kind: 'PENDING', label: 'Pendentes', count: dist.pendingCount, color: 'var(--nx-chart-secondary)' }
    ];
    const nonZero = segments.filter((s) => s.count > 0);
    const gapDeg = nonZero.length > 1 ? 4 : 0;
    const avail = 360 - gapDeg * nonZero.length;
    let cursor = 0;

    const arcsSvg = nonZero
      .map((seg) => {
        const sweep = Math.max(2, (seg.count / total) * avail);
        const d = describeArc(58, 58, 49, cursor, cursor + sweep);
        cursor += sweep + gapDeg;
        const cap = nonZero.length === 1 ? 'butt' : 'round';
        return `<path d="${d}" fill="none" stroke="${seg.color}" stroke-width="14" stroke-linecap="${cap}" style="cursor:pointer" data-action="open-tasks-tab"/>`;
      })
      .join('');

    const overdueText =
      dist.overdueCount === 0 ? '0 atrasadas'
        : dist.overdueCount === 1 ? '1 tarefa atrasada'
          : `${dist.overdueCount} tarefas atrasadas`;

    return `
      <div data-nexus-component="TaskRingChart" data-nexus-source="charts/TaskRingChart.kt" style="display:flex;flex-direction:column;gap:10px">
        <div class="nx-task-ring-wrap">
          <div class="nx-ring-box">
            <svg width="116" height="116" viewBox="0 0 116 116" aria-label="${completionPercent}% concluídas em ${esc(periodLabel)}">
              <circle cx="58" cy="58" r="49" fill="none" stroke="var(--nx-chart-track)" stroke-width="14"/>
              ${arcsSvg}
            </svg>
            <div class="nx-ring-center">
              <span class="nx-title-lg tnum">${total === 0 ? '0%' : `${completionPercent}%`}</span>
              <span class="nx-label-sm nx-text-secondary tnum">${total === 0 ? 'Sem prazo' : `${dist.completedCount}/${total}`}</span>
            </div>
          </div>
          <div style="flex:1;min-width:0;display:flex;flex-direction:column;gap:4px">
            <span class="nx-label-md nx-text-secondary">${esc(periodLabel)}</span>
            ${segments.map((seg) => `
              <button type="button" class="nx-legend-btn" style="padding:3px 4px" data-action="open-tasks-tab">
                <span style="display:inline-flex;align-items:center;gap:8px">
                  <span class="nx-dot" style="background:${seg.color};width:8px;height:8px"></span>
                  <span class="nx-label-md">${esc(seg.label)}</span>
                </span>
                <span class="nx-label-md tnum" style="font-weight:600">${seg.count}</span>
              </button>
            `).join('')}
          </div>
        </div>
        ${showMetricSummary ? `
          <hr class="nx-divider"/>
          <div class="nx-row-between">
            <button type="button" class="nx-btn-text" style="padding:2px 4px;display:inline-flex;align-items:center;gap:6px" data-action="${dist.overdueCount > 0 ? 'open-tasks-overdue' : 'open-tasks-tab'}">
              <span class="nx-dot" style="background:${dist.overdueCount > 0 ? 'var(--nx-danger)' : 'var(--nx-text-secondary)'}"></span>
              <span class="nx-label-md tnum" style="color:${dist.overdueCount > 0 ? 'var(--nx-danger)' : 'var(--nx-text-secondary)'}">${esc(overdueText)}</span>
            </button>
            ${dist.undatedPendingCount > 0 ? `<span class="nx-label-sm nx-text-secondary tnum">${dist.undatedPendingCount} sem prazo</span>` : ''}
          </div>
        ` : ''}
      </div>
    `;
  }

  function renderBarGroupChart({ entries, primaryLabel, secondaryLabel, selectedEntryId, actionAttr = 'data-action="select-bar-month"', emptyMessage = 'Sem movimentação suficiente no período.' }) {
    const hasAny = entries.some((e) => e.primaryValueCents > 0 || e.secondaryValueCents > 0);
    if (!entries.length || !hasAny) {
      return `<p class="nx-body-md nx-text-secondary">${esc(emptyMessage)}</p>`;
    }
    const maxVal = Math.max(1, ...entries.map((e) => Math.max(e.primaryValueCents, e.secondaryValueCents)));
    const selected = entries.find((e) => e.id === selectedEntryId) || entries[entries.length - 1];

    const svgW = 360;
    const svgH = 116;
    const baseline = svgH - 6;
    const maxBarH = baseline - 8;
    const groupW = svgW / entries.length;
    const barW = Math.min(20, Math.max(8, groupW * 0.28));
    const gap = 4;

    const barsSvg = entries
      .map((entry, idx) => {
        const isSel = selected && selected.id === entry.id;
        const cx = groupW * idx + groupW / 2;
        const pH = entry.primaryValueCents > 0 ? Math.max(4, (entry.primaryValueCents / maxVal) * maxBarH) : 0;
        const sH = entry.secondaryValueCents > 0 ? Math.max(4, (entry.secondaryValueCents / maxVal) * maxBarH) : 0;
        return `
          <g style="cursor:pointer" ${actionAttr} data-month-id="${esc(entry.id)}">
            ${isSel ? `<rect x="${(groupW * idx + 3).toFixed(1)}" y="2" width="${(groupW - 6).toFixed(1)}" height="${(baseline - 2).toFixed(1)}" rx="8" fill="var(--nx-surface-elevated)" opacity="0.9"/>` : ''}
            ${pH > 0 ? `<rect x="${(cx - barW - gap / 2).toFixed(1)}" y="${(baseline - pH).toFixed(1)}" width="${barW.toFixed(1)}" height="${pH.toFixed(1)}" rx="4" fill="var(--nx-accent-neon)"/>` : ''}
            ${sH > 0 ? `<rect x="${(cx + gap / 2).toFixed(1)}" y="${(baseline - sH).toFixed(1)}" width="${barW.toFixed(1)}" height="${sH.toFixed(1)}" rx="4" fill="var(--nx-chart-secondary)"/>` : ''}
          </g>
        `;
      })
      .join('');

    const resultCents = selected.primaryValueCents - selected.secondaryValueCents;

    return `
      <div data-nexus-component="BarGroupChart" data-nexus-source="charts/BarGroupChart.kt" style="display:flex;flex-direction:column;gap:8px">
        <div style="display:flex;gap:14px;align-items:center">
          <span style="display:inline-flex;align-items:center;gap:6px"><span class="nx-dot" style="background:var(--nx-accent-neon);width:8px;height:8px"></span><span class="nx-label-sm nx-text-secondary">${esc(primaryLabel)}</span></span>
          <span style="display:inline-flex;align-items:center;gap:6px"><span class="nx-dot" style="background:var(--nx-chart-secondary);width:8px;height:8px"></span><span class="nx-label-sm nx-text-secondary">${esc(secondaryLabel)}</span></span>
        </div>
        <svg viewBox="0 0 ${svgW} ${svgH}" width="100%" height="116" preserveAspectRatio="none" style="display:block">
          <line x1="0" y1="${(baseline - maxBarH / 2).toFixed(1)}" x2="${svgW}" y2="${(baseline - maxBarH / 2).toFixed(1)}" stroke="var(--nx-border)" stroke-opacity="0.55" stroke-width="1"/>
          <line x1="0" y1="${baseline}" x2="${svgW}" y2="${baseline}" stroke="var(--nx-border)" stroke-width="2"/>
          ${barsSvg}
        </svg>
        <div class="nx-bar-months">
          ${entries.map((e) => {
            const isSel = selected && selected.id === e.id;
            return `<button type="button" class="nx-bar-month-btn tnum ${isSel ? 'is-selected' : ''}" ${actionAttr} data-month-id="${esc(e.id)}">${esc(e.label)}</button>`;
          }).join('')}
        </div>
        <div class="nx-bar-summary tnum">
          <span style="font-weight:600">${esc(selected.label.toUpperCase())}</span>
          <span class="nx-positive">+${esc(formatBrl(selected.primaryValueCents))}</span>
          <span class="nx-text-primary">-${esc(formatBrl(selected.secondaryValueCents))}</span>
          <span style="font-weight:600;color:${resultCents >= 0 ? 'var(--nx-positive)' : 'var(--nx-danger)'}">Saldo: ${esc(formatBrl(resultCents))}</span>
        </div>
      </div>
    `;
  }

  function renderDonutChart({ slices, totalCents, centerLabel, selectedSliceId, sliceAction = 'select-donut-slice', emptyMessage = 'Sem despesas neste mês.' }) {
    if (!slices.length || totalCents <= 0) {
      return `<p class="nx-body-md nx-text-secondary">${esc(emptyMessage)}</p>`;
    }

    const gapDeg = slices.length > 1 ? 2.6 : 0;
    const avail = 360 - gapDeg * slices.length;
    let cursor = 0;

    const selected = slices.find((s) => s.id === selectedSliceId) || null;
    const pathsSvg = slices
      .map((slice) => {
        const isSel = selected && selected.id === slice.id;
        const isMuted = selected && !isSel;
        const sweep = Math.max(2, (slice.valueCents / totalCents) * avail);
        const d = describeArc(82, 82, 68, cursor, cursor + sweep);
        cursor += sweep + gapDeg;
        const sw = isSel ? 24 : 20;
        const cap = slices.length === 1 ? 'butt' : 'round';
        return `<path d="${d}" fill="none" stroke="${esc(slice.color)}" stroke-width="${sw}" stroke-linecap="${cap}" stroke-opacity="${isMuted ? '0.42' : '1'}" style="cursor:pointer" data-action="${sliceAction}" data-slice-id="${esc(slice.id)}"/>`;
      })
      .join('');

    return `
      <div data-nexus-component="DonutChart" data-nexus-source="charts/DonutChart.kt" style="display:flex;flex-direction:column;gap:12px">
        <div class="nx-donut-box">
          <svg width="164" height="164" viewBox="0 0 164 164">
            <circle cx="82" cy="82" r="68" fill="none" stroke="var(--nx-chart-track)" stroke-width="20"/>
            ${pathsSvg}
          </svg>
          <div class="nx-ring-center" style="padding:0 18px;text-align:center">
            <span class="nx-label-sm nx-text-secondary" style="max-width:96px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap">${esc(selected ? selected.label : centerLabel)}</span>
            <span class="nx-title-md tnum" style="font-weight:600">${esc(formatBrl(selected ? selected.valueCents : totalCents))}</span>
          </div>
        </div>
        <div style="display:flex;flex-direction:column;gap:4px">
          ${slices.map((slice) => {
            const isSel = selected && selected.id === slice.id;
            const isMuted = selected && !isSel;
            return `
              <button type="button" class="nx-legend-btn ${isSel ? 'is-selected' : ''}" style="opacity:${isMuted ? '0.68' : '1'}" data-action="${sliceAction}" data-slice-id="${esc(slice.id)}">
                <span style="display:inline-flex;align-items:center;gap:8px;min-width:0">
                  <span class="nx-dot" style="background:${esc(slice.color)};width:9px;height:9px"></span>
                  <span class="nx-body-sm" style="color:var(--nx-text-primary);font-weight:${isSel ? '600' : '400'};white-space:nowrap;overflow:hidden;text-overflow:ellipsis">${esc(slice.label)}</span>
                </span>
                <span class="nx-label-md tnum" style="color:${isSel ? 'var(--nx-accent-neon)' : 'var(--nx-text-secondary)'};font-weight:${isSel ? '600' : '500'};white-space:nowrap">
                  ${esc(formatBrl(slice.valueCents))} (${Math.round(slice.percentage)}%)
                </span>
              </button>
            `;
          }).join('')}
        </div>
      </div>
    `;
  }

  function renderProgressBarRow({ label, currentCents, targetCents, usedPercent, exceededCents }) {
    const pct = Math.min(100, Math.max(0, targetCents > 0 ? (currentCents / targetCents) * 100 : 0));
    const fillColor = usedPercent > 100 ? 'var(--nx-danger)' : usedPercent >= 80 ? 'var(--nx-warning)' : 'var(--nx-accent-neon)';
    return `
      <div data-nexus-component="ProgressBarRow" data-nexus-source="charts/ProgressBarRow.kt" style="display:flex;flex-direction:column;gap:5px">
        <div class="nx-row-between">
          <span class="nx-title-sm">${esc(label)}</span>
          <span class="nx-label-md tnum" style="color:${fillColor};font-weight:600">${usedPercent}%</span>
        </div>
        <div class="nx-progress-track">
          <div class="nx-progress-fill" style="width:${pct.toFixed(1)}%;background:${fillColor}"></div>
        </div>
        <div class="nx-row-between">
          <span class="nx-label-sm nx-text-secondary tnum">${esc(formatBrl(currentCents))} de ${esc(formatBrl(targetCents))}</span>
          ${exceededCents > 0 ? `<span class="nx-label-sm nx-danger tnum" style="font-weight:600">excedeu ${esc(formatBrl(exceededCents))}</span>` : ''}
        </div>
      </div>
    `;
  }

  function renderMarketHistoryChartSvg({ asset, series, range, selectedIdx, alertTargets = [] }) {
    if (!series || !series.length) {
      return '<p class="nx-body-sm">Série histórica indisponível.</p>';
    }
    const minVal = Math.min(...series);
    const maxVal = Math.max(...series);
    const span = Math.max(0.0001, maxVal - minVal);
    const first = series[0];
    const last = series[series.length - 1];
    const isPositive = last >= first;
    const strokeColor = isPositive ? 'var(--nx-positive)' : 'var(--nx-danger)';
    const idx = selectedIdx != null && selectedIdx >= 0 && selectedIdx < series.length ? selectedIdx : series.length - 1;
    const selVal = series[idx];

    const w = 360;
    const h = 148;
    const padX = 8;
    const padY = 14;
    const plotW = w - padX * 2;
    const plotH = h - padY * 2;

    const coords = series.map((v, i) => {
      const x = padX + (series.length === 1 ? plotW / 2 : (i / (series.length - 1)) * plotW);
      const y = padY + (1 - (v - minVal) / span) * plotH;
      return { x, y, v, i };
    });

    const linePath = coords.map((c, i) => `${i === 0 ? 'M' : 'L'} ${c.x.toFixed(1)} ${c.y.toFixed(1)}`).join(' ');
    const areaPath = `${linePath} L ${coords[coords.length - 1].x.toFixed(1)} ${(h - padY).toFixed(1)} L ${coords[0].x.toFixed(1)} ${(h - padY).toFixed(1)} Z`;
    const selCoord = coords[idx];

    const alertLinesSvg = alertTargets
      .filter((a) => a.targetPrice >= minVal * 0.96 && a.targetPrice <= maxVal * 1.04)
      .map((a) => {
        const clamped = Math.min(maxVal, Math.max(minVal, a.targetPrice));
        const ay = padY + (1 - (clamped - minVal) / span) * plotH;
        const col = a.status === 'TRIGGERED' ? 'var(--nx-positive)' : 'var(--nx-warning)';
        return `<line x1="${padX}" y1="${ay.toFixed(1)}" x2="${w - padX}" y2="${ay.toFixed(1)}" stroke="${col}" stroke-width="1.3" stroke-dasharray="4 4"/>`;
      })
      .join('');

    const hitTargetsSvg = coords.map((c) => `
      <circle cx="${c.x.toFixed(1)}" cy="${c.y.toFixed(1)}" r="7" fill="transparent" style="cursor:pointer" data-action="select-market-point" data-point-idx="${c.i}"/>
    `).join('');

    const periodPct = first > 0 ? ((last - first) / first) * 100 : 0;

    return `
      <div class="nx-chart-surface" data-nexus-component="MarketHistoryChartCard" data-nexus-source="MarketAlertSheetsAndCards.kt" data-nexus-status="simulado">
        <div class="nx-row-between">
          <div>
            <span class="nx-label-sm nx-text-secondary">Ponto ${idx + 1}/${series.length} (${esc(range)})</span>
            <div class="nx-title-md tnum" style="font-weight:700">${esc(formatMarketValue(selVal, asset.category, asset.quoteSymbol))}</div>
          </div>
          <span class="nx-badge ${periodPct >= 0 ? 'is-positive' : 'is-danger'} tnum">
            ${periodPct >= 0 ? '+' : ''}${periodPct.toFixed(2)}% no período
          </span>
        </div>

        <svg viewBox="0 0 ${w} ${h}" width="100%" height="148" style="display:block;overflow:visible">
          <defs>
            <linearGradient id="nxMktGrad" x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" stop-color="${strokeColor}" stop-opacity="0.28"/>
              <stop offset="100%" stop-color="${strokeColor}" stop-opacity="0.01"/>
            </linearGradient>
          </defs>
          <line x1="${padX}" y1="${padY}" x2="${w - padX}" y2="${padY}" stroke="var(--nx-border)" stroke-dasharray="2 4" stroke-width="1"/>
          <line x1="${padX}" y1="${h - padY}" x2="${w - padX}" y2="${h - padY}" stroke="var(--nx-border)" stroke-dasharray="2 4" stroke-width="1"/>
          ${alertLinesSvg}
          <path d="${areaPath}" fill="url(#nxMktGrad)"/>
          <path d="${linePath}" fill="none" stroke="${strokeColor}" stroke-width="2.2" stroke-linejoin="round" stroke-linecap="round"/>
          <line x1="${selCoord.x.toFixed(1)}" y1="${padY}" x2="${selCoord.x.toFixed(1)}" y2="${h - padY}" stroke="var(--nx-text-secondary)" stroke-dasharray="3 3" stroke-width="1"/>
          <circle cx="${selCoord.x.toFixed(1)}" cy="${selCoord.y.toFixed(1)}" r="4.8" fill="${strokeColor}" stroke="var(--nx-bg)" stroke-width="2"/>
          ${hitTargetsSvg}
        </svg>

        <div class="nx-row-between nx-label-sm nx-text-secondary tnum">
          <span>Mín: ${esc(formatMarketValue(minVal, asset.category, asset.quoteSymbol))}</span>
          <span>Máx: ${esc(formatMarketValue(maxVal, asset.category, asset.quoteSymbol))}</span>
        </div>
      </div>
    `;
  }

  window.NexusPilot.Charts = {
    renderTaskRingChart,
    renderBarGroupChart,
    renderDonutChart,
    renderProgressBarRow,
    renderMarketHistoryChartSvg
  };
})();
