package io.ruleforge.core.action;

import io.ruleforge.core.result.RuleResult;
import io.ruleforge.core.spi.ActionExecutor;
import io.ruleforge.core.spi.EvaluationContext;
import io.ruleforge.core.spi.SubRuleInvoker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Action executor that invokes a sub-rule by alias/ID.
 * Delegates to the {@link SubRuleInvoker} from the evaluation context.
 *
 * <p>Target: rule alias or ID (e.g., "credit-scoring").
 * <p>Parameters: input facts to merge into working memory before invocation.
 */
public final class RuleActionExecutor implements ActionExecutor {

    private static final Logger log = LoggerFactory.getLogger(RuleActionExecutor.class);

    @Override
    public String protocol() {
        return "rule";
    }

    @Override
    public Map<String, Object> execute(String target, Map<String, Object> params, EvaluationContext ctx) {
        SubRuleInvoker invoker = ctx.getSubRuleInvoker();
        if (invoker == null) {
            log.warn("No SubRuleInvoker available; cannot invoke rule '{}'", target);
            return Map.of("_error", "SubRuleInvoker not available");
        }

        // Merge input parameters into working memory before invocation
        for (var entry : params.entrySet()) {
            ctx.getWorkingMemory().assertFact(
                    entry.getKey(), entry.getValue(),
                    io.ruleforge.core.model.FactType.ANY);
        }

        RuleResult result = invoker.invoke(target, ctx.getWorkingMemory());
        if (result != null && result.hasOutput()) {
            return result.output();
        }
        return Map.of();
    }
}
