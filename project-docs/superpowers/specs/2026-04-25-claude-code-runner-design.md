# Claude Code Runner 架构设计

日期：2026-04-25

## 1. 背景

当前系统中的需求讨论与设计生成，核心仍依赖后端内的 `Skill -> LlmClient -> ModelRouter` 路径：

- `DiscussionService` 通过 `BrainstormingSkillAdapter` 发起需求澄清
- `BrainstormingSkillAdapter` 调用 `LlmClient`
- `TemplateLlmClient` / 其他模型客户端返回文本结果
- `WorkflowService` 在 PRD / UI / 验证阶段同样通过 skill + LLM 路径生成内容

这条链路适合“文本补全型模型调用”，但不适合接入 Claude Code 这类“带工作目录、带会话、带技能、可执行工程操作”的能力。用户希望将以下能力统一改为走 Claude Code：

- 需求讨论
- 代码生成
- 测试修复

并新增独立的 `Node Claude Runner` 服务，作为 Spring Boot 与 Claude Code CLI/Agent SDK 之间的执行桥梁。

## 2. 目标

### 2.1 业务目标

1. Requirement Agent 不再走 `agent-model.yml` 或 OpenAI / DeepSeek / Ollama。
2. 系统中的需求讨论、代码生成、测试修复统一改为调用 Claude Code Runner。
3. 每个任务在独立目录 `workspace/runs/{taskId}/` 中执行，避免污染主工程。
4. 原“模型配置页面”改为“Claude Code 运行环境检查页面”。
5. 用户无需手工配置模型，由后端统一使用 Claude Code 能力完成需求讨论和工程生成。

### 2.2 非目标

1. 本轮不把所有 skill 一次性都迁移到 Node Runner。
2. 本轮不实现多 Runner 分布式调度。
3. 本轮不做长期持久化会话恢复（例如服务重启后恢复 Claude 内部进程状态）；首版以任务活跃期内的持久会话为主。

## 3. 推荐方案

采用 **方案 A：Spring Boot 主控，Node Runner 仅负责 Claude Code 执行**。

### 原因

- 当前系统的任务状态、工作流阶段、前端接口都由 Spring Boot 驱动，保留主控最稳。
- Claude Code 的“会话 + 工作目录 + 执行工具”语义已经超出 `LlmClient`，不适合继续塞在纯模型抽象下。
- 用独立 Runner 可以更自然地承载：
  - 任务工作目录隔离
  - Claude 会话复用
  - CLI / Agent SDK 调用
  - 环境检查接口

## 4. 总体架构

### 4.1 新链路

#### 需求讨论

```text
Frontend
  ↓ HTTP
Spring Boot DiscussionController
  ↓
DiscussionService
  ↓
ClaudeCodeService
  ↓ HTTP
Node Claude Runner
  ↓
Claude Code CLI / Agent SDK
```

#### 代码生成 / 测试修复

```text
Frontend / Workflow trigger
  ↓ HTTP
Spring Boot WorkflowService
  ↓
ClaudeCodeService
  ↓ HTTP
Node Claude Runner
  ↓
Claude Code CLI / Agent SDK
  ↓
workspace/runs/{taskId}/
```

### 4.2 目录结构

```text
apps/
├── server/
├── web/
└── claude-runner/
    ├── package.json
    ├── src/
    │   ├── index.js
    │   ├── claudeRunner.js
    │   └── taskController.js
    └── README.md
```

## 5. 组件职责设计

## 5.1 Spring Boot

### `DiscussionService`
职责：
- 管理 discussionId / taskId 与用户会话映射
- 负责启动需求讨论、发送用户消息、结束讨论确认需求
- 不再通过 `BrainstormingSkillAdapter -> LlmClient`
- 改为直接调用 `ClaudeCodeService`

### `ClaudeCodeService`（新增）
职责：
- 封装对 Node Claude Runner 的 HTTP 调用
- 提供统一接口给 `DiscussionService` / `WorkflowService`
- 统一处理：
  - 创建任务
  - 发送消息
  - 执行代码生成
  - 执行测试修复
  - 查询环境检查信息

建议接口：
- `startDiscussion(taskId, requirement)`
- `continueDiscussion(taskId, history, userMessage)`
- `generateProject(taskId, enrichedRequirement)`
- `fixTests(taskId, context)`
- `checkEnvironment()`

