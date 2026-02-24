package io.ruleforge.tree.compiler;

import io.ruleforge.core.RuleCompilationException;
import io.ruleforge.core.expression.Expression;
import io.ruleforge.core.expression.RflParser;
import io.ruleforge.core.model.CallableAction;
import io.ruleforge.core.model.RuleDefinition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Compiles a {@link RuleDefinition} of type DECISION_TREE into a {@link CompiledDecisionTree}.
 * <p>
 * The definition payload is expected to contain:
 * <ul>
 *   <li>{@code "root"} - a Map representing the root tree node</li>
 *   <li>{@code "scoringMode"} - optional boolean, defaults to false</li>
 *   <li>{@code "earlyTermination"} - optional boolean, defaults to true</li>
 * </ul>
 * <p>
 * Each node map must contain a {@code "nodeType"} discriminator field with one of:
 * CONDITION, ACTION, SUB_RULE, SCORE, GATE.
 */
public final class TreeCompiler {

    private final RflParser rflParser = new RflParser();

    /**
     * Compiles the given rule definition into a {@link CompiledDecisionTree}.
     *
     * @param definition the rule definition to compile
     * @return the compiled decision tree
     * @throws RuleCompilationException if the definition is malformed or cannot be compiled
     */
    public CompiledDecisionTree compile(RuleDefinition definition) {
        Map<String, Object> payload = definition.definition();
        if (payload == null) {
            throw new RuleCompilationException(definition.id(), "Definition payload is null");
        }

        Object rootObj = payload.get("root");
        if (rootObj == null) {
            throw new RuleCompilationException(definition.id(), "Missing 'root' in definition");
        }
        if (!(rootObj instanceof Map<?, ?>)) {
            throw new RuleCompilationException(definition.id(), "'root' must be a Map, got: " + rootObj.getClass().getSimpleName());
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> rootMap = (Map<String, Object>) rootObj;

        boolean scoringMode = toBoolean(payload.get("scoringMode"), false);
        boolean earlyTermination = toBoolean(payload.get("earlyTermination"), !scoringMode);

        CompiledTreeNode compiledRoot;
        try {
            compiledRoot = compileNode(rootMap, definition.id());
        } catch (RuleCompilationException e) {
            throw e;
        } catch (Exception e) {
            throw new RuleCompilationException(definition.id(), "Failed to compile tree: " + e.getMessage(), e);
        }

        return new CompiledDecisionTree(
                definition.id(),
                definition.metadata(),
                System.currentTimeMillis(),
                compiledRoot,
                scoringMode,
                earlyTermination
        );
    }

    // ------------------------------------------------------------------
    // Node compilation
    // ------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private CompiledTreeNode compileNode(Map<String, Object> nodeMap, String ruleId) {
        String nodeType = requireString(nodeMap, "nodeType", ruleId);
        String nodeId = requireString(nodeMap, "nodeId", ruleId);

        return switch (nodeType) {
            case "CONDITION" -> compileConditionNode(nodeMap, nodeId, ruleId);
            case "ACTION" -> compileActionNode(nodeMap, nodeId);
            case "SUB_RULE" -> compileSubRuleNode(nodeMap, nodeId, ruleId);
            case "SCORE" -> compileScoreNode(nodeMap, nodeId);
            case "GATE" -> compileGateNode(nodeMap, nodeId, ruleId);
            default -> throw new RuleCompilationException(ruleId,
                    "Unknown nodeType '" + nodeType + "' at node '" + nodeId + "'");
        };
    }

    @SuppressWarnings("unchecked")
    private CompiledTreeNode compileConditionNode(Map<String, Object> nodeMap, String nodeId, String ruleId) {
        String field = requireString(nodeMap, "field", ruleId);
        String operator = requireString(nodeMap, "operator", ruleId);

        Object branchesObj = nodeMap.get("branches");
        if (!(branchesObj instanceof List<?> branchesList)) {
            throw new RuleCompilationException(ruleId,
                    "Condition node '" + nodeId + "' must have a 'branches' list");
        }

        List<CompiledTreeNode.CompiledConditionNode.CompiledBranch> compiledBranches = new ArrayList<>();
        for (Object branchObj : branchesList) {
            if (!(branchObj instanceof Map<?, ?>)) {
                throw new RuleCompilationException(ruleId,
                        "Each branch in node '" + nodeId + "' must be a Map");
            }
            Map<String, Object> branchMap = (Map<String, Object>) branchObj;

            Object value = branchMap.get("value");
            Object secondValue = branchMap.get("secondValue");
            double weight = toDouble(branchMap.get("weight"), 1.0);

            Object childObj = branchMap.get("child");
            if (!(childObj instanceof Map<?, ?>)) {
                throw new RuleCompilationException(ruleId,
                        "Branch in node '" + nodeId + "' must have a 'child' Map");
            }
            CompiledTreeNode childNode = compileNode((Map<String, Object>) childObj, ruleId);

            compiledBranches.add(new CompiledTreeNode.CompiledConditionNode.CompiledBranch(
                    value, secondValue, weight, childNode));
        }

        // Optional default branch
        CompiledTreeNode defaultBranch = null;
        Object defaultObj = nodeMap.get("defaultBranch");
        if (defaultObj instanceof Map<?, ?>) {
            defaultBranch = compileNode((Map<String, Object>) defaultObj, ruleId);
        }

        return new CompiledTreeNode.CompiledConditionNode(
                nodeId, field, operator, Collections.unmodifiableList(compiledBranches), defaultBranch);
    }

