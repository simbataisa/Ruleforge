# Story 013 — Add "Assign to Space" Action to RuleListPage (Single + Bulk)

**Story ID:** story-013
**Epic:** Space-Rule Assignment (Epic 4)
**Prepared by:** Tech Lead
**Date:** 2026-03-15
**Priority:** P0 (single) / P1 (bulk)
**Estimate:** 3 story points
**Depends on:** Story 012 (AssignToSpaceModal may reuse spacesApi), Epic 2 (useMoveRule hook)

---

## User Story

**As a** rule author,
**I want to** assign one or more rules to a different space directly from the Rules page,
**So that** I can organize rules by space without navigating to each space's detail page.

---

## Background

`RuleListPage` (834 lines) currently has a per-row action menu (⋯) with Edit, Duplicate, Move to Space, and Delete. It also has a bulk selection feature with a bulk action bar. Neither the action menu nor the bulk bar has an "Assign to Space" option.

This story adds:
1. "Assign to Space" to the single-rule action menu (P0)
2. "Assign to Space" to the bulk action bar (P1 — implement in the same story given shared component)
3. The `AssignToSpaceModal` component used by both

---

## Acceptance Criteria

1. The single-rule action menu (⋯) on `RuleListPage` includes an "Assign to Space" option for EDITOR+ roles.
2. Clicking "Assign to Space" for a single rule opens `AssignToSpaceModal` with that rule's ID and name.
3. The bulk action bar includes "Assign to Space" when ≥ 1 rows are selected, for EDITOR+ roles.
4. Clicking "Assign to Space" in the bulk bar opens `AssignToSpaceModal` with all selected rule IDs and names.
5. `AssignToSpaceModal` fetches all org spaces from `GET /spaces`.
6. The space dropdown lists spaces alphabetically; confirm is disabled until a space is selected.
7. On confirm, `PATCH /rules/{id}/move` is called for each rule ID with `{ "spaceId": selectedSpaceId }`.
8. On all-success: modal closes, toast "N rules assigned to [Space Name]", `['rules']` query invalidated.
9. On partial failure: modal closes, warning toast enumerates failed rule names.
10. Cancel and ESC close without side effects.
11. VIEWER role: "Assign to Space" is absent from action menu and bulk bar.
12. TypeScript strict — no `any` types.

---

## Files to Modify

### `frontend/src/features/rules/pages/RuleListPage.tsx`

**Single-rule action menu changes:**
- Add state for AssignToSpaceModal:
  ```typescript
  const [assignModalTarget, setAssignModalTarget] = useState<{
    ruleIds: string[];
    ruleNames: string[];
  } | null>(null);
  ```
- In the row action menu dropdown, add (after "Move to Space" if it exists, otherwise after "Duplicate"):
  ```typescript
  {canEdit && (
    <DropdownMenuItem
      onClick={() => setAssignModalTarget({ ruleIds: [rule.id], ruleNames: [rule.name] })}
    >
      <LinkIcon className="mr-2 h-4 w-4" />
      Assign to Space
    </DropdownMenuItem>
  )}
  ```

**Bulk action bar changes:**
- In the bulk action bar component/section, add:
  ```typescript
  {canEdit && (
    <Button
      variant="outline"
      size="sm"
      onClick={() => setAssignModalTarget({
        ruleIds: selectedRows.map(r => r.id),
        ruleNames: selectedRows.map(r => r.name),
      })}
    >
      <LinkIcon className="mr-2 h-4 w-4" />
      Assign to Space
    </Button>
  )}
  ```

**Modal rendering (bottom of JSX, alongside existing modals):**
```typescript
{assignModalTarget && (
  <AssignToSpaceModal
    isOpen={true}
    onClose={() => setAssignModalTarget(null)}
    ruleIds={assignModalTarget.ruleIds}
    ruleNames={assignModalTarget.ruleNames}
  />
)}
```

---

## Files to Create

### `frontend/src/features/rules/components/AssignToSpaceModal.tsx`

**Component interface:**
```typescript
interface AssignToSpaceModalProps {
  isOpen: boolean;
  onClose: () => void;
  ruleIds: string[];       // one ID for single; N IDs for bulk
  ruleNames: string[];     // for display in body text and error toasts
}

export function AssignToSpaceModal(props: AssignToSpaceModalProps): JSX.Element
```

**Implementation requirements:**

1. **Data fetching:**
   ```typescript
   const { data: spaces, isLoading, isError } = useQuery({
     queryKey: ['spaces'],
     queryFn: () => spacesApi.listSpaces(),
     enabled: isOpen,
   });
   ```

2. **Local state:**
   ```typescript
   const [selectedSpaceId, setSelectedSpaceId] = useState<string>('');
   const [isSubmitting, setIsSubmitting] = useState(false);
   ```
   Reset on modal close.

