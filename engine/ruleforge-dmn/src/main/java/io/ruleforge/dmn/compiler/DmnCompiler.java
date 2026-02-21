package io.ruleforge.dmn.compiler;

import io.ruleforge.core.RuleCompilationException;
import io.ruleforge.core.model.RuleDefinition;
import io.ruleforge.core.model.RuleMetadata;
import io.ruleforge.dmn.feel.FeelType;
import io.ruleforge.dmn.model.DmnDecision;
import io.ruleforge.dmn.model.DmnDecisionTable;
import io.ruleforge.dmn.model.DmnDecisionTable.DmnRule;
import io.ruleforge.dmn.model.DmnDecisionTable.InputClause;
import io.ruleforge.dmn.model.DmnDecisionTable.OutputClause;
import io.ruleforge.dmn.model.DmnDefinition;
import io.ruleforge.dmn.model.DmnInputData;
import io.ruleforge.dmn.model.HitPolicy;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Compiles a {@link RuleDefinition} with type DMN_RULE into a {@link CompiledDmn}.
 * <p>
 * The definition payload is expected to contain:
 * <ul>
 *   <li>{@code name} - DMN model name</li>
 *   <li>{@code namespace} - DMN namespace</li>
 *   <li>{@code inputs} - list of input data definitions</li>
 *   <li>{@code decisions} - list of decision definitions</li>
 * </ul>
 */
public final class DmnCompiler {

    @SuppressWarnings("unchecked")
    public CompiledDmn compile(RuleDefinition ruleDefinition) {
        Map<String, Object> def = ruleDefinition.definition();
        if (def == null) {
            throw new RuleCompilationException(ruleDefinition.id(), "DMN definition payload is null");
        }

        String modelName = stringOrDefault(def.get("name"), ruleDefinition.name());
        String namespace = stringOrDefault(def.get("namespace"), "");

        // Parse inputs
        List<DmnInputData> inputData = parseInputData(ruleDefinition.id(), def.get("inputs"));

        // Parse decisions
        List<DmnDecision> decisions = parseDecisions(ruleDefinition.id(), def.get("decisions"));

        if (decisions.isEmpty()) {
            throw new RuleCompilationException(ruleDefinition.id(), "DMN model must have at least one decision");
        }

        // Validate decision dependencies (no cycles)
        validateNoCycles(ruleDefinition.id(), decisions);

        DmnDefinition dmnDef = new DmnDefinition(
                ruleDefinition.id(), modelName, namespace, decisions, inputData
        );

        RuleMetadata metadata = ruleDefinition.metadata() != null
                ? ruleDefinition.metadata() : RuleMetadata.defaults();

        return new CompiledDmn(
                ruleDefinition.id(),
                ruleDefinition.name(),
                ruleDefinition.version(),
                metadata,
                System.currentTimeMillis(),
                dmnDef
        );
    }

