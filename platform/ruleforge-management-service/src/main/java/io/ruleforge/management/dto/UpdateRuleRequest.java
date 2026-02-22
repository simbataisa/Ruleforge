package io.ruleforge.management.dto;

import java.util.UUID;

public record UpdateRuleRequest(
        String name,
        String alias,
        String description,
        UUID folderId
) {}
