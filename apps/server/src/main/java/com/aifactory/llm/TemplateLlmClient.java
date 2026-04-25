package com.aifactory.llm;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TemplateLlmClient implements LlmClient {

    @Override
    public LlmResponse complete(LlmRequest request) {
        String productName = extractProductName(request.requirement());
        String content = switch (request.taskType()) {
            case "prd" -> """
                    # %s 产品需求文档

                    ## 目标
                    围绕“%s”构建第一版产品设计结果，明确目标用户、核心流程和交付边界。

                    ## 用户价值
                    - 让用户在单一入口提交需求
                    - 输出结构化产品设计结果
                    - 为后续研发和测试阶段提供稳定输入

                    ## 范围
                    - 需求录入
                    - 设计结果查看
                    - 日志与流程可观测
                    """.formatted(productName, request.requirement())
                    ;
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

        return new LlmResponse(request.model(), content);
    }

    private String extractProductName(String requirement) {
        List<String> examples = List.of("AI质检助手", "数据分析系统", "简单博客系统", "会议纪要助手");
        return examples.stream().filter(requirement::contains).findFirst().orElse("目标应用");
    }
}
