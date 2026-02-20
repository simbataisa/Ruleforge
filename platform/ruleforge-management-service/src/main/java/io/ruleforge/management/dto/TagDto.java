package io.ruleforge.management.dto;

import java.util.UUID;

public record TagDto(
        UUID id,
        String name,
        String color
) {}
