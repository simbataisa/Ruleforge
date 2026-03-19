# Solution Architecture — Webhook Auth v2: OAuth 2.0 & Custom Header (Epic 6)

**Date:** 2026-03-19
**Architect:** BMAD Solution Architect Agent
**Input:** `docs/prd-webhooks-oauth2-customheader.md`
**Status:** APPROVED

---

## 1. Overview

Epic 6 adds two previously deferred webhook auth types on top of the Epic 5 strategy-pattern foundation. The architecture is deliberately additive — no existing classes change behaviour, no DB migrations run. The changes fall into four zones:

| Zone | Change |
|---|---|
| **Sealed interface** | Add `OAuth2ClientCredentials` record to `WebhookAuthConfig` |
| **Injectors** | Add `CustomHeaderInjector` + `OAuth2ClientCredentialsInjector` (both Spring `@Component` — auto-registered by `WebhookAuthInjectorRegistry`) |
| **Token cache** | `OAuth2TokenCacheService` wraps `RedisTemplate<String, String>`; key `tenant:{orgId}:webhook:{id}:oauth_token` |
| **Delivery status** | Add `AUTH_FAILURE` to `WebhookDeliveryStatus` enum; circuit breaker excludes it |

No new infrastructure. No DB migrations. No changes to `CredentialEncryptionService`, `WebhookDispatcher` orchestration, or `OutboxPoller`.

---

## 2. Component Model

```
┌──────────────────────────────────────────────────────────────────┐
│  ruleforge-event-service                                          │
│                                                                   │
│  OutboxPoller ──► WebhookDispatcher                              │
│                        │                                         │
│                        ├─ WebhookAuthInjectorRegistry            │
│                        │       ├─ NoneAuthInjector               │
│                        │       ├─ SigningSecretInjector           │
│                        │       ├─ ApiKeyAuthInjector              │
│                        │       ├─ BasicAuthInjector               │
│                        │       ├─ BearerTokenInjector             │
│                        │       ├─ CustomHeaderInjector      [NEW] │
│                        │       └─ OAuth2ClientCredentialsInjector [NEW]
│                        │               │                         │
│                        │               └─ OAuth2TokenCacheService [NEW]
│                        │                       │                 │
│                        │               RedisTemplate<String,String>
│                        │               (existing Redis connection)│
│                        │                                         │
│                        └─ CredentialEncryptionService            │
│                             (existing, unchanged)                │
│                                                                   │
│  WebhookDeliveryStatus: + AUTH_FAILURE              [NEW]        │
│  WebhookAuthConfig sealed interface: + OAuth2ClientCredentials   [NEW]
└──────────────────────────────────────────────────────────────────┘
```

---

## 3. Custom Header Injector

### Design

`CustomHeaderInjector` is the simplest possible implementation — identical in structure to `ApiKeyAuthInjector` but injects `headerName: headerValue` instead of a key in a named header.

By the time `injectAuth` is called, `WebhookDispatcher` has already called `CredentialEncryptionService.decryptAuthConfig(authConfig)` so the `headerValue` arriving at the injector is plaintext.

```java
// io.ruleforge.event.delivery.CustomHeaderInjector
@Component
public class CustomHeaderInjector implements WebhookAuthInjector {

    @Override
    public WebhookAuthType supportedType() { return WebhookAuthType.CUSTOM_HEADER; }

    @Override
    public void injectAuth(HttpHeaders headers, WebhookAuthConfig config, byte[] payload) {
        if (config instanceof WebhookAuthConfig.CustomHeader ch) {
            if (ch.headerValue() == null || ch.headerValue().isBlank()) {
                throw new WebhookAuthException("CUSTOM_HEADER headerValue is blank");
            }
            String name = (ch.headerName() != null && !ch.headerName().isBlank())
                    ? ch.headerName() : "X-Custom-Header";
            headers.set(name, ch.headerValue());
        }
    }
}
```

