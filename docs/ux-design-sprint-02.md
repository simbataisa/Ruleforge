# UX Design Spec — Sprint 02: Rules Page Redesign & Approvals Enhancement

**Author:** UX/UI Designer Agent
**Date:** 2026-02-27
**Status:** Ready for Engineering Handoff
**Covers:** Rules List Page, Approvals Page, Request Approval Flow

---

## Table of Contents

1. [Design Goals](#design-goals)
2. [Part 1: Rules Page Redesign](#part-1-rules-page-redesign)
   - [Layout Architecture](#layout-architecture)
   - [Smart Search Bar](#smart-search-bar)
   - [Filter Panel](#filter-panel)
   - [Table View](#table-view)
   - [Card Grid View](#card-grid-view)
   - [Batch Actions](#batch-actions)
3. [Part 2: Approvals Page Redesign](#part-2-approvals-page-redesign)
   - [Page Header](#page-header)
   - [My Queue Tab](#my-queue-tab)
   - [All Requests Tab](#all-requests-tab)
   - [Request Approval Flow Modal](#request-approval-flow-modal)
   - [Approval Detail Assignee Section](#approval-detail-assignee-section)
4. [Shared Patterns](#shared-patterns)
5. [Responsive Behavior](#responsive-behavior)
6. [Accessibility Notes](#accessibility-notes)
7. [Component Inventory](#component-inventory)
8. [Keyboard Shortcuts](#keyboard-shortcuts)

---

## Design Goals

| Goal | Rationale |
|------|-----------|
| Professional tool aesthetic | Move away from basic spreadsheet look; users spend hours in this UI daily |
| Rapid filtering and discovery | Rules lists grow to hundreds; finding the right rule must be fast |
| Batch operations | Admins manage rules in bulk; individual actions are too slow |
| Clear approval ownership | Current approvals have no visibility into who is assigned to review |
| Progressive disclosure | Show essential info first; reveal details on demand |

---

## Part 1: Rules Page Redesign

### Layout Architecture

Three-zone layout with collapsible sidebar:

```
+---------------------------------------------------------------+
|  TOP BAR: Smart search bar + filter chips + view toggle       |
+-------------------+-------------------------------------------+
|   LEFT SIDEBAR    |          MAIN CONTENT AREA                |
|   (280px, min 0)  |  (flex-1)                                 |
|   -------------   |  ---------------------------------------- |
|   Folder tree     |  Table view  OR  Card grid view           |
|   (collapsible)   |  (togglable via icon buttons)             |
|   -------------   |                                           |
|   Filter panel    |  Pagination bar at bottom                 |
|   (expandable     |                                           |
|    accordion)     |                                           |
+-------------------+-------------------------------------------+
```

**Layout Notes:**
- Sidebar width: 280px default, collapsible to 0 via chevron toggle button
- Sidebar collapse stores preference in `localStorage` key `ruleforge:sidebar-collapsed`
- Main content area uses `flex-1` to fill remaining space
- View toggle (table/card) stored in `localStorage` key `ruleforge:rules-view-mode`
- Sidebar divider between folder tree and filter panel is draggable (optional enhancement)

**Sidebar Collapse Behavior:**
- Toggle button: left-pointing chevron (`ChevronLeft` from Lucide) at top-right of sidebar
- When collapsed: sidebar width is 0, toggle button becomes right-pointing chevron anchored at left edge of main content
- Transition: 200ms ease-out on width property

---

### Smart Search Bar

**Position:** Full width across the top of the main content area, above filter chips.

**Visual Spec:**
- Height: 40px
- Border: 1px solid `neutral-200`, border-radius 8px
- Background: white
- Left icon: `Search` (Lucide), 16px, `neutral-400`
- Placeholder text: "Search rules by name, description, or tags..." in `neutral-400`, font-size 14px
- Clear button: `X` icon, appears only when input has value, 16px, `neutral-400`, hover `neutral-600`
- Focus state: ring-2 ring-primary-500/20, border-primary-500

**Search Suggestions Dropdown:**

Appears after 2+ characters typed, debounced 300ms. Dropdown is an absolutely positioned panel below the search input, full width of search bar, max-height 360px with overflow scroll, elevation level 2 shadow.

Sections within the dropdown:

| Section | Content | Icon | Max items |
|---------|---------|------|-----------|
| Rules | Matching rule names with type badge | Rule type icon (colored) | 5 |
| Tags | Matching tag names with rule count in parentheses | `Tag` icon (Lucide) | 5 |
| Types | If query matches a type name, show "Filter by type: TABLE" | `Filter` icon (Lucide) | 1 per type match |

- Each suggestion row: 36px height, padding 8px 12px, hover background `neutral-50`
- Keyboard navigation: up/down arrows, Enter to select, Escape to close
- Selecting a Rule navigates to that rule's editor
- Selecting a Tag adds a tag filter chip
- Selecting a Type adds a type filter chip

**Filter Chips Bar:**

Rendered directly below the search input when any filters are active. Horizontal scrollable row.

- Each chip: inline-flex, height 28px, padding 4px 8px 4px 10px, border-radius 6px, font-size 12px, font-weight 500
- Chip content: `{FilterType}: {Value}` + dismiss `X` button (14px icon)
- Chip colors: background `primary-50`, text `primary-700`, dismiss icon `primary-400` hover `primary-600`
- "Clear all filters" link: appears at the right end of the chip row, font-size 12px, text `primary-600`, underline on hover
- "Save this filter" button: star icon (`Star` Lucide), 14px, at right end before "Clear all", tooltip "Save current filter as preset"
- Saved filter presets stored in `localStorage` key `ruleforge:saved-filters`, rendered as a dropdown from the star icon

**Example chips:**
- `Type: TABLE` (x)
- `Status: DRAFT` (x)
- `Tag: pricing` (x)
- `Updated: Last 7 days` (x)

---

### Filter Panel

Located in the left sidebar, below the folder tree. Each section is a collapsible accordion (`Collapsible` from Shadcn/ui).

**Section 1: Status**
- Checkbox group, vertical stack
- Options: DRAFT, PUBLISHED, ARCHIVED, PENDING_APPROVAL
- Each checkbox row: 32px height, checkbox 16px + 8px gap + label 14px
- Status dot (colored, 8px circle) before each label:
  - DRAFT: `neutral-400` (gray)
  - PUBLISHED: `success-500` (green)
  - ARCHIVED: `neutral-300` (light gray)
  - PENDING_APPROVAL: `warning-500` (amber)
- Count badge after label showing number of rules matching, font-size 11px, `neutral-500`

**Section 2: Type**
- Checkbox group with colored type icons
- Options: TABLE, TREE, SCRIPT, FLOW, SCORECARD, DMN, CEP, ENSEMBLE
- Each type has a unique colored icon (same icons used throughout the app):

| Type | Icon (Lucide) | Color |
|------|---------------|-------|
| TABLE | `Table2` | `blue-500` |
| TREE | `GitBranch` | `green-500` |
| SCRIPT | `Code` | `purple-500` |
| FLOW | `Workflow` | `orange-500` |
| SCORECARD | `Calculator` | `teal-500` |
| DMN | `FileSpreadsheet` | `indigo-500` |
| CEP | `Activity` | `red-500` |
| ENSEMBLE | `Layers` | `amber-500` |

- Count badge after each label

**Section 3: Tags**
- Tag cloud layout: flex-wrap container
- Each tag: pill shape, height 24px, padding 2px 8px, font-size 12px, border 1px `neutral-200`, background white, border-radius 12px
- Tag text + count badge (e.g., "pricing (12)")
- Selected tags: background `primary-50`, border `primary-300`, text `primary-700`
- Show top 10 tags sorted by rule count descending
- "Show all tags" link at bottom if more than 10 exist, opens a scrollable list

**Section 4: Date Range**
- Label: "Last updated"
- Preset buttons (horizontal wrap): Today, Last 7 days, Last 30 days, Custom
- Preset buttons: 28px height, font-size 12px, pill shape, `neutral-100` background, `neutral-700` text
- Selected preset: `primary-50` background, `primary-700` text
- "Custom" opens a date range picker (Shadcn/ui `DatePickerWithRange`)

**Section 5: Created by**
- Label: "Created by"
- Searchable dropdown (`Combobox` from Shadcn/ui)
- Each option: avatar (24px circle, initials fallback) + full name
- Supports multi-select; selected users shown as small avatar stack above dropdown

---

### Table View

Default view. Enhanced data table with sorting, selection, and inline actions.

**Table Header Row:**
- Background: `neutral-50`
- Height: 40px
- Font-size: 12px, font-weight 600, text-transform uppercase, letter-spacing 0.05em, color `neutral-500`
- Sortable columns have sort icon (`ChevronsUpDown` Lucide, 14px) that changes to `ChevronUp`/`ChevronDown` when active

**Column Definitions:**

| # | Column Header | Width | Sortable | Content |
|---|---------------|-------|----------|---------|
| 1 | (checkbox) | 40px | No | `Checkbox` component. Header checkbox toggles select all on current page. |
| 2 | Name | flex (min 240px) | Yes (alphabetical) | **Primary:** Rule name, font-weight 600, font-size 14px, color `neutral-900`, truncate with ellipsis. **Secondary (below name):** Description excerpt, font-size 12px, color `neutral-500`, max 1 line, truncate. **Tertiary (below description):** Tag chips, max 3 shown + "+N more" badge. Tag chips: height 20px, font-size 11px, `neutral-100` background, `neutral-600` text, border-radius 4px. |
| 3 | Type | 120px | Yes | Colored type icon (16px) + type label badge. Badge: font-size 11px, font-weight 500, colored background (type-specific, 10% opacity), colored text. |
| 4 | Status | 160px | Yes | Status badge: height 24px, font-size 12px, font-weight 500, border-radius 4px, colored per status. Lock icon (`Lock` Lucide, 14px, `neutral-400`) appended if rule is locked. Bell icon (`Bell` Lucide, 14px, `warning-500`) appended if PENDING_APPROVAL. |
| 5 | Version | 80px | Yes (numeric) | "v{n}" text, font-size 13px, `neutral-700`. If rule has a pending draft version, show small orange dot indicator (6px) next to version number, tooltip: "Has pending v{n+1}". |
| 6 | Updated | 120px | Yes (chronological, default desc) | Relative time string (e.g., "2h ago", "3 days ago"), font-size 13px, `neutral-500`. Full datetime shown in tooltip on hover (format: "Feb 27, 2026 at 2:30 PM"). |
| 7 | Actions | 48px | No | Vertical dots icon button (`MoreVertical` Lucide, 16px). Opens dropdown menu on click. |

**Actions Dropdown Menu:**

| Action | Icon (Lucide) | Condition | Keyboard Shortcut |
|--------|---------------|-----------|-------------------|
| Edit | `Pencil` | Always | E |
| Test | `Play` | PUBLISHED or DRAFT | T |
| Request Approval | `SendHorizontal` | DRAFT only | — |
| Duplicate | `Copy` | Always | — |
| Export | `Download` | Always | — |
| Archive | `Archive` | PUBLISHED or DRAFT | — |
| Delete | `Trash2` (danger color) | DRAFT or ARCHIVED | — |

- Menu divider between "Export" and "Archive"
- "Delete" row text in `danger-500`
- Delete requires confirmation dialog

**Row Behavior:**
- Default height: auto (content-dependent, min 56px)
- Hover: background `neutral-50`, transition 150ms
- Selected (checkbox checked): background `primary-50`, left border 2px `primary-500`
- Click on row (not on checkbox/actions): navigates to rule editor
- Row hover reveals action buttons inline? No — use only the dots menu to avoid clutter

**Pagination Bar:**
- Bottom of table, height 48px, border-top 1px `neutral-200`
- Left side: "Showing 1-25 of 142 rules" text, font-size 13px, `neutral-500`
- Center: page size selector dropdown (10, 25, 50, 100), default 25
- Right side: pagination controls (First, Prev, page numbers, Next, Last)
- Page number buttons: 32px square, current page `primary-50` background with `primary-600` text

**Empty State:**
- Centered in content area
- Illustration: abstract geometric illustration (use a simple SVG placeholder, 200x160px)
- Heading: "No rules found", font-size 18px, font-weight 600, `neutral-700`
- If filters active: subtext "Try adjusting your filters" + "Clear filters" button (secondary)
- If no rules at all: subtext "Create your first rule to get started" + "Create Rule" button (primary)

**Loading State:**
- Skeleton rows: 8 skeleton rows matching table column widths
- Each skeleton row: height 56px, animated pulse
- Skeleton cells: rounded rectangles, `neutral-100` background

---

### Card Grid View

Toggled via icon buttons in the top bar area (next to the search bar, right-aligned). Two buttons: `LayoutList` (table) and `LayoutGrid` (card), Lucide icons, 16px. Active view button has `primary-500` color, inactive is `neutral-400`.

**Card Spec:**

```
+-----------------------------------+
|  [checkbox]          [TYPE BADGE] |  <- top row: checkbox top-left (on hover), type badge top-right
|                                   |
|  **Rule Name Here**               |  <- font-size 16px, font-weight 600, max 2 lines, truncate
|  Description text that can        |  <- font-size 13px, neutral-500, max 2 lines, truncate
|  span up to two lines...          |
|  ---------------------------------|
|  v3  [PUBLISHED]  [lock icon]     |  <- version + status badge + optional lock
|  ---------------------------------|
|  #fraud  #payments  #risk         |  <- tags row, max 3 + "+N"
|  ---------------------------------|
|  Updated 2h ago       [Edit] [..] |  <- footer: relative time left, actions right
+-----------------------------------+
```

**Card Dimensions:**
- Width: determined by grid (responsive columns)
- Min-height: 200px
- Padding: 16px
- Border: 1px `neutral-200`, border-radius 12px
- Background: white

**Card States:**
- Default: elevation level 0 (flat)
- Hover: elevation level 1, border-color `neutral-300`, transition 200ms ease
- Selected: border-color `primary-400`, background `primary-50`, checkbox visible and checked
- Focus (keyboard): ring-2 ring-primary-500/20

**Checkbox Behavior:**
- Hidden by default on each card
- Appears in top-left corner on hover (absolute positioned, 8px from top and left)
- Always visible when card is selected
- Click on checkbox selects card without navigating
- Click on card body navigates to rule editor
- Shift+click selects range (same as table)

**Grid Responsive Columns:**
- >= 1440px: 3 columns, gap 16px
- 1024-1439px: 2 columns, gap 16px
- 768-1023px: 2 columns, gap 12px
- < 768px: 1 column, gap 12px

**Card Footer Actions:**
- "Edit" button: ghost variant, font-size 13px, `primary-600` text
- Dots menu (`MoreVertical`): same dropdown as table view actions

---

### Batch Actions

Activated when one or more rules are selected (via checkbox in table or card view).

**Floating Action Bar:**
- Position: fixed, bottom 24px, left 50%, transform translateX(-50%)
- Width: auto (content-dependent), max-width 600px
- Height: 48px
- Background: `neutral-900`, border-radius 12px, elevation level 3
- Padding: 8px 16px
- Z-index: 50
- Entrance animation: slide up from bottom + fade in, 200ms ease-out
- Exit animation: slide down + fade out, 150ms ease-in

**Bar Content (left to right):**
1. Checkbox icon (checked, `white`) + "{N} rules selected" text, font-size 14px, font-weight 500, `white`
2. Divider: 1px vertical line, `neutral-700`, height 24px, margin 0 12px
3. Action buttons (ghost variant on dark background):
   - "Request Approval" — `SendHorizontal` icon + text, `white`, only if all selected are DRAFT
   - "Archive" — `Archive` icon + text, `white`
   - "Export" — `Download` icon + text, `white`
4. Clear selection button: `X` icon, 16px, `neutral-400`, hover `white`

**Batch Request Approval Modal:**

Triggered when "Request Approval" clicked from batch action bar.

```
+------------------------------------------------------+
|  Request Approval for {N} Rules                  [X] |
|  --------------------------------------------------- |
|                                                       |
|  Rules:                                               |
|  +--------------------------------------------------+|
|  | [TABLE] Fraud Detection Rule         v3          ||
|  | [TREE]  Customer Segmentation        v2          ||
|  | [SCRIPT] Price Calculator            v1          ||
|  +--------------------------------------------------+|
|                                                       |
|  Note: Only DRAFT rules are shown. {M} rules were    |
|  skipped (already published or pending approval).     |
|                                                       |
|  Assign to approvers:                                 |
|  +--------------------------------------------------+|
|  | [Search icon] Search members...                  ||
|  | [check] Sarah Kim (APPROVER) -------- suggested  ||
|  | [ ] Mike Chen (OWNER)                            ||
|  | [ ] Anna Lee (APPROVER)                          ||
|  +--------------------------------------------------+|
|  Selected: [Sarah Kim x]                              |
|                                                       |
|  Note to approvers (optional):         0/500          |
|  +--------------------------------------------------+|
|  |                                                  ||
|  +--------------------------------------------------+|
|                                                       |
|  SLA: 48 hours (from space settings)                  |
|                                                       |
|  [Cancel]                       [Request Approval]    |
+------------------------------------------------------+
```

- Modal width: 560px, centered, elevation level 3, overlay background `neutral-900/50`
- Rules list: scrollable if more than 5, max-height 200px
- Approver list: checkboxes, sorted with APPROVER role first, then OWNER
- "Suggested" badge: `primary-50` background, `primary-600` text, font-size 11px, next to space default approvers
- Note textarea: 3 rows, max 500 characters, character count in bottom-right
- SLA info line: `neutral-500` text, font-size 13px, info icon
- "Request Approval" button: primary variant, disabled until at least 1 approver selected
- On submit: toast "Approval requested for {N} rules"

---

## Part 2: Approvals Page Redesign

### Page Header

```
+---------------------------------------------------------------+
|  Approvals                                                     |
|  [My Queue]  [My Requests]  [All Requests]                     |
|  ----------------------------------------------------------   |
|  5 pending  *  2 overdue  *  1 approved today                  |
+---------------------------------------------------------------+
```

**Spec:**
- Page title: "Approvals", font-size 24px, font-weight 700, `neutral-900`
- Tabs: Shadcn/ui `Tabs` component, underline variant. Tab visibility is **role-dependent**:

| Role | Visible Tabs | Default Tab |
|------|-------------|-------------|
| APPROVER | My Queue | My Queue |
| EDITOR | My Requests | My Requests |
| OWNER / ADMIN | My Queue, My Requests, All Requests | All Requests |

  - "My Queue" — approval requests assigned to the current user (for approvers)
  - "My Requests" — approval requests submitted by the current user (for editors/requesters)
  - "All Requests" — all approval requests across the organization (OWNER/ADMIN only)
- Summary bar: below tabs, height 32px, flex row, gap 16px
  - Each metric: number (font-weight 600) + label (font-weight 400), font-size 14px
  - "pending": `neutral-700` text
  - "overdue": `danger-500` text (number only), rest `neutral-700`
  - "approved today": `success-600` text (number only), rest `neutral-700`
  - Separator: middle dot, `neutral-300`

---

### My Queue Tab

Same two-panel layout as current implementation, filtered to show only requests assigned to the current user.

**Left Panel — Queue List:**

Each queue item enhanced:

```
+-----------------------------------------------+
| [checkbox]                                     |
| [TYPE_ICON] Rule Name                          |
|   Requested by: John Smith  *  2h ago          |
|   Assignees: [avatar] [avatar] [+1]            |
|   [======----] 18h remaining                   |
+-----------------------------------------------+
```

**Queue Item Spec:**
- Height: auto, min 80px, padding 12px 16px
- Checkbox: top-left, for batch approve/reject
- Type icon: colored, 16px, next to rule name
- Rule name: font-size 14px, font-weight 600, `neutral-900`
- "Requested by" line: font-size 12px, `neutral-500`. Requester name is font-weight 500.
- "Assignees" line: avatar stack (20px circles, overlapping by 6px). Current user's avatar has a ring `primary-400`. "+N" badge if more than 3 assignees.
- SLA bar: width 100%, height 4px, border-radius 2px
  - Green (`success-500`): > 50% time remaining
  - Amber (`warning-500`): 25-50% time remaining
  - Red (`danger-500`): < 25% time remaining or overdue
  - Time remaining text: font-size 11px, same color as bar
  - Overdue: "Overdue by 3h" in `danger-500`, pulsing dot

**Queue Item States:**
- Default: background white, border-bottom 1px `neutral-100`
- Hover: background `neutral-50`
- Selected/Active: background `primary-50`, left border 3px `primary-500`
- Overdue: left border 3px `danger-400`

**Right Panel — Detail View:**

Same as current implementation with the addition of the Assignee Section (see below).

**Batch Actions (Queue):**
When checkboxes are selected in the queue:
- Floating bar appears at bottom (same pattern as Rules page):
  - "{N} selected" + "Approve All" (success) + "Reject All" (danger) + clear (X)
- "Approve All" requires confirmation dialog
- "Reject All" opens a rejection reason modal (required textarea, 500 chars)

---

### My Requests Tab

Visible to EDITOR role (and OWNER/ADMIN). Shows approval requests that the current user submitted. Provides visibility into where their rules are in the review process.

**Layout:** Same table format as All Requests tab (below), but filtered to `requestedBy = currentUser`.

**Table Columns:**

| # | Column Header | Width | Sortable | Content |
|---|---------------|-------|----------|---------|
| 1 | Rule | flex (min 200px) | Yes | Rule name (font-weight 600) + type badge |
| 2 | Version | 80px | Yes | "v{n}" |
| 3 | Submitted at | 120px | Yes (default desc) | Relative time, tooltip with full datetime |
| 4 | Assignees | 140px | No | Avatar stack (24px). Tooltip shows names + per-assignee status. |
| 5 | SLA | 140px | Yes | Progress bar + time remaining text |
| 6 | Status | 120px | Yes | PENDING / APPROVED / REJECTED badge |
| 7 | Actions | 48px | No | "View" link to detail panel |

**Key differences from All Requests:**
- No "Requested by" column (always the current user)
- No "Re-assign" action (only OWNER can re-assign)
- "Submitted at" instead of "Requested at" (user-facing language)

**Empty state:** "No pending requests" / "When you submit rules for approval, they will appear here"

---

### All Requests Tab

Visible only to OWNER and ADMIN roles. Switches from queue layout to a full sortable table.

**Table Columns:**

| # | Column Header | Width | Sortable | Content |
|---|---------------|-------|----------|---------|
| 1 | Rule | flex (min 200px) | Yes | Rule name (font-weight 600) + type badge (inline, right of name) |
| 2 | Version | 80px | Yes | "v{n}", font-size 13px |
| 3 | Requested by | 160px | Yes | Avatar (24px) + full name, font-size 13px |
| 4 | Requested at | 120px | Yes (default desc) | Relative time, tooltip with full datetime |
| 5 | Assignees | 140px | No | Avatar stack (24px circles, max 3 shown + "+N"). Tooltip on hover shows all names with their status. |
| 6 | SLA | 140px | Yes (by time remaining) | Progress bar (4px height, 80px width) + time remaining text. Color coding same as queue items. |
| 7 | Status | 120px | Yes | Badge: PENDING (`warning`), APPROVED (`success`), REJECTED (`danger`) |
| 8 | Actions | 80px | No | "View" link + "Re-assign" button (icon, OWNER only) |

**Filters (above table):**
Horizontal filter bar with dropdowns:
- Status: PENDING / APPROVED / REJECTED (multi-select)
- Assignee: searchable user dropdown
- Requested by: searchable user dropdown
- Date range: preset buttons (Today, 7 days, 30 days, Custom)
- Rule type: type dropdown with colored icons

Each active filter shown as a dismissible chip below the filter bar (same pattern as Rules page).

**Re-assign Action (OWNER only):**
- Opens a small popover with approver search/select
- Current assignees shown with remove (x) buttons
- "Add approver" search input
- "Save" button to confirm changes
- On save: toast "Assignees updated"

---

### Request Approval Flow Modal

Triggered from: rule editor "Request Approval" button, rules list action menu, or batch action bar.

```
+------------------------------------------------------+
|  Request Approval                                [X] |
|  --------------------------------------------------- |
|                                                       |
|  Rule: Fraud Detection Rule                           |
|  Version: v3                                          |
|  --------------------------------------------------- |
|                                                       |
|  Assign to approvers:                                 |
|  +--------------------------------------------------+|
|  | [Search icon] Search members...                  ||
|  +--------------------------------------------------+|
|  |                                                  ||
|  | [check] Sarah Kim (APPROVER) ------- suggested   ||
|  | [ ] Mike Chen (OWNER)                            ||
|  | [ ] Anna Lee (APPROVER)                          ||
|  |                                                  ||
|  +--------------------------------------------------+|
|                                                       |
|  Selected: [Sarah Kim x] [Mike Chen x]                |
|                                                       |
|  Note to approvers (optional):             0/500      |
|  +--------------------------------------------------+|
|  |                                                  ||
|  |                                                  ||
|  +--------------------------------------------------+|
|                                                       |
|  SLA: 48 hours (from space settings)                  |
|                                                       |
|              [Cancel]          [Request Approval]      |
+------------------------------------------------------+
```

**Modal Spec:**
- Width: 520px
- Centered vertically and horizontally with overlay `neutral-900/50`
- Border-radius: 16px
- Padding: 24px
- Elevation: level 3

**Rule Info Section:**
- Rule name: font-size 16px, font-weight 600, `neutral-900`
- Version: font-size 14px, `neutral-500`, below rule name
- Divider: 1px `neutral-200`, margin 16px 0

**Approver Selection:**
- Search input: height 36px, `Search` icon left, placeholder "Search members..."
- List: max-height 180px, overflow-y scroll
- Each row: height 40px, padding 8px 12px
  - Checkbox (16px) + avatar (24px) + name (14px, `neutral-900`) + role badge (11px, `neutral-500`)
  - "suggested" badge: right-aligned, `primary-50` bg, `primary-600` text, font-size 11px, border-radius 4px
  - "suggested" appears on members who are configured as default approvers in space settings
- Pre-populated: space's default approvers are pre-checked
- Selected display: below the list, flex-wrap row of pills
  - Each pill: avatar (16px) + name (12px) + dismiss X, height 28px, `neutral-100` bg, border-radius 14px

**Note Textarea:**
- Height: 72px (3 rows)
- Max characters: 500
- Character count: bottom-right, font-size 11px, `neutral-400` (turns `danger-500` at 480+)
- Placeholder: "Add context for the reviewers..."

**SLA Info:**
- `Info` icon (14px, `neutral-400`) + "48 hours (from space settings)" text, font-size 13px, `neutral-500`
- Non-editable, informational only

**Action Buttons:**
- "Cancel": ghost variant, left side
- "Request Approval": primary variant, right side
- Disabled state: when no approvers selected, button is `neutral-300` bg, `neutral-500` text, cursor not-allowed
- Loading state: spinner replacing button text, "Requesting..."
- On success: modal closes, toast "Approval requested for {rule name}"

---

### Approval Detail Assignee Section

New section added to the approval detail panel, positioned between the metadata section and the diff viewer.

```
Assignees
-------------------------------------------
[SK] Sarah Kim        [pending icon] Pending
[MC] Mike Chen        [check icon] Approved  2h ago
[AL] Anna Lee         [x icon] Rejected  1h ago
```

**Spec:**
- Section heading: "Assignees", font-size 14px, font-weight 600, `neutral-700`, margin-bottom 12px
- Each assignee row: height 40px, flex row, align-center
  - Avatar: 32px circle, initials fallback with colored background
  - Name: font-size 14px, font-weight 500, `neutral-900`, margin-left 10px
  - Status area (right-aligned):
    - Pending: `Clock` icon (14px, `warning-500`) + "Pending" text, `warning-600`
    - Approved: `Check` icon (14px, `success-500`) + "Approved" text, `success-600` + relative time, `neutral-400`
    - Rejected: `X` icon (14px, `danger-500`) + "Rejected" text, `danger-600` + relative time, `neutral-400`
- Divider below section: 1px `neutral-200`

**Re-assign capability (OWNER only):**
- Small "Edit" link (12px, `primary-600`) next to section heading
- Clicking opens inline edit mode: existing assignees get remove (x) buttons, "Add approver" search field appears below
- "Save" and "Cancel" buttons appear at bottom of section

---

## Shared Patterns

### Empty States

All empty states follow a consistent structure:
- Centered within their container
- Illustration: abstract SVG graphic, 200x160px, `neutral-200` and `neutral-300` tones
- Heading: font-size 18px, font-weight 600, `neutral-700`
- Subtext: font-size 14px, `neutral-500`, max-width 320px, text-center
- CTA button: below subtext, 12px gap. Primary or secondary variant depending on context.

| Context | Heading | Subtext | CTA |
|---------|---------|---------|-----|
| Rules list (no rules) | "No rules yet" | "Create your first rule to get started" | "Create Rule" (primary) |
| Rules list (filters) | "No rules found" | "Try adjusting your filters or search terms" | "Clear filters" (secondary) |
| My Queue (no pending) | "All caught up" | "No approval requests assigned to you" | — |
| My Requests (none) | "No pending requests" | "When you submit rules for approval, they will appear here" | — |
| All Requests (none) | "No approval requests" | "Requests will appear here when rules are submitted for review" | — |

### Loading Skeletons

- Use skeleton rectangles matching the layout of the content they replace
- Animation: Tailwind `animate-pulse` (2s infinite ease-in-out)
- Skeleton color: `neutral-100`
- Table: 8 skeleton rows with column-width-matched rectangles
- Card grid: 6 skeleton cards with content-area rectangles
- Queue: 5 skeleton items

### Filter Chips Animation

- Enter: scale from 0.8 to 1.0 + fade in, 150ms ease-out
- Exit: scale from 1.0 to 0.8 + fade out, 100ms ease-in
- Use `framer-motion` `AnimatePresence` for exit animations (already available in project)

### Confirmation Dialogs

For destructive actions (archive, delete):

```
+------------------------------------------+
|  Delete Rule                         [X] |
|  ----------------------------------------|
|                                          |
|  Are you sure you want to delete         |
|  "Fraud Detection Rule"?                 |
|                                          |
|  This action cannot be undone.           |
|                                          |
|        [Cancel]          [Delete]        |
+------------------------------------------+
```

- "Delete" button: danger variant
- "Cancel" button: ghost variant
- Modal width: 400px
- Destructive action name in button text matches the action ("Delete", "Archive")

### Toast Notifications

- Position: bottom-right, 24px from edges
- Stack: newest on top, max 3 visible, oldest auto-dismiss
- Duration: 5 seconds (success/info), persistent until dismissed (error)
- Types:
  - Success: `success-500` left border, `Check` icon
  - Error: `danger-500` left border, `AlertCircle` icon
  - Info: `primary-500` left border, `Info` icon
- Dimensions: width 360px, min-height 48px, elevation level 2
- Dismiss: X button on right, or auto-dismiss after duration

---

## Responsive Behavior

### Rules Page

| Breakpoint | Sidebar | Search | View Mode | Batch Bar |
|------------|---------|--------|-----------|-----------|
| >= 1280px (Desktop) | 280px, visible | Full width | Table or Card (3 col) | Fixed bottom center |
| 768-1279px (Tablet) | Collapsed by default, toggle to overlay | Full width | Table (compact) or Card (2 col) | Fixed bottom center |
| < 768px (Mobile) | Hidden, accessible via hamburger | Full width, simplified suggestions | Card only (1 col) | Fixed bottom, full width |

**Tablet adjustments:**
- Table view: hide Description excerpt and Tags from Name column (show name only)
- Table view: hide Version column
- Sidebar: overlay mode (slides over content with backdrop)

**Mobile adjustments:**
- No table view available — card view only
- Filter panel: accessible via "Filters" button that opens a bottom sheet
- Search suggestions: full-screen overlay
- Batch actions: simplified to icon-only buttons

### Approvals Page

| Breakpoint | Layout | Queue | Detail |
|------------|--------|-------|--------|
| >= 1280px | Two-panel side by side | 360px left panel | Remaining space |
| 768-1279px | Two-panel, narrower queue | 280px left panel | Remaining space |
| < 768px | Stacked, navigate between | Full-width queue list | Full-width detail (back button) |

**Mobile:** Queue and detail are separate views. Tapping a queue item navigates to detail view with a "Back to queue" button in the header.

---

## Accessibility Notes

### Rules Page
- Table has proper `<thead>`, `<tbody>`, `<th scope="col">` structure
- Sort buttons have `aria-sort` attribute (`ascending`/`descending`/`none`)
- Checkbox "select all" has `aria-label="Select all rules on this page"`
- Individual row checkboxes have `aria-label="Select {rule name}"`
- Card grid uses `role="grid"` with `role="gridcell"` on each card
- Search input has `role="combobox"` with `aria-expanded`, `aria-controls` for suggestions dropdown
- Suggestion items use `role="option"` with `aria-selected`
- Filter chips have `role="listbox"` container, each chip is `role="option"` with dismiss button
- View toggle buttons use `role="radiogroup"` with `role="radio"` and `aria-checked`
- Batch action bar: `role="toolbar"`, `aria-label="Batch actions for {N} selected rules"`
- Floating bar: `aria-live="polite"` to announce selection count changes

### Approvals Page
- Tabs use Shadcn `Tabs` which handles `role="tablist"`, `role="tab"`, `role-tabpanel"` automatically
- Queue items are keyboard navigable (J/K shortcuts retained)
- SLA progress bar: `role="progressbar"` with `aria-valuenow`, `aria-valuemin`, `aria-valuemax`, `aria-label`
- Assignee status: screen reader text includes full status (e.g., "Sarah Kim, Approved 2 hours ago")
- Modal: focus trap, Escape to close, autofocus on first interactive element

### Color Contrast
- All text meets WCAG AA (4.5:1 for normal, 3:1 for large)
- Status badges and type badges use sufficient contrast combinations
- SLA colors are supplemented with text labels (not color-only)
- Selected/active states use border + background (not color alone)

---

## Component Inventory

New or modified Shadcn/ui-based components needed:

| Component | New/Modified | Notes |
|-----------|-------------|-------|
| `SmartSearchBar` | New | Search input + suggestions dropdown + filter chips |
| `FilterChip` | New | Dismissible filter badge with animation |
| `FilterPanel` | New | Accordion sections for sidebar filters |
| `RuleTypeIcon` | New (utility) | Maps rule type to colored Lucide icon |
| `StatusBadge` | Modified | Add lock/bell icon support |
| `RuleCard` | New | Card view component for rules grid |
| `BatchActionBar` | New | Floating dark bar for bulk operations |
| `RequestApprovalModal` | New | Approver selection + note + SLA info |
| `AssigneeSection` | New | Approval detail assignee display |
| `AvatarStack` | New | Overlapping avatar group with +N badge |
| `SlaProgressBar` | New | Colored progress bar with time remaining |
| `SavedFilterPresets` | New | Dropdown for saved filter management |
| `ApprovalQueueItem` | Modified | Add requester, assignees, enhanced SLA |
| `AllRequestsTable` | New | Full table for OWNER/ADMIN approval view |

---

## Keyboard Shortcuts

### Rules Page

| Shortcut | Action | Context |
|----------|--------|---------|
| `/` | Focus search bar | Anywhere on page |
| `Escape` | Clear search / close dropdown / deselect all | Contextual |
| `Enter` | Navigate to selected suggestion / open selected rule | Search / table |
| `Ctrl+A` / `Cmd+A` | Select all rules on current page | Table/card view focused |
| `G` then `T` | Switch to table view | Rules page |
| `G` then `C` | Switch to card view | Rules page |

### Approvals Page (existing + new)

| Shortcut | Action | Context |
|----------|--------|---------|
| `J` | Next queue item | My Queue |
| `K` | Previous queue item | My Queue |
| `A` | Approve selected request | Detail view |
| `R` | Reject selected request | Detail view |
| `Enter` | Open selected queue item detail | Queue list focused |
| `/` | Focus filter search | All Requests tab |

---

## State Management Notes for Engineering

### New Zustand Store Slices

**Rules Page Filters (`useRulesFilterStore`):**
```
{
  searchQuery: string
  selectedTypes: RuleType[]
  selectedStatuses: Status[]
  selectedTags: string[]
  dateRange: { start: Date | null, end: Date | null }
  createdBy: string | null
  viewMode: 'table' | 'card'
  selectedRuleIds: Set<string>
  savedPresets: FilterPreset[]
}
```
- Persist `viewMode` and `savedPresets` to localStorage
- Filter changes trigger React Query refetch with debounced params

**Approvals Page (`useApprovalsStore`):**
```
{
  activeTab: 'my-queue' | 'my-requests' | 'all-requests'
  selectedRequestIds: Set<string>
  queueFilters: { status, assignee, requestedBy, dateRange, ruleType }
}
```

### API Requirements

New or modified endpoints needed:

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `GET /v1/rules` | Modified | Add query params: `search`, `types`, `statuses`, `tags`, `updatedAfter`, `updatedBefore`, `createdBy` |
| `POST /v1/rules/batch/request-approval` | New | Batch approval request: `{ ruleIds, approverIds, note }` |
| `POST /v1/rules/batch/archive` | New | Batch archive: `{ ruleIds }` |
| `POST /v1/rules/batch/export` | New | Batch export: `{ ruleIds, format }` |
| `GET /v1/approvals/my-queue` | New | Approval requests assigned to current user |
| `GET /v1/approvals/my-requests` | New | Approval requests submitted by current user |
| `GET /v1/approvals` | Modified | Add filters: `status`, `assigneeId`, `requestedById`, `ruleType`, date range |
| `PUT /v1/approvals/{id}/assignees` | New | Re-assign approvers: `{ approverIds }` |
| `GET /v1/tags` | New (if not exists) | List tags with rule counts for filter panel |
| `GET /v1/spaces/{id}/approval-settings` | Existing | Get default approvers and SLA for modal |

---

*End of UX Design Spec — Sprint 02*
