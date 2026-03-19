# Story 016 — WebhookAuthInjector Strategy + Delivery Integration

**Story ID:** story-016
**Epic:** Webhooks Authentication Enhancement (Epic 5)
**Prepared by:** Tech Lead
**Date:** 2026-03-18
**Assigned to:** Backend Engineer
**Priority:** P0 — Sprint 5.1
**Estimate:** 3 story points
**Depends on:** Story 015 (`CredentialEncryptionService` + `WebhookAuthConfig` must exist)

---

## User Story

**As a** webhook subscriber using a secured API,
**I want** RuleForge to inject the appropriate `Authorization` or custom header into every outbound delivery request based on the webhook's configured auth type,
**So that** my API accepts the delivery instead of rejecting it with 401/403.

---

## Background

ADR-008 decided: Strategy Pattern. Each auth type has its own `WebhookAuthInjector` implementation, auto-discovered by Spring. `WebhookDeliveryService` calls `registry.getFor(authType).injectAuth(headers, config, payload)` — no `switch` or `if-else`.

Existing `SigningSecretInjector` logic (HMAC-SHA256 `X-RuleForge-Signature`) is extracted from wherever it currently lives in `WebhookDeliveryService` into `SigningSecretInjector` to keep it consistent with the new pattern.

---

## Acceptance Criteria

1. `WebhookAuthInjector` interface exists with `supportedType()` and `injectAuth(HttpHeaders, WebhookAuthConfig, byte[])`.
2. Five injector implementations exist: `NoneAuthInjector`, `SigningSecretInjector`, `ApiKeyAuthInjector`, `BasicAuthInjector`, `BearerTokenInjector`.
3. `WebhookAuthInjectorRegistry` is a Spring `@Component` that maps `WebhookAuthType` → `WebhookAuthInjector` via constructor injection of `List<WebhookAuthInjector>`.
4. `WebhookDeliveryService` calls the registry to get the injector and calls `injectAuth()` before sending the HTTP request.
5. Injected headers per type:
   - `NONE`: no auth header added
   - `SIGNING_SECRET`: `X-RuleForge-Signature: sha256=<hmac>` (existing behaviour preserved)
   - `API_KEY`: `<headerName>: <apiKey>` (headerName defaults to `X-API-Key` if null/blank)
   - `BASIC_AUTH`: `Authorization: Basic <base64(username:password)>`
   - `BEARER_TOKEN`: `Authorization: Bearer <token>`
6. `CredentialEncryptionService.decrypt()` is called inside `WebhookDeliveryService` (not inside injectors) — injectors receive the already-decrypted `WebhookAuthConfig`.
7. Decrypted credentials are **not** stored in any variable with a scope wider than the delivery method.
8. If `decrypt()` throws `WebhookCredentialDecryptionException`, delivery is marked `FAILED` with reason `"CREDENTIAL_DECRYPT_ERROR"` — **not** retried (no point retrying a bad key).
9. Unit tests: one test per injector verifying the correct header is set for valid input, and that no header is set for `NoneAuthInjector`.
10. `./gradlew :ruleforge-event-service:test` passes.

---

## Files to Create

### `io.ruleforge.event.delivery.WebhookAuthInjector` (interface)

```java
package io.ruleforge.event.delivery;

public interface WebhookAuthInjector {
    WebhookAuthType supportedType();
    void injectAuth(HttpHeaders headers, WebhookAuthConfig config, byte[] payload);
}
```

### `io.ruleforge.event.delivery.NoneAuthInjector`

```java
@Component
public class NoneAuthInjector implements WebhookAuthInjector {
    @Override public WebhookAuthType supportedType() { return WebhookAuthType.NONE; }
    @Override public void injectAuth(HttpHeaders headers, WebhookAuthConfig config, byte[] payload) {
        // intentionally empty — no auth header for NONE type
    }
}
```

### `io.ruleforge.event.delivery.SigningSecretInjector`

```java
@Component
public class SigningSecretInjector implements WebhookAuthInjector {
    @Override public WebhookAuthType supportedType() { return WebhookAuthType.SIGNING_SECRET; }
    @Override public void injectAuth(HttpHeaders headers, WebhookAuthConfig config, byte[] payload) {
        if (config instanceof WebhookAuthConfig.SigningSecret ss) {
            String signature = computeHmacSha256(ss.secret(), payload);
            headers.set("X-RuleForge-Signature", "sha256=" + signature);
        }
    }
    // HMAC-SHA256 logic extracted from existing WebhookDeliveryService
}
```

### `io.ruleforge.event.delivery.ApiKeyAuthInjector`

