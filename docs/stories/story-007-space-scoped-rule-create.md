# Story 007: Space-Scoped Rule Creation

**Epic:** Space-Scoped Rule Management
**Priority:** P0
**Story Points:** 1
**PRD Reference:** US-2
**Tech Lead Refinement:** 2026-03-14

---

## User Story

**As a** Rule Author working within a Space,
**I want** the "Create Rule" button on SpaceDetailPage to automatically create the rule in that space,
**So that** I never accidentally create rules in the wrong space.

---

## Technical Breakdown

### Frontend Changes

#### `frontend/src/api/rules.api.ts`
- Add `useCreateSpaceRule(spaceId: string)` mutation hook
  - Calls `POST /v1/spaces/{spaceId}/rules`
  - On success: invalidates `['rules', 'space', spaceId]` query cache

#### `frontend/src/features/spaces/pages/SpaceDetailPage.tsx`
- Wire "Create Rule" button to use `useCreateSpaceRule(spaceId)` — or pass `spaceId` to existing `CreateRuleModal`
- The space selection in `CreateRuleModal` should be pre-set to this space and non-editable

### No Backend Changes Required
Endpoint `POST /v1/spaces/{spaceId}/rules` already exists.

---

## Acceptance Criteria

- [ ] "Create Rule" button on SpaceDetailPage creates rule in the current space
- [ ] API call goes to `POST /v1/spaces/{spaceId}/rules`
- [ ] Space field is pre-filled and not editable in the create modal
- [ ] On success, new rule appears in SpaceDetailPage rules list (no page reload)
- [ ] On success, toast notification shown
- [ ] On error, error message shown in modal

---

## Definition of Done

- [ ] Code implemented and passes ESLint
- [ ] No TypeScript errors
- [ ] Verified via manual test: create rule from SpaceDetailPage → rule appears in space list
