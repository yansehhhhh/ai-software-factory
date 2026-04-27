package com.aifactory.skill;

import com.aifactory.dto.DiscussionMessage;
import com.aifactory.llm.LlmClient;
import com.aifactory.llm.LlmRequest;
import com.aifactory.llm.LlmResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Superpowers Brainstorming Skill 适配器
 * 用于需求讨论与设计阶段，通过 LLM 动态生成问题进行需求澄清。
 */
@Component
public class BrainstormingSkillAdapter implements Skill {

    private static final Logger log = LoggerFactory.getLogger(BrainstormingSkillAdapter.class);

    private final LlmClient llmClient;

    public BrainstormingSkillAdapter(LlmClient llmClient) {
        this.llmClient = llmClient;
        log.info("BrainstormingSkillAdapter initialized with LlmClient: {}", llmClient.getClass().getSimpleName());
    }

    @Override
    public String id() {
        return "brainstorming-skill";
    }

    @Override
    public String description() {
        return "需求讨论与设计 skill - 通过 LLM 动态提问进行需求澄清";
    }

    @Override
    public SkillExecution execute(SkillRequest request) {
        String requirement = request.prompt();
        List<DiscussionMessage> history = extractHistory(request);

        log.info("Brainstorming skill executing, history size: {}", history.size());

        LlmRequest llmRequest = new LlmRequest(
                "brainstorming",
                request.modelHint(),
                requirement,
                requirement,
                Map.of("discussionHistory", history)
        );

        LlmResponse response = llmClient.complete(llmRequest);
        String content = response.content();

        log.info("LLM response content length: {}", content.length());
        log.debug("LLM response content: {}", content);

        if (content == null || content.isEmpty()) {
            log.warn("LLM returned empty content, using fallback");
            content = "请问这个系统的核心功能是什么？";
        }

        boolean isComplete = content.contains("[讨论完成]");

        return new SkillExecution(
                request.skillId(),
                "success",
                isComplete ? "需求讨论已完成" : "问题已生成",
                new SkillOutput(
                        content,
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        content
                )
        );
    }

    private List<DiscussionMessage> extractHistory(SkillRequest request) {
        Object historyObj = request.context().get("discussionHistory");
        if (historyObj instanceof List) {
            return (List<DiscussionMessage>) historyObj;
        }
        return List.of();
    }
}