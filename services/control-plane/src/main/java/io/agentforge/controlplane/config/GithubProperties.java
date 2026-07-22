package io.agentforge.controlplane.config;

import java.net.URI;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("agentforge.github")
public record GithubProperties(
        URI apiBaseUrl,
        String apiVersion,
        String appSlug,
        long appId,
        String privateKey) {

    public boolean appCredentialsConfigured() {
        return appId > 0 && privateKey != null && !privateKey.isBlank();
    }

    public String installUrl() {
        return appSlug == null || appSlug.isBlank()
                ? null
                : "https://github.com/apps/" + appSlug + "/installations/new";
    }
}

