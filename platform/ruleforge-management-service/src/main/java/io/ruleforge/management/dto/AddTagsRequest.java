package io.ruleforge.management.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record AddTagsRequest(
        @NotEmpty(message = "Tags list must not be empty")
        List<String> tags
) {}
