package io.ruleforge.event.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.ruleforge.common.event.DomainEvent;
import io.ruleforge.event.entity.WebhookDeliveryEntity;
import io.ruleforge.event.entity.WebhookEntity;
import io.ruleforge.event.repository.WebhookDeliveryRepository;
import io.ruleforge.event.repository.WebhookRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
public class WebhookDispatcher {

    private static final Logger log = LoggerFactory.getLogger(WebhookDispatcher.class);
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String SIGNATURE_HEADER = "X-Webhook-Signature";

    private final WebhookRepository webhookRepository;
    private final WebhookDeliveryRepository webhookDeliveryRepository;
    private final RestClient webhookClient;
    private final ObjectMapper objectMapper;

    public WebhookDispatcher(WebhookRepository webhookRepository,
                             WebhookDeliveryRepository webhookDeliveryRepository,
                             RestClient webhookClient,
                             ObjectMapper objectMapper) {
        this.webhookRepository = webhookRepository;
        this.webhookDeliveryRepository = webhookDeliveryRepository;
        this.webhookClient = webhookClient;
        this.objectMapper = objectMapper;
    }

    @Async
    public void dispatch(UUID spaceId, DomainEvent event) {
        List<WebhookEntity> activeWebhooks = webhookRepository.findBySpaceIdAndActiveTrue(spaceId);

        for (WebhookEntity webhook : activeWebhooks) {
            if (isSubscribedToEvent(webhook, event.eventType())) {
                deliverWebhook(webhook, event);
            }
        }
    }

    private boolean isSubscribedToEvent(WebhookEntity webhook, String eventType) {
        List<String> subscribedEvents = deserializeEvents(webhook.getEvents());
        if (subscribedEvents.isEmpty()) {
            return false;
        }
        // Support wildcard "*" to subscribe to all events
        if (subscribedEvents.contains("*")) {
            return true;
        }
        // Support prefix matching, e.g. "rule.*" matches "rule.created"
        for (String subscribed : subscribedEvents) {
            if (subscribed.equals(eventType)) {
                return true;
            }
            if (subscribed.endsWith(".*")) {
                String prefix = subscribed.substring(0, subscribed.length() - 1);
                if (eventType.startsWith(prefix)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void deliverWebhook(WebhookEntity webhook, DomainEvent event) {
        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize event {} for webhook {}: {}",
                    event.eventId(), webhook.getId(), e.getMessage());
            return;
        }

        String signature = computeSignature(webhook.getSecret(), payloadJson);

        WebhookDeliveryEntity delivery = new WebhookDeliveryEntity();
        delivery.setWebhookId(webhook.getId());
        delivery.setEventType(event.eventType());
        delivery.setPayload(payloadJson);
        delivery.setDeliveredAt(Instant.now());

        try {
            var response = webhookClient.post()
                    .uri(webhook.getUrl())
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(SIGNATURE_HEADER, signature)
                    .body(payloadJson)
                    .retrieve()
                    .toEntity(String.class);

            int statusCode = response.getStatusCode().value();
            delivery.setStatusCode(statusCode);
            delivery.setResponseBody(truncateResponse(response.getBody()));
            delivery.setSuccess(response.getStatusCode().is2xxSuccessful());

            log.debug("Webhook {} delivered to {} with status {}",
                    webhook.getId(), webhook.getUrl(), statusCode);

        } catch (Exception e) {
            log.warn("Webhook delivery failed for webhook {} to {}: {}",
                    webhook.getId(), webhook.getUrl(), e.getMessage());
            delivery.setStatusCode(0);
            delivery.setResponseBody(e.getMessage());
            delivery.setSuccess(false);
        }

        webhookDeliveryRepository.save(delivery);
    }

    private String computeSignature(String secret, String payload) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(
                    secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
            mac.init(keySpec);
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return "sha256=" + HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("Failed to compute HMAC-SHA256 signature", e);
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

    private String truncateResponse(String body) {
        if (body == null) {
            return null;
        }
        return body.length() > 1024 ? body.substring(0, 1024) : body;
    }
}
