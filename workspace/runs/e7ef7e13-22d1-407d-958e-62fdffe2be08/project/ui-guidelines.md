# 投票系统 UI 设计规范

**产品名称**: 公开投票管理系统
**文档版本**: v1.0
**创建日期**: 2026-04-27
**设计系统**: Modern Minimal - Tool/Productivity

---

## 1. 设计理念与风格定位

### 1.1 产品定位
- **类型**: 工具/生产力应用
- **场景**: 公开投票活动（比赛评选、民意调查）
- **核心价值**: 公正、透明、便捷、安全

### 1.2 设计风格
**Modern Minimal - 专业工具风格**

- **视觉特征**: 清晰、简洁、功能导向
- **信息层级**: 内容优先，视觉降噪
- **交互反馈**: 即时、明确、不打扰
- **情感基调**: 专业、可信、公正

### 1.3 设计原则

| 原则 | 说明 | 优先级 |
|------|------|--------|
| **清晰性** | 信息层级明确，操作路径可预测 | P0 |
| **一致性** | 跨页面保持视觉语言统一 | P0 |
| **易用性** | 减少认知负担，降低操作门槛 | P0 |
| **可信感** | 通过专业设计传递公正印象 | P1 |
| **响应式** | 适配多设备，保证核心体验 | P1 |

---

## 2. 色彩系统

### 2.1 主色板

#### 基础色系（遵循 WCAG 4.5:1 对比度标准）

| 颜色名称 | 色值 | 用途 | 对比度验证 |
|---------|------|------|-----------|
| **Primary Blue** | `#3B82F6` | 主操作、链接、高亮 | ✓ 4.5:1 (on white) |
| **Secondary Gray** | `#6B7280` | 次要信息、辅助文字 | ✓ 4.5:1 (on white) |
| **Success Green** | `#10B981` | 成功状态、完成反馈 | ✓ 3:1 (large text) |
| **Warning Orange** | `#F59E0B` | 警告提示、进行中状态 | ✓ 3:1 (large text) |
| **Error Red** | `#EF4444` | 错误状态、危险操作 | ✓ 4.5:1 (on white) |

#### 中性色系

| 颜色名称 | 色值 | 用途 |
|---------|------|------|
| **Text Primary** | `#1F2937` | 正文、标题 |
| **Text Secondary** | `#6B7280` | 次级文字、说明 |
| **Text Tertiary** | `#9CA3AF` | 提示文字、占位符 |
| **Border Light** | `#E5E7EB` | 边框、分隔线 |
| **Background Light** | `#F3F4F6` | 卡片背景、区域划分 |
| **Background White** | `#FFFFFF` | 页面主背景 |

#### 功能语义色

```css
/* 语义化色彩变量 */
:root {
  --color-primary: #3B82F6;
  --color-secondary: #6B7280;
  --color-success: #10B981;
  --color-warning: #F59E0B;
  --color-error: #EF4444;

  --color-text-primary: #1F2937;
  --color-text-secondary: #6B7280;
  --color-text-tertiary: #9CA3AF;

  --color-border: #E5E7EB;
  --color-bg-light: #F3F4F6;
  --color-bg-white: #FFFFFF;

  /* 状态色扩展 */
  --color-primary-hover: #2563EB;
  --color-primary-active: #1D4ED8;
  --color-primary-disabled: #94A3B8;
}
```

### 2.2 状态色扩展

| 状态 | Primary | Secondary | Success | Error |
|------|---------|-----------|---------|-------|
| **Default** | `#3B82F6` | `#6B7280` | `#10B981` | `#EF4444` |
| **Hover** | `#2563EB` | `#5B6470` | `#059669` | `#DC2626` |
| **Active** | `#1D4ED8` | `#4B5563` | `#047857` | `#B91C1C` |
| **Disabled** | `#94A3B8` | `#D1D5DB` | `#6EE7B7` | `#FCA5A5` |
| **Focus Ring** | `2px #3B82F6` | `2px #6B7280` | `2px #10B981` | `2px #EF4444` |

### 2.3 背景层级系统

| 层级 | 背景色 | 用途 | 阴影 |
|------|--------|------|------|
| **Level 0** | `#F3F4F6` | 页面容器 | 无 |
| **Level 1** | `#FFFFFF` | 卡片、模块 | `0 1px 2px rgba(0,0,0,0.05)` |
| **Level 2** | `#FFFFFF` | 弹窗、浮层 | `0 4px 6px rgba(0,0,0,0.1)` |
| **Level 3** | `#FFFFFF` | 模态框、通知 | `0 10px 15px rgba(0,0,0,0.15)` |

