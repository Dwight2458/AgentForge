package io.agentforge.controlplane.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RunStatusTest {

    @Test
    void permitsRepairLoopAndHappyPath() {
        assertThat(RunStatus.QUEUED.canTransitionTo(RunStatus.PROVISIONING)).isTrue();
        assertThat(RunStatus.VERIFYING.canTransitionTo(RunStatus.RUNNING)).isTrue();
        assertThat(RunStatus.PACKAGING.canTransitionTo(RunStatus.SUCCEEDED)).isTrue();
    }

    @Test
    void terminalStatesCannotTransition() {
        assertThat(RunStatus.SUCCEEDED.canTransitionTo(RunStatus.RUNNING)).isFalse();
        assertThat(RunStatus.FAILED.canTransitionTo(RunStatus.PACKAGING)).isFalse();
        assertThat(RunStatus.CANCELLED.canTransitionTo(RunStatus.QUEUED)).isFalse();
    }
}
