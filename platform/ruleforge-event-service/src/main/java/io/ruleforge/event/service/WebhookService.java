package io.ruleforge.event.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.ruleforge.common.dto.PageResponse;
import io.ruleforge.common.exception.EntityNotFoundException;
import io.ruleforge.common.event.DomainEvent;
import io.ruleforge.event.dto.CreateWebhookRequest;
import io.ruleforge.event.dto.UpdateWebhookRequest;
import io.ruleforge.event.dto.WebhookAuthConfigDto;
import io.ruleforge.event.dto.WebhookAuthConfigRequest;
import io.ruleforge.event.dto.WebhookDeliveryDto;
import io.ruleforge.event.dto.WebhookDto;
import io.ruleforge.event.dto.WebhookRetryConfigDto;
import io.ruleforge.event.dto.WebhookRetryConfigRequest;
import io.ruleforge.event.entity.WebhookEntity;
import io.ruleforge.event.model.WebhookAuthConfig;
import io.ruleforge.event.model.WebhookAuthType;
import io.ruleforge.event.model.WebhookScopeType;
import io.ruleforge.event.repository.WebhookDeliveryRepository;
import io.ruleforge.event.repository.WebhookRepository;

import io.ruleforge.common.tenant.TenantContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class WebhookService {

    private static final Logger log = LoggerFactory.getLogger(WebhookService.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int SECRET_LENGTH_BYTES = 32;
    private static final Set<String> EDITOR_PLUS_ROLES = Set.of("EDITOR", "ADMIN", "OWNER");
    private static final Set<String> VIEWER_ROLES = Set.of("VIEWER");

    private final WebhookRepository webhookRepository;
    private final WebhookDeliveryRepository webhookDeliveryRepository;
    private final ObjectMapper objectMapper;
    private final SsrfGuard ssrfGuard;
    private final AuditEventService auditEventService;
    private final CredentialEncryptionService credentialEncryptionService;
    private final OAuth2TokenCacheService oauth2TokenCacheService;

    public WebhookService(WebhookRepository webhookRepository,
                          WebhookDeliveryRepository webhookDeliveryRepository,
                          ObjectMapper objectMapper,
                          SsrfGuard ssrfGuard,
                          AuditEventService auditEventService,
                          CredentialEncryptionService credentialEncryptionService,
                          OAuth2TokenCacheService oauth2TokenCacheService) {
        this.webhookRepository = webhookRepository;
        this.webhookDeliveryRepository = webhookDeliveryRepository;
        this.objectMapper = objectMapper;
        this.ssrfGuard = ssrfGuard;
        this.auditEventService = auditEventService;
        this.credentialEncryptionService = credentialEncryptionService;
        this.oauth2TokenCacheService = oauth2TokenCacheService;
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
        entity.setActive(request.active() != null ? request.active() : true);
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

        // Apply auth config
        resolveAndEncryptAuthConfig(request.authType(), request.authConfig(), entity);

        // Apply retry config
        applyRetryConfig(request.retryConfig(), entity);

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

        // Apply auth config if provided
        if (request.authType() != null || request.authConfig() != null) {
            // Evict OAuth2 token cache only when the current or new auth type is OAUTH2_CLIENT_CREDENTIALS
            WebhookAuthType previousAuthType = entity.getAuthType();
            WebhookAuthType newAuthType = request.authType() != null ? request.authType() : previousAuthType;

            resolveAndEncryptAuthConfig(request.authType(), request.authConfig(), entity);

            if (previousAuthType == WebhookAuthType.OAUTH2_CLIENT_CREDENTIALS
                    || newAuthType == WebhookAuthType.OAUTH2_CLIENT_CREDENTIALS) {
                oauth2TokenCacheService.evict(orgId, webhookId);
            }
        }

        // Apply retry config if provided
        applyRetryConfig(request.retryConfig(), entity);

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
                        e.getStatus(),
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
        String role = TenantContext.get().role();
        if (role == null || !EDITOR_PLUS_ROLES.contains(role)) {
            throw new AccessDeniedException("EDITOR or higher role required to rotate webhook secret");
        }

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
        String role = TenantContext.get() != null ? TenantContext.get().role() : null;
        boolean isViewer = role != null && VIEWER_ROLES.contains(role);
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
                entity.getCreatedAt(),
                entity.getAuthType() != null ? entity.getAuthType().name() : null,
                maskAuthConfig(entity, isViewer),
                buildRetryConfigDto(entity)
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

    /**
     * Resolve the auth type and config from the request, encrypt the credentials, and set on the entity.
     */
    private void resolveAndEncryptAuthConfig(WebhookAuthType authType,
                                             WebhookAuthConfigRequest authConfigRequest,
                                             WebhookEntity entity) {
        if (authType == null) {
            // Legacy: no authType provided — check if authConfigRequest has a secret
            if (authConfigRequest != null && authConfigRequest.getSecret() != null) {
                WebhookAuthConfig config = new WebhookAuthConfig.SigningSecret(authConfigRequest.getSecret());
                entity.setAuthType(WebhookAuthType.SIGNING_SECRET);
                entity.setAuthConfig(credentialEncryptionService.encrypt(config));
            } else if (authConfigRequest == null) {
                // No auth info provided — keep default SIGNING_SECRET using the generated secret
                entity.setAuthType(WebhookAuthType.SIGNING_SECRET);
            } else {
                entity.setAuthType(WebhookAuthType.NONE);
                entity.setAuthConfig(null);
            }
            return;
        }

        entity.setAuthType(authType);

        if (authType == WebhookAuthType.NONE) {
            entity.setAuthConfig(null);
            return;
        }

        if (authConfigRequest == null) {
            // No config for non-NONE type: keep existing config (for updates)
            return;
        }

        WebhookAuthConfig config;
        switch (authType) {
            case SIGNING_SECRET -> {
                String secret = authConfigRequest.getSecret();
                if (secret == null || secret.isBlank()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "authConfig.secret is required for SIGNING_SECRET auth type");
                }
                config = new WebhookAuthConfig.SigningSecret(secret);
            }
            case API_KEY -> {
                if (authConfigRequest.getApiKey() == null || authConfigRequest.getApiKey().isBlank()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "authConfig.apiKey is required for API_KEY auth type");
                }
                config = new WebhookAuthConfig.ApiKey(authConfigRequest.getHeaderName(), authConfigRequest.getApiKey());
            }
            case BASIC_AUTH -> {
                if (authConfigRequest.getUsername() == null || authConfigRequest.getPassword() == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "authConfig.username and authConfig.password are required for BASIC_AUTH type");
                }
                config = new WebhookAuthConfig.BasicAuth(authConfigRequest.getUsername(), authConfigRequest.getPassword());
            }
            case BEARER_TOKEN -> {
                if (authConfigRequest.getToken() == null || authConfigRequest.getToken().isBlank()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "authConfig.token is required for BEARER_TOKEN auth type");
                }
                config = new WebhookAuthConfig.BearerToken(authConfigRequest.getToken());
            }
            case CUSTOM_HEADER -> {
                if (authConfigRequest.getHeaderName() == null || authConfigRequest.getHeaderValue() == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "authConfig.headerName and authConfig.headerValue are required for CUSTOM_HEADER type");
                }
                config = new WebhookAuthConfig.CustomHeader(authConfigRequest.getHeaderName(), authConfigRequest.getHeaderValue());
            }
            case OAUTH2_CLIENT_CREDENTIALS -> {
                if (authConfigRequest.getTokenUrl() == null || authConfigRequest.getTokenUrl().isBlank()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "authConfig.tokenUrl is required for OAUTH2_CLIENT_CREDENTIALS auth type");
                }
                if (!authConfigRequest.getTokenUrl().startsWith("https://")) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "authConfig.tokenUrl must use HTTPS for OAUTH2_CLIENT_CREDENTIALS auth type");
                }
                if (authConfigRequest.getClientId() == null || authConfigRequest.getClientId().isBlank()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "authConfig.clientId is required for OAUTH2_CLIENT_CREDENTIALS auth type");
                }
                if (authConfigRequest.getClientSecret() == null || authConfigRequest.getClientSecret().isBlank()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "authConfig.clientSecret is required for OAUTH2_CLIENT_CREDENTIALS auth type");
                }
                config = new WebhookAuthConfig.OAuth2ClientCredentials(
                        authConfigRequest.getTokenUrl(),
                        authConfigRequest.getClientId(),
                        authConfigRequest.getClientSecret(),
                        authConfigRequest.getScope());
            }
            default -> {
                // Future auth types: store nothing
                entity.setAuthConfig(null);
                return;
            }
        }
        entity.setAuthConfig(credentialEncryptionService.encrypt(config));
    }

    private void applyRetryConfig(WebhookRetryConfigRequest retryConfig, WebhookEntity entity) {
        if (retryConfig == null) {
            return;
        }
        if (retryConfig.getInitialDelaySeconds() != null) {
            entity.setRetryInitialDelaySeconds(retryConfig.getInitialDelaySeconds());
        }
        if (retryConfig.getBackoffMultiplier() != null) {
            entity.setRetryBackoffMultiplier(retryConfig.getBackoffMultiplier());
        }
        if (retryConfig.getMaxAttempts() != null) {
            entity.setRetryMaxAttempts(retryConfig.getMaxAttempts());
        }
        if (retryConfig.getRetryOnStatusCodes() != null && !retryConfig.getRetryOnStatusCodes().isEmpty()) {
            entity.setRetryOnStatusCodes(retryConfig.getRetryOnStatusCodes().stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(",")));
        }
    }

    /**
     * Build a masked auth config DTO for the response.
     * VIEWERs see nothing; all credential fields are replaced with [REDACTED].
     */
    private WebhookAuthConfigDto maskAuthConfig(WebhookEntity entity, boolean isViewer) {
        if (isViewer || entity.getAuthType() == null || entity.getAuthType() == WebhookAuthType.NONE) {
            return null;
        }
        WebhookAuthConfigDto.Builder builder = WebhookAuthConfigDto.builder();
        switch (entity.getAuthType()) {
            case SIGNING_SECRET -> builder.secret("[REDACTED]");
            case API_KEY -> {
                // Decrypt to get non-credential field (headerName)
                if (entity.getAuthConfig() != null) {
                    try {
                        WebhookAuthConfig cfg = credentialEncryptionService.decrypt(entity.getAuthConfig(), entity.getAuthType());
                        if (cfg instanceof WebhookAuthConfig.ApiKey ak) {
                            builder.headerName(ak.headerName());
                        }
                    } catch (Exception e) {
                        log.warn("Could not decrypt auth config for webhook {} to get headerName", entity.getId());
                    }
                }
                builder.apiKey("[REDACTED]");
            }
            case BASIC_AUTH -> {
                if (entity.getAuthConfig() != null) {
                    try {
                        WebhookAuthConfig cfg = credentialEncryptionService.decrypt(entity.getAuthConfig(), entity.getAuthType());
                        if (cfg instanceof WebhookAuthConfig.BasicAuth ba) {
                            builder.username(ba.username());
                        }
                    } catch (Exception e) {
                        log.warn("Could not decrypt auth config for webhook {} to get username", entity.getId());
                    }
                }
                builder.password("[REDACTED]");
            }
            case BEARER_TOKEN -> builder.token("[REDACTED]");
            case CUSTOM_HEADER -> {
                if (entity.getAuthConfig() != null) {
                    try {
                        WebhookAuthConfig cfg = credentialEncryptionService.decrypt(entity.getAuthConfig(), entity.getAuthType());
                        if (cfg instanceof WebhookAuthConfig.CustomHeader ch) {
                            builder.headerName(ch.headerName());
                        }
                    } catch (Exception e) {
                        log.warn("Could not decrypt auth config for webhook {} to get headerName", entity.getId());
                    }
                }
                builder.headerValue("[REDACTED]");
            }
            case OAUTH2_CLIENT_CREDENTIALS -> {
                if (entity.getAuthConfig() != null) {
                    try {
                        WebhookAuthConfig cfg = credentialEncryptionService.decrypt(entity.getAuthConfig(), entity.getAuthType());
                        if (cfg instanceof WebhookAuthConfig.OAuth2ClientCredentials oauth2) {
                            builder.tokenUrl(oauth2.tokenUrl());
                            builder.clientId(oauth2.clientId());
                            builder.scope(oauth2.scope());
                        }
                    } catch (Exception e) {
                        log.warn("Could not decrypt auth config for webhook {} to get OAuth2 fields", entity.getId());
                    }
                }
                builder.clientSecret("[REDACTED]");
            }
            default -> { return null; }
        }
        return builder.build();
    }

    private WebhookRetryConfigDto buildRetryConfigDto(WebhookEntity entity) {
        List<Integer> statusCodes = Collections.emptyList();
        if (entity.getRetryOnStatusCodes() != null && !entity.getRetryOnStatusCodes().isBlank()) {
            statusCodes = Arrays.stream(entity.getRetryOnStatusCodes().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(Integer::parseInt)
                    .collect(Collectors.toList());
        }
        return WebhookRetryConfigDto.builder()
                .initialDelaySeconds(entity.getRetryInitialDelaySeconds())
                .backoffMultiplier(entity.getRetryBackoffMultiplier())
                .maxAttempts(entity.getRetryMaxAttempts())
                .retryOnStatusCodes(statusCodes)
                .build();
    }

    private String generateSecret() {
        byte[] bytes = new byte[SECRET_LENGTH_BYTES];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
