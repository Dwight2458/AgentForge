package io.agentforge.controlplane.config;

import java.net.URI;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("agentforge")
public record AgentForgeProperties(
        Security security,
        Runtime runtime,
        Messaging messaging) {

    public record Security(boolean devAuth) {}

    public record Runtime(
            URI baseUrl,
            Duration runTimeout,
            int maxRepairRounds,
            int maxToolCalls,
            int maxTokens,
            int maxParallelSubagents) {}

    public record Messaging(boolean outboxEnabled) {}
}

