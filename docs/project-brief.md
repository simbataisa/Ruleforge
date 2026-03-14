# Project Brief — Spaces Page Features

**Date:** 2026-03-14
**Sponsor:** Engineering Lead, RuleForge Platform
**Analyst:** BMAD Business Analyst Agent (Phase 1)

---

## Executive Summary

Spaces are the primary organizational unit in RuleForge — sitting between an Organization and the folders/rules inside it. Despite this central role, the current Spaces implementation is incomplete: users can list and create spaces but cannot view space details, configure settings, manage approval workflows, edit or delete existing spaces, or navigate meaningfully within a space context. This gap means rule authors and space administrators must work around missing functionality daily, reducing platform adoption and increasing support burden. This brief defines the full scope of work needed to deliver a complete, production-grade Spaces experience.

---

## Problem Statement

The current Spaces page provides only a bare list view with a create modal — no detail page, no settings page, no CRUD beyond creation, and missing backend fields (`slug`, `ruleCount`, `description`). Rule authors cannot see how many rules are in a space, space admins cannot configure approval settings from the UI, and the OrgSettings "Manage" button navigates nowhere. **Business Impact:** Incomplete Spaces UX blocks the adoption of multi-space organizational workflows, prevents self-service administration, and exposes a broken product experience to enterprise customers evaluating RuleForge.

---

## Stakeholder Analysis

| Stakeholder | Role | Power | Interest | Needs | Concerns |
|---|---|---|---|---|---|
| Rule Author | Primary user | Low | High | See spaces, navigate to rules within space | Confusion navigating fragmented UI |
| Space Admin | Space operator | Medium | High | Create/edit/delete spaces, set approval policy | No way to configure space today |
| Org Owner | Organization admin | High | High | Full CRUD on spaces, enforce governance | Broken "Manage" link in OrgSettings |
| Frontend Engineer | Implementor | Medium | High | Clear API contracts, typed DTOs | Undefined/missing fields cause TS errors |
| Backend Engineer | Implementor | Medium | High | Clear scope for new endpoints | Risk of breaking existing API consumers |
| QA/QE | Validator | Medium | High | Testable acceptance criteria, smoke tests | No test coverage on Spaces endpoints |

---

## Current State Assessment

### How It Works Today

1. User navigates to `/spaces` → `SpaceListPage.tsx` renders a list of spaces via `GET /v1/organizations/current/spaces`
2. A "Create Space" button opens a modal → `POST /v1/organizations/current/spaces`
3. Each space card shows only the name — no description, no rule count
4. Clicking a space card does nothing (no navigation)
5. OrgSettingsPage shows a "Manage" button per space with no `onClick` handler
6. No way to edit or delete a space
7. No way to view what's inside a space (folders/rules)
8. `SpaceApprovalSettings` endpoints exist in management-service but no frontend UI

### Backend State

- `spaces` table: `id`, `org_id`, `name`, `description`, `limits` (JSONB), `created_at`, `updated_at`
- `space_approval_settings` table: exists with approvers, SLA, require-all settings
- `SpaceDto` returned by auth-service: only `id`, `name`, `orgId` — missing `slug`, `description`, `ruleCount`, `updatedAt`
- No `slug` column in database
- No `PUT` (update) or `DELETE` endpoints for spaces
- `GET /v1/organizations/current/spaces/:id` may not exist or is incomplete

### Pain Points

- **Broken navigation**: "Manage" button in OrgSettings has no destination; space cards are not clickable
- **Missing data**: Frontend cannot display `ruleCount` or `description` because backend doesn't return them
- **No CRUD**: After creation, spaces cannot be renamed, described, or deleted
- **No approval UI**: Approval settings exist in DB but are inaccessible to admins
- **Poor discoverability**: No slug-based URLs; no way to link directly to a space
- **TypeScript errors**: Frontend code expects fields (`slug`, `ruleCount`, `description`) that the API doesn't return

---

## Desired Future State

### Vision

