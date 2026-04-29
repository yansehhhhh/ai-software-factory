package com.aifactory.service;

import com.aifactory.dto.ClaudeMessageResult;
import com.aifactory.dto.ClaudeSessionStartResult;
import com.aifactory.dto.DiscussionChatResult;
import com.aifactory.dto.DiscussionMessage;
import com.aifactory.dto.DiscussionStartResult;
import com.aifactory.dto.WorkflowStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DiscussionService {

    private final Map<String, DiscussionSession> sessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final WorkflowService workflowService;
    private final ClaudeCodeService claudeCodeService;

    public DiscussionService(WorkflowService workflowService, ClaudeCodeService claudeCodeService) {
        this.workflowService = workflowService;
        this.claudeCodeService = claudeCodeService;
    }

    public DiscussionStartResult start(String requirement) {
        String discussionId = UUID.randomUUID().toString();
        String taskId = "discussion-" + discussionId;

        ClaudeSessionStartResult sessionStart = claudeCodeService.startDiscussionSession(taskId, requirement);
        ClaudeMessageResult firstTurn = claudeCodeService.sendDiscussionMessage(
                taskId,
                sessionStart.sessionId(),
                requirement,
                List.of(DiscussionMessage.user("我的需求是：" + requirement))
        );

        ParsedDiscussionResponse parsed = parseDiscussionResponse(firstTurn.content(), firstTurn.isComplete());
        List<DiscussionMessage> history = new ArrayList<>();
        history.add(DiscussionMessage.user("我的需求是：" + requirement));
        history.add(DiscussionMessage.ai(parsed.displayContent(), parsed.options()));
        DiscussionSession session = new DiscussionSession(
                discussionId,
                taskId,
                sessionStart.sessionId(),
                sessionStart.workspaceDir(),
                requirement,
                history,
                parsed.complete()
        );
        sessions.put(discussionId, session);

        return new DiscussionStartResult(discussionId, parsed.displayContent(), history);
    }

    public DiscussionChatResult chat(String discussionId, String message) {
        DiscussionSession session = sessions.get(discussionId);
        if (session == null) {
            throw new IllegalArgumentException("Discussion session not found: " + discussionId);
        }

        List<DiscussionMessage> nextHistory = new ArrayList<>(session.history);
        nextHistory.add(DiscussionMessage.user(message));

        ClaudeMessageResult response = claudeCodeService.sendDiscussionMessage(
                session.taskId,
                session.sessionId,
                message,
                nextHistory
        );

        ParsedDiscussionResponse parsed = parseDiscussionResponse(response.content(), response.isComplete());
        nextHistory.add(DiscussionMessage.ai(parsed.displayContent(), parsed.options()));
        session.history.clear();
        session.history.addAll(nextHistory);
        session.completed = parsed.complete();

        return new DiscussionChatResult(
                session.completed ? "" : parsed.displayContent(),
                session.completed,
                List.copyOf(session.history)
        );
    }

    public WorkflowStatus confirm(String discussionId) {
        DiscussionSession session = sessions.get(discussionId);
        if (session == null) {
            throw new IllegalArgumentException("Discussion session not found: " + discussionId);
        }

        String enrichedRequirement = generateEnrichedRequirement(session);
        WorkflowStatus status = workflowService.start(enrichedRequirement);
        sessions.remove(discussionId);
        return status;
    }

    public List<DiscussionMessage> getHistory(String discussionId) {
        DiscussionSession session = sessions.get(discussionId);
        if (session == null) {
            throw new IllegalArgumentException("Discussion session not found: " + discussionId);
        }
        return List.copyOf(session.history);
    }

    private ParsedDiscussionResponse parseDiscussionResponse(String content, boolean cliComplete) {
        boolean markerComplete = content != null && (content.contains("[DISCUSSION_COMPLETE]") || content.contains("[讨论完成]"));
        if (content == null || content.isBlank()) {
            return new ParsedDiscussionResponse("", List.of(), cliComplete || markerComplete);
        }

        String cleanedContent = stripMarkdownCodeBlock(content);

        try {
            JsonNode root = objectMapper.readTree(cleanedContent);
            boolean complete = cliComplete || markerComplete || root.path("complete").asBoolean(false);
            String question = root.path("question").asText("").strip();
            String summary = root.path("summary").asText("").strip();
            List<String> options = new ArrayList<>();
            JsonNode optionNodes = root.path("options");
            if (optionNodes.isArray()) {
                optionNodes.forEach(option -> {
                    String value = option.asText("").strip();
                    if (!value.isEmpty()) {
                        options.add(value);
                    }
                });
            }
            String displayContent = complete && !summary.isEmpty() ? summary : question;
            if (displayContent.isEmpty()) {
                displayContent = content;
            }
            return new ParsedDiscussionResponse(displayContent, complete ? List.of() : options, complete);
        } catch (Exception ignored) {
            return new ParsedDiscussionResponse(content, List.of(), cliComplete || markerComplete);
        }
    }

    private String stripMarkdownCodeBlock(String content) {
        if (content == null) {
            return null;
        }
        String stripped = content.strip();
        if (stripped.startsWith("```json") || stripped.startsWith("```")) {
            int startIndex = stripped.indexOf('\n');
            if (startIndex >= 0) {
                int endIndex = stripped.lastIndexOf("```");
                if (endIndex > startIndex) {
                    return stripped.substring(startIndex + 1, endIndex).strip();
                }
            }
        }
        return stripped;
    }

    private String generateEnrichedRequirement(DiscussionSession session) {
        StringBuilder enriched = new StringBuilder();
        enriched.append(session.requirement).append("\n\n");
        enriched.append("需求讨论补充：\n");

        for (int i = 0; i < session.history.size(); i++) {
            DiscussionMessage msg = session.history.get(i);
            if (msg.role().equals("user") && !msg.content().startsWith("我的需求是：")) {
                if (i > 0 && session.history.get(i - 1).role().equals("ai")) {
                    String question = session.history.get(i - 1).content();
                    enriched.append("- ").append(question.replace("？", "").replace("?", ""))
                            .append(": ").append(msg.content()).append("\n");
                }
            }
        }

        return enriched.toString();
    }

    private record ParsedDiscussionResponse(String displayContent, List<String> options, boolean complete) {
    }

    private static final class DiscussionSession {
        final String discussionId;
        final String taskId;
        final String sessionId;
        final String workspaceDir;
        final String requirement;
        final List<DiscussionMessage> history;
        boolean completed;

        DiscussionSession(String discussionId, String taskId, String sessionId, String workspaceDir, String requirement, List<DiscussionMessage> history, boolean completed) {
            this.discussionId = discussionId;
            this.taskId = taskId;
            this.sessionId = sessionId;
            this.workspaceDir = workspaceDir;
            this.requirement = requirement;
            this.history = history;
            this.completed = completed;
        }
    }
}
