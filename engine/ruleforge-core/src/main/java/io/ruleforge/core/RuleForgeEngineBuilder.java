package io.ruleforge.core;

import io.ruleforge.core.engine.DefaultRuleForgeEngine;
import io.ruleforge.core.engine.RuleEngineRegistry;
import io.ruleforge.core.spi.CompilationPhaseExtension;
import io.ruleforge.core.spi.ConflictResolutionStrategy;
import io.ruleforge.core.spi.DataEnricherExtension;
import io.ruleforge.core.spi.FunctionExtension;
import io.ruleforge.core.spi.OperatorExtension;
import io.ruleforge.core.spi.RuleLifecycleListener;
import io.ruleforge.core.spi.RuleTypeEngine;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Builder for constructing {@link RuleForgeEngine} instances with custom configuration.
 *
 * <p>Usage:
 * <pre>{@code
 * RuleForgeEngine engine = RuleForgeEngine.builder()
 *     .registerEngine(new DecisionTableEngine())
 *     .registerEngine(new DecisionTreeEngine())
 *     .registerOperator(new PhoneticMatchOperator())
 *     .defaultStrategy(CompositeConflictResolver.defaultChain())
 *     .maxIterations(200)
 *     .defaultTimeout(Duration.ofSeconds(10))
 *     .build();
 * }</pre>
 */
public class RuleForgeEngineBuilder {

    private final RuleEngineRegistry registry = new RuleEngineRegistry();
    private ConflictResolutionStrategy defaultStrategy;
    private final List<OperatorExtension> operators = new ArrayList<>();
    private final List<FunctionExtension> functions = new ArrayList<>();
    private final List<DataEnricherExtension> enrichers = new ArrayList<>();
    private final List<RuleLifecycleListener> listeners = new ArrayList<>();
    private final List<CompilationPhaseExtension> compilationPhases = new ArrayList<>();
    private int maxForwardChainIterations = 100;
    private Duration defaultTimeout = Duration.ofSeconds(5);

    /**
     * Registers a type-specific rule engine (e.g., decision table, decision tree, scorecard).
     *
     * @param engine the rule type engine to register
     * @return this builder for fluent chaining
     */
    public RuleForgeEngineBuilder registerEngine(RuleTypeEngine engine) {
        Objects.requireNonNull(engine, "engine must not be null");
        registry.register(engine);
        return this;
    }

    /**
     * Registers a custom operator extension (e.g., PHONETIC_MATCH, GEO_WITHIN).
     *
     * @param op the operator extension to register
     * @return this builder for fluent chaining
     */
    public RuleForgeEngineBuilder registerOperator(OperatorExtension op) {
        Objects.requireNonNull(op, "operator must not be null");
        operators.add(op);
        return this;
    }

    /**
     * Registers a custom function extension (e.g., CREDIT_SCORE_BAND, GEO_DISTANCE).
     *
     * @param fn the function extension to register
     * @return this builder for fluent chaining
     */
    public RuleForgeEngineBuilder registerFunction(FunctionExtension fn) {
        Objects.requireNonNull(fn, "function must not be null");
        functions.add(fn);
        return this;
    }

    /**
     * Registers a data enricher extension (e.g., JDBC, HTTP, Redis).
     *
     * @param enricher the data enricher to register
     * @return this builder for fluent chaining
     */
    public RuleForgeEngineBuilder registerEnricher(DataEnricherExtension enricher) {
        Objects.requireNonNull(enricher, "enricher must not be null");
        enrichers.add(enricher);
        return this;
    }

    /**
     * Adds a rule lifecycle listener for metrics, auditing, logging, and monitoring.
     *
     * @param listener the lifecycle listener to add
     * @return this builder for fluent chaining
     */
    public RuleForgeEngineBuilder addListener(RuleLifecycleListener listener) {
        Objects.requireNonNull(listener, "listener must not be null");
        listeners.add(listener);
        return this;
    }

    /**
     * Adds a compilation phase extension for custom compilation passes
     * (e.g., regulatory compliance checking, optimization).
     *
     * @param phase the compilation phase extension to add
     * @return this builder for fluent chaining
     */
    public RuleForgeEngineBuilder addCompilationPhase(CompilationPhaseExtension phase) {
        Objects.requireNonNull(phase, "phase must not be null");
        compilationPhases.add(phase);
        return this;
    }

    /**
     * Sets the default conflict resolution strategy used when no strategy is
     * specified in evaluation options.
     *
     * @param strategy the default strategy
     * @return this builder for fluent chaining
     */
    public RuleForgeEngineBuilder defaultStrategy(ConflictResolutionStrategy strategy) {
        this.defaultStrategy = strategy;
        return this;
    }

    /**
     * Sets the maximum number of forward-chaining iterations before the engine
     * throws a {@link RuleEngineException} to prevent infinite loops.
     *
     * @param max the maximum iteration count (must be positive)
     * @return this builder for fluent chaining
     */
    public RuleForgeEngineBuilder maxIterations(int max) {
        if (max <= 0) {
            throw new IllegalArgumentException("maxIterations must be positive, got: " + max);
        }
        this.maxForwardChainIterations = max;
        return this;
    }

    /**
     * Sets the default evaluation timeout.
     *
     * @param timeout the default timeout duration
     * @return this builder for fluent chaining
     */
    public RuleForgeEngineBuilder defaultTimeout(Duration timeout) {
        Objects.requireNonNull(timeout, "timeout must not be null");
        this.defaultTimeout = timeout;
        return this;
    }

    /**
     * Builds and returns a configured {@link RuleForgeEngine} instance.
     *
     * @return a new engine instance
     */
    public RuleForgeEngine build() {
        return new DefaultRuleForgeEngine(this);
    }

    // ---------------------------------------------------------------
    // Package-private getters for DefaultRuleForgeEngine to read
    // ---------------------------------------------------------------

    public RuleEngineRegistry getRegistry() {
        return registry;
    }

    public ConflictResolutionStrategy getDefaultStrategy() {
        return defaultStrategy;
    }

    public List<OperatorExtension> getOperators() {
        return operators;
    }

    public List<FunctionExtension> getFunctions() {
        return functions;
    }

    public List<DataEnricherExtension> getEnrichers() {
        return enrichers;
    }

    public List<RuleLifecycleListener> getListeners() {
        return listeners;
    }

    public List<CompilationPhaseExtension> getCompilationPhases() {
        return compilationPhases;
    }

    public int getMaxForwardChainIterations() {
        return maxForwardChainIterations;
    }

    public Duration getDefaultTimeout() {
        return defaultTimeout;
    }
}
