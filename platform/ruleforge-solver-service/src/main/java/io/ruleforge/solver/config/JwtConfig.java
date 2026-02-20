package io.ruleforge.solver.config;

import io.ruleforge.common.security.JwtTokenProvider;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures the {@link JwtTokenProvider} bean used for JWT validation
 * in the solver service.
 */
@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class JwtConfig {

    @Bean
    public JwtTokenProvider jwtTokenProvider(JwtProperties properties) {
        return new JwtTokenProvider(
                properties.secret(),
                properties.accessTokenValidityMs(),
                properties.refreshTokenValidityMs()
        );
    }
}
