## Context

当前阶段为“完成前验证”，修订对象限定在 `docs/移动端应用：会议室预约系统/测试/` 以及生成工程 `generated/HX-Meeting/frontend`、`generated/HX-Meeting/backend`。用户明确要求所有接口必须使用真实后端实现，而不是前端 mock。现有测试已包含部分真实 API 验证，但需要将该要求固化为 OpenSpec 变更，并在实施阶段补齐可审计的测试脚本与报告。

## Goals / Non-Goals

**Goals:**
- 确保验收时前端默认不启用 mock/demo API 数据，所有业务接口通过真实 HTTP 请求到达生成后端。
- 用 Playwright 覆盖核心前后端链路：身份、会议室查询、预约提交、冲突错误态、通知/我的预约/违规/转让、管理员权限与审批入口。
- 在测试报告中明确记录真实后端地址、接口清单、验证命令和“未使用 mock”的判断依据。
- 只修改允许范围内的测试产物和 `generated/HX-Meeting` 前后端工程。

**Non-Goals:**
- 不修改平台运行时 `apps/`、`packages/` 或 `.claude/skills/`。
- 不重新设计会议室预约业务流程或数据库模型。
- 不以静态截图、前端 fixture、路由拦截模拟响应替代真实接口验收。

## Decisions

1. **真实接口判定以网络请求和后端响应为准**
   - 方案：E2E 同时使用浏览器页面的 `waitForResponse` 和 Playwright `request` 调用 `API_BASE_URL`，断言响应状态、响应体业务字段和页面展示。
   - 原因：只检查页面文本无法证明接口来自后端；直接检查 HTTP 响应可审计。
   - 替代方案：仅运行前端单元测试或 mock service worker，不能满足用户反馈要求。

2. **前端 demo 数据仅保留显式开关，默认关闭**
   - 方案：验收环境不设置 `VITE_USE_DEMO=true`，前端 API client 默认走 `/api/hx-meeting/v1`。
   - 原因：保留演示能力但避免默认 mock 污染完成前验证。
   - 替代方案：删除所有 demo 数据，影响离线演示且超出当前阶段修订范围。

3. **后端以生成工程 Spring Boot 实现作为唯一 API 来源**
   - 方案：验收命令启动或连接 `generated/HX-Meeting/backend`，前端 Vite proxy 指向该后端端口；测试中的 `API_BASE_URL` 也指向同一后端。
   - 原因：避免请求误打到平台后端或前端本地 mock。
   - 替代方案：将接口代理到平台后端，不符合生成项目验收目标。

4. **报告必须包含 mock 排除证据**
   - 方案：在 `dev-integration-report.md` 与 `test-report.md` 中记录环境变量、代理目标、接口覆盖表、真实 HTTP 断言和未启用 mock 的结论。
   - 原因：完成前验证需要可追溯证据，便于用户和流水线复核。

## Risks / Trade-offs

- [H2 文件数据库被旧进程占用] → 测试前停止旧预览后端，或使用独立端口/数据目录运行后端。
- [预约冲突测试依赖动态时间和数据状态] → 使用未来日期、先查询可用会议室，再通过真实后端预置冲突数据。
- [角色权限导致普通用户访问管理员接口返回 403] → 报告中明确普通用户 403 为预期，同时使用 `admin-token` 验证管理员接口 200。
- [前端仍存在 demo 数据代码] → 验收以默认环境和网络请求为准，实施时检查 mock 开关只在显式 `VITE_USE_DEMO=true` 时生效。