```java
@Component
public class ApiKeyAuthInjector implements WebhookAuthInjector {
    @Override public WebhookAuthType supportedType() { return WebhookAuthType.API_KEY; }
    @Override public void injectAuth(HttpHeaders headers, WebhookAuthConfig config, byte[] payload) {
        if (config instanceof WebhookAuthConfig.ApiKey ak) {
            String headerName = (ak.headerName() != null && !ak.headerName().isBlank())
                ? ak.headerName() : "X-API-Key";
            headers.set(headerName, ak.apiKey());
        }
    }
}
```

### `io.ruleforge.event.delivery.BasicAuthInjector`

```java
@Component
public class BasicAuthInjector implements WebhookAuthInjector {
    @Override public WebhookAuthType supportedType() { return WebhookAuthType.BASIC_AUTH; }
    @Override public void injectAuth(HttpHeaders headers, WebhookAuthConfig config, byte[] payload) {
        if (config instanceof WebhookAuthConfig.BasicAuth ba) {
            String credentials = ba.username() + ":" + ba.password();
            String encoded = Base64.getEncoder().encodeToString(
                credentials.getBytes(StandardCharsets.UTF_8));
            headers.set(HttpHeaders.AUTHORIZATION, "Basic " + encoded);
        }
    }
}
```

### `io.ruleforge.event.delivery.BearerTokenInjector`

```java
@Component
public class BearerTokenInjector implements WebhookAuthInjector {
    @Override public WebhookAuthType supportedType() { return WebhookAuthType.BEARER_TOKEN; }
    @Override public void injectAuth(HttpHeaders headers, WebhookAuthConfig config, byte[] payload) {
        if (config instanceof WebhookAuthConfig.BearerToken bt) {
            headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + bt.token());
        }
    }
}
```

### `io.ruleforge.event.delivery.WebhookAuthInjectorRegistry`

```java
@Component
public class WebhookAuthInjectorRegistry {
    private final Map<WebhookAuthType, WebhookAuthInjector> injectors;

    public WebhookAuthInjectorRegistry(List<WebhookAuthInjector> injectors) {
        this.injectors = injectors.stream()
            .collect(Collectors.toMap(WebhookAuthInjector::supportedType, Function.identity()));
    }

    public WebhookAuthInjector getFor(WebhookAuthType type) {
        return injectors.getOrDefault(type,
            injectors.getOrDefault(WebhookAuthType.NONE,
                (h, c, p) -> {}  // fallback no-op
            ));
    }
}
```

---

## Files to Modify

### `WebhookDeliveryService.java`

In the delivery method, replace the existing HMAC signing block with the registry call:

```java
// Before building the HTTP request:
WebhookAuthConfig authConfig = resolveAuthConfig(webhook);

// Build headers
HttpHeaders headers = new HttpHeaders();
headers.setContentType(MediaType.APPLICATION_JSON);

// Inject auth via strategy
authInjectorRegistry.getFor(webhook.getAuthType())
    .injectAuth(headers, authConfig, payloadBytes);

// ... proceed with HTTP request
```

New private helper `resolveAuthConfig`:
```java
private WebhookAuthConfig resolveAuthConfig(WebhookEntity webhook) {
    if (webhook.getAuthType() == WebhookAuthType.NONE) {
        return new WebhookAuthConfig.None();
    }
    if (webhook.getAuthConfig() == null) {
        // Legacy: no auth_config yet (before migration ran)
        if (webhook.getSecret() != null) {
            return new WebhookAuthConfig.SigningSecret(webhook.getSecret());
        }
        return new WebhookAuthConfig.None();
    }
    try {
        return encryptionService.decrypt(webhook.getAuthConfig(), webhook.getAuthType());
    } catch (WebhookCredentialDecryptionException e) {
        log.error("Cannot decrypt credentials for webhook {} — marking delivery FAILED", webhook.getId());
        throw e;  // OutboxPoller catches this and sets status=FAILED, no retry
    }
}
```

---

## Definition of Done

- [ ] `WebhookAuthInjector` interface in `io.ruleforge.event.delivery`
- [ ] 5 injectors: `None`, `SigningSecret`, `ApiKey`, `BasicAuth`, `BearerToken` — all `@Component`
- [ ] `WebhookAuthInjectorRegistry` auto-discovers injectors via constructor injection
- [ ] `WebhookDeliveryService` uses registry; old HMAC inline block removed
- [ ] `resolveAuthConfig()` handles: new encrypted path, legacy `secret` fallback, decrypt failure
- [ ] Decrypt failure marks delivery FAILED (no retry)
- [ ] Decrypted credentials do not persist beyond delivery method scope
- [ ] Unit tests per injector (correct header injected)
- [ ] `./gradlew :ruleforge-event-service:test` passes
- [ ] Existing smoke test webhook deliveries still pass (backward compat)
