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
     * Stores the token using SET NX PX — atomic set-if-absent.
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

    String cacheKey(UUID orgId, UUID webhookId) {
        return "tenant:" + orgId + ":webhook:" + webhookId + ":oauth_token";
    }
}
