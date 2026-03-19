# UI Spec — Webhook Auth v2: OAuth 2.0 & Custom Header (Epic 6)

**Date:** 2026-03-19
**Author:** UX Designer (BMAD)
**Input:** `docs/prd-webhooks-oauth2-customheader.md`, `docs/architecture/solution-architecture-webhooks-oauth2-customheader.md`
**Design system:** shadcn/ui + Tailwind CSS 3.4, Zinc base, Blue accent, Inter 14px, radius 0.5rem
**Existing spec reference:** `docs/ux/ui-spec-webhooks-auth.md` (Epic 5 — continue design language exactly)
**Status:** APPROVED — Ready for Frontend Implementation

---

## 1. Personas

Same personas as Epic 5. Emphasis for this epic:

**Marcus (Org Owner)** is setting up a webhook to Salesforce's REST API, which uses OAuth 2.0. He knows the token URL (from Salesforce connected app), client ID, and client secret. He wants:
- Clear field labels that map to what Salesforce shows him
- Confidence that his client secret is not displayed back to him
- A helpful hint about what scope format to use

**Priya (Rule Author / Editor)** is connecting to an internal API gateway that uses a proprietary `X-Service-Auth` header. She wants:
- One field for the header name (she already knows it)
- One field for the token value
- No confusion with the existing "API Key" option

---

## 2. Affected Components

| Component | Change Type | Story |
|---|---|---|
| `WebhookAuthConfigForm.tsx` | Additive — two new auth type field groups | S-026 |
| `WebhookSettingsTab.tsx` | Additive — display + rotate controls for new types | S-026 |
| `CreateWebhookModal.tsx` | Minor — no layout change; inherits new options from form | S-026 |

---

## 3. Auth Type Dropdown — Updated Option List

The existing dropdown currently shows 5 options. This epic adds 2 more. Updated option list for `AUTH_TYPE_OPTIONS` in `WebhookAuthConfigForm.tsx`:

| # | Value | Label | Icon (Lucide) | Description |
|---|---|---|---|---|
| 1 | `NONE` | None | `ShieldOff` | "No authentication. Use for public or VPN-protected endpoints." |
| 2 | `SIGNING_SECRET` | Signing Secret | `ShieldCheck` | "RuleForge signs each request with HMAC-SHA256." |
| 3 | `API_KEY` | API Key | `KeyRound` | "Sends a key in a custom header (e.g., X-API-Key)." |
| 4 | `BASIC_AUTH` | Basic Auth | `Lock` | "Sends username and password via Authorization: Basic." |
| 5 | `BEARER_TOKEN` | Bearer Token | `BadgeCheck` | "Sends a static token via Authorization: Bearer." |
| 6 | `CUSTOM_HEADER` | Custom Header | `Tag` | "Sends any value in a header you specify. Flexible — for proprietary API auth." |
| 7 | `OAUTH2_CLIENT_CREDENTIALS` | OAuth 2.0 (Client Credentials) | `RefreshCw` | "Fetches a Bearer token automatically using client credentials. For Salesforce, Azure AD, Okta, and similar." |

**Ordering:** OAuth 2.0 and Custom Header appear after the existing types (appended), preserving muscle memory for existing users.

---

## 4. New Field Group: Custom Header

### 4.1 Trigger

Shown when `authType === 'CUSTOM_HEADER'`

### 4.2 Field Layout

```
┌──────────────────────────────────────────────────────────────────┐
│  Auth Type                                                        │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │ Custom Header                                          ▾   │  │
│  └────────────────────────────────────────────────────────────┘  │
│  Flexible — for proprietary API auth.                            │
│                                                                   │
│  Header Name *                                                    │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │ X-Custom-Header                                           │  │
│  └────────────────────────────────────────────────────────────┘  │
│  ℹ️  The exact header name your API expects (case-sensitive).     │
│                                                                   │
│  Header Value *                                                   │
│  ┌─────────────────────────────────────────────────────── 👁 ┐  │
│  │ ••••••••••••••••••••••                                    │  │
│  └────────────────────────────────────────────────────────────┘  │
│  ℹ️  Stored encrypted. Never shown after saving.                  │
└──────────────────────────────────────────────────────────────────┘
```

### 4.3 Field Specifications

| Field | Component | Props |
|---|---|---|
| **Header Name** | `Input` (text) | `id="custom-header-name"`, `label="Header Name"`, default value `"X-Custom-Header"`, `required`, `placeholder="e.g. X-Service-Auth"` |
| **Header Value** | `PasswordInput` | `id="custom-header-value"`, `label="Header Value"`, `required`, `placeholder="Enter header value"`, `ariaLabel="Header Value"` |

### 4.4 Form State Shape

