package com.aifactory.controller;

import com.aifactory.dto.ClaudeEnvironmentView;
import com.aifactory.service.ClaudeCodeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/claude")
public class ClaudeEnvironmentController {

    private final ClaudeCodeService claudeCodeService;

    public ClaudeEnvironmentController(ClaudeCodeService claudeCodeService) {
        this.claudeCodeService = claudeCodeService;
    }

    @GetMapping("/environment")
    public ClaudeEnvironmentView environment() {
        return claudeCodeService.checkEnvironment();
    }
}
