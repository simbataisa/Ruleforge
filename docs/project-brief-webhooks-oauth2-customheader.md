# Project Brief — Webhook Auth v2: OAuth 2.0 & Custom Header

**Date:** 2026-03-19
**Sponsor:** admin@ruleforge.io (Owner)
**Analyst:** BMAD Business Analyst Agent
**Epic:** Webhook Auth v2 — OAuth 2.0 Client Credentials & Custom Header (Epic 6)
**Input:** Deferred scope from Epic 5 PRD (US-9 Custom Header, US-10 OAuth 2.0)

---

## Executive Summary

Epic 5 delivered multi-auth webhook support (API Key, Basic Auth, Bearer Token, Signing Secret, None) with AES-256-GCM credential encryption. Two auth types were explicitly deferred: **Custom Header** (niche but straightforward) and **OAuth 2.0 Client Credentials** (highest enterprise demand, highest complexity). This epic delivers both. Custom Header enables arbitrary single-header injection for bespoke API authentication patterns. OAuth 2.0 Client Credentials enables M2M authentication against enterprise identity providers (Okta, Azure AD, Ping Identity), unlocking integrations with Salesforce, ServiceNow, PagerDuty, and any OAuth 2.0-protected API. The backend infrastructure from Epic 5 (injector strategy pattern, credential encryption, `WebhookAuthType` enum, `WebhookAuthConfig` sealed interface) is already in place — this epic adds the two missing injector implementations, OAuth token caching, API contract extension, and frontend form support.

---

## Problem Statement

Two auth types defined in the Epic 5 schema are not yet implemented:

1. **Custom Header** (`CUSTOM_HEADER` in enum, `CustomHeader` record in `WebhookAuthConfig`, no injector): Enterprise APIs with non-standard auth patterns (e.g., `X-Tenant-Token`, `X-Service-Key`, custom gateway auth headers) cannot be targeted by RuleForge webhooks.

2. **OAuth 2.0 Client Credentials** (`OAUTH2_CLIENT_CREDENTIALS` in enum, missing record in `WebhookAuthConfig`, no injector): Any downstream API secured by an OAuth 2.0 authorization server rejects RuleForge webhook deliveries with 401. This includes the majority of enterprise SaaS APIs (Salesforce, ServiceNow, Microsoft Graph, AWS API Gateway + Cognito, Okta-protected endpoints).

**Business Impact:**
- Custom Header blocks ~20% of enterprise integration targets that use proprietary auth headers
- OAuth 2.0 absence blocks ~60% of modern enterprise SaaS API integrations
- Both are publicly deferred promises — enterprise evaluators who reviewed Epic 5 were told v2 would close these gaps

---

## Stakeholder Analysis

| Stakeholder | Role | Power | Interest | Needs | Concerns |
|---|---|---|---|---|---|
| admin@ruleforge.io | Owner / Platform Admin | High | High | OAuth 2.0 config for Salesforce/ServiceNow integrations; Custom Header for internal APIs | Token refresh must be transparent; credentials encrypted; token not logged |
| editor@ruleforge.io | Rule Author / EDITOR | Medium | High | Ability to configure OAuth 2.0 and Custom Header during webhook creation/editing | OAuth config must be intuitive — token URL + client ID + secret + scope is complex enough |
| viewer@ruleforge.io | Read-only consumer / VIEWER | Low | Low | View auth type label only (no credential visibility) | No accidental exposure of tokens or secrets |
| Integration Partners (Salesforce, ServiceNow, Okta) | Downstream API Providers | High | Medium | Receive properly-credentialed HTTP POST; OAuth tokens must be current (not expired) | RuleForge must not store plaintext OAuth client secrets |
| Security/Compliance Team | Gate-keeper | High | High | OAuth tokens cached securely; client secrets encrypted at rest; token values never logged | Token leakage via logs, responses, or error messages |
| DevOps/On-call Team | Operator | Medium | Medium | Observability of OAuth token fetch failures; distinction between delivery failure vs auth failure | Alert fatigue; need actionable error codes for token fetch vs delivery failure |

---

