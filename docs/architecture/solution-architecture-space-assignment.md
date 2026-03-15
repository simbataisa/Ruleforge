# Solution Architecture — Space-Rule Assignment (Epic 4)

**Prepared by:** Solution Architect
**Date:** 2026-03-15
**Epic:** Space-Rule Assignment (Epic 4)
**Input:** `docs/prd-space-assignment.md`
**Status:** APPROVED

---

## 1. Overview

Epic 4 is a pure frontend change. The existing backend API fully supports all required operations:

- Assigning a rule to a space: `PATCH /rules/{id}/move` with `{ "spaceId": "..." }`
- Listing org rules: `GET /rules` (org-scoped, no space filter)
- Listing spaces: `GET /spaces`

No new backend endpoints, no schema changes, no migration scripts.

---

## 2. Affected Files

### Files to Modify

| File | Change |
|------|--------|
| `frontend/src/features/spaces/pages/SpaceDetailPage.tsx` | Remove Create Rule button + modal invocation; add Assign Rules button |
| `frontend/src/features/rules/pages/RuleListPage.tsx` | Add "Assign to Space" to single-rule action menu and bulk action bar |

### Files to Create

| File | Purpose |
|------|---------|
| `frontend/src/features/spaces/components/AssignRulesModal.tsx` | Multi-select modal for assigning org rules to a space |
| `frontend/src/features/rules/components/AssignToSpaceModal.tsx` | Space-picker modal for assigning a rule (or rules) to a chosen space |

### Files Unchanged

- All backend services
- `MoveRuleModal.tsx`, `CopyRuleModal.tsx` — existing modals unaffected
- All hooks in `frontend/src/features/spaces/hooks/` and `frontend/src/features/rules/hooks/`

---

## 3. Component Design

### 3.1 AssignRulesModal

**Location:** `frontend/src/features/spaces/components/AssignRulesModal.tsx`

**Purpose:** Shown from SpaceDetailPage. Lists all org rules NOT currently in the target space. User selects via checkboxes, confirms, triggers PATCH per rule.

**Props Interface:**
```typescript
interface AssignRulesModalProps {
  isOpen: boolean;
  onClose: () => void;
  spaceId: string;
  spaceName: string;
  /** Rule IDs already in this space — used to exclude from the assignable list */
  existingRuleIds: string[];
}
```

**Internal State:**
- `searchQuery: string` — filters rule list by name
- `selectedIds: Set<string>` — tracks checked rule IDs

**Data Dependencies:**
- `useRules()` — fetches `GET /rules` (all org rules, no space filter). React Query; cached.
- `useMoveRule()` — wraps `PATCH /rules/{id}/move`; called per selected rule on confirm

**Derived data:**
```
assignableRules = allOrgRules
  .filter(r => !existingRuleIds.includes(r.id))
  .filter(r => r.name.toLowerCase().includes(searchQuery.toLowerCase()))
```

**Behavior:**
1. On open: fetch (or use cached) org rules; compute `assignableRules`.
2. Render two-panel layout: left = filtered rule list with checkboxes; right = selected summary.
3. Confirm button label: "Assign (N)" where N = `selectedIds.size`.
4. On confirm: call `useMoveRule` for each selected ID sequentially via `Promise.allSettled`.
5. On all-success: close modal, show success toast, invalidate `['spaces', spaceId, 'rules']` query.
6. On partial failure: show warning toast listing failed rule names.
7. ESC and Cancel both close without side effects.

---

### 3.2 AssignToSpaceModal

**Location:** `frontend/src/features/rules/components/AssignToSpaceModal.tsx`

**Purpose:** Shown from RuleListPage (single-rule action menu OR bulk action bar). User selects a target space from a dropdown, confirms, triggers PATCH per rule.

**Props Interface:**
```typescript
interface AssignToSpaceModalProps {
  isOpen: boolean;
  onClose: () => void;
  /** Single rule or multiple rules (bulk) */
  ruleIds: string[];
  ruleNames: string[];  // for display / error messages
}
```

**Internal State:**
- `selectedSpaceId: string | null`

**Data Dependencies:**
- `useSpaces()` — fetches `GET /spaces` (org-scoped). React Query; cached.
- `useMoveRule()` — wraps `PATCH /rules/{id}/move`

**Behavior:**
1. On open: fetch (or use cached) spaces list.
2. Render space dropdown (Select component from Shadcn/ui).
3. Title adapts: "Assign to Space" (single) or "Assign N Rules to Space" (bulk).
4. Confirm disabled until a space is selected.
5. On confirm: call `useMoveRule` for each rule ID with `selectedSpaceId`.
6. On success: close, toast, invalidate `['rules']` query.
7. On partial failure: toast listing failed rule names.

---

## 4. Data Flow Diagrams

### 4.1 AssignRulesModal (SpaceDetailPage → Assign)

