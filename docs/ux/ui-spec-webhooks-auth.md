# UI Spec — Webhooks Authentication Enhancement (Epic 5)

**Date:** 2026-03-18
**Author:** UX Designer (BMAD)
**Input:** `docs/prd-webhooks-auth.md`, `docs/architecture/solution-architecture-webhooks-auth.md`
**Design system:** shadcn/ui + Tailwind CSS 3.4, Zinc base, Blue accent, Inter 14px, radius 0.5rem
**Status:** APPROVED — Ready for Frontend Implementation

---

## 1. Personas

### Marcus — Platform Admin / Org Owner
- **Role:** Sets up integrations for the org; manages webhook configurations
- **Goal:** Connect RuleForge to secured third-party APIs (Salesforce, SIEM, monitoring)
- **Frustration:** Current webhook form has no auth fields — he has to work around it
- **Success:** Configure an API Key webhook in <2 minutes with no documentation

### Priya — Rule Author / Editor
- **Role:** Creates and publishes rules; sets up event notifications to her team's Slack and PagerDuty
- **Goal:** Quickly create a webhook to a known endpoint with minimal config
- **Frustration:** Unclear which auth type to pick; wants smart defaults
- **Success:** Create a "Bearer Token" webhook in one form fill, no confusion

---

## 2. Affected Screens

| Screen ID | Screen Name | Change Type |
|---|---|---|
| S-01 | Webhook Creation Modal (CreateWebhookModal) | Major revision — add Auth section |
| S-02 | WebhookSettingsTab | Minor revision — add Auth config display + edit |
| S-03 | WebhookDetailPage | Minor revision — add Metrics tab (5th tab) |
| S-04 | WebhookMetricsTab (new) | New screen |
| S-05 | WebhookListPage | Minor revision — add health badge per row |

---

## 3. Screen S-01: Webhook Creation Modal — Auth Section

### 3.1 Overview

The creation modal gains an **Authentication** section between the Events section and the Submit button. The section uses progressive disclosure: selecting an auth type reveals type-specific credential fields inline.

### 3.2 Layout

```
┌─────────────────────────────────────────────────────────────────┐
│ Create Webhook                                          [×]      │
├─────────────────────────────────────────────────────────────────┤
│  Name *                                                          │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │ My Integration                                            │  │
│  └───────────────────────────────────────────────────────────┘  │
│                                                                  │
│  Endpoint URL *                                                  │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │ https://                                                  │  │
│  └───────────────────────────────────────────────────────────┘  │
│                                                                  │
│  Scope                                                           │
│  ● Organization-wide   ○ Space-specific                          │
│                                                                  │
│  Events                                                          │
│  [rule.published ×] [rule.executed ×] [+ Add Event]              │
│                                                                  │
│  ── Authentication ──────────────────────────────────────────── │
│                                                                  │
│  Auth Type                                                       │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │ None                                                  ▾   │  │
│  └───────────────────────────────────────────────────────────┘  │
│                                                                  │
│  [auth type fields appear here — see 3.3]                        │
│                                                                  │
│  ── Advanced ▶ ─────────────────────────────────────────────── │
│  [collapsed by default — see 3.5]                                │
│                                                                  │
├─────────────────────────────────────────────────────────────────┤
│                                        [Cancel]  [Create Webhook]│
└─────────────────────────────────────────────────────────────────┘
```

### 3.3 Auth Type Dropdown Options

| Option | Label | Icon | Description shown below dropdown |
|---|---|---|---|
| `NONE` | None | Shield-off | "No authentication. Use for public or VPN-protected endpoints." |
| `SIGNING_SECRET` | Signing Secret | Shield-check | "RuleForge signs each request with HMAC-SHA256." |
| `API_KEY` | API Key | Key-round | "Sends a key in a custom header (e.g., X-API-Key)." |
| `BASIC_AUTH` | Basic Auth | Lock | "Sends username and password via Authorization: Basic." |
| `BEARER_TOKEN` | Bearer Token | Badge-check | "Sends a static token via Authorization: Bearer." |

### 3.4 Auth Type Conditional Fields

#### NONE (default)
```
[no additional fields]
ℹ️  No authentication — only use for endpoints that don't require it.
```

#### SIGNING_SECRET
```
Secret (auto-generated or custom)
┌────────────────────────────────────────────┐ [Regenerate]
│ ••••••••••••••••••••                       │
└────────────────────────────────────────────┘
ℹ️  RuleForge adds X-RuleForge-Signature to each request using HMAC-SHA256.
```

