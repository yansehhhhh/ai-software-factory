package com.aifactory;

import com.aifactory.dto.ResultView;
import com.aifactory.dto.WorkflowStatus;
import com.aifactory.llm.LlmClient;
import com.aifactory.llm.LlmRequest;
import com.aifactory.llm.LlmResponse;
import com.aifactory.service.WorkflowService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
class WorkflowServiceIntegrationTests {

    @Autowired
    private WorkflowService workflowService;

    @MockBean
    private LlmClient llmClient;

    @Test
    void shouldGenerateStructuredDesignArtifacts() throws Exception {
        when(llmClient.complete(any(LlmRequest.class)))
                .thenReturn(new LlmResponse("gpt-4.1", "# PRD"))
                .thenReturn(new LlmResponse("qwen-max", "# UI"));

        workflowService.start("做一个 AI 质检助手，支持上传日志文件，分析问题并生成报告");
        WorkflowStatus status = awaitStatus("success", Duration.ofSeconds(5));
        ResultView result = workflowService.result();

        assertEquals("已完成", status.currentStage());
        assertTrue(result.designAvailable());
        assertTrue(result.prdMarkdown().contains("PRD"));
        assertFalse(result.pageSpecs().isEmpty());
        assertFalse(result.componentSpecs().isEmpty());
        assertFalse(result.userFlowSpecs().isEmpty());
    }

    @Test
    void shouldExposeErrorWhenDesignStageFails() throws Exception {
        when(llmClient.complete(any(LlmRequest.class)))
                .thenReturn(new LlmResponse("gpt-4.1", "# PRD"));

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
