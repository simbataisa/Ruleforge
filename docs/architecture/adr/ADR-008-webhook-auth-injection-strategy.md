# ADR-008: Webhook Auth Injection — Strategy Pattern

**Date:** 2026-03-18
**Status:** Accepted
**Author:** Solution Architect (BMAD Epic 5)

---

## Context

The `WebhookDeliveryService` currently builds outbound HTTP requests using Spring's `RestTemplate`. It has a single code path: if `webhook.secret` is set, add `X-RuleForge-Signature` header.

With 5+ auth types to support (NONE, SIGNING_SECRET, API_KEY, BASIC_AUTH, BEARER_TOKEN, CUSTOM_HEADER), the delivery logic must inject different headers per auth type. The question is how to structure this without a large `switch` statement or `if-else` chain in the delivery hot path, and how to make adding new auth types (e.g., OAuth 2.0 in v2) require minimal changes.

---

## Decision

Implement the **Strategy Pattern** for auth injection:

1. Define `WebhookAuthInjector` interface with `supportedType()` and `injectAuth()` methods
2. Implement one class per auth type: `NoneAuthInjector`, `SigningSecretInjector`, `ApiKeyAuthInjector`, `BasicAuthInjector`, `BearerTokenInjector`, `CustomHeaderInjector`
3. Register all injectors in `WebhookAuthInjectorRegistry` (Spring `@Component`, populated via `List<WebhookAuthInjector>` constructor injection)
4. `WebhookDeliveryService` calls `registry.getFor(webhook.authType).injectAuth(headers, decryptedConfig, payload)`

Each injector is a Spring `@Component` and is automatically discovered and registered.

---

## Rationale

1. **Open/Closed Principle**: Adding OAuth 2.0 in v2 requires creating `OAuth2AuthInjector` and annotating it `@Component`. Zero changes to `WebhookDeliveryService` or the registry.

2. **Single Responsibility**: Each injector handles exactly one auth scheme. `BasicAuthInjector` only knows about `Authorization: Basic base64(user:pass)`. It doesn't care about API keys.

3. **Testability**: Each injector is independently unit-testable with no delivery infrastructure required. `ApiKeyAuthInjector` test just calls `injectAuth()` and asserts the resulting `HttpHeaders`.

4. **No reflection or annotation magic**: Strategy lookup is a map lookup — O(1), deterministic, debuggable.

5. **Consistent interface for future OAuth complexity**: When OAuth 2.0 is added, `OAuth2AuthInjector` can internally obtain/cache tokens from Redis, all hidden behind the same `injectAuth()` interface. `WebhookDeliveryService` is unchanged.

---

## Interface Design

```java
package io.ruleforge.event.delivery;

public interface WebhookAuthInjector {
    WebhookAuthType supportedType();

    /**
     * Injects authentication headers into the outbound HTTP request.
     *
     * @param headers  mutable HttpHeaders to add auth to
     * @param config   decrypted auth config (never null; use WebhookAuthConfig.None for NONE type)
     * @param payload  raw request body bytes (needed for HMAC signing only)
     */
    void injectAuth(HttpHeaders headers, WebhookAuthConfig config, byte[] payload);
}
```

**Example implementation — ApiKeyAuthInjector:**

```java
@Component
public class ApiKeyAuthInjector implements WebhookAuthInjector {

    @Override
    public WebhookAuthType supportedType() { return WebhookAuthType.API_KEY; }

    @Override
    public void injectAuth(HttpHeaders headers, WebhookAuthConfig config, byte[] payload) {
        if (config instanceof WebhookAuthConfig.ApiKey apiKey) {
            String headerName = apiKey.headerName() != null ? apiKey.headerName() : "X-API-Key";
            headers.set(headerName, apiKey.apiKey());
        }
    }
}
```

---

## Consequences

**Positive:**
- Clean extension point for OAuth 2.0 in v2
- Easy unit testing per injector
- No `switch` in delivery hot path
- Spring auto-discovers new injectors

**Negative:**
- Slight indirection: tracing a delivery auth path requires knowing which injector handles that type. Mitigated by clear naming convention and `supportedType()` registry dump in DEBUG logs.

---

## Alternatives Considered

### Option 1: Switch statement in WebhookDeliveryService
- **Pros**: Simple, all logic in one place
- **Rejected**: Every new auth type requires modifying `WebhookDeliveryService`. High change risk in delivery hot path. Violates OCP.

### Option 2: Lambda map (auth type → BiConsumer<HttpHeaders, config>)
- **Pros**: Compact; no extra classes
- **Rejected**: Lambdas are not independently testable, not Spring beans, cannot inject dependencies (e.g., OAuth2AuthInjector needs Redis). Insufficient for v2.

### Option 3: Spring HttpRequestInterceptor chain
- **Pros**: Spring-native; could stack interceptors
- **Rejected**: `RestTemplate` interceptors are global, not per-request. Would require webhook context threading. Strategy is simpler.
