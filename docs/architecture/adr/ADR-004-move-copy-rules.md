# ADR-004: Move Rule Implementation Approach

**Date:** 2026-03-14
**Status:** ACCEPTED
**Deciders:** Solution Architect, Backend Engineer
**Context:** Space-Scoped Rule Management epic

---

## Context

To move a rule from one space to another, we have three options:
1. Add `spaceId` to `UpdateRuleRequest` (use existing `PUT /rules/{id}`)
2. Create a dedicated `PATCH /rules/{id}/move` endpoint
3. Require frontend to delete + recreate (no backend change)

## Decision

**Implement `PATCH /rules/{id}/move`** — a dedicated lightweight endpoint.

## Rationale

### Why not option 1 (extend UpdateRuleRequest)?
- `UpdateRuleRequest` currently updates metadata (name, description, folder, tags). Adding `spaceId` conflates two semantically different operations: "edit rule metadata" vs. "reorganize rule ownership".
- Moving a rule requires validation logic (target space must belong to same org, rule must not be locked) that is distinct from normal update validation.
- Mixing concerns in `PUT /rules/{id}` makes it unclear from a REST semantics standpoint whether a full PUT should also allow space changes.

### Why not option 3 (delete + recreate)?
- Would reset rule history and version numbers — unacceptable for audit compliance
- Would change the rule's UUID — breaking any external references or solver cache entries
- Creates data integrity risk during the delete window

### Why option 2 (dedicated PATCH)?
- Clean semantics: PATCH for partial update of a specific attribute (space assignment)
- Enables specific validation and audit logging (`MOVE_RULE` event distinct from `UPDATE_RULE`)
- Minimal implementation: one DTO, one controller method, one service method
- HTTP PATCH is the correct verb for partial updates per RFC 5789
- Idempotent: calling PATCH /rules/{id}/move with the same targetSpaceId twice is safe

## Implementation Notes

- DTO: `MoveRuleRequest { UUID targetSpaceId @NotNull }`
- Service validates: target space exists AND belongs to same org as the rule's current space
- Service validates: rule is not locked (locked rules cannot be moved)
- Audit event: `MOVE_RULE` with `{ fromSpaceId, toSpaceId }`
- No version increment required (space assignment is not a content change)

## Consequences

- Requires implementing one new DTO, one controller method, one service method
- Consistent with existing `POST /rules/{id}/copy` pattern (dedicated sub-resource action)
- All version history is preserved; UUID unchanged
