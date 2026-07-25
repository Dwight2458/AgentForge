package io.agentforge.controlplane.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "inbox_events")
public class InboxEventEntity {

    @EmbeddedId
    private InboxEventId id;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    protected InboxEventEntity() {}

    public InboxEventId getId() {
        return id;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }
}
