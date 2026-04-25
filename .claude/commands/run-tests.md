# run-tests

## 用途
运行平台当前已有的测试入口，优先复用根目录已有脚本。

## 主要输入
- 测试范围：前端、后端、端到端
- 是否只验证某个模块

## 执行步骤
1. 查看根目录 `package.json` 中已有脚本。
2. 端到端测试优先使用 `yarn test:e2e`。
3. 后端测试优先使用 `yarn server:test`。
4. 如需前端构建验证，可使用 `yarn build:web`。
5. 如测试失败，优先定位真实失败点，不跳过现有检查。

## 涉及目录
- `package.json`
- `tests/`
- `apps/server/`
- `apps/web/`