### `WorkflowService`
职责调整：
- 保留当前工作流状态机和前端状态接口
- 把“代码生成 / 测试修复”阶段改成调用 `ClaudeCodeService`
- PRD/UI 设计阶段可分步迁移：
  - 若本轮范围要求完全统一，则 PRD/UI 也走 Claude Runner
  - 若首版聚焦讨论 + 生成 + 修复，则暂时保留设计阶段 skill，后续再统一

**建议**：为了满足“全部走 Claude Code Runner”的目标，本轮将以下阶段统一迁移：
- brainstorming / 需求讨论
- 代码生成
- 测试修复
- 后续 PRD/UI 若仍保留，可标记为过渡方案；但目标态应全部迁走

### `ModelRouter` / `TemplateLlmClient` / 现有模型适配层
处理建议：
- 不立刻物理删除，避免一次性大拆。
- 改成“非主路径 / 兼容层”。
- `DiscussionService` 与新的生成/修复链路不再依赖它们。
- 后续再按第二阶段清理。

## 5.2 Node Claude Runner

### `src/index.js`
职责：
- 启动 HTTP 服务（默认 `7001`）
- 注册任务控制路由
- 提供健康检查

### `src/taskController.js`
职责：
- 定义 HTTP API
- 做参数校验
- 调用 `claudeRunner.js`

建议接口：

#### `POST /claude/run`
通用任务执行接口。

请求体：
```json
{
  "taskId": "task-123",
  "mode": "discussion|generate|fix-tests|plan|custom",
  "prompt": "...",
  "workspaceDir": "...",
  "sessionId": "optional",
  "metadata": {}
}
```

返回：
```json
{
  "taskId": "task-123",
  "sessionId": "session-123",
  "status": "success",
  "content": "...",
  "artifacts": [],
  "logs": []
}
```

#### `POST /claude/session/start`
创建任务会话并初始化工作目录。

#### `POST /claude/session/message`
向已有会话发送消息，适合多轮需求讨论。

#### `POST /claude/session/close`
关闭会话。

#### `GET /claude/env`
返回 Claude Code 环境检查信息。

### `src/claudeRunner.js`
职责：
- 管理 taskId -> session 映射
- 维护任务工作目录
- 调用 Claude Code CLI / Agent SDK
- 统一返回 stdout / structured result / error

建议内部能力：
- `ensureWorkspace(taskId)`
- `startSession(taskId, workspaceDir)`
- `sendMessage(taskId, prompt)`
- `runGenerate(taskId, prompt)`
- `runFixTests(taskId, prompt)`
- `checkEnvironment()`

## 6. 会话与隔离设计

### 6.1 任务隔离目录

每个任务使用独立目录：

```text
workspace/runs/{taskId}/
```

建议结构：

```text
workspace/runs/{taskId}/
├── project/          # Claude Code 实际工作的工程副本或生成目标目录
├── logs/             # runner 输出日志
├── context/          # task context / exported summaries
└── metadata.json     # 任务元信息
```

### 6.2 隔离策略

推荐策略：
1. 对于“基于现有主工程修改”的任务：
   - 将主工程必要内容复制或 worktree 化到 `workspace/runs/{taskId}/project`
   - Claude Code 只在隔离目录运行
2. 对于“新工程生成”的任务：
   - 直接在 `workspace/runs/{taskId}/project` 生成
3. Spring Boot 只读取隔离目录产物，不让 Claude Code 直接操作主仓库

### 6.3 会话持久化

用户已选择“持久任务会话”，因此：
- 每个 `taskId` 需要对应一个 Runner 内部 session
- 需求讨论期间多轮消息必须进入同一 Claude 会话
- 代码生成与测试修复可复用同一任务上下文

首版约束：
- 会话在 Runner 进程内持久存在
- 服务重启后不强制恢复同一内部 Claude 进程
- 但 `taskId -> workspaceDir -> exported context` 需要可重建

## 7. Prompt 与上下文设计

## 7.1 需求讨论

Spring Boot 发送给 Runner 的 prompt 应包含：
- 原始需求
- 当前 discussion history
- 角色约束：Requirement Agent
- 输出约束：只问一个澄清问题，必要时在完成时输出明确完成标记

建议完成标记统一为：
```text
[DISCUSSION_COMPLETE]
```

这样可替代现在 `DiscussionService` 中的 `[讨论完成]` 字符串判断。

## 7.2 代码生成