    @SuppressWarnings("unchecked")
    private List<DmnInputData> parseInputData(String ruleId, Object inputsRaw) {
        List<DmnInputData> result = new ArrayList<>();
        if (inputsRaw == null) return result;
        if (!(inputsRaw instanceof List<?> inputList)) {
            throw new RuleCompilationException(ruleId, "'inputs' must be a list");
        }
        for (int i = 0; i < inputList.size(); i++) {
            Object item = inputList.get(i);
            if (!(item instanceof Map<?, ?> inputMap)) {
                throw new RuleCompilationException(ruleId, "Input[" + i + "] must be a map");
            }
            Map<String, Object> map = (Map<String, Object>) inputMap;
            String id = stringOrDefault(map.get("id"), "input_" + i);
            String name = stringOrDefault(map.get("name"), id);
            FeelType type = parseFeelType(map.get("type"));
            result.add(new DmnInputData(id, name, type));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private List<DmnDecision> parseDecisions(String ruleId, Object decisionsRaw) {
        List<DmnDecision> result = new ArrayList<>();
        if (decisionsRaw == null) return result;
        if (!(decisionsRaw instanceof List<?> decisionList)) {
            throw new RuleCompilationException(ruleId, "'decisions' must be a list");
        }
        for (int i = 0; i < decisionList.size(); i++) {
            Object item = decisionList.get(i);
            if (!(item instanceof Map<?, ?> decMap)) {
                throw new RuleCompilationException(ruleId, "Decision[" + i + "] must be a map");
            }
            Map<String, Object> map = (Map<String, Object>) decMap;
            String id = stringOrDefault(map.get("id"), "decision_" + i);
            String name = stringOrDefault(map.get("name"), id);

            List<String> requiredInputs = parseStringList(map.get("requiredInputs"));
            List<String> requiredDecisions = parseStringList(map.get("requiredDecisions"));

            DmnDecisionTable table = parseDecisionTable(ruleId, id, map.get("decisionTable"));

            result.add(new DmnDecision(id, name, table, requiredInputs, requiredDecisions));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private DmnDecisionTable parseDecisionTable(String ruleId, String decisionId, Object tableRaw) {
        if (tableRaw == null) {
            throw new RuleCompilationException(ruleId,
                    "Decision '" + decisionId + "' must have a 'decisionTable'");
        }
        if (!(tableRaw instanceof Map<?, ?> tableMap)) {
            throw new RuleCompilationException(ruleId,
                    "Decision '" + decisionId + "': 'decisionTable' must be a map");
        }
        Map<String, Object> map = (Map<String, Object>) tableMap;

        HitPolicy hitPolicy = parseHitPolicy(map.get("hitPolicy"));

        List<InputClause> inputClauses = parseInputClauses(ruleId, decisionId, map.get("inputClauses"));
        List<OutputClause> outputClauses = parseOutputClauses(ruleId, decisionId, map.get("outputClauses"));
        List<DmnRule> rules = parseDmnRules(ruleId, decisionId, map.get("rules"));

        return new DmnDecisionTable(hitPolicy, inputClauses, outputClauses, rules);
    }

    @SuppressWarnings("unchecked")
    private List<InputClause> parseInputClauses(String ruleId, String decisionId, Object raw) {
        List<InputClause> result = new ArrayList<>();
        if (raw == null) {
            throw new RuleCompilationException(ruleId,
                    "Decision '" + decisionId + "': 'inputClauses' is required");
        }
        if (!(raw instanceof List<?> list)) {
            throw new RuleCompilationException(ruleId,
                    "Decision '" + decisionId + "': 'inputClauses' must be a list");
        }
        for (Object item : list) {
            if (item instanceof Map<?, ?> m) {
                Map<String, Object> map = (Map<String, Object>) m;
                String name = stringOrDefault(map.get("name"), "");
                String expr = stringOrDefault(map.get("expression"), name);
                result.add(new InputClause(name, expr));
            } else if (item instanceof String s) {
                result.add(new InputClause(s, s));
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private List<OutputClause> parseOutputClauses(String ruleId, String decisionId, Object raw) {
        List<OutputClause> result = new ArrayList<>();
        if (raw == null) {
            throw new RuleCompilationException(ruleId,
                    "Decision '" + decisionId + "': 'outputClauses' is required");
        }
        if (!(raw instanceof List<?> list)) {
            throw new RuleCompilationException(ruleId,
                    "Decision '" + decisionId + "': 'outputClauses' must be a list");
        }
        for (Object item : list) {
            if (item instanceof Map<?, ?> m) {
                Map<String, Object> map = (Map<String, Object>) m;
                String name = stringOrDefault(map.get("name"), "");
                String defaultValue = map.get("defaultValue") != null
                        ? map.get("defaultValue").toString() : null;
                result.add(new OutputClause(name, defaultValue));
            } else if (item instanceof String s) {
                result.add(new OutputClause(s, null));
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private List<DmnRule> parseDmnRules(String ruleId, String decisionId, Object raw) {
        List<DmnRule> result = new ArrayList<>();
        if (raw == null) return result;
        if (!(raw instanceof List<?> list)) {
            throw new RuleCompilationException(ruleId,
                    "Decision '" + decisionId + "': 'rules' must be a list");
        }
        for (int i = 0; i < list.size(); i++) {
            Object item = list.get(i);
            if (!(item instanceof Map<?, ?> ruleMap)) {
                throw new RuleCompilationException(ruleId,
                        "Decision '" + decisionId + "': rule[" + i + "] must be a map");
            }
            Map<String, Object> map = (Map<String, Object>) ruleMap;

            int ruleNumber = map.get("ruleNumber") instanceof Number n ? n.intValue() : i + 1;
            List<String> inputEntries = parseStringList(map.get("inputEntries"));
            List<String> outputEntries = parseStringList(map.get("outputEntries"));
            String annotation = map.get("annotation") != null ? map.get("annotation").toString() : null;

            result.add(new DmnRule(ruleNumber, inputEntries, outputEntries, annotation));
        }
        return result;
    }

    private void validateNoCycles(String ruleId, List<DmnDecision> decisions) {
        Set<String> visited = new HashSet<>();
        Set<String> inStack = new HashSet<>();
        Map<String, DmnDecision> byId = new java.util.HashMap<>();
        for (DmnDecision d : decisions) byId.put(d.id(), d);

        for (DmnDecision d : decisions) {
            if (!visited.contains(d.id())) {
                if (hasCycle(d.id(), byId, visited, inStack)) {
                    throw new RuleCompilationException(ruleId,
                            "Cycle detected in decision dependencies involving: " + d.id());
                }
            }
        }
    }

    private boolean hasCycle(String id, Map<String, DmnDecision> byId,
                             Set<String> visited, Set<String> inStack) {
        visited.add(id);
        inStack.add(id);
        DmnDecision d = byId.get(id);
        if (d != null) {
            for (String dep : d.requiredDecisions()) {
                if (!visited.contains(dep)) {
                    if (hasCycle(dep, byId, visited, inStack)) return true;
                } else if (inStack.contains(dep)) {
                    return true;
                }
            }
        }
        inStack.remove(id);
        return false;
    }

    private HitPolicy parseHitPolicy(Object raw) {
        if (raw == null) return HitPolicy.UNIQUE;
        try {
            return HitPolicy.valueOf(raw.toString().toUpperCase());
        } catch (IllegalArgumentException e) {
            return HitPolicy.UNIQUE;
        }
    }

    private FeelType parseFeelType(Object raw) {
        if (raw == null) return FeelType.ANY;
        try {
            return FeelType.valueOf(raw.toString().toUpperCase());
        } catch (IllegalArgumentException e) {
            return FeelType.ANY;
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> parseStringList(Object raw) {
        if (raw == null) return List.of();
        if (raw instanceof List<?> list) {
            List<String> result = new ArrayList<>();
            for (Object item : list) {
                result.add(item != null ? item.toString() : "");
            }
            return result;
        }
        return List.of();
    }

    private String stringOrDefault(Object val, String defaultVal) {
        return val != null ? val.toString() : defaultVal;
    }
}
