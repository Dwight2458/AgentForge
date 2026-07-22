package io.agentforge.controlplane.domain;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public enum RunStatus {
    QUEUED,
    PROVISIONING,
    RUNNING,
    VERIFYING,
    PACKAGING,
    SUCCEEDED,
    FAILED,
    TIMEOUT,
    CANCELLED;

    private static final Map<RunStatus, Set<RunStatus>> TRANSITIONS = Map.of(
            QUEUED, EnumSet.of(PROVISIONING, CANCELLED, FAILED),
            PROVISIONING, EnumSet.of(RUNNING, CANCELLED, FAILED, TIMEOUT),
            RUNNING, EnumSet.of(VERIFYING, CANCELLED, FAILED, TIMEOUT),
            VERIFYING, EnumSet.of(RUNNING, PACKAGING, CANCELLED, FAILED, TIMEOUT),
            PACKAGING, EnumSet.of(SUCCEEDED, FAILED, TIMEOUT),
            SUCCEEDED, EnumSet.noneOf(RunStatus.class),
            FAILED, EnumSet.noneOf(RunStatus.class),
            TIMEOUT, EnumSet.noneOf(RunStatus.class),
            CANCELLED, EnumSet.noneOf(RunStatus.class));

    public boolean canTransitionTo(RunStatus target) {
        return TRANSITIONS.get(this).contains(target);
    }
}

