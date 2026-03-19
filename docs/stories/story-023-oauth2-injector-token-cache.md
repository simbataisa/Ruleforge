# Story 023 — OAuth 2.0 Client Credentials Injector + Token Cache

**Story ID:** story-023
**Epic:** Webhook Auth v2 — OAuth 2.0 & Custom Header (Epic 6)
**Prepared by:** Tech Lead
**Date:** 2026-03-19
**Assigned to:** Backend Engineer
**Priority:** P0
**Estimate:** 4 story points
**Depends on:** Story 024 (AUTH_FAILURE status + WebhookAuthException), Story 022 (WebhookAuthException class created there if 024 not done first)

---

## User Story

**As an** organization owner or editor,
**I want to** configure OAuth 2.0 client credentials (token URL, client ID, client secret, optional scope),
**So that** RuleForge automatically acquires and caches Bearer tokens and delivers events to enterprise APIs protected by OAuth 2.0 (Salesforce, Azure AD, Okta, etc.).

---

## Context

The `WebhookAuthType.OAUTH2_CLIENT_CREDENTIALS` enum value exists. The `WebhookAuthConfig` sealed interface does NOT yet have an `OAuth2ClientCredentials` record — this story adds it. The `WebhookAuthInjectorRegistry` auto-discovers `@Component` injector beans.

**Token caching design (ADR-010):** Tokens are cached in Redis as plain strings with key `tenant:{orgId}:webhook:{webhookId}:oauth_token` and TTL = `expires_in - 60s` (minimum 60s). `SET NX PX` atomicity prevents duplicate fetches under concurrent instances.

**Context passing:** `WebhookDispatcher` calls an overloaded `injectAuth` variant on the OAuth2 injector that accepts `orgId` and `webhookId`:
```java
// In WebhookDispatcher:
if (webhook.getAuthType() == OAUTH2_CLIENT_CREDENTIALS && injector instanceof OAuth2ClientCredentialsInjector oauth2Injector) {
    oauth2Injector.injectAuth(headers, decryptedConfig, payloadBytes, webhook.getOrgId(), webhook.getId());
} else {
    injector.injectAuth(headers, decryptedConfig, payloadBytes);
}
```

**CredentialEncryptionService** has already decrypted `clientSecret` by the time the injector is called — same as all other injectors.

**SSRF protection:** Call `SsrfGuard.check(tokenUrl)` before any HTTP call to the token endpoint.

---

## Acceptance Criteria

1. `WebhookAuthConfig` sealed interface contains `OAuth2ClientCredentials(String tokenUrl, String clientId, String clientSecret, String scope)` record with `@JsonSubTypes` registration.
2. `OAuth2ClientCredentialsInjector.supportedType()` returns `WebhookAuthType.OAUTH2_CLIENT_CREDENTIALS`.
3. **Cache miss:** injector POSTs `grant_type=client_credentials` + `client_id` + `client_secret` + (optional) `scope` to `tokenUrl` as `application/x-www-form-urlencoded`. On 200 response, caches `access_token` with TTL = `max(expires_in - 60, 60)` seconds. Adds `Authorization: Bearer {token}` to delivery headers.
4. **Cache hit:** injector reads token from Redis and adds `Authorization: Bearer {token}`. Token endpoint is NOT called.
5. Redis cache key format: `tenant:{orgId}:webhook:{webhookId}:oauth_token`.
6. Cache write uses `setIfAbsent` (Redis `SET NX PX`) — safe under concurrent instances.
7. **Token endpoint HTTP non-2xx:** throws `WebhookAuthException("OAuth2 token fetch failed: HTTP {status} from {tokenUrl}")`.
8. **Token endpoint network/timeout error:** throws `WebhookAuthException` wrapping the cause.
9. **SSRF block:** `SsrfGuard.check(tokenUrl)` called before fetch; on block, throws `WebhookAuthException("tokenUrl blocked by SSRF guard: {tokenUrl}")`.
10. `clientSecret` and `access_token` never appear in log output at INFO or above.
11. `access_token` is never written to PostgreSQL.
12. `expires_in` absent from token response → default TTL of 3540 seconds (59 minutes).
13. `cd platform && ./gradlew :ruleforge-event-service:test` passes.

