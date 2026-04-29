# 设计系统 MASTER：简单个人博客

> Global Source of Truth  
> 适用范围：首页、文章列表页、文章详情页、后续关于我/标签/归档扩展  
> 技术约束：静态 HTML/CSS/少量 JavaScript，不创建 Vue/React 工程

---

## 1. Design System Summary

### 1.1 产品类型

- 类型：个人博客 / 内容展示网站
- 运行方式：静态展示
- 数据方式：静态文章数据
- 用户状态：无需登录
- 核心任务：浏览文章、阅读文章、了解博主内容方向

### 1.2 设计方向

采用 **Content-first Minimal Blog** 设计模式。

设计关键词：

- Minimalism / 极简
- Editorial / 编辑式阅读
- Warm neutral / 温暖中性色
- Calm / 安静克制
- Responsive / 响应式
- Accessible / 可访问

### 1.3 设计反模式

避免以下做法：

- 使用强烈渐变背景覆盖全文阅读区域。
- 使用复杂动画干扰阅读。
- 大量使用低对比浅灰正文。
- 使用 emoji 作为结构性图标。
- 文章详情正文铺满大屏宽度。
- 导航在不同页面位置变化。
- 将静态博客设计成需要登录或后台系统的产品。

---

## 2. Design Tokens

### 2.1 Color Tokens

```css
:root {
  --color-bg: #FAF7F0;
  --color-surface: #FFFFFF;
  --color-surface-soft: #F5EFE3;
  --color-text: #1F2933;
  --color-text-muted: #697586;
  --color-border: #E7DDCE;
  --color-primary: #B7791F;
  --color-primary-dark: #8A5A14;
  --color-accent: #2F6F73;
  --color-focus: #2563EB;
  --color-danger: #B42318;
}
```

#### 颜色语义

| Token | 语义 | 使用场景 |
|---|---|---|
| `color-bg` | 页面底色 | body 背景 |
| `color-surface` | 主表面 | 卡片、文章内容容器 |
| `color-surface-soft` | 柔和表面 | 标签、Hero 背景点缀 |
| `color-text` | 主文本 | 标题、正文 |
| `color-text-muted` | 辅助文本 | 日期、摘要、页脚 |
| `color-border` | 边界 | 卡片边框、分割线 |
| `color-primary` | 品牌主色 | CTA、链接、激活导航 |
| `color-accent` | 次级强调 | 标签、统计信息 |
| `color-focus` | 可访问焦点 | focus ring |

### 2.2 Typography Tokens

```css
:root {
  --font-sans: Inter, "Noto Sans SC", "PingFang SC", "Microsoft YaHei", system-ui, sans-serif;
  --font-serif: Georgia, "Times New Roman", "Noto Serif SC", serif;

  --text-xs: 0.75rem;
  --text-sm: 0.875rem;
  --text-base: 1rem;
  --text-lg: 1.125rem;
  --text-xl: 1.375rem;
  --text-2xl: 1.75rem;
  --text-3xl: clamp(2rem, 5vw, 3.5rem);

  --leading-tight: 1.2;
  --leading-normal: 1.6;
  --leading-readable: 1.85;
}
```

#### 字体规则

- UI 与导航使用 `--font-sans`。
- 文章正文默认使用 `--font-sans`，如需更强文学感可在后续使用 `--font-serif` 作为正文变体。
- 正文字号不小于 16px，文章详情正文建议 17px。

### 2.3 Spacing Tokens

```css
:root {
  --space-1: 0.25rem;
  --space-2: 0.5rem;
  --space-3: 0.75rem;
  --space-4: 1rem;
  --space-5: 1.25rem;
  --space-6: 1.5rem;
  --space-8: 2rem;
  --space-10: 2.5rem;
  --space-12: 3rem;
  --space-16: 4rem;
}
```

### 2.4 Radius Tokens

```css
:root {
  --radius-sm: 0.5rem;
  --radius-md: 0.875rem;
  --radius-lg: 1.25rem;
  --radius-xl: 1.5rem;
  --radius-pill: 999px;
}
```

### 2.5 Shadow Tokens

```css
:root {
  --shadow-sm: 0 1px 2px rgba(31, 41, 51, 0.06);
  --shadow-md: 0 10px 30px rgba(31, 41, 51, 0.08);
  --shadow-lg: 0 18px 50px rgba(31, 41, 51, 0.12);
}
```

### 2.6 Motion Tokens

```css
:root {
  --motion-fast: 120ms;
  --motion-normal: 180ms;
  --motion-slow: 240ms;
  --ease-standard: cubic-bezier(.2,.8,.2,1);
}
```

---

## 3. Layout System

### 3.1 Containers

| Class | Max Width | 用途 |
|---|---:|---|
| `.container` | 1120px | 通用页面容器 |
| `.content-container` | 760px | 文章详情正文 |
| `.narrow-container` | 640px | 空状态、说明文本 |

### 3.2 Grid

首页精选文章：

- 手机：1 列
- 平板：2 列
- 桌面：3 列或 2 列，视内容长度而定

文章列表：

- 推荐纵向列表，便于扫描标题和摘要。
- 不使用瀑布流。

### 3.3 Breakpoints

