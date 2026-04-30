package io.ruleforge.event.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.ruleforge.event.delivery.OAuth2ClientCredentialsInjector;
import io.ruleforge.event.delivery.WebhookAuthInjectorRegistry;
import io.ruleforge.event.exception.WebhookAuthException;
import io.ruleforge.event.exception.WebhookCredentialDecryptionException;
import io.ruleforge.event.entity.WebhookEntity;
import io.ruleforge.event.entity.WebhookOutboxEntity;
import io.ruleforge.event.model.WebhookAuthConfig;
import io.ruleforge.event.model.WebhookAuthType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Handles direct HTTP delivery of webhook payloads.
 * Used by OutboxPoller for production delivery and by the test endpoint.
 */
@Service
public class WebhookDispatcher {

    private static final Logger log = LoggerFactory.getLogger(WebhookDispatcher.class);
    private static final int MAX_STORED_RESPONSE_BODY_LENGTH = 255;

    private final RestClient webhookClient;
    private final ObjectMapper objectMapper;
    private final WebhookAuthInjectorRegistry webhookAuthInjectorRegistry;
    private final CredentialEncryptionService credentialEncryptionService;

    public WebhookDispatcher(RestClient webhookClient,
                             ObjectMapper objectMapper,
                             WebhookAuthInjectorRegistry webhookAuthInjectorRegistry,
                             CredentialEncryptionService credentialEncryptionService) {
        this.webhookClient = webhookClient;
        this.objectMapper = objectMapper;
        this.webhookAuthInjectorRegistry = webhookAuthInjectorRegistry;
        this.credentialEncryptionService = credentialEncryptionService;
    }

