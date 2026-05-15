## ADDED Requirements

### Requirement: 阶段完成后暂停等待确认
系统 SHALL 在每个可审查阶段完成后暂停主工作流，并将已完成阶段的工作流状态暴露为 `awaiting_review`。

#### Scenario: 需求产物阶段完成
- **WHEN** 需求产物生成阶段成功完成
- **THEN** 工作流状态为 `awaiting_review`
- **AND** 当前审查阶段为 `product-design-artifacts`
- **AND** 下一阶段不会自动开始

#### Scenario: UI 设计阶段完成
- **WHEN** UI 设计阶段成功完成
- **THEN** 工作流状态为 `awaiting_review`
- **AND** 当前审查阶段为 `ui-design`
- **AND** 架构设计阶段不会自动开始

### Requirement: 用户可以确认当前阶段
系统 SHALL 允许用户确认当前审查阶段，并从下一阶段继续执行工作流。

#### Scenario: 用户确认阶段产物
- **WHEN** 工作流处于 `awaiting_review`
- **AND** 用户点击确认通过
- **THEN** 系统记录当前阶段已确认
- **AND** 启动下一个工作流阶段

#### Scenario: 用户确认最后一个可审查阶段
- **WHEN** 工作流处于最后一个可审查阶段的 `awaiting_review`
- **AND** 用户点击确认通过
- **THEN** 系统完成交付步骤
- **AND** 暴露最终结果为可用状态

### Requirement: 用户可以请求阶段修订
系统 SHALL 允许用户在确认当前阶段前，对当前阶段请求修改。

#### Scenario: 用户带反馈请求修订
- **WHEN** 工作流处于 `awaiting_review`
- **AND** 用户提交修订反馈
- **THEN** 系统将反馈记录到当前工作流 run 和当前阶段
- **AND** 工作流保持在当前阶段，不会启动下一阶段

### Requirement: 阶段审查展示产物
系统 SHALL 在 `awaiting_review` 状态下向用户展示当前审查阶段的产物。

#### Scenario: 阶段存在生成产物
- **WHEN** 阶段进入 `awaiting_review`
- **THEN** 阶段审查面板展示生成产物列表
- **AND** 每个产物包含显示名称、类型和查看或下载 URL

#### Scenario: 阶段没有生成产物
- **WHEN** 阶段进入 `awaiting_review` 且没有生成产物
- **THEN** 系统展示空产物状态
- **AND** 仍然允许用户确认通过或请求修订
