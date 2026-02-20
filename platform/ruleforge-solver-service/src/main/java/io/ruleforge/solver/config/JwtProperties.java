package io.ruleforge.solver.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT configuration properties bound from {@code app.jwt.*}.
 *
 * @param secret                Base64-encoded HMAC secret key
 * @param accessTokenValidityMs access token TTL in milliseconds
 * @param refreshTokenValidityMs refresh token TTL in milliseconds
 */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
        String secret,
        long accessTokenValidityMs,
        long refreshTokenValidityMs
) {}
