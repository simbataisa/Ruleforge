package io.ruleforge.management.dto;

import java.util.UUID;

public record EligibleApproverDto(
        UUID memberId,
        UUID userId,
        String name,
        String email,
        String role
) {}
