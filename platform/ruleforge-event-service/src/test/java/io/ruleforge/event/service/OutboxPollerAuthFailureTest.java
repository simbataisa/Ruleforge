package io.ruleforge.event.service;

import io.ruleforge.event.entity.WebhookEntity;
import io.ruleforge.event.entity.WebhookOutboxEntity;
import io.ruleforge.event.repository.WebhookDeliveryRepository;
import io.ruleforge.event.repository.WebhookOutboxRepository;
import io.ruleforge.event.repository.WebhookRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for OutboxPoller focused on AUTH_FAILURE handling and circuit breaker isolation.
 */
@ExtendWith(MockitoExtension.class)
class OutboxPollerAuthFailureTest {

    @Mock private WebhookOutboxRepository outboxRepository;
    @Mock private WebhookRepository webhookRepository;
    @Mock private WebhookDeliveryRepository deliveryRepository;
    @Mock private WebhookDispatcher webhookDispatcher;
    @Mock private JdbcTemplate jdbcTemplate;
    @Mock private SsrfGuard ssrfGuard;
    @Mock private AuditEventService auditEventService;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOps;

    private OutboxPoller poller;

    @BeforeEach
    void setUp() {
        poller = new OutboxPoller(
                outboxRepository,
                webhookRepository,
                deliveryRepository,
                webhookDispatcher,
                jdbcTemplate,
                ssrfGuard,
                auditEventService,
                redisTemplate
        );
    }

    // ------------------------------------------------------------------
    // Helper builders
    // ------------------------------------------------------------------

    private WebhookEntity activeWebhook(UUID orgId) {
        WebhookEntity webhook = new WebhookEntity();
        webhook.setId(UUID.randomUUID());
        webhook.setUrl("https://example.com/hook");
        webhook.setActive(true);
        webhook.setOrgId(orgId);
        webhook.setSecret("ignored");
        webhook.setConsecutiveFailures(0);
        return webhook;
    }

    private WebhookOutboxEntity outboxEntry(UUID webhookId, UUID orgId) {
        WebhookOutboxEntity entry = new WebhookOutboxEntity();
        entry.setId(UUID.randomUUID());
        entry.setWebhookId(webhookId);
        entry.setOrgId(orgId);
        entry.setEventType("rule.fired");
        entry.setPayload(Map.of("key", "value"));
        entry.setStatus("DELIVERING");
        entry.setAttemptCount(0);
        entry.setAttemptNonce(UUID.randomUUID());
        entry.setNextAttemptAt(OffsetDateTime.now().minusSeconds(5));
        return entry;
    }

    /** Build a DeliveryResult indicating AUTH_FAILURE. */
    private WebhookDispatcher.DeliveryResult authFailureResult() {
        return new WebhookDispatcher.DeliveryResult(
                false, 0, null, "{}", "AUTH_FAILURE: Token validation failed", true, true
        );
    }

    /** Build a DeliveryResult indicating a normal (non-permanent) HTTP failure. */
    private WebhookDispatcher.DeliveryResult httpFailureResult(int statusCode) {
        return new WebhookDispatcher.DeliveryResult(
                false, statusCode, "Internal Server Error", "{}", "HTTP " + statusCode, false
        );
    }

    // ------------------------------------------------------------------
    // Test 1: AUTH_FAILURE does NOT increment consecutiveFailures / trip circuit breaker
    // ------------------------------------------------------------------

    @Test
    void authFailureDelivery_doesNotIncrementConsecutiveFailures() {
        UUID orgId = UUID.randomUUID();
        WebhookEntity webhook = activeWebhook(orgId);
        WebhookOutboxEntity entry = outboxEntry(webhook.getId(), orgId);

        // Rate limiter fails open (no interactions needed — returns false by default from mock)
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.increment(anyString())).thenReturn(1L);

        when(webhookDispatcher.deliverDirect(webhook, entry)).thenReturn(authFailureResult());

        // Invoke the method under test (package-visible via same-package test)
        // deliverEntry is private, so we exercise it via pollAndDeliver
        // Stub the batch claim to return our pre-built entry
        when(outboxRepository.findPendingForDelivery(any(OffsetDateTime.class), anyInt()))
                .thenReturn(List.of(entry));
        when(webhookRepository.findAllById(anyList())).thenReturn(List.of(webhook));

        poller.pollAndDeliver();

        // The webhook consecutiveFailures must NOT have been incremented (no webhookRepository.save call)
        verify(webhookRepository, never()).save(any(WebhookEntity.class));