**Auth failure path:** `WebhookAuthException` thrown by the injector is caught in `WebhookDispatcher`, which sets `DeliveryResult(success=false, permanent=true, errorMessage=..., AUTH_FAILURE status)`.

---

## 4. OAuth 2.0 Client Credentials Injector

### 4.1 Sealed Interface Addition

```java
// WebhookAuthConfig.java — add to sealed interface
@JsonSubTypes.Type(value = WebhookAuthConfig.OAuth2ClientCredentials.class, name = "OAUTH2_CLIENT_CREDENTIALS")

record OAuth2ClientCredentials(
    String tokenUrl,
    String clientId,
    String clientSecret,   // encrypted at rest; decrypted before injection
    String scope           // nullable — omitted from token request if null
) implements WebhookAuthConfig {}
```

### 4.2 Token Cache Design

**Requirement:** Token endpoint called at most once per TTL window across all event-service instances.

**Choice: Redis string with TTL** (see ADR-010 for full rationale)

```
Key:   tenant:{orgId}:webhook:{webhookId}:oauth_token
Value: access_token string (plaintext in Redis — in-flight credential)
TTL:   expires_in - 60  (seconds); minimum 60s
```

**Race condition handling:** Use `RedisTemplate.opsForValue().setIfAbsent(key, token, ttl, SECONDS)` (maps to Redis `SET NX PX`). If two instances simultaneously fetch a token, only one writes; the other uses the now-cached value on its next Redis read. This is benign — both tokens are valid until expiry.

### 4.3 Injector Sequence

```
OAuth2ClientCredentialsInjector.injectAuth(headers, config, payload):

  1. key = "tenant:{orgId}:webhook:{webhookId}:oauth_token"
     (orgId + webhookId passed via enriched config or thread-local TenantContext)

  2. token = redisTemplate.opsForValue().get(key)

  3. if token == null:
       a. SsrfGuard.check(config.tokenUrl())  →  throw WebhookAuthException if blocked
       b. POST {tokenUrl} form body:
             grant_type=client_credentials
             client_id={config.clientId()}
             client_secret={decryptedSecret}     // CredentialEncryptionService already ran
             scope={config.scope()}              // omit if null
       c. if HTTP non-2xx or IOException:
             throw WebhookAuthException("OAuth2 token fetch failed: HTTP {status} from {tokenUrl}")
       d. parse response: { access_token, expires_in, token_type }
       e. ttl = max(expires_in - 60, 60)
       f. redisTemplate.opsForValue().setIfAbsent(key, access_token, ttl, SECONDS)
       g. token = access_token

  4. headers.set("Authorization", "Bearer " + token)
```

### 4.4 Cache Invalidation

Two triggers force cache eviction:

1. **Auth config update** (`PUT /v1/webhooks/{id}/auth-config`): `WebhookService` calls `redisTemplate.delete("tenant:{orgId}:webhook:{id}:oauth_token")` after persisting the updated config.
2. **Manual circuit-breaker reset** (existing): no change needed; stale tokens self-expire via TTL.

### 4.5 Token Fetch HTTP Client

Uses the existing `RestClient webhookClient` bean (already in `WebhookDispatcher`). The injector receives it via constructor injection. This avoids creating a second HTTP client bean.

For the token request, use `RestClient.post().uri(tokenUrl).contentType(APPLICATION_FORM_URLENCODED).body(formParams).retrieve()`. Parse the JSON response with `ObjectMapper`.

---

## 5. AUTH_FAILURE Delivery Status

### Change: `WebhookDeliveryStatus` enum

```java
public enum WebhookDeliveryStatus {
    PENDING, DELIVERING, DELIVERED, FAILED, IN_DLQ, CIRCUIT_OPEN,
    AUTH_FAILURE   // NEW — permanent failure; auth config broken; no retry
}
```

### WebhookDispatcher error handling

`WebhookAuthException` (new unchecked exception) signals that delivery cannot proceed due to auth setup failure:

