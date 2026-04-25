# AI Software Factory

这是一个面向 Claude Code 的 AI 软件工厂仓库，核心目标是把需求分析、界面设计、代码生成、自动化测试和结果展示串成一条可执行工作流。

## 仓库结构
- `apps/web`：平台前端，Vue 3 页面、组件、路由和状态管理。
- `apps/server`：平台后端，Spring Boot 控制器、服务、工作流编排、模型路由和运行时 skill 适配。
- `packages/`：平台运行时可复用模块，包括 agents、workflows、skills、templates。
- `generated/`：AI 生成的演示应用与产物输出目录。
- `tests/`：平台级测试与端到端验证。
- `.claude/skills/`：供 Claude Code 使用的作者工具层 skills。

## 在本仓库中的默认工作方式
- 优先复用现有目录和分层，不引入平行架构。
- 前端改动优先遵循 `apps/web/src/components`、`apps/web/src/views`、`apps/web/src/stores` 的现有组织方式。
- 后端改动优先遵循 `controller -> service -> workflow/skill` 的职责分层。
- 运行时能力优先放在 `packages/` 和 `apps/server`，不要把平台运行时逻辑塞进 `.claude/`。
- 测试优先复用现有 Playwright 与 `tests/` 目录，必要时再补充最小测试。
- 涉及生成 demo 或样例时，优先参考 `apps/inspector-demo`、`generated/`、`scripts/` 现有内容。

## 重要区分
- `.claude/skills/` 是 Claude Code 使用的作者工具层说明，不是平台运行时模块。
- `packages/skills/` 和 `apps/server/src/main/java/com/aifactory/skill/` 才是平台运行时 skill 的实现和适配层。

## 期望输出
- 变更尽量小而明确。
- 说明、命令、agent 定义应直接引用当前真实目录。
- 除非明确要求，不要顺带重构业务代码。