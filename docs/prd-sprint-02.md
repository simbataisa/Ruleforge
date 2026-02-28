# PRD Sprint 02 — Rules Page Overhaul & Approval Assignment System

**Date:** 2026-02-27
**Author:** Product Owner / Business Analyst
**Sprint:** 02
**Status:** READY FOR REVIEW

---

## 1. Executive Summary

Sprint 02 addresses two high-impact gaps in RuleForge: (1) the Rules Page lacks effective discovery, filtering, and batch workflow capabilities, forcing users into slow manual processes, and (2) the Approval system has no concept of assigned reviewers, making it impossible to track who is responsible for approving what. These improvements directly reduce rule management overhead, accelerate approval turnaround, and establish accountability in the governance workflow.

**Business Impact:**
- Rule authors spend excessive time locating rules in large repositories (100+ rules per org)
- Approval bottlenecks occur because no one knows they are responsible for reviewing a specific rule
- Compliance risk: no audit trail of approval assignment means no proof of segregation of duties

---

## 2. User Personas and Pain Points

### Personas

| Persona | Role | Primary Activities | Sprint 02 Impact |
|---------|------|--------------------|------------------|
| **Rule Author** | EDITOR | Creates/edits rules, requests approval, tests rules | Needs faster rule discovery, batch approval requests, clear approval status tracking |
| **Approver** | OWNER / APPROVER | Reviews and approves/rejects rule changes | Needs personal approval queue, clear assignment of responsibility |
| **Admin** | OWNER | Manages org settings, users, all rules | Needs full visibility into all approvals, ability to reassign, batch operations |
| **Viewer** | VIEWER | Read-only access to rules and analytics | Needs better search/filter to find relevant rules quickly |

### Pain Points

#### Rules Page
1. **Search is name-only** — Users cannot find rules by description, alias, or tags. In orgs with 100+ rules, this forces manual scrolling or memorizing exact rule names.
2. **No tag filtering** — Tags exist on rules but cannot be used to filter the list, making the tagging system largely decorative.
3. **No batch operations** — Requesting approval for multiple rules requires navigating to each rule individually. For bulk publishing workflows (e.g., quarterly rate updates), this is a significant time sink.
4. **No quick actions** — Common actions like "Request Approval" require opening the rule detail page, adding unnecessary clicks to the most frequent workflow.
5. **No visual differentiation** — All rules look identical in the table regardless of status, lock state, or pending approval. Users must read status text to understand rule state.
6. **No date/creator filtering** — Cannot answer "what rules were changed this week?" or "what rules did I create?" without manual inspection.

#### Approvals
1. **No assignment** — When a rule author clicks "Request Approval," the request goes into a global pool. No specific person is notified or made responsible.
2. **No personal queue** — Approvers see ALL pending approvals, not just ones assigned to them. In large orgs, this creates bystander effect — everyone assumes someone else will handle it.
3. **No request tracking** — No record of who requested approval, when, or who was asked to review. This breaks audit trail requirements for regulated industries.
4. **No predefined approvers** — Each approval request requires manual selection (if assignment existed). Spaces should have default approver lists to streamline the process.

---

## 3. Epic 1: Rules Page Overhaul

### User Stories

#### P0 — Must Have (Sprint 02 Commitment)

**S1: Multi-field search**
> As a rule author, I want to search across rule name, description, alias, and tags simultaneously, so that I can find rules without knowing the exact name.

Acceptance Criteria:
- [ ] Single search input searches across: `name`, `description`, `alias`, and associated `tag.name` fields
- [ ] Search is case-insensitive substring match (ILIKE) across all fields
- [ ] Results highlight which field matched (e.g., "matched on tag: pricing")
- [ ] Search debounces at 300ms to avoid excessive API calls
- [ ] Empty search returns all rules (current behavior preserved)
- [ ] Backend: new `q` query parameter on `GET /v1/rules` that searches across all four fields (OR logic)
- [ ] Performance: search returns results within 500ms for up to 1,000 rules

**S2: Tag filter**
> As a rule author, I want to filter rules by one or more tags, so that I can narrow the list to a specific domain (e.g., "pricing", "compliance").

