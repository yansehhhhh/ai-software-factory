# 设计系统 MASTER：慢慢写生活

> Global Source of Truth  
> 适用范围：静态个人博客 HTML 原型与后续前端实现  
> 风格定位：温暖文艺、生活记录、阅读优先

---

## 1. 设计系统原则

### 1.1 内容优先

所有视觉设计应服务文字内容。页面不使用复杂装饰和高干扰动画，避免喧宾夺主。

### 1.2 温暖克制

通过暖米白背景、陶土棕主色、柔和卡片和自然留白建立亲近感，不使用高饱和撞色。

### 1.3 静态可维护

设计系统适配无登录、无数据库、内置示例文章的静态博客。组件应简单、语义清晰，方便开发者后续手动修改文章内容。

### 1.4 可访问性底线

- 正文对比度不低于 4.5:1。
- 交互元素触达区域不低于 44px。
- 键盘焦点可见。
- 不依赖颜色单独表达状态。

---

## 2. Design Tokens

### 2.1 Colors

```css
:root {
  --color-bg: #FBF5EC;
  --color-bg-gradient-start: #FBF5EC;
  --color-bg-gradient-end: #F3E1CF;
  --color-surface: #FFFDF8;
  --color-surface-soft: #F7EADB;
  --color-surface-muted: #F2DFCB;

  --color-primary: #A55F3F;
  --color-primary-dark: #7E432D;
  --color-primary-soft: #E7C4AA;

  --color-secondary: #D9A875;
  --color-accent: #7E8F68;
  --color-accent-soft: #E4E8D9;

  --color-text: #3F312A;
  --color-text-strong: #2F241F;
  --color-muted: #7D6E64;
  --color-subtle: #9B8778;

  --color-border: #E9D8C7;
  --color-border-strong: #D8BFA9;
  --color-focus: #6E7F53;
  --color-white: #FFFFFF;
}
```

### 2.2 Typography

```css
:root {
  --font-sans: -apple-system, BlinkMacSystemFont, "PingFang SC", "Microsoft YaHei", "Helvetica Neue", Arial, sans-serif;

  --text-xs: 12px;
  --text-sm: 14px;
  --text-base: 16px;
  --text-md: 17px;
  --text-lg: 20px;
  --text-xl: 24px;
  --text-2xl: 30px;
  --text-3xl: 42px;
  --text-display: 56px;

  --leading-tight: 1.15;
  --leading-title: 1.25;
  --leading-normal: 1.6;
  --leading-relaxed: 1.85;
}
```

