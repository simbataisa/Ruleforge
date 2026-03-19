# PRD — Webhooks Authentication Enhancement (Epic 5)

**Prepared by:** Product Owner
**Date:** 2026-03-18
**Epic:** Webhooks Authentication Enhancement (Epic 5)
**Input:** `docs/project-brief-webhooks-auth.md`
**Status:** APPROVED — Proceeding to Architecture Phase

---

## 1. Executive Summary

Enterprise webhook consumers require RuleForge to authenticate outbound HTTP deliveries using industry-standard methods: API Key, Basic Auth, Bearer Token, OAuth 2.0 client credentials, Custom Header, and optionally no authentication. The current system supports only HMAC-SHA256 signing secrets. This epic adds multi-type outbound authentication to the webhook creation and settings workflow, encrypts all credentials at rest, introduces configurable backoff retry parameters, and adds a delivery metrics panel for observability. The scope is a backend schema extension to `event-service`, a new credential encryption layer, and a revised frontend webhook creation/settings form with an auth type selector.

**MVP scope (this epic):** Auth type selector + API Key + Basic Auth + Bearer Token + Signing Secret (existing) + credential encryption + configurable retry + delivery metrics UI.
**Deferred to v2:** OAuth 2.0 client credentials flow (token refresh complexity), Custom Header auth type.

---

## 2. RICE Prioritization

| Story | Reach | Impact | Confidence | Effort | RICE Score | Priority |
|-------|-------|--------|------------|--------|------------|----------|
| US-1: Auth type selector + API Key auth | 9 | 9 | 9 | 3 | 243 | P0 |
| US-2: Basic Auth outbound | 8 | 8 | 9 | 2 | 288 | P0 |
| US-3: Bearer Token outbound | 9 | 9 | 9 | 2 | 364 | P0 |
| US-4: Credential encryption at rest | 10 | 10 | 9 | 3 | 300 | P0 (security enabler) |
| US-5: Signing Secret (existing) in new form model | 9 | 8 | 10 | 1 | 720 | P0 (backward compat) |
| US-6: Configurable backoff retry | 6 | 7 | 8 | 3 | 112 | P1 |
| US-7: Delivery metrics panel | 7 | 8 | 7 | 4 | 98 | P1 |
| US-8: NONE auth type (no credentials) | 8 | 6 | 10 | 1 | 480 | P0 |
| US-9: Custom Header auth (v2 deferral) | 4 | 6 | 7 | 2 | 84 | P2 (deferred) |
| US-10: OAuth 2.0 client credentials (v2 deferral) | 5 | 9 | 6 | 8 | 34 | P2 (deferred) |

*Reach: 1–10 scale (10 = all active enterprise users). Impact: business/security value. Confidence: certainty in estimate. Effort: story points (1 = minimal, 8 = large).*

**MVP includes:** US-1, US-2, US-3, US-4, US-5, US-6, US-7, US-8
**v2 scope:** US-9 (Custom Header), US-10 (OAuth 2.0)

---

## 3. MoSCoW Classification

| Feature | Classification | Rationale |
|---|---|---|
| Auth type selector in create/edit form | Must | Gate for all auth types |
| API Key outbound injection | Must | Most common enterprise API auth pattern |
| Basic Auth outbound injection | Must | Required for legacy and SIEM integrations |
| Bearer Token outbound injection | Must | Required for modern REST API integrations |
| NONE auth type | Must | Required for public webhook targets |
| Credential encryption at rest | Must | Security/compliance non-negotiable |
| Backward compat for existing SIGNING_SECRET webhooks | Must | Zero migration risk required |
| Configurable retry parameters | Should | Improves enterprise reliability; not a blocker |
| Delivery metrics panel | Should | Improves observability; not a blocker for auth |
| Custom Header auth | Could | Niche; deferred to v2 |
| OAuth 2.0 client credentials | Could | High complexity; deferred to v2 |
| OAuth token refresh caching | Won't (this epic) | Depends on OAuth 2.0 implementation in v2 |

---

## 4. User Stories

---

### US-1 (P0): Auth Type Selector and API Key Authentication

