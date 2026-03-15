# UI Specification — Space-Scoped Rule Management

**Date:** 2026-03-14
**Designer:** BMAD UX/UI Designer Agent
**Input:** `docs/prd-spaces-rules.md` (v1.0), `docs/architecture/solution-architecture-spaces-rules.md` (v1.0)
**Status:** APPROVED

---

## Changelog

| Version | Date | Author | Change |
|---------|------|--------|--------|
| 1.0 | 2026-03-14 | UX Agent | Initial UI spec |

---

## 1. Design Principles

- **Contextual clarity**: Users should always know which space they're operating in.
- **Progressive disclosure**: Actions (move, copy, delete) appear in a context menu; primary actions (create, search) are always visible.
- **Familiar patterns**: Reuse existing Modal, Button, Badge, Input, Select, Spinner, EmptyState components.
- **Accessible**: WCAG 2.1 AA — keyboard navigation, ARIA labels, focus management in modals.

---

## 2. SpaceDetailPage — Rules Section

### 2.1 Layout

The existing `SpaceDetailPage` retains its header, breadcrumb, and metadata section. Below the existing content, a new **Rules Section** is added:

```
┌─────────────────────────────────────────────────────────────────┐
│ [← Back]  Space: Lending Rules                    [Settings] [⋯] │
│ slug: lending-rules  ·  Created March 14, 2026                   │
├─────────────────────────────────────────────────────────────────┤
│ Description: Rules for evaluating lending applications          │
├─────────────────────────────────────────────────────────────────┤
│  RULES                                    [+ Create Rule]        │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │ 🔍 Search rules...     [Type ▼]  [Status ▼]             │    │
│  └─────────────────────────────────────────────────────────┘    │
│                                                                   │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │ Name               Type      Status       Version  Actions │  │
│  ├────────────────────────────────────────────────────────────┤  │
│  │ Credit Check       TABLE  ● PUBLISHED  v3   [✏] [⋯]      │  │
│  │ Loan Approval Tree TREE   ● DRAFT      v1   [✏] [⋯]      │  │
│  │ Risk Scorecard     SCORE  ● PUBLISHED  v2   [✏] [⋯]      │  │
│  └────────────────────────────────────────────────────────────┘  │
│                                                                   │
│  Showing 3 rules                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### 2.2 Rules Section Header

- **Section title**: "Rules" (h2, `text-lg font-semibold text-gray-900`)
- **Create Rule button**: Primary button, top-right of section (`+ Create Rule`)
  - Action: Opens existing `CreateRuleModal` with `spaceId` pre-set and locked
  - API: `POST /v1/spaces/{spaceId}/rules`

### 2.3 Search & Filter Bar

- **Search input**: Full-width (or 50% width), placeholder "Search rules by name..."
  - Client-side filter (instant, no API call for small lists <100 rules)
  - For large lists (>100 rules), debounced 300ms then API call with `?search=` param
- **Type dropdown**: Multi-select or single-select. Options: All Types, TABLE, TREE, SCORECARD, FLOW, SCRIPT, DMN, CEP, ENSEMBLE
  - Filters rule list in-place
- **Status dropdown**: Options: All Statuses, DRAFT, PUBLISHED, ARCHIVED, PENDING_APPROVAL
  - Filters rule list in-place

### 2.4 Rule Row

Each rule is displayed as a table row:

| Column | Content | Width |
|--------|---------|-------|
| Name | Rule name (bold), description truncated below (optional) | 40% |
| Type | Badge with rule type color | 12% |
| Status | Badge with status color | 14% |
| Version | "v{N}" text (gray) | 8% |
| Updated | Relative time (e.g., "2 days ago") | 14% |
| Actions | Edit icon + dropdown menu | 12% |

**Badge Colors (consistent with existing):**
- TABLE: blue
- TREE: purple
- SCORECARD: orange
- FLOW: teal
- SCRIPT: yellow
- DRAFT: gray
- PUBLISHED: green
- ARCHIVED: red/muted
- PENDING_APPROVAL: amber

**Action Dropdown (⋯ menu):**
```
┌────────────┐
│ ✏ Edit     │  → navigate to rule editor
│ 📋 Copy    │  → opens CopyRuleModal
│ ↗ Move     │  → opens MoveRuleModal
│ ─────────  │
│ 🗑 Delete  │  → opens ConfirmDialog
└────────────┘
```

### 2.5 Empty State

When no rules exist (or filters return 0):

```
┌────────────────────────────────────────────────┐
│                                                 │
│         📄 No rules in this space yet           │
│                                                 │
│   Create your first rule to get started.        │
│                                                 │
│              [+ Create Rule]                    │
│                                                 │
└────────────────────────────────────────────────┘
```

If filters are applied and 0 results:
```
No rules match your filters. [Clear filters]
```

### 2.6 Loading State

While fetching rules: Show skeleton rows (3 ghost rows) with animated shimmer.

### 2.7 Error State

If API call fails:
```
Failed to load rules. [Retry]
```

---

## 3. MoveRuleModal

### 3.1 Trigger

Clicking "Move" in the rule action dropdown.

### 3.2 Modal Layout

```
┌──────────────────────────────────────────────┐
│  Move Rule                              [×]   │
├──────────────────────────────────────────────┤
│                                              │
│  Move "Credit Check" to another space.       │
│                                              │
│  Target Space                                │
│  ┌──────────────────────────────────────┐   │
│  │ Select a space...                  ▼ │   │
│  └──────────────────────────────────────┘   │
│                                              │
│  ⚠ The rule will be removed from the         │
│    current space.                            │
│                                              │
│              [Cancel]  [Move Rule]           │
└──────────────────────────────────────────────┘
```

### 3.3 Props

```typescript
interface MoveRuleModalProps {
  ruleId: string;
  ruleName: string;
  currentSpaceId: string;
  onClose: () => void;
  onSuccess: () => void;
}
```

### 3.4 Behavior

- Space dropdown: loads all spaces from `useSpaces()`, excludes `currentSpaceId`
- "Move Rule" button: disabled until a target space is selected
- On confirm: calls `PATCH /v1/rules/{id}/move { targetSpaceId }`
- On success: close modal, call `onSuccess()`, show toast "Rule moved to {targetSpaceName}"
- On error: show inline error message below the dropdown
- Loading state: button shows spinner + "Moving..."
- If no other spaces exist: show "No other spaces available" message; Move button disabled

### 3.5 Accessibility

- Modal has `role="dialog"`, `aria-labelledby` pointing to title
- Focus trapped inside modal
- Escape key closes modal (calls onClose)
- "Move Rule" button has descriptive `aria-label="Move {ruleName} to selected space"`

---

## 4. CopyRuleModal

### 4.1 Trigger

Clicking "Copy" in the rule action dropdown.

### 4.2 Modal Layout

```
┌──────────────────────────────────────────────┐
│  Copy Rule                              [×]   │
├──────────────────────────────────────────────┤
│                                              │
│  Create a copy of "Credit Check".            │
│                                              │
│  New Name                                    │
│  ┌──────────────────────────────────────┐   │
│  │ Copy of Credit Check                 │   │
│  └──────────────────────────────────────┘   │
│                                              │
│  Target Space                                │
│  ┌──────────────────────────────────────┐   │
│  │ Lending Rules (current)            ▼ │   │
│  └──────────────────────────────────────┘   │
│                                              │
│  ℹ The original rule will not be changed.    │
│                                              │
│              [Cancel]  [Copy Rule]           │
└──────────────────────────────────────────────┘
```

### 4.3 Props

```typescript
interface CopyRuleModalProps {
  ruleId: string;
  ruleName: string;
  currentSpaceId: string;
  onClose: () => void;
  onSuccess: () => void;
}
```

### 4.4 Behavior

- Name field: pre-filled with "Copy of {ruleName}", editable, required
- Space dropdown: loads all spaces, default = current space
- "Copy Rule" button: disabled if name is empty
- On confirm: calls `POST /v1/rules/{id}/copy { name, targetSpaceId }`
- On success: close modal, call `onSuccess()`, show toast "Rule copied to {targetSpaceName}"
- On error: show inline error message
- Loading state: button shows spinner + "Copying..."

---

## 5. RuleListPage — Space Context Mode

### 5.1 Space Context Banner

When `?spaceId=<uuid>` is in the URL:

```
┌────────────────────────────────────────────────────────────────────┐
│  📁 Viewing rules in: Lending Rules          [View Space] [×Clear] │
└────────────────────────────────────────────────────────────────────┘
```

- Background: `bg-blue-50 border-b border-blue-200`
- "View Space": link to `/spaces/{spaceId}`
- "× Clear": removes `?spaceId=` param, returns to org-wide view
- Space name loaded from `useSpace(spaceId)` hook

### 5.2 Create Rule in Space Context

When space context is active, "Create Rule" button:
- Opens `CreateRuleModal` with `spaceId` pre-set (not editable)
- Uses `POST /v1/spaces/{spaceId}/rules` API

### 5.3 Default Behavior (No spaceId)

No changes. Existing behavior preserved.

---

## 6. Component Map

| New/Modified | File Path | Dependencies |
|-------------|-----------|--------------|
| NEW | `features/spaces/components/MoveRuleModal.tsx` | Modal, Button, Select, Spinner from shared |
| NEW | `features/spaces/components/CopyRuleModal.tsx` | Modal, Button, Input, Select, Spinner from shared |
| MODIFIED | `features/spaces/pages/SpaceDetailPage.tsx` | useSpaceRules, MoveRuleModal, CopyRuleModal |
| MODIFIED | `features/rules/pages/RuleListPage.tsx` | useSearchParams (RRv6), useSpace |
| MODIFIED | `api/rules.api.ts` | useSpaceRules, useCreateSpaceRule, useMoveRule, useCopyRule (extended) |

---

## 7. Cross-Review Results (SA ↔ UX)

| UI Action | API Endpoint | Status |
|-----------|-------------|--------|
| SpaceDetailPage rule list | `GET /v1/spaces/{id}/rules` | ✅ Match |
| Create Rule button | `POST /v1/spaces/{id}/rules` | ✅ Match |
| MoveRuleModal submit | `PATCH /v1/rules/{id}/move` | ✅ Match (new endpoint) |
| CopyRuleModal submit | `POST /v1/rules/{id}/copy { name, targetSpaceId }` | ✅ Match (extended DTO) |
| RuleListPage space filter | `GET /v1/spaces/{id}/rules` | ✅ Match |
| Space dropdown in modals | `GET /v1/spaces` (useSpaces hook) | ✅ Exists |

**Cross-Review Result: 0 gaps. All UI actions have matching API contracts.**
