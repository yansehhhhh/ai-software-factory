# generate-demo

## 用途
生成或刷新当前仓库中的演示应用和输出产物。

## 主要输入
- 自然语言需求描述
- 目标演示范围
- 是否落地产物到 `generated/`

## 执行步骤
1. 先阅读 `README.md`、`docs/workflow.md` 和 `docs/demo-script.md`，理解当前工作流。
2. 如需参考固定样例，优先查看 `apps/inspector-demo/`。
3. 如需生成或刷新产物，优先查看 `scripts/` 与 `generated/` 的现有结构。
4. 输出时保持前端、后端、测试目录与仓库现有结构一致。

## 涉及目录
- `apps/inspector-demo/`
- `generated/`
- `scripts/`
- `docs/`