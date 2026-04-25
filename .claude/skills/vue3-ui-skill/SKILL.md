# Vue3 UI Skill

用于指导 Claude Code 在本项目中编写 Vue 3 前端页面、组件拆分、状态流转和交互细节。

优先遵循：
- `apps/web/src/components`
- `apps/web/src/views`
- `apps/web/src/stores`

约束：
- 优先复用现有视图、组件和 store 的组织方式。
- 变更应服务于平台前端，不把运行时逻辑写进 `.claude/`。

说明：
- 这是作者工具层 skill，不等同于平台运行时 `packages/skills/`。

参考资料见 `skills/vue3-ui-skill/` 与 `apps/web/src/`。