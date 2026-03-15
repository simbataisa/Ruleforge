# PRD — Space-Scoped Rule Management

**Date:** 2026-03-14
**Product Owner:** BMAD Product Owner Agent
**Input Artifact:** `docs/project-brief-spaces-rules.md` (v1.0)
**Status:** APPROVED

---

## Changelog

| Version | Date | Author | Change |
|---------|------|--------|--------|
| 1.0 | 2026-03-14 | PO Agent | Initial PRD from project brief |

---

## Alignment Check vs. Project Brief

| Brief FR | Story | Status |
|----------|-------|--------|
| FR-001/002: Space-scoped list & create endpoints (already exist) | US-1, US-2 | ✅ Covered |
| FR-003: Copy with target space | US-3 | ✅ Covered |
| FR-004: Move endpoint | US-3 | ✅ Covered |
| FR-005 to FR-010: SpaceDetailPage rules tab | US-4 | ✅ Covered |
| FR-011/012: Move/Copy modals | US-3 | ✅ Covered |
| FR-013/014: RuleListPage space context | US-2 | ✅ Covered |

**Alignment Result:** PASS — all 14 functional requirements covered by 4 user stories.

---

## Epic Overview

**Epic:** Space-Scoped Rule Management
**Goal:** Close the gap between the data model (space_id exists on rules) and the user experience, enabling space-level governance and rule organization.
**Success Metric:** 100% of rule creation from SpaceDetailPage correctly scoped to that space; SpaceDetailPage bounce rate reduced from ~90% to <20% (users stay and interact with rules).

---

## RICE Prioritization

| Story | Reach | Impact | Confidence | Effort | RICE Score | Priority |
|-------|-------|--------|------------|--------|------------|----------|
| US-4: SpaceDetailPage rules tab | 9 | 3 | 0.9 | 3 | 8.1 | P0 |
| US-2: Space-scoped rule create from SpaceDetailPage | 8 | 3 | 0.95 | 1 | 22.8 | P0 |
| US-1: Space-scoped rule list (URL filter) | 7 | 2 | 0.9 | 1 | 12.6 | P0 |
| US-3: Move/Copy rules between spaces | 6 | 3 | 0.8 | 3 | 4.8 | P1 |

*RICE = (Reach × Impact × Confidence) / Effort. Higher = build first.*

---

## User Stories

---

### US-1: Space-Scoped Rule List in RuleListPage

**RICE Score:** 12.6 | **Priority:** P0 | **MoSCoW:** Must Have

**As a** Rule Author navigating from a Space page,
**I want** the RuleListPage to automatically filter to show only rules in that space,
**So that** I can quickly see what rules exist in a specific business unit without noise from other spaces.

#### Acceptance Criteria (Gherkin)

```gherkin
Feature: Space-scoped rule list via URL parameter

  Scenario: Navigate to RuleListPage with spaceId parameter
    Given I am on SpaceDetailPage for Space "Lending Rules"
    When I click "View All Rules" or navigate to /rules?spaceId=<uuid>
    Then I should see a space context banner "Viewing rules in: Lending Rules"
    And the rule list should show only rules belonging to Space "Lending Rules"
    And I should see a "Clear space filter" option

  Scenario: RuleListPage without spaceId shows all rules
    Given I navigate to /rules without a spaceId parameter
    Then the rule list shows all rules across all spaces (existing behavior)
    And no space context banner is displayed

  Scenario: spaceId in URL is invalid
    Given I navigate to /rules?spaceId=non-existent-uuid
    Then the list shows an error state "Space not found"
    And I can navigate back to /rules to see all rules

  Scenario: Empty space filter
    Given Space "New Space" has no rules
    When I navigate to /rules?spaceId=<new-space-uuid>
    Then I see the empty state "No rules in this space yet. Create the first one."
```

---

### US-2: Space-Scoped Rule Creation

**RICE Score:** 22.8 | **Priority:** P0 | **MoSCoW:** Must Have

**As a** Rule Author working within a specific Space,
**I want** the "Create Rule" button on SpaceDetailPage to automatically create the rule in that space,
**So that** I never accidentally create rules in the wrong space.

#### Acceptance Criteria (Gherkin)

```gherkin
Feature: Create rule scoped to current space

  Scenario: Create rule from SpaceDetailPage
    Given I am on SpaceDetailPage for Space "Lending Rules" (spaceId = "abc-123")
    When I click "Create Rule"
    Then a rule creation form/modal opens
    And the space is pre-selected as "Lending Rules" (not editable from this context)
    When I fill in rule name "Loan Approval Table" and type "TABLE" and submit
    Then the API call is POST /spaces/abc-123/rules
    And the new rule "Loan Approval Table" appears in the space rules list
    And no page navigation occurs (in-page update)

  Scenario: Create rule from RuleListPage in space context
    Given I am on /rules?spaceId=abc-123 (space context banner shown)
    When I click "Create Rule"
    Then the rule creation form uses POST /spaces/abc-123/rules
    And the new rule appears in the filtered list

  Scenario: Create rule from RuleListPage without space context
    Given I am on /rules (no spaceId parameter)
    When I click "Create Rule"
    Then the rule creation form uses POST /rules (existing behavior)
    And the user selects their target space in the form
```

---

### US-3: Move and Copy Rules Between Spaces

**RICE Score:** 4.8 | **Priority:** P1 | **MoSCoW:** Should Have

**As a** Space Admin,
**I want** to move a rule from one space to another, or copy a rule into a different space,
**So that** I can reorganize rules and share rule logic across spaces without manual recreation.

#### Acceptance Criteria (Gherkin)

