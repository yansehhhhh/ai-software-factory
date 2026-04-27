# Using-Superpowers Skill

**来源**：superpowers 项目 - 用于 skill 发现和调用。

## 核心规则

### 规则 1：在响应前调用

**Invoke relevant or requested skills BEFORE any response or action.**

即使只有 1% 的可能性某个 skill 适用，也要调用该 skill 检查。

### 规则 2：使用 Skill 工具

在 Claude Code 中，使用 `Skill` 工具加载 skill 内容。不要直接使用 Read 读取 skill 文件。

### 规则 3：优先级顺序

用户指令 > Superpowers skills > 默认系统提示

### 规则 4：先处理类 skill

先调用过程类 skill（brainstorming、debugging），再调用实现类 skill。

## 红旗信号（避免）

出现以下想法意味着你在规避规则：
- "这只是个简单问题"
- "我需要先获取更多上下文"
- "我可以先回答，然后再调用 skill"

**正确做法**：先调用 skill，再做任何其他事情。

## Skill 分类

**过程类**（优先调用）：
- brainstorming
- systematic-debugging
- using-superpowers

**实现类**（过程类完成后调用）：
- writing-plans
- executing-plans
- test-driven-development
- verification-before-completion

## 在本项目中使用

当用户输入需求时，按以下顺序调用：
1. `brainstorming` - 需求讨论与设计
2. `writing-plans` - 制定实现计划
3. `verification-before-completion` - 完成前验证