**As an** organization owner or editor,
**I want to** select "API Key" as the authentication type when creating or editing a webhook and provide a key value and header name,
**So that** RuleForge can authenticate to downstream APIs that require API key headers.

**RICE Score:** 243

#### Acceptance Criteria

```gherkin
Feature: API Key webhook authentication

  Background:
    Given I am logged in as OWNER or EDITOR
    And I am on the webhook creation form

  Scenario: Auth type selector is present on creation form
    When I open the webhook creation form
    Then I see an "Authentication" section with a dropdown defaulting to "None"
    And the dropdown options are: None, Signing Secret, API Key, Basic Auth, Bearer Token

  Scenario: Selecting API Key shows credential fields
    When I select "API Key" from the auth type dropdown
    Then I see a "Header Name" field (default value: "X-API-Key")
    And I see an "API Key" text field (required)
    And the "Header Name" field is editable

  Scenario: Creating a webhook with API Key auth
    When I fill in the webhook URL, events, and API Key credential fields
    And I click "Create Webhook"
    Then POST /v1/webhooks is called with { "authType": "API_KEY", "authConfig": { "headerName": "X-API-Key", "apiKey": "<value>" } }
    And the response shows the webhook with "authType": "API_KEY"
    And the "apiKey" value is masked in the response as "[REDACTED]"

  Scenario: Outbound delivery includes API Key header
    When a rule lifecycle event triggers webhook delivery
    Then the HTTP POST to the target URL includes the header "X-API-Key: <stored-key>"
    And no Authorization header is added

  Scenario: Editing auth config
    Given an existing webhook with authType API_KEY
    When I navigate to the webhook Settings tab
    And I update the API Key value and click Save
    Then the new encrypted key replaces the previous one
    And a success toast "Webhook updated" is shown

  Scenario: VIEWER cannot modify auth config
    Given I am logged in as VIEWER
    When I navigate to the webhook Settings tab
    Then the auth credential fields are disabled (read-only)
    And the Save button is hidden or disabled
```

---

### US-2 (P0): Basic Auth Outbound

**As an** organization owner or editor,
**I want to** configure Basic Auth (username + password) as webhook authentication,
**So that** RuleForge can deliver events to legacy APIs and SIEM platforms that use HTTP Basic Auth.

**RICE Score:** 288

#### Acceptance Criteria

```gherkin
Feature: Basic Auth webhook authentication

  Background:
    Given I am logged in as OWNER or EDITOR

  Scenario: Selecting Basic Auth shows credential fields
    When I select "Basic Auth" from the auth type dropdown
    Then I see "Username" field (required)
    And I see "Password" field (required, masked input type)

  Scenario: Creating a webhook with Basic Auth
    When I submit the form with username "webhook_user" and password "s3cr3t"
    Then POST /v1/webhooks is called with { "authType": "BASIC_AUTH", "authConfig": { "username": "webhook_user", "password": "s3cr3t" } }
    And the response masks password as "[REDACTED]"

  Scenario: Outbound delivery includes Authorization header
    When delivery occurs
    Then the HTTP POST includes "Authorization: Basic <base64(username:password)>"

  Scenario: Credentials are encrypted at rest
    Given the webhook is created with Basic Auth credentials
    When querying the database directly
    Then the "auth_config" column contains encrypted ciphertext, not plaintext credentials
```

---

### US-3 (P0): Bearer Token Outbound

**As an** organization owner or editor,
**I want to** configure a static Bearer Token as webhook authentication,
**So that** RuleForge can deliver events to APIs secured by OAuth 2.0 access tokens or API gateway bearer tokens.

**RICE Score:** 364

#### Acceptance Criteria

```gherkin
Feature: Bearer Token webhook authentication

  Scenario: Selecting Bearer Token shows token field
    When I select "Bearer Token" from the auth type dropdown
    Then I see a "Token" field (required, masked)

  Scenario: Creating webhook with Bearer Token
    When I submit with a bearer token
    Then POST /v1/webhooks includes { "authType": "BEARER_TOKEN", "authConfig": { "token": "<value>" } }

  Scenario: Outbound delivery
    When delivery occurs
    Then HTTP POST includes "Authorization: Bearer <token>"

  Scenario: Token masked in GET response
    When I call GET /v1/webhooks/{id}
    Then the response contains "authType": "BEARER_TOKEN"
    And "token" is "[REDACTED]" or only last 4 chars shown

  Scenario: Rotating the token
    When I navigate to Settings tab and enter a new token value
    And click Save
    Then the old token is replaced with the new encrypted value
    And an AuditEvent is recorded with event type "WEBHOOK_CREDENTIAL_ROTATED"
```

