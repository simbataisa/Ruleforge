package io.ruleforge.core.engine;

import io.ruleforge.core.RuleEngineException;
import io.ruleforge.core.memory.WorkingMemory;
import io.ruleforge.core.resolution.ActivatedRule;
import io.ruleforge.core.result.DecisionTrace;
import io.ruleforge.core.result.MatchedRule;
import io.ruleforge.core.result.RuleResult;
import io.ruleforge.core.result.RuleResultMetadata;
import io.ruleforge.core.spi.ConflictResolutionStrategy;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Implements forward chaining: fire rules, modify facts, re-evaluate, repeat
 * until a stable state is reached or the maximum iteration count is exceeded.
 *
 * <p>Cycle detection is performed using a composite key of ruleId and the
 * working memory fact hash. If a rule is about to fire with the exact same
 * fact state it has already seen, it is skipped to prevent infinite loops.
 */
public class ForwardChainingController {

    public static final int DEFAULT_MAX_ITERATIONS = 100;

    private final int maxIterations;

    public ForwardChainingController() {
        this(DEFAULT_MAX_ITERATIONS);
    }

    public ForwardChainingController(int maxIterations) {
        if (maxIterations <= 0) {
            throw new IllegalArgumentException("maxIterations must be positive, got: " + maxIterations);
        }
        this.maxIterations = maxIterations;
    }

    /**
     * Execute forward chaining with the given activated rules and working memory.
     *
     * <p>Algorithm:
     * <ol>
     *   <li>Create an {@link Agenda} with the initial activations</li>
     *   <li>While the agenda has activations and iteration &lt; max:
     *     <ol type="a">
     *       <li>Resolve conflicts to determine firing order</li>
     *       <li>For each rule to fire, perform cycle detection (ruleId + factHash)</li>
     *       <li>Fire the rule by calling {@code compiledRule.evaluate(workingMemory)}</li>
     *       <li>Collect results and matched rules</li>
     *       <li>If working memory was modified, re-add all rules to the agenda for re-evaluation</li>
     *       <li>Clear the modification flag</li>
     *     </ol>
     *   </li>
     *   <li>If iteration &gt;= max, throw {@link RuleEngineException}</li>
     *   <li>Aggregate and return results</li>
     * </ol>
     *
     * @param initialActivations the initially matched rules
     * @param workingMemory      the working memory containing facts
     * @param strategy           the conflict resolution strategy
     * @param ctx                the execution context (iteration counter is updated)
     * @return aggregated {@link RuleResult} from all fired rules
     */
    public RuleResult execute(
            List<ActivatedRule> initialActivations,
            WorkingMemory workingMemory,
            ConflictResolutionStrategy strategy,
            DefaultExecutionContext ctx) {

        long startNanos = System.nanoTime();

        // Cycle detection: tracks (ruleId, factHash) combinations already fired
        Set<String> firedStates = new HashSet<>();

        // Aggregated outputs and metadata
        Map<String, Object> aggregatedOutput = new LinkedHashMap<>();
        List<MatchedRule> allMatchedRules = new ArrayList<>();
        List<DecisionTrace> traces = new ArrayList<>();
        int totalRulesEvaluated = 0;
        int totalRulesFired = 0;

        // Create agenda with initial activations
        Agenda agenda = new Agenda(strategy);
        agenda.addActivations(initialActivations);

        int iteration = 0;
        while (agenda.hasActivations() && iteration < maxIterations) {
            ctx.setCurrentIteration(iteration);

            // Resolve conflicts to determine firing order
            List<ActivatedRule> toFire = agenda.resolveConflicts(ctx);
            agenda.clear();

            boolean anyFired = false;

            for (ActivatedRule activated : toFire) {
                totalRulesEvaluated++;

                // Cycle detection: ruleId + factHash
                long factHash = workingMemory.factHash();
                String stateKey = activated.id() + ":" + factHash;

                if (firedStates.contains(stateKey)) {
                    // This rule has already fired with this exact fact state -- skip
                    continue;
                }

                // Check noLoop: if the rule has noLoop set and has been fired at all, skip
                if (activated.metadata() != null && activated.metadata().noLoop()
                        && firedStates.stream().anyMatch(s -> s.startsWith(activated.id() + ":"))) {
                    continue;
                }

                // Clear modification flag before firing so we can detect changes
                workingMemory.clearModificationFlag();

                // Fire the rule
                RuleResult result = activated.compiledRule().evaluate(workingMemory);
                firedStates.add(stateKey);
                totalRulesFired++;
                anyFired = true;

                // Collect results
                if (result.hasOutput()) {
                    aggregatedOutput.putAll(result.output());
                }
                allMatchedRules.addAll(result.matchedRules());

                // If the fired rule itself produced matched rule info, add it
                if (result.matchedRules().isEmpty()) {
                    allMatchedRules.add(new MatchedRule(
                            activated.id(),
                            activated.name(),
                            0, // version not tracked at activation level
                            activated.priority(),
                            activated.matchedConditions(),
                            result.output()));
                }

                if (result.trace() != null) {
                    traces.add(result.trace());
                }

                // If working memory was modified, we need to re-evaluate
                if (workingMemory.isModified()) {
                    // Re-add all initial activations for re-evaluation
                    agenda.addActivations(initialActivations);
                    break; // restart the inner loop from conflict resolution
                }
            }

            if (!anyFired) {
                // No rule fired in this iteration -- stable state reached
                break;
            }

            iteration++;
        }

        if (iteration >= maxIterations) {
            throw new RuleEngineException(
                    "Forward chaining exceeded maximum iterations (" + maxIterations
                            + "). Possible infinite loop detected. "
                            + "Total rules fired: " + totalRulesFired
                            + ", unique states visited: " + firedStates.size());
        }

        Duration elapsed = Duration.ofNanos(System.nanoTime() - startNanos);

        RuleResultMetadata metadata = new RuleResultMetadata(
                elapsed,
                totalRulesEvaluated,
                totalRulesFired,
                iteration,
                strategy.name());

        // Build a composite trace if there were any traces
        DecisionTrace compositeTrace = null;
        if (!traces.isEmpty()) {
            // Use the first trace as representative; in a full implementation
            // these would be combined into a tree structure
            compositeTrace = traces.getFirst();
        }

        return new RuleResult(
                aggregatedOutput.isEmpty() ? Map.of() : aggregatedOutput,
                metadata,
                allMatchedRules,
                compositeTrace);
    }
}
