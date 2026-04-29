本项目采用 Monorepo 架构，前端基于 Vue3 + JavaScript + Axios，后端基于 Java + Spring Boot，自动化测试采用 Playwright。平台通过 Agent 与 Skill 编排，实现从需求分析、UI设计、代码生成到自动化测试的完整闭环。

ai-software-factory/
├── AGENTS.md                         # Codex 项目总说明
├── README.md
├── package.json
├── pnpm-workspace.yaml
├── .env.example
├── .gitignore
│
├── skills/                           # ✅ Codex 可加载的 Skills
│   ├── vue3-ui-skill/
│   │   ├── SKILL.md
│   │   └── references/
│   │       └── ui-guidelines.md
│   │
│   ├── springboot-api-skill/
│   │   ├── SKILL.md
│   │   └── references/
│   │       └── springboot-guidelines.md
│   │
│   ├── playwright-test-skill/
│   │   ├── SKILL.md
│   │   └── references/
│   │       └── e2e-guidelines.md
│   │
│   ├── workflow-orchestration-skill/
│   │   ├── SKILL.md
│   │   └── references/
│   │       └── workflow-state-machine.md
│   │
│   ├── project-scaffold-skill/
│   │   ├── SKILL.md
│   │   └── references/
│   │       └── scaffold-rules.md
│   │
│   └── prompt-engineering-skill/
│       ├── SKILL.md
│       └── references/
│           └── prompt-patterns.md
│
├── apps/
│   ├── web/                          # 平台前端：Vue3 + JS + Axios
│   │   ├── public/
│   │   ├── src/
│   │   │   ├── api/
│   │   │   │   ├── request.js
│   │   │   │   ├── workflowApi.js
│   │   │   │   ├── agentApi.js
│   │   │   │   ├── logApi.js
│   │   │   │   └── resultApi.js
│   │   │   │
│   │   │   ├── views/
│   │   │   │   ├── DashboardView.vue
│   │   │   │   ├── WorkflowDetailView.vue
│   │   │   │   └── ResultView.vue
│   │   │   │
│   │   │   ├── components/
│   │   │   │   ├── RequirementInput.vue
│   │   │   │   ├── WorkflowProgress.vue
│   │   │   │   ├── StatusCards.vue
│   │   │   │   ├── RealtimeLogPanel.vue
│   │   │   │   ├── AgentTable.vue
│   │   │   │   └── ResultActions.vue
│   │   │   │
│   │   │   ├── router/
│   │   │   │   └── index.js
│   │   │   │
│   │   │   ├── stores/
│   │   │   │   ├── workflowStore.js
│   │   │   │   ├── agentStore.js
│   │   │   │   └── logStore.js
│   │   │   │
│   │   │   ├── workflow/
│   │   │   │   ├── status.js
│   │   │   │   └── steps.js
│   │   │   │
│   │   │   ├── utils/
│   │   │   │   └── format.js
│   │   │   │
│   │   │   ├── App.vue
│   │   │   └── main.js
│   │   │
│   │   ├── package.json
│   │   └── vite.config.js
│   │
│   ├── server/                       # 平台后端：Java + Spring Boot
│   │   ├── pom.xml
│   │   ├── mvnw
│   │   ├── mvnw.cmd
│   │   ├── .mvn/
│   │   │   └── wrapper/
│   │   │       └── maven-wrapper.properties
│   │   │
│   │   ├── src/main/java/com/aifactory/
│   │   │   ├── AiFactoryApplication.java
│   │   │   │
│   │   │   ├── controller/
│   │   │   │   ├── WorkflowController.java
│   │   │   │   ├── AgentController.java
│   │   │   │   ├── LogController.java
│   │   │   │   ├── ResultController.java
│   │   │   │   └── HealthController.java
│   │   │   │
│   │   │   ├── service/
│   │   │   │   ├── WorkflowService.java
│   │   │   │   ├── AgentService.java
│   │   │   │   ├── LogService.java
│   │   │   │   ├── ResultService.java
│   │   │   │   └── ProjectGenerateService.java
│   │   │   │
│   │   │   ├── agent/                # 平台里的多 Agent
│   │   │   │   ├── Agent.java
│   │   │   │   ├── ProductAgent.java
│   │   │   │   ├── DesignAgent.java
│   │   │   │   ├── DeveloperAgent.java
│   │   │   │   ├── QaAgent.java
│   │   │   │   └── ReviewerAgent.java
│   │   │   │
│   │   │   ├── skill/                # 后端调用运行时 Skill 的适配层
│   │   │   │   ├── Skill.java
│   │   │   │   ├── SkillRegistry.java
│   │   │   │   ├── FileWriteSkill.java
│   │   │   │   ├── ShellSkill.java
│   │   │   │   ├── TemplateRenderSkill.java
│   │   │   │   └── PlaywrightSkill.java
│   │   │   │
│   │   │   ├── workflow/
│   │   │   │   ├── WorkflowOrchestrator.java
│   │   │   │   ├── WorkflowStep.java
│   │   │   │   ├── WorkflowStatus.java
│   │   │   │   └── WorkflowContext.java
│   │   │   │
│   │   │   ├── llm/
│   │   │   │   ├── LlmClient.java
│   │   │   │   ├── LlmRequest.java
│   │   │   │   ├── LlmResponse.java
│   │   │   │   ├── ModelRouter.java
│   │   │   │   ├── OpenAiClient.java
│   │   │   │   ├── DeepSeekClient.java
│   │   │   │   └── QwenClient.java
│   │   │   │
│   │   │   ├── config/
│   │   │   │   ├── AgentModelConfig.java
│   │   │   │   ├── CorsConfig.java
│   │   │   │   └── WebSocketConfig.java
│   │   │   │
│   │   │   ├── dto/
│   │   │   │   ├── CreateWorkflowRequest.java
│   │   │   │   ├── WorkflowStatusResponse.java
│   │   │   │   ├── AgentStatusResponse.java
│   │   │   │   └── ResultResponse.java
│   │   │   │
│   │   │   └── common/
│   │   │       ├── ApiResponse.java
│   │   │       ├── ErrorCode.java
│   │   │       └── BizException.java
│   │   │
│   │   └── src/main/resources/
│   │       ├── application.yml
│   │       ├── agent-model.yml
│   │       └── prompts/
│   │           ├── product-agent.md
│   │           ├── design-agent.md
│   │           ├── developer-agent.md
│   │           ├── qa-agent.md
│   │           └── reviewer-agent.md
│   │
│   └── inspector-demo/               # 固定演示应用
│       ├── frontend/
│       │   ├── src/
│       │   ├── package.json
│       │   └── vite.config.js
│       │
│       ├── backend/
│       │   ├── pom.xml
│       │   ├── mvnw
│       │   ├── src/main/java/com/inspector/
│       │   └── src/main/resources/application.yml
│       │
│       └── tests/
│           ├── package.json
│           ├── playwright.config.js
│           └── e2e/
│               └── inspector.spec.js
│
├── packages/                         # 平台运行时可复用模块
│   ├── core/
│   │   ├── orchestrator/
│   │   ├── agent-runtime/
│   │   ├── model-router/
│   │   ├── memory/
│   │   └── types/
│   │
│   ├── agents/
│   │   ├── product-agent/
│   │   ├── design-agent/
│   │   ├── developer-agent/
│   │   ├── qa-agent/
│   │   └── reviewer-agent/
│   │
│   ├── skills/                       # ✅ 平台运行时 Skill，不是 Codex Skill
│   │   ├── prd-skill/
│   │   ├── ui-generate-skill/
│   │   ├── code-generate-skill/
│   │   ├── test-generate-skill/
│   │   ├── shell-skill/
│   │   ├── file-write-skill/
│   │   ├── report-generate-skill/
│   │   └── export-skill/
│   │
│   ├── workflows/
│   │   ├── software-dev-workflow/
│   │   └── quality-inspector-workflow/
│   │
│   └── templates/
│       ├── vue3-js-template/
│       │   ├── package.json
│       │   ├── vite.config.js
│       │   └── src/
│       │
│       ├── springboot-template/
│       │   ├── pom.xml
│       │   ├── mvnw
│       │   ├── mvnw.cmd
│       │   ├── .mvn/
│       │   └── src/
│       │
│       └── playwright-template/
│           ├── package.json
│           ├── playwright.config.js
│           └── e2e/
│
├── generated/                        # AI 动态生成产物
│   └── ai-quality-inspector/
│       ├── frontend/
│       ├── backend/
│       ├── tests/
│       └── generation-report.json
│
├── tests/                            # 平台级测试
│   ├── unit/
│   ├── integration/
│   └── e2e/
│
├── docs/                             # 主链路交付产物目录
│   └── ${需求或项目名称}/
│       ├── 产品设计/                  # UI 侧按“需求阶段产物”展示
│       │   ├── 01-变更记录/
│       │   │   └── 版本说明.md
│       │   ├── 02-产品需求文档/
│       │   │   ├── PRD.md
│       │   │   └── PRD.docx
│       │   ├── 03-UI设计规范/
│       │   │   └── UI-Design-Spec.md
│       │   ├── 04-流程图/
│       │   │   ├── 业务流程图.puml
│       │   │   ├── 信息架构图.puml
│       │   │   ├── 页面流转图.puml
│       │   │   └── 页面流转图.svg
│       │   └── 05-附录/
│       │       └── 术语表.md
│       ├── UI原型/
│       │   ├── 设计稿/
│       │   ├── 组件库/
│       │   │   └── 组件清单.md
│       │   ├── 交互原型/
│       │   │   └── 交互说明.md
│       │   └── 移动端适配/
│       │       └── 响应式断点参考.md
│       ├── 架构设计/
│       │   ├── 01-系统架构/
│       │   │   ├── 系统架构设计.md
│       │   │   ├── 系统架构图.puml
│       │   │   └── 系统架构图.svg
│       │   ├── 02-接口设计/
│       │   │   └── 接口定义/
│       │   ├── 03-部署架构/
│       │   └── 04-技术选型/
│       ├── 数据库设计/
│       │   ├── 数据字典/
│       │   ├── 建表脚本/
│       │   └── 数据迁移脚本/
│       ├── 测试/
│       │   ├── 测试用例/
│       │   └── 测试报告/
│       └── 其他文档/
│           └── 会议纪要/
├── project-docs/                     # 项目说明与历史设计文档
│   ├── architecture.md
│   ├── workflow.md
│   ├── skill-design.md
│   ├── model-routing.md
│   └── demo-script.md
│
├── scripts/
│   ├── run-web.sh
│   ├── run-server.sh
│   ├── run-demo.sh
│   ├── generate-inspector.sh
│   └── clean-generated.sh
│
└── docker/
    ├── docker-compose.yml
    ├── web.Dockerfile
    ├── server.Dockerfile
    └── generated-app.Dockerfile