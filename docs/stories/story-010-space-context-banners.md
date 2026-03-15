# Story 010 — Space Context Banners

**Epic:** Space Context Awareness | **Priority:** P0 | **Points:** 1

## Description
Show contextual banners in SpaceDetailPage identifying whether the current space is Sandbox or production.

## Acceptance Criteria
- Sandbox space (name === "Sandbox"): show amber banner with FlaskConical icon + sandbox message
- Non-Sandbox space: show blue banner with Shield icon + production message
- Banner appears between header and stats bar
- No TypeScript errors

## Implementation Notes
- Inline in `SpaceDetailPage.tsx` — no new component file
- Conditional on `space.name === 'Sandbox'`
- Use `FlaskConical` and `Shield` from lucide-react (already installed)

## Files
- `frontend/src/features/spaces/pages/SpaceDetailPage.tsx`

