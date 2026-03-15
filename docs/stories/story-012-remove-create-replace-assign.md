# Story 012 — Remove Create Rule from SpaceDetailPage, Add AssignRulesModal

**Story ID:** story-012
**Epic:** Space-Rule Assignment (Epic 4)
**Prepared by:** Tech Lead
**Date:** 2026-03-15
**Priority:** P0
**Estimate:** 3 story points
**Depends on:** Epic 2 (MoveRuleModal pattern), Epic 1 (SpaceDetailPage scaffold)

---

## User Story

**As a** space admin,
**I want** SpaceDetailPage to let me assign existing org rules to the space (instead of creating new ones),
**So that** I can populate the space without generating rule duplicates.

---

## Background

`SpaceDetailPage` currently has a "Create Rule" button invoking `CreateRuleModal`. This is the sole P0 change for Epic 4's space side. The `CreateRuleModal` invocation is removed and replaced with an `AssignRulesModal` component that presents org rules not yet in the current space.

Reference files for pattern:
- `frontend/src/features/spaces/components/MoveRuleModal.tsx` — modal + useMoveRule pattern
- `frontend/src/features/spaces/pages/SpaceDetailPage.tsx` — current state (482 lines)

---

## Acceptance Criteria

1. The "Create Rule" button is no longer rendered on `SpaceDetailPage` for any role.
2. An "Assign Rules" button renders in the same position for users with EDITOR or OWNER role; it is absent for VIEWER role.
3. Clicking "Assign Rules" opens `AssignRulesModal`.
4. `AssignRulesModal` fetches all org rules (`GET /rules`) and excludes those already in the current space.
5. The modal renders a two-panel layout: left = searchable/filterable rule list with checkboxes; right = selected rules summary.
6. The confirm button is labeled "Assign (N)" and is disabled when N = 0.
7. On confirm, `PATCH /rules/{id}/move` is called for each selected rule with `{ "spaceId": currentSpaceId }`.
8. On all-success: modal closes, toast "N rules assigned to [Space Name]", React Query cache for `['spaces', spaceId, 'rules']` is invalidated.
9. On partial failure: modal closes, warning toast lists failed rule names.
10. Cancel and ESC close the modal without side effects.
11. Modal is WCAG 2.1 AA compliant: focus trap, ESC closes, aria-labels on checkboxes, `aria-live` count region.
12. TypeScript strict — no `any` types.

---

## Files to Modify

### `frontend/src/features/spaces/pages/SpaceDetailPage.tsx`

- **Remove:** Import of `CreateRuleModal` (or `CreateRuleButton`) and all related state (`isCreateModalOpen`, `setIsCreateModalOpen`).
- **Remove:** The JSX rendering the "Create Rule" `<Button>` and `<CreateRuleModal>`.
- **Add:** Import `AssignRulesModal` from `../components/AssignRulesModal`.
- **Add:** State: `const [isAssignModalOpen, setIsAssignModalOpen] = useState(false)`.
- **Add:** "Assign Rules" button (EDITOR+ only) in the rules section header, triggering `setIsAssignModalOpen(true)`.
- **Add:** `<AssignRulesModal>` rendered conditionally (same pattern as existing modals on the page).
- **Preserve:** All existing MoveRuleModal, CopyRuleModal, DeleteRule logic — untouched.

**Key props to thread through to AssignRulesModal:**
```typescript
spaceId={space.id}
spaceName={space.name}
existingRuleIds={rules.map(r => r.id)}  // rules already in this space
isOpen={isAssignModalOpen}
onClose={() => setIsAssignModalOpen(false)}
```

---

## Files to Create

### `frontend/src/features/spaces/components/AssignRulesModal.tsx`

**Component interface:**
```typescript
interface AssignRulesModalProps {
  isOpen: boolean;
  onClose: () => void;
  spaceId: string;
  spaceName: string;
  existingRuleIds: string[];
}

export function AssignRulesModal(props: AssignRulesModalProps): JSX.Element
```

**Implementation requirements:**

1. **Data fetching:**
   ```typescript
   // All org rules (no space filter)
   const { data: allRules, isLoading, isError } = useQuery({
     queryKey: ['rules'],
     queryFn: () => rulesApi.listRules(),
     enabled: isOpen,
   });
   ```

