# Story 026 — Frontend: OAuth 2.0 & Custom Header Form Fields

**Story ID:** story-026
**Epic:** Webhook Auth v2 — OAuth 2.0 & Custom Header (Epic 6)
**Prepared by:** Tech Lead
**Date:** 2026-03-19
**Assigned to:** Frontend Engineer
**Priority:** P1
**Estimate:** 2 story points
**Depends on:** Story 025 (API contract finalised — TypeScript types must match backend)

---

## User Story

**As an** organization owner or editor,
**I want** the webhook creation and settings forms to show the correct fields for Custom Header and OAuth 2.0 auth types,
**So that** I can configure these auth types through the UI without manually editing JSON or API calls.

---

## Context

`WebhookAuthConfigForm.tsx` currently renders 5 auth types (NONE, SIGNING_SECRET, API_KEY, BASIC_AUTH, BEARER_TOKEN). The `AUTH_TYPE_OPTIONS` array and the conditional field rendering must be extended for two new types.

`WebhookSettingsTab.tsx` currently shows auth config in display mode for existing types. It must be extended to display `tokenUrl`, `clientId`, `scope` (non-sensitive) and offer an inline "Rotate Secret" / "Rotate Value" control for the masked fields.

**Design spec:** `docs/ux/ui-spec-webhooks-oauth2-customheader.md` — follow exactly.

**Important:** `CUSTOM_HEADER`'s `headerValue` field is a distinct field from `API_KEY`'s `apiKey` field — both have a `headerName`, but the value field names differ. Ensure the form state shape sends the correct field name.

---

## Acceptance Criteria

1. Auth type dropdown includes `CUSTOM_HEADER` ("Custom Header") and `OAUTH2_CLIENT_CREDENTIALS` ("OAuth 2.0 (Client Credentials)") as options with descriptions.
2. Selecting "Custom Header" shows: `headerName` (text, default `"X-Custom-Header"`) + `headerValue` (PasswordInput, required).
3. Selecting "OAuth 2.0 (Client Credentials)" shows: `tokenUrl` (URL input, required) + `clientId` (text, required) + `clientSecret` (PasswordInput, required) + `scope` (text, optional).
4. Live validation: if `tokenUrl` starts with `http://` (not https), inline error "Token URL must use HTTPS" appears immediately on change.
5. `tokenUrl` field shows `⚠️ Must use HTTPS` hint below the input at all times when not HTTPS.
6. A collapsible provider hints section ("▶ Where to find these values") is shown below the scope field for OAuth 2.0, listing Salesforce/Azure AD/Okta/Google token URL patterns. Collapsed by default.
7. Form submission with `CUSTOM_HEADER` sends `{ authType: "CUSTOM_HEADER", headerName, headerValue }` in the API call body.
8. Form submission with `OAUTH2_CLIENT_CREDENTIALS` sends `{ authType: "OAUTH2_CLIENT_CREDENTIALS", tokenUrl, clientId, clientSecret, scope }`.
9. Switching auth type clears the previous type's fields (same behaviour as existing types).
10. `WebhookSettingsTab` shows `tokenUrl` + `clientId` + `scope` in plain text for OAuth 2.0 webhooks; `clientSecret` shows `"[REDACTED]"` with a `[Rotate Secret ↺]` button.
11. `WebhookSettingsTab` shows `headerName` in plain text for Custom Header webhooks; `headerValue` shows `"[REDACTED]"` with a `[Rotate Value ↺]` button.
12. "Rotate Secret ↺" / "Rotate Value ↺": clicking replaces the `[REDACTED]` text with an inline `PasswordInput` + `[Save]` / `[Cancel]`. Save calls `PUT` auth-config endpoint with the new value. On success: toast "Authentication updated", return to display mode.
13. VIEWER role: auth type label shown, no credential fields, no rotate buttons.
14. `npm run build` 0 TypeScript errors.
15. `npm test` — new tests for `WebhookAuthConfigForm` OAuth2 and Custom Header renders pass.

---

## Files to Modify

### `frontend/src/features/webhooks/components/WebhookAuthConfigForm.tsx`

**1. Update `AUTH_TYPE_OPTIONS`** — append two entries:

