package io.agentforge.controlplane.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "agent_runs")
public class RunEntity {

    @Id
    private UUID id;

    @Column(name = "task_id", nullable = false)
    private UUID taskId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RunStatus status;

    @Column(name = "max_minutes", nullable = false)
    private int maxMinutes;

    @Column(name = "max_repair_rounds", nullable = false)
    private int maxRepairRounds;

    @Column(name = "max_tool_calls", nullable = false)
    private int maxToolCalls;

    @Column(name = "max_tokens", nullable = false)
    private int maxTokens;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected RunEntity() {}

    public RunEntity(UUID taskId, int maxMinutes, int maxRepairRounds, int maxToolCalls, int maxTokens) {
        this.id = UUID.randomUUID();
        this.taskId = taskId;
        this.status = RunStatus.QUEUED;
        this.maxMinutes = maxMinutes;
        this.maxRepairRounds = maxRepairRounds;
        this.maxToolCalls = maxToolCalls;
        this.maxTokens = maxTokens;
        this.createdAt = Instant.now();
        this.updatedAt = createdAt;
    }

    public void transitionTo(RunStatus target) {
        if (!status.canTransitionTo(target)) {
            throw new IllegalStateException("Invalid run transition: " + status + " -> " + target);
        }
        status = target;
        updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getTaskId() { return taskId; }
    public RunStatus getStatus() { return status; }
    public int getMaxMinutes() { return maxMinutes; }
    public int getMaxRepairRounds() { return maxRepairRounds; }
    public int getMaxToolCalls() { return maxToolCalls; }
    public int getMaxTokens() { return maxTokens; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}

