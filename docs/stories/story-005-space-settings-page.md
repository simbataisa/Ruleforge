# Story 005 — Space Settings Page

**Epic:** Spaces Page Features
**Phase:** Frontend
**Assignee:** Frontend Engineer
**Priority:** P0
**Depends on:** Story 001, Story 002, Story 003 (useUpdateSpace, useDeleteSpace hooks)

---

## User Story

As a space admin or org owner,
I want a dedicated Space Settings page with General settings and a Danger Zone,
So that I can update space name/description and delete the space from one place.

---

## Acceptance Criteria

1. Route `/spaces/:id/settings` renders `SpaceSettingsPage`
2. Breadcrumb: "← Spaces / {name} / Settings"
3. Tab navigation: "General" (default), "Danger Zone" (OWNER only)
4. **General tab:** Pre-populated name + description form; Save/Cancel; success toast on save; 409 inline error on slug conflict
5. **Danger Zone tab (OWNER only):** Delete button + `ConfirmDialog`; on confirm → DELETE API → navigate to `/spaces`
6. Loading state while space data fetches
7. 404 state if space not found

---

## Technical Implementation

### New File: `frontend/src/features/spaces/pages/SpaceSettingsPage.tsx`

```typescript
import { useState, useEffect } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { PageHeader } from '@/shared/components/PageHeader';
import { Button } from '@/shared/components/Button';
import { Input } from '@/shared/components/Input';
import { Spinner } from '@/shared/components/Spinner';
import { ConfirmDialog } from '@/shared/components/ConfirmDialog';
import { useSpace, useUpdateSpace, useDeleteSpace } from '@/api/org.api';
import { useToastStore } from '@/stores/toast.store';
import { useAuthStore } from '@/stores/auth.store';

export function SpaceSettingsPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const addToast = useToastStore((s) => s.addToast);
  const { user } = useAuthStore();
  const isOwner = user?.orgRole === 'OWNER';

  const { data: space, isLoading, isError } = useSpace(id!);
  const updateSpace = useUpdateSpace();
  const deleteSpace = useDeleteSpace();

  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [nameError, setNameError] = useState('');
  const [showDeleteDialog, setShowDeleteDialog] = useState(false);
  const [activeTab, setActiveTab] = useState<'general' | 'danger'>('general');

  // Sync form state when space loads
  useEffect(() => {
    if (space) {
      setName(space.name);
      setDescription(space.description ?? '');
    }
  }, [space]);

  if (isLoading) return <SpaceSettingsSkeleton />;
  if (isError || !space) {
    return (
      <div className="text-center py-12">
        <p className="text-gray-500 mb-4">Space not found.</p>
        <Button variant="outline" onClick={() => navigate('/spaces')}>Back to Spaces</Button>
      </div>
    );
  }

  const hasChanges = name !== space.name || description !== (space.description ?? '');

  const handleSave = () => {
    setNameError('');
    if (!name.trim()) { setNameError('Name is required'); return; }
    updateSpace.mutate(
      { id: id!, name: name.trim(), description: description.trim() || undefined },
      {
        onSuccess: () => addToast({ type: 'success', title: 'Space updated successfully' }),
        onError: (err: any) => {
          if (err?.response?.status === 409) {
            setNameError('A space with this name already exists');
          } else {
            addToast({ type: 'error', title: 'Failed to update space' });
          }
        },
      }
    );
  };

  const handleDelete = () => {
    deleteSpace.mutate(id!, {
      onSuccess: () => {
        addToast({ type: 'success', title: `Space "${space.name}" deleted` });
        navigate('/spaces');
      },
      onError: () => addToast({ type: 'error', title: 'Failed to delete space' }),
    });
    setShowDeleteDialog(false);
  };

  const tabs = [
    { key: 'general', label: 'General' },
    ...(isOwner ? [{ key: 'danger', label: 'Danger Zone' }] : []),
  ] as const;

  return (
    <div>
      {/* Breadcrumb */}
      <nav className="text-sm text-gray-500 mb-4">
        <Link to="/spaces" className="hover:text-gray-700">Spaces</Link>
        <span className="mx-2">/</span>
        <Link to={`/spaces/${id}`} className="hover:text-gray-700">{space.name}</Link>
        <span className="mx-2">/</span>
        <span className="text-gray-900">Settings</span>
      </nav>

      <h1 className="text-2xl font-bold text-gray-900 mb-6">Space Settings</h1>

      {/* Tabs */}
      <div className="border-b border-gray-200 mb-6">
        <nav className="-mb-px flex space-x-8">
          {tabs.map((tab) => (
            <button
              key={tab.key}
              onClick={() => setActiveTab(tab.key as 'general' | 'danger')}
              className={`pb-3 text-sm font-medium border-b-2 ${
                activeTab === tab.key
                  ? 'border-indigo-600 text-indigo-600'
                  : 'border-transparent text-gray-500 hover:text-gray-700'
              } ${tab.key === 'danger' ? 'text-red-500 hover:text-red-600' : ''}`}
            >
              {tab.label}
            </button>
          ))}
        </nav>
      </div>

      {/* General Tab */}
      {activeTab === 'general' && (
        <div className="max-w-lg">
          <div className="mb-4">
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Space Name <span className="text-red-500">*</span>
            </label>
            <Input
              value={name}
              onChange={(e) => { setName(e.target.value); setNameError(''); }}
              maxLength={255}
              className={nameError ? 'border-red-500' : ''}
            />
            {nameError && <p className="text-red-500 text-sm mt-1">{nameError}</p>}
          </div>
          <div className="mb-6">
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Description <span className="text-gray-400">(optional)</span>
            </label>
            <textarea
              className="w-full border border-gray-300 rounded-md p-2 text-sm focus:ring-indigo-500 focus:border-indigo-500"
              rows={3}
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              maxLength={2000}
              placeholder="Describe the purpose of this space..."
            />
          </div>
          <div className="flex gap-3">
            <Button
              variant="outline"
              onClick={() => { setName(space.name); setDescription(space.description ?? ''); setNameError(''); }}
              disabled={!hasChanges}
            >
              Cancel
            </Button>
            <Button
              onClick={handleSave}
              disabled={!hasChanges || !name.trim() || updateSpace.isPending}
            >
              {updateSpace.isPending ? 'Saving...' : 'Save Changes'}
            </Button>
          </div>
        </div>
      )}

      {/* Danger Zone Tab (OWNER only) */}
      {activeTab === 'danger' && isOwner && (
        <div className="border border-red-200 rounded-lg p-6 max-w-lg">
          <h3 className="text-base font-semibold text-gray-900 mb-2">Delete this space</h3>
          <p className="text-sm text-gray-500 mb-4">
            Once you delete a space, there is no going back. This will permanently delete the space
            and all its configuration.
            {space.ruleCount > 0 && (
              <span className="block mt-2 text-red-600 font-medium">
                This space contains {space.ruleCount} rules. Remove them before deleting.
              </span>
            )}
          </p>
          <Button
            variant="destructive"
            disabled={space.ruleCount > 0}
            title={space.ruleCount > 0 ? 'Remove all rules before deleting' : undefined}
            onClick={() => setShowDeleteDialog(true)}
          >
            Delete Space
          </Button>
        </div>
      )}

      <ConfirmDialog
        open={showDeleteDialog}
        onClose={() => setShowDeleteDialog(false)}
        onConfirm={handleDelete}
        title={`Delete '${space.name}'?`}
        description="This action cannot be undone. The space and all its configuration will be permanently deleted."
        confirmLabel="Delete Space"
        variant="destructive"
      />
    </div>
  );
}

function SpaceSettingsSkeleton() {
  return (
    <div className="animate-pulse">
      <div className="h-4 bg-gray-200 rounded w-48 mb-4" />
      <div className="h-8 bg-gray-200 rounded w-56 mb-6" />
      <div className="h-6 bg-gray-200 rounded w-64 mb-6" />
      <div className="h-10 bg-gray-200 rounded mb-4" />
      <div className="h-20 bg-gray-200 rounded" />
    </div>
  );
}
```

