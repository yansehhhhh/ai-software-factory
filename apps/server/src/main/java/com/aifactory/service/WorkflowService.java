package com.aifactory.service;

import com.aifactory.dto.AgentStatus;
import com.aifactory.dto.ArtifactView;
import com.aifactory.dto.ClaudeRunResult;
import com.aifactory.dto.LogEntry;
import com.aifactory.dto.ResultView;
import com.aifactory.dto.StepStatus;
import com.aifactory.dto.WorkflowStatus;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class WorkflowService {

    private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss", Locale.CHINA);
    private static final List<String> EXAMPLES = List.of("AI质检助手", "数据分析系统", "简单博客系统", "会议纪要助手", "数据库管理系统", "智能客服系统");
    private static final int MAX_FIX_ATTEMPTS = 2;
    private static final List<WorkflowStageDefinition> STAGES = List.of(
            new WorkflowStageDefinition("requirements", "需求分析", "Requirement Agent", "需求分析师", "需求摘要", "开始整理需求输入并生成需求摘要", "需求摘要整理完成", 8, null),
            new WorkflowStageDefinition("brainstorming", "需求讨论", "Requirement Agent", "需求讨论专家", "设计文档", "开始整理需求讨论结论", "需求讨论结论整理完成", 15, null),
            new WorkflowStageDefinition("writing-plans", "制定计划", "Product Agent", "计划工程师", "实现计划", "开始制定阶段化实现计划", "阶段化实现计划整理完成", 22, null),
            new WorkflowStageDefinition("product-design-artifacts", "需求产物生成", "Product Agent", "产品文档工程师", "需求阶段产物", "开始生成变更记录、PRD、流程图和附录等需求产物", "需求阶段产物生成完成", 34, "prd"),
            new WorkflowStageDefinition("ui-design", "UI设计", "Design Agent", "UI设计师", "UI设计产物", "开始根据需求文档生成 UI 设计规范和设计图", "UI 设计产物生成完成", 45, "ui"),
            new WorkflowStageDefinition("architecture-design", "架构设计", "Architecture Agent", "架构师", "架构设计", "开始生成前后端架构设计产物", "架构设计产物生成完成", 53, "architecture"),
            new WorkflowStageDefinition("api-design", "接口设计", "API Agent", "接口设计师", "接口定义", "开始生成接口清单与 OpenAPI 定义", "接口设计产物生成完成", 60, "api"),
            new WorkflowStageDefinition("database-design", "数据库设计", "Database Agent", "数据库设计师", "数据库设计", "开始生成数据字典、建表脚本和迁移脚本", "数据库设计产物生成完成", 67, "database"),
            new WorkflowStageDefinition("frontend-development", "前端开发", "Frontend Agent", "前端开发工程师", "前端代码", "开始根据设计产物生成前端代码", "前端开发阶段完成", 76, "generate"),
            new WorkflowStageDefinition("backend-development", "后端开发", "Backend Agent", "后端开发工程师", "后端代码", "开始根据设计产物生成后端代码", "后端代码生成完成", 82, "backend"),
            new WorkflowStageDefinition("test-case-generation", "测试用例生成", "QA Agent", "测试设计工程师", "测试用例", "开始生成测试用例和 E2E 测试", "测试用例生成完成", 87, "test-cases"),
            new WorkflowStageDefinition("playwright-execution", "Playwright执行", "QA Agent", "端到端测试工程师", "Playwright结果", "开始执行 Playwright 测试", "Playwright 执行完成", 91, "playwright"),
            new WorkflowStageDefinition("verification-before-completion", "完成前验证", "QA Agent", "验证工程师", "验证结果", "开始执行完成前验证与测试修复", "完成前验证与测试修复完成", 96, "fix-tests"),
            new WorkflowStageDefinition("delivery", "交付", "Orchestrator", "交付协调器", "交付结果", "开始整理最终交付结果", "全流程执行完成，可查看生成结果", 100, null)
    );

    private final ClaudeCodeService claudeCodeService;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "workflow-design-executor");
        thread.setDaemon(true);
        return thread;
    });

    private WorkflowRun currentRun = WorkflowRun.idle();

    public WorkflowService(ClaudeCodeService claudeCodeService) {
        this.claudeCodeService = claudeCodeService;
    }

    public synchronized WorkflowStatus currentStatus() {
        return currentRun.toWorkflowStatus();
    }

    public synchronized WorkflowStatus start(String nextRequirement) {
        WorkflowRun nextRun = WorkflowRun.starting(nextRequirement.strip(), EXAMPLES);
        currentRun = nextRun;
        executor.submit(() -> runPipeline(nextRun.taskId));
        return currentRun.toWorkflowStatus();
    }

    public synchronized List<LogEntry> logs() {
        return List.copyOf(currentRun.logs);
    }

    public synchronized void clearLogs() {
        currentRun.logs.clear();
    }

    public synchronized ResultView result() {
        return currentRun.result;
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdownNow();
    }

    private void runPipeline(String taskId) {
        try {
            addLog(taskId, "Orchestrator", "info", "任务已创建");
            ClaudeRunResult productDesignResult = null;
            for (WorkflowStageDefinition stage : STAGES) {
                startStage(taskId, stage);
                if ("prd".equals(stage.runnerMode())) {
                    ClaudeRunResult result = claudeCodeService.runTask(taskId, currentSessionId(taskId), "prd", currentRequirement(taskId));
                    productDesignResult = result;
                    completeStage(taskId, stage, result);
                } else if ("ui".equals(stage.runnerMode())) {
                    ClaudeRunResult result = claudeCodeService.runTask(taskId, currentSessionId(taskId), "ui", buildUiDesignPrompt(taskId, productDesignResult));
                    completeStage(taskId, stage, result);
                } else if ("architecture".equals(stage.runnerMode()) || "api".equals(stage.runnerMode()) || "database".equals(stage.runnerMode())) {
                    ClaudeRunResult result = claudeCodeService.runTask(taskId, currentSessionId(taskId), stage.runnerMode(), currentRequirement(taskId));
                    completeStage(taskId, stage, result);
                } else if ("generate".equals(stage.runnerMode())) {
                    ClaudeRunResult result = claudeCodeService.runGenerate(taskId, currentSessionId(taskId), currentRequirement(taskId));
                    completeStage(taskId, stage, result);
                } else if ("backend".equals(stage.runnerMode())) {
                    ClaudeRunResult result = claudeCodeService.runBackend(taskId, currentSessionId(taskId), buildBackendPrompt(taskId));
                    completeStage(taskId, stage, result);
                } else if ("test-cases".equals(stage.runnerMode())) {
                    ClaudeRunResult result = claudeCodeService.runTestCases(taskId, currentSessionId(taskId), buildTestCasesPrompt(taskId));
                    completeStage(taskId, stage, result);
                } else if ("playwright".equals(stage.runnerMode())) {
                    ClaudeRunResult result = claudeCodeService.runPlaywright(taskId, currentSessionId(taskId), buildPlaywrightPrompt(taskId));
                    completeStage(taskId, stage, result);
                } else if ("fix-tests".equals(stage.runnerMode())) {
                    ClaudeRunResult result = runFixLoop(taskId);
                    completeStage(taskId, stage, result);
                } else {
                    completePlaceholderStage(taskId, stage);
                }
                sleepQuietly(120L);
            }
            completeWorkflow(taskId);
        } catch (Exception exception) {
            markError(taskId, exception.getMessage() == null ? "Claude Runner 执行失败" : exception.getMessage());
        }
    }

    private String buildUiDesignPrompt(String taskId, ClaudeRunResult productDesignResult) {
        StringBuilder builder = new StringBuilder();
        builder.append("原始需求：\n").append(currentRequirement(taskId)).append("\n\n");
        builder.append("请基于上一阶段生成的产品设计产物继续 UI 设计，不要只根据原始需求生成通用页面。\n");
        builder.append("必须读取并遵循 PRD.md、UI-Design-Spec.md、业务流程图、信息架构图、页面流转图和术语表。\n");
        if (productDesignResult != null && productDesignResult.artifacts() != null && !productDesignResult.artifacts().isEmpty()) {
            builder.append("\n产品设计产物路径：\n");
            for (String artifact : productDesignResult.artifacts()) {
                builder.append("- ").append(artifact).append("\n");
            }
        }
        builder.append("\n请输出 UI 原型、设计图、组件库、交互说明和移动端适配产物。");
        return builder.toString();
    }

    private String buildBackendPrompt(String taskId) {
        return buildImplementationPrompt(taskId, "请生成真实后端代码到 project/backend 目录，优先 Spring Boot，并遵循 controller -> service -> workflow/skill 分层。");
    }

    private String buildTestCasesPrompt(String taskId) {
        return buildImplementationPrompt(taskId, "请生成测试用例文档到 project/tests/test-cases，并生成 Playwright 测试到 project/tests/e2e。");
    }

    private String buildPlaywrightPrompt(String taskId) {
        return buildImplementationPrompt(taskId, "请在生成工程目录内执行或准备执行 Playwright 测试，并将执行摘要写入 project/test-report.md。");
    }

    private synchronized String buildImplementationPrompt(String taskId, String instruction) {
        StringBuilder builder = new StringBuilder();
        builder.append("原始需求：\n").append(currentRequirement(taskId)).append("\n\n");
        builder.append("任务要求：\n").append(instruction).append("\n\n");
        builder.append("已有产物摘要：\n").append(currentRun.result.prdMarkdown()).append("\n\n");
        if (currentRun.result.artifacts() != null && !currentRun.result.artifacts().isEmpty()) {
            builder.append("已有产物路径：\n");
            for (ArtifactView artifact : currentRun.result.artifacts()) {
                builder.append("- ").append(artifact.name()).append("：").append(artifact.path()).append("\n");
            }
        }
        return builder.toString();
    }

    private ClaudeRunResult runFixLoop(String taskId) {
        ClaudeRunResult result = null;
        for (int attempt = 1; attempt <= MAX_FIX_ATTEMPTS; attempt++) {
            addLog(taskId, "QA Agent", "info", "开始第 " + attempt + " 轮测试修复");
            result = claudeCodeService.runFixTests(taskId, currentSessionId(taskId), "请在当前工程目录执行测试并修复失败项，第 " + attempt + " 轮。");
            addLog(taskId, "QA Agent", "info", "第 " + attempt + " 轮测试修复完成");
        }
        return result;
    }

    private synchronized String currentRequirement(String taskId) {
        return isCurrent(taskId) ? currentRun.requirement : "";
    }

    private synchronized String currentSessionId(String taskId) {
        if (!isCurrent(taskId)) {
            return "";
        }
        if (currentRun.sessionId == null || currentRun.sessionId.isBlank()) {
            currentRun.sessionId = "workflow-" + taskId;
        }
        return currentRun.sessionId;
    }

    private synchronized void startStage(String taskId, WorkflowStageDefinition stage) {
        if (!isCurrent(taskId)) {
            return;
        }

        currentRun.status = "running";
        currentRun.currentStage = stage.title();
        currentRun.currentArtifactType = stage.artifactType();
        currentRun.designProgressMessage = stage.startMessage();
        currentRun.progress = Math.max(1, stage.progress() - 4);
        currentRun.estimatedRemaining = stage.progress() >= 100 ? "00:00" : "执行中";
        currentRun.estimatedCompletion = stage.progress() >= 100 ? "已完成" : "执行中";
        currentRun.logs.add(log(stage.agentName(), "info", stage.startMessage()));
        updateStep(taskId, stage.key(), "running", Math.max(5, stage.progress() - 10), "进行中", "0s", null);
        updateAgent(taskId, stage.key(), "running", Math.max(5, stage.progress() - 10));
    }

    private synchronized void completePlaceholderStage(String taskId, WorkflowStageDefinition stage) {
        if (!isCurrent(taskId)) {
            return;
        }

        currentRun.logs.add(log(stage.agentName(), "info", stage.completeMessage()));
        updateStep(taskId, stage.key(), "success", 100, stage.completeMessage(), elapsedLabel(currentRun.startedAt), null);
        updateAgent(taskId, stage.key(), "success", 100);
        currentRun.progress = stage.progress();
    }

    private synchronized void completeStage(String taskId, WorkflowStageDefinition stage, ClaudeRunResult result) {
        if (!isCurrent(taskId)) {
            return;
        }

        currentRun.logs.add(log(stage.agentName(), "info", stage.completeMessage()));
        updateStep(taskId, stage.key(), "success", 100, stage.completeMessage(), elapsedLabel(currentRun.startedAt), null);
        updateAgent(taskId, stage.key(), "success", 100);
        currentRun.progress = stage.progress();

        if (result == null) {
            return;
        }

        if ("prd".equals(stage.runnerMode())) {
            List<ArtifactView> productArtifacts = deliveryArtifactViews(stage.key(), result.artifacts());
            currentRun.result = new ResultView(
                    false,
                    !productArtifacts.isEmpty(),
                    artifactUrl(result.docsProjectDir()),
                    currentRun.result.reportUrl(),
                    currentRun.result.zipUrl(),
                    summaryMarkdown(result.projectName(), productArtifacts),
                    currentRun.result.pageSpecs(),
                    currentRun.result.componentSpecs(),
                    currentRun.result.userFlowSpecs(),
                    currentRun.result.uiGuidelines(),
                    mergeArtifacts(currentRun.result.artifacts(), productArtifacts)
            );
            return;
        }

        if ("ui".equals(stage.runnerMode())) {
            List<ArtifactView> uiArtifacts = deliveryArtifactViews(stage.key(), result.artifacts());
            currentRun.result = new ResultView(
                    currentRun.result.available(),
                    true,
                    artifactUrl(result.docsProjectDir()),
                    currentRun.result.reportUrl(),
                    currentRun.result.zipUrl(),
                    currentRun.result.prdMarkdown(),
                    currentRun.result.pageSpecs(),
                    currentRun.result.componentSpecs(),
                    currentRun.result.userFlowSpecs(),
                    artifactSummaryLines(uiArtifacts),
                    mergeArtifacts(currentRun.result.artifacts(), uiArtifacts)
            );
            return;
        }

        if ("architecture".equals(stage.runnerMode()) || "api".equals(stage.runnerMode()) || "database".equals(stage.runnerMode())) {
            List<ArtifactView> designArtifacts = deliveryArtifactViews(stage.key(), result.artifacts());
            currentRun.result = new ResultView(
                    currentRun.result.available(),
                    true,
                    artifactUrl(result.docsProjectDir()),
                    currentRun.result.reportUrl(),
                    currentRun.result.zipUrl(),
                    currentRun.result.prdMarkdown(),
                    currentRun.result.pageSpecs(),
                    currentRun.result.componentSpecs(),
                    currentRun.result.userFlowSpecs(),
                    currentRun.result.uiGuidelines(),
                    mergeArtifacts(currentRun.result.artifacts(), designArtifacts)
            );
            return;
        }

        if ("backend".equals(stage.runnerMode()) || "test-cases".equals(stage.runnerMode())) {
            List<ArtifactView> implementationArtifacts = artifactViews(stage.key(), result.artifacts());
            currentRun.result = new ResultView(
                    currentRun.result.available(),
                    currentRun.result.designAvailable(),
                    currentRun.result.projectUrl(),
                    currentRun.result.reportUrl(),
                    currentRun.result.zipUrl(),
                    currentRun.result.prdMarkdown(),
                    currentRun.result.pageSpecs(),
                    currentRun.result.componentSpecs(),
                    currentRun.result.userFlowSpecs(),
                    currentRun.result.uiGuidelines(),
                    mergeArtifacts(currentRun.result.artifacts(), implementationArtifacts)
            );
            return;
        }

        if ("playwright".equals(stage.runnerMode())) {
            List<ArtifactView> playwrightArtifacts = artifactViews(stage.key(), result.artifacts());
            String projectDir = result.projectDir();
            currentRun.result = new ResultView(
                    currentRun.result.available(),
                    currentRun.result.designAvailable(),
                    currentRun.result.projectUrl(),
                    artifactUrl(projectDir == null ? null : projectDir + "/test-report.md"),
                    currentRun.result.zipUrl(),
                    currentRun.result.prdMarkdown(),
                    currentRun.result.pageSpecs(),
                    currentRun.result.componentSpecs(),
                    currentRun.result.userFlowSpecs(),
                    currentRun.result.uiGuidelines(),
                    mergeArtifacts(currentRun.result.artifacts(), playwrightArtifacts)
            );
            return;
        }

        if ("generate".equals(stage.runnerMode())) {
            String projectDir = result.projectDir();
            String docsProjectDir = result.docsProjectDir();
            List<ArtifactView> mergedArtifacts = mergeArtifacts(currentRun.result.artifacts(), artifactViews("code", result.artifacts()));
            mergedArtifacts = appendProjectDirectoryArtifact(mergedArtifacts, projectDir);
            currentRun.result = new ResultView(
                    !mergedArtifacts.isEmpty(),
                    currentRun.result.designAvailable(),
                    generatedProjectUrl(projectDir),
                    artifactUrl(projectDir == null ? null : projectDir + "/test-report.md"),
                    artifactUrl(projectDir == null ? null : projectDir + "/project.zip"),
                    currentRun.result.prdMarkdown(),
                    currentRun.result.pageSpecs(),
                    currentRun.result.componentSpecs(),
                    currentRun.result.userFlowSpecs(),
                    currentRun.result.uiGuidelines(),
                    mergedArtifacts
            );
            return;
        }

        if ("fix-tests".equals(stage.runnerMode())) {
            String projectDir = result.projectDir();
            List<ArtifactView> mergedArtifacts = mergeArtifacts(currentRun.result.artifacts(), artifactViews("test-docs", result.artifacts()));
            currentRun.result = new ResultView(
                    currentRun.result.available(),
                    currentRun.result.designAvailable(),
                    currentRun.result.projectUrl(),
                    artifactUrl(projectDir == null ? null : projectDir + "/test-report.md"),
                    currentRun.result.zipUrl(),
                    currentRun.result.prdMarkdown(),
                    currentRun.result.pageSpecs(),
                    currentRun.result.componentSpecs(),
                    currentRun.result.userFlowSpecs(),
                    currentRun.result.uiGuidelines(),
                    mergedArtifacts
            );
        }
    }

    private synchronized void completeWorkflow(String taskId) {
        if (!isCurrent(taskId)) {
            return;
        }

        currentRun.logs.add(log("Orchestrator", "info", "全流程执行完成，可查看生成结果"));
        currentRun.status = "success";
        currentRun.currentStage = "已完成";
        currentRun.currentArtifactType = "生成结果";
        currentRun.designProgressMessage = "阶段化软件工厂链路已完成";
        currentRun.progress = 100;
        currentRun.estimatedRemaining = "00:00";
        currentRun.estimatedCompletion = "已完成";
    }

    private void updateStep(String taskId, String stageKey, String status, int progress, String message, String duration, String error) {
        if (!isCurrent(taskId)) {
            return;
        }
        int stageIndex = stageIndex(stageKey);
        if (stageIndex < 0) {
            return;
        }
        StepStatus step = currentRun.steps.get(stageIndex);
        currentRun.steps.set(stageIndex, new StepStatus(step.index(), step.key(), step.title(), status, progress, message, duration, error));
    }

    private void updateAgent(String taskId, String stageKey, String status, int progress) {
        if (!isCurrent(taskId)) {
            return;
        }
        int stageIndex = stageIndex(stageKey);
        if (stageIndex < 0) {
            return;
        }
        AgentStatus agent = currentRun.agents.get(stageIndex);
        currentRun.agents.set(stageIndex, new AgentStatus(agent.name(), agent.role(), status, agent.model(), elapsedLabel(currentRun.startedAt), progress));
    }

    private int stageIndex(String stageKey) {
        for (int index = 0; index < currentRun.steps.size(); index++) {
            if (Objects.equals(currentRun.steps.get(index).key(), stageKey)) {
                return index;
            }
        }
        return -1;
    }

    private List<ArtifactView> artifactViews(String stage, List<String> paths) {
        if (paths == null || paths.isEmpty()) {
            return List.of();
        }
        return paths.stream()
                .map(path -> new ArtifactView(stage, artifactName(stage, path), artifactType(path), artifactUrl(path), true))
                .toList();
    }

    private List<ArtifactView> deliveryArtifactViews(String stage, List<String> paths) {
        if (paths == null || paths.isEmpty()) {
            return List.of();
        }
        return paths.stream()
                .filter(this::isDeliveryArtifact)
                .map(path -> new ArtifactView(artifactStage(stage, path), artifactName(stage, path), artifactType(path), artifactUrl(path), true))
                .toList();
    }

    private String artifactStage(String fallbackStage, String artifactPath) {
        if (artifactPath == null || artifactPath.isBlank()) {
            return fallbackStage;
        }
        String normalized = artifactPath.replace('\\', '/');
        if (normalized.contains("/产品设计/")) {
            return "requirement";
        }
        if (normalized.contains("/UI原型/")) {
            return "ui-prototype";
        }
        if (normalized.contains("/架构设计/")) {
            return "architecture";
        }
        if (normalized.contains("/数据库设计/")) {
            return "database";
        }
        if (normalized.contains("/测试/")) {
            return "test-docs";
        }
        if (normalized.contains("/其他文档/")) {
            return "misc";
        }
        return fallbackStage;
    }

    private boolean isDeliveryArtifact(String path) {
        if (path == null || path.isBlank()) {
            return false;
        }
        return path.replace('\\', '/').contains("/docs/") || path.startsWith("docs/");
    }

    private List<ArtifactView> mergeArtifacts(List<ArtifactView> existing, List<ArtifactView> next) {
        List<ArtifactView> merged = new ArrayList<>();
        if (existing != null) {
            merged.addAll(existing);
        }
        merged.addAll(next);
        return List.copyOf(merged);
    }

    private List<ArtifactView> appendProjectDirectoryArtifact(List<ArtifactView> artifacts, String projectDir) {
        if (projectDir == null || projectDir.isBlank()) {
            return artifacts;
        }
        String projectUrl = artifactUrl(projectDir);
        if (artifacts.stream().anyMatch(item -> Objects.equals(item.path(), projectUrl))) {
            return artifacts;
        }
        List<ArtifactView> merged = new ArrayList<>(artifacts);
        merged.add(new ArtifactView("code", "生成项目", "directory", projectUrl, true));
        return List.copyOf(merged);
    }

    private String summaryMarkdown(String projectName, List<ArtifactView> artifacts) {
        String resolvedProjectName = (projectName == null || projectName.isBlank()) ? "当前项目" : projectName;
        if (artifacts == null || artifacts.isEmpty()) {
            return resolvedProjectName + " 的设计产物已生成。";
        }
        StringBuilder builder = new StringBuilder();
        builder.append(resolvedProjectName).append(" 的设计产物已生成：");
        for (ArtifactView artifact : artifacts) {
            builder.append("\n- ").append(artifact.name());
        }
        return builder.toString();
    }

    private List<String> artifactSummaryLines(List<ArtifactView> artifacts) {
        if (artifacts == null || artifacts.isEmpty()) {
            return List.of("UI 原型产物已生成。");
        }
        return artifacts.stream()
                .map(artifact -> artifact.name() + " 已生成")
                .toList();
    }

    private String artifactName(String stage, String artifactPath) {
        String fileName = fileName(artifactPath);
        return switch (fileName) {
            case "pm-product-pack.md" -> "需求文档";
            case "prd.md", "PRD.md" -> "PRD";
            case "requirements.docx", "PRD.docx" -> "需求文档 Word";
            case "ui-guidelines.md", "UI-Design-Spec.md" -> "UI 设计规范";
            case "prototype.md", "ui-prototype.md" -> "原型说明";
            case "ui-design.svg", "UI-Design.svg", "desktop.svg" -> "桌面端设计图";
            case "mobile.svg" -> "移动端设计图";
            case "component-library.html" -> "组件库 HTML";
            case "component-library.svg" -> "组件库设计图";
            case "interaction-flow.svg" -> "交互流程图";
            case "responsive-preview.html" -> "响应式 HTML 预览";
            case "mobile-preview.svg" -> "移动端适配图";
            case "业务流程图.puml" -> "业务流程图";
            case "信息架构图.puml" -> "信息架构图";
            case "页面流转图.puml" -> "页面流转图";
            case "页面流转图.svg" -> "页面流转图预览";
            case "术语表.md" -> "术语表";
            case "组件清单.md" -> "组件清单";
            case "交互说明.md" -> "交互说明";
            case "响应式断点参考.md" -> "响应式断点参考";
            case "系统架构设计.md" -> "系统架构设计";
            case "系统架构图.puml" -> "系统架构图";
            case "系统架构图.svg" -> "系统架构图预览";
            case "部署架构.md" -> "部署架构";
            case "技术选型.md" -> "技术选型";
            case "接口清单.md" -> "接口清单";
            case "openapi.yaml", "OpenAPI.yaml" -> "OpenAPI 定义";
            case "数据字典.md" -> "数据字典";
            case "schema.sql" -> "建表脚本";
            case "migration.sql" -> "数据迁移脚本";
            case "MASTER.md" -> "设计系统";
            case "index.html" -> "HTML 原型";
            case "project.zip" -> "代码压缩包";
            case "backend", "backend/" -> "后端工程";
            case "backend-summary.md" -> "后端生成摘要";
            case "test-cases", "test-cases/" -> "测试用例目录";
            case "test-case-summary.md" -> "测试用例摘要";
            case "e2e", "e2e/" -> "Playwright 测试目录";
            case "playwright-report", "playwright-report/" -> "Playwright HTML 报告";
            case "test-results", "test-results/" -> "Playwright 测试结果";
            case "test-report.md", "test-report.html" -> "测试报告";
            default -> "code".equals(stage) ? "生成项目" : fileName;
        };
    }

    private String generatedProjectUrl(String projectDir) {
        if (projectDir == null || projectDir.isBlank()) {
            return "";
        }
        return artifactUrl(projectDir + "/dist/index.html");
    }

    private String artifactUrl(String artifactPath) {
        return artifactPath == null || artifactPath.isBlank() ? "" : "/api/artifacts?path=" + java.net.URLEncoder.encode(artifactPath, java.nio.charset.StandardCharsets.UTF_8);
    }

    private String fileName(String artifactPath) {
        if (artifactPath == null || artifactPath.isBlank()) {
            return "产物";
        }
        String fileName = artifactPath.replace('\\', '/');
        int index = fileName.lastIndexOf('/');
        return index >= 0 ? fileName.substring(index + 1) : fileName;
    }

    private String artifactType(String artifactPath) {
        if (artifactPath == null || artifactPath.isBlank()) {
            return "file";
        }
        String normalized = artifactPath.toLowerCase(Locale.ROOT);
        if (normalized.endsWith(".md")) {
            return "markdown";
        }
        if (normalized.endsWith(".html")) {
            return "html";
        }
        if (normalized.endsWith(".puml")) {
            return "plantuml";
        }
        if (normalized.endsWith(".yaml") || normalized.endsWith(".yml")) {
            return "yaml";
        }
        if (normalized.endsWith(".sql")) {
            return "sql";
        }
        if (normalized.endsWith(".zip")) {
            return "zip";
        }
        if (normalized.endsWith(".docx")) {
            return "word";
        }
        if (normalized.endsWith(".svg") || normalized.endsWith(".png") || normalized.endsWith(".jpg") || normalized.endsWith(".jpeg")) {
            return "image";
        }
        return "directory";
    }

    private synchronized void markError(String taskId, String message) {
        if (!isCurrent(taskId)) {
            return;
        }

        currentRun.status = "error";
        currentRun.error = message;
        currentRun.estimatedRemaining = "--";
        currentRun.designProgressMessage = "Claude Runner 执行中断，请查看日志并重试";
        currentRun.logs.add(log("Orchestrator", "error", message));

        for (int i = 0; i < currentRun.steps.size(); i++) {
            StepStatus step = currentRun.steps.get(i);
            if ("running".equals(step.status())) {
                currentRun.steps.set(i, new StepStatus(step.index(), step.key(), step.title(), "error", step.progress(), step.title() + "失败", elapsedLabel(currentRun.startedAt), message));
                AgentStatus agent = currentRun.agents.get(i);
                currentRun.agents.set(i, new AgentStatus(agent.name(), agent.role(), "error", agent.model(), elapsedLabel(currentRun.startedAt), agent.progress()));
                break;
            }
        }
    }

    private synchronized void addLog(String taskId, String agent, String level, String message) {
        if (isCurrent(taskId)) {
            currentRun.logs.add(log(agent, level, message));
        }
    }

    private LogEntry log(String agent, String level, String message) {
        return new LogEntry(TIME_FORMATTER.format(LocalTime.now(SHANGHAI)), agent, level, message);
    }

    private boolean isCurrent(String taskId) {
        return Objects.equals(currentRun.taskId, taskId);
    }

    private String elapsedLabel(Instant startedAt) {
        long seconds = Math.max(0, Duration.between(startedAt, Instant.now()).toSeconds());
        return seconds + "s";
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    private record WorkflowStageDefinition(
            String key,
            String title,
            String agentName,
            String agentRole,
            String artifactType,
            String startMessage,
            String completeMessage,
            int progress,
            String runnerMode
    ) {
    }

    private static final class WorkflowRun {

        private final String taskId;
        private final String requirement;
        private final Instant startedAt;
        private final List<String> examples;
        private final List<LogEntry> logs;
        private final List<StepStatus> steps;
        private final List<AgentStatus> agents;
        private String sessionId;
        private String status;
        private String currentStage;
        private String currentArtifactType;
        private String designProgressMessage;
        private int progress;
        private String estimatedRemaining;
        private String estimatedCompletion;
        private String error;
        private ResultView result;

        private WorkflowRun(
                String taskId,
                String requirement,
                Instant startedAt,
                List<String> examples,
                List<LogEntry> logs,
                List<StepStatus> steps,
                List<AgentStatus> agents,
                String sessionId,
                String status,
                String currentStage,
                String currentArtifactType,
                String designProgressMessage,
                int progress,
                String estimatedRemaining,
                String estimatedCompletion,
                String error,
                ResultView result
        ) {
            this.taskId = taskId;
            this.requirement = requirement;
            this.startedAt = startedAt;
            this.examples = examples;
            this.logs = logs;
            this.steps = steps;
            this.agents = agents;
            this.sessionId = sessionId;
            this.status = status;
            this.currentStage = currentStage;
            this.currentArtifactType = currentArtifactType;
            this.designProgressMessage = designProgressMessage;
            this.progress = progress;
            this.estimatedRemaining = estimatedRemaining;
            this.estimatedCompletion = estimatedCompletion;
            this.error = error;
            this.result = result;
        }

        private static WorkflowRun idle() {
            return new WorkflowRun(
                    "idle",
                    "",
                    Instant.now(),
                    List.of(),
                    new ArrayList<>(),
                    stageStatuses(),
                    stageAgents(),
                    "",
                    "pending",
                    "未开始",
                    "--",
                    "等待任务启动",
                    0,
                    "--",
                    "--",
                    null,
                    new ResultView(false, false, null, null, null, null, List.of(), List.of(), List.of(), List.of(), List.of())
            );
        }

        private static WorkflowRun starting(String requirement, List<String> examples) {
            return new WorkflowRun(
                    UUID.randomUUID().toString(),
                    requirement,
                    Instant.now(),
                    examples,
                    new ArrayList<>(),
                    stageStatuses(),
                    stageAgents(),
                    "",
                    "running",
                    "需求分析",
                    "需求摘要",
                    "正在整理需求上下文",
                    5,
                    "执行中",
                    "执行中",
                    null,
                    new ResultView(false, false, null, null, null, null, List.of(), List.of(), List.of(), List.of(), List.of())
            );
        }

        private static ArrayList<StepStatus> stageStatuses() {
            ArrayList<StepStatus> steps = new ArrayList<>();
            for (int index = 0; index < STAGES.size(); index++) {
                WorkflowStageDefinition stage = STAGES.get(index);
                steps.add(new StepStatus(index + 1, stage.key(), stage.title(), index == 0 ? "running" : "pending", index == 0 ? 5 : 0, index == 0 ? "正在整理需求上下文" : "等待上一步完成", index == 0 ? "0s" : "--", null));
            }
            return steps;
        }

        private static ArrayList<AgentStatus> stageAgents() {
            ArrayList<AgentStatus> agents = new ArrayList<>();
            for (int index = 0; index < STAGES.size(); index++) {
                WorkflowStageDefinition stage = STAGES.get(index);
                agents.add(new AgentStatus(stage.agentName(), stage.agentRole(), index == 0 ? "running" : "pending", "Claude Code", index == 0 ? "0s" : "--", index == 0 ? 5 : 0));
            }
            return agents;
        }

        private WorkflowStatus toWorkflowStatus() {
            return new WorkflowStatus(
                    taskId,
                    requirement,
                    status,
                    statusLabel(status),
                    currentStage,
                    currentArtifactType,
                    designProgressMessage,
                    progress,
                    estimatedRemaining,
                    estimatedCompletion,
                    "--",
                    "在线",
                    examples,
                    List.copyOf(steps),
                    List.copyOf(agents),
                    error
            );
        }

        private static String statusLabel(String status) {
            return switch (status) {
                case "running" -> "执行中";
                case "success" -> "已完成";
                case "error" -> "失败";
                default -> "未开始";
            };
        }
    }
}
