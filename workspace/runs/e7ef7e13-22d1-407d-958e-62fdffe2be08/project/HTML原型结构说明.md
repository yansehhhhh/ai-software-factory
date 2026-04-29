# 投票系统 HTML 原型结构说明

**产品名称**: 公开投票管理系统
**文档版本**: v1.0
**创建日期**: 2026-04-27
**负责人**: Product Agent

---

## 1. 文档概述

本文档描述投票系统的 HTML 页面结构和布局，为前端开发人员提供具体的实现参考。包括页面目录结构、主要页面 HTML 结构示例、CSS 类名规范等内容。

---

## 2. 页面目录结构

### 2.1 用户端页面结构

```
apps/web/src/views/
├── home/
│   ├── index.vue          # 首页 - 活动列表页
│   └── components/
│       ├── ActivityCard.vue      # 活动卡片组件
│       ├── FilterTabs.vue        # 筛选标签组件
│       └── SearchBox.vue         # 搜索框组件
├── activity/
│   ├── detail.vue         # 活动详情页
│   ├── result.vue         # 投票结果页
│   └── components/
│       ├── ActivityInfo.vue      # 活动信息组件
│       ├── OptionCard.vue        # 选项卡片组件
│       ├── VoteButton.vue        # 投票按钮组件
│       ├── VoteResult.vue        # 投票结果组件
│       └── ResultChart.vue       # 结果图表组件
├── user/
│   ├── login.vue          # 登录弹窗
│   ├── profile.vue        # 个人中心页
│   └── components/
│       ├── LoginModal.vue        # 登录弹窗组件
│       ├── UserInfo.vue          # 用户信息组件
│       └── VoteRecord.vue        # 投票记录组件
└── components/
    ├── Header.vue         # 页面头部组件
    ├── Footer.vue         # 底部导航组件
    ├── Toast.vue          # 提示组件
    ├── Modal.vue          # 弹窗组件
    └── Loading.vue        # 加载组件
```

### 2.2 管理端页面结构

```
apps/web/src/views/admin/
├── login/
│   ├── index.vue          # 管理员登录页
├── dashboard/
│   ├── index.vue          # 后台首页
│   └── components/
│       ├── Sidebar.vue           # 左侧导航组件
│       ├── StatsOverview.vue     # 统计概览组件
│       └── RecentActivities.vue  # 最近活动组件
├── activity/
│   ├── list.vue           # 活动列表页
│   ├── create.vue         # 创建活动页
│   ├── edit.vue           # 编辑活动页
│   ├── stats.vue          # 活动统计页
│   └── components/
│       ├── ActivityTable.vue     # 活动表格组件
│       ├── ActivityForm.vue      # 活动表单组件
│       ├── OptionForm.vue        # 选项表单组件
│       ├── ImageUpload.vue       # 图片上传组件
│       └── ActivityStats.vue     # 活动统计组件
└── components/
    ├── AdminHeader.vue    # 管理端头部组件
    └── Pagination.vue     # 分页组件
```

---

## 3. 用户端页面 HTML 结构

### 3.1 首页 - 活动列表页

#### 3.1.1 页面整体结构

```html
<template>
  <div class="home-page">
    <!-- 页面顶部 -->
    <header class="header">
      <div class="logo">投票系统</div>
      <button class="login-btn" @click="showLoginModal">登录</button>
      <div class="profile-icon" @click="goToProfile">
        <i class="icon-user"></i>
      </div>
    </header>

    <!-- 筛选区域 -->
    <div class="filter-section">
      <FilterTabs :tabs="filterTabs" :activeTab="activeTab" @change="onTabChange" />
      <SearchBox :placeholder="搜索活动" @search="onSearch" />
    </div>

    <!-- 活动列表 -->
    <div class="activity-list">
      <ActivityCard v-for="activity in activities" :key="activity.id" :activity="activity" />
      <div v-if="activities.length === 0" class="empty-state">
        <i class="icon-empty"></i>
        <p>暂无投票活动</p>
      </div>
    </div>

    <!-- 底部导航 -->
    <footer class="footer-nav">
      <div class="nav-item active">
        <i class="icon-home"></i>
        <span>首页</span>
      </div>
      <div class="nav-item">
        <i class="icon-vote"></i>
        <span>我的投票</span>
      </div>
      <div class="nav-item">
        <i class="icon-profile"></i>
        <span>个人中心</span>
      </div>
    </footer>

    <!-- 登录弹窗 -->
    <LoginModal v-if="showLogin" @close="closeLoginModal" />
  </div>
</template>
```

