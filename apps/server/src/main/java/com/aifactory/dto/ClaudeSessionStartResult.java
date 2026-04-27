package com.aifactory.dto;

import java.util.List;

public record ClaudeSessionStartResult(
        String taskId,
        String sessionId,
        String workspaceDir,
        String projectDir,
        List<DiscussionMessage> history
) {
}
