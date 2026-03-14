import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Calendar, Activity, CheckCircle, AlertCircle, Clock, Plus, Pause, Play, Trash2, GitBranch, Pencil, Search, X, Copy } from 'lucide-react';
import { PageHeader } from '@/shared/components/PageHeader';
import { Button } from '@/shared/components/Button';
import { Badge } from '@/shared/components/Badge';
import { DataTable, type Column } from '@/shared/components/DataTable';
import { EmptyState } from '@/shared/components/EmptyState';
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
import { useWorkflows, useDeleteWorkflow, type Workflow } from '@/api/workflows.api';
import {
  useHolidayCalendars,
  useDeleteHolidayCalendar,
  type HolidayCalendar,
} from '@/api/holiday-calendars.api';
import { CreateScheduleModal } from '../components/CreateScheduleModal';
import { EditScheduleModal } from '../components/EditScheduleModal';
import { CreateCalendarModal } from '../components/CreateCalendarModal';
import { EditCalendarModal } from '../components/EditCalendarModal';
import { ConfirmDialog } from '@/shared/components/ConfirmDialog';

import { cronToHuman } from '../utils/cronHelpers';
import { RULE_TYPE_COLORS, MARKET_FLAGS, statusColor } from '../utils/constants';

type TabId = 'schedules' | 'workflows' | 'holidays';

