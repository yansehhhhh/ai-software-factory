package com.aifactory.service;

import com.aifactory.dto.ClaudeMessageResult;
import com.aifactory.dto.ClaudeSessionStartResult;
import com.aifactory.dto.DiscussionChatResult;
import com.aifactory.dto.DiscussionMessage;
import com.aifactory.dto.DiscussionStartResult;
import com.aifactory.dto.WorkflowStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DiscussionService {

    private final Map<String, DiscussionSession> sessions = new ConcurrentHashMap<>();
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
                sessionStart.history()
        );

        List<DiscussionMessage> history = new ArrayList<>(firstTurn.history());
        DiscussionSession session = new DiscussionSession(
                discussionId,
                taskId,
                sessionStart.sessionId(),
                sessionStart.workspaceDir(),
                requirement,
                history,
                firstTurn.isComplete()
        );
        sessions.put(discussionId, session);

        return new DiscussionStartResult(discussionId, firstTurn.content(), history);
    }

    public DiscussionChatResult chat(String discussionId, String message) {
        DiscussionSession session = sessions.get(discussionId);
        if (session == null) {
            throw new IllegalArgumentException("Discussion session not found: " + discussionId);
        }

        ClaudeMessageResult response = claudeCodeService.sendDiscussionMessage(
                session.taskId,
                session.sessionId,
                message,
                session.history
        );

        session.history.clear();
        session.history.addAll(response.history());
        session.completed = response.isComplete() || response.content().contains("[DISCUSSION_COMPLETE]") || response.content().contains("[讨论完成]");

        return new DiscussionChatResult(
                session.completed ? "" : response.content(),
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
