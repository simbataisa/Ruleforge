# Project Brief — Space-Rule Assignment (Epic 4)

**Prepared by:** Business Analyst
**Date:** 2026-03-15
**Epic:** Space-Rule Assignment (Epic 4)
**Status:** APPROVED — Proceeding to PO Phase

---

## 1. Problem Statement

### 1.1 Current Pain Points

**Problem 1: Rule duplication via SpaceDetailPage**
The `SpaceDetailPage` currently exposes a "Create Rule" button that opens `CreateRuleModal` scoped to that space. This creates a parallel rule-creation path that bypasses the canonical `Rules` page workflow. Consequences:
- Rules created from a space context bypass org-level tagging, folder assignment, and approval workflows that the Rules page enforces.
- Users create net-new rules when they intend to reuse an existing one, causing rule proliferation and version drift.
- There is no visual indication on the Rules page that a duplicate exists in another space.

**Problem 2: No assignment flow from the Rules page**
Users who manage rules centrally from the `RuleListPage` have no way to assign a rule to a different space without navigating away to that space's detail page. The existing "Move" action on `SpaceDetailPage` is space-first (you must know which space you're in), not rule-first (start from the rule, pick the destination space).

**Problem 3: UX inconsistency**
The mental model is broken: spaces are containers for organizing rules, not factories for creating them. The Create button on `SpaceDetailPage` contradicts this model and confuses new users who expect rule creation to live on the Rules page.

### 1.2 Business Impact

| Impact | Description |
|--------|-------------|
| Rule proliferation | Estimated 15-30% of rules created via spaces are duplicates of existing rules |
| Support cost | Confusion about "which copy is canonical" generates recurring support requests |
| Data quality | Duplicate rules diverge over time — fixes applied to one are not applied to the other |
| Onboarding friction | New users don't discover assignment patterns; they default to creation |

---

## 2. Scope

### 2.1 In Scope

1. **Remove rule creation from SpaceDetailPage** — remove the "Create Rule" button and `CreateRuleModal` invocation from the space context entirely.
2. **Add "Assign Rules" to SpaceDetailPage** — replace the Create button with an "Assign Rules" button that opens a new `AssignRulesModal` to select existing org rules.
3. **Add "Assign to Space" to RuleListPage** — surface an action in the single-rule action menu and the bulk-selection action bar to assign one or more rules to a chosen space.
4. **AssignRulesModal** — searchable, filterable list of org rules not yet in the current space; multi-select; confirm triggers one PATCH per selected rule.
5. **AssignToSpaceModal** — space picker (dropdown of all org spaces); confirms assignment via the existing `PATCH /rules/{id}/move` endpoint.

### 2.2 Out of Scope

- Backend API changes (existing `PATCH /rules/{id}/move` is sufficient)
- Rule creation workflow changes on the Rules page
- Space permission model changes
- Rule copy-to-space functionality (covered by existing CopyRuleModal; not removed)

### 2.3 Assumptions

- `PATCH /rules/{id}/move` with `{ spaceId }` body atomically reassigns a rule's space. This is confirmed from Epic 2.
- `GET /rules` (org-wide, no space filter) returns the full assignable rule pool.
- `GET /spaces` returns all spaces the current user can see in the org.
- A rule can belong to exactly one space at a time (1:N, not junction table).
- Bulk assignment is sequential client-side (one PATCH per rule); no bulk backend endpoint required.

---

## 3. Stakeholders

| Role | Name | Interest |
|------|------|----------|
| Space Admin | — | Can assign existing rules to their space without creating duplicates |
| Rule Author | — | Can organize rules into spaces from the Rules page |
| Org Owner | — | Reduces rule proliferation, improves governance |
| Developer | — | Clear single path for rule creation; assignment is a distinct concern |

---

## 4. Functional Requirements

| ID | Requirement | Priority |
|----|-------------|----------|
| FR-001 | The "Create Rule" button MUST be removed from SpaceDetailPage | P0 |
| FR-002 | SpaceDetailPage MUST have an "Assign Rules" button visible to users with EDITOR role or above | P0 |
| FR-003 | AssignRulesModal MUST display all org rules not already assigned to the current space | P0 |
| FR-004 | AssignRulesModal MUST support text search filtering rules by name | P0 |
| FR-005 | AssignRulesModal MUST support multi-select (checkbox per rule) | P0 |
| FR-006 | AssignRulesModal MUST show rule type badge, folder path, and last-modified date per rule | P1 |
| FR-007 | Confirming AssignRulesModal MUST call PATCH /rules/{id}/move for each selected rule | P0 |
| FR-008 | RuleListPage single-rule action menu MUST include "Assign to Space" option | P0 |
| FR-009 | RuleListPage bulk action bar MUST include "Assign to Space" when ≥1 rules are selected | P0 |
| FR-010 | AssignToSpaceModal MUST show a dropdown of all accessible org spaces | P0 |
| FR-011 | AssignToSpaceModal MUST confirm before executing assignment | P0 |
| FR-012 | AssignToSpaceModal MUST call PATCH /rules/{id}/move for each selected rule | P0 |
| FR-013 | After successful assignment, the rules list MUST refresh (invalidate React Query cache) | P0 |
| FR-014 | Partial failure in bulk assignment MUST surface an error listing which rules failed | P1 |

---

## 5. Non-Functional Requirements

| ID | Requirement | Target |
|----|-------------|--------|
| NFR-001 | Assignment modal opens in < 300ms (rules list already cached by React Query) | < 300ms |
| NFR-002 | Bulk assignment of 50 rules completes in < 10s (sequential PATCH calls) | < 10s |
| NFR-003 | WCAG 2.1 AA compliance — modals must be keyboard navigable and screen-reader labelled | AA |
| NFR-004 | RBAC enforced — VIEWER role sees no "Assign Rules" or "Assign to Space" controls | Strict |
| NFR-005 | No regression in existing Move/Copy/Delete actions on SpaceDetailPage | Zero regression |
| NFR-006 | TypeScript strict mode — no `any` types in new components | 0 errors |

---

## 6. Feasibility Assessment

| Dimension | Assessment | Notes |
|-----------|------------|-------|
| Technical | GREEN | No backend changes; existing endpoint covers all cases |
| UX | GREEN | Established modal patterns from Epic 2 (MoveRuleModal) to follow |
| Timeline | GREEN | 2 new components + 2 page modifications; estimated 2-3 days engineering |
| Risk | LOW | Removing a button is non-breaking; existing data unaffected |

---

## 7. KPIs

| KPI | Baseline | Target (30 days post-release) |
|-----|----------|-------------------------------|
| Duplicate rules created via space context | ~15-30% of space-created rules | 0% (path removed) |
| "Assign to Space" usage from Rules page | 0 (feature doesn't exist) | > 40% of rule space changes |
| Support tickets about "which copy is canonical" | ~3/month | < 1/month |
| Rules page assignment completion rate | N/A | > 85% of assignment modal sessions complete |

---

## 8. Risks and Mitigations

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| Users confused by removal of Create button | Medium | Medium | In-place tooltip: "To create rules, visit the Rules page" |
| PATCH /move endpoint rate-limited under bulk load | Low | Medium | Sequential calls with delay; surface partial errors |
| AssignRulesModal rule list pagination needed for large orgs | Low | Low | React Query pagination or virtual scroll (P1 enhancement) |

---

*Handoff to Product Owner: Apply RICE prioritization and write Gherkin acceptance criteria.*
