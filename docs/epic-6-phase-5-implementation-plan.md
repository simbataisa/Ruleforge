# Epic 6 Phase 5 Implementation Plan

**Project:** RuleForge  
**Epic:** Webhook Auth v2 - OAuth 2.0 & Custom Header  
**Prepared by:** Product Owner  
**Date:** 2026-04-30  
**Status:** Ready for Backend, Frontend, and QE execution  
**Source brief:** `.bmad/brainstorms/product-owner-epic-6-phase-5-implementation.md`

---

## 1. Objective

Deliver Epic 6 to acceptance-complete status: all six stories pass their acceptance criteria, smoke tests are updated, and the work is ready for Tech Lead release review.

The business outcome is enterprise-ready webhook authentication for proprietary header auth and OAuth 2.0 client credentials, with explicit auth-failure handling so broken credentials do not create retry storms.

## 2. Scope

### In Scope

| Story | Owner | Priority | Purpose |
|---|---|---:|---|
| 024 - AUTH_FAILURE delivery status | Backend | P0 | Safety gate; permanent auth failures do not retry or trip circuit breaker |
| 022 - Custom Header injector | Backend | P0 | Sends arbitrary configured header name/value |
| 023 - OAuth2 injector + token cache | Backend | P0 | Fetches and caches bearer tokens using Redis |
| 025 - API contract | Backend | P0 | Create/update/get contract, validation, masking, token cache eviction |
| 026 - Frontend form fields | Frontend | P1 | Adds Custom Header and OAuth2 configuration UI |
| 027 - Smoke tests section 3.22 | QE / Backend / TL | P1 | Proves contract, masking, RBAC, HTTPS validation, and AUTH_FAILURE visibility |

### Out of Scope

- New database migration.
- PKCE, mTLS, authorization code flow, or interactive OAuth.
- Token force-refresh on target 401 unless capacity remains after all P0/P1 acceptance criteria pass.
- New webhook delivery infrastructure.

## 3. Execution Model

Use parallel Backend/Frontend execution with backend leading the contract.

### Workstream A - Backend Foundation

1. Implement Story 024 first.
2. Implement Story 022 after AUTH_FAILURE and `WebhookAuthException` exist.
3. Implement Story 023 after the auth-failure path is stable.
4. Implement Story 025 to stabilize the create/update/get contract and masking behavior.

Backend must publish the final request/response shape from Story 025 before Frontend locks Story 026.

### Workstream B - Frontend Preparation

Frontend may prepare Story 026 in parallel using the approved UX spec, but should avoid final API assumptions until Story 025 is confirmed.

Frontend can safely start:

- Auth type option labels, ordering, and descriptions.
- Conditional form sections for `CUSTOM_HEADER` and `OAUTH2_CLIENT_CREDENTIALS`.
- HTTPS inline validation behavior.
- Provider hint copy.
- Display-mode layout for masked credentials.

Frontend should wait for Story 025 before finalizing:

- TypeScript API types.
- Create/update payload shape.
- Response masking assumptions.
- Rotate Secret / Rotate Value endpoint wiring.

### Workstream C - QE / Smoke Tests

QE starts smoke-test planning as soon as Story 025 contract is stable, but Story 027 should execute after Stories 022-026 are implemented.

Smoke tests must cover:

- Custom Header create/get masking.
- OAuth2 create/get masking.
- HTTP token URL rejected with 400.
- VIEWER cannot create new auth types.
- AUTH_FAILURE status visible or gracefully noted if no local trigger path exists.

## 4. Recommended Story Sequence

| Step | Story | Gate to proceed |
|---:|---|---|
| 1 | 024 - AUTH_FAILURE delivery status | Event-service unit tests pass; AUTH_FAILURE does not retry or trip circuit breaker |
| 2 | 022 - Custom Header injector | Registry returns CustomHeaderInjector; blank value maps to AUTH_FAILURE |
| 3 | 023 - OAuth2 injector + token cache | Cache hit/miss/failure/SSRF tests pass; secrets and tokens not logged |
| 4 | 025 - API contract | Create/update/get contract masks secrets; HTTPS + SSRF validation passes; cache eviction on auth update |
| 5 | 026 - Frontend form fields | Build passes; component tests pass; UI sends final contract payload |
| 6 | 027 - Smoke tests section 3.22 | Full smoke section passes or documents any environment-only skipped AUTH_FAILURE trigger |
| 7 | TL release review | All story DoDs checked; release readiness evidence complete |

