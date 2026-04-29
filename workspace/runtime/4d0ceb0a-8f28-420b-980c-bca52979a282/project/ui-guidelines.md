# 公司内部投票系统 UI 原型交付包

## 1. 设计概览

### 1.1 产品定位

公司内部投票系统面向企业内部员工和管理员，核心价值是“快速投票、可控公开、实名审计”。界面应体现内部工具的专业、清晰和可信赖感，避免娱乐化和复杂装饰。

### 1.2 设计关键词

- 企业级
- 清晰高效
- 表单友好
- 数据可读
- 半匿名边界清晰
- PC + 手机响应式
- 管理后台高密度但不拥挤

### 1.3 核心页面范围

| 端 | 页面 | 目标 |
|---|---|---|
| 员工端 | 登录页 | 使用工号和密码安全进入系统 |
| 员工端 | 问卷列表页 | 快速识别可投、已投、未开始、已截止问卷 |
| 员工端 | 投票详情页 | 完成单选、多选、“其他”自定义答案提交 |
| 员工端 | 提交成功页 | 明确提交成功且不可修改 |
| 员工端 | 统计结果页 | 在管理员公开后查看统计，不展示实名明细 |
| 管理端 | 后台首页 | 查看关键概览和快捷入口 |
| 管理端 | 问卷管理页 | 创建、编辑、发布、关闭、公开问卷 |
| 管理端 | 问卷编辑页 | 配置标题、时间、题目、选项和“其他”能力 |
| 管理端 | 结果统计页 | 查看统计、实名明细、导出 Excel、控制公开 |
| 管理端 | 员工账号页 | 手动维护员工账号、密码、状态和角色 |

## 2. 视觉设计规范

### 2.1 设计风格

采用现代企业后台风格：浅色背景、白色卡片、蓝色主操作、清晰状态标签、轻量阴影。员工端更偏任务流，强调“当前状态 + 主操作”；管理端更偏信息管理，强调“筛选 + 表格 + 操作 + 数据结果”。

### 2.2 色彩系统

#### 2.2.1 语义色 Token

| Token | 色值 | 用途 |
|---|---|---|
| `--color-primary` | `#2563EB` | 主按钮、链接、选中态、焦点态 |
| `--color-primary-hover` | `#1D4ED8` | 主按钮悬停 |
| `--color-primary-soft` | `#DBEAFE` | 主色浅背景、选中卡片背景 |
| `--color-success` | `#16A34A` | 提交成功、进行中、已公开 |
| `--color-success-soft` | `#DCFCE7` | 成功浅背景 |
| `--color-warning` | `#F59E0B` | 未开始、未公开、即将截止 |
| `--color-warning-soft` | `#FEF3C7` | 警告浅背景 |
| `--color-danger` | `#DC2626` | 错误、关闭、停用、删除 |
| `--color-danger-soft` | `#FEE2E2` | 错误浅背景 |
| `--color-info` | `#0891B2` | 提示、说明、系统信息 |
| `--color-bg` | `#F3F4F6` | 页面背景 |
| `--color-surface` | `#FFFFFF` | 卡片、弹窗、表单面板 |
| `--color-surface-muted` | `#F9FAFB` | 表格头、弱强调区域 |
| `--color-border` | `#E5E7EB` | 边框、分割线 |
| `--color-text-primary` | `#111827` | 一级文字 |
| `--color-text-secondary` | `#374151` | 二级文字 |
| `--color-text-muted` | `#6B7280` | 辅助文字 |
| `--color-text-disabled` | `#9CA3AF` | 禁用文字 |

#### 2.2.2 状态标签颜色

