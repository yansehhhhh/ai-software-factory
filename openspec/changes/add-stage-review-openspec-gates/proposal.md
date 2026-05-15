## Why

当前 AI 软件工厂主链路在每个阶段完成后会自动进入下一阶段，用户无法在阶段产物被后续环节消费前确认、讨论或修订，容易导致 PRD、UI、架构或代码阶段的偏差持续放大。

本变更引入阶段产物确认门控，并将 OpenSpec 作为阶段产物修订的管理机制，让每个阶段都可以“先验收、再推进”。

## What Changes

- 主链路每个需要验收的阶段完成后进入 `awaiting_review` 状态，不再自动进入下一阶段。
- 用户可以查看当前阶段产物，并选择：
  - 确认通过，进入下一阶段。
  - 发起修改讨论，围绕当前阶段产物继续与 AI 沟通。
- 阶段修订工作区提供 OpenSpec 操作入口：
  - 讨论：触发 `/opsx:explore`，基于当前阶段产物和用户反馈探索修改方案。
  - 制定计划：触发 `/opsx:propose`，为当前阶段修订创建 OpenSpec change。
  - 执行计划：触发 `/opsx:apply`，按 change tasks 修改当前阶段产物。
  - 归档：触发 `/opsx:archive`，归档已完成的阶段修订 change。
- OpenSpec 阶段修订需要关联 `workflowRunId`、`stageKey`、`artifactPaths`、`userFeedback` 和 `changeId`。
- OpenSpec 修订归档后回到当前阶段 `awaiting_review`，只有用户再次点击确认通过才进入下一阶段。
- 现有需求讨论/阶段讨论消息从纯文本展示升级为安全 Markdown 展示，支持标题、列表、表格、代码块等格式，并进行 HTML 清洗以降低 XSS 风险。

## Capabilities

### New Capabilities
- `stage-review-gates`: 定义阶段产物完成后的确认门控、用户确认、用户要求修改和继续执行行为。
- `openspec-stage-revisions`: 定义阶段修订如何绑定 OpenSpec explore/propose/apply/archive 流程及其上下文数据。
- `markdown-discussion-rendering`: 定义需求讨论和阶段讨论消息的 Markdown 展示能力与安全约束。

### Modified Capabilities

无。

## Impact

- 后端工作流状态机：`apps/server/src/main/java/com/aifactory/service/WorkflowService.java`
- 后端工作流 API：`apps/server/src/main/java/com/aifactory/controller/WorkflowController.java`
- 后端讨论/确认服务：`apps/server/src/main/java/com/aifactory/service/DiscussionService.java`
- 前端工作流页面：`apps/web/src/views/HomeView.vue`
- 前端讨论组件：`apps/web/src/components/DiscussionPanel.vue`
- 前端结果/产物展示组件：`apps/web/src/components/DesignResult.vue`
- 前端 API 封装：`apps/web/src/api/workflow.js`、`apps/web/src/api/discussion.js`
- 可能新增 Markdown 渲染与 HTML 清洗依赖，如 `markdown-it` 与 `dompurify`。
- 可能新增后端 DTO，用于阶段确认、阶段修订上下文和 OpenSpec 操作请求。
