package io.ruleforge.event.controller;

import io.ruleforge.common.dto.ApiResponse;
import io.ruleforge.common.dto.PageResponse;
import io.ruleforge.common.tenant.TenantContext;
import io.ruleforge.event.dto.CreateWebhookRequest;
import io.ruleforge.event.dto.UpdateWebhookRequest;
import io.ruleforge.event.dto.WebhookDeliveryDto;
import io.ruleforge.event.dto.WebhookDto;
import io.ruleforge.event.service.WebhookService;

import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/webhooks")
public class WebhookController {

    private final WebhookService webhookService;

    public WebhookController(WebhookService webhookService) {
        this.webhookService = webhookService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<WebhookDto> createWebhook(@Valid @RequestBody CreateWebhookRequest request) {
        UUID spaceId = TenantContext.get().spaceId();
        WebhookDto webhook = webhookService.createWebhook(spaceId, request);
        return ApiResponse.ok(webhook, "Webhook created successfully");
    }

    @GetMapping
    public ApiResponse<List<WebhookDto>> listWebhooks() {
        UUID spaceId = TenantContext.get().spaceId();
        List<WebhookDto> webhooks = webhookService.listWebhooks(spaceId);
        return ApiResponse.ok(webhooks);
    }

    @GetMapping("/{id}")
    public ApiResponse<WebhookDto> getWebhook(@PathVariable UUID id) {
        UUID spaceId = TenantContext.get().spaceId();
        WebhookDto webhook = webhookService.getWebhook(spaceId, id);
        return ApiResponse.ok(webhook);
    }

    @PutMapping("/{id}")
    public ApiResponse<WebhookDto> updateWebhook(@PathVariable UUID id,
                                                  @Valid @RequestBody UpdateWebhookRequest request) {
        UUID spaceId = TenantContext.get().spaceId();
        WebhookDto webhook = webhookService.updateWebhook(spaceId, id, request);
        return ApiResponse.ok(webhook, "Webhook updated successfully");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteWebhook(@PathVariable UUID id) {
        UUID spaceId = TenantContext.get().spaceId();
        webhookService.deleteWebhook(spaceId, id);
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
}
