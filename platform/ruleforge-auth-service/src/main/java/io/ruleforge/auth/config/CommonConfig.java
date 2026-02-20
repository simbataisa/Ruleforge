package io.ruleforge.auth.config;

import io.ruleforge.common.config.CorrelationIdFilter;
import io.ruleforge.common.config.JacksonConfig;
import io.ruleforge.common.exception.GlobalExceptionHandler;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Imports shared components from the ruleforge-common module that are not
 * auto-discovered because they reside outside the auth service's base package.
 */
@Configuration
@Import({
        GlobalExceptionHandler.class,
        JacksonConfig.class,
        CorrelationIdFilter.class
})
public class CommonConfig {
}