#### 3.1.2 CSS 类名说明

```css
/* 页面容器 */
.home-page {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  background-color: #f3f4f6;
}

/* 页面顶部 */
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  background-color: #ffffff;
  border-bottom: 1px solid #e5e7eb;
  position: sticky;
  top: 0;
  z-index: 100;
}

.logo {
  font-size: 18px;
  font-weight: bold;
  color: #1f2937;
}

.login-btn {
  padding: 8px 16px;
  background-color: #3b82f6;
  color: #ffffff;
  border-radius: 4px;
  border: none;
  font-size: 14px;
}

.profile-icon {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #6b7280;
}

/* 筛选区域 */
.filter-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  background-color: #ffffff;
}

/* 活动列表 */
.activity-list {
  flex: 1;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px 16px;
  color: #9ca3af;
}

/* 底部导航 */
.footer-nav {
  display: flex;
  justify-content: space-around;
  padding: 12px 16px;
  background-color: #ffffff;
  border-top: 1px solid #e5e7eb;
  position: sticky;
  bottom: 0;
}

.nav-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  color: #6b7280;
  font-size: 12px;
}

.nav-item.active {
  color: #3b82f6;
}

/* 响应式布局 */
@media (min-width: 768px) {
  .activity-list {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 16px;
  }
}

@media (min-width: 1024px) {
  .activity-list {
    grid-template-columns: repeat(3, 1fr);
  }
}
```

### 3.2 活动卡片组件

```html
<template>
  <div class="activity-card" @click="goToDetail">
    <div class="card-cover">
      <img :src="activity.coverImage" :alt="activity.title" class="cover-image">
      <div v-if="activity.status === 'ongoing'" class="status-tag ongoing">进行中</div>
      <div v-else class="status-tag ended">已结束</div>
    </div>
    <div class="card-content">
      <h3 class="card-title">{{ activity.title }}</h3>
      <div class="card-meta">
        <span class="meta-item">
          <i class="icon-time"></i>
          {{ activity.startTime }}
        </span>
        <span class="meta-item">
          <i class="icon-users"></i>
          {{ activity.participantCount }}人参与
        </span>
      </div>
      <button class="card-btn" :class="activity.status === 'ongoing' ? 'btn-primary' : 'btn-secondary'">
        {{ activity.status === 'ongoing' ? '立即参与' : '查看结果' }}
      </button>
    </div>
  </div>
</template>
```

```css
/* 活动卡片 */
.activity-card {
  display: flex;
  flex-direction: column;
  background-color: #ffffff;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  cursor: pointer;
  transition: box-shadow 0.3s;
}

.activity-card:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
}

/* 卡片封面 */
.card-cover {
  position: relative;
  width: 100%;
  height: 150px;
}

.cover-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.status-tag {
  position: absolute;
  top: 12px;
  right: 12px;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  color: #ffffff;
}

.status-tag.ongoing {
  background-color: #10b981;
}

.status-tag.ended {
  background-color: #9ca3af;
}

/* 卡片内容 */
.card-content {
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.card-title {
  font-size: 16px;
  font-weight: bold;
  color: #1f2937;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-meta {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #6b7280;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

/* 卡片按钮 */
.card-btn {
  padding: 8px 16px;
  border-radius: 4px;
  border: none;
  font-size: 14px;
  cursor: pointer;
}

.btn-primary {
  background-color: #3b82f6;
  color: #ffffff;
}

.btn-secondary {
  background-color: #f3f4f6;
  color: #6b7280;
}
```

