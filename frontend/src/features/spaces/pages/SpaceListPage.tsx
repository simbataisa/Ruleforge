import { useState } from 'react';
import { Plus } from 'lucide-react';
import { PageHeader } from '@/shared/components/PageHeader';
import { Button } from '@/shared/components/Button';
import { Input } from '@/shared/components/Input';
import { Badge } from '@/shared/components/Badge';
import { DataTable } from '@/shared/components/DataTable';
import { Spinner } from '@/shared/components/Spinner';
import { EmptyState } from '@/shared/components/EmptyState';
import { Modal } from '@/shared/components/Modal';
import { useOrgSpaces, useCreateSpace, type OrgSpace } from '@/api/org.api';
import { useToastStore } from '@/stores/toast.store';

export function SpaceListPage() {
  const addToast = useToastStore((s) => s.addToast);
  const { data: spaces, isLoading } = useOrgSpaces();
  const createSpace = useCreateSpace();
  const [showModal, setShowModal] = useState(false);
  const [spaceName, setSpaceName] = useState('');

  const handleCreate = () => {
    const name = spaceName.trim();
    if (!name) return;
    createSpace.mutate(
      { name },
      {
        onSuccess: () => {
          setShowModal(false);
          setSpaceName('');
          addToast({ type: 'success', title: 'Space created' });
        },
        onError: () => addToast({ type: 'error', title: 'Failed to create space' }),
      },
    );
  };

  const columns = [
    {
      key: 'name',
      header: 'Name',
      render: (space: OrgSpace) => (
        <span className="font-medium text-gray-900">{space.name}</span>
      ),
    },
    {
      key: 'slug',
      header: 'Slug',
      render: (space: OrgSpace) => (
        <span className="text-gray-500 font-mono text-sm">{space.slug}</span>
      ),
    },
    {
      key: 'ruleCount',
      header: 'Rules',
      render: (space: OrgSpace) => (
        <Badge className="bg-indigo-100 text-indigo-800">{space.ruleCount}</Badge>
      ),
    },
  ];

  if (isLoading) {
    return (
      <div className="flex items-center justify-center h-64">
        <Spinner size="lg" />
      </div>
    );
  }

  const spaceList = spaces ?? [];

  return (
    <div>
      <PageHeader
        title="Spaces"
        description="Manage workspaces to organize rules within your organization."
        actions={
          <Button onClick={() => setShowModal(true)}>
            <Plus className="h-4 w-4" />
            Create Space
          </Button>
        }
      />

      {spaceList.length === 0 ? (
        <EmptyState
          title="No spaces yet"
          description="Create your first space to start organizing rules."
        />
      ) : (
        <DataTable data={spaceList} columns={columns} />
      )}

      <Modal
        open={showModal}
        onClose={() => setShowModal(false)}
        title="Create Space"
        footer={
          <>
            <Button variant="outline" onClick={() => setShowModal(false)}>
              Cancel
            </Button>
            <Button
              onClick={handleCreate}
              disabled={!spaceName.trim() || createSpace.isPending}
            >
              Create
            </Button>
          </>
        }
      >
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Space Name</label>
          <Input
            value={spaceName}
            onChange={(e) => setSpaceName(e.target.value)}
            placeholder="e.g., Production Rules"
            onKeyDown={(e) => e.key === 'Enter' && handleCreate()}
          />
        </div>
      </Modal>
    </div>
  );
}
