package io.ruleforge.solver.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

import java.util.UUID;

@Entity
@Immutable
@Table(name = "ab_tests")
public class SolverAbTestEntity {

    @Id
    private UUID id;

    @Column(name = "rule_id", nullable = false)
    private UUID ruleId;

    @Column(name = "version_a", nullable = false)
    private int versionA;

    @Column(name = "version_b", nullable = false)
    private int versionB;

    @Column(name = "traffic_split", nullable = false)
    private int trafficSplit;

    @Column(nullable = false)
    private String status;

    public UUID getId() { return id; }
    public UUID getRuleId() { return ruleId; }
    public int getVersionA() { return versionA; }
    public int getVersionB() { return versionB; }
    public int getTrafficSplit() { return trafficSplit; }
    public String getStatus() { return status; }
}