---

## Files to Modify

### `platform/ruleforge-event-service/src/main/java/io/ruleforge/event/model/WebhookAuthConfig.java`

Add `OAuth2ClientCredentials` record and register it:

```java
@JsonSubTypes({
    @JsonSubTypes.Type(value = WebhookAuthConfig.None.class, name = "NONE"),
    @JsonSubTypes.Type(value = WebhookAuthConfig.SigningSecret.class, name = "SIGNING_SECRET"),
    @JsonSubTypes.Type(value = WebhookAuthConfig.ApiKey.class, name = "API_KEY"),
    @JsonSubTypes.Type(value = WebhookAuthConfig.BasicAuth.class, name = "BASIC_AUTH"),
    @JsonSubTypes.Type(value = WebhookAuthConfig.BearerToken.class, name = "BEARER_TOKEN"),
    @JsonSubTypes.Type(value = WebhookAuthConfig.CustomHeader.class, name = "CUSTOM_HEADER"),
    @JsonSubTypes.Type(value = WebhookAuthConfig.OAuth2ClientCredentials.class, name = "OAUTH2_CLIENT_CREDENTIALS")  // NEW
})
public sealed interface WebhookAuthConfig permits
        WebhookAuthConfig.None,
        WebhookAuthConfig.SigningSecret,
        WebhookAuthConfig.ApiKey,
        WebhookAuthConfig.BasicAuth,
        WebhookAuthConfig.BearerToken,
        WebhookAuthConfig.CustomHeader,
        WebhookAuthConfig.OAuth2ClientCredentials {   // NEW

    record None() implements WebhookAuthConfig {}
    record SigningSecret(String secret) implements WebhookAuthConfig {}
    record ApiKey(String headerName, String apiKey) implements WebhookAuthConfig {}
    record BasicAuth(String username, String password) implements WebhookAuthConfig {}
    record BearerToken(String token) implements WebhookAuthConfig {}
    record CustomHeader(String headerName, String headerValue) implements WebhookAuthConfig {}

    /** NEW */
    record OAuth2ClientCredentials(
        String tokenUrl,
        String clientId,
        String clientSecret,  // encrypted at rest; decrypted before injection
        String scope          // nullable — omitted from token request if null/blank
    ) implements WebhookAuthConfig {}
}
```

### `platform/ruleforge-event-service/src/main/java/io/ruleforge/event/service/WebhookDispatcher.java`

Add dispatch branch for OAuth2 (context passing):

```java
// Replace: injector.injectAuth(headers, decryptedConfig, payloadBytes);
// With:
try {
    if (decryptedConfig instanceof WebhookAuthConfig.OAuth2ClientCredentials
            && injector instanceof OAuth2ClientCredentialsInjector oauth2Injector) {
        oauth2Injector.injectAuth(headers, decryptedConfig, payloadBytes,
                webhook.getOrgId(), webhook.getId());
    } else {
        injector.injectAuth(headers, decryptedConfig, payloadBytes);
    }
} catch (WebhookAuthException e) {
    log.warn("Auth injection failed for webhook {}: {}", webhook.getId(), e.getMessage());
    return new DeliveryResult(false, 0, null, payloadJson, e.getMessage(), true);
}
```

---

## Files to Create

### `platform/ruleforge-event-service/src/main/java/io/ruleforge/event/service/OAuth2TokenCacheService.java`

