package io.agentforge.controlplane.github;

public class GithubConfigurationException extends RuntimeException {
    public GithubConfigurationException(String message) {
        super(message);
    }

    public GithubConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}