| 状态 | 背景 | 文字 | 说明 |
|---|---|---|---|
| 草稿 | `#F3F4F6` | `#374151` | 管理端草稿问卷 |
| 未开始 | `#FEF3C7` | `#92400E` | 已发布但未到开始时间 |
| 进行中 | `#DCFCE7` | `#166534` | 当前可投票 |
| 已截止 | `#F3F4F6` | `#4B5563` | 时间已截止 |
| 已关闭 | `#FEE2E2` | `#991B1B` | 管理员手动关闭 |
| 已提交 | `#DBEAFE` | `#1E40AF` | 员工已投 |
| 未提交 | `#F9FAFB` | `#374151` | 员工尚未投 |
| 已公开 | `#DCFCE7` | `#166534` | 员工可看统计 |
| 未公开 | `#FEF3C7` | `#92400E` | 员工不可看统计 |

### 2.3 字体与排版

优先使用系统字体，保证企业内网环境加载稳定：

```css
font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", "PingFang SC", "Microsoft YaHei", "Helvetica Neue", Arial, sans-serif;
```

| 样式 | PC | Mobile | 字重 | 行高 | 使用场景 |
|---|---:|---:|---:|---:|---|
| Display | 32px | 26px | 700 | 1.25 | 登录页品牌标题、后台大标题 |
| H1 | 28px | 22px | 700 | 1.3 | 页面标题 |
| H2 | 22px | 20px | 600 | 1.35 | 分区标题 |
| H3 | 18px | 18px | 600 | 1.4 | 卡片标题、题目标题 |
| Body | 16px | 16px | 400 | 1.6 | 正文、选项内容 |
| Body Small | 14px | 14px | 400 | 1.5 | 表格、辅助说明 |
| Caption | 12px | 12px | 400/500 | 1.4 | 标签、错误提示、状态说明 |
| Number | 28px | 24px | 700 | 1.2 | 统计数字 |

### 2.4 间距系统

采用 4px 基础网格，常用间距：

| Token | 值 | 用途 |
|---|---:|---|
| `space-1` | 4px | 图标与文字小间距 |
| `space-2` | 8px | 表单提示、标签间距 |
| `space-3` | 12px | 紧凑列表内边距 |
| `space-4` | 16px | 移动端页面边距、卡片内边距 |
| `space-5` | 20px | 表单行间距 |
| `space-6` | 24px | PC 卡片内边距、分区间距 |
| `space-8` | 32px | 页面区块间距 |
| `space-10` | 40px | 登录页上下留白 |
| `space-12` | 48px | 大区块间距 |

### 2.5 圆角、阴影和边框

| Token | 值 | 用途 |
|---|---:|---|
| `radius-sm` | 6px | 标签、小按钮、输入框内部元素 |
| `radius-md` | 8px | 输入框、普通按钮、表格卡片 |
| `radius-lg` | 12px | 问卷卡片、题目卡片、统计卡片 |
| `radius-xl` | 16px | 登录卡片、移动端底部面板 |
| `shadow-card` | `0 1px 3px rgba(15,23,42,0.08)` | 默认卡片 |
| `shadow-popover` | `0 12px 32px rgba(15,23,42,0.16)` | 弹窗、下拉层 |
| `border-default` | `1px solid #E5E7EB` | 控件和卡片边框 |

## 3. 布局规范

### 3.1 全局页面结构

```text
应用根容器
├── 顶部导航 / 管理端顶部栏
├── 侧边栏（管理端 PC）
├── 主内容区
│   ├── 页面标题区
│   ├── 状态/提示区
│   ├── 主操作区
│   └── 内容卡片/表格/表单
└── 移动端底部固定操作区（仅关键提交/保存场景）
```

### 3.2 员工端布局

#### PC

- 顶部导航高度：64px。
- 主内容最大宽度：960px，居中。
- 页面左右内边距：24px。
- 问卷卡片纵向排列，卡片间距 16px。
- 投票详情页提交区位于内容底部，同时在长问卷时允许底部 sticky 操作条。

#### Mobile

- 顶部导航高度：56px。
- 页面左右内边距：16px。
- 问卷列表采用全宽卡片。
- 投票页题目卡片单列展示。
- 提交按钮使用底部固定 CTA，需预留安全区和底部内边距。

### 3.3 管理端布局

#### PC

