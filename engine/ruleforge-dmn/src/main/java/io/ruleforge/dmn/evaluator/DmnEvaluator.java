package io.ruleforge.dmn.evaluator;

import io.ruleforge.core.RuleEvaluationException;
import io.ruleforge.core.memory.WorkingMemory;
import io.ruleforge.core.model.PriorityVector;
import io.ruleforge.core.result.DecisionTrace;
import io.ruleforge.core.result.MatchedRule;
import io.ruleforge.core.result.RuleResult;
import io.ruleforge.core.result.RuleResultMetadata;
import io.ruleforge.dmn.compiler.CompiledDmn;
import io.ruleforge.dmn.feel.FeelEvaluator;
import io.ruleforge.dmn.feel.FeelLexer;
import io.ruleforge.dmn.feel.FeelNode;
import io.ruleforge.dmn.feel.FeelParser;
import io.ruleforge.dmn.model.DmnDecision;
import io.ruleforge.dmn.model.DmnDecisionTable;
import io.ruleforge.dmn.model.DmnDecisionTable.DmnRule;
import io.ruleforge.dmn.model.DmnDecisionTable.InputClause;
import io.ruleforge.dmn.model.DmnDecisionTable.OutputClause;
import io.ruleforge.dmn.model.DmnDefinition;
import io.ruleforge.dmn.model.HitPolicy;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Evaluates a compiled DMN model against working memory.
 * <p>
 * Processes decisions in dependency order (topological sort), evaluates
 * decision tables using S-FEEL unary tests, and applies hit policies.
 */
public final class DmnEvaluator {

    public RuleResult evaluate(CompiledDmn compiledDmn, WorkingMemory workingMemory) {
        long startNanos = System.nanoTime();
        DecisionTrace.Builder traceBuilder = DecisionTrace.builder(compiledDmn.ruleId());

        DmnDefinition def = compiledDmn.definition();
        Map<String, Object> context = new HashMap<>(workingMemory.toMap());
        Map<String, Object> allOutputs = new LinkedHashMap<>();
        List<MatchedRule> matchedRules = new ArrayList<>();
        int rulesEvaluated = 0;

        // Evaluate decisions in dependency order
        List<DmnDecision> ordered = topologicalSort(def.decisions());

        for (DmnDecision decision : ordered) {
            Map<String, Object> decisionOutput = evaluateDecision(
                    compiledDmn.ruleId(), decision, context, matchedRules);
            rulesEvaluated += decision.decisionTable().rules().size();

            // Make decision output available to downstream decisions
            context.putAll(decisionOutput);
            allOutputs.putAll(decisionOutput);
        }

        Duration elapsed = Duration.ofNanos(System.nanoTime() - startNanos);
        traceBuilder.addFactChanges(workingMemory.getChangelog());
        DecisionTrace trace = traceBuilder.build();
        RuleResultMetadata metadata = new RuleResultMetadata(
                elapsed, rulesEvaluated, matchedRules.size(), 0, "DMN"
        );

        return new RuleResult(allOutputs, metadata, matchedRules, trace);
    }

    private Map<String, Object> evaluateDecision(String ruleId, DmnDecision decision,
                                                   Map<String, Object> context,
                                                   List<MatchedRule> matchedRules) {
        DmnDecisionTable table = decision.decisionTable();
        List<InputClause> inputs = table.inputClauses();
        List<OutputClause> outputs = table.outputClauses();

        // Resolve input values from context
        FeelEvaluator feelEvaluator = new FeelEvaluator(context);
        List<Object> inputValues = new ArrayList<>();
        for (InputClause input : inputs) {
            String expr = input.expression();
            Object val = context.get(expr);
            if (val == null && !expr.equals(input.name())) {
                // Try evaluating as FEEL expression
                try {
                    FeelLexer lexer = new FeelLexer(expr);
                    FeelParser parser = new FeelParser(lexer.tokenize());
                    FeelNode node = parser.parse();
                    val = feelEvaluator.evaluate(node);
                } catch (Exception e) {
                    val = null;
                }
            }
            inputValues.add(val);
        }

        // Evaluate rules (rows)
        List<DmnRule> matchedDmnRules = new ArrayList<>();
        for (DmnRule rule : table.rules()) {
            if (matchesRule(rule, inputValues, context)) {
                matchedDmnRules.add(rule);
            }
        }

        // Apply hit policy
        Map<String, Object> output = applyHitPolicy(
                ruleId, decision, table.hitPolicy(), matchedDmnRules, outputs, context, matchedRules);

        return output;
    }

