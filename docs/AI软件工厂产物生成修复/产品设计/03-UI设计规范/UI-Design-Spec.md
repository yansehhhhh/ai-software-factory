# 产物生成与预览修复计划

## Goal
修复产品设计阶段页面流转图无法预览的问题，并让架构设计阶段在 `架构设计/01-系统架构/` 下同时产出 `系统架构设计.md`、`系统架构图.puml` 和 `系统架构图.svg`。同步更新主链路目录约定和已整合的 `pm-product-pack`、`prd-generator` skill 文档，最后完成验证并重启服务。

## Architecture
产物生成入口在 `apps/claude-runner/src/claudeRunner.js`，负责把 Claude 输出或兜底内容落盘到 `docs/${项目名称}/`。服务端 `WorkflowService` 将产物路径转换为结果面板可展示的名称、类型和 `/api/artifacts` URL，`ArtifactController` 负责返回文件内容。前端 `DesignResult.vue` 只消费后端返回的 artifact 元数据并打开对应 URL，因此本次改动应优先保证生成端落出可直接预览的 SVG，同时让 `.puml` 可 inline 预览源码。

## Tech Stack
- Node.js / JavaScript：Claude Runner 产物生成
- Spring Boot / Java：产物访问与结果元数据
- Vue 3：结果面板展示
- Markdown：CLAUDE.md、README.md、Skill 文档
- PlantUML / SVG：流程图和架构图产物格式

## Task 1: 补充产物生成测试覆盖

### Files
- `apps/server/src/test/java/com/aifactory/WorkflowServiceIntegrationTests.java` — 修改

### Steps
- [ ] 在 `shouldGenerateStructuredDesignArtifacts` 的 PRD 阶段 mock 产物列表中加入 `docs/AI质检助手/产品设计/04-流程图/页面流转图.svg`。
- [ ] 在架构阶段 mock 产物列表中加入 `docs/AI质检助手/架构设计/01-系统架构/系统架构图.puml` 和 `docs/AI质检助手/架构设计/01-系统架构/系统架构图.svg`。
- [ ] 增加断言：页面流转图 SVG 以 `image` 类型出现在需求阶段产物中。
- [ ] 增加断言：系统架构图 puml 以 `plantuml` 类型出现在架构设计中。
- [ ] 增加断言：系统架构图 svg 以 `image` 类型出现在架构设计中。
- [ ] 运行服务端测试，确认新增断言在实现前失败。

## Task 2: 生成页面流转图 SVG 并规范 PlantUML

### Files
- `apps/claude-runner/src/claudeRunner.js` — 修改

### Steps
- [ ] 将 `buildPlantUmlDiagram("页面流转图", "page-flow", content)` 输出改成纯 `@startuml` 开头，不使用中文 diagram id。
- [ ] 将 page-flow 分支改成稳定页面节点语法，避免 `[*]` 状态伪节点和 `rectangle/diamond` 混用。
- [ ] 新增 `buildPageFlowSvg(content)`，用内联 SVG 表达“需求输入 → 需求讨论 → 产物展示 → 后续设计”的页面流转。
- [ ] 在 `writeModeArtifacts(mode === "prd")` 中定义 `pageFlowSvgFile = path.join(workspace.artifactDir.flowDir, "页面流转图.svg")`。
- [ ] 在 PRD 阶段 Promise 中写入 `页面流转图.svg`。
- [ ] 在 PRD 阶段 artifacts 返回列表中包含 `pageFlowSvgFile`，让服务端结果面板能展示。

## Task 3: 生成系统架构图 PUML 和 SVG

### Files
- `apps/claude-runner/src/claudeRunner.js` — 修改

### Steps
- [ ] 新增 `buildSystemArchitecturePuml(content)`，生成 `@startuml` / `@enduml` 包裹的组件关系图，包含用户、Web 前端、Spring Boot 后端、Claude Runner、Claude Code CLI、docs/generated 产物存储。
- [ ] 新增 `buildSystemArchitectureSvg(content)`，生成可直接预览的系统架构 SVG，包含同样节点与数据流。
- [ ] 在 `writeModeArtifacts(mode === "architecture")` 中定义 `systemPumlFile` 和 `systemSvgFile`。
- [ ] 在架构阶段 Promise 中写入 `系统架构图.puml` 和 `系统架构图.svg`。
- [ ] 在架构阶段 return 列表中包含 `systemFile`、`systemPumlFile`、`systemSvgFile`、`deploymentFile`、`techSelectionFile`。