```text
┌───────────────┬────────────────────────────────────┐
│ 侧边栏 240px   │ 顶部栏 64px                         │
│               ├────────────────────────────────────┤
│ 导航菜单       │ 主内容区                             │
│               │ 标题 + 操作 + 表格/表单/统计          │
└───────────────┴────────────────────────────────────┘
```

- 侧边栏宽度：240px。
- 主内容区内边距：24px-32px。
- 表格外包裹卡片，表格头使用浅灰背景。
- 问卷编辑页采用“基本信息卡片 + 题目配置卡片列表 + 底部操作条”。

#### Mobile / Tablet

- 侧边栏收起为顶部菜单按钮或抽屉。
- 表格转换为卡片列表，保留关键字段和主要操作。
- 问卷编辑页采用折叠分组：基本信息、题目 1、题目 2、发布设置。
- 结果页采用 Tab：统计汇总 / 实名明细。

## 4. 关键页面设计规范

### 4.1 登录页

#### 信息层级

1. 产品标题：公司内部投票系统
2. 副标题：使用工号和密码登录
3. 工号输入框
4. 密码输入框 + 显示/隐藏密码
5. 登录按钮
6. 错误提示区

#### 状态

| 状态 | 表现 |
|---|---|
| 默认 | 登录卡片居中，按钮可点击 |
| 输入中 | 输入框边框高亮，标签保持可见 |
| 校验错误 | 字段下方展示错误文案，错误区域使用红色文本 |
| 登录中 | 登录按钮 loading 且禁用 |
| 账号停用 | 顶部或字段下展示“账号已停用，请联系管理员” |

### 4.2 员工问卷列表页

#### 卡片内容

- 问卷标题
- 问卷说明摘要
- 状态标签：进行中 / 未开始 / 已截止 / 已关闭
- 时间信息：开始时间、截止时间
- 提交状态：未提交 / 已提交
- 结果状态：已公开 / 未公开
- 主操作：去投票 / 查看提交 / 查看统计 / 不可提交

#### 操作规则

| 问卷状态 | 提交状态 | 结果状态 | 主按钮 |
|---|---|---|---|
| 进行中 | 未提交 | 任意 | 去投票 |
| 进行中 | 已提交 | 未公开 | 查看提交 |
| 进行中 | 已提交 | 已公开 | 查看统计 |
| 未开始 | 任意 | 任意 | 未开始（禁用） |
| 已截止 | 未提交 | 未公开 | 已截止（禁用） |
| 已截止 | 已提交 | 已公开 | 查看统计 |
| 已关闭 | 任意 | 已公开 | 查看统计 |

### 4.3 员工投票详情页

#### 结构

```text
问卷信息卡片
├── 标题
├── 状态标签：进行中
├── 截止时间提示
└── 说明：每人仅可提交一次，提交后不可修改

题目卡片 1：单选题
├── 题号 + 必填标记 + 题型标签
├── Radio 选项列表
└── 错误提示区

题目卡片 2：多选题
├── 题号 + 必填标记 + 题型标签
├── Checkbox 选项列表
├── “其他”选项
├── 自定义输入框（选择其他后出现）
└── 多选说明

提交区
├── 次按钮：返回列表
└── 主按钮：提交投票
```

#### 表单交互

1. 单选题点击整行选项即可选中。
2. 多选题点击整行选项切换选中状态。
3. “其他”被选中后，在其下方展开输入框。
4. 提交前弹窗确认：“提交后不可修改，确认提交吗？”
5. 提交时按钮 loading，防止重复点击。
6. 若存在错误，页面滚动并聚焦到第一个错误题目。

### 4.4 提交成功页

- 使用成功图标占位（SVG 图标，不使用 emoji）。
- 文案：提交成功。
- 说明：每个问卷仅可提交一次，提交后不可修改。
- 操作：返回问卷列表；若结果已公开，显示“查看统计”。

### 4.5 员工统计结果页

#### 信息边界

普通员工统计页只展示聚合数据，不展示实名明细、工号、姓名、部门、提交时间和个人答案列表。

#### 结构