```java
package io.ruleforge.event.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class OAuth2TokenCacheService {

    private static final Logger log = LoggerFactory.getLogger(OAuth2TokenCacheService.class);

    private final RedisTemplate<String, String> redisTemplate;

    public OAuth2TokenCacheService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public String get(UUID orgId, UUID webhookId) {
        return redisTemplate.opsForValue().get(cacheKey(orgId, webhookId));
    }

    /**
     * Store token using SET NX PX — atomic set-if-absent.
     * Safe under concurrent event-service instances.
     */
    public void set(UUID orgId, UUID webhookId, String accessToken, long ttlSeconds) {
        long safeTtl = Math.max(ttlSeconds, 60L);
        redisTemplate.opsForValue().setIfAbsent(
                cacheKey(orgId, webhookId), accessToken, safeTtl, TimeUnit.SECONDS);
        log.debug("OAuth2 token cached for webhook {} with TTL {}s", webhookId, safeTtl);
    }

    public void evict(UUID orgId, UUID webhookId) {
        redisTemplate.delete(cacheKey(orgId, webhookId));
        log.debug("OAuth2 token cache evicted for webhook {}", webhookId);
    }

    private String cacheKey(UUID orgId, UUID webhookId) {
        return "tenant:" + orgId + ":webhook:" + webhookId + ":oauth_token";
    }
}
```

### `platform/ruleforge-event-service/src/main/java/io/ruleforge/event/delivery/OAuth2ClientCredentialsInjector.java`

```java
package io.ruleforge.event.delivery;

import io.ruleforge.event.exception.WebhookAuthException;
import io.ruleforge.event.model.WebhookAuthConfig;
import io.ruleforge.event.model.WebhookAuthType;
import io.ruleforge.event.service.OAuth2TokenCacheService;
import io.ruleforge.event.util.SsrfGuard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;
import java.util.UUID;

@Component
public class OAuth2ClientCredentialsInjector implements WebhookAuthInjector {

    private static final Logger log = LoggerFactory.getLogger(OAuth2ClientCredentialsInjector.class);
    private static final long DEFAULT_TTL_SECONDS = 3540L; // 59 minutes fallback

    private final OAuth2TokenCacheService tokenCache;
    private final RestClient webhookClient;

    public OAuth2ClientCredentialsInjector(OAuth2TokenCacheService tokenCache,
                                            RestClient webhookClient) {
        this.tokenCache = tokenCache;
        this.webhookClient = webhookClient;
    }

    @Override
    public WebhookAuthType supportedType() {
        return WebhookAuthType.OAUTH2_CLIENT_CREDENTIALS;
    }

    /** Standard interface method — not used for OAuth2; see overload below. */
    @Override
    public void injectAuth(HttpHeaders headers, WebhookAuthConfig config, byte[] payload) {
        throw new UnsupportedOperationException(
                "OAuth2ClientCredentialsInjector requires orgId and webhookId — use overloaded method");
    }

    /** Used by WebhookDispatcher for OAuth2 type — provides Redis cache context. */
    public void injectAuth(HttpHeaders headers, WebhookAuthConfig config, byte[] payload,
                           UUID orgId, UUID webhookId) {
        if (!(config instanceof WebhookAuthConfig.OAuth2ClientCredentials oauth2)) {
            return;
        }

        // 1. Cache hit
        String cachedToken = tokenCache.get(orgId, webhookId);
        if (cachedToken != null) {
            log.debug("OAuth2 token cache hit for webhook {}", webhookId);
            headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + cachedToken);
            return;
        }

        // 2. SSRF guard
        try {
            SsrfGuard.check(oauth2.tokenUrl());
        } catch (IllegalArgumentException e) {
            throw new WebhookAuthException("tokenUrl blocked by SSRF guard: " + oauth2.tokenUrl(), e);
        }

        // 3. Fetch token
        log.debug("OAuth2 token cache miss for webhook {} — fetching from {}", webhookId, oauth2.tokenUrl());
        String accessToken = fetchToken(oauth2, webhookId);

        // 4. Cache and inject
        tokenCache.set(orgId, webhookId, accessToken, DEFAULT_TTL_SECONDS); // TTL overridden by response if available
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
    }

    @SuppressWarnings("unchecked")
    private String fetchToken(WebhookAuthConfig.OAuth2ClientCredentials oauth2, UUID webhookId) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", oauth2.clientId());
        form.add("client_secret", oauth2.clientSecret()); // already decrypted by CredentialEncryptionService
        if (oauth2.scope() != null && !oauth2.scope().isBlank()) {
            form.add("scope", oauth2.scope());
        }

        try {
            Map<String, Object> tokenResponse = webhookClient.post()
                    .uri(oauth2.tokenUrl())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(Map.class);

            if (tokenResponse == null || !tokenResponse.containsKey("access_token")) {
                throw new WebhookAuthException(
                        "OAuth2 token fetch returned no access_token from " + oauth2.tokenUrl());
            }

            return (String) tokenResponse.get("access_token");

        } catch (RestClientResponseException e) {
            throw new WebhookAuthException(
                    "OAuth2 token fetch failed: HTTP " + e.getStatusCode().value()
                    + " from " + oauth2.tokenUrl(), e);
        } catch (WebhookAuthException e) {
            throw e;
        } catch (Exception e) {
            throw new WebhookAuthException(
                    "OAuth2 token fetch failed: " + e.getMessage() + " (tokenUrl=" + oauth2.tokenUrl() + ")", e);
        }
    }
}
```