```typescript
const AUTH_TYPE_OPTIONS: Array<{ value: WebhookAuthType; label: string; description: string }> = [
  { value: 'NONE', label: 'None', description: 'No authentication. Use for public or VPN-protected endpoints.' },
  { value: 'SIGNING_SECRET', label: 'Signing Secret', description: 'RuleForge signs each request with HMAC-SHA256.' },
  { value: 'API_KEY', label: 'API Key', description: 'Sends a key in a custom header (e.g., X-API-Key).' },
  { value: 'BASIC_AUTH', label: 'Basic Auth', description: 'Sends username and password via Authorization: Basic.' },
  { value: 'BEARER_TOKEN', label: 'Bearer Token', description: 'Sends a static token via Authorization: Bearer.' },
  // NEW:
  { value: 'CUSTOM_HEADER', label: 'Custom Header', description: 'Sends any value in a header you specify. Flexible — for proprietary API auth.' },
  { value: 'OAUTH2_CLIENT_CREDENTIALS', label: 'OAuth 2.0 (Client Credentials)', description: 'Fetches a Bearer token automatically using client credentials. For Salesforce, Azure AD, Okta, and similar.' },
];
```

**2. Add Custom Header field group:**

```tsx
{authType === 'CUSTOM_HEADER' && (
  <>
    <Input
      id="custom-header-name"
      label="Header Name"
      value={authConfig.headerName ?? 'X-Custom-Header'}
      onChange={(e) => update({ headerName: e.target.value })}
      placeholder="e.g. X-Service-Auth"
      disabled={disabled}
    />
    <p className="mt-1 text-xs text-gray-500">
      The exact header name your API expects (case-sensitive).
    </p>
    <div>
      <label htmlFor="custom-header-value" className="mb-1.5 block text-sm font-medium text-gray-700">
        Header Value *
      </label>
      <PasswordInput
        id="custom-header-value"
        value={authConfig.headerValue ?? ''}
        onChange={(v) => update({ headerValue: v })}
        placeholder="Enter header value"
        disabled={disabled}
        ariaLabel="Header Value"
      />
      <p className="mt-1 text-xs text-gray-500">Stored encrypted. Never shown after saving.</p>
    </div>
  </>
)}
```

**3. Add OAuth 2.0 field group:**

```tsx
{authType === 'OAUTH2_CLIENT_CREDENTIALS' && (
  <>
    <div>
      <Input
        id="oauth2-token-url"
        label="Token URL"
        type="url"
        value={authConfig.tokenUrl ?? ''}
        onChange={(e) => update({ tokenUrl: e.target.value })}
        placeholder="https://auth.example.com/oauth/token"
        disabled={disabled}
      />
      {authConfig.tokenUrl && !authConfig.tokenUrl.startsWith('https://') && (
        <p className="mt-1 text-xs text-amber-600" role="alert">⚠️ Must use HTTPS.</p>
      )}
    </div>
    <Input
      id="oauth2-client-id"
      label="Client ID"
      value={authConfig.clientId ?? ''}
      onChange={(e) => update({ clientId: e.target.value })}
      disabled={disabled}
      autoComplete="off"
    />
    <div>
      <label htmlFor="oauth2-client-secret" className="mb-1.5 block text-sm font-medium text-gray-700">
        Client Secret *
      </label>
      <PasswordInput
        id="oauth2-client-secret"
        value={authConfig.clientSecret ?? ''}
        onChange={(v) => update({ clientSecret: v })}
        placeholder="Enter client secret"
        disabled={disabled}
        ariaLabel="Client Secret"
      />
      <p className="mt-1 text-xs text-gray-500">Stored encrypted. Never shown after saving.</p>
    </div>
    <Input
      id="oauth2-scope"
      label="Scope"
      value={authConfig.scope ?? ''}
      onChange={(e) => update({ scope: e.target.value })}
      placeholder='e.g. webhooks:write api:read'
      disabled={disabled}
    />
    <details className="mt-1">
      <summary className="cursor-pointer text-xs text-gray-500 hover:text-gray-700">
        ▶ Where to find these values
      </summary>
      <div className="mt-2 rounded-md bg-gray-50 p-3 text-xs text-gray-500 space-y-1">
        <p><strong>Salesforce:</strong> Setup → Apps → Connected App → Consumer Key/Secret; Token URL: https://login.salesforce.com/services/oauth2/token</p>
        <p><strong>Azure AD:</strong> App registrations → Certificates &amp; secrets; Token URL: https://login.microsoftonline.com/&#123;tenant&#125;/oauth2/v2.0/token</p>
        <p><strong>Okta:</strong> Applications → API services; Token URL: https://&#123;domain&#125;/oauth2/default/v1/token</p>
        <p><strong>Google:</strong> Service Account → Keys; Token URL: https://oauth2.googleapis.com/token</p>
      </div>
    </details>
  </>
)}
```

### `frontend/src/api/webhooks.api.ts`

**Update `WebhookAuthType`:**

