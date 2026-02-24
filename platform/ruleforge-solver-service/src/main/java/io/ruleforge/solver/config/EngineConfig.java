package io.ruleforge.solver.config;

import io.ruleforge.cep.CepEngine;
import io.ruleforge.core.RuleForgeEngine;
import io.ruleforge.core.action.HttpActionExecutor;
import io.ruleforge.core.action.LogActionExecutor;
import io.ruleforge.core.action.RuleActionExecutor;
import io.ruleforge.core.action.ScriptActionExecutor;
import io.ruleforge.core.engine.ActionExecutorRegistry;
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

/**
 * Configures the RuleForge engine with all supported rule type engines.
 * The engine is shared across all solve requests and is thread-safe.
 */
@Configuration
public class EngineConfig {

    @Bean
    public ActionExecutorRegistry actionExecutorRegistry() {
        return new ActionExecutorRegistry()
                .register(new HttpActionExecutor())
                .register(new RuleActionExecutor())
                .register(new ScriptActionExecutor())
                .register(new LogActionExecutor());
    }

    @Bean
    public RuleForgeEngine ruleForgeEngine(ActionExecutorRegistry actionExecutorRegistry) {
        return RuleForgeEngine.builder()
                .registerEngine(new DecisionTableEngine())
                .registerEngine(new DecisionTreeEngine())
                .registerEngine(new ScriptingEngine())
                .registerEngine(new FlowEngine())
                .registerEngine(new ScorecardEngine())
                .registerEngine(new EnsembleEngine())
                .registerEngine(new DmnEngine())
                .registerEngine(new CepEngine())
                .registerEngine(new HierarchyEngine())
                .actionExecutorRegistry(actionExecutorRegistry)
                .maxIterations(200)
                .defaultTimeout(Duration.ofSeconds(10))
                .build();
    }
}
