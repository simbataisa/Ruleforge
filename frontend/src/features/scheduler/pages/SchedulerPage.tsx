import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Calendar, Activity, CheckCircle, AlertCircle, Clock, Plus, Pause, Play, Trash2, GitBranch, Pencil } from 'lucide-react';
import { PageHeader } from '@/shared/components/PageHeader';
import { Button } from '@/shared/components/Button';
import { Badge } from '@/shared/components/Badge';
import { DataTable, type Column } from '@/shared/components/DataTable';
import { EmptyState } from '@/shared/components/EmptyState';
import { Spinner } from '@/shared/components/Spinner';
import { useToastStore } from '@/stores/toast.store';
import { cn } from '@/shared/utils/cn';
import { formatRelativeTime } from '@/features/rules/utils/formatRelativeTime';
import {
  useSchedules,
  useScheduleStats,
  useDeleteSchedule,
  usePauseSchedule,
  useResumeSchedule,
  type Schedule,
} from '@/api/schedules.api';
import { useWorkflows, type Workflow } from '@/api/workflows.api';
import {
  useHolidayCalendars,
  useDeleteHolidayCalendar,
  type HolidayCalendar,
} from '@/api/holiday-calendars.api';
import { CreateScheduleModal } from '../components/CreateScheduleModal';
import { EditScheduleModal } from '../components/EditScheduleModal';
import { CreateCalendarModal } from '../components/CreateCalendarModal';
import { EditCalendarModal } from '../components/EditCalendarModal';

type BadgeColor = 'gray' | 'green' | 'blue' | 'yellow' | 'red';

const RULE_TYPE_COLORS: Record<string, { bg: string; text: string }> = {
  TABLE: { bg: 'bg-blue-100', text: 'text-blue-700' },
  TREE: { bg: 'bg-emerald-100', text: 'text-emerald-700' },
  SCRIPT: { bg: 'bg-orange-100', text: 'text-orange-700' },
  FLOW: { bg: 'bg-violet-100', text: 'text-violet-700' },
  SCORECARD: { bg: 'bg-teal-100', text: 'text-teal-700' },
  DMN: { bg: 'bg-indigo-100', text: 'text-indigo-700' },
  ENSEMBLE: { bg: 'bg-rose-100', text: 'text-rose-700' },
  CEP: { bg: 'bg-amber-100', text: 'text-amber-700' },
};

function cronToHuman(cron: string): string {
  const map: Record<string, string> = {
    '*/5 * * * *': 'Every 5 minutes',
    '*/10 * * * *': 'Every 10 minutes',
    '*/15 * * * *': 'Every 15 minutes',
    '*/30 * * * *': 'Every 30 minutes',
    '0 * * * *': 'Every hour',
    '0 */2 * * *': 'Every 2 hours',
    '0 */6 * * *': 'Every 6 hours',
    '0 0 * * *': 'Daily at midnight',
    '0 9 * * *': 'Daily at 9:00 AM',
    '0 9 * * 1': 'Weekly on Monday at 9 AM',
    '0 0 1 * *': 'Monthly on the 1st',
  };
  return map[cron] ?? cron;
}

function statusColor(status: string): BadgeColor {
  switch (status) {
    case 'ACTIVE':
      return 'green';
    case 'PAUSED':
      return 'yellow';
    case 'COMPLETED':
      return 'blue';
    case 'DRAFT':
      return 'gray';
    case 'FAILED':
      return 'red';
    default:
      return 'gray';
  }
}

type TabId = 'schedules' | 'workflows' | 'holidays';

