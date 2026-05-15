package com.aifactory.controller;

import com.aifactory.dto.LogEntry;
import com.aifactory.dto.OpenSpecActionRequest;
import com.aifactory.dto.RecoverProjectRequest;
import com.aifactory.dto.ResultView;
import com.aifactory.dto.ProjectSummary;
import com.aifactory.dto.StageRevisionRequest;
import com.aifactory.dto.StartWorkflowRequest;
import com.aifactory.dto.WorkflowStatus;
import com.aifactory.service.WorkflowService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class WorkflowController {

    private final WorkflowService workflowService;

    public WorkflowController(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    @GetMapping("/workflow/status")
    public WorkflowStatus status() {
        return workflowService.currentStatus();
    }

    @PostMapping("/workflow/start")
    public WorkflowStatus start(@Valid @RequestBody StartWorkflowRequest request) {
        return workflowService.start(request.requirement());
    }

    @PostMapping("/workflow/recover/ui")
    public WorkflowStatus recoverUiStage(@RequestBody Map<String, String> request) {
        return workflowService.recoverUiStage(request == null ? null : request.get("projectName"));
    }

    @GetMapping("/workflow/projects")
    public List<ProjectSummary> projects() {
        return workflowService.projects();
    }

    @PostMapping("/workflow/recover/project")
    public WorkflowStatus recoverProject(@RequestBody RecoverProjectRequest request) {
        return workflowService.recoverProject(request == null ? null : request.projectName());
    }

    @PostMapping("/workflow/review/approve")
    public WorkflowStatus approveCurrentStage() {
        workflowService.approveCurrentStage();
        return workflowService.continueRecoveredWorkflow();
    }

    @PostMapping("/workflow/retry")
    public WorkflowStatus retryFailedStage() {
        return workflowService.retryFailedStage();
    }

    @PostMapping("/workflow/review/revise")
    public WorkflowStatus submitStageRevision(@RequestBody StageRevisionRequest request) {
        return workflowService.submitStageRevision(request == null ? "" : request.feedback());
    }

    @PostMapping("/workflow/review/openspec/{action}")
    public WorkflowStatus runOpenSpecAction(@org.springframework.web.bind.annotation.PathVariable String action, @RequestBody(required = false) OpenSpecActionRequest request) {
        return workflowService.runOpenSpecAction(action, request);
    }

    @GetMapping("/logs")
    public List<LogEntry> logs() {
        return workflowService.logs();
    }

    @DeleteMapping("/logs")
    public ResponseEntity<Void> clearLogs() {
        workflowService.clearLogs();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/result")
    public ResultView result() {
        return workflowService.result();
    }
}
