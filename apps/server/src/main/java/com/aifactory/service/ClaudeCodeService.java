package com.aifactory.service;

import com.aifactory.config.ClaudeRunnerConfig;
import com.aifactory.dto.ClaudeEnvironmentView;
import com.aifactory.dto.ClaudeMessageResult;
import com.aifactory.dto.ClaudeRunResult;
import com.aifactory.dto.ClaudeSessionStartResult;
import com.aifactory.dto.DiscussionMessage;
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
        factory.setReadTimeout(0);
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
        return runTask(taskId, sessionId, "generate", prompt);
    }

    public ClaudeRunResult runBackend(String taskId, String sessionId, String prompt) {
        return runTask(taskId, sessionId, "backend", prompt);
    }

    public ClaudeRunResult runTestCases(String taskId, String sessionId, String prompt) {
        return runTask(taskId, sessionId, "test-cases", prompt);
    }

    public ClaudeRunResult runPlaywright(String taskId, String sessionId, String prompt) {
        return runTask(taskId, sessionId, "playwright", prompt);
    }

    public ClaudeRunResult runFixTests(String taskId, String sessionId, String prompt) {
        return runTask(taskId, sessionId, "fix-tests", prompt);
    }

    public ClaudeRunResult runTask(String taskId, String sessionId, String mode, String prompt) {
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(Map.of(
                "taskId", taskId,
                "sessionId", sessionId == null ? "" : sessionId,
                "mode", mode,
                "prompt", prompt,
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

    private String buildDiscussionPrompt(String latestMessage, List<DiscussionMessage> history) {
        StringBuilder builder = new StringBuilder();
        builder.append("你是 Requirement Agent，请基于以下讨论历史继续需求澄清。每次只提出一个问题。\n");
        builder.append("请只输出 JSON，不要输出 Markdown 或额外解释。JSON 结构为：{\"question\":\"一个中文问题；如果已足够明确则为空字符串\",\"options\":[\"2到4个可选答案\"],\"complete\":false,\"summary\":\"完成时用不超过5条中文 bullet 总结\"}。\n");
        builder.append("如果需求已经足够明确，complete 必须为 true，question 和 options 可以为空，summary 必须包含已确认需求。\n\n");
        for (DiscussionMessage message : history) {
            builder.append(message.role()).append(": ").append(message.content()).append("\n");
        }
        builder.append("user: ").append(latestMessage);
        return builder.toString();
    }
}
