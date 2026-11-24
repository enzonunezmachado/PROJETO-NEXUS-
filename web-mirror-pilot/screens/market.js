/*
 * NEXUS Web Mirror Pilot — MarketScreen & MarketAssetDetailScreen
 * Fonte Android: MarketScreen.kt, MarketPresentation.kt, MarketAlertSheetsAndCards.kt
 * Nota de fidelidade: Cotações, séries históricas e verificação de alertas são simuladas localmente (sem rede HTTP).
 */
(function () {
  const {
    MARKET_ASSET_CATALOG,
    LABELS,
    formatMarketValue,
    getAssetSeries
  } = window.NexusPilot.Data;
  const {
    esc,
    ICONS,
    renderSectionHeader
  } = window.NexusPilot.Primitives;
  const { renderMarketHistoryChartSvg } = window.NexusPilot.Charts;

  const MARKET_CATEGORIES = [
    { key: 'ALL', label: 'Todos' },
    { key: 'CURRENCY', label: 'Moedas' },
    { key: 'CRYPTO', label: 'Cripto' },
    { key: 'INDEX', label: 'Índices' },
    { key: 'STOCK', label: 'Ações' }
  ];

  const HISTORY_RANGES = ['1D', '5D', '1M', '6M', '1A'];

  function formatApproxBrl(priceUsd, state) {
    const usdQuote = state.marketQuotes['USD-BRL'];
    if (!usdQuote || !usdQuote.price) return null;
    const brl = priceUsd * usdQuote.price;
    return brl.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL', minimumFractionDigits: 2, maximumFractionDigits: 2 });
  }

  function renderAssetRowCard(asset, state) {
    const q = state.marketQuotes[asset.code];
    const isFav = state.marketFavorites.includes(asset.code);
    const activeAlerts = state.marketAlerts.filter((a) => a.assetCode === asset.code && a.isActive).length;
    const pct = q ? q.changePercent : 0;
    const pctTone = pct > 0 ? 'is-positive' : pct < 0 ? 'is-danger' : 'is-neutral';

    return `
      <div class="nx-card is-elevated" data-nexus-component="MarketAssetCard" data-nexus-source="MarketScreen.kt" data-nexus-status="simulado" data-nexus-entity="asset:${esc(asset.code)}">
        <div class="nx-row-between" style="align-items:flex-start">
          <div style="flex:1;min-width:0;cursor:pointer" data-action="open-market-asset" data-asset-code="${esc(asset.code)}">
            <div style="display:flex;align-items:center;gap:6px;flex-wrap:wrap">
              <span class="nx-title-md">${esc(asset.displayName)}</span>
              <span class="nx-badge is-neutral">${esc(asset.ticker)}</span>
            </div>
            <div class="nx-label-sm nx-text-secondary" style="margin-top:2px">
              ${esc(asset.exchange)} · ${esc(q ? q.modality : LABELS.AssetCategory[asset.category])}${q && q.isDelayed ? ' · Atraso ~15m' : ''}
            </div>
          </div>
          <button type="button" class="nx-star-btn ${isFav ? 'is-fav' : ''}" data-action="toggle-market-fav" data-asset-code="${esc(asset.code)}" aria-label="${isFav ? 'Remover dos favoritos' : 'Favoritar ativo'}">
            ${isFav ? ICONS.starFilled : ICONS.star}
          </button>
        </div>

        <div class="nx-row-between" style="cursor:pointer;margin-top:2px" data-action="open-market-asset" data-asset-code="${esc(asset.code)}">
          <div>
            <div class="nx-title-lg tnum" style="font-weight:700">${esc(formatMarketValue(q ? q.price : null, asset.category, asset.quoteSymbol))}</div>
            ${asset.quoteSymbol === 'USD' && q ? `<div class="nx-label-sm nx-text-secondary tnum">≈ ${esc(formatApproxBrl(q.price, state))}</div>` : ''}
          </div>
          <div style="display:flex;flex-direction:column;align-items:flex-end;gap:4px">
            <span class="nx-badge ${pctTone} tnum">${pct >= 0 ? '+' : ''}${pct.toFixed(2)}%</span>
            ${activeAlerts > 0 ? `<span class="nx-label-sm nx-accent tnum">${activeAlerts} alerta(s)</span>` : ''}
          </div>
        </div>
      </div>
    `;
  }

  function renderMarketAlertRow(alert, state) {
    const asset = MARKET_ASSET_CATALOG.find((a) => a.code === alert.assetCode);
    const statusLabel = LABELS.MarketAlertStatus[alert.status] || alert.status;
    const statusTone = alert.status === 'TRIGGERED' ? 'is-positive' : alert.status === 'WAITING' && alert.isActive ? 'is-warning' : 'is-neutral';

    return `
      <div class="nx-card is-elevated" data-nexus-component="MarketPriceAlertCard" data-nexus-source="MarketAlertSheetsAndCards.kt" data-nexus-status="simulado" data-nexus-entity="market-alert:${esc(alert.id)}">
        <div class="nx-row-between">
          <div>
            <div class="nx-title-sm">${esc(asset ? asset.displayName : alert.assetCode)} (${esc(asset ? asset.ticker : alert.assetCode)})</div>
            <div class="nx-label-md nx-text-secondary tnum">
              ${esc(LABELS.MarketAlertCondition[alert.condition])} ${esc(asset ? formatMarketValue(alert.targetPrice, asset.category, asset.quoteSymbol) : alert.targetPrice)}
            </div>
          </div>
          <span class="nx-badge ${statusTone}">${esc(statusLabel)}</span>
        </div>
        ${alert.triggeredAt ? `
          <div class="nx-label-sm nx-positive tnum">Disparado em ${esc(alert.triggeredAt)}</div>
        ` : ''}
        <div class="nx-row-between" style="margin-top:2px">
          <div style="display:flex;gap:6px">
            ${alert.status === 'TRIGGERED' ? `<button type="button" class="nx-btn-text" data-action="reactivate-market-alert" data-alert-id="${esc(alert.id)}">Reativar</button>` : ''}
            <button type="button" class="nx-btn-text" data-action="edit-market-alert" data-alert-id="${esc(alert.id)}">Editar</button>
            <button type="button" class="nx-btn-text nx-btn-danger-text" data-action="delete-market-alert" data-alert-id="${esc(alert.id)}">Excluir</button>
          </div>
          <button type="button" class="nx-switch ${alert.isActive ? 'is-on' : ''}" data-action="toggle-market-alert" data-alert-id="${esc(alert.id)}" aria-label="Ativar ou pausar alerta"></button>
        </div>
      </div>
    `;
  }

  function renderMarketScreen(state, ui) {
    const catFilter = ui.marketCategoryFilter || 'ALL';
    const favAssets = MARKET_ASSET_CATALOG.filter((a) => state.marketFavorites.includes(a.code));
    const filteredAssets = MARKET_ASSET_CATALOG.filter((a) => catFilter === 'ALL' || a.category === catFilter);
    const activeAlertsCount = state.marketAlerts.filter((a) => a.isActive).length;

    const groups = [
      { key: 'CURRENCY', title: 'Moedas' },
      { key: 'CRYPTO', title: 'Criptomoedas' },
      { key: 'INDEX', title: 'Índices' },
      { key: 'STOCK', title: 'Ações' }
    ].filter((g) => filteredAssets.some((a) => a.category === g.key));

    return `
      <div class="nx-screen-scroll no-bottom-nav" data-nexus-component="MarketScreen" data-nexus-source="MarketScreen.kt" data-nexus-status="simulado">
        <div class="nx-row-between">
          <div>
            <div style="display:flex;align-items:center;gap:8px">
              <h2 class="nx-headline-md">Mercado</h2>
              <span class="pilot-trace-tag simulado">Simulado offline</span>
            </div>
            <span class="nx-label-sm nx-text-secondary tnum">12 ativos · No Android usa AwesomeAPI, CoinGecko e Yahoo/B3</span>
          </div>
          <button type="button" class="nx-btn-outlined" data-action="refresh-market-sim">
            ${ICONS.refresh} <span>Atualizar</span>
          </button>
        </div>

        <div class="nx-card" data-nexus-component="MarketOverviewSummaryCard" data-nexus-source="MarketScreen.kt" data-nexus-status="simulado">
          <div class="nx-row-2col tnum">
            <div>
              <div class="nx-label-sm nx-text-secondary">Ativos monitorados</div>
              <div class="nx-title-lg">${MARKET_ASSET_CATALOG.length}/12 carregados</div>
            </div>
            <div>
              <div class="nx-label-sm nx-text-secondary">Favoritos & Alertas</div>
              <div class="nx-title-lg">${favAssets.length} fav · ${activeAlertsCount} ativo(s)</div>
            </div>
          </div>
          <div class="nx-label-sm nx-text-secondary tnum">Snapshot simulado: ${esc(state.marketQuotes['USD-BRL']?.fetchedAt || '23/11/2026 14:35')}</div>
        </div>

        <div class="nx-chip-row">
          ${MARKET_CATEGORIES.map((c) => `
            <button type="button" class="nx-chip ${catFilter === c.key ? 'is-active' : ''}" data-action="set-market-category" data-category="${c.key}">
              ${esc(c.label)}
            </button>
          `).join('')}
        </div>

        ${favAssets.length ? `
          <div style="display:flex;flex-direction:column;gap:8px">
            ${renderSectionHeader({ title: 'Minha Watchlist', trailing: `${favAssets.length} favorito(s)` })}
            <div class="nx-grid-2">
              ${favAssets.map((a) => renderAssetRowCard(a, state)).join('')}
            </div>
          </div>
        ` : ''}

        <div style="display:flex;flex-direction:column;gap:8px">
          ${renderSectionHeader({
            title: 'Alertas de preço',
            trailing: `${state.marketAlerts.length}`,
            actionLabel: '+ Novo alerta',
            actionAttr: 'data-action="new-market-alert"'
          })}
          ${state.marketAlerts.length ? `
            <div class="nx-grid-2">
              ${state.marketAlerts.map((a) => renderMarketAlertRow(a, state)).join('')}
            </div>
          ` : `
            <div class="nx-card" style="padding:14px">
              <span class="nx-body-sm">Nenhum alerta configurado. Toque em "+ Novo alerta" para monitorar preço alvo.</span>
            </div>
          `}
        </div>

        ${groups.map((g) => {
          const items = filteredAssets.filter((a) => a.category === g.key);
          return `
            <div style="display:flex;flex-direction:column;gap:8px">
              ${renderSectionHeader({ title: g.title, trailing: String(items.length) })}
              <div class="nx-grid-2">
                ${items.map((a) => renderAssetRowCard(a, state)).join('')}
              </div>
            </div>
          `;
        }).join('')}
      </div>
    `;
  }

  function renderMarketAssetDetailScreen(state, ui) {
    const code = ui.selectedMarketAssetCode || 'USD-BRL';
    const asset = MARKET_ASSET_CATALOG.find((a) => a.code === code) || MARKET_ASSET_CATALOG[0];
    const q = state.marketQuotes[asset.code];
    const range = ui.marketHistoryRange || '1M';
    const series = getAssetSeries(state, asset.code, range);
    const selectedIdx = ui.marketSelectedPointIdx != null ? ui.marketSelectedPointIdx : series.length - 1;
    const isFav = state.marketFavorites.includes(asset.code);
    const assetAlerts = state.marketAlerts.filter((a) => a.assetCode === asset.code);
    const isTwoPane = ui.viewport === 'tablet-landscape' || ui.viewport === 'fluid';
    const pct = q ? q.changePercent : 0;

    const heroAndAlerts = `
      <div class="nx-card" data-nexus-component="MarketHeroPriceCard" data-nexus-source="MarketScreen.kt:MarketAssetDetailScreen" data-nexus-status="simulado">
        <div class="nx-row-between">
          <div>
            <div style="display:flex;align-items:center;gap:6px">
              <h2 class="nx-title-lg">${esc(asset.displayName)}</h2>
              <span class="nx-badge is-neutral">${esc(asset.ticker)}</span>
            </div>
            <div class="nx-label-md nx-text-secondary">${esc(asset.exchange)} · ${esc(q ? q.modality : LABELS.AssetCategory[asset.category])}</div>
          </div>
          <button type="button" class="nx-star-btn ${isFav ? 'is-fav' : ''}" data-action="toggle-market-fav" data-asset-code="${esc(asset.code)}">
            ${isFav ? ICONS.starFilled : ICONS.star}
          </button>
        </div>

        <div style="display:flex;align-items:baseline;justify-content:space-between;gap:10px;margin-top:4px">
          <div>
            <div class="nx-headline-md tnum">${esc(formatMarketValue(q ? q.price : null, asset.category, asset.quoteSymbol))}</div>
            ${asset.quoteSymbol === 'USD' && q ? `<div class="nx-label-md nx-text-secondary tnum">Conversão aprox.: ${esc(formatApproxBrl(q.price, state))}</div>` : ''}
          </div>
          <span class="nx-badge ${pct >= 0 ? 'is-positive' : 'is-danger'} tnum">${pct >= 0 ? '+' : ''}${pct.toFixed(2)}% (${esc(q?.variationPeriodLabel || '24h')})</span>
        </div>

        <hr class="nx-divider"/>
        <div style="display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:8px" class="tnum">
          <div>
            <div class="nx-label-sm nx-text-secondary">Compra</div>
            <div class="nx-title-sm">${esc(q?.bidPrice != null ? formatMarketValue(q.bidPrice, asset.category, asset.quoteSymbol) : '--')}</div>
          </div>
          <div>
            <div class="nx-label-sm nx-text-secondary">Venda</div>
            <div class="nx-title-sm">${esc(q?.askPrice != null ? formatMarketValue(q.askPrice, asset.category, asset.quoteSymbol) : '--')}</div>
          </div>
          <div>
            <div class="nx-label-sm nx-text-secondary">Fech. anterior</div>
            <div class="nx-title-sm">${esc(q?.previousClose != null ? formatMarketValue(q.previousClose, asset.category, asset.quoteSymbol) : '--')}</div>
          </div>
        </div>
      </div>

      <div style="display:flex;flex-direction:column;gap:8px">
        ${renderSectionHeader({
          title: `Alertas de ${asset.ticker}`,
          trailing: String(assetAlerts.length),
          actionLabel: '+ Novo alerta',
          actionAttr: `data-action="new-market-alert" data-asset-code="${esc(asset.code)}"`
        })}
        ${assetAlerts.length ? assetAlerts.map((a) => renderMarketAlertRow(a, state)).join('') : `
          <div class="nx-card is-elevated">
            <span class="nx-body-sm">Nenhum alerta de preço configurado para ${esc(asset.displayName)}.</span>
          </div>
        `}
      </div>
    `;

    const chartAndMeta = `
      <div class="nx-card">
        <div class="nx-row-between">
          <span class="nx-title-md">Histórico simulado</span>
          <div class="nx-seg-control">
            ${HISTORY_RANGES.map((r) => `
              <button type="button" class="nx-seg-item ${range === r ? 'is-active' : ''}" data-action="set-market-range" data-range="${r}">${r}</button>
            `).join('')}
          </div>
        </div>
        ${renderMarketHistoryChartSvg({
          asset,
          series,
          range,
          selectedIdx,
          alertTargets: assetAlerts.filter((a) => a.isActive)
        })}
      </div>

      <div class="nx-card is-elevated" data-nexus-component="MarketSourceMetadataCard" data-nexus-source="MarketScreen.kt" data-nexus-status="simulado">
        <div class="nx-row-between">
          <span class="nx-title-sm">Transparência e fonte</span>
          <span class="pilot-trace-tag simulado">Simulado</span>
        </div>
        <div class="nx-label-md nx-text-secondary tnum">Fonte: ${esc(q?.sourceName || 'Simulado local')}</div>
        <div class="nx-label-md nx-text-secondary tnum">Referência da fonte: ${esc(q?.sourceTimestamp || '--')} · Cache: ${esc(q?.fetchedAt || '--')}</div>
        ${q?.delayNotice ? `<div class="nx-label-sm nx-warning">${esc(q.delayNotice)}</div>` : ''}
      </div>
    `;

    if (isTwoPane) {
      return `
        <div class="nx-two-pane-market" style="padding:12px 20px 16px" data-nexus-component="MarketAssetDetailScreen" data-nexus-source="MarketScreen.kt:MarketAssetDetailScreen" data-nexus-status="simulado">
          <div class="nx-pane-scroll">${heroAndAlerts}</div>
          <div class="nx-pane-scroll">${chartAndMeta}</div>
        </div>
      `;
    }

    return `
      <div class="nx-screen-scroll no-bottom-nav" data-nexus-component="MarketAssetDetailScreen" data-nexus-source="MarketScreen.kt:MarketAssetDetailScreen" data-nexus-status="simulado">
        ${heroAndAlerts}
        ${chartAndMeta}
      </div>
    `;
  }

  window.NexusPilot = window.NexusPilot || {};
  window.NexusPilot.Screens = window.NexusPilot.Screens || {};
  window.NexusPilot.Screens.Market = {
    renderMarketScreen,
    renderMarketAssetDetailScreen
  };
})();
