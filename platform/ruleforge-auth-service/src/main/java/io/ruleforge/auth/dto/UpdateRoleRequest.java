package io.ruleforge.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateRoleRequest(
        @NotBlank(message = "Role is required")
        @Pattern(regexp = "^(OWNER|ADMIN|EDITOR|VIEWER|SOLVER)$", message = "Role must be one of: OWNER, ADMIN, EDITOR, VIEWER, SOLVER")
        String role
) {}
