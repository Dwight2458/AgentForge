package io.agentforge.controlplane.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import io.agentforge.controlplane.domain.RunEntity;
import io.agentforge.controlplane.domain.RunStatus;
import io.agentforge.controlplane.repository.InboxEventRepository;
import io.agentforge.controlplane.repository.RunRepository;
import io.agentforge.controlplane.service.RunEventService;

class RunEventConsumerTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final EventEnvelopeCodec codec = new EventEnvelopeCodec(objectMapper);
    private final InboxEventRepository inbox = mock(InboxEventRepository.class);
    private final RunRepository runs = mock(RunRepository.class);
    private final RunEventService runEvents = mock(RunEventService.class);
    private RunEventConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new RunEventConsumer(codec, objectMapper, inbox, runs, runEvents);
    }

    @Test
    void projectsNewStatusEventAndRecordsInboxEntry() {
        RunEntity run = new RunEntity(UUID.randomUUID(), 60, 3, 150, 200_000);
        UUID eventId = UUID.randomUUID();
        EventEnvelope envelope = envelope(eventId, run.getId(), RunStatus.PROVISIONING);
        when(inbox.insertIfAbsent(RunEventConsumer.CONSUMER_NAME, eventId)).thenReturn(1);
        when(runs.findById(run.getId())).thenReturn(Optional.of(run));

        consumer.consume(codec.encode(envelope));

        assertThat(run.getStatus()).isEqualTo(RunStatus.PROVISIONING);
        verify(runEvents).append(
                eq(run.getId()),
                eq(RunEventConsumer.RUN_STATUS_CHANGED),
                eq("Runtime"),
                eq("Sandbox provisioning started"),
                eq("{\"namespace\":\"agentforge-run-test\"}"));
    }

    @Test
    void ignoresAlreadyProcessedEvent() {
        UUID eventId = UUID.randomUUID();
        EventEnvelope envelope = envelope(eventId, UUID.randomUUID(), RunStatus.PROVISIONING);
        when(inbox.insertIfAbsent(RunEventConsumer.CONSUMER_NAME, eventId)).thenReturn(0);

        consumer.consume(codec.encode(envelope));

        verify(runs, never()).findById(any());
        verify(runEvents, never()).append(any(), any(), any(), any(), any());
    }

    private EventEnvelope envelope(UUID eventId, UUID runId, RunStatus status) {
        return new EventEnvelope(
                EventEnvelope.CURRENT_SCHEMA_VERSION,
                eventId,
                RunEventConsumer.RUN_STATUS_CHANGED,
                "agentforge-agent-runtime",
                "AgentRun",
                runId,
                Instant.now(),
                runId.toString(),
                objectMapper.createObjectNode()
                        .put("status", status.name())
                        .put("agent", "Runtime")
                        .put("summary", "Sandbox provisioning started")
                        .set("details", objectMapper.createObjectNode()
                                .put("namespace", "agentforge-run-test")));
    }
}
