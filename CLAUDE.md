# AI Software Factory

这是一个面向 Claude Code 的 AI 软件工厂仓库，核心目标是把需求分析、需求讨论、实现计划、代码实现、完成前验证和结果展示串成一条可执行工作流。

## 仓库结构
- `apps/web`：平台前端，Vue 3 页面、组件、路由和状态管理。
- `apps/server`：平台后端，Spring Boot 控制器、服务、工作流编排、模型路由和运行时 skill 适配。
- `packages/`：平台运行时可复用模块，包括 agents、workflows、skills、templates。
- `generated/`：AI 生成的前后端工程产物输出目录，按 `generated/{项目英文名}/frontend`、`generated/{项目英文名}/backend` 归档。
- `docs/${需求或项目名称}/`：主链路交付产物目录，包含产品设计、UI原型、架构设计、数据库设计、测试和其他文档。
- `tests/`：平台级测试与端到端验证。
- `.claude/skills/`：供 Claude Code 使用的作者工具层 skills。

## 在本仓库中的默认工作方式
- 优先复用现有目录和分层，不引入平行架构。
- 前端改动优先遵循 `apps/web/src/components`、`apps/web/src/views`、`apps/web/src/stores` 的现有组织方式。
- 后端改动优先遵循 `controller -> service -> workflow/skill` 的职责分层。
- 运行时能力优先放在 `packages/` 和 `apps/server`，不要把平台运行时逻辑塞进 `.claude/`。
- 测试优先复用现有 Playwright 与 `tests/` 目录，必要时再补充最小测试。
- 涉及生成 demo 或样例时，优先参考 `apps/inspector-demo`、`generated/`、`scripts/` 现有内容。
- 文档书写时优先使用中文，除非特殊要求，例如插件格式要求标题英文之类的可保留英文。

## 重要区分
- `.claude/skills/` 是 Claude Code 使用的作者工具层说明，不是平台运行时模块。
- `packages/skills/` 和 `apps/server/src/main/java/com/aifactory/skill/` 才是平台运行时 skill 的实现和适配层。

## 期望输出
- 变更尽量小而明确。
- 说明、命令、agent 定义应直接引用当前真实目录。
- 除非明确要求，不要顺带重构业务代码。

## 交付产物目录结构

主链路产物统一归档到 `docs/${需求或项目名称}/`。其中 `产品设计/` 是当前需求阶段产物的物理目录，前端结果面板按“需求阶段产物”展示该目录下的文件。

```text
docs/${需求或项目名称}/
├── 产品设计/
│   ├── 01-变更记录/版本说明.md
│   ├── 02-产品需求文档/PRD.md
│   ├── 03-UI设计规范/UI-Design-Spec.md
│   ├── 04-流程图/业务流程图.puml
│   ├── 04-流程图/信息架构图.puml
│   ├── 04-流程图/页面流转图.puml
│   └── 05-附录/术语表.md
├── UI原型/
│   ├── 设计稿/
│   ├── 组件库/组件清单.md
│   ├── 交互原型/交互说明.md
│   └── 移动端适配/响应式断点参考.md
├── 架构设计/
│   ├── 01-系统架构/系统架构设计.md
│   ├── 01-系统架构/系统架构图.puml
│   ├── 01-系统架构/系统架构图.svg
│   ├── 02-接口设计/接口定义/
│   ├── 03-部署架构/
│   └── 04-技术选型/
├── 数据库设计/
│   ├── 数据字典/
│   ├── 建表脚本/
│   └── 数据迁移脚本/
├── 测试/
│   ├── 测试用例/
│   └── 测试报告/
└── 其他文档/
    └── 会议纪要/
```

## 需求分析工作流（Superpowers Skills）

当用户输入需求时，按以下顺序调用 `.claude/skills/` 下的 superpowers 系列 skill：

### 1. 需求讨论与设计 — brainstorming
用户提出功能需求后，**必须先调用**：
```
/brainstorming
```
- 探索项目上下文，理解现有架构和约束
- 逐一提出澄清问题（每次只问一个，优先选择题）
- 提出 2-3 种方案并说明权衡
- 编写设计文档保存到 `docs/${项目名称}/产品设计/02-产品需求文档/PRD.md`
- 获得用户批准后才能进入下一步

**强制门控**：在设计批准前禁止编写任何代码或调用实现类 skill。

### 2. 制定实现计划 — writing-plans
设计批准后，调用：
```
/writing-plans
```
- 创建详细的实现计划，保存到 `docs/${项目名称}/产品设计/03-UI设计规范/UI-Design-Spec.md`
- 每个步骤必须包含完整代码，不允许占位符
- 遵循 TDD 原则：先写测试，再实现

### 3. 验证完成 — verification-before-completion
实现完成后，调用：
```
/verification-before-completion
```
- 运行验证命令，阅读完整输出
- 确认结果后再声称完成
- 无新鲜验证证据，禁止声称完成

### Skill 调用顺序
```
用户输入需求 → brainstorming → writing-plans → 实现代码 → verification-before-completion
```

**注意**：brainstorming 完成后只能调用 writing-plans，不能直接跳到实现类 skill。