package io.ruleforge.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record AddMemberRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Must be a valid email address")
        String email,

        @NotBlank(message = "Role is required")
        @Pattern(regexp = "^(OWNER|ADMIN|EDITOR|VIEWER|SOLVER)$", message = "Role must be one of: OWNER, ADMIN, EDITOR, VIEWER, SOLVER")
        String role
) {}
