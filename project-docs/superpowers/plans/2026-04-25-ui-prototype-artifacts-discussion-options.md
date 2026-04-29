# UI 原型、阶段产物展示与讨论选项实施计划

## Goal

让 AI 软件工厂在 UI 阶段通过 Claude CLI 使用 `/ui-ux-pro-max` 生成静态 HTML 原型基础产物；让后端暴露 PRD/UI/code/test 阶段 artifacts；让 Web 页面展示需求阶段、UI 阶段、代码测试阶段产物；让需求讨论支持 AI 问题选项，同时保留自由输入。

## Architecture

主链路保持 `apps/web -> apps/server -> apps/claude-runner -> claude CLI`。Claude CLI skills 只放在 `.claude/skills`。`claude-runner` 的 `ui` mode 负责调用 `/ui-ux-pro-max` 并写出 `ui-guidelines.md`、`design-system/MASTER.md`、`prototype/index.html`、`ui-prototype.md`。后端 `WorkflowService` 从 `ClaudeRunResult.artifacts` 构造 `ArtifactView` 列表并放入 `ResultView`。前端 `DesignResult.vue` 按 stage 分组展示 artifacts。讨论消息 DTO 增加 `options`，后端从 Claude JSON 响应中解析选项，前端展示选项按钮。

## Tech Stack

- Claude Code skills: `.claude/skills/<name>/SKILL.md`
- Claude Runner: Node.js ESM
- Backend: Spring Boot Java records/services
- Frontend: Vue 3 single-file components
- Verification: Yarn, Maven, Node import smoke checks

## Task 1: 引入 ui-ux-pro-max Claude CLI skill

### Files
- `.claude/skills/ui-ux-pro-max/SKILL.md` — 创建
- `.claude/skills/ui-ux-pro-max/scripts/search.py` — 创建
- `.claude/skills/ui-ux-pro-max/scripts/core.py` — 创建
- `.claude/skills/ui-ux-pro-max/scripts/design_system.py` — 创建
- `.claude/skills/ui-ux-pro-max/data/**` — 创建

### Steps
- [ ] 从 `nextlevelbuilder/ui-ux-pro-max-skill` 复制 `.claude/skills/ui-ux-pro-max/SKILL.md`。
- [ ] 复制 `src/ui-ux-pro-max/scripts` 到 `.claude/skills/ui-ux-pro-max/scripts`。
- [ ] 复制 `src/ui-ux-pro-max/data` 到 `.claude/skills/ui-ux-pro-max/data`。
- [ ] 调整 skill 文档中脚本路径为 `.claude/skills/ui-ux-pro-max/scripts/search.py`。

## Task 2: 修改 claude-runner UI mode

### Files
- `apps/claude-runner/src/claudeRunner.js` — 修改

### Steps
- [ ] 在 `buildModePrompt` 中为 `ui` mode 前置 `/ui-ux-pro-max`。
- [ ] 要求 UI 阶段生成静态 HTML 原型和设计系统产物。
- [ ] 修改 `writeModeArtifacts('ui')`，写出 `ui-guidelines.md`、`ui-prototype.md`、`design-system/MASTER.md`、`prototype/index.html`。
- [ ] 保持 UI mode 不使用 `Bash`，只允许 `Read/Write/Edit`。

## Task 3: 后端暴露阶段 artifacts

### Files
- `apps/server/src/main/java/com/aifactory/dto/ArtifactView.java` — 创建
- `apps/server/src/main/java/com/aifactory/dto/ResultView.java` — 修改
- `apps/server/src/main/java/com/aifactory/service/WorkflowService.java` — 修改

### Steps
- [ ] 新增 `ArtifactView` record，字段为 `stage/name/type/path/downloadable`。
- [ ] 在 `ResultView` 增加 `List<ArtifactView> artifacts`。
- [ ] 更新所有 `new ResultView(...)` 构造调用。
- [ ] `updateAfterPrd` 使用 `prdResult.artifacts()` 生成 requirement 阶段 artifacts。
- [ ] `updateAfterUi` 合并已有 artifacts 并加入 UI 阶段 artifacts。
- [ ] `updateAfterGenerate` 保留已有 artifacts 并加入 code/test 阶段 artifact 条目。

## Task 4: 讨论消息支持 options

### Files
- `apps/server/src/main/java/com/aifactory/dto/DiscussionMessage.java` — 修改
- `apps/server/src/main/java/com/aifactory/service/ClaudeCodeService.java` — 修改
- `apps/server/src/main/java/com/aifactory/service/DiscussionService.java` — 修改

### Steps
- [ ] `DiscussionMessage` 增加 `List<String> options` 字段，并让静态构造方法保持兼容。
- [ ] 更新 `ClaudeCodeService.buildDiscussionPrompt`，要求 AI 返回 JSON：`question/options/complete/summary`。
- [ ] 在 `DiscussionService` 中解析 AI 响应 JSON，转换为带 options 的 AI message。
- [ ] 解析失败时保留原文本，options 为空。
- [ ] 完成判断同时支持 JSON `complete=true` 和原有完成标记。

## Task 5: 前端展示讨论选项和阶段产物

### Files
- `apps/web/src/components/DiscussionPanel.vue` — 修改
- `apps/web/src/components/DesignResult.vue` — 修改

### Steps
- [ ] `DiscussionPanel.vue` 对最新 AI 消息展示 options chips。
- [ ] 点击 option 直接通过现有 `send` 事件发送选项文本。
- [ ] 保留 textarea 自由输入。
- [ ] `DesignResult.vue` 按 `result.artifacts` 分组展示需求阶段、UI 阶段、代码测试阶段。
- [ ] 保留现有打开项目、查看报告、下载 zip 按钮。
- [ ] 展示 `result.prdMarkdown` 的 Markdown 文本摘要。

## Task 6: 验证

### Files
- `apps/claude-runner/src/claudeRunner.js` — 验证
- `apps/server` — 测试
- `apps/web` — 构建

### Steps
- [ ] 运行 runner skill discovery Node 检查，确认包含 `ui-ux-pro-max`。
- [ ] 运行 `yarn server:test`。
- [ ] 运行 `yarn build:web`。
- [ ] 检查 `grep` 确认 `ui` prompt 会调用 `/ui-ux-pro-max`。
