package io.agentforge.controlplane.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.agentforge.controlplane.config.AgentForgeProperties;
import io.agentforge.controlplane.domain.GrillMessageEntity;
import io.agentforge.controlplane.domain.OutboxEventEntity;
import io.agentforge.controlplane.domain.ProjectEntity;
import io.agentforge.controlplane.domain.RequirementSpecEntity;
import io.agentforge.controlplane.domain.RunEntity;
import io.agentforge.controlplane.domain.TaskEntity;
import io.agentforge.controlplane.repository.GrillMessageRepository;
import io.agentforge.controlplane.repository.OutboxEventRepository;
import io.agentforge.controlplane.repository.ProjectRepository;
import io.agentforge.controlplane.repository.RequirementSpecRepository;
import io.agentforge.controlplane.repository.RunRepository;
import io.agentforge.controlplane.repository.TaskRepository;

@Service
public class TaskWorkflowService {

    private static final List<String> DEFAULT_QUESTIONS = List.of(
            "Which behavior and edge cases must the implementation support?",
            "Are there compatibility or file-scope constraints we must preserve?",
            "Which automated checks prove that the task is complete?");

    private final ProjectRepository projects;
    private final TaskRepository tasks;
    private final GrillMessageRepository messages;
    private final RequirementSpecRepository specs;
    private final RunRepository runs;
    private final OutboxEventRepository outbox;
    private final AgentForgeProperties properties;
    private final ObjectMapper objectMapper;

    public TaskWorkflowService(
            ProjectRepository projects,
            TaskRepository tasks,
            GrillMessageRepository messages,
            RequirementSpecRepository specs,
            RunRepository runs,
            OutboxEventRepository outbox,
            AgentForgeProperties properties,
            ObjectMapper objectMapper) {
        this.projects = projects;
        this.tasks = tasks;
        this.messages = messages;
        this.specs = specs;
        this.runs = runs;
        this.outbox = outbox;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ProjectEntity importProject(long installationId, String repositoryFullName, String cloneUrl, String defaultBranch) {
        if (projects.existsByRepositoryFullName(repositoryFullName)) {
            throw new IllegalArgumentException("Repository is already imported: " + repositoryFullName);
        }
        return projects.save(new ProjectEntity(installationId, repositoryFullName, cloneUrl, defaultBranch));
    }

    @Transactional
    public TaskEntity createTask(UUID projectId, String requestText) {
        projects.findById(projectId).orElseThrow(() -> new NotFoundException("Project not found: " + projectId));
        TaskEntity task = tasks.save(new TaskEntity(projectId, requestText));
        messages.save(new GrillMessageEntity(task.getId(), "assistant", DEFAULT_QUESTIONS.getFirst()));
        return task;
    }

    @Transactional
    public GrillResult addGrillMessage(UUID taskId, String content, boolean finalize) {
        TaskEntity task = tasks.findById(taskId).orElseThrow(() -> new NotFoundException("Task not found: " + taskId));
        if (task.getStatus() != io.agentforge.controlplane.domain.TaskStatus.GRILLING) {
            throw new IllegalStateException("Task is no longer accepting Grill messages");
        }

        messages.save(new GrillMessageEntity(taskId, "user", content));
        long answers = messages.countByTaskIdAndRole(taskId, "user");

        if (!finalize && answers < DEFAULT_QUESTIONS.size()) {
            GrillMessageEntity next = messages.save(new GrillMessageEntity(
                    taskId, "assistant", DEFAULT_QUESTIONS.get((int) answers)));
            return new GrillResult(task, next, null, null);
        }

        List<GrillMessageEntity> conversation = messages.findByTaskIdOrderByCreatedAtAsc(taskId);
        RequirementSpecEntity spec = specs.save(buildSpec(task, conversation));
        task.markSpecReady();

        AgentForgeProperties.Runtime budget = properties.runtime();
        RunEntity run = runs.save(new RunEntity(
                taskId,
                Math.toIntExact(budget.runTimeout().toMinutes()),
                budget.maxRepairRounds(),
                budget.maxToolCalls(),
                budget.maxTokens()));

        outbox.save(new OutboxEventEntity(
                "AgentRun",
                run.getId(),
                "RUN_QUEUED",
                toJson(new RunQueuedPayload(run.getId(), taskId, task.getProjectId()))));
        return new GrillResult(task, null, spec, run);
    }

    @Transactional(readOnly = true)
    public List<GrillMessageEntity> getConversation(UUID taskId) {
        if (!tasks.existsById(taskId)) {
            throw new NotFoundException("Task not found: " + taskId);
        }
        return messages.findByTaskIdOrderByCreatedAtAsc(taskId);
    }

    @Transactional(readOnly = true)
    public RequirementSpecEntity getSpec(UUID taskId) {
        return specs.findById(taskId).orElseThrow(() -> new NotFoundException("Requirement spec not found: " + taskId));
    }

    private RequirementSpecEntity buildSpec(TaskEntity task, List<GrillMessageEntity> conversation) {
        List<String> answers = conversation.stream()
                .filter(message -> message.getRole().equals("user"))
                .map(GrillMessageEntity::getContent)
                .toList();
        return new RequirementSpecEntity(
                task.getId(),
                task.getRequestText(),
                toJson(List.of("Requested behavior is implemented", "Existing behavior remains compatible")),
                toJson(answers),
                toJson(List.of("Run project tests", "Inspect Git diff", "Validate each acceptance criterion")));
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize workflow payload", exception);
        }
    }

    public record GrillResult(TaskEntity task, GrillMessageEntity nextQuestion, RequirementSpecEntity spec, RunEntity run) {}
    private record RunQueuedPayload(UUID runId, UUID taskId, UUID projectId) {}
}

