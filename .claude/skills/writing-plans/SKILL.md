# Writing-Plans Skill

**来源**：superpowers 项目 - 用于制定实现计划。

**核心目的**：为多步骤任务创建全面的实现计划。

## 启动声明

使用此 skill 时，首先声明：
> "我正在使用 writing-plans skill 来创建实现计划。"

## 保存位置

`docs/${项目名称}/产品设计/03-UI设计规范/UI-Design-Spec.md`

## 核心原则

- 假设工程师"对本代码库零上下文，品味存疑"
- DRY、YAGNI、TDD、频繁提交
- 每个步骤 = 2-5 分钟可完成的动作
- 每个步骤必须包含完整代码 — 不允许占位符

## 必需的计划头部

```markdown
# <计划标题>

## Goal
<目标描述>

## Architecture
<架构概述>

## Tech Stack
<技术栈列表>
```

## 任务结构

每个任务列出：
- **Files**：创建/修改/测试的文件
- **Steps**：带复选框的步骤

```
## Task 1: <任务名称>

### Files
- <文件路径> — 创建/修改/测试

### Steps
- [ ] Write failing test for X
- [ ] Run test, confirm failure
- [ ] Implement X
- [ ] Run test, confirm pass
- [ ] Commit "feat: add X"
```

## 禁止的占位符

- "TBD"、"TODO"、"implement later"
- "Add appropriate error handling"
- "Write tests for the above"（无实际代码）
- 引用未定义的函数

## 自查清单

1. **规格覆盖**：每个需求都有对应任务
2. **占位符扫描**：修复所有红旗标记
3. **类型一致性**：名称在各任务间匹配

## 完成后选项

保存计划后，提供两种执行选项：
1. **Subagent-Driven**（推荐）— 每个任务启动新子代理
2. **Inline Execution** — 分批执行，带检查点