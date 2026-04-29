# UI 原型、阶段产物展示与需求讨论选项设计

## 背景

当前 AI 软件工厂主链路已经通过 `claude-runner` 调用 Claude CLI，PRD 阶段已接入 `.claude/skills/pm-product-pack`。但 UI 阶段仍主要输出 Markdown 规范，前端结果区域只展示最终项目/报告/zip 操作，没有需求阶段和 UI 阶段产物展示/下载区。需求讨论也只有 AI 提问 + 用户自由输入，缺少 AI 给出的可选答案。

## 目标

1. 引入 `nextlevelbuilder/ui-ux-pro-max-skill` 中适合本项目的 Claude CLI skill，让 UI 阶段使用 `/ui-ux-pro-max` 生成设计系统与静态 HTML 原型。
2. 第一版 UI 原型形态为静态 HTML：`project/prototype/index.html`。
3. 为后续截图预览打基础：UI 阶段产物结构稳定，后续可由 Playwright 打开 `prototype/index.html` 截图。
4. Web 页面增加需求阶段产物、UI 阶段产物展示/下载区域。
5. 需求讨论支持：AI 提问后展示 2-4 个选项，用户可点选；如果没有合适选项，仍可在输入框自由输入。

## 非目标

- 本次不生成 Figma 文件。
- 本次不做真正 AI 图片生成。
- 本次不生成完整 Vue 原型项目。
- 本次不要求完成 Playwright 截图落地，只为截图阶段保留稳定 `prototype/index.html` 入口和 artifacts。

## 方案比较

### 方案 A：静态 HTML 原型（推荐）

UI 阶段调用 `/ui-ux-pro-max`，生成：

- `ui-guidelines.md`
- `design-system/MASTER.md`
- `prototype/index.html`
- `ui-prototype.md`

优点：依赖少、速度快、容易截图、不会和最终代码生成阶段冲突。

缺点：与最终 Vue 代码存在一定距离。

### 方案 B：Vue 原型

UI 阶段生成 Vue 页面和组件。

优点：更接近最终实现。

缺点：构建复杂，需要依赖安装/路由/样式体系，容易和后续代码生成阶段职责重叠。

### 方案 C：只输出设计系统和原型说明

优点：实现最简单。

缺点：仍然没有真实可预览原型。

## 推荐方案

采用方案 A：静态 HTML 原型。

## 设计细节

### 1. 引入 UI/UX Claude CLI skill

从 `nextlevelbuilder/ui-ux-pro-max-skill` 引入以下最小 skill：

- `.claude/skills/ui-ux-pro-max/`

第一版只引入核心 `ui-ux-pro-max`，不一次性引入 `brand`、`slides`、`banner-design` 等非主链路 skill。

如核心 skill 依赖脚本或数据，则同时保留其 `scripts/`、`data/` 或说明中引用的必要文件。若上游路径假设为 `skills/ui-ux-pro-max/scripts/search.py`，需要在本项目中保持可被 skill prompt 引用的相对路径，或在本项目 skill 文档中改成 `.claude/skills/ui-ux-pro-max/scripts/search.py`。

### 2. 修改 Claude Runner UI mode

修改 `apps/claude-runner/src/claudeRunner.js`：

- `ui` mode 的 prompt 前置 `/ui-ux-pro-max`。
- 明确本次为自动化主链路，不等待用户确认。
- 要求输出并/或写入：
  - `project/ui-guidelines.md`
  - `project/design-system/MASTER.md`
  - `project/prototype/index.html`
  - `project/ui-prototype.md`
- `writeModeArtifacts('ui')` 返回上述 artifact 路径。
- UI mode 允许 `Read`、`Write`、`Edit`；第一版静态 HTML 不需要 `Bash`。

### 3. 阶段产物模型

当前 `ClaudeRunResult.artifacts` 已有 artifact 路径，但 `WorkflowService` 没有暴露到 `ResultView`。

新增 DTO：

```java
public record ArtifactView(
    String stage,
    String name,
    String type,
    String path,
    boolean downloadable
) {}
```

扩展 `ResultView`：

```java
List<ArtifactView> artifacts
```

阶段建议：

- `requirement`：PM 产品产物包、PRD
- `ui`：UI 规范、设计系统、HTML 原型
- `code`：项目目录、代码包
- `test`：测试报告

### 4. Web 结果展示区设计

改造 `DesignResult.vue` 为“阶段产物面板”，保留最终操作按钮。

建议结构：

```text
结果与产物
├── 需求阶段产物
│   ├── PM 产品产物包 Markdown
│   └── PRD Markdown
├── UI 阶段产物
│   ├── UI 设计规范
│   ├── Design System
│   └── 静态 HTML 原型
├── 代码与测试
│   ├── 打开生成项目
│   ├── 查看测试报告
│   └── 下载代码包
└── 预览区
    ├── PRD/UI Markdown 摘要
    └── 原型入口提示
```

第一版可以只提供路径/打开按钮，不做复杂 markdown 渲染器。已有 `result.prdMarkdown` 可继续显示为摘要。

### 5. 需求讨论选项设计

扩展 `DiscussionMessage`：

```java
public record DiscussionMessage(
    String role,
    String content,
    List<String> options
)
```

保留静态构造方法兼容旧消息：

- `ai(String content)` → options 空列表
- `user(String content)` → options 空列表
- 新增 `ai(String content, List<String> options)`

更新讨论 prompt，要求 AI 输出一段 JSON 或稳定格式，例如：

```json
{
  "question": "你希望优先生成哪种应用？",
  "options": ["管理后台", "SaaS 落地页", "数据看板", "移动端 App"],
  "complete": false
}
```

后端解析后存入 `DiscussionMessage.content` 和 `DiscussionMessage.options`。

如果解析失败，降级为原文本，无选项。

### 6. 讨论 UI 交互

修改 `DiscussionPanel.vue`：

- 对最新 AI 消息，如果 `options.length > 0`，在消息下方显示 option chips。
- 点击 option 后：
  - 将输入框填入该选项，或直接发送该选项。
- 保留 textarea，自由输入仍然可用。

推荐第一版：点击选项直接发送，减少一步操作。

### 7. 下载/打开策略

当前后端返回的是本地文件路径，浏览器直接打开可能不可用。第一版仍可展示路径并复用现有 `openLink` 行为；后续应补稳定下载 API：

```text
GET /api/artifacts?path=...
```

但为了控制范围，本次先以 artifact list 展示为主。

## 自查

- UI 原型第一版明确为静态 HTML，不引入 Vue 原型复杂度。
- `ui-ux-pro-max` 用于主链路 UI 阶段，不走 `packages/skills`。
- 阶段产物展示复用现有 `ResultView`，只补 `artifacts` 列表。
- 讨论选项保持向后兼容：无 options 时仍是普通聊天。
- 下载 API 作为后续，不阻塞第一版展示。
