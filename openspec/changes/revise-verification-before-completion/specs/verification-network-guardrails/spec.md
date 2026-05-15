## ADDED Requirements

### Requirement: 普通用户不得触发管理员看板请求
系统 SHALL 在普通用户启动应用、进入首页或进入预约相关菜单时避免请求 `/api/hx-meeting/v1/admin/dashboard`。

#### Scenario: 普通用户应用启动不请求管理员看板
- **WHEN** 浏览器使用普通用户 token 启动生成前端并完成基础数据加载
- **THEN** 网络请求中 MUST NOT 出现 `/api/hx-meeting/v1/admin/dashboard`

#### Scenario: 管理员角色允许加载管理员看板
- **WHEN** 当前用户身份包含 `ADMIN` 角色并进入需要管理员看板数据的流程
- **THEN** 系统 MAY 请求 `/api/hx-meeting/v1/admin/dashboard` 并正常展示管理员统计数据

### Requirement: 预约菜单不得默认查询会议室
系统 SHALL 在用户仅进入预约菜单 `/booking` 时不自动请求 `/api/hx-meeting/v1/rooms`，会议室查询 MUST 由用户点击查询按钮触发。

#### Scenario: 进入预约菜单不默认请求会议室列表
- **WHEN** 普通用户进入 `/booking` 页面且尚未点击“查询可用会议室”按钮
- **THEN** 网络请求中 MUST NOT 出现 GET `/api/hx-meeting/v1/rooms`

#### Scenario: 点击查询后请求会议室列表
- **WHEN** 普通用户在 `/booking` 页面填写或确认筛选条件并点击“查询可用会议室”按钮
- **THEN** 系统 MUST 发起 GET `/api/hx-meeting/v1/rooms` 请求并根据响应展示会议室列表或空态
