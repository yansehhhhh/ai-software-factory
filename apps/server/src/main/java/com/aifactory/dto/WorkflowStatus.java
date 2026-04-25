package com.aifactory.dto;

import java.util.List;

public record WorkflowStatus(
        String taskId,
        String requirement,
        String status,
        String statusLabel,
        String currentStage,
        String currentArtifactType,
        String designProgressMessage,
        int progress,
        String estimatedRemaining,
        String estimatedCompletion,
        String testPassRate,
        String systemStatus,
        List<String> examples,
        List<StepStatus> steps,
        List<AgentStatus> agents,
        String error
) {
}