```
SpaceDetailPage
  │
  ├─ renders rules list (useSpaceRules(spaceId))
  │    └─ GET /spaces/{spaceId}/rules → existingRuleIds[]
  │
  └─ "Assign Rules" button clicked
       │
       ▼
  AssignRulesModal (open)
       │
       ├─ useRules()  ──────────────────►  GET /rules  (org-wide)
       │    └─ allOrgRules[]
       │
       ├─ compute assignableRules = allOrgRules − existingRuleIds
       │
       ├─ [user searches / selects checkboxes]
       │
       └─ "Assign (N)" confirmed
            │
            ├─ for each selectedId:
            │    useMoveRule(id, spaceId)  ──►  PATCH /rules/{id}/move
            │                                    body: { spaceId }
            │
            ├─ Promise.allSettled(...)
            │
            ├─ on success: invalidate ['spaces', spaceId, 'rules']
            └─ on failure: toast with failed rule names
```

### 4.2 AssignToSpaceModal (RuleListPage → Assign)

```
RuleListPage
  │
  ├─ single rule: action menu ⋯ → "Assign to Space"
  │   ruleIds = [rule.id], ruleNames = [rule.name]
  │
  └─ bulk: checkbox selection → bulk action bar → "Assign to Space"
       ruleIds = selectedRows.map(r => r.id)
       ruleNames = selectedRows.map(r => r.name)
       │
       ▼
  AssignToSpaceModal (open)
       │
       ├─ useSpaces()  ──────────────────►  GET /spaces  (org-scoped)
       │    └─ spaces[]
       │
       ├─ [user selects space from dropdown]
       │
       └─ "Assign" confirmed
            │
            ├─ for each ruleId:
            │    useMoveRule(id, selectedSpaceId)  ──►  PATCH /rules/{id}/move
            │                                           body: { spaceId: selectedSpaceId }
            │
            ├─ Promise.allSettled(...)
            │
            ├─ on success: invalidate ['rules']
            └─ on failure: toast with failed rule names
```

---

## 5. API Contracts (Existing — No Changes)

### PATCH /rules/{id}/move

```
PATCH /v1/rules/{ruleId}/move
Authorization: Bearer <token>
Content-Type: application/json

{
  "spaceId": "550e8400-e29b-41d4-a716-446655440000"
}

Response 200:
{
  "id": "...",
  "name": "...",
  "spaceId": "550e8400-e29b-41d4-a716-446655440000",
  ...
}

Response 403: User does not have EDITOR+ role in target space
Response 404: Rule or space not found
```

### GET /rules (org-wide)

```
GET /v1/rules
Authorization: Bearer <token>

Response 200: PageResponse<RuleDto>
{
  "content": [{ "id", "name", "ruleType", "spaceId", "folderPath", "updatedAt", ... }],
  "totalElements": 47,
  ...
}
```

### GET /spaces

```
GET /v1/spaces
Authorization: Bearer <token>

Response 200: [{ "id", "name", "slug", "description", ... }]
```

---

## 6. React Query Cache Strategy

| Query Key | Invalidated On |
|-----------|----------------|
| `['rules']` | After any AssignToSpaceModal confirm |
| `['spaces', spaceId, 'rules']` | After AssignRulesModal confirm |
| `['rules', ruleId]` | Not needed (list invalidation covers it) |

Existing `useMoveRule` hook from Epic 2 already handles invalidation of `['rules']`. `AssignRulesModal` must additionally invalidate `['spaces', spaceId, 'rules']`.

---

## 7. RBAC Enforcement

| Control | VIEWER | EDITOR | OWNER |
|---------|--------|--------|-------|
| "Assign Rules" button on SpaceDetailPage | Hidden | Visible | Visible |
| AssignRulesModal | N/A | Allowed | Allowed |
| "Assign to Space" in rule action menu | Hidden | Visible | Visible |
| "Assign to Space" in bulk action bar | Hidden | Visible | Visible |
| AssignToSpaceModal | N/A | Allowed | Allowed |

Enforcement: check `currentUser.role !== 'VIEWER'` (same pattern as existing Move/Copy buttons).

---

## 8. Error Handling

| Scenario | Behavior |
|----------|----------|
| `GET /rules` fetch fails | Modal shows error state with retry button |
| `GET /spaces` fetch fails | Modal shows error state with retry button |
| Single PATCH fails | Toast: "Failed to assign [Rule Name]: [error message]" |
| All PATCHes fail | Error toast; modal stays open |
| Partial PATCHes fail | Warning toast with list of failed names; modal closes |

---

## 9. Testing Approach

- Unit tests: `AssignRulesModal` and `AssignToSpaceModal` with MSW mocks
- Integration: SpaceDetailPage smoke test (smoke-test.sh section 3.19 to be added in Phase 4)
- RBAC: VIEWER smoke test asserts button absence
- Accessibility: axe-core integration in Vitest for modals

---

*Handoff to UX Designer: Component interfaces and data flows are defined. Design the visual layout and interaction states.*
