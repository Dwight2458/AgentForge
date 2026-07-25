package io.agentforge.controlplane.domain;

import java.io.Serializable;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public record InboxEventId(
        @Column(nullable = false) String consumer,
        @Column(name = "event_id", nullable = false) UUID eventId) implements Serializable {}