```gherkin
Feature: Move rule to a different space

  Scenario: Successfully move a rule to another space
    Given Rule "Credit Check" is in Space "Lending Rules"
    And I am viewing the rule in SpaceDetailPage or RuleListPage
    When I click the "Move" action on rule "Credit Check"
    Then MoveRuleModal opens showing rule name "Credit Check"
    And a dropdown shows all available spaces except "Lending Rules"
    When I select "Risk Management" space and click "Move"
    Then the API call is PATCH /rules/<id>/move with body { targetSpaceId: "..." }
    And "Credit Check" is removed from "Lending Rules" rule list
    And a success toast "Rule moved to Risk Management" is shown

  Scenario: Move rule modal cancel
    Given MoveRuleModal is open
    When I click "Cancel"
    Then the modal closes and no changes are made

  Scenario: Copy rule to a different space
    Given Rule "Base Scoring" is in Space "Lending Rules"
    When I click the "Copy" action on rule "Base Scoring"
    Then CopyRuleModal opens with default new name "Copy of Base Scoring"
    And a dropdown shows all available spaces (including current)
    When I change the name to "Base Scoring - Risk" and select "Risk Management" space
    And click "Copy"
    Then the API call is POST /rules/<id>/copy with body { targetSpaceId: "...", newName: "Base Scoring - Risk" }
    And a success toast "Rule copied to Risk Management" is shown
    And the source rule "Base Scoring" remains in "Lending Rules"

  Scenario: No other spaces available for move
    Given the user's organization has only one space
    When I click "Move" on any rule
    Then the MoveRuleModal shows message "No other spaces available"
    And the "Move" button is disabled
```

---

### US-4: SpaceDetailPage Rules Tab

**RICE Score:** 8.1 | **Priority:** P0 | **MoSCoW:** Must Have

**As a** Space Admin or Rule Author,
**I want** SpaceDetailPage to show all rules in the space with search, filter, and sort,
**So that** I can understand the rule landscape for my space without navigating away.

#### Acceptance Criteria (Gherkin)

```gherkin
Feature: SpaceDetailPage shows rules belonging to the space

  Scenario: Load SpaceDetailPage with rules
    Given Space "Lending Rules" has 5 rules (2 TABLE, 2 TREE, 1 SCORECARD)
    When I navigate to SpaceDetailPage for "Lending Rules"
    Then I see a rules section with 5 rule cards/rows
    And each row shows: rule name, type badge, status badge, version number, last updated
    And I see action buttons: Edit (pencil), Copy, Move, Delete

  Scenario: Search rules by name
    Given I am on SpaceDetailPage showing 5 rules
    When I type "Credit" in the search input
    Then only rules with "Credit" in the name are shown
    And the count updates to reflect filtered results

  Scenario: Filter by rule type
    Given I am on SpaceDetailPage showing 5 rules (2 TABLE, 2 TREE, 1 SCORECARD)
    When I select "TABLE" from the type filter
    Then only 2 TABLE rules are shown

  Scenario: Filter by status
    Given rules include: 3 PUBLISHED, 1 DRAFT, 1 ARCHIVED
    When I select "DRAFT" from the status filter
    Then only the 1 DRAFT rule is shown

  Scenario: Empty space (no rules)
    Given Space "New Space" has no rules
    When I navigate to SpaceDetailPage for "New Space"
    Then I see the empty state: "No rules in this space yet"
    And a "Create Rule" button is prominently shown

  Scenario: Loading state
    Given the rules API call is in-flight
    When I navigate to SpaceDetailPage
    Then I see a loading skeleton/spinner in the rules section

  Scenario: Error state
    Given the rules API call fails (5xx)
    When SpaceDetailPage loads
    Then I see an error message with a "Retry" button
```

---

## Non-Functional Requirements

| ID | Requirement | Metric | Priority |
|----|-------------|--------|----------|
| NFR-001 | Space rule list response time | <500ms p95 for up to 1000 rules | P0 |
| NFR-002 | Move/Copy operations | <1000ms p95 | P1 |
| NFR-003 | Tenant isolation | Move/Copy must not allow cross-tenant operations; enforced by RLS | P0 |
| NFR-004 | Accessibility | All new modals WCAG 2.1 AA; keyboard accessible, screen-reader labels | P1 |
| NFR-005 | Backward compatibility | GET /rules and POST /rules must continue to work as-is | P0 |

---

## Out of Scope (Won't Have — v1)

- Bulk move/copy (move multiple rules at once)
- Drag-and-drop rule reordering within a space
- Space-level rule permissions (beyond existing RBAC)
- Rule templates scoped to a space

---

## Release Plan

**MVP (this sprint):**
- US-4: SpaceDetailPage rules tab (P0, highest RICE-adjusted value for users)
- US-2: Space-scoped rule create (P0)
- US-1: RuleListPage space context mode (P0)

**v1.1 (next sprint):**
- US-3: Move/Copy rules between spaces (P1)

**Rationale:** US-3 requires a backend endpoint that may need implementation; MVP can be delivered with existing endpoints. US-3 adds moderate backend complexity and can ship separately.

---

## Success Metrics

| Metric | Baseline | Target | Measurement |
|--------|----------|--------|-------------|
| Rules created in correct space | ~60% (estimated) | >99% | API logs: space_id matches user's current space context |
| SpaceDetailPage bounce rate | ~90% (no content) | <25% | Frontend analytics: time-on-page, navigation events |
| "Wrong space" support tickets | N/A (not tracked) | 0 after launch | Support ticket tagging |
| Space rule list load time | N/A | <500ms p95 | APM traces |
