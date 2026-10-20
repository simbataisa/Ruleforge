package io.ruleforge.management.service;

import io.ruleforge.common.exception.ConflictException;
import io.ruleforge.management.entity.RuleEntity;

import java.util.UUID;

/**
 * A locked rule can only be changed by the user who holds the lock.
 */
final class RuleLockGuard {

    private RuleLockGuard() {
    }

    static void assertEditableBy(RuleEntity rule, UUID userId) {
        if (rule.isLocked() && rule.getLockedBy() != null && !rule.getLockedBy().equals(userId)) {
            throw new ConflictException("Rule is locked by another user. Ask them to unlock it first.");
        }
    }
}
