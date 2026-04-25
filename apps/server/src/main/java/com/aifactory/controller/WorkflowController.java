package com.aifactory.controller;

import com.aifactory.dto.LogEntry;
import com.aifactory.dto.ResultView;
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