    @SuppressWarnings("unchecked")
    private CompiledTreeNode compileActionNode(Map<String, Object> nodeMap, String nodeId) {
        Object outputsObj = nodeMap.get("outputs");
        Map<String, Object> outputs;
        if (outputsObj instanceof Map<?, ?> m) {
            outputs = Collections.unmodifiableMap(new LinkedHashMap<>((Map<String, Object>) m));
        } else {
            outputs = Map.of();
        }

        List<CallableAction> callableActions = compileCallableActions(nodeMap);
        return new CompiledTreeNode.CompiledActionNode(nodeId, outputs, callableActions);
    }

    @SuppressWarnings("unchecked")
    private List<CallableAction> compileCallableActions(Map<String, Object> nodeMap) {
        Object actionsObj = nodeMap.get("callableActions");
        if (!(actionsObj instanceof List<?> actionsList) || actionsList.isEmpty()) {
            return List.of();
        }

        List<CallableAction> actions = new ArrayList<>();
        for (Object actionObj : actionsList) {
            if (!(actionObj instanceof Map<?, ?> actionMap)) continue;
            Map<String, Object> am = (Map<String, Object>) actionMap;

            String actionId = am.getOrDefault("actionId", "action_" + actions.size()).toString();
            String protocol = am.getOrDefault("protocol", "").toString();
            String target = am.getOrDefault("target", "").toString();

            Map<String, Object> parameters;
            Object paramsObj = am.get("parameters");
            if (paramsObj instanceof Map<?, ?> pm) {
                parameters = new LinkedHashMap<>((Map<String, Object>) pm);
            } else {
                parameters = Map.of();
            }

            Object outputMappingObj = am.get("outputMapping");
            String outputMapping = outputMappingObj != null ? outputMappingObj.toString() : null;

            actions.add(new CallableAction(actionId, protocol, target, parameters, outputMapping));
        }
        return Collections.unmodifiableList(actions);
    }

    private CompiledTreeNode compileSubRuleNode(Map<String, Object> nodeMap, String nodeId, String ruleId) {
        String subRuleId = requireString(nodeMap, "subRuleId", ruleId);
        int subRuleVersion = toInt(nodeMap.get("subRuleVersion"), 1);
        return new CompiledTreeNode.CompiledSubRuleNode(nodeId, subRuleId, subRuleVersion);
    }

    @SuppressWarnings("unchecked")
    private CompiledTreeNode compileScoreNode(Map<String, Object> nodeMap, String nodeId) {
        double score = toDouble(nodeMap.get("score"), 0.0);
        Object outputsObj = nodeMap.get("outputs");
        Map<String, Object> outputs;
        if (outputsObj instanceof Map<?, ?> m) {
            outputs = Collections.unmodifiableMap(new LinkedHashMap<>((Map<String, Object>) m));
        } else {
            outputs = Map.of();
        }
        return new CompiledTreeNode.CompiledScoreNode(nodeId, score, outputs);
    }

    @SuppressWarnings("unchecked")
    private CompiledTreeNode compileGateNode(Map<String, Object> nodeMap, String nodeId, String ruleId) {
        String expressionStr = requireString(nodeMap, "expression", ruleId);
        Expression compiledExpr;
        try {
            compiledExpr = rflParser.parse(expressionStr);
        } catch (Exception e) {
            throw new RuleCompilationException(ruleId,
                    "Failed to parse gate expression '" + expressionStr + "' at node '" + nodeId + "': " + e.getMessage(), e);
        }

        Object trueObj = nodeMap.get("trueChild");
        Object falseObj = nodeMap.get("falseChild");

        CompiledTreeNode trueChild = null;
        if (trueObj instanceof Map<?, ?>) {
            trueChild = compileNode((Map<String, Object>) trueObj, ruleId);
        }

        CompiledTreeNode falseChild = null;
        if (falseObj instanceof Map<?, ?>) {
            falseChild = compileNode((Map<String, Object>) falseObj, ruleId);
        }

        if (trueChild == null && falseChild == null) {
            throw new RuleCompilationException(ruleId,
                    "Gate node '" + nodeId + "' must have at least one of 'trueChild' or 'falseChild'");
        }

        return new CompiledTreeNode.CompiledGateNode(nodeId, compiledExpr, trueChild, falseChild);
    }

    // ------------------------------------------------------------------
    // Utility methods
    // ------------------------------------------------------------------

    private static String requireString(Map<String, Object> map, String key, String ruleId) {
        Object val = map.get(key);
        if (val == null) {
            throw new RuleCompilationException(ruleId, "Missing required field '" + key + "' in node " + map.get("nodeId"));
        }
        return val.toString();
    }

    private static boolean toBoolean(Object value, boolean defaultValue) {
        if (value == null) return defaultValue;
        if (value instanceof Boolean b) return b;
        return Boolean.parseBoolean(value.toString());
    }

    private static double toDouble(Object value, double defaultValue) {
        if (value == null) return defaultValue;
        if (value instanceof Number n) return n.doubleValue();
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static int toInt(Object value, int defaultValue) {
        if (value == null) return defaultValue;
        if (value instanceof Number n) return n.intValue();
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
