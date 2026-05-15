# Playwright Test Skill

用于指导 Claude Code 为平台和生成产物编写 Playwright 端到端测试。

重点：
- 优先验证真实用户流程
- 默认使用真实前后端联调环境，不用全局 mock 掩盖接口缺失
- 覆盖主导航连续跳转、返回/前进、刷新、重复点击、详情/编辑/提交后跳转等路由回归
- 监听 console error、pageerror、未处理 promise rejection 和网络 4xx/5xx，发现白屏或关键区域消失必须失败
- 使用稳定选择器
- 尽量避免与后端真实时序强耦合
- 优先复用现有 `tests/` 与 `playwright.config.js`

说明：
- 这是作者工具层 skill，不等同于平台运行时 `packages/skills/`。

参考资料见 `skills/playwright-test-skill/`、`tests/` 与根目录 `playwright.config.js`。