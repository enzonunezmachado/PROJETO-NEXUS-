import { useMemo, useState } from 'react';
import JSZip from 'jszip';
import {
  AlertTriangle,
  Check,
  Copy,
  Database,
  Download,
  FileCode2,
  FileJson,
  FolderTree,
  Search,
  ShieldCheck,
} from 'lucide-react';

type InspectorTab = 'files' | 'architecture' | 'backup';

interface ProjectFile {
  path: string;
  content: string;
  lines: number;
}

const rootFiles = import.meta.glob(
  [
    '/.github/workflows/android-build.yml',
    '/NEXUS_ARCHITECTURE_AND_PROGRESS.md',
    '/settings.gradle.kts',
    '/build.gradle.kts',
    '/gradle.properties',
    '/gradle/libs.versions.toml',
    '/gradle/wrapper/gradle-wrapper.properties',
  ],
  { query: '?raw', import: 'default', eager: true }
) as Record<string, string>;

const appFiles = import.meta.glob('/app/**/*', {
  query: '?raw',
  import: 'default',
  eager: true,
}) as Record<string, string>;

const ALL_PROJECT_FILES: ProjectFile[] = Object.entries({ ...rootFiles, ...appFiles })
  .map(([rawPath, content]) => {
    const cleanPath = rawPath.startsWith('/') ? rawPath.slice(1) : rawPath;
    const text = typeof content === 'string' ? content : '';
    return {
      path: cleanPath,
      content: text,
      lines: text.split('\n').length,
    };
  })
  .sort((a, b) => a.path.localeCompare(b.path));

const SAMPLE_BACKUP_TEMPLATE = JSON.stringify(
  {
    formatVersion: 4,
    appVersion: '5.0.0',
    exportedAt: new Date().toISOString(),
    databaseVersion: 4,
    settings: [
      {
        id: 'nexus_settings_default',
        userName: 'Enzo',
        darkTheme: true,
        declaredBalanceCents: 0,
        createdAt: Date.now(),
        updatedAt: Date.now(),
      },
    ],
    categories: [],
    projects: [],
    events: [],
    tasks: [],
    subtasks: [],
    appointments: [],
    creditCards: [],
    creditCardStatements: [],
    installmentPurchases: [],
    recurringRules: [],
    transactions: [],
    loans: [],
    budgets: [],
    financialGoals: [],
    auditHistory: [],
    errorLogs: [],
    reminders: [],
    routineSettings: [
      {
        id: 'nexus_routine_default',
        notificationsEnabled: true,
        scheduleChannelEnabled: true,
        financeChannelEnabled: true,
        eventsChannelEnabled: true,
        routineChannelEnabled: true,
        morningBriefingEnabled: true,
        morningBriefingHour: 8,
        morningBriefingMinute: 0,
        nightReviewEnabled: true,
        nightReviewHour: 21,
        nightReviewMinute: 0,
        quietHoursEnabled: false,
        quietHoursStartHour: 22,
        quietHoursStartMinute: 0,
        quietHoursEndHour: 7,
        quietHoursEndMinute: 0,
        defaultTaskReminderMinutes: 30,
        defaultAppointmentReminderMinutes: 30,
        defaultEventReminderMinutes: 60,
        defaultFinanceReminderMinutes: 0,
        createdAt: Date.now(),
        updatedAt: Date.now(),
      },
    ],
    briefingHistory: [],
    personalizationSettings: [
      {
        id: 'nexus_personalization_default',
        greetingEnabled: true,
        greetingTemplate: 'AUTO',
        dateFormatPattern: 'WEEKDAY_DD_MM_YYYY',
        showDailyQuoteOnDashboard: true,
        compactDashboardMode: false,
        showMetricSummaryOnDashboard: true,
        dashboardBlocksJson: '',
        lastQuoteDateIso: null,
        lastQuoteId: null,
        previousQuoteId: null,
        createdAt: Date.now(),
        updatedAt: Date.now(),
      },
    ],
    quotes: [],
    personalGoals: [],
    goalMilestones: [],
    goalProgressLogs: [],
  },
  null,
  2
);

interface ParsedBackupSummary {
  valid: boolean;
  error?: string;
  formatVersion?: number;
  appVersion?: string;
  exportedAt?: string;
  counts?: Record<string, number>;
  unknownKeys?: string[];
}

