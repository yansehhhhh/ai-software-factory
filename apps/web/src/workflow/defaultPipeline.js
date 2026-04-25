export const defaultPipeline = [
  {
    id: "requirements",
    title: "需求分析",
    owner: "Product Agent",
    status: "ready"
  },
  {
    id: "design",
    title: "UI 设计",
    owner: "Design Agent",
    status: "queued"
  },
  {
    id: "implementation",
    title: "代码生成",
    owner: "Developer Agent",
    status: "queued"
  },
  {
    id: "verification",
    title: "自动化测试",
    owner: "QA Agent",
    status: "queued"
  }
];