3. **Derived display:**
   ```typescript
   const isBulk = ruleIds.length > 1;
   const bodyText = isBulk
     ? `Assign ${ruleIds.length} rules to a space.`
     : `Assign "${ruleNames[0]}" to a space.`;
   const modalTitle = 'Assign to Space';
   ```

4. **Mutation:**
   ```typescript
   const { mutateAsync: moveRule } = useMoveRule();

   async function handleConfirm() {
     setIsSubmitting(true);
     const results = await Promise.allSettled(
       ruleIds.map(id => moveRule({ ruleId: id, spaceId: selectedSpaceId }))
     );
     setIsSubmitting(false);

     const failed = results
       .filter((r, i): r is PromiseRejectedResult => r.status === 'rejected')
       .map((_, i) => ruleNames[i]);
     const spaceName = spaces?.find(s => s.id === selectedSpaceId)?.name ?? 'space';

     queryClient.invalidateQueries({ queryKey: ['rules'] });

     if (failed.length === 0) {
       const label = ruleIds.length === 1 ? `"${ruleNames[0]}"` : `${ruleIds.length} rules`;
       toast.success(`${label} assigned to ${spaceName}`);
       onClose();
     } else if (failed.length < ruleIds.length) {
       toast.warning(
         `${ruleIds.length - failed.length} of ${ruleIds.length} rules assigned. Failed: ${failed.join(', ')}`
       );
       onClose();
     } else {
       toast.error('Assignment failed. Please try again.');
       // do not close — let user retry
     }
   }
   ```

5. **UI structure** (per UX spec §5):
   - `Dialog`, `DialogContent` (`max-w-sm`)
   - `DialogHeader` with title "Assign to Space"
   - Body: `<p>{bodyText}</p>` + label "Target Space" + `Select` component
   - `Select` options: sorted spaces, `placeholder="Select a space..."`
   - Footer: Cancel + Assign button (disabled when no space selected or submitting)
   - Loading dropdown: `SelectTrigger` disabled with text "Loading spaces..."
   - Error: inline error message with Retry

6. **Accessibility:**
   - `aria-labelledby="assign-to-space-title"`
   - Space select: `aria-label="Target space"`
   - Assign button: `aria-disabled` when disabled

---

## Definition of Done

- [ ] "Assign to Space" added to single-rule action menu on `RuleListPage` (EDITOR+ only)
- [ ] "Assign to Space" added to bulk action bar on `RuleListPage` (EDITOR+ only)
- [ ] `AssignToSpaceModal.tsx` created in `frontend/src/features/rules/components/`
- [ ] Modal fetches and displays org spaces in dropdown
- [ ] Confirm disabled until space selected
- [ ] PATCH called per rule ID on confirm
- [ ] React Query cache invalidated (`['rules']`)
- [ ] Success and partial-failure toasts
- [ ] Cancel and ESC close without side effects; state reset on close
- [ ] VIEWER role: no "Assign to Space" controls visible
- [ ] `npm run lint` passes; `tsc --noEmit` 0 errors
- [ ] Existing RuleListPage smoke tests pass

---

## Test Cases

| Test | Expected |
|------|----------|
| VIEWER opens action menu | No "Assign to Space" item |
| EDITOR opens action menu | "Assign to Space" is present |
| Click "Assign to Space" (single) | Modal opens; body shows rule name |
| Select space, click "Assign" | PATCH called; toast "Rule assigned to [Space]"; list refreshes |
| Click "Cancel" | No PATCH; modal closes |
| ESC key | Modal closes; no PATCH |
| Select 3 rules, bulk "Assign to Space" | Modal opens with "Assign 3 Rules to Space" |
| Bulk confirm (all success) | 3 PATCHes; toast "3 rules assigned to [Space]"; checkboxes cleared |
| Bulk confirm (1 of 3 fails) | 2 succeed; warning toast with failed name |
| All PATCHes fail | Error toast; modal stays open |
| Spaces API error | Inline error with retry button |

---

## Notes for Implementer

- `useMoveRule` hook: check Epic 2 implementation. The hook should accept `{ ruleId: string, spaceId: string }` and call `PATCH /rules/{ruleId}/move` with `{ spaceId }` body.
- `spacesApi.listSpaces()`: verify this function exists in `frontend/src/api/` (added in Epic 1). If not, add it.
- The `selectedRows` state on RuleListPage may be stored as `string[]` (IDs) or as full rule objects — check the current implementation and adapt accordingly.
- If the existing bulk action bar already has a pattern for passing selected rule objects (not just IDs), follow that pattern for `ruleNames`.
- Do NOT change or remove the existing "Move to Space" action — it remains for backward compatibility and may be evaluated for consolidation in a future epic.
- `LinkIcon` is from `lucide-react` (already a project dependency).
