package com.aifactory.dto;

import java.util.List;

public record ClaudeMessageResult(
        String taskId,
        String sessionId,
        String status,
        String content,
        List<DiscussionMessage> history,
        boolean isComplete
) {
}
