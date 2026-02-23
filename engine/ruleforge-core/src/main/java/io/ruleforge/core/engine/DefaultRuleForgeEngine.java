package io.ruleforge.core.engine;

import io.ruleforge.core.RuleCompilationException;
import io.ruleforge.core.RuleEngineException;
import io.ruleforge.core.RuleEvaluationException;
import io.ruleforge.core.RuleForgeEngine;
import io.ruleforge.core.RuleForgeEngineBuilder;
import io.ruleforge.core.RuleValidationException;
import io.ruleforge.core.memory.WorkingMemory;
import io.ruleforge.core.model.CompiledRule;
import io.ruleforge.core.model.EvaluationOptions;
import io.ruleforge.core.model.ExecutionMode;
import io.ruleforge.core.model.RuleDefinition;
import io.ruleforge.core.model.ValidationResult;
import io.ruleforge.core.resolution.CompositeConflictResolver;
import io.ruleforge.core.result.MatchedRule;
import io.ruleforge.core.result.RuleResult;
import io.ruleforge.core.result.RuleResultMetadata;
import io.ruleforge.core.spi.CompilationPhaseExtension;
import io.ruleforge.core.spi.ConflictResolutionStrategy;
import io.ruleforge.core.spi.EvaluationContext;
import io.ruleforge.core.spi.RuleLifecycleListener;
import io.ruleforge.core.spi.RuleTypeEngine;

import java.time.Duration;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Default implementation of {@link RuleForgeEngine}.
 *
 * <p>Orchestrates rule compilation, validation, and evaluation by delegating
 * to type-specific {@link RuleTypeEngine} implementations via the
 * {@link RuleEngineRegistry}, applying lifecycle listeners, and running
 * compilation phase extensions.
 */
public class DefaultRuleForgeEngine implements RuleForgeEngine {

    private final RuleEngineRegistry registry;
    private final ConflictResolutionStrategy defaultStrategy;
    private final List<RuleLifecycleListener> listeners;
    private final List<CompilationPhaseExtension> compilationPhases;
    private final int maxForwardChainIterations;
    private final Duration defaultTimeout;

    /**
     * Constructs the engine from builder configuration.
     * Package-private: instances are created via {@link RuleForgeEngineBuilder#build()}.
     */
    public DefaultRuleForgeEngine(RuleForgeEngineBuilder builder) {
        this.registry = builder.getRegistry();
        this.defaultStrategy = builder.getDefaultStrategy() != null
                ? builder.getDefaultStrategy()
                : CompositeConflictResolver.defaultChain();
        this.listeners = List.copyOf(builder.getListeners());
        this.compilationPhases = builder.getCompilationPhases().stream()
                .sorted(Comparator.comparingInt(CompilationPhaseExtension::order))
                .toList();
        this.maxForwardChainIterations = builder.getMaxForwardChainIterations();
        this.defaultTimeout = builder.getDefaultTimeout();
    }

    @Override
    public RuleResult evaluate(String ruleId, int version, Map<String, Object> input) {
        return evaluate(ruleId, version, input, EvaluationOptions.defaults());
    }

    @Override
    public RuleResult evaluate(String ruleId, int version, Map<String, Object> input, EvaluationOptions options) {
        // Rule lookup by ID requires a RuleStore implementation which is provided
        // by the platform solver service. This engine layer operates on CompiledRule
        // and WorkingMemory directly.
        throw new UnsupportedOperationException(
                "Rule lookup by ID requires a RuleStore implementation (Phase 1 - Solver Service). "
                        + "Use evaluate(CompiledRule, WorkingMemory) for direct evaluation.");
    }