### Modified: `frontend/src/app/routes.tsx`

Add lazy import:
```typescript
const SpaceSettingsPage = lazy(() =>
  import('@/features/spaces/pages/SpaceSettingsPage').then((m) => ({ default: m.SpaceSettingsPage })),
);
```

Add route in children array:
```typescript
{ path: '/spaces/:id/settings', element: <Lazy><SpaceSettingsPage /></Lazy> },
```

---

## Definition of Done

- [ ] `/spaces/:id/settings` route registered in `routes.tsx`
- [ ] `SpaceSettingsPage` component created
- [ ] Breadcrumb renders with working links
- [ ] General tab: pre-populated form, saves changes, Cancel resets, inline 409 error
- [ ] Danger Zone tab visible to OWNER only
- [ ] Delete button disabled when ruleCount > 0
- [ ] Delete flow: ConfirmDialog → DELETE API → navigate('/spaces') → success toast
- [ ] Loading skeleton shown while fetching
- [ ] 404/error state renders
- [ ] No TypeScript errors

---

## Dependencies

- Story 001 (enriched SpaceDto)
- Story 002 (PUT + DELETE endpoints)
- Story 003 (`useUpdateSpace`, `useDeleteSpace` hooks)
- `ConfirmDialog` at `frontend/src/shared/components/ConfirmDialog.tsx` (already exists)

**Estimated Complexity:** Medium (2 days)