    private boolean matchesRule(DmnRule rule, List<Object> inputValues, Map<String, Object> context) {
        List<String> entries = rule.inputEntries();
        for (int i = 0; i < entries.size() && i < inputValues.size(); i++) {
            String entry = entries.get(i).trim();
            if (entry.isEmpty() || "-".equals(entry)) {
                continue; // wildcard
            }
            Object inputVal = inputValues.get(i);
            if (!evaluateUnaryTest(entry, inputVal, context)) {
                return false;
            }
        }
        return true;
    }

    private boolean evaluateUnaryTest(String expression, Object inputValue, Map<String, Object> context) {
        try {
            FeelLexer lexer = new FeelLexer(expression);
            FeelParser parser = new FeelParser(lexer.tokenize());
            FeelNode testNode = parser.parseUnaryTest();
            FeelEvaluator evaluator = new FeelEvaluator(context);
            return evaluator.evaluateUnaryTest(testNode, inputValue);
        } catch (Exception e) {
            return false;
        }
    }

    private Object evaluateOutputEntry(String expression, Map<String, Object> context) {
        if (expression == null || expression.trim().isEmpty()) {
            return null;
        }
        try {
            FeelLexer lexer = new FeelLexer(expression.trim());
            FeelParser parser = new FeelParser(lexer.tokenize());
            FeelNode node = parser.parse();
            FeelEvaluator evaluator = new FeelEvaluator(context);
            return evaluator.evaluate(node);
        } catch (Exception e) {
            // Return raw string if parsing fails
            return expression.trim();
        }
    }

    private Map<String, Object> applyHitPolicy(String ruleId, DmnDecision decision,
                                                 HitPolicy hitPolicy,
                                                 List<DmnRule> matchedDmnRules,
                                                 List<OutputClause> outputClauses,
                                                 Map<String, Object> context,
                                                 List<MatchedRule> matchedRules) {
        Map<String, Object> output = new LinkedHashMap<>();

        if (matchedDmnRules.isEmpty()) {
            // Apply default values from output clauses
            for (OutputClause oc : outputClauses) {
                if (oc.defaultValue() != null) {
                    output.put(oc.name(), evaluateOutputEntry(oc.defaultValue(), context));
                }
            }
            return output;
        }

        switch (hitPolicy) {
            case UNIQUE -> {
                if (matchedDmnRules.size() > 1) {
                    throw new RuleEvaluationException(ruleId,
                            "UNIQUE hit policy violated: " + matchedDmnRules.size()
                                    + " rules matched in decision '" + decision.name() + "'");
                }
                DmnRule rule = matchedDmnRules.getFirst();
                output.putAll(extractOutputs(rule, outputClauses, context));
                matchedRules.add(toMatchedRule(ruleId, decision.name(), rule, output));
            }
            case FIRST -> {
                DmnRule rule = matchedDmnRules.getFirst();
                output.putAll(extractOutputs(rule, outputClauses, context));
                matchedRules.add(toMatchedRule(ruleId, decision.name(), rule, output));
            }
            case ANY -> {
                // All matched rules must produce the same output
                Map<String, Object> firstOutput = extractOutputs(matchedDmnRules.getFirst(), outputClauses, context);
                output.putAll(firstOutput);
                matchedRules.add(toMatchedRule(ruleId, decision.name(), matchedDmnRules.getFirst(), output));
            }
            case PRIORITY -> {
                // Use rule order as priority (lower rule number = higher priority)
                DmnRule best = matchedDmnRules.stream()
                        .min(Comparator.comparingInt(DmnRule::ruleNumber))
                        .orElseThrow();
                output.putAll(extractOutputs(best, outputClauses, context));
                matchedRules.add(toMatchedRule(ruleId, decision.name(), best, output));
            }
            case RULE_ORDER -> {
                List<Map<String, Object>> collected = new ArrayList<>();
                for (DmnRule rule : matchedDmnRules) {
                    Map<String, Object> ruleOutput = extractOutputs(rule, outputClauses, context);
                    collected.add(ruleOutput);
                    matchedRules.add(toMatchedRule(ruleId, decision.name(), rule, ruleOutput));
                }
                mergeCollectedOutputs(output, collected, outputClauses);
            }
            case OUTPUT_ORDER -> {
                List<DmnRule> sorted = new ArrayList<>(matchedDmnRules);
                sorted.sort(Comparator.comparingInt(DmnRule::ruleNumber));
                List<Map<String, Object>> collected = new ArrayList<>();
                for (DmnRule rule : sorted) {
                    Map<String, Object> ruleOutput = extractOutputs(rule, outputClauses, context);
                    collected.add(ruleOutput);
                    matchedRules.add(toMatchedRule(ruleId, decision.name(), rule, ruleOutput));
                }
                mergeCollectedOutputs(output, collected, outputClauses);
            }
            case COLLECT -> {
                List<Map<String, Object>> collected = new ArrayList<>();
                for (DmnRule rule : matchedDmnRules) {
                    Map<String, Object> ruleOutput = extractOutputs(rule, outputClauses, context);
                    collected.add(ruleOutput);
                    matchedRules.add(toMatchedRule(ruleId, decision.name(), rule, ruleOutput));
                }
                mergeCollectedOutputs(output, collected, outputClauses);
            }
        }

        return output;
    }

