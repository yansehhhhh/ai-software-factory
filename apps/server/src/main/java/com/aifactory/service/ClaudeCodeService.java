package com.aifactory.service;

import com.aifactory.config.ClaudeRunnerConfig;
import com.aifactory.dto.ClaudeEnvironmentView;
import com.aifactory.dto.ClaudeMessageResult;
import com.aifactory.dto.ClaudeRunResult;
import com.aifactory.dto.ClaudeSessionStartResult;
import com.aifactory.dto.DiscussionMessage;
import com.aifactory.dto.OpenSpecRunResult;
import com.aifactory.dto.StageRevisionContext;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
public class ClaudeCodeService {

    private final RestTemplate restTemplate;
    private final RestTemplate runTemplate;
    private final ClaudeRunnerConfig config;

    public ClaudeCodeService(RestTemplateBuilder builder, ClaudeRunnerConfig config) {
        this.config = config;
        this.restTemplate = builder
                .setConnectTimeout(Duration.ofMillis(config.connectTimeoutMillis()))
                .setReadTimeout(Duration.ofMillis(config.readTimeoutMillis()))
                .build();
        this.runTemplate = createRunTemplate(config);
    }

    private static RestTemplate createRunTemplate(ClaudeRunnerConfig config) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) config.connectTimeoutMillis());
        factory.setReadTimeout((int) config.readTimeoutMillis());
        return new RestTemplate(factory);
    }

    public ClaudeEnvironmentView checkEnvironment() {
        return restTemplate.getForObject(config.baseUrl() + "/claude/env", ClaudeEnvironmentView.class);
    }

    public ClaudeSessionStartResult startDiscussionSession(String taskId, String requirement) {
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(Map.of(
                "taskId", taskId,
                "requirement", requirement,
                "workspaceRoot", config.workspaceRoot()
        ));
        ResponseEntity<ClaudeSessionStartResult> response = restTemplate.exchange(
                config.baseUrl() + "/claude/session/start",
                HttpMethod.POST,
                request,
                ClaudeSessionStartResult.class
        );
        return response.getBody();
    }

    public ClaudeMessageResult sendDiscussionMessage(String taskId, String sessionId, String prompt, List<DiscussionMessage> history) {
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(Map.of(
                "taskId", taskId,
                "sessionId", sessionId,
                "prompt", buildDiscussionPrompt(prompt, history)
        ));
        ResponseEntity<ClaudeMessageResult> response = restTemplate.exchange(
                config.baseUrl() + "/claude/session/message",
                HttpMethod.POST,
                request,
                ClaudeMessageResult.class
        );
        return response.getBody();
    }

    public ClaudeRunResult runGenerate(String taskId, String sessionId, String prompt) {
        return runGenerate(taskId, sessionId, prompt, "");
    }

    public ClaudeRunResult runGenerate(String taskId, String sessionId, String prompt, String agentId) {
        return runTask(taskId, sessionId, "generate", prompt, agentId);
    }

    public ClaudeRunResult runBackend(String taskId, String sessionId, String prompt) {
        return runBackend(taskId, sessionId, prompt, "");
    }

    public ClaudeRunResult runBackend(String taskId, String sessionId, String prompt, String agentId) {
        return runTask(taskId, sessionId, "backend", prompt, agentId);
    }

    public ClaudeRunResult runTestCases(String taskId, String sessionId, String prompt) {
        return runTestCases(taskId, sessionId, prompt, "");
    }

    public ClaudeRunResult runTestCases(String taskId, String sessionId, String prompt, String agentId) {
        return runTask(taskId, sessionId, "test-cases", prompt, agentId);
    }

    public ClaudeRunResult runPlaywright(String taskId, String sessionId, String prompt) {
        return runPlaywright(taskId, sessionId, prompt, "");
    }

    public ClaudeRunResult runPlaywright(String taskId, String sessionId, String prompt, String agentId) {
        return runTask(taskId, sessionId, "playwright", prompt, agentId);
    }

    public ClaudeRunResult runFixTests(String taskId, String sessionId, String prompt) {
        return runFixTests(taskId, sessionId, prompt, "");
    }

    public ClaudeRunResult runFixTests(String taskId, String sessionId, String prompt, String agentId) {
        return runTask(taskId, sessionId, "fix-tests", prompt, agentId);
    }

    public ClaudeRunResult runTask(String taskId, String sessionId, String mode, String prompt) {
        return runTask(taskId, sessionId, mode, prompt, "");
    }

    public ClaudeRunResult runTask(String taskId, String sessionId, String mode, String prompt, String agentId) {
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(Map.of(
                "taskId", taskId,
                "sessionId", sessionId == null ? "" : sessionId,
                "mode", mode,
                "prompt", prompt,
                "agentId", agentId == null ? "" : agentId,
                "workspaceRoot", config.workspaceRoot()
        ));
        ResponseEntity<ClaudeRunResult> response = runTemplate.exchange(
                config.baseUrl() + "/claude/run",
                HttpMethod.POST,
                request,
                ClaudeRunResult.class
        );
        return response.getBody();
    }

    public OpenSpecRunResult runOpenSpecAction(String action, StageRevisionContext context) {
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(Map.of(
                "taskId", context.workflowRunId(),
                "action", action,
                "context", context,
                "workspaceRoot", config.workspaceRoot()
        ));
        ResponseEntity<OpenSpecRunResult> response = runTemplate.exchange(
                config.baseUrl() + "/claude/openspec",
                HttpMethod.POST,
                request,
                OpenSpecRunResult.class
        );
        return response.getBody();
    }

    private String buildDiscussionPrompt(String latestMessage, List<DiscussionMessage> history) {
        StringBuilder builder = new StringBuilder();
        builder.append("你是 Requirement Agent，请基于以下讨论历史继续需求澄清。\n");
        builder.append("请使用中文 Markdown 输出，支持标题、列表、表格和代码块。每次只提出一个核心问题；如果需要给选项，请用 Markdown 列表呈现 2 到 4 个选项。\n");
        builder.append("如果需求已经足够明确，请在回复末尾单独一行输出 [DISCUSSION_COMPLETE]，并用不超过 5 条中文 bullet 总结已确认需求。\n");
        builder.append("不要输出 JSON，除非用户明确要求。\n\n");
        for (DiscussionMessage message : history) {
            builder.append(message.role()).append(": ").append(message.content()).append("\n");
        }
        return builder.toString();
    }
}
