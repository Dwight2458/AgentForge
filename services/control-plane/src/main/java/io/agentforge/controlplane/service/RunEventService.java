package io.agentforge.controlplane.service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import io.agentforge.controlplane.domain.RunEventEntity;
import io.agentforge.controlplane.repository.RunEventRepository;
import io.agentforge.controlplane.repository.RunRepository;

@Service
public class RunEventService {

    private final RunRepository runs;
    private final RunEventRepository events;
    private final Map<UUID, CopyOnWriteArrayList<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public RunEventService(RunRepository runs, RunEventRepository events) {
        this.runs = runs;
        this.events = events;
    }

    @Transactional
    public synchronized RunEventEntity append(UUID runId, String type, String agent, String summary, String payload) {
        runs.findById(runId).orElseThrow(() -> new NotFoundException("Run not found: " + runId));
        long nextSequence = events.findTopByRunIdOrderBySequenceDesc(runId)
                .map(event -> event.getSequence() + 1)
                .orElse(1L);
        RunEventEntity saved = events.save(new RunEventEntity(runId, nextSequence, type, agent, summary, payload));
        publishAfterCommit(saved);
        return saved;
    }

    @Transactional(readOnly = true)
    public SseEmitter subscribe(UUID runId, long afterSequence) {
        runs.findById(runId).orElseThrow(() -> new NotFoundException("Run not found: " + runId));
        SseEmitter emitter = new SseEmitter(0L);
        emitters.computeIfAbsent(runId, ignored -> new CopyOnWriteArrayList<>()).add(emitter);
        emitter.onCompletion(() -> remove(runId, emitter));
        emitter.onTimeout(() -> remove(runId, emitter));
        emitter.onError(error -> remove(runId, emitter));

        events.findByRunIdAndSequenceGreaterThanOrderBySequenceAsc(runId, afterSequence)
                .forEach(event -> send(emitter, event));
        return emitter;
    }

    private void publish(RunEventEntity event) {
        List<SseEmitter> runEmitters = emitters.getOrDefault(event.getRunId(), new CopyOnWriteArrayList<>());
        runEmitters.forEach(emitter -> send(emitter, event));
    }

    private void publishAfterCommit(RunEventEntity event) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            publish(event);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                publish(event);
            }
        });
    }

    private void send(SseEmitter emitter, RunEventEntity event) {
        try {
            emitter.send(SseEmitter.event()
                    .id(Long.toString(event.getSequence()))
                    .name(event.getType())
                    .data(event));
        } catch (IOException exception) {
            emitter.completeWithError(exception);
        }
    }

    private void remove(UUID runId, SseEmitter emitter) {
        emitters.getOrDefault(runId, new CopyOnWriteArrayList<>()).remove(emitter);
    }
}
