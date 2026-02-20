package io.ruleforge.spring;

import io.ruleforge.core.RuleForgeEngine;
import io.ruleforge.core.spi.RuleTypeEngine;
import io.ruleforge.table.DecisionTableEngine;
import io.ruleforge.tree.DecisionTreeEngine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import java.time.Duration;
import java.util.List;

@AutoConfiguration
public class RuleForgeAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(RuleForgeEngine.class)
    public RuleForgeEngine ruleForgeEngine(
            @Autowired(required = false) List<RuleTypeEngine> additionalEngines) {
        var builder = RuleForgeEngine.builder()
                .registerEngine(new DecisionTableEngine())
                .registerEngine(new DecisionTreeEngine())
                .maxIterations(200)
                .defaultTimeout(Duration.ofSeconds(10));

        if (additionalEngines != null) {
            for (RuleTypeEngine engine : additionalEngines) {
                builder.registerEngine(engine);
            }
        }

        return builder.build();
    }
}
