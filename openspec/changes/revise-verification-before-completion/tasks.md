## 1. 前端请求门控修订

- [x] 1.1 修改 `generated/HX-Meeting/frontend/src/stores/appStore.js`，将 `authApi.me()` 从并行请求中拆出并优先执行。
- [x] 1.2 在 `bootstrap` 中仅当 `currentUser.roles` 包含 `ADMIN` 时调用 `adminApi.dashboard()`；普通用户不得调用该接口，`adminDashboard` 保持 `null` 或已有安全默认值。
- [x] 1.3 修改 `generated/HX-Meeting/frontend/src/views/meeting/BookingSearchView.vue`，移除进入页面时自动执行的 `searchRooms`。
- [x] 1.4 确认点击“查询可用会议室”仍会调用 `roomsApi.list`，并保持选会议室、提交预约的既有流程可用。

## 2. 完成前验证产物修订

- [x] 2.1 更新 `docs/移动端应用：会议室预约系统/测试/e2e/dev-integration.spec.js`，新增普通用户启动/进入预约页不请求 `/api/hx-meeting/v1/admin/dashboard` 的断言。
- [x] 2.2 更新 `docs/移动端应用：会议室预约系统/测试/e2e/dev-integration.spec.js`，新增进入 `/booking` 后点击查询前不请求 GET `/api/hx-meeting/v1/rooms`、点击后才请求的断言。
- [x] 2.3 更新 `docs/移动端应用：会议室预约系统/测试/测试用例/功能测试用例.md` 与 `test-case-summary.md`，记录两个网络请求回归用例。
- [x] 2.4 更新 `docs/移动端应用：会议室预约系统/测试/测试报告/dev-integration-report.md` 与 `test-report.md`，纳入本次反馈项的验证结论和执行命令。

## 3. 验证与收尾

- [x] 3.1 运行生成前端相关构建或测试命令，确认前端修改无语法错误。
- [x] 3.2 运行或说明完成前验证 E2E 命令，确认新增断言覆盖普通用户管理员接口禁用与预约页手动查询行为。
- [x] 3.3 检查最终改动仅位于允许修改范围和本 OpenSpec change 目录内。
