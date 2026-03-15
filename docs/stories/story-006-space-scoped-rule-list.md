# Story 006: Space-Scoped Rule List (RuleListPage URL Context)

**Epic:** Space-Scoped Rule Management
**Priority:** P0
**Story Points:** 2
**PRD Reference:** US-1
**Tech Lead Refinement:** 2026-03-14

---

## User Story

**As a** Rule Author navigating from a SpaceDetailPage,
**I want** the RuleListPage to automatically filter rules by space when `?spaceId=` is in the URL,
**So that** I can see and manage only the rules in a specific space without noise from other spaces.

---

## Technical Breakdown

### Frontend Changes

#### `frontend/src/api/rules.api.ts`
- Add `useSpaceRules(spaceId: string)` hook using `GET /v1/spaces/{spaceId}/rules`
- Modify `useRules()` to accept optional `spaceId` param — when provided, delegate to `useSpaceRules`
- Query key: `['rules', 'space', spaceId]` for space-scoped; `['rules']` for org-wide

#### `frontend/src/features/rules/pages/RuleListPage.tsx`
- Read `spaceId` from `useSearchParams()`
- If `spaceId` present: show space context banner, use `useSpaceRules(spaceId)` for data
- Space context banner: show space name (loaded via `useSpace(spaceId)`), "View Space" link, "× Clear" button
- Create Rule button: when in space context, use `useCreateSpaceRule(spaceId)`
- Preserve all existing behavior when `spaceId` is absent

### No Backend Changes Required
Endpoint `GET /v1/spaces/{spaceId}/rules` already exists.

---

## Acceptance Criteria

- [ ] Navigating to `/rules?spaceId=<valid-uuid>` shows only rules from that space
- [ ] Space context banner displays space name
- [ ] "× Clear" button removes `?spaceId=` from URL and shows all rules
- [ ] "View Space" link navigates to `/spaces/{spaceId}`
- [ ] Navigating to `/rules` (no spaceId) shows existing behavior — no banner
- [ ] Create Rule button uses `POST /spaces/{spaceId}/rules` when in space context
- [ ] Invalid/non-existent `spaceId` shows error state in the list
- [ ] Empty space shows empty state message specific to space context

---

## Definition of Done

- [ ] Code implemented and passes ESLint (`npm run lint`)
- [ ] No TypeScript errors
- [ ] Behavior verified manually in local dev
- [ ] Story 007 (space-scoped create) must be implemented alongside this story
