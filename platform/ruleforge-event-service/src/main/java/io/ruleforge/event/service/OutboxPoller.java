package io.ruleforge.event.service;

import io.ruleforge.common.event.DomainEvent;
import io.ruleforge.event.entity.WebhookDeliveryEntity;
import io.ruleforge.event.entity.WebhookEntity;
import io.ruleforge.event.entity.WebhookOutboxEntity;
import io.ruleforge.event.repository.WebhookDeliveryRepository;
import io.ruleforge.event.repository.WebhookOutboxRepository;
import io.ruleforge.event.repository.WebhookRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
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
import java.util.concurrent.TimeUnit;
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
    private final SsrfGuard ssrfGuard;
    private final AuditEventService auditEventService;
    private final StringRedisTemplate redisTemplate;

    public OutboxPoller(WebhookOutboxRepository outboxRepository,
                        WebhookRepository webhookRepository,
                        WebhookDeliveryRepository deliveryRepository,
                        WebhookDispatcher webhookDispatcher,
                        JdbcTemplate jdbcTemplate,
                        SsrfGuard ssrfGuard,
                        AuditEventService auditEventService,
                        StringRedisTemplate redisTemplate) {
        this.outboxRepository = outboxRepository;
        this.webhookRepository = webhookRepository;
        this.deliveryRepository = deliveryRepository;
        this.webhookDispatcher = webhookDispatcher;
        this.jdbcTemplate = jdbcTemplate;
        this.ssrfGuard = ssrfGuard;
        this.auditEventService = auditEventService;
        this.redisTemplate = redisTemplate;
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

        // SSRF guard: validate URL before every HTTP delivery attempt.
        // This is a PERMANENT failure — if the URL resolves to a blocked range, no retry is useful.
        // Set status=DLQ immediately without incrementing the attempt retry loop.
        try {
            ssrfGuard.validate(webhook.getUrl());
        } catch (IllegalArgumentException e) {
            log.warn("SSRF guard blocked delivery for webhook {} (entry {}): {}",
                    webhook.getId(), entry.getId(), e.getMessage());
            updateResultDlq(entry, "SSRF blocked: " + e.getMessage());
            return;
        }

        // Rate limiting: check per-webhook and per-org limits before delivery.
        // If rate limited, reschedule for 60 seconds later without counting as a failure.
        if (isRateLimited(webhook.getId(), webhook.getOrgId())) {
            log.debug("Rate limited delivery for webhook {} (entry {}), rescheduling in 60s",
                    webhook.getId(), entry.getId());
            updateRateLimited(entry);
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
        // Safety: UUID.fromString() validates the value before concatenation; UUID.toString() only
        // contains hex digits and hyphens, making string concatenation safe here. PostgreSQL JDBC
        // drivers do not support '?' parameters in SET LOCAL statements.
        UUID validatedOrgId = UUID.fromString(entry.getOrgId().toString());
        jdbcTemplate.execute("SET LOCAL app.current_org_id = '" + validatedOrgId + "'");

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

            boolean circuitWasOpen = webhook.getCircuitOpenedAt() != null;
            webhook.setConsecutiveFailures(0);
            webhook.setLastDeliveredAt(OffsetDateTime.now());
            webhookRepository.save(webhook);

            if (circuitWasOpen) {
                // Circuit closed automatically on successful delivery; webhook re-enable requires
                // explicit POST /reset-circuit call — we only log the recovery here.
                log.info("Webhook {} successfully delivered after circuit was open; " +
                        "circuit auto-closed (consecutiveFailures reset). " +
                        "Webhook remains inactive until POST /reset-circuit is called.",
                        webhook.getId());
            }

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

                    // Write audit event for circuit breaker opening
                    try {
                        DomainEvent circuitEvent = DomainEvent.create(
                                "WEBHOOK_CIRCUIT_OPENED",
                                "Webhook",
                                webhook.getId().toString(),
                                webhook.getOrgId(),
                                null,
                                Map.of(
                                        "webhookId", webhook.getId().toString(),
                                        "consecutiveFailures", newFailures,
                                        "lastError", result.errorMessage() != null ? result.errorMessage() : ""
                                )
                        );
                        auditEventService.recordEvent(circuitEvent);
                    } catch (Exception auditEx) {
                        log.warn("Failed to write WEBHOOK_CIRCUIT_OPENED audit event for webhook {}: {}",
                                webhook.getId(), auditEx.getMessage());
                    }
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

    /**
     * Checks per-webhook (60/min) and per-org (300/min) Redis rate limits.
     * Fails open if Redis is unavailable — delivery proceeds without rate limiting.
     */
    private boolean isRateLimited(UUID webhookId, UUID orgId) {
        try {
            // Per-webhook: max 60 deliveries per minute
            String webhookKey = "ratelimit:webhook:" + webhookId;
            Long webhookCount = redisTemplate.opsForValue().increment(webhookKey);
            if (webhookCount != null && webhookCount == 1) {
                redisTemplate.expire(webhookKey, 60, TimeUnit.SECONDS);
            }
            if (webhookCount != null && webhookCount > 60) {
                log.debug("Rate limit exceeded for webhook {}: {}/60 per minute", webhookId, webhookCount);
                return true;
            }

            // Per-org: max 300 deliveries per minute
            String orgKey = "ratelimit:org:" + orgId;
            Long orgCount = redisTemplate.opsForValue().increment(orgKey);
            if (orgCount != null && orgCount == 1) {
                redisTemplate.expire(orgKey, 60, TimeUnit.SECONDS);
            }
            if (orgCount != null && orgCount > 300) {
                log.debug("Rate limit exceeded for org {}: {}/300 per minute", orgId, orgCount);
                return true;
            }
        } catch (Exception e) {
            // Fail open: if Redis is unavailable, proceed without rate limiting
            log.warn("Redis unavailable for rate limiting (webhook={}, org={}), proceeding without rate limit: {}",
                    webhookId, orgId, e.getMessage());
        }
        return false;
    }

    /**
     * Rate-limited path: reschedule the entry for 60 seconds later without counting as a failure.
     * The attempt count is NOT incremented — rate limiting is transparent to the retry budget.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateRateLimited(WebhookOutboxEntity entry) {
        entry.setNextAttemptAt(OffsetDateTime.now().plusSeconds(60));
        entry.setStatus("PENDING");
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
