# Project Brief — Space-Scoped Rule Management

**Date:** 2026-03-14
**Sponsor:** RuleForge Platform Team
**Analyst:** BMAD Business Analyst Agent

---

## Changelog

| Version | Date | Author | Change |
|---------|------|--------|--------|
| 1.0 | 2026-03-14 | BA Agent | Initial draft — Space-Scoped Rule Management epic |

---

## Executive Summary

RuleForge currently allows users to create and manage rules, but the multi-tenancy model includes an intermediate organizational level called "Spaces" that is not fully utilized in the rule management workflow. Users belonging to multiple spaces cannot easily filter rules by space context, create rules within a specific space, or move/copy rules between spaces. This gap forces users to work with unscoped rule lists and manually track which rules belong to which space — a significant productivity drain for enterprise users managing hundreds of rules across multiple business units. This epic delivers full space-scoped rule management, closing the gap between the data model (which already has `space_id`) and the user experience.

---

## Problem Statement

The RuleForge platform supports an organizational hierarchy: **Organization → Space → Folder → Rule**. However, the current frontend and API usage does not enforce this hierarchy at the UI level:

1. `RuleListPage` calls `POST /rules` (no space context) when creating rules, defaulting to whatever space the backend assigns
2. `GET /rules` returns rules across all spaces with no filtering capability in the UI
3. `SpaceDetailPage` shows placeholder statistics rather than the actual rules belonging to that space
4. There is no UI to move a rule from one space to another
5. There is no UI to copy a rule into a specific target space

**Business Impact:** Enterprise customers with multiple spaces (e.g., regional business units, product lines) cannot effectively manage rules by space. This creates rule naming conflicts, operational confusion, and inhibits space-level governance — reducing platform adoption and increasing support burden.

---

## Stakeholder Analysis

| Stakeholder | Role | Power | Interest | Needs | Concerns |
|-------------|------|-------|----------|-------|----------|
| Platform Engineer (Rule Author) | Primary User | Medium | High | Create rules in correct space, move/copy rules | Creating rules in wrong space by accident |
| Space Admin | Power User | High | High | See all rules in space, govern access | Rules from other spaces appearing in their view |
| Organization Admin | Executive | High | Medium | Audit rules by space, enforce governance | Compliance — rules in wrong space affecting wrong processes |
| Frontend Dev Team | Implementer | Medium | High | Clear API contract, reusable components | API behavior consistency |
| Backend Dev Team | Implementer | Medium | High | Clean endpoint design, DTO completeness | Breaking existing consumers |
| QA Team | Gatekeeper | Medium | High | Testable acceptance criteria | Edge cases around empty spaces, permissions |

---

## Current State Assessment

### How It Works Today

1. **Rule List** (`GET /rules`): Returns all rules across all spaces for the tenant. The frontend displays them in a flat list with folder-based organization.
2. **Rule Create** (`POST /rules`): Creates a rule with a `spaceId` in the request body, but the frontend's `CreateRuleModal` may not always pass the correct space context — it relies on user-selected space or defaults.
3. **Space Detail Page**: Displays basic space metadata (name, slug, description) with placeholder stat cards. No rule list is shown.
4. **Move/Copy**: There is a `POST /rules/{id}/copy` endpoint. There is no dedicated "move" endpoint — the only way to change a rule's space is via a full rule update if `UpdateRuleRequest` supports `spaceId`.
5. **Search/Filter**: Global rule list has search; no space-scoped search exists.

### Pain Points

- **No space context in rule creation**: Users creating rules from the RuleListPage don't see which space they're creating into, leading to misplaced rules.
- **No space-scoped rule view**: SpaceDetailPage is a dead end — users must navigate away to find that space's rules.
- **No move capability**: Once a rule is in the wrong space, there is no UI to correct it without deleting and recreating.
- **No space-filtered search**: Searching for rules returns results across all spaces, increasing noise.
- **Copy loses space context**: Copying a rule defaults to the source space; no UI to copy into a different target space.

### Current Capacity/Constraints

- Backend: `spaces` and `rules` tables exist with proper FK (`rules.space_id`). The `GET /spaces/{spaceId}/rules` and `POST /spaces/{spaceId}/rules` endpoints exist in the management service.
- Backend: `POST /rules/{id}/copy` endpoint exists.
- Backend: Move endpoint (`PATCH /rules/{id}/move`) may not exist — needs verification.
- Frontend: React Query + Zustand pattern established; reusable components (Modal, Button, Badge, Select) available.
- No mobile scope for this epic.

---

## Desired Future State

### Vision

Users can navigate to any Space and immediately see, create, search, and manage rules scoped to that space. They can move rules between spaces through an intuitive modal workflow and copy rules into any target space. The RuleListPage preserves backward compatibility while gaining space-context awareness via URL query parameters.

### Key Capabilities Needed

1. **Space-Scoped Rule List**: SpaceDetailPage shows a full, searchable, filterable, sortable list of rules belonging to that space. Includes rule type, status, version, and quick actions.

2. **Space-Scoped Rule Creation**: When a user creates a rule from within a SpaceDetailPage, the rule is automatically created in that space via `POST /spaces/{spaceId}/rules`. No manual space selection needed.

3. **Move Rule Between Spaces**: A "Move" action on any rule opens a modal to select a target space and executes `PATCH /rules/{id}/move`. The rule disappears from the current space view upon success.

4. **Copy Rule to Space**: A "Copy" action on any rule opens a modal to select a target space (and optionally rename), executes `POST /rules/{id}/copy` with target space context. The source rule remains unchanged.

5. **Space Context in RuleListPage**: When navigated to with `?spaceId=<id>`, RuleListPage shows a space context banner and filters the list to that space. Create button uses space-scoped API.