```css
/* mobile first */
@media (min-width: 640px) { /* tablet */ }
@media (min-width: 1024px) { /* desktop */ }
@media (min-width: 1280px) { /* wide desktop */ }
```

---

## 4. Component Specifications

### 4.1 Header / Navigation

#### Anatomy

- Brand：博客名称
- Nav Links：首页、文章列表
- Active Indicator：当前页胶囊背景或下划线

#### Rules

- Header 高度建议 72px。
- 移动端可换行但不隐藏核心导航。
- 导航触达区域高度 ≥44px。
- 当前页必须视觉高亮。

### 4.2 Button

#### Primary Button

```css
.btn-primary {
  min-height: 44px;
  padding: 0 20px;
  border-radius: var(--radius-pill);
  background: var(--color-primary);
  color: #fff;
}
```

#### Secondary Button

```css
.btn-secondary {
  min-height: 44px;
  padding: 0 18px;
  border-radius: var(--radius-pill);
  border: 1px solid var(--color-border);
  background: var(--color-surface);
  color: var(--color-text);
}
```

### 4.3 Post Card

#### Anatomy

1. Date / Meta
2. Title
3. Summary
4. Tags
5. Read More Link

#### Rules

- 整卡可聚焦或至少标题/阅读链接可聚焦。
- 卡片悬停仅轻微上移，不造成布局跳动。
- 摘要最多展示 2-3 行。

### 4.4 Tag

```css
.tag {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 10px;
  border-radius: var(--radius-pill);
  background: var(--color-surface-soft);
  color: var(--color-accent);
  font-size: var(--text-xs);
  font-weight: 700;
}
```

### 4.5 Article Content

#### Text Rules

- `h1`：文章标题，仅出现一次。
- `h2`：正文一级小标题。
- `p`：行高 `var(--leading-readable)`。
- `blockquote`：左边框 + 柔和背景。
- `ul/ol`：保留缩进与段间距。

---

## 5. Accessibility Requirements

### 5.1 必须满足

- HTML 根元素：`<html lang="zh-CN">`
- 页面包含 skip link。
- 所有链接和按钮具备可见 focus 样式。
- 普通文本对比度 ≥4.5:1。
- 大文本对比度 ≥3:1。
- 交互目标 ≥44×44px。
- 不禁用浏览器缩放。
- 使用语义标签：`header/nav/main/article/footer`。

### 5.2 Focus Ring

```css
:focus-visible {
  outline: 3px solid var(--color-focus);
  outline-offset: 3px;
}
```

### 5.3 Reduced Motion

```css
@media (prefers-reduced-motion: reduce) {
  *, *::before, *::after {
    animation-duration: 0.01ms !important;
    animation-iteration-count: 1 !important;
    scroll-behavior: auto !important;
    transition-duration: 0.01ms !important;
  }
}
```

---

## 6. Page Rules

### 6.1 首页 Home

目标：建立个人博客的第一印象，引导阅读。

模块顺序：

1. Header
2. Hero
3. 精选文章
4. 最新文章
5. Footer

设计注意：

- Hero 不要占用过多首屏高度，保证文章入口可见。
- CTA 文案建议“查看全部文章”。
- 可展示“静态博客 / 无需登录 / 内容持续更新”等轻量说明。

### 6.2 文章列表 Posts

目标：让用户快速浏览文章。

模块顺序：

1. Header
2. Page Intro
3. Post List
4. Footer

设计注意：

- 按日期倒序展示。
- 卡片间距 ≥16px。
- 标题可点击，阅读全文链接也可点击。

### 6.3 文章详情 Post Detail

目标：提供专注阅读体验。

模块顺序：

1. Header
2. Back Link
3. Article Header
4. Article Body
5. Related / Back CTA
6. Footer

设计注意：

- 正文宽度控制在 760px 内。
- 避免右侧复杂侧栏。
- 返回入口放在文章标题上方和正文末尾均可。

---

## 7. Static Prototype Rules

### 7.1 路由方式

原型采用 hash 路由：

| 路径 | 页面 |
|---|---|
| `#/` | 首页 |
| `#/posts` | 文章列表 |
| `#/post/hello-blog` | 文章详情 |

### 7.2 数据方式

文章数据以内嵌 JavaScript 数组维护：

```js
const posts = [
  {
    id: 'hello-blog',
    title: '我的第一篇博客',
    date: '2026-04-27',
    tags: ['随笔', '个人博客'],
    summary: '这是我的第一篇个人博客文章，用来记录博客的开始。',
    content: '...'
  }
]
```

### 7.3 禁止项

- 不请求后端 API。
- 不使用 localStorage 模拟登录。
- 不引入路由库或构建工具。
- 不创建框架工程。

---

## 8. QA Checklist

- [ ] 页面在浏览器直接打开可用。
- [ ] 首页可跳转文章列表。
- [ ] 首页精选文章可跳转详情。
- [ ] 列表页每篇文章可跳转详情。
- [ ] 详情页返回入口可用。
- [ ] 不存在文章时有错误状态。
- [ ] 手机宽度 375px 无横向滚动。
- [ ] 所有按钮/链接键盘可访问。
- [ ] 焦点样式可见。
- [ ] 正文阅读体验舒适。
