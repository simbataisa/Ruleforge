package io.ruleforge.event.service;

import io.ruleforge.event.entity.WebhookDeliveryEntity;
import io.ruleforge.event.entity.WebhookEntity;
import io.ruleforge.event.entity.WebhookOutboxEntity;
import io.ruleforge.event.repository.WebhookDeliveryRepository;
import io.ruleforge.event.repository.WebhookOutboxRepository;
import io.ruleforge.event.repository.WebhookRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * Polls the webhook_outbox table and delivers pending entries.
 * Implements exponential backoff with jitter and circuit breaker logic.
 *
 * Transaction design (avoids holding row locks across HTTP I/O):
 *   1. claimBatch()   — REQUIRES_NEW: fetch + mark DELIVERING, commit immediately
 *   2. deliver()      — outside any transaction: HTTP call
 *   3. updateResult() — REQUIRES_NEW: persist outcome, sets RLS context via SET LOCAL
 */
@Component
@EnableScheduling
public class OutboxPoller {

    private static final Logger log = LoggerFactory.getLogger(OutboxPoller.class);
    private static final int MAX_ATTEMPTS = 5;
    private static final int CIRCUIT_BREAKER_THRESHOLD = 5;
    private static final int BATCH_SIZE = 50;

    // Backoff delays in seconds indexed by the NEW attempt count after failure:
    // index 0 unused, 1 → 30s (1st retry), 2 → 5m, 3 → 30m, 4 → 2h
    private static final long[] BACKOFF_SECONDS = {0, 30, 300, 1800, 7200};

    private final WebhookOutboxRepository outboxRepository;
    private final WebhookRepository webhookRepository;
    private final WebhookDeliveryRepository deliveryRepository;
    private final WebhookDispatcher webhookDispatcher;
    private final JdbcTemplate jdbcTemplate;

