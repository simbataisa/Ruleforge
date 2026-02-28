package io.ruleforge.management.dto;

import java.util.List;
import java.util.UUID;

public record UpdateSpaceApprovalSettingsRequest(
        List<UUID> defaultApprovers,
        Integer slaHours,
        Boolean requireAllApprovers
) {}
