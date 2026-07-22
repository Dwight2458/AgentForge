package io.agentforge.controlplane.web;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.agentforge.controlplane.domain.GrillMessageEntity;
import io.agentforge.controlplane.domain.ProjectEntity;
import io.agentforge.controlplane.domain.RequirementSpecEntity;
import io.agentforge.controlplane.domain.RunEntity;
import io.agentforge.controlplane.domain.TaskEntity;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class ApiDtos {

    private ApiDtos() {}

    public record ImportProjectRequest(
            @Min(1) long githubInstallationId,
            @NotBlank @Pattern(regexp = "[^/]+/[^/]+") String repositoryFullName) {}

    public record ProjectResponse(
            UUID id,
            long githubInstallationId,
            String repositoryFullName,
            String defaultBranch,
            Instant createdAt) {
        public static ProjectResponse from(ProjectEntity project) {
            return new ProjectResponse(
                    project.getId(),
                    project.getGithubInstallationId(),
                    project.getRepositoryFullName(),
                    project.getDefaultBranch(),
                    project.getCreatedAt());
        }
    }

    public record CreateTaskRequest(@NotBlank @Size(max = 8_000) String request) {}

    public record TaskResponse(UUID id, UUID projectId, String request, String status, Instant createdAt) {
        public static TaskResponse from(TaskEntity task) {
            return new TaskResponse(
                    task.getId(), task.getProjectId(), task.getRequestText(), task.getStatus().name(), task.getCreatedAt());
        }
    }

    public record GrillMessageRequest(
            @NotBlank @Size(max = 8_000) String content,
            @JsonProperty("finalize") boolean finalizeRun) {}

    public record GrillMessageResponse(UUID id, String role, String content, Instant createdAt) {
        public static GrillMessageResponse from(GrillMessageEntity message) {
            return new GrillMessageResponse(message.getId(), message.getRole(), message.getContent(), message.getCreatedAt());
        }
    }

    public record GrillResultResponse(
            TaskResponse task,
            GrillMessageResponse nextQuestion,
            RequirementSpecResponse spec,
            RunResponse run) {}

    public record RequirementSpecResponse(
            UUID taskId,
            String goal,
            String acceptanceCriteria,
            String constraints,
            String testPlan,
            Instant createdAt) {
        public static RequirementSpecResponse from(RequirementSpecEntity spec) {
            return new RequirementSpecResponse(
                    spec.getTaskId(),
                    spec.getGoal(),
                    spec.getAcceptanceCriteria(),
                    spec.getConstraints(),
                    spec.getTestPlan(),
                    spec.getCreatedAt());
        }
    }

    public record RunResponse(
            UUID id,
            UUID taskId,
            String status,
            int maxMinutes,
            int maxRepairRounds,
            int maxToolCalls,
            int maxTokens,
            Instant createdAt) {
        public static RunResponse from(RunEntity run) {
            return new RunResponse(
                    run.getId(), run.getTaskId(), run.getStatus().name(), run.getMaxMinutes(),
                    run.getMaxRepairRounds(), run.getMaxToolCalls(), run.getMaxTokens(), run.getCreatedAt());
        }
    }

    public record AppendRunEventRequest(
            @NotBlank String type,
            String agent,
            @NotBlank @Size(max = 2_000) String summary,
            @NotNull String payload) {}

    public record ApiError(String code, String message, List<String> details, Instant timestamp) {}
}