生成 prompt 应包含：
- 已确认需求
- 当前仓库或目标工程目录说明
- 输出目标
- 测试要求
- 禁止修改隔离目录外的文件

## 7.3 测试修复

修复 prompt 应包含：
- 测试失败输出
- 当前项目目录
- 修复目标
- 限制范围（只修复测试相关问题，不做无关重构）

## 8. 前端改造设计

## 8.1 删除左侧“模型配置”导航

当前侧边栏中的“模型配置”要删除或改名。

### 新导航建议
- 首页
- 历史记录
- Claude 环境检查

如果保持目标图的简洁度，也可以只保留：
- 首页
- 历史记录

并通过首页中的入口跳转到环境检查页面。

## 8.2 模型配置页改为 Claude Code 环境检查页

页面目标：显示系统是否具备调用 Claude Code 的运行条件。

### 展示字段
- Claude CLI 是否安装
- 是否已登录
- 当前工作目录
- 可用 skills
- 执行权限
- Runner 服务状态
- Runner 监听地址

### 建议后端接口

Spring Boot：
- `GET /api/claude/environment`

由 Spring Boot 转调：
- `GET http://localhost:7001/claude/env`

返回示例：
```json
{
  "runnerOnline": true,
  "cliInstalled": true,
  "loggedIn": true,
  "workingDirectory": "/Users/.../workspace/runs",
  "availableSkills": ["brainstorming", "writing-plans", "verification-before-completion"],
  "permissions": {
    "bash": true,
    "read": true,
    "write": true
  }
}
```

## 9. 迁移步骤

### Phase 1：Runner 基础设施
1. 新增 `apps/claude-runner`
2. 实现 `/claude/env`
3. 实现 `/claude/session/start`
4. 实现 `/claude/session/message`
5. 实现工作目录隔离

### Phase 2：需求讨论迁移
1. Spring Boot 新增 `ClaudeCodeService`
2. `DiscussionService` 改为调用 `ClaudeCodeService`
3. 保留现有 discussion API，不改前端协议
4. Requirement Agent 逻辑切到 Claude Code

### Phase 3：代码生成/测试修复迁移
1. 在 `WorkflowService` 中接入 `ClaudeCodeService`
2. 将生成与修复阶段切到 Runner
3. 产物从隔离目录回传给系统展示

### Phase 4：环境检查页替换
1. 删除模型配置入口
2. 前端增加 Claude 环境检查页面
3. 接入 `/api/claude/environment`

### Phase 5：旧模型链路降级为兼容层
1. 停止主路径使用 `ModelRouter`
2. 停止 discussion / generation / fix-tests 依赖 `LlmClient`
3. 视情况移除旧 provider 配置页面与文档

## 10. 风险与处理

### 风险 1：Claude Code 会话不可恢复
处理：
- 会话级状态只保证 Runner 生命周期内持久
- 关键上下文写入 `workspace/runs/{taskId}/context/`
- Runner 重启后允许重建会话

### 风险 2：Claude Code 误修改主工程
处理：
- 强制所有任务在 `workspace/runs/{taskId}/project` 执行
- 禁止 Runner 使用主仓库根目录作为工作目录

### 风险 3：Runner 与 Spring 状态不一致
处理：
- 由 Spring Boot 作为任务主状态源
- Runner 仅持有执行会话与工作目录元信息
- 所有任务事件回写 Spring

### 风险 4：讨论、生成、修复的 prompt 质量不稳定
处理：
- 为三类任务分别固定 system prompt 模板
- 完成标记、输出格式、错误格式统一规范

## 11. 设计结论

最终推荐实施路线：

1. 新增 `apps/claude-runner`，作为 Claude Code 持久会话执行器
2. Spring Boot 新增 `ClaudeCodeService` 统一调用 Runner
3. `DiscussionService` 从 `BrainstormingSkillAdapter -> LlmClient` 改为 `ClaudeCodeService -> Claude Runner`
4. `WorkflowService` 后续统一把代码生成、测试修复切到 Claude Runner
5. 所有任务都在 `workspace/runs/{taskId}/` 隔离目录运行
6. 前端删除模型配置入口，改为 Claude Code 环境检查视图

该方案在保留现有 Spring Boot 编排中心的同时，把 Claude Code 的“工程执行能力”独立出来，既满足需求讨论，也为后续工程生成和测试修复提供统一执行基座。
