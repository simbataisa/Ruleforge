# Story 019 — Frontend: WebhookAuthConfigForm + CreateWebhookModal Integration

**Story ID:** story-019
**Epic:** Webhooks Authentication Enhancement (Epic 5)
**Prepared by:** Tech Lead
**Date:** 2026-03-18
**Assigned to:** Frontend Engineer
**Priority:** P0 — Sprint 5.1
**Estimate:** 3 story points
**Depends on:** Story 017 (backend API contract must accept authType + authConfig)

---

## User Story

**As an** org owner or editor,
**I want** the webhook creation form to have an auth type selector with type-specific credential fields,
**So that** I can configure authentication when creating a webhook to a secured API.

---

## Background

`CreateWebhookModal.tsx` currently has fields for name, URL, events, and scope. It needs a new "Authentication" section and an "Advanced" collapsible for retry config. The auth section uses progressive disclosure: selecting an auth type reveals type-specific credential fields inline, and switching type clears previously entered credentials.

See UI Spec S-01 for full layout and interaction details.

---

## Acceptance Criteria

1. `WebhookAuthConfigForm` component renders an auth type `<Select>` with options: None, Signing Secret, API Key, Basic Auth, Bearer Token.
2. Selecting an auth type reveals type-specific fields (per UI Spec §3.4); switching type clears credential fields.
3. Password / token / secret fields are `type="password"` by default with a show/hide toggle (`EyeOff` / `Eye` icon).
4. API Key's "Header Name" field pre-fills with `"X-API-Key"` and is editable.
5. An informational hint text appears below the credential fields explaining the injected header format.
6. `WebhookRetryConfigForm` (collapsible "Advanced" section) renders Initial Delay, Backoff Multiplier, Max Attempts, and Retry Status Codes with correct defaults (30, 2.0, 5, [429,408,500,502,503,504]).
7. `CreateWebhookModal` includes `WebhookAuthConfigForm` and `WebhookRetryConfigForm` in its form.
8. On submit, `POST /v1/webhooks` request body includes `authType`, `authConfig`, and (if Advanced opened and changed) `retryConfig`.
9. Validation prevents submit when required credential fields are blank for the selected auth type.
10. VIEWER role: the modal is not accessible (existing RBAC check on "Create Webhook" button applies).
11. `npm run build` produces 0 TypeScript errors.
12. Vitest unit tests cover: auth type switching clears fields, show/hide toggle, required field validation.

---

## Files to Create

### `frontend/src/features/webhooks/components/WebhookAuthConfigForm.tsx`

```typescript
interface WebhookAuthConfigFormProps {
  authType: WebhookAuthType
  authConfig: Partial<WebhookAuthConfig>
  onChange: (authType: WebhookAuthType, authConfig: Partial<WebhookAuthConfig>) => void
  disabled?: boolean
}

export function WebhookAuthConfigForm({
  authType, authConfig, onChange, disabled
}: WebhookAuthConfigFormProps): JSX.Element
```

**Implementation requirements:**

1. Auth type `<Select>` with options mapped from `AUTH_TYPE_OPTIONS` constant:
   ```typescript
   const AUTH_TYPE_OPTIONS = [
     { value: 'NONE', label: 'None', description: 'No authentication.' },
     { value: 'SIGNING_SECRET', label: 'Signing Secret', description: 'HMAC-SHA256 signature.' },
     { value: 'API_KEY', label: 'API Key', description: 'Custom header key.' },
     { value: 'BASIC_AUTH', label: 'Basic Auth', description: 'Username and password.' },
     { value: 'BEARER_TOKEN', label: 'Bearer Token', description: 'Static bearer token.' },
   ] as const
   ```

2. On auth type change: call `onChange(newType, {})` — empty authConfig clears all credential state in parent.

3. Conditional field rendering via `switch (authType)`:
   - `API_KEY`: `headerName` (Input, default "X-API-Key") + `apiKey` (PasswordInput)
   - `BASIC_AUTH`: `username` (Input) + `password` (PasswordInput)
   - `BEARER_TOKEN`: `token` (PasswordInput)
   - `SIGNING_SECRET`: `secret` (PasswordInput + Regenerate button)
   - `NONE`: hint text only

4. `PasswordInput` sub-component (local, not exported):
   ```typescript
   function PasswordInput({ value, onChange, ...props }) {
     const [visible, setVisible] = useState(false)
     // ... Input with type={visible ? 'text' : 'password'} + Eye/EyeOff toggle
   }
   ```

