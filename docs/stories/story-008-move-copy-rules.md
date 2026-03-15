# Story 008: Move and Copy Rules Between Spaces

**Epic:** Space-Scoped Rule Management
**Priority:** P1
**Story Points:** 5
**PRD Reference:** US-3
**ADR References:** ADR-003, ADR-004
**Tech Lead Refinement:** 2026-03-14

---

## User Story

**As a** Space Admin or Rule Author,
**I want** to move or copy a rule to a different space,
**So that** I can reorganize rules and share logic across spaces.

---

## Technical Breakdown

### Backend Changes (ruleforge-management-service)

#### New File: `dto/MoveRuleRequest.java`
```java
public record MoveRuleRequest(
    @NotNull UUID targetSpaceId
) {}
```

#### Modified File: `dto/CopyRuleRequest.java`
- Add `UUID targetSpaceId` field (optional, nullable)
- Backward compatible: `null` = copy to same space

#### Modified File: `service/RuleService.java`
- `moveRule(UUID ruleId, UUID targetSpaceId)`:
  - Verify rule exists and belongs to current tenant (via findRuleOrThrow)
  - Verify targetSpace exists and belongs to same org
  - Verify rule is not locked (if locked, throw ConflictException)
  - Update `rule.setSpaceId(targetSpaceId)`
  - Save and publish MOVE_RULE event
  - Return updated RuleDto
- Modify `copyRule(UUID ruleId, String nameOverride)` → `copyRule(UUID ruleId, String nameOverride, UUID targetSpaceId)`:
  - When `targetSpaceId` is provided: `copy.setSpaceId(targetSpaceId)` instead of `original.getSpaceId()`
  - When `targetSpaceId` is null: existing behavior (copy to same space)

#### Modified File: `controller/RuleController.java`
- Add `@PatchMapping("/rules/{ruleId}/move")` method calling `ruleService.moveRule()`
- Modify `copyRule()` to pass `request.targetSpaceId()` to service

### Frontend Changes

#### `frontend/src/api/rules.api.ts`
- `useMoveRule()` mutation: `PATCH /v1/rules/{id}/move { targetSpaceId }`
  - On success: invalidate `['rules', 'space', fromSpaceId]` and `['rules', 'space', toSpaceId]`
- Extend `useCopyRule()` to accept `targetSpaceId`: `POST /v1/rules/{id}/copy { name, targetSpaceId }`

#### New File: `frontend/src/features/spaces/components/MoveRuleModal.tsx`
- Props: `{ ruleId, ruleName, currentSpaceId, onClose, onSuccess }`
- Space dropdown: all spaces except currentSpaceId (from `useSpaces()`)
- Submit: `useMoveRule()` → on success: toast + onSuccess() + onClose()
- Handle "no other spaces" case

#### New File: `frontend/src/features/spaces/components/CopyRuleModal.tsx`
- Props: `{ ruleId, ruleName, currentSpaceId, onClose, onSuccess }`
- Name field: pre-filled "Copy of {ruleName}"
- Space dropdown: all spaces including current
- Submit: `useCopyRule()` with targetSpaceId → on success: toast + onSuccess() + onClose()

---

## Acceptance Criteria

### Move Rule
- [ ] "Move" option visible in rule action menu on SpaceDetailPage
- [ ] MoveRuleModal shows all spaces except current space
- [ ] Move button disabled until a target space is selected
- [ ] Move calls `PATCH /v1/rules/{id}/move`
- [ ] On success: rule removed from current space list, success toast shown
- [ ] If rule is locked: error shown "Unlock rule before moving"
- [ ] If no other spaces: message shown, button disabled

### Copy Rule
- [ ] "Copy" option visible in rule action menu on SpaceDetailPage
- [ ] CopyRuleModal shows name field (pre-filled) and space dropdown
- [ ] Copy calls `POST /v1/rules/{id}/copy` with `{ name, targetSpaceId }`
- [ ] On success: original rule unchanged, success toast shown
- [ ] Copy to same space: works (creates duplicate in same space)
- [ ] Copy to different space: rule appears in target space

### Backend
- [ ] `PATCH /v1/rules/{id}/move` returns 200 with updated RuleDto
- [ ] Move endpoint validates targetSpaceId belongs to same org (403 if not)
- [ ] Move endpoint rejects locked rules with 409
- [ ] `POST /v1/rules/{id}/copy` with `targetSpaceId` creates copy in target space
- [ ] Backward compatible: `POST /v1/rules/{id}/copy` with no `targetSpaceId` still works

---

## Definition of Done

- [ ] Backend compiles: `./gradlew :ruleforge-management-service:build -x test`
- [ ] Frontend passes ESLint and TypeScript check
- [ ] Manual test: move rule → verify disappears from source space, appears in target
- [ ] Manual test: copy rule → verify source unchanged, copy in target space
- [ ] Smoke test section 3.17 passes
