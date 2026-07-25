package io.agentforge.controlplane.service;

import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import io.agentforge.controlplane.config.AgentForgeProperties;
import io.agentforge.controlplane.domain.OutboxEventEntity;
import io.agentforge.controlplane.messaging.EventEnvelopeCodec;
import io.agentforge.controlplane.repository.OutboxEventRepository;

@Component
@ConditionalOnProperty(name = "agentforge.messaging.outbox-enabled", havingValue = "true")
public class OutboxPublisher {

    private final OutboxEventRepository outbox;
    private final KafkaTemplate<String, String> kafka;
    private final EventEnvelopeCodec envelopes;
    private final AgentForgeProperties properties;

    public OutboxPublisher(
            OutboxEventRepository outbox,
            KafkaTemplate<String, String> kafka,
            EventEnvelopeCodec envelopes,
            AgentForgeProperties properties) {
        this.outbox = outbox;
        this.kafka = kafka;
        this.envelopes = envelopes;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${agentforge.messaging.outbox-delay:500ms}")
    @Transactional
    public void publishBatch() {
        List<OutboxEventEntity> batch = outbox.findByPublishedAtIsNullOrderByOccurredAtAsc(PageRequest.of(0, 50));
        for (OutboxEventEntity event : batch) {
            kafka.send(
                    properties.messaging().runCommandsTopic(),
                    event.getAggregateId().toString(),
                    envelopes.encode(event)).join();
            event.markPublished();
        }
    }
}
