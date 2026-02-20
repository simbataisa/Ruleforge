package io.ruleforge.solver.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Configures the REST client used to validate API keys against the auth service.
 */
@Configuration
public class ApiKeyValidationConfig {

    @Bean
    public RestClient authServiceClient(@Value("${app.auth-service.url}") String baseUrl) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }
}
