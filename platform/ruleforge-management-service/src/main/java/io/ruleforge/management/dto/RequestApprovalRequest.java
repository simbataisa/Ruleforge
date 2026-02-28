package io.ruleforge.management.dto;

import java.util.List;
import java.util.UUID;

public record RequestApprovalRequest(
        List<UUID> assigneeIds,
        String note
) {}
