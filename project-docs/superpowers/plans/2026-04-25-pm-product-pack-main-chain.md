# PM Product Pack 主链路接入实施计划

## Goal

让 AI 软件工厂主链路在 PRD 阶段通过 Claude CLI 实际使用 `.claude/skills/pm-product-pack`，一次性产出 PRD、User Stories、WWAS、Test Scenarios、Dummy Dataset，并移除此前误接入到 `packages/skills` 和后端 Java runtime skill 的改动。

## Architecture

主链路保持为：`apps/web` → `apps/server` → `apps/claude-runner` → `claude CLI`。本次不使用 `packages/skills` / `SkillRegistry` 驱动主链路。`claude-runner` 负责在 `prd` mode 中把用户需求包装成 `/pm-product-pack` slash-command-first prompt，并将 Claude CLI 输出写入 `project/prd.md` 与 `project/pm-product-pack.md`。

## Tech Stack

- Spring Boot: 现有后端主链路保持不变
- Node.js: `apps/claude-runner/src/claudeRunner.js`
- Claude Code skills: `.claude/skills/<skill>/SKILL.md`
- Yarn / Maven: 验证现有测试

## Task 1: 移除误接入的 runtime PM skill

### Files
- `packages/skills/src/index.js` — 修改
- `packages/skills/create-prd-skill/` — 删除
- `packages/skills/user-stories-skill/` — 删除
- `packages/skills/wwas-skill/` — 删除
- `packages/skills/test-scenarios-skill/` — 删除
- `packages/skills/dummy-dataset-skill/` — 删除
- `apps/server/src/main/resources/prompts/pm/` — 删除
- `apps/server/src/main/java/com/aifactory/skill/PmPromptSkillSupport.java` — 删除
- `apps/server/src/main/java/com/aifactory/skill/CreatePrdSkillAdapter.java` — 删除
- `apps/server/src/main/java/com/aifactory/skill/UserStoriesSkillAdapter.java` — 删除
- `apps/server/src/main/java/com/aifactory/skill/WwasSkillAdapter.java` — 删除
- `apps/server/src/main/java/com/aifactory/skill/TestScenariosSkillAdapter.java` — 删除
- `apps/server/src/main/java/com/aifactory/skill/DummyDatasetSkillAdapter.java` — 删除
- `apps/server/src/main/java/com/aifactory/service/DesignArtifactFactory.java` — 修改

### Steps
- [ ] 从 `packages/skills/src/index.js` 删除 5 个 PM runtime skill catalog 条目。
- [ ] 删除 5 个 `packages/skills/*-skill` 目录。
- [ ] 删除 `apps/server/src/main/resources/prompts/pm/`。
- [ ] 删除 6 个 Java runtime PM skill 文件。
- [ ] 从 `DesignArtifactFactory` 删除 `createPmMarkdownOutput` 方法。

## Task 2: 新增 Claude CLI PM product pack skill

### Files
- `.claude/skills/pm-product-pack/SKILL.md` — 创建

### Steps
- [ ] 创建 `.claude/skills/pm-product-pack/`。
- [ ] 写入 `SKILL.md`，包含 YAML frontmatter：`name: pm-product-pack`。
- [ ] 定义非交互自动化执行规则。
- [ ] 固定输出章节：PRD、User Stories、WWAS、Test Scenarios、Dummy Dataset、待确认问题。

## Task 3: 修改 claude-runner 主链路调用 skill

### Files
- `apps/claude-runner/src/claudeRunner.js` — 修改

### Steps
- [ ] 新增 repo root 解析函数，确保 Claude CLI cwd 能发现仓库根目录 `.claude/skills`。
- [ ] 修改 skill 发现逻辑，从 `.claude/skills/*/SKILL.md` 读取 skill 名。
- [ ] 新增 `buildModePrompt(mode, prompt, workspace)`，在 `prd` mode 前置 `/pm-product-pack`。
- [ ] 修改 `runTask` 调用 Claude CLI 时使用 repo root cwd，并通过 prompt 明确目标输出目录。
- [ ] 修改 `writeModeArtifacts('prd')`，同时写入 `project/prd.md` 和 `project/pm-product-pack.md`。

## Task 4: 验证

### Files
- `apps/claude-runner/src/claudeRunner.js` — 测试/检查
- `.claude/skills/pm-product-pack/SKILL.md` — 检查
- `apps/server` — 测试

### Steps
- [ ] 运行 `yarn server:test`，确认删除 runtime adapter 后 Spring 编译和测试通过。
- [ ] 运行 Node one-liner 调用 `checkEnvironment()`，确认 availableSkills 包含 `pm-product-pack`。
- [ ] 检查 `packages/skills/src/index.js` 不再包含误加的 5 个 PM runtime skill。
- [ ] 如环境允许，启动 runner smoke test；否则说明未做完整 CLI smoke 的原因。
