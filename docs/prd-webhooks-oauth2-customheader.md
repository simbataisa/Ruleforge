# PRD — Webhook Auth v2: OAuth 2.0 & Custom Header (Epic 6)

**Prepared by:** Product Owner
**Date:** 2026-03-19
**Epic:** Webhook Auth v2 — OAuth 2.0 Client Credentials & Custom Header
**Input:** `docs/project-brief-webhooks-oauth2-customheader.md`
**Status:** APPROVED — Proceeding to Architecture Phase

---

## 1. Executive Summary

Epic 6 closes the two auth types deferred from Epic 5. **Custom Header** is a low-effort, high-value addition that enables arbitrary single-header injection for bespoke API authentication patterns. **OAuth 2.0 Client Credentials** is the highest-demand enterprise capability — it unlocks webhook delivery to any OAuth 2.0-protected API (Salesforce, ServiceNow, Azure AD, Okta-fronted endpoints). The Epic 5 foundation (injector strategy pattern, credential encryption, enum, JSONB auth_config column, Redis) makes both additions surgical. No DB migration needed. Estimated effort: 9 story points across 6 implementation stories.

---

## 2. RICE Prioritization

| Story | Reach | Impact | Confidence | Effort | RICE Score | Priority |
|---|---|---|---|---|---|---|
| S-022: CustomHeaderInjector backend | 6 | 7 | 10 | 1 | 420 | P0 |
| S-023: OAuth2 model + injector + token cache | 8 | 10 | 8 | 4 | 160 | P0 |
| S-024: AUTH_FAILURE delivery status | 7 | 8 | 9 | 1 | 504 | P0 (safety gate) |
| S-025: API contract for OAuth2 + Custom Header | 8 | 8 | 9 | 1 | 576 | P0 |
| S-026: Frontend Custom Header + OAuth2 form fields | 8 | 9 | 8 | 2 | 288 | P1 |
| S-027: Smoke tests (section 3.22) | 8 | 7 | 10 | 1 | 560 | P1 |

*Reach 1–10 (10 = all active users). Impact: business/security value. Confidence: estimate certainty. Effort: story points (1 = minimal, 4 = large).*

**Total estimated effort: 10 story points (1 sprint)**

---

## 3. MoSCoW Classification

| Feature | Classification | Rationale |
|---|---|---|
| `CustomHeaderInjector` backend implementation | Must | Silent failure today — `CUSTOM_HEADER` webhooks send no auth |
| `OAuth2ClientCredentials` record in sealed interface | Must | Jackson deserialization failure without it |
| `OAuth2ClientCredentialsInjector` with token fetch | Must | Core capability of this epic |
| Redis token cache (`tenant:{orgId}:webhook:{id}:oauth_token`) | Must | Without cache, token endpoint saturated — not production-safe |
| `AUTH_FAILURE` delivery status (permanent, no retry) | Must | Prevents retry storms against OAuth providers on auth failure |
| SSRF guard applied to `tokenUrl` | Must | Security: same guard as webhook delivery URL |
| API contract: OAuth2 fields in create/update/get | Must | Frontend and integration contract |
| `clientSecret` + `headerValue` masked in GET responses | Must | Security non-negotiable |
| Frontend Custom Header field wiring | Should | Currently shows fields but silently drops them |
| Frontend OAuth 2.0 form fields (tokenUrl, clientId, clientSecret, scope) | Should | Completes the auth form for this auth type |
| Token force-refresh on delivery 401 | Could | Handles early token revocation; deferred if time-constrained |
| PKCE for OAuth flows | Won't (this epic) | Not required for client credentials; future enterprise auth epic |
| mTLS for token endpoint | Won't (this epic) | Out of scope; niche requirement |

---

## 4. User Stories

---

### Story 022 (P0): Custom Header Auth Injector

**As an** organization owner or editor,
**I want to** select "Custom Header" as the webhook auth type and configure a header name and value,
**So that** RuleForge can deliver events to APIs that use arbitrary proprietary authentication headers.

**Story Points:** 1
**Assigned to:** Backend Engineer
**Depends on:** Epic 5 (complete)

#### Acceptance Criteria

