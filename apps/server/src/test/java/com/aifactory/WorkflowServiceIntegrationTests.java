package com.aifactory;

import com.aifactory.dto.ClaudeRunResult;
import com.aifactory.dto.OpenSpecRunResult;
import com.aifactory.dto.ResultView;
import com.aifactory.dto.StageRevisionContext;
import com.aifactory.dto.WorkflowStatus;
import com.aifactory.service.ClaudeCodeService;
import com.aifactory.service.WorkflowService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
class WorkflowServiceIntegrationTests {

    @Autowired
    private WorkflowService workflowService;

    @MockBean
    private ClaudeCodeService claudeCodeService;

    @Test
    void shouldGenerateStructuredDesignArtifacts() throws Exception {
        when(claudeCodeService.runTask(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(new ClaudeRunResult("task-1", "session-1", "success", "# PRD", List.of(
                        "docs/AI质检助手/产品设计/01-变更记录/版本说明.md",
                        "docs/AI质检助手/产品设计/02-产品需求文档/PRD.md",
                        "docs/AI质检助手/产品设计/03-UI设计规范/UI-Design-Spec.md",
                        "docs/AI质检助手/产品设计/04-流程图/业务流程图.puml",
                        "docs/AI质检助手/产品设计/04-流程图/信息架构图.puml",
                        "docs/AI质检助手/产品设计/04-流程图/页面流转图.puml",
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
        when(claudeCodeService.runGenerate(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(new ClaudeRunResult("task-1", "session-1", "success", "generated", List.of(
                        "generated/AI/frontend/package.json",
                        "generated/AI/frontend/src",
                        "generated/AI/frontend/README.md",
                        "generated/AI/frontend/dist/index.html"
                ), "workspace/runtime/task-1", "generated/AI", "docs/AI质检助手/产品设计", "AI质检助手"));
        when(claudeCodeService.runBackend(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(new ClaudeRunResult("task-1", "session-1", "success", "backend", List.of(
                        "generated/AI/backend",
                        "generated/AI/backend/README.md",
                        "generated/AI/backend/backend-summary.md"
                ), "workspace/runtime/task-1", "generated/AI", "docs/AI质检助手/产品设计", "AI质检助手"));
        when(claudeCodeService.runTestCases(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(new ClaudeRunResult("task-1", "session-1", "success", "test cases", List.of(
                        "docs/AI质检助手/测试/测试用例/test-case-summary.md",
                        "docs/AI质检助手/测试/e2e"
                ), "workspace/runtime/task-1", "generated/AI", "docs/AI质检助手/产品设计", "AI质检助手"));
        when(claudeCodeService.runPlaywright(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(new ClaudeRunResult("task-1", "session-1", "success", "dev integration", List.of(
                        "docs/AI质检助手/测试/测试报告/dev-integration-report.md"
                ), "workspace/runtime/task-1", "generated/AI", "docs/AI质检助手/产品设计", "AI质检助手"))
                .thenReturn(new ClaudeRunResult("task-1", "session-1", "success", "e2e acceptance", List.of(
                        "docs/AI质检助手/测试/测试报告/test-report.md",
                        "docs/AI质检助手/测试/测试报告/playwright-report",
                        "docs/AI质检助手/测试/测试报告/test-results",
                        "docs/AI质检助手/测试/e2e"
                ), "workspace/runtime/task-1", "generated/AI", "docs/AI质检助手/产品设计", "AI质检助手"));
        when(claudeCodeService.runFixTests(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(new ClaudeRunResult("task-1", "session-1", "success", "tests fixed", List.of(), "workspace/runtime/task-1", "generated/AI", "docs/AI质检助手/产品设计", "AI质检助手"));
        when(claudeCodeService.runOpenSpecAction(anyString(), any()))
                .thenReturn(new OpenSpecRunResult("task-1", "propose", "revise-product-design-artifacts-task-1", "success", "OpenSpec propose 已完成", "2026-04-29T00:00:00Z"));

        workflowService.start("做一个 AI 质检助手，支持上传日志文件，分析问题并生成报告");
        WorkflowStatus firstReview = awaitStatus("awaiting_review", Duration.ofSeconds(5));
        assertNotNull(firstReview.review());
        assertEquals("product-design-artifacts", firstReview.review().stageKey());
        assertTrue(firstReview.review().artifacts().stream().anyMatch(artifact -> "PRD".equals(artifact.name())));

        workflowService.submitStageRevision("补充 Markdown 表格形式的阶段反馈");
        WorkflowStatus revisionStatus = workflowService.runOpenSpecAction("propose", new com.aifactory.dto.OpenSpecActionRequest("补充 Markdown 表格形式的阶段反馈"));
        assertNotNull(revisionStatus.review().revision());
        assertEquals("product-design-artifacts", revisionStatus.review().revision().stageKey());
        assertTrue(revisionStatus.review().revision().artifactPaths().stream().anyMatch(path -> path.contains("PRD.md")));
        assertTrue(revisionStatus.review().revision().userFeedback().contains("Markdown 表格"));

        approveUntilSuccess(Duration.ofSeconds(8));
        WorkflowStatus status = workflowService.currentStatus();
        ResultView result = workflowService.result();

        assertEquals("已完成", status.currentStage());
        assertEquals(15, status.steps().size());
        assertTrue(status.steps().stream().anyMatch(step -> "product-design-artifacts".equals(step.key())));
        assertTrue(status.steps().stream().anyMatch(step -> "ui-design".equals(step.key())));
        assertTrue(status.steps().stream().anyMatch(step -> "architecture-design".equals(step.key())));
        assertTrue(status.steps().stream().anyMatch(step -> "api-design".equals(step.key())));
        assertTrue(status.steps().stream().anyMatch(step -> "database-design".equals(step.key())));
        assertTrue(status.steps().stream().anyMatch(step -> "backend-development".equals(step.key()) && step.index() == 9));
        assertTrue(status.steps().stream().anyMatch(step -> "frontend-development".equals(step.key()) && step.index() == 10));
        assertTrue(status.steps().stream().anyMatch(step -> "development-integration".equals(step.key())));
        assertTrue(status.steps().stream().anyMatch(step -> "test-case-generation".equals(step.key())));
        assertTrue(status.steps().stream().anyMatch(step -> "e2e-acceptance-testing".equals(step.key())));
        assertTrue(status.steps().stream().anyMatch(step -> "delivery".equals(step.key())));
        assertTrue(result.available());
        assertTrue(result.designAvailable());
        assertTrue(result.artifacts().stream().anyMatch(artifact -> "业务流程图".equals(artifact.name()) && "plantuml".equals(artifact.type())));
        assertTrue(result.artifacts().stream().anyMatch(artifact -> "信息架构图".equals(artifact.name()) && "plantuml".equals(artifact.type())));
        assertTrue(result.artifacts().stream().anyMatch(artifact -> "页面流转图".equals(artifact.name()) && "plantuml".equals(artifact.type())));
        assertTrue(result.artifacts().stream().noneMatch(artifact -> "页面流转图预览".equals(artifact.name()) && "image".equals(artifact.type())));
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
        assertTrue(result.artifacts().stream().anyMatch(artifact -> "code".equals(artifact.stage()) && "生成项目".equals(artifact.name())));
        assertTrue(result.artifacts().stream().anyMatch(artifact -> "backend-development".equals(artifact.stage()) && "后端工程".equals(artifact.name())));
        assertTrue(result.artifacts().stream().anyMatch(artifact -> "test-case-generation".equals(artifact.stage()) && "测试用例摘要".equals(artifact.name())));
        assertTrue(result.artifacts().stream().anyMatch(artifact -> "e2e-acceptance-testing".equals(artifact.stage()) && "测试报告".equals(artifact.name())));
        assertEquals("/api/artifacts?path=generated%2FAI%2Ftest-report.md", result.reportUrl());
        assertEquals("/api/artifacts?path=generated%2FAI", result.projectUrl());
        assertTrue(result.prdMarkdown().contains("PRD"));

        ArgumentCaptor<StageRevisionContext> revisionContext = ArgumentCaptor.forClass(StageRevisionContext.class);
        verify(claudeCodeService).runOpenSpecAction(anyString(), revisionContext.capture());
        assertEquals(firstReview.review().workflowRunId(), revisionContext.getValue().workflowRunId());
        assertEquals("product-design-artifacts", revisionContext.getValue().stageKey());
        assertTrue(revisionContext.getValue().artifactPaths().stream().anyMatch(path -> path.contains("PRD.md")));
        assertTrue(revisionContext.getValue().userFeedback().contains("Markdown 表格"));
        assertTrue(revisionContext.getValue().allowedPaths().contains("产品设计"));

        ArgumentCaptor<String> frontendPrompt = ArgumentCaptor.forClass(String.class);
        verify(claudeCodeService).runGenerate(anyString(), anyString(), frontendPrompt.capture(), anyString());
        assertTrue(frontendPrompt.getValue().contains("AI 质检助手"));
        assertTrue(frontendPrompt.getValue().contains("UI 设计规范"));
        assertTrue(frontendPrompt.getValue().contains("UI 原型"));
        assertTrue(frontendPrompt.getValue().contains("UI-Design-Spec.md"));
        assertTrue(frontendPrompt.getValue().contains("前端代码"));
        assertTrue(frontendPrompt.getValue().contains("generated/{英文项目名}/frontend"));
        assertTrue(frontendPrompt.getValue().contains("generated/{英文项目名}/backend"));
        assertTrue(frontendPrompt.getValue().contains("完全禁止 mock"));

        ArgumentCaptor<String> backendPrompt = ArgumentCaptor.forClass(String.class);
        verify(claudeCodeService).runBackend(anyString(), anyString(), backendPrompt.capture(), anyString());
        assertTrue(backendPrompt.getValue().contains("H2 + Flyway"));
        assertTrue(backendPrompt.getValue().contains("application-prod.yml"));
    }

    @Test
    void shouldRecoverUiDesignWhenRunnerTimeoutButArtifactsExist() throws Exception {
        Path projectRoot = Path.of("target/docs/recovered-ui-design");
        Files.createDirectories(projectRoot.resolve("产品设计/03-UI设计规范"));
        Files.createDirectories(projectRoot.resolve("UI原型/设计稿"));
        Files.createDirectories(projectRoot.resolve("UI原型/组件库"));
        Files.createDirectories(projectRoot.resolve("UI原型/交互原型"));
        Files.createDirectories(projectRoot.resolve("UI原型/移动端适配"));
        Files.writeString(projectRoot.resolve("产品设计/03-UI设计规范/UI-Design-Spec.md"), "# UI Design Spec");
        Files.writeString(projectRoot.resolve("UI原型/设计稿/index.html"), "<html></html>");
        Files.writeString(projectRoot.resolve("UI原型/设计稿/desktop.svg"), "<svg></svg>");
        Files.writeString(projectRoot.resolve("UI原型/设计稿/mobile.svg"), "<svg></svg>");
        Files.writeString(projectRoot.resolve("UI原型/组件库/组件清单.md"), "# 组件清单");
        Files.writeString(projectRoot.resolve("UI原型/交互原型/交互说明.md"), "# 交互说明");
        Files.writeString(projectRoot.resolve("UI原型/移动端适配/响应式断点参考.md"), "# 响应式断点参考");

        when(claudeCodeService.runTask(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(new ClaudeRunResult("task-recover", "session-recover", "success", "# PRD", List.of(
                        projectRoot.resolve("产品设计/02-产品需求文档/PRD.md").toString(),
                        projectRoot.resolve("产品设计/03-UI设计规范/UI-Design-Spec.md").toString()
                ), "workspace/runtime/task-recover", "workspace/runtime/task-recover/project", projectRoot.resolve("产品设计").toString(), "recovered-ui-design"))
                .thenThrow(new RuntimeException(new SocketTimeoutException("Read timed out")))
                .thenReturn(new ClaudeRunResult("task-recover", "session-recover", "success", "# 架构设计", List.of(), "workspace/runtime/task-recover", "workspace/runtime/task-recover/project", projectRoot.resolve("产品设计").toString(), "recovered-ui-design"))
                .thenReturn(new ClaudeRunResult("task-recover", "session-recover", "success", "# 接口设计", List.of(), "workspace/runtime/task-recover", "workspace/runtime/task-recover/project", projectRoot.resolve("产品设计").toString(), "recovered-ui-design"))
                .thenReturn(new ClaudeRunResult("task-recover", "session-recover", "success", "# 数据库设计", List.of(), "workspace/runtime/task-recover", "workspace/runtime/task-recover/project", projectRoot.resolve("产品设计").toString(), "recovered-ui-design"));
        when(claudeCodeService.runGenerate(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(new ClaudeRunResult("task-recover", "session-recover", "success", "generated", List.of(), "workspace/runtime/task-recover", "workspace/runtime/task-recover/project", projectRoot.resolve("产品设计").toString(), "recovered-ui-design"));
        when(claudeCodeService.runBackend(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(new ClaudeRunResult("task-recover", "session-recover", "success", "backend", List.of(), "workspace/runtime/task-recover", "workspace/runtime/task-recover/project", projectRoot.resolve("产品设计").toString(), "recovered-ui-design"));
        when(claudeCodeService.runTestCases(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(new ClaudeRunResult("task-recover", "session-recover", "success", "test cases", List.of(), "workspace/runtime/task-recover", "workspace/runtime/task-recover/project", projectRoot.resolve("产品设计").toString(), "recovered-ui-design"));
        when(claudeCodeService.runPlaywright(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(new ClaudeRunResult("task-recover", "session-recover", "success", "playwright", List.of(), "workspace/runtime/task-recover", "workspace/runtime/task-recover/project", projectRoot.resolve("产品设计").toString(), "recovered-ui-design"));
        when(claudeCodeService.runFixTests(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(new ClaudeRunResult("task-recover", "session-recover", "success", "tests fixed", List.of(), "workspace/runtime/task-recover", "workspace/runtime/task-recover/project", projectRoot.resolve("产品设计").toString(), "recovered-ui-design"));

        workflowService.start("做一个 UI 恢复演练系统");
        WorkflowStatus productReview = awaitStatus("awaiting_review", Duration.ofSeconds(5));
        assertEquals("product-design-artifacts", productReview.review().stageKey());
        approveUntilSuccess(Duration.ofSeconds(8));

        WorkflowStatus status = workflowService.currentStatus();
        ResultView result = workflowService.result();
        assertEquals("success", status.status());
        assertTrue(result.designAvailable());
        assertTrue(result.artifacts().stream().anyMatch(artifact -> "ui-prototype".equals(artifact.stage()) && "HTML 原型".equals(artifact.name())));
        assertTrue(result.artifacts().stream().anyMatch(artifact -> "ui-prototype".equals(artifact.stage()) && "组件清单".equals(artifact.name())));
        assertTrue(result.artifacts().stream().anyMatch(artifact -> "ui-prototype".equals(artifact.stage()) && "交互说明".equals(artifact.name())));
        assertTrue(result.artifacts().stream().anyMatch(artifact -> "ui-prototype".equals(artifact.stage()) && "响应式断点参考".equals(artifact.name())));
        assertTrue(workflowService.logs().stream().anyMatch(log -> log.message().contains("恢复 UI 设计阶段结果")));
    }

    @Test
    void shouldRecoverToBackendWhenOnlyFrontendExistsForGeneratedProject() throws Exception {
        Path repoRoot = Path.of("../..").toAbsolutePath().normalize();
        Path docsProjectRoot = repoRoot.resolve("docs/移动端应用：会议室预约系统");
        Path frontendDir = repoRoot.resolve("generated/HX-Meeting/frontend");
        assertTrue(Files.isRegularFile(docsProjectRoot.resolve("产品设计/02-产品需求文档/PRD.md")));
        assertTrue(Files.isRegularFile(docsProjectRoot.resolve("数据库设计/数据字典/数据字典.md")));
        assertTrue(Files.isRegularFile(frontendDir.resolve("package.json")));

        WorkflowStatus status = workflowService.recoverProject("移动端应用：会议室预约系统");

        assertNotNull(status.currentStage());
        assertTrue(workflowService.result().artifacts().stream().anyMatch(artifact -> "code".equals(artifact.stage()) && "前端工程".equals(artifact.name())));
        assertTrue(workflowService.result().projectUrl().contains("generated%2FHX-Meeting"));
        assertTrue(workflowService.result().artifacts().stream().noneMatch(artifact -> artifact.path().contains("Generated-App")));
    }

    @Test
    void shouldExposeErrorWhenDesignStageFails() throws Exception {
        when(claudeCodeService.runTask(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(new ClaudeRunResult("task-2", "session-2", "success", "# PRD", List.of(), "workspace/runtime/task-2", "workspace/runtime/task-2/project", "docs/失败演练系统/产品设计", "失败演练系统"))
                .thenThrow(new IllegalStateException("runner failed"));

        workflowService.start("做一个失败演练系统");
        WorkflowStatus review = awaitStatus("awaiting_review", Duration.ofSeconds(5));
        assertEquals("product-design-artifacts", review.review().stageKey());
        workflowService.approveCurrentStage();
        WorkflowStatus status = awaitStatus("error", Duration.ofSeconds(5));

        assertEquals("error", status.status());
        assertTrue(workflowService.logs().stream().anyMatch(item -> "error".equals(item.level())));
    }

    private void approveUntilSuccess(Duration timeout) throws Exception {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            WorkflowStatus status = workflowService.currentStatus();
            if ("success".equals(status.status())) {
                return;
            }
            if ("awaiting_review".equals(status.status())) {
                workflowService.approveCurrentStage();
            }
            Thread.sleep(50L);
        }
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
