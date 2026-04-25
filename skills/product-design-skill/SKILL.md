# Product Design Skill

## 目的
把自然语言需求拆成结构化 PRD、用户流程、页面清单和核心组件建议，供 Product Agent 与 Design Agent 在设计阶段复用。

## 使用边界
- 这是 Codex 作者工具层能力，不是平台运行时依赖。
- 运行时请走 `packages/skills/prd-skill` 与 `packages/skills/ui-generate-skill`。
- 输出优先结构化，避免只给泛泛的描述性文案。

## 输出要求
1. `PRD 摘要`
2. `目标用户`
3. `核心流程`
4. `页面清单`
5. `组件建议`
6. `UI 规范`

## 参考
- `references/prd-checklist.md`
- `references/ui-checklist.md`
