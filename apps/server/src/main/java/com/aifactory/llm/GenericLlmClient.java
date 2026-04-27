package com.aifactory.llm;

import com.aifactory.dto.DiscussionMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 通用 LLM 客户端
 * 支持多种 API 格式：Anthropic、OpenAI兼容、DashScope 等
 *
 * 配置项（application.properties 或环境变量）：
 * - llm.api.key: API Key
 * - llm.api.endpoint: API 地址
 * - llm.model: 模型名称
 * - llm.api.type: API 类型（anthropic/openai/dashscope）
 */
@Component
@ConditionalOnExpression("'${llm.api.key:}' != ''")
public class GenericLlmClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(GenericLlmClient.class);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String endpoint;
    private final String model;
    private final String apiType;

    public GenericLlmClient(
            @Value("${llm.api.key}") String apiKey,
            @Value("${llm.api.endpoint}") String endpoint,
            @Value("${llm.model:claude-3-5-sonnet-20241022}") String model,
            @Value("${llm.api.type:anthropic}") String apiType
    ) {
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(java.time.Duration.ofSeconds(120))
                .build();
        this.objectMapper = new ObjectMapper();
        this.apiKey = apiKey;
        this.endpoint = endpoint;
        this.model = model;
        this.apiType = apiType;

        log.info("GenericLlmClient initialized: type={}, model={}, endpoint={}", apiType, model, endpoint);
    }

    @Override
    public LlmResponse complete(LlmRequest request) {
        log.info("LLM request: taskType={}", request.taskType());

        try {
            String jsonBody = buildRequestBody(request);
            log.debug("Request body: {}", jsonBody);

            HttpRequest httpRequest = buildHttpRequest(jsonBody);
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            log.info("Response status: {}", response.statusCode());
            log.info("Response body: {}", response.body());

            if (response.statusCode() != 200) {
                log.error("API error: {}", response.body());
                return new LlmResponse(model, "API错误: " + response.body());
            }

            String content = parseResponse(response.body());
            log.info("Generated content length: {}", content.length());

            if (content.isEmpty()) {
                log.warn("Empty content from API");
                return new LlmResponse(model, "LLM返回空内容，请检查模型配置");
            }

            return new LlmResponse(model, content);

        } catch (Exception e) {
            log.error("LLM call failed: {}", e.getMessage(), e);
            return new LlmResponse(model, "LLM调用失败: " + e.getMessage());
        }
    }

    private String buildRequestBody(LlmRequest request) throws Exception {
        List<Map<String, String>> messages = buildMessages(request);

        if ("anthropic".equals(apiType) || "dashscope".equals(apiType)) {
            // Anthropic/DashScope 格式
            Map<String, Object> body = Map.of(
                    "model", model,
                    "max_tokens", 1024,
                    "messages", messages
            );
            return objectMapper.writeValueAsString(body);
        } else {
            // OpenAI 兼容格式
            Map<String, Object> body = Map.of(
                    "model", model,
                    "messages", messages
            );
            return objectMapper.writeValueAsString(body);
        }
    }

    private HttpRequest buildHttpRequest(String jsonBody) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody));

        if ("anthropic".equals(apiType) || "dashscope".equals(apiType)) {
            // Anthropic/DashScope 使用 x-api-key
            builder.header("x-api-key", apiKey);
            builder.header("anthropic-version", "2023-06-01");
        } else {
            // OpenAI 兼容格式使用 Authorization Bearer
            builder.header("Authorization", "Bearer " + apiKey);
        }

        return builder.build();
    }

    private String parseResponse(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);
        log.info("Parsing response, root keys: {}", root.fieldNames().toString());

        // DashScope/GLM 特殊格式: content 数组，包含 thinking 和 text 类型
        JsonNode contentArray = root.path("content");
        if (contentArray.isArray()) {
            log.info("Content is array with {} elements", contentArray.size());
            for (JsonNode item : contentArray) {
                String type = item.path("type").asText();
                log.info("Content item type: {}", type);
                if ("text".equals(type)) {
                    String text = item.path("text").asText();
                    log.info("Found text content: {}", text);
                    return text;
                }
            }
        }

        // Anthropic 格式: content[0].text
        String anthropicContent = root.path("content")
                .path(0)
                .path("text")
                .asText();
        if (!anthropicContent.isEmpty()) return anthropicContent;

        // OpenAI 格式: choices[0].message.content
        String openaiContent = root.path("choices")
                .path(0)
                .path("message")
                .path("content")
                .asText();
        if (!openaiContent.isEmpty()) return openaiContent;

        return "";
    }

    private List<Map<String, String>> buildMessages(LlmRequest request) {
        List<Map<String, String>> messages = new ArrayList<>();

        String systemPrompt = buildSystemPrompt(request.taskType());
        messages.add(Map.of("role", "user", "content", systemPrompt + "\n\n用户需求：" + request.requirement()));

        if ("brainstorming".equals(request.taskType())) {
            Map<String, Object> context = request.context();
            Object historyObj = context.get("discussionHistory");
            if (historyObj instanceof List) {
                List<DiscussionMessage> history = (List<DiscussionMessage>) historyObj;
                for (DiscussionMessage msg : history) {
                    if (!msg.content().startsWith("我的需求是：")) {
                        messages.add(Map.of(
                                "role", msg.role().equals("ai") ? "assistant" : "user",
                                "content", msg.content()
                        ));
                    }
                }
            }
        }

        return messages;
    }

    private String buildSystemPrompt(String taskType) {
        return switch (taskType) {
            case "brainstorming" ->
                    "你是需求分析师，正在与用户讨论需求细节。" +
                    "你的任务是根据用户的需求描述，逐一提问澄清关键细节。" +
                    "每次只问一个简洁的问题，等待用户回复后再继续。" +
                    "重点询问：用户登录、权限管理、数据存储、导入导出、目标用户、部署环境等。" +
                    "当收集到足够信息后（至少5个问题的回复），在回复开头加上'[讨论完成]'标记。";
            case "prd" ->
                    "你是产品经理，需要根据用户需求生成产品需求文档(PRD)。" +
                    "输出结构化的PRD，包含：目标、用户价值、功能范围、用户流程。";
            case "ui" ->
                    "你是UI设计师，需要根据PRD生成UI规范。" +
                    "输出结构化的UI规范，包含：页面清单、组件建议、交互细节。";
            default -> "你是AI助手，帮助用户完成需求分析和设计。";
        };
    }
}