```typescript
export type WebhookAuthType =
  | 'NONE'
  | 'SIGNING_SECRET'
  | 'API_KEY'
  | 'BASIC_AUTH'
  | 'BEARER_TOKEN'
  | 'CUSTOM_HEADER'                   // NEW
  | 'OAUTH2_CLIENT_CREDENTIALS';      // NEW
```

**Update `WebhookAuthConfig` interface:**

```typescript
export interface WebhookAuthConfig {
  headerName?: string;
  apiKey?: string;
  headerValue?: string;               // NEW — Custom Header value
  username?: string;
  password?: string;
  token?: string;
  secret?: string;
  tokenUrl?: string;                  // NEW — OAuth2
  clientId?: string;                  // NEW — OAuth2
  clientSecret?: string;              // NEW — OAuth2
  scope?: string;                     // NEW — OAuth2
}
```

### `frontend/src/features/webhooks/components/WebhookSettingsTab.tsx`

Add display rows for the new auth types. Locate the existing auth config display section and extend it:

```tsx
// Custom Header display
{webhook.authType === 'CUSTOM_HEADER' && (
  <>
    <SettingsRow label="Header Name" value={webhook.authConfig?.headerName ?? '—'} />
    <SettingsRow
      label="Header Value"
      value="[REDACTED]"
      action={canEdit ? <RotateCredentialButton
        label="Rotate Value"
        webhookId={webhook.id}
        field="headerValue"
        onSuccess={() => toast.success('Authentication updated')}
      /> : undefined}
    />
  </>
)}

// OAuth 2.0 display
{webhook.authType === 'OAUTH2_CLIENT_CREDENTIALS' && (
  <>
    <SettingsRow label="Token URL" value={webhook.authConfig?.tokenUrl ?? '—'} />
    <SettingsRow label="Client ID" value={webhook.authConfig?.clientId ?? '—'} />
    <SettingsRow
      label="Client Secret"
      value="[REDACTED]"
      action={canEdit ? <RotateCredentialButton
        label="Rotate Secret"
        webhookId={webhook.id}
        field="clientSecret"
        onSuccess={() => toast.success('Authentication updated')}
      /> : undefined}
    />
    {webhook.authConfig?.scope && (
      <SettingsRow label="Scope" value={webhook.authConfig.scope} />
    )}
  </>
)}
```

**`RotateCredentialButton` inline pattern** (small inline component or part of SettingsTab):

```tsx
// Renders: [REDACTED]  [Rotate Value ↺]
// On click: shows PasswordInput + [Save] [Cancel]
// On Save: PUT /v1/webhooks/{id}/auth-config with { authType, [field]: newValue }
// On success: toast + reset to display
```

---

## Files to Create (Tests)

### `frontend/src/features/webhooks/components/WebhookAuthConfigForm.test.tsx`

Add to existing or create new test file:

```typescript
describe('WebhookAuthConfigForm — Custom Header', () => {
  it('renders headerName and headerValue fields when CUSTOM_HEADER selected')
  it('defaults headerName to X-Custom-Header')
  it('calls onChange with headerName and headerValue in authConfig')
})

describe('WebhookAuthConfigForm — OAuth 2.0', () => {
  it('renders tokenUrl, clientId, clientSecret, scope fields when OAUTH2 selected')
  it('shows HTTPS warning when tokenUrl starts with http://')
  it('does not show HTTPS warning for valid https:// tokenUrl')
  it('renders provider hints details element (collapsed)')
  it('calls onChange with all OAuth2 fields')
})
```

---

## Definition of Done

- [ ] `WebhookAuthType` and `WebhookAuthConfig` TypeScript types updated
- [ ] `AUTH_TYPE_OPTIONS` includes CUSTOM_HEADER and OAUTH2_CLIENT_CREDENTIALS with descriptions
- [ ] Custom Header field group (headerName + headerValue) renders and submits correctly
- [ ] OAuth 2.0 field group (tokenUrl + clientId + clientSecret + scope) renders and submits correctly
- [ ] Live HTTPS validation on tokenUrl — amber warning on `http://`
- [ ] Provider hints `<details>` section renders, collapsed by default
- [ ] `WebhookSettingsTab` shows correct display for both new auth types
- [ ] "Rotate Secret ↺" / "Rotate Value ↺" inline edit pattern implemented
- [ ] VIEWER sees auth type label only — no credentials, no buttons
- [ ] All new Vitest tests pass
- [ ] `npm run build` 0 TypeScript errors
- [ ] `npm run lint` clean
- [ ] Existing auth types (API_KEY, BASIC_AUTH, BEARER_TOKEN, etc.) regression-free
