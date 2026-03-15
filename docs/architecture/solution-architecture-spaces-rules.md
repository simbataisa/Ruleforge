# Solution Architecture — Space-Scoped Rule Management

**Date:** 2026-03-14
**Architect:** BMAD Solution Architect Agent
**Input:** `docs/prd-spaces-rules.md` (v1.0)
**Status:** APPROVED

---

## Changelog

| Version | Date | Author | Change |
|---------|------|--------|--------|
| 1.0 | 2026-03-14 | SA Agent | Initial architecture for space-scoped rule management |

---

## 1. Existing Endpoint Inventory (Management Service — Port 8082)

After reviewing `RuleController.java`, the following is confirmed:

| Endpoint | Status | Notes |
|----------|--------|-------|
| `GET /v1/spaces/{spaceId}/rules` | EXISTS | Returns paged rules for a space; supports folderId, status params |
| `POST /v1/spaces/{spaceId}/rules` | EXISTS | Creates rule in specific space |
| `GET /v1/rules` | EXISTS | Returns all rules for org (org-wide convenience) |
| `POST /v1/rules` | EXISTS | Creates rule in first space found for org (convenience) |
| `POST /v1/rules/{id}/copy` | EXISTS (partial) | Only accepts `name`; copies to **same space** — needs `targetSpaceId` |
| `PATCH /v1/rules/{id}/move` | MISSING | Needs implementation |
| `PUT /v1/rules/{id}` | EXISTS | UpdateRuleRequest does NOT include `spaceId` |

**Gap Summary:**
1. `copyRule()` service method copies to same space only — needs `targetSpaceId` parameter
2. `CopyRuleRequest` record only has `name` — needs `targetSpaceId` field added
3. `PATCH /v1/rules/{id}/move` endpoint does not exist — needs `MoveRuleRequest` DTO + controller method + service method

---

## 2. API Contract

### 2.1 Existing Endpoints (No Change)

#### GET /v1/spaces/{spaceId}/rules
```
GET /v1/spaces/{spaceId}/rules?status=PUBLISHED&page=0&size=20
Authorization: Bearer <token>

Response 200:
{
  "success": true,
  "data": {
    "content": [RuleDto],
    "page": 0,
    "size": 20,
    "totalElements": 5,
    "totalPages": 1
  }
}
```

#### POST /v1/spaces/{spaceId}/rules
```
POST /v1/spaces/{spaceId}/rules
Authorization: Bearer <token>
Content-Type: application/json

{
  "name": "Loan Approval",
  "type": "TABLE",
  "description": "Optional",
  "folderId": null,
  "tags": []
}

Response 201: { "success": true, "data": RuleDto, "message": "Rule created successfully" }
```

### 2.2 Modified Endpoints

#### POST /v1/rules/{ruleId}/copy (MODIFIED)
Add `targetSpaceId` to `CopyRuleRequest`. When provided, copy lands in target space. When absent, copy stays in source space (backward compatible).

```
POST /v1/rules/{ruleId}/copy
Authorization: Bearer <token>
Content-Type: application/json

{
  "name": "Copy of Loan Approval",    // optional — defaults to "Copy of <name>"
  "targetSpaceId": "uuid-of-target"   // optional — defaults to same space
}

Response 201: { "success": true, "data": RuleDto, "message": "Rule copied successfully" }
Response 404: Space or rule not found
```

### 2.3 New Endpoints

#### PATCH /v1/rules/{ruleId}/move (NEW)
Atomically changes the rule's `space_id` to the target space.

```
PATCH /v1/rules/{ruleId}/move
Authorization: Bearer <token>
Content-Type: application/json

{
  "targetSpaceId": "uuid-of-target-space"
}

Response 200: { "success": true, "data": RuleDto, "message": "Rule moved successfully" }
Response 400: targetSpaceId is null or invalid UUID
Response 404: Rule or target space not found
Response 403: User lacks permission to move rules
Response 409: Rule is locked (must unlock before moving)
```

---

## 3. Component Impact

### 3.1 Backend: ruleforge-management-service

