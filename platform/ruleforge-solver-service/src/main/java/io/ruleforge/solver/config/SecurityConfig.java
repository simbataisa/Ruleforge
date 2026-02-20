package io.ruleforge.solver.config;

import io.ruleforge.common.security.ApiKeyAuthenticationFilter;
import io.ruleforge.common.security.ApiKeyValidator;
import io.ruleforge.common.security.JwtAuthenticationFilter;
import io.ruleforge.common.security.JwtTokenProvider;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Security configuration for the solver service.
 * Supports both JWT (Bearer token) and API Key authentication.
 * JWT is checked first; if no JWT is present, the API key filter activates.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    private final ApiKeyValidator apiKeyValidator;

    public SecurityConfig(JwtTokenProvider jwtTokenProvider, ApiKeyValidator apiKeyValidator) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.apiKeyValidator = apiKeyValidator;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(
                        new JwtAuthenticationFilter(jwtTokenProvider),
                        UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(
                        new ApiKeyAuthenticationFilter(apiKeyValidator),
                        JwtAuthenticationFilter.class);

        return http.build();
    }
}
