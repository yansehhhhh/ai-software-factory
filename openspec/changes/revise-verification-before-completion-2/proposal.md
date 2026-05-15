## Why

用户反馈当前完成前验证仍可能依赖前端 mock 或模拟数据链路，无法证明“所有接口都使用真实的、后端实现的接口”。需要修订完成前验证产物，建立可执行的真实前后端联调验收标准，确保移动端会议室预约系统的接口调用均到达 `generated/HX-Meeting/backend`。

## What Changes

- 修订完成前验证 E2E 与测试报告，明确禁止前端 mock、路由拦截 mock、fixture mock 作为接口验收依据。
- 扩展真实接口联调覆盖：用户身份、会议室查询、预约提交、冲突错误、通知、我的预约、转让、违规汇总、管理员接口权限与管理员审批入口。
- 在前端生成工程中保留 demo 数据开关但默认关闭；验收时必须通过 Vite proxy 或显式 API_BASE_URL 访问生成后端。
- 在后端生成工程中确保验收涉及的接口均由 Spring Boot controller/service/workflow 实现并返回真实响应。
- 更新测试报告，记录真实后端地址、验证命令、覆盖接口清单、mock 排除结论和风险。

## Capabilities

### New Capabilities
- `real-backend-verification`: 定义完成前验证阶段必须以真实生成后端接口作为验收依据，覆盖前端调用链、接口响应、权限与错误状态验证。

### Modified Capabilities

## Impact

- 允许修改范围内的测试产物：`docs/移动端应用：会议室预约系统/测试/`。
- 允许修改范围内的生成工程：`generated/HX-Meeting/frontend`、`generated/HX-Meeting/backend`。
- 影响前端 API client、mock/demo 数据开关、Vite proxy、E2E Playwright 脚本与测试报告。
- 影响后端控制器/服务的验收可用性，但不引入平台运行时或 `.claude/skills/` 变更。
