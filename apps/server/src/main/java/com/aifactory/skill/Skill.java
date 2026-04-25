package com.aifactory.skill;

public interface Skill {

    String id();

    String description();

    SkillExecution execute(SkillRequest request);
}
