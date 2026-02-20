package io.ruleforge.script.validator;

import io.ruleforge.core.model.RuleDefinition;
import io.ruleforge.core.model.RuleType;
import io.ruleforge.core.model.ValidationResult;
import io.ruleforge.core.model.ValidationResult.ValidationError;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Source;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Validates a script rule definition for structural correctness and syntax.
 */
public final class ScriptValidator {

    public ValidationResult validate(RuleDefinition definition) {
        List<ValidationError> errors = new ArrayList<>();

        if (definition.type() != RuleType.SCRIPTING_RULE) {
            errors.add(new ValidationError("type", "INVALID_TYPE",
                    "Expected SCRIPTING_RULE but got " + definition.type()));
            return ValidationResult.failure(errors);
        }

        Map<String, Object> payload = definition.definition();
        if (payload == null) {
            errors.add(new ValidationError("definition", "NULL_DEFINITION",
                    "Script definition payload must not be null"));
            return ValidationResult.failure(errors);
        }

        Object scriptBodyObj = payload.get("scriptBody");
        if (scriptBodyObj == null) {
            errors.add(new ValidationError("definition.scriptBody", "MISSING_SCRIPT_BODY",
                    "Script definition must have a 'scriptBody' field"));
            return ValidationResult.failure(errors);
        }

        String scriptBody = scriptBodyObj.toString();
        if (scriptBody.isBlank()) {
            errors.add(new ValidationError("definition.scriptBody", "EMPTY_SCRIPT_BODY",
                    "Script body must not be empty"));
        }

        String language = (String) payload.getOrDefault("language", "js");
        if (!"js".equals(language) && !"javascript".equals(language)) {
            errors.add(new ValidationError("definition.language", "UNSUPPORTED_LANGUAGE",
                    "Language '" + language + "' is not supported. Use 'js'"));
        }

        // Syntax check via GraalVM parse
        if (!scriptBody.isBlank() && errors.isEmpty()) {
            try (Context ctx = Context.newBuilder("js")
                    .allowAllAccess(false)
                    .option("engine.WarnInterpreterOnly", "false")
                    .build()) {
                Source source = Source.newBuilder("js", scriptBody, "rule-validation.js").build();
                ctx.parse(source);
            } catch (PolyglotException e) {
                errors.add(new ValidationError("definition.scriptBody", "SYNTAX_ERROR",
                        "Script syntax error: " + e.getMessage()));
            } catch (Exception e) {
                // Ignore other validation errors - may be environment-specific
            }
        }

        return errors.isEmpty() ? ValidationResult.success() : ValidationResult.failure(errors);
    }
}