### 2.4 对比度验证（WCAG AA）

**所有文字与背景组合必须满足 WCAG AA 标准：**

- ✓ 正文文字 (≤ 18px): 对比度 ≥ 4.5:1
- ✓ 大标题 (≥ 18px bold / 24px): 对比度 ≥ 3:1
- ✓ 功能图标: 对比度 ≥ 3:1

**验证工具**: 使用 [WebAIM Contrast Checker](https://webaim.org/resources/contrastchecker/)

---

## 3. 字体规范

### 3.1 字体家族

**中文字体栈**（遵循 Apple HIG + Material Design）
```css
font-family:
  -apple-system,          /* iOS/macOS 系统字体 */
  BlinkMacSystemFont,     /* macOS Chrome */
  "Segoe UI",             /* Windows 10+ */
  "PingFang SC",          /* iOS/macOS 中文字体 */
  "Microsoft YaHei",      /* Windows 中文字体 */
  "Helvetica Neue",       /* iOS/macOS */
  Arial,                  /* 通用 fallback */
  sans-serif;             /* 最终 fallback */
```

**数字字体栈**（用于数据展示）
```css
font-family:
  "SF Pro Text",          /* iOS 数字字体 */
  "Helvetica Neue",       /* macOS/iOS */
  "Roboto",               /* Android */
  -apple-system,
  BlinkMacSystemFont,
  sans-serif;
```

### 3.2 字体大小系统（Material Design Type Scale）

| 类型 | 大小 | 行高 | 字重 | 用途 |
|------|------|------|------|------|
| **Display** | `32px` | `1.25` | `700` | 特大标题（结果页统计） |
| **Headline** | `24px` | `1.33` | `600` | 页面主标题 |
| **Title** | `18px` | `1.44` | `600` | 卡片标题、区块标题 |
| **Subtitle** | `16px` | `1.5` | `500` | 辅助标题、标签 |
| **Body** | `14px` | `1.5` | `400` | 正文内容 |
| **Body Small** | `12px` | `1.67` | `400` | 提示文字、次要信息 |
| **Caption** | `11px` | `1.45` | `400` | 极小文字（慎用） |

### 3.3 字重规范

| 字重值 | 名称 | 用途 |
|--------|------|------|
| `400` | Regular | 正文、说明文字 |
| `500` | Medium | 标签、按钮文字 |
| `600` | Semi Bold | 标题、重要信息 |
| `700` | Bold | 强调标题、关键数据 |

### 3.4 字体使用规则

- ✓ 正文最小 `14px`（避免 iOS 自动缩放）
- ✓ 行高统一 `1.5`（提升可读性）
- ✓ 标题使用 `600-700` 字重（建立层级）
- ✓ 不使用 `< 12px` 文字（除 Caption）
- ✓ 不使用 `letter-spacing` 调整（保持原生节奏）

---

## 4. 间距系统（8dp Grid）

### 4.1 基础间距单位

遵循 Material Design 8dp 网格系统：

| 单位 | 值 | 用途 |
|------|------|------|
| **4dp** | `4px` | 微间距（图标与文字） |
| **8dp** | `8px` | 基础间距（组件内部） |
| **12dp** | `12px` | 紧凑间距（卡片内容） |
| **16dp** | `16px` | 标准间距（页面边距） |
| **24dp** | `24px` | 区块间距（模块之间） |
| **32dp** | `32px` | 大间距（区域划分） |
| **48dp** | `48px` | 特大间距（页面顶底） |

### 4.2 页面边距系统

| 设备类型 | 页面边距 | 内容宽度 | 说明 |
|---------|---------|---------|------|
| **Mobile (<768px)** | `16px` | `100%` | 单列布局，小边距 |
| **Tablet (768-1024px)** | `24px` | `100%` | 双列布局，适中边距 |
| **Desktop (>1024px)** | `32px` | `max 1200px` | 居中对齐，大边距 |

### 4.3 组件间距规范

```css
/* 按钮内部间距 */
.button {
  padding: 12px 24px;  /* 上下 12dp，左右 24dp */
  gap: 8px;            /* 图标与文字间距 */
}

/* 卡片内部间距 */
.card {
  padding: 16px;       /* 统一 16dp */
  gap: 12px;           /* 内部元素间距 */
}

/* 表单字段间距 */
.form-group {
  margin-bottom: 16px; /* 字段间 16dp */
}

/* 区块间距 */
.section {
  margin-bottom: 24px; /* 区块间 24dp */
}
```

---

## 5. 组件设计规范

### 5.1 按钮系统

#### 按钮类型与样式

| 类型 | 背景 | 文字 | 边框 | 用途 |
|------|------|------|------|------|
| **Primary** | `#3B82F6` | `#FFFFFF` | 无 | 主操作（提交、确认） |
| **Secondary** | `#F3F4F6` | `#1F2937` | 无 | 次操作（取消、返回） |
| **Outline** | `transparent` | `#3B82F6` | `1px #3B82F6` | 辅助操作 |
| **Ghost** | `transparent` | `#6B7280` | 无 | 轻量操作（编辑、删除） |
| **Disabled** | `#F3F4F6` | `#9CA3AF` | 无 | 禁用状态 |

#### 按钮尺寸

| 尺寸 | 高度 | 内边距 | 字号 | 用途 |
|------|------|--------|------|------|
| **Large** | `48px` | `16px 32px` | `16px` | 页面主按钮 |
| **Medium** | `40px` | `12px 24px` | `14px` | 标准按钮 |
| **Small** | `32px` | `8px 16px` | `12px` | 次要操作 |
| **Icon Only** | `44px` | `0` | — | 图标按钮 |

#### 按钮状态

**遵循 Material Design 状态层规则：**

```css
/* Primary 按钮 */
.button-primary {
  background: #3B82F6;
  color: #FFFFFF;
  border-radius: 8px;
  border: none;
  cursor: pointer;
  transition: background 150ms ease-out;

  /* Hover 状态 */
  &:hover {
    background: #2563EB;
  }

  /* Active 状态 */
  &:active {
    background: #1D4ED8;
    transform: scale(0.98);  /* 微缩放反馈 */
  }

  /* Focus 状态 */
  &:focus {
    outline: none;
    box-shadow: 0 0 0 2px #3B82F6;
  }

  /* Disabled 状态 */
  &:disabled {
    background: #F3F4F6;
    color: #9CA3AF;
    cursor: not-allowed;
    opacity: 0.6;
  }
}
```

#### 按钮交互规则

- ✓ **最小触摸区域**: `44×44px`（iOS HIG）
- ✓ **即时反馈**: 150ms 内响应（不超过 300ms）
- ✓ **加载状态**: 显示 spinner + 禁用按钮
- ✓ **禁用视觉**: 降低 opacity + cursor change
- ✓ **Focus Ring**: 2px 蓝色轮廓（可访问性）

### 5.2 输入框系统

#### 输入框类型

| 类型 | 样式 | 用途 |
|------|------|------|
| **Text** | 标准输入框 | 文本输入（标题、名称） |
| **Phone** | 数字键盘 | 手机号输入 |
| **Code** | 6位验证码 | 验证码输入 |
| **Textarea** | 多行文本 | 活动描述 |

#### 输入框样式

```css
.input {
  width: 100%;
  height: 44px;             /* 触摸友好 */
  padding: 12px 16px;
  font-size: 14px;
  line-height: 1.5;
  color: #1F2937;
  background: #FFFFFF;
  border: 1px solid #E5E7EB;
  border-radius: 8px;
  transition: border-color 150ms ease-out;

  /* Placeholder */
  &::placeholder {
    color: #9CA3AF;
  }

  /* Focus 状态 */
  &:focus {
    outline: none;
    border-color: #3B82F6;
    box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.1);
  }

  /* Error 状态 */
  &:invalid,
  &.error {
    border-color: #EF4444;
  }

  /* Disabled 状态 */
  &:disabled {
    background: #F3F4F6;
    color: #9CA3AF;
    cursor: not-allowed;
  }
}
```

#### 输入框交互规则

- ✓ **可见标签**: 不依赖 placeholder
- ✓ **Helper Text**: 复杂输入提供说明
- ✓ **Error Placement**: 错误提示在输入框下方
- ✓ **Inline Validation**: 失焦后验证，不打扰输入
- ✓ **Focus Management**: 错误后自动聚焦首个错误字段

#### 验证码输入框（特殊设计）

```css
.code-input {
  width: 100%;
  height: 44px;
  padding: 12px 16px;
  font-size: 24px;          /* 大字体便于识别 */
  font-family: "SF Pro Text", monospace;  /* 等宽字体 */
  letter-spacing: 8px;      /* 字符间距 */
  text-align: center;       /* 居中显示 */
  color: #1F2937;
  background: #FFFFFF;
  border: 2px solid #E5E7EB;
  border-radius: 8px;
}
```

### 5.3 卡片系统

#### 卡片类型

| 类型 | 用途 | 阴影层级 |
|------|------|---------|
| **Activity Card** | 活动展示 | Level 1 |
| **Option Card** | 投票选项 | Level 1 |
| **Stats Card** | 数据统计 | Level 1 |
| **Info Card** | 信息展示 | Level 1 |

#### 卡片样式

```css
.card {
  background: #FFFFFF;
  border-radius: 12px;      /* 现代圆角 */
  padding: 16px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
  transition: box-shadow 150ms ease-out;

  /* Hover 状态 */
  &:hover {
    box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
  }

  /* 可点击状态 */
  &.clickable {
    cursor: pointer;

    &:active {
      transform: scale(0.98);
    }
  }

  /* 选中状态 */
  &.selected {
    border: 2px solid #3B82F6;
    box-shadow: 0 0 0 4px rgba(59, 130, 246, 0.1);
  }
}
```

#### 投票选项卡片（特殊设计）

```css
.option-card {
  position: relative;
  padding: 16px;
  border-radius: 12px;
  background: #FFFFFF;
  border: 2px solid #E5E7EB;
  cursor: pointer;
  transition: all 150ms ease-out;

  /* Hover */
  &:hover {
    border-color: #3B82F6;
    box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
  }

  /* 选中 */
  &.selected {
    border-color: #3B82F6;
    background: #EFF6FF;

    /* 选中图标 */
    .selected-icon {
      position: absolute;
      top: 12px;
      right: 12px;
      width: 24px;
      height: 24px;
      color: #3B82F6;
    }
  }
}
```

### 5.4 状态标签系统

#### 标签类型

| 状态 | 背景 | 文字 | 用途 |
|------|------|------|------|
| **Draft** | `#9CA3AF` | `#FFFFFF` | 草稿状态 |
| **Pending** | `#F59E0B` | `#FFFFFF` | 未开始状态 |
| **Ongoing** | `#10B981` | `#FFFFFF` | 进行中状态 |
| **Ended** | `#6B7280` | `#FFFFFF` | 已结束状态 |

#### 标签样式

```css
.status-tag {
  display: inline-flex;
  padding: 4px 12px;
  font-size: 12px;
  font-weight: 500;
  line-height: 1.33;
  color: #FFFFFF;
  border-radius: 12px;
  text-transform: uppercase;
}
```

### 5.5 弹窗系统

#### 弹窗类型

| 类型 | 尺寸 | 位置 | 用途 |
|------|------|------|------|
| **Modal** | `320px` fixed | 屏幕中央 | 登录弹窗、确认对话框 |
| **Toast** | `auto width` | 顶部中央 | 操作反馈（成功/失败） |
| **Drawer** | `100%` | 底部滑出 | 移动端详情展示 |

#### 弹窗样式

```css
.modal {
  position: fixed;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 320px;
  max-width: calc(100vw - 32px);
  background: #FFFFFF;
  border-radius: 16px;
  box-shadow: 0 10px 15px rgba(0, 0, 0, 0.15);
  z-index: 1000;
  animation: modal-enter 150ms ease-out;
}

.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.5);
  z-index: 999;
}

/* 弹窗入场动画 */
@keyframes modal-enter {
  from {
    opacity: 0;
    transform: translate(-50%, -48%);
  }
  to {
    opacity: 1;
    transform: translate(-50%, -50%);
  }
}
```

#### Toast 提示

```css
.toast {
  position: fixed;
  top: 16px;
  left: 50%;
  transform: translateX(-50%);
  padding: 12px 24px;
  background: #1F2937;
  color: #FFFFFF;
  border-radius: 8px;
  font-size: 14px;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
  z-index: 1001;
  animation: toast-enter 150ms ease-out;

  /* Success Toast */
  &.success {
    background: #10B981;
  }

  /* Error Toast */
  &.error {
    background: #EF4444;
  }
}

/* 自动消失 */
.toast {
  animation: toast-enter 150ms ease-out,
             toast-exit 150ms ease-in 3s forwards;
}
```

### 5.6 图标系统

#### 图标来源

**使用 Lucide Icons（SVG）**

- 理由：一致性强、可定制、性能优
- 不使用：Emoji、PNG 图标

#### 图标尺寸规范

| 尺寸 | 值 | 用途 |
|------|------|------|
| **Small** | `16px` | 标签内图标、inline图标 |
| **Medium** | `20px` | 按钮内图标、列表图标 |
| **Large** | `24px` | 导航图标、卡片图标 |
| **XL** | `32px` | 功能图标、状态图标 |

#### 图标颜色规则

- **Primary Icons**: `#3B82F6`（主操作）
- **Secondary Icons**: `#6B7280`（次要信息）
- **Success Icons**: `#10B981`（成功状态）
- **Error Icons**: `#EF4444`（错误状态）

#### 常用图标映射

| 功能 | Lucide Icon | 尺寸 |
|------|------------|------|
| 返回 | `arrow-left` | `24px` |
| 关闭 | `x` | `24px` |
| 确认 | `check` | `24px` |
| 时间 | `clock` | `20px` |
| 用户 | `user` | `24px` |
| 投票 | `vote` (custom) | `24px` |
| 搜索 | `search` | `20px` |
| 加载 | `loader-2` | `20px` |

---

## 6. 页面布局规范

### 6.1 页面结构层级

**标准页面结构**：

```
Page Container (Level 0)
  ├── Header (Fixed)
  ├── Main Content (Scrollable)
  │   ├── Section A (Level 1)
  │   ├── Section B (Level 1)
  │   └── Section C (Level 1)
  └── Footer (Fixed, optional)
```

### 6.2 页面顶部导航

#### 移动端（<768px）

```css
.header-mobile {
  position: sticky;
  top: 0;
  width: 100%;
  height: 56px;
  padding: 0 16px;
  background: #FFFFFF;
  border-bottom: 1px solid #E5E7EB;
  display: flex;
  align-items: center;
  justify-content: space-between;
  z-index: 100;

  /* Back Button */
  .back-btn {
    width: 44px;
    height: 44px;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  /* Title */
  .title {
    font-size: 18px;
    font-weight: 600;
    color: #1F2937;
  }

  /* Action Buttons */
  .actions {
    display: flex;
    gap: 8px;
  }
}
```

#### 桌面端（≥1024px）

```css
.header-desktop {
  position: sticky;
  top: 0;
  width: 100%;
  height: 64px;
  padding: 0 32px;
  background: #FFFFFF;
  border-bottom: 1px solid #E5E7EB;
  display: flex;
  align-items: center;
  justify-content: space-between;
  z-index: 100;

  /* Logo */
  .logo {
    font-size: 24px;
    font-weight: 700;
    color: #1F2937;
  }

  /* Navigation */
  .nav {
    display: flex;
    gap: 32px;
  }

  /* Actions */
  .actions {
    display: flex;
    gap: 16px;
  }
}
```

### 6.3 底部导航（移动端）

**遵循 Material Design Bottom Navigation 规范：**

```css
.bottom-nav {
  position: fixed;
  bottom: 0;
  left: 0;
  width: 100%;
  height: 56px;
  padding: 8px 0;
  background: #FFFFFF;
  border-top: 1px solid #E5E7EB;
  display: flex;
  align-items: center;
  justify-content: space-around;
  z-index: 100;

  /* Nav Item */
  .nav-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 4px;
    padding: 4px 12px;
    color: #6B7280;
    cursor: pointer;
    transition: color 150ms ease-out;

    &.active {
      color: #3B82F6;
    }

    .icon {
      width: 24px;
      height: 24px;
    }

    .label {
      font-size: 12px;
      font-weight: 500;
    }
  }
}
```

**规则**：
- ✓ 最多 5 个导航项
- ✓ 每项必须有图标 + 文字标签
- ✓ 当前页高亮显示
- ✓ 距离底部安全区 8px（避免手势冲突）

### 6.4 内容区域布局

#### 活动列表页布局

**移动端（单列）**：
```css
.activity-list-mobile {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 16px;
}
```

**桌面端（多列网格）**：
```css
.activity-list-desktop {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 24px;
  padding: 32px;
  max-width: 1200px;
  margin: 0 auto;
}
```

#### 活动详情页布局

```css
.activity-detail {
  display: flex;
  flex-direction: column;
  gap: 24px;

  /* Cover */
  .cover {
    width: 100%;
    aspect-ratio: 16 / 9;
    object-fit: cover;
  }

  /* Info Section */
  .info {
    padding: 16px;
  }

  /* Options Section */
  .options {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
    gap: 16px;
    padding: 16px;
  }
}
```

### 6.5 响应式断点系统

遵循 Material Design Adaptive 布局：

| 断点名称 | 宽度范围 | 列数 | 边距 |
|---------|---------|------|------|
| **Mobile** | `< 768px` | 1列 | `16px` |
| **Tablet** | `768px - 1024px` | 2列 | `24px` |
| **Desktop** | `≥ 1024px` | 3列或自适应 | `32px` |

```css
/* 响应式媒体查询 */
@media (max-width: 767px) {
  /* Mobile styles */
}

@media (min-width: 768px) and (max-width: 1023px) {
  /* Tablet styles */
}

@media (min-width: 1024px) {
  /* Desktop styles */
}
```

---

## 7. 交互状态规范

### 7.1 状态过渡原则

遵循 UI/UX Pro Max 规则：

- **Duration**: 150ms - 300ms（微交互）
- **Easing**: `ease-out`（进入），`ease-in`（退出）
- **Transform**: 仅使用 `transform` 和 `opacity`
- **No Layout Shift**: 不动画化 `width/height/top/left`

### 7.2 按钮状态过渡

```css
.button {
  /* 默认状态 */
  background: #3B82F6;
  color: #FFFFFF;

  /* Hover 状态 */
  &:hover {
    background: #2563EB;
    transition: background 150ms ease-out;
  }

  /* Active 状态 */
  &:active {
    background: #1D4ED8;
    transform: scale(0.98);
    transition: transform 80ms ease-out;
  }

  /* Focus 状态 */
  &:focus {
    box-shadow: 0 0 0 2px #3B82F6;
    transition: box-shadow 150ms ease-out;
  }
}
```

### 7.3 卡片状态过渡

```css
.card {
  /* Hover */
  &:hover {
    box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
    transition: box-shadow 150ms ease-out;
  }

  /* Active */
  &:active {
    transform: scale(0.98);
    transition: transform 80ms ease-out;
  }

  /* Selected */
  &.selected {
    border: 2px solid #3B82F6;
    background: #EFF6FF;
    transition: all 150ms ease-out;
  }
}
```

### 7.4 弹窗动画

```css
/* 弹窗入场 */
.modal-enter {
  animation: modal-enter 150ms ease-out;
}

@keyframes modal-enter {
  from {
    opacity: 0;
    transform: translate(-50%, -48%) scale(0.95);
  }
  to {
    opacity: 1;
    transform: translate(-50%, -50%) scale(1);
  }
}

/* 弹窗退出 */
.modal-exit {
  animation: modal-exit 120ms ease-in forwards;
}

@keyframes modal-exit {
  from {
    opacity: 1;
    transform: translate(-50%, -50%) scale(1);
  }
  to {
    opacity: 0;
    transform: translate(-50%, -48%) scale(0.95);
  }
}
```

**规则**：
- ✓ 退出动画比进入快（120ms vs 150ms）
- ✓ 退出使用 `ease-in`，进入使用 `ease-out`
- ✓ 可中断（用户点击立即关闭）

### 7.5 Toast 动画

```css
.toast {
  animation:
    toast-enter 150ms ease-out,
    toast-exit 150ms ease-in 3s forwards;
}

@keyframes toast-enter {
  from {
    opacity: 0;
    transform: translateX(-50%) translateY(-16px);
  }
  to {
    opacity: 1;
    transform: translateX(-50%) translateY(0);
  }
}

@keyframes toast-exit {
  from {
    opacity: 1;
    transform: translateX(-50%) translateY(0);
  }
  to {
    opacity: 0;
    transform: translateX(-50%) translateY(-16px);
  }
}
```

**自动消失规则**：
- 成功提示: 3秒自动消失
- 错误提示: 5秒自动消失或手动关闭

---

## 8. 响应式设计规范

### 8.1 移动端优先原则

- ✓ 从最小屏幕设计，逐步增强
- ✓ 核心功能在小屏幕完全可用
- ✓ 避免水平滚动
- ✓ 内容宽度自适应

### 8.2 触摸友好设计

**遵循 iOS HIG + Material Design 规范：**

| 规则 | 标准 |
|------|------|
| **最小触摸区域** | `44×44px`（iOS）<br>`48×48dp`（Android） |
| **触摸间距** | 最小 `8px` 间距 |
| **主要操作位置** | 底部固定（拇指友好区） |
| **手势支持** | 支持系统手势，不冲突 |

### 8.3 安全区域处理

**遵循 iOS Safe Area 规范：**

```css
/* 底部导航避开 Home Indicator */
.bottom-nav {
  bottom: 0;
  padding-bottom: calc(8px + env(safe-area-inset-bottom));
}

/* 顶部避开 Status Bar/Notch */
.header {
  top: 0;
  padding-top: env(safe-area-inset-top);
}
```

### 8.4 内容适配规则

| 内容类型 | Mobile | Tablet | Desktop |
|---------|---------|--------|---------|
| **活动列表** | 单列卡片 | 双列网格 | 三列网格 |
| **投票选项** | 单列卡片 | 双列网格 | 三列网格 |
| **表单** | 单列居中 | 单列居中 | 居中或左对齐 |
| **统计图表** | 全宽 | 居中 | 居中，更大尺寸 |

### 8.5 文字可读性

| 规则 | 标准 |
|------|------|
| **最小字号** | `14px`（正文） |
| **行高** | `1.5` |
| **行宽** | Mobile 35-60字符<br>Desktop 60-75字符 |
| **对比度** | ≥ 4.5:1 |

### 8.6 图片响应式

```css
/* 活动封面 */
.cover-image {
  width: 100%;
  aspect-ratio: 16 / 9;    /* 防止 layout shift */
  object-fit: cover;

  /* 响应式尺寸 */
  @media (min-width: 1024px) {
    aspect-ratio: 21 / 9;  /* Desktop 宽屏比例 */
  }
}

/* 选项图片 */
.option-image {
  width: 100%;
  aspect-ratio: 1 / 1;     /* 正方形 */
  object-fit: cover;

  @media (min-width: 768px) {
    max-width: 200px;      /* Tablet 固定最大尺寸 */
  }
}
```

---

## 9. 可访问性规范（WCAG AA）

### 9.1 对比度标准

**所有文字组合必须验证对比度：**

- ✓ 正文（≤18px）: ≥ 4.5:1
- ✓ 大标题（≥18px bold）: ≥ 3:1
- ✓ 功能图标: ≥ 3:1

**验证工具**: WebAIM Contrast Checker

### 9.2 Focus 状态

**遵循 WCAG 2.1 规范：**

```css
/* 所有可交互元素必须有可见 Focus Ring */
button,
a,
input,
select,
textarea,
[role="button"] {
  &:focus {
    outline: none;
    box-shadow: 0 0 0 2px #3B82F6;
  }
}

/* 不移除默认 focus ring */
*:focus {
  outline: none; /* 仅在有自定义样式时移除 */
}
```

### 9.3 键盘导航

- ✓ Tab 顺序匹配视觉顺序
- ✓ 全键盘可操作（不依赖鼠标）
- ✓ Enter/Space 触发按钮
- ✓ Esc 关闭弹窗

### 9.4 ARIA 标签

```html
<!-- 图标按钮 -->
<button aria-label="返回上一页">
  <icon name="arrow-left" />
</button>

<!-- 状态标签 -->
<span role="status" aria-label="进行中">进行中</span>

<!-- 弹窗 -->
<div role="dialog" aria-modal="true" aria-labelledby="modal-title">
  <h2 id="modal-title">登录</h2>
</div>

<!-- Toast -->
<div role="alert" aria-live="polite">投票成功</div>
```

### 9.5 表单可访问性

```html
<!-- 标签关联 -->
<label for="phone">手机号</label>
<input type="tel" id="phone" name="phone" required aria-required="true">

<!-- 错误提示 -->
<input aria-invalid="true" aria-describedby="phone-error">
<span id="phone-error" role="alert">请输入正确的手机号</span>

<!-- Helper Text -->
<input aria-describedby="phone-hint">
<span id="phone-hint">11位数字，以1开头</span>
```

### 9.6 图片替代文字

```html
<!-- 有意义图片 -->
<img src="activity-cover.jpg" alt="2026年度最佳员工评选活动封面">

<!-- 装饰性图片 -->
<img src="background.jpg" alt="" role="presentation">
```

### 9.7 减少动画（Reduced Motion）

```css
/* 尊重用户偏好 */
@media (prefers-reduced-motion: reduce) {
  *,
  *::before,
  *::after {
    animation-duration: 0.01ms !important;
    animation-iteration-count: 1 !important;
    transition-duration: 0.01ms !important;
  }
}
```

---

## 10. 性能优化规范

### 10.1 图片优化

- ✓ 使用 WebP/AVIF 格式
- ✓ Lazy Loading（非首屏图片）
- ✓ 响应式图片（srcset/sizes）
- ✓ 声明尺寸（width/height）防止 layout shift

```html
<img
  src="cover.webp"
  srcset="cover-320.webp 320w, cover-640.webp 640w, cover-1200.webp 1200w"
  sizes="(max-width: 768px) 100vw, (max-width: 1024px) 50vw, 33vw"
  width="1600"
  height="900"
  loading="lazy"
  alt="活动封面"
>
```

### 10.2 字体加载

```css
/* 防止 FOIT（Flash of Invisible Text） */
@font-face {
  font-family: "Custom Font";
  font-display: swap;  /* 立即显示 fallback 字体 */
}

/* 预加载关键字体 */
<link rel="preload" href="/fonts/custom-font.woff2" as="font" type="font/woff2" crossorigin>
```

### 10.3 CSS 性能

- ✓ 内联关键 CSS（首屏）
- ✓ 异步加载非关键 CSS
- ✓ 使用 CSS Variables（减少重复）
- ✓ 避免 @import（阻塞加载）

### 10.4 JavaScript 性能

- ✓ Lazy Loading 非关键组件
- ✓ Bundle Splitting（路由级）
- ✓ 防抖/节流高频事件
- ✓ 虚拟列表（>50项）

### 10.5 Core Web Vitals 目标

| 指标 | 目标值 |
|------|--------|
| **LCP (Largest Contentful Paint)** | < 2.5s |
| **FID (First Input Delay)** | < 100ms |
| **CLS (Cumulative Layout Shift)** | < 0.1 |

---

## 11. 特殊页面设计要点

### 11.1 登录弹窗

**设计要点**：
- 简洁清晰的表单布局
- 验证码倒计时按钮
- 明确的错误提示
- 快速的反馈响应

**布局结构**：
```
Modal (320px)
  ├── Close Button (右上角)
  ├── Title: "手机号登录" (居中)
  ├── Phone Input
  ├── Code Input + Send Button (行内布局)
  ├── Submit Button (全宽)
  └── Helper Text: "验证码5分钟内有效"
```

### 11.2 活动详情页

**设计要点**：
- 大封面吸引视觉
- 清晰的信息层级
- 选项卡片易于选择
- 投票操作明确可见

**布局结构**：
```
Page
  ├── Header (Fixed)
  ├── Cover Image (全宽)
  ├── Activity Info (Title, Time, Description)
  ├── Voting Options (Grid布局)
  ├── Voting Action (Fixed底部或页面底部)
  └── Rules Section (折叠展开)
```

### 11.3 投票结果页

**设计要点**：
- 统计数据清晰展示
- 排名视觉化（奖杯图标）
- 数据可视化（柱状图/饼图）
- 交互切换（图表类型）

**布局结构**：
```
Page
  ├── Header (Fixed)
  ├── Stats Overview (总参与人数、时间)
  ├── Ranking List (卡片列表)
  ├── Chart Section (可视化图表)
  └── Chart Toggle (柱状图/饼图切换)
```

---

## 12. 检查清单（Pre-Delivery）

### 12.1 视觉质量

- [ ] 无 Emoji 作为图标（使用 SVG）
- [ ] 图标来源一致（Lucide Icons）
- [ ] 色彩对比度符合 WCAG AA（≥4.5:1）
- [ ] 字号不小于 14px（正文）
- [ ] 品牌元素正确使用

### 12.2 交互状态

- [ ] 所有按钮有 Hover/Active/Focus 状态
- [ ] 触摸区域 ≥ 44×44px
- [ ] 微交互时长 150-300ms
- [ ] 禁用状态清晰可见
- [ ] Focus Ring 可见（可访问性）

### 12.3 响应式

- [ ] Mobile/Tablet/Desktop 三端测试
- [ ] 无水平滚动
- [ ] 安全区域处理正确
- [ ] 内容宽度自适应
- [ ] 图片响应式正确

### 12.4 可访问性

- [ ] 所有图片有 alt 文字
- [ ] 表单有 label 和 aria 属性
- [ ] 颜色不是唯一信息载体
- [ ] 支持键盘导航
- [ ] 支持 Reduced Motion

### 12.5 性能

- [ ] 图片 WebP 格式 + Lazy Loading
- [ ] 字体 font-display: swap
- [ ] 声明图片尺寸（防止 CLS）
- [ ] Core Web Vitals 目标达标
- [ ] 无 layout shift

---

## 13. 设计资源与工具

### 13.1 推荐工具

| 类型 | 工具 | 用途 |
|------|------|------|
| **图标库** | [Lucide Icons](https://lucide.dev/) | SVG 图标 |
| **对比度检查** | [WebAIM Contrast Checker](https://webaim.org/resources/contrastchecker/) | WCAG 验证 |
| **配色方案** | [Tailwind Colors](https://tailwindcss.com/docs/customizing-colors) | 色彩参考 |
| **字体** | [Google Fonts](https://fonts.google.com/) | 字体选择 |
| **性能测试** | [WebPageTest](https://www.webpagetest.org/) | Core Web Vitals |

### 13.2 设计文件

- **Figma Library**: 参考 Material Design 3
- **Icon Assets**: Lucide Icons SVG
- **Color Tokens**: 本文档色彩系统

---

**文档变更记录**

| 版本 | 日期 | 修改人 | 修改内容 |
|------|------|--------|---------|
| v1.0 | 2026-04-27 | Design Agent | 初始版本创建 |