package com.aifactory.skill;

import java.util.List;
import java.util.Map;

public record SkillRequest(
        String skillId,
        String taskId,
        String taskType,
        String prompt,
        List<String> artifacts,
        Map<String, Object> context,
        String modelHint
) {
}
