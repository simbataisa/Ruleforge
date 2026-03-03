import { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeft, Play, Pause, Trash2, Zap, Pencil } from 'lucide-react';
import { Button } from '@/shared/components/Button';
import { Badge } from '@/shared/components/Badge';
import { Spinner } from '@/shared/components/Spinner';
import { EmptyState } from '@/shared/components/EmptyState';
import { useToastStore } from '@/stores/toast.store';
import { cn } from '@/shared/utils/cn';
import { JsonModelEditor } from '@/features/rules/components/JsonModelEditor';
import {
  useSchedule,
  useScheduleRuns,
  useTriggerSchedule,
  usePauseSchedule,
  useResumeSchedule,
  useDeleteSchedule,
  type Schedule,
  type ScheduleRun,
} from '@/api/schedules.api';
import { EditScheduleModal } from '../components/EditScheduleModal';

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

function runStatusColor(status: string): BadgeColor {
  switch (status) {
    case 'SUCCESS':
      return 'green';
    case 'FAILED':
      return 'red';
    case 'RUNNING':
      return 'blue';
    case 'CANCELLED':
      return 'gray';
    default:
      return 'gray';
  }
}

function formatDuration(run: ScheduleRun): string {
  if (!run.completedAt || !run.startedAt) return '-';
  const ms = new Date(run.completedAt).getTime() - new Date(run.startedAt).getTime();
  if (ms < 1000) return `${ms}ms`;
  return `${(ms / 1000).toFixed(1)}s`;
}

function formatDateTime(dateStr: string): string {
  return new Date(dateStr).toLocaleString('en-US', {
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
  });
}

