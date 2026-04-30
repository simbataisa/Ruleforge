package io.ruleforge.event.delivery;

import io.ruleforge.event.exception.WebhookAuthException;
import io.ruleforge.event.model.WebhookAuthConfig;
import io.ruleforge.event.model.WebhookAuthType;
import io.ruleforge.event.service.OAuth2TokenCacheService;
import io.ruleforge.event.service.SsrfGuard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OAuth2ClientCredentialsInjectorTest {

    @Mock
    private OAuth2TokenCacheService tokenCache;

    @Mock
    private RestClient restClient;

    @Mock
    private SsrfGuard ssrfGuard;

    // RestClient fluent chain mocks
    @Mock
    private RestClient.RequestBodyUriSpec uriSpec;

    @Mock
    private RestClient.RequestBodySpec bodySpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    private OAuth2ClientCredentialsInjector injector;

    private final UUID orgId = UUID.randomUUID();
    private final UUID webhookId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        injector = new OAuth2ClientCredentialsInjector(tokenCache, restClient, ssrfGuard);
    }

    @Test
    void supportedType_isOAuth2ClientCredentials() {
        assertThat(injector.supportedType()).isEqualTo(WebhookAuthType.OAUTH2_CLIENT_CREDENTIALS);
    }

    @Test
    void standardInjectAuth_throwsUnsupportedOperationException() {
        HttpHeaders headers = new HttpHeaders();
        WebhookAuthConfig.OAuth2ClientCredentials config =
                new WebhookAuthConfig.OAuth2ClientCredentials("https://auth.example.com/token", "clientId", "secret", null);

        assertThatThrownBy(() -> injector.injectAuth(headers, config, new byte[0]))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("orgId and webhookId");
    }

    @Test
    void cacheHit_usesToken_doesNotCallRestClient() {
        when(tokenCache.get(orgId, webhookId)).thenReturn("cached-token");

        WebhookAuthConfig.OAuth2ClientCredentials config =
                new WebhookAuthConfig.OAuth2ClientCredentials("https://auth.example.com/token", "clientId", "secret", null);
        HttpHeaders headers = new HttpHeaders();

        injector.injectAuth(headers, config, new byte[0], orgId, webhookId);

        assertThat(headers.getFirst(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer cached-token");
        verifyNoInteractions(restClient);
        verify(ssrfGuard, never()).validate(any());
    }

    @Test
    void cacheMiss_fetchesToken_cachesAndInjects() {
        when(tokenCache.get(orgId, webhookId)).thenReturn(null);
        doNothing().when(ssrfGuard).validate(anyString());

        setupRestClientMock(Map.of("access_token", "new-token", "expires_in", 3600));

        WebhookAuthConfig.OAuth2ClientCredentials config =
                new WebhookAuthConfig.OAuth2ClientCredentials("https://auth.example.com/token", "clientId", "secret", null);
        HttpHeaders headers = new HttpHeaders();

        injector.injectAuth(headers, config, new byte[0], orgId, webhookId);

        assertThat(headers.getFirst(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer new-token");
        // TTL = 3600 - 60 = 3540
        verify(tokenCache).set(orgId, webhookId, "new-token", 3540L);
    }

    @Test
    void cacheMiss_tokenEndpointReturns401_throwsWebhookAuthException() {
        when(tokenCache.get(orgId, webhookId)).thenReturn(null);
        doNothing().when(ssrfGuard).validate(anyString());

        RestClientResponseException ex = mock(RestClientResponseException.class);
        org.springframework.http.HttpStatusCode statusCode = org.springframework.http.HttpStatus.UNAUTHORIZED;
        when(ex.getStatusCode()).thenReturn(statusCode);

        doReturn(uriSpec).when(restClient).post();
        doReturn(bodySpec).when(uriSpec).uri(anyString());
        doReturn(bodySpec).when(bodySpec).contentType(any(MediaType.class));
        doReturn(bodySpec).when(bodySpec).body(any(Object.class));
        doReturn(responseSpec).when(bodySpec).retrieve();
        doThrow(ex).when(responseSpec).body(Map.class);

        WebhookAuthConfig.OAuth2ClientCredentials config =
                new WebhookAuthConfig.OAuth2ClientCredentials("https://auth.example.com/token", "clientId", "secret", null);

        assertThatThrownBy(() -> injector.injectAuth(new HttpHeaders(), config, new byte[0], orgId, webhookId))
                .isInstanceOf(WebhookAuthException.class)
                .hasMessageContaining("HTTP 401");
    }

    @Test
    void cacheMiss_networkError_throwsWebhookAuthException() {
        when(tokenCache.get(orgId, webhookId)).thenReturn(null);
        doNothing().when(ssrfGuard).validate(anyString());

        doReturn(uriSpec).when(restClient).post();
        doReturn(bodySpec).when(uriSpec).uri(anyString());
        doReturn(bodySpec).when(bodySpec).contentType(any(MediaType.class));
        doReturn(bodySpec).when(bodySpec).body(any(Object.class));
        doReturn(responseSpec).when(bodySpec).retrieve();
        doThrow(new RuntimeException("Connection refused")).when(responseSpec).body(Map.class);

        WebhookAuthConfig.OAuth2ClientCredentials config =
                new WebhookAuthConfig.OAuth2ClientCredentials("https://auth.example.com/token", "clientId", "secret", null);

        assertThatThrownBy(() -> injector.injectAuth(new HttpHeaders(), config, new byte[0], orgId, webhookId))
                .isInstanceOf(WebhookAuthException.class)
                .hasMessageContaining("Connection refused")
                .cause().isInstanceOf(RuntimeException.class);
    }

    @Test
    void ssrfBlockedUrl_throwsWebhookAuthException() {
        when(tokenCache.get(orgId, webhookId)).thenReturn(null);
        doThrow(new IllegalArgumentException("Webhook URL resolves to private/site-local address (blocked)"))
                .when(ssrfGuard).validate("https://192.168.1.1/token");

        WebhookAuthConfig.OAuth2ClientCredentials config =
                new WebhookAuthConfig.OAuth2ClientCredentials("https://192.168.1.1/token", "clientId", "secret", null);

        assertThatThrownBy(() -> injector.injectAuth(new HttpHeaders(), config, new byte[0], orgId, webhookId))
                .isInstanceOf(WebhookAuthException.class)
                .hasMessageContaining("tokenUrl blocked by SSRF guard:")
                .hasMessageContaining("https://192.168.1.1/token");

        verifyNoInteractions(restClient);
    }

    @Test
    void missingExpiresIn_usesFallbackTtl() {
        when(tokenCache.get(orgId, webhookId)).thenReturn(null);
        doNothing().when(ssrfGuard).validate(anyString());

        setupRestClientMock(Map.of("access_token", "fallback-token"));

        WebhookAuthConfig.OAuth2ClientCredentials config =
                new WebhookAuthConfig.OAuth2ClientCredentials("https://auth.example.com/token", "clientId", "secret", null);

        injector.injectAuth(new HttpHeaders(), config, new byte[0], orgId, webhookId);

        verify(tokenCache).set(orgId, webhookId, "fallback-token", OAuth2ClientCredentialsInjector.DEFAULT_TTL_SECONDS);
    }

    @Test
    @SuppressWarnings("unchecked")
    void scopeNull_notIncludedInForm() {
        when(tokenCache.get(orgId, webhookId)).thenReturn(null);
        doNothing().when(ssrfGuard).validate(anyString());

        ArgumentCaptor<Object> bodyCaptor = ArgumentCaptor.forClass(Object.class);
        doReturn(uriSpec).when(restClient).post();
        doReturn(bodySpec).when(uriSpec).uri(anyString());
        doReturn(bodySpec).when(bodySpec).contentType(any(MediaType.class));
        doReturn(bodySpec).when(bodySpec).body(bodyCaptor.capture());
        doReturn(responseSpec).when(bodySpec).retrieve();
        doReturn(Map.of("access_token", "tok", "expires_in", 300)).when(responseSpec).body(Map.class);

        WebhookAuthConfig.OAuth2ClientCredentials config =
                new WebhookAuthConfig.OAuth2ClientCredentials("https://auth.example.com/token", "clientId", "secret", null);

        injector.injectAuth(new HttpHeaders(), config, new byte[0], orgId, webhookId);

        MultiValueMap<String, String> form = (MultiValueMap<String, String>) bodyCaptor.getValue();
        assertThat(form.containsKey("scope")).isFalse();
        assertThat(form.getFirst("grant_type")).isEqualTo("client_credentials");
        assertThat(form.getFirst("client_id")).isEqualTo("clientId");
        assertThat(form.getFirst("client_secret")).isEqualTo("secret");
    }

    @Test
    void smallExpiresIn_usesMinimumTtl() {
        when(tokenCache.get(orgId, webhookId)).thenReturn(null);
        doNothing().when(ssrfGuard).validate(anyString());

        setupRestClientMock(Map.of("access_token", "short-lived-token", "expires_in", 30));

        WebhookAuthConfig.OAuth2ClientCredentials config =
                new WebhookAuthConfig.OAuth2ClientCredentials("https://auth.example.com/token", "clientId", "secret", null);

        injector.injectAuth(new HttpHeaders(), config, new byte[0], orgId, webhookId);

        verify(tokenCache).set(orgId, webhookId, "short-lived-token", 60L);
    }

    @Test
    @SuppressWarnings("unchecked")
    void scopePresent_includedInForm() {
        when(tokenCache.get(orgId, webhookId)).thenReturn(null);
        doNothing().when(ssrfGuard).validate(anyString());

        ArgumentCaptor<Object> bodyCaptor = ArgumentCaptor.forClass(Object.class);
        doReturn(uriSpec).when(restClient).post();
        doReturn(bodySpec).when(uriSpec).uri(anyString());
        doReturn(bodySpec).when(bodySpec).contentType(any(MediaType.class));
        doReturn(bodySpec).when(bodySpec).body(bodyCaptor.capture());
        doReturn(responseSpec).when(bodySpec).retrieve();
        doReturn(Map.of("access_token", "tok", "expires_in", 300)).when(responseSpec).body(Map.class);

        WebhookAuthConfig.OAuth2ClientCredentials config =
                new WebhookAuthConfig.OAuth2ClientCredentials("https://auth.example.com/token", "clientId", "secret", "api:read");

        injector.injectAuth(new HttpHeaders(), config, new byte[0], orgId, webhookId);

        MultiValueMap<String, String> form = (MultiValueMap<String, String>) bodyCaptor.getValue();
        assertThat(form.getFirst("scope")).isEqualTo("api:read");
    }

    private void setupRestClientMock(Map<String, Object> responseBody) {
        doReturn(uriSpec).when(restClient).post();
        doReturn(bodySpec).when(uriSpec).uri(anyString());
        doReturn(bodySpec).when(bodySpec).contentType(any(MediaType.class));
        doReturn(bodySpec).when(bodySpec).body(any(Object.class));
        doReturn(responseSpec).when(bodySpec).retrieve();
        doReturn(responseBody).when(responseSpec).body(Map.class);
    }
}
