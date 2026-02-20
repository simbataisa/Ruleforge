package io.ruleforge.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ruleforge.jwt")
public record JwtProperties(
        String secret,
        long accessTokenValidityMs,
        long refreshTokenValidityMs
) {
    public JwtProperties {
        if (accessTokenValidityMs <= 0) {
            accessTokenValidityMs = 3_600_000L; // 1 hour
        }
        if (refreshTokenValidityMs <= 0) {
            refreshTokenValidityMs = 604_800_000L; // 7 days
        }
    }
}
