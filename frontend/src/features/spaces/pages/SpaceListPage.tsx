import { useState } from 'react';
import { Plus, Settings, ExternalLink, Pencil, Trash2 } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { PageHeader } from '@/shared/components/PageHeader';
import { Button } from '@/shared/components/Button';
import { Input } from '@/shared/components/Input';
import { Badge } from '@/shared/components/Badge';
import { Spinner } from '@/shared/components/Spinner';
import { EmptyState } from '@/shared/components/EmptyState';
import { Modal } from '@/shared/components/Modal';
import { ConfirmDialog } from '@/shared/components/ConfirmDialog';
import { useOrgSpaces, useCreateSpace, useDeleteSpace, type OrgSpace } from '@/api/org.api';
import { useToastStore } from '@/stores/toast.store';

export function SpaceListPage() {
  const navigate = useNavigate();
  const addToast = useToastStore((s) => s.addToast);
  const { data: spaces, isLoading } = useOrgSpaces();
  const createSpace = useCreateSpace();
  const deleteSpace = useDeleteSpace();

  const [showCreateModal, setShowCreateModal] = useState(false);
  const [spaceName, setSpaceName] = useState('');

  const [deleteTarget, setDeleteTarget] = useState<OrgSpace | null>(null);

  const handleCreate = () => {
    const name = spaceName.trim();
    if (!name) return;
    createSpace.mutate(
      { name },
      {
        onSuccess: () => {
          setShowCreateModal(false);
          setSpaceName('');
          addToast({ type: 'success', title: 'Space created' });
        },
        onError: () => addToast({ type: 'error', title: 'Failed to create space' }),
      },
    );
  };

  const handleDelete = () => {
    if (!deleteTarget) return;
    deleteSpace.mutate(deleteTarget.id, {
      onSuccess: () => {
        setDeleteTarget(null);
        addToast({ type: 'success', title: `Space "${deleteTarget.name}" deleted` });
      },
      onError: () =>
        addToast({ type: 'error', title: 'Cannot delete space — remove all rules first' }),
    });
  };

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
          <Button onClick={() => setShowCreateModal(true)}>
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
        <div className="grid gap-4 p-6 grid-cols-1 md:grid-cols-2 xl:grid-cols-3">
          {spaceList.map((space) => (
            <div
              key={space.id}
              className="bg-white border border-gray-200 rounded-lg p-5 hover:border-gray-300 hover:shadow-sm transition-all flex flex-col gap-3"
            >
              {/* Header */}
              <div className="flex items-start justify-between gap-2">
                <div className="min-w-0">
                  <h3 className="font-semibold text-gray-900 truncate">{space.name}</h3>
                  <p className="text-xs text-gray-400 font-mono mt-0.5 truncate">{space.slug}</p>
                </div>
                <Badge className="bg-indigo-100 text-indigo-800 shrink-0">
                  {space.ruleCount} rule{space.ruleCount !== 1 ? 's' : ''}
                </Badge>
              </div>

              {/* Description */}
              {space.description ? (
                <p className="text-sm text-gray-500 line-clamp-2">{space.description}</p>
              ) : (
                <p className="text-sm text-gray-400 italic">No description</p>
              )}

              {/* Actions */}
              <div className="flex items-center gap-2 mt-auto pt-1 border-t border-gray-100">
                <Button
                  size="sm"
                  variant="outline"
                  className="flex-1"
                  onClick={() => navigate(`/spaces/${space.id}`)}
                >
                  <ExternalLink className="h-3.5 w-3.5" />
                  Open
                </Button>
                <Button
                  size="sm"
                  variant="outline"
                  onClick={() => navigate(`/spaces/${space.id}/settings`)}
                  title="Settings"
                >
                  <Settings className="h-3.5 w-3.5" />
                </Button>
                <Button
                  size="sm"
                  variant="outline"
                  onClick={() => navigate(`/spaces/${space.id}/settings`)}
                  title="Edit"
                >
                  <Pencil className="h-3.5 w-3.5" />
                </Button>
                <Button
                  size="sm"
                  variant="outline"
                  onClick={() => setDeleteTarget(space)}
                  title="Delete"
                  className="text-red-600 hover:bg-red-50 hover:border-red-300"
                >
                  <Trash2 className="h-3.5 w-3.5" />
                </Button>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Create Space Modal */}
      <Modal
        open={showCreateModal}
        onClose={() => setShowCreateModal(false)}
        title="Create Space"
        footer={
          <>
            <Button variant="outline" onClick={() => setShowCreateModal(false)}>
              Cancel
            </Button>
            <Button
              onClick={handleCreate}
              disabled={!spaceName.trim() || createSpace.isPending}
              loading={createSpace.isPending}
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
            autoFocus
          />
        </div>
      </Modal>

      {/* Delete Confirm */}
      <ConfirmDialog
        open={!!deleteTarget}
        onClose={() => setDeleteTarget(null)}
        onConfirm={handleDelete}
        title="Delete Space"
        message={`Are you sure you want to delete "${deleteTarget?.name}"? This action cannot be undone.`}
        confirmLabel="Delete Space"
        confirmVariant="danger"
        isLoading={deleteSpace.isPending}
      />
    </div>
  );
}
