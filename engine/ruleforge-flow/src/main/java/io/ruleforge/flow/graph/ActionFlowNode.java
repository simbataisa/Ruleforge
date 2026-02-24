package io.ruleforge.flow.graph;

import io.ruleforge.core.model.CallableAction;

import java.util.List;
import java.util.Map;

/**
 * An action node that sets output facts, optionally executes callable actions,
 * and terminates the flow path.
 */
public record ActionFlowNode(
        String id,
        Map<String, Object> outputs,
        List<CallableAction> callableActions
) implements FlowNode {

    public ActionFlowNode(String id, Map<String, Object> outputs) {
        this(id, outputs, List.of());
    }

    public ActionFlowNode {
        outputs = outputs == null ? Map.of() : Map.copyOf(outputs);
        callableActions = callableActions == null ? List.of() : List.copyOf(callableActions);
    }

    @Override
    public String type() {
        return "ACTION";
    }
}