### 3.3 登录弹窗组件

```html
<template>
  <div class="modal-overlay" @click.self="close">
    <div class="login-modal">
      <button class="modal-close" @click="close">
        <i class="icon-close"></i>
      </button>

      <div class="modal-title">手机号登录</div>

      <div class="form-group">
        <label class="form-label">手机号</label>
        <input
          v-model="phone"
          type="text"
          placeholder="请输入手机号"
          class="form-input"
          maxlength="11"
        >
        <div v-if="phoneError" class="form-error">{{ phoneError }}</div>
      </div>

      <div class="form-group">
        <label class="form-label">验证码</label>
        <div class="code-input-group">
          <input
            v-model="code"
            type="text"
            placeholder="请输入验证码"
            class="form-input code-input"
            maxlength="6"
          >
          <button
            class="code-btn"
            :class="isCountingDown ? 'disabled' : ''"
            :disabled="isCountingDown"
            @click="sendCode"
          >
            {{ isCountingDown ? `${countdown}秒后重发` : '获取验证码' }}
          </button>
        </div>
        <div v-if="codeError" class="form-error">{{ codeError }}</div>
      </div>

      <button class="login-btn" :class="isSubmitting ? 'loading' : ''" @click="submitLogin">
        登录
      </button>

      <div class="form-tip">验证码5分钟内有效</div>
    </div>
  </div>
</template>
```

```css
/* 弹窗遮罩 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

/* 登录弹窗 */
.login-modal {
  width: 320px;
  padding: 24px;
  background-color: #ffffff;
  border-radius: 8px;
  position: relative;
}

/* 关闭按钮 */
.modal-close {
  position: absolute;
  top: 16px;
  right: 16px;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: transparent;
  border: none;
  color: #9ca3af;
  cursor: pointer;
}

/* 弹窗标题 */
.modal-title {
  font-size: 18px;
  font-weight: bold;
  color: #1f2937;
  margin-bottom: 24px;
  text-align: center;
}

/* 表单组 */
.form-group {
  margin-bottom: 16px;
}

.form-label {
  font-size: 14px;
  color: #6b7280;
  margin-bottom: 8px;
}

.form-input {
  width: 100%;
  padding: 12px;
  border: 1px solid #e5e7eb;
  border-radius: 4px;
  font-size: 14px;
}

.form-input:focus {
  border-color: #3b82f6;
  outline: none;
}

/* 验证码输入组 */
.code-input-group {
  display: flex;
  gap: 8px;
}

.code-input {
  flex: 1;
}

.code-btn {
  padding: 12px 16px;
  background-color: #3b82f6;
  color: #ffffff;
  border-radius: 4px;
  border: none;
  font-size: 14px;
  cursor: pointer;
}

.code-btn.disabled {
  background-color: #9ca3af;
  cursor: not-allowed;
}

/* 错误提示 */
.form-error {
  font-size: 12px;
  color: #ef4444;
  margin-top: 4px;
}

/* 登录按钮 */
.login-btn {
  width: 100%;
  padding: 12px;
  background-color: #3b82f6;
  color: #ffffff;
  border-radius: 4px;
  border: none;
  font-size: 14px;
  cursor: pointer;
  margin-top: 16px;
}

.login-btn.loading {
  background-color: #9ca3af;
  cursor: not-allowed;
}

/* 提示文字 */
.form-tip {
  font-size: 12px;
  color: #9ca3af;
  text-align: center;
  margin-top: 16px;
}

/* 移动端适配 */
@media (max-width: 768px) {
  .login-modal {
    width: 100%;
    max-width: 320px;
  }
}
```

