package io.ruleforge.management.dto;

import java.util.List;
import java.util.UUID;

public record BatchApprovalResultDto(
        List<ApprovalSuccessItem> succeeded,
        List<ApprovalFailureItem> failed
) {
    public record ApprovalSuccessItem(UUID ruleId, int version, UUID requestId) {}
    public record ApprovalFailureItem(UUID ruleId, String reason) {}
}
