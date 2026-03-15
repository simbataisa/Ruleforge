# PRD — Space-Rule Assignment (Epic 4)

**Prepared by:** Product Owner
**Date:** 2026-03-15
**Epic:** Space-Rule Assignment (Epic 4)
**Input:** `docs/project-brief-space-assignment.md`
**Status:** APPROVED — Proceeding to Architecture Phase

---

## 1. Executive Summary

This PRD defines the scope and acceptance criteria for Epic 4: Space-Rule Assignment. The feature removes the misleading "Create Rule" path from `SpaceDetailPage` and replaces it with an assignment-only model consistent with how spaces are intended to function — as organizational containers, not rule factories. It also closes the gap on the Rules page by adding a direct "Assign to Space" action. No backend changes are required.

---

## 2. RICE Prioritization

| Story | Reach | Impact | Confidence | Effort | RICE Score | Priority |
|-------|-------|--------|------------|--------|------------|----------|
| US-1: Assign existing rules to space (SpaceDetailPage) | 8 | 9 | 9 | 2 | 324 | P0 |
| US-2: Assign rule to space from Rules page (single) | 9 | 8 | 9 | 2 | 324 | P0 |
| US-3: Bulk-assign multiple rules to a space | 6 | 7 | 8 | 3 | 112 | P1 |
| US-4: Remove Create Rule from SpaceDetailPage | 10 | 8 | 10 | 1 | 800 | P0 (enabler) |

*Reach: estimated % of active users affected (10-point scale). Impact: business value. Confidence: certainty. Effort: story points (lower = less effort).*

---

## 3. User Stories

---

### US-1 (P0): Assign Existing Rules to a Space

**As a** space admin,
**I want to** assign existing org rules to a space from the Space Detail page,
**So that** I can populate my space with reusable rules without creating duplicates.

**RICE Score:** 324

#### Acceptance Criteria (Gherkin)

```gherkin
Feature: Assign existing rules to a space

  Background:
    Given I am logged in as a user with EDITOR role or above
    And I am on the Space Detail page for "Underwriting Space"
    And "Underwriting Space" currently contains 2 rules
    And the org has 10 rules total (8 not in this space)

  Scenario: Open Assign Rules modal
    When I click the "Assign Rules" button
    Then a modal titled "Assign Rules to Underwriting Space" opens
    And the modal shows a searchable list of 8 rules not in this space
    And each rule shows its name, type badge, and folder path

  Scenario: Search filters the rule list
    When I click "Assign Rules"
    And I type "credit" in the search box
    Then only rules whose names contain "credit" (case-insensitive) are shown

  Scenario: Select and assign rules
    When I click "Assign Rules"
    And I check the checkbox next to "Credit Score Rule" and "Income Threshold Rule"
    And I click "Assign (2)"
    Then PATCH /rules/{id}/move is called for each selected rule with { "spaceId": "<current-space-id>" }
    And a success toast "2 rules assigned to Underwriting Space" appears
    And the rules list on SpaceDetailPage refreshes to show 4 rules

  Scenario: Cancel discards selection
    When I click "Assign Rules"
    And I select 3 rules
    And I click "Cancel"
    Then the modal closes
    And no PATCH calls are made
    And the rules list is unchanged

  Scenario: VIEWER cannot assign
    Given I am logged in as a user with VIEWER role
    When I view the Space Detail page
    Then the "Assign Rules" button is not visible
```

#### Definition of Done
- [ ] "Create Rule" button removed from SpaceDetailPage
- [ ] "Assign Rules" button renders for EDITOR+ roles
- [ ] `AssignRulesModal` component implemented per UX spec
- [ ] Modal excludes rules already in the current space
- [ ] Search input filters in real-time (no debounce required for < 200 items)
- [ ] Multi-select via checkboxes
- [ ] Confirm button label shows count: "Assign (N)"
- [ ] PATCH called per selected rule; toast on success
- [ ] React Query cache invalidated for space rules list
- [ ] TypeScript strict; no `any`

---

### US-2 (P0): Assign a Rule to a Space from the Rules Page

**As a** rule author,
**I want to** assign a rule to a different space directly from the Rules page,
**So that** I can organize rules without navigating to each individual space.

**RICE Score:** 324

#### Acceptance Criteria (Gherkin)

```gherkin
Feature: Assign rule to space from Rules page (single rule)

  Background:
    Given I am logged in as a user with EDITOR role or above
    And I am on the Rules page
    And "Credit Score Rule" is currently assigned to "Underwriting Space"
    And the org has 3 spaces: "Underwriting Space", "Fraud Space", "Default Space"

  Scenario: Action menu shows Assign to Space
    When I open the action menu (⋯) for "Credit Score Rule"
    Then the menu contains an "Assign to Space" option

  Scenario: Assign to Space opens modal
    When I click "Assign to Space" for "Credit Score Rule"
    Then a modal titled "Assign to Space" opens
    And a space dropdown shows all 3 org spaces
    And the current space "Underwriting Space" is shown but selectable

  Scenario: Confirm assignment
    When I select "Fraud Space" from the dropdown
    And I click "Assign"
    Then PATCH /rules/credit-score-rule-id/move is called with { "spaceId": "<fraud-space-id>" }
    And a success toast "Credit Score Rule assigned to Fraud Space" appears
    And the Rules page list refreshes

  Scenario: Cancel discards
    When I open "Assign to Space" for "Credit Score Rule"
    And I select "Fraud Space"
    And I click "Cancel"
    Then no PATCH call is made

  Scenario: VIEWER role — no action available
    Given I am logged in as a VIEWER
    When I open the action menu for "Credit Score Rule"
    Then "Assign to Space" is not present in the menu
```