## Current State Assessment

### What Is Already Built (Epic 5)

- `WebhookAuthType` enum: `NONE`, `SIGNING_SECRET`, `API_KEY`, `BASIC_AUTH`, `BEARER_TOKEN`, `CUSTOM_HEADER`, `OAUTH2_CLIENT_CREDENTIALS`
- `WebhookAuthConfig` sealed interface with records: `None`, `SigningSecret`, `ApiKey`, `BasicAuth`, `BearerToken`, `CustomHeader(headerName, headerValue)`
  - **Missing:** `OAuth2ClientCredentials` record
- `WebhookAuthInjector` strategy interface with `supportedType()` + `injectAuth(HttpHeaders, WebhookAuthConfig, byte[])`
- Injector implementations: `NoneAuthInjector`, `SigningSecretInjector`, `ApiKeyAuthInjector`, `BasicAuthInjector`, `BearerTokenInjector`
  - **Missing:** `CustomHeaderInjector`, `OAuth2ClientCredentialsInjector`
- `WebhookAuthInjectorRegistry` wires injectors to types
- `CredentialEncryptionService` (AES-256-GCM): encrypts/decrypts auth config before persistence
- `WebhookDispatcher` calls injector before HTTP delivery
- Frontend `WebhookAuthConfigForm.tsx`: renders per-type fields (renders CUSTOM_HEADER and OAUTH2 options but not wired to save)
- DB: `auth_type` column + `auth_config` JSONB column on `webhook` table — already can store any JSON structure

### Pain Points / Gaps

| Gap | Impact |
|---|---|
| No `CustomHeaderInjector` | `CUSTOM_HEADER` auth type silently fails — injector registry returns `NoneAuthInjector` fallback |
| No `OAuth2ClientCredentials` record in sealed interface | `OAUTH2_CLIENT_CREDENTIALS` cannot be deserialized or persisted; Jackson will throw on unknown subtype |
| No `OAuth2ClientCredentialsInjector` | OAuth 2.0 delivery is impossible |
| No token cache | Even if injector exists, calling token endpoint on every delivery would saturate OAuth providers (rate limits, latency) |
| Frontend form incomplete | `WebhookAuthConfigForm` lists CUSTOM_HEADER and OAUTH2 in dropdown but field groups are not wired to API contract |
| No distinction in error handling | Token fetch failures (auth server down) vs delivery failures (target down) currently fold into same retry logic |

---

## Desired Future State

### Vision

Webhook operators can configure OAuth 2.0 client credentials and Custom Header auth types in the same creation/settings flow as all other auth types. OAuth token acquisition is transparent — the system fetches and caches tokens automatically, handles expiry, and reports token fetch failures distinctly from delivery failures. Custom Header injection is a zero-config extension of the existing API Key pattern.

### Key Capabilities Needed

1. **Custom Header auth type (backend):** `CustomHeaderInjector` adds `{headerName}: {headerValue}` to outbound requests. Uses encrypted `headerValue`. Credential handling is identical to API Key.

2. **OAuth 2.0 Client Credentials auth type (backend):** `OAuth2ClientCredentials` record added to `WebhookAuthConfig` sealed interface. `OAuth2ClientCredentialsInjector` fetches a Bearer token from the configured `tokenUrl` using `client_id` + `client_secret` (+ optional `scope`), then adds `Authorization: Bearer {token}`. Token is cached in Redis with TTL = `expires_in - 60s`. On token fetch failure, delivery is aborted and marked `AUTH_FAILURE` (not retried).

3. **Token cache (Redis):** Keys follow `tenant:{orgId}:webhook:{webhookId}:oauth_token`. TTL set from OAuth response `expires_in`. Cache miss triggers synchronous token fetch before delivery. Token value is never persisted to PostgreSQL.

4. **Auth failure classification:** New `DeliveryStatus` value `AUTH_FAILURE` (permanent failure, no retry) for when token acquisition fails or returns non-2xx. This prevents retry storms against OAuth servers.

