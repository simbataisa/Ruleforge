import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Calendar, Activity, CheckCircle, AlertCircle, Clock, Plus, Pause, Play, Trash2 } from 'lucide-react';
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
import { CreateScheduleModal } from '../components/CreateScheduleModal';

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
    case 'FAILED':
      return 'red';
    default:
      return 'gray';
  }
}

export function SchedulerPage() {
  const addToast = useToastStore((s) => s.addToast);
  const navigate = useNavigate();
  const [showCreate, setShowCreate] = useState(false);
  const { data, isLoading } = useSchedules();
  const { data: stats } = useScheduleStats();
  const deleteSchedule = useDeleteSchedule();
  const pauseSchedule = usePauseSchedule();
  const resumeSchedule = useResumeSchedule();

  const schedules = data?.content ?? [];

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

  const columns: Column<Schedule>[] = [
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
            columns={columns}
            data={schedules}
            onRowClick={(s) => navigate(`/schedules/${s.id}`)}
          />
        </div>
      )}

      <CreateScheduleModal
        isOpen={showCreate}
        onClose={() => setShowCreate(false)}
      />
    </div>
  );
}
