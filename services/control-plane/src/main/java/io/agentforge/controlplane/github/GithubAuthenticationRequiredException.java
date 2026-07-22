package io.agentforge.controlplane.github;

public class GithubAuthenticationRequiredException extends RuntimeException {
    public GithubAuthenticationRequiredException(String message) {
        super(message);
    }
}

