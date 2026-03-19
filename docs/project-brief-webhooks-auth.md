# Project Brief — Webhooks Authentication Enhancement

**Date:** 2026-03-18
**Sponsor:** admin@ruleforge.io (Owner)
**Analyst:** BMAD Business Analyst Agent

---

## Executive Summary

RuleForge's current webhook system supports only a single authentication mechanism — an HMAC-SHA256 signing secret — for validating outbound webhook deliveries. Enterprise customers increasingly require webhooks that can authenticate to protected downstream APIs using established standards: API keys, Basic Auth, Bearer tokens, OAuth 2.0 client credentials, and custom headers. Without these capabilities, RuleForge cannot integrate with secured third-party systems (SIEM platforms, CRM APIs, compliance tools), which blocks enterprise adoption and forces customers to expose unauthenticated ingestion endpoints. This epic delivers multi-authentication webhook support, configurable backoff retry, and observability tooling to bring RuleForge's webhook delivery to enterprise-grade standards.

---

## Problem Statement

The current `WebhookEntity` supports a single `secret` field used for HMAC-SHA256 signing of outbound payloads. There is no mechanism to configure credentials for authenticating *to* downstream endpoints. Customers whose target systems require API keys, Basic Auth, Bearer tokens, or OAuth 2.0 cannot use RuleForge webhooks without exposing their endpoints to unauthenticated HTTP traffic.

**Business Impact:**
- Blocked integrations with Salesforce, ServiceNow, PagerDuty, Splunk, and other enterprise platforms that mandate authenticated inbound HTTP
- Lost enterprise deals requiring integration with access-controlled APIs
- Security risk: customers work around the limitation by disabling auth on receiving endpoints, creating attack surface
- Competitive disadvantage: Zapier, Make, and Workato all support multi-auth webhook delivery

---

## Stakeholder Analysis

| Stakeholder | Role | Power | Interest | Needs | Concerns |
|---|---|---|---|---|---|
| admin@ruleforge.io | Owner / Platform Admin | High | High | Configure webhooks to secured endpoints; full auth type control | Credentials stored securely; no plaintext secrets in DB |
| editor@ruleforge.io | Rule Author / EDITOR | Medium | High | Create webhooks with appropriate auth type for their integration target | Simple UX for common auth types (API Key, Bearer); OAuth complexity must be abstracted |
| viewer@ruleforge.io | Read-only consumer / VIEWER | Low | Low | View webhook status and delivery logs only | No accidental misconfiguration |
| Integration Partners (Salesforce, ServiceNow) | Downstream API Consumers | High | Medium | Receive properly authenticated HTTP POST with valid credentials | RuleForge must not leak or expose their API credentials |
| Security/Compliance Team | Gate-keeper | High | Medium | Credentials encrypted at rest, never logged; RBAC enforcement on credential visibility | Plaintext credential exposure in logs or responses |
| DevOps/On-call Team | Operator | Medium | Medium | Visibility into webhook delivery health, failure patterns, retry backlog | Alert fatigue from noisy DLQ; need actionable observability |

---

## Current State Assessment

### How It Works Today

1. Org owner/editor creates a webhook via `POST /v1/webhooks` with a `url`, list of `events`, optional `scope` (org-wide or space-scoped), and an optional `secret`.
2. On rule lifecycle events, `OutboxPoller` picks up `OutboxEvent` records and delivers them via `WebhookDeliveryService` to the configured URL.
3. The delivery uses Spring's `RestTemplate` / `WebClient` and optionally adds an `X-RuleForge-Signature` header derived from HMAC-SHA256 of the payload using the stored `secret`.
4. If delivery fails (non-2xx or timeout), exponential backoff retry kicks in up to a configured maximum. After max retries, the `WebhookDelivery` moves to `IN_DLQ` status.
5. Circuit breaker pauses delivery to endpoints returning repeated errors, moving the webhook to `CIRCUIT_OPEN` state.
6. Frontend: `WebhookListPage` shows all webhooks; `WebhookDetailPage` has 4 tabs (Settings, Security, Delivery Logs, Events). The Security tab allows rotating the HMAC signing secret.

### Pain Points