    @Override
    public RuleResult evaluate(CompiledRule rule, WorkingMemory workingMemory) {
        long startNanos = System.nanoTime();
        String ruleId = rule.ruleId();

        // Notify listeners before evaluation
        for (RuleLifecycleListener listener : listeners) {
            try {
                listener.beforeEvaluation(null, workingMemory);
            } catch (Exception e) {
                // Listener errors should not abort evaluation
            }
        }

        try {
            // Dispatch evaluation through the RuleTypeEngine if available,
            // falling back to direct CompiledRule.evaluate() for engines that support it.
            RuleResult result;
            RuleTypeEngine typeEngine = registry.findEngine(rule.type());
            if (typeEngine != null) {
                EvaluationContext ctx = new SimpleEvaluationContext(workingMemory);
                result = typeEngine.evaluate(rule, workingMemory, ctx);
            } else {
                result = rule.evaluate(workingMemory);
            }

            Duration elapsed = Duration.ofNanos(System.nanoTime() - startNanos);

            // Enrich metadata with execution timing if the result has default metadata
            RuleResultMetadata enrichedMetadata = new RuleResultMetadata(
                    elapsed,
                    1,
                    result.hasOutput() ? 1 : 0,
                    0,
                    defaultStrategy.name());

            RuleResult enrichedResult = new RuleResult(
                    result.output(),
                    enrichedMetadata,
                    result.matchedRules(),
                    result.trace());

            // Notify listeners after evaluation
            for (RuleLifecycleListener listener : listeners) {
                try {
                    listener.afterEvaluation(null, enrichedResult, elapsed);
                } catch (Exception e) {
                    // Listener errors should not affect the result
                }
            }

            return enrichedResult;

        } catch (RuleEngineException e) {
            // Notify listeners of error
            for (RuleLifecycleListener listener : listeners) {
                try {
                    listener.onError(null, e);
                } catch (Exception listenerEx) {
                    // Suppress listener errors
                }
            }
            throw e;
        } catch (Exception e) {
            RuleEvaluationException evalEx = new RuleEvaluationException(ruleId, e.getMessage(), e);
            // Notify listeners of error
            for (RuleLifecycleListener listener : listeners) {
                try {
                    listener.onError(null, evalEx);
                } catch (Exception listenerEx) {
                    // Suppress listener errors
                }
            }
            throw evalEx;
        }
    }

    @Override
    public List<RuleResult> evaluateBulk(String ruleId, int version, List<Map<String, Object>> inputs) {
        // Bulk evaluation by ID requires a RuleStore implementation to look up
        // and cache the compiled rule. In a full implementation this would use
        // virtual threads for parallel evaluation.
        throw new UnsupportedOperationException(
                "Bulk evaluation by ID requires a RuleStore implementation (Phase 1 - Solver Service).");
    }

    @Override
    public CompiledRule compile(RuleDefinition definition) {
        if (definition == null) {
            throw new IllegalArgumentException("RuleDefinition must not be null");
        }

        // Find the appropriate RuleTypeEngine from the registry
        if (!registry.hasEngine(definition.type())) {
            throw new RuleCompilationException(
                    definition.id(),
                    "No RuleTypeEngine registered for type: " + definition.type());
        }

        RuleTypeEngine typeEngine = registry.getEngine(definition.type());

        // Validate the definition first
        ValidationResult validation = typeEngine.validate(definition);
        if (!validation.valid()) {
            throw new RuleValidationException(validation);
        }

        // Compile via the type engine
        CompiledRule compiled = typeEngine.compile(definition);

        // Apply compilation phase extensions in order
        if (!compilationPhases.isEmpty()) {
            DefaultCompilationContext compilationCtx = new DefaultCompilationContext(definition);

            for (CompilationPhaseExtension phase : compilationPhases) {
                compiled = phase.process(compiled, compilationCtx);

                // If a phase produced errors, abort compilation
                if (compilationCtx.hasErrors()) {
                    ValidationResult phaseResult = compilationCtx.toValidationResult();
                    throw new RuleCompilationException(
                            definition.id(),
                            "Compilation phase failed with " + phaseResult.errors().size()
                                    + " error(s): " + phaseResult.errors().getFirst().message());
                }
            }
        }

        return compiled;
    }

    @Override
    public ValidationResult validate(RuleDefinition definition) {
        if (definition == null) {
            throw new IllegalArgumentException("RuleDefinition must not be null");
        }

        // Find the appropriate RuleTypeEngine from the registry
        if (!registry.hasEngine(definition.type())) {
            return ValidationResult.failure(List.of(
                    new ValidationResult.ValidationError(
                            definition.id(),
                            "UNSUPPORTED_TYPE",
                            "No RuleTypeEngine registered for type: " + definition.type())));
        }

        RuleTypeEngine typeEngine = registry.getEngine(definition.type());
        return typeEngine.validate(definition);
    }

    /**
     * Returns the registry for testing/introspection purposes.
     */
    public RuleEngineRegistry getRegistry() {
        return registry;
    }

    /**
     * Returns the default conflict resolution strategy.
     */
    public ConflictResolutionStrategy getDefaultStrategy() {
        return defaultStrategy;
    }

    /**
     * Returns the maximum forward chaining iterations.
     */
    public int getMaxForwardChainIterations() {
        return maxForwardChainIterations;
    }

    /**
     * Returns the default evaluation timeout.
     */
    public Duration getDefaultTimeout() {
        return defaultTimeout;
    }
}