---

### US-4 (P0 — Security Enabler): Credential Encryption at Rest

**As an** organization owner,
**I want** all webhook authentication credentials stored encrypted in the database,
**So that** a database breach does not expose API keys, passwords, or tokens in plaintext.

**RICE Score:** 300

#### Acceptance Criteria

```gherkin
Feature: Webhook credential encryption

  Scenario: New webhook credentials are encrypted
    When any webhook with auth credentials is created or updated
    Then the "auth_config" JSONB column in PostgreSQL contains AES-256 encrypted ciphertext
    And the plaintext credential values are never stored

  Scenario: Delivery service decrypts at runtime
    When OutboxPoller processes a pending delivery
    Then credentials are decrypted in memory before injection into the HTTP request
    And decrypted values are not written to any log

  Scenario: Existing signing secrets migrated
    Given webhooks created before this epic with plaintext "secret" values
    When the new Flyway migration runs
    Then all existing "secret" values are encrypted in place
    And webhook delivery continues to function without re-configuration

  Scenario: GET API never returns plaintext
    When any GET /v1/webhooks or GET /v1/webhooks/{id} is called
    Then credential fields in the response are "[REDACTED]" or masked
    And no credential value appears in the response body

  Scenario: Audit events do not contain credentials
    When a webhook is created, updated, or credential is rotated
    Then the AuditEvent payload stored in the audit trail does not contain any credential value
```

---

### US-5 (P0 — Backward Compat): Signing Secret in New Form Model

**As an** organization owner,
**I want** existing webhooks with signing secrets to continue working after the auth type migration,
**So that** no re-configuration is required for webhooks already in production.

**RICE Score:** 720

#### Acceptance Criteria

```gherkin
Feature: Backward compatibility for Signing Secret

  Scenario: Existing webhooks retain SIGNING_SECRET auth type
    Given a webhook created before this epic with a "secret" value
    When the Flyway migration runs
    Then the webhook's authType is set to "SIGNING_SECRET"
    And the secret value is encrypted and stored in auth_config
    And X-RuleForge-Signature header continues to be injected on delivery

  Scenario: New webhooks can select Signing Secret
    When I select "Signing Secret" from the auth type dropdown
    Then I see a "Secret" field
    And the X-RuleForge-Signature header behavior is preserved

  Scenario: Mixed org — some SIGNING_SECRET, some API_KEY
    Given an org with two webhooks: one SIGNING_SECRET, one API_KEY
    When both trigger on the same rule lifecycle event
    Then each receives its appropriate auth header independently

  Scenario: POST /v1/webhooks backward compat
    When a client submits POST /v1/webhooks with only "secret" field and no "authType"
    Then the system infers authType = "SIGNING_SECRET"
    And the webhook is created successfully
    And no breaking change is surfaced to older API clients
```

---

### US-6 (P1): Configurable Backoff Retry Parameters

**As an** organization owner,
**I want to** configure retry behaviour per webhook (initial delay, multiplier, max attempts, retry status codes),
**So that** I can tune delivery reliability for webhooks targeting systems with different availability characteristics.

**RICE Score:** 112

#### Acceptance Criteria