    private Map<String, Object> extractOutputs(DmnRule rule, List<OutputClause> outputClauses,
                                                 Map<String, Object> context) {
        Map<String, Object> result = new LinkedHashMap<>();
        List<String> entries = rule.outputEntries();
        for (int i = 0; i < outputClauses.size(); i++) {
            OutputClause oc = outputClauses.get(i);
            String entry = i < entries.size() ? entries.get(i) : null;
            Object value = evaluateOutputEntry(entry, context);
            if (value == null && oc.defaultValue() != null) {
                value = evaluateOutputEntry(oc.defaultValue(), context);
            }
            if (value != null) {
                result.put(oc.name(), value);
            }
        }
        return result;
    }

    private void mergeCollectedOutputs(Map<String, Object> output,
                                        List<Map<String, Object>> collected,
                                        List<OutputClause> outputClauses) {
        for (OutputClause oc : outputClauses) {
            List<Object> values = new ArrayList<>();
            for (Map<String, Object> row : collected) {
                Object val = row.get(oc.name());
                if (val != null) values.add(val);
            }
            if (!values.isEmpty()) {
                output.put(oc.name(), values.size() == 1 ? values.getFirst() : values);
            }
        }
    }

    private MatchedRule toMatchedRule(String ruleId, String decisionName, DmnRule rule,
                                      Map<String, Object> output) {
        PriorityVector priority = new PriorityVector(
                0, 0, 1.0, Instant.now(), rule.ruleNumber()
        );
        Map<String, Object> conditions = new LinkedHashMap<>();
        for (int i = 0; i < rule.inputEntries().size(); i++) {
            conditions.put("input_" + i, rule.inputEntries().get(i));
        }
        return new MatchedRule(ruleId, decisionName, 0, priority, conditions, output);
    }

    // ---------------------------------------------------------------
    // Topological sort for decision dependencies
    // ---------------------------------------------------------------

    private List<DmnDecision> topologicalSort(List<DmnDecision> decisions) {
        Map<String, DmnDecision> byId = new LinkedHashMap<>();
        for (DmnDecision d : decisions) byId.put(d.id(), d);

        List<DmnDecision> sorted = new ArrayList<>();
        Map<String, Integer> state = new HashMap<>(); // 0=unvisited, 1=in-progress, 2=done

        for (DmnDecision d : decisions) {
            if (state.getOrDefault(d.id(), 0) == 0) {
                visit(d, byId, state, sorted);
            }
        }
        return sorted;
    }

    private void visit(DmnDecision d, Map<String, DmnDecision> byId,
                       Map<String, Integer> state, List<DmnDecision> sorted) {
        state.put(d.id(), 1);
        for (String dep : d.requiredDecisions()) {
            int depState = state.getOrDefault(dep, 0);
            if (depState == 0) {
                DmnDecision depDecision = byId.get(dep);
                if (depDecision != null) {
                    visit(depDecision, byId, state, sorted);
                }
            }
        }
        state.put(d.id(), 2);
        sorted.add(d);
    }
}