- **No outbound auth**: `WebhookDeliveryService` sends HTTP POST with no `Authorization` header. Any downstream endpoint requiring auth will reject with 401/403, causing immediate retry loops and DLQ.
- **Single auth model**: `WebhookEntity.secret` is the only credential field — there is no schema for API keys, usernames, bearer tokens, or OAuth config.
- **No credential encryption**: The current `secret` field is stored as plaintext in PostgreSQL. Enterprise credential types (API keys, OAuth client secrets) must be encrypted at rest.
- **No retry observability**: Admins cannot see the retry schedule for in-flight deliveries, or get aggregate views of success/failure rates per webhook.
- **Backoff not configurable**: Retry backoff is hardcoded. Enterprise customers need per-webhook control over initial delay, multiplier, and max attempts.
- **Creation UX is flat**: The webhook creation form has no auth type selector. Users with secured endpoints have nowhere to enter credentials.
- **OAuth 2.0 flow absent**: No OAuth client credentials flow for M2M auth. Required for integrations with enterprise identity providers (Okta, Azure AD, etc.).

### Current Capacity

- `WebhookEntity` schema: `id`, `orgId`, `spaceId`, `name`, `url`, `events`, `status`, `secret`, `active`, `createdAt`, `updatedAt`, plus delivery tracking fields
- Delivery infrastructure: Outbox poller, circuit breaker, DLQ — all functional
- Frontend: 4-tab `WebhookDetailPage` (Settings, Security, Delivery Logs, Events)
- Backend: `WebhookService`, `WebhookController`, `WebhookRepository` in event-service (port 8084)
- Credential encryption: connector-service has a `CredentialEncryptionService` pattern that can be referenced

---

## Desired Future State

### Vision

Webhook endpoints in RuleForge support all major HTTP authentication patterns used by enterprise APIs. Platform admins and editors configure the appropriate auth type per webhook via a clear, type-safe UI. Credentials are encrypted at rest and never appear in logs or GET responses. A new observability panel provides delivery success rates, retry counts, and DLQ depth per webhook.

### Key Capabilities Needed

1. **Multi-type outbound authentication**: System supports selecting one of: No Auth, API Key, Basic Auth, Bearer Token, Custom Header, OAuth 2.0 (client credentials), Webhook Signing Secret (existing). Each type has its own credential fields.
2. **Credential encryption at rest**: All auth credentials (API keys, passwords, tokens, OAuth secrets) are AES-256 encrypted in PostgreSQL. Credential values are write-only in API responses (masked after creation).
3. **Configurable backoff retry**: Per-webhook settings for initial retry delay (seconds), backoff multiplier, max attempts, and retry-on status codes. Frontend exposes these in an Advanced tab or Settings section.
4. **Webhook observability panel**: Per-webhook metrics tab showing: delivery success rate (7d), failure count by error type, current DLQ depth, retry queue size, average latency, last-N delivery timeline chart.
5. **OAuth 2.0 client credentials flow**: System can autonomously obtain and refresh Bearer tokens using client_id + client_secret + token_endpoint before each delivery. Token cached in Redis with TTL.

---

## Requirements Summary

### Functional Requirements

**Authentication Configuration**
- FR-01: Users with OWNER or EDITOR role must be able to select an auth type when creating or editing a webhook. Auth types: `NONE`, `API_KEY`, `BASIC_AUTH`, `BEARER_TOKEN`, `CUSTOM_HEADER`, `OAUTH2_CLIENT_CREDENTIALS`, `SIGNING_SECRET`.
- FR-02: Each auth type must present a type-specific credential form (see Gap Analysis for field details per type).
- FR-03: The system must inject the appropriate `Authorization` or custom header into each outbound delivery HTTP request.
- FR-04: Credential values must be masked in API GET responses (show only last 4 characters or `[REDACTED]`).
- FR-05: VIEWER role must not see credential fields or be able to modify auth configuration.

**Credential Security**
- FR-06: All auth credential values must be AES-256 encrypted before persistence in PostgreSQL.
- FR-07: Credential fields must never appear in application logs or event audit trail payloads.
- FR-08: OAuth 2.0 access tokens must be cached in Redis with TTL equal to the OAuth server's `expires_in` value.
- FR-09: OWNER role can rotate credentials via the Security tab (existing pattern for signing secret rotation).

**Backoff Retry Configuration**
- FR-10: Users with OWNER role must be able to configure per-webhook retry settings: `initialDelaySeconds` (1–3600), `backoffMultiplier` (1.0–10.0), `maxAttempts` (1–25), `retryOnStatusCodes` (default: 5xx, 408, 429).
- FR-11: System must honour per-webhook retry config; default config applies when not overridden.
- FR-12: Retry schedule must be visible in the Delivery Logs tab for in-flight deliveries.

