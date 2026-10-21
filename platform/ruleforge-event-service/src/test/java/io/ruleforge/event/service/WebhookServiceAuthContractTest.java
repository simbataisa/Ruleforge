package io.ruleforge.event.service;

import tools.jackson.databind.ObjectMapper;
import io.ruleforge.common.tenant.TenantContext;
import io.ruleforge.common.tenant.TenantScopeExtension;
import io.ruleforge.event.dto.CreateWebhookRequest;
import io.ruleforge.event.dto.UpdateWebhookRequest;
import io.ruleforge.event.dto.WebhookAuthConfigRequest;
import io.ruleforge.event.dto.WebhookDto;
import io.ruleforge.event.entity.WebhookEntity;
import io.ruleforge.event.model.WebhookAuthConfig;
import io.ruleforge.event.model.WebhookAuthType;
import io.ruleforge.event.model.WebhookScopeType;
import io.ruleforge.event.repository.WebhookDeliveryRepository;
import io.ruleforge.event.repository.MemberLookupRepository;
import io.ruleforge.event.repository.SpaceLookupRepository;
import io.ruleforge.event.repository.WebhookRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@ExtendWith(TenantScopeExtension.class)
class WebhookServiceAuthContractTest implements TenantScopeExtension.TenantProvider {

    private TenantContext.TenantInfo tenant;

    @Override
    public TenantContext.TenantInfo tenant() {
        return tenant;
    }

    @Mock private WebhookRepository webhookRepository;
    @Mock private WebhookDeliveryRepository webhookDeliveryRepository;
    @Mock private SsrfGuard ssrfGuard;
    @Mock private AuditEventService auditEventService;
    @Mock private CredentialEncryptionService credentialEncryptionService;
    @Mock private OAuth2TokenCacheService oauth2TokenCacheService;
    @Mock private MemberLookupRepository memberLookupRepository;
    @Mock private SpaceLookupRepository spaceLookupRepository;

    private WebhookService service;
    private UUID orgId;
    private UUID userId;
    private UUID memberId;

    @BeforeEach
    void setUp() {
        service = new WebhookService(
                webhookRepository,
                webhookDeliveryRepository,
                new ObjectMapper(),
                ssrfGuard,
                auditEventService,
                credentialEncryptionService,
                oauth2TokenCacheService,
                memberLookupRepository,
                spaceLookupRepository
        );
        orgId = UUID.randomUUID();
        userId = UUID.randomUUID();
        memberId = UUID.randomUUID();
        tenant = new TenantContext.TenantInfo(orgId, null, userId, "OWNER");
    }


