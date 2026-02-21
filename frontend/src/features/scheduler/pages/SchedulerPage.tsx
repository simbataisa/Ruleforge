import { useState } from 'react';
import { Clock, Plus, Pause, Play, Trash2 } from 'lucide-react';
import { PageHeader } from '@/shared/components/PageHeader';
import { Button } from '@/shared/components/Button';
import { Badge } from '@/shared/components/Badge';
import { DataTable, type Column } from '@/shared/components/DataTable';
import { EmptyState } from '@/shared/components/EmptyState';
import { Spinner } from '@/shared/components/Spinner';
import { useToastStore } from '@/stores/toast.store';
import {
  useSchedules,
  useDeleteSchedule,
  usePauseSchedule,
  useResumeSchedule,
  type Schedule,
} from '@/api/schedules.api';
import { CreateScheduleModal } from '../components/CreateScheduleModal';

type BadgeColor = 'gray' | 'green' | 'blue' | 'yellow' | 'red';

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

function formatDate(dateStr?: string): string {
  if (!dateStr) return '-';
  return new Date(dateStr).toLocaleString('en-US', {
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}

export function SchedulerPage() {
  const addToast = useToastStore((s) => s.addToast);
  const [showCreate, setShowCreate] = useState(false);
  const { data, isLoading } = useSchedules();
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
      render: (s) => (
        <div>
          <p className="font-medium text-gray-900">{s.name}</p>
          {s.ruleName && (
            <p className="text-xs text-gray-500">Rule: {s.ruleName}</p>
          )}
        </div>
      ),
    },
    {
      key: 'cron',
      header: 'Cron Expression',
      render: (s) => (
        <code className="rounded bg-gray-100 px-1.5 py-0.5 text-xs">{s.cronExpression}</code>
      ),
    },
    {
      key: 'status',
      header: 'Status',
      render: (s) => <Badge color={statusColor(s.status)}>{s.status}</Badge>,
    },
    {
      key: 'nextRun',
      header: 'Next Run',
      render: (s) => (
        <span className="text-sm text-gray-600">{formatDate(s.nextRunAt)}</span>
      ),
    },
    {
      key: 'lastRun',
      header: 'Last Run',
      render: (s) => (
        <span className="text-sm text-gray-600">{formatDate(s.lastRunAt)}</span>
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

  return (
    <div>
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
          <DataTable columns={columns} data={schedules} />
        </div>
      )}

      <CreateScheduleModal
        isOpen={showCreate}
        onClose={() => setShowCreate(false)}
      />
    </div>
  );
}