**Observability**
- FR-13: A new "Metrics" section (or tab) in `WebhookDetailPage` must show: 7-day delivery success rate (%), total deliveries, failure count, DLQ depth, average delivery latency (ms), and a delivery timeline chart (pass/fail per day).
- FR-14: `GET /v1/webhooks/{id}/metrics` endpoint must return aggregated delivery stats.
- FR-15: `WebhookListPage` must show a health indicator badge per webhook (green/amber/red based on recent success rate).

### Non-Functional Requirements

- **Security**: Credentials encrypted with AES-256-GCM. Key managed via application.yml secret or AWS KMS (configuration-driven). Credentials never in logs or stack traces.
- **Performance**: OAuth token fetch must not add >200ms to delivery latency (cached in Redis). Encryption/decryption overhead <5ms per delivery.
- **Scalability**: Credential storage and encryption must support 10,000+ webhooks per org without degradation.
- **Compliance**: Credential masking must be enforced at the DTO layer — no conditional masking in controllers.
- **Backward compatibility**: Existing webhooks with `SIGNING_SECRET` auth must continue to function without migration or re-configuration.
- **Reliability**: OAuth token refresh failure must not prevent delivery attempt — fall back to unauthenticated delivery and log a warning.
- **Audit trail**: Auth type changes and credential rotations must be captured as `AuditEvent` records.

---

## Gap Analysis

| Current Capability | Gap | Impact | Priority |
|---|---|---|---|
| Single `secret` field (HMAC signing) | No API Key outbound auth (`Authorization: ApiKey <key>` or custom header like `X-API-Key`) | Blocks integrations with Stripe, Twilio, custom REST APIs | P0 |
| Single `secret` field | No Basic Auth (`Authorization: Basic base64(user:pass)`) | Blocks integrations with legacy enterprise APIs, Splunk HEC, Jenkins | P0 |
| Single `secret` field | No Bearer Token auth (`Authorization: Bearer <token>`) | Blocks integrations with most modern REST APIs, GitHub, Slack | P0 |
| Single `secret` field | No OAuth 2.0 client credentials flow | Blocks integrations with Salesforce, ServiceNow, Azure AD, Okta | P1 |
| Single `secret` field | No Custom Header auth (arbitrary header name + value) | Blocks integrations with proprietary APIs using non-standard auth | P1 |
| Plaintext `secret` in DB | No credential encryption at rest | Security/compliance violation for enterprise customers | P0 |
| Hardcoded retry backoff | No per-webhook retry configuration | Enterprise customers cannot tune delivery behaviour | P1 |
| No delivery metrics UI | No webhook observability | DevOps blind to delivery health; cannot triage failures | P1 |
| Flat creation form | No auth type selector in webhook create/edit UI | Cannot configure any auth type beyond signing secret | P0 |
| No OAuth token caching | — | N/A (new capability) | P1 |

**Auth Type Field Specifications:**

| Auth Type | Required Fields | Optional Fields |
|---|---|---|
| `NONE` | — | — |
| `API_KEY` | `apiKey` | `headerName` (default: `X-API-Key`) |
| `BASIC_AUTH` | `username`, `password` | — |
| `BEARER_TOKEN` | `token` | — |
| `CUSTOM_HEADER` | `headerName`, `headerValue` | — |
| `OAUTH2_CLIENT_CREDENTIALS` | `clientId`, `clientSecret`, `tokenEndpoint` | `scope` |
| `SIGNING_SECRET` | `secret` | — (existing behaviour) |

---

## Feasibility Assessment

### Technical Feasibility: GREEN
- Spring Boot 3.3.5 + Spring Security 6 natively support all required HTTP auth schemes via `RestTemplate`/`WebClient` interceptors
- AES-256-GCM encryption implementable with Java standard library (`javax.crypto`) — no new dependencies
- OAuth 2.0 client credentials flow available via `spring-security-oauth2-client` (already a Spring Boot transitive dep)
- Redis token caching uses existing `RedisTemplate` infrastructure in solver-service pattern
- connector-service has `CredentialEncryptionService` pattern to reference

### Timeline Feasibility: GREEN
- Estimated effort: 4–5 sprints (backend: 2 sprints, frontend: 1.5 sprints, QA + integration: 0.5 sprint)
- No hard external deadline; can release incrementally (core auth types first, OAuth 2.0 second)

