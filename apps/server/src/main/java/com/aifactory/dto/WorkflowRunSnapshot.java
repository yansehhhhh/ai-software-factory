package com.aifactory.dto;

import java.util.List;

public record WorkflowRunSnapshot(
        int schemaVersion,
        String taskId,
        String requirement,
        String sessionId,
        String status,
        String currentStage,
        String currentArtifactType,
        String designProgressMessage,
        int progress,
        String estimatedRemaining,
        String estimatedCompletion,
        String error,
        String updatedAt,
        ResultView result,
        List<LogEntry> logs,
        List<StepStatus> steps,
        List<AgentStatus> agents,
        StageReviewState review,
        StageRevisionContext revision,
        String reviewFeedback,
        List<DiscussionMessage> revisionMessages,
        int nextStageIndex,
        List<String> approvedStageKeys
) {
}
