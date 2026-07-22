package io.agentforge.controlplane.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "requirement_specs")
public class RequirementSpecEntity {

    @Id
    @Column(name = "task_id")
    private UUID taskId;

    @Column(nullable = false, columnDefinition = "text")
    private String goal;

    @Column(name = "acceptance_criteria", nullable = false, columnDefinition = "text")
    private String acceptanceCriteria;

    @Column(nullable = false, columnDefinition = "text")
    private String constraints;

    @Column(name = "test_plan", nullable = false, columnDefinition = "text")
    private String testPlan;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected RequirementSpecEntity() {}

    public RequirementSpecEntity(UUID taskId, String goal, String acceptanceCriteria, String constraints, String testPlan) {
        this.taskId = taskId;
        this.goal = goal;
        this.acceptanceCriteria = acceptanceCriteria;
        this.constraints = constraints;
        this.testPlan = testPlan;
        this.createdAt = Instant.now();
    }

    public UUID getTaskId() { return taskId; }
    public String getGoal() { return goal; }
    public String getAcceptanceCriteria() { return acceptanceCriteria; }
    public String getConstraints() { return constraints; }
    public String getTestPlan() { return testPlan; }
    public Instant getCreatedAt() { return createdAt; }
}