### Budget Feasibility: GREEN
- No new infrastructure required. OAuth token caching uses existing Redis. No new services.
- AES key management can use application.yml secret initially; AWS KMS deferred to Phase 2.

### Organizational Feasibility: GREEN
- Backend engineer familiar with Spring Security interceptors and JPA encryption patterns
- Frontend engineer familiar with Zustand + React Query + shadcn/ui form patterns
- connector-service credential encryption pattern is reference implementation
- Risk: OAuth 2.0 token flow complexity — mitigated by using Spring's `OAuth2AuthorizedClientManager`

### Overall Risk Level: MEDIUM

**Top Risks:**

1. **Credential migration**: Existing webhooks have plaintext `secret` values. Migration to encrypted store requires careful one-time migration script with rollback path.
   - Mitigation: Flyway migration encrypts existing `secret` values at deploy time; dual-read support during transition window.

2. **OAuth token refresh race condition**: Multiple `OutboxPoller` threads may attempt token refresh simultaneously.
   - Mitigation: Redis-based distributed lock on token refresh; `SETNX` pattern with 30s TTL.

3. **Credential masking gaps**: Credentials leaked via error messages, stack traces, or audit events.
   - Mitigation: Custom `@JsonSerialize` masking annotation on DTO fields; integration test that asserts no plaintext in responses.

4. **Backward compatibility break**: Existing `POST /v1/webhooks` schema change (adding `authType` and nested credential object) must not break existing clients.
   - Mitigation: `authType` defaults to `SIGNING_SECRET` when `secret` provided; `NONE` when omitted — no breaking change.

---

## Assumptions

- AES-256 key is stored in `application.yml` as `ruleforge.webhook.credential-key` for MVP; KMS integration deferred.
- OAuth 2.0 scope: only `client_credentials` grant type for M2M (no user delegation flows).
- Credential rotation (PATCH secret) follows existing pattern in `WebhookService.rotateSecret()`.
- No mobile interface needed — enterprise BRMS is desktop-only (confirmed in tech-stack.md).
- Frontend TypeScript interfaces extend existing `Webhook` type without breaking existing pages.

---

## Unknowns (Research Needed)

- **Encryption key rotation strategy**: If `credential-key` is rotated, existing encrypted values become unreadable. Research: re-encryption migration strategy or key versioning.
  - Owner: Backend Engineer; resolve in Architecture phase.
- **OAuth token endpoint variety**: Some IdPs (Azure AD) require `tenant_id` in token URL; others (Okta) use `authorization_server_id`. Need to confirm whether a single `tokenEndpoint` field is sufficient or if provider-specific templates are needed.
  - Owner: Solution Architect; resolve before implementation.
- **Delivery metrics storage**: Should per-delivery aggregate metrics be computed from existing `webhook_deliveries` table (query on-demand) or pre-aggregated into a `webhook_metrics` table by a background job?
  - Owner: Solution Architect; resolve in Architecture phase.

---

## Success Criteria

- **Auth coverage**: All 7 auth types (`NONE`, `API_KEY`, `BASIC_AUTH`, `BEARER_TOKEN`, `CUSTOM_HEADER`, `OAUTH2_CLIENT_CREDENTIALS`, `SIGNING_SECRET`) functional end-to-end, verified by smoke tests.
- **Security**: Zero plaintext credential values in database, logs, or GET API responses — verified by automated security test.
- **Backward compatibility**: 100% of existing webhooks continue to deliver successfully after migration — verified by smoke test Section 3.20.
- **Observability**: `GET /v1/webhooks/{id}/metrics` returns accurate 7-day delivery stats — verified by integration test.
- **Frontend**: `npm run build` produces 0 TypeScript errors; all new form states covered by Vitest tests.
- **Backend**: `./gradlew build` succeeds with 0 compilation errors; 80% unit test coverage on new auth injection logic.
- **Performance**: Delivery latency p95 ≤ 500ms with auth overhead; OAuth token cache hit rate ≥ 95% in load test.

---

## Next Steps

1. **Sponsor approval** of this project brief (admin@ruleforge.io)
2. **Architecture research spike** on OAuth token URL pattern and delivery metrics storage strategy (Solution Architect, ~2 days)
3. **Handoff to Product Owner** to create PRD with stories, acceptance criteria, and RICE prioritization

**Status:** READY FOR SIGN-OFF
**Sponsor Sign-Off:** admin@ruleforge.io — 2026-03-18