### 3.4 活动详情页

```html
<template>
  <div class="activity-detail-page">
    <!-- 页面顶部 -->
    <header class="detail-header">
      <button class="back-btn" @click="goBack">
        <i class="icon-back"></i>
      </button>
      <h2 class="detail-title">{{ activity.title }}</h2>
    </header>

    <!-- 活动封面 -->
    <div class="activity-cover">
      <img :src="activity.coverImage" :alt="activity.title" class="cover-image">
    </div>

    <!-- 活动信息 -->
    <ActivityInfo :activity="activity" />

    <!-- 投票选项 -->
    <div class="options-section">
      <h3 class="section-title">投票选项</h3>
      <div class="options-list">
        <OptionCard
          v-for="option in activity.options"
          :key="option.id"
          :option="option"
          :selected="selectedOptionId === option.id"
          @select="selectOption"
        />
      </div>
    </div>

    <!-- 投票操作 -->
    <div class="vote-section">
      <div class="selected-info">
        <span v-if="selectedOptionId">已选择: {{ selectedOptionName }}</span>
        <span v-else>请选择投票选项</span>
      </div>
      <VoteButton :status="voteButtonStatus" @click="submitVote" />
      <div class="vote-rules">
        <h4>投票规则说明</h4>
        <ul>
          <li>每人仅可投票一次</li>
          <li>投票后不可更改</li>
          <li>活动结束后公开结果</li>
        </ul>
      </div>
    </div>

    <!-- Toast 提示 -->
    <Toast v-if="showToast" :message="toastMessage" :type="toastType" @close="closeToast" />
  </div>
</template>
```

```css
/* 详情页容器 */
.activity-detail-page {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  background-color: #f3f4f6;
}

/* 详情页头部 */
.detail-header {
  display: flex;
  align-items: center;
  padding: 16px;
  background-color: #ffffff;
  border-bottom: 1px solid #e5e7eb;
  position: sticky;
  top: 0;
  z-index: 100;
}

.back-btn {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: transparent;
  border: none;
  color: #6b7280;
  cursor: pointer;
}

.detail-title {
  flex: 1;
  font-size: 18px;
  font-weight: bold;
  color: #1f2937;
  text-align: center;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 活动封面 */
.activity-cover {
  width: 100%;
  aspect-ratio: 16 / 9;
  overflow: hidden;
}

.cover-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

/* 投票选项区域 */
.options-section {
  padding: 16px;
  background-color: #ffffff;
  margin-top: 12px;
}

.section-title {
  font-size: 16px;
  font-weight: bold;
  color: #1f2937;
  margin-bottom: 12px;
}

.options-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

/* 响应式布局 */
@media (min-width: 768px) {
  .options-list {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 16px;
  }
}

@media (min-width: 1024px) {
  .options-list {
    grid-template-columns: repeat(3, 1fr);
  }
}

/* 投票操作区域 */
.vote-section {
  padding: 16px;
  background-color: #ffffff;
  margin-top: 12px;
}

.selected-info {
  font-size: 14px;
  color: #6b7280;
  margin-bottom: 12px;
}

/* 投票规则 */
.vote-rules {
  margin-top: 16px;
  padding: 12px;
  background-color: #f3f4f6;
  border-radius: 4px;
}

.vote-rules h4 {
  font-size: 14px;
  color: #1f2937;
  margin-bottom: 8px;
}

.vote-rules ul {
  list-style: none;
  padding: 0;
}

.vote-rules li {
  font-size: 12px;
  color: #6b7280;
  margin-bottom: 4px;
  padding-left: 16px;
  position: relative;
}

.vote-rules li::before {
  content: '•';
  position: absolute;
  left: 0;
  color: #3b82f6;
}
```

### 3.5 投票结果页

