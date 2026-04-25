# refactor-ui

## 用途
在不改变平台整体架构的前提下，重构或优化 `apps/web` 的页面和组件实现。

## 主要输入
- 目标页面或组件
- 期望交互或视觉改动
- 是否需要同步调整状态管理

## 执行步骤
1. 先确认改动范围位于 `apps/web/src/views`、`components`、`stores` 中的哪一层。
2. 优先复用已有组件、路由和 store 结构，不新增平行目录。
3. 如涉及接口联动，再检查 `apps/web/src/api/` 中的现有调用方式。
4. 完成后优先验证页面黄金路径与关键交互。

## 涉及目录
- `apps/web/src/views/`
- `apps/web/src/components/`
- `apps/web/src/stores/`
- `apps/web/src/api/`