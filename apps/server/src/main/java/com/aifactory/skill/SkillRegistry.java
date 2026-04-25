package com.aifactory.skill;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class SkillRegistry {

    private final Map<String, Skill> skills;

    public SkillRegistry(List<Skill> skillList) {
        this.skills = skillList.stream().collect(Collectors.toMap(Skill::id, Function.identity()));
    }

    public Optional<Skill> find(String skillId) {
        return Optional.ofNullable(skills.get(skillId));
    }

    public List<String> ids() {
        return skills.keySet().stream().sorted().toList();
    }
}