#### API_KEY
```
Header Name *
┌────────────────────────────────────────────┐
│ X-API-Key                                  │   ← pre-filled default
└────────────────────────────────────────────┘

API Key *
┌────────────────────────────────────────────┐ [👁 Show]
│ ••••••••••••••••••••                       │
└────────────────────────────────────────────┘
ℹ️  Sent as: <Header Name>: <API Key>
```

#### BASIC_AUTH
```
Username *
┌────────────────────────────────────────────┐
│                                            │
└────────────────────────────────────────────┘

Password *
┌────────────────────────────────────────────┐ [👁 Show]
│ ••••••••••••••••••••                       │
└────────────────────────────────────────────┘
ℹ️  Sent as: Authorization: Basic <base64(username:password)>
```

#### BEARER_TOKEN
```
Token *
┌────────────────────────────────────────────┐ [👁 Show]
│ ••••••••••••••••••••                       │
└────────────────────────────────────────────┘
ℹ️  Sent as: Authorization: Bearer <token>
```

### 3.5 Advanced Section (Retry Config — collapsed by default)

```
── Advanced ▼ ──────────────────────────────────────────────────

  Retry Settings

  Initial Delay        Backoff Multiplier     Max Attempts
  ┌───────────────┐   ┌────────────────────┐  ┌─────────────┐
  │ 30        sec │   │ 2.0                │  │ 5           │
  └───────────────┘   └────────────────────┘  └─────────────┘

  Retry On Status Codes
  [429 ×] [408 ×] [500 ×] [502 ×] [503 ×] [504 ×]  [+ Add]

  ℹ️  Defaults apply to most use cases. Only adjust if your endpoint
      has specific availability patterns.
```

### 3.6 Interaction Specifications

#### Auth Type Change
- **Trigger:** User selects different option from Auth Type dropdown
- **Effect:** Credential fields animate out (100ms fade), new fields animate in (100ms fade)
- **State:** Previously entered credentials in hidden fields are **cleared** (not preserved) to prevent accidental leakage of wrong-type credentials
- **Confirmation:** No confirmation needed — user hasn't saved yet

#### Password / Token Show / Hide
- **Trigger:** Click eye icon
- **Effect:** Input type toggles `password` ↔ `text`
- **Icon:** `Eye` / `EyeOff` from Lucide React
- **Auto-hide:** Reverts to masked after 30s of inactivity

#### Form Validation
| Field | Rule | Error message |
|---|---|---|
| API Key | Required when authType = API_KEY | "API Key is required" |
| Header Name (API_KEY) | Must be valid HTTP header name (alphanumeric + hyphens) | "Invalid header name" |
| Username | Required when authType = BASIC_AUTH | "Username is required" |
| Password | Required when authType = BASIC_AUTH | "Password is required" |
| Token | Required when authType = BEARER_TOKEN | "Token is required" |
| Initial Delay | 1–3600 | "Must be between 1 and 3600 seconds" |
| Max Attempts | 1–25 | "Must be between 1 and 25" |
| Backoff Multiplier | 1.0–10.0 | "Must be between 1.0 and 10.0" |

#### Submit Loading State
- **Trigger:** Click "Create Webhook"
- **Effect:** Button shows `<Loader2 className="animate-spin" />` + "Creating…" text; form fields disabled
- **Success:** Modal closes; `WebhookListPage` shows success toast "Webhook created"
- **Error (400):** Inline field errors shown; button re-enabled
- **Error (5xx):** Toast "Something went wrong. Try again."

### 3.7 VIEWER Role Behaviour
- Auth type dropdown: disabled
- Credential fields: not rendered (hidden, not just disabled)
- Rationale: VIEWER should not know what auth type is configured, even masked

---

## 4. Screen S-02: WebhookSettingsTab — Auth Config Display

### 4.1 Auth Config Card (new addition to existing Settings tab)

```
┌────────────────────────────────────────────────────────────────┐
│ Authentication                                    [Edit] (OWNER)│
├────────────────────────────────────────────────────────────────┤
│  Auth Type    API Key                                           │
│  Header Name  X-API-Key                                         │
│  API Key      ••••••••••••••••  [Copy last 4: ab3f]             │
└────────────────────────────────────────────────────────────────┘
```

- **Auth Type** row: shows the human-readable label (e.g., "API Key", "Bearer Token", "None")
- **Credential rows**: show masked value (`••••••••••••••••`); for API_KEY / BEARER_TOKEN, show last 4 characters as hint
- **Copy button**: copies full decrypted value to clipboard (requires confirm dialog for security-sensitive contexts)
- **[Edit] button**: visible to OWNER only; opens inline edit mode (same fields as creation form)
- **VIEWER**: sees "Authentication" card but all credential fields show `[Restricted]` and no Copy button

