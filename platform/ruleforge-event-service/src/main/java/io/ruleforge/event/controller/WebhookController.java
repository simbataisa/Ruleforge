package io.ruleforge.event.controller;

import io.ruleforge.common.dto.ApiResponse;
import io.ruleforge.common.dto.PageResponse;
import io.ruleforge.common.tenant.TenantContext;
import io.ruleforge.event.dto.CreateWebhookRequest;
import io.ruleforge.event.dto.UpdateWebhookRequest;
import io.ruleforge.event.dto.WebhookAuthConfigRequest;
import io.ruleforge.event.dto.WebhookDeliveryDto;
import io.ruleforge.event.dto.WebhookDto;
import io.ruleforge.event.dto.WebhookMetricsDto;
import io.ruleforge.event.dto.WebhookOutboxDto;
import io.ruleforge.event.entity.WebhookDeliveryEntity;
import io.ruleforge.event.entity.WebhookEntity;
import io.ruleforge.event.entity.WebhookOutboxEntity;
import io.ruleforge.event.model.WebhookAuthType;
import io.ruleforge.event.model.WebhookScopeType;
import io.ruleforge.event.repository.WebhookDeliveryRepository;
import io.ruleforge.event.repository.WebhookOutboxRepository;
import io.ruleforge.event.repository.WebhookRepository;
import io.ruleforge.event.service.SsrfGuard;
import io.ruleforge.event.service.WebhookDispatcher;
import io.ruleforge.event.service.WebhookMetricsService;
import io.ruleforge.event.service.WebhookOutboxService;
import io.ruleforge.event.service.WebhookService;

import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/v1/webhooks")
public class WebhookController {

    private final WebhookService webhookService;
    private final WebhookDispatcher webhookDispatcher;
    private final WebhookDeliveryRepository webhookDeliveryRepository;
    private final WebhookOutboxRepository webhookOutboxRepository;
    private final WebhookRepository webhookRepository;
    private final WebhookOutboxService webhookOutboxService;
    private final SsrfGuard ssrfGuard;
    private final WebhookMetricsService webhookMetricsService;

    public WebhookController(WebhookService webhookService,
                             WebhookDispatcher webhookDispatcher,
                             WebhookDeliveryRepository webhookDeliveryRepository,
                             WebhookOutboxRepository webhookOutboxRepository,
                             WebhookRepository webhookRepository,
                             WebhookOutboxService webhookOutboxService,
                             SsrfGuard ssrfGuard,
                             WebhookMetricsService webhookMetricsService) {
        this.webhookService = webhookService;
        this.webhookDispatcher = webhookDispatcher;
        this.webhookDeliveryRepository = webhookDeliveryRepository;
        this.webhookOutboxRepository = webhookOutboxRepository;
        this.webhookRepository = webhookRepository;
        this.webhookOutboxService = webhookOutboxService;
        this.ssrfGuard = ssrfGuard;
        this.webhookMetricsService = webhookMetricsService;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", ex.getMessage()));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<WebhookDto> createWebhook(@Valid @RequestBody CreateWebhookRequest request) {
        validateAuthConfig(request.authType(), request.authConfig());
        UUID orgId = TenantContext.get().orgId();
        UUID userId = TenantContext.get().userId();
        WebhookDto webhook = webhookService.createWebhook(orgId, userId, request);
        return ApiResponse.ok(webhook, "Webhook created successfully");
    }

    @GetMapping
    public ApiResponse<List<WebhookDto>> listWebhooks(
            @RequestParam(required = false) WebhookScopeType scopeType,
            @RequestParam(required = false) UUID scopeId) {
        UUID orgId = TenantContext.get().orgId();
        List<WebhookDto> webhooks = webhookService.listWebhooks(orgId, scopeType, scopeId);
        return ApiResponse.ok(webhooks);
    }

