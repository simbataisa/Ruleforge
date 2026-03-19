# Story 025 — API Contract: OAuth 2.0 & Custom Header

**Story ID:** story-025
**Epic:** Webhook Auth v2 — OAuth 2.0 & Custom Header (Epic 6)
**Prepared by:** Tech Lead
**Date:** 2026-03-19
**Assigned to:** Backend Engineer
**Priority:** P0
**Estimate:** 1 story point
**Depends on:** Story 023 (`OAuth2ClientCredentials` record must exist in sealed interface before DTO update)

---

## User Story

**As a** frontend engineer and integration developer,
**I want** the create/update/get webhook API to accept and return OAuth 2.0 and Custom Header auth config fields,
**So that** I can configure these auth types via the API and the frontend form without needing to send raw JSON.

---

## Context

Epic 5 (Story 017) established the API contract for `authType` + `authConfig` fields. DTOs `WebhookAuthConfigRequest` and `WebhookAuthConfigDto` need two additions:
- Custom Header: `headerValue` is new (distinct from `apiKey` — `headerName` field already exists)
- OAuth 2.0: `tokenUrl`, `clientId`, `clientSecret`, `scope` are all new

`WebhookController.createWebhook()` and `updateWebhook()` must validate `tokenUrl` (HTTPS + SSRF) before persisting.

When auth config is updated to/from OAuth 2.0, `OAuth2TokenCacheService.evict()` must be called to flush any stale cached token.

---

## Acceptance Criteria

1. `POST /v1/webhooks` accepts `authType: "CUSTOM_HEADER"` with `headerName` + `headerValue` fields — returns 201 with `authType: "CUSTOM_HEADER"` and `headerValue: "[REDACTED]"`.
2. `POST /v1/webhooks` accepts `authType: "OAUTH2_CLIENT_CREDENTIALS"` with `tokenUrl`, `clientId`, `clientSecret`, `scope` — returns 201 with `clientSecret: "[REDACTED]"`.
3. `GET /v1/webhooks/{id}` response: `clientSecret`, `headerValue`, and all other credential fields masked as `"[REDACTED]"`.
4. `PUT /v1/webhooks/{id}` (or equivalent update endpoint) with new `clientSecret` or `headerValue` encrypts and persists the new value via `CredentialEncryptionService`.
5. On auth config update for `OAUTH2_CLIENT_CREDENTIALS`, `OAuth2TokenCacheService.evict(orgId, webhookId)` is called.
6. `tokenUrl` must start with `https://` — `POST`/`PUT` with `http://` returns 400 with `"tokenUrl must use HTTPS"`.
7. `tokenUrl` must pass `SsrfGuard.check()` — blocked URL returns 400 with `"tokenUrl resolves to a disallowed address"`.
8. `clientId` is required when `authType` is `OAUTH2_CLIENT_CREDENTIALS` — missing returns 400.
9. `clientSecret` is required on create when `authType` is `OAUTH2_CLIENT_CREDENTIALS` — missing returns 400.
10. Existing Epic 5 auth types (API_KEY, BASIC_AUTH, BEARER_TOKEN, SIGNING_SECRET, NONE) work without regression.

---

## Files to Modify

### `platform/ruleforge-event-service/src/main/java/io/ruleforge/event/dto/WebhookAuthConfigRequest.java`

Add new fields:

```java
public record WebhookAuthConfigRequest(
    WebhookAuthType authType,
    // API_KEY + CUSTOM_HEADER
    String headerName,
    // API_KEY
    String apiKey,
    // CUSTOM_HEADER
    String headerValue,          // NEW
    // BASIC_AUTH
    String username,
    String password,
    // BEARER_TOKEN
    String token,
    // SIGNING_SECRET
    String secret,
    // OAUTH2_CLIENT_CREDENTIALS (all NEW)
    String tokenUrl,
    String clientId,
    String clientSecret,
    String scope                  // nullable
) {}
```

### `platform/ruleforge-event-service/src/main/java/io/ruleforge/event/dto/WebhookAuthConfigDto.java`

Same fields as request. Masking applied by `WebhookService.toDto()`:

```java
// In toDto() or the DTO builder — mask sensitive fields:
private static final String REDACTED = "[REDACTED]";

// Masked fields: apiKey, password, token, secret, headerValue, clientSecret
// Non-masked: headerName, username, tokenUrl, clientId, scope
```

### `platform/ruleforge-event-service/src/main/java/io/ruleforge/event/service/WebhookService.java`

**`toAuthConfig()` mapping** (Request → `WebhookAuthConfig` sealed record):

```java
case CUSTOM_HEADER -> new WebhookAuthConfig.CustomHeader(
    request.headerName(), request.headerValue());

case OAUTH2_CLIENT_CREDENTIALS -> new WebhookAuthConfig.OAuth2ClientCredentials(
    request.tokenUrl(), request.clientId(), request.clientSecret(), request.scope());
```

**Auth config update — cache eviction:**

```java
public void updateAuthConfig(UUID orgId, UUID webhookId, WebhookAuthConfigRequest request) {
    // ... existing validation and encryption ...
    webhookRepository.save(updated);
    // Evict OAuth2 token cache on any auth config change
    oauth2TokenCacheService.evict(orgId, webhookId);
}
```

### `platform/ruleforge-event-service/src/main/java/io/ruleforge/event/controller/WebhookController.java`

**Validation for `OAUTH2_CLIENT_CREDENTIALS`:**

```java
private void validateAuthConfig(WebhookAuthConfigRequest req) {
    if (req.authType() == WebhookAuthType.OAUTH2_CLIENT_CREDENTIALS) {
        if (req.tokenUrl() == null || !req.tokenUrl().startsWith("https://")) {
            throw new ValidationException("tokenUrl must use HTTPS");
        }
        try {
            SsrfGuard.check(req.tokenUrl());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("tokenUrl resolves to a disallowed address");
        }
        if (req.clientId() == null || req.clientId().isBlank()) {
            throw new ValidationException("clientId is required for OAuth2 auth type");
        }
    }
}
```

Call `validateAuthConfig(request.authConfig())` in both `createWebhook` and `updateWebhook`.

---

## Integration Test Cases

Add to existing `WebhookControllerIntegrationTest` (or create `WebhookAuthContractTest`):

```java
@Test void createWebhook_customHeader_returns201_masksHeaderValue()
@Test void createWebhook_oauth2_returns201_masksClientSecret()
@Test void getWebhook_oauth2_showsTokenUrlAndClientId_masksClientSecret()
@Test void createWebhook_oauth2_httpTokenUrl_returns400()
@Test void createWebhook_oauth2_ssrfTokenUrl_returns400()
@Test void createWebhook_oauth2_missingClientId_returns400()
@Test void updateAuthConfig_oauth2_evictsRedisTokenCache()
```

---

## Definition of Done

- [ ] `WebhookAuthConfigRequest` has `tokenUrl`, `clientId`, `clientSecret`, `scope`, `headerValue` fields
- [ ] `WebhookAuthConfigDto` has same fields with credential masking
- [ ] `WebhookService.toAuthConfig()` maps new auth types to sealed records
- [ ] `WebhookController` validates `tokenUrl` (HTTPS + SSRF) on create/update
- [ ] `WebhookService.updateAuthConfig()` calls `OAuth2TokenCacheService.evict()` on any auth config update
- [ ] All integration tests pass
- [ ] Existing Epic 5 auth type smoke test cases still pass
- [ ] `cd platform && ./gradlew :ruleforge-event-service:test` green
