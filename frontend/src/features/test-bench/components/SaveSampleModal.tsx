import { useState, useEffect } from 'react';
import { Modal } from '@/shared/components/Modal';
import { Button } from '@/shared/components/Button';

interface SaveSampleModalProps {
  open: boolean;
  onClose: () => void;
  onSave: (name: string, description: string) => void;
  loading?: boolean;
  initialName?: string;
  initialDescription?: string;
  title?: string;
}

export function SaveSampleModal({
  open,
  onClose,
  onSave,
  loading,
  initialName = '',
  initialDescription = '',
  title = 'Save as Sample Input',
}: SaveSampleModalProps) {
  const [name, setName] = useState(initialName);
  const [description, setDescription] = useState(initialDescription);

  useEffect(() => {
    if (open) {
      setName(initialName);
      setDescription(initialDescription);
    }
  }, [open, initialName, initialDescription]);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim()) return;
    onSave(name.trim(), description.trim());
  };

  return (
    <Modal
      open={open}
      onClose={onClose}
      title={title}
      footer={
        <>
          <Button variant="secondary" size="sm" onClick={onClose} disabled={loading}>
            Cancel
          </Button>
          <Button size="sm" onClick={handleSubmit} loading={loading} disabled={!name.trim()}>
            Save
          </Button>
        </>
      }
    >
      <form onSubmit={handleSubmit} className="space-y-4">
        <div>
          <label htmlFor="sample-name" className="block text-sm font-medium text-gray-700 mb-1">
            Name
          </label>
          <input
            id="sample-name"
            type="text"
            value={name}
            onChange={(e) => setName(e.target.value)}
            className="block w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
            placeholder="e.g., High credit score applicant"
            autoFocus
          />
        </div>
        <div>
          <label htmlFor="sample-description" className="block text-sm font-medium text-gray-700 mb-1">
            Description
          </label>
          <textarea
            id="sample-description"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            rows={3}
            className="block w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
            placeholder="Describe what this sample input tests..."
          />
        </div>
      </form>
    </Modal>
  );
}
