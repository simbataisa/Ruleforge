import { useState, useEffect } from 'react';
import { Modal } from '@/shared/components/Modal';
import { Button } from '@/shared/components/Button';
import { Input } from '@/shared/components/Input';
import { useToastStore } from '@/stores/toast.store';
import { useUpdateSchedule, type Schedule } from '@/api/schedules.api';
import { useRules } from '@/api/rules.api';
import { useWorkflows } from '@/api/workflows.api';
import { useHolidayCalendars, type SkipOnHoliday } from '@/api/holiday-calendars.api';
import { JsonModelEditor } from '@/features/rules/components/JsonModelEditor';
import { cn } from '@/shared/utils/cn';

interface EditScheduleModalProps {
  isOpen: boolean;
  onClose: () => void;
  schedule: Schedule;
}

import { CRON_PRESETS, cronToHuman, validateCron } from '../utils/cronHelpers';
import { TimezoneSelect } from '@/shared/components/TimezoneSelect';

type TargetType = 'rule' | 'workflow';

export function EditScheduleModal({ isOpen, onClose, schedule }: EditScheduleModalProps) {
  const addToast = useToastStore((s) => s.addToast);
  const updateSchedule = useUpdateSchedule();
  const { data: rulesData } = useRules({ size: 100 });
  const { data: workflowsData } = useWorkflows();

  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [targetType, setTargetType] = useState<TargetType>('rule');
  const [ruleId, setRuleId] = useState('');
  const [ruleName, setRuleName] = useState('');
  const [ruleType, setRuleType] = useState('');
  const [workflowId, setWorkflowId] = useState('');
  const [cronExpression, setCronExpression] = useState('');
  const [timezone, setTimezone] = useState('UTC');
  const [inputTemplate, setInputTemplate] = useState('{\n  \n}');
  const [holidayCalendarId, setHolidayCalendarId] = useState('');
  const [skipOnHoliday, setSkipOnHoliday] = useState<SkipOnHoliday>('SKIP');

  const { data: calendarsData } = useHolidayCalendars();
  const calendars = calendarsData ?? [];
  const rules = rulesData?.content ?? [];
  const workflows = workflowsData?.content ?? [];

  // Pre-populate form when schedule changes
  useEffect(() => {
    if (schedule && isOpen) {
      setName(schedule.name);
      setDescription(schedule.description ?? '');
      setRuleId(schedule.ruleId ?? '');
      setRuleName(schedule.ruleName ?? '');
      setRuleType(schedule.ruleType ?? '');
      setWorkflowId(schedule.workflowId ?? '');
      setCronExpression(schedule.cronExpression);
      setTimezone(schedule.timezone ?? 'UTC');
      setInputTemplate(
        schedule.inputTemplate
          ? JSON.stringify(schedule.inputTemplate, null, 2)
          : '{\n  \n}',
      );
      setTargetType(schedule.workflowId ? 'workflow' : 'rule');
      setHolidayCalendarId(schedule.holidayCalendarId ?? '');
      setSkipOnHoliday((schedule.skipOnHoliday as SkipOnHoliday) ?? 'SKIP');
    }
  }, [schedule, isOpen]);

  const handleRuleChange = (selectedId: string) => {
    setRuleId(selectedId);
    const rule = rules.find((r) => r.id === selectedId);
    if (rule) {
      setRuleName(rule.name);
      setRuleType(rule.type);
    } else {
      setRuleName('');
      setRuleType('');
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim() || !cronExpression.trim()) return;
    if (targetType === 'rule' && !ruleId) return;
    if (targetType === 'workflow' && !workflowId) return;

    const cronError = validateCron(cronExpression);
    if (cronError) {
      addToast({ type: 'error', title: cronError });
      return;
    }

    let parsedPayload: Record<string, unknown> | undefined;
    try {
      const trimmed = inputTemplate.trim();
      if (trimmed && trimmed !== '{}' && trimmed !== '{\n  \n}') {
        parsedPayload = JSON.parse(trimmed);
      }
    } catch {
      addToast({ type: 'error', title: 'Invalid JSON in input template' });
      return;
    }

    try {
      await updateSchedule.mutateAsync({
        id: schedule.id,
        data: {
          name: name.trim(),
          description: description.trim() || undefined,
          ruleId: targetType === 'rule' ? ruleId : undefined,
          ruleName: targetType === 'rule' ? ruleName || undefined : undefined,
          ruleType: targetType === 'rule' ? ruleType || undefined : undefined,
          workflowId: targetType === 'workflow' ? workflowId : undefined,
          cronExpression: cronExpression.trim(),
          timezone,
          inputTemplate: parsedPayload,
          holidayCalendarId: holidayCalendarId || undefined,
          skipOnHoliday: holidayCalendarId ? skipOnHoliday : undefined,
        },
      });
      addToast({ type: 'success', title: 'Schedule updated successfully' });
      onClose();
    } catch {
      addToast({ type: 'error', title: 'Failed to update schedule' });
    }
  };

  return (
    <Modal open={isOpen} onClose={onClose} title="Edit Schedule">
      <form onSubmit={handleSubmit} className="space-y-4">
        <div>
          <label htmlFor="edit-sched-name" className="block text-sm font-medium text-gray-700 mb-1">
            Schedule Name
          </label>
          <Input
            id="edit-sched-name"
            value={name}
            onChange={(e) => setName(e.target.value)}
            placeholder="e.g. Nightly pricing recalculation"
            autoFocus
            required
          />
        </div>

        <div>
          <label htmlFor="edit-sched-desc" className="block text-sm font-medium text-gray-700 mb-1">
            Description
          </label>
          <textarea
            id="edit-sched-desc"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            placeholder="Optional description..."
            rows={2}
            className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm text-gray-900 focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
          />
        </div>

        {/* Target type toggle */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-2">Target</label>
          <div className="flex gap-4">
            {(['rule', 'workflow'] as const).map((type) => (
              <label key={type} className="flex items-center gap-2 cursor-pointer">
                <input
                  type="radio"
                  name="targetType"
                  value={type}
                  checked={targetType === type}
                  onChange={() => setTargetType(type)}
                  className="h-4 w-4 text-indigo-600 border-gray-300 focus:ring-indigo-500"
                />
                <span className="text-sm text-gray-700 capitalize">{type}</span>
              </label>
            ))}
          </div>
        </div>

        {targetType === 'rule' ? (
          <div>
            <label htmlFor="edit-sched-rule" className="block text-sm font-medium text-gray-700 mb-1">
              Rule
            </label>
            <select
              id="edit-sched-rule"
              value={ruleId}
              onChange={(e) => handleRuleChange(e.target.value)}
              className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm text-gray-900 focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
              required
            >
              <option value="">Select a rule...</option>
              {rules.map((r) => (
                <option key={r.id} value={r.id}>
                  {r.name} ({r.type})
                </option>
              ))}
            </select>
          </div>
        ) : (
          <div>
            <label htmlFor="edit-sched-workflow" className="block text-sm font-medium text-gray-700 mb-1">
              Workflow
            </label>
            <select
              id="edit-sched-workflow"
              value={workflowId}
              onChange={(e) => setWorkflowId(e.target.value)}
              className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm text-gray-900 focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
              required
            >
              <option value="">Select a workflow...</option>
              {workflows.map((w) => (
                <option key={w.id} value={w.id}>
                  {w.name}
                </option>
              ))}
            </select>
          </div>
        )}

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-2">Frequency</label>
          <div className="flex flex-wrap gap-2 mb-2">
            {CRON_PRESETS.map((preset) => (
              <button
                key={preset.cron}
                type="button"
                onClick={() => setCronExpression(preset.cron)}
                className={cn(
                  'rounded-full px-3 py-1 text-xs font-medium border transition-colors',
                  cronExpression === preset.cron
                    ? 'bg-indigo-100 text-indigo-700 border-indigo-300'
                    : 'bg-white text-gray-600 border-gray-200 hover:bg-gray-50',
                )}
              >
                {preset.label}
              </button>
            ))}
          </div>
          <Input
            value={cronExpression}
            onChange={(e) => setCronExpression(e.target.value)}
            placeholder="Custom cron: minute hour day month weekday"
            required
          />
          {cronExpression && (() => {
            const error = validateCron(cronExpression);
            const human = cronToHuman(cronExpression);
            if (error) return <p className="text-xs text-red-500 mt-1">{error}</p>;
            if (human !== cronExpression) return <p className="text-xs text-indigo-600 mt-1">{human}</p>;
            return <p className="text-xs text-green-600 mt-1">Valid cron expression</p>;
          })()}
        </div>

        <div>
          <label htmlFor="edit-sched-tz" className="block text-sm font-medium text-gray-700 mb-1">
            Timezone
          </label>
          <TimezoneSelect
            id="edit-sched-tz"
            value={timezone}
            onChange={setTimezone}
          />
        </div>

        {calendars.length > 0 && (
          <div>
            <label htmlFor="edit-sched-calendar" className="block text-sm font-medium text-gray-700 mb-1">
              Holiday Calendar
            </label>
            <select
              id="edit-sched-calendar"
              value={holidayCalendarId}
              onChange={(e) => setHolidayCalendarId(e.target.value)}
              className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm text-gray-900 focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
            >
              <option value="">None</option>
              {calendars.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.name} ({c.market})
                </option>
              ))}
            </select>
            {holidayCalendarId && (
              <div className="mt-2">
                <label className="block text-xs font-medium text-gray-600 mb-1.5">
                  On Holiday
                </label>
                <div className="flex gap-4">
                  {([['SKIP', 'Skip'], ['DEFER_NEXT', 'Defer to Next Business Day'], ['RUN_ANYWAY', 'Run Anyway']] as const).map(([value, label]) => (
                    <label key={value} className="flex items-center gap-1.5 cursor-pointer">
                      <input
                        type="radio"
                        name="editSkipOnHoliday"
                        value={value}
                        checked={skipOnHoliday === value}
                        onChange={() => setSkipOnHoliday(value)}
                        className="h-3.5 w-3.5 text-indigo-600 border-gray-300 focus:ring-indigo-500"
                      />
                      <span className="text-xs text-gray-700">{label}</span>
                    </label>
                  ))}
                </div>
              </div>
            )}
          </div>
        )}

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">
            Input Template (JSON)
          </label>
          <div className="rounded border border-gray-200 overflow-hidden">
            <JsonModelEditor
              value={inputTemplate}
              onChange={setInputTemplate}
              height="200px"
            />
          </div>
        </div>

        <div className="flex justify-end gap-3 pt-2">
          <Button type="button" variant="outline" onClick={onClose}>
            Cancel
          </Button>
          <Button
            type="submit"
            loading={updateSchedule.isPending}
            disabled={
              !name.trim() ||
              !cronExpression.trim() ||
              (targetType === 'rule' && !ruleId) ||
              (targetType === 'workflow' && !workflowId)
            }
          >
            Save Changes
          </Button>
        </div>
      </form>
    </Modal>
  );
}