    public OutboxPoller(WebhookOutboxRepository outboxRepository,
                        WebhookRepository webhookRepository,
                        WebhookDeliveryRepository deliveryRepository,
                        WebhookDispatcher webhookDispatcher,
                        JdbcTemplate jdbcTemplate) {
        this.outboxRepository = outboxRepository;
        this.webhookRepository = webhookRepository;
        this.deliveryRepository = deliveryRepository;
        this.webhookDispatcher = webhookDispatcher;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Scheduled(fixedDelay = 5000)
    public void pollAndDeliver() {
        // Step 1: Claim a batch in its own transaction (releases SKIP LOCKED immediately)
        List<WebhookOutboxEntity> claimed = claimBatch();
        if (claimed.isEmpty()) {
            return;
        }

        // Step 2: Batch-fetch all referenced webhooks (N+1 fix)
        List<UUID> webhookIds = claimed.stream()
                .map(WebhookOutboxEntity::getWebhookId)
                .distinct()
                .collect(Collectors.toList());
        Map<UUID, WebhookEntity> webhookMap = webhookRepository.findAllById(webhookIds)
                .stream()
                .collect(Collectors.toMap(WebhookEntity::getId, w -> w));

        // Step 3: Deliver each claimed entry (outside any transaction)
        for (WebhookOutboxEntity entry : claimed) {
            try {
                WebhookEntity webhook = webhookMap.get(entry.getWebhookId());
                deliverEntry(entry, webhook);
            } catch (Exception e) {
                log.error("Unexpected error delivering outbox entry {}: {}",
                        entry.getId(), e.getMessage(), e);
            }
        }
    }

    /**
     * Transaction 1: Claim pending rows with SKIP LOCKED, mark as DELIVERING.
     * Commits immediately so the row lock is released before HTTP I/O.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<WebhookOutboxEntity> claimBatch() {
        List<WebhookOutboxEntity> pending = outboxRepository.findPendingForDelivery(
                OffsetDateTime.now(), BATCH_SIZE
        );
        for (WebhookOutboxEntity entry : pending) {
            entry.setStatus("DELIVERING");
            entry.setAttemptNonce(UUID.randomUUID());
            entry.setLastAttemptAt(OffsetDateTime.now());
            outboxRepository.save(entry);
        }
        return pending;
    }

    /**
     * Deliver a single outbox entry. Runs outside any transaction.
     * The actual outcome is persisted via updateResult() in its own transaction.
     */
    private void deliverEntry(WebhookOutboxEntity entry, WebhookEntity webhook) {
        if (webhook == null) {
            log.warn("Webhook {} not found for outbox entry {}, marking DLQ",
                    entry.getWebhookId(), entry.getId());
            updateResultDlq(entry, "Webhook not found");
            return;
        }

        if (!webhook.isActive()) {
            log.debug("Webhook {} is inactive, marking outbox entry {} as DLQ",
                    webhook.getId(), entry.getId());
            updateResultDlq(entry, "Webhook inactive");
            return;
        }

        // HTTP delivery — outside any transaction
        WebhookDispatcher.DeliveryResult result = webhookDispatcher.deliverDirect(webhook, entry);

        // Transaction 3: persist outcome
        updateResult(entry, webhook, result);
    }

    /**
     * Transaction 3 (success/failure path): persist delivery record + update outbox + webhook.
     * Sets RLS session variable FIRST (before any writes) so all writes within this transaction
     * are allowed by the webhooks table RLS policy.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateResult(WebhookOutboxEntity entry, WebhookEntity webhook,
                             WebhookDispatcher.DeliveryResult result) {
        // SET LOCAL first — before any writes — so RLS allows all writes in this transaction.
        // webhook_outbox has no RLS; webhooks does (org_id = current_setting('app.current_org_id')).
        jdbcTemplate.execute("SET LOCAL app.current_org_id = '" + entry.getOrgId() + "'");

        // Record delivery
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
            entry.setStatus("DELIVERED");
            entry.setLastStatusCode(result.statusCode());
            outboxRepository.save(entry);

            webhook.setConsecutiveFailures(0);
            webhook.setLastDeliveredAt(OffsetDateTime.now());
            webhookRepository.save(webhook);

            log.debug("Successfully delivered outbox entry {} to webhook {}",
                    entry.getId(), webhook.getId());
        } else {
            // Use NEW attempt count (after increment) for backoff indexing
            int newAttemptCount = entry.getAttemptCount() + 1;
            entry.setAttemptCount(newAttemptCount);
            entry.setLastStatusCode(result.statusCode());
            entry.setLastError(result.errorMessage());

            if (newAttemptCount >= MAX_ATTEMPTS) {
                entry.setStatus("DLQ");
                outboxRepository.save(entry);

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
                // Use newAttemptCount as index: 1st retry → index 1 → 30s
                entry.setNextAttemptAt(computeNextAttempt(newAttemptCount));
                outboxRepository.save(entry);
            }

            log.debug("Delivery failed for outbox entry {} (attempt {}/{}): {}",
                    entry.getId(), newAttemptCount, MAX_ATTEMPTS, result.errorMessage());
        }
    }

    /**
     * Transaction 3 (DLQ path): mark outbox entry as DLQ without webhook write.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateResultDlq(WebhookOutboxEntity entry, String reason) {
        entry.setStatus("DLQ");
        entry.setLastError(reason);
        outboxRepository.save(entry);
    }

    private OffsetDateTime computeNextAttempt(int newAttemptCount) {
        // newAttemptCount is already incremented (1 = 1st retry, 2 = 2nd, ...)
        // Use it directly as the index so 1st retry → BACKOFF_SECONDS[1] = 30s
        int index = Math.min(newAttemptCount, BACKOFF_SECONDS.length - 1);
        long baseDelay = BACKOFF_SECONDS[index];
        long jitter = baseDelay > 0
                ? ThreadLocalRandom.current().nextLong(0, Math.max(1, baseDelay / 5))
                : 0;
        return OffsetDateTime.now().plusSeconds(baseDelay + jitter);
    }

    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void cleanupOldDlq() {
        // Cross-tenant maintenance task: webhook_outbox has NO RLS policy (only the webhooks
        // table has one — see V048). Deleting old DLQ rows across all orgs is intentional and
        // safe without setting app.current_org_id. No SET LOCAL is required here.
        int deleted = outboxRepository.deleteOldDlqEntries(OffsetDateTime.now().minusDays(30));
        if (deleted > 0) {
            log.info("Cleaned up {} old DLQ outbox entries", deleted);
        }
    }
}
