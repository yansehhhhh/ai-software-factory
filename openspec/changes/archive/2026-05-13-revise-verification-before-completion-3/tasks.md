## 1. 现状定位与失败复现

- [x] 1.1 检查 `generated/HX-Meeting/frontend/src/views/admin/RoomAdminView.vue`、`src/api/hxMeeting.js` 与生成后端管理端会议室接口，确认“新增”当前是否只停留在样式/本地状态或缺少真实接口
- [x] 1.2 检查 `generated/HX-Meeting/frontend/src/components/layout/BottomNavigation.vue`、`AppShell.vue`、`router/index.js` 和底部导航目标页面，定位切换后正文空白的条件渲染、异步加载或路由异常来源
- [x] 1.3 检查 `NotificationsView.vue`、通知相关路由和后端通知接口，定位点击通知进入详情页白屏的路由缺失、组件缺失或数据字段异常

## 2. 会议室管理新增修复

- [x] 2.1 如后端缺少真实新增能力，在 `generated/HX-Meeting/backend` 补齐管理员 `POST /api/hx-meeting/v1/admin/rooms` 接口，复用现有鉴权、响应 envelope、H2 持久化和会议室数据模型
- [x] 2.2 在 `generated/HX-Meeting/frontend` 补齐会议室新增 API client 方法，确保使用管理员 token 调用真实后端而非 demoState/mock
- [x] 2.3 改造 `RoomAdminView.vue` 的新增入口为真实表单交互，包含必填校验、容量合法性校验、提交中状态、成功提示、失败提示和列表刷新
- [x] 2.4 验证普通用户不能访问或调用会议室新增能力，保持管理端鉴权约束

## 3. 底部导航白屏修复

- [x] 3.1 修复底部导航目标路由与实际页面组件不一致、重复跳转或异常吞掉的问题，保证每个导航项都有稳定可渲染目标
- [x] 3.2 为首页、预约、通知/我的、违规、管理入口等底部导航页面补齐加载、空状态、错误状态或权限提示，避免接口异常时只显示标题
- [x] 3.3 检查页面异步请求和权限门控，确保普通用户与管理员 token 切换后不会因未捕获异常导致 `router-view` 正文空白

## 4. 通知详情白屏修复

- [x] 4.1 在前端补齐或修复通知详情路由，例如 `/notifications/:id`，并确保通知列表点击使用有效 id 导航
- [x] 4.2 新增或修复通知详情视图，展示标题、正文/摘要、时间、已读状态等信息，并提供返回通知列表入口
- [x] 4.3 如后端缺少通知详情接口，在 `generated/HX-Meeting/backend` 补齐 `GET /api/hx-meeting/v1/notifications/{id}`；如接口已存在，则对齐前端字段解析
- [x] 4.4 处理通知不存在、接口失败或字段为空场景，页面展示明确提示，不抛出未捕获错误

## 5. 完成前验证用例与报告

- [x] 5.1 更新 `docs/移动端应用：会议室预约系统/测试/e2e/dev-integration.spec.js`，新增管理员真实新增会议室并刷新列表展示的 E2E 回归断言
- [x] 5.2 更新同一 E2E 文件，新增底部导航多轮切换并断言每页正文/空状态/权限提示可见的回归断言
- [x] 5.3 更新同一 E2E 文件，新增通知列表点击进入详情且详情页不白屏的回归断言
- [x] 5.4 更新 `docs/移动端应用：会议室预约系统/测试/测试用例/` 下相关测试用例说明，补充三类缺陷回归场景
- [x] 5.5 执行前端构建、后端启动和完成前验证 E2E，确认所有新增与既有用例通过且未使用 Playwright route fulfillment 或前端 mock
- [x] 5.6 更新 `docs/移动端应用：会议室预约系统/测试/测试报告/` 下测试报告，记录本次修复项、执行命令、通过结果和风险说明