---

## Requirements Summary

### Functional Requirements

- FR-001: System must provide `GET /spaces/{spaceId}/rules` (list) — **already exists**
- FR-002: System must provide `POST /spaces/{spaceId}/rules` (create) — **already exists**
- FR-003: System must provide `POST /rules/{id}/copy` (copy to same or different space) — **already exists, may need target space param**
- FR-004: System must provide `PATCH /rules/{id}/move` (change rule's spaceId) — **may be missing, needs implementation**
- FR-005: SpaceDetailPage must display rules tab with full rule list scoped to the space
- FR-006: SpaceDetailPage must have a "Create Rule" button that creates in the current space
- FR-007: SpaceDetailPage must support search by rule name within the space
- FR-008: SpaceDetailPage must support filter by rule type (TABLE, TREE, SCORECARD, etc.)
- FR-009: SpaceDetailPage must support filter by status (DRAFT, PUBLISHED, ARCHIVED)
- FR-010: SpaceDetailPage must show each rule with: name, type badge, status badge, version, created date, actions
- FR-011: Move Rule modal must show available spaces (excluding current space)
- FR-012: Copy Rule modal must show available spaces and optional new name field
- FR-013: RuleListPage must accept `?spaceId=` query param to scope the list
- FR-014: RuleListPage must show space context banner when in space-scoped mode

### Non-Functional Requirements

- **Performance:** Space-scoped rule list must load in <500ms for up to 1000 rules per space (using existing pagination)
- **Security:** All endpoints must enforce tenant isolation via existing RLS; users can only move/copy rules within their organization
- **Usability:** Move/Copy modals must be discoverable (accessible from rule action menus), accessible (keyboard navigation), and confirm before executing destructive operations
- **Consistency:** UI patterns (modals, badges, buttons, loading states) must match existing component library conventions
- **Backward Compatibility:** Existing `GET /rules` and `POST /rules` must not be broken; RuleListPage default behavior unchanged

---

## Gap Analysis

| Current Capability | Gap | Impact | Priority |
|-------------------|-----|--------|----------|
| `GET /rules` (all spaces) | No space filter in UI | Users see all spaces' rules, noisy | P0 |
| `POST /rules` (unscoped) | No space context in create flow | Rules created in wrong space | P0 |
| SpaceDetailPage (placeholder) | No rule list visible | Space page is a dead end | P0 |
| `POST /rules/{id}/copy` | No target space selection | Can't copy to different space | P1 |
| No move endpoint | Can't change rule's space | Rule stuck in wrong space | P1 |
| Rule list (no space filter) | No space-scoped search | Search returns cross-space noise | P1 |

---

## Feasibility Assessment

### Technical Feasibility: GREEN
- All required DB schema (space_id FK) already exists
- Most backend endpoints already exist
- Frontend component library is mature — Modal, Select, Badge, Button all available
- React Query patterns established; adding new hooks is straightforward

### Timeline Feasibility: GREEN
- Estimated effort: 3-5 days (1 backend endpoint, 4-5 frontend files)
- No new infrastructure or schema migrations required

### Budget Feasibility: GREEN
- No external dependencies or licensing required
- Leverages existing platform capabilities

### Organizational Feasibility: GREEN
- Backend and frontend engineers familiar with codebase
- Patterns established from prior space management epics

### Overall Risk Level: LOW

**Top Risks:**
1. **Move endpoint doesn't exist and UpdateRuleRequest doesn't support spaceId change**: Mitigation — verify first, implement minimal `PATCH /rules/{id}/move` DTO if needed.
2. **Copy endpoint doesn't accept targetSpaceId**: Mitigation — check `CopyRuleRequest` DTO, extend if needed, or add query param.
3. **SpaceDetailPage refactor breaks existing UI**: Mitigation — keep existing sections, add rules as a new tab/section below existing content.

---

## Assumptions

- The `rules.space_id` column is already present and not nullable (or has a sensible default).
- The existing `useSpaces()` hook returns all spaces visible to the current user for use in Move/Copy modals.
- The management-service is the sole owner of rule CRUD operations; no other service needs to be modified.
- React Router v6 `useSearchParams` hook is available for reading `?spaceId=` from URLs.

---

## Unknowns (Research Needed)

- **PATCH /rules/{id}/move exists?**: Backend Engineer to verify RuleController and RuleService — verify in Phase 4a.
- **CopyRuleRequest includes targetSpaceId?**: Backend Engineer to verify DTO — verify in Phase 4a.
- **UpdateRuleRequest includes spaceId?**: If so, the move could be done via existing PUT — verify in Phase 4a.

---

## Success Criteria

- **SC-001**: SpaceDetailPage shows rule list for that space; verified by navigating to any space and seeing its rules (0 to N)
- **SC-002**: Creating a rule from SpaceDetailPage correctly sets `space_id` to the current space — verified via smoke test
- **SC-003**: Moving a rule successfully changes its `space_id` — verified by moving a rule and confirming it no longer appears in source space
- **SC-004**: Copying a rule to a different space creates a new rule in the target space — verified via smoke test
- **SC-005**: RuleListPage with `?spaceId=X` shows only rules from space X — verified via URL navigation
- **SC-006**: All operations complete within SLA (<500ms response for list, <1s for mutations)

---

## Next Steps

1. Handoff to Product Owner to create `docs/prd-spaces-rules.md` with RICE-prioritized stories and Gherkin ACs
2. Backend Engineer to verify existing endpoint coverage before architecture phase
3. Architecture phase to define API contracts and ADRs

**Status:** APPROVED — Ready for Product Owner
**Sponsor Sign-Off:** Platform Team Lead, 2026-03-14
