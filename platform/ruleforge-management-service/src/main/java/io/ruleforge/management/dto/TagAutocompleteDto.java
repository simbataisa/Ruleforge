package io.ruleforge.management.dto;

public record TagAutocompleteDto(
        String tag,
        long ruleCount
) {}