Users navigating to the Spaces section can see all spaces with rich metadata (description, rule count), click into a space to view its content and stats, manage the space's name/description and approval policy from a dedicated settings page, and delete a space with a safety confirmation. URLs are human-readable (slug-based). All CRUD operations work end-to-end.

### Key Capabilities Needed

1. **Enriched Spaces List**: Cards show name, description, rule count; actions for edit, delete, navigate to detail
2. **Space Detail Page** (`/spaces/:id`): Breadcrumb, stats bar (rule count, created date), overview content
3. **Space Settings Page** (`/spaces/:id/settings`): General tab (name/description form), Approval Settings tab (approvers, SLA, requireAll), Danger Zone (delete with confirmation)
4. **Full CRUD Backend**: `PUT /v1/organizations/current/spaces/:id` (update), `DELETE /v1/organizations/current/spaces/:id` (delete)
5. **SpaceDto Enrichment**: Backend returns `slug`, `description`, `ruleCount`, `updatedAt`
6. **Slug Generation**: Auto-generated from name at write time, stored in DB, unique per org
7. **OrgSettings Fix**: "Manage" button wired to navigate to space settings

---

## Requirements Summary

### Functional Requirements

**Spaces List (Enhanced)**
- FR-01: System must display space cards with name, description (or placeholder if null), and ruleCount badge
- FR-02: Each space card must have a "Settings" action navigating to `/spaces/:id/settings`
- FR-03: Each space card must have an "Open" action navigating to `/spaces/:id`
- FR-04: Each space card must have a "Delete" action triggering a confirmation dialog
- FR-05: "Edit" action must allow updating space name and description inline via modal

**Space Detail Page**
- FR-06: `/spaces/:id` must render with breadcrumb (Spaces → {name})
- FR-07: Page must display stats: ruleCount, createdAt date, updatedAt date
- FR-08: Page must include a link/button to Space Settings
- FR-09: Page must handle loading, empty, and error states

**Space Settings Page**
- FR-10: `/spaces/:id/settings` must have a "General" tab with name/description form (save on submit)
- FR-11: Must have an "Approval Settings" tab showing current approvers, SLA, requireAll toggle
- FR-12: Approval Settings tab must allow saving changes via `PUT /v1/spaces/:id/approval-settings`
- FR-13: Must have a "Danger Zone" section with delete space action + `ConfirmDialog`
- FR-14: Delete must navigate back to `/spaces` on success

**Backend Endpoints**
- FR-15: `PUT /v1/organizations/current/spaces/:id` — update name and/or description (OWNER/ADMIN only)
- FR-16: `DELETE /v1/organizations/current/spaces/:id` — delete space (OWNER only)
- FR-17: `GET /v1/organizations/current/spaces/:id` — get single space with full enriched DTO
- FR-18: `SpaceDto` must include: `id`, `name`, `slug`, `description`, `ruleCount`, `updatedAt`, `createdAt`

**Slug**
- FR-19: Slug is auto-generated from name on create/update (`my-space-name` format)
- FR-20: Slug must be unique per organization (database constraint)
- FR-21: Slug must be stored in `spaces` table (migration required)

**OrgSettings Fix**
- FR-22: "Manage" button in OrgSettingsPage must navigate to `/spaces/:id/settings`

### Non-Functional Requirements

- **Performance**: Space list page must render in <200ms (API response + render)
- **Security / RBAC**: DELETE restricted to OWNER; PUT restricted to OWNER or ADMIN; GET accessible to all org members
- **Error Handling**: Slug collision (duplicate name) must return 409 Conflict with user-friendly message
- **Cascade Safety**: Deleting a space with rules should either be blocked (409) or display a warning listing affected rules
- **Accessibility**: All new pages must meet WCAG 2.1 AA (keyboard navigation, ARIA labels)
- **TypeScript**: All new API responses must be typed; no `any` in new code
- **Test Coverage**: All new backend endpoints covered by smoke tests (Section 3.16)

---

## Gap Analysis

