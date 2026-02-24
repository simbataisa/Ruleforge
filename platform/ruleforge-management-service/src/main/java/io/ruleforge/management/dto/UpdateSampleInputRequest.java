package io.ruleforge.management.dto;

public record UpdateSampleInputRequest(
        String name,
        String description,
        Object input,
        Integer sortOrder
) {}
