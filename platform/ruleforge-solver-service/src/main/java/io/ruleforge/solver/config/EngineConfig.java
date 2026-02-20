package io.ruleforge.solver.config;

import io.ruleforge.core.RuleForgeEngine;
import io.ruleforge.flow.FlowEngine;
import io.ruleforge.script.ScriptingEngine;
import io.ruleforge.table.DecisionTableEngine;
import io.ruleforge.tree.DecisionTreeEngine;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Configures the RuleForge engine with all supported rule type engines.
 * The engine is shared across all solve requests and is thread-safe.
 */
@Configuration
public class EngineConfig {

    @Bean
    public RuleForgeEngine ruleForgeEngine() {
        return RuleForgeEngine.builder()
                .registerEngine(new DecisionTableEngine())
                .registerEngine(new DecisionTreeEngine())
                .registerEngine(new ScriptingEngine())
                .registerEngine(new FlowEngine())
                .maxIterations(200)
                .defaultTimeout(Duration.ofSeconds(10))
                .build();
    }
}
