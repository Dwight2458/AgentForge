package io.agentforge.controlplane.web;

import java.util.List;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import io.agentforge.controlplane.domain.RunStatus;
import io.agentforge.controlplane.repository.RunRepository;
import io.agentforge.controlplane.service.NotFoundException;
import io.agentforge.controlplane.service.RunEventService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/runs")
public class RunController {

    private final RunRepository runs;
    private final RunEventService events;

    public RunController(RunRepository runs, RunEventService events) {
        this.runs = runs;
        this.events = events;
    }

    @GetMapping
    public List<ApiDtos.RunResponse> list() {
        return runs.findTop20ByOrderByCreatedAtDesc().stream().map(ApiDtos.RunResponse::from).toList();
    }

    @GetMapping("/{runId}")
    public ApiDtos.RunResponse get(@PathVariable UUID runId) {
        return ApiDtos.RunResponse.from(runs.findById(runId)
                .orElseThrow(() -> new NotFoundException("Run not found: " + runId)));
    }

    @GetMapping(path = "/{runId}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter events(
            @PathVariable UUID runId,
            @RequestHeader(name = "Last-Event-ID", defaultValue = "0") long lastEventId) {
        return events.subscribe(runId, lastEventId);
    }

    @PostMapping("/{runId}/events")
    public void appendEvent(
            @PathVariable UUID runId,
            @Valid @RequestBody ApiDtos.AppendRunEventRequest request) {
        events.append(runId, request.type(), request.agent(), request.summary(), request.payload());
    }

    @PostMapping("/{runId}/cancel")
    public ApiDtos.RunResponse cancel(@PathVariable UUID runId) {
        var run = runs.findById(runId).orElseThrow(() -> new NotFoundException("Run not found: " + runId));
        run.transitionTo(RunStatus.CANCELLED);
        runs.save(run);
        events.append(runId, "RUN_CANCELLED", "ControlPlane", "Run cancelled by user", "{}");
        return ApiDtos.RunResponse.from(run);
    }
}

