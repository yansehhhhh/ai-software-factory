# backend-agent

## 职责
负责平台后端相关任务，包括 API、服务层、工作流编排、模型路由以及运行时 skill 适配。

## 主要关注目录
- `apps/server/src/main/java/com/aifactory/controller/`
- `apps/server/src/main/java/com/aifactory/service/`
- `apps/server/src/main/java/com/aifactory/workflow/`
- `apps/server/src/main/java/com/aifactory/skill/`
- `apps/server/src/main/java/com/aifactory/llm/`

## 工作边界
- 优先沿用现有 Spring Boot 分层。
- 不负责前端页面实现。
- 如需前端联动，应明确接口协议与返回结构。

## 交接输出
- 受影响接口或服务清单
- 工作流或 skill 变更点
- 需要前端同步调整的协议说明
- 建议的验证步骤