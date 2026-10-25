# Product Requirements Document — Spaces Page Features

## Changelog

- 2026-10-25: Retarget the original Spaces input to its preserved feature brief after correcting the whole-product brief.

- 2026-10-25: Add an unapproved brownfield product-inventory supplement from README and source history; preserve the original approved Spaces scope and prioritization.

**Version:** 1.0
**Date:** 2026-03-14
**Author:** BMAD Product Owner Agent (Phase 2)
**Input:** [Spaces Page Features brief](project-brief-spaces-page-features.md) — original March 2026 feature input. The current [project brief](project-brief.md) describes the whole RuleForge product.
**Status:** APPROVED — Ready for Architecture

---

## Executive Summary

This PRD defines the full feature set required to deliver a production-complete Spaces experience in RuleForge. Spaces are the primary workspace unit (Org → Space → Folder → Rule), yet the current UI supports only listing and creating spaces. This document captures 7 user stories covering CRUD operations, space detail/settings pages, approval settings UI, and backend data enrichment — all required to unblock enterprise workspace administration.

---

## Goals

1. Deliver a complete Space CRUD lifecycle (create, view, edit, delete)
2. Expose a dedicated Space Detail Page as the workspace entry point
3. Expose a Space Settings Page for name/description management and approval policy
4. Enrich the Space API with `slug`, `ruleCount`, `description`, `updatedAt`
5. Fix broken OrgSettings navigation ("Manage" → Space Settings)

---

## Non-Functional Requirements

| NFR | Requirement | Acceptance Criteria |
|---|---|---|
| Performance | Space list and detail render <200ms | Measured at p95 under normal load |
| RBAC | DELETE restricted to OWNER; PUT restricted to OWNER/ADMIN | Returns 403 for insufficient role |
| Error Handling | Slug collision returns 409 Conflict | Response body includes error message |
| Cascade Safety | Delete blocked if space has rules, OR explicit cascade warning shown | No silent data loss |
| Type Safety | No `any` types in new/modified TypeScript | ESLint no-explicit-any passes |
| Accessibility | New pages meet WCAG 2.1 AA | Keyboard navigable, ARIA labels present |
| Test Coverage | All new endpoints covered by smoke tests | Section 3.16 in smoke-test.sh all PASS |

---

## User Stories & Acceptance Criteria

### US-1 — View All Spaces (Enhanced List)

**As a** rule author or space admin,
**I want** to see all spaces in my organization with description and rule count,
**So that** I can quickly assess space contents and navigate to the right workspace.

**RICE Score:**
- Reach: 100 (all org users see list)
- Impact: 3x (foundational navigation)
- Confidence: 100%
- Effort: 2 days
- **Score: 150** → **P0**

**Acceptance Criteria:**

```gherkin
Feature: Enhanced Spaces List

  Scenario: Space cards show enriched metadata
    Given I am logged in as any org member
    When I navigate to /spaces
    Then I see a card for each space in my org
    And each card displays the space name
    And each card displays the space description (or "No description" placeholder)
    And each card displays a rule count badge (e.g. "12 rules")
    And each card has a clickable "Open" button

  Scenario: Space card actions visible to admins
    Given I am logged in as OWNER or ADMIN
    When I view the spaces list
    Then each card has a "Settings" action button
    And each card has a "Delete" action button (with trash icon)

  Scenario: Empty spaces list
    Given I am logged in as any org member
    And the org has no spaces
    When I navigate to /spaces
    Then I see an empty state message "No spaces yet"
    And I see a "Create Space" button
```

---

### US-2 — Create a Space (with Slug Auto-Generation)

**As a** space admin or org owner,
**I want** to create a new space with a name and optional description,
**So that** a slug is auto-generated and the space is immediately available.

**RICE Score:**
- Reach: 20 (admins/owners only)
- Impact: 3x (core workflow)
- Confidence: 100%
- Effort: 1 day
- **Score: 60** → **P0**

**Acceptance Criteria:**

```gherkin
Feature: Space Creation with Slug

  Scenario: Create space generates slug
    Given I am logged in as OWNER or ADMIN
    When I create a space with name "My First Space"
    Then the API returns 201 Created
    And the response includes slug "my-first-space"
    And the response includes description (null or provided value)
    And the space appears in the spaces list

  Scenario: Duplicate name returns conflict
    Given a space named "Analytics" already exists in my org
    When I try to create another space named "Analytics"
    Then the API returns 409 Conflict
    And the error message says "A space with this name already exists"
    And the create modal shows the error inline

  Scenario: Name with special characters slugifies correctly
    When I create a space named "Q4 Sales & Marketing!"
    Then the slug is "q4-sales-marketing"
```

