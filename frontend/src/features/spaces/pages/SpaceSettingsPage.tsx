import { useState, useEffect } from 'react';
import { useNavigate, useParams, Link } from 'react-router-dom';
import { ArrowLeft, ChevronRight, AlertTriangle } from 'lucide-react';
import { Button } from '@/shared/components/Button';
import { Input } from '@/shared/components/Input';
import { Spinner } from '@/shared/components/Spinner';
import { ConfirmDialog } from '@/shared/components/ConfirmDialog';
import { useSpace, useUpdateSpace, useDeleteSpace } from '@/api/org.api';
import { useToastStore } from '@/stores/toast.store';

export function SpaceSettingsPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const addToast = useToastStore((s) => s.addToast);

  const { data: space, isLoading, isError } = useSpace(id ?? '');
  const updateSpace = useUpdateSpace(id ?? '');
  const deleteSpace = useDeleteSpace();

  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);

  // Populate form once space data loads
  useEffect(() => {
    if (space) {
      setName(space.name);
      setDescription(space.description ?? '');
    }
  }, [space]);

  const handleSave = (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim()) return;
    updateSpace.mutate(
      { name: name.trim(), description: description.trim() || undefined },
      {
        onSuccess: () => addToast({ type: 'success', title: 'Space updated' }),
        onError: () => addToast({ type: 'error', title: 'Failed to update space' }),
      },
    );
  };

  const handleDelete = () => {
    if (!id) return;
    deleteSpace.mutate(id, {
      onSuccess: () => {
        addToast({ type: 'success', title: `Space "${space?.name}" deleted` });
        navigate('/spaces');
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

  if (isError || !space) {
    return (
      <div className="flex flex-col items-center justify-center h-64 gap-4">
        <p className="text-gray-500">Space not found or you don't have access.</p>
        <Button variant="outline" onClick={() => navigate('/spaces')}>
          Back to Spaces
        </Button>
      </div>
    );
  }

  const isDirty = name !== space.name || (description || '') !== (space.description ?? '');

  return (
    <div className="flex flex-col h-full">
      {/* Breadcrumb */}
      <div className="flex items-center gap-1.5 px-6 pt-4 text-sm text-gray-500">
        <Link to="/spaces" className="hover:text-gray-700 transition-colors">
          Spaces
        </Link>
        <ChevronRight className="h-3.5 w-3.5" />
        <Link to={`/spaces/${id}`} className="hover:text-gray-700 transition-colors">
          {space.name}
        </Link>
        <ChevronRight className="h-3.5 w-3.5" />
        <span className="text-gray-900 font-medium">Settings</span>
      </div>

      {/* Header */}
      <div className="flex items-center gap-3 px-6 py-4 border-b border-gray-200">
        <button
          onClick={() => navigate(`/spaces/${id}`)}
          className="p-1.5 rounded-md text-gray-400 hover:text-gray-600 hover:bg-gray-100 transition-colors"
          aria-label="Back to space"
        >
          <ArrowLeft className="h-4 w-4" />
        </button>
        <div>
          <h1 className="text-xl font-semibold text-gray-900">Space Settings</h1>
          <p className="text-sm text-gray-400 font-mono">{space.slug}</p>
        </div>
      </div>

      <div className="flex-1 overflow-auto p-6">
        <div className="max-w-lg space-y-8">
          {/* General Settings */}
          <section>
            <h2 className="text-base font-semibold text-gray-900 mb-4">General</h2>
            <form onSubmit={handleSave} className="space-y-4">
              <div>
                <label htmlFor="space-name" className="block text-sm font-medium text-gray-700 mb-1">
                  Space Name
                </label>
                <Input
                  id="space-name"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  placeholder="e.g., Production Rules"
                  required
                />
              </div>

              <div>
                <label htmlFor="space-description" className="block text-sm font-medium text-gray-700 mb-1">
                  Description
                  <span className="text-gray-400 font-normal ml-1">(optional)</span>
                </label>
                <textarea
                  id="space-description"
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                  placeholder="Describe the purpose of this space..."
                  rows={3}
                  maxLength={2000}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm text-gray-900 placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent resize-none"
                />
                <p className="text-xs text-gray-400 mt-1">{description.length}/2000</p>
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Slug</label>
                <Input value={space.slug} readOnly className="bg-gray-50 text-gray-500 font-mono" />
                <p className="text-xs text-gray-400 mt-1">
                  Slug is auto-generated from the name and cannot be changed directly.
                </p>
              </div>

              <div className="flex justify-end">
                <Button
                  type="submit"
                  disabled={!isDirty || !name.trim()}
                  loading={updateSpace.isPending}
                >
                  Save Changes
                </Button>
              </div>
            </form>
          </section>

          {/* Danger Zone */}
          <section className="border border-red-200 rounded-lg p-5">
            <div className="flex items-center gap-2 mb-3">
              <AlertTriangle className="h-4 w-4 text-red-500" />
              <h2 className="text-base font-semibold text-red-700">Danger Zone</h2>
            </div>
            <div className="flex items-start justify-between gap-4">
              <div>
                <p className="text-sm font-medium text-gray-900">Delete this space</p>
                <p className="text-xs text-gray-500 mt-0.5">
                  Permanently delete this space. This action cannot be undone. All rules in this
                  space must be removed first.
                </p>
              </div>
              <Button
                variant="danger"
                size="sm"
                onClick={() => setShowDeleteConfirm(true)}
                className="shrink-0"
              >
                Delete Space
              </Button>
            </div>
          </section>
        </div>
      </div>

      <ConfirmDialog
        open={showDeleteConfirm}
        onClose={() => setShowDeleteConfirm(false)}
        onConfirm={handleDelete}
        title="Delete Space"
        message={`Are you sure you want to permanently delete "${space.name}"? This action cannot be undone. All rules inside must be deleted first.`}
        confirmLabel="Delete Space"
        confirmVariant="danger"
        isLoading={deleteSpace.isPending}
      />
    </div>
  );
}
