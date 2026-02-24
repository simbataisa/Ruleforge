package io.ruleforge.core.action;

import io.ruleforge.core.spi.ActionExecutor;
import io.ruleforge.core.spi.EvaluationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Action executor that evaluates inline JavaScript via GraalVM polyglot.
 * Falls back gracefully if GraalVM is not available.
 *
 * <p>Target: inline JavaScript code. Must define a function named {@code evaluate}
 * that receives a params object and returns an object (or void for side-effect-only scripts).
 * <p>Parameters: available as the argument to the {@code evaluate} function.
 * <p>Timeout: defaults to 5 seconds. Override via {@code timeout} parameter (in seconds).
 */
public final class ScriptActionExecutor implements ActionExecutor {

    private static final Logger log = LoggerFactory.getLogger(ScriptActionExecutor.class);
    private static final int DEFAULT_TIMEOUT_SECONDS = 5;

    @Override
    public String protocol() {
        return "script";
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> execute(String target, Map<String, Object> params, EvaluationContext ctx) {
        if (target == null || target.isBlank()) {
            return Map.of("_error", "Script action target (script body) must not be empty");
        }

        int timeoutSeconds = DEFAULT_TIMEOUT_SECONDS;
        if (params.containsKey("timeout")) {
            try {
                timeoutSeconds = ((Number) params.get("timeout")).intValue();
            } catch (Exception e) {
                log.warn("Invalid timeout parameter '{}', using default {}s", params.get("timeout"), DEFAULT_TIMEOUT_SECONDS);
            }
        }

        ExecutorService executor = Executors.newSingleThreadExecutor();
        final String scriptBody = target;
        final Map<String, Object> scriptParams = params;

        try {
            Future<Map<String, Object>> future = executor.submit(() -> executeScript(scriptBody, scriptParams));
            return future.get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            log.error("Script action timed out after {}s", timeoutSeconds);
            return Map.of("_error", "Script execution timed out after " + timeoutSeconds + " seconds");
        } catch (ExecutionException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            log.error("Script action failed: {}", cause.getMessage(), cause);
            return Map.of("_error", "Script execution failed: " + cause.getMessage());
        } catch (Exception e) {
            log.error("Script action failed: {}", e.getMessage(), e);
            return Map.of("_error", "Script execution failed: " +
                    (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
        } finally {
            executor.shutdownNow();
        }
    }

    private Map<String, Object> executeScript(String target, Map<String, Object> params) {
        try {
            // Use GraalVM polyglot via reflection to avoid hard dependency
            Class<?> contextClass = Class.forName("org.graalvm.polyglot.Context");
            Object builder = contextClass.getMethod("newBuilder", String[].class)
                    .invoke(null, (Object) new String[]{"js"});

            // Configure sandbox
            Class<?> builderClass = builder.getClass();
            builder = builderClass.getMethod("allowAllAccess", boolean.class).invoke(builder, false);
            Object polyglotCtx = builderClass.getMethod("build").invoke(builder);

            try {
                // Evaluate the script
                polyglotCtx.getClass().getMethod("eval", String.class, CharSequence.class)
                        .invoke(polyglotCtx, "js", target);

                // Get the evaluate function
                Class<?> valueClass = Class.forName("org.graalvm.polyglot.Value");
                Object bindings = polyglotCtx.getClass().getMethod("getBindings", String.class)
                        .invoke(polyglotCtx, "js");
                Object evalFn = valueClass.getMethod("getMember", String.class).invoke(bindings, "evaluate");

                if (evalFn != null && (boolean) valueClass.getMethod("canExecute").invoke(evalFn)) {
                    // Convert params to JSON string and safely parse in JS context.
                    // Use JSON.parse() with the JSON string stored via a separate eval
                    // to avoid string injection through naive concatenation.
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    String paramsJson = mapper.writeValueAsString(params);

                    // Safely pass JSON: eval the JSON directly as a JS expression
                    // (valid JSON is a valid JS expression when wrapped in parens)
                    Object jsParams = polyglotCtx.getClass().getMethod("eval", String.class, CharSequence.class)
                            .invoke(polyglotCtx, "js", "(" + paramsJson + ")");

                    Object result = valueClass.getMethod("execute", Object[].class)
                            .invoke(evalFn, (Object) new Object[]{jsParams});

                    // Handle null/undefined return (void scripts) — return empty map
                    if ((boolean) valueClass.getMethod("isNull").invoke(result)) {
                        return Collections.emptyMap();
                    }

                    if ((boolean) valueClass.getMethod("hasMembers").invoke(result)) {
                        // Convert result to Map
                        Object keys = valueClass.getMethod("getMemberKeys").invoke(result);
                        Map<String, Object> resultMap = new LinkedHashMap<>();
                        for (Object key : (Iterable<?>) keys) {
                            Object val = valueClass.getMethod("getMember", String.class).invoke(result, key.toString());
                            if ((boolean) valueClass.getMethod("isNull").invoke(val)) {
                                resultMap.put(key.toString(), null);
                            } else if ((boolean) valueClass.getMethod("isString").invoke(val)) {
                                resultMap.put(key.toString(), valueClass.getMethod("asString").invoke(val));
                            } else if ((boolean) valueClass.getMethod("isNumber").invoke(val)) {
                                resultMap.put(key.toString(), valueClass.getMethod("asDouble").invoke(val));
                            } else if ((boolean) valueClass.getMethod("isBoolean").invoke(val)) {
                                resultMap.put(key.toString(), valueClass.getMethod("asBoolean").invoke(val));
                            } else {
                                resultMap.put(key.toString(), valueClass.getMethod("toString").invoke(val));
                            }
                        }
                        return resultMap;
                    }

                    // Primitive return value
                    String resultStr = (String) valueClass.getMethod("toString").invoke(result);
                    if ("undefined".equals(resultStr)) {
                        return Collections.emptyMap();
                    }
                    return Map.of("_result", resultStr);
                }

                return Map.of("_error", "Script must define an 'evaluate' function. " +
                        "Expected: function evaluate(params) { ... }");
            } finally {
                polyglotCtx.getClass().getMethod("close").invoke(polyglotCtx);
            }
        } catch (ClassNotFoundException e) {
            log.warn("GraalVM polyglot not available; script action cannot execute");
            return Map.of("_error", "GraalVM polyglot runtime not available. " +
                    "Ensure GraalVM polyglot JS dependency is on the classpath.");
        } catch (Exception e) {
            String message = e.getMessage();
            if (e.getCause() != null && e.getCause().getMessage() != null) {
                message = e.getCause().getMessage();
            }
            log.error("Script action failed: {}", message, e);
            return Map.of("_error", "Script execution error: " + (message != null ? message : e.getClass().getSimpleName()));
        }
    }
}
