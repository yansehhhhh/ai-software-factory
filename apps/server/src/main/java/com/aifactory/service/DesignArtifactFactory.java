package com.aifactory.service;

import com.aifactory.dto.ArtifactSection;
import com.aifactory.dto.ComponentSpec;
import com.aifactory.dto.PageSpec;
import com.aifactory.dto.UserFlowSpec;
import com.aifactory.skill.SkillOutput;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DesignArtifactFactory {

    public SkillOutput createPrdOutput(String requirement, String rawMarkdown) {
        String productName = productName(requirement);
        return new SkillOutput(
                productName + " 的第一版 PRD 已生成，聚焦需求输入、设计编排与结果展示。",
                List.of(
                        new ArtifactSection("目标用户", List.of("平台使用者", "产品经理", "研发协作者")),
                        new ArtifactSection("核心能力", List.of("需求输入", "设计结果生成", "流程与日志可观测")),
                        new ArtifactSection("交付边界", List.of("第一版只落地产品设计阶段", "输出 PRD 与 UI 规范"))
                ),
                List.of(),
                List.of(),
                List.of(
                        new UserFlowSpec("提交需求并生成设计", List.of("输入业务需求", "触发产品设计", "查看 PRD 结果")),
                        new UserFlowSpec("查看 UI 设计建议", List.of("进入结果区", "查看页面列表", "查看组件建议"))
                ),
                List.of(),
                rawMarkdown
        );
    }

    public SkillOutput createUiOutput(String requirement, String rawMarkdown) {
        String productName = productName(requirement);
        return new SkillOutput(
                productName + " 的 UI 规范已生成，包含页面结构、关键组件和设计原则。",
                List.of(
                        new ArtifactSection("视觉原则", List.of("信息优先", "分区清晰", "状态可观测")),
                        new ArtifactSection("交互原则", List.of("一步触发", "日志可追踪", "结果可复用"))
                ),
                List.of(
                        new PageSpec("Dashboard", "承载需求输入、流程进度、日志与设计结果总览", List.of("需求输入", "KPI 卡片", "流程状态", "设计结果摘要")),
                        new PageSpec("Design Result Detail", "承载 PRD、页面建议、组件建议与用户流程", List.of("PRD 摘要", "页面列表", "组件清单", "用户流程"))
                ),
                List.of(
                        new ComponentSpec("RequirementInput", "输入自然语言需求并触发设计阶段", List.of("支持多行输入", "支持快速示例")),
                        new ComponentSpec("WorkflowProgress", "展示 Product/Design 阶段执行状态", List.of("步骤状态", "当前产物类型", "进度消息")),
                        new ComponentSpec("DesignResultPanel", "展示 PRD、页面和组件规范", List.of("按模块分块展示", "支持空态和失败态"))
                ),
                List.of(
                        new UserFlowSpec("阅读产品设计结果", List.of("查看 PRD 摘要", "浏览页面列表", "核对组件建议")),
                        new UserFlowSpec("异常恢复", List.of("定位错误日志", "点击重试", "重新生成设计结果"))
                ),
                List.of(
                        "页面保持浅色工作台风格",
                        "设计结果优先结构化展示，不直接渲染草图",
                        "错误与重试入口必须显式"
                ),
                rawMarkdown
        );
    }

    private String productName(String requirement) {
        if (requirement == null || requirement.isBlank()) {
            return "目标应用";
        }
        if (requirement.contains("AI质检助手")) {
            return "AI质检助手";
        }
        if (requirement.contains("数据分析")) {
            return "数据分析系统";
        }
        return "目标应用";
    }
}