- 问卷标题和提交人数。
- 公开状态说明。
- 每个题目一个统计卡片。
- 每个选项展示票数、百分比、进度条。
- 多选题展示提示：“多选题按提交人数计算，占比合计可能超过 100%”。
- 未公开状态展示空提示：“结果暂未公开，请等待管理员公开”。

### 4.6 管理端问卷管理页

#### PC 表格列

| 列 | 说明 |
|---|---|
| 问卷标题 | 标题 + 简短说明 |
| 状态 | 草稿/未开始/进行中/已截止/已关闭 |
| 投票时间 | 开始时间 - 截止时间 |
| 提交人数 | 已提交人数 |
| 公开状态 | 已公开/未公开 |
| 创建人 | 管理员姓名 |
| 操作 | 编辑、结果、发布、关闭、公开、导出 |

#### Mobile 卡片字段

- 标题
- 状态 + 公开状态
- 时间
- 提交人数
- 主要操作按钮：结果 / 编辑 / 更多

### 4.7 管理端问卷编辑页

#### 基本信息区

- 问卷标题（必填）
- 问卷说明（可选）
- 开始时间（必填）
- 截止时间（必填）
- 状态预览：草稿 / 未开始 / 进行中

#### 题目配置区

每个题目卡片包含：

- 题目标题输入框
- 题型选择：单选 / 多选
- 选项列表：选项文本、删除按钮、排序控件
- 添加选项按钮
- 是否允许“其他”开关
- 题目删除按钮

#### 操作区

- 保存草稿
- 预览
- 发布
- 返回

#### 校验

- 标题必填。
- 截止时间必须晚于开始时间。
- 每个题目至少 2 个选项。
- 题目标题不能为空。
- 选项文本不能为空。

### 4.8 管理端结果统计页

#### 顶部概览

- 问卷标题
- 状态标签
- 提交人数
- 公开状态
- 操作按钮：公开/取消公开、导出统计 Excel、导出实名明细 Excel

#### 内容区

- Tab 1：统计汇总
  - 每题统计卡片
  - 横向进度条
  - 票数和百分比
  - “其他”汇总数量
- Tab 2：实名明细
  - 工号
  - 姓名
  - 部门
  - 提交时间
  - 每题答案
  - 其他自定义文本

#### 权限提示

页面顶部需标注：“实名明细仅管理员可见，请按公司数据管理要求使用和导出。”

### 4.9 员工账号管理页

#### 表格列

| 列 | 说明 |
|---|---|
| 工号 | 唯一登录标识 |
| 姓名 | 员工姓名 |
| 部门 | 所属部门 |
| 角色 | 普通员工 / 管理员 |
| 状态 | 启用 / 停用 |
| 更新时间 | 最近编辑时间 |
| 操作 | 编辑、重置密码、启用/停用 |

#### 表单字段

- 工号
- 姓名
- 部门
- 初始密码 / 重置密码
- 角色
- 状态

## 5. HTML 设计稿结构

> 以下结构用于后续生成静态 HTML 原型或前端页面，不包含业务实现代码。

### 5.1 全局结构

```html
<div class="app-shell">
  <header class="top-bar" role="banner">
    <a class="skip-link" href="#main">跳转到主要内容</a>
    <div class="brand">公司内部投票系统</div>
    <nav class="top-nav" aria-label="主导航"></nav>
    <div class="user-menu"></div>
  </header>
  <main id="main" class="main-content" tabindex="-1"></main>
</div>
```

### 5.2 登录页结构

```html
<main class="login-page">
  <section class="login-card" aria-labelledby="login-title">
    <h1 id="login-title">公司内部投票系统</h1>
    <p class="login-subtitle">请使用工号和密码登录</p>
    <form class="login-form">
      <div class="form-field">
        <label for="employeeNo">工号</label>
        <input id="employeeNo" name="employeeNo" autocomplete="username" />
        <p class="field-error" role="alert"></p>
      </div>
      <div class="form-field">
        <label for="password">密码</label>
        <div class="password-input">
          <input id="password" name="password" type="password" autocomplete="current-password" />
          <button type="button" aria-label="显示或隐藏密码">显示</button>
        </div>
        <p class="field-error" role="alert"></p>
      </div>
      <button class="btn btn-primary" type="submit">登录</button>
    </form>
  </section>
</main>
```

