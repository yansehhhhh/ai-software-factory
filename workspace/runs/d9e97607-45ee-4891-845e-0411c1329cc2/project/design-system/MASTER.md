# Simple Todo 设计系统 MASTER

## 1. 设计系统定位

本设计系统服务于“简单待办事项应用”的第一版 MVP。目标是在静态 HTML 原型、后续前端实现和自动化测试中保持一致的视觉语言、组件行为和可访问性标准。

设计关键词：

- 简洁
- 专注
- 温和
- 高可读性
- 移动优先
- 可访问

## 2. Design Principles 设计原则

### 2.1 内容优先

任务内容是页面核心。视觉装饰不得抢夺任务文本、输入框和状态控件的注意力。

### 2.2 即开即用

用户无需学习即可理解：输入任务、点击添加、勾选完成、删除任务。

### 2.3 状态清晰

每个任务必须明确表达：未完成、已完成、可删除。状态表达不能只依赖颜色。

### 2.4 可恢复感

虽然 MVP 不强制撤销删除，但界面应通过轻提示让用户理解操作已发生，避免“点击后无反馈”。

### 2.5 无障碍默认开启

所有组件默认具备键盘焦点、语义标签、可读对比度和足够触控区域。

## 3. Design Tokens

### 3.1 Color Tokens

```css
:root {
  --color-bg: #F6F8FC;
  --color-bg-gradient-start: #EFF6FF;
  --color-bg-gradient-end: #F8FAFC;

  --color-surface: #FFFFFF;
  --color-surface-muted: #EEF4FF;
  --color-surface-subtle: #F8FAFC;

  --color-primary: #2563EB;
  --color-primary-hover: #1D4ED8;
  --color-primary-active: #1E40AF;
  --color-primary-soft: #DBEAFE;

  --color-success: #16A34A;
  --color-success-hover: #15803D;
  --color-success-soft: #DCFCE7;

  --color-danger: #DC2626;
  --color-danger-hover: #B91C1C;
  --color-danger-soft: #FEE2E2;

  --color-warning: #D97706;
  --color-warning-soft: #FEF3C7;

  --color-text: #0F172A;
  --color-text-muted: #64748B;
  --color-text-subtle: #94A3B8;
  --color-text-inverse: #FFFFFF;

  --color-border: #E2E8F0;
  --color-border-strong: #CBD5E1;
  --color-focus: #93C5FD;
}
```

### 3.2 Typography Tokens

```css
:root {
  --font-sans: Inter, ui-sans-serif, system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;

  --text-xs: 12px;
  --text-sm: 14px;
  --text-base: 16px;
  --text-lg: 18px;
  --text-xl: 20px;
  --text-2xl: 24px;
  --text-3xl: 32px;

  --leading-xs: 16px;
  --leading-sm: 20px;
  --leading-base: 24px;
  --leading-lg: 28px;
  --leading-xl: 30px;
  --leading-2xl: 32px;
  --leading-3xl: 40px;

  --font-normal: 400;
  --font-medium: 500;
  --font-semibold: 650;
  --font-bold: 700;
}
```

### 3.3 Spacing Tokens

```css
:root {
  --space-1: 4px;
  --space-2: 8px;
  --space-3: 12px;
  --space-4: 16px;
  --space-5: 20px;
  --space-6: 24px;
  --space-8: 32px;
  --space-10: 40px;
  --space-12: 48px;
  --space-16: 64px;
}
```

### 3.4 Radius Tokens

```css
:root {
  --radius-sm: 8px;
  --radius-md: 12px;
  --radius-lg: 16px;
  --radius-xl: 24px;
  --radius-full: 999px;
}
```

### 3.5 Shadow Tokens

```css
:root {
  --shadow-sm: 0 2px 8px rgba(15, 23, 42, 0.06);
  --shadow-md: 0 8px 24px rgba(15, 23, 42, 0.08);
  --shadow-lg: 0 20px 60px rgba(15, 23, 42, 0.10);
}
```

### 3.6 Motion Tokens

```css
:root {
  --motion-fast: 120ms;
  --motion-base: 180ms;
  --motion-slow: 260ms;
  --ease-standard: cubic-bezier(0.2, 0, 0, 1);
}
```

## 4. Component System

### 4.1 App Shell

用途：承载整个待办页面。

规范：

- 背景使用柔和浅色渐变。
- 主内容最大宽度 `720px`。
- 移动端边距 `16px`。
- 桌面端边距 `24px`。
- 使用 `main` 语义标签。

### 4.2 Header

组成：

- 产品标签：`个人效率工具`
- 页面标题：`今天要完成什么？`
- 简短说明：`记录任务，逐项完成，保持专注。`

规范：

- 标题为页面唯一 `h1`。
- 文案简洁，不超过两行。
- 标题和说明左对齐。

### 4.3 Summary Cards

组成：

- 全部
- 未完成
- 已完成

规范：

- 使用三列网格。
- 每个卡片显示数字和标签。
- 数字为视觉重点。
- 移动端保持可读，不得拥挤。

