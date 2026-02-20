package io.ruleforge.management.dto;

public record CreateVersionRequest(
        String definition,
        String inputModel,
        String outputModel,
        String changeNote
) {}
