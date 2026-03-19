package io.ruleforge.event.delivery;

import io.ruleforge.event.exception.WebhookAuthException;
import io.ruleforge.event.model.WebhookAuthConfig;
import io.ruleforge.event.model.WebhookAuthType;
import io.ruleforge.event.service.OAuth2TokenCacheService;
import io.ruleforge.event.service.SsrfGuard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;
import java.util.UUID;

/**
 * Injects OAuth 2.0 Bearer tokens for webhooks using client credentials grant.
 * Tokens are cached in Redis with ADR-010 key scheme and TTL = expires_in - 60s.
 * Uses an overloaded {@code injectAuth} variant that accepts {@code orgId} and
 * {@code webhookId} for cache key construction — routed by {@code WebhookDispatcher}.
 */
@Component
public class OAuth2ClientCredentialsInjector implements WebhookAuthInjector {

    private static final Logger log = LoggerFactory.getLogger(OAuth2ClientCredentialsInjector.class);
    static final long DEFAULT_TTL_SECONDS = 3540L; // 59 minutes fallback when expires_in absent

    private final OAuth2TokenCacheService tokenCache;
    private final RestClient webhookClient;
    private final SsrfGuard ssrfGuard;

    public OAuth2ClientCredentialsInjector(OAuth2TokenCacheService tokenCache,
                                           RestClient webhookClient,
                                           SsrfGuard ssrfGuard) {
        this.tokenCache = tokenCache;
        this.webhookClient = webhookClient;
        this.ssrfGuard = ssrfGuard;
    }

    @Override
    public WebhookAuthType supportedType() {
        return WebhookAuthType.OAUTH2_CLIENT_CREDENTIALS;
    }

    /**
     * Standard interface method — not used for OAuth2 because orgId and webhookId
     * are required for Redis cache key construction.
     */
    @Override
    public void injectAuth(HttpHeaders headers, WebhookAuthConfig config, byte[] payload) {
        throw new UnsupportedOperationException(
                "OAuth2ClientCredentialsInjector requires orgId and webhookId — use overloaded method");
    }

    /**
     * Used by WebhookDispatcher for OAUTH2_CLIENT_CREDENTIALS type.
     * Checks Redis cache first; on miss, fetches token from tokenUrl.
     */
    public void injectAuth(HttpHeaders headers, WebhookAuthConfig config, byte[] payload,
                           UUID orgId, UUID webhookId) {
        if (!(config instanceof WebhookAuthConfig.OAuth2ClientCredentials oauth2)) {
            return;
        }

        // 1. Cache hit
        String cachedToken = tokenCache.get(orgId, webhookId);
        if (cachedToken != null) {
            log.debug("OAuth2 token cache hit for webhook {}", webhookId);
            headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + cachedToken);
            return;
        }

        // 2. SSRF guard before any outbound call
        try {
            ssrfGuard.validate(oauth2.tokenUrl());
        } catch (IllegalArgumentException e) {
            throw new WebhookAuthException("tokenUrl blocked by SSRF guard: " + oauth2.tokenUrl(), e);
        }

        // 3. Fetch token from token endpoint
        log.debug("OAuth2 token cache miss for webhook {} — fetching from {}", webhookId, oauth2.tokenUrl());
        TokenResponse tokenResponse = fetchToken(oauth2);

        // 4. Cache with TTL = expires_in - 60s (min 60s); default 59 min if absent
        long ttl = tokenResponse.expiresIn() > 0
                ? Math.max(tokenResponse.expiresIn() - 60L, 60L)
                : DEFAULT_TTL_SECONDS;
        tokenCache.set(orgId, webhookId, tokenResponse.accessToken(), ttl);

        // 5. Inject
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + tokenResponse.accessToken());
    }

    @SuppressWarnings("unchecked")
    private TokenResponse fetchToken(WebhookAuthConfig.OAuth2ClientCredentials oauth2) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", oauth2.clientId());
        form.add("client_secret", oauth2.clientSecret()); // already decrypted by CredentialEncryptionService
        if (oauth2.scope() != null && !oauth2.scope().isBlank()) {
            form.add("scope", oauth2.scope());
        }

        try {
            Map<String, Object> body = webhookClient.post()
                    .uri(oauth2.tokenUrl())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(Map.class);

            if (body == null || !body.containsKey("access_token")) {
                throw new WebhookAuthException(
                        "OAuth2 token fetch returned no access_token from " + oauth2.tokenUrl());
            }

            String accessToken = (String) body.get("access_token");
            long expiresIn = 0;
            if (body.containsKey("expires_in")) {
                Object raw = body.get("expires_in");
                expiresIn = raw instanceof Number n ? n.longValue() : 0L;
            }
            return new TokenResponse(accessToken, expiresIn);

        } catch (RestClientResponseException e) {
            throw new WebhookAuthException(
                    "OAuth2 token fetch failed: HTTP " + e.getStatusCode().value()
                    + " from " + oauth2.tokenUrl(), e);
        } catch (WebhookAuthException e) {
            throw e; // re-throw as-is
        } catch (Exception e) {
            throw new WebhookAuthException(
                    "OAuth2 token fetch network error from " + oauth2.tokenUrl() + ": " + e.getMessage(), e);
        }
    }

    record TokenResponse(String accessToken, long expiresIn) {}
}
