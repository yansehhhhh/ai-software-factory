## 背景

当前主链路由 `WorkflowService.runPipeline()` 按阶段顺序自动执行。阶段完成后，服务端立即进入下一阶段，前端通过轮询 `/api/workflow/status` 展示进度和产物。现有讨论区主要服务需求澄清，消息以纯文本方式展示，无法良好呈现模型输出中的 Markdown 表格、列表和代码块。

本设计将主链路从“全自动串行执行”调整为“阶段执行 + 产物确认 + 用户驱动继续”。OpenSpec 不替代主链路状态机，而是在用户要求修改某个阶段产物时，作为阶段修订的变更管理流程。

目标体验：

```text
阶段运行
  ↓
产物完成
  ↓
等待确认 awaiting_review
  ├─ 确认通过 → 下一阶段
  └─ 需要修改 → OpenSpec 阶段修订
                  ├─ 讨论 /opsx:explore
                  ├─ 制定计划 /opsx:propose
                  ├─ 执行计划 /opsx:apply
                  └─ 归档 /opsx:archive
                       ↓
                    回到 awaiting_review
```

## 目标 / 非目标

**目标：**

- 每个主链路阶段完成后暂停，等待用户确认。
- 用户确认通过后才执行下一阶段。
- 用户需要修改时，可以围绕当前阶段产物继续讨论并通过 OpenSpec 管理修订。
- OpenSpec 修订上下文必须携带 `workflowRunId`、`stageKey`、`artifactPaths`、`userFeedback`。
- OpenSpec 归档后回到当前阶段确认态，而不是自动推进。
- 讨论消息支持安全 Markdown 展示，包括表格、列表、代码块。
- 保持现有主链路阶段和产物目录结构，不引入平行架构。

**非目标：**

- 不把 OpenSpec 改造成主链路调度器。
- 不要求每个阶段都必须创建 OpenSpec change；只有用户要求修改时才进入。
- 不实现多人协作审批、权限体系或持久化审计流。
- 不要求 OpenSpec apply 能任意修改全仓库；阶段修订应限制在当前阶段相关产物范围。

## 设计决策

### 1. 主链路增加阶段确认状态，而不是拆成多个独立 workflow

`WorkflowService` 保留统一 run 对象，但阶段执行逻辑从一次性 for-loop 调整为可暂停/恢复的阶段状态机。

建议状态：

```text
idle
running
awaiting_review
revision_discussing
revision_proposed
revision_applying
revision_archived
success
error
```

阶段完成后记录当前 `stageKey`、阶段产物、下一阶段索引，并进入 `awaiting_review`。`approve` API 只负责推进下一阶段。

**备选方案：**

- 继续自动串行，只在前端做“伪暂停”：会导致后续阶段已经消费未确认产物，无法满足需求。
- 每个阶段都创建新 workflow：实现成本更高，状态和产物聚合复杂。

### 2. OpenSpec 作为“阶段修订上下文”，不是默认流程

当用户点击“讨论 / 制定计划 / 执行计划 / 归档”时，系统基于当前阶段上下文构造 OpenSpec 操作输入。每个 OpenSpec change 绑定一个阶段修订。

建议修订上下文：

```json
{
  "workflowRunId": "...",
  "stageKey": "ui-design",
  "stageTitle": "UI设计",
  "artifactPaths": ["docs/.../UI原型/设计稿/index.html"],
  "userFeedback": "移动端预约流程需要更突出签到二维码",
  "changeId": "revise-ui-design-..."
}
```

OpenSpec 操作完成后更新阶段修订状态。归档成功只表示修订流程完成，系统仍回到 `awaiting_review` 等待用户确认。

**备选方案：**

- 每次阶段完成自动创建 OpenSpec change：会让无修改场景变重。
- 只用普通聊天修改产物：缺少 proposal/tasks/spec 的可追踪性。

### 3. 前端新增通用 StageReviewPanel

现有 `DiscussionPanel` 主要面向需求澄清。阶段确认能力应抽象为通用阶段审查面板，复用或扩展现有聊天能力。

页面布局参考确认后的三栏工作台形态：