```gherkin
Feature: Configurable webhook retry parameters

  Background:
    Given I am logged in as OWNER
    And I am editing or creating a webhook

  Scenario: Advanced retry settings section is visible
    When I expand the "Advanced" section on the webhook form
    Then I see fields:
      | Field                  | Default | Range         |
      | Initial Delay (sec)    | 30      | 1–3600        |
      | Backoff Multiplier     | 2.0     | 1.0–10.0      |
      | Max Attempts           | 5       | 1–25          |
      | Retry on Status Codes  | 5xx, 408, 429 | multi-select |

  Scenario: Saving retry config
    When I set Max Attempts to 10 and Initial Delay to 60
    And save the webhook
    Then PUT /v1/webhooks/{id} includes { "retryConfig": { "initialDelaySeconds": 60, "maxAttempts": 10, "backoffMultiplier": 2.0 } }
    And the OutboxPoller uses these values for future delivery attempts

  Scenario: Validation
    When I enter Initial Delay = 0 or Max Attempts = 26
    Then a validation error is shown before form submission

  Scenario: Default values used when not configured
    Given a webhook with no explicit retryConfig
    When delivery fails
    Then the system uses org-level defaults (30s initial delay, 2x multiplier, 5 max attempts)

  Scenario: Retry schedule visible in Delivery Logs tab
    Given a delivery is in PENDING retry state
    When I view the Delivery Logs tab
    Then I see "Next retry at: [timestamp]" for in-flight retries
```

---

### US-7 (P1): Delivery Metrics Panel

**As a** DevOps engineer or organization owner,
**I want to** see delivery success rates, failure counts, and DLQ depth for each webhook,
**So that** I can proactively identify unhealthy webhooks and triage delivery failures.

**RICE Score:** 98

#### Acceptance Criteria

```gherkin
Feature: Webhook delivery metrics

  Scenario: Metrics tab visible in WebhookDetailPage
    When I navigate to a webhook's detail page
    Then I see a "Metrics" tab (alongside Settings, Security, Delivery Logs, Events)

  Scenario: Metrics panel shows key indicators
    When I click the "Metrics" tab
    Then I see:
      | Metric                          | Display        |
      | 7-day success rate              | e.g., "94.3%"  |
      | Total deliveries (7d)           | e.g., "256"    |
      | Failed deliveries (7d)          | e.g., "15"     |
      | Current DLQ depth               | e.g., "3"      |
      | Average delivery latency (7d)   | e.g., "142ms"  |

  Scenario: Delivery timeline chart
    When I view the Metrics tab
    Then a bar chart shows per-day delivery counts (success vs failure) for the last 7 days

  Scenario: WebhookListPage health badges
    When I view the WebhookListPage
    Then each webhook row shows a health indicator:
      | Condition                         | Badge  |
      | 7d success rate >= 95%            | Green  |
      | 7d success rate 80–95%            | Amber  |
      | 7d success rate < 80% or DLQ > 0  | Red    |

  Scenario: Metrics API
    When I call GET /v1/webhooks/{id}/metrics
    Then the response contains:
      {
        "successRate7d": 94.3,
        "totalDeliveries7d": 256,
        "failedDeliveries7d": 15,
        "dlqDepth": 3,
        "avgLatencyMs": 142,
        "deliveryTimeline": [ { "date": "2026-03-12", "success": 38, "failed": 2 }, ... ]
      }

  Scenario: Empty state
    Given a webhook with no deliveries yet
    When I view the Metrics tab
    Then I see "No delivery data yet. Metrics will appear after the first delivery."
```

---

### US-8 (P0): NONE Auth Type

**As an** organization owner or editor,
**I want to** explicitly select "None" as the authentication type for a webhook,
**So that** I can direct events to public or internally-accessible endpoints that do not require authentication.

**RICE Score:** 480

#### Acceptance Criteria

```gherkin
Feature: No authentication webhook

  Scenario: Default auth type is NONE
    When I open the webhook creation form
    Then the auth type dropdown defaults to "None"

  Scenario: NONE auth type creates webhook with no credentials
    When I submit the webhook form with auth type "None"
    Then POST /v1/webhooks includes { "authType": "NONE" }
    And no authConfig credentials are stored

  Scenario: Outbound delivery with NONE
    When delivery occurs for a NONE auth webhook
    Then no Authorization header is added to the HTTP request
    And no X-RuleForge-Signature header is added (unless explicitly chosen)

  Scenario: Switching from auth type to NONE clears credentials
    Given an existing webhook with API_KEY auth
    When I change auth type to NONE and save
    Then the stored auth_config credentials are cleared/deleted
    And future deliveries include no auth header
```

---

## 5. Non-Functional Requirements

