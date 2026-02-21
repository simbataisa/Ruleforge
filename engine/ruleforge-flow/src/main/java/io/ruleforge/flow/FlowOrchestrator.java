package io.ruleforge.flow;

import io.ruleforge.core.RuleEvaluationException;
import io.ruleforge.core.memory.WorkingMemory;
import io.ruleforge.core.result.RuleResult;
import io.ruleforge.core.result.RuleResultMetadata;
import io.ruleforge.core.spi.SubRuleInvoker;
import io.ruleforge.flow.executor.FlowExecutor;
import io.ruleforge.flow.graph.FlowGraph;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * DAG-based flow orchestrator for rule workflows.
 * Supports timeout enforcement and execution statistics.
 *
 * <p>Delegates actual graph traversal to {@link FlowExecutor} while adding
 * orchestration concerns: timeout enforcement, execution statistics collection,
 * and sub-rule invoker wiring.
 */
public class FlowOrchestrator {

    private final SubRuleInvoker subRuleInvoker;
    private final Duration defaultTimeout;

    public FlowOrchestrator() {
        this(null, Duration.ofSeconds(30));
    }

    public FlowOrchestrator(SubRuleInvoker subRuleInvoker, Duration defaultTimeout) {
        this.subRuleInvoker = subRuleInvoker;
        this.defaultTimeout = defaultTimeout != null ? defaultTimeout : Duration.ofSeconds(30);
    }

    /**
     * Orchestrates flow execution with default timeout.
     *
     * @param ruleId        the rule identifier
     * @param graph         the compiled flow graph
     * @param workingMemory the working memory for evaluation
     * @return the execution result with statistics
     */
    public FlowExecutionResult orchestrate(String ruleId, FlowGraph graph, WorkingMemory workingMemory) {
        return orchestrate(ruleId, graph, workingMemory, defaultTimeout);
    }

    /**
     * Orchestrates flow execution with timeout enforcement and statistics.
     *
     * @param ruleId        the rule identifier
     * @param graph         the compiled flow graph
     * @param workingMemory the working memory for evaluation
     * @param timeout       maximum execution duration
     * @return the execution result with statistics
     */
    public FlowExecutionResult orchestrate(String ruleId, FlowGraph graph,
                                            WorkingMemory workingMemory, Duration timeout) {
        long startNanos = System.nanoTime();
        FlowExecutor executor = new FlowExecutor(ruleId, subRuleInvoker);

        Duration effectiveTimeout = timeout != null ? timeout : defaultTimeout;

        ExecutorService threadPool = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "flow-orchestrator-" + ruleId);
            t.setDaemon(true);
            return t;
        });

        try {
            Future<RuleResult> future = threadPool.submit(() -> executor.execute(graph, workingMemory));

            RuleResult result;
            try {
                result = future.get(effectiveTimeout.toMillis(), TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                future.cancel(true);
                throw new RuleEvaluationException(ruleId,
                        "Flow execution timed out after " + effectiveTimeout.toMillis() + "ms");
            } catch (ExecutionException e) {
                Throwable cause = e.getCause();
                if (cause instanceof RuleEvaluationException ree) {
                    throw ree;
                }
                throw new RuleEvaluationException(ruleId, "Flow execution failed: " + cause.getMessage());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuleEvaluationException(ruleId, "Flow execution interrupted");
            } catch (CancellationException e) {
                throw new RuleEvaluationException(ruleId, "Flow execution cancelled");
            }

            Duration elapsed = Duration.ofNanos(System.nanoTime() - startNanos);

            Map<String, Object> stats = new LinkedHashMap<>();
            stats.put("executionTimeMs", elapsed.toMillis());
            stats.put("nodeCount", graph.getNodes().size());
            stats.put("edgeCount", graph.getEdges().size());
            stats.put("timedOut", false);
            if (result.metadata() != null) {
                stats.put("nodesVisited", result.metadata().rulesEvaluated());
            }

            return new FlowExecutionResult(result, stats, elapsed);
        } finally {
            threadPool.shutdownNow();
        }
    }

    /**
     * Result of an orchestrated flow execution including statistics.
     */
    public record FlowExecutionResult(
            RuleResult result,
            Map<String, Object> statistics,
            Duration totalExecutionTime
    ) {}
}
