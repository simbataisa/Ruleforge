package io.ruleforge.core.action;

import io.ruleforge.core.spi.ActionExecutor;
import io.ruleforge.core.spi.EvaluationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Action executor that logs a message with template variable interpolation.
 *
 * <p>Target: log message template with {@code ${varName}} placeholders.
 * <p>Parameters: template variable values.
 */
public final class LogActionExecutor implements ActionExecutor {

    private static final Logger log = LoggerFactory.getLogger(LogActionExecutor.class);
    private static final Pattern TEMPLATE_VAR = Pattern.compile("\\$\\{(\\w+)}");

    @Override
    public String protocol() {
        return "log";
    }

    @Override
    public Map<String, Object> execute(String target, Map<String, Object> params, EvaluationContext ctx) {
        // Resolve parameter values: if a param value is a string matching a fact name,
        // resolve it from working memory so templates get actual fact values
        Map<String, Object> resolvedParams = resolveParams(params, ctx);
        String message = interpolate(target, resolvedParams);
        log.info("[RuleAction] {}", message);
        return Map.of("_logged", message);
    }

    private static Map<String, Object> resolveParams(Map<String, Object> params, EvaluationContext ctx) {
        if (ctx == null || ctx.getWorkingMemory() == null) return params;
        var wm = ctx.getWorkingMemory();
        var resolved = new java.util.LinkedHashMap<String, Object>();
        for (var entry : params.entrySet()) {
            Object val = entry.getValue();
            if (val instanceof String factName && wm.hasFact(factName)) {
                resolved.put(entry.getKey(), wm.get(factName));
            } else {
                resolved.put(entry.getKey(), val);
            }
        }
        return resolved;
    }

    private static String interpolate(String template, Map<String, Object> params) {
        if (template == null || template.isEmpty()) return "";
        Matcher matcher = TEMPLATE_VAR.matcher(template);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String varName = matcher.group(1);
            Object value = params.getOrDefault(varName, "${" + varName + "}");
            matcher.appendReplacement(sb, Matcher.quoteReplacement(String.valueOf(value)));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}