Acceptance Criteria:
- [ ] Tag filter is a multi-select dropdown populated from `GET /v1/tags`
- [ ] Selecting multiple tags uses OR logic (show rules that have any of the selected tags)
- [ ] Tag filter composes with existing filters (status, type, folder) using AND logic
- [ ] Tag filter composes with search (AND logic)
- [ ] Filter state is reflected in the URL query params for shareability
- [ ] Clear filter button resets tag selection

**S3: Batch select and batch request approval**
> As a rule author, I want to select multiple DRAFT rules and request approval for all of them at once, so that I can efficiently submit bulk changes for review.

Acceptance Criteria:
- [ ] Checkbox column appears on rule list for multi-select
- [ ] "Select All" checkbox selects all visible rules on current page
- [ ] Batch action toolbar appears when 1+ rules are selected, showing count
- [ ] "Request Approval" batch action is available when all selected rules are DRAFT status
- [ ] If any selected rule is not DRAFT, the "Request Approval" action is disabled with tooltip explaining why
- [ ] Batch request approval opens a single dialog to select assignee(s) (ties into Epic 2)
- [ ] Backend: `POST /v1/rules/batch/request-approval` accepting `{ ruleIds: UUID[], assigneeIds?: UUID[], comment?: string }`
- [ ] Success: toast notification with count of rules submitted
- [ ] Partial failure: show which rules failed and why

**S4: Quick "Request Approval" action on DRAFT rules**
> As a rule author, I want a "Request Approval" button directly in the rule list row, so that I can submit a single rule for approval without opening the detail page.

Acceptance Criteria:
- [ ] "Request Approval" action appears in the actions column/menu for rules with status DRAFT
- [ ] Action is hidden for rules not in DRAFT status
- [ ] Clicking opens a compact dialog: select assignee(s) (optional), add comment (optional), confirm
- [ ] On success, rule status updates to PENDING_APPROVAL in the list without page reload
- [ ] On error, display error message in dialog (e.g., "Rule has validation errors")

#### P1 — Should Have

**S5: Date range filter**
> As an admin, I want to filter rules by created date or last updated date range, so that I can find recently changed rules for review.

Acceptance Criteria:
- [ ] Date range picker with "from" and "to" fields
- [ ] Toggle between "Created Date" and "Last Updated" date filtering
- [ ] Preset quick options: "Today", "Last 7 days", "Last 30 days", "Custom"
- [ ] Composes with all other filters (AND logic)

**S6: Creator/owner filter**
> As an admin, I want to filter rules by the user who created them, so that I can review a specific team member's work.

Acceptance Criteria:
- [ ] Dropdown populated from org members
- [ ] Supports type-ahead search for large orgs
- [ ] Composes with all other filters

**S7: Visual rule cards mode**
> As a rule author, I want to toggle between table view and card grid view, so that I can see more context about each rule at a glance.

Acceptance Criteria:
- [ ] Toggle button (table/grid icon) in toolbar
- [ ] Card view shows: name, type icon, status badge, tags, description preview (2 lines), last updated, locked icon if locked, pending approval badge
- [ ] View preference persisted in localStorage
- [ ] Card view supports the same select/batch actions as table view

**S8: Visual status indicators**
> As a rule author, I want to see visual badges for locked rules and rules pending approval, so that I can quickly identify rule states.

Acceptance Criteria:
- [ ] Locked rules show a lock icon badge (in both table and card views)
- [ ] PENDING_APPROVAL rules show an amber "Pending Approval" badge
- [ ] PUBLISHED rules show a green indicator
- [ ] ARCHIVED rules show a gray/muted indicator

**S9: Saved filter presets**
> As a rule author, I want to save my frequently used filter combinations, so that I can quickly apply them without reconfiguring each time.

Acceptance Criteria:
- [ ] "Save current filters" button stores filter state with a user-provided name
- [ ] Saved presets appear as quick-select buttons above the filter bar
- [ ] Presets stored in localStorage (per-user, per-org)
- [ ] Delete preset option available
- [ ] Maximum 10 presets per user

#### P2 — Nice to Have

**S10: Extended sort options**
> As a rule author, I want to sort the rule list by type, status, last updated, or name, so that I can organize the list for different workflows.

Acceptance Criteria:
- [ ] Clickable column headers for sort
- [ ] Sort direction toggle (asc/desc) per column
- [ ] Default sort: last updated descending

**S11: Keyboard navigation**
> As a power user, I want to navigate the rule list using keyboard shortcuts, so that I can work faster.

