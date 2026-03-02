import { useState } from 'react';
import { Modal } from '@/shared/components/Modal';
import { Button } from '@/shared/components/Button';
import { Input } from '@/shared/components/Input';
import { useToastStore } from '@/stores/toast.store';
import { useCreateSchedule } from '@/api/schedules.api';
import { useRules } from '@/api/rules.api';
import { useHolidayCalendars, type SkipOnHoliday } from '@/api/holiday-calendars.api';
import { JsonModelEditor } from '@/features/rules/components/JsonModelEditor';
import { cn } from '@/shared/utils/cn';

interface CreateScheduleModalProps {
  isOpen: boolean;
  onClose: () => void;
}

const CRON_PRESETS = [
  { label: 'Every 5 min', cron: '*/5 * * * *' },
  { label: 'Hourly', cron: '0 * * * *' },
  { label: 'Daily', cron: '0 0 * * *' },
  { label: 'Weekly', cron: '0 9 * * 1' },
  { label: 'Monthly', cron: '0 0 1 * *' },
] as const;

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
  return map[cron] ?? '';
}

export function CreateScheduleModal({ isOpen, onClose }: CreateScheduleModalProps) {
  const addToast = useToastStore((s) => s.addToast);
  const createSchedule = useCreateSchedule();
  const { data: rulesData } = useRules({ size: 100 });

  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [ruleId, setRuleId] = useState('');
  const [ruleName, setRuleName] = useState('');
  const [ruleType, setRuleType] = useState('');
  const [cronExpression, setCronExpression] = useState('');
  const [timezone, setTimezone] = useState('UTC');
  const [inputTemplate, setInputTemplate] = useState('{\n  \n}');
  const [holidayCalendarId, setHolidayCalendarId] = useState('');
  const [skipOnHoliday, setSkipOnHoliday] = useState<SkipOnHoliday>('SKIP');

  const { data: calendarsData } = useHolidayCalendars();
  const calendars = calendarsData ?? [];
  const rules = rulesData?.content ?? [];

  const handleClose = () => {
    setName('');
    setDescription('');
    setRuleId('');
    setRuleName('');
    setRuleType('');
    setCronExpression('');
    setTimezone('UTC');
    setInputTemplate('{\n  \n}');
    setHolidayCalendarId('');
    setSkipOnHoliday('SKIP');
    onClose();
  };

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
    if (!name.trim() || !ruleId || !cronExpression.trim()) return;

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
      await createSchedule.mutateAsync({
        name: name.trim(),
        description: description.trim() || undefined,
        ruleId,
        ruleName: ruleName || undefined,
        ruleType: ruleType || undefined,
        cronExpression: cronExpression.trim(),
        timezone,
        inputTemplate: parsedPayload,
        holidayCalendarId: holidayCalendarId || undefined,
        skipOnHoliday: holidayCalendarId ? skipOnHoliday : undefined,
      });
      addToast({ type: 'success', title: 'Schedule created successfully' });
      handleClose();
    } catch {
      addToast({ type: 'error', title: 'Failed to create schedule' });
    }
  };

  const humanCron = cronToHuman(cronExpression);

  return (
    <Modal open={isOpen} onClose={handleClose} title="Create Schedule">
      <form onSubmit={handleSubmit} className="space-y-4">
        <div>
          <label htmlFor="sched-name" className="block text-sm font-medium text-gray-700 mb-1">
            Schedule Name
          </label>
          <Input
            id="sched-name"
            value={name}
            onChange={(e) => setName(e.target.value)}
            placeholder="e.g. Nightly pricing recalculation"
            autoFocus
            required
          />
        </div>

        <div>
          <label htmlFor="sched-desc" className="block text-sm font-medium text-gray-700 mb-1">
            Description
          </label>
          <textarea
            id="sched-desc"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            placeholder="Optional description..."
            rows={2}
            className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm text-gray-900 focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
          />
        </div>

        <div>
          <label htmlFor="sched-rule" className="block text-sm font-medium text-gray-700 mb-1">
            Rule
          </label>
          <select
            id="sched-rule"
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

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-2">
            Frequency
          </label>
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
          {humanCron && (
            <p className="text-xs text-indigo-600 mt-1">{humanCron}</p>
          )}
        </div>

        <div>
          <label htmlFor="sched-tz" className="block text-sm font-medium text-gray-700 mb-1">
            Timezone
          </label>
          <Input
            id="sched-tz"
            value={timezone}
            onChange={(e) => setTimezone(e.target.value)}
            placeholder="UTC"
          />
        </div>

        {calendars.length > 0 && (
          <div>
            <label htmlFor="sched-calendar" className="block text-sm font-medium text-gray-700 mb-1">
              Holiday Calendar
            </label>
            <select
              id="sched-calendar"
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
                        name="skipOnHoliday"
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
          <Button type="button" variant="outline" onClick={handleClose}>
            Cancel
          </Button>
          <Button
            type="submit"
            loading={createSchedule.isPending}
            disabled={!name.trim() || !ruleId || !cronExpression.trim()}
          >
            Create Schedule
          </Button>
        </div>
      </form>
    </Modal>
  );
}
