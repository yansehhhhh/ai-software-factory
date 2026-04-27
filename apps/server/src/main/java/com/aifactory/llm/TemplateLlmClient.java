package com.aifactory.llm;

import com.aifactory.dto.DiscussionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 模板 LLM 客户端（备用方案）
 * 当 GlmLlmClient 未配置 API Key 时使用此客户端
 */
@Component
@ConditionalOnMissingBean(GenericLlmClient.class)
public class TemplateLlmClient implements LlmClient {

    @Override
    public LlmResponse complete(LlmRequest request) {
        String productName = extractProductName(request.requirement());
        String content = switch (request.taskType()) {
            case "brainstorming" -> generateBrainstormingQuestion(request);
            case "prd" -> """
                    # %s 产品需求文档

                    ## 目标
                    围绕"%s"构建第一版产品设计结果，明确目标用户、核心流程和交付边界。

                    ## 用户价值
                    - 让用户在单一入口提交需求
                    - 输出结构化产品设计结果
                    - 为后续研发和测试阶段提供稳定输入

                    ## 范围
                    - 需求录入
                    - 设计结果查看
                    - 日志与流程可观测
                    """.formatted(productName, request.requirement());
            case "ui" -> """
                    # %s UI 规范

                    ## 页面
                    - Dashboard：任务输入、流程和设计结果总览
                    - Design Detail：PRD、用户流程、页面和组件建议详情
                    - Logs Panel：日志滚动与异常提示

                    ## 交互
                    - 提交需求后立即显示当前阶段
                    - 设计完成后展示结构化卡片与列表
                    - 异常态支持重试
                    """.formatted(productName);
            default -> "# Unsupported task";
        };
        return new LlmResponse("template", content);
    }

    private String generateBrainstormingQuestion(LlmRequest request) {
        Map<String, Object> context = request.context();
        Object historyObj = context.getOrDefault("discussionHistory", List.of());
        List<DiscussionMessage> history = historyObj instanceof List ? (List<DiscussionMessage>) historyObj : List.of();

        int userReplyCount = 0;
        for (DiscussionMessage msg : history) {
            if (msg.role().equals("user") && !msg.content().startsWith("我的需求是：")) {
                userReplyCount++;
            }
        }

        List<String> questions = List.of(
                "请问这个系统是否需要用户登录和身份认证？",
                "是否需要权限管理（如管理员、普通用户等角色区分）？",
                "数据存储方式是什么？（本地数据库、云端存储、或无需持久化）",
                "是否需要导入/导出功能？",
                "目标用户群体是什么？"
        );

        if (userReplyCount >= questions.size()) {
            return "[讨论完成]\n\n需求讨论已完成，我已理解您的需求要点。点击\"结束讨论并生成\"即可开始设计流程。";
        }

        return questions.get(userReplyCount);
    }

    private String extractProductName(String requirement) {
        List<String> examples = List.of("AI质检助手", "数据分析系统", "简单博客系统", "会议纪要助手");
        return examples.stream().filter(requirement::contains).findFirst().orElse("目标应用");
    }
}