    @Test
    void createWebhook_customHeader_encryptsAndReturnsMaskedHeaderValue() {
        stubMemberLookup();
        when(credentialEncryptionService.encrypt(any(WebhookAuthConfig.class))).thenReturn("encrypted-custom-header");
        when(credentialEncryptionService.decrypt("encrypted-custom-header", WebhookAuthType.CUSTOM_HEADER))
                .thenReturn(new WebhookAuthConfig.CustomHeader("X-Service-Key", "plain-header-value"));
        when(webhookRepository.save(any(WebhookEntity.class))).thenAnswer(invocation -> {
            WebhookEntity entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        WebhookDto dto = service.createWebhook(orgId, userId, createRequest(
                WebhookAuthType.CUSTOM_HEADER,
                customHeaderConfig("X-Service-Key", "plain-header-value")
        ));

        ArgumentCaptor<WebhookAuthConfig> encryptedConfig = ArgumentCaptor.forClass(WebhookAuthConfig.class);
        verify(credentialEncryptionService).encrypt(encryptedConfig.capture());
        assertThat(encryptedConfig.getValue())
                .isEqualTo(new WebhookAuthConfig.CustomHeader("X-Service-Key", "plain-header-value"));

        assertThat(dto.authType()).isEqualTo("CUSTOM_HEADER");
        assertThat(dto.authConfig().getHeaderName()).isEqualTo("X-Service-Key");
        assertThat(dto.authConfig().getHeaderValue()).isEqualTo("[REDACTED]");
    }

    @Test
    void createWebhook_storesMembershipIdAsCreatedBy() {
        stubMemberLookup();
        when(webhookRepository.save(any(WebhookEntity.class))).thenAnswer(invocation -> {
            WebhookEntity entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        service.createWebhook(orgId, userId, createRequest(null, null));

        ArgumentCaptor<WebhookEntity> savedEntity = ArgumentCaptor.forClass(WebhookEntity.class);
        verify(webhookRepository).save(savedEntity.capture());
        assertThat(savedEntity.getValue().getCreatedBy()).isEqualTo(memberId);
        assertThat(savedEntity.getValue().getCreatedBy()).isNotEqualTo(userId);
    }

    @Test
    void createWebhook_deniesCreateWhenUserIsNotMemberOfWebhookOrg() {
        when(memberLookupRepository.findMemberId(orgId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createWebhook(orgId, userId, createRequest(null, null)))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("User is not a member of the webhook organization");
    }

    @Test
    void createWebhook_deniesOrgScopeWhenRoleIsEditor() {
        TenantContext.runAs(new TenantContext.TenantInfo(orgId, null, userId, "EDITOR"), () ->
                assertThatThrownBy(() -> service.createWebhook(orgId, userId, createRequest(null, null)))
                        .isInstanceOf(AccessDeniedException.class)
                        .hasMessage("OWNER or ADMIN role required to create organization webhook"));
    }

    @Test
    void createWebhook_deniesViewerForSpaceScope() {
        TenantContext.runAs(new TenantContext.TenantInfo(orgId, null, userId, "VIEWER"), () ->
                assertThatThrownBy(() -> service.createWebhook(orgId, userId, spaceCreateRequest(null, null, UUID.randomUUID())))
                        .isInstanceOf(AccessDeniedException.class)
                        .hasMessage("EDITOR or higher role required to create webhook"));
    }

    @Test
    void createWebhook_deniesSpaceOutsideAuthenticatedOrg() {
        UUID spaceId = UUID.randomUUID();
        UUID otherOrgId = UUID.randomUUID();
        when(spaceLookupRepository.findOrgId(spaceId)).thenReturn(Optional.of(otherOrgId));

        assertThatThrownBy(() -> service.createWebhook(orgId, userId, spaceCreateRequest(null, null, spaceId)))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Space does not belong to the authenticated organization");
    }

    @Test
    void createWebhook_oauth2_encryptsAndReturnsMaskedClientSecret() {
        stubMemberLookup();
        when(credentialEncryptionService.encrypt(any(WebhookAuthConfig.class))).thenReturn("encrypted-oauth2");
        when(credentialEncryptionService.decrypt("encrypted-oauth2", WebhookAuthType.OAUTH2_CLIENT_CREDENTIALS))
                .thenReturn(new WebhookAuthConfig.OAuth2ClientCredentials(
                        "https://auth.example.com/token",
                        "client-id",
                        "plain-client-secret",
                        "api:read"));
        when(webhookRepository.save(any(WebhookEntity.class))).thenAnswer(invocation -> {
            WebhookEntity entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        WebhookDto dto = service.createWebhook(orgId, userId, createRequest(
                WebhookAuthType.OAUTH2_CLIENT_CREDENTIALS,
                oauth2Config("https://auth.example.com/token", "client-id", "plain-client-secret", "api:read")
        ));

        ArgumentCaptor<WebhookAuthConfig> encryptedConfig = ArgumentCaptor.forClass(WebhookAuthConfig.class);
        verify(credentialEncryptionService).encrypt(encryptedConfig.capture());
        assertThat(encryptedConfig.getValue())
                .isEqualTo(new WebhookAuthConfig.OAuth2ClientCredentials(
                        "https://auth.example.com/token",
                        "client-id",
                        "plain-client-secret",
                        "api:read"));

        assertThat(dto.authType()).isEqualTo("OAUTH2_CLIENT_CREDENTIALS");
        assertThat(dto.authConfig().getTokenUrl()).isEqualTo("https://auth.example.com/token");
        assertThat(dto.authConfig().getClientId()).isEqualTo("client-id");
        assertThat(dto.authConfig().getScope()).isEqualTo("api:read");
        assertThat(dto.authConfig().getClientSecret()).isEqualTo("[REDACTED]");
    }

    @Test
    void getWebhook_masksStoredOAuth2CredentialFields() {
        UUID webhookId = UUID.randomUUID();
        WebhookEntity entity = entity(webhookId, WebhookAuthType.OAUTH2_CLIENT_CREDENTIALS, "encrypted-oauth2");
        when(webhookRepository.findByIdAndOrgId(webhookId, orgId)).thenReturn(Optional.of(entity));
        when(credentialEncryptionService.decrypt("encrypted-oauth2", WebhookAuthType.OAUTH2_CLIENT_CREDENTIALS))
                .thenReturn(new WebhookAuthConfig.OAuth2ClientCredentials(
                        "https://auth.example.com/token",
                        "client-id",
                        "plain-client-secret",
                        "api:read"));

        WebhookDto dto = service.getWebhook(orgId, webhookId);

        assertThat(dto.authConfig().getTokenUrl()).isEqualTo("https://auth.example.com/token");
        assertThat(dto.authConfig().getClientId()).isEqualTo("client-id");
        assertThat(dto.authConfig().getScope()).isEqualTo("api:read");
        assertThat(dto.authConfig().getClientSecret()).isEqualTo("[REDACTED]");
    }

    @Test
    void updateWebhook_oauth2EncryptsNewSecretAndEvictsTokenCache() {
        UUID webhookId = UUID.randomUUID();
        WebhookEntity existing = entity(webhookId, WebhookAuthType.OAUTH2_CLIENT_CREDENTIALS, "old-encrypted-oauth2");
        when(webhookRepository.findByIdAndOrgId(webhookId, orgId)).thenReturn(Optional.of(existing));
        when(credentialEncryptionService.encrypt(any(WebhookAuthConfig.class))).thenReturn("new-encrypted-oauth2");
        when(credentialEncryptionService.decrypt("new-encrypted-oauth2", WebhookAuthType.OAUTH2_CLIENT_CREDENTIALS))
                .thenReturn(new WebhookAuthConfig.OAuth2ClientCredentials(
                        "https://auth.example.com/token",
                        "client-id",
                        "new-client-secret",
                        null));
        when(webhookRepository.save(any(WebhookEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WebhookDto dto = service.updateWebhook(orgId, webhookId, new UpdateWebhookRequest(
                null,
                null,
                null,
                null,
                WebhookAuthType.OAUTH2_CLIENT_CREDENTIALS,
                oauth2Config("https://auth.example.com/token", "client-id", "new-client-secret", null),
                null
        ));

        ArgumentCaptor<WebhookAuthConfig> encryptedConfig = ArgumentCaptor.forClass(WebhookAuthConfig.class);
        verify(credentialEncryptionService).encrypt(encryptedConfig.capture());
        assertThat(encryptedConfig.getValue())
                .isEqualTo(new WebhookAuthConfig.OAuth2ClientCredentials(
                        "https://auth.example.com/token",
                        "client-id",
                        "new-client-secret",
                        null));
        verify(oauth2TokenCacheService).evict(orgId, webhookId);
        assertThat(existing.getAuthConfig()).isEqualTo("new-encrypted-oauth2");
        assertThat(dto.authConfig().getClientSecret()).isEqualTo("[REDACTED]");
    }

    private CreateWebhookRequest createRequest(WebhookAuthType authType, WebhookAuthConfigRequest authConfig) {
        return new CreateWebhookRequest(
                "https://hooks.example.com/ruleforge",
                List.of("rule.published"),
                WebhookScopeType.ORG,
                orgId,
                "Contract webhook",
                true,
                authType,
                authConfig,
                null
        );
    }

    private CreateWebhookRequest spaceCreateRequest(WebhookAuthType authType,
                                                    WebhookAuthConfigRequest authConfig,
                                                    UUID spaceId) {
        return new CreateWebhookRequest(
                "https://hooks.example.com/ruleforge",
                List.of("rule.published"),
                WebhookScopeType.SPACE,
                spaceId,
                "Contract webhook",
                true,
                authType,
                authConfig,
                null
        );
    }

    private void stubMemberLookup() {
        when(memberLookupRepository.findMemberId(orgId, userId)).thenReturn(Optional.of(memberId));
    }

    private WebhookAuthConfigRequest customHeaderConfig(String headerName, String headerValue) {
        WebhookAuthConfigRequest request = new WebhookAuthConfigRequest();
        request.setHeaderName(headerName);
        request.setHeaderValue(headerValue);
        return request;
    }

    private WebhookAuthConfigRequest oauth2Config(String tokenUrl, String clientId, String clientSecret, String scope) {
        WebhookAuthConfigRequest request = new WebhookAuthConfigRequest();
        request.setTokenUrl(tokenUrl);
        request.setClientId(clientId);
        request.setClientSecret(clientSecret);
        request.setScope(scope);
        return request;
    }

    private WebhookEntity entity(UUID webhookId, WebhookAuthType authType, String authConfig) {
        WebhookEntity entity = new WebhookEntity();
        entity.setId(webhookId);
        entity.setName("Contract webhook");
        entity.setScopeType(WebhookScopeType.ORG);
        entity.setScopeId(orgId);
        entity.setOrgId(orgId);
        entity.setUrl("https://hooks.example.com/ruleforge");
        entity.setEvents("[\"rule.published\"]");
        entity.setActive(true);
        entity.setSecret("legacy-secret");
        entity.setAuthType(authType);
        entity.setAuthConfig(authConfig);
        return entity;
    }
}