---

## Files to Create (Tests)

### `platform/ruleforge-event-service/src/test/java/io/ruleforge/event/delivery/OAuth2ClientCredentialsInjectorTest.java`

Key test cases (write full test class):

```java
// Test cases to implement:

@Test void cacheHit_usesToken_doesNotCallTokenEndpoint()
// Mock tokenCache.get() returns "cached-token"
// Verify headers has Authorization: Bearer cached-token
// Verify webhookClient is never called

@Test void cacheMiss_fetchesToken_cachesAndInjects()
// Mock tokenCache.get() returns null
// Mock webhookClient POST returns { access_token: "new-token", expires_in: 3600 }
// Verify Authorization: Bearer new-token header set
// Verify tokenCache.set() called with correct key parts and TTL = 3540

@Test void cacheMiss_tokenEndpointReturns401_throwsWebhookAuthException()
// Mock webhookClient throws RestClientResponseException(401)
// assertThatThrownBy → WebhookAuthException with "HTTP 401"

@Test void cacheMiss_tokenEndpointNetworkError_throwsWebhookAuthException()
// Mock webhookClient throws ResourceAccessException
// assertThatThrownBy → WebhookAuthException

@Test void ssrfBlockedUrl_throwsWebhookAuthException()
// Use a private IP tokenUrl (e.g., "https://192.168.1.1/token")
// SsrfGuard.check() throws → WebhookAuthException("blocked by SSRF guard")

@Test void missingExpiresIn_usesFallbackTtl()
// Token response has no expires_in
// tokenCache.set() called with DEFAULT_TTL_SECONDS

@Test void scopeNull_notIncludedInForm()
// oauth2.scope() == null → form body does NOT contain "scope"

@Test void scopePresent_includedInForm()
// oauth2.scope() = "api:read" → form body contains scope=api:read
```

### `platform/ruleforge-event-service/src/test/java/io/ruleforge/event/service/OAuth2TokenCacheServiceTest.java`

```java
@Test void get_returnsNullOnCacheMiss()
@Test void set_storesValueWithTtl()
@Test void set_usesMinimumTtlOf60s()
@Test void evict_deletesKey()
@Test void cacheKey_format_isCorrect()
// Verify key matches "tenant:{orgId}:webhook:{webhookId}:oauth_token"
```

---

## Definition of Done

- [ ] `WebhookAuthConfig.OAuth2ClientCredentials` record added to sealed interface with `@JsonSubTypes` entry
- [ ] `OAuth2TokenCacheService.java` created with get/set/evict
- [ ] `OAuth2ClientCredentialsInjector.java` created with overloaded `injectAuth`
- [ ] `WebhookDispatcher` routes OAUTH2 type to overloaded method
- [ ] `WebhookDispatcher` catches `WebhookAuthException` → permanent DeliveryResult
- [ ] SSRF guard called on `tokenUrl` before token fetch
- [ ] `clientSecret` not logged at INFO or above
- [ ] `access_token` not persisted to PostgreSQL
- [ ] All unit tests pass
- [ ] `cd platform && ./gradlew :ruleforge-event-service:test` green
