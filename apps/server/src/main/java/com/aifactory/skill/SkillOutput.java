package com.aifactory.skill;

import com.aifactory.dto.ArtifactSection;
import com.aifactory.dto.ComponentSpec;
import com.aifactory.dto.PageSpec;
import com.aifactory.dto.UserFlowSpec;

import java.util.List;

public record SkillOutput(
        String summary,
        List<ArtifactSection> sections,
        List<PageSpec> pages,
        List<ComponentSpec> components,
        List<UserFlowSpec> userFlows,
        List<String> uiGuidelines,
        String rawMarkdown
) {
}
