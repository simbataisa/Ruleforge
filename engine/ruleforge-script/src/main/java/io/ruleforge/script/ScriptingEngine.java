package io.ruleforge.script;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.ruleforge.core.RuleEvaluationException;
import io.ruleforge.core.memory.WorkingMemory;
import io.ruleforge.core.model.*;
import io.ruleforge.core.result.MatchedRule;
import io.ruleforge.core.result.RuleResult;
import io.ruleforge.core.result.RuleResultMetadata;
import io.ruleforge.core.spi.EvaluationContext;
import io.ruleforge.core.spi.RuleTypeEngine;
import io.ruleforge.script.compiler.CompiledScriptRule;
import io.ruleforge.script.compiler.ScriptCompiler;
import io.ruleforge.script.validator.ScriptValidator;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * GraalVM-based sandboxed scripting engine for JavaScript rule execution.
 * <p>
 * Implements {@link RuleTypeEngine} for rule type {@link RuleType#SCRIPTING_RULE}.
 * Each evaluation creates a fresh sandboxed GraalVM context and closes it
 * after execution to prevent resource leaks.
 */
public final class ScriptingEngine implements RuleTypeEngine {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final ScriptCompiler compiler = new ScriptCompiler();
    private final ScriptValidator validator = new ScriptValidator();

    @Override
    public RuleType supportedType() {
        return RuleType.SCRIPTING_RULE;
    }

    @Override
    public CompiledRule compile(RuleDefinition definition) {
        return compiler.compile(definition);
    }

    @Override
    public RuleResult evaluate(CompiledRule rule, WorkingMemory workingMemory, EvaluationContext ctx) {
        if (!(rule instanceof CompiledScriptRule scriptRule)) {
            throw new RuleEvaluationException(
                    rule.ruleId(),
                    "Expected CompiledScriptRule but got " + rule.getClass().getSimpleName());
        }

        long startMs = System.currentTimeMillis();

        try (Context context = scriptRule.contextPool().createContext(scriptRule.language())) {
            // Build input map from working memory facts
            Map<String, Object> inputMap = buildInputMap(workingMemory);
            String inputJson = OBJECT_MAPPER.writeValueAsString(inputMap);

            // Evaluate script body and capture the evaluate function
            context.eval(scriptRule.language(), scriptRule.scriptBody());

            // Pass input as JSON-parsed object and call evaluate()
            context.eval(scriptRule.language(), "var __input = JSON.parse(" +
                    escapeJsonForJs(inputJson) + ");");
            Value evaluateFn = context.getBindings(scriptRule.language()).getMember("evaluate");

            if (evaluateFn == null || !evaluateFn.canExecute()) {
                throw new RuleEvaluationException(rule.ruleId(),
                        "Script must define a function named 'evaluate'");
            }

            Value scriptResult = evaluateFn.execute(
                    context.getBindings(scriptRule.language()).getMember("__input"));

            Map<String, Object> output = convertValueToMap(scriptResult);
            long elapsed = System.currentTimeMillis() - startMs;

            var metadata = new RuleResultMetadata(
                    Duration.ofMillis(elapsed), 1, 1, 0, "SCRIPT");

            var matchedRules = output.isEmpty() ? List.<MatchedRule>of() : List.of(
                    new MatchedRule(rule.ruleId(), rule.ruleId(), 0,
                            new PriorityVector(0, 0, 1.0, Instant.now(), 0),
                            inputMap, output));

            return new RuleResult(output, metadata, matchedRules, null);

        } catch (PolyglotException e) {
            throw new RuleEvaluationException(rule.ruleId(),
                    "Script execution error: " + e.getMessage(), e);
        } catch (RuleEvaluationException e) {
            throw e;
        } catch (Exception e) {
            throw new RuleEvaluationException(rule.ruleId(),
                    "Unexpected script error: " + e.getMessage(), e);
        }
    }

    @Override
    public ValidationResult validate(RuleDefinition definition) {
        return validator.validate(definition);
    }

    private Map<String, Object> buildInputMap(WorkingMemory workingMemory) {
        Map<String, Object> inputMap = new LinkedHashMap<>();
        for (String path : workingMemory.getFactPaths()) {
            var fact = workingMemory.getFact(path);
            if (fact != null) {
                inputMap.put(path, fact.value());
            }
        }
        return inputMap;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> convertValueToMap(Value value) {
        if (value == null || value.isNull()) return Map.of();

        Map<String, Object> result = new LinkedHashMap<>();
        if (value.hasMembers()) {
            for (String key : value.getMemberKeys()) {
                Value member = value.getMember(key);
                result.put(key, convertValue(member));
            }
        }
        return result;
    }

    private Object convertValue(Value value) {
        if (value == null || value.isNull()) return null;
        if (value.isBoolean()) return value.asBoolean();
        if (value.isNumber()) {
            if (value.fitsInLong()) return value.asLong();
            return value.asDouble();
        }
        if (value.isString()) return value.asString();
        if (value.hasArrayElements()) {
            List<Object> list = new ArrayList<>();
            for (long i = 0; i < value.getArraySize(); i++) {
                list.add(convertValue(value.getArrayElement(i)));
            }
            return list;
        }
        if (value.hasMembers()) {
            Map<String, Object> map = new LinkedHashMap<>();
            for (String key : value.getMemberKeys()) {
                map.put(key, convertValue(value.getMember(key)));
            }
            return map;
        }
        return value.toString();
    }

    private String escapeJsonForJs(String json) {
        return "'" + json.replace("\\", "\\\\").replace("'", "\\'") + "'";
    }
}
