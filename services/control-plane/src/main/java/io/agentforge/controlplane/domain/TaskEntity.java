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
@Table(name = "tasks")
public class TaskEntity {

    @Id
    private UUID id;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "request_text", nullable = false, columnDefinition = "text")
    private String requestText;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected TaskEntity() {}

    public TaskEntity(UUID projectId, String requestText) {
        this.id = UUID.randomUUID();
        this.projectId = projectId;
        this.requestText = requestText;
        this.status = TaskStatus.GRILLING;
        this.createdAt = Instant.now();
        this.updatedAt = createdAt;
    }

    public void markSpecReady() {
        this.status = TaskStatus.SPEC_READY;
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getProjectId() { return projectId; }
    public String getRequestText() { return requestText; }
    public TaskStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}

