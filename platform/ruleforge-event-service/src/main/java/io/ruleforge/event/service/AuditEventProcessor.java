package io.ruleforge.event.service;

import io.ruleforge.common.event.DomainEvent;
import io.ruleforge.event.entity.WebhookEntity;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Processes Kafka domain events transactionally: records the audit event and
 * enqueues outbox entries for matching webhooks.
 *
 * Extracted into a separate @Service bean so that Spring AOP can intercept
 * the @Transactional annotation. (Calling a @Transactional protected method
 * on the same object via 'this' bypasses the proxy and silently drops the
 * transaction boundary.)
 */
@Service
public class AuditEventProcessor {

    private final AuditEventService auditEventService;
    private final WebhookScopeResolver webhookScopeResolver;
    private final WebhookOutboxService webhookOutboxService;

    public AuditEventProcessor(AuditEventService auditEventService,
                                WebhookScopeResolver webhookScopeResolver,
                                WebhookOutboxService webhookOutboxService) {
        this.auditEventService = auditEventService;
        this.webhookScopeResolver = webhookScopeResolver;
        this.webhookOutboxService = webhookOutboxService;
    }

    @Transactional
    public void processEvent(DomainEvent event) {
        auditEventService.recordEvent(event);
        List<WebhookEntity> matched = webhookScopeResolver.resolve(event);
        if (!matched.isEmpty()) {
            webhookOutboxService.enqueue(event, matched);
        }
    }
}
