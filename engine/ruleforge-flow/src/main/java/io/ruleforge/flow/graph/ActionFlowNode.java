package io.ruleforge.flow.graph;

import java.util.Map;

/**
 * An action node that sets output facts and terminates the flow path.
 */
public record ActionFlowNode(String id, Map<String, Object> outputs) implements FlowNode {

    public ActionFlowNode {
        outputs = outputs == null ? Map.of() : Map.copyOf(outputs);
    }

    @Override
    public String type() {
        return "ACTION";
    }
}
