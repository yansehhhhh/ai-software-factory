package com.aifactory.skill;

public record SkillExecution(
        String skillId,
        String status,
        String message,
        SkillOutput outputs
) {
}