#### Definition of Done
- [ ] "Assign to Space" item added to single-rule action dropdown on `RuleListPage`
- [ ] `AssignToSpaceModal` component implemented per UX spec
- [ ] Space dropdown populated from `GET /spaces` (org-scoped)
- [ ] PATCH /rules/{id}/move called on confirm
- [ ] Toast on success; error message on failure
- [ ] React Query cache invalidated for rules list
- [ ] RBAC: hidden for VIEWER role
- [ ] TypeScript strict; no `any`

---

### US-3 (P1): Bulk-Assign Multiple Rules to a Space

**As a** space admin,
**I want to** select multiple rules on the Rules page and assign them all to a space at once,
**So that** I can efficiently populate a new space without performing repeated single-rule operations.

**RICE Score:** 112

#### Acceptance Criteria (Gherkin)

```gherkin
Feature: Bulk assign rules to a space

  Background:
    Given I am logged in as a user with EDITOR role or above
    And I am on the Rules page
    And I have selected 5 rules via the checkbox column

  Scenario: Bulk action bar shows Assign to Space
    When 5 rules are checked
    Then the bulk action bar appears
    And it contains an "Assign to Space" button

  Scenario: Bulk assign opens modal with count
    When I click "Assign to Space" in the bulk action bar
    Then a modal titled "Assign 5 Rules to Space" opens
    And the modal shows the space dropdown

  Scenario: Bulk assign executes all PATCHes
    When I select "Fraud Space" and click "Assign"
    Then PATCH /rules/{id}/move is called for each of the 5 selected rules
    And a success toast "5 rules assigned to Fraud Space" appears
    And checkboxes are cleared

  Scenario: Partial failure
    When 3 of 5 PATCH calls succeed and 2 fail
    Then a warning toast "3 of 5 rules assigned. 2 failed: [Rule A, Rule B]" appears
    And the successful assignments are not rolled back

  Scenario: Deselect after assignment
    When assignment completes (all success)
    Then all checkboxes are cleared
    And the rules list refreshes
```

#### Definition of Done
- [ ] Bulk action bar includes "Assign to Space" when ≥ 1 rules are selected
- [ ] `AssignToSpaceModal` reused with count in title
- [ ] All PATCHes executed; `Promise.allSettled` used (no early abort)
- [ ] Partial failure error enumerated in toast
- [ ] Checkboxes cleared post-assignment
- [ ] Cache invalidated
- [ ] TypeScript strict; no `any`

---

### US-4 (P0 Enabler): Remove Create Rule from SpaceDetailPage

**As an** org owner,
**I want** the "Create Rule" capability removed from the Space Detail page,
**So that** rules are only created through the canonical Rules page workflow.

*(This is an enabler story — no user-facing Gherkin needed beyond US-1's removal assertion. Covered by US-1 DoD.)*

---

## 4. Non-Functional Requirements

| ID | Requirement | Acceptance |
|----|-------------|------------|
| NFR-001 | AssignRulesModal list renders in < 300ms | Lighthouse / React DevTools profiler |
| NFR-002 | Bulk assign 50 rules in < 10s | Manual timing with network throttle off |
| NFR-003 | WCAG 2.1 AA — modal focus trap, ESC closes, aria-labels on search | axe DevTools scan: 0 critical |
| NFR-004 | RBAC strict — VIEWER sees no assign controls | QE verification |
| NFR-005 | Zero TypeScript strict errors | `tsc --noEmit` in CI |
| NFR-006 | Existing Move/Copy/Delete unaffected | Existing smoke test section 3.17 still passes |

---

## 5. Out of Scope

- Creating new rules from any space context (permanently removed)
- Backend bulk-assign endpoint (sequential client PATCHes are sufficient)
- Cross-org rule assignment
- Space permission granularity beyond existing RBAC roles

---

## 6. Dependencies

| Dependency | Status | Owner |
|------------|--------|-------|
| `PATCH /rules/{id}/move` endpoint | Available (Epic 2) | Backend |
| `GET /spaces` endpoint | Available (Epic 1) | Backend |
| `GET /rules` org-wide endpoint | Available (Epic 2) | Backend |
| `useMoveRule` hook | Available (Epic 2) | Frontend |
| `MoveRuleModal` pattern reference | Available (Epic 2) | Frontend |

---

*Handoff to Solution Architect: No backend changes needed. Focus on frontend component design and data flow.*
