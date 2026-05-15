package com.aifactory.service;

import com.aifactory.dto.AgentStatus;
import com.aifactory.dto.ArtifactView;
import com.aifactory.dto.ClaudeRunResult;
import com.aifactory.dto.DiscussionMessage;
import com.aifactory.dto.LogEntry;
import com.aifactory.dto.OpenSpecActionRequest;
import com.aifactory.dto.OpenSpecRunResult;
import com.aifactory.dto.ProjectSummary;
import com.aifactory.dto.ResultView;
import com.aifactory.dto.StageReviewState;
import com.aifactory.dto.StageRevisionContext;
import com.aifactory.dto.StepStatus;
import com.aifactory.dto.WorkflowRunSnapshot;
import com.aifactory.dto.WorkflowStatus;
import com.aifactory.config.ClaudeRunnerConfig;
import com.aifactory.exception.WorkflowStateException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;

import java.net.SocketTimeoutException;
import java.net.URLDecoder;
import java.net.http.HttpTimeoutException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class WorkflowService {

    private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss", Locale.CHINA);
    private static final List<String> EXAMPLES = List.of("AI质检助手", "数据分析系统", "简单博客系统", "会议纪要助手", "数据库管理系统", "智能客服系统");
    private static final List<String> ARCHIVE_SYNC_OPTIONS = List.of("同步规格后归档", "直接归档/跳过同步");
    private static final int MAX_FIX_ATTEMPTS = 2;
    private static final int UI_RECOVERY_SCAN_ATTEMPTS = 5;
    private static final long UI_RECOVERY_SCAN_DELAY_MILLIS = 500L;
    private static final List<String> UI_ARTIFACT_RELATIVE_PATHS = List.of(
            "UI原型/设计稿/index.html",
            "UI原型/设计稿/desktop.svg",
            "UI原型/设计稿/mobile.svg",
            "UI原型/组件库/组件清单.md",
            "UI原型/组件库/component-library.html",
            "UI原型/组件库/component-library.svg",
            "UI原型/交互原型/交互说明.md",
            "UI原型/交互原型/interaction-flow.svg",
            "UI原型/移动端适配/响应式断点参考.md",
            "UI原型/移动端适配/responsive-preview.html",
            "UI原型/移动端适配/mobile-preview.svg"
    );
    private static final List<String> REQUIRED_UI_ARTIFACT_RELATIVE_PATHS = List.of(
            "UI原型/设计稿/index.html",
            "UI原型/组件库/组件清单.md",
            "UI原型/交互原型/交互说明.md",
            "UI原型/移动端适配/响应式断点参考.md"
    );
    private static final List<String> REVIEWABLE_STAGE_KEYS = List.of(
            "product-design-artifacts",
            "ui-design",
            "architecture-design",
            "api-design",
            "database-design",
            "backend-development",
            "frontend-development",
            "development-integration",
            "test-case-generation",
            "e2e-acceptance-testing",
            "verification-before-completion"
    );
    private static final List<AgentDefinition> AGENTS = List.of(
            new AgentDefinition("product-manager", "产品经理", "需求分析、需求讨论、PRD 与产品产物"),
            new AgentDefinition("ui-designer", "UI设计师", "UI 原型、交互说明、设计规范"),
            new AgentDefinition("architect", "架构师", "系统架构、接口设计、数据库设计"),
            new AgentDefinition("backend-engineer", "后端开发工程师", "后端接口、服务、数据库相关开发"),
            new AgentDefinition("frontend-engineer", "前端开发工程师", "前端页面、组件、交互与接口对接"),
            new AgentDefinition("test-engineer", "测试工程师", "测试用例、E2E、验证报告与缺陷反馈"),
            new AgentDefinition("ops-engineer", "运维工程师", "部署、环境、数据库运维与交付检查")
    );
    private static final List<WorkflowStageDefinition> STAGES = List.of(
            new WorkflowStageDefinition("requirements", "需求分析", "Requirement Agent", "需求分析师", "product-manager", "需求摘要", "开始整理需求输入并生成需求摘要", "需求摘要整理完成", 8, null),
            new WorkflowStageDefinition("brainstorming", "需求讨论", "Requirement Agent", "需求讨论专家", "product-manager", "设计文档", "开始整理需求讨论结论", "需求讨论结论整理完成", 15, null),
            new WorkflowStageDefinition("writing-plans", "制定计划", "Product Agent", "计划工程师", "product-manager", "实现计划", "开始制定阶段化实现计划", "阶段化实现计划整理完成", 22, null),
            new WorkflowStageDefinition("product-design-artifacts", "需求产物生成", "Product Agent", "产品文档工程师", "product-manager", "需求阶段产物", "开始生成变更记录、PRD、流程图和附录等需求产物", "需求阶段产物生成完成", 34, "prd"),
            new WorkflowStageDefinition("ui-design", "UI设计", "Design Agent", "UI设计师", "ui-designer", "UI设计产物", "开始根据需求文档生成 UI 设计规范和设计图", "UI 设计产物生成完成", 45, "ui"),
            new WorkflowStageDefinition("architecture-design", "架构设计", "Architecture Agent", "架构师", "architect", "架构设计", "开始生成前后端架构设计产物", "架构设计产物生成完成", 53, "architecture"),
            new WorkflowStageDefinition("api-design", "接口设计", "API Agent", "接口设计师", "architect", "接口定义", "开始生成接口清单与 OpenAPI 定义", "接口设计产物生成完成", 60, "api"),
            new WorkflowStageDefinition("database-design", "数据库设计", "Database Agent", "数据库设计师", "architect", "数据库设计", "开始生成数据字典、建表脚本和迁移脚本", "数据库设计产物生成完成", 67, "database"),
            new WorkflowStageDefinition("backend-development", "后端开发", "Backend Agent", "后端开发工程师", "backend-engineer", "后端代码", "开始根据数据库和接口设计生成可运行后端代码", "后端代码生成完成", 74, "backend"),
            new WorkflowStageDefinition("frontend-development", "前端开发", "Frontend Agent", "前端开发工程师", "frontend-engineer", "前端代码", "开始基于真实后端接口生成前端代码", "前端开发阶段完成", 80, "generate"),
            new WorkflowStageDefinition("development-integration", "开发联调", "Integration Agent", "开发联调工程师", "frontend-engineer", "开发联调报告", "开始启动前后端并执行开发自测联调", "开发联调完成", 86, "dev-integration"),
            new WorkflowStageDefinition("test-case-generation", "测试用例生成", "Test Agent", "测试工程师", "test-engineer", "测试用例", "开始生成测试用例和 E2E 验收测试", "测试用例生成完成", 90, "test-cases"),
            new WorkflowStageDefinition("e2e-acceptance-testing", "E2E验收测试", "Test Agent", "测试工程师", "test-engineer", "E2E验收结果", "开始执行 E2E 验收测试", "E2E 验收测试完成", 94, "playwright"),
            new WorkflowStageDefinition("verification-before-completion", "完成前验证", "Test Agent", "测试工程师", "test-engineer", "验证结果", "开始执行完成前验证与测试修复", "完成前验证与测试修复完成", 97, "fix-tests"),
            new WorkflowStageDefinition("delivery", "交付", "Ops Agent", "运维工程师", "ops-engineer", "交付结果", "开始整理最终交付结果", "全流程执行完成，可查看生成结果", 100, null)
    );

    private final ClaudeCodeService claudeCodeService;
    private final ClaudeRunnerConfig config;
    private final ObjectMapper objectMapper;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "workflow-design-executor");
        thread.setDaemon(true);
        return thread;
    });

    private WorkflowRun currentRun = WorkflowRun.idle();
    private final Object reviewMonitor = new Object();

    public WorkflowService(ClaudeCodeService claudeCodeService, ClaudeRunnerConfig config, ObjectMapper objectMapper) {
        this.claudeCodeService = claudeCodeService;
        this.config = config;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void restoreLatestSnapshotOnStartup() {
        WorkflowRunSnapshot snapshot = loadLatestSnapshot();
        if (snapshot != null) {
            synchronized (this) {
                restoreFromSnapshot(snapshot, true);
            }
        }
    }

    public synchronized WorkflowStatus currentStatus() {
        return currentRun.toWorkflowStatus();
    }

    public synchronized WorkflowStatus start(String nextRequirement) {
        WorkflowRun nextRun = WorkflowRun.starting(nextRequirement.strip(), EXAMPLES);
        currentRun = nextRun;
        persistSnapshotQuietly();
        executor.submit(() -> runPipeline(nextRun.taskId));
        return currentRun.toWorkflowStatus();
    }

    public synchronized List<LogEntry> logs() {
        return List.copyOf(currentRun.logs);
    }

    public synchronized void clearLogs() {
        currentRun.logs.clear();
        persistSnapshotQuietly();
    }

    public synchronized ResultView result() {
        return currentRun.result;
    }

    public List<ProjectSummary> projects() {
        Map<String, ProjectSummary> summaries = new LinkedHashMap<>();
        snapshotSummaries().stream()
                .filter(project -> hasExistingGeneratedProject(project.name()))
                .forEach(project -> summaries.put(project.name(), project));
        docsProjectSummaries().stream()
                .forEach(project -> summaries.putIfAbsent(project.name(), project));
        return summaries.values().stream()
                .sorted(Comparator.comparing(ProjectSummary::updatedAt).reversed())
                .toList();
    }

    public synchronized WorkflowStatus recoverProject(String projectName) {
        String resolvedProjectName = projectName == null || projectName.isBlank() ? "" : projectName.strip();
        if (resolvedProjectName.isBlank()) {
            throw new IllegalArgumentException("请选择要恢复的历史项目");
        }

        WorkflowRunSnapshot snapshot = latestSnapshotForProject(resolvedProjectName);
        if (snapshot != null) {
            restoreFromSnapshot(snapshot, false);
            if (snapshotArtifactsAreValidForProject(resolvedProjectName) && snapshotCodeArtifactsAreCurrent(resolvedProjectName) && snapshotRecoveredStagesAreCurrent(resolvedProjectName)) {
                persistSnapshotQuietly();
                return currentRun.toWorkflowStatus();
            }
        }

        Path docsProjectRoot = resolveExistingDocsProjectRoot(resolvedProjectName);
        List<String> productArtifacts = scanExistingProductArtifacts(docsProjectRoot);
        List<String> uiArtifacts = scanExistingUiArtifacts(docsProjectRoot);
        List<String> architectureArtifacts = scanDirectoryArtifacts(docsProjectRoot.resolve("架构设计"));
        List<String> databaseArtifacts = scanDirectoryArtifacts(docsProjectRoot.resolve("数据库设计"));
        List<String> testArtifacts = scanDirectoryArtifacts(docsProjectRoot.resolve("测试"));
        List<String> frontendArtifacts = scanExistingFrontendArtifacts(resolvedProjectName);
        List<String> backendArtifacts = scanExistingBackendArtifacts(resolvedProjectName);
        List<String> codeArtifacts = new ArrayList<>();
        codeArtifacts.addAll(frontendArtifacts);
        codeArtifacts.addAll(backendArtifacts);
        List<String> miscArtifacts = scanDirectoryArtifacts(docsProjectRoot.resolve("其他文档"));
        if (productArtifacts.isEmpty() && uiArtifacts.isEmpty() && architectureArtifacts.isEmpty() && databaseArtifacts.isEmpty() && testArtifacts.isEmpty() && codeArtifacts.isEmpty() && miscArtifacts.isEmpty()) {
            throw new IllegalStateException("未找到可恢复产物");
        }

        WorkflowRun recoveredRun = WorkflowRun.starting("恢复流程：" + resolvedProjectName, EXAMPLES);
        currentRun = recoveredRun;
        currentRun.sessionId = "workflow-" + currentRun.taskId;
        currentRun.result = recoveredResult(resolvedProjectName, docsProjectRoot, productArtifacts, uiArtifacts, architectureArtifacts, databaseArtifacts, codeArtifacts, testArtifacts, miscArtifacts);
        currentRun.logs.add(log("Orchestrator", "info", "已从历史项目恢复：" + resolvedProjectName));
        applyRecoveredProgress(productArtifacts, uiArtifacts, architectureArtifacts, databaseArtifacts, frontendArtifacts, backendArtifacts, testArtifacts);
        persistSnapshotQuietly();
        return currentRun.toWorkflowStatus();
    }

    public synchronized WorkflowStatus recoverUiStage(String projectName) {
        String resolvedProjectName = projectName == null || projectName.isBlank() ? "公司内部使用的会议室预约系统" : projectName.strip();
        Path docsProjectRoot = resolveExistingDocsProjectRoot(resolvedProjectName);
        List<String> productArtifacts = scanExistingProductArtifacts(docsProjectRoot);
        List<String> uiArtifacts = scanExistingUiArtifacts(docsProjectRoot);
        if (productArtifacts.isEmpty() || !hasMinimumUiArtifacts(uiArtifacts)) {
            throw new IllegalStateException("未找到完整的需求产物或 UI 原型产物，无法恢复流程");
        }

        WorkflowRun recoveredRun = WorkflowRun.starting("恢复流程：" + resolvedProjectName, EXAMPLES);
        currentRun = recoveredRun;
        currentRun.sessionId = "workflow-" + currentRun.taskId;
        currentRun.status = "awaiting_review";
        currentRun.currentStage = "UI设计";
        currentRun.currentArtifactType = "UI设计产物";
        currentRun.designProgressMessage = "UI设计 已完成，请确认产物或提交修订反馈";
        currentRun.progress = 45;
        currentRun.estimatedRemaining = "等待确认";
        currentRun.estimatedCompletion = "等待用户确认";
        currentRun.error = null;
        currentRun.nextStageIndex = stageIndex("ui-design") + 1;
        currentRun.approvedStageKeys.add("product-design-artifacts");
        currentRun.logs.add(log("Orchestrator", "info", "已从已落盘产物恢复到 UI设计 等待确认"));

        updateStep(currentRun.taskId, "requirements", "success", 100, "需求摘要整理完成", elapsedLabel(currentRun.startedAt), null);
        updateStep(currentRun.taskId, "brainstorming", "success", 100, "需求讨论结论整理完成", elapsedLabel(currentRun.startedAt), null);
        updateStep(currentRun.taskId, "writing-plans", "success", 100, "阶段化实现计划整理完成", elapsedLabel(currentRun.startedAt), null);
        updateStep(currentRun.taskId, "product-design-artifacts", "success", 100, "需求阶段产物生成完成", elapsedLabel(currentRun.startedAt), null);
        updateStep(currentRun.taskId, "ui-design", "awaiting_review", 100, "等待用户确认", elapsedLabel(currentRun.startedAt), null);
        updateAgent(currentRun.taskId, "requirements", "success", 100);
        updateAgent(currentRun.taskId, "brainstorming", "success", 100);
        updateAgent(currentRun.taskId, "writing-plans", "success", 100);
        updateAgent(currentRun.taskId, "product-design-artifacts", "success", 100);
        updateAgent(currentRun.taskId, "ui-design", "success", 100);

        ClaudeRunResult productResult = new ClaudeRunResult(currentRun.taskId, currentRun.sessionId, "recovered", "需求产物已从磁盘恢复。", productArtifacts, null, null, docsProjectRoot.resolve("产品设计").toString(), resolvedProjectName);
        completeStage(currentRun.taskId, STAGES.get(stageIndex("product-design-artifacts")), productResult);
        ClaudeRunResult uiResult = new ClaudeRunResult(currentRun.taskId, currentRun.sessionId, "recovered", "UI 原型产物已从磁盘恢复。", uiArtifacts, null, null, docsProjectRoot.resolve("产品设计").toString(), resolvedProjectName);
        completeStage(currentRun.taskId, STAGES.get(stageIndex("ui-design")), uiResult);
        currentRun.status = "awaiting_review";
        currentRun.currentStage = "UI设计";
        currentRun.currentArtifactType = "UI设计产物";
        currentRun.designProgressMessage = "UI设计 已完成，请确认产物或提交修订反馈";
        currentRun.estimatedRemaining = "等待确认";
        currentRun.estimatedCompletion = "等待用户确认";
        currentRun.reviewFeedback = "";
        currentRun.revisionMessages.clear();
        currentRun.revision = null;
        currentRun.review = buildReviewState("ui-design", "UI设计", currentRun.nextStageIndex);
        persistSnapshotQuietly();
        return currentRun.toWorkflowStatus();
    }

    public WorkflowStatus approveCurrentStage() {
        synchronized (this) {
            if (!"awaiting_review".equals(currentRun.status) || currentRun.review == null) {
                throw new IllegalStateException("当前没有等待确认的阶段");
            }
            String approvedStageKey = currentRun.review.stageKey();
            String approvedStageTitle = currentRun.review.stageTitle();
            currentRun.approvedStageKeys.add(approvedStageKey);
            updateStep(currentRun.taskId, approvedStageKey, "success", 100, approvedStageTitle + "已确认", elapsedLabel(currentRun.startedAt), null);
            updateAgent(currentRun.taskId, approvedStageKey, "success", 100);
            String nextStageTitle = currentRun.review.nextStageTitle();
            currentRun.logs.add(log("Orchestrator", "info", approvedStageTitle + " 已确认" + (nextStageTitle == null ? "，完成交付" : "，继续执行" + nextStageTitle)));
            currentRun.status = "running";
            currentRun.designProgressMessage = "用户已确认 " + currentRun.review.stageTitle() + (nextStageTitle == null ? "，正在完成交付" : "，正在继续执行" + nextStageTitle);
            currentRun.review = null;
            currentRun.revisionMessages.clear();
            persistSnapshotQuietly();
        }
        synchronized (reviewMonitor) {
            reviewMonitor.notifyAll();
        }
        return currentStatus();
    }

    public synchronized WorkflowStatus submitStageRevision(String feedback) {
        ensureReviewStateReady();
        if (!"awaiting_review".equals(currentRun.status) || currentRun.review == null) {
            throw new WorkflowStateException("当前阶段状态已刷新，不再处于可修订状态。请重新打开当前阶段的“修订”面板后再提交。");
        }
        String resolvedFeedback = feedback == null ? "" : feedback.strip();
        currentRun.reviewFeedback = resolvedFeedback;
        currentRun.revisionMessages.add(DiscussionMessage.user(resolvedFeedback));
        currentRun.revisionMessages.add(DiscussionMessage.ai(revisionAssessmentMessage(resolvedFeedback)));
        currentRun.revision = revisionContext(currentRun.review.stageKey(), currentRun.review.stageTitle(), resolvedFeedback, null, "feedback_submitted", "未创建", "未执行", "未归档");
        currentRun.review = buildReviewState(currentRun.review.stageKey(), currentRun.review.stageTitle(), currentRun.nextStageIndex);
        currentRun.logs.add(log("OpenSpec", "info", "已记录 " + currentRun.review.stageTitle() + " 阶段修订反馈"));
        persistSnapshotQuietly();
        return currentRun.toWorkflowStatus();
    }

    private String revisionAssessmentMessage(String feedback) {
        String normalized = feedback == null ? "" : feedback;
        String type = normalized.lines()
                .filter(line -> line.startsWith("反馈类型："))
                .map(line -> line.substring("反馈类型：".length()).strip())
                .findFirst()
                .orElse("缺陷修复");
        String scope = assessRevisionScope(normalized);
        String suggestion = switch (type) {
            case "需求变更" -> "这类反馈可能改变产品范围，建议先点击“探索修订”或创建 OpenSpec 变更，确认是否需要更新需求、接口、实现和测试。";
            case "验收/测试辅助" -> "这类反馈可作为当前阶段的验收辅助处理，建议限制在预览/测试环境，并补充测试说明。";
            default -> "这类反馈可作为当前阶段缺陷修复处理，修复后需要重新验证相关产物。";
        };
        return "已收到修订反馈。\n\n"
                + "我的初步评估：\n"
                + "- 反馈类型：" + type + "\n"
                + "- 影响范围：" + scope + "\n"
                + "- 处理建议：" + suggestion + "\n\n"
                + "下一步可以点击“探索修订”让我进一步分析修改方案；如果修改点已经明确，也可以直接点击“制定计划”。";
    }

    private String assessRevisionScope(String feedback) {
        List<String> scopes = new ArrayList<>();
        if (feedback.contains("前端") || feedback.contains("页面") || feedback.contains("按钮") || feedback.contains("URL") || feedback.contains("参数") || feedback.contains("样式") || feedback.contains("交互")) {
            scopes.add("前端");
        }
        if (feedback.contains("后端") || feedback.contains("接口") || feedback.contains("API") || feedback.contains("数据库") || feedback.contains("权限") || feedback.contains("token")) {
            scopes.add("后端");
        }
        if (feedback.contains("测试") || feedback.contains("验收") || feedback.contains("E2E") || feedback.contains("Playwright")) {
            scopes.add("测试");
        }
        if (feedback.contains("文档") || feedback.contains("PRD") || feedback.contains("需求") || feedback.contains("说明")) {
            scopes.add("文档");
        }
        return scopes.isEmpty() ? "待进一步确认" : String.join("、", scopes);
    }

    public WorkflowStatus continueRecoveredWorkflow() {
        String taskId;
        int startIndex;
        synchronized (this) {
            if (!"running".equals(currentRun.status) || currentRun.review != null || currentRun.nextStageIndex < stageIndex("ui-design")) {
                return currentRun.toWorkflowStatus();
            }
            taskId = currentRun.taskId;
            startIndex = currentRun.nextStageIndex;
        }
        executor.submit(() -> runPipelineFrom(taskId, startIndex));
        return currentStatus();
    }

    public WorkflowStatus retryFailedStage() {
        String taskId;
        int startIndex;
        synchronized (this) {
            if (!"error".equals(currentRun.status)) {
                return currentRun.toWorkflowStatus();
            }
            startIndex = stageIndexByTitle(currentRun.currentStage);
            if (startIndex < 0) {
                throw new IllegalStateException("无法识别当前失败阶段：" + currentRun.currentStage);
            }
            currentRun.status = "running";
            currentRun.error = null;
            currentRun.designProgressMessage = "正在重试" + currentRun.currentStage;
            currentRun.estimatedRemaining = "执行中";
            currentRun.estimatedCompletion = "执行中";
            currentRun.logs.add(log("Orchestrator", "info", "正在重试" + currentRun.currentStage));
            taskId = currentRun.taskId;
            persistSnapshotQuietly();
        }
        executor.submit(() -> runPipelineFrom(taskId, startIndex));
        return currentStatus();
    }

    public WorkflowStatus runOpenSpecAction(String action, OpenSpecActionRequest request) {
        StageRevisionContext context;
        synchronized (this) {
            ensureReviewStateReady();
            if (!"awaiting_review".equals(currentRun.status) || currentRun.review == null) {
                throw new WorkflowStateException("当前阶段状态已刷新，不再处于可修订状态。请重新打开当前阶段的“修订”面板后再操作。");
            }
            String feedback = request == null || request.feedback() == null || request.feedback().isBlank() ? currentRun.reviewFeedback : request.feedback().strip();
            currentRun.reviewFeedback = feedback;
            String stageKey = currentRun.review.stageKey();
            String stageTitle = currentRun.review.stageTitle();
            String changeId = currentRun.revision == null ? null : currentRun.revision.changeId();
            if (("apply".equals(action) || "archive".equals(action)) && (changeId == null || changeId.isBlank())) {
                throw new IllegalStateException("请先制定计划，生成 changeId 后再执行计划或归档");
            }
            context = revisionContext(stageKey, stageTitle, feedback, changeId, actionStatus(action), currentRun.revision == null ? "未创建" : currentRun.revision.proposalStatus(), currentRun.revision == null ? "未执行" : currentRun.revision.applyStatus(), currentRun.revision == null ? "未归档" : currentRun.revision.archiveStatus());
            currentRun.revision = context;
            currentRun.review = buildReviewState(stageKey, stageTitle, currentRun.nextStageIndex);
            String startMessage = "开始" + actionLabel(action) + "阶段修订：" + stageTitle;
            currentRun.revisionMessages.add(DiscussionMessage.ai("正在" + actionLabel(action) + "「" + stageTitle + "」阶段修订，请稍候……"));
            currentRun.revisionMessages.add(DiscussionMessage.ai(startMessage, timeLabel()));
            currentRun.logs.add(log("OpenSpec", "info", startMessage));
            persistSnapshotQuietly();
        }

        OpenSpecRunResult result;
        try {
            result = claudeCodeService.runOpenSpecAction(action, context);
        } catch (Exception error) {
            synchronized (this) {
                if (currentRun.review == null) {
                    return currentRun.toWorkflowStatus();
                }
                String proposalStatus = currentRun.revision == null ? "未创建" : currentRun.revision.proposalStatus();
                String applyStatus = currentRun.revision == null ? "未执行" : currentRun.revision.applyStatus();
                String archiveStatus = currentRun.revision == null ? "未归档" : currentRun.revision.archiveStatus();
                String failureStatus = switch (action) {
                    case "propose" -> "创建失败";
                    case "apply" -> "执行失败";
                    case "archive" -> "归档失败";
                    default -> currentRun.revision == null ? "未创建" : currentRun.revision.proposalStatus();
                };
                if ("propose".equals(action) || "explore".equals(action)) {
                    proposalStatus = failureStatus;
                } else if ("apply".equals(action)) {
                    applyStatus = failureStatus;
                } else if ("archive".equals(action)) {
                    archiveStatus = failureStatus;
                }
                currentRun.revision = revisionContext(currentRun.review.stageKey(), currentRun.review.stageTitle(), currentRun.reviewFeedback, context.changeId(), "feedback_submitted", proposalStatus, applyStatus, archiveStatus);
                currentRun.review = buildReviewState(currentRun.review.stageKey(), currentRun.review.stageTitle(), currentRun.nextStageIndex);
                String message = "OpenSpec " + actionLabel(action) + "未完成：" + error.getMessage() + "。请确认 Claude Runner/CLI 是否仍在运行，然后可重新点击该操作。";
                String failureLogMessage = actionLabel(action) + "阶段修订失败：" + currentRun.review.stageTitle();
                currentRun.revisionMessages.add(DiscussionMessage.ai(message));
                currentRun.logs.add(log("OpenSpec", "error", failureLogMessage));
                persistSnapshotQuietly();
                return currentRun.toWorkflowStatus();
            }
        }

        synchronized (this) {
            if (currentRun.review == null) {
                return currentRun.toWorkflowStatus();
            }
            String stageKey = currentRun.review.stageKey();
            String stageTitle = currentRun.review.stageTitle();
            String changeId = result != null && result.changeId() != null && !result.changeId().isBlank() ? result.changeId() : context.changeId();
            String proposalStatus = currentRun.revision == null ? "未创建" : currentRun.revision.proposalStatus();
            String applyStatus = currentRun.revision == null ? "未执行" : currentRun.revision.applyStatus();
            String archiveStatus = currentRun.revision == null ? "未归档" : currentRun.revision.archiveStatus();
            String content = result == null || result.content() == null || result.content().isBlank() ? "OpenSpec " + actionLabel(action) + "已完成。" : result.content();
            List<String> options = extractOpenSpecOptions(action, content);

            if (options.isEmpty()) {
                switch (action) {
                    case "explore" -> proposalStatus = proposalStatus == null ? "未创建" : proposalStatus;
                    case "propose" -> proposalStatus = "已创建";
                    case "apply" -> applyStatus = "已执行";
                    case "archive" -> archiveStatus = "已归档";
                    default -> throw new IllegalArgumentException("不支持的 OpenSpec 操作：" + action);
                }
            }

            currentRun.revision = revisionContext(stageKey, stageTitle, currentRun.reviewFeedback, changeId, options.isEmpty() ? completedActionStatus(action) : "revision_waiting_choice", proposalStatus, applyStatus, archiveStatus);
            String completionLogMessage = options.isEmpty() ? "完成" + actionLabel(action) + "阶段修订：" + stageTitle : actionLabel(action) + "阶段修订等待选择：" + stageTitle;
            currentRun.revisionMessages.add(DiscussionMessage.ai(content, options));
            if ("archive".equals(action) && options.isEmpty()) {
                currentRun.revisionMessages.add(DiscussionMessage.ai("归档已完成，前面的确认选项仅用于本次归档决策，可忽略历史提示。"));
            }
            currentRun.review = buildReviewState(stageKey, stageTitle, currentRun.nextStageIndex);
            currentRun.logs.add(log("OpenSpec", "info", completionLogMessage));
            persistSnapshotQuietly();
            return currentRun.toWorkflowStatus();
        }
    }

    private List<String> extractOpenSpecOptions(String action, String content) {
        if (content == null || content.isBlank()) {
            return List.of();
        }
        List<String> options = content.lines()
                .map(String::strip)
                .filter(line -> line.matches("^\\d+[.)、]\\s+.+"))
                .map(line -> line.replaceFirst("^\\d+[.)、]\\s+", "").strip())
                .filter(line -> !line.isBlank() && line.length() <= 120)
                .toList();
        if (options.size() >= 2 && options.size() <= 4) {
            return options;
        }
        if ("archive".equals(action) && requiresArchiveSyncChoice(content)) {
            return ARCHIVE_SYNC_OPTIONS;
        }
        return List.of();
    }

    private boolean requiresArchiveSyncChoice(String content) {
        return content.contains("delta spec")
                && content.contains("主规格")
                && content.contains("请选择")
                && content.contains("同步规格")
                && content.contains("归档");
    }

    private String actionStatus(String action) {
        return switch (action) {
            case "explore" -> "revision_discussing";
            case "propose" -> "revision_proposing";
            case "apply" -> "revision_applying";
            case "archive" -> "revision_archiving";
            default -> throw new IllegalArgumentException("不支持的 OpenSpec 操作：" + action);
        };
    }

    private String completedActionStatus(String action) {
        return switch (action) {
            case "explore" -> "revision_explored";
            case "propose" -> "revision_proposed";
            case "apply" -> "revision_applied";
            case "archive" -> "revision_archived";
            default -> throw new IllegalArgumentException("不支持的 OpenSpec 操作：" + action);
        };
    }

    private String actionLabel(String action) {
        return switch (action) {
            case "explore" -> "探索修订";
            case "propose" -> "制定计划";
            case "apply" -> "执行计划";
            case "archive" -> "归档";
            default -> action;
        };
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdownNow();
    }

    private void runPipeline(String taskId) {
        try {
            addLog(taskId, "Orchestrator", "info", "任务已创建");
            runPipelineFrom(taskId, 0);
        } catch (Exception exception) {
            markError(taskId, exception.getMessage() == null ? "Claude Runner 执行失败" : exception.getMessage());
        }
    }

    private void runPipelineFrom(String taskId, int startIndex) {
        try {
            ClaudeRunResult productDesignResult = null;
            for (int index = startIndex; index < STAGES.size(); index++) {
                WorkflowStageDefinition stage = STAGES.get(index);
                startStage(taskId, stage);
                if ("prd".equals(stage.runnerMode())) {
                    ClaudeRunResult result = claudeCodeService.runTask(taskId, currentSessionId(taskId), "prd", currentRequirement(taskId), stage.agentId());
                    productDesignResult = result;
                    completeStage(taskId, stage, result);
                } else if ("ui".equals(stage.runnerMode())) {
                    if (productDesignResult == null) {
                        productDesignResult = currentProductDesignResult(taskId);
                    }
                    ClaudeRunResult result = runUiStageWithRecovery(taskId, stage, productDesignResult);
                    completeStage(taskId, stage, result);
                } else if ("architecture".equals(stage.runnerMode()) || "api".equals(stage.runnerMode()) || "database".equals(stage.runnerMode())) {
                    ClaudeRunResult result = claudeCodeService.runTask(taskId, currentSessionId(taskId), stage.runnerMode(), buildDesignPrompt(taskId, stage), stage.agentId());
                    completeStage(taskId, stage, result);
                } else if ("generate".equals(stage.runnerMode())) {
                    ClaudeRunResult result = claudeCodeService.runGenerate(taskId, currentSessionId(taskId), buildFrontendPrompt(taskId), stage.agentId());
                    completeStage(taskId, stage, result);
                } else if ("backend".equals(stage.runnerMode())) {
                    ClaudeRunResult result = claudeCodeService.runBackend(taskId, currentSessionId(taskId), buildBackendPrompt(taskId), stage.agentId());
                    completeStage(taskId, stage, result);
                } else if ("dev-integration".equals(stage.runnerMode())) {
                    ClaudeRunResult result = claudeCodeService.runPlaywright(taskId, currentSessionId(taskId), buildDevelopmentIntegrationPrompt(taskId), stage.agentId());
                    completeStage(taskId, stage, result);
                } else if ("test-cases".equals(stage.runnerMode())) {
                    ClaudeRunResult result = claudeCodeService.runTestCases(taskId, currentSessionId(taskId), buildTestCasesPrompt(taskId), stage.agentId());
                    completeStage(taskId, stage, result);
                } else if ("playwright".equals(stage.runnerMode())) {
                    ClaudeRunResult result = claudeCodeService.runPlaywright(taskId, currentSessionId(taskId), buildPlaywrightPrompt(taskId), stage.agentId());
                    completeStage(taskId, stage, result);
                } else if ("fix-tests".equals(stage.runnerMode())) {
                    ClaudeRunResult result = runFixLoop(taskId, stage.agentId());
                    completeStage(taskId, stage, result);
                } else {
                    completePlaceholderStage(taskId, stage);
                }
                if (isReviewable(stage)) {
                    enterReviewAndWait(taskId, stage);
                }
                sleepQuietly(120L);
            }
            completeWorkflow(taskId);
        } catch (Exception exception) {
            markError(taskId, exception.getMessage() == null ? "Claude Runner 执行失败" : exception.getMessage());
        }
    }

    private synchronized ClaudeRunResult currentProductDesignResult(String taskId) {
        if (!isCurrent(taskId) || currentRun.result == null || currentRun.result.artifacts() == null) {
            return null;
        }
        List<String> artifacts = currentRun.result.artifacts().stream()
                .filter(artifact -> reviewArtifactMatches("product-design-artifacts", artifact))
                .map(ArtifactView::path)
                .toList();
        if (artifacts.isEmpty()) {
            return null;
        }
        String docsProjectDir = artifacts.stream()
                .map(this::projectRootFromArtifactPath)
                .filter(Objects::nonNull)
                .findFirst()
                .map(path -> path.resolve("产品设计").toString())
                .orElse(null);
        return new ClaudeRunResult(taskId, currentSessionId(taskId), "recovered", "需求产物已从当前流程结果恢复。", artifacts, null, null, docsProjectDir, currentRun.result.projectName());
    }

    private ClaudeRunResult runUiStageWithRecovery(String taskId, WorkflowStageDefinition stage, ClaudeRunResult productDesignResult) {
        try {
            return claudeCodeService.runTask(taskId, currentSessionId(taskId), "ui", buildUiDesignPrompt(taskId, productDesignResult), stage.agentId());
        } catch (RuntimeException error) {
            if (!isProbablyHttpTimeout(error)) {
                throw error;
            }
            addLog(taskId, stage.agentName(), "warn", "UI 设计阶段 HTTP 返回超时，开始根据已落盘产物尝试恢复阶段状态");
            ClaudeRunResult recovered = recoverUiStageResult(taskId, productDesignResult);
            if (recovered == null) {
                addLog(taskId, stage.agentName(), "warn", "未扫描到完整 UI 原型产物，无法执行补偿恢复");
                throw error;
            }
            addLog(taskId, stage.agentName(), "info", "已根据 UI 原型文件恢复 UI 设计阶段结果");
            return recovered;
        }
    }

    private boolean isProbablyHttpTimeout(Throwable error) {
        Throwable current = error;
        while (current != null) {
            if (current instanceof SocketTimeoutException || current instanceof HttpTimeoutException) {
                return true;
            }
            String message = current.getMessage();
            if (message != null) {
                String normalized = message.toLowerCase(Locale.ROOT);
                if (normalized.contains("read timed out") || normalized.contains("timed out") || normalized.contains("timeout")) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }

    private ClaudeRunResult recoverUiStageResult(String taskId, ClaudeRunResult productDesignResult) {
        Path docsProjectRoot = resolveDocsProjectRoot(productDesignResult);
        if (docsProjectRoot == null) {
            return null;
        }

        for (int attempt = 1; attempt <= UI_RECOVERY_SCAN_ATTEMPTS; attempt++) {
            List<String> artifacts = scanExistingUiArtifacts(docsProjectRoot);
            if (hasMinimumUiArtifacts(artifacts)) {
                return new ClaudeRunResult(
                        taskId,
                        currentSessionId(taskId),
                        "recovered",
                        "UI 设计阶段 HTTP 返回超时，已根据已落盘 UI 原型产物恢复。",
                        artifacts,
                        productDesignResult == null ? null : productDesignResult.workspaceDir(),
                        productDesignResult == null ? null : productDesignResult.projectDir(),
                        docsProjectRoot.resolve("产品设计").toString(),
                        productDesignResult == null ? null : productDesignResult.projectName()
                );
            }
            if (attempt < UI_RECOVERY_SCAN_ATTEMPTS) {
                sleepQuietly(UI_RECOVERY_SCAN_DELAY_MILLIS);
            }
        }
        return null;
    }

    private Path runtimeRoot() {
        return Path.of(config.workspaceRoot());
    }

    private Path snapshotPath(String taskId) {
        return runtimeRoot().resolve(taskId).resolve("workflow-run.json");
    }

    private synchronized void persistSnapshotQuietly() {
        if (currentRun == null || currentRun.taskId == null || "idle".equals(currentRun.taskId)) {
            return;
        }
        try {
            Path path = snapshotPath(currentRun.taskId);
            Files.createDirectories(path.getParent());
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), toSnapshot());
        } catch (Exception ignored) {
        }
    }

    private WorkflowRunSnapshot toSnapshot() {
        return new WorkflowRunSnapshot(
                1,
                currentRun.taskId,
                currentRun.requirement,
                currentRun.sessionId,
                currentRun.status,
                currentRun.currentStage,
                currentRun.currentArtifactType,
                currentRun.designProgressMessage,
                currentRun.progress,
                currentRun.estimatedRemaining,
                currentRun.estimatedCompletion,
                currentRun.error,
                Instant.now().toString(),
                currentRun.result,
                List.copyOf(currentRun.logs),
                List.copyOf(currentRun.steps),
                List.copyOf(currentRun.agents),
                currentRun.review,
                currentRun.revision,
                currentRun.reviewFeedback,
                List.copyOf(currentRun.revisionMessages),
                currentRun.nextStageIndex,
                List.copyOf(currentRun.approvedStageKeys)
        );
    }

    private List<WorkflowRunSnapshot> snapshots() {
        Path root = runtimeRoot();
        if (!Files.isDirectory(root)) {
            return List.of();
        }
        try (var stream = Files.walk(root, 2)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(path -> "workflow-run.json".equals(path.getFileName().toString()))
                    .map(this::readSnapshot)
                    .filter(Objects::nonNull)
                    .toList();
        } catch (IOException ignored) {
            return List.of();
        }
    }

    private WorkflowRunSnapshot readSnapshot(Path path) {
        try {
            return objectMapper.readValue(path.toFile(), WorkflowRunSnapshot.class);
        } catch (IOException ignored) {
            return null;
        }
    }

    private WorkflowRunSnapshot loadLatestSnapshot() {
        return snapshots().stream()
                .filter(this::snapshotHasExistingGeneratedProject)
                .max(Comparator.comparing(snapshot -> snapshot.updatedAt() == null ? "" : snapshot.updatedAt()))
                .orElse(null);
    }

    private boolean snapshotHasExistingGeneratedProject(WorkflowRunSnapshot snapshot) {
        return snapshot != null
                && snapshot.result() != null
                && snapshot.result().projectName() != null
                && hasExistingGeneratedProject(snapshot.result().projectName());
    }

    private WorkflowRunSnapshot latestSnapshotForProject(String projectName) {
        return snapshots().stream()
                .filter(snapshot -> snapshot.result() != null)
                .filter(snapshot -> Objects.equals(snapshot.result().projectName(), projectName))
                .max(Comparator.comparing(snapshot -> snapshot.updatedAt() == null ? "" : snapshot.updatedAt()))
                .orElse(null);
    }

    private boolean snapshotArtifactsAreValidForProject(String projectName) {
        if (currentRun.result == null || currentRun.result.artifacts() == null) {
            return false;
        }
        String expectedSegment = "/docs/" + projectName + "/";
        return currentRun.result.artifacts().stream()
                .filter(artifact -> artifact.path() != null && artifact.path().contains("/api/artifacts?path="))
                .allMatch(artifact -> {
                    String path = decodeArtifactPath(artifact.path());
                    return path.contains(expectedSegment) && Files.isRegularFile(Path.of(path));
                });
    }

    private boolean snapshotCodeArtifactsAreCurrent(String projectName) {
        boolean generatedCodeExists = !scanExistingCodeArtifacts(projectName).isEmpty();
        boolean snapshotHasCode = currentRun.result != null
                && currentRun.result.artifacts() != null
                && currentRun.result.artifacts().stream().anyMatch(artifact -> "code".equals(artifact.stage()));
        return !generatedCodeExists || snapshotHasCode;
    }

    private boolean snapshotRecoveredStagesAreCurrent(String projectName) {
        List<String> frontendArtifacts = scanExistingFrontendArtifacts(projectName);
        List<String> backendArtifacts = scanExistingBackendArtifacts(projectName);
        Path docsProjectRoot = resolveExistingDocsProjectRoot(projectName);
        List<String> testArtifacts = scanDirectoryArtifacts(docsProjectRoot.resolve("测试"));
        return (backendArtifacts.isEmpty() || stepIsCompleted("backend-development"))
                && (frontendArtifacts.isEmpty() || stepIsCompleted("frontend-development"))
                && (frontendArtifacts.isEmpty() || backendArtifacts.isEmpty() || testArtifacts.isEmpty() || stepIsCompleted("development-integration"));
    }

    private boolean stepIsCompleted(String stageKey) {
        return currentRun.steps.stream()
                .anyMatch(step -> Objects.equals(step.key(), stageKey) && step.progress() == 100 && !"pending".equals(step.status()));
    }

    private String decodeArtifactPath(String artifactUrl) {
        int index = artifactUrl.indexOf("path=");
        String encoded = index >= 0 ? artifactUrl.substring(index + "path=".length()) : artifactUrl;
        return URLDecoder.decode(encoded, StandardCharsets.UTF_8);
    }

    private List<ProjectSummary> snapshotSummaries() {
        return snapshots().stream()
                .filter(this::snapshotHasExistingGeneratedProject)
                .filter(snapshot -> snapshot.result() != null && snapshot.result().projectName() != null && !snapshot.result().projectName().isBlank())
                .map(snapshot -> new ProjectSummary(
                        snapshot.taskId(),
                        snapshot.result().projectName(),
                        snapshotPath(snapshot.taskId()).toString(),
                        snapshot.updatedAt(),
                        hasStageArtifacts(snapshot.result(), "product-design-artifacts") || hasStageArtifacts(snapshot.result(), "requirement"),
                        hasStageArtifacts(snapshot.result(), "ui-design") || hasStageArtifacts(snapshot.result(), "ui-prototype")
                ))
                .toList();
    }

    private List<ProjectSummary> docsProjectSummaries() {
        return docsRootCandidates().stream()
                .filter(Files::isDirectory)
                .flatMap(root -> {
                    try {
                        return Files.list(root);
                    } catch (IOException ignored) {
                        return java.util.stream.Stream.<Path>empty();
                    }
                })
                .filter(Files::isDirectory)
                .map(this::projectSummary)
                .filter(Objects::nonNull)
                .filter(project -> project.hasProductArtifacts() || project.hasUiArtifacts())
                .toList();
    }

    private boolean hasStageArtifacts(ResultView result, String stage) {
        return result.artifacts() != null && result.artifacts().stream().anyMatch(artifact -> Objects.equals(artifact.stage(), stage));
    }

    private void restoreFromSnapshot(WorkflowRunSnapshot snapshot, boolean startupRestore) {
        WorkflowRun restored = new WorkflowRun(
                snapshot.taskId(),
                snapshot.requirement(),
                Instant.now(),
                EXAMPLES,
                new ArrayList<>(snapshot.logs() == null ? List.of() : snapshot.logs()),
                new ArrayList<>(snapshot.steps() == null ? WorkflowRun.stageStatuses() : snapshot.steps()),
                new ArrayList<>(snapshot.agents() == null ? WorkflowRun.stageAgents() : snapshot.agents()),
                snapshot.sessionId(),
                snapshot.status(),
                snapshot.currentStage(),
                snapshot.currentArtifactType(),
                snapshot.designProgressMessage(),
                snapshot.progress(),
                snapshot.estimatedRemaining(),
                snapshot.estimatedCompletion(),
                snapshot.error(),
                snapshot.result() == null ? new ResultView(false, false, null, null, null, null, null, List.of(), List.of(), List.of(), List.of(), List.of()) : snapshot.result()
        );
        restored.review = snapshot.review();
        restored.revision = snapshot.revision();
        restored.reviewFeedback = snapshot.reviewFeedback() == null ? "" : snapshot.reviewFeedback();
        restored.revisionMessages.addAll(snapshot.revisionMessages() == null ? List.of() : snapshot.revisionMessages());
        normalizeRestoredRevisionState(restored);
        restored.nextStageIndex = snapshot.nextStageIndex();
        restored.approvedStageKeys.addAll(snapshot.approvedStageKeys() == null ? List.of() : snapshot.approvedStageKeys());
        currentRun = restored;
        markApprovedStagesAfterSnapshotRestore();
        rebuildReviewAfterSnapshotRestore();
        restoreMissingReviewFromAwaitingStep();
        if (startupRestore && "running".equals(currentRun.status)) {
            currentRun.status = "error";
            currentRun.error = "服务重启后检测到流程中断，请从历史项目恢复或重新发起当前阶段。";
            currentRun.estimatedRemaining = "--";
            currentRun.estimatedCompletion = "已中断";
            currentRun.designProgressMessage = "服务重启，流程已中断，未自动重跑当前阶段";
            currentRun.logs.add(log("Orchestrator", "error", currentRun.error));
            for (int i = 0; i < currentRun.steps.size(); i++) {
                StepStatus step = currentRun.steps.get(i);
                if ("running".equals(step.status())) {
                    currentRun.steps.set(i, new StepStatus(step.index(), step.key(), step.title(), "error", step.progress(), step.title() + "已中断", elapsedLabel(currentRun.startedAt), currentRun.error));
                    updateAgent(currentRun.taskId, step.key(), "error", step.progress());
                    break;
                }
            }
            persistSnapshotQuietly();
        }
    }

    private void markApprovedStagesAfterSnapshotRestore() {
        for (String stageKey : currentRun.approvedStageKeys) {
            if (currentRun.review != null && Objects.equals(currentRun.review.stageKey(), stageKey)) {
                continue;
            }
            WorkflowStageDefinition stage = stageDefinition(stageKey);
            if (stage != null) {
                updateStep(currentRun.taskId, stageKey, "success", 100, stage.title() + "已确认", elapsedLabel(currentRun.startedAt), null);
                updateAgent(currentRun.taskId, stageKey, "success", 100);
            }
        }
    }

    private void normalizeRestoredRevisionState(WorkflowRun restored) {
        if (restored.revision == null) {
            return;
        }
        String status = restored.revision.status();
        boolean hasAssistantResult = restored.revisionMessages.stream().anyMatch(message -> "ai".equals(message.role()) && isRevisionResultMessage(message.content()));
        String normalizedStatus = status;
        if ("revision_discussing".equals(status) && hasAssistantResult) {
            normalizedStatus = "revision_explored";
        } else if ("revision_proposing".equals(status) && hasAssistantResult) {
            normalizedStatus = restored.revision.changeId() == null || restored.revision.changeId().isBlank() ? "feedback_submitted" : "revision_proposed";
        } else if ("revision_applying".equals(status) && hasAssistantResult) {
            normalizedStatus = "revision_applied";
        } else if ("revision_archiving".equals(status) && hasAssistantResult) {
            normalizedStatus = "revision_archived";
        }
        if (!Objects.equals(status, normalizedStatus)) {
            restored.revision = revisionContext(restored.revision.stageKey(), restored.revision.stageTitle(), restored.reviewFeedback, restored.revision.changeId(), normalizedStatus, restored.revision.proposalStatus(), restored.revision.applyStatus(), restored.revision.archiveStatus());
        }
    }

    private boolean isRevisionResultMessage(String content) {
        if (content == null || content.isBlank()) {
            return false;
        }
        if (content.startsWith("正在")) {
            return false;
        }
        return !content.matches("^开始.+阶段修订：.+$");
    }

    private void rebuildReviewAfterSnapshotRestore() {
        if (currentRun.review == null) {
            return;
        }
        currentRun.review = buildReviewState(currentRun.review.stageKey(), currentRun.review.stageTitle(), currentRun.nextStageIndex);
    }

    private void ensureReviewStateReady() {
        if (currentRun.review != null && !"awaiting_review".equals(currentRun.status)) {
            currentRun.status = "awaiting_review";
        }
        restoreMissingReviewFromAwaitingStep();
    }

    private void restoreMissingReviewFromAwaitingStep() {
        boolean hasAwaitingStep = currentRun.steps.stream().anyMatch(step -> "awaiting_review".equals(step.status()));
        if (currentRun.review != null || !hasAwaitingStep) {
            return;
        }
        currentRun.steps.stream()
                .filter(step -> "awaiting_review".equals(step.status()))
                .findFirst()
                .map(StepStatus::key)
                .map(this::stageDefinition)
                .ifPresent(stage -> {
                    currentRun.status = "awaiting_review";
                    currentRun.currentStage = stage.title();
                    currentRun.currentArtifactType = stage.artifactType();
                    currentRun.designProgressMessage = stage.title() + " 已从历史项目恢复，请确认产物或提交修订反馈";
                    currentRun.estimatedRemaining = "等待确认";
                    currentRun.estimatedCompletion = "等待用户确认";
                    currentRun.error = null;
                    currentRun.nextStageIndex = stageIndex(stage.key()) + 1;
                    currentRun.review = buildReviewState(stage.key(), stage.title(), currentRun.nextStageIndex);
                });
    }

    private Path resolveDocsProjectRoot(ClaudeRunResult productDesignResult) {
        if (productDesignResult == null) {
            return null;
        }
        Path fromDocsProjectDir = projectRootFromArtifactPath(productDesignResult.docsProjectDir());
        if (fromDocsProjectDir != null) {
            return fromDocsProjectDir;
        }
        if (productDesignResult.artifacts() == null) {
            return null;
        }
        for (String artifact : productDesignResult.artifacts()) {
            Path projectRoot = projectRootFromArtifactPath(artifact);
            if (projectRoot != null) {
                return projectRoot;
            }
        }
        return null;
    }

    private Path projectRootFromArtifactPath(String artifactPath) {
        if (artifactPath == null || artifactPath.isBlank()) {
            return null;
        }
        String normalized = artifactPath.replace('\\', '/');
        int productDesignIndex = normalized.indexOf("/产品设计");
        int uiPrototypeIndex = normalized.indexOf("/UI原型");
        int splitIndex = productDesignIndex >= 0 ? productDesignIndex : uiPrototypeIndex;
        if (splitIndex >= 0) {
            return Path.of(normalized.substring(0, splitIndex));
        }
        if (normalized.endsWith("产品设计")) {
            return Path.of(normalized.substring(0, normalized.length() - "产品设计".length()));
        }
        if (normalized.endsWith("UI原型")) {
            return Path.of(normalized.substring(0, normalized.length() - "UI原型".length()));
        }
        return null;
    }

    private Path resolveExistingDocsProjectRoot(String projectName) {
        List<Path> candidates = docsRootCandidates().stream()
                .map(root -> root.resolve(projectName))
                .toList();
        return candidates.stream()
                .filter(Files::isDirectory)
                .findFirst()
                .orElse(candidates.get(0));
    }

    private List<Path> docsRootCandidates() {
        return List.of(
                Path.of("docs"),
                Path.of(System.getProperty("user.dir")).resolve("docs"),
                Path.of(System.getProperty("user.dir")).resolve("../..").normalize().resolve("docs"),
                Path.of(config.workspaceRoot()).resolve("docs")
        );
    }

    private ProjectSummary projectSummary(Path projectRoot) {
        try {
            String projectName = projectRoot.getFileName().toString();
            return new ProjectSummary(
                    projectName,
                    projectName,
                    projectRoot.toString(),
                    Files.getLastModifiedTime(projectRoot).toInstant().toString(),
                    !scanExistingProductArtifacts(projectRoot).isEmpty(),
                    hasMinimumUiArtifacts(scanExistingUiArtifacts(projectRoot))
            );
        } catch (IOException ignored) {
            return null;
        }
    }

    private List<String> scanExistingProductArtifacts(Path docsProjectRoot) {
        List<String> relativePaths = List.of(
                "产品设计/01-变更记录/版本说明.md",
                "产品设计/02-产品需求文档/PRD.md",
                "产品设计/03-UI设计规范/UI-Design-Spec.md",
                "产品设计/04-流程图/业务流程图.puml",
                "产品设计/04-流程图/信息架构图.puml",
                "产品设计/04-流程图/页面流转图.puml",
                "产品设计/05-附录/术语表.md"
        );
        List<String> artifacts = new ArrayList<>();
        for (String relativePath : relativePaths) {
            Path artifactPath = docsProjectRoot.resolve(relativePath);
            if (Files.isRegularFile(artifactPath)) {
                artifacts.add(artifactPath.toString());
            }
        }
        return artifacts;
    }

    private List<String> scanExistingUiArtifacts(Path docsProjectRoot) {
        List<String> artifacts = new ArrayList<>();
        Path uiDesignSpec = docsProjectRoot.resolve("产品设计/03-UI设计规范/UI-Design-Spec.md");
        if (Files.isRegularFile(uiDesignSpec)) {
            artifacts.add(uiDesignSpec.toString());
        }
        for (String relativePath : UI_ARTIFACT_RELATIVE_PATHS) {
            Path artifactPath = docsProjectRoot.resolve(relativePath);
            if (Files.isRegularFile(artifactPath)) {
                artifacts.add(artifactPath.toString());
            }
        }
        return artifacts;
    }

    private boolean hasMinimumUiArtifacts(List<String> artifacts) {
        if (artifacts == null || artifacts.isEmpty()) {
            return false;
        }
        return REQUIRED_UI_ARTIFACT_RELATIVE_PATHS.stream()
                .allMatch(requiredPath -> artifacts.stream().anyMatch(artifact -> artifact.replace('\\', '/').endsWith(requiredPath)));
    }

    private List<String> scanDirectoryArtifacts(Path directory) {
        if (!Files.isDirectory(directory)) {
            return List.of();
        }
        try (var stream = Files.walk(directory, 8)) {
            return stream
                    .filter(Files::isRegularFile)
                    .map(Path::toString)
                    .sorted()
                    .toList();
        } catch (IOException ignored) {
            return List.of();
        }
    }

    private List<String> scanExistingCodeArtifacts(String projectName) {
        List<String> artifacts = new ArrayList<>();
        artifacts.addAll(scanExistingFrontendArtifacts(projectName));
        artifacts.addAll(scanExistingBackendArtifacts(projectName));
        return artifacts;
    }

    private List<String> scanExistingFrontendArtifacts(String projectName) {
        Path frontendDir = generatedProjectRoot(projectName).resolve("frontend");
        return Files.isRegularFile(frontendDir.resolve("package.json")) ? List.of(frontendDir.toString()) : List.of();
    }

    private List<String> scanExistingBackendArtifacts(String projectName) {
        Path backendDir = generatedProjectRoot(projectName).resolve("backend");
        if (Files.isRegularFile(backendDir.resolve("pom.xml")) || Files.isRegularFile(backendDir.resolve("README.md"))) {
            return List.of(backendDir.toString());
        }
        return List.of();
    }

    private Path generatedProjectRoot(String projectName) {
        String generatedProjectName = generatedProjectName(projectName);
        List<Path> candidates = generatedRootCandidates().stream()
                .map(root -> root.resolve(generatedProjectName).normalize())
                .toList();
        return candidates.stream()
                .filter(Files::isDirectory)
                .findFirst()
                .orElseGet(() -> existingGeneratedProjectRoot(projectName).orElse(candidates.get(0)));
    }

    private boolean hasExistingGeneratedProject(String projectName) {
        return existingGeneratedProjectRoot(projectName).isPresent();
    }

    private Optional<Path> existingGeneratedProjectRoot(String projectName) {
        for (Path root : generatedRootCandidates()) {
            if (!Files.isDirectory(root)) {
                continue;
            }
            try (var stream = Files.list(root)) {
                Optional<Path> matched = stream
                        .filter(Files::isDirectory)
                        .filter(path -> !"Generated-App".equals(path.getFileName().toString()))
                        .filter(this::hasGeneratedProjectArtifacts)
                        .filter(path -> generatedProjectMatches(path, projectName))
                        .findFirst();
                if (matched.isPresent()) {
                    return matched;
                }
            } catch (IOException ignored) {
            }
        }
        return Optional.empty();
    }

    private boolean hasGeneratedProjectArtifacts(Path projectRoot) {
        return Files.isRegularFile(projectRoot.resolve("frontend/package.json"))
                || Files.isRegularFile(projectRoot.resolve("backend/pom.xml"))
                || Files.isRegularFile(projectRoot.resolve("backend/README.md"));
    }

    private boolean generatedProjectMatches(Path projectRoot, String projectName) {
        String rootName = projectRoot.getFileName().toString();
        String generatedName = generatedProjectName(projectName);
        return Objects.equals(rootName, generatedName)
                || rootName.equalsIgnoreCase(generatedName)
                || rootName.equalsIgnoreCase(projectName == null ? "" : projectName.replaceAll("\\s+", "-"));
    }

    private List<Path> generatedRootCandidates() {
        return List.of(
                Path.of("generated"),
                Path.of(System.getProperty("user.dir")).resolve("generated"),
                Path.of(System.getProperty("user.dir")).resolve("../..").normalize().resolve("generated")
        );
    }

    private String generatedProjectName(String projectName) {
        String normalized = projectName == null ? "" : projectName.strip();
        if ("移动端应用：会议室预约系统".equals(normalized) || "HX-Meeting 会议室预约系统".equals(normalized) || "会议室预约系统".equals(normalized)) {
            return "HX-Meeting";
        }
        String ascii = normalized
                .replaceAll("[^a-zA-Z0-9\\s_-]", " ")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
        if (!ascii.isBlank()) {
            return ascii;
        }
        int hash = Math.abs(normalized.hashCode());
        return "Project-" + Integer.toString(hash, 36);
    }

    private ResultView recoveredResult(String projectName, Path docsProjectRoot, List<String> productArtifacts, List<String> uiArtifacts, List<String> architectureArtifacts, List<String> databaseArtifacts, List<String> codeArtifacts, List<String> testArtifacts, List<String> miscArtifacts) {
        List<ArtifactView> artifacts = new ArrayList<>();
        artifacts.addAll(deliveryArtifactViews("product-design-artifacts", productArtifacts));
        artifacts.addAll(deliveryArtifactViews("ui-design", uiArtifacts));
        artifacts.addAll(deliveryArtifactViews("architecture-design", architectureArtifacts));
        artifacts.addAll(deliveryArtifactViews("database-design", databaseArtifacts));
        artifacts.addAll(deliveryArtifactViews("code", codeArtifacts));
        artifacts.addAll(deliveryArtifactViews("verification-before-completion", testArtifacts));
        artifacts.addAll(deliveryArtifactViews("misc", miscArtifacts));
        String reportUrl = testArtifacts.stream()
                .filter(path -> path.replace('\\', '/').endsWith("test-report.md"))
                .findFirst()
                .map(this::artifactUrl)
                .orElse(null);
        return new ResultView(
                !artifacts.isEmpty(),
                !productArtifacts.isEmpty() || !uiArtifacts.isEmpty(),
                projectName,
                codeArtifacts.isEmpty() ? null : artifactUrl(generatedProjectRoot(projectName).toString()),
                reportUrl,
                null,
                summaryMarkdown(projectName, artifacts),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.copyOf(artifacts)
        );
    }

    private void applyRecoveredProgress(List<String> productArtifacts, List<String> uiArtifacts, List<String> architectureArtifacts, List<String> databaseArtifacts, List<String> frontendArtifacts, List<String> backendArtifacts, List<String> testArtifacts) {
        if (!productArtifacts.isEmpty()) {
            markRecoveredStage("requirements", "需求摘要整理完成");
            markRecoveredStage("brainstorming", "需求讨论结论整理完成");
            markRecoveredStage("writing-plans", "阶段化实现计划整理完成");
            markRecoveredStage("product-design-artifacts", "需求阶段产物生成完成");
        }
        if (hasMinimumUiArtifacts(uiArtifacts)) {
            markRecoveredStage("ui-design", "UI 设计产物生成完成");
        }
        if (!architectureArtifacts.isEmpty()) {
            markRecoveredStage("architecture-design", "架构设计产物生成完成");
            markRecoveredStage("api-design", "接口设计产物生成完成");
        }
        if (!databaseArtifacts.isEmpty()) {
            markRecoveredStage("database-design", "数据库设计产物生成完成");
        }
        if (!backendArtifacts.isEmpty()) {
            markRecoveredStage("backend-development", "后端代码已从 generated 目录恢复");
        }
        if (!frontendArtifacts.isEmpty()) {
            markRecoveredStage("frontend-development", "前端代码已从 generated 目录恢复");
        }
        if (!frontendArtifacts.isEmpty() && !backendArtifacts.isEmpty() && !testArtifacts.isEmpty()) {
            markRecoveredStage("development-integration", "开发联调完成");
        }
        if (!testArtifacts.isEmpty()) {
            markRecoveredStage("test-case-generation", "测试用例生成完成");
            markRecoveredStage("e2e-acceptance-testing", "E2E 验收测试完成");
            markRecoveredStage("verification-before-completion", "完成前验证与测试修复完成");
        }

        String reviewStageKey = recoveredReviewStage(productArtifacts, uiArtifacts, architectureArtifacts, databaseArtifacts, frontendArtifacts, backendArtifacts, testArtifacts);
        WorkflowStageDefinition reviewStage = stageDefinition(reviewStageKey);
        currentRun.status = "awaiting_review";
        currentRun.currentStage = reviewStage.title();
        currentRun.currentArtifactType = reviewStage.artifactType();
        currentRun.designProgressMessage = reviewStage.title() + " 已从历史项目恢复，请确认产物或提交修订反馈";
        currentRun.progress = reviewStage.progress();
        currentRun.estimatedRemaining = "等待确认";
        currentRun.estimatedCompletion = "等待用户确认";
        currentRun.nextStageIndex = stageIndex(reviewStageKey) + 1;
        currentRun.approvedStageKeys.remove(reviewStageKey);
        updateStep(currentRun.taskId, reviewStageKey, "awaiting_review", 100, "等待用户确认", elapsedLabel(currentRun.startedAt), null);
        updateAgent(currentRun.taskId, reviewStageKey, "success", 100);
        currentRun.review = buildReviewState(reviewStageKey, reviewStage.title(), currentRun.nextStageIndex);
    }

    private void markRecoveredStage(String stageKey, String message) {
        updateStep(currentRun.taskId, stageKey, "success", 100, message, elapsedLabel(currentRun.startedAt), null);
        updateAgent(currentRun.taskId, stageKey, "success", 100);
        currentRun.approvedStageKeys.add(stageKey);
    }

    private String recoveredReviewStage(List<String> productArtifacts, List<String> uiArtifacts, List<String> architectureArtifacts, List<String> databaseArtifacts, List<String> frontendArtifacts, List<String> backendArtifacts, List<String> testArtifacts) {
        if (!testArtifacts.isEmpty()) {
            return "verification-before-completion";
        }
        if (!frontendArtifacts.isEmpty() && !backendArtifacts.isEmpty()) {
            return testArtifacts.isEmpty() ? "development-integration" : "verification-before-completion";
        }
        if (!backendArtifacts.isEmpty()) {
            return "backend-development";
        }
        if (!databaseArtifacts.isEmpty()) {
            return "database-design";
        }
        if (!architectureArtifacts.isEmpty()) {
            return "architecture-design";
        }
        if (hasMinimumUiArtifacts(uiArtifacts)) {
            return "ui-design";
        }
        if (!productArtifacts.isEmpty()) {
            return "product-design-artifacts";
        }
        throw new IllegalStateException("未找到可恢复产物");
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

    private synchronized String buildDesignPrompt(String taskId, WorkflowStageDefinition stage) {
        StringBuilder builder = new StringBuilder();
        String projectName = currentRun.result == null || currentRun.result.projectName() == null || currentRun.result.projectName().isBlank()
                ? currentRequirement(taskId).replaceFirst("^恢复流程：", "")
                : currentRun.result.projectName();
        builder.append("目标应用名称：").append(projectName).append("\n\n");
        builder.append("原始业务需求：\n").append(currentRequirement(taskId)).append("\n\n");
        builder.append("当前任务：").append(stage.title()).append("。\n");
        builder.append("必须为目标业务应用本身设计，不要分析、复用或描述 ai-software-factory 平台、Claude Runner、apps/web、apps/server 或本仓库目录结构。\n");
        builder.append("必须优先读取并基于以下已生成产物：PRD、UI 设计规范、流程图、术语表、UI 原型、组件清单、交互说明和响应式断点参考。\n\n");
        if (currentRun.result != null && currentRun.result.artifacts() != null && !currentRun.result.artifacts().isEmpty()) {
            builder.append("已生成产物路径：\n");
            for (ArtifactView artifact : currentRun.result.artifacts()) {
                builder.append("- ").append(artifact.name()).append("：").append(artifact.path()).append("\n");
            }
            builder.append("\n");
        }
        builder.append("输出要求：\n");
        if ("architecture".equals(stage.runnerMode())) {
            builder.append("生成会议室预约系统的系统架构、模块职责、数据流、部署架构、技术选型、关键风险与约束。\n");
        } else if ("api".equals(stage.runnerMode())) {
            builder.append("生成会议室预约系统的接口清单、资源模型、请求/响应字段、错误码、分页过滤约定和 OpenAPI 设计要点。\n");
        } else if ("database".equals(stage.runnerMode())) {
            builder.append("生成会议室预约系统的数据字典、核心表、字段类型、主键外键、索引建议、DDL 和迁移脚本。\n");
        }
        return builder.toString();
    }

    private String buildFrontendPrompt(String taskId) {
        return buildImplementationPrompt(taskId, "前端开发", "请基于已生成产物实现目标业务应用的真实前端代码到 generated/{英文项目名}/frontend 目录。必须优先读取 PRD、UI 设计规范、UI 原型、组件库、交互说明、响应式断点参考、架构设计、接口设计、数据库设计，以及 generated/{英文项目名}/backend 下的 controller、DTO、entity、README、application 配置和测试。不得只基于原始需求生成通用模板。优先生成 Vue 3 / Vite 前端，必须包含 package.json、src、入口文件、页面/组件、基础样式、API client 和 README。必须基于后端真实接口实现请求，核心业务操作必须调用后端 API。完全禁止 mock、demoData、本地假数据、伪造成功响应或静态假交互；如果后端接口不可用，应显示真实错误状态。前端界面必须体现会议室查询、预约、审批、服务确认、签到、自动释放、取消、转让、违规限制、通知和管理员维护等业务对象、核心流程、表单、状态和结果展示。");
    }

    private String buildBackendPrompt(String taskId) {
        return buildImplementationPrompt(taskId, "后端开发", "请基于已生成产物实现目标业务应用的真实后端代码到 generated/{英文项目名}/backend 目录。必须优先读取 PRD、架构设计、接口设计、数据库设计、UI 交互说明和业务流程图；不得只基于原始需求生成通用 CRUD。必须生成可运行 Spring Boot 项目，遵循 controller -> service -> workflow/adapter -> repository 分层，覆盖会议室、预约、超长审批、贵宾服务确认、签到自动释放、取消、转让、违规限制、通知、管理员维护和外部 SSO/OA/化学通适配。必须使用 H2 + Flyway 作为默认本地开发配置，启动后自动建表并初始化演示数据；必须额外提供 application-prod.yml 生产 profile，使用 PostgreSQL 或 MySQL 连接配置占位，并在 README 中说明如何切换生产数据库。必须包含 README.md、pom.xml、应用入口、controller、service、repository、entity、DTO、application.yml、application-prod.yml、db/migration/V1__*.sql、db/migration/V2__*.sql 和最小测试。");
    }

    private String buildDevelopmentIntegrationPrompt(String taskId) {
        return buildImplementationPrompt(taskId, "开发联调", "请执行开发自测联调：启动 generated/{英文项目名}/backend 后端服务，启动 generated/{英文项目名}/frontend 前端服务，验证前端 API client 能访问后端真实接口，覆盖至少一个核心业务 happy path 和一个错误状态。允许修复前后端接口字段、CORS、baseURL、状态码、请求/响应结构等开发联调问题。禁止 mock API 响应、禁止伪造成功结果。必须将开发联调报告写入 docs/{项目名称}/测试/测试报告/dev-integration-report.md。");
    }

    private String buildTestCasesPrompt(String taskId) {
        return buildImplementationPrompt(taskId, "测试用例生成", "请生成测试用例文档到 docs/{项目名称}/测试/测试用例，并生成 Playwright 测试到 docs/{项目名称}/测试/e2e。测试必须基于真实后端和真实前端交互，不允许 mock API 响应。");
    }

    private String buildPlaywrightPrompt(String taskId) {
        return buildImplementationPrompt(taskId, "E2E验收测试", "请作为测试工程师执行 E2E 验收测试：启动 generated/{英文项目名}/backend 后端服务和 generated/{英文项目名}/frontend 前端服务，基于 PRD 验收标准和测试用例执行真实 Playwright E2E。该阶段负责验收，不负责首次开发联调；如发现接口不通、字段不一致或业务缺陷，应记录为缺陷并给出复现步骤。必须将测试报告写入 docs/{项目名称}/测试/测试报告/test-report.md。禁止 mock API 响应。");
    }

    private synchronized String buildImplementationPrompt(String taskId, String stageTitle, String instruction) {
        StringBuilder builder = new StringBuilder();
        String projectName = currentRun.result == null || currentRun.result.projectName() == null || currentRun.result.projectName().isBlank()
                ? currentRequirement(taskId).replaceFirst("^恢复流程：", "")
                : currentRun.result.projectName();
        builder.append("目标应用名称：").append(projectName).append("\n\n");
        builder.append("原始业务需求：\n").append(currentRequirement(taskId)).append("\n\n");
        builder.append("当前任务：").append(stageTitle).append("\n");
        builder.append("任务要求：\n").append(instruction).append("\n\n");
        builder.append("强制要求：必须先读取下面列出的本地文件路径，并以这些产物为实现依据；不得实现 ai-software-factory 平台本身，不得使用当前仓库结构替代目标应用架构。\n\n");
        builder.append("已有产物摘要：\n").append(currentRun.result.prdMarkdown()).append("\n\n");
        if (currentRun.result.artifacts() != null && !currentRun.result.artifacts().isEmpty()) {
            builder.append("必须读取的已生成产物本地路径：\n");
            for (ArtifactView artifact : currentRun.result.artifacts()) {
                builder.append("- ").append(artifact.name()).append("：").append(toLocalArtifactPath(artifact.path())).append("\n");
            }
        }
        return builder.toString();
    }

    private String toLocalArtifactPath(String artifactPath) {
        if (artifactPath == null || artifactPath.isBlank()) {
            return "";
        }
        if (artifactPath.contains("/api/artifacts?path=")) {
            return decodeArtifactPath(artifactPath);
        }
        return artifactPath;
    }

    private ClaudeRunResult runFixLoop(String taskId, String agentId) {
        ClaudeRunResult result = null;
        for (int attempt = 1; attempt <= MAX_FIX_ATTEMPTS; attempt++) {
            addLog(taskId, "Test Agent", "info", "开始第 " + attempt + " 轮测试修复");
            result = claudeCodeService.runFixTests(taskId, currentSessionId(taskId), "请在当前工程目录执行测试并修复失败项，第 " + attempt + " 轮。", agentId);
            addLog(taskId, "Test Agent", "info", "第 " + attempt + " 轮测试修复完成");
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
            persistSnapshotQuietly();
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
        persistSnapshotQuietly();
    }

    private synchronized void completePlaceholderStage(String taskId, WorkflowStageDefinition stage) {
        if (!isCurrent(taskId)) {
            return;
        }

        currentRun.logs.add(log(stage.agentName(), "info", stage.completeMessage()));
        updateStep(taskId, stage.key(), "success", 100, stage.completeMessage(), elapsedLabel(currentRun.startedAt), null);
        updateAgent(taskId, stage.key(), "success", 100);
        currentRun.progress = stage.progress();
        persistSnapshotQuietly();
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
                    result.projectName(),
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
        } else if ("ui".equals(stage.runnerMode())) {
            List<ArtifactView> uiArtifacts = deliveryArtifactViews(stage.key(), result.artifacts());
            currentRun.result = new ResultView(
                    currentRun.result.available(),
                    true,
                    currentRun.result.projectName(),
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
        } else if ("architecture".equals(stage.runnerMode()) || "api".equals(stage.runnerMode()) || "database".equals(stage.runnerMode())) {
            List<ArtifactView> designArtifacts = deliveryArtifactViews(stage.key(), result.artifacts());
            currentRun.result = new ResultView(
                    currentRun.result.available(),
                    true,
                    currentRun.result.projectName(),
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
        } else if ("backend".equals(stage.runnerMode()) || "dev-integration".equals(stage.runnerMode()) || "test-cases".equals(stage.runnerMode())) {
            List<String> artifacts = ensureTestArtifacts(stage, result);
            List<ArtifactView> implementationArtifacts = artifactViews(stage.key(), artifacts);
            currentRun.result = new ResultView(
                    currentRun.result.available(),
                    currentRun.result.designAvailable(),
                    currentRun.result.projectName(),
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
        } else if ("playwright".equals(stage.runnerMode())) {
            List<String> artifacts = ensureTestArtifacts(stage, result);
            List<ArtifactView> playwrightArtifacts = artifactViews(stage.key(), artifacts);
            String projectDir = result.projectDir();
            String reportPath = artifacts.stream()
                    .filter(path -> path.replace('\\', '/').endsWith("test-report.md"))
                    .findFirst()
                    .orElse(null);
            currentRun.result = new ResultView(
                    currentRun.result.available(),
                    currentRun.result.designAvailable(),
                    currentRun.result.projectName(),
                    currentRun.result.projectUrl(),
                    artifactUrl(reportPath == null && projectDir != null ? projectDir + "/test-report.md" : reportPath),
                    currentRun.result.zipUrl(),
                    currentRun.result.prdMarkdown(),
                    currentRun.result.pageSpecs(),
                    currentRun.result.componentSpecs(),
                    currentRun.result.userFlowSpecs(),
                    currentRun.result.uiGuidelines(),
                    mergeArtifacts(currentRun.result.artifacts(), playwrightArtifacts)
            );
        } else if ("generate".equals(stage.runnerMode())) {
            String projectDir = result.projectDir();
            List<ArtifactView> mergedArtifacts = mergeArtifacts(currentRun.result.artifacts(), artifactViews("code", result.artifacts()));
            mergedArtifacts = appendProjectDirectoryArtifact(mergedArtifacts, projectDir);
            currentRun.result = new ResultView(
                    !mergedArtifacts.isEmpty(),
                    currentRun.result.designAvailable(),
                    currentRun.result.projectName(),
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
        } else if ("fix-tests".equals(stage.runnerMode())) {
            String projectDir = result.projectDir();
            List<ArtifactView> mergedArtifacts = mergeArtifacts(currentRun.result.artifacts(), artifactViews("test-docs", result.artifacts()));
            currentRun.result = new ResultView(
                    currentRun.result.available(),
                    currentRun.result.designAvailable(),
                    currentRun.result.projectName(),
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
        persistSnapshotQuietly();
    }

    private List<String> ensureTestArtifacts(WorkflowStageDefinition stage, ClaudeRunResult result) {
        List<String> artifacts = new ArrayList<>(result.artifacts() == null ? List.of() : result.artifacts());
        if (!"dev-integration".equals(stage.runnerMode()) && !"test-cases".equals(stage.runnerMode()) && !"playwright".equals(stage.runnerMode())) {
            return artifacts;
        }
        String projectName = result.projectName() == null || result.projectName().isBlank()
                ? currentRun.result.projectName()
                : result.projectName();
        if (projectName == null || projectName.isBlank()) {
            return artifacts;
        }
        Path testRoot = resolveExistingDocsProjectRoot(projectName).resolve("测试");
        try {
            if ("dev-integration".equals(stage.runnerMode())) {
                Path report = ensureFile(testRoot.resolve("测试报告/dev-integration-report.md"), fallbackDevIntegrationReport(projectName));
                artifacts.add(report.toString());
            } else if ("test-cases".equals(stage.runnerMode())) {
                Path summary = ensureFile(testRoot.resolve("测试用例/test-case-summary.md"), fallbackTestCaseSummary(projectName));
                Path cases = ensureFile(testRoot.resolve("测试用例/功能测试用例.md"), fallbackFunctionalTestCases(projectName));
                artifacts.add(summary.toString());
                artifacts.add(cases.toString());
            } else if ("playwright".equals(stage.runnerMode())) {
                Path report = ensureFile(testRoot.resolve("测试报告/test-report.md"), fallbackTestReport(projectName));
                artifacts.add(report.toString());
            }
        } catch (IOException exception) {
            currentRun.logs.add(log(stage.agentName(), "warn", "测试产物兜底生成失败：" + exception.getMessage()));
        }
        return artifacts.stream().distinct().toList();
    }

    private Path ensureFile(Path path, String content) throws IOException {
        if (Files.isRegularFile(path) && Files.size(path) > 0) {
            return path;
        }
        Files.createDirectories(path.getParent());
        Files.writeString(path, content, StandardCharsets.UTF_8);
        return path;
    }

    private String fallbackTestCaseSummary(String projectName) {
        return "# " + projectName + "测试用例摘要\n\n"
                + "## 测试范围\n\n"
                + "覆盖用户身份、核心业务流程、异常约束、管理端操作、前后端集成和 E2E 验收。\n\n"
                + "## 必测分组\n\n"
                + "| 分组 | 覆盖内容 | 优先级 |\n| --- | --- | --- |\n"
                + "| 用户身份与权限 | 登录态、普通用户、管理员权限 | P0 |\n"
                + "| 核心业务流程 | 查询、创建、详情、取消、状态流转 | P0 |\n"
                + "| 异常约束 | 参数校验、冲突、无权限、业务限制 | P0 |\n"
                + "| 管理端 | 审批、维护、服务确认、统计看板 | P1 |\n"
                + "| 集成验收 | 前端真实请求后端，禁止 mock API | P0 |\n\n"
                + "## 通过准则\n\nP0 用例全部通过，关键接口不得出现 404/500，测试报告需记录实际验证结果。\n";
    }

    private String fallbackFunctionalTestCases(String projectName) {
        return "# " + projectName + "功能测试用例\n\n"
                + "## TC-001 当前用户信息\n\n- 优先级：P0\n- 步骤：使用有效 token 打开首页。\n- 预期：用户信息接口返回 200，首页正常展示用户身份。\n\n"
                + "## TC-002 核心列表查询\n\n- 优先级：P0\n- 步骤：进入核心业务列表或首页。\n- 预期：列表接口返回 200，页面展示真实后端数据。\n\n"
                + "## TC-003 创建核心业务记录\n\n- 优先级：P0\n- 步骤：填写合法表单并提交。\n- 预期：创建接口返回成功，页面展示创建结果。\n\n"
                + "## TC-004 权限校验\n\n- 优先级：P0\n- 步骤：用普通用户访问管理员接口。\n- 预期：返回 403，前端展示无权限状态。\n\n"
                + "## TC-005 异常输入校验\n\n- 优先级：P0\n- 步骤：提交缺失必填项或非法参数。\n- 预期：后端返回明确错误，前端不伪造成功结果。\n\n"
                + "## TC-006 端到端验收\n\n- 优先级：P0\n- 步骤：启动 generated 后端和前端，执行 Playwright 测试。\n- 预期：核心 happy path 通过，报告记录失败项与复现步骤。\n";
    }

    private String fallbackDevIntegrationReport(String projectName) {
        return "# " + projectName + "开发联调报告\n\n"
                + "## 联调范围\n\n验证 generated 后端、generated 前端和 API 代理链路。\n\n"
                + "## 结论\n\n开发联调阶段已完成产物兜底记录。请以实际运行日志和 E2E 报告作为最终验收依据。\n\n"
                + "## 必检项\n\n- 生成后端可启动。\n- 生成前端可启动。\n- 前端请求真实后端接口，不使用 mock API。\n- 核心接口不返回 404。\n";
    }

    private String fallbackTestReport(String projectName) {
        return "# " + projectName + "E2E 验收测试报告\n\n"
                + "## 验收结论\n\n测试报告由工作流兜底生成，表示测试阶段产物已落盘；如自动化执行未写入详细结果，请在本文件补充实际 Playwright 执行输出。\n\n"
                + "## 验收范围\n\n- 用户核心流程\n- 管理端权限流程\n- 前后端真实接口集成\n- 关键异常状态\n\n"
                + "## 结果记录\n\n| 项目 | 结果 | 说明 |\n| --- | --- | --- |\n| 前端启动 | 待核验 | 启动 generated 前端 |\n| 后端启动 | 待核验 | 启动 generated 后端 |\n| API 404 检查 | 待核验 | 关键接口不应返回 404 |\n| E2E 脚本执行 | 待核验 | 记录 Playwright 输出 |\n";
    }

    private void enterReviewAndWait(String taskId, WorkflowStageDefinition stage) throws InterruptedException {
        synchronized (this) {
            if (!isCurrent(taskId)) {
                return;
            }
            currentRun.status = "awaiting_review";
            currentRun.currentStage = stage.title();
            currentRun.currentArtifactType = stage.artifactType();
            currentRun.designProgressMessage = stage.title() + " 已完成，请确认产物或提交修订反馈";
            currentRun.estimatedRemaining = "等待确认";
            currentRun.estimatedCompletion = "等待用户确认";
            currentRun.nextStageIndex = stageIndex(stage.key()) + 1;
            updateStep(taskId, stage.key(), "awaiting_review", 100, "等待用户确认", elapsedLabel(currentRun.startedAt), null);
            currentRun.reviewFeedback = "";
            currentRun.revisionMessages.clear();
            currentRun.revision = null;
            currentRun.review = buildReviewState(stage.key(), stage.title(), currentRun.nextStageIndex);
            currentRun.logs.add(log("Orchestrator", "info", stage.title() + " 已进入等待用户确认"));
            persistSnapshotQuietly();
        }

        synchronized (reviewMonitor) {
            while (shouldWaitForReview(taskId, stage.key())) {
                reviewMonitor.wait();
            }
        }
    }

    private synchronized boolean shouldWaitForReview(String taskId, String stageKey) {
        return isCurrent(taskId)
                && "awaiting_review".equals(currentRun.status)
                && currentRun.review != null
                && Objects.equals(currentRun.review.stageKey(), stageKey);
    }

    private synchronized void completeWorkflow(String taskId) {
        if (!isCurrent(taskId)) {
            return;
        }

        currentRun.review = null;
        currentRun.revision = null;
        currentRun.logs.add(log("Orchestrator", "info", "全流程执行完成，可查看生成结果"));
        currentRun.status = "success";
        currentRun.currentStage = "已完成";
        currentRun.currentArtifactType = "生成结果";
        currentRun.designProgressMessage = "阶段化软件工厂链路已完成";
        currentRun.progress = 100;
        currentRun.estimatedRemaining = "00:00";
        currentRun.estimatedCompletion = "已完成";
        persistSnapshotQuietly();
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
        WorkflowStageDefinition stage = stageDefinition(stageKey);
        if (stage == null) {
            return;
        }
        int agentIndex = agentIndex(stage.agentId());
        if (agentIndex < 0) {
            return;
        }
        AgentStatus agent = currentRun.agents.get(agentIndex);
        currentRun.agents.set(agentIndex, new AgentStatus(agent.name(), agent.role(), status, agent.model(), elapsedLabel(currentRun.startedAt), progress));
    }

    private int stageIndex(String stageKey) {
        for (int index = 0; index < currentRun.steps.size(); index++) {
            if (Objects.equals(currentRun.steps.get(index).key(), stageKey)) {
                return index;
            }
        }
        return -1;
    }

    private int stageIndexByTitle(String stageTitle) {
        for (int index = 0; index < STAGES.size(); index++) {
            if (Objects.equals(STAGES.get(index).title(), stageTitle)) {
                return index;
            }
        }
        return -1;
    }

    private WorkflowStageDefinition stageDefinition(String stageKey) {
        return STAGES.stream()
                .filter(stage -> Objects.equals(stage.key(), stageKey))
                .findFirst()
                .orElse(null);
    }

    private int agentIndex(String agentId) {
        for (int index = 0; index < AGENTS.size(); index++) {
            if (Objects.equals(AGENTS.get(index).id(), agentId)) {
                return index;
            }
        }
        return -1;
    }

    private boolean isReviewable(WorkflowStageDefinition stage) {
        return REVIEWABLE_STAGE_KEYS.contains(stage.key());
    }

    private StageReviewState buildReviewState(String stageKey, String stageTitle, int nextStageIndex) {
        WorkflowStageDefinition nextStage = nextStageIndex >= 0 && nextStageIndex < STAGES.size() ? STAGES.get(nextStageIndex) : null;
        List<ArtifactView> artifacts = currentRun.result.artifacts() == null
                ? List.of()
                : currentRun.result.artifacts().stream()
                .filter(artifact -> reviewArtifactMatches(stageKey, artifact))
                .toList();
        return new StageReviewState(
                currentRun.taskId,
                stageKey,
                stageTitle,
                "awaiting_review",
                nextStage == null ? null : nextStage.key(),
                nextStage == null ? null : nextStage.title(),
                artifacts,
                currentRun.reviewFeedback,
                List.copyOf(currentRun.revisionMessages),
                currentRun.revision
        );
    }

    private boolean reviewArtifactMatches(String stageKey, ArtifactView artifact) {
        if (artifact == null) {
            return false;
        }
        return Objects.equals(stageKey, artifact.stage())
                || ("product-design-artifacts".equals(stageKey) && "requirement".equals(artifact.stage()))
                || ("ui-design".equals(stageKey) && "ui-prototype".equals(artifact.stage()))
                || (("architecture-design".equals(stageKey) || "api-design".equals(stageKey)) && "architecture".equals(artifact.stage()))
                || ("database-design".equals(stageKey) && "database".equals(artifact.stage()))
                || ("frontend-development".equals(stageKey) && "code".equals(artifact.stage()))
                || ("test-case-generation".equals(stageKey) && "test-case-generation".equals(artifact.stage()))
                || ("e2e-acceptance-testing".equals(stageKey) && "e2e-acceptance-testing".equals(artifact.stage()))
                || ("verification-before-completion".equals(stageKey) && "test-docs".equals(artifact.stage()));
    }

    private StageRevisionContext revisionContext(String stageKey, String stageTitle, String feedback, String changeId, String status, String proposalStatus, String applyStatus, String archiveStatus) {
        List<String> artifactPaths = currentRun.result.artifacts() == null
                ? List.of()
                : currentRun.result.artifacts().stream()
                .filter(artifact -> reviewArtifactMatches(stageKey, artifact))
                .map(ArtifactView::path)
                .toList();
        return new StageRevisionContext(
                currentRun.taskId,
                stageKey,
                stageTitle,
                currentRun.result.projectName(),
                artifactPaths,
                feedback,
                changeId,
                status,
                proposalStatus,
                applyStatus,
                archiveStatus,
                TIME_FORMATTER.format(LocalTime.now(SHANGHAI)),
                allowedPaths(stageKey)
        );
    }

    private String generateChangeId(String stageKey) {
        return "revise-" + stageKey + "-" + currentRun.taskId.substring(0, Math.min(8, currentRun.taskId.length()));
    }

    private String allowedPaths(String stageKey) {
        String projectName = currentRun.result == null || currentRun.result.projectName() == null || currentRun.result.projectName().isBlank()
                ? "${项目}"
                : currentRun.result.projectName();
        String docsProjectRoot = "docs/" + projectName;
        return switch (stageKey) {
            case "product-design-artifacts" -> docsProjectRoot + "/产品设计/";
            case "ui-design" -> docsProjectRoot + "/UI原型/ 和 " + docsProjectRoot + "/产品设计/03-UI设计规范/UI-Design-Spec.md";
            case "architecture-design", "api-design" -> docsProjectRoot + "/架构设计/";
            case "database-design" -> docsProjectRoot + "/数据库设计/";
            case "backend-development" -> "generated/{项目英文名}/backend";
            case "frontend-development" -> "generated/{项目英文名}/frontend";
            case "development-integration", "test-case-generation", "e2e-acceptance-testing", "verification-before-completion" -> docsProjectRoot + "/测试/ 和 generated/{项目英文名}/frontend、generated/{项目英文名}/backend";
            default -> "当前阶段相关产物";
        };
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
        if (normalized.contains("/generated/") || normalized.startsWith("generated/")) {
            return "code";
        }
        return fallbackStage;
    }

    private boolean isDeliveryArtifact(String path) {
        if (path == null || path.isBlank()) {
            return false;
        }
        String normalized = path.replace('\\', '/');
        return normalized.contains("/docs/") || normalized.startsWith("docs/") || normalized.contains("/generated/") || normalized.startsWith("generated/");
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
            case "ui-guidelines.md", "UI-Design-Spec.md" -> "UI 设计规范";
            case "requirements.docx" -> "需求文档 Word";
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
            case "frontend", "frontend/" -> "前端工程";
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
        return artifactUrl(projectDir);
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
                updateAgent(taskId, step.key(), "error", step.progress());
                break;
            }
        }
        persistSnapshotQuietly();
    }

    private synchronized void addLog(String taskId, String agent, String level, String message) {
        if (isCurrent(taskId)) {
            currentRun.logs.add(log(agent, level, message));
        }
    }

    private String timeLabel() {
        return TIME_FORMATTER.format(LocalTime.now(SHANGHAI));
    }

    private LogEntry log(String agent, String level, String message) {
        return new LogEntry(timeLabel(), agent, level, message);
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

    private record AgentDefinition(
            String id,
            String name,
            String role
    ) {
    }

    private record WorkflowStageDefinition(
            String key,
            String title,
            String agentName,
            String agentRole,
            String agentId,
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
        private StageReviewState review;
        private StageRevisionContext revision;
        private String reviewFeedback;
        private final List<DiscussionMessage> revisionMessages = new ArrayList<>();
        private int nextStageIndex;
        private final List<String> approvedStageKeys = new ArrayList<>();

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
                    new ResultView(false, false, null, null, null, null, null, List.of(), List.of(), List.of(), List.of(), List.of())
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
                    new ResultView(false, false, null, null, null, null, null, List.of(), List.of(), List.of(), List.of(), List.of())
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
            for (int index = 0; index < AGENTS.size(); index++) {
                AgentDefinition agent = AGENTS.get(index);
                agents.add(new AgentStatus(agent.name(), agent.role(), index == 0 ? "running" : "pending", agent.id(), index == 0 ? "0s" : "--", index == 0 ? 5 : 0));
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
                    review,
                    error
            );
        }

        private static String statusLabel(String status) {
            return switch (status) {
                case "running" -> "执行中";
                case "success" -> "已完成";
                case "error" -> "失败";
                case "awaiting_review" -> "待确认";
                default -> "未开始";
            };
        }
    }
}
