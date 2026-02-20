package io.ruleforge.core.spi;

import io.ruleforge.core.model.CompiledRule;

/**
 * SPI for adding custom compilation phases (e.g., regulatory compliance checking,
 * custom optimization passes).
 */
public interface CompilationPhaseExtension {

    /**
     * Ordering of this phase relative to other phases (lower = earlier).
     */
    int order();

    /**
     * Process a compiled rule, potentially transforming or validating it.
     *
     * @param input the compiled rule from the previous phase
     * @param ctx compilation context
     * @return the processed compiled rule
     */
    CompiledRule process(CompiledRule input, CompilationContext ctx);
}
