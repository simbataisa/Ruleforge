package io.ruleforge.event.service;

import io.ruleforge.event.entity.WebhookDeliveryEntity;
import io.ruleforge.event.entity.WebhookEntity;
import io.ruleforge.event.entity.WebhookOutboxEntity;
import io.ruleforge.event.repository.WebhookDeliveryRepository;
import io.ruleforge.event.repository.WebhookOutboxRepository;
import io.ruleforge.event.repository.WebhookRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Polls the webhook_outbox table and delivers pending entries.
 * Implements exponential backoff with jitter and circuit breaker logic.
 */
@Component
@EnableScheduling
public class OutboxPoller {

    private static final Logger log = LoggerFactory.getLogger(OutboxPoller.class);
    private static final int MAX_ATTEMPTS = 5;
    private static final int CIRCUIT_BREAKER_THRESHOLD = 5;
    private static final int BATCH_SIZE = 50;

    // Backoff delays in seconds: attempt 1→0s, 2→30s, 3→5min, 4→30min, 5→2h
    private static final long[] BACKOFF_SECONDS = {0, 30, 300, 1800, 7200};

    private final WebhookOutboxRepository outboxRepository;
    private final WebhookRepository webhookRepository;
    private final WebhookDeliveryRepository deliveryRepository;
    private final WebhookDispatcher webhookDispatcher;

    public OutboxPoller(WebhookOutboxRepository outboxRepository,
                        WebhookRepository webhookRepository,
                        WebhookDeliveryRepository deliveryRepository,
                        WebhookDispatcher webhookDispatcher) {
        this.outboxRepository = outboxRepository;
        this.webhookRepository = webhookRepository;
        this.deliveryRepository = deliveryRepository;
        this.webhookDispatcher = webhookDispatcher;
    }

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void pollAndDeliver() {
        List<WebhookOutboxEntity> pending = outboxRepository.findPendingForDelivery(
                OffsetDateTime.now(), BATCH_SIZE
        );
        for (WebhookOutboxEntity entry : pending) {
            try {
                deliverEntry(entry);
            } catch (Exception e) {
                log.error("Unexpected error delivering outbox entry {}: {}",
                        entry.getId(), e.getMessage(), e);
            }
        }
    }

    private void deliverEntry(WebhookOutboxEntity entry) {
        // 1. Set status=DELIVERING, generate new attemptNonce, set lastAttemptAt
        entry.setStatus("DELIVERING");
        entry.setAttemptNonce(UUID.randomUUID());
        entry.setLastAttemptAt(OffsetDateTime.now());
        outboxRepository.save(entry);

        // 2. Fetch webhook entity
        Optional<WebhookEntity> webhookOpt = webhookRepository.findById(entry.getWebhookId());
        if (webhookOpt.isEmpty()) {
            log.warn("Webhook {} not found for outbox entry {}, marking DLQ",
                    entry.getWebhookId(), entry.getId());
            entry.setStatus("DLQ");
            entry.setLastError("Webhook not found");
            outboxRepository.save(entry);
            return;
        }

        WebhookEntity webhook = webhookOpt.get();
        if (!webhook.isActive()) {
            log.debug("Webhook {} is inactive, marking outbox entry {} as DLQ",
                    webhook.getId(), entry.getId());
            entry.setStatus("DLQ");
            entry.setLastError("Webhook inactive");
            outboxRepository.save(entry);
            return;
        }

        // 3. Deliver via dispatcher
        WebhookDispatcher.DeliveryResult result = webhookDispatcher.deliverDirect(webhook, entry);

        // 4. Record delivery
        WebhookDeliveryEntity delivery = new WebhookDeliveryEntity();
        delivery.setWebhookId(webhook.getId());
        delivery.setEventType(entry.getEventType());
        delivery.setPayload(result.payloadJson());
        delivery.setStatusCode(result.statusCode());
        delivery.setResponseBody(result.responseBody());
        delivery.setSuccess(result.success());
        delivery.setDeliveredAt(Instant.now());
        deliveryRepository.save(delivery);

        if (result.success()) {
            // Success path
            entry.setStatus("DELIVERED");
            entry.setLastStatusCode(result.statusCode());
            outboxRepository.save(entry);

            webhook.setConsecutiveFailures(0);
            webhook.setLastDeliveredAt(OffsetDateTime.now());
            webhookRepository.save(webhook);

            log.debug("Successfully delivered outbox entry {} to webhook {}",
                    entry.getId(), webhook.getId());
        } else {
            // Failure path
            int newAttemptCount = entry.getAttemptCount() + 1;
            entry.setAttemptCount(newAttemptCount);
            entry.setLastStatusCode(result.statusCode());
            entry.setLastError(result.errorMessage());

            if (newAttemptCount >= MAX_ATTEMPTS) {
                entry.setStatus("DLQ");
                outboxRepository.save(entry);

                // Increment consecutive failures on webhook
                int newFailures = webhook.getConsecutiveFailures() + 1;
                webhook.setConsecutiveFailures(newFailures);

                if (newFailures >= CIRCUIT_BREAKER_THRESHOLD) {
                    webhook.setActive(false);
                    webhook.setCircuitOpenedAt(OffsetDateTime.now());
                    log.warn("Circuit breaker opened for webhook {} after {} consecutive failures",
                            webhook.getId(), newFailures);
                }
                webhookRepository.save(webhook);
            } else {
                entry.setStatus("FAILED");
                entry.setNextAttemptAt(computeNextAttempt(newAttemptCount));
                outboxRepository.save(entry);
            }

            log.debug("Delivery failed for outbox entry {} (attempt {}/{}): {}",
                    entry.getId(), newAttemptCount, MAX_ATTEMPTS, result.errorMessage());
        }
    }

    private OffsetDateTime computeNextAttempt(int attemptCount) {
        int index = Math.min(attemptCount - 1, BACKOFF_SECONDS.length - 1);
        long baseDelay = BACKOFF_SECONDS[index];
        // Add jitter: up to 20% of the base delay
        long jitter = baseDelay > 0
                ? ThreadLocalRandom.current().nextLong(0, Math.max(1, baseDelay / 5))
                : 0;
        return OffsetDateTime.now().plusSeconds(baseDelay + jitter);
    }

    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void cleanupOldDlq() {
        int deleted = outboxRepository.deleteOldDlqEntries(OffsetDateTime.now().minusDays(30));
        if (deleted > 0) {
            log.info("Cleaned up {} old DLQ outbox entries", deleted);
        }
    }
}