export function SchedulerPage() {
  const addToast = useToastStore((s) => s.addToast);
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = useState<TabId>('schedules');
  const [showCreate, setShowCreate] = useState(false);
  const [showCreateCalendar, setShowCreateCalendar] = useState(false);
  const [editingSchedule, setEditingSchedule] = useState<Schedule | null>(null);
  const [editingCalendar, setEditingCalendar] = useState<HolidayCalendar | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<{ type: 'schedule' | 'calendar' | 'workflow'; id: string; name: string } | null>(null);
  const [cloningSchedule, setCloningSchedule] = useState<Schedule | null>(null);
  const [scheduleSearch, setScheduleSearch] = useState('');
  const [scheduleStatus, setScheduleStatus] = useState('');
  const [workflowSearch, setWorkflowSearch] = useState('');
  const [workflowStatus, setWorkflowStatus] = useState('');
  const [calendarSearch, setCalendarSearch] = useState('');
  const { data, isLoading } = useSchedules();
  const { data: stats } = useScheduleStats();
  const deleteSchedule = useDeleteSchedule();
  const pauseSchedule = usePauseSchedule();
  const resumeSchedule = useResumeSchedule();
  const { data: workflowsData, isLoading: workflowsLoading } = useWorkflows();
  const { data: calendarsData, isLoading: calendarsLoading } = useHolidayCalendars();
  const deleteCalendar = useDeleteHolidayCalendar();
  const deleteWorkflow = useDeleteWorkflow();

  const schedules = data?.content ?? [];
  const workflows = workflowsData?.content ?? [];
  const calendars = calendarsData ?? [];

  const filteredSchedules = schedules.filter((s) => {
    const matchesSearch = !scheduleSearch ||
      s.name.toLowerCase().includes(scheduleSearch.toLowerCase()) ||
      (s.description?.toLowerCase().includes(scheduleSearch.toLowerCase()));
    const matchesStatus = !scheduleStatus || s.status === scheduleStatus;
    return matchesSearch && matchesStatus;
  });

  const filteredWorkflows = workflows.filter((w) => {
    const matchesSearch = !workflowSearch ||
      w.name.toLowerCase().includes(workflowSearch.toLowerCase()) ||
      (w.description?.toLowerCase().includes(workflowSearch.toLowerCase()));
    const matchesStatus = !workflowStatus || w.status === workflowStatus;
    return matchesSearch && matchesStatus;
  });

  const filteredCalendars = calendars.filter((c) => {
    const matchesSearch = !calendarSearch ||
      c.name.toLowerCase().includes(calendarSearch.toLowerCase()) ||
      (c.description?.toLowerCase().includes(calendarSearch.toLowerCase())) ||
      c.market.toLowerCase().includes(calendarSearch.toLowerCase());
    return matchesSearch;
  });

  const handleConfirmDelete = async () => {
    if (!deleteTarget) return;
    try {
      if (deleteTarget.type === 'schedule') {
        await deleteSchedule.mutateAsync(deleteTarget.id);
        addToast({ type: 'success', title: 'Schedule deleted' });
      } else if (deleteTarget.type === 'workflow') {
        await deleteWorkflow.mutateAsync(deleteTarget.id);
        addToast({ type: 'success', title: 'Workflow deleted' });
      } else {
        await deleteCalendar.mutateAsync(deleteTarget.id);
        addToast({ type: 'success', title: 'Calendar deleted' });
      }
      setDeleteTarget(null);
    } catch {
      addToast({ type: 'error', title: `Failed to delete ${deleteTarget.type}` });
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
            onClick={(e) => { e.stopPropagation(); setDeleteTarget({ type: 'calendar', id: c.id, name: c.name }); }}
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
      render: (s) => (
        <div>
          <p className="font-medium text-gray-900">{s.name}</p>
          {s.description && (
            <p className="text-xs text-gray-500 truncate max-w-[200px] mt-0.5">{s.description}</p>
          )}
        </div>
      ),
    },
    {
      key: 'target',
      header: 'Target',
      render: (s) => {
        if (s.workflowId) {
          return (
            <div className="flex items-center gap-1.5">
              <span className="inline-flex items-center rounded-full bg-purple-100 px-2 py-0.5 text-[10px] font-semibold text-purple-700">
                Workflow
              </span>
              <button
                onClick={(e) => { e.stopPropagation(); navigate(`/workflows/${s.workflowId}`); }}
                className="text-xs text-purple-600 hover:text-purple-800 font-medium truncate max-w-[120px]"
              >
                {s.workflowName ?? 'View'}
              </button>
            </div>
          );
        }
        const colors = s.ruleType
          ? RULE_TYPE_COLORS[s.ruleType] ?? { bg: 'bg-gray-100', text: 'text-gray-600' }
          : null;
        return (
          <div className="flex items-center gap-1.5">
            <span className="inline-flex items-center rounded-full bg-blue-100 px-2 py-0.5 text-[10px] font-semibold text-blue-700">
              Rule
            </span>
            {colors && (
              <span className={cn('inline-flex items-center rounded-full px-2 py-0.5 text-[10px] font-semibold', colors.bg, colors.text)}>
                {s.ruleType}
              </span>
            )}
            {s.ruleName && (
              <span className="text-xs text-gray-600 truncate max-w-[120px]">{s.ruleName}</span>
            )}
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
          <button
            onClick={(e) => { e.stopPropagation(); setCloningSchedule(s); }}
            className="rounded p-1 text-gray-400 hover:bg-gray-100 hover:text-blue-600"
            title="Clone"
          >
            <Copy className="h-4 w-4" />
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
            onClick={(e) => { e.stopPropagation(); setDeleteTarget({ type: 'schedule', id: s.id, name: s.name }); }}
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
          <button
            onClick={(e) => { e.stopPropagation(); setDeleteTarget({ type: 'workflow', id: w.id, name: w.name }); }}
            className="rounded p-1 text-gray-400 hover:bg-gray-100 hover:text-red-600"
            title="Delete"
          >
            <Trash2 className="h-4 w-4" />
          </button>
        </div>
      ),
    },
  ];

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
      {stats ? (
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
      ) : (
        <div className="grid grid-cols-4 gap-4 mb-6">
          {[...Array(4)].map((_, i) => (
            <div key={i} className="animate-pulse rounded-lg border border-gray-200 bg-white p-4">
              <div className="flex items-center gap-3">
                <div className="h-8 w-8 rounded bg-gray-200" />
                <div className="space-y-2">
                  <div className="h-6 w-12 rounded bg-gray-200" />
                  <div className="h-3 w-20 rounded bg-gray-200" />
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

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

          {isLoading ? (
            <div className="animate-pulse space-y-3 rounded-lg border border-gray-200 bg-white p-4">
              {[...Array(5)].map((_, i) => (
                <div key={i} className="flex items-center gap-4 px-4 py-3">
                  <div className="h-4 w-32 rounded bg-gray-200" />
                  <div className="h-4 w-24 rounded bg-gray-200" />
                  <div className="h-4 w-20 rounded bg-gray-200" />
                  <div className="h-4 w-16 rounded bg-gray-200" />
                </div>
              ))}
            </div>
          ) : schedules.length === 0 ? (
            <EmptyState
              icon={<Clock className="h-12 w-12" />}
              title="No schedules yet"
              description="Create a schedule to run rules on a cron-based schedule."
              actionLabel="Create Schedule"
              onAction={() => setShowCreate(true)}
            />
          ) : (
            <>
              <div className="flex items-center gap-3 mb-4">
                <div className="relative flex-1 max-w-sm">
                  <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400" />
                  <input
                    type="text"
                    value={scheduleSearch}
                    onChange={(e) => setScheduleSearch(e.target.value)}
                    placeholder="Search schedules..."
                    className="w-full rounded-lg border border-gray-300 pl-9 pr-3 py-2 text-sm focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
                  />
                  {scheduleSearch && (
                    <button onClick={() => setScheduleSearch('')} className="absolute right-3 top-1/2 -translate-y-1/2">
                      <X className="h-4 w-4 text-gray-400 hover:text-gray-600" />
                    </button>
                  )}
                </div>
                <select
                  value={scheduleStatus}
                  onChange={(e) => setScheduleStatus(e.target.value)}
                  className="rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
                >
                  <option value="">All Statuses</option>
                  <option value="ACTIVE">Active</option>
                  <option value="PAUSED">Paused</option>
                </select>
              </div>
              <div className="rounded-lg border border-gray-200 bg-white">
                <DataTable
                  columns={scheduleColumns}
                  data={filteredSchedules}
                  onRowClick={(s) => navigate(`/schedules/${s.id}`)}
                />
              </div>
            </>
          )}

          <CreateScheduleModal
            isOpen={showCreate || !!cloningSchedule}
            onClose={() => { setShowCreate(false); setCloningSchedule(null); }}
            initialData={cloningSchedule ? {
              name: `${cloningSchedule.name} (Copy)`,
              description: cloningSchedule.description,
              ruleId: cloningSchedule.ruleId,
              ruleName: cloningSchedule.ruleName,
              ruleType: cloningSchedule.ruleType,
              workflowId: cloningSchedule.workflowId,
              cronExpression: cloningSchedule.cronExpression,
              timezone: cloningSchedule.timezone,
              inputTemplate: cloningSchedule.inputTemplate,
              holidayCalendarId: cloningSchedule.holidayCalendarId,
              skipOnHoliday: cloningSchedule.skipOnHoliday,
            } : undefined}
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
            <div className="animate-pulse space-y-3 rounded-lg border border-gray-200 bg-white p-4">
              {[...Array(5)].map((_, i) => (
                <div key={i} className="flex items-center gap-4 px-4 py-3">
                  <div className="h-4 w-32 rounded bg-gray-200" />
                  <div className="h-4 w-24 rounded bg-gray-200" />
                  <div className="h-4 w-20 rounded bg-gray-200" />
                  <div className="h-4 w-16 rounded bg-gray-200" />
                </div>
              ))}
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
            <>
              <div className="flex items-center gap-3 mb-4">
                <div className="relative flex-1 max-w-sm">
                  <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400" />
                  <input
                    type="text"
                    value={workflowSearch}
                    onChange={(e) => setWorkflowSearch(e.target.value)}
                    placeholder="Search workflows..."
                    className="w-full rounded-lg border border-gray-300 pl-9 pr-3 py-2 text-sm focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
                  />
                  {workflowSearch && (
                    <button onClick={() => setWorkflowSearch('')} className="absolute right-3 top-1/2 -translate-y-1/2">
                      <X className="h-4 w-4 text-gray-400 hover:text-gray-600" />
                    </button>
                  )}
                </div>
                <select
                  value={workflowStatus}
                  onChange={(e) => setWorkflowStatus(e.target.value)}
                  className="rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
                >
                  <option value="">All Statuses</option>
                  <option value="ACTIVE">Active</option>
                  <option value="DRAFT">Draft</option>
                </select>
              </div>
              <div className="rounded-lg border border-gray-200 bg-white">
                <DataTable
                  columns={workflowColumns}
                  data={filteredWorkflows}
                  onRowClick={(w) => navigate(`/workflows/${w.id}`)}
                />
              </div>
            </>
          )}
        </>
      )}

      <ConfirmDialog
        open={!!deleteTarget}
        onClose={() => setDeleteTarget(null)}
        onConfirm={handleConfirmDelete}
        title={`Delete ${deleteTarget?.type === 'calendar' ? 'Calendar' : deleteTarget?.type === 'workflow' ? 'Workflow' : 'Schedule'}`}
        message={`Are you sure you want to delete "${deleteTarget?.name}"? This action cannot be undone.`}
        isLoading={deleteSchedule.isPending || deleteCalendar.isPending || deleteWorkflow.isPending}
      />

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
            <div className="animate-pulse space-y-3 rounded-lg border border-gray-200 bg-white p-4">
              {[...Array(5)].map((_, i) => (
                <div key={i} className="flex items-center gap-4 px-4 py-3">
                  <div className="h-4 w-32 rounded bg-gray-200" />
                  <div className="h-4 w-24 rounded bg-gray-200" />
                  <div className="h-4 w-20 rounded bg-gray-200" />
                  <div className="h-4 w-16 rounded bg-gray-200" />
                </div>
              ))}
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
            <>
              <div className="flex items-center gap-3 mb-4">
                <div className="relative flex-1 max-w-sm">
                  <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400" />
                  <input
                    type="text"
                    value={calendarSearch}
                    onChange={(e) => setCalendarSearch(e.target.value)}
                    placeholder="Search calendars..."
                    className="w-full rounded-lg border border-gray-300 pl-9 pr-3 py-2 text-sm focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
                  />
                  {calendarSearch && (
                    <button onClick={() => setCalendarSearch('')} className="absolute right-3 top-1/2 -translate-y-1/2">
                      <X className="h-4 w-4 text-gray-400 hover:text-gray-600" />
                    </button>
                  )}
                </div>
              </div>
              <div className="rounded-lg border border-gray-200 bg-white">
                <DataTable
                  columns={calendarColumns}
                  data={filteredCalendars}
                  onRowClick={(c) => navigate(`/holiday-calendars/${c.id}`)}
                />
              </div>
            </>
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
