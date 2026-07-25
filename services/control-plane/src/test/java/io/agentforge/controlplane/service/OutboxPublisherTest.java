package io.agentforge.controlplane.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import io.agentforge.controlplane.config.AgentForgeProperties;
import io.agentforge.controlplane.domain.OutboxEventEntity;
import io.agentforge.controlplane.messaging.EventEnvelope;
import io.agentforge.controlplane.messaging.EventEnvelopeCodec;
import io.agentforge.controlplane.repository.OutboxEventRepository;

class OutboxPublisherTest {

    @SuppressWarnings("unchecked")
    @Test
    void publishesEnvelopeBeforeMarkingEventPublished() {
        OutboxEventRepository outbox = mock(OutboxEventRepository.class);
        KafkaTemplate<String, String> kafka = mock(KafkaTemplate.class);
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        EventEnvelopeCodec codec = new EventEnvelopeCodec(objectMapper);
        AgentForgeProperties properties = properties();
        OutboxEventEntity event = new OutboxEventEntity(
                "AgentRun",
                UUID.randomUUID(),
                "RUN_QUEUED",
                "{\"taskId\":\"task-1\"}");
        when(outbox.findByPublishedAtIsNullOrderByOccurredAtAsc(any(Pageable.class)))
                .thenReturn(List.of(event));
        when(kafka.send(
                eq("agentforge.run.commands.v1"),
                eq(event.getAggregateId().toString()),
                any(String.class))).thenReturn(CompletableFuture.completedFuture(null));

        new OutboxPublisher(outbox, kafka, codec, properties).publishBatch();

        assertThat(event.getPublishedAt()).isNotNull();
        verify(kafka).send(
                eq("agentforge.run.commands.v1"),
                eq(event.getAggregateId().toString()),
                org.mockito.ArgumentMatchers.argThat(value -> {
                    EventEnvelope envelope = codec.decode(value);
                    return envelope.eventId().equals(event.getId())
                            && envelope.eventType().equals("RUN_QUEUED");
                }));
    }

    private AgentForgeProperties properties() {
        return new AgentForgeProperties(
                new AgentForgeProperties.Security(true),
                new AgentForgeProperties.Runtime(
                        URI.create("http://localhost:8081"),
                        Duration.ofMinutes(60),
                        3,
                        150,
                        200_000,
                        3),
                new AgentForgeProperties.Messaging(
                        true,
                        false,
                        "agentforge.run.commands.v1",
                        "agentforge.run.events.v1",
                        "agentforge-control-plane-v1"));
    }
}
