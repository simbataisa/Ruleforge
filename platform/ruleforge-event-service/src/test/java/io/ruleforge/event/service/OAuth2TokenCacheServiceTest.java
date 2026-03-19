package io.ruleforge.event.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OAuth2TokenCacheServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private OAuth2TokenCacheService service;

    private final UUID orgId = UUID.randomUUID();
    private final UUID webhookId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new OAuth2TokenCacheService(redisTemplate);
    }

    @Test
    void get_delegatesToRedisTemplate() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        String expectedKey = service.cacheKey(orgId, webhookId);
        when(valueOperations.get(expectedKey)).thenReturn("my-token");

        String result = service.get(orgId, webhookId);

        assertThat(result).isEqualTo("my-token");
        verify(valueOperations).get(expectedKey);
    }

    @Test
    void set_usesSetIfAbsent_withTtl() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        service.set(orgId, webhookId, "access-token", 3540L);

        verify(valueOperations).setIfAbsent(
                service.cacheKey(orgId, webhookId),
                "access-token",
                3540L,
                TimeUnit.SECONDS);
    }

    @Test
    void set_enforcesMinTtlOf60s() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        service.set(orgId, webhookId, "tok", 30L);

        verify(valueOperations).setIfAbsent(
                service.cacheKey(orgId, webhookId),
                "tok",
                60L,
                TimeUnit.SECONDS);
    }

    @Test
    void evict_callsDelete() {
        String expectedKey = service.cacheKey(orgId, webhookId);

        service.evict(orgId, webhookId);

        verify(redisTemplate).delete(expectedKey);
    }

    @Test
    void cacheKey_matchesExpectedFormat() {
        String key = service.cacheKey(orgId, webhookId);

        assertThat(key).isEqualTo("tenant:" + orgId + ":webhook:" + webhookId + ":oauth_token");
    }
}
