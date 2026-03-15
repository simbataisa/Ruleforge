import { useState } from 'react';
import { AlertTriangle } from 'lucide-react';
import { Modal } from '@/shared/components/Modal';
import { Button } from '@/shared/components/Button';
import { Select } from '@/shared/components/Select';
import { useMoveRule } from '@/api/rules.api';
import { useOrgSpaces } from '@/api/org.api';
import { useToastStore } from '@/stores/toast.store';
import type { Rule } from '@/api/types/rule.types';

interface AssignToSpaceModalProps {
  rules: Rule[];
  onClose: () => void;
  onSuccess: () => void;
}

export function AssignToSpaceModal({ rules, onClose, onSuccess }: AssignToSpaceModalProps) {
  const [targetSpaceId, setTargetSpaceId] = useState('');
  const [isAssigning, setIsAssigning] = useState(false);

  const addToast = useToastStore((s) => s.addToast);
  const moveRule = useMoveRule();
  const { data: spaces } = useOrgSpaces();

  const availableSpaces = spaces ?? [];

  const handleAssign = async () => {
    if (!targetSpaceId) return;
    setIsAssigning(true);

    let successCount = 0;
    let failCount = 0;

    for (const rule of rules) {
      try {
        await moveRule.mutateAsync({ ruleId: rule.id, targetSpaceId });
        successCount++;
      } catch {
        failCount++;
      }
    }

    setIsAssigning(false);

    const spaceName = availableSpaces.find((s) => s.id === targetSpaceId)?.name ?? 'selected space';

    if (successCount > 0) {
      addToast({
        type: 'success',
        title: `${successCount} rule${successCount !== 1 ? 's' : ''} assigned to "${spaceName}"`,
      });
      onSuccess();
      onClose();
    }
    if (failCount > 0) {
      addToast({
        type: 'error',
        title: `${failCount} rule${failCount !== 1 ? 's' : ''} failed to assign`,
      });
    }
  };

  const ruleLabel = rules.length === 1 ? `"${rules[0].name}"` : `${rules.length} rules`;

  return (
    <Modal
      open
      onClose={onClose}
      title="Assign to Space"
      footer={
        <div className="flex justify-end gap-3">
          <Button variant="outline" onClick={onClose} disabled={isAssigning}>
            Cancel
          </Button>
          <Button
            onClick={handleAssign}
            disabled={!targetSpaceId || isAssigning || availableSpaces.length === 0}
          >
            {isAssigning ? 'Assigning...' : 'Assign'}
          </Button>
        </div>
      }
    >
      <div className="space-y-4">
        <p className="text-sm text-gray-600">
          Assign {ruleLabel} to a space.
        </p>

        {availableSpaces.length === 0 ? (
          <div className="flex items-center gap-2 p-3 rounded-md bg-amber-50 border border-amber-200 text-amber-800 text-sm">
            <AlertTriangle className="h-4 w-4 shrink-0" />
            No spaces available.
          </div>
        ) : (
          <Select
            label="Target Space"
            value={targetSpaceId}
            onChange={(e) => setTargetSpaceId(e.target.value)}
            options={availableSpaces.map((s) => ({ value: s.id, label: s.name }))}
            placeholder="Select a space..."
          />
        )}

        {targetSpaceId && (
          <div className="flex items-start gap-2 p-3 rounded-md bg-amber-50 border border-amber-200 text-amber-800 text-sm">
            <AlertTriangle className="h-4 w-4 shrink-0 mt-0.5" />
            <span>
              The rule{rules.length !== 1 ? 's' : ''} will be moved from{' '}
              {rules.length !== 1 ? 'their' : 'its'} current space.
            </span>
          </div>
        )}
      </div>
    </Modal>
  );
}