    /**
     * Result of a direct webhook delivery attempt.
     */
    public record DeliveryResult(
            boolean success,
            int statusCode,
            String responseBody,
            String payloadJson,
            String errorMessage,
            boolean permanent,
            boolean authFailure
    ) {
        /** Backwards-compatible 6-arg constructor — authFailure defaults to false. */
        public DeliveryResult(boolean success, int statusCode, String responseBody,
                              String payloadJson, String errorMessage, boolean permanent) {
            this(success, statusCode, responseBody, payloadJson, errorMessage, permanent, false);
        }
    }

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
                    "Payload serialization failed: " + e.getMessage(), false);
        }

        long timestamp = Instant.now().getEpochSecond();
        String nonce = outboxEntry.getAttemptNonce() != null
                ? outboxEntry.getAttemptNonce().toString()
                : UUID.randomUUID().toString();

        byte[] payloadBytes = payloadJson.getBytes(StandardCharsets.UTF_8);

        // Inject auth via strategy — decrypt failure is permanent (do not retry)
        HttpHeaders authHeaders = new HttpHeaders();
        WebhookAuthConfig authConfig;
        try {
            authConfig = resolveAuthConfig(webhook);
        } catch (WebhookCredentialDecryptionException e) {
            log.error("Credential decryption failed for webhook {}, marking permanent failure: {}",
                    webhook.getId(), e.getMessage());
            return new DeliveryResult(false, 0, null, payloadJson,
                    "CREDENTIAL_DECRYPT_ERROR: " + e.getMessage(), true);
        }
        try {
            var injector = webhookAuthInjectorRegistry.getFor(webhook.getAuthType());
            if (authConfig instanceof WebhookAuthConfig.OAuth2ClientCredentials
                    && injector instanceof OAuth2ClientCredentialsInjector oauth2Injector) {
                oauth2Injector.injectAuth(authHeaders, authConfig, payloadBytes,
                        webhook.getOrgId(), webhook.getId());
            } else {
                injector.injectAuth(authHeaders, authConfig, payloadBytes);
            }
        } catch (WebhookAuthException e) {
            log.warn("Auth injection failed for webhook {}, marking permanent auth failure: {}",
                    webhook.getId(), e.getMessage());
            return new DeliveryResult(false, 0, null, payloadJson,
                    "AUTH_FAILURE: " + e.getMessage(), true, true);
        }

        try {
            var requestSpec = webhookClient.post()
                    .uri(webhook.getUrl())
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Webhook-Timestamp", String.valueOf(timestamp))
                    .header("X-Webhook-Delivery-Id", nonce)
                    .header("X-Webhook-Id", webhook.getId().toString())
                    .header("X-Webhook-Schema-Version", "1")
                    .header("X-Ruleforge-Event", outboxEntry.getEventType());

            // Apply auth headers from injector
            authHeaders.forEach((name, values) -> values.forEach(value -> requestSpec.header(name, value)));

            var response = requestSpec
                    .body(payloadJson)
                    .retrieve()
                    .toEntity(String.class);

            int statusCode = response.getStatusCode().value();
            boolean success = response.getStatusCode().is2xxSuccessful();
            String responseBody = truncateResponse(response.getBody());

            log.debug("Webhook {} delivered to {} with status {}",
                    webhook.getId(), webhook.getUrl(), statusCode);

            return new DeliveryResult(success, statusCode, responseBody, payloadJson,
                    success ? null : "HTTP " + statusCode, false);
        } catch (Exception e) {
            log.warn("Webhook delivery failed for webhook {} to {}: {}",
                    webhook.getId(), webhook.getUrl(), e.getMessage());
            return new DeliveryResult(false, 0, null, payloadJson, e.getMessage(), false);
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
                    "Payload serialization failed: " + e.getMessage(), false);
        }

        long timestamp = Instant.now().getEpochSecond();
        String nonce = UUID.randomUUID().toString();
        byte[] payloadBytes = payloadJson.getBytes(StandardCharsets.UTF_8);

        // Inject auth via strategy — decrypt failure is permanent (do not retry)
        HttpHeaders authHeaders = new HttpHeaders();
        WebhookAuthConfig authConfig;
        try {
            authConfig = resolveAuthConfig(webhook);
        } catch (WebhookCredentialDecryptionException e) {
            log.error("Credential decryption failed for webhook {} (test), marking permanent failure: {}",
                    webhook.getId(), e.getMessage());
            return new DeliveryResult(false, 0, null, payloadJson,
                    "CREDENTIAL_DECRYPT_ERROR: " + e.getMessage(), true);
        }
        try {
            var injector = webhookAuthInjectorRegistry.getFor(webhook.getAuthType());
            if (authConfig instanceof WebhookAuthConfig.OAuth2ClientCredentials
                    && injector instanceof OAuth2ClientCredentialsInjector oauth2Injector) {
                oauth2Injector.injectAuth(authHeaders, authConfig, payloadBytes,
                        webhook.getOrgId(), webhook.getId());
            } else {
                injector.injectAuth(authHeaders, authConfig, payloadBytes);
            }
        } catch (WebhookAuthException e) {
            log.warn("Auth injection failed for webhook {} (test), marking permanent auth failure: {}",
                    webhook.getId(), e.getMessage());
            return new DeliveryResult(false, 0, null, payloadJson,
                    "AUTH_FAILURE: " + e.getMessage(), true, true);
        }

        try {
            var requestSpec = webhookClient.post()
                    .uri(webhook.getUrl())
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Webhook-Timestamp", String.valueOf(timestamp))
                    .header("X-Webhook-Delivery-Id", nonce)
                    .header("X-Webhook-Id", webhook.getId().toString())
                    .header("X-Webhook-Schema-Version", "1")
                    .header("X-Ruleforge-Event", "webhook.test");

            // Apply auth headers from injector
            authHeaders.forEach((name, values) -> values.forEach(value -> requestSpec.header(name, value)));

            var response = requestSpec
                    .body(payloadJson)
                    .retrieve()
                    .toEntity(String.class);

            int statusCode = response.getStatusCode().value();
            boolean success = response.getStatusCode().is2xxSuccessful();
            return new DeliveryResult(success, statusCode, truncateResponse(response.getBody()),
                    payloadJson, success ? null : "HTTP " + statusCode, false);
        } catch (Exception e) {
            return new DeliveryResult(false, 0, null, payloadJson, e.getMessage(), false);
        }
    }

    private WebhookAuthConfig resolveAuthConfig(WebhookEntity webhook) {
        if (webhook.getAuthType() == null || webhook.getAuthType() == WebhookAuthType.NONE) {
            return new WebhookAuthConfig.None();
        }
        if (webhook.getAuthConfig() != null) {
            return credentialEncryptionService.decrypt(webhook.getAuthConfig(), webhook.getAuthType());
        }
        // Legacy fallback: auth_config not yet migrated, use raw secret column
        if (webhook.getSecret() != null) {
            return new WebhookAuthConfig.SigningSecret(webhook.getSecret());
        }
        return new WebhookAuthConfig.None();
    }

    private String truncateResponse(String body) {
        if (body == null) {
            return null;
        }
        return body.length() > MAX_STORED_RESPONSE_BODY_LENGTH
                ? body.substring(0, MAX_STORED_RESPONSE_BODY_LENGTH)
                : body;
    }
}
