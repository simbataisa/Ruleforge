package io.ruleforge.event.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.ruleforge.common.dto.PageResponse;
import io.ruleforge.common.exception.EntityNotFoundException;
import io.ruleforge.event.dto.CreateWebhookRequest;
import io.ruleforge.event.dto.UpdateWebhookRequest;
import io.ruleforge.event.dto.WebhookDeliveryDto;
import io.ruleforge.event.dto.WebhookDto;
import io.ruleforge.event.entity.WebhookEntity;
import io.ruleforge.event.repository.WebhookDeliveryRepository;
import io.ruleforge.event.repository.WebhookRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class WebhookService {

    private static final Logger log = LoggerFactory.getLogger(WebhookService.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int SECRET_LENGTH_BYTES = 32;

    private final WebhookRepository webhookRepository;
    private final WebhookDeliveryRepository webhookDeliveryRepository;
    private final ObjectMapper objectMapper;

    public WebhookService(WebhookRepository webhookRepository,
                          WebhookDeliveryRepository webhookDeliveryRepository,
                          ObjectMapper objectMapper) {
        this.webhookRepository = webhookRepository;
        this.webhookDeliveryRepository = webhookDeliveryRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public WebhookDto createWebhook(UUID spaceId, CreateWebhookRequest request) {
        WebhookEntity entity = new WebhookEntity();
        entity.setSpaceId(spaceId);
        entity.setUrl(request.url());
        entity.setEvents(serializeEvents(request.events()));
        entity.setSecret(generateSecret());
        entity.setActive(true);

        WebhookEntity saved = webhookRepository.save(entity);
        log.info("Created webhook {} for space {}", saved.getId(), spaceId);
        return toDto(saved);
    }

    @Transactional
    public WebhookDto updateWebhook(UUID spaceId, UUID webhookId, UpdateWebhookRequest request) {
        WebhookEntity entity = webhookRepository.findByIdAndSpaceId(webhookId, spaceId)
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

        WebhookEntity saved = webhookRepository.save(entity);
        log.info("Updated webhook {} for space {}", webhookId, spaceId);
        return toDto(saved);
    }

    @Transactional
    public void deleteWebhook(UUID spaceId, UUID webhookId) {
        WebhookEntity entity = webhookRepository.findByIdAndSpaceId(webhookId, spaceId)
                .orElseThrow(() -> new EntityNotFoundException("Webhook", webhookId.toString()));
        webhookRepository.delete(entity);
        log.info("Deleted webhook {} for space {}", webhookId, spaceId);
    }

    @Transactional(readOnly = true)
    public List<WebhookDto> listWebhooks(UUID spaceId) {
        return webhookRepository.findBySpaceId(spaceId).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public WebhookDto getWebhook(UUID spaceId, UUID webhookId) {
        WebhookEntity entity = webhookRepository.findByIdAndSpaceId(webhookId, spaceId)
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

    private WebhookDto toDto(WebhookEntity entity) {
        return new WebhookDto(
                entity.getId(),
                entity.getSpaceId(),
                entity.getUrl(),
                deserializeEvents(entity.getEvents()),
                entity.isActive(),
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
