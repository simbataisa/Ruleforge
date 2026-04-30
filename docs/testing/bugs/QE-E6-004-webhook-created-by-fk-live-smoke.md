# Defect QE-E6-004 - Webhook Creation Fails Live Smoke Due To `created_by` Foreign Key

**Date:** 2026-04-30  
**Reporter:** Tester & QE  
**Severity:** P1  
**Status:** Resolved  
**Area:** Event service / Webhook API / live stack smoke  
**Environment:** Local live stack via `./scripts/start-local.sh`; smoke run executed with Docker and localhost access outside sandbox.

## Summary

Live smoke sections 3.20, 3.21, and 3.22 fail because webhook create requests cannot persist rows in `webhooks`.

The event-service attempts to insert `webhooks.created_by` with the JWT user id, but the database foreign key `webhooks_created_by_fkey` expects a key present in `members`. Seed users exist and authentication works, but user ids such as `b0000000-0000-0000-0000-000000000001` are not valid `members.id` values.

## Reproduction

1. Start the local RuleForge stack.
2. Run:

   ```bash
   ./scripts/smoke-test.sh
   ```

3. Observe that infrastructure, services, auth, rule, scheduler, workflow, analytics, audit, and spaces checks pass.
4. Observe webhook create tests fail:

   - `3.20.1 POST /v1/webhooks (scopeType=SPACE)`
   - `3.20.3 POST /v1/webhooks (scopeType=ORG) as OWNER`
   - `3.21.1 POST /v1/webhooks (authType=API_KEY)`
   - `3.21.7 POST /v1/webhooks (authType=BASIC_AUTH)`
   - `3.22.1 POST /v1/webhooks (authType=CUSTOM_HEADER)`
   - `3.22.3 POST /v1/webhooks (authType=OAUTH2_CLIENT_CREDENTIALS)`

## Actual Result

Smoke result:

```text
PASS: 103  FAIL: 7  SKIP: 10  Total: 120
```

Event-service log excerpt:

```text
ERROR: insert or update on table "webhooks" violates foreign key constraint "webhooks_created_by_fkey"
Detail: Key (created_by)=(b0000000-0000-0000-0000-000000000001) is not present in table "members".
```

## Expected Result

Webhook create requests from valid OWNER / EDITOR users should persist successfully where RBAC allows the operation.

Expected smoke behavior:

- SPACE-scoped webhook create as OWNER returns 201.
- ORG-scoped webhook create as OWNER returns 201.
- ORG-scoped webhook create as EDITOR returns 403, not a persistence failure.
- Custom header and OAuth2 webhook create paths pass and unlock follow-up redaction checks.

## Impact

This blocks live-stack release smoke for Epic 6 Phase 5 webhook auth functionality.

The lower-layer unit/service/controller tests and security scans pass, but the live runtime path is not release-clean until this is fixed or the schema/application contract is explicitly changed.

## Root-Cause Hypothesis

Likely mismatch between one of these:

- `webhooks.created_by` schema constraint references `members(id)`.
- `WebhookService` / controller stores authenticated `userId` in `created_by`.

The fix should align schema and application semantics:

- If `created_by` means user id, update the FK to reference `users(id)`.
- If `created_by` means member id, resolve the current member id for `(orgId, userId)` before saving.

## Verification Required After Fix

Run:

```bash
./scripts/smoke-test.sh
```

Acceptance target:

- No failures in sections 3.20, 3.21, and 3.22.
- Overall smoke result has 0 failed checks, allowing known optional skips only.

## Resolution

Implemented on 2026-04-30:

- Event-service now resolves `webhooks.created_by` from `(org_id, user_id)` to `members.id`.
- Webhook create now enforces scope RBAC: OWNER/ADMIN for ORG scope, EDITOR+ for create generally, and SPACE scope must belong to the authenticated org.
- Webhook test delivery response bodies are truncated before persistence to fit the live schema.
- Authenticated authorization denials from webhook APIs now return HTTP 403.
- Smoke assertions were updated to the current DTO contract (`secretOnCreation`, `successDeliveries`, `deliveryTimeline`, redacted credential placeholders).

Verified:

```text
./gradlew :ruleforge-event-service:test
BUILD SUCCESSFUL

./gradlew :ruleforge-event-service:bootJar
BUILD SUCCESSFUL

./scripts/smoke-test.sh
PASS: 128  FAIL: 0  SKIP: 1  Total: 129
```