```java
// In WebhookDispatcher.deliverDirect():
try {
    injector.injectAuth(headers, decryptedConfig, payloadBytes);
} catch (WebhookAuthException e) {
    log.warn("Auth injection failed for webhook {}: {}", webhookId, e.getMessage());
    return new DeliveryResult(false, 0, null, payloadJson, e.getMessage(), /* permanent= */ true);
}
```

`OutboxPoller` already checks `result.permanent()` to skip retry and move directly to DLQ/final status. It must additionally check if the status should be `AUTH_FAILURE` rather than `FAILED`:

```java
// OutboxPoller — determine terminal status
WebhookDeliveryStatus terminalStatus = result.isAuthFailure()
    ? WebhookDeliveryStatus.AUTH_FAILURE
    : WebhookDeliveryStatus.IN_DLQ;
```

`DeliveryResult` gains a boolean `authFailure` field (or the caller can infer from `permanent=true && statusCode==0` but explicit is cleaner).

### Circuit Breaker Exclusion

The circuit breaker in `OutboxPoller` currently trips after N consecutive `FAILED` deliveries. `AUTH_FAILURE` must not count:

```java
// Only count non-auth failures toward circuit breaker
if (delivery.getStatus() == WebhookDeliveryStatus.FAILED) {
    consecutiveFailures++;
}
// AUTH_FAILURE: log, skip retry, do NOT increment consecutiveFailures
```

---

## 6. API Contract Changes

### 6.1 `WebhookAuthConfigRequest` (create/update)

```java
// Add OAuth2 fields alongside existing ones
public record WebhookAuthConfigRequest(
    WebhookAuthType authType,
    // existing...
    String headerName,    // CUSTOM_HEADER + API_KEY
    String headerValue,   // CUSTOM_HEADER only
    String apiKey,        // API_KEY
    String username,      // BASIC_AUTH
    String password,      // BASIC_AUTH
    String token,         // BEARER_TOKEN
    String secret,        // SIGNING_SECRET
    // new:
    String tokenUrl,      // OAUTH2
    String clientId,      // OAUTH2
    String clientSecret,  // OAUTH2
    String scope          // OAUTH2, nullable
) {}
```

### 6.2 `WebhookAuthConfigDto` (response)

Same fields as request, but `clientSecret`, `password`, `apiKey`, `token`, `secret`, and `headerValue` are always masked as `"[REDACTED]"` in response serialization.

### 6.3 Validation in `WebhookController`

```java
// Applied to both create and update:
if (request.authType() == OAUTH2_CLIENT_CREDENTIALS) {
    if (!request.tokenUrl().startsWith("https://")) {
        throw new ValidationException("tokenUrl must use HTTPS");
    }
    SsrfGuard.check(request.tokenUrl());  // throws on private IP
    // clientId and clientSecret required
}
```

### 6.4 Auth Config Update → Cache Eviction

```java
// WebhookService.updateAuthConfig():
webhookRepository.save(updated);
String cacheKey = "tenant:" + orgId + ":webhook:" + webhookId + ":oauth_token";
redisTemplate.delete(cacheKey);  // evict stale token so next delivery fetches fresh
```

---

## 7. Injector Context: Passing orgId + webhookId to OAuth2 Injector

The current `WebhookAuthInjector` interface signature is:

```java
void injectAuth(HttpHeaders headers, WebhookAuthConfig config, byte[] payload);
```

The OAuth2 injector needs `orgId` and `webhookId` to construct the Redis cache key. Two options:

| Option | Description | Verdict |
|---|---|---|
| **A) Pass via enriched record** | Add `orgId` + `webhookId` fields to `OAuth2ClientCredentials` record (non-persistent, populated by `WebhookDispatcher` before calling injector) | ✅ **Chosen** — no interface change; isolated to OAuth2 |
| B) Extend interface | Change signature to `injectAuth(headers, config, payload, webhookId, orgId)` | ❌ — breaks all 5 existing injectors |
| C) ThreadLocal | Push orgId via `TenantContext.getOrgId()` + pass webhookId separately | ❌ — fragile; implicit coupling |

