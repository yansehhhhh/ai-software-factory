package com.aifactory.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "claude.runner")
public record ClaudeRunnerConfig(
        String baseUrl,
        String workspaceRoot,
        long connectTimeoutMillis,
        long readTimeoutMillis
) {
}
