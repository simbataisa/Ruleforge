import { useState } from 'react';
import { Plus, X } from 'lucide-react';
import { PageHeader } from '@/shared/components/PageHeader';
import { Button } from '@/shared/components/Button';
import { Input } from '@/shared/components/Input';
import { Spinner } from '@/shared/components/Spinner';
import { EmptyState } from '@/shared/components/EmptyState';
import { useTags, useCreateTag, useDeleteTag } from '@/api/tags.api';
import { useToastStore } from '@/stores/toast.store';

const TAG_COLORS = [
  'bg-blue-100 text-blue-800',
  'bg-green-100 text-green-800',
  'bg-purple-100 text-purple-800',
  'bg-yellow-100 text-yellow-800',
  'bg-pink-100 text-pink-800',
  'bg-indigo-100 text-indigo-800',
  'bg-red-100 text-red-800',
  'bg-cyan-100 text-cyan-800',
];

function getTagColor(name: string): string {
  let hash = 0;
  for (let i = 0; i < name.length; i++) {
    hash = name.charCodeAt(i) + ((hash << 5) - hash);
  }
  return TAG_COLORS[Math.abs(hash) % TAG_COLORS.length];
}

export function TagListPage() {
  const addToast = useToastStore((s) => s.addToast);
  const { data: tags, isLoading } = useTags();
  const createTag = useCreateTag();
  const deleteTag = useDeleteTag();
  const [newTagName, setNewTagName] = useState('');

  const handleCreate = () => {
    const name = newTagName.trim();
    if (!name) return;
    createTag.mutate(
      { name },
      {
        onSuccess: () => {
          setNewTagName('');
          addToast({ type: 'success', title: 'Tag created' });
        },
        onError: () => addToast({ type: 'error', title: 'Failed to create tag' }),
      },
    );
  };

  const handleDelete = (id: string) => {
    deleteTag.mutate(id, {
      onSuccess: () => addToast({ type: 'success', title: 'Tag deleted' }),
      onError: () => addToast({ type: 'error', title: 'Failed to delete tag' }),
    });
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter') handleCreate();
  };

  if (isLoading) {
    return (
      <div className="flex items-center justify-center h-64">
        <Spinner size="lg" />
      </div>
    );
  }

  const tagList = tags ?? [];

  return (
    <div>
      <PageHeader
        title="Tags"
        description="Organize rules with tags for easy filtering and categorization."
      />

      <div className="flex items-center gap-2 mb-6">
        <Input
          value={newTagName}
          onChange={(e) => setNewTagName(e.target.value)}
          onKeyDown={handleKeyDown}
          placeholder="Enter tag name..."
          className="max-w-xs"
        />
        <Button
          onClick={handleCreate}
          disabled={!newTagName.trim() || createTag.isPending}
        >
          <Plus className="h-4 w-4" />
          Add Tag
        </Button>
      </div>

      {tagList.length === 0 ? (
        <EmptyState
          title="No tags yet"
          description="Create your first tag to start organizing rules."
        />
      ) : (
        <div className="flex flex-wrap gap-2">
          {tagList.map((tag) => (
            <span
              key={tag.id}
              className={`inline-flex items-center gap-1.5 rounded-full px-3 py-1.5 text-sm font-medium ${getTagColor(tag.name)}`}
            >
              {tag.name}
              <button
                onClick={() => handleDelete(tag.id)}
                className="rounded-full p-0.5 hover:bg-black/10"
              >
                <X className="h-3 w-3" />
              </button>
            </span>
          ))}
        </div>
      )}
    </div>
  );
}
