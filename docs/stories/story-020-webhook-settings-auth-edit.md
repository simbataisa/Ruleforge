# Story 020 — Frontend: WebhookSettingsTab Auth Config Display + Edit

**Story ID:** story-020
**Epic:** Webhooks Authentication Enhancement (Epic 5)
**Prepared by:** Tech Lead
**Date:** 2026-03-18
**Assigned to:** Frontend Engineer
**Priority:** P0 — Sprint 5.1
**Estimate:** 2 story points
**Depends on:** Story 017 (backend returns authType + masked authConfig), Story 019 (WebhookAuthConfigForm reused here)

---

## User Story

**As an** org owner,
**I want** to see the current auth type on my webhook's Settings tab and be able to update it,
**So that** I can change authentication config without recreating the webhook.

---

## Background

The existing `WebhookSettingsTab` shows webhook name, URL, events, and scope. This story adds an "Authentication" card section that displays the current auth type + masked credentials (read mode) and allows OWNER to switch to edit mode and update the config.

---

## Acceptance Criteria

1. `WebhookSettingsTab` renders an "Authentication" card section showing: auth type label + relevant non-sensitive fields (e.g., header name for API_KEY; username for BASIC_AUTH) + masked credential value (`••••••••••••`).
2. OWNER sees an `[Edit]` button on the auth card; EDITOR and VIEWER do not.
3. Clicking `[Edit]` switches the card to inline edit mode — renders `WebhookAuthConfigForm` (reused from Story 019) pre-populated with current `authType` and non-sensitive fields; credential fields are blank (user must re-enter).
4. Credential field placeholder: `"Leave blank to keep current value"`.
5. Clicking `[Save]` calls `PUT /v1/webhooks/{id}` (or `PATCH`) with `authType` and `authConfig`; if credential fields are blank, does NOT send them (keeps existing encrypted value server-side).
6. On save success: card returns to read mode, toast "Webhook updated", query cache invalidated.
7. Clicking `[Cancel]` returns to read mode without API call.
8. Retry config fields (Initial Delay, Backoff Multiplier, Max Attempts) are shown in the existing Settings section as editable fields for OWNER.
9. VIEWER: sees "Authentication" card with auth type label only; credential row shows `[Restricted]`; no Edit button.
10. `npm run build` 0 TypeScript errors.

---

## Files to Modify

### `frontend/src/features/webhooks/components/WebhookSettingsTab.tsx`

**State additions:**
```typescript
const [isEditingAuth, setIsEditingAuth] = useState(false)
const [editAuthType, setEditAuthType] = useState<WebhookAuthType>(webhook.authType ?? 'NONE')
const [editAuthConfig, setEditAuthConfig] = useState<Partial<WebhookAuthConfig>>({})
```

**Auth config read view (add to JSX):**
```tsx
<Card>
  <CardHeader className="flex flex-row items-center justify-between pb-2">
    <CardTitle className="text-sm font-medium">Authentication</CardTitle>
    {canOwner && !isEditingAuth && (
      <Button variant="ghost" size="sm" onClick={() => setIsEditingAuth(true)}>
        Edit
      </Button>
    )}
  </CardHeader>
  <CardContent>
    {!isEditingAuth ? (
      <AuthConfigReadView webhook={webhook} isViewer={isViewer} />
    ) : (
      <AuthConfigEditView
        authType={editAuthType}
        authConfig={editAuthConfig}
        onChange={(type, config) => { setEditAuthType(type); setEditAuthConfig(config) }}
        onSave={handleSaveAuth}
        onCancel={() => { setIsEditingAuth(false); setEditAuthConfig({}) }}
        isSaving={isUpdating}
      />
    )}
  </CardContent>
</Card>
```

**`AuthConfigReadView` (local sub-component):**

| Auth Type | Fields shown |
|---|---|
| NONE | "None — No authentication" |
| SIGNING_SECRET | Auth Type: Signing Secret · Secret: `••••••••••••` |
| API_KEY | Auth Type: API Key · Header: `{headerName}` · Key: `••••••••••••` |
| BASIC_AUTH | Auth Type: Basic Auth · Username: `{username}` · Password: `••••••••••••` |
| BEARER_TOKEN | Auth Type: Bearer Token · Token: `••••••••••••` |

For VIEWER: replace `••••••••••••` with `[Restricted]`.

**`handleSaveAuth`:**
```typescript
async function handleSaveAuth() {
  const payload: UpdateWebhookRequest = {
    authType: editAuthType,
    authConfig: buildAuthConfigPayload(editAuthType, editAuthConfig),
    // only include credential fields if user entered a value
  }
  await updateWebhook({ id: webhook.id, ...payload })
  setIsEditingAuth(false)
  setEditAuthConfig({})
}

function buildAuthConfigPayload(type: WebhookAuthType, config: Partial<WebhookAuthConfig>) {
  // Only include non-blank credential fields in the payload
  // This preserves existing encrypted credentials for untouched fields
  const result: Partial<WebhookAuthConfig> = {}
  if (type === 'API_KEY') {
    if (config.headerName) result.headerName = config.headerName
    if (config.apiKey?.trim()) result.apiKey = config.apiKey
  }
  // ... similar for other types
  return Object.keys(result).length > 0 ? result : undefined
}
```

**Retry config fields** (in existing settings section):
Add `initialDelaySeconds`, `backoffMultiplier`, `maxAttempts` as editable number inputs with the same save/cancel pattern. Only visible to OWNER.

---

## Definition of Done

- [ ] Auth config card section added to `WebhookSettingsTab`
- [ ] Read mode: auth type + masked credentials
- [ ] Edit mode: `WebhookAuthConfigForm` reused (not reimplemented)
- [ ] Save sends only non-blank credential fields
- [ ] OWNER sees Edit button; VIEWER sees `[Restricted]`
- [ ] Retry config fields editable for OWNER in settings
- [ ] Query cache invalidated on save (`['webhooks', id]`)
- [ ] Toast on success; cancel returns to read mode
- [ ] `tsc --noEmit` 0 errors; `npm run lint` passes
- [ ] Existing `WebhookSettingsTab` form save still works (no regression)
