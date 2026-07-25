package io.agentforge.controlplane.messaging;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;

public record EventEnvelope(
        int schemaVersion,
        UUID eventId,
        String eventType,
        String source,
        String aggregateType,
        UUID aggregateId,
        Instant occurredAt,
        String correlationId,
        JsonNode payload) {

    public static final int CURRENT_SCHEMA_VERSION = 1;

    public EventEnvelope {
        if (schemaVersion != CURRENT_SCHEMA_VERSION) {
            throw new InvalidEventEnvelopeException("Unsupported event envelope version: " + schemaVersion);
        }
        if (eventId == null || aggregateId == null || occurredAt == null || payload == null) {
            throw new InvalidEventEnvelopeException("Event envelope identifiers, time, and payload are required");
        }
        if (isBlank(eventType) || isBlank(source) || isBlank(aggregateType) || isBlank(correlationId)) {
            throw new InvalidEventEnvelopeException("Event envelope routing fields are required");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
