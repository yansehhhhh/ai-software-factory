## ADDED Requirements

### Requirement: 阶段修订上下文绑定到工作流阶段
当用户开始基于 OpenSpec 的阶段修订时，系统 SHALL 创建或维护阶段修订上下文，其中包含 `workflowRunId`、`stageKey`、`artifactPaths`、`userFeedback`，以及可选的 `changeId`。

#### Scenario: 用户开始修订讨论
- **WHEN** 工作流处于 `awaiting_review`
- **AND** 用户点击 OpenSpec 讨论操作
- **THEN** 系统向 OpenSpec explore prompt 提供当前 `workflowRunId`、`stageKey`、生成产物路径和最新用户反馈

#### Scenario: 当前阶段已有修订 change
- **WHEN** 用户对已有 `changeId` 的阶段触发 OpenSpec 操作
- **THEN** 系统复用已有 `changeId`
- **AND** 将该 `changeId` 包含在操作上下文中

### Requirement: 用户可以为阶段修订创建 OpenSpec 提案
系统 SHALL 允许用户基于当前阶段上下文和讨论反馈，为当前阶段修订创建 OpenSpec proposal。

#### Scenario: 用户从阶段审查创建提案
- **WHEN** 工作流处于 `awaiting_review`
- **AND** 用户点击制定计划操作
- **THEN** 系统使用当前阶段产物和反馈调用 OpenSpec propose 流程
- **AND** 将生成的 `changeId` 关联到阶段修订上下文

### Requirement: 用户可以执行 OpenSpec 阶段修订
系统 SHALL 允许用户执行与当前阶段修订关联的 OpenSpec change。

#### Scenario: 用户执行阶段修订 change
- **WHEN** 阶段修订已关联 `changeId`
- **AND** 用户点击执行计划操作
- **THEN** 系统对该 `changeId` 调用 OpenSpec apply
- **AND** 将操作限制在当前阶段相关产物或代码路径内

### Requirement: 用户可以归档已完成的阶段修订
系统 SHALL 允许用户在阶段修订完成后归档对应 OpenSpec change。

#### Scenario: 用户归档阶段修订 change
- **WHEN** 阶段修订 change 已执行
- **AND** 用户点击归档操作
- **THEN** 系统对关联的 `changeId` 调用 OpenSpec archive
- **AND** 工作流回到同一阶段的 `awaiting_review`
- **AND** 不会自动启动下一工作流阶段

### Requirement: 非审查状态不可触发阶段修订操作
当没有工作流阶段处于等待确认状态时，系统 SHALL 阻止 OpenSpec 阶段修订操作。

#### Scenario: 工作流运行中点击修订操作
- **WHEN** 工作流状态为 `running`
- **AND** 用户尝试触发 OpenSpec 阶段修订操作
- **THEN** 系统拒绝该操作
- **AND** 提示修订仅能在阶段等待确认时使用
