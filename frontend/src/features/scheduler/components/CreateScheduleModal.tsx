import { useState } from 'react';
import { Modal } from '@/shared/components/Modal';
import { Button } from '@/shared/components/Button';
import { Input } from '@/shared/components/Input';
import { useToastStore } from '@/stores/toast.store';
import { useCreateSchedule } from '@/api/schedules.api';

interface CreateScheduleModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export function CreateScheduleModal({ isOpen, onClose }: CreateScheduleModalProps) {
  const addToast = useToastStore((s) => s.addToast);
  const createSchedule = useCreateSchedule();

  const [name, setName] = useState('');
  const [ruleId, setRuleId] = useState('');
  const [cronExpression, setCronExpression] = useState('');
  const [timezone, setTimezone] = useState('UTC');

  const handleClose = () => {
    setName('');
    setRuleId('');
    setCronExpression('');
    setTimezone('UTC');
    onClose();
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim() || !ruleId.trim() || !cronExpression.trim()) return;

    try {
      await createSchedule.mutateAsync({
        name: name.trim(),
        ruleId: ruleId.trim(),
        cronExpression: cronExpression.trim(),
        timezone,
      });
      addToast({ type: 'success', title: 'Schedule created successfully' });
      handleClose();
    } catch {
      addToast({ type: 'error', title: 'Failed to create schedule' });
    }
  };

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
          <label htmlFor="sched-rule" className="block text-sm font-medium text-gray-700 mb-1">
            Rule ID
          </label>
          <Input
            id="sched-rule"
            value={ruleId}
            onChange={(e) => setRuleId(e.target.value)}
            placeholder="Enter the rule ID to execute"
            required
          />
        </div>

        <div>
          <label htmlFor="sched-cron" className="block text-sm font-medium text-gray-700 mb-1">
            Cron Expression
          </label>
          <Input
            id="sched-cron"
            value={cronExpression}
            onChange={(e) => setCronExpression(e.target.value)}
            placeholder="e.g. 0 0 * * *"
            required
          />
          <p className="text-xs text-gray-400 mt-1">
            Standard cron format: minute hour day month weekday
          </p>
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

        <div className="flex justify-end gap-3 pt-2">
          <Button type="button" variant="outline" onClick={handleClose}>
            Cancel
          </Button>
          <Button
            type="submit"
            loading={createSchedule.isPending}
            disabled={!name.trim() || !ruleId.trim() || !cronExpression.trim()}
          >
            Create Schedule
          </Button>
        </div>
      </form>
    </Modal>
  );
}
