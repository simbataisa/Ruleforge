package io.ruleforge.management.config;

import io.ruleforge.cep.CepEngine;
import io.ruleforge.core.RuleForgeEngine;
import io.ruleforge.dmn.DmnEngine;
import io.ruleforge.ensemble.EnsembleEngine;
import io.ruleforge.flow.FlowEngine;
import io.ruleforge.hierarchy.HierarchyEngine;
import io.ruleforge.scorecard.ScorecardEngine;
import io.ruleforge.script.ScriptingEngine;
import io.ruleforge.table.DecisionTableEngine;
import io.ruleforge.tree.DecisionTreeEngine;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class EngineConfig {

    @Bean
    public RuleForgeEngine ruleForgeEngine() {
        return RuleForgeEngine.builder()
                .registerEngine(new DecisionTableEngine())
                .registerEngine(new DecisionTreeEngine())
                .registerEngine(new ScorecardEngine())
                .registerEngine(new EnsembleEngine())
                .registerEngine(new FlowEngine())
                .registerEngine(new DmnEngine())
                .registerEngine(new ScriptingEngine())
                .registerEngine(new CepEngine())
                .registerEngine(new HierarchyEngine())
                .maxIterations(100)
                .defaultTimeout(Duration.ofSeconds(5))
                .build();
    }
}
