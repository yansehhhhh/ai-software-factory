## ADDED Requirements

### Requirement: 讨论消息以 Markdown 渲染
系统 SHALL 将需求讨论和阶段审查讨论消息以 Markdown 渲染，而不是纯文本展示。

#### Scenario: AI 回复包含 Markdown 表格
- **WHEN** AI 讨论消息包含 Markdown 表格
- **THEN** 讨论面板将内容展示为 HTML 表格
- **AND** 保持消息顺序和发送者样式

#### Scenario: AI 回复包含代码块
- **WHEN** AI 讨论消息包含 fenced code block
- **THEN** 讨论面板将其展示为格式化代码块

#### Scenario: 用户消息包含列表
- **WHEN** 用户讨论消息包含 Markdown 列表语法
- **THEN** 讨论面板将其展示为列表

### Requirement: Markdown 渲染必须清洗 HTML
系统 SHALL 在将 Markdown HTML 插入 DOM 前进行安全清洗。

#### Scenario: 消息包含 script 标签
- **WHEN** 讨论消息包含 `<script>` 标签
- **THEN** 渲染后的消息不会执行脚本
- **AND** 不安全标签会被移除或转义

#### Scenario: 消息包含不安全内联事件
- **WHEN** 讨论消息包含带内联事件处理器的 HTML
- **THEN** 渲染后的消息不会保留不安全事件处理器

### Requirement: Markdown 渲染保持可读布局
系统 SHALL 为 Markdown 输出提供样式，使表格、列表、段落和代码块在现有讨论布局中保持可读。

#### Scenario: 展示较宽 Markdown 表格
- **WHEN** 消息包含较宽的 Markdown 表格
- **THEN** 讨论面板允许横向滚动或响应式换行
- **AND** 表格不会破坏页面布局

#### Scenario: 展示多段落回复
- **WHEN** 消息包含多个 Markdown 段落
- **THEN** 讨论面板保留段落间距
- **AND** 消息气泡仍与发送者视觉对齐
