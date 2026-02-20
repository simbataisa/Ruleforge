import { useState } from 'react';
import { Button } from '@/shared/components/Button';
import { Input } from '@/shared/components/Input';
import { Select } from '@/shared/components/Select';
import { Modal } from '@/shared/components/Modal';
import type { RuleType } from '@/api/types/rule.types';
import type { Folder } from '@/api/types/folder.types';

interface CreateRuleModalProps {
  open: boolean;
  onClose: () => void;
  onSubmit: (data: { name: string; type: RuleType; folderId?: string }) => void;
  loading?: boolean;
  folders: Folder[];
}

const ruleTypes: { value: RuleType; label: string }[] = [
  { value: 'TABLE', label: 'Decision Table' },
  { value: 'TREE', label: 'Decision Tree' },
  { value: 'SCRIPT', label: 'Script (GraalVM JS)' },
  { value: 'FLOW', label: 'Flow (DAG)' },
];

export function CreateRuleModal({
  open,
  onClose,
  onSubmit,
  loading,
  folders,
}: CreateRuleModalProps) {
  const [name, setName] = useState('');
  const [type, setType] = useState<RuleType>('TABLE');
  const [folderId, setFolderId] = useState('');

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim()) return;
    onSubmit({
      name: name.trim(),
      type,
      folderId: folderId || undefined,
    });
  };

  const handleClose = () => {
    setName('');
    setType('TABLE');
    setFolderId('');
    onClose();
  };

  return (
    <Modal open={open} onClose={handleClose} title="Create New Rule">
      <form onSubmit={handleSubmit} className="space-y-4">
        <div>
          <label
            htmlFor="rule-name"
            className="block text-sm font-medium text-gray-700 mb-1"
          >
            Name
          </label>
          <Input
            id="rule-name"
            value={name}
            onChange={(e) => setName(e.target.value)}
            placeholder="Enter rule name"
            autoFocus
            required
          />
        </div>
        <div>
          <label
            htmlFor="rule-type"
            className="block text-sm font-medium text-gray-700 mb-1"
          >
            Type
          </label>
          <Select
            id="rule-type"
            value={type}
            onChange={(e) => setType(e.target.value as RuleType)}
          >
            {ruleTypes.map((rt) => (
              <option key={rt.value} value={rt.value}>
                {rt.label}
              </option>
            ))}
          </Select>
        </div>
        <div>
          <label
            htmlFor="rule-folder"
            className="block text-sm font-medium text-gray-700 mb-1"
          >
            Folder (optional)
          </label>
          <Select
            id="rule-folder"
            value={folderId}
            onChange={(e) => setFolderId(e.target.value)}
          >
            <option value="">No folder</option>
            {folders.map((f) => (
              <option key={f.id} value={f.id}>
                {f.name}
              </option>
            ))}
          </Select>
        </div>
        <div className="flex justify-end gap-3 pt-2">
          <Button type="button" variant="outline" onClick={handleClose}>
            Cancel
          </Button>
          <Button type="submit" loading={loading} disabled={!name.trim()}>
            Create Rule
          </Button>
        </div>
      </form>
    </Modal>
  );
}