export function ScheduleDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const addToast = useToastStore((s) => s.addToast);
  const [activeTab, setActiveTab] = useState<'history' | 'config'>('history');
  const [showEdit, setShowEdit] = useState(false);

  const { data: schedule, isLoading } = useSchedule(id ?? '');
  const { data: runs } = useScheduleRuns(id ?? '');
  const triggerSchedule = useTriggerSchedule();
  const pauseSchedule = usePauseSchedule();
  const resumeSchedule = useResumeSchedule();
  const deleteSchedule = useDeleteSchedule();

  if (isLoading || !schedule) {
    return (
      <div className="flex items-center justify-center py-20">
        <Spinner size="lg" />
      </div>
    );
  }

  const ruleTypeColors = schedule.ruleType
    ? RULE_TYPE_COLORS[schedule.ruleType] ?? { bg: 'bg-gray-100', text: 'text-gray-600' }
    : null;

  const handleTrigger = async () => {
    try {
      await triggerSchedule.mutateAsync(schedule.id);
      addToast({ type: 'success', title: 'Schedule triggered' });
    } catch {
      addToast({ type: 'error', title: 'Failed to trigger schedule' });
    }
  };

  const handleToggle = async () => {
    try {
      if (schedule.status === 'ACTIVE') {
        await pauseSchedule.mutateAsync(schedule.id);
        addToast({ type: 'success', title: 'Schedule paused' });
      } else {
        await resumeSchedule.mutateAsync(schedule.id);
        addToast({ type: 'success', title: 'Schedule resumed' });
      }
    } catch {
      addToast({ type: 'error', title: 'Failed to update schedule' });
    }
  };

  const handleDelete = async () => {
    if (!confirm(`Delete schedule "${schedule.name}"?`)) return;
    try {
      await deleteSchedule.mutateAsync(schedule.id);
      addToast({ type: 'success', title: 'Schedule deleted' });
      navigate('/schedules');
    } catch {
      addToast({ type: 'error', title: 'Failed to delete schedule' });
    }
  };

  const runList = runs ?? [];

  return (
    <div>
      {/* Back button */}
      <button
        onClick={() => navigate('/schedules')}
        className="mb-4 flex items-center gap-1 text-sm text-gray-500 hover:text-gray-700"
      >
        <ArrowLeft className="h-4 w-4" />
        Back to Schedules
      </button>

      {/* Header */}
      <div className="mb-6">
        <div className="flex items-center gap-3 mb-1">
          <h1 className="text-2xl font-bold text-gray-900">{schedule.name}</h1>
          {ruleTypeColors && (
            <span className={cn('inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-semibold', ruleTypeColors.bg, ruleTypeColors.text)}>
              {schedule.ruleType}
            </span>
          )}
          <span className="text-sm text-gray-500">{cronToHuman(schedule.cronExpression)}</span>
        </div>
        {schedule.description && (
          <p className="text-sm text-gray-600 mt-1">{schedule.description}</p>
        )}
      </div>

      {/* Linked workflow */}
      {schedule.workflowId && (
        <div className="mb-6 rounded-lg border border-purple-200 bg-purple-50 p-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <span className="inline-flex items-center rounded-full bg-purple-100 px-2.5 py-0.5 text-xs font-semibold text-purple-700">
                Workflow
              </span>
              <span className="text-sm font-medium text-gray-900">
                {schedule.workflowName ?? schedule.workflowId}
              </span>
            </div>
            <button
              onClick={() => navigate(`/workflows/${schedule.workflowId}`)}
              className="rounded-lg bg-purple-600 px-3 py-1.5 text-xs font-medium text-white hover:bg-purple-700 transition-colors"
            >
              View Workflow
            </button>
          </div>
          <p className="text-xs text-purple-600 mt-2">
            Cron changes sync to the workflow's trigger node.
          </p>
        </div>
      )}

      {/* Action buttons */}
      <div className="flex items-center gap-3 mb-6">
        <Button onClick={handleTrigger} loading={triggerSchedule.isPending}>
          <Zap className="h-4 w-4" />
          Trigger Now
        </Button>
        <Button variant="outline" onClick={() => setShowEdit(true)}>
          <Pencil className="h-4 w-4" />
          Edit
        </Button>
        {(schedule.status === 'ACTIVE' || schedule.status === 'PAUSED') && (
          <Button variant="outline" onClick={handleToggle}>
            {schedule.status === 'ACTIVE' ? (
              <>
                <Pause className="h-4 w-4" />
                Pause
              </>
            ) : (
              <>
                <Play className="h-4 w-4" />
                Resume
              </>
            )}
          </Button>
        )}
        <Button variant="outline" className="border-red-300 text-red-600 hover:bg-red-50" onClick={handleDelete}>
          <Trash2 className="h-4 w-4" />
          Delete
        </Button>
      </div>

      {/* Tabs */}
      <div className="flex items-center gap-1 border-b border-gray-200 mb-4">
        {(['history', 'config'] as const).map((tab) => (
          <button
            key={tab}
            onClick={() => setActiveTab(tab)}
            className={cn(
              'px-4 py-2 text-sm font-medium transition-colors',
              activeTab === tab
                ? 'text-indigo-600 border-b-2 border-indigo-600'
                : 'text-gray-500 hover:text-gray-700',
            )}
          >
            {tab === 'history' ? 'Execution History' : 'Configuration'}
          </button>
        ))}
      </div>

      {/* Tab content */}
      {activeTab === 'history' ? (
        runList.length === 0 ? (
          <EmptyState
            title="No executions yet"
            description="Trigger this schedule or wait for the next scheduled run."
          />
        ) : (
          <div className="rounded-lg border border-gray-200 bg-white overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead>
                <tr className="border-b border-gray-200">
                  <th className="px-4 py-3 text-xs font-medium uppercase tracking-wider text-gray-500">Status</th>
                  <th className="px-4 py-3 text-xs font-medium uppercase tracking-wider text-gray-500">Started At</th>
                  <th className="px-4 py-3 text-xs font-medium uppercase tracking-wider text-gray-500">Duration</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {runList.map((run) => (
                  <tr key={run.id}>
                    <td className="px-4 py-3">
                      <Badge color={runStatusColor(run.status)}>{run.status}</Badge>
                    </td>
                    <td className="px-4 py-3 text-gray-600">{formatDateTime(run.startedAt)}</td>
                    <td className="px-4 py-3 text-gray-600">{formatDuration(run)}</td>
                    {run.status === 'FAILED' && run.error && (
                      <td colSpan={3} className="px-4 pb-3">
                        <div className="rounded bg-red-50 border border-red-200 p-2 text-xs text-red-700 mt-1">
                          {run.error}
                        </div>
                      </td>
                    )}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )
      ) : (
        <div className="rounded-lg border border-gray-200 bg-white p-6 space-y-6">
          {/* Input Template */}
          <div>
            <h3 className="text-sm font-semibold text-gray-700 mb-2">Input Template</h3>
            {schedule.inputTemplate ? (
              <div className="rounded border border-gray-200 overflow-hidden">
                <JsonModelEditor
                  value={JSON.stringify(schedule.inputTemplate, null, 2)}
                  readOnly
                  height="300px"
                />
              </div>
            ) : (
              <p className="text-sm text-gray-400">No input template configured.</p>
            )}
          </div>

          {/* Details */}
          <div className="grid grid-cols-2 gap-4">
            <div>
              <p className="text-xs font-medium text-gray-500 uppercase tracking-wider">Timezone</p>
              <p className="text-sm text-gray-900 mt-1">{schedule.timezone}</p>
            </div>
            <div>
              <p className="text-xs font-medium text-gray-500 uppercase tracking-wider">Created</p>
              <p className="text-sm text-gray-900 mt-1">{formatDateTime(schedule.createdAt)}</p>
            </div>
            <div>
              <p className="text-xs font-medium text-gray-500 uppercase tracking-wider">Cron Expression</p>
              <code className="text-sm text-gray-900 mt-1">{schedule.cronExpression}</code>
            </div>
            <div>
              <p className="text-xs font-medium text-gray-500 uppercase tracking-wider">Rule ID</p>
              <p className="text-sm text-gray-900 mt-1 font-mono">{schedule.ruleId}</p>
            </div>
            {schedule.holidayCalendarId && (
              <>
                <div>
                  <p className="text-xs font-medium text-gray-500 uppercase tracking-wider">Holiday Calendar</p>
                  <button
                    onClick={() => navigate(`/holiday-calendars/${schedule.holidayCalendarId}`)}
                    className="text-sm text-indigo-600 hover:text-indigo-800 font-medium mt-1"
                  >
                    {schedule.holidayCalendarName ?? schedule.holidayCalendarId}
                  </button>
                </div>
                <div>
                  <p className="text-xs font-medium text-gray-500 uppercase tracking-wider">Holiday Behavior</p>
                  <p className="text-sm text-gray-900 mt-1">
                    {schedule.skipOnHoliday === 'SKIP' && 'Skip execution'}
                    {schedule.skipOnHoliday === 'DEFER_NEXT' && 'Defer to next business day'}
                    {schedule.skipOnHoliday === 'RUN_ANYWAY' && 'Run anyway'}
                    {!schedule.skipOnHoliday && 'Skip execution'}
                  </p>
                </div>
              </>
            )}
          </div>
        </div>
      )}

      {showEdit && (
        <EditScheduleModal
          isOpen={showEdit}
          onClose={() => setShowEdit(false)}
          schedule={schedule as Schedule}
        />
      )}
    </div>
  );
}
