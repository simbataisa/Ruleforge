package io.ruleforge.management.dto;

import java.util.UUID;

public record SampleInputDto(
        UUID id,
        String name,
        String description,
        String input,
        int sortOrder
) {}
