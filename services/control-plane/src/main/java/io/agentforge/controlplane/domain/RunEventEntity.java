package io.agentforge.controlplane.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "run_events")
public class RunEventEntity {

    @Id
    private UUID id;

    @Column(name = "run_id", nullable = false)
    private UUID runId;

    @Column(name = "sequence_no", nullable = false)
    private long sequence;

    @Column(nullable = false)
    private String type;

    private String agent;

    @Column(nullable = false, columnDefinition = "text")
    private String summary;

    @Column(nullable = false, columnDefinition = "text")
    private String payload;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected RunEventEntity() {}

    public RunEventEntity(UUID runId, long sequence, String type, String agent, String summary, String payload) {
        this.id = UUID.randomUUID();
        this.runId = runId;
        this.sequence = sequence;
        this.type = type;
        this.agent = agent;
        this.summary = summary;
        this.payload = payload;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getRunId() { return runId; }
    public long getSequence() { return sequence; }
    public String getType() { return type; }
    public String getAgent() { return agent; }
    public String getSummary() { return summary; }
    public String getPayload() { return payload; }
    public Instant getCreatedAt() { return createdAt; }
}

