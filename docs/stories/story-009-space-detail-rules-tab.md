# Story 009: SpaceDetailPage Rules Tab

**Epic:** Space-Scoped Rule Management
**Priority:** P0
**Story Points:** 5
**PRD Reference:** US-4
**RICE Score:** 8.1 (highest user value for existing users)
**Tech Lead Refinement:** 2026-03-14

---

## User Story

**As a** Space Admin or Rule Author,
**I want** SpaceDetailPage to show all rules in the space with search, filter, and actions,
**So that** I can understand and manage the rule landscape for my space without navigating away.

---

## Technical Breakdown

### Frontend Changes Only (no backend changes needed)

#### `frontend/src/api/rules.api.ts`
- Add `useSpaceRules(spaceId: string)` hook:
  ```typescript
  export function useSpaceRules(spaceId: string) {
    return useQuery({
      queryKey: ['rules', 'space', spaceId],
      queryFn: () => apiGet(`/spaces/${spaceId}/rules`),
      enabled: !!spaceId,
    });
  }
  ```

#### `frontend/src/features/spaces/pages/SpaceDetailPage.tsx`
- Import and use `useSpaceRules(id)` to fetch rules for the current space
- Add rules section below existing space metadata:
  - Search input (client-side filter on `rule.name`)
  - Type filter dropdown (TABLE, TREE, SCORECARD, etc.)
  - Status filter dropdown (DRAFT, PUBLISHED, etc.)
  - Rule list table with columns: Name, Type badge, Status badge, Version, Updated, Actions
  - Action dropdown per row: Edit, Copy, Move, Delete
  - Empty state when no rules
  - Loading skeleton
  - Error retry state
- Integrate `MoveRuleModal` and `CopyRuleModal` components
- Integrate `ConfirmDialog` for delete action
- "Create Rule" button uses `useCreateSpaceRule(id)`

### Implementation Notes

- Client-side search/filter is sufficient for up to ~500 rules (React memo + useMemo for filtered list)
- For pagination: use existing `PageResponse` pattern; add page controls if `totalElements > 20`
- Reuse existing `Badge` component for type and status indicators
- Reuse `ConfirmDialog` for delete confirmation
- Use `useDeleteRule()` hook (check if exists, create if not)

---

## Acceptance Criteria

- [ ] SpaceDetailPage displays a "Rules" section below existing metadata
- [ ] Rules section shows all rules belonging to that space
- [ ] Rules show: name, type badge (colored), status badge (colored), version, last updated
- [ ] Search input filters rules by name (case-insensitive, client-side)
- [ ] Type filter dropdown correctly filters to selected type
- [ ] Status filter dropdown correctly filters to selected status
- [ ] "Clear filters" resets all filters
- [ ] "Create Rule" button creates rule in this space (Story 007)
- [ ] Edit action navigates to rule editor for that rule
- [ ] Copy action opens CopyRuleModal (Story 008)
- [ ] Move action opens MoveRuleModal (Story 008)
- [ ] Delete action opens ConfirmDialog, deletes on confirm
- [ ] Empty state shown when no rules (or filters show 0)
- [ ] Loading state shown during fetch
- [ ] Error state with retry shown on API failure
- [ ] Result count shown ("Showing N rules" or "Showing N of M rules")

---

## Definition of Done

- [ ] Frontend passes ESLint and TypeScript check
- [ ] Renders correctly with 0, 1, and 20+ rules
- [ ] All filter combinations work correctly
- [ ] MoveRuleModal and CopyRuleModal integrated and functional
- [ ] Manual test: navigate to SpaceDetailPage → see rules → search → filter → actions
- [ ] No console errors or warnings
