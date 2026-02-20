package io.ruleforge.auth.dto;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        UserDto user
) {}