---

### US-3 — Edit Space Name and Description

**As a** space admin or org owner,
**I want** to update a space's name and/or description,
**So that** I can correct mistakes or evolve the space's purpose over time.

**RICE Score:**
- Reach: 20
- Impact: 2x
- Confidence: 100%
- Effort: 1.5 days
- **Score: 27** → **P0**

**Acceptance Criteria:**

```gherkin
Feature: Space Editing

  Scenario: Edit space name via settings page
    Given I am logged in as OWNER or ADMIN
    And I am on /spaces/:id/settings
    When I change the name to "Updated Space Name"
    And I click Save
    Then the API calls PUT /v1/organizations/current/spaces/:id
    And the API returns 200 OK
    And the page shows the updated name
    And the space list refreshes to show the new name

  Scenario: Edit rejected for non-admin
    Given I am logged in as a VIEWER role
    When I call PUT /v1/organizations/current/spaces/:id
    Then the API returns 403 Forbidden

  Scenario: Slug updates when name changes
    When I update a space name from "Old Name" to "New Name"
    Then the response includes slug "new-name"
```

---

### US-4 — Delete a Space

**As an** org owner,
**I want** to delete a space after confirming the action,
**So that** I can remove unused workspaces and keep the org clean.

**RICE Score:**
- Reach: 10
- Impact: 2x
- Confidence: 90%
- Effort: 1.5 days
- **Score: 12** → **P0**

**Acceptance Criteria:**

```gherkin
Feature: Space Deletion

  Scenario: Delete empty space succeeds
    Given I am logged in as OWNER
    And the space has no rules or folders
    When I click Delete on the space
    And I confirm the deletion in the ConfirmDialog
    Then the API calls DELETE /v1/organizations/current/spaces/:id
    And the API returns 204 No Content
    And the space no longer appears in the list
    And I am redirected to /spaces

  Scenario: Delete blocked for non-empty space
    Given the space contains at least one rule or folder
    When I call DELETE /v1/organizations/current/spaces/:id
    Then the API returns 409 Conflict
    And the error message includes "This space contains rules and cannot be deleted"

  Scenario: Delete rejected for non-owner
    Given I am logged in as ADMIN (not OWNER)
    When I call DELETE /v1/organizations/current/spaces/:id
    Then the API returns 403 Forbidden

  Scenario: ConfirmDialog shows space name
    When I click Delete on a space named "Production Rules"
    Then the ConfirmDialog shows "Delete 'Production Rules'?"
    And the dialog explains this action cannot be undone
```

---

### US-5 — View Space Detail Page

**As a** rule author,
**I want** to open a dedicated space page showing its stats and navigation,
**So that** I have a clear workspace landing page before accessing rules.

**RICE Score:**
- Reach: 100
- Impact: 2x
- Confidence: 100%
- Effort: 2 days
- **Score: 100** → **P0**

**Acceptance Criteria:**

```gherkin
Feature: Space Detail Page

  Scenario: Space detail page renders
    Given I am logged in as any org member
    When I navigate to /spaces/:id
    Then I see a breadcrumb "Spaces > {space name}"
    And I see the space name as the page heading
    And I see a stats bar with rule count and created date
    And I see a "Settings" button that links to /spaces/:id/settings

  Scenario: Unknown space ID shows 404 state
    When I navigate to /spaces/non-existent-id
    Then I see a "Space not found" message
    And I see a "Back to Spaces" link

  Scenario: Loading state shown while fetching
    When I navigate to /spaces/:id
    Then a loading spinner is shown until the API responds
```

---

### US-6 — View and Configure Space Approval Settings

**As a** space admin or org owner,
**I want** to view and configure the space's approval policy (approvers, SLA, require-all),
**So that** I can enforce governance on rule changes within the space.

**RICE Score:**
- Reach: 15
- Impact: 2x
- Confidence: 80%
- Effort: 2 days
- **Score: 12** → **P1**

**Acceptance Criteria:**

```gherkin
Feature: Space Approval Settings UI

  Scenario: View approval settings
    Given I am logged in as OWNER or ADMIN
    And I am on /spaces/:id/settings
    When I click the "Approval Settings" tab
    Then I see the current approver list
    And I see the SLA hours field
    And I see the "Require All Approvers" toggle

  Scenario: Update approval settings
    When I change the SLA to 48 hours and click Save
    Then the API calls PUT /v1/spaces/:id/approval-settings
    And the API returns 200 OK
    And the tab shows the updated SLA value

  Scenario: Approval settings tab hidden for non-admins
    Given I am logged in as VIEWER
    When I visit /spaces/:id/settings
    Then I do not see the "Approval Settings" tab
```

