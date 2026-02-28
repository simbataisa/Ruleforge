package io.ruleforge.management.dto;

import java.util.List;
import java.util.UUID;

public record SpaceApprovalSettingsDto(
        UUID spaceId,
        List<UUID> defaultApprovers,
        int slaHours,
        boolean requireAllApprovers
) {}