**Decision:** `WebhookDispatcher` constructs a transient enriched version of the config before calling the injector:

```java
WebhookAuthConfig configToInject = config;
if (config instanceof WebhookAuthConfig.OAuth2ClientCredentials oauth2) {
    // Create a transient wrapper carrying runtime context
    configToInject = new WebhookAuthConfig.OAuth2ClientCredentials(
        oauth2.tokenUrl(), oauth2.clientId(), oauth2.clientSecret(), oauth2.scope()
    );
    // Pass context via injector's setContext() before injectAuth()
}
```

Actually simpler: make `OAuth2ClientCredentialsInjector` a stateless Spring bean; pass `orgId` + `webhookId` as fields on a subclass `OAuth2ClientCredentials` record that carries them transiently, OR use a dedicated method:

**Cleanest approach:** Add an overloaded method to the injector only for OAuth2:

```java
// In WebhookDispatcher:
if (webhook.getAuthType() == OAUTH2_CLIENT_CREDENTIALS) {
    ((OAuth2ClientCredentialsInjector) injector)
        .injectAuth(headers, decryptedConfig, payloadBytes, webhook.getOrgId(), webhook.getId());
} else {
    injector.injectAuth(headers, decryptedConfig, payloadBytes);
}
```

This keeps the interface clean for all other injectors while giving OAuth2 the context it needs. The `instanceof` cast is safe because registry lookup guarantees type match.

---

## 8. Security Properties

| Property | Mechanism |
|---|---|
| `clientSecret` encrypted at rest | `CredentialEncryptionService.encrypt()` on write (same AES-256-GCM key as Epic 5) |
| `clientSecret` never in API responses | `WebhookAuthConfigDto` masks to `"[REDACTED]"` |
| `clientSecret` never logged | `OAuth2ClientCredentialsInjector` logs `tokenUrl` only, not credentials |
| `access_token` not in PostgreSQL | Token lives only in Redis (TTL) and in-memory during delivery |
| `access_token` not in logs | Logged only at TRACE level (disabled in production) |
| `tokenUrl` SSRF protection | `SsrfGuard.check()` at API layer (create/update) AND at injector layer (token fetch) — double defence |
| `tokenUrl` must be HTTPS | Enforced at API layer validation |
| `headerValue` encrypted at rest | Same `CredentialEncryptionService` as `apiKey`, `password`, `token` |

---

## 9. Testing Strategy

### Unit Tests

| Class | Tests |
|---|---|
| `CustomHeaderInjectorTest` | normal inject; null headerValue → exception; custom name; default name |
| `OAuth2ClientCredentialsInjectorTest` | cache hit → no HTTP call; cache miss → fetch + cache + inject; fetch 401 → WebhookAuthException; SSRF block → WebhookAuthException; null secret → exception |
| `OAuth2TokenCacheServiceTest` | get miss; get hit; set with TTL; evict |
| `WebhookDispatcherTest` | AUTH_FAILURE from injector → permanent=true result |
| `OutboxPollerTest` | AUTH_FAILURE status → no retry; no circuit breaker increment |

### Integration Tests

- Mock OAuth2 token endpoint (WireMock): returns `{ access_token, expires_in: 3600 }`
- Verify subsequent delivery within TTL uses cache (only one WireMock call)
- Verify cache eviction on auth config update

---

## 10. Rollout Considerations

- **Feature flag:** Not required — the new auth types are additive. Existing webhooks are unaffected.
- **Zero downtime:** Adding `OAuth2ClientCredentials` to the sealed interface requires redeploying event-service. Old instances will fail to deserialize the new subtype if both versions run simultaneously. Roll forward (not blue/green with mixed versions) recommended.
- **Redis key namespace:** `tenant:*:webhook:*:oauth_token` — no collision with existing keys (`tenant:*:rule:*:compiled`).

---

**Architect Sign-Off:** BMAD Solution Architect Agent — 2026-03-19
