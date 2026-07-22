package io.agentforge.controlplane.web;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.agentforge.controlplane.service.TaskWorkflowService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/tasks")
public class TaskController {

    private final TaskWorkflowService workflow;

    public TaskController(TaskWorkflowService workflow) {
        this.workflow = workflow;
    }

    @GetMapping("/{taskId}/grill/messages")
    public List<ApiDtos.GrillMessageResponse> getConversation(@PathVariable UUID taskId) {
        return workflow.getConversation(taskId).stream().map(ApiDtos.GrillMessageResponse::from).toList();
    }

    @PostMapping("/{taskId}/grill/messages")
    public ApiDtos.GrillResultResponse addMessage(
            @PathVariable UUID taskId,
            @Valid @RequestBody ApiDtos.GrillMessageRequest request) {
        TaskWorkflowService.GrillResult result = workflow.addGrillMessage(taskId, request.content(), request.finalizeRun());
        return new ApiDtos.GrillResultResponse(
                ApiDtos.TaskResponse.from(result.task()),
                result.nextQuestion() == null ? null : ApiDtos.GrillMessageResponse.from(result.nextQuestion()),
                result.spec() == null ? null : ApiDtos.RequirementSpecResponse.from(result.spec()),
                result.run() == null ? null : ApiDtos.RunResponse.from(result.run()));
    }

    @GetMapping("/{taskId}/spec")
    public ApiDtos.RequirementSpecResponse getSpec(@PathVariable UUID taskId) {
        return ApiDtos.RequirementSpecResponse.from(workflow.getSpec(taskId));
    }
}
