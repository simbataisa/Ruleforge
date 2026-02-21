package io.ruleforge.ensemble.compiler;

import io.ruleforge.core.model.RuleDefinition;
import io.ruleforge.ensemble.model.AggregationStrategy;
import io.ruleforge.ensemble.model.EnsembleDefinition;
import io.ruleforge.ensemble.model.MemberRule;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Compiles a {@link RuleDefinition} whose payload encodes an ensemble rule
 * into a {@link CompiledEnsemble}.
 *
 * <p>Expected definition map structure:
 * <pre>{@code
 * {
 *   "strategy":    "MAJORITY_VOTE",
 *   "outputField": "result",
 *   "members": [
 *     { "ruleId": "uuid-1", "alias": "credit-check", "weight": 0.6, "version": 0 },
 *     { "ruleId": "uuid-2", "alias": "income-check", "weight": 0.4, "version": 0 }
 *   ]
 * }
 * }</pre>
 */
public final class EnsembleCompiler {

    @SuppressWarnings("unchecked")
    public CompiledEnsemble compile(RuleDefinition definition) {
        Map<String, Object> payload = definition.definition();
        if (payload == null) {
            throw new IllegalArgumentException(
                    "Ensemble definition payload is null for rule: " + definition.id());
        }

        String strategyStr = (String) payload.getOrDefault("strategy", "MAJORITY_VOTE");
        AggregationStrategy strategy;
        try {
            strategy = AggregationStrategy.valueOf(strategyStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Unknown aggregation strategy '" + strategyStr + "' for rule: " + definition.id());
        }

        String outputField = (String) payload.getOrDefault("outputField", "result");

        List<Map<String, Object>> memberDefs =
                (List<Map<String, Object>>) payload.getOrDefault("members", List.of());
        List<MemberRule> members = parseMembers(memberDefs);

        EnsembleDefinition ensembleDefinition = new EnsembleDefinition(
                definition.id(),
                definition.name(),
                members,
                strategy,
                outputField
        );

        return new CompiledEnsemble(
                definition.id(),
                definition.metadata(),
                System.currentTimeMillis(),
                ensembleDefinition
        );
    }

    @SuppressWarnings("unchecked")
    private List<MemberRule> parseMembers(List<Map<String, Object>> memberDefs) {
        List<MemberRule> members = new ArrayList<>();
        for (Map<String, Object> memberDef : memberDefs) {
            String ruleId = (String) memberDef.get("ruleId");
            if (ruleId == null || ruleId.isBlank()) {
                continue; // skip malformed entries
            }
            String alias = (String) memberDef.getOrDefault("alias", ruleId);
            double weight = ((Number) memberDef.getOrDefault("weight", 1.0)).doubleValue();
            int version = ((Number) memberDef.getOrDefault("version", 0)).intValue();
            members.add(new MemberRule(ruleId, alias, weight, version));
        }
        return members;
    }
}