```typescript
// In WebhookAuthConfig
{
  authType: 'CUSTOM_HEADER',
  headerName: string,     // default 'X-Custom-Header', user-editable
  headerValue: string     // masked; empty string on component mount
}
```

### 4.5 Validation

| Rule | Error message |
|---|---|
| `headerName` empty | "Header name is required" |
| `headerValue` empty | "Header value is required" |

### 4.6 Distinction from API Key

**API Key** is for the specific pattern of sending a key in a header (the header name defaults to `X-API-Key` and the field is labelled "API Key"). **Custom Header** is for any header-based auth where the user controls both the name and value with no API Key semantics implied. The descriptions in the dropdown make this clear.

---

## 5. New Field Group: OAuth 2.0 (Client Credentials)

### 5.1 Trigger

Shown when `authType === 'OAUTH2_CLIENT_CREDENTIALS'`

### 5.2 Field Layout (Creation)

```
┌──────────────────────────────────────────────────────────────────┐
│  Auth Type                                                        │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │ OAuth 2.0 (Client Credentials)                         ▾   │  │
│  └────────────────────────────────────────────────────────────┘  │
│  Fetches a Bearer token automatically using client credentials.  │
│                                                                   │
│  Token URL *                                                      │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │ https://auth.example.com/oauth/token                      │  │
│  └────────────────────────────────────────────────────────────┘  │
│  ⚠️  Must use HTTPS.                                             │
│                                                                   │
│  Client ID *                                                      │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │                                                           │  │
│  └────────────────────────────────────────────────────────────┘  │
│                                                                   │
│  Client Secret *                                                  │
│  ┌─────────────────────────────────────────────────────── 👁 ┐  │
│  │ ••••••••••••••••••••••                                    │  │
│  └────────────────────────────────────────────────────────────┘  │
│  ℹ️  Stored encrypted. Never shown after saving.                  │
│                                                                   │
│  Scope  (optional)                                                │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │                                                           │  │
│  └────────────────────────────────────────────────────────────┘  │
│  ℹ️  Space-separated. e.g. "webhooks:write api:read"              │
└──────────────────────────────────────────────────────────────────┘
```

### 5.3 Field Specifications

| Field | Component | Props |
|---|---|---|
| **Token URL** | `Input` (url) | `id="oauth2-token-url"`, `label="Token URL"`, `required`, `type="url"`, `placeholder="https://auth.example.com/oauth/token"` |
| **Client ID** | `Input` (text) | `id="oauth2-client-id"`, `label="Client ID"`, `required`, `autoComplete="off"` |
| **Client Secret** | `PasswordInput` | `id="oauth2-client-secret"`, `label="Client Secret"`, `required`, `ariaLabel="Client Secret"` |
| **Scope** | `Input` (text) | `id="oauth2-scope"`, `label="Scope"`, `required={false}`, `placeholder='e.g. webhooks:write api:read'` |

### 5.4 Form State Shape

```typescript
{
  authType: 'OAUTH2_CLIENT_CREDENTIALS',
  tokenUrl: string,        // required; must start with https://
  clientId: string,        // required
  clientSecret: string,    // required; masked
  scope: string            // optional; space-separated
}
```

### 5.5 Validation (client-side)

| Rule | Error message | When shown |
|---|---|---|
| `tokenUrl` empty | "Token URL is required" | On blur / submit |
| `tokenUrl` does not start with `https://` | "Token URL must use HTTPS" | On change (live) + submit |
| `tokenUrl` is not a valid URL | "Enter a valid URL" | On blur / submit |
| `clientId` empty | "Client ID is required" | On blur / submit |
| `clientSecret` empty | "Client Secret is required" | On blur / submit |

**Live HTTPS validation:** Show an inline warning `⚠️ Must use HTTPS` below the Token URL field at all times when an `http://` value is detected. Do not wait for submit.

### 5.6 Help Text — Provider Hints

Below the Scope field, show a collapsed `<details>` hint:

```
▶ Where to find these values
  Salesforce:   Setup → Apps → Connected App → Consumer Key/Secret; Token URL: https://login.salesforce.com/services/oauth2/token
  Azure AD:     App registrations → Certificates & secrets; Token URL: https://login.microsoftonline.com/{tenant}/oauth2/v2.0/token
  Okta:         Applications → API services; Token URL: https://{domain}/oauth2/default/v1/token
  Google:       Service Account → Keys; Token URL: https://oauth2.googleapis.com/token
```

Use `<details>` / `<summary>` pattern in a `text-xs text-gray-500` style. Collapsed by default. This is purely static text — no links.

---

## 6. Settings Tab — Display & Edit (Existing Webhooks)

### 6.1 Custom Header — Settings Display

