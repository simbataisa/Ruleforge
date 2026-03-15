package io.ruleforge.event.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.ruleforge.common.dto.PageResponse;
import io.ruleforge.common.exception.EntityNotFoundException;
import io.ruleforge.common.event.DomainEvent;
import io.ruleforge.event.dto.CreateWebhookRequest;
import io.ruleforge.event.dto.UpdateWebhookRequest;
import io.ruleforge.event.dto.WebhookDeliveryDto;
import io.ruleforge.event.dto.WebhookDto;
import io.ruleforge.event.entity.WebhookEntity;
import io.ruleforge.event.model.WebhookScopeType;
import io.ruleforge.event.repository.WebhookDeliveryRepository;
import io.ruleforge.event.repository.WebhookRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class WebhookService {

    private static final Logger log = LoggerFactory.getLogger(WebhookService.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int SECRET_LENGTH_BYTES = 32;

    private final WebhookRepository webhookRepository;
    private final WebhookDeliveryRepository webhookDeliveryRepository;
    private final ObjectMapper objectMapper;
    private final SsrfGuard ssrfGuard;
    private final AuditEventService auditEventService;

    public WebhookService(WebhookRepository webhookRepository,
                          WebhookDeliveryRepository webhookDeliveryRepository,
                          ObjectMapper objectMapper,
                          SsrfGuard ssrfGuard,
                          AuditEventService auditEventService) {
        this.webhookRepository = webhookRepository;
        this.webhookDeliveryRepository = webhookDeliveryRepository;
        this.objectMapper = objectMapper;
        this.ssrfGuard = ssrfGuard;
        this.auditEventService = auditEventService;
    }

    @Transactional
    public WebhookDto createWebhook(UUID orgId, UUID userId, CreateWebhookRequest request) {
        // Validate URL for SSRF before saving
        ssrfGuard.validate(request.url());

        WebhookEntity entity = new WebhookEntity();
        entity.setName(request.name());
        entity.setScopeType(request.scopeType());
        entity.setScopeId(request.scopeId());
        entity.setUrl(request.url());
        entity.setEvents(serializeEvents(request.events()));
        entity.setActive(true);
        entity.setCreatedBy(userId);

        // Resolve orgId: for ORG scope, scopeId IS the orgId.
        // For other scopes, use the orgId from TenantContext.
        if (request.scopeType() == WebhookScopeType.ORG) {
            entity.setOrgId(request.scopeId());
        } else {
            entity.setOrgId(orgId);
        }

        String secret = generateSecret();
        entity.setSecret(secret);

        WebhookEntity saved = webhookRepository.save(entity);
        log.info("Created webhook {} (scope={}, scopeId={})",
                saved.getId(), request.scopeType(), request.scopeId());

        // Return DTO with secret visible on creation
        return toDto(saved, secret);
    }

    @Transactional
    public WebhookDto updateWebhook(UUID orgId, UUID webhookId, UpdateWebhookRequest request) {
        WebhookEntity entity = webhookRepository.findByIdAndOrgId(webhookId, orgId)
                .orElseThrow(() -> new EntityNotFoundException("Webhook", webhookId.toString()));

        if (request.url() != null) {
            entity.setUrl(request.url());
        }
        if (request.events() != null) {
            entity.setEvents(serializeEvents(request.events()));
        }
        if (request.active() != null) {
            entity.setActive(request.active());
        }
        if (request.name() != null) {
            entity.setName(request.name());
        }

        WebhookEntity saved = webhookRepository.save(entity);
        log.info("Updated webhook {} for org {}", webhookId, orgId);
        return toDto(saved);
    }

    @Transactional
    public void deleteWebhook(UUID orgId, UUID webhookId) {
        WebhookEntity entity = webhookRepository.findByIdAndOrgId(webhookId, orgId)
                .orElseThrow(() -> new EntityNotFoundException("Webhook", webhookId.toString()));
        webhookRepository.delete(entity);
        log.info("Deleted webhook {} for org {}", webhookId, orgId);
    }

    @Transactional(readOnly = true)
    public List<WebhookDto> listWebhooks(UUID orgId, WebhookScopeType scopeType, UUID scopeId) {
        List<WebhookEntity> entities;
        if (scopeType != null && scopeId != null) {
            entities = webhookRepository.findByScopeTypeAndScopeId(scopeType, scopeId);
        } else {
            entities = webhookRepository.findByOrgId(orgId);
        }
        return entities.stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public WebhookDto getWebhook(UUID orgId, UUID webhookId) {
        WebhookEntity entity = webhookRepository.findByIdAndOrgId(webhookId, orgId)
                .orElseThrow(() -> new EntityNotFoundException("Webhook", webhookId.toString()));
        return toDto(entity);
    }

    @Transactional(readOnly = true)
    public PageResponse<WebhookDeliveryDto> getDeliveries(UUID webhookId, Pageable pageable) {
        var page = webhookDeliveryRepository.findByWebhookIdOrderByDeliveredAtDesc(webhookId, pageable);
        return new PageResponse<>(
                page.getContent().stream().map(e -> new WebhookDeliveryDto(
                        e.getId(),
                        e.getWebhookId(),
                        e.getEventType(),
                        e.getStatusCode(),
                        e.isSuccess(),
                        e.getDeliveredAt()
                )).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }

    @Transactional
    public WebhookDto resetCircuit(UUID orgId, UUID webhookId) {
        WebhookEntity entity = webhookRepository.findByIdAndOrgId(webhookId, orgId)
                .orElseThrow(() -> new EntityNotFoundException("Webhook", webhookId.toString()));
        entity.setConsecutiveFailures(0);
        entity.setCircuitOpenedAt(null);
        entity.setActive(true);
        WebhookEntity saved = webhookRepository.save(entity);
        log.info("Reset circuit breaker for webhook {}", webhookId);
        return toDto(saved);
    }

    /**
     * Get the raw webhook entity for use by dispatcher/controller.
     */
    @Transactional(readOnly = true)
    public WebhookEntity getWebhookEntity(UUID orgId, UUID webhookId) {
        return webhookRepository.findByIdAndOrgId(webhookId, orgId)
                .orElseThrow(() -> new EntityNotFoundException("Webhook", webhookId.toString()));
    }

    /**
     * Rotate the signing secret for the given webhook.
     * Returns the new secret — shown exactly once to the caller.
     */
    @Transactional
    public String rotateSecret(UUID webhookId, UUID orgId, UUID userId) {
        WebhookEntity entity = webhookRepository.findByIdAndOrgId(webhookId, orgId)
                .orElseThrow(() -> new EntityNotFoundException("Webhook", webhookId.toString()));

        String newSecret = generateSecret();
        entity.setSecret(newSecret);
        webhookRepository.save(entity);

        log.info("Rotated secret for webhook {} by user {}", webhookId, userId);

        // Write audit event
        DomainEvent auditEvent = DomainEvent.create(
                "WEBHOOK_SECRET_ROTATED",
                "Webhook",
                webhookId.toString(),
                orgId,
                userId,
                Map.of("webhookId", webhookId.toString(), "webhookName", entity.getName() != null ? entity.getName() : "")
        );
        auditEventService.recordEvent(auditEvent);

        return newSecret;
    }

    private WebhookDto toDto(WebhookEntity entity) {
        return toDto(entity, null);
    }

    private WebhookDto toDto(WebhookEntity entity, String secretOnCreation) {
        String circuitStatus = entity.getCircuitOpenedAt() != null ? "OPEN" : "CLOSED";
        return new WebhookDto(
                entity.getId(),
                entity.getName(),
                entity.getScopeType(),
                entity.getScopeId(),
                entity.getOrgId(),
                entity.getUrl(),
                deserializeEvents(entity.getEvents()),
                entity.isActive(),
                circuitStatus,
                entity.getConsecutiveFailures(),
                entity.getLastDeliveredAt(),
                secretOnCreation,
                entity.getCreatedAt()
        );
    }

    private String serializeEvents(List<String> events) {
        try {
            return objectMapper.writeValueAsString(events);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to serialize events list", e);
        }
    }

    private List<String> deserializeEvents(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            log.warn("Failed to deserialize webhook events: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private String generateSecret() {
        byte[] bytes = new byte[SECRET_LENGTH_BYTES];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