export function SchedulerPage() {
  const addToast = useToastStore((s) => s.addToast);
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = useState<TabId>('schedules');
  const [showCreate, setShowCreate] = useState(false);
  const [showCreateCalendar, setShowCreateCalendar] = useState(false);
  const [editingSchedule, setEditingSchedule] = useState<Schedule | null>(null);
  const [editingCalendar, setEditingCalendar] = useState<HolidayCalendar | null>(null);
  const { data, isLoading } = useSchedules();
  const { data: stats } = useScheduleStats();
  const deleteSchedule = useDeleteSchedule();
  const pauseSchedule = usePauseSchedule();
  const resumeSchedule = useResumeSchedule();
  const { data: workflowsData, isLoading: workflowsLoading } = useWorkflows();
  const { data: calendarsData, isLoading: calendarsLoading } = useHolidayCalendars();
  const deleteCalendar = useDeleteHolidayCalendar();

  const schedules = data?.content ?? [];
  const workflows = workflowsData?.content ?? [];
  const calendars = calendarsData ?? [];

  const handleDelete = async (id: string) => {
    try {
      await deleteSchedule.mutateAsync(id);
      addToast({ type: 'success', title: 'Schedule deleted' });
    } catch {
      addToast({ type: 'error', title: 'Failed to delete schedule' });
    }
  };

  const handlePause = async (id: string) => {
    try {
      await pauseSchedule.mutateAsync(id);
      addToast({ type: 'success', title: 'Schedule paused' });
    } catch {
      addToast({ type: 'error', title: 'Failed to pause schedule' });
    }
  };

  const handleResume = async (id: string) => {
    try {
      await resumeSchedule.mutateAsync(id);
      addToast({ type: 'success', title: 'Schedule resumed' });
    } catch {
      addToast({ type: 'error', title: 'Failed to resume schedule' });
    }
  };

  const handleDeleteCalendar = async (calId: string) => {
    try {
      await deleteCalendar.mutateAsync(calId);
      addToast({ type: 'success', title: 'Calendar deleted' });
    } catch {
      addToast({ type: 'error', title: 'Failed to delete calendar' });
    }
  };

  const calendarColumns: Column<HolidayCalendar>[] = [
    {
      key: 'name',
      header: 'Name',
      render: (c) => (
        <div>
          <p className="font-medium text-gray-900">{c.name}</p>
          {c.description && (
            <p className="text-xs text-gray-500 truncate max-w-[250px]">{c.description}</p>
          )}
        </div>
      ),
    },
    {
      key: 'market',
      header: 'Market',
      render: (c) => {
        const MARKET_FLAGS: Record<string, string> = {
          US: '\u{1F1FA}\u{1F1F8}', UK: '\u{1F1EC}\u{1F1E7}', SG: '\u{1F1F8}\u{1F1EC}', AU: '\u{1F1E6}\u{1F1FA}',
          JP: '\u{1F1EF}\u{1F1F5}', DE: '\u{1F1E9}\u{1F1EA}', IN: '\u{1F1EE}\u{1F1F3}', GLOBAL: '\u{1F30C}',
          GB: '\u{1F1EC}\u{1F1E7}', HK: '\u{1F1ED}\u{1F1F0}', FR: '\u{1F1EB}\u{1F1F7}', CN: '\u{1F1E8}\u{1F1F3}',
          KR: '\u{1F1F0}\u{1F1F7}', CA: '\u{1F1E8}\u{1F1E6}', BR: '\u{1F1E7}\u{1F1F7}',
        };
        const flag = MARKET_FLAGS[c.market.toUpperCase()] ?? '';
        return <span className="text-sm text-gray-900">{flag && <span className="mr-1">{flag}</span>}{c.market}</span>;
      },
    },
    {
      key: 'region',
      header: 'Region',
      render: (c) => <span className="text-sm text-gray-600">{c.region ?? '-'}</span>,
    },
    {
      key: 'entryCount',
      header: 'Holidays',
      render: (c) => <span className="text-sm text-gray-600">{c.entryCount}</span>,
    },
    {
      key: 'weekendDays',
      header: 'Weekend Days',
      render: (c) => (
        <span className="text-xs text-gray-600">
          {(c.weekendDays ?? []).map((d) => d.slice(0, 3)).join(', ')}
        </span>
      ),
    },
    {
      key: 'status',
      header: 'Status',
      render: (c) => (
        <Badge color={c.enabled ? 'green' : 'gray'}>
          {c.enabled ? 'Enabled' : 'Disabled'}
        </Badge>
      ),
    },
    {
      key: 'actions',
      header: 'Actions',
      render: (c) => (
        <div className="flex items-center gap-1">
          <button
            onClick={(e) => { e.stopPropagation(); setEditingCalendar(c); }}
            className="rounded p-1 text-gray-400 hover:bg-gray-100 hover:text-indigo-600"
            title="Edit"
          >
            <Pencil className="h-4 w-4" />
          </button>
          <button
            onClick={(e) => { e.stopPropagation(); handleDeleteCalendar(c.id); }}
            className="rounded p-1 text-gray-400 hover:bg-gray-100 hover:text-red-600"
            title="Delete"
          >
            <Trash2 className="h-4 w-4" />
          </button>
        </div>
      ),
    },
  ];

  const scheduleColumns: Column<Schedule>[] = [
    {
      key: 'name',
      header: 'Name',
      render: (s) => {
        const colors = s.ruleType
          ? RULE_TYPE_COLORS[s.ruleType] ?? { bg: 'bg-gray-100', text: 'text-gray-600' }
          : null;
        return (
          <div>
            <p className="font-medium text-gray-900">{s.name}</p>
            <div className="flex items-center gap-2 mt-0.5">
              {colors && (
                <span className={cn('inline-flex items-center rounded-full px-2 py-0.5 text-[10px] font-semibold', colors.bg, colors.text)}>
                  {s.ruleType}
                </span>
              )}
              {s.description && (
                <p className="text-xs text-gray-500 truncate max-w-[200px]">{s.description}</p>
              )}
            </div>
          </div>
        );
      },
    },
    {
      key: 'frequency',
      header: 'Frequency',
      render: (s) => (
        <div>
          <p className="text-sm text-gray-900">{cronToHuman(s.cronExpression)}</p>
          {cronToHuman(s.cronExpression) !== s.cronExpression && (
            <code className="text-[11px] text-gray-400">{s.cronExpression}</code>
          )}
        </div>
      ),
    },
    {
      key: 'status',
      header: 'Status',
      render: (s) => (
        <div className="flex items-center gap-2">
          <Badge color={statusColor(s.status)}>{s.status}</Badge>
        </div>
      ),
    },
    {
      key: 'lastRun',
      header: 'Last Run',
      render: (s) => (
        <span className="text-sm text-gray-600">
          {s.lastRunAt ? formatRelativeTime(s.lastRunAt) : '-'}
        </span>
      ),
    },
    {
      key: 'nextRun',
      header: 'Next Run',
      render: (s) => (
        <span className="text-sm text-gray-600">
          {s.status === 'PAUSED' ? 'Paused' : s.nextRunAt ? formatRelativeTime(s.nextRunAt) : '-'}
        </span>
      ),
    },
    {
      key: 'actions',
      header: 'Actions',
      render: (s) => (
        <div className="flex items-center gap-1">
          <button
            onClick={(e) => { e.stopPropagation(); setEditingSchedule(s); }}
            className="rounded p-1 text-gray-400 hover:bg-gray-100 hover:text-indigo-600"
            title="Edit"
          >
            <Pencil className="h-4 w-4" />
          </button>
          {s.status === 'ACTIVE' ? (
            <button
              onClick={(e) => { e.stopPropagation(); handlePause(s.id); }}
              className="rounded p-1 text-gray-400 hover:bg-gray-100 hover:text-yellow-600"
              title="Pause"
            >
              <Pause className="h-4 w-4" />
            </button>
          ) : s.status === 'PAUSED' ? (
            <button
              onClick={(e) => { e.stopPropagation(); handleResume(s.id); }}
              className="rounded p-1 text-gray-400 hover:bg-gray-100 hover:text-green-600"
              title="Resume"
            >
              <Play className="h-4 w-4" />
            </button>
          ) : null}
          <button
            onClick={(e) => { e.stopPropagation(); handleDelete(s.id); }}
            className="rounded p-1 text-gray-400 hover:bg-gray-100 hover:text-red-600"
            title="Delete"
          >
            <Trash2 className="h-4 w-4" />
          </button>
        </div>
      ),
    },
  ];

  const workflowColumns: Column<Workflow>[] = [
    {
      key: 'name',
      header: 'Name',
      render: (w) => (
        <div>
          <p className="font-medium text-gray-900">{w.name}</p>
          {w.description && (
            <p className="text-xs text-gray-500 truncate max-w-[300px]">{w.description}</p>
          )}
        </div>
      ),
    },
    {
      key: 'steps',
      header: 'Steps',
      render: (w) => (
        <span className="text-sm text-gray-600">
          {w.definition?.nodes?.length ?? 0} nodes
        </span>
      ),
    },
    {
      key: 'status',
      header: 'Status',
      render: (w) => (
        <Badge color={statusColor(w.status)}>{w.status}</Badge>
      ),
    },
    {
      key: 'updatedAt',
      header: 'Last Updated',
      render: (w) => (
        <span className="text-sm text-gray-600">
          {w.updatedAt ? formatRelativeTime(w.updatedAt) : '-'}
        </span>
      ),
    },
    {
      key: 'actions',
      header: 'Actions',
      render: (w) => (
        <div className="flex items-center gap-1">
          <button
            onClick={(e) => { e.stopPropagation(); navigate(`/workflows/${w.id}`); }}
            className="rounded p-1 text-gray-400 hover:bg-gray-100 hover:text-indigo-600"
            title="Edit"
          >
            <Pencil className="h-4 w-4" />
          </button>
        </div>
      ),
    },
  ];

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-20">
        <Spinner size="lg" />
      </div>
    );
  }

  const statCards = [
    { icon: Calendar, label: 'Total Schedules', value: stats?.total ?? 0, color: 'text-indigo-600' },
    { icon: Activity, label: 'Active', value: stats?.active ?? 0, color: 'text-green-600' },
    { icon: CheckCircle, label: 'Succeeded (24h)', value: stats?.successLast24h ?? 0, color: 'text-emerald-600' },
    { icon: AlertCircle, label: 'Failed (24h)', value: stats?.failedLast24h ?? 0, color: 'text-red-600' },
  ];

  const tabs: { id: TabId; label: string }[] = [
    { id: 'schedules', label: `Schedules (${schedules.length})` },
    { id: 'workflows', label: `Workflows (${workflows.length})` },
    { id: 'holidays', label: `Holidays (${calendars.length})` },
  ];

  return (
    <div>
      {/* Stats bar */}
      <div className="grid grid-cols-4 gap-4 mb-6">
        {statCards.map((card) => (
          <div key={card.label} className="flex items-center gap-3 rounded-lg border border-gray-200 bg-white p-4 shadow-sm">
            <card.icon className={cn('h-8 w-8', card.color)} />
            <div>
              <p className="text-2xl font-bold text-gray-900">{card.value}</p>
              <p className="text-xs text-gray-500">{card.label}</p>
            </div>
          </div>
        ))}
      </div>

      {/* Tab navigation */}
      <div className="border-b border-gray-200 mb-6">
        <nav className="-mb-px flex gap-6">
          {tabs.map((tab) => (
            <button
              key={tab.id}
              onClick={() => setActiveTab(tab.id)}
              className={cn(
                'py-2 px-1 border-b-2 text-sm font-medium transition-colors',
                activeTab === tab.id
                  ? 'border-indigo-500 text-indigo-600'
                  : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300',
              )}
            >
              {tab.label}
            </button>
          ))}
        </nav>
      </div>

      {activeTab === 'schedules' && (
        <>
          <PageHeader
            title="Schedules"
            description="Manage scheduled rule executions"
            actions={
              <Button onClick={() => setShowCreate(true)}>
                <Plus className="h-4 w-4" />
                Create Schedule
              </Button>
            }
          />

          {schedules.length === 0 ? (
            <EmptyState
              icon={<Clock className="h-12 w-12" />}
              title="No schedules yet"
              description="Create a schedule to run rules on a cron-based schedule."
              actionLabel="Create Schedule"
              onAction={() => setShowCreate(true)}
            />
          ) : (
            <div className="rounded-lg border border-gray-200 bg-white">
              <DataTable
                columns={scheduleColumns}
                data={schedules}
                onRowClick={(s) => navigate(`/schedules/${s.id}`)}
              />
            </div>
          )}

          <CreateScheduleModal
            isOpen={showCreate}
            onClose={() => setShowCreate(false)}
          />

          {editingSchedule && (
            <EditScheduleModal
              isOpen={!!editingSchedule}
              onClose={() => setEditingSchedule(null)}
              schedule={editingSchedule}
            />
          )}
        </>
      )}

      {activeTab === 'workflows' && (
        <>
          <PageHeader
            title="Workflows"
            description="Visual rule execution pipelines"
            actions={
              <Button onClick={() => navigate('/workflows/new')}>
                <Plus className="h-4 w-4" />
                Create Workflow
              </Button>
            }
          />

          {workflowsLoading ? (
            <div className="flex items-center justify-center py-12">
              <Spinner size="lg" />
            </div>
          ) : workflows.length === 0 ? (
            <EmptyState
              icon={<GitBranch className="h-12 w-12" />}
              title="No workflows yet"
              description="Create a workflow to chain multiple rules into a visual pipeline."
              actionLabel="Create Workflow"
              onAction={() => navigate('/workflows/new')}
            />
          ) : (
            <div className="rounded-lg border border-gray-200 bg-white">
              <DataTable
                columns={workflowColumns}
                data={workflows}
                onRowClick={(w) => navigate(`/workflows/${w.id}`)}
              />
            </div>
          )}
        </>
      )}

      {activeTab === 'holidays' && (
        <>
          <PageHeader
            title="Holiday Calendars"
            description="Manage market holiday calendars for schedule skip logic"
            actions={
              <Button onClick={() => setShowCreateCalendar(true)}>
                <Plus className="h-4 w-4" />
                Create Calendar
              </Button>
            }
          />

          {calendarsLoading ? (
            <div className="flex items-center justify-center py-12">
              <Spinner size="lg" />
            </div>
          ) : calendars.length === 0 ? (
            <EmptyState
              icon={<Calendar className="h-12 w-12" />}
              title="No holiday calendars yet"
              description="Create a holiday calendar to define market holidays and skip schedules."
              actionLabel="Create Calendar"
              onAction={() => setShowCreateCalendar(true)}
            />
          ) : (
            <div className="rounded-lg border border-gray-200 bg-white">
              <DataTable
                columns={calendarColumns}
                data={calendars}
                onRowClick={(c) => navigate(`/holiday-calendars/${c.id}`)}
              />
            </div>
          )}

          <CreateCalendarModal
            isOpen={showCreateCalendar}
            onClose={() => setShowCreateCalendar(false)}
          />

          {editingCalendar && (
            <EditCalendarModal
              isOpen={!!editingCalendar}
              onClose={() => setEditingCalendar(null)}
              calendar={editingCalendar}
            />
          )}
        </>
      )}
    </div>
  );
}
