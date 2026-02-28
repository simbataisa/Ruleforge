package io.ruleforge.management.dto;

import java.util.List;
import java.util.UUID;

public record UpdateRuleRequest(
        String name,
        String alias,
        String description,
        UUID folderId,
        List<String> tags
) {}