```gherkin
Feature: Custom Header webhook authentication injection

  Scenario: CustomHeaderInjector adds configured header to delivery
    Given a webhook with authType CUSTOM_HEADER, headerName "X-Tenant-Token", headerValue "abc123"
    When a rule lifecycle event triggers delivery
    Then the HTTP POST includes header "X-Tenant-Token: abc123"
    And no Authorization header is added by this injector
    And the delivery completes with status 200

  Scenario: CustomHeaderInjector is registered for CUSTOM_HEADER type
    Given the WebhookAuthInjectorRegistry
    When supportedType CUSTOM_HEADER is looked up
    Then CustomHeaderInjector is returned (not fallback NoneAuthInjector)

  Scenario: headerValue is decrypted before injection
    Given headerValue is stored encrypted in the database
    When delivery occurs
    Then CredentialEncryptionService.decrypt() is called to resolve the plaintext value
    And the plaintext value is injected as the header value
    And the plaintext value is not logged at INFO or above

  Scenario: Missing headerValue triggers AUTH_FAILURE
    Given a webhook with authType CUSTOM_HEADER and null headerValue
    When delivery is attempted
    Then delivery is marked AUTH_FAILURE with permanent=true
    And no HTTP request is made to the target URL
```

#### Definition of Done
- [ ] `CustomHeaderInjector.java` created in `io.ruleforge.event.delivery`
- [ ] `WebhookAuthInjectorRegistry` registers `CustomHeaderInjector` for `CUSTOM_HEADER`
- [ ] Unit tests cover: normal injection, decrypt call, null value → AUTH_FAILURE
- [ ] Existing injector unit tests still pass

---

### Story 023 (P0): OAuth 2.0 Client Credentials Injector with Token Cache

**As an** organization owner or editor,
**I want to** configure OAuth 2.0 client credentials (token URL, client ID, client secret, optional scope) for a webhook,
**So that** RuleForge can authenticate to enterprise APIs protected by OAuth 2.0 authorization servers (Okta, Azure AD, Salesforce, etc.).

**Story Points:** 4
**Assigned to:** Backend Engineer
**Depends on:** Story 022 (pattern), Epic 5 `CredentialEncryptionService`

#### Acceptance Criteria

```gherkin
Feature: OAuth 2.0 client credentials webhook authentication

  Scenario: OAuth2ClientCredentials record added to sealed interface
    Given the WebhookAuthConfig sealed interface
    Then it permits OAuth2ClientCredentials(tokenUrl, clientId, clientSecret, scope)
    And @JsonSubTypes includes OAUTH2_CLIENT_CREDENTIALS subtype

  Scenario: Token fetch on first delivery (cache miss)
    Given a webhook with authType OAUTH2_CLIENT_CREDENTIALS
    And no token exists in Redis for this webhook
    When delivery is triggered
    Then injector POSTs to tokenUrl with:
      | field         | value                  |
      | grant_type    | client_credentials     |
      | client_id     | <configured clientId>  |
      | client_secret | <decrypted secret>     |
      | scope         | <scope if configured>  |
    And on 200 response, access_token is cached in Redis
    And Redis TTL = expires_in - 60 seconds (minimum 60s)
    And Authorization: Bearer {access_token} is added to delivery headers

  Scenario: Token reuse on cache hit
    Given a token is cached in Redis with TTL > 0
    When delivery is triggered for the same webhook
    Then tokenUrl is NOT called
    And the cached access_token is used directly

  Scenario: Token cache key format
    Given orgId "org-123" and webhookId "wh-456"
    Then Redis key is "tenant:org-123:webhook:wh-456:oauth_token"

  Scenario: Token fetch failure → AUTH_FAILURE
    Given the token endpoint returns HTTP 401
    When delivery is triggered
    Then delivery is marked AUTH_FAILURE with permanent=true
    And no HTTP request is made to the webhook target URL
    And delivery log shows "OAuth2 token fetch failed: HTTP 401 from {tokenUrl}"

  Scenario: Token endpoint network error → AUTH_FAILURE
    Given the token endpoint is unreachable (connection timeout)
    When delivery is triggered
    Then delivery is marked AUTH_FAILURE with permanent=true
    And error message distinguishes token fetch failure from delivery failure

  Scenario: SSRF guard applied to tokenUrl
    Given tokenUrl is "http://169.254.169.254/latest/meta-data/"
    When the injector attempts to fetch a token
    Then SsrfGuard blocks the request
    And delivery is marked AUTH_FAILURE with permanent=true

  Scenario: clientSecret is never logged
    Given delivery with OAuth2 auth type
    When token fetch occurs (success or failure)
    Then clientSecret value does NOT appear in any log output at any level
    And access_token does NOT appear in logs at INFO or above
```