5. **API contract extension:** `WebhookAuthConfigRequest` and `WebhookAuthConfigDto` updated to include `OAuth2ClientCredentials` fields: `tokenUrl`, `clientId`, `clientSecret`, `scope` (optional). `clientSecret` is write-only in responses.

6. **Frontend form completion:** `WebhookAuthConfigForm.tsx` renders `tokenUrl`, `clientId`, `clientSecret`, `scope` fields when `OAUTH2_CLIENT_CREDENTIALS` selected; confirms Custom Header fields are wired to the save action.

---

## Requirements Summary

### Functional Requirements

**Custom Header (simple):**
- FR-1: `CustomHeaderInjector` must add header `{headerName}: {decryptedHeaderValue}` to every outbound delivery
- FR-2: `headerName` must default to `X-Custom-Header` in the frontend but be user-editable
- FR-3: `headerValue` must be encrypted at rest via `CredentialEncryptionService`
- FR-4: `headerValue` must be write-only in API responses (masked as `[REDACTED]`)

**OAuth 2.0 Client Credentials (complex):**
- FR-5: System must support `tokenUrl`, `clientId`, `clientSecret`, optional `scope` as OAuth config fields
- FR-6: `OAuth2ClientCredentialsInjector` must POST to `tokenUrl` with `grant_type=client_credentials`, `client_id`, `client_secret`, and optional `scope` (form-encoded body)
- FR-7: On successful token response, system must cache `access_token` in Redis with TTL = `expires_in - 60s` (grace period)
- FR-8: On cache hit, system must use cached token without calling token URL
- FR-9: On token fetch failure (HTTP non-2xx, network error, timeout), delivery must be marked `AUTH_FAILURE` with `permanent=true` (no retry)
- FR-10: `clientSecret` must be encrypted at rest via `CredentialEncryptionService`
- FR-11: `clientSecret` must be write-only in API responses
- FR-12: `access_token` values must NEVER be stored in PostgreSQL
- FR-13: OAuth token fetch errors must be distinguishable in delivery logs from endpoint delivery errors

**RBAC:**
- FR-14: VIEWER role must not be able to read or modify auth credentials for either type
- FR-15: OWNER and EDITOR roles may create and update auth config

### Non-Functional Requirements

- **Performance:** OAuth token cache hit rate must be >95% in steady state (i.e., tokens are not refetched on every delivery)
- **Security:** `clientSecret` and `headerValue` encrypted at rest (AES-256-GCM, same key as Epic 5); tokens never logged beyond DEBUG level; tokens never in DB
- **Reliability:** Token fetch failure → `AUTH_FAILURE` (no retry storm against OAuth server); circuit breaker not triggered by `AUTH_FAILURE`
- **Scalability:** Token cache in Redis (shared across event-service instances) — no per-JVM token state
- **Compliance:** `clientSecret` masked in all API responses; never appears in audit logs
- **Compatibility:** Must not break existing `CustomHeader` record in sealed interface (it already exists, only the injector is missing)

### Constraints

- Must use existing `CredentialEncryptionService` (no new encryption key or algorithm)
- Must use existing Redis instance (no new caching infrastructure)
- `WebhookAuthConfig` sealed interface changes must maintain Jackson polymorphic deserialization compatibility
- No changes to DB schema — `auth_config` JSONB column already supports arbitrary auth type structures
- Token fetch must use an HTTP client consistent with the service's existing `RestTemplate` / `WebClient`

---

## Gap Analysis

| Current Capability | Gap | Impact | Priority |
|---|---|---|---|
| `CustomHeader` record in sealed interface | No `CustomHeaderInjector` — auth silently not applied | High — silent failure for CUSTOM_HEADER webhooks | P0 |
| `OAUTH2_CLIENT_CREDENTIALS` in enum | No `OAuth2ClientCredentials` record in sealed interface | High — Jackson deserialization failure on save | P0 |
| `WebhookAuthInjectorRegistry` | No `OAuth2ClientCredentialsInjector` registered | High — OAuth delivery impossible | P0 |
| Redis available in event-service | No token cache implementation | High — without cache, token endpoint called per delivery | P0 |
| `WebhookAuthConfigForm.tsx` renders both types | Fields not connected to save API call | Medium — UI appears to support but silently drops config | P1 |
| `DeliveryResult` has `permanent` boolean | No `AUTH_FAILURE` status to distinguish from delivery failure | Medium — ops cannot distinguish auth vs network failures | P1 |

