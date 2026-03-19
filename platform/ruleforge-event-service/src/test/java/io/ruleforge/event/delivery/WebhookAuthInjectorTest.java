package io.ruleforge.event.delivery;

import io.ruleforge.event.model.WebhookAuthConfig;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for each WebhookAuthInjector implementation.
 * No Spring context — injectors are plain components with no dependencies.
 */
class WebhookAuthInjectorTest {

    private static final byte[] DUMMY_PAYLOAD = "{\"event\":\"test\"}".getBytes(StandardCharsets.UTF_8);

    // ------------------------------------------------------------------
    // NoneAuthInjector
    // ------------------------------------------------------------------

    @Test
    void noneInjector_addsNoHeaders() {
        NoneAuthInjector injector = new NoneAuthInjector();
        HttpHeaders headers = new HttpHeaders();

        injector.injectAuth(headers, new WebhookAuthConfig.None(), DUMMY_PAYLOAD);

        assertTrue(headers.isEmpty(), "NoneAuthInjector must not add any headers");
    }

    // ------------------------------------------------------------------
    // SigningSecretInjector
    // ------------------------------------------------------------------

    @Test
    void signingSecretInjector_addsHmacHeader() throws Exception {
        SigningSecretInjector injector = new SigningSecretInjector();
        HttpHeaders headers = new HttpHeaders();
        WebhookAuthConfig.SigningSecret config = new WebhookAuthConfig.SigningSecret("test-secret");

        injector.injectAuth(headers, config, DUMMY_PAYLOAD);

        String signature = headers.getFirst("X-RuleForge-Signature");
        assertNotNull(signature, "X-RuleForge-Signature header must be present");
        assertTrue(signature.startsWith("sha256="),
                "Signature must be prefixed with 'sha256='");
        assertTrue(signature.length() > "sha256=".length(),
                "Signature must contain a non-empty HMAC hex value");
    }

    @Test
    void signingSecretInjector_isDeterministicForSameInputs() throws Exception {
        SigningSecretInjector injector = new SigningSecretInjector();
        WebhookAuthConfig.SigningSecret config = new WebhookAuthConfig.SigningSecret("deterministic-secret");
        byte[] payload = "hello world".getBytes(StandardCharsets.UTF_8);

        HttpHeaders headers1 = new HttpHeaders();
        HttpHeaders headers2 = new HttpHeaders();
        injector.injectAuth(headers1, config, payload);
        injector.injectAuth(headers2, config, payload);

        assertEquals(headers1.getFirst("X-RuleForge-Signature"),
                headers2.getFirst("X-RuleForge-Signature"),
                "HMAC is deterministic — same key + payload must produce the same signature");
    }

    // ------------------------------------------------------------------
    // ApiKeyAuthInjector
    // ------------------------------------------------------------------

    @Test
    void apiKeyInjector_addsApiKeyHeader() {
        ApiKeyAuthInjector injector = new ApiKeyAuthInjector();
        HttpHeaders headers = new HttpHeaders();
        WebhookAuthConfig.ApiKey config = new WebhookAuthConfig.ApiKey("X-Api-Token", "super-secret-key");

        injector.injectAuth(headers, config, DUMMY_PAYLOAD);

        assertEquals("super-secret-key", headers.getFirst("X-Api-Token"),
                "ApiKeyAuthInjector must set the configured header name with the API key value");
    }

    @Test
    void apiKeyInjector_usesDefaultHeaderWhenNameIsBlank() {
        ApiKeyAuthInjector injector = new ApiKeyAuthInjector();
        HttpHeaders headers = new HttpHeaders();
        WebhookAuthConfig.ApiKey config = new WebhookAuthConfig.ApiKey("", "my-api-key");

        injector.injectAuth(headers, config, DUMMY_PAYLOAD);

        assertEquals("my-api-key", headers.getFirst("X-API-Key"),
                "ApiKeyAuthInjector must fall back to 'X-API-Key' when headerName is blank");
    }

    // ------------------------------------------------------------------
    // BasicAuthInjector
    // ------------------------------------------------------------------

    @Test
    void basicAuthInjector_addsAuthorizationHeader() {
        BasicAuthInjector injector = new BasicAuthInjector();
        HttpHeaders headers = new HttpHeaders();
        WebhookAuthConfig.BasicAuth config = new WebhookAuthConfig.BasicAuth("alice", "p4ssw0rd");

        injector.injectAuth(headers, config, DUMMY_PAYLOAD);

        String authorization = headers.getFirst(HttpHeaders.AUTHORIZATION);
        assertNotNull(authorization, "Authorization header must be present");
        assertTrue(authorization.startsWith("Basic "),
                "Authorization header must use Basic scheme");

        String encoded = authorization.substring("Basic ".length());
        String decoded = new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8);
        assertEquals("alice:p4ssw0rd", decoded,
                "Decoded credentials must be 'username:password'");
    }

    // ------------------------------------------------------------------
    // BearerTokenInjector
    // ------------------------------------------------------------------

    @Test
    void bearerTokenInjector_addsBearerHeader() {
        BearerTokenInjector injector = new BearerTokenInjector();
        HttpHeaders headers = new HttpHeaders();
        WebhookAuthConfig.BearerToken config = new WebhookAuthConfig.BearerToken("my.jwt.token");

        injector.injectAuth(headers, config, DUMMY_PAYLOAD);

        String authorization = headers.getFirst(HttpHeaders.AUTHORIZATION);
        assertNotNull(authorization, "Authorization header must be present");
        assertEquals("Bearer my.jwt.token", authorization,
                "Authorization header must be 'Bearer <token>'");
    }
}
