const { test, expect } = require("@playwright/test");

test("renders the factory workflow dashboard", async ({ page }) => {
  const workflowStatus = {
    taskId: "test-task",
    requirement: "",
    status: "pending",
    statusLabel: "未开始",
    currentStage: "未开始",
    currentArtifactType: "--",
    designProgressMessage: "等待任务启动",
    progress: 0,
    estimatedRemaining: "--",
    estimatedCompletion: "--",
    testPassRate: "--",
    systemStatus: "在线",
    examples: ["AI质检助手", "数据分析系统", "简单博客系统"],
    steps: [
      {
        index: 1,
        key: "prd",
        title: "需求分析",
        status: "pending",
        progress: 0,
        detail: "未开始",
        duration: "--",
        error: null
      },
      {
        index: 2,
        key: "ui",
        title: "UI设计",
        status: "pending",
        progress: 0,
        detail: "等待需求分析",
        duration: "--",
        error: null
      }
    ],
    agents: [
      {
        name: "Product Agent",
        role: "需求分析",
        status: "pending",
        model: "gpt-4.1",
        duration: "--",
        progress: 0
      },
      {
        name: "Design Agent",
        role: "UI 设计",
        status: "pending",
        model: "qwen-max",
        duration: "--",
        progress: 0
      }
    ],
    error: null
  };

  await page.route("**/api/health", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        status: "UP",
        service: "ai-software-factory-server"
      })
    });
  });
  await page.route("**/api/workflow/status", async (route) => {
    await route.fulfill({ status: 200, contentType: "application/json", body: JSON.stringify(workflowStatus) });
  });
  await page.route("**/api/workflow/start", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        ...workflowStatus,
        status: "running",
        currentStage: "需求分析",
        currentArtifactType: "PRD",
        designProgressMessage: "Product Agent 正在生成 PRD",
        progress: 8
      })
    });
  });
  await page.route("**/api/logs", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify([
        { time: "10:15:23", agent: "Orchestrator", level: "info", message: "任务已创建" }
      ])
    });
  });
  await page.route("**/api/result", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        available: false,
        designAvailable: true,
        projectUrl: null,
        reportUrl: null,
        zipUrl: null,
        prdMarkdown: "# AI 质检助手\n\n聚焦上传日志、分析问题和输出报告。",
        pageSpecs: [
          {
            name: "Dashboard",
            description: "承载需求输入与结果总览",
            sections: ["需求输入", "执行流程", "设计结果"]
          }
        ],
        componentSpecs: [
          {
            name: "RequirementInput",
            description: "输入自然语言需求",
            capabilities: ["多行输入", "快速示例"]
          }
        ],
        userFlowSpecs: [
          {
            name: "提交需求并生成设计",
            steps: ["输入需求", "生成设计", "查看结果"]
          }
        ],
        uiGuidelines: ["页面保持浅色工作台风格"]
      })
    });
  });

  await page.goto("/");

  await expect(page.getByRole("heading", { name: "AI 编排平台" })).toBeVisible();
  await expect(page.getByRole("heading", { name: "输入需求" })).toBeVisible();
  await expect(page.getByPlaceholder("做一个AI质检助手")).toBeVisible();
  await expect(page.getByRole("heading", { name: "执行流程" })).toBeVisible();
  await expect(page.getByRole("heading", { name: "需求分析" })).toBeVisible();
  await expect(page.getByRole("heading", { name: "实时日志" })).toBeVisible();
  await expect(page.getByText("[Orchestrator]")).toBeVisible();
  await expect(page.getByRole("heading", { name: "参与 Agent 列表" })).toBeVisible();
  await expect(page.getByText("Product Agent")).toBeVisible();
  await expect(page.getByRole("heading", { name: "结果与操作" })).toBeVisible();
  await expect(page.getByRole("heading", { name: "产品设计结果" })).toBeVisible();
  await expect(page.getByText("Dashboard")).toBeVisible();
  await expect(page.getByText("RequirementInput")).toBeVisible();

  await page.getByRole("button", { name: "AI质检助手" }).click();
  await expect(page.getByPlaceholder("做一个AI质检助手")).toHaveValue(/AI质检助手/);
  await page.getByRole("button", { name: "一键生成并运行" }).click();
});
