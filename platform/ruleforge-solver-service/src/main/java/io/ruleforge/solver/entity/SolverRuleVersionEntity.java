package io.ruleforge.solver.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

import java.time.Instant;
import java.util.UUID;

/**
 * Read-only JPA entity mapping to the {@code rule_versions} table.
 * The solver service never writes to this table; it only reads
 * version data needed for compiling and evaluating rules.
 */
@Entity
@Table(name = "rule_versions")
@Immutable
public class SolverRuleVersionEntity {

    @Id
    private UUID id;

    @Column(name = "rule_id", nullable = false)
    private UUID ruleId;

    @Column(nullable = false)
    private int version;

    @Column(columnDefinition = "jsonb")
    private String definition;

    @Column(name = "input_model", columnDefinition = "jsonb")
    private String inputModel;

    @Column(name = "output_model", columnDefinition = "jsonb")
    private String outputModel;

    @Column
    private byte[] compiled;

    @Column(nullable = false)
    private String status;

    @Column(name = "published_at")
    private Instant publishedAt;

    // Getters only (read-only entity)

    public UUID getId() {
        return id;
    }

    public UUID getRuleId() {
        return ruleId;
    }

    public int getVersion() {
        return version;
    }

    public String getDefinition() {
        return definition;
    }

    public String getInputModel() {
        return inputModel;
    }

    public String getOutputModel() {
        return outputModel;
    }

    public byte[] getCompiled() {
        return compiled;
    }

    public String getStatus() {
        return status;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }
}
