package io.ruleforge.core.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Supported fact/field types in the RuleForge type system.
 */
public enum FactType {

    STRING(String.class),
    NUMBER(Double.class),
    INTEGER(Long.class),
    BOOLEAN(Boolean.class),
    DATE(LocalDate.class),
    DATETIME(Instant.class),
    ARRAY(List.class),
    OBJECT(Map.class),
    MONEY(BigDecimal.class),
    ENUM(String.class),
    ANY(Object.class);

    private final Class<?> javaType;

    FactType(Class<?> javaType) {
        this.javaType = javaType;
    }

    /**
     * Returns the default Java class that backs this fact type.
     */
    public Class<?> javaType() {
        return javaType;
    }
}
