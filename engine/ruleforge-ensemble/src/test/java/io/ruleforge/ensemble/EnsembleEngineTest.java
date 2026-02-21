package io.ruleforge.ensemble;

import io.ruleforge.core.memory.WorkingMemory;
import io.ruleforge.core.model.RuleMetadata;
import io.ruleforge.core.model.RuleType;
import io.ruleforge.core.result.RuleResult;
import io.ruleforge.core.result.RuleResultMetadata;
import io.ruleforge.core.spi.SubRuleInvoker;
import io.ruleforge.ensemble.compiler.CompiledEnsemble;
import io.ruleforge.ensemble.model.AggregationStrategy;
import io.ruleforge.ensemble.model.EnsembleDefinition;
import io.ruleforge.ensemble.model.MemberRule;
import io.ruleforge.core.engine.DefaultEvaluationContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class EnsembleEngineTest {

    private final EnsembleEngine engine = new EnsembleEngine();
    private WorkingMemory wm;

    @BeforeEach
    void setUp() {
        wm = new WorkingMemory();
    }

    @Test
    void supportedTypeIsEnsemble() {
        assertThat(engine.supportedType()).isEqualTo(RuleType.ENSEMBLE);
    }

    @Test
    void evaluateWithoutInvokerReturnsFallback() {
        CompiledEnsemble rule = makeEnsemble(AggregationStrategy.AVERAGE, "result",
                new MemberRule("r1", "member1", 1.0, 0));

        DefaultEvaluationContext ctx = new DefaultEvaluationContext(wm, "corr-1");
        RuleResult result = engine.evaluate(rule, wm, ctx);

        assertThat(result.output()).containsEntry("ensembleReady", true);
        assertThat(result.output()).containsEntry("memberCount", 1);
    }

    @Test
    void evaluateAverageStrategy() {
        SubRuleInvoker invoker = (ruleId, memory) -> {
            double value = ruleId.equals("r1") ? 10.0 : 20.0;
            return new RuleResult(Map.of("result", value), metaStub(), List.of(), null);
        };

        CompiledEnsemble rule = makeEnsemble(AggregationStrategy.AVERAGE, "result",
                new MemberRule("r1", "m1", 1.0, 0),
                new MemberRule("r2", "m2", 1.0, 0));

        DefaultEvaluationContext ctx = new DefaultEvaluationContext(
                wm, "corr-1", Map.of(), Map.of(), 0, invoker);
        RuleResult result = engine.evaluate(rule, wm, ctx);

        assertThat(result.output().get("result")).isEqualTo(15.0);
    }

    @Test
    void evaluateWeightedAverageStrategy() {
        SubRuleInvoker invoker = (ruleId, memory) -> {
            double value = ruleId.equals("r1") ? 10.0 : 30.0;
            return new RuleResult(Map.of("result", value), metaStub(), List.of(), null);
        };

        CompiledEnsemble rule = makeEnsemble(AggregationStrategy.WEIGHTED_AVERAGE, "result",
                new MemberRule("r1", "m1", 0.75, 0),
                new MemberRule("r2", "m2", 0.25, 0));

        DefaultEvaluationContext ctx = new DefaultEvaluationContext(
                wm, "corr-1", Map.of(), Map.of(), 0, invoker);
        RuleResult result = engine.evaluate(rule, wm, ctx);

        // (10 * 0.75 + 30 * 0.25) / (0.75 + 0.25) = 15.0
        assertThat((Double) result.output().get("result")).isEqualTo(15.0);
    }

    @Test
    void evaluateMaxStrategy() {
        SubRuleInvoker invoker = (ruleId, memory) -> {
            double value = switch (ruleId) {
                case "r1" -> 5.0;
                case "r2" -> 15.0;
                default -> 10.0;
            };
            return new RuleResult(Map.of("result", value), metaStub(), List.of(), null);
        };

        CompiledEnsemble rule = makeEnsemble(AggregationStrategy.MAX, "result",
                new MemberRule("r1", "m1", 1.0, 0),
                new MemberRule("r2", "m2", 1.0, 0),
                new MemberRule("r3", "m3", 1.0, 0));

        DefaultEvaluationContext ctx = new DefaultEvaluationContext(
                wm, "corr-1", Map.of(), Map.of(), 0, invoker);
        RuleResult result = engine.evaluate(rule, wm, ctx);

        assertThat((Double) result.output().get("result")).isEqualTo(15.0);
    }

    @Test
    void evaluateMinStrategy() {
        SubRuleInvoker invoker = (ruleId, memory) -> {
            double value = ruleId.equals("r1") ? 3.0 : 7.0;
            return new RuleResult(Map.of("result", value), metaStub(), List.of(), null);
        };

        CompiledEnsemble rule = makeEnsemble(AggregationStrategy.MIN, "result",
                new MemberRule("r1", "m1", 1.0, 0),
                new MemberRule("r2", "m2", 1.0, 0));

        DefaultEvaluationContext ctx = new DefaultEvaluationContext(
                wm, "corr-1", Map.of(), Map.of(), 0, invoker);
        RuleResult result = engine.evaluate(rule, wm, ctx);

        assertThat((Double) result.output().get("result")).isEqualTo(3.0);
    }

    @Test
    void evaluateMajorityVoteStrategy() {
        SubRuleInvoker invoker = (ruleId, memory) -> {
            String vote = ruleId.equals("r3") ? "B" : "A";
            return new RuleResult(Map.of("result", vote), metaStub(), List.of(), null);
        };

        CompiledEnsemble rule = makeEnsemble(AggregationStrategy.MAJORITY_VOTE, "result",
                new MemberRule("r1", "m1", 1.0, 0),
                new MemberRule("r2", "m2", 1.0, 0),
                new MemberRule("r3", "m3", 1.0, 0));

        DefaultEvaluationContext ctx = new DefaultEvaluationContext(
                wm, "corr-1", Map.of(), Map.of(), 0, invoker);
        RuleResult result = engine.evaluate(rule, wm, ctx);

        assertThat(result.output().get("result")).isEqualTo("A");
    }

    @Test
    void evaluateFirstMatchStrategy() {
        SubRuleInvoker invoker = (ruleId, memory) -> {
            if (ruleId.equals("r1")) {
                return new RuleResult(Map.of("result", "first"), metaStub(), List.of(), null);
            }
            return new RuleResult(Map.of("result", "second"), metaStub(), List.of(), null);
        };

        CompiledEnsemble rule = makeEnsemble(AggregationStrategy.FIRST_MATCH, "result",
                new MemberRule("r1", "m1", 1.0, 0),
                new MemberRule("r2", "m2", 1.0, 0));

        DefaultEvaluationContext ctx = new DefaultEvaluationContext(
                wm, "corr-1", Map.of(), Map.of(), 0, invoker);
        RuleResult result = engine.evaluate(rule, wm, ctx);

        assertThat(result.output().get("result")).isEqualTo("first");
    }

    @Test
    void memberFailureIsSkipped() {
        SubRuleInvoker invoker = (ruleId, memory) -> {
            if (ruleId.equals("r1")) throw new RuntimeException("boom");
            return new RuleResult(Map.of("result", 42.0), metaStub(), List.of(), null);
        };

        CompiledEnsemble rule = makeEnsemble(AggregationStrategy.AVERAGE, "result",
                new MemberRule("r1", "m1", 1.0, 0),
                new MemberRule("r2", "m2", 1.0, 0));

        DefaultEvaluationContext ctx = new DefaultEvaluationContext(
                wm, "corr-1", Map.of(), Map.of(), 0, invoker);
        RuleResult result = engine.evaluate(rule, wm, ctx);

        assertThat(result.output().get("result")).isEqualTo(42.0);
    }

    private CompiledEnsemble makeEnsemble(AggregationStrategy strategy, String outputField, MemberRule... members) {
        EnsembleDefinition def = new EnsembleDefinition(
                "ensemble-1", "Test Ensemble", List.of(members), strategy, outputField);
        return new CompiledEnsemble("ensemble-1", RuleMetadata.defaults(), System.currentTimeMillis(), def);
    }

    private RuleResultMetadata metaStub() {
        return new RuleResultMetadata(Duration.ZERO, 1, 1, 0, "TEST");
    }
}
