package io.agentforge.controlplane.web;

import static io.agentforge.controlplane.web.ApiDtos.ProjectResponse;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.agentforge.controlplane.repository.ProjectRepository;
import io.agentforge.controlplane.github.GithubProjectService;
import io.agentforge.controlplane.service.TaskWorkflowService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/projects")
public class ProjectController {

    private final ProjectRepository projects;
    private final TaskWorkflowService workflow;
    private final GithubProjectService github;

    public ProjectController(ProjectRepository projects, TaskWorkflowService workflow, GithubProjectService github) {
        this.projects = projects;
        this.workflow = workflow;
        this.github = github;
    }

    @GetMapping
    public List<ProjectResponse> list() {
        return projects.findAll().stream().map(ProjectResponse::from).toList();
    }

    @PostMapping("/import")
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectResponse importProject(
            Authentication authentication,
            @Valid @RequestBody ApiDtos.ImportProjectRequest request) {
        return ProjectResponse.from(github.importRepository(
                authentication, request.githubInstallationId(), request.repositoryFullName()));
    }

    @PostMapping("/{projectId}/tasks")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiDtos.TaskResponse createTask(
            @org.springframework.web.bind.annotation.PathVariable UUID projectId,
            @Valid @RequestBody ApiDtos.CreateTaskRequest request) {
        return ApiDtos.TaskResponse.from(workflow.createTask(projectId, request.request()));
    }
}
