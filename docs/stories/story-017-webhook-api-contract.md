# Story 017 — Webhook API Contract: authType + authConfig + retryConfig

**Story ID:** story-017
**Epic:** Webhooks Authentication Enhancement (Epic 5)
**Prepared by:** Tech Lead
**Date:** 2026-03-18
**Assigned to:** Backend Engineer
**Priority:** P0 — Sprint 5.1
**Estimate:** 2 story points
**Depends on:** Story 015 (`WebhookAuthConfig` model), Story 014 (entity fields)

---

## User Story

**As a** frontend developer or API consumer,
**I want** `POST /v1/webhooks` and `PUT /v1/webhooks/{id}` to accept `authType`, `authConfig`, and `retryConfig` fields,
**And** `GET /v1/webhooks` / `GET /v1/webhooks/{id}` to return them with credentials masked,
**So that** I can create and view webhooks with full auth configuration via the API.

---

## Background

The current `WebhookRequest` DTO and `WebhookDto` have no `authType`, `authConfig`, or `retryConfig` fields. This story wires the API layer — request deserialization, service mapping, response serialization with credential masking.

Key constraint: backward compatibility. A request with only `"secret": "xyz"` (old client) must still work; the service infers `authType = SIGNING_SECRET`.

---

## Acceptance Criteria

1. `WebhookRequest` DTO accepts: `authType` (optional, defaults to `NONE`), `authConfig` (optional, type-discriminated object), `retryConfig` (optional).
2. If `authType` is absent and `secret` is present, service maps to `authType = SIGNING_SECRET` with `authConfig.secret = <value>`.
3. If both `authType` and `secret` are absent, `authType = NONE`, no credentials stored.
4. `WebhookService.createWebhook()` and `updateWebhook()` call `CredentialEncryptionService.encrypt(authConfig)` before persisting, when `authConfig` is non-null.
5. `WebhookDto` includes `authType` (String) and `authConfig` (object with credential fields **masked**).
6. Masking rules:
   - `apiKey` → `"[REDACTED]"`
   - `password` → `"[REDACTED]"`
   - `token` → `"[REDACTED]"`
   - `secret` → `"[REDACTED]"`
   - `headerValue` → `"[REDACTED]"`
   - `headerName` → **not masked** (not sensitive)
   - `username` → **not masked** (not sensitive)
7. `GET /v1/webhooks/{id}` returns `retryConfig` with current values (or defaults if not set).
8. `PATCH /v1/webhooks/{id}` (partial update) for auth config: if credential field is blank/null in request, keep existing encrypted value unchanged.
9. `400 Bad Request` if `authType = API_KEY` but `authConfig.apiKey` is null/blank.
10. `400 Bad Request` if `authType = BASIC_AUTH` but `username` or `password` is null/blank.
11. `400 Bad Request` if `authType = BEARER_TOKEN` but `token` is null/blank.
12. VIEWER role: `GET /v1/webhooks/{id}` returns `authType` but `authConfig` is entirely omitted from response (not just masked).
13. `./gradlew :ruleforge-event-service:test` passes with new DTO tests.

---

## Files to Create / Modify

### `WebhookRequest.java` (modify)

Add fields:
```java
@JsonProperty("authType")
private WebhookAuthType authType;  // null = infer from 'secret' field

@JsonProperty("authConfig")
private WebhookAuthConfigRequest authConfig;  // type-specific credential input

@JsonProperty("retryConfig")
private WebhookRetryConfigRequest retryConfig;  // optional retry overrides
```

### `WebhookAuthConfigRequest.java` (new DTO)

```java
public class WebhookAuthConfigRequest {
    private String headerName;   // API_KEY / CUSTOM_HEADER
    private String apiKey;       // API_KEY
    private String username;     // BASIC_AUTH
    private String password;     // BASIC_AUTH
    private String token;        // BEARER_TOKEN
    private String headerValue;  // CUSTOM_HEADER
    private String secret;       // SIGNING_SECRET
}
```

### `WebhookRetryConfigRequest.java` (new DTO)

```java
public class WebhookRetryConfigRequest {
    @Min(1) @Max(3600)
    private Integer initialDelaySeconds;
    @DecimalMin("1.0") @DecimalMax("10.0")
    private BigDecimal backoffMultiplier;
    @Min(1) @Max(25)
    private Integer maxAttempts;
    private List<Integer> retryOnStatusCodes;
}
```

### `WebhookDto.java` (modify)

Add fields with masking:
```java
@JsonProperty("authType")
private String authType;

@JsonProperty("authConfig")
@JsonSerialize(using = WebhookAuthConfigMaskingSerializer.class)
private WebhookAuthConfigDto authConfig;  // null for VIEWER role

@JsonProperty("retryConfig")
private WebhookRetryConfigDto retryConfig;
```

### `WebhookAuthConfigMaskingSerializer.java` (new)

Custom `JsonSerializer<WebhookAuthConfigDto>` that replaces credential fields with `"[REDACTED]"`. Applied via `@JsonSerialize` on `WebhookDto.authConfig`.

### `WebhookService.java` (modify)

In `createWebhook()` and `updateWebhook()`:
```java
// Infer authType from legacy 'secret' field for backward compat
WebhookAuthType authType = resolveAuthType(request);
WebhookAuthConfig authConfig = buildAuthConfig(request, authType);

if (authConfig != null && !(authConfig instanceof WebhookAuthConfig.None)) {
    entity.setAuthConfig(credentialEncryptionService.encrypt(authConfig));
}
entity.setAuthType(authType);

// Apply retryConfig if present
if (request.getRetryConfig() != null) {
    applyRetryConfig(entity, request.getRetryConfig());
}
```

Validation method `validateAuthConfig(WebhookAuthType, WebhookAuthConfigRequest)`:
- Throws `ValidationException` if required credential field missing for given auth type.

### `WebhookController.java` — VIEWER masking

```java
// In GET endpoint, after fetching dto:
if (!currentUserHasRole(OWNER, EDITOR)) {
    dto.setAuthConfig(null);  // VIEWER sees authType only
}
```

---

## Definition of Done

- [ ] `WebhookRequest` accepts `authType`, `authConfig`, `retryConfig`
- [ ] Backward compat: `secret`-only request maps to `SIGNING_SECRET`
- [ ] `WebhookService` encrypts credentials before persist
- [ ] Validation rejects missing required credential fields with 400
- [ ] `WebhookDto` returns `authType` and masked `authConfig`
- [ ] VIEWER receives `authConfig: null` in response
- [ ] `retryConfig` values round-trip through API
- [ ] Unit tests: creation with each auth type, masking assertions, VIEWER masking, backward compat
- [ ] `./gradlew :ruleforge-event-service:test` passes
