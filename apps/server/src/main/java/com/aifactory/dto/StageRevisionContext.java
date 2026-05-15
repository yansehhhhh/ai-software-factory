package com.aifactory.dto;

import java.util.List;

public record StageRevisionContext(
        String workflowRunId,
        String stageKey,
        String stageTitle,
        String projectName,
        List<String> artifactPaths,
        String userFeedback,
        String changeId,
        String status,
        String proposalStatus,
        String applyStatus,
        String archiveStatus,
        String updatedAt,
        String allowedPaths
) {
}
