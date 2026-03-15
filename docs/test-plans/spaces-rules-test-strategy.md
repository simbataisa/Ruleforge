# Test Strategy — Space-Scoped Rule Management

**Date:** 2026-03-14
**Tester:** BMAD Tester & QE Agent
**Epic:** Space-Scoped Rule Management
**Input:** `docs/prd-spaces-rules.md`, `docs/architecture/solution-architecture-spaces-rules.md`

---

## 1. Test Scope

### In Scope
- `GET /v1/spaces/{spaceId}/rules` — list rules in space
- `POST /v1/spaces/{spaceId}/rules` — create rule in space
- `PATCH /v1/rules/{ruleId}/move` — move rule to different space (new endpoint)
- `POST /v1/rules/{ruleId}/copy` with `targetSpaceId` — copy to specific space (extended)
- `POST /v1/rules/{ruleId}/copy` without `targetSpaceId` — backward compatibility
- SpaceDetailPage rules tab (frontend)
- MoveRuleModal (frontend)
- CopyRuleModal (frontend)
- RuleListPage `?spaceId=` context mode (frontend)

### Out of Scope
- Bulk move/copy operations
- Cross-tenant move/copy (security negative test — handled by RLS, not unit tested here)
- Mobile

---

## 2. Test Categories

### 2.1 Backend API Tests (Smoke Test Section 3.17)

Located in `scripts/smoke-test.sh`, Section 3.17.

| Test ID | Test Name | Endpoint | Expected |
|---------|-----------|----------|----------|
| ST-317-01 | List rules in space | `GET /spaces/{id}/rules` | 200 + content array |
| ST-317-02 | Space rules response shape | `GET /spaces/{id}/rules` | Has `content`, `totalElements` |
| ST-317-03 | Create rule in space | `POST /spaces/{id}/rules` | 201 + rule with ID |
| ST-317-04 | Move rule (idempotent) | `PATCH /rules/{id}/move` | 200 |
| ST-317-05 | Copy rule (no targetSpaceId) | `POST /rules/{id}/copy` | 201 backward compat |
| ST-317-06 | Copy rule with targetSpaceId | `POST /rules/{id}/copy { targetSpaceId }` | 201 |
| ST-317-07 | Move rule validation | `PATCH /rules/{id}/move {}` (no targetSpaceId) | 400 |
| ST-317-08 | Cleanup | `DELETE /rules/{id}` | 200 |

### 2.2 Backend Unit Tests (Manual — No Automated Tests Required)

| Scenario | Expected Behavior |
|----------|------------------|
| Move to non-existent space | 404 Space not found |
| Move a locked rule | 409 Conflict — "Rule is locked" |
| Copy to space in different org | 403 Access Denied |
| Move to space in different org | 403 Access Denied |
| Move rule, verify spaceId updated | Rule.spaceId = targetSpaceId |

### 2.3 Frontend Manual Test Checklist

#### SpaceDetailPage Rules Tab
- [ ] Navigate to `/spaces/{spaceId}` — rules section loads
- [ ] Rules display: name, type badge, status badge, version, updated time
- [ ] Search by partial name — list filters in real-time
- [ ] Type filter "TABLE" — only TABLE rules shown
- [ ] Status filter "DRAFT" — only DRAFT rules shown
- [ ] Multiple filters combined — all applied simultaneously
- [ ] "Clear filters" resets all filters
- [ ] Empty space shows empty state with Create Rule button
- [ ] Loading skeleton visible during fetch
- [ ] Error state shown on API failure with Retry button
- [ ] "Create Rule" button opens modal, creates rule in this space
- [ ] Created rule appears in list without page reload

#### MoveRuleModal
- [ ] "Move" in action menu opens MoveRuleModal
- [ ] Modal shows rule name in title message
- [ ] Dropdown shows all spaces except current space
- [ ] Move button disabled until space selected
- [ ] Move button disabled if no other spaces available
- [ ] On success: rule removed from list, toast shown
- [ ] On cancel: modal closes, no changes
- [ ] Escape key closes modal

#### CopyRuleModal
- [ ] "Copy" in action menu opens CopyRuleModal
- [ ] New name pre-filled with "Copy of {ruleName}"
- [ ] Name field is editable
- [ ] Space dropdown shows all spaces (current pre-selected)
- [ ] Copy button disabled if name is empty
- [ ] On success: original rule unchanged, toast shown
- [ ] On cancel: modal closes, no changes

#### RuleListPage Space Context
- [ ] Navigate to `/rules?spaceId={id}` — space context banner shown
- [ ] Banner shows space name
- [ ] Banner has "View Space" link to `/spaces/{id}`
- [ ] Banner has "× Clear" to remove filter
- [ ] List shows only rules from that space
- [ ] "Create Rule" in space context uses `POST /spaces/{id}/rules`
- [ ] Navigate to `/rules` (no param) — no banner, existing behavior

---

## 3. Regression Tests

These existing flows must remain unaffected:

- [ ] `GET /rules` (org-wide) still works
- [ ] `POST /rules` (convenience endpoint) still works
- [ ] `POST /rules/{id}/copy` without targetSpaceId still copies to same space
- [ ] RuleListPage without `?spaceId=` shows all rules (unchanged)
- [ ] Existing SpaceDetailPage header/metadata unchanged

---

## 4. Performance Tests

| Test | Target | Method |
|------|--------|--------|
| `GET /spaces/{id}/rules` with 100 rules | <200ms p95 | curl timing |
| SpaceDetailPage initial load | <1s FCP | Browser DevTools |
| Search filter response (client-side) | <100ms | Browser DevTools |

---

## 5. Security Tests

| Test | Expected |
|------|----------|
| Move rule with unauthenticated request | 401 |
| Move rule to space in different org | 403 |
| Copy rule to space in different org | 403 |
| Inject spaceId from different tenant | Blocked by RLS |

---

## 6. Known Limitations & Deferred

- Pagination for space rules: `GET /spaces/{id}/rules` currently only fetches page 0 (first 20). Pagination UI in SpaceDetailPage is deferred.
- Client-side search: effective for <200 rules. For larger spaces, server-side search via `?search=` param should be added in a follow-up.
- `rule.spaceId` not currently returned in `RuleDto` — frontend cannot verify space assignment from response alone. Backend enhancement deferred.

---

## 7. Test Execution Log

| Date | Tester | Environment | Result | Notes |
|------|--------|-------------|--------|-------|
| 2026-03-14 | BMAD QE Agent | Local (smoke-test --quick) | TBD | Service health checks |