```html
<template>
  <div class="result-page">
    <!-- 页面顶部 -->
    <header class="result-header">
      <button class="back-btn" @click="goBack">
        <i class="icon-back"></i>
      </button>
      <h2 class="result-title">投票结果</h2>
    </header>

    <!-- 结果统计概览 -->
    <div class="stats-overview">
      <div class="stat-item">
        <span class="stat-label">参与总人数</span>
        <span class="stat-value">{{ result.totalVotes }}人</span>
      </div>
      <div class="stat-item">
        <span class="stat-label">活动时间</span>
        <span class="stat-value">{{ activity.startTime }} 至 {{ activity.endTime }}</span>
      </div>
    </div>

    <!-- 结果排名 -->
    <div class="ranking-section">
      <h3 class="section-title">投票排名</h3>
      <div class="ranking-list">
        <VoteResult
          v-for="(option, index) in sortedOptions"
          :key="option.id"
          :option="option"
          :rank="index + 1"
        />
      </div>
    </div>

    <!-- 数据可视化 -->
    <div class="chart-section">
      <h3 class="section-title">数据可视化</h3>
      <div class="chart-tabs">
        <button class="chart-tab active" @click="switchChart('bar')">柱状图</button>
        <button class="chart-tab" @click="switchChart('pie')">饼图</button>
      </div>
      <ResultChart :type="chartType" :data="chartData" />
    </div>
  </div>
</template>
```

```css
/* 结果页容器 */
.result-page {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  background-color: #f3f4f6;
}

/* 结果页头部 */
.result-header {
  display: flex;
  align-items: center;
  padding: 16px;
  background-color: #ffffff;
  border-bottom: 1px solid #e5e7eb;
  position: sticky;
  top: 0;
  z-index: 100;
}

/* 统计概览 */
.stats-overview {
  display: flex;
  justify-content: space-around;
  padding: 16px;
  background-color: #ffffff;
  margin-top: 12px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.stat-label {
  font-size: 12px;
  color: #6b7280;
}

.stat-value {
  font-size: 16px;
  font-weight: bold;
  color: #1f2937;
}

/* 排名区域 */
.ranking-section {
  padding: 16px;
  background-color: #ffffff;
  margin-top: 12px;
}

.ranking-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

/* 图表区域 */
.chart-section {
  padding: 16px;
  background-color: #ffffff;
  margin-top: 12px;
}

.chart-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}

.chart-tab {
  padding: 8px 16px;
  background-color: #f3f4f6;
  border-radius: 4px;
  border: none;
  font-size: 14px;
  color: #6b7280;
  cursor: pointer;
}

.chart-tab.active {
  background-color: #3b82f6;
  color: #ffffff;
}
```

---

## 4. 管理端页面 HTML 结构

### 4.1 管理员登录页

```html
<template>
  <div class="admin-login-page">
    <div class="login-container">
      <div class="login-title">管理后台登录</div>

      <div class="form-group">
        <label class="form-label">用户名</label>
        <input
          v-model="username"
          type="text"
          placeholder="请输入用户名"
          class="form-input"
        >
      </div>

      <div class="form-group">
        <label class="form-label">密码</label>
        <input
          v-model="password"
          type="password"
          placeholder="请输入密码"
          class="form-input"
        >
      </div>

      <div class="form-checkbox">
        <input v-model="rememberMe" type="checkbox" class="checkbox-input">
        <label class="checkbox-label">记住密码</label>
      </div>

      <button class="login-btn" @click="submitLogin">登录</button>

      <div v-if="errorMessage" class="error-message">{{ errorMessage }}</div>
    </div>
  </div>
</template>
```