Acceptance Criteria:
- [ ] Arrow keys navigate between rows
- [ ] Enter opens rule detail
- [ ] Space toggles row selection
- [ ] `/` focuses the search input

---

## 4. Epic 2: Approval Assignment System

### Current Process Flow (As-Is)

```
Rule Author edits rule (DRAFT)
    -> Clicks "Request Approval"
        -> Rule status changes to PENDING_APPROVAL
            -> ALL owners/approvers see it in approval queue
                -> Any approver can approve/reject
                    -> No record of who was supposed to review
                    -> No notification to specific person
```

**Gaps identified:**
- No `approval_requests` table — status lives only on the rule itself
- No assignment mechanism — impossible to direct a request to a specific reviewer
- No personal scoping — approvers see a global queue, leading to bystander effect
- No request metadata — no audit of who requested, when, who was assigned
- No space-level defaults — each request requires manual reviewer selection

### To-Be Process Flow

```
Rule Author edits rule (DRAFT)
    -> Clicks "Request Approval"
        -> Dialog: select assignee(s) OR auto-populate from space defaults
            -> System creates approval_request record
            -> Rule status changes to PENDING_APPROVAL
            -> Assigned approvers are notified
                -> Approver sees request in "My Queue" tab
                    -> Approver approves/rejects
                        -> approval_request updated with resolution
                        -> Full audit trail preserved
```

### User Stories

#### P0 — Must Have (Sprint 02 Commitment)

**S12: Approval requests tracking table**
> As a platform admin, I want all approval requests tracked in a dedicated table with full metadata, so that we have an auditable record of every approval workflow.

Acceptance Criteria:
- [ ] New `approval_requests` table with columns: `id` (UUID PK), `rule_id` (FK), `rule_version` (int), `requested_by` (FK to users), `requested_at` (timestamp), `status` (PENDING/APPROVED/REJECTED/CANCELLED), `resolved_by` (FK nullable), `resolved_at` (timestamp nullable), `comment` (text nullable), `org_id` (FK for RLS)
- [ ] New `approval_request_assignees` join table: `request_id` (FK), `user_id` (FK), `status` (PENDING/APPROVED/REJECTED), `responded_at` (timestamp nullable), `comment` (text nullable)
- [ ] RLS policy: users can see approval_requests for their org only
- [ ] Flyway migration (V029) creates both tables with indexes
- [ ] Existing approval workflow updated to create records in this table

**S13: Request Approval flow with assignee selection**
> As a rule author, I want to select specific approver(s) when requesting approval, so that the right people are responsible for reviewing my rule change.

Acceptance Criteria:
- [ ] "Request Approval" dialog shows list of eligible approvers (users with OWNER or APPROVER role in the org)
- [ ] Author can select 1 or more assignees (multi-select)
- [ ] Assignee selection is optional — if none selected, request goes to all eligible approvers (backward compatible)
- [ ] Comment field allows author to add context for the reviewer
- [ ] Backend: `POST /v1/rules/{ruleId}/approval-requests` with body `{ assigneeIds?: UUID[], comment?: string }`
- [ ] Response includes the created approval_request with status and assignees
- [ ] Rule status transitions to PENDING_APPROVAL on successful request creation
- [ ] Validation: rule must be DRAFT, user must have EDITOR+ role, assignees must have OWNER/APPROVER role

**S14: Approvals page — My Queue vs All Requests**
> As an approver, I want to see a "My Queue" tab showing only approval requests assigned to me, so that I can focus on my responsibilities without noise from the global queue.

Acceptance Criteria:
- [ ] Approvals page has two tabs: "My Queue" and "All Requests"
- [ ] "My Queue" (default tab): shows only requests where current user is in `approval_request_assignees` and assignee status is PENDING
- [ ] "All Requests" tab: visible only to OWNER role, shows all pending requests across the org
- [ ] Both tabs show: rule name, rule type, requested by, requested date, SLA remaining, assignee(s)
- [ ] Clicking a request opens the existing approval detail panel
- [ ] Badge count on "My Queue" tab showing number of pending items

**S15: Assignee display in approval detail**
> As an approver, I want to see who else is assigned to review the same rule, so that I know if I am the sole reviewer or part of a group.

