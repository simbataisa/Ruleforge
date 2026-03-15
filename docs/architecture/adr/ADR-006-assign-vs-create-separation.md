# ADR-006: Assign-vs-Create Separation — Remove Rule Creation from Space Context

**Status:** ACCEPTED
**Date:** 2026-03-15
**Deciders:** Solution Architect, Tech Lead, Product Owner
**Epic:** Space-Rule Assignment (Epic 4)

---

## Context

`SpaceDetailPage` currently renders a "Create Rule" button that opens `CreateRuleModal` scoped to that space. This was introduced during Epic 1 as a convenience shortcut. As of Epic 2, the Rules page (`RuleListPage`) became the canonical home for rule creation with full support for folder assignment, tagging, and approval workflow integration.

Two interaction models now coexist:

1. **Create from space context** — user is on SpaceDetailPage, clicks "Create Rule", gets a modal that creates a rule already assigned to this space. Bypasses the Rules page workflow.
2. **Create from Rules page** — user navigates to Rules, creates a rule, optionally assigns it to a space.

This duplication creates the following problems:
- Rules created via the space context bypass org-level tagging and approval workflows.
- Users conflate "creating a rule" with "adding a rule to a space" — conceptually different actions.
- The system has no concept of "rule assignment" from the space side; it only has "rule creation that happens to be in a space."
- From the Rules page, there is no way to assign an existing rule to a different space without navigating to that space.

---

## Decision

**Remove rule creation from all space contexts. Spaces are assignment-only containers.**

Concretely:
1. The "Create Rule" button and `CreateRuleModal` invocation are removed from `SpaceDetailPage`.
2. `SpaceDetailPage` replaces the Create button with an "Assign Rules" button that opens `AssignRulesModal` — a modal that lets the user select from already-existing org rules.
3. `RuleListPage` gains an "Assign to Space" action (single + bulk) to support the rule-first assignment flow.
4. Rule creation remains exclusively on `RuleListPage`.

The assignment mechanism uses the existing `PATCH /rules/{id}/move` endpoint — no new backend code.

---

## Alternatives Considered

### Option A: Keep Create in both places; add Assign alongside it

Keep the Create button on SpaceDetailPage but add an Assign button next to it.

**Rejected because:**
- Does not eliminate the conceptual confusion — users still have two paths.
- Does not reduce rule proliferation; users will still hit Create by habit.
- UI becomes cluttered with two competing CTAs.

### Option B: Remove Create from space; no Assign added; rely on Rules page only

Remove the Create button but offer no replacement on SpaceDetailPage. Users must go to the Rules page to assign rules.

**Rejected because:**
- SpaceDetailPage becomes a dead-end read view with no management capability.
- Forces unnecessary navigation for a common task (populating a new space).
- User research (BA brief) identifies this as a key friction point.

### Option C: Chosen — Remove Create; add Assign-only model

Remove Create from space context. Add `AssignRulesModal` on SpaceDetailPage and `AssignToSpaceModal` on RuleListPage.

**Accepted because:**
- Enforces clean mental model: spaces are containers, Rules page is the factory.
- Eliminates duplicate creation path without removing management capability.
- Leverages existing PATCH endpoint — zero backend risk.
- Assignment modal pattern already established in Epic 2 (MoveRuleModal).

---

## Consequences

### Positive

- **Single creation path**: All rule creation flows through `RuleListPage`, ensuring consistent application of org-level metadata (tags, folders, approval workflows).
- **Reduced rule proliferation**: Removing the shortcut eliminates the primary cause of accidental duplicates.
- **Cleaner UX mental model**: Spaces = organizational containers; Rules page = management surface.
- **Bidirectional assignment**: Users can assign rules to spaces from either the space side (AssignRulesModal) or the rule side (AssignToSpaceModal), solving the navigation friction in both directions.
- **No backend changes**: Risk profile is low — one endpoint (`PATCH /rules/{id}/move`) already exists and is tested.

### Negative / Trade-offs

- **Minor regression for space-first users**: Users who habitually created rules from SpaceDetailPage must change workflow. Mitigated by a tooltip on the now-absent button area: "To create rules, visit the Rules page."
- **Multi-PATCH for bulk assignment**: Bulk assignment issues N sequential `PATCH` calls rather than a single atomic backend operation. For N ≤ 50, this is acceptable (< 10s). For larger orgs, a dedicated bulk endpoint would be preferable (deferred to a future epic).
- **Eventual consistency risk**: If a PATCH call fails mid-bulk, some rules are assigned and some are not. `Promise.allSettled` plus partial-failure toast mitigates user confusion but does not provide transactionality.

### Neutral

- `CreateRuleModal` is not deleted from the codebase — it remains in use on `RuleListPage`.
- Existing `MoveRuleModal` and `CopyRuleModal` on `SpaceDetailPage` are unaffected.

---

## Compliance

- RBAC: VIEWER role cannot access Assign controls (same enforcement as Create).
- WCAG 2.1 AA: New modals must meet accessibility requirements (keyboard navigation, ARIA labels, focus trap).
- Audit: Each `PATCH /rules/{id}/move` call is logged by the event-service audit consumer (no changes needed).

---

## Review Notes

- ADR reviewed by Tech Lead: no objections.
- Product Owner confirmed that removing Create from space context is acceptable to stakeholders.
- No data migration required — existing rules and space assignments are unaffected.

---

*Related ADRs: ADR-003 (Space-Scoped Rule API), ADR-004 (Move/Copy Rules)*
