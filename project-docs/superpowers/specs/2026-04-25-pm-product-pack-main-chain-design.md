# PM Product Pack 主链路接入设计

## 背景

当前 AI 软件工厂主链路是：前端输入需求，Spring Boot 后端调用 `apps/claude-runner`，runner 再通过 Claude CLI 执行 `prd`、`ui`、`generate`、`fix-tests` 等 mode。用户希望 PM skills 真正进入这条主链路，由 Claude CLI 使用 skill 一次性产出产品相关结果，而不是接入 `packages/skills` / Java `SkillRegistry` 这套平台 runtime skill。

## 目标

- 主链路中由 Claude CLI 实际使用 `.claude/skills` 下的 PM skill。
- 一次执行生成 5 类 PM 产物：
  - PRD
  - User Stories
  - WWAS
  - Test Scenarios
  - Dummy Dataset
- 移除此前误加的 `packages/skills` 和后端 runtime adapter 改动。
- 保持现有前后端 API 尽量不变。

## 非目标

- 本次不实现真实 UI 原型图、截图预览或可交互原型。
- 本次不改造成 Java `SkillRegistry` 驱动主链路。
- 本次不引入外部 PM skills 仓库作为 git submodule 或运行时依赖。

## 备选方案

### 方案 A：五个 `.claude/skills` 依次执行

优点：产物模块化，后续可单独重跑。

缺点：主链路需要多次 Claude CLI 调用，耗时和失败面更大。

### 方案 B：一个 `.claude/skills/pm-product-pack` 一次执行

优点：最符合当前用户选择；主链路简单；一次 CLI 调用即可产出完整 PM 产品包。

缺点：单个 skill 更大，后续想单独重跑某个产物需要再拆分。

### 方案 C：只把 PM 规则写进 runner prompt

优点：实现最简单。

缺点：不是真正使用 Claude Code skill，不符合目标。

## 推荐方案

采用方案 B：新增一个 `.claude/skills/pm-product-pack/SKILL.md`，在该 skill 内定义 5 类 PM 产物的输出结构。`apps/claude-runner` 在 `prd` mode 中显式调用 `/pm-product-pack`，并将返回内容写入 `project/pm-product-pack.md`，同时兼容写入 `project/prd.md` 以不破坏现有前端展示。

## 设计细节

### 1. 删除误接入的 runtime skill 改动

移除以下新增内容：

- `packages/skills/create-prd-skill/`
- `packages/skills/user-stories-skill/`
- `packages/skills/wwas-skill/`
- `packages/skills/test-scenarios-skill/`
- `packages/skills/dummy-dataset-skill/`
- `apps/server/src/main/resources/prompts/pm/`
- `apps/server/src/main/java/com/aifactory/skill/PmPromptSkillSupport.java`
- `apps/server/src/main/java/com/aifactory/skill/CreatePrdSkillAdapter.java`
- `apps/server/src/main/java/com/aifactory/skill/UserStoriesSkillAdapter.java`
- `apps/server/src/main/java/com/aifactory/skill/WwasSkillAdapter.java`
- `apps/server/src/main/java/com/aifactory/skill/TestScenariosSkillAdapter.java`
- `apps/server/src/main/java/com/aifactory/skill/DummyDatasetSkillAdapter.java`

回退 `packages/skills/src/index.js` 中新增的 5 个 runtime skill catalog 条目。

回退 `DesignArtifactFactory` 中新增的通用 PM runtime 输出方法。

### 2. 新增 Claude CLI skill

新增：

- `.claude/skills/pm-product-pack/SKILL.md`

该 skill 使用 YAML frontmatter：

- `name: pm-product-pack`
- `description: 一次性生成 PRD、用户故事、WWAS、测试场景和样例数据的 PM 产品产物包。`

内容要求：

- 支持非交互自动化执行。
- 不等待用户二次确认。
- 未知信息放入“待确认问题”。
- 输出 Markdown。
- 固定章节：
  - 产品需求文档 PRD
  - 用户故事 User Stories
  - WWAS 条目
  - 测试场景 Test Scenarios
  - 样例数据 Dummy Dataset
  - 待确认问题

### 3. 修改 Claude Runner 的 `prd` mode

修改 `apps/claude-runner/src/claudeRunner.js`：

- 为 `prd` mode 构造 slash-command-first prompt：
  - `/pm-product-pack`
  - 附加需求内容、工作目录、输出要求。
- 保持 HTTP API 不变，后端仍然调用 `runTask(..., "prd", ...)`。
- 保持 `writeModeArtifacts("prd", ...)` 写入 `project/prd.md`。
- 同时新增写入 `project/pm-product-pack.md`，便于区分完整 PM 产品包。

### 4. 确保 Claude CLI 能发现 `.claude/skills`

当前 runner 的 `prd` mode 以 `workspace.taskRoot` 作为 cwd，可能无法向上发现仓库根目录的 `.claude/skills`。

推荐修改：

- 计算 repo root。
- `prd`、`ui`、`generate`、`fix-tests` 的 CLI cwd 使用 repo root。
- 在 system prompt / user prompt 中明确目标输出目录是 `workspace.projectDir`。

这样 Claude CLI 能看到 repo root 的 `.claude/skills`，同时仍把产物写到每次任务的 workspace。

### 5. 环境能力显示

可选改进 `detectAvailableSkills()`：

- 从 repo root `.claude/skills/*/SKILL.md` 读取 skill 名。
- 不再只从 `CLAUDE.md` 的反引号 slash command 中猜测。

这不影响主链路，但有助于前端环境页展示 Claude CLI 可用 skill。

## prd-generator 能力评估

`.claude/skills/prd-generator` 当前可以生成：

- `PRD.md`
- `PRD.docx`
- `UI-Design-Spec.md`
- PlantUML 流程图 `.puml`
- 可选 PlantUML PNG 流程图预览

它不能生成：

- 真实 UI 原型
- UI 设计图
- 浏览器截图
- 可交互 HTML/Vue 原型

因此本次不使用 `prd-generator` 承担原型生成职责。

## 验证方式

- 运行 runner/env 或前端环境页，确认能看到 `pm-product-pack` skill。
- 执行一次 PRD 阶段，确认 Claude CLI 响应中使用 `/pm-product-pack`。
- 确认生成目录包含：
  - `project/prd.md`
  - `project/pm-product-pack.md`
- 运行：
  - `yarn server:test`
  - 如改动 runner 相关脚本，可启动 runner 做一次 smoke test。
