package io.ruleforge.auth.dto;

import java.time.Instant;
import java.util.UUID;

public record MemberDto(
        UUID id,
        UUID userId,
        String email,
        String fullName,
        String role,
        Instant createdAt
) {}