---

### US-7 — Navigate from OrgSettings to Space

**As an** org owner managing spaces from the OrgSettings page,
**I want** the "Manage" button on each space in OrgSettings to navigate to the space settings page,
**So that** I have a consistent path to manage any space from the admin area.

**RICE Score:**
- Reach: 10
- Impact: 1x
- Confidence: 100%
- Effort: 0.5 days
- **Score: 20** → **P0**

**Acceptance Criteria:**

```gherkin
Feature: OrgSettings Space Navigation

  Scenario: Manage button navigates to space settings
    Given I am logged in as OWNER
    And I am on the Organization Settings page
    When I click "Manage" for a space
    Then I am navigated to /spaces/:id/settings
    And the Space Settings page renders correctly

  Scenario: Manage button present for each space
    Given the org has 3 spaces
    When I view the Spaces section of OrgSettings
    Then each space row has a "Manage" button
```

---

## RICE Prioritization Summary

| Story | Reach | Impact | Confidence | Effort | Score | Priority |
|---|---|---|---|---|---|---|
| US-1: View all spaces (enhanced) | 100 | 3x | 100% | 2d | 150 | **P0** |
| US-5: Space detail page | 100 | 2x | 100% | 2d | 100 | **P0** |
| US-2: Create space with slug | 20 | 3x | 100% | 1d | 60 | **P0** |
| US-3: Edit space | 20 | 2x | 100% | 1.5d | 27 | **P0** |
| US-7: OrgSettings Manage button | 10 | 1x | 100% | 0.5d | 20 | **P0** |
| US-4: Delete space | 10 | 2x | 90% | 1.5d | 12 | **P0** |
| US-6: Approval settings UI | 15 | 2x | 80% | 2d | 12 | **P1** |

**MVP (this sprint):** US-1, US-2, US-3, US-4, US-5, US-7 — all P0 stories
**v1.1:** US-6 (Approval Settings UI) — deferred P1, endpoints already exist

---

## Functional Requirements Traceability

| FR-ID | Requirement | User Story | Priority |
|---|---|---|---|
| FR-01 | Cards show description + ruleCount | US-1 | P0 |
| FR-02 | Settings action → `/spaces/:id/settings` | US-1 | P0 |
| FR-03 | Open action → `/spaces/:id` | US-1 | P0 |
| FR-04 | Delete action + ConfirmDialog | US-4 | P0 |
| FR-05 | Edit name/description via modal | US-3 | P0 |
| FR-06 | SpaceDetailPage with breadcrumb | US-5 | P0 |
| FR-07 | Stats bar: ruleCount, createdAt | US-5 | P0 |
| FR-08 | Link to Space Settings | US-5 | P0 |
| FR-09 | Loading/empty/error states | US-1, US-5 | P0 |
| FR-10 | General tab: name/description form | US-3 | P0 |
| FR-11 | Approval Settings tab UI | US-6 | P1 |
| FR-12 | Save approval settings | US-6 | P1 |
| FR-13 | Danger Zone: delete + ConfirmDialog | US-4 | P0 |
| FR-14 | Delete redirects to /spaces | US-4 | P0 |
| FR-15 | PUT endpoint (update space) | US-3 | P0 |
| FR-16 | DELETE endpoint (delete space) | US-4 | P0 |
| FR-17 | GET /:id (single space, enriched) | US-5 | P0 |
| FR-18 | SpaceDto enrichment | US-1, US-2, US-3 | P0 |
| FR-19–21 | Slug generation + unique constraint | US-2 | P0 |
| FR-22 | OrgSettings "Manage" button fixed | US-7 | P0 |

---

## Out of Scope (This Sprint)

- Space member management (invite/remove users from a space)
- Space-level role assignment beyond org roles
- Space activity feed / audit log filtered by space
- Space templates or cloning

---

## Dependencies

| Dependency | Owner | Status |
|---|---|---|
| `ConfirmDialog` component | Frontend (shared) | ✅ Already exists |
| `space_approval_settings` endpoints | Management service | ✅ Already exist |
| V047 DB migration (slug column) | Backend Engineer | 🔲 Required |
| PUT + DELETE endpoints in auth-service | Backend Engineer | 🔲 Required |
| Updated SpaceDto | Backend Engineer | 🔲 Required |

---

## Success Metrics