```css
/* 登录页容器 */
.admin-login-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background-color: #f3f4f6;
}

/* 登录容器 */
.login-container {
  width: 400px;
  padding: 32px;
  background-color: #ffffff;
  border-radius: 8px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
}

/* 登录标题 */
.login-title {
  font-size: 24px;
  font-weight: bold;
  color: #1f2937;
  text-align: center;
  margin-bottom: 32px;
}

/* 表单组 */
.form-group {
  margin-bottom: 16px;
}

/* 复选框 */
.form-checkbox {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 24px;
}

.checkbox-input {
  width: 16px;
  height: 16px;
}

.checkbox-label {
  font-size: 14px;
  color: #6b7280;
}

/* 登录按钮 */
.login-btn {
  width: 100%;
  padding: 12px;
  background-color: #3b82f6;
  color: #ffffff;
  border-radius: 4px;
  border: none;
  font-size: 16px;
  cursor: pointer;
}

/* 错误消息 */
.error-message {
  font-size: 14px;
  color: #ef4444;
  text-align: center;
  margin-top: 16px;
}
```

### 4.2 后台首页

```html
<template>
  <div class="admin-dashboard-page">
    <!-- 页面顶部 -->
    <header class="admin-header">
      <div class="logo">投票系统管理后台</div>
      <div class="admin-info">
        <span class="admin-name">{{ adminName }}</span>
        <button class="logout-btn" @click="logout">退出登录</button>
      </div>
    </header>

    <!-- 主体内容 -->
    <div class="dashboard-body">
      <!-- 左侧导航 -->
      <aside class="sidebar">
        <nav class="sidebar-nav">
          <div class="nav-group">
            <div class="nav-group-title">活动管理</div>
            <div class="nav-item active">
              <i class="icon-list"></i>
              <span>活动列表</span>
            </div>
            <div class="nav-item">
              <i class="icon-add"></i>
              <span>创建活动</span>
            </div>
          </div>
          <div class="nav-group">
            <div class="nav-group-title">数据统计</div>
            <div class="nav-item">
              <i class="icon-chart"></i>
              <span>数据统计</span>
            </div>
          </div>
          <div class="nav-group">
            <div class="nav-group-title">系统管理</div>
            <div class="nav-item">
              <i class="icon-setting"></i>
              <span>系统配置</span>
            </div>
          </div>
        </nav>
      </aside>

      <!-- 内容区域 -->
      <main class="dashboard-content">
        <div class="content-header">
          <h2 class="content-title">后台首页</h2>
        </div>

        <!-- 统计概览 -->
        <StatsOverview :stats="dashboardStats" />

        <!-- 最近活动 -->
        <RecentActivities :activities="recentActivities" />
      </main>
    </div>
  </div>
</template>
```

```css
/* 后台首页容器 */
.admin-dashboard-page {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  background-color: #f3f4f6;
}

/* 管理端头部 */
.admin-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  background-color: #ffffff;
  border-bottom: 1px solid #e5e7eb;
}

.logo {
  font-size: 18px;
  font-weight: bold;
  color: #1f2937;
}

.admin-info {
  display: flex;
  align-items: center;
  gap: 16px;
}

.admin-name {
  font-size: 14px;
  color: #6b7280;
}

.logout-btn {
  padding: 8px 16px;
  background-color: #f3f4f6;
  border-radius: 4px;
  border: none;
  font-size: 14px;
  color: #6b7280;
  cursor: pointer;
}

/* 主体内容 */
.dashboard-body {
  display: flex;
  flex: 1;
}

/* 左侧导航 */
.sidebar {
  width: 240px;
  background-color: #ffffff;
  border-right: 1px solid #e5e7eb;
  padding: 16px 0;
}

.sidebar-nav {
  display: flex;
  flex-direction: column;
}

.nav-group {
  margin-bottom: 24px;
}

.nav-group-title {
  padding: 8px 24px;
  font-size: 12px;
  color: #9ca3af;
  font-weight: bold;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 24px;
  color: #6b7280;
  cursor: pointer;
  transition: background-color 0.3s;
}

.nav-item:hover {
  background-color: #f3f4f6;
}

.nav-item.active {
  background-color: #3b82f6;
  color: #ffffff;
}

/* 内容区域 */
.dashboard-content {
  flex: 1;
  padding: 24px;
}

.content-header {
  margin-bottom: 24px;
}

.content-title {
  font-size: 24px;
  font-weight: bold;
  color: #1f2937;
}
```