#### Definition of Done
- [ ] `WebhookAuthConfig.OAuth2ClientCredentials(tokenUrl, clientId, clientSecret, scope)` record added to sealed interface
- [ ] `@JsonSubTypes` annotation updated with `OAUTH2_CLIENT_CREDENTIALS` subtype
- [ ] `OAuth2ClientCredentialsInjector.java` created in `io.ruleforge.event.delivery`
- [ ] Token fetched via `RestTemplate` / `WebClient` (consistent with existing dispatcher HTTP client)
- [ ] Redis token cache: key `tenant:{orgId}:webhook:{id}:oauth_token`, TTL = `expires_in - 60s`, minimum 60s
- [ ] `SsrfGuard.check(tokenUrl)` called before token fetch
- [ ] `CredentialEncryptionService.decrypt()` called on `clientSecret` before token fetch
- [ ] Token fetch failure → `AUTH_FAILURE` with `permanent=true`
- [ ] `access_token` never persisted to PostgreSQL
- [ ] Unit tests: cache hit, cache miss → fetch, fetch failure → AUTH_FAILURE, SSRF block, null secret → AUTH_FAILURE
- [ ] Integration test: mock token endpoint returns token, subsequent delivery uses cache

---

### Story 024 (P0): AUTH_FAILURE Delivery Status

**As a** DevOps engineer monitoring webhook health,
**I want** a distinct `AUTH_FAILURE` delivery status separate from `FAILED`,
**So that** I can distinguish between "target endpoint down" (FAILED, may recover) and "authentication broken" (AUTH_FAILURE, needs operator action).

**Story Points:** 1
**Assigned to:** Backend Engineer
**Depends on:** none (prerequisite for 022, 023)

#### Acceptance Criteria

```gherkin
Feature: AUTH_FAILURE delivery status

  Scenario: AUTH_FAILURE is a permanent failure
    Given a delivery is marked AUTH_FAILURE
    Then permanent=true is set on DeliveryResult
    And the delivery is not retried
    And the circuit breaker is NOT triggered (it should only trigger on endpoint failures)

  Scenario: AUTH_FAILURE appears in delivery logs
    When a delivery has status AUTH_FAILURE
    Then GET /v1/webhooks/{id}/deliveries returns the entry with status "AUTH_FAILURE"
    And the errorMessage field contains a human-readable description of the auth error

  Scenario: AUTH_FAILURE does not count toward circuit breaker
    Given 5 consecutive AUTH_FAILURE deliveries
    Then the webhook circuit breaker is NOT opened
    And webhook status remains ACTIVE (not CIRCUIT_OPEN)
```

#### Definition of Done
- [ ] `WebhookDeliveryStatus.AUTH_FAILURE` added to the delivery status enum
- [ ] `OutboxPoller` / `WebhookDispatcher` uses `AUTH_FAILURE` for injector-thrown auth errors
- [ ] Circuit breaker logic excludes `AUTH_FAILURE` from failure count
- [ ] `WebhookDeliveryEntity` and `WebhookDeliveryDto` include the new status value
- [ ] Existing `FAILED` and `IN_DLQ` status handling unchanged
- [ ] Unit tests cover circuit breaker exclusion

---

### Story 025 (P0): API Contract — OAuth 2.0 & Custom Header

**As a** backend engineer,
**I want** the create/update/get webhook API to accept and return OAuth 2.0 and Custom Header auth config,
**So that** the frontend and integration clients can configure and read these auth types.

**Story Points:** 1
**Assigned to:** Backend Engineer
**Depends on:** Story 023 (sealed interface record)

#### Acceptance Criteria

```gherkin
Feature: API contract for OAuth2 and Custom Header auth types

  Scenario: Create webhook with OAUTH2_CLIENT_CREDENTIALS
    When POST /v1/webhooks with:
      {
        "url": "https://api.example.com/events",
        "events": ["rule.activated"],
        "authType": "OAUTH2_CLIENT_CREDENTIALS",
        "authConfig": {
          "tokenUrl": "https://auth.example.com/oauth/token",
          "clientId": "my-client-id",
          "clientSecret": "my-secret",
          "scope": "webhooks:receive"
        }
      }
    Then response 201 contains authType: "OAUTH2_CLIENT_CREDENTIALS"
    And response authConfig contains tokenUrl and clientId
    And response authConfig does NOT contain clientSecret (masked as "[REDACTED]")

  Scenario: Create webhook with CUSTOM_HEADER
    When POST /v1/webhooks with authType CUSTOM_HEADER, headerName "X-App-Token", headerValue "tok123"
    Then response 201 contains authType: "CUSTOM_HEADER"
    And response authConfig contains headerName: "X-App-Token"
    And response authConfig does NOT contain headerValue (masked as "[REDACTED]")

  Scenario: GET webhook masks sensitive fields
    Given an existing webhook with authType OAUTH2_CLIENT_CREDENTIALS
    When GET /v1/webhooks/{id}
    Then clientSecret is "[REDACTED]"
    And tokenUrl and clientId are returned

  Scenario: PUT webhook updates OAuth2 config
    Given an existing OAUTH2 webhook
    When PUT /v1/webhooks/{id}/auth-config with new clientSecret
    Then the new clientSecret is encrypted and stored
    And the Redis token cache entry for this webhook is invalidated

  Scenario: Validation — tokenUrl must be HTTPS
    When POST /v1/webhooks with tokenUrl "http://insecure.example.com/token"
    Then response 400 with error "tokenUrl must use HTTPS"

  Scenario: Validation — tokenUrl SSRF check
    When POST /v1/webhooks with tokenUrl pointing to private IP range
    Then response 400 with error "tokenUrl resolves to a disallowed address"
```

