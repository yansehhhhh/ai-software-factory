package com.aifactory.dto;

import java.util.List;

public record StageReviewState(
        String workflowRunId,
        String stageKey,
        String stageTitle,
        String status,
        String nextStageKey,
        String nextStageTitle,
        List<ArtifactView> artifacts,
        String userFeedback,
        List<DiscussionMessage> revisionMessages,
        StageRevisionContext revision
) {
}
