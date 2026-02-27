# UX Design Specification: Feature Sprint 01

**Scope:** Approvals Redesign, Analytics Redesign, A/B Testing Redesign
**Date:** 2026-02-27
**Designer:** BMAD UX/UI Designer Agent
**Tech Stack:** React 18, TypeScript, Tailwind CSS, Shadcn/ui, Recharts, Lucide React, React Router v6

---

## Table of Contents

1. [Approvals Redesign](#1-approvals-redesign)
2. [Analytics Redesign](#2-analytics-redesign)
3. [A/B Testing Redesign](#3-ab-testing-redesign)
4. [Shared Design Patterns](#4-shared-design-patterns)
5. [Navigation Changes Summary](#5-navigation-changes-summary)

---

## Current State Analysis

### Approvals (`ApprovalQueuePage.tsx`)
- Single flat table with rule name, version, type, date, approve/reject buttons
- No filtering, no detail panel, no comments, no bulk actions
- Reject modal is a bare textarea -- no conversation threading
- No SLA or urgency indicators
- Uses `window.confirm()` for approve -- poor UX

### Analytics (`AnalyticsDashboardPage.tsx`)
- Time range selector (1h/24h/7d/30d) as button group -- functional but limited
- Chart data is **derived from metrics with random jitter** (lines 47-58, 82-92) -- not real time-series
- No drill-down from metric cards
- No decision log filtering, no export
- No auto-refresh

### Analytics - Rule Stats (`RuleStatsPage.tsx`)
- Four static metric cards, no trend data, no sparklines
- No historical comparison
- No decision log scoped to the rule

### A/B Testing (`AbTestListPage.tsx`, `AbTestCard.tsx`)
- Card grid with status badge and traffic split bar
- `handleViewResults` sets state but **does nothing** (line 72-74) -- broken
- No detail page route exists
- No tabs for filtering by status
- Create modal is basic -- no hypothesis, sample size, or significance fields

---

## 1. Approvals Redesign

### 1.1 Page Layout

The Approval Center uses a **master-detail two-panel layout** (list/detail split).

```
+-------------------------------------------------------------------+
| Approval Center                          [3 pending] [Bulk Actions]|
+-------------------------------------------------------------------+
| FILTERS: [Rule Type v] [Space v] [Date Range] [SLA Status v]      |
+-------------------+-----------------------------------------------+
|  QUEUE LIST       |  DETAIL PANEL                                  |
|  (scrollable)     |                                                |
|  +-------------+  |  Rule: "Pricing Discount v3"                   |
|  | Rule name   |  |  Submitted by: editor@ruleforge.io             |
|  | v3 | TABLE  |  |  Submitted: Feb 25 | Due in 2h [!]             |
|  | Due in 2h ! |  |  +-----------------------------------------+  |
|  +-------------+  |  | VERSION DIFF TAB | COMMENTS TAB          |  |
|  +-------------+  |  +-----------------------------------------+  |
|  | Rule name   |  |  |  [Inline diff viewer]                   |  |
|  | v2 | TREE   |  |  |  - condition: age >= 18                 |  |
|  | 5d ago      |  |  |  + condition: age >= 21                 |  |
|  +-------------+  |  |                                         |  |
|  +-------------+  |  +-----------------------------------------+  |
|  | Rule name   |  |                                                |
|  | v1 | SCRIPT |  |  +-------------------------------------------+|
|  | No deadline  |  |  | COMMENT THREAD (collapsed below diff)     ||
|  +-------------+  |  | [avatar] John: "Why change to 21?"         ||
|                   |  | [avatar] Jane: "Legal compliance update"    ||
|                   |  | [Type a comment...             ] [Send]     ||
|                   |  +-------------------------------------------+|
|                   |                                                |
|                   |    [Approve (A)]  [Reject (R)]  [Skip (N)]     |
+-------------------+-----------------------------------------------+
| Keyboard: A=Approve  R=Reject  N=Next  P=Previous  ?=Help         |
+-------------------------------------------------------------------+
```

**Panel proportions:** Left panel 360px fixed width, right panel fills remaining space. On screens below 1024px, the layout collapses to a single-column view where selecting a queue item pushes to the detail view with a back button.

### 1.2 Component Inventory

| Component | Type | Props / Key Details |
|---|---|---|
| `ApprovalCenterPage` | Page (replaces `ApprovalQueuePage`) | Top-level layout with two panels |
| `ApprovalFilterBar` | Component | `filters: ApprovalFilters`, `onChange`, `pendingCount: number` |
| `ApprovalQueueList` | Component | `items: ApprovalItem[]`, `selectedId`, `onSelect`, `bulkSelected: Set<string>`, `onBulkToggle` |
| `ApprovalQueueItem` | Component | `item: ApprovalItem`, `selected: boolean`, `checked: boolean`, `onSelect`, `onCheck` |
| `ApprovalDetailPanel` | Component | `approval: ApprovalItem \| null` |
| `ApprovalDiffViewer` | Component | `previousDefinition: object`, `currentDefinition: object` -- JSON diff with add/remove highlighting |
| `ApprovalCommentThread` | Component | `comments: Comment[]`, `onAddComment: (text: string) => void` |
| `SLABadge` | Component | `deadline: string \| null` -- renders "Due in Xh" with color coding (green >24h, yellow 2-24h, red <2h, gray if no deadline) |
| `BulkActionBar` | Component | `selectedCount: number`, `onApproveAll`, `onRejectAll`, `onClearSelection` -- appears when >= 1 item checked |
| `KeyboardShortcutHelp` | Component (modal) | Displays shortcut reference, triggered by `?` key |

**Types:**

```typescript
interface ApprovalItem {
  id: string;
  ruleId: string;
  ruleName: string;
  ruleType: string;
  version: number;
  previousVersion: number | null;
  spaceName: string;
  submittedBy: string;
  submittedAt: string;
  deadline: string | null;
  commentCount: number;
  definition: object;
  previousDefinition: object | null;
}

interface ApprovalFilters {
  ruleType: string | null;
  spaceId: string | null;
  dateRange: { from: string; to: string } | null;
  slaStatus: 'overdue' | 'due-soon' | 'on-track' | null;
}
```

### 1.3 User Journey

1. User clicks "Approvals" in sidebar -- badge shows pending count (e.g., "Approvals (3)")
2. Page loads with queue list sorted by urgency (overdue first, then nearest deadline)
3. First item is auto-selected; detail panel shows its diff and comment thread
4. User reviews the diff (changes highlighted in green/red like GitHub)
5. User scrolls down to see/add comments if needed
6. User presses `A` to approve or clicks [Approve] button
7. Toast confirms "Version approved and published"
8. Queue auto-advances to next item; previous item fades out of list
9. For bulk operations: user checks multiple items via checkbox, bulk action bar appears at top with "Approve 3 selected" / "Reject 3 selected" buttons
10. For rejection: clicking [Reject] or pressing `R` expands an inline rejection reason textarea below the action buttons (not a modal) with required reason field

### 1.4 Navigation Changes

| Change | Details |
|---|---|
| Sidebar link | "Approvals" with `Badge` showing pending count fetched from `GET /v1/rules?status=PENDING_APPROVAL&countOnly=true` |
| Route | `/approvals` -- no change to path, but component changes from `ApprovalQueuePage` to `ApprovalCenterPage` |
| Breadcrumb | `Approvals` (single level) |

### 1.5 Interaction Patterns

**Hover states:**
- Queue items: light background highlight (`bg-gray-50`), cursor pointer
- Selected queue item: left border accent (`border-l-2 border-indigo-500 bg-indigo-50`)

**Loading states:**
- Queue list: skeleton rows (4 items)
- Detail panel: skeleton block with diff placeholder
- Button loading: spinner replaces icon during mutation

**Empty states:**
- No pending approvals: centered illustration with "All caught up!" message and description "No rules are pending approval."
- No comments yet: "No comments yet. Start a conversation." with ghost text in input

**Error states:**
- Failed to load queue: error banner with retry button
- Failed to approve/reject: toast error, button resets to enabled state
- Stale data (another user approved): toast warning "This item was already approved by another user", item removed from list

**Keyboard shortcuts:**
| Key | Action | Context |
|---|---|---|
| `A` | Approve selected | When detail panel is focused |
| `R` | Open reject flow | When detail panel is focused |
| `N` / `J` | Select next item | Queue navigation |
| `P` / `K` | Select previous item | Queue navigation |
| `X` | Toggle bulk checkbox | On selected queue item |
| `?` | Show keyboard help | Global on page |
| `Escape` | Cancel reject / clear selection | Context-dependent |

---

## 2. Analytics Redesign

### 2.1 Page Layout -- Dashboard

```
+---------------------------------------------------------------------+
| Analytics                                                            |
+---------------------------------------------------------------------+
| TABS: [Dashboard] [Decision Log] [Alerts]                           |
+---------------------------------------------------------------------+
| [Date Range Picker: Feb 20 - Feb 27 v] [Presets: 1h|24h|7d|30d|90d]|
| [Auto-refresh: Off v]                                                |
+---------------------------------------------------------------------+
| +---------------+ +---------------+ +---------------+ +-----------+ |
| | Total         | | Throughput    | | Avg Latency   | | Error Rate| |
| | Decisions     | | /min          | | ms            | | %         | |
| | 142,891       | | 23.4          | | 12.3 ms       | | 0.42%     | |
| | +8.2% vs prev | | +2.1% vs prev | | -1.1ms vs prev| | -0.1% vs  | |
| | [clickable]   | | [clickable]   | | [clickable]   | | [clickable]| |
| +---------------+ +---------------+ +---------------+ +-----------+ |
+---------------------------------------------------------------------+
| +----------------------------------+ +-----------------------------+ |
| | Execution Timeline               | | Rule Type Distribution      | |
| | [Smoothed area chart]            | | [Donut chart with labels]   | |
| | Real ES time-series data         | | TABLE: 62%                  | |
| | x: timestamps, y: count         | | TREE: 24%                   | |
| |                                  | | SCRIPT: 8%                  | |
| |                                  | | OTHER: 6%                   | |
| +----------------------------------+ +-----------------------------+ |
| +----------------------------------+ +-----------------------------+ |
| | Pass / Fail Over Time            | | Latency Percentiles         | |
| | [Stacked bar per hour]           | | [Band chart: p50/p95/p99]   | |
| | Green=pass, Red=fail             | | Shaded area between bands   | |
| +----------------------------------+ +-----------------------------+ |
+---------------------------------------------------------------------+
| Top Rules by Execution Count                                         |
| [Table with sparkline column, clickable rows -> /analytics/rules/:id]|
+---------------------------------------------------------------------+
```

### 2.2 Page Layout -- Decision Log Tab

```
+---------------------------------------------------------------------+
| Analytics > Decision Log                                             |
+---------------------------------------------------------------------+
| FILTER BAR:                                                          |
| [Search rule name...] [Status: All v] [Latency: Any v] [Export CSV] |
+---------------------------------------------------------------------+
| | Rule Name    | Status  | Latency | Timestamp        | Expand     | |
| |--------------|---------|---------|------------------|------------|  |
| | Pricing v3   | PASS    | 4ms     | Feb 27 14:32:01  | [>]        | |
| |   Expanded: Input JSON | Output JSON | Trace                     | |
| | Credit Check | FAIL    | 12ms    | Feb 27 14:31:58  | [>]        | |
| | Discount Calc| PASS    | 8ms     | Feb 27 14:31:55  | [>]        | |
| |              |         |         |                  |            | |
| [< Page 1 of 47 >]                                                  |
+---------------------------------------------------------------------+
```

### 2.3 Page Layout -- Alerts Tab

```
+---------------------------------------------------------------------+
| Analytics > Alerts                                                   |
+---------------------------------------------------------------------+
| [+ Create Alert Rule]                                                |
+---------------------------------------------------------------------+
| | Alert Name          | Rule       | Condition           | Status  | |
| |---------------------|------------|---------------------|---------|  |
| | High Error Rate     | Pricing v3 | error_rate > 5%     | ACTIVE  | |
| | Slow Latency        | All rules  | p95_latency > 200ms | ACTIVE  | |
| | No Executions       | Credit Chk | exec_count = 0 / 1h | PAUSED  | |
+---------------------------------------------------------------------+
```

### 2.4 Component Inventory

| Component | Type | Props / Key Details |
|---|---|---|
| `AnalyticsDashboardPage` | Page (replaces existing) | Tab-based layout: Dashboard / Decision Log / Alerts |
| `AnalyticsDateRangePicker` | Component | `range: { from: Date; to: Date }`, `presets`, `onChange` -- uses Shadcn DateRangePicker with presets |
| `AutoRefreshToggle` | Component | `interval: number \| null`, `onChange` -- dropdown: Off, 30s, 1m, 5m |
| `MetricCard` (enhanced) | Component | Add `trend: number` (% change vs previous period), `onClick` (drill-down), `sparklineData?: number[]` |
| `ExecutionTimelineChart` (rewrite) | Component | `data: TimeSeriesPoint[]` from ES -- smoothed area chart, not random jitter |
| `ExecutionsByTypeChart` (rewrite) | Component | `data: TypeCount[]` -- donut chart with percentage labels inside |
| `PassFailStackedChart` (new) | Component | `data: HourlyPassFail[]` -- stacked bar chart grouped by hour |
| `LatencyBandChart` (new, replaces `LatencyChart`) | Component | `data: LatencyPercentile[]` -- area band chart showing p50/p95/p99 over time |
| `TopRulesTable` (enhanced) | Component | Add inline sparkline column (last 7 days), clickable rows navigate to `/analytics/rules/:id` |
| `DecisionLogTable` (enhanced) | Component | Add expandable rows (input/output JSON viewer), filter bar, CSV export button |
| `DecisionLogFilterBar` | Component | `filters: LogFilters`, `onChange`, `onExport` |
| `DecisionLogExpandedRow` | Component | `input: object`, `output: object`, `trace?: TraceNode[]` -- collapsible JSON viewers |
| `AlertListTable` | Component | `alerts: AlertRule[]`, `onEdit`, `onToggle`, `onDelete` |
| `CreateAlertModal` | Component | `onCreated` -- form: name, rule (or "All"), metric, operator, threshold, notification channel |

**Types:**

```typescript
interface TimeSeriesPoint {
  timestamp: string;  // ISO from Elasticsearch
  count: number;
}

interface HourlyPassFail {
  hour: string;
  passed: number;
  failed: number;
}

interface LatencyPercentile {
  timestamp: string;
  p50: number;
  p95: number;
  p99: number;
}

interface LogFilters {
  ruleNameSearch: string;
  status: 'ALL' | 'PASS' | 'FAIL';
  latencyRange: { min: number; max: number } | null;
}

interface AlertRule {
  id: string;
  name: string;
  ruleId: string | null;  // null = all rules
  metric: 'error_rate' | 'p95_latency' | 'exec_count';
  operator: 'gt' | 'lt' | 'eq';
  threshold: number;
  windowMinutes: number;
  status: 'ACTIVE' | 'PAUSED';
  lastTriggered: string | null;
}
```

### 2.5 User Journey -- Dashboard

1. User clicks "Analytics" in sidebar
2. Dashboard tab loads with default 24h range, auto-refresh off
3. Metric cards show real aggregated data with trend % vs previous equivalent period
4. User clicks "Total Decisions" metric card -- switches to Decision Log tab with no filters (shows all)
5. User clicks "Error Rate" metric card -- switches to Decision Log tab pre-filtered to `status=FAIL`
6. User changes date range to custom range (datepicker opens calendar)
7. Charts re-fetch from Elasticsearch with new time bounds
8. User enables auto-refresh at 30s -- small pulsing dot indicator appears next to toggle
9. User clicks a row in Top Rules table -- navigates to `/analytics/rules/:id`

### 2.6 User Journey -- Decision Log

1. User clicks "Decision Log" tab
2. Table loads with paginated results (20 per page)
3. User types rule name in search -- table filters with 300ms debounce
4. User clicks expand arrow on a row -- row expands to show Input JSON (left) and Output JSON (right) with syntax highlighting
5. User clicks "Export CSV" -- downloads filtered results (up to 10,000 rows)

### 2.7 Rule Stats Page Enhancements

Add to the existing `RuleStatsPage`:
- **30-day trend sparkline** next to each metric card (inline, small area chart)
- **Comparison badge**: "+12% vs previous 30 days" in green/red
- **Execution history chart**: time-series for this specific rule over the selected period
- **Decision log table** scoped to this rule ID (reuse `DecisionLogTable` with fixed `ruleId` filter)
- **Breadcrumb**: `Analytics > [Rule Name]`

### 2.8 Navigation Changes

| Change | Details |
|---|---|
| Route `/analytics` | Now has 3 tabs: Dashboard (default), Decision Log, Alerts |
| Route `/analytics/rules/:ruleId` | Enhanced with sparklines and scoped decision log |
| Route `/analytics/alerts` | Tab within analytics page (not separate route -- tab state in URL hash `#alerts`) |
| Sidebar | "Analytics" link unchanged; add sub-item "Alerts" with count of active alert rules |

### 2.9 Interaction Patterns

**Hover states:**
- Metric cards: subtle shadow elevation on hover, cursor pointer
- Chart data points: tooltip with exact values
- Top Rules table rows: background highlight, cursor pointer

**Loading states:**
- Metric cards: `SkeletonCard` (existing)
- Charts: skeleton rectangle matching chart dimensions
- Decision log: skeleton rows (5 rows)
- Auto-refresh: small spinner icon next to refresh toggle during data fetch

**Empty states:**
- No data for range: "No execution data found for this time range. Try expanding the date range or check that rules are being executed."
- No alerts configured: "No alert rules configured. Create your first alert to get notified about anomalies." with [Create Alert] button
- Decision log empty: "No decisions match your filters." with [Clear Filters] link

**Error states:**
- Elasticsearch unavailable: "Analytics data is temporarily unavailable. Execution data will appear when the analytics service reconnects." (non-blocking -- page still renders with empty state)
- Export failure: toast error "Failed to export CSV. Please try again."

---

## 3. A/B Testing Redesign

### 3.1 Page Layout -- Test List

```
+---------------------------------------------------------------------+
| A/B Tests                                        [+ Create Test]     |
+---------------------------------------------------------------------+
| TABS: [Active (2)] [Scheduled (1)] [Completed (5)] [All (8)]        |
+---------------------------------------------------------------------+
| +---------------------------+ +---------------------------+          |
| | Pricing Discount Test     | | Credit Check Optimization |          |
| | ACTIVE [live dot]         | | ACTIVE [live dot]         |          |
| | v2 vs v3                  | | v1 vs v4                  |          |
| | [====60%====|==40%==]     | | [===50%===|===50%===]     |          |
| | A: 12,340 req | B: 8,227 | | A: 5,621 req | B: 5,598   |          |
| | Avg: 4.2ms  | Avg: 3.8ms | | Err: 0.1%  | Err: 0.3%   |          |
| | [View Details]            | | [View Details]             |          |
| +---------------------------+ +---------------------------+          |
+---------------------------------------------------------------------+
```

### 3.2 Page Layout -- Test Detail (`/ab-tests/:id`)

```
+---------------------------------------------------------------------+
| < Back to A/B Tests                                                  |
| Pricing Discount Test                     ACTIVE [Promote Winner v]  |
| Rule: Pricing Discount | v2 vs v3 | Started Feb 20                  |
+---------------------------------------------------------------------+
| Hypothesis: "Version 3 with simplified conditions will reduce        |
| latency without increasing error rate"                               |
| Min sample: 10,000 | Significance: 95% | Current: 87% confidence    |
+---------------------------------------------------------------------+
| METRIC COMPARISON                                                    |
| +--------------------+--------------------+-------------------------+|
| | Metric             | Version A (v2)     | Version B (v3)          ||
| |--------------------|--------------------+-------------------------||
| | Total Requests     | 12,340             | 8,227                   ||
| | Avg Latency        | 4.2 ms             | 3.8 ms  [*winner]      ||
| | Error Rate         | 0.42%              | 0.38%                   ||
| | p95 Latency        | 12.1 ms            | 9.8 ms  [*winner]      ||
| +--------------------+--------------------+-------------------------+|
+---------------------------------------------------------------------+
| SIGNIFICANCE: [==========87%==========] 95% threshold                |
| "Not yet significant. Need ~3,200 more samples."                     |
+---------------------------------------------------------------------+
| PERFORMANCE OVER TIME                                                |
| [Dual-line chart: A vs B latency over time]                          |
| [Dual-line chart: A vs B error rate over time]                       |
+---------------------------------------------------------------------+
| TRAFFIC SPLIT                                                        |
| [====== Version A: 60% (12,340) ======|==== Version B: 40% (8,227)==]|
+---------------------------------------------------------------------+
| DECISION LOG (scoped to this test)                                   |
| [Reuses DecisionLogTable with testId filter]                         |
+---------------------------------------------------------------------+
```

### 3.3 Component Inventory

| Component | Type | Props / Key Details |
|---|---|---|
| `AbTestListPage` (rewrite) | Page | Tabbed layout: Active / Scheduled / Completed / All |
| `AbTestStatusTabs` | Component | `counts: Record<string, number>`, `activeTab`, `onTabChange` |
| `AbTestCard` (enhanced) | Component | Add live execution counts, avg latency per variant, pulsing dot for ACTIVE |
| `AbTestDetailPage` (new) | Page | Full detail view at `/ab-tests/:id` |
| `AbTestMetricComparison` | Component | `metricsA: VariantMetrics`, `metricsB: VariantMetrics` -- table with winner indicators |
| `SignificanceMeter` | Component | `confidence: number`, `threshold: number`, `samplesNeeded: number` -- progress bar with label |
| `VariantPerformanceChart` | Component | `dataA: TimeSeriesPoint[]`, `dataB: TimeSeriesPoint[]`, `metric: string` -- dual line overlay chart |
| `TrafficSplitBar` (enhanced) | Component | `splitA: number`, `splitB: number`, `countA: number`, `countB: number` -- adds execution counts |
| `PromoteWinnerButton` | Component | `testId`, `enabled: boolean`, `winner: 'A' \| 'B' \| null` -- dropdown: Promote A / Promote B / End without winner |
| `CreateAbTestModal` (enhanced) | Component | Add fields: hypothesis (textarea), minSampleSize (number), significanceThreshold (select: 90%/95%/99%) |

**Types:**

```typescript
interface AbTestDetail extends AbTest {
  hypothesis: string | null;
  minSampleSize: number;
  significanceThreshold: number;  // 0.90, 0.95, or 0.99
  currentConfidence: number;      // 0.0 to 1.0
  winner: 'A' | 'B' | null;
  metricsA: VariantMetrics;
  metricsB: VariantMetrics;
}

interface VariantMetrics {
  totalRequests: number;
  avgLatencyMs: number;
  p95LatencyMs: number;
  errorRate: number;
  errorCount: number;
}

interface AbTestTimeSeriesPoint {
  timestamp: string;
  valueA: number;
  valueB: number;
}
```

### 3.4 User Journey -- Creating a Test

1. User navigates to `/ab-tests`
2. Clicks [+ Create Test] button in page header
3. Modal opens with fields:
   - **Rule**: searchable dropdown (existing)
   - **Version A**: dropdown populated from rule versions (existing)
   - **Version B**: dropdown populated from rule versions (existing)
   - **Test Name**: text input (existing)
   - **Hypothesis**: textarea -- "What do you expect to happen?" (NEW)
   - **Traffic Split**: slider 10%-90% (existing)
   - **Min Sample Size**: number input, default 1000 (NEW)
   - **Significance Threshold**: select 90%/95%/99%, default 95% (NEW)
   - **Schedule**: optional start/end date pickers (existing)
4. User fills in fields and clicks [Create Test]
5. Test appears in Active (or Scheduled) tab
6. Toast: "A/B test created successfully"

### 3.5 User Journey -- Monitoring a Test

1. User sees test card on list page with live counts (auto-refreshed every 30s when ACTIVE)
2. Clicks [View Details] -- navigates to `/ab-tests/:testId`
3. Detail page shows hypothesis, live metrics comparison, significance meter
4. Significance meter shows progress toward threshold: "87% confidence -- need ~3,200 more samples"
5. Performance charts show A vs B over time for latency and error rate
6. When significance >= threshold, the `SignificanceMeter` turns green and `PromoteWinnerButton` becomes enabled
7. User clicks [Promote Winner] dropdown, selects "Promote Version B"
8. Confirmation dialog: "This will route 100% of traffic to v3 and end the test. Continue?"
9. Test status changes to COMPLETED, winner recorded

### 3.6 Navigation Changes

| Change | Details |
|---|---|
| Route `/ab-tests` | Enhanced list page with tabs |
| Route `/ab-tests/:id` (NEW) | Test detail page |
| Sidebar | "A/B Tests" link unchanged |
| Breadcrumb on detail | `A/B Tests > [Test Name]` |

### 3.7 Interaction Patterns

**Hover states:**
- Test cards: shadow elevation on hover, cursor pointer on [View Details]
- Metric comparison cells: highlight row on hover
- Winner indicator: tooltip explaining "Lower latency with statistical significance"

**Loading states:**
- List page: skeleton cards (3 cards in grid)
- Detail page: skeleton blocks for metric table, charts, significance meter
- Live counters: subtle number transition animation (count up) when data refreshes

**Empty states:**
- No tests at all: EmptyState with flask icon (existing) + "Create your first A/B test to compare rule versions side by side."
- No tests in filtered tab (e.g., Active): "No active tests. Scheduled and completed tests appear in their respective tabs."
- Test has no execution data yet: "Waiting for first executions. Data will appear once traffic reaches this test."

**Error states:**
- Failed to load test detail: error banner with retry
- Failed to promote winner: toast error, button resets
- Test already stopped by another user: redirect to list with toast warning

---

## 4. Shared Design Patterns

### 4.1 Date Range Picker

Used in Analytics and potentially A/B Testing. Consistent component with:
- Calendar popover for custom range selection (Shadcn `DateRangePicker`)
- Preset buttons: 1h, 24h, 7d, 30d, 90d (pill group)
- Display format: "Feb 20 - Feb 27, 2026"
- Selecting a preset updates both the picker display and the active pill

### 4.2 Expandable Table Rows

Used in Decision Log and test detail decision log:
- Chevron icon rotates 90 degrees on expand (200ms ease-out)
- Expanded content has a slightly darker background (`bg-gray-50`)
- JSON displayed with syntax highlighting (use `react-json-view-lite` or similar lightweight library)
- Expand/collapse animation: height transition 200ms

### 4.3 Metric Cards with Drill-Down

Enhanced `MetricCard` pattern used across Analytics and A/B Testing:
- Existing: label, value, description, icon
- Added: `trend` (percentage change with up/down arrow and green/red coloring)
- Added: `onClick` handler for drill-down navigation
- Added: optional `sparklineData` for inline mini area chart (32px tall)
- Hover state: `cursor-pointer` + subtle shadow when clickable

### 4.4 Skeleton Loading

All pages use consistent skeleton patterns:
- Cards: rounded rectangle with pulse animation (Shadcn `Skeleton`)
- Tables: 5 skeleton rows with varying column widths
- Charts: single rounded rectangle matching chart container dimensions
- Text: multiple lines with decreasing widths (100%, 80%, 60%)

### 4.5 Error Recovery

Consistent across all three features:
- **Network errors**: banner with retry button, auto-retry 3 times with exponential backoff
- **Auth expired (401)**: redirect to login, preserve current URL for redirect-back
- **Permission denied (403)**: inline message "You don't have permission to perform this action"
- **Stale data conflicts**: toast warning with explanation, auto-refresh data
- **Server errors (500)**: error banner with retry button and "If the issue persists, contact your administrator"

### 4.6 Toast Notifications

All mutations use the existing `useToastStore`:
- Success: green left border, auto-dismiss 5 seconds
- Error: red left border, persists until dismissed
- Warning: yellow left border, auto-dismiss 8 seconds

---

## 5. Navigation Changes Summary

### Sidebar Updates

```
Rules
Approvals (3)          <-- badge with pending count
Spaces
Tags
-----------
Analytics
  Dashboard            <-- default sub-route (tab-based, not separate route)
  Alerts (1)           <-- sub-item showing active alert count
A/B Tests
Schedules
Connectors
-----------
Audit Log
Webhooks
API Keys
-----------
Settings
```

The Approvals badge count is fetched via a lightweight polling query (every 60 seconds) or pushed via WebSocket if available.

### New Routes

| Route | Component | Description |
|---|---|---|
| `/approvals` | `ApprovalCenterPage` | Two-panel approval center (replaces existing) |
| `/analytics` | `AnalyticsDashboardPage` | Tabbed: Dashboard / Decision Log / Alerts |
| `/analytics/rules/:ruleId` | `RuleStatsPage` | Enhanced with sparklines and scoped log |
| `/ab-tests` | `AbTestListPage` | Tabbed list with live counters |
| `/ab-tests/:id` | `AbTestDetailPage` (NEW) | Full experiment detail view |

### Breadcrumb Patterns

| Page | Breadcrumb |
|---|---|
| Approval Center | `Approvals` |
| Analytics Dashboard | `Analytics` |
| Analytics - Decision Log | `Analytics > Decision Log` (tab, no breadcrumb change) |
| Analytics - Alerts | `Analytics > Alerts` (tab, no breadcrumb change) |
| Rule Stats | `Analytics > [Rule Name]` |
| A/B Test List | `A/B Tests` |
| A/B Test Detail | `A/B Tests > [Test Name]` |

---

## 6. Accessibility Notes

All new components must meet WCAG 2.2 AA:

- **Keyboard navigation**: All interactive elements reachable via Tab. Custom shortcuts (A/R/N) only active when Approval Center is focused and do not conflict with browser defaults.
- **Screen readers**: Approval queue items announce rule name, version, type, and SLA status. Chart data available via `aria-label` with summary text. Metric cards announce label + value + trend.
- **Color contrast**: All text meets 4.5:1 ratio. Status badges use both color and text label. Chart colors accompanied by pattern/shape differentiation for color-blind users.
- **Focus management**: When an approval is processed, focus moves to next queue item. Modal trap on create/reject flows with Escape to close.
- **Live regions**: Pending count badge uses `aria-live="polite"`. Auto-refresh data updates use `aria-live="polite"` on metric values. Toast notifications use `role="status"`.

---

## 7. Responsive Behavior

| Breakpoint | Approvals | Analytics | A/B Tests |
|---|---|---|---|
| >= 1280px (Desktop) | Two-panel side-by-side | Full grid layout | 3-column card grid |
| 768-1279px (Tablet) | Two-panel, left panel narrower (280px) | 2-column chart grid | 2-column card grid |
| < 768px (Mobile) | Single column, list view with tap-to-detail navigation | Single column, stacked cards and charts | Single column card list |

For mobile on Approvals, tapping a queue item navigates to a full-screen detail view with a back button at the top. Bulk actions are accessible via a floating action button.
