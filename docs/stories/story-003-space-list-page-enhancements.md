# Story 003 — Space List Page Enhancements

**Epic:** Spaces Page Features
**Phase:** Frontend
**Assignee:** Frontend Engineer
**Priority:** P0
**Depends on:** Story 001, Story 002

---

## User Story

As a rule author or space admin,
I want the spaces list to show enriched cards with description, rule count, and actions,
So that I can understand space contents at a glance and manage spaces directly from the list.

---

## Acceptance Criteria

1. Space cards display: name, description (or "No description" placeholder), rule count badge
2. Each card has an "Open" button → navigates to `/spaces/:id`
3. ADMIN/OWNER see a "Settings" gear icon button → navigates to `/spaces/:id/settings`
4. OWNER sees a "Delete" trash icon button; disabled when `ruleCount > 0`
5. Delete triggers `ConfirmDialog`; on confirm: calls DELETE API → removes card → toast success
6. Create Space modal extended with optional description field
7. `OrgSpace` TypeScript type extended with `description`, `updatedAt` fields
8. New `useUpdateSpace` and `useDeleteSpace` hooks added to `org.api.ts`
9. OrgSettingsPage "Manage" button wired to `navigate('/spaces/:id/settings')`

---

## Technical Implementation

### Modified: `frontend/src/api/org.api.ts`

Extend `OrgSpace` interface:
```typescript
export interface OrgSpace {
  id: string;
  name: string;
  slug: string;
  description: string | null;
  ruleCount: number;
  orgId: string;
  createdAt: string;
  updatedAt: string;
}
```

Add new hooks:
```typescript
export function useSpace(spaceId: string) {
  return useQuery({
    queryKey: ['spaces', spaceId],
    queryFn: async () => {
      const res = await apiClient.get<ApiResponse<OrgSpace>>(
        `/organizations/current/spaces/${spaceId}`,
      );
      return res.data.data;
    },
    enabled: !!spaceId,
  });
}

export function useUpdateSpace() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({ id, name, description }: { id: string; name: string; description?: string }) => {
      const res = await apiClient.put<ApiResponse<OrgSpace>>(
        `/organizations/current/spaces/${id}`,
        { name, description },
      );
      return res.data.data;
    },
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ['organization', 'spaces'] });
      queryClient.invalidateQueries({ queryKey: ['spaces', data.id] });
    },
  });
}

export function useDeleteSpace() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (spaceId: string) => {
      await apiClient.delete(`/organizations/current/spaces/${spaceId}`);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['organization', 'spaces'] });
    },
  });
}
```

Also extend `useCreateSpace` to accept optional `description`:
```typescript
mutationFn: async (data: { name: string; description?: string }) => { ... }
```

### Modified: `frontend/src/features/spaces/pages/SpaceListPage.tsx`

Complete rewrite of the card rendering. Key changes:
1. Switch from `DataTable` to card grid layout
2. Add description, rule count badge
3. Add Open, Settings (ADMIN/OWNER), Delete (OWNER) actions using `useAuthStore` role check
4. Import `ConfirmDialog` from `@/shared/components/ConfirmDialog`
5. Import `useNavigate` from `react-router-dom`

Card actions (role-based):
```typescript
const { user } = useAuthStore();
const role = user?.orgRole; // 'OWNER' | 'ADMIN' | 'EDITOR' | 'VIEWER'

// Settings button: visible to ADMIN and OWNER
{(role === 'ADMIN' || role === 'OWNER') && (
  <Button variant="ghost" size="sm" onClick={() => navigate(`/spaces/${space.id}/settings`)}>
    <Settings className="h-4 w-4" />
  </Button>
)}

// Delete button: visible to OWNER only
{role === 'OWNER' && (
  <Button
    variant="ghost"
    size="sm"
    className="text-red-600 hover:text-red-700"
    disabled={space.ruleCount > 0}
    title={space.ruleCount > 0 ? 'Remove all rules before deleting' : 'Delete space'}
    onClick={() => setSpaceToDelete(space)}
  >
    <Trash2 className="h-4 w-4" />
  </Button>
)}
```

Create modal extension (add description field below name):
```tsx
<div className="mt-3">
  <label className="block text-sm font-medium text-gray-700 mb-1">
    Description <span className="text-gray-400">(optional)</span>
  </label>
  <textarea
    className="w-full border border-gray-300 rounded-md p-2 text-sm"
    rows={2}
    value={spaceDescription}
    onChange={(e) => setSpaceDescription(e.target.value)}
    placeholder="Describe the purpose of this space..."
    maxLength={2000}
  />
</div>
```

### Modified: `frontend/src/features/admin/pages/OrgSettingsPage.tsx`

Find the "Manage" button in the spaces section and add `onClick`:
```typescript
<Button
  variant="outline"
  size="sm"
  onClick={() => navigate(`/spaces/${space.id}/settings`)}
>
  Manage
</Button>
```

---

## Definition of Done

- [ ] Space cards show name, description, rule count badge
- [ ] Cards use card layout (not DataTable)
- [ ] "Open" button navigates to `/spaces/:id`
- [ ] Settings button visible to ADMIN/OWNER, navigates to `/spaces/:id/settings`
- [ ] Delete button visible to OWNER, disabled when `ruleCount > 0`
- [ ] Delete flow: ConfirmDialog → DELETE API → success toast → card removed
- [ ] Create modal has optional description field
- [ ] `OrgSpace` type extended with all new fields
- [ ] `useUpdateSpace`, `useDeleteSpace`, `useSpace` hooks added
- [ ] OrgSettings "Manage" button wired to navigate
- [ ] No TypeScript errors
- [ ] Loading, empty, error states all work

---

## Dependencies

- Story 001 (enriched SpaceDto)
- Story 002 (DELETE endpoint)
- `ConfirmDialog` at `frontend/src/shared/components/ConfirmDialog.tsx` (already exists)

**Estimated Complexity:** Medium (2 days)
