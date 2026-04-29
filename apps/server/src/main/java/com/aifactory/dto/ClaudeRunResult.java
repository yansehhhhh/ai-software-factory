package com.aifactory.dto;

import java.util.List;

public record ClaudeRunResult(
        String taskId,
        String sessionId,
        String status,
        String content,
        List<String> artifacts,
        String workspaceDir,
        String projectDir,
        String docsProjectDir,
        String projectName
) {
}
