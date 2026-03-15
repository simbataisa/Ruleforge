package io.ruleforge.event.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.ruleforge.event.entity.WebhookEntity;
import io.ruleforge.event.entity.WebhookOutboxEntity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;

/**
 * Handles direct HTTP delivery of webhook payloads.
 * Used by OutboxPoller for production delivery and by the test endpoint.
 */
@Service
public class WebhookDispatcher {

    private static final Logger log = LoggerFactory.getLogger(WebhookDispatcher.class);
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String SIGNATURE_HEADER = "X-Webhook-Signature";

    private final RestClient webhookClient;
    private final ObjectMapper objectMapper;

    public WebhookDispatcher(RestClient webhookClient, ObjectMapper objectMapper) {
        this.webhookClient = webhookClient;
        this.objectMapper = objectMapper;
    }

    /**
     * Result of a direct webhook delivery attempt.
     */
    public record DeliveryResult(
            boolean success,
            int statusCode,
            String responseBody,
            String payloadJson,
            String errorMessage
    ) {}

    /**
     * Deliver a webhook payload directly via HTTP POST.
     *
     * @param webhook      the webhook entity with url and secret
     * @param outboxEntry  the outbox entry containing the payload
     * @return delivery result
     */
    public DeliveryResult deliverDirect(WebhookEntity webhook, WebhookOutboxEntity outboxEntry) {
        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(outboxEntry.getPayload());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize payload for webhook {}: {}",
                    webhook.getId(), e.getMessage());
            return new DeliveryResult(false, 0, null, null,
                    "Payload serialization failed: " + e.getMessage());
        }

        String signature = computeSignature(webhook.getSecret(), payloadJson);

        try {
            var response = webhookClient.post()
                    .uri(webhook.getUrl())
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(SIGNATURE_HEADER, signature)
                    .body(payloadJson)
                    .retrieve()
                    .toEntity(String.class);

            int statusCode = response.getStatusCode().value();
            boolean success = response.getStatusCode().is2xxSuccessful();
            String responseBody = truncateResponse(response.getBody());

            log.debug("Webhook {} delivered to {} with status {}",
                    webhook.getId(), webhook.getUrl(), statusCode);

            return new DeliveryResult(success, statusCode, responseBody, payloadJson,
                    success ? null : "HTTP " + statusCode);
        } catch (Exception e) {
            log.warn("Webhook delivery failed for webhook {} to {}: {}",
                    webhook.getId(), webhook.getUrl(), e.getMessage());
            return new DeliveryResult(false, 0, null, payloadJson, e.getMessage());
        }
    }

    /**
     * Deliver a test payload directly (for the test endpoint).
     *
     * @param webhook the webhook entity
     * @param testPayload a synthetic test payload map
     * @return delivery result
     */
    public DeliveryResult deliverTestPayload(WebhookEntity webhook, Map<String, Object> testPayload) {
        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(testPayload);
        } catch (JsonProcessingException e) {
            return new DeliveryResult(false, 0, null, null,
                    "Payload serialization failed: " + e.getMessage());
        }

        String signature = computeSignature(webhook.getSecret(), payloadJson);

        try {
            var response = webhookClient.post()
                    .uri(webhook.getUrl())
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(SIGNATURE_HEADER, signature)
                    .body(payloadJson)
                    .retrieve()
                    .toEntity(String.class);

            int statusCode = response.getStatusCode().value();
            boolean success = response.getStatusCode().is2xxSuccessful();
            return new DeliveryResult(success, statusCode, truncateResponse(response.getBody()),
                    payloadJson, success ? null : "HTTP " + statusCode);
        } catch (Exception e) {
            return new DeliveryResult(false, 0, null, payloadJson, e.getMessage());
        }
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

    private String truncateResponse(String body) {
        if (body == null) {
            return null;
        }
        return body.length() > 1024 ? body.substring(0, 1024) : body;
    }
}