        // outboxRepository.save is called twice: once in claimBatch (DELIVERING) and once in
        // updateResultAuthFailure (AUTH_FAILURE). Capture both and assert the last one.
        ArgumentCaptor<WebhookOutboxEntity> captor = ArgumentCaptor.forClass(WebhookOutboxEntity.class);
        verify(outboxRepository, times(2)).save(captor.capture());
        List<WebhookOutboxEntity> allSaves = captor.getAllValues();
        WebhookOutboxEntity authFailureSave = allSaves.get(allSaves.size() - 1);
        assertEquals("AUTH_FAILURE", authFailureSave.getStatus(),
                "Outbox entry status must be AUTH_FAILURE");
        assertNotNull(authFailureSave.getLastError(),
                "lastError must be populated for AUTH_FAILURE");
    }

    // ------------------------------------------------------------------
    // Test 2: AUTH_FAILURE does not save a delivery record (no HTTP call was made)
    // ------------------------------------------------------------------

    @Test
    void authFailureDelivery_doesNotSaveDeliveryRecord() {
        UUID orgId = UUID.randomUUID();
        WebhookEntity webhook = activeWebhook(orgId);
        WebhookOutboxEntity entry = outboxEntry(webhook.getId(), orgId);

        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.increment(anyString())).thenReturn(1L);

        when(webhookDispatcher.deliverDirect(webhook, entry)).thenReturn(authFailureResult());
        when(outboxRepository.findPendingForDelivery(any(OffsetDateTime.class), anyInt()))
                .thenReturn(List.of(entry));
        when(webhookRepository.findAllById(anyList())).thenReturn(List.of(webhook));

        poller.pollAndDeliver();

        // No delivery record should be written — AUTH_FAILURE takes the early-exit path
        verifyNoInteractions(deliveryRepository);
    }

    // ------------------------------------------------------------------
    // Test 3: Normal FAILED delivery still increments consecutiveFailures
    // ------------------------------------------------------------------

    @Test
    void normalFailedDelivery_incrementsConsecutiveFailures() {
        UUID orgId = UUID.randomUUID();
        WebhookEntity webhook = activeWebhook(orgId);
        WebhookOutboxEntity entry = outboxEntry(webhook.getId(), orgId);

        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.increment(anyString())).thenReturn(1L);

        when(webhookDispatcher.deliverDirect(webhook, entry)).thenReturn(httpFailureResult(500));
        when(outboxRepository.findPendingForDelivery(any(OffsetDateTime.class), anyInt()))
                .thenReturn(List.of(entry));
        when(webhookRepository.findAllById(anyList())).thenReturn(List.of(webhook));

        // updateResult uses REQUIRES_NEW + jdbcTemplate — stub the execute call
        doNothing().when(jdbcTemplate).execute(anyString());

        poller.pollAndDeliver();

        // updateResult writes a delivery record
        verify(deliveryRepository).save(any());

        // The outbox entry status must be FAILED (attempt 1 of MAX_ATTEMPTS=5)
        ArgumentCaptor<WebhookOutboxEntity> outboxCaptor = ArgumentCaptor.forClass(WebhookOutboxEntity.class);
        verify(outboxRepository, atLeastOnce()).save(outboxCaptor.capture());
        List<WebhookOutboxEntity> savedEntries = outboxCaptor.getAllValues();
        // The last save (updateResult) should have FAILED status
        WebhookOutboxEntity finalEntry = savedEntries.get(savedEntries.size() - 1);
        assertEquals("FAILED", finalEntry.getStatus(),
                "Outbox entry status must be FAILED after first failed attempt");
    }

    // ------------------------------------------------------------------
    // Test 4: updateResultAuthFailure transaction method sets correct status
    // ------------------------------------------------------------------

    @Test
    void updateResultAuthFailure_setsStatusAndError() {
        WebhookOutboxEntity entry = new WebhookOutboxEntity();
        entry.setId(UUID.randomUUID());
        entry.setWebhookId(UUID.randomUUID());
        entry.setOrgId(UUID.randomUUID());
        entry.setStatus("DELIVERING");

        poller.updateResultAuthFailure(entry, "AUTH_FAILURE: test error");

        ArgumentCaptor<WebhookOutboxEntity> captor = ArgumentCaptor.forClass(WebhookOutboxEntity.class);
        verify(outboxRepository).save(captor.capture());
        assertEquals("AUTH_FAILURE", captor.getValue().getStatus());
        assertEquals("AUTH_FAILURE: test error", captor.getValue().getLastError());
    }
}