| Component | Change Required |
|-----------|----------------|
| `CopyRuleRequest.java` | Add `UUID targetSpaceId` field |
| `MoveRuleRequest.java` | New DTO: `UUID targetSpaceId` (required, @NotNull) |
| `RuleController.java` | Add `@PatchMapping("/rules/{id}/move")` method |
| `RuleService.java` | Modify `copyRule()` to accept targetSpaceId; add `moveRule()` |

### 3.2 Frontend: ruleforge-frontend

| Component | Change Required |
|-----------|----------------|
| `api/rules.api.ts` | Add `useSpaceRules`, `useCreateSpaceRule`, `useMoveRule`; modify `useCopyRule` |
| `features/spaces/pages/SpaceDetailPage.tsx` | Replace placeholder stats with full rules tab |
| `features/spaces/components/MoveRuleModal.tsx` | New component |
| `features/spaces/components/CopyRuleModal.tsx` | New component |
| `features/rules/pages/RuleListPage.tsx` | Add `?spaceId=` query param support + space context banner |

---

## 4. Data Model (No Schema Changes Required)

```sql
-- Existing schema (no migrations needed)
CREATE TABLE rules (
    id          UUID PRIMARY KEY,
    space_id    UUID NOT NULL REFERENCES spaces(id),
    folder_id   UUID REFERENCES folders(id),
    org_id      UUID NOT NULL REFERENCES organizations(id),
    name        VARCHAR NOT NULL,
    alias       VARCHAR UNIQUE NOT NULL,
    type        VARCHAR NOT NULL,
    status      VARCHAR NOT NULL DEFAULT 'DRAFT',
    ...
);
```

The `PATCH /move` operation simply updates `space_id` on the rule row. No schema migration needed.

---

## 5. Sequence Diagrams

### 5.1 Move Rule Flow
```
User → SpaceDetailPage → [click Move] → MoveRuleModal
MoveRuleModal → useSpaces() → GET /v1/spaces (fetch available spaces)
User → [select target space] → [click Move]
MoveRuleModal → useMoveRule() → PATCH /v1/rules/{id}/move { targetSpaceId }
Management Service → validateTenant() → validateSpaceOwnership()
Management Service → DB: UPDATE rules SET space_id = ? WHERE id = ?
Management Service → EventPublisher: MOVE_RULE event
Management Service → 200 OK { RuleDto with new spaceId }
MoveRuleModal → onSuccess() → refetch SpaceDetailPage rules → rule removed from list
```

### 5.2 Copy Rule to Different Space Flow
```
User → SpaceDetailPage → [click Copy] → CopyRuleModal
CopyRuleModal → useSpaces() → GET /v1/spaces
User → [select target space, optionally rename] → [click Copy]
CopyRuleModal → useCopyRule() → POST /v1/rules/{id}/copy { name, targetSpaceId }
Management Service → creates new rule in targetSpaceId
Management Service → 201 Created { RuleDto }
CopyRuleModal → onSuccess() → toast "Copied to <space>"
```

---

## 6. Error Handling

| Scenario | HTTP Status | Frontend Behavior |
|----------|-------------|-------------------|
| Rule not found | 404 | Toast: "Rule not found" |
| Target space not found | 404 | Toast: "Target space not found" |
| Rule is locked (move) | 409 | Toast: "Unlock rule before moving" |
| Cross-tenant attempt | 403 | Toast: "Access denied" |
| Network error | 5xx | Toast: "Operation failed. Retry?" |

---

## 7. Security Considerations

- All endpoints are authenticated (existing JWT filter applies)
- Move/Copy operations validate that the target space belongs to the same tenant (org_id must match)
- RLS in PostgreSQL enforces tenant isolation at DB level for any direct DB access
- Move operation publishes a `MOVE_RULE` audit event for compliance traceability

---

## 8. Performance Considerations

- `GET /spaces/{spaceId}/rules` is a keyed query (`WHERE space_id = ?`) — no full table scan
- `rules.space_id` should have an index (verify: `CREATE INDEX IF NOT EXISTS idx_rules_space_id ON rules(space_id)`)
- Move operation is a single-row UPDATE — negligible overhead
- Frontend uses React Query caching; after move/copy, invalidate `['rules', spaceId]` queries

---

## ADRs Created

- `docs/architecture/adr/ADR-003-space-scoped-rule-api.md` — API design rationale
- `docs/architecture/adr/ADR-004-move-copy-rules.md` — Move vs. delete+recreate approach
