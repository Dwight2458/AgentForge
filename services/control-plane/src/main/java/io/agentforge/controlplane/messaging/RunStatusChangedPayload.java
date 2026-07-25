package io.agentforge.controlplane.messaging;

import com.fasterxml.jackson.databind.JsonNode;

import io.agentforge.controlplane.domain.RunStatus;

public record RunStatusChangedPayload(
        RunStatus status,
        String agent,
        String summary,
        JsonNode details) {

    public RunStatusChangedPayload {
        if (status == null) {
            throw new InvalidEventEnvelopeException("Run status is required");
        }
        if (summary == null || summary.isBlank()) {
            throw new InvalidEventEnvelopeException("Run event summary is required");
        }
    }
}
