import { useState } from 'react';
import { Button } from '@/shared/components/Button';
import { Input } from '@/shared/components/Input';
import { Modal } from '@/shared/components/Modal';

interface CreateFolderModalProps {
  open: boolean;
  onClose: () => void;
  onSubmit: (data: { name: string; parentId?: string }) => void;
  loading?: boolean;
  parentId?: string;
}

export function CreateFolderModal({
  open,
  onClose,
  onSubmit,
  loading,
  parentId,
}: CreateFolderModalProps) {
  const [name, setName] = useState('');

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim()) return;
    onSubmit({ name: name.trim(), parentId });
  };

  const handleClose = () => {
    setName('');
    onClose();
  };

  return (
    <Modal open={open} onClose={handleClose} title="Create Folder">
      <form onSubmit={handleSubmit} className="space-y-4">
        <div>
          <label
            htmlFor="folder-name"
            className="block text-sm font-medium text-gray-700 mb-1"
          >
            Folder Name
          </label>
          <Input
            id="folder-name"
            value={name}
            onChange={(e) => setName(e.target.value)}
            placeholder="Enter folder name"
            autoFocus
            required
          />
        </div>
        <div className="flex justify-end gap-3 pt-2">
          <Button type="button" variant="outline" onClick={handleClose}>
            Cancel
          </Button>
          <Button type="submit" loading={loading} disabled={!name.trim()}>
            Create Folder
          </Button>
        </div>
      </form>
    </Modal>
  );
}