| Current Capability | Gap | Impact | Priority |
|---|---|---|---|
| Spaces list renders | No description or ruleCount shown | Rule authors can't assess space size | P0 |
| Space create works | No edit or delete | Admins stuck with mistakes, no lifecycle | P0 |
| SpaceDto has id/name | Missing slug, ruleCount, description, updatedAt | TypeScript errors; broken UI expectations | P0 |
| spaces table has description | No slug column | Can't generate human-readable URLs | P0 |
| approval_settings endpoints exist | No frontend UI for approval settings | Admins can't configure approval policy | P1 |
| OrgSettings "Manage" button renders | No onClick handler | Button is dead — broken UX | P0 |
| No SpaceDetailPage exists | Missing `/spaces/:id` route and component | Core workspace navigation missing | P0 |
| No SpaceSettingsPage exists | Missing `/spaces/:id/settings` route and component | No way to manage space configuration | P0 |

---

## Feasibility Assessment

### Technical Feasibility: GREEN
- All required technologies (Spring Boot, React, PostgreSQL) are already in use
- Pattern for new controller endpoints established across all other BMAD features
- `ConfirmDialog` already exists at `frontend/src/shared/components/ConfirmDialog.tsx`
- Slug generation is a standard utility (regex + lowercase transform)
- ruleCount query is a simple COUNT join on the `rules` table in the same database

### Timeline Feasibility: GREEN
- Scope is well-bounded: 1 DB migration, ~5 backend file changes, ~5 frontend file changes
- No new infrastructure required
- Estimated effort: 2-3 days implementation (single sprint)

### Budget Feasibility: GREEN
- Zero external cost; all work is internal development

### Organizational Feasibility: GREEN
- Patterns exist in codebase for all required work
- Backend and frontend engineers have demonstrated capability in prior sprints

### Overall Risk Level: LOW

**Top Risks:**
1. **Slug uniqueness collision** (LOW): Two spaces with same name in one org → `UniqueConstraintViolation`. Mitigation: catch `DataIntegrityViolationException`, return 409 with message "Space name already exists in this organization."
2. **Cascade delete impact** (MEDIUM): Deleting a space with rules/folders could break navigation. Mitigation: check for child entities before delete; return 409 if space is non-empty, or prompt user to confirm cascade.
3. **ruleCount accuracy** (LOW): Real-time JOIN adds minor query cost. Mitigation: use simple COUNT query; no caching needed at this scale.

---

## Assumptions

- Auth-service owns the `spaces` table and is the correct service for space CRUD (confirmed from existing code)
- Management-service owns `space_approval_settings` (confirmed from existing code)
- ruleCount can be computed via a direct JPQL/SQL count on the `rules` table from auth-service (same database)
- The existing `ConfirmDialog` component is sufficient for delete confirmation UX
- All users in an org can view spaces; OWNER/ADMIN roles required only for mutations

## Unknowns (Research Needed)

- None blocking implementation — all major questions answered by codebase exploration

---

## Success Criteria

- **Smoke Test Section 3.16 passes**: `GET`, `POST`, `PUT`, `DELETE` /v1/organizations/current/spaces all return expected status codes
- **SpaceDto includes all fields**: API responses contain `slug`, `ruleCount`, `description`, `updatedAt`
- **UI renders without errors**: `/spaces`, `/spaces/:id`, `/spaces/:id/settings` all load successfully
- **Create → Edit → Delete flow**: End-to-end CRUD works with no errors
- **OrgSettings "Manage" navigates**: Button links to correct space settings page
- **0 TypeScript errors** in new/modified frontend files

---

## Next Steps

1. Approve this project brief
2. Product Owner creates PRD (`docs/prd.md`) with prioritized user stories and acceptance criteria
3. Solution Architect designs technical solution (ADRs, API contracts, data model)
4. Enterprise Architect covers RBAC, security, and compliance concerns
5. UX/UI Designer produces screen specs
6. Tech Lead refines into 5 implementation stories
7. Backend + Frontend Engineers implement
8. QA/QE validates with smoke tests
9. Release Readiness sign-off

**Status:** APPROVED — Ready for PRD Creation
**Sponsor Sign-Off:** Engineering Lead (BMAD Simulation, 2026-03-14)