### 5.3 员工问卷列表结构

```html
<main class="employee-survey-list">
  <section class="page-header">
    <h1>问卷列表</h1>
    <p>请选择需要参与的内部投票</p>
  </section>
  <section class="survey-card-list" aria-label="问卷列表">
    <article class="survey-card">
      <div class="survey-card-header">
        <h2>2026 年团建活动投票</h2>
        <span class="status-tag status-active">进行中</span>
      </div>
      <p class="survey-summary">请根据个人时间和偏好选择团建安排。</p>
      <dl class="meta-list">
        <div><dt>开始时间</dt><dd>2026-05-01 09:00</dd></div>
        <div><dt>截止时间</dt><dd>2026-05-07 18:00</dd></div>
        <div><dt>提交状态</dt><dd><span class="status-tag">未提交</span></dd></div>
        <div><dt>结果状态</dt><dd><span class="status-tag status-warning">未公开</span></dd></div>
      </dl>
      <div class="card-actions">
        <button class="btn btn-primary">去投票</button>
      </div>
    </article>
  </section>
</main>
```

### 5.4 员工投票页结构

```html
<main class="vote-page">
  <section class="survey-hero-card">
    <div class="title-row">
      <h1>2026 年团建活动投票</h1>
      <span class="status-tag status-active">进行中</span>
    </div>
    <p>每人仅可提交一次，提交后不可修改。</p>
    <p class="deadline">截止时间：2026-05-07 18:00</p>
  </section>

  <form class="vote-form">
    <fieldset class="question-card">
      <legend><span>1.</span> 你更希望团建安排在哪一天？ <span class="required">*</span></legend>
      <p class="question-type">单选题</p>
      <label class="choice-row"><input type="radio" name="q1" /> 周五下午</label>
      <label class="choice-row"><input type="radio" name="q1" /> 周六全天</label>
      <label class="choice-row"><input type="radio" name="q1" /> 其他</label>
      <input class="other-input" placeholder="请输入其他答案" />
      <p class="field-error" role="alert">请选择一个选项</p>
    </fieldset>

    <fieldset class="question-card">
      <legend><span>2.</span> 你偏好的团建活动类型有哪些？ <span class="required">*</span></legend>
      <p class="question-type">多选题</p>
      <label class="choice-row"><input type="checkbox" name="q2" /> 户外徒步</label>
      <label class="choice-row"><input type="checkbox" name="q2" /> 桌游轰趴</label>
      <label class="choice-row"><input type="checkbox" name="q2" /> 其他</label>
      <input class="other-input" placeholder="请输入其他答案" />
    </fieldset>

    <div class="sticky-action-bar">
      <button class="btn btn-secondary" type="button">返回列表</button>
      <button class="btn btn-primary" type="submit">提交投票</button>
    </div>
  </form>
</main>
```

### 5.5 管理后台结构

```html
<div class="admin-shell">
  <aside class="admin-sidebar" aria-label="后台导航">
    <div class="brand">投票管理</div>
    <nav>
      <a class="nav-item active" href="/admin/surveys">问卷管理</a>
      <a class="nav-item" href="/admin/employees">员工账号</a>
    </nav>
  </aside>
  <div class="admin-main-shell">
    <header class="admin-topbar">
      <button class="menu-button" aria-label="打开导航菜单"></button>
      <div class="admin-user">李管理员</div>
    </header>
    <main class="admin-content" id="main"></main>
  </div>
</div>
```

### 5.6 管理端结果区结构

