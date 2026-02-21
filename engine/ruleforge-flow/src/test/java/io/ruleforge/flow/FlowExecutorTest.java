package io.ruleforge.flow;

import io.ruleforge.core.memory.WorkingMemory;
import io.ruleforge.core.model.FactType;
import io.ruleforge.core.result.RuleResult;
import io.ruleforge.core.result.RuleResultMetadata;
import io.ruleforge.core.spi.SubRuleInvoker;
import io.ruleforge.flow.executor.FlowExecutor;
import io.ruleforge.flow.graph.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class FlowExecutorTest {

    private WorkingMemory wm;

    @BeforeEach
    void setUp() {
        wm = new WorkingMemory();
    }

    @Test
    void executesSimpleActionFlow() {
        // Start -> Action -> End
        FlowGraph graph = new FlowGraph(
                List.of(
                        new ActionFlowNode("start", Map.of("output", "hello")),
                        new ActionFlowNode("end", Map.of("done", true))
                ),
                List.of(new FlowEdge("start", "end")),
                "start", 100
        );

        FlowExecutor executor = new FlowExecutor("test-rule");
        RuleResult result = executor.execute(graph, wm);

        assertThat(result.output()).containsEntry("output", "hello");
        assertThat(result.output()).containsEntry("done", true);
    }

    @Test
    void conditionNodeBranchesCorrectly() {
        wm.assertFact("approved", true, FactType.BOOLEAN);

        FlowGraph graph = new FlowGraph(
                List.of(
                        new ConditionFlowNode("check", "approved"),
                        new ActionFlowNode("yes", Map.of("result", "approved")),
                        new ActionFlowNode("no", Map.of("result", "rejected"))
                ),
                List.of(
                        new FlowEdge("check", "yes", "true"),
                        new FlowEdge("check", "no", "false")
                ),
                "check", 100
        );

        FlowExecutor executor = new FlowExecutor("test-rule");
        RuleResult result = executor.execute(graph, wm);

        assertThat(result.output()).containsEntry("result", "approved");
    }

    @Test
    void subRuleNodeInvokesSubRule() {
        SubRuleInvoker invoker = (ruleId, memory) -> new RuleResult(
                Map.of("subResult", 42),
                new RuleResultMetadata(Duration.ZERO, 1, 1, 0, "TEST"),
                List.of(), null
        );

        FlowGraph graph = new FlowGraph(
                List.of(new SubRuleFlowNode("start", "sub-rule-1")),
                List.of(),
                "start", 100
        );

        FlowExecutor executor = new FlowExecutor("test-rule", invoker);
        RuleResult result = executor.execute(graph, wm);

        assertThat(result.output()).containsEntry("subResult", 42);
    }

    @Test
    void subRuleNodeFallsBackWithoutInvoker() {
        FlowGraph graph = new FlowGraph(
                List.of(new SubRuleFlowNode("start", "sub-rule-1")),
                List.of(),
                "start", 100
        );

        FlowExecutor executor = new FlowExecutor("test-rule");
        RuleResult result = executor.execute(graph, wm);

        assertThat(result.output()).containsEntry("_subRuleInvoked", "sub-rule-1");
    }

    @Test
    void forkJoinTraversesBothBranches() {
        FlowGraph graph = new FlowGraph(
                List.of(
                        new ForkFlowNode("fork"),
                        new ActionFlowNode("branch1", Map.of("a", 1)),
                        new ActionFlowNode("branch2", Map.of("b", 2)),
                        new JoinFlowNode("join"),
                        new ActionFlowNode("end", Map.of("c", 3))
                ),
                List.of(
                        new FlowEdge("fork", "branch1"),
                        new FlowEdge("fork", "branch2"),
                        new FlowEdge("branch1", "join"),
                        new FlowEdge("branch2", "join"),
                        new FlowEdge("join", "end")
                ),
                "fork", 100
        );

        FlowExecutor executor = new FlowExecutor("test-rule");
        RuleResult result = executor.execute(graph, wm);

        assertThat(result.output()).containsEntry("a", 1);
        assertThat(result.output()).containsEntry("b", 2);
        assertThat(result.output()).containsEntry("c", 3);
    }

    @Test
    void rflExpressionEvaluation() {
        wm.assertFact("score", 85, FactType.INTEGER);

        FlowGraph graph = new FlowGraph(
                List.of(
                        new ConditionFlowNode("check", "score >= 70"),
                        new ActionFlowNode("pass", Map.of("grade", "pass")),
                        new ActionFlowNode("fail", Map.of("grade", "fail"))
                ),
                List.of(
                        new FlowEdge("check", "pass", "true"),
                        new FlowEdge("check", "fail", "false")
                ),
                "check", 100
        );

        FlowExecutor executor = new FlowExecutor("test-rule");
        RuleResult result = executor.execute(graph, wm);

        assertThat(result.output()).containsEntry("grade", "pass");
    }

    @Test
    void maxIterationsLimitsExecution() {
        // Create a graph where BFS would visit many nodes
        FlowGraph graph = new FlowGraph(
                List.of(
                        new ActionFlowNode("n1", Map.of("v1", 1)),
                        new ActionFlowNode("n2", Map.of("v2", 2)),
                        new ActionFlowNode("n3", Map.of("v3", 3))
                ),
                List.of(
                        new FlowEdge("n1", "n2"),
                        new FlowEdge("n2", "n3")
                ),
                "n1", 2 // max 2 iterations
        );

        FlowExecutor executor = new FlowExecutor("test-rule");
        RuleResult result = executor.execute(graph, wm);

        // Should visit n1 and n2 but not n3
        assertThat(result.output()).containsKey("v1");
        assertThat(result.output()).containsKey("v2");
        assertThat(result.output()).doesNotContainKey("v3");
    }

    @Test
    void actionNodeAssertsFacts() {
        FlowGraph graph = new FlowGraph(
                List.of(new ActionFlowNode("action", Map.of("computed", 42))),
                List.of(),
                "action", 100
        );

        FlowExecutor executor = new FlowExecutor("test-rule");
        executor.execute(graph, wm);

        assertThat((Integer) wm.get("computed")).isEqualTo(42);
    }
}