### 2.3 Spacing

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
  --space-20: 80px;
}
```

### 2.4 Radius

```css
:root {
  --radius-sm: 10px;
  --radius-md: 16px;
  --radius-lg: 24px;
  --radius-xl: 32px;
  --radius-pill: 999px;
}
```

### 2.5 Shadows

```css
:root {
  --shadow-sm: 0 6px 18px rgba(84, 55, 35, 0.07);
  --shadow-md: 0 14px 34px rgba(84, 55, 35, 0.10);
  --shadow-lg: 0 24px 60px rgba(84, 55, 35, 0.14);
}
```

### 2.6 Motion

```css
:root {
  --motion-fast: 150ms ease;
  --motion-normal: 220ms ease;
  --motion-slow: 320ms ease;
}
```

---

## 3. Component System

### 3.1 App Shell

用途：页面整体结构。

组成：

- Sticky Header。
- Main Content。
- Footer。

规则：

- Header 固定在页面顶部时必须保持背景可读。
- 主内容与 Header 之间预留足够空间。
- Footer 仅放轻量版权或说明，不放复杂链接组。

### 3.2 Header / Navigation

结构：

- 左侧：博客名称。
- 右侧：首页、文章。

状态：

- Active：浅色圆角背景 + 主色文字。
- Hover：浅杏色背景。
- Focus：2px 焦点环。

限制：

- 导航项不超过 3 个。
- 不使用图标作为主要导航，保持文字清晰。

### 3.3 Hero

用途：首页首屏表达博客定位。

内容：

- Eyebrow 文案，如“生活记录 / 阅读随笔 / 日常片段”。
- 主标题，如“慢慢写生活”。
- 简介文案。
- 主 CTA：查看所有文章。
- 次入口：阅读精选。

视觉：

- 可使用柔和渐变背景。
- 可使用抽象纸张/便签式装饰卡片。
- 不使用真人照片或复杂插画作为强依赖资源。

### 3.4 Post Card

用途：展示文章摘要。

字段：

- `title`
- `date`
- `category`
- `excerpt`
- `featured`

布局规则：

- 首页精选：桌面端 3 列，移动端 1 列。
- 文章列表：桌面端 2 列，移动端 1 列。
- 标题最多自然换行 2 行。
- 摘要最多 2-3 行。

交互状态：

- Hover：轻微上浮，阴影增强。
- Focus：外轮廓清晰。
- Active：轻微缩放。

### 3.5 Tag

用途：分类标签。

样式：

- 胶囊形。
- 背景低饱和。
- 文本 13-14px。

### 3.6 Article Detail

用途：长文阅读。

结构：

- 返回链接。
- 标题。
- Meta 信息。
- 正文。
- 底部返回按钮。

规则：

- 正文最大宽度 760px。
- 段落行高 1.85。
- 背景不使用纯白大块，采用暖白 surface。
- 段落之间保持 20px 左右间距。

### 3.7 Empty / Not Found

用途：文章不存在或空状态。

文案：

- 文章不存在：`没有找到这篇文章，也许它还在路上。`
- 无文章：`还没有文章。等风来，也等第一篇文字。`

操作：

- 返回首页。
- 返回所有文章。

---

## 4. Page Patterns

### 4.1 Home Page

页面目标：建立博客气质，引导阅读精选文章。

模块顺序：

1. Header。
2. Hero。
3. Featured Posts。
4. Quiet Footer。

必须包含：

- 博客标题。
- 博客简介。
- 精选文章。

### 4.2 Posts Page

页面目标：展示所有文章，帮助用户选择内容。

模块顺序：

1. Header。
2. Page Intro。
3. Posts Grid。

必须包含：

- 页面标题。
- 全部文章卡片。
- 每篇文章的标题、日期、分类、摘要、入口。

### 4.3 Post Detail Page

页面目标：提供沉浸且舒适的阅读体验。

模块顺序：

1. Header。
2. Article Header。
3. Article Body。
4. Back CTA。

必须包含：

- 文章标题。
- 日期。
- 分类。
- 完整正文。
- 返回入口。

---

## 5. Responsive Rules

### 5.1 Mobile `< 640px`

- 页面左右边距 20px。
- Hero 标题约 38px。
- 文章卡片单列。
- Header 导航保持可点击区域不小于 44px。
- 文章正文 16px 起，行高不低于 1.75。

### 5.2 Tablet `640px - 1023px`

- 页面左右边距 28px。
- 首页精选可 2 列或单列。
- 文章详情仍保持 760px 内阅读宽度。

### 5.3 Desktop `>= 1024px`

- 页面容器最大 1120px。
- 首页精选 3 列。
- 文章列表 2 列。
- Hero 可左右分栏。

---

## 6. Accessibility Rules

必须遵守：

- 使用语义化 HTML。
- 所有链接和按钮有明确文本。
- `:focus-visible` 必须有明显样式。
- 不禁用浏览器缩放。
- 颜色对比度满足 WCAG AA。
- 使用 `aria-live` 更新文章详情状态时提示屏幕阅读器。
- 动画遵守 `prefers-reduced-motion`。

---

## 7. Anti-Patterns

避免以下做法：

- 为静态博客引入登录、数据库或后台入口。
- 使用大面积高饱和色或强对比黑白。
- 用 hover 才显示“阅读全文”等关键入口。
- 正文全屏铺满导致行宽过长。
- 移动端出现横向滚动。
- 使用 emoji 作为结构性图标。
- 使用复杂滚动视差影响阅读。
- 将文章数据散落在多个脚本位置。

---

## 8. Implementation Notes for Static HTML

- 文章数据集中在 `posts` 数组中。
- 使用 hash 或 query 参数模拟详情切换均可。
- 原型应能直接双击 `prototype/index.html` 在浏览器打开。
- 不依赖构建工具。
- 不依赖外部 CDN，保证离线可打开。
- CSS、HTML、JS 可内联在单文件中，便于交付查看。

---

## 9. Final QA Checklist

- [ ] 首页展示博客标题、简介、精选文章。
- [ ] 文章列表展示全部内置文章。
- [ ] 文章详情可由卡片点击进入。
- [ ] 不存在文章 ID 时展示友好提示。
- [ ] 所有交互元素支持键盘访问。
- [ ] 移动端 375px 宽度无横向滚动。
- [ ] `prefers-reduced-motion` 下动画禁用。
- [ ] 无登录、无数据库、无后端依赖。
