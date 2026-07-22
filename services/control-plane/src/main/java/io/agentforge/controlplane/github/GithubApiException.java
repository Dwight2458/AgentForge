package io.agentforge.controlplane.github;

public class GithubApiException extends RuntimeException {
    private final int githubStatus;

    public GithubApiException(int githubStatus, String message) {
        super(message);
        this.githubStatus = githubStatus;
    }

    public GithubApiException(String message, Throwable cause) {
        super(message, cause);
        this.githubStatus = 0;
    }

    public int githubStatus() {
        return githubStatus;
    }
}

