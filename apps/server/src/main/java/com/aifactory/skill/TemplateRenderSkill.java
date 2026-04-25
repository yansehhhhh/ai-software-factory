package com.aifactory.skill;

import org.springframework.stereotype.Component;

@Component
public class TemplateRenderSkill implements Skill {

    @Override
    public String id() {
        return "template-render-skill";
    }

    @Override
    public String description() {
        return "Adapter for prompt, PRD, and template rendering runtime modules.";
    }

    @Override
    public SkillExecution execute(SkillRequest request) {
        return new SkillExecution(
                id(),
                "success",
                "Template renderer prepared structured output for the next agent.",
                new SkillOutput(
                        "模板渲染运行时模块已输出结构化上下文。",
                        java.util.List.of(),
                        java.util.List.of(),
                        java.util.List.of(),
                        java.util.List.of(),
                        java.util.List.of(),
                        "packages/skills/prd-skill,packages/skills/ui-generate-skill"
                )
        );
    }
}
