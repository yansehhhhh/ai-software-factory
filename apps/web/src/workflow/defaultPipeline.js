export const defaultPipeline = [
  {
    id: "requirements",
    title: "需求分析",
    owner: "Requirement Agent",
    status: "ready"
  },
  {
    id: "brainstorming",
    title: "需求讨论",
    owner: "Requirement Agent",
    status: "queued"
  },
  {
    id: "writing-plans",
    title: "制定计划",
    owner: "Product Agent",
    status: "queued"
  },
  {
    id: "product-design-artifacts",
    title: "需求产物生成",
    owner: "Product Agent",
    status: "queued"
  },
  {
    id: "ui-design",
    title: "UI设计",
    owner: "Design Agent",
    status: "queued"
  },
  {
    id: "architecture-design",
    title: "架构设计",
    owner: "Architecture Agent",
    status: "queued"
  },
  {
    id: "api-design",
    title: "接口设计",
    owner: "API Agent",
    status: "queued"
  },
  {
    id: "database-design",
    title: "数据库设计",
    owner: "Database Agent",
    status: "queued"
  },
  {
    id: "backend-development",
    title: "后端开发",
    owner: "Backend Agent",
    status: "queued"
  },
  {
    id: "frontend-development",
    title: "前端开发",
    owner: "Frontend Agent",
    status: "queued"
  },
  {
    id: "development-integration",
    title: "开发联调",
    owner: "Frontend Agent",
    status: "queued"
  },
  {
    id: "test-case-generation",
    title: "测试用例生成",
    owner: "QA Agent",
    status: "queued"
  },
  {
    id: "e2e-acceptance-testing",
    title: "E2E验收测试",
    owner: "QA Agent",
    status: "queued"
  },
  {
    id: "verification-before-completion",
    title: "完成前验证",
    owner: "QA Agent",
    status: "queued"
  },
  {
    id: "delivery",
    title: "交付",
    owner: "Orchestrator",
    status: "queued"
  }
];
