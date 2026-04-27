package com.aifactory.dto;

import java.util.List;
import java.util.Map;

public record ClaudeEnvironmentView(
        boolean runnerOnline,
        boolean cliInstalled,
        boolean loggedIn,
        String workingDirectory,
        String workspaceRoot,
        List<String> availableSkills,
        Map<String, Boolean> permissions
) {
}
