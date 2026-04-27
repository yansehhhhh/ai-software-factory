package com.aifactory;

import com.aifactory.dto.ClaudeRunResult;
import com.aifactory.dto.ResultView;
import com.aifactory.dto.WorkflowStatus;
import com.aifactory.service.ClaudeCodeService;
import com.aifactory.service.WorkflowService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
class WorkflowServiceIntegrationTests {

    @Autowired
    private WorkflowService workflowService;

    @MockBean
    private ClaudeCodeService claudeCodeService;

    @Test
    void shouldGenerateStructuredDesignArtifacts() throws Exception {
        when(claudeCodeService.runTask(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(new ClaudeRunResult("task-1", "session-1", "success", "# PRD", List.of(), "workspace/runs/task-1", "workspace/runs/task-1/project"))
                .thenReturn(new ClaudeRunResult("task-1", "session-1", "success", "# UI", List.of(), "workspace/runs/task-1", "workspace/runs/task-1/project"));
        when(claudeCodeService.runGenerate(anyString(), anyString(), anyString()))
                .thenReturn(new ClaudeRunResult("task-1", "session-1", "success", "generated", List.of(), "workspace/runs/task-1", "workspace/runs/task-1/project"));
        when(claudeCodeService.runFixTests(anyString(), anyString(), anyString()))
                .thenReturn(new ClaudeRunResult("task-1", "session-1", "success", "tests fixed", List.of(), "workspace/runs/task-1", "workspace/runs/task-1/project"));

        workflowService.start("做一个 AI 质检助手，支持上传日志文件，分析问题并生成报告");
        WorkflowStatus status = awaitStatus("success", Duration.ofSeconds(5));
        ResultView result = workflowService.result();

        assertEquals("已完成", status.currentStage());
        assertTrue(result.available());
        assertTrue(result.designAvailable());
        assertEquals("workspace/runs/task-1/project", result.projectUrl());
        assertTrue(result.prdMarkdown().contains("PRD"));
    }

    @Test
    void shouldExposeErrorWhenDesignStageFails() throws Exception {
        when(claudeCodeService.runTask(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(new ClaudeRunResult("task-2", "session-2", "success", "# PRD", List.of(), "workspace/runs/task-2", "workspace/runs/task-2/project"))
                .thenThrow(new IllegalStateException("runner failed"));

        workflowService.start("做一个失败演练系统");
        WorkflowStatus status = awaitStatus("error", Duration.ofSeconds(5));

        assertEquals("error", status.status());
        assertTrue(workflowService.logs().stream().anyMatch(item -> "error".equals(item.level())));
    }

    private WorkflowStatus awaitStatus(String expected, Duration timeout) throws Exception {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        WorkflowStatus status = workflowService.currentStatus();

        while (System.currentTimeMillis() < deadline) {
            status = workflowService.currentStatus();
            if (expected.equals(status.status())) {
                return status;
            }
            Thread.sleep(50L);
        }

        return status;
    }
}
