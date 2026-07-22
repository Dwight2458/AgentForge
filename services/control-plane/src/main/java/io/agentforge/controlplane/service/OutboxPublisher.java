package io.agentforge.controlplane.service;

import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import io.agentforge.controlplane.domain.OutboxEventEntity;
import io.agentforge.controlplane.repository.OutboxEventRepository;

@Component
@ConditionalOnProperty(name = "agentforge.messaging.outbox-enabled", havingValue = "true")
public class OutboxPublisher {

    private final OutboxEventRepository outbox;
    private final KafkaTemplate<String, String> kafka;

    public OutboxPublisher(OutboxEventRepository outbox, KafkaTemplate<String, String> kafka) {
        this.outbox = outbox;
        this.kafka = kafka;
    }

    @Scheduled(fixedDelayString = "${agentforge.messaging.outbox-delay:500ms}")
    @Transactional
    public void publishBatch() {
        List<OutboxEventEntity> batch = outbox.findByPublishedAtIsNullOrderByOccurredAtAsc(PageRequest.of(0, 50));
        for (OutboxEventEntity event : batch) {
            kafka.send("agentforge.run.commands.v1", event.getAggregateId().toString(), event.getPayload()).join();
            event.markPublished();
        }
    }
}