## 5. Acceptance Gates

### Backend Gate

- `cd platform && ./gradlew :ruleforge-event-service:test` passes.
- AUTH_FAILURE is terminal, permanent, visible in delivery logs, and excluded from circuit breaker counts.
- Existing Epic 5 auth types continue to pass tests.
- OAuth2 token cache key is `tenant:{orgId}:webhook:{webhookId}:oauth_token`.
- Redis TTL is `max(expires_in - 60, 60)` seconds; absent `expires_in` defaults to 3540 seconds.
- `clientSecret`, `headerValue`, and `access_token` are not logged at INFO or above.

### Frontend Gate

- `npm run build` passes in `frontend/`.
- New tests for `WebhookAuthConfigForm` OAuth2 and Custom Header cases pass.
- `CUSTOM_HEADER` sends `headerValue`, not `apiKey`.
- OAuth2 validates HTTPS immediately and on submit.
- VIEWER role does not expose credential fields or rotation actions.

### QE Gate

- `scripts/smoke-test.sh` includes Section 3.22.
- Full local smoke run passes against `./scripts/start-local.sh`.
- `--quick` mode skips or gracefully avoids Section 3.22.
- Evidence is attached or referenced for the Tech Lead release check.

## 6. Product Acceptance Checklist

| Check | Required outcome |
|---|---|
| Custom Header business value | Proprietary header auth can be configured and sent without manual JSON/API work |
| OAuth2 business value | OAuth2 client credentials can be configured and used for delivery with token caching |
| Operator safety | Broken auth becomes AUTH_FAILURE and does not retry indefinitely |
| Secret handling | Sensitive values are encrypted at rest, masked in GET responses, and absent from logs |
| Contract confidence | Backend contract and frontend payloads align for both new auth types |
| Release readiness | Smoke tests and story DoDs are complete before TL review |

## 7. Risks And Mitigations

| Risk | Impact | Mitigation |
|---|---|---|
| Backend/frontend contract drift | Frontend rework, failed payloads | Story 025 is the contract gate before final Story 026 wiring |
| OAuth token cache edge cases | Retry storms or token endpoint saturation | Unit tests for cache hit, miss, TTL, absent `expires_in`, and failure behavior |
| Secret leakage | Security release blocker | Masking tests, logging review, and smoke assertions that raw secrets are absent |
| AUTH_FAILURE incorrectly trips circuit breaker | False operational outage | Story 024 must pass before 022/023; circuit breaker tests required |
| Smoke tests added too late | Release readiness delays | QE plans Section 3.22 after Story 025, implements after 026 |
| Local environment cannot trigger AUTH_FAILURE delivery | Incomplete smoke evidence | Make 3.22.7 informational or skipped with a note if no deterministic local trigger exists; rely on unit/integration tests for delivery path |

## 8. Required Source References

- PRD: `docs/prd-webhooks-oauth2-customheader.md`
- Architecture: `docs/architecture/solution-architecture-webhooks-oauth2-customheader.md`
- UX spec: `docs/ux/ui-spec-webhooks-oauth2-customheader.md`
- ADR: `docs/architecture/adr/ADR-010-oauth2-token-caching-strategy.md`
- Stories:
  - `docs/stories/story-024-auth-failure-status.md`
  - `docs/stories/story-022-custom-header-injector.md`
  - `docs/stories/story-023-oauth2-injector-token-cache.md`
  - `docs/stories/story-025-api-contract-oauth2-customheader.md`
  - `docs/stories/story-026-frontend-oauth2-customheader-form.md`
  - `docs/stories/story-027-smoke-tests-section-322.md`

## 9. Suggested Agent Routing

1. Backend Engineer: implement Stories 024, 022, 023, 025 in order.
2. Frontend Engineer: prepare Story 026 from UX spec, finalize after Story 025.
3. Tester/QE: prepare Story 027 smoke plan after Story 025, execute after Story 026.
4. Tech Lead: run release review after all acceptance gates pass.

## 10. PO Decision Log

| Date | Decision |
|---|---|
| 2026-04-30 | Phase 5 target is acceptance-complete implementation, not a backend-only or minimal-risk subset. |
| 2026-04-30 | Execution model is parallel BE/FE with backend leading contracts. |
| 2026-04-30 | Story order remains 024 -> 022 -> 023 -> 025 -> 026 -> 027. |
