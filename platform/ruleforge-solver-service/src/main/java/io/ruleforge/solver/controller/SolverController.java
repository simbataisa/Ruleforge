package io.ruleforge.solver.controller;

import io.ruleforge.common.dto.ApiResponse;
import io.ruleforge.solver.dto.BulkSolveRequest;
import io.ruleforge.solver.dto.BulkSolveResponse;
import io.ruleforge.solver.dto.SolveRequest;
import io.ruleforge.solver.dto.SolveResponse;
import io.ruleforge.solver.service.SolverService;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for rule evaluation (solving).
 * Provides endpoints for single and bulk rule execution, with optional
 * version pinning and correlation ID tracking.
 */
@RestController
@RequestMapping("/v1/solve")
public class SolverController {

    private final SolverService solverService;

    public SolverController(SolverService solverService) {
        this.solverService = solverService;
    }

    /**
     * Evaluate a rule using its latest published version.
     *
     * @param ruleIdOrAlias the rule UUID or alias
     * @param request       the solve request with input facts
     * @param correlationId optional correlation ID for request tracing
     * @return the evaluation result
     */
    @PostMapping("/{ruleIdOrAlias}")
    public ApiResponse<SolveResponse> solve(
            @PathVariable String ruleIdOrAlias,
            @RequestBody @Valid SolveRequest request,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId) {
        return ApiResponse.ok(solverService.solve(ruleIdOrAlias, null, request));
    }

    /**
     * Evaluate a rule at a specific version.
     *
     * @param ruleIdOrAlias the rule UUID or alias
     * @param version       the version number to evaluate
     * @param request       the solve request with input facts
     * @param correlationId optional correlation ID for request tracing
     * @return the evaluation result
     */
    @PostMapping("/{ruleIdOrAlias}/{version}")
    public ApiResponse<SolveResponse> solveVersion(
            @PathVariable String ruleIdOrAlias,
            @PathVariable int version,
            @RequestBody @Valid SolveRequest request,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId) {
        return ApiResponse.ok(solverService.solve(ruleIdOrAlias, version, request));
    }

    /**
     * Evaluate a rule in bulk against multiple input sets, using the latest published version.
     *
     * @param ruleIdOrAlias the rule UUID or alias
     * @param request       the bulk solve request with multiple input sets
     * @param correlationId optional correlation ID for request tracing
     * @return the bulk evaluation result
     */
    @PostMapping("/bulk/{ruleIdOrAlias}")
    public ApiResponse<BulkSolveResponse> solveBulk(
            @PathVariable String ruleIdOrAlias,
            @RequestBody @Valid BulkSolveRequest request,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId) {
        return ApiResponse.ok(solverService.solveBulk(ruleIdOrAlias, null, request));
    }

    /**
     * Evaluate a rule in bulk against multiple input sets at a specific version.
     *
     * @param ruleIdOrAlias the rule UUID or alias
     * @param version       the version number to evaluate
     * @param request       the bulk solve request with multiple input sets
     * @param correlationId optional correlation ID for request tracing
     * @return the bulk evaluation result
     */
    @PostMapping("/bulk/{ruleIdOrAlias}/{version}")
    public ApiResponse<BulkSolveResponse> solveBulkVersion(
            @PathVariable String ruleIdOrAlias,
            @PathVariable int version,
            @RequestBody @Valid BulkSolveRequest request,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId) {
        return ApiResponse.ok(solverService.solveBulk(ruleIdOrAlias, version, request));
    }
}
