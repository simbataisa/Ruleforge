package io.ruleforge.management.dto;

import java.util.List;
import java.util.UUID;

public record FolderDto(
        UUID id,
        String name,
        UUID parentId,
        int sortOrder,
        List<FolderDto> children
) {}