```html
<main class="result-page">
  <section class="result-header-card">
    <div>
      <h1>2026 年团建活动投票</h1>
      <p>提交人数：28 人</p>
    </div>
    <div class="action-group">
      <button class="btn btn-secondary">公开结果</button>
      <button class="btn btn-primary">导出统计 Excel</button>
      <button class="btn btn-primary">导出实名明细 Excel</button>
    </div>
  </section>

  <div class="tabs" role="tablist" aria-label="结果视图">
    <button role="tab" aria-selected="true">统计汇总</button>
    <button role="tab" aria-selected="false">实名明细</button>
  </div>

  <section class="stats-panel" role="tabpanel">
    <article class="stat-question-card">
      <h2>你更希望团建安排在哪一天？</h2>
      <div class="result-row">
        <span>周五下午</span>
        <div class="progress"><div style="width: 50%"></div></div>
        <strong>14 票 / 50%</strong>
      </div>
    </article>
  </section>

  <section class="detail-panel" role="tabpanel" hidden>
    <table class="data-table">
      <thead>
        <tr><th>工号</th><th>姓名</th><th>部门</th><th>提交时间</th><th>答案</th></tr>
      </thead>
      <tbody></tbody>
    </table>
  </section>
</main>
```

## 6. 组件库清单

| 组件 | 用途 | 关键状态 | 可访问性要求 |
|---|---|---|---|
| Button 按钮 | 登录、提交、保存、发布、导出 | default、hover、active、disabled、loading、danger | 使用 `button` 语义；loading 时 `aria-busy=true`；禁用使用 `disabled` |
| Text Input 输入框 | 工号、密码、题目标题、选项、其他答案 | default、focus、error、disabled、readonly | 必须有 `label`；错误用 `role=alert` 或 `aria-describedby` |
| Password Input 密码框 | 登录和重置密码 | hidden、visible、error、loading | 显示/隐藏按钮需有 `aria-label` |
| Radio Group 单选组 | 单选题作答 | unchecked、checked、focus、error、disabled | 使用 `fieldset` + `legend`；键盘方向键可切换 |
| Checkbox Group 多选组 | 多选题作答 | unchecked、checked、focus、error、disabled | 使用 `fieldset` + `legend`；点击行也可切换 |
| Select/Segmented Control | 题型选择、角色选择、状态筛选 | default、selected、disabled、error | 明确 label，移动端触控高度 ≥44px |
| Switch 开关 | 是否允许其他、是否公开结果 | on、off、disabled、focus | 需读出当前状态，如“已开启允许其他答案” |
| Status Tag 状态标签 | 问卷状态、提交状态、公开状态 | 多语义色 | 不仅依赖颜色，必须有文本 |
| Survey Card 问卷卡片 | 员工端问卷列表 | normal、active、submitted、disabled | 卡片内主操作按钮可键盘访问 |
| Question Card 题目卡片 | 投票题目和后台题目配置 | normal、error、readonly、collapsed | 标题层级清晰；错误可被读屏感知 |
| Result Bar 结果条 | 统计票数和百分比 | 0%、部分、100%、empty | 进度条需有文本数值，不只靠宽度 |
| Data Table 数据表 | 问卷管理、实名明细、员工账号 | loading、empty、sorted、selected | 表头语义正确；排序使用 `aria-sort` |
| Tabs 标签页 | 统计汇总/实名明细切换 | active、inactive、focus | 使用 `role=tablist/tab/tabpanel` |
| Modal 确认弹窗 | 提交确认、关闭问卷、公开结果、删除 | open、loading、error | 打开时焦点进入弹窗；Esc 可关闭；焦点限制在弹窗内 |
| Toast 提示 | 操作成功、导出失败、网络错误 | success、error、warning、info | `aria-live=polite`；不抢焦点 |
| Empty State 空状态 | 无问卷、无提交、未公开结果 | default、actionable | 文案说明原因和下一步操作 |
| Skeleton 骨架屏 | 列表/统计加载 | loading | 避免布局跳动，尊重 reduced motion |
| Drawer 抽屉导航 | 移动端后台导航 | open、closed | 打开后焦点管理，遮罩可关闭 |
| Sticky Action Bar 固定操作条 | 移动端提交、后台保存 | default、loading、disabled | 不遮挡内容；底部安全区留白 |