### 4.4 Add Todo Form

组成：

- 可见 label
- 文本输入框
- 添加按钮
- 帮助文案 / 错误文案

行为：

- 点击添加或按 Enter 提交。
- 输入会 trim。
- 空值和超长值阻止提交。
- 错误显示在输入框下方。

可访问性：

- `label for="todo-input"`
- 错误提示使用 `role="alert"`
- 输入框通过 `aria-describedby` 关联帮助和错误信息。

### 4.5 Todo Item

组成：

- 复选框
- 任务标题
- 状态辅助文本
- 删除按钮

状态：

- 默认：白底、边框。
- hover：轻阴影、边框加深。
- completed：标题删除线、颜色变浅、状态文案显示“已完成”。
- focus-within：显示焦点环或边框强调。

### 4.6 Empty State

展示条件：无任务。

组成：

- SVG 线性图形
- 标题：暂无待办
- 描述：添加你的第一个任务，开始保持专注。

### 4.7 Toast

用途：展示删除成功、本地存储不可用等非阻断反馈。

规范：

- 位置：主卡片下方或右下角，原型中使用主卡片内底部区域。
- 自动消失：3 秒。
- `aria-live="polite"`。
- 不抢焦点。

## 5. Interaction States

### 5.1 Button States

| 状态 | 表现 |
|---|---|
| Default | 主色背景，白色文字 |
| Hover | 背景加深 |
| Active | 背景进一步加深，轻微下压感 |
| Focus | 3px 蓝色焦点环 |
| Disabled | 透明度 0.55，禁用点击 |

### 5.2 Input States

| 状态 | 表现 |
|---|---|
| Default | 白底、浅边框 |
| Hover | 边框稍加深 |
| Focus | 主色边框 + 焦点环 |
| Error | 危险色边框 + 错误文案 |
| Disabled | 浅灰背景，禁用输入 |

### 5.3 Todo States

| 状态 | 表现 |
|---|---|
| Active | 标题主文本色，复选框未选中 |
| Completed | 复选框选中，标题删除线，状态文案“已完成” |
| Hover | 卡片阴影增强 |
| Focus | 焦点环可见 |

## 6. Responsive Rules

```css
@media (max-width: 520px) {
  .todo-form-row {
    flex-direction: column;
  }

  .button-primary {
    width: 100%;
  }
}
```

规则：

- `375px` 下必须完整可用。
- 任务标题允许自然换行。
- 删除按钮保持 `44px` 触控面积。
- 页面不出现横向滚动。

## 7. Accessibility Requirements

必须满足：

- 普通文本对比度 ≥ 4.5:1。
- 大文本对比度 ≥ 3:1。
- 可交互控件触控面积 ≥ 44px。
- 所有交互控件有键盘焦点样式。
- 图标按钮必须有 aria-label。
- 表单字段必须有可见 label。
- 错误信息必须可被读屏感知。
- 不移除浏览器缩放能力。

## 8. Content Guidelines

### 8.1 语气

- 简洁
- 友好
- 行动导向
- 不使用技术术语

### 8.2 推荐文案

| 场景 | 文案 |
|---|---|
| 页面标题 | 今天要完成什么？ |
| 页面说明 | 记录任务，逐项完成，保持专注。 |
| 输入 label | 新增待办 |
| 输入 placeholder | 例如：购买牛奶 |
| 添加按钮 | 添加任务 |
| 空状态标题 | 暂无待办 |
| 空状态说明 | 添加你的第一个任务，开始保持专注。 |
| 空输入错误 | 请输入待办内容。 |
| 超长错误 | 任务内容不能超过 100 个字符。 |
| 删除反馈 | 任务已删除。 |

## 9. Anti-patterns 禁止项

- 禁止使用 emoji 作为结构图标。
- 禁止只有 placeholder 而没有 label。
- 禁止移除 focus outline。
- 禁止只用颜色表示完成状态。
- 禁止低对比度灰色正文。
- 禁止移动端横向滚动。
- 禁止小于 44px 的点击区域。
- 禁止复杂多页面导航进入 MVP。
- 禁止在 MVP 中加入登录、协作、日历、标签等超范围能力。

## 10. Implementation Notes for Static HTML Prototype

静态原型应：

- 只使用 HTML、CSS、少量原生 JavaScript。
- 可直接通过浏览器打开 `prototype/index.html`。
- 不依赖 Vue、React、构建工具或后端服务。
- 使用 localStorage 模拟本地持久化。
- 包含示例任务、空状态、校验、完成、删除交互。

## 11. Quality Checklist

- [ ] 页面唯一 `h1`。
- [ ] 输入框有 label。
- [ ] 错误提示在字段附近。
- [ ] 所有按钮支持键盘访问。
- [ ] 所有交互目标 ≥ 44px。
- [ ] 375px 宽度无横向滚动。
- [ ] 完成状态有复选框、删除线和文字说明。
- [ ] 删除后有反馈。
- [ ] 刷新后任务保留。
- [ ] `prefers-reduced-motion` 下动效降低。
