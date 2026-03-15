import { useState } from 'react';
import { AlertTriangle } from 'lucide-react';
import { Modal } from '@/shared/components/Modal';
import { Button } from '@/shared/components/Button';
import { Select } from '@/shared/components/Select';
import { useMoveRule } from '@/api/rules.api';
import { useOrgSpaces } from '@/api/org.api';
import { useToastStore } from '@/stores/toast.store';

interface MoveRuleModalProps {
  ruleId: string;
  ruleName: string;
  currentSpaceId: string;
  onClose: () => void;
  onSuccess: () => void;
}

export function MoveRuleModal({
  ruleId,
  ruleName,
  currentSpaceId,
  onClose,
  onSuccess,
}: MoveRuleModalProps) {
  const [targetSpaceId, setTargetSpaceId] = useState('');
  const [error, setError] = useState<string | null>(null);

  const addToast = useToastStore((s) => s.addToast);
  const moveRule = useMoveRule();
  const { data: spaces } = useOrgSpaces();

  const availableSpaces = (spaces ?? []).filter((s) => s.id !== currentSpaceId);
  const targetSpace = availableSpaces.find((s) => s.id === targetSpaceId);

  const handleMove = () => {
    if (!targetSpaceId) return;
    setError(null);

    moveRule.mutate(
      { ruleId, targetSpaceId },
      {
        onSuccess: () => {
          addToast({
            type: 'success',
            title: `Rule moved to "${targetSpace?.name ?? 'selected space'}"`,
          });
          onSuccess();
          onClose();
        },
        onError: (err: unknown) => {
          const message =
            err instanceof Error ? err.message : 'Failed to move rule. Please try again.';
          setError(message);
        },
      },
    );
  };

  return (
    <Modal
      open
      onClose={onClose}
      title="Move Rule"
      footer={
        <div className="flex justify-end gap-3">
          <Button variant="outline" onClick={onClose} disabled={moveRule.isPending}>
            Cancel
          </Button>
          <Button
            onClick={handleMove}
            disabled={!targetSpaceId || moveRule.isPending || availableSpaces.length === 0}
            aria-label={`Move ${ruleName} to selected space`}
          >
            {moveRule.isPending ? 'Moving...' : 'Move Rule'}
          </Button>
        </div>
      }
    >
      <div className="space-y-4">
        <p className="text-sm text-gray-600">
          Move <span className="font-semibold">"{ruleName}"</span> to another space.
        </p>

        {availableSpaces.length === 0 ? (
          <div className="flex items-center gap-2 p-3 rounded-md bg-amber-50 border border-amber-200 text-amber-800 text-sm">
            <AlertTriangle className="h-4 w-4 shrink-0" />
            No other spaces available. Create another space first.
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
            <span>The rule will be removed from the current space.</span>
          </div>
        )}

        {error && (
          <p className="text-sm text-red-600 bg-red-50 border border-red-200 rounded-md p-3">
            {error}
          </p>
        )}
      </div>
    </Modal>
  );
}