## 7. 交互原型说明

### 7.1 员工投票关键路径

```text
登录页 → 问卷列表页 → 投票详情页 → 提交确认弹窗 → 提交成功页 → 返回列表/查看统计
```

#### 反馈规则

1. 登录失败：输入框下方展示“工号或密码错误”。
2. 账号停用：展示“账号已停用，请联系管理员”。
3. 问卷不可提交：按钮禁用，并显示原因标签，如“未开始”“已截止”。
4. 必填题未答：题目卡片边框变为错误色，错误文案显示在题目下方。
5. 其他未填：输入框下方显示“请填写其他内容”。
6. 提交中：提交按钮 loading，禁止重复点击。
7. 提交成功：成功页展示明确确认和不可修改提示。
8. 重复提交：若服务端返回重复提交，展示错误提示并切换为已提交只读状态。

### 7.2 管理员创建问卷路径

```text
后台登录 → 问卷管理 → 新建问卷 → 填写基本信息 → 添加题目 → 配置选项 → 保存草稿/预览 → 发布
```

#### 反馈规则

1. 保存草稿：toast “草稿已保存”。
2. 发布前校验失败：页面顶部显示错误摘要，并定位到第一个错误项。
3. 删除题目/选项：弹出二次确认。
4. 关闭问卷：危险确认弹窗，说明关闭后员工不可继续提交。
5. 公开结果：确认弹窗，说明公开后普通员工可查看统计。

### 7.3 管理员查看结果路径

```text
问卷管理 → 查看结果 → 统计汇总 / 实名明细 → 导出统计 Excel / 导出实名明细 Excel → 公开结果
```

#### 反馈规则

1. 统计加载：展示统计卡片骨架屏。
2. 无提交数据：展示空状态“暂无提交数据”。
3. 导出中：导出按钮 loading，不重复触发。
4. 导出成功：toast “导出成功”。
5. 导出失败：toast “导出失败，请稍后重试”。
6. 普通员工误访问导出/实名接口：显示无权限页或错误提示。

### 7.4 错误状态

| 场景 | UI 表现 | 恢复路径 |
|---|---|---|
| 网络错误 | 页面内错误卡片 + 重试按钮 | 点击重试 |
| 登录过期 | Toast 提示 + 跳转登录页 | 重新登录 |
| 无权限 | 无权限页，说明当前账号不可访问 | 返回问卷列表/联系管理员 |
| 表单校验失败 | 字段级错误 + 顶部错误摘要 | 修改字段后重新提交 |
| 导出失败 | Toast + 按钮恢复可点击 | 稍后重试 |
| 问卷不存在 | 空状态页 | 返回列表 |
| 结果未公开 | 空状态卡片 | 等待管理员公开 |

### 7.5 空状态文案

| 页面 | 空状态文案 | 操作 |
|---|---|---|
| 员工问卷列表 | 暂无可查看的投票问卷 | 刷新 |
| 管理端问卷列表 | 还没有投票问卷 | 新建问卷 |
| 统计结果 | 暂无提交数据 | 返回问卷管理 |
| 实名明细 | 暂无员工提交记录 | 返回统计汇总 |
| 员工账号 | 暂无员工账号 | 新增员工 |
| 未公开结果 | 结果暂未公开，请等待管理员公开 | 返回问卷列表 |

## 8. 响应式适配参考

### 8.1 断点定义

| 断点 | 宽度 | 设备假设 | 布局策略 |
|---|---:|---|---|
| XS | 375px | 小屏手机 | 单列、底部 CTA、卡片化表格 |
| SM | 768px | 平板/大屏手机横屏 | 单列或双列混合，侧边栏仍可收起 |
| MD | 1024px | 小桌面/平板横屏 | 管理端可显示侧边栏，内容双列 |
| LG | 1440px | 桌面大屏 | 完整后台布局，表格和统计并列展示 |

### 8.2 375px 手机

