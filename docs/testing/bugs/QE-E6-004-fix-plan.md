# Fix Plan - QE-E6-004 Webhook `created_by` Foreign Key Failure

**Date:** 2026-04-30  
**Owner:** Tech Lead  
**Assigned to:** Backend Engineer  
**Severity:** P1  
**Scope:** Event service live smoke release blocker

## Problem

Live smoke sections 3.20, 3.21, and 3.22 fail when creating webhooks. The event-service saves JWT `userId` into `webhooks.created_by`, but `db/migrations/V012__create_webhooks.sql` defines `created_by UUID NOT NULL REFERENCES members(id)`.

## Decision

Preserve the schema contract. `created_by` means `members.id`, matching other tenant-scoped tables such as `rules`, `rule_versions`, `schedules`, `api_keys`, and `connectors`.

Event-service must resolve the current membership id for `(orgId, userId)` and save that member id in `webhooks.created_by`.

## Implementation Tasks

1. Add a read path in event-service to resolve `members.id` by `org_id` and `user_id`.
2. Update `WebhookService.createWebhook` to store the resolved member id.
3. Return 403 if the authenticated user has no membership in the target org.
4. Add unit coverage proving `createdBy` receives the member id, not the user id.
5. Rerun event-service tests.
6. Restart event-service and rerun `./scripts/smoke-test.sh`.

## Acceptance Criteria

- `./gradlew :ruleforge-event-service:test` passes.
- `./scripts/smoke-test.sh` no longer fails sections 3.20, 3.21, or 3.22 due to `webhooks_created_by_fkey`.
- Overall live smoke has 0 failed checks, allowing only documented optional skips.

## Closeout

**Status:** Complete  
**Date:** 2026-04-30

Implemented:

- Added membership lookup for `webhooks.created_by`.
- Added space ownership lookup for SPACE-scope create authorization.
- Enforced webhook create RBAC for OWNER/ADMIN ORG scope and EDITOR+ create scope.
- Mapped webhook service authorization denials to HTTP 403.
- Truncated stored webhook delivery response bodies to avoid live schema persistence overflow.
- Updated smoke expectations to current webhook DTO fields and redaction semantics.

Verified:

```text
./gradlew :ruleforge-event-service:test
BUILD SUCCESSFUL

./gradlew :ruleforge-event-service:bootJar
BUILD SUCCESSFUL

./scripts/smoke-test.sh
PASS: 128  FAIL: 0  SKIP: 1  Total: 129
```