## Task 4: 更新后端产物识别与 inline 预览

### Files
- `apps/server/src/main/java/com/aifactory/controller/ArtifactController.java` — 修改
- `apps/server/src/main/java/com/aifactory/service/WorkflowService.java` — 修改

### Steps
- [ ] 在 `ArtifactController.mediaType` 中把 `.puml` 返回为 `text/plain;charset=UTF-8`。
- [ ] 在 `ArtifactController.inline` 中加入 `.puml`，避免浏览器打开 puml 时触发下载。
- [ ] 在 `WorkflowService.artifactName` 中为 `页面流转图.svg` 返回 `页面流转图预览`。
- [ ] 在 `WorkflowService.artifactName` 中为 `系统架构图.puml` 返回 `系统架构图`。
- [ ] 在 `WorkflowService.artifactName` 中为 `系统架构图.svg` 返回 `系统架构图预览`。

## Task 5: 更新前端结果面板映射

### Files
- `apps/web/src/components/DesignResult.vue` — 修改

### Steps
- [ ] 在 `artifactNameMap` 中加入 `页面流转图.svg: "页面流转图预览"`。
- [ ] 在 `artifactNameMap` 中加入 `系统架构图.puml: "系统架构图"`。
- [ ] 在 `artifactNameMap` 中加入 `系统架构图.svg: "系统架构图预览"`。
- [ ] 保持 `image` 类型显示为 `图片`，点击后仍通过 `openLink(artifact.path)` 打开后端 inline URL。

## Task 6: 更新仓库约定文档

### Files
- `CLAUDE.md` — 修改
- `README.md` — 修改

### Steps
- [ ] 在 `CLAUDE.md` 的 `产品设计/04-流程图/` 下补充 `页面流转图.svg`。
- [ ] 在 `CLAUDE.md` 的 `架构设计/01-系统架构/` 下补充 `系统架构设计.md`、`系统架构图.puml`、`系统架构图.svg`。
- [ ] 在 `README.md` 的 `产品设计/04-流程图/` 下补充 `页面流转图.svg`。
- [ ] 在 `README.md` 的 `架构设计/01-系统架构/` 下补充 `系统架构设计.md`、`系统架构图.puml`、`系统架构图.svg`。

## Task 7: 更新 PM 与 PRD skill 文档

### Files
- `.claude/skills/pm-product-pack/SKILL.md` — 修改
- `.claude/skills/prd-generator/SKILL.md` — 修改

### Steps
- [ ] 在 `pm-product-pack` 中说明主链路已将 PM 产物包能力整合进 `prd-generator`，不再要求单独输出 `pm-product-pack.md`。
- [ ] 在 `pm-product-pack` 中说明用户故事、WWAS、测试场景、样例数据应进入产品设计拆分产物，特别是 `05-附录/术语表.md`。
- [ ] 在 `prd-generator` 目录结构中补充 `页面流转图.svg`。
- [ ] 在 `prd-generator` 流程图要求中规定 PlantUML 使用纯 `@startuml`，不要使用中文 diagram id。
- [ ] 在 `prd-generator` 中说明页面流转图需要同时输出 `.puml` 和 `.svg`，`.svg` 用于结果面板图片预览。

## Task 8: 验证并重启服务

### Files
- `apps/server/src/test/java/com/aifactory/WorkflowServiceIntegrationTests.java` — 测试
- `apps/web/src/components/DesignResult.vue` — 构建验证
- `apps/claude-runner/src/claudeRunner.js` — 运行时验证

### Steps
- [ ] 运行服务端相关测试，确认 WorkflowService 产物识别通过。
- [ ] 运行前端构建或测试，确认 Vue 组件映射无语法错误。
- [ ] 如有可用脚本，重启 web、server、claude-runner 相关服务；若当前没有后台服务，启动对应服务并报告状态。
- [ ] 调用 `verification-before-completion`，基于新鲜验证结果确认完成状态。