function validateBackupJsonLikeAndroid(raw: string): ParsedBackupSummary {
  if (!raw.trim()) {
    return { valid: false, error: 'O JSON está vazio.' };
  }
  try {
    const parsed = JSON.parse(raw) as unknown;
    if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) {
      return { valid: false, error: 'O conteúdo raiz não é um objeto JSON válido.' };
    }
    const obj = parsed as Record<string, unknown>;
    const tableKeys = [
      'settings',
      'categories',
      'projects',
      'events',
      'tasks',
      'subtasks',
      'appointments',
      'creditCards',
      'creditCardStatements',
      'installmentPurchases',
      'recurringRules',
      'transactions',
      'loans',
      'budgets',
      'financialGoals',
      'auditHistory',
      'errorLogs',
      'reminders',
      'routineSettings',
      'briefingHistory',
      'personalizationSettings',
      'quotes',
      'personalGoals',
      'goalMilestones',
      'goalProgressLogs',
    ];
    const recognized = new Set([
      'formatVersion',
      'appVersion',
      'exportedAt',
      'databaseVersion',
      ...tableKeys,
    ]);
    const keys = Object.keys(obj);
    if (!keys.some((k) => recognized.has(k))) {
      return { valid: false, error: 'Este JSON não contém tabelas reconhecidas do NEXUS.' };
    }
    const formatVersion = typeof obj.formatVersion === 'number' ? obj.formatVersion : 1;
    if (formatVersion < 1) {
      return { valid: false, error: `Versão de formato inválida: ${formatVersion}` };
    }
    const countArr = (val: unknown) => (Array.isArray(val) ? val.length : 0);
    const counts: Record<string, number> = {};
    for (const k of tableKeys) {
      counts[k] = countArr(obj[k]);
    }
    return {
      valid: true,
      formatVersion,
      appVersion: typeof obj.appVersion === 'string' && obj.appVersion ? obj.appVersion : 'Desconhecida',
      exportedAt: typeof obj.exportedAt === 'string' && obj.exportedAt ? obj.exportedAt : 'Não informada',
      counts,
      unknownKeys: keys.filter((k) => !recognized.has(k)),
    };
  } catch (err) {
    return {
      valid: false,
      error: `JSON malformado: ${err instanceof Error ? err.message : 'erro desconhecido'}`,
    };
  }
}

const ROOM_TABLES = [
  { table: 'settings', pk: 'id (TEXT PK)', notes: 'Nome do usuário ("Enzo"), tema escuro/claro e saldo informado.' },
  { table: 'categories', pk: 'id (UUID TEXT PK)', notes: 'Categorias editáveis compartilhadas entre tarefas, agenda, eventos e finanças.' },
  { table: 'projects', pk: 'id (UUID TEXT PK)', notes: 'Projetos com prazo, status, progresso automático, custo agregado e FK goalId SET_NULL.' },
  { table: 'events', pk: 'id (UUID TEXT PK)', notes: 'Eventos completos com local, links Maps/Rota e ingresso parcelado ligado ao financeiro.' },
  { table: 'tasks', pk: 'id (UUID TEXT PK)', notes: 'Tarefas com FK SET_NULL para projects, categories, events e personal_goals (goalId).' },
  { table: 'subtasks', pk: 'id (UUID TEXT PK)', notes: 'Subtarefas com FK CASCADE para tasks(id) e ordenação.' },
  { table: 'appointments', pk: 'id (UUID TEXT PK)', notes: 'Compromissos com início/término, dia inteiro, local, link e detecção de conflito.' },
  { table: 'credit_cards', pk: 'id (UUID TEXT PK)', notes: 'Cartões de crédito com limite calculado em tempo real, fechamento e vencimento.' },
  { table: 'credit_card_statements', pk: 'id (UUID TEXT PK)', notes: 'Faturas mensais (OPEN/CLOSED/OVERDUE/PAID); quitação sem duplicar despesa.' },
  { table: 'installment_purchases', pk: 'id (UUID TEXT PK)', notes: 'Compras parceladas com geração automática das parcelas e exclusão segura.' },
  { table: 'recurring_rules', pk: 'id (UUID TEXT PK)', notes: 'Regras recorrentes com geração idempotente (OnConflict IGNORE) e preservação de histórico.' },
  { table: 'transactions', pk: 'id (UUID TEXT PK)', notes: 'Receitas e despesas (previsto/pendente/realizado/atrasado) + índice único de recorrência.' },
  { table: 'loans', pk: 'id (UUID TEXT PK)', notes: 'Empréstimos e dívidas com saldo devedor atualizado pelo usuário.' },
  { table: 'budgets', pk: 'id (UUID TEXT PK)', notes: 'Limites mensais por categoria com índice único (categoryId, yearMonth).' },
  { table: 'financial_goals', pk: 'id (UUID TEXT PK)', notes: 'Metas financeiras com acompanhamento de valor acumulado.' },
  { table: 'audit_history', pk: 'id (UUID TEXT PK)', notes: 'Histórico de ações críticas financeiras, faturas, rotina e ingressos.' },
  { table: 'error_logs', pk: 'id (UUID TEXT PK)', notes: 'Captura exceções de banco e handler global; exibe últimos 50 em Diagnóstico.' },
  { table: 'reminders', pk: 'id (UUID TEXT PK)', notes: 'Fila persistente de lembretes nativos (AlarmManager/WorkManager), snooze e status.' },
  { table: 'routine_assistant_settings', pk: 'id (TEXT PK)', notes: 'Configurações de canais, modo silêncio, horários de briefing/revisão e antecedências.' },
  { table: 'briefing_history', pk: 'id (UUID TEXT PK)', notes: 'Histórico diário de briefings matinais e revisões noturnas marcados pelo usuário.' },
  { table: 'personalization_settings', pk: 'id (TEXT PK)', notes: 'Saudação, formato de data, modo compacto e blocos modulares ordenáveis do Dashboard.' },
  { table: 'quotes', pk: 'id (UUID TEXT PK)', notes: 'Biblioteca local de frases (autoria opcional) para rotação diária determinística.' },
  { table: 'personal_goals', pk: 'id (UUID TEXT PK)', notes: 'Objetivos pessoais e planejamento (AUTO/MANUAL) vinculados a projetos e tarefas.' },
  { table: 'goal_milestones', pk: 'id (UUID TEXT PK)', notes: 'Marcos ordenáveis com FK CASCADE para personal_goals(id).' },
  { table: 'goal_progress_logs', pk: 'id (UUID TEXT PK)', notes: 'Histórico cronológico de evolução dos objetivos pessoais com FK CASCADE.' },
];

