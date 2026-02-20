package io.ruleforge.common.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method for audit logging. An AOP aspect (provided by each service
 * or a shared auto-configuration) intercepts annotated methods and records
 * the action, resource type, actor, and timestamp.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditLog {

    /**
     * The action being performed, e.g. "CREATE_RULE", "DELETE_SPACE".
     */
    String action();

    /**
     * The type of resource being acted upon, e.g. "Rule", "Space".
     * Defaults to empty (inferred from context).
     */
    String resourceType() default "";
}