### 4.2 Inline Edit Mode

When OWNER clicks [Edit]:
```
┌────────────────────────────────────────────────────────────────┐
│ Authentication                                [Cancel] [Save]  │
├────────────────────────────────────────────────────────────────┤
│  Auth Type    [API Key                      ▾]                  │
│  Header Name  [X-API-Key                    ]                   │
│  API Key      [                             ] [👁]              │
│               (leave blank to keep existing value)              │
└────────────────────────────────────────────────────────────────┘
```

- **Credential field placeholder**: "Leave blank to keep current value" — allows changing auth type without re-entering unchanged credential
- **Save**: calls `PATCH /v1/webhooks/{id}` with only changed fields
- **Audit event**: saved to audit trail as `WEBHOOK_AUTH_UPDATED` (no credential values in payload)

---

## 5. Screen S-03: WebhookDetailPage — Updated Tab Bar

### 5.1 Tab Order (updated)

```
[Settings]  [Security]  [Delivery Logs]  [Events]  [Metrics]
                                                     ↑ new tab
```

The Metrics tab is added as the 5th tab. Existing tabs are unchanged.

---

## 6. Screen S-04: WebhookMetricsTab (New)

### 6.1 Layout

```
┌────────────────────────────────────────────────────────────────┐
│ Delivery Metrics   Last 7 days                    [Refresh ↺]  │
├────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐         │
│  │ Success Rate │  │  Deliveries  │  │  Failures    │         │
│  │   94.3%      │  │    256       │  │     15       │         │
│  │ ▲ vs last wk │  │  total       │  │  (5.7%)      │         │
│  └──────────────┘  └──────────────┘  └──────────────┘         │
│                                                                 │
│  ┌──────────────┐  ┌──────────────┐                           │
│  │  DLQ Depth   │  │ Avg Latency  │                           │
│  │      3       │  │   142 ms     │                           │
│  │ ⚠ 3 in queue│  │  p95: 380ms  │                           │
│  └──────────────┘  └──────────────┘                           │
│                                                                 │
│  ── Delivery Timeline (7 days) ──────────────────────────────  │
│                                                                 │
│  50 ┤      ██                                                  │
│  40 ┤  ██  ██  ██  ██  ██                                      │
│  30 ┤  ██  ██  ██  ██  ██  ██  ██                              │
│  20 ┤  ██  ██  ██  ██  ██  ██  ██                              │
│     └──────────────────────────────────────────────────        │
│      Mon  Tue  Wed  Thu  Fri  Sat  Sun                         │
│            ■ Success   ■ Failed                                 │
│                                                                 │
│  [View Delivery Logs →]                                         │
└────────────────────────────────────────────────────────────────┘
```

### 6.2 Stat Cards

| Metric | Card Variant | Condition |
|---|---|---|
| Success Rate ≥ 95% | Default (neutral) | Healthy |
| Success Rate 80–94% | `border-yellow-400` warning ring | Degraded |
| Success Rate < 80% | `border-destructive` error ring | Unhealthy |
| DLQ Depth > 0 | `border-yellow-400` + warning icon | Attention needed |
| DLQ Depth = 0 | Default | Healthy |

### 6.3 Chart Specification

| Property | Value |
|---|---|
| Library | Recharts (`<BarChart>`) — already in frontend |
| Type | Stacked bar chart |
| X-axis | 7 days (formatted: "Mon", "Tue", etc.) |
| Y-axis | Delivery count (auto-scale) |
| Success bar | `fill="#22c55e"` (green-500) |
| Failed bar | `fill="#ef4444"` (red-500) |
| Tooltip | "Date: {date}\nSuccessful: {n}\nFailed: {n}" |
| Height | 200px |

### 6.4 Empty State

```
┌────────────────────────────────────────────────────────────────┐
│                                                                 │
│                     📊                                         │
│              No delivery data yet                               │
│     Metrics will appear after the first delivery attempt.      │
│                                                                 │
│                  [Test Webhook →]                               │
│                                                                 │
└────────────────────────────────────────────────────────────────┘
```

### 6.5 Error State
```
⚠️  Unable to load metrics  [Retry]
    Check your connection and try again.
```

### 6.6 Loading State
- Stat cards: `<Skeleton className="h-20 w-32" />` × 5
- Chart: `<Skeleton className="h-48 w-full" />`

---

## 7. Screen S-05: WebhookListPage — Health Badge

### 7.1 Health Badge in Webhook Row

Each row in the webhooks table gains a small health indicator badge to the right of the webhook status badge:

