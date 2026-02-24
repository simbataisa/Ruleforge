package io.ruleforge.flow.compiler;

import io.ruleforge.core.model.CallableAction;
import io.ruleforge.core.model.RuleDefinition;
import io.ruleforge.flow.graph.*;

import java.util.*;

/**
 * Compiles a {@link RuleDefinition} with a flow payload into a {@link CompiledFlowRule}.
 */
public final class FlowCompiler {

    @SuppressWarnings("unchecked")
    public CompiledFlowRule compile(RuleDefinition definition) {
        Map<String, Object> payload = definition.definition();
        if (payload == null) {
            throw new IllegalArgumentException("Flow definition payload is null for rule: " + definition.id());
        }

        List<FlowNode> nodes = parseNodes((List<Map<String, Object>>) payload.getOrDefault("nodes", List.of()));
        List<FlowEdge> edges = parseEdges((List<Map<String, Object>>) payload.getOrDefault("edges", List.of()));

        String startNodeId = (String) payload.get("startNodeId");
        if (startNodeId == null && !nodes.isEmpty()) {
            startNodeId = nodes.get(0).id();
        }

        int maxIterations = ((Number) payload.getOrDefault("maxIterations", 100)).intValue();

        FlowGraph graph = new FlowGraph(nodes, edges, startNodeId, maxIterations);

        return new CompiledFlowRule(
                definition.id(),
                definition.metadata(),
                System.currentTimeMillis(),
                graph
        );
    }

    @SuppressWarnings("unchecked")
    private List<FlowNode> parseNodes(List<Map<String, Object>> nodeDefs) {
        List<FlowNode> nodes = new ArrayList<>();
        for (Map<String, Object> nodeDef : nodeDefs) {
            String id = (String) nodeDef.get("id");
            String type = (String) nodeDef.get("type");
            if (id == null || type == null) continue;

            FlowNode node = switch (type) {
                case "CONDITION" -> new ConditionFlowNode(id, (String) nodeDef.getOrDefault("expression", "true"));
                case "ACTION" -> {
                    Object outputsObj = nodeDef.get("outputs");
                    Map<String, Object> outputs = outputsObj instanceof Map<?, ?> m
                            ? (Map<String, Object>) m
                            : Map.of();
                    List<CallableAction> callableActions = parseCallableActions(nodeDef);
                    yield new ActionFlowNode(id, outputs, callableActions);
                }
                case "GATE" -> new GateFlowNode(id, (String) nodeDef.getOrDefault("gateType", "AND"));
                case "FORK" -> new ForkFlowNode(id);
                case "JOIN" -> new JoinFlowNode(id);
                case "SUB_RULE" -> new SubRuleFlowNode(id, (String) nodeDef.get("ruleId"));
                default -> new ActionFlowNode(id, Map.of()); // unknown type treated as no-op
            };
            nodes.add(node);
        }
        return nodes;
    }

    @SuppressWarnings("unchecked")
    private List<CallableAction> parseCallableActions(Map<String, Object> nodeDef) {
        Object actionsObj = nodeDef.get("callableActions");
        if (!(actionsObj instanceof List<?> actionsList) || actionsList.isEmpty()) {
            return List.of();
        }
        List<CallableAction> actions = new ArrayList<>();
        for (Object actionObj : actionsList) {
            if (!(actionObj instanceof Map<?, ?> am)) continue;
            Map<String, Object> actionMap = (Map<String, Object>) am;

            String actionId = actionMap.getOrDefault("actionId", "action_" + actions.size()).toString();
            String protocol = actionMap.getOrDefault("protocol", "").toString();
            String target = actionMap.getOrDefault("target", "").toString();

            Map<String, Object> parameters;
            Object paramsObj = actionMap.get("parameters");
            if (paramsObj instanceof Map<?, ?> pm) {
                parameters = new LinkedHashMap<>((Map<String, Object>) pm);
            } else {
                parameters = Map.of();
            }

            Object outputMappingObj = actionMap.get("outputMapping");
            String outputMapping = outputMappingObj != null ? outputMappingObj.toString() : null;

            actions.add(new CallableAction(actionId, protocol, target, parameters, outputMapping));
        }
        return actions;
    }

    private List<FlowEdge> parseEdges(List<Map<String, Object>> edgeDefs) {
        List<FlowEdge> edges = new ArrayList<>();
        for (Map<String, Object> edgeDef : edgeDefs) {
            String from = (String) edgeDef.get("from");
            String to = (String) edgeDef.get("to");
            String condition = (String) edgeDef.getOrDefault("condition", "true");
            if (from != null && to != null) {
                edges.add(new FlowEdge(from, to, condition));
            }
        }
        return edges;
    }
}
