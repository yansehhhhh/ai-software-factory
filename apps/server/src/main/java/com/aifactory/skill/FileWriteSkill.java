package com.aifactory.skill;

import org.springframework.stereotype.Component;

@Component
public class FileWriteSkill implements Skill {

    @Override
    public String id() {
        return "file-write-skill";
    }

    @Override
    public String description() {
        return "Adapter for runtime file writing modules in packages/skills.";
    }

    @Override
    public SkillExecution execute(SkillRequest request) {
        return new SkillExecution(
                id(),
                "success",
                "File write adapter prepared the payload for generated files.",
                new SkillOutput(
                        "文件写入运行时模块已接收任务。",
                        java.util.List.of(),
                        java.util.List.of(),
                        java.util.List.of(),
                        java.util.List.of(),
                        java.util.List.of(),
                        "packages/skills/file-write-skill"
                )
        );
    }
}
