import { useMemo, useState } from 'react';
import { AlertTriangle } from 'lucide-react';
import { Modal } from '@/shared/components/Modal';
import { Button } from '@/shared/components/Button';
import { Select } from '@/shared/components/Select';
import { useUpdateRule } from '@/api/rules.api';
import { useToastStore } from '@/stores/toast.store';
import type { Rule } from '@/api/types/rule.types';
import type { Folder } from '@/api/types/folder.types';

interface MoveToFolderModalProps {
  rules: Rule[];
  folders: Folder[];
  onClose: () => void;
  onSuccess: () => void;
}

/** "Parent / Child" label for a folder in a flat list linked by parentId. */
function folderPath(folder: Folder, byId: Map<string, Folder>): string {
  const names: string[] = [];
  let current: Folder | undefined = folder;
  const seen = new Set<string>();
  while (current && !seen.has(current.id)) {
    seen.add(current.id);
    names.unshift(current.name);
    current = current.parentId ? byId.get(current.parentId) : undefined;
  }
  return names.join(' / ');
}

export function MoveToFolderModal({ rules, folders, onClose, onSuccess }: MoveToFolderModalProps) {
  const [targetFolderId, setTargetFolderId] = useState('');
  const [isMoving, setIsMoving] = useState(false);
  const addToast = useToastStore((s) => s.addToast);
  const updateRule = useUpdateRule();

  // Folders belong to a space, so every selected rule must share one
  const spaceIds = useMemo(() => new Set(rules.map((r) => r.spaceId)), [rules]);
  const spaceId = spaceIds.size === 1 ? [...spaceIds][0] : null;

  const options = useMemo(() => {
    const byId = new Map(folders.map((f) => [f.id, f]));
    return folders
      .filter((f) => f.spaceId === spaceId)
      .map((f) => ({ value: f.id, label: folderPath(f, byId) }))
      .sort((a, b) => a.label.localeCompare(b.label));
  }, [folders, spaceId]);

  const handleMove = async () => {
    if (!targetFolderId) return;
    setIsMoving(true);

    let successCount = 0;
    let failCount = 0;
    for (const rule of rules) {
      try {
        await updateRule.mutateAsync({ id: rule.id, data: { folderId: targetFolderId } });
        successCount++;
      } catch {
        failCount++;
      }
    }

    setIsMoving(false);
    const folderLabel = options.find((o) => o.value === targetFolderId)?.label ?? 'folder';

    if (successCount > 0) {
      addToast({
        type: 'success',
        title: `${successCount} rule${successCount !== 1 ? 's' : ''} moved to "${folderLabel}"`,
      });
      onSuccess();
      onClose();
    }
    if (failCount > 0) {
      addToast({
        type: 'error',
        title: `${failCount} rule${failCount !== 1 ? 's' : ''} could not be moved`,
        message: 'A rule locked by another user cannot be moved.',
      });
    }
  };

  const ruleLabel = rules.length === 1 ? `"${rules[0].name}"` : `${rules.length} rules`;

  return (
    <Modal
      open
      onClose={onClose}
      title="Move to Folder"
      footer={
        <div className="flex justify-end gap-3">
          <Button variant="outline" onClick={onClose} disabled={isMoving}>
            Cancel
          </Button>
          <Button onClick={handleMove} disabled={!targetFolderId || isMoving || !spaceId}>
            {isMoving ? 'Moving...' : 'Move'}
          </Button>
        </div>
      }
    >
      <div className="space-y-4">
        <p className="text-sm text-gray-600">Move {ruleLabel} to a folder.</p>

        {!spaceId ? (
          <div className="flex items-center gap-2 p-3 rounded-md bg-amber-50 border border-amber-200 text-amber-800 text-sm">
            <AlertTriangle className="h-4 w-4 shrink-0" />
            The selected rules are in different spaces. Move rules from one space at a time.
          </div>
        ) : options.length === 0 ? (
          <div className="flex items-center gap-2 p-3 rounded-md bg-amber-50 border border-amber-200 text-amber-800 text-sm">
            <AlertTriangle className="h-4 w-4 shrink-0" />
            This space has no folders yet. Create one from the folder sidebar first.
          </div>
        ) : (
          <Select
            label="Target Folder"
            value={targetFolderId}
            onChange={(e) => setTargetFolderId(e.target.value)}
            options={options}
            placeholder="Select a folder..."
          />
        )}
      </div>
    </Modal>
  );
}