export default function App() {
  const [activeTab, setActiveTab] = useState<InspectorTab>('files');
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedPath, setSelectedPath] = useState<string>(
    ALL_PROJECT_FILES.find((f) => f.path.endsWith('MainActivity.kt'))?.path ??
      ALL_PROJECT_FILES[0]?.path ??
      ''
  );
  const [copiedPath, setCopiedPath] = useState<string | null>(null);
  const [isExportingZip, setIsExportingZip] = useState(false);
  const [backupInput, setBackupInput] = useState(SAMPLE_BACKUP_TEMPLATE);

  const filteredFiles = useMemo(() => {
    const q = searchQuery.trim().toLowerCase();
    if (!q) return ALL_PROJECT_FILES;
    return ALL_PROJECT_FILES.filter(
      (f) => f.path.toLowerCase().includes(q) || f.content.toLowerCase().includes(q)
    );
  }, [searchQuery]);

  const selectedFile = useMemo(
    () => ALL_PROJECT_FILES.find((f) => f.path === selectedPath) ?? filteredFiles[0] ?? null,
    [selectedPath, filteredFiles]
  );

  const backupCheck = useMemo(() => validateBackupJsonLikeAndroid(backupInput), [backupInput]);

  async function handleDownloadZip() {
    setIsExportingZip(true);
    try {
      const zip = new JSZip();
      for (const file of ALL_PROJECT_FILES) {
        zip.file(file.path, file.content);
      }
      const blob = await zip.generateAsync({ type: 'blob' });
      const url = URL.createObjectURL(blob);
      const anchor = document.createElement('a');
      anchor.href = url;
      anchor.download = 'nexus-android-phase1-a-6.zip';
      document.body.appendChild(anchor);
      anchor.click();
      anchor.remove();
      URL.revokeObjectURL(url);
    } finally {
      setIsExportingZip(false);
    }
  }

  async function handleCopyFile(file: ProjectFile) {
    await navigator.clipboard.writeText(file.content);
    setCopiedPath(file.path);
    setTimeout(() => setCopiedPath(null), 1800);
  }

  return (
    <div className="min-h-screen bg-[#050505] text-[#F2F2F2] flex flex-col">
      <header className="border-b border-[#242424] bg-[#0C0C0C]">
        <div className="max-w-[1440px] mx-auto px-6 py-4 flex items-center justify-between gap-8">
          <span className="text-lg font-semibold tracking-tight text-[#FFFFFF] whitespace-nowrap shrink-0">
            NEXUS
          </span>

          <nav className="flex items-center gap-6 text-sm font-medium">
            <button
              type="button"
              onClick={() => setActiveTab('files')}
              className={`whitespace-nowrap shrink-0 pb-1 border-b-2 transition-colors ${
                activeTab === 'files'
                  ? 'border-[#FFFFFF] text-[#FFFFFF]'
                  : 'border-transparent text-[#9A9A9A] hover:text-[#F2F2F2]'
              }`}
            >
              Arquivos Android
            </button>
            <button
              type="button"
              onClick={() => setActiveTab('architecture')}
              className={`whitespace-nowrap shrink-0 pb-1 border-b-2 transition-colors ${
                activeTab === 'architecture'
                  ? 'border-[#FFFFFF] text-[#FFFFFF]'
                  : 'border-transparent text-[#9A9A9A] hover:text-[#F2F2F2]'
              }`}
            >
              Banco e Fases
            </button>
            <button
              type="button"
              onClick={() => setActiveTab('backup')}
              className={`whitespace-nowrap shrink-0 pb-1 border-b-2 transition-colors ${
                activeTab === 'backup'
                  ? 'border-[#FFFFFF] text-[#FFFFFF]'
                  : 'border-transparent text-[#9A9A9A] hover:text-[#F2F2F2]'
              }`}
            >
              Validador de Backup
            </button>
          </nav>

          <button
            type="button"
            onClick={handleDownloadZip}
            disabled={isExportingZip || ALL_PROJECT_FILES.length === 0}
            className="inline-flex items-center gap-2 px-4 py-2 text-xs font-semibold rounded-lg bg-[#FFFFFF] text-[#050505] hover:bg-[#E5E5E5] disabled:opacity-50 transition-colors whitespace-nowrap shrink-0"
          >
            <Download className="w-3.5 h-3.5" />
            <span>{isExportingZip ? 'Gerando ZIP...' : 'Baixar Projeto (.zip)'}</span>
          </button>
        </div>
      </header>

      <div className="border-b border-[#242424] bg-[#0C0C0C]">
        <div className="max-w-[1440px] mx-auto px-6 py-3 flex items-start gap-3 text-sm">
          <AlertTriangle className="w-4 h-4 text-[#E5A000] shrink-0 mt-0.5" />
          <p className="text-[#9A9A9A] leading-relaxed">
            <span className="text-[#F2F2F2] font-medium">Limitação deste ambiente:</span> este
            sandbox executa apenas Node.js/Web (sem JDK, Gradle, Android SDK ou emulador Android).
            Todo o código-fonte nativo das Fases 1 a 6 em Kotlin, Jetpack Compose, Room (v4),
            WorkManager, AlarmManager e Jetpack Glance (widgets nativos) está disponível para
            inspeção e download em ZIP para compilação no Android Studio.
          </p>
        </div>
      </div>

      <main className="flex-1 max-w-[1440px] w-full mx-auto px-6 py-6">
        {activeTab === 'files' && (
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
            <aside className="lg:col-span-4 border border-[#242424] bg-[#0C0C0C] rounded-xl overflow-hidden">
              <div className="p-4 border-b border-[#242424] flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <FolderTree className="w-4 h-4 text-[#9A9A9A]" />
                  <h2 className="text-sm font-semibold text-[#F2F2F2]">
                    Estrutura Gradle / Kotlin
                  </h2>
                </div>
                <span className="text-xs font-mono tabular-nums text-[#9A9A9A]">
                  {ALL_PROJECT_FILES.length} arquivos
                </span>
              </div>

              <div className="p-3 border-b border-[#242424]">
                <div className="relative">
                  <Search className="w-4 h-4 text-[#9A9A9A] absolute left-3 top-1/2 -translate-y-1/2" />
                  <input
                    type="text"
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                    placeholder="Filtrar arquivo ou símbolo..."
                    className="w-full bg-[#121212] border border-[#242424] rounded-lg pl-9 pr-3 py-2 text-xs text-[#F2F2F2] placeholder:text-[#777777] focus:outline-none focus:border-[#4C8BF5]"
                  />
                </div>
              </div>

              <div className="max-h-[600px] overflow-y-auto divide-y divide-[#181818]">
                {filteredFiles.map((file) => {
                  const active = selectedFile?.path === file.path;
                  return (
                    <button
                      key={file.path}
                      type="button"
                      onClick={() => setSelectedPath(file.path)}
                      className={`w-full text-left px-4 py-2.5 flex items-center justify-between gap-2 transition-colors ${
                        active
                          ? 'bg-[#121212] text-[#FFFFFF]'
                          : 'text-[#9A9A9A] hover:bg-[#101010] hover:text-[#F2F2F2]'
                      }`}
                    >
                      <span className="text-xs font-mono truncate">{file.path}</span>
                      <span className="text-[11px] font-mono tabular-nums text-[#777777] shrink-0">
                        {file.lines}L
                      </span>
                    </button>
                  );
                })}
                {filteredFiles.length === 0 && (
                  <p className="p-4 text-xs text-[#9A9A9A]">
                    Nenhum arquivo corresponde ao filtro.
                  </p>
                )}
              </div>
            </aside>

            <section className="lg:col-span-8 border border-[#242424] bg-[#0C0C0C] rounded-xl overflow-hidden">
              {selectedFile ? (
                <>
                  <div className="px-4 py-3 border-b border-[#242424] flex items-center justify-between gap-4 bg-[#121212]">
                    <div className="flex items-center gap-2 min-w-0">
                      <FileCode2 className="w-4 h-4 text-[#9A9A9A] shrink-0" />
                      <span className="text-xs font-mono text-[#F2F2F2] truncate">
                        {selectedFile.path}
                      </span>
                    </div>
                    <button
                      type="button"
                      onClick={() => handleCopyFile(selectedFile)}
                      className="inline-flex items-center gap-1.5 px-3 py-1.5 text-xs border border-[#242424] rounded-lg bg-[#0C0C0C] text-[#F2F2F2] hover:border-[#4C8BF5] transition-colors whitespace-nowrap shrink-0"
                    >
                      {copiedPath === selectedFile.path ? (
                        <>
                          <Check className="w-3.5 h-3.5 text-[#34C759]" />
                          <span>Copiado</span>
                        </>
                      ) : (
                        <>
                          <Copy className="w-3.5 h-3.5" />
                          <span>Copiar código</span>
                        </>
                      )}
                    </button>
                  </div>
                  <pre className="p-4 text-xs font-mono leading-relaxed text-[#E2E2E2] overflow-x-auto max-h-[660px] overflow-y-auto">
                    <code>{selectedFile.content}</code>
                  </pre>
                </>
              ) : (
                <div className="p-8 text-sm text-[#9A9A9A]">
                  Selecione um arquivo na lista para visualizar.
                </div>
              )}
            </section>
          </div>
        )}

        {activeTab === 'architecture' && (
          <div className="space-y-6">
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              <div className="border border-[#242424] bg-[#0C0C0C] rounded-xl p-5">
                <p className="text-xs text-[#9A9A9A]">Arquitetura</p>
                <p className="text-lg font-semibold text-[#F2F2F2] mt-1">
                  MVVM · Módulo Único · 1 Activity
                </p>
                <p className="text-sm text-[#9A9A9A] mt-2">
                  ViewModel com StateFlow, Repository centralizando integrações das Fases 1 a 6,
                  AlarmManager/WorkManager offline, Jetpack Glance e Jetpack Compose Material 3.
                </p>
              </div>
              <div className="border border-[#242424] bg-[#0C0C0C] rounded-xl p-5">
                <p className="text-xs text-[#9A9A9A]">Persistência e Evolução</p>
                <p className="text-lg font-semibold text-[#F2F2F2] mt-1">
                  Room v4 · MIGRATION_1_2 + 2_3 + 3_4 · 25 Tabelas
                </p>
                <p className="text-sm text-[#9A9A9A] mt-2">
                  Chaves primárias UUID, zero fallback destrutivo, backup JSON v4 compatível com
                  v1/v2/v3 e exportação/importação SAF.
                </p>
              </div>
              <div className="border border-[#242424] bg-[#0C0C0C] rounded-xl p-5">
                <p className="text-xs text-[#9A9A9A]">Privacidade e Rede</p>
                <p className="text-lg font-semibold text-[#F2F2F2] mt-1">
                  100% Offline · Sem Permissão INTERNET
                </p>
                <p className="text-sm text-[#9A9A9A] mt-2">
                  Sem IA, sem backend e sem Firebase. Notificações locais, 4 widgets Glance na tela
                  inicial com app fechado e links externos via Intent.ACTION_VIEW.
                </p>
              </div>
            </div>

            <div className="border border-[#242424] bg-[#0C0C0C] rounded-xl overflow-hidden">
              <div className="px-5 py-4 border-b border-[#242424] flex items-center gap-2">
                <Database className="w-4 h-4 text-[#9A9A9A]" />
                <h2 className="text-base font-semibold text-[#F2F2F2]">
                  Tabelas Room Preservadas (Fases 1 a 6 · 25 tabelas · v4)
                </h2>
              </div>
              <div className="divide-y divide-[#1E1E1E]">
                {ROOM_TABLES.map((row) => (
                  <div
                    key={row.table}
                    className="px-5 py-3.5 grid grid-cols-1 md:grid-cols-12 gap-2 items-center"
                  >
                    <span className="md:col-span-3 text-sm font-mono text-[#FFFFFF]">
                      {row.table}
                    </span>
                    <span className="md:col-span-3 text-xs font-mono text-[#9A9A9A]">{row.pk}</span>
                    <span className="md:col-span-6 text-sm text-[#9A9A9A]">{row.notes}</span>
                  </div>
                ))}
              </div>
            </div>
          </div>
        )}

        {activeTab === 'backup' && (
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
            <div className="lg:col-span-7 border border-[#242424] bg-[#0C0C0C] rounded-xl overflow-hidden">
              <div className="px-4 py-3 border-b border-[#242424] flex items-center justify-between bg-[#121212]">
                <div className="flex items-center gap-2">
                  <FileJson className="w-4 h-4 text-[#9A9A9A]" />
                  <h2 className="text-sm font-semibold text-[#F2F2F2]">
                    Simulador de Validação do Backup JSON (v1, v2, v3 e v4)
                  </h2>
                </div>
                <button
                  type="button"
                  onClick={() => setBackupInput(SAMPLE_BACKUP_TEMPLATE)}
                  className="text-xs text-[#9A9A9A] hover:text-[#FFFFFF] transition-colors"
                >
                  Restaurar modelo v4
                </button>
              </div>
              <textarea
                value={backupInput}
                onChange={(e) => setBackupInput(e.target.value)}
                spellCheck={false}
                rows={20}
                className="w-full p-4 bg-[#0C0C0C] text-xs font-mono text-[#E2E2E2] focus:outline-none resize-y"
              />
            </div>

            <div className="lg:col-span-5 border border-[#242424] bg-[#0C0C0C] rounded-xl p-5 space-y-4">
              <div className="flex items-center gap-2">
                <ShieldCheck
                  className={`w-5 h-5 ${backupCheck.valid ? 'text-[#34C759]' : 'text-[#E5484D]'}`}
                />
                <h3 className="text-base font-semibold text-[#F2F2F2]">
                  {backupCheck.valid ? 'Backup JSON válido' : 'Backup inválido'}
                </h3>
              </div>

              {!backupCheck.valid ? (
                <p className="text-sm text-[#E5484D] leading-relaxed">{backupCheck.error}</p>
              ) : (
                <div className="space-y-3 text-sm">
                  <div className="text-xs text-[#9A9A9A] space-y-1">
                    <p>
                      Formato: <span className="font-mono text-[#F2F2F2]">v{backupCheck.formatVersion}</span>
                    </p>
                    <p>
                      App de origem: <span className="font-mono text-[#F2F2F2]">{backupCheck.appVersion}</span>
                    </p>
                    <p>
                      Exportado em: <span className="font-mono text-[#F2F2F2]">{backupCheck.exportedAt}</span>
                    </p>
                  </div>

                  {backupCheck.unknownKeys && backupCheck.unknownKeys.length > 0 && (
                    <div className="p-3 rounded-lg border border-[#242424] bg-[#121212] text-xs text-[#E5A000]">
                      Campos extras detectados ({backupCheck.unknownKeys.join(', ')}): serão
                      ignorados com segurança por kotlinx.serialization (ignoreUnknownKeys = true).
                    </div>
                  )}

                  <div className="border-t border-[#242424] pt-3 space-y-1.5 font-mono text-xs">
                    {backupCheck.counts &&
                      Object.entries(backupCheck.counts).map(([table, count]) => (
                        <div key={table} className="flex items-center justify-between">
                          <span className="text-[#9A9A9A]">{table}</span>
                          <span className="tabular-nums text-[#F2F2F2]">{count}</span>
                        </div>
                      ))}
                  </div>
                </div>
              )}
            </div>
          </div>
        )}
      </main>
    </div>
  );
}