### 4.3 活动列表页

```html
<template>
  <div class="activity-list-page">
    <div class="page-header">
      <h2 class="page-title">活动管理</h2>
      <button class="create-btn" @click="goToCreate">创建活动</button>
    </div>

    <!-- 筛选搜索 -->
    <div class="filter-section">
      <div class="filter-tabs">
        <button class="filter-tab active">全部</button>
        <button class="filter-tab">草稿</button>
        <button class="filter-tab">未开始</button>
        <button class="filter-tab">进行中</button>
        <button class="filter-tab">已结束</button>
      </div>
      <input
        v-model="searchKeyword"
        type="text"
        placeholder="搜索活动标题"
        class="search-input"
      >
    </div>

    <!-- 活动表格 -->
    <div class="activity-table">
      <table class="table">
        <thead class="table-head">
          <tr>
            <th>活动标题</th>
            <th>状态</th>
            <th>开始时间</th>
            <th>结束时间</th>
            <th>参与人数</th>
            <th>创建时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody class="table-body">
          <tr v-for="activity in activities" :key="activity.id">
            <td>{{ activity.title }}</td>
            <td>
              <span class="status-tag" :class="activity.status">{{ statusText }}</span>
            </td>
            <td>{{ activity.startTime }}</td>
            <td>{{ activity.endTime }}</td>
            <td>{{ activity.participantCount }}</td>
            <td>{{ activity.createdAt }}</td>
            <td>
              <div class="action-buttons">
                <button class="action-btn" @click="editActivity(activity)">编辑</button>
                <button class="action-btn" @click="deleteActivity(activity)">删除</button>
                <button class="action-btn" @click="publishActivity(activity)">发布</button>
                <button class="action-btn" @click="viewStats(activity)">统计</button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 分页 -->
    <Pagination :total="total" :page="currentPage" :size="pageSize" @change="onPageChange" />
  </div>
</template>
```

```css
/* 活动列表页容器 */
.activity-list-page {
  padding: 24px;
}

/* 页面头部 */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.page-title {
  font-size: 24px;
  font-weight: bold;
  color: #1f2937;
}

.create-btn {
  padding: 12px 24px;
  background-color: #3b82f6;
  color: #ffffff;
  border-radius: 4px;
  border: none;
  font-size: 14px;
  cursor: pointer;
}

/* 筛选搜索 */
.filter-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.filter-tabs {
  display: flex;
  gap: 8px;
}

.filter-tab {
  padding: 8px 16px;
  background-color: #f3f4f6;
  border-radius: 4px;
  border: none;
  font-size: 14px;
  color: #6b7280;
  cursor: pointer;
}

.filter-tab.active {
  background-color: #3b82f6;
  color: #ffffff;
}

.search-input {
  width: 240px;
  padding: 8px 12px;
  border: 1px solid #e5e7eb;
  border-radius: 4px;
  font-size: 14px;
}

/* 活动表格 */
.activity-table {
  background-color: #ffffff;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.table {
  width: 100%;
  border-collapse: collapse;
}

.table-head {
  background-color: #f3f4f6;
}

.table-head th {
  padding: 12px 16px;
  font-size: 14px;
  font-weight: bold;
  color: #6b7280;
  text-align: left;
  border-bottom: 1px solid #e5e7eb;
}

.table-body td {
  padding: 16px;
  font-size: 14px;
  color: #1f2937;
  border-bottom: 1px solid #e5e7eb;
}

/* 状态标签 */
.status-tag {
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
}

.status-tag.draft {
  background-color: #9ca3af;
  color: #ffffff;
}

.status-tag.ongoing {
  background-color: #10b981;
  color: #ffffff;
}

.status-tag.ended {
  background-color: #6b7280;
  color: #ffffff;
}

/* 操作按钮 */
.action-buttons {
  display: flex;
  gap: 8px;
}

.action-btn {
  padding: 6px 12px;
  background-color: #f3f4f6;
  border-radius: 4px;
  border: none;
  font-size: 12px;
  color: #6b7280;
  cursor: pointer;
}

.action-btn:hover {
  background-color: #e5e7eb;
}
```

