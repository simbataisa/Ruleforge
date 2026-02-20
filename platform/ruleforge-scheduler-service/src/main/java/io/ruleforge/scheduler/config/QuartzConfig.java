package io.ruleforge.scheduler.config;

import org.springframework.context.annotation.Configuration;

/**
 * Quartz scheduler configuration.
 * Spring Boot auto-configures Quartz based on application.yml settings.
 * The in-memory RAMJobStore is used (job-store-type: memory).
 */
@Configuration
public class QuartzConfig {
    // Spring Boot auto-configuration handles Quartz setup via application.yml
}
