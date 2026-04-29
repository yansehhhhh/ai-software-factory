我已完成测试修复方案设计，并准备进入执行阶段。

计划要点：
- 先跑 `yarn server:test`，再跑 `yarn test:e2e`
- 优先修复真实失败项
- 高概率需要处理：
  - `tests/e2e/home.spec.js` 与当前前端流程不一致的问题
  - 可能的后端异步测试脆弱性：`apps/server/src/test/java/com/aifactory/WorkflowServiceIntegrationTests.java`

如果你批准，我下一步就会直接开始执行测试并修复第 1 轮失败项。