package io.ruleforge.solver.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

import java.util.UUID;

/**
 * Read-only JPA entity mapping to the {@code rules} table.
 * The solver service never writes to this table; it only reads
 * rule metadata needed for loading and compiling rules.
 */
@Entity
@Table(name = "rules")
@Immutable
public class SolverRuleEntity {

    @Id
    private UUID id;

    @Column(name = "space_id", nullable = false)
    private UUID spaceId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String alias;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private String status;

    @Column(name = "latest_version", nullable = false)
    private int latestVersion;

    // Getters only (read-only entity)

    public UUID getId() {
        return id;
    }

    public UUID getSpaceId() {
        return spaceId;
    }

    public String getName() {
        return name;
    }

    public String getAlias() {
        return alias;
    }

    public String getType() {
        return type;
    }

    public String getStatus() {
        return status;
    }

    public int getLatestVersion() {
        return latestVersion;
    }
}
