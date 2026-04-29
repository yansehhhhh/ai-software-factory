package com.aifactory;

import com.aifactory.controller.ClaudeEnvironmentController;
import com.aifactory.dto.ClaudeEnvironmentView;
import com.aifactory.service.ClaudeCodeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ClaudeEnvironmentController.class)
class ClaudeEnvironmentControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ClaudeCodeService claudeCodeService;

    @Test
    void shouldReturnRunnerEnvironment() throws Exception {
        when(claudeCodeService.checkEnvironment()).thenReturn(new ClaudeEnvironmentView(
                true,
                true,
                false,
                "/tmp/workspace",
                "workspace/runtime",
                List.of("brainstorming", "writing-plans", "verification-before-completion"),
                Map.of("bash", true, "edit", true)
        ));

        mockMvc.perform(get("/api/claude/environment"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.runnerOnline").value(true))
                .andExpect(jsonPath("$.cliInstalled").value(true))
                .andExpect(jsonPath("$.loggedIn").value(false))
                .andExpect(jsonPath("$.workingDirectory").value("/tmp/workspace"))
                .andExpect(jsonPath("$.workspaceRoot").value("workspace/runtime"))
                .andExpect(jsonPath("$.availableSkills[0]").value("brainstorming"))
                .andExpect(jsonPath("$.permissions.bash").value(true));
    }
}
