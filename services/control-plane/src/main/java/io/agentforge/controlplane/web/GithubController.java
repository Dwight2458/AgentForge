package io.agentforge.controlplane.web;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.agentforge.controlplane.github.GithubApiClient.Installation;
import io.agentforge.controlplane.github.GithubApiClient.Repository;
import io.agentforge.controlplane.github.GithubProjectService;

@RestController
@RequestMapping("/api/v1/github")
public class GithubController {

    private final GithubProjectService github;

    public GithubController(GithubProjectService github) {
        this.github = github;
    }

    @GetMapping("/installations")
    public List<Installation> installations(Authentication authentication) {
        return github.listInstallations(authentication);
    }

    @GetMapping("/installations/{installationId}/repositories")
    public List<Repository> repositories(
            Authentication authentication,
            @PathVariable long installationId) {
        return github.listRepositories(authentication, installationId);
    }
}

