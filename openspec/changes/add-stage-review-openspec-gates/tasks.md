## 1. 工作流审查状态

- [x] 1.1 扩展工作流状态 DTO，暴露 `awaiting_review`、当前审查阶段、当前阶段产物、已确认阶段和修订状态。
- [x] 1.2 将 `WorkflowService` 从单次自动阶段循环重构为可恢复的阶段执行器，并能在每个可审查阶段完成后暂停。
- [x] 1.3 在内存态 run 中保存用户确认后继续执行下一阶段所需的字段。
- [x] 1.4 新增确认当前阶段并继续下一阶段的工作流 API。
- [x] 1.5 新增提交当前阶段修订反馈的工作流 API。
- [x] 1.6 确保最终阶段确认后完成交付，不再启动额外阶段。

## 2. 阶段审查 UI

- [x] 2.1 新增可复用阶段审查面板，采用三栏工作台布局：左侧需求输入入口和主流程进度，中间当前阶段审查、产物列表、产物预览、阶段讨论和操作按钮，右侧 Agent 状态、OpenSpec 修订状态和执行日志。
- [x] 2.2 保留需求输入入口，即使工作流进入阶段审查，也能在左侧或同等显著位置看到需求输入/起点信息。
- [x] 2.3 将“确认通过，进入下一阶段”作为主路径按钮接入工作流确认 API，并在按钮文案中展示下一阶段名称。
- [x] 2.4 将修订反馈提交接入工作流修订反馈 API，并让反馈输入框明确绑定当前审查阶段。
- [x] 2.5 将 OpenSpec 操作按钮放入“需要修改/修订流程”区域，支持展开收起，并按 `changeId`、`awaiting_review`、执行中和错误状态控制禁用与 loading 展示。
- [x] 2.6 在右侧展示当前 `changeId` 以及 Proposal、Apply、Archive 的状态和时间信息。
- [x] 2.7 当可审查阶段没有产物时展示清晰的空状态。
- [x] 2.8 保持现有进度和日志展示兼容 `awaiting_review` 状态。

## 3. OpenSpec 阶段修订集成

- [x] 3.1 新增后端 DTO 或模型，表示包含 `workflowRunId`、`stageKey`、`artifactPaths`、`userFeedback` 和 `changeId` 的阶段修订上下文。
- [x] 3.2 新增 API 或 runner 桥接能力，用当前阶段上下文触发 OpenSpec explore。
- [x] 3.3 新增 API 或 runner 桥接能力，用当前阶段上下文触发 OpenSpec propose，并保存生成的 `changeId`。
- [x] 3.4 新增 API 或 runner 桥接能力，对已保存的 `changeId` 触发 OpenSpec apply。
- [x] 3.5 新增 API 或 runner 桥接能力，对已保存的 `changeId` 触发 OpenSpec archive。
- [x] 3.6 OpenSpec archive 完成后，将工作流返回到同一阶段的 `awaiting_review` 状态，而不是自动继续。
- [x] 3.7 在 OpenSpec prompt 或 runner 上下文中包含当前阶段允许修改的路径范围。

## 4. Markdown 讨论展示

- [x] 4.1 为 web 应用增加 Markdown 渲染和 HTML 清洗依赖。
- [x] 4.2 创建可复用 Markdown 消息组件，用于安全渲染讨论内容。
- [x] 4.3 将需求讨论面板中的纯文本消息展示替换为 Markdown 消息组件。
- [x] 4.4 在阶段审查讨论区域复用 Markdown 消息组件。
- [x] 4.5 为消息气泡中的 Markdown 表格、列表、段落和代码块增加响应式样式。

## 5. 验证

- [x] 5.1 增加后端测试，验证阶段完成后进入 `awaiting_review` 且不会自动启动下一阶段。
- [x] 5.2 增加后端测试，验证用户确认阶段后能从正确的下一阶段继续执行。
- [x] 5.3 增加后端测试，验证修订反馈和 OpenSpec 操作上下文包含 `workflowRunId`、`stageKey`、`artifactPaths` 和 `userFeedback`。
- [x] 5.4 增加前端测试或手工验证，覆盖阶段审查 UI 的主要操作。
- [x] 5.5 验证 Markdown 渲染支持表格、列表和代码块，并能清洗不安全 HTML。
- [x] 5.6 运行 server 测试、web build 和端到端工作流冒烟测试，覆盖“直接确认”和“修订后确认”两条路径。
