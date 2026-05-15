## MODIFIED Requirements

### Requirement: 完成前验证必须覆盖缺陷回归
系统 MUST 在完成前验证测试产物中覆盖会议室管理新增、底部导航稳定切换、通知详情访问以及 HX-Meeting 移动端 H5 UI 产物一致性回归路径，并记录真实验证命令与结果。UI 阶段修订完成前，验证 MUST 确认 PC 设计图不再作为核心交付依据，主 HTML 原型、组件库、交互说明、移动端适配产物和 UI 设计规范均以移动端 H5 为准。

#### Scenario: E2E 覆盖三类缺陷回归
- **WHEN** 执行完成前验证 E2E 用例
- **THEN** 测试 MUST 覆盖管理员新增会议室、底部导航多轮切换、通知详情点击进入，并断言真实后端响应或页面关键内容

#### Scenario: 测试报告记录新鲜验证证据
- **WHEN** 缺陷修复完成后更新测试报告
- **THEN** 报告 MUST 记录本次修订内容、执行命令、通过结果和仍需关注的风险，不得复用旧验证结论冒充新结果

#### Scenario: UI 阶段移动 H5 产物一致性验证
- **WHEN** UI 阶段修订完成并执行完成前验证
- **THEN** 验证 MUST 检查 `UI原型/设计稿/index.html`、`UI原型/组件库/组件清单.md`、`UI原型/组件库/component-library.html`、`UI原型/交互原型/交互说明.md`、`UI原型/移动端适配/响应式断点参考.md` 和 `产品设计/03-UI设计规范/UI-Design-Spec.md` 均明确以移动端 H5 为设计基准

#### Scenario: PC 设计图取消验证
- **WHEN** 验证 UI 阶段当前产物路径
- **THEN** 验证 MUST 确认 `UI原型/设计稿/desktop.svg` 不再展示有效 PC 设计方案，或 UI 设计规范不再将其列为核心评审依据
