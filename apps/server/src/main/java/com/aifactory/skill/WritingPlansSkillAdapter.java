package com.aifactory.skill;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Superpowers Writing-Plans Skill 适配器
 * 用于制定实现计划，在设计批准后创建详细的实现步骤。

 * 对应 .claude/skills/writing-plans/SKILL.md
 */
@Component
public class WritingPlansSkillAdapter implements Skill {

    @Override
    public String id() {
        return "writing-plans-skill";
    }

    @Override
    public String description() {
        return "制定实现计划 skill - 创建详细的实现步骤，遵循 TDD 原则";
    }

    @Override
    public SkillExecution execute(SkillRequest request) {
        String requirement = request.prompt();

        // 模拟 writing-plans skill 的输出
        String implementationPlan = generateImplementationPlan(requirement);

        return new SkillExecution(
                request.skillId(),
                "success",
                "实现计划已生成",
                new SkillOutput(
                        implementationPlan,
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        implementationPlan
                )
        );
    }

    private String generateImplementationPlan(String requirement) {
        return """
                # 实现计划

                ## Goal
                %s

                ## Architecture
                - Controller: 处理 HTTP 请求
                - Service: 业务逻辑层
                - Skill/Workflow: 能力执行层

                ## Tech Stack
                - Spring Boot 3.3.5
                - Vue 3 + Vite
                - Pinia 状态管理

                ## Task 1: 创建数据模型

                ### Files
                - apps/server/src/main/java/com/aifactory/dto/XxxDto.java — 创建
                - apps/server/src/test/java/com/aifactory/XxxDtoTest.java — 创建

                ### Steps
                - [ ] Write failing test for XxxDto validation
                - [ ] Run test, confirm failure
                - [ ] Implement XxxDto with validation annotations
                - [ ] Run test, confirm pass
                - [ ] Commit "feat: add XxxDto"

                ## Task 2: 编写单元测试

                ### Files
                - apps/server/src/test/java/com/aifactory/XxxServiceTest.java — 创建

                ### Steps
                - [ ] Write failing test for service behavior
                - [ ] Run test, confirm failure
                - [ ] Implement service method
                - [ ] Run test, confirm pass
                - [ ] Commit "test: add XxxService tests"

                ## Task 3: 实现核心逻辑

                ### Files
                - apps/server/src/main/java/com/aifactory/service/XxxService.java — 创建
                - apps/server/src/main/java/com/aifactory/controller/XxxController.java — 创建

                ### Steps
                - [ ] Implement service layer logic
                - [ ] Add controller endpoint
                - [ ] Run integration test
                - [ ] Commit "feat: implement Xxx feature"

                ## Task 4: 集成验证

                ### Steps
                - [ ] Run full test suite
                - [ ] Verify API endpoint works
                - [ ] Check frontend integration
                - [ ] Commit "chore: verify integration"

                ## 执行选项
                1. Subagent-Driven (推荐): 每个任务启动新子代理
                2. Inline Execution: 分批执行，带检查点
                """.formatted(requirement);
    }
}