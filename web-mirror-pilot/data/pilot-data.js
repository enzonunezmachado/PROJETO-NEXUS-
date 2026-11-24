/*
 * NEXUS Web Mirror Pilot — Relational Store & Domain Engines
 * Perfil fictício coerente: Gabriel (data de referência: 2026-11-23, mês 2026-11).
 * Espelha entidades de NexusEntities.kt, NexusEnums.kt, MarketQuoteModels.kt,
 * AgendaEngine.kt, FinanceRules.kt e GoalProgressRules.kt.
 */
(function () {
  const DEFAULT_BLOCKS = [
    { type: 'GREETING', title: 'Saudação', enabled: true, order: 0, showPrimaryDetail: true, showSecondaryDetail: true, showOverdueOrAlerts: true, maxItems: 3 },
    { type: 'DAILY_QUOTE', title: 'Frase diária', enabled: true, order: 1, showPrimaryDetail: true, showSecondaryDetail: true, showOverdueOrAlerts: true, maxItems: 1 },
    { type: 'TASK_SUMMARY', title: 'Resumo de tarefas', enabled: true, order: 2, showPrimaryDetail: true, showSecondaryDetail: true, showOverdueOrAlerts: true, maxItems: 4 },
    { type: 'TODAY_AGENDA', title: 'Agenda do dia', enabled: true, order: 3, showPrimaryDetail: true, showSecondaryDetail: true, showOverdueOrAlerts: true, maxItems: 3 },
    { type: 'FINANCE_SUMMARY', title: 'Resumo financeiro', enabled: true, order: 4, showPrimaryDetail: true, showSecondaryDetail: true, showOverdueOrAlerts: true, maxItems: 3 },
    { type: 'CHARTS_INDICATORS', title: 'Gráficos e indicadores', enabled: true, order: 5, showPrimaryDetail: true, showSecondaryDetail: true, showOverdueOrAlerts: true, maxItems: 6 },
    { type: 'UPCOMING_EVENTS', title: 'Próximos eventos', enabled: true, order: 6, showPrimaryDetail: true, showSecondaryDetail: true, showOverdueOrAlerts: true, maxItems: 3 },
    { type: 'UPCOMING_PAYMENTS', title: 'Próximos pagamentos', enabled: true, order: 7, showPrimaryDetail: true, showSecondaryDetail: true, showOverdueOrAlerts: true, maxItems: 4 },
    { type: 'GOALS', title: 'Metas', enabled: true, order: 8, showPrimaryDetail: true, showSecondaryDetail: true, showOverdueOrAlerts: true, maxItems: 3 }
  ];

  // Catálogo exato dos 12 ativos de MarketQuoteModels.kt (MarketAssetCatalog.SUPPORTED_ASSETS)
  const MARKET_ASSET_CATALOG = [
    { code: 'USD-BRL', baseSymbol: 'USD', quoteSymbol: 'BRL', displayName: 'Dólar (USD/BRL)', category: 'CURRENCY', ticker: 'USD/BRL', exchange: 'Câmbio', isPointsUnit: false },
    { code: 'EUR-BRL', baseSymbol: 'EUR', quoteSymbol: 'BRL', displayName: 'Euro (EUR/BRL)', category: 'CURRENCY', ticker: 'EUR/BRL', exchange: 'Câmbio', isPointsUnit: false },
    { code: 'BTC-BRL', baseSymbol: 'BTC', quoteSymbol: 'BRL', displayName: 'Bitcoin', category: 'CRYPTO', ticker: 'BTC', exchange: 'Spot', isPointsUnit: false },
    { code: 'ETH-BRL', baseSymbol: 'ETH', quoteSymbol: 'BRL', displayName: 'Ethereum', category: 'CRYPTO', ticker: 'ETH', exchange: 'Spot', isPointsUnit: false },
    { code: 'SOL-BRL', baseSymbol: 'SOL', quoteSymbol: 'BRL', displayName: 'Solana', category: 'CRYPTO', ticker: 'SOL', exchange: 'Spot', isPointsUnit: false },
    { code: 'IBOV', baseSymbol: 'IBOV', quoteSymbol: 'BRL', displayName: 'Ibovespa', category: 'INDEX', ticker: 'IBOV', exchange: 'B3', isPointsUnit: true },
    { code: 'SPX', baseSymbol: 'SPX', quoteSymbol: 'USD', displayName: 'S&P 500', category: 'INDEX', ticker: 'SPX', exchange: 'NYSE / S&P', isPointsUnit: true },
    { code: 'IXIC', baseSymbol: 'IXIC', quoteSymbol: 'USD', displayName: 'Nasdaq Composite', category: 'INDEX', ticker: 'IXIC', exchange: 'Nasdaq', isPointsUnit: true },
    { code: 'PETR4', baseSymbol: 'PETR4', quoteSymbol: 'BRL', displayName: 'Petrobras', category: 'STOCK', ticker: 'PETR4', exchange: 'B3', isPointsUnit: false },
    { code: 'VALE3', baseSymbol: 'VALE3', quoteSymbol: 'BRL', displayName: 'Vale', category: 'STOCK', ticker: 'VALE3', exchange: 'B3', isPointsUnit: false },
    { code: 'ITUB4', baseSymbol: 'ITUB4', quoteSymbol: 'BRL', displayName: 'Itaú Unibanco', category: 'STOCK', ticker: 'ITUB4', exchange: 'B3', isPointsUnit: false },
    { code: 'MELI', baseSymbol: 'MELI', quoteSymbol: 'USD', displayName: 'Mercado Livre', category: 'STOCK', ticker: 'MELI', exchange: 'Nasdaq', isPointsUnit: false }
  ];

  const LABELS = {
    TransactionType: { INCOME: 'Receita', EXPENSE: 'Despesa' },
    FinancialStatus: { PLANNED: 'Previsto', PENDING: 'Pendente', SETTLED: 'Realizado', OVERDUE: 'Atrasado', CANCELLED: 'Cancelado' },
    PaymentMethod: { PIX: 'Pix', DEBIT: 'Débito', CREDIT_CARD: 'Cartão de crédito', CASH: 'Dinheiro', BANK_TRANSFER: 'Transferência', BOLETO: 'Boleto', OTHER: 'Outro' },
    StatementStatus: { OPEN: 'Aberta', CLOSED: 'Fechada', PENDING: 'Pendente', PAID: 'Paga', OVERDUE: 'Atrasada' },
    TicketStatus: { FREE: 'Gratuito', NOT_BOUGHT: 'Não comprado', PENDING: 'Pendente', PARTIALLY_PAID: 'Parcialmente pago', BOUGHT: 'Comprado', PAID: 'Pago', CANCELLED: 'Cancelado' },
    EventLifecycleStatus: { PLANNED: 'Planejado', CONFIRMED: 'Confirmado', ATTENDED: 'Realizado', CANCELLED: 'Cancelado' },
    EventCostType: { TICKET: 'Ingresso', TRANSPORT: 'Transporte', FOOD: 'Alimentação', LODGING: 'Hospedagem', PARKING: 'Estacionamento', SHOPPING: 'Compras', OTHER: 'Outros' },
    TaskStatus: { NOT_STARTED: 'Não iniciada', IN_PROGRESS: 'Em andamento', WAITING: 'Aguardando', BLOCKED: 'Bloqueada', COMPLETED: 'Concluída', CANCELLED: 'Cancelada' },
    TaskPriority: { LOW: 'Baixa', NORMAL: 'Normal', HIGH: 'Alta', URGENT: 'Urgente' },
    ProjectStatus: { PLANNED: 'Planejado', ACTIVE: 'Em andamento', ON_HOLD: 'Pausado', COMPLETED: 'Concluído', ARCHIVED: 'Arquivado' },
    GoalStatus: { ACTIVE: 'Em andamento', PAUSED: 'Pausado', COMPLETED: 'Concluído', CANCELLED: 'Cancelado' },
    GoalProgressMode: { AUTO: 'Automático (marcos, tarefas e projetos)', MANUAL: 'Manual (%)' },
    RecurrenceFrequency: { DAILY: 'Diária', WEEKLY: 'Semanal', BIWEEKLY: 'Quinzenal', MONTHLY: 'Mensal', BIMONTHLY: 'Bimestral', QUARTERLY: 'Trimestral', SEMIANNUAL: 'Semestral', ANNUAL: 'Anual', CUSTOM_DAYS: 'Personalizada (dias)' },
    TaskRecurrenceFrequency: { DAILY: 'Diariamente', SPECIFIC_WEEKDAYS: 'Dias específicos da semana', WEEKLY: 'Semanalmente', BIWEEKLY: 'Quinzenalmente', MONTHLY: 'Mensalmente', CUSTOM_DAYS: 'Intervalo personalizado' },
    CategoryScope: { GENERAL: 'Geral', APPOINTMENT: 'Agenda', TASK: 'Tarefa', FINANCE: 'Financeiro', EVENT: 'Evento' },
    ReminderStatus: { ACTIVE: 'Agendado', FIRED: 'Disparado', DISMISSED: 'Dispensado', CANCELLED: 'Cancelado' },
    AssetCategory: { CURRENCY: 'Moeda', CRYPTO: 'Criptomoeda', INDEX: 'Índice', STOCK: 'Ação' },
    MarketAlertCondition: { PRICE_ABOVE: 'Acima ou igual a', PRICE_BELOW: 'Abaixo ou igual a' },
    MarketAlertStatus: { WAITING: 'Aguardando', TRIGGERED: 'Disparado', PAUSED: 'Pausado' },
    NexusDateFormat: {
      WEEKDAY_DD_MM_YYYY: 'Dia da semana, DD/MM/AAAA',
      DD_MM_YYYY: 'DD/MM/AAAA',
      DD_MMM_YYYY: 'DD de mês de AAAA',
      DD_MM_WEEKDAY: 'DD/MM · Dia da semana'
    }
  };

  function generateMarketSeries(basePrice, pointsCount, volatilityPct, seedOffset) {
    const pts = [];
    let p = basePrice * (1 - volatilityPct * 0.45);
    for (let i = 0; i < pointsCount; i++) {
      const wave = Math.sin((i + seedOffset) * 0.65) * volatilityPct + Math.cos((i + seedOffset) * 0.27) * (volatilityPct * 0.4);
      p = i === pointsCount - 1 ? basePrice : Math.max(basePrice * 0.72, basePrice * (1 + wave));
      pts.push(Number(p.toFixed(basePrice < 20 ? 4 : 2)));
    }
    return pts;
  }

  function createInitialState() {
    return {
      todayIso: '2026-11-23',
      selectedMonthIso: '2026-11',
      userName: 'Gabriel',
      settings: {
        darkTheme: true,
        dateFormatPattern: 'WEEKDAY_DD_MM_YYYY'
      },
      personalization: {
        greetingEnabled: true,
        greetingTemplate: 'AUTO',
        showDailyQuoteOnDashboard: true,
        compactDashboardMode: false,
        showMetricSummaryOnDashboard: true
      },
      quotes: [
        { id: 'qt_1', text: 'Clareza precede execução: organize o essencial antes de acelerar.', author: 'Caderno NEXUS', isActive: true },
        { id: 'qt_2', text: 'Sistemas consistentes superam picos isolados de motivação.', author: 'Notas de Arquitetura', isActive: true },
        { id: 'qt_3', text: 'Cada compromisso no papel libera a mente para pensar com profundidade.', author: '', isActive: true },
        { id: 'qt_4', text: 'Pequenos ajustes diários evitam grandes correções de rota.', author: 'Gabriel', isActive: false }
      ],
      dailyQuote: {
        id: 'qt_1',
        text: 'Clareza precede execução: organize o essencial antes de acelerar.',
        author: 'Caderno NEXUS'
      },
      blocks: DEFAULT_BLOCKS.map((b) => ({ ...b })),
      declaredBalanceCents: 482500, // R$ 4.825,00
      briefingState: {
        morningReviewedToday: false,
        nightReviewedTonight: false,
        history: [
          {
            id: 'bh_1',
            briefingType: 'Revisão Noturna',
            dateIso: '2026-11-22',
            summaryText: '3 tarefas concluídas · 1 conta agendada para hoje · foco em preparativos da Conferência.',
            reviewedAt: '22/11/2026 21:40'
          },
          {
            id: 'bh_2',
            briefingType: 'Briefing Diário',
            dateIso: '2026-11-22',
            summaryText: '2 compromissos · 4 tarefas · R$ 310,00 em contas do dia.',
            reviewedAt: '22/11/2026 08:12'
          }
        ]
      },
      routineSettings: {
        notificationsEnabled: true,
        scheduleChannelEnabled: true,
        financeChannelEnabled: true,
        eventsChannelEnabled: true,
        routineChannelEnabled: true,
        morningBriefingEnabled: true,
        morningBriefingTime: '08:00',
        nightReviewEnabled: true,
        nightReviewTime: '21:30',
        quietHoursEnabled: true,
        quietHoursStart: '22:30',
        quietHoursEnd: '07:00',
        defaultTaskReminderMinutes: 30,
        defaultAppointmentReminderMinutes: 30,
        defaultEventReminderMinutes: 60,
        defaultFinanceReminderMinutes: 15
      },
      categories: [
        { id: 'cat_moradia', name: 'Moradia', colorHex: '#5CC8FF', scope: 'FINANCE' },
        { id: 'cat_eventos', name: 'Eventos & Estudos', colorHex: '#7CFFB2', scope: 'FINANCE' },
        { id: 'cat_alim', name: 'Alimentação', colorHex: '#FFD166', scope: 'FINANCE' },
        { id: 'cat_transp', name: 'Transporte', colorHex: '#C792FF', scope: 'FINANCE' },
        { id: 'cat_saude', name: 'Saúde', colorHex: '#FF7A90', scope: 'FINANCE' },
        { id: 'cat_trabalho', name: 'Trabalho', colorHex: '#4DE1C1', scope: 'GENERAL' },
        { id: 'cat_equip', name: 'Equipamentos', colorHex: '#FF9F6B', scope: 'FINANCE' }
      ],
      personalGoals: [
        {
          id: 'pg_1',
          title: 'Consolidar UI Multi-dispositivo NEXUS',
          description: 'Padronizar responsividade em celular, tablet retrato e paisagem no ecossistema pessoal.',
          categoryId: 'cat_trabalho',
          targetDateIso: '2026-12-20',
          status: 'ACTIVE',
          progressMode: 'AUTO',
          manualProgressPercent: 72,
          notes: 'Manter contraste Grafite Suave no tema claro.',
          milestones: [
            { id: 'ms_1', title: 'Especificar breakpoints 720dp e 840dp', description: 'Validar duas colunas e painel duplo', dueDateIso: '2026-11-15', isCompleted: true },
            { id: 'ms_2', title: 'Unificar tokens de borda ColoredBorderRole', description: 'Aplicar larguras e alfas proporcionais', dueDateIso: '2026-11-22', isCompleted: true },
            { id: 'ms_3', title: 'Revisar fluxos financeiros completos', description: 'Cartões, parcelamentos, empréstimos e auditoria', dueDateIso: '2026-11-30', isCompleted: false },
            { id: 'ms_4', title: 'Validar exportação e backup JSON v7', description: 'Restauração atômica em testes', dueDateIso: '2026-12-15', isCompleted: false }
          ],
          progressLogs: [
            { id: 'pl_1', dateIso: '2026-11-18', percent: 50, note: 'Concluída etapa de grade adaptativa e tokens iniciais.' },
            { id: 'pl_2', dateIso: '2026-11-22', percent: 68, note: 'Faltam telas secundárias e revisão final.' }
          ]
        },
        {
          id: 'pg_2',
          title: 'Autonomia financeira e reserva de 6 meses',
          description: 'Acompanhar aportes mensais e eliminar parcelamentos longos até março.',
          categoryId: 'cat_moradia',
          targetDateIso: '2027-03-31',
          status: 'ACTIVE',
          progressMode: 'MANUAL',
          manualProgressPercent: 82,
          notes: 'Vinculada à meta Reserva de emergência (6 meses).',
          milestones: [
            { id: 'ms_5', title: 'Atingir R$ 24.500 em reserva líquida', description: 'Meta intermediária de novembro', dueDateIso: '2026-11-25', isCompleted: true }
          ],
          progressLogs: [
            { id: 'pl_3', dateIso: '2026-11-10', percent: 82, note: 'Reserva em R$ 24.500 / R$ 30.000.' }
          ]
        }
      ],
      projects: [
        {
          id: 'proj_nexus',
          name: 'Evolução NEXUS 2026',
          description: 'Refinamento visual, arquitetura offline-first e cobertura completa de testes do centro de comando.',
          deadlineDateIso: '2026-12-18',
          status: 'ACTIVE',
          priority: 'HIGH',
          goalId: 'pg_1'
        },
        {
          id: 'proj_sp',
          name: 'Viagem Técnica SP',
          description: 'Participação na Conferência Kotlin & Android Brasil e visitas técnicas.',
          deadlineDateIso: '2026-11-25',
          status: 'ACTIVE',
          priority: 'NORMAL',
          goalId: 'pg_1'
        },
        {
          id: 'proj_setup',
          name: 'Atualização Setup Estúdio',
          description: 'Ergonomia e equipamentos de trabalho remoto.',
          deadlineDateIso: '2026-12-30',
          status: 'PLANNED',
          priority: 'LOW',
          goalId: 'pg_2'
        }
      ],
      creditCards: [
        {
          id: 'card_uv',
          name: 'Nubank Ultravioleta',
          institution: 'Nu Pagamentos',
          totalLimitCents: 1200000,
          availableLimitCents: 984000,
          closingDay: 10,
          dueDay: 17,
          notes: 'Cartão principal para despesas de trabalho e viagens.'
        },
        {
          id: 'card_itau',
          name: 'Itaú Click Platinum',
          institution: 'Itaú',
          totalLimitCents: 650000,
          availableLimitCents: 582000,
          closingDay: 5,
          dueDay: 12,
          notes: 'Uso recorrente e mercado.'
        }
      ],
      statements: [
        { id: 'stmt_uv_2026_11', creditCardId: 'card_uv', yearMonth: '2026-11', dueDateIso: '2026-11-23', status: 'OVERDUE' },
        { id: 'stmt_uv_2026_12', creditCardId: 'card_uv', yearMonth: '2026-12', dueDateIso: '2026-12-17', status: 'OPEN' },
        { id: 'stmt_itau_2026_12', creditCardId: 'card_itau', yearMonth: '2026-12', dueDateIso: '2026-12-12', status: 'OPEN' }
      ],
      events: [
        {
          id: 'evt_kotlin_sp',
          name: 'Conferência Kotlin & Android Brasil 2026',
          description: 'Trilhas de arquitetura Compose Multiplatform, Room offline-first e performance em Android.',
          categoryId: 'cat_eventos',
          startDateIso: '2026-11-23',
          startTime: '18:30',
          endDateIso: '2026-11-23',
          endTime: '21:30',
          isAllDay: false,
          locationName: 'Expo Center Norte',
          address: 'Rua José Bernardo Pinto, 333',
          city: 'São Paulo',
          state: 'SP',
          officialLink: 'https://kotlinbrasil.example.org',
          ticketLink: 'https://tickets.example.org/kotlin-2026',
          eventStatus: 'CONFIRMED',
          ticketStatus: 'PAID',
          ticketPriceCents: 62000,
          ticketQuantity: 1,
          ticketFeesCents: 6000,
          ticketDueDateIso: '2026-11-23',
          paymentMethod: 'CREDIT_CARD',
          creditCardId: 'card_uv',
          installmentsCount: 1,
          reminderMinutesBefore: 60,
          notes: 'Levar documento com foto e QR Code offline salvo.'
        },
        {
          id: 'evt_workshop_design',
          name: 'Workshop Sistemas de Design Mobile',
          description: 'Sessão prática sobre tokens multi-tema e acessibilidade em telas adaptativas.',
          categoryId: 'cat_eventos',
          startDateIso: '2026-11-28',
          startTime: '09:30',
          endDateIso: '2026-11-28',
          endTime: '13:00',
          isAllDay: false,
          locationName: 'Auditório Paulista',
          address: 'Av. Paulista, 1578',
          city: 'São Paulo',
          state: 'SP',
          officialLink: 'https://designmobile.example.org',
          ticketLink: '',
          eventStatus: 'PLANNED',
          ticketStatus: 'PENDING',
          ticketPriceCents: 24000,
          ticketQuantity: 1,
          ticketFeesCents: 0,
          ticketDueDateIso: '2026-11-26',
          paymentMethod: 'PIX',
          creditCardId: null,
          installmentsCount: 1,
          reminderMinutesBefore: 60,
          notes: 'Confirmar disponibilidade antes de quitar ingresso.'
        }
      ],
      tasks: [
        {
          id: 'task_credencial',
          title: 'Emitir QR Code offline da Conferência Kotlin',
          description: 'Salvar comprovante em PDF e validar credenciamento antecipado no Expo Center Norte.',
          status: 'IN_PROGRESS',
          priority: 'HIGH',
          dueDateIso: '2026-11-23',
          hasDueTime: true,
          dueTime: '14:00',
          categoryId: 'cat_eventos',
          projectId: 'proj_sp',
          eventId: 'evt_kotlin_sp',
          goalId: null,
          subtasks: [
            { id: 'st_1', title: 'Baixar comprovante de pagamento', isDone: true },
            { id: 'st_2', title: 'Gerar QR Code offline na carteira', isDone: false }
          ],
          recurrence: null,
          reminderMinutesBefore: 30,
          notes: 'Chegar 30 min antes do painel inicial.'
        },
        {
          id: 'task_revisar_fatura',
          title: 'Conferir lançamentos da fatura Ultravioleta',
          description: 'Bater valores de ingresso, transporte e assinaturas na fatura 2026-11.',
          status: 'NOT_STARTED',
          priority: 'URGENT',
          dueDateIso: '2026-11-21',
          hasDueTime: false,
          dueTime: null,
          categoryId: 'cat_trabalho',
          projectId: 'proj_nexus',
          eventId: null,
          goalId: 'pg_2',
          subtasks: [
            { id: 'st_3', title: 'Conferir compras de viagem', isDone: false },
            { id: 'st_4', title: 'Validar parcelas Monitor Dell', isDone: false },
            { id: 'st_5', title: 'Agendar pagamento total', isDone: false }
          ],
          recurrence: { frequency: 'MONTHLY', startDateIso: '2026-10-21', endMode: 'NONE', customIntervalDays: 30, weekdaysMask: 0 },
          reminderMinutesBefore: 60,
          notes: ''
        },
        {
          id: 'task_relatorio_sprint',
          title: 'Fechar checklist de entregas da semana',
          description: 'Consolidar pendências de UI e testes unitários.',
          status: 'NOT_STARTED',
          priority: 'NORMAL',
          dueDateIso: '2026-11-23',
          hasDueTime: true,
          dueTime: '17:00',
          categoryId: 'cat_trabalho',
          projectId: 'proj_nexus',
          eventId: null,
          goalId: 'pg_1',
          subtasks: [
            { id: 'st_6', title: 'Revisar tokens de borda', isDone: true },
            { id: 'st_7', title: 'Executar suíte Compose', isDone: true },
            { id: 'st_8', title: 'Atualizar changelog', isDone: false },
            { id: 'st_9', title: 'Publicar resumo', isDone: false }
          ],
          recurrence: null,
          reminderMinutesBefore: 15,
          notes: ''
        },
        {
          id: 'task_preparar_slide',
          title: 'Validar roteiro do Workshop de Design',
          description: 'Preparar exemplos de contraste Dark vs Grafite Suave.',
          status: 'WAITING',
          priority: 'NORMAL',
          dueDateIso: '2026-11-25',
          hasDueTime: true,
          dueTime: '10:00',
          categoryId: 'cat_eventos',
          projectId: 'proj_nexus',
          eventId: 'evt_workshop_design',
          goalId: 'pg_1',
          subtasks: [],
          recurrence: null,
          reminderMinutesBefore: 30,
          notes: ''
        },
        {
          id: 'task_ergonomia',
          title: 'Cotar suporte articulado para monitor',
          description: 'Comparar modelos com ajuste de altura e passagem de cabos.',
          status: 'BLOCKED',
          priority: 'LOW',
          dueDateIso: '2026-11-27',
          hasDueTime: false,
          dueTime: null,
          categoryId: 'cat_equip',
          projectId: 'proj_setup',
          eventId: null,
          goalId: null,
          subtasks: [],
          recurrence: null,
          reminderMinutesBefore: null,
          notes: 'Aguardando confirmação das medidas da mesa.'
        },
        {
          id: 'task_backup_mensal',
          title: 'Exportar snapshot JSON v7 do NEXUS',
          description: 'Garantir cópia local atualizada de todas as tabelas.',
          status: 'COMPLETED',
          priority: 'NORMAL',
          dueDateIso: '2026-11-18',
          hasDueTime: false,
          dueTime: null,
          categoryId: 'cat_trabalho',
          projectId: 'proj_nexus',
          eventId: null,
          goalId: 'pg_1',
          subtasks: [{ id: 'st_10', title: 'Validar schema backup v7', isDone: true }],
          recurrence: null,
          reminderMinutesBefore: null,
          notes: ''
        },
        {
          id: 'task_reserva_hotel',
          title: 'Revisar notas da palestra de abertura',
          description: 'Estudar referências de Compose Multiplatform.',
          status: 'COMPLETED',
          priority: 'LOW',
          dueDateIso: '2026-11-22',
          hasDueTime: false,
          dueTime: null,
          categoryId: 'cat_eventos',
          projectId: 'proj_sp',
          eventId: 'evt_kotlin_sp',
          goalId: null,
          subtasks: [],
          recurrence: null,
          reminderMinutesBefore: null,
          notes: ''
        },
        {
          id: 'task_organizacao_mesa',
          title: 'Organizar arquivos de referência do tablet',
          description: 'Pasta com PDFs e guias de design offline.',
          status: 'NOT_STARTED',
          priority: 'LOW',
          dueDateIso: null,
          hasDueTime: false,
          dueTime: null,
          categoryId: 'cat_trabalho',
          projectId: null,
          eventId: null,
          goalId: null,
          subtasks: [],
          recurrence: null,
          reminderMinutesBefore: null,
          notes: ''
        }
      ],
      appointments: [
        {
          id: 'appt_1',
          title: 'Alinhamento arquitetura offline-first',
          description: 'Revisão de migrações Room e índices de consulta.',
          dateIso: '2026-11-23',
          endDateIso: '2026-11-23',
          startTime: '10:00',
          endTime: '11:00',
          isAllDay: false,
          location: 'Sala Meet',
          link: 'https://meet.example.org/nexus-arch',
          categoryId: 'cat_trabalho',
          eventId: null,
          reminderMinutesBefore: 15
        },
        {
          id: 'appt_2',
          title: 'Revisão de orçamento mensal',
          description: 'Balanço de despesas fixas e faturas abertas de novembro.',
          dateIso: '2026-11-23',
          endDateIso: '2026-11-23',
          startTime: '10:30',
          endTime: '11:15',
          isAllDay: false,
          location: 'Escritório',
          link: '',
          categoryId: 'cat_moradia',
          eventId: null,
          reminderMinutesBefore: 30
        },
        {
          id: 'appt_3',
          title: 'Check-in Expo Center Norte',
          description: 'Retirada de crachá antes da trilha de abertura.',
          dateIso: '2026-11-23',
          endDateIso: '2026-11-23',
          startTime: '18:00',
          endTime: '18:30',
          isAllDay: false,
          location: 'Expo Center Norte',
          link: '',
          categoryId: 'cat_eventos',
          eventId: 'evt_kotlin_sp',
          reminderMinutesBefore: 60
        },
        {
          id: 'appt_tm_1',
          title: 'Retrospectiva de design UI',
          description: 'Avaliação dos ajustes do tema Grafite Suave.',
          dateIso: '2026-11-24',
          endDateIso: '2026-11-24',
          startTime: '09:30',
          endTime: '10:30',
          isAllDay: false,
          location: 'Remoto',
          link: 'https://meet.example.org/ui-retro',
          categoryId: 'cat_trabalho',
          eventId: null,
          reminderMinutesBefore: 15
        },
        {
          id: 'appt_wk_1',
          title: 'Mentoria técnica Android Compose',
          description: 'Sessão de mentoria quinzenal.',
          dateIso: '2026-11-26',
          endDateIso: '2026-11-26',
          startTime: '15:00',
          endTime: '16:00',
          isAllDay: false,
          location: 'Google Meet',
          link: '',
          categoryId: 'cat_trabalho',
          eventId: null,
          reminderMinutesBefore: 30
        }
      ],
      transactions: [
        {
          id: 'tx_salario',
          type: 'INCOME',
          description: 'Recebimento Consultoria Mobile',
          amountCents: 820000,
          categoryId: 'cat_trabalho',
          status: 'SETTLED',
          paymentMethod: 'BANK_TRANSFER',
          dueDateIso: '2026-11-05',
          creditCardId: null,
          statementId: null,
          projectId: 'proj_nexus',
          eventId: null,
          taskId: null,
          eventCostType: null,
          notes: 'Nota fiscal 1042 liquidada.'
        },
        {
          id: 'tx_freela',
          type: 'INCOME',
          description: 'Mentoria Jetpack Compose (previsto)',
          amountCents: 165000,
          categoryId: 'cat_trabalho',
          status: 'PLANNED',
          paymentMethod: 'PIX',
          dueDateIso: '2026-11-27',
          creditCardId: null,
          statementId: null,
          projectId: 'proj_nexus',
          eventId: null,
          taskId: null,
          eventCostType: null,
          notes: 'Recebimento previsto para sexta-feira.'
        },
        {
          id: 'tx_kotlin_ingresso',
          type: 'EXPENSE',
          description: 'Ingresso Conferência Kotlin Brasil',
          amountCents: 68000,
          categoryId: 'cat_eventos',
          status: 'PENDING',
          paymentMethod: 'CREDIT_CARD',
          dueDateIso: '2026-11-23',
          creditCardId: 'card_uv',
          statementId: 'stmt_uv_2026_11',
          projectId: 'proj_sp',
          eventId: 'evt_kotlin_sp',
          taskId: 'task_credencial',
          eventCostType: 'TICKET',
          notes: 'Ingresso + taxa de plataforma lançados na fatura 2026-11 do Ultravioleta.'
        },
        {
          id: 'tx_kotlin_transp',
          type: 'EXPENSE',
          description: 'Translado e estacionamento Expo Center',
          amountCents: 18500,
          categoryId: 'cat_transp',
          status: 'PENDING',
          paymentMethod: 'CREDIT_CARD',
          dueDateIso: '2026-11-23',
          creditCardId: 'card_uv',
          statementId: 'stmt_uv_2026_11',
          projectId: 'proj_sp',
          eventId: 'evt_kotlin_sp',
          taskId: null,
          eventCostType: 'TRANSPORT',
          notes: 'Despesa de deslocamento vinculada ao mesmo evento e fatura.'
        },
        {
          id: 'tx_aluguel',
          type: 'EXPENSE',
          description: 'Aluguel e condomínio',
          amountCents: 215000,
          categoryId: 'cat_moradia',
          status: 'SETTLED',
          paymentMethod: 'PIX',
          dueDateIso: '2026-11-10',
          creditCardId: null,
          statementId: null,
          projectId: null,
          eventId: null,
          taskId: null,
          eventCostType: null,
          notes: ''
        },
        {
          id: 'tx_mercado',
          type: 'EXPENSE',
          description: 'Supermercado mensal',
          amountCents: 84000,
          categoryId: 'cat_alim',
          status: 'SETTLED',
          paymentMethod: 'CREDIT_CARD',
          dueDateIso: '2026-11-12',
          creditCardId: 'card_itau',
          statementId: 'stmt_itau_2026_12',
          projectId: null,
          eventId: null,
          taskId: null,
          eventCostType: null,
          notes: ''
        },
        {
          id: 'tx_internet',
          type: 'EXPENSE',
          description: 'Internet fibra residencial',
          amountCents: 13990,
          categoryId: 'cat_moradia',
          status: 'OVERDUE',
          paymentMethod: 'BOLETO',
          dueDateIso: '2026-11-20',
          creditCardId: null,
          statementId: null,
          projectId: null,
          eventId: null,
          taskId: null,
          eventCostType: null,
          notes: 'Boleto vencido aguardando baixa.'
        },
        {
          id: 'tx_academia',
          type: 'EXPENSE',
          description: 'Plano saúde e academia',
          amountCents: 38000,
          categoryId: 'cat_saude',
          status: 'SETTLED',
          paymentMethod: 'DEBIT',
          dueDateIso: '2026-11-14',
          creditCardId: null,
          statementId: null,
          projectId: null,
          eventId: null,
          taskId: null,
          eventCostType: null,
          notes: ''
        },
        {
          id: 'tx_monitor_4_6',
          type: 'EXPENSE',
          description: 'Monitor Dell 27" 4K (4/6)',
          amountCents: 42000,
          categoryId: 'cat_equip',
          status: 'PENDING',
          paymentMethod: 'CREDIT_CARD',
          dueDateIso: '2026-11-23',
          creditCardId: 'card_uv',
          statementId: 'stmt_uv_2026_11',
          projectId: 'proj_setup',
          eventId: null,
          taskId: null,
          eventCostType: null,
          installmentGroupId: 'inst_monitor',
          notes: 'Parcela 4 de 6.'
        },
        {
          id: 'tx_workshop_ingresso',
          type: 'EXPENSE',
          description: 'Inscrição Workshop Design Mobile',
          amountCents: 24000,
          categoryId: 'cat_eventos',
          status: 'PENDING',
          paymentMethod: 'PIX',
          dueDateIso: '2026-11-26',
          creditCardId: null,
          statementId: null,
          projectId: 'proj_nexus',
          eventId: 'evt_workshop_design',
          taskId: null,
          eventCostType: 'TICKET',
          notes: 'Pix agendado.'
        },
        {
          id: 'tx_energia',
          type: 'EXPENSE',
          description: 'Conta de energia elétrica',
          amountCents: 21800,
          categoryId: 'cat_moradia',
          status: 'PENDING',
          paymentMethod: 'PIX',
          dueDateIso: '2026-12-03',
          creditCardId: null,
          statementId: null,
          projectId: null,
          eventId: null,
          taskId: null,
          eventCostType: null,
          notes: 'Previsão dentro dos próximos 30 dias.'
        },
        {
          id: 'tx_cloud_dez',
          type: 'EXPENSE',
          description: 'Assinatura servidor e domínio',
          amountCents: 12900,
          categoryId: 'cat_trabalho',
          status: 'PENDING',
          paymentMethod: 'CREDIT_CARD',
          dueDateIso: '2026-12-04',
          creditCardId: 'card_uv',
          statementId: 'stmt_uv_2026_12',
          projectId: 'proj_nexus',
          eventId: null,
          taskId: null,
          eventCostType: null,
          notes: 'Ciclo da próxima fatura.'
        }
      ],
      installmentPurchases: [
        {
          id: 'inst_monitor',
          description: 'Monitor Dell 27" 4K',
          totalAmountCents: 252000,
          installmentCount: 6,
          paidCount: 3,
          remainingCount: 3,
          installmentAmountCents: 42000,
          paymentMethod: 'CREDIT_CARD',
          creditCardId: 'card_uv',
          categoryId: 'cat_equip',
          firstDueDateIso: '2026-08-23'
        },
        {
          id: 'inst_teclado',
          description: 'Teclado Mecânico Keychron K3',
          totalAmountCents: 75000,
          installmentCount: 3,
          paidCount: 2,
          remainingCount: 1,
          installmentAmountCents: 25000,
          paymentMethod: 'CREDIT_CARD',
          creditCardId: 'card_itau',
          categoryId: 'cat_equip',
          firstDueDateIso: '2026-10-12'
        }
      ],
      recurringRules: [
        {
          id: 'rec_aluguel',
          description: 'Aluguel e condomínio',
          type: 'EXPENSE',
          amountCents: 215000,
          frequency: 'MONTHLY',
          paymentMethod: 'PIX',
          categoryId: 'cat_moradia',
          nextOccurrenceIso: '2026-12-10',
          isActive: true
        },
        {
          id: 'rec_internet',
          description: 'Internet fibra residencial',
          type: 'EXPENSE',
          amountCents: 13990,
          frequency: 'MONTHLY',
          paymentMethod: 'BOLETO',
          categoryId: 'cat_moradia',
          nextOccurrenceIso: '2026-12-20',
          isActive: true
        },
        {
          id: 'rec_cloud',
          description: 'Assinatura servidor e domínio',
          type: 'EXPENSE',
          amountCents: 12900,
          frequency: 'MONTHLY',
          paymentMethod: 'CREDIT_CARD',
          categoryId: 'cat_trabalho',
          nextOccurrenceIso: '2026-12-04',
          isActive: true
        },
        {
          id: 'rec_salario',
          description: 'Recebimento Consultoria Mobile',
          type: 'INCOME',
          amountCents: 820000,
          frequency: 'MONTHLY',
          paymentMethod: 'BANK_TRANSFER',
          categoryId: 'cat_trabalho',
          nextOccurrenceIso: '2026-12-05',
          isActive: true
        }
      ],
      loans: [
        {
          id: 'loan_1',
          name: 'Financiamento Notebook Trabalho',
          lender: 'Banco Inter',
          principalCents: 720000,
          remainingCents: 360000,
          monthlyInstallmentCents: 60000,
          totalInstallments: 12,
          paidInstallments: 6,
          dueDay: 15,
          interestNote: 'Taxa fixa 1,15% a.m.',
          notes: 'Quitável antecipadamente sem multa.'
        }
      ],
      auditHistory: [
        { id: 'aud_1', timestamp: '23/11/2026 09:12', actionType: 'ATUALIZAÇÃO', entityType: 'Saldo informado', summary: 'Saldo informado ajustado para R$ 4.825,00.' },
        { id: 'aud_2', timestamp: '22/11/2026 18:40', actionType: 'CRIAÇÃO', entityType: 'Lançamento', summary: 'Registrado "Ingresso Conferência Kotlin Brasil" (R$ 680,00).' },
        { id: 'aud_3', timestamp: '15/11/2026 10:05', actionType: 'PAGAMENTO', entityType: 'Empréstimo', summary: 'Parcela 6/12 de "Financiamento Notebook Trabalho" quitada.' }
      ],
      errorLogs: [
        { id: 'err_1', timestamp: '22/11/2026 14:22', origin: 'Mercado (Simulado)', message: 'Simulação de timeout da fonte B3 tratada com cache local preservado.' },
        { id: 'err_2', timestamp: '20/11/2026 09:00', origin: 'Agendador de lembretes', message: 'Aviso informativo: alarme de internet fibra executado via fallback de janela.' }
      ],
      historicalCashflow: [
        { yearMonthIso: '2026-06', yearMonthLabel: 'Jun', realizedIncome: 740000, realizedExpense: 421000, plannedIncome: 760000, plannedExpense: 440000 },
        { yearMonthIso: '2026-07', yearMonthLabel: 'Jul', realizedIncome: 790000, realizedExpense: 468000, plannedIncome: 790000, plannedExpense: 475000 },
        { yearMonthIso: '2026-08', yearMonthLabel: 'Ago', realizedIncome: 755000, realizedExpense: 392000, plannedIncome: 780000, plannedExpense: 410000 },
        { yearMonthIso: '2026-09', yearMonthLabel: 'Set', realizedIncome: 840000, realizedExpense: 512000, plannedIncome: 840000, plannedExpense: 520000 },
        { yearMonthIso: '2026-10', yearMonthLabel: 'Out', realizedIncome: 810000, realizedExpense: 445000, plannedIncome: 825000, plannedExpense: 460000 }
      ],
      budgets: [
        { id: 'bdg_1', categoryId: 'cat_moradia', limitCents: 240000 },
        { id: 'bdg_2', categoryId: 'cat_eventos', limitCents: 85000 },
        { id: 'bdg_3', categoryId: 'cat_alim', limitCents: 110000 }
      ],
      financialGoals: [
        { id: 'fg_1', name: 'Reserva de emergência (6 meses)', currentAmountCents: 2450000, targetAmountCents: 3000000, targetDateIso: '2027-03-31' },
        { id: 'fg_2', name: 'Fundo Eventos & Conferências 2027', currentAmountCents: 120000, targetAmountCents: 350000, targetDateIso: '2027-06-30' }
      ],
      reminders: [
        {
          id: 'rem_1',
          sourceType: 'TRANSACTION',
          sourceId: 'tx_kotlin_ingresso',
          title: 'Vencimento: Ingresso Conferência Kotlin Brasil',
          message: 'Fatura Nubank Ultravioleta (2026-11) · R$ 680,00',
          triggerLabel: '23/11/2026 15:00',
          status: 'ACTIVE',
          isExactScheduled: true
        },
        {
          id: 'rem_2',
          sourceType: 'EVENT',
          sourceId: 'evt_kotlin_sp',
          title: 'Evento hoje: Conferência Kotlin & Android Brasil 2026',
          message: 'Expo Center Norte · 18:30 (60 min antes)',
          triggerLabel: '23/11/2026 17:30',
          status: 'ACTIVE',
          isExactScheduled: true
        },
        {
          id: 'rem_3',
          sourceType: 'TRANSACTION',
          sourceId: 'tx_internet',
          title: 'Conta atrasada: Internet fibra residencial',
          message: 'Boleto vencido em 20/11/2026 · R$ 139,90',
          triggerLabel: '20/11/2026 09:00',
          status: 'FIRED',
          isExactScheduled: false
        }
      ],
      // Estado completo simulado de Mercado (12 ativos do MarketAssetCatalog)
      marketFavorites: ['USD-BRL', 'BTC-BRL', 'IBOV', 'MELI'],
      marketQuotes: {
        'USD-BRL': { assetCode: 'USD-BRL', price: 5.7420, bidPrice: 5.7410, askPrice: 5.7435, previousClose: 5.7180, changePercent: 0.42, variationPeriodLabel: '24h', modality: 'Dólar Comercial', sourceName: 'Simulado · AwesomeAPI', sourceTimestamp: '23/11/2026 14:32', fetchedAt: '23/11/2026 14:35', isDelayed: false, isStale: false, isOffline: false },
        'EUR-BRL': { assetCode: 'EUR-BRL', price: 6.1240, bidPrice: 6.1210, askPrice: 6.1280, previousClose: 6.1350, changePercent: -0.18, variationPeriodLabel: '24h', modality: 'Euro Comercial', sourceName: 'Simulado · AwesomeAPI', sourceTimestamp: '23/11/2026 14:32', fetchedAt: '23/11/2026 14:35', isDelayed: false, isStale: false, isOffline: false },
        'BTC-BRL': { assetCode: 'BTC-BRL', price: 528400.00, bidPrice: 528150.00, askPrice: 528690.00, previousClose: 517278.00, changePercent: 2.15, variationPeriodLabel: '24h', modality: 'Spot (24h)', sourceName: 'Simulado · CoinGecko', sourceTimestamp: '23/11/2026 14:34', fetchedAt: '23/11/2026 14:35', isDelayed: false, isStale: false, isOffline: false },
        'ETH-BRL': { assetCode: 'ETH-BRL', price: 18950.00, bidPrice: 18935.00, askPrice: 18970.00, previousClose: 18688.00, changePercent: 1.40, variationPeriodLabel: '24h', modality: 'Spot (24h)', sourceName: 'Simulado · CoinGecko', sourceTimestamp: '23/11/2026 14:34', fetchedAt: '23/11/2026 14:35', isDelayed: false, isStale: false, isOffline: false },
        'SOL-BRL': { assetCode: 'SOL-BRL', price: 1384.50, bidPrice: 1383.20, askPrice: 1385.90, previousClose: 1405.80, changePercent: -1.52, variationPeriodLabel: '24h', modality: 'Spot (24h)', sourceName: 'Simulado · CoinGecko', sourceTimestamp: '23/11/2026 14:34', fetchedAt: '23/11/2026 14:35', isDelayed: false, isStale: false, isOffline: false },
        'IBOV': { assetCode: 'IBOV', price: 129450.00, bidPrice: null, askPrice: null, previousClose: 128603.00, changePercent: 0.66, variationPeriodLabel: 'pregão', modality: 'Índice', sourceName: 'Simulado · B3 / Yahoo', sourceTimestamp: '23/11/2026 14:18', fetchedAt: '23/11/2026 14:35', isDelayed: true, delayNotice: 'Atraso de ~15 min', isStale: false, isOffline: false },
        'SPX': { assetCode: 'SPX', price: 5968.40, bidPrice: null, askPrice: null, previousClose: 5949.15, changePercent: 0.32, variationPeriodLabel: 'pregão', modality: 'Índice', sourceName: 'Simulado · Stooq/Yahoo', sourceTimestamp: '23/11/2026 14:20', fetchedAt: '23/11/2026 14:35', isDelayed: true, delayNotice: 'Atraso de ~15 min', isStale: false, isOffline: false },
        'IXIC': { assetCode: 'IXIC', price: 19054.20, bidPrice: null, askPrice: null, previousClose: 19102.80, changePercent: -0.25, variationPeriodLabel: 'pregão', modality: 'Índice', sourceName: 'Simulado · Stooq/Yahoo', sourceTimestamp: '23/11/2026 14:20', fetchedAt: '23/11/2026 14:35', isDelayed: true, delayNotice: 'Atraso de ~15 min', isStale: false, isOffline: false },
        'PETR4': { assetCode: 'PETR4', price: 38.64, bidPrice: 38.63, askPrice: 38.65, previousClose: 38.12, changePercent: 1.36, variationPeriodLabel: 'pregão', modality: 'Pregão', sourceName: 'Simulado · B3 / brapi', sourceTimestamp: '23/11/2026 14:19', fetchedAt: '23/11/2026 14:35', isDelayed: true, delayNotice: 'Atraso de ~15 min', isStale: false, isOffline: false },
        'VALE3': { assetCode: 'VALE3', price: 58.92, bidPrice: 58.90, askPrice: 58.94, previousClose: 59.40, changePercent: -0.81, variationPeriodLabel: 'pregão', modality: 'Pregão', sourceName: 'Simulado · B3 / brapi', sourceTimestamp: '23/11/2026 14:19', fetchedAt: '23/11/2026 14:35', isDelayed: true, delayNotice: 'Atraso de ~15 min', isStale: false, isOffline: false },
        'ITUB4': { assetCode: 'ITUB4', price: 34.48, bidPrice: 34.47, askPrice: 34.49, previousClose: 34.15, changePercent: 0.97, variationPeriodLabel: 'pregão', modality: 'Pregão', sourceName: 'Simulado · B3 / brapi', sourceTimestamp: '23/11/2026 14:19', fetchedAt: '23/11/2026 14:35', isDelayed: true, delayNotice: 'Atraso de ~15 min', isStale: false, isOffline: false },
        'MELI': { assetCode: 'MELI', price: 1948.30, bidPrice: 1947.50, askPrice: 1949.10, previousClose: 1916.40, changePercent: 1.66, variationPeriodLabel: 'pregão', modality: 'Pregão', sourceName: 'Simulado · Nasdaq', sourceTimestamp: '23/11/2026 14:21', fetchedAt: '23/11/2026 14:35', isDelayed: true, delayNotice: 'Atraso de ~15 min', isStale: false, isOffline: false }
      },
      marketAlerts: [
        {
          id: 'ma_1',
          assetCode: 'USD-BRL',
          condition: 'PRICE_BELOW',
          targetPrice: 5.65,
          isActive: true,
          status: 'WAITING',
          createdAt: '20/11/2026 11:15',
          triggeredAt: null,
          triggeredPrice: null
        },
        {
          id: 'ma_2',
          assetCode: 'BTC-BRL',
          condition: 'PRICE_ABOVE',
          targetPrice: 525000,
          isActive: true,
          status: 'TRIGGERED',
          createdAt: '18/11/2026 09:40',
          triggeredAt: '23/11/2026 13:10',
          triggeredPrice: 526120
        }
      ],
      marketDemoQuotes: [
        { code: 'USD-BRL', label: 'Dólar Comercial', symbol: 'USD/BRL', value: 'R$ 5,74', change: '+0,42%', positive: true },
        { code: 'EUR-BRL', label: 'Euro Comercial', symbol: 'EUR/BRL', value: 'R$ 6,12', change: '-0,18%', positive: false },
        { code: 'BTC-BRL', label: 'Bitcoin', symbol: 'BTC', value: 'R$ 528.400', change: '+2,15%', positive: true }
      ]
    };
  }

  function appendAudit(state, actionType, entityType, summary) {
    state.auditHistory.unshift({
      id: `aud_${Date.now()}_${Math.floor(Math.random() * 999)}`,
      timestamp: '23/11/2026 agora',
      actionType,
      entityType,
      summary
    });
    if (state.auditHistory.length > 40) state.auditHistory.length = 40;
  }

  function formatBrl(cents) {
    const num = (Number(cents) || 0) / 100;
    return num.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
  }

  function formatDateBr(iso) {
    if (!iso) return 'Sem data';
    const [y, m, d] = iso.split('-');
    if (!y || !m || !d) return iso;
    return `${d}/${m}/${y}`;
  }

  function formatDayMonth(iso) {
    if (!iso) return '';
    const [, m, d] = iso.split('-');
    return `${d}/${m}`;
  }

  const MONTH_NAMES = ['Janeiro', 'Fevereiro', 'Março', 'Abril', 'Maio', 'Junho', 'Julho', 'Agosto', 'Setembro', 'Outubro', 'Novembro', 'Dezembro'];
  const MONTH_SHORT = ['Jan', 'Fev', 'Mar', 'Abr', 'Mai', 'Jun', 'Jul', 'Ago', 'Set', 'Out', 'Nov', 'Dez'];
  const WEEKDAYS_FULL = ['Domingo', 'Segunda-feira', 'Terça-feira', 'Quarta-feira', 'Quinta-feira', 'Sexta-feira', 'Sábado'];
  const WEEKDAYS_SHORT = ['Dom', 'Seg', 'Ter', 'Qua', 'Qui', 'Sex', 'Sáb'];

  function parseIsoDate(iso) {
    const [y, m, d] = iso.split('-').map(Number);
    return new Date(y, m - 1, d);
  }

  function toIsoDate(dt) {
    const y = dt.getFullYear();
    const m = String(dt.getMonth() + 1).padStart(2, '0');
    const d = String(dt.getDate()).padStart(2, '0');
    return `${y}-${m}-${d}`;
  }

  function addDaysIso(iso, delta) {
    const dt = parseIsoDate(iso);
    dt.setDate(dt.getDate() + delta);
    return toIsoDate(dt);
  }

  function getWeekDatesIso(anchorIso) {
    const dt = parseIsoDate(anchorIso);
    const day = dt.getDay(); // 0=Dom..6=Sab
    const diffToMon = day === 0 ? -6 : 1 - day;
    const mon = new Date(dt);
    mon.setDate(dt.getDate() + diffToMon);
    const arr = [];
    for (let i = 0; i < 7; i++) {
      const cur = new Date(mon);
      cur.setDate(mon.getDate() + i);
      arr.push(toIsoDate(cur));
    }
    return arr;
  }

  function formatDateByPattern(iso, pattern = 'WEEKDAY_DD_MM_YYYY') {
    const dt = parseIsoDate(iso);
    const wd = WEEKDAYS_FULL[dt.getDay()];
    const d = String(dt.getDate()).padStart(2, '0');
    const m = String(dt.getMonth() + 1).padStart(2, '0');
    const y = dt.getFullYear();
    const mName = MONTH_NAMES[dt.getMonth()].toLowerCase();
    if (pattern === 'DD_MM_YYYY') return `${d}/${m}/${y}`;
    if (pattern === 'DD_MMM_YYYY') return `${dt.getDate()} de ${mName} de ${y}`;
    if (pattern === 'DD_MM_WEEKDAY') return `${d}/${m} · ${wd}`;
    return `${wd}, ${d}/${m}/${y}`;
  }

  function formatWeekdayShort(iso) {
    return WEEKDAYS_SHORT[parseIsoDate(iso).getDay()];
  }

  function formatMonthTitle(ym) {
    const [y, m] = ym.split('-').map(Number);
    return `${MONTH_NAMES[(m - 1 + 12) % 12]} de ${y}`;
  }

  function formatMonthShort(ym) {
    const [, m] = ym.split('-').map(Number);
    return MONTH_SHORT[(m - 1 + 12) % 12];
  }

  function shiftMonthIso(ym, delta) {
    const [y, m] = ym.split('-').map(Number);
    const dt = new Date(y, m - 1 + delta, 1);
    const ny = dt.getFullYear();
    const nm = String(dt.getMonth() + 1).padStart(2, '0');
    return `${ny}-${nm}`;
  }

  function computeTaskCounts(task) {
    const subs = Array.isArray(task.subtasks) ? task.subtasks : [];
    return {
      subtasksDone: subs.filter((s) => s.isDone).length,
      subtasksTotal: subs.length
    };
  }

  function computeTaskExpenseCents(state, taskId) {
    return state.transactions
      .filter((tx) => tx.taskId === taskId && tx.type === 'EXPENSE' && tx.status !== 'CANCELLED')
      .reduce((acc, tx) => acc + tx.amountCents, 0);
  }

  function computeProjectProgress(state, project) {
    const linkedTasks = state.tasks.filter((t) => t.projectId === project.id && t.status !== 'CANCELLED');
    const completedTasks = linkedTasks.filter((t) => t.status === 'COMPLETED').length;
    const totalTasks = linkedTasks.length;
    const progressPercent = totalTasks > 0 ? Math.round((completedTasks * 100) / totalTasks) : (project.status === 'COMPLETED' ? 100 : 0);
    const linkedTxs = state.transactions.filter((tx) => tx.projectId === project.id && tx.type === 'EXPENSE' && tx.status !== 'CANCELLED');
    const estimatedCostCents = linkedTxs.reduce((acc, tx) => acc + tx.amountCents, 0);
    const paidCostCents = linkedTxs.filter((tx) => tx.status === 'SETTLED').reduce((acc, tx) => acc + tx.amountCents, 0);
    return {
      project,
      totalTasks,
      completedTasks,
      progressPercent,
      linkedTransactionsCount: linkedTxs.length,
      estimatedCostCents,
      paidCostCents
    };
  }

  function computeGoalDetails(state, goal) {
    const milestones = Array.isArray(goal.milestones) ? goal.milestones : [];
    const completedMilestonesCount = milestones.filter((m) => m.isCompleted).length;
    const linkedTasks = state.tasks.filter((t) => t.goalId === goal.id);
    const linkedProjects = state.projects.filter((p) => p.goalId === goal.id).map((p) => computeProjectProgress(state, p));
    const category = state.categories.find((c) => c.id === goal.categoryId) || null;

    let progressPercent = goal.manualProgressPercent || 0;
    if (goal.progressMode === 'AUTO') {
      const scores = [];
      if (milestones.length > 0) {
        scores.push((completedMilestonesCount * 100) / milestones.length);
      }
      if (linkedTasks.length > 0) {
        const doneT = linkedTasks.filter((t) => t.status === 'COMPLETED').length;
        scores.push((doneT * 100) / linkedTasks.length);
      }
      if (linkedProjects.length > 0) {
        const avgProj = linkedProjects.reduce((a, p) => a + p.progressPercent, 0) / linkedProjects.length;
        scores.push(avgProj);
      }
      progressPercent = scores.length > 0
        ? Math.round(scores.reduce((a, b) => a + b, 0) / scores.length)
        : (goal.status === 'COMPLETED' ? 100 : 0);
    }

    const isOverdue = goal.status === 'ACTIVE' && Boolean(goal.targetDateIso) && goal.targetDateIso < state.todayIso;
    return {
      goal,
      category,
      milestones,
      completedMilestonesCount,
      linkedTasks,
      linkedProjects,
      progressLogs: goal.progressLogs || [],
      progressPercent,
      isOverdue
    };
  }

  function computeAppointmentConflicts(appointments) {
    const map = {};
    const timed = appointments.filter((a) => !a.isAllDay);
    for (let i = 0; i < timed.length; i++) {
      for (let j = i + 1; j < timed.length; j++) {
        const a = timed[i];
        const b = timed[j];
        if (a.dateIso === b.dateIso && a.startTime < b.endTime && b.startTime < a.endTime) {
          map[a.id] = map[a.id] || [];
          map[b.id] = map[b.id] || [];
          map[a.id].push(b.title);
          map[b.id].push(a.title);
        }
      }
    }
    return map;
  }

  function computeForecast30Days(state) {
    const start = state.todayIso;
    const end = addDaysIso(start, 30);
    const activeFuture = state.transactions.filter((tx) => {
      if (tx.status === 'SETTLED' || tx.status === 'CANCELLED') return false;
      const d = tx.dueDateIso || '';
      return d >= start && d <= end;
    });
    const plannedIncomeCents = activeFuture
      .filter((tx) => tx.type === 'INCOME')
      .reduce((acc, tx) => acc + tx.amountCents, 0);
    const upcomingExpenses = activeFuture
      .filter((tx) => tx.type === 'EXPENSE')
      .sort((a, b) => (a.dueDateIso || '').localeCompare(b.dueDateIso || ''));
    const upcomingExpensesCents = upcomingExpenses.reduce((acc, tx) => acc + tx.amountCents, 0);
    return {
      plannedIncomeCents,
      upcomingExpensesCents,
      topUpcomingExpenses: upcomingExpenses.slice(0, 4)
    };
  }

  function computeFinanceSelectors(state, monthIso) {
    const monthTxs = state.transactions.filter((tx) => (tx.dueDateIso || '').startsWith(monthIso) && tx.status !== 'CANCELLED');
    const receivedIncomeCents = monthTxs
      .filter((tx) => tx.type === 'INCOME' && tx.status === 'SETTLED')
      .reduce((acc, tx) => acc + tx.amountCents, 0);
    const plannedIncomeCents = monthTxs
      .filter((tx) => tx.type === 'INCOME' && tx.status !== 'SETTLED')
      .reduce((acc, tx) => acc + tx.amountCents, 0);
    const paidExpenseCents = monthTxs
      .filter((tx) => tx.type === 'EXPENSE' && tx.status === 'SETTLED')
      .reduce((acc, tx) => acc + tx.amountCents, 0);
    const pendingExpenseCents = monthTxs
      .filter((tx) => tx.type === 'EXPENSE' && (tx.status === 'PENDING' || tx.status === 'PLANNED' || tx.status === 'OVERDUE'))
      .reduce((acc, tx) => acc + tx.amountCents, 0);

    const overdueAll = state.transactions.filter((tx) => tx.type === 'EXPENSE' && tx.status === 'OVERDUE');
    const overdueExpenseCents = overdueAll.reduce((acc, tx) => acc + tx.amountCents, 0);

    const upcomingAll = state.transactions
      .filter((tx) => tx.type === 'EXPENSE' && tx.status !== 'SETTLED' && tx.status !== 'CANCELLED')
      .sort((a, b) => (a.dueDateIso || '').localeCompare(b.dueDateIso || ''));

    const nextDueTransaction = upcomingAll[0] || null;

    function buildCategoryPoints(paidOnly) {
      const map = new Map();
      monthTxs.forEach((tx) => {
        if (tx.type !== 'EXPENSE') return;
        if (paidOnly && tx.status !== 'SETTLED') return;
        const key = tx.categoryId || '__UNCATEGORIZED__';
        map.set(key, (map.get(key) || 0) + tx.amountCents);
      });
      const list = Array.from(map.entries()).map(([catId, totalCents]) => {
        const cat = state.categories.find((c) => c.id === catId);
        return {
          categoryId: catId === '__UNCATEGORIZED__' ? null : catId,
          categoryName: cat ? cat.name : 'Sem categoria',
          colorHex: cat ? cat.colorHex : '#A3B1C6',
          totalCents
        };
      });
      return list.sort((a, b) => b.totalCents - a.totalCents);
    }

    const novRealizedIncome = state.transactions
      .filter((tx) => (tx.dueDateIso || '').startsWith('2026-11') && tx.type === 'INCOME' && tx.status === 'SETTLED')
      .reduce((acc, tx) => acc + tx.amountCents, 0);
    const novRealizedExpense = state.transactions
      .filter((tx) => (tx.dueDateIso || '').startsWith('2026-11') && tx.type === 'EXPENSE' && tx.status === 'SETTLED')
      .reduce((acc, tx) => acc + tx.amountCents, 0);
    const novPlannedIncome = state.transactions
      .filter((tx) => (tx.dueDateIso || '').startsWith('2026-11') && tx.type === 'INCOME' && tx.status !== 'CANCELLED')
      .reduce((acc, tx) => acc + tx.amountCents, 0);
    const novPlannedExpense = state.transactions
      .filter((tx) => (tx.dueDateIso || '').startsWith('2026-11') && tx.type === 'EXPENSE' && tx.status !== 'CANCELLED')
      .reduce((acc, tx) => acc + tx.amountCents, 0);

    const cashflow6Months = [
      ...state.historicalCashflow,
      {
        yearMonthIso: '2026-11',
        yearMonthLabel: 'Nov',
        realizedIncome: novRealizedIncome,
        realizedExpense: novRealizedExpense,
        plannedIncome: novPlannedIncome,
        plannedExpense: novPlannedExpense
      }
    ];

    const budgetProgress = state.budgets.map((b) => {
      const cat = state.categories.find((c) => c.id === b.categoryId) || { id: b.categoryId, name: 'Categoria' };
      const spentCents = monthTxs
        .filter((tx) => tx.type === 'EXPENSE' && tx.categoryId === b.categoryId)
        .reduce((acc, tx) => acc + tx.amountCents, 0);
      const usedPercent = b.limitCents > 0 ? Math.round((spentCents * 100) / b.limitCents) : 0;
      const exceededCents = Math.max(0, spentCents - b.limitCents);
      return { budget: b, category: cat, spentCents, usedPercent, exceededCents };
    });

    const cardsWithStatements = state.creditCards.map((card) => {
      const stmts = state.statements
        .filter((s) => s.creditCardId === card.id)
        .map((s) => {
          const txs = state.transactions.filter((tx) => tx.statementId === s.id);
          const totalCents = txs.reduce((acc, tx) => acc + tx.amountCents, 0);
          return { statement: s, card, transactions: txs, totalCents };
        });
      const openTotal = stmts
        .filter((sw) => sw.statement.status !== 'PAID')
        .reduce((acc, sw) => acc + sw.totalCents, 0);
      const computedRemainingLimitCents = Math.max(0, card.totalLimitCents - openTotal);
      return { card, statements: stmts, currentOpenTotalCents: openTotal, computedRemainingLimitCents };
    });

    const openStatements = cardsWithStatements
      .flatMap((cw) => cw.statements)
      .filter((sw) => sw.statement.status !== 'PAID');

    return {
      receivedIncomeCents,
      plannedIncomeCents,
      paidExpenseCents,
      pendingExpenseCents,
      overdueExpenseCents,
      overdueCount: overdueAll.length,
      monthResultCents: receivedIncomeCents - paidExpenseCents,
      nextDueTransaction,
      upcomingPayments: upcomingAll,
      categorySpendPaidAndPlanned: buildCategoryPoints(false),
      categorySpendPaidOnly: buildCategoryPoints(true),
      cashflow6Months,
      forecast30Days: computeForecast30Days(state),
      budgetProgress,
      cardsWithStatements,
      openStatements
    };
  }

  function computeTaskDistribution(state) {
    const dated = state.tasks.filter((t) => t.dueDateIso && t.status !== 'CANCELLED');
    const completedCount = dated.filter((t) => t.status === 'COMPLETED').length;
    const inProgressCount = dated.filter((t) => t.status === 'IN_PROGRESS').length;
    const pendingCount = dated.filter((t) => t.status !== 'COMPLETED' && t.status !== 'IN_PROGRESS').length;
    const overdueCount = dated.filter((t) => t.status !== 'COMPLETED' && t.dueDateIso < state.todayIso).length;
    const undatedPendingCount = state.tasks.filter((t) => !t.dueDateIso && t.status !== 'COMPLETED' && t.status !== 'CANCELLED').length;
    return {
      completedCount,
      inProgressCount,
      pendingCount,
      overdueCount,
      undatedPendingCount,
      overdueTasks: state.tasks.filter((t) => t.status !== 'COMPLETED' && t.status !== 'CANCELLED' && t.dueDateIso && t.dueDateIso < state.todayIso),
      priorityTasksToday: state.tasks.filter((t) => t.status !== 'COMPLETED' && t.status !== 'CANCELLED' && t.dueDateIso === state.todayIso)
    };
  }

  function computeEventDetails(state, eventEntity) {
    const linkedTransactions = state.transactions.filter((tx) => tx.eventId === eventEntity.id);
    const linkedTasks = state.tasks.filter((t) => t.eventId === eventEntity.id);
    const category = state.categories.find((c) => c.id === eventEntity.categoryId) || null;
    const estimatedTotalCostCents = linkedTransactions
      .filter((tx) => tx.type === 'EXPENSE' && tx.status !== 'CANCELLED')
      .reduce((acc, tx) => acc + tx.amountCents, 0);
    const paidTotalCostCents = linkedTransactions
      .filter((tx) => tx.type === 'EXPENSE' && tx.status === 'SETTLED')
      .reduce((acc, tx) => acc + tx.amountCents, 0);
    return {
      event: eventEntity,
      category,
      linkedTransactions,
      linkedTasks,
      estimatedTotalCostCents,
      paidTotalCostCents
    };
  }

  function computeDailyBriefing(state) {
    const today = state.todayIso;
    const tomorrow = addDaysIso(today, 1);
    const fin = computeFinanceSelectors(state, '2026-11');
    const conflictMap = computeAppointmentConflicts(state.appointments);
    const todayAppointments = state.appointments
      .filter((a) => a.dateIso === today)
      .map((a) => ({
        ...a,
        categoryName: state.categories.find((c) => c.id === a.categoryId)?.name || 'Agenda',
        conflictTitles: conflictMap[a.id] || []
      }));
    const overdueTasks = state.tasks.filter((t) => t.status !== 'COMPLETED' && t.status !== 'CANCELLED' && t.dueDateIso && t.dueDateIso < today);
    const todayTasks = state.tasks.filter((t) => t.status !== 'COMPLETED' && t.status !== 'CANCELLED' && t.dueDateIso === today);
    const completedTasksToday = state.tasks.filter((t) => t.status === 'COMPLETED');
    const todayEvents = state.events
      .filter((e) => e.startDateIso === today)
      .map((e) => computeEventDetails(state, e));
    const overdueBills = state.transactions.filter((tx) => tx.type === 'EXPENSE' && tx.status === 'OVERDUE');
    const billsDueToday = state.transactions.filter((tx) => tx.type === 'EXPENSE' && tx.status !== 'SETTLED' && tx.status !== 'CANCELLED' && tx.dueDateIso === today);
    const allBills = [...overdueBills, ...billsDueToday];
    const totalBillsDueAmountCents = allBills.reduce((acc, tx) => acc + tx.amountCents, 0);

    const tomorrowAppointments = state.appointments
      .filter((a) => a.dateIso === tomorrow)
      .map((a) => ({
        ...a,
        categoryName: state.categories.find((c) => c.id === a.categoryId)?.name || 'Agenda',
        conflictTitles: conflictMap[a.id] || []
      }));
    const tomorrowTasks = state.tasks.filter((t) => t.status !== 'COMPLETED' && t.dueDateIso === tomorrow);
    const tomorrowBills = state.transactions.filter((tx) => tx.type === 'EXPENSE' && tx.status !== 'SETTLED' && tx.dueDateIso === tomorrow);

    return {
      todayAppointments,
      overdueTasks,
      todayTasks,
      completedTasksToday,
      todayEvents,
      overdueBills,
      billsDueToday,
      totalBillsDueAmountCents,
      statementsDueTodayOrOverdue: fin.openStatements.filter((sw) => sw.statement.status === 'OVERDUE' || sw.statement.dueDateIso === today),
      tomorrowAppointments,
      tomorrowTasks,
      tomorrowBills,
      isReviewedToday: state.briefingState.morningReviewedToday,
      isReviewedTonight: state.briefingState.nightReviewedTonight
    };
  }

  // Motor da Agenda unificada (AgendaEngine.kt: 6 fontes -> 5 kinds)
  function computeAgendaOccurrences(state, windowStartIso, windowEndIso, filter = {}) {
    const conflictMap = computeAppointmentConflicts(state.appointments);
    const occurrences = [];
    const kindFilter = filter.kindFilter || null;
    const catFilter = filter.categoryFilterId || null;
    const search = (filter.searchQuery || '').trim().toLowerCase();
    const showPaid = Boolean(filter.showPaid);

    // 1. Compromissos
    if (!kindFilter || kindFilter === 'APPOINTMENT') {
      state.appointments.forEach((appt) => {
        const spanStart = appt.dateIso;
        const spanEnd = appt.endDateIso && appt.endDateIso >= spanStart ? appt.endDateIso : spanStart;
        const cat = state.categories.find((c) => c.id === appt.categoryId);
        const conflicts = conflictMap[appt.id] || [];
        const timeLabel = appt.isAllDay || spanStart !== spanEnd ? 'Dia inteiro' : `${appt.startTime}–${appt.endTime}`;
        const sub = [timeLabel, cat ? cat.name : null, appt.location || null].filter(Boolean).join(' · ');
        let d = spanStart < windowStartIso ? windowStartIso : spanStart;
        const endClamp = spanEnd > windowEndIso ? windowEndIso : spanEnd;
        while (d <= endClamp) {
          occurrences.push({
            stableKey: `APPT:${appt.id}:${d}`,
            sourceType: 'APPOINTMENT',
            sourceId: appt.id,
            kind: 'APPOINTMENT',
            occurrenceDateIso: d,
            spanStartDateIso: spanStart,
            spanEndDateIso: spanEnd,
            isAllDay: appt.isAllDay || spanStart !== spanEnd,
            title: appt.title,
            subtitle: sub,
            categoryId: appt.categoryId,
            colorHex: cat ? cat.colorHex : '#4C8BF5',
            isOverdue: false,
            isDoneOrSettled: false,
            hasConflict: conflicts.length > 0,
            conflictTitles: conflicts,
            sortTime: appt.isAllDay ? '00:00' : (appt.startTime || '09:00'),
            searchableText: `${appt.title} ${appt.description || ''} ${appt.location || ''}`.toLowerCase()
          });
          d = addDaysIso(d, 1);
        }
      });
    }

    // 2. Tarefas com prazo + recorrências mensais virtuais
    if (!kindFilter || kindFilter === 'TASK') {
      state.tasks.forEach((task) => {
        if (!task.dueDateIso || task.status === 'CANCELLED') return;
        const cat = state.categories.find((c) => c.id === task.categoryId);
        const proj = state.projects.find((p) => p.id === task.projectId);
        const isDone = task.status === 'COMPLETED';
        const isOverdue = !isDone && task.dueDateIso < state.todayIso;
        if (task.dueDateIso >= windowStartIso && task.dueDateIso <= windowEndIso) {
          occurrences.push({
            stableKey: `TASK:${task.id}:${task.dueDateIso}`,
            sourceType: 'TASK',
            sourceId: task.id,
            kind: 'TASK',
            occurrenceDateIso: task.dueDateIso,
            spanStartDateIso: task.dueDateIso,
            spanEndDateIso: task.dueDateIso,
            isAllDay: !task.hasDueTime,
            title: task.title,
            subtitle: [task.hasDueTime && task.dueTime ? task.dueTime : 'Dia inteiro', LABELS.TaskStatus[task.status], proj ? proj.name : null].filter(Boolean).join(' · '),
            categoryId: task.categoryId,
            colorHex: cat ? cat.colorHex : '#7CFFB2',
            isOverdue,
            isDoneOrSettled: isDone,
            hasConflict: false,
            conflictTitles: [],
            sortTime: task.hasDueTime && task.dueTime ? task.dueTime : '23:59',
            searchableText: `${task.title} ${task.description || ''} ${proj ? proj.name : ''}`.toLowerCase(),
            isVirtualRecurrence: false
          });
        }
        // Projeção virtual simples para tarefas recorrentes mensais dentro da janela futura
        if (task.recurrence && task.recurrence.frequency === 'MONTHLY') {
          const dayPart = task.dueDateIso.slice(8, 10);
          const nextIso = `${shiftMonthIso(task.dueDateIso.slice(0, 7), 1)}-${dayPart}`;
          if (nextIso >= windowStartIso && nextIso <= windowEndIso && nextIso !== task.dueDateIso) {
            occurrences.push({
              stableKey: `VTASK:${task.id}:${nextIso}`,
              sourceType: 'TASK',
              sourceId: task.id,
              kind: 'TASK',
              occurrenceDateIso: nextIso,
              spanStartDateIso: nextIso,
              spanEndDateIso: nextIso,
              isAllDay: !task.hasDueTime,
              title: `${task.title} (recorrência)`,
              subtitle: `Recorrente · Mensal`,
              categoryId: task.categoryId,
              colorHex: cat ? cat.colorHex : '#7CFFB2',
              isOverdue: false,
              isDoneOrSettled: false,
              hasConflict: false,
              conflictTitles: [],
              sortTime: '23:59',
              searchableText: `${task.title} recorrente`.toLowerCase(),
              isVirtualRecurrence: true
            });
          }
        }
      });
    }

    // 3. Eventos
    if (!kindFilter || kindFilter === 'EVENT') {
      state.events.forEach((ev) => {
        if (ev.eventStatus === 'CANCELLED') return;
        const spanStart = ev.startDateIso;
        const spanEnd = ev.endDateIso && ev.endDateIso >= spanStart ? ev.endDateIso : spanStart;
        const cat = state.categories.find((c) => c.id === ev.categoryId);
        let d = spanStart < windowStartIso ? windowStartIso : spanStart;
        const endClamp = spanEnd > windowEndIso ? windowEndIso : spanEnd;
        while (d <= endClamp) {
          occurrences.push({
            stableKey: `EVENT:${ev.id}:${d}`,
            sourceType: 'EVENT',
            sourceId: ev.id,
            kind: 'EVENT',
            occurrenceDateIso: d,
            spanStartDateIso: spanStart,
            spanEndDateIso: spanEnd,
            isAllDay: ev.isAllDay || spanStart !== spanEnd,
            title: ev.name,
            subtitle: [ev.isAllDay ? 'Dia inteiro' : `${ev.startTime}–${ev.endTime}`, ev.locationName, LABELS.TicketStatus[ev.ticketStatus]].filter(Boolean).join(' · '),
            categoryId: ev.categoryId,
            colorHex: cat ? cat.colorHex : '#7CFFB2',
            isOverdue: false,
            isDoneOrSettled: ev.eventStatus === 'ATTENDED',
            hasConflict: false,
            conflictTitles: [],
            sortTime: ev.isAllDay ? '00:00' : (ev.startTime || '09:00'),
            searchableText: `${ev.name} ${ev.locationName || ''} ${ev.city || ''}`.toLowerCase()
          });
          d = addDaysIso(d, 1);
        }
      });
    }

    // 4. Contas financeiras diretas (sem fatura) + Faturas de cartão
    if (!kindFilter || kindFilter === 'FINANCE') {
      state.transactions.forEach((tx) => {
        if (tx.type !== 'EXPENSE' || tx.statementId || tx.status === 'CANCELLED' || !tx.dueDateIso) return;
        if (!showPaid && tx.status === 'SETTLED') return;
        if (tx.dueDateIso < windowStartIso || tx.dueDateIso > windowEndIso) return;
        const cat = state.categories.find((c) => c.id === tx.categoryId);
        occurrences.push({
          stableKey: `TX:${tx.id}:${tx.dueDateIso}`,
          sourceType: 'TRANSACTION',
          sourceId: tx.id,
          kind: 'FINANCE',
          occurrenceDateIso: tx.dueDateIso,
          spanStartDateIso: tx.dueDateIso,
          spanEndDateIso: tx.dueDateIso,
          isAllDay: true,
          title: tx.description,
          subtitle: `${LABELS.FinancialStatus[tx.status]} · ${LABELS.PaymentMethod[tx.paymentMethod]}`,
          categoryId: tx.categoryId,
          colorHex: tx.status === 'OVERDUE' ? '#E5484D' : '#E5A000',
          amountCents: tx.amountCents,
          isOverdue: tx.status === 'OVERDUE' || (tx.status !== 'SETTLED' && tx.dueDateIso < state.todayIso),
          isDoneOrSettled: tx.status === 'SETTLED',
          hasConflict: false,
          conflictTitles: [],
          sortTime: '08:00',
          searchableText: `${tx.description} ${tx.notes || ''}`.toLowerCase()
        });
      });

      state.statements.forEach((st) => {
        if (!showPaid && st.status === 'PAID') return;
        if (st.dueDateIso < windowStartIso || st.dueDateIso > windowEndIso) return;
        const card = state.creditCards.find((c) => c.id === st.creditCardId);
        const totalCents = state.transactions
          .filter((tx) => tx.statementId === st.id && tx.status !== 'CANCELLED')
          .reduce((acc, tx) => acc + tx.amountCents, 0);
        occurrences.push({
          stableKey: `STMT:${st.id}:${st.dueDateIso}`,
          sourceType: 'STATEMENT',
          sourceId: st.id,
          kind: 'FINANCE',
          occurrenceDateIso: st.dueDateIso,
          spanStartDateIso: st.dueDateIso,
          spanEndDateIso: st.dueDateIso,
          isAllDay: true,
          title: `Fatura ${card ? card.name : 'Cartão'} (${st.yearMonth})`,
          subtitle: `${LABELS.StatementStatus[st.status]} · Vencimento ${formatDateBr(st.dueDateIso)}`,
          categoryId: null,
          colorHex: st.status === 'OVERDUE' ? '#E5484D' : '#E5A000',
          amountCents: totalCents,
          isOverdue: st.status === 'OVERDUE',
          isDoneOrSettled: st.status === 'PAID',
          hasConflict: false,
          conflictTitles: [],
          sortTime: '08:30',
          searchableText: `fatura ${card ? card.name : ''} ${st.yearMonth}`.toLowerCase()
        });
      });
    }

    // 5. Prazos de Projetos
    if (!kindFilter || kindFilter === 'PROJECT') {
      state.projects.forEach((proj) => {
        if (!proj.deadlineDateIso || proj.status === 'ARCHIVED') return;
        if (proj.deadlineDateIso < windowStartIso || proj.deadlineDateIso > windowEndIso) return;
        const pr = computeProjectProgress(state, proj);
        occurrences.push({
          stableKey: `PROJ:${proj.id}:${proj.deadlineDateIso}`,
          sourceType: 'PROJECT',
          sourceId: proj.id,
          kind: 'PROJECT',
          occurrenceDateIso: proj.deadlineDateIso,
          spanStartDateIso: proj.deadlineDateIso,
          spanEndDateIso: proj.deadlineDateIso,
          isAllDay: true,
          title: `Prazo: ${proj.name}`,
          subtitle: `${LABELS.ProjectStatus[proj.status]} · ${pr.progressPercent}% (${pr.completedTasks}/${pr.totalTasks} tarefas)`,
          categoryId: null,
          colorHex: '#5CC8FF',
          isOverdue: proj.status !== 'COMPLETED' && proj.deadlineDateIso < state.todayIso,
          isDoneOrSettled: proj.status === 'COMPLETED',
          hasConflict: false,
          conflictTitles: [],
          sortTime: '23:50',
          searchableText: `${proj.name} ${proj.description || ''}`.toLowerCase()
        });
      });
    }

    return occurrences
      .filter((o) => {
        if (catFilter && o.categoryId !== catFilter) return false;
        if (search && !o.searchableText.includes(search) && !o.title.toLowerCase().includes(search)) return false;
        return true;
      })
      .sort((a, b) => {
        if (a.occurrenceDateIso !== b.occurrenceDateIso) return a.occurrenceDateIso.localeCompare(b.occurrenceDateIso);
        return (a.sortTime || '').localeCompare(b.sortTime || '');
      });
  }

  function formatMarketValue(price, category, quoteSymbol) {
    if (price == null || Number.isNaN(Number(price))) return '--';
    const num = Number(price);
    if (category === 'INDEX') {
      return `${num.toLocaleString('pt-BR', { minimumFractionDigits: 0, maximumFractionDigits: 2 })} pts`;
    }
    if (quoteSymbol === 'USD') {
      return `US$ ${num.toLocaleString('pt-BR', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
    }
    const maxDec = category === 'CURRENCY' ? 4 : num < 100 ? 2 : 2;
    const minDec = category === 'CURRENCY' ? 4 : 2;
    return num.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL', minimumFractionDigits: minDec, maximumFractionDigits: maxDec });
  }

  function getAssetSeries(state, assetCode, range = '1M') {
    const q = state.marketQuotes[assetCode];
    if (!q) return [];
    const base = q.price;
    const rangeMap = { '1D': [18, 0.012, 1], '5D': [22, 0.024, 3], '1M': [24, 0.048, 5], '6M': [26, 0.095, 8], '1A': [30, 0.14, 11] };
    const [cnt, vol, seed] = rangeMap[range] || rangeMap['1M'];
    return generateMarketSeries(base, cnt, vol, seed + assetCode.length);
  }

  // Contagem exata das 27 tabelas do Room (DiagnosticsScreen.kt / NexusDatabase.kt)
  function computeDatabaseTableCounts(state) {
    const subsCount = state.tasks.reduce((a, t) => a + (t.subtasks ? t.subtasks.length : 0), 0);
    const msCount = state.personalGoals.reduce((a, g) => a + (g.milestones ? g.milestones.length : 0), 0);
    const logsCount = state.personalGoals.reduce((a, g) => a + (g.progressLogs ? g.progressLogs.length : 0), 0);
    const recTaskCount = state.tasks.filter((t) => Boolean(t.recurrence)).length;
    return [
      { table: 'settings', count: 1 },
      { table: 'personalization_settings', count: 1 },
      { table: 'dashboard_blocks', count: state.blocks.length },
      { table: 'quotes', count: state.quotes.length },
      { table: 'categories', count: state.categories.length },
      { table: 'projects', count: state.projects.length },
      { table: 'tasks', count: state.tasks.length },
      { table: 'subtasks', count: subsCount },
      { table: 'task_recurrences', count: recTaskCount },
      { table: 'recurrence_exceptions', count: 0 },
      { table: 'appointments', count: state.appointments.length },
      { table: 'events', count: state.events.length },
      { table: 'transactions', count: state.transactions.length },
      { table: 'credit_cards', count: state.creditCards.length },
      { table: 'credit_card_statements', count: state.statements.length },
      { table: 'installment_purchases', count: state.installmentPurchases.length },
      { table: 'recurring_rules', count: state.recurringRules.length },
      { table: 'budgets', count: state.budgets.length },
      { table: 'financial_goals', count: state.financialGoals.length },
      { table: 'loans', count: state.loans.length },
      { table: 'personal_goals', count: state.personalGoals.length },
      { table: 'goal_milestones', count: msCount },
      { table: 'goal_progress_logs', count: logsCount },
      { table: 'reminders', count: state.reminders.length },
      { table: 'routine_assistant_settings', count: 1 },
      { table: 'briefing_history', count: state.briefingState.history.length },
      { table: 'audit_history', count: state.auditHistory.length }
    ];
  }

  window.NexusPilot = window.NexusPilot || {};
  window.NexusPilot.Data = {
    MARKET_ASSET_CATALOG,
    createInitialState,
    appendAudit,
    LABELS,
    formatBrl,
    formatDateBr,
    formatDayMonth,
    formatDateByPattern,
    formatWeekdayShort,
    formatMonthTitle,
    formatMonthShort,
    shiftMonthIso,
    addDaysIso,
    getWeekDatesIso,
    parseIsoDate,
    toIsoDate,
    computeTaskCounts,
    computeTaskExpenseCents,
    computeProjectProgress,
    computeGoalDetails,
    computeAppointmentConflicts,
    computeForecast30Days,
    computeFinanceSelectors,
    computeTaskDistribution,
    computeEventDetails,
    computeDailyBriefing,
    computeAgendaOccurrences,
    formatMarketValue,
    getAssetSeries,
    computeDatabaseTableCounts
  };
})();
