package com.aifactory.dto;

public record OpenSpecRunResult(
        String taskId,
        String action,
        String changeId,
        String status,
        String content,
        String updatedAt
) {
}
