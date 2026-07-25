package io.agentforge.controlplane.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import io.agentforge.controlplane.domain.OutboxEventEntity;

class EventEnvelopeCodecTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final EventEnvelopeCodec codec = new EventEnvelopeCodec(objectMapper);

    @Test
    void wrapsOutboxPayloadInVersionedEnvelope() {
        UUID runId = UUID.randomUUID();
        OutboxEventEntity outbox = new OutboxEventEntity(
                "AgentRun",
                runId,
                "RUN_QUEUED",
                "{\"runId\":\"" + runId + "\"}");

        EventEnvelope envelope = codec.decode(codec.encode(outbox));

        assertThat(envelope.schemaVersion()).isEqualTo(EventEnvelope.CURRENT_SCHEMA_VERSION);
        assertThat(envelope.eventId()).isEqualTo(outbox.getId());
        assertThat(envelope.aggregateId()).isEqualTo(runId);
        assertThat(envelope.correlationId()).isEqualTo(runId.toString());
        assertThat(envelope.payload().path("runId").asText()).isEqualTo(runId.toString());
    }

    @Test
    void rejectsUnsupportedEnvelopeVersion() {
        String value = """
                {
                  "schemaVersion": 2,
                  "eventId": "%s",
                  "eventType": "RUN_STATUS_CHANGED",
                  "source": "test",
                  "aggregateType": "AgentRun",
                  "aggregateId": "%s",
                  "occurredAt": "%s",
                  "correlationId": "test",
                  "payload": {}
                }
                """.formatted(UUID.randomUUID(), UUID.randomUUID(), Instant.now());

        assertThatThrownBy(() -> codec.decode(value))
                .isInstanceOf(InvalidEventEnvelopeException.class);
    }
}
