# Workflow Orchestration Skill

用于指导 Claude Code 设计和实现编排流程、步骤状态机、日志输出和任务结果流转。

重点：
- 优先复用现有工作流概念与状态定义
- 关注 `apps/server` 中的 orchestrator、workflow context、step/status 模型
- 明确日志、结果、agent 状态之间的流转关系

说明：
- 这是作者工具层 skill，不等同于平台运行时 `packages/skills/`。

参考资料见 `skills/workflow-orchestration-skill/`、`docs/workflow.md` 与 `apps/server/src/main/java/com/aifactory/workflow/`。