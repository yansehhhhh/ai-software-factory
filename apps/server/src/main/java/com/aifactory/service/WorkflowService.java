package com.aifactory.service;

import com.aifactory.dto.AgentStatus;
import com.aifactory.dto.ComponentSpec;
import com.aifactory.dto.LogEntry;
import com.aifactory.dto.PageSpec;
import com.aifactory.dto.ResultView;
import com.aifactory.dto.StepStatus;
import com.aifactory.dto.UserFlowSpec;
import com.aifactory.dto.WorkflowStatus;
import com.aifactory.skill.Skill;
import com.aifactory.skill.SkillExecution;
import com.aifactory.skill.SkillRegistry;
import com.aifactory.skill.SkillRequest;
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
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class WorkflowService {

    private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss", Locale.CHINA);
    private static final List<String> EXAMPLES = List.of("AI质检助手", "数据分析系统", "简单博客系统", "会议纪要助手", "数据库管理系统", "智能客服系统");

    private final SkillRegistry skillRegistry;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "workflow-design-executor");
        thread.setDaemon(true);
        return thread;
    });

    private WorkflowRun currentRun = WorkflowRun.idle();

    public WorkflowService(SkillRegistry skillRegistry) {
        this.skillRegistry = skillRegistry;
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
            addLog(taskId, "Product Agent", "info", "开始调用 prd-skill 生成 PRD");
            updateStage(taskId, "需求分析", "PRD", "Product Agent 正在输出产品需求文档", 10, "running", 16);
            sleepQuietly(250L);

            SkillExecution prdExecution = executeSkill(
                    "prd-skill",
                    taskId,
                    "prd",
                    List.of("prdMarkdown", "userFlows"),
                    Map.of("requirement", currentRequirement(taskId)),
                    "gpt-4.1"
            );

            updateAfterPrd(taskId, prdExecution);
            sleepQuietly(250L);

            addLog(taskId, "Design Agent", "info", "开始调用 ui-generate-skill 生成 UI 规范");
            updateStage(taskId, "UI设计", "UI 规范", "Design Agent 正在生成页面清单与组件建议", 58, "running", 22);
            sleepQuietly(300L);

            SkillExecution uiExecution = executeSkill(
                    "ui-generate-skill",
                    taskId,
                    "ui",
                    List.of("pages", "components", "uiGuidelines"),
                    Map.of("requirement", currentRequirement(taskId)),
                    "qwen-max"
            );

            updateAfterUi(taskId, uiExecution);
        } catch (Exception exception) {
            markError(taskId, exception.getMessage() == null ? "设计阶段执行失败" : exception.getMessage());
        }
    }

    private SkillExecution executeSkill(
            String skillId,
            String taskId,
            String taskType,
            List<String> artifacts,
            Map<String, Object> context,
            String modelHint
    ) {
        Skill skill = skillRegistry.find(skillId)
                .orElseThrow(() -> new IllegalStateException("Skill not registered: " + skillId));

        if (String.valueOf(context.getOrDefault("requirement", "")).contains("失败")) {
            throw new IllegalStateException("设计阶段失败：需求描述触发了失败演练");
        }

        return skill.execute(new SkillRequest(skillId, taskId, taskType, String.valueOf(context.get("requirement")), artifacts, context, modelHint));
    }

    private synchronized String currentRequirement(String taskId) {
        return isCurrent(taskId) ? currentRun.requirement : "";
    }

    private synchronized void updateAfterPrd(String taskId, SkillExecution execution) {
        if (!isCurrent(taskId)) {
            return;
        }

        currentRun.logs.add(log("Product Agent", "info", "PRD 生成完成"));
        currentRun.steps.set(0, new StepStatus(1, "prd", "需求分析", "success", 100, "PRD 输出完成", elapsedLabel(currentRun.startedAt), null));
        currentRun.agents.set(0, new AgentStatus("Product Agent", "产品需求分析", "success", "gpt-4.1", elapsedLabel(currentRun.startedAt), 100));
        currentRun.progress = 52;
        currentRun.currentStage = "UI设计";
        currentRun.currentArtifactType = "UI 规范";
        currentRun.designProgressMessage = "Design Agent 准备生成页面结构与组件建议";
        currentRun.estimatedRemaining = "00:01";
        currentRun.estimatedCompletion = "00:01";
        currentRun.result = new ResultView(
                false,
                false,
                null,
                null,
                null,
                execution.outputs().rawMarkdown(),
                List.of(),
                List.of(),
                execution.outputs().userFlows(),
                List.of()
        );
        currentRun.steps.set(1, new StepStatus(2, "ui", "UI设计", "running", 15, "正在整理页面与组件建议", "0s", null));
        currentRun.agents.set(1, new AgentStatus("Design Agent", "UI 设计", "running", "qwen-max", "0s", 15));
    }

    private synchronized void updateAfterUi(String taskId, SkillExecution execution) {
        if (!isCurrent(taskId)) {
            return;
        }

        currentRun.logs.add(log("Design Agent", "info", "UI 规范生成完成"));
        currentRun.logs.add(log("Orchestrator", "info", "产品设计阶段完成，可查看结构化设计结果"));
        currentRun.status = "success";
        currentRun.currentStage = "已完成";
        currentRun.currentArtifactType = "设计结果";
        currentRun.designProgressMessage = "PRD 与 UI 规范已生成，可作为后续研发输入";
        currentRun.progress = 100;
        currentRun.estimatedRemaining = "00:00";
        currentRun.estimatedCompletion = "已完成";
        currentRun.steps.set(1, new StepStatus(2, "ui", "UI设计", "success", 100, "页面与组件建议生成完成", elapsedLabel(currentRun.startedAt), null));
        currentRun.agents.set(1, new AgentStatus("Design Agent", "UI 设计", "success", "qwen-max", elapsedLabel(currentRun.startedAt), 100));
        currentRun.result = new ResultView(
                false,
                true,
                null,
                null,
                null,
                currentRun.result.prdMarkdown(),
                execution.outputs().pages(),
                execution.outputs().components(),
                currentRun.result.userFlowSpecs(),
                execution.outputs().uiGuidelines()
        );
    }

    private synchronized void markError(String taskId, String message) {
        if (!isCurrent(taskId)) {
            return;
        }

        currentRun.status = "error";
        currentRun.error = message;
        currentRun.estimatedRemaining = "--";
        currentRun.designProgressMessage = "设计阶段中断，请查看错误日志并重试";
        currentRun.logs.add(log("Orchestrator", "error", message));

        if ("running".equals(currentRun.steps.get(0).status())) {
            currentRun.steps.set(0, new StepStatus(1, "prd", "需求分析", "error", currentRun.steps.get(0).progress(), "PRD 生成失败", elapsedLabel(currentRun.startedAt), message));
            currentRun.agents.set(0, new AgentStatus("Product Agent", "产品需求分析", "error", "gpt-4.1", elapsedLabel(currentRun.startedAt), currentRun.agents.get(0).progress()));
        } else {
            currentRun.steps.set(1, new StepStatus(2, "ui", "UI设计", "error", currentRun.steps.get(1).progress(), "UI 规范生成失败", elapsedLabel(currentRun.startedAt), message));
            currentRun.agents.set(1, new AgentStatus("Design Agent", "UI 设计", "error", "qwen-max", elapsedLabel(currentRun.startedAt), currentRun.agents.get(1).progress()));
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
        currentRun.estimatedRemaining = "00:02";
        currentRun.estimatedCompletion = "00:02";
        int stepIndex = "需求分析".equals(stage) ? 0 : 1;
        String stepKey = stepIndex == 0 ? "prd" : "ui";
        currentRun.steps.set(stepIndex, new StepStatus(stepIndex + 1, stepKey, stage, "running", stageProgress, designMessage, elapsedLabel(currentRun.startedAt), null));
        if (stepIndex == 0) {
            currentRun.agents.set(0, new AgentStatus("Product Agent", "产品需求分析", "running", "gpt-4.1", elapsedLabel(currentRun.startedAt), stageProgress));
        } else {
            currentRun.agents.set(1, new AgentStatus("Design Agent", "UI 设计", "running", "qwen-max", elapsedLabel(currentRun.startedAt), stageProgress));
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

    private static final class WorkflowRun {

        private final String taskId;
        private final String requirement;
        private final Instant startedAt;
        private final List<String> examples;
        private final List<LogEntry> logs;
        private final List<StepStatus> steps;
        private final List<AgentStatus> agents;
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
                            new StepStatus(1, "prd", "需求分析", "running", 5, "Product Agent 正在生成 PRD", "0s", null),
                            new StepStatus(2, "ui", "UI设计", "pending", 0, "等待 PRD 产出", "--", null)
                    )),
                    new ArrayList<>(List.of(
                            new AgentStatus("Product Agent", "产品需求分析", "running", "gpt-4.1", "0s", 5),
                            new AgentStatus("Design Agent", "UI 设计", "pending", "qwen-max", "--", 0)
                    )),
                    "running",
                    "需求分析",
                    "PRD",
                    "Product Agent 正在读取需求并生成 PRD",
                    5,
                    "00:02",
                    "00:02",
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
