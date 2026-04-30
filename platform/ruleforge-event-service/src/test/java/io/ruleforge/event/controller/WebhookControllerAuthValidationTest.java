package io.ruleforge.event.controller;

import io.ruleforge.event.dto.CreateWebhookRequest;
import io.ruleforge.event.dto.WebhookAuthConfigRequest;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.doThrow;

class WebhookControllerAuthValidationTest {

    private WebhookService webhookService;
    private SsrfGuard ssrfGuard;
    private WebhookController controller;

    @BeforeEach
    void setUp() {
        webhookService = mock(WebhookService.class);
        ssrfGuard = mock(SsrfGuard.class);
        controller = new WebhookController(
                webhookService,
                mock(WebhookDispatcher.class),
                mock(WebhookDeliveryRepository.class),
                mock(WebhookOutboxRepository.class),
                mock(WebhookRepository.class),
                mock(WebhookOutboxService.class),
                ssrfGuard,
                mock(WebhookMetricsService.class)
        );
    }

    @Test
    void createWebhook_oauth2HttpTokenUrl_returnsStableHttpsValidationErrorBeforeSsrfCheck() {
        CreateWebhookRequest request = oauth2CreateRequest("http://auth.example.com/token", "client-id", "client-secret");

        assertThatThrownBy(() -> controller.createWebhook(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("tokenUrl must use HTTPS for OAuth2 auth type");

        verifyNoInteractions(ssrfGuard, webhookService);
    }

    @Test
    void createWebhook_oauth2SsrfBlockedUrl_returnsStableDisallowedAddressError() {
        String tokenUrl = "https://127.0.0.1/token";
        doThrow(new IllegalArgumentException("Webhook URL resolves to loopback address (blocked)"))
                .when(ssrfGuard).validate(tokenUrl);

        CreateWebhookRequest request = oauth2CreateRequest(tokenUrl, "client-id", "client-secret");

        assertThatThrownBy(() -> controller.createWebhook(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("tokenUrl resolves to a disallowed address");

        verify(ssrfGuard).validate(tokenUrl);
        verifyNoInteractions(webhookService);
    }

    @Test
    void createWebhook_oauth2MissingClientId_returnsValidationError() {
        CreateWebhookRequest request = oauth2CreateRequest("https://auth.example.com/token", "", "client-secret");

        assertThatThrownBy(() -> controller.createWebhook(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("clientId is required for OAuth2 auth type");
    }

    @Test
    void createWebhook_oauth2MissingClientSecret_returnsValidationError() {
        CreateWebhookRequest request = oauth2CreateRequest("https://auth.example.com/token", "client-id", "");

        assertThatThrownBy(() -> controller.createWebhook(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("clientSecret is required for OAuth2 auth type");
    }

    private CreateWebhookRequest oauth2CreateRequest(String tokenUrl, String clientId, String clientSecret) {
        WebhookAuthConfigRequest authConfig = new WebhookAuthConfigRequest();
        authConfig.setTokenUrl(tokenUrl);
        authConfig.setClientId(clientId);
        authConfig.setClientSecret(clientSecret);
        authConfig.setScope("api:read");
        return new CreateWebhookRequest(
                "https://hooks.example.com/ruleforge",
                List.of("rule.published"),
                WebhookScopeType.ORG,
                UUID.randomUUID(),
                "OAuth webhook",
                true,
                WebhookAuthType.OAUTH2_CLIENT_CREDENTIALS,
                authConfig,
                null
        );
    }
}
