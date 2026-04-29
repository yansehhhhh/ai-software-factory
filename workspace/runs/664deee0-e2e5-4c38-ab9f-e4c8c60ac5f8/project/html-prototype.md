# 博客网站 — HTML 原型结构说明

## 文档信息

| 项目 | 内容 |
|------|------|
| 文档类型 | HTML 原型结构说明 |
| 产品名称 | 个人博客网站 |
| 版本 | v1.0 |
| 创建日期 | 2026-04-27 |
| 技术栈 | Vue 3 + VitePress |

---

## 目录

1. [HTML 设计原则](#1-html-设计原则)
2. [通用布局模板](#2-通用布局模板)
3. [页面 HTML 结构](#3-页面-html-结构)
4. [组件 HTML 结构](#4-组件-html-结构)
5. [CSS 类命名规范](#5-css-类命名规范)
6. [Vue 组件映射](#6-vue-组件映射)
7. [静态资源引用](#7-静态资源引用)

---

## 1. HTML 设计原则

### 1.1 语义化

- 使用 HTML5 语义标签：`<header>`, `<nav>`, `<main>`, `<article>`, `<footer>`, `<section>`, `<aside>`
- 文章详情使用 `<article>` 包裹
- 导航使用 `<nav>` 包裹
- 时间使用 `<time datetime="">` 标签

### 1.2 可访问性

- 所有图片提供 `alt` 属性
- 导航区域使用 `aria-label` 标注
- 主要区域使用 `role` 或语义标签
- 跳转链接使用 `<a>` 标签，支持键盘聚焦

### 1.3 渐进增强

- 核心内容在禁用 JavaScript 下仍可阅读（VitePress SSG 保证）
- 交互增强（如进度条、动态筛选）在 JS 可用时激活

---

## 2. 通用布局模板

### 2.1 全局布局骨架

```html
<!DOCTYPE html>
<html lang="zh-CN">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title><!-- 页面标题 | 博客名 --></title>
  <meta name="description" content="<!-- 页面描述 -->">
  <!-- VitePress 自动注入 CSS/JS -->
</head>
<body>
  <div id="app">
    <!-- 全局布局组件 -->
    <div class="layout">

      <!-- 顶部导航栏 -->
      <header class="site-header" role="banner">
        <nav class="site-nav" aria-label="主导航">
          <div class="nav-inner">
            <!-- 见 4.1 SiteNav -->
          </div>
        </nav>
      </header>

      <!-- 主内容区 -->
      <main class="site-main" role="main">
        <div class="content-container">
          <!-- 页面特有内容 -->
          <!-- 见各页面结构 -->
        </div>
      </main>

      <!-- 底部页脚 -->
      <footer class="site-footer" role="contentinfo">
        <div class="footer-inner">
          <!-- 见 4.2 SiteFooter -->
        </div>
      </footer>

    </div>
  </div>
</body>
</html>
```

### 2.2 CSS 容器规范

```css
/* 全局容器 */
.content-container {
  max-width: 960px;
  margin: 0 auto;
  padding: 0 max(16px, calc((100vw - 960px) / 2));
}

/* 文章内容区（较窄，便于阅读） */
.article-container {
  max-width: 720px;
  margin: 0 auto;
}

/* 卡片列表容器 */
.card-list {
  display: flex;
  flex-direction: column;
  gap: 24px;
}
```

---

## 3. 页面 HTML 结构

### 3.1 首页 `/` (index.md)

```html
<main class="site-main">
  <div class="content-container">

    <!-- 作者简介区 -->
    <section class="author-bio" aria-label="作者简介">
      <div class="author-bio__inner">
        <img
          class="author-bio__avatar"
          src="/avatar.png"
          alt="张三的头像"
          width="120"
          height="120"
          loading="eager"
        >
        <h1 class="author-bio__name">张三</h1>
        <p class="author-bio__summary">前端开发 · 技术写作者</p>
        <div class="author-bio__links">
          <a href="https://github.com/zhangsan"
             target="_blank"
             rel="noopener noreferrer"
             aria-label="GitHub">
            <!-- GitHub 图标 SVG -->
          </a>
          <a href="mailto:zhangsan@example.com"
             aria-label="Email">
            <!-- Email 图标 SVG -->
          </a>
        </div>
      </div>
    </section>

    <!-- 最新文章区 -->
    <section class="recent-posts" aria-label="最新文章">
      <h2 class="section-title">最新文章</h2>

      <div class="card-list">
        <!-- 循环渲染 ArticleCard -->
        <article class="article-card">
          <a class="article-card__link" href="/posts/hello-world">
            <time class="article-card__date"
                  datetime="2026-04-27">
              2026-04-27
            </time>
            <h3 class="article-card__title">
              TypeScript 高级类型技巧
            </h3>
            <p class="article-card__summary">
              本文介绍 TypeScript 中一些实用的高级类型...
            </p>
          </a>
          <div class="article-card__tags">
            <a class="tag-badge" href="/posts/?tag=TypeScript">
              TypeScript
            </a>
            <a class="tag-badge" href="/posts/?tag=前端">
              前端
            </a>
          </div>
        </article>
        <!-- 更多文章卡片... -->
      </div>

      <div class="recent-posts__more">
        <a class="btn-link" href="/posts/">
          查看全部文章 →
        </a>
      </div>
    </section>

    <!-- 标签云区 -->
    <aside class="tag-cloud-section" aria-label="标签云">
      <h2 class="section-title">标签云</h2>
      <div class="tag-cloud">
        <a class="tag-cloud__item tag-cloud__item--large"
           href="/posts/?tag=TypeScript">
          TypeScript (12)
        </a>
        <a class="tag-cloud__item tag-cloud__item--medium"
           href="/posts/?tag=Vue">
          Vue (8)
        </a>
        <!-- 更多标签... -->
      </div>
    </aside>

  </div>
</main>
```

**CSS 关键类**：

| 类名 | 用途 |
|------|------|
| `.author-bio` | 作者简介外层容器，居中，padding 48px 0 |
| `.author-bio__avatar` | 圆形头像，border-radius: 50% |
| `.author-bio__name` | 标题字体，28px，font-weight 700 |
| `.author-bio__summary` | 次级文字色，16px |
| `.recent-posts` | 最新文章外层 |
| `.section-title` | 区域标题，22px，带下划线装饰 |
| `.recent-posts__more` | "查看全部"链接，居右 |
| `.tag-cloud-section` | 标签云外层，margin 48px 0 |
| `.tag-cloud` | Flexbox 容器，flex-wrap，gap 12px |
| `.tag-cloud__item` | 标签项，padding 6px 16px，圆角 |

---

### 3.2 文章列表页 `/posts/` (posts.md)

```html
<main class="site-main">
  <div class="content-container">

    <!-- 列表页头部 -->
    <header class="list-header">
      <h1 class="list-header__title">全部文章</h1>

      <!-- 标签筛选器 -->
      <div class="tag-selector">
        <button class="tag-selector__trigger"
                aria-expanded="false"
                aria-haspopup="listbox">
          <span class="tag-selector__label">标签筛选: </span>
          <span class="tag-selector__current">全部</span>
          <span class="tag-selector__arrow">▼</span>
        </button>
        <!-- 下拉列表（默认隐藏） -->
        <ul class="tag-selector__dropdown"
            role="listbox"
            hidden>
          <li class="tag-selector__option"
              role="option"
              aria-selected="true">
            <a href="/posts/">全部</a>
          </li>
          <li class="tag-selector__option"
              role="option">
            <a href="/posts/?tag=TypeScript">
              TypeScript (12)
            </a>
          </li>
          <li class="tag-selector__option"
              role="option">
            <a href="/posts/?tag=Vue">
              Vue (8)
            </a>
          </li>
          <!-- 更多标签选项... -->
        </ul>
      </div>
    </header>

    <!-- 激活的筛选标签（仅在有筛选时显示） -->
    <div class="active-filter" hidden>
      <span>当前筛选：</span>
      <span class="tag-badge tag-badge--closable">
        Vue
        <button class="tag-badge__close"
                aria-label="清除筛选">
          ✕
        </button>
      </span>
    </div>

    <!-- 文章卡片列表 -->
    <div class="card-list">
      <article class="article-card">
        <a class="article-card__link" href="/posts/typescript-advanced">
          <time class="article-card__date"
                datetime="2026-04-27">
            2026-04-27
          </time>
          <h3 class="article-card__title">
            TypeScript 高级类型技巧
          </h3>
          <p class="article-card__summary">
            本文介绍 TypeScript 中一些实用的高级类型...
          </p>
        </a>
        <div class="article-card__tags">
          <a class="tag-badge" href="/posts/?tag=TypeScript">
            TypeScript
          </a>
          <a class="tag-badge" href="/posts/?tag=前端">
            前端
          </a>
        </div>
      </article>
      <!-- 更多文章... -->
    </div>

    <!-- 空结果状态（默认隐藏） -->
    <div class="empty-state" hidden>
      <p class="empty-state__text">该标签下暂无文章</p>
      <a class="btn-link" href="/posts/">返回全部文章</a>
    </div>

    <!-- 分页 -->
    <nav class="pagination" aria-label="文章列表分页">
      <a class="pagination__item pagination__item--prev"
         href="/posts/page/1"
         aria-disabled="true">
        ◀ 上一页
      </a>
      <span class="pagination__item pagination__item--active">
        1
      </span>
      <a class="pagination__item" href="/posts/page/2">
        2
      </a>
      <a class="pagination__item" href="/posts/page/3">
        3
      </a>
      <a class="pagination__item pagination__item--next"
         href="/posts/page/2">
        下一页 ▶
      </a>
    </nav>

  </div>
</main>
```

**CSS 关键类**：

| 类名 | 用途 |
|------|------|
| `.list-header` | 列表页头部，flex justify-between |
| `.tag-selector` | 标签筛选器容器，position relative |
| `.tag-selector__trigger` | 筛选按钮，带边框，cursor pointer |
| `.tag-selector__dropdown` | 下拉菜单，position absolute，z-index 10 |
| `.tag-selector__option` | 下拉选项，hover 高亮 |
| `.active-filter` | 激活筛选的指示条，背景淡色 |
| `.pagination` | 分页容器，flex center，gap 8px |
| `.pagination__item` | 页码按钮，40px×40px，居中 |
| `.pagination__item--active` | 当前页，主题色背景，白色文字 |
| `.pagination__item--prev` / `--next` | 上一页/下一页，可选加宽 |
| `.empty-state` | 空状态居中容器 |

---

### 3.3 文章详情页 `/posts/[slug]`

```html
<main class="site-main">
  <div class="content-container">
    <div class="article-container">

      <!-- 可选：阅读进度条 -->
      <div class="reading-progress" role="progressbar"
           aria-valuenow="0" aria-valuemin="0"
           aria-valuemax="100" hidden>
        <div class="reading-progress__bar"
             style="width: 0%">
        </div>
      </div>

      <!-- 文章 -->
      <article class="post-detail">

        <!-- 文章头部 -->
        <header class="post-header">
          <h1 class="post-title">
            TypeScript 高级类型技巧
          </h1>

          <div class="post-meta">
            <time class="post-meta__date"
                  datetime="2026-04-27">
              2026-04-27
            </time>
            <span class="post-meta__separator">·</span>
            <span class="post-meta__author">张三</span>
            <span class="post-meta__separator">·</span>
            <span class="post-meta__reading-time">
              阅读约 8 分钟
            </span>
          </div>

          <div class="post-tags">
            <a class="tag-badge" href="/posts/?tag=TypeScript">
              TypeScript
            </a>
            <a class="tag-badge" href="/posts/?tag=前端">
              前端
            </a>
          </div>
        </header>

        <!-- 文章正文（Markdown 渲染输出） -->
        <div class="post-content">
          <!-- VitePress 将 Markdown 渲染为以下 HTML 结构 -->

          <p>TypeScript 的类型系统非常强大，除了基础类型外...</p>

          <h2 id="条件类型">
            <a href="#条件类型" class="header-anchor">#</a>
            条件类型
          </h2>
          <p>条件类型是 TypeScript 中一个强大特性...</p>

          <div class="code-block">
            <div class="code-block__header">
              <span class="code-block__lang">typescript</span>
            </div>
            <pre><code class="language-typescript">
type IsString&lt;T&gt; = T extends string ? true : false;
            </code></pre>
          </div>

          <h2 id="映射类型">
            <a href="#映射类型" class="header-anchor">#</a>
            映射类型
          </h2>
          <p>映射类型允许你基于旧类型创建新类型...</p>

          <!-- 更多内容... -->
        </div>

        <!-- 文章底部标签 -->
        <div class="post-tags post-tags--bottom">
          <span class="post-tags__label">标签：</span>
          <a class="tag-badge" href="/posts/?tag=TypeScript">
            TypeScript
          </a>
          <a class="tag-badge" href="/posts/?tag=前端">
            前端
          </a>
        </div>

        <!-- 上一篇 / 下一篇导航 -->
        <nav class="post-nav" aria-label="文章导航">
          <a class="post-nav__item post-nav__item--prev"
             href="/posts/vue3-composition-api">
            <span class="post-nav__direction">上一篇</span>
            <span class="post-nav__title">
              Vue 3 组合式 API 实战
            </span>
          </a>
          <a class="post-nav__item post-nav__item--next"
             href="/posts/build-cli-tool">
            <span class="post-nav__direction">下一篇</span>
            <span class="post-nav__title">
              构建一个 CLI 工具
            </span>
          </a>
        </nav>

      </article>

      <!-- 404 状态（默认隐藏） -->
      <div class="not-found" hidden>
        <h2 class="not-found__code">404</h2>
        <p class="not-found__text">文章未找到</p>
        <a class="btn-link" href="/">← 返回首页</a>
      </div>

    </div>
  </div>
</main>
```

**CSS 关键类**：

| 类名 | 用途 |
|------|------|
| `.reading-progress` | 固定顶部，height 3px，z-index 100 |
| `.reading-progress__bar` | 主题色，transition width 0.1s |
| `.post-header` | 文章头部，margin-bottom 32px |
| `.post-title` | 28px，font-weight 700，line-height 1.4 |
| `.post-meta` | flex，gap 8px，次级文字色 |
| `.post-meta__separator` | 分隔点，color #ccc |
| `.post-tags` | flex，gap 8px，margin 16px 0 |
| `.post-content` | 文章正文区，见下方详细规范 |
| `.post-content h2` | 22px，600，margin 32px 0 12px |
| `.post-content h3` | 18px，600，margin 24px 0 8px |
| `.post-content p` | 16px，line-height 1.8，margin 12px 0 |
| `.post-content img` | max-width 100%，border-radius 8px |
| `.post-content blockquote` | 左边框 4px 主题色，padding 12px 20px |
| `.post-content table` | 全宽，border-collapse |
| `.code-block` | 代码块容器，border-radius 8px，overflow hidden |
| `.code-block__header` | 代码块顶部栏，背景深色，语言标注 |
| `.post-nav` | flex，justify-between，margin 48px 0 |
| `.post-nav__item` | 固定宽度 45%，padding 16px，border |
| `.post-nav__direction` | 小字 12px，次级色 |
| `.post-nav__title` | 16px，font-weight 500 |
| `.not-found` | 居中，margin 80px 0 |

---

### 3.4 标签聚合页 `/tags/`

```html
<main class="site-main">
  <div class="content-container">

    <header class="page-header">
      <h1 class="page-header__title">所有标签</h1>
    </header>

    <!-- 标签卡片网格 -->
    <div class="tag-grid">
      <a class="tag-card" href="/posts/?tag=TypeScript">
        <span class="tag-card__name">TypeScript</span>
        <span class="tag-card__count">12 篇</span>
      </a>
      <a class="tag-card" href="/posts/?tag=Vue">
        <span class="tag-card__name">Vue</span>
        <span class="tag-card__count">8 篇</span>
      </a>
      <!-- 更多标签卡片... -->
    </div>

    <!-- 按标签分组的文章链接 -->
    <div class="tag-article-groups">

      <section class="tag-group" id="tag-TypeScript">
        <h2 class="tag-group__title">
          <a class="tag-badge tag-badge--large"
             href="/posts/?tag=TypeScript">
            TypeScript
          </a>
          <span class="tag-group__count">12 篇</span>
        </h2>
        <ul class="tag-group__articles">
          <li class="tag-group__article">
            <a class="tag-group__link"
               href="/posts/typescript-advanced">
              TypeScript 高级类型技巧
            </a>
            <time class="tag-group__date"
                  datetime="2026-04-27">
              2026-04-27
            </time>
          </li>
          <li class="tag-group__article">
            <a class="tag-group__link"
               href="/posts/typescript-decorators">
              TypeScript 装饰器实战
            </a>
            <time class="tag-group__date"
                  datetime="2026-04-10">
              2026-04-10
            </time>
          </li>
          <!-- 更多文章... -->
        </ul>
      </section>

      <section class="tag-group" id="tag-Vue">
        <!-- Vue 分组... -->
      </section>

      <!-- 更多分组... -->
    </div>

  </div>
</main>
```

**CSS 关键类**：

| 类名 | 用途 |
|------|------|
| `.tag-grid` | CSS Grid，grid-template-columns: repeat(auto-fill, minmax(140px, 1fr))，gap 12px |
| `.tag-card` | 标签卡片，border，border-radius 8px，padding 16px，hover 阴影 |
| `.tag-card__name` | 16px，600 |
| `.tag-card__count` | 12px，次级色 |
| `.tag-article-groups` | 分组容器，margin 48px 0 |
| `.tag-group` | 单个分组，margin-bottom 32px |
| `.tag-group__title` | flex，align-items center，gap 12px |
| `.tag-group__articles` | list-style none，padding 0 |
| `.tag-group__article` | flex justify-between，padding 8px 0，border-bottom divider |
| `.tag-group__link` | 文章标题链接 |
| `.tag-group__date` | 日期，次级色，14px |

---

### 3.5 关于页 `/about/`

```html
<main class="site-main">
  <div class="content-container">
    <div class="article-container">

      <article class="about-page">

        <!-- 头像 + 名称 -->
        <div class="about-header">
          <img class="about-header__avatar"
               src="/avatar.png"
               alt="张三的头像"
               width="180"
               height="180"
               loading="eager">
          <h1 class="about-header__name">张三</h1>
          <p class="about-header__tagline">
            前端开发 · 技术写作者
          </p>
        </div>

        <!-- 简介内容（Markdown 渲染） -->
        <div class="about-content">
          <h2>关于我</h2>
          <p>你好！我是一名前端开发工程师，目前居住在北京。...</p>
          <p>这个博客是我个人技术积累的输出窗口...</p>

          <h3>技能</h3>
          <div class="skill-list">
            <span class="tag-badge">TypeScript</span>
            <span class="tag-badge">Vue</span>
            <span class="tag-badge">React</span>
            <span class="tag-badge">Node.js</span>
            <span class="tag-badge">CSS</span>
            <span class="tag-badge">Git</span>
            <span class="tag-badge">CI/CD</span>
          </div>

          <h3>找到我</h3>
          <div class="social-links">
            <a class="social-link" href="https://github.com/zhangsan"
               target="_blank" rel="noopener noreferrer">
              <!-- GitHub 图标 -->
              GitHub
            </a>
            <a class="social-link" href="https://twitter.com/zhangsan"
               target="_blank" rel="noopener noreferrer">
              <!-- Twitter 图标 -->
              Twitter
            </a>
            <a class="social-link" href="mailto:zhangsan@example.com">
              <!-- Email 图标 -->
              Email
            </a>
          </div>
        </div>

      </article>

    </div>
  </div>
</main>
```

---

### 3.6 404 页面

```html
<main class="site-main">
  <div class="content-container">

    <div class="not-found-page">
      <h1 class="not-found-page__code">404</h1>
      <p class="not-found-page__title">页面未找到</p>
      <p class="not-found-page__description">
        你访问的页面不存在或已被移除
      </p>
      <a class="btn-link" href="/">← 返回首页</a>
    </div>

  </div>
</main>
```

**CSS 关键类**：

| 类名 | 用途 |
|------|------|
| `.not-found-page` | text-align center，padding 120px 0 |
| `.not-found-page__code` | 96px，font-weight 800，主题色，opacity 0.3 |
| `.not-found-page__title` | 28px，600 |
| `.not-found-page__description` | 16px，次级色 |

---

## 4. 组件 HTML 结构

### 4.1 SiteNav — 顶部导航栏

```html
<nav class="site-nav" aria-label="主导航">
  <div class="nav-inner">

    <!-- Logo / 站点名 -->
    <a class="nav-logo" href="/" aria-label="返回首页">
      我的博客
    </a>

    <!-- 桌面端导航链接 -->
    <div class="nav-links">
      <a class="nav-link" href="/">首页</a>
      <a class="nav-link" href="/posts/">文章</a>
      <a class="nav-link" href="/tags/">标签</a>
      <a class="nav-link" href="/about/">关于</a>
    </div>

    <!-- 移动端汉堡按钮（仅在 < 640px 显示） -->
    <button class="nav-hamburger"
            aria-label="打开菜单"
            aria-expanded="false">
      <span class="nav-hamburger__line"></span>
      <span class="nav-hamburger__line"></span>
      <span class="nav-hamburger__line"></span>
    </button>

    <!-- 移动端全屏菜单（默认隐藏） -->
    <div class="nav-mobile-menu" hidden>
      <div class="nav-mobile-menu__overlay"></div>
      <div class="nav-mobile-menu__panel">
        <a class="nav-mobile-menu__link" href="/">首页</a>
        <a class="nav-mobile-menu__link" href="/posts/">文章</a>
        <a class="nav-mobile-menu__link" href="/tags/">标签</a>
        <a class="nav-mobile-menu__link" href="/about/">关于</a>
      </div>
    </div>

  </div>
</nav>
```

**CSS 关键类**：

| 类名 | 用途 |
|------|------|
| `.site-nav` | sticky top 0，height 56px，背景 blur，z-index 100 |
| `.nav-inner` | max-width 960px，flex justify-between align-items center |
| `.nav-logo` | 18px，font-weight 700，无下划线 |
| `.nav-links` | flex，gap 24px（桌面）；hidden（移动） |
| `.nav-link` | 16px，次级色，hover 主题色 |
| `.nav-link--active` | 当前页面高亮，主题色 + 下划线 |
| `.nav-hamburger` | 仅在移动端显示，3 条横线 |
| `.nav-mobile-menu__overlay` | fixed inset-0，背景半透明 |
| `.nav-mobile-menu__panel` | fixed right 0，width 280px，背景白色 |

### 4.2 SiteFooter — 底部页脚

```html
<footer class="site-footer" role="contentinfo">
  <div class="footer-inner">
    <p class="footer-copyright">
      © 2026 张三
    </p>
    <p class="footer-powered">
      Powered by <a href="https://vitepress.dev/">VitePress</a>
    </p>
  </div>
</footer>
```

**CSS 关键类**：

| 类名 | 用途 |
|------|------|
| `.site-footer` | border-top，padding 24px 0，次级色 |
| `.footer-inner` | max-width 960px，flex justify-between |
| `.footer-copyright` | 14px |
| `.footer-powered` | 14px，链接主题色 |

### 4.3 ArticleCard — 文章卡片

```html
<article class="article-card">
  <a class="article-card__link" href="/posts/hello-world">
    <time class="article-card__date"
          datetime="2026-04-27">
      2026-04-27
    </time>
    <h3 class="article-card__title">
      TypeScript 高级类型技巧
    </h3>
    <p class="article-card__summary">
      本文介绍 TypeScript 中一些实用的高级类型...
    </p>
  </a>
  <div class="article-card__tags">
    <a class="tag-badge" href="/posts/?tag=TypeScript">TypeScript</a>
    <a class="tag-badge" href="/posts/?tag=前端">前端</a>
  </div>
</article>
```

**CSS 关键类**：

| 类名 | 用途 |
|------|------|
| `.article-card` | border-bottom 1px divider，padding 20px 0 |
| `.article-card__link` | block，text-decoration none，color inherit |
| `.article-card__date` | 12px，次级色 |
| `.article-card__title` | 20px，600，margin 4px 0，hover 主题色 |
| `.article-card__summary` | 14px，次级色，line-height 1.6 |
| `.article-card__tags` | flex，gap 8px，margin-top 12px |

### 4.4 TagBadge — 标签徽章

```html
<!-- 普通模式 -->
<a class="tag-badge" href="/posts/?tag=TypeScript">
  TypeScript
</a>

<!-- 可关闭模式（用于筛选激活态） -->
<span class="tag-badge tag-badge--closable">
  TypeScript
  <button class="tag-badge__close"
          aria-label="清除 TypeScript 筛选"
          type="button">
    ✕
  </button>
</span>

<!-- 大尺寸（标签页使用） -->
<a class="tag-badge tag-badge--large" href="/posts/?tag=TypeScript">
  TypeScript
</a>
```

**CSS 关键类**：

| 类名 | 尺寸 | 样式 |
|------|------|------|
| `.tag-badge` (默认) | padding 2px 10px | border-radius 12px，次级背景色，12px 字体 |
| `.tag-badge--large` | padding 6px 20px | 16px 字体 |
| `.tag-badge--closable` | 同默认 | flex，gap 4px |
| `.tag-badge__close` | 16px×16px | 圆形按钮，hover 背景加深 |
| `.tag-badge:hover` | - | 主题色背景，白色文字 |

### 4.5 Pagination — 分页

```html
<nav class="pagination" aria-label="文章列表分页">
  <!-- 上一页（第一页时 disabled） -->
  <span class="pagination__item pagination__item--prev pagination__item--disabled">
    ◀ 上一页
  </span>

  <!-- 页码 -->
  <span class="pagination__item pagination__item--active">1</span>
  <a class="pagination__item" href="/posts/page/2">2</a>
  <a class="pagination__item" href="/posts/page/3">3</a>

  <!-- 下一页 -->
  <a class="pagination__item pagination__item--next"
     href="/posts/page/2">
    下一页 ▶
  </a>
</nav>
```

**状态说明**：

| 状态 | 实现 |
|------|------|
| 当前页 | `<span>` + `.pagination__item--active` |
| 可点击页 | `<a href="...">` |
| 禁用（首/末页） | `<span>` + `.pagination__item--disabled` |
| 总页数 = 1 | 整个分页隐藏 |

### 4.6 TagSelector — 标签筛选下拉

```html
<div class="tag-selector">
  <button class="tag-selector__trigger"
          aria-expanded="false"
          aria-haspopup="listbox"
          type="button">
    <span class="tag-selector__label">标签筛选: </span>
    <span class="tag-selector__current">全部</span>
    <span class="tag-selector__arrow">▼</span>
  </button>

  <ul class="tag-selector__dropdown"
      role="listbox"
      hidden>
    <li class="tag-selector__option tag-selector__option--active"
        role="option"
        aria-selected="true">
      <a href="/posts/">全部</a>
    </li>
    <li class="tag-selector__option" role="option">
      <a href="/posts/?tag=TypeScript">TypeScript (12)</a>
    </li>
    <!-- 更多标签... -->
  </ul>
</div>
```

**交互状态**：

| 状态 | CSS 表现 |
|------|----------|
| 默认 | `.tag-selector__dropdown[hidden]` |
| 展开 | 移除 `hidden`，`aria-expanded="true"` |
| 选项悬停 | `.tag-selector__option:hover` 背景色变化 |
| 选项激活 | `.tag-selector__option--active` 字体加粗 |

---

## 5. CSS 类命名规范

### 5.1 命名约定

采用 **BEM (Block-Element-Modifier)** 变体：

```
.block-name__element--modifier
```

| 部分 | 说明 | 示例 |
|------|------|------|
| Block | 独立组件 | `.article-card` |
| Element | 组件的子部分 | `.article-card__title` |
| Modifier | 状态或变体 | `.article-card--featured` |

### 5.2 全局工具类

```css
/* 布局 */
.content-container   /* 全局内容容器 */
.article-container   /* 文章阅读宽度容器 */

/* 排版 */
.section-title       /* 区域标题 */
.page-header         /* 页面标题区 */
.page-header__title  /* 页面主标题 */

/* 组件通用 */
.btn-link            /* 链接样式按钮 */
.tag-badge           /* 标签徽章 */
.card-list           /* 卡片列表容器 */
.empty-state         /* 空状态 */

/* 可见性 */
.hidden              /* display: none */
.sr-only             /* 屏幕阅读器专用（视觉隐藏） */

/* 响应式 */
.mobile-only         /* 仅在移动端显示 */
.desktop-only        /* 仅在桌面端显示 */
```

### 5.3 CSS 变量体系

```css
:root {
  /* 颜色 */
  --color-bg: #ffffff;
  --color-bg-secondary: #f8f8f8;
  --color-text: #1a1a1a;
  --color-text-secondary: #666666;
  --color-primary: #3451b2;
  --color-primary-hover: #2a4099;
  --color-border: #e0e0e0;
  --color-divider: #eeeeee;

  /* 排版 */
  --font-sans: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
  --font-mono: 'JetBrains Mono', 'Fira Code', 'Cascadia Code', monospace;
  --font-size-sm: 12px;
  --font-size-base: 16px;
  --font-size-lg: 18px;
  --font-size-xl: 20px;
  --font-size-2xl: 22px;
  --font-size-3xl: 28px;

  /* 间距 */
  --space-xs: 4px;
  --space-sm: 8px;
  --space-md: 16px;
  --space-lg: 24px;
  --space-xl: 32px;
  --space-2xl: 48px;

  /* 圆角 */
  --radius-sm: 4px;
  --radius-md: 8px;
  --radius-lg: 12px;
  --radius-full: 50%;

  /* 阴影 */
  --shadow-card: 0 1px 3px rgba(0, 0, 0, 0.08);
  --shadow-card-hover: 0 4px 12px rgba(0, 0, 0, 0.12);

  /* 断点 (参考用，实际用媒体查询) */
  --breakpoint-mobile: 640px;
  --breakpoint-tablet: 1024px;
  --breakpoint-desktop: 1280px;

  /* 导航 */
  --navbar-height: 56px;
}
```

---

## 6. Vue 组件映射

### 6.1 组件目录建议

```
.vitepress/theme/
├── index.ts                  # 主题入口，注册全局组件
├── style.css                 # 全局样式 + CSS 变量
├── components/
│   ├── SiteNav.vue           # 顶部导航栏
│   ├── SiteFooter.vue        # 底部页脚
│   ├── ArticleCard.vue       # 文章卡片
│   ├── TagBadge.vue          # 标签徽章
│   ├── Pagination.vue        # 分页
│   ├── TagSelector.vue       # 标签筛选下拉
│   ├── AuthorBio.vue         # 作者简介
│   ├── PostNav.vue           # 文章上下篇导航
│   ├── TagCloud.vue          # 标签云
│   └── ReadingProgress.vue   # 阅读进度条（P2）
└── layouts/
    └── Layout.vue            # 全局布局（覆盖 VitePress 默认）
```

### 6.2 组件与 HTML 结构对照

| HTML 结构章节 | Vue 组件 | Props |
|---------------|----------|-------|
| 4.1 SiteNav | `SiteNav.vue` | `currentPath: string` |
| 4.2 SiteFooter | `SiteFooter.vue` | `author: string`, `year: number` |
| 4.3 ArticleCard | `ArticleCard.vue` | `post: Post` |
| 4.4 TagBadge | `TagBadge.vue` | `label`, `to?`, `closable?`, `size?` |
| 4.5 Pagination | `Pagination.vue` | `current`, `total`, `basePath` |
| 4.6 TagSelector | `TagSelector.vue` | `tags`, `selected?` |

### 6.3 VitePress 数据加载

```typescript
// .vitepress/config.ts
// 使用 VitePress 的 createContentLoader 加载文章数据

import { createContentLoader } from 'vitepress'

export interface Post {
  title: string
  url: string
  date: {
    time: number
    string: string
  }
  tags: string[]
  summary?: string
  excerpt?: string
}

// 按日期排序的所有文章
export default createContentLoader('posts/*.md', {
  excerpt: true,
  transform(raw): Post[] {
    return raw
      .filter(page => !page.frontmatter.draft)
      .map(page => ({
        title: page.frontmatter.title,
        url: page.url,
        date: {
          time: +new Date(page.frontmatter.date),
          string: page.frontmatter.date
        },
        tags: page.frontmatter.tags || [],
        summary: page.frontmatter.summary || page.excerpt
      }))
      .sort((a, b) => b.date.time - a.date.time)
  }
})
```

---

## 7. 静态资源引用

### 7.1 资源目录

```
public/
├── avatar.png          # 作者头像 (推荐 400×400px)
├── favicon.ico         # 网站图标
├── og-image.png        # Open Graph 默认分享图 (1200×630px)
└── images/             # 文章内嵌图片（可选）
    └── post/
        └── 2026-04-27/
            └── diagram.png
```

### 7.2 引用方式

```html
<!-- 公共资源：/ 开头 -->
<img src="/avatar.png" alt="作者头像">

<!-- 文章内图片：相对路径或 / 开头 -->
<img src="/images/post/2026-04-27/diagram.png" alt="架构图">

<!-- Markdown 中的图片 -->
![架构图](/images/post/2026-04-27/diagram.png)
```

### 7.3 SEO 相关 Meta

```html
<!-- VitePress 自动生成，frontmatter 补充 -->
---
title: TypeScript 高级类型技巧
description: 本文详细介绍 TypeScript 中条件类型、映射类型等高级技巧
date: 2026-04-27
tags:
  - TypeScript
  - 前端
head:
  - - meta
    - property: og:title
      content: TypeScript 高级类型技巧
  - - meta
    - property: og:description
      content: 本文详细介绍 TypeScript 中条件类型、映射类型等高级技巧
  - - meta
    - property: og:image
      content: /og-image.png
---
```

---

## 附录：HTML 检查清单

| # | 检查项 | 页面 |
|---|--------|------|
| ✓ | 所有 `<img>` 有 `alt` 属性 | 全部 |
| ✓ | `<nav>` 有 `aria-label` | 全部 |
| ✓ | `<time>` 有 `datetime` 属性 | 列表、详情 |
| ✓ | 使用 `<article>` 包裹文章内容 | 列表、详情 |
| ✓ | 使用 `<header>` / `<footer>` / `<main>` | 全部 |
| ✓ | `lang="zh-CN"` 在 `<html>` 标签 | 全部 |
| ✓ | `viewport` meta 标签 | 全部 |
| ✓ | 链接为 `<a>` 标签（非 `<div onclick>`） | 全部 |
| ✓ | 空状态有明确文字说明 | 列表、详情 |
| ✓ | 分页有 `aria-label` | 列表 |
| ✓ | 汉堡菜单 `aria-expanded` | 导航 |
| ✓ | 代码块有语言标注 | 详情 |
