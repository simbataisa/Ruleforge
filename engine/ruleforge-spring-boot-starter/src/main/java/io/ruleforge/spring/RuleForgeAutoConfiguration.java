package io.ruleforge.spring;

import io.ruleforge.core.RuleForgeEngine;
import io.ruleforge.core.spi.RuleTypeEngine;
import io.ruleforge.table.DecisionTableEngine;
import io.ruleforge.tree.DecisionTreeEngine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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

    @Configuration
    @ConditionalOnClass(name = "io.ruleforge.scorecard.ScorecardEngine")
    static class ScorecardAutoConfiguration {
        @Bean
        @ConditionalOnMissingBean(name = "scorecardEngine")
        public RuleTypeEngine scorecardEngine() throws Exception {
            Class<?> clazz = Class.forName("io.ruleforge.scorecard.ScorecardEngine");
            return (RuleTypeEngine) clazz.getDeclaredConstructor().newInstance();
        }
    }

    @Configuration
    @ConditionalOnClass(name = "io.ruleforge.ensemble.EnsembleEngine")
    static class EnsembleAutoConfiguration {
        @Bean
        @ConditionalOnMissingBean(name = "ensembleEngine")
        public RuleTypeEngine ensembleEngine() throws Exception {
            Class<?> clazz = Class.forName("io.ruleforge.ensemble.EnsembleEngine");
            return (RuleTypeEngine) clazz.getDeclaredConstructor().newInstance();
        }
    }

    @Configuration
    @ConditionalOnClass(name = "io.ruleforge.flow.FlowEngine")
    static class FlowAutoConfiguration {
        @Bean
        @ConditionalOnMissingBean(name = "flowEngine")
        public RuleTypeEngine flowEngine() throws Exception {
            Class<?> clazz = Class.forName("io.ruleforge.flow.FlowEngine");
            return (RuleTypeEngine) clazz.getDeclaredConstructor().newInstance();
        }
    }

    @Configuration
    @ConditionalOnClass(name = "io.ruleforge.script.ScriptingEngine")
    static class ScriptAutoConfiguration {
        @Bean
        @ConditionalOnMissingBean(name = "scriptingEngine")
        public RuleTypeEngine scriptingEngine() throws Exception {
            Class<?> clazz = Class.forName("io.ruleforge.script.ScriptingEngine");
            return (RuleTypeEngine) clazz.getDeclaredConstructor().newInstance();
        }
    }

    @Configuration
    @ConditionalOnClass(name = "io.ruleforge.dmn.DmnEngine")
    static class DmnAutoConfiguration {
        @Bean
        @ConditionalOnMissingBean(name = "dmnEngine")
        public RuleTypeEngine dmnEngine() throws Exception {
            Class<?> clazz = Class.forName("io.ruleforge.dmn.DmnEngine");
            return (RuleTypeEngine) clazz.getDeclaredConstructor().newInstance();
        }
    }

    @Configuration
    @ConditionalOnClass(name = "io.ruleforge.hierarchy.HierarchicalRuleResolver")
    static class HierarchyAutoConfiguration {
        @Bean
        @ConditionalOnMissingBean(name = "hierarchyResolver")
        public Object hierarchyResolver() throws Exception {
            Class<?> clazz = Class.forName("io.ruleforge.hierarchy.HierarchicalRuleResolver");
            return clazz.getDeclaredConstructor().newInstance();
        }
    }

    @Configuration
    @ConditionalOnClass(name = "io.ruleforge.cep.CepEngine")
    static class CepAutoConfiguration {
        @Bean
        @ConditionalOnMissingBean(name = "cepEngine")
        public Object cepEngine() throws Exception {
            Class<?> clazz = Class.forName("io.ruleforge.cep.CepEngine");
            return clazz.getDeclaredConstructor().newInstance();
        }
    }
}
