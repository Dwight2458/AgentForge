package io.agentforge.controlplane.messaging;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.agentforge.controlplane.domain.OutboxEventEntity;

@Component
public class EventEnvelopeCodec {

    private static final String CONTROL_PLANE_SOURCE = "agentforge-control-plane";

    private final ObjectMapper objectMapper;

    public EventEnvelopeCodec(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String encode(OutboxEventEntity event) {
        try {
            JsonNode payload = objectMapper.readTree(event.getPayload());
            return encode(new EventEnvelope(
                    event.getEnvelopeVersion(),
                    event.getId(),
                    event.getEventType(),
                    CONTROL_PLANE_SOURCE,
                    event.getAggregateType(),
                    event.getAggregateId(),
                    event.getOccurredAt(),
                    event.getCorrelationId(),
                    payload));
        } catch (JsonProcessingException exception) {
            throw new InvalidEventEnvelopeException("Outbox payload is not valid JSON", exception);
        }
    }

    public String encode(EventEnvelope envelope) {
        try {
            return objectMapper.writeValueAsString(envelope);
        } catch (JsonProcessingException exception) {
            throw new InvalidEventEnvelopeException("Unable to serialize event envelope", exception);
        }
    }

    public EventEnvelope decode(String value) {
        try {
            return objectMapper.readValue(value, EventEnvelope.class);
        } catch (JsonProcessingException | IllegalArgumentException exception) {
            throw new InvalidEventEnvelopeException("Unable to parse event envelope", exception);
        }
    }
}
