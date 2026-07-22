package io.agentforge.controlplane.github;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import io.agentforge.controlplane.domain.ProjectEntity;
import io.agentforge.controlplane.github.GithubApiClient.Installation;
import io.agentforge.controlplane.github.GithubApiClient.Repository;
import io.agentforge.controlplane.service.TaskWorkflowService;

@Service
public class GithubProjectService {

    private final GithubAuthorizedClientTokenResolver tokens;
    private final GithubApiClient github;
    private final TaskWorkflowService workflow;

    public GithubProjectService(
            GithubAuthorizedClientTokenResolver tokens,
            GithubApiClient github,
            TaskWorkflowService workflow) {
        this.tokens = tokens;
        this.github = github;
        this.workflow = workflow;
    }

    public List<Installation> listInstallations(Authentication authentication) {
        return github.listInstallations(tokens.resolve(authentication));
    }

    public List<Repository> listRepositories(Authentication authentication, long installationId) {
        return github.listRepositories(tokens.resolve(authentication), installationId);
    }

    public ProjectEntity importRepository(
            Authentication authentication,
            long installationId,
            String repositoryFullName) {
        Repository repository = listRepositories(authentication, installationId).stream()
                .filter(candidate -> candidate.fullName().equalsIgnoreCase(repositoryFullName))
                .findFirst()
                .orElseThrow(() -> new GithubAuthenticationRequiredException(
                        "The selected GitHub installation cannot access " + repositoryFullName));
        return workflow.importProject(
                installationId,
                repository.fullName(),
                repository.cloneUrl(),
                repository.defaultBranch());
    }

    public GithubApiClient.InstallationToken createRunToken(long installationId, String repositoryFullName) {
        return github.createInstallationToken(installationId, repositoryFullName);
    }
}

