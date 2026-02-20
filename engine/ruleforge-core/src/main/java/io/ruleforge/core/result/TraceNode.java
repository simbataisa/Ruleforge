package io.ruleforge.core.result;

import java.time.Duration;
import java.util.List;
import java.util.Map;

public sealed interface TraceNode
    permits TraceNode.ConditionEval, TraceNode.ActionExec,
            TraceNode.RuleInvocation, TraceNode.FlowStep {

    record ConditionEval(
        String expression,
        Object leftValue,
        String operator,
        Object rightValue,
        boolean result,
        Duration time
    ) implements TraceNode {}

    record ActionExec(
        String action,
        String path,
        Object oldValue,
        Object newValue,
        Duration time
    ) implements TraceNode {}

    record RuleInvocation(
        String ruleId,
        String ruleName,
        RuleResult result,
        Duration time,
        List<TraceNode> children
    ) implements TraceNode {
        public RuleInvocation {
            children = children == null ? List.of() : List.copyOf(children);
        }
    }

    record FlowStep(
        String nodeId,
        String nodeType,
        TraceNode inner,
        Duration time
    ) implements TraceNode {}
}