```text
┌──────────────────────────────────────────────────────────────────────────────┐
│ 顶部：项目选择 / 当前阶段 / awaiting_review 状态 / 下一阶段 / 用户入口         │
├──────────────┬───────────────────────────────────────────────┬───────────────┤
│ 左侧栏        │ 中央阶段审查区                                  │ 右侧状态栏     │
│              │                                               │               │
│ 需求输入入口   │ 当前阶段标题 + 等待用户确认提示                    │ Agent 状态     │
│ 主流程进度     │ 阶段产物列表 + Markdown/HTML 预览                  │ OpenSpec 状态  │
│              │ 阶段修订讨论区 + 反馈输入框                         │ 执行日志       │
│              │ 修订操作按钮：讨论 / 制定计划 / 执行计划 / 归档       │               │
│              │ 主操作按钮：确认通过，进入下一阶段                   │               │
└──────────────┴───────────────────────────────────────────────┴───────────────┘
```

实现约束：

- 需求输入入口必须保留在左侧或同等显著位置，即使工作流已进入阶段审查，也不能被阶段审查页面完全替代。
- 左侧主流程进度需要展示每个阶段的状态、进度和当前高亮阶段。
- 中央区域以当前阶段审查为主，包含阶段标题、`awaiting_review` 状态提示、当前阶段产物列表和选中产物预览。
- “确认通过，进入下一阶段”是主路径按钮，应比 OpenSpec 修订按钮更显著，并显示下一阶段名称。
- OpenSpec 修订按钮是修改分支操作，建议默认放在“需要修改/收起修订流程”区域内，避免用户误以为每个阶段都必须走修订流程。
- 阶段讨论输入框应绑定当前阶段上下文，提示用户描述希望修改的当前阶段产物内容。
- 右侧状态栏展示 Agent 状态、当前 OpenSpec `changeId`、Proposal / Apply / Archive 状态和执行日志。
- 当没有 `changeId` 时禁用执行计划和归档；当非 `awaiting_review` 或修订操作执行中时禁用相关按钮并展示 loading/error 状态。

`DiscussionPanel` 可继续用于初始需求澄清，但消息渲染能力抽成 MarkdownMessage 组件，供需求讨论和阶段讨论共用。

### 4. Markdown 渲染使用白名单清洗

前端引入 Markdown 渲染和 HTML 清洗，例如 `markdown-it` + `dompurify`。所有 AI 和用户消息渲染前必须清洗，禁止直接把未清洗 HTML 传给 `v-html`。

推荐封装：

```text
MarkdownMessage.vue
  props: content
  computed: sanitizedHtml = DOMPurify.sanitize(markdown.render(content))
```

### 5. 阶段修订范围按 stageKey 限制

OpenSpec apply 执行时应把允许修改范围写入 prompt，降低越权修改风险：

| 阶段 | 建议修改范围 |
|---|---|
| product-design-artifacts | `docs/${项目}/产品设计/` |
| ui-design | `docs/${项目}/UI原型/` 和 UI 设计规范 |
| architecture-design/api-design/database-design | `docs/${项目}/架构设计/`、`数据库设计/` |
| frontend-development | `workspace/runtime/${taskId}/project` 前端工程 |
| backend-development | `workspace/runtime/${taskId}/project/backend` |
| test-case-generation/playwright-execution | `workspace/runtime/${taskId}/project/tests`、测试报告 |

## 风险 / 权衡

- 阶段暂停会拉长端到端自动生成时间 → 提供明确的“确认通过，进入下一阶段”按钮，并保留状态提示。
- OpenSpec 操作链路较重 → 只在用户要求修改时进入，不影响无修改的快速路径。
- Markdown 渲染可能引入 XSS 风险 → 使用 DOMPurify 清洗，禁用或清洗原始 HTML。
- 工作流状态变复杂 → 明确 run-level 状态、stage-level 状态和 revision-level 状态边界。
- OpenSpec apply 可能修改超出当前阶段的文件 → 在 prompt 和后端约束中携带 stageKey 的允许修改范围，并在结果中展示修改文件。

## 迁移计划

1. 先实现阶段完成后暂停和确认继续。
2. 再把讨论消息渲染改为安全 Markdown。
3. 再新增阶段修订上下文与 OpenSpec 操作按钮。
4. 最后补充端到端验证，覆盖“确认通过”和“需要修改后归档再确认”两条路径。

回滚策略：保留后端配置或开关使 workflow 可退回自动串行执行；若 OpenSpec 操作不可用，阶段确认仍可正常使用。

## 待确认问题

- OpenSpec 操作是由平台后端直接调用 CLI，还是由 Claude Code/runner 代理执行？
- 阶段修订 changeId 是否由系统自动生成，还是允许用户命名？
- 是否需要持久化 workflow run 和 revision records，还是先沿用当前内存态？
- Markdown 是否允许渲染原始 HTML，还是完全禁用 HTML？建议第一版禁用或清洗后渲染。
