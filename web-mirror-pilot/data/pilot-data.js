/*
 * NEXUS Web Mirror Pilot — Relational Mock Store & Domain Calculations
 * Perfil ficticio coerente: Gabriel (mes de referencia: 2026-11).
 * Espelha entidades de NexusEntities.kt, DomainModels.kt e regras basicas de FinanceRules.kt.
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

  function createInitialState() {
    return {
      todayIso: '2026-11-23',
      selectedMonthIso: '2026-11',
      userName: 'Gabriel',
      personalization: {
        greetingEnabled: true,
        greetingTemplate: 'AUTO',
        showDailyQuoteOnDashboard: true,
        compactDashboardMode: false,
        showMetricSummaryOnDashboard: true
      },
      dailyQuote: {
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
        { id: 'cat_trabalho', name: 'Trabalho', colorHex: '#4DE1C1', scope: 'GENERAL' }
      ],
      projects: [
        { id: 'proj_nexus', name: 'Evolução NEXUS 2026' },
        { id: 'proj_sp', name: 'Viagem Técnica SP' }
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
        {
          id: 'stmt_uv_2026_11',
          creditCardId: 'card_uv',
          yearMonth: '2026-11',
          dueDateIso: '2026-11-23',
          status: 'OVERDUE'
        },
        {
          id: 'stmt_uv_2026_12',
          creditCardId: 'card_uv',
          yearMonth: '2026-12',
          dueDateIso: '2026-12-17',
          status: 'OPEN'
        },
        {
          id: 'stmt_itau_2026_12',
          creditCardId: 'card_itau',
          yearMonth: '2026-12',
          dueDateIso: '2026-12-12',
          status: 'OPEN'
        }
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
          status: 'IN_PROGRESS',
          priority: 'HIGH',
          dueDateIso: '2026-11-23',
          dueTime: '14:00',
          projectId: 'proj_sp',
          eventId: 'evt_kotlin_sp',
          subtasksDone: 1,
          subtasksTotal: 2,
          expenseCents: 68000,
          recurrenceLabel: null
        },
        {
          id: 'task_revisar_fatura',
          title: 'Conferir lançamentos da fatura Ultravioleta',
          status: 'NOT_STARTED',
          priority: 'URGENT',
          dueDateIso: '2026-11-21',
          dueTime: null,
          projectId: 'proj_nexus',
          eventId: null,
          subtasksDone: 0,
          subtasksTotal: 3,
          expenseCents: 0,
          recurrenceLabel: 'Mensal'
        },
        {
          id: 'task_relatorio_sprint',
          title: 'Fechar checklist de entregas da semana',
          status: 'NOT_STARTED',
          priority: 'NORMAL',
          dueDateIso: '2026-11-23',
          dueTime: '17:00',
          projectId: 'proj_nexus',
          eventId: null,
          subtasksDone: 2,
          subtasksTotal: 4,
          expenseCents: 0,
          recurrenceLabel: null
        },
        {
          id: 'task_backup_mensal',
          title: 'Exportar snapshot ZIP do NEXUS',
          status: 'COMPLETED',
          priority: 'NORMAL',
          dueDateIso: '2026-11-18',
          dueTime: null,
          projectId: 'proj_nexus',
          eventId: null,
          subtasksDone: 1,
          subtasksTotal: 1,
          expenseCents: 0,
          recurrenceLabel: null
        },
        {
          id: 'task_reserva_hotel',
          title: 'Revisar notas da palestra de abertura',
          status: 'COMPLETED',
          priority: 'LOW',
          dueDateIso: '2026-11-22',
          dueTime: null,
          projectId: 'proj_sp',
          eventId: 'evt_kotlin_sp',
          subtasksDone: 0,
          subtasksTotal: 0,
          expenseCents: 0,
          recurrenceLabel: null
        },
        {
          id: 'task_organizacao_mesa',
          title: 'Organizar arquivos de referência do tablet',
          status: 'NOT_STARTED',
          priority: 'LOW',
          dueDateIso: null,
          dueTime: null,
          projectId: null,
          eventId: null,
          subtasksDone: 0,
          subtasksTotal: 0,
          expenseCents: 0,
          recurrenceLabel: null
        }
      ],
      appointments: [
        {
          id: 'appt_1',
          title: 'Alinhamento arquitetura offline-first',
          dateIso: '2026-11-23',
          startTime: '10:00',
          endTime: '11:00',
          location: 'Sala Meet',
          categoryName: 'Trabalho',
          conflictTitles: ['Revisão de orçamento mensal']
        },
        {
          id: 'appt_2',
          title: 'Revisão de orçamento mensal',
          dateIso: '2026-11-23',
          startTime: '10:30',
          endTime: '11:15',
          location: 'Escritório',
          categoryName: 'Financeiro',
          conflictTitles: ['Alinhamento arquitetura offline-first']
        },
        {
          id: 'appt_3',
          title: 'Check-in Expo Center Norte',
          dateIso: '2026-11-23',
          startTime: '18:00',
          endTime: '18:30',
          location: 'Expo Center Norte',
          categoryName: 'Eventos',
          conflictTitles: []
        },
        {
          id: 'appt_tm_1',
          title: 'Retrospectiva de design UI',
          dateIso: '2026-11-24',
          startTime: '09:30',
          endTime: '10:30',
          location: 'Remoto',
          categoryName: 'Trabalho',
          conflictTitles: []
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
      personalGoals: [
        { id: 'pg_1', title: 'Consolidar UI Multi-dispositivo NEXUS', progressPercent: 72, subtitle: '3 de 4 marcos concluídos' }
      ],
      financialGoals: [
        { id: 'fg_1', name: 'Reserva de emergência (6 meses)', currentAmountCents: 2450000, targetAmountCents: 3000000 }
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
      marketDemoQuotes: [
        { code: 'USD_BRL', label: 'Dólar Comercial', symbol: 'USD/BRL', value: 'R$ 5,74', change: '+0,42%', positive: true },
        { code: 'EUR_BRL', label: 'Euro Comercial', symbol: 'EUR/BRL', value: 'R$ 6,12', change: '-0,18%', positive: false },
        { code: 'BTC_BRL', label: 'Bitcoin', symbol: 'BTC', value: 'R$ 528.400', change: '+2,15%', positive: true }
      ]
    };
  }

  const LABELS = {
    TransactionType: { INCOME: 'Receita', EXPENSE: 'Despesa' },
    FinancialStatus: { PLANNED: 'Previsto', PENDING: 'Pendente', SETTLED: 'Realizado', OVERDUE: 'Atrasado', CANCELLED: 'Cancelado' },
    PaymentMethod: { PIX: 'Pix', DEBIT: 'Débito', CREDIT_CARD: 'Cartão de crédito', CASH: 'Dinheiro', BANK_TRANSFER: 'Transferência', BOLETO: 'Boleto', OTHER: 'Outro' },
    StatementStatus: { OPEN: 'Aberta', CLOSED: 'Fechada', PENDING: 'Pendente', PAID: 'Paga', OVERDUE: 'Atrasada' },
    TicketStatus: { FREE: 'Gratuito', NOT_BOUGHT: 'Não comprado', PENDING: 'Pendente', PARTIALLY_PAID: 'Parcialmente pago', BOUGHT: 'Comprado', PAID: 'Pago', CANCELLED: 'Cancelado' },
    EventLifecycleStatus: { PLANNED: 'Planejado', CONFIRMED: 'Confirmado', ATTENDED: 'Realizado', CANCELLED: 'Cancelado' },
    EventCostType: { TICKET: 'Ingresso', TRANSPORT: 'Transporte', FOOD: 'Alimentação', LODGING: 'Hospedagem', PARKING: 'Estacionamento', SHOPPING: 'Compras', OTHER: 'Outros' },
    TaskStatus: { NOT_STARTED: 'Não iniciada', IN_PROGRESS: 'Em andamento', WAITING: 'Aguardando', BLOCKED: 'Bloqueada', COMPLETED: 'Concluída', CANCELLED: 'Cancelada' },
    ReminderStatus: { ACTIVE: 'Agendado', FIRED: 'Disparado', DISMISSED: 'Dispensado', CANCELLED: 'Cancelado' }
  };

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

  function formatMonthTitle(ym) {
    const [y, m] = ym.split('-').map(Number);
    const months = ['Janeiro', 'Fevereiro', 'Março', 'Abril', 'Maio', 'Junho', 'Julho', 'Agosto', 'Setembro', 'Outubro', 'Novembro', 'Dezembro'];
    return `${months[(m - 1 + 12) % 12]} de ${y}`;
  }

  function formatMonthShort(ym) {
    const [, m] = ym.split('-').map(Number);
    const months = ['Jan', 'Fev', 'Mar', 'Abr', 'Mai', 'Jun', 'Jul', 'Ago', 'Set', 'Out', 'Nov', 'Dez'];
    return months[(m - 1 + 12) % 12];
  }

  function shiftMonthIso(ym, delta) {
    const [y, m] = ym.split('-').map(Number);
    const dt = new Date(y, m - 1 + delta, 1);
    const ny = dt.getFullYear();
    const nm = String(dt.getMonth() + 1).padStart(2, '0');
    return `${ny}-${nm}`;
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
    const tomorrow = '2026-11-24';
    const fin = computeFinanceSelectors(state, '2026-11');
    const todayAppointments = state.appointments.filter((a) => a.dateIso === today);
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

    const tomorrowAppointments = state.appointments.filter((a) => a.dateIso === tomorrow);
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

  window.NexusPilot = window.NexusPilot || {};
  window.NexusPilot.Data = {
    createInitialState,
    LABELS,
    formatBrl,
    formatDateBr,
    formatMonthTitle,
    formatMonthShort,
    shiftMonthIso,
    computeFinanceSelectors,
    computeTaskDistribution,
    computeEventDetails,
    computeDailyBriefing
  };
})();
