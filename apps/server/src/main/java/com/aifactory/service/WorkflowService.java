package com.aifactory.service;

import com.aifactory.dto.AgentStatus;
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
            addLog(taskId, "Requirement Agent", "info", "需求讨论已确认，开始生成结构化产物");
            updateStage(taskId, "需求讨论", "需求摘要", "正在整理需求上下文", 10, "running", 25);
            sleepQuietly(200L);

            updateAfterDiscussion(taskId);
            sleepQuietly(150L);

            addLog(taskId, "Product Agent", "info", "开始调用 Claude Runner 生成产品设计结果");
            updateStage(taskId, "PRD生成", "PRD", "Claude Code 正在生成产品需求文档", 35, "running", 30);
            var prdResult = claudeCodeService.runTask(taskId, currentSessionId(taskId), "prd", currentRequirement(taskId));
            updateAfterPrd(taskId, prdResult.content());
            sleepQuietly(150L);

            addLog(taskId, "Design Agent", "info", "开始调用 Claude Runner 生成 UI 设计规范");
            updateStage(taskId, "UI设计", "UI 规范", "Claude Code 正在生成页面和组件规范", 60, "running", 45);
            var uiResult = claudeCodeService.runTask(taskId, currentSessionId(taskId), "ui", currentRequirement(taskId));
            updateAfterUi(taskId, uiResult.content());
            sleepQuietly(150L);

            addLog(taskId, "Developer Agent", "info", "开始调用 Claude Runner 执行代码生成");
            updateStage(taskId, "代码生成", "工程代码", "Claude Code 正在生成项目代码", 80, "running", 65);
            var generateResult = claudeCodeService.runGenerate(taskId, currentSessionId(taskId), currentRequirement(taskId));
            updateAfterGenerate(taskId, generateResult.projectDir());
            sleepQuietly(150L);

            addLog(taskId, "QA Agent", "info", "开始调用 Claude Runner 执行测试修复");
            updateStage(taskId, "自动化测试", "测试修复", "Claude Code 正在执行测试并修复问题", 92, "running", 85);
            runFixLoop(taskId);

            completeWorkflow(taskId);
        } catch (Exception exception) {
            markError(taskId, exception.getMessage() == null ? "Claude Runner 执行失败" : exception.getMessage());
        }
    }

    private void runFixLoop(String taskId) {
        for (int attempt = 1; attempt <= MAX_FIX_ATTEMPTS; attempt++) {
            addLog(taskId, "QA Agent", "info", "开始第 " + attempt + " 轮测试修复");
            claudeCodeService.runFixTests(taskId, currentSessionId(taskId), "请在当前工程目录执行测试并修复失败项，第 " + attempt + " 轮。");
            addLog(taskId, "QA Agent", "info", "第 " + attempt + " 轮测试修复完成");
        }
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

    private synchronized void updateAfterDiscussion(String taskId) {
        if (!isCurrent(taskId)) {
            return;
        }

        currentRun.logs.add(log("Requirement Agent", "info", "需求摘要整理完成"));
        currentRun.steps.set(0, new StepStatus(1, "brainstorming", "需求讨论", "success", 100, "需求上下文已确认", elapsedLabel(currentRun.startedAt), null));
        currentRun.agents.set(0, new AgentStatus("Requirement Agent", "需求讨论专家", "success", "Claude Code", elapsedLabel(currentRun.startedAt), 100));
        currentRun.progress = 20;
        currentRun.currentStage = "PRD生成";
        currentRun.currentArtifactType = "PRD";
        currentRun.designProgressMessage = "Product Agent 准备输出产品需求文档";
        currentRun.estimatedRemaining = "00:03";
        currentRun.estimatedCompletion = "00:03";
        currentRun.steps.set(1, new StepStatus(2, "prd", "PRD生成", "running", 20, "正在生成 PRD", "0s", null));
        currentRun.agents.set(1, new AgentStatus("Product Agent", "需求分析师", "running", "Claude Code", "0s", 20));
    }

    private synchronized void updateAfterPrd(String taskId, String prdContent) {
        if (!isCurrent(taskId)) {
            return;
        }

        currentRun.logs.add(log("Product Agent", "info", "PRD 生成完成"));
        currentRun.steps.set(1, new StepStatus(2, "prd", "PRD生成", "success", 100, "PRD 输出完成", elapsedLabel(currentRun.startedAt), null));
        currentRun.agents.set(1, new AgentStatus("Product Agent", "需求分析师", "success", "Claude Code", elapsedLabel(currentRun.startedAt), 100));
        currentRun.progress = 45;
        currentRun.currentStage = "UI设计";
        currentRun.currentArtifactType = "UI 规范";
        currentRun.designProgressMessage = "Design Agent 准备生成 UI 规范";
        currentRun.estimatedRemaining = "00:02";
        currentRun.estimatedCompletion = "00:02";
        currentRun.result = new ResultView(
                false,
                false,
                null,
                null,
                null,
                prdContent,
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );
        currentRun.steps.set(2, new StepStatus(3, "ui", "UI设计", "running", 25, "正在生成 UI 规范", "0s", null));
        currentRun.agents.set(2, new AgentStatus("Design Agent", "UI 设计师", "running", "Claude Code", "0s", 25));
    }

    private synchronized void updateAfterUi(String taskId, String uiContent) {
        if (!isCurrent(taskId)) {
            return;
        }

        currentRun.logs.add(log("Design Agent", "info", "UI 规范生成完成"));
        currentRun.steps.set(2, new StepStatus(3, "ui", "UI设计", "success", 100, "页面与组件建议生成完成", elapsedLabel(currentRun.startedAt), null));
        currentRun.agents.set(2, new AgentStatus("Design Agent", "UI 设计师", "success", "Claude Code", elapsedLabel(currentRun.startedAt), 100));
        currentRun.progress = 70;
        currentRun.currentStage = "代码生成";
        currentRun.currentArtifactType = "工程代码";
        currentRun.designProgressMessage = "Developer Agent 准备生成代码";
        currentRun.estimatedRemaining = "00:01";
        currentRun.estimatedCompletion = "00:01";
        currentRun.result = new ResultView(
                false,
                true,
                null,
                null,
                null,
                currentRun.result.prdMarkdown() + "\n\n" + uiContent,
                List.of(),
                List.of(),
                List.of(),
                List.of("Claude Code 生成的 UI 规范已写入隔离目录")
        );
        currentRun.steps.set(3, new StepStatus(4, "codegen", "代码生成", "running", 30, "正在生成代码", "0s", null));
        currentRun.agents.set(3, new AgentStatus("Developer Agent", "开发工程师", "running", "Claude Code", "0s", 30));
    }

    private synchronized void updateAfterGenerate(String taskId, String projectDir) {
        if (!isCurrent(taskId)) {
            return;
        }

        currentRun.logs.add(log("Developer Agent", "info", "代码生成完成"));
        currentRun.steps.set(3, new StepStatus(4, "codegen", "代码生成", "success", 100, "项目代码生成完成", elapsedLabel(currentRun.startedAt), null));
        currentRun.agents.set(3, new AgentStatus("Developer Agent", "开发工程师", "success", "Claude Code", elapsedLabel(currentRun.startedAt), 100));
        currentRun.progress = 88;
        currentRun.currentStage = "自动化测试";
        currentRun.currentArtifactType = "测试修复";
        currentRun.designProgressMessage = "QA Agent 准备执行测试与修复";
        currentRun.estimatedRemaining = "00:01";
        currentRun.estimatedCompletion = "00:01";
        currentRun.result = new ResultView(
                true,
                true,
                projectDir,
                projectDir == null ? null : projectDir + "/test-report.html",
                projectDir == null ? null : projectDir + "/project.zip",
                currentRun.result.prdMarkdown(),
                List.of(),
                List.of(),
                List.of(),
                currentRun.result.uiGuidelines()
        );
        currentRun.steps.set(4, new StepStatus(5, "verification", "自动化测试", "running", 40, "正在执行测试修复", "0s", null));
        currentRun.agents.set(4, new AgentStatus("QA Agent", "测试工程师", "running", "Claude Code", "0s", 40));
    }

    private synchronized void completeWorkflow(String taskId) {
        if (!isCurrent(taskId)) {
            return;
        }

        currentRun.logs.add(log("QA Agent", "info", "自动化测试修复完成"));
        currentRun.logs.add(log("Orchestrator", "info", "全流程执行完成，可查看生成结果"));
        currentRun.status = "success";
        currentRun.currentStage = "已完成";
        currentRun.currentArtifactType = "生成结果";
        currentRun.designProgressMessage = "需求讨论、设计、代码生成与测试修复已完成";
        currentRun.progress = 100;
        currentRun.estimatedRemaining = "00:00";
        currentRun.estimatedCompletion = "已完成";
        currentRun.steps.set(4, new StepStatus(5, "verification", "自动化测试", "success", 100, "测试修复完成", elapsedLabel(currentRun.startedAt), null));
        currentRun.agents.set(4, new AgentStatus("QA Agent", "测试工程师", "success", "Claude Code", elapsedLabel(currentRun.startedAt), 100));
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

    private synchronized void updateStage(
            String taskId,
            String stage,
            String artifactType,
            String designMessage,
            int progress,
            String taskStatus,
            int stageProgress
    ) {
        if (!isCurrent(taskId)) {
            return;
        }

        currentRun.status = taskStatus;
        currentRun.currentStage = stage;
        currentRun.currentArtifactType = artifactType;
        currentRun.designProgressMessage = designMessage;
        currentRun.progress = progress;
        currentRun.estimatedRemaining = "00:04";
        currentRun.estimatedCompletion = "00:04";
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
                    new ArrayList<>(),
                    new ArrayList<>(),
                    "",
                    "pending",
                    "未开始",
                    "--",
                    "等待任务启动",
                    0,
                    "--",
                    "--",
                    null,
                    new ResultView(false, false, null, null, null, null, List.of(), List.of(), List.of(), List.of())
            );
        }

        private static WorkflowRun starting(String requirement, List<String> examples) {
            return new WorkflowRun(
                    UUID.randomUUID().toString(),
                    requirement,
                    Instant.now(),
                    examples,
                    new ArrayList<>(),
                    new ArrayList<>(List.of(
                            new StepStatus(1, "brainstorming", "需求讨论", "running", 5, "正在整理需求上下文", "0s", null),
                            new StepStatus(2, "prd", "PRD生成", "pending", 0, "等待需求确认完成", "--", null),
                            new StepStatus(3, "ui", "UI设计", "pending", 0, "等待 PRD 生成", "--", null),
                            new StepStatus(4, "codegen", "代码生成", "pending", 0, "等待 UI 规范输出", "--", null),
                            new StepStatus(5, "verification", "自动化测试", "pending", 0, "等待代码生成完成", "--", null)
                    )),
                    new ArrayList<>(List.of(
                            new AgentStatus("Requirement Agent", "需求讨论专家", "running", "Claude Code", "0s", 5),
                            new AgentStatus("Product Agent", "需求分析师", "pending", "Claude Code", "--", 0),
                            new AgentStatus("Design Agent", "UI 设计师", "pending", "Claude Code", "--", 0),
                            new AgentStatus("Developer Agent", "开发工程师", "pending", "Claude Code", "--", 0),
                            new AgentStatus("QA Agent", "测试工程师", "pending", "Claude Code", "--", 0)
                    )),
                    "",
                    "running",
                    "需求讨论",
                    "需求摘要",
                    "正在整理需求上下文",
                    5,
                    "00:04",
                    "00:04",
                    null,
                    new ResultView(false, false, null, null, null, null, List.of(), List.of(), List.of(), List.of())
            );
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
