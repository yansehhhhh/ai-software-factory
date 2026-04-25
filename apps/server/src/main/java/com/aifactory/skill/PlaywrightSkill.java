package com.aifactory.skill;

import org.springframework.stereotype.Component;

@Component
public class PlaywrightSkill implements Skill {

    @Override
    public String id() {
        return "playwright-skill";
    }

    @Override
    public String description() {
        return "Adapter for runtime Playwright testing tasks.";
    }

    @Override
    public SkillExecution execute(SkillRequest request) {
        return new SkillExecution(
                id(),
                "success",
                "Playwright adapter prepared test execution metadata.",
                new SkillOutput(
                        "自动化测试运行时模块已准备执行元数据。",
                        java.util.List.of(),
                        java.util.List.of(),
                        java.util.List.of(),
                        java.util.List.of(),
                        java.util.List.of(),
                        "packages/skills/test-generate-skill"
                )
        );
    }
}