---

## Feasibility Assessment

### Technical Feasibility: GREEN
- Custom Header is a trivial 1-class addition; sealed interface already has the record
- OAuth 2.0 client credentials flow is standard RFC 6749 — no novel implementation required
- Redis token caching is straightforward; existing Redis config in event-service
- No DB migration needed — `auth_config` JSONB column is already flexible

### Timeline Feasibility: GREEN
- Custom Header: ~2 story points (backend only; frontend already partially done)
- OAuth 2.0 + token cache: ~6–8 story points (injector + cache + error classification + frontend)
- Total: ~8–10 story points — fits comfortably in one sprint

### Budget Feasibility: GREEN
- No new infrastructure (uses existing Redis, existing encryption key, existing injector pattern)
- Minimal developer effort; high leverage from Epic 5 foundation

### Organizational Feasibility: GREEN
- Backend: same team that built Epic 5 injectors; pattern is well-established
- Frontend: same `WebhookAuthConfigForm.tsx` already has placeholder fields

### Overall Risk Level: LOW–MEDIUM

**Top Risks:**
1. **OAuth token cache race condition** (Medium): Two event-service instances may simultaneously fetch a new token on cache expiry. Mitigation: use Redis `SET NX PX` (atomic set-if-absent) to elect one fetcher; others wait and retry from cache.
2. **OAuth provider rate limiting** (Low): If cache TTL is set incorrectly, token endpoint may receive excessive calls. Mitigation: `expires_in - 60s` grace; monitor via delivery log token-fetch metrics.
3. **sealed interface compatibility** (Low): Adding `OAuth2ClientCredentials` to `WebhookAuthConfig` requires `@JsonSubTypes` annotation update. Mitigation: already done for `CustomHeader` — same pattern applies.

---

## Assumptions

- `WEBHOOK_CREDENTIAL_KEY` env var is set in production (required by Epic 5; documented in release readiness doc)
- Redis is available and healthy in event-service's classpath (already confirmed in Epic 5 stack)
- Token endpoint URLs are HTTPS only (SSRF guard `SsrfGuard` already in place — apply same check to `tokenUrl`)
- OAuth providers return standard RFC 6749 token response (`access_token`, `expires_in`, `token_type`)
- `expires_in` field is always present in token responses (industry standard; treat missing as 3600s fallback)

---

## Unknowns

- **PKCE / confidential client support:** Some OAuth providers require PKCE even for client credentials. Out of scope for now — flag as enhancement.
- **mTLS for OAuth token endpoint:** Some enterprise IdPs require client certificate auth at the token endpoint. Deferred to a future epic.
- **Token refresh on 401:** If a cached token is rejected by the delivery endpoint (early revocation), should we force-refresh? Recommended behavior: on delivery 401, flush Redis cache entry and retry once. Flag for Product Owner prioritization.

---

## Success Criteria

- `CUSTOM_HEADER` webhooks successfully deliver with the configured header (verified by smoke tests)
- `OAUTH2_CLIENT_CREDENTIALS` webhooks acquire tokens, cache them, and deliver with `Authorization: Bearer {token}` header
- Token endpoint called only once per TTL window across all event-service instances (Redis cache verified)
- `AUTH_FAILURE` delivery status distinguishable in delivery logs from `FAILED` (endpoint error)
- `clientSecret` and `headerValue` never appear in API GET responses
- All existing auth types (Epic 5) continue to work without regression

---

## Next Steps

1. Sponsor review and sign-off on this brief
2. Product Owner creates PRD with story breakdown and RICE scoring
3. Solution Architect creates ADR-010 (OAuth token caching strategy)
4. Implementation begins — estimated 1 sprint

**Status:** READY FOR SIGN-OFF
**Analyst Sign-Off:** BMAD Business Analyst Agent — 2026-03-19
