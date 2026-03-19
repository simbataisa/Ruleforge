# ADR-010: OAuth 2.0 Token Caching Strategy

**Date:** 2026-03-19
**Status:** Accepted
**Deciders:** BMAD Solution Architect, Tech Lead
**Epic:** Webhook Auth v2 (Epic 6)

---

## Context

The `OAuth2ClientCredentialsInjector` must fetch an `access_token` from a third-party OAuth 2.0 authorization server before each webhook delivery. Calling the token endpoint on every delivery is not acceptable:

- Most OAuth providers enforce rate limits on the token endpoint (e.g., Okta: 100 req/min per client)
- Token endpoint calls add 50–500ms of latency per delivery
- Sending the client secret on every request increases the attack surface

The token endpoint returns `expires_in` (seconds until expiry), which defines a natural cache TTL. Tokens must be shared across multiple event-service instances (the service scales horizontally).

---

## Decision

**Cache OAuth 2.0 access tokens in Redis as plain strings with TTL = `expires_in - 60` seconds.**

### Key format

```
tenant:{orgId}:webhook:{webhookId}:oauth_token
```

### Value

Raw `access_token` string (plaintext). Tokens are short-lived credentials — they are not secrets in the same sense as `clientSecret`. The Redis connection is secured at the infrastructure level (TLS + auth in production).

### TTL

`max(expires_in - 60, 60)` seconds.

The 60-second grace period ensures tokens are refreshed before they actually expire, even accounting for clock skew and delivery latency.

### Write semantics

`SET NX PX {ttl_ms}` (via `RedisTemplate.opsForValue().setIfAbsent()`). This is atomic — if two event-service instances simultaneously fetch a token on a cache miss, only the first write succeeds. Both tokens are valid; the duplicate is silently discarded. This avoids a distributed lock while remaining correct.

### Cache eviction

Tokens are evicted in two ways:
1. **Natural expiry** via Redis TTL
2. **Explicit delete** when auth config is updated (`WebhookService.updateAuthConfig()` calls `redisTemplate.delete(key)`)

---

## Options Considered

### Option A: Redis string with TTL ✅ (chosen)

- Reuses existing Redis connection in event-service
- TTL-based expiry aligns with OAuth token lifetime
- Shared across all event-service instances (stateless scaling)
- `SET NX PX` prevents duplicate writes without locking
- Simple: one string value, one key

### Option B: Caffeine in-memory cache per JVM

- **Pro:** No network hop for cache reads (faster)
- **Con:** Each event-service instance has its own cache — N instances = N token fetches per TTL window
- **Con:** Cache is lost on restart
- **Verdict:** Rejected — violates the >95% cache hit rate target under horizontal scaling

### Option C: Store token in PostgreSQL (webhook_auth_config JSONB)

- **Pro:** Durable, no additional infrastructure
- **Con:** Tokens must never be stored persistently (security posture — tokens are ephemeral credentials)
- **Con:** No automatic TTL eviction; needs a background cleanup job
- **Verdict:** Rejected — security violation; operational complexity

### Option D: Redis hash (one hash per webhook, TTL on field)

- Redis does not support per-field TTL on hashes
- Overengineered for a single string value
- **Verdict:** Rejected

---

## Consequences

### Positive
- Token endpoint called at most once per `expires_in - 60` seconds per webhook across all instances
- Scales horizontally — new event-service instances immediately benefit from cached tokens
- Consistent with existing Redis usage in the service (L2 rule cache uses same Redis connection)

### Negative / Risks
- Access tokens are stored in Redis. If Redis is compromised, tokens can be used until they expire (minutes to hours). Mitigation: Redis instance secured with auth + TLS in production (existing infrastructure requirement).
- If `expires_in` is absent from token response (non-standard provider), TTL defaults to 3540s (59 min). This is safe but may result in longer-than-necessary token reuse.
- A Redis outage causes all OAuth2 webhook deliveries to fail (cache miss → token fetch → success, but Redis write fails → next delivery also misses cache → performance degrades but correctness maintained).

---

## Implementation Notes

```java
// Cache key construction
private String cacheKey(UUID orgId, UUID webhookId) {
    return "tenant:" + orgId + ":webhook:" + webhookId + ":oauth_token";
}

// Token fetch and cache
long ttlSeconds = Math.max(tokenResponse.expiresIn() - 60, 60);
redisTemplate.opsForValue().setIfAbsent(
    cacheKey(orgId, webhookId),
    tokenResponse.accessToken(),
    ttlSeconds,
    TimeUnit.SECONDS
);

// Cache eviction on config update
redisTemplate.delete(cacheKey(orgId, webhookId));
```

---

**Status:** Accepted — 2026-03-19
