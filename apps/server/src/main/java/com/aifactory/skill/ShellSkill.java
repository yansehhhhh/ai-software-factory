package com.aifactory.skill;

import org.springframework.stereotype.Component;

@Component
public class ShellSkill implements Skill {

    @Override
    public String id() {
        return "shell-skill";
    }

    @Override
    public String description() {
        return "Adapter for shell-oriented runtime tasks.";
    }

    @Override
    public SkillExecution execute(SkillRequest request) {
        return new SkillExecution(
                id(),
                "success",
                "Shell adapter accepted the execution request.",
                new SkillOutput(
                        "命令执行运行时模块已接收任务。",
                        java.util.List.of(),
                        java.util.List.of(),
                        java.util.List.of(),
                        java.util.List.of(),
                        java.util.List.of(),
                        "packages/skills/shell-skill"
                )
        );
    }
}
