package io.ruleforge.core.model;

import io.ruleforge.core.spi.ConflictResolutionStrategy;

import java.util.List;

/**
 * Groups a set of {@link RuleUnit}s under a common execution policy.
 *
 * @param name              Group name.
 * @param type              Logical grouping type.
 * @param strategy          Conflict resolution strategy applied to this group.
 * @param children          Ordered list of child rule units.
 * @param inheritedMetadata Metadata inherited by children that do not override it.
 */
public record RuleGroup(
        String name,
        GroupType type,
        ConflictResolutionStrategy strategy,
        List<RuleUnit> children,
        RuleMetadata inheritedMetadata
) {

    /**
     * Defines how child rules within a group interact during execution.
     */
    public enum GroupType {
        /** All children must match. */
        AND,
        /** At least one child must match. */
        OR,
        /** Exactly one child must match. */
        XOR,
        /** Children fire in strict priority order. */
        PRIORITY_ORDERED,
        /** Only one rule in the group may fire per evaluation cycle. */
        ACTIVATION_GROUP,
        /** Logical grouping on the agenda; focus determines which group fires. */
        AGENDA_GROUP
    }
}
