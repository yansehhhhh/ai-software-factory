# Playwright Test Skill

用于指导 Claude Code 为平台和生成产物编写 Playwright 端到端测试。

重点：
- 优先验证真实用户流程
- 使用稳定选择器
- 尽量避免与后端真实时序强耦合
- 优先复用现有 `tests/` 与 `playwright.config.js`

说明：
- 这是作者工具层 skill，不等同于平台运行时 `packages/skills/`。

参考资料见 `skills/playwright-test-skill/`、`tests/` 与根目录 `playwright.config.js`。