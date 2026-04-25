# Skill Design

This project separates skill concepts into three layers:

- `skills/`: Codex-readable authoring skills used to help code generation.
- `packages/skills/`: runtime modules that belong to the platform domain and are invoked by agents.
- `apps/server/src/main/java/com/aifactory/skill/`: Spring Boot adapters that expose runtime skills to the orchestration layer.
