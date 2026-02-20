package io.ruleforge.event.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class WebhookConfig {

    @Bean
    public RestClient webhookClient() {
        return RestClient.builder()
                .requestFactory(createRequestFactory())
                .build();
    }

    private org.springframework.http.client.SimpleClientHttpRequestFactory createRequestFactory() {
        var factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(10));
        return factory;
    }
}