Acceptance Criteria:
- [ ] Approval detail panel shows "Assigned Reviewers" section
- [ ] Each assignee shows: name, avatar, individual status (Pending/Approved/Rejected), response timestamp
- [ ] Current user's entry is highlighted
- [ ] If any assignee has already responded, their decision is visible to other assignees
- [ ] "Requested by" field shows the rule author who initiated the request, with timestamp

#### P1 — Should Have

**S16: Predefined approvers per space**
> As an admin, I want to configure default approvers for each space, so that rule authors don't need to manually select reviewers every time.

Acceptance Criteria:
- [ ] New `space_approval_settings` table: `space_id` (FK), `approver_user_id` (FK), `org_id` (FK for RLS)
- [ ] Space settings page includes "Default Approvers" section
- [ ] Admin can add/remove users from the default approver list
- [ ] Only users with OWNER/APPROVER role can be added as default approvers

**S17: Auto-populate assignees from space settings**
> As a rule author, I want the "Request Approval" dialog to pre-fill assignees from my space's default approver list, so that I can submit quickly without manual selection.

Acceptance Criteria:
- [ ] "Request Approval" dialog auto-populates assignees from `space_approval_settings` for the rule's space
- [ ] Author can override: add or remove assignees before submitting
- [ ] If no space defaults configured, dialog shows empty assignee list with manual selection
- [ ] Visual indicator shows "Pre-filled from space settings" when defaults are applied

**S18: Notification to assigned approvers**
> As an approver, I want to be notified when a new approval request is assigned to me, so that I respond promptly.

Acceptance Criteria:
- [ ] In-app notification created for each assignee when request is submitted
- [ ] Notification includes: rule name, requested by, link to approval detail
- [ ] Notification bell shows unread count
- [ ] Notification marked as read when user views the approval request

**S19: Per-assignee approval status**
> As an admin, I want to see each assignee's individual approval/rejection status, so that I can track who has responded and who hasn't.

Acceptance Criteria:
- [ ] Approval detail shows per-assignee status: PENDING, APPROVED, REJECTED
- [ ] Approval resolution logic: request is approved when at least one assignee approves (OR logic, configurable later)
- [ ] If any assignee rejects, request is rejected (rejection takes precedence)
- [ ] Each assignee's response timestamp and comment are recorded

#### P2 — Nice to Have

**S20: Re-assign approval request**
> As an admin, I want to re-assign an approval request to different approvers, so that I can handle out-of-office or workload balancing situations.

Acceptance Criteria:
- [ ] OWNER can edit assignees on a pending approval request
- [ ] Removed assignees see the request removed from their queue
- [ ] New assignees are notified
- [ ] Audit trail records the reassignment

**S21: Approval deadline per request**
> As a rule author, I want approval requests to have a deadline based on space SLA settings, so that requests don't languish indefinitely.

Acceptance Criteria:
- [ ] `approval_requests.deadline` column (timestamp nullable)
- [ ] Deadline auto-calculated from `space_approval_settings.sla_hours` if configured
- [ ] Overdue requests highlighted in red in both "My Queue" and "All Requests"
- [ ] No auto-escalation in this story (see S22)

**S22: Escalation on deadline**
> As an admin, I want overdue approval requests to escalate to the space OWNER, so that stale requests are surfaced.

Acceptance Criteria:
- [ ] Scheduler job checks for overdue approval requests (runs hourly)
- [ ] Overdue requests: notification sent to all space OWNERs
- [ ] Escalation recorded in audit trail
- [ ] Configurable escalation rules per space (future)

---

## 5. Dependencies and Open Questions for Architect

### Dependencies
1. **S12 depends on DB migration** — New tables (`approval_requests`, `approval_request_assignees`) require Flyway V029 migration. Must be applied before backend work begins.
2. **S13 depends on S12** — Approval request API requires the tracking table.
3. **S3 depends on S13** — Batch approval request uses the same assignee selection flow.
4. **S14 depends on S12** — My Queue tab queries the new tables.
5. **S16 depends on S12** — Space settings table is a separate migration (V030) but logically follows the request table.
6. **S18 depends on notification infrastructure** — In-app notifications may require a new `notifications` table and WebSocket or polling mechanism. Architect to determine approach.

