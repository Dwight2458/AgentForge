package io.agentforge.controlplane.messaging;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.agentforge.controlplane.domain.RunEntity;
import io.agentforge.controlplane.repository.InboxEventRepository;
import io.agentforge.controlplane.repository.RunRepository;
import io.agentforge.controlplane.service.NotFoundException;
import io.agentforge.controlplane.service.RunEventService;

@Component
@ConditionalOnProperty(name = "agentforge.messaging.consumer-enabled", havingValue = "true")
public class RunEventConsumer {

    static final String CONSUMER_NAME = "control-plane-run-projector-v1";
    static final String RUN_STATUS_CHANGED = "RUN_STATUS_CHANGED";

    private final EventEnvelopeCodec envelopes;
    private final ObjectMapper objectMapper;
    private final InboxEventRepository inbox;
    private final RunRepository runs;
    private final RunEventService runEvents;

    public RunEventConsumer(
            EventEnvelopeCodec envelopes,
            ObjectMapper objectMapper,
            InboxEventRepository inbox,
            RunRepository runs,
            RunEventService runEvents) {
        this.envelopes = envelopes;
        this.objectMapper = objectMapper;
        this.inbox = inbox;
        this.runs = runs;
        this.runEvents = runEvents;
    }

    @KafkaListener(
            topics = "${agentforge.messaging.run-events-topic}",
            groupId = "${agentforge.messaging.consumer-group}")
    @Transactional
    public void consume(String message) {
        EventEnvelope envelope = envelopes.decode(message);
        if (!"AgentRun".equals(envelope.aggregateType())) {
            throw new InvalidEventEnvelopeException(
                    "Unsupported aggregate type: " + envelope.aggregateType());
        }
        if (!RUN_STATUS_CHANGED.equals(envelope.eventType())) {
            throw new InvalidEventEnvelopeException(
                    "Unsupported run event type: " + envelope.eventType());
        }
        if (inbox.insertIfAbsent(CONSUMER_NAME, envelope.eventId()) == 0) {
            return;
        }

        RunStatusChangedPayload payload = readPayload(envelope);
        RunEntity run = runs.findById(envelope.aggregateId())
                .orElseThrow(() -> new NotFoundException("Run not found: " + envelope.aggregateId()));
        if (run.getStatus() != payload.status()) {
            run.transitionTo(payload.status());
        }
        runEvents.append(
                run.getId(),
                envelope.eventType(),
                payload.agent(),
                payload.summary(),
                writeDetails(payload));
    }

    private RunStatusChangedPayload readPayload(EventEnvelope envelope) {
        try {
            return objectMapper.treeToValue(envelope.payload(), RunStatusChangedPayload.class);
        } catch (JsonProcessingException | IllegalArgumentException exception) {
            throw new InvalidEventEnvelopeException("Invalid run status event payload", exception);
        }
    }

    private String writeDetails(RunStatusChangedPayload payload) {
        try {
            return objectMapper.writeValueAsString(payload.details());
        } catch (JsonProcessingException exception) {
            throw new InvalidEventEnvelopeException("Unable to serialize run event details", exception);
        }
    }
}