#### Definition of Done
- [ ] `WebhookAuthConfigRequest` updated: `OAuth2ClientCredentials` fields (`tokenUrl`, `clientId`, `clientSecret`, `scope`)
- [ ] `WebhookAuthConfigDto` updated: same fields, `clientSecret` and `headerValue` masked in responses
- [ ] `WebhookController` validates `tokenUrl` is HTTPS before persisting
- [ ] `WebhookController` calls `SsrfGuard` on `tokenUrl` during create/update
- [ ] On auth config update, Redis cache key for the webhook is deleted (`redisTemplate.delete(...)`)
- [ ] OpenAPI / Swagger annotations updated (if present)
- [ ] Integration tests cover create/get/update for both new auth types

---

### Story 026 (P1): Frontend — Custom Header & OAuth 2.0 Form Fields

**As an** organization owner or editor,
**I want** the webhook creation and settings forms to show the correct fields for Custom Header and OAuth 2.0 auth types,
**So that** I can configure these auth types without editing raw JSON.

**Story Points:** 2
**Assigned to:** Frontend Engineer
**Depends on:** Story 025 (API contract)

#### Acceptance Criteria

```gherkin
Feature: Frontend auth form for Custom Header and OAuth 2.0

  Background:
    Given I am logged in as OWNER or EDITOR
    And I am on the webhook creation form

  Scenario: Custom Header fields render correctly
    When I select "Custom Header" from the auth type dropdown
    Then I see field "Header Name" (text input, default "X-Custom-Header", required)
    And I see field "Header Value" (password input, masked, required)
    And both fields are present in the form submission payload

  Scenario: OAuth 2.0 fields render correctly
    When I select "OAuth 2.0 (Client Credentials)" from the auth type dropdown
    Then I see field "Token URL" (URL input, required, placeholder "https://auth.example.com/oauth/token")
    And I see field "Client ID" (text input, required)
    And I see field "Client Secret" (password input, masked, required)
    And I see field "Scope" (text input, optional, placeholder "e.g. webhooks:receive api:read")
    And all fields are present in the form submission payload

  Scenario: OAuth 2.0 Token URL validation
    When I enter a non-HTTPS token URL (e.g. "http://...")
    Then an inline validation error shows "Token URL must use HTTPS"
    And the form cannot be submitted

  Scenario: Existing OAuth2 webhook shows config in settings (read-only sensitive)
    Given a webhook with authType OAUTH2_CLIENT_CREDENTIALS
    When I open the Settings tab
    Then Token URL and Client ID are shown
    And Client Secret shows "[REDACTED]" placeholder
    And a "Rotate Secret" button is available to enter a new client secret

  Scenario: Custom Header settings shows masked value
    Given a webhook with authType CUSTOM_HEADER
    When I open the Settings tab
    Then Header Name is shown
    And Header Value shows "[REDACTED]" placeholder
    And a "Rotate Value" button is available

  Scenario: VIEWER sees auth type label only
    Given I am logged in as VIEWER
    When I view a webhook with authType OAUTH2_CLIENT_CREDENTIALS
    Then I see "OAuth 2.0 (Client Credentials)" as the auth type label
    And no credential fields are visible
    And no edit controls are shown
```

