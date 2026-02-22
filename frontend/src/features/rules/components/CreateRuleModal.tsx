import { useState, useMemo } from 'react';
import { Button } from '@/shared/components/Button';
import { Input } from '@/shared/components/Input';
import { Select } from '@/shared/components/Select';
import { Modal } from '@/shared/components/Modal';
import type { RuleType } from '@/api/types/rule.types';
import type { Folder } from '@/api/types/folder.types';

interface CreateRuleModalProps {
  open: boolean;
  onClose: () => void;
  onSubmit: (data: { name: string; type: RuleType; description?: string; alias?: string; folderId?: string }) => void;
  loading?: boolean;
  folders: Folder[];
}

const ruleTypes: { value: RuleType; label: string }[] = [
  { value: 'TABLE', label: 'Decision Table' },
  { value: 'TREE', label: 'Decision Tree' },
  { value: 'SCRIPT', label: 'Script (GraalVM JS)' },
  { value: 'FLOW', label: 'Flow (DAG)' },
  { value: 'SCORECARD', label: 'Scorecard' },
  { value: 'DMN', label: 'DMN Table' },
  { value: 'CEP', label: 'Complex Event Processing' },
  { value: 'ENSEMBLE', label: 'Ensemble' },
];

function generateAlias(name: string): string {
  return name
    .toLowerCase()
    .replace(/\s+/g, '-')
    .replace(/[^a-z0-9-]/g, '');
}

export function CreateRuleModal({
  open,
  onClose,
  onSubmit,
  loading,
  folders,
}: CreateRuleModalProps) {
  const [name, setName] = useState('');
  const [type, setType] = useState<RuleType>('TABLE');
  const [description, setDescription] = useState('');
  const [folderId, setFolderId] = useState('');

  const alias = useMemo(() => generateAlias(name), [name]);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim()) return;
    onSubmit({
      name: name.trim(),
      type,
      description: description.trim() || undefined,
      alias: alias || undefined,
      folderId: folderId || undefined,
    });
  };

  const handleClose = () => {
    setName('');
    setType('TABLE');
    setDescription('');
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
          {name.trim() && (
            <p className="mt-1 text-xs text-gray-500">
              Alias: <span className="font-mono">{alias}</span>
            </p>
          )}
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
            htmlFor="rule-description"
            className="block text-sm font-medium text-gray-700 mb-1"
          >
            Description (optional)
          </label>
          <textarea
            id="rule-description"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            placeholder="Describe what this rule does"
            rows={3}
            className="block w-full rounded-md border-gray-300 shadow-sm focus:border-indigo-500 focus:ring-indigo-500 sm:text-sm"
          />
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