- All smoke test Section 3.16 assertions pass (0 FAIL)
- `/spaces`, `/spaces/:id`, `/spaces/:id/settings` pages render without console errors
- SpaceDto API response includes `slug`, `ruleCount`, `description`, `updatedAt`
- Create → Edit → Delete end-to-end flow completes without errors
- OrgSettings "Manage" button navigates correctly
- 0 TypeScript `any` types in modified files

---

**Status:** APPROVED — Handoff to Solution Architect
**Product Owner Sign-Off:** BMAD PO Agent, 2026-03-14

---

## Brownfield Product Inventory — Draft Supplement (2026-10-25)

> **Status:** Draft — auto-seeded by /product-owner:onboard. Refine via /product-owner:create-prd. Source: README/manifest as of 2026-10-25.
>
> Existing Spaces requirements, acceptance criteria, priorities, and approval above retain their original scope. This supplement is unapproved and does not supersede those decisions.

### Vision

An enterprise Business Rules Management System for authoring, managing, and executing business decisions. Source: README product description.

### User Personas

[populate — validate users, goals, decision makers, and concerns]

Rule authors, administrators, and integrating developers are candidate roles inferred from documented workflows. README sector examples are use cases, not confirmed personas or regulatory obligations.

### Features

Each row is a documented capability candidate; availability and acceptance are not verified by this inventory.