- 页面左右边距：16px。
- 顶部栏高度：56px。
- 主按钮高度：48px。
- 输入框高度：48px。
- 问卷卡片单列展示。
- 管理端表格全部转换为卡片列表。
- 问卷编辑题目使用折叠卡片，默认展开当前编辑题。
- 底部固定操作条高度约 72px，含安全区 padding。
- 不允许横向滚动。

### 8.3 768px 平板

- 页面左右边距：24px。
- 员工端内容最大宽度可设为 720px。
- 问卷列表可保持单列，卡片更宽。
- 管理端导航可使用抽屉或窄侧边栏。
- 统计结果可采用单列卡片，实名明细仍建议卡片化或横向可控表格容器。

### 8.4 1024px 小桌面

- 管理端显示 220-240px 侧边栏。
- 主内容区 padding 24px。
- 问卷编辑基本信息可采用 2 列表单。
- 统计页顶部指标可 3-4 列排列。
- 表格正常展示，操作列使用“更多”收纳低频操作。

### 8.5 1440px 大桌面

- 主内容区最大宽度建议 1180-1280px，避免过宽导致阅读困难。
- 结果页可采用左侧统计卡片、右侧概览/操作辅助区。
- 表格列完整展示。
- 问卷编辑页可使用右侧预览栏，但 MVP 可不强制。

### 8.6 响应式 CSS 参考

```css
:root {
  --page-padding: 16px;
  --content-max-width: 960px;
}

@media (min-width: 768px) {
  :root { --page-padding: 24px; }
}

@media (min-width: 1024px) {
  :root {
    --page-padding: 32px;
    --admin-sidebar-width: 240px;
  }
}

@media (min-width: 1440px) {
  :root { --content-max-width: 1280px; }
}
```

## 9. 可访问性与可用性要求

1. 正常文本对比度不低于 4.5:1，大号文字不低于 3:1。
2. 所有表单控件必须有可见 label。
3. Radio、Checkbox、Tab、Modal 支持键盘操作。
4. 焦点状态使用 2px 蓝色描边或外发光，不可移除。
5. 错误提示不能只依赖颜色，需要有明确文字。
6. 状态标签必须包含文本，不只使用颜色。
7. 移动端触控目标不小于 44px × 44px。
8. Toast 使用 `aria-live=polite`，不抢占焦点。
9. 弹窗打开后焦点移动到弹窗标题或第一个操作按钮，关闭后焦点回到触发按钮。
10. 路由切换后焦点移动到主内容区。
11. 支持浏览器缩放，不禁用用户缩放。
12. 动画尊重 `prefers-reduced-motion`。

## 10. 动效规范

| 场景 | 动效 | 时长 | 说明 |
|---|---|---:|---|
| 按钮点击 | 轻微背景色变化/透明度变化 | 100-150ms | 不改变布局尺寸 |
| 卡片 hover | 阴影增强 + 边框变蓝 | 150ms | PC 可用，移动端不依赖 hover |
| “其他”输入框展开 | opacity + translateY | 180ms | 展示因果关系 |
| 弹窗出现 | fade + scale(0.98→1) | 180-220ms | 保持轻量 |
| Toast | fade + translateY | 200ms | 自动 3-5 秒消失 |
| Tab 切换 | crossfade | 150ms | 避免大幅移动 |
| 骨架屏 | shimmer 可选 | 1000-1400ms | reduced motion 时禁用 shimmer |

## 11. 设计交付与实现注意事项

1. `ui-design.svg` 表达主要页面布局、视觉层级和组件状态，不作为最终像素级视觉稿。
2. 后续 HTML/CSS 实现应使用语义化标签，优先原生表单控件。
3. 管理员与普通员工的数据边界必须在 UI 层清楚表达，但最终安全由服务端权限控制。
4. 普通员工统计页不得出现实名字段。
5. 导出按钮仅在管理员结果页出现。
6. 手机端后台只要求关键流程可用，复杂表格应转卡片或使用列优先级折叠。
7. 任何危险操作（关闭问卷、停用账号、删除题目）均需要二次确认。
8. 表单保存、发布、提交均需 loading 和错误恢复路径。
