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
    private ValueOperations<String, String> valueOps;

    private OAuth2TokenCacheService service;

    private final UUID orgId = UUID.randomUUID();
    private final UUID webhookId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new OAuth2TokenCacheService(redisTemplate);
    }

    @Test
    void get_delegatesToRedisTemplate() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        String expectedKey = "tenant:" + orgId + ":webhook:" + webhookId + ":oauth_token";
        when(valueOps.get(expectedKey)).thenReturn("my-token");

        String result = service.get(orgId, webhookId);

        assertThat(result).isEqualTo("my-token");
        verify(valueOps).get(expectedKey);
    }

    @Test
    void get_returnsNullOnCacheMiss() {
        UUID orgId = UUID.randomUUID();
        UUID webhookId = UUID.randomUUID();
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(anyString())).thenReturn(null);
        assertThat(service.get(orgId, webhookId)).isNull();
    }

    @Test
    void set_usesSetIfAbsent_withTtl() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        service.set(orgId, webhookId, "access-token", 3540L);

        verify(valueOps).setIfAbsent(
                "tenant:" + orgId + ":webhook:" + webhookId + ":oauth_token",
                "access-token",
                3540L,
                TimeUnit.SECONDS);
    }

    @Test
    void set_enforcesMinTtlOf60s() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        service.set(orgId, webhookId, "tok", 30L);

        verify(valueOps).setIfAbsent(
                "tenant:" + orgId + ":webhook:" + webhookId + ":oauth_token",
                "tok",
                60L,
                TimeUnit.SECONDS);
    }

    @Test
    void evict_callsDelete() {
        String expectedKey = "tenant:" + orgId + ":webhook:" + webhookId + ":oauth_token";

        service.evict(orgId, webhookId);

        verify(redisTemplate).delete(expectedKey);
    }
}
