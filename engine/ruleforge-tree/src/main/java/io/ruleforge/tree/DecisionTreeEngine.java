package io.ruleforge.tree;

import io.ruleforge.core.RuleEvaluationException;
import io.ruleforge.core.memory.WorkingMemory;
import io.ruleforge.core.model.CompiledRule;
import io.ruleforge.core.model.RuleDefinition;
import io.ruleforge.core.model.RuleType;
import io.ruleforge.core.model.ValidationResult;
import io.ruleforge.core.model.ValidationResult.ValidationError;
import io.ruleforge.core.result.RuleResult;
import io.ruleforge.core.spi.EvaluationContext;
import io.ruleforge.core.spi.RuleTypeEngine;
import io.ruleforge.tree.compiler.CompiledDecisionTree;
import io.ruleforge.tree.compiler.TreeCompiler;
import io.ruleforge.tree.evaluator.TreeTraverser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Engine for evaluating decision tree rules.
 * <p>
 * Implements the {@link RuleTypeEngine} SPI for rule type {@link RuleType#DECISION_TREE}.
 * Supports multi-branch conditions, weighted branches, gate expressions, scoring mode,
 * and early termination.
 * <p>
 * Compilation delegates to {@link TreeCompiler} which parses the tree structure from the
 * definition payload. Evaluation delegates to {@link TreeTraverser} which performs
 * depth-first traversal of the compiled tree.
 */
public final class DecisionTreeEngine implements RuleTypeEngine {

    private final TreeCompiler compiler = new TreeCompiler();

    @Override
    public RuleType supportedType() {
        return RuleType.DECISION_TREE;
    }

    @Override
    public CompiledRule compile(RuleDefinition definition) {
        return compiler.compile(definition);
    }

    @Override
    public RuleResult evaluate(CompiledRule rule, WorkingMemory workingMemory, EvaluationContext ctx) {
        if (!(rule instanceof CompiledDecisionTree tree)) {
            throw new RuleEvaluationException(
                    rule.ruleId(),
                    "Expected CompiledDecisionTree but got " + rule.getClass().getSimpleName());
        }
        var traverser = new TreeTraverser(
                tree.ruleId(), tree.scoringMode(), tree.earlyTermination(), ctx);
        return traverser.traverse(tree.root(), workingMemory);
    }

    @Override
    public ValidationResult validate(RuleDefinition definition) {
        List<ValidationError> errors = new ArrayList<>();

        // Check rule type
        if (definition.type() != RuleType.DECISION_TREE) {
            errors.add(new ValidationError(
                    "type", "INVALID_TYPE",
                    "Expected DECISION_TREE but got " + definition.type()));
            return ValidationResult.failure(errors);
        }

        // Check definition payload exists
        Map<String, Object> payload = definition.definition();
        if (payload == null) {
            errors.add(new ValidationError(
                    "definition", "NULL_DEFINITION",
                    "Definition payload must not be null"));
            return ValidationResult.failure(errors);
        }

        // Check root node exists
        Object rootObj = payload.get("root");
        if (rootObj == null) {
            errors.add(new ValidationError(
                    "definition.root", "MISSING_ROOT",
                    "Decision tree must have a 'root' node"));
            return ValidationResult.failure(errors);
        }

        if (!(rootObj instanceof Map<?, ?>)) {
            errors.add(new ValidationError(
                    "definition.root", "INVALID_ROOT",
                    "'root' must be a Map, got " + rootObj.getClass().getSimpleName()));
            return ValidationResult.failure(errors);
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> rootMap = (Map<String, Object>) rootObj;

        // Validate tree structure recursively
        validateNode(rootMap, "definition.root", errors);

        return errors.isEmpty() ? ValidationResult.success() : ValidationResult.failure(errors);
    }

    // ------------------------------------------------------------------
    // Recursive tree validation
    // ------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private void validateNode(Map<String, Object> nodeMap, String path, List<ValidationError> errors) {
        // nodeType is required
        Object nodeTypeObj = nodeMap.get("nodeType");
        if (nodeTypeObj == null) {
            errors.add(new ValidationError(
                    path + ".nodeType", "MISSING_NODE_TYPE",
                    "Every node must have a 'nodeType' field"));
            return;
        }
        String nodeType = nodeTypeObj.toString();

        // nodeId is required
        if (nodeMap.get("nodeId") == null) {
            errors.add(new ValidationError(
                    path + ".nodeId", "MISSING_NODE_ID",
                    "Every node must have a 'nodeId' field"));
        }

        switch (nodeType) {
            case "CONDITION" -> validateConditionNode(nodeMap, path, errors);
            case "ACTION" -> validateActionNode(nodeMap, path, errors);
            case "SUB_RULE" -> validateSubRuleNode(nodeMap, path, errors);
            case "SCORE" -> validateScoreNode(nodeMap, path, errors);
            case "GATE" -> validateGateNode(nodeMap, path, errors);
            default -> errors.add(new ValidationError(
                    path + ".nodeType", "UNKNOWN_NODE_TYPE",
                    "Unknown node type: " + nodeType + ". Expected one of: CONDITION, ACTION, SUB_RULE, SCORE, GATE"));
        }
    }

    @SuppressWarnings("unchecked")
    private void validateConditionNode(Map<String, Object> nodeMap, String path,
                                       List<ValidationError> errors) {
        if (nodeMap.get("field") == null) {
            errors.add(new ValidationError(
                    path + ".field", "MISSING_FIELD",
                    "Condition node must have a 'field'"));
        }
        if (nodeMap.get("operator") == null) {
            errors.add(new ValidationError(
                    path + ".operator", "MISSING_OPERATOR",
                    "Condition node must have an 'operator'"));
        }

        Object branchesObj = nodeMap.get("branches");
        if (!(branchesObj instanceof List<?> branchesList)) {
            errors.add(new ValidationError(
                    path + ".branches", "MISSING_BRANCHES",
                    "Condition node must have at least one branch"));
            return;
        }

        if (branchesList.isEmpty()) {
            errors.add(new ValidationError(
                    path + ".branches", "EMPTY_BRANCHES",
                    "Condition node must have at least one branch"));
            return;
        }

        for (int i = 0; i < branchesList.size(); i++) {
            Object branchObj = branchesList.get(i);
            if (!(branchObj instanceof Map<?, ?> branchMap)) {
                errors.add(new ValidationError(
                        path + ".branches[" + i + "]", "INVALID_BRANCH",
                        "Branch must be a Map"));
                continue;
            }

            Object childObj = branchMap.get("child");
            if (!(childObj instanceof Map<?, ?>)) {
                errors.add(new ValidationError(
                        path + ".branches[" + i + "].child", "MISSING_CHILD",
                        "Each branch must have a 'child' node"));
            } else {
                validateNode((Map<String, Object>) childObj,
                        path + ".branches[" + i + "].child", errors);
            }
        }

        // Validate optional default branch
        Object defaultObj = nodeMap.get("defaultBranch");
        if (defaultObj instanceof Map<?, ?>) {
            validateNode((Map<String, Object>) defaultObj, path + ".defaultBranch", errors);
        }
    }

    private void validateActionNode(Map<String, Object> nodeMap, String path,
                                    List<ValidationError> errors) {
        Object outputsObj = nodeMap.get("outputs");
        if (outputsObj == null) {
            errors.add(new ValidationError(
                    path + ".outputs", "MISSING_OUTPUTS",
                    "Action node must have an 'outputs' map"));
        } else if (!(outputsObj instanceof Map<?, ?>)) {
            errors.add(new ValidationError(
                    path + ".outputs", "INVALID_OUTPUTS",
                    "Action node 'outputs' must be a Map"));
        } else if (((Map<?, ?>) outputsObj).isEmpty()) {
            errors.add(new ValidationError(
                    path + ".outputs", "EMPTY_OUTPUTS",
                    "Action node must have at least one output"));
        }
    }

    private void validateSubRuleNode(Map<String, Object> nodeMap, String path,
                                     List<ValidationError> errors) {
        if (nodeMap.get("subRuleId") == null) {
            errors.add(new ValidationError(
                    path + ".subRuleId", "MISSING_SUB_RULE_ID",
                    "SubRule node must have a 'subRuleId'"));
        }
    }

    private void validateScoreNode(Map<String, Object> nodeMap, String path,
                                   List<ValidationError> errors) {
        Object scoreObj = nodeMap.get("score");
        if (scoreObj == null) {
            errors.add(new ValidationError(
                    path + ".score", "MISSING_SCORE",
                    "Score node must have a 'score' value"));
        } else if (!(scoreObj instanceof Number)) {
            try {
                Double.parseDouble(scoreObj.toString());
            } catch (NumberFormatException e) {
                errors.add(new ValidationError(
                        path + ".score", "INVALID_SCORE",
                        "Score node 'score' must be a numeric value"));
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void validateGateNode(Map<String, Object> nodeMap, String path,
                                  List<ValidationError> errors) {
        if (nodeMap.get("expression") == null) {
            errors.add(new ValidationError(
                    path + ".expression", "MISSING_EXPRESSION",
                    "Gate node must have an 'expression'"));
        }

        Object trueChild = nodeMap.get("trueChild");
        Object falseChild = nodeMap.get("falseChild");

        if (trueChild == null && falseChild == null) {
            errors.add(new ValidationError(
                    path, "MISSING_GATE_CHILDREN",
                    "Gate node must have at least one of 'trueChild' or 'falseChild'"));
        }

        if (trueChild instanceof Map<?, ?>) {
            validateNode((Map<String, Object>) trueChild, path + ".trueChild", errors);
        }
        if (falseChild instanceof Map<?, ?>) {
            validateNode((Map<String, Object>) falseChild, path + ".falseChild", errors);
        }
    }
}
