package io.ruleforge.event.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.ruleforge.event.delivery.WebhookAuthInjector;
import io.ruleforge.event.delivery.WebhookAuthInjectorRegistry;
import io.ruleforge.event.entity.WebhookEntity;
import io.ruleforge.event.entity.WebhookOutboxEntity;
import io.ruleforge.event.exception.WebhookAuthException;
import io.ruleforge.event.model.WebhookAuthConfig;
import io.ruleforge.event.model.WebhookAuthType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for WebhookDispatcher focusing on WebhookAuthException handling.
 */
@ExtendWith(MockitoExtension.class)
class WebhookDispatcherAuthTest {

    @Mock private RestClient webhookClient;
    @Mock private WebhookAuthInjectorRegistry webhookAuthInjectorRegistry;
    @Mock private CredentialEncryptionService credentialEncryptionService;

    private WebhookDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        dispatcher = new WebhookDispatcher(
                webhookClient,
                new ObjectMapper(),
                webhookAuthInjectorRegistry,
                credentialEncryptionService
        );
    }

    // ------------------------------------------------------------------
    // Helper builders
    // ------------------------------------------------------------------

    private WebhookEntity buildWebhook() {
        WebhookEntity webhook = new WebhookEntity();
        webhook.setId(UUID.randomUUID());
        webhook.setUrl("https://example.com/webhook");
        webhook.setAuthType(WebhookAuthType.BEARER_TOKEN);
        webhook.setActive(true);
        webhook.setOrgId(UUID.randomUUID());
        webhook.setSecret("ignored");
        return webhook;
    }

    private WebhookOutboxEntity buildOutboxEntry(UUID webhookId) {
        WebhookOutboxEntity entry = new WebhookOutboxEntity();
        entry.setId(UUID.randomUUID());
        entry.setWebhookId(webhookId);
        entry.setOrgId(UUID.randomUUID());
        entry.setEventType("rule.fired");
        entry.setPayload(Map.of("key", "value"));
        entry.setAttemptNonce(UUID.randomUUID());
        return entry;
    }

    // ------------------------------------------------------------------
    // Test: injector throws WebhookAuthException → authFailure=true, permanent=true
    // ------------------------------------------------------------------

    @Test
    void deliverDirect_whenInjectorThrowsWebhookAuthException_returnsAuthFailureResult() {
        WebhookEntity webhook = buildWebhook();
        WebhookOutboxEntity entry = buildOutboxEntry(webhook.getId());

        // Auth config resolution returns a BearerToken config
        WebhookAuthConfig.BearerToken authConfig = new WebhookAuthConfig.BearerToken("bad-token");
        when(credentialEncryptionService.decrypt(any(), eq(WebhookAuthType.BEARER_TOKEN)))
                .thenReturn(authConfig);
        webhook.setAuthConfig("some-encrypted-blob");

        // Injector is stubbed to throw WebhookAuthException
        WebhookAuthInjector injector = mock(WebhookAuthInjector.class);
        doThrow(new WebhookAuthException("Token validation failed"))
                .when(injector).injectAuth(any(HttpHeaders.class), any(WebhookAuthConfig.class), any(byte[].class));
        when(webhookAuthInjectorRegistry.getFor(WebhookAuthType.BEARER_TOKEN)).thenReturn(injector);

        WebhookDispatcher.DeliveryResult result = dispatcher.deliverDirect(webhook, entry);

        assertTrue(result.authFailure(), "authFailure must be true when injector throws WebhookAuthException");
        assertTrue(result.permanent(), "permanent must be true for auth failures");
        assertFalse(result.success(), "success must be false for auth failures");
        assertEquals(0, result.statusCode(), "statusCode must be 0 (no HTTP call was made)");
        assertNotNull(result.errorMessage(), "errorMessage must be populated");
        assertTrue(result.errorMessage().contains("AUTH_FAILURE"),
                "errorMessage must be prefixed with AUTH_FAILURE");

        // Verify the HTTP call was never made
        verifyNoInteractions(webhookClient);
    }

    // ------------------------------------------------------------------
    // Test: normal success path — authFailure defaults to false
    // ------------------------------------------------------------------

    @Test
    void deliverResult_defaultConstructor_authFailureIsFalse() {
        // Verify the 6-arg backwards-compat constructor sets authFailure=false
        WebhookDispatcher.DeliveryResult result =
                new WebhookDispatcher.DeliveryResult(true, 200, "OK", "{}", null, false);

        assertFalse(result.authFailure(), "6-arg constructor must default authFailure to false");
        assertTrue(result.success());
        assertEquals(200, result.statusCode());
    }

    // ------------------------------------------------------------------
    // Test: 7-arg constructor — explicit authFailure=true
    // ------------------------------------------------------------------

    @Test
    void deliverResult_sevenArgConstructor_authFailureTrue() {
        WebhookDispatcher.DeliveryResult result =
                new WebhookDispatcher.DeliveryResult(false, 0, null, "{}", "AUTH_FAILURE: bad", true, true);

        assertTrue(result.authFailure());
        assertTrue(result.permanent());
        assertFalse(result.success());
    }
}