5. Hint text per type shown as `<p className="text-xs text-muted-foreground mt-1">`:
   - API_KEY: `Sent as: {headerName || 'X-API-Key'}: [key]`
   - BASIC_AUTH: `Sent as: Authorization: Basic [base64]`
   - BEARER_TOKEN: `Sent as: Authorization: Bearer [token]`
   - SIGNING_SECRET: `Adds X-RuleForge-Signature header to each request.`
   - NONE: `No authentication header will be added.`

### `frontend/src/features/webhooks/components/WebhookRetryConfigForm.tsx`

```typescript
interface WebhookRetryConfigFormProps {
  retryConfig: Partial<WebhookRetryConfig>
  onChange: (retryConfig: Partial<WebhookRetryConfig>) => void
  disabled?: boolean
}

export function WebhookRetryConfigForm({
  retryConfig, onChange, disabled
}: WebhookRetryConfigFormProps): JSX.Element
```

**Implementation:**
- Uses shadcn `<Collapsible>` with trigger "Advanced ▶" / "Advanced ▼"
- Collapsed by default
- Fields: `initialDelaySeconds` (number input, 1–3600), `backoffMultiplier` (number, 1.0–10.0, step 0.1), `maxAttempts` (number, 1–25)
- `retryOnStatusCodes`: rendered as a tag-like input; default codes shown as removable `<Badge>` chips + "Add" input

---

## Files to Modify

### `frontend/src/features/webhooks/components/CreateWebhookModal.tsx`

1. Add state:
   ```typescript
   const [authType, setAuthType] = useState<WebhookAuthType>('NONE')
   const [authConfig, setAuthConfig] = useState<Partial<WebhookAuthConfig>>({})
   const [retryConfig, setRetryConfig] = useState<Partial<WebhookRetryConfig>>({})
   ```

2. Include `<WebhookAuthConfigForm>` between Events and Submit sections.

3. Include `<WebhookRetryConfigForm>` below auth section.

4. In submit handler, include in request body:
   ```typescript
   authType,
   authConfig: authType === 'NONE' ? undefined : authConfig,
   retryConfig: Object.keys(retryConfig).length > 0 ? retryConfig : undefined,
   ```

5. Validation before submit:
   ```typescript
   function validateAuth(): string | null {
     if (authType === 'API_KEY' && !authConfig.apiKey?.trim()) return 'API Key is required'
     if (authType === 'BASIC_AUTH' && !authConfig.username?.trim()) return 'Username is required'
     if (authType === 'BASIC_AUTH' && !authConfig.password?.trim()) return 'Password is required'
     if (authType === 'BEARER_TOKEN' && !authConfig.token?.trim()) return 'Token is required'
     return null
   }
   ```

---

## Types to Add

### `frontend/src/api/webhooks.api.ts`

```typescript
export type WebhookAuthType =
  | 'NONE' | 'SIGNING_SECRET' | 'API_KEY' | 'BASIC_AUTH' | 'BEARER_TOKEN' | 'CUSTOM_HEADER'

export interface WebhookAuthConfig {
  headerName?: string
  apiKey?: string
  username?: string
  password?: string
  token?: string
  secret?: string
  headerValue?: string
}

export interface WebhookRetryConfig {
  initialDelaySeconds?: number
  backoffMultiplier?: number
  maxAttempts?: number
  retryOnStatusCodes?: number[]
}
```

Update `CreateWebhookRequest` interface to include `authType`, `authConfig`, `retryConfig`.
Update `Webhook` response interface to include `authType`, `authConfig` (values may be `"[REDACTED]"`), `retryConfig`.

---

## Definition of Done

- [ ] `WebhookAuthConfigForm.tsx` — auth type selector + conditional credential fields
- [ ] `WebhookRetryConfigForm.tsx` — collapsible advanced section
- [ ] `CreateWebhookModal` includes both new forms
- [ ] Auth type switching clears credential fields
- [ ] Password/token fields masked with show/hide toggle
- [ ] Required field validation before submit
- [ ] TypeScript types added to `webhooks.api.ts`
- [ ] `npm run lint` passes; `tsc --noEmit` 0 errors
- [ ] Vitest tests: auth type switching, show/hide, validation errors
- [ ] `npm run build` 0 errors

---

## Notes for Implementer

- `WebhookAuthConfigForm` and `WebhookRetryConfigForm` are **controlled components** — state lives in `CreateWebhookModal`. This matches the existing form pattern in the codebase.
- Do NOT use `react-hook-form` for these new components unless the existing modal already uses it — check the current `CreateWebhookModal` implementation first and match the pattern.
- The `PasswordInput` sub-component should auto-hide after 30 seconds (set a `setTimeout` in the `visible=true` state that calls `setVisible(false)`) — this is a UX spec requirement.
- Max file length is 300 lines per team convention — if `CreateWebhookModal` approaches 300 lines after this change, extract the auth section into `WebhookAuthConfigForm` more aggressively.
