package io.ruleforge.solver.dto;

import java.util.List;

/**
 * Response payload for bulk rule evaluation.
 *
 * @param results  individual solve results for each input
 * @param metadata aggregate metadata for the bulk operation
 */
public record BulkSolveResponse(
        List<SolveResponse> results,
        BulkMetadata metadata
) {

    /**
     * Aggregate metadata for a bulk solve operation.
     *
     * @param totalInputs  total number of inputs submitted
     * @param successCount number of successful evaluations
     * @param errorCount   number of failed evaluations
     * @param totalTimeMs  total wall-clock time in milliseconds
     */
    public record BulkMetadata(
            int totalInputs,
            int successCount,
            int errorCount,
            long totalTimeMs
    ) {}
}