| Requirement | Description | Acceptance Criteria |
|---|---|---|
| **Credential encryption** | AES-256-GCM, key from `ruleforge.webhook.credential-key` in application.yml | No plaintext in DB; verified by DB query test |
| **Credential masking** | All GET responses mask credential values as `[REDACTED]` | Zero plaintext in API response assertions |
| **Audit trail** | Auth type changes and rotations create `AuditEvent` records | Integration test verifies AuditEvent creation |
| **RBAC enforcement** | VIEWER role cannot modify auth config; EDITOR can configure; OWNER can rotate credentials | Role-based API tests |
| **Backward compatibility** | Existing webhooks with `secret` field continue working post-migration | Smoke test Section 3.20 — backward compat suite |
| **Delivery latency** | Auth header injection adds <5ms to delivery; no sync HTTP calls during delivery except token fetch | Load test p95 ≤ 500ms end-to-end |
| **Build gates** | `./gradlew build` passes; `npm run build` 0 TypeScript errors | CI gates |

---

## 6. Out of Scope (v2)

| Feature | Reason for Deferral |
|---|---|
| OAuth 2.0 client credentials (token endpoint, refresh, Redis caching) | Significant complexity; token refresh races, key expiry management, provider-specific URL formats. Separate story in v2. |
| Custom Header auth type | Niche use case; covers <5% of integrations. Low RICE score. |
| AES encryption key rotation / KMS integration | Operational complexity deferred; application.yml key sufficient for MVP. |
| Webhook auth metrics dashboard (org-wide) | Per-webhook metrics sufficient for MVP; org-wide rollup deferred. |

---

## 7. Dependencies

| Dependency | Type | Required By |
|---|---|---|
| `WebhookEntity` schema migration (Flyway V050+) | Backend | US-1, US-2, US-3, US-4, US-5 |
| `CredentialEncryptionService` (new, modeled after connector-service) | Backend | US-4 (all auth types depend on this) |
| `WebhookAuthInjector` interceptor in `OutboxPoller` / `WebhookDeliveryService` | Backend | US-1, US-2, US-3 (delivery) |
| `WebhookDto` / `WebhookRequest` DTO updates with `authType` + `authConfig` | Backend | All US |
| Frontend `CreateWebhookModal` or `WebhookSettingsTab` auth type selector | Frontend | US-1, US-2, US-3, US-8 |
| React Query cache invalidation on auth config update | Frontend | US-1–US-5 (stale form prevention) |
| `GET /v1/webhooks/{id}/metrics` endpoint in event-service | Backend | US-7 |
| `webhook_deliveries` table aggregate query or `webhook_metrics` view | Database | US-7 |

---

## 8. Release Roadmap

### MVP (This Epic — Sprint 5.1 + 5.2)
- US-8: NONE auth type (1 point)
- US-5: Signing Secret backward compat in new model (1 point)
- US-4: Credential encryption (3 points)
- US-1: API Key auth (3 points)
- US-2: Basic Auth (2 points)
- US-3: Bearer Token (2 points)

**Sprint 5.1 total: 12 points**

### v1 Extension (Sprint 5.3)
- US-6: Configurable retry (3 points)
- US-7: Delivery metrics panel (4 points)

**Sprint 5.3 total: 7 points**

### v2 (Future Epic)
- US-9: Custom Header auth (2 points)
- US-10: OAuth 2.0 client credentials (8 points)

---

## 9. Success Metrics

| Metric | Target | Measurement |
|---|---|---|
| Auth types functional | 5/5 MVP types (NONE, SIGNING_SECRET, API_KEY, BASIC_AUTH, BEARER_TOKEN) | Smoke test Section 3.20 |
| Zero credential plaintext in DB | 0 plaintext rows after migration | DB query assertion in integration test |
| Zero credential exposure in API | 0 plaintext in any GET /webhooks response | API contract test |
| Backward compatibility | 100% existing webhooks deliver post-migration | Smoke test regression suite |
| Frontend TypeScript errors | 0 | `npm run build` output |
| Backend build | SUCCESS | `./gradlew build` output |
| Delivery latency with auth | p95 ≤ 500ms | Manual load test (50 concurrent deliveries) |

---

**Gate Status:** PASS
**Product Owner Sign-off:** 2026-03-18