2. **Derived state:**
   ```typescript
   const assignableRules = useMemo(() =>
     (allRules?.content ?? [])
       .filter(r => !existingRuleIds.includes(r.id))
       .filter(r => r.name.toLowerCase().includes(searchQuery.toLowerCase())),
     [allRules, existingRuleIds, searchQuery]
   );
   ```

3. **Selection state:**
   ```typescript
   const [selectedIds, setSelectedIds] = useState<Set<string>>(new Set());
   const [searchQuery, setSearchQuery] = useState('');
   ```
   Reset both on modal close (`useEffect` watching `isOpen`).

4. **Mutation:**
   ```typescript
   const { mutateAsync: moveRule } = useMoveRule();

   async function handleConfirm() {
     const results = await Promise.allSettled(
       Array.from(selectedIds).map(id =>
         moveRule({ ruleId: id, spaceId })
       )
     );
     const failed = results
       .filter(r => r.status === 'rejected')
       .map((_, i) => Array.from(selectedIds)[i]);
     // invalidate + toast
     queryClient.invalidateQueries({ queryKey: ['spaces', spaceId, 'rules'] });
     if (failed.length === 0) {
       toast.success(`${selectedIds.size} rules assigned to ${spaceName}`);
     } else {
       toast.warning(`${selectedIds.size - failed.length} of ${selectedIds.size} rules assigned. Failed: ...`);
     }
     onClose();
   }
   ```

5. **UI structure** (per UX spec §3):
   - `Dialog` from Shadcn/ui
   - Two-column layout (`grid grid-cols-[1fr_240px]`)
   - Left: `Input` search, scrollable list with `Checkbox` per rule, type `Badge`, folder path, date
   - Right: selected rule names with deselect `✕`
   - Footer: Cancel + "Assign (N)" Button
   - Loading: 3 `Skeleton` rows
   - Error: retry button

6. **Accessibility:**
   - Modal: `aria-labelledby="assign-rules-modal-title"`
   - Search: `aria-label="Search available rules"`
   - Each checkbox: `aria-label={\`Select ${rule.name}\`}`
   - Count: `<span aria-live="polite">{selectedIds.size} rules selected</span>`

---

## Definition of Done

- [ ] "Create Rule" button removed from SpaceDetailPage; no TypeScript errors introduced
- [ ] "Assign Rules" button renders for EDITOR+ users; hidden for VIEWER
- [ ] `AssignRulesModal.tsx` created in `frontend/src/features/spaces/components/`
- [ ] Modal fetches org rules, excludes existing space rules
- [ ] Search filters in real-time
- [ ] Multi-select with live count in confirm button label
- [ ] PATCH called per selected rule on confirm
- [ ] React Query cache invalidated (`['spaces', spaceId, 'rules']`)
- [ ] Success and partial-failure toasts implemented
- [ ] ESC and Cancel close without side effects; state reset on close
- [ ] WCAG 2.1 AA: focus trap, aria-labels, aria-live, ESC close
- [ ] `npm run lint` passes; `tsc --noEmit` 0 errors
- [ ] Existing SpaceDetailPage smoke tests (section 3.17) still pass

---

## Test Cases

| Test | Expected |
|------|----------|
| VIEWER visits SpaceDetailPage | No "Assign Rules" button visible |
| EDITOR clicks "Assign Rules" | Modal opens with list of assignable rules |
| Search "credit" | Only rules matching "credit" shown |
| Select 2 rules, click "Assign (2)" | 2 PATCH calls made; toast shows "2 rules assigned"; list refreshes |
| Click "Cancel" after selecting | No PATCH calls; modal closes |
| ESC key pressed | Modal closes; no PATCH calls |
| All org rules already in space | Modal left panel shows "All org rules are already in this space" |
| API fetch error | Error state with retry button |
| One PATCH fails out of 3 | Warning toast: "2 of 3 rules assigned. Failed: [Rule Name]" |

---

## Notes for Implementer

- Follow `MoveRuleModal.tsx` (Epic 2) for the `useMoveRule` hook integration pattern.
- `useMoveRule` hook lives at `frontend/src/features/spaces/hooks/useMoveRule.ts` (or similar path from Epic 2 — verify exact location).
- The `rulesApi.listRules()` function (no space filter) should already exist from Epic 2; if not, add a param-less overload to `frontend/src/api/rules.api.ts`.
- Do NOT delete `CreateRuleModal.tsx` — it is still used by `RuleListPage`.