```
Name              Status    Health   Events        Last Delivery
──────────────────────────────────────────────────────────────
Salesforce Hook   ACTIVE  ● Healthy  rule.published  2 min ago
PagerDuty Alert   ACTIVE  ● Degraded rule.executed   5 min ago
SIEM Integration  ACTIVE  ▲ Unhealthy rule.published  1 hr ago
```

| Health | Badge | Condition |
|---|---|---|
| Healthy | Green dot `●` | 7d success rate ≥ 95% AND dlqDepth = 0 |
| Degraded | Amber dot `●` | 7d success rate 80–94% OR dlqDepth 1–5 |
| Unhealthy | Red triangle `▲` | 7d success rate < 80% OR dlqDepth > 5 |
| No data | Gray dash `–` | No deliveries in 7 days |

**Implementation note:** `WebhookHealthBadge` component fetches health status from `GET /v1/webhooks/{id}/metrics` (cached 60s in React Query). For list page efficiency, health badges render in parallel using `Promise.all` on initial load, but only for the visible rows (virtual scroll or first-page rows).

### 7.2 Tooltip on Health Badge

Hover over health badge shows:
```
● Degraded
7-day success rate: 87%
DLQ: 2 messages
[View Metrics →]
```

---

## 8. Component Inventory

| Component | shadcn Base | New / Modified |
|---|---|---|
| `WebhookAuthConfigForm` | `<Select>`, `<Input>`, `<Label>`, `<FormField>` | New |
| `WebhookRetryConfigForm` | `<Collapsible>`, `<Input>`, `<Badge>` | New |
| `WebhookMetricsTab` | `<Card>`, Recharts `<BarChart>`, `<Skeleton>` | New |
| `WebhookHealthBadge` | `<Badge>`, `<Tooltip>` | New |
| `CreateWebhookModal` | existing + `WebhookAuthConfigForm` + `WebhookRetryConfigForm` | Modified |
| `WebhookSettingsTab` | existing + auth config card section | Modified |
| `WebhookDetailPage` | existing tabs + "Metrics" tab | Modified |
| `WebhookListPage` | existing table + `WebhookHealthBadge` column | Modified |

---

## 9. Accessibility (WCAG 2.2 AA)

| Requirement | Implementation |
|---|---|
| Auth type dropdown keyboard navigable | `<Select>` from shadcn is fully keyboard accessible (↑/↓/Enter) |
| Password field screen reader | `aria-label="API Key"`, `aria-describedby` → hint text |
| Show/hide password button | `aria-label="Show API Key"` / `"Hide API Key"` — toggled dynamically |
| Credential fields not pre-filled | Never use `autocomplete="current-password"` on auth config forms |
| Metric stats color + text | Success rate never conveyed by color alone — value + label always shown |
| Chart accessible alternative | `aria-label` on chart container + data available in table below chart |
| Health badge | `role="status"` + `aria-label="Health: Degraded"` — not color-only |
| Focus management in modal | On open: focus first input; on close: return focus to trigger button |
| Error announcements | `aria-live="polite"` on form error summary region |

---

## 10. Responsive Behaviour

| Screen | Desktop (≥1280px) | Tablet (768–1279px) | Mobile (<768px) |
|---|---|---|---|
| Create Webhook Modal | 540px fixed width | 90vw | Full screen Sheet |
| WebhookSettingsTab auth card | Side-by-side label/value | Stack to column | Stack to column |
| Metrics stat cards | 5 cards in one row | 3 + 2 rows | 2 + 2 + 1 rows |
| Metrics chart | Full width | Full width | Scrollable horizontally |
| WebhookListPage health badge | In table column | In table column | In card row |

---

## 11. Error State Mapping

| Error | HTTP | User Message | Action |
|---|---|---|---|
| Auth config invalid | 400 | Field-specific inline error | Scroll to error |
| Credential too long | 400 | "Value exceeds maximum length" | Show limit hint |
| Webhook URL unreachable (test) | 400/timeout | "Could not reach endpoint. Check the URL." | Retry or update URL |
| Unauthorized (session expired) | 401 | "Session expired. Refreshing…" | Auto-refresh JWT |
| Forbidden (wrong role) | 403 | "You don't have permission to edit auth config." | — |
| Metrics unavailable | 500 | "Unable to load metrics" | [Retry] button |

---

**Handoff ready:** All screens specified with states, interactions, accessibility, and responsive behaviour.
**Frontend engineer:** Implement `WebhookAuthConfigForm` first (US-1 through US-5 depend on it), then `WebhookRetryConfigForm` (US-6), then `WebhookMetricsTab` (US-7).
