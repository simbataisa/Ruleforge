package io.ruleforge.management.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record CreateFolderRequest(
        @NotBlank(message = "Name is required")
        String name,

        UUID parentId,

        int sortOrder
) {}
