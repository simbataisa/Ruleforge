package io.ruleforge.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Shared Jackson configuration for all RuleForge services.
 * Keeps Jackson 2 defaults so JSON output is unchanged by the Jackson 3 upgrade,
 * and disables timestamp serialisation so that {@code Instant}, {@code LocalDateTime},
 * etc. are rendered as ISO-8601 strings (java.time support is built into Jackson 3).
 */
@Configuration
public class JacksonConfig {

    @Bean
    public JsonMapper jsonMapper() {
        return JsonMapper.builderWithJackson2Defaults()
                .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build();
    }
}
