package io.ruleforge.flow.validator;

import io.ruleforge.core.model.RuleDefinition;
import io.ruleforge.core.model.RuleType;
import io.ruleforge.core.model.ValidationResult;
import io.ruleforge.core.model.ValidationResult.ValidationError;

import java.util.*;

/**
 * Validates a flow rule definition for structural correctness.
 */
public final class FlowValidator {

    @SuppressWarnings("unchecked")
    public ValidationResult validate(RuleDefinition definition) {
        List<ValidationError> errors = new ArrayList<>();

        if (definition.type() != RuleType.RULE_FLOW) {
            errors.add(new ValidationError("type", "INVALID_TYPE",
                    "Expected RULE_FLOW but got " + definition.type()));
            return ValidationResult.failure(errors);
        }

        Map<String, Object> payload = definition.definition();
        if (payload == null) {
            errors.add(new ValidationError("definition", "NULL_DEFINITION",
                    "Flow definition payload must not be null"));
            return ValidationResult.failure(errors);
        }

        Object nodesObj = payload.get("nodes");
        if (!(nodesObj instanceof List<?> nodesList)) {
            errors.add(new ValidationError("definition.nodes", "MISSING_NODES",
                    "Flow definition must have a 'nodes' list"));
            return ValidationResult.failure(errors);
        }

        if (nodesList.isEmpty()) {
            errors.add(new ValidationError("definition.nodes", "EMPTY_NODES",
                    "Flow definition must have at least one node"));
        }

        String startNodeId = (String) payload.get("startNodeId");
        Set<String> nodeIds = new HashSet<>();
        for (Object nodeObj : nodesList) {
            if (nodeObj instanceof Map<?, ?> nodeMap) {
                String id = (String) ((Map<String, Object>) nodeMap).get("id");
                String type = (String) ((Map<String, Object>) nodeMap).get("type");
                if (id == null) {
                    errors.add(new ValidationError("definition.nodes", "MISSING_NODE_ID",
                            "Each node must have an 'id' field"));
                } else {
                    nodeIds.add(id);
                }
                if (type == null) {
                    errors.add(new ValidationError("definition.nodes", "MISSING_NODE_TYPE",
                            "Each node must have a 'type' field"));
                }
            }
        }

        if (startNodeId != null && !nodeIds.contains(startNodeId)) {
            errors.add(new ValidationError("definition.startNodeId", "INVALID_START_NODE",
                    "startNodeId '" + startNodeId + "' does not reference a known node"));
        }

        // Validate edges
        Object edgesObj = payload.get("edges");
        if (edgesObj instanceof List<?> edgesList) {
            for (Object edgeObj : edgesList) {
                if (edgeObj instanceof Map<?, ?> edgeMap) {
                    Map<String, Object> edge = (Map<String, Object>) edgeMap;
                    String from = (String) edge.get("from");
                    String to = (String) edge.get("to");
                    if (from == null) {
                        errors.add(new ValidationError("definition.edges", "MISSING_FROM",
                                "Each edge must have a 'from' field"));
                    } else if (!nodeIds.contains(from)) {
                        errors.add(new ValidationError("definition.edges", "INVALID_FROM",
                                "Edge 'from' node '" + from + "' does not exist"));
                    }
                    if (to == null) {
                        errors.add(new ValidationError("definition.edges", "MISSING_TO",
                                "Each edge must have a 'to' field"));
                    } else if (!nodeIds.contains(to)) {
                        errors.add(new ValidationError("definition.edges", "INVALID_TO",
                                "Edge 'to' node '" + to + "' does not exist"));
                    }
                }
            }
        }

        return errors.isEmpty() ? ValidationResult.success() : ValidationResult.failure(errors);
    }
}