```
┌──────────────────────────────────────────────────────────────────┐
│  Authentication                                                   │
│  ─────────────────────────────────────────────────────────────   │
│  Auth Type       Custom Header                                    │
│  Header Name     X-Service-Auth                                   │
│  Header Value    [REDACTED]      [Rotate Value ↺]                 │
└──────────────────────────────────────────────────────────────────┘
```

**"Rotate Value" flow:** Clicking "Rotate Value ↺" replaces the `[REDACTED]` static text with a `PasswordInput` field + `[Save]` / `[Cancel]` inline. On Save, calls `PUT /v1/webhooks/{id}/auth-config` with only the new `headerValue`. On success, shows toast "Authentication updated", returns to display mode.

### 6.2 OAuth 2.0 — Settings Display

```
┌──────────────────────────────────────────────────────────────────┐
│  Authentication                                                   │
│  ─────────────────────────────────────────────────────────────   │
│  Auth Type       OAuth 2.0 (Client Credentials)                  │
│  Token URL       https://login.salesforce.com/services/oauth2/token
│  Client ID       3MVG9...                                        │
│  Client Secret   [REDACTED]      [Rotate Secret ↺]               │
│  Scope           webhooks:write api:read                          │
└──────────────────────────────────────────────────────────────────┘
```

**"Rotate Secret" flow:** Same inline pattern as "Rotate Value". Replace `[REDACTED]` with `PasswordInput` + `[Save]` / `[Cancel]`. On Save: `PUT /v1/webhooks/{id}/auth-config` with new `clientSecret`. On success: toast "Authentication updated", reset to display mode.

**Note:** Token URL, Client ID, and Scope are non-sensitive and shown in plain text. Only `clientSecret` uses the rotate pattern.

**Edit full config:** An `[Edit Auth Config]` button below the table opens the full edit form (all fields editable) in an inline section or modal, following the same pattern as other Settings tab edit flows.

### 6.3 VIEWER Role Display

Regardless of auth type, VIEWER sees:

```
Auth Type    OAuth 2.0 (Client Credentials)
             [credentials hidden — contact an Owner or Editor to view]
```

No credential fields, no rotate buttons. The auth type label is always visible.

---

## 7. TypeScript Type Updates

```typescript
// webhooks.api.ts — extend WebhookAuthType
export type WebhookAuthType =
  | 'NONE'
  | 'SIGNING_SECRET'
  | 'API_KEY'
  | 'BASIC_AUTH'
  | 'BEARER_TOKEN'
  | 'CUSTOM_HEADER'                    // NEW
  | 'OAUTH2_CLIENT_CREDENTIALS';       // NEW

// WebhookAuthConfig — add new fields (all optional at type level; validated at form level)
export interface WebhookAuthConfig {
  // existing fields...
  headerName?: string;
  apiKey?: string;
  headerValue?: string;                 // NEW — Custom Header
  username?: string;
  password?: string;
  token?: string;
  secret?: string;
  // new OAuth2 fields:
  tokenUrl?: string;                    // NEW
  clientId?: string;                    // NEW
  clientSecret?: string;               // NEW
  scope?: string;                       // NEW
}
```

---

## 8. Accessibility

| Rule | Implementation |
|---|---|
| All new fields have `htmlFor` + `id` linkage | Same as existing fields |
| Password fields use `autoComplete="new-password"` | Prevents browser autofill leaking credentials |
| HTTPS validation error has `role="alert"` | Screen reader announces live validation |
| Provider hints `<details>` is keyboard navigable | Native HTML behaviour |
| "Rotate Secret" button has descriptive `aria-label` | `aria-label="Rotate OAuth 2.0 Client Secret"` |

---

## 9. Error States

| Scenario | UI Response |
|---|---|
| API returns 400 (tokenUrl SSRF blocked) | Toast: "Token URL is not allowed — it resolves to a restricted address." |
| API returns 400 (tokenUrl not HTTPS) | Toast: "Token URL must use HTTPS." |
| Save fails (network error) | Toast: "Failed to update webhook. Please try again." |
| VIEWER tries to POST via API | Frontend gate prevents form submission before API call |

---

## 10. No-Change Scope

These components are **not modified** by this epic:

- `WebhookListPage.tsx` — no new columns needed for the two new auth types
- `WebhookMetricsTab.tsx` — unchanged (Epic 5)
- `WebhookHealthBadge.tsx` — unchanged
- `WebhookDeliveryLogsTab.tsx` — the new `AUTH_FAILURE` status will appear automatically if the delivery log renders status as a badge; no code change needed if status is rendered from string

---

**UX Designer Sign-Off:** BMAD UX Designer Agent — 2026-03-19
