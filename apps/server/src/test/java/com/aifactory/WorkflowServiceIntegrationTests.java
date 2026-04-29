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
                .thenReturn(new ClaudeRunResult("task-1", "session-1", "success", "# PRD", List.of(
                        "docs/AI质检助手/产品设计/01-变更记录/版本说明.md",
                        "docs/AI质检助手/产品设计/02-产品需求文档/PRD.md",
                        "docs/AI质检助手/产品设计/02-产品需求文档/PRD.docx",
                        "docs/AI质检助手/产品设计/03-UI设计规范/UI-Design-Spec.md",
                        "docs/AI质检助手/产品设计/04-流程图/业务流程图.puml",
                        "docs/AI质检助手/产品设计/04-流程图/信息架构图.puml",
                        "docs/AI质检助手/产品设计/04-流程图/页面流转图.puml",
                        "docs/AI质检助手/产品设计/04-流程图/页面流转图.svg",
                        "docs/AI质检助手/产品设计/05-附录/术语表.md"
                ), "workspace/runtime/task-1", "workspace/runtime/task-1/project", "docs/AI质检助手/产品设计", "AI质检助手"))
                .thenReturn(new ClaudeRunResult("task-1", "session-1", "success", "# UI", List.of(
                        "docs/AI质检助手/产品设计/03-UI设计规范/UI-Design-Spec.md",
                        "docs/AI质检助手/UI原型/设计稿/index.html",
                        "docs/AI质检助手/UI原型/组件库/组件清单.md",
                        "docs/AI质检助手/UI原型/交互原型/交互说明.md",
                        "docs/AI质检助手/UI原型/移动端适配/响应式断点参考.md"
                ), "workspace/runtime/task-1", "workspace/runtime/task-1/project", "docs/AI质检助手/产品设计", "AI质检助手"))
                .thenReturn(new ClaudeRunResult("task-1", "session-1", "success", "# 架构设计", List.of(
                        "docs/AI质检助手/架构设计/01-系统架构/系统架构设计.md",
                        "docs/AI质检助手/架构设计/01-系统架构/系统架构图.puml",
                        "docs/AI质检助手/架构设计/01-系统架构/系统架构图.svg",
                        "docs/AI质检助手/架构设计/03-部署架构/部署架构.md",
                        "docs/AI质检助手/架构设计/04-技术选型/技术选型.md"
                ), "workspace/runtime/task-1", "workspace/runtime/task-1/project", "docs/AI质检助手/产品设计", "AI质检助手"))
                .thenReturn(new ClaudeRunResult("task-1", "session-1", "success", "# 接口设计", List.of(
                        "docs/AI质检助手/架构设计/02-接口设计/接口清单.md",
                        "docs/AI质检助手/架构设计/02-接口设计/接口定义/openapi.yaml"
                ), "workspace/runtime/task-1", "workspace/runtime/task-1/project", "docs/AI质检助手/产品设计", "AI质检助手"))
                .thenReturn(new ClaudeRunResult("task-1", "session-1", "success", "# 数据库设计", List.of(
                        "docs/AI质检助手/数据库设计/数据字典/数据字典.md",
                        "docs/AI质检助手/数据库设计/建表脚本/schema.sql",
                        "docs/AI质检助手/数据库设计/数据迁移脚本/migration.sql"
                ), "workspace/runtime/task-1", "workspace/runtime/task-1/project", "docs/AI质检助手/产品设计", "AI质检助手"));
        when(claudeCodeService.runGenerate(anyString(), anyString(), anyString()))
                .thenReturn(new ClaudeRunResult("task-1", "session-1", "success", "generated", List.of(), "workspace/runtime/task-1", "workspace/runtime/task-1/project", "docs/AI质检助手/产品设计", "AI质检助手"));
        when(claudeCodeService.runBackend(anyString(), anyString(), anyString()))
                .thenReturn(new ClaudeRunResult("task-1", "session-1", "success", "backend", List.of(
                        "workspace/runtime/task-1/project/backend",
                        "workspace/runtime/task-1/project/backend/README.md",
                        "workspace/runtime/task-1/project/backend/backend-summary.md"
                ), "workspace/runtime/task-1", "workspace/runtime/task-1/project", "docs/AI质检助手/产品设计", "AI质检助手"));
        when(claudeCodeService.runTestCases(anyString(), anyString(), anyString()))
                .thenReturn(new ClaudeRunResult("task-1", "session-1", "success", "test cases", List.of(
                        "workspace/runtime/task-1/project/tests/test-cases",
                        "workspace/runtime/task-1/project/tests/e2e",
                        "workspace/runtime/task-1/project/tests/test-cases/test-case-summary.md"
                ), "workspace/runtime/task-1", "workspace/runtime/task-1/project", "docs/AI质检助手/产品设计", "AI质检助手"));
        when(claudeCodeService.runPlaywright(anyString(), anyString(), anyString()))
                .thenReturn(new ClaudeRunResult("task-1", "session-1", "success", "playwright", List.of(
                        "workspace/runtime/task-1/project/test-report.md",
                        "workspace/runtime/task-1/project/playwright-report",
                        "workspace/runtime/task-1/project/test-results",
                        "workspace/runtime/task-1/project/tests/e2e"
                ), "workspace/runtime/task-1", "workspace/runtime/task-1/project", "docs/AI质检助手/产品设计", "AI质检助手"));
        when(claudeCodeService.runFixTests(anyString(), anyString(), anyString()))
                .thenReturn(new ClaudeRunResult("task-1", "session-1", "success", "tests fixed", List.of(), "workspace/runtime/task-1", "workspace/runtime/task-1/project", "docs/AI质检助手/产品设计", "AI质检助手"));

        workflowService.start("做一个 AI 质检助手，支持上传日志文件，分析问题并生成报告");
        WorkflowStatus status = awaitStatus("success", Duration.ofSeconds(5));
        ResultView result = workflowService.result();

        assertEquals("已完成", status.currentStage());
        assertEquals(14, status.steps().size());
        assertTrue(status.steps().stream().anyMatch(step -> "product-design-artifacts".equals(step.key())));
        assertTrue(status.steps().stream().anyMatch(step -> "ui-design".equals(step.key())));
        assertTrue(status.steps().stream().anyMatch(step -> "architecture-design".equals(step.key())));
        assertTrue(status.steps().stream().anyMatch(step -> "api-design".equals(step.key())));
        assertTrue(status.steps().stream().anyMatch(step -> "database-design".equals(step.key())));
        assertTrue(status.steps().stream().anyMatch(step -> "frontend-development".equals(step.key())));
        assertTrue(status.steps().stream().anyMatch(step -> "backend-development".equals(step.key())));
        assertTrue(status.steps().stream().anyMatch(step -> "test-case-generation".equals(step.key())));
        assertTrue(status.steps().stream().anyMatch(step -> "playwright-execution".equals(step.key())));
        assertTrue(status.steps().stream().anyMatch(step -> "delivery".equals(step.key())));
        assertTrue(result.available());
        assertTrue(result.designAvailable());
        assertTrue(result.artifacts().stream().anyMatch(artifact -> "业务流程图".equals(artifact.name()) && "plantuml".equals(artifact.type())));
        assertTrue(result.artifacts().stream().anyMatch(artifact -> "信息架构图".equals(artifact.name()) && "plantuml".equals(artifact.type())));
        assertTrue(result.artifacts().stream().anyMatch(artifact -> "页面流转图".equals(artifact.name()) && "plantuml".equals(artifact.type())));
        assertTrue(result.artifacts().stream().anyMatch(artifact -> "页面流转图预览".equals(artifact.name()) && "image".equals(artifact.type())));
        assertTrue(result.artifacts().stream().anyMatch(artifact -> "术语表".equals(artifact.name())));
        assertTrue(result.artifacts().stream().anyMatch(artifact -> "ui-prototype".equals(artifact.stage()) && "HTML 原型".equals(artifact.name())));
        assertTrue(result.artifacts().stream().anyMatch(artifact -> "ui-prototype".equals(artifact.stage()) && "组件清单".equals(artifact.name())));
        assertTrue(result.artifacts().stream().anyMatch(artifact -> "ui-prototype".equals(artifact.stage()) && "交互说明".equals(artifact.name())));
        assertTrue(result.artifacts().stream().anyMatch(artifact -> "ui-prototype".equals(artifact.stage()) && "响应式断点参考".equals(artifact.name())));
        assertTrue(result.artifacts().stream().anyMatch(artifact -> "architecture".equals(artifact.stage()) && "系统架构设计".equals(artifact.name())));
        assertTrue(result.artifacts().stream().anyMatch(artifact -> "architecture".equals(artifact.stage()) && "系统架构图".equals(artifact.name()) && "plantuml".equals(artifact.type())));
        assertTrue(result.artifacts().stream().anyMatch(artifact -> "architecture".equals(artifact.stage()) && "系统架构图预览".equals(artifact.name()) && "image".equals(artifact.type())));
        assertTrue(result.artifacts().stream().anyMatch(artifact -> "architecture".equals(artifact.stage()) && "部署架构".equals(artifact.name())));
        assertTrue(result.artifacts().stream().anyMatch(artifact -> "architecture".equals(artifact.stage()) && "技术选型".equals(artifact.name())));
        assertTrue(result.artifacts().stream().anyMatch(artifact -> "architecture".equals(artifact.stage()) && "接口清单".equals(artifact.name())));
        assertTrue(result.artifacts().stream().anyMatch(artifact -> "architecture".equals(artifact.stage()) && "OpenAPI 定义".equals(artifact.name()) && "yaml".equals(artifact.type())));
        assertTrue(result.artifacts().stream().anyMatch(artifact -> "database".equals(artifact.stage()) && "数据字典".equals(artifact.name())));
        assertTrue(result.artifacts().stream().anyMatch(artifact -> "database".equals(artifact.stage()) && "建表脚本".equals(artifact.name()) && "sql".equals(artifact.type())));
        assertTrue(result.artifacts().stream().anyMatch(artifact -> "database".equals(artifact.stage()) && "数据迁移脚本".equals(artifact.name()) && "sql".equals(artifact.type())));
        assertTrue(result.artifacts().stream().anyMatch(artifact -> "backend-development".equals(artifact.stage()) && "后端工程".equals(artifact.name())));
        assertTrue(result.artifacts().stream().anyMatch(artifact -> "test-case-generation".equals(artifact.stage()) && "测试用例摘要".equals(artifact.name())));
        assertTrue(result.artifacts().stream().anyMatch(artifact -> "playwright-execution".equals(artifact.stage()) && "测试报告".equals(artifact.name())));
        assertEquals("/api/artifacts?path=workspace%2Fruntime%2Ftask-1%2Fproject%2Ftest-report.md", result.reportUrl());
        assertEquals("/api/artifacts?path=workspace%2Fruntime%2Ftask-1%2Fproject%2Fdist%2Findex.html", result.projectUrl());
        assertTrue(result.prdMarkdown().contains("PRD"));
    }

    @Test
    void shouldExposeErrorWhenDesignStageFails() throws Exception {
        when(claudeCodeService.runTask(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(new ClaudeRunResult("task-2", "session-2", "success", "# PRD", List.of(), "workspace/runtime/task-2", "workspace/runtime/task-2/project", "docs/失败演练系统/产品设计", "失败演练系统"))
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