    @GetMapping("/{id}")
    public ApiResponse<WebhookDto> getWebhook(@PathVariable UUID id) {
        UUID orgId = TenantContext.get().orgId();
        WebhookDto webhook = webhookService.getWebhook(orgId, id);
        return ApiResponse.ok(webhook);
    }

    @PutMapping("/{id}")
    public ApiResponse<WebhookDto> updateWebhook(@PathVariable UUID id,
                                                  @Valid @RequestBody UpdateWebhookRequest request) {
        validateAuthConfig(request.authType(), request.authConfig());
        UUID orgId = TenantContext.get().orgId();
        WebhookDto webhook = webhookService.updateWebhook(orgId, id, request);
        return ApiResponse.ok(webhook, "Webhook updated successfully");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteWebhook(@PathVariable UUID id) {
        UUID orgId = TenantContext.get().orgId();
        webhookService.deleteWebhook(orgId, id);
        return ApiResponse.ok(null, "Webhook deleted successfully");
    }

    @GetMapping("/{id}/deliveries")
    public ApiResponse<PageResponse<WebhookDeliveryDto>> getDeliveries(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        PageResponse<WebhookDeliveryDto> deliveries = webhookService.getDeliveries(id, pageable);
        return ApiResponse.ok(deliveries);
    }

    @PostMapping("/{id}/test")
    public ApiResponse<WebhookDeliveryDto> testWebhook(@PathVariable UUID id) {
        UUID orgId = TenantContext.get().orgId();
        WebhookEntity webhook = webhookService.getWebhookEntity(orgId, id);

        // SSRF guard: validate the stored URL before making an outbound HTTP call.
        // The outbox poller checks this too, but the test endpoint bypasses the poller.
        ssrfGuard.validate(webhook.getUrl());

        // Build a synthetic test payload
        Map<String, Object> testPayload = Map.of(
                "eventId", UUID.randomUUID().toString(),
                "eventType", "webhook.test",
                "aggregateType", "Webhook",
                "aggregateId", id.toString(),
                "orgId", orgId.toString(),
                "timestamp", Instant.now().toString(),
                "payload", Map.of("test", true, "message", "This is a test webhook delivery")
        );

        WebhookDispatcher.DeliveryResult result = webhookDispatcher.deliverTestPayload(webhook, testPayload);

        // Record the delivery
        WebhookDeliveryEntity delivery = new WebhookDeliveryEntity();
        delivery.setWebhookId(webhook.getId());
        delivery.setEventType("webhook.test");
        delivery.setPayload(result.payloadJson());
        delivery.setStatusCode(result.statusCode());
        delivery.setResponseBody(result.responseBody());
        delivery.setSuccess(result.success());
        String status = result.success() ? "SUCCESS" : (result.authFailure() ? "AUTH_FAILURE" : "FAILED");
        delivery.setStatus(status);
        delivery.setAttemptedAt(Instant.now());
        delivery.setDeliveredAt(Instant.now());
        WebhookDeliveryEntity saved = webhookDeliveryRepository.save(delivery);

        WebhookDeliveryDto dto = new WebhookDeliveryDto(
                saved.getId(),
                saved.getWebhookId(),
                saved.getEventType(),
                saved.getStatusCode(),
                saved.isSuccess(),
                saved.getStatus(),
                saved.getDeliveredAt()
        );
        return ApiResponse.ok(dto, result.success() ? "Test delivery successful" : "Test delivery failed");
    }

    @PostMapping("/{id}/reset-circuit")
    public ApiResponse<WebhookDto> resetCircuit(@PathVariable UUID id) {
        UUID orgId = TenantContext.get().orgId();
        WebhookDto webhook = webhookService.resetCircuit(orgId, id);
        return ApiResponse.ok(webhook, "Circuit breaker reset successfully");
    }

    @GetMapping("/{id}/metrics")
    @PreAuthorize("hasAnyRole('OWNER', 'EDITOR')")
    public ApiResponse<WebhookMetricsDto> getMetrics(@PathVariable UUID id) {
        UUID orgId = TenantContext.get().orgId();
        return ApiResponse.ok(webhookMetricsService.getMetrics(id, orgId));
    }

    @PostMapping("/{id}/rotate-secret")
    public ApiResponse<Map<String, String>> rotateSecret(@PathVariable UUID id) {
        UUID orgId = TenantContext.get().orgId();
        UUID userId = TenantContext.get().userId();
        String newSecret = webhookService.rotateSecret(id, orgId, userId);
        return ApiResponse.ok(Map.of("secret", newSecret), "Secret rotated successfully");
    }

    @GetMapping("/{id}/outbox")
    public ApiResponse<List<WebhookOutboxDto>> getOutboxEntries(
            @PathVariable UUID id,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        List<WebhookOutboxEntity> entries;
        if (status != null && !status.isBlank()) {
            entries = webhookOutboxRepository.findByWebhookIdAndStatusOrderByCreatedAtDesc(id, status, pageable);
        } else {
            entries = webhookOutboxRepository.findByWebhookIdOrderByCreatedAtDesc(id, pageable);
        }
        List<WebhookOutboxDto> dtos = entries.stream().map(this::toOutboxDto).toList();
        return ApiResponse.ok(dtos);
    }

    @PostMapping("/{id}/outbox/{outboxId}/retry")
    public ApiResponse<WebhookOutboxDto> retryOutboxEntry(
            @PathVariable UUID id,
            @PathVariable UUID outboxId) {
        UUID orgId = TenantContext.get().orgId();
        // Verify the webhook belongs to the current tenant before touching the outbox entry
        webhookRepository.findByIdAndOrgId(id, orgId)
                .orElseThrow(() -> new io.ruleforge.common.exception.EntityNotFoundException(
                        "Webhook", id.toString()));
        // Atomically reset the outbox entry for retry in a single transaction to prevent
        // a race condition with OutboxPoller claiming the entry between find and save.
        WebhookOutboxEntity saved = webhookOutboxService.resetForRetry(outboxId, id);
        return ApiResponse.ok(toOutboxDto(saved), "Outbox entry queued for retry");
    }

    /**
     * Validates the auth config for security constraints before persisting.
     * For OAUTH2_CLIENT_CREDENTIALS: tokenUrl must use HTTPS and must not resolve to a
     * private/loopback address (SSRF guard); clientId must be present.
     */
    private void validateAuthConfig(WebhookAuthType authType, WebhookAuthConfigRequest req) {
        if (authType == null) return;
        if (authType == WebhookAuthType.OAUTH2_CLIENT_CREDENTIALS) {
            if (req == null) {
                throw new IllegalArgumentException("authConfig is required when authType is OAUTH2_CLIENT_CREDENTIALS");
            }
            String tokenUrl = req.getTokenUrl();
            if (tokenUrl == null || !tokenUrl.startsWith("https://")) {
                throw new IllegalArgumentException("tokenUrl must use HTTPS for OAuth2 auth type");
            }
            try {
                ssrfGuard.validate(tokenUrl);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("tokenUrl resolves to a disallowed address", e);
            }
            if (req.getClientId() == null || req.getClientId().isBlank()) {
                throw new IllegalArgumentException("clientId is required for OAuth2 auth type");
            }
            if (req.getClientSecret() == null || req.getClientSecret().isBlank()) {
                throw new IllegalArgumentException("clientSecret is required for OAuth2 auth type");
            }
        }
    }

    private WebhookOutboxDto toOutboxDto(WebhookOutboxEntity entity) {
        return new WebhookOutboxDto(
                entity.getId(),
                entity.getWebhookId(),
                entity.getEventType(),
                entity.getStatus(),
                entity.getAttemptCount(),
                entity.getNextAttemptAt(),
                entity.getLastAttemptAt(),
                entity.getLastStatusCode(),
                entity.getLastError(),
                entity.getCreatedAt()
        );
    }
}
