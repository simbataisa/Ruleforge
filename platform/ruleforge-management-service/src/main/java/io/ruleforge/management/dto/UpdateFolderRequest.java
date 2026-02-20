package io.ruleforge.management.dto;

public record UpdateFolderRequest(
        String name,
        int sortOrder
) {}
