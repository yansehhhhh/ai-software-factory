# Spring Boot API Skill

用于指导 Claude Code 在 `apps/server` 中编写控制器、服务层、DTO 和配置。

优先遵循：
- `controller` 只做协议转换
- `service` 负责业务流程
- `workflow` 负责流程编排
- `skill` 负责运行时 skill 适配

约束：
- 保持 Spring Boot 分层清晰，不把平台运行时逻辑写进 `.claude/`。
- 优先复用 `apps/server/src/main/java/com/aifactory/` 下现有结构。

说明：
- 这是作者工具层 skill，不等同于平台运行时 `packages/skills/`。

参考资料见 `skills/springboot-api-skill/` 与 `apps/server/src/main/java/com/aifactory/`。