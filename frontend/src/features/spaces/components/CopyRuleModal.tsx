import { useState } from 'react';
import { Info } from 'lucide-react';
import { Modal } from '@/shared/components/Modal';
import { Button } from '@/shared/components/Button';
import { Input } from '@/shared/components/Input';
import { Select } from '@/shared/components/Select';
import { useCopyRule } from '@/api/rules.api';
import { useOrgSpaces } from '@/api/org.api';
import { useToastStore } from '@/stores/toast.store';

interface CopyRuleModalProps {
  ruleId: string;
  ruleName: string;
  currentSpaceId: string;
  onClose: () => void;
  onSuccess: () => void;
}

export function CopyRuleModal({
  ruleId,
  ruleName,
  currentSpaceId,
  onClose,
  onSuccess,
}: CopyRuleModalProps) {
  const [newName, setNewName] = useState(`Copy of ${ruleName}`);
  const [targetSpaceId, setTargetSpaceId] = useState(currentSpaceId);
  const [error, setError] = useState<string | null>(null);

  const addToast = useToastStore((s) => s.addToast);
  const copyRule = useCopyRule();
  const { data: spaces } = useOrgSpaces();

  const allSpaces = spaces ?? [];
  const targetSpace = allSpaces.find((s) => s.id === targetSpaceId);

  const handleCopy = () => {
    if (!newName.trim()) return;
    setError(null);

    copyRule.mutate(
      { ruleId, name: newName.trim(), targetSpaceId: targetSpaceId || undefined },
      {
        onSuccess: () => {
          addToast({
            type: 'success',
            title: `Rule copied to "${targetSpace?.name ?? 'selected space'}"`,
          });
          onSuccess();
          onClose();
        },
        onError: (err: unknown) => {
          const message =
            err instanceof Error ? err.message : 'Failed to copy rule. Please try again.';
          setError(message);
        },
      },
    );
  };

  return (
    <Modal
      open
      onClose={onClose}
      title="Copy Rule"
      footer={
        <div className="flex justify-end gap-3">
          <Button variant="outline" onClick={onClose} disabled={copyRule.isPending}>
            Cancel
          </Button>
          <Button
            onClick={handleCopy}
            disabled={!newName.trim() || copyRule.isPending}
            aria-label={`Copy ${ruleName} to selected space`}
          >
            {copyRule.isPending ? 'Copying...' : 'Copy Rule'}
          </Button>
        </div>
      }
    >
      <div className="space-y-4">
        <p className="text-sm text-gray-600">
          Create a copy of <span className="font-semibold">"{ruleName}"</span>.
        </p>

        <Input
          label="New Name"
          value={newName}
          onChange={(e) => setNewName(e.target.value)}
          placeholder="Enter rule name"
          required
        />

        {allSpaces.length > 0 && (
          <Select
            label="Target Space"
            value={targetSpaceId}
            onChange={(e) => setTargetSpaceId(e.target.value)}
            options={allSpaces.map((s) => ({
              value: s.id,
              label: s.id === currentSpaceId ? `${s.name} (current)` : s.name,
            }))}
          />
        )}

        <div className="flex items-start gap-2 p-3 rounded-md bg-blue-50 border border-blue-200 text-blue-800 text-sm">
          <Info className="h-4 w-4 shrink-0 mt-0.5" />
          <span>The original rule will not be changed.</span>
        </div>

        {error && (
          <p className="text-sm text-red-600 bg-red-50 border border-red-200 rounded-md p-3">
            {error}
          </p>
        )}
      </div>
    </Modal>
  );
}
