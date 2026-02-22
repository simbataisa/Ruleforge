package io.ruleforge.management.dto;

public record UpdateVersionRequest(
        String definition,
        String inputModel,
        String outputModel,
        String changeNote
) {}