---

## 5. CSS 类名规范

### 5.1 命名规范

**页面级类名**
- 格式: `xxx-page` (如 `home-page`, `activity-detail-page`)
- 说明: 页面最外层容器

**区块级类名**
- 格式: `xxx-section` (如 `filter-section`, `vote-section`)
- 说明: 页面内的主要区块

**组件级类名**
- 格式: `xxx-card`, `xxx-btn`, `xxx-modal` (如 `activity-card`, `login-btn`)
- 说明: 可复用的组件

**状态类名**
- 格式: `xxx-state` (如 `active`, `disabled`, `loading`)
- 说明: 表示元素的状态

**修饰类名**
- 格式: `xxx-yyy` (如 `btn-primary`, `btn-secondary`)
- 说明: 对基础类名的修饰

### 5.2 常用类名库

**布局类**
```
.container      - 容器
.wrapper        - 包装器
.header         - 头部
.footer         - 底部
.sidebar        - 侧边栏
.main           - 主体内容
.content        - 内容区域
```

**组件类**
```
.card           - 卡片
.btn            - 按钮
.btn-primary    - 主要按钮
.btn-secondary  - 次要按钮
.btn-disabled   - 禁用按钮
.input          - 输入框
.label          - 标签
.icon           - 图标
.modal          - 弹窗
.toast          - 提示
.loading        - 加载
```

**状态类**
```
.active         - 激活状态
.disabled       - 禁用状态
.loading        - 加载状态
.hidden         - 隐藏状态
.visible        - 可见状态
.error          - 错误状态
.success        - 成功状态
```

**文本类**
```
.title          - 标题
.subtitle       - 子标题
.text           - 文本
.description    - 描述
.label          - 标签文字
.value          - 值文字
.tip            - 提示文字
.error-message  - 错误消息
```

---

## 6. 待确认问题

### 6.1 技术选型相关
1. **UI框架**: 使用哪个 UI 框架（Ant Design Vue、Element Plus、Vuetify）？
2. **CSS方案**: 使用哪种 CSS 方案（CSS Modules、Styled Components、Tailwind CSS）？
3. **图标库**: 使用哪个图标库（Font Awesome、Iconfont、Heroicons）？
4. **图表库**: 使用哪个图表库（ECharts、Chart.js、D3.js）？

### 6.2 布局细节相关
1. **页面边距**: 移动端和PC端的页面边距具体数值？
2. **卡片尺寸**: 活动卡片和选项卡片的具体尺寸？
3. **图片尺寸**: 封面图和选项图片的具体尺寸和比例？
4. **字体大小**: 各种文字的具体字号范围？

### 6.3 响应式设计相关
1. **断点设计**: 具体的响应式断点数值？
2. **布局切换**: 不同屏幕尺寸下的布局切换规则？
3. **图片适配**: 不同屏幕尺寸下的图片适配方案？
4. **交互适配**: 不同屏幕尺寸下的交互适配方案？

### 6.4 性能优化相关
1. **图片懒加载**: 是否需要图片懒加载？
2. **虚拟列表**: 活动列表是否需要虚拟列表？
3. **预加载策略**: 是否需要预加载策略？
4. **缓存策略**: 是否需要页面缓存？

---

**文档变更记录**

| 版本 | 日期 | 修改人 | 修改内容 |
|------|------|--------|---------|
| v1.0 | 2026-04-27 | Product Agent | 初始版本创建 |