| ID | Feature | Description | Priority | Source |
|---|---|---|---|---|
| F-001 | Decision Tables | Author tabular conditions and business outcomes. | [populate priority] | [README: Key Features](../README.md#key-features) |
| F-002 | Decision Trees | Author branching decision logic visually. | [populate priority] | [README: Key Features](../README.md#key-features) |
| F-003 | Scorecards | Calculate weighted scores from input characteristics. | [populate priority] | [README: Key Features](../README.md#key-features) |
| F-004 | Ensembles | Combine outcomes from multiple rules. | [populate priority] | [README: Key Features](../README.md#key-features) |
| F-005 | DMN 1.4 | Express and chain standardized decision models. | [populate priority] | [README: Key Features](../README.md#key-features) |
| F-006 | Rule Flows | Orchestrate decisions and actions through rule flows. | [populate priority] | [README: Key Features](../README.md#key-features) |
| F-007 | Scripting Rules | Execute scripted business logic with resource limits. | [populate priority] | [README: Key Features](../README.md#key-features) |
| F-008 | Complex Event Processing | Detect event patterns across sequences, aggregation, and frequency. | [populate priority] | [README: Key Features](../README.md#key-features) |
| F-009 | RFL Expression Language | Express reusable conditions and functions in RuleForge language. | [populate priority] | [README: Key Features](../README.md#key-features) |
| F-010 | Multi-Layer Caching | Reuse compiled decisions to support repeated rule execution. | [populate priority] | [README: Key Features](../README.md#key-features) |
| F-011 | Import/Export | Transfer single or multiple rule definitions between environments. | [populate priority] | [README: Key Features](../README.md#key-features) |
| F-012 | AI Assistant | Assist rule creation, optimization, conflict detection, and natural-language queries. | [populate priority] | [README: Key Features](../README.md#key-features) |
| F-013 | Approval Workflow | Review and approve or reject rule versions before publication. | [populate priority] | [README: Key Features](../README.md#key-features) |
| F-014 | A/B Testing | Compare outcomes from rule versions using traffic allocation. | [populate priority] | [README: Key Features](../README.md#key-features) |
| F-015 | Real-Time Notifications | Notify users of rule lifecycle activity. | [populate priority] | [README: Key Features](../README.md#key-features) |
| F-016 | Analytics Dashboard | Inspect execution volumes, results, and latency trends. | [populate priority] | [README: Key Features](../README.md#key-features) |
| F-017 | Audit Trail | Review and filter recorded activity and resource history. | [populate priority] | [README: Key Features](../README.md#key-features) |
| F-018 | Webhook Management | Register webhook endpoints and inspect delivery history. | [populate priority] | [README: Key Features](../README.md#key-features) |
| F-019 | User Profile | Maintain profile details, credentials, and session behavior. | [populate priority] | [README: Key Features](../README.md#key-features) |
| F-020 | Error Handling | Show loading, error, and retry feedback during product use. | [populate priority] | [README: Key Features](../README.md#key-features) |
| F-021 | Security Hardening | Protect user sessions and display security-related feedback. | [populate priority] | [README: Key Features](../README.md#key-features) |
| F-022 | Performance Optimization | Support responsive navigation and editing as views grow. | [populate priority] | [README: Key Features](../README.md#key-features) |
| F-023 | Monaco Code Editor | Edit scripts with syntax highlighting and completion. | [populate priority] | [README: Key Features](../README.md#key-features) |
| F-024 | Version Diff Viewer | Compare rule versions side by side. | [populate priority] | [README: Key Features](../README.md#key-features) |
| F-025 | Keyboard Shortcuts | Save and test from rule editors using keyboard shortcuts. | [populate priority] | [README: Key Features](../README.md#key-features) |
| F-026 | Client SDKs | Integrate rule execution through Java, Node.js, and Python SDKs. | [populate priority] | [README: Key Features](../README.md#key-features) |

> **Action:** PO scores each feature with RICE (Reach × Impact × Confidence ÷ Effort). New reach, impact, confidence, effort, and priority values require evidence; existing Spaces scores above are preserved.

### Recent Source Deltas

No CHANGELOG was found. Recent commit subjects and corresponding rule API hooks provide these additional candidates:

| ID | Candidate delta | Evidence | Priority |
|---|---|---|---|
| D-001 | Archive and restore rules | feat(rules): archive and restore rules; solver refuses archived rules; frontend/src/api/rules.api.ts | [populate priority] |
| D-002 | Lock/unlock rules, move to folders, bulk deletion and batch export | Recent feat(rules) commit; frontend/src/api/rules.api.ts | [populate priority] |
| D-003 | Persist list filters and improve Rules page CRUD/navigation | Recent feat/fix(rules) commit subjects | [populate priority] |

These deltas are derived from source history, not approved product requirements or new release assignments.

### MVP Scope

[populate — distinguish currently shipping capabilities from next-release scope]

The original Spaces MVP above applies to its March 2026 scope. README roadmap completion labels do not define a new MVP.

### Out of Scope

[populate — future-release non-goals require Product Owner decisions]

The preceding “Out of Scope (This Sprint)” list remains specific to the Spaces sprint and is not generalized to the whole platform.

### Acceptance Criteria

[populate per feature — Business Analyst refines via /business-analyst:create-user-story]

For each prioritized feature: define observable outcome, permitted users, failure behavior, and acceptance evidence. Existing Spaces criteria remain preserved above.

### Success Metrics

[populate — see project-brief.md for project-level metrics]

### Sources and Related Artifacts

- [Current README](../README.md)
- [Project context](../.bmad/PROJECT-CONTEXT.md)
- [Confirmed technology inventory](../.bmad/tech-stack.md)
- [Solution architecture](architecture/solution-architecture.md)
- [Enterprise architecture](architecture/enterprise-architecture.md)
- [UX design](ux/DESIGN.md)
- [ADR index](architecture/adr/ADR-INDEX.md)
- [Project brief supplement](project-brief.md#brownfield-product-overview--draft-supplement-2026-10-25).

### Evidence and Interpretation

| Claim | Evidence class | Source / limitation |
|---|---|---|
| RuleForge is an enterprise BRMS combining an embeddable engine and a platform | derived_from_source | README product description |
| Financial services, insurance, healthcare, and e-commerce are example domains | derived_from_source | README use cases; these are not validated customer personas |
| Feature inventory describes documented capabilities | derived_from_source | README Key Features; no current runtime verification |
| Java 25 is required for the current engine | derived_from_source | README Java 25 notes; this constrains consumers embedding the engine |
| Product licensing is proprietary, all rights reserved | derived_from_source | README License; not a legal assessment of dependency or SDK licenses |
| Named business stakeholders, adoption baseline, budget, and go-live date | unknown | Not supplied by current product signals |
| Applicable regulatory obligations and data classification | unknown | Sector examples do not establish HIPAA, GDPR, or other obligations |

README's “Production-Ready” description and “Done” roadmap rows are source claims. Onboarding does not turn them into release approval.

### Open Questions and Source Gaps

- README lists eight rule types; the engine enum declares ten, while frontend RuleType declares nine labels. Reconcile naming, supported surfaces, and feature coverage before making a product-wide rule-type promise.
- README Known Issues reports event publication failure affecting audit, analytics, and invalidation. Their descriptions below remain capability claims with this limitation.
- README reports missing showcase seeds on fresh databases and an unavailable MinIO image. Operational readiness remains a verification question.
- No CHANGELOG, standalone ROADMAP/MILESTONES “Future” section, pricing document, or commercial-license file was found. Future scope and commercial model beyond proprietary licensing remain unknown.
- Product Owner must distinguish currently supported capabilities from the next release, name stakeholders, and validate success metrics before this draft becomes a business mandate.