#### Definition of Done
- [ ] `WebhookAuthConfigForm.tsx`: Custom Header fields (`headerName`, `headerValue`) wired to form state and submission
- [ ] `WebhookAuthConfigForm.tsx`: OAuth 2.0 fields (`tokenUrl`, `clientId`, `clientSecret`, `scope`) rendered and submitted
- [ ] Client-side validation: `tokenUrl` must start with `https://`
- [ ] `WebhookSettingsTab.tsx`: displays `tokenUrl` + `clientId` for OAuth2; shows "[REDACTED]" for secrets; "Rotate Secret/Value" pattern for updates
- [ ] Auth type dropdown label: "OAuth 2.0 (Client Credentials)" (not raw enum value)
- [ ] `webhooks.api.ts` updated to include new fields in request/response types
- [ ] Frontend TypeScript types updated for `OAuth2ClientCredentials` and complete `CustomHeader`
- [ ] Existing auth types (API Key, Basic Auth, Bearer Token, Signing Secret, None) still work without regression
- [ ] Component renders correctly in Storybook / snapshot tests (if applicable)

---

### Story 027 (P1): Smoke Tests — Section 3.22

**As a** tech lead,
**I want** smoke test cases for Custom Header and OAuth 2.0 auth types,
**So that** post-deploy verification confirms both new auth types are functional end-to-end.

**Story Points:** 1
**Assigned to:** Backend Engineer / Tech Lead
**Depends on:** Stories 022–026

#### Acceptance Criteria

```gherkin
Feature: Smoke tests for Epic 6 auth types

  Scenario: 3.22.1 — Create webhook with CUSTOM_HEADER
    When POST /v1/webhooks with authType CUSTOM_HEADER
    Then response 201, authType is CUSTOM_HEADER, headerValue is [REDACTED]

  Scenario: 3.22.2 — Create webhook with OAUTH2_CLIENT_CREDENTIALS
    When POST /v1/webhooks with tokenUrl, clientId, clientSecret
    Then response 201, authType is OAUTH2_CLIENT_CREDENTIALS, clientSecret is [REDACTED]

  Scenario: 3.22.3 — GET response masks credentials for both types
    Given webhooks created in 3.22.1 and 3.22.2
    When GET /v1/webhooks/{id} for each
    Then headerValue and clientSecret are both [REDACTED]

  Scenario: 3.22.4 — VIEWER denied create for both types
    Given I am VIEWER
    When POST /v1/webhooks with either new auth type
    Then 401 or 403 response

  Scenario: 3.22.5 — tokenUrl HTTPS validation
    When POST /v1/webhooks with tokenUrl starting with "http://"
    Then 400 response with validation error message
```

#### Definition of Done
- [ ] `scripts/smoke-test.sh` section 3.22 added with all 5 test cases
- [ ] All tests pass against a running local stack
- [ ] Section follows existing smoke test format and conventions

---

## 5. Release Sequencing

```
Sprint 1 (all P0 first):
  Day 1-2:  Story 024 — AUTH_FAILURE status (prerequisite, 1 SP)
  Day 1-3:  Story 022 — CustomHeaderInjector (1 SP, parallelizable)
  Day 2-5:  Story 023 — OAuth2 injector + token cache (4 SP, core work)
  Day 4-5:  Story 025 — API contract (1 SP, after 023 sealed interface)

Sprint 1 continues (P1):
  Day 5-7:  Story 026 — Frontend (2 SP, after 025 API contract)
  Day 7-8:  Story 027 — Smoke tests (1 SP, after 022-026)
```

**Dependencies:**
- 024 → unblocks 022, 023 (both need AUTH_FAILURE status)
- 023 → must complete sealed interface before 025 (API contract DTO update)
- 025 → unblocks 026 (frontend needs final API contract)
- 026, 027 → final verification before release readiness

---

## 6. Out of Scope (v3 candidates)

| Feature | Rationale for deferral |
|---|---|
| PKCE for OAuth flows | Not required for client credentials grant (RFC 6749 §4.4) |
| mTLS for token endpoint | Niche enterprise requirement; needs PKI infrastructure |
| Token force-refresh on delivery 401 | Nice-to-have; cache invalidation on 401 adds complexity |
| OAuth 2.0 Authorization Code flow | Interactive user flow, incompatible with M2M webhook context |
| Bulk metrics endpoint | Separate backlog item from Epic 5 v2 scope |

---

## 7. Success Metrics

| Metric | Target | Measurement |
|---|---|---|
| CUSTOM_HEADER delivery success rate | 100% for valid credentials | Smoke test + monitoring |
| OAuth2 token cache hit rate | >95% in steady state | Redis cache metrics |
| AUTH_FAILURE vs FAILED status distinction | Both present in delivery logs | Smoke test |
| clientSecret / headerValue in GET response | Never (0 occurrences) | Security smoke test |
| No regression on existing auth types | 100% existing smoke tests pass | CI smoke test suite |

---

**Product Owner Sign-Off:** BMAD Product Owner Agent — 2026-03-19
