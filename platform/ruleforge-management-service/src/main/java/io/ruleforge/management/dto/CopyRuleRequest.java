package io.ruleforge.management.dto;

import java.util.UUID;

public record CopyRuleRequest(
        String name,
        UUID targetSpaceId
) {}