### Open Questions for Architect
1. **Full-text search vs ILIKE** — For S1 (multi-field search), should we use PostgreSQL `tsvector` full-text search for better performance at scale, or is ILIKE sufficient for the expected data volume (< 10K rules per org)?
2. **Approval resolution logic** — S19 uses "any approve = approved, any reject = rejected." Should this be configurable per space (e.g., require ALL assignees to approve)? If so, what's the schema impact?
3. **Notification delivery** — S18 requires in-app notifications. Do we introduce a `notifications` table now, or use Kafka events + a notification-service? What about WebSocket push vs polling?
4. **Batch API design** — S3 requires a batch endpoint. Should this be a single transaction (all-or-nothing) or individual operations with partial success reporting?
5. **Tag search performance** — S2 requires joining rules with tags. Is the current schema (separate `tags` and `rule_tags` tables) performant enough, or do we need a denormalized `tag_names` array column on the rules table?
6. **URL filter state** — S2 mentions URL query params for filter shareability. Should this use standard query params or a compressed/encoded filter string?

---

## 6. RICE-Scored Prioritized Backlog

**Scoring assumptions:**
- Reach: estimated users affected per month (out of ~200 active users across all orgs)
- Impact: 3 = massive, 2 = high, 1 = medium, 0.5 = low
- Confidence: percentage belief in estimates
- Effort: engineering days

| # | Story | Epic | Reach | Impact | Confidence | Effort (days) | RICE Score | Priority |
|---|-------|------|-------|--------|------------|---------------|------------|----------|
| S1 | Multi-field search | E1 | 200 | 3 | 90% | 3 | 180 | P0 |
| S12 | Approval requests table | E2 | 150 | 3 | 95% | 2 | 214 | P0 |
| S13 | Request Approval with assignees | E2 | 150 | 3 | 90% | 4 | 101 | P0 |
| S14 | My Queue vs All Requests | E2 | 120 | 3 | 90% | 3 | 108 | P0 |
| S2 | Tag filter | E1 | 180 | 2 | 90% | 2 | 162 | P0 |
| S4 | Quick Request Approval action | E1 | 150 | 2 | 95% | 2 | 143 | P0 |
| S3 | Batch select + batch approval | E1 | 100 | 2 | 80% | 4 | 40 | P0 |
| S15 | Assignee display in detail | E2 | 120 | 2 | 95% | 2 | 114 | P0 |
| S8 | Visual status indicators | E1 | 200 | 1 | 95% | 1 | 190 | P1 |
| S5 | Date range filter | E1 | 100 | 1 | 90% | 2 | 45 | P1 |
| S6 | Creator filter | E1 | 80 | 1 | 85% | 2 | 34 | P1 |
| S17 | Auto-populate from space settings | E2 | 100 | 2 | 80% | 3 | 53 | P1 |
| S16 | Predefined approvers per space | E2 | 100 | 2 | 85% | 3 | 57 | P1 |
| S7 | Card grid view | E1 | 150 | 1 | 70% | 4 | 26 | P1 |
| S9 | Saved filter presets | E1 | 80 | 1 | 75% | 2 | 30 | P1 |
| S19 | Per-assignee status | E2 | 80 | 2 | 80% | 2 | 64 | P1 |
| S18 | Notification to approvers | E2 | 120 | 2 | 70% | 5 | 34 | P1 |
| S10 | Extended sort options | E1 | 150 | 0.5 | 90% | 1 | 68 | P2 |
| S11 | Keyboard navigation | E1 | 30 | 1 | 70% | 3 | 7 | P2 |
| S20 | Re-assign approval | E2 | 40 | 1 | 80% | 2 | 16 | P2 |
| S21 | Approval deadline | E2 | 60 | 1 | 75% | 2 | 23 | P2 |
| S22 | Escalation on deadline | E2 | 40 | 1 | 60% | 3 | 8 | P2 |

### Sprint 02 Commitment (P0 stories)

| Story | Effort | Running Total |
|-------|--------|---------------|
| S12: Approval requests table | 2d | 2d |
| S1: Multi-field search | 3d | 5d |
| S13: Request Approval with assignees | 4d | 9d |
| S14: My Queue vs All Requests | 3d | 12d |
| S2: Tag filter | 2d | 14d |
| S4: Quick Request Approval action | 2d | 16d |
| S15: Assignee display in detail | 2d | 18d |
| S3: Batch select + batch approval | 4d | 22d |

**Total P0 effort: ~22 engineering days** (split across backend + frontend engineers)

---

**Sign-off:** Product Owner / Business Analyst — 2026-02-27
